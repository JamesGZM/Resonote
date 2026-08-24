package com.resonote.feature.player.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.AuthRepository
import com.resonote.core.data.LikedSongsRepository
import com.resonote.core.data.LyricsPreferencesRepository
import com.resonote.core.data.LyricsRepository
import com.resonote.core.model.AuthState
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.LyricsDocument
import com.resonote.core.model.LyricsPreferences
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.PlaybackMode
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.playback.PlaybackController
import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackOrigin
import com.resonote.core.playback.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LyricsUiState {
    data object Idle : LyricsUiState
    data object Loading : LyricsUiState
    data object Empty : LyricsUiState
    data object Unavailable : LyricsUiState
    data class Content(val document: LyricsDocument) : LyricsUiState
    data class Error(val failure: ContentFailure) : LyricsUiState
}

sealed interface LikeUiState {
    data object Unsupported : LikeUiState
    data object LoginRequired : LikeUiState
    data object Loading : LikeUiState
    data class Available(val isLiked: Boolean, val isUpdating: Boolean = false) : LikeUiState
}

sealed interface PlayerEvent {
    data object LoginRequired : PlayerEvent
    data object LikeFailed : PlayerEvent
    data object LikeUnsupported : PlayerEvent
}

data class PlayerUiState(
    val playback: PlaybackState = PlaybackState(),
    val lyrics: LyricsUiState = LyricsUiState.Idle,
    val lyricsPreferences: LyricsPreferences = LyricsPreferences(),
    val like: LikeUiState = LikeUiState.Unsupported,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
    private val lyricsRepository: LyricsRepository,
    private val lyricsPreferencesRepository: LyricsPreferencesRepository,
    private val likedSongsRepository: LikedSongsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    constructor(
        playbackController: PlaybackController,
        lyricsRepository: LyricsRepository,
    ) : this(
        playbackController,
        lyricsRepository,
        TestLyricsPreferencesRepository,
        TestLikedSongsRepository,
        TestAuthRepository,
    )
    private val lyricsState = MutableStateFlow<LyricsUiState>(LyricsUiState.Idle)
    private val likeState = MutableStateFlow<LikeUiState>(LikeUiState.Unsupported)
    private val mutableEvents = MutableSharedFlow<PlayerEvent>(extraBufferCapacity = 1)
    private var lyricsGeneration = 0L
    private var currentUserId: String? = null
    val events: SharedFlow<PlayerEvent> = mutableEvents.asSharedFlow()

    val uiState: StateFlow<PlayerUiState> = combine(
        playbackController.state,
        lyricsState,
        lyricsPreferencesRepository.preferences,
        likeState,
    ) { playback, lyrics, lyricsPreferences, like ->
        PlayerUiState(playback, lyrics, lyricsPreferences, like)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerUiState(playbackController.state.value),
    )

    init {
        viewModelScope.launch {
            playbackController.state
                .map { state ->
                    state.currentItem?.let { item ->
                        item.lyricsRequest()?.let(LyricsTarget::Request) ?: LyricsTarget.Unavailable
                    } ?: LyricsTarget.None
                }
                .distinctUntilChanged()
                .collectLatest { target ->
                    when (target) {
                        LyricsTarget.None -> lyricsState.value = LyricsUiState.Idle
                        LyricsTarget.Unavailable -> lyricsState.value = LyricsUiState.Unavailable
                        is LyricsTarget.Request -> loadLyrics(target.value)
                    }
                }
        }
        viewModelScope.launch {
            combine(
                playbackController.state.map { (it.currentItem?.origin as? PlaybackOrigin.Online)?.song },
                authRepository.authState,
            ) { song, auth -> song to (auth as? AuthState.Authenticated)?.userId }
                .distinctUntilChanged { old, new -> old.first?.hash == new.first?.hash && old.second == new.second }
                .collectLatest { (song, userId) ->
                    currentUserId = userId
                    likeState.value = when {
                        song == null -> LikeUiState.Unsupported
                        userId == null -> LikeUiState.LoginRequired
                        else -> {
                            when (val snapshot = likedSongsRepository.load()) {
                                is CollectionLoadResult.Available ->
                                    LikeUiState.Available(snapshot.value?.fileIdsByHash?.containsKey(song.hash) == true)
                                is CollectionLoadResult.Failed -> LikeUiState.Available(isLiked = false)
                            }
                        }
                    }
                }
        }
    }

    fun retryLyrics() {
        playbackController.state.value.currentItem?.lyricsRequest()?.let { request ->
            viewModelScope.launch { loadLyrics(request) }
        }
    }

    fun togglePlayPause() = playbackController.togglePlayPause()

    fun previous() = playbackController.previous()

    fun next() = playbackController.next()

    fun seekTo(positionMillis: Long) = playbackController.seekTo(positionMillis)

    fun setMode(mode: PlaybackMode) = playbackController.setMode(mode)

    fun setPlaybackSpeed(speed: PlaybackSpeed) = playbackController.setPlaybackSpeed(speed)

    fun setCurrentOnlineQuality(quality: OnlinePlaybackQuality) = playbackController.setCurrentOnlineQuality(quality)

    fun toggleLike() {
        val song = (playbackController.state.value.currentItem?.origin as? PlaybackOrigin.Online)?.song
        if (song == null) {
            mutableEvents.tryEmit(PlayerEvent.LikeUnsupported)
            return
        }
        when (val current = likeState.value) {
            LikeUiState.LoginRequired -> mutableEvents.tryEmit(PlayerEvent.LoginRequired)
            is LikeUiState.Available -> if (!current.isUpdating) {
                val userId = currentUserId ?: return
                likeState.value = current.copy(isUpdating = true)
                viewModelScope.launch {
                    val result = if (current.isLiked) {
                        likedSongsRepository.unlike(
                            song,
                        )
                    } else {
                        likedSongsRepository.like(song)
                    }
                    if (
                        currentUserId == userId &&
                        (playbackController.state.value.currentItem?.origin as? PlaybackOrigin.Online)?.song?.hash ==
                        song.hash
                    ) {
                        likeState.value = if (result is CollectionLoadResult.Available) {
                            LikeUiState.Available(!current.isLiked)
                        } else {
                            mutableEvents.tryEmit(PlayerEvent.LikeFailed)
                            current
                        }
                    }
                }
            }
            LikeUiState.Loading -> Unit
            LikeUiState.Unsupported -> mutableEvents.tryEmit(PlayerEvent.LikeUnsupported)
        }
    }

    fun selectQueueItem(index: Int) = playbackController.selectQueueItem(index)

    fun removeQueueItem(index: Int) = playbackController.removeQueueItem(index)

    fun clearQueue() = playbackController.clear()

    private suspend fun loadLyrics(request: LyricsRequest) {
        val generation = ++lyricsGeneration
        lyricsState.value = LyricsUiState.Loading
        val loadedState = when (val result = lyricsRepository.loadLyrics(request.hash, request.albumAudioId)) {
            is CollectionLoadResult.Available -> if (result.value.lines.isEmpty()) {
                LyricsUiState.Empty
            } else {
                LyricsUiState.Content(LyricsDocument(result.value.lines.sortedBy { it.timeMillis }))
            }
            is CollectionLoadResult.Failed -> LyricsUiState.Error(result.failure)
        }
        if (generation == lyricsGeneration && request == playbackController.state.value.currentItem?.lyricsRequest()) {
            lyricsState.value = loadedState
        }
    }
}

private object TestLyricsPreferencesRepository : LyricsPreferencesRepository {
    override val preferences = flowOf(LyricsPreferences())
    override suspend fun setPreferences(value: LyricsPreferences) = Unit
    override suspend fun reset() = Unit
}

private object TestLikedSongsRepository : LikedSongsRepository {
    override suspend fun load() = CollectionLoadResult.Available<com.resonote.core.data.LikedSongsSnapshot?>(null)
    override suspend fun like(song: com.resonote.core.model.OnlineSong) = CollectionLoadResult.Available(Unit)
    override suspend fun unlike(song: com.resonote.core.model.OnlineSong) = CollectionLoadResult.Available(Unit)
}

private object TestAuthRepository : AuthRepository {
    override val authState = flowOf<AuthState>(AuthState.Anonymous)
    override suspend fun acknowledgeAuthenticationGate() = Unit
    override suspend fun logout() = Unit
    override suspend fun sendMobileCode(mobile: String): com.resonote.core.model.SendMobileCodeResult = error("unused")
    override suspend fun loginWithMobileCode(
        mobile: String,
        code: String,
        selectedUserId: String?,
    ): com.resonote.core.model.MobileCodeLoginResult = error("unused")
    override suspend fun loginWithPassword(
        username: String,
        password: String,
    ): com.resonote.core.model.PasswordLoginResult = error("unused")
    override suspend fun createQrLoginKey(): com.resonote.core.model.QrLoginKeyResult = error("unused")
    override suspend fun checkQrLogin(key: String): com.resonote.core.model.QrLoginCheckResult = error("unused")
}

private sealed interface LyricsTarget {
    data object None : LyricsTarget
    data object Unavailable : LyricsTarget
    data class Request(val value: LyricsRequest) : LyricsTarget
}

private data class LyricsRequest(val hash: String, val albumAudioId: String?)

private fun PlaybackItem.lyricsRequest(): LyricsRequest? = when (val value = origin) {
    is PlaybackOrigin.Online -> LyricsRequest(value.song.hash, value.song.albumAudioId)
    is PlaybackOrigin.Cloud -> LyricsRequest(value.track.hash, value.track.albumAudioId)
    is PlaybackOrigin.Local -> null
}

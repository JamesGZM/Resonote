package com.resonote.feature.library.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.AuthRepository
import com.resonote.core.data.LibraryRepository
import com.resonote.core.data.UserProfileRepository
import com.resonote.core.model.AuthState
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaylistTrackInput
import com.resonote.core.model.UserPlaylist
import com.resonote.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

@HiltViewModel
class MyViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: UserProfileRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<MyUiState>(MyUiState.CheckingAccount)
    val uiState: StateFlow<MyUiState> = mutableUiState.asStateFlow()
    private val mutableRefreshFailures = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshFailures: SharedFlow<Unit> = mutableRefreshFailures.asSharedFlow()

    private var activeUserId: String? = null
    private var lastVisibleUserId: String? = null
    private var refreshJob: Job? = null
    private var createPlaylistJob: Job? = null
    private var addTrackJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.authState.collectLatest { authState ->
                lastVisibleUserId = null
                refreshJob?.cancel()
                refreshJob = null
                createPlaylistJob?.cancel()
                createPlaylistJob = null
                addTrackJob?.cancel()
                addTrackJob = null
                when (authState) {
                    AuthState.Anonymous, is AuthState.AuthenticationRequired -> {
                        activeUserId = null
                        mutableUiState.value = MyUiState.Anonymous
                    }
                    is AuthState.Authenticated -> {
                        activeUserId = authState.userId
                        mutableUiState.value = MyUiState.Authenticated(userId = authState.userId)
                        loadAll(authState.userId)
                    }
                }
            }
        }
    }

    fun onVisible() {
        val userId = activeUserId ?: return
        if (lastVisibleUserId != userId) {
            lastVisibleUserId = userId
            return
        }
        refresh(userId, showIndicator = false, reportFailure = false)
    }

    fun refresh() {
        val userId = activeUserId ?: return
        refresh(userId, showIndicator = true, reportFailure = true)
    }

    private fun refresh(userId: String, showIndicator: Boolean, reportFailure: Boolean) {
        if (
            refreshJob?.isActive == true ||
            createPlaylistJob?.isActive == true ||
            addTrackJob?.isActive == true
        ) {
            return
        }
        if (showIndicator) {
            mutableUiState.update { state ->
                (state as? MyUiState.Authenticated)?.copy(isRefreshing = true) ?: state
            }
        }
        refreshJob = viewModelScope.launch {
            val succeeded = refreshAll(userId)
            updateAuthenticated(userId) { it.copy(isRefreshing = false) }
            if (!succeeded && reportFailure) mutableRefreshFailures.tryEmit(Unit)
        }
    }

    fun retryProfile() {
        val userId = activeUserId ?: return
        updateAuthenticated(userId) { it.copy(profile = MySectionState.Loading) }
        viewModelScope.launch { loadProfile(userId) }
    }

    fun retryPlaylists() {
        val userId = activeUserId ?: return
        updateAuthenticated(userId) { it.copy(playlists = MySectionState.Loading) }
        viewModelScope.launch { loadPlaylists(userId) }
    }

    fun createPlaylist(name: String) {
        val userId = activeUserId ?: return
        val normalizedName = name.trim()
        if (
            normalizedName.isEmpty() ||
            createPlaylistJob?.isActive == true ||
            refreshJob?.isActive == true ||
            addTrackJob?.isActive == true
        ) {
            return
        }

        updateAuthenticated(userId) {
            it.copy(playlistCreation = PlaylistCreationUiState.Submitting)
        }
        lateinit var job: Job
        job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                when (val result = libraryRepository.createPlaylist(normalizedName)) {
                    is CollectionLoadResult.Failed -> updateAuthenticated(userId) {
                        it.copy(playlistCreation = PlaylistCreationUiState.Failed(result.failure))
                    }
                    is CollectionLoadResult.Available -> refreshAfterPlaylistCreation(
                        userId = userId,
                        name = normalizedName,
                        listId = result.value,
                    )
                }
            } finally {
                if (createPlaylistJob === job) createPlaylistJob = null
            }
        }
        createPlaylistJob = job
        job.start()
    }

    fun dismissPlaylistCreation() {
        val userId = activeUserId ?: return
        updateAuthenticated(userId) { state ->
            if (state.playlistCreation == PlaylistCreationUiState.Submitting) {
                state
            } else {
                state.copy(playlistCreation = PlaylistCreationUiState.Idle)
            }
        }
    }

    fun acknowledgePlaylistCreation() {
        val userId = activeUserId ?: return
        updateAuthenticated(userId) { state ->
            if (state.playlistCreation is PlaylistCreationUiState.Created) {
                state.copy(playlistCreation = PlaylistCreationUiState.Idle)
            } else {
                state
            }
        }
    }

    fun preparePlaylistAddition() {
        val userId = activeUserId ?: return
        updateAuthenticated(userId) { state ->
            if (state.playlistAddition is PlaylistAdditionUiState.Submitting) {
                state
            } else {
                state.copy(playlistAddition = PlaylistAdditionUiState.Idle)
            }
        }
    }

    fun addSongToPlaylist(playlist: UserPlaylist, song: OnlineSong) {
        val userId = activeUserId ?: return
        if (
            addTrackJob?.isActive == true ||
            refreshJob?.isActive == true ||
            createPlaylistJob?.isActive == true
        ) {
            return
        }
        val state = mutableUiState.value as? MyUiState.Authenticated ?: return
        val writablePlaylist = (state.playlists as? MySectionState.Available)
            ?.value
            ?.firstOrNull { it.listId == playlist.listId && it.isMine }
            ?: return

        updateAuthenticated(userId) {
            it.copy(playlistAddition = PlaylistAdditionUiState.Submitting(writablePlaylist.listId))
        }
        lateinit var job: Job
        job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                val input = PlaylistTrackInput(
                    hash = song.hash,
                    title = song.title,
                    artist = song.artist.orEmpty(),
                    albumId = song.albumId,
                    albumAudioId = song.albumAudioId,
                )
                when (val result = libraryRepository.addTracks(writablePlaylist.listId, listOf(input))) {
                    is CollectionLoadResult.Failed -> updateAuthenticated(userId) {
                        it.copy(
                            playlistAddition = PlaylistAdditionUiState.Failed(
                                writablePlaylist.listId,
                                result.failure,
                            ),
                        )
                    }
                    is CollectionLoadResult.Available -> updateAuthenticated(userId) { latest ->
                        latest.copy(
                            playlists = latest.playlists.incrementPlaylistCount(writablePlaylist.listId),
                            playlistAddition = PlaylistAdditionUiState.Added(
                                playlistName = writablePlaylist.name,
                                songTitle = song.title,
                            ),
                        )
                    }
                }
            } finally {
                if (addTrackJob === job) addTrackJob = null
            }
        }
        addTrackJob = job
        job.start()
    }

    fun dismissPlaylistAdditionFailure() {
        val userId = activeUserId ?: return
        updateAuthenticated(userId) { state ->
            if (state.playlistAddition is PlaylistAdditionUiState.Failed) {
                state.copy(playlistAddition = PlaylistAdditionUiState.Idle)
            } else {
                state
            }
        }
    }

    fun acknowledgePlaylistAddition() {
        val userId = activeUserId ?: return
        updateAuthenticated(userId) { state ->
            if (state.playlistAddition is PlaylistAdditionUiState.Added) {
                state.copy(playlistAddition = PlaylistAdditionUiState.Idle)
            } else {
                state
            }
        }
    }

    private suspend fun loadAll(userId: String) = supervisorScope {
        launch { loadProfile(userId) }
        launch { loadPlaylists(userId) }
    }

    private suspend fun refreshAll(userId: String): Boolean = supervisorScope {
        val profile = async { profileRepository.loadProfile() }
        val playlists = async { libraryRepository.loadPlaylists() }
        val profileResult = profile.await()
        val playlistsResult = playlists.await()
        applyProfileRefresh(userId, profileResult)
        applyPlaylistRefresh(userId, playlistsResult)
        profileResult is CollectionLoadResult.Available && playlistsResult is CollectionLoadResult.Available
    }

    private fun applyProfileRefresh(userId: String, result: CollectionLoadResult<UserProfile>) {
        updateAuthenticated(userId) { state ->
            when (result) {
                is CollectionLoadResult.Available -> state.copy(profile = MySectionState.Available(result.value))
                is CollectionLoadResult.Failed -> if (state.profile is MySectionState.Available) {
                    state
                } else {
                    state.copy(profile = MySectionState.Failed(result.failure))
                }
            }
        }
    }

    private fun applyPlaylistRefresh(userId: String, result: CollectionLoadResult<List<UserPlaylist>>) {
        updateAuthenticated(userId) { state ->
            when (result) {
                is CollectionLoadResult.Available -> state.copy(playlists = MySectionState.Available(result.value))
                is CollectionLoadResult.Failed -> if (state.playlists is MySectionState.Available) {
                    state
                } else {
                    state.copy(playlists = MySectionState.Failed(result.failure))
                }
            }
        }
    }

    private suspend fun loadProfile(userId: String) {
        val section = when (val result = profileRepository.loadProfile()) {
            is CollectionLoadResult.Available -> MySectionState.Available(result.value)
            is CollectionLoadResult.Failed -> MySectionState.Failed(result.failure)
        }
        updateAuthenticated(userId) { it.copy(profile = section) }
    }

    private suspend fun loadPlaylists(userId: String) {
        val section = when (val result = libraryRepository.loadPlaylists()) {
            is CollectionLoadResult.Available -> MySectionState.Available(result.value)
            is CollectionLoadResult.Failed -> MySectionState.Failed(result.failure)
        }
        updateAuthenticated(userId) { it.copy(playlists = section) }
    }

    private suspend fun refreshAfterPlaylistCreation(userId: String, name: String, listId: String) {
        when (val refreshed = libraryRepository.loadPlaylists()) {
            is CollectionLoadResult.Available -> updateAuthenticated(userId) {
                it.copy(
                    playlists = MySectionState.Available(refreshed.value),
                    playlistCreation = PlaylistCreationUiState.Created(
                        name = name,
                        listId = listId,
                        refreshFailed = false,
                    ),
                )
            }
            is CollectionLoadResult.Failed -> updateAuthenticated(userId) {
                it.copy(
                    playlistCreation = PlaylistCreationUiState.Created(
                        name = name,
                        listId = listId,
                        refreshFailed = true,
                    ),
                )
            }
        }
    }

    private inline fun updateAuthenticated(
        userId: String,
        transform: (MyUiState.Authenticated) -> MyUiState.Authenticated,
    ) {
        if (activeUserId != userId) return
        mutableUiState.update { state ->
            val authenticated = state as? MyUiState.Authenticated ?: return@update state
            transform(authenticated)
        }
    }
}

private fun MySectionState<List<UserPlaylist>>.incrementPlaylistCount(
    listId: String,
): MySectionState<List<UserPlaylist>> = when (this) {
    is MySectionState.Available -> copy(
        value = value.map { playlist ->
            if (playlist.listId == listId) playlist.copy(count = playlist.count + 1) else playlist
        },
    )
    else -> this
}

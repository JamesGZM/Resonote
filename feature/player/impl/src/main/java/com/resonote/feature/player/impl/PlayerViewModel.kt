package com.resonote.feature.player.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.LyricsRepository
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.LyricLine
import com.resonote.core.playback.PlaybackController
import com.resonote.core.playback.PlaybackMode
import com.resonote.core.playback.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LyricsUiState {
    data object Idle : LyricsUiState
    data object Loading : LyricsUiState
    data object Empty : LyricsUiState
    data class Content(val lines: List<LyricLine>) : LyricsUiState
    data class Error(val failure: ContentFailure) : LyricsUiState
}

data class PlayerUiState(
    val playback: PlaybackState = PlaybackState(),
    val lyrics: LyricsUiState = LyricsUiState.Idle,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackController: PlaybackController,
    private val lyricsRepository: LyricsRepository,
) : ViewModel() {
    private val lyricsState = MutableStateFlow<LyricsUiState>(LyricsUiState.Idle)
    private var lyricsGeneration = 0L

    val uiState: StateFlow<PlayerUiState> = combine(playbackController.state, lyricsState) { playback, lyrics ->
        PlayerUiState(playback, lyrics)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerUiState(playbackController.state.value),
    )

    init {
        viewModelScope.launch {
            playbackController.state
                .map { state -> state.currentSong?.let { song -> LyricsRequest(song.hash, song.albumAudioId) } }
                .distinctUntilChanged()
                .collectLatest { request ->
                    if (request == null) {
                        lyricsState.value = LyricsUiState.Idle
                    } else {
                        loadLyrics(request)
                    }
                }
        }
    }

    fun retryLyrics() {
        playbackController.state.value.currentSong?.let { song ->
            viewModelScope.launch { loadLyrics(LyricsRequest(song.hash, song.albumAudioId)) }
        }
    }

    fun togglePlayPause() = playbackController.togglePlayPause()

    fun previous() = playbackController.previous()

    fun next() = playbackController.next()

    fun seekTo(positionMillis: Long) = playbackController.seekTo(positionMillis)

    fun setMode(mode: PlaybackMode) = playbackController.setMode(mode)

    fun selectQueueItem(index: Int) = playbackController.selectQueueItem(index)

    fun removeQueueItem(index: Int) = playbackController.removeQueueItem(index)

    fun moveQueueItem(fromIndex: Int, toIndex: Int) = playbackController.moveQueueItem(fromIndex, toIndex)

    fun clearQueue() = playbackController.clear()

    private suspend fun loadLyrics(request: LyricsRequest) {
        val generation = ++lyricsGeneration
        lyricsState.value = LyricsUiState.Loading
        val loadedState = when (val result = lyricsRepository.loadLyrics(request.hash, request.albumAudioId)) {
            is CollectionLoadResult.Available -> if (result.value.isEmpty()) {
                LyricsUiState.Empty
            } else {
                LyricsUiState.Content(result.value.sortedBy { it.timeMillis })
            }
            is CollectionLoadResult.Failed -> LyricsUiState.Error(result.failure)
        }
        if (generation == lyricsGeneration && request == playbackController.state.value.currentSong?.let {
                song -> LyricsRequest(song.hash, song.albumAudioId)
            }
        ) {
            lyricsState.value = loadedState
        }
    }
}

private data class LyricsRequest(val hash: String, val albumAudioId: String?)

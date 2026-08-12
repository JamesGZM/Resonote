package com.resonote.core.playback

import com.resonote.core.model.CloudTrack
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaybackUnavailableReason
import com.resonote.core.model.ResolvedSongSource
import kotlinx.coroutines.flow.StateFlow

sealed interface PlaybackOrigin {
    data object Online : PlaybackOrigin

    data class Cloud(val track: CloudTrack) : PlaybackOrigin
}

data class PlaybackItem(
    val song: OnlineSong,
    val origin: PlaybackOrigin = PlaybackOrigin.Online,
    val resolvedSource: ResolvedSongSource? = null,
)

enum class PlaybackStatus {
    Idle,
    Resolving,
    Buffering,
    Playing,
    Paused,
    Ended,
    Failed,
}

enum class PlaybackMode {
    ListLoop,
    Shuffle,
    SingleLoop,
    Sequential,
}

sealed interface PlaybackIssue {
    data class Unavailable(val reason: PlaybackUnavailableReason) : PlaybackIssue

    data class SourceFailure(val failure: ContentFailure) : PlaybackIssue

    data class PlayerFailure(val message: String?) : PlaybackIssue
}

data class PlaybackState(
    val queue: List<PlaybackItem> = emptyList(),
    val currentIndex: Int = -1,
    val status: PlaybackStatus = PlaybackStatus.Idle,
    val positionMillis: Long = 0,
    val durationMillis: Long = 0,
    val bufferedPositionMillis: Long = 0,
    val mode: PlaybackMode = PlaybackMode.ListLoop,
    val issue: PlaybackIssue? = null,
) {
    val currentItem: PlaybackItem?
        get() = queue.getOrNull(currentIndex)

    val currentSong: OnlineSong?
        get() = currentItem?.song

    val isPlaying: Boolean
        get() = status == PlaybackStatus.Playing

    val progress: Float
        get() = if (durationMillis > 0) {
            (positionMillis.toFloat() / durationMillis).coerceIn(0f, 1f)
        } else {
            0f
        }
}

interface PlaybackController {
    val state: StateFlow<PlaybackState>

    fun play(item: PlaybackItem)

    fun playAll(items: List<PlaybackItem>, startIndex: Int = 0)

    fun append(items: List<PlaybackItem>)

    fun selectQueueItem(index: Int)

    fun removeQueueItem(index: Int)

    fun moveQueueItem(fromIndex: Int, toIndex: Int)

    fun togglePlayPause()

    fun pause()

    fun next()

    fun previous()

    fun seekTo(positionMillis: Long)

    fun setMode(mode: PlaybackMode)

    fun clear()
}

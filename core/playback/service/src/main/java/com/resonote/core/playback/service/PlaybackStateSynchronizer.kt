package com.resonote.core.playback.service

import androidx.media3.common.C
import androidx.media3.common.Player
import com.resonote.core.playback.PlaybackIssue
import com.resonote.core.playback.PlaybackState
import com.resonote.core.playback.PlaybackStatus

internal fun Player.snapshotPlaybackState(
    queue: PlaybackQueue,
    previousState: PlaybackState,
    pausedPreviewGeneration: Long,
    loadGeneration: Long,
): PlaybackState {
    val hasLoadedMedia = mediaItemCount > 0
    val duration = queue.currentItem?.vipPreviewDurationMillisOrNull()
        ?: this.duration.takeUnless { it == C.TIME_UNSET || it < 0 }
        ?: queue.currentItem?.resolvedSource?.durationMillis
        ?: queue.currentItem?.metadata?.durationMillis
        ?: 0L
    val status = when {
        playerError != null -> PlaybackStatus.Failed
        playbackState == Player.STATE_BUFFERING -> PlaybackStatus.Buffering
        playbackState == Player.STATE_ENDED && pausedPreviewGeneration == loadGeneration -> PlaybackStatus.Paused
        playbackState == Player.STATE_ENDED -> PlaybackStatus.Ended
        isPlaying -> PlaybackStatus.Playing
        queue.currentItem != null -> PlaybackStatus.Paused
        else -> PlaybackStatus.Idle
    }
    return previousState.copy(
        queue = queue.items,
        currentIndex = queue.currentIndex,
        status = status,
        positionMillis = if (hasLoadedMedia) currentPosition.coerceAtLeast(0) else previousState.positionMillis,
        durationMillis = duration,
        bufferedPositionMillis = if (hasLoadedMedia) {
            bufferedPosition.coerceAtLeast(0)
        } else {
            previousState.bufferedPositionMillis
        },
        issue = playerError?.let { PlaybackIssue.PlayerFailure(it.message) },
    )
}

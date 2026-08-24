package com.resonote.core.playback.service

import com.resonote.core.model.PlaybackMode
import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackOrigin
import com.resonote.core.playback.PlaybackStatus

internal fun playbackPreloadCandidate(queue: PlaybackQueue, mode: PlaybackMode): PlaybackItem? {
    val candidate = when (mode) {
        PlaybackMode.Sequential -> queue.peekNext(wrap = false)
        PlaybackMode.ListLoop -> queue.peekNext(wrap = true)
        PlaybackMode.Shuffle, PlaybackMode.SingleLoop -> null
    }
    return candidate?.takeUnless { it.origin is PlaybackOrigin.Local }
}

internal fun shouldStartPlaybackPreload(
    status: PlaybackStatus,
    positionMillis: Long,
    durationMillis: Long,
    alreadyAttempted: Boolean,
): Boolean {
    if (alreadyAttempted || status != PlaybackStatus.Playing || durationMillis <= 0) return false
    val remainingMillis = durationMillis - positionMillis
    return remainingMillis in 1..PLAYBACK_PRELOAD_THRESHOLD_MILLIS
}

internal fun isPrefetchedSourceFresh(resolvedAtElapsedRealtimeMillis: Long, nowElapsedRealtimeMillis: Long): Boolean =
    nowElapsedRealtimeMillis - resolvedAtElapsedRealtimeMillis in 0..PREFETCHED_SOURCE_MAX_AGE_MILLIS

internal const val PLAYBACK_PRELOAD_THRESHOLD_MILLIS = 30_000L
internal const val PREFETCHED_SOURCE_MAX_AGE_MILLIS = 60_000L

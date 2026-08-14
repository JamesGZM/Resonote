package com.resonote.core.playback.service

import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackMode
import com.resonote.core.playback.PlaybackOrigin

internal enum class PlaybackCompletionAction {
    Advance,
    Pause,
    Replay,
}

internal fun PlaybackItem.vipPreviewDurationMillisOrNull(): Long? {
    if (!metadata.isVip) return null
    val declaredPreviewDurationMillis = (origin as? PlaybackOrigin.Online)
        ?.song
        ?.previewDurationMillis
        ?.takeIf { durationMillis ->
            durationMillis > 0 &&
                metadata.durationMillis - durationMillis > VIP_PREVIEW_DURATION_TOLERANCE_MILLIS
        }
    if (declaredPreviewDurationMillis != null) return declaredPreviewDurationMillis

    val sourceDurationMillis = resolvedSource?.durationMillis ?: return null
    return sourceDurationMillis.takeIf { durationMillis ->
        durationMillis > 0 &&
            metadata.durationMillis - durationMillis > VIP_PREVIEW_DURATION_TOLERANCE_MILLIS
    }
}

internal fun vipPreviewCompletionAction(
    item: PlaybackItem,
    mode: PlaybackMode,
    queueSize: Int,
    positionMillis: Long,
): PlaybackCompletionAction? {
    val previewDurationMillis = item.vipPreviewDurationMillisOrNull() ?: return null
    if (positionMillis < previewDurationMillis - VIP_PREVIEW_BOUNDARY_TOLERANCE_MILLIS) return null

    return if (mode == PlaybackMode.SingleLoop || queueSize <= 1) {
        PlaybackCompletionAction.Pause
    } else {
        PlaybackCompletionAction.Advance
    }
}

internal fun playbackEndedCompletionAction(mode: PlaybackMode): PlaybackCompletionAction =
    if (mode == PlaybackMode.SingleLoop) PlaybackCompletionAction.Replay else PlaybackCompletionAction.Advance

private const val VIP_PREVIEW_DURATION_TOLERANCE_MILLIS = 5_000L
private const val VIP_PREVIEW_BOUNDARY_TOLERANCE_MILLIS = 300L

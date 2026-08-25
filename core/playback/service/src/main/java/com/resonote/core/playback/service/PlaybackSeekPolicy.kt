package com.resonote.core.playback.service

import kotlin.math.abs

internal fun shouldRetainPendingSeekPosition(
    targetPositionMillis: Long,
    reportedPositionMillis: Long,
    elapsedSinceRequestMillis: Long,
): Boolean = elapsedSinceRequestMillis < SEEK_ACKNOWLEDGEMENT_TIMEOUT_MILLIS &&
    abs(targetPositionMillis - reportedPositionMillis) > SEEK_ACKNOWLEDGEMENT_TOLERANCE_MILLIS

private const val SEEK_ACKNOWLEDGEMENT_TOLERANCE_MILLIS = 2_000L
private const val SEEK_ACKNOWLEDGEMENT_TIMEOUT_MILLIS = 2_500L

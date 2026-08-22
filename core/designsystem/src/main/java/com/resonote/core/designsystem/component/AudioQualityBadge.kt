package com.resonote.core.designsystem.component

import com.resonote.core.model.AudioQuality

fun AudioQuality.compactBadgeLabel(): String? = when (this) {
    AudioQuality.Standard -> null
    AudioQuality.HighQuality -> "HQ"
    AudioQuality.HighResolution -> "HR"
    AudioQuality.Lossless -> "SQ"
}

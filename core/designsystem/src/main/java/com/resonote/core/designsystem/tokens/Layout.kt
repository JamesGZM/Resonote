package com.resonote.core.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ResonoteWindowWidth {
    COMPACT,
    MEDIUM,
    EXPANDED,
    LARGE,
    EXTRA_LARGE,
}

enum class ResonoteLayoutMode {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

val ResonoteWindowWidth.layoutMode: ResonoteLayoutMode
    get() = when (this) {
        ResonoteWindowWidth.COMPACT -> ResonoteLayoutMode.COMPACT
        ResonoteWindowWidth.MEDIUM -> ResonoteLayoutMode.MEDIUM
        ResonoteWindowWidth.EXPANDED,
        ResonoteWindowWidth.LARGE,
        ResonoteWindowWidth.EXTRA_LARGE,
        -> ResonoteLayoutMode.EXPANDED
    }

fun calculateResonoteWindowWidth(width: Dp): ResonoteWindowWidth = when {
    width < 600.dp -> ResonoteWindowWidth.COMPACT
    width < 840.dp -> ResonoteWindowWidth.MEDIUM
    width < 1_200.dp -> ResonoteWindowWidth.EXPANDED
    width < 1_600.dp -> ResonoteWindowWidth.LARGE
    else -> ResonoteWindowWidth.EXTRA_LARGE
}

@Immutable
data class ResonoteLayoutTokens internal constructor(
    val compactColumns: Int = 4,
    val compactOuterMargin: Dp = 16.dp,
    val compactGutter: Dp = 16.dp,
    val mediumColumns: Int = 8,
    val mediumOuterMargin: Dp = 24.dp,
    val mediumGutter: Dp = 24.dp,
    val expandedColumns: Int = 12,
    val expandedMinimumOuterMargin: Dp = 32.dp,
    val expandedGutter: Dp = 24.dp,
    val expandedMaximumBodyWidth: Dp = 1_200.dp,
    val readingMaximumWidth: Dp = 720.dp,
)

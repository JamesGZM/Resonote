package com.resonote.core.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class ResonoteSpacing internal constructor(
    val space0: Dp = 0.dp,
    val space1: Dp = 4.dp,
    val space2: Dp = 8.dp,
    val space3: Dp = 12.dp,
    val space4: Dp = 16.dp,
    val space6: Dp = 24.dp,
    val space8: Dp = 32.dp,
    val space10: Dp = 40.dp,
    val space12: Dp = 48.dp,
    val space16: Dp = 64.dp,
)

@Immutable
data class ResonoteBorders internal constructor(
    val hairline: Dp = 1.dp,
    val strong: Dp = 2.dp,
    val focusIndicatorWidth: Dp = 2.dp,
    val focusIndicatorOffset: Dp = 2.dp,
)

@Immutable
data class ResonoteTouchTargets internal constructor(
    val minimum: Dp = 48.dp,
)

@Immutable
data class ResonoteIconTokens internal constructor(
    val small: Dp = 20.dp,
    val default: Dp = 24.dp,
    val large: Dp = 40.dp,
    val display: Dp = 48.dp,
    val touchTarget: Dp = 48.dp,
    val viewport: Dp = 24.dp,
    val liveArea: Dp = 20.dp,
    val maximumOpticalShift: Dp = 1.dp,
)

@Immutable
data class ResonoteArtworkTokens internal constructor(
    val aspectRatio: Float = 1f,
    val contentScale: ContentScale = ContentScale.Crop,
    val alignment: Alignment = Alignment.Center,
    val overlayInset: Dp = 8.dp,
    val loadingContainerRole: ResonoteSemanticColorRole = ResonoteSemanticColorRole.SURFACE_CONTAINER_HIGH,
    val missingContainerRole: ResonoteSemanticColorRole = ResonoteSemanticColorRole.SURFACE_CONTAINER_HIGHEST,
    val missingContentRole: ResonoteSemanticColorRole = ResonoteSemanticColorRole.ON_SURFACE_VARIANT,
    val errorContainerRole: ResonoteSemanticColorRole = ResonoteSemanticColorRole.SURFACE_CONTAINER_HIGHEST,
    val errorContentRole: ResonoteSemanticColorRole = ResonoteSemanticColorRole.ON_SURFACE_VARIANT,
)

package com.resonote.core.designsystem.tokens

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class ResonoteSystemColors internal constructor(
    val shadow: Color = Color.Black,
    val onScrim: Color = Color.White,
    val mediaCanvas: Color = Color.Black,
    val onMediaCanvas: Color = Color.White,
)

@Immutable
data class ResonoteArtworkShapes internal constructor(
    val standard: Shape = RoundedCornerShape(12.dp),
    val compact: Shape = RoundedCornerShape(8.dp),
    val hero: Shape = RoundedCornerShape(16.dp),
)

@Immutable
data class ResonoteExtendedShapes internal constructor(val none: Shape = RectangleShape, val full: Shape = CircleShape)

internal val LocalResonoteSpacing = staticCompositionLocalOf { ResonoteSpacing() }
internal val LocalResonoteBorders = staticCompositionLocalOf { ResonoteBorders() }
internal val LocalResonoteTouchTargets = staticCompositionLocalOf { ResonoteTouchTargets() }
internal val LocalResonoteElevation = staticCompositionLocalOf { ResonoteElevation() }
internal val LocalResonoteLayout = staticCompositionLocalOf { ResonoteLayoutTokens() }
internal val LocalResonoteIcons = staticCompositionLocalOf { ResonoteIconTokens() }
internal val LocalResonoteArtwork = staticCompositionLocalOf { ResonoteArtworkTokens() }
internal val LocalResonoteArtworkShapes = staticCompositionLocalOf { ResonoteArtworkShapes() }
internal val LocalResonoteExtendedShapes = staticCompositionLocalOf { ResonoteExtendedShapes() }
internal val LocalResonoteMotion = staticCompositionLocalOf { ResonoteMotionScheme.Standard }
internal val LocalResonoteStateLayers = staticCompositionLocalOf { ResonoteStateLayers() }
internal val LocalResonoteSystemColors = staticCompositionLocalOf { ResonoteSystemColors() }

object ResonoteTokens {
    val spacing: ResonoteSpacing
        @Composable @ReadOnlyComposable
        get() = LocalResonoteSpacing.current
    val borders: ResonoteBorders
        @Composable @ReadOnlyComposable
        get() = LocalResonoteBorders.current
    val touchTargets: ResonoteTouchTargets
        @Composable @ReadOnlyComposable
        get() = LocalResonoteTouchTargets.current
    val elevation: ResonoteElevation
        @Composable @ReadOnlyComposable
        get() = LocalResonoteElevation.current
    val layout: ResonoteLayoutTokens
        @Composable @ReadOnlyComposable
        get() = LocalResonoteLayout.current
    val icons: ResonoteIconTokens
        @Composable @ReadOnlyComposable
        get() = LocalResonoteIcons.current
    val artwork: ResonoteArtworkTokens
        @Composable @ReadOnlyComposable
        get() = LocalResonoteArtwork.current
    val artworkShapes: ResonoteArtworkShapes
        @Composable @ReadOnlyComposable
        get() = LocalResonoteArtworkShapes.current
    val shapes: ResonoteExtendedShapes
        @Composable @ReadOnlyComposable
        get() = LocalResonoteExtendedShapes.current
    val motion: ResonoteMotionScheme
        @Composable @ReadOnlyComposable
        get() = LocalResonoteMotion.current
    val stateLayers: ResonoteStateLayers
        @Composable @ReadOnlyComposable
        get() = LocalResonoteStateLayers.current
    val systemColors: ResonoteSystemColors
        @Composable @ReadOnlyComposable
        get() = LocalResonoteSystemColors.current
}

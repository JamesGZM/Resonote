package com.resonote.core.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class ResonoteElevationLevel(
    val tonal: Dp,
    val preferredSurfaceRole: ResonoteSemanticColorRole,
    val defaultShadow: Dp,
    val maximumShadow: Dp,
)

@Immutable
data class ResonoteElevation internal constructor(
    val level0: ResonoteElevationLevel = ResonoteElevationLevel(
        tonal = 0.dp,
        preferredSurfaceRole = ResonoteSemanticColorRole.SURFACE,
        defaultShadow = 0.dp,
        maximumShadow = 0.dp,
    ),
    val level1: ResonoteElevationLevel = ResonoteElevationLevel(
        tonal = 1.dp,
        preferredSurfaceRole = ResonoteSemanticColorRole.SURFACE_CONTAINER_LOW,
        defaultShadow = 0.dp,
        maximumShadow = 1.dp,
    ),
    val level2: ResonoteElevationLevel = ResonoteElevationLevel(
        tonal = 3.dp,
        preferredSurfaceRole = ResonoteSemanticColorRole.SURFACE_CONTAINER,
        defaultShadow = 0.dp,
        maximumShadow = 3.dp,
    ),
    val level3: ResonoteElevationLevel = ResonoteElevationLevel(
        tonal = 6.dp,
        preferredSurfaceRole = ResonoteSemanticColorRole.SURFACE_CONTAINER_HIGH,
        defaultShadow = 0.dp,
        maximumShadow = 6.dp,
    ),
    val level4: ResonoteElevationLevel = ResonoteElevationLevel(
        tonal = 8.dp,
        preferredSurfaceRole = ResonoteSemanticColorRole.SURFACE_CONTAINER_HIGHEST,
        defaultShadow = 0.dp,
        maximumShadow = 8.dp,
    ),
    val level5: ResonoteElevationLevel = ResonoteElevationLevel(
        tonal = 12.dp,
        preferredSurfaceRole = ResonoteSemanticColorRole.SURFACE_CONTAINER_HIGHEST,
        defaultShadow = 0.dp,
        maximumShadow = 12.dp,
    ),
)

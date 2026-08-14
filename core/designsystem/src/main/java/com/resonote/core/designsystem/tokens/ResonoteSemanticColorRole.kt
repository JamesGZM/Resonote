package com.resonote.core.designsystem.tokens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

enum class ResonoteSemanticColorRole {
    SURFACE,
    SURFACE_CONTAINER_LOW,
    SURFACE_CONTAINER,
    SURFACE_CONTAINER_HIGH,
    SURFACE_CONTAINER_HIGHEST,
    ON_SURFACE_VARIANT,
    ;

    val color: Color
        @Composable
        @ReadOnlyComposable
        get() = when (this) {
            SURFACE -> MaterialTheme.colorScheme.surface
            SURFACE_CONTAINER_LOW -> MaterialTheme.colorScheme.surfaceContainerLow
            SURFACE_CONTAINER -> MaterialTheme.colorScheme.surfaceContainer
            SURFACE_CONTAINER_HIGH -> MaterialTheme.colorScheme.surfaceContainerHigh
            SURFACE_CONTAINER_HIGHEST -> MaterialTheme.colorScheme.surfaceContainerHighest
            ON_SURFACE_VARIANT -> MaterialTheme.colorScheme.onSurfaceVariant
        }
}

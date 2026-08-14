package com.resonote.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ThemeColorSchemeTest {
    @Test
    fun lightSurfaceRolesPreserveMaterialHierarchy() {
        assertThat(ResonoteLightColorScheme.surfaceContainerLowest).isEqualTo(Color.White)
        assertThat(ResonoteLightColorScheme.surfaceContainerLow)
            .isNotEqualTo(ResonoteLightColorScheme.surfaceContainerLowest)
        assertThat(ResonoteLightColorScheme.surfaceContainer)
            .isNotEqualTo(ResonoteLightColorScheme.surfaceContainerLow)
        assertThat(ResonoteLightColorScheme.surface)
            .isNotEqualTo(ResonoteLightColorScheme.surfaceContainer)
    }

    @Test
    fun amoledKeepsBlackBaseAndDistinctContainers() {
        assertThat(ResonoteAmoledColorScheme.background).isEqualTo(Color.Black)
        assertThat(ResonoteAmoledColorScheme.surface).isEqualTo(Color.Black)
        assertThat(ResonoteAmoledColorScheme.surfaceContainer).isNotEqualTo(Color.Black)
        assertThat(ResonoteAmoledColorScheme.surfaceContainerHigh)
            .isNotEqualTo(ResonoteAmoledColorScheme.surfaceContainer)
    }

    @Test
    fun bodyAndPrimaryTextMeetNormalTextContrast() {
        listOf(
            ResonoteLightColorScheme.onSurface to ResonoteLightColorScheme.surface,
            ResonoteDarkColorScheme.onSurface to ResonoteDarkColorScheme.surface,
            ResonoteAmoledColorScheme.onSurface to ResonoteAmoledColorScheme.surface,
            ResonoteLightColorScheme.onPrimary to ResonoteLightColorScheme.primary,
            ResonoteDarkColorScheme.onPrimary to ResonoteDarkColorScheme.primary,
        ).forEach { (foreground, background) ->
            assertThat(contrastRatio(foreground, background)).isAtLeast(4.5f)
        }
    }
}

private fun contrastRatio(first: Color, second: Color): Float {
    val lighter = maxOf(first.luminance(), second.luminance())
    val darker = minOf(first.luminance(), second.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}

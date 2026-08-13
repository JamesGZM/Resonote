package com.resonote.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ThemeColorSchemeTest {
    @Test
    fun lightThemeKeepsChromeAndCardsWhiteOnTintedPageCanvas() {
        assertThat(ResonoteLightColorScheme.surface).isEqualTo(Color.White)
        assertThat(ResonoteLightColorScheme.surfaceContainerLow).isEqualTo(Color.White)
        assertThat(ResonoteLightColorScheme.background).isNotEqualTo(Color.White)
    }
}

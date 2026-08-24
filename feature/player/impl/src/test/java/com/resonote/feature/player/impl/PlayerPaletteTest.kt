package com.resonote.feature.player.impl

import android.graphics.Bitmap
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlayerPaletteTest {
    @Test
    fun softwareBitmapRemainsPaletteCompatible() {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)

        assertThat(bitmap.paletteCompatibleBitmap()).isSameInstanceAs(bitmap)
    }

    @Test
    fun paletteSelectsReadableContentForLightAndDarkSurfaces() {
        val dark = PlayerPalette.fromSeed(PlayerPaletteSeed("dark", "art", 0xFF061113.toInt(), 0xFFFFA35B.toInt()))
        val light = PlayerPalette.fromSeed(PlayerPaletteSeed("light", "art", 0xFFF5EDE3.toInt(), 0xFF9A3E00.toInt()))

        assertThat(ColorUtils.calculateContrast(dark.contentPrimary.toArgb(), dark.background.toArgb())).isAtLeast(4.5)
        assertThat(
            ColorUtils.calculateContrast(light.contentPrimary.toArgb(), light.background.toArgb()),
        ).isAtLeast(4.5)
        assertThat(ColorUtils.calculateContrast(dark.contentOnAccent.toArgb(), dark.accent.toArgb())).isAtLeast(4.5)
    }
}

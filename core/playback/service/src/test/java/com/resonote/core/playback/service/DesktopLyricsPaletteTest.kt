package com.resonote.core.playback.service

import androidx.core.graphics.ColorUtils
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DesktopLyricsPaletteTest {
    @Test
    fun extractedColorsChooseReadableContent() {
        val dark = DesktopLyricsPalette.fromColors(0xFF061113.toInt(), 0xFFFFA35B.toInt())
        val light = DesktopLyricsPalette.fromColors(0xFFF5EDE3.toInt(), 0xFF9A3E00.toInt())

        assertThat(ColorUtils.calculateContrast(dark.onSurfaceArgb, dark.surfaceArgb)).isAtLeast(4.5)
        assertThat(ColorUtils.calculateContrast(light.onSurfaceArgb, light.surfaceArgb)).isAtLeast(4.5)
        assertThat(ColorUtils.calculateContrast(dark.onAccentArgb, dark.accentArgb)).isAtLeast(4.5)
    }

    @Test
    fun paletteInterpolationMovesDirectlyBetweenCurrentAndTargetColors() {
        val from = DesktopLyricsPalette.fromColors(0xFF101010.toInt(), 0xFFFF0000.toInt())
        val to = DesktopLyricsPalette.fromColors(0xFFF0F0F0.toInt(), 0xFF0000FF.toInt())

        assertThat(interpolateDesktopLyricsPalette(from, to, 0f)).isEqualTo(from)
        assertThat(interpolateDesktopLyricsPalette(from, to, 1f)).isEqualTo(to)
        assertThat(interpolateDesktopLyricsPalette(from, to, 0.5f).accentArgb)
            .isEqualTo(ColorUtils.blendARGB(from.accentArgb, to.accentArgb, 0.5f))
    }
}

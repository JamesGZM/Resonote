package com.resonote.core.datastore

import com.google.common.truth.Truth.assertThat
import com.resonote.core.datastore.proto.LyricsPreferences
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class LyricsPreferencesSerializerTest {
    @Test
    fun desktopLyricsStyleRoundTrips() = runTest {
        val stored = LyricsPreferences(
            desktopLyricsSurfaceOpacity = 72,
            desktopLyricsBackgroundColorArgb = 0xFF102030.toInt(),
            desktopLyricsForegroundColorArgb = 0xFFF0A040.toInt(),
            desktopLyricsShadowColorArgb = 0xFF245B83.toInt(),
            desktopLyricsShadowOffsetXDp = -2f,
            desktopLyricsShadowOffsetYDp = 3f,
            desktopLyricsShadowBlurRadiusDp = 7f,
            desktopLyricsWidthPercent = 80,
            desktopLyricsFontSizeSp = 32,
            desktopLyricsOutlineColorArgb = 0xFFEEDDCC.toInt(),
            desktopLyricsOutlineWidthDp = 2.5f,
            desktopLyricsStyleSet = true,
        )
        val output = ByteArrayOutputStream()

        LyricsPreferencesSerializer.writeTo(stored, output)

        assertThat(LyricsPreferencesSerializer.readFrom(ByteArrayInputStream(output.toByteArray())))
            .isEqualTo(stored)
    }

    @Test
    fun legacyPayloadUsesNewDesktopStyleDefaults() = runTest {
        val output = ByteArrayOutputStream()
        LyricsPreferences(desktopLyricsEnabled = true).writeTo(output)

        val restored = LyricsPreferencesSerializer.readFrom(ByteArrayInputStream(output.toByteArray()))

        assertThat(restored.desktopLyricsShadowColorArgb).isEqualTo(0xFF000000.toInt())
        assertThat(restored.desktopLyricsBackgroundColorArgb).isEqualTo(0xFFFFFFFF.toInt())
        assertThat(restored.desktopLyricsForegroundColorArgb).isEqualTo(0xFFAE2A4B.toInt())
        assertThat(restored.desktopLyricsWidthPercent).isEqualTo(100)
        assertThat(restored.desktopLyricsStyleSet).isFalse()
    }
}

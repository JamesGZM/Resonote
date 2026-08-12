package com.resonote.feature.player.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.playback.PlaybackFormat
import org.junit.Test

class PlaybackFormatLabelTest {
    @Test
    fun localFormatUsesActualTechnicalMetadata() {
        val format = PlaybackFormat.Local(
            mimeType = "audio/flac",
            extension = "flac",
            sampleRateHz = 96_000,
            bitDepth = 24,
            bitrateBitsPerSecond = 2_304_000,
        )

        assertThat(format.badgeLabel()).isEqualTo("FLAC · 96 kHz · 24-bit · 2304 kbps")
    }

    @Test
    fun localFormatFallsBackToMimeSubtypeAndKeepsFractionalSampleRate() {
        val format = PlaybackFormat.Local(
            mimeType = "audio/wav",
            extension = null,
            sampleRateHz = 44_100,
            bitDepth = 16,
            bitrateBitsPerSecond = null,
        )

        assertThat(format.badgeLabel()).isEqualTo("WAV · 44.1 kHz · 16-bit")
    }
}

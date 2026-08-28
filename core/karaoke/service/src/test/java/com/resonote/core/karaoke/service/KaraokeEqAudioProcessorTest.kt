package com.resonote.core.karaoke.service

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class KaraokeEqAudioProcessorTest {
    @Test
    fun flatEq_preservesPcmSamples() {
        val processor = KaraokeEqAudioProcessor(0f, 0f, 0f)
        processor.configure(AudioProcessor.AudioFormat(48_000, 1, C.ENCODING_PCM_16BIT))
        processor.flush()
        val samples = shortArrayOf(-12_000, -1_000, 0, 1_000, 12_000)
        val input = samples.toBuffer()

        processor.queueInput(input)

        assertThat(processor.outputSamples()).containsExactly(*samples.toTypedArray()).inOrder()
    }

    @Test
    fun lowBoost_increasesSteadyLowFrequencyEnergy() {
        val processor = KaraokeEqAudioProcessor(6f, 0f, 0f)
        processor.configure(AudioProcessor.AudioFormat(48_000, 1, C.ENCODING_PCM_16BIT))
        processor.flush()
        val samples = ShortArray(4_000) { 1_000 }

        processor.queueInput(samples.toBuffer())

        assertThat(processor.outputSamples().last()).isGreaterThan(1_700)
    }

    private fun ShortArray.toBuffer(): ByteBuffer = ByteBuffer.allocate(size * Short.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .apply {
            forEach(::putShort)
            flip()
        }

    private fun KaraokeEqAudioProcessor.outputSamples(): List<Short> {
        val buffer = output.order(ByteOrder.nativeOrder())
        return buildList { while (buffer.remaining() >= Short.SIZE_BYTES) add(buffer.short) }
    }
}

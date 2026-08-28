package com.resonote.core.playback.service

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ResonoteEqualizerAudioProcessorTest {
    @Test
    fun disabledProcessorPassesSamplesThrough() {
        val processor = configuredProcessor()
        val samples = shortArrayOf(100, -200, 300, -400)

        val input = samples.toBuffer()
        processor.queueInput(input)
        val reusedOutputView = processor.output.duplicate()
        processor.queueInput(reusedOutputView)

        assertThat(processor.outputSamples()).containsExactlyElementsIn(samples.asList()).inOrder()
    }

    @Test
    fun enabledProcessorChangesAudioWithoutSystemEffects() {
        val processor = configuredProcessor().apply { update(true, 12, 0, 0) }
        val samples = ShortArray(64) { 1_000 }

        processor.queueInput(samples.toBuffer())

        assertThat(processor.outputSamples()).isNotEqualTo(samples.asList())
    }

    private fun configuredProcessor() = ResonoteEqualizerAudioProcessor().apply {
        configure(AudioProcessor.AudioFormat(48_000, 1, C.ENCODING_PCM_16BIT))
        flush()
    }

    private fun ShortArray.toBuffer(): ByteBuffer =
        ByteBuffer.allocateDirect(size * Short.SIZE_BYTES).order(ByteOrder.nativeOrder()).apply {
            forEach(::putShort)
            flip()
        }

    private fun ResonoteEqualizerAudioProcessor.outputSamples(): List<Short> {
        val output = output.order(ByteOrder.nativeOrder())
        return buildList {
            while (output.remaining() >= Short.SIZE_BYTES) add(output.short)
        }
    }
}

package com.resonote.feature.recognition.impl

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class RecognitionPcmAnalyzerTest {
    @Test
    fun silenceProducesZeroEnergyAndFlatWaveform() {
        val analysis = analyzeRecognitionPcm(ShortArray(96).toPcmBytes(), byteCount = 192)

        assertThat(analysis.rmsAmplitude).isEqualTo(0f)
        assertThat(analysis.waveform).containsExactlyElementsIn(List(12) { 0f }).inOrder()
    }

    @Test
    fun extremaKeepTheirOrderWithinEachTimeWindow() {
        val samples = shortArrayOf(0, 1_000, 3_000, -2_000, -4_000, 0)

        val waveform = analyzeRecognitionPcm(samples.toPcmBytes(), samples.size * 2, pointCount = 2).waveform

        assertThat(waveform).hasSize(2)
        assertThat(waveform[0]).isGreaterThan(0f)
        assertThat(waveform[1]).isLessThan(0f)
        assertThat(waveform[0]).isWithin(0.0001f).of(3_333.3333f / 32_768f)
        assertThat(waveform[1]).isWithin(0.0001f).of(-3_666.6667f / 32_768f)
    }

    @Test
    fun sineWaveProducesAlternatingPositiveAndNegativeExtrema() {
        val samples = ShortArray(96) { index ->
            (sin(index * 2.0 * PI / 16.0) * 12_000).toInt().toShort()
        }

        val waveform = analyzeRecognitionPcm(samples.toPcmBytes(), samples.size * 2).waveform

        assertThat(waveform).hasSize(12)
        waveform.chunked(2).forEach { extrema ->
            assertThat(extrema.any { it > 0f }).isTrue()
            assertThat(extrema.any { it < 0f }).isTrue()
        }
    }

    @Test
    fun louderPcmProducesHigherEnergyAndLargerExtrema() {
        val quiet = sinePcm(amplitude = 2_000)
        val loud = sinePcm(amplitude = 12_000)

        val quietAnalysis = analyzeRecognitionPcm(quiet, quiet.size)
        val loudAnalysis = analyzeRecognitionPcm(loud, loud.size)

        assertThat(loudAnalysis.rmsAmplitude).isGreaterThan(quietAnalysis.rmsAmplitude)
        assertThat(loudAnalysis.waveform.maxOf { kotlin.math.abs(it) })
            .isGreaterThan(quietAnalysis.waveform.maxOf { kotlin.math.abs(it) })
    }

    private fun sinePcm(amplitude: Int) = ShortArray(96) { index ->
        (sin(index * 2.0 * PI / 16.0) * amplitude).toInt().toShort()
    }.toPcmBytes()

    private fun ShortArray.toPcmBytes() = ByteArray(size * Short.SIZE_BYTES).also { bytes ->
        forEachIndexed { index, sample ->
            bytes[index * 2] = sample.toInt().toByte()
            bytes[index * 2 + 1] = (sample.toInt() shr 8).toByte()
        }
    }
}

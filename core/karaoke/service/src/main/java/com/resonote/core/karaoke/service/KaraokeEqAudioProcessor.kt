package com.resonote.core.karaoke.service

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt

internal class KaraokeEqAudioProcessor(private val lowDb: Float, private val midDb: Float, private val highDb: Float) :
    BaseAudioProcessor() {
    private var lowState = DoubleArray(0)
    private var highCutState = DoubleArray(0)
    private var channelIndex = 0

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun isActive(): Boolean = lowDb != 0f || midDb != 0f || highDb != 0f

    override fun onFlush() {
        lowState = DoubleArray(inputAudioFormat.channelCount)
        highCutState = DoubleArray(inputAudioFormat.channelCount)
        channelIndex = 0
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val output = replaceOutputBuffer(inputBuffer.remaining()).order(ByteOrder.nativeOrder())
        inputBuffer.order(ByteOrder.nativeOrder())
        val lowAlpha = filterAlpha(LOW_CUTOFF_HZ, inputAudioFormat.sampleRate)
        val highAlpha = filterAlpha(HIGH_CUTOFF_HZ, inputAudioFormat.sampleRate)
        val lowGain = dbToLinear(lowDb)
        val midGain = dbToLinear(midDb)
        val highGain = dbToLinear(highDb)
        while (inputBuffer.remaining() >= Short.SIZE_BYTES) {
            val channel = channelIndex % inputAudioFormat.channelCount
            val sample = inputBuffer.short.toDouble()
            lowState[channel] += lowAlpha * (sample - lowState[channel])
            highCutState[channel] += highAlpha * (sample - highCutState[channel])
            val low = lowState[channel]
            val high = sample - highCutState[channel]
            val mid = sample - low - high
            val mixed = low * lowGain + mid * midGain + high * highGain
            output.putShort(mixed.roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
            channelIndex += 1
        }
        output.flip()
    }

    private fun filterAlpha(cutoffHz: Double, sampleRate: Int): Double = 1.0 - exp(-2.0 * PI * cutoffHz / sampleRate)

    private fun dbToLinear(db: Float): Double = 10.0.pow(db / 20.0)

    private companion object {
        const val LOW_CUTOFF_HZ = 250.0
        const val HIGH_CUTOFF_HZ = 4_000.0
    }
}

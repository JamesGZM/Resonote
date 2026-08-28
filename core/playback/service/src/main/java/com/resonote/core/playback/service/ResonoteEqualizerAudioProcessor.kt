package com.resonote.core.playback.service

import android.annotation.SuppressLint
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
internal class ResonoteEqualizerAudioProcessor : BaseAudioProcessor() {
    @Volatile
    private var settings = Settings()
    private var lowState = DoubleArray(0)
    private var highCutState = DoubleArray(0)
    private var channelIndex = 0
    private var inputScratch = ByteArray(0)

    fun update(enabled: Boolean, lowDb: Int, midDb: Int, highDb: Int) {
        settings = Settings(
            enabled = enabled,
            lowDb = lowDb.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB),
            midDb = midDb.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB),
            highDb = highDb.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB),
        )
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    // Keep the processor in the playback chain so enabling or changing EQ works without rebuilding the player.
    @SuppressLint("MissingSuperCall")
    override fun isActive(): Boolean = true

    override fun onFlush() {
        lowState = DoubleArray(inputAudioFormat.channelCount)
        highCutState = DoubleArray(inputAudioFormat.channelCount)
        channelIndex = 0
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val inputSize = inputBuffer.remaining()
        if (inputScratch.size < inputSize) {
            inputScratch = ByteArray(inputSize)
        }
        inputBuffer.get(inputScratch, 0, inputSize)
        val output = replaceOutputBuffer(inputSize).order(ByteOrder.nativeOrder())
        val input = ByteBuffer.wrap(inputScratch, 0, inputSize).order(ByteOrder.nativeOrder())
        val current = settings
        if (!current.enabled) {
            output.put(inputScratch, 0, inputSize)
            output.flip()
            return
        }
        val lowAlpha = filterAlpha(LOW_CUTOFF_HZ, inputAudioFormat.sampleRate)
        val highAlpha = filterAlpha(HIGH_CUTOFF_HZ, inputAudioFormat.sampleRate)
        val lowGain = dbToLinear(current.lowDb)
        val midGain = dbToLinear(current.midDb)
        val highGain = dbToLinear(current.highDb)
        while (input.remaining() >= Short.SIZE_BYTES) {
            val channel = channelIndex % inputAudioFormat.channelCount
            val sample = input.short.toDouble()
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

    private fun dbToLinear(db: Int): Double = 10.0.pow(db / 20.0)

    private data class Settings(
        val enabled: Boolean = false,
        val lowDb: Int = 0,
        val midDb: Int = 0,
        val highDb: Int = 0,
    )

    private companion object {
        const val MIN_GAIN_DB = -12
        const val MAX_GAIN_DB = 12
        const val LOW_CUTOFF_HZ = 250.0
        const val HIGH_CUTOFF_HZ = 4_000.0
    }
}

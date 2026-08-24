package com.resonote.feature.recognition.impl

import kotlin.math.sqrt

internal data class RecognitionPcmAnalysis(val rmsAmplitude: Float, val waveform: List<Float>)

/** Reduces one PCM16 frame without discarding the time order of its positive and negative peaks. */
internal fun analyzeRecognitionPcm(pcm: ByteArray, byteCount: Int, pointCount: Int = 12): RecognitionPcmAnalysis {
    require(pointCount > 0) { "pointCount must be positive" }
    val readableByteCount = byteCount.coerceIn(0, pcm.size) and -Short.SIZE_BYTES
    val sampleCount = readableByteCount / Short.SIZE_BYTES
    if (sampleCount == 0) return RecognitionPcmAnalysis(0f, emptyList())

    val samples = IntArray(sampleCount)
    var sampleSum = 0L
    for (sampleIndex in 0 until sampleCount) {
        val byteIndex = sampleIndex * Short.SIZE_BYTES
        val sample =
            (
                (pcm[byteIndex].toInt() and 0xFF) or
                    (pcm[byteIndex + 1].toInt() shl 8)
                ).toShort().toInt()
        samples[sampleIndex] = sample
        sampleSum += sample
    }

    val dcOffset = sampleSum.toDouble() / sampleCount
    var sumSquares = 0.0
    samples.forEach { sample ->
        val centered = sample - dcOffset
        sumSquares += centered * centered
    }
    val rms = sqrt(sumSquares / sampleCount) / PCM_16_FULL_SCALE

    val windowCount = minOf((pointCount + 1) / 2, sampleCount)
    val waveform = ArrayList<Float>(minOf(pointCount, windowCount * 2))
    for (window in 0 until windowCount) {
        val startSample = window * sampleCount / windowCount
        val endSample = ((window + 1) * sampleCount / windowCount).coerceAtMost(sampleCount)
        var minimum = Double.POSITIVE_INFINITY
        var maximum = Double.NEGATIVE_INFINITY
        var minimumIndex = startSample
        var maximumIndex = startSample
        for (sampleIndex in startSample until endSample) {
            val centered = samples[sampleIndex] - dcOffset
            if (centered < minimum) {
                minimum = centered
                minimumIndex = sampleIndex
            }
            if (centered > maximum) {
                maximum = centered
                maximumIndex = sampleIndex
            }
        }

        val first = if (maximumIndex < minimumIndex) maximum else minimum
        val second = if (maximumIndex < minimumIndex) minimum else maximum
        waveform += (first / PCM_16_FULL_SCALE).toFloat().coerceIn(-1f, 1f)
        if (waveform.size < pointCount) {
            waveform += (second / PCM_16_FULL_SCALE).toFloat().coerceIn(-1f, 1f)
        }
    }

    return RecognitionPcmAnalysis(
        rmsAmplitude = (rms * RMS_DISPLAY_GAIN).coerceIn(0.0, 1.0).toFloat(),
        waveform = waveform,
    )
}

private const val PCM_16_FULL_SCALE = 32_768.0
private const val RMS_DISPLAY_GAIN = 4.5

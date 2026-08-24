package com.resonote.feature.recognition.impl

internal const val RECOGNITION_SAMPLE_RATE = 8_000
internal const val RECOGNITION_MAX_DURATION_MILLIS = 10_000L
internal const val RECOGNITION_MIN_DURATION_MILLIS = 1_000L

internal sealed interface RecognitionCaptureResult {
    data class Captured(val pcm: ByteArray) : RecognitionCaptureResult
    data object Failed : RecognitionCaptureResult
}

internal interface RecognitionRecorder {
    suspend fun capture(
        maxDurationMillis: Long = RECOGNITION_MAX_DURATION_MILLIS,
        onProgress: (elapsedMillis: Long, amplitude: Float, waveform: List<Float>) -> Unit,
    ): RecognitionCaptureResult

    fun stop()

    fun cancel()
}

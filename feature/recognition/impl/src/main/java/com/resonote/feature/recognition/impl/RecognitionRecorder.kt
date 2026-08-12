package com.resonote.feature.recognition.impl

internal const val RecognitionSampleRate = 8_000
internal const val RecognitionMaxDurationMillis = 10_000L
internal const val RecognitionMinDurationMillis = 1_000L

internal sealed interface RecognitionCaptureResult {
    data class Captured(val pcm: ByteArray) : RecognitionCaptureResult
    data object Failed : RecognitionCaptureResult
}

internal interface RecognitionRecorder {
    suspend fun capture(
        maxDurationMillis: Long = RecognitionMaxDurationMillis,
        onProgress: (elapsedMillis: Long) -> Unit,
    ): RecognitionCaptureResult

    fun stop()

    fun cancel()
}

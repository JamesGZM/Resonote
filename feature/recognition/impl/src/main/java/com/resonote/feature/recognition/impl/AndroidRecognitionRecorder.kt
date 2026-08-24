package com.resonote.feature.recognition.impl

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AndroidRecognitionRecorder @Inject constructor() : RecognitionRecorder {
    private val capturing = AtomicBoolean(false)
    private val recorderLock = Any()
    private var activeRecorder: AudioRecord? = null

    override suspend fun capture(
        maxDurationMillis: Long,
        onProgress: (elapsedMillis: Long, amplitude: Float, waveform: List<Float>) -> Unit,
    ): RecognitionCaptureResult = withContext(Dispatchers.IO) {
        require(maxDurationMillis > 0) { "maxDurationMillis must be positive" }
        val bytesPerSecond = RECOGNITION_SAMPLE_RATE * Short.SIZE_BYTES
        val maximumBytes = (bytesPerSecond * maxDurationMillis / 1_000L).toInt()
        val minimumBuffer = AudioRecord.getMinBufferSize(
            RECOGNITION_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minimumBuffer <= 0) return@withContext RecognitionCaptureResult.Failed

        val recorder = try {
            AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RECOGNITION_SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(minimumBuffer.coerceAtLeast(bytesPerSecond / 2))
                .build()
        } catch (_: IllegalArgumentException) {
            return@withContext RecognitionCaptureResult.Failed
        } catch (_: SecurityException) {
            return@withContext RecognitionCaptureResult.Failed
        }

        val output = ByteArrayOutputStream(maximumBytes)
        val buffer = ByteArray(minimumBuffer.coerceAtLeast(256).coerceAtMost(512))
        synchronized(recorderLock) { activeRecorder = recorder }
        capturing.set(true)
        var smoothedAmplitude = 0f
        try {
            recorder.startRecording()
            if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                return@withContext RecognitionCaptureResult.Failed
            }
            while (capturing.get() && output.size() < maximumBytes) {
                currentCoroutineContext().ensureActive()
                val requested = minOf(buffer.size, maximumBytes - output.size())
                val count = recorder.read(buffer, 0, requested, AudioRecord.READ_BLOCKING)
                if (count <= 0) return@withContext RecognitionCaptureResult.Failed
                output.write(buffer, 0, count)
                val analysis = analyzeRecognitionPcm(buffer, count)
                val measuredAmplitude = analysis.rmsAmplitude
                val smoothing = if (measuredAmplitude > smoothedAmplitude) 0.52f else 0.18f
                smoothedAmplitude += (measuredAmplitude - smoothedAmplitude) * smoothing
                onProgress(
                    output.size().toLong() * 1_000L / bytesPerSecond,
                    smoothedAmplitude,
                    analysis.waveform,
                )
            }
            RecognitionCaptureResult.Captured(output.toByteArray())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: IllegalStateException) {
            RecognitionCaptureResult.Failed
        } catch (_: SecurityException) {
            RecognitionCaptureResult.Failed
        } finally {
            capturing.set(false)
            synchronized(recorderLock) { activeRecorder = null }
            runCatching {
                if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder.stop()
            }
            recorder.release()
            buffer.fill(0)
            output.reset()
        }
    }

    override fun stop() {
        capturing.set(false)
    }

    override fun cancel() {
        capturing.set(false)
        synchronized(recorderLock) { activeRecorder }.stopSafely()
    }

    private fun AudioRecord?.stopSafely() {
        if (this == null) return
        runCatching { if (recordingState == AudioRecord.RECORDSTATE_RECORDING) stop() }
    }
}

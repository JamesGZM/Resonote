package com.resonote.feature.recognition.impl

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

@Singleton
internal class AndroidRecognitionRecorder @Inject constructor() : RecognitionRecorder {
    private val capturing = AtomicBoolean(false)
    private val recorderLock = Any()
    private var activeRecorder: AudioRecord? = null

    override suspend fun capture(
        maxDurationMillis: Long,
        onProgress: (elapsedMillis: Long) -> Unit,
    ): RecognitionCaptureResult = withContext(Dispatchers.IO) {
        require(maxDurationMillis > 0) { "maxDurationMillis must be positive" }
        val bytesPerSecond = RecognitionSampleRate * Short.SIZE_BYTES
        val maximumBytes = (bytesPerSecond * maxDurationMillis / 1_000L).toInt()
        val minimumBuffer = AudioRecord.getMinBufferSize(
            RecognitionSampleRate,
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
                        .setSampleRate(RecognitionSampleRate)
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
        val buffer = ByteArray(minimumBuffer.coerceAtLeast(1_024))
        synchronized(recorderLock) { activeRecorder = recorder }
        capturing.set(true)
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
                onProgress(output.size().toLong() * 1_000L / bytesPerSecond)
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

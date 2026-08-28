package com.resonote.core.karaoke.service

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

internal data class KaraokeCaptureSummary(val durationMillis: Long, val peakAmplitude: Int)

internal class KaraokeRecordingEngine @Inject constructor() {
    private val recording = AtomicBoolean(false)

    fun arm() {
        recording.set(true)
    }

    suspend fun record(path: String): KaraokeCaptureSummary? = withContext(Dispatchers.IO) {
        val minimumBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minimumBuffer <= 0) {
            recording.set(false)
            return@withContext null
        }
        val recorder = try {
            AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE_HZ)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(minimumBuffer.coerceAtLeast(SAMPLE_RATE_HZ))
                .build()
        } catch (_: IllegalArgumentException) {
            recording.set(false)
            return@withContext null
        } catch (_: SecurityException) {
            recording.set(false)
            return@withContext null
        }
        val buffer = ByteArray(minimumBuffer.coerceAtLeast(4_096))
        var totalBytes = 0L
        var peak = 0
        if (!recording.get()) {
            recorder.release()
            return@withContext null
        }
        try {
            recorder.startRecording()
            if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) return@withContext null
            FileOutputStream(File(path), false).use { output ->
                output.write(wavHeader(0))
                while (recording.get()) {
                    currentCoroutineContext().ensureActive()
                    val count = recorder.read(buffer, 0, buffer.size, AudioRecord.READ_NON_BLOCKING)
                    if (count == 0) {
                        delay(READ_RETRY_DELAY_MILLIS)
                        continue
                    }
                    if (count < 0) {
                        if (!recording.get()) break
                        return@withContext null
                    }
                    output.write(buffer, 0, count)
                    totalBytes += count
                    var index = 0
                    while (index + 1 < count) {
                        val sample = ((buffer[index + 1].toInt() shl 8) or (buffer[index].toInt() and 0xff)).toShort()
                        peak = maxOf(peak, kotlin.math.abs(sample.toInt()))
                        index += 2
                    }
                }
                output.fd.sync()
            }
            patchWavHeader(path, totalBytes)
            KaraokeCaptureSummary(totalBytes * 1_000L / BYTES_PER_SECOND, peak)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        } finally {
            recording.set(false)
            runCatching { if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder.stop() }
            recorder.release()
            buffer.fill(0)
        }
    }

    fun stop() {
        recording.set(false)
    }

    private fun wavHeader(dataSize: Long): ByteArray = java.nio.ByteBuffer.allocate(WAV_HEADER_BYTES)
        .order(java.nio.ByteOrder.LITTLE_ENDIAN)
        .apply {
            put("RIFF".encodeToByteArray())
            putInt((dataSize + 36).toInt())
            put("WAVEfmt ".encodeToByteArray())
            putInt(16)
            putShort(1)
            putShort(1)
            putInt(SAMPLE_RATE_HZ)
            putInt(BYTES_PER_SECOND)
            putShort(Short.SIZE_BYTES.toShort())
            putShort(16)
            put("data".encodeToByteArray())
            putInt(dataSize.toInt())
        }.array()

    private fun patchWavHeader(path: String, dataSize: Long) {
        RandomAccessFile(path, "rw").use { file ->
            file.seek(0)
            file.write(wavHeader(dataSize))
        }
    }

    private companion object {
        const val SAMPLE_RATE_HZ = 48_000
        const val BYTES_PER_SECOND = SAMPLE_RATE_HZ * Short.SIZE_BYTES
        const val WAV_HEADER_BYTES = 44
        const val READ_RETRY_DELAY_MILLIS = 5L
    }
}

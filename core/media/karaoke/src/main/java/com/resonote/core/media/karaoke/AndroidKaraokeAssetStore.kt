package com.resonote.core.media.karaoke

import android.content.Context
import android.net.Uri
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resumeWithException

@Singleton
internal class AndroidKaraokeAssetStore @Inject constructor(
    @ApplicationContext context: Context,
    private val callFactory: Call.Factory,
) : KaraokeAssetStore {
    private val root = File(context.filesDir, ROOT_DIRECTORY)
    private val availableBytes = { StatFs(context.filesDir.absolutePath).availableBytes }

    override suspend fun persistSource(
        projectId: String,
        assetId: String,
        sourceUri: String,
        extension: String?,
    ): KaraokeStoreResult<KaraokeStoredAsset> = withContext(Dispatchers.IO) {
        val safeProjectId = projectId.safeName() ?: return@withContext failure(KaraokeStoreFailure.StorageUnavailable)
        val safeAssetId = assetId.safeName() ?: return@withContext failure(KaraokeStoreFailure.StorageUnavailable)
        val source = runCatching { Uri.parse(sourceUri) }.getOrNull()
            ?: return@withContext failure(KaraokeStoreFailure.InvalidSource)
        if (source.scheme != "https" && source.scheme != "http" && source.scheme != "file") {
            return@withContext failure(KaraokeStoreFailure.InvalidSource)
        }
        val directory = File(root, "$PROJECTS_DIRECTORY/$safeProjectId/$ASSETS_DIRECTORY")
        val suffix = extension?.safeExtension() ?: DEFAULT_SOURCE_EXTENSION
        val destination = File(directory, "$safeAssetId.$suffix")
        val partial = File(directory, "$safeAssetId.part")
        try {
            if (!directory.mkdirs() && !directory.isDirectory) throw IOException()
            if (destination.exists() || partial.exists()) throw IOException()
            val requiredBytes = remoteContentLength(sourceUri).coerceAtLeast(MIN_SOURCE_ESTIMATE_BYTES)
            if (!hasFreeBytes(requiredBytes)) return@withContext failure(KaraokeStoreFailure.InsufficientStorage)
            val size = copySource(sourceUri, partial)
            if (size <= 0L) return@withContext failure(KaraokeStoreFailure.SourceUnavailable)
            atomicMove(partial, destination)
            KaraokeStoreResult.Success(KaraokeStoredAsset(destination.absolutePath, size))
        } catch (_: SecurityException) {
            failure(KaraokeStoreFailure.StorageUnavailable)
        } catch (_: IOException) {
            failure(KaraokeStoreFailure.SourceUnavailable)
        } finally {
            partial.delete()
        }
    }

    override suspend fun createRecordingFile(projectId: String, segmentId: String): KaraokeStoreResult<String> =
        withContext(Dispatchers.IO) {
            val safeProjectId = projectId.safeName()
                ?: return@withContext failure(KaraokeStoreFailure.StorageUnavailable)
            val safeSegmentId = segmentId.safeName()
                ?: return@withContext failure(KaraokeStoreFailure.StorageUnavailable)
            if (!hasFreeBytes(MIN_RECORDING_START_BYTES)) {
                return@withContext failure(KaraokeStoreFailure.InsufficientStorage)
            }
            val directory = File(root, "$PROJECTS_DIRECTORY/$safeProjectId/$VOCALS_DIRECTORY")
            try {
                if (!directory.mkdirs() && !directory.isDirectory) throw IOException()
                val file = File(directory, "$safeSegmentId.wav")
                if (file.exists() || !file.createNewFile()) throw IOException()
                KaraokeStoreResult.Success(file.absolutePath)
            } catch (_: IOException) {
                failure(KaraokeStoreFailure.StorageUnavailable)
            }
        }

    override suspend fun hasRecordingCapacity(expectedDurationMillis: Long): Boolean = withContext(Dispatchers.IO) {
        val pcmBytes = expectedDurationMillis.coerceAtLeast(0L) * SAMPLE_RATE_HZ * PCM_BYTES_PER_SAMPLE / 1_000L
        hasFreeBytes(pcmBytes * WORKING_SPACE_MULTIPLIER)
    }

    override suspend fun removeProject(projectId: String): KaraokeStoreResult<Unit> = withContext(Dispatchers.IO) {
        val safeProjectId = projectId.safeName() ?: return@withContext failure(KaraokeStoreFailure.StorageUnavailable)
        val directory = File(root, "$PROJECTS_DIRECTORY/$safeProjectId")
        try {
            if (!directory.exists() || directory.deleteRecursively()) {
                KaraokeStoreResult.Success(Unit)
            } else {
                failure(KaraokeStoreFailure.StorageUnavailable)
            }
        } catch (_: SecurityException) {
            failure(KaraokeStoreFailure.StorageUnavailable)
        }
    }

    private fun hasFreeBytes(requiredBytes: Long): Boolean = runCatching {
        val free = availableBytes()
        free > SPACE_RESERVE_BYTES && requiredBytes <= free - SPACE_RESERVE_BYTES
    }.getOrDefault(false)

    private suspend fun remoteContentLength(uri: String): Long {
        if (Uri.parse(uri).scheme == "file") return File(requireNotNull(Uri.parse(uri).path)).length()
        val request = Request.Builder().url(uri).head().build()
        return runCatching { callFactory.newCall(request).await().use { it.body?.contentLength() ?: -1L } }
            .getOrDefault(-1L)
    }

    private suspend fun copySource(uri: String, destination: File): Long {
        val parsed = Uri.parse(uri)
        val input = if (parsed.scheme == "file") {
            File(requireNotNull(parsed.path)).inputStream()
        } else {
            val response = callFactory.newCall(Request.Builder().url(uri).build()).await()
            if (!response.isSuccessful) {
                response.close()
                throw IOException("Source request failed")
            }
            response.body?.byteStream()?.let { stream -> response to stream }
                ?: run {
                    response.close()
                    throw IOException("Source response is empty")
                }
        }
        val closeable = if (input is Pair<*, *>) input.first as Response else null
        val stream = if (input is Pair<*, *>) input.second as java.io.InputStream else input as java.io.InputStream
        try {
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    coroutineContext.ensureActive()
                    val count = stream.read(buffer)
                    if (count < 0) break
                    if (count == 0) continue
                    output.write(buffer, 0, count)
                    total += count
                }
                output.fd.sync()
                return total
            }
        } finally {
            stream.close()
            closeable?.close()
        }
    }

    private fun atomicMove(source: File, destination: File) {
        try {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            if (!source.renameTo(destination)) throw IOException("Atomic move unavailable")
        }
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response) { _, cancelledResponse, _ -> cancelledResponse.close() }
                }
            },
        )
    }

    private fun String.safeName(): String? = takeIf { isNotBlank() && all { it.isLetterOrDigit() || it in "-_" } }
    private fun String.safeExtension(): String? = lowercase().takeIf {
        isNotBlank() && length <= 8 && all(Char::isLetterOrDigit)
    }
    private fun <T> failure(reason: KaraokeStoreFailure): KaraokeStoreResult<T> = KaraokeStoreResult.Failure(reason)

    private companion object {
        const val ROOT_DIRECTORY = "karaoke"
        const val PROJECTS_DIRECTORY = "projects"
        const val ASSETS_DIRECTORY = "assets"
        const val VOCALS_DIRECTORY = "vocals"
        const val DEFAULT_SOURCE_EXTENSION = "audio"
        const val SAMPLE_RATE_HZ = 48_000L
        const val PCM_BYTES_PER_SAMPLE = 2L
        const val WORKING_SPACE_MULTIPLIER = 2L
        const val SPACE_RESERVE_BYTES = 512L * 1024L * 1024L
        const val MIN_SOURCE_ESTIMATE_BYTES = 8L * 1024L * 1024L
        const val MIN_RECORDING_START_BYTES = 2L * 1024L * 1024L
    }
}

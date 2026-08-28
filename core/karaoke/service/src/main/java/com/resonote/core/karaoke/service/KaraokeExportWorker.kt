package com.resonote.core.karaoke.service

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.resonote.core.data.KaraokeRenderInput
import com.resonote.core.data.KaraokeRepository
import com.resonote.core.model.KaraokeProjectId
import com.resonote.core.model.KaraokeProjectStatus
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

@OptIn(UnstableApi::class)
internal class KaraokeExportWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val projectId = inputData.getString(PROJECT_ID)?.let(::KaraokeProjectId) ?: return Result.failure()
        val repository = EntryPointAccessors.fromApplication(
            applicationContext,
            KaraokeExportEntryPoint::class.java,
        ).karaokeRepository()
        val input = repository.renderInput(projectId) ?: return Result.failure()
        if (input.segments.isEmpty()) return Result.failure()
        repository.updateExportStatus(projectId, KaraokeProjectStatus.Exporting)
        val temp = File(applicationContext.cacheDir, "karaoke-export-${projectId.value}.m4a")
        temp.delete()
        val exported = runCatching { exportComposition(input, temp) }.getOrDefault(false)
        if (!exported) {
            temp.delete()
            repository.updateExportStatus(projectId, KaraokeProjectStatus.ExportFailed)
            return Result.failure()
        }
        val contentUri = publishToMediaStore(input, temp)
        temp.delete()
        return if (contentUri == null) {
            repository.updateExportStatus(projectId, KaraokeProjectStatus.ExportFailed)
            Result.failure()
        } else {
            repository.updateExportStatus(projectId, KaraokeProjectStatus.Exported, contentUri)
            Result.success()
        }
    }

    private suspend fun exportComposition(input: KaraokeRenderInput, destination: File): Boolean =
        withContext(Dispatchers.Main) {
            val composition = KaraokeCompositionFactory.create(input)
            suspendCancellableCoroutine { continuation ->
                lateinit var transformer: Transformer
                val listener = object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (continuation.isActive) continuation.resume(true)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException,
                    ) {
                        if (continuation.isActive) continuation.resume(false)
                    }
                }
                transformer = Transformer.Builder(applicationContext)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .addListener(listener)
                    .build()
                continuation.invokeOnCancellation { transformer.cancel() }
                transformer.start(composition, destination.absolutePath)
            }
        }

    private suspend fun publishToMediaStore(input: KaraokeRenderInput, source: File): String? =
        withContext(Dispatchers.IO) {
            val resolver = applicationContext.contentResolver
            val displayName = buildString {
                append(input.project.songTitle.safeFileName())
                append(" - ")
                append(input.project.artist.orEmpty().ifBlank { "Resonote" }.safeFileName())
                append(".m4a")
            }
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/Resonote/Karaoke")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
            try {
                resolver.openOutputStream(uri, "w")?.use { output -> source.inputStream().use { it.copyTo(output) } }
                    ?: error("MediaStore output is unavailable")
                resolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
                uri.toString()
            } catch (_: Exception) {
                resolver.delete(uri, null, null)
                null
            }
        }

    private fun String.safeFileName(): String = replace(Regex("[\\\\/:*?\"<>|]"), "_").take(80).ifBlank { "Karaoke" }

    companion object {
        const val PROJECT_ID = "karaoke_project_id"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface KaraokeExportEntryPoint {
    fun karaokeRepository(): KaraokeRepository
}

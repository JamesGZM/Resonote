@file:androidx.media3.common.util.UnstableApi

package com.resonote.core.playback.service

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.resonote.core.data.PlaybackPreferencesRepository
import com.resonote.core.data.SongPlaybackRepository
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.ResolveSongSourceResult
import com.resonote.core.model.ResolvedSongSource
import com.resonote.core.playback.MusicDownload
import com.resonote.core.playback.MusicDownloadController
import com.resonote.core.playback.MusicDownloadState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultMusicDownloadController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val store: PlaybackDownloadStore,
    private val songPlaybackRepository: SongPlaybackRepository,
    private val playbackPreferencesRepository: PlaybackPreferencesRepository,
) : MusicDownloadController,
    DownloadManager.Listener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableDownloads = MutableStateFlow<List<MusicDownload>>(emptyList())
    override val downloads: StateFlow<List<MusicDownload>> = mutableDownloads.asStateFlow()
    private val preparing = linkedMapOf<String, MusicDownload>()
    private var lastKnownDownloads = emptyList<Download>()
    private var refreshJob: Job? = null

    init {
        store.manager.addListener(this)
        refresh()
        scope.launch {
            while (isActive) {
                if (
                    mutableDownloads.value.any {
                        it.state == MusicDownloadState.Queued ||
                            it.state == MusicDownloadState.Downloading ||
                            it.state == MusicDownloadState.Removing
                    }
                ) {
                    refresh()
                }
                delay(PROGRESS_REFRESH_MILLIS)
            }
        }
    }

    override fun download(song: OnlineSong) {
        val id = downloadId(song.hash)
        if (mutableDownloads.value.any { it.id == id && it.state != MusicDownloadState.Failed }) return
        scope.launch {
            val quality = playbackPreferencesRepository.onlinePlaybackQuality.first()
            preparing[id] = MusicDownload(
                id = id,
                song = song,
                quality = quality,
                sourceUri = "",
                extension = null,
                state = MusicDownloadState.Preparing,
                progressPercent = null,
                bytesDownloaded = 0,
                totalBytes = null,
                updatedAtEpochMillis = System.currentTimeMillis(),
            )
            publish(lastKnownDownloads)
            resolveAndEnqueue(id, song, quality)
        }
    }

    override fun pause(id: String) {
        DownloadService.sendSetStopReason(context, ResonoteDownloadService::class.java, id, STOP_REASON_PAUSED, false)
    }

    override fun resume(id: String) {
        DownloadService.sendSetStopReason(
            context,
            ResonoteDownloadService::class.java,
            id,
            Download.STOP_REASON_NONE,
            false,
        )
    }

    override fun retry(id: String) {
        val item = mutableDownloads.value.firstOrNull { it.id == id } ?: return
        scope.launch {
            preparing[id] = item.copy(
                state = MusicDownloadState.Preparing,
                progressPercent = null,
                updatedAtEpochMillis = System.currentTimeMillis(),
            )
            publish(lastKnownDownloads)
            resolveAndEnqueue(id, item.song, item.quality)
        }
    }

    override fun remove(id: String) {
        if (preparing.remove(id) != null) publish(lastKnownDownloads)
        DownloadService.sendRemoveDownload(context, ResonoteDownloadService::class.java, id, false)
    }

    override fun pauseAll() {
        DownloadService.sendPauseDownloads(context, ResonoteDownloadService::class.java, false)
    }

    override fun resumeAll() {
        mutableDownloads.value
            .filter { it.state == MusicDownloadState.Paused }
            .forEach { resume(it.id) }
        DownloadService.sendResumeDownloads(context, ResonoteDownloadService::class.java, false)
    }

    override fun completedSource(songHash: String, quality: OnlinePlaybackQuality?): ResolvedSongSource? =
        mutableDownloads.value
            .firstOrNull { item ->
                item.song.hash == songHash &&
                    item.state == MusicDownloadState.Completed &&
                    (quality == null || quality == item.quality)
            }
            ?.completedPlaybackSource()

    override fun onInitialized(downloadManager: DownloadManager) = refresh()

    override fun onDownloadChanged(downloadManager: DownloadManager, download: Download, finalException: Exception?) {
        preparing.remove(download.request.id)
        refresh()
    }

    override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
        preparing.remove(download.request.id)
        refresh()
    }

    override fun onDownloadsPausedChanged(downloadManager: DownloadManager, downloadsPaused: Boolean) = refresh()

    private suspend fun resolveAndEnqueue(id: String, song: OnlineSong, quality: OnlinePlaybackQuality) {
        val source = try {
            when (val result = songPlaybackRepository.resolveSource(song, quality)) {
                is ResolveSongSourceResult.Resolved -> result.source.takeUnless { it.isPreview }
                is ResolveSongSourceResult.Unavailable,
                is ResolveSongSourceResult.Failed,
                -> null
            }
        } catch (cancelled: CancellationException) {
            preparing.remove(id)
            publish(lastKnownDownloads)
            throw cancelled
        } catch (_: Exception) {
            null
        }
        if (source == null) {
            preparing[id] = checkNotNull(preparing[id]).copy(
                state = MusicDownloadState.Failed,
                updatedAtEpochMillis = System.currentTimeMillis(),
            )
            publish(lastKnownDownloads)
            return
        }
        val metadata = MusicDownloadMetadata(song, quality, source.extension)
        val request = DownloadRequest.Builder(id, source.uri.toUri())
            .setCustomCacheKey(id)
            .setData(MusicDownloadMetadataCodec.encode(metadata))
            .build()
        preparing.remove(id)
        DownloadService.sendAddDownload(context, ResonoteDownloadService::class.java, request, false)
        refresh()
    }

    private fun refresh() {
        if (refreshJob?.isActive == true) return
        val active = store.manager.currentDownloads.toList()
        refreshJob = scope.launch {
            val terminal = withContext(Dispatchers.IO) {
                runCatching {
                    buildList {
                        store.manager.downloadIndex.getDownloads(
                            Download.STATE_COMPLETED,
                            Download.STATE_FAILED,
                        ).use { cursor ->
                            while (cursor.moveToNext()) add(cursor.download)
                        }
                    }
                }.getOrDefault(emptyList())
            }
            lastKnownDownloads = (active + terminal).distinctBy { it.request.id }
            publish(lastKnownDownloads)
        }
    }

    private fun publish(downloads: List<Download>) {
        val mapped = downloads.mapNotNull { it.toExternalModel() }
        mutableDownloads.value = (preparing.values + mapped)
            .distinctBy(MusicDownload::id)
            .sortedWith(
                compareBy<MusicDownload> { it.state == MusicDownloadState.Completed }
                    .thenByDescending(MusicDownload::updatedAtEpochMillis),
            )
    }

    private fun Download.toExternalModel(): MusicDownload? {
        val metadata = MusicDownloadMetadataCodec.decode(request.data) ?: return null
        return MusicDownload(
            id = request.id,
            song = metadata.song,
            quality = metadata.quality,
            sourceUri = request.uri.toString(),
            extension = metadata.extension,
            state = when (state) {
                Download.STATE_QUEUED -> MusicDownloadState.Queued
                Download.STATE_STOPPED -> MusicDownloadState.Paused
                Download.STATE_DOWNLOADING,
                Download.STATE_RESTARTING,
                -> MusicDownloadState.Downloading
                Download.STATE_COMPLETED -> MusicDownloadState.Completed
                Download.STATE_FAILED -> MusicDownloadState.Failed
                Download.STATE_REMOVING -> MusicDownloadState.Removing
                else -> MusicDownloadState.Failed
            },
            progressPercent = percentDownloaded.takeUnless { it == C.PERCENTAGE_UNSET.toFloat() },
            bytesDownloaded = bytesDownloaded.coerceAtLeast(0),
            totalBytes = contentLength.takeIf { it > 0 },
            updatedAtEpochMillis = updateTimeMs,
        )
    }

    private companion object {
        const val STOP_REASON_PAUSED = 1
        const val PROGRESS_REFRESH_MILLIS = 750L

        fun downloadId(songHash: String) = "$DOWNLOAD_ID_PREFIX$songHash"
    }
}

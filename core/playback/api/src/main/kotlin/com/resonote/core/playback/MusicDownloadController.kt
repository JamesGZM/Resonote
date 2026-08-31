package com.resonote.core.playback

import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.ResolvedSongSource
import kotlinx.coroutines.flow.StateFlow

enum class MusicDownloadState {
    Preparing,
    Queued,
    Downloading,
    Paused,
    Completed,
    Failed,
    Removing,
}

data class MusicDownload(
    val id: String,
    val song: OnlineSong,
    val quality: OnlinePlaybackQuality,
    val sourceUri: String,
    val extension: String?,
    val state: MusicDownloadState,
    val progressPercent: Float?,
    val bytesDownloaded: Long,
    val totalBytes: Long?,
    val updatedAtEpochMillis: Long,
) {
    val isActive: Boolean
        get() = state == MusicDownloadState.Preparing ||
            state == MusicDownloadState.Queued ||
            state == MusicDownloadState.Downloading ||
            state == MusicDownloadState.Paused ||
            state == MusicDownloadState.Removing

    fun completedPlaybackSource(): ResolvedSongSource? = takeIf {
        state == MusicDownloadState.Completed && sourceUri.isNotBlank()
    }?.let {
        ResolvedSongSource(
            uri = sourceUri,
            durationMillis = song.durationMillis,
            extension = extension,
            cacheKey = id,
            isOffline = true,
        )
    }
}

interface MusicDownloadController {
    val downloads: StateFlow<List<MusicDownload>>

    fun download(song: OnlineSong)

    fun pause(id: String)

    fun resume(id: String)

    fun retry(id: String)

    fun remove(id: String)

    fun pauseAll()

    fun resumeAll()

    fun completedSource(songHash: String, quality: OnlinePlaybackQuality? = null): ResolvedSongSource?
}

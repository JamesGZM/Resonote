package com.resonote.feature.local.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.OnlineSong
import com.resonote.core.playback.MusicDownload
import com.resonote.core.playback.MusicDownloadState
import com.resonote.core.playback.PlaybackOrigin
import org.junit.Test

class LocalMusicUiStateTest {
    @Test
    fun completedDownloadsJoinLibraryWhileActiveTasksOnlyCountAsStatus() {
        val completed = download("completed", MusicDownloadState.Completed)
        val active = download("active", MusicDownloadState.Downloading)
        val state = LocalMusicUiState(
            downloads = listOf(completed, active),
            isLoading = false,
        )

        assertThat(state.libraryItems).hasSize(1)
        assertThat(state.activeDownloadCount).isEqualTo(1)
        val item = state.libraryItems.single() as LocalLibraryItem.Downloaded
        assertThat((item.toPlaybackItem().origin as PlaybackOrigin.Online).song.hash).isEqualTo("completed")
        assertThat(item.toPlaybackItem().resolvedSource?.cacheKey).isEqualTo("download:completed")
        assertThat(item.toPlaybackItem().resolvedSource?.isOffline).isTrue()
    }

    private fun download(hash: String, state: MusicDownloadState) = MusicDownload(
        id = "download:$hash",
        song = OnlineSong(
            hash = hash,
            title = hash,
            artist = "artist",
            coverUrl = null,
            albumId = null,
            albumAudioId = null,
            durationMillis = 180_000,
            quality = AudioQuality.Lossless,
            vip = false,
        ),
        quality = OnlinePlaybackQuality.Lossless,
        sourceUri = "https://example.test/$hash.flac",
        extension = "flac",
        state = state,
        progressPercent = null,
        bytesDownloaded = 1_024,
        totalBytes = 1_024,
        updatedAtEpochMillis = 1_000,
    )
}

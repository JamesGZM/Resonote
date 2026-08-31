package com.resonote.core.playback.service

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.CloudRepository
import com.resonote.core.data.LocalMediaRepository
import com.resonote.core.data.SongPlaybackRepository
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.CloudPage
import com.resonote.core.model.CloudTrack
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.LocalMedia
import com.resonote.core.model.LocalMediaDeleteResult
import com.resonote.core.model.LocalMediaDuplicateAction
import com.resonote.core.model.LocalMediaId
import com.resonote.core.model.LocalMediaImportResult
import com.resonote.core.model.LocalMediaPlaybackSource
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.ResolveSongSourceResult
import com.resonote.core.model.ResolvedSongSource
import com.resonote.core.playback.MusicDownload
import com.resonote.core.playback.MusicDownloadController
import com.resonote.core.playback.PlaybackItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PlaybackSourceResolverTest {
    @Test
    fun onlineAndCloudItemsUseTheirMatchingRepositories() = runTest {
        val songRepository = FakeSongRepository()
        val cloudRepository = FakeCloudRepository()
        val resolver = PlaybackSourceResolver(songRepository, cloudRepository, FakeLocalRepository())
        val cloudTrack = cloud("cloud")

        resolver.resolve(PlaybackItem(song("online")))
        resolver.resolve(PlaybackItem(cloudTrack))

        assertThat(songRepository.resolvedHashes).containsExactly("online")
        assertThat(cloudRepository.resolvedHashes).containsExactly("cloud")
    }

    @Test
    fun preResolvedItemSkipsBothRepositories() = runTest {
        val songRepository = FakeSongRepository()
        val cloudRepository = FakeCloudRepository()
        val resolver = PlaybackSourceResolver(songRepository, cloudRepository, FakeLocalRepository())
        val source = ResolvedSongSource("https://media.example/song.mp3", 180_000, "mp3")

        val result = resolver.resolve(PlaybackItem(song("ready"), resolvedSource = source))

        assertThat((result as ResolveSongSourceResult.Resolved).source).isEqualTo(source)
        assertThat(songRepository.resolvedHashes).isEmpty()
        assertThat(cloudRepository.resolvedHashes).isEmpty()
    }

    @Test
    fun onlineItemPassesItsPerSongQualityOverride() = runTest {
        val songRepository = FakeSongRepository()
        val resolver = PlaybackSourceResolver(songRepository, FakeCloudRepository(), FakeLocalRepository())

        resolver.resolve(
            PlaybackItem(song("lossless")).copy(
                onlineQualityOverride = OnlinePlaybackQuality.Lossless,
            ),
        )

        assertThat(songRepository.qualityOverrides).containsExactly(OnlinePlaybackQuality.Lossless)
    }

    @Test
    fun completedDownloadSkipsNetworkResolution() = runTest {
        val songRepository = FakeSongRepository()
        val downloadedSource = ResolvedSongSource(
            uri = "https://media.example/offline.flac",
            durationMillis = 180_000,
            extension = "flac",
            cacheKey = "download:offline",
            isOffline = true,
        )
        val resolver = PlaybackSourceResolver(
            songRepository,
            FakeCloudRepository(),
            FakeLocalRepository(),
            FakeDownloadController(downloadedSource),
        )

        val result = resolver.resolve(PlaybackItem(song("offline")))

        assertThat((result as ResolveSongSourceResult.Resolved).source).isEqualTo(downloadedSource)
        assertThat(songRepository.resolvedHashes).isEmpty()
    }

    @Test
    fun localItemUsesPersistentPrivateSourceWithoutNetworkResolution() = runTest {
        val songRepository = FakeSongRepository()
        val cloudRepository = FakeCloudRepository()
        val localRepository = FakeLocalRepository(localSource())
        val resolver = PlaybackSourceResolver(songRepository, cloudRepository, localRepository)

        val result = resolver.resolve(PlaybackItem(localMedia()))

        assertThat((result as ResolveSongSourceResult.Resolved).source).isEqualTo(
            ResolvedSongSource("file:/private/signals.flac", 180_000, "flac", isOffline = true),
        )
        assertThat(localRepository.resolvedIds).containsExactly(LocalMediaId("local-id"))
        assertThat(songRepository.resolvedHashes).isEmpty()
        assertThat(cloudRepository.resolvedHashes).isEmpty()
    }

    private class FakeSongRepository : SongPlaybackRepository {
        val resolvedHashes = mutableListOf<String>()
        val qualityOverrides = mutableListOf<OnlinePlaybackQuality?>()

        override suspend fun resolveSource(
            song: OnlineSong,
            qualityOverride: OnlinePlaybackQuality?,
        ): ResolveSongSourceResult {
            resolvedHashes += song.hash
            qualityOverrides += qualityOverride
            return resolved(song.hash)
        }
    }

    private class FakeCloudRepository : CloudRepository {
        val resolvedHashes = mutableListOf<String>()

        override suspend fun loadTracks(page: Int, pageSize: Int): CollectionLoadResult<CloudPage> = error("unused")

        override suspend fun resolveSource(track: CloudTrack): ResolveSongSourceResult {
            resolvedHashes += track.hash
            return resolved(track.hash)
        }
    }

    private class FakeLocalRepository(private val source: LocalMediaPlaybackSource? = null) : LocalMediaRepository {
        val resolvedIds = mutableListOf<LocalMediaId>()

        override suspend fun recoverStorage(): Boolean = true

        override fun observeAll() = flowOf(emptyList<LocalMedia>())

        override suspend fun scanDirectory(treeUri: String) = error("unused")

        override suspend fun importFromUri(
            sourceUri: String,
            duplicateAction: LocalMediaDuplicateAction,
        ): LocalMediaImportResult = error("unused")

        override suspend fun delete(id: LocalMediaId): LocalMediaDeleteResult = error("unused")

        override suspend fun resolvePlaybackSource(id: LocalMediaId): LocalMediaPlaybackSource? {
            resolvedIds += id
            return source
        }
    }

    private class FakeDownloadController(private val source: ResolvedSongSource) : MusicDownloadController {
        override val downloads = MutableStateFlow(emptyList<MusicDownload>())
        override fun download(song: OnlineSong) = Unit
        override fun pause(id: String) = Unit
        override fun resume(id: String) = Unit
        override fun retry(id: String) = Unit
        override fun remove(id: String) = Unit
        override fun pauseAll() = Unit
        override fun resumeAll() = Unit
        override fun completedSource(songHash: String, quality: OnlinePlaybackQuality?) = source
    }

    private companion object {
        fun resolved(hash: String) = ResolveSongSourceResult.Resolved(
            ResolvedSongSource("https://media.example/$hash.mp3", 180_000, "mp3"),
        )

        fun song(hash: String) = OnlineSong(
            hash = hash,
            title = hash,
            artist = "artist",
            coverUrl = null,
            albumId = null,
            albumAudioId = null,
            durationMillis = 180_000,
            quality = AudioQuality.Standard,
            vip = false,
        )

        fun cloud(hash: String) = CloudTrack(
            hash = hash,
            title = hash,
            artist = "artist",
            album = "album",
            coverUrl = null,
            durationMillis = 180_000,
            albumAudioId = "audio-$hash",
        )

        fun localMedia() = LocalMedia(
            id = LocalMediaId("local-id"),
            displayName = "signals.flac",
            title = "Signals",
            artist = "artist",
            albumTitle = "album",
            artworkUri = null,
            durationMillis = 180_000,
            mimeType = "audio/flac",
            fileExtension = "flac",
            sizeBytes = 4_096,
            sampleRateHz = 96_000,
            bitDepth = 24,
            bitrateBitsPerSecond = 2_304_000,
            importedAtEpochMillis = 1_000,
        )

        fun localSource() = LocalMediaPlaybackSource(
            uri = "file:/private/signals.flac",
            media = localMedia(),
        )
    }
}

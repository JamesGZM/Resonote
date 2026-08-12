package com.resonote.core.playback.service

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.CloudRepository
import com.resonote.core.data.SongPlaybackRepository
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.CloudPage
import com.resonote.core.model.CloudTrack
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.ResolveSongSourceResult
import com.resonote.core.model.ResolvedSongSource
import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackOrigin
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PlaybackSourceResolverTest {
    @Test
    fun onlineAndCloudItemsUseTheirMatchingRepositories() = runTest {
        val songRepository = FakeSongRepository()
        val cloudRepository = FakeCloudRepository()
        val resolver = PlaybackSourceResolver(songRepository, cloudRepository)
        val cloudTrack = cloud("cloud")

        resolver.resolve(PlaybackItem(song("online")))
        resolver.resolve(PlaybackItem(song("cloud"), PlaybackOrigin.Cloud(cloudTrack)))

        assertThat(songRepository.resolvedHashes).containsExactly("online")
        assertThat(cloudRepository.resolvedHashes).containsExactly("cloud")
    }

    @Test
    fun preResolvedItemSkipsBothRepositories() = runTest {
        val songRepository = FakeSongRepository()
        val cloudRepository = FakeCloudRepository()
        val resolver = PlaybackSourceResolver(songRepository, cloudRepository)
        val source = ResolvedSongSource("https://media.example/song.mp3", 180_000, "mp3")

        val result = resolver.resolve(PlaybackItem(song("ready"), resolvedSource = source))

        assertThat((result as ResolveSongSourceResult.Resolved).source).isEqualTo(source)
        assertThat(songRepository.resolvedHashes).isEmpty()
        assertThat(cloudRepository.resolvedHashes).isEmpty()
    }

    private class FakeSongRepository : SongPlaybackRepository {
        val resolvedHashes = mutableListOf<String>()

        override suspend fun resolveSource(song: OnlineSong): ResolveSongSourceResult {
            resolvedHashes += song.hash
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
    }
}

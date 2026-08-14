package com.resonote.core.data

import com.google.common.truth.Truth.assertThat
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ContentFailure
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.model.NetworkMobileCodeLoginResult
import com.resonote.core.network.model.NetworkPlaylistInfo
import com.resonote.core.network.model.NetworkPlaylistPage
import com.resonote.core.network.model.NetworkPlaylistSummary
import com.resonote.core.network.model.NetworkRanking
import com.resonote.core.network.model.NetworkRecommendationMode
import com.resonote.core.network.model.NetworkSearchPage
import com.resonote.core.network.model.NetworkSong
import com.resonote.core.network.model.NetworkSongPage
import com.resonote.core.network.model.NetworkSongSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

class CollectionRepositoriesTest {
    @Test
    fun rankingRepositoryMapsListAndPagedSongs() = runTest {
        val network = FakeNetwork()
        val repository = DefaultRankingRepository(network, RiskChallengeRegistry())

        val rankings = repository.loadRankings() as CollectionLoadResult.Available
        val songs = repository.loadSongs("rank-1", page = 2, pageSize = 30) as CollectionLoadResult.Available

        assertThat(rankings.value.single()).isEqualTo(
            com.resonote.core.model.Ranking("rank-1", "热歌榜", "https://img/480.jpg"),
        )
        assertThat(songs.value.page).isEqualTo(2)
        assertThat(songs.value.total).isEqualTo(31)
        assertThat(songs.value.hasMore).isFalse()
        assertThat(songs.value.songs.single().quality).isEqualTo(AudioQuality.Lossless)
        assertThat(network.rankingRequest).isEqualTo(Triple("rank-1", 2, 30))
    }

    @Test
    fun playlistRepositoryMapsDetailsTracksAndPaging() = runTest {
        val network = FakeNetwork()
        val repository = DefaultPlaylistRepository(network, RiskChallengeRegistry())

        val result = repository.loadPlaylist("gid", page = 1, pageSize = 50) as CollectionLoadResult.Available

        assertThat(result.value.details?.id).isEqualTo("gid")
        assertThat(result.value.details?.coverUrl).isEqualTo("https://playlist/480.jpg")
        assertThat(result.value.songs.single().fileId).isEqualTo("file-1")
        assertThat(result.value.songs.single().coverUrl).isEqualTo("https://song/480.jpg")
        assertThat(result.value.hasMore).isTrue()
        assertThat(network.playlistRequest).isEqualTo(Triple("gid", 1, 50))
    }

    @Test
    fun repositoriesMapProtocolFailureAndPropagateCancellation() = runTest {
        val network = FakeNetwork().apply {
            rankingFailure =
                ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)
        }
        val repository = DefaultRankingRepository(network, RiskChallengeRegistry())

        val failure = repository.loadRankings() as CollectionLoadResult.Failed
        assertThat(failure.failure).isEqualTo(ContentFailure.Protocol)

        network.rankingFailure = CancellationException("cancelled")
        val cancellation = runCatching { repository.loadRankings() }.exceptionOrNull()
        assertThat(cancellation).isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun blankIdentifiersAreRejectedBeforeNetwork() {
        val network = FakeNetwork()

        assertThrows(IllegalArgumentException::class.java) {
            runTest { DefaultRankingRepository(network, RiskChallengeRegistry()).loadSongs(" ") }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runTest { DefaultPlaylistRepository(network, RiskChallengeRegistry()).loadPlaylist("") }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runTest { DefaultRankingRepository(network, RiskChallengeRegistry()).loadSongs("rank", page = 0) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runTest { DefaultPlaylistRepository(network, RiskChallengeRegistry()).loadPlaylist("gid", pageSize = 101) }
        }
    }

    private class FakeNetwork : TestApiNetworkDataSource() {
        var rankingFailure: Throwable? = null
        var rankingRequest: Triple<String, Int, Int>? = null
        var playlistRequest: Triple<String, Int, Int>? = null

        override suspend fun rankings(): List<NetworkRanking> {
            rankingFailure?.let { throw it }
            return listOf(NetworkRanking("rank-1", "热歌榜", "https://img/{size}.jpg"))
        }

        override suspend fun rankingSongs(rankId: String, page: Int, pageSize: Int): NetworkSongPage {
            rankingRequest = Triple(rankId, page, pageSize)
            return NetworkSongPage(listOf(song()), 31, false)
        }

        override suspend fun playlistSongs(globalCollectionId: String, page: Int, pageSize: Int): NetworkPlaylistPage {
            playlistRequest = Triple(globalCollectionId, page, pageSize)
            return NetworkPlaylistPage(
                NetworkPlaylistInfo("gid", "歌单", "简介", "https://playlist/{size}.jpg", 51),
                listOf(song()),
                true,
            )
        }

        private fun song() = NetworkSong(
            hash = "hash",
            title = "歌曲",
            artist = "歌手",
            coverUrl = "http://song/{size}.jpg",
            albumId = "1",
            albumAudioId = "2",
            durationMillis = 180_000,
            highQualityHash = null,
            losslessHash = null,
            vip = false,
            losslessAvailable = true,
            albumTitle = "专辑",
            fileId = "file-1",
        )

        override suspend fun dailyRecommendations(): List<NetworkSong> = error("unused")
        override suspend fun recommendedPlaylists(page: Int, pageSize: Int): List<NetworkPlaylistSummary> =
            error("unused")
        override suspend fun newSongs(page: Int, pageSize: Int): List<NetworkSong> = error("unused")
        override suspend fun radioRecommendations(mode: NetworkRecommendationMode): List<NetworkSong> = error("unused")
        override suspend fun resolveSongSource(
            hash: String,
            albumId: String?,
            albumAudioId: String?,
            requestedQuality: String,
        ): NetworkSongSource = error("unused")
        override suspend fun searchSongs(keywords: String, page: Int, pageSize: Int): NetworkSearchPage =
            error("unused")
        override suspend fun sendMobileCode(mobile: String) = error("unused")
        override suspend fun loginWithMobileCode(
            mobile: String,
            code: String,
            selectedUserId: String?,
        ): NetworkMobileCodeLoginResult = error("unused")
        override suspend fun loginWithPassword(
            username: String,
            password: String,
        ): com.resonote.core.network.model.NetworkPasswordLoginResult = error("unused")
        override suspend fun userDetail(): com.resonote.core.network.model.NetworkUserDetail = error("unused")
        override suspend fun userVip(): com.resonote.core.network.model.NetworkUserVip = error("unused")
        override suspend fun userPlaylists(
            page: Int,
            pageSize: Int,
        ): List<com.resonote.core.network.model.NetworkUserPlaylist> = error("unused")
        override suspend fun createPlaylist(name: String): String = error("unused")
        override suspend fun addPlaylistTracks(
            listId: String,
            tracks: List<com.resonote.core.network.model.NetworkPlaylistTrackInput>,
        ) = error("unused")
        override suspend fun deletePlaylistTracks(listId: String, fileIds: List<String>) = error("unused")
        override suspend fun cloudTracks(page: Int, pageSize: Int): com.resonote.core.network.model.NetworkCloudPage =
            error("unused")
        override suspend fun resolveCloudSongSource(
            hash: String,
            albumAudioId: String?,
            name: String,
        ): NetworkSongSource = error("unused")
        override suspend fun banners(): List<com.resonote.core.network.model.NetworkBanner> = error("unused")
        override suspend fun playlistCategories(): List<com.resonote.core.network.model.NetworkPlaylistCategory> =
            error("unused")
        override suspend fun newAlbums(page: Int, pageSize: Int): List<com.resonote.core.network.model.NetworkAlbum> =
            error("unused")
        override suspend fun albumSongs(
            albumId: String,
            page: Int,
            pageSize: Int,
        ): com.resonote.core.network.model.NetworkAlbumSongPage = error("unused")
        override suspend fun artistDetail(artistId: String): com.resonote.core.network.model.NetworkArtistInfo? =
            error("unused")
        override suspend fun artistSongs(
            artistId: String,
            page: Int,
            pageSize: Int,
            newestFirst: Boolean,
        ): com.resonote.core.network.model.NetworkArtistSongPage = error("unused")
        override suspend fun searchComplex(keywords: String): com.resonote.core.network.model.NetworkComplexSearch =
            error("unused")
        override suspend fun hotSearchKeywords(): List<com.resonote.core.network.model.NetworkSearchKeyword> =
            error("unused")
        override suspend fun searchSuggestions(keywords: String): List<String> = error("unused")
        override suspend fun searchLyric(
            hash: String,
            albumAudioId: String?,
        ): com.resonote.core.network.model.NetworkLyricCandidate? = error("unused")
        override suspend fun downloadLyric(candidate: com.resonote.core.network.model.NetworkLyricCandidate): String? =
            error("unused")
        override suspend fun resolveVideoUrl(hash: String): String? = error("unused")
        override suspend fun recognizeAudio(
            pcm: ByteArray,
        ): List<com.resonote.core.network.model.NetworkRecognitionMatch> = error("unused")
        override suspend fun createQrLoginKey(): String = error("unused")
        override suspend fun checkQrLogin(key: String): com.resonote.core.network.model.NetworkQrLoginStatus =
            error("unused")
        override suspend fun claimDailyVip(receiveDay: String): com.resonote.core.network.model.NetworkVipRewardResult =
            error("unused")
        override suspend fun upgradeDailyVip(): com.resonote.core.network.model.NetworkVipRewardResult = error("unused")
    }
}

package com.resonote.core.data

import com.google.common.truth.Truth.assertThat
import com.resonote.core.model.CloudTrack
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.PlaybackUnavailableReason
import com.resonote.core.model.PlaylistTrackInput
import com.resonote.core.model.ResolveSongSourceResult
import com.resonote.core.network.ApiAuthenticationRequiredException
import com.resonote.core.network.ApiNetworkException
import com.resonote.core.network.ApiPlaybackUnavailableException
import com.resonote.core.network.ApiRiskBlockedException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.model.NetworkCloudPage
import com.resonote.core.network.model.NetworkCloudStorage
import com.resonote.core.network.model.NetworkCloudTrack
import com.resonote.core.network.model.NetworkMobileCodeLoginResult
import com.resonote.core.network.model.NetworkPasswordLoginResult
import com.resonote.core.network.model.NetworkPlaylistPage
import com.resonote.core.network.model.NetworkPlaylistSummary
import com.resonote.core.network.model.NetworkPlaylistTrackInput
import com.resonote.core.network.model.NetworkRanking
import com.resonote.core.network.model.NetworkRecommendationMode
import com.resonote.core.network.model.NetworkSearchPage
import com.resonote.core.network.model.NetworkSong
import com.resonote.core.network.model.NetworkSongPage
import com.resonote.core.network.model.NetworkSongSource
import com.resonote.core.network.model.NetworkUserDetail
import com.resonote.core.network.model.NetworkUserPlaylist
import com.resonote.core.network.model.NetworkUserVip
import com.resonote.core.network.model.NetworkVipRewardResult
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

class UserRepositoriesTest {
    @Test
    fun profileMapsMobileFieldsAndImageSizes() = runTest {
        val network = FakeNetwork()
        val repository = DefaultUserProfileRepository(network, RiskChallengeRegistry())

        val result = repository.loadProfile() as CollectionLoadResult.Available

        assertThat(result.value.userId).isEqualTo("42")
        assertThat(result.value.avatarUrl).isEqualTo("https://avatar/240")
        assertThat(result.value.backgroundUrl).isEqualTo("https://background/720")
        assertThat(result.value.isVip).isTrue()
        assertThat(result.value.vipLabel).isEqualTo("SVIP")
        assertThat(network.detailCalls).isEqualTo(1)
        assertThat(network.vipCalls).isEqualTo(1)
    }

    @Test
    fun vipFailureDegradesToNonVipWithoutHidingProfile() = runTest {
        val network =
            FakeNetwork(vipFailure = ApiNetworkException(ApiNetworkException.Kind.Offline, IOException("offline")))
        val repository = DefaultUserProfileRepository(network, RiskChallengeRegistry())

        val result = repository.loadProfile() as CollectionLoadResult.Available

        assertThat(result.value.isVip).isFalse()
        assertThat(result.value.vipLabel).isEmpty()
    }

    @Test
    fun detailFailureAndMissingAuthenticationRemainTyped() = runTest {
        val offlineNetwork =
            FakeNetwork(detailFailure = ApiNetworkException(ApiNetworkException.Kind.Offline, IOException("offline")))
        val offline =
            DefaultUserProfileRepository(
                offlineNetwork,
                RiskChallengeRegistry(),
            ).loadProfile() as CollectionLoadResult.Failed
        val authenticationNetwork = FakeNetwork(detailFailure = ApiAuthenticationRequiredException())
        val authentication =
            DefaultUserProfileRepository(
                authenticationNetwork,
                RiskChallengeRegistry(),
            ).loadProfile() as CollectionLoadResult.Failed

        assertThat(offline.failure).isEqualTo(ContentFailure.Network)
        assertThat(authentication.failure).isEqualTo(ContentFailure.AuthenticationRequired)
        assertThat(offlineNetwork.detailCalls).isEqualTo(1)
        assertThat(offlineNetwork.vipCalls).isEqualTo(1)
        assertThat(authenticationNetwork.detailCalls).isEqualTo(1)
        assertThat(authenticationNetwork.vipCalls).isEqualTo(1)
    }

    @Test
    fun explicitVipAuthenticationFailureConfirmsFailedProfileSession() = runTest {
        val network = FakeNetwork(
            detailFailure = ApiServiceException("20017"),
            vipFailure = ApiAuthenticationRequiredException(),
        )
        val repository = DefaultUserProfileRepository(network, RiskChallengeRegistry())

        val result = repository.loadProfile() as CollectionLoadResult.Failed

        assertThat(result.failure).isEqualTo(ContentFailure.AuthenticationRequired)
        assertThat(network.detailCalls).isEqualTo(1)
        assertThat(network.vipCalls).isEqualTo(1)
    }

    @Test
    fun ordinaryVipFailureDoesNotOverrideFailedProfileRequest() = runTest {
        val network = FakeNetwork(
            detailFailure = ApiNetworkException(ApiNetworkException.Kind.Offline, IOException("detail offline")),
            vipFailure = ApiServiceException("20017"),
        )
        val repository = DefaultUserProfileRepository(network, RiskChallengeRegistry())

        val result = repository.loadProfile() as CollectionLoadResult.Failed

        assertThat(result.failure).isEqualTo(ContentFailure.Network)
        assertThat(network.detailCalls).isEqualTo(1)
        assertThat(network.vipCalls).isEqualTo(1)
    }

    @Test
    fun explicitVipAuthenticationFailureIsNotHiddenBySuccessfulProfileRequest() = runTest {
        val network = FakeNetwork(vipFailure = ApiAuthenticationRequiredException())
        val repository = DefaultUserProfileRepository(network, RiskChallengeRegistry())

        val result = repository.loadProfile() as CollectionLoadResult.Failed

        assertThat(result.failure).isEqualTo(ContentFailure.AuthenticationRequired)
        assertThat(network.detailCalls).isEqualTo(1)
        assertThat(network.vipCalls).isEqualTo(1)
    }

    @Test
    fun playlistsMapIdsOwnershipAndPaging() = runTest {
        val network = FakeNetwork()
        val repository = DefaultLibraryRepository(network, RiskChallengeRegistry())

        val result = repository.loadPlaylists(page = 2, pageSize = 100) as CollectionLoadResult.Available

        assertThat(result.value.single().listId).isEqualTo("list-id")
        assertThat(result.value.single().globalId).isEqualTo("global-id")
        assertThat(result.value.single().coverUrl).isEqualTo("https://cover/240")
        assertThat(network.playlistRequest).isEqualTo(2 to 100)
    }

    @Test
    fun playlistMutationsMapTypedInputsAndResults() = runTest {
        val network = FakeNetwork()
        val repository = DefaultLibraryRepository(network, RiskChallengeRegistry())

        val created = repository.createPlaylist("  Road Trip  ") as CollectionLoadResult.Available
        val added =
            repository.addTracks(
                "list-id",
                listOf(PlaylistTrackInput("HASH", "Title", "Artist", "12", "34")),
            )
        val removed = repository.removeTracks("list-id", listOf("91", "92"))

        assertThat(created.value).isEqualTo("created-list")
        assertThat(added).isEqualTo(CollectionLoadResult.Available(Unit))
        assertThat(removed).isEqualTo(CollectionLoadResult.Available(Unit))
        assertThat(network.createdName).isEqualTo("Road Trip")
        assertThat(network.addedTracks.single().albumAudioId).isEqualTo("34")
        assertThat(network.removedFileIds).containsExactly("91", "92").inOrder()
    }

    @Test
    fun playlistMutationAuthenticationFailureIsTyped() = runTest {
        val repository =
            DefaultLibraryRepository(
                FakeNetwork(mutationFailure = ApiAuthenticationRequiredException()),
                RiskChallengeRegistry(),
            )

        val result = repository.createPlaylist("name") as CollectionLoadResult.Failed

        assertThat(result.failure).isEqualTo(ContentFailure.AuthenticationRequired)
    }

    @Test
    fun cloudRepositoryMapsPagingStorageAndImageSize() = runTest {
        val network = FakeNetwork()
        val repository = DefaultCloudRepository(network, RiskChallengeRegistry())

        val result = repository.loadTracks(page = 2, pageSize = 50) as CollectionLoadResult.Available

        assertThat(result.value.page).isEqualTo(2)
        assertThat(result.value.total).isEqualTo(101)
        assertThat(result.value.hasMore).isTrue()
        assertThat(result.value.storage?.maxBytes).isEqualTo(1_000)
        assertThat(result.value.tracks.single().coverUrl).isEqualTo("https://cloud-cover/240")
        assertThat(network.cloudRequest).isEqualTo(2 to 50)
    }

    @Test
    fun cloudRepositoryUsesTrackDurationAndMapsUnavailable() = runTest {
        val track = CloudTrack("hash", "Cloud Song", "Artist", null, null, 123_000, "321")
        val network = FakeNetwork()
        val resolved = DefaultCloudRepository(
            network,
            RiskChallengeRegistry(),
        ).resolveSource(track) as ResolveSongSourceResult.Resolved
        val unavailable =
            DefaultCloudRepository(
                FakeNetwork(
                    cloudSourceFailure = ApiPlaybackUnavailableException(ApiPlaybackUnavailableException.Reason.Cloud),
                ),
                RiskChallengeRegistry(),
            ).resolveSource(track) as ResolveSongSourceResult.Unavailable

        assertThat(resolved.source.durationMillis).isEqualTo(123_000)
        assertThat(resolved.source.cacheKey).isEqualTo("cloud:hash")
        assertThat(network.cloudSourceRequest).isEqualTo(Triple("hash", "321", "Cloud Song"))
        assertThat(unavailable.reason).isEqualTo(PlaybackUnavailableReason.Cloud)
    }

    @Test
    fun vipRewardRepositoryPreservesAlreadyDoneAndUpgradeFlags() = runTest {
        val repository = DefaultVipRewardRepository(FakeNetwork(), RiskChallengeRegistry())

        val claimed = repository.claimDaily("2026-08-12") as CollectionLoadResult.Available
        val upgraded = repository.upgradeDaily() as CollectionLoadResult.Available

        assertThat(claimed.value.alreadyDone).isTrue()
        assertThat(claimed.value.canUpgrade).isTrue()
        assertThat(upgraded.value.canUpgrade).isFalse()
    }

    @Test
    fun vipRewardRiskBlockRemainsTyped() = runTest {
        val repository = DefaultVipRewardRepository(
            FakeNetwork(rewardFailure = ApiRiskBlockedException("20028")),
            RiskChallengeRegistry(),
        )

        val result = repository.claimDaily("2026-08-12") as CollectionLoadResult.Failed

        assertThat(result.failure).isEqualTo(ContentFailure.RiskBlocked)
    }

    private class FakeNetwork(
        private val detailFailure: Throwable? = null,
        private val vipFailure: Throwable? = null,
        private val mutationFailure: Throwable? = null,
        private val cloudSourceFailure: Throwable? = null,
        private val rewardFailure: Throwable? = null,
    ) : TestApiNetworkDataSource() {
        var detailCalls = 0
        var vipCalls = 0
        var playlistRequest: Pair<Int, Int>? = null
        var createdName: String? = null
        var addedTracks: List<NetworkPlaylistTrackInput> = emptyList()
        var removedFileIds: List<String> = emptyList()
        var cloudRequest: Pair<Int, Int>? = null
        var cloudSourceRequest: Triple<String, String?, String>? = null

        override suspend fun userDetail(): NetworkUserDetail {
            detailCalls += 1
            detailFailure?.let { throw it }
            return NetworkUserDetail(
                "42",
                "Fixture",
                "https://avatar/{size}",
                "https://background/{size}",
                "bio",
                1,
                2,
                3,
            )
        }

        override suspend fun userVip(): NetworkUserVip {
            vipCalls += 1
            vipFailure?.let { throw it }
            return NetworkUserVip(true, "SVIP")
        }

        override suspend fun userPlaylists(page: Int, pageSize: Int): List<NetworkUserPlaylist> {
            playlistRequest = page to pageSize
            return listOf(NetworkUserPlaylist("list-id", "global-id", "我喜欢", "https://cover/{size}", 8, true, true))
        }

        override suspend fun createPlaylist(name: String): String {
            mutationFailure?.let { throw it }
            createdName = name
            return "created-list"
        }

        override suspend fun addPlaylistTracks(listId: String, tracks: List<NetworkPlaylistTrackInput>) {
            mutationFailure?.let { throw it }
            addedTracks = tracks
        }

        override suspend fun deletePlaylistTracks(listId: String, fileIds: List<String>) {
            mutationFailure?.let { throw it }
            removedFileIds = fileIds
        }

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
        override suspend fun rankings(): List<NetworkRanking> = error("unused")
        override suspend fun rankingSongs(rankId: String, page: Int, pageSize: Int): NetworkSongPage = error("unused")
        override suspend fun playlistSongs(globalCollectionId: String, page: Int, pageSize: Int): NetworkPlaylistPage =
            error("unused")
        override suspend fun searchSongs(keywords: String, page: Int, pageSize: Int): NetworkSearchPage =
            error("unused")
        override suspend fun sendMobileCode(mobile: String) = error("unused")
        override suspend fun loginWithMobileCode(
            mobile: String,
            code: String,
            selectedUserId: String?,
        ): NetworkMobileCodeLoginResult = error("unused")
        override suspend fun loginWithPassword(username: String, password: String): NetworkPasswordLoginResult =
            error("unused")
        override suspend fun cloudTracks(page: Int, pageSize: Int): NetworkCloudPage {
            cloudRequest = page to pageSize
            return NetworkCloudPage(
                tracks = listOf(
                    NetworkCloudTrack(
                        "hash",
                        "Cloud Song",
                        "Artist",
                        "Album",
                        "https://cloud-cover/{size}",
                        123_000,
                        "321",
                    ),
                ),
                total = 101,
                hasMore = true,
                storage = NetworkCloudStorage(100, 1_000),
            )
        }

        override suspend fun resolveCloudSongSource(
            hash: String,
            albumAudioId: String?,
            name: String,
        ): NetworkSongSource {
            cloudSourceFailure?.let { throw it }
            cloudSourceRequest = Triple(hash, albumAudioId, name)
            return NetworkSongSource("https://audio/cloud.mp3", 0, "mp3")
        }
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
        override suspend fun claimDailyVip(receiveDay: String): NetworkVipRewardResult {
            rewardFailure?.let { throw it }
            return NetworkVipRewardResult(true, true)
        }
        override suspend fun upgradeDailyVip(): NetworkVipRewardResult {
            rewardFailure?.let { throw it }
            return NetworkVipRewardResult(false, false)
        }
    }
}

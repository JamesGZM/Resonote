package com.resonote.core.data

import com.google.common.truth.Truth.assertThat
import com.resonote.core.datastore.HomeSnapshotStorage
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.HomeContent
import com.resonote.core.model.HomeRefreshResult
import com.resonote.core.model.HomeSection
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.model.PlaybackUnavailableReason
import com.resonote.core.model.RecommendationMode
import com.resonote.core.model.ResolveSongSourceResult
import com.resonote.core.network.ApiNetworkException
import com.resonote.core.network.ApiPlaybackUnavailableException
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiRiskException
import com.resonote.core.network.model.NetworkMobileCodeLoginResult
import com.resonote.core.network.model.NetworkPlaylistPage
import com.resonote.core.network.model.NetworkPlaylistSummary
import com.resonote.core.network.model.NetworkRanking
import com.resonote.core.network.model.NetworkRecommendationMode
import com.resonote.core.network.model.NetworkSearchPage
import com.resonote.core.network.model.NetworkSong
import com.resonote.core.network.model.NetworkSongPage
import com.resonote.core.network.model.NetworkSongSource
import com.resonote.core.network.risk.ApiRiskChallenge
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultHomeRepositoryTest {
    private val riskChallenges = RiskChallengeRegistry()

    @Test
    fun startupNetworkBeginsWhileCachedContentIsStillLoading() = runTest {
        val cache = BlockingHomeSnapshotStorage()
        val networkStarted = CompletableDeferred<Unit>()
        val repository = createHomeRepository(
            FakeNetwork(daily = {
                networkStarted.complete(Unit)
                emptyList()
            }),
            storage = cache,
        )

        val refresh = async { repository.refresh() }
        runCurrent()

        assertThat(cache.readStarted.isCompleted).isTrue()
        assertThat(networkStarted.isCompleted).isTrue()
        cache.releaseRead.complete(Unit)
        refresh.await()
    }

    @Test
    fun cachedContentIsPublishedBeforeStartupRefreshCompletes() = runTest {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val cached = HomeContent(
            dailyRecommendations = listOf(domainSong(120_000).copy(hash = "cached")),
            recommendedPlaylists = listOf(com.resonote.core.model.PlaylistSummary("cached", "Cached", null, 1)),
            newSongs = emptyList(),
        )
        val storage = FakeHomeSnapshotStorage(HomeSnapshotCodec().encode(cached))
        val network = FakeNetwork(daily = {
            started.complete(Unit)
            release.await()
            listOf(song("fresh"))
        })
        val repository = createHomeRepository(network, storage = storage)

        val refresh = async { repository.refresh() }
        started.await()

        assertThat(repository.content.first { it != null }).isEqualTo(cached)
        release.complete(Unit)
        refresh.await()
    }

    @Test
    fun successfulEmptySectionsReplaceCachedContentAndPersist() = runTest {
        val cached = HomeContent(
            dailyRecommendations = listOf(domainSong(120_000)),
            recommendedPlaylists = listOf(com.resonote.core.model.PlaylistSummary("cached", "Cached", null, 1)),
            newSongs = listOf(domainSong(120_000).copy(hash = "new")),
        )
        val storage = FakeHomeSnapshotStorage(HomeSnapshotCodec().encode(cached))
        val repository = createHomeRepository(FakeNetwork(), storage = storage)

        val result = repository.refresh() as HomeRefreshResult.Updated

        assertThat(result.content.dailyRecommendations).isEmpty()
        assertThat(result.content.recommendedPlaylists).isEmpty()
        assertThat(result.content.newSongs).isEmpty()
        assertThat(HomeSnapshotCodec().decode(requireNotNull(storage.snapshotJson.value))).isEqualTo(result.content)
    }

    @Test
    fun allFailedRefreshRetainsCachedContent() = runTest {
        val failure = { throw ApiNetworkException(ApiNetworkException.Kind.Connection, IOException()) }
        val cached = HomeContent(listOf(domainSong(120_000)), emptyList(), emptyList())
        val storage = FakeHomeSnapshotStorage(HomeSnapshotCodec().encode(cached))
        val repository = createHomeRepository(
            FakeNetwork(daily = failure, playlists = { _, _ -> failure() }, newSongLoader = { _, _ -> failure() }),
            storage = storage,
        )

        val result = repository.refresh()

        assertThat(result).isInstanceOf(HomeRefreshResult.Failed::class.java)
        assertThat(repository.content.value).isEqualTo(cached)
        assertThat(HomeSnapshotCodec().decode(requireNotNull(storage.snapshotJson.value))).isEqualTo(cached)
    }

    @Test
    fun refreshLoadsSectionsConcurrentlyAndSamplesExactlySixDailySongs() = runTest {
        val gates = List(3) { CompletableDeferred<Unit>() }
        val started = List(3) { CompletableDeferred<Unit>() }
        val network =
            FakeNetwork(
                daily = {
                    started[0].complete(Unit)
                    gates[0].await()
                    List(8) { song("daily-$it") }
                },
                playlists = { _, _ ->
                    started[1].complete(Unit)
                    gates[1].await()
                    List(8) { playlist("playlist-$it") }
                },
                newSongLoader = { _, _ ->
                    started[2].complete(Unit)
                    gates[2].await()
                    List(8) { song("new-$it") }
                },
            )
        val repository =
            createHomeRepository(network, HomeRecommendationSampler { songs, count -> songs.takeLast(count) })

        val refresh = async { repository.refresh() }
        started.forEach { it.await() }
        gates.forEach { it.complete(Unit) }
        val result = refresh.await() as HomeRefreshResult.Updated

        assertThat(result.content.dailyRecommendations.map { it.hash })
            .containsExactly("daily-2", "daily-3", "daily-4", "daily-5", "daily-6", "daily-7")
            .inOrder()
        assertThat(result.content.recommendedPlaylists).hasSize(6)
        assertThat(result.content.newSongs).hasSize(6)
    }

    @Test
    fun eachSuccessfulRefreshSamplesDailyPoolAgain() = runTest {
        var sampleCall = 0
        val repository =
            createHomeRepository(
                FakeNetwork(daily = { List(8) { song("daily-$it") } }),
                HomeRecommendationSampler { songs, count ->
                    val offset = sampleCall++ % songs.size
                    (songs.drop(offset) + songs.take(offset)).take(count)
                },
            )

        val first = repository.refresh() as HomeRefreshResult.Updated
        val second = repository.refresh() as HomeRefreshResult.Updated

        assertThat(first.content.dailyRecommendations.first().hash).isEqualTo("daily-0")
        assertThat(second.content.dailyRecommendations.first().hash).isEqualTo("daily-1")
    }

    @Test
    fun partialFailureKeepsOldSectionAndUpdatesSuccessfulSections() = runTest {
        var failDaily = false
        var playlistVersion = "old"
        val network =
            FakeNetwork(
                daily = {
                    if (failDaily) throw ApiNetworkException(ApiNetworkException.Kind.Offline, IOException())
                    List(6) { song("daily-$it") }
                },
                playlists = { _, _ -> listOf(playlist(playlistVersion)) },
            )
        val repository = createHomeRepository(network, HomeRecommendationSampler { songs, count -> songs.take(count) })
        repository.refresh()
        failDaily = true
        playlistVersion = "new"

        val result = repository.refresh() as HomeRefreshResult.Updated

        assertThat(
            result.content.dailyRecommendations.map {
                it.hash
            },
        ).containsExactlyElementsIn(List(6) { "daily-$it" }).inOrder()
        assertThat(result.content.recommendedPlaylists.single().id).isEqualTo("new")
        assertThat(result.issues.single().section).isEqualTo(HomeSection.DailyRecommendations)
        assertThat(result.issues.single().failure).isEqualTo(ContentFailure.Network)
    }

    @Test
    fun firstRefreshAllFailedKeepsEmptyState() = runTest {
        val failure = { throw ApiNetworkException(ApiNetworkException.Kind.Connection, IOException()) }
        val repository =
            createHomeRepository(
                FakeNetwork(
                    daily = failure,
                    playlists = { _, _ -> failure() },
                    newSongLoader = { _, _ -> failure() },
                ),
                HomeRecommendationSampler { songs, count -> songs.take(count) },
            )

        val result = repository.refresh()

        assertThat(result).isInstanceOf(HomeRefreshResult.Failed::class.java)
        assertThat((result as HomeRefreshResult.Failed).issues).hasSize(3)
        assertThat(repository.content.value).isNull()
    }

    @Test
    fun firstRefreshPartiallySuccessfulPublishesAvailableSections() = runTest {
        val repository =
            createHomeRepository(
                FakeNetwork(
                    daily = { throw ApiNetworkException(ApiNetworkException.Kind.Offline, IOException()) },
                    playlists = { _, _ -> listOf(playlist("available")) },
                    newSongLoader = { _, _ ->
                        throw ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)
                    },
                ),
                HomeRecommendationSampler { songs, count -> songs.take(count) },
            )

        val result = repository.refresh() as HomeRefreshResult.Updated

        assertThat(result.content.dailyRecommendations).isEmpty()
        assertThat(result.content.recommendedPlaylists.single().id).isEqualTo("available")
        assertThat(result.content.newSongs).isEmpty()
        assertThat(result.issues.map { it.section })
            .containsExactly(HomeSection.DailyRecommendations, HomeSection.NewSongs)
            .inOrder()
    }

    @Test
    fun cancellationPropagates() {
        val repository =
            createHomeRepository(
                FakeNetwork(daily = { throw CancellationException("cancelled") }),
                HomeRecommendationSampler { songs, count -> songs.take(count) },
            )

        assertThrows(CancellationException::class.java) { runTest { repository.refresh() } }
    }

    @Test
    fun programmingFailuresAreNotReportedAsProtocolFailures() {
        val repository =
            createHomeRepository(
                FakeNetwork(daily = { throw AssertionError("mapper bug") }),
                HomeRecommendationSampler { songs, count -> songs.take(count) },
            )

        assertThrows(AssertionError::class.java) { runTest { repository.refresh() } }
    }

    @Test
    fun olderRequestCannotOverwriteNewerRefresh() = runTest {
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        var dailyCall = 0
        val network =
            FakeNetwork(
                daily = {
                    dailyCall += 1
                    if (dailyCall == 1) {
                        firstStarted.complete(Unit)
                        releaseFirst.await()
                        listOf(song("old"))
                    } else {
                        listOf(song("new"))
                    }
                },
            )
        val repository = createHomeRepository(network, HomeRecommendationSampler { songs, count -> songs.take(count) })

        val first = async { repository.refresh() }
        firstStarted.await()
        repository.refresh()
        releaseFirst.complete(Unit)
        val superseded = first.await()

        assertThat(repository.content.value?.dailyRecommendations?.single()?.hash).isEqualTo("new")
        assertThat(superseded).isEqualTo(HomeRefreshResult.Superseded)
    }

    @Test
    fun radioMapsEveryModeAndDomainFields() = runTest {
        val observed = mutableListOf<NetworkRecommendationMode>()
        val network = FakeNetwork(radio = { mode ->
            observed += mode
            listOf(song("radio", lossless = true))
        })
        val repository = createHomeRepository(network, HomeRecommendationSampler { songs, count -> songs.take(count) })

        RecommendationMode.entries.forEach { repository.loadRadio(it) }

        assertThat(observed).containsExactlyElementsIn(NetworkRecommendationMode.entries).inOrder()
        val result = repository.loadRadio() as com.resonote.core.model.RadioRecommendationResult.Available
        assertThat(result.songs.single().quality).isEqualTo(AudioQuality.Lossless)
    }

    @Test
    fun highQualityAvailabilityDoesNotMapToHighResolution() = runTest {
        val repository =
            createHomeRepository(
                FakeNetwork(daily = { listOf(song("hq", highQuality = true)) }),
                HomeRecommendationSampler { songs, count -> songs.take(count) },
            )

        val result = repository.refresh() as HomeRefreshResult.Updated

        assertThat(result.content.dailyRecommendations.single().quality).isEqualTo(AudioQuality.HighQuality)
    }

    @Test
    fun playbackRepositoryMapsResolvedUnavailableAndNetworkResults() = runTest {
        var requestedQuality: String? = null
        val network = FakeNetwork(source = { _, _, _, quality ->
            requestedQuality = quality
            NetworkSongSource("https://cdn/song.mp3", 0, "mp3", isPreview = true)
        })
        val preferences = FakePlaybackPreferencesRepository(OnlinePlaybackQuality.HighResolution)
        val repository = DefaultSongPlaybackRepository(network, riskChallenges, preferences)
        val song = domainSong(durationMillis = 123_000)

        val resolved = repository.resolveSource(song) as ResolveSongSourceResult.Resolved
        assertThat(resolved.source.durationMillis).isEqualTo(123_000)
        assertThat(resolved.source.isPreview).isTrue()
        assertThat(resolved.source.cacheKey).isEqualTo("online:hash:HighResolution:preview")
        assertThat(requestedQuality).isEqualTo("high")

        val overridden = repository.resolveSource(
            song,
            OnlinePlaybackQuality.Lossless,
        ) as ResolveSongSourceResult.Resolved
        assertThat(overridden.source.cacheKey).isEqualTo("online:hash:Lossless:preview")
        assertThat(requestedQuality).isEqualTo("flac")

        network.source =
            { _, _, _, _ -> throw ApiPlaybackUnavailableException(ApiPlaybackUnavailableException.Reason.Vip) }
        val unavailable = repository.resolveSource(song) as ResolveSongSourceResult.Unavailable
        assertThat(unavailable.reason).isEqualTo(PlaybackUnavailableReason.Vip)

        network.source =
            { _, _, _, _ -> throw ApiPlaybackUnavailableException(ApiPlaybackUnavailableException.Reason.Copyright) }
        val copyright = repository.resolveSource(song) as ResolveSongSourceResult.Unavailable
        assertThat(copyright.reason).isEqualTo(PlaybackUnavailableReason.Copyright)

        network.source = { _, _, _, _ -> throw ApiNetworkException(ApiNetworkException.Kind.Timeout, IOException()) }
        val failed = repository.resolveSource(song) as ResolveSongSourceResult.Failed
        assertThat(failed.failure).isEqualTo(ContentFailure.Network)

        network.source = { _, _, _, _ -> throw ApiProtocolException(ApiProtocolException.Reason.MalformedResponse) }
        val malformed = repository.resolveSource(song) as ResolveSongSourceResult.Failed
        assertThat(malformed.failure).isEqualTo(ContentFailure.Protocol)

        network.source = { _, _, _, _ ->
            throw ApiRiskException(
                ApiRiskChallenge(eventId = "redacted"),
                ApiRiskException.Reason.VerificationUnavailable,
            )
        }
        val risk = repository.resolveSource(song) as ResolveSongSourceResult.Failed
        val required = risk.failure as ContentFailure.RiskVerificationRequired
        assertThat(required.challenge.value).isNotEmpty()
        assertThat(required.challenge.toString()).doesNotContain("provider-event")
    }

    @Test
    fun playbackRepositoryUsesStandardQualityWhenPreferenceReadFails() = runTest {
        var requestedQuality: String? = null
        val network = FakeNetwork(source = { _, _, _, quality ->
            requestedQuality = quality
            NetworkSongSource("https://cdn/song.mp3", 0, "mp3")
        })
        val preferences = object : PlaybackPreferencesRepository {
            override val playbackSpeed = MutableStateFlow(PlaybackSpeed.Normal)
            override val onlinePlaybackQuality = flow<OnlinePlaybackQuality> { throw IOException("broken preference") }
            override suspend fun setPlaybackSpeed(speed: PlaybackSpeed) = Unit
            override suspend fun setOnlinePlaybackQuality(quality: OnlinePlaybackQuality) = Unit
        }
        val repository = DefaultSongPlaybackRepository(network, riskChallenges, preferences)

        val result = repository.resolveSource(domainSong(durationMillis = 123_000))

        assertThat(result).isInstanceOf(ResolveSongSourceResult.Resolved::class.java)
        assertThat(requestedQuality).isEqualTo("128")
    }

    private fun song(id: String, lossless: Boolean = false, highQuality: Boolean = false) = NetworkSong(
        hash = id,
        title = "Title $id",
        artist = "Artist",
        coverUrl = "https://img/{size}.jpg",
        albumId = "1",
        albumAudioId = "2",
        durationMillis = 180_000,
        highQualityHash = null,
        losslessHash = if (lossless) "sq" else null,
        vip = false,
        highQualityAvailable = highQuality,
    )

    private fun playlist(id: String) = NetworkPlaylistSummary(id, "Playlist $id", "https://img/{size}.jpg", 100)

    private fun domainSong(durationMillis: Long) =
        OnlineSong("hash", "Title", "Artist", null, "1", "2", durationMillis, AudioQuality.Standard, false)

    private fun createHomeRepository(
        network: TestApiNetworkDataSource,
        sampler: HomeRecommendationSampler = HomeRecommendationSampler { songs, count -> songs.take(count) },
        storage: HomeSnapshotStorage = FakeHomeSnapshotStorage(),
    ) = DefaultHomeRepository(network, network, sampler, riskChallenges, storage)

    private class FakeHomeSnapshotStorage(initialJson: String? = null) : HomeSnapshotStorage {
        private val state = MutableStateFlow(initialJson)
        override val snapshotJson = state

        override suspend fun write(json: String) {
            state.value = json
        }

        override suspend fun clear() {
            state.value = null
        }
    }

    private class BlockingHomeSnapshotStorage : HomeSnapshotStorage {
        val readStarted = CompletableDeferred<Unit>()
        val releaseRead = CompletableDeferred<Unit>()
        override val snapshotJson = flow<String?> {
            readStarted.complete(Unit)
            releaseRead.await()
            emit(null)
        }

        override suspend fun write(json: String) = Unit

        override suspend fun clear() = Unit
    }

    private class FakeNetwork(
        var daily: suspend () -> List<NetworkSong> = { emptyList() },
        var playlists: suspend (Int, Int) -> List<NetworkPlaylistSummary> = { _, _ -> emptyList() },
        var newSongLoader: suspend (Int, Int) -> List<NetworkSong> = { _, _ -> emptyList() },
        var radio: suspend (NetworkRecommendationMode) -> List<NetworkSong> = { emptyList() },
        var source: suspend (String, String?, String?, String) -> NetworkSongSource = { _, _, _, _ -> error("unused") },
    ) : TestApiNetworkDataSource() {
        override suspend fun dailyRecommendations() = daily()
        override suspend fun recommendedPlaylists(page: Int, pageSize: Int) = playlists(page, pageSize)
        override suspend fun newSongs(page: Int, pageSize: Int): List<NetworkSong> = newSongLoader(page, pageSize)
        override suspend fun radioRecommendations(mode: NetworkRecommendationMode) = radio(mode)
        override suspend fun resolveSongSource(
            hash: String,
            albumId: String?,
            albumAudioId: String?,
            requestedQuality: String,
        ) = source(hash, albumId, albumAudioId, requestedQuality)

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

    private class FakePlaybackPreferencesRepository(initialQuality: OnlinePlaybackQuality) :
        PlaybackPreferencesRepository {
        override val playbackSpeed = MutableStateFlow(PlaybackSpeed.Normal)
        override val onlinePlaybackQuality = MutableStateFlow(initialQuality)
        override suspend fun setPlaybackSpeed(speed: PlaybackSpeed) {
            playbackSpeed.value = speed
        }
        override suspend fun setOnlinePlaybackQuality(quality: OnlinePlaybackQuality) {
            onlinePlaybackQuality.value =
                quality
        }
    }
}

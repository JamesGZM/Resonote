package com.resonote.core.network.retrofit

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.resonote.core.network.ApiAuthenticationRequiredException
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.api.MusicApi
import com.resonote.core.network.protocol.ApiDefaultsInterceptor
import com.resonote.core.network.protocol.ApiDeviceIdentityFactory
import com.resonote.core.network.protocol.ApiEndpointOrigins
import com.resonote.core.network.protocol.ApiProtocolCrypto
import com.resonote.core.network.protocol.ApiRequestSigner
import com.resonote.core.network.protocol.ApiResponseMetadataInterceptor
import com.resonote.core.network.protocol.ApiSigningInterceptor
import com.resonote.core.network.protocol.DeviceRegistrationCoordinator
import com.resonote.core.network.protocol.DeviceRegistrationProfile
import com.resonote.core.network.protocol.DeviceRegistrationProfileProvider
import com.resonote.core.network.protocol.ProductionApiOriginPolicy
import com.resonote.core.network.protocol.ProtocolRandom
import com.resonote.core.network.protocol.ProtocolTransport
import com.resonote.core.network.risk.ApiRiskChallengeDetector
import com.resonote.core.network.session.ApiAuthenticationGateReason
import com.resonote.core.network.session.ApiSession
import com.resonote.core.network.session.ApiSessionManager
import com.resonote.core.network.session.ApiSessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import org.junit.Assume.assumeTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.security.SecureRandom
import java.time.Clock
import java.util.Optional

class LiveApiSearchCanaryTest {
    @Test
    fun anonymousSearchReturnsConfirmedAuthenticationFailure() = runTest {
        assumeTrue(System.getenv("RESONOTE_RUN_LIVE_API_TESTS") == "true")
        val fixture = registeredLiveFixture()

        val failure =
            try {
                fixture.search.searchSongs("周杰伦", page = 1, pageSize = 1)
                null
            } catch (failure: ApiAuthenticationRequiredException) {
                failure
            }

        assertThat(failure).isNotNull()
        assertThat(failure?.reason).isEqualTo(ApiAuthenticationGateReason.LoginRequired)
        assertThat(failure?.serviceCode).isEqualTo(ANONYMOUS_SEARCH_REQUIRES_AUTHENTICATION)
    }

    @Test
    fun publicHomeEndpointsAndPlaybackUrlAreConsumable() = runTest {
        assumeTrue(System.getenv("RESONOTE_RUN_LIVE_API_TESTS") == "true")
        val fixture = registeredLiveFixture()
        val dataSource = fixture.dataSource

        val daily = liveStep("daily recommendations") { dataSource.dailyRecommendations() }
        val radio = liveStep("radio recommendations") {
            dataSource.radioRecommendations(com.resonote.core.network.model.NetworkRecommendationMode.Personal)
        }
        val playlists = liveStep("recommended playlists") { dataSource.recommendedPlaylists(page = 1, pageSize = 6) }
        val newSongs = liveStep("new songs") { dataSource.newSongs(page = 1, pageSize = 6) }

        assertThat(daily).isNotEmpty()
        assertThat(radio).isNotEmpty()
        assertThat(playlists).isNotEmpty()
        assertThat(newSongs).isNotEmpty()
        val playlistSongPools =
            playlists.take(3).mapNotNull { playlist ->
                runCatching { dataSource.playlistSongs(playlist.id, page = 1, pageSize = 30).songs }
                    .getOrNull()
                    ?.takeIf(List<*>::isNotEmpty)
            }
        val candidates =
            roundRobinCandidates(
                listOf(newSongs, daily, radio) + playlistSongPools,
                limit = 5,
            )
        var resolved: com.resonote.core.network.model.NetworkSongSource? = null
        val failureKinds = mutableListOf<String>()
        for (song in candidates) {
            val attempt = runCatching { fixture.playback.resolveSongSource(song.hash, song.albumId, song.albumAudioId) }
            resolved = attempt.getOrNull()
            attempt.exceptionOrNull()?.let { failure ->
                val candidateShape =
                    "albumId=${!song.albumId.isNullOrBlank()},albumAudioId=${!song.albumAudioId.isNullOrBlank()},vip=${song.vip}"
                failureKinds +=
                    if (failure is com.resonote.core.network.ApiPlaybackUnavailableException) {
                        "ApiPlaybackUnavailableException.${failure.reason}($candidateShape)"
                    } else if (failure is ApiServiceException) {
                        "ApiServiceException.${failure.serviceCode}($candidateShape)"
                    } else if (failure is ApiProtocolException) {
                        "ApiProtocolException.${failure.reason}($candidateShape)"
                    } else {
                        "${failure::class.simpleName.orEmpty()}($candidateShape)"
                    }
            }
            if (resolved != null) break
        }

        assertWithMessage("Playback candidate failures: $failureKinds").that(resolved).isNotNull()
        val source = checkNotNull(resolved)
        val mediaUrl = source.uri.toHttpUrl()
        assertThat(mediaUrl.scheme).isAnyOf("http", "https")
        if (mediaUrl.scheme == "http") {
            assertThat(mediaUrl.host == "kugou.com" || mediaUrl.host.endsWith(".kugou.com")).isTrue()
        }
        OkHttpClient().newCall(
            Request.Builder()
                .url(mediaUrl)
                .header("Range", "bytes=0-1023")
                .build(),
        ).execute().use { response ->
            assertThat(response.isSuccessful).isTrue()
            val bytesRead = response.body?.source()?.read(Buffer(), 1_024) ?: -1
            assertThat(bytesRead).isGreaterThan(0)
        }
    }

    @Test
    fun publicRankingAndPlaylistDetailsAreConsumable() = runTest {
        assumeTrue(System.getenv("RESONOTE_RUN_LIVE_API_TESTS") == "true")
        val fixture = registeredLiveFixture()
        val dataSource = fixture.dataSource

        val rankings = dataSource.rankings()
        assertThat(rankings).isNotEmpty()
        val rankingPage =
            rankings.take(3).firstNotNullOfOrNull { ranking ->
                runCatching { dataSource.rankingSongs(ranking.id, page = 1, pageSize = 3) }
                    .getOrNull()
                    ?.takeIf { it.songs.isNotEmpty() }
            }
        assertWithMessage("No consumable songs in the first three public rankings").that(rankingPage).isNotNull()

        val playlists = dataSource.recommendedPlaylists(page = 1, pageSize = 3)
        assertThat(playlists).isNotEmpty()
        val playlistPage =
            playlists.take(3).firstNotNullOfOrNull { playlist ->
                runCatching { dataSource.playlistSongs(playlist.id, page = 1, pageSize = 3) }
                    .getOrNull()
                    ?.takeIf { it.info != null && it.songs.isNotEmpty() }
            }
        assertWithMessage("No consumable details in the first three public playlists").that(playlistPage).isNotNull()
        assertWithMessage("Playlist songs should expose artwork URLs")
            .that(checkNotNull(playlistPage).songs.any { !it.coverUrl.isNullOrBlank() })
            .isTrue()
    }

    @Test
    fun publicPlaylistPaginationIsConsumable() = runTest {
        assumeTrue(System.getenv("RESONOTE_RUN_LIVE_API_TESTS") == "true")
        val dataSource = registeredLiveFixture().dataSource
        val playlists = dataSource.recommendedPlaylists(page = 1, pageSize = 12)
        val pagedPlaylist = playlists.firstNotNullOfOrNull { playlist ->
            runCatching { dataSource.playlistSongs(playlist.id, page = 1, pageSize = 50) }
                .getOrNull()
                ?.takeIf { it.songs.isNotEmpty() && it.hasMore }
                ?.let { playlist to it }
        }

        assertWithMessage("No recommended playlist exposes a second page").that(pagedPlaylist).isNotNull()
        val (playlist, firstPage) = checkNotNull(pagedPlaylist)
        val secondPage = dataSource.playlistSongs(playlist.id, page = 2, pageSize = 50)

        assertThat(firstPage.songs).isNotEmpty()
        assertThat(secondPage.songs).isNotEmpty()
        assertThat(secondPage.songs.map { it.hash }.toSet())
            .containsNoneIn(firstPage.songs.map { it.hash }.toSet())
    }

    private fun liveFixture(): LiveFixture {
        val json = Json { ignoreUnknownKeys = true }
        val sessions = ApiSessionManager(Optional.of(MemoryStore()), ApiDeviceIdentityFactory())
        val riskDetector = ApiRiskChallengeDetector()
        val signer = ApiRequestSigner()
        val random = ProtocolRandom { length ->
            val alphabet = "1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            val secureRandom = SecureRandom()
            buildString(length) { repeat(length) { append(alphabet[secureRandom.nextInt(alphabet.length)]) } }
        }
        val crypto = ApiProtocolCrypto(random)
        val clock = Clock.systemUTC()
        val client =
            OkHttpClient.Builder()
                .retryOnConnectionFailure(false)
                .addInterceptor(ApiDefaultsInterceptor(clock, sessions))
                .addInterceptor(ApiSigningInterceptor(signer))
                .addInterceptor(ApiResponseMetadataInterceptor(json))
                .build()
        val executor = ProtocolTransport(
            { client },
            json,
            clock,
            signer,
            sessions,
            riskDetector,
            ProductionApiOriginPolicy(),
        )
        val origins = ApiEndpointOrigins()
        val musicApi =
            Retrofit.Builder()
                .baseUrl("https://gateway.kugou.com/")
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(MusicApi::class.java)
        val registration =
            DeviceRegistrationCoordinator(
                executor,
                json,
                crypto,
                sessions,
                origins,
                DeviceRegistrationProfileProvider {
                    DeviceRegistrationProfile(
                        totalMemoryBytes = 4_983_533_568,
                        availableInternalStorageBytes = 48_114_719,
                        availableExternalStorageBytes = 48_114_717,
                        brand = "unknown",
                        buildId = "unknown",
                        device = "unknown",
                        manufacturer = "unknown",
                    )
                },
            )
        val calls = ApiCallExecutor(sessions)
        val responses = ApiResponseVerifier(riskDetector, sessions)
        val home = RealHomeNetworkDataSource(musicApi, registration, signer, clock, responses, calls)
        val catalog =
            RealCatalogNetworkDataSource(musicApi, registration, signer, clock, crypto, responses, calls, origins)
        val ranking = RealRankingNetworkDataSource(musicApi, registration, responses, calls)
        val playlist = RealPlaylistNetworkDataSource(musicApi, registration, responses, calls)
        return LiveFixture(
            dataSource = LivePublicDataSource(home, catalog, ranking, playlist),
            search = RealSearchNetworkDataSource(musicApi, registration, responses, calls, origins),
            playback = RealPlaybackNetworkDataSource(musicApi, registration, signer, calls, responses),
            registration = registration,
        )
    }

    private suspend fun registeredLiveFixture(): LiveFixture {
        cachedFixture?.let { return it }
        var lastFailure: ApiProtocolException? = null
        repeat(MAX_REGISTRATION_ATTEMPTS) {
            val fixture = liveFixture()
            try {
                fixture.registration.ensureRegisteredSession()
                cachedFixture = fixture
                return fixture
            } catch (failure: ApiProtocolException) {
                if (failure.reason != ApiProtocolException.Reason.MissingRequiredField) throw failure
                lastFailure = failure
            }
        }
        throw checkNotNull(lastFailure)
    }

    private fun roundRobinCandidates(
        pools: List<List<com.resonote.core.network.model.NetworkSong>>,
        limit: Int,
    ): List<com.resonote.core.network.model.NetworkSong> {
        val orderedPools =
            pools.map { pool ->
                pool.distinctBy { it.hash }
                    .shuffled()
                    .sortedWith(
                        compareBy(
                            com.resonote.core.network.model.NetworkSong::vip,
                            com.resonote.core.network.model.NetworkSong::losslessAvailable,
                            com.resonote.core.network.model.NetworkSong::highQualityAvailable,
                        ),
                    )
            }
        return sequence {
            repeat(orderedPools.maxOfOrNull { it.size } ?: 0) { index ->
                orderedPools.forEach { pool -> pool.getOrNull(index)?.let { yield(it) } }
            }
        }.distinctBy { it.hash }.take(limit).toList()
    }

    private data class LiveFixture(
        val dataSource: LivePublicDataSource,
        val search: RealSearchNetworkDataSource,
        val playback: RealPlaybackNetworkDataSource,
        val registration: DeviceRegistrationCoordinator,
    )

    private class LivePublicDataSource(
        home: RealHomeNetworkDataSource,
        catalog: RealCatalogNetworkDataSource,
        ranking: RealRankingNetworkDataSource,
        playlist: RealPlaylistNetworkDataSource,
    ) : com.resonote.core.network.HomeNetworkDataSource by home,
        com.resonote.core.network.CatalogNetworkDataSource by catalog,
        com.resonote.core.network.RankingNetworkDataSource by ranking,
        com.resonote.core.network.PlaylistNetworkDataSource by playlist

    private suspend fun <T> liveStep(label: String, block: suspend () -> T): T = try {
        block()
    } catch (failure: Throwable) {
        throw AssertionError("$label failed with ${failure::class.simpleName}", failure)
    }

    private companion object {
        const val MAX_REGISTRATION_ATTEMPTS = 5
        const val ANONYMOUS_SEARCH_REQUIRES_AUTHENTICATION = "152"

        @Volatile
        var cachedFixture: LiveFixture? = null
    }

    private class MemoryStore : ApiSessionStore {
        private val state = MutableStateFlow<ApiSession?>(null)
        override val session = state
        override suspend fun read() = state.value
        override suspend fun write(session: ApiSession) {
            state.value = session
        }
        override suspend fun clearAuthentication() {
            state.value = null
        }
    }
}

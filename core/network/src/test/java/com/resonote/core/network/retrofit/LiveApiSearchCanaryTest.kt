package com.resonote.core.network.retrofit

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.api.MusicApi
import com.resonote.core.network.protocol.ProtocolTransport
import com.resonote.core.network.protocol.ApiDeviceIdentityFactory
import com.resonote.core.network.protocol.DeviceRegistrationCoordinator
import com.resonote.core.network.protocol.DeviceRegistrationProfile
import com.resonote.core.network.protocol.DeviceRegistrationProfileProvider
import com.resonote.core.network.protocol.ApiEndpointOrigins
import com.resonote.core.network.protocol.ApiProtocolCrypto
import com.resonote.core.network.protocol.ApiDefaultsInterceptor
import com.resonote.core.network.protocol.ApiResponseMetadataInterceptor
import com.resonote.core.network.protocol.ApiSigningInterceptor
import com.resonote.core.network.protocol.ApiRequestSigner
import com.resonote.core.network.protocol.ProductionApiOriginPolicy
import com.resonote.core.network.protocol.ProtocolRandom
import com.resonote.core.network.protocol.MobileAuthProtocolClient
import com.resonote.core.network.risk.ApiRiskChallengeDetector
import com.resonote.core.network.session.ApiSession
import com.resonote.core.network.session.ApiSessionManager
import com.resonote.core.network.session.ApiSessionStore
import java.security.SecureRandom
import java.time.Clock
import java.util.Optional
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assume.assumeTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class LiveApiSearchCanaryTest {
    @Test
    fun registeredLiteDeviceCanSearch() = runTest {
        assumeTrue(System.getenv("RESONOTE_RUN_LIVE_API_TESTS") == "true")
        val fixture = registeredLiveFixture()

        val result =
            try {
                fixture.dataSource.searchSongs("周杰伦", page = 1, pageSize = 1)
            } catch (failure: ApiServiceException) {
                assumeTrue(
                    "Anonymous search requires an authenticated cookie when the service returns code 152",
                    failure.serviceCode != ANONYMOUS_SEARCH_REQUIRES_AUTHENTICATION,
                )
                throw failure
            }

        assertThat(result.items).isNotEmpty()
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
            val attempt = runCatching { dataSource.resolveSongSource(song.hash, song.albumId, song.albumAudioId) }
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
        assertThat(resolved?.uri).startsWith("https://")
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
            { client }, json, clock, signer, sessions, riskDetector, ProductionApiOriginPolicy(),
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
        val mobileAuth = MobileAuthProtocolClient(executor, registration, json, crypto, signer, origins)
        return LiveFixture(
            dataSource = RealApiNetworkDataSource(
                musicApi,
                registration,
                mobileAuth,
                signer,
                clock,
                riskDetector,
            ),
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
        val dataSource: RealApiNetworkDataSource,
        val registration: DeviceRegistrationCoordinator,
    )

    private suspend fun <T> liveStep(label: String, block: suspend () -> T): T =
        try {
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
        override suspend fun write(session: ApiSession) { state.value = session }
        override suspend fun clearAuthentication() { state.value = null }
    }
}

package com.resonote.core.network.retrofit

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.resonote.core.network.protocol.ApiCallExecutor
import com.resonote.core.network.protocol.ApiDeviceIdentityFactory
import com.resonote.core.network.protocol.ApiEndpointOrigins
import com.resonote.core.network.protocol.ApiProtocolCrypto
import com.resonote.core.network.protocol.ApiRequestSigner
import com.resonote.core.network.protocol.ProductionApiOriginPolicy
import com.resonote.core.network.protocol.ProtocolRandom
import com.resonote.core.network.risk.ApiRiskChallengeDetector
import com.resonote.core.network.risk.RiskAwareApiExecutor
import com.resonote.core.network.session.ApiSession
import com.resonote.core.network.session.ApiSessionManager
import com.resonote.core.network.session.ApiSessionStore
import dagger.Lazy
import java.security.SecureRandom
import java.time.Clock
import java.util.Optional
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.Assume.assumeTrue
import org.junit.Test

class LiveApiSearchCanaryTest {
    @Test
    fun registeredLiteDeviceCanSearch() = runTest {
        assumeTrue(System.getenv("RESONOTE_RUN_LIVE_API_TESTS") == "true")
        val dataSource = liveDataSource()

        val result = dataSource.searchSongs("周杰伦", page = 1, pageSize = 1)

        assertThat(result.items).isNotEmpty()
    }

    @Test
    fun publicHomeEndpointsAndPlaybackUrlAreConsumable() = runTest {
        assumeTrue(System.getenv("RESONOTE_RUN_LIVE_API_TESTS") == "true")
        val dataSource = liveDataSource()

        val daily = dataSource.dailyRecommendations()
        val radio = dataSource.radioRecommendations(com.resonote.core.network.model.NetworkRecommendationMode.Personal)
        val playlists = dataSource.recommendedPlaylists(page = 1, pageSize = 6)
        val newSongs = dataSource.newSongs(page = 1, pageSize = 6)

        assertThat(daily).isNotEmpty()
        assertThat(radio).isNotEmpty()
        assertThat(playlists).isNotEmpty()
        assertThat(newSongs).isNotEmpty()
        val candidates =
            (daily + radio + newSongs)
                .distinctBy { it.hash }
                .sortedBy { it.vip }
                .take(5)
        var resolved: com.resonote.core.network.model.NetworkSongSource? = null
        val failureKinds = mutableListOf<String>()
        for (song in candidates) {
            val attempt = runCatching { dataSource.resolveSongSource(song.hash, song.albumId, song.albumAudioId) }
            resolved = attempt.getOrNull()
            attempt.exceptionOrNull()?.let { failure ->
                failureKinds +=
                    if (failure is com.resonote.core.network.ApiPlaybackUnavailableException) {
                        "ApiPlaybackUnavailableException.${failure.reason}"
                    } else {
                        failure::class.simpleName.orEmpty()
                    }
            }
            if (resolved != null) break
        }

        assertWithMessage("Playback candidate failures: $failureKinds").that(resolved).isNotNull()
        assertThat(resolved?.uri).startsWith("https://")
    }

    private fun liveDataSource(): RealApiNetworkDataSource {
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        val sessions = ApiSessionManager(Optional.of(MemoryStore()), ApiDeviceIdentityFactory())
        val risk = RiskAwareApiExecutor(ApiRiskChallengeDetector(), Optional.empty())
        val signer = ApiRequestSigner()
        val random = ProtocolRandom { length ->
            val alphabet = "1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            val secureRandom = SecureRandom()
            buildString(length) { repeat(length) { append(alphabet[secureRandom.nextInt(alphabet.length)]) } }
        }
        val crypto = ApiProtocolCrypto(random)
        val executor = ApiCallExecutor(
            OkHttpClient(), json, Clock.systemUTC(), signer, sessions, Lazy { risk }, ProductionApiOriginPolicy(),
        )
        return RealApiNetworkDataSource(executor, json, crypto, signer, sessions, ApiEndpointOrigins())
    }

    private class MemoryStore : ApiSessionStore {
        private val state = MutableStateFlow<ApiSession?>(null)
        override val session = state
        override suspend fun read() = state.value
        override suspend fun write(session: ApiSession) { state.value = session }
        override suspend fun clearAuthentication() { state.value = null }
    }
}

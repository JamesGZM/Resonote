package com.resonote.core.network.retrofit

import com.google.common.truth.Truth.assertThat
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
        val dataSource = RealApiNetworkDataSource(executor, json, crypto, signer, sessions, ApiEndpointOrigins())

        val result = dataSource.searchSongs("周杰伦", page = 1, pageSize = 1)

        assertThat(result.items).isNotEmpty()
        assertThat(sessions.current().dfid).isNotEmpty()
    }

    private class MemoryStore : ApiSessionStore {
        private val state = MutableStateFlow<ApiSession?>(null)
        override val session = state
        override suspend fun read() = state.value
        override suspend fun write(session: ApiSession) { state.value = session }
        override suspend fun clearAuthentication() { state.value = null }
    }
}

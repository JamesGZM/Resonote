package com.resonote.core.network.risk

import com.google.common.truth.Truth.assertThat
import com.resonote.core.network.protocol.ApiCallExecutor
import com.resonote.core.network.protocol.ApiDeviceIdentityFactory
import com.resonote.core.network.protocol.ApiEndpointOrigins
import com.resonote.core.network.protocol.ApiOriginPolicy
import com.resonote.core.network.protocol.ApiProtocolCrypto
import com.resonote.core.network.protocol.ApiRequestSigner
import com.resonote.core.network.protocol.ProtocolRandom
import com.resonote.core.network.session.ApiSession
import com.resonote.core.network.session.ApiSessionManager
import com.resonote.core.network.session.ApiSessionStore
import dagger.Lazy
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class RealApiRiskGatewayTest {
    private lateinit var server: MockWebServer
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun startServer() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun stopServer() {
        server.shutdown()
    }

    @Test
    fun headerOnlyChallengeGetsContextAndVerificationEndpointBypassesCoordinator() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":1}"""))
        val clock = Clock.fixed(Instant.ofEpochMilli(1_700_000_000_123), ZoneOffset.UTC)
        val session = ApiSession("guid", "mid", "dev", dfid = "dfid", userId = "42")
        val sessions = ApiSessionManager(Optional.of(MemoryStore(session)), ApiDeviceIdentityFactory())
        val riskExecutor = RiskAwareApiExecutor(ApiRiskChallengeDetector(), Optional.empty())
        val executor = ApiCallExecutor(
            OkHttpClient(), json, clock, ApiRequestSigner(), sessions, Lazy { riskExecutor }, ApiOriginPolicy { true },
        )
        val origin = server.url("/").toString().removeSuffix("/")
        val gateway = RealApiRiskGateway(
            executor,
            ApiProtocolCrypto(ProtocolRandom { length -> "A".repeat(length) }),
            ApiEndpointOrigins(origin, origin, origin, origin, origin),
            ApiRiskContextFactory(clock),
        )

        gateway.submit(ApiRiskChallenge("event"), ApiRiskProof.Sms("246810"))

        val request = server.takeRequest()
        val body = json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertThat(request.path).contains("/v4/verify_user_info?")
        assertThat(request.requestUrl?.queryParameter("clientver")).isEqualTo("11510")
        assertThat(body["v_type"]?.jsonPrimitive?.content).isEqualTo("32")
        assertThat(body["code"]?.jsonPrimitive?.content).isEqualTo("246810")
        assertThat(body["sid"]?.jsonPrimitive?.content).isNotEmpty()
        assertThat(body["edt"]?.jsonPrimitive?.content).isNotEmpty()
    }

    private class MemoryStore(initial: ApiSession?) : ApiSessionStore {
        private val state = MutableStateFlow(initial)
        override val session = state
        override suspend fun read() = state.value
        override suspend fun write(session: ApiSession) { state.value = session }
        override suspend fun clearAuthentication() { state.value = null }
    }
}

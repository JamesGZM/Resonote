package com.resonote.core.network.risk

import com.google.common.truth.Truth.assertThat
import com.resonote.core.network.protocol.ProtocolTransport
import com.resonote.core.network.protocol.ApiDeviceIdentityFactory
import com.resonote.core.network.protocol.ApiEndpointOrigins
import com.resonote.core.network.protocol.ApiOriginPolicy
import com.resonote.core.network.protocol.ApiProtocolCrypto
import com.resonote.core.network.protocol.ApiRequestSigner
import com.resonote.core.network.protocol.ProtocolRandom
import com.resonote.core.network.session.ApiSession
import com.resonote.core.network.session.ApiSessionManager
import com.resonote.core.network.session.ApiSessionStore
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import java.util.concurrent.TimeUnit
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

class RealApiRiskVerificationServiceTest {
    private lateinit var gatewayServer: MockWebServer
    private lateinit var mobileCodeServer: MockWebServer
    private lateinit var mobileLoginServer: MockWebServer
    private lateinit var deviceRegistrationServer: MockWebServer
    private lateinit var riskVerificationServer: MockWebServer
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun startServers() {
        gatewayServer = MockWebServer().apply { start() }
        mobileCodeServer = MockWebServer().apply { start() }
        mobileLoginServer = MockWebServer().apply { start() }
        deviceRegistrationServer = MockWebServer().apply { start() }
        riskVerificationServer = MockWebServer().apply { start() }
    }

    @After
    fun stopServers() {
        gatewayServer.shutdown()
        mobileCodeServer.shutdown()
        mobileLoginServer.shutdown()
        deviceRegistrationServer.shutdown()
        riskVerificationServer.shutdown()
    }

    @Test
    fun submitUsesIsolatedRiskOriginAndBypassesRecursiveVerification() {
        listOf(gatewayServer, mobileCodeServer, mobileLoginServer, deviceRegistrationServer).forEach {
            it.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":1}"""))
        }
        riskVerificationServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"status":1,"error_code":20028,"ssaCode":"must-not-recurse"}"""),
        )
        val clock = Clock.fixed(Instant.ofEpochMilli(1_700_000_000_123), ZoneOffset.UTC)
        val session = ApiSession("guid", "mid", "dev", dfid = "dfid", userId = "42")
        val sessions = ApiSessionManager(Optional.of(MemoryStore(session)), ApiDeviceIdentityFactory())
        val executor = ProtocolTransport(
            { OkHttpClient() }, json, clock, ApiRequestSigner(), sessions, ApiRiskChallengeDetector(), ApiOriginPolicy { true },
        )
        val service = RealApiRiskVerificationService(
            executor,
            ApiProtocolCrypto(ProtocolRandom { length -> "A".repeat(length) }),
            ApiEndpointOrigins(
                gateway = gatewayServer.origin(),
                mobileCode = mobileCodeServer.origin(),
                mobileLogin = mobileLoginServer.origin(),
                deviceRegistration = deviceRegistrationServer.origin(),
                riskVerification = riskVerificationServer.origin(),
            ),
            ApiRiskContextFactory(clock),
        )

        runTest { service.submit(ApiRiskChallenge("event"), ApiRiskProof.Sms("246810")) }

        val request = checkNotNull(riskVerificationServer.takeRequest(1, TimeUnit.SECONDS)) {
            "Risk verification request was sent to the wrong origin"
        }
        val body = json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertThat(request.path).contains("/v4/verify_user_info?")
        assertThat(request.requestUrl?.queryParameter("clientver")).isEqualTo("11510")
        assertThat(body["v_type"]?.jsonPrimitive?.content).isEqualTo("32")
        assertThat(body["code"]?.jsonPrimitive?.content).isEqualTo("246810")
        assertThat(body["sid"]?.jsonPrimitive?.content).isNotEmpty()
        assertThat(body["edt"]?.jsonPrimitive?.content).isNotEmpty()
        assertThat(gatewayServer.requestCount).isEqualTo(0)
        assertThat(mobileCodeServer.requestCount).isEqualTo(0)
        assertThat(mobileLoginServer.requestCount).isEqualTo(0)
        assertThat(deviceRegistrationServer.requestCount).isEqualTo(0)
    }

    private fun MockWebServer.origin(): String = url("/").toString().removeSuffix("/")

    private class MemoryStore(initial: ApiSession?) : ApiSessionStore {
        private val state = MutableStateFlow(initial)
        override val session = state
        override suspend fun read() = state.value
        override suspend fun write(session: ApiSession) { state.value = session }
        override suspend fun clearAuthentication() { state.value = null }
    }
}

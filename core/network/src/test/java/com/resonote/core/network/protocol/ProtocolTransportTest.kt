package com.resonote.core.network.protocol

import com.google.common.truth.Truth.assertThat
import com.resonote.core.network.ApiRiskException
import com.resonote.core.network.retrofit.ApiRawResponse
import com.resonote.core.network.risk.ApiRiskChallengeDetector
import com.resonote.core.network.session.ApiSession
import com.resonote.core.network.session.ApiSessionManager
import com.resonote.core.network.session.ApiSessionStore
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Optional
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class ProtocolTransportTest {
    private lateinit var server: MockWebServer

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
    fun riskChallengeIsSurfacedWithoutAutomaticReplay() {
        server.enqueue(jsonResponse("""{"status":0,"error_code":20028,"ssaCode":"event"}"""))
        val session = ApiSession("guid", "mid", "dev", dfid = "dfid")
        val sessions = ApiSessionManager(Optional.of(MemoryStore(session)), ApiDeviceIdentityFactory())
        val executor = ProtocolTransport(
            { OkHttpClient() },
            Json { ignoreUnknownKeys = true },
            StepClock(1_700_000_000_000),
            ApiRequestSigner(),
            sessions,
            ApiRiskChallengeDetector(),
            ApiOriginPolicy { true },
        )

        val failure = org.junit.Assert.assertThrows(ApiRiskException::class.java) {
            runTest {
                val ignored = executor.execute { _, _ ->
                    ApiExchange(
                        ApiEndpointSpec(
                            id = "test-no-replay",
                            origin = server.url("/").toString().removeSuffix("/"),
                            path = "/risk",
                            method = ApiHttpMethod.Get,
                        ),
                        decode = ApiRawResponse::statusCode,
                    )
                }
                assertThat(ignored).isEqualTo(200)
            }
        }

        assertThat(failure.challenge.eventId).isEqualTo("event")
        assertThat(failure.reason).isEqualTo(ApiRiskException.Reason.VerificationUnavailable)
        assertThat(server.requestCount).isEqualTo(1)
    }

    private fun jsonResponse(body: String) =
        MockResponse().setResponseCode(200).addHeader("Content-Type", "application/json").setBody(body)

    private class StepClock(private var currentMillis: Long) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = Instant.ofEpochMilli(currentMillis).also { currentMillis += 1_000 }
    }

    private class MemoryStore(initial: ApiSession?) : ApiSessionStore {
        private val state = MutableStateFlow(initial)
        override val session = state
        override suspend fun read() = state.value
        override suspend fun write(session: ApiSession) { state.value = session }
        override suspend fun clearAuthentication() { state.value = null }
    }
}

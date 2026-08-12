package com.resonote.core.network.protocol

import com.google.common.truth.Truth.assertThat
import com.resonote.core.network.ApiRiskException
import com.resonote.core.network.ApiAuthenticationRequiredException
import com.resonote.core.network.ApiHttpException
import com.resonote.core.network.risk.ApiRiskChallengeDetector
import com.resonote.core.network.session.ApiSession
import com.resonote.core.network.session.ApiSessionManager
import com.resonote.core.network.session.ApiSessionStore
import com.resonote.core.network.session.ApiAuthenticationGateReason
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

    @Test
    fun fullSessionProtocolHttpUnauthorizedExpiresTheSession() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        val session = ApiSession("guid", "mid", "dev", dfid = "dfid", token = "token", userId = "42")
        val executor = transport(session)

        val failure = runCatching {
            executor.execute { _, _ ->
                ApiExchange(
                    ApiEndpointSpec("test-authenticated", origin = server.origin(), path = "/user", method = ApiHttpMethod.Get),
                    ApiRawResponse::statusCode,
                )
            }
        }.exceptionOrNull() as ApiAuthenticationRequiredException

        assertThat(failure.reason).isEqualTo(ApiAuthenticationGateReason.SessionExpired)
    }

    @Test
    fun deviceOnlyProtocolHttpForbiddenRemainsAnOrdinaryHttpFailure() = runTest {
        server.enqueue(MockResponse().setResponseCode(403))
        val executor = transport(ApiSession("guid", "mid", "dev", dfid = "dfid"))

        val failure = runCatching {
            executor.execute { _, _ ->
                ApiExchange(
                    ApiEndpointSpec(
                        "test-login",
                        origin = server.origin(),
                        path = "/login",
                        method = ApiHttpMethod.Get,
                        sessionPropagation = ApiSessionPropagation.DeviceOnly,
                    ),
                    ApiRawResponse::statusCode,
                )
            }
        }.exceptionOrNull() as ApiHttpException

        assertThat(failure.statusCode).isEqualTo(403)
    }

    private fun transport(session: ApiSession): ProtocolTransport {
        val sessions = ApiSessionManager(Optional.of(MemoryStore(session)), ApiDeviceIdentityFactory())
        return ProtocolTransport(
            { OkHttpClient() },
            Json { ignoreUnknownKeys = true },
            StepClock(1_700_000_000_000),
            ApiRequestSigner(),
            sessions,
            ApiRiskChallengeDetector(),
            ApiOriginPolicy { true },
        )
    }

    private fun MockWebServer.origin(): String = url("/").toString().removeSuffix("/")

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

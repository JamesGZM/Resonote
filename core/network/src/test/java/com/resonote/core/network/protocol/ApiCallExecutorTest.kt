package com.resonote.core.network.protocol

import com.google.common.truth.Truth.assertThat
import com.resonote.core.network.retrofit.ApiRawResponse
import com.resonote.core.network.risk.ApiRiskChallengeDetector
import com.resonote.core.network.risk.ApiRiskVerificationResult
import com.resonote.core.network.risk.ApiRiskVerifier
import com.resonote.core.network.risk.RiskAwareApiExecutor
import com.resonote.core.network.session.ApiSession
import com.resonote.core.network.session.ApiSessionManager
import com.resonote.core.network.session.ApiSessionStore
import dagger.Lazy
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

class ApiCallExecutorTest {
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
    fun verifiedRiskReplayRebuildsTimestampAndSignature() = runTest {
        server.enqueue(jsonResponse("""{"status":0,"error_code":20028,"ssaCode":"event"}"""))
        server.enqueue(jsonResponse("""{"status":1}"""))
        val session = ApiSession("guid", "mid", "dev", dfid = "dfid")
        val sessions = ApiSessionManager(Optional.of(MemoryStore(session)), ApiDeviceIdentityFactory())
        val risk = RiskAwareApiExecutor(
            ApiRiskChallengeDetector(),
            Optional.of(ApiRiskVerifier { ApiRiskVerificationResult.Verified }),
        )
        val executor = ApiCallExecutor(
            OkHttpClient(),
            Json { ignoreUnknownKeys = true },
            StepClock(1_700_000_000_000),
            ApiRequestSigner(),
            sessions,
            Lazy { risk },
            ApiOriginPolicy { true },
        )

        val result = executor.execute { _, _ ->
            ApiExchange(
                ApiEndpointSpec(
                    id = "test-replay",
                    origin = server.url("/").toString().removeSuffix("/"),
                    path = "/replay",
                    method = ApiHttpMethod.Get,
                ),
                decode = ApiRawResponse::statusCode,
            )
        }

        assertThat(result).isEqualTo(200)
        val first = server.takeRequest().requestUrl!!
        val second = server.takeRequest().requestUrl!!
        assertThat(first.queryParameter("clienttime")).isEqualTo("1700000000")
        assertThat(second.queryParameter("clienttime")).isEqualTo("1700000001")
        assertThat(second.queryParameter("signature")).isNotEqualTo(first.queryParameter("signature"))
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

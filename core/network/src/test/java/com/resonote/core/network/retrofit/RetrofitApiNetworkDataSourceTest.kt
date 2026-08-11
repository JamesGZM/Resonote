package com.resonote.core.network.retrofit

import com.google.common.truth.Truth.assertThat
import com.resonote.core.network.ApiHttpException
import com.resonote.core.network.ApiNetworkException
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.protocol.ApiDeviceIdentity
import com.resonote.core.network.protocol.ApiProtocolInterceptor
import com.resonote.core.network.protocol.ApiRequestSigner
import com.resonote.core.network.risk.ApiRiskChallengeDetector
import com.resonote.core.network.risk.ApiRiskVerificationResult
import com.resonote.core.network.risk.ApiRiskVerifier
import com.resonote.core.network.risk.RiskAwareApiExecutor
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Optional
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class RetrofitApiNetworkDataSourceTest {
    private lateinit var server: MockWebServer
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun searchBuildsSignedRequestAndTolerantlyMapsConfirmedFields() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(testResource("search_response_synthetic.json")))
        val dataSource = dataSource(clock = Clock.fixed(Instant.ofEpochSecond(1_700_000_000), ZoneOffset.UTC))

        val page = dataSource.searchSongsSignedCanary("  synthetic query  ", page = 2, pageSize = 5)
        val request = server.takeRequest()

        assertThat(request.method).isEqualTo("GET")
        assertThat(request.requestUrl?.encodedPath).isEqualTo("/v3/search/song")
        assertThat(request.requestUrl?.queryParameter("keyword")).isEqualTo("synthetic query")
        assertThat(request.requestUrl?.queryParameter("page")).isEqualTo("2")
        assertThat(request.requestUrl?.queryParameter("pagesize")).isEqualTo("5")
        assertThat(request.requestUrl?.queryParameter("appid")).isEqualTo("3116")
        assertThat(request.requestUrl?.queryParameter("clientver")).isEqualTo("11440")
        assertThat(request.requestUrl?.queryParameter("signature")).hasLength(32)
        assertThat(request.getHeader("x-router")).isEqualTo("complexsearch.kugou.com")
        assertThat(page.total).isEqualTo(2)
        assertThat(page.items).hasSize(2)
        assertThat(page.items.first().title).isEqualTo("Synthetic Song")
        assertThat(page.items.first().durationSeconds).isEqualTo(245)
        assertThat(page.items.first().losslessHash).isEqualTo("hash-lossless")
    }

    @Test
    fun verifiedRiskRebuildsTimestampAndSignatureBeforeSingleRetry() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"status":0,"error_code":20028,"ssaCode":"event"}""",
            ),
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody(testResource("search_response_synthetic.json")))
        val clock = SteppingClock(1_700_000_000)
        val verifier = ApiRiskVerifier { ApiRiskVerificationResult.Verified }
        val dataSource = dataSource(clock = clock, verifier = verifier)

        dataSource.searchSongsSignedCanary("query")
        val first = server.takeRequest().requestUrl
        val second = server.takeRequest().requestUrl

        assertThat(first?.queryParameter("clienttime")).isEqualTo("1700000000")
        assertThat(second?.queryParameter("clienttime")).isEqualTo("1700000001")
        assertThat(first?.queryParameter("signature")).isNotEqualTo(second?.queryParameter("signature"))
    }

    @Test
    fun httpAndMalformedResponsesRemainDistinctFailures() = runTest {
        server.enqueue(MockResponse().setResponseCode(503).setBody("""{"status":0}"""))
        val dataSource = dataSource(Clock.systemUTC())
        val http = expectFailure<ApiHttpException> { dataSource.searchSongsSignedCanary("query") }
        assertThat(http.statusCode).isEqualTo(503)

        server.enqueue(MockResponse().setResponseCode(200).setBody("not-json"))
        val malformed = expectFailure<ApiProtocolException> { dataSource.searchSongsSignedCanary("query") }
        assertThat(malformed.reason).isEqualTo(ApiProtocolException.Reason.MalformedResponse)
    }

    @Test
    fun timeoutIsClassifiedWithoutRiskRetry() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val dataSource = dataSource(Clock.systemUTC(), readTimeoutMillis = 100)

        val failure = expectFailure<ApiNetworkException> { dataSource.searchSongsSignedCanary("query") }

        assertThat(failure.kind).isEqualTo(ApiNetworkException.Kind.Timeout)
        assertThat(server.requestCount).isEqualTo(1)
    }

    private fun dataSource(
        clock: Clock,
        verifier: ApiRiskVerifier? = null,
        readTimeoutMillis: Long = 10_000,
    ): RetrofitApiNetworkDataSource {
        val identity = ApiDeviceIdentity(guid = "fixture-guid", mid = "123456")
        val client =
            OkHttpClient.Builder()
                .addInterceptor(ApiProtocolInterceptor(clock, identity, ApiRequestSigner()))
                .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
                .build()
        val retrofit = Retrofit.Builder().baseUrl(server.url("/")).client(client).build()
        val executor =
            RiskAwareApiExecutor(
                ApiRiskChallengeDetector(),
                Optional.ofNullable(verifier),
            )
        return RetrofitApiNetworkDataSource(retrofit, json, executor)
    }

    private fun testResource(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource(name)).readText()

    private suspend inline fun <reified T : Throwable> expectFailure(noinline block: suspend () -> Unit): T =
        try {
            block()
            throw AssertionError("Expected ${T::class.java.simpleName}")
        } catch (throwable: Throwable) {
            if (throwable is T) throwable else throw throwable
        }

    private class SteppingClock(startEpochSecond: Long) : Clock() {
        private var next = startEpochSecond

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = Instant.ofEpochSecond(next++)
    }
}

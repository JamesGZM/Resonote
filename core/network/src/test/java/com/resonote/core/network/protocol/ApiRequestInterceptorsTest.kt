package com.resonote.core.network.protocol

import com.google.common.truth.Truth.assertThat
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.api.model.ApiResponse
import com.resonote.core.network.session.ApiSession
import com.resonote.core.network.session.ApiSessionManager
import com.resonote.core.network.session.ApiSessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional

class ApiRequestInterceptorsTest {
    private lateinit var server: MockWebServer
    private val json = Json { ignoreUnknownKeys = true }
    private val clock = Clock.fixed(Instant.ofEpochSecond(1_700_000_000), ZoneOffset.UTC)
    private val session =
        ApiSession(
            guid = "guid",
            mid = "mid",
            dev = "dev",
            dfid = "dfid",
            token = "token",
            userId = "42",
            cookies = mapOf("token" to "token", "userid" to "42"),
        )

    @Before
    fun startServer() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun stopServer() {
        server.shutdown()
    }

    @Test
    fun retrofitPolicyReceivesSessionDefaultsCookieAndSignature() = runTest {
        server.enqueue(jsonResponse("""{"status":1,"data":{"value":"ok"}}"""))
        val api = fixture().api

        api.signed(page = 2, body = TestBody(1))

        val recorded = server.takeRequest()
        assertThat(recorded.requestUrl?.queryParameter("page")).isEqualTo("2")
        assertThat(recorded.requestUrl?.queryParameter("dfid")).isEqualTo("dfid")
        assertThat(recorded.requestUrl?.queryParameter("token")).isEqualTo("token")
        assertThat(recorded.requestUrl?.queryParameter("userid")).isEqualTo("42")
        assertThat(recorded.requestUrl?.queryParameter("clienttime")).isEqualTo("1700000000")
        assertThat(recorded.requestUrl?.queryParameter("signature")).isNotEmpty()
        assertThat(recorded.getHeader("Cookie")).isEqualTo("token=token; userid=42")
        assertThat(recorded.getHeader("mid")).isEqualTo("mid")
        assertThat(recorded.getHeader("x-router")).isEqualTo("songs.service.kugou.com")
        assertThat(recorded.getHeader("kg-tid")).isEqualTo("255")
        assertThat(recorded.body.readUtf8()).isEqualTo("""{"value":1}""")
        val signedUrl = checkNotNull(recorded.requestUrl)
        val unsignedQuery =
            signedUrl.queryParameterNames
                .filterNot { it == "signature" }
                .associateWith { signedUrl.queryParameter(it).orEmpty() }
        assertThat(signedUrl.queryParameter("signature"))
            .isEqualTo(ApiRequestSigner().sign(unsignedQuery, """{"value":1}""".encodeToByteArray()))
    }

    @Test
    fun fullPropagationUsesCachedCookiesWithoutReconstructingThem() = runTest {
        server.enqueue(jsonResponse("""{"status":1,"data":{"value":"ok"}}"""))
        fixture(session.copy(cookies = mapOf("dfid" to "dfid"))).api.signed(1, TestBody(1))
        val authenticatedRequest = server.takeRequest()

        server.enqueue(jsonResponse("""{"status":1,"data":{"value":"ok"}}"""))
        fixture(
            session.copy(
                token = null,
                userId = null,
                cookies = mapOf("dfid" to "dfid", "TOKEN" to "stale", "userid" to "42", "t1" to "stale"),
            ),
        ).api.signed(1, TestBody(1))
        val anonymousRequest = server.takeRequest()

        assertThat(authenticatedRequest.getHeader("Cookie")).isEqualTo("dfid=dfid")
        assertThat(anonymousRequest.getHeader("Cookie"))
            .isEqualTo("dfid=dfid; TOKEN=stale; userid=42; t1=stale")
    }

    @Test
    fun unannotatedRequestPassesThroughWithoutProtocolFields() = runTest {
        server.enqueue(jsonResponse("""{"status":1,"data":{"value":"ok"}}"""))

        fixture().api.plain()

        val recorded = server.takeRequest()
        assertThat(recorded.requestUrl?.queryParameter("signature")).isNull()
        assertThat(recorded.getHeader("Cookie")).isNull()
    }

    @Test
    fun noneModesDoNotInjectSessionOrSignature() = runTest {
        server.enqueue(jsonResponse("""{"status":1,"data":{"value":"ok"}}"""))

        fixture().api.unsigned()

        val recorded = server.takeRequest()
        assertThat(recorded.requestUrl?.queryParameter("existing")).isEqualTo("1")
        assertThat(recorded.requestUrl?.queryParameter("dfid")).isNull()
        assertThat(recorded.requestUrl?.queryParameter("signature")).isNull()
        assertThat(recorded.getHeader("mid")).isNull()
    }

    @Test
    fun responseHeaderRiskEventIsNormalizedIntoTypedEnvelope() = runTest {
        server.enqueue(
            jsonResponse(
                """{"status":0,"error_code":20028,"data":{"value":"blocked"}}""",
            ).addHeader("ssa-code", "event-id"),
        )

        val response = fixture().api.signed(page = 1, body = TestBody(1))

        assertThat(response.ssaCode).isEqualTo("event-id")
        assertThat(response.errorCode).isEqualTo("20028")
    }

    @Test
    fun responseMetadataRejectsKnownOversizedBody() {
        server.enqueue(
            MockResponse()
                .addHeader("Content-Type", "application/json")
                .addHeader("ssa-code", "event-id")
                .setBody("x".repeat(MAX_METADATA_BYTES + 1)),
        )

        assertThrows(ApiProtocolException::class.java) {
            runTest { fixture().api.signed(page = 1, body = TestBody(1)) }
        }
    }

    @Test
    fun responseMetadataRejectsChunkedOversizedBodyWithoutUnboundedRead() {
        server.enqueue(
            MockResponse()
                .addHeader("Content-Type", "application/json")
                .addHeader("ssa-code", "event-id")
                .setChunkedBody("x".repeat(MAX_METADATA_BYTES + 1), 1_024),
        )

        assertThrows(ApiProtocolException::class.java) {
            runTest { fixture().api.signed(page = 1, body = TestBody(1)) }
        }
    }

    @Test
    fun rawOkHttpRequestDoesNotReceiveRetrofitProtocol() {
        server.enqueue(MockResponse().setBody("{}"))
        val fixture = fixture()

        fixture.client.newCall(Request.Builder().url(server.url("/plain?existing=1")).build()).execute().close()

        val recorded = server.takeRequest()
        assertThat(recorded.requestUrl?.queryParameter("existing")).isEqualTo("1")
        assertThat(recorded.requestUrl?.queryParameter("signature")).isNull()
    }

    @Test
    fun policyHeaderRejectsConflictingRetrofitHeader() {
        assertThrows(IllegalArgumentException::class.java) {
            fixture().api.conflictingRouter("other.service.kugou.com").execute()
        }
    }

    private fun fixture(initialSession: ApiSession = session): Fixture {
        val sessions = ApiSessionManager(Optional.of(MemoryStore(initialSession)), ApiDeviceIdentityFactory())
        runBlocking { sessions.current() }
        val client =
            OkHttpClient.Builder()
                .retryOnConnectionFailure(false)
                .addInterceptor(ApiDefaultsInterceptor(clock, sessions))
                .addInterceptor(ApiSigningInterceptor(ApiRequestSigner()))
                .addInterceptor(ApiResponseMetadataInterceptor(json))
                .build()
        val api =
            Retrofit.Builder()
                .baseUrl(server.url("/"))
                .callFactory(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(TestApi::class.java)
        return Fixture(client, api)
    }

    private fun jsonResponse(body: String) =
        MockResponse().setResponseCode(200).addHeader("Content-Type", "application/json").setBody(body)

    private interface TestApi {
        @ApiRequestPolicy(router = "songs.service.kugou.com", kgTid = 255)
        @POST("songs")
        suspend fun signed(@Query("page") page: Int, @Body body: TestBody): ApiResponse<TestData>

        @ApiRequestPolicy(router = "songs.service.kugou.com")
        @GET("conflict")
        fun conflictingRouter(@Header("x-router") router: String): Call<ApiResponse<TestData>>

        @GET("plain")
        suspend fun plain(): ApiResponse<TestData>

        @ApiRequestPolicy(
            signatureMode = ApiSignatureMode.None,
            sessionPropagation = ApiSessionPropagation.None,
            includeDefaultParams = false,
        )
        @GET("plain?existing=1")
        suspend fun unsigned(): ApiResponse<TestData>
    }

    @Serializable private data class TestBody(val value: Int)

    @Serializable private data class TestData(val value: String)
    private data class Fixture(val client: OkHttpClient, val api: TestApi)

    private class MemoryStore(initial: ApiSession?) : ApiSessionStore {
        private val state = MutableStateFlow(initial)
        override val session = state
        override suspend fun read() = state.value
        override suspend fun write(session: ApiSession) {
            state.value = session
        }
        override suspend fun clearAuthentication() {
            state.value = null
        }
    }

    private companion object {
        const val MAX_METADATA_BYTES = 2 * 1024 * 1024
    }
}

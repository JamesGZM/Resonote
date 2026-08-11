package com.resonote.core.network.retrofit

import com.google.common.truth.Truth.assertThat
import com.resonote.core.network.ApiHttpException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.model.NetworkMobileCodeLoginResult
import com.resonote.core.network.protocol.ApiCallExecutor
import com.resonote.core.network.protocol.ApiDeviceIdentityFactory
import com.resonote.core.network.protocol.ApiEndpointOrigins
import com.resonote.core.network.protocol.ApiOriginPolicy
import com.resonote.core.network.protocol.ApiProtocolCrypto
import com.resonote.core.network.protocol.ApiRequestSigner
import com.resonote.core.network.protocol.ProtocolRandom
import com.resonote.core.network.risk.ApiRiskChallengeDetector
import com.resonote.core.network.risk.RiskAwareApiExecutor
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
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class ApiNetworkDataSourceTest {
    private lateinit var server: MockWebServer
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val crypto = ApiProtocolCrypto(ProtocolRandom { length -> "A".repeat(length) })
    private val session = ApiSession(
        guid = "fixture-guid",
        mid = "fixture-mid",
        dev = "FIXTUREDEV",
        dfid = "fixture-dfid",
        token = "existing-token",
        userId = "99",
        cookies = mapOf("token" to "existing-token", "userid" to "99", "dfid" to "fixture-dfid"),
    )

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
    fun sendMobileCodeUsesIsolatedMidIdentityAndExactBody() = runTest {
        server.enqueue(jsonResponse("""{"status":1}"""))

        dataSource().sendMobileCode("13800000000")

        val request = server.takeRequest()
        assertThat(request.path).contains("/v7/send_mobile_code?")
        assertThat(request.requestUrl?.queryParameter("token")).isNull()
        assertThat(request.requestUrl?.queryParameter("userid")).isNull()
        assertThat(request.getHeader("Cookie")).isEqualTo("mid=fixture-mid")
        assertThat(request.body.readUtf8()).isEqualTo("""{"businessid":5,"mobile":"13800000000","plat":3}""")
    }

    @Test
    fun missingDfidRegistersAnonymousDeviceBeforeSignedSearch() = runTest {
        val registration = crypto.encryptPlaylist("""{"status":1,"data":{"dfid":"registered-dfid"}}""")
        server.enqueue(MockResponse().setResponseCode(200).setBody(Buffer().write(registration.ciphertext)))
        server.enqueue(jsonResponse("""{"status":1,"data":{"lists":[],"total":0}}"""))

        dataSource(session.copy(dfid = null, token = null, userId = null, cookies = emptyMap()))
            .searchSongs("fixture", page = 1, pageSize = 1)

        val register = server.takeRequest()
        val search = server.takeRequest()
        assertThat(register.path).contains("/risk/v2/r_register_dev?")
        assertThat(register.requestUrl?.queryParameter("part")).isEqualTo("1")
        assertThat(register.requestUrl?.queryParameter("p")).isNotEmpty()
        assertThat(register.body.readUtf8()).matches("[A-Za-z0-9+/]+={0,2}")
        assertThat(search.requestUrl?.queryParameter("dfid")).isEqualTo("registered-dfid")
        assertThat(search.requestUrl?.queryParameter("signature")).isNotEmpty()
    }

    @Test
    fun mobileLoginBuildsLiteBodyMergesCookiesAndAcceptsObjectSecret() = runTest {
        val encrypted = crypto.encryptTemporary("""{"token":"new-token","userid":"42","t1":"new-t1"}""")
        server.enqueue(
            jsonResponse("""{"status":1,"data":{"secu_params":"${encrypted.ciphertextHex}"}}""")
                .addHeader("Set-Cookie", "server_cookie=value; Path=/; HttpOnly"),
        )

        val result = dataSource().loginWithMobileCode("13800000000", "246810", "42")

        val authenticated = result as NetworkMobileCodeLoginResult.Authenticated
        assertThat(authenticated.session.token).isEqualTo("new-token")
        assertThat(authenticated.session.userId).isEqualTo("42")
        assertThat(authenticated.session.cookies).containsEntry("server_cookie", "value")
        val body = json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertThat(body["t1"]?.jsonPrimitive?.content).isNotEqualTo("0")
        assertThat(body["t2"]?.jsonPrimitive?.content).isNotEqualTo("0")
        assertThat(body).doesNotContainKey("t3")
        assertThat(body["dfid"]?.jsonPrimitive?.content).isEqualTo("fixture-dfid")
        assertThat(body["dev"]?.jsonPrimitive?.content).isEqualTo("FIXTUREDEV")
        assertThat(body["gitversion"]?.jsonPrimitive?.content).isEqualTo("5f0b7c4")
        assertThat(body["userid"]?.jsonPrimitive?.content).isEqualTo("42")
    }

    @Test
    fun mobileLoginAcceptsPlaintextTokenSecret() = runTest {
        val encrypted = crypto.encryptTemporary("plain-token")
        server.enqueue(jsonResponse("""{"status":1,"data":{"userid":42,"secu_params":"${encrypted.ciphertextHex}"}}"""))

        val result = dataSource().loginWithMobileCode("13800000000", "246810")

        assertThat((result as NetworkMobileCodeLoginResult.Authenticated).session.token).isEqualTo("plain-token")
    }

    @Test
    fun mobileLoginMapsMultipleAccountsWithoutChangingSession() = runTest {
        server.enqueue(
            jsonResponse(
                """{"status":0,"error_code":1001,"data":{"info_list":[{"userid":42,"nickname":"first","pic":"avatar","p_grade":7}]}}""",
            ),
        )

        val result = dataSource().loginWithMobileCode("13800000000", "246810")

        val accounts = (result as NetworkMobileCodeLoginResult.MultipleAccounts).accounts
        assertThat(accounts).hasSize(1)
        assertThat(accounts.single().userId).isEqualTo("42")
        assertThat(accounts.single().nickname).isEqualTo("first")
    }

    @Test
    fun httpStatusIsClassifiedBeforeNonJsonBody() {
        server.enqueue(MockResponse().setResponseCode(503).setBody("upstream unavailable"))

        val failure = assertThrows(ApiHttpException::class.java) {
            runTest { dataSource().sendMobileCode("13800000000") }
        }

        assertThat(failure.statusCode).isEqualTo(503)
    }

    @Test
    fun jsonServiceRejectionRemainsTyped() {
        server.enqueue(jsonResponse("""{"status":0,"error_code":"12345"}"""))

        val failure = assertThrows(ApiServiceException::class.java) {
            runTest { dataSource().sendMobileCode("13800000000") }
        }

        assertThat(failure.serviceCode).isEqualTo("12345")
    }

    private fun dataSource(initialSession: ApiSession = session): RealApiNetworkDataSource {
        val store = MemoryStore(initialSession)
        val sessions = ApiSessionManager(Optional.of(store), ApiDeviceIdentityFactory())
        val risk = RiskAwareApiExecutor(ApiRiskChallengeDetector(), Optional.empty())
        val signer = ApiRequestSigner()
        val executor = ApiCallExecutor(
            OkHttpClient(), json, Clock.fixed(Instant.ofEpochMilli(1_700_000_000_123), ZoneOffset.UTC), signer,
            sessions, Lazy { risk }, ApiOriginPolicy { true },
        )
        val origin = server.url("/").toString().removeSuffix("/")
        return RealApiNetworkDataSource(
            executor,
            json,
            crypto,
            signer,
            sessions,
            ApiEndpointOrigins(origin, origin, origin, origin, origin),
        )
    }

    private fun jsonResponse(body: String) =
        MockResponse().setResponseCode(200).addHeader("Content-Type", "application/json").setBody(body)

    private class MemoryStore(initial: ApiSession?) : ApiSessionStore {
        private val state = MutableStateFlow(initial)
        override val session = state
        override suspend fun read() = state.value
        override suspend fun write(session: ApiSession) { state.value = session }
        override suspend fun clearAuthentication() { state.value = null }
    }
}

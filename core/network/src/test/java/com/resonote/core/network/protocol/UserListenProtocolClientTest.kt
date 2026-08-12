package com.resonote.core.network.protocol

import com.google.common.truth.Truth.assertThat
import com.resonote.core.network.ApiAuthenticationRequiredException
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiRiskException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.risk.ApiRiskChallengeDetector
import com.resonote.core.network.session.ApiSession
import com.resonote.core.network.session.ApiSessionManager
import com.resonote.core.network.session.ApiSessionStore
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
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class UserListenProtocolClientTest {
    private lateinit var server: MockWebServer
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun startServer() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun stopServer() {
        server.shutdown()
    }

    @Test
    fun accountHistoryMatchesAndroidContractAndDecodesConsumerConfirmedFields() = runTest {
        server.enqueue(jsonResponse(fixture("user_listen_response_synthetic.json")))
        val signer = ApiRequestSigner()

        val songs = client(signer = signer).accountHistory()

        assertThat(songs).hasSize(2)
        assertThat(songs[0].title).isEqualTo("First Song")
        assertThat(songs[0].artist).isEqualTo("Artist One")
        assertThat(songs[0].coverUrl).isEqualTo("https://image.example/{size}/first.jpg")
        assertThat(songs[0].durationMillis).isEqualTo(245_000)
        assertThat(songs[1].durationMillis).isEqualTo(180_000)
        val request = server.takeRequest()
        val requestUrl = requireNotNull(request.requestUrl)
        val bodyBytes = request.body.readByteArray()
        val body = json.parseToJsonElement(bodyBytes.decodeToString()).jsonObject
        assertThat(request.method).isEqualTo("POST")
        assertThat(requestUrl.encodedPath).isEqualTo("/v2/get_list")
        assertThat(requestUrl.queryParameter("clienttime")).isEqualTo("1700000000")
        assertThat(requestUrl.queryParameter("plat")).isEqualTo("0")
        assertThat(requestUrl.queryParameter("token")).isEqualTo("fixture-token")
        assertThat(requestUrl.queryParameter("userid")).isEqualTo("99")
        assertThat(body["t_userid"]?.jsonPrimitive?.content).isEqualTo("99")
        assertThat(body["userid"]?.jsonPrimitive?.content).isEqualTo("99")
        assertThat(body["list_type"]?.jsonPrimitive?.content).isEqualTo("1")
        assertThat(body["area_code"]?.jsonPrimitive?.content).isEqualTo("1")
        assertThat(body["cover"]?.jsonPrimitive?.content).isEqualTo("2")
        assertThat(body["p"]?.jsonPrimitive?.content).matches("[0-9A-F]{256}")
        assertThat(request.getHeader("Cookie")).contains("token=fixture-token")
        val signedParameters =
            requestUrl.queryParameterNames
                .filterNot { it == "signature" }
                .associateWith { requestUrl.queryParameter(it).orEmpty() }
        assertThat(requestUrl.queryParameter("signature")).isEqualTo(signer.sign(signedParameters, bodyBytes))
    }

    @Test
    fun partialUnknownItemsAreSkippedButValidOrderIsPreserved() = runTest {
        server.enqueue(
            jsonResponse(
                """{"status":1,"data":{"lists":[{"hash":"","name":"Bad"},{"hash":"GOOD","name":"Artist - Track","duration":"12"}]}}""",
            ),
        )

        val songs = client().accountHistory()

        assertThat(songs.map { it.hash }).containsExactly("GOOD").inOrder()
        assertThat(songs.single().artist).isEqualTo("Artist")
        assertThat(songs.single().durationMillis).isEqualTo(12_000)
    }

    @Test
    fun nonEmptyHistoryWithoutConsumableSongsIsProtocolFailure() {
        server.enqueue(jsonResponse("""{"status":1,"data":{"lists":[{"hash":123,"name":"Bad"}]}}"""))

        val failure = assertThrows(ApiProtocolException::class.java) {
            runTest { client().accountHistory() }
        }

        assertThat(failure.reason).isEqualTo(ApiProtocolException.Reason.MissingRequiredField)
    }

    @Test
    fun missingListsIsProtocolFailure() {
        server.enqueue(jsonResponse("""{"status":1,"data":{}}"""))

        val failure = assertThrows(ApiProtocolException::class.java) {
            runTest { client().accountHistory() }
        }

        assertThat(failure.reason).isEqualTo(ApiProtocolException.Reason.MissingRequiredField)
    }

    @Test
    fun anonymousSessionIsRejectedBeforeHistoryRequest() {
        val anonymous = authenticatedSession.copy(token = null, userId = null, cookies = emptyMap())

        assertThrows(ApiAuthenticationRequiredException::class.java) {
            runTest { client(initialSession = anonymous).accountHistory() }
        }

        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun riskAndServiceFailuresRemainTyped() {
        server.enqueue(jsonResponse("""{"status":0,"error_code":20028,"ssaCode":"risk-event"}"""))
        server.enqueue(jsonResponse("""{"status":0,"error_code":"E_UPSTREAM"}"""))

        val risk = assertThrows(ApiRiskException::class.java) {
            runTest { client().accountHistory() }
        }
        val service = assertThrows(ApiServiceException::class.java) {
            runTest { client().accountHistory() }
        }

        assertThat(risk.challenge.eventId).isEqualTo("risk-event")
        assertThat(service.serviceCode).isEqualTo("E_UPSTREAM")
    }

    private fun client(
        initialSession: ApiSession = authenticatedSession,
        signer: ApiRequestSigner = ApiRequestSigner(),
    ): UserListenProtocolClient {
        val sessions = ApiSessionManager(Optional.of(MemoryStore(initialSession)), ApiDeviceIdentityFactory())
        val crypto = ApiProtocolCrypto(ProtocolRandom { length -> "A".repeat(length) })
        val origins = ApiEndpointOrigins(listen = server.origin())
        val transport =
            ProtocolTransport(
                { OkHttpClient() },
                json,
                Clock.fixed(Instant.ofEpochMilli(1_700_000_000_123), ZoneOffset.UTC),
                signer,
                sessions,
                ApiRiskChallengeDetector(),
                ApiOriginPolicy { true },
            )
        val registration =
            DeviceRegistrationCoordinator(
                transport,
                json,
                crypto,
                sessions,
                origins,
                fixtureDeviceProfileProvider(),
            )
        return UserListenProtocolClient(transport, registration, crypto, origins)
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "Missing fixture $name" }
            .bufferedReader()
            .use { it.readText() }

    private fun fixtureDeviceProfileProvider() = DeviceRegistrationProfileProvider {
        DeviceRegistrationProfile(
            totalMemoryBytes = 8_000_000_000,
            availableInternalStorageBytes = 64_000_000_000,
            availableExternalStorageBytes = 32_000_000_000,
            brand = "FixtureBrand",
            buildId = "FixtureBuild",
            device = "fixture-device",
            manufacturer = "FixtureManufacturer",
        )
    }

    private fun MockWebServer.origin(): String = url("/").toString().removeSuffix("/")

    private fun jsonResponse(body: String) =
        MockResponse().setResponseCode(200).addHeader("Content-Type", "application/json").setBody(body)

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
        val authenticatedSession =
            ApiSession(
                guid = "fixture-guid",
                mid = "fixture-mid",
                dev = "FIXTUREDEV",
                dfid = "fixture-dfid",
                token = "fixture-token",
                userId = "99",
                cookies = mapOf("token" to "fixture-token", "userid" to "99", "dfid" to "fixture-dfid"),
            )
    }
}

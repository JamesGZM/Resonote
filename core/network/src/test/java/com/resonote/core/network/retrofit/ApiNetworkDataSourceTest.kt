package com.resonote.core.network.retrofit

import com.google.common.truth.Truth.assertThat
import com.resonote.core.network.ApiHttpException
import com.resonote.core.network.ApiPlaybackUnavailableException
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.model.NetworkMobileCodeLoginResult
import com.resonote.core.network.model.NetworkRecommendationMode
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
    private lateinit var gatewayServer: MockWebServer
    private lateinit var mobileCodeServer: MockWebServer
    private lateinit var mobileLoginServer: MockWebServer
    private lateinit var deviceRegistrationServer: MockWebServer
    private lateinit var riskVerificationServer: MockWebServer
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
    fun dailyRecommendationsUsesMobileContractAndDecodesRequiredFields() = runTest {
        gatewayServer.enqueue(
            jsonResponse(
                """{"status":1,"data":{"song_list":[{"hash":"ABC","ori_audio_name":"Song","author_name":"Artist","sizable_cover":"https://img/{size}.jpg","time_length":245,"hash_320":"HQ","hash_flac":"SQ","privilege":10}]}}""",
            ),
        )

        val songs = dataSource().dailyRecommendations()

        assertThat(songs).hasSize(1)
        assertThat(songs.single().durationMillis).isEqualTo(245_000)
        assertThat(songs.single().losslessHash).isEqualTo("SQ")
        val request = gatewayServer.takeRequest()
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.requestUrl?.host).isEqualTo(gatewayServer.hostName)
        assertThat(request.requestUrl?.encodedPath).isEqualTo("/everyday_song_recommend")
        assertThat(request.requestUrl?.queryParameter("platform")).isEqualTo("ios")
        assertThat(request.getHeader("x-router")).isEqualTo("everydayrec.service.kugou.com")
        assertThat(request.requestUrl?.queryParameter("signature")).isNotEmpty()
    }

    @Test
    fun nonEmptySongListWithNoConsumableItemsIsProtocolFailure() {
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"song_list":[{"hash":"","songname":""}]}}"""))

        assertThrows(ApiProtocolException::class.java) {
            runTest { dataSource().dailyRecommendations() }
        }
    }

    @Test
    fun recommendedPlaylistsUsesNestedMobileBody() = runTest {
        gatewayServer.enqueue(
            jsonResponse(
                """{"status":1,"data":{"special_list":[{"global_collection_id":"gid","specialname":"Playlist","flexible_cover":"https://img/{size}.jpg","play_count":1234}]}}""",
            ),
        )

        val playlists = dataSource().recommendedPlaylists(page = 1, pageSize = 6)

        assertThat(playlists.single().id).isEqualTo("gid")
        val request = gatewayServer.takeRequest()
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.requestUrl?.host).isEqualTo(gatewayServer.hostName)
        assertThat(request.requestUrl?.encodedPath).isEqualTo("/v2/special_recommend")
        assertThat(request.getHeader("x-router")).isEqualTo("specialrec.service.kugou.com")
        val body = json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertThat(body["page"]?.jsonPrimitive?.content).isEqualTo("1")
        assertThat(body["pagesize"]?.jsonPrimitive?.content).isEqualTo("6")
        assertThat(body["key"]?.jsonPrimitive?.content).isNotEmpty()
        val nested = body["special_recommend"]?.jsonObject
        assertThat(nested?.get("categoryid")?.jsonPrimitive?.content).isEqualTo("0")
        assertThat(nested?.get("withsong")?.jsonPrimitive?.content).isEqualTo("0")
    }

    @Test
    fun newSongsUsesFixedUnclassifiedRankAndDecodesDeprecatedFallback() = runTest {
        gatewayServer.enqueue(
            jsonResponse(
                """{"status":1,"data":[{"filename":"Artist - New Song","deprecated":{"hash":"DEF","duration":210},"trans_param":{"union_cover":"https://img/{size}.jpg"}}],"total":1}""",
            ),
        )

        val songs = dataSource().newSongs(page = 1, pageSize = 6)

        assertThat(songs.single().hash).isEqualTo("DEF")
        assertThat(songs.single().title).isEqualTo("New Song")
        val request = gatewayServer.takeRequest()
        val body = json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.requestUrl?.host).isEqualTo(gatewayServer.hostName)
        assertThat(request.requestUrl?.encodedPath).isEqualTo("/musicadservice/container/v1/newsong_publish")
        assertThat(request.requestUrl?.queryParameter("signature")).isNotEmpty()
        assertThat(body["rank_id"]?.jsonPrimitive?.content).isEqualTo("21608")
        assertThat(body["tags"]?.toString()).isEqualTo("[]")
    }

    @Test
    fun radioRecommendationsMapsAllSupportedCardIds() = runTest {
        NetworkRecommendationMode.entries.forEach {
            gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"song_list":[]}}"""))
        }

        NetworkRecommendationMode.entries.forEach { mode ->
            dataSource().radioRecommendations(mode)
            val request = gatewayServer.takeRequest()
            assertThat(request.method).isEqualTo("POST")
            assertThat(request.requestUrl?.host).isEqualTo(gatewayServer.hostName)
            assertThat(request.requestUrl?.encodedPath).isEqualTo("/singlecardrec.service/v1/single_card_recommend")
            assertThat(request.requestUrl?.queryParameter("card_id")).isEqualTo(mode.cardId.toString())
            assertThat(request.requestUrl?.queryParameter("platform")).isEqualTo("ios")
            assertThat(request.requestUrl?.queryParameter("signature")).isNotEmpty()
            val body = json.parseToJsonElement(request.body.readUtf8()).jsonObject
            assertThat(body["platform"]?.jsonPrimitive?.content).isEqualTo("android")
            assertThat(body["client_playlist"]?.toString()).isEqualTo("[]")
        }
    }

    @Test
    fun songSourceUsesRegisteredIdentitySongKeyAndHttpsBackup() = runTest {
        gatewayServer.enqueue(
            jsonResponse(
                """{"status":1,"url":["http://unsafe.example/song.mp3"],"backupUrl":["https://cdn.example/song.mp3"],"timeLength":321000,"extName":"mp3"}""",
            ),
        )

        val source = dataSource().resolveSongSource("ABCDEF", "12", "34")

        assertThat(source.uri).isEqualTo("https://cdn.example/song.mp3")
        assertThat(source.durationMillis).isEqualTo(321_000)
        val request = gatewayServer.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.requestUrl?.host).isEqualTo(gatewayServer.hostName)
        assertThat(request.requestUrl?.encodedPath).isEqualTo("/v5/url")
        assertThat(request.getHeader("x-router")).isEqualTo("trackercdn.kugou.com")
        assertThat(request.requestUrl?.queryParameter("hash")).isEqualTo("abcdef")
        assertThat(request.requestUrl?.queryParameter("album_id")).isEqualTo("12")
        assertThat(request.requestUrl?.queryParameter("album_audio_id")).isEqualTo("34")
        assertThat(request.requestUrl?.queryParameter("dfid")).isEqualTo("fixture-dfid")
        assertThat(request.requestUrl?.queryParameter("key")).isNotEmpty()
        assertThat(request.requestUrl?.queryParameter("signature")).isNotEmpty()
    }

    @Test
    fun songSourceClassifiesCopyrightWithoutAcceptingCleartextUrl() {
        gatewayServer.enqueue(jsonResponse("""{"status":3,"url":[]}"""))

        val failure = assertThrows(ApiPlaybackUnavailableException::class.java) {
            runTest { dataSource().resolveSongSource("ABCDEF") }
        }

        assertThat(failure.reason).isEqualTo(ApiPlaybackUnavailableException.Reason.Copyright)
    }

    @Test
    fun songSourceClassifiesVipAndMalformedResponseSeparately() {
        gatewayServer.enqueue(jsonResponse("""{"status":1,"url":[],"backupUrl":[]}"""))
        gatewayServer.enqueue(jsonResponse("""{"url":[]}"""))

        val vip = assertThrows(ApiPlaybackUnavailableException::class.java) {
            runTest { dataSource().resolveSongSource("ABCDEF") }
        }
        assertThat(vip.reason).isEqualTo(ApiPlaybackUnavailableException.Reason.Vip)
        assertThrows(ApiProtocolException::class.java) {
            runTest { dataSource().resolveSongSource("ABCDEF") }
        }
    }

    @Test
    fun sendMobileCodeUsesIsolatedMidIdentityAndExactBody() = runTest {
        mobileCodeServer.enqueue(jsonResponse("""{"status":1}"""))

        dataSource().sendMobileCode("13800000000")

        val request = mobileCodeServer.takeRequest()
        assertThat(request.path).contains("/v7/send_mobile_code?")
        assertThat(request.requestUrl?.queryParameter("token")).isNull()
        assertThat(request.requestUrl?.queryParameter("userid")).isNull()
        assertThat(request.getHeader("Cookie")).isEqualTo("mid=fixture-mid")
        assertThat(request.body.readUtf8()).isEqualTo("""{"businessid":5,"mobile":"13800000000","plat":3}""")
        assertThat(gatewayServer.requestCount).isEqualTo(0)
        assertThat(mobileLoginServer.requestCount).isEqualTo(0)
    }

    @Test
    fun missingDfidRegistersAnonymousDeviceBeforeSignedSearch() = runTest {
        val registration = crypto.encryptPlaylist("""{"status":1,"data":{"dfid":"registered-dfid"}}""")
        deviceRegistrationServer.enqueue(MockResponse().setResponseCode(200).setBody(Buffer().write(registration.ciphertext)))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"lists":[],"total":0}}"""))

        dataSource(session.copy(dfid = null, token = null, userId = null, cookies = emptyMap()))
            .searchSongs("fixture", page = 1, pageSize = 1)

        val register = deviceRegistrationServer.takeRequest()
        val search = gatewayServer.takeRequest()
        assertThat(register.path).contains("/risk/v2/r_register_dev?")
        assertThat(register.requestUrl?.queryParameter("part")).isEqualTo("1")
        assertThat(register.requestUrl?.queryParameter("p")).isNotEmpty()
        assertThat(register.body.readUtf8()).matches("[A-Za-z0-9+/]+={0,2}")
        assertThat(search.requestUrl?.queryParameter("dfid")).isEqualTo("registered-dfid")
        assertThat(search.requestUrl?.queryParameter("signature")).isNotEmpty()
    }

    @Test
    fun homeRequestsReuseSingleDeviceRegistration() = runTest {
        val registration = crypto.encryptPlaylist("""{"status":1,"data":{"dfid":"registered-dfid"}}""")
        deviceRegistrationServer.enqueue(MockResponse().setResponseCode(200).setBody(Buffer().write(registration.ciphertext)))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"song_list":[]}}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"special_list":[]}}"""))
        val dataSource = dataSource(session.copy(dfid = null, token = null, userId = null, cookies = emptyMap()))

        dataSource.dailyRecommendations()
        dataSource.recommendedPlaylists(page = 1, pageSize = 6)

        assertThat(deviceRegistrationServer.requestCount).isEqualTo(1)
        assertThat(gatewayServer.takeRequest().requestUrl?.queryParameter("dfid")).isEqualTo("registered-dfid")
        assertThat(gatewayServer.takeRequest().requestUrl?.queryParameter("dfid")).isEqualTo("registered-dfid")
    }

    @Test
    fun mobileLoginBuildsLiteBodyMergesCookiesAndAcceptsObjectSecret() = runTest {
        val encrypted = crypto.encryptTemporary("""{"token":"new-token","userid":"42","t1":"new-t1"}""")
        mobileLoginServer.enqueue(
            jsonResponse("""{"status":1,"data":{"secu_params":"${encrypted.ciphertextHex}"}}""")
                .addHeader("Set-Cookie", "server_cookie=value; Path=/; HttpOnly"),
        )

        val result = dataSource().loginWithMobileCode("13800000000", "246810", "42")

        val authenticated = result as NetworkMobileCodeLoginResult.Authenticated
        assertThat(authenticated.session.token).isEqualTo("new-token")
        assertThat(authenticated.session.userId).isEqualTo("42")
        assertThat(authenticated.session.cookies).containsEntry("server_cookie", "value")
        val request = mobileLoginServer.takeRequest()
        val body = json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertThat(request.getHeader("Cookie")).contains("token=existing-token")
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
        mobileLoginServer.enqueue(jsonResponse("""{"status":1,"data":{"userid":42,"secu_params":"${encrypted.ciphertextHex}"}}"""))

        val result = dataSource().loginWithMobileCode("13800000000", "246810")

        assertThat((result as NetworkMobileCodeLoginResult.Authenticated).session.token).isEqualTo("plain-token")
    }

    @Test
    fun mobileLoginMapsMultipleAccountsWithoutChangingSession() = runTest {
        mobileLoginServer.enqueue(
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
        mobileCodeServer.enqueue(MockResponse().setResponseCode(503).setBody("upstream unavailable"))

        val failure = assertThrows(ApiHttpException::class.java) {
            runTest { dataSource().sendMobileCode("13800000000") }
        }

        assertThat(failure.statusCode).isEqualTo(503)
    }

    @Test
    fun jsonServiceRejectionRemainsTyped() {
        mobileCodeServer.enqueue(jsonResponse("""{"status":0,"error_code":"12345"}"""))

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
        return RealApiNetworkDataSource(
            executor,
            json,
            crypto,
            signer,
            sessions,
            ApiEndpointOrigins(
                gateway = gatewayServer.origin(),
                mobileCode = mobileCodeServer.origin(),
                mobileLogin = mobileLoginServer.origin(),
                deviceRegistration = deviceRegistrationServer.origin(),
                riskVerification = riskVerificationServer.origin(),
            ),
        )
    }

    private fun MockWebServer.origin(): String = url("/").toString().removeSuffix("/")

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

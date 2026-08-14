package com.resonote.core.network.retrofit

import com.google.common.truth.Truth.assertThat
import com.resonote.core.network.ApiAuthenticationRequiredException
import com.resonote.core.network.ApiHttpException
import com.resonote.core.network.ApiNetworkException
import com.resonote.core.network.ApiPlaybackUnavailableException
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiRiskException
import com.resonote.core.network.ApiRiskBlockedException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.AuthNetworkDataSource
import com.resonote.core.network.CatalogNetworkDataSource
import com.resonote.core.network.CloudNetworkDataSource
import com.resonote.core.network.HomeNetworkDataSource
import com.resonote.core.network.LyricsNetworkDataSource
import com.resonote.core.network.VideoNetworkDataSource
import com.resonote.core.network.RecognitionNetworkDataSource
import com.resonote.core.network.LibraryNetworkDataSource
import com.resonote.core.network.PlaybackNetworkDataSource
import com.resonote.core.network.PlaylistNetworkDataSource
import com.resonote.core.network.RankingNetworkDataSource
import com.resonote.core.network.SearchNetworkDataSource
import com.resonote.core.network.UserProfileNetworkDataSource
import com.resonote.core.network.VipNetworkDataSource
import com.resonote.core.network.api.MusicApi
import com.resonote.core.network.model.NetworkMobileCodeLoginResult
import com.resonote.core.network.model.NetworkPasswordLoginResult
import com.resonote.core.network.model.NetworkPlaylistTrackInput
import com.resonote.core.network.model.NetworkRecommendationMode
import com.resonote.core.network.protocol.ProtocolTransport
import com.resonote.core.network.protocol.ApiDeviceIdentityFactory
import com.resonote.core.network.protocol.DeviceRegistrationCoordinator
import com.resonote.core.network.protocol.DeviceRegistrationProfile
import com.resonote.core.network.protocol.DeviceRegistrationProfileProvider
import com.resonote.core.network.protocol.ApiEndpointOrigins
import com.resonote.core.network.protocol.ApiOriginPolicy
import com.resonote.core.network.protocol.ApiProtocolCrypto
import com.resonote.core.network.protocol.ApiDefaultsInterceptor
import com.resonote.core.network.protocol.ApiResponseMetadataInterceptor
import com.resonote.core.network.protocol.ApiSigningInterceptor
import com.resonote.core.network.protocol.ApiRequestSigner
import com.resonote.core.network.protocol.ProtocolRandom
import com.resonote.core.network.protocol.MobileAuthProtocolClient
import com.resonote.core.network.protocol.CloudProtocolClient
import com.resonote.core.network.risk.ApiRiskChallengeDetector
import com.resonote.core.network.session.ApiSession
import com.resonote.core.network.session.ApiSessionManager
import com.resonote.core.network.session.ApiSessionStore
import com.resonote.core.network.session.ApiAuthenticationGateReason
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64
import java.util.Optional
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ApiNetworkDataSourceTest {
    private lateinit var gatewayServer: MockWebServer
    private lateinit var mobileCodeServer: MockWebServer
    private lateinit var mobileLoginServer: MockWebServer
    private lateinit var deviceRegistrationServer: MockWebServer
    private lateinit var riskVerificationServer: MockWebServer
    private lateinit var vipServer: MockWebServer
    private lateinit var cloudServer: MockWebServer
    private lateinit var openApiServer: MockWebServer
    private lateinit var complexSearchServer: MockWebServer
    private lateinit var lyricsServer: MockWebServer
    private lateinit var qrLoginServer: MockWebServer
    private val json = Json { ignoreUnknownKeys = true }
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
    private val anonymousSession: ApiSession
        get() = session.copy(token = null, userId = "0", cookies = mapOf("dfid" to "fixture-dfid"))

    @Before
    fun startServers() {
        gatewayServer = MockWebServer().apply { start() }
        mobileCodeServer = MockWebServer().apply { start() }
        mobileLoginServer = MockWebServer().apply { start() }
        deviceRegistrationServer = MockWebServer().apply { start() }
        riskVerificationServer = MockWebServer().apply { start() }
        vipServer = MockWebServer().apply { start() }
        cloudServer = MockWebServer().apply { start() }
        openApiServer = MockWebServer().apply { start() }
        complexSearchServer = MockWebServer().apply { start() }
        lyricsServer = MockWebServer().apply { start() }
        qrLoginServer = MockWebServer().apply { start() }
    }

    @After
    fun stopServers() {
        gatewayServer.shutdown()
        mobileCodeServer.shutdown()
        mobileLoginServer.shutdown()
        deviceRegistrationServer.shutdown()
        riskVerificationServer.shutdown()
        vipServer.shutdown()
        cloudServer.shutdown()
        openApiServer.shutdown()
        complexSearchServer.shutdown()
        lyricsServer.shutdown()
        qrLoginServer.shutdown()
    }

    @Test
    fun dailyRecommendationsUsesMobileContractAndDecodesRequiredFields() = runTest {
        gatewayServer.enqueue(
            jsonResponse(
                """{"status":1,"server_added_field":{"future":true},"data":{"song_list":[{"hash":"ABC","ori_audio_name":"Song","author_name":"Artist","sizable_cover":"https://img/{size}.jpg","time_length":"245","hash_320":"HQ","hash_flac":"SQ","privilege":10,"future_song_field":"ignored"}]}}""",
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
    fun knownTextFieldsRejectWrongJsonScalarTypes() {
        gatewayServer.enqueue(
            jsonResponse("""{"status":1,"data":{"song_list":[{"hash":123,"songname":"Song"}]}}"""),
        )

        val failure = assertThrows(ApiProtocolException::class.java) {
            runTest { dataSource().dailyRecommendations() }
        }

        assertThat(failure.reason).isEqualTo(ApiProtocolException.Reason.MalformedResponse)
    }

    @Test
    fun typedRetrofitResponseStillFeedsRiskCoordinator() {
        gatewayServer.enqueue(
            jsonResponse("""{"status":0,"error_code":20028,"ssaCode":"event-id","data":{"song_list":[]}}"""),
        )

        val failure = assertThrows(ApiRiskException::class.java) {
            runTest { dataSource().dailyRecommendations() }
        }

        assertThat(failure.reason).isEqualTo(ApiRiskException.Reason.VerificationUnavailable)
        assertThat(failure.challenge.eventId).isEqualTo("event-id")
    }

    @Test
    fun standardEndpointDoesNotRetryHttpFailure() {
        gatewayServer.enqueue(MockResponse().setResponseCode(503).setBody("upstream unavailable"))

        val failure = assertThrows(ApiHttpException::class.java) {
            runTest { dataSource().dailyRecommendations() }
        }

        assertThat(failure.statusCode).isEqualTo(503)
        assertThat(gatewayServer.requestCount).isEqualTo(1)
    }

    @Test
    fun standardEndpointDoesNotRetryConnectionFailure() {
        gatewayServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        assertThrows(ApiNetworkException::class.java) {
            runTest { dataSource().dailyRecommendations() }
        }

        assertThat(gatewayServer.requestCount).isEqualTo(1)
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
        assertThat(nested?.get("withsong")?.jsonPrimitive?.content).isEqualTo("1")
    }

    @Test
    fun categoryPlaylistsPassesMobileCategoryAndPaging() = runTest {
        gatewayServer.enqueue(
            jsonResponse(
                """{"status":1,"data":{"special_list":[{"global_collection_id":"category-gid","specialname":"分类歌单","play_count":88}]}}""",
            ),
        )

        val playlists = dataSource().categoryPlaylists(categoryId = 42, page = 2, pageSize = 30)

        assertThat(playlists.single().id).isEqualTo("category-gid")
        val request = gatewayServer.takeRequest()
        val body = json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertThat(body["page"]?.jsonPrimitive?.content).isEqualTo("2")
        assertThat(body["pagesize"]?.jsonPrimitive?.content).isEqualTo("30")
        assertThat(body["special_recommend"]?.jsonObject?.get("categoryid")?.jsonPrimitive?.content).isEqualTo("42")
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
    fun authenticatedSongSourceUsesPrivilegeHashFullPlaybackAndHttpsBackup() = runTest {
        gatewayServer.enqueue(
            jsonResponse(
                """{"status":1,"data":[{"hash":"abcdef","quality":"flac","level":1,"relate_goods":[{"hash":"STANDARD_HASH","quality":"128","level":1}]}]}""",
            ),
        )
        gatewayServer.enqueue(
            jsonResponse(
                """{"status":1,"url":["http://unsafe.example/song.mp3"],"backupUrl":["https://cdn.example/song.mp3"],"timeLength":321000,"extName":"mp3"}""",
            ),
        )

        val source = dataSource().resolveSongSource("ABCDEF", "12", "34")

        assertThat(source.uri).isEqualTo("https://cdn.example/song.mp3")
        assertThat(source.durationMillis).isEqualTo(321_000)
        val privilegeRequest = gatewayServer.takeRequest()
        assertThat(privilegeRequest.method).isEqualTo("POST")
        assertThat(privilegeRequest.requestUrl?.encodedPath).isEqualTo("/v2/get_res_privilege/lite")
        assertThat(privilegeRequest.getHeader("x-router")).isEqualTo("media.store.kugou.com")
        val privilegeBody = json.parseToJsonElement(privilegeRequest.body.readUtf8()).jsonObject
        assertThat(privilegeBody["resource"]?.jsonArray?.single()?.jsonObject?.get("hash")?.jsonPrimitive?.content)
            .isEqualTo("abcdef")

        val sourceRequest = gatewayServer.takeRequest()
        assertThat(sourceRequest.method).isEqualTo("GET")
        assertThat(sourceRequest.requestUrl?.host).isEqualTo(gatewayServer.hostName)
        assertThat(sourceRequest.requestUrl?.encodedPath).isEqualTo("/v5/url")
        assertThat(sourceRequest.getHeader("x-router")).isEqualTo("trackercdn.kugou.com")
        assertThat(sourceRequest.requestUrl?.queryParameter("hash")).isEqualTo("standard_hash")
        assertThat(sourceRequest.requestUrl?.queryParameter("quality")).isEqualTo("128")
        assertThat(sourceRequest.requestUrl?.queryParameter("IsFreePart")).isEqualTo("0")
        assertThat(sourceRequest.requestUrl?.queryParameter("ppage_id")).isEqualTo("356753938")
        assertThat(sourceRequest.requestUrl?.queryParameter("album_id")).isEqualTo("12")
        assertThat(sourceRequest.requestUrl?.queryParameter("album_audio_id")).isEqualTo("34")
        assertThat(sourceRequest.requestUrl?.queryParameter("dfid")).isEqualTo("fixture-dfid")
        assertThat(sourceRequest.requestUrl?.queryParameter("userid")).isEqualTo("99")
        assertThat(sourceRequest.requestUrl?.queryParameter("token")).isEqualTo("existing-token")
        assertThat(sourceRequest.getHeader("Cookie")).contains("token=existing-token")
        assertThat(sourceRequest.requestUrl?.queryParameter("key")).isNotEmpty()
        assertThat(sourceRequest.requestUrl?.queryParameter("signature")).isNotEmpty()
    }

    @Test
    fun privilegeBusinessFailureFallsBackWithoutExpiringSession() = runTest {
        gatewayServer.enqueue(jsonResponse("""{"status":0,"error_code":20010}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"url":["https://cdn.example/song.mp3"],"extName":"mp3"}"""))
        val source = dataSource()

        val song = source.resolveSongSource("ABCDEF")

        assertThat(song.uri).isEqualTo("https://cdn.example/song.mp3")
        assertThat(source.authenticationClearCount).isEqualTo(0)
        assertThat(gatewayServer.requestCount).isEqualTo(2)
    }

    @Test
    fun privilegeHttpUnauthorizedFallsBackWithoutExpiringSession() = runTest {
        gatewayServer.enqueue(MockResponse().setResponseCode(401))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"url":["https://cdn.example/song.mp3"],"extName":"mp3"}"""))
        val source = dataSource()

        val song = source.resolveSongSource("ABCDEF")

        assertThat(song.uri).isEqualTo("https://cdn.example/song.mp3")
        assertThat(source.authenticationClearCount).isEqualTo(0)
        assertThat(gatewayServer.requestCount).isEqualTo(2)
    }

    @Test
    fun privilegeStatusTwoExpiresAuthenticatedSessionWithoutCodeMapping() = runTest {
        gatewayServer.enqueue(jsonResponse("""{"status":2,"error_code":20018}"""))
        val source = dataSource()

        val failure = runCatching { source.resolveSongSource("ABCDEF") }.exceptionOrNull()

        assertThat(failure).isInstanceOf(ApiAuthenticationRequiredException::class.java)
        failure as ApiAuthenticationRequiredException
        assertThat(failure.reason).isEqualTo(ApiAuthenticationGateReason.SessionExpired)
        assertThat(failure.serviceCode).isEqualTo("20018")
        assertThat(source.authenticationClearCount).isEqualTo(1)
        assertThat(gatewayServer.requestCount).isEqualTo(1)
    }

    @Test
    fun songUrlInvalidTokenExpiresAuthenticatedSession() = runTest {
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":[]}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":2,"error_code":20017}"""))
        val source = dataSource()

        val failure = runCatching { source.resolveSongSource("ABCDEF") }.exceptionOrNull()

        assertThat(failure).isInstanceOf(ApiAuthenticationRequiredException::class.java)
        failure as ApiAuthenticationRequiredException
        assertThat(failure.reason).isEqualTo(ApiAuthenticationGateReason.SessionExpired)
        assertThat(failure.serviceCode).isEqualTo("20017")
        assertThat(source.authenticationClearCount).isEqualTo(1)
        assertThat(gatewayServer.requestCount).isEqualTo(2)
    }

    @Test
    fun privilegeFailureFallsBackToOriginalHash() = runTest {
        gatewayServer.enqueue(MockResponse().setResponseCode(503).setBody("upstream unavailable"))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"url":["https://cdn.example/song.mp3"],"extName":"mp3"}"""))

        val source = dataSource().resolveSongSource("ORIGINAL", requestedQuality = "320")

        assertThat(source.uri).isEqualTo("https://cdn.example/song.mp3")
        gatewayServer.takeRequest()
        val fallback = gatewayServer.takeRequest()
        assertThat(fallback.requestUrl?.queryParameter("hash")).isEqualTo("original")
        assertThat(fallback.requestUrl?.queryParameter("quality")).isEqualTo("320")
    }

    @Test
    fun mp4CandidateFallsBackToLowerQuality() = runTest {
        gatewayServer.enqueue(
            jsonResponse(
                """{"status":1,"data":[{"hash":"FLAC_HASH","quality":"flac","level":1,"relate_goods":[{"hash":"HQ_HASH","quality":"320","level":1},{"hash":"STD_HASH","quality":"128","level":1}]}]}""",
            ),
        )
        gatewayServer.enqueue(jsonResponse("""{"status":1,"url":["https://cdn.example/video.mp4"],"extName":"MP4"}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"url":["https://cdn.example/song.mp3"],"extName":"mp3"}"""))

        val source = dataSource().resolveSongSource("ORIGINAL", requestedQuality = "flac")

        assertThat(source.uri).isEqualTo("https://cdn.example/song.mp3")
        gatewayServer.takeRequest()
        val flac = gatewayServer.takeRequest()
        val high = gatewayServer.takeRequest()
        assertThat(flac.requestUrl?.queryParameter("hash")).isEqualTo("flac_hash")
        assertThat(flac.requestUrl?.queryParameter("quality")).isEqualTo("flac")
        assertThat(high.requestUrl?.queryParameter("hash")).isEqualTo("hq_hash")
        assertThat(high.requestUrl?.queryParameter("quality")).isEqualTo("320")
    }

    @Test
    fun successfulCandidateWithoutUrlFallsBackToLowerQuality() = runTest {
        gatewayServer.enqueue(
            jsonResponse(
                """{"status":1,"data":[{"hash":"HQ_HASH","quality":"320","level":1,"relate_goods":[{"hash":"STD_HASH","quality":"128","level":1}]}]}""",
            ),
        )
        gatewayServer.enqueue(jsonResponse("""{"status":1,"url":[],"extName":"mp3"}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"url":["https://cdn.example/song.mp3"],"extName":"mp3"}"""))

        val source = dataSource().resolveSongSource("ORIGINAL", requestedQuality = "320")

        assertThat(source.uri).isEqualTo("https://cdn.example/song.mp3")
        gatewayServer.takeRequest()
        assertThat(gatewayServer.takeRequest().requestUrl?.queryParameter("quality")).isEqualTo("320")
        assertThat(gatewayServer.takeRequest().requestUrl?.queryParameter("quality")).isEqualTo("128")
    }

    @Test
    fun unavailablePrivilegeVariantsUseRequestedQualityFallbackChain() = runTest {
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":[]}"""))
        repeat(3) { gatewayServer.enqueue(jsonResponse("""{"status":0,"error_code":31863}""")) }
        gatewayServer.enqueue(jsonResponse("""{"status":1,"url":["https://cdn.example/song.mp3"],"extName":"mp3"}"""))

        val source = dataSource().resolveSongSource("ORIGINAL", requestedQuality = "high")

        assertThat(source.uri).isEqualTo("https://cdn.example/song.mp3")
        gatewayServer.takeRequest()
        val requests = List(4) { gatewayServer.takeRequest() }
        assertThat(requests.map { it.requestUrl?.queryParameter("quality") })
            .containsExactly("high", "flac", "320", "128").inOrder()
        assertThat(requests.map { it.requestUrl?.queryParameter("hash") }.distinct())
            .containsExactly("original")
        assertThat(requests.map { it.requestUrl?.queryParameter("ppage_id") }.distinct())
            .containsExactly("356753938")
    }

    @Test
    fun songSourceAcceptsCleartextKugouCdnUrl() = runTest {
        gatewayServer.enqueue(
            jsonResponse(
                """{"status":1,"url":["http://fs.youthandroid2.kugou.com/song.mp3?token=value"],"timeLength":321000,"extName":"mp3"}""",
            ),
        )

        val source = dataSource(anonymousSession).resolveSongSource("ABCDEF", "12", "34")

        assertThat(source.uri).isEqualTo("http://fs.youthandroid2.kugou.com/song.mp3?token=value")
        val request = gatewayServer.takeRequest()
        assertThat(request.requestUrl?.queryParameter("IsFreePart")).isEqualTo("1")
        assertThat(request.requestUrl?.queryParameter("ppage_id")).isEqualTo("356753938,823673182,967485191")
    }

    @Test
    fun authenticatedSongSourceUsesFirstPrivilegeHashForEachQuality() = runTest {
        gatewayServer.enqueue(
            jsonResponse(
                """{"status":1,"data":[{"hash":"FIRST","quality":"128","level":1,"relate_goods":[{"hash":"SECOND","quality":"128","level":1}]}]}""",
            ),
        )
        gatewayServer.enqueue(jsonResponse("""{"status":1,"url":["https://cdn.example/song.mp3"],"extName":"mp3"}"""))

        val source = dataSource().resolveSongSource("ORIGINAL")

        assertThat(source.uri).isEqualTo("https://cdn.example/song.mp3")
        gatewayServer.takeRequest()
        assertThat(gatewayServer.takeRequest().requestUrl?.queryParameter("hash")).isEqualTo("first")
        assertThat(gatewayServer.requestCount).isEqualTo(2)
    }

    @Test
    fun songSourceRejectsCleartextUrlOutsideKugouDomain() {
        gatewayServer.enqueue(
            jsonResponse(
                """{"status":1,"url":["http://cdn.example/song.mp3"],"timeLength":321000,"extName":"mp3"}""",
            ),
        )

        val failure = assertThrows(ApiProtocolException::class.java) {
            runTest { dataSource(anonymousSession).resolveSongSource("ABCDEF", "12", "34") }
        }

        assertThat(failure.reason).isEqualTo(ApiProtocolException.Reason.InsecureMediaUrl)
    }

    @Test
    fun songSourceClassifiesCopyrightWithoutAcceptingCleartextUrl() {
        gatewayServer.enqueue(jsonResponse("""{"status":3,"url":[]}"""))

        val failure = assertThrows(ApiPlaybackUnavailableException::class.java) {
            runTest { dataSource(anonymousSession).resolveSongSource("ABCDEF") }
        }

        assertThat(failure.reason).isEqualTo(ApiPlaybackUnavailableException.Reason.Copyright)
    }

    @Test
    fun songSourceClassifiesKnownVipServiceCodeWithoutHidingOtherServiceFailures() {
        gatewayServer.enqueue(jsonResponse("""{"status":0,"error_code":35104,"url":[]}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":0,"error_code":35105,"url":[]}"""))

        val vip = assertThrows(ApiPlaybackUnavailableException::class.java) {
            runTest { dataSource(anonymousSession).resolveSongSource("ABCDEF") }
        }
        assertThat(vip.reason).isEqualTo(ApiPlaybackUnavailableException.Reason.Vip)

        val other = assertThrows(ApiServiceException::class.java) {
            runTest { dataSource(anonymousSession).resolveSongSource("ABCDEF") }
        }
        assertThat(other.serviceCode).isEqualTo("35105")
    }

    @Test
    fun songSourceClassifiesVipAndMalformedResponseSeparately() {
        gatewayServer.enqueue(jsonResponse("""{"status":1,"url":[],"backupUrl":[]}"""))
        gatewayServer.enqueue(jsonResponse("""{"url":[]}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"url":["not-a-url"]}"""))

        val vip = assertThrows(ApiPlaybackUnavailableException::class.java) {
            runTest { dataSource(anonymousSession).resolveSongSource("ABCDEF") }
        }
        assertThat(vip.reason).isEqualTo(ApiPlaybackUnavailableException.Reason.Vip)
        assertThrows(ApiProtocolException::class.java) {
            runTest { dataSource(anonymousSession).resolveSongSource("ABCDEF") }
        }
        assertThrows(ApiProtocolException::class.java) {
            runTest { dataSource(anonymousSession).resolveSongSource("ABCDEF") }
        }
    }

    @Test
    fun rankingsUseFixedQueryAndDecodeRequiredFields() = runTest {
        gatewayServer.enqueue(
            jsonResponse(
                """{"status":1,"data":{"info":[{"rankid":6666,"rankname":"热歌榜","imgurl":"https://img/{size}.jpg"}]}}""",
            ),
        )

        val rankings = dataSource().rankings()

        assertThat(rankings.single().id).isEqualTo("6666")
        assertThat(rankings.single().title).isEqualTo("热歌榜")
        val request = gatewayServer.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.requestUrl?.encodedPath).isEqualTo("/ocean/v6/rank/list")
        assertThat(request.requestUrl?.queryParameter("plat")).isEqualTo("2")
        assertThat(request.requestUrl?.queryParameter("withsong")).isEqualTo("1")
        assertThat(request.requestUrl?.queryParameter("parentid")).isEqualTo("0")
        assertThat(request.requestUrl?.queryParameter("signature")).isNotEmpty()
    }

    @Test
    fun rankingSongsUseFixedBodyHeaderAndDecodePage() = runTest {
        gatewayServer.enqueue(
            jsonResponse(
                """{"status":1,"data":{"total":31,"songlist":[{"hash":"RANK-HASH","filename":"歌手 - 榜单歌曲","duration":201,"remark":"专辑"}]}}""",
            ),
        )

        val page = dataSource().rankingSongs(rankId = "6666", page = 2, pageSize = 30)

        assertThat(page.songs.single().title).isEqualTo("榜单歌曲")
        assertThat(page.songs.single().albumTitle).isEqualTo("专辑")
        assertThat(page.total).isEqualTo(31)
        assertThat(page.hasMore).isFalse()
        val request = gatewayServer.takeRequest()
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.requestUrl?.encodedPath).isEqualTo("/openapi/kmr/v2/rank/audio")
        assertThat(request.getHeader("kg-tid")).isEqualTo("369")
        assertThat(request.requestUrl?.queryParameter("signature")).isNotEmpty()
        val body = json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertThat(body["rank_id"]?.jsonPrimitive?.content).isEqualTo("6666")
        assertThat(body["page"]?.jsonPrimitive?.content).isEqualTo("2")
        assertThat(body["pagesize"]?.jsonPrimitive?.content).isEqualTo("30")
        assertThat(body["rank_cid"]?.jsonPrimitive?.content).isEqualTo("0")
        assertThat(body["show_portrait_mv"]?.jsonPrimitive?.content).isEqualTo("1")
    }

    @Test
    fun playlistSongsUseOffsetAndDecodeInfoTracksAndFileId() = runTest {
        gatewayServer.enqueue(
            jsonResponse(
                """{"status":1,"data":{"count":51,"list_info":{"name":"精选歌单","intro":"简介","pic":"https://img/{size}.jpg"},"songs":[{"hash":"PLAYLIST-HASH","name":"歌手 - 歌单歌曲","timelen":180000,"fileid":42,"cover":"https://song/{size}.jpg"}]}}""",
            ),
        )

        val page = dataSource().playlistSongs("collection-id", page = 2, pageSize = 50)

        assertThat(page.info?.id).isEqualTo("collection-id")
        assertThat(page.info?.title).isEqualTo("精选歌单")
        assertThat(page.info?.songCount).isEqualTo(51)
        assertThat(page.songs.single().fileId).isEqualTo("42")
        assertThat(page.songs.single().coverUrl).isEqualTo("https://song/{size}.jpg")
        assertThat(page.hasMore).isFalse()
        val request = gatewayServer.takeRequest()
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.requestUrl?.encodedPath).isEqualTo("/pubsongs/v2/get_other_list_file_nofilt")
        assertThat(request.requestUrl?.queryParameter("begin_idx")).isEqualTo("50")
        assertThat(request.requestUrl?.queryParameter("pagesize")).isEqualTo("50")
        assertThat(request.requestUrl?.queryParameter("global_collection_id")).isEqualTo("collection-id")
        assertThat(request.requestUrl?.queryParameter("extend_fields")).isEqualTo("abtags,hot_cmt,popularization")
        assertThat(request.requestUrl?.queryParameter("signature")).isNotEmpty()
    }

    @Test
    fun collectionEndpointsRejectNonEmptyUnusableListsAsProtocolFailure() {
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"info":[{"rankid":"","rankname":""}]}}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"songlist":[{"hash":"","name":""}]}}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"songs":[{"hash":"","name":""}]}}"""))

        assertThrows(ApiProtocolException::class.java) { runTest { dataSource().rankings() } }
        assertThrows(ApiProtocolException::class.java) { runTest { dataSource().rankingSongs("1") } }
        assertThrows(ApiProtocolException::class.java) { runTest { dataSource().playlistSongs("gid") } }
    }

    @Test
    fun playlistRejectsListInfoWithoutRequiredTitle() {
        gatewayServer.enqueue(
            jsonResponse(
                """{"status":1,"data":{"count":1,"list_info":{"intro":"missing title"},"songs":[]}}""",
            ),
        )

        assertThrows(ApiProtocolException::class.java) {
            runTest { dataSource().playlistSongs("gid") }
        }
    }

    @Test
    fun playlistDoesNotCreateBlankDetailsFromCountAlone() = runTest {
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"count":1,"songs":[]}}"""))

        val page = dataSource().playlistSongs("gid")

        assertThat(page.info).isNull()
    }

    @Test
    fun collectionPagingTreatsZeroTotalsAsUnknownAndStopsOnShortPages() = runTest {
        val rankSong = """{"hash":"RANK","filename":"歌手 - 榜单歌曲"}"""
        val playlistSong = """{"hash":"PLAYLIST","name":"歌手 - 歌单歌曲"}"""
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"total":0,"songlist":[$rankSong,$rankSong]}}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"total":100,"songlist":[$rankSong]}}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"count":0,"songs":[$playlistSong,$playlistSong]}}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"count":100,"songs":[$playlistSong]}}"""))

        val rankingWithUnknownTotal = dataSource().rankingSongs("rank", page = 1, pageSize = 2)
        assertThat(rankingWithUnknownTotal.hasMore).isTrue()
        assertThat(rankingWithUnknownTotal.total).isNull()
        assertThat(dataSource().rankingSongs("rank", page = 1, pageSize = 2).hasMore).isFalse()
        assertThat(dataSource().playlistSongs("gid", page = 1, pageSize = 2).hasMore).isTrue()
        assertThat(dataSource().playlistSongs("gid", page = 1, pageSize = 2).hasMore).isFalse()
    }

    @Test
    fun songQualityUsesRelateGoodsWhenQualityHashesAreAbsent() = runTest {
        gatewayServer.enqueue(
            jsonResponse(
                """{"status":1,"data":{"song_list":[{"hash":"HQ","songname":"高品质","relate_goods":[{},{}]},{"hash":"SQ","songname":"无损","relate_goods":[{},{},{}]}]}}""",
            ),
        )

        val songs = dataSource().dailyRecommendations()

        assertThat(songs[0].highQualityAvailable).isTrue()
        assertThat(songs[0].losslessAvailable).isFalse()
        assertThat(songs[0].artist).isNull()
        assertThat(songs[1].losslessAvailable).isTrue()
    }

    @Test
    fun dailyRecommendationsTreatObjectRelateGoodsAsUnavailable() = runTest {
        gatewayServer.enqueue(
            jsonResponse(
                """{"status":1,"data":{"song_list":[{"hash":"HASH","songname":"Song","relate_goods":{}}]}}""",
            ),
        )

        val song = dataSource().dailyRecommendations().single()

        assertThat(song.highQualityAvailable).isFalse()
        assertThat(song.losslessAvailable).isFalse()
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
    fun searchSongsDecodeIntoSharedNetworkSong() = runTest {
        gatewayServer.enqueue(
            jsonResponse(
                """{"status":1,"data":{"lists":[{"FileHash":"SEARCH_HASH","OriSongName":"Search Song","SingerName":"Search Artist","Image":"https://example.com/cover.jpg","Duration":245,"HQFileHash":"HQ_HASH","SQFileHash":"SQ_HASH","Privilege":10,"trans_param":{"hash_offset":{"start_ms":48971,"end_ms":108971}}}],"total":1}}""",
            ),
        )

        val page = dataSource().searchSongs("fixture", page = 1, pageSize = 1)
        val request = gatewayServer.takeRequest()

        assertThat(page.total).isEqualTo(1)
        assertThat(request.getHeader("Cookie")).contains("token=existing-token")
        assertThat(request.getHeader("Cookie")).contains("userid=99")
        assertThat(request.requestUrl?.queryParameter("token")).isEqualTo("existing-token")
        assertThat(request.requestUrl?.queryParameter("userid")).isEqualTo("99")
        with(page.items.single()) {
            assertThat(hash).isEqualTo("SEARCH_HASH")
            assertThat(title).isEqualTo("Search Song")
            assertThat(artist).isEqualTo("Search Artist")
            assertThat(coverUrl).isEqualTo("https://example.com/cover.jpg")
            assertThat(durationMillis).isEqualTo(245_000)
            assertThat(highQualityHash).isEqualTo("HQ_HASH")
            assertThat(losslessHash).isEqualTo("SQ_HASH")
            assertThat(highQualityAvailable).isTrue()
            assertThat(losslessAvailable).isTrue()
            assertThat(vip).isTrue()
            assertThat(previewDurationMillis).isEqualTo(60_000)
        }
    }

    @Test
    fun anonymousSearchAuthenticationCodeBecomesLoginRequired() = runTest {
        gatewayServer.enqueue(jsonResponse("""{"status":0,"error_code":" 152 ","data":{"lists":[]}}"""))
        val anonymous = session.copy(token = null, userId = null, cookies = mapOf("dfid" to "fixture-dfid"))

        val failure = runCatching { dataSource(anonymous).searchSongs("fixture", page = 1, pageSize = 1) }
            .exceptionOrNull() as ApiAuthenticationRequiredException

        assertThat(failure.reason).isEqualTo(ApiAuthenticationGateReason.LoginRequired)
        assertThat(failure.serviceCode).isEqualTo("152")
        val request = gatewayServer.takeRequest()
        assertThat(request.getHeader("Cookie")).doesNotContain("token=")
        assertThat(request.requestUrl?.queryParameter("token")).isNull()
        assertThat(request.requestUrl?.queryParameter("userid")).isNull()
    }

    @Test
    fun authenticatedSearchAuthenticationCodeExpiresSession() = runTest {
        gatewayServer.enqueue(jsonResponse("""{"status":0,"error_code":152,"data":{"lists":[]}}"""))
        val source = dataSource()

        val failure = runCatching { source.searchSongs("fixture", page = 1, pageSize = 1) }
            .exceptionOrNull() as ApiAuthenticationRequiredException

        assertThat(failure.reason).isEqualTo(ApiAuthenticationGateReason.SessionExpired)
        assertThat(failure.serviceCode).isEqualTo("152")
        assertThat(source.authenticationClearCount).isEqualTo(1)
    }

    @Test
    fun typedSearchAuthenticationCodeUsesTheSharedSearchGate() = runTest {
        complexSearchServer.enqueue(jsonResponse("""{"status":0,"error_code":152,"data":{"lists":[]}}"""))

        val source = dataSource()
        val failure = runCatching { source.searchPlaylists("fixture", page = 1, pageSize = 30) }
            .exceptionOrNull() as ApiAuthenticationRequiredException

        assertThat(failure.reason).isEqualTo(ApiAuthenticationGateReason.SessionExpired)
        assertThat(failure.serviceCode).isEqualTo("152")
        assertThat(source.authenticationClearCount).isEqualTo(1)
    }

    @Test
    fun songCandidateHttpUnauthorizedFallsBackWithoutExpiringSession() = runTest {
        gatewayServer.enqueue(
            jsonResponse(
                """{"status":1,"data":[{"hash":"HIGH","quality":"320","level":1,"relate_goods":[{"hash":"STANDARD","quality":"128","level":1}]}]}""",
            ),
        )
        gatewayServer.enqueue(MockResponse().setResponseCode(401))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"url":["https://cdn.example/song.mp3"],"extName":"mp3"}"""))
        val source = dataSource()

        val song = source.resolveSongSource("hash", requestedQuality = "320")

        assertThat(song.uri).isEqualTo("https://cdn.example/song.mp3")
        assertThat(source.authenticationClearCount).isEqualTo(0)
        assertThat(gatewayServer.requestCount).isEqualTo(3)
    }

    @Test
    fun aggregatedSearchHttpUnauthorizedUsesTheSameAuthenticationGate() = runTest {
        gatewayServer.enqueue(MockResponse().setResponseCode(401))

        val failure = runCatching { dataSource().searchSongs("fixture", 1, 1) }
            .exceptionOrNull() as ApiAuthenticationRequiredException

        assertThat(failure.reason).isEqualTo(ApiAuthenticationGateReason.SessionExpired)
    }

    @Test
    fun qrLoginHttpForbiddenRemainsOrdinaryHttpFailure() = runTest {
        qrLoginServer.enqueue(MockResponse().setResponseCode(403))

        val failure = runCatching { dataSource().createQrLoginKey() }
            .exceptionOrNull() as ApiHttpException

        assertThat(failure.statusCode).isEqualTo(403)
    }

    @Test
    fun typedSearchUsesMobilePathsPagingAndConsumerFields() = runTest {
        complexSearchServer.enqueue(jsonResponse("""{"status":1,"data":{"lists":[{"gid":"gid","specialname":"<em>歌单</em>","nickname":"作者","song_count":8,"play_count":99}],"total":61}}"""))
        complexSearchServer.enqueue(jsonResponse("""{"status":1,"data":{"lists":[{"albumid":"album","albumname":"<em>专辑</em>","singername":"歌手","songcount":7,"publish_time":"2026-08-12 00:00:00"}],"total":1}}"""))
        complexSearchServer.enqueue(jsonResponse("""{"status":1,"data":{"lists":[{"AuthorId":"artist","AuthorName":"<em>歌手</em>","AlbumCount":3,"AudioCount":9}],"total":1}}"""))
        complexSearchServer.enqueue(jsonResponse("""{"status":1,"data":{"lists":[{"MvHash":"mv","MvName":"<em>MV</em>","SingerName":"歌手","Duration":180}],"total":1}}"""))

        val playlists = dataSource().searchPlaylists(" query ", page = 2, pageSize = 30)
        val albums = dataSource().searchAlbums("query", page = 1, pageSize = 30)
        val artists = dataSource().searchArtists("query", page = 1, pageSize = 30)
        val mvs = dataSource().searchMvs("query", page = 1, pageSize = 30)

        assertThat(playlists.items.single().name).isEqualTo("歌单")
        assertThat(playlists.total).isEqualTo(61)
        assertThat(playlists.hasMore).isTrue()
        assertThat(albums.items.single().publishDate).isEqualTo("2026-08-12")
        assertThat(artists.items.single().songCount).isEqualTo(9)
        assertThat(mvs.items.single().durationMillis).isEqualTo(180_000)
        listOf("special", "album", "author", "mv").forEachIndexed { index, type ->
            val request = complexSearchServer.takeRequest()
            assertThat(request.requestUrl?.encodedPath).isEqualTo("/v1/search/$type")
            assertThat(request.requestUrl?.queryParameter("keyword")).isEqualTo("query")
            assertThat(request.requestUrl?.queryParameter("page")).isEqualTo(if (index == 0) "2" else "1")
            assertThat(request.getHeader("x-router")).isEqualTo("complexsearch.kugou.com")
        }
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
        val encodedBody = register.body.readUtf8()
        assertThat(encodedBody).matches("[A-Za-z0-9+/]+={0,2}")
        val encryptedBody = Base64.getDecoder().decode(encodedBody)
        val registrationBody = json.parseToJsonElement(crypto.decryptPlaylist(encryptedBody, "aaaaaa")).jsonObject
        assertThat(registrationBody["availableRamSize"]?.jsonPrimitive?.content).isEqualTo("8000000000")
        assertThat(registrationBody["availableRomSize"]?.jsonPrimitive?.content).isEqualTo("64000000000")
        assertThat(registrationBody["availableSDSize"]?.jsonPrimitive?.content).isEqualTo("32000000000")
        assertThat(registrationBody["brand"]?.jsonPrimitive?.content).isEqualTo("FixtureBrand")
        assertThat(registrationBody["buildSerial"]?.jsonPrimitive?.content).isEqualTo("FixtureBuild")
        assertThat(registrationBody["device"]?.jsonPrimitive?.content).isEqualTo("fixture-device")
        assertThat(registrationBody["manufacturer"]?.jsonPrimitive?.content).isEqualTo("FixtureManufacturer")
        assertThat(registrationBody["imei"]?.jsonPrimitive?.content).isEqualTo("fixture-guid")
        assertThat(registrationBody["uuid"]?.jsonPrimitive?.content).isEqualTo("fixture-guid")
        assertThat(search.requestUrl?.queryParameter("dfid")).isEqualTo("registered-dfid")
        assertThat(search.requestUrl?.queryParameter("signature")).isNotEmpty()
    }

    @Test
    fun deviceRegistrationAcceptsDfidFromResponseCookie() = runTest {
        val registration = crypto.encryptPlaylist("""{"status":1}""")
        deviceRegistrationServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Set-Cookie", "dfid=cookie-dfid; Path=/; HttpOnly")
                .setBody(Buffer().write(registration.ciphertext)),
        )
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"lists":[],"total":0}}"""))

        dataSource(session.copy(dfid = null, token = null, userId = null, cookies = emptyMap()))
            .searchSongs("fixture", page = 1, pageSize = 1)

        val search = gatewayServer.takeRequest()
        assertThat(search.requestUrl?.queryParameter("dfid")).isEqualTo("cookie-dfid")
        assertThat(search.getHeader("Cookie")).contains("dfid=cookie-dfid")
    }

    @Test
    fun contentRequestsReuseSingleDeviceRegistration() = runTest {
        val registration = crypto.encryptPlaylist("""{"status":1,"data":{"dfid":"registered-dfid"}}""")
        deviceRegistrationServer.enqueue(MockResponse().setResponseCode(200).setBody(Buffer().write(registration.ciphertext)))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"song_list":[]}}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"special_list":[]}}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"info":[]}}"""))
        val dataSource = dataSource(session.copy(dfid = null, token = null, userId = null, cookies = emptyMap()))

        dataSource.dailyRecommendations()
        dataSource.recommendedPlaylists(page = 1, pageSize = 6)
        dataSource.rankings()

        assertThat(deviceRegistrationServer.requestCount).isEqualTo(1)
        assertThat(gatewayServer.takeRequest().requestUrl?.queryParameter("dfid")).isEqualTo("registered-dfid")
        assertThat(gatewayServer.takeRequest().requestUrl?.queryParameter("dfid")).isEqualTo("registered-dfid")
        assertThat(gatewayServer.takeRequest().requestUrl?.queryParameter("dfid")).isEqualTo("registered-dfid")
    }

    @Test
    fun concurrentRequestsShareOneDeviceRegistration() = runTest {
        val registration = crypto.encryptPlaylist("""{"status":1,"data":{"dfid":"registered-dfid"}}""")
        deviceRegistrationServer.enqueue(MockResponse().setResponseCode(200).setBody(Buffer().write(registration.ciphertext)))
        repeat(3) {
            gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"lists":[],"total":0}}"""))
        }
        val dataSource = dataSource(session.copy(dfid = null, token = null, userId = null, cookies = emptyMap()))

        List(3) { index ->
            async { dataSource.searchSongs("fixture-$index", page = 1, pageSize = 1) }
        }.awaitAll()

        assertThat(deviceRegistrationServer.requestCount).isEqualTo(1)
        repeat(3) {
            assertThat(gatewayServer.takeRequest().requestUrl?.queryParameter("dfid")).isEqualTo("registered-dfid")
        }
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
        assertThat(request.getHeader("Cookie")).isEqualTo("mid=fixture-mid")
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
    fun passwordLoginMatchesMobileContractAndCommitsDecodedCredentials() = runTest {
        val encrypted = crypto.encryptTemporary("""{"token":"password-token","userid":"84","vip_type":"2","vip_token":"vip"}""")
        gatewayServer.enqueue(
            jsonResponse("""{"status":1,"data":{"secu_params":"${encrypted.ciphertextHex}"}}""")
                .addHeader("Set-Cookie", "server_cookie=password-value; Path=/; HttpOnly"),
        )

        val result = dataSource().loginWithPassword(" 13800000000 ", "fixture-password")

        val authenticated = result as NetworkPasswordLoginResult.Authenticated
        assertThat(authenticated.session.token).isEqualTo("password-token")
        assertThat(authenticated.session.userId).isEqualTo("84")
        assertThat(authenticated.session.cookies).containsEntry("vip_type", "2")
        assertThat(authenticated.session.cookies).containsEntry("vip_token", "vip")
        assertThat(authenticated.session.cookies).containsEntry("server_cookie", "password-value")
        val request = gatewayServer.takeRequest()
        assertThat(request.requestUrl?.encodedPath).isEqualTo("/v9/login_by_pwd")
        assertThat(request.requestUrl?.queryParameter("signature")).isNotEmpty()
        assertThat(request.getHeader("x-router")).isEqualTo("login.user.kugou.com")
        assertThat(request.getHeader("Cookie")).isEqualTo("mid=fixture-mid")
        val rawBody = request.body.readUtf8()
        val body = json.parseToJsonElement(rawBody).jsonObject
        assertThat(body["plat"]?.jsonPrimitive?.content).isEqualTo("1")
        assertThat(body["support_multi"]?.jsonPrimitive?.content).isEqualTo("1")
        assertThat(body["clienttime_ms"]?.jsonPrimitive?.content).isEqualTo("1700000000123")
        assertThat(body["username"]?.jsonPrimitive?.content).isEqualTo("13800000000")
        assertThat(body["t1"]?.jsonPrimitive?.content).isNotEmpty()
        assertThat(body["t2"]?.jsonPrimitive?.content).isNotEmpty()
        assertThat(body["t3"]?.jsonPrimitive?.content).isEqualTo("MCwwLDAsMCwwLDAsMCwwLDA=")
        assertThat(body["pk"]?.jsonPrimitive?.content).matches("[0-9A-F]+")
        val decrypted = crypto.decryptTemporary(body.getValue("params").jsonPrimitive.content, "a".repeat(16))
        assertThat(decrypted).contains("\"pwd\":\"fixture-password\"")
        assertThat(rawBody).doesNotContain("fixture-password")
    }

    @Test
    fun passwordLoginMapsMultipleAccounts() = runTest {
        gatewayServer.enqueue(
            jsonResponse(
                """{"status":0,"error_code":1001,"data":{"info_list":[{"userid":"84","nickname":"account","pic":"avatar"}]}}""",
            ),
        )

        val result = dataSource().loginWithPassword("account", "password")

        val accounts = (result as NetworkPasswordLoginResult.MultipleAccounts).accounts
        assertThat(accounts.single().userId).isEqualTo("84")
    }

    @Test
    fun passwordLoginSurfacesRiskBeforeServiceRejection() {
        gatewayServer.enqueue(
            jsonResponse("""{"status":0,"error_code":20028,"data":{"ssaCode":"password-event","sid":"sid","edt":"edt"}}"""),
        )

        val failure = assertThrows(ApiRiskException::class.java) {
            runTest { dataSource().loginWithPassword("account", "password") }
        }

        assertThat(failure.challenge.eventId).isEqualTo("password-event")
        assertThat(failure.challenge.sid).isEqualTo("sid")
        assertThat(failure.challenge.edt).isEqualTo("edt")
    }

    @Test
    fun passwordLoginRejectsMalformedEncryptedCredentials() {
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"secu_params":"not-hex"}}"""))

        val failure = assertThrows(ApiProtocolException::class.java) {
            runTest { dataSource().loginWithPassword("account", "password") }
        }

        assertThat(failure.reason).isEqualTo(ApiProtocolException.Reason.MalformedResponse)
    }

    @Test
    fun passwordLoginKeepsServiceRejectionTyped() {
        gatewayServer.enqueue(jsonResponse("""{"status":0,"error_code":"PASSWORD_REJECTED"}"""))

        val failure = assertThrows(ApiServiceException::class.java) {
            runTest { dataSource().loginWithPassword("account", "password") }
        }

        assertThat(failure.serviceCode).isEqualTo("PASSWORD_REJECTED")
    }

    @Test
    fun userDetailUsesAuthenticatedMobileContractAndMapsConsumerFields() = runTest {
        gatewayServer.enqueue(
            jsonResponse(
                """{"status":1,"data":{"userid":99,"nickname":"Fixture User","pic":"https://avatar/{size}","bg_pic":"https://background/{size}","descri":"signature","fans":"12","follows":3,"duration":456}}""",
            ),
        )

        val detail = dataSource().userDetail()

        assertThat(detail.userId).isEqualTo("99")
        assertThat(detail.nickname).isEqualTo("Fixture User")
        assertThat(detail.fans).isEqualTo(12)
        assertThat(detail.listenMinutes).isEqualTo(456)
        val request = gatewayServer.takeRequest()
        assertThat(request.requestUrl?.encodedPath).isEqualTo("/v3/get_my_info")
        assertThat(request.requestUrl?.queryParameter("plat")).isEqualTo("1")
        assertThat(request.getHeader("x-router")).isEqualTo("usercenter.kugou.com")
        val body = json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertThat(body["visit_time"]?.jsonPrimitive?.content).isEqualTo("1700000000")
        assertThat(body["usertype"]?.jsonPrimitive?.content).isEqualTo("1")
        assertThat(body["userid"]?.jsonPrimitive?.content).isEqualTo("99")
        assertThat(body["p"]?.jsonPrimitive?.content).matches("[0-9A-F]+")
    }

    @Test
    fun userDetailBusinessCodesRemainOrdinaryServiceFailures() = runTest {
        listOf("152", "20010", "20017").forEach { serviceCode ->
            gatewayServer.enqueue(jsonResponse("""{"status":0,"error_code":$serviceCode}"""))
            val source = dataSource()

            val failure = runCatching { source.userDetail() }.exceptionOrNull()

            assertThat(failure).isInstanceOf(ApiServiceException::class.java)
            failure as ApiServiceException
            assertThat(failure.serviceCode).isEqualTo(serviceCode)
            assertThat(source.authenticationClearCount).isEqualTo(0)
        }
        assertThat(vipServer.requestCount).isEqualTo(0)
    }

    @Test
    fun userVipUsesDedicatedOriginAndMapsActiveSvip() = runTest {
        vipServer.enqueue(
            jsonResponse("""{"status":1,"data":{"busi_vip":[{"is_vip":0,"product_type":"vip"},{"is_vip":"1","product_type":"SVIP"}]}}"""),
        )

        val vip = dataSource().userVip()

        assertThat(vip.isVip).isTrue()
        assertThat(vip.label).isEqualTo("SVIP")
        val request = vipServer.takeRequest()
        assertThat(request.requestUrl?.encodedPath).isEqualTo("/v1/get_union_vip")
        assertThat(request.requestUrl?.queryParameter("busi_type")).isEqualTo("concept")
        assertThat(request.getHeader("Cookie")).contains("token=existing-token")
    }

    @Test
    fun userVipBusinessFailureDoesNotExpireAuthenticatedSession() = runTest {
        vipServer.enqueue(jsonResponse("""{"status":0,"error_code":20017}"""))
        val source = dataSource()

        val failure = runCatching { source.userVip() }.exceptionOrNull()

        assertThat(failure).isInstanceOf(ApiServiceException::class.java)
        failure as ApiServiceException
        assertThat(failure.serviceCode).isEqualTo("20017")
        assertThat(source.authenticationClearCount).isEqualTo(0)
    }

    @Test
    fun userVipHttpUnauthorizedExpiresAuthenticatedSession() = runTest {
        vipServer.enqueue(MockResponse().setResponseCode(401))
        val source = dataSource()

        val failure = runCatching { source.userVip() }.exceptionOrNull()

        assertThat(failure).isInstanceOf(ApiAuthenticationRequiredException::class.java)
        failure as ApiAuthenticationRequiredException
        assertThat(failure.reason).isEqualTo(ApiAuthenticationGateReason.SessionExpired)
        assertThat(source.authenticationClearCount).isEqualTo(1)
    }

    @Test
    fun userPlaylistsMatchesMobilePagingAndFiltersAlbumEntries() = runTest {
        gatewayServer.enqueue(
            jsonResponse(
                """{"status":1,"data":{"info":[{"listid":1,"list_create_gid":"gid-like","name":"我喜欢","pic":"https://cover/{size}","count":"8","list_create_userid":99},{"listid":"2","list_create_gid":"gid-album","name":"album","authors":["artist"]},{"listid":"","list_create_gid":"bad","name":"invalid"}]}}""",
            ),
        )

        val playlists = dataSource().userPlaylists(page = 1, pageSize = 200)

        assertThat(playlists).hasSize(1)
        assertThat(playlists.single().listId).isEqualTo("1")
        assertThat(playlists.single().isMine).isTrue()
        assertThat(playlists.single().isLike).isTrue()
        val request = gatewayServer.takeRequest()
        assertThat(request.requestUrl?.encodedPath).isEqualTo("/v7/get_all_list")
        assertThat(request.requestUrl?.queryParameter("userid")).isEqualTo("99")
        assertThat(request.requestUrl?.queryParameter("token")).isEqualTo("existing-token")
        assertThat(request.getHeader("x-router")).isEqualTo("cloudlist.service.kugou.com")
        val body = json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertThat(body["total_ver"]?.jsonPrimitive?.content).isEqualTo("979")
        assertThat(body["type"]?.jsonPrimitive?.content).isEqualTo("2")
        assertThat(body["pagesize"]?.jsonPrimitive?.content).isEqualTo("200")
    }

    @Test
    fun userEndpointsRejectAnonymousSessionBeforeRequest() {
        val anonymous = session.copy(token = null, userId = null, cookies = emptyMap())

        assertThrows(ApiAuthenticationRequiredException::class.java) {
            runTest { dataSource(anonymous).userDetail() }
        }

        assertThat(gatewayServer.requestCount).isEqualTo(0)
    }

    @Test
    fun unverifiedAccountEndpointCodesRemainOrdinaryServiceFailures() = runTest {
        suspend fun assertServiceFailure(source: TestNetworkDataSource, action: suspend () -> Unit) {
            val failure = runCatching { action() }.exceptionOrNull()
            assertThat(failure).isInstanceOf(ApiServiceException::class.java)
            failure as ApiServiceException
            assertThat(failure.serviceCode).isEqualTo("20017")
            assertThat(source.authenticationClearCount).isEqualTo(0)
        }

        gatewayServer.enqueue(jsonResponse("""{"status":0,"error_code":20017}"""))
        val playlists = dataSource()
        assertServiceFailure(playlists) { playlists.userPlaylists(1, 20) }

        gatewayServer.enqueue(jsonResponse("""{"status":0,"error_code":20017}"""))
        val create = dataSource()
        assertServiceFailure(create) { create.createPlaylist("List") }

        gatewayServer.enqueue(jsonResponse("""{"status":0,"error_code":20017}"""))
        val cloud = dataSource()
        assertServiceFailure(cloud) { cloud.resolveCloudSongSource("HASH", "1", "Song") }

        gatewayServer.enqueue(jsonResponse("""{"status":0,"error_code":20017}"""))
        val vip = dataSource()
        assertServiceFailure(vip) { vip.claimDailyVip("2026-08-13") }
    }

    @Test
    fun createPlaylistMatchesMobileBodyAndReturnsListId() = runTest {
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"info":{"listid":321}}}"""))

        val listId = dataSource().createPlaylist("  Road Trip  ")

        assertThat(listId).isEqualTo("321")
        val request = gatewayServer.takeRequest()
        assertThat(request.requestUrl?.encodedPath).isEqualTo("/cloudlist.service/v5/add_list")
        assertThat(request.requestUrl?.queryParameter("last_time")).isEqualTo("1700000000")
        assertThat(request.requestUrl?.queryParameter("last_area")).isEqualTo("gztx")
        assertThat(request.requestUrl?.queryParameter("userid")).isEqualTo("99")
        assertThat(request.requestUrl?.queryParameter("token")).isEqualTo("existing-token")
        val body = json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertThat(body["userid"]?.jsonPrimitive?.content).isEqualTo("99")
        assertThat(body["token"]?.jsonPrimitive?.content).isEqualTo("existing-token")
        assertThat(body["name"]?.jsonPrimitive?.content).isEqualTo("Road Trip")
        assertThat(body["type"]?.jsonPrimitive?.content).isEqualTo("0")
        assertThat(body["source"]?.jsonPrimitive?.content).isEqualTo("1")
        assertThat(body["list_create_userid"]?.jsonPrimitive?.content).isEqualTo("99")
        assertThat(body["list_create_gid"]?.jsonPrimitive?.content).isEmpty()
    }

    @Test
    fun addPlaylistTracksBuildsResourcesAndSanitizesSeparators() = runTest {
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{}}"""))

        dataSource().addPlaylistTracks(
            "list-id",
            listOf(NetworkPlaylistTrackInput("HASH", "Title,Part", "Artist|Name", "12", "34")),
        )

        val request = gatewayServer.takeRequest()
        assertThat(request.requestUrl?.encodedPath).isEqualTo("/cloudlist.service/v6/add_song")
        assertThat(request.requestUrl?.queryParameter("last_time")).isEqualTo("1700000000")
        assertThat(request.requestUrl?.queryParameter("last_area")).isEqualTo("gztx")
        assertThat(request.requestUrl?.queryParameter("userid")).isEqualTo("99")
        val body = json.parseToJsonElement(request.body.readUtf8()).jsonObject
        val resource = body.getValue("data").jsonArray.single().jsonObject
        assertThat(resource["name"]?.jsonPrimitive?.content).isEqualTo("Artist Name - Title Part")
        assertThat(resource["hash"]?.jsonPrimitive?.content).isEqualTo("HASH")
        assertThat(resource["album_id"]?.jsonPrimitive?.content).isEqualTo("12")
        assertThat(resource["mixsongid"]?.jsonPrimitive?.content).isEqualTo("34")
        assertThat(body["scene"]?.jsonPrimitive?.content).isEqualTo("false;null")
    }

    @Test
    fun deletePlaylistTracksUsesFileIdsAndCloudListRouter() = runTest {
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{}}"""))

        dataSource().deletePlaylistTracks("list-id", listOf("91", "92"))

        val request = gatewayServer.takeRequest()
        assertThat(request.requestUrl?.encodedPath).isEqualTo("/v4/delete_songs")
        assertThat(request.getHeader("x-router")).isEqualTo("cloudlist.service.kugou.com")
        val body = json.parseToJsonElement(request.body.readUtf8()).jsonObject
        val fileIds = body.getValue("data").jsonArray.map { it.jsonObject.getValue("fileid").jsonPrimitive.content }
        assertThat(fileIds).containsExactly("91", "92").inOrder()
        assertThat(body["listid"]?.jsonPrimitive?.content).isEqualTo("list-id")
        assertThat(body["list_ver"]?.jsonPrimitive?.content).isEqualTo("0")
    }

    @Test
    fun playlistMutationRejectsNonSuccessAndDoesNotRetry() {
        gatewayServer.enqueue(jsonResponse("""{"status":0,"error_code":"WRITE_REJECTED"}"""))

        val failure = assertThrows(ApiServiceException::class.java) {
            runTest { dataSource().createPlaylist("name") }
        }

        assertThat(failure.serviceCode).isEqualTo("WRITE_REJECTED")
        assertThat(gatewayServer.requestCount).isEqualTo(1)
    }

    @Test
    fun playlistMutationRejectsNonZeroErrorCodeEvenWhenStatusIsOne() {
        gatewayServer.enqueue(jsonResponse("""{"status":1,"error_code":"WRITE_REJECTED"}"""))

        val failure = assertThrows(ApiServiceException::class.java) {
            runTest { dataSource().createPlaylist("name") }
        }

        assertThat(failure.serviceCode).isEqualTo("WRITE_REJECTED")
    }

    @Test
    fun cloudTracksUsesEncryptedMobileContractAndMapsCloudPage() = runTest {
        cloudServer.enqueue(
            encryptedCloudResponse(
                """{"status":1,"data":{"list":[{"hash":"CLOUDHASH","filename":"Fallback Artist - Fallback Song.mp3","author_name":"Fixture Artist","name":"Fixture Song","album_name":"Fixture Album","album_info":{"sizable_cover":"https://cover/{size}"},"timelen":"245","album_audio_id":321}],"list_count":"51","used_size":"100","max_size":1000}}""",
            ),
        )

        val page = dataSource().cloudTracks(page = 1, pageSize = 50)

        assertThat(page.tracks).hasSize(1)
        assertThat(page.tracks.single().title).isEqualTo("Fixture Song")
        assertThat(page.tracks.single().artist).isEqualTo("Fixture Artist")
        assertThat(page.tracks.single().durationMillis).isEqualTo(245_000)
        assertThat(page.tracks.single().albumAudioId).isEqualTo("321")
        assertThat(page.total).isEqualTo(51)
        assertThat(page.hasMore).isTrue()
        assertThat(page.storage?.usedBytes).isEqualTo(100)
        val request = cloudServer.takeRequest()
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.requestUrl?.encodedPath).isEqualTo("/v1/get_list")
        assertThat(request.getHeader("Content-Type")).startsWith("application/octet-stream")
        assertThat(request.requestUrl?.queryParameter("clienttime")).isEqualTo("1700000000")
        assertThat(request.requestUrl?.queryParameter("mid")).isEqualTo("fixture-mid")
        assertThat(request.requestUrl?.queryParameter("p")).matches("[0-9A-F]+")
        assertThat(request.requestUrl?.queryParameter("signature")).isNull()
        assertThat(request.requestUrl?.queryParameter("token")).isNull()
        val plaintext = crypto.decryptPlaylist(request.body.readByteArray(), "aaaaaa")
        val body = json.parseToJsonElement(plaintext).jsonObject
        assertThat(body["page"]?.jsonPrimitive?.content).isEqualTo("1")
        assertThat(body["pagesize"]?.jsonPrimitive?.content).isEqualTo("50")
        assertThat(body["getkmr"]?.jsonPrimitive?.content).isEqualTo("1")
    }

    @Test
    fun encryptedCloudRiskResponseRemainsTyped() {
        cloudServer.enqueue(
            encryptedCloudResponse("""{"status":0,"error_code":20028,"ssaCode":"cloud-event"}"""),
        )

        val failure = assertThrows(ApiRiskException::class.java) {
            runTest { dataSource().cloudTracks() }
        }

        assertThat(failure.challenge.eventId).isEqualTo("cloud-event")
        assertThat(failure.reason).isEqualTo(ApiRiskException.Reason.VerificationUnavailable)
    }

    @Test
    fun cloudSongUrlUsesMobileSignatureAndRequiresHttps() = runTest {
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"url":["https://audio.example/cloud-song.mp3"]}}"""))

        val source = dataSource().resolveCloudSongSource("  ABCDEF  ", "321", " Fixture Song ")

        assertThat(source.uri).isEqualTo("https://audio.example/cloud-song.mp3")
        assertThat(source.extension).isEqualTo("mp3")
        val request = gatewayServer.takeRequest()
        assertThat(request.requestUrl?.encodedPath).isEqualTo("/bsstrackercdngz/v2/query_musicclound_url")
        assertThat(request.requestUrl?.queryParameter("hash")).isEqualTo("abcdef")
        assertThat(request.requestUrl?.queryParameter("album_audio_id")).isEqualTo("321")
        assertThat(request.requestUrl?.queryParameter("pid")).isEqualTo("20026")
        assertThat(request.requestUrl?.queryParameter("bucket")).isEqualTo("musicclound")
        assertThat(request.requestUrl?.queryParameter("name")).isEqualTo("Fixture Song")
        assertThat(request.requestUrl?.queryParameter("key")).isEqualTo(ApiRequestSigner().signCloudKey("abcdef"))
    }

    @Test
    fun cloudSongUrlRejectsCleartextAndEmptyResults() {
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"url":"http://audio.example/song.mp3"}}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"url":[]}}"""))

        val insecure = assertThrows(ApiProtocolException::class.java) {
            runTest { dataSource().resolveCloudSongSource("hash") }
        }
        val unavailable = assertThrows(ApiPlaybackUnavailableException::class.java) {
            runTest { dataSource().resolveCloudSongSource("hash") }
        }

        assertThat(insecure.reason).isEqualTo(ApiProtocolException.Reason.InsecureMediaUrl)
        assertThat(unavailable.reason).isEqualTo(ApiPlaybackUnavailableException.Reason.Cloud)
    }

    @Test
    fun bannersAndPlaylistTagsMatchMobileContracts() = runTest {
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"ads":[{"id":7,"title":"精选","img_url":"https://image/{size}","extra":{"url":"https://example/banner"}}]}}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":[{"tag_id":1,"tag_name":"语种","son":[{"tag_id":11,"tag_name":"华语"}]}]}"""))

        val banners = dataSource().banners()
        val categories = dataSource().playlistCategories()

        assertThat(banners.single().id).isEqualTo("7")
        assertThat(banners.single().linkUrl).isEqualTo("https://example/banner")
        assertThat(categories.single().children.single().tagId).isEqualTo(11)
        val bannerRequest = gatewayServer.takeRequest()
        val bannerBody = json.parseToJsonElement(bannerRequest.body.readUtf8()).jsonObject
        assertThat(bannerRequest.requestUrl?.encodedPath).isEqualTo("/ads.gateway/v3/listen_banner")
        assertThat(bannerBody["channel"]?.jsonPrimitive?.content).isEqualTo("201")
        assertThat(bannerBody["userid"]?.jsonPrimitive?.content).isEqualTo("99")
        val tagsRequest = gatewayServer.takeRequest()
        val tagsBody = json.parseToJsonElement(tagsRequest.body.readUtf8()).jsonObject
        assertThat(tagsRequest.requestUrl?.encodedPath).isEqualTo("/pubsongs/v1/get_tags_by_type")
        assertThat(tagsBody["tag_type"]?.jsonPrimitive?.content).isEqualTo("collection")
        assertThat(tagsBody["source"]?.jsonPrimitive?.content).isEqualTo("3")
    }

    @Test
    fun albumsAndNestedAlbumSongsMapOnlyMobileConsumerFields() = runTest {
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"chn":[{"albumid":10,"albumname":"中文专辑","singername":"歌手","imgurl":"https://album/{size}","publishtime":"2026-08-12 00:00:00","songcount":"2"}],"eur":[],"jpn":[],"kor":[]}}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"songs":[{"audio_info":{"hash":"ALBUMHASH","duration":245,"hash_320":"HQ"},"base":{"audio_name":"专辑歌曲","author_name":"歌手","album_audio_id":321},"album_info":{"album_id":10,"album_name":"中文专辑","sizable_cover":"https://cover/{size}"},"copyright":{"privilege":10}}],"total":"31"}}"""))

        val albums = dataSource().newAlbums(page = 1, pageSize = 30)
        val songs = dataSource().albumSongs("10", page = 1, pageSize = 30)

        assertThat(albums.single().publishDate).isEqualTo("2026-08-12")
        assertThat(albums.single().songCount).isEqualTo(2)
        assertThat(songs.songs.single().durationMillis).isEqualTo(245_000)
        assertThat(songs.songs.single().vip).isTrue()
        assertThat(songs.total).isEqualTo(31)
        assertThat(songs.hasMore).isTrue()
        val albumsBody = json.parseToJsonElement(gatewayServer.takeRequest().body.readUtf8()).jsonObject
        assertThat(albumsBody["apiver"]?.jsonPrimitive?.content).isEqualTo("20")
        assertThat(albumsBody["withpriv"]?.jsonPrimitive?.content).isEqualTo("1")
        val songsRequest = gatewayServer.takeRequest()
        assertThat(songsRequest.getHeader("x-router")).isEqualTo("openapi.kugou.com")
        assertThat(songsRequest.getHeader("kg-tid")).isEqualTo("255")
    }

    @Test
    fun artistDetailAndAudiosUseMobileHeadersSortAndDedicatedOrigin() = runTest {
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"author_name":"Fixture Artist","sizable_avatar":"https://artist/{size}","intro":"bio","audio_count":"31","album_count":2,"mv_count":3,"fansnums":"400"}}"""))
        openApiServer.enqueue(jsonResponse("""{"status":1,"data":[{"hash":"ARTISTHASH","audio_name":"Artist Song","author_name":"Fixture Artist","album_id":10,"album_audio_id":20,"timelength":180,"hash_flac":"SQ","trans_param":{"union_cover":"https://song/{size}"}}]}"""))

        val detail = dataSource().artistDetail("88")
        val page = dataSource().artistSongs("88", page = 2, pageSize = 30, newestFirst = true)

        assertThat(detail?.songCount).isEqualTo(31)
        assertThat(detail?.fansCount).isEqualTo(400)
        assertThat(page.songs.single().losslessHash).isEqualTo("SQ")
        val detailRequest = gatewayServer.takeRequest()
        assertThat(detailRequest.getHeader("kg-tid")).isEqualTo("36")
        assertThat(json.parseToJsonElement(detailRequest.body.readUtf8()).jsonObject["author_id"]?.jsonPrimitive?.content).isEqualTo("88")
        val audioRequest = openApiServer.takeRequest()
        val audioBody = json.parseToJsonElement(audioRequest.body.readUtf8()).jsonObject
        assertThat(audioRequest.requestUrl?.encodedPath).isEqualTo("/kmr/v1/audio_group/author")
        assertThat(audioRequest.getHeader("kg-tid")).isEqualTo("220")
        assertThat(audioBody["sort"]?.jsonPrimitive?.content).isEqualTo("2")
        assertThat(audioBody["area_code"]?.jsonPrimitive?.content).isEqualTo("all")
    }

    @Test
    fun complexHotAndSuggestSearchMatchMobileConsumerShapes() = runTest {
        complexSearchServer.enqueue(jsonResponse("""{"status":1,"data":{"lists":[{"type":"author","lists":[{"AuthorId":1,"AuthorName":"<em>歌手</em>","AudioCount":9}]},{"type":"song","total":31,"lists":[{"FileHash":"HASH","OriSongName":"<em>歌曲</em>","SingerName":"歌手","Duration":245}]},{"type":"album","total":1,"lists":[{"albumid":2,"albumname":"专辑","singername":"歌手"}]},{"type":"collect","total":1,"lists":[{"gid":"gid","specialname":"歌单"}]},{"type":"mv","total":1,"lists":[{"MvHash":"MVHASH","MvName":"MV","Pic":"202608121234.jpg","Duration":180}]}]}}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"list":[{"keywords":[{"keyword":"热搜","reason":"热门"}]}]}}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":[{"RecordDatas":[{"HintInfo":"建议"},{"HintInfo":"建议"},{"HintInfo":"建议二"}]}]}"""))

        val complex = dataSource().searchComplex("  query  ")
        val hot = dataSource().hotSearchKeywords()
        val suggestions = dataSource().searchSuggestions("que")

        assertThat(complex.artists.single().name).isEqualTo("歌手")
        assertThat(complex.songs.single().durationMillis).isEqualTo(245_000)
        assertThat(complex.songsTotal).isEqualTo(31)
        assertThat(complex.mvs.single().coverUrl).isEqualTo("https://imge.kugou.com/mvhdpic/480/20260812/202608121234.jpg")
        assertThat(hot.single().reason).isEqualTo("热门")
        assertThat(suggestions).containsExactly("建议", "建议二").inOrder()
        assertThat(complexSearchServer.takeRequest().requestUrl?.queryParameter("keyword")).isEqualTo("query")
        assertThat(gatewayServer.takeRequest().getHeader("x-router")).isEqualTo("msearch.kugou.com")
        assertThat(gatewayServer.takeRequest().getHeader("x-router")).isEqualTo("searchtip.kugou.com")
    }

    @Test
    fun lyricEndpointsMatchAnonymousV2ContractAndDecodeKrc() = runTest {
        lyricsServer.enqueue(jsonResponse("""{"status":200,"candidates":[{"id":7,"accesskey":"access"}]}"""))
        val krc = "[0,1200]<0,600,0>Moe<600,600,0>Koe\\n[language:W10=]"
        val encoded = "a3JjMTjb6kGOA0B1Yb6EHB7jXVmQdtGEk33BRuAWDcIyBvbVqNuly6rgsLMFnUFuzQk2aQpfb3Q="
        lyricsServer.enqueue(jsonResponse("""{"status":200,"content":"$encoded","contenttype":0}"""))

        val candidate = dataSource().searchLyric("HASH", "321")
        val lyric = dataSource().downloadLyric(checkNotNull(candidate))

        assertThat(candidate.id).isEqualTo("7")
        assertThat(lyric).isEqualTo(krc)
        val search = lyricsServer.takeRequest()
        assertThat(search.requestUrl?.encodedPath).isEqualTo("/search")
        assertThat(search.requestUrl?.queryParameter("ver")).isEqualTo("1")
        assertThat(search.requestUrl?.queryParameter("man")).isEqualTo("yes")
        assertThat(search.requestUrl?.queryParameter("client")).isEqualTo("pc")
        assertThat(search.requestUrl?.queryParameter("hash")).isEqualTo("HASH")
        assertThat(search.requestUrl?.queryParameter("signature")).isNull()
        assertThat(search.requestUrl?.queryParameter("dfid")).isNull()
        assertThat(search.requestUrl?.queryParameter("token")).isNull()
        assertThat(search.requestUrl?.queryParameter("userid")).isNull()
        assertThat(search.getHeader("Cookie")).isNull()
        val download = lyricsServer.takeRequest()
        assertThat(download.requestUrl?.queryParameter("fmt")).isEqualTo("krc")
        assertThat(download.requestUrl?.queryParameter("signature")).isNull()
        assertThat(download.getHeader("Cookie")).isNull()
    }

    @Test
    fun emptyCloudListScalarIsTreatedAsAnAvailableEmptyPage() = runTest {
        cloudServer.enqueue(
            encryptedCloudResponse(
                """{"status":1,"error_code":0,"data":{"list":"","list_count":0,"used_size":"100","max_size":"1000"}}""",
            ),
        )

        val page = dataSource().cloudTracks(page = 1, pageSize = 50)

        assertThat(page.tracks).isEmpty()
        assertThat(page.total).isEqualTo(0)
        assertThat(page.hasMore).isFalse()
        assertThat(page.storage?.usedBytes).isEqualTo(100)
        assertThat(page.storage?.maxBytes).isEqualTo(1_000)
    }

    @Test
    fun lyricBusinessFailuresRemainTypedWhileRecognitionNoMatchStaysEmpty() = runTest {
        lyricsServer.enqueue(jsonResponse("""{"status":0,"error_code":12345}"""))
        lyricsServer.enqueue(jsonResponse("""{"status":0,"error_code":12346}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":0,"data":[]}"""))

        val searchFailure = runCatching { dataSource().searchLyric("HASH", "321") }.exceptionOrNull() as ApiServiceException
        val downloadFailure = runCatching {
            dataSource().downloadLyric(com.resonote.core.network.model.NetworkLyricCandidate("7", "access"))
        }.exceptionOrNull() as ApiServiceException
        val noMatches = dataSource().recognizeAudio(byteArrayOf(1, 2))

        assertThat(searchFailure.serviceCode).isEqualTo("12345")
        assertThat(downloadFailure.serviceCode).isEqualTo("12346")
        assertThat(noMatches).isEmpty()
    }

    @Test
    fun ordinaryRiskVipCopyrightAndQrStatesNeverClearAuthentication() = runTest {
        val source = dataSource()
        gatewayServer.enqueue(jsonResponse("""{"status":0,"error_code":12345,"data":{"song_list":[]}}"""))
        assertThat(runCatching { source.dailyRecommendations() }.exceptionOrNull())
            .isInstanceOf(ApiServiceException::class.java)

        gatewayServer.enqueue(jsonResponse("""{"status":0,"error_code":20028,"ssaCode":"event","data":{"song_list":[]}}"""))
        assertThat(runCatching { source.dailyRecommendations() }.exceptionOrNull())
            .isInstanceOf(ApiRiskException::class.java)

        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":[]}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":0,"error_code":35104,"url":[]}"""))
        assertThat(runCatching { source.resolveSongSource("VIP") }.exceptionOrNull())
            .isInstanceOf(ApiPlaybackUnavailableException::class.java)

        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":[]}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":3,"url":[]}"""))
        assertThat(runCatching { source.resolveSongSource("COPYRIGHT") }.exceptionOrNull())
            .isInstanceOf(ApiPlaybackUnavailableException::class.java)

        qrLoginServer.enqueue(jsonResponse("""{"status":1,"data":{"status":1}}"""))
        assertThat(source.checkQrLogin("qr-key")).isEqualTo(com.resonote.core.network.model.NetworkQrLoginStatus.Waiting)

        assertThat(source.authenticationClearCount).isEqualTo(0)
        assertThat(source.store.read()).isEqualTo(session)
    }

    @Test
    fun videoUrlUsesSongKeyAndRejectsCleartext() = runTest {
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"dynamic":{"backupdownurl":["https://video.example/fixture.mp4"]}}}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{"dynamic":{"downurl":"http://video.example/insecure.mp4"}}}"""))

        val url = dataSource().resolveVideoUrl("MVHASH")
        val insecure = runCatching { dataSource().resolveVideoUrl("MVHASH") }.exceptionOrNull() as ApiProtocolException

        assertThat(url).isEqualTo("https://video.example/fixture.mp4")
        assertThat(insecure.reason).isEqualTo(ApiProtocolException.Reason.InsecureMediaUrl)
        val request = gatewayServer.takeRequest()
        assertThat(request.getHeader("x-router")).isEqualTo("trackermv.kugou.com")
        assertThat(request.requestUrl?.queryParameter("cmd")).isEqualTo("123")
        assertThat(request.requestUrl?.queryParameter("key")).isNotEmpty()
    }

    @Test
    fun recognitionPostsRawPcmAndSortsMatchesByConfidence() = runTest {
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":[{"hash":"LOW","songname":"Low","dist":0.8},{"hash":"HIGH","songname":"High","singername":"Artist","dist":0.1,"timelength":180,"album":[{"albumname":"Album","sizable_cover":"https://cover/{size}"}]}]}"""))
        val pcm = byteArrayOf(1, 2, 3, 4)

        val matches = dataSource().recognizeAudio(pcm)

        assertThat(matches.map { it.song.hash }).containsExactly("HIGH", "LOW").inOrder()
        assertThat(matches.first().confidence).isWithin(0.0001).of(0.9)
        val request = gatewayServer.takeRequest()
        assertThat(request.requestUrl?.encodedPath).isEqualTo("/fingerprint.service/v1/music_trackid_mulit")
        assertThat(request.requestUrl?.queryParameter("fpid")).isEqualTo("1700000000123")
        assertThat(request.requestUrl?.queryParameter("useid")).isEqualTo("99")
        assertThat(request.requestUrl?.queryParameter("multi_result")).isEqualTo("1")
        assertThat(request.getHeader("User-Agent")).isEqualTo("KuGou/11490 (Android)")
        assertThat(request.body.readByteArray()).isEqualTo(pcm)
    }

    @Test
    fun qrLoginUsesWebSignatureAndBuildsAuthenticatedSession() = runTest {
        qrLoginServer.enqueue(jsonResponse("""{"status":1,"data":{"qrcode":"qr-key"}}"""))
        qrLoginServer.enqueue(jsonResponse("""{"status":1,"data":{"status":2,"nickname":"Fixture"}}"""))
        qrLoginServer.enqueue(jsonResponse("""{"status":1,"data":{"status":4,"token":"qr-token","userid":123}}"""))

        val key = dataSource().createQrLoginKey()
        val scanned = dataSource().checkQrLogin(key) as com.resonote.core.network.model.NetworkQrLoginStatus.Scanned
        val authenticated = dataSource().checkQrLogin(key) as com.resonote.core.network.model.NetworkQrLoginStatus.Authenticated

        assertThat(key).isEqualTo("qr-key")
        assertThat(scanned.nickname).isEqualTo("Fixture")
        assertThat(authenticated.session.token).isEqualTo("qr-token")
        assertThat(authenticated.session.userId).isEqualTo("123")
        assertThat(authenticated.session.cookies).containsEntry("token", "qr-token")
        val create = qrLoginServer.takeRequest()
        assertThat(create.requestUrl?.encodedPath).isEqualTo("/v2/qrcode")
        assertThat(create.requestUrl?.queryParameter("appid")).isEqualTo("1001")
        assertThat(create.requestUrl?.queryParameter("srcappid")).isEqualTo("2919")
        assertThat(create.requestUrl?.queryParameter("qrcode_txt")).contains("appid=3116")
        assertThat(create.requestUrl?.queryParameter("signature")).isNotEmpty()
        val check = qrLoginServer.takeRequest()
        assertThat(check.requestUrl?.encodedPath).isEqualTo("/v2/get_userinfo_qrcode")
        assertThat(check.requestUrl?.queryParameter("qrcode")).isEqualTo("qr-key")
        assertThat(check.requestUrl?.queryParameter("appid")).isEqualTo("3116")
    }

    @Test
    fun dailyVipEndpointsRequireAuthAndPreserveAlreadyDoneSemantics() = runTest {
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{}}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"error_code":131001}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"data":{}}"""))

        val claimed = dataSource().claimDailyVip("2026-08-12")
        val already = dataSource().claimDailyVip("2026-08-12")
        val upgraded = dataSource().upgradeDailyVip()

        assertThat(claimed.alreadyDone).isFalse()
        assertThat(claimed.canUpgrade).isTrue()
        assertThat(already.alreadyDone).isTrue()
        assertThat(already.canUpgrade).isTrue()
        assertThat(upgraded.alreadyDone).isFalse()
        val claim = gatewayServer.takeRequest()
        assertThat(claim.requestUrl?.encodedPath).isEqualTo("/youth/v1/recharge/receive_vip_listen_song")
        assertThat(claim.requestUrl?.queryParameter("source_id")).isEqualTo("90139")
        assertThat(claim.requestUrl?.queryParameter("receive_day")).isEqualTo("2026-08-12")
        assertThat(claim.getHeader("Cookie")).contains("token=existing-token")
        gatewayServer.takeRequest()
        val upgrade = gatewayServer.takeRequest()
        assertThat(upgrade.requestUrl?.queryParameter("kugouid")).isEqualTo("99")
        assertThat(upgrade.requestUrl?.queryParameter("ad_type")).isEqualTo("1")
        val anonymous = session.copy(token = null, userId = null, cookies = emptyMap())
        assertThat(runCatching { dataSource(anonymous).upgradeDailyVip() }.exceptionOrNull()).isInstanceOf(ApiAuthenticationRequiredException::class.java)
    }

    @Test
    fun dailyVipRiskCodeWithoutChallengeRemainsBlocked() = runTest {
        gatewayServer.enqueue(jsonResponse("""{"status":0,"error_code":20028}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":0,"error_code":"20028"}"""))

        val claim = runCatching { dataSource().claimDailyVip("2026-08-12") }.exceptionOrNull()
        val upgrade = runCatching { dataSource().upgradeDailyVip() }.exceptionOrNull()

        assertThat(claim).isInstanceOf(ApiRiskBlockedException::class.java)
        assertThat(upgrade).isInstanceOf(ApiRiskBlockedException::class.java)
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

    @Test
    fun nonNumericServiceRejectionRemainsTyped() {
        gatewayServer.enqueue(jsonResponse("""{"status":1,"error_code":"E_UPSTREAM","data":{"song_list":[]}}"""))

        val failure = assertThrows(ApiServiceException::class.java) {
            runTest { dataSource().dailyRecommendations() }
        }

        assertThat(failure.serviceCode).isEqualTo("E_UPSTREAM")
    }

    private fun dataSource(initialSession: ApiSession = session): TestNetworkDataSource {
        val store = MemoryStore(initialSession)
        val sessions = ApiSessionManager(Optional.of(store), ApiDeviceIdentityFactory())
        val riskDetector = ApiRiskChallengeDetector()
        val signer = ApiRequestSigner()
        val clock = Clock.fixed(Instant.ofEpochMilli(1_700_000_000_123), ZoneOffset.UTC)
        val client =
            OkHttpClient.Builder()
                .retryOnConnectionFailure(false)
                .addInterceptor(ApiDefaultsInterceptor(clock, sessions))
                .addInterceptor(ApiSigningInterceptor(signer))
                .addInterceptor(ApiResponseMetadataInterceptor(json))
                .build()
        val executor = ProtocolTransport(
            { client }, json, clock, signer,
            sessions, riskDetector, ApiOriginPolicy { true },
        )
        val origins =
            ApiEndpointOrigins(
                gateway = gatewayServer.origin(),
                mobileCode = mobileCodeServer.origin(),
                mobileLogin = mobileLoginServer.origin(),
                deviceRegistration = deviceRegistrationServer.origin(),
                riskVerification = riskVerificationServer.origin(),
                vip = vipServer.origin(),
                cloud = cloudServer.origin(),
                openApi = openApiServer.origin(),
                complexSearch = complexSearchServer.origin(),
                lyrics = lyricsServer.origin(),
                qrLogin = qrLoginServer.origin(),
            )
        val musicApi =
            Retrofit.Builder()
                .baseUrl(gatewayServer.url("/"))
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(MusicApi::class.java)
        val registration = DeviceRegistrationCoordinator(executor, json, crypto, sessions, origins, fixtureDeviceProfileProvider())
        val mobileAuth = MobileAuthProtocolClient(executor, registration, json, crypto, signer, origins)
        val cloudProtocol = CloudProtocolClient(executor, registration, json, crypto, signer, origins, riskDetector)
        val responses = ApiResponseVerifier(riskDetector, sessions)
        val calls = ApiCallExecutor(sessions)
        val home = RealHomeNetworkDataSource(musicApi, registration, signer, clock, responses, calls)
        val catalog = RealCatalogNetworkDataSource(musicApi, registration, signer, clock, responses, calls, origins)
        val ranking = RealRankingNetworkDataSource(musicApi, registration, responses, calls)
        val playlist = RealPlaylistNetworkDataSource(musicApi, registration, responses, calls)
        val search = RealSearchNetworkDataSource(musicApi, registration, responses, calls, origins)
        val lyrics = RealLyricsNetworkDataSource(musicApi, registration, responses, calls, origins)
        val video = RealVideoNetworkDataSource(musicApi, registration, signer, responses, calls)
        val recognition = RealRecognitionNetworkDataSource(musicApi, registration, clock, responses, calls)
        val auth = RealAuthNetworkDataSource(musicApi, registration, mobileAuth, origins, calls, responses)
        val cloud = RealCloudNetworkDataSource(musicApi, registration, cloudProtocol, signer, calls, responses)
        val playback = RealPlaybackNetworkDataSource(musicApi, registration, signer, calls, responses)
        val user = RealUserProfileNetworkDataSource(musicApi, registration, clock, crypto, origins, calls, responses)
        val library = RealLibraryNetworkDataSource(musicApi, registration, clock, calls, responses)
        val vip = RealVipNetworkDataSource(musicApi, registration, calls, responses)
        return TestNetworkDataSource(home, catalog, ranking, playlist, search, lyrics, video, recognition, auth, cloud, playback, user, library, vip, store) { store.clearCount }
    }

    private fun MockWebServer.origin(): String = url("/").toString().removeSuffix("/")

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

    private fun jsonResponse(body: String) =
        MockResponse().setResponseCode(200).addHeader("Content-Type", "application/json").setBody(body)

    private fun encryptedCloudResponse(body: String): MockResponse {
        val encrypted = crypto.encryptPlaylist(body)
        return MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/octet-stream")
            .setBody(Buffer().write(encrypted.ciphertext))
    }

    private class MemoryStore(initial: ApiSession?) : ApiSessionStore {
        private val state = MutableStateFlow(initial)
        var clearCount = 0
        override val session = state
        override suspend fun read() = state.value
        override suspend fun write(session: ApiSession) { state.value = session }
        override suspend fun clearAuthentication() { clearCount += 1; state.value = null }
    }
}

private class TestNetworkDataSource(
    home: RealHomeNetworkDataSource,
    catalog: RealCatalogNetworkDataSource,
    ranking: RealRankingNetworkDataSource,
    playlist: RealPlaylistNetworkDataSource,
    search: RealSearchNetworkDataSource,
    lyrics: RealLyricsNetworkDataSource,
    video: RealVideoNetworkDataSource,
    recognition: RealRecognitionNetworkDataSource,
    auth: RealAuthNetworkDataSource,
    cloud: RealCloudNetworkDataSource,
    playback: RealPlaybackNetworkDataSource,
    user: RealUserProfileNetworkDataSource,
    library: RealLibraryNetworkDataSource,
    vip: RealVipNetworkDataSource,
    val store: ApiSessionStore,
    private val clearCount: () -> Int,
) : HomeNetworkDataSource by home,
    CatalogNetworkDataSource by catalog,
    RankingNetworkDataSource by ranking,
    PlaylistNetworkDataSource by playlist,
    PlaybackNetworkDataSource by playback,
    AuthNetworkDataSource by auth,
    UserProfileNetworkDataSource by user,
    LibraryNetworkDataSource by library,
    CloudNetworkDataSource by cloud,
    SearchNetworkDataSource by search,
    LyricsNetworkDataSource by lyrics,
    VideoNetworkDataSource by video,
    RecognitionNetworkDataSource by recognition,
    VipNetworkDataSource by vip {
    val authenticationClearCount: Int get() = clearCount()
}

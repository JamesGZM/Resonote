package com.resonote.core.network.retrofit

import com.google.common.truth.Truth.assertThat
import com.resonote.core.network.ApiHttpException
import com.resonote.core.network.ApiNetworkException
import com.resonote.core.network.ApiPlaybackUnavailableException
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiRiskException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.api.MusicApi
import com.resonote.core.network.model.NetworkMobileCodeLoginResult
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
import com.resonote.core.network.risk.ApiRiskChallengeDetector
import com.resonote.core.network.session.ApiSession
import com.resonote.core.network.session.ApiSessionManager
import com.resonote.core.network.session.ApiSessionStore
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
    fun songSourceRejectsCleartextFallbackWhenNoHttpsUrlExists() {
        gatewayServer.enqueue(
            jsonResponse(
                """{"status":1,"url":["http://cdn.example/song.mp3?token=value"],"timeLength":321000,"extName":"mp3"}""",
            ),
        )

        val failure = assertThrows(ApiProtocolException::class.java) {
            runTest { dataSource().resolveSongSource("ABCDEF", "12", "34") }
        }

        assertThat(failure.reason).isEqualTo(ApiProtocolException.Reason.InsecureMediaUrl)
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
    fun songSourceClassifiesKnownVipServiceCodeWithoutHidingOtherServiceFailures() {
        gatewayServer.enqueue(jsonResponse("""{"status":0,"error_code":35104,"url":[]}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":0,"error_code":35105,"url":[]}"""))

        val vip = assertThrows(ApiPlaybackUnavailableException::class.java) {
            runTest { dataSource().resolveSongSource("ABCDEF") }
        }
        assertThat(vip.reason).isEqualTo(ApiPlaybackUnavailableException.Reason.Vip)

        val other = assertThrows(ApiServiceException::class.java) {
            runTest { dataSource().resolveSongSource("ABCDEF") }
        }
        assertThat(other.serviceCode).isEqualTo("35105")
    }

    @Test
    fun songSourceClassifiesVipAndMalformedResponseSeparately() {
        gatewayServer.enqueue(jsonResponse("""{"status":1,"url":[],"backupUrl":[]}"""))
        gatewayServer.enqueue(jsonResponse("""{"url":[]}"""))
        gatewayServer.enqueue(jsonResponse("""{"status":1,"url":["not-a-url"]}"""))

        val vip = assertThrows(ApiPlaybackUnavailableException::class.java) {
            runTest { dataSource().resolveSongSource("ABCDEF") }
        }
        assertThat(vip.reason).isEqualTo(ApiPlaybackUnavailableException.Reason.Vip)
        assertThrows(ApiProtocolException::class.java) {
            runTest { dataSource().resolveSongSource("ABCDEF") }
        }
        assertThrows(ApiProtocolException::class.java) {
            runTest { dataSource().resolveSongSource("ABCDEF") }
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
                """{"status":1,"data":{"lists":[{"FileHash":"SEARCH_HASH","OriSongName":"Search Song","SingerName":"Search Artist","Image":"https://example.com/cover.jpg","Duration":245,"HQFileHash":"HQ_HASH","SQFileHash":"SQ_HASH"}],"total":1}}""",
            ),
        )

        val page = dataSource().searchSongs("fixture", page = 1, pageSize = 1)

        assertThat(page.total).isEqualTo(1)
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

    @Test
    fun nonNumericServiceRejectionRemainsTyped() {
        gatewayServer.enqueue(jsonResponse("""{"status":1,"error_code":"E_UPSTREAM","data":{"song_list":[]}}"""))

        val failure = assertThrows(ApiServiceException::class.java) {
            runTest { dataSource().dailyRecommendations() }
        }

        assertThat(failure.serviceCode).isEqualTo("E_UPSTREAM")
    }

    private fun dataSource(initialSession: ApiSession = session): RealApiNetworkDataSource {
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
        return RealApiNetworkDataSource(
            musicApi,
            registration,
            mobileAuth,
            signer,
            clock,
            riskDetector,
        )
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

    private class MemoryStore(initial: ApiSession?) : ApiSessionStore {
        private val state = MutableStateFlow(initial)
        override val session = state
        override suspend fun read() = state.value
        override suspend fun write(session: ApiSession) { state.value = session }
        override suspend fun clearAuthentication() { state.value = null }
    }
}

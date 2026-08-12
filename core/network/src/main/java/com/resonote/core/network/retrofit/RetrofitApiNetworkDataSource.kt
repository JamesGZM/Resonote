package com.resonote.core.network.retrofit

import com.resonote.core.network.ApiNetworkDataSource
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiPlaybackUnavailableException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.model.NetworkAccountOption
import com.resonote.core.network.model.NetworkHomePlaylist
import com.resonote.core.network.model.NetworkHomeSong
import com.resonote.core.network.model.NetworkMobileCodeLoginResult
import com.resonote.core.network.model.NetworkRecommendationMode
import com.resonote.core.network.model.NetworkSearchPage
import com.resonote.core.network.model.NetworkSong
import com.resonote.core.network.model.NetworkSongSource
import com.resonote.core.network.protocol.ApiCallExecutor
import com.resonote.core.network.protocol.ApiCleartextPolicy
import com.resonote.core.network.protocol.ApiEndpointSpec
import com.resonote.core.network.protocol.ApiEndpointOrigins
import com.resonote.core.network.protocol.ApiExchange
import com.resonote.core.network.protocol.ApiHttpMethod
import com.resonote.core.network.protocol.ApiProtocolConfig
import com.resonote.core.network.protocol.ApiProtocolCrypto
import com.resonote.core.network.protocol.ApiRequestSigner
import com.resonote.core.network.protocol.ApiResponseFormat
import com.resonote.core.network.protocol.ApiRiskPolicy
import com.resonote.core.network.protocol.ApiSessionMode
import com.resonote.core.network.protocol.ApiSignatureMode
import com.resonote.core.network.protocol.randomProtocolString
import com.resonote.core.network.session.ApiSession
import com.resonote.core.network.session.ApiSessionManager
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Singleton
internal class RealApiNetworkDataSource @Inject constructor(
    private val executor: ApiCallExecutor,
    private val json: Json,
    private val crypto: ApiProtocolCrypto,
    private val signer: ApiRequestSigner,
    private val sessions: ApiSessionManager,
    private val origins: ApiEndpointOrigins,
) : ApiNetworkDataSource {
    private val registrationMutex = Mutex()

    override suspend fun dailyRecommendations(): List<NetworkHomeSong> {
        ensureRegisteredSession()
        return executor.execute { _, _ ->
            ApiExchange(
                spec =
                    ApiEndpointSpec(
                        id = "API-DISCOVER-003",
                        origin = origins.gateway,
                        path = "/everyday_song_recommend",
                        method = ApiHttpMethod.Post,
                        query = mapOf("platform" to "ios"),
                        headers = mapOf("x-router" to "everydayrec.service.kugou.com"),
                    ),
                decode = { response -> decodeSongList(response, "song_list") },
            )
        }
    }

    override suspend fun recommendedPlaylists(page: Int, pageSize: Int): List<NetworkHomePlaylist> {
        validatePage(page, pageSize)
        ensureRegisteredSession()
        return executor.execute { session, nowMillis ->
            val clientTime = (nowMillis / 1_000).toString()
            val body =
                buildJsonObject {
                    put("appid", ApiProtocolConfig.APP_ID.toInt())
                    put("mid", session.mid)
                    put("clientver", ApiProtocolConfig.CLIENT_VERSION.toInt())
                    put("platform", "android")
                    put("clienttime", clientTime)
                    put("userid", session.userId?.toLongOrNull() ?: 0)
                    put("module_id", 1)
                    put("page", page)
                    put("pagesize", pageSize)
                    put("key", signer.signParamsKey(clientTime))
                    put(
                        "special_recommend",
                        buildJsonObject {
                            put("withtag", 1)
                            put("withsong", 0)
                            put("sort", 1)
                            put("ugc", 1)
                            put("is_selected", 0)
                            put("withrecommend", 1)
                            put("area_code", 1)
                            put("categoryid", 0)
                        },
                    )
                    put("req_multi", 1)
                    put("retrun_min", 5)
                    put("return_special_falg", 1)
                }.toString().encodeToByteArray()
            ApiExchange(
                spec =
                    ApiEndpointSpec(
                        id = "API-DISCOVER-012",
                        origin = origins.gateway,
                        path = "/v2/special_recommend",
                        method = ApiHttpMethod.Post,
                        headers = mapOf("x-router" to "specialrec.service.kugou.com"),
                        body = body,
                    ),
                decode = ::decodePlaylists,
            )
        }
    }

    override suspend fun newSongs(page: Int, pageSize: Int): List<NetworkHomeSong> {
        validatePage(page, pageSize)
        ensureRegisteredSession()
        return executor.execute { session, _ ->
            val body =
                buildJsonObject {
                    put("rank_id", 21608)
                    put("userid", session.userId?.toLongOrNull() ?: 0)
                    put("page", page)
                    put("pagesize", pageSize)
                    put("tags", buildJsonArray {})
                }.toString().encodeToByteArray()
            ApiExchange(
                spec =
                    ApiEndpointSpec(
                        id = "API-DISCOVER-013",
                        origin = origins.gateway,
                        path = "/musicadservice/container/v1/newsong_publish",
                        method = ApiHttpMethod.Post,
                        body = body,
                    ),
                decode = { response -> decodeSongList(response) },
            )
        }
    }

    override suspend fun radioRecommendations(mode: NetworkRecommendationMode): List<NetworkHomeSong> {
        ensureRegisteredSession()
        return executor.execute { session, nowMillis ->
            val body =
                buildJsonObject {
                    put("appid", ApiProtocolConfig.APP_ID.toInt())
                    put("clientver", ApiProtocolConfig.CLIENT_VERSION.toInt())
                    put("platform", "android")
                    put("clienttime", nowMillis)
                    put("userid", session.userId?.toLongOrNull() ?: 0)
                    put("key", signer.signParamsKey(nowMillis.toString()))
                    put("fakem", TOP_CARD_FAKEM)
                    put("area_code", 1)
                    put("mid", session.mid)
                    put("uuid", "-")
                    put("client_playlist", buildJsonArray {})
                    put("u_info", TOP_CARD_USER_INFO)
                }.toString().encodeToByteArray()
            ApiExchange(
                spec =
                    ApiEndpointSpec(
                        id = "API-DISCOVER-009",
                        origin = origins.gateway,
                        path = "/singlecardrec.service/v1/single_card_recommend",
                        method = ApiHttpMethod.Post,
                        query =
                            mapOf(
                                "card_id" to mode.cardId.toString(),
                                "fakem" to TOP_CARD_FAKEM,
                                "area_code" to "1",
                                "platform" to "ios",
                            ),
                        body = body,
                    ),
                decode = { response -> decodeSongList(response, "song_list") },
            )
        }
    }

    override suspend fun resolveSongSource(hash: String, albumId: String?, albumAudioId: String?): NetworkSongSource {
        require(hash.isNotBlank()) { "hash must not be blank" }
        ensureRegisteredSession()
        return executor.execute { current, _ ->
            val normalizedHash = hash.trim().lowercase()
            ApiExchange(
                spec =
                    ApiEndpointSpec(
                        id = "API-SONG-011",
                        origin = origins.gateway,
                        path = "/v5/url",
                        method = ApiHttpMethod.Get,
                        query =
                            linkedMapOf(
                                "album_id" to (albumId?.toLongOrNull() ?: 0).toString(),
                                "area_code" to "1",
                                "hash" to normalizedHash,
                                "ssa_flag" to "is_fromtrack",
                                "version" to "11430",
                                "page_id" to "967177915",
                                "quality" to "128",
                                "album_audio_id" to (albumAudioId?.toLongOrNull() ?: 0).toString(),
                                "behavior" to "play",
                                "pid" to "411",
                                "cmd" to "26",
                                "pidversion" to "3001",
                                "IsFreePart" to "1",
                                "ppage_id" to "356753938,823673182,967485191",
                                "cdnBackup" to "1",
                                "module" to "",
                                "clientver" to "11430",
                                "key" to signer.signSongKey(normalizedHash, current.mid, current.userId),
                            ),
                        headers = mapOf("x-router" to "trackercdn.kugou.com"),
                    ),
                decode = ::decodeSongSource,
            )
        }
    }

    override suspend fun searchSongs(keywords: String, page: Int, pageSize: Int): NetworkSearchPage {
        validateSearchRequest(keywords, page, pageSize)
        ensureRegisteredSession()
        return executor.execute { _, _ ->
            ApiExchange(
                spec =
                    ApiEndpointSpec(
                        id = "API-SEARCH-001",
                        origin = origins.gateway,
                        path = "/v3/search/song",
                        method = ApiHttpMethod.Get,
                        query =
                            linkedMapOf(
                                "albumhide" to "0",
                                "iscorrection" to "1",
                                "keyword" to keywords.trim(),
                                "nocollect" to "0",
                                "page" to page.toString(),
                                "pagesize" to pageSize.toString(),
                                "platform" to "AndroidFilter",
                            ),
                        headers = mapOf("x-router" to "complexsearch.kugou.com"),
                    ),
                decode = ::decodeSearchPage,
            )
        }
    }

    override suspend fun sendMobileCode(mobile: String) {
        require(MOBILE_PATTERN.matches(mobile)) { "mobile must be an 11-digit mainland number" }
        ensureRegisteredSession()
        executor.execute { _, _ ->
            val body = buildJsonObject { put("businessid", 5); put("mobile", mobile); put("plat", 3) }.toString().encodeToByteArray()
            ApiExchange(
                spec =
                    ApiEndpointSpec(
                        id = "API-LOGIN-001",
                        origin = origins.mobileCode,
                        path = "/v7/send_mobile_code",
                        method = ApiHttpMethod.Post,
                        body = body,
                        sessionMode = ApiSessionMode.DeviceOnly,
                        cleartextPolicy = ApiCleartextPolicy.LoginMobileCode,
                    ),
                decode = { response ->
                    val root = response.requireBody()
                    if (root.text("status") != "1") throw ApiServiceException(root.text("error_code") ?: root.text("status"))
                    Unit
                },
            )
        }
    }

    override suspend fun loginWithMobileCode(
        mobile: String,
        code: String,
        selectedUserId: String?,
    ): NetworkMobileCodeLoginResult {
        require(MOBILE_PATTERN.matches(mobile)) { "mobile must be an 11-digit mainland number" }
        require(code.isNotBlank()) { "code must not be blank" }
        selectedUserId?.let { require(it.isNotBlank() && it != "0") { "selectedUserId must be valid" } }
        ensureRegisteredSession()
        return executor.execute { session, nowMillis -> mobileLoginExchange(session, nowMillis, mobile, code, selectedUserId) }
    }

    private fun mobileLoginExchange(
        session: ApiSession,
        nowMillis: Long,
        mobile: String,
        code: String,
        selectedUserId: String?,
    ): ApiExchange<NetworkMobileCodeLoginResult> {
        val encrypted = crypto.encryptTemporary(buildJsonObject { put("mobile", mobile); put("code", code) }.toString())
        val t1 = crypto.fixedAesHex("|$nowMillis", LITE_T1_KEY, LITE_T1_IV)
        val t2 =
            crypto.fixedAesHex(
                "${session.guid}|$LOGIN_DEVICE_MARKER|${ApiProtocolConfig.MAC}|${session.dev}|$nowMillis",
                LITE_T2_KEY,
                LITE_T2_IV,
            )
        val envelope = buildJsonObject { put("clienttime_ms", nowMillis); put("key", encrypted.temporaryKey) }.toString()
        val body =
            buildJsonObject {
                put("plat", 1)
                put("support_multi", 1)
                put("t1", t1)
                put("t2", t2)
                put("clienttime_ms", nowMillis)
                put("mobile", "${mobile.take(2)}*****${mobile.last()}")
                put("key", signer.signParamsKey(nowMillis.toString()))
                put("pk", crypto.rawLiteRsa(envelope).uppercase())
                put("params", encrypted.ciphertextHex)
                selectedUserId?.let { put("userid", it) }
                put("dfid", session.dfid ?: randomProtocolString(24))
                put("dev", session.dev)
                put("gitversion", "5f0b7c4")
            }.toString().encodeToByteArray()
        return ApiExchange(
            spec =
                ApiEndpointSpec(
                    id = "API-LOGIN-004",
                    origin = origins.mobileLogin,
                    path = "/v7/login_by_verifycode",
                    method = ApiHttpMethod.Post,
                    headers = mapOf("support-calm" to "1", "User-Agent" to ApiProtocolConfig.LOGIN_USER_AGENT),
                    body = body,
                ),
            decode = { response -> decodeMobileLogin(response, session, encrypted.temporaryKey) },
        )
    }

    private suspend fun ensureRegisteredSession(): ApiSession = registrationMutex.withLock {
        val current = sessions.current()
        if (!current.dfid.isNullOrBlank()) return@withLock current
        val registered = executor.execute { session, _ -> registrationExchange(session) }
        sessions.write(registered)
        registered
    }

    private fun registrationExchange(session: ApiSession): ApiExchange<ApiSession> {
        val deviceBody =
            buildJsonObject {
                put("availableRamSize", 4_000_000_000)
                put("availableRomSize", 32_000_000_000)
                put("availableSDSize", 0)
                put("basebandVer", "")
                put("batteryLevel", 100)
                put("batteryStatus", 3)
                put("brand", "Resonote")
                put("buildSerial", "unknown")
                put("device", "android")
                put("imei", session.guid)
                put("imsi", "")
                put("manufacturer", "Resonote")
                put("uuid", session.guid)
                SENSOR_FIELDS.forEach { (name, value) -> if (name.endsWith("Value")) put(name, value) else put(name, false) }
            }.toString()
        val encrypted = crypto.encryptPlaylist(deviceBody)
        val p = crypto.pkcs1LiteRsa(buildJsonObject { put("aes", encrypted.key); put("uid", 0); put("token", "") }.toString())
        val body = Base64.getEncoder().encode(encrypted.ciphertext)
        return ApiExchange(
            spec =
                ApiEndpointSpec(
                    id = "API-DEVICE-001",
                    origin = origins.deviceRegistration,
                    path = "/risk/v2/r_register_dev",
                    method = ApiHttpMethod.Post,
                    query = mapOf("part" to "1", "platid" to "1", "p" to p),
                    body = body,
                    responseFormat = ApiResponseFormat.Bytes,
                ),
            decode = { response ->
                val decrypted = crypto.decryptPlaylist(response.bytes, encrypted.key)
                val root = json.parseToJsonElement(decrypted) as? JsonObject
                    ?: throw ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)
                if (root.text("status") != "1") throw ApiServiceException(root.text("error_code") ?: root.text("status"))
                val data = root["data"] as? JsonObject ?: throw missingField()
                val dfid = data.text("dfid")?.takeIf(String::isNotBlank) ?: throw missingField()
                session.copy(dfid = dfid, cookies = session.cookies + ("dfid" to dfid))
            },
        )
    }

    private fun decodeMobileLogin(
        response: ApiRawResponse,
        baseSession: ApiSession,
        temporaryKey: String,
    ): NetworkMobileCodeLoginResult {
        val root = response.requireBody()
        if (root.text("status") != "1") {
            val accounts = readAccounts(root)
            if (accounts.isNotEmpty()) return NetworkMobileCodeLoginResult.MultipleAccounts(accounts)
            throw ApiServiceException(root.text("error_code") ?: root.text("status"))
        }
        val data = root["data"] as? JsonObject ?: throw missingField()
        val secure = data.text("secu_params")?.takeIf(String::isNotBlank) ?: throw missingField()
        val plaintext =
            runCatching { crypto.decryptTemporary(secure, temporaryKey) }
                .getOrElse { throw ApiProtocolException(ApiProtocolException.Reason.MalformedResponse) }
        val decrypted = runCatching { json.parseToJsonElement(plaintext) }.getOrElse { JsonPrimitive(plaintext) }
        val secret = decrypted as? JsonObject
        val token = secret?.text("token") ?: (decrypted as? JsonPrimitive)?.contentOrNull ?: data.text("token")
        val userId = secret?.text("userid") ?: data.text("userid")
        if (token.isNullOrBlank() || userId.isNullOrBlank() || userId == "0") throw missingField()
        val responseCookies = response.setCookies()
        val authCookies =
            mapOf(
                "token" to token,
                "userid" to userId,
                "t1" to (secret?.text("t1") ?: data.text("t1")).orEmpty(),
                "vip_type" to (secret?.text("vip_type") ?: data.text("vip_type") ?: "0"),
                "vip_token" to (secret?.text("vip_token") ?: data.text("vip_token")).orEmpty(),
            )
        return NetworkMobileCodeLoginResult.Authenticated(
            baseSession.copy(token = token, userId = userId, cookies = baseSession.cookies + responseCookies + authCookies),
        )
    }

    private fun readAccounts(root: JsonObject): List<NetworkAccountOption> =
        runCatching {
            (root["data"] as? JsonObject)?.get("info_list")?.jsonArray.orEmpty().mapNotNull { element ->
                val account = element as? JsonObject ?: return@mapNotNull null
                val userId = account.text("userid")?.takeIf { it.isNotBlank() && it != "0" } ?: return@mapNotNull null
                NetworkAccountOption(
                    userId = userId,
                    nickname = account.text("nickname").orEmpty(),
                    avatarUrl = account.text("pic")?.takeIf(String::isNotBlank),
                    grade = account.text("p_grade")?.takeIf(String::isNotBlank),
                )
            }
        }.getOrDefault(emptyList())

    private fun decodeSearchPage(response: ApiRawResponse): NetworkSearchPage {
        val body = response.requireBody()
        val status = body["status"].textValue()?.toIntOrNull()
        val errorCode = body["error_code"].textValue()
        if (status == 0 || errorCode?.toIntOrNull()?.let { it != 0 } == true) throw ApiServiceException(errorCode)
        val data = body["data"] as? JsonObject ?: throw missingField()
        val rawItems = runCatching { data["lists"]?.jsonArray }.getOrNull() ?: throw missingField()
        val items = rawItems.mapNotNull { it.toNetworkSongOrNull() }
        if (rawItems.isNotEmpty() && items.isEmpty()) throw missingField()
        val total = data["total"].longValue()?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt() ?: items.size
        return NetworkSearchPage(items, total)
    }

    private fun decodeSongList(response: ApiRawResponse, nestedName: String? = null): List<NetworkHomeSong> {
        val root = response.requireSuccessfulBody()
        val container = if (nestedName == null) root else root["data"] as? JsonObject ?: throw missingField()
        val rawItems =
            when {
                nestedName != null -> container[nestedName] as? JsonArray ?: throw missingField()
                root["data"] is JsonArray -> root["data"] as JsonArray
                else -> throw missingField()
            }
        val items = rawItems.mapNotNull { it.toNetworkHomeSongOrNull() }
        if (rawItems.isNotEmpty() && items.isEmpty()) throw missingField()
        return items
    }

    private fun decodePlaylists(response: ApiRawResponse): List<NetworkHomePlaylist> {
        val root = response.requireSuccessfulBody()
        val data = root["data"] as? JsonObject ?: throw missingField()
        val rawItems = data["special_list"] as? JsonArray ?: throw missingField()
        val items = rawItems.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val id = item.text("global_collection_id", "specialid")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val title = item.text("specialname")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            NetworkHomePlaylist(
                id = id,
                title = title,
                coverUrl = item.text("flexible_cover", "cover", "imgurl")?.takeIf(String::isNotBlank),
                playCount = item["play_count"].longValue()?.coerceAtLeast(0),
            )
        }
        if (rawItems.isNotEmpty() && items.isEmpty()) throw missingField()
        return items
    }

    private fun decodeSongSource(response: ApiRawResponse): NetworkSongSource {
        val root = response.requireBody()
        val status = root["status"].longValue() ?: throw missingField()
        if (status == 0L) throw ApiServiceException(root.text("error_code"))
        val urls =
            sequenceOf(root["url"], root["backupUrl"], root["backup_url"])
                .flatMap(::stringValues)
                .mapNotNull { it.toHttpUrlOrNull() }
                .filter { it.isHttps }
                .map { it.toString() }
                .toList()
        if (urls.isEmpty()) {
            val reason =
                if (status == 3L) {
                    ApiPlaybackUnavailableException.Reason.Copyright
                } else {
                    ApiPlaybackUnavailableException.Reason.Vip
                }
            throw ApiPlaybackUnavailableException(reason)
        }
        return NetworkSongSource(
            uri = urls.first(),
            durationMillis = normalizeDurationMillis(root["timeLength"].longValue()),
            extension = root.text("extName")?.takeIf(String::isNotBlank),
        )
    }

    private fun JsonElement.toNetworkHomeSongOrNull(): NetworkHomeSong? = runCatching {
        val item = this as? JsonObject ?: return null
        val deprecated = item["deprecated"] as? JsonObject
        val transParam = item["trans_param"] as? JsonObject
        val hash = item.text("hash", "FileHash") ?: deprecated?.text("hash") ?: return null
        val filename = item.text("filename", "FileName").orEmpty()
        val (filenameArtist, filenameTitle) = splitArtistTitle(filename)
        val title = item.text("ori_audio_name", "songname", "OriSongName", "SongName") ?: filenameTitle
        if (hash.isBlank() || title.isBlank()) return null
        NetworkHomeSong(
            hash = hash,
            title = title,
            artist = item.text("author_name", "SingerName") ?: filenameArtist,
            coverUrl =
                transParam?.text("union_cover")
                    ?: item.text("sizable_cover", "album_sizable_cover", "Image"),
            albumId = item.text("album_id"),
            albumAudioId = item.text("album_audio_id", "audio_id", "mixsongid"),
            durationMillis =
                normalizeDurationMillis(
                    item["time_length"].longValue()
                        ?: item["duration"].longValue()
                        ?: deprecated?.get("duration").longValue()
                        ?: item["timelength"].longValue()
                        ?: item["timelen"].longValue(),
                ),
            highQualityHash = item.text("HQFileHash", "hash_320"),
            losslessHash = item.text("SQFileHash", "sqhash", "hash_flac"),
            vip = (item["privilege"].longValue() ?: deprecated?.get("pay_type").longValue() ?: 0) >= 10,
        )
    }.getOrNull()

    private fun ApiRawResponse.requireSuccessfulBody(): JsonObject {
        val root = requireBody()
        val status = root["status"].longValue()
        val errorCode = root["error_code"].longValue()
        if (status == 0L || errorCode?.let { it != 0L } == true) {
            throw ApiServiceException(root.text("error_code") ?: root.text("status"))
        }
        return root
    }

    private fun stringValues(element: JsonElement?): Sequence<String> =
        when (element) {
            is JsonPrimitive -> listOfNotNull(element.contentOrNull?.trim()?.takeIf(String::isNotEmpty)).asSequence()
            is JsonArray -> element.asSequence().mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotEmpty) }
            else -> emptySequence()
        }

    private fun normalizeDurationMillis(value: Long?): Long =
        value?.takeIf { it > 0 }?.let { if (it < 10_000) it * 1_000 else it } ?: 0

    private fun splitArtistTitle(filename: String): Pair<String, String> {
        val separator = filename.indexOf(" - ")
        return if (separator > 0) filename.substring(0, separator) to filename.substring(separator + 3) else "" to filename
    }

    private fun validatePage(page: Int, pageSize: Int) {
        require(page > 0) { "page must be positive" }
        require(pageSize in 1..100) { "pageSize must be between 1 and 100" }
    }

    private fun JsonElement.toNetworkSongOrNull(): NetworkSong? = runCatching {
        val item = this as? JsonObject ?: return null
        val hash = item.text("FileHash")?.takeIf(String::isNotBlank) ?: return null
        val title = item.text("OriSongName", "SongName", "FileName")?.takeIf(String::isNotBlank) ?: return null
        NetworkSong(
            hash = hash,
            title = title,
            singerName = item.text("SingerName").orEmpty(),
            imageUrl = item.text("Image")?.takeIf(String::isNotBlank),
            durationSeconds = item["Duration"].longValue()?.coerceAtLeast(0) ?: 0,
            highQualityHash = item.text("HQFileHash")?.takeIf(String::isNotBlank),
            losslessHash = item.text("SQFileHash")?.takeIf(String::isNotBlank),
        )
    }.getOrNull()

    private fun validateSearchRequest(keywords: String, page: Int, pageSize: Int) {
        require(keywords.isNotBlank()) { "keywords must not be blank" }
        require(page > 0) { "page must be positive" }
        require(pageSize in 1..100) { "pageSize must be between 1 and 100" }
    }

    private fun ApiRawResponse.requireBody(): JsonObject = body ?: throw ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)

    private fun ApiRawResponse.setCookies(): Map<String, String> =
        headers.entries.firstOrNull { it.key.equals("set-cookie", true) }?.value.orEmpty().mapNotNull { raw ->
            val pair = raw.substringBefore(';')
            val separator = pair.indexOf('=')
            if (separator <= 0) null else pair.substring(0, separator).trim() to pair.substring(separator + 1).trim()
        }.toMap()

    private fun JsonObject.text(vararg names: String): String? =
        names.firstNotNullOfOrNull { name -> (get(name) as? JsonPrimitive)?.contentOrNull }

    private fun JsonElement?.textValue(): String? = (this as? JsonPrimitive)?.contentOrNull
    private fun JsonElement?.longValue(): Long? = (this as? JsonPrimitive)?.longOrNull
    private fun missingField() = ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)

    private companion object {
        const val TOP_CARD_FAKEM = "60f7ebf1f812edbac3c63a7310001701760f"
        const val TOP_CARD_USER_INFO = "a0c35cd40af564444b5584c2754dedec"
        val MOBILE_PATTERN = Regex("^1\\d{10}$")
        const val LOGIN_DEVICE_MARKER = "0f607264fc6318a92b9e13c65db7cd3c"
        const val LITE_T1_KEY = "5e4ef500e9597fe004bd09a46d8add98"
        const val LITE_T1_IV = "04bd09a46d8add98"
        const val LITE_T2_KEY = "fd14b35e3f81af3817a20ae7adae7020"
        const val LITE_T2_IV = "17a20ae7adae7020"
        val SENSOR_FIELDS =
            listOf("accelerometer", "gravity", "gyroscope", "light", "magnetic", "orientation", "pressure", "step_counter", "temperature")
                .flatMap { listOf(it to "", "${it}Value" to "") }
    }
}

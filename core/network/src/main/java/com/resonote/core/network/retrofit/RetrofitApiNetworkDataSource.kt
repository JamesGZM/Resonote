package com.resonote.core.network.retrofit

import com.resonote.core.network.ApiNetworkDataSource
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.model.NetworkAccountOption
import com.resonote.core.network.model.NetworkMobileCodeLoginResult
import com.resonote.core.network.model.NetworkSearchPage
import com.resonote.core.network.model.NetworkSong
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

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

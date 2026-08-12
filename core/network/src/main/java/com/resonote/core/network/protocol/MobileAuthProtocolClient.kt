package com.resonote.core.network.protocol

import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.model.NetworkAccountOption
import com.resonote.core.network.model.NetworkMobileCodeLoginResult
import com.resonote.core.network.retrofit.ApiRawResponse
import com.resonote.core.network.session.ApiSession
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put

@Singleton
internal class MobileAuthProtocolClient @Inject constructor(
    private val transport: ProtocolTransport,
    private val registration: DeviceRegistrationCoordinator,
    private val json: Json,
    private val crypto: ApiProtocolCrypto,
    private val signer: ApiRequestSigner,
    private val origins: ApiEndpointOrigins,
) {
    suspend fun sendMobileCode(mobile: String) {
        require(MOBILE_PATTERN.matches(mobile)) { "mobile must be an 11-digit mainland number" }
        registration.ensureRegisteredSession()
        transport.execute { _, _ ->
            val body = buildJsonObject { put("businessid", 5); put("mobile", mobile); put("plat", 3) }
                .toString()
                .encodeToByteArray()
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
                    if (root.text("status") != "1") {
                        throw ApiServiceException(root.text("error_code") ?: root.text("status"))
                    }
                },
            )
        }
    }

    suspend fun loginWithMobileCode(
        mobile: String,
        code: String,
        selectedUserId: String?,
    ): NetworkMobileCodeLoginResult {
        require(MOBILE_PATTERN.matches(mobile)) { "mobile must be an 11-digit mainland number" }
        require(code.isNotBlank()) { "code must not be blank" }
        selectedUserId?.let { require(it.isNotBlank() && it != "0") { "selectedUserId must be valid" } }
        registration.ensureRegisteredSession()
        return transport.execute { session, nowMillis ->
            loginExchange(session, nowMillis, mobile, code, selectedUserId)
        }
    }

    private fun loginExchange(
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
            decode = { response -> decodeLogin(response, session, encrypted.temporaryKey) },
        )
    }

    private fun decodeLogin(
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
        val plaintext = runCatching { crypto.decryptTemporary(secure, temporaryKey) }
            .getOrElse { throw ApiProtocolException(ApiProtocolException.Reason.MalformedResponse) }
        val decrypted = runCatching { json.parseToJsonElement(plaintext) }.getOrElse { JsonPrimitive(plaintext) }
        val secret = decrypted as? JsonObject
        val token = secret?.text("token") ?: (decrypted as? JsonPrimitive)?.contentOrNull ?: data.text("token")
        val userId = secret?.text("userid") ?: data.text("userid")
        if (token.isNullOrBlank() || userId.isNullOrBlank() || userId == "0") throw missingField()
        val authCookies =
            mapOf(
                "token" to token,
                "userid" to userId,
                "t1" to (secret?.text("t1") ?: data.text("t1")).orEmpty(),
                "vip_type" to (secret?.text("vip_type") ?: data.text("vip_type") ?: "0"),
                "vip_token" to (secret?.text("vip_token") ?: data.text("vip_token")).orEmpty(),
            )
        return NetworkMobileCodeLoginResult.Authenticated(
            baseSession.copy(
                token = token,
                userId = userId,
                cookies = baseSession.cookies + response.responseCookies() + authCookies,
            ),
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

    private fun ApiRawResponse.requireBody(): JsonObject =
        body ?: throw ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)

    private fun JsonObject.text(name: String): String? = (get(name) as? JsonPrimitive)?.contentOrNull

    private fun missingField() = ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)

    private companion object {
        val MOBILE_PATTERN = Regex("^1\\d{10}$")
        const val LOGIN_DEVICE_MARKER = "0f607264fc6318a92b9e13c65db7cd3c"
        const val LITE_T1_KEY = "5e4ef500e9597fe004bd09a46d8add98"
        const val LITE_T1_IV = "04bd09a46d8add98"
        const val LITE_T2_KEY = "fd14b35e3f81af3817a20ae7adae7020"
        const val LITE_T2_IV = "17a20ae7adae7020"
    }
}

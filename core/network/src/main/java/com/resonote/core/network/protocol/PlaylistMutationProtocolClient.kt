package com.resonote.core.network.protocol

import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiRiskException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.risk.ApiRiskChallengeDetector
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PlaylistMutationProtocolClient @Inject constructor(
    private val transport: ProtocolTransport,
    private val registration: DeviceRegistrationCoordinator,
    private val json: Json,
    private val crypto: ApiProtocolCrypto,
    private val signer: ApiRequestSigner,
    private val origins: ApiEndpointOrigins,
    private val riskDetector: ApiRiskChallengeDetector,
) {
    suspend fun deletePlaylist(listId: String) {
        val normalizedListId = listId.toLongOrNull()?.takeIf { it > 0 }
            ?: throw IllegalArgumentException("listId must be a positive number")
        registration.requireAuthenticatedSession()
        transport.execute { session, nowMillis ->
            val encrypted = crypto.encryptPlaylist(
                buildJsonObject {
                    put("listid", normalizedListId)
                    put("total_ver", 0)
                    put("type", 1)
                }.toString(),
            )
            val p = crypto.pkcs1LiteRsa(
                buildJsonObject {
                    put("aes", encrypted.key)
                    put("uid", requireNotNull(session.userId))
                    put("token", requireNotNull(session.token))
                }.toString(),
            ).uppercase()
            val clientTime = nowMillis / 1_000
            ApiExchange(
                spec = ApiEndpointSpec(
                    origin = origins.gateway,
                    path = "/v2/delete_list",
                    method = ApiHttpMethod.Post,
                    query = linkedMapOf(
                        "clienttime" to clientTime.toString(),
                        "key" to signer.signParamsKey(clientTime.toString()),
                        "last_area" to "gztx",
                        "clientver" to ApiProtocolConfig.CLIENT_VERSION,
                        "appid" to ApiProtocolConfig.APP_ID,
                        "last_time" to clientTime.toString(),
                        "p" to p,
                    ),
                    headers = mapOf("x-router" to "cloudlist.service.kugou.com"),
                    body = encrypted.ciphertext,
                    contentType = "application/octet-stream",
                    signatureMode = ApiSignatureMode.Android,
                    includeDefaultParams = false,
                    responseFormat = ApiResponseFormat.Bytes,
                ),
                decode = { response ->
                    val plaintext = runCatching { crypto.decryptPlaylist(response.bytes, encrypted.key) }
                        .getOrElse { throw malformedResponse() }
                    val root = runCatching { json.parseToJsonElement(plaintext) as? JsonObject }
                        .getOrNull() ?: throw malformedResponse()
                    riskDetector.detect(response.copy(body = root))?.let {
                        throw ApiRiskException(it, ApiRiskException.Reason.VerificationUnavailable)
                    }
                    if (root.text("status") != "1") {
                        throw ApiServiceException(root.text("error_code") ?: root.text("status"))
                    }
                },
            )
        }
    }

    private fun malformedResponse() = ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)

    private fun JsonObject.text(name: String): String? = (get(name) as? JsonPrimitive)?.contentOrNull
}

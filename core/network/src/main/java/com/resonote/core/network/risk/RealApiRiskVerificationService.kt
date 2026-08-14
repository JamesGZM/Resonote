package com.resonote.core.network.risk

import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.protocol.ProtocolTransport
import com.resonote.core.network.protocol.ApiEndpointSpec
import com.resonote.core.network.protocol.ApiEndpointOrigins
import com.resonote.core.network.protocol.ApiExchange
import com.resonote.core.network.protocol.ApiHttpMethod
import com.resonote.core.network.protocol.ApiProtocolCrypto
import com.resonote.core.network.protocol.ApiRiskPolicy
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

@Singleton
internal class RealApiRiskVerificationService @Inject constructor(
    private val transport: ProtocolTransport,
    private val crypto: ApiProtocolCrypto,
    private val origins: ApiEndpointOrigins,
    private val riskContextFactory: ApiRiskContextFactory,
) : ApiRiskVerificationService {
    override suspend fun methodFor(challenge: ApiRiskChallenge): ApiRiskMethod =
        transport.execute { session, _ ->
            val body =
                buildJsonObject {
                    put("eventid", challenge.eventId)
                    put("userid", session.userId?.toLongOrNull() ?: 0)
                    put("platid", 2)
                    put("rtype", 1)
                    put("wasm", 1)
                    put("i", "")
                    put("sid", "")
                    put("edt", "")
                }.toString().encodeToByteArray()
            ApiExchange(
                spec =
                    ApiEndpointSpec(
                        origin = origins.gateway,
                        path = "/verifyservice/v3/get_verify_info",
                        method = ApiHttpMethod.Post,
                        body = body,
                        riskPolicy = ApiRiskPolicy.Bypass,
                    ),
                decode = { response ->
                    val root = response.body ?: malformed()
                    requireSuccess(root)
                    val data = root["data"] as? JsonObject ?: malformed()
                    when (val type = data.text("v_type")?.toIntOrNull() ?: malformed()) {
                        32 -> ApiRiskMethod.Sms
                        23 -> ApiRiskMethod.Tencent(data.text("txappid")?.takeIf(String::isNotBlank) ?: malformed())
                        else -> ApiRiskMethod.Unsupported(type)
                    }
                },
            )
        }

    override suspend fun submit(challenge: ApiRiskChallenge, proof: ApiRiskProof) {
        transport.execute { session, _ ->
            val completeChallenge = riskContextFactory.complete(challenge, session)
            val temporary =
                when (proof) {
                    is ApiRiskProof.Sms -> crypto.encryptTemporary(buildJsonObject { put("code", proof.code) }.toString())
                    is ApiRiskProof.Tencent -> crypto.encryptTemporary("{}")
                }
            val verifyCode =
                when (proof) {
                    is ApiRiskProof.Sms -> proof.code
                    is ApiRiskProof.Tencent ->
                        "KGCodeTX|" + buildJsonObject {
                            put("ticket", proof.ticket)
                            put("randstr", proof.randomString)
                            put("txappid", proof.appId)
                        }
                }
            val type = if (proof is ApiRiskProof.Sms) 32 else 23
            val body =
                buildJsonObject {
                    put("eventid", completeChallenge.eventId)
                    put("userid", session.userId?.toLongOrNull() ?: 0)
                    put("platid", 2)
                    put("v_type", type)
                    put("wasm", 1)
                    put("i", "")
                    put("sid", completeChallenge.sid.orEmpty())
                    put("edt", completeChallenge.edt.orEmpty())
                    if (type == 23) put("verifycode", verifyCode) else put("code", verifyCode)
                    put("pk", crypto.rawLiteRsa(buildJsonObject { put("key", temporary.temporaryKey) }.toString()))
                    put("params", temporary.ciphertextHex)
                }.toString().encodeToByteArray()
            ApiExchange(
                spec =
                    ApiEndpointSpec(
                        origin = origins.riskVerification,
                        path = "/v4/verify_user_info",
                        method = ApiHttpMethod.Post,
                        query = mapOf("clientver" to "11510"),
                        body = body,
                        riskPolicy = ApiRiskPolicy.Bypass,
                    ),
                decode = { response ->
                    requireSuccess(response.body ?: malformed())
                    Unit
                },
            )
        }
    }

    private fun requireSuccess(root: JsonObject) {
        if (root.text("status") != "1") throw ApiServiceException(root.text("error_code") ?: root.text("status"))
    }

    private fun JsonObject.text(name: String): String? = (get(name) as? JsonPrimitive)?.contentOrNull

    private fun malformed(): Nothing = throw ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)
}

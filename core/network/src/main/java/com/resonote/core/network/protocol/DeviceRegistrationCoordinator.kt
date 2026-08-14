package com.resonote.core.network.protocol

import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiAuthenticationRequiredException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.session.ApiSession
import com.resonote.core.network.session.ApiAuthenticationGateReason
import com.resonote.core.network.session.ApiSessionManager
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

@Singleton
internal class DeviceRegistrationCoordinator @Inject constructor(
    private val transport: ProtocolTransport,
    private val json: Json,
    private val crypto: ApiProtocolCrypto,
    private val sessions: ApiSessionManager,
    private val origins: ApiEndpointOrigins,
    private val deviceProfiles: DeviceRegistrationProfileProvider,
) {
    private val mutex = Mutex()

    suspend fun ensureRegisteredSession(): ApiSession = mutex.withLock {
        val current = sessions.current()
        if (!current.dfid.isNullOrBlank()) return@withLock current
        val registered = transport.execute { session, _ -> registrationExchange(session) }
        sessions.write(registered)
        registered
    }

    suspend fun requireAuthenticatedSession(): ApiSession {
        ensureRegisteredSession()
        return sessions.authenticatedSessionOrReportRequired()
            ?: throw ApiAuthenticationRequiredException(ApiAuthenticationGateReason.LoginRequired)
    }

    private fun registrationExchange(session: ApiSession): ApiExchange<ApiSession> {
        val profile = deviceProfiles.current()
        val deviceBody =
            buildJsonObject {
                put("availableRamSize", profile.totalMemoryBytes)
                put("availableRomSize", profile.availableInternalStorageBytes)
                put("availableSDSize", profile.availableExternalStorageBytes)
                put("basebandVer", "")
                put("batteryLevel", 100)
                put("batteryStatus", 3)
                put("brand", profile.brand)
                put("buildSerial", profile.buildId)
                put("device", profile.device)
                put("imei", session.guid)
                put("imsi", "")
                put("manufacturer", profile.manufacturer)
                put("uuid", session.guid)
                SENSOR_FIELDS.forEach { (name, value) ->
                    if (name.endsWith("Value")) put(name, value) else put(name, false)
                }
            }.toString()
        val encrypted = crypto.encryptPlaylist(deviceBody)
        val keyEnvelope = buildJsonObject { put("aes", encrypted.key); put("uid", 0); put("token", "") }.toString()
        val body = Base64.getEncoder().encode(encrypted.ciphertext)
        return ApiExchange(
            spec =
                ApiEndpointSpec(
                    origin = origins.deviceRegistration,
                    path = "/risk/v2/r_register_dev",
                    method = ApiHttpMethod.Post,
                    query = mapOf("part" to "1", "platid" to "1", "p" to crypto.pkcs1LiteRsa(keyEnvelope)),
                    body = body,
                    responseFormat = ApiResponseFormat.Bytes,
                ),
            decode = { response ->
                val decrypted = crypto.decryptPlaylist(response.bytes, encrypted.key)
                val root = json.parseToJsonElement(decrypted) as? JsonObject
                    ?: throw ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)
                if (root.text("status") != "1") {
                    throw ApiServiceException(root.text("error_code") ?: root.text("status"))
                }
                val responseCookies = response.responseCookies()
                val dfid =
                    (root["data"] as? JsonObject)?.text("dfid")?.takeIf(String::isNotBlank)
                        ?: responseCookies["dfid"]?.takeIf(String::isNotBlank)
                        ?: throw missingField()
                session.copy(dfid = dfid, cookies = session.cookies + responseCookies + ("dfid" to dfid))
            },
        )
    }

    private fun JsonObject.text(name: String): String? = (get(name) as? JsonPrimitive)?.contentOrNull

    private fun missingField() = ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)

    private companion object {
        val SENSOR_FIELDS =
            listOf(
                "accelerometer",
                "gravity",
                "gyroscope",
                "light",
                "magnetic",
                "orientation",
                "pressure",
                "step_counter",
                "temperature",
            ).flatMap { listOf(it to "", "${it}Value" to "") }
    }
}

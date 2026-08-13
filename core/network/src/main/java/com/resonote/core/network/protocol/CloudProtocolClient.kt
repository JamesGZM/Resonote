package com.resonote.core.network.protocol

import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiRiskException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.model.NetworkCloudPage
import com.resonote.core.network.model.NetworkCloudStorage
import com.resonote.core.network.model.NetworkCloudTrack
import com.resonote.core.network.risk.ApiRiskChallengeDetector
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

@Singleton
internal class CloudProtocolClient @Inject constructor(
    private val transport: ProtocolTransport,
    private val registration: DeviceRegistrationCoordinator,
    private val json: Json,
    private val crypto: ApiProtocolCrypto,
    private val signer: ApiRequestSigner,
    private val origins: ApiEndpointOrigins,
    private val riskDetector: ApiRiskChallengeDetector,
) {
    suspend fun tracks(page: Int, pageSize: Int): NetworkCloudPage {
        require(page > 0) { "page must be positive" }
        require(pageSize in 1..100) { "pageSize must be between 1 and 100" }
        registration.requireAuthenticatedSession()
        return transport.execute { session, nowMillis ->
            val encrypted = crypto.encryptPlaylist(buildJsonObject { put("page", page); put("pagesize", pageSize); put("getkmr", 1) }.toString())
            val p =
                crypto.pkcs1LiteRsa(
                    buildJsonObject {
                        put("aes", encrypted.key)
                        put("uid", requireNotNull(session.userId))
                        put("token", requireNotNull(session.token))
                    }.toString(),
                ).uppercase()
            val clientTime = nowMillis / 1_000
            ApiExchange(
                spec =
                    ApiEndpointSpec(
                        id = "API-CLOUD-001",
                        origin = origins.cloud,
                        path = "/v1/get_list",
                        method = ApiHttpMethod.Post,
                        query =
                            linkedMapOf(
                                "clienttime" to clientTime.toString(),
                                "mid" to session.mid,
                                "key" to signer.signParamsKey(clientTime.toString()),
                                "clientver" to ApiProtocolConfig.CLIENT_VERSION,
                                "appid" to ApiProtocolConfig.APP_ID,
                                "p" to p,
                            ),
                        body = encrypted.ciphertext,
                        contentType = "application/octet-stream",
                        signatureMode = ApiSignatureMode.None,
                        includeDefaultParams = false,
                        responseFormat = ApiResponseFormat.Bytes,
                    ),
                decode = { response ->
                    val plaintext =
                        runCatching { crypto.decryptPlaylist(response.bytes, encrypted.key) }
                            .getOrElse { throw ApiProtocolException(ApiProtocolException.Reason.MalformedResponse) }
                    val root =
                        runCatching { json.parseToJsonElement(plaintext) as? JsonObject }
                            .getOrNull() ?: throw ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)
                    riskDetector.detect(response.copy(body = root))?.let {
                        throw ApiRiskException(it, ApiRiskException.Reason.VerificationUnavailable)
                    }
                    decodePage(root, page, pageSize)
                },
            )
        }
    }

    private fun decodePage(root: JsonObject, page: Int, pageSize: Int): NetworkCloudPage {
        if (root.text("status") != "1") throw ApiServiceException(root.text("error_code") ?: root.text("status"))
        val data = root["data"] as? JsonObject ?: throw missingField()
        val total = data.long("list_count")?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt()
        val raw = data.cloudTrackList() ?: if (total == 0) JsonArray(emptyList()) else throw missingField()
        val tracks = raw.mapNotNull(::decodeTrack)
        if (raw.isNotEmpty() && tracks.isEmpty()) throw missingField()
        val resolvedTotal = total ?: tracks.size
        val max = data.long("max_size")?.coerceAtLeast(0) ?: 0
        val used = data.long("used_size")?.coerceAtLeast(0) ?: 0
        return NetworkCloudPage(
            tracks = tracks,
            total = resolvedTotal,
            hasMore = if (resolvedTotal > 0) page.toLong() * pageSize < resolvedTotal else tracks.size >= pageSize,
            storage = max.takeIf { it > 0 }?.let { NetworkCloudStorage(used, it) },
        )
    }

    private fun decodeTrack(element: kotlinx.serialization.json.JsonElement): NetworkCloudTrack? {
        val item = element as? JsonObject ?: return null
        val hash = item.text("hash")?.takeIf(String::isNotBlank) ?: return null
        val filename = item.text("filename").orEmpty().replace(FILE_EXTENSION_PATTERN, "")
        val (filenameArtist, filenameTitle) = splitArtistTitle(filename)
        val albumInfo = item["album_info"] as? JsonObject
        val firstAuthor = (item["authors"] as? JsonArray)?.firstOrNull() as? JsonObject
        return NetworkCloudTrack(
            hash = hash,
            title = item.text("name")?.takeIf(String::isNotBlank) ?: filenameTitle.ifBlank { hash },
            artist = item.text("author_name")?.takeIf(String::isNotBlank) ?: filenameArtist.takeIf(String::isNotBlank),
            album = item.text("album_name")?.takeIf(String::isNotBlank),
            coverUrl = albumInfo?.text("sizable_cover")?.takeIf(String::isNotBlank) ?: firstAuthor?.text("sizable_avatar")?.takeIf(String::isNotBlank),
            durationMillis = normalizeDuration(item.long("timelen")),
            albumAudioId = item.text("album_audio_id")?.takeIf(String::isNotBlank),
        )
    }

    private fun splitArtistTitle(value: String): Pair<String, String> {
        val separator = value.indexOf(" - ")
        return if (separator > 0) value.substring(0, separator) to value.substring(separator + 3) else "" to value
    }

    private fun normalizeDuration(value: Long?): Long = value?.takeIf { it > 0 }?.let { if (it < 10_000) it * 1_000 else it } ?: 0

    private fun JsonObject.text(name: String): String? = (get(name) as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.long(name: String): Long? = text(name)?.toDoubleOrNull()?.toLong()
    private fun JsonObject.cloudTrackList(): JsonArray? =
        listOfNotNull(get("list"), get("info")).firstNotNullOfOrNull { value ->
            when (value) {
                is JsonArray -> value
                is JsonPrimitive -> {
                    val encoded = value.contentOrNull?.trim().orEmpty()
                    if (encoded.isEmpty() || encoded == "0" || encoded == "null") {
                        JsonArray(emptyList())
                    } else {
                        runCatching { json.parseToJsonElement(encoded) as? JsonArray }.getOrNull()
                    }
                }
                else -> null
            }
        }
    private fun missingField() = ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)

    private companion object {
        val FILE_EXTENSION_PATTERN = Regex("\\.[a-zA-Z0-9]{2,5}$")
    }
}

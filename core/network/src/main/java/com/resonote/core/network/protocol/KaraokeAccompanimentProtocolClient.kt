package com.resonote.core.network.protocol

import com.resonote.core.network.KaraokeNetworkDataSource
import com.resonote.core.network.NetworkKaraokeAccompaniment
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class KaraokeAccompanimentProtocolClient @Inject constructor(
    private val transport: ProtocolTransport,
    private val origins: ApiEndpointOrigins,
) : KaraokeNetworkDataSource {
    override suspend fun matchAccompaniment(
        originalHash: String,
        albumAudioId: String?,
        fileName: String,
    ): NetworkKaraokeAccompaniment? {
        require(originalHash.isNotBlank()) { "originalHash must not be blank" }
        val query = linkedMapOf(
            "isteen" to "0",
            "mixId" to (albumAudioId?.toLongOrNull() ?: 0L).toString(),
            "usemkv" to "1",
            "platform" to "2",
            "fileName" to fileName.trim(),
            "hash" to originalHash.trim(),
            "version" to KARAOKE_VERSION,
            "appid" to KARAOKE_APP_ID,
        )
        query["sign"] = karaokeAccompanimentSign(query)
        return transport.execute { _, _ ->
            ApiExchange(
                spec = ApiEndpointSpec(
                    origin = origins.karaoke,
                    path = "/sing7/accompanywan/json/v2/cdn/optimal_matching_accompany_2_listen.do",
                    method = ApiHttpMethod.Get,
                    query = query,
                    signatureMode = ApiSignatureMode.None,
                    sessionPropagation = ApiSessionPropagation.None,
                    includeDefaultParams = false,
                    riskPolicy = ApiRiskPolicy.Bypass,
                ),
                decode = { response -> decode(response.body) },
            )
        }
    }

    private fun decode(root: JsonObject?): NetworkKaraokeAccompaniment? {
        val payload = root?.accompanimentObject() ?: return null
        val hash = payload.text("hash")?.takeIf(String::isNotBlank) ?: return null
        return NetworkKaraokeAccompaniment(
            hash = hash,
            songId = payload.long("songid") ?: payload.long("accompanySongId"),
            songName = payload.text("songname"),
            singerName = payload.text("singername"),
            durationMillis = (payload.long("duration") ?: 0L) * 1_000L,
            extension = payload.text("extname"),
            bitrateKbps = payload.int("bitrate"),
            sizeBytes = payload.long("filesize"),
            remark = payload.text("remark"),
            showMic = payload.boolean("showMic") ?: true,
        )
    }

    private fun JsonObject.accompanimentObject(): JsonObject? {
        if (text("hash") != null) return this
        val data = get("data")
        return when (data) {
            is JsonObject -> data.takeIf { it.text("hash") != null }
                ?: (data["list"] as? JsonArray)?.firstOrNull() as? JsonObject
            is JsonArray -> data.firstOrNull() as? JsonObject
            else -> null
        }
    }

    private fun JsonObject.text(name: String): String? = (get(name) as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.long(name: String): Long? = (get(name) as? JsonPrimitive)?.let {
        it.longOrNull ?: it.contentOrNull?.toLongOrNull()
    }
    private fun JsonObject.int(name: String): Int? = (get(name) as? JsonPrimitive)?.let {
        it.intOrNull ?: it.contentOrNull?.toIntOrNull()
    }
    private fun JsonObject.boolean(name: String): Boolean? = (get(name) as? JsonPrimitive)?.let {
        it.booleanOrNull ?: when (it.contentOrNull) {
            "1" -> true
            "0" -> false
            else -> null
        }
    }

    private companion object {
        const val KARAOKE_APP_ID = "1005"
        const val KARAOKE_VERSION = "12375"
    }
}

internal fun karaokeAccompanimentSign(parameters: Map<String, String>): String {
    val canonical = parameters.toSortedMap().entries.joinToString("&") { (key, value) -> "$key=$value" }
    return MessageDigest.getInstance("MD5")
        .digest("$canonical*s&iN#G70*".encodeToByteArray())
        .toHex()
        .substring(8, 24)
}

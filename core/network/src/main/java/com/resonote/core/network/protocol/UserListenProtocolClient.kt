package com.resonote.core.network.protocol

import com.resonote.core.network.ApiAuthenticationRequiredException
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.ListeningHistoryNetworkDataSource
import com.resonote.core.network.model.NetworkSong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

@Singleton
internal class UserListenProtocolClient @Inject constructor(
    private val transport: ProtocolTransport,
    private val registration: DeviceRegistrationCoordinator,
    private val crypto: ApiProtocolCrypto,
    private val origins: ApiEndpointOrigins,
) : ListeningHistoryNetworkDataSource {
    override suspend fun accountHistory(): List<NetworkSong> {
        registration.requireAuthenticatedSession()
        return transport.execute { session, nowMillis ->
            if (!session.isAuthenticated) throw ApiAuthenticationRequiredException()
            val clientTime = nowMillis / 1_000
            val userId = requireNotNull(session.userId)
            val token = requireNotNull(session.token)
            val encryptedSession =
                crypto.rawLiteRsa(
                    buildJsonObject {
                        put("clienttime", clientTime)
                        put("token", token)
                    }.toString(),
                ).uppercase()
            val body =
                buildJsonObject {
                    put("t_userid", userId)
                    put("userid", userId)
                    put("list_type", ALL_TIME_HISTORY)
                    put("area_code", 1)
                    put("cover", 2)
                    put("p", encryptedSession)
                }.toString().encodeToByteArray()
            ApiExchange(
                spec =
                    ApiEndpointSpec(
                        id = ENDPOINT_ID,
                        origin = origins.listen,
                        path = "/v2/get_list",
                        method = ApiHttpMethod.Post,
                        query = linkedMapOf("clienttime" to clientTime.toString(), "plat" to "0"),
                        body = body,
                    ),
                decode = { response -> decodeHistory(response.body ?: throw malformedResponse()) },
            )
        }
    }

    private fun decodeHistory(root: JsonObject): List<NetworkSong> {
        if (root.scalar("status") != "1") {
            throw ApiServiceException(root.scalar("error_code") ?: root.scalar("status"))
        }
        val data = root["data"] as? JsonObject ?: throw missingField()
        val rawSongs = data["lists"] as? JsonArray ?: throw missingField()
        val songs = rawSongs.mapNotNull(::decodeSong)
        if (rawSongs.isNotEmpty() && songs.isEmpty()) throw missingField()
        return songs
    }

    private fun decodeSong(element: kotlinx.serialization.json.JsonElement): NetworkSong? {
        val item = element as? JsonObject ?: return null
        val hash = item.string("hash")?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val rawName = item.string("name")?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val (nameArtist, title) = splitArtistTitle(rawName)
        return NetworkSong(
            hash = hash,
            title = title,
            artist =
                item.string("author_name")?.trim()?.takeIf(String::isNotEmpty)
                    ?: item.string("singername")?.trim()?.takeIf(String::isNotEmpty)
                    ?: nameArtist,
            coverUrl = item.string("image")?.trim()?.takeIf(String::isNotEmpty),
            albumId = null,
            albumAudioId = null,
            durationMillis = normalizeDuration(item.long("duration")),
            highQualityHash = null,
            losslessHash = null,
            vip = false,
        )
    }

    private fun splitArtistTitle(value: String): Pair<String?, String> {
        val separator = value.indexOf(" - ")
        if (separator <= 0) return null to value
        val artist = value.substring(0, separator).trim()
        val title = value.substring(separator + 3).trim()
        return artist.takeIf(String::isNotEmpty) to title.ifEmpty { value }
    }

    private fun normalizeDuration(value: Long?): Long =
        value?.takeIf { it > 0 }?.let { if (it < 10_000) it * 1_000 else it } ?: 0

    private fun JsonObject.string(name: String): String? =
        (get(name) as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.contentOrNull

    private fun JsonObject.scalar(name: String): String? =
        (get(name) as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.long(name: String): Long? {
        val value = get(name) as? JsonPrimitive ?: return null
        if (!value.isString && value.contentOrNull == null) return null
        return value.contentOrNull?.toDoubleOrNull()?.toLong()
    }

    private fun missingField() = ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)

    private fun malformedResponse() = ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)

    private companion object {
        const val ENDPOINT_ID = "API-USER-007"
        const val ALL_TIME_HISTORY = 1
    }
}

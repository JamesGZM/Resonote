package com.resonote.core.network.protocol

import com.resonote.core.network.ApiAuthenticationRequiredException
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.ListeningHistoryNetworkDataSource
import com.resonote.core.network.NetworkListeningHistoryPage
import com.resonote.core.network.model.NetworkSong
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class UserListenProtocolClient @Inject constructor(
    private val transport: ProtocolTransport,
    private val registration: DeviceRegistrationCoordinator,
    private val origins: ApiEndpointOrigins,
) : ListeningHistoryNetworkDataSource {
    override suspend fun accountHistory(cursor: String?): NetworkListeningHistoryPage {
        registration.requireAuthenticatedSession()
        return transport.execute { session, _ ->
            if (!session.isAuthenticated) throw ApiAuthenticationRequiredException()
            val userId = requireNotNull(session.userId)
            val token = requireNotNull(session.token)
            val body =
                buildJsonObject {
                    put("userid", userId)
                    put("token", token)
                    put("source_classify", "app")
                    put("to_subdivide_sr", 1)
                    cursor?.trim()?.takeIf(String::isNotEmpty)?.let { put("bp", it) }
                }.toString().encodeToByteArray()
            ApiExchange(
                spec =
                ApiEndpointSpec(
                    origin = origins.gateway,
                    path = "/playhistory/v1/get_songs",
                    method = ApiHttpMethod.Post,
                    body = body,
                ),
                decode = { response -> decodeHistory(response.body ?: throw malformedResponse()) },
            )
        }
    }

    override suspend fun uploadAccountPlayback(albumAudioId: String) {
        val mixSongId = albumAudioId.trim().toLongOrNull()
            ?: throw ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)
        registration.requireAuthenticatedSession()
        transport.execute { session, clientTime ->
            if (!session.isAuthenticated) throw ApiAuthenticationRequiredException()
            val userId = requireNotNull(session.userId)
            val token = requireNotNull(session.token)
            val body = buildJsonObject {
                put(
                    "songs",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put("mxid", mixSongId)
                                put("op", 1)
                                put("ot", clientTime / 1_000)
                                put("pc", 1)
                            },
                        )
                    },
                )
                put("token", token)
                put("userid", userId)
            }.toString().encodeToByteArray()
            ApiExchange(
                spec = ApiEndpointSpec(
                    origin = origins.gateway,
                    path = "/playhistory/v1/upload_songs",
                    method = ApiHttpMethod.Post,
                    query = mapOf("plat" to "3"),
                    body = body,
                ),
                decode = { response ->
                    val root = response.body ?: throw malformedResponse()
                    if (root.scalar("status") != "1") {
                        throw ApiServiceException(root.scalar("error_code") ?: root.scalar("status"))
                    }
                },
            )
        }
    }

    private fun decodeHistory(root: JsonObject): NetworkListeningHistoryPage {
        if (root.scalar("status") != "1") {
            throw ApiServiceException(root.scalar("error_code") ?: root.scalar("status"))
        }
        val data = root["data"] as? JsonObject ?: throw missingField()
        val rawSongs = sequenceOf("songs", "lists", "list", "info")
            .mapNotNull { data[it] as? JsonArray }
            .firstOrNull() ?: throw missingField()
        val songs = rawSongs.mapNotNull(::decodeSong).asReversed()
        if (rawSongs.isNotEmpty() && songs.isEmpty()) throw missingField()
        val nextCursor = data.scalar("bp")?.trim()?.takeIf(String::isNotEmpty)
        val hasMore = data.boolean("has_more")
            ?: data.boolean("hasMore")
            ?: (nextCursor != null && data.scalar("bp_finished") != nextCursor)
        return NetworkListeningHistoryPage(
            songs = songs,
            nextCursor = nextCursor,
            hasMore = hasMore && nextCursor != null,
        )
    }

    private fun decodeSong(element: kotlinx.serialization.json.JsonElement): NetworkSong? {
        val item = element as? JsonObject ?: return null
        val info = (item["info"] as? JsonObject) ?: item
        val hash = info.firstString("hash", "Hash", "FileHash", "file_hash")
            ?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val rawName = info.firstString("name", "songname", "SongName", "filename")
            ?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val (nameArtist, title) = splitArtistTitle(rawName)
        return NetworkSong(
            hash = hash,
            title = title,
            artist =
            info.firstString("author_name", "singername", "singer_name", "author")
                ?.trim()?.takeIf(String::isNotEmpty)
                ?: nameArtist,
            coverUrl = info.firstString("image", "cover", "img")?.trim()?.takeIf(String::isNotEmpty),
            albumId = info.firstScalar("album_id", "albumid")?.trim()?.takeIf(String::isNotEmpty),
            albumAudioId = info.firstScalar("mixsongid", "MixSongID", "album_audio_id")
                ?: item.firstScalar("mxid", "mixsongid", "MixSongID"),
            durationMillis = normalizeDuration(info.firstLong("duration", "time_length", "timelength")),
            highQualityHash = null,
            losslessHash = null,
            vip = false,
            albumTitle = info.firstString("album_name", "albumname", "remark")
                ?.trim()?.takeIf(String::isNotEmpty),
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

    private fun JsonObject.scalar(name: String): String? = (get(name) as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.firstScalar(vararg names: String): String? = names.firstNotNullOfOrNull { scalar(it) }

    private fun JsonObject.firstString(vararg names: String): String? = names.firstNotNullOfOrNull { name ->
        (get(name) as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.contentOrNull
    }

    private fun JsonObject.firstLong(vararg names: String): Long? =
        names.firstNotNullOfOrNull { scalar(it)?.toDoubleOrNull()?.toLong() }

    private fun JsonObject.boolean(name: String): Boolean? = scalar(name)?.let {
        when (it.lowercase()) {
            "1", "true" -> true
            "0", "false" -> false
            else -> null
        }
    }

    private fun missingField() = ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)

    private fun malformedResponse() = ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)
}

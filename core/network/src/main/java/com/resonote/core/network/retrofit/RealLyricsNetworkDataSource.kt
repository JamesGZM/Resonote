package com.resonote.core.network.retrofit

import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.LyricsNetworkDataSource
import com.resonote.core.network.api.MusicApi
import com.resonote.core.network.model.NetworkLyricCandidate
import com.resonote.core.network.protocol.ApiEndpointOrigins
import com.resonote.core.network.protocol.DeviceRegistrationCoordinator
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Singleton
internal class RealLyricsNetworkDataSource @Inject constructor(
    private val musicApi: MusicApi,
    private val registration: DeviceRegistrationCoordinator,
    private val responses: ApiResponseVerifier,
    private val calls: ApiCallExecutor,
    private val origins: ApiEndpointOrigins,
) : LyricsNetworkDataSource {
    override suspend fun searchLyric(hash: String, albumAudioId: String?): NetworkLyricCandidate? {
        require(hash.isNotBlank()) { "hash must not be blank" }
        registration.ensureRegisteredSession()
        val root = calls.execute {
            musicApi.searchLyric("${origins.lyrics}/v1/search", (albumAudioId?.toLongOrNull() ?: 0).toString(), hash = hash.trim())
        }.obj() ?: throw malformedResponse()
        responses.requireJsonSuccess(root, SEARCH_LYRIC_ENDPOINT_ID, setOf("200"))
        val first = root.array("candidates").orEmpty().firstOrNull().obj() ?: return null
        return NetworkLyricCandidate(
            first.text("id")?.takeIf(String::isNotBlank) ?: return null,
            first.text("accesskey")?.takeIf(String::isNotBlank) ?: return null,
        )
    }

    override suspend fun downloadLyric(candidate: NetworkLyricCandidate): String? {
        require(candidate.id.isNotBlank() && candidate.accessKey.isNotBlank()) { "lyric candidate must be complete" }
        registration.ensureRegisteredSession()
        val root = calls.execute {
            musicApi.downloadLyric("${origins.lyrics}/download", id = candidate.id, accessKey = candidate.accessKey)
        }.obj() ?: throw malformedResponse()
        responses.requireJsonSuccess(root, DOWNLOAD_LYRIC_ENDPOINT_ID, setOf("200"))
        val encoded = root.text("content")?.takeIf(String::isNotBlank) ?: return null
        return runCatching { Base64.getDecoder().decode(encoded).decodeToString() }
            .getOrElse { throw malformedResponse() }
            .takeIf(String::isNotBlank)
    }

    private fun JsonElement?.obj(): JsonObject? = this as? JsonObject
    private fun JsonObject.array(name: String): JsonArray? = get(name) as? JsonArray
    private fun JsonObject.text(name: String): String? = (get(name) as? JsonPrimitive)?.contentOrNull
    private fun malformedResponse() = ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)

    private companion object {
        const val SEARCH_LYRIC_ENDPOINT_ID = "API-SEARCH-005"
        const val DOWNLOAD_LYRIC_ENDPOINT_ID = "API-LYRICS-001"
    }
}

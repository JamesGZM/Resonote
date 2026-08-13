package com.resonote.core.network.retrofit

import com.resonote.core.network.ApiPlaybackUnavailableException
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.CloudNetworkDataSource
import com.resonote.core.network.api.MusicApi
import com.resonote.core.network.model.NetworkCloudPage
import com.resonote.core.network.model.NetworkSongSource
import com.resonote.core.network.protocol.ApiRequestSigner
import com.resonote.core.network.protocol.CloudProtocolClient
import com.resonote.core.network.protocol.DeviceRegistrationCoordinator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Singleton
internal class RealCloudNetworkDataSource @Inject constructor(
    private val musicApi: MusicApi,
    private val registration: DeviceRegistrationCoordinator,
    private val cloudProtocol: CloudProtocolClient,
    private val signer: ApiRequestSigner,
    private val calls: ApiCallExecutor,
    private val responses: ApiResponseVerifier,
) : CloudNetworkDataSource {
    override suspend fun cloudTracks(page: Int, pageSize: Int): NetworkCloudPage =
        cloudProtocol.tracks(page, pageSize)

    override suspend fun resolveCloudSongSource(hash: String, albumAudioId: String?, name: String): NetworkSongSource {
        require(hash.isNotBlank()) { "hash must not be blank" }
        requireAuthenticatedSession()
        val normalizedHash = hash.trim().lowercase()
        val response = calls.execute {
            musicApi.cloudSongUrl(
                hash = normalizedHash,
                albumAudioId = (albumAudioId?.toLongOrNull() ?: 0).toString(),
                key = signer.signCloudKey(normalizedHash),
                name = name.trim(),
            )
        }
        responses.requireSuccess(response)
        val data = response.data as? JsonObject ?: throw missingField()
        val rawUrls = when (val url = data["url"]) {
            is JsonArray -> url.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            is JsonPrimitive -> listOfNotNull(url.contentOrNull)
            else -> emptyList()
        }.map(String::trim).filter(String::isNotEmpty)
        val parsedUrls = rawUrls.mapNotNull { it.toHttpUrlOrNull() }
        val secureUrl = parsedUrls.firstOrNull { it.isHttps }
        if (rawUrls.isNotEmpty() && secureUrl == null) {
            if (parsedUrls.any { it.scheme == "http" }) {
                throw ApiProtocolException(ApiProtocolException.Reason.InsecureMediaUrl)
            }
            throw ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)
        }
        secureUrl ?: throw ApiPlaybackUnavailableException(ApiPlaybackUnavailableException.Reason.Cloud)
        val extension = secureUrl.pathSegments.lastOrNull()?.substringAfterLast('.', "")?.takeIf(String::isNotBlank)
        return NetworkSongSource(secureUrl.toString(), 0, extension)
    }

    private suspend fun requireAuthenticatedSession() =
        registration.requireAuthenticatedSession()

    private fun missingField() = ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)

}

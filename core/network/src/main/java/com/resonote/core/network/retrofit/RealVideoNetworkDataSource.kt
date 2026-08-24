package com.resonote.core.network.retrofit

import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.VideoNetworkDataSource
import com.resonote.core.network.api.MusicApi
import com.resonote.core.network.protocol.ApiRequestSigner
import com.resonote.core.network.protocol.DeviceRegistrationCoordinator
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RealVideoNetworkDataSource @Inject constructor(
    private val musicApi: MusicApi,
    private val registration: DeviceRegistrationCoordinator,
    private val signer: ApiRequestSigner,
    private val responses: ApiResponseVerifier,
    private val calls: ApiCallExecutor,
) : VideoNetworkDataSource {
    override suspend fun resolveVideoUrl(hash: String): String? {
        require(hash.isNotBlank()) { "hash must not be blank" }
        val session = registration.ensureRegisteredSession()
        val videoHash = hash.trim()
        val response = calls.execute {
            musicApi.videoUrl(
                hash = videoHash,
                key = signer.signVideoKey(videoHash, session.mid, session.userId),
            )
        }
        responses.requireSuccess(response)
        val entry = response.data.obj()?.values?.firstOrNull().obj() ?: return null
        val rawUrls = buildList {
            when (val backup = entry["backupdownurl"]) {
                is JsonArray -> backup.mapNotNullTo(this) { (it as? JsonPrimitive)?.contentOrNull }
                is JsonPrimitive -> add(backup.contentOrNull.orEmpty())
                else -> Unit
            }
            entry.text("downurl")?.let(::add)
        }.map(String::trim).filter(String::isNotEmpty)
        val parsedUrls = rawUrls.mapNotNull { it.toHttpUrlOrNull() }
        val playableUrl = parsedUrls.firstOrNull { it.isHttps || it.isAllowedCleartextVideoUrl() }
        if (playableUrl != null) return playableUrl.toString()
        if (parsedUrls.any { it.scheme == "http" }) {
            throw ApiProtocolException(ApiProtocolException.Reason.InsecureMediaUrl)
        }
        if (rawUrls.isNotEmpty()) throw malformedResponse()
        return null
    }

    private fun JsonElement?.obj(): JsonObject? = this as? JsonObject
    private fun JsonObject.text(name: String): String? = (get(name) as? JsonPrimitive)?.contentOrNull
    private fun okhttp3.HttpUrl.isAllowedCleartextVideoUrl(): Boolean =
        scheme == "http" && (host == KUGOU_DOMAIN || host.endsWith(".$KUGOU_DOMAIN"))

    private fun malformedResponse() = ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)

    private companion object {
        const val KUGOU_DOMAIN = "kugou.com"
    }
}

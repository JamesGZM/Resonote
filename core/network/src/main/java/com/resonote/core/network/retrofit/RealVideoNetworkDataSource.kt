package com.resonote.core.network.retrofit

import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.VideoNetworkDataSource
import com.resonote.core.network.api.MusicApi
import com.resonote.core.network.protocol.ApiRequestSigner
import com.resonote.core.network.protocol.DeviceRegistrationCoordinator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

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
        val normalizedHash = hash.trim().lowercase()
        val response = calls.execute { musicApi.videoUrl(hash = normalizedHash, key = signer.signSongKey(normalizedHash, session.mid, session.userId)) }
        responses.requireSuccess(response)
        val entry = response.data.obj()?.values?.firstOrNull().obj() ?: return null
        val backup = entry["backupdownurl"]
        val raw = when (backup) {
            is JsonArray -> (backup.firstOrNull() as? JsonPrimitive)?.contentOrNull
            is JsonPrimitive -> backup.contentOrNull
            else -> null
        }?.takeIf(String::isNotBlank) ?: entry.text("downurl")?.takeIf(String::isNotBlank) ?: return null
        val parsed = raw.toHttpUrlOrNull() ?: throw malformedResponse()
        if (!parsed.isHttps) throw ApiProtocolException(ApiProtocolException.Reason.InsecureMediaUrl)
        return parsed.toString()
    }

    private fun JsonElement?.obj(): JsonObject? = this as? JsonObject
    private fun JsonObject.text(name: String): String? = (get(name) as? JsonPrimitive)?.contentOrNull
    private fun malformedResponse() = ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)
}

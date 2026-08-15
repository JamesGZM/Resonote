package com.resonote.core.network.retrofit

import com.resonote.core.network.ApiAuthenticationRequiredException
import com.resonote.core.network.ApiException
import com.resonote.core.network.ApiPlaybackUnavailableException
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiRiskException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.PlaybackNetworkDataSource
import com.resonote.core.network.api.MusicApi
import com.resonote.core.network.api.model.SongPrivilegeRequest
import com.resonote.core.network.api.model.SongPrivilegeResource
import com.resonote.core.network.api.model.SongSourceResponse
import com.resonote.core.network.model.NetworkSongSource
import com.resonote.core.network.protocol.ApiProtocolConfig
import com.resonote.core.network.protocol.ApiRequestSigner
import com.resonote.core.network.protocol.DeviceRegistrationCoordinator
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RealPlaybackNetworkDataSource @Inject constructor(
    private val musicApi: MusicApi,
    private val registration: DeviceRegistrationCoordinator,
    private val signer: ApiRequestSigner,
    private val calls: ApiCallExecutor,
    private val responses: ApiResponseVerifier,
) : PlaybackNetworkDataSource {
    override suspend fun resolveSongSource(
        hash: String,
        albumId: String?,
        albumAudioId: String?,
        requestedQuality: String,
    ): NetworkSongSource {
        require(hash.isNotBlank()) { "hash must not be blank" }
        val session = registration.ensureRegisteredSession()
        val normalizedHash = hash.trim().lowercase()
        val normalizedAlbumId = (albumId?.toLongOrNull() ?: 0).toString()
        val normalizedAlbumAudioId = (albumAudioId?.toLongOrNull() ?: 0).toString()
        val normalizedQuality = requestedQuality.takeIf(QUALITY_LEVELS::contains) ?: STANDARD_QUALITY
        val candidates = if (session.isAuthenticated) {
            resolveCandidates(
                hash = normalizedHash,
                requestedQuality = normalizedQuality,
                token = requireNotNull(session.token),
                userId = requireNotNull(session.userId),
            )
        } else {
            listOf(PlaybackCandidate(normalizedHash, STANDARD_QUALITY))
        }
        var lastFailureCode: String? = null
        var lastFailure: ApiException? = null
        var sawVipRequired = false
        for (candidate in candidates) {
            val response = try {
                calls.execute(detectHttpAuthenticationFailure = false) {
                    musicApi.songSource(
                        albumId = if (session.isAuthenticated) NO_ALBUM_ID else normalizedAlbumId,
                        hash = candidate.hash,
                        quality = candidate.quality,
                        albumAudioId = if (session.isAuthenticated) NO_ALBUM_AUDIO_ID else normalizedAlbumAudioId,
                        isFreePart = if (session.isAuthenticated) "0" else "1",
                        parentPageId =
                        if (session.isAuthenticated) {
                            AUTHENTICATED_PARENT_PAGE_ID
                        } else {
                            ANONYMOUS_PARENT_PAGE_ID
                        },
                        key = signer.signSongKey(candidate.hash, session.mid, session.userId),
                        token = session.token,
                        userId = session.userId,
                    )
                }
            } catch (authentication: ApiAuthenticationRequiredException) {
                throw authentication
            } catch (risk: ApiRiskException) {
                throw risk
            } catch (failure: ApiException) {
                lastFailure = failure
                continue
            }
            responses.requireNoRiskChallenge(response)
            val failureCode = responses.serviceFailureCodeOrNull(response)
            if (failureCode == null) {
                if (response.extension.equals("mp4", ignoreCase = true)) continue
                if (response.status?.toDoubleOrNull() == SUCCESS_STATUS && !response.hasAnyUrl()) {
                    lastFailure = ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)
                    continue
                }
                return decodeSongSource(response, isPreview = !session.isAuthenticated)
            }
            if (failureCode == SONG_SOURCE_VIP_REQUIRED_CODE) {
                sawVipRequired = true
            } else {
                lastFailureCode = failureCode
            }
        }
        lastFailureCode?.let { throw ApiServiceException(it) }
        if (sawVipRequired) throw ApiPlaybackUnavailableException(ApiPlaybackUnavailableException.Reason.Vip)
        lastFailure?.let { throw it }
        throw ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)
    }

    private fun decodeSongSource(response: SongSourceResponse, isPreview: Boolean): NetworkSongSource {
        val status = response.status?.toLongOrNull() ?: throw missingField()
        val rawUrls = sequenceOf(response.url, response.backupUrl, response.legacyBackupUrl)
            .flatten()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()
        val parsedUrls = rawUrls.mapNotNull { it.toHttpUrlOrNull() }
        val playableUrl = parsedUrls.firstOrNull { it.isHttps || it.isAllowedCleartextMediaUrl() }
        if (rawUrls.isNotEmpty() && playableUrl == null) {
            if (parsedUrls.any { it.scheme == "http" }) {
                throw ApiProtocolException(ApiProtocolException.Reason.InsecureMediaUrl)
            }
            throw ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)
        }
        if (playableUrl == null) {
            if (status == 3L) {
                throw ApiPlaybackUnavailableException(ApiPlaybackUnavailableException.Reason.Copyright)
            }
            throw ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)
        }
        return NetworkSongSource(
            uri = playableUrl.toString(),
            durationMillis = normalizeDurationMillis(response.timeLength),
            extension = response.extension?.takeIf(String::isNotBlank),
            isPreview = isPreview,
        )
    }

    private suspend fun resolveCandidates(
        hash: String,
        requestedQuality: String,
        token: String,
        userId: String,
    ): List<PlaybackCandidate> {
        val fallbackQualities = fallbackQualities(requestedQuality)
        val response = try {
            calls.execute(detectHttpAuthenticationFailure = false) {
                musicApi.songPrivilege(
                    token = token,
                    userId = userId,
                    body = SongPrivilegeRequest(
                        appid = ApiProtocolConfig.APP_ID.toInt(),
                        clientver = ApiProtocolConfig.CLIENT_VERSION.toInt(),
                        resource = listOf(SongPrivilegeResource(hash = hash)),
                    ),
                )
            }
        } catch (authentication: ApiAuthenticationRequiredException) {
            throw authentication
        } catch (risk: ApiRiskException) {
            throw risk
        } catch (_: ApiException) {
            return fallbackQualities.map { PlaybackCandidate(hash, it) }
        }
        responses.requireNoRiskChallenge(response)
        responses.serviceFailureCodeOrNull(response)?.let {
            return fallbackQualities.map { PlaybackCandidate(hash, it) }
        }
        val candidatesByQuality = response.data
            ?.asArray()
            .orEmpty()
            .flatMap { resource -> resource.variants() }
            .mapNotNull { variant ->
                val item = variant.asObject() ?: return@mapNotNull null
                val quality = item.text("quality")?.takeIf(QUALITY_LEVELS::contains) ?: return@mapNotNull null
                val candidateHash =
                    item.text("hash")?.trim()?.lowercase()?.takeIf(String::isNotEmpty) ?: return@mapNotNull null
                PlaybackCandidate(candidateHash, quality).takeIf { item.int("level") != 0 }
            }
            .distinctBy(PlaybackCandidate::quality)
            .associateBy(PlaybackCandidate::quality)
        return fallbackQualities.mapNotNull(candidatesByQuality::get)
            .ifEmpty { fallbackQualities.map { PlaybackCandidate(hash, it) } }
    }

    private fun fallbackQualities(requestedQuality: String): List<String> = QUALITY_LEVELS.take(
        QUALITY_LEVELS.indexOf(requestedQuality) + 1,
    ).asReversed()

    private fun normalizeDurationMillis(value: Long?): Long =
        value?.coerceAtLeast(0)?.let { if (it in 1..86_400) it * 1_000 else it } ?: 0

    private fun missingField() = ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)

    private fun JsonElement.asArray(): JsonArray? = this as? JsonArray

    private fun JsonElement.asObject(): JsonObject? = this as? JsonObject

    private fun JsonElement.variants(): List<JsonElement> {
        val item = asObject() ?: return emptyList()
        return listOf(this) + (item["relate_goods"] as? JsonArray).orEmpty()
    }

    private fun JsonObject.text(name: String): String? = (get(name) as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.int(name: String): Int? = (get(name) as? JsonPrimitive)?.intOrNull

    private fun SongSourceResponse.hasAnyUrl(): Boolean = sequenceOf(url, backupUrl, legacyBackupUrl).flatten().any {
        it.isNotBlank()
    }

    private fun okhttp3.HttpUrl.isAllowedCleartextMediaUrl(): Boolean =
        scheme == "http" && (host == KUGOU_DOMAIN || host.endsWith(".$KUGOU_DOMAIN"))

    private companion object {
        data class PlaybackCandidate(val hash: String, val quality: String)

        const val KUGOU_DOMAIN = "kugou.com"
        const val NO_ALBUM_ID = "0"
        const val NO_ALBUM_AUDIO_ID = "0"
        const val STANDARD_QUALITY = "128"
        const val AUTHENTICATED_PARENT_PAGE_ID = "356753938"
        const val ANONYMOUS_PARENT_PAGE_ID = "356753938,823673182,967485191"
        const val SONG_SOURCE_VIP_REQUIRED_CODE = "35104"
        const val SUCCESS_STATUS = 1.0
        val QUALITY_LEVELS = listOf("128", "320", "flac", "high", "viper_atmos", "viper_clear", "viper_tape")
    }
}

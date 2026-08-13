package com.resonote.core.network.retrofit

import com.resonote.core.network.ApiPlaybackUnavailableException
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.PlaybackNetworkDataSource
import com.resonote.core.network.api.MusicApi
import com.resonote.core.network.api.model.SongSourceResponse
import com.resonote.core.network.model.NetworkSongSource
import com.resonote.core.network.protocol.ApiRequestSigner
import com.resonote.core.network.protocol.DeviceRegistrationCoordinator
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Singleton
internal class RealPlaybackNetworkDataSource @Inject constructor(
    private val musicApi: MusicApi,
    private val registration: DeviceRegistrationCoordinator,
    private val signer: ApiRequestSigner,
    private val calls: ApiCallExecutor,
    private val responses: ApiResponseVerifier,
) : PlaybackNetworkDataSource {
    override suspend fun resolveSongSource(hash: String, albumId: String?, albumAudioId: String?): NetworkSongSource {
        require(hash.isNotBlank()) { "hash must not be blank" }
        val session = registration.ensureRegisteredSession()
        val normalizedHash = hash.trim().lowercase()
        val response = calls.execute {
            musicApi.songSource(
                albumId = (albumId?.toLongOrNull() ?: 0).toString(),
                hash = normalizedHash,
                albumAudioId = (albumAudioId?.toLongOrNull() ?: 0).toString(),
                key = signer.signSongKey(normalizedHash, session.mid, session.userId),
            )
        }
        return decodeSongSource(response)
    }

    private fun decodeSongSource(response: SongSourceResponse): NetworkSongSource {
        responses.requireNoRiskChallenge(response)
        responses.serviceFailureCodeOrNull(response)?.let { serviceCode ->
            if (serviceCode.trim() == SONG_SOURCE_VIP_REQUIRED_CODE) {
                throw ApiPlaybackUnavailableException(ApiPlaybackUnavailableException.Reason.Vip)
            }
            throw ApiServiceException(serviceCode)
        }
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
            val reason = if (status == 3L) {
                ApiPlaybackUnavailableException.Reason.Copyright
            } else {
                ApiPlaybackUnavailableException.Reason.Vip
            }
            throw ApiPlaybackUnavailableException(reason)
        }
        return NetworkSongSource(
            uri = playableUrl.toString(),
            durationMillis = normalizeDurationMillis(response.timeLength),
            extension = response.extension?.takeIf(String::isNotBlank),
        )
    }

    private fun normalizeDurationMillis(value: Long?): Long =
        value?.coerceAtLeast(0)?.let { if (it in 1..86_400) it * 1_000 else it } ?: 0

    private fun missingField() = ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)

    private fun okhttp3.HttpUrl.isAllowedCleartextMediaUrl(): Boolean =
        scheme == "http" && (host == KUGOU_DOMAIN || host.endsWith(".$KUGOU_DOMAIN"))

    private companion object {
        const val KUGOU_DOMAIN = "kugou.com"
        const val SONG_SOURCE_VIP_REQUIRED_CODE = "35104"
    }
}

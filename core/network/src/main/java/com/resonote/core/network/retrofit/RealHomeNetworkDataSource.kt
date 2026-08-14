package com.resonote.core.network.retrofit

import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.HomeNetworkDataSource
import com.resonote.core.network.api.MusicApi
import com.resonote.core.network.api.model.NewSongsRequest
import com.resonote.core.network.api.model.RadioRecommendationsRequest
import com.resonote.core.network.model.NetworkRecommendationMode
import com.resonote.core.network.model.NetworkSong
import com.resonote.core.network.protocol.ApiProtocolConfig
import com.resonote.core.network.protocol.ApiRequestSigner
import com.resonote.core.network.protocol.DeviceRegistrationCoordinator
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RealHomeNetworkDataSource @Inject constructor(
    private val musicApi: MusicApi,
    private val registration: DeviceRegistrationCoordinator,
    private val signer: ApiRequestSigner,
    private val clock: Clock,
    private val responses: ApiResponseVerifier,
    private val calls: ApiCallExecutor,
) : HomeNetworkDataSource {
    override suspend fun dailyRecommendations(): List<NetworkSong> {
        registration.ensureRegisteredSession()
        val response = calls.execute { musicApi.dailyRecommendations() }
        responses.requireSuccess(response)
        val raw = response.data?.songs ?: throw missingField()
        return raw.mapNotNull { it.toNetworkSongOrNull() }.also { requireConsumableItems(raw, it) }
    }

    override suspend fun newSongs(page: Int, pageSize: Int): List<NetworkSong> {
        validatePage(page, pageSize)
        val session = registration.ensureRegisteredSession()
        val response = calls.execute {
            musicApi.newSongs(NewSongsRequest(21608, session.userId?.toLongOrNull() ?: 0, page, pageSize, emptyList()))
        }
        responses.requireSuccess(response)
        val raw = response.data ?: throw missingField()
        return raw.mapNotNull { it.toNetworkSongOrNull() }.also { requireConsumableItems(raw, it) }
    }

    override suspend fun radioRecommendations(mode: NetworkRecommendationMode): List<NetworkSong> {
        val session = registration.ensureRegisteredSession()
        val nowMillis = clock.millis()
        val body = RadioRecommendationsRequest(
            ApiProtocolConfig.APP_ID.toInt(), ApiProtocolConfig.CLIENT_VERSION.toInt(), "android", nowMillis,
            session.userId?.toLongOrNull() ?: 0, signer.signParamsKey(nowMillis.toString()), TOP_CARD_FAKEM, 1,
            session.mid, "-", emptyList(), TOP_CARD_USER_INFO,
        )
        val response = calls.execute { musicApi.radioRecommendations(mode.cardId, TOP_CARD_FAKEM, body = body) }
        responses.requireSuccess(response)
        val raw = response.data?.songs ?: throw missingField()
        return raw.mapNotNull { it.toNetworkSongOrNull() }.also { requireConsumableItems(raw, it) }
    }

    private fun validatePage(page: Int, pageSize: Int) {
        require(page > 0) { "page must be positive" }
        require(pageSize in 1..100) { "pageSize must be between 1 and 100" }
    }

    private fun <R, T> requireConsumableItems(raw: List<R>, decoded: List<T>) {
        if (raw.isNotEmpty() && decoded.isEmpty()) throw missingField()
    }

    private fun missingField() = ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)

    private companion object {
        const val TOP_CARD_FAKEM = "60f7ebf1f812edbac3c63a7310001701760f"
        const val TOP_CARD_USER_INFO = "a0c35cd40af564444b5584c2754dedec"
    }
}

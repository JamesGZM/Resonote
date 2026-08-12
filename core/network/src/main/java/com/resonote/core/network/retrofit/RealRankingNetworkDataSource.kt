package com.resonote.core.network.retrofit

import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.RankingNetworkDataSource
import com.resonote.core.network.api.MusicApi
import com.resonote.core.network.api.model.RankingSongsRequest
import com.resonote.core.network.model.NetworkRanking
import com.resonote.core.network.model.NetworkSongPage
import com.resonote.core.network.protocol.DeviceRegistrationCoordinator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RealRankingNetworkDataSource @Inject constructor(
    private val musicApi: MusicApi,
    private val registration: DeviceRegistrationCoordinator,
    private val responses: ApiResponseVerifier,
    private val calls: ApiCallExecutor,
) : RankingNetworkDataSource {
    override suspend fun rankings(): List<NetworkRanking> {
        registration.ensureRegisteredSession()
        val response = calls.execute { musicApi.rankings() }
        responses.requireSuccess(response)
        val raw = response.data?.rankings ?: throw missingField()
        return raw.mapNotNull { item ->
            val id = (item.rankid ?: item.rankId)?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val title = item.rankname?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            NetworkRanking(id, title, (item.imgurl ?: item.image9 ?: item.banner9)?.takeIf(String::isNotBlank))
        }.also { requireConsumableItems(raw, it) }
    }

    override suspend fun rankingSongs(rankId: String, page: Int, pageSize: Int): NetworkSongPage {
        require(rankId.isNotBlank()) { "rankId must not be blank" }
        validatePage(page, pageSize)
        registration.ensureRegisteredSession()
        val response = calls.execute {
            musicApi.rankingSongs(RankingSongsRequest(1, 1, 1, 1, pageSize, 0, 1, page, rankId.trim()))
        }
        responses.requireSuccess(response)
        val data = response.data ?: throw missingField()
        val raw = data.songs ?: throw missingField()
        val songs = raw.mapNotNull { it.toNetworkSongOrNull() }
        requireConsumableItems(raw, songs)
        val total = sequenceOf(data.total, data.totalCount, response.total).filterNotNull().firstOrNull()
            ?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt()?.takeIf { it > 0 }
        return NetworkSongPage(songs, total, songs.size >= pageSize)
    }

    private fun validatePage(page: Int, pageSize: Int) {
        require(page > 0) { "page must be positive" }
        require(pageSize in 1..100) { "pageSize must be between 1 and 100" }
    }
    private fun <R, T> requireConsumableItems(raw: List<R>, decoded: List<T>) {
        if (raw.isNotEmpty() && decoded.isEmpty()) throw missingField()
    }
    private fun missingField() = ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)
}

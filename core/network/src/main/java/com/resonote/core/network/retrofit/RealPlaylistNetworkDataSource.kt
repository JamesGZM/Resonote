package com.resonote.core.network.retrofit

import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.PlaylistNetworkDataSource
import com.resonote.core.network.api.MusicApi
import com.resonote.core.network.model.NetworkPlaylistInfo
import com.resonote.core.network.model.NetworkPlaylistPage
import com.resonote.core.network.protocol.DeviceRegistrationCoordinator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RealPlaylistNetworkDataSource @Inject constructor(
    private val musicApi: MusicApi,
    private val registration: DeviceRegistrationCoordinator,
    private val responses: ApiResponseVerifier,
    private val calls: ApiCallExecutor,
) : PlaylistNetworkDataSource {
    override suspend fun playlistSongs(globalCollectionId: String, page: Int, pageSize: Int): NetworkPlaylistPage {
        require(globalCollectionId.isNotBlank()) { "globalCollectionId must not be blank" }
        validatePage(page, pageSize)
        registration.ensureRegisteredSession()
        val normalizedId = globalCollectionId.trim()
        val response = calls.execute {
            musicApi.playlistSongs(
                beginIndex = (page - 1) * pageSize,
                pageSize = pageSize,
                globalCollectionId = normalizedId,
            )
        }
        responses.requireSuccess(response)
        val data = response.data ?: throw missingField()
        val raw = data.songs ?: throw missingField()
        val songs = raw.mapNotNull { it.toNetworkSongOrNull() }
        if (raw.isNotEmpty() && songs.isEmpty()) throw missingField()
        val listInfo = data.info
        val count = sequenceOf(data.count, listInfo?.count).filterNotNull().firstOrNull()
            ?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt()
        val title = listInfo?.name?.takeIf(String::isNotBlank)
        if (listInfo != null && title == null) throw missingField()
        val info = title?.let {
            NetworkPlaylistInfo(normalizedId, it, listInfo.intro.orEmpty(), listInfo.pic?.takeIf(String::isNotBlank), count ?: 0)
        }
        return NetworkPlaylistPage(
            info, songs,
            raw.size >= pageSize && (count == null || count <= 0 || page.toLong() * pageSize < count),
        )
    }

    private fun validatePage(page: Int, pageSize: Int) {
        require(page > 0) { "page must be positive" }
        require(pageSize in 1..100) { "pageSize must be between 1 and 100" }
    }
    private fun missingField() = ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)
}

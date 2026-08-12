package com.resonote.core.network

import com.resonote.core.network.model.NetworkSearchPage
import com.resonote.core.network.model.NetworkMobileCodeLoginResult
import com.resonote.core.network.model.NetworkHomePlaylist
import com.resonote.core.network.model.NetworkHomeSong
import com.resonote.core.network.model.NetworkRecommendationMode
import com.resonote.core.network.model.NetworkSongSource

interface ApiNetworkDataSource {
    suspend fun dailyRecommendations(): List<NetworkHomeSong>

    suspend fun recommendedPlaylists(
        page: Int = 1,
        pageSize: Int = 6,
    ): List<NetworkHomePlaylist>

    suspend fun newSongs(
        page: Int = 1,
        pageSize: Int = 6,
    ): List<NetworkHomeSong>

    suspend fun radioRecommendations(
        mode: NetworkRecommendationMode = NetworkRecommendationMode.Personal,
    ): List<NetworkHomeSong>

    suspend fun resolveSongSource(
        hash: String,
        albumId: String? = null,
        albumAudioId: String? = null,
    ): NetworkSongSource

    suspend fun searchSongs(
        keywords: String,
        page: Int = 1,
        pageSize: Int = 30,
    ): NetworkSearchPage

    suspend fun sendMobileCode(mobile: String)

    suspend fun loginWithMobileCode(
        mobile: String,
        code: String,
        selectedUserId: String? = null,
    ): NetworkMobileCodeLoginResult
}

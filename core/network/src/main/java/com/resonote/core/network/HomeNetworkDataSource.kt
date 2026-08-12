package com.resonote.core.network

import com.resonote.core.network.model.NetworkRecommendationMode
import com.resonote.core.network.model.NetworkSong

interface HomeNetworkDataSource {
    suspend fun dailyRecommendations(): List<NetworkSong>
    suspend fun newSongs(page: Int = 1, pageSize: Int = 6): List<NetworkSong>
    suspend fun radioRecommendations(mode: NetworkRecommendationMode = NetworkRecommendationMode.Personal): List<NetworkSong>
}

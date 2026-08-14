package com.resonote.core.network

import com.resonote.core.network.model.NetworkRanking
import com.resonote.core.network.model.NetworkSongPage

interface RankingNetworkDataSource {
    suspend fun rankings(): List<NetworkRanking>
    suspend fun rankingSongs(rankId: String, page: Int = 1, pageSize: Int = 30): NetworkSongPage
}

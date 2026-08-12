package com.resonote.core.network

import com.resonote.core.network.model.NetworkSong

interface ListeningHistoryNetworkDataSource {
    suspend fun accountHistory(): List<NetworkSong>
}

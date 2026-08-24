package com.resonote.core.network

import com.resonote.core.network.model.NetworkSong

data class NetworkListeningHistoryPage(val songs: List<NetworkSong>, val nextCursor: String?, val hasMore: Boolean)

interface ListeningHistoryNetworkDataSource {
    suspend fun accountHistory(cursor: String? = null): NetworkListeningHistoryPage

    suspend fun uploadAccountPlayback(albumAudioId: String)
}

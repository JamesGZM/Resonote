package com.resonote.core.network

import com.resonote.core.network.model.NetworkCloudPage
import com.resonote.core.network.model.NetworkSongSource

interface CloudNetworkDataSource {
    suspend fun cloudTracks(page: Int = 1, pageSize: Int = 50): NetworkCloudPage
    suspend fun resolveCloudSongSource(
        hash: String,
        albumAudioId: String? = null,
        name: String = "",
    ): NetworkSongSource
}

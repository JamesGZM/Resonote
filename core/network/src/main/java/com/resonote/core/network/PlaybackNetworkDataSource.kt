package com.resonote.core.network

import com.resonote.core.network.model.NetworkSongSource

interface PlaybackNetworkDataSource {
    suspend fun resolveSongSource(
        hash: String,
        albumId: String? = null,
        albumAudioId: String? = null,
        requestedQuality: String = "128",
    ): NetworkSongSource
}

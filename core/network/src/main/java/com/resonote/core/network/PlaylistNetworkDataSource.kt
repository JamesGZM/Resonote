package com.resonote.core.network

import com.resonote.core.network.model.NetworkPlaylistPage

interface PlaylistNetworkDataSource {
    suspend fun playlistSongs(globalCollectionId: String, page: Int = 1, pageSize: Int = 50): NetworkPlaylistPage
}

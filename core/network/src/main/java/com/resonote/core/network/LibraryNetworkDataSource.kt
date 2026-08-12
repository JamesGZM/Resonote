package com.resonote.core.network

import com.resonote.core.network.model.NetworkPlaylistTrackInput
import com.resonote.core.network.model.NetworkUserPlaylist

interface LibraryNetworkDataSource {
    suspend fun userPlaylists(page: Int = 1, pageSize: Int = 200): List<NetworkUserPlaylist>
    suspend fun createPlaylist(name: String): String
    suspend fun addPlaylistTracks(listId: String, tracks: List<NetworkPlaylistTrackInput>)
    suspend fun deletePlaylistTracks(listId: String, fileIds: List<String>)
}

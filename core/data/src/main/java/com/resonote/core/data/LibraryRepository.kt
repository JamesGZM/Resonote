package com.resonote.core.data

import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.PlaylistTrackInput
import com.resonote.core.model.UserPlaylist

interface LibraryRepository {
    suspend fun loadPlaylists(page: Int = 1, pageSize: Int = 200): CollectionLoadResult<List<UserPlaylist>>
    suspend fun createPlaylist(name: String): CollectionLoadResult<String>
    suspend fun addTracks(listId: String, tracks: List<PlaylistTrackInput>): CollectionLoadResult<Unit>
    suspend fun removeTracks(listId: String, fileIds: List<String>): CollectionLoadResult<Unit>
}

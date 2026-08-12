package com.resonote.core.data

import com.resonote.core.model.PlaylistTrackInput
import com.resonote.core.model.UserPlaylist
import com.resonote.core.network.LibraryNetworkDataSource
import com.resonote.core.network.model.NetworkPlaylistTrackInput
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultLibraryRepository @Inject constructor(
    private val network: LibraryNetworkDataSource,
    private val riskChallenges: RiskChallengeRegistry,
) : LibraryRepository {
    override suspend fun loadPlaylists(page: Int, pageSize: Int) = loadCollection(riskChallenges) {
        require(page > 0) { "page must be positive" }
        require(pageSize in 1..200) { "pageSize must be between 1 and 200" }
        network.userPlaylists(page, pageSize).map {
            UserPlaylist(it.listId, it.globalId, it.name, it.coverUrl?.replace("{size}", "240"), it.count, it.isMine, it.isLike)
        }
    }

    override suspend fun createPlaylist(name: String) = loadCollection(riskChallenges) {
        require(name.isNotBlank()) { "name must not be blank" }
        network.createPlaylist(name.trim())
    }

    override suspend fun addTracks(listId: String, tracks: List<PlaylistTrackInput>) = loadCollection(riskChallenges) {
        require(listId.isNotBlank()) { "listId must not be blank" }
        require(tracks.isNotEmpty()) { "tracks must not be empty" }
        network.addPlaylistTracks(
            listId,
            tracks.map { NetworkPlaylistTrackInput(it.hash, it.title, it.artist, it.albumId, it.albumAudioId) },
        )
    }

    override suspend fun removeTracks(listId: String, fileIds: List<String>) = loadCollection(riskChallenges) {
        require(listId.isNotBlank()) { "listId must not be blank" }
        require(fileIds.isNotEmpty()) { "fileIds must not be empty" }
        network.deletePlaylistTracks(listId, fileIds)
    }
}

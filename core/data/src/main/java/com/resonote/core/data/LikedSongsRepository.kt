package com.resonote.core.data

import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaylistTrackInput
import javax.inject.Inject
import javax.inject.Singleton

data class LikedSongsSnapshot(
    val playlistListId: String,
    val playlistGlobalId: String,
    val fileIdsByHash: Map<String, String>,
)

interface LikedSongsRepository {
    suspend fun load(): CollectionLoadResult<LikedSongsSnapshot?>
    suspend fun like(song: OnlineSong): CollectionLoadResult<Unit>
    suspend fun unlike(song: OnlineSong): CollectionLoadResult<Unit>
}

@Singleton
internal class DefaultLikedSongsRepository @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val playlistRepository: PlaylistRepository,
) : LikedSongsRepository {
    override suspend fun load(): CollectionLoadResult<LikedSongsSnapshot?> {
        val playlists = libraryRepository.loadPlaylists()
        if (playlists is CollectionLoadResult.Failed) return playlists
        val liked = (playlists as CollectionLoadResult.Available).value.firstOrNull { it.isLike }
            ?: return CollectionLoadResult.Available(null)
        val fileIds = buildMap {
            var page = 1
            do {
                when (val result = playlistRepository.loadPlaylist(liked.globalId, page, PAGE_SIZE)) {
                    is CollectionLoadResult.Failed -> return result
                    is CollectionLoadResult.Available -> {
                        result.value.songs.forEach { song ->
                            song.fileId?.takeIf(String::isNotBlank)?.let { put(song.hash, it) }
                        }
                        page++
                        if (!result.value.hasMore) break
                    }
                }
            } while (page <= MAX_PAGES)
        }
        return CollectionLoadResult.Available(LikedSongsSnapshot(liked.listId, liked.globalId, fileIds))
    }

    override suspend fun like(song: OnlineSong): CollectionLoadResult<Unit> = when (val snapshot = load()) {
        is CollectionLoadResult.Failed -> snapshot
        is CollectionLoadResult.Available -> {
            val value = snapshot.value ?: return CollectionLoadResult.Failed(
                com.resonote.core.model.ContentFailure.Protocol,
            )
            libraryRepository.addTracks(
                value.playlistListId,
                listOf(
                    PlaylistTrackInput(
                        hash = song.hash,
                        title = song.title,
                        artist = song.artist.orEmpty(),
                        albumId = song.albumId,
                        albumAudioId = song.albumAudioId,
                    ),
                ),
            )
        }
    }

    override suspend fun unlike(song: OnlineSong): CollectionLoadResult<Unit> = when (val snapshot = load()) {
        is CollectionLoadResult.Failed -> snapshot
        is CollectionLoadResult.Available -> {
            val value = snapshot.value ?: return CollectionLoadResult.Failed(
                com.resonote.core.model.ContentFailure.Protocol,
            )
            val fileId = value.fileIdsByHash[song.hash] ?: return CollectionLoadResult.Failed(
                com.resonote.core.model.ContentFailure.Protocol,
            )
            libraryRepository.removeTracks(value.playlistListId, listOf(fileId))
        }
    }

    private companion object {
        const val PAGE_SIZE = 100
        const val MAX_PAGES = 20
    }
}

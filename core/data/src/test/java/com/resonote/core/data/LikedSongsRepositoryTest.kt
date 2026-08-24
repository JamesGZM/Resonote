package com.resonote.core.data

import com.google.common.truth.Truth.assertThat
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaylistDetails
import com.resonote.core.model.PlaylistPage
import com.resonote.core.model.PlaylistTrackInput
import com.resonote.core.model.UserPlaylist
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LikedSongsRepositoryTest {
    @Test
    fun likeUsesLikedPlaylistAndUnlikeUsesResolvedFileId() = runTest {
        val library = FakeLibraryRepository()
        val repository = DefaultLikedSongsRepository(library, FakePlaylistRepository())

        assertThat(repository.like(SONG)).isEqualTo(CollectionLoadResult.Available(Unit))
        assertThat(library.added).isEqualTo("liked-list" to listOf(PlaylistTrackInput("hash", "Song", "Artist")))

        assertThat(repository.unlike(SONG)).isEqualTo(CollectionLoadResult.Available(Unit))
        assertThat(library.removed).isEqualTo("liked-list" to listOf("file-1"))
    }

    private class FakeLibraryRepository : LibraryRepository {
        var added: Pair<String, List<PlaylistTrackInput>>? = null
        var removed: Pair<String, List<String>>? = null

        override suspend fun loadPlaylists(page: Int, pageSize: Int) = CollectionLoadResult.Available(
            listOf(UserPlaylist("liked-list", "liked-global", "我喜欢", null, 1, true, true)),
        )

        override suspend fun addTracks(listId: String, tracks: List<PlaylistTrackInput>) =
            CollectionLoadResult.Available(Unit).also { added = listId to tracks }

        override suspend fun removeTracks(listId: String, fileIds: List<String>) =
            CollectionLoadResult.Available(Unit).also { removed = listId to fileIds }

        override suspend fun createPlaylist(name: String) = error("unused")
    }

    private class FakePlaylistRepository : PlaylistRepository {
        override suspend fun loadPlaylist(globalCollectionId: String, page: Int, pageSize: Int) =
            CollectionLoadResult.Available(
                PlaylistPage(
                    PlaylistDetails("liked-global", "我喜欢", "", null, 1),
                    listOf(SONG.copy(fileId = "file-1")),
                    1,
                    false,
                ),
            )
    }

    private companion object {
        val SONG = OnlineSong("hash", "Song", "Artist", null, null, null, 1000, AudioQuality.HighQuality, false)
    }
}

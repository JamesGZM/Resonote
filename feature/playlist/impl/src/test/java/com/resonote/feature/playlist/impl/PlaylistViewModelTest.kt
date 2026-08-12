package com.resonote.feature.playlist.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.PlaylistRepository
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaylistDetails
import com.resonote.core.model.PlaylistPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun firstPagePublishesRealDetailsAndSongs() = runTest(dispatcher) {
        val repository = FakePlaylistRepository()
        val viewModel = PlaylistViewModel(repository)

        viewModel.load("playlist")
        advanceUntilIdle()

        val state = viewModel.uiState.value as PlaylistUiState.Content
        assertThat(state.details?.title).isEqualTo("深夜歌单")
        assertThat(state.songs.map { it.hash }).containsExactly("song-1", "song-2").inOrder()
        assertThat(state.hasMore).isTrue()
        assertThat(repository.requests).containsExactly("playlist" to 1)
    }

    @Test
    fun missingDetailsAndSongsBecomeExplicitEmptyState() = runTest(dispatcher) {
        val repository = FakePlaylistRepository(emptyFirstPage = true)
        val viewModel = PlaylistViewModel(repository)

        viewModel.load("empty")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(PlaylistUiState.Empty)
    }

    @Test
    fun authenticationFailureCanBeRetriedAfterLogin() = runTest(dispatcher) {
        val repository = FakePlaylistRepository(failFirstRequest = true)
        val viewModel = PlaylistViewModel(repository)
        viewModel.load("protected")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value)
            .isEqualTo(PlaylistUiState.Error(ContentFailure.AuthenticationRequired))

        repository.failFirstRequest = false
        viewModel.retry()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(PlaylistUiState.Content::class.java)
        assertThat(repository.requests).containsExactly("protected" to 1, "protected" to 1).inOrder()
    }

    @Test
    fun nextPageAppendsUniqueSongsAndKeepsFirstPageDetails() = runTest(dispatcher) {
        val repository = FakePlaylistRepository()
        val viewModel = PlaylistViewModel(repository)
        viewModel.load("playlist")
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value as PlaylistUiState.Content
        assertThat(state.details?.title).isEqualTo("深夜歌单")
        assertThat(state.songs.map { it.hash }).containsExactly("song-1", "song-2", "song-3").inOrder()
        assertThat(state.page).isEqualTo(2)
        assertThat(state.hasMore).isFalse()
    }

    @Test
    fun nextPageFailureKeepsLoadedSongsAndExposesFooterError() = runTest(dispatcher) {
        val repository = FakePlaylistRepository(failSecondPage = true)
        val viewModel = PlaylistViewModel(repository)
        viewModel.load("playlist")
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value as PlaylistUiState.Content
        assertThat(state.songs.map { it.hash }).containsExactly("song-1", "song-2").inOrder()
        assertThat(state.loadMoreFailure).isEqualTo(ContentFailure.Network)
        assertThat(state.isLoadingMore).isFalse()
    }

    private class FakePlaylistRepository(
        var failFirstRequest: Boolean = false,
        private val failSecondPage: Boolean = false,
        private val emptyFirstPage: Boolean = false,
    ) : PlaylistRepository {
        val requests = mutableListOf<Pair<String, Int>>()

        override suspend fun loadPlaylist(
            globalCollectionId: String,
            page: Int,
            pageSize: Int,
        ): CollectionLoadResult<PlaylistPage> {
            requests += globalCollectionId to page
            if (page == 1 && failFirstRequest) {
                return CollectionLoadResult.Failed(ContentFailure.AuthenticationRequired)
            }
            if (page == 2 && failSecondPage) return CollectionLoadResult.Failed(ContentFailure.Network)
            if (emptyFirstPage) return CollectionLoadResult.Available(PlaylistPage(null, emptyList(), 1, false))
            return CollectionLoadResult.Available(
                if (page == 1) {
                    PlaylistPage(
                        PlaylistDetails("playlist", "深夜歌单", "适合安静聆听", null, 3),
                        listOf(song("song-1"), song("song-2")),
                        page = 1,
                        hasMore = true,
                    )
                } else {
                    PlaylistPage(
                        details = null,
                        songs = listOf(song("song-2"), song("song-3")),
                        page = 2,
                        hasMore = false,
                    )
                },
            )
        }
    }

    private companion object {
        fun song(id: String) = OnlineSong(
            hash = id,
            title = id,
            artist = "artist",
            coverUrl = null,
            albumId = null,
            albumAudioId = null,
            durationMillis = 180_000,
            quality = AudioQuality.Lossless,
            vip = false,
        )
    }
}

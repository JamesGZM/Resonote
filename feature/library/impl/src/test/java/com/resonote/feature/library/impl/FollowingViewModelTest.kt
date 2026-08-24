package com.resonote.feature.library.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.ContentCatalogRepository
import com.resonote.core.model.Album
import com.resonote.core.model.ArtistInfo
import com.resonote.core.model.ArtistSongsPage
import com.resonote.core.model.Banner
import com.resonote.core.model.CatalogSongPage
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.FollowedArtist
import com.resonote.core.model.PlaylistCategory
import com.resonote.core.model.PlaylistSummary
import com.resonote.core.model.SongPage
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
class FollowingViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun initialLoadShowsFirstPageAndLoadMoreRevealsRemainingArtists() = runTest(dispatcher) {
        val repository = FakeFollowingRepository(artists(35))
        val viewModel = FollowingViewModel(repository)
        advanceUntilIdle()

        var state = viewModel.uiState.value as FollowingUiState.Content
        assertThat(state.artists).hasSize(30)
        assertThat(state.total).isEqualTo(35)
        assertThat(state.hasMore).isTrue()

        viewModel.loadMore()

        state = viewModel.uiState.value as FollowingUiState.Content
        assertThat(state.artists).hasSize(35)
        assertThat(state.hasMore).isFalse()
    }

    @Test
    fun refreshFailureKeepsCurrentArtists() = runTest(dispatcher) {
        val repository = FakeFollowingRepository(artists(2))
        val viewModel = FollowingViewModel(repository)
        advanceUntilIdle()
        repository.nextLoadFailure = ContentFailure.Network

        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value as FollowingUiState.Content
        assertThat(state.artists).hasSize(2)
        assertThat(state.isRefreshing).isFalse()
        assertThat(state.refreshingFailure).isEqualTo(ContentFailure.Network)
    }

    @Test
    fun successfulUnfollowRemovesArtistFromList() = runTest(dispatcher) {
        val repository = FakeFollowingRepository(artists(2))
        val viewModel = FollowingViewModel(repository)
        advanceUntilIdle()
        val artist = (viewModel.uiState.value as FollowingUiState.Content).artists.first()

        viewModel.unfollow(artist)
        advanceUntilIdle()

        val state = viewModel.uiState.value as FollowingUiState.Content
        assertThat(state.artists.map(FollowedArtist::id)).doesNotContain(artist.id)
        assertThat(state.total).isEqualTo(1)
        assertThat(repository.unfollowedIds).containsExactly(artist.id)
    }

    private fun artists(count: Int) = (1..count).map {
        FollowedArtist(it.toString(), "歌手 $it", "https://artist/$it")
    }

    private class FakeFollowingRepository(private val artists: List<FollowedArtist>) : ContentCatalogRepository {
        var nextLoadFailure: ContentFailure? = null
        val unfollowedIds = mutableListOf<String>()

        override suspend fun loadFollowedArtists(): CollectionLoadResult<List<FollowedArtist>> = nextLoadFailure?.let {
            nextLoadFailure = null
            CollectionLoadResult.Failed(it)
        } ?: CollectionLoadResult.Available(artists)

        override suspend fun setArtistFollowed(artistId: String, followed: Boolean): CollectionLoadResult<Boolean> {
            check(!followed)
            unfollowedIds += artistId
            return CollectionLoadResult.Available(false)
        }

        override suspend fun loadBanners(): CollectionLoadResult<List<Banner>> = error("unused")
        override suspend fun loadPlaylistCategories(): CollectionLoadResult<List<PlaylistCategory>> = error("unused")
        override suspend fun loadCategoryPlaylists(
            categoryId: Int,
            page: Int,
            pageSize: Int,
        ): CollectionLoadResult<List<PlaylistSummary>> = error("unused")
        override suspend fun loadNewAlbums(page: Int, pageSize: Int): CollectionLoadResult<List<Album>> =
            error("unused")
        override suspend fun loadNewSongs(page: Int, pageSize: Int): CollectionLoadResult<SongPage> = error("unused")
        override suspend fun loadAlbumSongs(
            albumId: String,
            page: Int,
            pageSize: Int,
        ): CollectionLoadResult<CatalogSongPage> = error("unused")
        override suspend fun loadArtistDetail(artistId: String): CollectionLoadResult<ArtistInfo?> = error("unused")
        override suspend fun loadArtistSongs(
            artistId: String,
            page: Int,
            pageSize: Int,
            newestFirst: Boolean,
        ): CollectionLoadResult<ArtistSongsPage> = error("unused")
    }
}

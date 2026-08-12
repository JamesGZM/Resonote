package com.resonote.feature.album.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.ContentCatalogRepository
import com.resonote.core.model.Album
import com.resonote.core.model.ArtistInfo
import com.resonote.core.model.ArtistSongsPage
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.Banner
import com.resonote.core.model.CatalogSongPage
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaylistCategory
import com.resonote.core.model.PlaylistSummary
import com.resonote.feature.album.api.AlbumNavKey
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
class AlbumViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun firstPageCombinesNavigationMetadataWithRealTotalAndSongs() = runTest(dispatcher) {
        val repository = FakeCatalogRepository()
        val viewModel = AlbumViewModel(repository)

        viewModel.load(key())
        advanceUntilIdle()

        val state = viewModel.uiState.value as AlbumUiState.Content
        assertThat(state.metadata.title).isEqualTo("夜航日志")
        assertThat(state.metadata.artist).isEqualTo("林澈")
        assertThat(state.metadata.songCount).isEqualTo(3)
        assertThat(state.songs.map { it.hash }).containsExactly("song-1", "song-2").inOrder()
        assertThat(repository.requests).containsExactly("album" to 1)
    }

    @Test
    fun deepLinkWithoutHintsDerivesTruthFromFirstSong() = runTest(dispatcher) {
        val viewModel = AlbumViewModel(FakeCatalogRepository())

        viewModel.load(AlbumNavKey("album"))
        advanceUntilIdle()

        val metadata = (viewModel.uiState.value as AlbumUiState.Content).metadata
        assertThat(metadata.title).isEqualTo("响应中的专辑")
        assertThat(metadata.artist).isEqualTo("响应歌手")
        assertThat(metadata.coverUrl).isEqualTo("https://cover")
    }

    @Test
    fun emptyAlbumRetainsNavigationMetadata() = runTest(dispatcher) {
        val viewModel = AlbumViewModel(FakeCatalogRepository(emptyFirstPage = true))

        viewModel.load(key())
        advanceUntilIdle()

        val state = viewModel.uiState.value as AlbumUiState.Empty
        assertThat(state.metadata.title).isEqualTo("夜航日志")
        assertThat(state.metadata.songCount).isEqualTo(0)
    }

    @Test
    fun firstPageFailureKeepsHintTitleForErrorRecovery() = runTest(dispatcher) {
        val repository = FakeCatalogRepository(failFirstPage = true)
        val viewModel = AlbumViewModel(repository)

        viewModel.load(key())
        advanceUntilIdle()

        assertThat(viewModel.uiState.value)
            .isEqualTo(AlbumUiState.Error(ContentFailure.Network, "夜航日志"))
    }

    @Test
    fun nextPageAppendsUniqueSongs() = runTest(dispatcher) {
        val viewModel = AlbumViewModel(FakeCatalogRepository())
        viewModel.load(key())
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value as AlbumUiState.Content
        assertThat(state.songs.map { it.hash }).containsExactly("song-1", "song-2", "song-3").inOrder()
        assertThat(state.page).isEqualTo(2)
        assertThat(state.hasMore).isFalse()
    }

    @Test
    fun nextPageFailureKeepsContentAndExposesFooterRecovery() = runTest(dispatcher) {
        val viewModel = AlbumViewModel(FakeCatalogRepository(failSecondPage = true))
        viewModel.load(key())
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value as AlbumUiState.Content
        assertThat(state.songs.map { it.hash }).containsExactly("song-1", "song-2").inOrder()
        assertThat(state.loadMoreFailure).isEqualTo(ContentFailure.Network)
        assertThat(state.isLoadingMore).isFalse()
    }

    private class FakeCatalogRepository(
        private val emptyFirstPage: Boolean = false,
        private val failFirstPage: Boolean = false,
        private val failSecondPage: Boolean = false,
    ) : ContentCatalogRepository {
        val requests = mutableListOf<Pair<String, Int>>()

        override suspend fun loadAlbumSongs(
            albumId: String,
            page: Int,
            pageSize: Int,
        ): CollectionLoadResult<CatalogSongPage> {
            requests += albumId to page
            if (page == 1 && failFirstPage) return CollectionLoadResult.Failed(ContentFailure.Network)
            if (page == 2 && failSecondPage) return CollectionLoadResult.Failed(ContentFailure.Network)
            if (emptyFirstPage) {
                return CollectionLoadResult.Available(CatalogSongPage(emptyList(), 1, 0, false))
            }
            return CollectionLoadResult.Available(
                if (page == 1) {
                    CatalogSongPage(listOf(song("song-1"), song("song-2")), 1, 3, true)
                } else {
                    CatalogSongPage(listOf(song("song-2"), song("song-3")), 2, 3, false)
                },
            )
        }

        override suspend fun loadBanners(): CollectionLoadResult<List<Banner>> = unused()
        override suspend fun loadPlaylistCategories(): CollectionLoadResult<List<PlaylistCategory>> = unused()
        override suspend fun loadCategoryPlaylists(categoryId: Int, page: Int, pageSize: Int): CollectionLoadResult<List<PlaylistSummary>> = unused()
        override suspend fun loadNewAlbums(page: Int, pageSize: Int): CollectionLoadResult<List<Album>> = unused()
        override suspend fun loadNewSongs(page: Int, pageSize: Int): CollectionLoadResult<com.resonote.core.model.SongPage> = unused()
        override suspend fun loadArtistDetail(artistId: String): CollectionLoadResult<ArtistInfo?> = unused()
        override suspend fun loadArtistSongs(artistId: String, page: Int, pageSize: Int, newestFirst: Boolean): CollectionLoadResult<ArtistSongsPage> = unused()
    }

    private companion object {
        fun key() = AlbumNavKey(
            albumId = "album",
            name = "夜航日志",
            artist = "林澈",
            coverUrl = "https://hint-cover",
            publishDate = "2026-08-13 00:00:00",
            songCount = 8,
        )

        fun song(id: String) = OnlineSong(
            hash = id,
            title = id,
            artist = "响应歌手",
            coverUrl = "https://cover",
            albumId = "album",
            albumAudioId = "audio-$id",
            durationMillis = 180_000,
            quality = AudioQuality.Lossless,
            vip = false,
            albumTitle = "响应中的专辑",
        )

        fun <T> unused(): T = error("unused")
    }
}

package com.resonote.feature.artist.impl

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
import com.resonote.feature.artist.api.ArtistNavKey
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
class ArtistViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun firstPagePublishesRealProfileAndPopularSongs() = runTest(dispatcher) {
        val repository = FakeCatalogRepository()
        val viewModel = ArtistViewModel(repository)

        viewModel.load(key())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.profile?.name).isEqualTo("林澈与潮汐记忆")
        assertThat(state.profile?.intro).isEqualTo("来自响应的歌手简介")
        assertThat(state.profile?.fansCount).isEqualTo(128_000L)
        assertThat((state.popular as ArtistPageUiState.Content).songs.map { it.hash })
            .containsExactly("hot-1", "hot-2").inOrder()
        assertThat(repository.requests).containsExactly(Request(page = 1, newestFirst = false))
    }

    @Test
    fun selectingLatestLoadsIndependentPageAndPreservesPopular() = runTest(dispatcher) {
        val repository = FakeCatalogRepository()
        val viewModel = ArtistViewModel(repository)
        viewModel.load(key())
        advanceUntilIdle()

        viewModel.selectSection(ArtistSongSection.LATEST)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.selectedSection).isEqualTo(ArtistSongSection.LATEST)
        assertThat((state.popular as ArtistPageUiState.Content).songs.map { it.hash })
            .containsExactly("hot-1", "hot-2").inOrder()
        assertThat((state.latest as ArtistPageUiState.Content).songs.map { it.hash })
            .containsExactly("new-1", "new-2").inOrder()
        assertThat(repository.requests).containsExactly(
            Request(1, false),
            Request(1, true),
        ).inOrder()
    }

    @Test
    fun returningToCachedSectionDoesNotReload() = runTest(dispatcher) {
        val repository = FakeCatalogRepository()
        val viewModel = ArtistViewModel(repository)
        viewModel.load(key())
        advanceUntilIdle()
        viewModel.selectSection(ArtistSongSection.LATEST)
        advanceUntilIdle()

        viewModel.selectSection(ArtistSongSection.POPULAR)
        advanceUntilIdle()

        assertThat(repository.requests).containsExactly(
            Request(1, false),
            Request(1, true),
        ).inOrder()
    }

    @Test
    fun selectedSectionPaginationAppendsUniqueSongs() = runTest(dispatcher) {
        val viewModel = ArtistViewModel(FakeCatalogRepository())
        viewModel.load(key())
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        val page = viewModel.uiState.value.popular as ArtistPageUiState.Content
        assertThat(page.songs.map { it.hash }).containsExactly("hot-1", "hot-2", "hot-3").inOrder()
        assertThat(page.page).isEqualTo(2)
        assertThat(page.hasMore).isFalse()
    }

    @Test
    fun latestFailureDoesNotReplaceLoadedPopularSection() = runTest(dispatcher) {
        val viewModel = ArtistViewModel(FakeCatalogRepository(failLatestFirstPage = true))
        viewModel.load(key())
        advanceUntilIdle()

        viewModel.selectSection(ArtistSongSection.LATEST)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.latest).isEqualTo(ArtistPageUiState.Error(ContentFailure.Network))
        assertThat(state.popular).isInstanceOf(ArtistPageUiState.Content::class.java)
    }

    @Test
    fun loadMoreFailureKeepsLoadedSongsAndOffersFooterRecovery() = runTest(dispatcher) {
        val viewModel = ArtistViewModel(FakeCatalogRepository(failPopularSecondPage = true))
        viewModel.load(key())
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        val page = viewModel.uiState.value.popular as ArtistPageUiState.Content
        assertThat(page.songs.map { it.hash }).containsExactly("hot-1", "hot-2").inOrder()
        assertThat(page.loadMoreFailure).isEqualTo(ContentFailure.Network)
        assertThat(page.isLoadingMore).isFalse()
    }

    @Test
    fun missingDetailKeepsSearchHints() = runTest(dispatcher) {
        val viewModel = ArtistViewModel(FakeCatalogRepository(includeInfo = false))

        viewModel.load(key())
        advanceUntilIdle()

        val profile = viewModel.uiState.value.profile
        assertThat(profile?.name).isEqualTo("搜索结果歌手")
        assertThat(profile?.songCount).isEqualTo(36)
        assertThat(profile?.albumCount).isEqualTo(4)
    }

    private data class Request(val page: Int, val newestFirst: Boolean)

    private class FakeCatalogRepository(
        private val includeInfo: Boolean = true,
        private val failLatestFirstPage: Boolean = false,
        private val failPopularSecondPage: Boolean = false,
    ) : ContentCatalogRepository {
        val requests = mutableListOf<Request>()

        override suspend fun loadArtistSongs(
            artistId: String,
            page: Int,
            pageSize: Int,
            newestFirst: Boolean,
        ): CollectionLoadResult<ArtistSongsPage> {
            requests += Request(page, newestFirst)
            if (newestFirst && page == 1 && failLatestFirstPage) {
                return CollectionLoadResult.Failed(ContentFailure.Network)
            }
            if (!newestFirst && page == 2 && failPopularSecondPage) {
                return CollectionLoadResult.Failed(ContentFailure.Network)
            }
            val songs = when {
                newestFirst && page == 1 -> listOf(song("new-1"), song("new-2"))
                newestFirst -> listOf(song("new-2"), song("new-3"))
                page == 1 -> listOf(song("hot-1"), song("hot-2"))
                else -> listOf(song("hot-2"), song("hot-3"))
            }
            return CollectionLoadResult.Available(
                ArtistSongsPage(
                    info = if (page == 1 && includeInfo) info() else null,
                    songs = songs,
                    page = page,
                    total = 36,
                    hasMore = page == 1,
                ),
            )
        }

        override suspend fun loadBanners(): CollectionLoadResult<List<Banner>> = unused()
        override suspend fun loadPlaylistCategories(): CollectionLoadResult<List<PlaylistCategory>> = unused()
        override suspend fun loadCategoryPlaylists(categoryId: Int, page: Int, pageSize: Int): CollectionLoadResult<List<PlaylistSummary>> = unused()
        override suspend fun loadNewAlbums(page: Int, pageSize: Int): CollectionLoadResult<List<Album>> = unused()
        override suspend fun loadAlbumSongs(albumId: String, page: Int, pageSize: Int): CollectionLoadResult<CatalogSongPage> = unused()
        override suspend fun loadArtistDetail(artistId: String): CollectionLoadResult<ArtistInfo?> = unused()
    }

    private companion object {
        fun key() = ArtistNavKey(
            artistId = "artist",
            name = "搜索结果歌手",
            avatarUrl = "https://search-avatar",
            songCount = 36,
            albumCount = 4,
        )

        fun info() = ArtistInfo(
            name = "林澈与潮汐记忆",
            avatarUrl = "https://real-avatar",
            intro = "来自响应的歌手简介",
            songCount = 36,
            albumCount = 4,
            mvCount = 7,
            fansCount = 128_000,
        )

        fun song(id: String) = OnlineSong(
            hash = id,
            title = id,
            artist = "林澈",
            coverUrl = null,
            albumId = "album",
            albumAudioId = "audio-$id",
            durationMillis = 180_000,
            quality = AudioQuality.Lossless,
            vip = false,
        )

        fun <T> unused(): T = error("unused")
    }
}

package com.resonote.feature.discover.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.ContentCatalogRepository
import com.resonote.core.data.RankingRepository
import com.resonote.core.model.Album
import com.resonote.core.model.AlbumRegion
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
import com.resonote.core.model.Ranking
import com.resonote.core.model.SongPage
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun initializationLoadsCategoriesAndRecommendedPlaylistsIndependently() = runTest(dispatcher) {
        val catalog = FakeCatalogRepository()
        val viewModel = DiscoverViewModel(catalog, FakeRankingRepository())

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat((state.categories as DiscoverLoadState.Content).value.map { it.name })
            .containsExactly("风格", "场景").inOrder()
        assertThat((state.playlists as DiscoverPageState.Content).items.map { it.id })
            .containsExactly("playlist-0")
        assertThat(catalog.playlistRequests).containsExactly(Triple(0, 1, 30))
    }

    @Test
    fun categoryFailureDoesNotDiscardRecommendedPlaylists() = runTest(dispatcher) {
        val viewModel = DiscoverViewModel(
            FakeCatalogRepository(failCategories = true),
            FakeRankingRepository(),
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.categories).isEqualTo(DiscoverLoadState.Error(ContentFailure.Network))
        assertThat(state.playlists).isInstanceOf(DiscoverPageState.Content::class.java)
    }

    @Test
    fun selectingParentLoadsItsFirstChildCategory() = runTest(dispatcher) {
        val catalog = FakeCatalogRepository()
        val viewModel = DiscoverViewModel(catalog, FakeRankingRepository())
        advanceUntilIdle()

        viewModel.selectPlaylistParent(10)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.selectedParentCategoryId).isEqualTo(10)
        assertThat(state.selectedPlaylistCategoryId).isEqualTo(11)
        assertThat(catalog.playlistRequests).containsExactly(
            Triple(0, 1, 30),
            Triple(11, 1, 30),
        ).inOrder()
    }

    @Test
    fun rapidParentAndChildSelectionOnlyLoadsFinalCategory() = runTest(dispatcher) {
        val catalog = FakeCatalogRepository()
        val viewModel = DiscoverViewModel(catalog, FakeRankingRepository())
        advanceUntilIdle()

        viewModel.selectPlaylistParent(10)
        viewModel.selectPlaylistCategory(12)
        advanceUntilIdle()

        assertThat(catalog.playlistRequests).containsExactly(
            Triple(0, 1, 30),
            Triple(12, 1, 30),
        ).inOrder()
    }

    @Test
    fun repeatedSelectedFiltersDoNotReloadPlaylists() = runTest(dispatcher) {
        val catalog = FakeCatalogRepository()
        val viewModel = DiscoverViewModel(catalog, FakeRankingRepository())
        advanceUntilIdle()

        viewModel.selectPlaylistParent(10)
        advanceUntilIdle()
        viewModel.selectPlaylistParent(10)
        viewModel.selectPlaylistCategory(11)
        advanceUntilIdle()

        assertThat(catalog.playlistRequests).containsExactly(
            Triple(0, 1, 30),
            Triple(11, 1, 30),
        ).inOrder()
    }

    @Test
    fun secondarySectionsLoadLazilyAndRemainCached() = runTest(dispatcher) {
        val catalog = FakeCatalogRepository()
        val rankings = FakeRankingRepository()
        val viewModel = DiscoverViewModel(catalog, rankings)
        advanceUntilIdle()

        viewModel.selectSection(DiscoverSection.RANKINGS)
        advanceUntilIdle()
        viewModel.selectSection(DiscoverSection.ALBUMS)
        advanceUntilIdle()
        viewModel.selectSection(DiscoverSection.SONGS)
        advanceUntilIdle()
        viewModel.selectSection(DiscoverSection.RANKINGS)
        advanceUntilIdle()

        assertThat(rankings.listRequests).isEqualTo(1)
        assertThat(catalog.albumRequests).isEqualTo(1)
        assertThat(catalog.songRequests).containsExactly(1)
    }

    @Test
    fun newSongPaginationAppendsUniqueSongs() = runTest(dispatcher) {
        val viewModel = DiscoverViewModel(FakeCatalogRepository(), FakeRankingRepository())
        advanceUntilIdle()
        viewModel.selectSection(DiscoverSection.SONGS)
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        val page = viewModel.uiState.value.songs as DiscoverPageState.Content
        assertThat(page.items.map { it.hash }).containsExactly("new-1", "new-2", "new-3").inOrder()
        assertThat(page.page).isEqualTo(2)
        assertThat(page.hasMore).isFalse()
    }

    @Test
    fun playlistPaginationAppendsUniqueItems() = runTest(dispatcher) {
        val catalog = FakeCatalogRepository(paginatePlaylists = true)
        val viewModel = DiscoverViewModel(catalog, FakeRankingRepository())
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        val page = viewModel.uiState.value.playlists as DiscoverPageState.Content
        assertThat(page.items).hasSize(31)
        assertThat(page.items.map { it.id }).contains("playlist-30")
        assertThat(page.page).isEqualTo(2)
        assertThat(page.hasMore).isFalse()
        assertThat(catalog.playlistRequests).containsExactly(
            Triple(0, 1, 30),
            Triple(0, 2, 30),
        ).inOrder()
    }

    @Test
    fun playlistPaginationFailureStopsUntilExplicitRetry() = runTest(dispatcher) {
        val catalog = FakeCatalogRepository(paginatePlaylists = true)
        val viewModel = DiscoverViewModel(catalog, FakeRankingRepository())
        advanceUntilIdle()
        catalog.failingPlaylistPages += 2

        viewModel.loadMore()
        advanceUntilIdle()

        val failed = viewModel.uiState.value.playlists as DiscoverPageState.Content
        assertThat(failed.loadMoreFailure).isEqualTo(ContentFailure.Network)
        assertThat(catalog.playlistRequests).hasSize(2)

        catalog.failingPlaylistPages.clear()
        viewModel.loadMore()
        advanceUntilIdle()

        val recovered = viewModel.uiState.value.playlists as DiscoverPageState.Content
        assertThat(recovered.loadMoreFailure).isNull()
        assertThat(recovered.page).isEqualTo(2)
        assertThat(catalog.playlistRequests).hasSize(3)
    }

    @Test
    fun albumRegionSelectionDoesNotReloadAlbums() = runTest(dispatcher) {
        val catalog = FakeCatalogRepository()
        val viewModel = DiscoverViewModel(catalog, FakeRankingRepository())
        advanceUntilIdle()
        viewModel.selectSection(DiscoverSection.ALBUMS)
        advanceUntilIdle()

        viewModel.selectAlbumRegion(AlbumRegion.Japanese)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedAlbumRegion).isEqualTo(AlbumRegion.Japanese)
        assertThat(catalog.albumRequests).isEqualTo(1)
    }

    @Test
    fun refreshingCurrentPlaylistKeepsFiltersAndReplacesFirstPage() = runTest(dispatcher) {
        val catalog = FakeCatalogRepository()
        val viewModel = DiscoverViewModel(catalog, FakeRankingRepository())
        advanceUntilIdle()
        viewModel.selectPlaylistParent(10)
        advanceUntilIdle()

        viewModel.refreshCurrent()
        assertThat(viewModel.uiState.value.refreshingSection).isEqualTo(DiscoverSection.PLAYLISTS)
        assertThat(viewModel.uiState.value.playlists).isInstanceOf(DiscoverPageState.Content::class.java)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.refreshingSection).isNull()
        assertThat(state.selectedParentCategoryId).isEqualTo(10)
        assertThat(state.selectedPlaylistCategoryId).isEqualTo(11)
        assertThat(catalog.playlistRequests).containsExactly(
            Triple(0, 1, 30),
            Triple(11, 1, 30),
            Triple(11, 1, 30),
        ).inOrder()
    }

    @Test
    fun refreshFailureKeepsContentAndEmitsFailure() = runTest(dispatcher) {
        val catalog = FakeCatalogRepository()
        val viewModel = DiscoverViewModel(catalog, FakeRankingRepository())
        advanceUntilIdle()
        val original = viewModel.uiState.value.playlists
        val event = async(start = CoroutineStart.UNDISPATCHED) { viewModel.refreshFailures.first() }
        catalog.failPlaylists = true

        viewModel.refreshCurrent()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.playlists).isEqualTo(original)
        assertThat(viewModel.uiState.value.refreshingSection).isNull()
        assertThat(event.await()).isEqualTo(ContentFailure.Network)
    }

    private class FakeCatalogRepository(
        private val failCategories: Boolean = false,
        private val paginatePlaylists: Boolean = false,
    ) : ContentCatalogRepository {
        var failPlaylists = false
        val failingPlaylistPages = mutableSetOf<Int>()
        val playlistRequests = mutableListOf<Triple<Int, Int, Int>>()
        var albumRequests = 0
        val songRequests = mutableListOf<Int>()

        override suspend fun loadPlaylistCategories(): CollectionLoadResult<List<PlaylistCategory>> =
            if (failCategories) {
                CollectionLoadResult.Failed(ContentFailure.Network)
            } else {
                CollectionLoadResult.Available(categories())
            }

        override suspend fun loadCategoryPlaylists(
            categoryId: Int,
            page: Int,
            pageSize: Int,
        ): CollectionLoadResult<List<PlaylistSummary>> {
            playlistRequests += Triple(categoryId, page, pageSize)
            if (failPlaylists || page in failingPlaylistPages) {
                return CollectionLoadResult.Failed(ContentFailure.Network)
            }
            return CollectionLoadResult.Available(
                if (!paginatePlaylists) {
                    listOf(PlaylistSummary("playlist-$categoryId", "歌单 $categoryId", null, 12_000))
                } else if (page == 1) {
                    List(pageSize) { index ->
                        PlaylistSummary("playlist-$index", "歌单 $index", null, 12_000)
                    }
                } else {
                    listOf(
                        PlaylistSummary("playlist-29", "重复歌单", null, 12_000),
                        PlaylistSummary("playlist-30", "新歌单", null, 13_000),
                    )
                },
            )
        }

        override suspend fun loadNewAlbums(page: Int, pageSize: Int): CollectionLoadResult<List<Album>> {
            albumRequests += 1
            return CollectionLoadResult.Available(
                listOf(album("album-cn", AlbumRegion.Chinese), album("album-jp", AlbumRegion.Japanese)),
            )
        }

        override suspend fun loadNewSongs(page: Int, pageSize: Int): CollectionLoadResult<SongPage> {
            songRequests += page
            return CollectionLoadResult.Available(
                if (page == 1) {
                    SongPage(listOf(song("new-1"), song("new-2")), 1, null, true)
                } else {
                    SongPage(listOf(song("new-2"), song("new-3")), 2, null, false)
                },
            )
        }

        override suspend fun loadBanners(): CollectionLoadResult<List<Banner>> = unused()
        override suspend fun loadAlbumSongs(
            albumId: String,
            page: Int,
            pageSize: Int,
        ): CollectionLoadResult<CatalogSongPage> = unused()
        override suspend fun loadArtistDetail(artistId: String): CollectionLoadResult<ArtistInfo?> = unused()
        override suspend fun loadArtistSongs(
            artistId: String,
            page: Int,
            pageSize: Int,
            newestFirst: Boolean,
        ): CollectionLoadResult<ArtistSongsPage> = unused()
    }

    private class FakeRankingRepository : RankingRepository {
        var listRequests = 0

        override suspend fun loadRankings(): CollectionLoadResult<List<Ranking>> {
            listRequests += 1
            return CollectionLoadResult.Available(listOf(Ranking("ranking", "热歌榜", null)))
        }

        override suspend fun loadSongs(rankId: String, page: Int, pageSize: Int): CollectionLoadResult<SongPage> =
            unused()
    }

    private companion object {
        fun categories() = listOf(
            PlaylistCategory(
                10,
                "风格",
                listOf(PlaylistCategory(11, "流行", emptyList()), PlaylistCategory(12, "摇滚", emptyList())),
            ),
            PlaylistCategory(20, "场景", listOf(PlaylistCategory(21, "通勤", emptyList()))),
        )

        fun album(id: String, region: AlbumRegion) = Album(id, "新碟 $id", "林澈", null, "2026-08-13", 12, region)

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

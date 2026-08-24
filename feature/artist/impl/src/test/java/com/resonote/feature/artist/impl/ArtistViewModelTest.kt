package com.resonote.feature.artist.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.ContentCatalogRepository
import com.resonote.core.model.Album
import com.resonote.core.model.ArtistAlbum
import com.resonote.core.model.ArtistAlbumsPage
import com.resonote.core.model.ArtistInfo
import com.resonote.core.model.ArtistSongsPage
import com.resonote.core.model.ArtistVideo
import com.resonote.core.model.ArtistVideosPage
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.Banner
import com.resonote.core.model.CatalogSongPage
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaylistCategory
import com.resonote.core.model.PlaylistSummary
import com.resonote.feature.artist.api.ArtistNavKey
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
class ArtistViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun firstPagePublishesProfileAndPopularSongs() = runTest(dispatcher) {
        val repository = FakeCatalogRepository()
        val viewModel = ArtistViewModel(repository)

        viewModel.load(key())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.profile?.name).isEqualTo("林澈与潮汐记忆")
        assertThat(state.profile?.intro).isEqualTo("来自响应的歌手简介")
        assertThat(state.popularSongs.songIds()).containsExactly("hot-1", "hot-2").inOrder()
        assertThat(repository.requests).containsExactly(Request.Songs(1, false))
    }

    @Test
    fun newNavigationSessionReloadsSameArtist() = runTest(dispatcher) {
        val repository = FakeCatalogRepository()
        val viewModel = ArtistViewModel(repository)

        viewModel.load(key(sessionId = 1))
        advanceUntilIdle()
        viewModel.selectSection(ArtistSection.ALBUMS)
        advanceUntilIdle()
        viewModel.load(key(sessionId = 2))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedSection).isEqualTo(ArtistSection.SONGS)
        assertThat(repository.requests).containsExactly(
            Request.Songs(1, false),
            Request.Albums(1, false),
            Request.Songs(1, false),
        ).inOrder()
    }

    @Test
    fun latestSortLoadsIndependentPageAndReturningUsesCache() = runTest(dispatcher) {
        val repository = FakeCatalogRepository()
        val viewModel = ArtistViewModel(repository)
        viewModel.load(key())
        advanceUntilIdle()

        viewModel.selectSort(ArtistSort.LATEST)
        advanceUntilIdle()
        viewModel.selectSort(ArtistSort.POPULAR)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.popularSongs.songIds()).containsExactly("hot-1", "hot-2").inOrder()
        assertThat(state.latestSongs.songIds()).containsExactly("new-1", "new-2").inOrder()
        assertThat(repository.requests).containsExactly(
            Request.Songs(1, false),
            Request.Songs(1, true),
        ).inOrder()
    }

    @Test
    fun albumsAndVideosLoadAsIndependentSections() = runTest(dispatcher) {
        val repository = FakeCatalogRepository()
        val viewModel = ArtistViewModel(repository)
        viewModel.load(key())
        advanceUntilIdle()

        viewModel.selectSection(ArtistSection.ALBUMS)
        advanceUntilIdle()
        viewModel.selectSort(ArtistSort.LATEST)
        advanceUntilIdle()
        viewModel.selectSection(ArtistSection.MVS)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.popularAlbums.albumIds()).containsExactly("album-hot")
        assertThat(state.latestAlbums.albumIds()).containsExactly("album-new")
        assertThat(state.videos.videoIds()).containsExactly("mv-1")
        assertThat(repository.requests).containsAtLeast(
            Request.Albums(1, false),
            Request.Albums(1, true),
            Request.Videos(1),
        )
    }

    @Test
    fun loadMoreAppendsUniqueItems() = runTest(dispatcher) {
        val viewModel = ArtistViewModel(FakeCatalogRepository())
        viewModel.load(key())
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        val page = viewModel.uiState.value.popularSongs as ArtistPageUiState.Content
        assertThat(page.songIds()).containsExactly("hot-1", "hot-2", "hot-3").inOrder()
        assertThat(page.page).isEqualTo(2)
        assertThat(page.hasMore).isFalse()
    }

    @Test
    fun refreshFailureKeepsCurrentItemsUntilAcknowledged() = runTest(dispatcher) {
        val viewModel = ArtistViewModel(FakeCatalogRepository(failPopularRefresh = true))
        viewModel.load(key())
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        val failed = viewModel.uiState.value.popularSongs as ArtistPageUiState.Content
        assertThat(failed.songIds()).containsExactly("hot-1", "hot-2").inOrder()
        assertThat(failed.refreshFailure).isEqualTo(ContentFailure.Network)

        viewModel.acknowledgeRefreshFailure()
        assertThat((viewModel.uiState.value.popularSongs as ArtistPageUiState.Content).refreshFailure).isNull()
    }

    @Test
    fun followStateLoadsAndTogglePersists() = runTest(dispatcher) {
        val repository = FakeCatalogRepository(initiallyFollowed = false)
        val viewModel = ArtistViewModel(repository)

        viewModel.load(key())
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.follow).isEqualTo(ArtistFollowUiState.Available(false))

        viewModel.toggleFollow()
        advanceUntilIdle()

        assertThat(repository.followTargets).containsExactly(true)
        assertThat(viewModel.uiState.value.follow).isEqualTo(ArtistFollowUiState.Available(true))
    }

    @Test
    fun unauthenticatedFollowRequestsLogin() = runTest(dispatcher) {
        val repository = FakeCatalogRepository(followLoadFailure = ContentFailure.AuthenticationRequired)
        val viewModel = ArtistViewModel(repository)
        viewModel.load(key())
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.follow).isEqualTo(ArtistFollowUiState.AuthenticationRequired)

        val loginRequest = backgroundScope.async(start = CoroutineStart.UNDISPATCHED) {
            viewModel.loginRequests.first()
        }
        viewModel.toggleFollow()

        assertThat(loginRequest.await()).isEqualTo(Unit)
    }

    private sealed interface Request {
        data class Songs(val page: Int, val newestFirst: Boolean) : Request
        data class Albums(val page: Int, val newestFirst: Boolean) : Request
        data class Videos(val page: Int) : Request
    }

    private class FakeCatalogRepository(
        private val failPopularRefresh: Boolean = false,
        private val initiallyFollowed: Boolean = false,
        private val followLoadFailure: ContentFailure? = null,
    ) : ContentCatalogRepository {
        val requests = mutableListOf<Request>()
        val followTargets = mutableListOf<Boolean>()

        override suspend fun loadArtistFollowed(artistId: String): CollectionLoadResult<Boolean> =
            followLoadFailure?.let { CollectionLoadResult.Failed(it) }
                ?: CollectionLoadResult.Available(initiallyFollowed)

        override suspend fun setArtistFollowed(artistId: String, followed: Boolean): CollectionLoadResult<Boolean> {
            followTargets += followed
            return CollectionLoadResult.Available(followed)
        }

        override suspend fun loadArtistSongs(
            artistId: String,
            page: Int,
            pageSize: Int,
            newestFirst: Boolean,
        ): CollectionLoadResult<ArtistSongsPage> {
            requests += Request.Songs(page, newestFirst)
            if (!newestFirst && page == 1 && failPopularRefresh && requests.count { it is Request.Songs } > 1) {
                return CollectionLoadResult.Failed(ContentFailure.Network)
            }
            val songs = when {
                newestFirst -> listOf(song("new-1"), song("new-2"))
                page == 1 -> listOf(song("hot-1"), song("hot-2"))
                else -> listOf(song("hot-2"), song("hot-3"))
            }
            return CollectionLoadResult.Available(
                ArtistSongsPage(
                    info = if (page == 1) info() else null,
                    songs = songs,
                    page = page,
                    total = 36,
                    hasMore = !newestFirst && page == 1,
                ),
            )
        }

        override suspend fun loadArtistAlbums(
            artistId: String,
            page: Int,
            pageSize: Int,
            newestFirst: Boolean,
        ): CollectionLoadResult<ArtistAlbumsPage> {
            requests += Request.Albums(page, newestFirst)
            return CollectionLoadResult.Available(
                ArtistAlbumsPage(
                    albums = listOf(album(if (newestFirst) "album-new" else "album-hot")),
                    page = page,
                    total = 4,
                    hasMore = false,
                ),
            )
        }

        override suspend fun loadArtistVideos(
            artistId: String,
            page: Int,
            pageSize: Int,
        ): CollectionLoadResult<ArtistVideosPage> {
            requests += Request.Videos(page)
            return CollectionLoadResult.Available(
                ArtistVideosPage(
                    videos = listOf(ArtistVideo("mv-1", "潮汐 MV", "林澈", null, 180_000)),
                    page = page,
                    total = 7,
                    hasMore = false,
                ),
            )
        }

        override suspend fun loadBanners(): CollectionLoadResult<List<Banner>> = unused()
        override suspend fun loadPlaylistCategories(): CollectionLoadResult<List<PlaylistCategory>> = unused()
        override suspend fun loadCategoryPlaylists(
            categoryId: Int,
            page: Int,
            pageSize: Int,
        ): CollectionLoadResult<List<PlaylistSummary>> = unused()
        override suspend fun loadNewAlbums(page: Int, pageSize: Int): CollectionLoadResult<List<Album>> = unused()
        override suspend fun loadNewSongs(
            page: Int,
            pageSize: Int,
        ): CollectionLoadResult<com.resonote.core.model.SongPage> = unused()
        override suspend fun loadAlbumSongs(
            albumId: String,
            page: Int,
            pageSize: Int,
        ): CollectionLoadResult<CatalogSongPage> = unused()
        override suspend fun loadArtistDetail(artistId: String): CollectionLoadResult<ArtistInfo?> = unused()
    }

    private companion object {
        fun ArtistPageUiState.songIds() = (this as ArtistPageUiState.Content).songIds()

        fun ArtistPageUiState.Content.songIds() = items.mapNotNull { (it as? ArtistItem.Song)?.value?.hash }

        fun ArtistPageUiState.albumIds() = (this as ArtistPageUiState.Content).items
            .mapNotNull { (it as? ArtistItem.Album)?.value?.id }

        fun ArtistPageUiState.videoIds() = (this as ArtistPageUiState.Content).items
            .mapNotNull { (it as? ArtistItem.Video)?.value?.hash }

        fun key(sessionId: Long = 0) = ArtistNavKey(
            artistId = "artist",
            name = "搜索结果歌手",
            avatarUrl = "https://search-avatar",
            songCount = 36,
            albumCount = 4,
            sessionId = sessionId,
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

        fun album(id: String) = ArtistAlbum(id, id, "林澈", null, "2026-08-23", 10)

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

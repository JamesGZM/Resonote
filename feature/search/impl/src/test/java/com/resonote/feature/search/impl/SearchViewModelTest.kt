package com.resonote.feature.search.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.SearchHistoryRepository
import com.resonote.core.data.SearchRepository
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ComplexSearchResult
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.SearchAlbum
import com.resonote.core.model.SearchArtist
import com.resonote.core.model.SearchKeyword
import com.resonote.core.model.SearchMv
import com.resonote.core.model.SearchPage
import com.resonote.core.model.SearchPlaylist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun categoriesFollowLegacyMobileOrder() {
        assertThat(SearchCategory.entries).containsExactly(
            SearchCategory.ALL,
            SearchCategory.SONGS,
            SearchCategory.PLAYLISTS,
            SearchCategory.ALBUMS,
            SearchCategory.MVS,
            SearchCategory.ARTISTS,
        ).inOrder()
    }

    @Test
    fun aNewSearchSessionResetsTransientStateButReturningToTheSameSessionDoesNot() = runTest(dispatcher) {
        val viewModel = viewModel(FakeSearchRepository())
        advanceUntilIdle()

        viewModel.initialize(sessionId = 1, initialQuery = "林澈")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.result).isInstanceOf(SearchResultUiState.Content::class.java)

        viewModel.initialize(sessionId = 1, initialQuery = "")
        assertThat(viewModel.uiState.value.query).isEqualTo("林澈")

        viewModel.initialize(sessionId = 2, initialQuery = "")
        assertThat(viewModel.uiState.value.query).isEmpty()
        assertThat(viewModel.uiState.value.selectedCategory).isEqualTo(SearchCategory.ALL)
        assertThat(viewModel.uiState.value.result).isEqualTo(SearchResultUiState.Idle)
    }

    @Test
    fun hotKeywordFailureDoesNotBlockManualSearch() = runTest(dispatcher) {
        val repository = FakeSearchRepository(hotResult = CollectionLoadResult.Failed(ContentFailure.Network))
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.updateQuery("周杰伦")
        viewModel.submit()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.hotKeywords).isEmpty()
        assertThat(viewModel.uiState.value.result).isInstanceOf(SearchResultUiState.Content::class.java)
        assertThat(repository.complexQueries).containsExactly("周杰伦")
    }

    @Test
    fun suggestionsAreDebouncedAndStaleValuesAreNotPublished() = runTest(dispatcher) {
        val repository = FakeSearchRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.updateQuery("周")
        advanceTimeBy(150)
        viewModel.updateQuery("周杰伦")
        advanceTimeBy(301)
        advanceUntilIdle()

        assertThat(repository.suggestionQueries).containsExactly("周杰伦")
        assertThat(viewModel.uiState.value.suggestions).containsExactly("周杰伦 建议")
    }

    @Test
    fun emptyAggregateBecomesExplicitEmptyState() = runTest(dispatcher) {
        val repository = FakeSearchRepository(complexResult = CollectionLoadResult.Available(emptyResult()))
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.updateQuery("不存在")
        viewModel.submit()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.result)
            .isEqualTo(SearchResultUiState.Empty("不存在", SearchCategory.ALL))
    }

    @Test
    fun retryRepeatsFailedQueryInSelectedCategory() = runTest(dispatcher) {
        val repository = FakeSearchRepository(complexResult = CollectionLoadResult.Failed(ContentFailure.Network))
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.updateQuery("离线")
        viewModel.submit()
        advanceUntilIdle()
        viewModel.retry()
        advanceUntilIdle()

        assertThat(repository.complexQueries).containsExactly("离线", "离线").inOrder()
    }

    @Test
    fun editingSubmittedQueryStopsPresentingOldResultsAsCurrent() = runTest(dispatcher) {
        val viewModel = viewModel(FakeSearchRepository())
        advanceUntilIdle()
        viewModel.updateQuery("旧查询")
        viewModel.submit()
        advanceUntilIdle()

        viewModel.updateQuery("新查询")

        assertThat(viewModel.uiState.value.result).isEqualTo(SearchResultUiState.Idle)
        assertThat(viewModel.uiState.value.query).isEqualTo("新查询")
    }

    @Test
    fun submittedQueryIsPersistedInRecentUniqueOrder() = runTest(dispatcher) {
        val history = FakeSearchHistoryRepository(listOf("旧记录"))
        val viewModel = SearchViewModel(FakeSearchRepository(), history)
        advanceUntilIdle()

        viewModel.submit("新记录")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.history).containsExactly("新记录", "旧记录").inOrder()
    }

    @Test
    fun categorySwitchUsesTypedApiAndPaginationAppendsUniqueItems() = runTest(dispatcher) {
        val repository = FakeSearchRepository()
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.submit("歌曲")
        advanceUntilIdle()

        viewModel.selectCategory(SearchCategory.SONGS)
        advanceUntilIdle()

        var page = (viewModel.uiState.value.result as SearchResultUiState.Content).value as SearchContentUiState.Page
        assertThat(repository.songRequests).containsExactly("歌曲" to 1)
        assertThat(page.items.map { it.stableId }).containsExactly("song-1", "song-2").inOrder()

        viewModel.loadMore()
        advanceUntilIdle()

        page = (viewModel.uiState.value.result as SearchResultUiState.Content).value as SearchContentUiState.Page
        assertThat(repository.songRequests).containsExactly("歌曲" to 1, "歌曲" to 2).inOrder()
        assertThat(page.items.map { it.stableId }).containsExactly("song-1", "song-2", "song-3").inOrder()
        assertThat(page.hasMore).isFalse()
    }

    @Test
    fun loadMoreFailureKeepsLoadedPageAndExposesFooterRetry() = runTest(dispatcher) {
        val repository = FakeSearchRepository(failSecondSongPage = true)
        val viewModel = viewModel(repository)
        advanceUntilIdle()
        viewModel.submit("歌曲")
        advanceUntilIdle()
        viewModel.selectCategory(SearchCategory.SONGS)
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        val page = (viewModel.uiState.value.result as SearchResultUiState.Content).value as SearchContentUiState.Page
        assertThat(page.items.map { it.stableId }).containsExactly("song-1", "song-2").inOrder()
        assertThat(page.loadMoreFailure).isEqualTo(ContentFailure.Network)
        assertThat(page.isLoadingMore).isFalse()
    }

    private fun viewModel(repository: FakeSearchRepository) = SearchViewModel(repository, FakeSearchHistoryRepository())

    private class FakeSearchHistoryRepository(initial: List<String> = emptyList()) : SearchHistoryRepository {
        override val queries = MutableStateFlow(initial)

        override suspend fun record(query: String) {
            queries.update { listOf(query.trim()) + it.filterNot { existing -> existing == query.trim() } }
        }

        override suspend fun remove(query: String) {
            queries.update { it.filterNot { existing -> existing == query } }
        }

        override suspend fun clear() {
            queries.value = emptyList()
        }
    }

    private class FakeSearchRepository(
        private val hotResult: CollectionLoadResult<List<SearchKeyword>> =
            CollectionLoadResult.Available(listOf(SearchKeyword("热门", ""))),
        private val complexResult: CollectionLoadResult<ComplexSearchResult> =
            CollectionLoadResult.Available(result()),
        private val failSecondSongPage: Boolean = false,
    ) : SearchRepository {
        val suggestionQueries = mutableListOf<String>()
        val complexQueries = mutableListOf<String>()
        val songRequests = mutableListOf<Pair<String, Int>>()

        override suspend fun loadHotKeywords() = hotResult

        override suspend fun loadSuggestions(keywords: String): CollectionLoadResult<List<String>> {
            suggestionQueries += keywords
            return CollectionLoadResult.Available(listOf("$keywords 建议"))
        }

        override suspend fun searchComplex(keywords: String): CollectionLoadResult<ComplexSearchResult> {
            complexQueries += keywords
            return complexResult
        }

        override suspend fun searchSongs(
            keywords: String,
            page: Int,
            pageSize: Int,
        ): CollectionLoadResult<SearchPage<OnlineSong>> {
            songRequests += keywords to page
            if (page == 2 && failSecondSongPage) return CollectionLoadResult.Failed(ContentFailure.Network)
            return CollectionLoadResult.Available(
                if (page == 1) {
                    SearchPage(listOf(song("song-1"), song("song-2")), 1, 3, true)
                } else {
                    SearchPage(listOf(song("song-2"), song("song-3")), 2, 3, false)
                },
            )
        }

        override suspend fun searchPlaylists(keywords: String, page: Int, pageSize: Int) = emptyPage<SearchPlaylist>()
        override suspend fun searchAlbums(keywords: String, page: Int, pageSize: Int) = emptyPage<SearchAlbum>()
        override suspend fun searchArtists(keywords: String, page: Int, pageSize: Int) = emptyPage<SearchArtist>()
        override suspend fun searchMvs(keywords: String, page: Int, pageSize: Int) = emptyPage<SearchMv>()

        private fun <T> emptyPage(): CollectionLoadResult<SearchPage<T>> =
            CollectionLoadResult.Available(SearchPage(emptyList(), 1, 0, false))
    }

    private companion object {
        fun song(id: String) = OnlineSong(
            hash = id,
            title = id,
            artist = "artist",
            coverUrl = null,
            albumId = null,
            albumAudioId = null,
            durationMillis = 60_000,
            quality = AudioQuality.Standard,
            vip = false,
        )

        fun result() = ComplexSearchResult(
            artists = emptyList(), songs = emptyList(), songsTotal = 0,
            albums = emptyList(), albumsTotal = 0,
            playlists = listOf(SearchPlaylist("playlist", "歌单", null, null, 1, 1)), playlistsTotal = 1,
            mvs = emptyList(), mvsTotal = 0,
        )

        fun emptyResult() = ComplexSearchResult(
            artists = emptyList(), songs = emptyList(), songsTotal = 0,
            albums = emptyList(), albumsTotal = 0,
            playlists = emptyList(), playlistsTotal = 0,
            mvs = emptyList(), mvsTotal = 0,
        )
    }
}

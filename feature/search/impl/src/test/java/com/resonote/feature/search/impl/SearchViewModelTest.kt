package com.resonote.feature.search.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.SearchRepository
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
    fun hotKeywordFailureDoesNotBlockManualSearch() = runTest(dispatcher) {
        val repository = FakeSearchRepository(hotResult = CollectionLoadResult.Failed(ContentFailure.Network))
        val viewModel = SearchViewModel(repository)
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
        val viewModel = SearchViewModel(repository)
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
        val viewModel = SearchViewModel(repository)
        advanceUntilIdle()

        viewModel.updateQuery("不存在")
        viewModel.submit()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.result).isEqualTo(SearchResultUiState.Empty("不存在"))
    }

    @Test
    fun retryRepeatsFailedQuery() = runTest(dispatcher) {
        val repository = FakeSearchRepository(complexResult = CollectionLoadResult.Failed(ContentFailure.Network))
        val viewModel = SearchViewModel(repository)
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
        val viewModel = SearchViewModel(FakeSearchRepository())
        advanceUntilIdle()
        viewModel.updateQuery("旧查询")
        viewModel.submit()
        advanceUntilIdle()

        viewModel.updateQuery("新查询")

        assertThat(viewModel.uiState.value.result).isEqualTo(SearchResultUiState.Idle)
        assertThat(viewModel.uiState.value.query).isEqualTo("新查询")
    }

    private class FakeSearchRepository(
        private val hotResult: CollectionLoadResult<List<SearchKeyword>> =
            CollectionLoadResult.Available(listOf(SearchKeyword("热门", ""))),
        private val complexResult: CollectionLoadResult<ComplexSearchResult> =
            CollectionLoadResult.Available(result()),
    ) : SearchRepository {
        val suggestionQueries = mutableListOf<String>()
        val complexQueries = mutableListOf<String>()

        override suspend fun loadHotKeywords() = hotResult

        override suspend fun loadSuggestions(keywords: String): CollectionLoadResult<List<String>> {
            suggestionQueries += keywords
            return CollectionLoadResult.Available(listOf("$keywords 建议"))
        }

        override suspend fun searchComplex(keywords: String): CollectionLoadResult<ComplexSearchResult> {
            complexQueries += keywords
            return complexResult
        }

        override suspend fun searchSongs(keywords: String, page: Int, pageSize: Int) = page<OnlineSong>()
        override suspend fun searchPlaylists(keywords: String, page: Int, pageSize: Int) = page<SearchPlaylist>()
        override suspend fun searchAlbums(keywords: String, page: Int, pageSize: Int) = page<SearchAlbum>()
        override suspend fun searchArtists(keywords: String, page: Int, pageSize: Int) = page<SearchArtist>()
        override suspend fun searchMvs(keywords: String, page: Int, pageSize: Int) = page<SearchMv>()

        private fun <T> page(): CollectionLoadResult<SearchPage<T>> =
            CollectionLoadResult.Available(SearchPage(emptyList(), 1, 0, false))
    }

    private companion object {
        fun result() = ComplexSearchResult(
            artists = emptyList(),
            songs = emptyList(),
            songsTotal = 1,
            albums = emptyList(),
            albumsTotal = 0,
            playlists = listOf(SearchPlaylist("playlist", "歌单", null, null, 1, 1)),
            playlistsTotal = 1,
            mvs = emptyList(),
            mvsTotal = 0,
        )

        fun emptyResult() = ComplexSearchResult(
            artists = emptyList(), songs = emptyList(), songsTotal = 0,
            albums = emptyList(), albumsTotal = 0,
            playlists = emptyList(), playlistsTotal = 0,
            mvs = emptyList(), mvsTotal = 0,
        )
    }
}

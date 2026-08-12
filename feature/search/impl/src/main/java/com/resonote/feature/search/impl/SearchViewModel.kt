package com.resonote.feature.search.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.SearchHistoryRepository
import com.resonote.core.data.SearchRepository
import com.resonote.core.model.CollectionLoadResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SearchRepository,
    private val historyRepository: SearchHistoryRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = mutableUiState.asStateFlow()

    private var suggestionJob: Job? = null
    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null

    init {
        loadHotKeywords()
        viewModelScope.launch {
            historyRepository.queries.collect { queries ->
                mutableUiState.update { it.copy(history = queries) }
            }
        }
    }

    fun updateQuery(value: String) {
        mutableUiState.update { state ->
            val submittedQuery = state.result.queryOrNull()
            state.copy(
                query = value,
                result = if (submittedQuery != null && submittedQuery != value.trim()) {
                    SearchResultUiState.Idle
                } else {
                    state.result
                },
            )
        }
        suggestionJob?.cancel()
        val query = value.trim()
        if (query.isEmpty()) {
            mutableUiState.update { it.copy(suggestions = emptyList()) }
            return
        }
        suggestionJob = viewModelScope.launch {
            delay(SUGGESTION_DEBOUNCE_MILLIS)
            when (val result = repository.loadSuggestions(query)) {
                is CollectionLoadResult.Available -> mutableUiState.update { state ->
                    if (state.query.trim() == query) state.copy(suggestions = result.value.distinct().take(8)) else state
                }
                is CollectionLoadResult.Failed -> mutableUiState.update { state ->
                    if (state.query.trim() == query) state.copy(suggestions = emptyList()) else state
                }
            }
        }
    }

    fun submit(queryOverride: String? = null) {
        val query = (queryOverride ?: mutableUiState.value.query).trim()
        if (query.isEmpty()) return
        suggestionJob?.cancel()
        viewModelScope.launch { historyRepository.record(query) }
        runSearch(query, mutableUiState.value.selectedCategory)
    }

    fun selectCategory(category: SearchCategory) {
        val state = mutableUiState.value
        if (state.selectedCategory == category) return
        mutableUiState.update { it.copy(selectedCategory = category) }
        state.result.queryOrNull()?.let { runSearch(it, category) }
    }

    fun retry() {
        val state = mutableUiState.value
        runSearch(state.result.queryOrNull() ?: state.query.trim(), state.selectedCategory)
    }

    fun loadMore() {
        if (loadMoreJob?.isActive == true) return
        val state = mutableUiState.value
        val result = state.result as? SearchResultUiState.Content ?: return
        val page = result.value as? SearchContentUiState.Page ?: return
        if (!page.hasMore || page.isLoadingMore) return
        val nextPage = page.page + 1
        mutableUiState.update {
            it.copy(result = result.copy(value = page.copy(isLoadingMore = true, loadMoreFailure = null)))
        }
        loadMoreJob = viewModelScope.launch {
            when (val next = loadContent(result.query, result.category, nextPage)) {
                is CollectionLoadResult.Available -> appendPage(result.query, result.category, next.value)
                is CollectionLoadResult.Failed -> updatePage(result.query, result.category) {
                    it.copy(isLoadingMore = false, loadMoreFailure = next.failure)
                }
            }
        }
    }

    fun removeHistory(query: String) {
        viewModelScope.launch { historyRepository.remove(query) }
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepository.clear() }
    }

    private fun runSearch(query: String, category: SearchCategory) {
        if (query.isBlank()) return
        searchJob?.cancel()
        loadMoreJob?.cancel()
        mutableUiState.update {
            it.copy(
                query = query,
                suggestions = emptyList(),
                selectedCategory = category,
                result = SearchResultUiState.Loading(query, category),
            )
        }
        searchJob = viewModelScope.launch {
            when (val result = loadContent(query, category, page = 1)) {
                is CollectionLoadResult.Available -> mutableUiState.update { state ->
                    if (state.query != query || state.selectedCategory != category) state else state.copy(
                        result = if (result.value.hasContent()) {
                            SearchResultUiState.Content(query, category, result.value)
                        } else {
                            SearchResultUiState.Empty(query, category)
                        },
                    )
                }
                is CollectionLoadResult.Failed -> mutableUiState.update { state ->
                    if (state.query != query || state.selectedCategory != category) state else state.copy(
                        result = SearchResultUiState.Error(query, category, result.failure),
                    )
                }
            }
        }
    }

    private suspend fun loadContent(
        query: String,
        category: SearchCategory,
        page: Int,
    ): CollectionLoadResult<SearchContentUiState> = when (category) {
        SearchCategory.ALL -> repository.searchComplex(query).mapAvailable(SearchContentUiState::Aggregate)
        SearchCategory.SONGS -> repository.searchSongs(query, page).mapAvailable { value ->
            value.toPage { SearchResultItem.Song(it) }
        }
        SearchCategory.PLAYLISTS -> repository.searchPlaylists(query, page).mapAvailable { value ->
            value.toPage { SearchResultItem.Playlist(it) }
        }
        SearchCategory.ALBUMS -> repository.searchAlbums(query, page).mapAvailable { value ->
            value.toPage { SearchResultItem.Album(it) }
        }
        SearchCategory.MVS -> repository.searchMvs(query, page).mapAvailable { value ->
            value.toPage { SearchResultItem.Mv(it) }
        }
        SearchCategory.ARTISTS -> repository.searchArtists(query, page).mapAvailable { value ->
            value.toPage { SearchResultItem.Artist(it) }
        }
    }

    private fun appendPage(query: String, category: SearchCategory, next: SearchContentUiState) {
        val nextPage = next as? SearchContentUiState.Page ?: return
        updatePage(query, category) { current ->
            val existingIds = current.items.mapTo(mutableSetOf()) { it.stableId }
            current.copy(
                items = current.items + nextPage.items.filter { existingIds.add(it.stableId) },
                page = nextPage.page,
                total = nextPage.total,
                hasMore = nextPage.hasMore,
                isLoadingMore = false,
                loadMoreFailure = null,
            )
        }
    }

    private fun updatePage(
        query: String,
        category: SearchCategory,
        transform: (SearchContentUiState.Page) -> SearchContentUiState.Page,
    ) {
        mutableUiState.update { state ->
            val result = state.result as? SearchResultUiState.Content
            val page = result?.value as? SearchContentUiState.Page
            if (result?.query != query || result.category != category || page == null) state else {
                state.copy(result = result.copy(value = transform(page)))
            }
        }
    }

    private fun loadHotKeywords() {
        viewModelScope.launch {
            when (val result = repository.loadHotKeywords()) {
                is CollectionLoadResult.Available -> mutableUiState.update {
                    it.copy(hotKeywords = result.value.distinctBy { keyword -> keyword.keyword }.take(12))
                }
                is CollectionLoadResult.Failed -> Unit
            }
        }
    }

    private companion object {
        const val SUGGESTION_DEBOUNCE_MILLIS = 300L
    }
}

private fun SearchResultUiState.queryOrNull(): String? = when (this) {
    SearchResultUiState.Idle -> null
    is SearchResultUiState.Loading -> query
    is SearchResultUiState.Content -> query
    is SearchResultUiState.Empty -> query
    is SearchResultUiState.Error -> query
}

private inline fun <T, R> CollectionLoadResult<T>.mapAvailable(
    transform: (T) -> R,
): CollectionLoadResult<R> = when (this) {
    is CollectionLoadResult.Available -> CollectionLoadResult.Available(transform(value))
    is CollectionLoadResult.Failed -> this
}

private inline fun <T> com.resonote.core.model.SearchPage<T>.toPage(
    transform: (T) -> SearchResultItem,
) = SearchContentUiState.Page(items.map(transform), page, total, hasMore)

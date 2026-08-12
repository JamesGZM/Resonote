package com.resonote.feature.search.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = mutableUiState.asStateFlow()

    private var suggestionJob: Job? = null
    private var searchJob: Job? = null

    init {
        loadHotKeywords()
    }

    fun updateQuery(value: String) {
        mutableUiState.update { state ->
            val submittedQuery = when (val result = state.result) {
                is SearchResultUiState.Content -> result.query
                is SearchResultUiState.Empty -> result.query
                is SearchResultUiState.Error -> result.query
                is SearchResultUiState.Loading -> result.query
                SearchResultUiState.Idle -> null
            }
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
        searchJob?.cancel()
        mutableUiState.update {
            it.copy(query = query, suggestions = emptyList(), result = SearchResultUiState.Loading(query))
        }
        searchJob = viewModelScope.launch {
            when (val result = repository.searchComplex(query)) {
                is CollectionLoadResult.Available -> mutableUiState.update { state ->
                    if (state.query != query) state else state.copy(
                        result = if (result.value.hasContent()) {
                            SearchResultUiState.Content(query, result.value)
                        } else {
                            SearchResultUiState.Empty(query)
                        },
                    )
                }
                is CollectionLoadResult.Failed -> mutableUiState.update { state ->
                    if (state.query != query) state else state.copy(result = SearchResultUiState.Error(query, result.failure))
                }
            }
        }
    }

    fun retry() {
        val query = when (val result = mutableUiState.value.result) {
            is SearchResultUiState.Error -> result.query
            else -> mutableUiState.value.query
        }
        submit(query)
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

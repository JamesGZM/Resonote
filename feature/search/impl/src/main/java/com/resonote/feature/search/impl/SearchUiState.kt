package com.resonote.feature.search.impl

import androidx.compose.runtime.Immutable
import com.resonote.core.model.ComplexSearchResult
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.SearchKeyword

@Immutable
data class SearchUiState(
    val query: String = "",
    val hotKeywords: List<SearchKeyword> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val result: SearchResultUiState = SearchResultUiState.Idle,
)

@Immutable
sealed interface SearchResultUiState {
    data object Idle : SearchResultUiState

    data class Loading(val query: String) : SearchResultUiState

    data class Content(val query: String, val value: ComplexSearchResult) : SearchResultUiState

    data class Empty(val query: String) : SearchResultUiState

    data class Error(val query: String, val failure: ContentFailure) : SearchResultUiState
}

internal fun ComplexSearchResult.hasContent(): Boolean =
    artists.isNotEmpty() || songs.isNotEmpty() || albums.isNotEmpty() || playlists.isNotEmpty() || mvs.isNotEmpty()

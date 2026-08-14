package com.resonote.feature.search.impl

import androidx.compose.runtime.Immutable
import com.resonote.core.model.ComplexSearchResult
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.SearchAlbum
import com.resonote.core.model.SearchArtist
import com.resonote.core.model.SearchKeyword
import com.resonote.core.model.SearchMv
import com.resonote.core.model.SearchPlaylist

enum class SearchCategory {
    ALL,
    SONGS,
    PLAYLISTS,
    ALBUMS,
    MVS,
    ARTISTS,
}

@Immutable
data class SearchUiState(
    val query: String = "",
    val history: List<String> = emptyList(),
    val hotKeywords: List<SearchKeyword> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val selectedCategory: SearchCategory = SearchCategory.ALL,
    val result: SearchResultUiState = SearchResultUiState.Idle,
)

@Immutable
sealed interface SearchResultUiState {
    data object Idle : SearchResultUiState

    data class Loading(val query: String, val category: SearchCategory) : SearchResultUiState

    data class Content(val query: String, val category: SearchCategory, val value: SearchContentUiState) :
        SearchResultUiState

    data class Empty(val query: String, val category: SearchCategory) : SearchResultUiState

    data class Error(val query: String, val category: SearchCategory, val failure: ContentFailure) : SearchResultUiState
}

@Immutable
sealed interface SearchContentUiState {
    data class Aggregate(val value: ComplexSearchResult) : SearchContentUiState

    data class Page(
        val items: List<SearchResultItem>,
        val page: Int,
        val total: Int,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false,
        val loadMoreFailure: ContentFailure? = null,
    ) : SearchContentUiState
}

@Immutable
sealed interface SearchResultItem {
    val stableId: String

    data class Song(val value: OnlineSong) : SearchResultItem {
        override val stableId = value.hash
    }

    data class Playlist(val value: SearchPlaylist) : SearchResultItem {
        override val stableId = value.id
    }

    data class Album(val value: SearchAlbum) : SearchResultItem {
        override val stableId = value.id
    }

    data class Mv(val value: SearchMv) : SearchResultItem {
        override val stableId = value.hash
    }

    data class Artist(val value: SearchArtist) : SearchResultItem {
        override val stableId = value.id
    }
}

internal fun SearchContentUiState.hasContent(): Boolean = when (this) {
    is SearchContentUiState.Aggregate ->
        value.artists.isNotEmpty() ||
            value.songs.isNotEmpty() ||
            value.albums.isNotEmpty() ||
            value.playlists.isNotEmpty() ||
            value.mvs.isNotEmpty()
    is SearchContentUiState.Page -> items.isNotEmpty()
}

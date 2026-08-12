package com.resonote.feature.playlist.impl

import androidx.compose.runtime.Immutable
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaylistDetails

@Immutable
sealed interface PlaylistUiState {
    data object Loading : PlaylistUiState

    data class Content(
        val details: PlaylistDetails?,
        val songs: List<OnlineSong>,
        val page: Int,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false,
        val loadMoreFailure: ContentFailure? = null,
    ) : PlaylistUiState

    data object Empty : PlaylistUiState

    data class Error(val failure: ContentFailure) : PlaylistUiState
}

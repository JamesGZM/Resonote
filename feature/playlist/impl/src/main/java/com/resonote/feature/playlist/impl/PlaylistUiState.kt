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
        val writableListId: String? = null,
        val isLoadingMore: Boolean = false,
        val loadMoreFailure: ContentFailure? = null,
        val isRefreshing: Boolean = false,
        val refreshFailure: ContentFailure? = null,
        val removal: PlaylistRemovalUiState = PlaylistRemovalUiState.Idle,
    ) : PlaylistUiState

    data object Empty : PlaylistUiState

    data class Error(val failure: ContentFailure) : PlaylistUiState
}

@Immutable
sealed interface PlaylistRemovalUiState {
    data object Idle : PlaylistRemovalUiState

    data class Removing(val songHash: String) : PlaylistRemovalUiState

    data class Failed(val songHash: String, val failure: ContentFailure) : PlaylistRemovalUiState

    data class Removed(val title: String) : PlaylistRemovalUiState
}

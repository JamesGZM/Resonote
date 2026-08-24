package com.resonote.feature.library.impl

import androidx.compose.runtime.Immutable
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.FollowedArtist

sealed interface FollowingUiState {
    data object Loading : FollowingUiState

    data object Empty : FollowingUiState

    @Immutable
    data class Error(val failure: ContentFailure) : FollowingUiState

    @Immutable
    data class Content(
        val artists: List<FollowedArtist>,
        val total: Int,
        val hasMore: Boolean,
        val isRefreshing: Boolean = false,
        val refreshingFailure: ContentFailure? = null,
        val updatingArtistIds: Set<String> = emptySet(),
        val updateFailure: ContentFailure? = null,
    ) : FollowingUiState
}

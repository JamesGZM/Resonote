package com.resonote.feature.library.impl

import androidx.compose.runtime.Immutable
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.UserPlaylist
import com.resonote.core.model.UserProfile

@Immutable
sealed interface MySectionState<out T> {
    data object Loading : MySectionState<Nothing>

    data class Available<T>(val value: T) : MySectionState<T>

    data class Failed(val failure: ContentFailure) : MySectionState<Nothing>
}

@Immutable
sealed interface PlaylistCreationUiState {
    data object Idle : PlaylistCreationUiState

    data object Submitting : PlaylistCreationUiState

    data class Failed(val failure: ContentFailure) : PlaylistCreationUiState

    data class Created(
        val name: String,
        val listId: String,
        val refreshFailed: Boolean,
    ) : PlaylistCreationUiState
}

@Immutable
sealed interface MyUiState {
    data object CheckingAccount : MyUiState

    data object Anonymous : MyUiState

    data class Authenticated(
        val userId: String,
        val profile: MySectionState<UserProfile> = MySectionState.Loading,
        val playlists: MySectionState<List<UserPlaylist>> = MySectionState.Loading,
        val isRefreshing: Boolean = false,
        val playlistCreation: PlaylistCreationUiState = PlaylistCreationUiState.Idle,
    ) : MyUiState
}

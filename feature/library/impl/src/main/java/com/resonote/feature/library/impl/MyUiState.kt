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
sealed interface MyUiState {
    data object CheckingAccount : MyUiState

    data object Anonymous : MyUiState

    data class Authenticated(
        val profile: MySectionState<UserProfile> = MySectionState.Loading,
        val playlists: MySectionState<List<UserPlaylist>> = MySectionState.Loading,
        val isRefreshing: Boolean = false,
    ) : MyUiState
}

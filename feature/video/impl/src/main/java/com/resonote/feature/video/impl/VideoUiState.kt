package com.resonote.feature.video.impl

import com.resonote.core.model.ContentFailure

sealed interface VideoUiState {
    data object Idle : VideoUiState

    data object Loading : VideoUiState

    data class Ready(val url: String) : VideoUiState {
        init {
            require(url.isNotBlank()) { "url must not be blank" }
        }
    }

    data object Unavailable : VideoUiState

    data class Failed(val failure: ContentFailure) : VideoUiState
}

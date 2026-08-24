package com.resonote.feature.album.impl

import androidx.compose.runtime.Immutable
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong

@Immutable
data class AlbumMetadata(
    val id: String,
    val title: String?,
    val artist: String?,
    val coverUrl: String?,
    val publishDate: String?,
    val songCount: Int?,
)

@Immutable
sealed interface AlbumUiState {
    data object Loading : AlbumUiState

    data class Content(
        val metadata: AlbumMetadata,
        val songs: List<OnlineSong>,
        val page: Int,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false,
        val loadMoreFailure: ContentFailure? = null,
        val isRefreshing: Boolean = false,
        val refreshFailure: ContentFailure? = null,
    ) : AlbumUiState

    data class Empty(val metadata: AlbumMetadata) : AlbumUiState

    data class Error(val failure: ContentFailure, val title: String?) : AlbumUiState
}

package com.resonote.feature.discover.impl

import androidx.compose.runtime.Immutable
import com.resonote.core.model.Album
import com.resonote.core.model.AlbumRegion
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaylistCategory
import com.resonote.core.model.PlaylistSummary
import com.resonote.core.model.Ranking

enum class DiscoverSection { PLAYLISTS, RANKINGS, ALBUMS, SONGS }

@Immutable
sealed interface DiscoverLoadState<out T> {
    data object Idle : DiscoverLoadState<Nothing>
    data object Loading : DiscoverLoadState<Nothing>
    data class Content<T>(val value: T) : DiscoverLoadState<T>
    data object Empty : DiscoverLoadState<Nothing>
    data class Error(val failure: ContentFailure) : DiscoverLoadState<Nothing>
}

@Immutable
sealed interface DiscoverPageState<out T> {
    data object Idle : DiscoverPageState<Nothing>
    data object Loading : DiscoverPageState<Nothing>

    data class Content<T>(
        val items: List<T>,
        val page: Int,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false,
        val loadMoreFailure: ContentFailure? = null,
    ) : DiscoverPageState<T>

    data object Empty : DiscoverPageState<Nothing>
    data class Error(val failure: ContentFailure) : DiscoverPageState<Nothing>
}

@Immutable
data class DiscoverUiState(
    val selectedSection: DiscoverSection = DiscoverSection.PLAYLISTS,
    val refreshingSection: DiscoverSection? = null,
    val categories: DiscoverLoadState<List<PlaylistCategory>> = DiscoverLoadState.Idle,
    val selectedParentCategoryId: Int? = null,
    val selectedPlaylistCategoryId: Int = 0,
    val playlists: DiscoverPageState<PlaylistSummary> = DiscoverPageState.Idle,
    val rankings: DiscoverLoadState<List<Ranking>> = DiscoverLoadState.Idle,
    val selectedAlbumRegion: AlbumRegion? = null,
    val albums: DiscoverLoadState<List<Album>> = DiscoverLoadState.Idle,
    val songs: DiscoverPageState<OnlineSong> = DiscoverPageState.Idle,
)

package com.resonote.feature.cloud.impl

import com.resonote.core.model.CloudStorage
import com.resonote.core.model.CloudTrack
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.ResolvedSongSource

data class CloudUiState(
    val tracks: List<CloudTrack> = emptyList(),
    val page: Int = 0,
    val total: Int = 0,
    val hasMore: Boolean = true,
    val storage: CloudStorage? = null,
    val initialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isIndexing: Boolean = false,
    val failure: ContentFailure? = null,
    val loadMoreFailure: ContentFailure? = null,
    val query: String = "",
    val sort: CloudSort = CloudSort.UploadOrder,
    val viewMode: CloudViewMode = CloudViewMode.List,
    val playback: CloudPlaybackUiState = CloudPlaybackUiState.Idle,
) {
    val visibleTracks: List<CloudTrack>
        get() {
            val normalizedQuery = query.trim()
            val filtered = if (normalizedQuery.isEmpty()) {
                tracks
            } else {
                tracks.filter { track ->
                    track.title.contains(normalizedQuery, ignoreCase = true) ||
                        track.artist.orEmpty().contains(normalizedQuery, ignoreCase = true) ||
                        track.album.orEmpty().contains(normalizedQuery, ignoreCase = true)
                }
            }
            return when (sort) {
                CloudSort.UploadOrder -> filtered
                CloudSort.Title -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                CloudSort.Artist -> filtered.sortedWith { first, second ->
                    val artistOrder = String.CASE_INSENSITIVE_ORDER.compare(
                        first.artist.orEmpty(),
                        second.artist.orEmpty(),
                    )
                    if (artistOrder != 0) artistOrder else String.CASE_INSENSITIVE_ORDER.compare(first.title, second.title)
                }
                CloudSort.Duration -> filtered.sortedByDescending(CloudTrack::durationMillis)
            }
        }
}

enum class CloudSort { UploadOrder, Title, Artist, Duration }

enum class CloudViewMode { List, Grid }

sealed interface CloudPlaybackUiState {
    data object Idle : CloudPlaybackUiState
    data class Resolving(val trackHash: String) : CloudPlaybackUiState
    data class Failed(val trackHash: String, val issue: CloudPlaybackIssue) : CloudPlaybackUiState
}

sealed interface CloudPlaybackIssue {
    data object Unavailable : CloudPlaybackIssue
    data class Failed(val failure: ContentFailure) : CloudPlaybackIssue
}

data class CloudPlaybackRequest(
    val tracks: List<CloudTrack>,
    val startIndex: Int,
    val source: ResolvedSongSource,
) {
    init {
        require(tracks.isNotEmpty()) { "tracks must not be empty" }
        require(startIndex in tracks.indices) { "startIndex must point to a track" }
    }
}

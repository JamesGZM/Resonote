package com.resonote.feature.artist.impl

import androidx.compose.runtime.Immutable
import com.resonote.core.model.ArtistAlbum
import com.resonote.core.model.ArtistVideo
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong

@Immutable
data class ArtistProfile(
    val id: String,
    val name: String?,
    val avatarUrl: String? = null,
    val intro: String? = null,
    val songCount: Int? = null,
    val albumCount: Int? = null,
    val mvCount: Int? = null,
    val fansCount: Long? = null,
)

enum class ArtistSection {
    SONGS,
    ALBUMS,
    MVS,
}

enum class ArtistSort {
    POPULAR,
    LATEST,
}

sealed interface ArtistItem {
    val stableId: String

    @Immutable
    data class Song(val value: OnlineSong) : ArtistItem {
        override val stableId: String = "song:${value.hash}"
    }

    @Immutable
    data class Album(val value: ArtistAlbum) : ArtistItem {
        override val stableId: String = "album:${value.id}"
    }

    @Immutable
    data class Video(val value: ArtistVideo) : ArtistItem {
        override val stableId: String = "video:${value.hash}"
    }
}

sealed interface ArtistPageUiState {
    data object Idle : ArtistPageUiState

    data object Loading : ArtistPageUiState

    data object Empty : ArtistPageUiState

    @Immutable
    data class Error(val failure: ContentFailure) : ArtistPageUiState

    @Immutable
    data class Content(
        val items: List<ArtistItem>,
        val page: Int,
        val total: Int?,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false,
        val loadMoreFailure: ContentFailure? = null,
        val isRefreshing: Boolean = false,
        val refreshFailure: ContentFailure? = null,
    ) : ArtistPageUiState
}

sealed interface ArtistFollowUiState {
    data object Loading : ArtistFollowUiState

    data object AuthenticationRequired : ArtistFollowUiState

    @Immutable
    data class Error(val failure: ContentFailure) : ArtistFollowUiState

    @Immutable
    data class Available(
        val isFollowed: Boolean,
        val isUpdating: Boolean = false,
        val updateFailure: ContentFailure? = null,
    ) : ArtistFollowUiState
}

@Immutable
data class ArtistUiState(
    val profile: ArtistProfile? = null,
    val selectedSection: ArtistSection = ArtistSection.SONGS,
    val selectedSort: ArtistSort = ArtistSort.POPULAR,
    val follow: ArtistFollowUiState = ArtistFollowUiState.Loading,
    val popularSongs: ArtistPageUiState = ArtistPageUiState.Idle,
    val latestSongs: ArtistPageUiState = ArtistPageUiState.Idle,
    val popularAlbums: ArtistPageUiState = ArtistPageUiState.Idle,
    val latestAlbums: ArtistPageUiState = ArtistPageUiState.Idle,
    val videos: ArtistPageUiState = ArtistPageUiState.Idle,
) {
    fun selectedPage(): ArtistPageUiState = page(selectedSection, selectedSort)

    fun page(section: ArtistSection, sort: ArtistSort): ArtistPageUiState = when (section) {
        ArtistSection.SONGS -> if (sort == ArtistSort.POPULAR) popularSongs else latestSongs
        ArtistSection.ALBUMS -> if (sort == ArtistSort.POPULAR) popularAlbums else latestAlbums
        ArtistSection.MVS -> videos
    }

    fun withPage(section: ArtistSection, sort: ArtistSort, page: ArtistPageUiState): ArtistUiState = when (section) {
        ArtistSection.SONGS -> if (sort == ArtistSort.POPULAR) {
            copy(popularSongs = page)
        } else {
            copy(latestSongs = page)
        }
        ArtistSection.ALBUMS -> if (sort == ArtistSort.POPULAR) {
            copy(popularAlbums = page)
        } else {
            copy(latestAlbums = page)
        }
        ArtistSection.MVS -> copy(videos = page)
    }
}

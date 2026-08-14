package com.resonote.feature.artist.impl

import androidx.compose.runtime.Immutable
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong

enum class ArtistSongSection { POPULAR, LATEST }

@Immutable
data class ArtistProfile(
    val id: String,
    val name: String?,
    val avatarUrl: String?,
    val intro: String?,
    val songCount: Int?,
    val albumCount: Int?,
    val mvCount: Int?,
    val fansCount: Long?,
)

@Immutable
sealed interface ArtistPageUiState {
    data object Idle : ArtistPageUiState
    data object Loading : ArtistPageUiState

    data class Content(
        val songs: List<OnlineSong>,
        val page: Int,
        val total: Int,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false,
        val loadMoreFailure: ContentFailure? = null,
    ) : ArtistPageUiState

    data object Empty : ArtistPageUiState
    data class Error(val failure: ContentFailure) : ArtistPageUiState
}

@Immutable
data class ArtistUiState(
    val profile: ArtistProfile? = null,
    val selectedSection: ArtistSongSection = ArtistSongSection.POPULAR,
    val popular: ArtistPageUiState = ArtistPageUiState.Idle,
    val latest: ArtistPageUiState = ArtistPageUiState.Idle,
) {
    fun selectedPage(): ArtistPageUiState = page(selectedSection)

    fun page(section: ArtistSongSection): ArtistPageUiState = when (section) {
        ArtistSongSection.POPULAR -> popular
        ArtistSongSection.LATEST -> latest
    }

    fun withPage(section: ArtistSongSection, page: ArtistPageUiState): ArtistUiState = when (section) {
        ArtistSongSection.POPULAR -> copy(popular = page)
        ArtistSongSection.LATEST -> copy(latest = page)
    }
}

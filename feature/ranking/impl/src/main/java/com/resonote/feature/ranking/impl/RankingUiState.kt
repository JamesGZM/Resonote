package com.resonote.feature.ranking.impl

import androidx.compose.runtime.Immutable
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong

@Immutable
data class RankingMetadata(val id: String, val title: String?, val coverUrl: String?)

@Immutable
sealed interface RankingUiState {
    data class Loading(val metadata: RankingMetadata) : RankingUiState

    data class Content(
        val metadata: RankingMetadata,
        val songs: List<OnlineSong>,
        val page: Int,
        val total: Int?,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false,
        val loadMoreFailure: ContentFailure? = null,
    ) : RankingUiState

    data class Empty(val metadata: RankingMetadata) : RankingUiState
    data class Error(val metadata: RankingMetadata, val failure: ContentFailure) : RankingUiState
}

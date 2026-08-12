package com.resonote.feature.ranking.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class RankingNavKey(
    val rankingId: String,
    val title: String? = null,
    val coverUrl: String? = null,
) : NavKey {
    init {
        require(rankingId.isNotBlank()) { "rankingId must not be blank" }
    }
}

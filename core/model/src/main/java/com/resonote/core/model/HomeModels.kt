package com.resonote.core.model

data class HomeContent(
    val dailyRecommendations: List<OnlineSong>,
    val recommendedPlaylists: List<PlaylistSummary>,
    val newSongs: List<OnlineSong>,
)

enum class HomeSection {
    DailyRecommendations,
    RecommendedPlaylists,
    NewSongs,
}

data class HomeIssue(val section: HomeSection, val failure: ContentFailure)

sealed interface HomeRefreshResult {
    data class Updated(val content: HomeContent, val issues: List<HomeIssue>) : HomeRefreshResult

    data class Failed(val issues: List<HomeIssue>) : HomeRefreshResult

    data object Superseded : HomeRefreshResult
}

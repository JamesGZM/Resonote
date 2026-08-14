package com.resonote.core.model

enum class RecommendationMode {
    Personal,
    Nostalgia,
    Popular,
    HiddenGems,
    Vip,
}

sealed interface RadioRecommendationResult {
    data class Available(val songs: List<OnlineSong>) : RadioRecommendationResult

    data class Failed(val failure: ContentFailure) : RadioRecommendationResult
}

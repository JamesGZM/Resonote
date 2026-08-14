package com.resonote.core.data

import com.resonote.core.model.HomeContent
import com.resonote.core.model.HomeRefreshResult
import com.resonote.core.model.RadioRecommendationResult
import com.resonote.core.model.RecommendationMode
import kotlinx.coroutines.flow.StateFlow

interface HomeRepository {
    val content: StateFlow<HomeContent?>

    suspend fun refresh(): HomeRefreshResult

    suspend fun loadRadio(mode: RecommendationMode = RecommendationMode.Personal): RadioRecommendationResult
}

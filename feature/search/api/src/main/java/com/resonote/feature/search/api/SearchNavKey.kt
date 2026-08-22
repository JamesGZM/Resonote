package com.resonote.feature.search.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class SearchNavKey(val sessionId: Long, val initialQuery: String = "") : NavKey

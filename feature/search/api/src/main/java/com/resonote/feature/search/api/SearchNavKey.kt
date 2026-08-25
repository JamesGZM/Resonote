package com.resonote.feature.search.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
enum class SearchTab {
    ALL,
    SONGS,
    PLAYLISTS,
    ALBUMS,
    MVS,
    ARTISTS,
}

@Serializable
data class SearchNavKey(
    val sessionId: Long,
    val initialQuery: String = "",
    val initialTab: SearchTab = SearchTab.ALL,
) : NavKey

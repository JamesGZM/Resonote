package com.resonote.feature.playlist.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistNavKey(val playlistId: String) : NavKey {
    init {
        require(playlistId.isNotBlank()) { "playlistId must not be blank" }
    }
}

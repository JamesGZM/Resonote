package com.resonote.feature.playlist.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistNavKey(
    val playlistId: String,
    val writableListId: String? = null,
    val writableAccountId: String? = null,
) : NavKey {
    init {
        require(playlistId.isNotBlank()) { "playlistId must not be blank" }
        require(writableListId == null || writableListId.isNotBlank()) { "writableListId must not be blank" }
        require(writableAccountId == null || writableAccountId.isNotBlank()) { "writableAccountId must not be blank" }
        require((writableListId == null) == (writableAccountId == null)) {
            "writableListId and writableAccountId must be supplied together"
        }
    }
}

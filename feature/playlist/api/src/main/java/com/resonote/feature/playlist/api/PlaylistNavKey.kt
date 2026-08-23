package com.resonote.feature.playlist.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistNavKey(
    val playlistId: String,
    val title: String? = null,
    val coverUrl: String? = null,
    val writableListId: String? = null,
    val writableAccountId: String? = null,
) : NavKey {
    init {
        require(playlistId.isNotBlank()) { "playlistId must not be blank" }
        require(title == null || title.isNotBlank()) { "title must not be blank" }
        require(coverUrl == null || coverUrl.isNotBlank()) { "coverUrl must not be blank" }
        require(writableListId == null || writableListId.isNotBlank()) { "writableListId must not be blank" }
        require(writableAccountId == null || writableAccountId.isNotBlank()) { "writableAccountId must not be blank" }
        require((writableListId == null) == (writableAccountId == null)) {
            "writableListId and writableAccountId must be supplied together"
        }
    }
}

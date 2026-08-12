package com.resonote.feature.album.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class AlbumNavKey(
    val albumId: String,
    val name: String? = null,
    val artist: String? = null,
    val coverUrl: String? = null,
    val publishDate: String? = null,
    val songCount: Int? = null,
) : NavKey {
    init {
        require(albumId.isNotBlank()) { "albumId must not be blank" }
        require(songCount == null || songCount >= 0) { "songCount must not be negative" }
    }
}

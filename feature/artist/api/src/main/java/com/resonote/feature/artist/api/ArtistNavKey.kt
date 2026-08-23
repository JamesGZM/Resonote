package com.resonote.feature.artist.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class ArtistNavKey(
    val artistId: String,
    val name: String? = null,
    val avatarUrl: String? = null,
    val songCount: Int? = null,
    val albumCount: Int? = null,
    val sessionId: Long = 0,
) : NavKey {
    init {
        require(artistId.isNotBlank()) { "artistId must not be blank" }
        require(songCount == null || songCount >= 0) { "songCount must not be negative" }
        require(albumCount == null || albumCount >= 0) { "albumCount must not be negative" }
        require(sessionId >= 0) { "sessionId must not be negative" }
    }
}

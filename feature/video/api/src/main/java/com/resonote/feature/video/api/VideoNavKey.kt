package com.resonote.feature.video.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class VideoNavKey(
    val hash: String,
    val title: String,
    val singer: String? = null,
    val coverUrl: String? = null,
    val durationMillis: Long = 0,
) : NavKey {
    init {
        require(hash.isNotBlank()) { "hash must not be blank" }
        require(title.isNotBlank()) { "title must not be blank" }
        require(durationMillis >= 0) { "durationMillis must not be negative" }
    }
}

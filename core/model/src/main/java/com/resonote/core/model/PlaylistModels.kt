package com.resonote.core.model

data class PlaylistSummary(val id: String, val title: String, val coverUrl: String?, val playCount: Long?)

data class PlaylistDetails(
    val id: String,
    val title: String,
    val description: String,
    val coverUrl: String?,
    val songCount: Int,
)

data class PlaylistPage(
    val details: PlaylistDetails?,
    val songs: List<OnlineSong>,
    val page: Int,
    val hasMore: Boolean,
)

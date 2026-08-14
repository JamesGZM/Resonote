package com.resonote.core.network.model

data class NetworkPlaylistSummary(val id: String, val title: String, val coverUrl: String?, val playCount: Long?)

data class NetworkPlaylistInfo(
    val id: String,
    val title: String,
    val description: String,
    val coverUrl: String?,
    val songCount: Int,
)

data class NetworkPlaylistPage(val info: NetworkPlaylistInfo?, val songs: List<NetworkSong>, val hasMore: Boolean)

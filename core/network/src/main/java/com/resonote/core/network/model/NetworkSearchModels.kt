package com.resonote.core.network.model

data class NetworkSearchPage(
    val items: List<NetworkSong>,
    val total: Int,
)

data class NetworkSong(
    val hash: String,
    val title: String,
    val singerName: String,
    val imageUrl: String?,
    val durationSeconds: Long,
    val highQualityHash: String?,
    val losslessHash: String?,
)

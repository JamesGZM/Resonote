package com.resonote.core.network.model

data class NetworkSong(
    val hash: String,
    val title: String,
    val artist: String?,
    val coverUrl: String?,
    val albumId: String?,
    val albumAudioId: String?,
    val durationMillis: Long,
    val highQualityHash: String?,
    val losslessHash: String?,
    val vip: Boolean,
    val highQualityAvailable: Boolean = false,
    val losslessAvailable: Boolean = false,
    val albumTitle: String? = null,
    val fileId: String? = null,
)

data class NetworkSongPage(
    val songs: List<NetworkSong>,
    val total: Int?,
    val hasMore: Boolean,
)

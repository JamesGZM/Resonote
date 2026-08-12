package com.resonote.core.network.model

data class NetworkCloudStorage(
    val usedBytes: Long,
    val maxBytes: Long,
)

data class NetworkCloudTrack(
    val hash: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val coverUrl: String?,
    val durationMillis: Long,
    val albumAudioId: String?,
)

data class NetworkCloudPage(
    val tracks: List<NetworkCloudTrack>,
    val total: Int,
    val hasMore: Boolean,
    val storage: NetworkCloudStorage?,
)

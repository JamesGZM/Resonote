package com.resonote.core.network.model

data class NetworkHomeSong(
    val hash: String,
    val title: String,
    val artist: String,
    val coverUrl: String?,
    val albumId: String?,
    val albumAudioId: String?,
    val durationMillis: Long,
    val highQualityHash: String?,
    val losslessHash: String?,
    val vip: Boolean,
)

data class NetworkHomePlaylist(
    val id: String,
    val title: String,
    val coverUrl: String?,
    val playCount: Long?,
)

enum class NetworkRecommendationMode(val cardId: Int) {
    Personal(1),
    Nostalgia(2),
    Popular(3),
    HiddenGems(4),
    Vip(6),
}

data class NetworkSongSource(
    val uri: String,
    val durationMillis: Long,
    val extension: String?,
)

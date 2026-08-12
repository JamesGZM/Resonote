package com.resonote.core.network.model

data class NetworkHomeSong(
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

data class NetworkRanking(
    val id: String,
    val title: String,
    val coverUrl: String?,
)

data class NetworkSongPage(
    val songs: List<NetworkHomeSong>,
    val total: Int?,
    val hasMore: Boolean,
)

data class NetworkPlaylistInfo(
    val id: String,
    val title: String,
    val description: String,
    val coverUrl: String?,
    val songCount: Int,
)

data class NetworkPlaylistPage(
    val info: NetworkPlaylistInfo?,
    val songs: List<NetworkHomeSong>,
    val hasMore: Boolean,
)

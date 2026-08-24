package com.resonote.core.model

enum class AudioQuality {
    Standard,
    HighQuality,
    HighResolution,
    Lossless,
}

data class OnlineSong(
    val hash: String,
    val title: String,
    val artist: String?,
    val coverUrl: String?,
    val albumId: String?,
    val albumAudioId: String?,
    val durationMillis: Long,
    val quality: AudioQuality,
    val vip: Boolean,
    val albumTitle: String? = null,
    val fileId: String? = null,
    val previewDurationMillis: Long? = null,
)

data class SongPage(val songs: List<OnlineSong>, val page: Int, val total: Int?, val hasMore: Boolean)

data class ListeningHistoryPage(val songs: List<OnlineSong>, val nextCursor: String?, val hasMore: Boolean)

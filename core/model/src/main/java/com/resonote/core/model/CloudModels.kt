package com.resonote.core.model

data class CloudStorage(val usedBytes: Long, val maxBytes: Long)

data class CloudTrack(
    val hash: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val coverUrl: String?,
    val durationMillis: Long,
    val albumAudioId: String?,
)

data class CloudPage(
    val tracks: List<CloudTrack>,
    val page: Int,
    val total: Int,
    val hasMore: Boolean,
    val storage: CloudStorage?,
)

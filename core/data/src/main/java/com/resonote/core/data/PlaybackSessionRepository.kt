package com.resonote.core.data

import com.resonote.core.model.AudioQuality

enum class PlaybackSessionEntryKind {
    Online,
    Cloud,
    Local,
}

data class PlaybackSessionEntry(
    val kind: PlaybackSessionEntryKind,
    val mediaId: String,
    val title: String,
    val artist: String?,
    val albumTitle: String?,
    val artworkUri: String?,
    val durationMillis: Long,
    val isVip: Boolean,
    val audioQuality: AudioQuality? = null,
    val albumId: String? = null,
    val albumAudioId: String? = null,
    val fileId: String? = null,
    val previewDurationMillis: Long? = null,
    val mimeType: String? = null,
    val extension: String? = null,
    val sampleRateHz: Int? = null,
    val bitDepth: Int? = null,
    val bitrateBitsPerSecond: Int? = null,
)

data class PlaybackSessionSnapshot(
    val entries: List<PlaybackSessionEntry>,
    val currentIndex: Int,
    val positionMillis: Long,
    val mode: String,
)

interface PlaybackSessionRepository {
    suspend fun load(): PlaybackSessionSnapshot?

    suspend fun save(snapshot: PlaybackSessionSnapshot)

    suspend fun clear()
}

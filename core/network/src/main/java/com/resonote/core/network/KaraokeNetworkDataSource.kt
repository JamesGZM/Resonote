package com.resonote.core.network

data class NetworkKaraokeAccompaniment(
    val hash: String,
    val songId: Long?,
    val songName: String?,
    val singerName: String?,
    val durationMillis: Long,
    val extension: String?,
    val bitrateKbps: Int?,
    val sizeBytes: Long?,
    val remark: String?,
    val showMic: Boolean,
)

interface KaraokeNetworkDataSource {
    suspend fun matchAccompaniment(
        originalHash: String,
        albumAudioId: String?,
        fileName: String,
    ): NetworkKaraokeAccompaniment?
}

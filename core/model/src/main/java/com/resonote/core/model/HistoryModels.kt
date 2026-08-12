package com.resonote.core.model

enum class DeviceHistorySource {
    Local,
    Cloud,
}

data class DeviceHistoryRecord(
    val source: DeviceHistorySource,
    val mediaId: String,
    val title: String,
    val artist: String?,
    val albumTitle: String?,
    val artworkUri: String?,
    val durationMillis: Long,
    val albumAudioId: String?,
) {
    init {
        require(mediaId.isNotBlank()) { "mediaId must not be blank" }
        require(title.isNotBlank()) { "title must not be blank" }
        require(durationMillis >= 0) { "durationMillis must not be negative" }
    }
}

data class DeviceHistoryItem(
    val record: DeviceHistoryRecord,
    val lastPlayedAtEpochMillis: Long,
    val playCount: Long,
) {
    init {
        require(lastPlayedAtEpochMillis >= 0) { "lastPlayedAtEpochMillis must not be negative" }
        require(playCount > 0) { "playCount must be positive" }
    }
}

package com.resonote.core.database.history

import androidx.room.Entity
import androidx.room.Index
import com.resonote.core.model.DeviceHistoryItem
import com.resonote.core.model.DeviceHistoryRecord
import com.resonote.core.model.DeviceHistorySource

@Entity(
    tableName = "device_playback_history",
    primaryKeys = ["source", "mediaId"],
    indices = [Index("lastPlayedAtEpochMillis")],
)
data class DeviceHistoryEntity(
    val source: String,
    val mediaId: String,
    val title: String,
    val artist: String?,
    val albumTitle: String?,
    val artworkUri: String?,
    val durationMillis: Long,
    val albumAudioId: String?,
    val lastPlayedAtEpochMillis: Long,
    val playCount: Long,
)

fun DeviceHistoryRecord.toEntity(playedAtEpochMillis: Long) = DeviceHistoryEntity(
    source = source.storageValue,
    mediaId = mediaId,
    title = title,
    artist = artist,
    albumTitle = albumTitle,
    artworkUri = artworkUri,
    durationMillis = durationMillis,
    albumAudioId = albumAudioId,
    lastPlayedAtEpochMillis = playedAtEpochMillis,
    playCount = 1,
)

fun DeviceHistoryEntity.asExternalModel() = DeviceHistoryItem(
    record =
    DeviceHistoryRecord(
        source = source.asHistorySource(),
        mediaId = mediaId,
        title = title,
        artist = artist,
        albumTitle = albumTitle,
        artworkUri = artworkUri,
        durationMillis = durationMillis,
        albumAudioId = albumAudioId,
    ),
    lastPlayedAtEpochMillis = lastPlayedAtEpochMillis,
    playCount = playCount,
)

private val DeviceHistorySource.storageValue: String
    get() = when (this) {
        DeviceHistorySource.Local -> "local"
        DeviceHistorySource.Cloud -> "cloud"
    }

private fun String.asHistorySource(): DeviceHistorySource = when (this) {
    "local" -> DeviceHistorySource.Local
    "cloud" -> DeviceHistorySource.Cloud
    else -> error("Unknown device history source")
}

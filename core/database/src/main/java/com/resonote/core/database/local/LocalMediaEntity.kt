package com.resonote.core.database.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.resonote.core.model.LocalMedia
import com.resonote.core.model.LocalMediaId
import java.io.File

@Entity(
    tableName = "local_media",
    indices = [
        Index(value = ["storagePath"], unique = true),
        Index(value = ["sizeBytes", "sha256"]),
        Index(value = ["importedAtEpochMillis"]),
    ],
)
data class LocalMediaEntity(
    @PrimaryKey val id: String,
    val storagePath: String,
    val displayName: String,
    val title: String,
    val artist: String?,
    val albumTitle: String?,
    val artworkPath: String?,
    val durationMillis: Long,
    val mimeType: String?,
    val fileExtension: String?,
    val sizeBytes: Long,
    val sha256: String,
    val sampleRateHz: Int?,
    val bitDepth: Int?,
    val bitrateBitsPerSecond: Int?,
    val importedAtEpochMillis: Long,
    val pendingDeletion: Boolean = false,
)

fun LocalMediaEntity.asExternalModel() = LocalMedia(
    id = LocalMediaId(id),
    displayName = displayName,
    title = title,
    artist = artist,
    albumTitle = albumTitle,
    artworkUri = artworkPath?.let { File(it).toURI().toString() },
    durationMillis = durationMillis,
    mimeType = mimeType,
    fileExtension = fileExtension,
    sizeBytes = sizeBytes,
    sampleRateHz = sampleRateHz,
    bitDepth = bitDepth,
    bitrateBitsPerSecond = bitrateBitsPerSecond,
    importedAtEpochMillis = importedAtEpochMillis,
)

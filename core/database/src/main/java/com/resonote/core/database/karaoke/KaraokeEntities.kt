package com.resonote.core.database.karaoke

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.resonote.core.model.KaraokeAudioAssetKind
import com.resonote.core.model.KaraokeMixSettings
import com.resonote.core.model.KaraokeProject
import com.resonote.core.model.KaraokeProjectId
import com.resonote.core.model.KaraokeProjectStatus
import com.resonote.core.model.KaraokeRecordingSegment
import com.resonote.core.model.KaraokeSourceMode

@Entity(
    tableName = "karaoke_projects",
    indices = [Index("updatedAtEpochMillis"), Index("status")],
)
data class KaraokeProjectEntity(
    @PrimaryKey val id: String,
    val songHash: String,
    val songTitle: String,
    val artist: String?,
    val artworkUri: String?,
    val sourceMode: String,
    val trimStartMillis: Long,
    val status: String,
    val vocalGainDb: Float,
    val accompanimentGainDb: Float,
    val vocalLowEqDb: Float,
    val vocalMidEqDb: Float,
    val vocalHighEqDb: Float,
    val vocalOffsetMillis: Int,
    val durationMillis: Long,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val exportedContentUri: String?,
)

@Entity(
    tableName = "karaoke_audio_assets",
    foreignKeys = [
        ForeignKey(
            entity = KaraokeProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId"), Index(value = ["storagePath"], unique = true)],
)
data class KaraokeAudioAssetEntity(
    @PrimaryKey val id: String,
    val projectId: String?,
    val kind: String,
    val storagePath: String,
    val mimeType: String?,
    val sourceHash: String?,
    val durationMillis: Long,
    val sizeBytes: Long,
    val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "karaoke_recording_segments",
    foreignKeys = [
        ForeignKey(
            entity = KaraokeProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = KaraokeAudioAssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["assetId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId"), Index(value = ["assetId"], unique = true)],
)
data class KaraokeRecordingSegmentEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val assetId: String,
    val timelineStartMillis: Long,
    val durationMillis: Long,
    val sampleRateHz: Int,
    val channelCount: Int,
    val peakAmplitude: Int,
    val nonSilent: Boolean,
    val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "karaoke_backing_segments",
    foreignKeys = [
        ForeignKey(
            entity = KaraokeProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = KaraokeAudioAssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["assetId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId"), Index("assetId")],
)
data class KaraokeBackingSegmentEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val assetId: String,
    val sourceMode: String,
    val timelineStartMillis: Long,
    val createdAtEpochMillis: Long,
)

fun KaraokeProjectEntity.asExternalModel() = KaraokeProject(
    id = KaraokeProjectId(id),
    songHash = songHash,
    songTitle = songTitle,
    artist = artist,
    artworkUri = artworkUri,
    sourceMode = KaraokeSourceMode.valueOf(sourceMode),
    trimStartMillis = trimStartMillis,
    status = KaraokeProjectStatus.valueOf(status),
    mixSettings = KaraokeMixSettings(
        vocalGainDb = vocalGainDb,
        accompanimentGainDb = accompanimentGainDb,
        vocalLowEqDb = vocalLowEqDb,
        vocalMidEqDb = vocalMidEqDb,
        vocalHighEqDb = vocalHighEqDb,
        vocalOffsetMillis = vocalOffsetMillis,
    ),
    durationMillis = durationMillis,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
    exportedContentUri = exportedContentUri,
)

fun KaraokeRecordingSegmentEntity.asExternalModel() = KaraokeRecordingSegment(
    id = id,
    projectId = KaraokeProjectId(projectId),
    timelineStartMillis = timelineStartMillis,
    durationMillis = durationMillis,
    sampleRateHz = sampleRateHz,
    channelCount = channelCount,
    peakAmplitude = peakAmplitude,
    nonSilent = nonSilent,
)

fun KaraokeAudioAssetKind.storageValue(): String = name

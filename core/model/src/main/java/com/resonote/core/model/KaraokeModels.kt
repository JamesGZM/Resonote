package com.resonote.core.model

@JvmInline
value class KaraokeProjectId(val value: String)

@JvmInline
value class KaraokeAssetId(val value: String)

enum class KaraokeSourceMode {
    Accompaniment,
    Original,
    Mixed,
}

enum class KaraokeProjectStatus {
    Draft,
    Edited,
    Exporting,
    Exported,
    ExportFailed,
}

enum class KaraokeAudioAssetKind {
    Accompaniment,
    Original,
    VocalSegment,
}

data class KaraokeMixSettings(
    val vocalGainDb: Float = 0f,
    val accompanimentGainDb: Float = 0f,
    val vocalLowEqDb: Float = 0f,
    val vocalMidEqDb: Float = 0f,
    val vocalHighEqDb: Float = 0f,
    val vocalOffsetMillis: Int = 0,
)

data class KaraokeProject(
    val id: KaraokeProjectId,
    val songHash: String,
    val songTitle: String,
    val artist: String?,
    val artworkUri: String?,
    val sourceMode: KaraokeSourceMode,
    val trimStartMillis: Long,
    val status: KaraokeProjectStatus,
    val mixSettings: KaraokeMixSettings,
    val durationMillis: Long,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val exportedContentUri: String?,
)

data class KaraokeBackingSegment(
    val id: String,
    val projectId: KaraokeProjectId,
    val assetId: KaraokeAssetId,
    val sourceMode: KaraokeSourceMode,
    val timelineStartMillis: Long,
)

data class KaraokeRecordingSegment(
    val id: String,
    val projectId: KaraokeProjectId,
    val timelineStartMillis: Long,
    val durationMillis: Long,
    val sampleRateHz: Int,
    val channelCount: Int,
    val peakAmplitude: Int,
    val nonSilent: Boolean,
)

package com.resonote.core.data

import com.resonote.core.model.KaraokeAssetId
import com.resonote.core.model.KaraokeBackingSegment
import com.resonote.core.model.KaraokeMixSettings
import com.resonote.core.model.KaraokeProject
import com.resonote.core.model.KaraokeProjectId
import com.resonote.core.model.KaraokeRecordingSegment
import com.resonote.core.model.KaraokeSourceMode
import com.resonote.core.model.ResolvedSongSource
import kotlinx.coroutines.flow.Flow

data class PreparedKaraokeProject(
    val project: KaraokeProject,
    val sources: Map<KaraokeSourceMode, PreparedKaraokeSource>,
)

data class PreparedKaraokeSource(
    val assetId: KaraokeAssetId,
    val uri: String,
    val extension: String?,
    val durationMillis: Long,
)

data class KaraokePreparationRequest(
    val songHash: String,
    val songTitle: String,
    val artist: String?,
    val artworkUri: String?,
    val durationMillis: Long,
    val albumAudioId: String?,
    val originalSource: ResolvedSongSource,
    val accompanimentLookupEnabled: Boolean,
    val timelineStartMillis: Long,
)

sealed interface PrepareKaraokeResult {
    data class Ready(val value: PreparedKaraokeProject) : PrepareKaraokeResult

    data class Failed(val reason: KaraokePreparationFailure) : PrepareKaraokeResult
}

enum class KaraokePreparationFailure {
    SourceUnavailable,
    InsufficientStorage,
    StorageUnavailable,
}

sealed interface KaraokeRecordingFileResult {
    data class Ready(val segmentId: String, val path: String) : KaraokeRecordingFileResult

    data object InsufficientStorage : KaraokeRecordingFileResult

    data object Failed : KaraokeRecordingFileResult
}

enum class KaraokeRecordingCommitResult {
    Saved,
    Discarded,
    Failed,
}

data class KaraokeRenderInput(
    val project: KaraokeProject,
    val backingSegments: List<KaraokeRenderBackingSegment>,
    val segments: List<KaraokeRenderSegment>,
)

data class KaraokeRenderBackingSegment(val path: String, val durationMillis: Long, val segment: KaraokeBackingSegment)

data class KaraokeRenderSegment(val path: String, val segment: KaraokeRecordingSegment)

interface KaraokeRepository {
    fun observeProjects(): Flow<List<KaraokeProject>>

    suspend fun findProject(id: KaraokeProjectId): KaraokeProject?

    suspend fun prepareProject(request: KaraokePreparationRequest): PrepareKaraokeResult

    suspend fun selectBackingSource(
        projectId: KaraokeProjectId,
        sourceMode: KaraokeSourceMode,
        timelineStartMillis: Long,
    ): Boolean

    suspend fun setTrimStart(projectId: KaraokeProjectId, trimStartMillis: Long): Boolean

    suspend fun createRecordingFile(
        projectId: KaraokeProjectId,
        expectedDurationMillis: Long,
    ): KaraokeRecordingFileResult

    suspend fun commitRecordingSegment(
        projectId: KaraokeProjectId,
        segmentId: String,
        path: String,
        timelineStartMillis: Long,
        durationMillis: Long,
        peakAmplitude: Int,
    ): KaraokeRecordingCommitResult

    suspend fun updateMix(projectId: KaraokeProjectId, settings: KaraokeMixSettings): Boolean

    suspend fun renderInput(projectId: KaraokeProjectId): KaraokeRenderInput?

    suspend fun updateExportStatus(
        projectId: KaraokeProjectId,
        status: com.resonote.core.model.KaraokeProjectStatus,
        exportedContentUri: String? = null,
    ): Boolean

    suspend fun deleteProjects(projectIds: Set<KaraokeProjectId>): Boolean
}

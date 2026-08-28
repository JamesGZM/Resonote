package com.resonote.core.data

import com.resonote.core.database.karaoke.KaraokeAudioAssetEntity
import com.resonote.core.database.karaoke.KaraokeBackingSegmentEntity
import com.resonote.core.database.karaoke.KaraokeDao
import com.resonote.core.database.karaoke.KaraokeProjectEntity
import com.resonote.core.database.karaoke.KaraokeRecordingSegmentEntity
import com.resonote.core.database.karaoke.asExternalModel
import com.resonote.core.media.karaoke.KaraokeAssetStore
import com.resonote.core.media.karaoke.KaraokeStoreFailure
import com.resonote.core.media.karaoke.KaraokeStoreResult
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.KaraokeAssetId
import com.resonote.core.model.KaraokeAudioAssetKind
import com.resonote.core.model.KaraokeBackingSegment
import com.resonote.core.model.KaraokeMixSettings
import com.resonote.core.model.KaraokeProjectId
import com.resonote.core.model.KaraokeProjectStatus
import com.resonote.core.model.KaraokeSourceMode
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.ResolveSongSourceResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultKaraokeRepository @Inject constructor(
    private val dao: KaraokeDao,
    private val network: com.resonote.core.network.KaraokeNetworkDataSource,
    private val playbackRepository: SongPlaybackRepository,
    private val store: KaraokeAssetStore,
) : KaraokeRepository {
    private val mutationMutex = Mutex()

    override fun observeProjects() = dao.observeProjects().map { rows -> rows.map { it.asExternalModel() } }

    override suspend fun findProject(id: KaraokeProjectId) = dao.findProject(id.value)?.asExternalModel()

    override suspend fun prepareProject(request: KaraokePreparationRequest): PrepareKaraokeResult =
        mutationMutex.withLock {
            val accompaniment = if (request.accompanimentLookupEnabled) {
                runCatching {
                    network.matchAccompaniment(
                        originalHash = request.songHash,
                        albumAudioId = request.albumAudioId,
                        fileName = listOfNotNull(request.artist, request.songTitle).joinToString(" - "),
                    )
                }.getOrNull()
            } else {
                null
            }
            val accompanimentSong = accompaniment?.let { matched ->
                OnlineSong(
                    hash = matched.hash,
                    title = matched.songName ?: request.songTitle,
                    artist = matched.singerName ?: request.artist,
                    coverUrl = request.artworkUri,
                    albumId = null,
                    albumAudioId = matched.songId?.toString(),
                    durationMillis = matched.durationMillis.takeIf { it > 0 } ?: request.durationMillis,
                    quality = AudioQuality.HighQuality,
                    vip = false,
                )
            }
            val resolvedAccompaniment = accompanimentSong?.let { resolve(it) }
            val projectId = UUID.randomUUID().toString()
            val candidates = buildList {
                add(KaraokeSourceMode.Original to (request.originalSource to request.songHash))
                resolvedAccompaniment?.let { add(KaraokeSourceMode.Accompaniment to (it to accompanimentSong.hash)) }
            }
            val preparedSources = linkedMapOf<KaraokeSourceMode, PreparedKaraokeSource>()
            val assets = mutableListOf<KaraokeAudioAssetEntity>()
            val now = System.currentTimeMillis()
            candidates.forEach candidate@{ (mode, sourceAndHash) ->
                val (source, sourceHash) = sourceAndHash
                val assetId = UUID.randomUUID().toString()
                val stored = when (
                    val persisted = store.persistSource(projectId, assetId, source.uri, source.extension)
                ) {
                    is KaraokeStoreResult.Success -> persisted.value
                    is KaraokeStoreResult.Failure -> {
                        if (mode == KaraokeSourceMode.Accompaniment) return@candidate
                        store.removeProject(projectId)
                        return@withLock PrepareKaraokeResult.Failed(persisted.reason.toPreparationFailure())
                    }
                }
                assets += KaraokeAudioAssetEntity(
                    id = assetId,
                    projectId = projectId,
                    kind = mode.name,
                    storagePath = stored.path,
                    mimeType = source.extension?.let { "audio/$it" },
                    sourceHash = sourceHash,
                    durationMillis = source.durationMillis,
                    sizeBytes = stored.sizeBytes,
                    createdAtEpochMillis = now,
                )
                preparedSources[mode] = PreparedKaraokeSource(
                    assetId = KaraokeAssetId(assetId),
                    uri = File(stored.path).toURI().toString(),
                    extension = source.extension,
                    durationMillis = source.durationMillis,
                )
            }
            val sourceMode = if (preparedSources.containsKey(KaraokeSourceMode.Accompaniment)) {
                KaraokeSourceMode.Accompaniment
            } else {
                KaraokeSourceMode.Original
            }
            val initialSource = preparedSources.getValue(sourceMode)
            val project = KaraokeProjectEntity(
                id = projectId,
                songHash = request.songHash,
                songTitle = request.songTitle,
                artist = request.artist,
                artworkUri = request.artworkUri,
                sourceMode = sourceMode.name,
                trimStartMillis = request.timelineStartMillis,
                status = KaraokeProjectStatus.Draft.name,
                vocalGainDb = 0f,
                accompanimentGainDb = 0f,
                vocalLowEqDb = 0f,
                vocalMidEqDb = 0f,
                vocalHighEqDb = 0f,
                vocalOffsetMillis = 0,
                durationMillis = request.originalSource.durationMillis,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
                exportedContentUri = null,
            )
            val backingSegment = KaraokeBackingSegmentEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                assetId = initialSource.assetId.value,
                sourceMode = sourceMode.name,
                timelineStartMillis = request.timelineStartMillis,
                createdAtEpochMillis = now,
            )
            try {
                dao.insertProjectWithAssets(project, assets, backingSegment)
            } catch (cancelled: CancellationException) {
                store.removeProject(projectId)
                throw cancelled
            } catch (_: Exception) {
                store.removeProject(projectId)
                return@withLock PrepareKaraokeResult.Failed(KaraokePreparationFailure.StorageUnavailable)
            }
            PrepareKaraokeResult.Ready(
                PreparedKaraokeProject(
                    project = project.asExternalModel(),
                    sources = preparedSources,
                ),
            )
        }

    override suspend fun selectBackingSource(
        projectId: KaraokeProjectId,
        sourceMode: KaraokeSourceMode,
        timelineStartMillis: Long,
    ): Boolean = mutationMutex.withLock {
        if (sourceMode == KaraokeSourceMode.Mixed) return@withLock false
        val project = dao.findProject(projectId.value) ?: return@withLock false
        val asset = dao.findAssets(projectId.value).firstOrNull { it.kind == sourceMode.name }
            ?: return@withLock false
        val latestSegment = dao.findBackingSegments(projectId.value).lastOrNull()
        val updatedMode = if (latestSegment?.timelineStartMillis == timelineStartMillis) {
            sourceMode
        } else {
            when (KaraokeSourceMode.valueOf(project.sourceMode)) {
                sourceMode -> sourceMode
                KaraokeSourceMode.Mixed -> KaraokeSourceMode.Mixed
                else -> KaraokeSourceMode.Mixed
            }
        }
        runCatching {
            dao.insertBackingSegment(
                KaraokeBackingSegmentEntity(
                    id = UUID.randomUUID().toString(),
                    projectId = projectId.value,
                    assetId = asset.id,
                    sourceMode = sourceMode.name,
                    timelineStartMillis = timelineStartMillis,
                    createdAtEpochMillis = System.currentTimeMillis(),
                ),
            )
            dao.updateProject(
                project.copy(sourceMode = updatedMode.name, updatedAtEpochMillis = System.currentTimeMillis()),
            )
        }.isSuccess
    }

    override suspend fun setTrimStart(projectId: KaraokeProjectId, trimStartMillis: Long): Boolean =
        mutationMutex.withLock {
            val project = dao.findProject(projectId.value) ?: return@withLock false
            runCatching {
                dao.updateProject(
                    project.copy(
                        trimStartMillis = trimStartMillis.coerceAtLeast(0L),
                        updatedAtEpochMillis = System.currentTimeMillis(),
                    ),
                )
            }.isSuccess
        }

    override suspend fun createRecordingFile(
        projectId: KaraokeProjectId,
        expectedDurationMillis: Long,
    ): KaraokeRecordingFileResult {
        if (!store.hasRecordingCapacity(expectedDurationMillis)) return KaraokeRecordingFileResult.InsufficientStorage
        val segmentId = UUID.randomUUID().toString()
        return when (val result = store.createRecordingFile(projectId.value, segmentId)) {
            is KaraokeStoreResult.Success -> KaraokeRecordingFileResult.Ready(segmentId, result.value)
            is KaraokeStoreResult.Failure -> if (result.reason == KaraokeStoreFailure.InsufficientStorage) {
                KaraokeRecordingFileResult.InsufficientStorage
            } else {
                KaraokeRecordingFileResult.Failed
            }
        }
    }

    override suspend fun commitRecordingSegment(
        projectId: KaraokeProjectId,
        segmentId: String,
        path: String,
        timelineStartMillis: Long,
        durationMillis: Long,
        peakAmplitude: Int,
    ): Boolean = mutationMutex.withLock {
        val file = File(path)
        val valid = durationMillis >= MIN_VALID_TAKE_MILLIS && peakAmplitude >= MIN_NON_SILENT_PEAK && file.length() > 0
        if (!valid) {
            file.delete()
            return@withLock false
        }
        val project = dao.findProject(projectId.value) ?: return@withLock false
        val now = System.currentTimeMillis()
        val assetId = UUID.randomUUID().toString()
        try {
            dao.insertRecordingSegment(
                asset = KaraokeAudioAssetEntity(
                    id = assetId,
                    projectId = projectId.value,
                    kind = KaraokeAudioAssetKind.VocalSegment.name,
                    storagePath = file.absolutePath,
                    mimeType = "audio/wav",
                    sourceHash = null,
                    durationMillis = durationMillis,
                    sizeBytes = file.length(),
                    createdAtEpochMillis = now,
                ),
                segment = KaraokeRecordingSegmentEntity(
                    id = segmentId,
                    projectId = projectId.value,
                    assetId = assetId,
                    timelineStartMillis = timelineStartMillis,
                    durationMillis = durationMillis,
                    sampleRateHz = SAMPLE_RATE_HZ,
                    channelCount = CHANNEL_COUNT,
                    peakAmplitude = peakAmplitude,
                    nonSilent = true,
                    createdAtEpochMillis = now,
                ),
            )
            dao.updateProject(
                project.copy(
                    durationMillis = maxOf(project.durationMillis, timelineStartMillis + durationMillis),
                    updatedAtEpochMillis = now,
                ),
            )
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun updateMix(projectId: KaraokeProjectId, settings: KaraokeMixSettings): Boolean =
        mutationMutex.withLock {
            val project = dao.findProject(projectId.value) ?: return@withLock false
            val normalized = settings.normalized()
            runCatching {
                dao.updateProject(
                    project.copy(
                        status = KaraokeProjectStatus.Edited.name,
                        vocalGainDb = normalized.vocalGainDb,
                        accompanimentGainDb = normalized.accompanimentGainDb,
                        vocalLowEqDb = normalized.vocalLowEqDb,
                        vocalMidEqDb = normalized.vocalMidEqDb,
                        vocalHighEqDb = normalized.vocalHighEqDb,
                        vocalOffsetMillis = normalized.vocalOffsetMillis,
                        updatedAtEpochMillis = System.currentTimeMillis(),
                    ),
                )
            }.isSuccess
        }

    override suspend fun renderInput(projectId: KaraokeProjectId): KaraokeRenderInput? {
        val project = dao.findProject(projectId.value) ?: return null
        val assets = dao.findAssets(projectId.value)
        val paths = assets.associateBy { it.id }
        val backingSegments = dao.findBackingSegments(projectId.value).mapNotNull { segment ->
            val asset = paths[segment.assetId] ?: return@mapNotNull null
            KaraokeRenderBackingSegment(
                path = asset.storagePath,
                durationMillis = asset.durationMillis,
                segment = KaraokeBackingSegment(
                    id = segment.id,
                    projectId = projectId,
                    assetId = KaraokeAssetId(segment.assetId),
                    sourceMode = KaraokeSourceMode.valueOf(segment.sourceMode),
                    timelineStartMillis = segment.timelineStartMillis,
                ),
            )
        }
        if (backingSegments.isEmpty()) return null
        val segments = dao.findSegments(projectId.value).mapNotNull { segment ->
            val asset = paths[segment.assetId] ?: return@mapNotNull null
            KaraokeRenderSegment(asset.storagePath, segment.asExternalModel())
        }
        return KaraokeRenderInput(project.asExternalModel(), backingSegments, segments)
    }

    override suspend fun updateExportStatus(
        projectId: KaraokeProjectId,
        status: KaraokeProjectStatus,
        exportedContentUri: String?,
    ): Boolean = mutationMutex.withLock {
        val project = dao.findProject(projectId.value) ?: return@withLock false
        runCatching {
            dao.updateProject(
                project.copy(
                    status = status.name,
                    exportedContentUri = exportedContentUri ?: project.exportedContentUri,
                    updatedAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        }.isSuccess
    }

    override suspend fun deleteProjects(projectIds: Set<KaraokeProjectId>): Boolean = mutationMutex.withLock {
        if (projectIds.isEmpty()) return@withLock true
        val ids = projectIds.mapTo(linkedSetOf()) { it.value }
        val deleted = runCatching { dao.deleteProjects(ids) }.getOrElse { return@withLock false }
        if (deleted != ids.size) return@withLock false
        ids.forEach { store.removeProject(it) }
        true
    }

    private suspend fun resolve(song: OnlineSong) =
        (playbackRepository.resolveSource(song) as? ResolveSongSourceResult.Resolved)?.source

    private fun KaraokeStoreFailure.toPreparationFailure() = when (this) {
        KaraokeStoreFailure.InsufficientStorage -> KaraokePreparationFailure.InsufficientStorage
        KaraokeStoreFailure.InvalidSource,
        KaraokeStoreFailure.SourceUnavailable,
        -> KaraokePreparationFailure.SourceUnavailable
        KaraokeStoreFailure.StorageUnavailable -> KaraokePreparationFailure.StorageUnavailable
    }

    private fun KaraokeMixSettings.normalized() = copy(
        vocalGainDb = vocalGainDb.coerceIn(-12f, 12f),
        accompanimentGainDb = accompanimentGainDb.coerceIn(-12f, 12f),
        vocalLowEqDb = vocalLowEqDb.coerceIn(-12f, 12f),
        vocalMidEqDb = vocalMidEqDb.coerceIn(-12f, 12f),
        vocalHighEqDb = vocalHighEqDb.coerceIn(-12f, 12f),
        vocalOffsetMillis = vocalOffsetMillis.coerceIn(-200, 200),
    )

    private companion object {
        const val MIN_VALID_TAKE_MILLIS = 1_000L
        const val MIN_NON_SILENT_PEAK = 256
        const val SAMPLE_RATE_HZ = 48_000
        const val CHANNEL_COUNT = 1
    }
}

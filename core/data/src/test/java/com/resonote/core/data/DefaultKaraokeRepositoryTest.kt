package com.resonote.core.data

import com.google.common.truth.Truth.assertThat
import com.resonote.core.database.karaoke.KaraokeAudioAssetEntity
import com.resonote.core.database.karaoke.KaraokeBackingSegmentEntity
import com.resonote.core.database.karaoke.KaraokeDao
import com.resonote.core.database.karaoke.KaraokeProjectEntity
import com.resonote.core.database.karaoke.KaraokeRecordingSegmentEntity
import com.resonote.core.media.karaoke.KaraokeAssetStore
import com.resonote.core.media.karaoke.KaraokeStoreFailure
import com.resonote.core.media.karaoke.KaraokeStoreResult
import com.resonote.core.media.karaoke.KaraokeStoredAsset
import com.resonote.core.model.KaraokeSourceMode
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.ResolveSongSourceResult
import com.resonote.core.model.ResolvedSongSource
import com.resonote.core.network.KaraokeNetworkDataSource
import com.resonote.core.network.NetworkKaraokeAccompaniment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File

class DefaultKaraokeRepositoryTest {
    @Test
    fun prepareProjectFallsBackToOriginalWhenAccompanimentPersistenceFails() = runTest {
        val dao = FakeKaraokeDao()
        val store = FakeKaraokeAssetStore()
        val repository = DefaultKaraokeRepository(
            dao = dao,
            network = FakeKaraokeNetworkDataSource,
            playbackRepository = FakeSongPlaybackRepository,
            store = store,
        )

        val result = repository.prepareProject(
            KaraokePreparationRequest(
                songHash = "original-hash",
                songTitle = "Song",
                artist = "Artist",
                artworkUri = null,
                durationMillis = 60_000,
                albumAudioId = "1",
                originalSource = ResolvedSongSource("https://original", 60_000, "mp3"),
                accompanimentLookupEnabled = true,
                timelineStartMillis = 0,
            ),
        )

        assertThat(result).isInstanceOf(PrepareKaraokeResult.Ready::class.java)
        val prepared = (result as PrepareKaraokeResult.Ready).value
        assertThat(prepared.sources.keys).containsExactly(KaraokeSourceMode.Original)
        assertThat(prepared.project.sourceMode).isEqualTo(KaraokeSourceMode.Original)
        assertThat(dao.assets.map { it.kind }).containsExactly(KaraokeSourceMode.Original.name)
        assertThat(store.removedProjectIds).isEmpty()
    }

    @Test
    fun commitRecordingSegmentReportsDiscardedForShortTake() = runTest {
        val repository = DefaultKaraokeRepository(
            dao = FakeKaraokeDao(),
            network = FakeKaraokeNetworkDataSource,
            playbackRepository = FakeSongPlaybackRepository,
            store = FakeKaraokeAssetStore(),
        )
        val project = prepareProject(repository)
        val recording = recordingFile()

        val result = repository.commitRecordingSegment(
            projectId = project.project.id,
            segmentId = "short-segment",
            path = recording.absolutePath,
            timelineStartMillis = 0,
            durationMillis = 500,
            peakAmplitude = 1_000,
        )

        assertThat(result).isEqualTo(KaraokeRecordingCommitResult.Discarded)
        assertThat(recording.exists()).isFalse()
    }

    @Test
    fun commitRecordingSegmentReportsFailedForPersistenceError() = runTest {
        val dao = FakeKaraokeDao()
        val repository = DefaultKaraokeRepository(
            dao = dao,
            network = FakeKaraokeNetworkDataSource,
            playbackRepository = FakeSongPlaybackRepository,
            store = FakeKaraokeAssetStore(),
        )
        val project = prepareProject(repository)
        val recording = recordingFile()
        dao.failSegmentInsert = true

        val result = repository.commitRecordingSegment(
            projectId = project.project.id,
            segmentId = "failed-segment",
            path = recording.absolutePath,
            timelineStartMillis = 0,
            durationMillis = 2_000,
            peakAmplitude = 1_000,
        )

        assertThat(result).isEqualTo(KaraokeRecordingCommitResult.Failed)
        recording.delete()
    }

    @Test
    fun commitRecordingSegmentReplacesSongDurationWithRecordedTimelineEnd() = runTest {
        val dao = FakeKaraokeDao()
        val repository = DefaultKaraokeRepository(
            dao = dao,
            network = FakeKaraokeNetworkDataSource,
            playbackRepository = FakeSongPlaybackRepository,
            store = FakeKaraokeAssetStore(),
        )
        val project = prepareProject(repository)
        val recording = recordingFile()

        val result = repository.commitRecordingSegment(
            projectId = project.project.id,
            segmentId = "three-second-take",
            path = recording.absolutePath,
            timelineStartMillis = 12_000,
            durationMillis = 3_000,
            peakAmplitude = 1_000,
        )

        assertThat(result).isEqualTo(KaraokeRecordingCommitResult.Saved)
        assertThat(dao.findProject(project.project.id.value)?.durationMillis).isEqualTo(15_000)
    }

    @Test
    fun findProjectRepairsLegacySongDurationFromRecordedSegments() = runTest {
        val dao = FakeKaraokeDao()
        val repository = DefaultKaraokeRepository(
            dao = dao,
            network = FakeKaraokeNetworkDataSource,
            playbackRepository = FakeSongPlaybackRepository,
            store = FakeKaraokeAssetStore(),
        )
        val project = prepareProject(repository)
        dao.insertSegment(
            KaraokeRecordingSegmentEntity(
                id = "legacy-segment",
                projectId = project.project.id.value,
                assetId = "legacy-asset",
                timelineStartMillis = 12_000,
                durationMillis = 3_000,
                sampleRateHz = 48_000,
                channelCount = 1,
                peakAmplitude = 1_000,
                nonSilent = true,
                createdAtEpochMillis = 1,
            ),
        )

        assertThat(repository.findProject(project.project.id)?.durationMillis).isEqualTo(15_000)
    }

    private suspend fun prepareProject(repository: DefaultKaraokeRepository): PreparedKaraokeProject {
        val result = repository.prepareProject(
            KaraokePreparationRequest(
                songHash = "original-hash",
                songTitle = "Song",
                artist = "Artist",
                artworkUri = null,
                durationMillis = 60_000,
                albumAudioId = "1",
                originalSource = ResolvedSongSource("https://original", 60_000, "mp3"),
                accompanimentLookupEnabled = false,
                timelineStartMillis = 0,
            ),
        )
        return (result as PrepareKaraokeResult.Ready).value
    }

    private fun recordingFile() = File.createTempFile("resonote-karaoke-recording", ".wav").apply {
        writeBytes(byteArrayOf(1, 2, 3, 4))
        deleteOnExit()
    }

    private class FakeKaraokeDao : KaraokeDao {
        val assets = mutableListOf<KaraokeAudioAssetEntity>()
        var failSegmentInsert = false
        private val projects = mutableMapOf<String, KaraokeProjectEntity>()
        private val backingSegments = mutableListOf<KaraokeBackingSegmentEntity>()
        private val recordingSegments = mutableListOf<KaraokeRecordingSegmentEntity>()

        override fun observeProjects(): Flow<List<KaraokeProjectEntity>> = flowOf(projects.values.toList())

        override suspend fun findProject(projectId: String) = projects[projectId]

        override suspend fun findSegments(projectId: String) = recordingSegments.filter { it.projectId == projectId }

        override suspend fun findBackingSegments(projectId: String) =
            backingSegments.filter { it.projectId == projectId }

        override suspend fun findAssets(projectId: String) = assets.filter { it.projectId == projectId }

        override suspend fun insertProject(project: KaraokeProjectEntity) {
            projects[project.id] = project
        }

        override suspend fun insertAsset(asset: KaraokeAudioAssetEntity) {
            assets += asset
        }

        override suspend fun insertSegment(segment: KaraokeRecordingSegmentEntity) {
            if (failSegmentInsert) error("segment insert failed")
            recordingSegments += segment
        }

        override suspend fun insertBackingSegment(segment: KaraokeBackingSegmentEntity) {
            backingSegments += segment
        }

        override suspend fun updateProject(project: KaraokeProjectEntity) {
            projects[project.id] = project
        }

        override suspend fun deleteProjects(projectIds: Set<String>): Int {
            val existing = projectIds.count(projects::containsKey)
            projectIds.forEach(projects::remove)
            return existing
        }
    }

    private class FakeKaraokeAssetStore : KaraokeAssetStore {
        val removedProjectIds = mutableListOf<String>()

        override suspend fun persistSource(projectId: String, assetId: String, sourceUri: String, extension: String?) =
            if (sourceUri == "https://original") {
                KaraokeStoreResult.Success(KaraokeStoredAsset("/tmp/original.mp3", 1_024))
            } else {
                KaraokeStoreResult.Failure(KaraokeStoreFailure.SourceUnavailable)
            }

        override suspend fun createRecordingFile(projectId: String, segmentId: String) =
            KaraokeStoreResult.Failure(KaraokeStoreFailure.StorageUnavailable)

        override suspend fun hasRecordingCapacity(expectedDurationMillis: Long) = true

        override suspend fun removeProject(projectId: String): KaraokeStoreResult<Unit> {
            removedProjectIds += projectId
            return KaraokeStoreResult.Success(Unit)
        }
    }

    private object FakeKaraokeNetworkDataSource : KaraokeNetworkDataSource {
        override suspend fun matchAccompaniment(originalHash: String, albumAudioId: String?, fileName: String) =
            NetworkKaraokeAccompaniment(
                hash = "accompaniment-hash",
                songId = 2,
                songName = "Song",
                singerName = "Artist",
                durationMillis = 60_000,
                extension = "mp3",
                bitrateKbps = 128,
                sizeBytes = 1_024,
                remark = null,
                showMic = true,
            )
    }

    private object FakeSongPlaybackRepository : SongPlaybackRepository {
        override suspend fun resolveSource(
            song: OnlineSong,
            qualityOverride: com.resonote.core.model.OnlinePlaybackQuality?,
        ) = ResolveSongSourceResult.Resolved(
            ResolvedSongSource("https://accompaniment", song.durationMillis, "mp3"),
        )
    }
}

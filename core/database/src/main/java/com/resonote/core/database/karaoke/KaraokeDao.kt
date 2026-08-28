package com.resonote.core.database.karaoke

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KaraokeDao {
    @Query("SELECT * FROM karaoke_projects ORDER BY updatedAtEpochMillis DESC")
    fun observeProjects(): Flow<List<KaraokeProjectEntity>>

    @Query("SELECT * FROM karaoke_projects WHERE id = :projectId LIMIT 1")
    suspend fun findProject(projectId: String): KaraokeProjectEntity?

    @Query(
        "SELECT * FROM karaoke_recording_segments WHERE projectId = :projectId ORDER BY timelineStartMillis, createdAtEpochMillis",
    )
    suspend fun findSegments(projectId: String): List<KaraokeRecordingSegmentEntity>

    @Query(
        "SELECT * FROM karaoke_backing_segments WHERE projectId = :projectId ORDER BY timelineStartMillis, createdAtEpochMillis",
    )
    suspend fun findBackingSegments(projectId: String): List<KaraokeBackingSegmentEntity>

    @Query("SELECT * FROM karaoke_audio_assets WHERE projectId = :projectId ORDER BY createdAtEpochMillis")
    suspend fun findAssets(projectId: String): List<KaraokeAudioAssetEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProject(project: KaraokeProjectEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAsset(asset: KaraokeAudioAssetEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSegment(segment: KaraokeRecordingSegmentEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBackingSegment(segment: KaraokeBackingSegmentEntity)

    @Update
    suspend fun updateProject(project: KaraokeProjectEntity)

    @Query("DELETE FROM karaoke_projects WHERE id IN (:projectIds)")
    suspend fun deleteProjects(projectIds: Set<String>): Int

    @Transaction
    suspend fun insertProjectWithAsset(project: KaraokeProjectEntity, asset: KaraokeAudioAssetEntity) {
        insertProject(project)
        insertAsset(asset)
    }

    @Transaction
    suspend fun insertProjectWithAssets(
        project: KaraokeProjectEntity,
        assets: List<KaraokeAudioAssetEntity>,
        backingSegment: KaraokeBackingSegmentEntity,
    ) {
        insertProject(project)
        assets.forEach { insertAsset(it) }
        insertBackingSegment(backingSegment)
    }

    @Transaction
    suspend fun insertRecordingSegment(asset: KaraokeAudioAssetEntity, segment: KaraokeRecordingSegmentEntity) {
        insertAsset(asset)
        insertSegment(segment)
    }
}

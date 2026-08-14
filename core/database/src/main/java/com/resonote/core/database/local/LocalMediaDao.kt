package com.resonote.core.database.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalMediaDao {
    @Query("SELECT * FROM local_media ORDER BY importedAtEpochMillis DESC, id DESC")
    suspend fun findAllForRecovery(): List<LocalMediaEntity>

    @Query(
        "SELECT * FROM local_media WHERE pendingDeletion = 0 " +
            "ORDER BY importedAtEpochMillis DESC, id DESC",
    )
    fun observeAll(): Flow<List<LocalMediaEntity>>

    @Query("SELECT * FROM local_media WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): LocalMediaEntity?

    @Query(
        "SELECT * FROM local_media WHERE sizeBytes = :sizeBytes AND sha256 = :sha256 " +
            "AND pendingDeletion = 0 ORDER BY importedAtEpochMillis DESC",
    )
    suspend fun findDuplicates(sizeBytes: Long, sha256: String): List<LocalMediaEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: LocalMediaEntity)

    @Query("UPDATE local_media SET pendingDeletion = 1 WHERE id = :id AND pendingDeletion = 0")
    suspend fun markPendingDeletion(id: String): Int

    @Query("UPDATE local_media SET pendingDeletion = 0 WHERE id = :id AND pendingDeletion = 1")
    suspend fun restorePendingDeletion(id: String): Int

    @Query("DELETE FROM local_media WHERE id = :id")
    suspend fun delete(id: String): Int
}

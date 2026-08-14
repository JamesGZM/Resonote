package com.resonote.core.database.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceHistoryDao {
    @Query(
        "SELECT * FROM device_playback_history " +
            "ORDER BY lastPlayedAtEpochMillis DESC, source ASC, mediaId ASC",
    )
    fun observeAll(): Flow<List<DeviceHistoryEntity>>

    @Query(
        "SELECT * FROM device_playback_history " +
            "ORDER BY lastPlayedAtEpochMillis DESC, source ASC, mediaId ASC",
    )
    suspend fun findAll(): List<DeviceHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: DeviceHistoryEntity): Long

    @Query(
        """
        UPDATE device_playback_history SET
            title = :title,
            artist = :artist,
            albumTitle = :albumTitle,
            artworkUri = :artworkUri,
            durationMillis = :durationMillis,
            albumAudioId = :albumAudioId,
            lastPlayedAtEpochMillis = MAX(lastPlayedAtEpochMillis, :playedAtEpochMillis),
            playCount = playCount + 1
        WHERE source = :source AND mediaId = :mediaId
        """,
    )
    suspend fun increment(
        source: String,
        mediaId: String,
        title: String,
        artist: String?,
        albumTitle: String?,
        artworkUri: String?,
        durationMillis: Long,
        albumAudioId: String?,
        playedAtEpochMillis: Long,
    ): Int

    @Query(
        """
        DELETE FROM device_playback_history
        WHERE rowid NOT IN (
            SELECT rowid FROM device_playback_history
            ORDER BY lastPlayedAtEpochMillis DESC, source ASC, mediaId ASC
            LIMIT :limit
        )
        """,
    )
    suspend fun trimToLimit(limit: Int): Int

    @Query("DELETE FROM device_playback_history WHERE source = :source AND mediaId = :mediaId")
    suspend fun delete(source: String, mediaId: String): Int

    @Query("DELETE FROM device_playback_history")
    suspend fun clear(): Int

    @Transaction
    suspend fun record(entity: DeviceHistoryEntity, limit: Int = MAX_HISTORY_ITEMS) {
        require(limit > 0) { "limit must be positive" }
        val updated = entity.incrementExisting()
        if (updated == 0 && insert(entity) == -1L) entity.incrementExisting()
        trimToLimit(limit)
    }

    private suspend fun DeviceHistoryEntity.incrementExisting(): Int = increment(
        source = source,
        mediaId = mediaId,
        title = title,
        artist = artist,
        albumTitle = albumTitle,
        artworkUri = artworkUri,
        durationMillis = durationMillis,
        albumAudioId = albumAudioId,
        playedAtEpochMillis = lastPlayedAtEpochMillis,
    )

    companion object {
        const val MAX_HISTORY_ITEMS = 500
    }
}

package com.resonote.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.resonote.core.database.history.DeviceHistoryDao
import com.resonote.core.database.history.DeviceHistoryEntity
import com.resonote.core.database.karaoke.KaraokeAudioAssetEntity
import com.resonote.core.database.karaoke.KaraokeBackingSegmentEntity
import com.resonote.core.database.karaoke.KaraokeDao
import com.resonote.core.database.karaoke.KaraokeProjectEntity
import com.resonote.core.database.karaoke.KaraokeRecordingSegmentEntity
import com.resonote.core.database.local.LocalMediaDao
import com.resonote.core.database.local.LocalMediaEntity

@Database(
    entities = [
        LocalMediaEntity::class,
        DeviceHistoryEntity::class,
        KaraokeProjectEntity::class,
        KaraokeAudioAssetEntity::class,
        KaraokeRecordingSegmentEntity::class,
        KaraokeBackingSegmentEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class ResonoteDatabase : RoomDatabase() {
    abstract fun localMediaDao(): LocalMediaDao
    abstract fun deviceHistoryDao(): DeviceHistoryDao
    abstract fun karaokeDao(): KaraokeDao
}

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
import com.resonote.core.database.vip.VipCheckInDao
import com.resonote.core.database.vip.VipCheckInEntity

@Database(
    entities = [
        VipCheckInEntity::class,
        LocalMediaEntity::class,
        DeviceHistoryEntity::class,
        KaraokeProjectEntity::class,
        KaraokeAudioAssetEntity::class,
        KaraokeRecordingSegmentEntity::class,
        KaraokeBackingSegmentEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class ResonoteDatabase : RoomDatabase() {
    abstract fun vipCheckInDao(): VipCheckInDao
    abstract fun localMediaDao(): LocalMediaDao
    abstract fun deviceHistoryDao(): DeviceHistoryDao
    abstract fun karaokeDao(): KaraokeDao
}

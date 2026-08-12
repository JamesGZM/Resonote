package com.resonote.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.resonote.core.database.local.LocalMediaDao
import com.resonote.core.database.local.LocalMediaEntity

@Database(
    entities = [LocalMediaEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ResonoteDatabase : RoomDatabase() {
    abstract fun localMediaDao(): LocalMediaDao
}

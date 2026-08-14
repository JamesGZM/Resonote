package com.resonote.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.resonote.core.database.ResonoteDatabase
import com.resonote.core.database.history.DeviceHistoryDao
import com.resonote.core.database.local.LocalMediaDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ResonoteDatabase = Room.databaseBuilder(
        context,
        ResonoteDatabase::class.java,
        "resonote.db",
    ).addMigrations(MIGRATION_1_2).build()

    @Provides
    fun provideLocalMediaDao(database: ResonoteDatabase): LocalMediaDao = database.localMediaDao()

    @Provides
    fun provideDeviceHistoryDao(database: ResonoteDatabase): DeviceHistoryDao = database.deviceHistoryDao()

    internal val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `device_playback_history` (
                    `source` TEXT NOT NULL,
                    `mediaId` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `artist` TEXT,
                    `albumTitle` TEXT,
                    `artworkUri` TEXT,
                    `durationMillis` INTEGER NOT NULL,
                    `albumAudioId` TEXT,
                    `lastPlayedAtEpochMillis` INTEGER NOT NULL,
                    `playCount` INTEGER NOT NULL,
                    PRIMARY KEY(`source`, `mediaId`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_device_playback_history_lastPlayedAtEpochMillis` " +
                    "ON `device_playback_history` (`lastPlayedAtEpochMillis`)",
            )
        }
    }
}

package com.resonote.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.resonote.core.database.ResonoteDatabase
import com.resonote.core.database.history.DeviceHistoryDao
import com.resonote.core.database.karaoke.KaraokeDao
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
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()

    @Provides
    fun provideLocalMediaDao(database: ResonoteDatabase): LocalMediaDao = database.localMediaDao()

    @Provides
    fun provideDeviceHistoryDao(database: ResonoteDatabase): DeviceHistoryDao = database.deviceHistoryDao()

    @Provides
    fun provideKaraokeDao(database: ResonoteDatabase): KaraokeDao = database.karaokeDao()

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

    internal val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `karaoke_projects` (
                    `id` TEXT NOT NULL,
                    `songHash` TEXT NOT NULL,
                    `songTitle` TEXT NOT NULL,
                    `artist` TEXT,
                    `artworkUri` TEXT,
                    `sourceMode` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `vocalGainDb` REAL NOT NULL,
                    `accompanimentGainDb` REAL NOT NULL,
                    `vocalLowEqDb` REAL NOT NULL,
                    `vocalMidEqDb` REAL NOT NULL,
                    `vocalHighEqDb` REAL NOT NULL,
                    `vocalOffsetMillis` INTEGER NOT NULL,
                    `durationMillis` INTEGER NOT NULL,
                    `createdAtEpochMillis` INTEGER NOT NULL,
                    `updatedAtEpochMillis` INTEGER NOT NULL,
                    `exportedContentUri` TEXT,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_karaoke_projects_updatedAtEpochMillis` ON `karaoke_projects` (`updatedAtEpochMillis`)",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_karaoke_projects_status` ON `karaoke_projects` (`status`)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `karaoke_audio_assets` (
                    `id` TEXT NOT NULL,
                    `projectId` TEXT,
                    `kind` TEXT NOT NULL,
                    `storagePath` TEXT NOT NULL,
                    `mimeType` TEXT,
                    `sourceHash` TEXT,
                    `durationMillis` INTEGER NOT NULL,
                    `sizeBytes` INTEGER NOT NULL,
                    `createdAtEpochMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`projectId`) REFERENCES `karaoke_projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_karaoke_audio_assets_projectId` ON `karaoke_audio_assets` (`projectId`)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_karaoke_audio_assets_storagePath` ON `karaoke_audio_assets` (`storagePath`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `karaoke_recording_segments` (
                    `id` TEXT NOT NULL,
                    `projectId` TEXT NOT NULL,
                    `assetId` TEXT NOT NULL,
                    `timelineStartMillis` INTEGER NOT NULL,
                    `durationMillis` INTEGER NOT NULL,
                    `sampleRateHz` INTEGER NOT NULL,
                    `channelCount` INTEGER NOT NULL,
                    `peakAmplitude` INTEGER NOT NULL,
                    `nonSilent` INTEGER NOT NULL,
                    `createdAtEpochMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`projectId`) REFERENCES `karaoke_projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`assetId`) REFERENCES `karaoke_audio_assets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_karaoke_recording_segments_projectId` ON `karaoke_recording_segments` (`projectId`)",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_karaoke_recording_segments_assetId` ON `karaoke_recording_segments` (`assetId`)",
            )
        }
    }

    internal val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `karaoke_projects` ADD COLUMN `trimStartMillis` INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `karaoke_backing_segments` (
                    `id` TEXT NOT NULL,
                    `projectId` TEXT NOT NULL,
                    `assetId` TEXT NOT NULL,
                    `sourceMode` TEXT NOT NULL,
                    `timelineStartMillis` INTEGER NOT NULL,
                    `createdAtEpochMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`projectId`) REFERENCES `karaoke_projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`assetId`) REFERENCES `karaoke_audio_assets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_karaoke_backing_segments_projectId` ON `karaoke_backing_segments` (`projectId`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_karaoke_backing_segments_assetId` ON `karaoke_backing_segments` (`assetId`)",
            )
            db.execSQL(
                """
                INSERT INTO `karaoke_backing_segments`
                    (`id`, `projectId`, `assetId`, `sourceMode`, `timelineStartMillis`, `createdAtEpochMillis`)
                SELECT 'migration-' || p.`id`, p.`id`, a.`id`, p.`sourceMode`, 0, p.`createdAtEpochMillis`
                FROM `karaoke_projects` p
                JOIN `karaoke_audio_assets` a ON a.`projectId` = p.`id`
                WHERE a.`kind` = p.`sourceMode`
                """.trimIndent(),
            )
        }
    }
}

package com.resonote.core.database

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.resonote.core.database.di.DatabaseModule
import com.resonote.core.database.history.DeviceHistoryEntity
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ResonoteDatabaseMigrationTest {
    @Test
    fun migrationFromVersionOnePreservesLocalMediaAndCreatesHistory() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val databaseName = "resonote-migration-1-2.db"
        context.deleteDatabase(databaseName)
        val path = context.getDatabasePath(databaseName)
        path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { legacy ->
            legacy.execSQL(LOCAL_MEDIA_CREATE_SQL)
            legacy.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_local_media_storagePath` ON `local_media` (`storagePath`)")
            legacy.execSQL("CREATE INDEX IF NOT EXISTS `index_local_media_sizeBytes_sha256` ON `local_media` (`sizeBytes`, `sha256`)")
            legacy.execSQL("CREATE INDEX IF NOT EXISTS `index_local_media_importedAtEpochMillis` ON `local_media` (`importedAtEpochMillis`)")
            legacy.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            legacy.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, ?)",
                arrayOf(VERSION_ONE_IDENTITY_HASH),
            )
            legacy.execSQL(
                """
                INSERT INTO local_media (
                    id, storagePath, displayName, title, artist, albumTitle, artworkPath,
                    durationMillis, mimeType, fileExtension, sizeBytes, sha256, sampleRateHz,
                    bitDepth, bitrateBitsPerSecond, importedAtEpochMillis, pendingDeletion
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    "retained",
                    "/private/retained.flac",
                    "retained.flac",
                    "Retained",
                    "Artist",
                    null,
                    null,
                    180_000,
                    "audio/flac",
                    "flac",
                    4_096,
                    SHA,
                    96_000,
                    24,
                    2_304_000,
                    1_000,
                    0,
                ),
            )
            legacy.version = 1
        }

        val migrated =
            Room.databaseBuilder(context, ResonoteDatabase::class.java, databaseName)
                .addMigrations(DatabaseModule.MIGRATION_1_2)
                .allowMainThreadQueries()
                .build()
        try {
            assertThat(migrated.localMediaDao().findById("retained")?.title).isEqualTo("Retained")
            migrated.deviceHistoryDao().record(historyEntity())
            assertThat(migrated.deviceHistoryDao().findAll().single().mediaId).isEqualTo("cloud-hash")
        } finally {
            migrated.close()
            context.deleteDatabase(databaseName)
        }
    }

    private companion object {
        const val VERSION_ONE_IDENTITY_HASH = "7c0785066712aab672cf6015373be25e"
        const val SHA = "c7c4e0f766c17694a51f3b92a5f01d3ba2d729391bb781e4c6299f51f91aa508"
        const val LOCAL_MEDIA_CREATE_SQL =
            "CREATE TABLE IF NOT EXISTS `local_media` (" +
                "`id` TEXT NOT NULL, `storagePath` TEXT NOT NULL, `displayName` TEXT NOT NULL, " +
                "`title` TEXT NOT NULL, `artist` TEXT, `albumTitle` TEXT, `artworkPath` TEXT, " +
                "`durationMillis` INTEGER NOT NULL, `mimeType` TEXT, `fileExtension` TEXT, " +
                "`sizeBytes` INTEGER NOT NULL, `sha256` TEXT NOT NULL, `sampleRateHz` INTEGER, " +
                "`bitDepth` INTEGER, `bitrateBitsPerSecond` INTEGER, `importedAtEpochMillis` INTEGER NOT NULL, " +
                "`pendingDeletion` INTEGER NOT NULL, PRIMARY KEY(`id`))"

        fun historyEntity() =
            DeviceHistoryEntity(
                source = "cloud",
                mediaId = "cloud-hash",
                title = "Cloud song",
                artist = "Artist",
                albumTitle = null,
                artworkUri = null,
                durationMillis = 180_000,
                albumAudioId = "123",
                lastPlayedAtEpochMillis = 2_000,
                playCount = 1,
            )
    }
}

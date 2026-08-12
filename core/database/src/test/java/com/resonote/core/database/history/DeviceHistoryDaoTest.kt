package com.resonote.core.database.history

import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.resonote.core.database.ResonoteDatabase
import com.resonote.core.database.local.LocalMediaEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DeviceHistoryDaoTest {
    private lateinit var database: ResonoteDatabase
    private lateinit var dao: DeviceHistoryDao

    @Before
    fun setUp() {
        database =
            Room.inMemoryDatabaseBuilder(
                RuntimeEnvironment.getApplication(),
                ResonoteDatabase::class.java,
            ).allowMainThreadQueries().build()
        dao = database.deviceHistoryDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun repeatedPlaybackUpdatesMetadataTimeAndCountWithoutDuplicating() = runTest {
        dao.record(entity("local", "same", playedAt = 1_000, title = "Old title"))
        dao.record(entity("local", "same", playedAt = 2_000, title = "New title"))

        val rows = dao.observeAll().first()

        assertThat(rows).hasSize(1)
        assertThat(rows.single().title).isEqualTo("New title")
        assertThat(rows.single().lastPlayedAtEpochMillis).isEqualTo(2_000)
        assertThat(rows.single().playCount).isEqualTo(2)
    }

    @Test
    fun recordKeepsNewestItemsWithinLimitInDeterministicOrder() = runTest {
        dao.record(entity("local", "old", playedAt = 1_000), limit = 2)
        dao.record(entity("cloud", "newest", playedAt = 3_000), limit = 2)
        dao.record(entity("local", "middle", playedAt = 2_000), limit = 2)

        assertThat(dao.findAll().map(DeviceHistoryEntity::mediaId))
            .containsExactly("newest", "middle")
            .inOrder()
    }

    @Test
    fun deletingHistoryDoesNotDeleteIndexedLocalMedia() = runTest {
        database.localMediaDao().insert(localMedia("local-id"))
        dao.record(entity("local", "local-id", playedAt = 1_000))

        assertThat(dao.delete("local", "local-id")).isEqualTo(1)

        assertThat(dao.findAll()).isEmpty()
        assertThat(database.localMediaDao().findById("local-id")).isNotNull()
    }

    @Test
    fun clearRemovesOnlyDeviceHistoryRows() = runTest {
        database.localMediaDao().insert(localMedia("retained"))
        dao.record(entity("local", "one", playedAt = 1_000))
        dao.record(entity("cloud", "two", playedAt = 2_000))

        assertThat(dao.clear()).isEqualTo(2)

        assertThat(dao.findAll()).isEmpty()
        assertThat(database.localMediaDao().findById("retained")).isNotNull()
    }

    private companion object {
        fun entity(
            source: String,
            mediaId: String,
            playedAt: Long,
            title: String = mediaId,
        ) = DeviceHistoryEntity(
            source = source,
            mediaId = mediaId,
            title = title,
            artist = "Artist",
            albumTitle = "Album",
            artworkUri = null,
            durationMillis = 180_000,
            albumAudioId = if (source == "cloud") "audio-$mediaId" else null,
            lastPlayedAtEpochMillis = playedAt,
            playCount = 1,
        )

        fun localMedia(id: String) =
            LocalMediaEntity(
                id = id,
                storagePath = "/private/$id.flac",
                displayName = "$id.flac",
                title = id,
                artist = "Artist",
                albumTitle = null,
                artworkPath = null,
                durationMillis = 180_000,
                mimeType = "audio/flac",
                fileExtension = "flac",
                sizeBytes = 4_096,
                sha256 = "c7c4e0f766c17694a51f3b92a5f01d3ba2d729391bb781e4c6299f51f91aa508",
                sampleRateHz = 96_000,
                bitDepth = 24,
                bitrateBitsPerSecond = 2_304_000,
                importedAtEpochMillis = 1_000,
            )
    }
}

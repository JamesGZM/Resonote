package com.resonote.core.database.local

import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.resonote.core.database.ResonoteDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocalMediaDaoTest {
    private lateinit var database: ResonoteDatabase
    private lateinit var dao: LocalMediaDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            ResonoteDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.localMediaDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun observeAllReturnsNewestFirstAndHidesPendingDeletion() = runTest {
        dao.insert(entity("older", importedAt = 1_000))
        dao.insert(entity("newer", importedAt = 2_000))

        assertThat(dao.observeAll().first().map { it.id }).containsExactly("newer", "older").inOrder()

        assertThat(dao.markPendingDeletion("newer")).isEqualTo(1)
        assertThat(dao.observeAll().first().map { it.id }).containsExactly("older")
        assertThat(dao.findAllForRecovery().map { it.id }).containsExactly("newer", "older").inOrder()

        assertThat(dao.restorePendingDeletion("newer")).isEqualTo(1)
        assertThat(dao.observeAll().first().map { it.id }).containsExactly("newer", "older").inOrder()
    }

    @Test
    fun duplicateModeAllowsSameHashWithIndependentIdentityAndPath() = runTest {
        dao.insert(entity("original", storagePath = "/private/original.flac"))
        dao.insert(entity("copy", storagePath = "/private/copy.flac", importedAt = 2_000))

        val duplicates = dao.findDuplicates(sizeBytes = 4_096, sha256 = SHA)

        assertThat(duplicates.map { it.id }).containsExactly("copy", "original").inOrder()
    }

    @Test
    fun deleteReturnsAffectedRowCount() = runTest {
        dao.insert(entity("song"))

        assertThat(dao.delete("song")).isEqualTo(1)
        assertThat(dao.delete("song")).isEqualTo(0)
        assertThat(dao.findById("song")).isNull()
    }

    @Test
    fun namedDatabaseRetainsCompleteLocalMediaAfterCloseAndReopen() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val databaseName = "local-media-restart-test.db"
        context.deleteDatabase(databaseName)
        val original = entity("persistent", storagePath = context.filesDir.resolve("persistent.flac").path)

        val firstDatabase = Room.databaseBuilder(context, ResonoteDatabase::class.java, databaseName)
            .allowMainThreadQueries()
            .build()
        try {
            firstDatabase.localMediaDao().insert(original)
        } finally {
            firstDatabase.close()
        }

        val reopenedDatabase = Room.databaseBuilder(context, ResonoteDatabase::class.java, databaseName)
            .allowMainThreadQueries()
            .build()
        val restored = try {
            reopenedDatabase.localMediaDao().findById("persistent")
        } finally {
            reopenedDatabase.close()
        }

        assertThat(restored).isEqualTo(original)
        assertThat(restored?.asExternalModel()).isEqualTo(original.asExternalModel())
        context.deleteDatabase(databaseName)
    }

    private companion object {
        const val SHA = "c7c4e0f766c17694a51f3b92a5f01d3ba2d729391bb781e4c6299f51f91aa508"

        fun entity(id: String, storagePath: String = "/private/$id.flac", importedAt: Long = 1_000) = LocalMediaEntity(
            id = id,
            storagePath = storagePath,
            displayName = "$id.flac",
            title = id,
            artist = "Resonote Artist",
            albumTitle = "Night Signals",
            artworkPath = null,
            durationMillis = 180_000,
            mimeType = "audio/flac",
            fileExtension = "flac",
            sizeBytes = 4_096,
            sha256 = SHA,
            sampleRateHz = 96_000,
            bitDepth = 24,
            bitrateBitsPerSecond = 2_304_000,
            importedAtEpochMillis = importedAt,
        )
    }
}

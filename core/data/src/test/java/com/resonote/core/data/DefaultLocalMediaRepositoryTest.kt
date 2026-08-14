package com.resonote.core.data

import com.google.common.truth.Truth.assertThat
import com.resonote.core.database.local.LocalMediaDao
import com.resonote.core.database.local.LocalMediaEntity
import com.resonote.core.media.local.LocalMediaDigest
import com.resonote.core.media.local.LocalMediaFiles
import com.resonote.core.media.local.LocalMediaMetadata
import com.resonote.core.media.local.LocalMediaPersistRequest
import com.resonote.core.media.local.LocalMediaSourceInspection
import com.resonote.core.media.local.LocalMediaStore
import com.resonote.core.media.local.LocalMediaStoreError
import com.resonote.core.media.local.LocalMediaStoreResult
import com.resonote.core.media.local.StoredLocalMedia
import com.resonote.core.model.LocalMediaDeleteResult
import com.resonote.core.model.LocalMediaDuplicateAction
import com.resonote.core.model.LocalMediaId
import com.resonote.core.model.LocalMediaImportFailure
import com.resonote.core.model.LocalMediaImportResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DefaultLocalMediaRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun storageRecoveryRetainsActiveFilesAndFinishesPendingDeletion() = runTest {
        val active = entity(id = "existing")
        val pending = entity(id = "pending").copy(pendingDeletion = true)
        val dao = FakeLocalMediaDao(mutableListOf(active, pending))
        val store = FakeLocalMediaStore(stored = storedMedia())
        val repository = repository(dao, store)

        assertThat(repository.recoverStorage()).isTrue()
        assertThat(store.recoverRequests.single()).containsExactly(
            LocalMediaFiles(active.storagePath, active.artworkPath),
        )
        assertThat(dao.rows).containsExactly(active)

        store.recoverResult = LocalMediaStoreResult.Failure(LocalMediaStoreError.StorageUnavailable)
        assertThat(repository.recoverStorage()).isFalse()
        assertThat(dao.rows.map(LocalMediaEntity::id)).containsExactly("existing")
    }

    @Test
    fun importPersistsThenIndexesStableLocalMedia() = runTest {
        val dao = FakeLocalMediaDao()
        val store = FakeLocalMediaStore(stored = storedMedia())
        val repository = repository(dao, store, id = "new-id")

        val result = repository.importFromUri(SOURCE_URI)

        val imported = result as LocalMediaImportResult.Imported
        assertThat(imported.media.id).isEqualTo(LocalMediaId("new-id"))
        assertThat(imported.media.title).isEqualTo("Night Signals")
        assertThat(imported.media.importedAtEpochMillis).isEqualTo(NOW)
        assertThat(dao.rows.single().storagePath).isEqualTo("/private/audio.flac")
        assertThat(dao.rows.single().sha256).isEqualTo(DIGEST.sha256)
        assertThat(store.persistRequests.single().storageKey).isEqualTo("new-id")
    }

    @Test
    fun duplicateRequiresConfirmationWithoutCopyingOrIndexing() = runTest {
        val existing = entity(id = "existing")
        val dao = FakeLocalMediaDao(mutableListOf(existing))
        val store = FakeLocalMediaStore(stored = storedMedia())
        val repository = repository(dao, store)

        val result = repository.importFromUri(SOURCE_URI)

        val conflict = result as LocalMediaImportResult.DuplicateConfirmationRequired
        assertThat(conflict.existing.map { it.id }).containsExactly(LocalMediaId("existing"))
        assertThat(conflict.candidate.displayName).isEqualTo("signals.flac")
        assertThat(store.persistRequests).isEmpty()
        assertThat(dao.rows).containsExactly(existing)
    }

    @Test
    fun confirmedDuplicateCreatesIndependentCopy() = runTest {
        val dao = FakeLocalMediaDao(mutableListOf(entity(id = "existing")))
        val store = FakeLocalMediaStore(stored = storedMedia())
        val repository = repository(dao, store, id = "copy-id")

        val result = repository.importFromUri(SOURCE_URI, LocalMediaDuplicateAction.ImportCopy)

        assertThat((result as LocalMediaImportResult.Imported).media.id).isEqualTo(LocalMediaId("copy-id"))
        assertThat(dao.rows.map { it.id }).containsExactly("existing", "copy-id")
        assertThat(store.persistRequests).hasSize(1)
    }

    @Test
    fun indexFailureRollsBackCommittedPrivateFiles() = runTest {
        val dao = FakeLocalMediaDao().apply { failInsert = true }
        val stored = storedMedia()
        val store = FakeLocalMediaStore(stored = stored)
        val repository = repository(dao, store)

        val result = repository.importFromUri(SOURCE_URI)

        assertThat(result).isEqualTo(LocalMediaImportResult.Failed(LocalMediaImportFailure.IndexUnavailable))
        assertThat(store.removedFiles).containsExactly(stored.files)
        assertThat(dao.rows).isEmpty()
    }

    @Test
    fun invalidMetadataRollsBackBeforeWritingIndex() = runTest {
        val dao = FakeLocalMediaDao()
        val stored = storedMedia().copy(metadata = METADATA.copy(durationMillis = -1))
        val store = FakeLocalMediaStore(stored = stored)
        val repository = repository(dao, store)

        val result = repository.importFromUri(SOURCE_URI)

        assertThat(result).isEqualTo(LocalMediaImportResult.Failed(LocalMediaImportFailure.MetadataUnavailable))
        assertThat(store.removedFiles).containsExactly(stored.files)
        assertThat(dao.rows).isEmpty()
    }

    @Test
    fun deleteRemovesFilesBeforeDeletingPendingIndex() = runTest {
        val existing = entity(id = "existing")
        val dao = FakeLocalMediaDao(mutableListOf(existing))
        val store = FakeLocalMediaStore(stored = storedMedia())
        val repository = repository(dao, store)

        val result = repository.delete(LocalMediaId("existing"))

        assertThat(result).isEqualTo(LocalMediaDeleteResult.Deleted)
        assertThat(store.removedFiles).containsExactly(LocalMediaFiles(existing.storagePath, existing.artworkPath))
        assertThat(dao.rows).isEmpty()
    }

    @Test
    fun deleteRestoresVisibleIndexWhenStorageRemovalFails() = runTest {
        val existing = entity(id = "existing")
        val dao = FakeLocalMediaDao(mutableListOf(existing))
        val store = FakeLocalMediaStore(stored = storedMedia()).apply {
            removeResult = LocalMediaStoreResult.Failure(LocalMediaStoreError.StorageUnavailable)
        }
        val repository = repository(dao, store)

        val result = repository.delete(LocalMediaId("existing"))

        assertThat(result).isEqualTo(LocalMediaDeleteResult.Failed)
        assertThat(dao.rows.single().pendingDeletion).isFalse()
    }

    @Test
    fun deleteRetriesPendingRecordLeftByEarlierAttempt() = runTest {
        val pending = entity(id = "existing").copy(pendingDeletion = true)
        val dao = FakeLocalMediaDao(mutableListOf(pending))
        val store = FakeLocalMediaStore(stored = storedMedia())
        val repository = repository(dao, store)

        val result = repository.delete(LocalMediaId("existing"))

        assertThat(result).isEqualTo(LocalMediaDeleteResult.Deleted)
        assertThat(store.removedFiles).containsExactly(LocalMediaFiles(pending.storagePath, pending.artworkPath))
        assertThat(dao.rows).isEmpty()
    }

    @Test
    fun resolvePlaybackSourceRequiresExistingPrivateFile() = runTest {
        val audio = temporaryFolder.newFile("local.flac")
        val dao = FakeLocalMediaDao(mutableListOf(entity(id = "existing", storagePath = audio.absolutePath)))
        val repository = repository(dao, FakeLocalMediaStore(stored = storedMedia()))

        val resolved = repository.resolvePlaybackSource(LocalMediaId("existing"))

        assertThat(resolved?.uri).isEqualTo(audio.toURI().toString())
        assertThat(resolved?.media?.id).isEqualTo(LocalMediaId("existing"))
        audio.delete()
        assertThat(repository.resolvePlaybackSource(LocalMediaId("existing"))).isNull()
    }

    private fun repository(dao: FakeLocalMediaDao, store: FakeLocalMediaStore, id: String = "generated-id") =
        DefaultLocalMediaRepository(
            dao = dao,
            store = store,
            newId = { LocalMediaId(id) },
            now = { NOW },
        )

    private class FakeLocalMediaDao(val rows: MutableList<LocalMediaEntity> = mutableListOf()) : LocalMediaDao {
        private val observed = MutableStateFlow(rows.toList())
        var failInsert = false

        override suspend fun findAllForRecovery(): List<LocalMediaEntity> = rows.toList()

        override fun observeAll(): Flow<List<LocalMediaEntity>> = observed

        override suspend fun findById(id: String): LocalMediaEntity? = rows.firstOrNull { it.id == id }

        override suspend fun findDuplicates(sizeBytes: Long, sha256: String): List<LocalMediaEntity> = rows.filter {
            it.sizeBytes ==
                sizeBytes &&
                it.sha256 == sha256 &&
                !it.pendingDeletion
        }

        override suspend fun insert(entity: LocalMediaEntity) {
            if (failInsert) error("database unavailable")
            rows += entity
            emitRows()
        }

        override suspend fun markPendingDeletion(id: String): Int {
            val index = rows.indexOfFirst { it.id == id && !it.pendingDeletion }
            if (index < 0) return 0
            rows[index] = rows[index].copy(pendingDeletion = true)
            emitRows()
            return 1
        }

        override suspend fun restorePendingDeletion(id: String): Int {
            val index = rows.indexOfFirst { it.id == id && it.pendingDeletion }
            if (index < 0) return 0
            rows[index] = rows[index].copy(pendingDeletion = false)
            emitRows()
            return 1
        }

        override suspend fun delete(id: String): Int {
            val removed = rows.removeAll { it.id == id }
            emitRows()
            return if (removed) 1 else 0
        }

        private fun emitRows() {
            observed.value = rows.toList()
        }
    }

    private class FakeLocalMediaStore(private val stored: StoredLocalMedia) : LocalMediaStore {
        val persistRequests = mutableListOf<LocalMediaPersistRequest>()
        val removedFiles = mutableListOf<LocalMediaFiles>()
        var recoverResult: LocalMediaStoreResult<Unit> = LocalMediaStoreResult.Success(Unit)
        var removeResult: LocalMediaStoreResult<Unit> = LocalMediaStoreResult.Success(Unit)

        val recoverRequests = mutableListOf<Set<LocalMediaFiles>>()

        override suspend fun recover(retainedFiles: Set<LocalMediaFiles>): LocalMediaStoreResult<Unit> {
            recoverRequests += retainedFiles
            return recoverResult
        }

        override suspend fun inspect(sourceUri: String) = LocalMediaStoreResult.Success(INSPECTION)

        override suspend fun calculateDigest(sourceUri: String, expectedSizeBytes: Long?) =
            LocalMediaStoreResult.Success(DIGEST)

        override suspend fun persist(request: LocalMediaPersistRequest): LocalMediaStoreResult<StoredLocalMedia> {
            persistRequests += request
            return LocalMediaStoreResult.Success(stored)
        }

        override suspend fun remove(files: LocalMediaFiles): LocalMediaStoreResult<Unit> {
            removedFiles += files
            return removeResult
        }
    }

    private companion object {
        const val SOURCE_URI = "content://provider/signals.flac"
        const val NOW = 1_786_560_000_000L
        val DIGEST = LocalMediaDigest(
            sizeBytes = 4_096,
            sha256 = "c7c4e0f766c17694a51f3b92a5f01d3ba2d729391bb781e4c6299f51f91aa508",
        )
        val METADATA = LocalMediaMetadata(
            title = "Night Signals",
            artist = "Resonote Artist",
            albumTitle = "Resonote Sessions",
            durationMillis = 180_000,
            detectedMimeType = "audio/flac",
            sampleRateHz = 96_000,
            bitDepth = 24,
            bitrateBitsPerSecond = 2_304_000,
        )
        val INSPECTION = LocalMediaSourceInspection(
            displayName = "signals.flac",
            reportedSizeBytes = DIGEST.sizeBytes,
            declaredMimeType = "audio/flac",
            fileExtension = "flac",
            metadata = METADATA,
        )

        fun storedMedia() = StoredLocalMedia(
            files = LocalMediaFiles("/private/audio.flac", "/private/artwork.image"),
            displayName = INSPECTION.displayName,
            fileExtension = INSPECTION.fileExtension,
            digest = DIGEST,
            metadata = METADATA,
        )

        fun entity(id: String, storagePath: String = "/private/$id.flac") = LocalMediaEntity(
            id = id,
            storagePath = storagePath,
            displayName = "$id.flac",
            title = "Night Signals",
            artist = "Resonote Artist",
            albumTitle = "Resonote Sessions",
            artworkPath = null,
            durationMillis = 180_000,
            mimeType = "audio/flac",
            fileExtension = "flac",
            sizeBytes = DIGEST.sizeBytes,
            sha256 = DIGEST.sha256,
            sampleRateHz = 96_000,
            bitDepth = 24,
            bitrateBitsPerSecond = 2_304_000,
            importedAtEpochMillis = NOW,
        )
    }
}

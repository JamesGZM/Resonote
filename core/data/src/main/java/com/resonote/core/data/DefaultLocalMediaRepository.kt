package com.resonote.core.data

import com.resonote.core.database.local.LocalMediaDao
import com.resonote.core.database.local.LocalMediaEntity
import com.resonote.core.database.local.asExternalModel
import com.resonote.core.media.local.LocalMediaFiles
import com.resonote.core.media.local.LocalMediaPersistRequest
import com.resonote.core.media.local.LocalMediaSourceInspection
import com.resonote.core.media.local.LocalMediaStore
import com.resonote.core.media.local.LocalMediaStoreError
import com.resonote.core.media.local.LocalMediaStoreResult
import com.resonote.core.media.local.LocalMediaTreeScanFailure
import com.resonote.core.media.local.LocalMediaTreeScanResult
import com.resonote.core.media.local.LocalMediaTreeSource
import com.resonote.core.media.local.StoredLocalMedia
import com.resonote.core.model.LocalMediaDeleteResult
import com.resonote.core.model.LocalMediaDuplicateAction
import com.resonote.core.model.LocalMediaId
import com.resonote.core.model.LocalMediaImportCandidate
import com.resonote.core.model.LocalMediaImportFailure
import com.resonote.core.model.LocalMediaImportResult
import com.resonote.core.model.LocalMediaPlaybackSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultLocalMediaRepository internal constructor(
    private val dao: LocalMediaDao,
    private val store: LocalMediaStore,
    private val treeSource: LocalMediaTreeSource,
    private val newId: () -> LocalMediaId,
    private val now: () -> Long,
) : LocalMediaRepository {
    @Inject
    constructor(
        dao: LocalMediaDao,
        store: LocalMediaStore,
        treeSource: LocalMediaTreeSource,
    ) : this(
        dao = dao,
        store = store,
        treeSource = treeSource,
        newId = { LocalMediaId(UUID.randomUUID().toString()) },
        now = System::currentTimeMillis,
    )

    private val mutationMutex = Mutex()

    override suspend fun scanDirectory(treeUri: String): LocalMediaDirectoryScanResult =
        when (val result = treeSource.scan(treeUri)) {
            is LocalMediaTreeScanResult.Available -> LocalMediaDirectoryScanResult.Available(result.documentUris)
            is LocalMediaTreeScanResult.Failed -> LocalMediaDirectoryScanResult.Failed(
                when (result.reason) {
                    LocalMediaTreeScanFailure.InvalidTree -> LocalMediaDirectoryScanFailure.InvalidTree
                    LocalMediaTreeScanFailure.PermissionDenied -> LocalMediaDirectoryScanFailure.PermissionDenied
                    LocalMediaTreeScanFailure.Unavailable -> LocalMediaDirectoryScanFailure.Unavailable
                },
            )
        }

    override suspend fun recoverStorage(): Boolean = mutationMutex.withLock {
        val rows = try {
            dao.findAllForRecovery()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return@withLock false
        }
        val retainedFiles = rows
            .filterNot(LocalMediaEntity::pendingDeletion)
            .map { LocalMediaFiles(it.storagePath, it.artworkPath) }
            .toSet()
        if (store.recover(retainedFiles) !is LocalMediaStoreResult.Success) return@withLock false
        rows.filter(LocalMediaEntity::pendingDeletion).all { pending ->
            try {
                dao.delete(pending.id) == 1
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }
        }
    }

    override fun observeAll() = dao.observeAll().map { rows -> rows.map(LocalMediaEntity::asExternalModel) }

    override suspend fun importFromUri(
        sourceUri: String,
        duplicateAction: LocalMediaDuplicateAction,
    ): LocalMediaImportResult = mutationMutex.withLock {
        val inspection = when (val result = store.inspect(sourceUri)) {
            is LocalMediaStoreResult.Success -> result.value
            is LocalMediaStoreResult.Failure -> return@withLock result.toImportFailure()
        }
        val digest = when (
            val result = store.calculateDigest(
                sourceUri = sourceUri,
                expectedSizeBytes = inspection.reportedSizeBytes,
            )
        ) {
            is LocalMediaStoreResult.Success -> result.value
            is LocalMediaStoreResult.Failure -> return@withLock result.toImportFailure()
        }
        val duplicateRows = try {
            dao.findDuplicates(digest.sizeBytes, digest.sha256)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return@withLock LocalMediaImportResult.Failed(LocalMediaImportFailure.IndexUnavailable)
        }
        if (duplicateRows.isNotEmpty() && duplicateAction == LocalMediaDuplicateAction.RequireConfirmation) {
            return@withLock LocalMediaImportResult.DuplicateConfirmationRequired(
                candidate = inspection.toCandidate(digest.sizeBytes),
                existing = duplicateRows.map(LocalMediaEntity::asExternalModel),
            )
        }

        val id = newId()
        val stored = when (
            val result = store.persist(
                LocalMediaPersistRequest(
                    sourceUri = sourceUri,
                    storageKey = id.value,
                    inspection = inspection,
                    expectedDigest = digest,
                ),
            )
        ) {
            is LocalMediaStoreResult.Success -> result.value
            is LocalMediaStoreResult.Failure -> return@withLock result.toImportFailure()
        }
        val entity = stored.toEntity(id = id, importedAtEpochMillis = now())
        val media = try {
            entity.asExternalModel()
        } catch (_: IllegalArgumentException) {
            withContext(NonCancellable) { runCatching { store.remove(stored.files) } }
            return@withLock LocalMediaImportResult.Failed(LocalMediaImportFailure.MetadataUnavailable)
        }
        try {
            dao.insert(entity)
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) { runCatching { store.remove(stored.files) } }
            throw cancelled
        } catch (_: Exception) {
            withContext(NonCancellable) { runCatching { store.remove(stored.files) } }
            return@withLock LocalMediaImportResult.Failed(LocalMediaImportFailure.IndexUnavailable)
        }
        LocalMediaImportResult.Imported(media)
    }

    override suspend fun delete(id: LocalMediaId): LocalMediaDeleteResult = mutationMutex.withLock {
        val entity = try {
            dao.findById(id.value)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return@withLock LocalMediaDeleteResult.Failed
        } ?: return@withLock LocalMediaDeleteResult.NotFound
        val wasPendingDeletion = entity.pendingDeletion
        if (!wasPendingDeletion) {
            val marked = try {
                dao.markPendingDeletion(id.value)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return@withLock LocalMediaDeleteResult.Failed
            }
            if (marked != 1) return@withLock LocalMediaDeleteResult.Failed
        }

        withContext(NonCancellable) {
            val removed = runCatching {
                store.remove(LocalMediaFiles(entity.storagePath, entity.artworkPath))
            }.getOrElse {
                if (!wasPendingDeletion) runCatching { dao.restorePendingDeletion(id.value) }
                return@withContext LocalMediaDeleteResult.Failed
            }
            if (removed !is LocalMediaStoreResult.Success) {
                if (!wasPendingDeletion) runCatching { dao.restorePendingDeletion(id.value) }
                return@withContext LocalMediaDeleteResult.Failed
            }
            val deleted = runCatching { dao.delete(id.value) }.getOrDefault(0)
            if (deleted == 1) LocalMediaDeleteResult.Deleted else LocalMediaDeleteResult.Failed
        }
    }

    override suspend fun resolvePlaybackSource(id: LocalMediaId): LocalMediaPlaybackSource? {
        val entity = try {
            dao.findById(id.value)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }?.takeUnless(LocalMediaEntity::pendingDeletion) ?: return null
        val audio = File(entity.storagePath).takeIf(File::isFile) ?: return null
        return LocalMediaPlaybackSource(
            uri = audio.toURI().toString(),
            media = entity.asExternalModel(),
        )
    }

    private fun LocalMediaSourceInspection.toCandidate(sizeBytes: Long) = LocalMediaImportCandidate(
        displayName = displayName,
        title = metadata.title,
        artist = metadata.artist,
        sizeBytes = sizeBytes,
        mimeType = metadata.detectedMimeType,
    )

    private fun StoredLocalMedia.toEntity(id: LocalMediaId, importedAtEpochMillis: Long) = LocalMediaEntity(
        id = id.value,
        storagePath = files.audioPath,
        displayName = displayName,
        title = metadata.title,
        artist = metadata.artist,
        albumTitle = metadata.albumTitle,
        artworkPath = files.artworkPath,
        durationMillis = metadata.durationMillis,
        mimeType = metadata.detectedMimeType,
        fileExtension = fileExtension,
        sizeBytes = digest.sizeBytes,
        sha256 = digest.sha256,
        sampleRateHz = metadata.sampleRateHz,
        bitDepth = metadata.bitDepth,
        bitrateBitsPerSecond = metadata.bitrateBitsPerSecond,
        importedAtEpochMillis = importedAtEpochMillis,
    )

    private fun LocalMediaStoreResult.Failure.toImportFailure() = LocalMediaImportResult.Failed(
        when (error) {
            LocalMediaStoreError.InvalidSource -> LocalMediaImportFailure.InvalidSource
            LocalMediaStoreError.PermissionDenied -> LocalMediaImportFailure.PermissionDenied
            LocalMediaStoreError.SourceUnavailable -> LocalMediaImportFailure.SourceUnavailable
            LocalMediaStoreError.EmptyFile -> LocalMediaImportFailure.EmptyFile
            LocalMediaStoreError.UnsupportedFormat -> LocalMediaImportFailure.UnsupportedFormat
            LocalMediaStoreError.MetadataUnavailable -> LocalMediaImportFailure.MetadataUnavailable
            LocalMediaStoreError.InsufficientStorage -> LocalMediaImportFailure.InsufficientStorage
            LocalMediaStoreError.HashFailed -> LocalMediaImportFailure.HashFailed
            LocalMediaStoreError.SourceChanged -> LocalMediaImportFailure.SourceChanged
            LocalMediaStoreError.StorageUnavailable -> LocalMediaImportFailure.StorageUnavailable
        },
    )
}

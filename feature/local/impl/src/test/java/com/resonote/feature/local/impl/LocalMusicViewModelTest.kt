package com.resonote.feature.local.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.LocalMediaRepository
import com.resonote.core.model.LocalMedia
import com.resonote.core.model.LocalMediaDeleteResult
import com.resonote.core.model.LocalMediaDuplicateAction
import com.resonote.core.model.LocalMediaId
import com.resonote.core.model.LocalMediaImportCandidate
import com.resonote.core.model.LocalMediaImportFailure
import com.resonote.core.model.LocalMediaImportResult
import com.resonote.core.model.LocalMediaPlaybackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocalMusicViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun repositoryFlowPublishesSearchableAndSortableDeviceLibrary() = runTest(dispatcher) {
        val repository = FakeLocalMediaRepository(
            initialMedia = listOf(
                media("one", title = "夜航", artist = "Beta", importedAt = 100),
                media("two", title = "岸线", artist = "Alpha", importedAt = 200),
            ),
        )
        val viewModel = LocalMusicViewModel(repository, FakeLocalMediaTreeSource())
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.visibleMedia.map { it.id.value })
            .containsExactly("two", "one").inOrder()

        viewModel.updateQuery("beta")
        assertThat(viewModel.uiState.value.visibleMedia.map { it.id.value }).containsExactly("one")

        viewModel.updateQuery("")
        viewModel.updateSort(LocalMusicSort.Artist)
        assertThat(viewModel.uiState.value.visibleMedia.map { it.id.value })
            .containsExactly("two", "one").inOrder()
    }

    @Test
    fun batchImportRunsSequentiallyAndReportsTypedFailures() = runTest(dispatcher) {
        val repository = FakeLocalMediaRepository(
            importResults = mutableMapOf(
                ImportRequest("content://one", LocalMediaDuplicateAction.RequireConfirmation) to
                    LocalMediaImportResult.Imported(media("one")),
                ImportRequest("content://two", LocalMediaDuplicateAction.RequireConfirmation) to
                    LocalMediaImportResult.Failed(LocalMediaImportFailure.UnsupportedFormat),
            ),
        )
        val viewModel = LocalMusicViewModel(repository, FakeLocalMediaTreeSource())

        viewModel.importUris(listOf("content://one", "content://two"))
        advanceUntilIdle()

        assertThat(repository.importRequests).containsExactly(
            ImportRequest("content://one", LocalMediaDuplicateAction.RequireConfirmation),
            ImportRequest("content://two", LocalMediaDuplicateAction.RequireConfirmation),
        ).inOrder()
        assertThat(viewModel.uiState.value.importState).isEqualTo(
            LocalImportUiState.Completed(
                total = 2,
                imported = 1,
                skipped = 0,
                failures = listOf(LocalMediaImportFailure.UnsupportedFormat),
            ),
        )
    }

    @Test
    fun duplicateCanBeSkippedAndBatchContinues() = runTest(dispatcher) {
        val existing = media("existing", title = "潮汐")
        val repository = FakeLocalMediaRepository(
            importResults = mutableMapOf(
                ImportRequest("content://duplicate", LocalMediaDuplicateAction.RequireConfirmation) to
                    duplicate(existing),
                ImportRequest("content://next", LocalMediaDuplicateAction.RequireConfirmation) to
                    LocalMediaImportResult.Imported(media("next")),
            ),
        )
        val viewModel = LocalMusicViewModel(repository, FakeLocalMediaTreeSource())

        viewModel.importUris(listOf("content://duplicate", "content://next"))
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.importState).isInstanceOf(LocalImportUiState.AwaitingDuplicate::class.java)

        viewModel.resolveDuplicate(importCopy = false)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.importState).isEqualTo(
            LocalImportUiState.Completed(total = 2, imported = 1, skipped = 1, failures = emptyList()),
        )
    }

    @Test
    fun duplicateCanBeImportedAsIndependentCopy() = runTest(dispatcher) {
        val existing = media("existing", title = "潮汐")
        val copy = media("copy", title = "潮汐")
        val repository = FakeLocalMediaRepository(
            importResults = mutableMapOf(
                ImportRequest("content://duplicate", LocalMediaDuplicateAction.RequireConfirmation) to
                    duplicate(existing),
                ImportRequest("content://duplicate", LocalMediaDuplicateAction.ImportCopy) to
                    LocalMediaImportResult.Imported(copy),
            ),
        )
        val viewModel = LocalMusicViewModel(repository, FakeLocalMediaTreeSource())

        viewModel.importUris(listOf("content://duplicate"))
        advanceUntilIdle()
        viewModel.resolveDuplicate(importCopy = true)
        advanceUntilIdle()

        assertThat(repository.importRequests.last()).isEqualTo(
            ImportRequest("content://duplicate", LocalMediaDuplicateAction.ImportCopy),
        )
        assertThat(viewModel.uiState.value.importState).isEqualTo(
            LocalImportUiState.Completed(total = 1, imported = 1, skipped = 0, failures = emptyList()),
        )
    }

    @Test
    fun cancellingActiveImportMarksOnlyRemainingItemsSkipped() = runTest(dispatcher) {
        val repository = FakeLocalMediaRepository(blockImports = true)
        val viewModel = LocalMusicViewModel(repository, FakeLocalMediaTreeSource())

        viewModel.importUris(listOf("content://one", "content://two"))
        runCurrent()
        assertThat(viewModel.uiState.value.importState).isEqualTo(
            LocalImportUiState.Running(completed = 0, total = 2, imported = 0, failed = 0),
        )

        viewModel.cancelImport()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.importState).isEqualTo(
            LocalImportUiState.Completed(total = 2, imported = 0, skipped = 2, failures = emptyList()),
        )
    }

    @Test
    fun busyImporterRejectsHandoffUntilCurrentBatchIsCancelled() = runTest(dispatcher) {
        val repository = FakeLocalMediaRepository(blockImports = true)
        val viewModel = LocalMusicViewModel(repository, FakeLocalMediaTreeSource())

        assertThat(viewModel.importUris(listOf("content://one"))).isTrue()
        runCurrent()
        assertThat(viewModel.importUris(listOf("content://queued"))).isFalse()

        viewModel.cancelImport()
        advanceUntilIdle()
        assertThat(viewModel.importUris(listOf("content://queued"))).isTrue()
        viewModel.cancelImport()
        advanceUntilIdle()
    }

    @Test
    fun directoryScanFeedsEveryDocumentIntoTheExistingImportPipeline() = runTest(dispatcher) {
        val repository = FakeLocalMediaRepository(
            importResults = mutableMapOf(
                ImportRequest("content://tree/one", LocalMediaDuplicateAction.RequireConfirmation) to
                    LocalMediaImportResult.Imported(media("one")),
                ImportRequest("content://tree/two", LocalMediaDuplicateAction.RequireConfirmation) to
                    LocalMediaImportResult.Failed(LocalMediaImportFailure.UnsupportedFormat),
            ),
        )
        val treeSource = FakeLocalMediaTreeSource(
            LocalMediaTreeScanResult.Available(listOf("content://tree/one", "content://tree/two")),
        )
        val viewModel = LocalMusicViewModel(repository, treeSource)

        assertThat(viewModel.importDirectory("content://provider/tree/root")).isTrue()
        advanceUntilIdle()

        assertThat(treeSource.scannedUris).containsExactly("content://provider/tree/root")
        assertThat(repository.importRequests.map(ImportRequest::uri))
            .containsExactly("content://tree/one", "content://tree/two").inOrder()
        assertThat(viewModel.uiState.value.importState).isEqualTo(
            LocalImportUiState.Completed(
                total = 2,
                imported = 1,
                skipped = 0,
                failures = listOf(LocalMediaImportFailure.UnsupportedFormat),
            ),
        )
    }

    @Test
    fun emptyAndDeniedDirectoriesRemainTypedUiResults() = runTest(dispatcher) {
        val repository = FakeLocalMediaRepository()
        val treeSource = FakeLocalMediaTreeSource(LocalMediaTreeScanResult.Available(emptyList()))
        val viewModel = LocalMusicViewModel(repository, treeSource)

        viewModel.importDirectory("content://provider/tree/empty")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.importState).isEqualTo(
            LocalImportUiState.DirectoryFailed(LocalDirectoryImportFailure.NoFiles),
        )

        treeSource.result = LocalMediaTreeScanResult.Failed(LocalMediaTreeScanFailure.PermissionDenied)
        viewModel.dismissImportResult()
        viewModel.importDirectory("content://provider/tree/denied")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.importState).isEqualTo(
            LocalImportUiState.DirectoryFailed(LocalDirectoryImportFailure.PermissionDenied),
        )
    }

    @Test
    fun deleteFailureIsVisibleAndSuccessClearsIt() = runTest(dispatcher) {
        val target = media("one")
        val repository = FakeLocalMediaRepository(initialMedia = listOf(target))
        val viewModel = LocalMusicViewModel(repository, FakeLocalMediaTreeSource())
        advanceUntilIdle()

        repository.deleteResult = LocalMediaDeleteResult.Failed
        viewModel.requestDelete(target)
        viewModel.confirmDelete()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.deleteFailed).isTrue()

        repository.deleteResult = LocalMediaDeleteResult.Deleted
        viewModel.requestDelete(target)
        viewModel.confirmDelete()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.deleteFailed).isFalse()
        assertThat(repository.deletedIds).containsExactly(LocalMediaId("one"), LocalMediaId("one"))
    }

    private class FakeLocalMediaRepository(
        initialMedia: List<LocalMedia> = emptyList(),
        private val importResults: MutableMap<ImportRequest, LocalMediaImportResult> = mutableMapOf(),
        private val blockImports: Boolean = false,
    ) : LocalMediaRepository {
        private val media = MutableStateFlow(initialMedia)
        val importRequests = mutableListOf<ImportRequest>()
        val deletedIds = mutableListOf<LocalMediaId>()
        var deleteResult: LocalMediaDeleteResult = LocalMediaDeleteResult.Deleted

        override suspend fun recoverStorage(): Boolean = true

        override fun observeAll(): Flow<List<LocalMedia>> = media

        override suspend fun importFromUri(
            sourceUri: String,
            duplicateAction: LocalMediaDuplicateAction,
        ): LocalMediaImportResult {
            val request = ImportRequest(sourceUri, duplicateAction)
            importRequests += request
            if (blockImports) awaitCancellation()
            return requireNotNull(importResults[request]) { "Missing result for $request" }
        }

        override suspend fun delete(id: LocalMediaId): LocalMediaDeleteResult {
            deletedIds += id
            return deleteResult
        }

        override suspend fun resolvePlaybackSource(id: LocalMediaId): LocalMediaPlaybackSource? = null
    }

    private class FakeLocalMediaTreeSource(
        var result: LocalMediaTreeScanResult = LocalMediaTreeScanResult.Available(emptyList()),
    ) : LocalMediaTreeSource {
        val scannedUris = mutableListOf<String>()

        override suspend fun scan(treeUri: String): LocalMediaTreeScanResult {
            scannedUris += treeUri
            return result
        }
    }

    private data class ImportRequest(
        val uri: String,
        val action: LocalMediaDuplicateAction,
    )

    private companion object {
        fun duplicate(existing: LocalMedia) = LocalMediaImportResult.DuplicateConfirmationRequired(
            candidate = LocalMediaImportCandidate(
                displayName = "潮汐.flac",
                title = "潮汐",
                artist = "林澈",
                sizeBytes = 1_024,
                mimeType = "audio/flac",
            ),
            existing = listOf(existing),
        )

        fun media(
            id: String,
            title: String = "夜航",
            artist: String? = "林澈",
            importedAt: Long = 100,
        ) = LocalMedia(
            id = LocalMediaId(id),
            displayName = "$title.flac",
            title = title,
            artist = artist,
            albumTitle = "潮线",
            artworkUri = null,
            durationMillis = 180_000,
            mimeType = "audio/flac",
            fileExtension = "flac",
            sizeBytes = 1_024,
            sampleRateHz = 96_000,
            bitDepth = 24,
            bitrateBitsPerSecond = 2_304_000,
            importedAtEpochMillis = importedAt,
        )
    }
}

package com.resonote.feature.local.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.KaraokeRepository
import com.resonote.core.data.LocalMediaDirectoryScanFailure
import com.resonote.core.data.LocalMediaDirectoryScanResult
import com.resonote.core.data.LocalMediaRepository
import com.resonote.core.karaoke.KaraokeExportController
import com.resonote.core.karaoke.KaraokePreviewController
import com.resonote.core.karaoke.KaraokePreviewState
import com.resonote.core.model.KaraokeMixSettings
import com.resonote.core.model.KaraokeProjectId
import com.resonote.core.model.LocalMedia
import com.resonote.core.model.LocalMediaDeleteResult
import com.resonote.core.model.LocalMediaDuplicateAction
import com.resonote.core.model.LocalMediaImportFailure
import com.resonote.core.model.LocalMediaImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocalMusicViewModel @Inject constructor(
    private val repository: LocalMediaRepository,
    private val karaokeRepository: KaraokeRepository,
    private val karaokeExportController: KaraokeExportController,
    private val karaokePreviewController: KaraokePreviewController,
) : ViewModel() {
    constructor(repository: LocalMediaRepository) : this(
        repository,
        EmptyKaraokeRepository,
        EmptyKaraokeExportController,
        EmptyKaraokePreviewController,
    )
    private val mutableUiState = MutableStateFlow(LocalMusicUiState())
    val uiState: StateFlow<LocalMusicUiState> = mutableUiState.asStateFlow()

    private var importJob: Job? = null
    private var scanJob: Job? = null
    private var karaokeProjectsJob: Job? = null
    private var pendingUris = ArrayDeque<String>()
    private var pendingDuplicateUri: String? = null
    private var batchTotal = 0
    private var completed = 0
    private var imported = 0
    private var skipped = 0
    private val failures = mutableListOf<LocalMediaImportFailure>()

    init {
        viewModelScope.launch {
            repository.observeAll()
                .catch { mutableUiState.update { state -> state.copy(isLoading = false) } }
                .collect { media -> mutableUiState.update { it.copy(media = media, isLoading = false) } }
        }
        observeKaraokeProjects()
        viewModelScope.launch {
            karaokePreviewController.state.collect { preview -> mutableUiState.update { it.copy(preview = preview) } }
        }
    }

    fun retryKaraokeProjects() = observeKaraokeProjects(force = true)

    private fun observeKaraokeProjects(force: Boolean = false) {
        if (!force && karaokeProjectsJob?.isActive == true) return
        karaokeProjectsJob?.cancel()
        mutableUiState.update { it.copy(karaokeProjectsLoading = true, karaokeProjectsLoadFailed = false) }
        karaokeProjectsJob = viewModelScope.launch {
            karaokeRepository.observeProjects()
                .catch {
                    mutableUiState.update {
                        it.copy(karaokeProjectsLoading = false, karaokeProjectsLoadFailed = true)
                    }
                }
                .collect { projects ->
                    mutableUiState.update { state ->
                        state.copy(
                            karaokeProjects = projects,
                            karaokeProjectsLoading = false,
                            karaokeProjectsLoadFailed = false,
                            selectedProjectIds = state.selectedProjectIds.intersect(projects.map { it.id }.toSet()),
                            editingProject = state.editingProject?.let { editing ->
                                projects.firstOrNull { it.id == editing.id }
                            },
                        )
                    }
                }
        }
    }

    fun selectTab(tab: LocalMusicTab) {
        if (tab == mutableUiState.value.selectedTab) return
        karaokePreviewController.stop()
        mutableUiState.update { it.copy(selectedTab = tab, selectedProjectIds = emptySet(), editingProject = null) }
    }

    fun toggleProjectSelection(id: KaraokeProjectId) {
        mutableUiState.update { state ->
            val selected = state.selectedProjectIds.toMutableSet()
            if (!selected.add(id)) selected.remove(id)
            state.copy(selectedProjectIds = selected)
        }
    }

    fun selectAllProjects() {
        mutableUiState.update { state ->
            val all = state.visibleKaraokeProjects.mapTo(linkedSetOf()) { it.id }
            state.copy(selectedProjectIds = if (state.selectedProjectIds == all) emptySet() else all)
        }
    }

    fun deleteSelectedProjects() {
        val ids = mutableUiState.value.selectedProjectIds
        if (ids.isEmpty()) return
        karaokePreviewController.stop()
        viewModelScope.launch {
            if (karaokeRepository.deleteProjects(ids)) {
                mutableUiState.update { it.copy(selectedProjectIds = emptySet(), editingProject = null) }
            }
        }
    }

    fun exportSelectedProjects() {
        karaokeExportController.export(mutableUiState.value.selectedProjectIds)
    }

    fun exportProject(id: KaraokeProjectId) {
        karaokeExportController.export(setOf(id))
    }

    fun togglePreview(id: KaraokeProjectId) = karaokePreviewController.toggle(id)

    fun previewProjectMix(settings: KaraokeMixSettings) {
        val project = mutableUiState.value.editingProject ?: return
        karaokePreviewController.toggle(project.id, settings)
    }

    fun editProject(id: KaraokeProjectId) {
        mutableUiState.update { state ->
            state.copy(editingProject = state.karaokeProjects.firstOrNull { it.id == id })
        }
    }

    fun dismissProjectEditor() {
        mutableUiState.update { it.copy(editingProject = null) }
    }

    fun saveProjectMix(settings: KaraokeMixSettings) {
        val project = mutableUiState.value.editingProject ?: return
        viewModelScope.launch {
            if (karaokeRepository.updateMix(project.id, settings)) {
                karaokePreviewController.stop()
                mutableUiState.update { it.copy(editingProject = null) }
            }
        }
    }

    fun updateQuery(query: String) {
        mutableUiState.update { it.copy(query = query) }
    }

    fun updateSort(sort: LocalMusicSort) {
        mutableUiState.update { it.copy(sort = sort) }
    }

    fun importUris(uris: List<String>): Boolean {
        if (importJob?.isActive == true || scanJob?.isActive == true || pendingDuplicateUri != null) return false
        val uniqueUris = uris.filter(String::isNotBlank).distinct()
        if (uniqueUris.isEmpty()) return false
        pendingUris = ArrayDeque(uniqueUris)
        batchTotal = uniqueUris.size
        completed = 0
        imported = 0
        skipped = 0
        failures.clear()
        continueImport()
        return true
    }

    fun importDirectory(treeUri: String): Boolean {
        if (treeUri.isBlank() ||
            importJob?.isActive == true ||
            scanJob?.isActive == true ||
            pendingDuplicateUri != null
        ) {
            return false
        }
        scanJob = viewModelScope.launch {
            mutableUiState.update { it.copy(importState = LocalImportUiState.ScanningDirectory) }
            try {
                when (val result = repository.scanDirectory(treeUri)) {
                    is LocalMediaDirectoryScanResult.Available -> {
                        scanJob = null
                        if (result.documentUris.isEmpty()) {
                            mutableUiState.update {
                                it.copy(
                                    importState = LocalImportUiState.DirectoryFailed(
                                        LocalDirectoryImportFailure.NoFiles,
                                    ),
                                )
                            }
                        } else {
                            importUris(result.documentUris)
                        }
                    }
                    is LocalMediaDirectoryScanResult.Failed -> {
                        scanJob = null
                        mutableUiState.update {
                            it.copy(importState = LocalImportUiState.DirectoryFailed(result.reason.asUiFailure()))
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                mutableUiState.update { it.copy(importState = LocalImportUiState.Idle) }
                throw cancelled
            }
        }
        return true
    }

    fun resolveDuplicate(importCopy: Boolean) {
        val uri = pendingDuplicateUri ?: return
        pendingDuplicateUri = null
        if (!importCopy) {
            completed += 1
            skipped += 1
            continueImport()
            return
        }
        importJob = viewModelScope.launch {
            publishProgress()
            try {
                when (
                    val result = repository.importFromUri(
                        uri,
                        duplicateAction = LocalMediaDuplicateAction.ImportCopy,
                    )
                ) {
                    is LocalMediaImportResult.Imported -> imported += 1
                    is LocalMediaImportResult.Failed -> failures += result.reason
                    is LocalMediaImportResult.DuplicateConfirmationRequired ->
                        failures +=
                            LocalMediaImportFailure.IndexUnavailable
                }
                completed += 1
                continueImport()
            } catch (cancelled: CancellationException) {
                publishCancelled()
                throw cancelled
            }
        }
    }

    fun cancelImport() {
        if (scanJob?.isActive == true) {
            scanJob?.cancel()
            scanJob = null
            mutableUiState.update { it.copy(importState = LocalImportUiState.Idle) }
            return
        }
        if (importJob?.isActive != true && pendingDuplicateUri == null) return
        importJob?.cancel()
        pendingDuplicateUri = null
        pendingUris.clear()
        publishCancelled()
    }

    fun dismissImportResult() {
        if (
            mutableUiState.value.importState is LocalImportUiState.Completed ||
            mutableUiState.value.importState is LocalImportUiState.DirectoryFailed
        ) {
            mutableUiState.update { it.copy(importState = LocalImportUiState.Idle) }
        }
    }

    fun requestDelete(media: LocalMedia) {
        mutableUiState.update { it.copy(pendingDelete = media, deleteFailed = false) }
    }

    fun dismissDelete() {
        mutableUiState.update { it.copy(pendingDelete = null, deleteFailed = false) }
    }

    fun confirmDelete() {
        val media = mutableUiState.value.pendingDelete ?: return
        if (mutableUiState.value.deletingMediaId != null) return
        mutableUiState.update {
            it.copy(pendingDelete = null, deletingMediaId = media.id.value, deleteFailed = false)
        }
        viewModelScope.launch {
            val result = repository.delete(media.id)
            mutableUiState.update {
                it.copy(
                    deletingMediaId = null,
                    deleteFailed = result !is LocalMediaDeleteResult.Deleted,
                )
            }
        }
    }

    fun dismissDeleteFailure() {
        mutableUiState.update { it.copy(deleteFailed = false) }
    }

    private fun continueImport() {
        importJob = viewModelScope.launch {
            publishProgress()
            try {
                while (pendingUris.isNotEmpty()) {
                    val uri = pendingUris.removeFirst()
                    when (val result = repository.importFromUri(uri)) {
                        is LocalMediaImportResult.Imported -> {
                            imported += 1
                            completed += 1
                            publishProgress()
                        }
                        is LocalMediaImportResult.Failed -> {
                            failures += result.reason
                            completed += 1
                            publishProgress()
                        }
                        is LocalMediaImportResult.DuplicateConfirmationRequired -> {
                            pendingDuplicateUri = uri
                            mutableUiState.update {
                                it.copy(
                                    importState = LocalImportUiState.AwaitingDuplicate(
                                        candidate = result.candidate,
                                        existing = result.existing,
                                        completed = completed,
                                        total = batchTotal,
                                        imported = imported,
                                        failed = failures.size,
                                    ),
                                )
                            }
                            return@launch
                        }
                    }
                }
                publishCompleted()
            } catch (cancelled: CancellationException) {
                publishCancelled()
                throw cancelled
            }
        }
    }

    private fun publishProgress() {
        mutableUiState.update {
            it.copy(
                importState = LocalImportUiState.Running(
                    completed = completed,
                    total = batchTotal,
                    imported = imported,
                    failed = failures.size,
                ),
            )
        }
    }

    private fun publishCancelled() {
        skipped += (batchTotal - completed).coerceAtLeast(0)
        completed = batchTotal
        publishCompleted()
    }

    private fun publishCompleted() {
        importJob = null
        pendingUris.clear()
        pendingDuplicateUri = null
        mutableUiState.update {
            it.copy(
                importState = LocalImportUiState.Completed(
                    total = batchTotal,
                    imported = imported,
                    skipped = skipped,
                    failures = failures.toList(),
                ),
            )
        }
    }

    private fun LocalMediaDirectoryScanFailure.asUiFailure() = when (this) {
        LocalMediaDirectoryScanFailure.InvalidTree -> LocalDirectoryImportFailure.InvalidTree
        LocalMediaDirectoryScanFailure.PermissionDenied -> LocalDirectoryImportFailure.PermissionDenied
        LocalMediaDirectoryScanFailure.Unavailable -> LocalDirectoryImportFailure.Unavailable
    }
}

private object EmptyKaraokeRepository : KaraokeRepository {
    override fun observeProjects() = flowOf(emptyList<com.resonote.core.model.KaraokeProject>())
    override suspend fun findProject(id: KaraokeProjectId) = null
    override suspend fun prepareProject(request: com.resonote.core.data.KaraokePreparationRequest) =
        com.resonote.core.data.PrepareKaraokeResult.Failed(
            com.resonote.core.data.KaraokePreparationFailure.SourceUnavailable,
        )
    override suspend fun selectBackingSource(
        projectId: KaraokeProjectId,
        sourceMode: com.resonote.core.model.KaraokeSourceMode,
        timelineStartMillis: Long,
    ) = false
    override suspend fun setTrimStart(projectId: KaraokeProjectId, trimStartMillis: Long) = false
    override suspend fun createRecordingFile(projectId: KaraokeProjectId, expectedDurationMillis: Long) =
        com.resonote.core.data.KaraokeRecordingFileResult.Failed
    override suspend fun commitRecordingSegment(
        projectId: KaraokeProjectId,
        segmentId: String,
        path: String,
        timelineStartMillis: Long,
        durationMillis: Long,
        peakAmplitude: Int,
    ) = com.resonote.core.data.KaraokeRecordingCommitResult.Failed
    override suspend fun updateMix(projectId: KaraokeProjectId, settings: KaraokeMixSettings) = false
    override suspend fun renderInput(projectId: KaraokeProjectId) = null
    override suspend fun updateExportStatus(
        projectId: KaraokeProjectId,
        status: com.resonote.core.model.KaraokeProjectStatus,
        exportedContentUri: String?,
    ) = false
    override suspend fun deleteProjects(projectIds: Set<KaraokeProjectId>) = false
}

private object EmptyKaraokeExportController : KaraokeExportController {
    override fun export(projectIds: Set<KaraokeProjectId>) = false
}

private object EmptyKaraokePreviewController : KaraokePreviewController {
    override val state = MutableStateFlow(KaraokePreviewState())
    override fun toggle(projectId: KaraokeProjectId, mixSettings: KaraokeMixSettings?) = Unit
    override fun stop() = Unit
}

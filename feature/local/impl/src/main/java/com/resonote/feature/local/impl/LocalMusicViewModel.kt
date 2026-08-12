package com.resonote.feature.local.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.LocalMediaRepository
import com.resonote.core.model.LocalMedia
import com.resonote.core.model.LocalMediaDeleteResult
import com.resonote.core.model.LocalMediaDuplicateAction
import com.resonote.core.model.LocalMediaImportFailure
import com.resonote.core.model.LocalMediaImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LocalMusicViewModel @Inject constructor(
    private val repository: LocalMediaRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(LocalMusicUiState())
    val uiState: StateFlow<LocalMusicUiState> = mutableUiState.asStateFlow()

    private var importJob: Job? = null
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
    }

    fun updateQuery(query: String) {
        mutableUiState.update { it.copy(query = query) }
    }

    fun updateSort(sort: LocalMusicSort) {
        mutableUiState.update { it.copy(sort = sort) }
    }

    fun importUris(uris: List<String>): Boolean {
        if (importJob?.isActive == true || pendingDuplicateUri != null) return false
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
                    is LocalMediaImportResult.DuplicateConfirmationRequired -> failures +=
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
        if (importJob?.isActive != true && pendingDuplicateUri == null) return
        importJob?.cancel()
        pendingDuplicateUri = null
        pendingUris.clear()
        publishCancelled()
    }

    fun dismissImportResult() {
        if (mutableUiState.value.importState is LocalImportUiState.Completed) {
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
}

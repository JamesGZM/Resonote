package com.resonote.feature.cloud.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.CloudRepository
import com.resonote.core.model.CloudPage
import com.resonote.core.model.CloudTrack
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ResolveSongSourceResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CloudViewModel @Inject constructor(private val repository: CloudRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(CloudUiState())
    val uiState: StateFlow<CloudUiState> = mutableUiState.asStateFlow()

    private val mutablePlaybackRequests = MutableSharedFlow<CloudPlaybackRequest>(extraBufferCapacity = 1)
    val playbackRequests: SharedFlow<CloudPlaybackRequest> = mutablePlaybackRequests.asSharedFlow()

    private var pageJob: Job? = null
    private var indexJob: Job? = null
    private var playbackJob: Job? = null

    init {
        loadInitial(refresh = false)
    }

    fun refresh() = loadInitial(refresh = true)

    fun loadMore() {
        val state = mutableUiState.value
        if (pageJob?.isActive == true || indexJob?.isActive == true || !state.hasMore || state.page < 1) return
        pageJob = viewModelScope.launch {
            mutableUiState.update { it.copy(isLoadingMore = true, loadMoreFailure = null) }
            when (val result = repository.loadTracks(state.page + 1, PAGE_SIZE)) {
                is CollectionLoadResult.Available -> appendPage(result.value, indexing = false)
                is CollectionLoadResult.Failed -> mutableUiState.update {
                    it.copy(isLoadingMore = false, loadMoreFailure = result.failure)
                }
            }
        }
    }

    fun retryMore() {
        if (mutableUiState.value.query.isNotBlank()) ensureCompleteSearchIndex() else loadMore()
    }

    fun updateQuery(query: String) {
        mutableUiState.update { it.copy(query = query) }
        if (query.isNotBlank()) ensureCompleteSearchIndex()
    }

    fun updateSort(sort: CloudSort) {
        mutableUiState.update { it.copy(sort = sort) }
    }

    fun updateViewMode(viewMode: CloudViewMode) {
        mutableUiState.update { it.copy(viewMode = viewMode) }
    }

    fun playTrack(trackHash: String) {
        val tracks = mutableUiState.value.visibleTracks
        val index = tracks.indexOfFirst { it.hash == trackHash }
        if (index >= 0) resolvePlayback(tracks, index)
    }

    fun playAll() {
        val tracks = mutableUiState.value.visibleTracks
        if (tracks.isNotEmpty()) resolvePlayback(tracks, 0)
    }

    fun retryPlayback() {
        val failed = mutableUiState.value.playback as? CloudPlaybackUiState.Failed ?: return
        playTrack(failed.trackHash)
    }

    fun dismissPlaybackIssue() {
        mutableUiState.update { it.copy(playback = CloudPlaybackUiState.Idle) }
    }

    private fun loadInitial(refresh: Boolean) {
        if (pageJob?.isActive == true) return
        indexJob?.cancel()
        pageJob = viewModelScope.launch {
            mutableUiState.update {
                if (refresh && it.tracks.isNotEmpty()) {
                    it.copy(isRefreshing = true, failure = null, loadMoreFailure = null)
                } else {
                    it.copy(initialLoading = true, failure = null, loadMoreFailure = null)
                }
            }
            when (val result = repository.loadTracks(page = 1, pageSize = PAGE_SIZE)) {
                is CollectionLoadResult.Available -> mutableUiState.update { current ->
                    current.copy(
                        tracks = result.value.tracks.distinctBy(CloudTrack::hash),
                        page = result.value.page,
                        total = result.value.total,
                        hasMore = result.value.hasMore,
                        storage = result.value.storage ?: current.storage,
                        initialLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        isIndexing = false,
                        failure = null,
                    )
                }
                is CollectionLoadResult.Failed -> mutableUiState.update {
                    val hasContent = it.tracks.isNotEmpty()
                    it.copy(
                        initialLoading = false,
                        isRefreshing = false,
                        isIndexing = false,
                        failure = result.failure.takeUnless { hasContent },
                        loadMoreFailure = result.failure.takeIf { hasContent },
                    )
                }
            }
            pageJob = null
            if (mutableUiState.value.query.isNotBlank()) ensureCompleteSearchIndex()
        }
    }

    private fun ensureCompleteSearchIndex() {
        val state = mutableUiState.value
        if (indexJob?.isActive == true || pageJob?.isActive == true || !state.hasMore || state.page < 1) return
        indexJob = viewModelScope.launch {
            mutableUiState.update { it.copy(isIndexing = true, loadMoreFailure = null) }
            while (mutableUiState.value.query.isNotBlank() && mutableUiState.value.hasMore) {
                val page = mutableUiState.value.page + 1
                when (val result = repository.loadTracks(page, PAGE_SIZE)) {
                    is CollectionLoadResult.Available -> appendPage(result.value, indexing = true)
                    is CollectionLoadResult.Failed -> {
                        mutableUiState.update {
                            it.copy(isIndexing = false, loadMoreFailure = result.failure)
                        }
                        return@launch
                    }
                }
            }
            mutableUiState.update { it.copy(isIndexing = false) }
        }
    }

    private fun appendPage(page: CloudPage, indexing: Boolean) {
        mutableUiState.update { current ->
            current.copy(
                tracks = (current.tracks + page.tracks).distinctBy(CloudTrack::hash),
                page = page.page,
                total = page.total,
                hasMore = page.hasMore,
                storage = page.storage ?: current.storage,
                isLoadingMore = false,
                isIndexing = indexing && page.hasMore && current.query.isNotBlank(),
                loadMoreFailure = null,
            )
        }
    }

    private fun resolvePlayback(tracks: List<CloudTrack>, startIndex: Int) {
        if (playbackJob?.isActive == true) return
        val track = tracks[startIndex]
        mutableUiState.update { it.copy(playback = CloudPlaybackUiState.Resolving(track.hash)) }
        playbackJob = viewModelScope.launch {
            when (val result = repository.resolveSource(track)) {
                is ResolveSongSourceResult.Resolved -> {
                    mutableUiState.update { it.copy(playback = CloudPlaybackUiState.Idle) }
                    mutablePlaybackRequests.emit(CloudPlaybackRequest(tracks, startIndex, result.source))
                }
                is ResolveSongSourceResult.Unavailable -> mutableUiState.update {
                    it.copy(playback = CloudPlaybackUiState.Failed(track.hash, CloudPlaybackIssue.Unavailable))
                }
                is ResolveSongSourceResult.Failed -> mutableUiState.update {
                    it.copy(
                        playback = CloudPlaybackUiState.Failed(track.hash, CloudPlaybackIssue.Failed(result.failure)),
                    )
                }
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 50
    }
}

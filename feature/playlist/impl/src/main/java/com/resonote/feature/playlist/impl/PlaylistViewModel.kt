package com.resonote.feature.playlist.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.PlaylistRepository
import com.resonote.core.model.CollectionLoadResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val repository: PlaylistRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<PlaylistUiState>(PlaylistUiState.Loading)
    val uiState: StateFlow<PlaylistUiState> = mutableUiState.asStateFlow()

    private var playlistId: String? = null
    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null

    fun load(id: String) {
        if (id == playlistId && mutableUiState.value !is PlaylistUiState.Error) return
        playlistId = id
        loadJob?.cancel()
        loadMoreJob?.cancel()
        mutableUiState.value = PlaylistUiState.Loading
        loadJob = viewModelScope.launch {
            when (val result = repository.loadPlaylist(id, page = 1)) {
                is CollectionLoadResult.Available -> {
                    val value = result.value
                    mutableUiState.value = if (value.details == null && value.songs.isEmpty()) {
                        PlaylistUiState.Empty
                    } else {
                        PlaylistUiState.Content(value.details, value.songs, value.page, value.hasMore)
                    }
                }
                is CollectionLoadResult.Failed -> mutableUiState.value = PlaylistUiState.Error(result.failure)
            }
        }
    }

    fun retry() {
        playlistId?.let { id ->
            playlistId = null
            load(id)
        }
    }

    fun loadMore() {
        if (loadMoreJob?.isActive == true) return
        val id = playlistId ?: return
        val current = mutableUiState.value as? PlaylistUiState.Content ?: return
        if (!current.hasMore || current.isLoadingMore) return
        mutableUiState.value = current.copy(isLoadingMore = true, loadMoreFailure = null)
        loadMoreJob = viewModelScope.launch {
            when (val result = repository.loadPlaylist(id, page = current.page + 1)) {
                is CollectionLoadResult.Available -> mutableUiState.update { state ->
                    val latest = state as? PlaylistUiState.Content ?: return@update state
                    val existing = latest.songs.mapTo(mutableSetOf()) { it.hash }
                    latest.copy(
                        details = latest.details ?: result.value.details,
                        songs = latest.songs + result.value.songs.filter { existing.add(it.hash) },
                        page = result.value.page,
                        hasMore = result.value.hasMore,
                        isLoadingMore = false,
                        loadMoreFailure = null,
                    )
                }
                is CollectionLoadResult.Failed -> mutableUiState.update { state ->
                    val latest = state as? PlaylistUiState.Content ?: return@update state
                    latest.copy(isLoadingMore = false, loadMoreFailure = result.failure)
                }
            }
        }
    }
}

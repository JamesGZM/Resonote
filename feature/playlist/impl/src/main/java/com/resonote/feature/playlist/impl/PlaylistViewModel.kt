package com.resonote.feature.playlist.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.LibraryRepository
import com.resonote.core.data.PlaylistRepository
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.OnlineSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<PlaylistUiState>(PlaylistUiState.Loading)
    val uiState: StateFlow<PlaylistUiState> = mutableUiState.asStateFlow()

    private var playlistId: String? = null
    private var writableListId: String? = null
    private var writableAccountId: String? = null
    private var loadGeneration = 0
    private var mutationGeneration = 0
    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null
    private var removeJob: Job? = null

    fun load(id: String, writableListId: String? = null, accountId: String? = null) {
        val normalizedListId = writableListId?.takeIf { accountId != null }
        val normalizedAccountId = accountId?.takeIf { normalizedListId != null }
        val playlistChanged = id != playlistId
        val writeContextChanged = normalizedListId != this.writableListId ||
            normalizedAccountId != writableAccountId
        if (!playlistChanged && !writeContextChanged && mutableUiState.value !is PlaylistUiState.Error) return

        if (playlistChanged || writeContextChanged) {
            mutationGeneration += 1
            removeJob?.cancel()
            removeJob = null
            this.writableListId = normalizedListId
            writableAccountId = normalizedAccountId
        }
        if (!playlistChanged) {
            mutableUiState.update { state ->
                (state as? PlaylistUiState.Content)?.copy(
                    writableListId = normalizedListId,
                    removal = PlaylistRemovalUiState.Idle,
                ) ?: state
            }
            if (mutableUiState.value !is PlaylistUiState.Error) return
        }

        playlistId = id
        loadGeneration += 1
        val generation = loadGeneration
        loadJob?.cancel()
        loadMoreJob?.cancel()
        removeJob?.cancel()
        mutableUiState.value = PlaylistUiState.Loading
        loadJob = viewModelScope.launch {
            when (val result = repository.loadPlaylist(id, page = 1)) {
                is CollectionLoadResult.Available -> {
                    if (generation != loadGeneration || id != playlistId) return@launch
                    val value = result.value
                    mutableUiState.value = if (value.details == null && value.songs.isEmpty()) {
                        PlaylistUiState.Empty
                    } else {
                        PlaylistUiState.Content(
                            value.details,
                            value.songs.distinctBy(OnlineSong::hash),
                            value.page,
                            value.hasMore,
                            writableListId = this@PlaylistViewModel.writableListId,
                        )
                    }
                }
                is CollectionLoadResult.Failed -> {
                    if (generation == loadGeneration && id == playlistId) {
                        mutableUiState.value = PlaylistUiState.Error(result.failure)
                    }
                }
            }
        }
    }

    fun retry() {
        val id = playlistId ?: return
        val listId = writableListId
        val accountId = writableAccountId
        playlistId = null
        load(id, listId, accountId)
    }

    fun refresh() {
        val id = playlistId ?: return
        val current = mutableUiState.value as? PlaylistUiState.Content ?: return
        if (current.isRefreshing ||
            removeJob?.isActive == true ||
            current.removal != PlaylistRemovalUiState.Idle
        ) {
            return
        }
        loadMoreJob?.cancel()
        mutableUiState.value = current.copy(
            isLoadingMore = false,
            loadMoreFailure = null,
            isRefreshing = true,
            refreshFailure = null,
        )
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            when (val result = repository.loadPlaylist(id, page = 1)) {
                is CollectionLoadResult.Available -> {
                    val value = result.value
                    mutableUiState.value = if (value.details == null && value.songs.isEmpty()) {
                        PlaylistUiState.Empty
                    } else {
                        PlaylistUiState.Content(
                            details = value.details ?: current.details,
                            songs = value.songs.distinctBy(OnlineSong::hash),
                            page = value.page,
                            hasMore = value.hasMore,
                            writableListId = current.writableListId,
                        )
                    }
                }
                is CollectionLoadResult.Failed -> mutableUiState.update { state ->
                    val latest = state as? PlaylistUiState.Content ?: return@update state
                    latest.copy(isRefreshing = false, refreshFailure = result.failure)
                }
            }
        }
    }

    fun acknowledgeRefreshFailure() {
        mutableUiState.update { state ->
            val content = state as? PlaylistUiState.Content ?: return@update state
            content.copy(refreshFailure = null)
        }
    }

    fun loadMore() {
        if (loadMoreJob?.isActive == true || removeJob?.isActive == true) return
        val id = playlistId ?: return
        val current = mutableUiState.value as? PlaylistUiState.Content ?: return
        if (!current.hasMore || current.isLoadingMore || current.isRefreshing) return
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

    fun removeSong(song: OnlineSong) {
        if (removeJob?.isActive == true || loadMoreJob?.isActive == true) return
        val current = mutableUiState.value as? PlaylistUiState.Content ?: return
        if (current.isRefreshing) return
        val listId = writableListId ?: return
        val accountId = writableAccountId ?: return
        val fileId = song.fileId?.takeIf(String::isNotBlank) ?: return
        if (current.writableListId != listId || current.songs.none { it.hash == song.hash }) return

        val generation = mutationGeneration
        mutableUiState.value = current.copy(removal = PlaylistRemovalUiState.Removing(song.hash))
        removeJob = viewModelScope.launch {
            try {
                when (val result = libraryRepository.removeTracks(listId, listOf(fileId))) {
                    is CollectionLoadResult.Available -> mutableUiState.update { state ->
                        if (!isCurrentMutation(generation, listId, accountId)) return@update state
                        val latest = state as? PlaylistUiState.Content ?: return@update state
                        latest.copy(
                            details = latest.details?.copy(songCount = (latest.details.songCount - 1).coerceAtLeast(0)),
                            songs = latest.songs.filterNot { it.hash == song.hash },
                            removal = PlaylistRemovalUiState.Removed(song.title),
                        )
                    }
                    is CollectionLoadResult.Failed -> mutableUiState.update { state ->
                        if (!isCurrentMutation(generation, listId, accountId)) return@update state
                        val latest = state as? PlaylistUiState.Content ?: return@update state
                        latest.copy(removal = PlaylistRemovalUiState.Failed(song.hash, result.failure))
                    }
                }
            } finally {
                if (generation == mutationGeneration) removeJob = null
            }
        }
    }

    fun dismissRemovalFailure() {
        mutableUiState.update { state ->
            val content = state as? PlaylistUiState.Content ?: return@update state
            if (content.removal is PlaylistRemovalUiState.Failed) {
                content.copy(removal = PlaylistRemovalUiState.Idle)
            } else {
                state
            }
        }
    }

    fun acknowledgeRemoval() {
        mutableUiState.update { state ->
            val content = state as? PlaylistUiState.Content ?: return@update state
            if (content.removal is PlaylistRemovalUiState.Removed) {
                content.copy(removal = PlaylistRemovalUiState.Idle)
            } else {
                state
            }
        }
    }

    private fun isCurrentMutation(generation: Int, listId: String, accountId: String): Boolean =
        generation == mutationGeneration && listId == writableListId && accountId == writableAccountId
}

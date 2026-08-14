package com.resonote.feature.album.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.ContentCatalogRepository
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.OnlineSong
import com.resonote.feature.album.api.AlbumNavKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(private val repository: ContentCatalogRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow<AlbumUiState>(AlbumUiState.Loading)
    val uiState: StateFlow<AlbumUiState> = mutableUiState.asStateFlow()

    private var albumKey: AlbumNavKey? = null
    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null

    fun load(key: AlbumNavKey) {
        if (key == albumKey && mutableUiState.value !is AlbumUiState.Error) return
        albumKey = key
        loadJob?.cancel()
        loadMoreJob?.cancel()
        mutableUiState.value = AlbumUiState.Loading
        loadJob = viewModelScope.launch {
            when (val result = repository.loadAlbumSongs(key.albumId, page = 1)) {
                is CollectionLoadResult.Available -> {
                    val page = result.value
                    val metadata = key.metadata(page.songs.firstOrNull(), page.total)
                    mutableUiState.value = if (page.songs.isEmpty()) {
                        AlbumUiState.Empty(metadata)
                    } else {
                        AlbumUiState.Content(metadata, page.songs, page.page, page.hasMore)
                    }
                }
                is CollectionLoadResult.Failed -> {
                    mutableUiState.value = AlbumUiState.Error(result.failure, key.name.nonBlank())
                }
            }
        }
    }

    fun retry() {
        albumKey?.let { key ->
            albumKey = null
            load(key)
        }
    }

    fun loadMore() {
        if (loadMoreJob?.isActive == true) return
        val key = albumKey ?: return
        val current = mutableUiState.value as? AlbumUiState.Content ?: return
        if (!current.hasMore || current.isLoadingMore) return
        mutableUiState.value = current.copy(isLoadingMore = true, loadMoreFailure = null)
        loadMoreJob = viewModelScope.launch {
            when (val result = repository.loadAlbumSongs(key.albumId, page = current.page + 1)) {
                is CollectionLoadResult.Available -> mutableUiState.update { state ->
                    val latest = state as? AlbumUiState.Content ?: return@update state
                    val existing = latest.songs.mapTo(mutableSetOf()) { it.hash }
                    latest.copy(
                        songs = latest.songs + result.value.songs.filter { existing.add(it.hash) },
                        page = result.value.page,
                        hasMore = result.value.hasMore,
                        isLoadingMore = false,
                        loadMoreFailure = null,
                    )
                }
                is CollectionLoadResult.Failed -> mutableUiState.update { state ->
                    val latest = state as? AlbumUiState.Content ?: return@update state
                    latest.copy(isLoadingMore = false, loadMoreFailure = result.failure)
                }
            }
        }
    }
}

private fun AlbumNavKey.metadata(firstSong: OnlineSong?, total: Int) = AlbumMetadata(
    id = albumId,
    title = name.nonBlank() ?: firstSong?.albumTitle.nonBlank(),
    artist = artist.nonBlank() ?: firstSong?.artist.nonBlank(),
    coverUrl = coverUrl.nonBlank() ?: firstSong?.coverUrl.nonBlank(),
    publishDate = publishDate.nonBlank(),
    songCount = total.takeIf { it >= 0 } ?: songCount,
)

private fun String?.nonBlank(): String? = this?.takeIf(String::isNotBlank)

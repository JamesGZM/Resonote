package com.resonote.feature.ranking.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.RankingRepository
import com.resonote.core.model.CollectionLoadResult
import com.resonote.feature.ranking.api.RankingNavKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RankingViewModel @Inject constructor(private val repository: RankingRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow<RankingUiState>(
        RankingUiState.Loading(RankingMetadata("", null, null)),
    )
    val uiState: StateFlow<RankingUiState> = mutableUiState.asStateFlow()

    private var rankingKey: RankingNavKey? = null
    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null

    fun load(key: RankingNavKey) {
        if (key == rankingKey && mutableUiState.value !is RankingUiState.Error) return
        rankingKey = key
        loadJob?.cancel()
        loadMoreJob?.cancel()
        val metadata = key.metadata()
        mutableUiState.value = RankingUiState.Loading(metadata)
        loadJob = viewModelScope.launch {
            when (val result = repository.loadSongs(key.rankingId, page = 1)) {
                is CollectionLoadResult.Available -> {
                    val value = result.value
                    mutableUiState.value = if (value.songs.isEmpty()) {
                        RankingUiState.Empty(metadata)
                    } else {
                        RankingUiState.Content(
                            metadata = metadata,
                            songs = value.songs,
                            page = value.page,
                            total = value.total,
                            hasMore = value.hasMore,
                        )
                    }
                }
                is CollectionLoadResult.Failed -> {
                    mutableUiState.value = RankingUiState.Error(metadata, result.failure)
                }
            }
        }
    }

    fun retry() {
        rankingKey?.let { key ->
            rankingKey = null
            load(key)
        }
    }

    fun refresh() {
        val key = rankingKey ?: return
        val current = mutableUiState.value as? RankingUiState.Content ?: return
        if (current.isRefreshing) return
        loadMoreJob?.cancel()
        mutableUiState.value = current.copy(
            isLoadingMore = false,
            loadMoreFailure = null,
            isRefreshing = true,
            refreshFailure = null,
        )
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            when (val result = repository.loadSongs(key.rankingId, page = 1)) {
                is CollectionLoadResult.Available -> {
                    val value = result.value
                    mutableUiState.value = if (value.songs.isEmpty()) {
                        RankingUiState.Empty(current.metadata)
                    } else {
                        RankingUiState.Content(
                            metadata = current.metadata,
                            songs = value.songs,
                            page = value.page,
                            total = value.total,
                            hasMore = value.hasMore,
                        )
                    }
                }
                is CollectionLoadResult.Failed -> mutableUiState.update { state ->
                    val latest = state as? RankingUiState.Content ?: return@update state
                    latest.copy(isRefreshing = false, refreshFailure = result.failure)
                }
            }
        }
    }

    fun acknowledgeRefreshFailure() {
        mutableUiState.update { state ->
            val content = state as? RankingUiState.Content ?: return@update state
            content.copy(refreshFailure = null)
        }
    }

    fun loadMore() {
        if (loadMoreJob?.isActive == true) return
        val key = rankingKey ?: return
        val current = mutableUiState.value as? RankingUiState.Content ?: return
        if (!current.hasMore || current.isLoadingMore || current.isRefreshing) return
        mutableUiState.value = current.copy(isLoadingMore = true, loadMoreFailure = null)
        loadMoreJob = viewModelScope.launch {
            when (val result = repository.loadSongs(key.rankingId, page = current.page + 1)) {
                is CollectionLoadResult.Available -> mutableUiState.update { state ->
                    val latest = state as? RankingUiState.Content ?: return@update state
                    val existing = latest.songs.mapTo(mutableSetOf()) { it.hash }
                    latest.copy(
                        songs = latest.songs + result.value.songs.filter { existing.add(it.hash) },
                        page = result.value.page,
                        total = result.value.total ?: latest.total,
                        hasMore = result.value.hasMore,
                        isLoadingMore = false,
                        loadMoreFailure = null,
                    )
                }
                is CollectionLoadResult.Failed -> mutableUiState.update { state ->
                    val latest = state as? RankingUiState.Content ?: return@update state
                    latest.copy(isLoadingMore = false, loadMoreFailure = result.failure)
                }
            }
        }
    }
}

private fun RankingNavKey.metadata() = RankingMetadata(
    id = rankingId,
    title = title?.takeIf(String::isNotBlank),
    coverUrl = coverUrl?.takeIf(String::isNotBlank),
)

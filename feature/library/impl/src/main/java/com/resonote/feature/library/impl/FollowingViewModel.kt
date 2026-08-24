package com.resonote.feature.library.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.ContentCatalogRepository
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.FollowedArtist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FollowingViewModel @Inject constructor(private val repository: ContentCatalogRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow<FollowingUiState>(FollowingUiState.Loading)
    val uiState: StateFlow<FollowingUiState> = mutableUiState.asStateFlow()

    private var allArtists: List<FollowedArtist> = emptyList()
    private var visibleCount = PAGE_SIZE
    private var loadJob: Job? = null
    private val updateJobs = mutableMapOf<String, Job>()

    init {
        load()
    }

    fun retry() {
        if (loadJob?.isActive == true) return
        mutableUiState.value = FollowingUiState.Loading
        load()
    }

    fun refresh() {
        val current = mutableUiState.value as? FollowingUiState.Content ?: return
        if (current.isRefreshing || loadJob?.isActive == true) return
        mutableUiState.value = current.copy(isRefreshing = true, refreshingFailure = null)
        load(isRefresh = true)
    }

    fun loadMore() {
        val current = mutableUiState.value as? FollowingUiState.Content ?: return
        if (!current.hasMore || current.isRefreshing) return
        visibleCount = (visibleCount + PAGE_SIZE).coerceAtMost(allArtists.size)
        mutableUiState.value = contentState(current)
    }

    fun unfollow(artist: FollowedArtist) {
        val current = mutableUiState.value as? FollowingUiState.Content ?: return
        if (artist.id in current.updatingArtistIds || updateJobs[artist.id]?.isActive == true) return
        mutableUiState.value = current.copy(
            updatingArtistIds = current.updatingArtistIds + artist.id,
            updateFailure = null,
        )
        updateJobs[artist.id] = viewModelScope.launch {
            when (val result = repository.setArtistFollowed(artist.id, followed = false)) {
                is CollectionLoadResult.Available -> {
                    allArtists = allArtists.filterNot { it.id == artist.id }
                    visibleCount = visibleCount.coerceAtMost(allArtists.size).coerceAtLeast(PAGE_SIZE)
                    val latest = mutableUiState.value as? FollowingUiState.Content
                    mutableUiState.value = if (allArtists.isEmpty()) {
                        FollowingUiState.Empty
                    } else {
                        contentState(latest)
                    }
                }
                is CollectionLoadResult.Failed -> mutableUiState.update { state ->
                    val latest = state as? FollowingUiState.Content ?: return@update state
                    latest.copy(
                        updatingArtistIds = latest.updatingArtistIds - artist.id,
                        updateFailure = result.failure,
                    )
                }
            }
            updateJobs.remove(artist.id)
        }
    }

    fun acknowledgeFailure() {
        mutableUiState.update { state ->
            val content = state as? FollowingUiState.Content ?: return@update state
            content.copy(refreshingFailure = null, updateFailure = null)
        }
    }

    private fun load(isRefresh: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            when (val result = repository.loadFollowedArtists()) {
                is CollectionLoadResult.Available -> {
                    allArtists = result.value.distinctBy(FollowedArtist::id)
                    if (!isRefresh) visibleCount = PAGE_SIZE
                    visibleCount = visibleCount.coerceAtMost(allArtists.size).coerceAtLeast(PAGE_SIZE)
                    mutableUiState.value = if (allArtists.isEmpty()) {
                        FollowingUiState.Empty
                    } else {
                        contentState()
                    }
                }
                is CollectionLoadResult.Failed -> {
                    val current = mutableUiState.value as? FollowingUiState.Content
                    mutableUiState.value = if (isRefresh && current != null) {
                        current.copy(isRefreshing = false, refreshingFailure = result.failure)
                    } else {
                        FollowingUiState.Error(result.failure)
                    }
                }
            }
        }
    }

    private fun contentState(previous: FollowingUiState.Content? = null): FollowingUiState.Content {
        val artists = allArtists.take(visibleCount)
        return FollowingUiState.Content(
            artists = artists,
            total = allArtists.size,
            hasMore = artists.size < allArtists.size,
            updatingArtistIds = previous?.updatingArtistIds.orEmpty().intersect(
                artists.mapTo(mutableSetOf()) {
                    it.id
                },
            ),
        )
    }

    private companion object {
        const val PAGE_SIZE = 30
    }
}

package com.resonote.feature.home.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.HomeRepository
import com.resonote.core.model.HomeRefreshResult
import com.resonote.core.model.HomeSection
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.RadioRecommendationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: HomeRepository) : ViewModel() {
    private val refreshState = MutableStateFlow<RefreshState>(RefreshState.Idle)
    private val radioSongs = MutableStateFlow<List<OnlineSong>>(emptyList())
    private val mutableRefreshFailures = MutableSharedFlow<Set<HomeSection>>(extraBufferCapacity = 1)
    val refreshFailures: SharedFlow<Set<HomeSection>> = mutableRefreshFailures
    private var refreshJob: Job? = null

    val uiState: StateFlow<HomeUiState> =
        combine(repository.content, refreshState, radioSongs) { content, refresh, radio ->
            when {
                content != null ->
                    HomeUiState.Content(
                        content = content.toUiState(radio),
                        isRefreshing = refresh is RefreshState.Refreshing,
                        issues = (refresh as? RefreshState.Complete)?.issues.orEmpty(),
                    )

                refresh is RefreshState.Complete -> HomeUiState.Error(refresh.issues)
                else -> HomeUiState.Loading
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = HomeUiState.Loading,
        )

    init {
        refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        val hadContent = repository.content.value != null
        refreshState.value = RefreshState.Refreshing

        refreshJob = viewModelScope.launch {
            val result = repository.refresh()
            val issues = when (result) {
                is HomeRefreshResult.Updated -> result.issues.failedSections()
                is HomeRefreshResult.Failed -> result.issues.failedSections()
                HomeRefreshResult.Superseded -> emptySet()
            }
            refreshState.value = if (result == HomeRefreshResult.Superseded) {
                RefreshState.Idle
            } else {
                RefreshState.Complete(issues)
            }
            if (hadContent && issues.isNotEmpty()) mutableRefreshFailures.emit(issues)
        }
    }

    suspend fun radioPlaybackRequest(): HomePlaybackRequest? = when (val result = repository.loadRadio()) {
        is RadioRecommendationResult.Available -> {
            radioSongs.value = result.songs
            result.songs.takeIf { it.isNotEmpty() }?.let { HomePlaybackRequest(it, 0) }
        }

        is RadioRecommendationResult.Failed -> null
    }

    fun playbackRequest(collection: HomeSongCollection, mediaId: String? = null): HomePlaybackRequest? {
        val content = repository.content.value ?: return null
        val songs =
            when (collection) {
                HomeSongCollection.RADIO -> radioSongs.value
                HomeSongCollection.DAILY_RECOMMENDATIONS -> content.dailyRecommendations
                HomeSongCollection.NEW_SONGS -> content.newSongs
            }
        if (songs.isEmpty()) return null
        val requestedIndex = mediaId?.let { id -> songs.indexOfFirst { it.hash == id } } ?: 0
        return HomePlaybackRequest(songs, requestedIndex.takeIf { it >= 0 } ?: 0)
    }

    fun songForAction(mediaId: String): OnlineSong? {
        val content = repository.content.value
        return (radioSongs.value + content?.dailyRecommendations.orEmpty() + content?.newSongs.orEmpty())
            .firstOrNull { it.hash == mediaId }
    }

    private sealed interface RefreshState {
        data object Idle : RefreshState

        data object Refreshing : RefreshState

        data class Complete(val issues: Set<HomeSection>) : RefreshState
    }
}

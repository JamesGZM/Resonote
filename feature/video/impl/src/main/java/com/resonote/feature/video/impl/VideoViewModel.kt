package com.resonote.feature.video.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.VideoRepository
import com.resonote.core.model.CollectionLoadResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class VideoViewModel @Inject constructor(
    private val repository: VideoRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<VideoUiState>(VideoUiState.Idle)
    val uiState: StateFlow<VideoUiState> = mutableUiState.asStateFlow()

    private var currentHash: String? = null
    private var loadJob: Job? = null

    fun load(hash: String) = load(hash, force = false)

    fun retry() {
        currentHash?.let { load(it, force = true) }
    }

    private fun load(hash: String, force: Boolean) {
        require(hash.isNotBlank()) { "hash must not be blank" }
        if (!force && currentHash == hash && mutableUiState.value !is VideoUiState.Idle) return
        loadJob?.cancel()
        currentHash = hash
        loadJob = viewModelScope.launch {
            mutableUiState.value = VideoUiState.Loading
            mutableUiState.value = when (val result = repository.resolveVideoUrl(hash)) {
                is CollectionLoadResult.Available -> result.value
                    ?.takeIf(String::isNotBlank)
                    ?.let(VideoUiState::Ready)
                    ?: VideoUiState.Unavailable
                is CollectionLoadResult.Failed -> VideoUiState.Failed(result.failure)
            }
        }
    }
}

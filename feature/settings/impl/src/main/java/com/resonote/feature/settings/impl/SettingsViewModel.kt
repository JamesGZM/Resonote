package com.resonote.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.PlaybackPreferencesRepository
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.model.OnlinePlaybackQuality
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data object LoadFailed : SettingsUiState

    data class Ready(
        val playbackSpeed: PlaybackSpeed,
        val onlinePlaybackQuality: OnlinePlaybackQuality = OnlinePlaybackQuality.Standard,
        val isSaving: Boolean = false,
        val saveFailed: Boolean = false,
    ) : SettingsUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val playbackPreferencesRepository: PlaybackPreferencesRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    private var observationJob: Job? = null

    init {
        observePreferences()
    }

    fun retry() = observePreferences()

    fun setPlaybackSpeed(speed: PlaybackSpeed) {
        val state = mutableUiState.value as? SettingsUiState.Ready ?: return
        if (state.isSaving || state.playbackSpeed == speed) return

        mutableUiState.value = state.copy(isSaving = true, saveFailed = false)
        viewModelScope.launch {
            try {
                playbackPreferencesRepository.setPlaybackSpeed(speed)
                mutableUiState.value = state.copy(playbackSpeed = speed, isSaving = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                val current = mutableUiState.value as? SettingsUiState.Ready ?: state
                mutableUiState.value = current.copy(isSaving = false, saveFailed = true)
            }
        }
    }

    fun setOnlinePlaybackQuality(quality: OnlinePlaybackQuality) {
        val state = mutableUiState.value as? SettingsUiState.Ready ?: return
        if (state.isSaving || state.onlinePlaybackQuality == quality) return

        mutableUiState.value = state.copy(isSaving = true, saveFailed = false)
        viewModelScope.launch {
            try {
                playbackPreferencesRepository.setOnlinePlaybackQuality(quality)
                mutableUiState.value = state.copy(onlinePlaybackQuality = quality, isSaving = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                val current = mutableUiState.value as? SettingsUiState.Ready ?: state
                mutableUiState.value = current.copy(isSaving = false, saveFailed = true)
            }
        }
    }

    fun acknowledgeSaveFailure() {
        val state = mutableUiState.value as? SettingsUiState.Ready ?: return
        mutableUiState.value = state.copy(saveFailed = false)
    }

    private fun observePreferences() {
        if (observationJob?.isActive == true) return
        observationJob = viewModelScope.launch {
            mutableUiState.value = SettingsUiState.Loading
            try {
                combine(
                    playbackPreferencesRepository.playbackSpeed,
                    playbackPreferencesRepository.onlinePlaybackQuality,
                ) { speed, quality -> speed to quality }.collect { (speed, quality) ->
                    val current = mutableUiState.value as? SettingsUiState.Ready
                    mutableUiState.value = SettingsUiState.Ready(
                        playbackSpeed = speed,
                        onlinePlaybackQuality = quality,
                        isSaving = current?.isSaving == true,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableUiState.value = SettingsUiState.LoadFailed
            }
        }
    }
}

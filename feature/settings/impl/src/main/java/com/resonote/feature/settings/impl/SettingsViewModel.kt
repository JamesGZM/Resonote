package com.resonote.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.AuthRepository
import com.resonote.core.data.LyricsPreferencesRepository
import com.resonote.core.data.PlaybackPreferencesRepository
import com.resonote.core.data.ThemePreferencesRepository
import com.resonote.core.model.AudioFocusPolicy
import com.resonote.core.model.AuthState
import com.resonote.core.model.CrossfadeDuration
import com.resonote.core.model.EqualizerPreset
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.PlaybackMode
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.model.ThemeMode
import com.resonote.core.model.ThemePreferences
import com.resonote.core.playback.PlaybackCacheController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SettingsSaveKey {
    Theme,
    DynamicColor,
    Quality,
    PlaybackMode,
    Gapless,
    Crossfade,
    Speed,
    Loudness,
    AudioFocus,
    Equalizer,
    Reset,
    Logout,
}

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data object LoadFailed : SettingsUiState

    data class Ready(
        val playbackSpeed: PlaybackSpeed,
        val onlinePlaybackQuality: OnlinePlaybackQuality = OnlinePlaybackQuality.Standard,
        val themePreferences: ThemePreferences = ThemePreferences(),
        val playbackMode: PlaybackMode = PlaybackMode.ListLoop,
        val gaplessEnabled: Boolean = true,
        val crossfadeDuration: CrossfadeDuration = CrossfadeDuration.Off,
        val loudnessNormalizationEnabled: Boolean = false,
        val audioFocusPolicy: AudioFocusPolicy = AudioFocusPolicy.Disallow,
        val equalizerEnabled: Boolean = false,
        val equalizerLowDb: Int = 0,
        val equalizerMidDb: Int = 0,
        val equalizerHighDb: Int = 0,
        val equalizerCustom: Boolean = false,
        val cacheBytes: Long? = null,
        val isAuthenticated: Boolean = false,
        val savingKey: SettingsSaveKey? = null,
        val isClearingCache: Boolean = false,
        val saveFailed: Boolean = false,
    ) : SettingsUiState {
        val isSaving: Boolean get() = savingKey != null
        val equalizerPreset: EqualizerPreset
            get() = EqualizerPreset.from(
                equalizerEnabled,
                equalizerLowDb,
                equalizerMidDb,
                equalizerHighDb,
                equalizerCustom,
            )
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val playbackPreferencesRepository: PlaybackPreferencesRepository,
    private val themePreferencesRepository: ThemePreferencesRepository,
    private val playbackCacheController: PlaybackCacheController,
    private val authRepository: AuthRepository,
    private val lyricsPreferencesRepository: LyricsPreferencesRepository? = null,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()
    private var observationJob: Job? = null
    private var latestCacheBytes: Long? = null

    init {
        observePreferences()
        refreshCacheSize()
    }

    fun retry() {
        observePreferences(force = true)
        refreshCacheSize()
    }

    fun setPlaybackSpeed(value: PlaybackSpeed) = save(SettingsSaveKey.Speed) {
        playbackPreferencesRepository.setPlaybackSpeed(value)
    }
    fun setOnlinePlaybackQuality(value: OnlinePlaybackQuality) =
        save(SettingsSaveKey.Quality) { playbackPreferencesRepository.setOnlinePlaybackQuality(value) }
    fun setPlaybackMode(value: PlaybackMode) =
        save(SettingsSaveKey.PlaybackMode) { playbackPreferencesRepository.setPlaybackMode(value) }
    fun setGaplessEnabled(value: Boolean) =
        save(SettingsSaveKey.Gapless) { playbackPreferencesRepository.setGaplessEnabled(value) }
    fun setCrossfadeDuration(value: CrossfadeDuration) =
        save(SettingsSaveKey.Crossfade) { playbackPreferencesRepository.setCrossfadeDuration(value) }
    fun setLoudnessNormalizationEnabled(value: Boolean) =
        save(SettingsSaveKey.Loudness) { playbackPreferencesRepository.setLoudnessNormalizationEnabled(value) }
    fun setAudioFocusPolicy(value: AudioFocusPolicy) =
        save(SettingsSaveKey.AudioFocus) { playbackPreferencesRepository.setAudioFocusPolicy(value) }
    fun setEqualizerEnabled(value: Boolean) =
        save(SettingsSaveKey.Equalizer) { playbackPreferencesRepository.setEqualizerEnabled(value) }
    fun setEqualizerGains(lowDb: Int, midDb: Int, highDb: Int) =
        save(SettingsSaveKey.Equalizer) { playbackPreferencesRepository.setEqualizerGains(lowDb, midDb, highDb) }
    fun setEqualizerPreset(value: EqualizerPreset) =
        save(SettingsSaveKey.Equalizer) { playbackPreferencesRepository.setEqualizerPreset(value) }
    fun setThemeMode(value: ThemeMode) = save(SettingsSaveKey.Theme) { themePreferencesRepository.setThemeMode(value) }
    fun setDynamicColorEnabled(value: Boolean) =
        save(SettingsSaveKey.DynamicColor) { themePreferencesRepository.setDynamicColorEnabled(value) }
    fun resetSettings() = save(SettingsSaveKey.Reset) {
        playbackPreferencesRepository.reset()
        themePreferencesRepository.reset()
        lyricsPreferencesRepository?.reset()
    }
    fun logout() = save(SettingsSaveKey.Logout) { authRepository.logout() }

    fun clearCache() {
        val state = mutableUiState.value as? SettingsUiState.Ready ?: return
        if (state.isClearingCache) return
        mutableUiState.value = state.copy(isClearingCache = true, saveFailed = false)
        viewModelScope.launch {
            try {
                val remaining = playbackCacheController.clear()
                updateReady { it.copy(cacheBytes = remaining, isClearingCache = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                updateReady { it.copy(isClearingCache = false, saveFailed = true) }
            }
        }
    }

    fun acknowledgeSaveFailure() = updateReady { it.copy(saveFailed = false) }

    private fun observePreferences(force: Boolean = false) {
        if (!force && observationJob?.isActive == true) return
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            mutableUiState.value = SettingsUiState.Loading
            try {
                combine(
                    playbackPreferencesRepository.preferences,
                    themePreferencesRepository.themePreferences,
                    authRepository.authState,
                ) { playback, theme, authState ->
                    Triple(playback, theme, authState)
                }.collect { (playback, theme, authState) ->
                    val current = mutableUiState.value as? SettingsUiState.Ready
                    mutableUiState.value = SettingsUiState.Ready(
                        playbackSpeed = playback.playbackSpeed,
                        onlinePlaybackQuality = playback.onlinePlaybackQuality,
                        themePreferences = theme,
                        playbackMode = playback.playbackMode,
                        gaplessEnabled = playback.gaplessEnabled,
                        crossfadeDuration = playback.crossfadeDuration,
                        loudnessNormalizationEnabled = playback.loudnessNormalizationEnabled,
                        audioFocusPolicy = playback.audioFocusPolicy,
                        equalizerEnabled = playback.equalizerEnabled,
                        equalizerLowDb = playback.equalizerLowDb,
                        equalizerMidDb = playback.equalizerMidDb,
                        equalizerHighDb = playback.equalizerHighDb,
                        equalizerCustom = playback.equalizerCustom,
                        cacheBytes = current?.cacheBytes ?: latestCacheBytes,
                        isAuthenticated = authState is AuthState.Authenticated,
                        savingKey = current?.savingKey,
                        isClearingCache = current?.isClearingCache == true,
                        saveFailed = current?.saveFailed == true,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableUiState.value = SettingsUiState.LoadFailed
            }
        }
    }

    private fun refreshCacheSize() {
        viewModelScope.launch {
            val bytes = runCatching { playbackCacheController.sizeBytes() }.getOrNull() ?: return@launch
            latestCacheBytes = bytes
            updateReady { it.copy(cacheBytes = bytes) }
        }
    }

    private fun save(key: SettingsSaveKey, block: suspend () -> Unit) {
        val state = mutableUiState.value as? SettingsUiState.Ready ?: return
        if (state.savingKey != null) return
        mutableUiState.value = state.copy(savingKey = key, saveFailed = false)
        viewModelScope.launch {
            try {
                block()
                updateReady { it.copy(savingKey = null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                updateReady { it.copy(savingKey = null, saveFailed = true) }
            }
        }
    }

    private inline fun updateReady(transform: (SettingsUiState.Ready) -> SettingsUiState.Ready) {
        val current = mutableUiState.value as? SettingsUiState.Ready ?: return
        mutableUiState.value = transform(current)
    }
}

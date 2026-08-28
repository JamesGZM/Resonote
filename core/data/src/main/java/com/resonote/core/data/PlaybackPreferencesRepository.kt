package com.resonote.core.data

import com.resonote.core.model.AudioFocusPolicy
import com.resonote.core.model.CrossfadeDuration
import com.resonote.core.model.EqualizerPreset
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.PlaybackMode
import com.resonote.core.model.PlaybackPreferences
import com.resonote.core.model.PlaybackSpeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

interface PlaybackPreferencesRepository {
    val preferences: Flow<PlaybackPreferences>
        get() = combine(playbackSpeed, onlinePlaybackQuality) { speed, quality ->
            PlaybackPreferences(playbackSpeed = speed, onlinePlaybackQuality = quality)
        }
    val playbackSpeed: Flow<PlaybackSpeed>
    val onlinePlaybackQuality: Flow<OnlinePlaybackQuality>

    suspend fun setPlaybackSpeed(speed: PlaybackSpeed)
    suspend fun setOnlinePlaybackQuality(quality: OnlinePlaybackQuality)
    suspend fun setPlaybackMode(mode: PlaybackMode) = Unit
    suspend fun setGaplessEnabled(enabled: Boolean) = Unit
    suspend fun setCrossfadeDuration(duration: CrossfadeDuration) = Unit
    suspend fun setLoudnessNormalizationEnabled(enabled: Boolean) = Unit
    suspend fun setAudioFocusPolicy(policy: AudioFocusPolicy) = Unit
    suspend fun setEqualizerEnabled(enabled: Boolean) = Unit
    suspend fun setEqualizerGains(lowDb: Int, midDb: Int, highDb: Int) = Unit
    suspend fun setEqualizerPreset(preset: EqualizerPreset) {
        if (preset == EqualizerPreset.Custom) {
            setEqualizerEnabled(true)
            return
        }
        setEqualizerEnabled(preset.enabled)
        setEqualizerGains(preset.lowDb, preset.midDb, preset.highDb)
    }
    suspend fun reset() = Unit
}

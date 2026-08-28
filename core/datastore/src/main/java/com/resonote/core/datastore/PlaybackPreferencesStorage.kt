package com.resonote.core.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface PlaybackPreferencesStorage {
    val playbackSpeedPercent: Flow<Int>
    val onlinePlaybackQuality: Flow<String>
    val playbackMode: Flow<String> get() = flowOf("")
    val gaplessEnabled: Flow<Boolean> get() = flowOf(true)
    val crossfadeDuration: Flow<String> get() = flowOf("")
    val loudnessNormalizationEnabled: Flow<Boolean> get() = flowOf(false)
    val audioFocusPolicy: Flow<String> get() = flowOf("")
    val equalizerEnabled: Flow<Boolean> get() = flowOf(false)
    val equalizerLowDb: Flow<Int> get() = flowOf(0)
    val equalizerMidDb: Flow<Int> get() = flowOf(0)
    val equalizerHighDb: Flow<Int> get() = flowOf(0)
    val equalizerCustom: Flow<Boolean> get() = flowOf(false)

    suspend fun setPlaybackSpeedPercent(percent: Int)
    suspend fun setOnlinePlaybackQuality(quality: String)
    suspend fun setPlaybackMode(mode: String) = Unit
    suspend fun setGaplessEnabled(enabled: Boolean) = Unit
    suspend fun setCrossfadeDuration(duration: String) = Unit
    suspend fun setLoudnessNormalizationEnabled(enabled: Boolean) = Unit
    suspend fun setAudioFocusPolicy(policy: String) = Unit
    suspend fun setEqualizerEnabled(enabled: Boolean) = Unit
    suspend fun setEqualizerGains(lowDb: Int, midDb: Int, highDb: Int) = Unit
    suspend fun setEqualizerPreset(enabled: Boolean, lowDb: Int, midDb: Int, highDb: Int) {
        setEqualizerGains(lowDb, midDb, highDb)
        setEqualizerEnabled(enabled)
    }
    suspend fun reset() = Unit
}

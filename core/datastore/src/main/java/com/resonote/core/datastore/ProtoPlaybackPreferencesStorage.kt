package com.resonote.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import com.resonote.core.datastore.proto.PlaybackPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

object PlaybackPreferencesSerializer : Serializer<PlaybackPreferences> {
    override val defaultValue: PlaybackPreferences = PlaybackPreferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): PlaybackPreferences = PlaybackPreferences.parseFrom(input)

    override suspend fun writeTo(t: PlaybackPreferences, output: OutputStream) = t.writeTo(output)
}

@Singleton
internal class ProtoPlaybackPreferencesStorage @Inject constructor(private val store: DataStore<PlaybackPreferences>) :
    PlaybackPreferencesStorage {
    override val playbackSpeedPercent: Flow<Int> = store.data.map { it.playbackSpeedPercent }
    override val onlinePlaybackQuality: Flow<String> = store.data.map { it.onlinePlaybackQuality }
    override val playbackMode: Flow<String> = store.data.map { it.playbackMode }
    override val gaplessEnabled: Flow<Boolean> = store.data.map {
        if (it.gaplessConfigured) it.gaplessEnabled else true
    }
    override val crossfadeDuration: Flow<String> = store.data.map { it.crossfadeDuration }
    override val loudnessNormalizationEnabled: Flow<Boolean> = store.data.map { it.loudnessNormalizationEnabled }
    override val audioFocusPolicy: Flow<String> = store.data.map { it.audioFocusPolicy }
    override val equalizerEnabled: Flow<Boolean> = store.data.map { it.equalizerEnabled }
    override val equalizerLowDb: Flow<Int> = store.data.map { it.equalizerLowDb }
    override val equalizerMidDb: Flow<Int> = store.data.map { it.equalizerMidDb }
    override val equalizerHighDb: Flow<Int> = store.data.map { it.equalizerHighDb }
    override val equalizerCustom: Flow<Boolean> = store.data.map { it.equalizerCustom }

    override suspend fun setPlaybackSpeedPercent(percent: Int) {
        require(percent > 0) { "playback speed percent must be positive" }
        store.updateData { it.copy(playbackSpeedPercent = percent) }
    }

    override suspend fun setOnlinePlaybackQuality(quality: String) {
        require(quality.isNotBlank()) { "online playback quality must not be blank" }
        store.updateData { it.copy(onlinePlaybackQuality = quality) }
    }

    override suspend fun setPlaybackMode(mode: String) {
        require(mode.isNotBlank()) { "playback mode must not be blank" }
        store.updateData { it.copy(playbackMode = mode) }
    }

    override suspend fun setGaplessEnabled(enabled: Boolean) {
        store.updateData { it.copy(gaplessEnabled = enabled, gaplessConfigured = true) }
    }

    override suspend fun setCrossfadeDuration(duration: String) {
        require(duration.isNotBlank()) { "crossfade duration must not be blank" }
        store.updateData { it.copy(crossfadeDuration = duration) }
    }

    override suspend fun setLoudnessNormalizationEnabled(enabled: Boolean) {
        store.updateData { it.copy(loudnessNormalizationEnabled = enabled) }
    }

    override suspend fun setAudioFocusPolicy(policy: String) {
        require(policy.isNotBlank()) { "audio focus policy must not be blank" }
        store.updateData { it.copy(audioFocusPolicy = policy) }
    }

    override suspend fun setEqualizerEnabled(enabled: Boolean) {
        store.updateData { it.copy(equalizerEnabled = enabled, equalizerCustom = enabled && it.equalizerCustom) }
    }

    override suspend fun setEqualizerGains(lowDb: Int, midDb: Int, highDb: Int) {
        require(lowDb in -12..12 && midDb in -12..12 && highDb in -12..12) {
            "equalizer gains must be between -12 dB and 12 dB"
        }
        store.updateData {
            it.copy(
                equalizerEnabled = true,
                equalizerLowDb = lowDb,
                equalizerMidDb = midDb,
                equalizerHighDb = highDb,
                equalizerCustom = true,
            )
        }
    }

    override suspend fun setEqualizerPreset(enabled: Boolean, lowDb: Int, midDb: Int, highDb: Int) {
        require(lowDb in -12..12 && midDb in -12..12 && highDb in -12..12) {
            "equalizer gains must be between -12 dB and 12 dB"
        }
        store.updateData {
            it.copy(
                equalizerEnabled = enabled,
                equalizerLowDb = lowDb,
                equalizerMidDb = midDb,
                equalizerHighDb = highDb,
                equalizerCustom = false,
            )
        }
    }

    override suspend fun reset() {
        store.updateData { com.resonote.core.datastore.proto.PlaybackPreferences.getDefaultInstance() }
    }
}

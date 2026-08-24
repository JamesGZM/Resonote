package com.resonote.core.data

import com.resonote.core.datastore.PlaybackPreferencesStorage
import com.resonote.core.model.AudioFocusPolicy
import com.resonote.core.model.CrossfadeDuration
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.PlaybackMode
import com.resonote.core.model.PlaybackPreferences
import com.resonote.core.model.PlaybackSpeed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultPlaybackPreferencesRepository @Inject constructor(
    private val storage: PlaybackPreferencesStorage,
) : PlaybackPreferencesRepository {
    override val playbackSpeed = storage.playbackSpeedPercent.map(PlaybackSpeed::fromPercent)
    override val onlinePlaybackQuality = storage.onlinePlaybackQuality.map { stored ->
        OnlinePlaybackQuality.entries.firstOrNull { it.name == stored } ?: OnlinePlaybackQuality.Standard
    }
    private val playbackMode = storage.playbackMode.map { stored ->
        PlaybackMode.entries.firstOrNull { it.name == stored } ?: PlaybackMode.ListLoop
    }
    private val crossfadeDuration = storage.crossfadeDuration.map { stored ->
        CrossfadeDuration.entries.firstOrNull { it.name == stored } ?: CrossfadeDuration.Off
    }
    private val audioFocusPolicy = storage.audioFocusPolicy.map { stored ->
        AudioFocusPolicy.entries.firstOrNull { it.name == stored } ?: AudioFocusPolicy.Disallow
    }
    override val preferences = combine(playbackSpeed, onlinePlaybackQuality) { speed, quality ->
        PlaybackPreferences(playbackSpeed = speed, onlinePlaybackQuality = quality)
    }.combine(playbackMode) { preferences, mode ->
        preferences.copy(playbackMode = mode)
    }.combine(storage.gaplessEnabled) { preferences, enabled ->
        preferences.copy(gaplessEnabled = enabled)
    }.combine(crossfadeDuration) { preferences, duration ->
        preferences.copy(crossfadeDuration = duration)
    }.combine(storage.loudnessNormalizationEnabled) { preferences, enabled ->
        preferences.copy(loudnessNormalizationEnabled = enabled)
    }.combine(audioFocusPolicy) { preferences, policy ->
        preferences.copy(audioFocusPolicy = policy)
    }

    override suspend fun setPlaybackSpeed(speed: PlaybackSpeed) {
        storage.setPlaybackSpeedPercent(speed.percent)
    }

    override suspend fun setOnlinePlaybackQuality(quality: OnlinePlaybackQuality) {
        storage.setOnlinePlaybackQuality(quality.name)
    }

    override suspend fun setPlaybackMode(mode: PlaybackMode) = storage.setPlaybackMode(mode.name)

    override suspend fun setGaplessEnabled(enabled: Boolean) = storage.setGaplessEnabled(enabled)

    override suspend fun setCrossfadeDuration(duration: CrossfadeDuration) = storage.setCrossfadeDuration(duration.name)

    override suspend fun setLoudnessNormalizationEnabled(enabled: Boolean) =
        storage.setLoudnessNormalizationEnabled(enabled)

    override suspend fun setAudioFocusPolicy(policy: AudioFocusPolicy) = storage.setAudioFocusPolicy(policy.name)

    override suspend fun reset() = storage.reset()
}

package com.resonote.core.data

import com.google.common.truth.Truth.assertThat
import com.resonote.core.datastore.PlaybackPreferencesStorage
import com.resonote.core.model.AudioFocusPolicy
import com.resonote.core.model.CrossfadeDuration
import com.resonote.core.model.EqualizerPreset
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.PlaybackMode
import com.resonote.core.model.PlaybackSpeed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultPlaybackPreferencesRepositoryTest {
    @Test
    fun unsetAndUnknownStoredValuesFallBackToNormalSpeed() = runTest {
        val storage = FakePlaybackPreferencesStorage(0)
        val repository = DefaultPlaybackPreferencesRepository(storage)

        assertThat(repository.playbackSpeed.first()).isEqualTo(PlaybackSpeed.Normal)

        storage.value.value = 133
        assertThat(repository.playbackSpeed.first()).isEqualTo(PlaybackSpeed.Normal)
        assertThat(repository.onlinePlaybackQuality.first()).isEqualTo(OnlinePlaybackQuality.Standard)
    }

    @Test
    fun onlineQualityUsesStablePersistedName() = runTest {
        val storage = FakePlaybackPreferencesStorage(100)
        val repository = DefaultPlaybackPreferencesRepository(storage)

        repository.setOnlinePlaybackQuality(OnlinePlaybackQuality.ViperClear)

        assertThat(storage.quality.value).isEqualTo("ViperClear")
        assertThat(repository.onlinePlaybackQuality.first()).isEqualTo(OnlinePlaybackQuality.ViperClear)
    }

    @Test
    fun supportedSpeedUsesStablePersistedPercent() = runTest {
        val storage = FakePlaybackPreferencesStorage(100)
        val repository = DefaultPlaybackPreferencesRepository(storage)

        repository.setPlaybackSpeed(PlaybackSpeed.ThreeQuarters)

        assertThat(storage.value.value).isEqualTo(75)
        assertThat(repository.playbackSpeed.first()).isEqualTo(PlaybackSpeed.ThreeQuarters)
    }

    @Test
    fun extendedPlaybackPreferencesUseStablePersistedValues() = runTest {
        val storage = FakePlaybackPreferencesStorage(100)
        val repository = DefaultPlaybackPreferencesRepository(storage)

        repository.setPlaybackMode(PlaybackMode.Shuffle)
        repository.setGaplessEnabled(false)
        repository.setCrossfadeDuration(CrossfadeDuration.FiveSeconds)
        repository.setLoudnessNormalizationEnabled(true)
        repository.setAudioFocusPolicy(AudioFocusPolicy.AllowMedia)
        repository.setEqualizerEnabled(true)
        repository.setEqualizerGains(6, -3, 9)

        assertThat(repository.preferences.first()).isEqualTo(
            com.resonote.core.model.PlaybackPreferences(
                playbackMode = PlaybackMode.Shuffle,
                gaplessEnabled = false,
                crossfadeDuration = CrossfadeDuration.FiveSeconds,
                loudnessNormalizationEnabled = true,
                audioFocusPolicy = AudioFocusPolicy.AllowMedia,
                equalizerEnabled = true,
                equalizerLowDb = 6,
                equalizerMidDb = -3,
                equalizerHighDb = 9,
                equalizerCustom = true,
            ),
        )
    }

    @Test
    fun presetAndManualGainsKeepDistinctEqualizerModes() = runTest {
        val storage = FakePlaybackPreferencesStorage(100)
        val repository = DefaultPlaybackPreferencesRepository(storage)

        repository.setEqualizerPreset(EqualizerPreset.Rock)
        assertThat(repository.preferences.first().equalizerPreset).isEqualTo(EqualizerPreset.Rock)

        repository.setEqualizerGains(5, -2, 4)
        assertThat(repository.preferences.first().equalizerPreset).isEqualTo(EqualizerPreset.Custom)
    }

    private class FakePlaybackPreferencesStorage(initial: Int) : PlaybackPreferencesStorage {
        val value = MutableStateFlow(initial)
        val quality = MutableStateFlow("")
        val mode = MutableStateFlow("")
        val gapless = MutableStateFlow(true)
        val crossfade = MutableStateFlow("")
        val loudness = MutableStateFlow(false)
        val audioFocus = MutableStateFlow("")
        val eqEnabled = MutableStateFlow(false)
        val eqLow = MutableStateFlow(0)
        val eqMid = MutableStateFlow(0)
        val eqHigh = MutableStateFlow(0)
        val eqCustom = MutableStateFlow(false)
        override val playbackSpeedPercent = value
        override val onlinePlaybackQuality = quality
        override val playbackMode = mode
        override val gaplessEnabled = gapless
        override val crossfadeDuration = crossfade
        override val loudnessNormalizationEnabled = loudness
        override val audioFocusPolicy = audioFocus
        override val equalizerEnabled = eqEnabled
        override val equalizerLowDb = eqLow
        override val equalizerMidDb = eqMid
        override val equalizerHighDb = eqHigh
        override val equalizerCustom = eqCustom

        override suspend fun setPlaybackSpeedPercent(percent: Int) {
            value.value = percent
        }

        override suspend fun setOnlinePlaybackQuality(quality: String) {
            this.quality.value = quality
        }

        override suspend fun setPlaybackMode(mode: String) {
            this.mode.value = mode
        }

        override suspend fun setGaplessEnabled(enabled: Boolean) {
            gapless.value = enabled
        }

        override suspend fun setCrossfadeDuration(duration: String) {
            crossfade.value = duration
        }

        override suspend fun setLoudnessNormalizationEnabled(enabled: Boolean) {
            loudness.value = enabled
        }

        override suspend fun setAudioFocusPolicy(policy: String) {
            audioFocus.value = policy
        }

        override suspend fun setEqualizerEnabled(enabled: Boolean) {
            eqEnabled.value = enabled
        }

        override suspend fun setEqualizerGains(lowDb: Int, midDb: Int, highDb: Int) {
            eqLow.value = lowDb
            eqMid.value = midDb
            eqHigh.value = highDb
            eqEnabled.value = true
            eqCustom.value = true
        }

        override suspend fun setEqualizerPreset(enabled: Boolean, lowDb: Int, midDb: Int, highDb: Int) {
            eqEnabled.value = enabled
            eqLow.value = lowDb
            eqMid.value = midDb
            eqHigh.value = highDb
            eqCustom.value = false
        }
    }
}

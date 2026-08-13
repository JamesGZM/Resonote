package com.resonote.core.data

import com.google.common.truth.Truth.assertThat
import com.resonote.core.datastore.PlaybackPreferencesStorage
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.model.OnlinePlaybackQuality
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

    private class FakePlaybackPreferencesStorage(initial: Int) : PlaybackPreferencesStorage {
        val value = MutableStateFlow(initial)
        val quality = MutableStateFlow("")
        override val playbackSpeedPercent = value
        override val onlinePlaybackQuality = quality

        override suspend fun setPlaybackSpeedPercent(percent: Int) {
            value.value = percent
        }

        override suspend fun setOnlinePlaybackQuality(quality: String) {
            this.quality.value = quality
        }
    }
}

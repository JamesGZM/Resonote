package com.resonote.core.data

import com.google.common.truth.Truth.assertThat
import com.resonote.core.datastore.PlaybackPreferencesStorage
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
        override val playbackSpeedPercent = value

        override suspend fun setPlaybackSpeedPercent(percent: Int) {
            value.value = percent
        }
    }
}

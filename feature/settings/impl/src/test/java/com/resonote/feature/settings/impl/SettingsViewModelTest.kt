package com.resonote.feature.settings.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.PlaybackPreferencesRepository
import com.resonote.core.model.PlaybackSpeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun repositoryPreferenceIsTheDisplayedFactSource() = runTest(dispatcher) {
        val repository = FakePlaybackPreferencesRepository(PlaybackSpeed.ThreeQuarters)
        val viewModel = SettingsViewModel(repository)

        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            SettingsUiState.Ready(PlaybackSpeed.ThreeQuarters),
        )

        repository.speed.value = PlaybackSpeed.Double
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(SettingsUiState.Ready(PlaybackSpeed.Double))
    }

    @Test
    fun selectingSpeedPersistsThroughTheSharedRepository() = runTest(dispatcher) {
        val repository = FakePlaybackPreferencesRepository(PlaybackSpeed.Normal)
        val viewModel = SettingsViewModel(repository)
        advanceUntilIdle()

        viewModel.setPlaybackSpeed(PlaybackSpeed.OneAndHalf)
        advanceUntilIdle()

        assertThat(repository.writes).containsExactly(PlaybackSpeed.OneAndHalf)
        assertThat(viewModel.uiState.value).isEqualTo(SettingsUiState.Ready(PlaybackSpeed.OneAndHalf))
    }

    @Test
    fun loadAndSaveFailuresRemainExplicitAndRecoverable() = runTest(dispatcher) {
        val failedLoad = object : PlaybackPreferencesRepository {
            override val playbackSpeed: Flow<PlaybackSpeed> = flow { error("broken read") }
            override suspend fun setPlaybackSpeed(speed: PlaybackSpeed) = Unit
        }
        val loadingViewModel = SettingsViewModel(failedLoad)
        advanceUntilIdle()
        assertThat(loadingViewModel.uiState.value).isEqualTo(SettingsUiState.LoadFailed)

        val failedSave = FakePlaybackPreferencesRepository(PlaybackSpeed.Normal, failWrites = true)
        val savingViewModel = SettingsViewModel(failedSave)
        advanceUntilIdle()
        savingViewModel.setPlaybackSpeed(PlaybackSpeed.Double)
        advanceUntilIdle()

        assertThat((savingViewModel.uiState.value as SettingsUiState.Ready).saveFailed).isTrue()
        savingViewModel.acknowledgeSaveFailure()
        assertThat((savingViewModel.uiState.value as SettingsUiState.Ready).saveFailed).isFalse()
    }

    private class FakePlaybackPreferencesRepository(
        initialSpeed: PlaybackSpeed,
        private val failWrites: Boolean = false,
    ) : PlaybackPreferencesRepository {
        val speed = MutableStateFlow(initialSpeed)
        val writes = mutableListOf<PlaybackSpeed>()
        override val playbackSpeed: Flow<PlaybackSpeed> = speed

        override suspend fun setPlaybackSpeed(speed: PlaybackSpeed) {
            writes += speed
            if (failWrites) error("broken write")
            this.speed.value = speed
        }
    }
}

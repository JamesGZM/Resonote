package com.resonote.feature.settings.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.LyricsPreferencesRepository
import com.resonote.core.model.DesktopLyricsControlsTimeout
import com.resonote.core.model.LyricsPreferences
import com.resonote.core.playback.DesktopLyricsController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LyricsSettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun rapidSupplementalChangesPersistIndependently() = runTest(dispatcher) {
        val repository = FakeLyricsPreferencesRepository()
        val viewModel = LyricsSettingsViewModel(repository)

        viewModel.setTranslationEnabled(false)
        viewModel.setTransliterationEnabled(false)
        advanceUntilIdle()

        assertThat(repository.preferences.value).isEqualTo(
            LyricsPreferences(translationEnabled = false, transliterationEnabled = false),
        )
    }

    @Test
    fun enablingDesktopLyricsPersistsBeforeShowingService() = runTest(dispatcher) {
        val repository = FakeLyricsPreferencesRepository()
        val controller = FakeDesktopLyricsController(repository)
        val viewModel = LyricsSettingsViewModel(repository, controller)

        viewModel.setDesktopLyricsEnabled(true)
        advanceUntilIdle()

        assertThat(repository.preferences.value.desktopLyricsEnabled).isTrue()
        assertThat(controller.enabledWhenShown).isTrue()
    }

    @Test
    fun resettingDesktopLyricsPositionPersistsAndNotifiesService() = runTest(dispatcher) {
        val repository = FakeLyricsPreferencesRepository()
        val controller = FakeDesktopLyricsController(repository)
        val viewModel = LyricsSettingsViewModel(repository, controller)

        viewModel.resetDesktopLyricsPosition()
        advanceUntilIdle()

        assertThat(repository.preferences.value.desktopLyricsPosition).isNull()
        assertThat(controller.resetCalls).isEqualTo(1)
    }

    @Test
    fun changingControllerTimeoutPersistsAndRefreshesService() = runTest(dispatcher) {
        val repository = FakeLyricsPreferencesRepository()
        val controller = FakeDesktopLyricsController(repository)
        val viewModel = LyricsSettingsViewModel(repository, controller)

        viewModel.setDesktopLyricsControlsTimeout(DesktopLyricsControlsTimeout.EightSeconds)
        advanceUntilIdle()

        assertThat(repository.preferences.value.desktopLyricsControlsTimeout)
            .isEqualTo(DesktopLyricsControlsTimeout.EightSeconds)
        assertThat(controller.refreshCalls).isEqualTo(1)
    }

    @Test
    fun changingSurfaceOpacityPersistsAndRefreshesService() = runTest(dispatcher) {
        val repository = FakeLyricsPreferencesRepository()
        val controller = FakeDesktopLyricsController(repository)
        val viewModel = LyricsSettingsViewModel(repository, controller)

        viewModel.setDesktopLyricsSurfaceOpacity(70)
        advanceUntilIdle()

        assertThat(repository.preferences.value.desktopLyricsSurfaceOpacity).isEqualTo(70)
        assertThat(controller.refreshCalls).isEqualTo(1)
    }
}

private class FakeDesktopLyricsController(private val repository: FakeLyricsPreferencesRepository) :
    DesktopLyricsController {
    var enabledWhenShown = false
    var resetCalls = 0
    var refreshCalls = 0

    override fun show() {
        enabledWhenShown = repository.preferences.value.desktopLyricsEnabled
    }

    override fun hide() = Unit

    override fun refresh() {
        refreshCalls++
    }

    override fun resetPosition() {
        resetCalls++
    }

    override fun setAppForeground(foreground: Boolean) = Unit
}

private class FakeLyricsPreferencesRepository : LyricsPreferencesRepository {
    override val preferences = MutableStateFlow(LyricsPreferences())

    override suspend fun setPreferences(value: LyricsPreferences) {
        preferences.value = value
    }

    override suspend fun reset() {
        preferences.value = LyricsPreferences()
    }
}

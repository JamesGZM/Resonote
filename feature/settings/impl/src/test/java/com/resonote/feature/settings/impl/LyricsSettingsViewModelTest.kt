package com.resonote.feature.settings.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.LyricsPreferencesRepository
import com.resonote.core.model.DesktopLyricsControlsTimeout
import com.resonote.core.model.DesktopLyricsDefaults
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
    fun changingShadowColorPersistsOpaqueColorAndRefreshesService() = runTest(dispatcher) {
        val repository = FakeLyricsPreferencesRepository()
        val controller = FakeDesktopLyricsController(repository)
        val viewModel = LyricsSettingsViewModel(repository, controller)

        viewModel.setDesktopLyricsShadowColor(0x00245B83)
        advanceUntilIdle()

        assertThat(repository.preferences.value.desktopLyricsShadowColorArgb).isEqualTo(0xFF245B83.toInt())
        assertThat(controller.refreshCalls).isEqualTo(1)
    }

    @Test
    fun changingLyricsBackgroundColorPersistsOpaqueColorAndRefreshesService() = runTest(dispatcher) {
        val repository = FakeLyricsPreferencesRepository()
        val controller = FakeDesktopLyricsController(repository)
        val viewModel = LyricsSettingsViewModel(repository, controller)

        viewModel.setDesktopLyricsBackgroundColor(0x00245B83)
        advanceUntilIdle()

        assertThat(repository.preferences.value.desktopLyricsBackgroundColorArgb).isEqualTo(0xFF245B83.toInt())
        assertThat(controller.refreshCalls).isEqualTo(1)
    }

    @Test
    fun restoringDesktopLyricsStyleResetsEveryGroupAndRefreshesService() = runTest(dispatcher) {
        val repository = FakeLyricsPreferencesRepository(
            LyricsPreferences(
                desktopLyricsBackgroundColorArgb = 0xFF123456.toInt(),
                desktopLyricsForegroundColorArgb = 0xFF654321.toInt(),
                desktopLyricsShadowColorArgb = 0xFF112233.toInt(),
                desktopLyricsShadowOffsetXDp = -4f,
                desktopLyricsShadowOffsetYDp = 5f,
                desktopLyricsShadowBlurRadiusDp = 8f,
                desktopLyricsWidthPercent = 72,
                desktopLyricsFontSizeSp = 31,
                desktopLyricsOutlineColorArgb = 0xFF334455.toInt(),
                desktopLyricsOutlineWidthDp = 2.4f,
                desktopLyricsControlsTimeout = DesktopLyricsControlsTimeout.EightSeconds,
            ),
        )
        val controller = FakeDesktopLyricsController(repository)
        val viewModel = LyricsSettingsViewModel(repository, controller)

        viewModel.resetDesktopLyricsBackgroundColor()
        viewModel.resetDesktopLyricsForegroundColor()
        viewModel.resetDesktopLyricsWidth()
        viewModel.resetDesktopLyricsFontSize()
        viewModel.resetDesktopLyricsOutline()
        viewModel.resetDesktopLyricsShadow()
        viewModel.resetDesktopLyricsControlsTimeout()
        advanceUntilIdle()

        val preferences = repository.preferences.value
        assertThat(preferences.desktopLyricsBackgroundColorArgb)
            .isEqualTo(DesktopLyricsDefaults.BACKGROUND_COLOR_ARGB)
        assertThat(preferences.desktopLyricsForegroundColorArgb)
            .isEqualTo(DesktopLyricsDefaults.FOREGROUND_COLOR_ARGB)
        assertThat(preferences.desktopLyricsWidthPercent).isEqualTo(DesktopLyricsDefaults.WIDTH_PERCENT)
        assertThat(preferences.desktopLyricsFontSizeSp).isEqualTo(DesktopLyricsDefaults.FONT_SIZE_SP)
        assertThat(preferences.desktopLyricsOutlineColorArgb)
            .isEqualTo(DesktopLyricsDefaults.OUTLINE_COLOR_ARGB)
        assertThat(preferences.desktopLyricsOutlineWidthDp).isEqualTo(DesktopLyricsDefaults.OUTLINE_WIDTH_DP)
        assertThat(preferences.desktopLyricsShadowColorArgb)
            .isEqualTo(DesktopLyricsDefaults.SHADOW_COLOR_ARGB)
        assertThat(preferences.desktopLyricsShadowOffsetXDp)
            .isEqualTo(DesktopLyricsDefaults.SHADOW_OFFSET_X_DP)
        assertThat(preferences.desktopLyricsShadowOffsetYDp)
            .isEqualTo(DesktopLyricsDefaults.SHADOW_OFFSET_Y_DP)
        assertThat(preferences.desktopLyricsShadowBlurRadiusDp)
            .isEqualTo(DesktopLyricsDefaults.SHADOW_BLUR_RADIUS_DP)
        assertThat(preferences.desktopLyricsControlsTimeout)
            .isEqualTo(DesktopLyricsControlsTimeout.FiveSeconds)
        assertThat(controller.refreshCalls).isEqualTo(7)
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
}

private class FakeLyricsPreferencesRepository(initial: LyricsPreferences = LyricsPreferences()) :
    LyricsPreferencesRepository {
    override val preferences = MutableStateFlow(initial)

    override suspend fun setPreferences(value: LyricsPreferences) {
        preferences.value = value
    }

    override suspend fun reset() {
        preferences.value = LyricsPreferences()
    }
}

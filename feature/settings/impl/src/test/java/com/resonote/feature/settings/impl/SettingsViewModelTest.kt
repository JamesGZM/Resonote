package com.resonote.feature.settings.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.AuthRepository
import com.resonote.core.data.PlaybackPreferencesRepository
import com.resonote.core.data.ThemePreferencesRepository
import com.resonote.core.model.AuthState
import com.resonote.core.model.MobileCodeLoginResult
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.PasswordLoginResult
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.model.QrLoginCheckResult
import com.resonote.core.model.QrLoginKeyResult
import com.resonote.core.model.SendMobileCodeResult
import com.resonote.core.model.ThemeMode
import com.resonote.core.model.ThemePreferences
import com.resonote.core.playback.PlaybackCacheController
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
        val viewModel = SettingsViewModel(
            repository,
            FakeThemePreferencesRepository(),
            FakePlaybackCacheController(),
            FakeAuthRepository(),
        )

        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            SettingsUiState.Ready(PlaybackSpeed.ThreeQuarters, cacheBytes = 0),
        )

        repository.speed.value = PlaybackSpeed.Double
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(SettingsUiState.Ready(PlaybackSpeed.Double, cacheBytes = 0))
    }

    @Test
    fun selectingSpeedPersistsThroughTheSharedRepository() = runTest(dispatcher) {
        val repository = FakePlaybackPreferencesRepository(PlaybackSpeed.Normal)
        val viewModel = SettingsViewModel(
            repository,
            FakeThemePreferencesRepository(),
            FakePlaybackCacheController(),
            FakeAuthRepository(),
        )
        advanceUntilIdle()

        viewModel.setPlaybackSpeed(PlaybackSpeed.OneAndHalf)
        advanceUntilIdle()

        assertThat(repository.writes).containsExactly(PlaybackSpeed.OneAndHalf)
        assertThat(viewModel.uiState.value).isEqualTo(SettingsUiState.Ready(PlaybackSpeed.OneAndHalf, cacheBytes = 0))
    }

    @Test
    fun selectingOnlineQualityPersistsThroughTheSharedRepository() = runTest(dispatcher) {
        val repository = FakePlaybackPreferencesRepository(PlaybackSpeed.Normal)
        val viewModel = SettingsViewModel(
            repository,
            FakeThemePreferencesRepository(),
            FakePlaybackCacheController(),
            FakeAuthRepository(),
        )
        advanceUntilIdle()

        viewModel.setOnlinePlaybackQuality(OnlinePlaybackQuality.Lossless)
        advanceUntilIdle()

        assertThat(repository.qualityWrites).containsExactly(OnlinePlaybackQuality.Lossless)
        assertThat((viewModel.uiState.value as SettingsUiState.Ready).onlinePlaybackQuality)
            .isEqualTo(OnlinePlaybackQuality.Lossless)
    }

    @Test
    fun appearanceChangesPersistThroughThemeRepository() = runTest(dispatcher) {
        val themeRepository = FakeThemePreferencesRepository()
        val viewModel = SettingsViewModel(
            FakePlaybackPreferencesRepository(PlaybackSpeed.Normal),
            themeRepository,
            FakePlaybackCacheController(),
            FakeAuthRepository(),
        )
        advanceUntilIdle()

        viewModel.setThemeMode(ThemeMode.AMOLED)
        advanceUntilIdle()
        assertThat(themeRepository.preferences.value).isEqualTo(ThemePreferences(ThemeMode.AMOLED, false))

        viewModel.setDynamicColorEnabled(true)
        advanceUntilIdle()
        assertThat(themeRepository.preferences.value).isEqualTo(ThemePreferences(ThemeMode.SYSTEM, true))
    }

    @Test
    fun loadAndSaveFailuresRemainExplicitAndRecoverable() = runTest(dispatcher) {
        val failedLoad = object : PlaybackPreferencesRepository {
            override val playbackSpeed: Flow<PlaybackSpeed> = flow { error("broken read") }
            override val onlinePlaybackQuality: Flow<OnlinePlaybackQuality> = flow { error("broken read") }
            override suspend fun setPlaybackSpeed(speed: PlaybackSpeed) = Unit
            override suspend fun setOnlinePlaybackQuality(quality: OnlinePlaybackQuality) = Unit
        }
        val loadingViewModel =
            SettingsViewModel(
                failedLoad,
                FakeThemePreferencesRepository(),
                FakePlaybackCacheController(),
                FakeAuthRepository(),
            )
        advanceUntilIdle()
        assertThat(loadingViewModel.uiState.value).isEqualTo(SettingsUiState.LoadFailed)

        val failedSave = FakePlaybackPreferencesRepository(PlaybackSpeed.Normal, failWrites = true)
        val savingViewModel =
            SettingsViewModel(
                failedSave,
                FakeThemePreferencesRepository(),
                FakePlaybackCacheController(),
                FakeAuthRepository(),
            )
        advanceUntilIdle()
        savingViewModel.setPlaybackSpeed(PlaybackSpeed.Double)
        advanceUntilIdle()

        assertThat((savingViewModel.uiState.value as SettingsUiState.Ready).saveFailed).isTrue()
        savingViewModel.acknowledgeSaveFailure()
        assertThat((savingViewModel.uiState.value as SettingsUiState.Ready).saveFailed).isFalse()
    }

    @Test
    fun logoutClearsTheAuthenticatedState() = runTest(dispatcher) {
        val authRepository = FakeAuthRepository(AuthState.Authenticated("42"))
        val viewModel = SettingsViewModel(
            FakePlaybackPreferencesRepository(PlaybackSpeed.Normal),
            FakeThemePreferencesRepository(),
            FakePlaybackCacheController(),
            authRepository,
        )
        advanceUntilIdle()
        assertThat((viewModel.uiState.value as SettingsUiState.Ready).isAuthenticated).isTrue()

        viewModel.logout()
        advanceUntilIdle()

        assertThat(authRepository.logoutCalls).isEqualTo(1)
        assertThat((viewModel.uiState.value as SettingsUiState.Ready).isAuthenticated).isFalse()
    }

    private class FakePlaybackPreferencesRepository(
        initialSpeed: PlaybackSpeed,
        private val failWrites: Boolean = false,
    ) : PlaybackPreferencesRepository {
        val speed = MutableStateFlow(initialSpeed)
        val quality = MutableStateFlow(OnlinePlaybackQuality.Standard)
        val writes = mutableListOf<PlaybackSpeed>()
        val qualityWrites = mutableListOf<OnlinePlaybackQuality>()
        override val playbackSpeed: Flow<PlaybackSpeed> = speed
        override val onlinePlaybackQuality: Flow<OnlinePlaybackQuality> = quality

        override suspend fun setPlaybackSpeed(speed: PlaybackSpeed) {
            writes += speed
            if (failWrites) error("broken write")
            this.speed.value = speed
        }

        override suspend fun setOnlinePlaybackQuality(quality: OnlinePlaybackQuality) {
            qualityWrites += quality
            if (failWrites) error("broken write")
            this.quality.value = quality
        }
    }

    private class FakeThemePreferencesRepository : ThemePreferencesRepository {
        val preferences = MutableStateFlow(ThemePreferences())
        override val themePreferences: Flow<ThemePreferences> = preferences

        override suspend fun setThemeMode(themeMode: ThemeMode) {
            preferences.value = preferences.value.copy(
                themeMode = themeMode,
                dynamicColorEnabled = preferences.value.dynamicColorEnabled && themeMode != ThemeMode.AMOLED,
            )
        }

        override suspend fun setDynamicColorEnabled(enabled: Boolean) {
            preferences.value = ThemePreferences(
                themeMode = if (enabled && preferences.value.themeMode == ThemeMode.AMOLED) {
                    ThemeMode.SYSTEM
                } else {
                    preferences.value.themeMode
                },
                dynamicColorEnabled = enabled,
            )
        }
    }

    private class FakeAuthRepository(initial: AuthState = AuthState.Anonymous) : AuthRepository {
        private val state = MutableStateFlow(initial)
        override val authState: Flow<AuthState> = state
        var logoutCalls = 0

        override suspend fun acknowledgeAuthenticationGate() = Unit

        override suspend fun logout() {
            logoutCalls += 1
            state.value = AuthState.Anonymous
        }

        override suspend fun sendMobileCode(mobile: String): SendMobileCodeResult = error("unused")
        override suspend fun loginWithMobileCode(
            mobile: String,
            code: String,
            selectedUserId: String?,
        ): MobileCodeLoginResult = error("unused")
        override suspend fun loginWithPassword(username: String, password: String): PasswordLoginResult =
            error("unused")
        override suspend fun createQrLoginKey(): QrLoginKeyResult = error("unused")
        override suspend fun checkQrLogin(key: String): QrLoginCheckResult = error("unused")
    }

    private class FakePlaybackCacheController : PlaybackCacheController {
        override suspend fun sizeBytes(): Long = 0

        override suspend fun clear(): Long = 0
    }
}

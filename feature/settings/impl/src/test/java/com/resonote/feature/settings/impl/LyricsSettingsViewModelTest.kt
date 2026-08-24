package com.resonote.feature.settings.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.LyricsPreferencesRepository
import com.resonote.core.model.LyricsPreferences
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

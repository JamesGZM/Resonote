package com.resonote.core.data

import com.google.common.truth.Truth.assertThat
import com.resonote.core.datastore.AppearancePreferencesStorage
import com.resonote.core.datastore.StoredAppearancePreferences
import com.resonote.core.model.ThemeMode
import com.resonote.core.model.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultThemePreferencesRepositoryTest {
    @Test
    fun emptyAndUnknownModesFallBackToSystemBrand() = runTest {
        val storage = FakeAppearancePreferencesStorage(StoredAppearancePreferences("unknown", false))
        val repository = DefaultThemePreferencesRepository(storage)

        assertThat(repository.themePreferences.first()).isEqualTo(ThemePreferences())
    }

    @Test
    fun selectingAmoledDisablesDynamicColor() = runTest {
        val storage = FakeAppearancePreferencesStorage(StoredAppearancePreferences("DARK", true))
        val repository = DefaultThemePreferencesRepository(storage)

        repository.setThemeMode(ThemeMode.AMOLED)

        assertThat(repository.themePreferences.first()).isEqualTo(ThemePreferences(ThemeMode.AMOLED, false))
    }

    @Test
    fun enablingDynamicColorWhileAmoledSwitchesToSystem() = runTest {
        val storage = FakeAppearancePreferencesStorage(StoredAppearancePreferences("AMOLED", false))
        val repository = DefaultThemePreferencesRepository(storage)

        repository.setDynamicColorEnabled(true)

        assertThat(repository.themePreferences.first()).isEqualTo(ThemePreferences(ThemeMode.SYSTEM, true))
    }

    private class FakeAppearancePreferencesStorage(initial: StoredAppearancePreferences) :
        AppearancePreferencesStorage {
        private val state = MutableStateFlow(initial)
        override val preferences = state

        override suspend fun update(transform: (StoredAppearancePreferences) -> StoredAppearancePreferences) {
            state.value = transform(state.value)
        }
    }
}

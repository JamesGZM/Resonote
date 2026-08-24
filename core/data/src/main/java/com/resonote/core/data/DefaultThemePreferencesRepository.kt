package com.resonote.core.data

import com.resonote.core.datastore.AppearancePreferencesStorage
import com.resonote.core.model.ThemeMode
import com.resonote.core.model.ThemePreferences
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultThemePreferencesRepository @Inject constructor(
    private val storage: AppearancePreferencesStorage,
) : ThemePreferencesRepository {
    override val themePreferences = storage.preferences.map { stored ->
        val themeMode = ThemeMode.entries.firstOrNull { it.name == stored.themeMode } ?: ThemeMode.SYSTEM
        ThemePreferences(
            themeMode = themeMode,
            dynamicColorEnabled = stored.dynamicColorEnabled && themeMode != ThemeMode.AMOLED,
        )
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        storage.update { current ->
            current.copy(
                themeMode = themeMode.name,
                dynamicColorEnabled = current.dynamicColorEnabled && themeMode != ThemeMode.AMOLED,
            )
        }
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        storage.update { current ->
            val currentMode = ThemeMode.entries.firstOrNull { it.name == current.themeMode } ?: ThemeMode.SYSTEM
            current.copy(
                themeMode = if (enabled && currentMode == ThemeMode.AMOLED) {
                    ThemeMode.SYSTEM.name
                } else {
                    currentMode.name
                },
                dynamicColorEnabled = enabled,
            )
        }
    }

    override suspend fun reset() {
        storage.update { com.resonote.core.datastore.StoredAppearancePreferences("", false) }
    }
}

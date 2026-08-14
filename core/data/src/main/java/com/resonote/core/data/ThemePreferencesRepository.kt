package com.resonote.core.data

import com.resonote.core.model.ThemeMode
import com.resonote.core.model.ThemePreferences
import kotlinx.coroutines.flow.Flow

interface ThemePreferencesRepository {
    val themePreferences: Flow<ThemePreferences>

    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setDynamicColorEnabled(enabled: Boolean)
}

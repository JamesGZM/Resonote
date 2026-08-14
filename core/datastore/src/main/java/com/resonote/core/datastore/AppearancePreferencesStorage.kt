package com.resonote.core.datastore

import kotlinx.coroutines.flow.Flow

data class StoredAppearancePreferences(val themeMode: String, val dynamicColorEnabled: Boolean)

interface AppearancePreferencesStorage {
    val preferences: Flow<StoredAppearancePreferences>

    suspend fun update(transform: (StoredAppearancePreferences) -> StoredAppearancePreferences)
}

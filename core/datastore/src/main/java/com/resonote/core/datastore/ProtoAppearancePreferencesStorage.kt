package com.resonote.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import com.resonote.core.datastore.proto.AppearancePreferences
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object AppearancePreferencesSerializer : Serializer<AppearancePreferences> {
    override val defaultValue: AppearancePreferences = AppearancePreferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): AppearancePreferences = AppearancePreferences.parseFrom(input)

    override suspend fun writeTo(t: AppearancePreferences, output: OutputStream) = t.writeTo(output)
}

@Singleton
internal class ProtoAppearancePreferencesStorage @Inject constructor(
    private val store: DataStore<AppearancePreferences>,
) : AppearancePreferencesStorage {
    override val preferences: Flow<StoredAppearancePreferences> = store.data.map { it.toStoredPreferences() }

    override suspend fun update(transform: (StoredAppearancePreferences) -> StoredAppearancePreferences) {
        store.updateData { current ->
            val updated = transform(current.toStoredPreferences())
            current.copy(
                themeMode = updated.themeMode,
                dynamicColorEnabled = updated.dynamicColorEnabled,
            )
        }
    }
}

private fun AppearancePreferences.toStoredPreferences() =
    StoredAppearancePreferences(
        themeMode = themeMode,
        dynamicColorEnabled = dynamicColorEnabled,
    )

package com.resonote.core.datastore

import androidx.datastore.core.DataStoreFactory
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProtoAppearancePreferencesStorageTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun preferencesPersistAcrossStoreInstances() = runTest {
        val file = File(temporaryFolder.newFolder("preferences"), "appearance_preferences.pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val firstStore = DataStoreFactory.create(
            serializer = AppearancePreferencesSerializer,
            scope = firstScope,
            produceFile = { file },
        )
        try {
            ProtoAppearancePreferencesStorage(firstStore).update {
                StoredAppearancePreferences("DARK", dynamicColorEnabled = true)
            }
        } finally {
            firstScope.cancel()
        }

        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val secondStore = DataStoreFactory.create(
            serializer = AppearancePreferencesSerializer,
            scope = secondScope,
            produceFile = { file },
        )
        try {
            assertThat(ProtoAppearancePreferencesStorage(secondStore).preferences.first()).isEqualTo(
                StoredAppearancePreferences("DARK", dynamicColorEnabled = true),
            )
        } finally {
            secondScope.cancel()
        }
    }
}

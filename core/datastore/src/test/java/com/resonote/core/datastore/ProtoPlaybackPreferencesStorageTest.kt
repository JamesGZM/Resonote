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

class ProtoPlaybackPreferencesStorageTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun playbackSpeedPersistsAcrossStoreInstances() = runTest {
        val file = File(temporaryFolder.newFolder("preferences"), "playback_preferences.pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val firstStore = DataStoreFactory.create(
            serializer = PlaybackPreferencesSerializer,
            scope = firstScope,
            produceFile = { file },
        )

        try {
            ProtoPlaybackPreferencesStorage(firstStore).setPlaybackSpeedPercent(150)
        } finally {
            firstScope.cancel()
        }

        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val secondStore = DataStoreFactory.create(
            serializer = PlaybackPreferencesSerializer,
            scope = secondScope,
            produceFile = { file },
        )
        try {
            assertThat(ProtoPlaybackPreferencesStorage(secondStore).playbackSpeedPercent.first()).isEqualTo(150)
        } finally {
            secondScope.cancel()
        }
    }
}

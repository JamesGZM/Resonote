package com.resonote.core.datastore

import androidx.datastore.core.DataStoreFactory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProtoSnapshotStorageTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun homeSnapshotPersistsAcrossStoreInstancesAndClears() = runTest {
        val file = File(temporaryFolder.newFolder("home"), "home.pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val first = DataStoreFactory.create(HomeSnapshotSerializer, scope = firstScope, produceFile = { file })
        ProtoHomeSnapshotStorage(first).write("{\"home\":true}")
        firstScope.cancel()

        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val second = ProtoHomeSnapshotStorage(
                DataStoreFactory.create(HomeSnapshotSerializer, scope = secondScope, produceFile = { file }),
            )
            assertThat(second.snapshotJson.first()).isEqualTo("{\"home\":true}")
            second.clear()
            assertThat(second.snapshotJson.first()).isNull()
        } finally {
            secondScope.cancel()
        }
    }

    @Test
    fun playbackSnapshotPersistsAndMalformedProtoFallsBackToEmpty() = runTest {
        val directory = temporaryFolder.newFolder("playback")
        val file = File(directory, "playback.pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val first = ProtoPlaybackSessionSnapshotStorage(
            DataStoreFactory.create(PlaybackSessionSnapshotSerializer, scope = firstScope, produceFile = { file }),
        )
        first.write("{\"queue\":[]}")
        assertThat(first.snapshotJson.first()).isEqualTo("{\"queue\":[]}")
        firstScope.cancel()

        file.writeBytes(byteArrayOf(0x0a, 0x7f))
        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val malformed = ProtoPlaybackSessionSnapshotStorage(
                DataStoreFactory.create(
                    PlaybackSessionSnapshotSerializer,
                    scope = secondScope,
                    produceFile = { file },
                ),
            )
            assertThat(malformed.snapshotJson.first()).isNull()
        } finally {
            secondScope.cancel()
        }
    }
}

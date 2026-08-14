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

class ProtoEncryptedSessionStorageTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun envelopeRoundTripsThroughRealProtoDataStoreAndClears() = runTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val file = File(temporaryFolder.newFolder("session"), "api_session.pb")
        val dataStore =
            DataStoreFactory.create(
                serializer = EncryptedApiSessionSerializer,
                scope = scope,
                produceFile = { file },
            )
        val storage = ProtoEncryptedSessionStorage(dataStore)
        val envelope =
            EncryptedSessionEnvelope(schemaVersion = 1, iv = byteArrayOf(1, 2), ciphertext = byteArrayOf(3, 4))

        try {
            storage.write(envelope)

            assertThat(storage.data.first()).isEqualTo(envelope)
            assertThat(file.exists()).isTrue()

            storage.clear()
            assertThat(storage.data.first()).isNull()
        } finally {
            scope.cancel()
        }
    }
}

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

class ProtoSearchHistoryStorageTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun historyPersistsMostRecentUniqueTwentyQueriesAndSupportsDeletion() = runTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val file = File(temporaryFolder.newFolder("history"), "search_history.pb")
        val dataStore = DataStoreFactory.create(
            serializer = SearchHistorySerializer,
            scope = scope,
            produceFile = { file },
        )
        val storage = ProtoSearchHistoryStorage(dataStore)

        try {
            repeat(22) { storage.add("query-$it") }
            storage.add(" query-10 ")

            assertThat(storage.queries.first()).hasSize(20)
            assertThat(storage.queries.first().first()).isEqualTo("query-10")
            assertThat(storage.queries.first().count { it == "query-10" }).isEqualTo(1)
            assertThat(storage.queries.first()).doesNotContain("query-0")

            storage.remove("query-10")
            assertThat(storage.queries.first()).doesNotContain("query-10")

            storage.clear()
            assertThat(storage.queries.first()).isEmpty()
            assertThat(file.exists()).isTrue()
        } finally {
            scope.cancel()
        }
    }
}

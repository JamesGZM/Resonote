package com.resonote.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import com.resonote.core.datastore.proto.SearchHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

object SearchHistorySerializer : Serializer<SearchHistory> {
    override val defaultValue: SearchHistory = SearchHistory.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SearchHistory = SearchHistory.parseFrom(input)

    override suspend fun writeTo(t: SearchHistory, output: OutputStream) = t.writeTo(output)
}

@Singleton
internal class ProtoSearchHistoryStorage @Inject constructor(private val store: DataStore<SearchHistory>) :
    SearchHistoryStorage {
    override val queries: Flow<List<String>> = store.data.map { it.queriesList }

    override suspend fun add(query: String) {
        val normalized = query.trim()
        if (normalized.isEmpty()) return
        store.updateData { current ->
            val updated = buildList {
                add(normalized)
                current.queriesList.filterTo(this) { it != normalized }
            }.take(MAX_HISTORY_SIZE)
            current.toBuilder().clearQueries().addAllQueries(updated).build()
        }
    }

    override suspend fun remove(query: String) {
        store.updateData { current ->
            current.toBuilder().clearQueries().addAllQueries(current.queriesList.filterNot { it == query }).build()
        }
    }

    override suspend fun clear() {
        store.updateData { SearchHistory.getDefaultInstance() }
    }

    private companion object {
        const val MAX_HISTORY_SIZE = 20
    }
}

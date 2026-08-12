package com.resonote.core.data

import com.resonote.core.datastore.SearchHistoryStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultSearchHistoryRepository @Inject constructor(
    private val storage: SearchHistoryStorage,
) : SearchHistoryRepository {
    override val queries = storage.queries

    override suspend fun record(query: String) = storage.add(query)

    override suspend fun remove(query: String) = storage.remove(query)

    override suspend fun clear() = storage.clear()
}

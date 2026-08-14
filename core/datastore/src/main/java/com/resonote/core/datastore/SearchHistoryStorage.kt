package com.resonote.core.datastore

import kotlinx.coroutines.flow.Flow

interface SearchHistoryStorage {
    val queries: Flow<List<String>>

    suspend fun add(query: String)

    suspend fun remove(query: String)

    suspend fun clear()
}

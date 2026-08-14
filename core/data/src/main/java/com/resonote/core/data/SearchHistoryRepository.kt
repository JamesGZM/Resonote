package com.resonote.core.data

import kotlinx.coroutines.flow.Flow

interface SearchHistoryRepository {
    val queries: Flow<List<String>>

    suspend fun record(query: String)

    suspend fun remove(query: String)

    suspend fun clear()
}

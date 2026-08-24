package com.resonote.core.data

import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.DeviceHistoryItem
import com.resonote.core.model.DeviceHistoryRecord
import com.resonote.core.model.ListeningHistoryPage
import kotlinx.coroutines.flow.Flow

interface ListeningHistoryRepository {
    suspend fun loadAccountHistory(cursor: String? = null): CollectionLoadResult<ListeningHistoryPage>

    fun observeDeviceHistory(): Flow<List<DeviceHistoryItem>>

    suspend fun recordDevicePlayback(record: DeviceHistoryRecord): Boolean

    suspend fun recordAccountPlayback(albumAudioId: String): Boolean

    suspend fun deleteDeviceHistory(record: DeviceHistoryRecord): Boolean

    suspend fun clearDeviceHistory(): Boolean
}

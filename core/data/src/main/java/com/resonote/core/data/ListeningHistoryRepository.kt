package com.resonote.core.data

import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.DeviceHistoryItem
import com.resonote.core.model.DeviceHistoryRecord
import com.resonote.core.model.OnlineSong
import kotlinx.coroutines.flow.Flow

interface ListeningHistoryRepository {
    suspend fun loadAccountHistory(): CollectionLoadResult<List<OnlineSong>>

    fun observeDeviceHistory(): Flow<List<DeviceHistoryItem>>

    suspend fun recordDevicePlayback(record: DeviceHistoryRecord): Boolean

    suspend fun deleteDeviceHistory(record: DeviceHistoryRecord): Boolean

    suspend fun clearDeviceHistory(): Boolean
}

package com.resonote.core.datastore

import kotlinx.coroutines.flow.Flow

interface HomeSnapshotStorage {
    val snapshotJson: Flow<String?>

    suspend fun write(json: String)

    suspend fun clear()
}

interface PlaybackSessionSnapshotStorage {
    val snapshotJson: Flow<String?>

    suspend fun write(json: String)

    suspend fun clear()
}

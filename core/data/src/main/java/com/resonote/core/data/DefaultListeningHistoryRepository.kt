package com.resonote.core.data

import com.resonote.core.database.history.DeviceHistoryDao
import com.resonote.core.database.history.asExternalModel
import com.resonote.core.database.history.toEntity
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.DeviceHistoryItem
import com.resonote.core.model.DeviceHistoryRecord
import com.resonote.core.model.ListeningHistoryPage
import com.resonote.core.network.ApiException
import com.resonote.core.network.ListeningHistoryNetworkDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultListeningHistoryRepository internal constructor(
    private val network: ListeningHistoryNetworkDataSource,
    private val riskChallenges: RiskChallengeRegistry,
    private val deviceHistory: DeviceHistoryDao,
    private val now: () -> Long,
) : ListeningHistoryRepository {
    @Inject
    constructor(
        network: ListeningHistoryNetworkDataSource,
        riskChallenges: RiskChallengeRegistry,
        deviceHistory: DeviceHistoryDao,
    ) : this(network, riskChallenges, deviceHistory, System::currentTimeMillis)

    override suspend fun loadAccountHistory(cursor: String?): CollectionLoadResult<ListeningHistoryPage> = try {
        val page = network.accountHistory(cursor)
        CollectionLoadResult.Available(
            ListeningHistoryPage(
                songs = page.songs.map { it.toOnlineSong() },
                nextCursor = page.nextCursor,
                hasMore = page.hasMore,
            ),
        )
    } catch (failure: ApiException) {
        CollectionLoadResult.Failed(failure.toContentFailure(riskChallenges))
    }

    override fun observeDeviceHistory(): Flow<List<DeviceHistoryItem>> = deviceHistory.observeAll().map { rows ->
        rows.map { it.asExternalModel() }
    }

    override suspend fun recordDevicePlayback(record: DeviceHistoryRecord): Boolean = mutate {
        deviceHistory.record(record.toEntity(now()))
    }

    override suspend fun recordAccountPlayback(albumAudioId: String): Boolean = mutate {
        network.uploadAccountPlayback(albumAudioId)
    }

    override suspend fun deleteDeviceHistory(record: DeviceHistoryRecord): Boolean = mutate {
        check(
            deviceHistory.delete(record.toEntity(0).source, record.mediaId) == 1,
        )
    }

    override suspend fun clearDeviceHistory(): Boolean = mutate { deviceHistory.clear() }

    private suspend fun mutate(block: suspend () -> Unit): Boolean = try {
        block()
        true
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        false
    }
}

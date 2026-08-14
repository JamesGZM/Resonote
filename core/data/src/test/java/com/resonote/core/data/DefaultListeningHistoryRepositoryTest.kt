package com.resonote.core.data

import com.google.common.truth.Truth.assertThat
import com.resonote.core.database.history.DeviceHistoryDao
import com.resonote.core.database.history.DeviceHistoryEntity
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.DeviceHistoryRecord
import com.resonote.core.model.DeviceHistorySource
import com.resonote.core.network.ApiAuthenticationRequiredException
import com.resonote.core.network.ListeningHistoryNetworkDataSource
import com.resonote.core.network.model.NetworkSong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultListeningHistoryRepositoryTest {
    @Test
    fun accountHistoryMapsNetworkSongsWithoutInventingQuality() = runTest {
        val repository = repository()

        val result = repository.loadAccountHistory() as CollectionLoadResult.Available

        assertThat(result.value.single().title).isEqualTo("Song")
        assertThat(result.value.single().coverUrl).isEqualTo("https://image/480/cover.jpg")
        assertThat(result.value.single().quality).isEqualTo(AudioQuality.Standard)
        assertThat(result.value.single().vip).isFalse()
    }

    @Test
    fun authenticationFailureRemainsTyped() = runTest {
        val repository =
            DefaultListeningHistoryRepository(
                FakeHistoryNetwork(ApiAuthenticationRequiredException()),
                RiskChallengeRegistry(),
                FakeDeviceHistoryDao(),
                now = { 2_000 },
            )

        val result = repository.loadAccountHistory() as CollectionLoadResult.Failed

        assertThat(result.failure).isEqualTo(ContentFailure.AuthenticationRequired)
    }

    @Test
    fun deviceHistoryUsesLocalClockAndMapsStableSource() = runTest {
        val dao = FakeDeviceHistoryDao()
        val repository = repository(dao)
        val record = deviceRecord()

        assertThat(repository.recordDevicePlayback(record)).isTrue()
        val item = repository.observeDeviceHistory().first().single()

        assertThat(item.record).isEqualTo(record)
        assertThat(item.lastPlayedAtEpochMillis).isEqualTo(2_000)
        assertThat(item.playCount).isEqualTo(1)
    }

    @Test
    fun deviceHistoryDeleteAndClearStayLocalToHistoryStorage() = runTest {
        val dao = FakeDeviceHistoryDao()
        val repository = repository(dao)
        repository.recordDevicePlayback(deviceRecord("one"))
        repository.recordDevicePlayback(deviceRecord("two"))

        assertThat(repository.deleteDeviceHistory(deviceRecord("one"))).isTrue()
        assertThat(repository.observeDeviceHistory().first().map { it.record.mediaId }).containsExactly("two")
        assertThat(repository.clearDeviceHistory()).isTrue()
        assertThat(repository.observeDeviceHistory().first()).isEmpty()
    }

    private fun repository(deviceHistory: DeviceHistoryDao = FakeDeviceHistoryDao()) =
        DefaultListeningHistoryRepository(
            FakeHistoryNetwork(),
            RiskChallengeRegistry(),
            deviceHistory,
            now = { 2_000 },
        )

    private fun deviceRecord(id: String = "local-id") = DeviceHistoryRecord(
        source = DeviceHistorySource.Local,
        mediaId = id,
        title = "Song $id",
        artist = "Artist",
        albumTitle = null,
        artworkUri = null,
        durationMillis = 120_000,
        albumAudioId = null,
    )

    private class FakeHistoryNetwork(private val failure: ApiAuthenticationRequiredException? = null) :
        ListeningHistoryNetworkDataSource {
        override suspend fun accountHistory(): List<NetworkSong> {
            failure?.let { throw it }
            return listOf(
                NetworkSong(
                    hash = "HASH",
                    title = "Song",
                    artist = "Artist",
                    coverUrl = "https://image/{size}/cover.jpg",
                    albumId = null,
                    albumAudioId = null,
                    durationMillis = 120_000,
                    highQualityHash = null,
                    losslessHash = null,
                    vip = false,
                ),
            )
        }
    }

    private class FakeDeviceHistoryDao : DeviceHistoryDao {
        private val rows = MutableStateFlow<List<DeviceHistoryEntity>>(emptyList())

        override fun observeAll() = rows

        override suspend fun findAll(): List<DeviceHistoryEntity> = rows.value

        override suspend fun insert(entity: DeviceHistoryEntity): Long {
            if (rows.value.any { it.source == entity.source && it.mediaId == entity.mediaId }) return -1
            rows.value += entity
            return rows.value.lastIndex.toLong()
        }

        override suspend fun increment(
            source: String,
            mediaId: String,
            title: String,
            artist: String?,
            albumTitle: String?,
            artworkUri: String?,
            durationMillis: Long,
            albumAudioId: String?,
            playedAtEpochMillis: Long,
        ): Int {
            val index = rows.value.indexOfFirst { it.source == source && it.mediaId == mediaId }
            if (index < 0) return 0
            val current = rows.value[index]
            rows.value = rows.value.toMutableList().apply {
                set(
                    index,
                    current.copy(
                        title = title,
                        artist = artist,
                        albumTitle = albumTitle,
                        artworkUri = artworkUri,
                        durationMillis = durationMillis,
                        albumAudioId = albumAudioId,
                        lastPlayedAtEpochMillis = maxOf(current.lastPlayedAtEpochMillis, playedAtEpochMillis),
                        playCount = current.playCount + 1,
                    ),
                )
            }
            return 1
        }

        override suspend fun trimToLimit(limit: Int): Int {
            val retained = rows.value.sortedByDescending(DeviceHistoryEntity::lastPlayedAtEpochMillis).take(limit)
            val removed = rows.value.size - retained.size
            rows.value = retained
            return removed
        }

        override suspend fun delete(source: String, mediaId: String): Int {
            val retained = rows.value.filterNot { it.source == source && it.mediaId == mediaId }
            val removed = rows.value.size - retained.size
            rows.value = retained
            return removed
        }

        override suspend fun clear(): Int {
            val removed = rows.value.size
            rows.value = emptyList()
            return removed
        }
    }
}

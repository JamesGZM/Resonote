package com.resonote.core.data

import com.google.common.truth.Truth.assertThat
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ContentFailure
import com.resonote.core.network.ApiAuthenticationRequiredException
import com.resonote.core.network.ListeningHistoryNetworkDataSource
import com.resonote.core.network.model.NetworkSong
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultListeningHistoryRepositoryTest {
    @Test
    fun accountHistoryMapsNetworkSongsWithoutInventingQuality() = runTest {
        val repository = DefaultListeningHistoryRepository(FakeHistoryNetwork(), RiskChallengeRegistry())

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
            )

        val result = repository.loadAccountHistory() as CollectionLoadResult.Failed

        assertThat(result.failure).isEqualTo(ContentFailure.AuthenticationRequired)
    }

    private class FakeHistoryNetwork(
        private val failure: ApiAuthenticationRequiredException? = null,
    ) : ListeningHistoryNetworkDataSource {
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
}

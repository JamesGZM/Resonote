package com.resonote.core.data

import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaybackUnavailableReason
import com.resonote.core.model.ResolveSongSourceResult
import com.resonote.core.model.ResolvedSongSource
import com.resonote.core.network.ApiNetworkDataSource
import com.resonote.core.network.ApiPlaybackUnavailableException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
internal class DefaultSongPlaybackRepository @Inject constructor(
    private val network: ApiNetworkDataSource,
) : SongPlaybackRepository {
    override suspend fun resolveSource(song: OnlineSong): ResolveSongSourceResult =
        try {
            val source = network.resolveSongSource(song.hash, song.albumId, song.albumAudioId)
            ResolveSongSourceResult.Resolved(
                ResolvedSongSource(source.uri, source.durationMillis.takeIf { it > 0 } ?: song.durationMillis, source.extension),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (unavailable: ApiPlaybackUnavailableException) {
            ResolveSongSourceResult.Unavailable(
                when (unavailable.reason) {
                    ApiPlaybackUnavailableException.Reason.Copyright -> PlaybackUnavailableReason.Copyright
                    ApiPlaybackUnavailableException.Reason.Vip -> PlaybackUnavailableReason.Vip
                },
            )
        } catch (failure: Throwable) {
            ResolveSongSourceResult.Failed(failure.toContentFailure())
        }
}

package com.resonote.core.data

import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaybackUnavailableReason
import com.resonote.core.model.ResolveSongSourceResult
import com.resonote.core.model.ResolvedSongSource
import com.resonote.core.network.ApiException
import com.resonote.core.network.ApiNetworkDataSource
import com.resonote.core.network.ApiPlaybackUnavailableException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultSongPlaybackRepository @Inject constructor(
    private val network: ApiNetworkDataSource,
    private val riskChallenges: RiskChallengeRegistry,
) : SongPlaybackRepository {
    override suspend fun resolveSource(song: OnlineSong): ResolveSongSourceResult =
        try {
            val source = network.resolveSongSource(song.hash, song.albumId, song.albumAudioId)
            ResolveSongSourceResult.Resolved(
                ResolvedSongSource(source.uri, source.durationMillis.takeIf { it > 0 } ?: song.durationMillis, source.extension),
            )
        } catch (unavailable: ApiPlaybackUnavailableException) {
            ResolveSongSourceResult.Unavailable(
                when (unavailable.reason) {
                    ApiPlaybackUnavailableException.Reason.Copyright -> PlaybackUnavailableReason.Copyright
                    ApiPlaybackUnavailableException.Reason.Vip -> PlaybackUnavailableReason.Vip
                },
            )
        } catch (failure: ApiException) {
            ResolveSongSourceResult.Failed(failure.toContentFailure(riskChallenges))
        }
}

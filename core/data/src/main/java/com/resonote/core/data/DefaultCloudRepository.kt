package com.resonote.core.data

import com.resonote.core.model.CloudPage
import com.resonote.core.model.CloudStorage
import com.resonote.core.model.CloudTrack
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.PlaybackUnavailableReason
import com.resonote.core.model.ResolveSongSourceResult
import com.resonote.core.model.ResolvedSongSource
import com.resonote.core.network.ApiException
import com.resonote.core.network.CloudNetworkDataSource
import com.resonote.core.network.ApiPlaybackUnavailableException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultCloudRepository @Inject constructor(
    private val network: CloudNetworkDataSource,
    private val riskChallenges: RiskChallengeRegistry,
) : CloudRepository {
    override suspend fun loadTracks(page: Int, pageSize: Int): CollectionLoadResult<CloudPage> {
        validateCollectionPage(page, pageSize)
        return loadCollection(riskChallenges) {
            val result = network.cloudTracks(page, pageSize)
            CloudPage(
                tracks = result.tracks.map {
                    CloudTrack(
                        hash = it.hash,
                        title = it.title,
                        artist = it.artist,
                        album = it.album,
                        coverUrl = it.coverUrl?.replace("{size}", "240"),
                        durationMillis = it.durationMillis,
                        albumAudioId = it.albumAudioId,
                    )
                },
                page = page,
                total = result.total,
                hasMore = result.hasMore,
                storage = result.storage?.let { CloudStorage(it.usedBytes, it.maxBytes) },
            )
        }
    }

    override suspend fun resolveSource(track: CloudTrack): ResolveSongSourceResult =
        try {
            val source = network.resolveCloudSongSource(track.hash, track.albumAudioId, track.title)
            ResolveSongSourceResult.Resolved(
                ResolvedSongSource(source.uri, source.durationMillis.takeIf { it > 0 } ?: track.durationMillis, source.extension),
            )
        } catch (unavailable: ApiPlaybackUnavailableException) {
            ResolveSongSourceResult.Unavailable(PlaybackUnavailableReason.Cloud)
        } catch (failure: ApiException) {
            ResolveSongSourceResult.Failed(failure.toContentFailure(riskChallenges))
        }
}

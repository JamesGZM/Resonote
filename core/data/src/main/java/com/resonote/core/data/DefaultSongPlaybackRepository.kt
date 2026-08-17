package com.resonote.core.data

import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaybackUnavailableReason
import com.resonote.core.model.ResolveSongSourceResult
import com.resonote.core.model.ResolvedSongSource
import com.resonote.core.network.ApiException
import com.resonote.core.network.ApiPlaybackUnavailableException
import com.resonote.core.network.PlaybackNetworkDataSource
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultSongPlaybackRepository @Inject constructor(
    private val network: PlaybackNetworkDataSource,
    private val riskChallenges: RiskChallengeRegistry,
    private val playbackPreferences: PlaybackPreferencesRepository,
) : SongPlaybackRepository {
    override suspend fun resolveSource(song: OnlineSong): ResolveSongSourceResult = try {
        val quality = playbackPreferences.onlinePlaybackQuality
            .catch { emit(OnlinePlaybackQuality.Standard) }
            .first()
        val source = network.resolveSongSource(song.hash, song.albumId, song.albumAudioId, quality.wireValue())
        ResolveSongSourceResult.Resolved(
            ResolvedSongSource(
                source.uri,
                source.durationMillis.takeIf {
                    it > 0
                } ?: song.durationMillis,
                source.extension,
                source.isPreview,
                onlinePlaybackCacheKey(song.hash, quality, source.isPreview),
            ),
        )
    } catch (unavailable: ApiPlaybackUnavailableException) {
        ResolveSongSourceResult.Unavailable(
            when (unavailable.reason) {
                ApiPlaybackUnavailableException.Reason.Copyright -> PlaybackUnavailableReason.Copyright
                ApiPlaybackUnavailableException.Reason.Vip -> PlaybackUnavailableReason.Vip
                ApiPlaybackUnavailableException.Reason.Cloud -> PlaybackUnavailableReason.Cloud
            },
        )
    } catch (failure: ApiException) {
        ResolveSongSourceResult.Failed(failure.toContentFailure(riskChallenges))
    }

    private fun OnlinePlaybackQuality.wireValue(): String = when (this) {
        OnlinePlaybackQuality.Standard -> "128"
        OnlinePlaybackQuality.HighQuality -> "320"
        OnlinePlaybackQuality.Lossless -> "flac"
        OnlinePlaybackQuality.HighResolution -> "high"
        OnlinePlaybackQuality.ViperAtmos -> "viper_atmos"
        OnlinePlaybackQuality.ViperClear -> "viper_clear"
        OnlinePlaybackQuality.ViperTape -> "viper_tape"
    }
}

internal fun onlinePlaybackCacheKey(songHash: String, quality: OnlinePlaybackQuality, isPreview: Boolean): String =
    buildString {
        append("online:")
        append(songHash)
        append(':')
        append(quality.name)
        append(if (isPreview) ":preview" else ":full")
    }

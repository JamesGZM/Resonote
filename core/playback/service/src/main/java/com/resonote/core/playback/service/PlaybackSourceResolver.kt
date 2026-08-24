package com.resonote.core.playback.service

import com.resonote.core.data.CloudRepository
import com.resonote.core.data.LocalMediaRepository
import com.resonote.core.data.SongPlaybackRepository
import com.resonote.core.model.PlaybackUnavailableReason
import com.resonote.core.model.ResolveSongSourceResult
import com.resonote.core.model.ResolvedSongSource
import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackOrigin
import javax.inject.Inject

internal class PlaybackSourceResolver @Inject constructor(
    private val songPlaybackRepository: SongPlaybackRepository,
    private val cloudRepository: CloudRepository,
    private val localMediaRepository: LocalMediaRepository,
) {
    suspend fun resolve(item: PlaybackItem): ResolveSongSourceResult =
        item.resolvedSource?.let(ResolveSongSourceResult::Resolved)
            ?: when (val origin = item.origin) {
                is PlaybackOrigin.Online -> songPlaybackRepository.resolveSource(
                    origin.song,
                    item.onlineQualityOverride,
                )
                is PlaybackOrigin.Cloud -> cloudRepository.resolveSource(origin.track)
                is PlaybackOrigin.Local -> localMediaRepository.resolvePlaybackSource(origin.id)?.let { source ->
                    ResolveSongSourceResult.Resolved(
                        ResolvedSongSource(
                            uri = source.uri,
                            durationMillis = source.media.durationMillis,
                            extension = source.media.fileExtension,
                        ),
                    )
                } ?: ResolveSongSourceResult.Unavailable(PlaybackUnavailableReason.Local)
            }
}

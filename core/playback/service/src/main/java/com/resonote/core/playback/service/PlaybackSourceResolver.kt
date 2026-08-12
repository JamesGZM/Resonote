package com.resonote.core.playback.service

import com.resonote.core.data.CloudRepository
import com.resonote.core.data.SongPlaybackRepository
import com.resonote.core.model.ResolveSongSourceResult
import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackOrigin
import javax.inject.Inject

internal class PlaybackSourceResolver @Inject constructor(
    private val songPlaybackRepository: SongPlaybackRepository,
    private val cloudRepository: CloudRepository,
) {
    suspend fun resolve(item: PlaybackItem): ResolveSongSourceResult =
        item.resolvedSource?.let(ResolveSongSourceResult::Resolved)
            ?: when (val origin = item.origin) {
                PlaybackOrigin.Online -> songPlaybackRepository.resolveSource(item.song)
                is PlaybackOrigin.Cloud -> cloudRepository.resolveSource(origin.track)
            }
}

package com.resonote.core.playback.service

import com.resonote.core.data.CloudRepository
import com.resonote.core.data.LocalMediaRepository
import com.resonote.core.data.SongPlaybackRepository
import com.resonote.core.model.PlaybackUnavailableReason
import com.resonote.core.model.ResolveSongSourceResult
import com.resonote.core.model.ResolvedSongSource
import com.resonote.core.playback.MusicDownloadController
import com.resonote.core.playback.PlaybackItem
import com.resonote.core.playback.PlaybackOrigin
import javax.inject.Inject

internal class PlaybackSourceResolver @Inject constructor(
    private val songPlaybackRepository: SongPlaybackRepository,
    private val cloudRepository: CloudRepository,
    private val localMediaRepository: LocalMediaRepository,
    private val musicDownloadController: MusicDownloadController,
) {
    internal constructor(
        songPlaybackRepository: SongPlaybackRepository,
        cloudRepository: CloudRepository,
        localMediaRepository: LocalMediaRepository,
    ) : this(songPlaybackRepository, cloudRepository, localMediaRepository, EmptyMusicDownloadController)

    suspend fun resolve(item: PlaybackItem): ResolveSongSourceResult =
        item.resolvedSource?.let(ResolveSongSourceResult::Resolved)
            ?: when (val origin = item.origin) {
                is PlaybackOrigin.Online ->
                    musicDownloadController.completedSource(origin.song.hash, item.onlineQualityOverride)
                        ?.let(ResolveSongSourceResult::Resolved)
                        ?: songPlaybackRepository.resolveSource(origin.song, item.onlineQualityOverride)
                is PlaybackOrigin.Cloud -> cloudRepository.resolveSource(origin.track)
                is PlaybackOrigin.Local -> localMediaRepository.resolvePlaybackSource(origin.id)?.let { source ->
                    ResolveSongSourceResult.Resolved(
                        ResolvedSongSource(
                            uri = source.uri,
                            durationMillis = source.media.durationMillis,
                            extension = source.media.fileExtension,
                            isOffline = true,
                        ),
                    )
                } ?: ResolveSongSourceResult.Unavailable(PlaybackUnavailableReason.Local)
            }

    private object EmptyMusicDownloadController : MusicDownloadController {
        override val downloads = kotlinx.coroutines.flow.MutableStateFlow(
            emptyList<com.resonote.core.playback.MusicDownload>(),
        )
        override fun download(song: com.resonote.core.model.OnlineSong) = Unit
        override fun pause(id: String) = Unit
        override fun resume(id: String) = Unit
        override fun retry(id: String) = Unit
        override fun remove(id: String) = Unit
        override fun pauseAll() = Unit
        override fun resumeAll() = Unit
        override fun completedSource(
            songHash: String,
            quality: com.resonote.core.model.OnlinePlaybackQuality?,
        ): com.resonote.core.model.ResolvedSongSource? = null
    }
}

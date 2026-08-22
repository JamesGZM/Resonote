package com.resonote.feature.search.impl

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteArtistItem
import com.resonote.core.designsystem.component.ResonoteArtistMetadata
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.ResonoteMediaCardItem
import com.resonote.core.designsystem.component.ResonoteMediaCardMetadata
import com.resonote.core.designsystem.component.ResonoteMusicItem
import com.resonote.core.designsystem.component.ResonotePlainAction
import com.resonote.core.designsystem.component.ResonoteSectionHeader
import com.resonote.core.designsystem.component.ResonoteVideoItem
import com.resonote.core.designsystem.component.ResonoteVideoMetadata
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.SearchAlbum
import com.resonote.core.model.SearchArtist
import com.resonote.core.model.SearchMv
import com.resonote.core.model.SearchPlaylist

@Composable
internal fun SearchResultSectionHeader(title: String, total: Int? = null, onClick: () -> Unit) {
    ResonoteSectionHeader(
        title = title,
        supportingText = total?.let {
            stringResource(R.string.feature_search_impl_search_result_count, it)
        } ?: stringResource(R.string.feature_search_impl_search_section_supporting),
        modifier = Modifier.padding(horizontal = 16.dp),
        trailingContent = {
            ResonotePlainAction(onClick = onClick) {
                Text(
                    text = stringResource(R.string.feature_search_impl_search_see_all),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    )
}

@Composable
internal fun SearchSongItem(
    song: OnlineSong,
    playingMediaId: String?,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)?,
) = ResonoteMusicItem(
    title = song.title,
    supportingText = song.artist.orEmpty(),
    modifier = Modifier.padding(horizontal = 8.dp),
    duration = song.durationMillis.durationLabel(),
    qualityLabel = song.quality.label(),
    isVip = song.vip,
    isPlaying = song.hash == playingMediaId,
    artworkUrl = song.coverUrl,
    onClick = onClick,
    onMoreClick = onMoreClick,
)

@Composable
internal fun SearchPlaylistItem(playlist: SearchPlaylist, modifier: Modifier, onClick: (() -> Unit)?) {
    ResonoteMediaCardItem(
        metadata = ResonoteMediaCardMetadata(
            title = playlist.name,
            playCount = playlist.playCount.takeIf { it > 0 }?.compactCount(),
            supportingText = listOfNotNull(
                playlist.creator?.takeIf(String::isNotBlank),
                stringResource(R.string.feature_search_impl_search_song_count, playlist.songCount),
            ).joinToString(" · "),
        ),
        artworkContentDescription = stringResource(
            R.string.feature_search_impl_search_playlist_artwork,
            playlist.name,
        ),
        onClick = { onClick?.invoke() },
        modifier = modifier,
        artworkState = if (playlist.coverUrl.isNullOrBlank()) {
            ResonoteArtworkState.MISSING
        } else {
            ResonoteArtworkState.LOADED
        },
        artworkUrl = playlist.coverUrl,
        enabled = onClick != null,
    )
}

@Composable
internal fun SearchAlbumItem(album: SearchAlbum, modifier: Modifier, onClick: (() -> Unit)?) {
    ResonoteMediaCardItem(
        metadata = ResonoteMediaCardMetadata(
            title = album.name,
            supportingText = listOfNotNull(
                album.artist?.takeIf(String::isNotBlank),
                album.publishDate.takeIf(String::isNotBlank),
            ).joinToString(" · ").ifBlank {
                stringResource(R.string.feature_search_impl_search_song_count, album.songCount)
            },
        ),
        artworkContentDescription = stringResource(
            R.string.feature_search_impl_search_album_artwork,
            album.name,
        ),
        onClick = { onClick?.invoke() },
        modifier = modifier,
        artworkState = if (album.coverUrl.isNullOrBlank()) {
            ResonoteArtworkState.MISSING
        } else {
            ResonoteArtworkState.LOADED
        },
        artworkUrl = album.coverUrl,
        enabled = onClick != null,
    )
}

@Composable
internal fun SearchArtistItem(artist: SearchArtist, modifier: Modifier, onClick: (() -> Unit)?) {
    ResonoteArtistItem(
        metadata = ResonoteArtistMetadata(
            title = artist.name,
            supportingText = stringResource(
                R.string.feature_search_impl_search_artist_metadata,
                artist.songCount,
                artist.albumCount,
            ),
        ),
        onClick = { onClick?.invoke() },
        modifier = modifier,
        artworkState = if (artist.avatarUrl.isNullOrBlank()) {
            ResonoteArtworkState.MISSING
        } else {
            ResonoteArtworkState.LOADED
        },
        artworkUrl = artist.avatarUrl,
        enabled = onClick != null,
    )
}

@Composable
internal fun SearchMvItem(mv: SearchMv, modifier: Modifier, onClick: (() -> Unit)?) {
    ResonoteVideoItem(
        metadata = ResonoteVideoMetadata(
            title = mv.name,
            supportingText = mv.singer,
            duration = mv.durationMillis.takeIf { it > 0 }?.durationLabel(),
        ),
        onClick = { onClick?.invoke() },
        modifier = modifier,
        artworkState = if (mv.coverUrl.isNullOrBlank()) ResonoteArtworkState.MISSING else ResonoteArtworkState.LOADED,
        artworkUrl = mv.coverUrl,
        enabled = onClick != null,
    )
}

internal fun Long.compactCount(): String = when {
    this >= 100_000_000 -> compactUnit(100_000_000, "亿")
    this >= 10_000 -> compactUnit(10_000, "万")
    else -> toString()
}

internal fun Long.compactUnit(divisor: Long, suffix: String): String {
    val number = if (this % divisor == 0L) {
        "%.0f".format(this / divisor.toDouble())
    } else {
        "%.1f".format(this / divisor.toDouble())
    }
    return "$number$suffix"
}

internal fun Long.durationLabel(): String {
    val totalSeconds = coerceAtLeast(0) / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

internal fun AudioQuality.label(): String? = when (this) {
    AudioQuality.Standard -> null
    AudioQuality.HighQuality -> "HQ"
    AudioQuality.HighResolution -> "HI-RES"
    AudioQuality.Lossless -> "LOSSLESS"
}

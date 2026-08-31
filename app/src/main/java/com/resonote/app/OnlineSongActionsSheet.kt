@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteBottomSheet
import com.resonote.core.designsystem.component.ResonoteBottomSheetHeader
import com.resonote.core.designsystem.component.ResonotePlainAction
import com.resonote.core.designsystem.component.ResonoteRemoteArtwork
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.OnlineSong
import java.util.Locale

internal data class OnlineSongActionRequest(val song: OnlineSong, val onRemoveRequest: (() -> Unit)? = null)

@Composable
internal fun OnlineSongActionsSheet(
    request: OnlineSongActionRequest,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAppendToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDownload: () -> Unit = {},
    onShowInfo: () -> Unit,
    snackbarHost: @Composable () -> Unit = {},
) {
    ResonoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            ResonoteBottomSheetHeader(
                title = request.song.title,
                subtitle = request.song.artist?.takeIf(String::isNotBlank),
                titleMaxLines = 2,
            )
            Spacer(Modifier.height(12.dp))
            SongActionRow(Icons.Rounded.PlayArrow, R.string.song_action_play, onPlay)
            SongActionRow(Icons.Rounded.SkipNext, R.string.song_action_play_next, onPlayNext)
            SongActionRow(Icons.AutoMirrored.Rounded.PlaylistAdd, R.string.song_action_append_queue, onAppendToQueue)
            SongActionRow(Icons.Rounded.LibraryAdd, R.string.song_action_add_playlist, onAddToPlaylist)
            SongActionRow(Icons.Rounded.Download, R.string.song_action_download, onDownload)
            SongActionRow(Icons.Rounded.Info, R.string.song_action_info, onShowInfo)
            request.onRemoveRequest?.let { remove ->
                HorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                SongActionRow(
                    icon = Icons.Rounded.DeleteOutline,
                    title = R.string.song_action_remove_playlist,
                    onClick = {
                        onDismiss()
                        remove()
                    },
                    destructive = true,
                )
            }
        }
        snackbarHost()
    }
}

@Composable
private fun SongActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: Int,
    onClick: () -> Unit,
    supporting: String? = null,
    destructive: Boolean = false,
) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = supporting?.let { text -> ({ Text(text) }) },
        leadingContent = {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (destructive) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp).size(22.dp),
                    tint = if (destructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    )
}

@Composable
internal fun OnlineSongInfoSheet(
    song: OnlineSong,
    onDismiss: () -> Unit,
    onSearchSong: () -> Unit,
    onSearchArtist: (() -> Unit)?,
    onSearchAlbum: (() -> Unit)?,
) {
    ResonoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 24.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ResonoteRemoteArtwork(
                    model = song.coverUrl,
                    contentDescription = stringResource(R.string.song_info_artwork, song.title),
                    modifier = Modifier.size(76.dp).clip(RoundedCornerShape(16.dp)),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    SongInfoLink(
                        text = song.title,
                        onClick = onSearchSong,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                    )
                    song.artist?.takeIf(String::isNotBlank)?.let { artist ->
                        onSearchArtist?.let { searchArtist ->
                            SongInfoLink(
                                text = artist,
                                onClick = searchArtist,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        } ?: Text(
                            text = artist,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                    Text(
                        stringResource(R.string.song_info_details),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    song.albumTitle?.takeIf(String::isNotBlank)?.let {
                        Spacer(Modifier.height(12.dp))
                        SongInfoField(
                            label = stringResource(R.string.song_info_album),
                            value = it,
                            onClick = onSearchAlbum,
                        )
                        HorizontalDivider(
                            Modifier.padding(vertical = 14.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        SongInfoMetric(
                            label = stringResource(R.string.song_info_duration),
                            value = song.durationMillis.asDurationLabel(),
                            modifier = Modifier.weight(1f),
                        )
                        SongInfoMetric(
                            label = stringResource(R.string.song_info_quality),
                            value = song.quality.label(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SongInfoLink(
    text: String,
    onClick: () -> Unit,
    style: TextStyle,
    color: androidx.compose.ui.graphics.Color,
    maxLines: Int,
) {
    ResonotePlainAction(
        onClick = onClick,
    ) {
        Text(
            text = text,
            color = color,
            style = style,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SongInfoField(label: String, value: String, onClick: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        if (onClick == null) {
            Text(value, style = MaterialTheme.typography.bodyLarge)
        } else {
            SongInfoLink(
                text = value,
                onClick = onClick,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun SongInfoMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
    }
}

private fun Long.asDurationLabel(): String {
    val seconds = (coerceAtLeast(0) / 1_000).toInt()
    return String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60)
}

@Composable
private fun AudioQuality.label(): String = stringResource(
    when (this) {
        AudioQuality.Standard -> R.string.song_info_quality_standard
        AudioQuality.HighQuality -> R.string.song_info_quality_hq
        AudioQuality.HighResolution -> R.string.song_info_quality_hi_res
        AudioQuality.Lossless -> R.string.song_info_quality_lossless
    },
)

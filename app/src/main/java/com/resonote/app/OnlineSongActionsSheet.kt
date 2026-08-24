@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
        Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 28.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ResonoteRemoteArtwork(
                    model = song.coverUrl,
                    contentDescription = stringResource(R.string.song_info_artwork, song.title),
                    modifier = Modifier.size(84.dp).clip(RoundedCornerShape(18.dp)),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SongInfoLink(
                        text = song.title,
                        onClick = onSearchSong,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                    song.artist?.takeIf(String::isNotBlank)?.let { artist ->
                        onSearchArtist?.let { searchArtist ->
                            SongInfoLink(
                                text = artist,
                                onClick = searchArtist,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        } ?: Text(
                            text = artist,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.song_info_details),
                modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                song.albumTitle?.takeIf(String::isNotBlank)?.let {
                    SongInfoField(
                        label = stringResource(R.string.song_info_album),
                        value = it,
                        onClick = onSearchAlbum,
                    )
                }
                SongInfoField(
                    stringResource(R.string.song_info_duration),
                    song.durationMillis.asDurationLabel(),
                )
                SongInfoField(stringResource(R.string.song_info_quality), song.quality.label())
            }
        }
    }
}

@Composable
private fun SongInfoLink(text: String, onClick: () -> Unit, style: TextStyle) {
    val linkColor = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        Color(0xFF8AB4F8)
    } else {
        Color(0xFF1565C0)
    }
    ResonotePlainAction(
        onClick = onClick,
    ) {
        Text(text = text, color = linkColor, style = style, textDecoration = TextDecoration.Underline)
    }
}

@Composable
private fun SongInfoField(label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            label,
            modifier = Modifier.weight(0.35f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (onClick == null) {
            Text(value, modifier = Modifier.weight(0.65f), style = MaterialTheme.typography.bodyMedium)
        } else {
            Box(Modifier.weight(0.65f)) {
                SongInfoLink(
                    text = value,
                    onClick = onClick,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
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

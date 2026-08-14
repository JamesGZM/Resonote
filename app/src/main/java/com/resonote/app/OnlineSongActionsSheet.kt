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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    onShareUnavailable: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    request.song.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                request.song.artist?.takeIf(String::isNotBlank)?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            SongActionRow(Icons.Rounded.PlayArrow, R.string.song_action_play, onPlay)
            SongActionRow(Icons.Rounded.SkipNext, R.string.song_action_play_next, onPlayNext)
            SongActionRow(Icons.AutoMirrored.Rounded.PlaylistAdd, R.string.song_action_append_queue, onAppendToQueue)
            SongActionRow(Icons.Rounded.LibraryAdd, R.string.song_action_add_playlist, onAddToPlaylist)
            SongActionRow(Icons.Rounded.Info, R.string.song_action_info, onShowInfo)
            SongActionRow(
                icon = Icons.Rounded.Share,
                title = R.string.song_action_share,
                onClick = onShareUnavailable,
                supporting = stringResource(R.string.song_action_unavailable),
            )
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
internal fun OnlineSongInfoDialog(song: OnlineSong, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Info, contentDescription = null) },
        title = { Text(stringResource(R.string.song_info_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SongInfoField(stringResource(R.string.song_info_name), song.title)
                song.artist?.takeIf(String::isNotBlank)?.let {
                    SongInfoField(stringResource(R.string.song_info_artist), it)
                }
                song.albumTitle?.takeIf(String::isNotBlank)?.let {
                    SongInfoField(stringResource(R.string.song_info_album), it)
                }
                SongInfoField(
                    stringResource(R.string.song_info_duration),
                    song.durationMillis.asDurationLabel(),
                )
                SongInfoField(stringResource(R.string.song_info_quality), song.quality.label())
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.song_info_close)) }
        },
    )
}

@Composable
private fun SongInfoField(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            label,
            modifier = Modifier.weight(0.35f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(value, modifier = Modifier.weight(0.65f), style = MaterialTheme.typography.bodyMedium)
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

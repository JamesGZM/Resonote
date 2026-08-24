package com.resonote.feature.player.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.playback.PlaybackFormat
import com.resonote.core.playback.PlaybackMetadata

@Composable
internal fun PlayerTopBar(
    song: PlaybackMetadata,
    qualityLabel: String?,
    onBack: () -> Unit,
    onMore: () -> Unit,
    palette: PlayerPalette,
) {
    Row(
        Modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                stringResource(R.string.feature_player_impl_collapse),
                tint = palette.contentPrimary,
            )
        }
        Column(
            Modifier.weight(1f).padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    song.title,
                    Modifier.weight(1f, fill = false),
                    color = palette.contentPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                qualityLabel?.let { label ->
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = palette.accent.copy(alpha = 0.18f),
                        contentColor = palette.accent,
                        shape = RoundedCornerShape(5.dp),
                    ) {
                        Text(
                            label,
                            Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            }
            Text(
                song.artist.orEmpty().ifBlank { stringResource(R.string.feature_player_impl_unknown_artist) },
                color = palette.contentSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onMore) {
            Icon(
                Icons.Rounded.MoreVert,
                stringResource(R.string.feature_player_impl_more),
                tint = palette.contentPrimary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerActionsSheet(
    onDismiss: () -> Unit,
    onPlayNext: (() -> Unit)?,
    onAppendToQueue: (() -> Unit)?,
    onAddToPlaylist: (() -> Unit)?,
    onShowInfo: (() -> Unit)?,
    onLyricsSettings: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        onPlayNext?.let {
            PlayerActionRow(Icons.Rounded.SkipNext, stringResource(R.string.feature_player_impl_play_next)) {
                onDismiss()
                it()
            }
        }
        onAppendToQueue?.let {
            PlayerActionRow(
                Icons.AutoMirrored.Rounded.PlaylistAdd,
                stringResource(R.string.feature_player_impl_append_queue),
            ) {
                onDismiss()
                it()
            }
        }
        onAddToPlaylist?.let {
            PlayerActionRow(Icons.Rounded.LibraryAdd, stringResource(R.string.feature_player_impl_add_playlist)) {
                onDismiss()
                it()
            }
        }
        onShowInfo?.let {
            PlayerActionRow(Icons.Rounded.Info, stringResource(R.string.feature_player_impl_song_info)) {
                onDismiss()
                it()
            }
        }
        PlayerActionRow(Icons.Rounded.Lyrics, stringResource(R.string.feature_player_impl_lyrics_settings)) {
            onDismiss()
            onLyricsSettings()
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PlayerActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null)
        Text(title, Modifier.padding(start = 18.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaybackSpeedSheet(selected: PlaybackSpeed, onSelect: (PlaybackSpeed) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(R.string.feature_player_impl_playback_speed),
            Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        PlaybackSpeed.entries.forEach { speed ->
            Row(
                Modifier.fillMaxWidth().selectable(
                    selected = speed == selected,
                    role = Role.RadioButton,
                    onClick = { onSelect(speed) },
                ).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(speed == selected, null)
                Text(speed.label(), Modifier.padding(start = 12.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaybackFormatSheet(
    format: PlaybackFormat,
    selectedQuality: OnlinePlaybackQuality?,
    onSelect: (OnlinePlaybackQuality) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(R.string.feature_player_impl_current_format),
            Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        if (format is PlaybackFormat.Online) {
            val selected = selectedQuality ?: format.defaultOnlineQuality()
            OnlinePlaybackQuality.entries.forEach { quality ->
                Row(
                    Modifier.fillMaxWidth().selectable(
                        selected = quality == selected,
                        role = Role.RadioButton,
                        onClick = { onSelect(quality) },
                    ).padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(quality == selected, null)
                    Text(quality.label(), Modifier.padding(start = 12.dp))
                }
            }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = true, onClick = null)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(format.toolLabel(), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.feature_player_impl_format_read_only),
                        Modifier.padding(top = 3.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

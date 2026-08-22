package com.resonote.feature.player.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resonote.core.model.PlaybackSpeed

@Composable
internal fun playerBackdrop(): Brush {
    val colors = MaterialTheme.colorScheme
    return Brush.verticalGradient(
        0f to colors.surface,
        0.45f to colors.surfaceContainerLow,
        1f to colors.primaryContainer.copy(alpha = 0.46f),
    )
}

@Composable
internal fun PlayerTopBar(
    onBack: () -> Unit,
    menuOpen: Boolean,
    onMenuChange: (Boolean) -> Unit,
    onSongMoreClick: (() -> Unit)?,
    playbackSpeed: PlaybackSpeed,
    onOpenSpeed: () -> Unit,
    onShare: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.feature_player_impl_collapse))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.feature_player_impl_now_playing),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Box(
                Modifier
                    .padding(top = 5.dp)
                    .width(28.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Box {
            IconButton(onClick = { onMenuChange(true) }) {
                Icon(Icons.Rounded.MoreVert, stringResource(R.string.feature_player_impl_more))
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { onMenuChange(false) },
            ) {
                if (onSongMoreClick != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.feature_player_impl_song_actions)) },
                        leadingIcon = { Icon(Icons.Rounded.MoreVert, contentDescription = null) },
                        onClick = {
                            onMenuChange(false)
                            onSongMoreClick()
                        },
                    )
                }
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(stringResource(R.string.feature_player_impl_playback_speed))
                            Text(
                                playbackSpeed.label(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    },
                    leadingIcon = { Icon(Icons.Rounded.Speed, contentDescription = null) },
                    onClick = onOpenSpeed,
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.feature_player_impl_share)) },
                    leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                    onClick = onShare,
                )
            }
        }
    }
}

@Composable
internal fun PlaybackSpeedDialog(selected: PlaybackSpeed, onSelect: (PlaybackSpeed) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feature_player_impl_playback_speed)) },
        text = {
            Column {
                PlaybackSpeed.entries.forEach { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = speed == selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(speed) },
                            )
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = speed == selected, onClick = null)
                        Text(speed.label(), modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.feature_player_impl_cancel))
            }
        },
    )
}

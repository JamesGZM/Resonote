package com.resonote.feature.player.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteBottomSheet
import com.resonote.core.designsystem.component.ResonoteBottomSheetHeader
import com.resonote.core.designsystem.component.ResonoteIconButton
import com.resonote.core.designsystem.component.ResonoteMusicItem
import com.resonote.core.model.PlaybackMode
import com.resonote.core.playback.PlaybackState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackQueueSheet(
    playback: PlaybackState,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
    onModeChange: (PlaybackMode) -> Unit,
    snackbarHost: @Composable () -> Unit = {},
) {
    ResonoteBottomSheet(onDismissRequest = onDismiss) {
        QueueSheetContent(
            playback = playback,
            onSelect = onSelect,
            onRemove = onRemove,
            onClear = onClear,
            onModeChange = onModeChange,
        )
        snackbarHost()
    }
}

@Composable
private fun QueueSheetContent(
    playback: PlaybackState,
    onSelect: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
    onModeChange: (PlaybackMode) -> Unit,
) {
    val modeLabel = playback.mode.queueModeLabel()
    val statusSlotWidth = with(LocalDensity.current) {
        when {
            fontScale >= 1.75f -> 56.dp
            fontScale >= 1.25f -> 48.dp
            else -> 44.dp
        }
    }
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        ResonoteBottomSheetHeader(
            title = stringResource(R.string.feature_player_impl_queue_title),
            subtitle = stringResource(
                R.string.feature_player_impl_queue_subtitle,
                modeLabel,
                playback.queue.size,
            ),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                Box(modifier = Modifier.width(statusSlotWidth), contentAlignment = Alignment.Center) {
                    ResonoteIconButton(
                        label = stringResource(R.string.feature_player_impl_queue_mode_action, modeLabel),
                        onClick = { onModeChange(playback.mode.nextQueueMode()) },
                        icon = { Icon(playback.mode.queueModeIcon(), contentDescription = null) },
                    )
                }
                ResonoteIconButton(
                    label = stringResource(R.string.feature_player_impl_clear),
                    onClick = onClear,
                    enabled = playback.queue.isNotEmpty(),
                    icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        if (playback.queue.isEmpty()) {
            Text(
                stringResource(R.string.feature_player_impl_empty_queue),
                modifier = Modifier.padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                itemsIndexed(playback.queue, key = { _, item -> item.queueKey }) { index, item ->
                    val selected = index == playback.currentIndex
                    ResonoteMusicItem(
                        title = item.metadata.title,
                        supportingText = item.metadata.artist.orEmpty().ifBlank {
                            stringResource(R.string.feature_player_impl_unknown_artist)
                        },
                        duration = item.metadata.durationMillis.timeLabel(),
                        artworkUrl = item.metadata.artworkUri,
                        qualityLabel = item.metadata.format.badgeLabel(),
                        isVip = item.metadata.isVip,
                        isPlaying = selected,
                        isSelected = selected,
                        onClick = { onSelect(index) },
                        onMoreClick = null,
                        trailingAction = {
                            ResonoteIconButton(
                                label = stringResource(
                                    R.string.feature_player_impl_remove,
                                    item.metadata.title,
                                ),
                                onClick = { onRemove(index) },
                                icon = { Icon(Icons.Rounded.Close, contentDescription = null) },
                            )
                        },
                    )
                }
            }
        }
    }
}

private fun PlaybackMode.nextQueueMode(): PlaybackMode = when (this) {
    PlaybackMode.ListLoop -> PlaybackMode.Shuffle
    PlaybackMode.Shuffle -> PlaybackMode.SingleLoop
    PlaybackMode.SingleLoop -> PlaybackMode.Sequential
    PlaybackMode.Sequential -> PlaybackMode.ListLoop
}

private fun PlaybackMode.queueModeIcon() = when (this) {
    PlaybackMode.ListLoop -> Icons.Rounded.Repeat
    PlaybackMode.Shuffle -> Icons.Rounded.Shuffle
    PlaybackMode.SingleLoop -> Icons.Rounded.RepeatOne
    PlaybackMode.Sequential -> Icons.AutoMirrored.Rounded.PlaylistPlay
}

@Composable
private fun PlaybackMode.queueModeLabel(): String = stringResource(
    when (this) {
        PlaybackMode.ListLoop -> R.string.feature_player_impl_mode_list_loop
        PlaybackMode.Shuffle -> R.string.feature_player_impl_mode_shuffle
        PlaybackMode.SingleLoop -> R.string.feature_player_impl_mode_single_loop
        PlaybackMode.Sequential -> R.string.feature_player_impl_mode_sequential
    },
)

private fun Long.timeLabel(): String {
    val totalSeconds = coerceAtLeast(0) / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

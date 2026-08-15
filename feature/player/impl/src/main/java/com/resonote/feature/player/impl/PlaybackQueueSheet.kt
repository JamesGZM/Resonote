package com.resonote.feature.player.impl

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resonote.core.playback.PlaybackMode
import com.resonote.core.playback.PlaybackState
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackQueueSheet(
    playback: PlaybackState,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onClear: () -> Unit,
    onModeChange: (PlaybackMode) -> Unit,
    snackbarHost: @Composable () -> Unit = {},
) {
    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        ModalBottomSheet(onDismissRequest = onDismiss) {
            CompositionLocalProvider(LocalRippleConfiguration provides RippleConfiguration()) {
                QueueSheetContent(
                    playback = playback,
                    onSelect = onSelect,
                    onRemove = onRemove,
                    onMove = onMove,
                    onClear = onClear,
                    onModeChange = onModeChange,
                )
                snackbarHost()
            }
        }
    }
}

@Composable
private fun QueueSheetContent(
    playback: PlaybackState,
    onSelect: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onClear: () -> Unit,
    onModeChange: (PlaybackMode) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.feature_player_impl_queue_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.feature_player_impl_queue_count, playback.queue.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { onModeChange(playback.mode.nextQueueMode()) }) {
                Text(playback.mode.queueModeLabel())
            }
            TextButton(onClick = onClear, enabled = playback.queue.isNotEmpty()) {
                Text(stringResource(R.string.feature_player_impl_clear))
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
            LazyColumn {
                itemsIndexed(playback.queue, key = { _, item -> item.queueKey }) { index, item ->
                    val selected = index == playback.currentIndex
                    QueueItem(
                        title = item.metadata.title,
                        artist = item.metadata.artist.orEmpty(),
                        selected = selected,
                        index = index,
                        lastIndex = playback.queue.lastIndex,
                        onSelect = { onSelect(index) },
                        onRemove = { onRemove(index) },
                        onMove = onMove,
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueItem(
    title: String,
    artist: String,
    selected: Boolean,
    index: Int,
    lastIndex: Int,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    val rowHeightPx = with(LocalDensity.current) { 72.dp.toPx() }
    var draggedIndex by remember(index) { mutableIntStateOf(index) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val moveUpLabel = stringResource(R.string.feature_player_impl_move_up)
    val moveDownLabel = stringResource(R.string.feature_player_impl_move_down)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                customActions = buildList {
                    if (index > 0) {
                        add(
                            CustomAccessibilityAction(moveUpLabel) {
                                onMove(index, index - 1)
                                true
                            },
                        )
                    }
                    if (index < lastIndex) {
                        add(
                            CustomAccessibilityAction(moveDownLabel) {
                                onMove(index, index + 1)
                                true
                            },
                        )
                    }
                }
            },
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.58f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        onClick = onSelect,
    ) {
        ListItem(
            headlineContent = {
                Text(
                    title,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Text(
                    if (selected) stringResource(R.string.feature_player_impl_now_playing_artist, artist) else artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingContent = {
                Icon(
                    Icons.Rounded.DragHandle,
                    stringResource(R.string.feature_player_impl_reorder),
                    modifier = Modifier
                        .size(48.dp)
                        .padding(12.dp)
                        .pointerInput(index, lastIndex) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedIndex = index
                                    dragOffset = 0f
                                },
                                onDragEnd = { dragOffset = 0f },
                                onDragCancel = { dragOffset = 0f },
                            ) { change, amount ->
                                change.consume()
                                dragOffset += amount.y
                                if (abs(dragOffset) >= rowHeightPx) {
                                    val target = (draggedIndex + if (dragOffset > 0) 1 else -1).coerceIn(0, lastIndex)
                                    if (target != draggedIndex) {
                                        onMove(draggedIndex, target)
                                        draggedIndex = target
                                    }
                                    dragOffset = 0f
                                }
                            }
                        },
                )
            },
            trailingContent = {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.feature_player_impl_remove, title))
                }
            },
            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        )
    }
}

private fun PlaybackMode.nextQueueMode(): PlaybackMode = when (this) {
    PlaybackMode.ListLoop -> PlaybackMode.Shuffle
    PlaybackMode.Shuffle -> PlaybackMode.SingleLoop
    PlaybackMode.SingleLoop -> PlaybackMode.Sequential
    PlaybackMode.Sequential -> PlaybackMode.ListLoop
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

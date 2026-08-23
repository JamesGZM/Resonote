@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.history.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.resonote.core.designsystem.component.ResonoteMusicItem
import com.resonote.core.model.DeviceHistoryItem
import com.resonote.core.model.DeviceHistorySource
import com.resonote.core.model.OnlineSong

internal fun androidx.compose.foundation.lazy.LazyListScope.onlineContent(
    state: HistoryUiState,
    playingMediaId: String?,
    onLoginRequest: () -> Unit,
    onRetry: () -> Unit,
    onPlay: (List<OnlineSong>, Int) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
) {
    when (val section = state.online) {
        OnlineHistoryUiState.NotLoaded -> item(key = "online-signed-out") {
            if (state.accountState == HistoryAccountState.Checking) {
                LoadingState(R.string.feature_history_impl_online_loading)
            } else {
                SignedOutState(onLoginRequest)
            }
        }
        OnlineHistoryUiState.Loading -> item(key = "online-loading") {
            LoadingState(R.string.feature_history_impl_online_loading)
        }
        is OnlineHistoryUiState.Failed -> item(key = "online-failure") {
            OnlineFailureState(
                failure = section.failure,
                onRetry = onRetry,
                modifier = Modifier.fillParentMaxHeight(0.55f),
            )
        }
        is OnlineHistoryUiState.Available -> if (section.songs.isEmpty()) {
            item(key = "online-empty") {
                EmptyState(
                    title = stringResource(R.string.feature_history_impl_online_empty_title),
                    body = stringResource(R.string.feature_history_impl_online_empty_body),
                    modifier = Modifier.fillParentMaxHeight(0.55f),
                )
            }
        } else {
            itemsIndexed(section.songs, key = { index, song -> "online:${song.hash}:$index" }) { index, song ->
                OnlineHistoryRow(
                    song = song,
                    isPlaying = playingMediaId == song.hash,
                    onClick = { onPlay(section.songs, index) },
                    onMoreClick = onSongMoreClick?.let { callback -> { callback(song) } },
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}

internal fun androidx.compose.foundation.lazy.LazyListScope.deviceContent(
    state: HistoryUiState,
    playingMediaId: String?,
    busy: Boolean,
    onPlay: (List<DeviceHistoryItem>, Int) -> Unit,
    onDelete: (DeviceHistoryItem) -> Unit,
) {
    when {
        state.deviceLoading -> item(key = "device-loading") {
            LoadingState(R.string.feature_history_impl_device_loading)
        }
        state.deviceLoadFailed -> item(key = "device-failure") {
            DeviceFailureState(modifier = Modifier.fillParentMaxHeight(0.55f))
        }
        state.deviceItems.isEmpty() -> item(key = "device-empty") {
            EmptyState(
                title = stringResource(R.string.feature_history_impl_device_empty_title),
                body = stringResource(R.string.feature_history_impl_device_empty_body),
                modifier = Modifier.fillParentMaxHeight(0.55f),
            )
        }
        else -> itemsIndexed(
            state.deviceItems,
            key = { index, item -> "${item.record.source}:${item.record.mediaId}:$index" },
        ) { index, item ->
            DeviceHistoryRow(
                item = item,
                isPlaying = playingMediaId == item.record.mediaId,
                enabled = !busy,
                onClick = { onPlay(state.deviceItems, index) },
                onDelete = { onDelete(item) },
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

@Composable
internal fun OnlineHistoryRow(
    song: OnlineSong,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val artist = song.artist.orEmpty().ifBlank { stringResource(R.string.feature_history_impl_unknown_artist) }
    val album = song.albumTitle?.takeIf(String::isNotBlank)?.let {
        stringResource(R.string.feature_history_impl_album_separator, it)
    }.orEmpty()
    ResonoteMusicItem(
        title = song.title,
        supportingText = stringResource(R.string.feature_history_impl_online_supporting, artist, album),
        duration = song.durationMillis.durationLabel(),
        onClick = onClick,
        onMoreClick = onMoreClick,
        modifier = modifier,
        qualityLabel = song.quality.label(),
        isVip = song.vip,
        isPlaying = isPlaying,
        artwork = { HistoryArtwork(song.coverUrl, song.title) },
    )
}

@Composable
internal fun DeviceHistoryRow(
    item: DeviceHistoryItem,
    isPlaying: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val record = item.record
    val source = stringResource(
        if (record.source == DeviceHistorySource.Local) {
            R.string.feature_history_impl_source_local
        } else {
            R.string.feature_history_impl_source_cloud
        },
    )
    val artist = record.artist.orEmpty().ifBlank { stringResource(R.string.feature_history_impl_unknown_artist) }
    ResonoteMusicItem(
        title = record.title,
        supportingText = stringResource(
            R.string.feature_history_impl_device_supporting,
            source,
            artist,
            item.playCount,
        ),
        duration = record.durationMillis.durationLabel(),
        onClick = onClick,
        onMoreClick = onDelete,
        modifier = modifier,
        isPlaying = isPlaying,
        enabled = enabled,
        artwork = { HistoryArtwork(record.artworkUri, record.title, record.source) },
    )
}

@Composable
internal fun HistoryArtwork(model: String?, title: String, source: DeviceHistorySource? = null) {
    val containerColor = when (source) {
        DeviceHistorySource.Local -> MaterialTheme.colorScheme.secondaryContainer
        DeviceHistorySource.Cloud -> MaterialTheme.colorScheme.tertiaryContainer
        null -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when (source) {
        DeviceHistorySource.Local -> MaterialTheme.colorScheme.onSecondaryContainer
        DeviceHistorySource.Cloud -> MaterialTheme.colorScheme.onTertiaryContainer
        null -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Box(
        modifier = Modifier.fillMaxSize().background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = when (source) {
                DeviceHistorySource.Local -> Icons.Rounded.LibraryMusic
                DeviceHistorySource.Cloud -> Icons.Rounded.Cloud
                null -> Icons.Rounded.MusicNote
            },
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp),
        )
        model?.let {
            AsyncImage(
                model = it,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

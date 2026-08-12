@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.playlist.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.ResonoteDestructiveButton
import com.resonote.core.designsystem.component.ResonoteMusicItem
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaylistDetails
import com.resonote.feature.playlist.api.PlaylistNavKey

typealias PlaylistSongMoreAction = (OnlineSong, (() -> Unit)?) -> Unit

@Composable
fun PlaylistRoute(
    key: PlaylistNavKey,
    playingMediaId: String?,
    currentAccountId: String?,
    onBack: () -> Unit,
    onPlayAll: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: PlaylistSongMoreAction?,
    viewModel: PlaylistViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val writableListId = key.writableListId.takeIf { key.writableAccountId == currentAccountId }
    LaunchedEffect(key.playlistId, writableListId, currentAccountId) {
        viewModel.load(key.playlistId, writableListId, currentAccountId)
    }
    LaunchedEffect(currentAccountId) {
        val error = viewModel.uiState.value as? PlaylistUiState.Error
        if (currentAccountId != null && error?.failure == ContentFailure.AuthenticationRequired) viewModel.retry()
    }
    PlaylistScreen(
        state = state,
        playingMediaId = playingMediaId,
        onBack = onBack,
        onRetry = viewModel::retry,
        onLoadMore = viewModel::loadMore,
        onPlayAll = onPlayAll,
        onSongClick = onSongClick,
        onSongMoreClick = onSongMoreClick,
        onRemoveSong = viewModel::removeSong,
        onDismissRemovalFailure = viewModel::dismissRemovalFailure,
        onAcknowledgeRemoval = viewModel::acknowledgeRemoval,
    )
}

@Composable
fun PlaylistScreen(
    state: PlaylistUiState,
    playingMediaId: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onPlayAll: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: PlaylistSongMoreAction?,
    modifier: Modifier = Modifier,
    onRemoveSong: (OnlineSong) -> Unit = {},
    onDismissRemovalFailure: () -> Unit = {},
    onAcknowledgeRemoval: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingRemovalHash by rememberSaveable { mutableStateOf<String?>(null) }
    val content = state as? PlaylistUiState.Content
    val removal = content?.removal
    val removedMessage = (removal as? PlaylistRemovalUiState.Removed)?.let {
        stringResource(R.string.feature_playlist_impl_remove_success, it.title)
    }

    LaunchedEffect(content?.writableListId) {
        if (content?.writableListId == null) pendingRemovalHash = null
    }
    LaunchedEffect(removedMessage) {
        if (removedMessage != null) {
            pendingRemovalHash = null
            snackbarHostState.showSnackbar(removedMessage)
            onAcknowledgeRemoval()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ResonoteTopAppBar(
                title = {
                    Text(
                        text = (state as? PlaylistUiState.Content)?.details?.title
                            ?: stringResource(R.string.playlist_title_fallback),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.playlist_back))
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            PlaylistUiState.Loading -> LoadingState(Modifier.padding(padding))
            PlaylistUiState.Empty -> MessageState(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                title = stringResource(R.string.playlist_empty_title),
                body = stringResource(R.string.playlist_empty_body),
                modifier = Modifier.padding(padding),
            )
            is PlaylistUiState.Error -> ErrorState(state.failure, onRetry, Modifier.padding(padding))
            is PlaylistUiState.Content -> PlaylistContent(
                state = state,
                playingMediaId = playingMediaId,
                onLoadMore = onLoadMore,
                onPlayAll = onPlayAll,
                onSongClick = onSongClick,
                onSongMoreClick = onSongMoreClick,
                onRemoveRequest = { pendingRemovalHash = it.hash },
                modifier = Modifier.padding(padding),
            )
        }
    }

    val pendingSong = content?.songs?.firstOrNull { it.hash == pendingRemovalHash }
    if (pendingSong != null && content.writableListId != null) {
        val removing = removal == PlaylistRemovalUiState.Removing(pendingSong.hash)
        val failure = (removal as? PlaylistRemovalUiState.Failed)
            ?.takeIf { it.songHash == pendingSong.hash }
            ?.failure
        RemoveSongDialog(
            song = pendingSong,
            removing = removing,
            failure = failure,
            onDismiss = {
                if (!removing) {
                    pendingRemovalHash = null
                    onDismissRemovalFailure()
                }
            },
            onConfirm = { onRemoveSong(pendingSong) },
        )
    }
}

@Composable
private fun PlaylistContent(
    state: PlaylistUiState.Content,
    playingMediaId: String?,
    onLoadMore: () -> Unit,
    onPlayAll: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: PlaylistSongMoreAction?,
    onRemoveRequest: (OnlineSong) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("playlist-list"),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item(key = "header") {
            PlaylistHeader(
                details = state.details,
                loadedSongCount = state.songs.size,
                canPlay = state.songs.isNotEmpty(),
                onPlayAll = { onPlayAll(state.songs) },
            )
        }
        items(state.songs, key = { "song-${it.hash}" }) { song ->
            val canRemove = state.writableListId != null &&
                !song.fileId.isNullOrBlank() &&
                !state.isLoadingMore &&
                state.removal !is PlaylistRemovalUiState.Removing
            val removeRequest: (() -> Unit)? = if (canRemove) {
                { onRemoveRequest(song) }
            } else {
                null
            }
            ResonoteMusicItem(
                title = song.title,
                supportingText = song.artist.orEmpty(),
                duration = song.durationMillis.durationLabel(),
                qualityLabel = song.quality.label(),
                isVip = song.vip,
                isPlaying = song.hash == playingMediaId,
                artworkState = ResonoteArtworkState.MISSING,
                onClick = { onSongClick(song) },
                onMoreClick = when {
                    onSongMoreClick != null -> ({ onSongMoreClick(song, removeRequest) })
                    removeRequest != null -> removeRequest
                    else -> null
                },
            )
        }
        if (state.hasMore || state.isLoadingMore || state.loadMoreFailure != null) {
            item(key = "load-more") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        state.isLoadingMore -> CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        state.loadMoreFailure != null -> TextButton(onClick = onLoadMore) {
                            Text(stringResource(R.string.playlist_load_more_retry))
                        }
                        state.hasMore -> TextButton(onClick = onLoadMore) {
                            Text(stringResource(R.string.playlist_load_more))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoveSongDialog(
    song: OnlineSong,
    removing: Boolean,
    failure: ContentFailure?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
        title = { Text(stringResource(R.string.feature_playlist_impl_remove_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.feature_playlist_impl_remove_body, song.title))
                failure?.let {
                    Text(
                        text = removalFailureMessage(it),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !removing) {
                Text(stringResource(R.string.feature_playlist_impl_remove_cancel))
            }
        },
        confirmButton = {
            ResonoteDestructiveButton(
                label = stringResource(R.string.feature_playlist_impl_remove_confirm),
                loadingLabel = stringResource(R.string.feature_playlist_impl_removing),
                loading = removing,
                enabled = !removing,
                onClick = onConfirm,
            )
        },
    )
}

@Composable
private fun removalFailureMessage(failure: ContentFailure): String = when (failure) {
    ContentFailure.AuthenticationRequired -> stringResource(R.string.feature_playlist_impl_remove_error_auth)
    ContentFailure.Network -> stringResource(R.string.feature_playlist_impl_remove_error_network)
    ContentFailure.ServiceRejected -> stringResource(R.string.feature_playlist_impl_remove_error_service)
    is ContentFailure.RiskVerificationRequired,
    ContentFailure.RiskBlocked,
    -> stringResource(R.string.feature_playlist_impl_remove_error_risk)
    ContentFailure.Protocol -> stringResource(R.string.feature_playlist_impl_remove_error_protocol)
}

@Composable
private fun PlaylistHeader(
    details: PlaylistDetails?,
    loadedSongCount: Int,
    canPlay: Boolean,
    onPlayAll: () -> Unit,
) {
    val title = details?.title ?: stringResource(R.string.playlist_title_fallback)
    val artworkDescription = stringResource(R.string.playlist_artwork, title)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(playlistGradient(details?.id ?: title))
                    .semantics { contentDescription = artworkDescription },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.White.copy(alpha = 0.9f),
                )
            }
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.playlist_song_count, details?.songCount ?: loadedSongCount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        details?.description?.takeIf { it.isNotBlank() }?.let { description ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    description,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Button(
            onClick = onPlayAll,
            enabled = canPlay,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 14.dp),
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.playlist_play_all))
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun ErrorState(failure: ContentFailure, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val body = when (failure) {
        ContentFailure.Network -> stringResource(R.string.playlist_error_network)
        ContentFailure.AuthenticationRequired -> stringResource(R.string.playlist_error_auth)
        else -> stringResource(R.string.playlist_error_generic)
    }
    MessageState(
        icon = Icons.AutoMirrored.Rounded.QueueMusic,
        title = stringResource(R.string.playlist_error_title),
        body = body,
        modifier = modifier,
        action = { Button(onClick = onRetry) { Text(stringResource(R.string.playlist_retry)) } },
    )
}

@Composable
private fun MessageState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.secondaryContainer) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(20.dp).size(36.dp))
            }
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            action?.invoke()
        }
    }
}

private fun playlistGradient(seed: String): Brush {
    val palettes = listOf(
        listOf(Color(0xFF5A061B), Color(0xFFE31353), Color(0xFFFF8DA9)),
        listOf(Color(0xFF042E48), Color(0xFF0879BC), Color(0xFFBBD9F4)),
        listOf(Color(0xFF20164B), Color(0xFF786EDB), Color(0xFFF4A9BC)),
        listOf(Color(0xFF123D36), Color(0xFF3A8068), Color(0xFFC6D9A8)),
    )
    return Brush.linearGradient(palettes[(seed.hashCode() and Int.MAX_VALUE) % palettes.size])
}

private fun Long.durationLabel(): String {
    val totalSeconds = coerceAtLeast(0) / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun AudioQuality.label(): String? = when (this) {
    AudioQuality.Standard -> null
    AudioQuality.HighQuality -> "HQ"
    AudioQuality.HighResolution -> "HI-RES"
    AudioQuality.Lossless -> "LOSSLESS"
}

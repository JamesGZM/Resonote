@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.playlist.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.LocalResonoteSnackbarController
import com.resonote.core.designsystem.component.ResonoteEmptyState
import com.resonote.core.designsystem.component.ResonoteErrorState
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong
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
    bottomContentPadding: Dp = 32.dp,
    viewModel: PlaylistViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarController = LocalResonoteSnackbarController.current
    val refreshFailureMessage = stringResource(R.string.feature_playlist_impl_playlist_refresh_failed)
    val refreshFailure = (state as? PlaylistUiState.Content)?.refreshFailure
    val writableListId = key.writableListId.takeIf { key.writableAccountId == currentAccountId }
    LaunchedEffect(key.playlistId, writableListId, currentAccountId) {
        viewModel.load(key.playlistId, writableListId, currentAccountId)
    }
    LaunchedEffect(currentAccountId) {
        val error = viewModel.uiState.value as? PlaylistUiState.Error
        if (currentAccountId != null && error?.failure == ContentFailure.AuthenticationRequired) viewModel.retry()
    }
    LaunchedEffect(refreshFailure, snackbarController) {
        if (refreshFailure != null) {
            snackbarController?.show(refreshFailureMessage)
            viewModel.acknowledgeRefreshFailure()
        }
    }
    PlaylistScreen(
        state = state,
        initialDetails = key.toInitialDetails(),
        heroPlaylistId = key.playlistId,
        playingMediaId = playingMediaId,
        onBack = onBack,
        onRetry = viewModel::retry,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onPlayAll = onPlayAll,
        onSongClick = onSongClick,
        onSongMoreClick = onSongMoreClick,
        bottomContentPadding = bottomContentPadding,
        onRemoveSong = viewModel::removeSong,
        onDismissRemovalFailure = viewModel::dismissRemovalFailure,
        onAcknowledgeRemoval = viewModel::acknowledgeRemoval,
    )
}

@Composable
fun PlaylistScreen(
    state: PlaylistUiState,
    initialDetails: com.resonote.core.model.PlaylistDetails? = null,
    heroPlaylistId: String? = initialDetails?.id,
    playingMediaId: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onPlayAll: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: PlaylistSongMoreAction?,
    bottomContentPadding: Dp = 32.dp,
    modifier: Modifier = Modifier,
    onRemoveSong: (OnlineSong) -> Unit = {},
    onDismissRemovalFailure: () -> Unit = {},
    onAcknowledgeRemoval: () -> Unit = {},
) {
    val snackbarController = LocalResonoteSnackbarController.current
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
            snackbarController?.show(removedMessage)
            onAcknowledgeRemoval()
        }
    }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val playlistContent = state as? PlaylistUiState.Content
        if (state is PlaylistUiState.Loading || playlistContent != null) {
            val displayDetails = playlistContent?.details.mergeInitial(initialDetails)
            PlaylistContentLayout(
                state = playlistContent,
                details = displayDetails,
                heroPlaylistId = heroPlaylistId,
                playingMediaId = playingMediaId,
                onBack = onBack,
                onRefresh = onRefresh,
                onLoadMore = onLoadMore,
                onPlayAll = onPlayAll,
                onSongClick = onSongClick,
                onSongMoreClick = onSongMoreClick,
                bottomContentPadding = bottomContentPadding,
                onRemoveRequest = { pendingRemovalHash = it.hash },
            )
        } else if (state is PlaylistUiState.Empty) {
            StandardStateScaffold(
                title = stringResource(R.string.feature_playlist_impl_playlist_title_fallback),
                onBack = onBack,
            ) { padding ->
                ResonoteEmptyState(
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    title = stringResource(R.string.feature_playlist_impl_playlist_empty_title),
                    message = stringResource(R.string.feature_playlist_impl_playlist_empty_body),
                    modifier = Modifier.padding(padding),
                )
            }
        } else {
            val failure = (state as? PlaylistUiState.Error)?.failure ?: ContentFailure.Protocol
            StandardStateScaffold(
                title = stringResource(R.string.feature_playlist_impl_playlist_title_fallback),
                onBack = onBack,
            ) { padding ->
                ResonoteErrorState(
                    onRetry = onRetry,
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    title = stringResource(R.string.feature_playlist_impl_playlist_error_title),
                    message = failure.errorMessage(),
                    modifier = Modifier.padding(padding),
                    retryLabel = stringResource(R.string.feature_playlist_impl_playlist_retry),
                )
            }
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

private fun PlaylistNavKey.toInitialDetails(): com.resonote.core.model.PlaylistDetails? {
    val initialTitle = title?.takeIf(String::isNotBlank) ?: return null
    return com.resonote.core.model.PlaylistDetails(
        id = playlistId,
        title = initialTitle,
        description = "",
        coverUrl = coverUrl?.takeIf(String::isNotBlank),
        songCount = 0,
    )
}

private fun com.resonote.core.model.PlaylistDetails?.mergeInitial(
    initial: com.resonote.core.model.PlaylistDetails?,
): com.resonote.core.model.PlaylistDetails? {
    val loaded = this ?: return initial
    if (initial == null) return loaded
    return loaded.copy(
        title = loaded.title.takeIf(String::isNotBlank) ?: initial.title,
        coverUrl = initial.coverUrl ?: loaded.coverUrl,
    )
}

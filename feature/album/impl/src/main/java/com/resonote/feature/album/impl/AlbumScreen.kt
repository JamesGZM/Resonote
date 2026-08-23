@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.album.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.resonote.feature.album.api.AlbumNavKey

@Composable
fun AlbumRoute(
    key: AlbumNavKey,
    playingMediaId: String?,
    onBack: () -> Unit,
    onPlayAll: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    bottomContentPadding: Dp = 32.dp,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarController = LocalResonoteSnackbarController.current
    val refreshFailureMessage = stringResource(R.string.feature_album_impl_album_refresh_failed)
    val refreshFailure = (state as? AlbumUiState.Content)?.refreshFailure
    LaunchedEffect(key) { viewModel.load(key) }
    LaunchedEffect(refreshFailure, snackbarController) {
        if (refreshFailure != null) {
            snackbarController?.show(refreshFailureMessage)
            viewModel.acknowledgeRefreshFailure()
        }
    }
    AlbumScreen(
        state = state,
        initialMetadata = key.toInitialMetadata(),
        playingMediaId = playingMediaId,
        onBack = onBack,
        onRetry = viewModel::retry,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onPlayAll = onPlayAll,
        onSongClick = onSongClick,
        onSongMoreClick = onSongMoreClick,
        bottomContentPadding = bottomContentPadding,
    )
}

@Composable
fun AlbumScreen(
    state: AlbumUiState,
    initialMetadata: AlbumMetadata? = null,
    playingMediaId: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onPlayAll: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    bottomContentPadding: Dp = 32.dp,
    modifier: Modifier = Modifier,
) {
    val fallbackTitle = stringResource(R.string.feature_album_impl_album_title_fallback)
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val albumContent = state as? AlbumUiState.Content
        if (state is AlbumUiState.Loading || albumContent != null) {
            val metadata = albumContent?.metadata.mergeInitial(initialMetadata)
                ?: AlbumMetadata("", null, null, null, null, null)
            AlbumContentLayout(
                state = albumContent,
                metadata = metadata,
                playingMediaId = playingMediaId,
                onBack = onBack,
                onRefresh = onRefresh,
                onLoadMore = onLoadMore,
                onPlayAll = onPlayAll,
                onSongClick = onSongClick,
                onSongMoreClick = onSongMoreClick,
                bottomContentPadding = bottomContentPadding,
            )
        } else if (state is AlbumUiState.Empty) {
            val title = (state as? AlbumUiState.Empty)?.metadata?.title ?: fallbackTitle
            StandardStateScaffold(title = title, onBack = onBack) { padding ->
                ResonoteEmptyState(
                    icon = Icons.Rounded.Album,
                    title = stringResource(R.string.feature_album_impl_album_empty_title),
                    message = stringResource(R.string.feature_album_impl_album_empty_body),
                    modifier = Modifier.padding(padding),
                )
            }
        } else {
            val error = state as? AlbumUiState.Error
            StandardStateScaffold(title = error?.title ?: fallbackTitle, onBack = onBack) { padding ->
                ResonoteErrorState(
                    onRetry = onRetry,
                    icon = Icons.Rounded.Album,
                    title = stringResource(R.string.feature_album_impl_album_error_title),
                    message = (error?.failure ?: ContentFailure.Protocol).errorMessage(),
                    modifier = Modifier.padding(padding),
                    retryLabel = stringResource(R.string.feature_album_impl_album_retry),
                )
            }
        }
    }
}

private fun AlbumNavKey.toInitialMetadata() = AlbumMetadata(
    id = albumId,
    title = name?.takeIf(String::isNotBlank),
    artist = artist?.takeIf(String::isNotBlank),
    coverUrl = coverUrl?.takeIf(String::isNotBlank),
    publishDate = publishDate?.takeIf(String::isNotBlank),
    songCount = songCount,
)

private fun AlbumMetadata?.mergeInitial(initial: AlbumMetadata?): AlbumMetadata? {
    val loaded = this ?: return initial
    if (initial == null) return loaded
    return loaded.copy(
        title = loaded.title ?: initial.title,
        artist = loaded.artist ?: initial.artist,
        coverUrl = initial.coverUrl ?: loaded.coverUrl,
        publishDate = loaded.publishDate ?: initial.publishDate,
        songCount = loaded.songCount ?: initial.songCount,
    )
}

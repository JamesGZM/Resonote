package com.resonote.feature.home.impl

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.LocalResonoteSnackbarController
import com.resonote.core.designsystem.component.ResonoteContentPhase
import com.resonote.core.designsystem.component.ResonoteContentStateLayout
import com.resonote.core.designsystem.component.ResonoteErrorState
import com.resonote.core.model.OnlineSong
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    playingMediaId: String?,
    bottomContentPadding: Dp,
    onSearchClick: () -> Unit,
    onRecognitionClick: () -> Unit,
    onPlay: (HomePlaybackRequest) -> Unit,
    onOpenRankings: () -> Unit,
    onOpenFeaturedPlaylists: () -> Unit,
    onSongMoreClick: (OnlineSong) -> Unit,
    onPlaylistClick: (HomePlaylistUiModel) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val snackbarController = LocalResonoteSnackbarController.current
    val refreshFailureMessage = stringResource(R.string.feature_home_impl_refresh_failed)
    LaunchedEffect(viewModel, snackbarController) {
        viewModel.refreshFailures.collect { snackbarController?.show(refreshFailureMessage) }
    }

    val state = uiState
    ResonoteContentStateLayout(
        phase = when (state) {
            HomeUiState.Loading -> ResonoteContentPhase.LOADING
            is HomeUiState.Error -> ResonoteContentPhase.ERROR
            is HomeUiState.Content -> ResonoteContentPhase.CONTENT
        },
        modifier = modifier,
        loading = {
            HomeLoading(
                bottomContentPadding = bottomContentPadding,
                onSearchClick = onSearchClick,
                onRecognitionClick = onRecognitionClick,
            )
        },
        error = {
            HomeLoadError(
                onRetry = viewModel::refresh,
                onSearchClick = onSearchClick,
                onRecognitionClick = onRecognitionClick,
            )
        },
        content = {
            val contentState = state as? HomeUiState.Content ?: return@ResonoteContentStateLayout
            HomeScreen(
                state = contentState.content,
                isRefreshing = contentState.isRefreshing,
                playingMediaId = playingMediaId,
                bottomContentPadding = bottomContentPadding,
                onRefresh = viewModel::refresh,
                onSearchClick = onSearchClick,
                onRecognitionClick = onRecognitionClick,
                onPlayRadio = {
                    coroutineScope.launch { viewModel.radioPlaybackRequest()?.let(onPlay) }
                },
                onOpenRankings = onOpenRankings,
                onOpenFeaturedPlaylists = onOpenFeaturedPlaylists,
                onSongClick = { collection, song ->
                    viewModel.playbackRequest(collection, song.id)?.let(onPlay)
                },
                onSongMoreClick = { song ->
                    viewModel.songForAction(song.id)?.let(onSongMoreClick)
                },
                onPlayAll = { collection ->
                    viewModel.playbackRequest(collection)?.let(onPlay)
                },
                onPlaylistClick = onPlaylistClick,
            )
        },
    )
}

@Composable
internal fun HomeLoadError(
    onRetry: () -> Unit,
    onSearchClick: () -> Unit,
    onRecognitionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.Scaffold(
        modifier = modifier,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        topBar = { HomeTopBar(onSearchClick, onRecognitionClick) },
    ) { padding ->
        ResonoteErrorState(
            onRetry = onRetry,
            title = stringResource(R.string.feature_home_impl_error_title),
            message = stringResource(R.string.feature_home_impl_error_message),
            modifier = Modifier.padding(padding),
        )
    }
}

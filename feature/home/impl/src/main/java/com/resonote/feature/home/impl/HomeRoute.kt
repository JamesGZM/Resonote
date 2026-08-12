package com.resonote.feature.home.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    when (val state = uiState) {
        HomeUiState.Loading -> HomeLoading(modifier)
        is HomeUiState.Error -> HomeLoadError(onRetry = viewModel::refresh, modifier = modifier)
        is HomeUiState.Content ->
            HomeScreen(
                state = state.content,
                playingMediaId = playingMediaId,
                bottomContentPadding = bottomContentPadding,
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
                modifier = modifier,
            )
    }
}

@Composable
private fun HomeLoading(modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun HomeLoadError(onRetry: () -> Unit, modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = onRetry) {
            Text(stringResource(R.string.feature_home_impl_retry))
        }
    }
}

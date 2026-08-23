@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.artist.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.model.OnlineSong
import com.resonote.feature.artist.api.ArtistNavKey

@Composable
fun ArtistRoute(
    key: ArtistNavKey,
    playingMediaId: String?,
    onBack: () -> Unit,
    onPlayAll: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    bottomContentPadding: Dp = 32.dp,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(key) { viewModel.load(key) }
    ArtistScreen(
        state = state,
        initialProfile = key.toProfile(),
        playingMediaId = playingMediaId,
        onBack = onBack,
        onSelectSection = viewModel::selectSection,
        onRetry = viewModel::retry,
        onLoadMore = viewModel::loadMore,
        onPlayAll = onPlayAll,
        onSongClick = onSongClick,
        onSongMoreClick = onSongMoreClick,
        bottomContentPadding = bottomContentPadding,
    )
}

@Composable
fun ArtistScreen(
    state: ArtistUiState,
    initialProfile: ArtistProfile? = null,
    playingMediaId: String?,
    onBack: () -> Unit,
    onSelectSection: (ArtistSongSection) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onPlayAll: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    bottomContentPadding: Dp = 32.dp,
    modifier: Modifier = Modifier,
) {
    val fallbackTitle = stringResource(R.string.feature_artist_impl_artist_title_fallback)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ResonoteTopAppBar(
                title = {
                    Text(
                        state.profile?.name ?: fallbackTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            stringResource(R.string.feature_artist_impl_artist_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        ArtistContent(
            state = state,
            profile = state.profile ?: initialProfile,
            playingMediaId = playingMediaId,
            onSelectSection = onSelectSection,
            onRetry = onRetry,
            onLoadMore = onLoadMore,
            onPlayAll = onPlayAll,
            onSongClick = onSongClick,
            onSongMoreClick = onSongMoreClick,
            bottomContentPadding = bottomContentPadding,
            modifier = Modifier.padding(padding),
        )
    }
}

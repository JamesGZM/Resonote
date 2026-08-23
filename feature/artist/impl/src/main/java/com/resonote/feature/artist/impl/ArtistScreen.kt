@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.artist.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.resonote.core.model.ArtistAlbum
import com.resonote.core.model.ArtistVideo
import com.resonote.core.model.OnlineSong
import com.resonote.feature.artist.api.ArtistNavKey

@Composable
fun ArtistRoute(
    key: ArtistNavKey,
    currentAccountId: String?,
    playingMediaId: String?,
    onBack: () -> Unit,
    onLoginRequest: () -> Unit,
    onPlayAll: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    onAlbumClick: (ArtistAlbum) -> Unit,
    onVideoClick: (ArtistVideo) -> Unit,
    bottomContentPadding: Dp = 32.dp,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarController = LocalResonoteSnackbarController.current
    val refreshFailureMessage = stringResource(R.string.feature_artist_impl_artist_refresh_failed)
    val followFailureMessage = stringResource(R.string.feature_artist_impl_artist_follow_failed)
    val refreshFailure = (state.selectedPage() as? ArtistPageUiState.Content)?.refreshFailure
    val followFailure = (state.follow as? ArtistFollowUiState.Available)?.updateFailure
    LaunchedEffect(key) { viewModel.load(key) }
    LaunchedEffect(currentAccountId) { viewModel.onAccountChanged(currentAccountId) }
    LaunchedEffect(viewModel) { viewModel.loginRequests.collect { onLoginRequest() } }
    LaunchedEffect(refreshFailure, snackbarController) {
        if (refreshFailure != null) {
            snackbarController?.show(refreshFailureMessage)
            viewModel.acknowledgeRefreshFailure()
        }
    }
    LaunchedEffect(followFailure, snackbarController) {
        if (followFailure != null) {
            snackbarController?.show(followFailureMessage)
            viewModel.acknowledgeFollowFailure()
        }
    }
    ArtistScreen(
        state = state,
        initialProfile = key.toProfile(),
        playingMediaId = playingMediaId,
        onBack = onBack,
        onFollowClick = viewModel::toggleFollow,
        onSelectSection = viewModel::selectSection,
        onSelectSort = viewModel::selectSort,
        onRetry = viewModel::retry,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onPlayAll = onPlayAll,
        onSongClick = onSongClick,
        onSongMoreClick = onSongMoreClick,
        onAlbumClick = onAlbumClick,
        onVideoClick = onVideoClick,
        bottomContentPadding = bottomContentPadding,
    )
}

@Composable
fun ArtistScreen(
    state: ArtistUiState,
    initialProfile: ArtistProfile? = null,
    playingMediaId: String?,
    onBack: () -> Unit,
    onFollowClick: () -> Unit,
    onSelectSection: (ArtistSection) -> Unit,
    onSelectSort: (ArtistSort) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onPlayAll: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    onAlbumClick: (ArtistAlbum) -> Unit,
    onVideoClick: (ArtistVideo) -> Unit,
    bottomContentPadding: Dp = 32.dp,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), propagateMinConstraints = true) {
        ArtistContent(
            state = state,
            profile = state.profile ?: initialProfile,
            playingMediaId = playingMediaId,
            onBack = onBack,
            onFollowClick = onFollowClick,
            onSelectSection = onSelectSection,
            onSelectSort = onSelectSort,
            onRetry = onRetry,
            onRefresh = onRefresh,
            onLoadMore = onLoadMore,
            onPlayAll = onPlayAll,
            onSongClick = onSongClick,
            onSongMoreClick = onSongMoreClick,
            onAlbumClick = onAlbumClick,
            onVideoClick = onVideoClick,
            bottomContentPadding = bottomContentPadding,
        )
    }
}

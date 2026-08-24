@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.library.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.LocalResonoteSnackbarController
import com.resonote.core.designsystem.component.ResonoteEmptyState
import com.resonote.core.designsystem.component.ResonoteErrorState
import com.resonote.core.designsystem.component.ResonoteLoadMoreEffect
import com.resonote.core.designsystem.component.ResonotePlainAction
import com.resonote.core.designsystem.component.ResonotePullToRefreshBox
import com.resonote.core.designsystem.component.ResonoteRemoteArtwork
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.model.FollowedArtist

@Composable
fun FollowingRoute(
    bottomContentPadding: Dp,
    onBack: () -> Unit,
    onArtistClick: (FollowedArtist) -> Unit,
    viewModel: FollowingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarController = LocalResonoteSnackbarController.current
    val failure = (state as? FollowingUiState.Content)?.let {
        it.refreshingFailure ?: it.updateFailure
    }
    val failureMessage = stringResource(R.string.feature_library_impl_following_action_failed)
    LaunchedEffect(failure, snackbarController) {
        if (failure != null) {
            snackbarController?.show(failureMessage)
            viewModel.acknowledgeFailure()
        }
    }
    FollowingScreen(
        state = state,
        bottomContentPadding = bottomContentPadding,
        onBack = onBack,
        onRetry = viewModel::retry,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onArtistClick = onArtistClick,
        onUnfollow = viewModel::unfollow,
    )
}

@Composable
internal fun FollowingScreen(
    state: FollowingUiState,
    bottomContentPadding: Dp,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onArtistClick: (FollowedArtist) -> Unit,
    onUnfollow: (FollowedArtist) -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = state as? FollowingUiState.Content
    val listState = rememberLazyListState()
    ResonoteLoadMoreEffect(
        listState = listState,
        itemCount = content?.artists?.size ?: 0,
        enabled = content?.hasMore == true && !content.isRefreshing,
        onLoadMore = onLoadMore,
    )
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ResonoteTopAppBar(
                title = { Text(stringResource(R.string.feature_library_impl_following_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            stringResource(R.string.feature_library_impl_following_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        ResonotePullToRefreshBox(
            isRefreshing = content?.isRefreshing == true,
            onRefresh = onRefresh,
            enabled = content != null,
            modifier = Modifier.fillMaxSize().padding(padding).testTag("following-pull-to-refresh"),
        ) {
            when (state) {
                FollowingUiState.Loading -> FollowingLoading()
                FollowingUiState.Empty -> ResonoteEmptyState(
                    icon = Icons.Rounded.PersonOff,
                    title = stringResource(R.string.feature_library_impl_following_empty_title),
                    message = stringResource(R.string.feature_library_impl_following_empty_body),
                    modifier = Modifier.fillMaxSize(),
                )
                is FollowingUiState.Error -> ResonoteErrorState(
                    onRetry = onRetry,
                    title = stringResource(R.string.feature_library_impl_following_error_title),
                    message = state.failure.message(),
                    retryLabel = stringResource(R.string.feature_library_impl_following_retry),
                    modifier = Modifier.fillMaxSize(),
                )
                is FollowingUiState.Content -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().testTag("following-list"),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 8.dp,
                        end = 16.dp,
                        bottom = bottomContentPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(state.artists, key = FollowedArtist::id) { artist ->
                        FollowingArtistItem(
                            artist = artist,
                            isUpdating = artist.id in state.updatingArtistIds,
                            onClick = { onArtistClick(artist) },
                            onUnfollow = { onUnfollow(artist) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowingLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.testTag("following-loading"))
    }
}

@Composable
internal fun FollowingArtistItem(
    artist: FollowedArtist,
    isUpdating: Boolean,
    onClick: () -> Unit,
    onUnfollow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ResonotePlainAction(
            onClick = onClick,
            modifier = Modifier.weight(1f).testTag("following-item-${artist.id}"),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ResonoteRemoteArtwork(
                    model = artist.avatarUrl,
                    contentDescription = stringResource(
                        R.string.feature_library_impl_following_avatar,
                        artist.name,
                    ),
                    modifier = Modifier.size(56.dp).clip(CircleShape),
                )
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.feature_library_impl_following_artist),
                        modifier = Modifier.padding(top = 3.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        OutlinedButton(
            onClick = onUnfollow,
            enabled = !isUpdating,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier = Modifier.testTag("following-button-${artist.id}"),
        ) {
            if (isUpdating) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.feature_library_impl_following_followed))
            }
        }
    }
}

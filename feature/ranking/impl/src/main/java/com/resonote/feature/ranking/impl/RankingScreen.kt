@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.ranking.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
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
import com.resonote.feature.ranking.api.RankingNavKey

@Composable
fun RankingRoute(
    key: RankingNavKey,
    playingMediaId: String?,
    onBack: () -> Unit,
    onPlayAll: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    bottomContentPadding: Dp = 32.dp,
    viewModel: RankingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarController = LocalResonoteSnackbarController.current
    val refreshFailureMessage = stringResource(R.string.feature_ranking_impl_ranking_refresh_failed)
    val refreshFailure = (state as? RankingUiState.Content)?.refreshFailure
    LaunchedEffect(key) { viewModel.load(key) }
    LaunchedEffect(refreshFailure, snackbarController) {
        if (refreshFailure != null) {
            snackbarController?.show(refreshFailureMessage)
            viewModel.acknowledgeRefreshFailure()
        }
    }
    RankingScreen(
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
fun RankingScreen(
    state: RankingUiState,
    initialMetadata: RankingMetadata? = null,
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
    val stateMetadata = state.metadata()
    val metadata = stateMetadata.takeIf { it.id.isNotBlank() } ?: initialMetadata ?: stateMetadata
    val title = metadata.title ?: stringResource(R.string.feature_ranking_impl_ranking_title_fallback)
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val rankingContent = state as? RankingUiState.Content
        if (state is RankingUiState.Loading || rankingContent != null) {
            RankingContentLayout(
                state = rankingContent,
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
        } else if (state is RankingUiState.Empty) {
            StandardStateScaffold(title = title, onBack = onBack) { padding ->
                ResonoteEmptyState(
                    icon = Icons.Rounded.BarChart,
                    title = stringResource(R.string.feature_ranking_impl_ranking_empty_title),
                    message = stringResource(R.string.feature_ranking_impl_ranking_empty_body),
                    modifier = Modifier.padding(padding),
                )
            }
        } else {
            val failure = (state as? RankingUiState.Error)?.failure ?: ContentFailure.Protocol
            StandardStateScaffold(title = title, onBack = onBack) { padding ->
                ResonoteErrorState(
                    onRetry = onRetry,
                    icon = Icons.Rounded.BarChart,
                    title = stringResource(R.string.feature_ranking_impl_ranking_error_title),
                    message = failure.message(),
                    modifier = Modifier.padding(padding),
                    retryLabel = stringResource(R.string.feature_ranking_impl_ranking_retry),
                )
            }
        }
    }
}

private fun RankingNavKey.toInitialMetadata() = RankingMetadata(
    id = rankingId,
    title = title?.takeIf(String::isNotBlank),
    coverUrl = coverUrl?.takeIf(String::isNotBlank),
)

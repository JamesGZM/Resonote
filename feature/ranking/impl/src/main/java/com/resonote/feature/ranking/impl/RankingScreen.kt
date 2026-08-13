@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.ranking.impl

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.ResonoteMusicItem
import com.resonote.core.designsystem.component.ResonoteRemoteArtwork
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.model.AudioQuality
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
    viewModel: RankingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(key) { viewModel.load(key) }
    RankingScreen(
        state = state,
        playingMediaId = playingMediaId,
        onBack = onBack,
        onRetry = viewModel::retry,
        onLoadMore = viewModel::loadMore,
        onPlayAll = onPlayAll,
        onSongClick = onSongClick,
        onSongMoreClick = onSongMoreClick,
    )
}

@Composable
fun RankingScreen(
    state: RankingUiState,
    playingMediaId: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onPlayAll: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val fallbackTitle = stringResource(R.string.ranking_title_fallback)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ResonoteTopAppBar(
                title = {
                    Text(
                        state.metadata().title ?: fallbackTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.ranking_back))
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            is RankingUiState.Loading -> LoadingState(Modifier.padding(padding))
            is RankingUiState.Empty -> MessageState(
                icon = Icons.Rounded.BarChart,
                title = stringResource(R.string.ranking_empty_title),
                body = stringResource(R.string.ranking_empty_body),
                modifier = Modifier.padding(padding),
            )
            is RankingUiState.Error -> ErrorState(state.failure, onRetry, Modifier.padding(padding))
            is RankingUiState.Content -> RankingContent(
                state = state,
                playingMediaId = playingMediaId,
                onLoadMore = onLoadMore,
                onPlayAll = onPlayAll,
                onSongClick = onSongClick,
                onSongMoreClick = onSongMoreClick,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun RankingContent(
    state: RankingUiState.Content,
    playingMediaId: String?,
    onLoadMore: () -> Unit,
    onPlayAll: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("ranking-list"),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item(key = "header") {
            RankingHeader(
                metadata = state.metadata,
                songCount = state.total ?: state.songs.size,
                onPlayAll = { onPlayAll(state.songs) },
            )
        }
        itemsIndexed(state.songs, key = { _, song -> "song-${song.hash}" }) { index, song ->
            ResonoteMusicItem(
                title = song.title,
                supportingText = song.artist.orEmpty(),
                duration = song.durationMillis.durationLabel(),
                qualityLabel = song.quality.label(),
                isVip = song.vip,
                isPlaying = song.hash == playingMediaId,
                artworkState = ResonoteArtworkState.LOADED,
                artworkUrl = song.coverUrl,
                artwork = {
                    Text(
                        text = (index + 1).toString().padStart(2, '0'),
                        modifier = Modifier.align(Alignment.Center),
                        color = if (index < 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                onClick = { onSongClick(song) },
                onMoreClick = onSongMoreClick?.let { callback -> { callback(song) } },
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
                            Text(stringResource(R.string.ranking_load_more_retry))
                        }
                        state.hasMore -> TextButton(onClick = onLoadMore) {
                            Text(stringResource(R.string.ranking_load_more))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingHeader(
    metadata: RankingMetadata,
    songCount: Int,
    onPlayAll: () -> Unit,
) {
    val title = metadata.title ?: stringResource(R.string.ranking_title_fallback)
    val artworkDescription = stringResource(R.string.ranking_artwork, title)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(rankingGradient(metadata.id))
                    .semantics { contentDescription = artworkDescription },
                contentAlignment = Alignment.Center,
            ) {
                if (!metadata.coverUrl.isNullOrBlank()) {
                    ResonoteRemoteArtwork(
                        model = metadata.coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        fallback = {},
                    )
                }
                Row(
                    modifier = Modifier.height(72.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(32.dp, 50.dp, 70.dp).forEach { height ->
                        Box(
                            Modifier.width(18.dp).height(height)
                                .clip(MaterialTheme.shapes.small)
                                .background(Color.White.copy(alpha = 0.9f)),
                        )
                    }
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.ranking_chart_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.ranking_song_count, songCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Button(
            onClick = onPlayAll,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 14.dp),
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.ranking_play_all))
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
        ContentFailure.Network -> stringResource(R.string.ranking_error_network)
        ContentFailure.AuthenticationRequired -> stringResource(R.string.ranking_error_auth)
        else -> stringResource(R.string.ranking_error_generic)
    }
    MessageState(
        icon = Icons.Rounded.BarChart,
        title = stringResource(R.string.ranking_error_title),
        body = body,
        modifier = modifier,
        action = { Button(onClick = onRetry) { Text(stringResource(R.string.ranking_retry)) } },
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
            Text(
                body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            action?.invoke()
        }
    }
}

private fun RankingUiState.metadata(): RankingMetadata = when (this) {
    is RankingUiState.Loading -> metadata
    is RankingUiState.Content -> metadata
    is RankingUiState.Empty -> metadata
    is RankingUiState.Error -> metadata
}

private fun rankingGradient(seed: String): Brush {
    val palettes = listOf(
        listOf(Color(0xFF420918), Color(0xFFC22653), Color(0xFFF28D68)),
        listOf(Color(0xFF063D3D), Color(0xFF14887E), Color(0xFFF1CE79)),
        listOf(Color(0xFF172351), Color(0xFF5877CD), Color(0xFFE9B5D1)),
        listOf(Color(0xFF452B08), Color(0xFFB9761D), Color(0xFFF5D797)),
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

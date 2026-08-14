@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.album.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.ResonoteMusicItem
import com.resonote.core.designsystem.component.ResonoteRemoteArtwork
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.designsystem.tokens.ResonoteTokens
import com.resonote.core.model.AudioQuality
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
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(key) { viewModel.load(key) }
    AlbumScreen(
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
fun AlbumScreen(
    state: AlbumUiState,
    playingMediaId: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onPlayAll: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val fallbackTitle = stringResource(R.string.feature_album_impl_album_title_fallback)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ResonoteTopAppBar(
                title = {
                    Text(
                        text = state.title() ?: fallbackTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            stringResource(R.string.feature_album_impl_album_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            AlbumUiState.Loading -> LoadingState(Modifier.padding(padding))
            is AlbumUiState.Empty -> MessageState(
                icon = Icons.Rounded.Album,
                title = stringResource(R.string.feature_album_impl_album_empty_title),
                body = stringResource(R.string.feature_album_impl_album_empty_body),
                modifier = Modifier.padding(padding),
            )
            is AlbumUiState.Error -> ErrorState(state.failure, onRetry, Modifier.padding(padding))
            is AlbumUiState.Content -> AlbumContent(
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
private fun AlbumContent(
    state: AlbumUiState.Content,
    playingMediaId: String?,
    onLoadMore: () -> Unit,
    onPlayAll: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("album-list"),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item(key = "header") {
            AlbumHeader(
                metadata = state.metadata,
                loadedSongCount = state.songs.size,
                onPlayAll = { onPlayAll(state.songs) },
            )
        }
        itemsIndexed(
            items = state.songs,
            key = { index, song -> "song-${song.hash}-$index" },
        ) { _, song ->
            ResonoteMusicItem(
                title = song.title,
                supportingText = song.artist.orEmpty(),
                duration = song.durationMillis.durationLabel(),
                qualityLabel = song.quality.label(),
                isVip = song.vip,
                isPlaying = song.hash == playingMediaId,
                artworkUrl = song.coverUrl,
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
                            Text(stringResource(R.string.feature_album_impl_album_load_more_retry))
                        }
                        state.hasMore -> TextButton(onClick = onLoadMore) {
                            Text(stringResource(R.string.feature_album_impl_album_load_more))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumHeader(metadata: AlbumMetadata, loadedSongCount: Int, onPlayAll: () -> Unit) {
    val title = metadata.title ?: stringResource(R.string.feature_album_impl_album_title_fallback)
    val artworkDescription = stringResource(R.string.feature_album_impl_album_artwork, title)
    val detailLine = listOfNotNull(
        metadata.publishDate?.substringBefore(' ')?.takeIf(String::isNotBlank),
        (metadata.songCount ?: loadedSongCount).let {
            stringResource(R.string.feature_album_impl_album_song_count, it)
        },
    ).joinToString(" · ")
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.width(156.dp).height(140.dp).semantics {
                    contentDescription = artworkDescription
                },
            ) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .align(Alignment.CenterEnd)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .border(18.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(24.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                    )
                }
                Surface(
                    modifier = Modifier.size(128.dp).align(Alignment.CenterStart),
                    shape = MaterialTheme.shapes.large,
                    shadowElevation = ResonoteTokens.elevation.level3.maximumShadow,
                ) {
                    ResonoteRemoteArtwork(
                        model = metadata.coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.feature_album_impl_album_type_label),
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
                metadata.artist?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    detailLine,
                    style = MaterialTheme.typography.labelMedium,
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
            Text(stringResource(R.string.feature_album_impl_album_play_all))
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
        ContentFailure.Network -> stringResource(R.string.feature_album_impl_album_error_network)
        ContentFailure.AuthenticationRequired -> stringResource(R.string.feature_album_impl_album_error_auth)
        else -> stringResource(R.string.feature_album_impl_album_error_generic)
    }
    MessageState(
        icon = Icons.Rounded.Album,
        title = stringResource(R.string.feature_album_impl_album_error_title),
        body = body,
        modifier = modifier,
        action = { Button(onClick = onRetry) { Text(stringResource(R.string.feature_album_impl_album_retry)) } },
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

private fun AlbumUiState.title(): String? = when (this) {
    AlbumUiState.Loading -> null
    is AlbumUiState.Content -> metadata.title
    is AlbumUiState.Empty -> metadata.title
    is AlbumUiState.Error -> title
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

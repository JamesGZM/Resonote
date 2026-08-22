@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.artist.impl

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.ResonoteMusicItem
import com.resonote.core.designsystem.component.ResonoteRemoteArtwork
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.ContentFailure
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

@Composable
private fun ArtistContent(
    state: ArtistUiState,
    playingMediaId: String?,
    onSelectSection: (ArtistSongSection) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onPlayAll: (List<OnlineSong>) -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onSongMoreClick: ((OnlineSong) -> Unit)?,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val page = state.selectedPage()
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("artist-list"),
        contentPadding = PaddingValues(bottom = bottomContentPadding),
    ) {
        item(key = "profile") { ArtistHeader(state.profile) }
        item(key = "sections") {
            PrimaryTabRow(
                selectedTabIndex = state.selectedSection.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                ArtistSongSection.entries.forEach { section ->
                    Tab(
                        selected = section == state.selectedSection,
                        onClick = { onSelectSection(section) },
                        text = {
                            Text(
                                when (section) {
                                    ArtistSongSection.POPULAR -> stringResource(
                                        R.string.feature_artist_impl_artist_popular,
                                    )
                                    ArtistSongSection.LATEST -> stringResource(
                                        R.string.feature_artist_impl_artist_latest,
                                    )
                                },
                            )
                        },
                    )
                }
            }
        }
        when (page) {
            ArtistPageUiState.Idle,
            ArtistPageUiState.Loading,
            -> item(key = "loading") { LoadingState() }
            ArtistPageUiState.Empty -> item(key = "empty") {
                MessageState(
                    icon = Icons.Rounded.Person,
                    title = stringResource(R.string.feature_artist_impl_artist_empty_title),
                    body = stringResource(R.string.feature_artist_impl_artist_empty_body),
                )
            }
            is ArtistPageUiState.Error -> item(key = "error") {
                ErrorState(page.failure, onRetry)
            }
            is ArtistPageUiState.Content -> {
                item(key = "play-all") {
                    Button(
                        onClick = { onPlayAll(page.songs) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.feature_artist_impl_artist_play_all))
                    }
                }
                itemsIndexed(
                    items = page.songs,
                    key = { index, song -> "${state.selectedSection}-${song.hash}-$index" },
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
                if (page.hasMore || page.isLoadingMore || page.loadMoreFailure != null) {
                    item(key = "load-more") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                page.isLoadingMore -> CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                page.loadMoreFailure != null -> TextButton(onClick = onLoadMore) {
                                    Text(stringResource(R.string.feature_artist_impl_artist_load_more_retry))
                                }
                                page.hasMore -> TextButton(onClick = onLoadMore) {
                                    Text(stringResource(R.string.feature_artist_impl_artist_load_more))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistHeader(profile: ArtistProfile?) {
    val name = profile?.name ?: stringResource(R.string.feature_artist_impl_artist_title_fallback)
    val portraitDescription = stringResource(R.string.feature_artist_impl_artist_portrait, name)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(148.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(132.dp).align(Alignment.TopStart).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                )
                Box(
                    modifier = Modifier
                        .size(124.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .semantics { contentDescription = portraitDescription },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!profile?.avatarUrl.isNullOrBlank()) {
                        ResonoteRemoteArtwork(
                            model = profile.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            fallback = {},
                        )
                    }
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                profile?.fansCount?.takeIf { it > 0 }?.let {
                    Text(
                        stringResource(R.string.feature_artist_impl_artist_fans, it.compactCount()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ArtistMetric(
                profile?.songCount,
                stringResource(R.string.feature_artist_impl_artist_songs),
                Modifier.weight(1f),
            )
            ArtistMetric(
                profile?.albumCount,
                stringResource(R.string.feature_artist_impl_artist_albums),
                Modifier.weight(1f),
            )
            ArtistMetric(profile?.mvCount, stringResource(R.string.feature_artist_impl_artist_mvs), Modifier.weight(1f))
        }
        profile?.intro?.takeIf(String::isNotBlank)?.let { intro ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    intro,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ArtistMetric(value: Int?, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value?.toString() ?: "—",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(failure: ContentFailure, onRetry: () -> Unit) {
    val body = when (failure) {
        ContentFailure.Network -> stringResource(R.string.feature_artist_impl_artist_error_network)
        ContentFailure.AuthenticationRequired -> stringResource(R.string.feature_artist_impl_artist_error_auth)
        else -> stringResource(R.string.feature_artist_impl_artist_error_generic)
    }
    MessageState(
        icon = Icons.Rounded.Person,
        title = stringResource(R.string.feature_artist_impl_artist_error_title),
        body = body,
        action = { Button(onClick = onRetry) { Text(stringResource(R.string.feature_artist_impl_artist_retry)) } },
    )
}

@Composable
private fun MessageState(icon: ImageVector, title: String, body: String, action: (@Composable () -> Unit)? = null) {
    Box(
        Modifier.fillMaxWidth().height(300.dp).padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
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

private fun Long.compactCount(): String = when {
    this >= 10_000 && this % 10_000 == 0L -> "${this / 10_000}万"
    this >= 10_000 -> "%.1f万".format(this / 10_000.0)
    else -> toString()
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

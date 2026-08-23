@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.playlist.impl

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.ResonoteContentPhase
import com.resonote.core.designsystem.component.ResonoteHeroKeys
import com.resonote.core.designsystem.component.ResonoteMusicItem
import com.resonote.core.designsystem.component.ResonoteRemoteArtwork
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.designsystem.component.rememberResonoteShimmer
import com.resonote.core.designsystem.component.resonoteHero
import com.resonote.core.designsystem.component.resonoteShimmer
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.PlaylistDetails

@Composable
internal fun PlaylistHeader(
    details: PlaylistDetails?,
    loadedSongCount: Int,
    canPlay: Boolean,
    onPlayAll: () -> Unit,
    heroPlaylistId: String? = details?.id,
) {
    val title = details?.title ?: stringResource(R.string.feature_playlist_impl_playlist_title_fallback)
    val artworkDescription = stringResource(R.string.feature_playlist_impl_playlist_artwork, title)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val topScrim = if (isDark) Color.Black.copy(alpha = 0.36f) else Color.White.copy(alpha = 0.3f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .semantics { contentDescription = artworkDescription },
    ) {
        ResonoteRemoteArtwork(
            model = details?.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .resonoteHero(heroPlaylistId?.let(ResonoteHeroKeys::playlist))
                .testTag("playlist-hero")
                .matchParentSize(),
            fallback = {
                Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.primaryContainer))
            },
        )
        Box(
            Modifier.fillMaxWidth().height(96.dp).background(
                Brush.verticalGradient(colors = listOf(topScrim, Color.Transparent)),
            ),
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val textShadow = Shadow(
                color = Color.Black.copy(alpha = 0.72f),
                offset = Offset(0f, 1.5f),
                blurRadius = 3.5f,
            )
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall.copy(shadow = textShadow),
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            details?.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.84f),
                    style = MaterialTheme.typography.bodySmall.copy(shadow = textShadow),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        R.string.feature_playlist_impl_playlist_song_count,
                        details?.songCount ?: loadedSongCount,
                    ),
                    color = Color.White.copy(alpha = 0.84f),
                    style = MaterialTheme.typography.bodyMedium.copy(shadow = textShadow),
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    onClick = onPlayAll,
                    enabled = canPlay,
                    modifier = Modifier.height(40.dp),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.38f),
                    contentColor = Color.White,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.feature_playlist_impl_playlist_play_all),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ImmersiveToolbar(title: String?, onBack: () -> Unit, collapseProgress: Float) {
    val surface = MaterialTheme.colorScheme.surface
    ResonoteTopAppBar(
        title = {
            if (collapseProgress > 0f && title != null) {
                Text(
                    text = title,
                    modifier = Modifier.graphicsLayer { alpha = collapseProgress },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        modifier = Modifier.fillMaxWidth().testTag("playlist-toolbar"),
        navigationIcon = {
            Surface(
                modifier = Modifier.padding(start = 4.dp).size(40.dp),
                shape = CircleShape,
                color = surface.copy(alpha = 0.7f * (1f - collapseProgress)),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        stringResource(R.string.feature_playlist_impl_playlist_back),
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = surface.copy(alpha = collapseProgress),
            scrolledContainerColor = surface,
        ),
    )
}

@Composable
internal fun rememberCollapseProgress(listState: LazyListState): Float {
    val density = LocalDensity.current
    val startPx = with(density) { 200.dp.roundToPx() }
    val endPx = with(density) { 300.dp.roundToPx() }
    val progress by remember(listState, startPx, endPx) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                ((listState.firstVisibleItemScrollOffset - startPx).toFloat() / (endPx - startPx))
                    .coerceIn(0f, 1f)
            }
        }
    }
    return progress
}

@Composable
internal fun PlaylistSkeleton(
    bottomContentPadding: Dp,
    initialDetails: PlaylistDetails? = null,
    heroPlaylistId: String? = initialDetails?.id,
) {
    val shimmer = rememberResonoteShimmer("playlist-skeleton")
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("playlist-skeleton"),
        contentPadding = PaddingValues(bottom = bottomContentPadding),
    ) {
        item(key = "header") {
            if (initialDetails != null) {
                PlaylistHeader(
                    details = initialDetails,
                    loadedSongCount = 0,
                    canPlay = false,
                    onPlayAll = {},
                    heroPlaylistId = heroPlaylistId,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .resonoteShimmer(shimmer, RectangleShape),
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        val placeholderColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                        Box(
                            Modifier
                                .fillMaxWidth(0.7f)
                                .height(28.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(placeholderColor),
                        )
                        Box(
                            Modifier
                                .fillMaxWidth(0.82f)
                                .height(12.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(placeholderColor),
                        )
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .width(84.dp)
                                    .height(14.dp)
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(placeholderColor),
                            )
                            Spacer(Modifier.weight(1f))
                            Box(
                                Modifier
                                    .width(108.dp)
                                    .height(40.dp)
                                    .clip(MaterialTheme.shapes.extraLarge)
                                    .background(placeholderColor),
                            )
                        }
                    }
                }
            }
        }
        item(key = "list-top-spacing") { Spacer(Modifier.height(12.dp)) }
        items(6, key = { "song-$it" }) {
            ResonoteMusicItem(
                title = "",
                supportingText = "",
                duration = "",
                modifier = Modifier.padding(horizontal = 8.dp),
                artworkState = ResonoteArtworkState.LOADING,
                enabled = false,
                onClick = {},
                onMoreClick = null,
            )
        }
    }
}

@Composable
internal fun PlaylistLoadingHeader() {
    val shimmer = rememberResonoteShimmer("playlist-loading-header")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .resonoteShimmer(shimmer, RectangleShape),
    ) {
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val placeholderColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
            Box(
                Modifier
                    .fillMaxWidth(0.7f)
                    .height(28.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(placeholderColor),
            )
            Box(
                Modifier
                    .fillMaxWidth(0.82f)
                    .height(12.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(placeholderColor),
            )
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .width(84.dp)
                        .height(14.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(placeholderColor),
                )
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .width(108.dp)
                        .height(40.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(placeholderColor),
                )
            }
        }
    }
}

@Composable
internal fun StandardStateScaffold(title: String, onBack: () -> Unit, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ResonoteTopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            stringResource(R.string.feature_playlist_impl_playlist_back),
                        )
                    }
                },
            )
        },
        content = content,
    )
}

internal fun PlaylistUiState.phase(): ResonoteContentPhase = when (this) {
    PlaylistUiState.Loading -> ResonoteContentPhase.LOADING
    PlaylistUiState.Empty -> ResonoteContentPhase.EMPTY
    is PlaylistUiState.Error -> ResonoteContentPhase.ERROR
    is PlaylistUiState.Content -> ResonoteContentPhase.CONTENT
}

@Composable
internal fun ContentFailure.errorMessage(): String = when (this) {
    ContentFailure.Network -> stringResource(R.string.feature_playlist_impl_playlist_error_network)
    ContentFailure.AuthenticationRequired -> stringResource(R.string.feature_playlist_impl_playlist_error_auth)
    else -> stringResource(R.string.feature_playlist_impl_playlist_error_generic)
}

internal fun Long.durationLabel(): String {
    val totalSeconds = coerceAtLeast(0) / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

internal fun AudioQuality.label(): String? = when (this) {
    AudioQuality.Standard -> null
    AudioQuality.HighQuality -> "HQ"
    AudioQuality.HighResolution -> "HI-RES"
    AudioQuality.Lossless -> "LOSSLESS"
}

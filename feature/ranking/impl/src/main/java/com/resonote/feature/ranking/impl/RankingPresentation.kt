@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.ranking.impl

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
import androidx.compose.ui.semantics.clearAndSetSemantics
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

@Composable
internal fun RankingHeader(metadata: RankingMetadata, songCount: Int, onPlayAll: () -> Unit, canPlay: Boolean = true) {
    val title = metadata.title ?: stringResource(R.string.feature_ranking_impl_ranking_title_fallback)
    val artworkDescription = stringResource(R.string.feature_ranking_impl_ranking_artwork, title)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val topScrim = if (isDark) Color.Black.copy(alpha = 0.36f) else Color.White.copy(alpha = 0.3f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .semantics { contentDescription = artworkDescription },
    ) {
        ResonoteRemoteArtwork(
            model = metadata.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .resonoteHero(ResonoteHeroKeys.ranking(metadata.id))
                .testTag("ranking-hero")
                .matchParentSize(),
            fallback = {
                Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.primaryContainer))
            },
        )
        Box(
            Modifier.fillMaxWidth().height(96.dp).background(
                Brush.verticalGradient(
                    colors = listOf(topScrim, Color.Transparent),
                ),
            ),
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.feature_ranking_impl_ranking_song_count, songCount),
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
                            text = stringResource(R.string.feature_ranking_impl_ranking_play_all),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun RankingPosition(position: Int) {
    val description = stringResource(R.string.feature_ranking_impl_ranking_position, position)
    val modifier = Modifier.width(36.dp).clearAndSetSemantics { contentDescription = description }
    when (position) {
        1 -> RankingPositionBadge(
            position = position,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = modifier,
        )
        2 -> RankingPositionBadge(
            position = position,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = modifier,
        )
        3 -> RankingPositionBadge(
            position = position,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = modifier,
        )
        else -> Box(modifier = modifier.height(36.dp), contentAlignment = Alignment.Center) {
            Text(
                text = position.toString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
internal fun RankingPositionBadge(position: Int, containerColor: Color, contentColor: Color, modifier: Modifier) {
    Surface(
        modifier = modifier.height(36.dp),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = position.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
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
        modifier = Modifier.fillMaxWidth().testTag("ranking-toolbar"),
        navigationIcon = {
            Surface(
                modifier = Modifier.padding(start = 4.dp).size(40.dp),
                shape = CircleShape,
                color = surface.copy(alpha = 0.7f * (1f - collapseProgress)),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        stringResource(R.string.feature_ranking_impl_ranking_back),
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
    val startPx = with(density) { 180.dp.roundToPx() }
    val endPx = with(density) { 280.dp.roundToPx() }
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
internal fun RankingSkeleton(bottomContentPadding: Dp, metadata: RankingMetadata? = null) {
    val shimmer = rememberResonoteShimmer("ranking-skeleton")
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("ranking-skeleton"),
        contentPadding = PaddingValues(bottom = bottomContentPadding),
    ) {
        item(key = "header") {
            if (metadata != null && metadata.id.isNotBlank()) {
                RankingHeader(metadata = metadata, songCount = 0, onPlayAll = {}, canPlay = false)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .resonoteShimmer(shimmer, RectangleShape),
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val placeholderColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                        Box(
                            Modifier
                                .fillMaxWidth(0.7f)
                                .height(28.dp)
                                .clip(MaterialTheme.shapes.small)
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
                leadingContent = {
                    Box(Modifier.size(36.dp).resonoteShimmer(shimmer, CircleShape))
                },
                onClick = {},
                onMoreClick = null,
            )
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
                            stringResource(R.string.feature_ranking_impl_ranking_back),
                        )
                    }
                },
            )
        },
        content = content,
    )
}

internal fun RankingUiState.phase(): ResonoteContentPhase = when (this) {
    is RankingUiState.Loading -> ResonoteContentPhase.LOADING
    is RankingUiState.Empty -> ResonoteContentPhase.EMPTY
    is RankingUiState.Error -> ResonoteContentPhase.ERROR
    is RankingUiState.Content -> ResonoteContentPhase.CONTENT
}

internal fun RankingUiState.metadata(): RankingMetadata = when (this) {
    is RankingUiState.Loading -> metadata
    is RankingUiState.Content -> metadata
    is RankingUiState.Empty -> metadata
    is RankingUiState.Error -> metadata
}

@Composable
internal fun ContentFailure.message(): String = when (this) {
    ContentFailure.Network -> stringResource(R.string.feature_ranking_impl_ranking_error_network)
    ContentFailure.AuthenticationRequired -> stringResource(R.string.feature_ranking_impl_ranking_error_auth)
    else -> stringResource(R.string.feature_ranking_impl_ranking_error_generic)
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

package com.resonote.feature.player.impl

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.resonote.core.designsystem.component.ResonoteArtwork
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.tokens.ResonoteTokens
import com.resonote.core.model.LyricLine
import com.resonote.core.playback.PlaybackMetadata
import kotlinx.coroutines.delay

@Composable
internal fun PlayerPager(
    song: PlaybackMetadata,
    lyrics: LyricsUiState,
    positionMillis: Long,
    onSeek: (Long) -> Unit,
    onRetryLyrics: () -> Unit,
    initialPage: Int,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(initialPage = initialPage.coerceIn(0, 1), pageCount = { 2 })
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) { page ->
            when (page) {
                0 -> CoverPage(song)
                else -> LyricsPage(lyrics, positionMillis, onSeek, onRetryLyrics)
            }
        }
        Row(
            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(2) { index ->
                Box(
                    Modifier
                        .width(if (pagerState.currentPage == index) 22.dp else 6.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun CoverPage(song: PlaybackMetadata) {
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.92f)
                .aspectRatio(1f)
                .background(
                    Brush.radialGradient(
                        listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.24f), Color.Transparent),
                    ),
                    CircleShape,
                ),
        )
        ResonoteArtwork(
            state = ResonoteArtworkState.LOADED,
            contentDescription = stringResource(R.string.feature_player_impl_artwork, song.title),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(ResonoteTokens.elevation.level3.maximumShadow, RoundedCornerShape(28.dp)),
        ) {
            SignalArtwork(song.mediaId)
            if (!song.artworkUri.isNullOrBlank()) {
                AsyncImage(
                    model = song.artworkUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun SignalArtwork(seed: String) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surface = MaterialTheme.colorScheme.surface
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val phase = (seed.hashCode().ushr(1) % 90).toFloat()
    Canvas(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(surface, primary.copy(alpha = 0.72f), secondary))),
    ) {
        val center = center.copy(x = center.x * 1.08f, y = center.y * 0.92f)
        val step = size.minDimension / 13f
        repeat(7) { ring ->
            drawCircle(
                color = onPrimary.copy(alpha = 0.08f + ring * 0.018f),
                radius = step * (ring + 1),
                center = center,
                style = Stroke(width = 1.2.dp.toPx()),
            )
        }
        repeat(5) { arc ->
            drawArc(
                color = onPrimary.copy(alpha = 0.18f + arc * 0.07f),
                startAngle = phase + arc * 21f,
                sweepAngle = 72f + arc * 12f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(center.x - step * (arc + 2), center.y - step * (arc + 2)),
                size = androidx.compose.ui.geometry.Size(step * (arc + 2) * 2, step * (arc + 2) * 2),
                style = Stroke(width = (1.5f + arc * 0.7f).dp.toPx()),
            )
        }
        drawCircle(onPrimary.copy(alpha = 0.92f), radius = step * 0.36f, center = center)
        drawCircle(primary, radius = step * 0.15f, center = center)
    }
}

@Composable
private fun LyricsPage(lyrics: LyricsUiState, positionMillis: Long, onSeek: (Long) -> Unit, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
        when (lyrics) {
            LyricsUiState.Idle,
            LyricsUiState.Loading,
            -> CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp)
            LyricsUiState.Empty, LyricsUiState.Unavailable ->
                LyricsMessage(stringResource(R.string.feature_player_impl_lyrics_empty))
            is LyricsUiState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LyricsMessage(lyrics.failure.lyricsMessage())
                Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(R.string.feature_player_impl_retry))
                }
            }
            is LyricsUiState.Content -> SyncedLyrics(lyrics.lines, positionMillis, onSeek)
        }
    }
}

@Composable
private fun SyncedLyrics(lines: List<LyricLine>, positionMillis: Long, onSeek: (Long) -> Unit) {
    val activeIndex = lines.indexOfLast { it.timeMillis <= positionMillis }
    val listState = rememberLazyListState()
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    var followEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(activeIndex, followEnabled) {
        if (followEnabled && activeIndex >= 0) listState.animateScrollToItem(activeIndex, scrollOffset = -180)
    }
    LaunchedEffect(isDragged) {
        if (isDragged) {
            followEnabled = false
        } else if (!followEnabled) {
            delay(3_500)
            followEnabled = true
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().testTag("player-lyrics"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 160.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        items(lines.size, key = { "${lines[it].timeMillis}-$it" }) { index ->
            val line = lines[index]
            val active = index == activeIndex
            Surface(
                onClick = { onSeek(line.timeMillis) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ResonoteTokens.touchTargets.minimum),
                shape = MaterialTheme.shapes.medium,
                color = if (active) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f)
                } else {
                    Color.Transparent
                },
                contentColor = if (active) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                },
            ) {
                Text(
                    text = line.text,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = if (active) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    lineHeight =
                    if (active) {
                        MaterialTheme.typography.headlineSmall.lineHeight
                    } else {
                        MaterialTheme.typography.titleMedium.lineHeight
                    },
                )
            }
        }
    }
}

package com.resonote.feature.player.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.resonote.core.designsystem.component.ResonoteArtwork
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.ResonoteQualityBadge
import com.resonote.core.designsystem.component.ResonoteVipBadge
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.LyricLine
import com.resonote.core.model.OnlineSong
import com.resonote.core.playback.PlaybackMode
import com.resonote.core.playback.PlaybackStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerRoute(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PlayerScreen(
        state = state,
        onBack = onBack,
        onTogglePlay = viewModel::togglePlayPause,
        onPrevious = viewModel::previous,
        onNext = viewModel::next,
        onSeek = viewModel::seekTo,
        onModeChange = viewModel::setMode,
        onRetryLyrics = viewModel::retryLyrics,
        onSelectQueueItem = viewModel::selectQueueItem,
        onRemoveQueueItem = viewModel::removeQueueItem,
        onMoveQueueItem = viewModel::moveQueueItem,
        onClearQueue = viewModel::clearQueue,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    state: PlayerUiState,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onModeChange: (PlaybackMode) -> Unit,
    onRetryLyrics: () -> Unit,
    onSelectQueueItem: (Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onClearQueue: () -> Unit,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
) {
    val song = state.playback.currentSong
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    var queueOpen by remember { mutableStateOf(false) }
    val unavailable = stringResource(R.string.feature_player_impl_share_unavailable)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(playerBackdrop())
                .padding(padding),
        ) {
            if (song == null) {
                EmptyPlayer(onBack)
            } else {
                Column(Modifier.fillMaxSize()) {
                    PlayerTopBar(
                        onBack = onBack,
                        menuOpen = menuOpen,
                        onMenuChange = { menuOpen = it },
                        onShare = {
                            menuOpen = false
                            scope.launch { snackbar.showSnackbar(unavailable) }
                        },
                    )
                    PlayerPager(
                        song = song,
                        lyrics = state.lyrics,
                        positionMillis = state.playback.positionMillis,
                        onSeek = onSeek,
                        onRetryLyrics = onRetryLyrics,
                        initialPage = initialPage,
                        modifier = Modifier.weight(1f),
                    )
                    SongIdentity(song)
                    PlayerProgress(
                        positionMillis = state.playback.positionMillis,
                        durationMillis = state.playback.durationMillis,
                        onSeek = onSeek,
                    )
                    PlaybackControls(
                        status = state.playback.status,
                        mode = state.playback.mode,
                        onTogglePlay = onTogglePlay,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onModeChange = onModeChange,
                        onOpenQueue = { queueOpen = true },
                    )
                }
            }
        }
    }

    if (queueOpen) {
        PlaybackQueueSheet(
            playback = state.playback,
            onDismiss = { queueOpen = false },
            onSelect = onSelectQueueItem,
            onRemove = onRemoveQueueItem,
            onMove = onMoveQueueItem,
            onClear = onClearQueue,
            onModeChange = onModeChange,
        )
    }
}

@Composable
private fun playerBackdrop(): Brush {
    val colors = MaterialTheme.colorScheme
    return Brush.verticalGradient(
        0f to colors.surface,
        0.45f to colors.surfaceContainerLow,
        1f to colors.primaryContainer.copy(alpha = 0.46f),
    )
}

@Composable
private fun PlayerTopBar(
    onBack: () -> Unit,
    menuOpen: Boolean,
    onMenuChange: (Boolean) -> Unit,
    onShare: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.feature_player_impl_collapse))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.feature_player_impl_now_playing),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Box(
                Modifier
                    .padding(top = 5.dp)
                    .width(28.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Box {
            IconButton(onClick = { onMenuChange(true) }) {
                Icon(Icons.Rounded.MoreVert, stringResource(R.string.feature_player_impl_more))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { onMenuChange(false) }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.feature_player_impl_share)) },
                    leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                    onClick = onShare,
                )
            }
        }
    }
}

@Composable
private fun PlayerPager(
    song: OnlineSong,
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
                            if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                        ),
                )
            }
        }
    }
}

@Composable
private fun CoverPage(song: OnlineSong) {
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
                .shadow(20.dp, RoundedCornerShape(28.dp), ambientColor = MaterialTheme.colorScheme.primary),
        ) {
            SignalArtwork(song.hash)
            if (!song.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = song.coverUrl,
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
                color = Color.White.copy(alpha = 0.08f + ring * 0.018f),
                radius = step * (ring + 1),
                center = center,
                style = Stroke(width = 1.2.dp.toPx()),
            )
        }
        repeat(5) { arc ->
            drawArc(
                color = Color.White.copy(alpha = 0.18f + arc * 0.07f),
                startAngle = phase + arc * 21f,
                sweepAngle = 72f + arc * 12f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(center.x - step * (arc + 2), center.y - step * (arc + 2)),
                size = androidx.compose.ui.geometry.Size(step * (arc + 2) * 2, step * (arc + 2) * 2),
                style = Stroke(width = (1.5f + arc * 0.7f).dp.toPx()),
            )
        }
        drawCircle(Color.White.copy(alpha = 0.92f), radius = step * 0.36f, center = center)
        drawCircle(primary, radius = step * 0.15f, center = center)
    }
}

@Composable
private fun LyricsPage(
    lyrics: LyricsUiState,
    positionMillis: Long,
    onSeek: (Long) -> Unit,
    onRetry: () -> Unit,
) {
    Box(Modifier.fillMaxSize().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
        when (lyrics) {
            LyricsUiState.Idle, LyricsUiState.Loading -> CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp)
            LyricsUiState.Empty -> LyricsMessage(stringResource(R.string.feature_player_impl_lyrics_empty))
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
            Text(
                text = line.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f)
                        else Color.Transparent,
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .clickable(onClick = { onSeek(line.timeMillis) }),
                color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                style = if (active) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                lineHeight = if (active) MaterialTheme.typography.headlineSmall.lineHeight else MaterialTheme.typography.titleMedium.lineHeight,
            )
        }
    }
}

@Composable
private fun SongIdentity(song: OnlineSong) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            song.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                song.artist.orEmpty().ifBlank { stringResource(R.string.feature_player_impl_unknown_artist) },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            song.quality.qualityLabel()?.let { label ->
                Spacer(Modifier.width(8.dp))
                ResonoteQualityBadge(label)
            }
            if (song.vip) {
                Spacer(Modifier.width(6.dp))
                ResonoteVipBadge()
            }
        }
    }
}

@Composable
private fun PlayerProgress(positionMillis: Long, durationMillis: Long, onSeek: (Long) -> Unit) {
    val duration = durationMillis.coerceAtLeast(1L)
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val progress = if (dragging) dragValue else (positionMillis.toFloat() / duration).coerceIn(0f, 1f)
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Slider(
            value = progress,
            onValueChange = { dragging = true; dragValue = it },
            onValueChangeFinished = {
                onSeek((dragValue * duration).toLong())
                dragging = false
            },
        )
        Row(Modifier.fillMaxWidth()) {
            Text((if (dragging) (dragValue * duration).toLong() else positionMillis).timeLabel(), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.weight(1f))
            Text(durationMillis.timeLabel(), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun PlaybackControls(
    status: PlaybackStatus,
    mode: PlaybackMode,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onModeChange: (PlaybackMode) -> Unit,
    onOpenQueue: () -> Unit,
) {
    val isPlaying = status == PlaybackStatus.Playing
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = { onModeChange(mode.next()) }, modifier = Modifier.size(48.dp)) {
            Icon(mode.icon(), mode.label(), tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipPrevious, stringResource(R.string.feature_player_impl_previous), Modifier.size(34.dp))
        }
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 10.dp,
            onClick = onTogglePlay,
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (status == PlaybackStatus.Resolving || status == PlaybackStatus.Buffering) {
                    CircularProgressIndicator(Modifier.size(28.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 3.dp)
                } else {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        stringResource(if (isPlaying) R.string.feature_player_impl_pause else R.string.feature_player_impl_play),
                        Modifier.size(38.dp),
                    )
                }
            }
        }
        IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipNext, stringResource(R.string.feature_player_impl_next), Modifier.size(34.dp))
        }
        IconButton(onClick = onOpenQueue, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Rounded.QueueMusic, stringResource(R.string.feature_player_impl_queue))
        }
    }
}

@Composable
private fun EmptyPlayer(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.StopCircle, contentDescription = null, modifier = Modifier.size(48.dp))
        Text(stringResource(R.string.feature_player_impl_empty_queue), modifier = Modifier.padding(top = 16.dp))
        Button(onClick = onBack, modifier = Modifier.padding(top = 20.dp)) {
            Text(stringResource(R.string.feature_player_impl_back))
        }
    }
}

@Composable
private fun LyricsMessage(message: String) {
    Text(
        message,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ContentFailure.lyricsMessage(): String = stringResource(
    when (this) {
        ContentFailure.AuthenticationRequired -> R.string.feature_player_impl_lyrics_auth
        ContentFailure.Network -> R.string.feature_player_impl_lyrics_network
        ContentFailure.ServiceRejected -> R.string.feature_player_impl_lyrics_service
        is ContentFailure.RiskVerificationRequired, ContentFailure.RiskBlocked -> R.string.feature_player_impl_lyrics_risk
        ContentFailure.Protocol -> R.string.feature_player_impl_lyrics_protocol
    },
)

private fun PlaybackMode.next(): PlaybackMode = when (this) {
    PlaybackMode.ListLoop -> PlaybackMode.Shuffle
    PlaybackMode.Shuffle -> PlaybackMode.SingleLoop
    PlaybackMode.SingleLoop -> PlaybackMode.Sequential
    PlaybackMode.Sequential -> PlaybackMode.ListLoop
}

private fun PlaybackMode.icon() = when (this) {
    PlaybackMode.ListLoop -> Icons.Rounded.Repeat
    PlaybackMode.Shuffle -> Icons.Rounded.Shuffle
    PlaybackMode.SingleLoop -> Icons.Rounded.RepeatOne
    PlaybackMode.Sequential -> Icons.AutoMirrored.Rounded.PlaylistPlay
}

@Composable
private fun PlaybackMode.label(): String = stringResource(
    when (this) {
        PlaybackMode.ListLoop -> R.string.feature_player_impl_mode_list_loop
        PlaybackMode.Shuffle -> R.string.feature_player_impl_mode_shuffle
        PlaybackMode.SingleLoop -> R.string.feature_player_impl_mode_single_loop
        PlaybackMode.Sequential -> R.string.feature_player_impl_mode_sequential
    },
)

private fun AudioQuality.qualityLabel(): String? = when (this) {
    AudioQuality.Standard -> null
    AudioQuality.HighQuality -> "HQ"
    AudioQuality.HighResolution -> "HI-RES"
    AudioQuality.Lossless -> "LOSSLESS"
}

private fun Long.timeLabel(): String {
    val totalSeconds = coerceAtLeast(0) / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

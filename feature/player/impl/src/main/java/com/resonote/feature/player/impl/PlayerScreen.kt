package com.resonote.feature.player.impl

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.resonote.core.designsystem.component.LocalResonoteSnackbarController
import com.resonote.core.designsystem.component.resonoteHero
import com.resonote.core.designsystem.tokens.ResonoteTokens
import com.resonote.core.model.LyricsBackgroundMode
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaybackMode
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.playback.PlaybackOrigin
import kotlinx.coroutines.delay

object ResonotePlayerHeroKeys {
    fun container(mediaId: String) = "player:${mediaId.trim()}:container"
    fun artwork(mediaId: String) = "player:${mediaId.trim()}:artwork"
}

@Composable
fun PlayerRoute(
    onBack: () -> Unit,
    onPlayNextClick: (OnlineSong) -> Unit,
    onAppendToQueueClick: (OnlineSong) -> Unit,
    onAddToPlaylistClick: (OnlineSong) -> Unit,
    onSongInfoClick: (OnlineSong) -> Unit,
    onLoginRequest: () -> Unit = {},
    onLyricsSettingsClick: () -> Unit = {},
    paletteSeed: PlayerPaletteSeed? = null,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalResonoteSnackbarController.current
    val likeFailed = stringResource(R.string.feature_player_impl_like_failed)
    val likeUnsupported = stringResource(R.string.feature_player_impl_like_unsupported)
    val queuedNext = stringResource(R.string.feature_player_impl_added_next)
    val queuedLast = stringResource(R.string.feature_player_impl_added_queue)
    val onlineSong = (state.playback.currentItem?.origin as? PlaybackOrigin.Online)?.song
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                PlayerEvent.LoginRequired -> onLoginRequest()
                PlayerEvent.LikeFailed -> snackbar?.show(likeFailed)
                PlayerEvent.LikeUnsupported -> snackbar?.show(likeUnsupported)
            }
        }
    }
    PlayerScreen(
        state = state,
        onBack = onBack,
        onTogglePlay = viewModel::togglePlayPause,
        onPrevious = viewModel::previous,
        onNext = viewModel::next,
        onSeek = viewModel::seekTo,
        onModeChange = viewModel::setMode,
        onPlaybackSpeedChange = viewModel::setPlaybackSpeed,
        onOnlineQualityChange = viewModel::setCurrentOnlineQuality,
        onRetryLyrics = viewModel::retryLyrics,
        onSelectQueueItem = viewModel::selectQueueItem,
        onRemoveQueueItem = viewModel::removeQueueItem,
        onClearQueue = viewModel::clearQueue,
        onToggleLike = viewModel::toggleLike,
        onLyricsSettingsClick = onLyricsSettingsClick,
        paletteSeed = paletteSeed,
        onPlayNextClick = onlineSong?.let { song ->
            {
                onPlayNextClick(song)
                snackbar?.show(queuedNext)
            }
        },
        onAppendToQueueClick = onlineSong?.let { song ->
            {
                onAppendToQueueClick(song)
                snackbar?.show(queuedLast)
            }
        },
        onAddToPlaylistClick = onlineSong?.let { song -> { onAddToPlaylistClick(song) } },
        onSongInfoClick = onlineSong?.let { song -> { onSongInfoClick(song) } },
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
    onPlaybackSpeedChange: (PlaybackSpeed) -> Unit,
    onRetryLyrics: () -> Unit,
    onSelectQueueItem: (Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onOnlineQualityChange: (OnlinePlaybackQuality) -> Unit = {},
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    onPlayNextClick: (() -> Unit)? = null,
    onAppendToQueueClick: (() -> Unit)? = null,
    onAddToPlaylistClick: (() -> Unit)? = null,
    onSongInfoClick: (() -> Unit)? = null,
    onToggleLike: () -> Unit = {},
    onLyricsSettingsClick: () -> Unit = {},
    paletteSeed: PlayerPaletteSeed? = null,
) {
    val song = state.playback.currentMetadata
    val fallbackPalette = defaultPlayerPalette()
    val initialMediaId = remember { song?.mediaId }
    var targetPalette by remember {
        mutableStateOf(
            paletteSeed?.takeIf { it.mediaId == song?.mediaId }?.let(PlayerPalette::fromSeed) ?: fallbackPalette,
        )
    }
    LaunchedEffect(song?.mediaId, paletteSeed) {
        val mediaId = song?.mediaId ?: return@LaunchedEffect
        if (mediaId == initialMediaId) return@LaunchedEffect
        val prepared = paletteSeed?.takeIf { it.mediaId == mediaId }
        if (prepared != null) {
            targetPalette = PlayerPalette.fromSeed(prepared)
        } else {
            delay(1_000)
            targetPalette = fallbackPalette
        }
    }
    val palette = animatePlayerPalette(targetPalette)
    var actionsOpen by remember { mutableStateOf(false) }
    var queueOpen by remember { mutableStateOf(false) }
    var speedOpen by remember { mutableStateOf(false) }
    var formatOpen by remember { mutableStateOf(false) }
    var currentPage by rememberSaveable { mutableStateOf(initialPage.coerceIn(0, 1)) }
    val artworkBackdropVisible = currentPage == 0 ||
        state.lyricsPreferences.backgroundMode == LyricsBackgroundMode.Artwork
    val decorationVisible = currentPage == 0 ||
        state.lyricsPreferences.backgroundMode != LyricsBackgroundMode.Off
    val artworkBackdropAlpha by animateFloatAsState(
        if (artworkBackdropVisible) 0.86f else 0f,
        animationSpec = ResonoteTokens.motion.effectsSlow(),
        label = "player artwork backdrop",
    )
    val decorationAlpha by animateFloatAsState(
        if (decorationVisible) 1f else 0f,
        animationSpec = ResonoteTokens.motion.effectsSlow(),
        label = "player backdrop decoration",
    )

    Scaffold(
        modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding)
                .then(
                    if (song !=
                        null
                    ) {
                        Modifier.resonoteHero(ResonotePlayerHeroKeys.container(song.mediaId))
                    } else {
                        Modifier
                    },
                )
                .background(palette.background),
        ) {
            if (decorationAlpha > 0f) {
                Box(
                    Modifier.matchParentSize().background(
                        Brush.radialGradient(
                            listOf(
                                palette.accent.copy(alpha = 0.24f * decorationAlpha),
                                palette.background.copy(alpha = 0.08f * decorationAlpha),
                                Color.Transparent,
                            ),
                        ),
                    ),
                )
            }
            if (song != null && !song.artworkUri.isNullOrBlank() && artworkBackdropAlpha > 0f) {
                AsyncImage(
                    song.artworkUri,
                    null,
                    Modifier.matchParentSize().graphicsLayer {
                        scaleX = 1.20f
                        scaleY = 1.20f
                        alpha = artworkBackdropAlpha
                    }.blur(52.dp, BlurredEdgeTreatment.Unbounded),
                    contentScale = ContentScale.Crop,
                )
            }
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        listOf(
                            palette.background.copy(alpha = 0.48f),
                            palette.background.copy(alpha = 0.18f),
                            palette.background.copy(alpha = 0.40f),
                            palette.background.copy(alpha = 0.82f),
                        ),
                    ),
                ),
            )
            if (song == null) {
                EmptyPlayer(onBack)
            } else {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val systemBarVerticalInset =
                        WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    val pagerHeight = (maxHeight - 330.dp - systemBarVerticalInset).coerceAtLeast(260.dp)
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    ) {
                        PlayerTopBar(
                            song,
                            state.playback.currentItem?.onlineQualityOverride?.playerTagLabel()
                                ?: song.format.playerTagLabel(),
                            onBack,
                            { actionsOpen = true },
                            palette,
                        )
                        PlayerPager(
                            song,
                            state.lyrics,
                            state.lyricsPreferences,
                            state.playback.positionMillis,
                            palette,
                            onSeek,
                            onRetryLyrics,
                            currentPage,
                            { currentPage = it },
                            Modifier.height(pagerHeight),
                        )
                        PlayerPageIndicator(currentPage, palette)
                        PlayerProgress(
                            state.playback.positionMillis,
                            state.playback.bufferedPositionMillis,
                            state.playback.durationMillis,
                            palette,
                            onSeek,
                        )
                        PlaybackControls(
                            state.playback.status,
                            state.playback.mode,
                            state.like,
                            palette,
                            onToggleLike,
                            onTogglePlay,
                            onPrevious,
                            onNext,
                            onModeChange,
                        )
                        PlayerToolRow(
                            palette,
                            state.playback.playbackSpeed,
                            { formatOpen = true },
                            { speedOpen = true },
                            { queueOpen = true },
                        )
                    }
                }
            }
        }
    }
    if (actionsOpen) {
        PlayerActionsSheet(
            { actionsOpen = false },
            onPlayNextClick,
            onAppendToQueueClick,
            onAddToPlaylistClick,
            onSongInfoClick,
            onLyricsSettingsClick,
        )
    }
    if (queueOpen) {
        PlaybackQueueSheet(state.playback, {
            queueOpen = false
        }, onSelectQueueItem, onRemoveQueueItem, onClearQueue, onModeChange)
    }
    if (speedOpen) {
        PlaybackSpeedSheet(state.playback.playbackSpeed, {
            onPlaybackSpeedChange(it)
            speedOpen = false
        }, {
            speedOpen =
                false
        })
    }
    if (formatOpen && song != null) {
        PlaybackFormatSheet(
            song.format,
            state.playback.currentItem?.onlineQualityOverride,
            {
                onOnlineQualityChange(it)
                formatOpen = false
            },
            { formatOpen = false },
        )
    }
}

package com.resonote.feature.player.impl

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
    onSongMoreClick: (OnlineSong) -> Unit,
    onLoginRequest: () -> Unit = {},
    onLyricsSettingsClick: () -> Unit = {},
    paletteSeed: PlayerPaletteSeed? = null,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalResonoteSnackbarController.current
    val likeFailed = stringResource(R.string.feature_player_impl_like_failed)
    val likeUnsupported = stringResource(R.string.feature_player_impl_like_unsupported)
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
        onSongMoreClick = (state.playback.currentItem?.origin as? PlaybackOrigin.Online)?.song?.let { song ->
            { onSongMoreClick(song) }
        },
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
    onSongMoreClick: (() -> Unit)? = null,
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
    val snackbar = LocalResonoteSnackbarController.current
    val unavailable = stringResource(R.string.feature_player_impl_share_unavailable)
    var actionsOpen by remember { mutableStateOf(false) }
    var queueOpen by remember { mutableStateOf(false) }
    var speedOpen by remember { mutableStateOf(false) }
    var formatOpen by remember { mutableStateOf(false) }
    var currentPage by rememberSaveable { mutableStateOf(initialPage.coerceIn(0, 1)) }

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
            if (song != null &&
                state.lyricsPreferences.backgroundMode == LyricsBackgroundMode.Artwork &&
                !song.artworkUri.isNullOrBlank()
            ) {
                AsyncImage(
                    song.artworkUri,
                    null,
                    Modifier.matchParentSize().graphicsLayer {
                        scaleX = 1.28f
                        scaleY = 1.28f
                        alpha = 0.42f
                    }.blur(64.dp),
                    contentScale = ContentScale.Crop,
                )
            }
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        listOf(
                            palette.background.copy(alpha = 0.78f),
                            palette.background.copy(alpha = 0.54f),
                            palette.background.copy(alpha = 0.96f),
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
            onSongMoreClick,
            onLyricsSettingsClick,
            { snackbar?.show(unavailable) },
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

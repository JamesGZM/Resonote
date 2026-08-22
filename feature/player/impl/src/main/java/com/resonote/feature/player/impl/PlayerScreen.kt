package com.resonote.feature.player.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.LocalResonoteSnackbarController
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaybackSpeed
import com.resonote.core.playback.PlaybackMode
import com.resonote.core.playback.PlaybackOrigin

@Composable
fun PlayerRoute(
    onBack: () -> Unit,
    onSongMoreClick: (OnlineSong) -> Unit,
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
        onPlaybackSpeedChange = viewModel::setPlaybackSpeed,
        onRetryLyrics = viewModel::retryLyrics,
        onSelectQueueItem = viewModel::selectQueueItem,
        onRemoveQueueItem = viewModel::removeQueueItem,
        onClearQueue = viewModel::clearQueue,
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
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    onSongMoreClick: (() -> Unit)? = null,
) {
    val song = state.playback.currentMetadata
    val snackbarController = LocalResonoteSnackbarController.current
    var menuOpen by remember { mutableStateOf(false) }
    var queueOpen by remember { mutableStateOf(false) }
    var speedDialogOpen by remember { mutableStateOf(false) }
    val unavailable = stringResource(R.string.feature_player_impl_share_unavailable)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
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
                        onSongMoreClick = onSongMoreClick,
                        playbackSpeed = state.playback.playbackSpeed,
                        onOpenSpeed = {
                            menuOpen = false
                            speedDialogOpen = true
                        },
                        onShare = {
                            menuOpen = false
                            snackbarController?.show(unavailable)
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
            onClear = onClearQueue,
            onModeChange = onModeChange,
        )
    }
    if (speedDialogOpen) {
        PlaybackSpeedDialog(
            selected = state.playback.playbackSpeed,
            onSelect = {
                onPlaybackSpeedChange(it)
                speedDialogOpen = false
            },
            onDismiss = { speedDialogOpen = false },
        )
    }
}

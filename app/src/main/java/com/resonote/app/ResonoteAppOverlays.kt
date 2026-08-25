package com.resonote.app

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.navigation3.runtime.NavKey
import com.resonote.core.designsystem.component.ResonoteSnackbarController
import com.resonote.core.designsystem.component.ResonoteSnackbarHost
import com.resonote.core.designsystem.tokens.ResonoteTokens
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaybackUnavailableReason
import com.resonote.core.model.RiskChallengeHandle
import com.resonote.core.navigation.TabsShellNavKey
import com.resonote.core.playback.PlaybackIssue
import com.resonote.core.playback.PlaybackState
import com.resonote.feature.library.impl.MyUiState
import com.resonote.feature.library.impl.MyViewModel
import com.resonote.feature.library.impl.PlaylistPickerSheet
import com.resonote.feature.player.api.PlayerNavKey
import com.resonote.feature.player.impl.MiniPlayerUiState
import com.resonote.feature.player.impl.PlaybackQueueSheet
import com.resonote.feature.player.impl.PlayerPaletteSeed
import com.resonote.feature.player.impl.ResonoteMiniPlayer
import com.resonote.feature.player.impl.badgeLabel
import com.resonote.feature.recognition.api.RecognitionNavKey
import com.resonote.feature.search.api.SearchTab
import com.resonote.feature.video.api.VideoNavKey
import com.resonote.feature.vip.impl.DailyVipDialogRoute
import com.resonote.feature.vip.impl.DailyVipViewModel

@Stable
internal class ResonoteOverlayState {
    var queueOpen by mutableStateOf(false)
    var dailyVipDialogOpen by mutableStateOf(false)
    var songActionRequest by mutableStateOf<OnlineSongActionRequest?>(null)
    var playlistPickerSong by mutableStateOf<OnlineSong?>(null)
    var infoSong by mutableStateOf<OnlineSong?>(null)
    var playbackChromeInset by mutableStateOf(0.dp)

    internal val snackbarHostSurface: SnackbarHostSurface?
        get() = when {
            playlistPickerSong != null -> SnackbarHostSurface.PlaylistPicker
            songActionRequest != null -> SnackbarHostSurface.SongActions
            queueOpen -> SnackbarHostSurface.PlaybackQueue
            else -> null
        }
}

@Composable
internal fun rememberResonoteOverlayState(): ResonoteOverlayState = remember { ResonoteOverlayState() }

@Composable
internal fun BoxScope.ResonoteAppOverlays(
    state: ResonoteOverlayState,
    myState: MyUiState,
    myViewModel: MyViewModel,
    playbackState: PlaybackState,
    playbackViewModel: PlaybackViewModel,
    snackbarHostState: SnackbarHostState,
    snackbarController: ResonoteSnackbarController,
    onOpenPlaylistPicker: (OnlineSong) -> Unit,
    onSearch: (String, SearchTab) -> Unit,
    onOpenRiskVerification: (RiskChallengeHandle) -> Unit,
    dailyVipViewModel: DailyVipViewModel,
) {
    val queueNextMessage = stringResource(R.string.song_action_added_next)
    val queueAddedMessage = stringResource(R.string.song_action_added_queue)
    val snackbarHostSurface = state.snackbarHostSurface

    if (state.queueOpen) {
        PlaybackQueueSheet(
            playback = playbackState,
            onDismiss = { state.queueOpen = false },
            onSelect = playbackViewModel::selectQueueItem,
            onRemove = playbackViewModel::removeQueueItem,
            onClear = playbackViewModel::clearQueue,
            onModeChange = playbackViewModel::setMode,
            snackbarHost = {
                if (snackbarHostSurface == SnackbarHostSurface.PlaybackQueue) {
                    ModalSnackbarHost(snackbarHostState)
                }
            },
        )
    }

    state.songActionRequest?.let { request ->
        OnlineSongActionsSheet(
            request = request,
            onDismiss = { state.songActionRequest = null },
            onPlay = {
                state.songActionRequest = null
                playbackViewModel.play(request.song)
            },
            onPlayNext = {
                state.songActionRequest = null
                if (playbackViewModel.playNextOnline(request.song)) {
                    snackbarController.show(queueNextMessage)
                }
            },
            onAppendToQueue = {
                state.songActionRequest = null
                playbackViewModel.appendOnline(request.song)
                snackbarController.show(queueAddedMessage)
            },
            onAddToPlaylist = {
                state.songActionRequest = null
                onOpenPlaylistPicker(request.song)
            },
            onShowInfo = {
                state.songActionRequest = null
                state.infoSong = request.song
            },
            snackbarHost = {
                if (snackbarHostSurface == SnackbarHostSurface.SongActions) {
                    ModalSnackbarHost(snackbarHostState)
                }
            },
        )
    }

    state.playlistPickerSong?.let { song ->
        PlaylistPickerSheet(
            state = myState,
            song = song,
            onDismiss = {
                state.playlistPickerSong = null
                myViewModel.dismissPlaylistAdditionFailure()
            },
            onRetryPlaylists = myViewModel::retryPlaylists,
            onPlaylistClick = { myViewModel.addSongToPlaylist(it, song) },
            onDismissFailure = myViewModel::dismissPlaylistAdditionFailure,
            snackbarHost = {
                if (snackbarHostSurface == SnackbarHostSurface.PlaylistPicker) {
                    ModalSnackbarHost(snackbarHostState)
                }
            },
        )
    }

    state.infoSong?.let { song ->
        OnlineSongInfoSheet(
            song = song,
            onDismiss = { state.infoSong = null },
            onSearchSong = {
                state.infoSong = null
                onSearch(song.title, SearchTab.SONGS)
            },
            onSearchArtist = song.artist?.takeIf(String::isNotBlank)?.let { artist ->
                {
                    state.infoSong = null
                    onSearch(artist, SearchTab.ARTISTS)
                }
            },
            onSearchAlbum = song.albumTitle?.takeIf(String::isNotBlank)?.let { album ->
                {
                    state.infoSong = null
                    onSearch(album, SearchTab.ALBUMS)
                }
            },
        )
    }

    DailyVipDialogRoute(
        visible = state.dailyVipDialogOpen,
        onDismiss = { state.dailyVipDialogOpen = false },
        onRewardApplied = {
            myViewModel.refresh()
            playbackViewModel.refreshCurrentOnlineSource(force = true)
        },
        onRiskVerificationRequired = onOpenRiskVerification,
        viewModel = dailyVipViewModel,
    )

    if (snackbarHostSurface == null) {
        GlobalSnackbarHost(
            hostState = snackbarHostState,
            bottomInset = state.playbackChromeInset,
        )
    }
}

internal fun NavKey?.hasPrimaryNavigation(): Boolean = this is TabsShellNavKey

internal fun NavKey?.showsMiniPlayer(): Boolean =
    this != null && this !is PlayerNavKey && this !is RecognitionNavKey && this !is VideoNavKey

internal enum class SnackbarHostSurface {
    PlaybackQueue,
    SongActions,
    PlaylistPicker,
}

@Composable
internal fun ModalSnackbarHost(hostState: SnackbarHostState) {
    val spacing = ResonoteTokens.spacing.space2
    Popup(
        alignment = Alignment.BottomCenter,
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            clippingEnabled = false,
        ),
    ) {
        ResonoteSnackbarHost(
            hostState = hostState,
            modifier = Modifier.padding(horizontal = spacing, vertical = spacing),
        )
    }
}

@Composable
internal fun BoxScope.GlobalSnackbarHost(hostState: SnackbarHostState, bottomInset: Dp) {
    val spacing = ResonoteTokens.spacing.space2
    ResonoteSnackbarHost(
        hostState = hostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .zIndex(Float.MAX_VALUE)
            .padding(
                start = spacing,
                end = spacing,
                bottom = spacing + bottomInset,
            ),
    )
}

@Composable
internal fun BoxScope.GlobalMiniPlayer(
    playbackState: PlaybackState,
    paletteSeed: PlayerPaletteSeed? = null,
    hasTabBar: Boolean,
    tabBarInset: Dp,
    visible: Boolean,
    onOpenPlayer: () -> Unit,
    onTogglePlay: () -> Unit,
    onOpenQueue: () -> Unit,
    onAnchorInsetChanged: (Dp) -> Unit = {},
    animationSpec: FiniteAnimationSpec<Dp> = ResonoteTokens.motion.spatialDefault(),
    modifier: Modifier = Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val density = LocalDensity.current
    val song = playbackState.currentMetadata
    var miniPlayerHeight by remember { mutableStateOf(0.dp) }
    val bottomInset by animateDpAsState(
        targetValue = if (hasTabBar) tabBarInset else 0.dp,
        animationSpec = animationSpec,
        label = "Mini player bottom inset",
    )
    val layoutBottomInset = bottomInset.coerceAtLeast(0.dp)
    LaunchedEffect(song, visible, layoutBottomInset, miniPlayerHeight, hasTabBar, tabBarInset) {
        onAnchorInsetChanged(
            if (song != null && visible) {
                layoutBottomInset + 16.dp + miniPlayerHeight
            } else if (hasTabBar) {
                tabBarInset
            } else {
                0.dp
            },
        )
    }
    if (song == null || !visible) return
    ResonoteMiniPlayer(
        state = MiniPlayerUiState(
            song.mediaId,
            song.title,
            song.artist.orEmpty(),
            song.format.badgeLabel(),
            song.isVip,
            playbackState.isPlaying,
            playbackState.progress,
            song.artworkUri,
        ),
        onOpenPlayer = onOpenPlayer,
        onTogglePlay = onTogglePlay,
        onOpenQueue = onOpenQueue,
        paletteSeed = paletteSeed,
        animatedVisibilityScope = animatedVisibilityScope,
        modifier = modifier
            .align(Alignment.BottomCenter)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .padding(bottom = layoutBottomInset)
            .testTag("resonote-mini-player")
            .onSizeChanged { size -> miniPlayerHeight = with(density) { size.height.toDp() } },
    )
}

@androidx.annotation.StringRes
internal fun PlaybackIssue.messageRes(): Int = when (this) {
    is PlaybackIssue.Unavailable -> when (reason) {
        PlaybackUnavailableReason.Copyright -> R.string.playback_error_copyright
        PlaybackUnavailableReason.Vip -> R.string.playback_error_vip
        PlaybackUnavailableReason.Cloud -> R.string.playback_error_cloud
        PlaybackUnavailableReason.Local -> R.string.playback_error_local
    }
    is PlaybackIssue.SourceFailure -> R.string.playback_error_source
    is PlaybackIssue.PlayerFailure -> R.string.playback_error_player
}

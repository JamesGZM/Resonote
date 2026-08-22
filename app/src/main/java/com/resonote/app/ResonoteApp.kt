package com.resonote.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.rememberNavBackStack
import com.resonote.core.designsystem.component.LocalResonoteSnackbarController
import com.resonote.core.designsystem.component.ResonoteSnackbarController
import com.resonote.core.designsystem.component.rememberResonoteSnackbarController
import com.resonote.core.model.AuthState
import com.resonote.core.model.OnlineSong
import com.resonote.core.navigation.LoginGateNavKey
import com.resonote.core.navigation.TabsShellNavKey
import com.resonote.feature.library.impl.MyUiState
import com.resonote.feature.library.impl.MyViewModel
import com.resonote.feature.library.impl.PlaylistAdditionUiState
import com.resonote.feature.local.api.LocalMusicNavKey
import com.resonote.feature.player.api.PlayerNavKey
import com.resonote.feature.video.api.VideoNavKey

@Composable
internal fun ResonoteApp(
    viewModel: MainActivityViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    snackbarController: ResonoteSnackbarController = rememberResonoteSnackbarController(snackbarHostState),
    onFinishExternalTask: () -> Unit = {},
) {
    val backStack = rememberNavBackStack(TabsShellNavKey)
    val tabsShellState = rememberTabsShellState()
    val overlayState = rememberResonoteOverlayState()
    val hasTabBar = backStack.lastOrNull().hasPrimaryNavigation()
    val playbackState by playbackViewModel.state.collectAsStateWithLifecycle()
    val standaloneBottomContentPadding = if (playbackState.currentMetadata == null) 32.dp else 120.dp
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val externalImportRequests by viewModel.externalImportRequests.collectAsStateWithLifecycle()
    val externalImportRequest = externalImportRequests.firstOrNull()
    val myViewModel: MyViewModel = hiltViewModel()
    val myState by myViewModel.uiState.collectAsStateWithLifecycle()
    val setVideoFullscreen = rememberVideoFullscreenController()
    var tabBarInset by remember { mutableStateOf(0.dp) }
    val addResult = (myState as? MyUiState.Authenticated)?.playlistAddition
    val addSuccessMessage = (addResult as? PlaylistAdditionUiState.Added)?.let {
        stringResource(R.string.song_action_add_success, it.songTitle, it.playlistName)
    }
    val playbackIssueMessage = playbackState.issue?.let { stringResource(it.messageRes()) }

    SyncSystemBars(
        navigationBarColor = if (hasTabBar) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.background
        },
        forceDarkStatusBar = backStack.lastOrNull() is VideoNavKey,
    )

    fun openSongActions(song: OnlineSong, onRemoveRequest: (() -> Unit)? = null) {
        overlayState.songActionRequest = OnlineSongActionRequest(song, onRemoveRequest)
    }

    fun openPlaylistPicker(song: OnlineSong) {
        if (authState is AuthState.Authenticated) {
            myViewModel.preparePlaylistAddition()
            overlayState.playlistPickerSong = song
        } else if (backStack.lastOrNull() !is LoginGateNavKey) {
            backStack.add(LoginGateNavKey(sessionExpired = false))
        }
    }

    LaunchedEffect(authState) {
        backStack.synchronizeAuthenticationGate(authState)
        if (authState !is AuthState.Authenticated) {
            overlayState.playlistPickerSong = null
            overlayState.dailyVipDialogOpen = false
        }
    }

    LaunchedEffect(authState, playbackState.currentItem?.queueKey) {
        if (authState is AuthState.Authenticated) {
            playbackViewModel.refreshCurrentOnlineSource()
        }
    }

    LaunchedEffect(addSuccessMessage) {
        val message = addSuccessMessage ?: return@LaunchedEffect
        overlayState.playlistPickerSong = null
        snackbarController.show(message)
        myViewModel.acknowledgePlaylistAddition()
    }

    LaunchedEffect(playbackIssueMessage) {
        playbackIssueMessage?.let(snackbarController::show)
    }

    LaunchedEffect(externalImportRequest?.id) {
        val request = externalImportRequest ?: return@LaunchedEffect
        if (backStack.lastOrNull() !is LocalMusicNavKey) {
            backStack.add(LocalMusicNavKey(finishTaskOnBack = request.finishTaskOnBack))
        }
    }

    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalResonoteSnackbarController provides snackbarController) {
            ResonoteNavDisplay(
                backStack = backStack,
                tabsShellState = tabsShellState,
                playbackState = playbackState,
                standaloneBottomContentPadding = standaloneBottomContentPadding,
                authState = authState,
                externalImportRequest = externalImportRequest,
                viewModel = viewModel,
                playbackViewModel = playbackViewModel,
                myViewModel = myViewModel,
                onFinishExternalTask = onFinishExternalTask,
                onOpenSongActions = ::openSongActions,
                onOpenPlaylistPicker = ::openPlaylistPicker,
                onOpenDailyVip = { overlayState.dailyVipDialogOpen = true },
                onTabBarInsetChanged = { tabBarInset = it },
                onVideoFullscreenChange = setVideoFullscreen,
            )
        }
        GlobalMiniPlayer(
            playbackState = playbackState,
            hasTabBar = hasTabBar,
            tabBarInset = tabBarInset,
            visible = backStack.lastOrNull().showsMiniPlayer(),
            onOpenPlayer = {
                if (backStack.lastOrNull() !is PlayerNavKey) backStack.add(PlayerNavKey)
            },
            onTogglePlay = playbackViewModel::togglePlayPause,
            onOpenQueue = { overlayState.queueOpen = true },
            onAnchorInsetChanged = { overlayState.playbackChromeInset = it },
        )
        ResonoteAppOverlays(
            state = overlayState,
            myState = myState,
            myViewModel = myViewModel,
            playbackState = playbackState,
            playbackViewModel = playbackViewModel,
            snackbarHostState = snackbarHostState,
            snackbarController = snackbarController,
            onOpenPlaylistPicker = ::openPlaylistPicker,
        )
    }
}

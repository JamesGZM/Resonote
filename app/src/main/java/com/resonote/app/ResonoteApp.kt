package com.resonote.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.resonote.core.designsystem.component.LocalResonoteSnackbarController
import com.resonote.core.designsystem.component.ResonoteSnackbarHost
import com.resonote.core.designsystem.component.ResonoteSnackbarController
import com.resonote.core.designsystem.component.rememberResonoteSnackbarController
import com.resonote.core.designsystem.tokens.ResonoteTokens
import com.resonote.core.model.AuthState
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PlaybackUnavailableReason
import com.resonote.core.navigation.LoginGateNavKey
import com.resonote.core.navigation.TabsShellNavKey
import com.resonote.core.playback.PlaybackIssue
import com.resonote.feature.album.api.AlbumNavKey
import com.resonote.feature.album.impl.AlbumRoute
import com.resonote.feature.artist.api.ArtistNavKey
import com.resonote.feature.artist.impl.ArtistRoute
import com.resonote.feature.auth.impl.LoginRoute
import com.resonote.feature.cloud.api.CloudNavKey
import com.resonote.feature.cloud.impl.CloudRoute
import com.resonote.feature.history.api.HistoryNavKey
import com.resonote.feature.history.api.HistoryTab
import com.resonote.feature.history.impl.HistoryRoute
import com.resonote.feature.library.impl.MyViewModel
import com.resonote.feature.library.impl.MyUiState
import com.resonote.feature.library.impl.PlaylistAdditionUiState
import com.resonote.feature.library.impl.PlaylistPickerSheet
import com.resonote.feature.local.api.LocalMusicNavKey
import com.resonote.feature.local.impl.LocalMusicRoute
import com.resonote.feature.playlist.api.PlaylistNavKey
import com.resonote.feature.playlist.impl.PlaylistRoute
import com.resonote.feature.player.api.PlayerNavKey
import com.resonote.feature.player.impl.PlayerRoute
import com.resonote.feature.ranking.api.RankingNavKey
import com.resonote.feature.ranking.impl.RankingRoute
import com.resonote.feature.recognition.api.RecognitionNavKey
import com.resonote.feature.recognition.impl.RecognitionRoute
import com.resonote.feature.search.api.SearchNavKey
import com.resonote.feature.search.impl.SearchRoute
import com.resonote.feature.settings.api.SettingsNavKey
import com.resonote.feature.settings.impl.SettingsRoute
import com.resonote.feature.vip.api.DailyVipNavKey
import com.resonote.feature.vip.impl.DailyVipRoute
import com.resonote.feature.video.api.VideoNavKey
import com.resonote.feature.video.impl.VideoRoute

@Composable
internal fun ResonoteApp(
    viewModel: MainActivityViewModel = hiltViewModel(),
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    snackbarController: ResonoteSnackbarController = rememberResonoteSnackbarController(snackbarHostState),
    onFinishExternalTask: () -> Unit = {},
) {
    val backStack = rememberNavBackStack(TabsShellNavKey)
    val hasTabBar = backStack.lastOrNull().hasPrimaryNavigation()
    val playbackState by playbackViewModel.state.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val externalImportRequests by viewModel.externalImportRequests.collectAsStateWithLifecycle()
    val externalImportRequest = externalImportRequests.firstOrNull()
    val myViewModel: MyViewModel = hiltViewModel()
    val myState by myViewModel.uiState.collectAsStateWithLifecycle()
    val setVideoFullscreen = rememberVideoFullscreenController()
    var tabSnackbarBottomInset by remember { mutableStateOf(0.dp) }
    var songActionRequest by remember { mutableStateOf<OnlineSongActionRequest?>(null) }
    var playlistPickerSong by remember { mutableStateOf<OnlineSong?>(null) }
    var infoSong by remember { mutableStateOf<OnlineSong?>(null) }
    val addResult = (myState as? MyUiState.Authenticated)?.playlistAddition
    val addSuccessMessage = (addResult as? PlaylistAdditionUiState.Added)?.let {
        stringResource(R.string.song_action_add_success, it.songTitle, it.playlistName)
    }
    val queueNextMessage = stringResource(R.string.song_action_added_next)
    val queueAddedMessage = stringResource(R.string.song_action_added_queue)
    val shareUnavailableMessage = stringResource(R.string.song_action_share_unavailable)
    val playbackIssueMessage = playbackState.issue?.let { stringResource(it.messageRes()) }
    val snackbarSpacing = ResonoteTokens.spacing.space2
    SyncSystemBars(
        navigationBarColor = if (hasTabBar) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.background
        },
    )

    fun openSongActions(
        song: OnlineSong,
        onRemoveRequest: (() -> Unit)? = null,
    ) {
        songActionRequest = OnlineSongActionRequest(song, onRemoveRequest)
    }

    LaunchedEffect(authState) {
        backStack.synchronizeAuthenticationGate(authState)
        if (authState !is AuthState.Authenticated) playlistPickerSong = null
    }

    LaunchedEffect(addSuccessMessage) {
        val message = addSuccessMessage ?: return@LaunchedEffect
        playlistPickerSong = null
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
            NavDisplay(
                backStack = backStack,
                entryProvider = entryProvider {
            entry<TabsShellNavKey> {
                TabsShell(
                    playbackState = playbackState,
                    onPlaySong = playbackViewModel::play,
                    onPlaySongs = playbackViewModel::playAll,
                    onTogglePlay = playbackViewModel::togglePlayPause,
                    onNext = playbackViewModel::next,
                    onOpenPlayer = {
                        if (backStack.lastOrNull() !is PlayerNavKey) backStack.add(PlayerNavKey)
                    },
                    onSelectQueueItem = playbackViewModel::selectQueueItem,
                    onRemoveQueueItem = playbackViewModel::removeQueueItem,
                    onMoveQueueItem = playbackViewModel::moveQueueItem,
                    onClearQueue = playbackViewModel::clearQueue,
                    onModeChange = playbackViewModel::setMode,
                    myViewModel = myViewModel,
                    onLoginRequest = {
                        if (backStack.lastOrNull() !is LoginGateNavKey) {
                            backStack.add(LoginGateNavKey(sessionExpired = false))
                        }
                    },
                    onSearchClick = { backStack.add(SearchNavKey()) },
                    onRecognitionClick = { backStack.add(RecognitionNavKey) },
                    onSongMoreClick = { openSongActions(it) },
                    onDailyVipClick = { backStack.navigateToDailyVip(authState) },
                    onHistoryClick = {
                        backStack.add(
                            HistoryNavKey(
                                initialTab = if (authState is AuthState.Authenticated) {
                                    HistoryTab.Online
                                } else {
                                    HistoryTab.Device
                                },
                            ),
                        )
                    },
                    onCloudClick = { backStack.navigateToCloud(authState) },
                    onLocalMusicClick = { backStack.add(LocalMusicNavKey()) },
                    onSettingsClick = { backStack.add(SettingsNavKey) },
                    onSnackbarBottomInsetChanged = { tabSnackbarBottomInset = it },
                    onPlaylistClick = { backStack.add(PlaylistNavKey(it)) },
                    onUserPlaylistClick = { playlist ->
                        val accountId = (authState as? AuthState.Authenticated)?.userId
                        val canWrite = playlist.isMine && accountId != null
                        backStack.add(
                            PlaylistNavKey(
                                playlistId = playlist.globalId,
                                writableListId = playlist.listId.takeIf { canWrite },
                                writableAccountId = accountId.takeIf { canWrite },
                            ),
                        )
                    },
                    onAlbumClick = { album ->
                        backStack.add(
                            AlbumNavKey(
                                albumId = album.id,
                                name = album.name,
                                artist = album.artist,
                                coverUrl = album.coverUrl,
                                publishDate = album.publishDate,
                                songCount = album.songCount,
                            ),
                        )
                    },
                    onRankingClick = { ranking ->
                        backStack.add(RankingNavKey(ranking.id, ranking.title, ranking.coverUrl))
                    },
                )
            }
            entry<SearchNavKey> { key ->
                SearchRoute(
                    initialQuery = key.initialQuery,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onRecognitionClick = { backStack.add(RecognitionNavKey) },
                    onSongClick = playbackViewModel::play,
                    onSongMoreClick = { openSongActions(it) },
                    onPlaylistClick = { backStack.add(PlaylistNavKey(it)) },
                    onAlbumClick = { album ->
                        backStack.add(
                            AlbumNavKey(
                                albumId = album.id,
                                name = album.name,
                                artist = album.artist,
                                coverUrl = album.coverUrl,
                                publishDate = album.publishDate,
                                songCount = album.songCount,
                            ),
                        )
                    },
                    onArtistClick = { artist ->
                        backStack.add(
                            ArtistNavKey(
                                artistId = artist.id,
                                name = artist.name,
                                avatarUrl = artist.avatarUrl,
                                songCount = artist.songCount,
                                albumCount = artist.albumCount,
                            ),
                        )
                    },
                    onMvClick = { mv ->
                        playbackViewModel.pause()
                        backStack.add(
                            VideoNavKey(
                                hash = mv.hash,
                                title = mv.name,
                                singer = mv.singer,
                                coverUrl = mv.coverUrl,
                                durationMillis = mv.durationMillis,
                            ),
                        )
                    },
                )
            }
            entry<PlayerNavKey> {
                PlayerRoute(
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onSongMoreClick = { openSongActions(it) },
                )
            }
            entry<SettingsNavKey> {
                SettingsRoute(onBack = { backStack.removeAt(backStack.lastIndex) })
            }
            entry<PlaylistNavKey> { key ->
                PlaylistRoute(
                    key = key,
                    playingMediaId = playbackState.currentMetadata?.mediaId,
                    currentAccountId = (authState as? AuthState.Authenticated)?.userId,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onPlayAll = { playbackViewModel.playAll(it) },
                    onSongClick = playbackViewModel::play,
                    onSongMoreClick = { song, onRemove -> openSongActions(song, onRemove) },
                )
            }
            entry<AlbumNavKey> { key ->
                AlbumRoute(
                    key = key,
                    playingMediaId = playbackState.currentMetadata?.mediaId,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onPlayAll = playbackViewModel::playAll,
                    onSongClick = playbackViewModel::play,
                    onSongMoreClick = { openSongActions(it) },
                )
            }
            entry<ArtistNavKey> { key ->
                ArtistRoute(
                    key = key,
                    playingMediaId = playbackState.currentMetadata?.mediaId,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onPlayAll = playbackViewModel::playAll,
                    onSongClick = playbackViewModel::play,
                    onSongMoreClick = { openSongActions(it) },
                )
            }
            entry<RankingNavKey> { key ->
                RankingRoute(
                    key = key,
                    playingMediaId = playbackState.currentMetadata?.mediaId,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onPlayAll = playbackViewModel::playAll,
                    onSongClick = playbackViewModel::play,
                    onSongMoreClick = { openSongActions(it) },
                )
            }
            entry<DailyVipNavKey> {
                DailyVipRoute(
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onRewardApplied = myViewModel::refresh,
                )
            }
            entry<CloudNavKey> {
                CloudRoute(
                    playingMediaId = playbackState.currentMetadata?.mediaId,
                    bottomContentPadding = if (playbackState.currentMetadata == null) 32.dp else 120.dp,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onPlayRequest = { request ->
                        playbackViewModel.playCloud(request.tracks, request.startIndex, request.source)
                    },
                    onAppendTracks = playbackViewModel::appendCloud,
                )
            }
            entry<HistoryNavKey> { key ->
                HistoryRoute(
                    initialTab = key.initialTab,
                    playingMediaId = playbackState.currentMetadata?.mediaId,
                    bottomContentPadding = if (playbackState.currentMetadata == null) 32.dp else 120.dp,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onLoginRequest = {
                        if (backStack.lastOrNull() !is LoginGateNavKey) {
                            backStack.add(LoginGateNavKey(sessionExpired = false))
                        }
                    },
                    onPlayOnline = playbackViewModel::playAll,
                    onSongMoreClick = { openSongActions(it) },
                    onPlayDevice = playbackViewModel::playDeviceHistory,
                )
            }
            entry<LocalMusicNavKey> { key ->
                LocalMusicRoute(
                    playingMediaId = playbackState.currentMetadata?.mediaId,
                    bottomContentPadding = if (playbackState.currentMetadata == null) 32.dp else 120.dp,
                    onBack = {
                        if (backStack.leaveLocalMusic(key)) onFinishExternalTask()
                    },
                    onPlayAll = playbackViewModel::playAllLocal,
                    onPlayMedia = playbackViewModel::playLocal,
                    pendingImportRequestId = externalImportRequest?.id,
                    pendingImportUris = externalImportRequest?.uris.orEmpty(),
                    onPendingImportAccepted = viewModel::acknowledgeExternalImportRequest,
                )
            }
            entry<VideoNavKey> { key ->
                VideoRoute(
                    key = key,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onFullscreenChange = setVideoFullscreen,
                )
            }
            entry<RecognitionNavKey> {
                RecognitionRoute(
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onCaptureStarted = playbackViewModel::pause,
                    onPlay = playbackViewModel::play,
                    onSearch = { match ->
                        val query = listOfNotNull(match.song.title, match.song.artist)
                            .filter(String::isNotBlank)
                            .joinToString(" ")
                        backStack.add(SearchNavKey(initialQuery = query))
                    },
                )
            }
            entry<LoginGateNavKey> { key ->
                LoginRoute(
                    sessionExpired = key.sessionExpired,
                    onBack = {
                        if (backStack.lastOrNull() is LoginGateNavKey) backStack.removeAt(backStack.lastIndex)
                        viewModel.acknowledgeAuthenticationGate()
                    },
                )
            }
                },
            )
        }
        ResonoteSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = snackbarSpacing,
                    end = snackbarSpacing,
                    bottom = snackbarSpacing + if (hasTabBar) tabSnackbarBottomInset else 0.dp,
                ),
        )
    }

    songActionRequest?.let { request ->
        OnlineSongActionsSheet(
            request = request,
            onDismiss = { songActionRequest = null },
            onPlay = {
                songActionRequest = null
                playbackViewModel.play(request.song)
            },
            onPlayNext = {
                songActionRequest = null
                if (playbackViewModel.playNextOnline(request.song)) {
                    snackbarController.show(queueNextMessage)
                }
            },
            onAppendToQueue = {
                songActionRequest = null
                playbackViewModel.appendOnline(request.song)
                snackbarController.show(queueAddedMessage)
            },
            onAddToPlaylist = {
                songActionRequest = null
                if (authState is AuthState.Authenticated) {
                    myViewModel.preparePlaylistAddition()
                    playlistPickerSong = request.song
                } else if (backStack.lastOrNull() !is LoginGateNavKey) {
                    backStack.add(LoginGateNavKey(sessionExpired = false))
                }
            },
            onShowInfo = {
                songActionRequest = null
                infoSong = request.song
            },
            onShareUnavailable = {
                songActionRequest = null
                snackbarController.show(shareUnavailableMessage)
            },
        )
    }

    playlistPickerSong?.let { song ->
        PlaylistPickerSheet(
            state = myState,
            song = song,
            onDismiss = {
                playlistPickerSong = null
                myViewModel.dismissPlaylistAdditionFailure()
            },
            onRetryPlaylists = myViewModel::retryPlaylists,
            onPlaylistClick = { myViewModel.addSongToPlaylist(it, song) },
            onDismissFailure = myViewModel::dismissPlaylistAdditionFailure,
        )
    }

    infoSong?.let { song ->
        OnlineSongInfoDialog(song = song, onDismiss = { infoSong = null })
    }
}

internal fun MutableList<NavKey>.leaveLocalMusic(key: LocalMusicNavKey): Boolean {
    if (key.finishTaskOnBack) return true
    if (lastOrNull() == key) removeAt(lastIndex)
    return false
}

internal fun NavKey?.hasPrimaryNavigation(): Boolean = this is TabsShellNavKey

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

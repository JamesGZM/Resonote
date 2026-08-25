package com.resonote.app

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.defaultPopTransitionSpec
import com.resonote.core.model.AuthState
import com.resonote.core.model.OnlineSong
import com.resonote.core.navigation.LoginGateNavKey
import com.resonote.core.navigation.RiskVerificationContinuation
import com.resonote.core.navigation.RiskVerificationNavKey
import com.resonote.core.navigation.TabsShellNavKey
import com.resonote.core.playback.PlaybackState
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
import com.resonote.feature.library.impl.FollowingRoute
import com.resonote.feature.library.impl.MyViewModel
import com.resonote.feature.local.api.LocalMusicNavKey
import com.resonote.feature.local.impl.LocalMusicRoute
import com.resonote.feature.player.api.PlayerNavKey
import com.resonote.feature.player.impl.PlayerPaletteSeed
import com.resonote.feature.player.impl.PlayerRoute
import com.resonote.feature.player.impl.PlayerViewModel
import com.resonote.feature.playlist.api.PlaylistNavKey
import com.resonote.feature.playlist.impl.PlaylistRoute
import com.resonote.feature.ranking.api.RankingNavKey
import com.resonote.feature.ranking.impl.RankingRoute
import com.resonote.feature.recognition.api.RecognitionNavKey
import com.resonote.feature.recognition.impl.RecognitionRoute
import com.resonote.feature.risk.impl.RiskVerificationRoute
import com.resonote.feature.search.api.SearchNavKey
import com.resonote.feature.search.impl.SearchRoute
import com.resonote.feature.settings.api.AboutSettingsNavKey
import com.resonote.feature.settings.api.LicenseSettingsNavKey
import com.resonote.feature.settings.api.LyricsSettingsNavKey
import com.resonote.feature.settings.api.OpenSourceLibrariesNavKey
import com.resonote.feature.settings.api.PermissionsSettingsNavKey
import com.resonote.feature.settings.api.PlaybackSettingsNavKey
import com.resonote.feature.settings.api.PrivacySettingsNavKey
import com.resonote.feature.settings.api.SettingsNavKey
import com.resonote.feature.settings.impl.AboutSettingsRoute
import com.resonote.feature.settings.impl.LicenseSettingsRoute
import com.resonote.feature.settings.impl.LyricsSettingsRoute
import com.resonote.feature.settings.impl.OpenSourceLibrariesRoute
import com.resonote.feature.settings.impl.PermissionsSettingsRoute
import com.resonote.feature.settings.impl.PlaybackSettingsRoute
import com.resonote.feature.settings.impl.PrivacySettingsRoute
import com.resonote.feature.settings.impl.SettingsRoute
import com.resonote.feature.video.api.VideoNavKey
import com.resonote.feature.video.impl.VideoRoute

private const val PLAYER_EXPAND_DURATION_MILLIS = 320
private const val PLAYER_COLLAPSE_DURATION_MILLIS = 260
private val PlayerExpandEasing = CubicBezierEasing(0.28f, 0.10f, 0.82f, 0.52f)

@Composable
internal fun ResonoteNavDisplay(
    backStack: NavBackStack<NavKey>,
    tabsShellState: TabsShellState,
    playbackState: PlaybackState,
    playerPaletteSeed: PlayerPaletteSeed?,
    playerViewModel: PlayerViewModel,
    playerTransitionOrigin: Rect?,
    standaloneBottomContentPadding: Dp,
    authState: AuthState,
    externalImportRequest: ExternalLocalImportRequest?,
    viewModel: MainActivityViewModel,
    playbackViewModel: PlaybackViewModel,
    myViewModel: MyViewModel,
    onFinishExternalTask: () -> Unit,
    onOpenSongActions: (OnlineSong, (() -> Unit)?) -> Unit,
    onOpenPlaylistPicker: (OnlineSong) -> Unit,
    onOpenSongInfo: (OnlineSong) -> Unit,
    onOpenDailyVip: () -> Unit,
    onTabBarInsetChanged: (Dp) -> Unit,
    onVideoFullscreenChange: (Boolean) -> Unit,
    completedLoginRiskHandle: String?,
    onLoginRiskHandled: () -> Unit,
    onLoginRiskVerified: (String) -> Unit,
    onDailyVipRiskVerified: (String) -> Unit,
) {
    NavDisplay(
        backStack = backStack,
        onBack = {
            backStack.popCurrentDestination(viewModel::acknowledgeAuthenticationGate)
        },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        predictivePopTransitionSpec = { _ ->
            defaultPopTransitionSpec<NavKey>().invoke(this)
        },
        entryProvider = entryProvider {
            entry<TabsShellNavKey> {
                TabsShell(
                    tabsShellState = tabsShellState,
                    isActiveDestination = backStack.lastOrNull() == TabsShellNavKey,
                    playbackState = playbackState,
                    onPlaySong = playbackViewModel::play,
                    onPlaySongs = playbackViewModel::playAll,
                    myViewModel = myViewModel,
                    onLoginRequest = {
                        if (backStack.lastOrNull() !is LoginGateNavKey) {
                            backStack.add(LoginGateNavKey(sessionExpired = false))
                        }
                    },
                    onSearchClick = { backStack.add(SearchNavKey(sessionId = System.nanoTime())) },
                    onRecognitionClick = {
                        backStack.add(RecognitionNavKey(sessionId = System.nanoTime()))
                    },
                    onSongMoreClick = { onOpenSongActions(it, null) },
                    onDailyVipClick = {
                        if (authState is AuthState.Authenticated) {
                            onOpenDailyVip()
                        } else if (backStack.lastOrNull() !is LoginGateNavKey) {
                            backStack.add(LoginGateNavKey(sessionExpired = false))
                        }
                    },
                    onFollowingClick = {
                        if (authState is AuthState.Authenticated) {
                            backStack.add(FollowingNavKey)
                        } else if (backStack.lastOrNull() !is LoginGateNavKey) {
                            backStack.add(LoginGateNavKey(sessionExpired = false))
                        }
                    },
                    onHistoryClick = {
                        backStack.add(HistoryNavKey(initialTab = HistoryTab.Online))
                    },
                    onCloudClick = { backStack.navigateToCloud(authState) },
                    onLocalMusicClick = { backStack.add(LocalMusicNavKey()) },
                    onSettingsClick = { backStack.add(SettingsNavKey) },
                    onBottomBarInsetChanged = onTabBarInsetChanged,
                    onPlaylistClick = { playlist ->
                        backStack.add(
                            PlaylistNavKey(
                                playlistId = playlist.id,
                                title = playlist.title,
                                coverUrl = playlist.coverUrl,
                            ),
                        )
                    },
                    onUserPlaylistClick = { playlist ->
                        val accountId = (authState as? AuthState.Authenticated)?.userId
                        val canWrite = playlist.isMine && accountId != null
                        backStack.add(
                            PlaylistNavKey(
                                playlistId = playlist.globalId,
                                title = playlist.name,
                                coverUrl = playlist.coverUrl,
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
                    sessionId = key.sessionId,
                    initialQuery = key.initialQuery,
                    initialTab = key.initialTab,
                    playingMediaId = playbackState.currentMetadata?.mediaId,
                    bottomContentPadding = standaloneBottomContentPadding,
                    onBack = { backStack.popIfCurrent(key) },
                    onRecognitionClick = {
                        backStack.add(RecognitionNavKey(sessionId = System.nanoTime()))
                    },
                    onSongClick = playbackViewModel::play,
                    onSongMoreClick = { onOpenSongActions(it, null) },
                    onPlaylistClick = { playlist ->
                        backStack.add(
                            PlaylistNavKey(
                                playlistId = playlist.id,
                                title = playlist.name,
                                coverUrl = playlist.coverUrl,
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
                    onArtistClick = { artist ->
                        backStack.add(
                            ArtistNavKey(
                                artistId = artist.id,
                                name = artist.name,
                                avatarUrl = artist.avatarUrl,
                                songCount = artist.songCount,
                                albumCount = artist.albumCount,
                                sessionId = System.nanoTime(),
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
            entry<FollowingNavKey> { key ->
                FollowingRoute(
                    bottomContentPadding = standaloneBottomContentPadding,
                    onBack = { backStack.popIfCurrent(key) },
                    onArtistClick = { artist ->
                        backStack.add(
                            ArtistNavKey(
                                artistId = artist.id,
                                name = artist.name,
                                avatarUrl = artist.avatarUrl,
                            ),
                        )
                    },
                )
            }
            entry<PlayerNavKey>(
                metadata = NavDisplay.transitionSpec {
                    fadeIn(animationSpec = tween(PLAYER_EXPAND_DURATION_MILLIS), initialAlpha = 1f) togetherWith
                        fadeOut(animationSpec = tween(PLAYER_EXPAND_DURATION_MILLIS), targetAlpha = 1f)
                } + NavDisplay.popTransitionSpec {
                    fadeIn(animationSpec = tween(PLAYER_COLLAPSE_DURATION_MILLIS), initialAlpha = 1f) togetherWith
                        fadeOut(animationSpec = tween(PLAYER_COLLAPSE_DURATION_MILLIS), targetAlpha = 1f)
                } + NavDisplay.predictivePopTransitionSpec { _ ->
                    fadeIn(animationSpec = tween(PLAYER_COLLAPSE_DURATION_MILLIS), initialAlpha = 1f) togetherWith
                        fadeOut(animationSpec = tween(PLAYER_COLLAPSE_DURATION_MILLIS), targetAlpha = 1f)
                },
            ) { key ->
                val playerTransition = LocalNavAnimatedContentScope.current.transition
                val playerTransitionProgress = playerTransition.animateFloat(
                    transitionSpec = {
                        if (targetState == EnterExitState.Visible) {
                            tween(PLAYER_EXPAND_DURATION_MILLIS, easing = PlayerExpandEasing)
                        } else {
                            tween(PLAYER_COLLAPSE_DURATION_MILLIS, easing = LinearOutSlowInEasing)
                        }
                    },
                    label = "Player container reveal",
                ) { state ->
                    if (state == EnterExitState.Visible) 1f else 0f
                }
                val playerTransitionProgressProvider = remember(playerTransitionProgress) {
                    { playerTransitionProgress.value }
                }
                PlayerRoute(
                    viewModel = playerViewModel,
                    onBack = { backStack.popIfCurrent(key) },
                    onPlayNextClick = { playbackViewModel.playNextOnline(it) },
                    onAppendToQueueClick = playbackViewModel::appendOnline,
                    onAddToPlaylistClick = onOpenPlaylistPicker,
                    onSongInfoClick = onOpenSongInfo,
                    onLoginRequest = {
                        if (backStack.lastOrNull() !is LoginGateNavKey) {
                            backStack.add(LoginGateNavKey(sessionExpired = false))
                        }
                    },
                    onLyricsSettingsClick = { backStack.add(LyricsSettingsNavKey) },
                    paletteSeed = playerPaletteSeed,
                    containerTransitionRunning = playerTransition.currentState != playerTransition.targetState,
                    containerTransitionOrigin = playerTransitionOrigin,
                    containerTransitionProgress = playerTransitionProgressProvider,
                )
            }
            entry<SettingsNavKey> { key ->
                SettingsRoute(
                    onBack = { backStack.popIfCurrent(key) },
                    onPlaybackClick = { backStack.add(PlaybackSettingsNavKey) },
                    onLyricsClick = { backStack.add(LyricsSettingsNavKey) },
                    onPermissionsClick = { backStack.add(PermissionsSettingsNavKey) },
                    onAboutClick = { backStack.add(AboutSettingsNavKey) },
                    bottomContentPadding = standaloneBottomContentPadding,
                )
            }
            entry<PlaybackSettingsNavKey> { key ->
                PlaybackSettingsRoute(
                    onBack = { backStack.popIfCurrent(key) },
                    bottomContentPadding = standaloneBottomContentPadding,
                )
            }
            entry<LyricsSettingsNavKey> { key ->
                LyricsSettingsRoute(
                    onBack = { backStack.popIfCurrent(key) },
                    bottomContentPadding = standaloneBottomContentPadding,
                )
            }
            entry<PermissionsSettingsNavKey> { key ->
                PermissionsSettingsRoute(
                    onBack = { backStack.popIfCurrent(key) },
                    bottomContentPadding = standaloneBottomContentPadding,
                )
            }
            entry<AboutSettingsNavKey> { key ->
                AboutSettingsRoute(
                    onBack = { backStack.popIfCurrent(key) },
                    onPrivacyClick = { backStack.add(PrivacySettingsNavKey) },
                    onLicenseClick = { backStack.add(LicenseSettingsNavKey) },
                    onLibrariesClick = { backStack.add(OpenSourceLibrariesNavKey) },
                    bottomContentPadding = standaloneBottomContentPadding,
                )
            }
            entry<PrivacySettingsNavKey> { key ->
                PrivacySettingsRoute(
                    onBack = { backStack.popIfCurrent(key) },
                    bottomContentPadding = standaloneBottomContentPadding,
                )
            }
            entry<LicenseSettingsNavKey> { key ->
                LicenseSettingsRoute(
                    onBack = { backStack.popIfCurrent(key) },
                    bottomContentPadding = standaloneBottomContentPadding,
                )
            }
            entry<OpenSourceLibrariesNavKey> { key ->
                OpenSourceLibrariesRoute(
                    onBack = { backStack.popIfCurrent(key) },
                    bottomContentPadding = standaloneBottomContentPadding,
                )
            }
            entry<PlaylistNavKey> { key ->
                PlaylistRoute(
                    key = key,
                    playingMediaId = playbackState.currentMetadata?.mediaId,
                    bottomContentPadding = standaloneBottomContentPadding,
                    currentAccountId = (authState as? AuthState.Authenticated)?.userId,
                    onBack = { backStack.popIfCurrent(key) },
                    onLoginRequest = {
                        if (backStack.lastOrNull() !is LoginGateNavKey) {
                            backStack.add(LoginGateNavKey(sessionExpired = false))
                        }
                    },
                    onPlayAll = { playbackViewModel.playAll(it) },
                    onSongClick = playbackViewModel::play,
                    onSongMoreClick = onOpenSongActions,
                )
            }
            entry<AlbumNavKey> { key ->
                AlbumRoute(
                    key = key,
                    playingMediaId = playbackState.currentMetadata?.mediaId,
                    bottomContentPadding = standaloneBottomContentPadding,
                    onBack = { backStack.popIfCurrent(key) },
                    onPlayAll = playbackViewModel::playAll,
                    onSongClick = playbackViewModel::play,
                    onSongMoreClick = { onOpenSongActions(it, null) },
                )
            }
            entry<ArtistNavKey> { key ->
                ArtistRoute(
                    key = key,
                    currentAccountId = (authState as? AuthState.Authenticated)?.userId,
                    playingMediaId = playbackState.currentMetadata?.mediaId,
                    bottomContentPadding = standaloneBottomContentPadding,
                    onBack = { backStack.popIfCurrent(key) },
                    onLoginRequest = {
                        if (backStack.lastOrNull() !is LoginGateNavKey) {
                            backStack.add(LoginGateNavKey(sessionExpired = false))
                        }
                    },
                    onPlayAll = playbackViewModel::playAll,
                    onSongClick = playbackViewModel::play,
                    onSongMoreClick = { onOpenSongActions(it, null) },
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
                    onVideoClick = { video ->
                        playbackViewModel.pause()
                        backStack.add(
                            VideoNavKey(
                                hash = video.hash,
                                title = video.name,
                                singer = video.singer,
                                coverUrl = video.coverUrl,
                                durationMillis = video.durationMillis,
                            ),
                        )
                    },
                )
            }
            entry<RankingNavKey> { key ->
                RankingRoute(
                    key = key,
                    playingMediaId = playbackState.currentMetadata?.mediaId,
                    bottomContentPadding = standaloneBottomContentPadding,
                    onBack = { backStack.popIfCurrent(key) },
                    onPlayAll = playbackViewModel::playAll,
                    onSongClick = playbackViewModel::play,
                    onSongMoreClick = { onOpenSongActions(it, null) },
                )
            }
            entry<CloudNavKey> { key ->
                CloudRoute(
                    playingMediaId = playbackState.currentMetadata?.mediaId,
                    bottomContentPadding = standaloneBottomContentPadding,
                    onBack = { backStack.popIfCurrent(key) },
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
                    bottomContentPadding = standaloneBottomContentPadding,
                    onBack = { backStack.popIfCurrent(key) },
                    onLoginRequest = {
                        if (backStack.lastOrNull() !is LoginGateNavKey) {
                            backStack.add(LoginGateNavKey(sessionExpired = false))
                        }
                    },
                    onPlayOnline = playbackViewModel::playAll,
                    onSongMoreClick = { onOpenSongActions(it, null) },
                    onPlayDevice = playbackViewModel::playDeviceHistory,
                )
            }
            entry<LocalMusicNavKey> { key ->
                LocalMusicRoute(
                    playingMediaId = playbackState.currentMetadata?.mediaId,
                    bottomContentPadding = standaloneBottomContentPadding,
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
                    onBack = { backStack.popIfCurrent(key) },
                    onFullscreenChange = onVideoFullscreenChange,
                )
            }
            entry<RecognitionNavKey> { key ->
                RecognitionRoute(
                    onBack = { backStack.popIfCurrent(key) },
                    onCaptureStarted = playbackViewModel::pause,
                    onPlay = playbackViewModel::play,
                    onSearch = { match ->
                        val query = listOfNotNull(match.song.title, match.song.artist)
                            .filter(String::isNotBlank)
                            .joinToString(" ")
                        backStack.add(SearchNavKey(sessionId = System.nanoTime(), initialQuery = query))
                    },
                    onAddToPlaylist = onOpenPlaylistPicker,
                )
            }
            entry<LoginGateNavKey> { key ->
                LoginRoute(
                    sessionExpired = key.sessionExpired,
                    completedRiskHandle = completedLoginRiskHandle,
                    onRiskHandleConsumed = onLoginRiskHandled,
                    onRiskVerificationRequired = { challenge ->
                        backStack.add(
                            RiskVerificationNavKey(
                                challengeHandle = challenge.value,
                                continuation = RiskVerificationContinuation.Login,
                            ),
                        )
                    },
                    onBack = {
                        if (backStack.popIfCurrent(key)) {
                            viewModel.acknowledgeAuthenticationGate()
                        }
                    },
                )
            }
            entry<RiskVerificationNavKey> { key ->
                RiskVerificationRoute(
                    key = key,
                    onBack = { backStack.popIfCurrent(key) },
                    onVerified = {
                        if (backStack.popIfCurrent(key)) {
                            if (key.continuation == RiskVerificationContinuation.Login) {
                                onLoginRiskVerified(key.challengeHandle)
                            } else if (key.continuation == RiskVerificationContinuation.DailyVip) {
                                onDailyVipRiskVerified(key.challengeHandle)
                            }
                        }
                    },
                )
            }
        },
    )
}

internal fun MutableList<NavKey>.leaveLocalMusic(key: LocalMusicNavKey): Boolean {
    if (key.finishTaskOnBack) return true
    popIfCurrent(key)
    return false
}

internal fun MutableList<NavKey>.popIfCurrent(key: NavKey): Boolean {
    if (key == TabsShellNavKey || lastOrNull() != key) return false
    removeAt(lastIndex)
    return true
}

internal fun MutableList<NavKey>.popCurrentDestination(onAuthenticationGateDismissed: () -> Unit): Boolean {
    val current = lastOrNull() ?: return false
    val popped = popIfCurrent(current)
    if (popped && current is LoginGateNavKey) onAuthenticationGateDismissed()
    return popped
}

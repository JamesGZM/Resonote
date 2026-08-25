package com.resonote.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
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

@Composable
internal fun ResonoteNavDisplay(
    backStack: NavBackStack<NavKey>,
    tabsShellState: TabsShellState,
    playbackState: PlaybackState,
    playerPaletteSeed: PlayerPaletteSeed?,
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
                    onBack = { backStack.popResonoteDestination() },
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
            entry<FollowingNavKey> {
                FollowingRoute(
                    bottomContentPadding = standaloneBottomContentPadding,
                    onBack = { backStack.popResonoteDestination() },
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
                metadata = NavDisplay.popTransitionSpec {
                    fadeIn(animationSpec = tween(420)) togetherWith
                        (
                            scaleOut(
                                targetScale = 0.18f,
                                transformOrigin = TransformOrigin(0.5f, 0.94f),
                                animationSpec = tween(460, easing = FastOutSlowInEasing),
                            ) + fadeOut(animationSpec = tween(360, delayMillis = 100))
                            )
                } + NavDisplay.predictivePopTransitionSpec { _ ->
                    fadeIn(animationSpec = tween(420)) togetherWith
                        (
                            scaleOut(
                                targetScale = 0.18f,
                                transformOrigin = TransformOrigin(0.5f, 0.94f),
                                animationSpec = tween(460, easing = FastOutSlowInEasing),
                            ) + fadeOut(animationSpec = tween(360, delayMillis = 100))
                            )
                },
            ) {
                PlayerRoute(
                    onBack = { backStack.popResonoteDestination() },
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
                )
            }
            entry<SettingsNavKey> {
                SettingsRoute(
                    onBack = { backStack.popResonoteDestination() },
                    onPlaybackClick = { backStack.add(PlaybackSettingsNavKey) },
                    onLyricsClick = { backStack.add(LyricsSettingsNavKey) },
                    onPermissionsClick = { backStack.add(PermissionsSettingsNavKey) },
                    onAboutClick = { backStack.add(AboutSettingsNavKey) },
                    bottomContentPadding = standaloneBottomContentPadding,
                )
            }
            entry<PlaybackSettingsNavKey> {
                PlaybackSettingsRoute(
                    onBack = { backStack.popResonoteDestination() },
                    bottomContentPadding = standaloneBottomContentPadding,
                )
            }
            entry<LyricsSettingsNavKey> {
                LyricsSettingsRoute(
                    onBack = { backStack.popResonoteDestination() },
                    bottomContentPadding = standaloneBottomContentPadding,
                )
            }
            entry<PermissionsSettingsNavKey> {
                PermissionsSettingsRoute(
                    onBack = { backStack.popResonoteDestination() },
                    bottomContentPadding = standaloneBottomContentPadding,
                )
            }
            entry<AboutSettingsNavKey> {
                AboutSettingsRoute(
                    onBack = { backStack.popResonoteDestination() },
                    onPrivacyClick = { backStack.add(PrivacySettingsNavKey) },
                    onLicenseClick = { backStack.add(LicenseSettingsNavKey) },
                    onLibrariesClick = { backStack.add(OpenSourceLibrariesNavKey) },
                    bottomContentPadding = standaloneBottomContentPadding,
                )
            }
            entry<PrivacySettingsNavKey> {
                PrivacySettingsRoute(
                    onBack = { backStack.popResonoteDestination() },
                    bottomContentPadding = standaloneBottomContentPadding,
                )
            }
            entry<LicenseSettingsNavKey> {
                LicenseSettingsRoute(
                    onBack = { backStack.popResonoteDestination() },
                    bottomContentPadding = standaloneBottomContentPadding,
                )
            }
            entry<OpenSourceLibrariesNavKey> {
                OpenSourceLibrariesRoute(
                    onBack = { backStack.popResonoteDestination() },
                    bottomContentPadding = standaloneBottomContentPadding,
                )
            }
            entry<PlaylistNavKey> { key ->
                PlaylistRoute(
                    key = key,
                    playingMediaId = playbackState.currentMetadata?.mediaId,
                    bottomContentPadding = standaloneBottomContentPadding,
                    currentAccountId = (authState as? AuthState.Authenticated)?.userId,
                    onBack = { backStack.popResonoteDestination() },
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
                    onBack = { backStack.popResonoteDestination() },
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
                    onBack = { backStack.popResonoteDestination() },
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
                    onBack = { backStack.popResonoteDestination() },
                    onPlayAll = playbackViewModel::playAll,
                    onSongClick = playbackViewModel::play,
                    onSongMoreClick = { onOpenSongActions(it, null) },
                )
            }
            entry<CloudNavKey> {
                CloudRoute(
                    playingMediaId = playbackState.currentMetadata?.mediaId,
                    bottomContentPadding = standaloneBottomContentPadding,
                    onBack = { backStack.popResonoteDestination() },
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
                    onBack = { backStack.popResonoteDestination() },
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
                    onBack = { backStack.popResonoteDestination() },
                    onFullscreenChange = onVideoFullscreenChange,
                )
            }
            entry<RecognitionNavKey> {
                RecognitionRoute(
                    onBack = { backStack.popResonoteDestination() },
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
                        if (backStack.lastOrNull() is LoginGateNavKey) backStack.popResonoteDestination()
                        viewModel.acknowledgeAuthenticationGate()
                    },
                )
            }
            entry<RiskVerificationNavKey> { key ->
                RiskVerificationRoute(
                    key = key,
                    onBack = { backStack.popResonoteDestination() },
                    onVerified = {
                        backStack.popResonoteDestination()
                        if (key.continuation == RiskVerificationContinuation.Login) {
                            onLoginRiskVerified(key.challengeHandle)
                        } else if (key.continuation == RiskVerificationContinuation.DailyVip) {
                            onDailyVipRiskVerified(key.challengeHandle)
                        }
                    },
                )
            }
        },
    )
}

internal fun MutableList<NavKey>.leaveLocalMusic(key: LocalMusicNavKey): Boolean {
    if (key.finishTaskOnBack) return true
    if (lastOrNull() == key) popResonoteDestination()
    return false
}

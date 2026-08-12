package com.resonote.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.resonote.core.navigation.LoginGateNavKey
import com.resonote.core.navigation.TabsShellNavKey
import com.resonote.core.model.AuthState
import com.resonote.feature.album.api.AlbumNavKey
import com.resonote.feature.album.impl.AlbumRoute
import com.resonote.feature.artist.api.ArtistNavKey
import com.resonote.feature.artist.impl.ArtistRoute
import com.resonote.feature.auth.impl.LoginRoute
import com.resonote.feature.cloud.api.CloudNavKey
import com.resonote.feature.cloud.impl.CloudRoute
import com.resonote.feature.library.impl.MyViewModel
import com.resonote.feature.playlist.api.PlaylistNavKey
import com.resonote.feature.playlist.impl.PlaylistRoute
import com.resonote.feature.ranking.api.RankingNavKey
import com.resonote.feature.ranking.impl.RankingRoute
import com.resonote.feature.recognition.api.RecognitionNavKey
import com.resonote.feature.recognition.impl.RecognitionRoute
import com.resonote.feature.search.api.SearchNavKey
import com.resonote.feature.search.impl.SearchRoute
import com.resonote.feature.vip.api.DailyVipNavKey
import com.resonote.feature.vip.impl.DailyVipRoute
import com.resonote.feature.video.api.VideoNavKey
import com.resonote.feature.video.impl.VideoRoute

@Composable
internal fun ResonoteApp(viewModel: MainActivityViewModel = hiltViewModel()) {
    val backStack = rememberNavBackStack(TabsShellNavKey)
    val playbackState = rememberPrototypePlaybackState()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val myViewModel: MyViewModel = hiltViewModel()
    val setVideoFullscreen = rememberVideoFullscreenController()

    LaunchedEffect(authState) {
        backStack.synchronizeAuthenticationGate(authState)
    }

    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<TabsShellNavKey> {
                TabsShell(
                    playbackState = playbackState,
                    myViewModel = myViewModel,
                    onLoginRequest = {
                        if (backStack.lastOrNull() !is LoginGateNavKey) {
                            backStack.add(LoginGateNavKey(sessionExpired = false))
                        }
                    },
                    onSearchClick = { backStack.add(SearchNavKey()) },
                    onRecognitionClick = { backStack.add(RecognitionNavKey) },
                    onDailyVipClick = { backStack.navigateToDailyVip(authState) },
                    onCloudClick = { backStack.navigateToCloud(authState) },
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
                    onSongClick = playbackState::play,
                    onSongMoreClick = null,
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
                        playbackState.pauseForVideo()
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
            entry<PlaylistNavKey> { key ->
                PlaylistRoute(
                    playlistId = key.playlistId,
                    playingMediaId = playbackState.currentSongId,
                    isAuthenticated = authState is AuthState.Authenticated,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onPlayAll = { playbackState.playAll(it) },
                    onSongClick = playbackState::play,
                    onSongMoreClick = null,
                )
            }
            entry<AlbumNavKey> { key ->
                AlbumRoute(
                    key = key,
                    playingMediaId = playbackState.currentSongId,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onPlayAll = playbackState::playAll,
                    onSongClick = playbackState::play,
                    onSongMoreClick = null,
                )
            }
            entry<ArtistNavKey> { key ->
                ArtistRoute(
                    key = key,
                    playingMediaId = playbackState.currentSongId,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onPlayAll = playbackState::playAll,
                    onSongClick = playbackState::play,
                    onSongMoreClick = null,
                )
            }
            entry<RankingNavKey> { key ->
                RankingRoute(
                    key = key,
                    playingMediaId = playbackState.currentSongId,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onPlayAll = playbackState::playAll,
                    onSongClick = playbackState::play,
                    onSongMoreClick = null,
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
                    playingMediaId = playbackState.currentSongId,
                    bottomContentPadding = if (playbackState.currentSong == null) 32.dp else 120.dp,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onPlayRequest = playbackState::playCloud,
                    onAppendTracks = playbackState::appendCloud,
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
                    onCaptureStarted = playbackState::pauseForRecognition,
                    onPlay = playbackState::play,
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

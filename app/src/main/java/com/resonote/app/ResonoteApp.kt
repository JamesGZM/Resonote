package com.resonote.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.resonote.core.navigation.LoginGateNavKey
import com.resonote.core.navigation.TabsShellNavKey
import com.resonote.core.model.AuthState
import com.resonote.feature.playlist.api.PlaylistNavKey
import com.resonote.feature.playlist.impl.PlaylistRoute
import com.resonote.feature.search.api.SearchNavKey
import com.resonote.feature.search.impl.SearchRoute

@Composable
internal fun ResonoteApp(viewModel: MainActivityViewModel = hiltViewModel()) {
    val backStack = rememberNavBackStack(TabsShellNavKey)
    val playbackState = rememberPrototypePlaybackState()
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    LaunchedEffect(authState) {
        backStack.synchronizeAuthenticationGate(authState)
    }

    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<TabsShellNavKey> {
                TabsShell(
                    playbackState = playbackState,
                    onSearchClick = { backStack.add(SearchNavKey()) },
                    onPlaylistClick = { backStack.add(PlaylistNavKey(it)) },
                )
            }
            entry<SearchNavKey> { key ->
                SearchRoute(
                    initialQuery = key.initialQuery,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onRecognitionClick = null,
                    onSongClick = playbackState::play,
                    onSongMoreClick = null,
                    onPlaylistClick = { backStack.add(PlaylistNavKey(it)) },
                    onAlbumClick = null,
                    onArtistClick = null,
                    onMvClick = null,
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
            entry<LoginGateNavKey> { key ->
                LoginGateScreen(
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

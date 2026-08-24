package com.resonote.feature.library.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.LocalResonoteSnackbarController
import com.resonote.core.designsystem.component.ResonotePullToRefreshBox
import com.resonote.core.model.UserPlaylist

@Composable
fun MyRoute(
    bottomContentPadding: Dp,
    onLoginClick: () -> Unit,
    onDailyVipClick: () -> Unit,
    onFollowingClick: () -> Unit = {},
    onHistoryClick: () -> Unit,
    onCloudClick: () -> Unit,
    onLocalMusicClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onPlaylistClick: (UserPlaylist) -> Unit,
    viewModel: MyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarController = LocalResonoteSnackbarController.current
    val refreshFailureMessage = stringResource(R.string.feature_library_impl_my_refresh_failed)
    LaunchedEffect(viewModel, snackbarController) {
        viewModel.refreshFailures.collect { snackbarController?.show(refreshFailureMessage) }
    }
    MyScreen(
        state = state,
        bottomContentPadding = bottomContentPadding,
        onLoginClick = onLoginClick,
        onDailyVipClick = onDailyVipClick,
        onFollowingClick = onFollowingClick,
        onHistoryClick = onHistoryClick,
        onCloudClick = onCloudClick,
        onLocalMusicClick = onLocalMusicClick,
        onSettingsClick = onSettingsClick,
        onRefresh = viewModel::refresh,
        onRetryProfile = viewModel::retryProfile,
        onRetryPlaylists = viewModel::retryPlaylists,
        onCreatePlaylist = viewModel::createPlaylist,
        onDismissPlaylistCreation = viewModel::dismissPlaylistCreation,
        onAcknowledgePlaylistCreation = viewModel::acknowledgePlaylistCreation,
        onPlaylistClick = onPlaylistClick,
    )
}

@Composable
internal fun MyScreen(
    state: MyUiState,
    bottomContentPadding: Dp,
    onLoginClick: () -> Unit,
    onDailyVipClick: () -> Unit,
    onFollowingClick: () -> Unit = {},
    onHistoryClick: () -> Unit,
    onCloudClick: () -> Unit,
    onLocalMusicClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRefresh: () -> Unit,
    onRetryProfile: () -> Unit,
    onRetryPlaylists: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDismissPlaylistCreation: () -> Unit,
    onAcknowledgePlaylistCreation: () -> Unit,
    onPlaylistClick: (UserPlaylist) -> Unit,
    modifier: Modifier = Modifier,
) {
    val authenticated = state as? MyUiState.Authenticated
    val accountKey = authenticated?.userId
    var createDialogOpen by rememberSaveable(accountKey) { mutableStateOf(false) }
    var playlistName by rememberSaveable(accountKey) { mutableStateOf("") }
    var selectedPlaylistGroup by rememberSaveable(accountKey) { mutableIntStateOf(0) }
    val snackbarController = LocalResonoteSnackbarController.current
    val creationState = authenticated?.playlistCreation ?: PlaylistCreationUiState.Idle
    val creationMessage = when (val creation = creationState) {
        is PlaylistCreationUiState.Created -> stringResource(
            if (creation.refreshFailed) {
                R.string.feature_library_impl_create_playlist_success_refresh_failed
            } else {
                R.string.feature_library_impl_create_playlist_success
            },
            creation.name,
        )
        else -> ""
    }

    LaunchedEffect(creationState) {
        if (creationState is PlaylistCreationUiState.Created) {
            createDialogOpen = false
            playlistName = ""
            snackbarController?.show(creationMessage)
            onAcknowledgePlaylistCreation()
        }
    }

    ResonotePullToRefreshBox(
        isRefreshing = authenticated?.isRefreshing == true,
        onRefresh = onRefresh,
        enabled = authenticated?.hasRefreshableContent() == true,
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag("my-pull-to-refresh"),
    ) {
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxSize().testTag("my-list"),
            contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, bottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (state) {
                MyUiState.CheckingAccount -> item { MyScreenSkeleton(bottomContentPadding) }
                MyUiState.Anonymous -> anonymousContent(
                    onLoginClick,
                    onHistoryClick,
                    onCloudClick,
                    onLocalMusicClick,
                    onSettingsClick,
                )
                is MyUiState.Authenticated -> authenticatedContent(
                    state = state,
                    selectedPlaylistGroup = selectedPlaylistGroup,
                    onSelectPlaylistGroup = { selectedPlaylistGroup = it },
                    onRetryProfile = onRetryProfile,
                    onRetryPlaylists = onRetryPlaylists,
                    onDailyVipClick = onDailyVipClick,
                    onFollowingClick = onFollowingClick,
                    onHistoryClick = onHistoryClick,
                    onCloudClick = onCloudClick,
                    onLocalMusicClick = onLocalMusicClick,
                    onSettingsClick = onSettingsClick,
                    onCreatePlaylistClick = {
                        onDismissPlaylistCreation()
                        playlistName = ""
                        createDialogOpen = true
                    },
                    onPlaylistClick = onPlaylistClick,
                )
            }
        }
    }

    if (createDialogOpen && authenticated != null) {
        PlaylistCreationDialog(
            name = playlistName,
            state = creationState,
            onNameChange = { playlistName = it },
            onDismiss = {
                if (creationState != PlaylistCreationUiState.Submitting) {
                    createDialogOpen = false
                    playlistName = ""
                    onDismissPlaylistCreation()
                }
            },
            onConfirm = { onCreatePlaylist(playlistName) },
        )
    }
}

private fun MyUiState.Authenticated.hasRefreshableContent() =
    profile is MySectionState.Available || playlists is MySectionState.Available

private fun LazyListScope.anonymousContent(
    onLoginClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onCloudClick: () -> Unit,
    onLocalMusicClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    item(key = "anonymous-account") { AnonymousAccountCard(onLoginClick, onSettingsClick) }
    item(key = "quick-entries") {
        QuickEntries(
            likedPlaylist = null,
            likedRequiresLogin = true,
            onLikedClick = onLoginClick,
            onHistoryClick = onHistoryClick,
            onCloudClick = onCloudClick,
            onLocalMusicClick = onLocalMusicClick,
        )
    }
    item(key = "anonymous-note") {
        Text(
            text = stringResource(R.string.feature_library_impl_my_anonymous_local_note),
            modifier = Modifier.padding(horizontal = 6.dp).testTag("my-anonymous"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun LazyListScope.authenticatedContent(
    state: MyUiState.Authenticated,
    selectedPlaylistGroup: Int,
    onSelectPlaylistGroup: (Int) -> Unit,
    onRetryProfile: () -> Unit,
    onRetryPlaylists: () -> Unit,
    onDailyVipClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onCloudClick: () -> Unit,
    onLocalMusicClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onPlaylistClick: (UserPlaylist) -> Unit,
) {
    item(key = "profile") {
        when (val profile = state.profile) {
            MySectionState.Loading -> ProfileSkeleton()
            is MySectionState.Available -> AccountCard(
                profile.value,
                onDailyVipClick,
                onFollowingClick,
                onSettingsClick,
            )
            is MySectionState.Failed -> SectionFailure(
                stringResource(R.string.feature_library_impl_my_profile_error),
                profile.failure,
                onRetryProfile,
                Modifier.testTag("my-profile-error"),
            )
        }
    }
    item(key = "quick-entries") {
        val liked = (state.playlists as? MySectionState.Available)?.value?.firstOrNull { it.isLike }
        QuickEntries(
            likedPlaylist = liked,
            likedRequiresLogin = false,
            onLikedClick = { liked?.let(onPlaylistClick) },
            onHistoryClick = onHistoryClick,
            onCloudClick = onCloudClick,
            onLocalMusicClick = onLocalMusicClick,
            modifier = Modifier.padding(vertical = 12.dp),
        )
    }
    item(key = "playlists") {
        PlaylistSection(
            state = state.playlists,
            selectedGroup = selectedPlaylistGroup,
            onSelectGroup = onSelectPlaylistGroup,
            onPlaylistClick = onPlaylistClick,
            onCreatePlaylistClick = onCreatePlaylistClick,
            onRetry = onRetryPlaylists,
            createEnabled = !state.isRefreshing &&
                state.playlistCreation != PlaylistCreationUiState.Submitting,
        )
    }
}

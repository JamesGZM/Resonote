@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.library.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.resonote.core.designsystem.component.ResonoteArtwork
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.ResonoteButton
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.UserPlaylist
import com.resonote.core.model.UserProfile
import kotlinx.coroutines.launch

@Composable
fun MyRoute(
    bottomContentPadding: Dp,
    onLoginClick: () -> Unit,
    onDailyVipClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onCloudClick: () -> Unit,
    onLocalMusicClick: () -> Unit,
    onPlaylistClick: (UserPlaylist) -> Unit,
    viewModel: MyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MyScreen(
        state = state,
        bottomContentPadding = bottomContentPadding,
        onLoginClick = onLoginClick,
        onDailyVipClick = onDailyVipClick,
        onHistoryClick = onHistoryClick,
        onCloudClick = onCloudClick,
        onLocalMusicClick = onLocalMusicClick,
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
    onHistoryClick: () -> Unit,
    onCloudClick: () -> Unit,
    onLocalMusicClick: () -> Unit,
    onRefresh: () -> Unit,
    onRetryProfile: () -> Unit,
    onRetryPlaylists: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDismissPlaylistCreation: () -> Unit,
    onAcknowledgePlaylistCreation: () -> Unit,
    onPlaylistClick: (UserPlaylist) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val authenticatedState = state as? MyUiState.Authenticated
    val accountKey = authenticatedState?.userId
    var createDialogOpen by rememberSaveable(accountKey) { mutableStateOf(false) }
    var playlistName by rememberSaveable(accountKey) { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val creationState = authenticatedState?.playlistCreation ?: PlaylistCreationUiState.Idle
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

    LaunchedEffect(accountKey) {
        snackbarHostState.currentSnackbarData?.dismiss()
    }

    LaunchedEffect(creationState) {
        if (creationState is PlaylistCreationUiState.Created) {
            createDialogOpen = false
            playlistName = ""
            snackbarScope.launch { snackbarHostState.showSnackbar(creationMessage) }
            onAcknowledgePlaylistCreation()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ResonoteTopAppBar(
                title = { Text(stringResource(R.string.my_title)) },
                actions = {
                    if (state is MyUiState.Authenticated) {
                        if (state.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(12.dp).size(24.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            IconButton(onClick = onRefresh) {
                                Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.my_refresh))
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).testTag("my-list"),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = bottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (state) {
                MyUiState.CheckingAccount -> checkingAccount()
                MyUiState.Anonymous -> anonymousAccount(
                    onLoginClick,
                    onDailyVipClick,
                    onHistoryClick,
                    onCloudClick,
                    onLocalMusicClick,
                )
                is MyUiState.Authenticated -> authenticatedAccount(
                    state = state,
                    onRetryProfile = onRetryProfile,
                    onRetryPlaylists = onRetryPlaylists,
                    onDailyVipClick = onDailyVipClick,
                    onHistoryClick = onHistoryClick,
                    onCloudClick = onCloudClick,
                    onLocalMusicClick = onLocalMusicClick,
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

    if (createDialogOpen && authenticatedState != null) {
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

private fun LazyListScope.checkingAccount() {
    item(key = "checking") {
        Column(
            modifier = Modifier.fillParentMaxHeight(0.7f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.my_checking_account),
                modifier = Modifier.padding(top = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun LazyListScope.anonymousAccount(
    onLoginClick: () -> Unit,
    onDailyVipClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onCloudClick: () -> Unit,
    onLocalMusicClick: () -> Unit,
) {
    item(key = "anonymous-hero") {
        Card(
            modifier = Modifier.fillMaxWidth().testTag("my-anonymous"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                            ),
                        ),
                    )
                    .padding(horizontal = 24.dp, vertical = 32.dp),
            ) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 38.dp, y = (-40).dp).size(150.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.22f),
                ) {}
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(28.dp))
                        }
                    }
                    Text(
                        text = stringResource(R.string.my_anonymous_title),
                        modifier = Modifier.padding(top = 24.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.my_anonymous_body),
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    ResonoteButton(
                        label = stringResource(R.string.my_login),
                        onClick = onLoginClick,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
        }
    }
    item(key = "daily-vip") {
        DailyVipEntryCard(onClick = onDailyVipClick, requiresLogin = true)
    }
    item(key = "history") {
        HistoryEntryCard(onClick = onHistoryClick)
    }
    item(key = "cloud") {
        CloudEntryCard(onClick = onCloudClick, requiresLogin = true)
    }
    item(key = "local-music") {
        LocalMusicEntryCard(onClick = onLocalMusicClick)
    }
    item(key = "anonymous-note") {
        Text(
            text = stringResource(R.string.my_anonymous_local_note),
            modifier = Modifier.padding(horizontal = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun LazyListScope.authenticatedAccount(
    state: MyUiState.Authenticated,
    onRetryProfile: () -> Unit,
    onRetryPlaylists: () -> Unit,
    onDailyVipClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onCloudClick: () -> Unit,
    onLocalMusicClick: () -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onPlaylistClick: (UserPlaylist) -> Unit,
) {
    item(key = "profile") {
        when (val profile = state.profile) {
            MySectionState.Loading -> ProfileLoadingCard()
            is MySectionState.Available -> ProfileCard(profile.value)
            is MySectionState.Failed -> SectionFailure(
                title = stringResource(R.string.my_profile_error),
                failure = profile.failure,
                onRetry = onRetryProfile,
                modifier = Modifier.testTag("my-profile-error"),
            )
        }
    }

    item(key = "daily-vip") {
        DailyVipEntryCard(onClick = onDailyVipClick, requiresLogin = false)
    }
    item(key = "history") {
        HistoryEntryCard(onClick = onHistoryClick)
    }
    item(key = "cloud") {
        CloudEntryCard(onClick = onCloudClick, requiresLogin = false)
    }
    item(key = "local-music") {
        LocalMusicEntryCard(onClick = onLocalMusicClick)
    }

    when (val playlists = state.playlists) {
        MySectionState.Loading -> item(key = "playlists-loading") { PlaylistsLoadingCard() }
        is MySectionState.Failed -> item(key = "playlists-error") {
            SectionFailure(
                title = stringResource(R.string.my_playlists_error),
                failure = playlists.failure,
                onRetry = onRetryPlaylists,
                modifier = Modifier.testTag("my-playlists-error"),
            )
        }
        is MySectionState.Available -> playlistSections(
            playlists = playlists.value,
            onPlaylistClick = onPlaylistClick,
            onCreatePlaylistClick = onCreatePlaylistClick,
            createEnabled = !state.isRefreshing && state.playlistCreation != PlaylistCreationUiState.Submitting,
        )
    }
}

@Composable
private fun PlaylistCreationDialog(
    name: String,
    state: PlaylistCreationUiState,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val isSubmitting = state == PlaylistCreationUiState.Submitting
    val failure = (state as? PlaylistCreationUiState.Failed)?.failure
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null) },
        title = { Text(stringResource(R.string.feature_library_impl_create_playlist_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.feature_library_impl_create_playlist_body),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth().testTag("my-create-playlist-name"),
                    enabled = !isSubmitting,
                    isError = failure != null,
                    singleLine = true,
                    label = { Text(stringResource(R.string.feature_library_impl_create_playlist_name)) },
                    supportingText = failure?.let {
                        {
                            Text(
                                text = it.message(),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text(stringResource(R.string.feature_library_impl_create_playlist_cancel))
            }
        },
        confirmButton = {
            ResonoteButton(
                label = stringResource(R.string.feature_library_impl_create_playlist_confirm),
                loadingLabel = stringResource(R.string.feature_library_impl_create_playlist_submitting),
                onClick = onConfirm,
                enabled = name.isNotBlank() && !isSubmitting,
                loading = isSubmitting,
            )
        },
    )
}

@Composable
private fun HistoryEntryCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("my-history"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(25.dp))
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    stringResource(R.string.feature_library_impl_recent_playback),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.feature_library_impl_recent_playback_body),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun LocalMusicEntryCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("my-local-music"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.LibraryMusic, contentDescription = null, modifier = Modifier.size(25.dp))
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    stringResource(R.string.feature_library_impl_local_music),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.feature_library_impl_local_music_body),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun CloudEntryCard(
    onClick: () -> Unit,
    requiresLogin: Boolean,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("my-cloud"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Cloud, contentDescription = null, modifier = Modifier.size(25.dp))
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    stringResource(R.string.my_cloud),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(if (requiresLogin) R.string.my_cloud_login else R.string.my_cloud_body),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun DailyVipEntryCard(
    onClick: () -> Unit,
    requiresLogin: Boolean,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("my-daily-vip"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.CardGiftcard, contentDescription = null, modifier = Modifier.size(25.dp))
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    stringResource(R.string.my_daily_vip),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(
                        if (requiresLogin) R.string.my_daily_vip_login else R.string.my_daily_vip_body,
                    ),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
        }
    }
}

private fun LazyListScope.playlistSections(
    playlists: List<UserPlaylist>,
    onPlaylistClick: (UserPlaylist) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    createEnabled: Boolean,
) {
    val liked = playlists.firstOrNull(UserPlaylist::isLike)
    val created = playlists.filter { it.isMine && !it.isLike }
    val collected = playlists.filterNot(UserPlaylist::isMine)

    if (liked != null) {
        item(key = "liked-${liked.globalId}") {
            LikedPlaylistCard(liked, onPlaylistClick)
        }
    }
    playlistGroup(
        keyPrefix = "created",
        title = R.string.my_created_playlists,
        emptyText = R.string.my_created_empty,
        playlists = created,
        onPlaylistClick = onPlaylistClick,
        onCreateClick = onCreatePlaylistClick,
        createEnabled = createEnabled,
    )
    playlistGroup(
        keyPrefix = "collected",
        title = R.string.my_collected_playlists,
        emptyText = R.string.my_collected_empty,
        playlists = collected,
        onPlaylistClick = onPlaylistClick,
    )
}

private fun LazyListScope.playlistGroup(
    keyPrefix: String,
    title: Int,
    emptyText: Int,
    playlists: List<UserPlaylist>,
    onPlaylistClick: (UserPlaylist) -> Unit,
    onCreateClick: (() -> Unit)? = null,
    createEnabled: Boolean = true,
) {
    item(key = "$keyPrefix-title") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.my_playlist_count, playlists.size),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
            if (onCreateClick != null) {
                IconButton(
                    onClick = onCreateClick,
                    modifier = Modifier.size(40.dp).testTag("my-create-playlist"),
                    enabled = createEnabled,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = stringResource(R.string.feature_library_impl_create_playlist_action),
                    )
                }
            }
        }
    }
    if (playlists.isEmpty()) {
        item(key = "$keyPrefix-empty") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    text = stringResource(emptyText),
                    modifier = Modifier.padding(20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    } else {
        item(key = "$keyPrefix-list") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                playlists.forEachIndexed { index, playlist ->
                    PlaylistRow(playlist = playlist, onClick = { onPlaylistClick(playlist) })
                    if (index < playlists.lastIndex) HorizontalDivider(modifier = Modifier.padding(start = 84.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(profile: UserProfile) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("my-profile"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(112.dp)) {
            Box(
                modifier = Modifier.fillMaxSize().background(profileGradient(profile.userId)),
            )
            profile.backgroundUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Avatar(
                profile = profile,
                modifier = Modifier.align(Alignment.BottomStart).offset(x = 18.dp, y = 34.dp),
            )
        }
        Column(modifier = Modifier.padding(start = 20.dp, top = 42.dp, end = 20.dp, bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = profile.nickname.ifBlank { stringResource(R.string.my_unnamed_user) },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (profile.isVip) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        shape = CircleShape,
                    ) {
                        Text(
                            text = profile.vipLabel.ifBlank { stringResource(R.string.my_vip) },
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.my_user_id, profile.userId),
                modifier = Modifier.padding(top = 3.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            profile.signature.takeIf(String::isNotBlank)?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProfileStat(stringResource(R.string.my_follows), profile.follows.compactNumber(), Modifier.weight(1f))
                ProfileStat(stringResource(R.string.my_fans), profile.fans.compactNumber(), Modifier.weight(1f))
                ProfileStat(stringResource(R.string.my_listen_time), profile.listenMinutes.listenTime(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Avatar(profile: UserProfile, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(72.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
    ) {
        Box(
            modifier = Modifier.padding(3.dp).clip(CircleShape).background(profileGradient(profile.nickname)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = profile.nickname.take(1).ifBlank { "·" },
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            profile.avatarUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = stringResource(R.string.my_avatar, profile.nickname),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
                label,
                modifier = Modifier.padding(top = 2.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LikedPlaylistCard(playlist: UserPlaylist, onPlaylistClick: (UserPlaylist) -> Unit) {
    Card(
        onClick = { onPlaylistClick(playlist) },
        modifier = Modifier.fillMaxWidth().testTag("my-liked"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(stringResource(R.string.my_liked), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.my_song_count, playlist.count),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun PlaylistRow(playlist: UserPlaylist, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(76.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaylistArtwork(playlist)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    playlist.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.my_song_count, playlist.count),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlaylistArtwork(playlist: UserPlaylist) {
    ResonoteArtwork(
        state = ResonoteArtworkState.LOADED,
        contentDescription = stringResource(R.string.my_playlist_artwork, playlist.name),
        modifier = Modifier.size(52.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(profileGradient(playlist.globalId)))
        playlist.coverUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ProfileLoadingCard() {
    Surface(
        modifier = Modifier.fillMaxWidth().height(260.dp).testTag("my-profile-loading"),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
}

@Composable
private fun PlaylistsLoadingCard() {
    Surface(
        modifier = Modifier.fillMaxWidth().height(150.dp).testTag("my-playlists-loading"),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            Text(
                stringResource(R.string.my_loading_playlists),
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionFailure(
    title: String,
    failure: ContentFailure,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CloudOff, contentDescription = null)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    failure.message(),
                    modifier = Modifier.padding(top = 3.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            androidx.compose.material3.TextButton(onClick = onRetry) { Text(stringResource(R.string.my_retry)) }
        }
    }
}

@Composable
private fun ContentFailure.message(): String = stringResource(
    when (this) {
        ContentFailure.AuthenticationRequired -> R.string.my_error_auth
        ContentFailure.Network -> R.string.my_error_network
        ContentFailure.ServiceRejected -> R.string.my_error_service
        is ContentFailure.RiskVerificationRequired -> R.string.my_error_risk
        ContentFailure.RiskBlocked -> R.string.my_error_risk
        ContentFailure.Protocol -> R.string.my_error_protocol
    },
)

@Composable
private fun profileGradient(seed: String): Brush {
    val palettes = listOf(
        listOf(Color(0xFF7A1538), Color(0xFFD8426D), Color(0xFFF4B2C3)),
        listOf(Color(0xFF173A4B), Color(0xFF377E92), Color(0xFFB9DDE0)),
        listOf(Color(0xFF38305E), Color(0xFF7769A8), Color(0xFFD8CAE8)),
        listOf(Color(0xFF5B361A), Color(0xFFB2723C), Color(0xFFE9C89C)),
    )
    return Brush.linearGradient(palettes[(seed.hashCode() and Int.MAX_VALUE) % palettes.size])
}

private fun Long.compactNumber(): String = when {
    this >= 100_000_000 -> "%.1f亿".format(this / 100_000_000.0)
    this >= 10_000 -> "%.1f万".format(this / 10_000.0)
    else -> toString()
}

private fun Long.listenTime(): String = when {
    this >= 60 -> "${this / 60}小时"
    this > 0 -> "${this}分钟"
    else -> "—"
}

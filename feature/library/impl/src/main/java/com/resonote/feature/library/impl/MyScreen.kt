package com.resonote.feature.library.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.resonote.core.designsystem.component.LocalResonoteSnackbarController
import com.resonote.core.designsystem.component.ResonoteArtworkState
import com.resonote.core.designsystem.component.ResonoteButton
import com.resonote.core.designsystem.component.ResonoteContentPhase
import com.resonote.core.designsystem.component.ResonoteContentStateLayout
import com.resonote.core.designsystem.component.ResonoteEmptyState
import com.resonote.core.designsystem.component.ResonoteErrorState
import com.resonote.core.designsystem.component.ResonoteFilterPill
import com.resonote.core.designsystem.component.ResonoteIconButton
import com.resonote.core.designsystem.component.ResonotePlaylistItem
import com.resonote.core.designsystem.component.ResonotePlaylistMetadata
import com.resonote.core.designsystem.component.ResonotePullToRefreshBox
import com.resonote.core.designsystem.component.ResonoteShimmer
import com.resonote.core.designsystem.component.ResonoteTextButton
import com.resonote.core.designsystem.component.ResonoteTextField
import com.resonote.core.designsystem.component.rememberResonoteShimmer
import com.resonote.core.designsystem.component.resonoteShimmer
import com.resonote.core.designsystem.tokens.ResonoteTokens
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.UserPlaylist
import com.resonote.core.model.UserProfile

@Composable
fun MyRoute(
    bottomContentPadding: Dp,
    onLoginClick: () -> Unit,
    onDailyVipClick: () -> Unit,
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

@Composable
private fun PlaylistSection(
    state: MySectionState<List<UserPlaylist>>,
    selectedGroup: Int,
    onSelectGroup: (Int) -> Unit,
    onPlaylistClick: (UserPlaylist) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onRetry: () -> Unit,
    createEnabled: Boolean,
) {
    val playlists = (state as? MySectionState.Available)?.value.orEmpty()
    val created = playlists.filter { it.isMine && !it.isLike }
    val collected = playlists.filterNot { it.isMine }
    val visible = if (selectedGroup == 0) created else collected
    val phase = when (state) {
        MySectionState.Loading -> ResonoteContentPhase.LOADING
        is MySectionState.Failed -> ResonoteContentPhase.ERROR
        is MySectionState.Available -> if (visible.isEmpty()) {
            ResonoteContentPhase.EMPTY
        } else {
            ResonoteContentPhase.CONTENT
        }
    }
    val stateModifier = if (phase == ResonoteContentPhase.CONTENT) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth().heightIn(min = 236.dp)
    }
    ResonoteContentStateLayout(
        phase = phase,
        modifier = stateModifier.testTag("my-playlist-state"),
        loading = { PlaylistSkeleton() },
        empty = {
            Column {
                PlaylistHeader(
                    createdCount = created.size,
                    collectedCount = collected.size,
                    selectedGroup = selectedGroup,
                    onSelectGroup = onSelectGroup,
                    onCreatePlaylistClick = onCreatePlaylistClick,
                    createEnabled = createEnabled,
                )
                ResonoteEmptyState(
                    title = stringResource(
                        if (selectedGroup == 0) {
                            R.string.feature_library_impl_my_created_empty_title
                        } else {
                            R.string.feature_library_impl_my_collected_empty_title
                        },
                    ),
                    message = stringResource(
                        if (selectedGroup == 0) {
                            R.string.feature_library_impl_my_created_empty
                        } else {
                            R.string.feature_library_impl_my_collected_empty
                        },
                    ),
                    modifier = Modifier.height(220.dp),
                )
            }
        },
        error = {
            ResonoteErrorState(
                onRetry = onRetry,
                title = stringResource(R.string.feature_library_impl_my_playlists_error),
                message = (state as MySectionState.Failed).failure.message(),
                modifier = Modifier.testTag("my-playlists-error"),
            )
        },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PlaylistHeader(
                    createdCount = created.size,
                    collectedCount = collected.size,
                    selectedGroup = selectedGroup,
                    onSelectGroup = onSelectGroup,
                    onCreatePlaylistClick = onCreatePlaylistClick,
                    createEnabled = createEnabled,
                )
                visible.chunked(2).forEach { row ->
                    PlaylistRow(row, onPlaylistClick)
                }
            }
        },
    )
}

@Composable
private fun PlaylistHeader(
    createdCount: Int,
    collectedCount: Int,
    selectedGroup: Int,
    onSelectGroup: (Int) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    createEnabled: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp).testTag("my-playlist-header"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.feature_library_impl_my_playlists),
                modifier = Modifier.weight(1f).testTag("my-playlist-title"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            ResonoteIconButton(
                label = stringResource(R.string.feature_library_impl_create_playlist_action),
                onClick = onCreatePlaylistClick,
                modifier = Modifier.offset(x = 12.dp).testTag("my-create-playlist"),
                enabled = createEnabled,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = null,
                    modifier = Modifier.testTag("my-create-playlist-icon"),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ResonoteFilterPill(
                stringResource(R.string.feature_library_impl_my_created_filter, createdCount),
                selectedGroup == 0,
                { onSelectGroup(0) },
            )
            ResonoteFilterPill(
                stringResource(R.string.feature_library_impl_my_collected_filter, collectedCount),
                selectedGroup == 1,
                { onSelectGroup(1) },
            )
        }
    }
}

@Composable
private fun PlaylistRow(row: List<UserPlaylist>, onPlaylistClick: (UserPlaylist) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        row.forEach { playlist ->
            ResonotePlaylistItem(
                metadata = ResonotePlaylistMetadata(
                    title = playlist.name,
                    supportingText = stringResource(
                        R.string.feature_library_impl_my_song_count,
                        playlist.count,
                    ),
                ),
                onClick = { onPlaylistClick(playlist) },
                modifier = Modifier.weight(1f).testTag("my-playlist-${playlist.globalId}"),
                artworkState = ResonoteArtworkState.LOADED,
                artworkUrl = playlist.coverUrl,
            )
        }
        if (row.size == 1) Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun AccountCard(profile: UserProfile, onDailyVipClick: () -> Unit, onSettingsClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("my-profile"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(profile)
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.nickname.ifBlank {
                            stringResource(R.string.feature_library_impl_my_unnamed_user)
                        },
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (profile.isVip) VipLabel(profile.vipLabel)
                }
                Text(
                    stringResource(R.string.feature_library_impl_my_user_id, profile.userId),
                    Modifier.padding(top = 3.dp),
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                profile.signature.takeIf(String::isNotBlank)?.let {
                    Text(
                        text = it,
                        modifier = Modifier.padding(top = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            SettingsButton(onSettingsClick)
        }
        DailyVipAction(onDailyVipClick)
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ProfileStat(stringResource(R.string.feature_library_impl_my_follows), profile.follows.compactNumber())
            ProfileStat(stringResource(R.string.feature_library_impl_my_fans), profile.fans.compactNumber())
            ProfileStat(
                stringResource(R.string.feature_library_impl_my_listen_time),
                profile.listenMinutes.listenTime(),
            )
        }
    }
}

@Composable
private fun AnonymousAccountCard(onLoginClick: () -> Unit, onSettingsClick: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                onClick = onLoginClick,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large,
                color = Color.Transparent,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Person, null, Modifier.size(30.dp))
                        }
                    }
                    Column(Modifier.weight(1f).padding(start = 16.dp)) {
                        Text(
                            stringResource(R.string.feature_library_impl_my_anonymous_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            stringResource(R.string.feature_library_impl_my_anonymous_body),
                            Modifier.padding(top = 5.dp),
                            MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                        )
                    }
                }
            }
            SettingsButton(onSettingsClick)
        }
        Row(
            modifier = Modifier.padding(start = 80.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.CardGiftcard,
                null,
                Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.feature_library_impl_my_anonymous_vip_note),
                Modifier.padding(start = 7.dp),
                MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun DailyVipAction(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("my-daily-vip"),
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(38.dp).testTag("my-daily-vip-icon"),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.CardGiftcard, null, Modifier.size(19.dp))
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    stringResource(R.string.feature_library_impl_my_daily_vip),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(R.string.feature_library_impl_my_daily_vip_body),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Chevron(Modifier.offset(x = 4.dp).testTag("my-daily-vip-trailing"))
        }
    }
}

@Composable
private fun SettingsButton(onClick: () -> Unit) {
    ResonoteIconButton(
        label = stringResource(R.string.feature_library_impl_settings),
        onClick = onClick,
        modifier = Modifier.offset(x = 12.dp).testTag("my-settings"),
    ) {
        Icon(
            Icons.Rounded.Settings,
            null,
            Modifier.testTag("my-settings-icon"),
        )
    }
}

@Composable
private fun QuickEntries(
    likedPlaylist: UserPlaylist?,
    likedRequiresLogin: Boolean,
    onLikedClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onCloudClick: () -> Unit,
    onLocalMusicClick: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 4.dp)) {
        QuickEntry(
            icon = { Icon(Icons.Rounded.Favorite, null) },
            label = stringResource(R.string.feature_library_impl_my_liked),
            onClick = onLikedClick,
            iconColor = MaterialTheme.colorScheme.primary,
            enabled = likedRequiresLogin || likedPlaylist != null,
            modifier = Modifier.weight(1f).testTag("my-liked"),
            iconTestTag = "my-liked-icon",
            horizontalBias = -1f,
        )
        QuickEntry(
            icon = { Icon(Icons.Rounded.History, null) },
            label = stringResource(R.string.feature_library_impl_recent_playback),
            onClick = onHistoryClick,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).testTag("my-history"),
            iconTestTag = "my-history-icon",
            horizontalBias = -1f / 3f,
        )
        QuickEntry(
            icon = { Icon(Icons.Rounded.Cloud, null) },
            label = stringResource(R.string.feature_library_impl_my_cloud),
            onClick = onCloudClick,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).testTag("my-cloud"),
            iconTestTag = "my-cloud-icon",
            horizontalBias = 1f / 3f,
        )
        QuickEntry(
            icon = { Icon(Icons.Rounded.LibraryMusic, null) },
            label = stringResource(R.string.feature_library_impl_local_music),
            onClick = onLocalMusicClick,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).testTag("my-local-music"),
            iconTestTag = "my-local-music-icon",
            horizontalBias = 1f,
        )
    }
}

@Composable
private fun QuickEntry(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    iconColor: Color,
    modifier: Modifier,
    iconTestTag: String? = null,
    horizontalBias: Float,
    enabled: Boolean = true,
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = BiasAlignment(horizontalBias, 0f),
        ) {
            Column(
                modifier = Modifier.width(44.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .then(if (iconTestTag == null) Modifier else Modifier.testTag(iconTestTag)),
                    shape = CircleShape,
                    color = if (iconColor == MaterialTheme.colorScheme.primary) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    contentColor = if (enabled) {
                        iconColor
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    },
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { icon() }
                    }
                }
                Text(
                    label,
                    Modifier.padding(top = 8.dp).wrapContentWidth(unbounded = true),
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun VipLabel(label: String) {
    Surface(
        modifier = Modifier.padding(start = 8.dp),
        shape = ResonoteTokens.shapes.full,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Text(
            label.ifBlank { stringResource(R.string.feature_library_impl_my_vip) },
            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun Avatar(profile: UserProfile) {
    Surface(
        modifier = Modifier.size(64.dp).testTag("my-avatar"),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        Box(Modifier.clip(CircleShape), contentAlignment = Alignment.Center) {
            Text(
                profile.nickname.take(1).ifBlank { "·" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            profile.avatarUrl?.let {
                AsyncImage(
                    it,
                    stringResource(R.string.feature_library_impl_my_avatar, profile.nickname),
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String) {
    Column(Modifier.width(88.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(
            label,
            Modifier.padding(top = 2.dp),
            MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun Chevron(modifier: Modifier = Modifier) {
    Icon(
        Icons.Rounded.ChevronRight,
        null,
        modifier,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MyScreenSkeleton(bottomContentPadding: Dp) {
    val shimmer = rememberResonoteShimmer("my-screen-skeleton")
    Column(
        Modifier.fillMaxWidth().padding(bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ProfileSkeleton(shimmer)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(4) {
                Spacer(
                    Modifier.weight(1f).height(88.dp)
                        .resonoteShimmer(shimmer, MaterialTheme.shapes.large),
                )
            }
        }
        PlaylistSkeleton(shimmer)
    }
}

@Composable
private fun ProfileSkeleton(shimmer: ResonoteShimmer = rememberResonoteShimmer("my-profile-skeleton")) {
    Spacer(
        Modifier.fillMaxWidth().height(244.dp)
            .resonoteShimmer(shimmer, MaterialTheme.shapes.extraLarge)
            .testTag("my-profile-loading"),
    )
}

@Composable
private fun PlaylistSkeleton(shimmer: ResonoteShimmer = rememberResonoteShimmer("my-playlist-skeleton")) {
    Column(Modifier.fillMaxWidth().testTag("my-playlists-loading"), Arrangement.spacedBy(12.dp)) {
        Spacer(Modifier.fillMaxWidth().height(80.dp).resonoteShimmer(shimmer, MaterialTheme.shapes.extraLarge))
        Spacer(Modifier.width(132.dp).height(24.dp).resonoteShimmer(shimmer, MaterialTheme.shapes.small))
        repeat(3) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.size(52.dp).resonoteShimmer(shimmer, MaterialTheme.shapes.medium))
                Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Spacer(Modifier.width(156.dp).height(16.dp).resonoteShimmer(shimmer, MaterialTheme.shapes.small))
                    Spacer(Modifier.width(64.dp).height(12.dp).resonoteShimmer(shimmer, MaterialTheme.shapes.small))
                }
            }
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
        modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
        shadowElevation = ResonoteTokens.elevation.level2.maximumShadow,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CloudOff, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    failure.message(),
                    Modifier.padding(top = 3.dp),
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onRetry) { Text(stringResource(R.string.feature_library_impl_my_retry)) }
        }
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
    val submitting = state == PlaylistCreationUiState.Submitting
    val failure = (state as? PlaylistCreationUiState.Failed)?.failure
    Dialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !submitting,
            dismissOnClickOutside = !submitting,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .widthIn(max = 360.dp)
                .testTag("my-create-playlist-dialog"),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp,
            shadowElevation = ResonoteTokens.elevation.level3.maximumShadow,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null, Modifier.size(26.dp))
                    }
                }
                Text(
                    text = stringResource(R.string.feature_library_impl_create_playlist_title),
                    modifier = Modifier.padding(top = 18.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.feature_library_impl_create_playlist_body),
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                ResonoteTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = stringResource(R.string.feature_library_impl_create_playlist_name),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .testTag("my-create-playlist-name"),
                    enabled = !submitting,
                    errorMessage = failure?.message(),
                )
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ResonoteButton(
                        stringResource(R.string.feature_library_impl_create_playlist_confirm),
                        onConfirm,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = name.isNotBlank() && !submitting,
                        loading = submitting,
                        loadingLabel = stringResource(R.string.feature_library_impl_create_playlist_submitting),
                    )
                    ResonoteTextButton(
                        label = stringResource(R.string.feature_library_impl_create_playlist_cancel),
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !submitting,
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentFailure.message() = stringResource(
    when (this) {
        ContentFailure.AuthenticationRequired -> R.string.feature_library_impl_my_error_auth
        ContentFailure.Network -> R.string.feature_library_impl_my_error_network
        ContentFailure.ServiceRejected -> R.string.feature_library_impl_my_error_service
        is ContentFailure.RiskVerificationRequired -> R.string.feature_library_impl_my_error_risk
        ContentFailure.RiskBlocked -> R.string.feature_library_impl_my_error_risk
        ContentFailure.Protocol -> R.string.feature_library_impl_my_error_protocol
    },
)

private fun Long.compactNumber() = when {
    this >= 100_000_000 -> "%.1f亿".format(this / 100_000_000.0)
    this >= 10_000 -> "%.1f万".format(this / 10_000.0)
    else -> toString()
}

private fun Long.listenTime() = when {
    this >= 60 -> "${this / 60}小时"
    this > 0 -> "${this}分钟"
    else -> "—"
}

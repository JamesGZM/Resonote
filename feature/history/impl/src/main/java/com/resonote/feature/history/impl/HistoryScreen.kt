@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.history.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.resonote.core.designsystem.component.ResonoteButton
import com.resonote.core.designsystem.component.ResonoteMusicItem
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.DeviceHistoryItem
import com.resonote.core.model.DeviceHistorySource
import com.resonote.core.model.OnlineSong
import com.resonote.feature.history.api.HistoryTab

@Composable
fun HistoryRoute(
    initialTab: HistoryTab,
    playingMediaId: String?,
    bottomContentPadding: Dp,
    onBack: () -> Unit,
    onLoginRequest: () -> Unit,
    onPlayOnline: (List<OnlineSong>, Int) -> Unit,
    onPlayDevice: (List<DeviceHistoryItem>, Int) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(initialTab) { viewModel.initialize(initialTab) }
    LaunchedEffect(viewModel) { viewModel.loginRequests.collect { onLoginRequest() } }
    HistoryScreen(
        state = state,
        playingMediaId = playingMediaId,
        bottomContentPadding = bottomContentPadding,
        onBack = onBack,
        onLoginRequest = onLoginRequest,
        onSelectTab = viewModel::selectTab,
        onRefreshOnline = viewModel::refreshOnline,
        onPlayOnline = onPlayOnline,
        onPlayDevice = { items, startIndex ->
            if (requiresLoginForDevicePlayback(state, items, startIndex)) {
                onLoginRequest()
            } else {
                onPlayDevice(items, startIndex)
            }
        },
        onDeleteDevice = viewModel::deleteDeviceItem,
        onClearDevice = viewModel::clearDeviceHistory,
        onDismissMutationFailure = viewModel::dismissMutationFailure,
    )
}

internal fun requiresLoginForDevicePlayback(
    state: HistoryUiState,
    items: List<DeviceHistoryItem>,
    startIndex: Int,
): Boolean =
    state.accountState != HistoryAccountState.Authenticated &&
        items.getOrNull(startIndex)?.record?.source == DeviceHistorySource.Cloud

@Composable
internal fun HistoryScreen(
    state: HistoryUiState,
    playingMediaId: String?,
    bottomContentPadding: Dp,
    onBack: () -> Unit,
    onLoginRequest: () -> Unit,
    onSelectTab: (HistoryTab) -> Unit,
    onRefreshOnline: () -> Unit,
    onPlayOnline: (List<OnlineSong>, Int) -> Unit,
    onPlayDevice: (List<DeviceHistoryItem>, Int) -> Unit,
    onDeleteDevice: (DeviceHistoryItem) -> Unit,
    onClearDevice: () -> Unit,
    onDismissMutationFailure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<DeviceHistoryItem?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val busy = state.mutation == DeviceHistoryMutation.Working

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ResonoteTopAppBar(
                title = { Text(stringResource(R.string.feature_history_impl_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.feature_history_impl_back))
                    }
                },
                actions = {
                    val onlineSelected = state.selectedTab == HistoryTab.Online
                    IconButton(
                        onClick = if (onlineSelected) onRefreshOnline else ({ confirmClear = true }),
                        enabled = if (onlineSelected) {
                            state.online !is OnlineHistoryUiState.Loading
                        } else {
                            state.deviceItems.isNotEmpty() && !busy
                        },
                    ) {
                        Icon(
                            imageVector = if (onlineSelected) Icons.Rounded.Refresh else Icons.Rounded.DeleteSweep,
                            contentDescription = stringResource(
                                if (onlineSelected) {
                                    R.string.feature_history_impl_refresh
                                } else {
                                    R.string.feature_history_impl_clear
                                },
                            ),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("history-list"),
            contentPadding = PaddingValues(bottom = bottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "tabs") {
                PrimaryTabRow(selectedTabIndex = state.selectedTab.ordinal) {
                    HistoryTab.entries.forEach { tab ->
                        Tab(
                            selected = state.selectedTab == tab,
                            onClick = { onSelectTab(tab) },
                            text = {
                                Text(
                                    stringResource(
                                        if (tab == HistoryTab.Online) {
                                            R.string.feature_history_impl_online_tab
                                        } else {
                                            R.string.feature_history_impl_device_tab
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
            }
            item(key = "archive") {
                ArchiveCard(
                    tab = state.selectedTab,
                    count = state.visibleCount,
                    canPlay = state.visibleCount > 0 && !busy,
                    onPlayAll = {
                        when (val online = state.online) {
                            is OnlineHistoryUiState.Available -> if (state.selectedTab == HistoryTab.Online) {
                                onPlayOnline(online.songs, 0)
                            }
                            else -> Unit
                        }
                        if (state.selectedTab == HistoryTab.Device && state.deviceItems.isNotEmpty()) {
                            onPlayDevice(state.deviceItems, 0)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            if (state.mutation == DeviceHistoryMutation.Failed) {
                item(key = "mutation-failure") {
                    MutationFailureCard(
                        onDismiss = onDismissMutationFailure,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            when (state.selectedTab) {
                HistoryTab.Online -> onlineContent(
                    state = state,
                    playingMediaId = playingMediaId,
                    onLoginRequest = onLoginRequest,
                    onRetry = onRefreshOnline,
                    onPlay = onPlayOnline,
                )
                HistoryTab.Device -> deviceContent(
                    state = state,
                    playingMediaId = playingMediaId,
                    busy = busy,
                    onPlay = onPlayDevice,
                    onDelete = { pendingDelete = it },
                )
            }
        }
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!busy) pendingDelete = null },
            title = { Text(stringResource(R.string.feature_history_impl_delete_title)) },
            text = { Text(stringResource(R.string.feature_history_impl_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDeleteDevice(item)
                    },
                    enabled = !busy,
                ) { Text(stringResource(R.string.feature_history_impl_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }, enabled = !busy) {
                    Text(stringResource(R.string.feature_history_impl_cancel))
                }
            },
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { if (!busy) confirmClear = false },
            title = { Text(stringResource(R.string.feature_history_impl_clear_title)) },
            text = { Text(stringResource(R.string.feature_history_impl_clear_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClearDevice()
                    },
                    enabled = !busy,
                ) { Text(stringResource(R.string.feature_history_impl_clear_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }, enabled = !busy) {
                    Text(stringResource(R.string.feature_history_impl_cancel))
                }
            },
        )
    }
}

private val HistoryUiState.visibleCount: Int
    get() = when (selectedTab) {
        HistoryTab.Online -> (online as? OnlineHistoryUiState.Available)?.songs?.size ?: 0
        HistoryTab.Device -> deviceItems.size
    }

@Composable
private fun ArchiveCard(
    tab: HistoryTab,
    count: Int,
    canPlay: Boolean,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = if (tab == HistoryTab.Online) {
        listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surfaceContainerLow)
    } else {
        listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.surfaceContainerLow)
    }
    Card(
        modifier = modifier.fillMaxWidth().testTag("history-archive"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(colors)).padding(22.dp),
        ) {
            Text(
                stringResource(R.string.feature_history_impl_archive_label),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(
                    if (tab == HistoryTab.Online) {
                        R.string.feature_history_impl_online_archive
                    } else {
                        R.string.feature_history_impl_device_archive
                    },
                ),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(
                    if (tab == HistoryTab.Online) {
                        R.string.feature_history_impl_online_archive_body
                    } else {
                        R.string.feature_history_impl_device_archive_body
                    },
                ),
                modifier = Modifier.padding(top = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.feature_history_impl_track_count, count),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                ResonoteButton(
                    label = stringResource(R.string.feature_history_impl_play_all),
                    onClick = onPlayAll,
                    enabled = canPlay,
                    leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.onlineContent(
    state: HistoryUiState,
    playingMediaId: String?,
    onLoginRequest: () -> Unit,
    onRetry: () -> Unit,
    onPlay: (List<OnlineSong>, Int) -> Unit,
) {
    when (val section = state.online) {
        OnlineHistoryUiState.NotLoaded -> item(key = "online-signed-out") {
            if (state.accountState == HistoryAccountState.Checking) {
                LoadingState(R.string.feature_history_impl_online_loading)
            } else {
                SignedOutState(onLoginRequest)
            }
        }
        OnlineHistoryUiState.Loading -> item(key = "online-loading") {
            LoadingState(R.string.feature_history_impl_online_loading)
        }
        is OnlineHistoryUiState.Failed -> item(key = "online-failure") {
            OnlineFailureState(section.failure, onRetry)
        }
        is OnlineHistoryUiState.Available -> if (section.songs.isEmpty()) {
            item(key = "online-empty") {
                EmptyState(
                    title = stringResource(R.string.feature_history_impl_online_empty_title),
                    body = stringResource(R.string.feature_history_impl_online_empty_body),
                )
            }
        } else {
            itemsIndexed(section.songs, key = { _, song -> "online:${song.hash}" }) { index, song ->
                OnlineHistoryRow(
                    song = song,
                    isPlaying = playingMediaId == song.hash,
                    onClick = { onPlay(section.songs, index) },
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.deviceContent(
    state: HistoryUiState,
    playingMediaId: String?,
    busy: Boolean,
    onPlay: (List<DeviceHistoryItem>, Int) -> Unit,
    onDelete: (DeviceHistoryItem) -> Unit,
) {
    when {
        state.deviceLoading -> item(key = "device-loading") {
            LoadingState(R.string.feature_history_impl_device_loading)
        }
        state.deviceLoadFailed -> item(key = "device-failure") { DeviceFailureState() }
        state.deviceItems.isEmpty() -> item(key = "device-empty") {
            EmptyState(
                title = stringResource(R.string.feature_history_impl_device_empty_title),
                body = stringResource(R.string.feature_history_impl_device_empty_body),
            )
        }
        else -> itemsIndexed(
            state.deviceItems,
            key = { _, item -> "${item.record.source}:${item.record.mediaId}" },
        ) { index, item ->
            DeviceHistoryRow(
                item = item,
                isPlaying = playingMediaId == item.record.mediaId,
                enabled = !busy,
                onClick = { onPlay(state.deviceItems, index) },
                onDelete = { onDelete(item) },
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

@Composable
private fun OnlineHistoryRow(
    song: OnlineSong,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val artist = song.artist.orEmpty().ifBlank { stringResource(R.string.feature_history_impl_unknown_artist) }
    val album = song.albumTitle?.takeIf(String::isNotBlank)?.let {
        stringResource(R.string.feature_history_impl_album_separator, it)
    }.orEmpty()
    ResonoteMusicItem(
        title = song.title,
        supportingText = stringResource(R.string.feature_history_impl_online_supporting, artist, album),
        duration = song.durationMillis.durationLabel(),
        onClick = onClick,
        onMoreClick = null,
        modifier = modifier,
        qualityLabel = song.quality.label(),
        isVip = song.vip,
        isPlaying = isPlaying,
        artwork = { HistoryArtwork(song.coverUrl, song.title) },
    )
}

@Composable
private fun DeviceHistoryRow(
    item: DeviceHistoryItem,
    isPlaying: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val record = item.record
    val source = stringResource(
        if (record.source == DeviceHistorySource.Local) {
            R.string.feature_history_impl_source_local
        } else {
            R.string.feature_history_impl_source_cloud
        },
    )
    val artist = record.artist.orEmpty().ifBlank { stringResource(R.string.feature_history_impl_unknown_artist) }
    ResonoteMusicItem(
        title = record.title,
        supportingText = stringResource(
            R.string.feature_history_impl_device_supporting,
            source,
            artist,
            item.playCount,
        ),
        duration = record.durationMillis.durationLabel(),
        onClick = onClick,
        onMoreClick = onDelete,
        modifier = modifier,
        isPlaying = isPlaying,
        enabled = enabled,
        artwork = { HistoryArtwork(record.artworkUri, record.title, record.source) },
    )
}

@Composable
private fun HistoryArtwork(
    model: String?,
    title: String,
    source: DeviceHistorySource? = null,
) {
    val containerColor = when (source) {
        DeviceHistorySource.Local -> MaterialTheme.colorScheme.secondaryContainer
        DeviceHistorySource.Cloud -> MaterialTheme.colorScheme.tertiaryContainer
        null -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when (source) {
        DeviceHistorySource.Local -> MaterialTheme.colorScheme.onSecondaryContainer
        DeviceHistorySource.Cloud -> MaterialTheme.colorScheme.onTertiaryContainer
        null -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Box(
        modifier = Modifier.fillMaxSize().background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = when (source) {
                DeviceHistorySource.Local -> Icons.Rounded.LibraryMusic
                DeviceHistorySource.Cloud -> Icons.Rounded.Cloud
                null -> Icons.Rounded.MusicNote
            },
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp),
        )
        model?.let {
            AsyncImage(
                model = it,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun SignedOutState(onLoginRequest: () -> Unit) {
    MessageState(
        icon = { Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(42.dp)) },
        title = stringResource(R.string.feature_history_impl_online_login_title),
        body = stringResource(R.string.feature_history_impl_online_login_body),
        action = {
            ResonoteButton(
                label = stringResource(R.string.feature_history_impl_login),
                onClick = onLoginRequest,
            )
        },
    )
}

@Composable
private fun OnlineFailureState(failure: ContentFailure, onRetry: () -> Unit) {
    MessageState(
        icon = { Icon(Icons.Rounded.CloudOff, contentDescription = null, modifier = Modifier.size(42.dp)) },
        title = stringResource(R.string.feature_history_impl_online_error_title),
        body = failure.message(),
        action = {
            ResonoteButton(
                label = stringResource(R.string.feature_history_impl_retry),
                onClick = onRetry,
            )
        },
    )
}

@Composable
private fun DeviceFailureState() {
    MessageState(
        icon = { Icon(Icons.Rounded.LibraryMusic, contentDescription = null, modifier = Modifier.size(42.dp)) },
        title = stringResource(R.string.feature_history_impl_device_error_title),
        body = stringResource(R.string.feature_history_impl_device_error_body),
    )
}

@Composable
private fun EmptyState(title: String, body: String) {
    MessageState(
        icon = { Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(42.dp)) },
        title = title,
        body = body,
    )
}

@Composable
private fun MessageState(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.primary,
        ) { Box(contentAlignment = Alignment.Center) { icon() } }
        Text(
            title,
            modifier = Modifier.padding(top = 18.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            body,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        action?.let { Box(modifier = Modifier.padding(top = 22.dp)) { it() } }
    }
}

@Composable
private fun LoadingState(label: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 58.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            stringResource(label),
            modifier = Modifier.padding(top = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MutationFailureCard(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().testTag("history-mutation-failure"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.feature_history_impl_mutation_failed),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_history_impl_dismiss)) }
        }
    }
}

@Composable
private fun ContentFailure.message(): String = stringResource(
    when (this) {
        ContentFailure.AuthenticationRequired -> R.string.feature_history_impl_error_auth
        ContentFailure.Network -> R.string.feature_history_impl_error_network
        ContentFailure.RiskBlocked, is ContentFailure.RiskVerificationRequired -> R.string.feature_history_impl_error_risk
        ContentFailure.ServiceRejected, ContentFailure.Protocol -> R.string.feature_history_impl_error_generic
    },
)

private fun AudioQuality.label(): String = when (this) {
    AudioQuality.Standard -> "SQ"
    AudioQuality.HighQuality -> "HQ"
    AudioQuality.HighResolution -> "Hi-Res"
    AudioQuality.Lossless -> "LOSSLESS"
}

private fun Long.durationLabel(): String {
    if (this <= 0) return "—:—"
    val totalSeconds = this / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

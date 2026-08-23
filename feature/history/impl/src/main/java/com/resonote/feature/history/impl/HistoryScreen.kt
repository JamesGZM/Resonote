@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.history.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.LocalResonoteSnackbarController
import com.resonote.core.designsystem.component.ResonoteLoadMoreEffect
import com.resonote.core.designsystem.component.ResonoteLoadMoreFooter
import com.resonote.core.designsystem.component.ResonoteLoadMoreState
import com.resonote.core.designsystem.component.ResonotePullToRefreshBox
import com.resonote.core.designsystem.component.ResonoteTabbedToolbar
import com.resonote.core.designsystem.component.ResonoteTextButton
import com.resonote.core.designsystem.component.ResonoteTopAppBar
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
    onSongMoreClick: (OnlineSong) -> Unit,
    onPlayDevice: (List<DeviceHistoryItem>, Int) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarController = LocalResonoteSnackbarController.current
    val refreshFailureMessage = stringResource(R.string.feature_history_impl_refresh_failed)
    LaunchedEffect(initialTab) { viewModel.initialize(initialTab) }
    LaunchedEffect(viewModel) { viewModel.loginRequests.collect { onLoginRequest() } }
    LaunchedEffect(viewModel, snackbarController) {
        viewModel.refreshFailures.collect { snackbarController?.show(refreshFailureMessage) }
    }
    HistoryScreen(
        state = state,
        playingMediaId = playingMediaId,
        bottomContentPadding = bottomContentPadding,
        onBack = onBack,
        onLoginRequest = onLoginRequest,
        onSelectTab = viewModel::selectTab,
        onRefreshOnline = viewModel::refreshOnline,
        onLoadMoreOnline = viewModel::loadMoreOnline,
        onPlayOnline = onPlayOnline,
        onSongMoreClick = onSongMoreClick,
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
): Boolean = state.accountState != HistoryAccountState.Authenticated &&
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
    onLoadMoreOnline: () -> Unit = {},
    onPlayOnline: (List<OnlineSong>, Int) -> Unit,
    onPlayDevice: (List<DeviceHistoryItem>, Int) -> Unit,
    onDeleteDevice: (DeviceHistoryItem) -> Unit,
    onClearDevice: () -> Unit,
    onDismissMutationFailure: () -> Unit,
    modifier: Modifier = Modifier,
    onSongMoreClick: ((OnlineSong) -> Unit)? = null,
) {
    var pendingDelete by remember { mutableStateOf<DeviceHistoryItem?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val busy = state.mutation == DeviceHistoryMutation.Working
    val online = state.online as? OnlineHistoryUiState.Available
    val listState = rememberLazyListState()
    ResonoteLoadMoreEffect(
        listState = listState,
        itemCount = online?.songs?.size ?: 0,
        enabled = state.selectedTab == HistoryTab.Online &&
            online?.let { it.hasMore && !it.isLoadingMore && it.loadMoreFailure == null } == true,
        onLoadMore = onLoadMoreOnline,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                ResonoteTopAppBar(
                    title = { Text(stringResource(R.string.feature_history_impl_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                stringResource(R.string.feature_history_impl_back),
                            )
                        }
                    },
                    actions = {
                        if (state.selectedTab == HistoryTab.Device) {
                            IconButton(
                                onClick = { confirmClear = true },
                                enabled = state.deviceItems.isNotEmpty() && !busy,
                            ) {
                                Icon(
                                    Icons.Rounded.DeleteSweep,
                                    stringResource(R.string.feature_history_impl_clear),
                                )
                            }
                        }
                    },
                )
                ResonoteTabbedToolbar(
                    labels = HistoryTab.entries.map { tab ->
                        stringResource(
                            if (tab == HistoryTab.Online) {
                                R.string.feature_history_impl_online_tab
                            } else {
                                R.string.feature_history_impl_device_tab
                            },
                        )
                    },
                    selectedIndex = state.selectedTab.ordinal,
                    onSelected = { onSelectTab(HistoryTab.entries[it]) },
                    windowInsets = WindowInsets(0),
                )
            }
        },
    ) { padding ->
        ResonotePullToRefreshBox(
            isRefreshing = online?.isRefreshing == true,
            onRefresh = onRefreshOnline,
            enabled = state.selectedTab == HistoryTab.Online && online?.songs?.isNotEmpty() == true,
            modifier = Modifier.fillMaxSize().padding(padding).testTag("history-pull-to-refresh"),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().testTag("history-list"),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 10.dp,
                    end = 16.dp,
                    bottom = bottomContentPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.visibleCount > 0) {
                    item(key = "summary") {
                        HistorySummary(
                            count = state.visibleCount,
                            enabled = !busy,
                            onPlayAll = {
                                when (val available = state.online) {
                                    is OnlineHistoryUiState.Available -> if (
                                        state.selectedTab == HistoryTab.Online
                                    ) {
                                        onPlayOnline(available.songs, 0)
                                    }
                                    else -> Unit
                                }
                                if (state.selectedTab == HistoryTab.Device && state.deviceItems.isNotEmpty()) {
                                    onPlayDevice(state.deviceItems, 0)
                                }
                            },
                        )
                    }
                }
                if (state.mutation == DeviceHistoryMutation.Failed) {
                    item(key = "mutation-failure") {
                        MutationFailureCard(onDismiss = onDismissMutationFailure)
                    }
                }
                when (state.selectedTab) {
                    HistoryTab.Online -> onlineContent(
                        state = state,
                        playingMediaId = playingMediaId,
                        onLoginRequest = onLoginRequest,
                        onRetry = onRefreshOnline,
                        onPlay = onPlayOnline,
                        onSongMoreClick = onSongMoreClick,
                    )
                    HistoryTab.Device -> deviceContent(
                        state = state,
                        playingMediaId = playingMediaId,
                        busy = busy,
                        onPlay = onPlayDevice,
                        onDelete = { pendingDelete = it },
                    )
                }
                if (state.selectedTab == HistoryTab.Online &&
                    online?.let { it.isLoadingMore || it.loadMoreFailure != null } == true
                ) {
                    item(key = "load-more") {
                        ResonoteLoadMoreFooter(
                            state = if (online.isLoadingMore) {
                                ResonoteLoadMoreState.LOADING
                            } else {
                                ResonoteLoadMoreState.ERROR
                            },
                            onRetry = onLoadMoreOnline,
                        )
                    }
                }
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

@Composable
private fun HistorySummary(count: Int, enabled: Boolean, onPlayAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, bottom = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.feature_history_impl_track_count, count),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(Modifier.weight(1f))
        ResonoteTextButton(
            label = stringResource(R.string.feature_history_impl_play_all),
            onClick = onPlayAll,
            enabled = enabled,
            leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
        )
    }
}

private val HistoryUiState.visibleCount: Int
    get() = when (selectedTab) {
        HistoryTab.Online -> (online as? OnlineHistoryUiState.Available)?.songs?.size ?: 0
        HistoryTab.Device -> deviceItems.size
    }

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.history.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
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

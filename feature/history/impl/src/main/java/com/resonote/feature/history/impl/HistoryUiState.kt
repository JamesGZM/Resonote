package com.resonote.feature.history.impl

import com.resonote.core.model.ContentFailure
import com.resonote.core.model.DeviceHistoryItem
import com.resonote.core.model.OnlineSong
import com.resonote.feature.history.api.HistoryTab

data class HistoryUiState(
    val selectedTab: HistoryTab = HistoryTab.Device,
    val accountState: HistoryAccountState = HistoryAccountState.Checking,
    val online: OnlineHistoryUiState = OnlineHistoryUiState.NotLoaded,
    val deviceItems: List<DeviceHistoryItem> = emptyList(),
    val deviceLoading: Boolean = true,
    val deviceLoadFailed: Boolean = false,
    val mutation: DeviceHistoryMutation = DeviceHistoryMutation.Idle,
)

enum class HistoryAccountState {
    Checking,
    Anonymous,
    Authenticated,
}

sealed interface OnlineHistoryUiState {
    data object NotLoaded : OnlineHistoryUiState
    data object Loading : OnlineHistoryUiState
    data class Available(val songs: List<OnlineSong>) : OnlineHistoryUiState
    data class Failed(val failure: ContentFailure) : OnlineHistoryUiState
}

sealed interface DeviceHistoryMutation {
    data object Idle : DeviceHistoryMutation
    data object Working : DeviceHistoryMutation
    data object Failed : DeviceHistoryMutation
}

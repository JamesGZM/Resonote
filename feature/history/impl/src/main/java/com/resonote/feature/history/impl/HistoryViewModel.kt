package com.resonote.feature.history.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.AuthRepository
import com.resonote.core.data.ListeningHistoryRepository
import com.resonote.core.model.AuthState
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.DeviceHistoryItem
import com.resonote.feature.history.api.HistoryTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: ListeningHistoryRepository,
    authRepository: AuthRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = mutableUiState.asStateFlow()

    private val mutableLoginRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val loginRequests: SharedFlow<Unit> = mutableLoginRequests.asSharedFlow()

    private var initialized = false
    private var activeUserId: String? = null
    private var onlineJob: Job? = null
    private var mutationJob: Job? = null

    init {
        viewModelScope.launch {
            historyRepository.observeDeviceHistory()
                .catch {
                    mutableUiState.update { state ->
                        state.copy(deviceLoading = false, deviceLoadFailed = true)
                    }
                }
                .collect { items ->
                    mutableUiState.update {
                        it.copy(deviceItems = items, deviceLoading = false, deviceLoadFailed = false)
                    }
                }
        }
        viewModelScope.launch {
            authRepository.authState.collectLatest(::onAuthState)
        }
    }

    fun initialize(initialTab: HistoryTab) {
        if (initialized) return
        initialized = true
        mutableUiState.update { it.copy(selectedTab = initialTab) }
        if (initialTab == HistoryTab.Online && activeUserId != null) loadOnline()
    }

    fun selectTab(tab: HistoryTab) {
        if (tab == mutableUiState.value.selectedTab) return
        mutableUiState.update { it.copy(selectedTab = tab) }
        if (tab != HistoryTab.Online) return

        when (mutableUiState.value.accountState) {
            HistoryAccountState.Checking -> Unit
            HistoryAccountState.Anonymous -> mutableLoginRequests.tryEmit(Unit)
            HistoryAccountState.Authenticated -> loadOnline()
        }
    }

    fun refreshOnline() {
        if (activeUserId == null) {
            mutableLoginRequests.tryEmit(Unit)
        } else {
            loadOnline(force = true)
        }
    }

    fun deleteDeviceItem(item: DeviceHistoryItem) = mutateDeviceHistory {
        historyRepository.deleteDeviceHistory(item.record)
    }

    fun clearDeviceHistory() = mutateDeviceHistory(historyRepository::clearDeviceHistory)

    fun dismissMutationFailure() {
        mutableUiState.update { it.copy(mutation = DeviceHistoryMutation.Idle) }
    }

    private fun onAuthState(authState: AuthState) {
        when (authState) {
            is AuthState.Authenticated -> {
                val accountChanged = activeUserId != authState.userId
                activeUserId = authState.userId
                mutableUiState.update { state ->
                    state.copy(
                        accountState = HistoryAccountState.Authenticated,
                        online = if (accountChanged) OnlineHistoryUiState.NotLoaded else state.online,
                    )
                }
                if (initialized && mutableUiState.value.selectedTab == HistoryTab.Online) {
                    loadOnline(force = accountChanged)
                }
            }
            AuthState.Anonymous, is AuthState.AuthenticationRequired -> {
                activeUserId = null
                onlineJob?.cancel()
                onlineJob = null
                mutableUiState.update {
                    it.copy(
                        accountState = HistoryAccountState.Anonymous,
                        online = OnlineHistoryUiState.NotLoaded,
                    )
                }
            }
        }
    }

    private fun loadOnline(force: Boolean = false) {
        val userId = activeUserId ?: return
        if (onlineJob?.isActive == true) return
        if (!force && mutableUiState.value.online is OnlineHistoryUiState.Available) return
        onlineJob = viewModelScope.launch {
            mutableUiState.update { it.copy(online = OnlineHistoryUiState.Loading) }
            val section = when (val result = historyRepository.loadAccountHistory()) {
                is CollectionLoadResult.Available -> OnlineHistoryUiState.Available(result.value)
                is CollectionLoadResult.Failed -> OnlineHistoryUiState.Failed(result.failure)
            }
            if (activeUserId == userId) mutableUiState.update { it.copy(online = section) }
            onlineJob = null
        }
    }

    private fun mutateDeviceHistory(block: suspend () -> Boolean) {
        if (mutationJob?.isActive == true) return
        mutationJob = viewModelScope.launch {
            mutableUiState.update { it.copy(mutation = DeviceHistoryMutation.Working) }
            val succeeded = try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }
            mutableUiState.update {
                it.copy(mutation = if (succeeded) DeviceHistoryMutation.Idle else DeviceHistoryMutation.Failed)
            }
            mutationJob = null
        }
    }
}

package com.resonote.feature.vip.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.VipRewardRepository
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ContentFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DailyVipViewModel @Inject constructor(
    private val repository: VipRewardRepository,
    clock: Clock,
) : ViewModel() {
    private val receiveDay = LocalDate.now(clock).format(DateTimeFormatter.ISO_LOCAL_DATE)
    private val mutableUiState = MutableStateFlow<DailyVipUiState>(DailyVipUiState.Ready(receiveDay))
    val uiState: StateFlow<DailyVipUiState> = mutableUiState.asStateFlow()

    private val mutableRewardApplied = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val rewardApplied: SharedFlow<Unit> = mutableRewardApplied.asSharedFlow()

    private var operationJob: Job? = null

    fun claim() {
        if (operationJob?.isActive == true) return
        val state = mutableUiState.value
        if (state !is DailyVipUiState.Ready &&
            !(state is DailyVipUiState.Failed && state.operation == DailyVipOperation.Claim)
        ) return
        mutableUiState.value = DailyVipUiState.Claiming(receiveDay)
        operationJob = viewModelScope.launch {
            when (val result = repository.claimDaily(receiveDay)) {
                is CollectionLoadResult.Available -> {
                    mutableRewardApplied.tryEmit(Unit)
                    mutableUiState.value = if (result.value.canUpgrade) {
                        DailyVipUiState.UpgradeChoice(receiveDay, result.value.alreadyDone)
                    } else {
                        DailyVipUiState.ClaimComplete(receiveDay, result.value.alreadyDone)
                    }
                }
                is CollectionLoadResult.Failed -> mutableUiState.value = result.failure.toClaimFailure()
            }
        }
    }

    fun upgrade() {
        if (operationJob?.isActive == true) return
        val state = mutableUiState.value
        val alreadyClaimed = when (state) {
            is DailyVipUiState.UpgradeChoice -> state.alreadyClaimed
            is DailyVipUiState.Failed -> state.alreadyClaimed.takeIf {
                state.operation == DailyVipOperation.Upgrade
            }
            else -> null
        } ?: return
        mutableUiState.value = DailyVipUiState.Upgrading(receiveDay, alreadyClaimed)
        operationJob = viewModelScope.launch {
            when (val result = repository.upgradeDaily()) {
                is CollectionLoadResult.Available -> {
                    mutableRewardApplied.tryEmit(Unit)
                    mutableUiState.value = DailyVipUiState.UpgradeComplete(
                        receiveDay = receiveDay,
                        alreadyUpgraded = result.value.alreadyDone,
                    )
                }
                is CollectionLoadResult.Failed -> mutableUiState.value = result.failure.toUpgradeFailure(alreadyClaimed)
            }
        }
    }

    fun declineUpgrade() {
        val state = mutableUiState.value as? DailyVipUiState.UpgradeChoice ?: return
        mutableUiState.value = DailyVipUiState.ClaimComplete(receiveDay, state.alreadyClaimed)
    }

    fun retry() {
        when ((mutableUiState.value as? DailyVipUiState.Failed)?.operation) {
            DailyVipOperation.Claim -> claim()
            DailyVipOperation.Upgrade -> upgrade()
            null -> Unit
        }
    }

    private fun ContentFailure.toClaimFailure(): DailyVipUiState =
        if (this is ContentFailure.RiskVerificationRequired || this is ContentFailure.RiskBlocked) {
            DailyVipUiState.RiskBlocked(receiveDay)
        } else {
            DailyVipUiState.Failed(receiveDay, DailyVipOperation.Claim, this)
        }

    private fun ContentFailure.toUpgradeFailure(alreadyClaimed: Boolean): DailyVipUiState =
        if (this is ContentFailure.RiskVerificationRequired || this is ContentFailure.RiskBlocked) {
            DailyVipUiState.RiskBlocked(receiveDay)
        } else {
            DailyVipUiState.Failed(
                receiveDay = receiveDay,
                operation = DailyVipOperation.Upgrade,
                failure = this,
                alreadyClaimed = alreadyClaimed,
            )
        }
}

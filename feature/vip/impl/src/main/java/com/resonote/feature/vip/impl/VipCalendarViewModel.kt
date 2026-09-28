package com.resonote.feature.vip.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.AuthRepository
import com.resonote.core.data.VipCheckInRepository
import com.resonote.core.model.AuthState
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.RiskChallengeHandle
import com.resonote.core.model.VipCheckInRecord
import com.resonote.core.model.VipCheckInResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

internal data class VipCalendarState(
    val month: YearMonth = YearMonth.now(),
    val today: LocalDate = LocalDate.now(),
    val records: List<VipCheckInRecord> = emptyList(),
    val automatic: Boolean = true,
    val busy: Boolean = false,
    val loading: Boolean = true,
    val error: ContentFailure? = null,
    val result: VipCheckInResult? = null,
)

@HiltViewModel
class VipCalendarViewModel @Inject constructor(
    private val repository: VipCheckInRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(VipCalendarState())
    internal val state = mutableState.asStateFlow()
    val rewardApplied = repository.changes
    private var operation: Job? = null
    private var refreshJob: Job? = null
    private var verified = false
    init {
        viewModelScope.launch { repository.onColdStart() }
        viewModelScope.launch {
            repository.automaticEnabled.collect { enabled -> mutableState.update { it.copy(automatic = enabled) } }
        }
        viewModelScope.launch { repository.busy.collect { busy -> mutableState.update { it.copy(busy = busy) } } }
        viewModelScope.launch {
            auth.authState.distinctUntilChanged().collectLatest { authState ->
                operation?.cancel()
                refreshJob?.cancel()
                mutableState.value =
                    VipCalendarState(
                        automatic = mutableState.value.automatic,
                        loading = false,
                        busy = repository.busy.value,
                    )
                verified = false
                if (authState is AuthState.Authenticated) {
                    repository.records.collect { records -> mutableState.update { it.copy(records = records) } }
                }
            }
        }
    }
    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, today = LocalDate.now()) }
            try {
                val error = repository.refresh()
                mutableState.update { it.copy(loading = false, error = error) }
            } catch (
                cancelled: CancellationException,
            ) {
                throw cancelled
            } catch (
                _: Exception,
            ) {
                mutableState.update { it.copy(loading = false, error = ContentFailure.Protocol) }
            }
        }
    }
    fun previousMonth() {
        mutableState.update { it.copy(month = it.month.minusMonths(1)) }
    }
    fun nextMonth() {
        mutableState.update {
            if (it.month <
                YearMonth.from(it.today)
            ) {
                it.copy(month = it.month.plusMonths(1))
            } else {
                it
            }
        }
    }
    fun setAutomatic(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.setAutomaticEnabled(enabled)
            } catch (
                cancelled: CancellationException,
            ) {
                throw cancelled
            } catch (
                _: Exception,
            ) {
                mutableState.update { it.copy(error = ContentFailure.Protocol) }
            }
        }
    }
    fun signIn() {
        verified = false
        submit()
    }
    private fun submit() {
        if (operation?.isActive == true || state.value.busy) return
        operation = viewModelScope.launch {
            mutableState.update { it.copy(result = null) }
            try {
                val result = repository.signIn(automatic = false)
                mutableState.update { it.copy(result = result) }
            } catch (
                cancelled: CancellationException,
            ) {
                throw cancelled
            } catch (
                _: Exception,
            ) {
                mutableState.update { it.copy(error = ContentFailure.Protocol) }
            }
        }
    }
    fun resumeAfterRisk(handle: RiskChallengeHandle) {
        val failure = state.value.result?.failure as? ContentFailure.RiskVerificationRequired ?: return
        if (failure.challenge != handle || verified) return
        verified = true
        submit()
    }
    internal fun canVerify(): Boolean = !verified
}

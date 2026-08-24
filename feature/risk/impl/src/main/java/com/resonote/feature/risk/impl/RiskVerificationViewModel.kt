package com.resonote.feature.risk.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.RiskVerificationRepository
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.RiskChallengeHandle
import com.resonote.core.model.RiskVerificationMethod
import com.resonote.core.model.RiskVerificationMethodResult
import com.resonote.core.model.RiskVerificationProof
import com.resonote.core.model.RiskVerificationSubmitResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RiskVerificationViewModel @Inject constructor(private val repository: RiskVerificationRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow<RiskVerificationUiState>(RiskVerificationUiState.Loading)
    val uiState: StateFlow<RiskVerificationUiState> = mutableUiState.asStateFlow()
    private val mutableVerified = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val verified: SharedFlow<Unit> = mutableVerified.asSharedFlow()
    private var challenge: RiskChallengeHandle? = null

    fun load(handle: RiskChallengeHandle) {
        if (challenge == handle) return
        challenge = handle
        mutableUiState.value = RiskVerificationUiState.Loading
        viewModelScope.launch {
            mutableUiState.value = when (val result = repository.methodFor(handle)) {
                is RiskVerificationMethodResult.Available -> when (val method = result.method) {
                    RiskVerificationMethod.Sms -> RiskVerificationUiState.Sms()
                    is RiskVerificationMethod.Tencent -> RiskVerificationUiState.Tencent(method.applicationId)
                    is RiskVerificationMethod.Unsupported -> RiskVerificationUiState.Unsupported(method.type)
                }
                is RiskVerificationMethodResult.Failed -> RiskVerificationUiState.Failed(result.failure)
            }
        }
    }

    fun updateSmsCode(value: String) {
        val state = mutableUiState.value as? RiskVerificationUiState.Sms ?: return
        mutableUiState.value = state.copy(code = value.filter(Char::isDigit).take(8), error = null)
    }

    fun submitSms() {
        val state = mutableUiState.value as? RiskVerificationUiState.Sms ?: return
        if (state.code.isBlank() || state.submitting) return
        submit(RiskVerificationProof.Sms(state.code)) { state.copy(submitting = it, error = null) }
    }

    fun submitTencent(ticket: String, randomString: String, applicationId: String) {
        val state = mutableUiState.value as? RiskVerificationUiState.Tencent ?: return
        if (state.submitting || state.applicationId != applicationId) return
        submit(RiskVerificationProof.Tencent(ticket, randomString, applicationId)) {
            state.copy(submitting = it)
        }
    }

    fun reportTencentFailure() {
        mutableUiState.value = RiskVerificationUiState.Failed(ContentFailure.Protocol)
    }

    fun retry() {
        val handle = challenge ?: return
        challenge = null
        load(handle)
    }

    private fun submit(proof: RiskVerificationProof, busyState: (Boolean) -> RiskVerificationUiState) {
        val handle = challenge ?: return
        mutableUiState.value = busyState(true)
        viewModelScope.launch {
            when (val result = repository.submit(handle, proof)) {
                RiskVerificationSubmitResult.Verified -> mutableVerified.tryEmit(Unit)
                is RiskVerificationSubmitResult.Failed -> {
                    mutableUiState.value = when (proof) {
                        is RiskVerificationProof.Sms -> RiskVerificationUiState.Sms(error = result.failure)
                        is RiskVerificationProof.Tencent -> RiskVerificationUiState.Failed(result.failure)
                    }
                }
            }
        }
    }
}

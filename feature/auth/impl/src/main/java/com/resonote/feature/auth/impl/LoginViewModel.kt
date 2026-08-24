package com.resonote.feature.auth.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.AuthRepository
import com.resonote.core.data.RiskVerificationRepository
import com.resonote.core.model.AuthFailure
import com.resonote.core.model.MobileCodeLoginResult
import com.resonote.core.model.PasswordLoginResult
import com.resonote.core.model.RiskChallengeHandle
import com.resonote.core.model.RiskVerificationMethod
import com.resonote.core.model.RiskVerificationMethodResult
import com.resonote.core.model.RiskVerificationProof
import com.resonote.core.model.RiskVerificationSubmitResult
import com.resonote.core.model.SendMobileCodeResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val riskRepository: RiskVerificationRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = mutableUiState.asStateFlow()
    private val mutableExternalRiskChallenges = MutableSharedFlow<RiskChallengeHandle>(extraBufferCapacity = 1)
    val externalRiskChallenges: SharedFlow<RiskChallengeHandle> = mutableExternalRiskChallenges.asSharedFlow()
    private var pendingPasswordRisk: RiskChallengeHandle? = null

    fun selectMethod(method: LoginMethod) {
        if (mutableUiState.value.isSendingCode || mutableUiState.value.isLoggingIn) return
        mutableUiState.update { it.copy(method = method, message = null, accounts = emptyList()) }
    }

    fun updateMobile(value: String) {
        mutableUiState.update {
            it.copy(mobile = value.filter(Char::isDigit).take(11), message = null, accounts = emptyList())
        }
    }

    fun updateCode(value: String) {
        mutableUiState.update {
            it.copy(code = value.filter(Char::isDigit).take(8), message = null, accounts = emptyList())
        }
    }

    fun updateUsername(value: String) {
        mutableUiState.update { it.copy(username = value, message = null) }
    }

    fun updatePassword(value: String) {
        mutableUiState.update { it.copy(password = value, message = null) }
    }

    fun updateSecuritySmsCode(value: String) {
        mutableUiState.update { state ->
            state.securitySms?.let {
                state.copy(securitySms = it.copy(code = value.filter(Char::isDigit).take(8), failed = false))
            } ?: state
        }
    }

    fun submitSecuritySms() {
        val sms = mutableUiState.value.securitySms ?: return
        if (sms.code.isBlank() || sms.submitting) return
        mutableUiState.update { it.copy(securitySms = sms.copy(submitting = true, failed = false)) }
        viewModelScope.launch {
            when (riskRepository.submit(sms.challenge, RiskVerificationProof.Sms(sms.code))) {
                RiskVerificationSubmitResult.Verified -> resumePasswordLogin(sms.challenge)
                is RiskVerificationSubmitResult.Failed -> mutableUiState.update {
                    it.copy(securitySms = sms.copy(submitting = false, failed = true))
                }
            }
        }
    }

    fun dismissSecuritySms() {
        val sms = mutableUiState.value.securitySms ?: return
        if (sms.submitting) return
        pendingPasswordRisk = null
        mutableUiState.update { it.copy(securitySms = null, isLoggingIn = false, message = null) }
    }

    fun resumeAfterExternalRisk(handle: RiskChallengeHandle) {
        if (pendingPasswordRisk != handle) return
        resumePasswordLogin(handle)
    }

    fun togglePasswordVisibility() {
        mutableUiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun sendCode() {
        val mobile = mutableUiState.value.mobile
        if (!mutableUiState.value.canSendCode) return
        mutableUiState.update { it.copy(isSendingCode = true, message = null, accounts = emptyList()) }
        viewModelScope.launch {
            val message = when (val result = repository.sendMobileCode(mobile)) {
                SendMobileCodeResult.Sent -> LoginMessage.CodeSent
                is SendMobileCodeResult.Failed -> result.failure.toMessage()
            }
            mutableUiState.update { it.copy(isSendingCode = false, message = message) }
        }
    }

    fun login() {
        val state = mutableUiState.value
        if (!state.canLogin) return
        mutableUiState.update { it.copy(isLoggingIn = true, message = null, accounts = emptyList()) }
        viewModelScope.launch {
            when (state.method) {
                LoginMethod.MobileCode -> handleMobileResult(
                    repository.loginWithMobileCode(state.mobile, state.code),
                )
                LoginMethod.Password -> handlePasswordResult(
                    repository.loginWithPassword(state.username.trim(), state.password),
                )
            }
        }
    }

    fun selectAccount(userId: String) {
        val state = mutableUiState.value
        if (state.method != LoginMethod.MobileCode || state.isLoggingIn) return
        val account = state.accounts.firstOrNull { it.userId == userId } ?: return
        mutableUiState.update { it.copy(isLoggingIn = true, message = null) }
        viewModelScope.launch {
            handleMobileResult(repository.loginWithMobileCode(state.mobile, state.code, account.userId))
        }
    }

    private fun handleMobileResult(result: MobileCodeLoginResult) {
        mutableUiState.update { state ->
            when (result) {
                MobileCodeLoginResult.Authenticated -> state.copy(isLoggingIn = false, code = "")
                is MobileCodeLoginResult.MultipleAccounts -> state.copy(
                    isLoggingIn = false,
                    accounts = result.accounts,
                    message = null,
                )
                is MobileCodeLoginResult.Failed -> state.copy(
                    isLoggingIn = false,
                    accounts = emptyList(),
                    message = result.failure.toMessage(),
                )
            }
        }
    }

    private fun handlePasswordResult(result: PasswordLoginResult) {
        val riskChallenge = (result as? PasswordLoginResult.Failed)?.failure as? AuthFailure.RiskVerificationRequired
        if (riskChallenge != null) {
            mutableUiState.update { it.copy(isLoggingIn = true, message = null) }
            preparePasswordRisk(riskChallenge.challenge)
            return
        }
        mutableUiState.update { state ->
            when (result) {
                PasswordLoginResult.Authenticated -> state.copy(isLoggingIn = false, password = "")
                is PasswordLoginResult.MultipleAccounts -> state.copy(
                    isLoggingIn = false,
                    message = LoginMessage.PasswordMultipleAccounts,
                )
                is PasswordLoginResult.Failed -> state.copy(isLoggingIn = false, message = result.failure.toMessage())
            }
        }
    }

    private fun preparePasswordRisk(challenge: RiskChallengeHandle) {
        pendingPasswordRisk = challenge
        viewModelScope.launch {
            when (val result = riskRepository.methodFor(challenge)) {
                is RiskVerificationMethodResult.Available -> when (val method = result.method) {
                    RiskVerificationMethod.Sms -> mutableUiState.update {
                        it.copy(isLoggingIn = false, securitySms = LoginSecuritySms(challenge))
                    }
                    is RiskVerificationMethod.Tencent -> {
                        mutableUiState.update { it.copy(isLoggingIn = false) }
                        mutableExternalRiskChallenges.emit(challenge)
                    }
                    is RiskVerificationMethod.Unsupported -> mutableUiState.update {
                        it.copy(isLoggingIn = false, message = LoginMessage.RiskVerificationRequired)
                    }
                }
                is RiskVerificationMethodResult.Failed -> mutableUiState.update {
                    it.copy(isLoggingIn = false, message = result.failure.toLoginMessage())
                }
            }
        }
    }

    private fun resumePasswordLogin(handle: RiskChallengeHandle) {
        if (pendingPasswordRisk != handle) return
        pendingPasswordRisk = null
        mutableUiState.update { it.copy(securitySms = null, isLoggingIn = false, message = null) }
        login()
    }
}

private fun com.resonote.core.model.ContentFailure.toLoginMessage(): LoginMessage = when (this) {
    com.resonote.core.model.ContentFailure.AuthenticationRequired -> LoginMessage.Rejected
    com.resonote.core.model.ContentFailure.Network -> LoginMessage.Network
    com.resonote.core.model.ContentFailure.ServiceRejected -> LoginMessage.Rejected
    is com.resonote.core.model.ContentFailure.RiskVerificationRequired,
    com.resonote.core.model.ContentFailure.RiskBlocked,
    -> LoginMessage.RiskVerificationRequired
    com.resonote.core.model.ContentFailure.Protocol -> LoginMessage.Protocol
}

private fun AuthFailure.toMessage(): LoginMessage = when (this) {
    AuthFailure.InvalidInput -> LoginMessage.InvalidInput
    AuthFailure.ServiceRejected -> LoginMessage.Rejected
    is AuthFailure.RiskVerificationRequired -> LoginMessage.RiskVerificationRequired
    AuthFailure.Network -> LoginMessage.Network
    AuthFailure.Protocol -> LoginMessage.Protocol
    AuthFailure.SecureStorage -> LoginMessage.SecureStorage
}

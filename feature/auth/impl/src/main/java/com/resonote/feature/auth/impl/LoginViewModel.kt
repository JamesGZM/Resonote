package com.resonote.feature.auth.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.AuthRepository
import com.resonote.core.model.AuthFailure
import com.resonote.core.model.MobileCodeLoginResult
import com.resonote.core.model.PasswordLoginResult
import com.resonote.core.model.SendMobileCodeResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = mutableUiState.asStateFlow()

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
        mutableUiState.update { state ->
            when (result) {
                PasswordLoginResult.Authenticated -> state.copy(isLoggingIn = false, password = "")
                is PasswordLoginResult.MultipleAccounts -> state.copy(
                    isLoggingIn = false,
                    message = LoginMessage.PasswordMultipleAccounts,
                )
                is PasswordLoginResult.Failed -> state.copy(
                    isLoggingIn = false,
                    message = result.failure.toMessage(),
                )
            }
        }
    }
}

private fun AuthFailure.toMessage(): LoginMessage = when (this) {
    AuthFailure.InvalidInput -> LoginMessage.InvalidInput
    AuthFailure.ServiceRejected -> LoginMessage.Rejected
    is AuthFailure.RiskVerificationRequired -> LoginMessage.RiskVerificationRequired
    AuthFailure.Network -> LoginMessage.Network
    AuthFailure.Protocol -> LoginMessage.Protocol
    AuthFailure.SecureStorage -> LoginMessage.SecureStorage
}

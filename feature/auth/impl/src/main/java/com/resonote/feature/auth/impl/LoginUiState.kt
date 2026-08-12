package com.resonote.feature.auth.impl

import com.resonote.core.model.AuthAccountOption

enum class LoginMethod {
    MobileCode,
    Password,
}

enum class LoginMessage {
    CodeSent,
    InvalidInput,
    Rejected,
    RiskVerificationRequired,
    Network,
    Protocol,
    SecureStorage,
    PasswordMultipleAccounts,
}

data class LoginUiState(
    val method: LoginMethod = LoginMethod.MobileCode,
    val mobile: String = "",
    val code: String = "",
    val username: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isSendingCode: Boolean = false,
    val isLoggingIn: Boolean = false,
    val message: LoginMessage? = null,
    val accounts: List<AuthAccountOption> = emptyList(),
) {
    val canSendCode: Boolean
        get() = mobile.matches(Regex("1\\d{10}")) && !isSendingCode && !isLoggingIn

    val canLogin: Boolean
        get() = when (method) {
            LoginMethod.MobileCode -> mobile.matches(Regex("1\\d{10}")) && code.isNotBlank()
            LoginMethod.Password -> username.isNotBlank() && password.isNotEmpty()
        } && !isSendingCode && !isLoggingIn
}

package com.resonote.core.model

sealed interface AuthState {
    data object Anonymous : AuthState

    data class Authenticated(val userId: String) : AuthState {
        override fun toString(): String = "Authenticated(userId=<redacted>)"
    }
}

sealed interface AuthFailure {
    data object InvalidInput : AuthFailure
    data object ServiceRejected : AuthFailure
    data object RiskVerificationUnavailable : AuthFailure
    data object Network : AuthFailure
    data object Protocol : AuthFailure
    data object SecureStorage : AuthFailure
}

sealed interface SendMobileCodeResult {
    data object Sent : SendMobileCodeResult
    data class Failed(val failure: AuthFailure) : SendMobileCodeResult
}

data class AuthAccountOption(
    val userId: String,
    val nickname: String,
    val avatarUrl: String?,
    val grade: String?,
) {
    override fun toString(): String = "AuthAccountOption(userId=<redacted>, nickname=<redacted>)"
}

sealed interface MobileCodeLoginResult {
    data object Authenticated : MobileCodeLoginResult
    data class MultipleAccounts(val accounts: List<AuthAccountOption>) : MobileCodeLoginResult
    data class Failed(val failure: AuthFailure) : MobileCodeLoginResult
}

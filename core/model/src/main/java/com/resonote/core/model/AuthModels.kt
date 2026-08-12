package com.resonote.core.model

sealed interface AuthState {
    data object Anonymous : AuthState

    data class AuthenticationRequired(val reason: AuthGateReason) : AuthState

    data class Authenticated(val userId: String) : AuthState {
        override fun toString(): String = "Authenticated(userId=<redacted>)"
    }
}

enum class AuthGateReason {
    Required,
    Expired,
}

sealed interface AuthFailure {
    data object InvalidInput : AuthFailure

    data object ServiceRejected : AuthFailure

    data class RiskVerificationRequired(val challenge: RiskChallengeHandle) : AuthFailure

    data object Network : AuthFailure

    data object Protocol : AuthFailure

    data object SecureStorage : AuthFailure
}

sealed interface SendMobileCodeResult {
    data object Sent : SendMobileCodeResult

    data class Failed(val failure: AuthFailure) : SendMobileCodeResult
}

data class AuthAccountOption(val userId: String, val nickname: String, val avatarUrl: String?, val grade: String?) {
    override fun toString(): String = "AuthAccountOption(userId=<redacted>, nickname=<redacted>)"
}

sealed interface MobileCodeLoginResult {
    data object Authenticated : MobileCodeLoginResult

    data class MultipleAccounts(val accounts: List<AuthAccountOption>) : MobileCodeLoginResult

    data class Failed(val failure: AuthFailure) : MobileCodeLoginResult
}

sealed interface PasswordLoginResult {
    data object Authenticated : PasswordLoginResult

    data class MultipleAccounts(val accounts: List<AuthAccountOption>) : PasswordLoginResult

    data class Failed(val failure: AuthFailure) : PasswordLoginResult
}

sealed interface QrLoginKeyResult {
    data class Ready(val key: String) : QrLoginKeyResult {
        override fun toString(): String = "Ready(key=<redacted>)"
    }
    data class Failed(val failure: AuthFailure) : QrLoginKeyResult
}

sealed interface QrLoginCheckResult {
    data object Waiting : QrLoginCheckResult
    data class Scanned(val nickname: String) : QrLoginCheckResult {
        override fun toString(): String = "Scanned(nickname=<redacted>)"
    }
    data object Expired : QrLoginCheckResult
    data object Authenticated : QrLoginCheckResult
    data class Failed(val failure: AuthFailure) : QrLoginCheckResult
}

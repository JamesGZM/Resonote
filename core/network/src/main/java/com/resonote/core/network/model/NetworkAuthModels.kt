package com.resonote.core.network.model

import com.resonote.core.network.session.ApiSession

data class NetworkAccountOption(
    val userId: String,
    val nickname: String,
    val avatarUrl: String?,
    val grade: String?,
) {
    override fun toString(): String = "NetworkAccountOption(userId=<redacted>, nickname=<redacted>)"
}

sealed interface NetworkMobileCodeLoginResult {
    data class Authenticated(val session: ApiSession) : NetworkMobileCodeLoginResult
    data class MultipleAccounts(val accounts: List<NetworkAccountOption>) : NetworkMobileCodeLoginResult
}

sealed interface NetworkPasswordLoginResult {
    data class Authenticated(val session: ApiSession) : NetworkPasswordLoginResult
    data class MultipleAccounts(val accounts: List<NetworkAccountOption>) : NetworkPasswordLoginResult
}

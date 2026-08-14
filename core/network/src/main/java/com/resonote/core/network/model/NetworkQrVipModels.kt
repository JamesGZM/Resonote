package com.resonote.core.network.model

import com.resonote.core.network.session.ApiSession

sealed interface NetworkQrLoginStatus {
    data object Waiting : NetworkQrLoginStatus
    data class Scanned(val nickname: String) : NetworkQrLoginStatus {
        override fun toString(): String = "Scanned(nickname=<redacted>)"
    }
    data object Expired : NetworkQrLoginStatus
    data class Authenticated(val session: ApiSession) : NetworkQrLoginStatus
}

data class NetworkVipRewardResult(val alreadyDone: Boolean, val canUpgrade: Boolean)

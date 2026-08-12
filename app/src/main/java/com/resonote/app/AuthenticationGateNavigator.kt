package com.resonote.app

import androidx.navigation3.runtime.NavKey
import com.resonote.core.model.AuthGateReason
import com.resonote.core.model.AuthState
import com.resonote.core.navigation.LoginGateNavKey
import com.resonote.feature.vip.api.DailyVipNavKey

internal fun MutableList<NavKey>.synchronizeAuthenticationGate(authState: AuthState) {
    val destination = when (authState) {
        is AuthState.AuthenticationRequired -> LoginGateNavKey(authState.reason == AuthGateReason.Expired)
        is AuthState.Authenticated, AuthState.Anonymous -> null
    }
    removeAll { it is LoginGateNavKey }
    destination?.let(::add)
}

internal fun MutableList<NavKey>.navigateToDailyVip(authState: AuthState) {
    val destination: NavKey = when (authState) {
        is AuthState.Authenticated -> DailyVipNavKey
        AuthState.Anonymous -> LoginGateNavKey(sessionExpired = false)
        is AuthState.AuthenticationRequired -> LoginGateNavKey(
            sessionExpired = authState.reason == AuthGateReason.Expired,
        )
    }
    if (lastOrNull() != destination) add(destination)
}

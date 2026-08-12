package com.resonote.app

import androidx.navigation3.runtime.NavKey
import com.resonote.core.model.AuthGateReason
import com.resonote.core.model.AuthState
import com.resonote.core.navigation.LoginGateNavKey
import com.resonote.core.navigation.LoginContinuation
import com.resonote.feature.cloud.api.CloudNavKey
import com.resonote.feature.vip.api.DailyVipNavKey

internal fun MutableList<NavKey>.synchronizeAuthenticationGate(authState: AuthState) {
    val continuation = filterIsInstance<LoginGateNavKey>().lastOrNull()?.continuation
    val destination = when (authState) {
        is AuthState.AuthenticationRequired -> LoginGateNavKey(
            sessionExpired = authState.reason == AuthGateReason.Expired,
            continuation = continuation,
        )
        is AuthState.Authenticated, AuthState.Anonymous -> null
    }
    removeAll { it is LoginGateNavKey }
    destination?.let(::add)
    if (authState is AuthState.Authenticated && continuation == LoginContinuation.Cloud && none { it is CloudNavKey }) {
        add(CloudNavKey)
    }
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

internal fun MutableList<NavKey>.navigateToCloud(authState: AuthState) {
    val destination: NavKey = when (authState) {
        is AuthState.Authenticated -> CloudNavKey
        AuthState.Anonymous -> LoginGateNavKey(
            sessionExpired = false,
            continuation = LoginContinuation.Cloud,
        )
        is AuthState.AuthenticationRequired -> LoginGateNavKey(
            sessionExpired = authState.reason == AuthGateReason.Expired,
            continuation = LoginContinuation.Cloud,
        )
    }
    if (lastOrNull() != destination) add(destination)
}

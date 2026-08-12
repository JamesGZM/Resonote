package com.resonote.app

import androidx.navigation3.runtime.NavKey
import com.resonote.core.model.AuthGateReason
import com.resonote.core.model.AuthState
import com.resonote.core.navigation.LoginGateNavKey

internal fun MutableList<NavKey>.synchronizeAuthenticationGate(authState: AuthState) {
    val destination = when (authState) {
        is AuthState.AuthenticationRequired -> LoginGateNavKey(authState.reason == AuthGateReason.Expired)
        is AuthState.Authenticated, AuthState.Anonymous -> null
    }
    removeAll { it is LoginGateNavKey }
    destination?.let(::add)
}

package com.resonote.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class LoginGateNavKey(val sessionExpired: Boolean) : NavKey

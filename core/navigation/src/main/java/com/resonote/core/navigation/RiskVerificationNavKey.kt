package com.resonote.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class RiskVerificationNavKey(
    val challengeHandle: String,
    val continuation: RiskVerificationContinuation = RiskVerificationContinuation.ReturnOnly,
) : NavKey {
    init {
        require(challengeHandle.isNotBlank()) { "challengeHandle must not be blank" }
    }
}

@Serializable
enum class RiskVerificationContinuation { ReturnOnly, Login, DailyVip, }

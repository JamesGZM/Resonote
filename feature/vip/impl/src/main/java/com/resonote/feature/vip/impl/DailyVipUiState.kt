package com.resonote.feature.vip.impl

import com.resonote.core.model.ContentFailure
import com.resonote.core.model.RiskChallengeHandle

sealed interface DailyVipUiState {
    val receiveDay: String

    data class Ready(override val receiveDay: String) : DailyVipUiState

    data class Claiming(override val receiveDay: String) : DailyVipUiState

    data class UpgradeChoice(override val receiveDay: String, val alreadyClaimed: Boolean) : DailyVipUiState

    data class ClaimComplete(override val receiveDay: String, val alreadyClaimed: Boolean) : DailyVipUiState

    data class Upgrading(override val receiveDay: String, val alreadyClaimed: Boolean) : DailyVipUiState

    data class UpgradeComplete(override val receiveDay: String, val alreadyUpgraded: Boolean) : DailyVipUiState

    data class RiskBlocked(override val receiveDay: String) : DailyVipUiState

    data class RiskVerificationRequired(
        override val receiveDay: String,
        val challenge: RiskChallengeHandle,
        val operation: DailyVipOperation,
        val alreadyClaimed: Boolean = false,
    ) : DailyVipUiState

    data class Failed(
        override val receiveDay: String,
        val operation: DailyVipOperation,
        val failure: ContentFailure,
        val alreadyClaimed: Boolean = false,
    ) : DailyVipUiState
}

enum class DailyVipOperation { Claim, Upgrade }

package com.resonote.feature.risk.impl

import com.resonote.core.model.ContentFailure

sealed interface RiskVerificationUiState {
    data object Loading : RiskVerificationUiState
    data class Tencent(val applicationId: String, val submitting: Boolean = false) : RiskVerificationUiState
    data class Sms(val code: String = "", val submitting: Boolean = false, val error: ContentFailure? = null) :
        RiskVerificationUiState
    data class Failed(val failure: ContentFailure) : RiskVerificationUiState
    data class Unsupported(val type: Int) : RiskVerificationUiState
}

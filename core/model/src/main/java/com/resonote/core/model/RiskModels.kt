package com.resonote.core.model

@JvmInline
value class RiskChallengeHandle(val value: String) {
    init {
        require(value.isNotBlank()) { "Risk challenge handle must not be blank" }
    }

    override fun toString(): String = "RiskChallengeHandle(<redacted>)"
}

sealed interface RiskVerificationMethod {
    data object Sms : RiskVerificationMethod

    data class Tencent(val applicationId: String) : RiskVerificationMethod {
        override fun toString(): String = "Tencent(applicationId=<redacted>)"
    }

    data class Unsupported(val type: Int) : RiskVerificationMethod
}

sealed interface RiskVerificationProof {
    data class Sms(val code: String) : RiskVerificationProof {
        init {
            require(code.isNotBlank()) { "SMS verification code must not be blank" }
        }

        override fun toString(): String = "Sms(code=<redacted>)"
    }

    data class Tencent(val ticket: String, val randomString: String, val applicationId: String) :
        RiskVerificationProof {
        init {
            require(ticket.isNotBlank()) { "Tencent ticket must not be blank" }
            require(randomString.isNotBlank()) { "Tencent random string must not be blank" }
            require(applicationId.isNotBlank()) { "Tencent application id must not be blank" }
        }

        override fun toString(): String =
            "Tencent(ticket=<redacted>, randomString=<redacted>, applicationId=<redacted>)"
    }
}

sealed interface RiskVerificationMethodResult {
    data class Available(val method: RiskVerificationMethod) : RiskVerificationMethodResult

    data class Failed(val failure: ContentFailure) : RiskVerificationMethodResult
}

sealed interface RiskVerificationSubmitResult {
    data object Verified : RiskVerificationSubmitResult

    data class Failed(val failure: ContentFailure) : RiskVerificationSubmitResult
}

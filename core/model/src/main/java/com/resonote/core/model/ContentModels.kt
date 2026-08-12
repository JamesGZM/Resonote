package com.resonote.core.model

sealed interface ContentFailure {
    data object AuthenticationRequired : ContentFailure

    data object Network : ContentFailure

    data object ServiceRejected : ContentFailure

    data class RiskVerificationRequired(val challenge: RiskChallengeHandle) : ContentFailure

    data object Protocol : ContentFailure
}

sealed interface CollectionLoadResult<out T> {
    data class Available<T>(val value: T) : CollectionLoadResult<T>

    data class Failed(val failure: ContentFailure) : CollectionLoadResult<Nothing>
}

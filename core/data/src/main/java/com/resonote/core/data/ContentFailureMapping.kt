package com.resonote.core.data

import com.resonote.core.model.ContentFailure
import com.resonote.core.network.ApiException
import com.resonote.core.network.ApiAuthenticationRequiredException
import com.resonote.core.network.ApiHttpException
import com.resonote.core.network.ApiNetworkException
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiRiskBlockedException
import com.resonote.core.network.ApiRiskException
import com.resonote.core.network.ApiServiceException

internal fun ApiException.toContentFailure(riskChallenges: RiskChallengeRegistry): ContentFailure =
    when (this) {
        is ApiAuthenticationRequiredException -> ContentFailure.AuthenticationRequired
        is ApiRiskException -> ContentFailure.RiskVerificationRequired(riskChallenges.register(challenge))
        is ApiRiskBlockedException -> ContentFailure.RiskBlocked
        is ApiNetworkException -> ContentFailure.Network
        is ApiServiceException, is ApiHttpException -> ContentFailure.ServiceRejected
        is ApiProtocolException -> ContentFailure.Protocol
        else -> ContentFailure.Protocol
    }

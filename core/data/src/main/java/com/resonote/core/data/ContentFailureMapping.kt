package com.resonote.core.data

import com.resonote.core.model.ContentFailure
import com.resonote.core.network.ApiException
import com.resonote.core.network.ApiHttpException
import com.resonote.core.network.ApiNetworkException
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiRiskException
import com.resonote.core.network.ApiServiceException

internal fun Throwable.toContentFailure(): ContentFailure =
    when (this) {
        is ApiRiskException -> ContentFailure.RiskVerificationUnavailable
        is ApiNetworkException -> ContentFailure.Network
        is ApiServiceException, is ApiHttpException -> ContentFailure.ServiceRejected
        is ApiProtocolException, is ApiException -> ContentFailure.Protocol
        else -> ContentFailure.Protocol
    }

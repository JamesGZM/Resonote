package com.resonote.core.network.retrofit

import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiRiskBlockedException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.VipNetworkDataSource
import com.resonote.core.network.api.MusicApi
import com.resonote.core.network.model.NetworkVipRewardResult
import com.resonote.core.network.protocol.DeviceRegistrationCoordinator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RealVipNetworkDataSource @Inject constructor(
    private val musicApi: MusicApi,
    private val registration: DeviceRegistrationCoordinator,
    private val calls: ApiCallExecutor,
    private val responses: ApiResponseVerifier,
) : VipNetworkDataSource {
    override suspend fun claimDailyVip(receiveDay: String): NetworkVipRewardResult {
        require(RECEIVE_DAY_PATTERN.matches(receiveDay)) { "receiveDay must use yyyy-MM-dd" }
        requireAuthenticatedSession()
        val response = calls.execute { musicApi.claimDailyVip(receiveDay = receiveDay) }
        val serviceCode = responses.serviceFailureCodeOrNull(response)
        if (serviceCode == DAILY_VIP_RISK_BLOCKED_CODE) throw ApiRiskBlockedException(serviceCode)
        responses.requireNoRiskChallenge(response)
        return when (serviceCode) {
            null -> NetworkVipRewardResult(alreadyDone = false, canUpgrade = true)
            DAILY_VIP_ALREADY_DONE_CODE -> NetworkVipRewardResult(alreadyDone = true, canUpgrade = true)
            else -> throw ApiServiceException(serviceCode)
        }
    }

    override suspend fun upgradeDailyVip(): NetworkVipRewardResult {
        val session = requireAuthenticatedSession()
        val userId = requireNotNull(session.userId).toLongOrNull()
            ?: throw ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)
        val response = calls.execute { musicApi.upgradeDailyVip(userId = userId) }
        val serviceCode = responses.serviceFailureCodeOrNull(response)
        if (serviceCode == DAILY_VIP_RISK_BLOCKED_CODE) throw ApiRiskBlockedException(serviceCode)
        responses.requireNoRiskChallenge(response)
        return when (serviceCode) {
            null -> NetworkVipRewardResult(alreadyDone = false, canUpgrade = false)
            DAILY_VIP_ALREADY_DONE_CODE -> NetworkVipRewardResult(alreadyDone = true, canUpgrade = false)
            else -> throw ApiServiceException(serviceCode)
        }
    }

    private suspend fun requireAuthenticatedSession() =
        registration.requireAuthenticatedSession()

    private companion object {
        val RECEIVE_DAY_PATTERN = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        const val DAILY_VIP_ALREADY_DONE_CODE = "131001"
        const val DAILY_VIP_RISK_BLOCKED_CODE = "20028"
    }
}

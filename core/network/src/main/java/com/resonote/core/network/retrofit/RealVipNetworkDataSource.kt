package com.resonote.core.network.retrofit

import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.ApiRiskBlockedException
import com.resonote.core.network.ApiServiceException
import com.resonote.core.network.NetworkVipCheckInRecord
import com.resonote.core.network.VipNetworkDataSource
import com.resonote.core.network.api.MusicApi
import com.resonote.core.network.model.NetworkVipRewardResult
import com.resonote.core.network.protocol.ApiServiceAuthenticationPolicy
import com.resonote.core.network.protocol.DeviceRegistrationCoordinator
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RealVipNetworkDataSource @Inject constructor(
    private val musicApi: MusicApi,
    private val registration: DeviceRegistrationCoordinator,
    private val calls: ApiCallExecutor,
    private val responses: ApiResponseVerifier,
) : VipNetworkDataSource {
    override suspend fun serverTimeSeconds(): Long {
        val session = requireAuthenticatedSession()
        val response = calls.execute {
            musicApi.serverTime(
                buildJsonObject {
                    put("token", session.token)
                    put("userid", session.userId)
                },
            )
        }
        responses.requireSuccess(response)
        return (response.data as? JsonObject)?.get("timestamp")?.jsonPrimitive?.longOrNull
            ?.takeIf { it > 0 } ?: throw ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)
    }

    override suspend fun vipCheckInRecords(): List<NetworkVipCheckInRecord> {
        requireAuthenticatedSession()
        val response = calls.execute { musicApi.vipCheckInRecords() }
        responses.requireSuccess(response)
        val data = response.data as? JsonObject
            ?: throw ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)
        val items = (data["list"] ?: data["record_list"]) as? JsonArray
            ?: throw ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)
        return items.map { item ->
            val record = item as? JsonObject
                ?: throw ApiProtocolException(ApiProtocolException.Reason.MalformedResponse)
            val day = (record["day"] as? JsonPrimitive)?.contentOrNull
                ?: throw ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)
            val date = runCatching {
                val timestamp = day.toLongOrNull()
                if (timestamp != null) {
                    Instant.ofEpochMilli(if (timestamp > 1_000_000_000_000) timestamp else timestamp * 1000)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                } else {
                    LocalDate.parse(day.take(10).replace('/', '-'))
                }
            }.getOrElse { throw ApiProtocolException(ApiProtocolException.Reason.MalformedResponse) }
            NetworkVipCheckInRecord(date.toString(), record["vip_type"]?.jsonPrimitive?.contentOrNull == "svip")
        }
    }

    override suspend fun claimDailyVip(receiveDay: String): NetworkVipRewardResult {
        require(RECEIVE_DAY_PATTERN.matches(receiveDay)) { "receiveDay must use yyyy-MM-dd" }
        requireAuthenticatedSession()
        val context = responses.authenticationContext()
        val response = calls.execute { musicApi.claimDailyVip(receiveDay = receiveDay) }
        val serviceCode = responses.serviceFailureCodeOrNull(response)
        try {
            responses.requireNoRiskChallenge(response)
        } catch (failure: ApiProtocolException) {
            if (serviceCode == DAILY_VIP_RISK_BLOCKED_CODE &&
                failure.reason == ApiProtocolException.Reason.MissingRiskEvent
            ) {
                throw ApiRiskBlockedException(serviceCode)
            }
            throw failure
        }
        if (serviceCode != null) {
            responses.requireValidAuthentication(
                ApiServiceAuthenticationPolicy.DailyVipSessionExpired.serviceCodes,
                serviceCode,
                context,
            )
        } else if (response.status?.trim() != "1" && response.errorCode?.trim() != "0") {
            throw ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)
        }
        if (serviceCode == DAILY_VIP_RISK_BLOCKED_CODE) throw ApiRiskBlockedException(serviceCode)
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
        val context = responses.authenticationContext()
        val response = calls.execute { musicApi.upgradeDailyVip(userId = userId) }
        val serviceCode = responses.serviceFailureCodeOrNull(response)
        try {
            responses.requireNoRiskChallenge(response)
        } catch (failure: ApiProtocolException) {
            if (serviceCode == DAILY_VIP_RISK_BLOCKED_CODE &&
                failure.reason == ApiProtocolException.Reason.MissingRiskEvent
            ) {
                throw ApiRiskBlockedException(serviceCode)
            }
            throw failure
        }
        if (serviceCode != null) {
            responses.requireValidAuthentication(
                ApiServiceAuthenticationPolicy.DailyVipSessionExpired.serviceCodes,
                serviceCode,
                context,
            )
        } else if (response.status?.trim() != "1" && response.errorCode?.trim() != "0") {
            throw ApiProtocolException(ApiProtocolException.Reason.MissingRequiredField)
        }
        if (serviceCode == DAILY_VIP_RISK_BLOCKED_CODE) throw ApiRiskBlockedException(serviceCode)
        return when (serviceCode) {
            null -> NetworkVipRewardResult(alreadyDone = false, canUpgrade = false)
            DAILY_VIP_ALREADY_DONE_CODE, "20030" -> NetworkVipRewardResult(alreadyDone = true, canUpgrade = false)
            else -> throw ApiServiceException(serviceCode)
        }
    }

    private suspend fun requireAuthenticatedSession() = registration.requireAuthenticatedSession()

    private companion object {
        val RECEIVE_DAY_PATTERN = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        const val DAILY_VIP_ALREADY_DONE_CODE = "131001"
        const val DAILY_VIP_RISK_BLOCKED_CODE = "20028"
    }
}

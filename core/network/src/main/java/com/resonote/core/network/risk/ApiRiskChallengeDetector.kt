package com.resonote.core.network.risk

import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.api.model.MusicApiResponse
import com.resonote.core.network.protocol.ApiRawResponse
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import javax.inject.Inject

internal class ApiRiskChallengeDetector @Inject constructor() {
    fun detect(response: ApiRawResponse): ApiRiskChallenge? {
        val root = response.body
        val data = root?.get("data") as? JsonObject
        return detect(
            ApiRiskMetadata(
                serviceCode = root?.text("error_code") ?: data?.text("error_code"),
                status = root?.text("status") ?: data?.text("status"),
                eventId = root?.text("ssaCode")
                    ?: root?.text("ssa_code")
                    ?: data?.text("ssaCode")
                    ?: data?.text("ssa_code")
                    ?: response.header("ssa-code")
                    ?: response.header("ssa")
                    ?: response.header("ssaCode"),
                sid = root?.text("sid") ?: data?.text("sid"),
                edt = root?.text("edt") ?: data?.text("edt"),
            ),
        )
    }

    fun detect(response: MusicApiResponse): ApiRiskChallenge? {
        val nested = response.risk
        return detect(
            ApiRiskMetadata(
                serviceCode = response.errorCode ?: nested?.errorCode,
                status = response.status ?: nested?.status,
                eventId = response.ssaCode ?: response.legacySsaCode ?: nested?.ssaCode ?: nested?.legacySsaCode,
                sid = response.sid ?: nested?.sid,
                edt = response.edt ?: nested?.edt,
            ),
        )
    }

    private fun detect(metadata: ApiRiskMetadata): ApiRiskChallenge? {
        val serviceCode = metadata.serviceCode
        val status = metadata.status
        val eventId = metadata.eventId
        val failed = if (status == null) true else status.toDoubleOrNull() == 0.0
        val isRisk = serviceCode == RISK_CODE || (failed && !eventId.isNullOrBlank())
        if (!isRisk) return null
        if (eventId.isNullOrBlank()) throw ApiProtocolException(ApiProtocolException.Reason.MissingRiskEvent)
        return ApiRiskChallenge(
            eventId = eventId,
            sid = metadata.sid,
            edt = metadata.edt,
            serviceCode = serviceCode,
        )
    }

    private fun ApiRawResponse.header(name: String): String? = headers.entries.firstOrNull { (key) ->
        key.equals(name, ignoreCase = true)
    }?.value?.firstOrNull()?.takeIf(String::isNotBlank)

    private fun JsonObject.text(name: String): String? =
        (get(name) as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)

    private companion object {
        const val RISK_CODE = "20028"
    }
}

internal data class ApiRiskMetadata(
    val serviceCode: String?,
    val status: String?,
    val eventId: String?,
    val sid: String?,
    val edt: String?,
)

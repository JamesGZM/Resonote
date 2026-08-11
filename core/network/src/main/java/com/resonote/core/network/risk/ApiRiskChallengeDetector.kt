package com.resonote.core.network.risk

import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.retrofit.ApiRawResponse
import javax.inject.Inject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal class ApiRiskChallengeDetector @Inject constructor() {
    fun detect(response: ApiRawResponse): ApiRiskChallenge? {
        val root = response.body ?: return null
        val data = root["data"] as? JsonObject
        val serviceCode = root.text("error_code") ?: data?.text("error_code")
        val eventId =
            root.text("ssaCode")
                ?: root.text("ssa_code")
                ?: data?.text("ssaCode")
                ?: data?.text("ssa_code")
                ?: response.header("ssa-code")
                ?: response.header("ssa")
                ?: response.header("ssaCode")
        val isRisk = serviceCode == RISK_CODE || !eventId.isNullOrBlank()
        if (!isRisk) return null
        if (eventId.isNullOrBlank()) throw ApiProtocolException(ApiProtocolException.Reason.MissingRiskEvent)
        return ApiRiskChallenge(
            eventId = eventId,
            sid = root.text("sid") ?: data?.text("sid"),
            edt = root.text("edt") ?: data?.text("edt"),
            serviceCode = serviceCode,
        )
    }

    private fun ApiRawResponse.header(name: String): String? =
        headers.entries.firstOrNull { (key) -> key.equals(name, ignoreCase = true) }?.value?.firstOrNull()?.takeIf(String::isNotBlank)

    private fun JsonObject.text(name: String): String? =
        (get(name) as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)

    private companion object {
        const val RISK_CODE = "20028"
    }
}

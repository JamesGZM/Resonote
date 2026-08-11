package com.resonote.core.network.risk

import com.google.common.truth.Truth.assertThat
import com.resonote.core.network.ApiProtocolException
import com.resonote.core.network.retrofit.ApiRawResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertThrows
import org.junit.Test

class ApiRiskChallengeDetectorTest {
    private val detector = ApiRiskChallengeDetector()

    @Test
    fun detectsNestedNumericRiskAndHeaderEvent() {
        val response =
            response(
                body = """{"status":0,"data":{"error_code":20028,"sid":"sid","edt":"edt"}}""",
                headers = mapOf("SSA-Code" to listOf("secret-event-123")),
            )

        val challenge = detector.detect(response)

        assertThat(challenge?.eventId).isEqualTo("secret-event-123")
        assertThat(challenge?.serviceCode).isEqualTo("20028")
        assertThat(challenge?.sid).isEqualTo("sid")
        assertThat(challenge?.toString()).doesNotContain("secret-event-123")
    }

    @Test
    fun riskCodeWithoutEventIsRejected() {
        val exception = assertThrows(ApiProtocolException::class.java) {
            detector.detect(response("""{"status":"0","error_code":"20028"}"""))
        }

        assertThat(exception.reason).isEqualTo(ApiProtocolException.Reason.MissingRiskEvent)
    }

    @Test
    fun ordinaryFailureIsNotRisk() {
        assertThat(detector.detect(response("""{"status":0,"error_code":123}"""))).isNull()
    }

    private fun response(
        body: String,
        headers: Map<String, List<String>> = emptyMap(),
    ) = ApiRawResponse(200, headers, Json.parseToJsonElement(body).jsonObject)
}

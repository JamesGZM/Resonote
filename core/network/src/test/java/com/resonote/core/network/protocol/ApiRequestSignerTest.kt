package com.resonote.core.network.protocol

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ApiRequestSignerTest {
    private val signer = ApiRequestSigner()

    @Test
    fun androidLiteSignatureMatchesFixedMitContract() {
        val signature =
            signer.sign(
                linkedMapOf(
                    "uuid" to "-",
                    "mid" to "123456",
                    "keyword" to "周杰伦",
                    "dfid" to "-",
                    "clientver" to "11440",
                    "clienttime" to "1700000000",
                    "appid" to "3116",
                ),
            )

        assertThat(signature).isEqualTo("c045082e23beac5e418faa229dcdac4d")
    }

    @Test
    fun androidBodyAndLoginParamsKeyMatchNodeGoldenValues() {
        val parameters = linkedMapOf(
            "appid" to "3116",
            "clienttime" to "1700000000",
            "clientver" to "11440",
            "dfid" to "fixture-dfid",
            "mid" to "fixture-mid",
            "uuid" to "-",
        )
        val body = """{"businessid":5,"mobile":"13800000000","plat":3}""".encodeToByteArray()

        assertThat(signer.sign(parameters, body)).isEqualTo("3f1957a934c09d18916a2ceaf9655335")
        assertThat(signer.signParamsKey("1700000000123")).isEqualTo("1e147fdee80c20bcf6e0a1d8681c84b9")
    }
}

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
}

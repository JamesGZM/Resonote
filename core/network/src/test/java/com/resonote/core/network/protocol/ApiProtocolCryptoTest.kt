package com.resonote.core.network.protocol

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ApiProtocolCryptoTest {
    private val crypto = ApiProtocolCrypto(ProtocolRandom { length -> "A".repeat(length) })

    @Test
    fun temporaryAesRoundTripsAndRawLiteRsaHasKeySizedUppercaseHex() {
        val encrypted = crypto.encryptTemporary("""{"mobile":"13800000000","code":"246810"}""")

        assertThat(encrypted.temporaryKey).isEqualTo("aaaaaaaaaaaaaaaa")
        assertThat(crypto.decryptTemporary(encrypted.ciphertextHex, encrypted.temporaryKey))
            .isEqualTo("""{"mobile":"13800000000","code":"246810"}""")

        val rsa = crypto.rawLiteRsa("""{"clienttime_ms":1700000000123,"key":"aaaaaaaaaaaaaaaa"}""").uppercase()
        assertThat(rsa).hasLength(256)
        assertThat(rsa).matches("[0-9A-F]{256}")
    }
}

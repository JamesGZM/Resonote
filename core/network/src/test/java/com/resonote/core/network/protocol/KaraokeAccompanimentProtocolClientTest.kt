package com.resonote.core.network.protocol

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KaraokeAccompanimentProtocolClientTest {
    @Test
    fun signature_matchesCanonicalReferenceAlgorithm() {
        val parameters = linkedMapOf(
            "isteen" to "0",
            "mixId" to "123",
            "usemkv" to "1",
            "platform" to "2",
            "fileName" to "歌手 - 歌名",
            "hash" to "ABCDEF",
            "version" to "12375",
            "appid" to "1005",
        )

        assertThat(karaokeAccompanimentSign(parameters)).isEqualTo("8370f103558c6c5c")
    }
}

package com.resonote.core.network.protocol

import com.google.common.truth.Truth.assertThat
import okhttp3.Request
import org.junit.Test

class RedactedNetworkLoggingInterceptorTest {
    @Test
    fun labelExcludesQueryAndSensitiveValues() {
        val request =
            Request.Builder()
                .url("https://example.test/search?token=secret&mid=device&signature=signed")
                .header("Cookie", "userid=private")
                .build()

        val label = request.redactedLabel()

        assertThat(label).isEqualTo("GET https://example.test/search")
        assertThat(label).doesNotContain("secret")
        assertThat(label).doesNotContain("device")
        assertThat(label).doesNotContain("private")
    }

    @Test
    fun failureDescriptionIncludesCauseButRedactsCredentials() {
        val failure = java.io.IOException(
            "request to https://example.test/image?token=secret failed",
            java.net.SocketException("signature=signed connection reset"),
        )

        val description = failure.redactedDescription()

        assertThat(description).contains("IOException")
        assertThat(description).contains("SocketException")
        assertThat(description).contains("connection reset")
        assertThat(description).doesNotContain("secret")
        assertThat(description).doesNotContain("signed")
    }
}

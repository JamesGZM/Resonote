package com.resonote.core.network.protocol

import com.google.common.truth.Truth.assertThat
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test

class ApiProtocolInterceptorTest {
    @Test
    fun bypassRequestRemovesInternalMarkerAndDoesNotAddProtocolParameters() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("{}"))
        server.start()
        try {
            val client =
                OkHttpClient.Builder()
                    .addInterceptor(
                        ApiProtocolInterceptor(
                            Clock.fixed(Instant.ofEpochSecond(1_700_000_000), ZoneOffset.UTC),
                            ApiDeviceIdentity("guid", "123456"),
                            ApiRequestSigner(),
                        ),
                    ).build()
            val request =
                Request.Builder()
                    .url(server.url("/risk/verify"))
                    .header(ApiProtocolInterceptor.BYPASS_PROTOCOL_HEADER, "true")
                    .build()

            client.newCall(request).execute().use { response -> assertThat(response.isSuccessful).isTrue() }
            val recorded = server.takeRequest()

            assertThat(recorded.getHeader(ApiProtocolInterceptor.BYPASS_PROTOCOL_HEADER)).isNull()
            assertThat(recorded.requestUrl?.queryParameter("signature")).isNull()
            assertThat(recorded.requestUrl?.queryParameter("mid")).isNull()
        } finally {
            server.shutdown()
        }
    }
}

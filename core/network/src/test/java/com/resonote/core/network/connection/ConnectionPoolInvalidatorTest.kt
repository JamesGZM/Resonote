package com.resonote.core.network.connection

import com.google.common.truth.Truth.assertThat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test

class ConnectionPoolInvalidatorTest {
    @Test
    fun evictAllClosesRegisteredIdleConnections() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("ok"))
            val client = OkHttpClient()
            client.newCall(Request.Builder().url(server.url("/")).build()).execute().close()
            assertThat(client.connectionPool.idleConnectionCount()).isEqualTo(1)

            ConnectionPoolInvalidator().apply {
                register(client.connectionPool)
                evictAll()
            }

            assertThat(client.connectionPool.idleConnectionCount()).isEqualTo(0)
        }
    }
}

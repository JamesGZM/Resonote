package com.resonote.core.network.di

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NetworkModuleTest {
    @Test
    fun apiClientRetriesRecoverableConnectionFailures() {
        val client = apiHttpClientBuilder().build()

        assertThat(client.retryOnConnectionFailure).isTrue()
    }
}

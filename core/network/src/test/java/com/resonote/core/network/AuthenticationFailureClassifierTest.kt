package com.resonote.core.network

import com.google.common.truth.Truth.assertThat
import com.resonote.core.network.session.ApiAuthenticationGateReason
import org.junit.Test

class AuthenticationFailureClassifierTest {
    @Test
    fun documentedSearchCodeIsRequestScopedLoginRequired() {
        val failure = AuthenticationFailureClassifier.requestAuthenticationFailure("API-SEARCH-001", "152")

        assertThat(failure).isNotNull()
        assertThat(failure?.reason).isEqualTo(ApiAuthenticationGateReason.LoginRequired)
        assertThat(failure?.serviceCode).isEqualTo("152")
    }

    @Test
    fun unverifiedAccountEndpointCodesAreNotClassifiedAsAuthenticationFailures() {
        val endpointIds = listOf(
            "API-USER-007",
            "API-USER-008",
            "API-PLAYLIST-001",
            "API-PLAYLIST-009",
            "API-PLAYLIST-010",
            "API-CLOUD-001",
            "API-CLOUD-003",
            "API-YOUTH-008",
            "API-YOUTH-009",
        )

        endpointIds.forEach { endpointId ->
            listOf("152", "20010", "20017").forEach { serviceCode ->
                assertThat(AuthenticationFailureClassifier.capturesServiceFailure(endpointId, serviceCode)).isFalse()
            }
        }
    }

    @Test
    fun accountCodesDoNotLeakIntoPublicLyricsEndpoint() {
        listOf("152", "20010", "20017").forEach { serviceCode ->
            assertThat(AuthenticationFailureClassifier.capturesServiceFailure("API-LYRICS-001", serviceCode)).isFalse()
        }
    }

    @Test
    fun inferredPlaybackAndProfileCodesAreNotGlobalAuthenticationFailures() {
        val endpointIds = listOf("API-SONG-007", "API-SONG-011", "API-USER-003", "API-USER-013")

        endpointIds.forEach { endpointId ->
            listOf("152", "20010", "20017", "20018").forEach { serviceCode ->
                assertThat(AuthenticationFailureClassifier.capturesServiceFailure(endpointId, serviceCode)).isFalse()
            }
        }
    }
}

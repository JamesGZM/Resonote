package com.resonote.core.network

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AuthenticationFailureClassifierTest {
    @Test
    fun documentedSearchCodeIsCapturedByTheExplicitPolicy() {
        assertThat(AuthenticationFailureClassifier.capturesServiceFailure(setOf("152"), "152")).isTrue()
    }

    @Test
    fun requestsWithoutDocumentedCodesDoNotClassifyServiceFailuresAsAuthentication() {
        listOf("152", "20010", "20017", "20018").forEach { serviceCode ->
            assertThat(AuthenticationFailureClassifier.capturesServiceFailure(emptySet(), serviceCode)).isFalse()
        }
    }

    @Test
    fun unrelatedCodesAreNotCapturedBySearchPolicy() {
        listOf("20010", "20017", "20018").forEach { serviceCode ->
            assertThat(AuthenticationFailureClassifier.capturesServiceFailure(setOf("152"), serviceCode)).isFalse()
        }
    }
}

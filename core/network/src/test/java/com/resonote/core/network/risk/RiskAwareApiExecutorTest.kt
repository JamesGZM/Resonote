package com.resonote.core.network.risk

import com.google.common.truth.Truth.assertThat
import com.resonote.core.network.ApiRiskException
import com.resonote.core.network.retrofit.ApiRawResponse
import java.util.Optional
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Test

class RiskAwareApiExecutorTest {
    private val detector = ApiRiskChallengeDetector()

    @Test
    fun verifiedChallengeRetriesExactlyOnce() = runTest {
        val calls = AtomicInteger()
        val verifier = ApiRiskVerifier { ApiRiskVerificationResult.Verified }
        val executor = RiskAwareApiExecutor(detector, Optional.of(verifier))

        val result = executor.execute {
            if (calls.incrementAndGet() == 1) riskResponse() else successResponse()
        }

        assertThat(result.body["status"].toString()).isEqualTo("1")
        assertThat(calls.get()).isEqualTo(2)
    }

    @Test
    fun repeatedChallengeStopsAfterOneRetry() = runTest {
        val calls = AtomicInteger()
        val executor = RiskAwareApiExecutor(detector, Optional.of(ApiRiskVerifier { ApiRiskVerificationResult.Verified }))

        val exception = expectRiskException {
            executor.execute {
                calls.incrementAndGet()
                riskResponse()
            }
        }

        assertThat(exception.reason).isEqualTo(ApiRiskException.Reason.RepeatedChallenge)
        assertThat(calls.get()).isEqualTo(2)
    }

    @Test
    fun cancelledAndUnavailableDoNotRetry() = runTest {
        for ((result, expected) in
            listOf(
                ApiRiskVerificationResult.Cancelled to ApiRiskException.Reason.Cancelled,
                ApiRiskVerificationResult.Unavailable to ApiRiskException.Reason.VerificationUnavailable,
            )
        ) {
            val calls = AtomicInteger()
            val executor = RiskAwareApiExecutor(detector, Optional.of(ApiRiskVerifier { result }))
            val exception = expectRiskException {
                executor.execute {
                    calls.incrementAndGet()
                    riskResponse()
                }
            }
            assertThat(exception.reason).isEqualTo(expected)
            assertThat(calls.get()).isEqualTo(1)
        }
    }

    @Test
    fun concurrentChallengesSerializeVerification() = runTest {
        var active = 0
        var maxActive = 0
        val verifier =
            ApiRiskVerifier {
                active += 1
                maxActive = maxOf(maxActive, active)
                delay(10)
                active -= 1
                ApiRiskVerificationResult.Verified
            }
        val executor = RiskAwareApiExecutor(detector, Optional.of(verifier))

        List(3) {
            async {
                var attempt = 0
                executor.execute { if (attempt++ == 0) riskResponse() else successResponse() }
            }
        }.awaitAll()

        assertThat(maxActive).isEqualTo(1)
    }

    @Test
    fun bypassNeverInvokesVerifier() = runTest {
        var verified = false
        val executor =
            RiskAwareApiExecutor(
                detector,
                Optional.of(ApiRiskVerifier { verified = true; ApiRiskVerificationResult.Verified }),
            )

        expectRiskException {
            executor.execute(ApiRiskHandling.Bypass) { riskResponse() }
        }
        assertThat(verified).isFalse()
    }

    @Test
    fun coroutineCancellationPropagatesWithoutRetry() = runTest {
        val calls = AtomicInteger()
        val executor = RiskAwareApiExecutor(detector, Optional.empty())
        val job =
            launch {
                executor.execute {
                    calls.incrementAndGet()
                    delay(Long.MAX_VALUE)
                    successResponse()
                }
            }

        job.cancel()
        try {
            job.join()
        } catch (_: CancellationException) {
            // Cancellation is the expected terminal state.
        }

        assertThat(job.isCancelled).isTrue()
        assertThat(calls.get()).isAtMost(1)
    }

    private fun riskResponse() =
        ApiRawResponse(
            200,
            emptyMap(),
            Json.parseToJsonElement("""{"status":0,"error_code":20028,"ssaCode":"event"}""").jsonObject,
        )

    private fun successResponse() =
        ApiRawResponse(200, emptyMap(), Json.parseToJsonElement("""{"status":1}""").jsonObject)

    private suspend fun expectRiskException(block: suspend () -> Unit): ApiRiskException =
        try {
            block()
            throw AssertionError("Expected ApiRiskException")
        } catch (exception: ApiRiskException) {
            exception
        }
}

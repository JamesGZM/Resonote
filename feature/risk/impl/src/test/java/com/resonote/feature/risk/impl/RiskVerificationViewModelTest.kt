package com.resonote.feature.risk.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.RiskVerificationRepository
import com.resonote.core.model.RiskChallengeHandle
import com.resonote.core.model.RiskVerificationMethod
import com.resonote.core.model.RiskVerificationMethodResult
import com.resonote.core.model.RiskVerificationProof
import com.resonote.core.model.RiskVerificationSubmitResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RiskVerificationViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsTencentMethodAndSubmitsSdkProof() = runTest(dispatcher) {
        val repository = FakeRiskRepository(RiskVerificationMethod.Tencent("fixture-app"))
        val viewModel = RiskVerificationViewModel(repository)
        val challenge = RiskChallengeHandle("fixture-handle")

        viewModel.load(challenge)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isEqualTo(RiskVerificationUiState.Tencent("fixture-app"))

        viewModel.submitTencent("ticket", "random", "fixture-app")
        advanceUntilIdle()

        assertThat(repository.proofs).containsExactly(
            RiskVerificationProof.Tencent("ticket", "random", "fixture-app"),
        )
    }

    @Test
    fun smsInputIsNormalizedBeforeSubmission() = runTest(dispatcher) {
        val repository = FakeRiskRepository(RiskVerificationMethod.Sms)
        val viewModel = RiskVerificationViewModel(repository)

        viewModel.load(RiskChallengeHandle("fixture-handle"))
        advanceUntilIdle()
        viewModel.updateSmsCode("24a6 810")
        viewModel.submitSms()
        advanceUntilIdle()

        assertThat(repository.proofs).containsExactly(RiskVerificationProof.Sms("246810"))
    }

    private class FakeRiskRepository(private val method: RiskVerificationMethod) : RiskVerificationRepository {
        val proofs = mutableListOf<RiskVerificationProof>()

        override suspend fun methodFor(challenge: RiskChallengeHandle) = RiskVerificationMethodResult.Available(method)

        override suspend fun submit(
            challenge: RiskChallengeHandle,
            proof: RiskVerificationProof,
        ): RiskVerificationSubmitResult {
            proofs += proof
            return RiskVerificationSubmitResult.Verified
        }
    }
}

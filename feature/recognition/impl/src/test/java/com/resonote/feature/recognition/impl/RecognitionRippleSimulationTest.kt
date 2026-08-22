package com.resonote.feature.recognition.impl

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RecognitionRippleSimulationTest {
    @Test
    fun silenceDoesNotCreateSyntheticRipples() {
        val simulation = RecognitionRippleSimulation()

        val frame = simulation.advance(seconds = 1f, amplitude = 0.02f)

        assertThat(frame.envelope).isEqualTo(0f)
        assertThat(frame.driverDisplacement).isEqualTo(0f)
        assertThat(frame.driverMotion).isEqualTo(0f)
        assertThat(frame.pulses).isEmpty()
    }

    @Test
    fun audibleInputCreatesTravellingPulsesWithSmoothAttack() {
        val simulation = RecognitionRippleSimulation()

        val firstFrame = simulation.step(1f / 60f, inputAmplitude = 0.7f)
        val laterFrame = simulation.advance(seconds = 0.5f, amplitude = 0.7f)

        assertThat(firstFrame.envelope).isGreaterThan(0f)
        assertThat(firstFrame.envelope).isLessThan(0.7f)
        assertThat(firstFrame.driverDisplacement).isGreaterThan(0f)
        assertThat(firstFrame.driverMotion).isGreaterThan(0f)
        assertThat(laterFrame.envelope).isGreaterThan(firstFrame.envelope)
        assertThat(laterFrame.pulses).isNotEmpty()
        assertThat(laterFrame.pulses.any { it.ageSeconds > 0f }).isTrue()
    }

    @Test
    fun silenceStopsInjectionButExistingWaterRipplesDecayNaturally() {
        val simulation = RecognitionRippleSimulation()
        simulation.advance(seconds = 0.7f, amplitude = 0.8f)

        val releaseFrame = simulation.advance(seconds = 0.25f, amplitude = 0f)

        assertThat(releaseFrame.envelope).isGreaterThan(0f)
        assertThat(releaseFrame.pulses).isNotEmpty()

        val settledFrame = simulation.advance(seconds = 2f, amplitude = 0f)
        assertThat(settledFrame.envelope).isLessThan(0.01f)
        assertThat(settledFrame.driverDisplacement).isWithin(0.001f).of(0f)
        assertThat(settledFrame.driverMotion).isLessThan(0.01f)
        assertThat(settledFrame.pulses).isEmpty()
    }

    @Test
    fun envelopeIsStableAcrossDifferentRenderCadences() {
        val thirtyFps = RecognitionRippleSimulation()
        val sixtyFps = RecognitionRippleSimulation()

        var thirtyFpsFrame = RecognitionRippleFrame()
        repeat(18) { thirtyFpsFrame = thirtyFps.step(1f / 30f, 0.68f) }
        val sixtyFpsFrame = sixtyFps.advance(seconds = 0.6f, amplitude = 0.68f)

        assertThat(thirtyFpsFrame.envelope).isWithin(0.015f).of(sixtyFpsFrame.envelope)
    }

    private fun RecognitionRippleSimulation.advance(seconds: Float, amplitude: Float): RecognitionRippleFrame {
        var frame = RecognitionRippleFrame()
        repeat((seconds * 60).toInt()) {
            frame = step(1f / 60f, amplitude)
        }
        return frame
    }
}

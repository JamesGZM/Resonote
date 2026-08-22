package com.resonote.feature.recognition.impl

import kotlin.math.abs
import kotlin.math.exp

internal data class RecognitionRipplePulse(val ageSeconds: Float, val energy: Float)

internal data class RecognitionRippleFrame(
    val envelope: Float = 0f,
    val driverDisplacement: Float = 0f,
    val driverMotion: Float = 0f,
    val pulses: List<RecognitionRipplePulse> = emptyList(),
)

/** Converts microphone energy into independently travelling, smoothly decaying ripple pulses. */
internal class RecognitionRippleSimulation {
    private var envelope = 0f
    private var emissionElapsedSeconds = 0.32f
    private var previousEnvelope = 0f
    private var previousInput = 0f
    private var driverDisplacement = 0f
    private var driverVelocity = 0f
    private val pulses = mutableListOf<RecognitionRipplePulse>()

    fun reset() {
        envelope = 0f
        emissionElapsedSeconds = 0.32f
        previousEnvelope = 0f
        previousInput = 0f
        driverDisplacement = 0f
        driverVelocity = 0f
        pulses.clear()
    }

    fun step(deltaSeconds: Float, inputAmplitude: Float): RecognitionRippleFrame {
        val delta = deltaSeconds.coerceIn(0f, MAX_FRAME_DELTA_SECONDS)
        val input = inputAmplitude.visualEnergy()
        val timeConstant = if (input > envelope) ATTACK_SECONDS else RELEASE_SECONDS
        val response = 1f - exp((-delta / timeConstant).toDouble()).toFloat()
        envelope += (input - envelope) * response

        val transient = (envelope - previousEnvelope).coerceAtLeast(0f)
        val inputTransient = (input - previousInput).coerceAtLeast(0f)
        driverVelocity += transient * DRIVER_ENVELOPE_IMPULSE + inputTransient * DRIVER_INPUT_IMPULSE
        val driverAcceleration =
            -DRIVER_STIFFNESS * driverDisplacement - DRIVER_DAMPING * driverVelocity
        driverVelocity += driverAcceleration * delta
        driverDisplacement = (driverDisplacement + driverVelocity * delta)
            .coerceIn(MIN_DRIVER_DISPLACEMENT, MAX_DRIVER_DISPLACEMENT)

        pulses.replaceAll { pulse -> pulse.copy(ageSeconds = pulse.ageSeconds + delta) }
        pulses.removeAll { pulse -> pulse.ageSeconds >= RIPPLE_LIFETIME_SECONDS }

        emissionElapsedSeconds += delta
        val modulation = abs(input - previousInput)
        val emissionInterval = lerp(MAX_EMISSION_INTERVAL_SECONDS, MIN_EMISSION_INTERVAL_SECONDS, envelope)
        val transientReady = transient >= TRANSIENT_THRESHOLD && emissionElapsedSeconds >= MIN_TRANSIENT_GAP_SECONDS
        val modulationReady = modulation >= MODULATION_THRESHOLD && emissionElapsedSeconds >= emissionInterval
        if (input > MIN_EMISSION_ENERGY && (transientReady || modulationReady)) {
            pulses += RecognitionRipplePulse(
                ageSeconds = 0f,
                energy = (
                    0.22f + envelope * 0.68f + transient * 0.55f +
                        abs(driverDisplacement) * 0.8f
                    ).coerceIn(0f, 1f),
            )
            emissionElapsedSeconds = 0f
        }
        previousEnvelope = envelope
        previousInput = input

        return RecognitionRippleFrame(
            envelope = envelope.coerceIn(0f, 1f),
            driverDisplacement = driverDisplacement,
            driverMotion = (
                abs(driverVelocity) * DRIVER_VELOCITY_VISUAL_SCALE +
                    abs(driverDisplacement) * DRIVER_DISPLACEMENT_VISUAL_SCALE
                ).coerceIn(0f, 1f),
            pulses = pulses.toList(),
        )
    }

    private fun Float.visualEnergy(): Float {
        val normalized = coerceIn(0f, 1f)
        return if (normalized <= NOISE_FLOOR) {
            0f
        } else {
            ((normalized - NOISE_FLOOR) / (1f - NOISE_FLOOR)).coerceIn(0f, 1f)
        }
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float =
        start + (end - start) * fraction.coerceIn(0f, 1f)

    internal companion object {
        const val RIPPLE_LIFETIME_SECONDS = 1.72f
        private const val ATTACK_SECONDS = 0.075f
        private const val RELEASE_SECONDS = 0.34f
        private const val NOISE_FLOOR = 0.055f
        private const val MIN_EMISSION_ENERGY = 0.025f
        private const val MIN_EMISSION_INTERVAL_SECONDS = 0.19f
        private const val MAX_EMISSION_INTERVAL_SECONDS = 0.43f
        private const val MIN_TRANSIENT_GAP_SECONDS = 0.09f
        private const val TRANSIENT_THRESHOLD = 0.085f
        private const val MODULATION_THRESHOLD = 0.012f
        private const val MAX_FRAME_DELTA_SECONDS = 0.05f
        private const val DRIVER_STIFFNESS = 210f
        private const val DRIVER_DAMPING = 17f
        private const val DRIVER_ENVELOPE_IMPULSE = 3.2f
        private const val DRIVER_INPUT_IMPULSE = 2.1f
        private const val MIN_DRIVER_DISPLACEMENT = -0.16f
        private const val MAX_DRIVER_DISPLACEMENT = 0.28f
        private const val DRIVER_VELOCITY_VISUAL_SCALE = 0.22f
        private const val DRIVER_DISPLACEMENT_VISUAL_SCALE = 2.4f
    }
}

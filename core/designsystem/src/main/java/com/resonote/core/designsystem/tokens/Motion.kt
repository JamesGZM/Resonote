package com.resonote.core.designsystem.tokens

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable

@Immutable
class ResonoteMotionScheme internal constructor(
    private val reducedMotion: Boolean,
) {
    internal companion object {
        val Standard = ResonoteMotionScheme(reducedMotion = false)
        val Reduced = ResonoteMotionScheme(reducedMotion = true)
    }

    fun <T> instant(): FiniteAnimationSpec<T> = snap()

    fun <T> effectsFast(): FiniteAnimationSpec<T> = effects(dampingRatio = 1f, stiffness = 3_800f)

    fun <T> effectsDefault(): FiniteAnimationSpec<T> = effects(dampingRatio = 1f, stiffness = 1_600f)

    fun <T> effectsSlow(): FiniteAnimationSpec<T> = effects(dampingRatio = 1f, stiffness = 800f)

    fun <T> spatialFast(): FiniteAnimationSpec<T> = effects(dampingRatio = 0.9f, stiffness = 1_400f)

    fun <T> spatialDefault(): FiniteAnimationSpec<T> = effects(dampingRatio = 0.9f, stiffness = 700f)

    fun <T> spatialSlow(): FiniteAnimationSpec<T> = effects(dampingRatio = 0.9f, stiffness = 300f)

    private fun <T> effects(dampingRatio: Float, stiffness: Float): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else spring(dampingRatio = dampingRatio, stiffness = stiffness)
}

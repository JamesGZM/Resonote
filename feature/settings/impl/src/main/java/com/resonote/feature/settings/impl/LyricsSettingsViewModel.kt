package com.resonote.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.LyricsPreferencesRepository
import com.resonote.core.model.DesktopLyricsControlsTimeout
import com.resonote.core.model.DesktopLyricsDefaults
import com.resonote.core.model.LyricsPreferences
import com.resonote.core.playback.DesktopLyricsController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class LyricsSettingsViewModel @Inject constructor(
    private val repository: LyricsPreferencesRepository,
    private val desktopLyricsController: DesktopLyricsController,
) : ViewModel() {
    constructor(repository: LyricsPreferencesRepository) : this(repository, NoOpDesktopLyricsController)

    private val updateMutex = Mutex()

    val preferences: StateFlow<LyricsPreferences> = repository.preferences.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        LyricsPreferences(),
    )

    fun update(value: LyricsPreferences) {
        persist({ value }) { if (value.desktopLyricsEnabled) desktopLyricsController.refresh() }
    }

    fun setTranslationEnabled(enabled: Boolean) = updateSupplementalText {
        copy(translationEnabled = enabled)
    }

    fun setTransliterationEnabled(enabled: Boolean) = updateSupplementalText {
        copy(transliterationEnabled = enabled)
    }

    fun setDesktopLyricsEnabled(enabled: Boolean) = persist({ copy(desktopLyricsEnabled = enabled) }) {
        if (enabled) desktopLyricsController.show() else desktopLyricsController.hide()
    }

    fun setDesktopLyricsShadowColor(value: Int) = persist(
        { copy(desktopLyricsShadowColorArgb = value or 0xFF000000.toInt()) },
        desktopLyricsController::refresh,
    )

    fun setDesktopLyricsSurfaceOpacity(value: Int) = persist(
        { copy(desktopLyricsSurfaceOpacity = value.coerceIn(0, 100)) },
        desktopLyricsController::refresh,
    )

    fun setDesktopLyricsBackgroundColor(value: Int) = persist(
        { copy(desktopLyricsBackgroundColorArgb = value or 0xFF000000.toInt()) },
        desktopLyricsController::refresh,
    )

    fun setDesktopLyricsForegroundColor(value: Int) = persist(
        { copy(desktopLyricsForegroundColorArgb = value or 0xFF000000.toInt()) },
        desktopLyricsController::refresh,
    )

    fun setDesktopLyricsOutlineColor(value: Int) = persist(
        { copy(desktopLyricsOutlineColorArgb = value or 0xFF000000.toInt()) },
        desktopLyricsController::refresh,
    )

    fun setDesktopLyricsShadowOffsetX(value: Float) = persist(
        { copy(desktopLyricsShadowOffsetXDp = value.coerceIn(-8f, 8f)) },
        desktopLyricsController::refresh,
    )

    fun setDesktopLyricsShadowOffsetY(value: Float) = persist(
        { copy(desktopLyricsShadowOffsetYDp = value.coerceIn(-8f, 8f)) },
        desktopLyricsController::refresh,
    )

    fun setDesktopLyricsShadowBlurRadius(value: Float) = persist(
        { copy(desktopLyricsShadowBlurRadiusDp = value.coerceIn(0f, 12f)) },
        desktopLyricsController::refresh,
    )

    fun setDesktopLyricsWidthPercent(value: Int) = persist(
        { copy(desktopLyricsWidthPercent = value.coerceIn(40, 100)) },
        desktopLyricsController::refresh,
    )

    fun setDesktopLyricsFontSizeSp(value: Int) = persist(
        { copy(desktopLyricsFontSizeSp = value.coerceIn(16, 40)) },
        desktopLyricsController::refresh,
    )

    fun setDesktopLyricsOutlineWidth(value: Float) = persist(
        { copy(desktopLyricsOutlineWidthDp = value.coerceIn(0f, 4f)) },
        desktopLyricsController::refresh,
    )

    fun setDesktopLyricsControlsTimeout(value: DesktopLyricsControlsTimeout) = persist(
        { copy(desktopLyricsControlsTimeout = value) },
        desktopLyricsController::refresh,
    )

    fun resetDesktopLyricsBackgroundColor() = persist(
        { copy(desktopLyricsBackgroundColorArgb = DesktopLyricsDefaults.BACKGROUND_COLOR_ARGB) },
        desktopLyricsController::refresh,
    )

    fun resetDesktopLyricsSurfaceOpacity() = persist(
        { copy(desktopLyricsSurfaceOpacity = DesktopLyricsDefaults.SURFACE_OPACITY) },
        desktopLyricsController::refresh,
    )

    fun resetDesktopLyricsForegroundColor() = persist(
        { copy(desktopLyricsForegroundColorArgb = DesktopLyricsDefaults.FOREGROUND_COLOR_ARGB) },
        desktopLyricsController::refresh,
    )

    fun resetDesktopLyricsWidth() = persist(
        { copy(desktopLyricsWidthPercent = DesktopLyricsDefaults.WIDTH_PERCENT) },
        desktopLyricsController::refresh,
    )

    fun resetDesktopLyricsFontSize() = persist(
        { copy(desktopLyricsFontSizeSp = DesktopLyricsDefaults.FONT_SIZE_SP) },
        desktopLyricsController::refresh,
    )

    fun resetDesktopLyricsOutline() = persist(
        {
            copy(
                desktopLyricsOutlineColorArgb = DesktopLyricsDefaults.OUTLINE_COLOR_ARGB,
                desktopLyricsOutlineWidthDp = DesktopLyricsDefaults.OUTLINE_WIDTH_DP,
            )
        },
        desktopLyricsController::refresh,
    )

    fun resetDesktopLyricsShadow() = persist(
        {
            copy(
                desktopLyricsShadowColorArgb = DesktopLyricsDefaults.SHADOW_COLOR_ARGB,
                desktopLyricsShadowOffsetXDp = DesktopLyricsDefaults.SHADOW_OFFSET_X_DP,
                desktopLyricsShadowOffsetYDp = DesktopLyricsDefaults.SHADOW_OFFSET_Y_DP,
                desktopLyricsShadowBlurRadiusDp = DesktopLyricsDefaults.SHADOW_BLUR_RADIUS_DP,
            )
        },
        desktopLyricsController::refresh,
    )

    fun resetDesktopLyricsControlsTimeout() = persist(
        { copy(desktopLyricsControlsTimeout = DesktopLyricsControlsTimeout.FiveSeconds) },
        desktopLyricsController::refresh,
    )

    fun setDesktopLyricsLocked(locked: Boolean) = persist(
        { copy(desktopLyricsLocked = locked) },
        desktopLyricsController::refresh,
    )

    fun resetDesktopLyricsPosition() = persist(
        { copy(desktopLyricsPosition = null) },
        desktopLyricsController::resetPosition,
    )

    private fun updateSupplementalText(transform: LyricsPreferences.() -> LyricsPreferences) {
        persist(transform, desktopLyricsController::refresh)
    }

    private fun persist(transform: LyricsPreferences.() -> LyricsPreferences, afterSave: () -> Unit = {}) {
        viewModelScope.launch {
            updateMutex.withLock {
                repository.setPreferences(repository.preferences.first().transform())
                afterSave()
            }
        }
    }
}

private object NoOpDesktopLyricsController : DesktopLyricsController {
    override fun show() = Unit
    override fun hide() = Unit
    override fun refresh() = Unit
    override fun resetPosition() = Unit
}

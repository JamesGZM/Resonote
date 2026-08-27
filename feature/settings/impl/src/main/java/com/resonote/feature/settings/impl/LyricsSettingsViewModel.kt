package com.resonote.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.LyricsPreferencesRepository
import com.resonote.core.model.DesktopLyricsControlsTimeout
import com.resonote.core.model.DesktopLyricsDisplayMode
import com.resonote.core.model.LyricsFontSize
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

    fun setDesktopLyricsDisplayMode(value: DesktopLyricsDisplayMode) = persist(
        { copy(desktopLyricsDisplayMode = value) },
        desktopLyricsController::refresh,
    )

    fun setDesktopLyricsFontSize(value: LyricsFontSize) = persist(
        { copy(desktopLyricsFontSize = value) },
        desktopLyricsController::refresh,
    )

    fun setDesktopLyricsSurfaceOpacity(value: Int) = persist(
        { copy(desktopLyricsSurfaceOpacity = value.coerceIn(0, 100)) },
        desktopLyricsController::refresh,
    )

    fun setDesktopLyricsControlsTimeout(value: DesktopLyricsControlsTimeout) = persist(
        { copy(desktopLyricsControlsTimeout = value) },
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
    override fun restoreIfEnabled() = Unit
}

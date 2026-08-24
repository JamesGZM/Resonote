package com.resonote.feature.recognition.impl

import com.resonote.core.model.ContentFailure
import com.resonote.core.model.RecognitionMatch

internal sealed interface RecognitionUiState {
    data object Idle : RecognitionUiState
    data class PermissionDenied(val permanently: Boolean) : RecognitionUiState
    data class Recording(
        val elapsedMillis: Long,
        val amplitude: Float = 0f,
        val waveform: List<Float> = emptyList(),
        val rippleHistory: List<Float> = emptyList(),
    ) : RecognitionUiState {
        init {
            require(amplitude in 0f..1f) { "amplitude must be normalized" }
            require(waveform.all { it in -1f..1f }) { "waveform samples must be normalized" }
            require(rippleHistory.all { it in 0f..1f }) { "ripple history must be normalized" }
        }
    }
    data object Recognizing : RecognitionUiState
    data class Matches(val items: List<RecognitionMatch>) : RecognitionUiState {
        init {
            require(items.isNotEmpty()) { "items must not be empty" }
        }
    }
    data object NoMatch : RecognitionUiState
    data object TooShort : RecognitionUiState
    data object CaptureFailed : RecognitionUiState
    data class Failed(val failure: ContentFailure) : RecognitionUiState
}

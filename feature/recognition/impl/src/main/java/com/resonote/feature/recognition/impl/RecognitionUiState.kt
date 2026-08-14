package com.resonote.feature.recognition.impl

import com.resonote.core.model.ContentFailure
import com.resonote.core.model.RecognitionMatch

internal sealed interface RecognitionUiState {
    data object Idle : RecognitionUiState
    data class PermissionDenied(val permanently: Boolean) : RecognitionUiState
    data class Recording(val elapsedMillis: Long) : RecognitionUiState
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

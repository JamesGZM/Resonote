package com.resonote.feature.recognition.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.RecognitionRepository
import com.resonote.core.model.CollectionLoadResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecognitionViewModel @Inject internal constructor(
    private val repository: RecognitionRepository,
    private val recorder: RecognitionRecorder,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<RecognitionUiState>(RecognitionUiState.Idle)
    internal val uiState: StateFlow<RecognitionUiState> = mutableUiState.asStateFlow()

    private var captureJob: Job? = null

    fun startRecording() {
        if (captureJob?.isActive == true) return
        captureJob = viewModelScope.launch {
            mutableUiState.value = RecognitionUiState.Recording(elapsedMillis = 0)
            when (val capture = recorder.capture(onProgress = ::updateProgress)) {
                is RecognitionCaptureResult.Captured -> recognize(capture.pcm)
                RecognitionCaptureResult.Failed -> mutableUiState.value = RecognitionUiState.CaptureFailed
            }
        }
    }

    fun stopRecording() {
        if (mutableUiState.value is RecognitionUiState.Recording) recorder.stop()
    }

    fun showPermissionDenied(permanently: Boolean) {
        cancelCapture()
        mutableUiState.value = RecognitionUiState.PermissionDenied(permanently)
    }

    fun permissionAvailable() {
        if (mutableUiState.value is RecognitionUiState.PermissionDenied) reset()
    }

    fun reset() {
        cancelCapture()
        mutableUiState.value = RecognitionUiState.Idle
    }

    fun cancelCapture() {
        recorder.cancel()
        captureJob?.cancel()
        captureJob = null
        if (mutableUiState.value is RecognitionUiState.Recording ||
            mutableUiState.value is RecognitionUiState.Recognizing
        ) {
            mutableUiState.value = RecognitionUiState.Idle
        }
    }

    override fun onCleared() {
        recorder.cancel()
        super.onCleared()
    }

    private fun updateProgress(elapsedMillis: Long) {
        if (mutableUiState.value is RecognitionUiState.Recording) {
            mutableUiState.value = RecognitionUiState.Recording(
                elapsedMillis.coerceIn(0, RECOGNITION_MAX_DURATION_MILLIS),
            )
        }
    }

    private suspend fun recognize(pcm: ByteArray) {
        val minimumBytes =
            (RECOGNITION_SAMPLE_RATE * Short.SIZE_BYTES * RECOGNITION_MIN_DURATION_MILLIS / 1_000L)
                .toInt()
        if (pcm.size < minimumBytes) {
            pcm.fill(0)
            mutableUiState.value = RecognitionUiState.TooShort
            return
        }
        mutableUiState.value = RecognitionUiState.Recognizing
        try {
            mutableUiState.value = when (val result = repository.recognizeAudio(pcm)) {
                is CollectionLoadResult.Available ->
                    result.value
                        .takeIf { it.isNotEmpty() }
                        ?.let { RecognitionUiState.Matches(result.value) }
                        ?: RecognitionUiState.NoMatch
                is CollectionLoadResult.Failed -> RecognitionUiState.Failed(result.failure)
            }
        } finally {
            pcm.fill(0)
        }
    }
}

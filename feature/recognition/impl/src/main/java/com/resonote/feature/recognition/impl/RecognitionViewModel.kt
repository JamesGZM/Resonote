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
    private var smoothedWaveform = emptyList<Float>()

    fun startRecording() {
        if (captureJob?.isActive == true) return
        smoothedWaveform = emptyList()
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
        smoothedWaveform = emptyList()
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

    private fun updateProgress(elapsedMillis: Long, amplitude: Float, waveform: List<Float>) {
        val current = mutableUiState.value
        if (current is RecognitionUiState.Recording) {
            val normalizedAmplitude = amplitude.coerceIn(0f, 1f)
            smoothedWaveform = waveform.stabilizedAgainst(smoothedWaveform)
            mutableUiState.value = RecognitionUiState.Recording(
                elapsedMillis = elapsedMillis.coerceIn(0, RECOGNITION_MAX_DURATION_MILLIS),
                amplitude = normalizedAmplitude,
                waveform = smoothedWaveform,
                rippleHistory = (current.rippleHistory + normalizedAmplitude).takeLast(RIPPLE_HISTORY_SIZE),
            )
        }
    }

    private fun List<Float>.stabilizedAgainst(previous: List<Float>): List<Float> {
        val polarityAligned = chunked(2).flatMap { extrema ->
            if (extrema.size == 2) {
                listOf(extrema.max(), extrema.min())
            } else {
                extrema
            }
        }.map { it.coerceIn(-1f, 1f) }
        if (polarityAligned.size != previous.size) return polarityAligned
        return polarityAligned.mapIndexed { index, sample ->
            val oldSample = previous[index]
            val response = if (kotlin.math.abs(sample) > kotlin.math.abs(oldSample)) 0.42f else 0.28f
            (oldSample + (sample - oldSample) * response).coerceIn(-1f, 1f)
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

    private companion object {
        const val RIPPLE_HISTORY_SIZE = 24
    }
}

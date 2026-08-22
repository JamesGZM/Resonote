package com.resonote.feature.recognition.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.RecognitionRepository
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.RecognitionMatch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecognitionViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun stopRecordingSubmitsPcmPublishesMatchesAndClearsBuffer() = runTest(dispatcher) {
        val pcm = ByteArray(RECOGNITION_SAMPLE_RATE * Short.SIZE_BYTES * 2) { 7 }
        val recorder = BlockingRecorder(pcm)
        val repository = FakeRecognitionRepository(CollectionLoadResult.Available(listOf(match("signal", 0.91))))
        val viewModel = RecognitionViewModel(repository, recorder)

        viewModel.startRecording()
        runCurrent()

        assertThat(viewModel.uiState.value).isEqualTo(
            RecognitionUiState.Recording(
                elapsedMillis = 2_500,
                amplitude = 0.64f,
                waveform = listOf(0.42f, -0.18f, 0.31f, -0.64f),
                rippleHistory = listOf(0.2f, 0.64f),
            ),
        )
        viewModel.stopRecording()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            RecognitionUiState.Matches(listOf(match("signal", 0.91))),
        )
        assertThat(recorder.stopCount).isEqualTo(1)
        assertThat(repository.receivedPcm).hasLength(pcm.size)
        assertThat(repository.receivedPcm!!.all { it == 0.toByte() }).isTrue()

        viewModel.reset()

        assertThat(viewModel.uiState.value).isEqualTo(RecognitionUiState.Idle)
    }

    @Test
    fun recordingShorterThanOneSecondNeverCallsApi() = runTest(dispatcher) {
        val recorder = ImmediateRecorder(
            RecognitionCaptureResult.Captured(ByteArray(RECOGNITION_SAMPLE_RATE * Short.SIZE_BYTES - 2) { 3 }),
        )
        val repository = FakeRecognitionRepository(CollectionLoadResult.Available(emptyList()))
        val viewModel = RecognitionViewModel(repository, recorder)

        viewModel.startRecording()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(RecognitionUiState.TooShort)
        assertThat(repository.callCount).isEqualTo(0)
    }

    @Test
    fun cancellingActiveCaptureStopsRecorderAndReturnsToIdle() = runTest(dispatcher) {
        val recorder = BlockingRecorder(ByteArray(RECOGNITION_SAMPLE_RATE * Short.SIZE_BYTES * 2))
        val viewModel = RecognitionViewModel(
            FakeRecognitionRepository(CollectionLoadResult.Available(emptyList())),
            recorder,
        )

        viewModel.startRecording()
        runCurrent()
        viewModel.cancelCapture()
        runCurrent()

        assertThat(viewModel.uiState.value).isEqualTo(RecognitionUiState.Idle)
        assertThat(recorder.cancelCount).isEqualTo(1)
    }

    @Test
    fun consecutivePcmFramesKeepPolarityAlignedAndSmoothLargeChanges() = runTest(dispatcher) {
        val recorder = SmoothingRecorder()
        val viewModel = RecognitionViewModel(
            FakeRecognitionRepository(CollectionLoadResult.Available(emptyList())),
            recorder,
        )

        viewModel.startRecording()
        runCurrent()

        val recording = viewModel.uiState.value as RecognitionUiState.Recording
        assertThat(recording.waveform[0]).isWithin(0.0001f).of(0.72f)
        assertThat(recording.waveform[1]).isWithin(0.0001f).of(-0.86f)
        assertThat(recording.waveform[2]).isWithin(0.0001f).of(0.72f)
        assertThat(recording.waveform[3]).isWithin(0.0001f).of(-0.86f)
        viewModel.cancelCapture()
        runCurrent()
    }

    @Test
    fun emptySuccessfulResponseIsNoMatchRatherThanFailure() = runTest(dispatcher) {
        val viewModel = RecognitionViewModel(
            FakeRecognitionRepository(CollectionLoadResult.Available(emptyList())),
            validRecorder(),
        )

        viewModel.startRecording()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(RecognitionUiState.NoMatch)
    }

    @Test
    fun networkFailureRemainsTypedAndRequiresNewRecording() = runTest(dispatcher) {
        val viewModel = RecognitionViewModel(
            FakeRecognitionRepository(CollectionLoadResult.Failed(ContentFailure.Network)),
            validRecorder(),
        )

        viewModel.startRecording()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(RecognitionUiState.Failed(ContentFailure.Network))
        viewModel.reset()
        assertThat(viewModel.uiState.value).isEqualTo(RecognitionUiState.Idle)
    }

    @Test
    fun captureFailureAndPermanentPermissionDenialStayDistinct() = runTest(dispatcher) {
        val recorder = ImmediateRecorder(RecognitionCaptureResult.Failed)
        val viewModel = RecognitionViewModel(
            FakeRecognitionRepository(CollectionLoadResult.Available(emptyList())),
            recorder,
        )

        viewModel.startRecording()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isEqualTo(RecognitionUiState.CaptureFailed)

        viewModel.showPermissionDenied(permanently = true)
        assertThat(viewModel.uiState.value).isEqualTo(RecognitionUiState.PermissionDenied(permanently = true))
        viewModel.permissionAvailable()
        assertThat(viewModel.uiState.value).isEqualTo(RecognitionUiState.Idle)
        assertThat(recorder.cancelCount).isAtLeast(1)
    }

    private class BlockingRecorder(private val pcm: ByteArray) : RecognitionRecorder {
        private val stopped = CompletableDeferred<Unit>()
        var stopCount = 0
        var cancelCount = 0

        override suspend fun capture(
            maxDurationMillis: Long,
            onProgress: (Long, Float, List<Float>) -> Unit,
        ): RecognitionCaptureResult {
            onProgress(1_200, 0.2f, listOf(0.1f, -0.1f))
            onProgress(2_500, 0.64f, TEST_WAVEFORM)
            stopped.await()
            return RecognitionCaptureResult.Captured(pcm)
        }

        override fun stop() {
            stopCount += 1
            stopped.complete(Unit)
        }

        override fun cancel() {
            cancelCount += 1
            stopped.complete(Unit)
        }
    }

    private class ImmediateRecorder(private val result: RecognitionCaptureResult) : RecognitionRecorder {
        var cancelCount = 0

        override suspend fun capture(maxDurationMillis: Long, onProgress: (Long, Float, List<Float>) -> Unit) = result
        override fun stop() = Unit
        override fun cancel() {
            cancelCount += 1
        }
    }

    private class SmoothingRecorder : RecognitionRecorder {
        private val stopped = CompletableDeferred<Unit>()

        override suspend fun capture(
            maxDurationMillis: Long,
            onProgress: (Long, Float, List<Float>) -> Unit,
        ): RecognitionCaptureResult {
            onProgress(100, 0.3f, listOf(1f, -1f, 1f, -1f))
            onProgress(200, 0.5f, listOf(-0.5f, 0f, -0.5f, 0f))
            stopped.await()
            return RecognitionCaptureResult.Captured(ByteArray(0))
        }

        override fun stop() {
            stopped.complete(Unit)
        }

        override fun cancel() {
            stopped.complete(Unit)
        }
    }

    private class FakeRecognitionRepository(private val result: CollectionLoadResult<List<RecognitionMatch>>) :
        RecognitionRepository {
        var callCount = 0
        var receivedPcm: ByteArray? = null

        override suspend fun recognizeAudio(pcm: ByteArray): CollectionLoadResult<List<RecognitionMatch>> {
            callCount += 1
            receivedPcm = pcm
            return result
        }
    }

    private companion object {
        val TEST_WAVEFORM = listOf(-0.18f, 0.42f, -0.64f, 0.31f)

        fun validRecorder() = ImmediateRecorder(
            RecognitionCaptureResult.Captured(ByteArray(RECOGNITION_SAMPLE_RATE * Short.SIZE_BYTES) { 5 }),
        )

        fun match(id: String, confidence: Double) = RecognitionMatch(
            confidence = confidence,
            song = OnlineSong(
                hash = id,
                title = "潮汐信号",
                artist = "林澈",
                coverUrl = null,
                albumId = null,
                albumAudioId = null,
                durationMillis = 265_000,
                quality = AudioQuality.HighQuality,
                vip = false,
            ),
        )
    }
}

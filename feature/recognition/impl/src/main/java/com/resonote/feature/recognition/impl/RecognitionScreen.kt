@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.recognition.impl

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.RecognitionMatch

@Composable
fun RecognitionRoute(
    onBack: () -> Unit,
    onCaptureStarted: () -> Unit,
    onPlay: (OnlineSong) -> Unit,
    onSearch: (RecognitionMatch) -> Unit,
    onAddToPlaylist: (OnlineSong) -> Unit,
    viewModel: RecognitionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity()
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionGranted by remember {
        mutableStateOf(context.hasMicrophonePermission())
    }

    val startCapture = {
        onCaptureStarted()
        viewModel.startRecording()
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionGranted = granted
        if (granted) {
            viewModel.permissionAvailable()
            startCapture()
        } else {
            val permanently = activity?.let {
                !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.RECORD_AUDIO)
            } ?: false
            viewModel.showPermissionDenied(permanently)
        }
    }
    val requestOrStart = {
        if (permissionGranted) startCapture() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = context.hasMicrophonePermission()
                if (permissionGranted) viewModel.permissionAvailable()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.reset()
        }
    }

    RecognitionScreen(
        state = state,
        onBack = onBack,
        onStart = requestOrStart,
        onStop = viewModel::stopRecording,
        onRetry = {
            viewModel.reset()
            requestOrStart()
        },
        onOpenSettings = { context.openAppSettings() },
        onPlay = onPlay,
        onSearch = onSearch,
        onAddToPlaylist = onAddToPlaylist,
        onReset = viewModel::reset,
    )
}

@Composable
internal fun RecognitionScreen(
    state: RecognitionUiState,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onPlay: (OnlineSong) -> Unit,
    onSearch: (RecognitionMatch) -> Unit,
    onAddToPlaylist: (OnlineSong) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    animateListeningField: Boolean = true,
) {
    Box(
        modifier = modifier.fillMaxSize().testTag("recognition-screen"),
    ) {
        when (state) {
            is RecognitionUiState.Matches -> {
                RecognitionRecordBackground(
                    amplitude = 0f,
                    active = false,
                    showListeningField = false,
                    modifier = Modifier.fillMaxSize(),
                )
                RecognitionTitleBlock()
                MatchResults(
                    matches = state.items,
                    onPlay = onPlay,
                    onSearch = onSearch,
                    onAddToPlaylist = onAddToPlaylist,
                    onReset = onReset,
                    modifier = Modifier.fillMaxSize().padding(top = 184.dp),
                )
            }
            else -> RecognitionStatusContent(
                state = state,
                onStart = onStart,
                onStop = onStop,
                onRetry = onRetry,
                onOpenSettings = onOpenSettings,
                animateListeningField = animateListeningField,
                modifier = Modifier.fillMaxSize(),
            )
        }
        RecognitionImmersiveToolbar(onBack)
    }
}

@Composable
private fun RecognitionImmersiveToolbar(onBack: () -> Unit) {
    val surface = MaterialTheme.colorScheme.surface
    ResonoteTopAppBar(
        title = {},
        modifier = Modifier.fillMaxWidth().zIndex(1f).testTag("recognition-toolbar"),
        navigationIcon = {
            Surface(
                modifier = Modifier.padding(start = 4.dp).size(40.dp),
                shape = CircleShape,
                color = surface.copy(alpha = 0.7f),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.feature_recognition_impl_back))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
    )
}

@Composable
private fun RecognitionStatusContent(
    state: RecognitionUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    animateListeningField: Boolean,
    modifier: Modifier = Modifier,
) {
    val amplitude = when (state) {
        is RecognitionUiState.Recording -> state.amplitude
        else -> 0f
    }
    val title = when (state) {
        RecognitionUiState.Idle -> stringResource(R.string.feature_recognition_impl_title)
        is RecognitionUiState.PermissionDenied -> stringResource(R.string.feature_recognition_impl_permission_title)
        is RecognitionUiState.Recording -> stringResource(R.string.feature_recognition_impl_recording_title)
        RecognitionUiState.Recognizing -> stringResource(R.string.feature_recognition_impl_recognizing_title)
        RecognitionUiState.NoMatch -> stringResource(R.string.feature_recognition_impl_no_match_title)
        RecognitionUiState.TooShort -> stringResource(R.string.feature_recognition_impl_too_short_title)
        RecognitionUiState.CaptureFailed -> stringResource(R.string.feature_recognition_impl_capture_error_title)
        is RecognitionUiState.Failed -> stringResource(R.string.feature_recognition_impl_error_title)
        is RecognitionUiState.Matches -> ""
    }
    val supportingText = when (state) {
        RecognitionUiState.Idle -> stringResource(R.string.feature_recognition_impl_idle_title)
        is RecognitionUiState.PermissionDenied -> stringResource(
            if (state.permanently) {
                R.string.feature_recognition_impl_permission_permanent_body
            } else {
                R.string.feature_recognition_impl_permission_body
            },
        )
        is RecognitionUiState.Recording -> stringResource(
            R.string.feature_recognition_impl_recording_time,
            state.elapsedMillis / 1_000L,
            RECOGNITION_MAX_DURATION_MILLIS / 1_000L,
        )
        RecognitionUiState.Recognizing -> stringResource(R.string.feature_recognition_impl_recognizing_body)
        RecognitionUiState.NoMatch -> stringResource(R.string.feature_recognition_impl_no_match_body)
        RecognitionUiState.TooShort -> stringResource(R.string.feature_recognition_impl_too_short_body)
        RecognitionUiState.CaptureFailed -> stringResource(R.string.feature_recognition_impl_capture_error_body)
        is RecognitionUiState.Failed -> state.failure.message()
        is RecognitionUiState.Matches -> ""
    }
    Box(modifier = modifier) {
        RecognitionRecordBackground(
            amplitude = amplitude,
            active = state is RecognitionUiState.Recording,
            animate = animateListeningField,
            modifier = Modifier.fillMaxSize(),
        )
        RecognitionTitleBlock(title = title, supportingText = supportingText)
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 76.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            when (state) {
                RecognitionUiState.Idle -> IdleContent(onStart)
                is RecognitionUiState.PermissionDenied -> PermissionContent(state.permanently, onStart, onOpenSettings)
                is RecognitionUiState.Recording -> RecordingContent(onStop)
                RecognitionUiState.Recognizing -> RecognizingContent()
                RecognitionUiState.NoMatch -> ResultMessage(onRetry)
                RecognitionUiState.TooShort -> ResultMessage(onRetry)
                RecognitionUiState.CaptureFailed -> ResultMessage(onRetry)
                is RecognitionUiState.Failed -> ResultMessage(onRetry)
                is RecognitionUiState.Matches -> Unit
            }
        }
    }
}

@Composable
private fun RecognitionTitleBlock(
    title: String = stringResource(R.string.feature_recognition_impl_title),
    supportingText: String? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 112.dp, end = 24.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
        )
        supportingText?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 10.dp).widthIn(max = 320.dp),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

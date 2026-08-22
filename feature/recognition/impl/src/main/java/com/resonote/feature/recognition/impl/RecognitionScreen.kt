@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.recognition.impl

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.ResonoteQualityBadge
import com.resonote.core.designsystem.component.ResonoteRemoteArtwork
import com.resonote.core.designsystem.component.ResonoteTonalIconButton
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.designsystem.component.ResonoteVipBadge
import com.resonote.core.designsystem.component.compactBadgeLabel
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.RecognitionMatch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

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

@Composable
private fun IdleContent(onStart: () -> Unit) {
    RecognitionAction(
        label = stringResource(R.string.feature_recognition_impl_start),
        onClick = onStart,
        icon = Icons.Rounded.Mic,
    )
}

@Composable
private fun PermissionContent(permanently: Boolean, onStart: () -> Unit, onOpenSettings: () -> Unit) {
    RecognitionAction(
        label = stringResource(
            if (permanently) {
                R.string.feature_recognition_impl_open_settings
            } else {
                R.string.feature_recognition_impl_request_permission
            },
        ),
        onClick = if (permanently) onOpenSettings else onStart,
        icon = if (permanently) Icons.Rounded.Settings else Icons.Rounded.Mic,
    )
}

@Composable
private fun RecordingContent(onStop: () -> Unit) {
    RecognitionAction(
        label = stringResource(R.string.feature_recognition_impl_stop),
        onClick = onStop,
        icon = Icons.Rounded.Pause,
    )
}

@Composable
private fun RecognizingContent() {
    LinearProgressIndicator(
        modifier = Modifier.width(180.dp),
        color = MaterialTheme.colorScheme.onPrimary,
        trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f),
    )
}

@Composable
private fun RecognitionRecordBackground(
    amplitude: Float,
    active: Boolean,
    showListeningField: Boolean = true,
    animate: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val latestAmplitude by rememberUpdatedState(amplitude)
    val simulation = remember { RecognitionRippleSimulation() }
    val rippleFrame = remember { mutableStateOf(RecognitionRippleFrame()) }

    LaunchedEffect(active, animate) {
        simulation.reset()
        rippleFrame.value = RecognitionRippleFrame()
        if (!active) return@LaunchedEffect

        if (!animate) {
            repeat(54) { frameIndex ->
                val previewEnvelope = 0.38f + abs(sin(frameIndex * 0.53f)) * 0.62f
                rippleFrame.value = simulation.step(
                    deltaSeconds = 1f / 60f,
                    inputAmplitude = latestAmplitude * previewEnvelope,
                )
            }
            return@LaunchedEffect
        }

        var previousFrameNanos = withFrameNanos { it }
        while (currentCoroutineContext().isActive) {
            withFrameNanos { frameNanos ->
                val deltaSeconds = (frameNanos - previousFrameNanos) / 1_000_000_000f
                rippleFrame.value = simulation.step(
                    deltaSeconds = deltaSeconds,
                    inputAmplitude = latestAmplitude,
                )
                previousFrameNanos = frameNanos
            }
        }
    }

    Canvas(modifier.testTag("recognition-ripples")) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(primary, primary.copy(alpha = 0.9f), primaryContainer),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiaryContainer.copy(alpha = 0.72f), Color.Transparent),
                center = Offset(size.width * 0.88f, size.height * 0.82f),
                radius = size.width * 0.9f,
            ),
            radius = size.width,
            center = Offset(size.width * 0.88f, size.height * 0.82f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryContainer.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(size.width * 0.88f, size.height * 0.1f),
                radius = size.width * 0.72f,
            ),
            radius = size.width * 0.72f,
            center = Offset(size.width * 0.88f, size.height * 0.1f),
        )

        if (!showListeningField) return@Canvas

        val frame = rippleFrame.value
        val center = Offset(size.width * 0.5f, size.height * 0.475f)
        val maximumRadius = min(size.width * 0.44f, size.height * 0.22f)
        val visualEnergy = frame.envelope.coerceIn(0f, 1f)
        val driverDisplacement = frame.driverDisplacement.coerceIn(-0.16f, 0.28f)
        val driverMotion = frame.driverMotion.coerceIn(0f, 1f)
        val baseOrbRadius = maximumRadius * 0.4f
        val orbRadius = (
            baseOrbRadius * (1f + visualEnergy * 0.025f + driverDisplacement * 0.48f)
            ).coerceIn(baseOrbRadius * 0.92f, baseOrbRadius * 1.15f)
        val innerWaveRadius = orbRadius * 1.06f
        val outerWaveRadius = maximumRadius * 1.02f

        repeat(7) { index ->
            val fraction = index / 6f
            drawCircle(
                color = onPrimary.copy(
                    alpha = 0.035f + fraction * 0.055f + visualEnergy * 0.025f,
                ),
                radius = maximumRadius * (0.52f + fraction * 0.46f),
                center = center,
                style = Stroke(width = (0.75f + fraction * 0.22f).dp.toPx()),
            )
        }

        frame.pulses.forEach { pulse ->
            val progress =
                (pulse.ageSeconds / RecognitionRippleSimulation.RIPPLE_LIFETIME_SECONDS).coerceIn(0f, 1f)
            val travel = 1f - (1f - progress).pow(1.18f)
            val radius = innerWaveRadius + (outerWaveRadius - innerWaveRadius) * travel
            val crest = sin(PI.toFloat() * progress).coerceAtLeast(0f).pow(0.72f)
            val fade = crest * (1f - progress * 0.48f)
            val alpha = (pulse.energy * fade).coerceIn(0f, 1f)
            if (alpha <= 0.002f) return@forEach

            val glowWidth = (4.5f + pulse.energy * 7f - progress * 2f).coerceAtLeast(2f).dp.toPx()
            drawCircle(
                color = tertiaryContainer.copy(alpha = alpha * 0.1f),
                radius = radius,
                center = center,
                style = Stroke(width = glowWidth),
            )
            drawCircle(
                color = onPrimary.copy(alpha = alpha * 0.82f),
                radius = radius,
                center = center,
                style = Stroke(
                    width = (1f + pulse.energy * 1.25f * (1f - progress * 0.35f)).dp.toPx(),
                    cap = StrokeCap.Round,
                ),
            )
            val echoRadius = (radius - maximumRadius * (0.02f + pulse.energy * 0.018f))
                .coerceAtLeast(innerWaveRadius)
            drawCircle(
                color = primaryContainer.copy(alpha = alpha * 0.26f),
                radius = echoRadius,
                center = center,
                style = Stroke(width = 0.8.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        val haloRadius = orbRadius * (1.52f + visualEnergy * 0.16f + driverMotion * 0.08f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    tertiaryContainer.copy(
                        alpha = 0.1f + visualEnergy * 0.22f + driverMotion * 0.12f,
                    ),
                    Color.Transparent,
                ),
                center = center + Offset(orbRadius * 0.24f, orbRadius * 0.28f),
                radius = haloRadius,
            ),
            radius = haloRadius,
            center = center,
        )
        if (driverMotion > 0.002f) {
            drawCircle(
                color = onPrimary.copy(alpha = driverMotion * 0.22f),
                radius = orbRadius * (1.07f + driverMotion * 0.06f),
                center = center,
                style = Stroke(
                    width = (0.8f + driverMotion * 1.4f).dp.toPx(),
                    cap = StrokeCap.Round,
                ),
            )
        }
        if (visualEnergy > 0f || driverMotion > 0f) {
            drawCircle(
                color = primaryContainer.copy(
                    alpha = 0.06f + visualEnergy * 0.09f + driverMotion * 0.12f,
                ),
                radius = orbRadius * (1.06f + driverMotion * 0.08f),
                center = center,
                style = Stroke(width = (0.8f + visualEnergy * 0.6f + driverMotion).dp.toPx()),
            )
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    tertiaryContainer.copy(alpha = 0.92f),
                    primaryContainer.copy(alpha = 0.78f),
                    primary.copy(alpha = 0.24f),
                ),
                center = center + Offset(orbRadius * 0.34f, orbRadius * 0.34f),
                radius = orbRadius * 1.35f,
            ),
            radius = orbRadius,
            center = center,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    onPrimary.copy(alpha = 0.1f + driverMotion * 0.08f),
                    Color.Transparent,
                ),
                center = center - Offset(orbRadius * 0.2f, orbRadius * 0.22f),
                radius = orbRadius * 0.82f,
            ),
            radius = orbRadius * 0.82f,
            center = center,
        )
        drawCircle(
            color = onPrimary.copy(alpha = 0.12f + driverMotion * 0.1f),
            radius = orbRadius,
            center = center,
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

@Composable
private fun RecognitionAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(56.dp).testTag("recognition-action"),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            }
        }
        Text(
            label,
            modifier = Modifier.padding(top = 10.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun ResultMessage(onRetry: () -> Unit) {
    RecognitionAction(
        label = stringResource(R.string.feature_recognition_impl_retry),
        onClick = onRetry,
        icon = Icons.Rounded.Refresh,
    )
}

@Composable
private fun MatchResults(
    matches: List<RecognitionMatch>,
    onPlay: (OnlineSong) -> Unit,
    onSearch: (RecognitionMatch) -> Unit,
    onAddToPlaylist: (OnlineSong) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { matches.size })
    BoxWithConstraints(modifier = modifier) {
        val pagerHeight = (maxHeight - 92.dp).coerceAtMost(440.dp).coerceAtLeast(320.dp)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.height(pagerHeight).fillMaxWidth().testTag("recognition-match-pager"),
                contentPadding = PaddingValues(horizontal = 56.dp),
                pageSpacing = 12.dp,
                verticalAlignment = Alignment.CenterVertically,
                key = { page -> "${matches[page].song.hash}-$page" },
            ) { page ->
                val offset = abs((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                    .coerceIn(0f, 1f)
                RecognitionMatchCard(
                    match = matches[page],
                    onPlay = { onPlay(matches[page].song) },
                    onAddToPlaylist = { onAddToPlaylist(matches[page].song) },
                    onSearch = { onSearch(matches[page]) },
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 4.dp)
                        .graphicsLayer {
                            val scale = 1f - offset * 0.045f
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f - offset * 0.18f
                        }
                        .testTag("recognition-match-card-$page"),
                )
            }
            MatchPagerIndicator(
                pageCount = matches.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.feature_recognition_impl_unsatisfied),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(
                    onClick = onReset,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(stringResource(R.string.feature_recognition_impl_restart))
                }
            }
        }
    }
}

@Composable
private fun RecognitionMatchCard(
    match: RecognitionMatch,
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box {
            ResonoteRemoteArtwork(
                model = match.song.coverUrl,
                contentDescription = stringResource(
                    R.string.feature_recognition_impl_artwork,
                    match.song.title,
                ),
                modifier = Modifier.fillMaxWidth().aspectRatio(1.06f),
            )
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.62f),
                contentColor = Color.White,
            ) {
                Text(
                    text = stringResource(
                        R.string.feature_recognition_impl_confidence,
                        (match.confidence * 100).roundToInt(),
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = match.song.title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
            )
            Text(
                text = match.song.artist ?: stringResource(R.string.feature_recognition_impl_unknown_artist),
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = match.song.durationMillis.durationLabel(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
                match.song.quality.compactBadgeLabel()?.let { ResonoteQualityBadge(it) }
                if (match.song.vip) ResonoteVipBadge()
            }
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RecognitionPlayButton(
                    onClick = onPlay,
                    modifier = Modifier.weight(1f),
                )
                ResonoteTonalIconButton(
                    label = stringResource(R.string.feature_recognition_impl_add_playlist),
                    onClick = onAddToPlaylist,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                }
                ResonoteTonalIconButton(
                    label = stringResource(R.string.feature_recognition_impl_search),
                    onClick = onSearch,
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun RecognitionPlayButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.feature_recognition_impl_play),
            maxLines = 1,
            softWrap = false,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun MatchPagerIndicator(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
    ) {
        repeat(pageCount) { index ->
            Box(
                Modifier
                    .width(if (currentPage == index) 22.dp else 6.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (currentPage == index) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.28f)
                        },
                    ),
            )
        }
    }
}

private fun Long.durationLabel(): String {
    val totalSeconds = coerceAtLeast(0) / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
private fun ContentFailure.message(): String = stringResource(
    when (this) {
        ContentFailure.AuthenticationRequired -> R.string.feature_recognition_impl_error_auth
        ContentFailure.Network -> R.string.feature_recognition_impl_error_network
        is ContentFailure.RiskVerificationRequired,
        ContentFailure.RiskBlocked,
        -> R.string.feature_recognition_impl_error_risk
        ContentFailure.Protocol, ContentFailure.ServiceRejected -> R.string.feature_recognition_impl_error_generic
    },
)

private fun Context.hasMicrophonePermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

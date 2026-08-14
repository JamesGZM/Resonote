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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.resonote.core.designsystem.component.ResonoteButton
import com.resonote.core.designsystem.component.ResonoteOutlinedButton
import com.resonote.core.designsystem.tokens.ResonoteTokens
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.RecognitionMatch
import kotlin.math.roundToInt

@Composable
fun RecognitionRoute(
    onBack: () -> Unit,
    onCaptureStarted: () -> Unit,
    onPlay: (OnlineSong) -> Unit,
    onSearch: (RecognitionMatch) -> Unit,
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
            viewModel.cancelCapture()
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
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize().testTag("recognition-screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.feature_recognition_impl_back))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.feature_recognition_impl_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.feature_recognition_impl_subtitle),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Spacer(Modifier.width(48.dp))
            }
        },
    ) { padding ->
        when (state) {
            is RecognitionUiState.Matches -> MatchResults(
                matches = state.items,
                onPlay = onPlay,
                onSearch = onSearch,
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> RecognitionStatusContent(
                state = state,
                onStart = onStart,
                onStop = onStop,
                onRetry = onRetry,
                onOpenSettings = onOpenSettings,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        }
    }
}

@Composable
private fun RecognitionStatusContent(
    state: RecognitionUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (state) {
            RecognitionUiState.Idle -> IdleContent(onStart)
            is RecognitionUiState.PermissionDenied -> PermissionContent(state.permanently, onStart, onOpenSettings)
            is RecognitionUiState.Recording -> RecordingContent(state.elapsedMillis, onStop)
            RecognitionUiState.Recognizing -> RecognizingContent()
            RecognitionUiState.NoMatch -> ResultMessage(
                icon = Icons.Rounded.Search,
                title = stringResource(R.string.feature_recognition_impl_no_match_title),
                body = stringResource(R.string.feature_recognition_impl_no_match_body),
                onRetry = onRetry,
            )
            RecognitionUiState.TooShort -> ResultMessage(
                icon = Icons.Rounded.TimerOff,
                title = stringResource(R.string.feature_recognition_impl_too_short_title),
                body = stringResource(R.string.feature_recognition_impl_too_short_body),
                onRetry = onRetry,
            )
            RecognitionUiState.CaptureFailed -> ResultMessage(
                icon = Icons.Rounded.MicOff,
                title = stringResource(R.string.feature_recognition_impl_capture_error_title),
                body = stringResource(R.string.feature_recognition_impl_capture_error_body),
                onRetry = onRetry,
            )
            is RecognitionUiState.Failed -> ResultMessage(
                icon = Icons.Rounded.Refresh,
                title = stringResource(R.string.feature_recognition_impl_error_title),
                body = state.failure.message(),
                onRetry = onRetry,
            )
            is RecognitionUiState.Matches -> Unit
        }
    }
}

@Composable
private fun IdleContent(onStart: () -> Unit) {
    ListeningOrb(progress = 0f, icon = Icons.Rounded.Mic)
    Text(
        stringResource(R.string.feature_recognition_impl_idle_title),
        modifier = Modifier.padding(top = 30.dp),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    Text(
        stringResource(R.string.feature_recognition_impl_idle_body),
        modifier = Modifier.padding(top = 10.dp).widthIn(max = 440.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
    ResonoteButton(
        label = stringResource(R.string.feature_recognition_impl_start),
        onClick = onStart,
        leadingIcon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null) },
        modifier = Modifier.padding(top = 28.dp),
    )
    PrivacyNote()
}

@Composable
private fun PermissionContent(permanently: Boolean, onStart: () -> Unit, onOpenSettings: () -> Unit) {
    ListeningOrb(progress = 0f, icon = Icons.Rounded.MicOff)
    Text(
        stringResource(R.string.feature_recognition_impl_permission_title),
        modifier = Modifier.padding(top = 30.dp),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    Text(
        stringResource(
            if (permanently) {
                R.string.feature_recognition_impl_permission_permanent_body
            } else {
                R.string.feature_recognition_impl_permission_body
            },
        ),
        modifier = Modifier.padding(top = 10.dp).widthIn(max = 440.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    ResonoteButton(
        label = stringResource(
            if (permanently) {
                R.string.feature_recognition_impl_open_settings
            } else {
                R.string.feature_recognition_impl_request_permission
            },
        ),
        onClick = if (permanently) onOpenSettings else onStart,
        leadingIcon = {
            Icon(if (permanently) Icons.Rounded.Settings else Icons.Rounded.Mic, contentDescription = null)
        },
        modifier = Modifier.padding(top = 26.dp),
    )
}

@Composable
private fun RecordingContent(elapsedMillis: Long, onStop: () -> Unit) {
    val progress = (elapsedMillis.toFloat() / RECOGNITION_MAX_DURATION_MILLIS).coerceIn(0f, 1f)
    ListeningOrb(progress = progress, icon = Icons.Rounded.GraphicEq)
    Text(
        stringResource(R.string.feature_recognition_impl_recording_title),
        modifier = Modifier.padding(top = 30.dp),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
    )
    Text(
        stringResource(
            R.string.feature_recognition_impl_recording_time,
            elapsedMillis / 1_000L,
            RECOGNITION_MAX_DURATION_MILLIS / 1_000L,
        ),
        modifier = Modifier.padding(top = 8.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.padding(top = 18.dp).fillMaxWidth().widthIn(max = 360.dp),
    )
    ResonoteButton(
        label = stringResource(R.string.feature_recognition_impl_stop),
        onClick = onStop,
        leadingIcon = { Icon(Icons.Rounded.Pause, contentDescription = null) },
        modifier = Modifier.padding(top = 28.dp),
    )
    Text(
        stringResource(R.string.feature_recognition_impl_auto_stop),
        modifier = Modifier.padding(top = 12.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun RecognizingContent() {
    ListeningOrb(progress = 1f, icon = Icons.Rounded.MusicNote, busy = true)
    Text(
        stringResource(R.string.feature_recognition_impl_recognizing_title),
        modifier = Modifier.padding(top = 30.dp),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
    )
    Text(
        stringResource(R.string.feature_recognition_impl_recognizing_body),
        modifier = Modifier.padding(top = 10.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ListeningOrb(
    progress: Float,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    busy: Boolean = false,
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(232.dp)) {
        Surface(
            modifier = Modifier.size(228.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
        ) {}
        Surface(
            modifier = Modifier.size(174.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.11f),
        ) {}
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(132.dp),
            strokeWidth = 5.dp,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
        )
        Surface(
            modifier = Modifier.size(104.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = ResonoteTokens.elevation.level3.maximumShadow,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(42.dp))
            }
        }
        if (busy) CircularProgressIndicator(modifier = Modifier.size(228.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun PrivacyNote() {
    Surface(
        modifier = Modifier.padding(top = 32.dp).widthIn(max = 480.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Text(
            stringResource(R.string.feature_recognition_impl_privacy),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ResultMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    onRetry: () -> Unit,
) {
    ListeningOrb(progress = 0f, icon = icon)
    Text(
        title,
        modifier = Modifier.padding(top = 30.dp),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    Text(
        body,
        modifier = Modifier.padding(top = 10.dp).widthIn(max = 440.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    ResonoteButton(
        label = stringResource(R.string.feature_recognition_impl_retry),
        onClick = onRetry,
        leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
        modifier = Modifier.padding(top = 26.dp),
    )
}

@Composable
private fun MatchResults(
    matches: List<RecognitionMatch>,
    onPlay: (OnlineSong) -> Unit,
    onSearch: (RecognitionMatch) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                stringResource(R.string.feature_recognition_impl_matches_label),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.feature_recognition_impl_matches_title),
                modifier = Modifier.padding(top = 5.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.feature_recognition_impl_matches_body),
                modifier = Modifier.padding(top = 7.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        itemsIndexed(matches, key = { index, match -> "${match.song.hash}-$index" }) { index, match ->
            MatchCard(index, match, onPlay, onSearch)
        }
        item {
            ResonoteOutlinedButton(
                label = stringResource(R.string.feature_recognition_impl_retry),
                onClick = onRetry,
                leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun MatchCard(
    index: Int,
    match: RecognitionMatch,
    onPlay: (OnlineSong) -> Unit,
    onSearch: (RecognitionMatch) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (index == 0) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        ListItem(
            headlineContent = {
                Text(match.song.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
            },
            supportingContent = {
                Text(
                    match.song.artist ?: stringResource(R.string.feature_recognition_impl_unknown_artist),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            overlineContent = {
                Text(
                    stringResource(R.string.feature_recognition_impl_confidence, (match.confidence * 100).roundToInt()),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            },
            leadingContent = { MatchArtwork(match.song) },
            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
        ) {
            ResonoteOutlinedButton(
                label = stringResource(R.string.feature_recognition_impl_search_action),
                onClick = { onSearch(match) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            )
            ResonoteButton(
                label = stringResource(R.string.feature_recognition_impl_play_action),
                onClick = { onPlay(match.song) },
                leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
            )
        }
    }
}

@Composable
private fun MatchArtwork(song: OnlineSong) {
    Box(
        modifier = Modifier.size(68.dp)
            .background(
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)),
                MaterialTheme.shapes.large,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (!song.coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = song.coverUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
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

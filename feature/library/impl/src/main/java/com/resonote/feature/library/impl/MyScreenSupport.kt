package com.resonote.feature.library.impl

import android.icu.text.CompactDecimalFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.resonote.core.designsystem.component.ResonoteButton
import com.resonote.core.designsystem.component.ResonoteShimmer
import com.resonote.core.designsystem.component.ResonoteTextButton
import com.resonote.core.designsystem.component.ResonoteTextField
import com.resonote.core.designsystem.component.rememberResonoteShimmer
import com.resonote.core.designsystem.component.resonoteShimmer
import com.resonote.core.designsystem.tokens.ResonoteTokens
import com.resonote.core.model.ContentFailure
import java.util.Locale

@Composable
internal fun MyScreenSkeleton(bottomContentPadding: Dp) {
    val shimmer = rememberResonoteShimmer("my-screen-skeleton")
    Column(
        Modifier.fillMaxWidth().padding(bottom = bottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ProfileSkeleton(shimmer)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(4) {
                Spacer(
                    Modifier.weight(1f).height(88.dp)
                        .resonoteShimmer(shimmer, MaterialTheme.shapes.large),
                )
            }
        }
        PlaylistSkeleton(shimmer)
    }
}

@Composable
internal fun ProfileSkeleton(shimmer: ResonoteShimmer = rememberResonoteShimmer("my-profile-skeleton")) {
    Spacer(
        Modifier.fillMaxWidth().height(244.dp)
            .resonoteShimmer(shimmer, MaterialTheme.shapes.extraLarge)
            .testTag("my-profile-loading"),
    )
}

@Composable
internal fun PlaylistSkeleton(shimmer: ResonoteShimmer = rememberResonoteShimmer("my-playlist-skeleton")) {
    Column(Modifier.fillMaxWidth().testTag("my-playlists-loading"), Arrangement.spacedBy(12.dp)) {
        Spacer(Modifier.fillMaxWidth().height(80.dp).resonoteShimmer(shimmer, MaterialTheme.shapes.extraLarge))
        Spacer(Modifier.width(132.dp).height(24.dp).resonoteShimmer(shimmer, MaterialTheme.shapes.small))
        repeat(3) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.size(52.dp).resonoteShimmer(shimmer, MaterialTheme.shapes.medium))
                Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Spacer(Modifier.width(156.dp).height(16.dp).resonoteShimmer(shimmer, MaterialTheme.shapes.small))
                    Spacer(Modifier.width(64.dp).height(12.dp).resonoteShimmer(shimmer, MaterialTheme.shapes.small))
                }
            }
        }
    }
}

@Composable
internal fun SectionFailure(
    title: String,
    failure: ContentFailure,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
        shadowElevation = ResonoteTokens.elevation.level2.maximumShadow,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CloudOff, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    failure.message(),
                    Modifier.padding(top = 3.dp),
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onRetry) { Text(stringResource(R.string.feature_library_impl_my_retry)) }
        }
    }
}

@Composable
internal fun PlaylistCreationDialog(
    name: String,
    state: PlaylistCreationUiState,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val submitting = state == PlaylistCreationUiState.Submitting
    val failure = (state as? PlaylistCreationUiState.Failed)?.failure
    Dialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !submitting,
            dismissOnClickOutside = !submitting,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .widthIn(max = 360.dp)
                .testTag("my-create-playlist-dialog"),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp,
            shadowElevation = ResonoteTokens.elevation.level3.maximumShadow,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null, Modifier.size(26.dp))
                    }
                }
                Text(
                    text = stringResource(R.string.feature_library_impl_create_playlist_title),
                    modifier = Modifier.padding(top = 18.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.feature_library_impl_create_playlist_body),
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                ResonoteTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = stringResource(R.string.feature_library_impl_create_playlist_name),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .testTag("my-create-playlist-name"),
                    enabled = !submitting,
                    errorMessage = failure?.message(),
                )
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ResonoteButton(
                        stringResource(R.string.feature_library_impl_create_playlist_confirm),
                        onConfirm,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = name.isNotBlank() && !submitting,
                        loading = submitting,
                        loadingLabel = stringResource(R.string.feature_library_impl_create_playlist_submitting),
                    )
                    ResonoteTextButton(
                        label = stringResource(R.string.feature_library_impl_create_playlist_cancel),
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !submitting,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ContentFailure.message() = stringResource(
    when (this) {
        ContentFailure.AuthenticationRequired -> R.string.feature_library_impl_my_error_auth
        ContentFailure.Network -> R.string.feature_library_impl_my_error_network
        ContentFailure.ServiceRejected -> R.string.feature_library_impl_my_error_service
        is ContentFailure.RiskVerificationRequired -> R.string.feature_library_impl_my_error_risk
        ContentFailure.RiskBlocked -> R.string.feature_library_impl_my_error_risk
        ContentFailure.Protocol -> R.string.feature_library_impl_my_error_protocol
    },
)

internal fun Long.compactNumber() = when {
    this >= 10_000 ->
        CompactDecimalFormat
            .getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT)
            .format(this)
    else -> toString()
}

@Composable
internal fun Long.listenTime() = when {
    this >= 60 -> stringResource(R.string.feature_library_impl_my_listen_time_hours, this / 60)
    this > 0 -> stringResource(R.string.feature_library_impl_my_listen_time_minutes, this)
    else -> "—"
}

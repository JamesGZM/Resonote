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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.resonote.core.designsystem.component.ResonoteQualityBadge
import com.resonote.core.designsystem.component.ResonoteRemoteArtwork
import com.resonote.core.designsystem.component.ResonoteTonalIconButton
import com.resonote.core.designsystem.component.ResonoteVipBadge
import com.resonote.core.designsystem.component.compactBadgeLabel
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.RecognitionMatch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun ResultMessage(onRetry: () -> Unit) {
    RecognitionAction(
        label = stringResource(R.string.feature_recognition_impl_retry),
        onClick = onRetry,
        icon = Icons.Rounded.Refresh,
    )
}

@Composable
internal fun MatchResults(
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
internal fun ContentFailure.message(): String = stringResource(
    when (this) {
        ContentFailure.AuthenticationRequired -> R.string.feature_recognition_impl_error_auth
        ContentFailure.Network -> R.string.feature_recognition_impl_error_network
        is ContentFailure.RiskVerificationRequired,
        ContentFailure.RiskBlocked,
        -> R.string.feature_recognition_impl_error_risk
        ContentFailure.Protocol, ContentFailure.ServiceRejected -> R.string.feature_recognition_impl_error_generic
    },
)

internal fun Context.hasMicrophonePermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

internal fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

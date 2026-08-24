@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.artist.impl

import android.icu.text.CompactDecimalFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteHeroKeys
import com.resonote.core.designsystem.component.ResonoteRemoteArtwork
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.designsystem.component.resonoteHeroElement
import com.resonote.core.model.AudioQuality
import java.util.Locale

@Composable
internal fun ArtistHeader(
    profile: ArtistProfile?,
    artworkUrl: String? = profile?.avatarUrl,
    follow: ArtistFollowUiState,
    onFollowClick: () -> Unit,
) {
    val name = profile?.name ?: stringResource(R.string.feature_artist_impl_artist_title_fallback)
    val portraitDescription = stringResource(R.string.feature_artist_impl_artist_portrait, name)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val topScrim = if (isDark) Color.Black.copy(alpha = 0.36f) else Color.White.copy(alpha = 0.28f)
    val textShadow = Shadow(
        color = Color.Black.copy(alpha = 0.76f),
        offset = Offset(0f, 1.5f),
        blurRadius = 3.5f,
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 340.dp)
            .semantics { contentDescription = portraitDescription },
    ) {
        ResonoteRemoteArtwork(
            model = artworkUrl,
            contentDescription = null,
            modifier = Modifier
                .resonoteHeroElement(profile?.id?.let(ResonoteHeroKeys::artist))
                .testTag("artist-hero")
                .matchParentSize(),
            fallback = {
                Box(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(top = 60.dp).size(96.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.62f),
                    )
                }
            },
        )
        Box(
            Modifier.fillMaxWidth().height(104.dp).background(
                Brush.verticalGradient(listOf(topScrim, Color.Transparent)),
            ),
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0.42f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.82f),
                ),
            ),
        )
        Column(
            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Spacer(Modifier.height(152.dp))
            Text(
                text = name,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall.copy(shadow = textShadow),
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                profile?.fansCount?.takeIf { it > 0 }?.let { fansCount ->
                    Text(
                        text = stringResource(
                            R.string.feature_artist_impl_artist_fans,
                            fansCount.compactCount(),
                        ),
                        color = Color.White.copy(alpha = 0.84f),
                        style = MaterialTheme.typography.bodyMedium.copy(shadow = textShadow),
                    )
                } ?: Spacer(Modifier.weight(1f))
                ArtistFollowButton(follow = follow, onClick = onFollowClick)
            }
            profile?.intro?.takeIf(String::isNotBlank)?.let { intro ->
                ArtistDescription(intro = intro, textShadow = textShadow)
            }
        }
    }
}

@Composable
private fun ArtistFollowButton(follow: ArtistFollowUiState, onClick: () -> Unit) {
    val available = follow as? ArtistFollowUiState.Available
    val isFollowed = available?.isFollowed == true
    val isBusy = follow is ArtistFollowUiState.Loading || available?.isUpdating == true
    val label = when {
        follow is ArtistFollowUiState.Error -> stringResource(R.string.feature_artist_impl_artist_follow_retry)
        isFollowed -> stringResource(R.string.feature_artist_impl_artist_followed)
        else -> stringResource(R.string.feature_artist_impl_artist_follow)
    }
    val containerColor = if (isFollowed) Color.White.copy(alpha = 0.18f) else Color.White
    val contentColor = if (isFollowed) Color.White else Color.Black.copy(alpha = 0.82f)
    Button(
        onClick = onClick,
        enabled = !isBusy,
        modifier = Modifier.height(34.dp).testTag("artist-follow"),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor,
        ),
        contentPadding = PaddingValues(horizontal = 14.dp),
    ) {
        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = contentColor,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ArtistDescription(intro: String, textShadow: Shadow) {
    var expanded by rememberSaveable(intro) { mutableStateOf(false) }
    val textStyle = MaterialTheme.typography.bodyMedium.copy(shadow = textShadow)
    val interactionSource = remember { MutableInteractionSource() }
    Text(
        text = intro,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("artist-description-toggle")
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { expanded = !expanded },
            )
            .padding(vertical = 12.dp),
        color = Color.White.copy(alpha = 0.84f),
        style = textStyle,
        maxLines = if (expanded) Int.MAX_VALUE else 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun ArtistImmersiveToolbar(title: String?, onBack: () -> Unit, listState: LazyListState) {
    val surface = MaterialTheme.colorScheme.surface
    val collapseProgress = rememberArtistCollapseProgress(listState)
    ResonoteTopAppBar(
        title = {
            if (title != null) {
                Text(
                    text = title,
                    modifier = Modifier
                        .graphicsLayer { alpha = collapseProgress.value }
                        .clearAndSetSemantics {
                            if (collapseProgress.value > 0f) text = AnnotatedString(title)
                        },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(color = surface, alpha = collapseProgress.value)
            }
            .testTag("artist-toolbar"),
        navigationIcon = {
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(40.dp)
                    .drawBehind {
                        drawCircle(
                            color = surface,
                            alpha = 0.7f * (1f - collapseProgress.value),
                        )
                    },
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        stringResource(R.string.feature_artist_impl_artist_back),
                    )
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
internal fun rememberArtistCollapseProgress(listState: LazyListState): State<Float> {
    val density = LocalDensity.current
    val startPx = with(density) { 220.dp.roundToPx() }
    val endPx = with(density) { 320.dp.roundToPx() }
    return remember(listState, startPx, endPx) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                ((listState.firstVisibleItemScrollOffset - startPx).toFloat() / (endPx - startPx))
                    .coerceIn(0f, 1f)
            }
        }
    }
}

internal fun Long.compactCount(): String = when {
    this >= 10_000 ->
        CompactDecimalFormat
            .getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT)
            .format(this)
    else -> toString()
}

internal fun Long.durationLabel(): String {
    val totalSeconds = coerceAtLeast(0) / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

internal fun AudioQuality.label(): String? = when (this) {
    AudioQuality.Standard -> null
    AudioQuality.HighQuality -> "HQ"
    AudioQuality.HighResolution -> "HI-RES"
    AudioQuality.Lossless -> "LOSSLESS"
}

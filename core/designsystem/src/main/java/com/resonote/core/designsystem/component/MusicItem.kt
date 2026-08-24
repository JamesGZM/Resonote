package com.resonote.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.R
import com.resonote.core.designsystem.tokens.ResonoteTokens

@Composable
fun ResonoteMusicItem(
    title: String,
    supportingText: String,
    duration: String,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    artworkState: ResonoteArtworkState = ResonoteArtworkState.LOADED,
    artworkUrl: String? = null,
    artwork: @Composable BoxScope.() -> Unit = { DefaultSongArtwork(title) },
    qualityLabel: String? = null,
    isVip: Boolean = false,
    isPlaying: Boolean = false,
    isSelected: Boolean = false,
    enabled: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val effectiveArtworkState = if (!artworkUrl.isNullOrBlank()) ResonoteArtworkState.LOADED else artworkState
    val hasTrailingAction = trailingAction != null || onMoreClick != null
    val colors = MaterialTheme.colorScheme
    val containerColor = when {
        isPlaying -> lerp(colors.surface, colors.primary, 0.08f)
        isSelected -> lerp(colors.surface, colors.primary, 0.06f)
        else -> Color.Transparent
    }
    val titleColor = if (isPlaying) colors.primary else colors.onSurface
    val statusSlotWidth = with(LocalDensity.current) {
        when {
            fontScale >= 1.75f -> 56.dp
            fontScale >= 1.25f -> 48.dp
            else -> 44.dp
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .clip(MaterialTheme.shapes.large)
            .background(containerColor)
            .semantics { selected = isSelected }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                start = 8.dp,
                top = 8.dp,
                end = if (hasTrailingAction) 0.dp else 8.dp,
                bottom = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingContent != null) {
            leadingContent()
            Spacer(Modifier.width(8.dp))
        }
        ResonoteArtwork(
            state = effectiveArtworkState,
            contentDescription = stringResource(R.string.core_designsystem_song_artwork, title),
            modifier = Modifier.size(64.dp),
            shape = ResonoteTokens.artworkShapes.standard,
            artwork = {
                if (artworkUrl.isNullOrBlank()) {
                    artwork()
                } else {
                    ResonoteRemoteArtwork(
                        model = artworkUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        fallback = artwork,
                    )
                }
                if (effectiveArtworkState == ResonoteArtworkState.LOADED) {
                    ResonoteArtworkBadge(
                        qualityLabel = qualityLabel,
                        isVip = isVip,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp),
                    )
                }
            },
        )
        Spacer(Modifier.width(12.dp))
        if (effectiveArtworkState == ResonoteArtworkState.LOADING) {
            LoadingMusicText(modifier = Modifier.weight(1f))
        } else {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    color = titleColor,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = supportingText,
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (hasTrailingAction) (-8).dp else 0.dp),
        ) {
            Box(
                modifier = Modifier.width(statusSlotWidth),
                contentAlignment = Alignment.Center,
            ) {
                if (isPlaying) {
                    Icon(
                        imageVector = Icons.Rounded.Equalizer,
                        contentDescription = stringResource(R.string.core_designsystem_playing),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        text = if (effectiveArtworkState == ResonoteArtworkState.LOADING) "--:--" else duration,
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
            }
            if (trailingAction != null) {
                trailingAction()
            } else if (onMoreClick != null) {
                ResonoteIconButton(
                    label = stringResource(R.string.core_designsystem_more_actions, title),
                    onClick = onMoreClick,
                    enabled = enabled,
                    icon = {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun ResonoteArtworkBadge(qualityLabel: String?, isVip: Boolean, modifier: Modifier = Modifier) {
    val label = listOfNotNull(
        qualityLabel?.toCompactQualityLabel(),
        "VIP".takeIf { isVip },
    ).joinToString(separator = " · ")
    if (label.isNotEmpty()) {
        Surface(
            modifier = modifier.testTag("resonote-artwork-badge"),
            color = Color.Black.copy(alpha = 0.62f),
            contentColor = Color.White,
            shape = RoundedCornerShape(4.dp),
        ) {
            val badgeFontSize = with(LocalDensity.current) { 8.dp.toSp() }
            val badgeLineHeight = with(LocalDensity.current) { 10.dp.toSp() }
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                style = MaterialTheme.typography.labelSmall,
                fontSize = badgeFontSize,
                lineHeight = badgeLineHeight,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

private fun String.toCompactQualityLabel(): String? = when (val normalized = trim().uppercase()) {
    "LOSSLESS", "SQ" -> "SQ"
    "HIGH QUALITY", "HIGH_QUALITY", "HQ" -> "HQ"
    "HI-RES", "HI RES", "HIRES", "HIGH RESOLUTION", "HIGH_RESOLUTION", "HR" -> "HR"
    else -> normalized.takeIf { it.length <= 3 }
}

@Composable
private fun LoadingMusicText(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .fillMaxWidth(0.56f)
                .height(12.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f)),
        )
        Box(
            Modifier
                .fillMaxWidth(0.78f)
                .height(10.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)),
        )
    }
}

package com.resonote.feature.home.impl

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteCompactFilledIconButton
import com.resonote.core.designsystem.component.ResonotePlainAction
import com.resonote.core.designsystem.tokens.ResonoteTokens

@Composable
internal fun RecommendationArea(
    onPlayRadio: () -> Unit,
    onOpenRankings: () -> Unit,
    onOpenFeaturedPlaylists: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space2),
    ) {
        RecommendationShortcut(
            title = stringResource(R.string.feature_home_impl_radio),
            supporting = stringResource(R.string.feature_home_impl_radio_supporting),
            iconRes = R.drawable.feature_home_impl_home_radio_waveform,
            iconWidth = 54.dp,
            iconHeight = 48.dp,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            actionLabel = stringResource(R.string.feature_home_impl_play_radio),
            onAction = onPlayRadio,
            modifier = Modifier.weight(1f),
        )
        RecommendationShortcut(
            title = stringResource(R.string.feature_home_impl_rankings),
            supporting = stringResource(R.string.feature_home_impl_popular_rankings),
            iconRes = R.drawable.feature_home_impl_home_ranking_bars,
            iconWidth = 64.dp,
            iconHeight = 43.dp,
            containerColor = ResonoteTokens.brandColors.harmonicViolet,
            contentColor = ResonoteTokens.brandColors.onHarmonicViolet,
            onClick = onOpenRankings,
            modifier = Modifier
                .weight(1f)
                .testTag("home-shortcut-rankings"),
        )
        RecommendationShortcut(
            title = stringResource(R.string.feature_home_impl_featured_playlists),
            supporting = stringResource(R.string.feature_home_impl_selected_for_you),
            iconRes = R.drawable.feature_home_impl_home_playlist_disc,
            iconWidth = 47.dp,
            iconHeight = 50.dp,
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
            onClick = onOpenFeaturedPlaylists,
            modifier = Modifier
                .weight(1f)
                .testTag("home-shortcut-featured-playlists"),
        )
    }
}

@Composable
private fun RecommendationShortcut(
    title: String,
    supporting: String,
    iconRes: Int,
    iconWidth: Dp,
    iconHeight: Dp,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val cardContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            lerp(containerColor, Color.White, 0.08f),
                            containerColor,
                            lerp(containerColor, Color.Black, 0.06f),
                        ),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(
                        start = ResonoteTokens.spacing.space3,
                        top = ResonoteTokens.spacing.space3,
                        end = ResonoteTokens.spacing.space3,
                        bottom = ResonoteTokens.spacing.space2,
                    ),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space1)) {
                    Text(
                        title,
                        color = contentColor,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        supporting,
                        color = contentColor,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier
                            .align(if (actionLabel == null) Alignment.BottomCenter else Alignment.BottomStart)
                            .width(iconWidth)
                            .height(iconHeight),
                        colorFilter = ColorFilter.tint(contentColor),
                    )
                }
            }
            if (actionLabel != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(ResonoteTokens.touchTargets.minimum),
                ) {
                    ResonoteCompactFilledIconButton(
                        label = actionLabel,
                        onClick = onAction,
                        modifier = Modifier.fillMaxSize(),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = contentColor,
                            contentColor = containerColor,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }

    val cardModifier = modifier.aspectRatio(1f)
    if (onClick == null) {
        Surface(
            modifier = cardModifier,
            color = Color.Transparent,
            contentColor = contentColor,
            shape = MaterialTheme.shapes.large,
            content = cardContent,
        )
    } else {
        Surface(
            onClick = onClick,
            modifier = cardModifier,
            color = Color.Transparent,
            contentColor = contentColor,
            shape = MaterialTheme.shapes.large,
            content = cardContent,
        )
    }
}

@Composable
internal fun HomePlayAllButton(onClick: () -> Unit) {
    ResonotePlainAction(onClick = onClick) {
        Text(
            text = stringResource(R.string.feature_home_impl_play_all),
            modifier = Modifier.padding(horizontal = 8.dp),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

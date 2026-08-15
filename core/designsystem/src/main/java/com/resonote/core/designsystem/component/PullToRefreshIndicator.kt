package com.resonote.core.designsystem.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.R
import com.resonote.core.designsystem.tokens.ResonoteTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResonotePullToRefreshIndicator(state: PullToRefreshState, isRefreshing: Boolean, modifier: Modifier = Modifier) {
    val elevation = ResonoteTokens.elevation.level3
    val pullProgress = state.distanceFraction.coerceIn(0f, 1f)
    val thresholdReached = !isRefreshing && state.distanceFraction >= 1f
    val refreshingDescription = stringResource(R.string.core_designsystem_refreshing)
    val semanticsModifier = when {
        isRefreshing -> Modifier.semantics { stateDescription = refreshingDescription }
        pullProgress > 0f -> Modifier.semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(pullProgress, 0f..1f)
        }

        else -> Modifier
    }
    PullToRefreshDefaults.IndicatorBox(
        state = state,
        isRefreshing = isRefreshing,
        modifier = modifier.then(semanticsModifier),
        shape = ResonoteTokens.shapes.full,
        containerColor = if (thresholdReached) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            elevation.preferredSurfaceRole.color
        },
        elevation = elevation.maximumShadow,
    ) {
        Crossfade(
            targetState = isRefreshing,
            animationSpec = ResonoteTokens.motion.effectsFast(),
            label = "pull-to-refresh-state",
        ) { refreshing ->
            if (refreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                    strokeWidth = 2.5.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.ArrowDownward,
                    contentDescription = null,
                    tint = if (thresholdReached) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            alpha = (pullProgress * 4f).coerceIn(0f, 1f)
                            rotationZ = 180f * ((pullProgress - 0.5f) * 2f).coerceIn(0f, 1f)
                            val scale = 0.8f + pullProgress * 0.2f
                            scaleX = scale
                            scaleY = scale
                        },
                )
            }
        }
    }
}

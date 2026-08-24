package com.resonote.feature.home.impl

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteIconButton
import com.resonote.core.designsystem.component.ResonoteShimmer
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.designsystem.component.rememberResonoteShimmer
import com.resonote.core.designsystem.component.resonoteShimmer
import com.resonote.core.designsystem.tokens.ResonoteTokens

@Composable
internal fun HomeLoading(
    bottomContentPadding: Dp,
    onSearchClick: () -> Unit,
    onRecognitionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shimmer = rememberResonoteShimmer(label = "home-skeleton")

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("home-loading-skeleton"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HomeTopBar(onSearchClick, onRecognitionClick) },
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = scaffoldPadding.calculateTopPadding()),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space2)) {
                    repeat(3) {
                        Spacer(
                            Modifier.weight(1f).aspectRatio(1f)
                                .resonoteShimmer(shimmer, MaterialTheme.shapes.large),
                        )
                    }
                }
            }
            item { SkeletonSection(shimmer, rows = 3) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SkeletonLine(shimmer, width = 176.dp, height = 20.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(2) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Spacer(
                                    Modifier.fillMaxWidth().aspectRatio(1f)
                                        .resonoteShimmer(shimmer, MaterialTheme.shapes.large),
                                )
                                SkeletonLine(shimmer, width = 116.dp, height = 14.dp)
                                SkeletonLine(shimmer, width = 72.dp, height = 12.dp)
                            }
                        }
                    }
                }
            }
            item { SkeletonSection(shimmer, rows = 3) }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun HomeTopBar(onSearchClick: () -> Unit, onRecognitionClick: () -> Unit) {
    ResonoteTopAppBar(
        title = {
            Image(
                painter = painterResource(R.drawable.feature_home_impl_resonote_wordmark),
                contentDescription = stringResource(R.string.feature_home_impl_brand),
                modifier = Modifier.width(124.dp).height(40.dp),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            )
        },
        actions = {
            ResonoteIconButton(
                label = stringResource(R.string.feature_home_impl_search),
                onClick = onSearchClick,
                icon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            )
            ResonoteIconButton(
                label = stringResource(R.string.feature_home_impl_recognize),
                onClick = onRecognitionClick,
                icon = { Icon(Icons.Rounded.Mic, contentDescription = null) },
            )
        },
    )
}

@Composable
private fun SkeletonSection(shimmer: ResonoteShimmer, rows: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SkeletonLine(shimmer, width = 156.dp, height = 20.dp)
        repeat(rows) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(Modifier.size(56.dp).resonoteShimmer(shimmer, MaterialTheme.shapes.medium))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonLine(shimmer, width = 180.dp, height = 15.dp)
                    SkeletonLine(shimmer, width = 112.dp, height = 12.dp)
                }
            }
        }
    }
}

@Composable
private fun SkeletonLine(shimmer: ResonoteShimmer, width: Dp, height: Dp) {
    Spacer(Modifier.width(width).height(height).resonoteShimmer(shimmer, MaterialTheme.shapes.small))
}

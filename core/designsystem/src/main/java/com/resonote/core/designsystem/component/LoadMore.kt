package com.resonote.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.R
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

enum class ResonoteLoadMoreState { IDLE, LOADING, ERROR }

@Composable
fun ResonoteLoadMoreEffect(
    listState: LazyListState,
    itemCount: Int,
    enabled: Boolean,
    prefetchDistance: Int = 4,
    onLoadMore: () -> Unit,
) {
    require(prefetchDistance >= 0) { "prefetchDistance must not be negative" }
    var triggeredItemCount by remember(listState) { mutableIntStateOf(-1) }

    LaunchedEffect(listState, itemCount, enabled, prefetchDistance) {
        if (itemCount < triggeredItemCount) triggeredItemCount = -1
        if (!enabled || itemCount == 0 || triggeredItemCount == itemCount) return@LaunchedEffect
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 1 - prefetchDistance
        }.distinctUntilChanged().filter { it }.collect {
            if (triggeredItemCount != itemCount) {
                triggeredItemCount = itemCount
                onLoadMore()
            }
        }
    }
}

@Composable
fun ResonoteLoadMoreFooter(
    state: ResonoteLoadMoreState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    loading: @Composable () -> Unit = {
        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
    },
    error: @Composable () -> Unit = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.core_designsystem_load_more_failed),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onRetry) { Text(stringResource(R.string.core_designsystem_retry)) }
        }
    },
) {
    if (state == ResonoteLoadMoreState.IDLE) return
    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("resonote-load-more-footer"),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            ResonoteLoadMoreState.IDLE -> Unit
            ResonoteLoadMoreState.LOADING -> loading()
            ResonoteLoadMoreState.ERROR -> error()
        }
    }
}

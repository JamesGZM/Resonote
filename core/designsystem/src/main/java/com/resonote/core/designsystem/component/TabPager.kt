package com.resonote.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.drop

@Composable
fun ResonoteTabPager(
    selectedPage: Int,
    pageCount: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = selectedPage.coerceIn(0, pageCount - 1),
        pageCount = { pageCount },
    )
    LaunchedEffect(selectedPage, pageCount) {
        val targetPage = selectedPage.coerceIn(0, pageCount - 1)
        if (pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .drop(1)
            .collect(onPageSelected)
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        key = { it },
    ) { page ->
        content(page)
    }
}

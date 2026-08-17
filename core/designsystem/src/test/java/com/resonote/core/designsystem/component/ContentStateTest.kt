package com.resonote.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.google.common.truth.Truth.assertThat
import com.resonote.core.designsystem.theme.ResonoteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ContentStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentStateLayoutUsesReplacementSlot() {
        composeRule.setContent {
            ResonoteTheme {
                ResonoteContentStateLayout(
                    phase = ResonoteContentPhase.LOADING,
                    loading = { Text("custom loading") },
                    error = {},
                    content = {},
                )
            }
        }

        composeRule.onNodeWithText("custom loading").assertExists()
        composeRule.onNodeWithTag("resonote-loading-state").assertDoesNotExist()
    }

    @Test
    fun defaultErrorInvokesRetry() {
        var retries = 0
        composeRule.setContent {
            ResonoteTheme { ResonoteErrorState(onRetry = { retries += 1 }) }
        }

        composeRule.onNodeWithText("Retry").performClick()

        assertThat(retries).isEqualTo(1)
    }

    @Test
    fun defaultEmptyExposesStableStateSemantics() {
        composeRule.setContent { ResonoteTheme { ResonoteEmptyState() } }

        composeRule.onNodeWithTag("resonote-empty-state").assertExists()
        composeRule.onNodeWithText("Nothing here yet").assertExists()
    }

    @Test
    fun emptyStateUsesReplacementIllustration() {
        composeRule.setContent {
            ResonoteTheme {
                ResonoteEmptyState(illustration = { Text("custom illustration") })
            }
        }

        composeRule.onNodeWithText("custom illustration").assertExists()
    }

    @Test
    fun disabledPullToRefreshDoesNotInvokeRefresh() {
        var refreshes = 0
        composeRule.setContent {
            ResonoteTheme {
                ResonotePullToRefreshBox(
                    isRefreshing = false,
                    onRefresh = { refreshes += 1 },
                    enabled = false,
                    modifier = Modifier.fillMaxSize().testTag("refresh-box"),
                ) {
                    Box(Modifier.fillMaxSize())
                }
            }
        }

        composeRule.onNodeWithTag("refresh-box").performTouchInput { swipeDown() }

        assertThat(refreshes).isEqualTo(0)
    }

    @Test
    fun loadMoreTriggersOncePerItemCountAndContinuesAfterGrowth() {
        val itemCount = mutableIntStateOf(20)
        val enabled = mutableStateOf(true)
        var calls = 0
        composeRule.setContent {
            ResonoteTheme {
                val listState = rememberLazyListState()
                ResonoteLoadMoreEffect(
                    listState = listState,
                    itemCount = itemCount.intValue,
                    enabled = enabled.value,
                    onLoadMore = {
                        calls += 1
                        enabled.value = false
                    },
                )
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().testTag("paged-list")) {
                    items(itemCount.intValue) { Text("Item $it") }
                }
            }
        }

        composeRule.onNodeWithTag("paged-list").performScrollToIndex(19)
        composeRule.waitForIdle()
        assertThat(calls).isEqualTo(1)

        composeRule.runOnIdle {
            itemCount.intValue = 30
            enabled.value = true
        }
        composeRule.onNodeWithTag("paged-list").performScrollToIndex(29)
        composeRule.waitForIdle()

        assertThat(calls).isEqualTo(2)
    }
}

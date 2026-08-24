package com.resonote.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.resonote.core.screenshottesting.captureResonoteThemes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ContentStateScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentStates_multipleThemes() {
        composeRule.captureResonoteThemes("ContentState", "ContentStateVariants") {
            Surface {
                Row(Modifier.width(720.dp).height(420.dp)) {
                    ResonoteEmptyState(
                        title = "Nothing saved yet",
                        message = "Try another category or come back later.",
                        modifier = Modifier.weight(1f),
                    )
                    ResonoteErrorState(
                        onRetry = {},
                        title = "Couldn’t load content",
                        message = "Check your connection and try again.",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

package com.resonote.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.resonote.core.screenshottesting.captureResonoteFontScale
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
class SectionHeaderScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sectionHeaderVariants_multipleThemes() {
        composeRule.captureResonoteThemes("SectionHeader", "SectionHeaderVariants") {
            Surface {
                SectionHeaderGallery()
            }
        }
    }

    @Test
    fun sectionHeaderVariants_fontScale200() {
        composeRule.captureResonoteFontScale("SectionHeader", "SectionHeaderVariants") {
            Surface {
                SectionHeaderGallery()
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun SectionHeaderGallery() {
    Column(
        modifier = Modifier
            .width(360.dp)
            .padding(16.dp),
    ) {
        ResonoteSectionHeader(
            title = "Daily recommendations",
            supportingText = "Made for you, refreshed daily",
            trailingContent = {
                ResonoteTextButton(
                    label = "Play all",
                    onClick = {},
                )
            },
        )
        ResonoteSectionHeader(
            title = "Recommended playlists",
            supportingText = "Playlists selected for your taste",
        )
    }
}

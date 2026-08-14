package com.resonote.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
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
class ButtonScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun buttonVariants_multipleThemes() {
        composeRule.captureResonoteThemes("Button", "ButtonVariants") {
            Surface {
                ButtonGallery()
            }
        }
    }

    @Test
    fun iconButtonVariants_multipleThemes() {
        composeRule.captureResonoteThemes("IconButton", "IconButtonVariants") {
            Surface {
                IconButtonGallery()
            }
        }
    }

    @Test
    fun buttons_fontScale200() {
        composeRule.captureResonoteFontScale("Button", "ButtonVariants") {
            Surface {
                LargeTextButtonGallery()
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun ButtonGallery() {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        ResonoteButton(
            label = "Play",
            onClick = {},
            leadingIcon = { TestIcon() },
        )
        ResonoteTonalButton(label = "Save", onClick = {})
        ResonoteOutlinedButton(label = "Add", onClick = {})
        ResonoteTextButton(label = "Details", onClick = {})
        ResonoteDestructiveButton(label = "Delete", onClick = {})
        ResonoteDestructiveTextButton(label = "Remove", onClick = {})
        ResonoteButton(label = "Disabled", onClick = {}, enabled = false)
        ResonoteTonalButton(
            label = "Save",
            loadingLabel = "Saving…",
            onClick = {},
            loading = true,
        )
    }
}

@androidx.compose.runtime.Composable
private fun LargeTextButtonGallery() {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        ResonoteButton(
            label = "Play selected album",
            onClick = {},
            leadingIcon = { TestIcon() },
        )
        ResonoteOutlinedButton(label = "Add to library", onClick = {})
        ResonoteDestructiveButton(label = "Delete selection", onClick = {})
        ResonoteTonalButton(
            label = "Save",
            loadingLabel = "Saving changes…",
            onClick = {},
            loading = true,
        )
    }
}

@androidx.compose.runtime.Composable
private fun IconButtonGallery() {
    Row(
        modifier = Modifier.padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ResonoteIconButton(label = "Standard", onClick = {}, icon = { TestIcon() })
        ResonoteFilledIconButton(label = "Filled", onClick = {}, icon = { TestIcon() })
        ResonoteTonalIconButton(label = "Tonal", onClick = {}, icon = { TestIcon() })
        ResonoteOutlinedIconButton(label = "Outlined", onClick = {}, icon = { TestIcon() })
        ResonoteIconButton(
            label = "Disabled",
            onClick = {},
            enabled = false,
            icon = { TestIcon() },
        )
        ResonoteIconToggleButton(
            checked = false,
            label = "Unchecked",
            onCheckedChange = {},
            icon = { TestIcon() },
            checkedIcon = { TestIcon(filled = true) },
        )
        ResonoteIconToggleButton(
            checked = true,
            label = "Checked",
            onCheckedChange = {},
            icon = { TestIcon() },
            checkedIcon = { TestIcon(filled = true) },
        )
    }
}

@androidx.compose.runtime.Composable
private fun TestIcon(filled: Boolean = false) {
    val shape = MaterialTheme.shapes.extraSmall
    Box(
        modifier = Modifier
            .size(18.dp)
            .then(
                if (filled) {
                    Modifier.background(LocalContentColor.current, shape)
                } else {
                    Modifier.border(2.dp, LocalContentColor.current, shape)
                },
            ),
    )
}

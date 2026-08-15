package com.resonote.core.designsystem.component

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.resonote.core.screenshottesting.captureResonoteThemes
import kotlinx.coroutines.yield
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class PlainActionScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun plainActionStates_multipleThemes() {
        composeRule.captureResonoteThemes("PlainAction", "PlainActionStates") {
            Surface { PlainActionGallery() }
        }
    }
}

@Composable
private fun PlainActionGallery() {
    val pressedInteractionSource = remember { MutableInteractionSource() }
    val focusedInteractionSource = remember { MutableInteractionSource() }
    LaunchedEffect(pressedInteractionSource, focusedInteractionSource) {
        yield()
        pressedInteractionSource.emit(PressInteraction.Press(Offset.Zero))
        focusedInteractionSource.emit(FocusInteraction.Focus())
    }

    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PlainActionSample("Default")
        PlainActionSample("Pressed", interactionSource = pressedInteractionSource)
        PlainActionSample("Focused", interactionSource = focusedInteractionSource)
        PlainActionSample("Disabled", enabled = false)
    }
}

@Composable
private fun PlainActionSample(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    ResonotePlainAction(
        onClick = {},
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

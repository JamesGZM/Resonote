package com.resonote.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.screenshottesting.captureResonoteConfiguration
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
class TextFieldScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun persistentStates_multipleThemes() {
        composeRule.captureResonoteThemes("TextField", "PersistentStates") {
            ScreenshotSurface { PersistentStateGallery() }
        }
    }

    @Test
    fun metadataAndActions_multipleThemes() {
        composeRule.captureResonoteThemes("TextField", "MetadataAndActions") {
            ScreenshotSurface { MetadataAndActionGallery() }
        }
    }

    @Test
    fun typography_fontScale130() {
        captureTypography(fontScale = 1.3f, name = "Typography_fontScale130")
    }

    @Test
    fun typography_fontScale200() {
        captureTypography(fontScale = 2f, name = "Typography_fontScale200")
    }

    @Test
    fun persistentStates_fontScale200() {
        composeRule.captureResonoteConfiguration(
            group = "TextField",
            name = "PersistentStates_fontScale200",
            fontScale = 2f,
        ) {
            ScreenshotSurface { PersistentStateGallery() }
        }
    }

    @Test
    fun content_rtl() {
        composeRule.captureResonoteConfiguration(
            group = "TextField",
            name = "Content_rtl",
            layoutDirection = LayoutDirection.Rtl,
        ) {
            ScreenshotSurface {
                ResonoteTextField(
                    value = "ملاحظات الاستماع",
                    onValueChange = {},
                    label = "العنوان",
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = "يجب أن يبقى ترتيب القراءة صحيحًا",
                    prefix = "♫ ",
                    suffix = " · 24",
                    maxLength = 40,
                )
            }
        }
    }

    private fun captureTypography(fontScale: Float, name: String) {
        composeRule.captureResonoteConfiguration(
            group = "TextField",
            name = name,
            fontScale = fontScale,
        ) {
            ScreenshotSurface {
                ResonoteTextField(
                    value = "中英文 Mixed listening note",
                    onValueChange = {},
                    label = "这是一段较长的输入字段标签",
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = "辅助说明必须完整换行，不裁切、不重叠，也不能强制缩小文字。",
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                    maxLength = 48,
                )
            }
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h1000dp")
class TextFieldLargeFontScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun metadataAndActions_fontScale200() {
        composeRule.captureResonoteConfiguration(
            group = "TextField",
            name = "MetadataAndActions_fontScale200",
            fontScale = 2f,
        ) {
            ScreenshotSurface { MetadataAndActionGallery() }
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h400dp")
class TextFieldCompactWindowScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun windowCompact_pairwiseMatrix() = composeRule.captureWindow(
        WindowCase("compact", ResonoteThemeMode.LIGHT, 1f, LayoutDirection.Ltr),
    )
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w700dp-h400dp")
class TextFieldMediumWindowScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun windowMedium_pairwiseMatrix() = composeRule.captureWindow(
        WindowCase("medium", ResonoteThemeMode.DARK, 1.3f, LayoutDirection.Rtl),
    )
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w1000dp-h400dp")
class TextFieldExpandedWindowScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun windowExpanded_pairwiseMatrix() = composeRule.captureWindow(
        WindowCase("expanded", ResonoteThemeMode.AMOLED, 2f, LayoutDirection.Ltr),
    )
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w1300dp-h400dp")
class TextFieldLargeWindowScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun windowLarge_pairwiseMatrix() = composeRule.captureWindow(
        WindowCase("large", ResonoteThemeMode.LIGHT, 1.3f, LayoutDirection.Rtl),
    )
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w1600dp-h400dp")
class TextFieldExtraLargeWindowScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun windowExtraLarge_pairwiseMatrix() = composeRule.captureWindow(
        WindowCase("extra_large", ResonoteThemeMode.DARK, 2f, LayoutDirection.Ltr),
    )
}

private fun ComposeContentTestRule.captureWindow(case: WindowCase) {
    captureResonoteConfiguration(
        group = "TextField",
        name = "Window_${case.name}",
        themeMode = case.themeMode,
        fontScale = case.fontScale,
        layoutDirection = case.layoutDirection,
    ) {
        Surface(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.padding(24.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                ResonoteTextField(
                    value = "Window-class evidence",
                    onValueChange = {},
                    label = "Responsive field",
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth(),
                    supportingText = "The semantic result stays identical at every width",
                    maxLength = 64,
                )
            }
        }
    }
}

@Composable
private fun ScreenshotSurface(content: @Composable () -> Unit) {
    Surface {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = { content() },
        )
    }
}

@Composable
private fun PersistentStateGallery() {
    val focusRequester = androidx.compose.runtime.remember { FocusRequester() }
    LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ResonoteTextField(
            value = "",
            onValueChange = {},
            label = "Empty",
            placeholder = "Placeholder",
            modifier = Modifier.fillMaxWidth(),
        )
        ResonoteTextField(
            value = "Focused value",
            onValueChange = {},
            label = "Focused",
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )
        ResonoteTextField(
            value = "Filled value",
            onValueChange = {},
            label = "Filled",
            modifier = Modifier.fillMaxWidth(),
            supportingText = "Supporting text",
        )
    }
}

@Composable
private fun MetadataAndActionGallery() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ResonoteTextField(
            value = "invalid address",
            onValueChange = {},
            label = "Error",
            modifier = Modifier.fillMaxWidth(),
            errorMessage = "Enter a valid email address",
            maxLength = 12,
        )
        ResonoteTextField(
            value = "Disabled value",
            onValueChange = {},
            label = "Disabled",
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            supportingText = "Disabled supporting text",
            maxLength = 20,
        )
        ResonoteTextField(
            value = "2026",
            onValueChange = {},
            label = "Metadata",
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            leadingIcon = { TestFieldIcon() },
            prefix = "ID · ",
            suffix = " / CN",
        )
        ResonoteTextField(
            value = "secret",
            onValueChange = {},
            label = "Password",
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            trailingAction = {
                ResonoteIconButton(
                    label = "Show password",
                    onClick = {},
                    icon = { TestFieldIcon(filled = true) },
                )
            },
        )
    }
}

@Composable
private fun TestFieldIcon(filled: Boolean = false) {
    val shape = MaterialTheme.shapes.extraSmall
    Box(
        modifier = Modifier
            .size(24.dp)
            .then(
                if (filled) {
                    Modifier.background(LocalContentColor.current, shape)
                } else {
                    Modifier.border(2.dp, LocalContentColor.current, shape)
                },
            ),
    )
}

private data class WindowCase(
    val name: String,
    val themeMode: ResonoteThemeMode,
    val fontScale: Float,
    val layoutDirection: LayoutDirection,
)

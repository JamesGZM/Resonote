package com.resonote.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.Locales
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.resonote.core.designsystem.theme.ResonoteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class TextFieldTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun enabledTextField_acceptsInput_andMeetsMd3MinimumHeight() {
        composeRule.setContent {
            var value by remember { mutableStateOf("") }
            ResonoteTheme {
                ResonoteTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = "Email",
                    modifier = Modifier.testTag("field"),
                )
            }
        }

        composeRule.onNodeWithTag("field")
            .assertIsEnabled()
            .assertHeightIsAtLeast(56.dp)
            .performTextInput("listener@example.com")
        composeRule.onNodeWithTag("field")
            .assertTextContains("listener@example.com")
    }

    @Test
    fun hoistedValue_reflectsProgrammaticUpdates() {
        val value = mutableStateOf("Before")
        composeRule.setContent {
            ResonoteTheme {
                ResonoteTextField(
                    value = value.value,
                    onValueChange = { value.value = it },
                    label = "Title",
                    modifier = Modifier.testTag("field"),
                )
            }
        }

        value.value = "After"
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("field").assertTextContains("After")
    }

    @Test
    fun maxLength_countsUnicodeCodePoints_andRejectsOversizedEdits() {
        var acceptedValue = ""
        var callbackCount = 0
        composeRule.setContent {
            var value by remember { mutableStateOf("") }
            ResonoteTheme {
                ResonoteTextField(
                    value = value,
                    onValueChange = {
                        callbackCount++
                        acceptedValue = it
                        value = it
                    },
                    label = "Code",
                    modifier = Modifier.testTag("field"),
                    maxLength = 3,
                )
            }
        }

        composeRule.onNodeWithTag("field").performTextInput("ab😀")
        composeRule.waitForIdle()
        val callbacksAtLimit = callbackCount
        composeRule.onNodeWithTag("field").performTextInput("x")
        composeRule.onNodeWithTag("field").performTextReplacement("oversized")

        assertThat(acceptedValue).isEqualTo("ab😀")
        assertThat(callbackCount).isEqualTo(callbacksAtLimit)
        composeRule.onNodeWithText("3 / 3").assertExists()
    }

    @Test
    fun externalOversizedValue_acceptsEditsThatMoveTowardTheLimit() {
        val value = mutableStateOf("abcdefghij")
        composeRule.setContent {
            ResonoteTheme {
                ResonoteTextField(
                    value = value.value,
                    onValueChange = { value.value = it },
                    label = "Code",
                    modifier = Modifier.testTag("field"),
                    maxLength = 5,
                )
            }
        }

        listOf("abcdefghi", "abcdefgh", "abcdefg", "abcdef", "abcde").forEach { shorterValue ->
            composeRule.onNodeWithTag("field").performTextReplacement(shorterValue)
            composeRule.waitForIdle()
            assertThat(value.value).isEqualTo(shorterValue)
        }

        composeRule.onNodeWithTag("field").assertTextContains("abcde")
        composeRule.onNodeWithText("5 / 5").assertExists()
    }

    @Test
    fun externalOversizedValue_isPreservedAndExposesError() {
        composeRule.setContent {
            ResonoteTheme {
                ResonoteTextField(
                    value = "four",
                    onValueChange = {},
                    label = "Code",
                    modifier = Modifier.testTag("field"),
                    maxLength = 3,
                )
            }
        }

        composeRule.onNodeWithTag("field")
            .assertTextContains("four")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Error,
                    "Maximum 3 characters.",
                ),
            )
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.MaxTextLength, 3))
        composeRule.onNodeWithText("Maximum 3 characters.").assertExists()
        composeRule.onNodeWithText("4 / 3").assertExists()
    }

    @Test
    fun externalOversizedValue_usesLocalizedErrorMessage() {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.Locales(LocaleList(Locale("zh-CN"))),
            ) {
                ResonoteTheme {
                    ResonoteTextField(
                        value = "four",
                        onValueChange = {},
                        label = "代码",
                        maxLength = 3,
                    )
                }
            }
        }

        composeRule.onNodeWithText("最多可输入 3 个字符。").assertExists()
    }

    @Test
    fun explicitError_takesPrecedenceOverSupportingText() {
        composeRule.setContent {
            ResonoteTheme {
                ResonoteTextField(
                    value = "invalid",
                    onValueChange = {},
                    label = "Email",
                    supportingText = "We never share your email",
                    errorMessage = "Enter a valid email",
                    modifier = Modifier.testTag("field"),
                )
            }
        }

        composeRule.onNodeWithTag("field").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Error, "Enter a valid email"),
        )
        composeRule.onNodeWithText("Enter a valid email").assertExists()
        composeRule.onNodeWithText("We never share your email").assertDoesNotExist()
    }

    @Test
    fun disabledAndReadOnlyFields_doNotExposeEditingAction() {
        composeRule.setContent {
            ResonoteTheme {
                androidx.compose.foundation.layout.Column {
                    ResonoteTextField(
                        value = "Disabled",
                        onValueChange = {},
                        label = "Disabled field",
                        modifier = Modifier.testTag("disabled"),
                        enabled = false,
                    )
                    ResonoteTextField(
                        value = "Read only",
                        onValueChange = {},
                        label = "Read-only field",
                        modifier = Modifier.testTag("read-only"),
                        readOnly = true,
                    )
                }
            }
        }

        composeRule.onNodeWithTag("disabled").assertIsNotEnabled()
        assertThat(composeRule.onNodeWithTag("read-only").fetchSemanticsNode().config[SemanticsProperties.IsEditable])
            .isFalse()
    }

    @Test
    fun imeAction_isForwarded() {
        var doneActions = 0
        composeRule.setContent {
            ResonoteTheme {
                ResonoteTextField(
                    value = "Ready",
                    onValueChange = {},
                    label = "Search",
                    modifier = Modifier.testTag("field"),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { doneActions++ }),
                )
            }
        }

        composeRule.onNodeWithTag("field").performImeAction()

        assertThat(doneActions).isEqualTo(1)
    }

    @Test
    fun multilineField_acceptsNewlines_andCanGrow() {
        composeRule.setContent {
            var value by remember { mutableStateOf("") }
            ResonoteTheme {
                ResonoteTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = "Note",
                    modifier = Modifier.testTag("field"),
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                )
            }
        }

        composeRule.onNodeWithTag("field").performTextInput("First line\nSecond line")
        composeRule.onNodeWithTag("field")
            .assertTextContains("First line\nSecond line")
            .assertHeightIsAtLeast(56.dp)
    }

    @Test
    fun passwordTransformation_exposesPasswordSemantics() {
        composeRule.setContent {
            ResonoteTheme {
                ResonoteTextField(
                    value = "secret",
                    onValueChange = {},
                    label = "Password",
                    modifier = Modifier.testTag("field"),
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
        }

        composeRule.onNodeWithTag("field")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
    }

    @Test
    fun leadingIconIsDecorative_andTrailingActionMeetsTouchTarget() {
        composeRule.setContent {
            ResonoteTheme {
                ResonoteTextField(
                    value = "secret",
                    onValueChange = {},
                    label = "Password",
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("leading-icon"),
                        )
                    },
                    trailingAction = {
                        ResonoteIconButton(
                            label = "Show password",
                            onClick = {},
                            icon = { Box(Modifier.fillMaxSize()) },
                        )
                    },
                )
            }
        }

        composeRule.onNodeWithTag("leading-icon", useUnmergedTree = true)
            .assertWidthIsEqualTo(24.dp)
        val trailingAction = composeRule.onNodeWithContentDescription("Show password")
            .performClick()
            .assertWidthIsEqualTo(40.dp)
        assertThat(trailingAction.fetchSemanticsNode().touchBoundsInRoot.width / composeRule.density.density)
            .isAtLeast(48f)
    }

    @Test
    fun decorativeTrailingIcon_isSilentAndConstrainedToTwentyFourDp() {
        var iconWidthPx = 0
        var iconHeightPx = 0
        composeRule.setContent {
            ResonoteTheme {
                ResonoteTextField(
                    value = "Metadata",
                    onValueChange = {},
                    label = "Metadata",
                    trailingIcon = {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .onSizeChanged {
                                    iconWidthPx = it.width
                                    iconHeightPx = it.height
                                }
                                .semantics { contentDescription = "Decorative metadata" },
                        )
                    },
                )
            }
        }

        composeRule.waitForIdle()

        assertThat(iconWidthPx / composeRule.density.density).isEqualTo(24f)
        assertThat(iconHeightPx / composeRule.density.density).isEqualTo(24f)
        composeRule.onNodeWithContentDescription("Decorative metadata").assertDoesNotExist()
    }

    @Test
    fun readOnlyField_remainsFocusableForSelection() {
        composeRule.setContent {
            ResonoteTheme {
                ResonoteTextField(
                    value = "Copy me",
                    onValueChange = {},
                    label = "Read-only",
                    modifier = Modifier.testTag("field"),
                    readOnly = true,
                )
            }
        }

        composeRule.onNodeWithTag("field").performClick().assertIsFocused()
        composeRule.onNode(hasSetTextAction()).assertDoesNotExist()
    }
}

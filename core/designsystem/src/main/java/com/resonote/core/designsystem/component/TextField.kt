/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.resonote.core.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.maxTextLength
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import com.resonote.core.designsystem.R
import com.resonote.core.designsystem.tokens.ResonoteTokens

@Composable
fun ResonoteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    placeholder: String? = null,
    supportingText: String? = null,
    errorMessage: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    trailingAction: (@Composable () -> Unit)? = null,
    prefix: String? = null,
    suffix: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    maxLength: Int? = null,
) {
    require(maxLength == null || maxLength >= 0) { "maxLength must be non-negative" }
    require(trailingIcon == null || trailingAction == null) {
        "Only one of trailingIcon or trailingAction may be provided"
    }

    val characterCount = value.codePointCount(0, value.length)
    val lengthExceeded = maxLength != null && characterCount > maxLength
    val resolvedErrorMessage = when {
        errorMessage != null -> errorMessage
        lengthExceeded -> pluralStringResource(
            R.plurals.core_designsystem_text_field_character_limit_exceeded,
            maxLength,
            maxLength,
        )
        else -> null
    }
    val isError = resolvedErrorMessage != null
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = OutlinedTextFieldDefaults.colors()
    val textColor = when {
        !enabled -> colors.disabledTextColor
        isError -> colors.errorTextColor
        focused -> colors.focusedTextColor
        else -> colors.unfocusedTextColor
    }
    val textStyle = MaterialTheme.typography.bodyLarge.merge(TextStyle(color = textColor))
    val density = LocalDensity.current
    val minimizedLabelHalfHeight = with(density) {
        MaterialTheme.typography.bodySmall.lineHeight.toDp() / 2
    }
    val stackAffixes = density.fontScale >= LARGE_TEXT_FONT_SCALE
    val resolvedLeadingIcon = if (leadingIcon != null) {
        decorativeFieldIcon(leadingIcon)
    } else {
        null
    }
    val resolvedTrailingIcon = when {
        trailingAction != null -> trailingAction
        trailingIcon != null -> decorativeFieldIcon(trailingIcon)
        else -> null
    }
    val supportingContent = if (
        resolvedErrorMessage != null ||
        supportingText != null ||
        maxLength != null ||
        (stackAffixes && (prefix != null || suffix != null))
    ) {
        @Composable {
            ResonoteTextFieldSupportingContent(
                message = resolvedErrorMessage ?: supportingText,
                characterCount = characterCount,
                maxLength = maxLength,
                stackedPrefix = prefix.takeIf { stackAffixes },
                stackedSuffix = suffix.takeIf { stackAffixes },
            )
        }
    } else {
        null
    }

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        // The stable MD3 decoration box preserves the official layout while allowing Resonote's
        // frozen 2dp unfocused error outline, which the high-level overload cannot customize.
        BasicTextField(
            value = value,
            onValueChange = { updatedValue ->
                val updatedCount = updatedValue.codePointCount(0, updatedValue.length)
                if (
                    maxLength == null ||
                    updatedCount <= maxLength ||
                    updatedCount < characterCount
                ) {
                    onValueChange(updatedValue)
                }
            },
            modifier = modifier
                .semantics(mergeDescendants = true) {
                    if (maxLength != null) maxTextLength = maxLength
                    if (resolvedErrorMessage != null) error(resolvedErrorMessage)
                }
                .minimumInteractiveComponentSize()
                .padding(top = minimizedLabelHalfHeight)
                .defaultMinSize(
                    minWidth = OutlinedTextFieldDefaults.MinWidth,
                    minHeight = OutlinedTextFieldDefaults.MinHeight,
                ),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle,
            cursorBrush = SolidColor(if (isError) colors.errorCursorColor else colors.cursorColor),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            decorationBox = { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value,
                    innerTextField = innerTextField,
                    enabled = enabled,
                    singleLine = singleLine,
                    visualTransformation = visualTransformation,
                    interactionSource = interactionSource,
                    isError = isError,
                    label = { Text(label) },
                    placeholder = placeholder?.let { text ->
                        { Text(text = text, style = MaterialTheme.typography.bodyLarge) }
                    },
                    leadingIcon = resolvedLeadingIcon,
                    trailingIcon = resolvedTrailingIcon,
                    prefix = prefix.takeUnless { stackAffixes }?.let { text ->
                        { Text(text = text, style = MaterialTheme.typography.bodyLarge) }
                    },
                    suffix = suffix.takeUnless { stackAffixes }?.let { text ->
                        { Text(text = text, style = MaterialTheme.typography.bodyLarge) }
                    },
                    supportingText = supportingContent,
                    colors = colors,
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = enabled,
                            isError = isError,
                            interactionSource = interactionSource,
                            colors = colors,
                            shape = MaterialTheme.shapes.extraSmall,
                            focusedBorderThickness = OutlinedTextFieldDefaults.FocusedBorderThickness,
                            unfocusedBorderThickness = resonoteUnfocusedTextFieldBorderThickness(isError),
                        )
                    },
                )
            },
        )
    }
}

@Composable
private fun decorativeFieldIcon(icon: @Composable () -> Unit): @Composable () -> Unit = {
    Box(
        modifier = Modifier
            .size(ResonoteTokens.icons.default)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

internal fun resonoteUnfocusedTextFieldBorderThickness(isError: Boolean) = if (isError) {
    OutlinedTextFieldDefaults.FocusedBorderThickness
} else {
    OutlinedTextFieldDefaults.UnfocusedBorderThickness
}

@Composable
private fun ResonoteTextFieldSupportingContent(
    message: String?,
    characterCount: Int,
    maxLength: Int?,
    stackedPrefix: String?,
    stackedSuffix: String?,
) {
    val counter = maxLength?.let { "$characterCount / $it" }

    when {
        message != null || counter != null || stackedPrefix != null || stackedSuffix != null -> Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space1),
        ) {
            stackedPrefix?.let { SupportingText(it) }
            stackedSuffix?.let { SupportingText(it) }
            message?.let { SupportingText(it) }
            if (counter != null) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    SupportingText(counter)
                }
            }
        }
    }
}

@Composable
private fun SupportingText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
    )
}

private const val LARGE_TEXT_FONT_SCALE = 2f

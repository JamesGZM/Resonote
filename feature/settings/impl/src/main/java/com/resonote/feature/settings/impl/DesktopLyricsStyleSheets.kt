@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.settings.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteBottomSheet
import com.resonote.core.designsystem.component.ResonoteBottomSheetHeader
import com.resonote.core.designsystem.component.ResonoteRotaryKnob
import com.resonote.core.model.DesktopLyricsDefaults
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun DesktopLyricsWidthSheet(
    widthPercent: Int,
    onWidthChange: (Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) = DesktopLyricsKnobSheet(
    title = stringResource(R.string.feature_settings_impl_desktop_lyrics_width),
    subtitle = stringResource(R.string.feature_settings_impl_desktop_lyrics_width_sheet_body),
    knobTitle = stringResource(R.string.feature_settings_impl_desktop_lyrics_width_knob),
    value = widthPercent.toFloat(),
    defaultValue = DesktopLyricsDefaults.WIDTH_PERCENT.toFloat(),
    valueLabel = { desktopLyricsWidthLabel(it.roundToInt()) },
    onValueChangeFinished = { onWidthChange(it.roundToInt()) },
    valueRange = 40f..100f,
    steps = 59,
    testTag = "desktop-lyrics-width-knob",
    onReset = onReset,
    onDismiss = onDismiss,
)

@Composable
internal fun DesktopLyricsOpacitySheet(
    opacityPercent: Int,
    onOpacityChange: (Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) = DesktopLyricsKnobSheet(
    title = stringResource(R.string.feature_settings_impl_desktop_lyrics_opacity),
    subtitle = stringResource(R.string.feature_settings_impl_desktop_lyrics_opacity_sheet_body),
    knobTitle = stringResource(R.string.feature_settings_impl_desktop_lyrics_opacity_knob),
    value = opacityPercent.toFloat(),
    defaultValue = DesktopLyricsDefaults.SURFACE_OPACITY.toFloat(),
    valueLabel = { desktopLyricsOpacityLabel(it.roundToInt()) },
    onValueChangeFinished = { onOpacityChange(it.roundToInt()) },
    valueRange = 0f..100f,
    steps = 99,
    testTag = "desktop-lyrics-opacity-knob",
    onReset = onReset,
    onDismiss = onDismiss,
)

@Composable
internal fun DesktopLyricsFontSizeSheet(
    fontSizeSp: Int,
    onFontSizeChange: (Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) = DesktopLyricsKnobSheet(
    title = stringResource(R.string.feature_settings_impl_desktop_lyrics_font_size),
    subtitle = stringResource(R.string.feature_settings_impl_desktop_lyrics_font_size_sheet_body),
    knobTitle = stringResource(R.string.feature_settings_impl_desktop_lyrics_font_size_knob),
    value = fontSizeSp.toFloat(),
    defaultValue = DesktopLyricsDefaults.FONT_SIZE_SP.toFloat(),
    valueLabel = { desktopLyricsFontSizeLabel(it.roundToInt()) },
    onValueChangeFinished = { onFontSizeChange(it.roundToInt()) },
    valueRange = 16f..40f,
    steps = 23,
    testTag = "desktop-lyrics-font-size-knob",
    onReset = onReset,
    onDismiss = onDismiss,
)

@Composable
internal fun DesktopLyricsOutlineSheet(
    colorArgb: Int,
    widthDp: Float,
    onColorClick: () -> Unit,
    onWidthChange: (Float) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var width by remember(widthDp) { mutableFloatStateOf(widthDp) }

    ResonoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
                .testTag("desktop-lyrics-outline-sheet"),
        ) {
            ResonoteBottomSheetHeader(
                title = stringResource(R.string.feature_settings_impl_desktop_lyrics_outline),
                subtitle = stringResource(R.string.feature_settings_impl_desktop_lyrics_outline_sheet_body),
                actions = {
                    RestoreDefaultAction {
                        width = DesktopLyricsDefaults.OUTLINE_WIDTH_DP
                        onReset()
                    }
                },
            )
            SettingsColorRow(
                title = stringResource(R.string.feature_settings_impl_desktop_lyrics_outline_color),
                color = Color(colorArgb),
                onClick = onColorClick,
                modifier = Modifier.testTag("desktop-lyrics-outline-color-picker"),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                ResonoteRotaryKnob(
                    title = stringResource(R.string.feature_settings_impl_desktop_lyrics_outline_thickness),
                    valueLabel = desktopLyricsOutlineLabel(width),
                    value = width,
                    onValueChange = { width = it },
                    onValueChangeFinished = onWidthChange,
                    valueRange = 0f..4f,
                    steps = 39,
                    knobSize = 136.dp,
                    modifier = Modifier.testTag("desktop-lyrics-outline-width-knob"),
                )
            }
        }
    }
}

@Composable
private fun DesktopLyricsKnobSheet(
    title: String,
    subtitle: String,
    knobTitle: String,
    value: Float,
    defaultValue: Float,
    valueLabel: @Composable (Float) -> String,
    onValueChangeFinished: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    testTag: String,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var currentValue by remember(value) { mutableFloatStateOf(value) }

    ResonoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 20.dp)) {
            ResonoteBottomSheetHeader(
                title = title,
                subtitle = subtitle,
                actions = {
                    RestoreDefaultAction {
                        currentValue = defaultValue
                        onReset()
                    }
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                ResonoteRotaryKnob(
                    title = knobTitle,
                    valueLabel = valueLabel(currentValue),
                    value = currentValue,
                    onValueChange = { currentValue = it },
                    onValueChangeFinished = onValueChangeFinished,
                    valueRange = valueRange,
                    steps = steps,
                    knobSize = 136.dp,
                    modifier = Modifier.testTag(testTag),
                )
            }
        }
    }
}

@Composable
internal fun DesktopLyricsShadowSheet(
    colorArgb: Int,
    offsetXDp: Float,
    offsetYDp: Float,
    blurRadiusDp: Float,
    onColorClick: () -> Unit,
    onOffsetXChange: (Float) -> Unit,
    onOffsetYChange: (Float) -> Unit,
    onBlurRadiusChange: (Float) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var horizontal by remember(offsetXDp) { mutableFloatStateOf(offsetXDp) }
    var vertical by remember(offsetYDp) { mutableFloatStateOf(offsetYDp) }
    var softness by remember(blurRadiusDp) { mutableFloatStateOf(blurRadiusDp) }

    ResonoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
                .testTag("desktop-lyrics-shadow-sheet"),
        ) {
            ResonoteBottomSheetHeader(
                title = stringResource(R.string.feature_settings_impl_desktop_lyrics_shadow),
                subtitle = stringResource(R.string.feature_settings_impl_desktop_lyrics_shadow_sheet_body),
                actions = {
                    RestoreDefaultAction {
                        horizontal = DesktopLyricsDefaults.SHADOW_OFFSET_X_DP
                        vertical = DesktopLyricsDefaults.SHADOW_OFFSET_Y_DP
                        softness = DesktopLyricsDefaults.SHADOW_BLUR_RADIUS_DP
                        onReset()
                    }
                },
            )
            SettingsColorRow(
                title = stringResource(R.string.feature_settings_impl_desktop_lyrics_shadow_color),
                color = Color(colorArgb),
                onClick = onColorClick,
                modifier = Modifier.testTag("desktop-lyrics-shadow-color-picker"),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ResonoteRotaryKnob(
                    title = stringResource(R.string.feature_settings_impl_desktop_lyrics_shadow_x),
                    valueLabel = desktopLyricsDecimalLabel(horizontal),
                    value = horizontal,
                    onValueChange = { horizontal = it },
                    onValueChangeFinished = onOffsetXChange,
                    valueRange = -8f..8f,
                    steps = 159,
                    modifier = Modifier.weight(1f).testTag("desktop-lyrics-shadow-x-knob"),
                )
                ResonoteRotaryKnob(
                    title = stringResource(R.string.feature_settings_impl_desktop_lyrics_shadow_y),
                    valueLabel = desktopLyricsDecimalLabel(vertical),
                    value = vertical,
                    onValueChange = { vertical = it },
                    onValueChangeFinished = onOffsetYChange,
                    valueRange = -8f..8f,
                    steps = 159,
                    modifier = Modifier.weight(1f).testTag("desktop-lyrics-shadow-y-knob"),
                )
                ResonoteRotaryKnob(
                    title = stringResource(R.string.feature_settings_impl_desktop_lyrics_shadow_z),
                    valueLabel = desktopLyricsDecimalLabel(softness),
                    value = softness,
                    onValueChange = { softness = it },
                    onValueChangeFinished = onBlurRadiusChange,
                    valueRange = 0f..12f,
                    steps = 119,
                    modifier = Modifier.weight(1f).testTag("desktop-lyrics-shadow-softness-knob"),
                )
            }
        }
    }
}

@Composable
internal fun desktopLyricsWidthLabel(value: Int): String =
    stringResource(R.string.feature_settings_impl_desktop_lyrics_integer_value, value)

@Composable
internal fun desktopLyricsOpacityLabel(value: Int): String =
    stringResource(R.string.feature_settings_impl_desktop_lyrics_percent_value, value)

@Composable
internal fun desktopLyricsFontSizeLabel(value: Int): String =
    stringResource(R.string.feature_settings_impl_desktop_lyrics_integer_value, value)

@Composable
internal fun desktopLyricsOutlineLabel(value: Float): String = desktopLyricsDecimalLabel(value)

@Composable
internal fun desktopLyricsShadowLabel(offsetX: Float, offsetY: Float, blur: Float): String = stringResource(
    R.string.feature_settings_impl_desktop_lyrics_shadow_summary,
    desktopLyricsDecimalLabel(offsetX),
    desktopLyricsDecimalLabel(offsetY),
    desktopLyricsDecimalLabel(blur),
)

@Composable
private fun desktopLyricsDecimalLabel(value: Float): String = stringResource(
    R.string.feature_settings_impl_desktop_lyrics_decimal_value,
    if (abs(value) < 0.05f) 0f else value,
)

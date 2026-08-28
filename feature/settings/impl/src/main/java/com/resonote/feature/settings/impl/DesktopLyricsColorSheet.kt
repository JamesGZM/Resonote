@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.settings.impl

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteBottomSheet
import com.resonote.core.designsystem.component.ResonoteBottomSheetHeader
import android.graphics.Color as AndroidColor

@Composable
internal fun DesktopLyricsColorSheet(
    title: String,
    subtitle: String,
    colorArgb: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialHsv = remember(colorArgb) {
        FloatArray(3).also { AndroidColor.colorToHSV(colorArgb, it) }
    }
    var hue by remember(colorArgb) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(colorArgb) { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember(colorArgb) { mutableFloatStateOf(initialHsv[2]) }
    val selectedColor = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness))
    val colorLabel = "#%06X".format(selectedColor and 0xFFFFFF)

    ResonoteBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 24.dp),
        ) {
            ResonoteBottomSheetHeader(
                title = title,
                subtitle = subtitle,
            )
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SaturationBrightnessPalette(
                    contentDescription = title,
                    hue = hue,
                    saturation = saturation,
                    brightness = brightness,
                    onChange = { newSaturation, newBrightness ->
                        saturation = newSaturation
                        brightness = newBrightness
                    },
                )
                HuePalette(hue = hue, onChange = { hue = it })
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.size(40.dp)
                            .background(Color(selectedColor), CircleShape),
                    )
                    Text(
                        text = stringResource(
                            R.string.feature_settings_impl_desktop_lyrics_color_value,
                            colorLabel,
                        ),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = { onSelect(selectedColor) }) {
                        Text(stringResource(R.string.feature_settings_impl_done))
                    }
                }
            }
        }
    }
}

@Composable
private fun SaturationBrightnessPalette(
    contentDescription: String,
    hue: Float,
    saturation: Float,
    brightness: Float,
    onChange: (Float, Float) -> Unit,
) {
    Canvas(
        Modifier.fillMaxWidth().height(180.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
            .semantics { this.contentDescription = contentDescription }
            .testTag("desktop-lyrics-shadow-palette")
            .pointerInput(hue) {
                fun update(position: Offset) {
                    onChange(
                        (position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f),
                        (1f - position.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f),
                    )
                }
                detectTapGestures(onPress = { update(it) })
            }
            .pointerInput(hue) {
                fun update(position: Offset) {
                    onChange(
                        (position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f),
                        (1f - position.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f),
                    )
                }
                detectDragGestures(
                    onDragStart = ::update,
                    onDrag = { change, _ ->
                        update(change.position)
                        change.consume()
                    },
                )
            },
    ) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f))))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val marker = Offset(saturation * size.width, (1f - brightness) * size.height)
        drawCircle(Color.White, radius = 7.dp.toPx(), center = marker, style = Stroke(2.dp.toPx()))
        drawCircle(Color.Black, radius = 9.dp.toPx(), center = marker, style = Stroke(1.dp.toPx()))
    }
}

@Composable
private fun HuePalette(hue: Float, onChange: (Float) -> Unit) {
    Canvas(
        Modifier.fillMaxWidth().height(28.dp)
            .pointerInput(Unit) {
                fun update(position: Offset) {
                    onChange((position.x / size.width.coerceAtLeast(1) * 360f).coerceIn(0f, 359.9f))
                }
                detectTapGestures(onPress = { update(it) })
            }
            .pointerInput(Unit) {
                fun update(position: Offset) {
                    onChange((position.x / size.width.coerceAtLeast(1) * 360f).coerceIn(0f, 359.9f))
                }
                detectDragGestures(
                    onDragStart = ::update,
                    onDrag = { change, _ ->
                        update(change.position)
                        change.consume()
                    },
                )
            },
    ) {
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(
                    Color.hsv(0f, 1f, 1f),
                    Color.hsv(60f, 1f, 1f),
                    Color.hsv(120f, 1f, 1f),
                    Color.hsv(180f, 1f, 1f),
                    Color.hsv(240f, 1f, 1f),
                    Color.hsv(300f, 1f, 1f),
                    Color.hsv(359.9f, 1f, 1f),
                ),
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
        )
        val markerX = hue / 360f * size.width
        drawCircle(Color.White, radius = 7.dp.toPx(), center = Offset(markerX, size.height / 2f))
        drawCircle(
            Color.Black,
            radius = 8.dp.toPx(),
            center = Offset(markerX, size.height / 2f),
            style = Stroke(1.dp.toPx()),
        )
    }
}

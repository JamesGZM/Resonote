@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.settings.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteBottomSheet
import com.resonote.core.designsystem.component.ResonoteBottomSheetHeader
import com.resonote.core.model.ThemeMode

@Composable
internal fun AppearanceSettingsSheet(
    themeMode: ThemeMode,
    dynamicColorEnabled: Boolean,
    supportsDynamicColor: Boolean,
    enabled: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ResonoteBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 24.dp)) {
            ResonoteBottomSheetHeader(
                title = stringResource(R.string.feature_settings_impl_appearance_section),
                subtitle = stringResource(R.string.feature_settings_impl_appearance_sheet_body),
            )
            Spacer(Modifier.height(14.dp))
            ThemeMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = mode == themeMode,
                            enabled = enabled,
                            role = Role.RadioButton,
                            onClick = { onThemeModeChange(mode) },
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = mode == themeMode, onClick = null, enabled = enabled)
                    Text(mode.label(), style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (supportsDynamicColor) {
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.feature_settings_impl_dynamic_color),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            stringResource(R.string.feature_settings_impl_dynamic_color_body),
                            modifier = Modifier.padding(top = 2.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = dynamicColorEnabled,
                        onCheckedChange = onDynamicColorChange,
                        enabled = enabled,
                    )
                }
            }
        }
    }
}

@Composable
internal fun <T> SettingsSingleChoiceSheet(
    title: String,
    selected: T,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    ResonoteBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
            ResonoteBottomSheetHeader(title = title)
            Spacer(Modifier.height(8.dp))
            options.forEach { (value, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = value == selected,
                            role = Role.RadioButton,
                            onClick = { onSelect(value) },
                        )
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = value == selected, onClick = null)
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
internal fun LyricsSupplementalTextSheet(
    translationEnabled: Boolean,
    transliterationEnabled: Boolean,
    onTranslationEnabledChange: (Boolean) -> Unit,
    onTransliterationEnabledChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ResonoteBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
            ResonoteBottomSheetHeader(
                title = stringResource(R.string.feature_settings_impl_lyrics_supplemental),
            )
            Spacer(Modifier.height(8.dp))
            SettingsSwitchRow(
                title = stringResource(R.string.feature_settings_impl_lyrics_translation),
                checked = translationEnabled,
                onCheckedChange = onTranslationEnabledChange,
                switchTestTag = "lyrics-translation-switch",
            )
            SettingsSwitchRow(
                title = stringResource(R.string.feature_settings_impl_lyrics_transliteration),
                checked = transliterationEnabled,
                onCheckedChange = onTransliterationEnabledChange,
                switchTestTag = "lyrics-transliteration-switch",
            )
        }
    }
}

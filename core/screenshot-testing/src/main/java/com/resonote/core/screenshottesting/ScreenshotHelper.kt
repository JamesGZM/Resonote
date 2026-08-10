/*
 * Copyright 2023 The Android Open Source Project
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

@file:OptIn(com.github.takahirom.roborazzi.ExperimentalRoborazziApi::class)

package com.resonote.core.screenshottesting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.checkRoboAccessibility
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode

val DefaultRoborazziOptions = RoborazziOptions(
    compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0f),
    recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5),
)

fun ComposeContentTestRule.captureResonoteThemes(
    group: String,
    name: String,
    checkAccessibility: Boolean = true,
    content: @Composable (ResonoteThemeMode) -> Unit,
) {
    var themeMode by mutableStateOf(ResonoteThemeMode.LIGHT)
    setContent {
        ResonoteTheme(themeMode = themeMode) {
            content(themeMode)
        }
    }

    listOf(
        ResonoteThemeMode.LIGHT,
        ResonoteThemeMode.DARK,
        ResonoteThemeMode.AMOLED,
    ).forEach { mode ->
        themeMode = mode
        waitForIdle()
        if (checkAccessibility) onRoot().checkRoboAccessibility()
        onRoot().captureRoboImage(
            filePath = "src/test/screenshots/$group/${name}_${mode.name.lowercase()}.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }
}

fun ComposeContentTestRule.captureResonoteFontScale(
    group: String,
    name: String,
    fontScale: Float = 2f,
    content: @Composable () -> Unit,
) {
    setContent {
        DeviceConfigurationOverride(
            override = DeviceConfigurationOverride.FontScale(fontScale),
        ) {
            ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT, content = content)
        }
    }
    waitForIdle()
    onRoot().captureRoboImage(
        filePath = "src/test/screenshots/$group/${name}_fontScale${fontScale.toInt()}.png",
        roborazziOptions = DefaultRoborazziOptions,
    )
}

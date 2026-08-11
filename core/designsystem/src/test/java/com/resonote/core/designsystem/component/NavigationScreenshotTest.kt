package com.resonote.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.github.takahirom.roborazzi.captureRoboImage
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.designsystem.tokens.ResonoteTokens
import com.resonote.core.screenshottesting.DefaultRoborazziOptions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w1200dp-h1200dp-420dpi")
@OptIn(ExperimentalMaterial3Api::class)
class NavigationScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun navigation_windowMatrix() {
        captureAdaptiveSeries(
            listOf(
                ScreenshotCase("compactWidth_compactHeight", 400.dp, 400.dp),
                ScreenshotCase("compactWidth_mediumHeight", 400.dp, 700.dp),
                ScreenshotCase("compactWidth_expandedHeight", 400.dp, 1000.dp),
                ScreenshotCase("mediumWidth_compactHeight", 700.dp, 400.dp),
                ScreenshotCase("mediumWidth_mediumHeight", 700.dp, 700.dp),
                ScreenshotCase("mediumWidth_expandedHeight", 700.dp, 1000.dp),
                ScreenshotCase("expandedWidth_compactHeight", 1000.dp, 400.dp),
                ScreenshotCase("expandedWidth_mediumHeight", 1000.dp, 700.dp),
                ScreenshotCase("expandedWidth_expandedHeight", 1000.dp, 1000.dp),
            ),
        )
    }

    @Test
    fun navigation_compact_multipleThemes() {
        captureAdaptiveSeries(
            listOf(
                ScreenshotCase("compact_light", 400.dp, 800.dp, ResonoteThemeMode.LIGHT),
                ScreenshotCase("compact_dark", 400.dp, 800.dp, ResonoteThemeMode.DARK),
                ScreenshotCase("compact_amoled", 400.dp, 800.dp, ResonoteThemeMode.AMOLED),
            ),
        )
    }

    @Test
    fun navigation_compact_fontScale200() {
        captureAdaptiveSeries(
            listOf(
                ScreenshotCase(
                    name = "compact_longLabels_fontScale200",
                    width = 400.dp,
                    height = 800.dp,
                    fontScale = 2f,
                    labels = listOf("音乐资料库", "Component library", "Listening history"),
                ),
            ),
        )
    }

    @Test
    fun topAppBar_variants() {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(600.dp, 360.dp)),
            ) {
                ResonoteTheme {
                    Column {
                        ResonoteTopAppBar(title = { Text("Title only") })
                        ResonoteTopAppBar(
                            title = { Text("Navigation and actions") },
                            navigationIcon = { TestAction(label = "Back") },
                            actions = {
                                TestAction(label = "Info")
                                TestAction(label = "Settings")
                            },
                        )
                    }
                }
            }
        }
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/TopAppBar/TopAppBarVariants_light.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    @Test
    fun topAppBar_scrolledContainer() {
        composeRule.setContent {
            DeviceConfigurationOverride(
                override = DeviceConfigurationOverride.ForcedSize(DpSize(600.dp, 180.dp)),
            ) {
                ResonoteTheme {
                    val scrolledBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
                        state = androidx.compose.material3.rememberTopAppBarState(
                            initialHeightOffsetLimit = -64f,
                            initialHeightOffset = -64f,
                            initialContentOffset = -64f,
                        ),
                    )
                    ResonoteTopAppBar(
                        title = { Text("Scrolled container") },
                        scrollBehavior = scrolledBehavior,
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/TopAppBar/TopAppBarScrolled_light.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }

    @Suppress("DEPRECATION")
    private fun captureAdaptiveSeries(cases: List<ScreenshotCase>) {
        var currentCase by mutableStateOf(cases.first())
        composeRule.setContent {
            val configuration = DeviceConfigurationOverride.ForcedSize(
                DpSize(currentCase.width, currentCase.height),
            ) then DeviceConfigurationOverride.FontScale(currentCase.fontScale)
            DeviceConfigurationOverride(override = configuration) {
                ResonoteTheme(themeMode = currentCase.themeMode) {
                    AdaptiveGallery(
                        labels = currentCase.labels,
                        adaptiveInfo = WindowAdaptiveInfo(
                            windowSizeClass = WindowSizeClass.compute(
                                currentCase.width.value,
                                currentCase.height.value,
                            ),
                            windowPosture = Posture(),
                        ),
                    )
                }
            }
        }
        cases.forEach { case ->
            currentCase = case
            composeRule.waitForIdle()
            composeRule.onRoot().captureRoboImage(
                filePath = "src/test/screenshots/Navigation/${case.name}.png",
                roborazziOptions = DefaultRoborazziOptions,
            )
        }
    }
}

private data class ScreenshotCase(
    val name: String,
    val width: Dp,
    val height: Dp,
    val themeMode: ResonoteThemeMode = ResonoteThemeMode.LIGHT,
    val fontScale: Float = 1f,
    val labels: List<String> = listOf("Foundation", "Components", "Music"),
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AdaptiveGallery(labels: List<String>, adaptiveInfo: WindowAdaptiveInfo) {
    ResonoteNavigationSuiteScaffold(
        navigationSuiteItems = {
            labels.forEachIndexed { index, label ->
                item(
                    selected = index == 0,
                    onClick = {},
                    icon = { TestGlyph(filled = false) },
                    selectedIcon = { TestGlyph(filled = true) },
                    label = { Text(label) },
                )
            }
        },
        windowAdaptiveInfo = adaptiveInfo,
    ) {
        Scaffold(
            topBar = { ResonoteTopAppBar(title = { Text("Resonote Catalog") }) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .padding(ResonoteTokens.spacing.space4),
                verticalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space2),
            ) {
                Text("Adaptive navigation", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "The same destinations and selected state remain available as the window changes.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TestAction(label: String) {
    ResonoteIconButton(
        label = label,
        onClick = {},
        icon = { TestGlyph(filled = false) },
    )
}

@Composable
private fun TestGlyph(filled: Boolean) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .then(
                if (filled) {
                    Modifier.background(LocalContentColor.current, RectangleShape)
                } else {
                    Modifier.background(LocalContentColor.current.copy(alpha = 0.45f), RectangleShape)
                },
            ),
    )
}

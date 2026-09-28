package com.resonote.feature.vip.impl

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import com.resonote.core.designsystem.theme.ResonoteTheme
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.VipCheckInRecord
import com.resonote.core.model.VipCheckInResult
import com.resonote.core.screenshottesting.DefaultRoborazziOptions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "zh-rCN-w390dp-h844dp-420dpi")
class VipCalendarScreenshotTest {
    @get:Rule val composeRule = createComposeRule()
    private val day = LocalDate.of(2026, 9, 28)

    @Test fun calendarShowsConfirmedDatesAndManualAction() {
        var submitted = false
        show(
            VipCalendarState(
                month = YearMonth.from(day),
                today = day,
                loading = false,
                records = listOf(VipCheckInRecord(day, true, false)),
            ),
            onSignIn = { submitted = true },
        )
        composeRule.onNodeWithText("本月已签 1 天").assertIsDisplayed()
        capture("current")
        composeRule.onNodeWithText("今日签到并升级").performScrollTo().performClick()
        assertThat(submitted).isTrue()
    }

    @Test fun upgradeFailureRemainsVisible() {
        show(
            VipCalendarState(
                month = YearMonth.from(day),
                today = day,
                loading = false,
                result = VipCheckInResult(day, true, false, ContentFailure.ServiceRejected),
            ),
        )
        composeRule.onNodeWithText("畅听 VIP 已领取，概念版升级失败").performScrollTo().assertIsDisplayed()
        capture("partial")
    }

    @Test fun leapMonthAndPreviousMonthAction() {
        var previous = false
        show(VipCalendarState(month = YearMonth.of(2024, 2), today = day, loading = false), onPrevious = {
            previous =
                true
        })
        composeRule.onNodeWithContentDescription("2024-02-29，无记录").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("上个月").performClick()
        assertThat(previous).isTrue()
        capture("history")
    }

    private fun show(state: VipCalendarState, onSignIn: () -> Unit = {}, onPrevious: () -> Unit = {}) {
        composeRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 844.dp))) {
                ResonoteTheme(themeMode = ResonoteThemeMode.LIGHT) {
                    VipCalendarScreen(state, {}, {}, onPrevious, {}, {}, onSignIn, {})
                }
            }
        }
    }
    private fun capture(name: String) {
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(
            "src/test/screenshots/VipCalendar/$name.png",
            roborazziOptions = DefaultRoborazziOptions,
        )
    }
}

package com.resonote.feature.video.impl

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VideoBackBehaviorTest {
    @Test
    fun fullscreenBackOnlyExitsFullscreen() {
        var fullscreenExitCount = 0
        var navigationBackCount = 0

        dispatchVideoBack(
            fullscreen = true,
            onExitFullscreen = { fullscreenExitCount += 1 },
            onNavigateBack = { navigationBackCount += 1 },
        )

        assertThat(fullscreenExitCount).isEqualTo(1)
        assertThat(navigationBackCount).isEqualTo(0)
    }

    @Test
    fun embeddedBackNavigatesAway() {
        var fullscreenExitCount = 0
        var navigationBackCount = 0

        dispatchVideoBack(
            fullscreen = false,
            onExitFullscreen = { fullscreenExitCount += 1 },
            onNavigateBack = { navigationBackCount += 1 },
        )

        assertThat(fullscreenExitCount).isEqualTo(0)
        assertThat(navigationBackCount).isEqualTo(1)
    }
}

package com.resonote.core.designsystem.component

import com.google.common.truth.Truth.assertThat
import com.resonote.core.model.AudioQuality
import org.junit.Test

class AudioQualityBadgeTest {
    @Test
    fun audioQualitiesUseCompactMusicBadgeLabels() {
        assertThat(AudioQuality.Standard.compactBadgeLabel()).isNull()
        assertThat(AudioQuality.HighQuality.compactBadgeLabel()).isEqualTo("HQ")
        assertThat(AudioQuality.HighResolution.compactBadgeLabel()).isEqualTo("HR")
        assertThat(AudioQuality.Lossless.compactBadgeLabel()).isEqualTo("SQ")
    }
}

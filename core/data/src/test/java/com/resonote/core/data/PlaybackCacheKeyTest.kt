package com.resonote.core.data

import com.google.common.truth.Truth.assertThat
import com.resonote.core.model.OnlinePlaybackQuality
import org.junit.Test

class PlaybackCacheKeyTest {
    @Test
    fun onlineCacheKeysSeparateQualityAndPreviewVariants() {
        val standard = onlinePlaybackCacheKey("song", OnlinePlaybackQuality.Standard, isPreview = false)
        val highQuality = onlinePlaybackCacheKey("song", OnlinePlaybackQuality.HighQuality, isPreview = false)
        val preview = onlinePlaybackCacheKey("song", OnlinePlaybackQuality.Standard, isPreview = true)

        assertThat(listOf(standard, highQuality, preview).distinct()).hasSize(3)
    }
}

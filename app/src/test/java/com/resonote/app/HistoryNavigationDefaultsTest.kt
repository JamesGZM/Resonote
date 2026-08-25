package com.resonote.app

import com.google.common.truth.Truth.assertThat
import com.resonote.feature.history.api.HistoryNavKey
import com.resonote.feature.history.api.HistoryTab
import org.junit.Test

class HistoryNavigationDefaultsTest {
    @Test
    fun recentHistoryDefaultsToOnlineTab() {
        assertThat(HistoryNavKey().initialTab).isEqualTo(HistoryTab.Online)
    }
}

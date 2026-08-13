package com.resonote.app

import com.google.common.truth.Truth.assertThat
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.PlaybackUnavailableReason
import com.resonote.core.navigation.TabsShellNavKey
import com.resonote.core.playback.PlaybackIssue
import com.resonote.feature.settings.api.SettingsNavKey
import org.junit.Test

class PlaybackPresentationTest {
    @Test
    fun playbackFailuresAlwaysMapToVisibleMessages() {
        assertThat(PlaybackIssue.Unavailable(PlaybackUnavailableReason.Copyright).messageRes())
            .isEqualTo(R.string.playback_error_copyright)
        assertThat(PlaybackIssue.Unavailable(PlaybackUnavailableReason.Vip).messageRes())
            .isEqualTo(R.string.playback_error_vip)
        assertThat(PlaybackIssue.Unavailable(PlaybackUnavailableReason.Cloud).messageRes())
            .isEqualTo(R.string.playback_error_cloud)
        assertThat(PlaybackIssue.Unavailable(PlaybackUnavailableReason.Local).messageRes())
            .isEqualTo(R.string.playback_error_local)
        assertThat(PlaybackIssue.SourceFailure(ContentFailure.Network).messageRes())
            .isEqualTo(R.string.playback_error_source)
        assertThat(PlaybackIssue.PlayerFailure("decoder").messageRes())
            .isEqualTo(R.string.playback_error_player)
    }

    @Test
    fun systemNavigationColorFollowsTopLevelChrome() {
        assertThat(TabsShellNavKey.hasPrimaryNavigation()).isTrue()
        assertThat(SettingsNavKey.hasPrimaryNavigation()).isFalse()
        assertThat(null.hasPrimaryNavigation()).isFalse()
    }
}

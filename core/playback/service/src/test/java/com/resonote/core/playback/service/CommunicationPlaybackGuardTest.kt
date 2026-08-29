@file:androidx.media3.common.util.UnstableApi

package com.resonote.core.playback.service

import android.content.Context
import android.media.AudioManager
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.test.utils.FakePlayer
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.TestScope
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CommunicationPlaybackGuardTest {
    @Test
    fun communicationModesPausePlayback() {
        communicationModes().forEach { mode ->
            val player = playingPlayer()
            guard(player).pauseForAudioMode(mode)
            shadowOf(android.os.Looper.getMainLooper()).idle()

            assertThat(player.playWhenReady).isFalse()
        }
    }

    @Test
    fun normalModeDoesNotPausePlayback() {
        val player = playingPlayer()

        guard(player).pauseForAudioMode(AudioManager.MODE_NORMAL)
        shadowOf(android.os.Looper.getMainLooper()).idle()

        assertThat(player.playWhenReady).isTrue()
    }

    @Test
    fun communicationPauseDoesNotResumeWhenModeReturnsToNormal() {
        val player = playingPlayer()
        val guard = guard(player)

        guard.pauseForAudioMode(AudioManager.MODE_IN_COMMUNICATION)
        shadowOf(android.os.Looper.getMainLooper()).idle()
        guard.pauseForAudioMode(AudioManager.MODE_NORMAL)
        shadowOf(android.os.Looper.getMainLooper()).idle()

        assertThat(player.playWhenReady).isFalse()
    }

    private fun guard(player: Player): CommunicationPlaybackGuard {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return CommunicationPlaybackGuard(
            audioManager = context.getSystemService(AudioManager::class.java),
            player = player,
            scope = TestScope(),
            mainExecutor = context.mainExecutor,
            sdkInt = 35,
        )
    }

    private fun playingPlayer(): FakePlayer = FakePlayer().apply {
        setMediaItem(MediaItem.fromUri("https://example.com/song.mp3"))
        setPlaybackState(Player.STATE_READY)
        play()
        shadowOf(android.os.Looper.getMainLooper()).idle()
    }

    private fun communicationModes(): List<Int> = listOf(
        AudioManager.MODE_RINGTONE,
        AudioManager.MODE_IN_CALL,
        AudioManager.MODE_IN_COMMUNICATION,
        AudioManager.MODE_CALL_SCREENING,
        AudioManager.MODE_CALL_REDIRECT,
        AudioManager.MODE_COMMUNICATION_REDIRECT,
    )
}

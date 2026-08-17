@file:androidx.media3.common.util.UnstableApi

package com.resonote.core.playback.service

import androidx.media3.common.Player
import androidx.media3.test.utils.FakePlayer
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class QueueAwarePlayerTest {
    @Before
    fun setUp() {
        shadowOf(android.os.Looper.getMainLooper()).idle()
    }

    @Test
    fun exposesStandardQueueCommandsForNotificationsAndExternalControllers() {
        val player = QueueAwarePlayer(FakePlayer(), router())

        assertThat(player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT)).isTrue()
        assertThat(player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)).isTrue()
        assertThat(player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS)).isTrue()
        assertThat(player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)).isTrue()
        assertThat(player.availableCommands.contains(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)).isTrue()
    }

    @Test
    fun allStandardQueueCommandsRouteExactlyOnce() {
        var nextCalls = 0
        var previousCalls = 0
        val router = PlaybackQueueCommandRouter().apply {
            bind(onNext = { nextCalls++ }, onPrevious = { previousCalls++ })
        }
        val player = QueueAwarePlayer(
            FakePlayer(),
            router,
        )

        player.seekToNext()
        player.seekToNextMediaItem()
        player.seekToPrevious()
        player.seekToPreviousMediaItem()

        assertThat(nextCalls).isEqualTo(2)
        assertThat(previousCalls).isEqualTo(2)
    }

    @Test
    fun preservesUnderlyingPlayerMediaItemAndPlaybackState() {
        val underlyingPlayer = FakePlayer()
        val player = QueueAwarePlayer(underlyingPlayer, router())
        val mediaItem = androidx.media3.common.MediaItem.Builder()
            .setMediaId("song-id")
            .setUri("https://example.com/song.mp3")
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle("Song title")
                    .setArtist("Artist")
                    .build(),
            )
            .build()

        underlyingPlayer.setMediaItem(mediaItem)
        underlyingPlayer.setPlaybackState(Player.STATE_READY)
        shadowOf(android.os.Looper.getMainLooper()).idle()

        assertThat(player.currentMediaItem?.mediaId).isEqualTo("song-id")
        assertThat(player.mediaMetadata.title.toString()).isEqualTo("Song title")
        assertThat(player.mediaMetadata.artist.toString()).isEqualTo("Artist")
        assertThat(player.playbackState).isEqualTo(Player.STATE_READY)
    }

    private fun router() = PlaybackQueueCommandRouter().apply {
        bind(onNext = {}, onPrevious = {})
    }
}

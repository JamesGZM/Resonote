@file:androidx.media3.common.util.UnstableApi

package com.resonote.core.playback.service

import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.Player
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

internal class QueueAwarePlayer(player: Player, private val commandRouter: PlaybackQueueCommandRouter) :
    ForwardingSimpleBasePlayer(player) {
    override fun getState(): State {
        val state = super.getState()
        return state.buildUpon()
            .setAvailableCommands(withQueueCommands(state.availableCommands))
            .build()
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> =
        when (seekCommand) {
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            -> {
                commandRouter.next()
                Futures.immediateVoidFuture()
            }

            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            -> {
                commandRouter.previous()
                Futures.immediateVoidFuture()
            }

            else -> super.handleSeek(mediaItemIndex, positionMs, seekCommand)
        }

    private companion object {
        val QUEUE_COMMANDS = setOf(
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
        )

        fun withQueueCommands(commands: Player.Commands): Player.Commands = commands.buildUpon()
            .addAll(*QUEUE_COMMANDS.toIntArray())
            .build()
    }
}

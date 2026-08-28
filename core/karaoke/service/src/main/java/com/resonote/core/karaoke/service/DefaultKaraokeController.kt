package com.resonote.core.karaoke.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.resonote.core.karaoke.KaraokeController
import com.resonote.core.playback.PlaybackItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultKaraokeController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val runtime: KaraokeSessionRuntime,
) : KaraokeController {
    override val state = runtime.state

    override fun enable(item: PlaybackItem) = runtime.enable(item)

    override fun disable() {
        runtime.disable()
        context.stopService(Intent(context, KaraokeRecordingService::class.java))
    }

    override fun start() {
        ContextCompat.startForegroundService(
            context,
            Intent(context, KaraokeRecordingService::class.java).setAction(KaraokeRecordingService.ACTION_START),
        )
        runtime.start()
    }

    override fun selectSource(sourceMode: com.resonote.core.model.KaraokeSourceMode) = runtime.selectSource(sourceMode)

    override fun pause() = runtime.pause()
    override fun resume() = runtime.resume()
    override fun previous() = runtime.previous()
    override fun next() = runtime.next()
    override fun stopAndSave() {
        runtime.stopAndSave()
        context.stopService(Intent(context, KaraokeRecordingService::class.java))
    }

    override fun acknowledgeFailure() = runtime.acknowledgeFailure()
}

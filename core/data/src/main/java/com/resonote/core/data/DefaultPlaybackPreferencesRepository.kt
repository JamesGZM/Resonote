package com.resonote.core.data

import com.resonote.core.datastore.PlaybackPreferencesStorage
import com.resonote.core.model.OnlinePlaybackQuality
import com.resonote.core.model.PlaybackSpeed
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultPlaybackPreferencesRepository @Inject constructor(
    private val storage: PlaybackPreferencesStorage,
) : PlaybackPreferencesRepository {
    override val playbackSpeed = storage.playbackSpeedPercent.map(PlaybackSpeed::fromPercent)
    override val onlinePlaybackQuality = storage.onlinePlaybackQuality.map { stored ->
        OnlinePlaybackQuality.entries.firstOrNull { it.name == stored } ?: OnlinePlaybackQuality.Standard
    }

    override suspend fun setPlaybackSpeed(speed: PlaybackSpeed) {
        storage.setPlaybackSpeedPercent(speed.percent)
    }

    override suspend fun setOnlinePlaybackQuality(quality: OnlinePlaybackQuality) {
        storage.setOnlinePlaybackQuality(quality.name)
    }
}

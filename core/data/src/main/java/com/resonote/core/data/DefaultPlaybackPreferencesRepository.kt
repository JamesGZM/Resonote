package com.resonote.core.data

import com.resonote.core.datastore.PlaybackPreferencesStorage
import com.resonote.core.model.PlaybackSpeed
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

@Singleton
internal class DefaultPlaybackPreferencesRepository @Inject constructor(
    private val storage: PlaybackPreferencesStorage,
) : PlaybackPreferencesRepository {
    override val playbackSpeed = storage.playbackSpeedPercent.map(PlaybackSpeed::fromPercent)

    override suspend fun setPlaybackSpeed(speed: PlaybackSpeed) {
        storage.setPlaybackSpeedPercent(speed.percent)
    }
}

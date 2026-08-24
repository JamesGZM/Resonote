package com.resonote.core.playback.service

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PlaybackQueueCommandRouter @Inject constructor() {
    private var onNext: (() -> Unit)? = null
    private var onPrevious: (() -> Unit)? = null

    fun bind(onNext: () -> Unit, onPrevious: () -> Unit) {
        this.onNext = onNext
        this.onPrevious = onPrevious
    }

    fun next() {
        onNext?.invoke()
    }

    fun previous() {
        onPrevious?.invoke()
    }
}

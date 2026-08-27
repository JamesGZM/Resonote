package com.resonote.core.playback

interface DesktopLyricsController {
    fun show()

    fun hide()

    fun refresh()

    fun resetPosition()

    fun restoreIfEnabled()
}

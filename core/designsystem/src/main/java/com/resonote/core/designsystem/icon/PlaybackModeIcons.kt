package com.resonote.core.designsystem.icon

import androidx.annotation.DrawableRes
import com.resonote.core.designsystem.R
import com.resonote.core.model.PlaybackMode

@DrawableRes
fun PlaybackMode.iconResource(): Int = when (this) {
    PlaybackMode.ListLoop -> R.drawable.core_designsystem_ic_repeat
    PlaybackMode.Shuffle -> R.drawable.core_designsystem_ic_shuffle
    PlaybackMode.SingleLoop -> R.drawable.core_designsystem_ic_repeat_one
    PlaybackMode.Sequential -> R.drawable.core_designsystem_ic_playlist_play
}

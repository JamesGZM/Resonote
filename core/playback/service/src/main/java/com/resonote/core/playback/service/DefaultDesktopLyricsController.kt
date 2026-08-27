package com.resonote.core.playback.service

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.resonote.core.data.LyricsPreferencesRepository
import com.resonote.core.playback.DesktopLyricsController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultDesktopLyricsController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesRepository: LyricsPreferencesRepository,
) : DesktopLyricsController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun show() {
        if (!Settings.canDrawOverlays(context)) return
        ContextCompat.startForegroundService(context, serviceIntent(DesktopLyricsService.ACTION_SHOW))
    }

    override fun hide() {
        context.stopService(serviceIntent())
    }

    override fun refresh() {
        scope.launch {
            if (
                preferencesRepository.preferences.first().desktopLyricsEnabled &&
                Settings.canDrawOverlays(context)
            ) {
                ContextCompat.startForegroundService(context, serviceIntent(DesktopLyricsService.ACTION_REFRESH))
            }
        }
    }

    override fun resetPosition() {
        scope.launch {
            if (
                preferencesRepository.preferences.first().desktopLyricsEnabled &&
                Settings.canDrawOverlays(context)
            ) {
                ContextCompat.startForegroundService(context, serviceIntent(DesktopLyricsService.ACTION_RESET_POSITION))
            }
        }
    }

    override fun restoreIfEnabled() {
        scope.launch {
            val preferences = preferencesRepository.preferences.first()
            if (!preferences.desktopLyricsEnabled) return@launch
            if (Settings.canDrawOverlays(context)) {
                show()
            } else {
                preferencesRepository.setPreferences(preferences.copy(desktopLyricsEnabled = false))
            }
        }
    }

    private fun serviceIntent(action: String? = null) = Intent(context, DesktopLyricsService::class.java).apply {
        this.action = action
    }
}

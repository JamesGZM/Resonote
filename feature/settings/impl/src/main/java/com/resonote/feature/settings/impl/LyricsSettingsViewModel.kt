package com.resonote.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.LyricsPreferencesRepository
import com.resonote.core.model.LyricsPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LyricsSettingsViewModel @Inject constructor(private val repository: LyricsPreferencesRepository) : ViewModel() {
    val preferences: StateFlow<LyricsPreferences> = repository.preferences.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        LyricsPreferences(),
    )

    fun update(value: LyricsPreferences) {
        viewModelScope.launch { repository.setPreferences(value) }
    }
}

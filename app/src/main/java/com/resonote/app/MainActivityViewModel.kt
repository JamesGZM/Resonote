package com.resonote.app

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.AuthRepository
import com.resonote.core.data.LocalMediaRepository
import com.resonote.core.data.ThemePreferencesRepository
import com.resonote.core.model.AuthState
import com.resonote.core.model.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@HiltViewModel
internal class MainActivityViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    localMediaRepository: LocalMediaRepository,
    themePreferencesRepository: ThemePreferencesRepository,
) : ViewModel() {
    private val mutableExternalImportRequests = MutableStateFlow<List<ExternalLocalImportRequest>>(emptyList())
    val externalImportRequests: StateFlow<List<ExternalLocalImportRequest>> =
        mutableExternalImportRequests.asStateFlow()

    private var nextExternalImportRequestId = 0L

    init {
        viewModelScope.launch { localMediaRepository.recoverStorage() }
    }

    val authState: StateFlow<AuthState> =
        authRepository.authState.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AuthState.Anonymous,
        )

    val themePreferences: StateFlow<ThemePreferences> =
        themePreferencesRepository.themePreferences.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemePreferences(),
        )

    fun acknowledgeAuthenticationGate() {
        viewModelScope.launch { authRepository.acknowledgeAuthenticationGate() }
    }

    fun handleExternalImportIntent(intent: Intent, finishTaskOnBack: Boolean): Boolean {
        val uris = ExternalLocalImportIntentParser.parse(intent)
        if (uris.isEmpty()) return false
        val request = ExternalLocalImportRequest(
            id = ++nextExternalImportRequestId,
            uris = uris,
            finishTaskOnBack = finishTaskOnBack,
        )
        mutableExternalImportRequests.update { it + request }
        return true
    }

    fun acknowledgeExternalImportRequest(id: Long) {
        mutableExternalImportRequests.update { requests -> requests.filterNot { it.id == id } }
    }
}

internal data class ExternalLocalImportRequest(
    val id: Long,
    val uris: List<String>,
    val finishTaskOnBack: Boolean,
) {
    init {
        require(id > 0) { "id must be positive" }
        require(uris.isNotEmpty()) { "uris must not be empty" }
        require(uris.none(String::isBlank)) { "uris must not contain blank values" }
    }
}

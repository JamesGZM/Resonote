package com.resonote.feature.library.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resonote.core.data.AuthRepository
import com.resonote.core.data.LibraryRepository
import com.resonote.core.data.UserProfileRepository
import com.resonote.core.model.AuthState
import com.resonote.core.model.CollectionLoadResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

@HiltViewModel
class MyViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: UserProfileRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<MyUiState>(MyUiState.CheckingAccount)
    val uiState: StateFlow<MyUiState> = mutableUiState.asStateFlow()

    private var activeUserId: String? = null
    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.authState.collectLatest { authState ->
                refreshJob?.cancel()
                refreshJob = null
                when (authState) {
                    AuthState.Anonymous, is AuthState.AuthenticationRequired -> {
                        activeUserId = null
                        mutableUiState.value = MyUiState.Anonymous
                    }
                    is AuthState.Authenticated -> {
                        activeUserId = authState.userId
                        mutableUiState.value = MyUiState.Authenticated()
                        loadAll(authState.userId)
                    }
                }
            }
        }
    }

    fun refresh() {
        val userId = activeUserId ?: return
        if (refreshJob?.isActive == true) return
        mutableUiState.update { state ->
            (state as? MyUiState.Authenticated)?.copy(isRefreshing = true) ?: state
        }
        refreshJob = viewModelScope.launch {
            loadAll(userId)
            updateAuthenticated(userId) { it.copy(isRefreshing = false) }
        }
    }

    fun retryProfile() {
        val userId = activeUserId ?: return
        updateAuthenticated(userId) { it.copy(profile = MySectionState.Loading) }
        viewModelScope.launch { loadProfile(userId) }
    }

    fun retryPlaylists() {
        val userId = activeUserId ?: return
        updateAuthenticated(userId) { it.copy(playlists = MySectionState.Loading) }
        viewModelScope.launch { loadPlaylists(userId) }
    }

    private suspend fun loadAll(userId: String) = supervisorScope {
        launch { loadProfile(userId) }
        launch { loadPlaylists(userId) }
    }

    private suspend fun loadProfile(userId: String) {
        val section = when (val result = profileRepository.loadProfile()) {
            is CollectionLoadResult.Available -> MySectionState.Available(result.value)
            is CollectionLoadResult.Failed -> MySectionState.Failed(result.failure)
        }
        updateAuthenticated(userId) { it.copy(profile = section) }
    }

    private suspend fun loadPlaylists(userId: String) {
        val section = when (val result = libraryRepository.loadPlaylists()) {
            is CollectionLoadResult.Available -> MySectionState.Available(result.value)
            is CollectionLoadResult.Failed -> MySectionState.Failed(result.failure)
        }
        updateAuthenticated(userId) { it.copy(playlists = section) }
    }

    private inline fun updateAuthenticated(
        userId: String,
        transform: (MyUiState.Authenticated) -> MyUiState.Authenticated,
    ) {
        if (activeUserId != userId) return
        mutableUiState.update { state ->
            val authenticated = state as? MyUiState.Authenticated ?: return@update state
            transform(authenticated)
        }
    }
}

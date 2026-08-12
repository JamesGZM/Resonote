package com.resonote.feature.library.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.AuthRepository
import com.resonote.core.data.LibraryRepository
import com.resonote.core.data.UserProfileRepository
import com.resonote.core.model.AuthFailure
import com.resonote.core.model.AuthState
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.MobileCodeLoginResult
import com.resonote.core.model.PasswordLoginResult
import com.resonote.core.model.PlaylistTrackInput
import com.resonote.core.model.QrLoginCheckResult
import com.resonote.core.model.QrLoginKeyResult
import com.resonote.core.model.SendMobileCodeResult
import com.resonote.core.model.UserPlaylist
import com.resonote.core.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun anonymousAccountNeverRequestsAccountScopedData() = runTest(dispatcher) {
        val auth = FakeAuthRepository(AuthState.Anonymous)
        val profile = FakeProfileRepository()
        val library = FakeLibraryRepository()
        val viewModel = MyViewModel(auth, profile, library)

        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(MyUiState.Anonymous)
        assertThat(profile.requests).isEqualTo(0)
        assertThat(library.loadRequests).isEqualTo(0)
    }

    @Test
    fun authenticatedAccountLoadsProfileAndPlaylistsIndependently() = runTest(dispatcher) {
        val profile = FakeProfileRepository(CollectionLoadResult.Available(profile("海岸线")))
        val playlists = listOf(playlist("liked", isMine = true, isLike = true))
        val library = FakeLibraryRepository(CollectionLoadResult.Available(playlists))
        val viewModel = MyViewModel(FakeAuthRepository(AuthState.Authenticated("42")), profile, library)

        advanceUntilIdle()

        val state = viewModel.uiState.value as MyUiState.Authenticated
        assertThat((state.profile as MySectionState.Available).value.nickname).isEqualTo("海岸线")
        assertThat((state.playlists as MySectionState.Available).value).containsExactlyElementsIn(playlists)
        assertThat(profile.requests).isEqualTo(1)
        assertThat(library.loadRequests).isEqualTo(1)
    }

    @Test
    fun playlistFailureDoesNotHideAvailableProfileAndCanRetry() = runTest(dispatcher) {
        val library = FakeLibraryRepository(
            CollectionLoadResult.Failed(ContentFailure.Network),
            CollectionLoadResult.Available(listOf(playlist("created"))),
        )
        val viewModel = MyViewModel(
            FakeAuthRepository(AuthState.Authenticated("42")),
            FakeProfileRepository(CollectionLoadResult.Available(profile("海岸线"))),
            library,
        )
        advanceUntilIdle()

        var state = viewModel.uiState.value as MyUiState.Authenticated
        assertThat(state.profile).isInstanceOf(MySectionState.Available::class.java)
        assertThat(state.playlists).isEqualTo(MySectionState.Failed(ContentFailure.Network))

        viewModel.retryPlaylists()
        advanceUntilIdle()

        state = viewModel.uiState.value as MyUiState.Authenticated
        assertThat((state.playlists as MySectionState.Available).value.single().name).isEqualTo("created")
        assertThat(library.loadRequests).isEqualTo(2)
    }

    @Test
    fun refreshReloadsBothSectionsAndAccountExitClearsTheirData() = runTest(dispatcher) {
        val auth = FakeAuthRepository(AuthState.Authenticated("42"))
        val profile = FakeProfileRepository(
            CollectionLoadResult.Available(profile("旧名字")),
            CollectionLoadResult.Available(profile("新名字")),
        )
        val library = FakeLibraryRepository(
            CollectionLoadResult.Available(listOf(playlist("old"))),
            CollectionLoadResult.Available(listOf(playlist("new"))),
        )
        val viewModel = MyViewModel(auth, profile, library)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        val refreshed = viewModel.uiState.value as MyUiState.Authenticated
        assertThat((refreshed.profile as MySectionState.Available).value.nickname).isEqualTo("新名字")
        assertThat((refreshed.playlists as MySectionState.Available).value.single().name).isEqualTo("new")
        assertThat(refreshed.isRefreshing).isFalse()

        auth.state.value = AuthState.Anonymous
        advanceUntilIdle()
        assertThat(viewModel.uiState.value).isEqualTo(MyUiState.Anonymous)
    }

    private class FakeAuthRepository(initial: AuthState) : AuthRepository {
        val state = MutableStateFlow(initial)
        override val authState = state
        override suspend fun acknowledgeAuthenticationGate() = Unit
        override suspend fun sendMobileCode(mobile: String): SendMobileCodeResult = unused()
        override suspend fun loginWithMobileCode(mobile: String, code: String, selectedUserId: String?): MobileCodeLoginResult = unused()
        override suspend fun loginWithPassword(username: String, password: String): PasswordLoginResult = unused()
        override suspend fun createQrLoginKey(): QrLoginKeyResult = unused()
        override suspend fun checkQrLogin(key: String): QrLoginCheckResult = unused()
    }

    private class FakeProfileRepository(vararg results: CollectionLoadResult<UserProfile>) : UserProfileRepository {
        private val results = ArrayDeque(results.toList().ifEmpty { listOf(CollectionLoadResult.Available(profile("unused"))) })
        var requests = 0

        override suspend fun loadProfile(): CollectionLoadResult<UserProfile> {
            requests++
            return results.removeFirst()
        }
    }

    private class FakeLibraryRepository(vararg results: CollectionLoadResult<List<UserPlaylist>>) : LibraryRepository {
        private val results = ArrayDeque(
            results.toList().ifEmpty { listOf(CollectionLoadResult.Available(emptyList())) },
        )
        var loadRequests = 0

        override suspend fun loadPlaylists(page: Int, pageSize: Int): CollectionLoadResult<List<UserPlaylist>> {
            loadRequests++
            return results.removeFirst()
        }

        override suspend fun createPlaylist(name: String): CollectionLoadResult<String> = unused()
        override suspend fun addTracks(listId: String, tracks: List<PlaylistTrackInput>): CollectionLoadResult<Unit> = unused()
        override suspend fun removeTracks(listId: String, fileIds: List<String>): CollectionLoadResult<Unit> = unused()
    }

    private companion object {
        fun profile(nickname: String) = UserProfile(
            userId = "42",
            nickname = nickname,
            avatarUrl = null,
            backgroundUrl = null,
            signature = "沿着海岸线，收藏每一种声音。",
            fans = 12_600,
            follows = 128,
            listenMinutes = 9_840,
            isVip = true,
            vipLabel = "SVIP",
        )

        fun playlist(name: String, isMine: Boolean = true, isLike: Boolean = false) = UserPlaylist(
            listId = "list-$name",
            globalId = "global-$name",
            name = name,
            coverUrl = null,
            count = 24,
            isMine = isMine,
            isLike = isLike,
        )

        fun <T> unused(): T = error("unused")
    }
}

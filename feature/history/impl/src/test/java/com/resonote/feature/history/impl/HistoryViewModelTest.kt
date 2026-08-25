package com.resonote.feature.history.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.AuthRepository
import com.resonote.core.data.ListeningHistoryRepository
import com.resonote.core.model.AudioQuality
import com.resonote.core.model.AuthState
import com.resonote.core.model.CollectionLoadResult
import com.resonote.core.model.DeviceHistoryItem
import com.resonote.core.model.DeviceHistoryRecord
import com.resonote.core.model.DeviceHistorySource
import com.resonote.core.model.ListeningHistoryPage
import com.resonote.core.model.MobileCodeLoginResult
import com.resonote.core.model.OnlineSong
import com.resonote.core.model.PasswordLoginResult
import com.resonote.core.model.QrLoginCheckResult
import com.resonote.core.model.QrLoginKeyResult
import com.resonote.core.model.SendMobileCodeResult
import com.resonote.feature.history.api.HistoryTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun initialStateDefaultsToOnlineHistory() {
        assertThat(HistoryUiState().selectedTab).isEqualTo(HistoryTab.Online)
    }

    @Test
    fun deviceHistoryIsAvailableWithoutAccount() = runTest(dispatcher) {
        val repository = FakeHistoryRepository(device = MutableStateFlow(listOf(deviceItem("local"))))
        val viewModel = HistoryViewModel(repository, FakeAuthRepository(AuthState.Anonymous))

        viewModel.initialize(HistoryTab.Device)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.accountState).isEqualTo(HistoryAccountState.Anonymous)
        assertThat(viewModel.uiState.value.deviceItems.map { it.record.mediaId }).containsExactly("local")
        assertThat(repository.accountLoads).isEqualTo(0)
    }

    @Test
    fun selectingOnlineWhileAnonymousRequestsLoginWithoutCallingApi() = runTest(dispatcher) {
        val repository = FakeHistoryRepository()
        val viewModel = HistoryViewModel(repository, FakeAuthRepository(AuthState.Anonymous))
        var loginRequests = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.loginRequests.collect { loginRequests++ }
        }
        viewModel.initialize(HistoryTab.Device)
        advanceUntilIdle()

        viewModel.selectTab(HistoryTab.Online)
        advanceUntilIdle()

        assertThat(loginRequests).isEqualTo(1)
        assertThat(repository.accountLoads).isEqualTo(0)
        assertThat(viewModel.uiState.value.online).isEqualTo(OnlineHistoryUiState.NotLoaded)
    }

    @Test
    fun authenticatedOnlineTabLoadsRealRepositoryResult() = runTest(dispatcher) {
        val repository = FakeHistoryRepository(
            accountResults = ArrayDeque(listOf(availablePage("first"))),
        )
        val viewModel = HistoryViewModel(repository, FakeAuthRepository(AuthState.Authenticated("user-a")))
        viewModel.initialize(HistoryTab.Online)

        advanceUntilIdle()

        val online = viewModel.uiState.value.online as OnlineHistoryUiState.Available
        assertThat(online.songs.map(OnlineSong::hash)).containsExactly("first")
        assertThat(repository.accountLoads).isEqualTo(1)
    }

    @Test
    fun reenteringAppliesInitialTabAndRefreshesOnlineHistory() = runTest(dispatcher) {
        val repository = FakeHistoryRepository(
            accountResults = ArrayDeque(listOf(availablePage("first"), availablePage("second"))),
        )
        val viewModel = HistoryViewModel(repository, FakeAuthRepository(AuthState.Authenticated("user-a")))
        viewModel.initialize(HistoryTab.Online)
        advanceUntilIdle()

        viewModel.selectTab(HistoryTab.Device)
        viewModel.initialize(HistoryTab.Online)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedTab).isEqualTo(HistoryTab.Online)
        val online = viewModel.uiState.value.online as OnlineHistoryUiState.Available
        assertThat(online.songs.map(OnlineSong::hash)).containsExactly("second")
        assertThat(repository.accountLoads).isEqualTo(2)
    }

    @Test
    fun accountSwitchDropsPreviousSongsAndReloadsForNewGeneration() = runTest(dispatcher) {
        val auth = FakeAuthRepository(AuthState.Authenticated("user-a"))
        val repository = FakeHistoryRepository(
            accountResults = ArrayDeque(
                listOf(
                    availablePage("first-account"),
                    availablePage("second-account"),
                ),
            ),
        )
        val viewModel = HistoryViewModel(repository, auth)
        viewModel.initialize(HistoryTab.Online)
        advanceUntilIdle()

        auth.state.value = AuthState.Authenticated("user-b")
        advanceUntilIdle()

        val online = viewModel.uiState.value.online as OnlineHistoryUiState.Available
        assertThat(online.songs.map(OnlineSong::hash)).containsExactly("second-account")
        assertThat(repository.accountLoads).isEqualTo(2)
    }

    @Test
    fun onlineHistoryLoadsTheNextCursorAndKeepsExistingSongs() = runTest(dispatcher) {
        val repository = FakeHistoryRepository(
            accountResults = ArrayDeque(
                listOf(
                    availablePage("first", nextCursor = "next", hasMore = true),
                    availablePage("second"),
                ),
            ),
        )
        val viewModel = HistoryViewModel(repository, FakeAuthRepository(AuthState.Authenticated("user-a")))
        viewModel.initialize(HistoryTab.Online)
        advanceUntilIdle()

        viewModel.loadMoreOnline()
        advanceUntilIdle()

        val online = viewModel.uiState.value.online as OnlineHistoryUiState.Available
        assertThat(online.songs.map(OnlineSong::hash)).containsExactly("first", "second").inOrder()
        assertThat(repository.requestedCursors).containsExactly(null, "next").inOrder()
        assertThat(online.hasMore).isFalse()
    }

    @Test
    fun failedDeviceDeletionRemainsVisibleAndDismissible() = runTest(dispatcher) {
        val repository = FakeHistoryRepository(deleteResult = false)
        val viewModel = HistoryViewModel(repository, FakeAuthRepository(AuthState.Anonymous))
        val item = deviceItem("local")
        advanceUntilIdle()

        viewModel.deleteDeviceItem(item)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.mutation).isEqualTo(DeviceHistoryMutation.Failed)

        viewModel.dismissMutationFailure()
        assertThat(viewModel.uiState.value.mutation).isEqualTo(DeviceHistoryMutation.Idle)
    }

    @Test
    fun anonymousCloudReplayRequiresLoginButLocalReplayDoesNot() {
        val state = HistoryUiState(accountState = HistoryAccountState.Anonymous)

        assertThat(requiresLoginForDevicePlayback(state, listOf(deviceItem("cloud", DeviceHistorySource.Cloud)), 0))
            .isTrue()
        assertThat(requiresLoginForDevicePlayback(state, listOf(deviceItem("local")), 0))
            .isFalse()
        assertThat(
            requiresLoginForDevicePlayback(
                state.copy(accountState = HistoryAccountState.Authenticated),
                listOf(deviceItem("cloud", DeviceHistorySource.Cloud)),
                0,
            ),
        ).isFalse()
    }

    private class FakeHistoryRepository(
        val device: MutableStateFlow<List<DeviceHistoryItem>> = MutableStateFlow(emptyList()),
        private val accountResults: ArrayDeque<CollectionLoadResult<ListeningHistoryPage>> = ArrayDeque(),
        private val deleteResult: Boolean = true,
    ) : ListeningHistoryRepository {
        var accountLoads = 0
        val requestedCursors = mutableListOf<String?>()

        override suspend fun loadAccountHistory(cursor: String?): CollectionLoadResult<ListeningHistoryPage> {
            accountLoads++
            requestedCursors += cursor
            return accountResults.removeFirst()
        }

        override fun observeDeviceHistory(): Flow<List<DeviceHistoryItem>> = device

        override suspend fun recordDevicePlayback(record: DeviceHistoryRecord): Boolean = true

        override suspend fun recordAccountPlayback(albumAudioId: String): Boolean = true

        override suspend fun deleteDeviceHistory(record: DeviceHistoryRecord): Boolean = deleteResult

        override suspend fun clearDeviceHistory(): Boolean = true
    }

    private class FakeAuthRepository(initial: AuthState) : AuthRepository {
        val state = MutableStateFlow(initial)
        override val authState: Flow<AuthState> = state
        override suspend fun acknowledgeAuthenticationGate() = Unit
        override suspend fun logout() = Unit
        override suspend fun sendMobileCode(mobile: String): SendMobileCodeResult = error("unused")
        override suspend fun loginWithMobileCode(
            mobile: String,
            code: String,
            selectedUserId: String?,
        ): MobileCodeLoginResult = error("unused")
        override suspend fun loginWithPassword(username: String, password: String): PasswordLoginResult =
            error("unused")
        override suspend fun createQrLoginKey(): QrLoginKeyResult = error("unused")
        override suspend fun checkQrLogin(key: String): QrLoginCheckResult = error("unused")
    }

    private companion object {
        fun availablePage(
            hash: String,
            nextCursor: String? = null,
            hasMore: Boolean = false,
        ): CollectionLoadResult<ListeningHistoryPage> = CollectionLoadResult.Available(
            ListeningHistoryPage(listOf(song(hash)), nextCursor, hasMore),
        )

        fun song(hash: String) = OnlineSong(
            hash = hash,
            title = hash,
            artist = "artist",
            coverUrl = null,
            albumId = null,
            albumAudioId = "audio-$hash",
            durationMillis = 180_000,
            quality = AudioQuality.HighQuality,
            vip = false,
        )

        fun deviceItem(mediaId: String, source: DeviceHistorySource = DeviceHistorySource.Local) = DeviceHistoryItem(
            record = DeviceHistoryRecord(
                source = source,
                mediaId = mediaId,
                title = mediaId,
                artist = "artist",
                albumTitle = "album",
                artworkUri = null,
                durationMillis = 180_000,
                albumAudioId = null,
            ),
            lastPlayedAtEpochMillis = 1_000,
            playCount = 1,
        )
    }
}

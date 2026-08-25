package com.resonote.app

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.AuthRepository
import com.resonote.core.data.LocalMediaRepository
import com.resonote.core.data.ThemePreferencesRepository
import com.resonote.core.model.AuthGateReason
import com.resonote.core.model.AuthState
import com.resonote.core.model.LocalMedia
import com.resonote.core.model.LocalMediaDeleteResult
import com.resonote.core.model.LocalMediaDuplicateAction
import com.resonote.core.model.LocalMediaId
import com.resonote.core.model.LocalMediaImportResult
import com.resonote.core.model.LocalMediaPlaybackSource
import com.resonote.core.model.MobileCodeLoginResult
import com.resonote.core.model.PasswordLoginResult
import com.resonote.core.model.QrLoginCheckResult
import com.resonote.core.model.QrLoginKeyResult
import com.resonote.core.model.SendMobileCodeResult
import com.resonote.core.model.ThemeMode
import com.resonote.core.model.ThemePreferences
import com.resonote.core.navigation.LoginContinuation
import com.resonote.core.navigation.LoginGateNavKey
import com.resonote.core.navigation.TabsShellNavKey
import com.resonote.feature.cloud.api.CloudNavKey
import com.resonote.feature.local.api.LocalMusicNavKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainActivityViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun requiredExpiredAcknowledgedAndAuthenticatedStatesRemainCentralized() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = MainActivityViewModel(repository, FakeLocalMediaRepository(), FakeThemePreferencesRepository())

        repository.state.value = AuthState.AuthenticationRequired(AuthGateReason.Required)
        assertThat(viewModel.authState.value).isEqualTo(AuthState.AuthenticationRequired(AuthGateReason.Required))

        repository.state.value = AuthState.AuthenticationRequired(AuthGateReason.Expired)
        assertThat(viewModel.authState.value).isEqualTo(AuthState.AuthenticationRequired(AuthGateReason.Expired))

        viewModel.acknowledgeAuthenticationGate()
        assertThat(repository.acknowledgements).isEqualTo(1)

        repository.state.value = AuthState.Authenticated("42")
        assertThat(viewModel.authState.value).isEqualTo(AuthState.Authenticated("42"))
    }

    @Test
    fun authenticationGateIsUniqueUpdatesItsReasonAndClearsWithAuthState() {
        val backStack = mutableListOf<NavKey>(TabsShellNavKey)

        backStack.synchronizeAuthenticationGate(AuthState.AuthenticationRequired(AuthGateReason.Required))
        backStack.synchronizeAuthenticationGate(AuthState.AuthenticationRequired(AuthGateReason.Required))
        assertThat(backStack.filterIsInstance<LoginGateNavKey>()).containsExactly(LoginGateNavKey(false))

        backStack.add(TabsShellNavKey)
        backStack.synchronizeAuthenticationGate(AuthState.AuthenticationRequired(AuthGateReason.Expired))
        assertThat(backStack.filterIsInstance<LoginGateNavKey>()).containsExactly(LoginGateNavKey(true))
        assertThat(backStack.last()).isEqualTo(LoginGateNavKey(true))

        backStack.synchronizeAuthenticationGate(AuthState.Anonymous)
        assertThat(backStack.filterIsInstance<LoginGateNavKey>()).isEmpty()
        backStack.synchronizeAuthenticationGate(AuthState.AuthenticationRequired(AuthGateReason.Required))
        backStack.synchronizeAuthenticationGate(AuthState.Authenticated("42"))
        assertThat(backStack.filterIsInstance<LoginGateNavKey>()).isEmpty()
    }

    @Test
    fun cloudNavigationContinuesAfterLoginWithoutDuplicatingDestination() {
        val backStack = mutableListOf<NavKey>(TabsShellNavKey)

        backStack.navigateToCloud(AuthState.Anonymous)
        assertThat(backStack.last()).isEqualTo(
            LoginGateNavKey(sessionExpired = false, continuation = LoginContinuation.Cloud),
        )
        assertThat(backStack.filterIsInstance<CloudNavKey>()).isEmpty()

        backStack.synchronizeAuthenticationGate(AuthState.Authenticated("42"))
        assertThat(backStack.last()).isEqualTo(CloudNavKey)
        assertThat(backStack.filterIsInstance<CloudNavKey>()).hasSize(1)

        backStack.synchronizeAuthenticationGate(AuthState.AuthenticationRequired(AuthGateReason.Expired))
        assertThat(backStack.last()).isEqualTo(LoginGateNavKey(sessionExpired = true))
        backStack.synchronizeAuthenticationGate(AuthState.Authenticated("42"))
        assertThat(backStack.filterIsInstance<CloudNavKey>()).hasSize(1)
        assertThat(backStack.last()).isEqualTo(CloudNavKey)
    }

    @Test
    fun externalIntentParserAcceptsViewSendAndMultipleContentUris() {
        val viewed = Intent(Intent.ACTION_VIEW, Uri.parse("content://media/audio/one"))
        assertThat(ExternalLocalImportIntentParser.parse(viewed))
            .containsExactly("content://media/audio/one")

        val sent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, Uri.parse("content://provider/two"))
            clipData = ClipData.newRawUri("audio", Uri.parse("content://provider/two"))
        }
        assertThat(ExternalLocalImportIntentParser.parse(sent))
            .containsExactly("content://provider/two")

        val multiple = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putParcelableArrayListExtra(
                Intent.EXTRA_STREAM,
                arrayListOf(Uri.parse("content://provider/three"), Uri.parse("content://provider/four")),
            )
        }
        assertThat(ExternalLocalImportIntentParser.parse(multiple)).containsExactly(
            "content://provider/three",
            "content://provider/four",
        ).inOrder()
    }

    @Test
    fun externalIntentParserRejectsUnrelatedActionsAndNonContentUris() {
        assertThat(
            ExternalLocalImportIntentParser.parse(
                Intent(Intent.ACTION_VIEW, Uri.parse("file:///storage/emulated/0/song.mp3")),
            ),
        ).isEmpty()
        assertThat(
            ExternalLocalImportIntentParser.parse(
                Intent(Intent.ACTION_EDIT, Uri.parse("content://provider/song")),
            ),
        ).isEmpty()
    }

    @Test
    fun externalImportRequestsQueueAndAcknowledgeWithoutOverwriting() {
        val viewModel = MainActivityViewModel(
            FakeAuthRepository(),
            FakeLocalMediaRepository(),
            FakeThemePreferencesRepository(),
        )

        assertThat(
            viewModel.handleExternalImportIntent(
                Intent(Intent.ACTION_VIEW, Uri.parse("content://provider/cold")),
                finishTaskOnBack = true,
            ),
        ).isTrue()
        assertThat(
            viewModel.handleExternalImportIntent(
                Intent(Intent.ACTION_SEND).putExtra(
                    Intent.EXTRA_STREAM,
                    Uri.parse("content://provider/foreground"),
                ),
                finishTaskOnBack = false,
            ),
        ).isTrue()

        assertThat(viewModel.externalImportRequests.value).containsExactly(
            ExternalLocalImportRequest(1, listOf("content://provider/cold"), finishTaskOnBack = true),
            ExternalLocalImportRequest(2, listOf("content://provider/foreground"), finishTaskOnBack = false),
        ).inOrder()

        viewModel.acknowledgeExternalImportRequest(1)
        assertThat(viewModel.externalImportRequests.value).containsExactly(
            ExternalLocalImportRequest(2, listOf("content://provider/foreground"), finishTaskOnBack = false),
        )
    }

    @Test
    fun coldExternalLocalBackFinishesTaskWhileForegroundBackRestoresPreviousPage() {
        val coldKey = LocalMusicNavKey(finishTaskOnBack = true)
        val coldStack = mutableListOf<NavKey>(TabsShellNavKey, coldKey)
        assertThat(coldStack.leaveLocalMusic(coldKey)).isTrue()
        assertThat(coldStack).containsExactly(TabsShellNavKey, coldKey).inOrder()

        val foregroundKey = LocalMusicNavKey(finishTaskOnBack = false)
        val foregroundStack = mutableListOf<NavKey>(TabsShellNavKey, foregroundKey)
        assertThat(foregroundStack.leaveLocalMusic(foregroundKey)).isFalse()
        assertThat(foregroundStack).containsExactly(TabsShellNavKey)
    }

    @Test
    fun appStartupTriggersLocalStorageRecovery() {
        val localMedia = FakeLocalMediaRepository()

        MainActivityViewModel(FakeAuthRepository(), localMedia, FakeThemePreferencesRepository())

        assertThat(localMedia.recoveryCalls).isEqualTo(1)
    }

    private class FakeAuthRepository : AuthRepository {
        val state = MutableStateFlow<AuthState>(AuthState.Anonymous)
        var acknowledgements = 0
        override val authState = state

        override suspend fun acknowledgeAuthenticationGate() {
            acknowledgements += 1
            state.value = AuthState.Anonymous
        }

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

    private class FakeLocalMediaRepository : LocalMediaRepository {
        var recoveryCalls = 0

        override suspend fun recoverStorage(): Boolean {
            recoveryCalls += 1
            return true
        }

        override fun observeAll() = flowOf(emptyList<LocalMedia>())

        override suspend fun scanDirectory(treeUri: String) = error("unused")

        override suspend fun importFromUri(
            sourceUri: String,
            duplicateAction: LocalMediaDuplicateAction,
        ): LocalMediaImportResult = error("unused")

        override suspend fun delete(id: LocalMediaId): LocalMediaDeleteResult = error("unused")

        override suspend fun resolvePlaybackSource(id: LocalMediaId): LocalMediaPlaybackSource? = error("unused")
    }

    private class FakeThemePreferencesRepository : ThemePreferencesRepository {
        override val themePreferences = flowOf(ThemePreferences())
        override suspend fun setThemeMode(themeMode: ThemeMode) = Unit
        override suspend fun setDynamicColorEnabled(enabled: Boolean) = Unit
    }
}

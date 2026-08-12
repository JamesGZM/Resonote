package com.resonote.core.data

import com.google.common.truth.Truth.assertThat
import com.resonote.core.model.AuthFailure
import com.resonote.core.model.AuthState
import com.resonote.core.model.MobileCodeLoginResult
import com.resonote.core.model.SendMobileCodeResult
import com.resonote.core.network.ApiNetworkDataSource
import com.resonote.core.network.model.NetworkAccountOption
import com.resonote.core.network.model.NetworkMobileCodeLoginResult
import com.resonote.core.network.model.NetworkHomePlaylist
import com.resonote.core.network.model.NetworkHomeSong
import com.resonote.core.network.model.NetworkRecommendationMode
import com.resonote.core.network.model.NetworkSongSource
import com.resonote.core.network.model.NetworkSearchPage
import com.resonote.core.network.session.ApiSession
import com.resonote.core.network.session.ApiSessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultAuthRepositoryTest {
    @Test
    fun successfulLoginCommitsSessionBeforeReportingAuthenticated() = runTest {
        val session = authenticatedSession()
        val store = FakeSessionStore()
        val repository = DefaultAuthRepository(FakeNetwork(login = NetworkMobileCodeLoginResult.Authenticated(session)), store)

        assertThat(repository.loginWithMobileCode("13800000000", "246810")).isEqualTo(MobileCodeLoginResult.Authenticated)
        assertThat(repository.authState.first()).isEqualTo(AuthState.Authenticated("42"))
    }

    @Test
    fun storageFailureNeverReportsAuthenticated() = runTest {
        val store = FakeSessionStore(failWrites = true)
        val repository = DefaultAuthRepository(FakeNetwork(login = NetworkMobileCodeLoginResult.Authenticated(authenticatedSession())), store)

        assertThat(repository.loginWithMobileCode("13800000000", "246810"))
            .isEqualTo(MobileCodeLoginResult.Failed(AuthFailure.SecureStorage))
        assertThat(repository.authState.first()).isEqualTo(AuthState.Anonymous)
    }

    @Test
    fun multipleAccountsAndSendResultAreMappedWithoutPersisting() = runTest {
        val store = FakeSessionStore()
        val account = NetworkAccountOption("42", "name", null, "1")
        val repository = DefaultAuthRepository(FakeNetwork(NetworkMobileCodeLoginResult.MultipleAccounts(listOf(account))), store)

        assertThat(repository.sendMobileCode("13800000000")).isEqualTo(SendMobileCodeResult.Sent)
        val result = repository.loginWithMobileCode("13800000000", "246810") as MobileCodeLoginResult.MultipleAccounts
        assertThat(result.accounts.single().userId).isEqualTo("42")
        assertThat(store.writes).isEqualTo(0)
    }

    private class FakeNetwork(
        private val login: NetworkMobileCodeLoginResult,
    ) : ApiNetworkDataSource {
        override suspend fun dailyRecommendations(): List<NetworkHomeSong> = error("unused")
        override suspend fun recommendedPlaylists(page: Int, pageSize: Int): List<NetworkHomePlaylist> = error("unused")
        override suspend fun newSongs(page: Int, pageSize: Int): List<NetworkHomeSong> = error("unused")
        override suspend fun radioRecommendations(mode: NetworkRecommendationMode): List<NetworkHomeSong> = error("unused")
        override suspend fun resolveSongSource(hash: String, albumId: String?, albumAudioId: String?): NetworkSongSource = error("unused")
        override suspend fun searchSongs(keywords: String, page: Int, pageSize: Int): NetworkSearchPage = error("unused")
        override suspend fun sendMobileCode(mobile: String) = Unit
        override suspend fun loginWithMobileCode(mobile: String, code: String, selectedUserId: String?) = login
    }

    private class FakeSessionStore(private val failWrites: Boolean = false) : ApiSessionStore {
        private val state = MutableStateFlow<ApiSession?>(null)
        override val session = state
        var writes = 0
        override suspend fun read() = state.value
        override suspend fun write(session: ApiSession) {
            if (failWrites) error("disk full")
            writes += 1
            state.value = session
        }
        override suspend fun clearAuthentication() { state.value = null }
    }

    private fun authenticatedSession() = ApiSession(
        guid = "fixture-guid", mid = "fixture-mid", dev = "fixture-dev", dfid = "fixture-dfid",
        token = "fixture-token", userId = "42",
    )
}

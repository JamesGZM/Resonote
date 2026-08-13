package com.resonote.core.data

import com.google.common.truth.Truth.assertThat
import com.resonote.core.model.AuthFailure
import com.resonote.core.model.AuthGateReason
import com.resonote.core.model.AuthState
import com.resonote.core.model.MobileCodeLoginResult
import com.resonote.core.model.PasswordLoginResult
import com.resonote.core.model.QrLoginCheckResult
import com.resonote.core.model.QrLoginKeyResult
import com.resonote.core.model.SendMobileCodeResult
import com.resonote.core.network.ApiRiskException
import com.resonote.core.network.model.NetworkAccountOption
import com.resonote.core.network.model.NetworkMobileCodeLoginResult
import com.resonote.core.network.model.NetworkPasswordLoginResult
import com.resonote.core.network.model.NetworkQrLoginStatus
import com.resonote.core.network.model.NetworkPlaylistSummary
import com.resonote.core.network.model.NetworkRecommendationMode
import com.resonote.core.network.model.NetworkRanking
import com.resonote.core.network.model.NetworkPlaylistPage
import com.resonote.core.network.model.NetworkSongPage
import com.resonote.core.network.model.NetworkSong
import com.resonote.core.network.model.NetworkSongSource
import com.resonote.core.network.model.NetworkSearchPage
import com.resonote.core.network.session.ApiSession
import com.resonote.core.network.session.ApiSessionStore
import com.resonote.core.network.session.ApiSessionManager
import com.resonote.core.network.protocol.ApiDeviceIdentityFactory
import com.resonote.core.network.risk.ApiRiskChallenge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import java.util.Optional
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultAuthRepositoryTest {
    @Test
    fun authenticationGateIsMappedAndCanBeAcknowledged() = runTest {
        val store = FakeSessionStore(initial = authenticatedSession())
        val manager = ApiSessionManager(Optional.of(store), ApiDeviceIdentityFactory())
        manager.current()
        val repository =
            DefaultAuthRepository(
                FakeNetwork(login = NetworkMobileCodeLoginResult.Authenticated(authenticatedSession())),
                manager,
                RiskChallengeRegistry(),
            )

        manager.reportAuthenticationFailure()

        assertThat(repository.authState.first())
            .isEqualTo(AuthState.AuthenticationRequired(AuthGateReason.Expired))
        repository.acknowledgeAuthenticationGate()
        assertThat(repository.authState.first()).isEqualTo(AuthState.Anonymous)
    }

    @Test
    fun successfulLoginCommitsSessionBeforeReportingAuthenticated() = runTest {
        val session = authenticatedSession()
        val store = FakeSessionStore()
        val repository =
            DefaultAuthRepository(
                FakeNetwork(login = NetworkMobileCodeLoginResult.Authenticated(session)),
                sessionManager(store),
                RiskChallengeRegistry(),
            )

        assertThat(repository.loginWithMobileCode("13800000000", "246810")).isEqualTo(MobileCodeLoginResult.Authenticated)
        assertThat(repository.authState.first()).isEqualTo(AuthState.Authenticated("42"))
    }

    @Test
    fun storageFailureNeverReportsAuthenticated() = runTest {
        val store = FakeSessionStore(failWrites = true)
        val repository =
            DefaultAuthRepository(
                FakeNetwork(login = NetworkMobileCodeLoginResult.Authenticated(authenticatedSession())),
                sessionManager(store),
                RiskChallengeRegistry(),
            )

        assertThat(repository.loginWithMobileCode("13800000000", "246810"))
            .isEqualTo(MobileCodeLoginResult.Failed(AuthFailure.SecureStorage))
        assertThat(repository.authState.first()).isEqualTo(AuthState.Anonymous)
    }

    @Test
    fun multipleAccountsAndSendResultAreMappedWithoutPersisting() = runTest {
        val store = FakeSessionStore()
        val account = NetworkAccountOption("42", "name", null, "1")
        val repository =
            DefaultAuthRepository(
                FakeNetwork(NetworkMobileCodeLoginResult.MultipleAccounts(listOf(account))),
                sessionManager(store),
                RiskChallengeRegistry(),
            )

        assertThat(repository.sendMobileCode("13800000000")).isEqualTo(SendMobileCodeResult.Sent)
        val result = repository.loginWithMobileCode("13800000000", "246810") as MobileCodeLoginResult.MultipleAccounts
        assertThat(result.accounts.single().userId).isEqualTo("42")
        assertThat(store.writes).isEqualTo(0)
    }

    @Test
    fun riskChallengeIsExposedAsOpaqueDomainHandle() = runTest {
        val repository =
            DefaultAuthRepository(
                FakeNetwork(
                    login = NetworkMobileCodeLoginResult.Authenticated(authenticatedSession()),
                    failure =
                        ApiRiskException(
                            ApiRiskChallenge(eventId = "provider-event"),
                            ApiRiskException.Reason.VerificationUnavailable,
                        ),
                ),
                sessionManager(FakeSessionStore()),
                RiskChallengeRegistry(),
            )

        val result = repository.sendMobileCode("13800000000") as SendMobileCodeResult.Failed
        val failure = result.failure as AuthFailure.RiskVerificationRequired

        assertThat(failure.challenge.value).isNotEmpty()
        assertThat(failure.challenge.toString()).doesNotContain("provider-event")
    }

    @Test
    fun passwordLoginCommitsSessionAndMapsMultipleAccounts() = runTest {
        val store = FakeSessionStore()
        val repository =
            DefaultAuthRepository(
                FakeNetwork(
                    login = NetworkMobileCodeLoginResult.Authenticated(authenticatedSession()),
                    passwordLogin = NetworkPasswordLoginResult.Authenticated(authenticatedSession()),
                ),
                sessionManager(store),
                RiskChallengeRegistry(),
            )

        assertThat(repository.loginWithPassword(" 13800000000 ", "password"))
            .isEqualTo(PasswordLoginResult.Authenticated)
        assertThat(store.writes).isEqualTo(1)

        val account = NetworkAccountOption("84", "account", null, null)
        val accountRepository =
            DefaultAuthRepository(
                FakeNetwork(
                    login = NetworkMobileCodeLoginResult.Authenticated(authenticatedSession()),
                    passwordLogin = NetworkPasswordLoginResult.MultipleAccounts(listOf(account)),
                ),
                sessionManager(FakeSessionStore()),
                RiskChallengeRegistry(),
            )
        val multiple = accountRepository.loginWithPassword("account", "password") as PasswordLoginResult.MultipleAccounts
        assertThat(multiple.accounts.single().userId).isEqualTo("84")
    }

    @Test
    fun passwordLoginValidatesInputAndNeverReportsSuccessAfterStorageFailure() = runTest {
        val repository =
            DefaultAuthRepository(
                FakeNetwork(
                    login = NetworkMobileCodeLoginResult.Authenticated(authenticatedSession()),
                    passwordLogin = NetworkPasswordLoginResult.Authenticated(authenticatedSession()),
                ),
                sessionManager(FakeSessionStore(failWrites = true)),
                RiskChallengeRegistry(),
            )

        assertThat(repository.loginWithPassword("", "password"))
            .isEqualTo(PasswordLoginResult.Failed(AuthFailure.InvalidInput))
        assertThat(repository.loginWithPassword("account", ""))
            .isEqualTo(PasswordLoginResult.Failed(AuthFailure.InvalidInput))
        assertThat(repository.loginWithPassword("account", "password"))
            .isEqualTo(PasswordLoginResult.Failed(AuthFailure.SecureStorage))
    }

    @Test
    fun passwordLoginMapsRiskToOpaqueHandle() = runTest {
        val repository =
            DefaultAuthRepository(
                FakeNetwork(
                    login = NetworkMobileCodeLoginResult.Authenticated(authenticatedSession()),
                    failure =
                        ApiRiskException(
                            ApiRiskChallenge(eventId = "password-event"),
                            ApiRiskException.Reason.VerificationUnavailable,
                        ),
                ),
                sessionManager(FakeSessionStore()),
                RiskChallengeRegistry(),
            )

        val result = repository.loginWithPassword("account", "password") as PasswordLoginResult.Failed

        assertThat(result.failure).isInstanceOf(AuthFailure.RiskVerificationRequired::class.java)
    }

    @Test
    fun qrLoginPersistsSessionBeforeReportingSuccess() = runTest {
        val store = FakeSessionStore()
        val repository =
            DefaultAuthRepository(
                FakeNetwork(
                    NetworkMobileCodeLoginResult.Authenticated(authenticatedSession()),
                    qrStatus = NetworkQrLoginStatus.Authenticated(authenticatedSession()),
                ),
                sessionManager(store),
                RiskChallengeRegistry(),
            )

        assertThat(repository.createQrLoginKey()).isEqualTo(QrLoginKeyResult.Ready("qr-key"))
        assertThat(repository.checkQrLogin("qr-key")).isEqualTo(QrLoginCheckResult.Authenticated)
        assertThat(store.writes).isEqualTo(1)
    }

    @Test
    fun qrLoginStorageFailureNeverReportsAuthenticated() = runTest {
        val repository =
            DefaultAuthRepository(
                FakeNetwork(
                    NetworkMobileCodeLoginResult.Authenticated(authenticatedSession()),
                    qrStatus = NetworkQrLoginStatus.Authenticated(authenticatedSession()),
                ),
                sessionManager(FakeSessionStore(failWrites = true)),
                RiskChallengeRegistry(),
            )

        val result = repository.checkQrLogin("qr-key") as QrLoginCheckResult.Failed

        assertThat(result.failure).isEqualTo(AuthFailure.SecureStorage)
    }

    private class FakeNetwork(
        private val login: NetworkMobileCodeLoginResult,
        private val passwordLogin: NetworkPasswordLoginResult? = null,
        private val failure: Throwable? = null,
        private val qrStatus: NetworkQrLoginStatus = NetworkQrLoginStatus.Waiting,
    ) : TestApiNetworkDataSource() {
        override suspend fun dailyRecommendations(): List<NetworkSong> = error("unused")
        override suspend fun recommendedPlaylists(page: Int, pageSize: Int): List<NetworkPlaylistSummary> = error("unused")
        override suspend fun newSongs(page: Int, pageSize: Int): List<NetworkSong> = error("unused")
        override suspend fun radioRecommendations(mode: NetworkRecommendationMode): List<NetworkSong> = error("unused")
        override suspend fun resolveSongSource(hash: String, albumId: String?, albumAudioId: String?, requestedQuality: String): NetworkSongSource = error("unused")
        override suspend fun rankings(): List<NetworkRanking> = error("unused")
        override suspend fun rankingSongs(rankId: String, page: Int, pageSize: Int): NetworkSongPage = error("unused")
        override suspend fun playlistSongs(globalCollectionId: String, page: Int, pageSize: Int): NetworkPlaylistPage = error("unused")
        override suspend fun searchSongs(keywords: String, page: Int, pageSize: Int): NetworkSearchPage = error("unused")
        override suspend fun sendMobileCode(mobile: String) {
            failure?.let { throw it }
        }

        override suspend fun loginWithMobileCode(mobile: String, code: String, selectedUserId: String?): NetworkMobileCodeLoginResult {
            failure?.let { throw it }
            return login
        }

        override suspend fun loginWithPassword(username: String, password: String): NetworkPasswordLoginResult {
            failure?.let { throw it }
            return passwordLogin ?: when (login) {
                is NetworkMobileCodeLoginResult.Authenticated -> NetworkPasswordLoginResult.Authenticated(login.session)
                is NetworkMobileCodeLoginResult.MultipleAccounts -> NetworkPasswordLoginResult.MultipleAccounts(login.accounts)
            }
        }
        override suspend fun userDetail(): com.resonote.core.network.model.NetworkUserDetail = error("unused")
        override suspend fun userVip(): com.resonote.core.network.model.NetworkUserVip = error("unused")
        override suspend fun userPlaylists(page: Int, pageSize: Int): List<com.resonote.core.network.model.NetworkUserPlaylist> = error("unused")
        override suspend fun createPlaylist(name: String): String = error("unused")
        override suspend fun addPlaylistTracks(listId: String, tracks: List<com.resonote.core.network.model.NetworkPlaylistTrackInput>) = error("unused")
        override suspend fun deletePlaylistTracks(listId: String, fileIds: List<String>) = error("unused")
        override suspend fun cloudTracks(page: Int, pageSize: Int): com.resonote.core.network.model.NetworkCloudPage = error("unused")
        override suspend fun resolveCloudSongSource(hash: String, albumAudioId: String?, name: String): NetworkSongSource = error("unused")
        override suspend fun banners(): List<com.resonote.core.network.model.NetworkBanner> = error("unused")
        override suspend fun playlistCategories(): List<com.resonote.core.network.model.NetworkPlaylistCategory> = error("unused")
        override suspend fun newAlbums(page: Int, pageSize: Int): List<com.resonote.core.network.model.NetworkAlbum> = error("unused")
        override suspend fun albumSongs(albumId: String, page: Int, pageSize: Int): com.resonote.core.network.model.NetworkAlbumSongPage = error("unused")
        override suspend fun artistDetail(artistId: String): com.resonote.core.network.model.NetworkArtistInfo? = error("unused")
        override suspend fun artistSongs(artistId: String, page: Int, pageSize: Int, newestFirst: Boolean): com.resonote.core.network.model.NetworkArtistSongPage = error("unused")
        override suspend fun searchComplex(keywords: String): com.resonote.core.network.model.NetworkComplexSearch = error("unused")
        override suspend fun hotSearchKeywords(): List<com.resonote.core.network.model.NetworkSearchKeyword> = error("unused")
        override suspend fun searchSuggestions(keywords: String): List<String> = error("unused")
        override suspend fun searchLyric(hash: String, albumAudioId: String?): com.resonote.core.network.model.NetworkLyricCandidate? = error("unused")
        override suspend fun downloadLyric(candidate: com.resonote.core.network.model.NetworkLyricCandidate): String? = error("unused")
        override suspend fun resolveVideoUrl(hash: String): String? = error("unused")
        override suspend fun recognizeAudio(pcm: ByteArray): List<com.resonote.core.network.model.NetworkRecognitionMatch> = error("unused")
        override suspend fun createQrLoginKey(): String = "qr-key"
        override suspend fun checkQrLogin(key: String): NetworkQrLoginStatus = qrStatus
        override suspend fun claimDailyVip(receiveDay: String): com.resonote.core.network.model.NetworkVipRewardResult = error("unused")
        override suspend fun upgradeDailyVip(): com.resonote.core.network.model.NetworkVipRewardResult = error("unused")
    }

    private fun sessionManager(store: ApiSessionStore) =
        ApiSessionManager(Optional.of(store), ApiDeviceIdentityFactory())

    private class FakeSessionStore(
        private val failWrites: Boolean = false,
        initial: ApiSession? = null,
    ) : ApiSessionStore {
        private val state = MutableStateFlow(initial)
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

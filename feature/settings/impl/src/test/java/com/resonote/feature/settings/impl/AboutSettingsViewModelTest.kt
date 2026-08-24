package com.resonote.feature.settings.impl

import com.google.common.truth.Truth.assertThat
import com.resonote.core.data.AppUpdateRepository
import com.resonote.core.model.AppRelease
import com.resonote.core.model.CollectionLoadResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AboutSettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun sameVersionWithVPrefixIsLatest() = runTest(dispatcher) {
        val viewModel = AboutSettingsViewModel(repository("v0.1.2"))

        viewModel.checkForUpdates("0.1.2")
        advanceUntilIdle()

        assertThat(viewModel.updateState.value).isEqualTo(AboutUpdateState.Latest("v0.1.2"))
    }

    @Test
    fun newerStableReleaseIsAvailable() = runTest(dispatcher) {
        val release = AppRelease("v0.2.0", "https://github.com/release")
        val viewModel = AboutSettingsViewModel(repository(release.version, release.releaseUrl))

        viewModel.checkForUpdates("0.1.2")
        advanceUntilIdle()

        assertThat(viewModel.updateState.value).isEqualTo(AboutUpdateState.Available(release))
    }

    @Test
    fun stableReleaseIsNewerThanMatchingPrerelease() {
        assertThat(AppVersionComparator.isNewer("v1.0.0", "1.0.0-beta.2")).isTrue()
        assertThat(AppVersionComparator.isNewer("v1.0.0-beta.1", "1.0.0")).isFalse()
        assertThat(AppVersionComparator.isNewer("v0.1.2", "0.2.0")).isFalse()
    }

    @Test
    fun failedCheckCanBeRetried() = runTest(dispatcher) {
        var attempts = 0
        val viewModel = AboutSettingsViewModel(
            object : AppUpdateRepository {
                override suspend fun latestRelease(): CollectionLoadResult<AppRelease> {
                    attempts++
                    return CollectionLoadResult.Failed(com.resonote.core.model.ContentFailure.Network)
                }
            },
        )

        viewModel.checkForUpdates("0.1.2")
        advanceUntilIdle()
        viewModel.checkForUpdates("0.1.2", force = true)
        advanceUntilIdle()

        assertThat(attempts).isEqualTo(2)
        assertThat(viewModel.updateState.value).isEqualTo(AboutUpdateState.Failed)
    }

    private fun repository(version: String, url: String = "https://github.com/release") = object : AppUpdateRepository {
        override suspend fun latestRelease() = CollectionLoadResult.Available(AppRelease(version, url))
    }
}

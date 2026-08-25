package com.resonote.app

import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import com.resonote.core.navigation.LoginGateNavKey
import com.resonote.core.navigation.TabsShellNavKey
import com.resonote.feature.search.api.SearchNavKey
import org.junit.Test

class NavigationBackStackTest {
    @Test
    fun staleEntryCannotPopCurrentDestination() {
        val staleEntry = SearchNavKey(sessionId = 1L)
        val currentEntry = SearchNavKey(sessionId = 2L)
        val backStack = mutableListOf<NavKey>(TabsShellNavKey, staleEntry, currentEntry)

        assertThat(backStack.popIfCurrent(staleEntry)).isFalse()
        assertThat(backStack).containsExactly(TabsShellNavKey, staleEntry, currentEntry).inOrder()
    }

    @Test
    fun currentEntryCanPopItself() {
        val currentEntry = SearchNavKey(sessionId = 1L)
        val backStack = mutableListOf<NavKey>(TabsShellNavKey, currentEntry)

        assertThat(backStack.popIfCurrent(currentEntry)).isTrue()
        assertThat(backStack).containsExactly(TabsShellNavKey)
    }

    @Test
    fun rootEntryCannotBePopped() {
        val backStack = mutableListOf<NavKey>(TabsShellNavKey)

        assertThat(backStack.popIfCurrent(TabsShellNavKey)).isFalse()
        assertThat(backStack).containsExactly(TabsShellNavKey)
    }

    @Test
    fun systemBackDismissesAuthenticationGateAndAcknowledgesIt() {
        val backStack = mutableListOf<NavKey>(TabsShellNavKey, LoginGateNavKey(sessionExpired = false))
        var acknowledgements = 0

        assertThat(backStack.popCurrentDestination { acknowledgements += 1 }).isTrue()

        assertThat(backStack).containsExactly(TabsShellNavKey)
        assertThat(acknowledgements).isEqualTo(1)
    }

    @Test
    fun systemBackFromRegularDestinationDoesNotAcknowledgeAuthenticationGate() {
        val destination = SearchNavKey(sessionId = 1L)
        val backStack = mutableListOf<NavKey>(TabsShellNavKey, destination)
        var acknowledgements = 0

        assertThat(backStack.popCurrentDestination { acknowledgements += 1 }).isTrue()

        assertThat(backStack).containsExactly(TabsShellNavKey)
        assertThat(acknowledgements).isEqualTo(0)
    }
}

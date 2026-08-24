package com.resonote.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabsShellStateTest {
    @Test
    fun repeatedSelection_isNoOp() {
        val state = TabsShellState(ResonoteTab.HOME)

        assertFalse(state.selectTab(ResonoteTab.HOME))
        assertEquals(ResonoteTab.HOME, state.selectedTab)
    }

    @Test
    fun backFromSecondaryTab_returnsHome_thenFallsThrough() {
        val state = TabsShellState(ResonoteTab.DISCOVER)

        assertTrue(state.handleBack())
        assertEquals(ResonoteTab.HOME, state.selectedTab)
        assertFalse(state.handleBack())
    }

    @Test
    fun secondaryTab_doesNotHandleBackWhileChildDestinationIsOnTop() {
        val state = TabsShellState(ResonoteTab.MY)

        assertFalse(state.canHandleBack(isActiveDestination = false))
        assertEquals(ResonoteTab.MY, state.selectedTab)
        assertTrue(state.canHandleBack(isActiveDestination = true))
    }

    @Test
    fun savedValue_restoresSelectedTab() {
        val restored = requireNotNull(TabsShellState.Saver.restore(ResonoteTab.MY.name))

        assertEquals(ResonoteTab.MY, restored.selectedTab)
    }

    @Test
    fun selectingEachTab_keepsOnlyTheLatestTabSelected() {
        val state = TabsShellState(ResonoteTab.HOME)

        ResonoteTab.entries.forEach { tab ->
            state.selectTab(tab)

            assertEquals(tab, state.selectedTab)
        }
    }
}

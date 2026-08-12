package com.resonote.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Stable
internal class TabsShellState(initialTab: ResonoteTab) {
    var selectedTab by mutableStateOf(initialTab)
        private set

    fun selectTab(tab: ResonoteTab): Boolean {
        if (tab == selectedTab) return false
        selectedTab = tab
        return true
    }

    fun handleBack(): Boolean {
        if (selectedTab == ResonoteTab.HOME) return false
        selectedTab = ResonoteTab.HOME
        return true
    }

    companion object {
        val Saver = Saver<TabsShellState, String>(
            save = { it.selectedTab.name },
            restore = { TabsShellState(ResonoteTab.valueOf(it)) },
        )
    }
}

@Composable
internal fun rememberTabsShellState(): TabsShellState = rememberSaveable(saver = TabsShellState.Saver) {
    TabsShellState(ResonoteTab.HOME)
}

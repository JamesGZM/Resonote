package com.resonote.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.resonote.core.navigation.LoginGateNavKey
import com.resonote.core.navigation.TabsShellNavKey

@Composable
internal fun ResonoteApp(viewModel: MainActivityViewModel = hiltViewModel()) {
    val backStack = rememberNavBackStack(TabsShellNavKey)
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    LaunchedEffect(authState) {
        backStack.synchronizeAuthenticationGate(authState)
    }

    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<TabsShellNavKey> { TabsShell() }
            entry<LoginGateNavKey> { key ->
                LoginGateScreen(
                    sessionExpired = key.sessionExpired,
                    onBack = {
                        if (backStack.lastOrNull() is LoginGateNavKey) backStack.removeAt(backStack.lastIndex)
                        viewModel.acknowledgeAuthenticationGate()
                    },
                )
            }
        },
    )
}

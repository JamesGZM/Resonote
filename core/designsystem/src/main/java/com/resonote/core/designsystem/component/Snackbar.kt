package com.resonote.core.designsystem.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

val LocalResonoteSnackbarController = staticCompositionLocalOf<ResonoteSnackbarController?> { null }

@Stable
class ResonoteSnackbarController internal constructor(
    private val hostState: SnackbarHostState,
    private val scope: CoroutineScope,
) {
    fun show(
        message: String,
        actionLabel: String? = null,
        withDismissAction: Boolean = actionLabel != null,
        duration: SnackbarDuration = if (actionLabel == null) SnackbarDuration.Long else SnackbarDuration.Indefinite,
        onResult: (SnackbarResult) -> Unit = {},
    ) {
        scope.launch {
            onResult(
                hostState.showSnackbar(
                    message = message,
                    actionLabel = actionLabel,
                    withDismissAction = withDismissAction,
                    duration = duration,
                ),
            )
        }
    }
}

@Composable
fun rememberResonoteSnackbarController(
    hostState: SnackbarHostState,
): ResonoteSnackbarController {
    val scope = rememberCoroutineScope()
    return remember(hostState, scope) { ResonoteSnackbarController(hostState, scope) }
}

@Composable
fun ResonoteSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.exclude(WindowInsets.ime))
            .testTag("resonote-snackbar-host"),
        snackbar = { data ->
            Snackbar(
                snackbarData = data,
                shape = MaterialTheme.shapes.extraSmall,
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                actionColor = MaterialTheme.colorScheme.inversePrimary,
            )
        },
    )
}

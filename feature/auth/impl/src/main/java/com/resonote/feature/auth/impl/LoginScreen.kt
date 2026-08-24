package com.resonote.feature.auth.impl

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.ResonoteButton
import com.resonote.core.designsystem.component.ResonoteTopAppBar

@Composable
fun LoginRoute(sessionExpired: Boolean, onBack: () -> Unit, viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LoginScreen(
        state = state,
        sessionExpired = sessionExpired,
        onBack = onBack,
        onMethodSelected = viewModel::selectMethod,
        onMobileChanged = viewModel::updateMobile,
        onCodeChanged = viewModel::updateCode,
        onUsernameChanged = viewModel::updateUsername,
        onPasswordChanged = viewModel::updatePassword,
        onPasswordVisibilityToggle = viewModel::togglePasswordVisibility,
        onSendCode = viewModel::sendCode,
        onLogin = viewModel::login,
        onAccountSelected = viewModel::selectAccount,
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun LoginScreen(
    state: LoginUiState,
    sessionExpired: Boolean,
    onBack: () -> Unit,
    onMethodSelected: (LoginMethod) -> Unit,
    onMobileChanged: (String) -> Unit,
    onCodeChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onSendCode: () -> Unit,
    onLogin: () -> Unit,
    onAccountSelected: (String) -> Unit,
) {
    BackHandler(onBack = onBack)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ResonoteTopAppBar(
                title = { Text(stringResource(R.string.feature_auth_impl_auth_login_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.feature_auth_impl_auth_back),
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .imePadding()
                .imeNestedScroll()
                .verticalScroll(rememberScrollState())
                .testTag("login-scroll")
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(40.dp))
            Text(
                text =
                stringResource(
                    if (sessionExpired) {
                        R.string.feature_auth_impl_auth_expired_title
                    } else {
                        R.string.feature_auth_impl_auth_title
                    },
                ),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text =
                stringResource(
                    if (sessionExpired) {
                        R.string.feature_auth_impl_auth_expired_body
                    } else {
                        R.string.feature_auth_impl_auth_body
                    },
                ),
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(24.dp))
            LoginMethodSelector(state.method, onMethodSelected)
            Spacer(Modifier.height(16.dp))
            when (state.method) {
                LoginMethod.MobileCode -> MobileFields(state, onMobileChanged, onCodeChanged, onSendCode)
                LoginMethod.Password -> PasswordFields(
                    state,
                    onUsernameChanged,
                    onPasswordChanged,
                    onPasswordVisibilityToggle,
                )
            }
            state.message?.let { message ->
                MessageBanner(message, modifier = Modifier.padding(top = 16.dp))
            }
            if (state.accounts.isNotEmpty()) {
                AccountPicker(
                    accounts = state.accounts,
                    enabled = !state.isLoggingIn,
                    onAccountSelected = onAccountSelected,
                    modifier = Modifier.padding(top = 18.dp),
                )
            } else {
                ResonoteButton(
                    label = stringResource(R.string.feature_auth_impl_auth_login),
                    loadingLabel = stringResource(R.string.feature_auth_impl_auth_logging_in),
                    onClick = onLogin,
                    enabled = state.canLogin,
                    loading = state.isLoggingIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp)
                        .height(44.dp)
                        .testTag("login-submit"),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.feature_auth_impl_auth_security_note),
                    modifier = Modifier.padding(start = 6.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

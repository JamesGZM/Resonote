package com.resonote.feature.auth.impl

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.ResonoteButton
import com.resonote.core.designsystem.component.ResonoteTextButton
import com.resonote.core.designsystem.component.ResonoteTextField
import com.resonote.core.model.AuthAccountOption

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
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .testTag("login-scroll")
                .padding(horizontal = 24.dp),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.padding(top = 4.dp)) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.feature_auth_impl_auth_back),
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.feature_auth_impl_auth_brand),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
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
                style = MaterialTheme.typography.displaySmall,
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
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(28.dp))
            LoginMethodSelector(state.method, onMethodSelected)
            Spacer(Modifier.height(20.dp))
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
                MessageBanner(message, modifier = Modifier.padding(top = 14.dp))
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
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
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

@Composable
private fun LoginMethodSelector(selected: LoginMethod, onSelected: (LoginMethod) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LoginMethod.entries.forEach { method ->
            val isSelected = selected == method
            Surface(
                onClick = { onSelected(method) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color =
                if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Text(
                    text = stringResource(
                        if (method == LoginMethod.MobileCode) {
                            R.string.feature_auth_impl_auth_mobile_method
                        } else {
                            R.string.feature_auth_impl_auth_password_method
                        },
                    ),
                    modifier = Modifier.padding(vertical = 12.dp),
                    color =
                    if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun MobileFields(
    state: LoginUiState,
    onMobileChanged: (String) -> Unit,
    onCodeChanged: (String) -> Unit,
    onSendCode: () -> Unit,
) {
    ResonoteTextField(
        value = state.mobile,
        onValueChange = onMobileChanged,
        label = stringResource(R.string.feature_auth_impl_auth_mobile),
        placeholder = stringResource(R.string.feature_auth_impl_auth_mobile_placeholder),
        prefix = "+86",
        maxLength = 11,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        enabled = !state.isLoggingIn,
        modifier = Modifier.fillMaxWidth(),
    )
    ResonoteTextField(
        value = state.code,
        onValueChange = onCodeChanged,
        label = stringResource(R.string.feature_auth_impl_auth_code),
        placeholder = stringResource(R.string.feature_auth_impl_auth_code_placeholder),
        maxLength = 8,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        enabled = !state.isLoggingIn,
        trailingAction = {
            ResonoteTextButton(
                label =
                stringResource(
                    if (state.message == LoginMessage.CodeSent) {
                        R.string.feature_auth_impl_auth_resend_code
                    } else {
                        R.string.feature_auth_impl_auth_send_code
                    },
                ),
                loadingLabel = stringResource(R.string.feature_auth_impl_auth_sending_code),
                onClick = onSendCode,
                enabled = state.canSendCode,
                loading = state.isSendingCode,
            )
        },
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
    )
}

@Composable
private fun PasswordFields(
    state: LoginUiState,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
) {
    ResonoteTextField(
        value = state.username,
        onValueChange = onUsernameChanged,
        label = stringResource(R.string.feature_auth_impl_auth_username),
        placeholder = stringResource(R.string.feature_auth_impl_auth_username_placeholder),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        enabled = !state.isLoggingIn,
        modifier = Modifier.fillMaxWidth(),
    )
    ResonoteTextField(
        value = state.password,
        onValueChange = onPasswordChanged,
        label = stringResource(R.string.feature_auth_impl_auth_password),
        visualTransformation =
        if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        enabled = !state.isLoggingIn,
        trailingAction = {
            IconButton(onClick = onPasswordVisibilityToggle) {
                Icon(
                    imageVector =
                    if (state.passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = stringResource(
                        if (state.passwordVisible) {
                            R.string.feature_auth_impl_auth_hide_password
                        } else {
                            R.string.feature_auth_impl_auth_show_password
                        },
                    ),
                )
            }
        },
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
    )
}

@Composable
private fun MessageBanner(message: LoginMessage, modifier: Modifier = Modifier) {
    val success = message == LoginMessage.CodeSent
    Surface(
        modifier = modifier.fillMaxWidth().testTag("login-message"),
        color =
        if (success) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
        contentColor =
        if (success) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onErrorContainer
        },
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (success) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Text(
                text = stringResource(message.stringResourceId()),
                modifier = Modifier.padding(start = if (success) 8.dp else 0.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun AccountPicker(
    accounts: List<AuthAccountOption>,
    enabled: Boolean,
    onAccountSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().testTag("account-picker")) {
        Text(
            stringResource(R.string.feature_auth_impl_auth_choose_account),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(R.string.feature_auth_impl_auth_choose_account_body),
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        accounts.forEachIndexed { index, account ->
            Card(
                onClick = { onAccountSelected(account.userId) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                account.nickname.take(1).ifBlank { "·" },
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(
                            account.nickname.ifBlank {
                                stringResource(R.string.feature_auth_impl_auth_unnamed_account)
                            },
                            fontWeight = FontWeight.Medium,
                        )
                        account.grade?.takeIf(String::isNotBlank)?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.feature_auth_impl_auth_continue),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            if (index < accounts.lastIndex) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

private fun LoginMessage.stringResourceId(): Int = when (this) {
    LoginMessage.CodeSent -> R.string.feature_auth_impl_auth_message_code_sent
    LoginMessage.InvalidInput -> R.string.feature_auth_impl_auth_message_invalid_input
    LoginMessage.Rejected -> R.string.feature_auth_impl_auth_message_rejected
    LoginMessage.RiskVerificationRequired -> R.string.feature_auth_impl_auth_message_risk
    LoginMessage.Network -> R.string.feature_auth_impl_auth_message_network
    LoginMessage.Protocol -> R.string.feature_auth_impl_auth_message_protocol
    LoginMessage.SecureStorage -> R.string.feature_auth_impl_auth_message_storage
    LoginMessage.PasswordMultipleAccounts -> R.string.feature_auth_impl_auth_message_password_multiple
}

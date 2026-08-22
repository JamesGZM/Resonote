package com.resonote.feature.auth.impl

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteTextButton
import com.resonote.core.designsystem.component.ResonoteTextField

@Composable
internal fun LoginMethodSelector(selected: LoginMethod, onSelected: (LoginMethod) -> Unit) {
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
internal fun MobileFields(
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
internal fun PasswordFields(
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
internal fun MessageBanner(message: LoginMessage, modifier: Modifier = Modifier) {
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

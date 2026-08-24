package com.resonote.feature.auth.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteTextButton
import com.resonote.core.designsystem.tokens.ResonoteTokens
import kotlinx.coroutines.flow.first

@Composable
internal fun LoginMethodSelector(selected: LoginMethod, onSelected: (LoginMethod) -> Unit) {
    Row(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        LoginMethod.entries.forEach { method ->
            LoginMethodTab(
                method = method,
                selected = selected == method,
                onClick = { onSelected(method) },
            )
        }
    }
}

@Composable
private fun LoginMethodTab(method: LoginMethod, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .height(48.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = false,
                    radius = 32.dp,
                    color = MaterialTheme.colorScheme.primary,
                ),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(
                if (method == LoginMethod.MobileCode) {
                    R.string.feature_auth_impl_auth_mobile_method
                } else {
                    R.string.feature_auth_impl_auth_password_method
                },
            ),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .width(24.dp)
                .height(3.dp)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    ResonoteTokens.shapes.full,
                )
                .testTag("login-method-indicator-${method.name}"),
        )
    }
}

@Composable
internal fun MobileFields(
    state: LoginUiState,
    onMobileChanged: (String) -> Unit,
    onCodeChanged: (String) -> Unit,
    onSendCode: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LoginInputField(
            value = state.mobile,
            onValueChange = onMobileChanged,
            label = stringResource(R.string.feature_auth_impl_auth_mobile),
            placeholder = stringResource(R.string.feature_auth_impl_auth_mobile_placeholder),
            leadingIcon = Icons.Rounded.PhoneAndroid,
            prefix = "+86",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            enabled = !state.isLoggingIn,
            modifier = Modifier.fillMaxWidth().testTag("login-mobile-input"),
        )
        LoginInputField(
            value = state.code,
            onValueChange = onCodeChanged,
            label = stringResource(R.string.feature_auth_impl_auth_code),
            placeholder = stringResource(R.string.feature_auth_impl_auth_code_placeholder),
            leadingIcon = Icons.Rounded.Shield,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            enabled = !state.isLoggingIn,
            trailingAction = {
                ResonoteTextButton(
                    label = stringResource(
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
            modifier = Modifier.fillMaxWidth().testTag("login-code-input"),
        )
    }
}

@Composable
internal fun PasswordFields(
    state: LoginUiState,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LoginInputField(
            value = state.username,
            onValueChange = onUsernameChanged,
            label = stringResource(R.string.feature_auth_impl_auth_username),
            placeholder = stringResource(R.string.feature_auth_impl_auth_username_placeholder),
            leadingIcon = Icons.Rounded.Person,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = !state.isLoggingIn,
            modifier = Modifier.fillMaxWidth().testTag("login-username-input"),
        )
        LoginInputField(
            value = state.password,
            onValueChange = onPasswordChanged,
            label = stringResource(R.string.feature_auth_impl_auth_password),
            placeholder = stringResource(R.string.feature_auth_impl_auth_password),
            leadingIcon = Icons.Rounded.Lock,
            visualTransformation = if (state.passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = !state.isLoggingIn,
            trailingAction = {
                IconButton(onClick = onPasswordVisibilityToggle, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = if (state.passwordVisible) {
                            Icons.Rounded.VisibilityOff
                        } else {
                            Icons.Rounded.Visibility
                        },
                        contentDescription = stringResource(
                            if (state.passwordVisible) {
                                R.string.feature_auth_impl_auth_hide_password
                            } else {
                                R.string.feature_auth_impl_auth_show_password
                            },
                        ),
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("login-password-input"),
        )
    }
}

@Composable
private fun LoginInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    prefix: String? = null,
    trailingAction: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    var isFocused by remember { mutableStateOf(false) }
    LaunchedEffect(isFocused) {
        if (isFocused) {
            snapshotFlow { imeInsets.getBottom(density) }
                .first { it > 0 }
            bringIntoViewRequester.bringIntoView()
        }
    }
    val textColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    Surface(
        modifier = modifier.height(56.dp),
        shape = ResonoteTokens.shapes.full,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxSize()
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusChanged { isFocused = it.isFocused }
                .semantics { contentDescription = label },
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            singleLine = true,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxSize().padding(start = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(10.dp))
                    prefix?.let {
                        Text(it, color = textColor, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(12.dp))
                        Box(
                            Modifier.width(1.dp).height(20.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        innerTextField()
                    }
                    trailingAction?.invoke()
                }
            },
        )
    }
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

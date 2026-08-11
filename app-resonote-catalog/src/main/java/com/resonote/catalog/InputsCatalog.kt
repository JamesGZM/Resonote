package com.resonote.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.resonote.core.designsystem.component.ResonoteIconButton
import com.resonote.core.designsystem.component.ResonoteTextField
import com.resonote.core.designsystem.tokens.ResonoteTokens

@Composable
internal fun InputsCatalog() {
    var email by rememberSaveable { mutableStateOf("") }
    var biography by rememberSaveable { mutableStateOf("Resonote keeps music notes close to the listening context.") }
    var password by rememberSaveable { mutableStateOf("resonote") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space4),
    ) {
        CatalogInputGroupTitle("Interactive input")
        ResonoteTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            modifier = Modifier.fillMaxWidth(),
            placeholder = "name@example.com",
            supportingText = "Used to sync your library",
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Email, contentDescription = null)
            },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Email,
            ),
            maxLength = 48,
        )

        CatalogInputGroupTitle("Error, disabled and read-only")
        ResonoteTextField(
            value = "invalid address",
            onValueChange = {},
            label = "Email",
            modifier = Modifier.fillMaxWidth(),
            errorMessage = "Enter a valid email address",
        )
        ResonoteTextField(
            value = "Disabled value",
            onValueChange = {},
            label = "Disabled",
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
        )
        ResonoteTextField(
            value = "RES-2026-0811",
            onValueChange = {},
            label = "Library ID",
            modifier = Modifier.fillMaxWidth(),
            supportingText = "Copy is available",
            readOnly = true,
            prefix = "ID · ",
        )

        CatalogInputGroupTitle("Multiline and metadata")
        ResonoteTextField(
            value = biography,
            onValueChange = { biography = it },
            label = "Listening note",
            modifier = Modifier.fillMaxWidth(),
            supportingText = "Describe what stood out",
            suffix = " note",
            singleLine = false,
            minLines = 3,
            maxLines = 5,
            maxLength = 160,
        )

        CatalogInputGroupTitle("Password transformation")
        ResonoteTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Password,
            ),
            trailingAction = {
                ResonoteIconButton(
                    label = if (passwordVisible) "Hide password" else "Show password",
                    onClick = { passwordVisible = !passwordVisible },
                    icon = {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = null,
                        )
                    },
                )
            },
        )
    }
}

@Composable
private fun CatalogInputGroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

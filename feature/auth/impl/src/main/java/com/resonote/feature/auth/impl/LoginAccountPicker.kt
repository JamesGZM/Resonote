package com.resonote.feature.auth.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import com.resonote.core.model.AuthAccountOption

@Composable
internal fun AccountPicker(
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

internal fun LoginMessage.stringResourceId(): Int = when (this) {
    LoginMessage.CodeSent -> R.string.feature_auth_impl_auth_message_code_sent
    LoginMessage.InvalidInput -> R.string.feature_auth_impl_auth_message_invalid_input
    LoginMessage.Rejected -> R.string.feature_auth_impl_auth_message_rejected
    LoginMessage.RiskVerificationRequired -> R.string.feature_auth_impl_auth_message_risk
    LoginMessage.Network -> R.string.feature_auth_impl_auth_message_network
    LoginMessage.Protocol -> R.string.feature_auth_impl_auth_message_protocol
    LoginMessage.SecureStorage -> R.string.feature_auth_impl_auth_message_storage
    LoginMessage.PasswordMultipleAccounts -> R.string.feature_auth_impl_auth_message_password_multiple
}

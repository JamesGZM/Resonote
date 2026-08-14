@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.resonote.feature.vip.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.ResonoteButton
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.model.ContentFailure

@Composable
fun DailyVipRoute(onBack: () -> Unit, onRewardApplied: () -> Unit, viewModel: DailyVipViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.rewardApplied.collect { onRewardApplied() }
    }
    DailyVipScreen(
        state = state,
        onBack = onBack,
        onClaim = viewModel::claim,
        onUpgrade = viewModel::upgrade,
        onDeclineUpgrade = viewModel::declineUpgrade,
        onRetry = viewModel::retry,
    )
}

@Composable
internal fun DailyVipScreen(
    state: DailyVipUiState,
    onBack: () -> Unit,
    onClaim: () -> Unit,
    onUpgrade: () -> Unit,
    onDeclineUpgrade: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ResonoteTopAppBar(
                title = { Text(stringResource(R.string.feature_vip_impl_daily_vip_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            stringResource(R.string.feature_vip_impl_daily_vip_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("daily-vip-list"),
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item(key = "ticket") {
                DailyPassTicket(receiveDay = state.receiveDay)
            }
            item(key = "status") {
                StatusCard(state = state, onClaim = onClaim, onRetry = onRetry)
            }
            item(key = "rules") {
                RulesCard()
            }
        }
    }

    (state as? DailyVipUiState.UpgradeChoice)?.let { choice ->
        AlertDialog(
            onDismissRequest = onDeclineUpgrade,
            icon = { Icon(Icons.Rounded.Star, contentDescription = null) },
            title = { Text(stringResource(R.string.feature_vip_impl_daily_vip_upgrade_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        if (choice.alreadyClaimed) {
                            R.string.feature_vip_impl_daily_vip_upgrade_dialog_already
                        } else {
                            R.string.feature_vip_impl_daily_vip_upgrade_dialog_claimed
                        },
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = onUpgrade) {
                    Text(stringResource(R.string.feature_vip_impl_daily_vip_upgrade_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDeclineUpgrade) {
                    Text(stringResource(R.string.feature_vip_impl_daily_vip_upgrade_decline))
                }
            },
        )
    }
}

@Composable
private fun DailyPassTicket(receiveDay: String) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("daily-vip-ticket"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ),
                )
                .padding(top = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.feature_vip_impl_daily_vip_eyebrow),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = stringResource(R.string.feature_vip_impl_daily_vip_one_day),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = stringResource(R.string.feature_vip_impl_daily_vip_reward),
                            modifier = Modifier.padding(start = 10.dp, bottom = 7.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.CardGiftcard, contentDescription = null, modifier = Modifier.size(28.dp))
                    }
                }
            }
            TicketPerforation()
            Text(
                text = stringResource(R.string.feature_vip_impl_daily_vip_date, receiveDay),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TicketPerforation() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        repeat(22) {
            Surface(
                modifier = Modifier.size(4.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.outlineVariant,
            ) {}
        }
    }
}

@Composable
private fun StatusCard(state: DailyVipUiState, onClaim: () -> Unit, onRetry: () -> Unit) {
    val presentation = state.presentation()
    Card(
        modifier = Modifier.fillMaxWidth().testTag("daily-vip-status"),
        colors = CardDefaults.cardColors(containerColor = presentation.containerColor()),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = presentation.iconContainerColor(),
                contentColor = presentation.iconColor(),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (state is DailyVipUiState.Claiming || state is DailyVipUiState.Upgrading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(presentation.icon, contentDescription = null, modifier = Modifier.size(23.dp))
                    }
                }
            }
            Text(
                text = stringResource(presentation.title),
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = presentation.body(state),
                modifier = Modifier.padding(top = 7.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            when (state) {
                is DailyVipUiState.Ready -> ResonoteButton(
                    label = stringResource(R.string.feature_vip_impl_daily_vip_claim),
                    onClick = onClaim,
                    modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
                )
                is DailyVipUiState.Claiming -> ResonoteButton(
                    label = stringResource(R.string.feature_vip_impl_daily_vip_claim),
                    loadingLabel = stringResource(R.string.feature_vip_impl_daily_vip_claiming),
                    loading = true,
                    enabled = false,
                    onClick = {},
                    modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
                )
                is DailyVipUiState.Failed -> if (state.failure !is ContentFailure.AuthenticationRequired) {
                    ResonoteButton(
                        label = stringResource(
                            if (state.operation == DailyVipOperation.Claim) {
                                R.string.feature_vip_impl_daily_vip_retry_claim
                            } else {
                                R.string.feature_vip_impl_daily_vip_retry_upgrade
                            },
                        ),
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
                    )
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun RulesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(
                stringResource(R.string.feature_vip_impl_daily_vip_how_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            RuleRow("01", stringResource(R.string.feature_vip_impl_daily_vip_rule_confirm))
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            RuleRow("02", stringResource(R.string.feature_vip_impl_daily_vip_rule_server))
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            RuleRow("03", stringResource(R.string.feature_vip_impl_daily_vip_rule_safe))
        }
    }
}

@Composable
private fun RuleRow(number: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = number,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f).padding(start = 14.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private data class StatusPresentation(val icon: ImageVector, val title: Int, val body: Int, val tone: StatusTone)

private enum class StatusTone { Neutral, Success, Error, Risk }

private fun DailyVipUiState.presentation(): StatusPresentation = when (this) {
    is DailyVipUiState.Ready -> StatusPresentation(
        Icons.Rounded.CardGiftcard,
        R.string.feature_vip_impl_daily_vip_ready_title,
        R.string.feature_vip_impl_daily_vip_ready_body,
        StatusTone.Neutral,
    )
    is DailyVipUiState.Claiming -> StatusPresentation(
        Icons.Rounded.CardGiftcard,
        R.string.feature_vip_impl_daily_vip_ready_title,
        R.string.feature_vip_impl_daily_vip_ready_body,
        StatusTone.Neutral,
    )
    is DailyVipUiState.UpgradeChoice -> successPresentation(alreadyClaimed)
    is DailyVipUiState.ClaimComplete -> successPresentation(alreadyClaimed)
    is DailyVipUiState.Upgrading -> StatusPresentation(
        Icons.Rounded.Star,
        R.string.feature_vip_impl_daily_vip_upgrading_title,
        R.string.feature_vip_impl_daily_vip_upgrading_body,
        StatusTone.Neutral,
    )
    is DailyVipUiState.UpgradeComplete -> StatusPresentation(
        Icons.Rounded.CheckCircle,
        if (alreadyUpgraded) {
            R.string.feature_vip_impl_daily_vip_already_upgraded_title
        } else {
            R.string.feature_vip_impl_daily_vip_upgraded_title
        },
        if (alreadyUpgraded) {
            R.string.feature_vip_impl_daily_vip_already_upgraded_body
        } else {
            R.string.feature_vip_impl_daily_vip_upgraded_body
        },
        StatusTone.Success,
    )
    is DailyVipUiState.RiskBlocked -> StatusPresentation(
        Icons.Rounded.Lock,
        R.string.feature_vip_impl_daily_vip_risk_title,
        R.string.feature_vip_impl_daily_vip_risk_body,
        StatusTone.Risk,
    )
    is DailyVipUiState.Failed -> StatusPresentation(
        Icons.Rounded.ErrorOutline,
        R.string.feature_vip_impl_daily_vip_error_title,
        R.string.feature_vip_impl_daily_vip_error_protocol,
        StatusTone.Error,
    )
}

private fun successPresentation(alreadyClaimed: Boolean) = StatusPresentation(
    Icons.Rounded.CheckCircle,
    if (alreadyClaimed) {
        R.string.feature_vip_impl_daily_vip_already_claimed_title
    } else {
        R.string.feature_vip_impl_daily_vip_claimed_title
    },
    if (alreadyClaimed) {
        R.string.feature_vip_impl_daily_vip_already_claimed_body
    } else {
        R.string.feature_vip_impl_daily_vip_claimed_body
    },
    StatusTone.Success,
)

@Composable
private fun StatusPresentation.body(state: DailyVipUiState): String = if (state is DailyVipUiState.Failed) {
    stringResource(
        when (state.failure) {
            ContentFailure.AuthenticationRequired -> R.string.feature_vip_impl_daily_vip_error_auth
            ContentFailure.Network -> R.string.feature_vip_impl_daily_vip_error_network
            ContentFailure.ServiceRejected -> R.string.feature_vip_impl_daily_vip_error_service
            is ContentFailure.RiskVerificationRequired -> R.string.feature_vip_impl_daily_vip_risk_body
            ContentFailure.RiskBlocked -> R.string.feature_vip_impl_daily_vip_risk_body
            ContentFailure.Protocol -> R.string.feature_vip_impl_daily_vip_error_protocol
        },
    )
} else {
    stringResource(body)
}

@Composable
private fun StatusPresentation.containerColor() = when (tone) {
    StatusTone.Neutral -> MaterialTheme.colorScheme.surfaceContainerLow
    StatusTone.Success -> MaterialTheme.colorScheme.primaryContainer
    StatusTone.Error -> MaterialTheme.colorScheme.errorContainer
    StatusTone.Risk -> MaterialTheme.colorScheme.secondaryContainer
}

@Composable
private fun StatusPresentation.iconContainerColor() = when (tone) {
    StatusTone.Neutral -> MaterialTheme.colorScheme.primaryContainer
    StatusTone.Success -> MaterialTheme.colorScheme.primary
    StatusTone.Error -> MaterialTheme.colorScheme.error
    StatusTone.Risk -> MaterialTheme.colorScheme.secondary
}

@Composable
private fun StatusPresentation.iconColor() = when (tone) {
    StatusTone.Neutral -> MaterialTheme.colorScheme.onPrimaryContainer
    StatusTone.Success -> MaterialTheme.colorScheme.onPrimary
    StatusTone.Error -> MaterialTheme.colorScheme.onError
    StatusTone.Risk -> MaterialTheme.colorScheme.onSecondary
}

package com.resonote.feature.vip.impl

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.ResonoteButton
import com.resonote.core.designsystem.component.ResonoteTextButton
import com.resonote.core.designsystem.tokens.ResonoteTokens
import com.resonote.core.model.ContentFailure

@Composable
fun DailyVipDialogRoute(
    visible: Boolean,
    onDismiss: () -> Unit,
    onRewardApplied: () -> Unit,
    viewModel: DailyVipViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.rewardApplied.collect { onRewardApplied() }
    }
    LaunchedEffect(visible) {
        if (visible) viewModel.reset()
    }
    if (visible) {
        DailyVipDialog(
            state = state,
            onDismiss = {
                if (state is DailyVipUiState.UpgradeChoice) viewModel.declineUpgrade()
                onDismiss()
            },
            onClaim = viewModel::claim,
            onUpgrade = viewModel::upgrade,
            onDeclineUpgrade = {
                viewModel.declineUpgrade()
                onDismiss()
            },
            onRetry = viewModel::retry,
        )
    }
}

@Composable
internal fun DailyVipDialog(
    state: DailyVipUiState,
    onDismiss: () -> Unit,
    onClaim: () -> Unit,
    onUpgrade: () -> Unit,
    onDeclineUpgrade: () -> Unit,
    onRetry: () -> Unit,
) {
    val busy = state is DailyVipUiState.Claiming || state is DailyVipUiState.Upgrading
    val presentation = state.presentation()
    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !busy,
            dismissOnClickOutside = !busy,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .widthIn(max = 360.dp)
                .testTag("daily-vip-dialog"),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp,
            shadowElevation = ResonoteTokens.elevation.level3.maximumShadow,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DialogIcon(presentation.icon, presentation.isError)
                Text(
                    text = stringResource(presentation.title),
                    modifier = Modifier.padding(top = 18.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = presentation.body(state),
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                if (state is DailyVipUiState.Ready || state is DailyVipUiState.Claiming) {
                    Text(
                        text = stringResource(R.string.feature_vip_impl_daily_vip_date, state.receiveDay),
                        modifier = Modifier.padding(top = 10.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                    )
                }
                DialogActions(
                    state = state,
                    onDismiss = onDismiss,
                    onClaim = onClaim,
                    onUpgrade = onUpgrade,
                    onDeclineUpgrade = onDeclineUpgrade,
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun DialogIcon(icon: ImageVector, isError: Boolean) {
    val container = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val content = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.size(52.dp),
        shape = CircleShape,
        color = container,
        contentColor = content,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun DialogActions(
    state: DailyVipUiState,
    onDismiss: () -> Unit,
    onClaim: () -> Unit,
    onUpgrade: () -> Unit,
    onDeclineUpgrade: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        when (state) {
            is DailyVipUiState.Ready, is DailyVipUiState.Claiming -> {
                ResonoteButton(
                    label = stringResource(R.string.feature_vip_impl_daily_vip_claim),
                    loadingLabel = stringResource(R.string.feature_vip_impl_daily_vip_claiming),
                    onClick = onClaim,
                    modifier = Modifier.fillMaxWidth(),
                    loading = state is DailyVipUiState.Claiming,
                )
                if (state is DailyVipUiState.Ready) {
                    CloseAction(onDismiss, R.string.feature_vip_impl_daily_vip_later)
                }
            }
            is DailyVipUiState.UpgradeChoice, is DailyVipUiState.Upgrading -> {
                ResonoteButton(
                    label = stringResource(R.string.feature_vip_impl_daily_vip_upgrade_confirm),
                    onClick = onUpgrade,
                    modifier = Modifier.fillMaxWidth(),
                    loading = state is DailyVipUiState.Upgrading,
                    loadingLabel = stringResource(R.string.feature_vip_impl_daily_vip_upgrading_title),
                )
                if (state is DailyVipUiState.UpgradeChoice) {
                    CloseAction(onDeclineUpgrade, R.string.feature_vip_impl_daily_vip_upgrade_decline)
                }
            }
            is DailyVipUiState.Failed -> {
                if (state.failure !is ContentFailure.AuthenticationRequired) {
                    ResonoteButton(
                        label = stringResource(
                            if (state.operation == DailyVipOperation.Claim) {
                                R.string.feature_vip_impl_daily_vip_retry_claim
                            } else {
                                R.string.feature_vip_impl_daily_vip_retry_upgrade
                            },
                        ),
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                CloseAction(onDismiss, R.string.feature_vip_impl_daily_vip_done)
            }
            is DailyVipUiState.ClaimComplete,
            is DailyVipUiState.UpgradeComplete,
            is DailyVipUiState.RiskBlocked,
            -> CloseAction(onDismiss, R.string.feature_vip_impl_daily_vip_done)
        }
    }
}

@Composable
private fun CloseAction(onClick: () -> Unit, @StringRes label: Int) {
    ResonoteTextButton(
        label = stringResource(label),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    )
}

private data class DialogPresentation(
    val icon: ImageVector,
    @param:StringRes val title: Int,
    @param:StringRes val body: Int,
    val isError: Boolean = false,
)

private fun DailyVipUiState.presentation(): DialogPresentation = when (this) {
    is DailyVipUiState.Ready, is DailyVipUiState.Claiming -> DialogPresentation(
        Icons.Rounded.CardGiftcard,
        R.string.feature_vip_impl_daily_vip_claim,
        R.string.feature_vip_impl_daily_vip_ready_body,
    )
    is DailyVipUiState.UpgradeChoice -> DialogPresentation(
        Icons.Rounded.Star,
        R.string.feature_vip_impl_daily_vip_upgrade_dialog_title,
        if (alreadyClaimed) {
            R.string.feature_vip_impl_daily_vip_upgrade_dialog_already
        } else {
            R.string.feature_vip_impl_daily_vip_upgrade_dialog_claimed
        },
    )
    is DailyVipUiState.ClaimComplete -> DialogPresentation(
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
    )
    is DailyVipUiState.Upgrading -> DialogPresentation(
        Icons.Rounded.Star,
        R.string.feature_vip_impl_daily_vip_upgrading_title,
        R.string.feature_vip_impl_daily_vip_upgrading_body,
    )
    is DailyVipUiState.UpgradeComplete -> DialogPresentation(
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
    )
    is DailyVipUiState.RiskBlocked -> DialogPresentation(
        Icons.Rounded.Lock,
        R.string.feature_vip_impl_daily_vip_risk_title,
        R.string.feature_vip_impl_daily_vip_risk_body,
        isError = true,
    )
    is DailyVipUiState.Failed -> DialogPresentation(
        Icons.Rounded.ErrorOutline,
        R.string.feature_vip_impl_daily_vip_error_title,
        R.string.feature_vip_impl_daily_vip_error_protocol,
        isError = true,
    )
}

@Composable
private fun DialogPresentation.body(state: DailyVipUiState): String = if (state is DailyVipUiState.Failed) {
    stringResource(
        when (state.failure) {
            ContentFailure.AuthenticationRequired -> R.string.feature_vip_impl_daily_vip_error_auth
            ContentFailure.Network -> R.string.feature_vip_impl_daily_vip_error_network
            ContentFailure.ServiceRejected -> R.string.feature_vip_impl_daily_vip_error_service
            is ContentFailure.RiskVerificationRequired,
            ContentFailure.RiskBlocked,
            -> R.string.feature_vip_impl_daily_vip_risk_body
            ContentFailure.Protocol -> R.string.feature_vip_impl_daily_vip_error_protocol
        },
    )
} else {
    stringResource(body)
}

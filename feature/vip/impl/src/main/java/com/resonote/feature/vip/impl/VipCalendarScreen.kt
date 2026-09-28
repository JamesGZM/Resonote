package com.resonote.feature.vip.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.model.ContentFailure
import com.resonote.core.model.RiskChallengeHandle
import java.time.YearMonth

@Composable
fun VipCalendarRoute(viewModel: VipCalendarViewModel, onBack: () -> Unit, onVerify: (RiskChallengeHandle) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }
    VipCalendarScreen(
        state, onBack, viewModel::refresh, viewModel::previousMonth, viewModel::nextMonth,
        viewModel::setAutomatic, viewModel::signIn, onVerify, viewModel.canVerify(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VipCalendarScreen(
    state: VipCalendarState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onAutomatic: (Boolean) -> Unit,
    onSignIn: () -> Unit,
    onVerify: (RiskChallengeHandle) -> Unit,
    canVerify: Boolean = true,
) {
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.feature_vip_impl_vip_calendar_title)) }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.feature_vip_impl_vip_calendar_back))
            }
        })
    }) { padding ->
        PullToRefreshBox(isRefreshing = state.loading, onRefresh = onRefresh, modifier = Modifier.padding(padding)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                val autoLabel = stringResource(R.string.feature_vip_impl_vip_calendar_auto)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.feature_vip_impl_vip_calendar_auto),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(R.string.feature_vip_impl_vip_calendar_auto_body),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = state.automatic,
                        onCheckedChange = onAutomatic,
                        modifier = Modifier.semantics { contentDescription = autoLabel },
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onPrevious) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                    stringResource(R.string.feature_vip_impl_vip_calendar_previous),
                                )
                            }
                            Text(
                                stringResource(
                                    R.string.feature_vip_impl_vip_calendar_month,
                                    state.month.year,
                                    state.month.monthValue,
                                ),
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            IconButton(onClick = onNext, enabled = state.month < YearMonth.from(state.today)) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    stringResource(R.string.feature_vip_impl_vip_calendar_next),
                                )
                            }
                        }
                        val weekdays = stringResource(R.string.feature_vip_impl_vip_calendar_weekdays).split(",")
                        Row {
                            weekdays.forEach {
                                Text(
                                    it,
                                    Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        val leading = state.month.atDay(1).dayOfWeek.value - 1
                        val cells = ((leading + state.month.lengthOfMonth() + 6) / 7) * 7
                        for (week in 0 until cells / 7) {
                            Row {
                                for (weekday in 0..6) {
                                    val number = week * 7 + weekday - leading + 1
                                    if (number !in 1..state.month.lengthOfMonth()) {
                                        Spacer(Modifier.weight(1f).height(48.dp))
                                    } else {
                                        val date = state.month.atDay(number)
                                        val signed = state.records.any { it.date == date && it.signed }
                                        val today = date == state.today
                                        val dateLabel = if (signed) {
                                            R.string.feature_vip_impl_vip_calendar_signed_date
                                        } else {
                                            R.string.feature_vip_impl_vip_calendar_empty_date
                                        }
                                        val description = stringResource(dateLabel, date.toString())
                                        val background = if (today) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerLow
                                        }
                                        Column(
                                            Modifier.weight(1f).heightIn(min = 48.dp)
                                                .background(background, CircleShape)
                                                .semantics {
                                                    contentDescription = description
                                                },
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Text(number.toString(), style = MaterialTheme.typography.bodyLarge)
                                            if (signed) Text("✓", color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                        Text(
                            stringResource(
                                R.string.feature_vip_impl_vip_calendar_count,
                                state.records.count {
                                    it.signed &&
                                        YearMonth.from(it.date) == state.month
                                },
                            ),
                        )
                    }
                }
                Text(
                    stringResource(R.string.feature_vip_impl_vip_calendar_note),
                    style = MaterialTheme.typography.bodySmall,
                )
                state.error?.let { Text(stringResource(it.calendarMessage()), color = MaterialTheme.colorScheme.error) }
                if (state.error != null && state.records.isEmpty()) {
                    TextButton(onClick = onRefresh, enabled = !state.loading) {
                        Text(stringResource(R.string.feature_vip_impl_vip_calendar_retry))
                    }
                }
                state.result?.let { result ->
                    Text(
                        stringResource(
                            when {
                                result.upgraded -> R.string.feature_vip_impl_vip_calendar_success
                                result.claimed -> R.string.feature_vip_impl_vip_calendar_partial
                                else -> R.string.feature_vip_impl_vip_calendar_failed
                            },
                        ),
                    )
                    result.failure?.let { Text(stringResource(it.calendarMessage())) }
                    val risk = result.failure as? ContentFailure.RiskVerificationRequired
                    if (risk != null && canVerify) {
                        Button(onClick = {
                            onVerify(risk.challenge)
                        }) { Text(stringResource(R.string.feature_vip_impl_vip_calendar_verify)) }
                    }
                }
                Button(onClick = onSignIn, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                    val label = if (state.busy) {
                        R.string.feature_vip_impl_vip_calendar_busy
                    } else {
                        R.string.feature_vip_impl_vip_calendar_manual
                    }
                    Text(stringResource(label))
                }
                Spacer(Modifier.height(128.dp))
            }
        }
    }
}

private fun ContentFailure.calendarMessage(): Int = when (this) {
    ContentFailure.Network -> R.string.feature_vip_impl_vip_calendar_network
    ContentFailure.AuthenticationRequired -> R.string.feature_vip_impl_vip_calendar_login
    ContentFailure.RiskBlocked -> R.string.feature_vip_impl_vip_calendar_blocked
    is ContentFailure.RiskVerificationRequired -> R.string.feature_vip_impl_vip_calendar_verification
    ContentFailure.Protocol -> R.string.feature_vip_impl_vip_calendar_protocol
    ContentFailure.ServiceRejected -> R.string.feature_vip_impl_vip_calendar_failed
}

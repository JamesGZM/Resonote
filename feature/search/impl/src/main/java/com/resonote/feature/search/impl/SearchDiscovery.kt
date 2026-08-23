package com.resonote.feature.search.impl

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.component.ResonoteEmptyState
import com.resonote.core.designsystem.component.ResonoteErrorState
import com.resonote.core.designsystem.component.ResonotePlainAction
import com.resonote.core.designsystem.tokens.ResonoteTokens
import com.resonote.core.model.ContentFailure

@Composable
private fun SearchSectionHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = ResonoteTokens.spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(ResonoteTokens.spacing.space2))
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        trailingContent?.invoke()
    }
}

@Composable
private fun SearchHeaderTextAction(label: String, onClick: () -> Unit) {
    ResonotePlainAction(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
            Text(
                label,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
internal fun SearchDiscovery(
    history: List<String>,
    hotKeywords: List<String>,
    suggestions: List<String>,
    onRemoveHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onKeywordClick: (String) -> Unit,
    bottomContentPadding: Dp,
) {
    var isEditingHistory by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomContentPadding),
    ) {
        if (suggestions.isNotEmpty()) {
            item {
                SearchSectionHeader(
                    stringResource(R.string.feature_search_impl_search_suggestions),
                    Icons.Rounded.Search,
                )
            }
            itemsIndexed(suggestions, key = { index, value -> "suggestion-$value-$index" }) { _, suggestion ->
                SearchSuggestionRow(
                    label = suggestion,
                    icon = Icons.Rounded.Search,
                    trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                    onClick = { onKeywordClick(suggestion) },
                )
            }
        } else {
            if (history.isNotEmpty()) {
                item {
                    SearchSectionHeader(
                        title = stringResource(R.string.feature_search_impl_search_history),
                        icon = Icons.Rounded.History,
                        trailingContent = {
                            if (isEditingHistory) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SearchHeaderTextAction(
                                        label = stringResource(R.string.feature_search_impl_search_history_clear),
                                        onClick = {
                                            isEditingHistory = false
                                            onClearHistory()
                                        },
                                    )
                                    SearchHeaderTextAction(
                                        label = stringResource(R.string.feature_search_impl_search_history_done),
                                        onClick = { isEditingHistory = false },
                                    )
                                }
                            } else {
                                SearchHeaderTextAction(
                                    label = stringResource(R.string.feature_search_impl_search_history_edit),
                                    onClick = { isEditingHistory = true },
                                )
                            }
                        },
                    )
                }
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = ResonoteTokens.spacing.space4),
                        horizontalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space2),
                        verticalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space2),
                    ) {
                        history.forEach { query ->
                            HistoryKeywordChip(
                                query = query,
                                editing = isEditingHistory,
                                removeLabel = stringResource(
                                    R.string.feature_search_impl_search_history_remove,
                                    query,
                                ),
                                onClick = { onKeywordClick(query) },
                                onRemove = { onRemoveHistory(query) },
                            )
                        }
                    }
                }
            }
            if (hotKeywords.isNotEmpty()) {
                item {
                    SearchSectionHeader(
                        stringResource(R.string.feature_search_impl_search_hot),
                        Icons.Rounded.GraphicEq,
                        modifier = if (history.isNotEmpty()) {
                            Modifier.padding(top = ResonoteTokens.spacing.space3)
                        } else {
                            Modifier
                        },
                    )
                }
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = ResonoteTokens.spacing.space4),
                        horizontalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space2),
                        verticalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space2),
                    ) {
                        hotKeywords.forEachIndexed { index, keyword ->
                            HotKeywordChip(
                                rank = index + 1,
                                keyword = keyword,
                                onClick = { onKeywordClick(keyword) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryKeywordChip(
    query: String,
    editing: Boolean,
    removeLabel: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        onClick = if (editing) onRemove else onClick,
        modifier = Modifier
            .height(36.dp)
            .then(if (editing) Modifier.semantics { contentDescription = removeLabel } else Modifier),
        shape = ResonoteTokens.shapes.full,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(ResonoteTokens.borders.hairline, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(start = 13.dp, end = if (editing) 9.dp else 13.dp),
            horizontalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(query, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (editing) {
                Icon(
                    Icons.Rounded.Clear,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SearchSuggestionRow(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    trailingIcon: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick)
            .padding(start = ResonoteTokens.spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(ResonoteTokens.spacing.space3))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        when {
            trailingContent != null -> trailingContent()
            trailingIcon != null -> Box(
                modifier = Modifier.size(ResonoteTokens.touchTargets.minimum),
                contentAlignment = Alignment.Center,
            ) {
                Icon(trailingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HotKeywordChip(rank: Int, keyword: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.height(36.dp),
        shape = ResonoteTokens.shapes.full,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(ResonoteTokens.borders.hairline, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = rank.toString(),
                color = if (rank <=
                    3
                ) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(keyword, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
internal fun LoadingState(query: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text(
                stringResource(R.string.feature_search_impl_search_loading, query),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun EmptyState() = ResonoteEmptyState(
    title = stringResource(R.string.feature_search_impl_search_empty_title),
    message = stringResource(R.string.feature_search_impl_search_empty_body),
)

@Composable
internal fun ErrorState(failure: ContentFailure, onRetry: () -> Unit) {
    val body = when (failure) {
        ContentFailure.Network -> stringResource(R.string.feature_search_impl_search_error_network)
        ContentFailure.AuthenticationRequired -> stringResource(R.string.feature_search_impl_search_error_auth)
        else -> stringResource(R.string.feature_search_impl_search_error_generic)
    }
    ResonoteErrorState(
        onRetry = onRetry,
        title = stringResource(R.string.feature_search_impl_search_error_title),
        message = body,
        retryLabel = stringResource(R.string.feature_search_impl_search_retry),
    )
}

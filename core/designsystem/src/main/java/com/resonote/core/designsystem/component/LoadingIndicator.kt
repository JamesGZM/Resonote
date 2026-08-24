package com.resonote.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ResonoteCircularLoadingIndicator(modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        modifier = modifier.size(20.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.primaryContainer,
        strokeWidth = 2.5.dp,
    )
}

package com.resonote.catalog

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.resonote.core.designsystem.component.ResonoteButton
import com.resonote.core.designsystem.component.ResonoteDestructiveButton
import com.resonote.core.designsystem.component.ResonoteDestructiveTextButton
import com.resonote.core.designsystem.component.ResonoteFilledIconButton
import com.resonote.core.designsystem.component.ResonoteIconButton
import com.resonote.core.designsystem.component.ResonoteIconToggleButton
import com.resonote.core.designsystem.component.ResonoteOutlinedButton
import com.resonote.core.designsystem.component.ResonoteOutlinedIconButton
import com.resonote.core.designsystem.component.ResonoteTextButton
import com.resonote.core.designsystem.component.ResonoteTonalButton
import com.resonote.core.designsystem.component.ResonoteTonalIconButton
import com.resonote.core.designsystem.tokens.ResonoteTokens

@Composable
internal fun ActionsCatalog() {
    var favorite by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = ResonoteTokens.spacing.space4),
        verticalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space4),
    ) {
        CatalogGroupTitle("Button variants")
        ScrollableActionRow {
            ResonoteButton(label = "Play", onClick = {})
            ResonoteTonalButton(label = "Save", onClick = {})
            ResonoteOutlinedButton(label = "Add", onClick = {})
            ResonoteTextButton(label = "Details", onClick = {})
        }

        CatalogGroupTitle("Leading icon and destructive")
        ScrollableActionRow {
            ResonoteButton(
                label = "Add to library",
                onClick = {},
                leadingIcon = { CatalogIcon(Icons.Outlined.Add) },
            )
            ResonoteDestructiveButton(
                label = "Delete",
                onClick = {},
                leadingIcon = { CatalogIcon(Icons.Outlined.Delete) },
            )
            ResonoteDestructiveTextButton(label = "Remove", onClick = {})
        }

        CatalogGroupTitle("Disabled and loading")
        ScrollableActionRow {
            ResonoteButton(label = "Disabled", onClick = {}, enabled = false)
            ResonoteTonalButton(
                label = "Save",
                loadingLabel = "Saving…",
                onClick = {},
                loading = true,
            )
            ResonoteOutlinedButton(
                label = "Download",
                loadingLabel = "Downloading…",
                onClick = {},
                loading = true,
                leadingIcon = { CatalogIcon(Icons.Outlined.Add) },
            )
        }

        CatalogGroupTitle("Icon button variants")
        ScrollableActionRow {
            ResonoteIconButton(
                label = "Add",
                onClick = {},
                icon = { CatalogIcon(Icons.Outlined.Add) },
            )
            ResonoteFilledIconButton(
                label = "Favorite",
                onClick = {},
                icon = { CatalogIcon(Icons.Filled.Favorite) },
            )
            ResonoteTonalIconButton(
                label = "Delete",
                onClick = {},
                icon = { CatalogIcon(Icons.Outlined.Delete) },
            )
            ResonoteOutlinedIconButton(
                label = "Add",
                onClick = {},
                icon = { CatalogIcon(Icons.Outlined.Add) },
            )
            ResonoteIconButton(
                label = "Disabled favorite",
                onClick = {},
                enabled = false,
                icon = { CatalogIcon(Icons.Outlined.FavoriteBorder) },
            )
        }

        CatalogGroupTitle("Toggle icon button")
        ResonoteIconToggleButton(
            checked = favorite,
            label = if (favorite) "Remove from favorites" else "Add to favorites",
            onCheckedChange = { favorite = it },
            icon = { CatalogIcon(Icons.Outlined.FavoriteBorder) },
            checkedIcon = { CatalogIcon(Icons.Filled.Favorite) },
        )
    }
}

@Composable
private fun ScrollableActionRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space2),
    ) {
        content()
    }
}

@Composable
private fun CatalogGroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CatalogIcon(imageVector: androidx.compose.ui.graphics.vector.ImageVector) {
    Icon(imageVector = imageVector, contentDescription = null)
}

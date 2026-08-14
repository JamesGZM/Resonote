package com.resonote.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.resonote.core.designsystem.component.ResonoteIconButton
import com.resonote.core.designsystem.component.ResonoteNavigationSuiteScaffold
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.designsystem.tokens.ResonoteTokens

private data class CatalogDestination(val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val CatalogDestinations = listOf(
    CatalogDestination("Foundation", Icons.Outlined.Palette, Icons.Filled.Palette),
    CatalogDestination("Components", Icons.Outlined.Widgets, Icons.Filled.Widgets),
    CatalogDestination("Music", Icons.Outlined.LibraryMusic, Icons.Filled.LibraryMusic),
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun AdaptiveCatalogScreen(themeMode: ResonoteThemeMode, onThemeModeChange: (ResonoteThemeMode) -> Unit) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    ResonoteNavigationSuiteScaffold(
        navigationSuiteItems = {
            CatalogDestinations.forEachIndexed { index, destination ->
                item(
                    selected = selectedIndex == index,
                    onClick = { selectedIndex = index },
                    icon = {
                        Icon(imageVector = destination.icon, contentDescription = null)
                    },
                    selectedIcon = {
                        Icon(imageVector = destination.selectedIcon, contentDescription = null)
                    },
                    label = { Text(destination.label) },
                )
            }
        },
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                ResonoteTopAppBar(
                    title = { Text(CatalogDestinations[selectedIndex].label) },
                    actions = {
                        ResonoteIconButton(
                            label = "Catalog information",
                            onClick = {},
                            icon = {
                                Icon(imageVector = Icons.Outlined.Info, contentDescription = null)
                            },
                        )
                        ResonoteIconButton(
                            label = "Catalog settings",
                            onClick = {},
                            icon = {
                                Icon(imageVector = Icons.Outlined.Settings, contentDescription = null)
                            },
                        )
                    },
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
                    ),
            ) {
                when (selectedIndex) {
                    0 -> FoundationCatalog(themeMode, onThemeModeChange)
                    1 -> CatalogPlaceholder(
                        title = "Component catalog",
                        description = "Buttons, icon buttons, text fields, navigation, and app bars.",
                    )
                    else -> CatalogPlaceholder(
                        title = "Music extensions",
                        description = "Album, song, metadata, and quality components will appear here.",
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogPlaceholder(title: String, description: String) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = ResonoteTokens.spacing.space4,
            vertical = ResonoteTokens.spacing.space6,
        ),
        verticalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space2),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space2)) {
                Text(text = title, style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

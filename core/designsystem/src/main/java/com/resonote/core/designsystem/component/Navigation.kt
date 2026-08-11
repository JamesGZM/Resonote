package com.resonote.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItemColors
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Resonote primary navigation that follows Material adaptive Bar/Rail selection.
 *
 * Navigation state and destination identity remain owned by the caller. Window changes only alter
 * the Material navigation presentation.
 */
@Composable
fun ResonoteNavigationSuiteScaffold(
    navigationSuiteItems: ResonoteNavigationSuiteScope.() -> Unit,
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val itemColors = NavigationSuiteItemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = colors.onSecondaryContainer,
            unselectedIconColor = colors.onSurfaceVariant,
            selectedTextColor = colors.onSecondaryContainer,
            unselectedTextColor = colors.onSurfaceVariant,
            indicatorColor = colors.secondaryContainer,
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = colors.onSecondaryContainer,
            unselectedIconColor = colors.onSurfaceVariant,
            selectedTextColor = colors.onSecondaryContainer,
            unselectedTextColor = colors.onSurfaceVariant,
            indicatorColor = colors.secondaryContainer,
        ),
        navigationDrawerItemColors = NavigationDrawerItemDefaults.colors(
            selectedIconColor = colors.onSecondaryContainer,
            unselectedIconColor = colors.onSurfaceVariant,
            selectedTextColor = colors.onSecondaryContainer,
            unselectedTextColor = colors.onSurfaceVariant,
            selectedContainerColor = colors.secondaryContainer,
        ),
    )

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            ResonoteNavigationSuiteScope(
                navigationSuiteScope = this,
                itemColors = itemColors,
            ).run(navigationSuiteItems)
        },
        modifier = modifier,
        layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(windowAdaptiveInfo),
        containerColor = Color.Transparent,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = colors.surfaceContainer,
            navigationRailContainerColor = colors.surface,
        ),
        content = content,
    )
}

/** Declares items without owning their identity, selection, or navigation state. */
class ResonoteNavigationSuiteScope internal constructor(
    private val navigationSuiteScope: NavigationSuiteScope,
    private val itemColors: NavigationSuiteItemColors,
) {
    fun item(
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        icon: @Composable () -> Unit,
        selectedIcon: @Composable () -> Unit = icon,
        label: @Composable (() -> Unit)? = null,
    ) = navigationSuiteScope.item(
        selected = selected,
        onClick = onClick,
        icon = { if (selected) selectedIcon() else icon() },
        modifier = modifier,
        label = label,
        colors = itemColors,
    )
}

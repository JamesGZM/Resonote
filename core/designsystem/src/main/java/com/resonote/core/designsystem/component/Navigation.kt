package com.resonote.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItemColors
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

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
    val items = mutableListOf<ResonoteNavigationSuiteItem>()
    ResonoteNavigationSuiteScope(items::add).navigationSuiteItems()
    val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(windowAdaptiveInfo)
    val itemColors = NavigationSuiteItemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = colors.primary,
            unselectedIconColor = colors.onSurfaceVariant,
            selectedTextColor = colors.primary,
            unselectedTextColor = colors.onSurfaceVariant,
            indicatorColor = Color.Transparent,
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = colors.primary,
            unselectedIconColor = colors.onSurfaceVariant,
            selectedTextColor = colors.primary,
            unselectedTextColor = colors.onSurfaceVariant,
            indicatorColor = Color.Transparent,
        ),
        navigationDrawerItemColors = NavigationDrawerItemDefaults.colors(
            selectedIconColor = colors.primary,
            unselectedIconColor = colors.onSurfaceVariant,
            selectedTextColor = colors.primary,
            unselectedTextColor = colors.onSurfaceVariant,
            selectedContainerColor = Color.Transparent,
        ),
    )

    if (layoutType == NavigationSuiteType.NavigationBar) {
        ResonoteCompactNavigationScaffold(
            items = items,
            modifier = modifier,
            content = content,
        )
        return
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            items.forEach { item ->
                item(
                    selected = item.selected,
                    onClick = item.onClick,
                    icon = { if (item.selected) item.selectedIcon() else item.icon() },
                    modifier = item.modifier,
                    label = item.label,
                    colors = itemColors,
                )
            }
        },
        modifier = modifier,
        layoutType = layoutType,
        containerColor = Color.Transparent,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = colors.surface,
            navigationRailContainerColor = colors.surface,
        ),
        content = content,
    )
}

/** Declares items without owning their identity, selection, or navigation state. */
class ResonoteNavigationSuiteScope internal constructor(
    private val addItem: (ResonoteNavigationSuiteItem) -> Unit,
) {
    fun item(
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        icon: @Composable () -> Unit,
        selectedIcon: @Composable () -> Unit = icon,
        label: @Composable (() -> Unit)? = null,
    ) = addItem(
        ResonoteNavigationSuiteItem(
            selected = selected,
            onClick = onClick,
            modifier = modifier,
            icon = icon,
            selectedIcon = selectedIcon,
            label = label,
        ),
    )
}

internal data class ResonoteNavigationSuiteItem(
    val selected: Boolean,
    val onClick: () -> Unit,
    val modifier: Modifier,
    val icon: @Composable () -> Unit,
    val selectedIcon: @Composable () -> Unit,
    val label: @Composable (() -> Unit)?,
)

@Composable
private fun ResonoteCompactNavigationScaffold(
    items: List<ResonoteNavigationSuiteItem>,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { ResonoteCompactNavigationBar(items) },
    ) { contentPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding),
        ) {
            content()
        }
    }
}

@Composable
private fun ResonoteCompactNavigationBar(items: List<ResonoteNavigationSuiteItem>) {
    val colors = MaterialTheme.colorScheme
    val windowInsets = NavigationBarDefaults.windowInsets
    val bottomInset = windowInsets.asPaddingValues().calculateBottomPadding()
    Box {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = -CompactNavigationTopShadowHeight)
                .fillMaxWidth()
                .height(CompactNavigationTopShadowHeight)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.7f to Color.Black.copy(alpha = 0.04f),
                        1f to Color.Black.copy(alpha = 0.14f),
                    ),
                ),
        )
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(CompactNavigationItemHeight + bottomInset)
                .testTag("resonote-navigation-bar"),
            containerColor = colors.surface,
            tonalElevation = 0.dp,
            windowInsets = windowInsets,
        ) {
            items.forEachIndexed { index, item ->
                key(index) {
                    ResonoteCompactNavigationItem(item)
                }
            }
        }
    }
}

@Composable
private fun RowScope.ResonoteCompactNavigationItem(item: ResonoteNavigationSuiteItem) {
    val colors = MaterialTheme.colorScheme
    val contentColor by animateColorAsState(
        targetValue = if (item.selected) colors.primary else colors.onSurfaceVariant,
        label = "Navigation item color",
    )
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = item.modifier
            .weight(1f)
            .fillMaxHeight()
            .selectable(
                selected = item.selected,
                onClick = item.onClick,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = false,
                    radius = CompactNavigationRippleRadius,
                    color = colors.primary,
                ),
            ),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            if (item.selected) item.selectedIcon() else item.icon()
            item.label?.let { label ->
                Spacer(Modifier.height(CompactNavigationIconLabelSpacing))
                ProvideTextStyle(MaterialTheme.typography.labelSmall) {
                    label()
                }
            }
        }
    }
}

private val CompactNavigationItemHeight = 64.dp
private val CompactNavigationTopShadowHeight = 12.dp
private val CompactNavigationIconLabelSpacing = 2.dp
private val CompactNavigationRippleRadius = 42.dp

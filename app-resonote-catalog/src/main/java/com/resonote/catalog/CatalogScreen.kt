package com.resonote.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resonote.core.designsystem.theme.ResonoteThemeMode
import com.resonote.core.designsystem.tokens.ResonoteTokens

private data class ColorEntry(val name: String, val color: Color)
private data class TypeEntry(val name: String, val style: TextStyle)
private data class ShapeEntry(val name: String, val shape: Shape)

@Composable
internal fun FoundationCatalog(
    themeMode: ResonoteThemeMode,
    onThemeModeChange: (ResonoteThemeMode) -> Unit,
) {
    val colors = colorEntries()
    val typography = typeEntries()
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = ResonoteTokens.spacing.space4,
                vertical = ResonoteTokens.spacing.space6,
            ),
            verticalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space2),
        ) {
            item {
                Text("Resonote Foundation Catalog", style = MaterialTheme.typography.headlineMedium)
                Text(
                    modifier = Modifier.padding(top = ResonoteTokens.spacing.space1),
                    text = "Material3 1.4.0 · Foundation 00–05 token evidence",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ThemeModeSelector(themeMode, onThemeModeChange)
            }

            item { SectionTitle("ColorScheme · 48 roles") }
            items(colors, key = { it.name }) { entry -> ColorSwatch(entry) }

            item { SectionTitle("Typography · 15 roles") }
            items(typography, key = { it.name }) { entry ->
                Column(modifier = Modifier.padding(vertical = ResonoteTokens.spacing.space1)) {
                    Text(entry.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Resonote 乐律 · Music 123", style = entry.style)
                }
            }

            item {
                SectionTitle("Shapes")
                ShapeGallery()
                SectionTitle("Extended foundation tokens")
                ExtendedTokenValues()
                SectionTitle("06A · Buttons & Actions")
                ActionsCatalog()
                SectionTitle("06B-1 · Text Field")
                InputsCatalog()
                Spacer(Modifier.height(ResonoteTokens.spacing.space6))
                Text(
                    text = "Catalog 是实现证据入口，不代表 V-01–V-10 已通过。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ThemeModeSelector(
    selected: ResonoteThemeMode,
    onSelected: (ResonoteThemeMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = ResonoteTokens.spacing.space4),
        horizontalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space2),
    ) {
        ResonoteThemeMode.entries.forEach { mode ->
            FilterChip(
                selected = mode == selected,
                onClick = { onSelected(mode) },
                label = { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)) },
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Column(modifier = Modifier.padding(top = ResonoteTokens.spacing.space6)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            modifier = Modifier.padding(top = ResonoteTokens.spacing.space4),
            text = title,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun ColorSwatch(entry: ColorEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ResonoteTokens.spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(ResonoteTokens.spacing.space2))
                .background(entry.color)
                .border(
                    width = ResonoteTokens.borders.hairline,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(ResonoteTokens.spacing.space2),
                ),
        )
        Spacer(Modifier.width(ResonoteTokens.spacing.space3))
        Column {
            Text(entry.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = entry.color.toHex(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ShapeGallery() {
    val shapes = MaterialTheme.shapes
    val artworkShapes = ResonoteTokens.artworkShapes
    val entries = listOf(
        ShapeEntry("extraSmall · 4dp", shapes.extraSmall),
        ShapeEntry("small · 8dp", shapes.small),
        ShapeEntry("medium · 12dp", shapes.medium),
        ShapeEntry("large · 16dp", shapes.large),
        ShapeEntry("extraLarge · 28dp", shapes.extraLarge),
        ShapeEntry("artwork standard · 12dp", artworkShapes.standard),
        ShapeEntry("artwork compact · 8dp", artworkShapes.compact),
        ShapeEntry("artwork hero · 16dp", artworkShapes.hero),
        ShapeEntry("none", ResonoteTokens.shapes.none),
        ShapeEntry("full", ResonoteTokens.shapes.full),
    )
    Column(verticalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space2)) {
        entries.forEach { entry ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = entry.shape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    modifier = Modifier.padding(ResonoteTokens.spacing.space4),
                    text = entry.name,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ExtendedTokenValues() {
    val spacing = ResonoteTokens.spacing
    val elevation = ResonoteTokens.elevation
    val icons = ResonoteTokens.icons
    val artwork = ResonoteTokens.artwork
    val state = ResonoteTokens.stateLayers
    val layout = ResonoteTokens.layout
    val elevationLevels = listOf(
        elevation.level0,
        elevation.level1,
        elevation.level2,
        elevation.level3,
        elevation.level4,
        elevation.level5,
    )
    val contentScale = if (artwork.contentScale == ContentScale.Crop) "Crop" else "Custom"
    val alignment = if (artwork.alignment == Alignment.Center) "Center" else "Custom"

    Column(verticalArrangement = Arrangement.spacedBy(ResonoteTokens.spacing.space2)) {
        TokenRow("Spacing", "${spacing.space0}, ${spacing.space1}, ${spacing.space2}, ${spacing.space3}, ${spacing.space4}, ${spacing.space6}, ${spacing.space8}, ${spacing.space10}, ${spacing.space12}, ${spacing.space16}")
        TokenRow("Borders", "hairline ${ResonoteTokens.borders.hairline} · strong ${ResonoteTokens.borders.strong}")
        TokenRow("Touch target", ResonoteTokens.touchTargets.minimum.toString())
        TokenRow("Elevation tonal", elevationLevels.joinToString { it.tonal.toString() })
        TokenRow("Elevation surfaces", elevationLevels.joinToString { it.preferredSurfaceRole.name })
        TokenRow("Icon sizes", "${icons.small}, ${icons.default}, ${icons.large}, ${icons.display} · target ${icons.touchTarget}")
        TokenRow("Artwork", "ratio ${artwork.aspectRatio}:1 · overlay ${artwork.overlayInset} · $contentScale / $alignment")
        TokenRow(
            "Artwork placeholders",
            "loading ${artwork.loadingContainerRole.name} · missing ${artwork.missingContainerRole.name}/${artwork.missingContentRole.name} · error ${artwork.errorContainerRole.name}/${artwork.errorContentRole.name}",
        )
        TokenRow("State opacity", "hover ${state.hoverOpacity} · focus ${state.focusOpacity} · pressed ${state.pressedOpacity} · dragged ${state.draggedOpacity}")
        TokenRow("Grid columns", "${layout.compactColumns} / ${layout.mediumColumns} / ${layout.expandedColumns}")
        TokenRow("Body widths", "expanded ${layout.expandedMaximumBodyWidth} · reading ${layout.readingMaximumWidth}")
        TokenRow("Motion", "Instant · Effects Fast/Default/Slow · Spatial Fast/Default/Slow")
        TokenRow("System shadow", ResonoteTokens.systemColors.shadow.toHex())
    }
}

@Composable
private fun TokenRow(name: String, value: String) {
    Column {
        Text(name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun colorEntries(): List<ColorEntry> = with(MaterialTheme.colorScheme) {
    listOf(
        ColorEntry("primary", primary), ColorEntry("onPrimary", onPrimary),
        ColorEntry("primaryContainer", primaryContainer), ColorEntry("onPrimaryContainer", onPrimaryContainer),
        ColorEntry("inversePrimary", inversePrimary), ColorEntry("secondary", secondary),
        ColorEntry("onSecondary", onSecondary), ColorEntry("secondaryContainer", secondaryContainer),
        ColorEntry("onSecondaryContainer", onSecondaryContainer), ColorEntry("tertiary", tertiary),
        ColorEntry("onTertiary", onTertiary), ColorEntry("tertiaryContainer", tertiaryContainer),
        ColorEntry("onTertiaryContainer", onTertiaryContainer), ColorEntry("background", background),
        ColorEntry("onBackground", onBackground), ColorEntry("surface", surface),
        ColorEntry("onSurface", onSurface), ColorEntry("surfaceVariant", surfaceVariant),
        ColorEntry("onSurfaceVariant", onSurfaceVariant), ColorEntry("surfaceTint", surfaceTint),
        ColorEntry("inverseSurface", inverseSurface), ColorEntry("inverseOnSurface", inverseOnSurface),
        ColorEntry("error", error), ColorEntry("onError", onError),
        ColorEntry("errorContainer", errorContainer), ColorEntry("onErrorContainer", onErrorContainer),
        ColorEntry("outline", outline), ColorEntry("outlineVariant", outlineVariant),
        ColorEntry("scrim", scrim), ColorEntry("surfaceBright", surfaceBright),
        ColorEntry("surfaceDim", surfaceDim), ColorEntry("surfaceContainer", surfaceContainer),
        ColorEntry("surfaceContainerHigh", surfaceContainerHigh),
        ColorEntry("surfaceContainerHighest", surfaceContainerHighest),
        ColorEntry("surfaceContainerLow", surfaceContainerLow),
        ColorEntry("surfaceContainerLowest", surfaceContainerLowest),
        ColorEntry("primaryFixed", primaryFixed), ColorEntry("primaryFixedDim", primaryFixedDim),
        ColorEntry("onPrimaryFixed", onPrimaryFixed), ColorEntry("onPrimaryFixedVariant", onPrimaryFixedVariant),
        ColorEntry("secondaryFixed", secondaryFixed), ColorEntry("secondaryFixedDim", secondaryFixedDim),
        ColorEntry("onSecondaryFixed", onSecondaryFixed), ColorEntry("onSecondaryFixedVariant", onSecondaryFixedVariant),
        ColorEntry("tertiaryFixed", tertiaryFixed), ColorEntry("tertiaryFixedDim", tertiaryFixedDim),
        ColorEntry("onTertiaryFixed", onTertiaryFixed), ColorEntry("onTertiaryFixedVariant", onTertiaryFixedVariant),
    )
}

@Composable
private fun typeEntries(): List<TypeEntry> = with(MaterialTheme.typography) {
    listOf(
        TypeEntry("displayLarge", displayLarge), TypeEntry("displayMedium", displayMedium),
        TypeEntry("displaySmall", displaySmall), TypeEntry("headlineLarge", headlineLarge),
        TypeEntry("headlineMedium", headlineMedium), TypeEntry("headlineSmall", headlineSmall),
        TypeEntry("titleLarge", titleLarge), TypeEntry("titleMedium", titleMedium),
        TypeEntry("titleSmall", titleSmall), TypeEntry("bodyLarge", bodyLarge),
        TypeEntry("bodyMedium", bodyMedium), TypeEntry("bodySmall", bodySmall),
        TypeEntry("labelLarge", labelLarge), TypeEntry("labelMedium", labelMedium),
        TypeEntry("labelSmall", labelSmall),
    )
}

private fun Color.toHex(): String = "#" + toArgb().toUInt().toString(16).uppercase().padStart(8, '0')

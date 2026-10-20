package com.aweagle.arrowbloom.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Palettes.LAGOON.accent,
    onPrimary = OnAccent,
    secondary = Palettes.DAILY.accent,
    onSecondary = OnAccent,
    tertiary = StarGold,
    background = Paper,
    onBackground = Ink,
    surface = SurfaceCard,
    onSurface = Ink,
    surfaceVariant = SurfaceSoft,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
)

/**
 * Always light: dark ink lines on paper, the way a logic puzzle reads best. System bar icons
 * are set dark in MainActivity (`SystemBarStyle.light`), so they stay visible on the paper.
 */
@Composable
fun ArrowBloomTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}

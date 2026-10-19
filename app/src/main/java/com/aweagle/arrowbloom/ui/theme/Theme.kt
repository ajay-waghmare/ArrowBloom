package com.aweagle.arrowbloom.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ArrowCyan,
    secondary = ArrowPink,
    tertiary = ArrowGold,
    background = BgDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceCard,
    onPrimary = BgDark,
    onSecondary = BgDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
)

/**
 * Always dark: the neon arrows need a deep background. System bars are handled by
 * `enableEdgeToEdge()` in MainActivity and inset padding on each screen, not by the
 * deprecated window colour properties.
 */
@Composable
fun ArrowBloomTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColorScheme, content = content)
}

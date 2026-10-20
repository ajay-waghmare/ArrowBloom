package com.aweagle.arrowbloom.ui.theme

import androidx.compose.ui.graphics.Color

// Surfaces: a calm paper-white world; each garden tints the background.
val Paper = Color(0xFFF6F8FC)
val SurfaceCard = Color(0xFFFFFFFF)
val SurfaceSoft = Color(0xFFEEF1F7)
val SurfaceBorder = Color(0xFFE3E7F0)
val Scrim = Color(0x991D2A4D)

// Ink: the arrows and the text.
val Ink = Color(0xFF1D2A4D)
val TextPrimary = Ink
val TextSecondary = Color(0xFF5E6A86)
val TextMuted = Color(0xFF8E98AE)
val OnAccent = Color(0xFFFFFFFF)

// Board and game feedback
val DotColor = Color(0xFFC0C7D6)
val GridLine = Color(0xFFE2E6EF)
val AccentError = Color(0xFFE5484D)
val HeartRed = Color(0xFFF0435A)
val HeartEmpty = Color(0xFFD5DAE4)
val StarGold = Color(0xFFFFB400)
/** Gold dark enough for text on white. */
val GoldText = Color(0xFF9A6A00)
val StarEmpty = Color(0xFFDDE2EC)
val PetalPink = Color(0xFFFF8FB1)
val PetalGold = Color(0xFFFFC94D)

/**
 * One garden's look. [accent] is strong enough for white text on it (4.5:1 or better) and
 * drives the hint glow, the buttons and the garden name; the arrows stay [ink] everywhere,
 * so a board always reads the same way and only the world around it changes.
 */
data class GardenPalette(
    val bgTop: Color,
    val bgBottom: Color,
    val accent: Color,
    val ink: Color = Ink,
    val petals: List<Color>,
)

object Palettes {
    val MEADOW = GardenPalette(Color(0xFFE6F4E9), Color(0xFFFAFDFB), Color(0xFF237A4B), petals = listOf(Color(0xFF3DBE7A), PetalPink, PetalGold))
    val ORCHARD = GardenPalette(Color(0xFFFFEEDC), Color(0xFFFFFCF8), Color(0xFFB85A12), petals = listOf(Color(0xFFFF9A3D), PetalPink, PetalGold))
    val LAGOON = GardenPalette(Color(0xFFE2EEFD), Color(0xFFFAFCFF), Color(0xFF2563EB), petals = listOf(Color(0xFF5B9BFF), Color(0xFF5CE1E6), PetalGold))
    val DUSK = GardenPalette(Color(0xFFEDE7FA), Color(0xFFFCFBFF), Color(0xFF6A3FC4), petals = listOf(Color(0xFFA77BFF), PetalPink, PetalGold))
    val AURORA = GardenPalette(Color(0xFFE0F4F1), Color(0xFFFAFEFD), Color(0xFF0B7C75), petals = listOf(Color(0xFF2FD1C4), Color(0xFF8BE36B), PetalGold))
    val INFINITE = GardenPalette(Color(0xFFE9EDF8), Color(0xFFFBFCFE), Color(0xFF3049D8), petals = listOf(Color(0xFF6E86FF), PetalPink, PetalGold))

    val ALL = listOf(MEADOW, ORCHARD, LAGOON, DUSK, AURORA, INFINITE)

    /** The Daily Bloom has its own look so it never feels like "just another level". */
    val DAILY = GardenPalette(Color(0xFFFCE6EF), Color(0xFFFFFBFD), Color(0xFFC23468), petals = listOf(PetalPink, Color(0xFFFF5C93), PetalGold))

    fun forGarden(index: Int): GardenPalette = ALL[index.coerceIn(0, ALL.lastIndex)]
}

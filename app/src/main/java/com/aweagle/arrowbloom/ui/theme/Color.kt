package com.aweagle.arrowbloom.ui.theme

import androidx.compose.ui.graphics.Color

// Base surfaces: deep navy, the same family as Block Bloom so the two games read as siblings.
val BgDark = Color(0xFF0C101D)
val BgDeep = Color(0xFF070A14)
val SurfaceDark = Color(0xFF151C30)
val SurfaceCard = Color(0xFF1E2844)
val SurfaceBorder = Color(0xFF2E3D66)
val Scrim = Color(0xCC070A14)

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF8E9BB5)
val TextMuted = Color(0xFF5A6682)

// Brand accents
val ArrowCyan = Color(0xFF00E5FF)
val ArrowPink = Color(0xFFFF2A7A)
val ArrowGold = Color(0xFFFFB800)
val ArrowEmerald = Color(0xFF00E676)
val ArrowViolet = Color(0xFFA855F7)
val ArrowOrange = Color(0xFFFF6D00)
val ArrowBlossom = Color(0xFFFF8FB1)
val ArrowMint = Color(0xFF5CF2C5)

val AccentCombo = Color(0xFFFFD700)
val AccentWin = Color(0xFF00FFB2)
val AccentError = Color(0xFFFF3366)
val AccentHint = Color(0xFF00E5FF)
val GridCellBg = Color(0xFF13192B)
val GridCellBorder = Color(0xFF1F2942)

/**
 * One garden's look: a background tint, the cell colours and the six arrow colours.
 * Arrow colours are chosen to stay distinguishable for the common colour-vision deficiencies
 * (no red/green pair at the same brightness; each pair differs in luminance as well as hue).
 */
data class GardenPalette(
    val bgTop: Color,
    val bgBottom: Color,
    val cell: Color,
    val cellBorder: Color,
    val accent: Color,
    val arrows: List<Color>,
)

object Palettes {
    val MEADOW = GardenPalette(
        bgTop = Color(0xFF0E1A24), bgBottom = BgDeep, cell = Color(0xFF13202B), cellBorder = Color(0xFF1F3240), accent = ArrowMint,
        arrows = listOf(ArrowMint, ArrowGold, ArrowBlossom, ArrowCyan, ArrowViolet, ArrowOrange),
    )
    val ORCHARD = GardenPalette(
        bgTop = Color(0xFF1D1410), bgBottom = BgDeep, cell = Color(0xFF221A17), cellBorder = Color(0xFF3A2A24), accent = ArrowGold,
        arrows = listOf(ArrowGold, ArrowOrange, ArrowBlossom, ArrowMint, ArrowCyan, ArrowViolet),
    )
    val LAGOON = GardenPalette(
        bgTop = Color(0xFF0A1A2A), bgBottom = BgDeep, cell = Color(0xFF0F2031), cellBorder = Color(0xFF1A3349), accent = ArrowCyan,
        arrows = listOf(ArrowCyan, ArrowMint, ArrowGold, ArrowPink, ArrowViolet, ArrowOrange),
    )
    val DUSK = GardenPalette(
        bgTop = Color(0xFF1A1030), bgBottom = BgDeep, cell = Color(0xFF1C1532), cellBorder = Color(0xFF30254F), accent = ArrowViolet,
        arrows = listOf(ArrowViolet, ArrowPink, ArrowGold, ArrowCyan, ArrowMint, ArrowOrange),
    )
    val AURORA = GardenPalette(
        bgTop = Color(0xFF0B1730), bgBottom = BgDeep, cell = Color(0xFF111C33), cellBorder = Color(0xFF203055), accent = ArrowEmerald,
        arrows = listOf(ArrowEmerald, ArrowCyan, ArrowViolet, ArrowPink, ArrowGold, ArrowOrange),
    )
    val INFINITE = GardenPalette(
        bgTop = Color(0xFF101224), bgBottom = BgDeep, cell = Color(0xFF151A30), cellBorder = Color(0xFF262D4F), accent = Color(0xFFF8F4FF),
        arrows = listOf(ArrowGold, ArrowCyan, ArrowPink, ArrowMint, ArrowViolet, ArrowOrange),
    )

    val ALL = listOf(MEADOW, ORCHARD, LAGOON, DUSK, AURORA, INFINITE)

    /** The Daily Bloom has its own look so it never feels like "just another level". */
    val DAILY = GardenPalette(
        bgTop = Color(0xFF231022), bgBottom = BgDeep, cell = Color(0xFF24172A), cellBorder = Color(0xFF3F2846), accent = ArrowBlossom,
        arrows = listOf(ArrowBlossom, ArrowGold, ArrowCyan, ArrowMint, ArrowViolet, ArrowOrange),
    )

    fun forGarden(index: Int): GardenPalette = ALL[index.coerceIn(0, ALL.lastIndex)]
}

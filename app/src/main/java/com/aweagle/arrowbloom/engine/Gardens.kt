package com.aweagle.arrowbloom.engine

import androidx.annotation.StringRes
import com.aweagle.arrowbloom.R

/**
 * A chapter of the Journey: twenty levels that share a board size, a difficulty curve and a
 * palette. Gardens are how progress is shown on the Home screen ("Orchard, level 7 of 20")
 * and how the generator is tuned without touching the engine.
 *
 * @param baseArrows arrows on the garden's first level; grows by [arrowStep] per level.
 * @param longArrowChance probability that an arrow gets a body longer than one cell.
 * @param maxBodyLength longest body a long arrow may have (1 = single cells only).
 */
data class Garden(
    val index: Int,
    @StringRes val nameRes: Int,
    val firstLevel: Int,
    val lastLevel: Int,
    val rows: Int,
    val cols: Int,
    val baseArrows: Int,
    val arrowStep: Float,
    val colorCount: Int,
    val longArrowChance: Float,
    val maxBodyLength: Int,
) {
    val isEndless: Boolean get() = lastLevel == Int.MAX_VALUE
    val levelCount: Int get() = if (isEndless) Int.MAX_VALUE else lastLevel - firstLevel + 1

    /** 1-based position of [level] inside this garden. */
    fun positionOf(level: Int): Int = (level - firstLevel + 1).coerceAtLeast(1)
}

object Gardens {

    const val LEVELS_PER_GARDEN = 20

    val ALL: List<Garden> = listOf(
        Garden(0, R.string.garden_meadow, 1, 20, rows = 4, cols = 4, baseArrows = 4, arrowStep = 0.35f, colorCount = 3, longArrowChance = 0f, maxBodyLength = 1),
        Garden(1, R.string.garden_orchard, 21, 40, rows = 5, cols = 5, baseArrows = 10, arrowStep = 0.45f, colorCount = 4, longArrowChance = 0.15f, maxBodyLength = 2),
        Garden(2, R.string.garden_lagoon, 41, 60, rows = 6, cols = 6, baseArrows = 16, arrowStep = 0.5f, colorCount = 4, longArrowChance = 0.25f, maxBodyLength = 2),
        Garden(3, R.string.garden_dusk, 61, 80, rows = 6, cols = 6, baseArrows = 22, arrowStep = 0.4f, colorCount = 5, longArrowChance = 0.3f, maxBodyLength = 3),
        Garden(4, R.string.garden_aurora, 81, 100, rows = 7, cols = 7, baseArrows = 28, arrowStep = 0.5f, colorCount = 6, longArrowChance = 0.35f, maxBodyLength = 3),
        Garden(5, R.string.garden_infinite, 101, Int.MAX_VALUE, rows = 8, cols = 8, baseArrows = 38, arrowStep = 0f, colorCount = 6, longArrowChance = 0.35f, maxBodyLength = 3),
    )

    fun forLevel(level: Int): Garden = ALL.firstOrNull { level in it.firstLevel..it.lastLevel } ?: ALL.last()

    fun configFor(level: Int): LevelConfig {
        val g = forLevel(level)
        val pos = (level - g.firstLevel).coerceAtLeast(0)
        val arrows = if (g.isEndless) g.baseArrows + (level % 10) else (g.baseArrows + g.arrowStep * pos).toInt()
        return LevelConfig(
            levelNumber = level,
            rows = g.rows,
            cols = g.cols,
            arrowCount = minOf(arrows, g.rows * g.cols),
            colorCount = g.colorCount,
            longArrowChance = g.longArrowChance,
            maxBodyLength = g.maxBodyLength,
            freeOnly = level == 1,
        )
    }
}

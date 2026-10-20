package com.aweagle.arrowbloom.engine

import androidx.annotation.StringRes
import com.aweagle.arrowbloom.R
import kotlin.math.roundToInt

/**
 * A chapter of the Journey: twenty levels that share a palette and a difficulty curve.
 * Inside a garden the board grows from [colsStart] to [colsEnd] columns, bodies get longer
 * and the board fills up. Every fifth level takes a shape (heart, flower, diamond, circle).
 */
data class Garden(
    val index: Int,
    @StringRes val nameRes: Int,
    val firstLevel: Int,
    val lastLevel: Int,
    val colsStart: Int,
    val colsEnd: Int,
    val minLength: Int,
    val maxLengthStart: Int,
    val maxLengthEnd: Int,
    val fillStart: Float,
    val fillEnd: Float,
    val straightness: Float,
    val attempts: Int,
) {
    val isEndless: Boolean get() = lastLevel == Int.MAX_VALUE
    val levelCount: Int get() = if (isEndless) Int.MAX_VALUE else lastLevel - firstLevel + 1

    /** 1-based position of [level] inside this garden. */
    fun positionOf(level: Int): Int = (level - firstLevel + 1).coerceAtLeast(1)
}

object Gardens {

    const val LEVELS_PER_GARDEN = 20
    private const val ROWS_PER_COL = 1.34f
    private val SHAPES = listOf(BoardShape.HEART, BoardShape.FLOWER, BoardShape.DIAMOND, BoardShape.CIRCLE)

    val ALL: List<Garden> = listOf(
        Garden(0, R.string.garden_meadow, 1, 20, colsStart = 5, colsEnd = 8, minLength = 2, maxLengthStart = 3, maxLengthEnd = 5, fillStart = 0.55f, fillEnd = 0.75f, straightness = 0.6f, attempts = 1),
        Garden(1, R.string.garden_orchard, 21, 40, colsStart = 7, colsEnd = 9, minLength = 2, maxLengthStart = 5, maxLengthEnd = 6, fillStart = 0.74f, fillEnd = 0.82f, straightness = 0.55f, attempts = 2),
        Garden(2, R.string.garden_lagoon, 41, 60, colsStart = 8, colsEnd = 10, minLength = 3, maxLengthStart = 6, maxLengthEnd = 8, fillStart = 0.8f, fillEnd = 0.86f, straightness = 0.5f, attempts = 3),
        Garden(3, R.string.garden_dusk, 61, 80, colsStart = 9, colsEnd = 11, minLength = 3, maxLengthStart = 7, maxLengthEnd = 9, fillStart = 0.82f, fillEnd = 0.88f, straightness = 0.5f, attempts = 3),
        Garden(4, R.string.garden_aurora, 81, 100, colsStart = 10, colsEnd = 12, minLength = 3, maxLengthStart = 8, maxLengthEnd = 10, fillStart = 0.84f, fillEnd = 0.9f, straightness = 0.45f, attempts = 4),
        Garden(5, R.string.garden_infinite, 101, Int.MAX_VALUE, colsStart = 11, colsEnd = 13, minLength = 3, maxLengthStart = 9, maxLengthEnd = 11, fillStart = 0.88f, fillEnd = 0.9f, straightness = 0.45f, attempts = 4),
    )

    fun forLevel(level: Int): Garden = ALL.firstOrNull { level in it.firstLevel..it.lastLevel } ?: ALL.last()

    fun shapeFor(level: Int): BoardShape =
        if (level <= 2 || level % 5 != 0) BoardShape.RECT else SHAPES[(level / 5) % SHAPES.size]

    fun configFor(level: Int): LevelConfig {
        val lvl = level.coerceAtLeast(1)
        if (lvl == 1) {
            // The first board teaches the tap: five short arrows, none in another's way.
            return LevelConfig(levelNumber = 1, rows = 6, cols = 5, minLength = 2, maxLength = 3, fill = 0.5f, maxArrows = 5, freeOnly = true)
        }
        val g = forLevel(lvl)
        val t = if (g.isEndless) (lvl % 10) / 9f else (lvl - g.firstLevel) / (g.levelCount - 1).toFloat()
        val shape = shapeFor(lvl)
        // A shape cuts away about a third of the rectangle, so shaped boards get a bigger box.
        val extra = if (shape == BoardShape.RECT) 0 else 2
        val cols = lerp(g.colsStart, g.colsEnd, t) + extra
        return LevelConfig(
            levelNumber = lvl,
            rows = (cols * ROWS_PER_COL).roundToInt(),
            cols = cols,
            shape = shape,
            minLength = g.minLength,
            maxLength = lerp(g.maxLengthStart, g.maxLengthEnd, t),
            fill = g.fillStart + (g.fillEnd - g.fillStart) * t,
            straightness = g.straightness,
            attempts = g.attempts,
        )
    }

    private fun lerp(a: Int, b: Int, t: Float): Int = (a + (b - a) * t.coerceIn(0f, 1f)).roundToInt()
}

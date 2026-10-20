package com.aweagle.arrowbloom.engine

import kotlin.math.abs

/**
 * The outline of a board. Every few levels the board takes a shape instead of a rectangle,
 * which changes how arrows have to wind around each other and gives a chapter a landmark.
 * Shapes are pure functions of a cell's centre, so a board stays deterministic per level.
 */
enum class BoardShape {
    RECT,
    HEART,
    FLOWER,
    DIAMOND,
    CIRCLE;

    /** True when cell ([row], [col]) of a [rows] x [cols] board belongs to this shape. */
    fun contains(row: Int, col: Int, rows: Int, cols: Int): Boolean {
        if (this == RECT) return true
        // Cell centre mapped to [-1, 1] on both axes, y pointing up.
        val x = ((col + 0.5) / cols) * 2.0 - 1.0
        val y = 1.0 - ((row + 0.5) / rows) * 2.0
        return when (this) {
            RECT -> true
            CIRCLE -> x * x + y * y <= 1.02
            DIAMOND -> abs(x) + abs(y) <= 1.1
            HEART -> {
                // (X^2 + Y^2 - 1)^3 - X^2 Y^3 <= 0, scaled so the heart fills the box.
                val hx = x * 1.16
                val hy = y * 1.14 + 0.1
                val a = hx * hx + hy * hy - 1.0
                a * a * a - hx * hx * hy * hy * hy <= 0.0
            }
            FLOWER -> {
                // Four round petals around a round centre, notched on the diagonals.
                x * x + y * y <= 0.36 ||
                    PETALS.any { (px, py) -> (x - px) * (x - px) + (y - py) * (y - py) <= 0.25 }
            }
        }
    }

    private companion object {
        val PETALS = listOf(0.0 to 0.5, 0.0 to -0.5, 0.5 to 0.0, -0.5 to 0.0)
    }
}

package com.aweagle.arrowbloom.engine

/**
 * Cardinal directions for arrows on the grid.
 *
 * In grid coordinates:
 * - row 0 is at the top, increasing downwards (dRow = +1 for DOWN, -1 for UP).
 * - col 0 is at the left, increasing rightwards (dCol = +1 for RIGHT, -1 for LEFT).
 */
enum class Direction(val dRow: Int, val dCol: Int, val degrees: Float) {
    UP(-1, 0, 0f),
    RIGHT(0, 1, 90f),
    DOWN(1, 0, 180f),
    LEFT(0, -1, 270f);

    val opposite: Direction
        get() = when (this) {
            UP -> DOWN
            RIGHT -> LEFT
            DOWN -> UP
            LEFT -> RIGHT
        }

    fun turnClockwise(): Direction = when (this) {
        UP -> RIGHT
        RIGHT -> DOWN
        DOWN -> LEFT
        LEFT -> UP
    }

    fun turnCounterClockwise(): Direction = when (this) {
        UP -> LEFT
        LEFT -> DOWN
        DOWN -> RIGHT
        RIGHT -> UP
    }
}

package com.aweagle.arrowbloom.engine

/**
 * An immutable 2D grid position (row, col).
 */
data class GridPos(val row: Int, val col: Int) {
    fun moved(dir: Direction, steps: Int = 1): GridPos {
        return GridPos(row + dir.dRow * steps, col + dir.dCol * steps)
    }

    fun isInside(rows: Int, cols: Int): Boolean {
        return row in 0 until rows && col in 0 until cols
    }

    override fun toString(): String = "($row,$col)"
}

package com.aweagle.arrowbloom.engine

/**
 * Immutable representation of the puzzle board.
 *
 * @param rows Board height in grid cells.
 * @param cols Board width in grid cells.
 * @param arrows Map of arrow ID to Arrow data.
 * @param shape Outline of the playable area inside the rows x cols rectangle (a heart, a
 *   flower...). Cells outside it are never occupied and are not drawn; arrows still fly
 *   through them on the way out, because escape is measured to the rectangle's edge.
 */
data class Board(
    val rows: Int,
    val cols: Int,
    val arrows: Map<Int, Arrow>,
    val shape: BoardShape = BoardShape.RECT,
) {
    // Fast lookup from GridPos to Arrow ID
    private val cellMap: Map<GridPos, Int> by lazy {
        val map = mutableMapOf<GridPos, Int>()
        arrows.values.forEach { arrow ->
            if (arrow.state != ArrowState.ESCAPED) {
                arrow.body.forEach { pos ->
                    map[pos] = arrow.id
                }
            }
        }
        map
    }

    private val playable: BooleanArray by lazy {
        BooleanArray(rows * cols) { i -> shape.contains(i / cols, i % cols, rows, cols) }
    }

    val activeArrows: List<Arrow>
        get() = arrows.values.filter { it.state != ArrowState.ESCAPED }

    val isCompleted: Boolean
        get() = activeArrows.isEmpty()

    val totalArrows: Int
        get() = arrows.size

    val clearedArrowsCount: Int
        get() = arrows.values.count { it.state == ArrowState.ESCAPED }

    fun getArrowAt(pos: GridPos): Arrow? {
        val id = cellMap[pos] ?: return null
        return arrows[id]
    }

    fun getArrowById(id: Int): Arrow? = arrows[id]

    fun isInside(pos: GridPos): Boolean = pos.isInside(rows, cols)

    /** Inside the rectangle and inside the shape: a cell that can hold an arrow or show a dot. */
    fun isPlayable(pos: GridPos): Boolean = isInside(pos) && playable[pos.row * cols + pos.col]

    fun withArrow(arrow: Arrow): Board {
        val newArrows = arrows.toMutableMap()
        newArrows[arrow.id] = arrow
        return copy(arrows = newArrows)
    }

    fun withArrowState(arrowId: Int, newState: ArrowState): Board {
        val arrow = arrows[arrowId] ?: return this
        return withArrow(arrow.withState(newState))
    }

    fun removeArrow(arrowId: Int): Board {
        val arrow = arrows[arrowId] ?: return this
        return withArrow(arrow.withState(ArrowState.ESCAPED))
    }

    companion object {
        fun empty(rows: Int, cols: Int): Board = Board(rows, cols, emptyMap())
    }
}

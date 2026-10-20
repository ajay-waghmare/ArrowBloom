package com.aweagle.arrowbloom.engine

sealed class EscapeResult {
    data object CanEscape : EscapeResult()

    /**
     * @param blockerId the arrow in the way (the tapped arrow's own id if it would run into itself).
     * @param collisionPoint the first occupied cell ahead of the head.
     * @param freeSteps empty cells between the head and that cell: how far the arrow slides
     *   before it bumps back.
     */
    data class Blocked(val blockerId: Int, val collisionPoint: GridPos, val freeSteps: Int) : EscapeResult()
}

/**
 * Arrows are lines that slide out along their own path, the way a train follows its engine:
 * the head moves forward in its direction and every body cell steps into the cell in front of
 * it. Over the whole move the arrow only ever covers its own cells and the cells straight
 * ahead of its head, so an arrow can leave exactly when the line from its head to the edge of
 * the board is empty.
 */
object CollisionDetector {

    fun checkEscape(arrow: Arrow, board: Board): EscapeResult {
        var step = 1
        while (true) {
            val next = arrow.head.moved(arrow.direction, step)
            if (!board.isInside(next)) return EscapeResult.CanEscape
            if (arrow.occupies(next)) return EscapeResult.Blocked(arrow.id, next, step - 1)
            val blocker = board.getArrowAt(next)
            if (blocker != null && blocker.id != arrow.id) return EscapeResult.Blocked(blocker.id, next, step - 1)
            step++
        }
    }

    fun canArrowEscape(arrow: Arrow, board: Board): Boolean = checkEscape(arrow, board) is EscapeResult.CanEscape

    /** All active arrows that can leave right now. */
    fun findEscapableArrows(board: Board): List<Arrow> = board.activeArrows.filter { canArrowEscape(it, board) }

    /** Cells from the head to the edge of the board's rectangle, the head excluded. */
    fun rayLength(head: GridPos, direction: Direction, rows: Int, cols: Int): Int = when (direction) {
        Direction.UP -> head.row
        Direction.DOWN -> rows - 1 - head.row
        Direction.LEFT -> head.col
        Direction.RIGHT -> cols - 1 - head.col
    }
}

package com.aweagle.arrowbloom.engine

sealed class EscapeResult {
    data object CanEscape : EscapeResult()
    data class Blocked(val blockerId: Int, val collisionPoint: GridPos) : EscapeResult()
}

/**
 * Handles raycasts and collision checks to determine whether arrows can escape the board.
 */
object CollisionDetector {

    /**
     * Determines whether [arrow] can move along its [Arrow.direction] and escape the [board].
     *
     * The arrow escapes if every cell of its body can sweep outward indefinitely
     * without hitting any other active arrow on the board.
     */
    fun checkEscape(arrow: Arrow, board: Board): EscapeResult {
        val dir = arrow.direction
        val maxSteps = maxOf(board.rows, board.cols)

        // For each segment of the arrow, raycast forward along the movement direction
        for (segment in arrow.body) {
            for (step in 1..maxSteps) {
                val nextPos = segment.moved(dir, step)
                if (!board.isInside(nextPos)) {
                    // This segment has escaped the boundary
                    break
                }

                // If nextPos is part of this arrow's own body, it does not block itself
                if (arrow.occupies(nextPos)) {
                    continue
                }

                // Check if another active arrow occupies this cell
                val blocker = board.getArrowAt(nextPos)
                if (blocker != null && blocker.id != arrow.id) {
                    return EscapeResult.Blocked(blocker.id, nextPos)
                }
            }
        }

        return EscapeResult.CanEscape
    }

    /**
     * Convenience function returning true if the arrow can escape unobstructed.
     */
    fun canArrowEscape(arrow: Arrow, board: Board): Boolean {
        return checkEscape(arrow, board) is EscapeResult.CanEscape
    }

    /**
     * Returns all currently active arrows on the board that have a clear path to escape.
     */
    fun findEscapableArrows(board: Board): List<Arrow> {
        return board.activeArrows.filter { canArrowEscape(it, board) }
    }
}

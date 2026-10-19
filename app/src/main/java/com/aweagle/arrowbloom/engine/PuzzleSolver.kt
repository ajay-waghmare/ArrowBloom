package com.aweagle.arrowbloom.engine

sealed class SolverResult {
    data class Solvable(val solutionOrder: List<Int>) : SolverResult()
    data class Unsolvable(val remainingArrows: List<Int>) : SolverResult()
}

/**
 * Validates and solves puzzle boards using topological raycast peeling.
 *
 * Monotonicity Theorem:
 * Removing an arrow only vacates cells and never introduces new obstructions.
 * Thus, if a board is solvable, greedily escaping any available unblocked arrow
 * will never cause an avoidable deadlock.
 */
object PuzzleSolver {

    /**
     * Determines whether the given board can be 100% solved, and returns
     * a valid sequence of arrow IDs to tap.
     */
    fun solve(board: Board): SolverResult {
        var currentBoard = board
        val solution = mutableListOf<Int>()

        while (!currentBoard.isCompleted) {
            val escapable = CollisionDetector.findEscapableArrows(currentBoard)
            if (escapable.isEmpty()) {
                return SolverResult.Unsolvable(currentBoard.activeArrows.map { it.id })
            }

            // Greedy step: escape the first available arrow
            val nextToEscape = escapable.first()
            solution.add(nextToEscape.id)
            currentBoard = currentBoard.removeArrow(nextToEscape.id)
        }

        return SolverResult.Solvable(solution)
    }

    /**
     * Returns true if the board is 100% solvable.
     */
    fun isSolvable(board: Board): Boolean {
        return solve(board) is SolverResult.Solvable
    }
}

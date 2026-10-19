package com.aweagle.arrowbloom.engine

data class MoveRecord(
    val arrowId: Int,
    val previousState: ArrowState,
    val scoreEarned: Int,
    val previousCombo: Int
)

sealed class TapResult {
    data class Escaped(val arrow: Arrow, val scoreDelta: Int, val combo: Int, val isLevelCompleted: Boolean) : TapResult()
    data class Blocked(val arrow: Arrow, val blockerId: Int) : TapResult()
    data object EmptyCell : TapResult()
    data object AlreadyEscaped : TapResult()
}

/**
 * Game state coordinator managing player interactions, combos, undo history, and hint queries.
 */
class GameEngine(
    initialBoard: Board,
    val levelNumber: Int
) {
    var board: Board = initialBoard
        private set

    var score: Int = 0
        private set

    var combo: Int = 0
        private set

    var movesCount: Int = 0
        private set

    private val undoStack = mutableListOf<MoveRecord>()

    val isLevelCompleted: Boolean
        get() = board.isCompleted

    val remainingArrowsCount: Int
        get() = board.activeArrows.size

    val totalArrowsCount: Int
        get() = board.totalArrows

    /**
     * Handles player tap on a grid coordinate.
     */
    fun onCellTapped(pos: GridPos): TapResult {
        val arrow = board.getArrowAt(pos) ?: return TapResult.EmptyCell
        return onArrowTapped(arrow.id)
    }

    /**
     * Handles player tap directly on an arrow ID.
     */
    fun onArrowTapped(arrowId: Int): TapResult {
        val arrow = board.getArrowById(arrowId) ?: return TapResult.EmptyCell
        if (arrow.state == ArrowState.ESCAPED) {
            return TapResult.AlreadyEscaped
        }

        movesCount++

        return when (val escapeCheck = CollisionDetector.checkEscape(arrow, board)) {
            is EscapeResult.CanEscape -> {
                combo++
                val points = 10 * combo
                score += points

                undoStack.add(
                    MoveRecord(
                        arrowId = arrow.id,
                        previousState = arrow.state,
                        scoreEarned = points,
                        previousCombo = combo - 1
                    )
                )

                board = board.removeArrow(arrow.id)
                TapResult.Escaped(
                    arrow = arrow,
                    scoreDelta = points,
                    combo = combo,
                    isLevelCompleted = board.isCompleted
                )
            }
            is EscapeResult.Blocked -> {
                // Reset combo on collision
                combo = 0
                board = board.withArrowState(arrow.id, ArrowState.BLOCKED)
                TapResult.Blocked(arrow, escapeCheck.blockerId)
            }
        }
    }

    /**
     * Resets blocked arrow visual state back to idle.
     */
    fun resetBlockedState(arrowId: Int) {
        val arrow = board.getArrowById(arrowId) ?: return
        if (arrow.state == ArrowState.BLOCKED) {
            board = board.withArrowState(arrowId, ArrowState.IDLE)
        }
    }

    /**
     * Undoes the last successful escape.
     */
    fun undo(): Boolean {
        val lastMove = undoStack.removeLastOrNull() ?: return false
        val arrow = board.getArrowById(lastMove.arrowId) ?: return false

        board = board.withArrow(arrow.withState(lastMove.previousState))
        score = maxOf(0, score - lastMove.scoreEarned)
        combo = lastMove.previousCombo
        movesCount++
        return true
    }

    /**
     * Returns an unblocked arrow ID suitable for a hint highlight, or null if none available.
     */
    fun getHintArrowId(): Int? {
        val escapable = CollisionDetector.findEscapableArrows(board)
        return escapable.firstOrNull()?.id
    }
}

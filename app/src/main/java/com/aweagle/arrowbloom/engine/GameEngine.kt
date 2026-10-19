package com.aweagle.arrowbloom.engine

data class MoveRecord(
    val arrowId: Int,
    val previousState: ArrowState,
    val scoreEarned: Int,
    val previousCombo: Int,
    val previousColorStreak: Int,
    val previousLastColor: Int,
)

sealed class TapResult {
    data class Escaped(
        val arrow: Arrow,
        val scoreDelta: Int,
        val combo: Int,
        val colorStreak: Int,
        val isLevelCompleted: Boolean,
    ) : TapResult()
    data class Blocked(val arrow: Arrow, val blockerId: Int) : TapResult()
    data object EmptyCell : TapResult()
    data object AlreadyEscaped : TapResult()
}

/**
 * One run of one board: taps, score, combo, colour streak, blocked-tap count (the star
 * rule), undo history and hints. Pure Kotlin; the view model owns the animations.
 */
class GameEngine(
    initialBoard: Board,
    val levelNumber: Int,
) {
    var board: Board = initialBoard
        private set
    var score: Int = 0
        private set
    var combo: Int = 0
        private set
    var colorStreak: Int = 0
        private set
    var movesCount: Int = 0
        private set
    var blockedTaps: Int = 0
        private set

    private var lastColor: Int = -1
    private val undoStack = ArrayList<MoveRecord>()
    private val history = ArrayList<Int>()

    /**
     * The run so far: escaped arrow ids in order, with [BLOCKED_MARK] where a blocked tap
     * happened. Replaying it rebuilds the exact score, combo and star count after a process death.
     */
    val escapeOrder: List<Int> get() = history

    val isLevelCompleted: Boolean get() = board.isCompleted
    val remainingArrowsCount: Int get() = board.activeArrows.size
    val totalArrowsCount: Int get() = board.totalArrows

    /** Every arrow needs one tap, so par is the arrow count. */
    val par: Int get() = board.totalArrows
    val starsSoFar: Int get() = Scoring.starsFor(blockedTaps)
    val canUndo: Boolean get() = undoStack.isNotEmpty()

    fun onCellTapped(pos: GridPos): TapResult {
        val arrow = board.getArrowAt(pos) ?: return TapResult.EmptyCell
        return onArrowTapped(arrow.id)
    }

    fun onArrowTapped(arrowId: Int): TapResult {
        val arrow = board.getArrowById(arrowId) ?: return TapResult.EmptyCell
        if (arrow.state == ArrowState.ESCAPED) return TapResult.AlreadyEscaped
        movesCount++
        return when (val check = CollisionDetector.checkEscape(arrow, board)) {
            is EscapeResult.CanEscape -> escape(arrow)
            is EscapeResult.Blocked -> {
                combo = 0
                colorStreak = 0
                lastColor = -1
                blockedTaps++
                history.add(BLOCKED_MARK)
                board = board.withArrowState(arrow.id, ArrowState.BLOCKED)
                TapResult.Blocked(arrow, check.blockerId)
            }
        }
    }

    private fun escape(arrow: Arrow): TapResult.Escaped {
        val previousCombo = combo
        val previousStreak = colorStreak
        val previousLast = lastColor
        combo++
        colorStreak = if (arrow.colorIndex == lastColor) colorStreak + 1 else 1
        lastColor = arrow.colorIndex
        val points = Scoring.pointsFor(combo, colorStreak)
        score += points
        undoStack.add(MoveRecord(arrow.id, arrow.state, points, previousCombo, previousStreak, previousLast))
        history.add(arrow.id)
        board = board.removeArrow(arrow.id)
        return TapResult.Escaped(arrow, points, combo, colorStreak, board.isCompleted)
    }

    /** Blocked is a transient visual state; the view model clears it after the shake. */
    fun resetBlockedState(arrowId: Int) {
        val arrow = board.getArrowById(arrowId) ?: return
        if (arrow.state == ArrowState.BLOCKED) board = board.withArrowState(arrowId, ArrowState.IDLE)
    }

    /** Takes back the last escape. Blocked taps are not undone: they are the star rule. */
    fun undo(): Boolean {
        val last = undoStack.removeLastOrNull() ?: return false
        val arrow = board.getArrowById(last.arrowId) ?: return false
        board = board.withArrow(arrow.withState(last.previousState))
        score = (score - last.scoreEarned).coerceAtLeast(0)
        combo = last.previousCombo
        colorStreak = last.previousColorStreak
        lastColor = last.previousLastColor
        val idx = history.lastIndexOf(last.arrowId)
        if (idx >= 0) history.removeAt(idx)
        return true
    }

    fun getHintArrowId(): Int? = CollisionDetector.findEscapableArrows(board).firstOrNull()?.id

    /**
     * Rebuilds a saved run by replaying [order]: escapes in sequence and [BLOCKED_MARK]s as
     * combo resets. Stops at the first id that cannot escape, which only happens with a
     * corrupt save. [blockedTapsBefore] is a floor for saves written without marks.
     * Returns how many escapes were applied.
     */
    fun restore(order: List<Int>, blockedTapsBefore: Int): Int {
        var applied = 0
        var blocked = 0
        for (id in order) {
            if (id == BLOCKED_MARK) {
                combo = 0
                colorStreak = 0
                lastColor = -1
                blocked++
                history.add(BLOCKED_MARK)
                continue
            }
            val arrow = board.getArrowById(id) ?: break
            if (arrow.state == ArrowState.ESCAPED) continue
            if (CollisionDetector.checkEscape(arrow, board) !is EscapeResult.CanEscape) break
            escape(arrow)
            applied++
        }
        blockedTaps = maxOf(blocked, blockedTapsBefore.coerceAtLeast(0))
        movesCount = applied + blockedTaps
        return applied
    }

    companion object {
        /** Sentinel in [escapeOrder] for a blocked tap. Arrow ids start at 1. */
        const val BLOCKED_MARK = -1
    }
}

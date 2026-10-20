package com.aweagle.arrowbloom.engine

sealed class TapResult {
    data class Escaped(
        val arrow: Arrow,
        val scoreDelta: Int,
        val combo: Int,
        val isLevelCompleted: Boolean,
    ) : TapResult()

    /**
     * The arrow slid [freeSteps] cells, bumped into [blockerId] and came back.
     * [isFailed] is true when that was the last heart.
     */
    data class Blocked(
        val arrow: Arrow,
        val blockerId: Int,
        val freeSteps: Int,
        val heartsLeft: Int,
        val isFailed: Boolean,
    ) : TapResult()

    data object EmptyCell : TapResult()
    data object AlreadyEscaped : TapResult()

    /** The board is over (cleared or out of hearts); taps are ignored. */
    data object Ignored : TapResult()
}

/**
 * One run of one board: taps, hearts, score, combo and hints. Pure Kotlin; the view model
 * owns the animations.
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
    var movesCount: Int = 0
        private set
    var blockedTaps: Int = 0
        private set

    private val history = ArrayList<Int>()

    /**
     * The run so far: escaped arrow ids in order, with [BLOCKED_MARK] where a wrong tap
     * happened. Replaying it rebuilds the exact hearts, score and combo after a process death.
     */
    val escapeOrder: List<Int> get() = history

    val isLevelCompleted: Boolean get() = board.isCompleted
    val heartsLeft: Int get() = Scoring.heartsLeft(blockedTaps)
    val isFailed: Boolean get() = heartsLeft == 0 && !board.isCompleted
    val remainingArrowsCount: Int get() = board.activeArrows.size
    val totalArrowsCount: Int get() = board.totalArrows
    val starsSoFar: Int get() = Scoring.starsFor(blockedTaps)

    fun onCellTapped(pos: GridPos): TapResult {
        val arrow = board.getArrowAt(pos) ?: return TapResult.EmptyCell
        return onArrowTapped(arrow.id)
    }

    fun onArrowTapped(arrowId: Int): TapResult {
        if (isFailed || isLevelCompleted) return TapResult.Ignored
        val arrow = board.getArrowById(arrowId) ?: return TapResult.EmptyCell
        if (arrow.state == ArrowState.ESCAPED) return TapResult.AlreadyEscaped
        movesCount++
        return when (val check = CollisionDetector.checkEscape(arrow, board)) {
            is EscapeResult.CanEscape -> escape(arrow)
            is EscapeResult.Blocked -> {
                combo = 0
                blockedTaps++
                history.add(BLOCKED_MARK)
                board = board.withArrowState(arrow.id, ArrowState.BLOCKED)
                TapResult.Blocked(arrow, check.blockerId, check.freeSteps, heartsLeft, isFailed)
            }
        }
    }

    private fun escape(arrow: Arrow): TapResult.Escaped {
        combo++
        val points = Scoring.pointsFor(combo)
        score += points
        history.add(arrow.id)
        board = board.removeArrow(arrow.id)
        return TapResult.Escaped(arrow, points, combo, board.isCompleted)
    }

    /** Blocked is a transient visual state; the view model clears it after the bump. */
    fun resetBlockedState(arrowId: Int) {
        val arrow = board.getArrowById(arrowId) ?: return
        if (arrow.state == ArrowState.BLOCKED) board = board.withArrowState(arrowId, ArrowState.IDLE)
    }

    /**
     * A free arrow to suggest: the one standing in front of the most other arrows, so the
     * hint opens the board up instead of picking an edge arrow nobody waits for.
     */
    fun getHintArrowId(): Int? {
        val free = CollisionDetector.findEscapableArrows(board)
        if (free.isEmpty()) return null
        val waiting = HashMap<Int, Int>()
        for (a in board.activeArrows) {
            val r = CollisionDetector.checkEscape(a, board)
            if (r is EscapeResult.Blocked && r.blockerId != a.id) waiting[r.blockerId] = (waiting[r.blockerId] ?: 0) + 1
        }
        return free.maxWithOrNull(compareBy<Arrow>({ waiting[it.id] ?: 0 }, { it.body.size }, { -it.id }))?.id
    }

    /**
     * Rebuilds a saved run by replaying [order]: escapes in sequence and [BLOCKED_MARK]s as
     * lost hearts. Stops at the first id that cannot escape, which only happens with a
     * corrupt save. [blockedTapsBefore] is a floor for saves written without marks.
     * Returns how many escapes were applied.
     */
    fun restore(order: List<Int>, blockedTapsBefore: Int): Int {
        var applied = 0
        var blocked = 0
        for (id in order) {
            if (id == BLOCKED_MARK) {
                combo = 0
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
        /** Sentinel in [escapeOrder] for a wrong tap. Arrow ids start at 1. */
        const val BLOCKED_MARK = -1
    }
}

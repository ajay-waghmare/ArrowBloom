package com.aweagle.arrowbloom.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    // A waits on B; C is free and nobody waits on it.
    private fun engine(withC: Boolean = false): GameEngine {
        val a = line(1, Direction.RIGHT, 2 to 1, 2 to 0)
        val b = line(2, Direction.UP, 1 to 3, 2 to 3, 3 to 3)
        val c = line(3, Direction.RIGHT, 4 to 4, 4 to 3)
        val board = if (withC) boardOf(5, 5, a, b, c) else boardOf(5, 5, a, b)
        return GameEngine(board, levelNumber = 7)
    }

    @Test fun emptyCell_isNothing() {
        assertEquals(TapResult.EmptyCell, engine().onCellTapped(GridPos(0, 0)))
    }

    @Test fun freeArrow_escapes_andBuildsCombo() {
        val e = engine()
        val r1 = e.onArrowTapped(2) as TapResult.Escaped
        assertEquals(1, r1.combo)
        assertEquals(10, r1.scoreDelta)
        val r2 = e.onCellTapped(GridPos(2, 0)) as TapResult.Escaped
        assertEquals(2, r2.combo)
        assertEquals(20, r2.scoreDelta)
        assertTrue(r2.isLevelCompleted)
        assertEquals(30, e.score)
        assertEquals(5, e.starsSoFar)
        assertEquals(TapResult.Ignored, e.onArrowTapped(1))
    }

    @Test fun wrongTap_costsAHeart_resetsCombo_andReportsTheSlide() {
        val e = engine(withC = true)
        e.onArrowTapped(3)
        assertEquals(1, e.combo)
        val r = e.onArrowTapped(1) as TapResult.Blocked
        assertEquals(2, r.blockerId)
        assertEquals(1, r.freeSteps)
        assertEquals(2, r.heartsLeft)
        assertFalse(r.isFailed)
        assertEquals(0, e.combo)
        assertEquals(ArrowState.BLOCKED, e.board.getArrowById(1)?.state)
        e.resetBlockedState(1)
        assertEquals(ArrowState.IDLE, e.board.getArrowById(1)?.state)
        assertEquals(4, e.starsSoFar)
    }

    @Test fun threeWrongTaps_loseTheBoard() {
        val e = engine()
        e.onArrowTapped(1)
        e.onArrowTapped(1)
        val last = e.onArrowTapped(1) as TapResult.Blocked
        assertTrue(last.isFailed)
        assertEquals(0, last.heartsLeft)
        assertTrue(e.isFailed)
        assertEquals(0, e.starsSoFar)
        assertEquals(TapResult.Ignored, e.onArrowTapped(2))
    }

    @Test fun hints_costStars_andAreRestored() {
        val e = engine(withC = true)
        assertEquals(2, e.useHint())
        assertEquals(1, e.hintsUsed)
        assertEquals(4, e.starsSoFar)
        e.onArrowTapped(1)                 // wrong: another star
        assertEquals(3, e.starsSoFar)
        val restored = engine(withC = true)
        restored.restore(e.escapeOrder.toList(), blockedTapsBefore = 0, hintsUsedBefore = e.hintsUsed)
        assertEquals(1, restored.hintsUsed)
        assertEquals(3, restored.starsSoFar)
    }

    @Test fun noHint_onAFinishedBoard() {
        val e = engine()
        e.onArrowTapped(2); e.onArrowTapped(1)
        assertTrue(e.isLevelCompleted)
        assertEquals(null, e.useHint())
        assertEquals(0, e.hintsUsed)
    }

    @Test fun hint_picksTheArrowOthersWaitFor() {
        val e = engine(withC = true)
        assertEquals(2, e.getHintArrowId())
        e.onArrowTapped(2)
        val next = e.getHintArrowId()
        assertTrue(next == 1 || next == 3)
    }

    @Test fun restore_replaysEscapesAndLostHearts() {
        val original = engine(withC = true)
        original.onArrowTapped(1)          // wrong
        original.onArrowTapped(3)
        original.onArrowTapped(2)
        val order = original.escapeOrder.toList()
        assertEquals(listOf(GameEngine.BLOCKED_MARK, 3, 2), order)

        val restored = engine(withC = true)
        assertEquals(2, restored.restore(order, blockedTapsBefore = 0))
        assertEquals(original.score, restored.score)
        assertEquals(original.combo, restored.combo)
        assertEquals(2, restored.heartsLeft)
        assertEquals(1, restored.remainingArrowsCount)
    }

    @Test fun restore_stopsAtAnImpossibleEscape() {
        val e = engine()
        assertEquals(0, e.restore(listOf(1, 2), blockedTapsBefore = 0))
        assertEquals(2, e.remainingArrowsCount)
    }
}

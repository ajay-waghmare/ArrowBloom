package com.aweagle.arrowbloom.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameEngineTest {

    private lateinit var engine: GameEngine

    @Before
    fun setUp() {
        // a1 (0,0) UP escapes · a2 (2,2) RIGHT escapes · a3 (2,1) RIGHT is blocked by a2.
        val a1 = Arrow(1, GridPos(0, 0), Direction.UP, colorIndex = 0)
        val a2 = Arrow(2, GridPos(2, 2), Direction.RIGHT, colorIndex = 1)
        val a3 = Arrow(3, GridPos(2, 1), Direction.RIGHT, colorIndex = 0)
        engine = GameEngine(Board(4, 4, mapOf(1 to a1, 2 to a2, 3 to a3)), levelNumber = 1)
    }

    @Test fun tapEmptyCell_changesNothing() {
        assertEquals(TapResult.EmptyCell, engine.onCellTapped(GridPos(3, 3)))
        assertEquals(0, engine.score)
        assertEquals(0, engine.combo)
        assertEquals(0, engine.movesCount)
    }

    @Test fun escape_scoresAndStartsCombo() {
        val r = engine.onArrowTapped(1) as TapResult.Escaped
        assertEquals(10, r.scoreDelta)
        assertEquals(1, r.combo)
        assertEquals(1, r.colorStreak)
        assertFalse(r.isLevelCompleted)
        assertEquals(10, engine.score)
        assertEquals(2, engine.remainingArrowsCount)
        assertEquals(listOf(1), engine.escapeOrder)
        assertTrue(engine.canUndo)
    }

    @Test fun consecutiveEscapes_differentColours_buildTheCombo() {
        engine.onArrowTapped(1)
        val r = engine.onArrowTapped(2) as TapResult.Escaped
        assertEquals(20, r.scoreDelta)
        assertEquals(2, r.combo)
        assertEquals(1, r.colorStreak)      // colour changed, no bonus
        assertEquals(30, engine.score)
    }

    @Test fun sameColourTwice_earnsTheColourBonus() {
        val b1 = Arrow(1, GridPos(0, 0), Direction.UP, colorIndex = 2)
        val b2 = Arrow(2, GridPos(0, 3), Direction.UP, colorIndex = 2)
        val e = GameEngine(Board(4, 4, mapOf(1 to b1, 2 to b2)), 1)
        e.onArrowTapped(1)
        val r = e.onArrowTapped(2) as TapResult.Escaped
        assertEquals(2, r.colorStreak)
        assertEquals(Scoring.pointsFor(2, 2), r.scoreDelta)
        assertEquals(25, r.scoreDelta)
        assertTrue(r.isLevelCompleted)
    }

    @Test fun blockedTap_resetsCombo_countsAgainstStars_andIsTransient() {
        engine.onArrowTapped(1)
        val r = engine.onArrowTapped(3) as TapResult.Blocked
        assertEquals(2, r.blockerId)
        assertEquals(0, engine.combo)
        assertEquals(10, engine.score)
        assertEquals(1, engine.blockedTaps)
        assertEquals(2, engine.starsSoFar)
        assertEquals(ArrowState.BLOCKED, engine.board.getArrowById(3)?.state)
        engine.resetBlockedState(3)
        assertEquals(ArrowState.IDLE, engine.board.getArrowById(3)?.state)
    }

    @Test fun threeBlockedTaps_leaveOneStar() {
        repeat(3) { engine.onArrowTapped(3); engine.resetBlockedState(3) }
        assertEquals(1, engine.starsSoFar)
    }

    @Test fun undo_restoresBoardScoreComboAndOrder() {
        engine.onArrowTapped(1)
        engine.onArrowTapped(2)
        assertTrue(engine.undo())
        assertEquals(10, engine.score)
        assertEquals(1, engine.combo)
        assertEquals(listOf(1), engine.escapeOrder)
        assertNotNull(engine.board.getArrowAt(GridPos(2, 2)))
        assertTrue(engine.undo())
        assertFalse(engine.undo())
        assertFalse(engine.canUndo)
        assertEquals(0, engine.score)
    }

    @Test fun undo_doesNotForgiveABlockedTap() {
        engine.onArrowTapped(3)
        engine.onArrowTapped(1)
        engine.undo()
        assertEquals(1, engine.blockedTaps)
        assertEquals(listOf(GameEngine.BLOCKED_MARK), engine.escapeOrder)
    }

    @Test fun hint_isAlwaysAnEscapableArrow() {
        val id = engine.getHintArrowId()
        assertNotNull(id)
        assertTrue(CollisionDetector.canArrowEscape(engine.board.getArrowById(id!!)!!, engine.board))
    }

    @Test fun clearingEverything_completesTheBoard_atPar() {
        assertEquals(3, engine.par)
        engine.onArrowTapped(1)
        engine.onArrowTapped(2)
        val r = engine.onArrowTapped(3) as TapResult.Escaped
        assertTrue(r.isLevelCompleted)
        assertTrue(engine.isLevelCompleted)
        assertEquals(3, engine.starsSoFar)
        assertEquals(engine.par, engine.movesCount)
    }

    @Test fun tappingAnEscapedArrow_isIgnored() {
        engine.onArrowTapped(1)
        assertEquals(TapResult.AlreadyEscaped, engine.onArrowTapped(1))
        assertNull(engine.board.getArrowAt(GridPos(0, 0)))
    }

    @Test fun restore_replaysASavedRun() {
        engine.onArrowTapped(1)
        engine.onArrowTapped(3)      // blocked
        engine.resetBlockedState(3)
        engine.onArrowTapped(2)
        val order = engine.escapeOrder.toList()
        assertEquals(listOf(1, GameEngine.BLOCKED_MARK, 2), order)
        val blocked = engine.blockedTaps

        val fresh = GameEngine(engine.boardAtStart(), 1)
        assertEquals(2, fresh.restore(order, blocked))
        assertEquals(engine.score, fresh.score)
        assertEquals(engine.combo, fresh.combo)
        assertEquals(1, fresh.remainingArrowsCount)
        assertEquals(blocked, fresh.blockedTaps)
        assertEquals(2, fresh.starsSoFar)
    }

    @Test fun restore_stopsAtAnImpossibleStep() {
        val fresh = GameEngine(engine.boardAtStart(), 1)
        // 3 cannot escape before 2: a corrupt save must not crash or cheat.
        assertEquals(0, fresh.restore(listOf(3, 2), 0))
        assertEquals(3, fresh.remainingArrowsCount)
    }

    private fun GameEngine.boardAtStart(): Board {
        val a1 = Arrow(1, GridPos(0, 0), Direction.UP, colorIndex = 0)
        val a2 = Arrow(2, GridPos(2, 2), Direction.RIGHT, colorIndex = 1)
        val a3 = Arrow(3, GridPos(2, 1), Direction.RIGHT, colorIndex = 0)
        return Board(4, 4, mapOf(1 to a1, 2 to a2, 3 to a3))
    }
}

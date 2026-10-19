package com.aweagle.arrowbloom.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameEngineTest {

    private lateinit var board: Board
    private lateinit var engine: GameEngine

    @Before
    fun setUp() {
        // Arrow 1: at (0, 0) pointing UP (can escape)
        // Arrow 2: at (2, 2) pointing RIGHT (can escape)
        // Arrow 3: at (2, 1) pointing RIGHT (blocked by Arrow 2)
        val a1 = Arrow(1, GridPos(0, 0), Direction.UP)
        val a2 = Arrow(2, GridPos(2, 2), Direction.RIGHT)
        val a3 = Arrow(3, GridPos(2, 1), Direction.RIGHT)

        board = Board(4, 4, mapOf(1 to a1, 2 to a2, 3 to a3))
        engine = GameEngine(board, levelNumber = 1)
    }

    @Test
    fun tapEmptyCell_returnsEmptyCell() {
        val result = engine.onCellTapped(GridPos(3, 3))
        assertEquals(TapResult.EmptyCell, result)
        assertEquals(0, engine.score)
        assertEquals(0, engine.combo)
    }

    @Test
    fun tapEscapableArrow_incrementsScoreAndCombo() {
        val result = engine.onArrowTapped(1)
        assertTrue(result is TapResult.Escaped)

        val escaped = result as TapResult.Escaped
        assertEquals(10, escaped.scoreDelta)
        assertEquals(1, escaped.combo)
        assertFalse(escaped.isLevelCompleted)

        assertEquals(10, engine.score)
        assertEquals(1, engine.combo)
        assertEquals(2, engine.remainingArrowsCount)
    }

    @Test
    fun consecutiveEscapes_buildComboMultiplier() {
        // First escape
        engine.onArrowTapped(1)
        assertEquals(10, engine.score)
        assertEquals(1, engine.combo)

        // Second escape: combo = 2, points = 20
        val result2 = engine.onArrowTapped(2)
        assertTrue(result2 is TapResult.Escaped)
        val escaped2 = result2 as TapResult.Escaped
        assertEquals(20, escaped2.scoreDelta)
        assertEquals(2, escaped2.combo)
        assertEquals(30, engine.score) // 10 + 20
        assertEquals(2, engine.combo)
    }

    @Test
    fun tapBlockedArrow_resetsComboAndMarksBlocked() {
        // Start combo
        engine.onArrowTapped(1)
        assertEquals(1, engine.combo)

        // Tap Arrow 3 which is blocked by Arrow 2
        val result = engine.onArrowTapped(3)
        assertTrue(result is TapResult.Blocked)
        val blocked = result as TapResult.Blocked
        assertEquals(2, blocked.blockerId)

        // Combo should reset to 0, score remains 10
        assertEquals(0, engine.combo)
        assertEquals(10, engine.score)
        assertEquals(ArrowState.BLOCKED, engine.board.getArrowById(3)?.state)

        // Reset blocked state
        engine.resetBlockedState(3)
        assertEquals(ArrowState.IDLE, engine.board.getArrowById(3)?.state)
    }

    @Test
    fun undo_revertsEscapeScoreAndCombo() {
        engine.onArrowTapped(1)
        assertEquals(10, engine.score)
        assertEquals(1, engine.combo)
        assertEquals(2, engine.remainingArrowsCount)

        val undone = engine.undo()
        assertTrue(undone)
        assertEquals(0, engine.score)
        assertEquals(0, engine.combo)
        assertEquals(3, engine.remainingArrowsCount)
        assertNotNull(engine.board.getArrowAt(GridPos(0, 0)))
    }

    @Test
    fun hint_returnsValidEscapableArrow() {
        val hintId = engine.getHintArrowId()
        assertNotNull(hintId)

        val hintArrow = engine.board.getArrowById(hintId!!)
        assertNotNull(hintArrow)
        assertTrue(CollisionDetector.canArrowEscape(hintArrow!!, engine.board))
    }

    @Test
    fun clearingAllArrows_triggersLevelCompleted() {
        engine.onArrowTapped(1) // 2 remain
        engine.onArrowTapped(2) // 1 remains (Arrow 3 was unblocked by 2)

        val result3 = engine.onArrowTapped(3)
        assertTrue(result3 is TapResult.Escaped)
        val escaped3 = result3 as TapResult.Escaped

        assertTrue(escaped3.isLevelCompleted)
        assertTrue(engine.isLevelCompleted)
        assertEquals(0, engine.remainingArrowsCount)
    }
}

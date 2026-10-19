package com.aweagle.arrowbloom.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardTest {

    @Test
    fun emptyBoard_hasZeroArrowsAndIsCompleted() {
        val board = Board.empty(5, 5)
        assertEquals(5, board.rows)
        assertEquals(5, board.cols)
        assertEquals(0, board.totalArrows)
        assertTrue(board.activeArrows.isEmpty())
        assertTrue(board.isCompleted)
    }

    @Test
    fun boundaryChecks_areStrict() {
        val board = Board.empty(6, 4)
        assertTrue(board.isInside(GridPos(0, 0)))
        assertTrue(board.isInside(GridPos(5, 3)))
        assertFalse(board.isInside(GridPos(-1, 0)))
        assertFalse(board.isInside(GridPos(0, -1)))
        assertFalse(board.isInside(GridPos(6, 0)))
        assertFalse(board.isInside(GridPos(0, 4)))
    }

    @Test
    fun cellLookup_andEscapedRemoval_workConsistently() {
        val a1 = Arrow(1, GridPos(2, 2), Direction.UP)
        val a2 = Arrow(2, GridPos(3, 3), Direction.DOWN)
        val board = Board(5, 5, mapOf(1 to a1, 2 to a2))

        assertEquals(2, board.totalArrows)
        assertEquals(2, board.activeArrows.size)
        assertEquals(a1, board.getArrowAt(GridPos(2, 2)))
        assertEquals(a2, board.getArrowAt(GridPos(3, 3)))
        assertNull(board.getArrowAt(GridPos(0, 0)))

        // Mark arrow 1 as escaped
        val updated = board.removeArrow(1)
        assertEquals(2, updated.totalArrows)
        assertEquals(1, updated.activeArrows.size)
        assertEquals(1, updated.clearedArrowsCount)
        assertFalse(updated.isCompleted)

        // Arrow 1 should no longer be returned by spatial lookup
        assertNull(updated.getArrowAt(GridPos(2, 2)))

        // Remove arrow 2
        val completed = updated.removeArrow(2)
        assertEquals(0, completed.activeArrows.size)
        assertEquals(2, completed.clearedArrowsCount)
        assertTrue(completed.isCompleted)
    }

    @Test
    fun withArrowState_modifiesTargetArrowOnly() {
        val a1 = Arrow(1, GridPos(1, 1), Direction.RIGHT)
        val a2 = Arrow(2, GridPos(2, 2), Direction.LEFT)
        val board = Board(4, 4, mapOf(1 to a1, 2 to a2))

        val modified = board.withArrowState(1, ArrowState.BLOCKED)
        assertEquals(ArrowState.BLOCKED, modified.getArrowById(1)?.state)
        assertEquals(ArrowState.IDLE, modified.getArrowById(2)?.state)
    }
}

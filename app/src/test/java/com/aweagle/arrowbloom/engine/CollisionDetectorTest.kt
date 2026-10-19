package com.aweagle.arrowbloom.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollisionDetectorTest {

    @Test
    fun singleArrow_pointingToEdge_escapes() {
        // Arrow at (0, 0) pointing UP can escape the top edge
        val arrow = Arrow(id = 1, head = GridPos(0, 0), direction = Direction.UP)
        val board = Board(rows = 4, cols = 4, arrows = mapOf(1 to arrow))

        val result = CollisionDetector.checkEscape(arrow, board)
        assertTrue(result is EscapeResult.CanEscape)
        assertTrue(CollisionDetector.canArrowEscape(arrow, board))
    }

    @Test
    fun arrowBlockedByAnotherArrow_failsEscape() {
        // Arrow 1 at (2, 1) pointing RIGHT towards (2, 2)
        // Arrow 2 at (2, 2) pointing UP
        val arrow1 = Arrow(id = 1, head = GridPos(2, 1), direction = Direction.RIGHT)
        val arrow2 = Arrow(id = 2, head = GridPos(2, 2), direction = Direction.UP)
        val board = Board(rows = 4, cols = 4, arrows = mapOf(1 to arrow1, 2 to arrow2))

        val result1 = CollisionDetector.checkEscape(arrow1, board)
        assertTrue("Arrow 1 should be blocked by Arrow 2", result1 is EscapeResult.Blocked)
        assertEquals(2, (result1 as EscapeResult.Blocked).blockerId)
        assertEquals(GridPos(2, 2), result1.collisionPoint)

        // Arrow 2 pointing UP has clear path to (1, 2) and (0, 2) and edge
        val result2 = CollisionDetector.checkEscape(arrow2, board)
        assertTrue("Arrow 2 pointing UP should be able to escape", result2 is EscapeResult.CanEscape)
    }

    @Test
    fun escapingBlocker_freesBlockedArrow() {
        // Arrow 1 blocked by Arrow 2
        val arrow1 = Arrow(id = 1, head = GridPos(2, 1), direction = Direction.RIGHT)
        val arrow2 = Arrow(id = 2, head = GridPos(2, 2), direction = Direction.UP)
        val board = Board(rows = 4, cols = 4, arrows = mapOf(1 to arrow1, 2 to arrow2))

        assertFalse(CollisionDetector.canArrowEscape(arrow1, board))

        // Once Arrow 2 escapes, Arrow 1 can escape
        val updatedBoard = board.removeArrow(2)
        assertTrue(CollisionDetector.canArrowEscape(arrow1, updatedBoard))
    }

    @Test
    fun circularBlock_detectsUnsolvable() {
        // Arrow 1 pointing RIGHT at Arrow 2
        // Arrow 2 pointing DOWN at Arrow 3
        // Arrow 3 pointing LEFT at Arrow 4
        // Arrow 4 pointing UP at Arrow 1 (Cycle!)
        val a1 = Arrow(1, GridPos(0, 0), Direction.RIGHT)
        val a2 = Arrow(2, GridPos(0, 1), Direction.DOWN)
        val a3 = Arrow(3, GridPos(1, 1), Direction.LEFT)
        val a4 = Arrow(4, GridPos(1, 0), Direction.UP)
        val board = Board(rows = 3, cols = 3, arrows = mapOf(1 to a1, 2 to a2, 3 to a3, 4 to a4))

        val escapable = CollisionDetector.findEscapableArrows(board)
        assertTrue("No arrow in a cycle should be escapable", escapable.isEmpty())
        assertFalse("Cyclic board must be unsolvable", PuzzleSolver.isSolvable(board))
    }
}

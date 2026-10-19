package com.aweagle.arrowbloom.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Journey curve and, above all, that every generated level is solvable. */
class GardensTest {

    @Test fun levelsMapToGardensInOrder() {
        assertEquals(0, Gardens.forLevel(1).index)
        assertEquals(0, Gardens.forLevel(20).index)
        assertEquals(1, Gardens.forLevel(21).index)
        assertEquals(4, Gardens.forLevel(100).index)
        assertEquals(5, Gardens.forLevel(101).index)
        assertEquals(5, Gardens.forLevel(5000).index)
        assertEquals(7, Gardens.forLevel(27).positionOf(27))
        assertTrue(Gardens.forLevel(101).isEndless)
    }

    @Test fun firstLevel_isGentle() {
        val c = Gardens.configFor(1)
        assertEquals(4, c.rows)
        assertEquals(4, c.arrowCount)
        assertEquals(1, c.maxBodyLength)
        val board = PuzzleGenerator.generateLevel(1)
        assertEquals(4, board.totalArrows)
        // every arrow of level 1 is a single cell: nothing to explain yet
        assertTrue(board.arrows.values.all { it.body.size == 1 })
        // and every one of them escapes from the start: the first tap can never be a blocked tap
        assertTrue(board.arrows.values.all { CollisionDetector.canArrowEscape(it, board) })
        assertTrue(Gardens.configFor(1).freeOnly)
        assertTrue(!Gardens.configFor(2).freeOnly)
    }

    @Test fun arrowCount_neverExceedsTheBoard_andGrowsWithinAGarden() {
        for (level in 1..160) {
            val c = Gardens.configFor(level)
            assertTrue("level $level", c.arrowCount <= c.rows * c.cols)
        }
        assertTrue(Gardens.configFor(20).arrowCount > Gardens.configFor(1).arrowCount)
        assertTrue(Gardens.configFor(60).arrowCount > Gardens.configFor(41).arrowCount)
    }

    @Test fun everyLevelUpTo140_isSolvable_withValidBodies() {
        var longArrowsSeen = 0
        for (level in 1..140) {
            val board = PuzzleGenerator.generateLevel(level)
            assertTrue("level $level has no arrows", board.totalArrows > 0)
            val seen = HashSet<GridPos>()
            for (a in board.arrows.values) {
                // contiguous straight body behind the head, inside the board, no overlaps
                assertEquals("level $level arrow ${a.id}", a.head, a.body.first())
                for ((i, cell) in a.body.withIndex()) {
                    assertTrue("level $level arrow ${a.id} outside", board.isInside(cell))
                    assertEquals("level $level arrow ${a.id} not straight", a.head.moved(a.direction.opposite, i), cell)
                    assertTrue("level $level overlap at $cell", seen.add(cell))
                }
                if (a.body.size > 1) longArrowsSeen++
            }
            val result = PuzzleSolver.solve(board)
            assertTrue("level $level unsolvable: $result", result is SolverResult.Solvable)
            assertEquals(board.totalArrows, (result as SolverResult.Solvable).solutionOrder.size)
        }
        assertTrue("long arrows should appear from the second garden", longArrowsSeen > 20)
    }

    @Test fun generation_isDeterministic() {
        val a = PuzzleGenerator.generateLevel(37)
        val b = PuzzleGenerator.generateLevel(37)
        assertEquals(a.arrows.keys, b.arrows.keys)
        for (id in a.arrows.keys) assertEquals(a.arrows[id], b.arrows[id])
    }

    @Test fun longArrow_isBlockedByAnythingInFrontOfItsBody() {
        // A 2-cell arrow pointing RIGHT occupying (1,0)(head) and (1,1)? No: body lies BEHIND the head.
        val long = Arrow(1, GridPos(1, 2), Direction.RIGHT, body = PuzzleGenerator.bodyFor(GridPos(1, 2), Direction.RIGHT, 2))
        assertEquals(listOf(GridPos(1, 2), GridPos(1, 1)), long.body)
        val blocker = Arrow(2, GridPos(1, 3), Direction.UP)
        val board = Board(4, 4, mapOf(1 to long, 2 to blocker))
        assertTrue(CollisionDetector.checkEscape(long, board) is EscapeResult.Blocked)
        assertTrue(CollisionDetector.canArrowEscape(long, board.removeArrow(2)))
        assertTrue(PuzzleGenerator.isGraphAcyclic(board))
    }
}

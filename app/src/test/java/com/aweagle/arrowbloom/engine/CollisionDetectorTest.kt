package com.aweagle.arrowbloom.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Builds an arrow from its cells, head first. */
internal fun line(id: Int, dir: Direction, vararg cells: Pair<Int, Int>): Arrow {
    val body = cells.map { GridPos(it.first, it.second) }
    return Arrow(id = id, head = body.first(), direction = dir, body = body)
}

internal fun boardOf(rows: Int, cols: Int, vararg arrows: Arrow): Board = Board(rows, cols, arrows.associateBy { it.id })

class CollisionDetectorTest {

    // A waits on B: B's body crosses the line in front of A's head.
    private val a = line(1, Direction.RIGHT, 2 to 1, 2 to 0)
    private val b = line(2, Direction.UP, 1 to 3, 2 to 3, 3 to 3)

    @Test fun clearLineAheadOfTheHead_escapes() {
        val board = boardOf(5, 5, a, b)
        assertEquals(EscapeResult.CanEscape, CollisionDetector.checkEscape(b, board))
    }

    @Test fun anotherArrowAhead_blocks_andReportsTheSlide() {
        val board = boardOf(5, 5, a, b)
        val r = CollisionDetector.checkEscape(a, board)
        assertTrue(r is EscapeResult.Blocked)
        r as EscapeResult.Blocked
        assertEquals(2, r.blockerId)
        assertEquals(GridPos(2, 3), r.collisionPoint)
        assertEquals(1, r.freeSteps)
    }

    @Test fun onlyTheHeadsLineMatters_notTheBody() {
        // C winds right next to D and E, but its head looks out over an empty line.
        val c = line(1, Direction.UP, 1 to 1, 2 to 1, 2 to 2, 3 to 2)
        val d = line(2, Direction.LEFT, 3 to 0, 3 to 1)
        val e = line(3, Direction.DOWN, 3 to 3, 2 to 3)
        val board = boardOf(4, 4, c, d, e)
        assertTrue(CollisionDetector.canArrowEscape(c, board))
        assertTrue(CollisionDetector.canArrowEscape(d, board))
        assertTrue(CollisionDetector.canArrowEscape(e, board))
    }

    @Test fun pointingIntoItsOwnBody_isBlockedByItself() {
        val s = line(7, Direction.RIGHT, 1 to 1, 1 to 0, 0 to 0, 0 to 1, 0 to 2, 1 to 2)
        val r = CollisionDetector.checkEscape(s, boardOf(3, 3, s))
        assertTrue(r is EscapeResult.Blocked)
        r as EscapeResult.Blocked
        assertEquals(7, r.blockerId)
        assertEquals(0, r.freeSteps)
    }

    @Test fun removingTheBlocker_freesTheArrow() {
        val board = boardOf(5, 5, a, b).removeArrow(2)
        assertTrue(CollisionDetector.canArrowEscape(a, board))
        assertEquals(listOf(1), CollisionDetector.findEscapableArrows(board).map { it.id })
    }

    @Test fun rayLength_countsCellsToTheEdge() {
        assertEquals(2, CollisionDetector.rayLength(GridPos(2, 3), Direction.UP, 6, 5))
        assertEquals(3, CollisionDetector.rayLength(GridPos(2, 3), Direction.DOWN, 6, 5))
        assertEquals(3, CollisionDetector.rayLength(GridPos(2, 3), Direction.LEFT, 6, 5))
        assertEquals(1, CollisionDetector.rayLength(GridPos(2, 3), Direction.RIGHT, 6, 5))
    }
}

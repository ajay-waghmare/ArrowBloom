package com.aweagle.arrowbloom.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GardensTest {

    @Test fun gardens_coverEveryLevel_withoutGaps() {
        for (i in 1 until Gardens.ALL.size) {
            assertEquals(Gardens.ALL[i - 1].lastLevel + 1, Gardens.ALL[i].firstLevel)
        }
        assertEquals(1, Gardens.ALL.first().firstLevel)
        assertTrue(Gardens.ALL.last().isEndless)
        assertEquals(0, Gardens.forLevel(1).index)
        assertEquals(0, Gardens.forLevel(20).index)
        assertEquals(1, Gardens.forLevel(21).index)
        assertEquals(5, Gardens.forLevel(5000).index)
    }

    @Test fun boards_growThroughTheJourney() {
        val first = Gardens.configFor(3)
        val later = Gardens.configFor(97)
        assertTrue(later.cols > first.cols)
        assertTrue(later.rows > first.rows)
        assertTrue(later.maxLength > first.maxLength)
        assertTrue(later.fill > first.fill)
    }

    @Test fun boards_arePortrait() {
        for (level in 1..140) {
            val c = Gardens.configFor(level)
            assertTrue("level $level ${c.cols}x${c.rows}", c.rows > c.cols)
            assertTrue(c.cols <= 15)
        }
    }

    @Test fun firstLevel_teachesTheTap() {
        val c = Gardens.configFor(1)
        assertTrue(c.freeOnly)
        assertEquals(BoardShape.RECT, c.shape)
        assertEquals(5, c.maxArrows)
    }

    @Test fun positionInGarden_isOneBased() {
        val g = Gardens.forLevel(25)
        assertEquals(5, g.positionOf(25))
        assertEquals(1, g.positionOf(21))
    }
}

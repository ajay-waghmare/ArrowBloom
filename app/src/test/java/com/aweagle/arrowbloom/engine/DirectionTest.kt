package com.aweagle.arrowbloom.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectionTest {

    @Test
    fun cardinalProperties_areAccurate() {
        assertEquals(-1, Direction.UP.dRow)
        assertEquals(0, Direction.UP.dCol)
        assertEquals(0f, Direction.UP.degrees)

        assertEquals(0, Direction.RIGHT.dRow)
        assertEquals(1, Direction.RIGHT.dCol)
        assertEquals(90f, Direction.RIGHT.degrees)

        assertEquals(1, Direction.DOWN.dRow)
        assertEquals(0, Direction.DOWN.dCol)
        assertEquals(180f, Direction.DOWN.degrees)

        assertEquals(0, Direction.LEFT.dRow)
        assertEquals(-1, Direction.LEFT.dCol)
        assertEquals(270f, Direction.LEFT.degrees)
    }

    @Test
    fun oppositeDirections_areReflexive() {
        assertEquals(Direction.DOWN, Direction.UP.opposite)
        assertEquals(Direction.UP, Direction.DOWN.opposite)
        assertEquals(Direction.LEFT, Direction.RIGHT.opposite)
        assertEquals(Direction.RIGHT, Direction.LEFT.opposite)

        for (dir in Direction.entries) {
            assertEquals(dir, dir.opposite.opposite)
        }
    }

    @Test
    fun clockwiseRotations_completeCircle() {
        assertEquals(Direction.RIGHT, Direction.UP.turnClockwise())
        assertEquals(Direction.DOWN, Direction.RIGHT.turnClockwise())
        assertEquals(Direction.LEFT, Direction.DOWN.turnClockwise())
        assertEquals(Direction.UP, Direction.LEFT.turnClockwise())

        for (dir in Direction.entries) {
            var rotated = dir
            repeat(4) { rotated = rotated.turnClockwise() }
            assertEquals(dir, rotated)
        }
    }

    @Test
    fun counterClockwiseRotations_completeCircle() {
        assertEquals(Direction.LEFT, Direction.UP.turnCounterClockwise())
        assertEquals(Direction.DOWN, Direction.LEFT.turnCounterClockwise())
        assertEquals(Direction.RIGHT, Direction.DOWN.turnCounterClockwise())
        assertEquals(Direction.UP, Direction.RIGHT.turnCounterClockwise())

        for (dir in Direction.entries) {
            var rotated = dir
            repeat(4) { rotated = rotated.turnCounterClockwise() }
            assertEquals(dir, rotated)
        }
    }
}

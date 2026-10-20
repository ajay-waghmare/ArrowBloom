package com.aweagle.arrowbloom.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoringTest {

    @Test fun points_growWithTheCombo() {
        assertEquals(10, Scoring.pointsFor(1))
        assertEquals(50, Scoring.pointsFor(5))
        assertEquals(10, Scoring.pointsFor(0))
    }

    @Test fun hearts_andStars() {
        assertEquals(3, Scoring.heartsLeft(0))
        assertEquals(1, Scoring.heartsLeft(2))
        assertEquals(0, Scoring.heartsLeft(5))
        assertEquals(3, Scoring.heartsLeft(-1))
        assertEquals(3, Scoring.starsFor(0))
        assertEquals(2, Scoring.starsFor(1))
        assertEquals(1, Scoring.starsFor(2))
        assertEquals(0, Scoring.starsFor(3))
    }
}

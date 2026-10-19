package com.aweagle.arrowbloom.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoringTest {

    @Test fun points_growWithComboAndColourStreak() {
        assertEquals(10, Scoring.pointsFor(combo = 1, colorStreak = 1))
        assertEquals(20, Scoring.pointsFor(combo = 2, colorStreak = 1))
        assertEquals(25, Scoring.pointsFor(combo = 2, colorStreak = 2))
        assertEquals(40, Scoring.pointsFor(combo = 3, colorStreak = 3))
        assertEquals(10, Scoring.pointsFor(combo = 0, colorStreak = 0))   // never below the base
    }

    @Test fun stars_dependOnBlockedTapsOnly() {
        assertEquals(3, Scoring.starsFor(0))
        assertEquals(2, Scoring.starsFor(1))
        assertEquals(2, Scoring.starsFor(2))
        assertEquals(1, Scoring.starsFor(3))
        assertEquals(1, Scoring.starsFor(99))
    }

    @Test fun blockedTapsLeft_forEachStarTier() {
        assertEquals(0, Scoring.blockedTapsLeftFor(3, 0))
        assertEquals(1, Scoring.blockedTapsLeftFor(2, 1))
        assertEquals(0, Scoring.blockedTapsLeftFor(2, 2))
        assertEquals(0, Scoring.blockedTapsLeftFor(1, 5))
    }
}

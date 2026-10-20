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
        assertEquals(5, Scoring.starsFor(0))
        assertEquals(4, Scoring.starsFor(1))
        assertEquals(3, Scoring.starsFor(2))
        assertEquals(0, Scoring.starsFor(3))
    }

    @Test fun everyHeartLostAndEveryHint_costsAStar_butAClearKeepsOne() {
        assertEquals(5, Scoring.starsFor(blockedTaps = 0, hintsUsed = 0))
        assertEquals(4, Scoring.starsFor(blockedTaps = 0, hintsUsed = 1))
        assertEquals(2, Scoring.starsFor(blockedTaps = 0, hintsUsed = 3))
        assertEquals(2, Scoring.starsFor(blockedTaps = 1, hintsUsed = 2))
        assertEquals(1, Scoring.starsFor(blockedTaps = 2, hintsUsed = 3))
        assertEquals(0, Scoring.starsFor(blockedTaps = 3, hintsUsed = 0))
        assertEquals(5, Scoring.MAX_STARS)
    }
}

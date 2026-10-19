package com.aweagle.arrowbloom.data

import com.aweagle.arrowbloom.testutil.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunCodecAndPrefsTest {

    @Test fun run_roundTrips() {
        val run = SavedRun(RunMode.DAILY, 9999, 20261020L, listOf(4, 1, 9), blockedTaps = 2, hintsUsed = 1)
        assertEquals(run, RunCodec.decode(RunCodec.encode(run)))
        val empty = SavedRun(RunMode.LEVEL, 3, 0L, emptyList(), 0, 0)
        assertEquals(empty, RunCodec.decode(RunCodec.encode(empty)))
    }

    @Test fun garbage_decodesToNull() {
        assertNull(RunCodec.decode(null))
        assertNull(RunCodec.decode(""))
        assertNull(RunCodec.decode("v9|LEVEL|1|0||0|0"))
        assertNull(RunCodec.decode("v1|LEVEL|x|0||0|0"))
        assertNull(RunCodec.decode("v1|NOPE|1|0||0|0"))
        assertNull(RunCodec.decode("v1|LEVEL|1|0|1,2"))
    }

    @Test fun prefs_defaults_andStarsRecording() {
        val p = Prefs(FakeSharedPreferences())
        assertEquals(1, p.currentLevel)
        assertEquals(0, p.highestCleared)
        assertTrue(p.bestStars().isEmpty())
        assertTrue(p.soundOn && p.musicOn && p.hapticsOn)
        assertNull(p.savedRun)

        assertTrue(p.recordStars(3, 2))
        assertFalse(p.recordStars(3, 1))      // not better
        assertTrue(p.recordStars(3, 3))
        assertTrue(p.recordStars(1, 3))
        assertEquals(mapOf(1 to 3, 3 to 3), p.bestStars())
        assertEquals("1:3,3:3", p.bestStarsCsv)

        p.savedRun = "v1|LEVEL|2|0|1|0|0"
        assertEquals("v1|LEVEL|2|0|1|0|0", p.savedRun)
        p.savedRun = null
        assertNull(p.savedRun)
    }

    @Test fun prefs_neverStoreNegatives() {
        val p = Prefs(FakeSharedPreferences())
        p.currentLevel = -4
        p.dailyStreak = -1
        p.dailyStars = 9
        assertEquals(1, p.currentLevel)
        assertEquals(0, p.dailyStreak)
        assertEquals(3, p.dailyStars)
    }
}

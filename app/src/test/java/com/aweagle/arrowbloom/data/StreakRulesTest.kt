package com.aweagle.arrowbloom.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakRulesTest {

    @Test fun firstEver_startsAtOne() {
        val o = StreakRules.advance(lastDay = -1, today = 100, streak = 0, shieldUsed = false)!!
        assertEquals(1, o.streak)
        assertTrue(o.resetShield)
    }

    @Test fun nextDay_extends_sameDayIsNull() {
        assertEquals(4, StreakRules.advance(99, 100, 3, false)!!.streak)
        assertNull(StreakRules.advance(100, 100, 3, false))
    }

    @Test fun oneMissedDay_isForgivenOnceAboveSeven() {
        val o = StreakRules.advance(lastDay = 98, today = 100, streak = 8, shieldUsed = false)!!
        assertEquals(9, o.streak)
        assertTrue(o.usedShield)
        assertFalse(o.resetShield)
        // shield already spent: the streak resets
        val r = StreakRules.advance(98, 100, 9, true)!!
        assertEquals(1, r.streak)
        assertTrue(r.resetShield)
        // below seven there is no shield
        assertEquals(1, StreakRules.advance(98, 100, 3, false)!!.streak)
    }

    @Test fun twoMissedDays_alwaysReset() {
        assertEquals(1, StreakRules.advance(97, 100, 20, false)!!.streak)
    }

    @Test fun current_showsWhatStillStandsToday() {
        assertEquals(5, StreakRules.current(lastDay = 100, today = 100, streak = 5, shieldUsed = false))
        assertEquals(5, StreakRules.current(99, 100, 5, false))
        assertEquals(0, StreakRules.current(98, 100, 5, false))
        assertEquals(8, StreakRules.current(98, 100, 8, false))     // shielded
        assertEquals(0, StreakRules.current(98, 100, 8, true))
        assertEquals(0, StreakRules.current(-1, 100, 0, false))
    }
}

package com.aweagle.arrowbloom.daily

import com.aweagle.arrowbloom.engine.PuzzleSolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyBloomTest {

    @Test fun seed_isTheDate() {
        assertEquals(20261020L, DailyBloom.seedFor(LocalDate.of(2026, 10, 20)))
        assertEquals(20270101L, DailyBloom.seedFor(LocalDate.of(2027, 1, 1)))
    }

    @Test fun number_countsFromLaunchDay() {
        assertEquals(1, DailyBloom.numberFor(DailyBloom.EPOCH))
        assertEquals(10, DailyBloom.numberFor(DailyBloom.EPOCH.plusDays(9)))
        assertEquals(1, DailyBloom.numberFor(DailyBloom.EPOCH.minusDays(3)))   // never below #1
    }

    @Test fun board_isDeterministic_andSolvable() {
        val d = LocalDate.of(2026, 11, 5)
        val a = DailyBloom.board(d)
        val b = DailyBloom.board(d)
        assertEquals(a.arrows.keys, b.arrows.keys)
        for (id in a.arrows.keys) assertEquals(a.arrows[id], b.arrows[id])
        assertTrue(PuzzleSolver.isSolvable(a))
        assertTrue("daily arrows ${a.totalArrows}", a.totalArrows >= 15)
        val shapes = HashSet<com.aweagle.arrowbloom.engine.BoardShape>()
        for (i in 0 until 30) {
            val day = DailyBloom.board(d.plusDays(i.toLong()))
            assertTrue(PuzzleSolver.isSolvable(day))
            shapes.add(day.shape)
        }
        assertTrue("the daily board changes shape through the week", shapes.size >= 4)
    }

    @Test fun shareText_isSpoilerFree() {
        val t = DailyBloom.shareText(number = 12, stars = 3, arrows = 24, blockedTaps = 0, storeUrl = "https://example.test/app")
        assertTrue(t.contains("Daily #12"))
        assertTrue(t.contains("⭐⭐⭐"))
        assertTrue(t.contains("no hearts lost"))
        assertTrue(t.endsWith("https://example.test/app"))
        val u = DailyBloom.shareText(number = 3, stars = 1, arrows = 20, blockedTaps = 2, storeUrl = "u")
        assertTrue(u.contains("⭐☆☆"))
        assertTrue(u.contains("2 hearts lost"))
        assertTrue(DailyBloom.shareText(1, 2, 20, 1, "u").contains("1 heart lost\n"))
    }
}

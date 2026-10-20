package com.aweagle.arrowbloom.daily

import com.aweagle.arrowbloom.engine.Board
import com.aweagle.arrowbloom.engine.PuzzleGenerator
import com.aweagle.arrowbloom.engine.Scoring
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The Daily Bloom: one board per calendar day, the same for everyone, numbered from the
 * launch date. Deterministic from the date alone, so it works offline and is comparable
 * between players, which is what makes the share text meaningful.
 */
object DailyBloom {

    val EPOCH: LocalDate = LocalDate.of(2026, 10, 20)

    fun seedFor(date: LocalDate): Long = date.year * 10_000L + date.monthValue * 100L + date.dayOfMonth

    /** 1-based puzzle number, #1 on launch day. */
    fun numberFor(date: LocalDate): Int = (ChronoUnit.DAYS.between(EPOCH, date) + 1).toInt().coerceAtLeast(1)

    fun board(date: LocalDate): Board = PuzzleGenerator.generateDailyChallenge(seedFor(date))

    /**
     * Spoiler-free result card: stars, the clean-solve line and the link. Kept in English on
     * purpose, the way Wordle's is: it travels between players who do not share a language.
     */
    fun shareText(number: Int, stars: Int, arrows: Int, blockedTaps: Int, storeUrl: String): String {
        val starRow = "⭐".repeat(stars.coerceIn(0, Scoring.MAX_STARS)) + "☆".repeat(Scoring.MAX_STARS - stars.coerceIn(0, Scoring.MAX_STARS))
        val clean = if (blockedTaps == 0) "no hearts lost" else "$blockedTaps heart${if (blockedTaps == 1) "" else "s"} lost"
        return buildString {
            append("🌸 Arrow Bloom Daily #").append(number).append('\n')
            append(starRow).append(" · ").append(arrows).append(" arrows · ").append(clean).append('\n')
            append("Can you clear it without losing a heart? ").append(storeUrl)
        }
    }
}

package com.aweagle.arrowbloom.engine

/**
 * The scoring rules in one place, pure so they are unit-tested.
 *
 * Combo: consecutive escapes without a blocked tap, each worth more than the last.
 * Colour streak: escaping the same colour twice in a row adds a small bonus, which gives a
 * player who looks ahead something to plan for without punishing one who does not.
 * Stars: a level's par is its arrow count (every arrow needs exactly one tap); the stars say
 * how clean the solve was, measured in blocked taps, not in time. The game has no timer.
 */
object Scoring {

    const val BASE_POINTS = 10
    const val COLOR_BONUS = 5
    const val MAX_STARS = 3

    fun pointsFor(combo: Int, colorStreak: Int): Int =
        BASE_POINTS * combo.coerceAtLeast(1) + COLOR_BONUS * (colorStreak - 1).coerceAtLeast(0)

    fun starsFor(blockedTaps: Int): Int = when {
        blockedTaps <= 0 -> 3
        blockedTaps <= 2 -> 2
        else -> 1
    }

    /** Blocked taps a player may still make and keep the current star count. */
    fun blockedTapsLeftFor(stars: Int, blockedTaps: Int): Int = when (stars) {
        3 -> 0
        2 -> (2 - blockedTaps).coerceAtLeast(0)
        else -> 0
    }
}

package com.aweagle.arrowbloom.engine

/**
 * The scoring rules in one place, pure so they are unit-tested.
 *
 * Hearts: every board starts with three. A tap on an arrow that cannot leave bumps it back
 * and costs a heart; at zero hearts the board is lost and starts over. There is no timer.
 * Stars: a cleared board is worth up to five. Every heart lost and every hint used costs one,
 * so five stars means no wrong tap and no hint; a cleared board always keeps at least one.
 * Combo: consecutive escapes without a wrong tap, each worth more than the last.
 */
object Scoring {

    const val BASE_POINTS = 10
    const val MAX_STARS = 5
    const val MAX_HEARTS = 3

    fun pointsFor(combo: Int): Int = BASE_POINTS * combo.coerceAtLeast(1)

    fun heartsLeft(blockedTaps: Int): Int = (MAX_HEARTS - blockedTaps.coerceAtLeast(0)).coerceAtLeast(0)

    /** Stars for a board: 5 minus hearts lost minus hints used, at least 1; 0 when the board was lost. */
    fun starsFor(blockedTaps: Int, hintsUsed: Int = 0): Int {
        if (heartsLeft(blockedTaps) == 0) return 0
        return (MAX_STARS - blockedTaps.coerceAtLeast(0) - hintsUsed.coerceAtLeast(0)).coerceIn(1, MAX_STARS)
    }
}

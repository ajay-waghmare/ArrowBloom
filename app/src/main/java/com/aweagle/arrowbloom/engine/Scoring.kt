package com.aweagle.arrowbloom.engine

/**
 * The scoring rules in one place, pure so they are unit-tested.
 *
 * Hearts: every board starts with three. A tap on an arrow that cannot leave bumps it back
 * and costs a heart; at zero hearts the board is lost and starts over. Stars are the hearts
 * kept, so three stars means not a single wrong tap. There is no timer.
 * Combo: consecutive escapes without a wrong tap, each worth more than the last.
 */
object Scoring {

    const val BASE_POINTS = 10
    const val MAX_STARS = 3
    const val MAX_HEARTS = 3

    fun pointsFor(combo: Int): Int = BASE_POINTS * combo.coerceAtLeast(1)

    fun heartsLeft(blockedTaps: Int): Int = (MAX_HEARTS - blockedTaps.coerceAtLeast(0)).coerceAtLeast(0)

    /** 3 for a clean board, one less per heart lost; 0 means the board was lost. */
    fun starsFor(blockedTaps: Int): Int = heartsLeft(blockedTaps).coerceAtMost(MAX_STARS)
}

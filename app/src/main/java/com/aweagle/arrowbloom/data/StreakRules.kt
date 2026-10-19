package com.aweagle.arrowbloom.data

/**
 * Daily Bloom streak rules, pure so they are unit-tested.
 *
 * A day counts when the Daily Bloom is completed. Once a streak reaches [SHIELD_MIN_STREAK],
 * one missed day is forgiven; the shield is spent on use and comes back only after the streak
 * resets and climbs to the threshold again.
 */
object StreakRules {

    const val SHIELD_MIN_STREAK = 7

    data class Outcome(val streak: Int, val usedShield: Boolean, val resetShield: Boolean)

    fun shieldAvailable(streak: Int, shieldUsed: Boolean): Boolean = !shieldUsed && streak >= SHIELD_MIN_STREAK

    /**
     * @param lastDay epoch day of the last completed Daily Bloom, or -1 if never.
     * @return null when today is already counted; otherwise the new streak.
     */
    fun advance(lastDay: Long, today: Long, streak: Int, shieldUsed: Boolean): Outcome? {
        if (lastDay == today) return null
        val gap = if (lastDay < 0) Long.MAX_VALUE else today - lastDay
        return when {
            gap == 1L -> Outcome(streak + 1, usedShield = false, resetShield = false)
            gap == 2L && shieldAvailable(streak, shieldUsed) -> Outcome(streak + 1, usedShield = true, resetShield = false)
            else -> Outcome(1, usedShield = false, resetShield = true)
        }
    }

    /** The streak as it stands today without completing anything: broken if two days passed (one with a shield). */
    fun current(lastDay: Long, today: Long, streak: Int, shieldUsed: Boolean): Int {
        if (lastDay < 0) return 0
        val gap = today - lastDay
        return when {
            gap <= 1L -> streak
            gap == 2L && shieldAvailable(streak, shieldUsed) -> streak
            else -> 0
        }
    }
}

package com.aweagle.arrowbloom.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.aweagle.arrowbloom.engine.Scoring

/**
 * Every persisted value of the game, in one SharedPreferences file. The internal constructor
 * takes any [SharedPreferences], so the rules that read and write these values are tested on
 * the JVM with an in-memory fake. Nothing here leaves the device.
 */
class Prefs internal constructor(private val sp: SharedPreferences) {

    constructor(context: Context) : this(context.getSharedPreferences(FILE, Context.MODE_PRIVATE))

    // ----- Journey progress -----------------------------------------------

    /** The level the player is on (the first not yet cleared, or the one being replayed). */
    var currentLevel: Int
        get() = sp.getInt(KEY_LEVEL, 1).coerceAtLeast(1)
        set(value) = sp.edit { putInt(KEY_LEVEL, value.coerceAtLeast(1)) }

    /** Highest level ever cleared; 0 until the first clear. */
    var highestCleared: Int
        get() = sp.getInt(KEY_HIGHEST, 0)
        set(value) = sp.edit { putInt(KEY_HIGHEST, value.coerceAtLeast(0)) }

    /** Best stars per level, as "level:stars" pairs. */
    var bestStarsCsv: String
        get() = sp.getString(KEY_STARS, "") ?: ""
        set(value) = sp.edit { putString(KEY_STARS, value) }

    fun bestStars(): Map<Int, Int> =
        bestStarsCsv.split(",").mapNotNull { part ->
            val kv = part.split(":")
            val level = kv.getOrNull(0)?.trim()?.toIntOrNull() ?: return@mapNotNull null
            val stars = kv.getOrNull(1)?.trim()?.toIntOrNull() ?: return@mapNotNull null
            level to stars.coerceIn(0, Scoring.MAX_STARS)
        }.toMap()

    /** Records [stars] for [level] if better than before; returns true when it was. */
    fun recordStars(level: Int, stars: Int): Boolean {
        val all = bestStars().toMutableMap()
        val previous = all[level] ?: 0
        if (stars <= previous) return false
        all[level] = stars
        bestStarsCsv = all.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value}" }
        return true
    }

    // ----- Daily Bloom + streak --------------------------------------------

    /** Epoch day of the last completed Daily Bloom, or -1. */
    var lastDailyDay: Long
        get() = sp.getLong(KEY_DAILY_DAY, -1L)
        set(value) = sp.edit { putLong(KEY_DAILY_DAY, value) }

    var dailyStars: Int
        get() = sp.getInt(KEY_DAILY_STARS, 0)
        set(value) = sp.edit { putInt(KEY_DAILY_STARS, value.coerceIn(0, Scoring.MAX_STARS)) }

    var dailyBlockedTaps: Int
        get() = sp.getInt(KEY_DAILY_BLOCKED, 0)
        set(value) = sp.edit { putInt(KEY_DAILY_BLOCKED, value.coerceAtLeast(0)) }

    /** Hints used on the best Daily Bloom run of the day (each costs a star). */
    var dailyHints: Int
        get() = sp.getInt(KEY_DAILY_HINTS, 0)
        set(value) = sp.edit { putInt(KEY_DAILY_HINTS, value.coerceAtLeast(0)) }

    var dailyStreak: Int
        get() = sp.getInt(KEY_STREAK, 0)
        set(value) = sp.edit { putInt(KEY_STREAK, value.coerceAtLeast(0)) }

    var bestStreak: Int
        get() = sp.getInt(KEY_BEST_STREAK, 0)
        set(value) = sp.edit { putInt(KEY_BEST_STREAK, value.coerceAtLeast(0)) }

    var streakShieldUsed: Boolean
        get() = sp.getBoolean(KEY_SHIELD_USED, false)
        set(value) = sp.edit { putBoolean(KEY_SHIELD_USED, value) }

    // ----- Lifetime statistics -----------------------------------------------

    var levelsCleared: Int
        get() = sp.getInt(KEY_LEVELS_CLEARED, 0)
        set(value) = sp.edit { putInt(KEY_LEVELS_CLEARED, value.coerceAtLeast(0)) }

    var arrowsEscaped: Int
        get() = sp.getInt(KEY_ARROWS, 0)
        set(value) = sp.edit { putInt(KEY_ARROWS, value.coerceAtLeast(0)) }

    var bestCombo: Int
        get() = sp.getInt(KEY_BEST_COMBO, 0)
        set(value) = sp.edit { putInt(KEY_BEST_COMBO, value.coerceAtLeast(0)) }

    var perfectClears: Int
        get() = sp.getInt(KEY_PERFECT, 0)
        set(value) = sp.edit { putInt(KEY_PERFECT, value.coerceAtLeast(0)) }

    // ----- Settings ----------------------------------------------------------

    var soundOn: Boolean
        get() = sp.getBoolean(KEY_SOUND, true)
        set(value) = sp.edit { putBoolean(KEY_SOUND, value) }

    var musicOn: Boolean
        get() = sp.getBoolean(KEY_MUSIC, true)
        set(value) = sp.edit { putBoolean(KEY_MUSIC, value) }

    var hapticsOn: Boolean
        get() = sp.getBoolean(KEY_HAPTICS, true)
        set(value) = sp.edit { putBoolean(KEY_HAPTICS, value) }

    /** Faint cell grid behind the arrows (the # button on the game screen). */
    var gridOn: Boolean
        get() = sp.getBoolean(KEY_GRID, false)
        set(value) = sp.edit { putBoolean(KEY_GRID, value) }

    /** The first-level coaching has been seen through to a completed board. */
    var coached: Boolean
        get() = sp.getBoolean(KEY_COACHED, false)
        set(value) = sp.edit { putBoolean(KEY_COACHED, value) }

    // ----- Resume ------------------------------------------------------------

    /** The in-progress run (see [RunCodec]), or null. */
    var savedRun: String?
        get() = sp.getString(KEY_RUN, null)
        set(value) = sp.edit { if (value == null) remove(KEY_RUN) else putString(KEY_RUN, value) }

    companion object {
        const val FILE = "arrow_bloom_prefs"
        // "saved_level" is the key the scaffold used; kept so the first testers keep their level.
        private const val KEY_LEVEL = "saved_level"
        private const val KEY_HIGHEST = "highest_cleared"
        private const val KEY_STARS = "best_stars"
        private const val KEY_DAILY_DAY = "daily_last_day"
        private const val KEY_DAILY_STARS = "daily_stars"
        private const val KEY_DAILY_BLOCKED = "daily_blocked"
        private const val KEY_DAILY_HINTS = "daily_hints"
        private const val KEY_STREAK = "daily_streak"
        private const val KEY_BEST_STREAK = "best_streak"
        private const val KEY_SHIELD_USED = "streak_shield_used"
        private const val KEY_LEVELS_CLEARED = "levels_cleared"
        private const val KEY_ARROWS = "arrows_escaped"
        private const val KEY_BEST_COMBO = "best_combo"
        private const val KEY_PERFECT = "perfect_clears"
        private const val KEY_SOUND = "sound_on"
        private const val KEY_MUSIC = "music_on"
        private const val KEY_HAPTICS = "haptics_on"
        private const val KEY_COACHED = "coached"
        private const val KEY_GRID = "grid_on"
        private const val KEY_RUN = "saved_run"
    }
}

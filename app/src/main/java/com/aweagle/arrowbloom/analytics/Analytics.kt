package com.aweagle.arrowbloom.analytics

/**
 * The one place gameplay reports events to. Version 1.0 ships with [NoOpAnalytics]: the
 * game collects nothing and the Play Data safety form says so. When Firebase is added
 * (a `google-services.json` for this package and one implementation of this interface),
 * every event below starts flowing without touching the view model.
 */
interface Analytics {
    fun event(name: String, params: Map<String, Any?> = emptyMap())

    companion object {
        const val LEVEL_START = "level_start"
        const val LEVEL_COMPLETE = "level_complete"
        const val DAILY_START = "daily_start"
        const val DAILY_COMPLETE = "daily_complete"
        const val FIRST_ESCAPE = "first_escape"
        const val BLOCKED_TAP = "blocked_tap"
        const val HINT_USED = "hint_used"
        const val UNDO = "undo"
        const val SHARE_RESULT = "share_result"
    }
}

object NoOpAnalytics : Analytics {
    override fun event(name: String, params: Map<String, Any?>) = Unit
}

package com.aweagle.arrowbloom.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Haptics. Sound lives in [MusicEngine]; this only answers the finger. Every call is gated
 * by the in-app setting through [hapticsOn] and every failure is swallowed.
 */
class Feedback(
    context: Context,
    private val hapticsOn: () -> Boolean,
) {
    private val appCtx = context.applicationContext

    private val vibrator: Vibrator? = run {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = appCtx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            mgr?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appCtx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /** Finger down on anything tappable: the button answers before it acts. */
    fun press() = buzz(9)

    fun tap() = buzz(12)

    /** An arrow escaped; a touch stronger as the combo grows. */
    fun escape(combo: Int) = buzz(16L + (combo - 1).coerceIn(0, 6) * 4L)

    /** A blocked tap: a short double buzz, clearly different from an escape. */
    fun blocked() {
        if (!hapticsOn()) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        try {
            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 28, 50, 28), -1))
        } catch (t: Throwable) { Log.w(TAG, "vibrate failed", t) }
    }

    fun complete() = buzz(60)

    private fun buzz(ms: Long) {
        if (!hapticsOn()) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        try {
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (t: Throwable) { Log.w(TAG, "vibrate failed", t) }
    }

    companion object { private const val TAG = "Feedback" }
}

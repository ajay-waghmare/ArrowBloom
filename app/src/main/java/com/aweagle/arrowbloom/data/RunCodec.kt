package com.aweagle.arrowbloom.data

enum class RunMode { LEVEL, DAILY }

/** Everything needed to put a player back mid-board after the process died. */
data class SavedRun(
    val mode: RunMode,
    val level: Int,
    val dateSeed: Long,
    val escapeOrder: List<Int>,
    val blockedTaps: Int,
    val hintsUsed: Int,
)

/**
 * Text form of a [SavedRun] for the single preferences slot. Boards are deterministic from
 * the level number or the date seed, so only the taps need saving.
 * Format: `v1|MODE|level|dateSeed|id,id,id|blockedTaps|hintsUsed`. Garbage decodes to null.
 */
object RunCodec {

    private const val HEADER = "v1"

    fun encode(run: SavedRun): String = listOf(
        HEADER,
        run.mode.name,
        run.level,
        run.dateSeed,
        run.escapeOrder.joinToString(","),
        run.blockedTaps,
        run.hintsUsed,
    ).joinToString("|")

    fun decode(raw: String?): SavedRun? {
        if (raw.isNullOrBlank()) return null
        return try {
            val p = raw.split("|")
            if (p.size != 7 || p[0] != HEADER) return null
            val order = if (p[4].isBlank()) emptyList() else p[4].split(",").map { it.trim().toInt() }
            SavedRun(
                mode = RunMode.valueOf(p[1]),
                level = p[2].toInt(),
                dateSeed = p[3].toLong(),
                escapeOrder = order,
                blockedTaps = p[5].toInt().coerceAtLeast(0),
                hintsUsed = p[6].toInt().coerceAtLeast(0),
            )
        } catch (e: RuntimeException) {
            null
        }
    }
}

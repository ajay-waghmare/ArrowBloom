package com.aweagle.arrowbloom.engine

import kotlin.random.Random

/**
 * Everything the generator needs for one board.
 *
 * @param fill share of the shape's cells to cover with arrows (the rest show as dots).
 * @param straightness chance that a body keeps going straight at each step instead of turning.
 * @param freeOnly no arrow may block another: every arrow can leave from the start (the first board).
 * @param attempts boards built from the same seed; the one with the deepest solve order wins.
 * @param inwardBias chance that the next head is chosen among the longest open lines, which
 *   makes later arrows land in front of earlier ones and so deepens the order.
 */
data class LevelConfig(
    val levelNumber: Int,
    val rows: Int,
    val cols: Int,
    val shape: BoardShape = BoardShape.RECT,
    val minLength: Int = 2,
    val maxLength: Int = 4,
    val fill: Float = 0.75f,
    val straightness: Float = 0.55f,
    val maxArrows: Int = Int.MAX_VALUE,
    val freeOnly: Boolean = false,
    val attempts: Int = 1,
    val inwardBias: Float = 0.6f,
)

/**
 * Builds winding-arrow boards that are always solvable.
 *
 * An arrow can leave when the line from its head to the edge is empty (see [CollisionDetector]).
 * Boards are built in reverse: each new arrow is given a head whose line to the edge is empty
 * at the moment it is placed, then a body that winds backwards from the head through free
 * cells. Arrows placed later may sit in front of earlier ones, never the other way round, so
 * tapping arrows in the reverse of the order they were placed always clears the board, and
 * because leaving only ever frees cells, any free arrow is a safe tap: greedy play cannot get
 * stuck. [PuzzleSolver] double-checks every board anyway.
 */
object PuzzleGenerator {

    const val DAILY_LEVEL_NUMBER = 9999
    private const val MIN_ARROWS = 3
    private const val MAX_MISSES = 400

    fun getConfigForLevel(level: Int): LevelConfig = Gardens.configFor(level)

    /** Deterministic: the same level number always produces the same board. */
    fun generateLevel(level: Int): Board = generate(getConfigForLevel(level), Random(seed = level * 31337L + 20261020L))

    /** The Daily Bloom's board for a date seed such as 20261020; its shape changes day by day. */
    fun dailyConfig(dateSeed: Long): LevelConfig {
        val shapes = listOf(BoardShape.FLOWER, BoardShape.HEART, BoardShape.CIRCLE, BoardShape.DIAMOND, BoardShape.RECT)
        val shape = shapes[Math.floorMod(dateSeed, shapes.size.toLong()).toInt()]
        val shaped = shape != BoardShape.RECT
        return LevelConfig(
            levelNumber = DAILY_LEVEL_NUMBER,
            rows = if (shaped) 14 else 12,
            cols = if (shaped) 11 else 9,
            shape = shape,
            minLength = 3,
            maxLength = 7,
            fill = 0.86f,
            straightness = 0.5f,
            attempts = 3,
        )
    }

    /** Deterministic daily board from a date seed such as 20261020. */
    fun generateDailyChallenge(dateSeed: Long): Board = generate(dailyConfig(dateSeed), Random(seed = dateSeed))

    fun generate(config: LevelConfig, random: Random = Random.Default): Board {
        var best: Board? = null
        var bestKey = Int.MIN_VALUE
        repeat(config.attempts.coerceAtLeast(1)) {
            val board = build(config, random)
            if (board.totalArrows >= MIN_ARROWS && PuzzleSolver.isSolvable(board)) {
                val key = PuzzleStats.depth(board) * 1000 + board.totalArrows
                if (key > bestKey) {
                    best = board
                    bestKey = key
                }
            }
        }
        return best ?: fallback(config)
    }

    private fun build(config: LevelConfig, random: Random): Board {
        val rows = config.rows
        val cols = config.cols
        val n = rows * cols
        val playable = BooleanArray(n) { config.shape.contains(it / cols, it % cols, rows, cols) }
        val target = (playable.count { it } * config.fill).toInt()
        val occ = IntArray(n)
        val onRay = BooleanArray(n)
        val clear = Array(4) { BooleanArray(n) }
        val rejected = HashSet<Int>()
        val arrows = LinkedHashMap<Int, Arrow>()
        val dirs = Direction.entries
        var covered = 0
        var misses = 0

        while (covered < target && arrows.size < config.maxArrows && misses < MAX_MISSES) {
            computeClear(clear, occ, rows, cols)
            val candidates = ArrayList<Int>()
            for (i in 0 until n) {
                if (!playable[i] || occ[i] != 0) continue
                if (config.freeOnly && onRay[i]) continue
                for (d in dirs.indices) {
                    if (!clear[d][i]) continue
                    val key = i * 4 + d
                    if (key in rejected) continue
                    val behind = GridPos(i / cols, i % cols).moved(dirs[d].opposite)
                    if (!behind.isInside(rows, cols)) continue
                    val bi = behind.row * cols + behind.col
                    if (!playable[bi] || occ[bi] != 0 || (config.freeOnly && onRay[bi])) continue
                    candidates.add(key)
                }
            }
            if (candidates.isEmpty()) break

            val key = pick(candidates, config, random, rows, cols)
            val head = GridPos((key / 4) / cols, (key / 4) % cols)
            val dir = dirs[key % 4]
            val body = grow(head, dir, config, random, occ, playable, onRay)
            if (body.size < config.minLength) {
                rejected.add(key)
                misses++
                continue
            }
            val id = arrows.size + 1
            arrows[id] = Arrow(id = id, head = head, direction = dir, body = body)
            for (p in body) occ[p.row * cols + p.col] = id
            covered += body.size
            if (config.freeOnly) {
                var p = head.moved(dir)
                while (p.isInside(rows, cols)) {
                    onRay[p.row * cols + p.col] = true
                    p = p.moved(dir)
                }
            }
            rejected.clear()
        }
        return Board(rows, cols, arrows, config.shape)
    }

    /** Mostly the head with the longest open line (deeper boards), sometimes any head. */
    private fun pick(candidates: List<Int>, config: LevelConfig, random: Random, rows: Int, cols: Int): Int {
        if (random.nextFloat() >= config.inwardBias) return candidates[random.nextInt(candidates.size)]
        val dirs = Direction.entries
        fun ray(key: Int) = CollisionDetector.rayLength(GridPos((key / 4) / cols, (key / 4) % cols), dirs[key % 4], rows, cols)
        val sorted = candidates.sortedByDescending { ray(it) }
        val top = sorted.take((sorted.size / 4).coerceAtLeast(1))
        return top[random.nextInt(top.size)]
    }

    /** The head, the cell behind it, then a walk backwards through free cells. */
    private fun grow(
        head: GridPos,
        dir: Direction,
        config: LevelConfig,
        random: Random,
        occ: IntArray,
        playable: BooleanArray,
        onRay: BooleanArray,
    ): List<GridPos> {
        val rows = config.rows
        val cols = config.cols
        val ahead = HashSet<GridPos>()
        var p = head.moved(dir)
        while (p.isInside(rows, cols)) {
            ahead.add(p)
            p = p.moved(dir)
        }
        val path = arrayListOf(head)
        fun ok(c: GridPos): Boolean {
            if (!c.isInside(rows, cols)) return false
            val i = c.row * cols + c.col
            return playable[i] && occ[i] == 0 && c !in ahead && c !in path && !(config.freeOnly && onRay[i])
        }
        val second = head.moved(dir.opposite)
        if (!ok(second)) return path
        path.add(second)
        var heading = dir.opposite
        val targetLength = random.nextInt(config.minLength, config.maxLength.coerceAtLeast(config.minLength) + 1)
        val turnWeight = (1f - config.straightness) / 2f
        while (path.size < targetLength) {
            val last = path.last()
            val options = ArrayList<Pair<Direction, Float>>(3)
            if (ok(last.moved(heading))) options.add(heading to config.straightness)
            val left = heading.turnCounterClockwise()
            val right = heading.turnClockwise()
            if (ok(last.moved(left))) options.add(left to turnWeight)
            if (ok(last.moved(right))) options.add(right to turnWeight)
            if (options.isEmpty()) break
            var roll = random.nextFloat() * options.sumOf { it.second.toDouble() }.toFloat()
            var choice = options.last().first
            for ((d, w) in options) {
                if (roll < w) { choice = d; break }
                roll -= w
            }
            path.add(last.moved(choice))
            heading = choice
        }
        return path
    }

    /** clear[d][i]: every cell ahead of cell i in direction d, up to the edge, is empty. */
    private fun computeClear(clear: Array<BooleanArray>, occ: IntArray, rows: Int, cols: Int) {
        val up = clear[Direction.UP.ordinal]
        val down = clear[Direction.DOWN.ordinal]
        val left = clear[Direction.LEFT.ordinal]
        val right = clear[Direction.RIGHT.ordinal]
        for (r in 0 until rows) for (c in 0 until cols) {
            val i = r * cols + c
            up[i] = r == 0 || (occ[i - cols] == 0 && up[i - cols])
            left[i] = c == 0 || (occ[i - 1] == 0 && left[i - 1])
        }
        for (r in rows - 1 downTo 0) for (c in cols - 1 downTo 0) {
            val i = r * cols + c
            down[i] = r == rows - 1 || (occ[i + cols] == 0 && down[i + cols])
            right[i] = c == cols - 1 || (occ[i + 1] == 0 && right[i + 1])
        }
    }

    /** Two-cell arrows on the top and bottom rows pointing out: trivially solvable, never expected. */
    private fun fallback(config: LevelConfig): Board {
        val arrows = LinkedHashMap<Int, Arrow>()
        val rows = config.rows
        val cols = config.cols
        fun playable(r: Int, c: Int) = r in 0 until rows && c in 0 until cols && config.shape.contains(r, c, rows, cols)
        for (c in 0 until cols) {
            val top = (0 until rows - 1).firstOrNull { playable(it, c) && playable(it + 1, c) }
            if (top != null && (0 until top).none { playable(it, c) }) {
                val id = arrows.size + 1
                arrows[id] = Arrow(id, GridPos(top, c), Direction.UP, listOf(GridPos(top, c), GridPos(top + 1, c)))
            }
        }
        if (arrows.isEmpty()) {
            arrows[1] = Arrow(1, GridPos(0, 0), Direction.UP, listOf(GridPos(0, 0), GridPos(1, 0)))
            return Board(rows, cols, arrows)
        }
        return Board(rows, cols, arrows, config.shape)
    }
}

enum class Difficulty { EASY, NORMAL, HARD, EXPERT }

/** Measures a board. */
object PuzzleStats {

    /**
     * Rounds needed when every free arrow leaves at once in each round: the length of the
     * longest chain of arrows that wait on each other. -1 for an unsolvable board.
     */
    fun depth(board: Board): Int {
        var b = board
        var rounds = 0
        while (!b.isCompleted) {
            val free = CollisionDetector.findEscapableArrows(b)
            if (free.isEmpty()) return -1
            val arrows = b.arrows.toMutableMap()
            for (a in free) arrows[a.id] = a.withState(ArrowState.ESCAPED)
            b = b.copy(arrows = arrows)
            rounds++
        }
        return rounds
    }

    fun difficultyOf(board: Board): Difficulty {
        val d = depth(board)
        return when {
            d <= 3 -> Difficulty.EASY
            d <= 6 -> Difficulty.NORMAL
            d <= 8 -> Difficulty.HARD
            else -> Difficulty.EXPERT
        }
    }
}

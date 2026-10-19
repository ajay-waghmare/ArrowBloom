package com.aweagle.arrowbloom.engine

import kotlin.random.Random

data class LevelConfig(
    val levelNumber: Int,
    val rows: Int,
    val cols: Int,
    val arrowCount: Int,
    val colorCount: Int = 4,
    val longArrowChance: Float = 0f,
    val maxBodyLength: Int = 1,
)

/**
 * Procedural level generator producing verified-solvable arrow puzzles.
 *
 * Obstruction graph: arrow `u` depends on arrow `v` (edge `u -> v`) when any cell of `v` lies
 * on the forward ray of any cell of `u`. Removing an arrow only ever vacates cells, so if the
 * graph is acyclic a topological order exists and greedy tapping of free arrows always
 * finishes the board. Arrows are placed one at a time and a candidate is kept only if the
 * graph stays acyclic; the solver double-checks the result.
 *
 * Long arrows (bodies of two or three cells, the head in front) arrive from the second
 * garden. Their body is laid out behind the head, and every body cell casts its own ray,
 * so a long arrow is blocked by anything in front of any part of it.
 */
object PuzzleGenerator {

    const val DAILY_LEVEL_NUMBER = 9999

    fun getConfigForLevel(level: Int): LevelConfig = Gardens.configFor(level)

    /** Deterministic: the same level number always produces the same board. */
    fun generateLevel(level: Int): Board {
        val config = getConfigForLevel(level)
        return generate(config, Random(seed = level * 31337L + 20261019L))
    }

    /** Deterministic daily board from a date seed such as 20261020. */
    fun generateDailyChallenge(dateSeed: Long): Board {
        val config = LevelConfig(
            levelNumber = DAILY_LEVEL_NUMBER,
            rows = 6, cols = 6,
            arrowCount = 24,
            colorCount = 5,
            longArrowChance = 0.25f,
            maxBodyLength = 2,
        )
        return generate(config, Random(seed = dateSeed))
    }

    fun generate(config: LevelConfig, random: Random = Random.Default): Board {
        val rows = config.rows
        val cols = config.cols
        val target = minOf(config.arrowCount, rows * cols)

        val positions = ArrayList<GridPos>(rows * cols)
        for (r in 0 until rows) for (c in 0 until cols) positions.add(GridPos(r, c))
        positions.shuffle(random)

        val occupied = HashSet<GridPos>()
        val placed = LinkedHashMap<Int, Arrow>()
        var nextId = 1

        for (pos in positions) {
            if (placed.size >= target) break
            if (pos in occupied) continue
            val wantLong = config.maxBodyLength > 1 && random.nextFloat() < config.longArrowChance
            val lengths = if (wantLong) (config.maxBodyLength downTo 1).toList() else listOf(1)
            var done = false
            for (dir in Direction.entries.shuffled(random)) {
                for (len in lengths) {
                    val body = bodyFor(pos, dir, len)
                    if (body.any { !it.isInside(rows, cols) || it in occupied }) continue
                    val candidate = Arrow(
                        id = nextId,
                        head = pos,
                        direction = dir,
                        body = body,
                        colorIndex = random.nextInt(config.colorCount),
                    )
                    val candidateBoard = Board(rows, cols, placed + (nextId to candidate))
                    if (isGraphAcyclic(candidateBoard)) {
                        placed[nextId] = candidate
                        occupied.addAll(body)
                        nextId++
                        done = true
                        break
                    }
                }
                if (done) break
            }
        }

        val board = Board(rows, cols, placed)
        return if (PuzzleSolver.isSolvable(board)) board else generateTrivialFallback(config)
    }

    /** The head first, then [len] - 1 cells behind it, opposite to the direction of travel. */
    internal fun bodyFor(head: GridPos, dir: Direction, len: Int): List<GridPos> =
        List(len.coerceAtLeast(1)) { head.moved(dir.opposite, it) }

    /** True when the obstruction graph of [board] has no directed cycle. */
    internal fun isGraphAcyclic(board: Board): Boolean {
        val arrows = board.activeArrows
        val adj = HashMap<Int, MutableSet<Int>>(arrows.size)
        for (arrow in arrows) adj[arrow.id] = LinkedHashSet()

        val maxSteps = maxOf(board.rows, board.cols)
        for (arrow in arrows) {
            val deps = adj.getValue(arrow.id)
            for (segment in arrow.body) {
                for (step in 1..maxSteps) {
                    val next = segment.moved(arrow.direction, step)
                    if (!board.isInside(next)) break
                    if (arrow.occupies(next)) continue
                    val blocker = board.getArrowAt(next) ?: continue
                    if (blocker.id != arrow.id) deps.add(blocker.id)
                }
            }
        }

        // DFS colouring: 0 unvisited, 1 on the stack, 2 finished.
        val state = HashMap<Int, Int>(arrows.size)
        fun hasCycle(u: Int): Boolean {
            state[u] = 1
            for (v in adj[u].orEmpty()) {
                when (state[v] ?: 0) {
                    1 -> return true
                    0 -> if (hasCycle(v)) return true
                }
            }
            state[u] = 2
            return false
        }
        for (arrow in arrows) {
            if ((state[arrow.id] ?: 0) == 0 && hasCycle(arrow.id)) return false
        }
        return true
    }

    /** Perimeter-facing single cells: trivially solvable, used only if the random pass failed. */
    private fun generateTrivialFallback(config: LevelConfig): Board {
        val arrows = LinkedHashMap<Int, Arrow>()
        var id = 1
        for (r in 0 until config.rows) {
            for (c in 0 until config.cols) {
                if (id > config.arrowCount) break
                val dir = when {
                    r == 0 -> Direction.UP
                    r == config.rows - 1 -> Direction.DOWN
                    c == 0 -> Direction.LEFT
                    else -> Direction.RIGHT
                }
                arrows[id] = Arrow(id, GridPos(r, c), dir, listOf(GridPos(r, c)), (r + c) % config.colorCount)
                id++
            }
        }
        return Board(config.rows, config.cols, arrows)
    }
}

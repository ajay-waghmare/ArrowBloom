package com.aweagle.arrowbloom.engine

import kotlin.random.Random

data class LevelConfig(
    val levelNumber: Int,
    val rows: Int,
    val cols: Int,
    val arrowCount: Int,
    val colorCount: Int = 4
)

/**
 * Procedural level generator producing 100% verified solvable arrow puzzles.
 *
 * Employs a Directed Acyclic Graph (DAG) obstruction invariant:
 * An arrow `u` depends on arrow `v` (edge `u -> v`) if `v` lies along `u`'s escape raycast.
 * The puzzle is guaranteed to be solvable if and only if this dependency graph contains
 * no directed cycles.
 */
object PuzzleGenerator {

    /**
     * Preset level configuration curves matching casual puzzle progression.
     */
    fun getConfigForLevel(level: Int): LevelConfig {
        return when {
            level <= 3 -> LevelConfig(level, rows = 4, cols = 4, arrowCount = 5 + level, colorCount = 3)
            level <= 10 -> LevelConfig(level, rows = 4, cols = 4, arrowCount = 8 + (level - 3), colorCount = 4)
            level <= 25 -> LevelConfig(level, rows = 5, cols = 5, arrowCount = 14 + (level - 10) / 2, colorCount = 4)
            level <= 50 -> LevelConfig(level, rows = 6, cols = 6, arrowCount = 20 + (level - 25) / 2, colorCount = 5)
            level <= 100 -> LevelConfig(level, rows = 7, cols = 7, arrowCount = 28 + (level - 50) / 3, colorCount = 6)
            else -> LevelConfig(level, rows = 8, cols = 8, arrowCount = 38 + (level % 10), colorCount = 6)
        }
    }

    /**
     * Generates a guaranteed solvable puzzle for the specified level.
     */
    fun generateLevel(level: Int): Board {
        val config = getConfigForLevel(level)
        val random = Random(seed = level * 31337L + 20261019L)
        return generate(config, random)
    }

    /**
     * Generates a deterministic daily challenge from a date seed (e.g. 20261019).
     */
    fun generateDailyChallenge(dateSeed: Long): Board {
        val random = Random(seed = dateSeed)
        val config = LevelConfig(
            levelNumber = 9999,
            rows = 6,
            cols = 6,
            arrowCount = 24,
            colorCount = 5
        )
        return generate(config, random)
    }

    /**
     * Core procedural generator using incremental DAG cycle avoidance.
     */
    fun generate(config: LevelConfig, random: Random = Random.Default): Board {
        val rows = config.rows
        val cols = config.cols
        val targetCount = minOf(config.arrowCount, rows * cols)

        // Shuffle all available grid coordinates
        val allPositions = mutableListOf<GridPos>()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                allPositions.add(GridPos(r, c))
            }
        }
        allPositions.shuffle(random)

        val placedArrows = mutableMapOf<Int, Arrow>()
        var nextId = 1

        for (pos in allPositions) {
            if (placedArrows.size >= targetCount) break

            // Try candidate directions in random order
            val directions = Direction.entries.shuffled(random)
            var placed = false

            for (dir in directions) {
                val candidate = Arrow(
                    id = nextId,
                    head = pos,
                    direction = dir,
                    body = listOf(pos),
                    colorIndex = random.nextInt(config.colorCount)
                )

                // Test if adding candidate keeps the dependency graph acyclic
                val candidateBoard = Board(rows, cols, placedArrows + (nextId to candidate))
                if (isGraphAcyclic(candidateBoard)) {
                    placedArrows[nextId] = candidate
                    nextId++
                    placed = true
                    break
                }
            }

            if (!placed && placedArrows.size + (allPositions.size - allPositions.indexOf(pos)) < targetCount) {
                // If density is too low, continue trying remaining positions
                continue
            }
        }

        val board = Board(rows, cols, placedArrows)

        // Double check with solver
        val solverResult = PuzzleSolver.solve(board)
        if (solverResult is SolverResult.Solvable) {
            return board
        }

        // Fallback: If edge case created deadlock, construct a pure peripheral board
        return generateTrivialFallback(config)
    }

    /**
     * Checks if the arrow obstruction graph contains any directed cycles.
     */
    private fun isGraphAcyclic(board: Board): Boolean {
        // Build adjacency list: arrow -> list of arrows directly blocking it
        val adj = mutableMapOf<Int, MutableList<Int>>()
        val inDegree = mutableMapOf<Int, Int>()

        for (arrow in board.activeArrows) {
            adj[arrow.id] = mutableListOf()
            inDegree[arrow.id] = 0
        }

        for (arrow in board.activeArrows) {
            val dir = arrow.direction
            val maxSteps = maxOf(board.rows, board.cols)

            for (step in 1..maxSteps) {
                val nextPos = arrow.head.moved(dir, step)
                if (!board.isInside(nextPos)) break

                val blocker = board.getArrowAt(nextPos)
                if (blocker != null && blocker.id != arrow.id) {
                    // arrow depends on blocker (blocker must be removed first)
                    // Edge: arrow -> blocker
                    adj[arrow.id]?.add(blocker.id)
                }
            }
        }

        // Cycle check using DFS coloring: 0 = unvisited, 1 = visiting (grey), 2 = visited (black)
        val state = mutableMapOf<Int, Int>()

        fun hasCycle(u: Int): Boolean {
            state[u] = 1
            val neighbors = adj[u] ?: emptyList()
            for (v in neighbors) {
                val neighborState = state[v] ?: 0
                if (neighborState == 1) return true // Back-edge detected: cycle!
                if (neighborState == 0 && hasCycle(v)) return true
            }
            state[u] = 2
            return false
        }

        for (arrow in board.activeArrows) {
            if ((state[arrow.id] ?: 0) == 0) {
                if (hasCycle(arrow.id)) return false
            }
        }

        return true
    }

    private fun generateTrivialFallback(config: LevelConfig): Board {
        val arrows = mutableMapOf<Int, Arrow>()
        var id = 1
        for (r in 0 until config.rows) {
            for (c in 0 until config.cols) {
                if (id <= config.arrowCount) {
                    // Outward-facing perimeter directions
                    val dir = when {
                        r == 0 -> Direction.UP
                        r == config.rows - 1 -> Direction.DOWN
                        c == 0 -> Direction.LEFT
                        else -> Direction.RIGHT
                    }
                    arrows[id] = Arrow(
                        id = id,
                        head = GridPos(r, c),
                        direction = dir,
                        body = listOf(GridPos(r, c)),
                        colorIndex = (r + c) % config.colorCount
                    )
                    id++
                }
            }
        }
        return Board(config.rows, config.cols, arrows)
    }
}

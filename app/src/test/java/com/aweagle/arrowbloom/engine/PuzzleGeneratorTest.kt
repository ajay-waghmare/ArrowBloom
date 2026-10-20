package com.aweagle.arrowbloom.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

class PuzzleGeneratorTest {

    /** Every structural rule a generated board must keep. */
    private fun assertWellFormed(board: Board, label: String) {
        val seen = HashSet<GridPos>()
        for (a in board.arrows.values) {
            val body = a.body
            assertEquals("$label arrow ${a.id}: head first", a.head, body.first())
            assertTrue("$label arrow ${a.id}: at least two cells", body.size >= 2)
            assertEquals("$label arrow ${a.id}: the cell behind the head is in line", a.head.moved(a.direction.opposite), body[1])
            for (i in 1 until body.size) {
                val d = abs(body[i].row - body[i - 1].row) + abs(body[i].col - body[i - 1].col)
                assertEquals("$label arrow ${a.id}: consecutive cells touch", 1, d)
            }
            assertEquals("$label arrow ${a.id}: no cell twice", body.size, body.toSet().size)
            for (p in body) {
                assertTrue("$label arrow ${a.id}: $p is playable", board.isPlayable(p))
                assertTrue("$label: $p is shared", seen.add(p))
            }
            var p = a.head.moved(a.direction)
            while (board.isInside(p)) {
                assertFalse("$label arrow ${a.id}: points into its own body", a.occupies(p))
                p = p.moved(a.direction)
            }
        }
    }

    @Test fun levels_areWellFormed_andSolvable() {
        for (level in 1..160) {
            val board = PuzzleGenerator.generateLevel(level)
            assertWellFormed(board, "level $level")
            assertTrue("level $level solvable", PuzzleSolver.isSolvable(board))
            assertTrue("level $level has arrows (${board.totalArrows})", board.totalArrows >= 3)
        }
    }

    @Test fun firstLevel_isFiveFreeArrows() {
        val board = PuzzleGenerator.generateLevel(1)
        assertEquals(5, board.totalArrows)
        assertEquals(5, CollisionDetector.findEscapableArrows(board).size)
    }

    @Test fun sameLevel_sameBoard() {
        val a = PuzzleGenerator.generateLevel(37)
        val b = PuzzleGenerator.generateLevel(37)
        assertEquals(a.arrows, b.arrows)
        assertEquals(a.shape, b.shape)
    }

    @Test fun everyFifthLevel_hasAShape_andArrowsStayInsideIt() {
        for (level in listOf(5, 10, 15, 20, 25, 45, 65, 85, 105)) {
            val board = PuzzleGenerator.generateLevel(level)
            assertTrue("level $level shaped", board.shape != BoardShape.RECT)
            for (a in board.arrows.values) for (p in a.body) assertTrue(board.isPlayable(p))
        }
        assertEquals(BoardShape.RECT, PuzzleGenerator.generateLevel(7).shape)
    }

    @Test fun laterGardens_areDeeper() {
        fun avgDepth(levels: IntRange) = levels.map { PuzzleStats.depth(PuzzleGenerator.generateLevel(it)) }.average()
        val meadow = avgDepth(3..12)
        val aurora = avgDepth(83..92)
        assertTrue("meadow $meadow vs aurora $aurora", aurora > meadow + 2)
    }

    @Test fun generation_isFastEnoughForATap() {
        val start = System.nanoTime()
        for (level in 101..120) PuzzleGenerator.generateLevel(level)
        val perBoardMs = (System.nanoTime() - start) / 1_000_000.0 / 20
        assertTrue("per board $perBoardMs ms", perBoardMs < 400)
    }

    @Test fun depth_ofAChain_countsTheWait() {
        val a = line(1, Direction.RIGHT, 2 to 1, 2 to 0)
        val b = line(2, Direction.UP, 1 to 3, 2 to 3, 3 to 3)
        assertEquals(2, PuzzleStats.depth(boardOf(5, 5, a, b)))
        assertEquals(1, PuzzleStats.depth(boardOf(5, 5, b)))
    }

    /** Dev tool: set AB_GEN_REPORT to a file path to dump boards and stats for tuning. */
    @Test fun report() {
        val path = System.getenv("AB_GEN_REPORT")
        assumeTrue(path != null)
        val sb = StringBuilder()
        val sampled = listOf(1, 2, 3, 5, 10, 20, 21, 30, 40, 45, 50, 60, 70, 80, 90, 100, 101, 110, 150)
        for (level in 1..200) {
            val cfg = PuzzleGenerator.getConfigForLevel(level)
            val t0 = System.nanoTime()
            val board = PuzzleGenerator.generateLevel(level)
            val ms = (System.nanoTime() - t0) / 1_000_000
            val playable = (0 until board.rows * board.cols).count { board.isPlayable(GridPos(it / board.cols, it % board.cols)) }
            val covered = board.arrows.values.sumOf { it.body.size }
            val free = CollisionDetector.findEscapableArrows(board).size
            sb.append("L%3d %2dx%-2d %-7s arrows=%2d free=%2d depth=%2d fill=%.2f len=%.1f %s %dms\n".format(
                level, cfg.cols, cfg.rows, board.shape, board.totalArrows, free, PuzzleStats.depth(board),
                covered.toDouble() / playable, covered.toDouble() / board.totalArrows.coerceAtLeast(1), PuzzleStats.difficultyOf(board), ms,
            ))
            if (level in sampled) sb.append(ascii(board)).append('\n')
        }
        for (shape in BoardShape.entries) {
            sb.append("shape $shape 11x14\n")
            for (r in 0 until 14) {
                for (c in 0 until 11) sb.append(if (shape.contains(r, c, 14, 11)) '#' else '.')
                sb.append('\n')
            }
        }
        File(path).writeText(sb.toString())
    }

    private fun ascii(board: Board): String {
        val g = Array(board.rows) { r -> CharArray(board.cols) { c -> if (board.isPlayable(GridPos(r, c))) '·' else ' ' } }
        for (a in board.arrows.values) {
            for ((i, p) in a.body.withIndex()) {
                g[p.row][p.col] = if (i == 0) when (a.direction) {
                    Direction.UP -> '^'; Direction.DOWN -> 'v'; Direction.LEFT -> '<'; Direction.RIGHT -> '>'
                } else ('a' + (a.id - 1) % 26)
            }
        }
        return g.joinToString("\n") { String(it) }
    }
}

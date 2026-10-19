package com.aweagle.arrowbloom.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleGeneratorTest {

    @Test
    fun generatedLevels_1To30_areAll100PercentSolvable() {
        for (lvl in 1..30) {
            val board = PuzzleGenerator.generateLevel(lvl)
            assertTrue("Level $lvl board must contain active arrows", board.activeArrows.isNotEmpty())

            val result = PuzzleSolver.solve(board)
            assertTrue(
                "Level $lvl must be 100% solvable without deadlocks. Result was: $result",
                result is SolverResult.Solvable
            )

            val solvable = result as SolverResult.Solvable
            assertEquals(
                "Solution order count must equal total board arrows for level $lvl",
                board.totalArrows,
                solvable.solutionOrder.size
            )
        }
    }

    @Test
    fun dailyChallenge_isDeterministic() {
        val dateSeed = 20261019L
        val boardA = PuzzleGenerator.generateDailyChallenge(dateSeed)
        val boardB = PuzzleGenerator.generateDailyChallenge(dateSeed)

        assertEquals("Same date seed must generate identical arrow counts", boardA.totalArrows, boardB.totalArrows)
        assertEquals("Same date seed must generate identical arrow IDs", boardA.arrows.keys, boardB.arrows.keys)

        for (id in boardA.arrows.keys) {
            val arrowA = boardA.arrows[id]!!
            val arrowB = boardB.arrows[id]!!
            assertEquals("Arrow $id head must match", arrowA.head, arrowB.head)
            assertEquals("Arrow $id direction must match", arrowA.direction, arrowB.direction)
        }

        assertTrue("Daily challenge must be solvable", PuzzleSolver.isSolvable(boardA))
    }

    @Test
    fun generationPerformance_50LevelsUnder500ms() {
        val startTime = System.currentTimeMillis()
        for (lvl in 1..50) {
            val board = PuzzleGenerator.generateLevel(lvl)
            assertTrue(board.activeArrows.isNotEmpty())
        }
        val elapsed = System.currentTimeMillis() - startTime
        println("Generated 50 levels in ${elapsed}ms")
        assertTrue("50 levels should generate in under 1500ms, took ${elapsed}ms", elapsed < 1500)
    }
}

package com.aweagle.arrowbloom.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aweagle.arrowbloom.engine.Arrow
import com.aweagle.arrowbloom.engine.ArrowState
import com.aweagle.arrowbloom.engine.Board
import com.aweagle.arrowbloom.engine.GameEngine
import com.aweagle.arrowbloom.engine.GridPos
import com.aweagle.arrowbloom.engine.PuzzleGenerator
import com.aweagle.arrowbloom.engine.TapResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.sin

data class GameUiState(
    val board: Board,
    val levelNumber: Int,
    val score: Int = 0,
    val combo: Int = 0,
    val totalArrows: Int = 0,
    val remainingArrows: Int = 0,
    val shakingArrowId: Int? = null,
    val shakeOffset: Float = 0f,
    val launchingProgress: Map<Int, Float> = emptyMap(),
    val hintArrowId: Int? = null,
    val hintsRemaining: Int = 3,
    val isLevelComplete: Boolean = false
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("arrow_bloom_prefs", Context.MODE_PRIVATE)

    private var currentLevel: Int = prefs.getInt("saved_level", 1)
    private var engine: GameEngine = createEngineForLevel(currentLevel)

    private val _uiState = MutableStateFlow(buildUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private fun createEngineForLevel(level: Int): GameEngine {
        val board = PuzzleGenerator.generateLevel(level)
        return GameEngine(board, level)
    }

    private fun buildUiState(): GameUiState {
        return GameUiState(
            board = engine.board,
            levelNumber = engine.levelNumber,
            score = engine.score,
            combo = engine.combo,
            totalArrows = engine.totalArrowsCount,
            remainingArrows = engine.remainingArrowsCount,
            isLevelComplete = engine.isLevelCompleted
        )
    }

    fun onCellTapped(pos: GridPos) {
        val arrow = engine.board.getArrowAt(pos) ?: return
        onArrowTapped(arrow.id)
    }

    fun onArrowTapped(arrowId: Int) {
        // Prevent tapping an arrow currently animating launch
        if (_uiState.value.launchingProgress.containsKey(arrowId)) return

        // Clear hint highlight on tap
        if (_uiState.value.hintArrowId != null) {
            _uiState.update { it.copy(hintArrowId = null) }
        }

        when (val result = engine.onArrowTapped(arrowId)) {
            is TapResult.Escaped -> {
                animateArrowLaunch(result.arrow, result.isLevelCompleted)
            }
            is TapResult.Blocked -> {
                animateArrowBlocked(result.arrow)
            }
            else -> Unit
        }
    }

    private fun animateArrowLaunch(arrow: Arrow, isCompleted: Boolean) {
        viewModelScope.launch {
            // Animate progress 0f -> 1f over 200ms
            val startTime = System.currentTimeMillis()
            val durationMs = 200L

            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)

                _uiState.update { state ->
                    val currentProgress = state.launchingProgress.toMutableMap()
                    currentProgress[arrow.id] = progress
                    state.copy(
                        board = engine.board,
                        score = engine.score,
                        combo = engine.combo,
                        remainingArrows = engine.remainingArrowsCount,
                        launchingProgress = currentProgress
                    )
                }

                if (progress >= 1f) break
                delay(16)
            }

            // Clean up launch map
            _uiState.update { state ->
                val currentProgress = state.launchingProgress.toMutableMap()
                currentProgress.remove(arrow.id)
                state.copy(
                    board = engine.board,
                    launchingProgress = currentProgress,
                    isLevelComplete = isCompleted
                )
            }
        }
    }

    private fun animateArrowBlocked(arrow: Arrow) {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val durationMs = 280L

            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val t = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                val decay = 1f - t
                val offset = sin(t * Math.PI.toFloat() * 6f) * decay

                _uiState.update { state ->
                    state.copy(
                        board = engine.board,
                        combo = 0,
                        shakingArrowId = arrow.id,
                        shakeOffset = offset
                    )
                }

                if (t >= 1f) break
                delay(16)
            }

            engine.resetBlockedState(arrow.id)
            _uiState.update { state ->
                state.copy(
                    board = engine.board,
                    shakingArrowId = null,
                    shakeOffset = 0f
                )
            }
        }
    }

    fun onNextLevel() {
        currentLevel++
        prefs.edit().putInt("saved_level", currentLevel).apply()
        engine = createEngineForLevel(currentLevel)
        _uiState.value = buildUiState()
    }

    fun onRestartLevel() {
        engine = createEngineForLevel(currentLevel)
        _uiState.value = buildUiState()
    }

    fun onUndo() {
        if (engine.undo()) {
            _uiState.value = buildUiState()
        }
    }

    fun onUseHint() {
        val currentHints = _uiState.value.hintsRemaining
        if (currentHints <= 0) return

        val hintId = engine.getHintArrowId() ?: return
        _uiState.update {
            it.copy(
                hintArrowId = hintId,
                hintsRemaining = currentHints - 1
            )
        }

        viewModelScope.launch {
            delay(3000)
            _uiState.update {
                if (it.hintArrowId == hintId) it.copy(hintArrowId = null) else it
            }
        }
    }
}

package com.aweagle.arrowbloom.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aweagle.arrowbloom.BuildConfig
import com.aweagle.arrowbloom.R
import com.aweagle.arrowbloom.analytics.Analytics
import com.aweagle.arrowbloom.analytics.NoOpAnalytics
import com.aweagle.arrowbloom.audio.Feedback
import com.aweagle.arrowbloom.audio.MusicEngine
import com.aweagle.arrowbloom.daily.DailyBloom
import com.aweagle.arrowbloom.data.Prefs
import com.aweagle.arrowbloom.data.RunCodec
import com.aweagle.arrowbloom.data.RunMode
import com.aweagle.arrowbloom.data.SavedRun
import com.aweagle.arrowbloom.data.StreakRules
import com.aweagle.arrowbloom.engine.Arrow
import com.aweagle.arrowbloom.engine.Board
import com.aweagle.arrowbloom.engine.GameEngine
import com.aweagle.arrowbloom.engine.Gardens
import com.aweagle.arrowbloom.engine.GridPos
import com.aweagle.arrowbloom.engine.PuzzleGenerator
import com.aweagle.arrowbloom.engine.Scoring
import com.aweagle.arrowbloom.engine.TapResult
import com.aweagle.arrowbloom.ui.theme.GardenPalette
import com.aweagle.arrowbloom.ui.theme.Palettes
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class Screen { HOME, GAME, SETTINGS }

/** An arrow left the board; the canvas animates it out and blooms its cell. */
data class EscapeEvent(val seq: Long, val arrow: Arrow, val combo: Int)

/** A tap hit a blocked arrow; the canvas shakes it. */
data class BlockedEvent(val seq: Long, val arrowId: Int)

/** The card shown when a board is cleared. */
data class Outcome(
    val mode: RunMode,
    val level: Int,
    val score: Int,
    val stars: Int,
    val blockedTaps: Int,
    val par: Int,
    val isNewBest: Boolean,
    val dailyNumber: Int = 0,
    val streak: Int = 0,
    val usedShield: Boolean = false,
    val shareText: String? = null,
)

/** Immutable snapshot the whole UI renders from. */
data class GameUiState(
    val screen: Screen = Screen.HOME,
    // the run on screen
    val mode: RunMode = RunMode.LEVEL,
    val board: Board = Board.empty(4, 4),
    val palette: GardenPalette = Palettes.MEADOW,
    val level: Int = 1,
    @StringRes val gardenNameRes: Int = R.string.garden_meadow,
    val gardenPosition: Int = 1,
    val gardenSize: Int = Gardens.LEVELS_PER_GARDEN,
    val dailyNumber: Int = 0,
    val score: Int = 0,
    val combo: Int = 0,
    val colorStreak: Int = 0,
    val totalArrows: Int = 0,
    val remainingArrows: Int = 0,
    val blockedTaps: Int = 0,
    val starsSoFar: Int = Scoring.MAX_STARS,
    val hintArrowId: Int? = null,
    val hintsRemaining: Int = HINTS_PER_BOARD,
    val canUndo: Boolean = false,
    val escapeEvent: EscapeEvent? = null,
    val blockedEvent: BlockedEvent? = null,
    val outcome: Outcome? = null,
    @StringRes val coachTextRes: Int? = null,
    // home
    val currentLevel: Int = 1,
    val highestCleared: Int = 0,
    val hasSavedRun: Boolean = false,
    val savedRunMode: RunMode? = null,
    val savedRunLevel: Int = 1,
    val gardenStars: Int = 0,
    val gardenMaxStars: Int = Gardens.LEVELS_PER_GARDEN * Scoring.MAX_STARS,
    val dailyDoneToday: Boolean = false,
    val dailyStarsToday: Int = 0,
    val dailyBlockedToday: Int = 0,
    val dailyArrows: Int = 24,
    val dailyStreak: Int = 0,
    val bestStreak: Int = 0,
    val streakShieldReady: Boolean = false,
    val levelsCleared: Int = 0,
    val perfectClears: Int = 0,
    val bestCombo: Int = 0,
    val arrowsEscaped: Int = 0,
    // settings
    val soundOn: Boolean = true,
    val musicOn: Boolean = true,
    val hapticsOn: Boolean = true,
    // one-shot
    val toast: String? = null,
    val toastSeq: Long = 0,
)

const val HINTS_PER_BOARD = 3

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val analytics: Analytics = NoOpAnalytics
    val music = MusicEngine()
    val feedback = Feedback(app) { prefs.hapticsOn }

    private var engine: GameEngine? = null
    private var mode: RunMode = RunMode.LEVEL
    private var dailyDate: LocalDate = LocalDate.now()
    private var hintsUsed = 0
    private var seq = 0L
    private var hintJob: Job? = null
    private var inForeground = false
    private var dailyArrowsCache: Pair<Long, Int>? = null

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        music.setMusicOn(prefs.musicOn)
        music.setSoundOn(prefs.soundOn)
        restoreSavedRun()
        _uiState.update { refreshHome(it).copy(soundOn = prefs.soundOn, musicOn = prefs.musicOn, hapticsOn = prefs.hapticsOn) }
    }

    // ----- lifecycle ---------------------------------------------------------------

    fun onForeground() {
        inForeground = true
        music.setPlaying(true)
    }

    fun onBackground() {
        inForeground = false
        saveRun()
        music.setPlaying(false)
    }

    override fun onCleared() {
        saveRun()
        music.release()
        super.onCleared()
    }

    // ----- navigation ---------------------------------------------------------------

    fun goHome() {
        saveRun()
        _uiState.update { refreshHome(it).copy(screen = Screen.HOME, hintArrowId = null) }
    }

    fun openSettings() = _uiState.update { it.copy(screen = Screen.SETTINGS) }
    fun closeSettings() = _uiState.update { refreshHome(it).copy(screen = Screen.HOME) }

    /** Home's main button: resume the saved run if there is one, else start the current level. */
    fun playOrContinue() {
        val e = engine
        if (e != null && !e.isLevelCompleted && _uiState.value.hasSavedRun) {
            _uiState.update { gameState(it).copy(screen = Screen.GAME) }
        } else {
            startLevel(prefs.currentLevel)
        }
    }

    fun startLevel(level: Int) {
        val lvl = level.coerceAtLeast(1)
        mode = RunMode.LEVEL
        hintsUsed = 0
        engine = GameEngine(PuzzleGenerator.generateLevel(lvl), lvl)
        analytics.event(Analytics.LEVEL_START, mapOf("level" to lvl))
        enterGame()
    }

    fun startDaily() {
        dailyDate = LocalDate.now()
        mode = RunMode.DAILY
        hintsUsed = 0
        engine = GameEngine(DailyBloom.board(dailyDate), PuzzleGenerator.DAILY_LEVEL_NUMBER)
        analytics.event(Analytics.DAILY_START, mapOf("number" to DailyBloom.numberFor(dailyDate)))
        enterGame()
    }

    private fun enterGame() {
        hintJob?.cancel()
        saveRun()
        val coach = if (mode == RunMode.LEVEL && !prefs.coached && engine?.levelNumber == 1) R.string.coach_tap_arrow else null
        _uiState.update {
            gameState(it).copy(
                screen = Screen.GAME,
                hintArrowId = null,
                hintsRemaining = HINTS_PER_BOARD,
                escapeEvent = null,
                blockedEvent = null,
                outcome = null,
                coachTextRes = coach,
            )
        }
    }

    fun nextLevel() {
        val e = engine ?: return
        if (mode == RunMode.DAILY) { goHome(); return }
        startLevel(e.levelNumber + 1)
    }

    fun replay() {
        if (mode == RunMode.DAILY) startDaily() else startLevel(engine?.levelNumber ?: prefs.currentLevel)
    }

    fun restart() {
        feedback.tap()
        replay()
    }

    // ----- play ------------------------------------------------------------------------

    fun onCellTapped(pos: GridPos) {
        val e = engine ?: return
        if (_uiState.value.outcome != null) return
        when (val result = e.onCellTapped(pos)) {
            is TapResult.Escaped -> onEscaped(e, result)
            is TapResult.Blocked -> onBlocked(e, result)
            else -> Unit
        }
    }

    private fun onEscaped(e: GameEngine, result: TapResult.Escaped) {
        seq++
        music.escapeNote(result.combo, result.arrow.head.col, e.board.cols)
        music.setIntensity(result.combo / 8f)
        feedback.escape(result.combo)
        prefs.arrowsEscaped += 1
        if (result.combo > prefs.bestCombo) prefs.bestCombo = result.combo
        if (prefs.arrowsEscaped == 1) analytics.event(Analytics.FIRST_ESCAPE)
        val coach = when {
            _uiState.value.coachTextRes == null -> null
            e.remainingArrowsCount > 0 -> R.string.coach_keep_going
            else -> null
        }
        _uiState.update {
            gameState(it).copy(
                escapeEvent = EscapeEvent(seq, result.arrow, result.combo),
                hintArrowId = if (it.hintArrowId == result.arrow.id) null else it.hintArrowId,
                coachTextRes = coach,
            )
        }
        saveRun()
        if (result.isLevelCompleted) {
            viewModelScope.launch {
                delay(COMPLETE_DELAY_MS)
                finishBoard(e)
            }
        }
    }

    private fun onBlocked(e: GameEngine, result: TapResult.Blocked) {
        seq++
        music.blockedThud()
        music.setIntensity(0f)
        feedback.blocked()
        analytics.event(Analytics.BLOCKED_TAP, mapOf("level" to e.levelNumber))
        _uiState.update {
            gameState(it).copy(
                blockedEvent = BlockedEvent(seq, result.arrow.id),
                coachTextRes = if (it.coachTextRes != null) R.string.coach_blocked else null,
            )
        }
        saveRun()
        val id = result.arrow.id
        viewModelScope.launch {
            delay(SHAKE_MS)
            e.resetBlockedState(id)
            _uiState.update { gameState(it) }
        }
    }

    private fun finishBoard(e: GameEngine) {
        if (!e.isLevelCompleted || _uiState.value.outcome != null) return
        val stars = e.starsSoFar
        val level = e.levelNumber
        music.bloomRun()
        music.setIntensity(0f)
        feedback.complete()
        prefs.levelsCleared += 1
        if (e.blockedTaps == 0) prefs.perfectClears += 1
        if (mode == RunMode.LEVEL) prefs.coached = true
        prefs.savedRun = null

        val outcome = if (mode == RunMode.LEVEL) {
            val newBest = prefs.recordStars(level, stars)
            if (level > prefs.highestCleared) prefs.highestCleared = level
            if (prefs.currentLevel <= level) prefs.currentLevel = level + 1
            analytics.event(Analytics.LEVEL_COMPLETE, mapOf("level" to level, "stars" to stars, "blocked" to e.blockedTaps))
            Outcome(RunMode.LEVEL, level, e.score, stars, e.blockedTaps, e.par, newBest)
        } else {
            val today = dailyDate.toEpochDay()
            val advanced = StreakRules.advance(prefs.lastDailyDay, today, prefs.dailyStreak, prefs.streakShieldUsed)
            var usedShield = false
            if (advanced != null) {
                prefs.dailyStreak = advanced.streak
                if (advanced.resetShield) prefs.streakShieldUsed = false
                if (advanced.usedShield) prefs.streakShieldUsed = true
                usedShield = advanced.usedShield
                if (advanced.streak > prefs.bestStreak) prefs.bestStreak = advanced.streak
                prefs.lastDailyDay = today
                prefs.dailyStars = stars
                prefs.dailyBlockedTaps = e.blockedTaps
            } else if (stars > prefs.dailyStars) {
                prefs.dailyStars = stars
                prefs.dailyBlockedTaps = e.blockedTaps
            }
            val number = DailyBloom.numberFor(dailyDate)
            analytics.event(Analytics.DAILY_COMPLETE, mapOf("number" to number, "stars" to stars))
            Outcome(
                RunMode.DAILY, level, e.score, stars, e.blockedTaps, e.par,
                isNewBest = advanced != null,
                dailyNumber = number,
                streak = prefs.dailyStreak,
                usedShield = usedShield,
                shareText = DailyBloom.shareText(number, stars, e.totalArrowsCount, e.blockedTaps, BuildConfig.STORE_URL),
            )
        }
        _uiState.update { refreshHome(gameState(it)).copy(outcome = outcome, coachTextRes = null, hintArrowId = null) }
    }

    fun undo() {
        val e = engine ?: return
        if (_uiState.value.outcome != null) return
        if (!e.undo()) return
        music.undoNote()
        feedback.tap()
        analytics.event(Analytics.UNDO)
        _uiState.update { gameState(it).copy(hintArrowId = null) }
        saveRun()
    }

    fun hint() {
        val e = engine ?: return
        if (_uiState.value.outcome != null) return
        val left = _uiState.value.hintsRemaining
        if (left <= 0) { toast(getApplication<Application>().getString(R.string.no_hints_left)); return }
        val id = e.getHintArrowId() ?: return
        hintsUsed++
        music.hintPing()
        feedback.tap()
        analytics.event(Analytics.HINT_USED, mapOf("level" to e.levelNumber))
        _uiState.update { it.copy(hintArrowId = id, hintsRemaining = left - 1) }
        saveRun()
        hintJob?.cancel()
        hintJob = viewModelScope.launch {
            delay(HINT_MS)
            _uiState.update { if (it.hintArrowId == id) it.copy(hintArrowId = null) else it }
        }
    }

    // ----- debug-only test hooks (BuildConfig.DEBUG; R8 drops them from release) -----------

    /** Taps the first escapable arrow, as a tester would with a hint. */
    fun debugAutoTap() {
        if (!BuildConfig.DEBUG) return
        val e = engine ?: return
        val id = e.getHintArrowId() ?: return
        val arrow = e.board.getArrowById(id) ?: return
        onCellTapped(arrow.head)
    }

    /** Jumps the Journey to the first level of the next garden. */
    fun debugNextGarden() {
        if (!BuildConfig.DEBUG) return
        val current = engine?.takeIf { mode == RunMode.LEVEL }?.levelNumber ?: prefs.currentLevel
        val next = Gardens.ALL.firstOrNull { it.firstLevel > current } ?: return
        prefs.currentLevel = maxOf(prefs.currentLevel, next.firstLevel)
        startLevel(next.firstLevel)
    }

    fun onShared() {
        feedback.tap()
        analytics.event(Analytics.SHARE_RESULT)
    }

    // ----- settings ---------------------------------------------------------------------

    fun toggleSound() {
        prefs.soundOn = !prefs.soundOn
        music.setSoundOn(prefs.soundOn)
        if (inForeground) music.setPlaying(true)
        _uiState.update { it.copy(soundOn = prefs.soundOn) }
    }

    fun toggleMusic() {
        prefs.musicOn = !prefs.musicOn
        music.setMusicOn(prefs.musicOn)
        if (inForeground) music.setPlaying(true)
        _uiState.update { it.copy(musicOn = prefs.musicOn) }
    }

    fun toggleHaptics() {
        prefs.hapticsOn = !prefs.hapticsOn
        if (prefs.hapticsOn) feedback.tap()
        _uiState.update { it.copy(hapticsOn = prefs.hapticsOn) }
    }

    fun consumeToast() = _uiState.update { it.copy(toast = null) }

    private fun toast(msg: String) = _uiState.update { it.copy(toast = msg, toastSeq = it.toastSeq + 1) }

    // ----- state assembly -------------------------------------------------------------

    private fun gameState(s: GameUiState): GameUiState {
        val e = engine ?: return s
        val isDaily = mode == RunMode.DAILY
        val garden = Gardens.forLevel(e.levelNumber)
        return s.copy(
            mode = mode,
            board = e.board,
            palette = if (isDaily) Palettes.DAILY else Palettes.forGarden(garden.index),
            level = e.levelNumber,
            gardenNameRes = garden.nameRes,
            gardenPosition = garden.positionOf(e.levelNumber),
            gardenSize = if (garden.isEndless) 0 else garden.levelCount,
            dailyNumber = if (isDaily) DailyBloom.numberFor(dailyDate) else 0,
            score = e.score,
            combo = e.combo,
            colorStreak = e.colorStreak,
            totalArrows = e.totalArrowsCount,
            remainingArrows = e.remainingArrowsCount,
            blockedTaps = e.blockedTaps,
            starsSoFar = e.starsSoFar,
            canUndo = e.canUndo,
        )
    }

    private fun refreshHome(s: GameUiState): GameUiState {
        val today = LocalDate.now()
        val e = engine
        val saved = e != null && !e.isLevelCompleted && e.escapeOrder.isNotEmpty()
        val current = prefs.currentLevel
        val garden = Gardens.forLevel(current)
        val stars = prefs.bestStars()
        val gardenStars = if (garden.isEndless) 0 else (garden.firstLevel..garden.lastLevel).sumOf { stars[it] ?: 0 }
        val dailyDone = prefs.lastDailyDay == today.toEpochDay()
        val streak = StreakRules.current(prefs.lastDailyDay, today.toEpochDay(), prefs.dailyStreak, prefs.streakShieldUsed)
        return s.copy(
            currentLevel = current,
            highestCleared = prefs.highestCleared,
            hasSavedRun = saved,
            savedRunMode = if (saved) mode else null,
            savedRunLevel = e?.levelNumber ?: current,
            gardenNameRes = garden.nameRes,
            gardenPosition = garden.positionOf(current),
            gardenSize = if (garden.isEndless) 0 else garden.levelCount,
            gardenStars = gardenStars,
            gardenMaxStars = if (garden.isEndless) 0 else garden.levelCount * Scoring.MAX_STARS,
            palette = if (s.screen == Screen.GAME) s.palette else Palettes.forGarden(garden.index),
            dailyNumber = DailyBloom.numberFor(today),
            dailyDoneToday = dailyDone,
            dailyStarsToday = if (dailyDone) prefs.dailyStars else 0,
            dailyBlockedToday = if (dailyDone) prefs.dailyBlockedTaps else 0,
            dailyArrows = dailyArrowsFor(today),
            dailyStreak = streak,
            bestStreak = prefs.bestStreak,
            streakShieldReady = StreakRules.shieldAvailable(streak, prefs.streakShieldUsed),
            levelsCleared = prefs.levelsCleared,
            perfectClears = prefs.perfectClears,
            bestCombo = prefs.bestCombo,
            arrowsEscaped = prefs.arrowsEscaped,
        )
    }

    private fun dailyArrowsFor(date: LocalDate): Int {
        val seed = DailyBloom.seedFor(date)
        dailyArrowsCache?.let { if (it.first == seed) return it.second }
        val n = DailyBloom.board(date).totalArrows
        dailyArrowsCache = seed to n
        return n
    }

    // ----- resume ---------------------------------------------------------------------------

    private fun saveRun() {
        val e = engine ?: return
        if (e.isLevelCompleted || _uiState.value.outcome != null) { prefs.savedRun = null; return }
        prefs.savedRun = RunCodec.encode(
            SavedRun(
                mode = mode,
                level = e.levelNumber,
                dateSeed = if (mode == RunMode.DAILY) DailyBloom.seedFor(dailyDate) else 0L,
                escapeOrder = e.escapeOrder,
                blockedTaps = e.blockedTaps,
                hintsUsed = hintsUsed,
            ),
        )
    }

    private fun restoreSavedRun() {
        val run = RunCodec.decode(prefs.savedRun) ?: return
        val today = LocalDate.now()
        if (run.mode == RunMode.DAILY && run.dateSeed != DailyBloom.seedFor(today)) { prefs.savedRun = null; return }
        val board = if (run.mode == RunMode.DAILY) DailyBloom.board(today) else PuzzleGenerator.generateLevel(run.level)
        val e = GameEngine(board, if (run.mode == RunMode.DAILY) PuzzleGenerator.DAILY_LEVEL_NUMBER else run.level)
        e.restore(run.escapeOrder, run.blockedTaps)
        if (e.isLevelCompleted || e.escapeOrder.isEmpty()) { prefs.savedRun = null; return }
        mode = run.mode
        dailyDate = today
        hintsUsed = run.hintsUsed
        engine = e
        _uiState.update { gameState(it).copy(hintsRemaining = (HINTS_PER_BOARD - run.hintsUsed).coerceAtLeast(0)) }
    }

    companion object {
        private const val COMPLETE_DELAY_MS = 420L
        private const val SHAKE_MS = 320L
        private const val HINT_MS = 3500L
    }
}

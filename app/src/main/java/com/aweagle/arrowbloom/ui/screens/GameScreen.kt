package com.aweagle.arrowbloom.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aweagle.arrowbloom.BuildConfig
import com.aweagle.arrowbloom.R
import com.aweagle.arrowbloom.data.RunMode
import com.aweagle.arrowbloom.engine.Difficulty
import com.aweagle.arrowbloom.engine.Scoring
import com.aweagle.arrowbloom.ui.GameUiState
import com.aweagle.arrowbloom.ui.GameViewModel
import com.aweagle.arrowbloom.ui.IconButton
import com.aweagle.arrowbloom.ui.components.ArrowCanvas
import com.aweagle.arrowbloom.ui.pressable
import com.aweagle.arrowbloom.ui.theme.HeartEmpty
import com.aweagle.arrowbloom.ui.theme.HeartRed
import com.aweagle.arrowbloom.ui.theme.Ink
import com.aweagle.arrowbloom.ui.theme.OnAccent
import com.aweagle.arrowbloom.ui.theme.SurfaceBorder
import com.aweagle.arrowbloom.ui.theme.SurfaceCard
import com.aweagle.arrowbloom.ui.theme.TextMuted
import com.aweagle.arrowbloom.ui.theme.TextPrimary
import com.aweagle.arrowbloom.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun GameScreen(state: GameUiState, vm: GameViewModel) {
    val res = LocalContext.current.resources
    val accent = state.palette.accent
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar: back, level and garden, settings
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.feedback.tap(); vm.goHome() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = stringResource(R.string.home), tint = accent)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    val title = if (state.mode == RunMode.DAILY) stringResource(R.string.daily_number, state.dailyNumber)
                    else stringResource(R.string.level_n, state.level)
                    val subtitle = when {
                        state.mode == RunMode.DAILY -> stringResource(R.string.daily_same_for_everyone_short)
                        state.gardenSize > 0 -> stringResource(R.string.level_in_garden, stringResource(state.gardenNameRes), state.gardenPosition, state.gardenSize)
                        else -> stringResource(state.gardenNameRes)
                    }
                    Text(title, color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.debugLongPress { vm.debugNextGarden() })
                    Text(subtitle, color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.debugLongPress { vm.debugSkipFive() })
                }
                IconButton(onClick = { vm.feedback.tap(); vm.openSettings() }) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings), tint = accent)
                }
            }

            Spacer(Modifier.height(6.dp))

            // Status: arrows left, hearts, difficulty
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val arrowsDesc = res.getQuantityString(R.plurals.arrows_left, state.remainingArrows, state.remainingArrows)
                Chip(Modifier.clearAndSetSemantics { contentDescription = arrowsDesc }.debugLongPress { vm.debugAutoTap() }) {
                    Icon(Icons.Filled.NorthEast, contentDescription = null, tint = Ink, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(state.remainingArrows.toString(), color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                HeartsRow(state.heartsLeft, Modifier.debugLongPress { vm.debugWrongTap() })
                Chip {
                    Text(stringResource(difficultyRes(state.difficulty)), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Coaching for the first board
            AnimatedVisibility(visible = state.coachTextRes != null, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    stringResource(state.coachTextRes ?: R.string.coach_tap_arrow),
                    color = accent,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                )
            }

            // Board
            key(state.boardSeq) {
                ArrowCanvas(
                    board = state.board,
                    palette = state.palette,
                    escapeEvent = state.escapeEvent,
                    blockedEvent = state.blockedEvent,
                    hintArrowId = state.hintArrowId,
                    showGrid = state.showGrid,
                    contentDescription = stringResource(R.string.a11y_board, state.board.rows, state.board.cols, state.remainingArrows),
                    onArrowTapped = vm::onArrowTapped,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp),
                )
            }

            // Controls
            Row(
                Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(26.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundAction(
                    icon = Icons.Filled.Lightbulb,
                    label = stringResource(R.string.hint),
                    accent = accent,
                    badge = state.hintsRemaining,
                    active = state.hintArrowId != null,
                    enabled = state.hintsRemaining > 0,
                ) { vm.hint() }
                RoundAction(Icons.Filled.Tag, stringResource(R.string.grid), accent, active = state.showGrid) { vm.toggleGrid() }
                RoundAction(Icons.Filled.Refresh, stringResource(R.string.restart), accent) { vm.restart() }
            }
        }

        if (state.failed && state.outcome == null) FailOverlay(state, vm)
        state.outcome?.let { OutcomeOverlay(it, state, vm) }
        ToastHost(state, vm)
    }
}

/** Debug builds only: long-press shortcuts used by the emulator walkthrough. */
private fun Modifier.debugLongPress(action: () -> Unit): Modifier =
    if (BuildConfig.DEBUG) pointerInput(Unit) { detectTapGestures(onLongPress = { action() }) } else this

private fun difficultyRes(d: Difficulty): Int = when (d) {
    Difficulty.EASY -> R.string.difficulty_easy
    Difficulty.NORMAL -> R.string.difficulty_normal
    Difficulty.HARD -> R.string.difficulty_hard
    Difficulty.EXPERT -> R.string.difficulty_expert
}

@Composable
private fun Chip(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard,
        modifier = modifier.border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { content() }
    }
}

/** Three hearts; a lost one empties with a little bounce. */
@Composable
fun HeartsRow(hearts: Int, modifier: Modifier = Modifier, size: Int = 28) {
    val desc = stringResource(R.string.a11y_hearts, hearts)
    Row(modifier.clearAndSetSemantics { contentDescription = desc }, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(Scoring.MAX_HEARTS) { i ->
            val alive = i < hearts
            val scale by animateFloatAsState(
                targetValue = if (alive) 1f else 0.82f,
                animationSpec = spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMedium),
                label = "heart$i",
            )
            Icon(
                if (alive) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = if (alive) HeartRed else HeartEmpty,
                modifier = Modifier.size(size.dp).graphicsLayer { scaleX = scale; scaleY = scale },
            )
        }
    }
}

@Composable
private fun RoundAction(
    icon: ImageVector,
    label: String,
    accent: Color,
    badge: Int? = null,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .pressable(onClick = onClick)
            .padding(6.dp),
    ) {
        Box(Modifier.size(62.dp)) {
            Box(
                Modifier
                    .padding(3.dp)
                    .fillMaxSize()
                    .shadow(if (active) 10.dp else 5.dp, CircleShape)
                    .background(if (active) accent else SurfaceCard, CircleShape)
                    .border(1.dp, if (active) accent else SurfaceBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = if (active) OnAccent else if (enabled) accent else TextMuted, modifier = Modifier.size(26.dp))
            }
            if (badge != null) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(24.dp)
                        .background(if (enabled) accent else TextMuted, CircleShape)
                        .border(2.dp, SurfaceCard, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(badge.toString(), color = OnAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** A quiet two-second message at the bottom of the screen. */
@Composable
fun ToastHost(state: GameUiState, vm: GameViewModel) {
    val text = state.toast
    LaunchedEffect(state.toastSeq) {
        if (state.toast != null) { delay(2200); vm.consumeToast() }
    }
    Box(Modifier.fillMaxSize().navigationBarsPadding(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible = text != null, enter = fadeIn(), exit = fadeOut()) {
            Surface(shape = RoundedCornerShape(14.dp), color = Ink, modifier = Modifier.padding(bottom = 110.dp)) {
                Text(text ?: "", color = OnAccent, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
            }
        }
    }
}

package com.aweagle.arrowbloom.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aweagle.arrowbloom.R
import com.aweagle.arrowbloom.data.RunMode
import com.aweagle.arrowbloom.ui.GameUiState
import com.aweagle.arrowbloom.ui.GameViewModel
import com.aweagle.arrowbloom.ui.IconButton
import com.aweagle.arrowbloom.ui.components.ArrowCanvas
import com.aweagle.arrowbloom.ui.components.StarsRow
import com.aweagle.arrowbloom.ui.pressable
import com.aweagle.arrowbloom.ui.theme.AccentCombo
import com.aweagle.arrowbloom.ui.theme.SurfaceBorder
import com.aweagle.arrowbloom.ui.theme.SurfaceCard
import com.aweagle.arrowbloom.ui.theme.TextMuted
import com.aweagle.arrowbloom.ui.theme.TextPrimary
import com.aweagle.arrowbloom.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun GameScreen(state: GameUiState, vm: GameViewModel) {
    val res = LocalContext.current.resources
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header: home, title + score, stars so far
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.feedback.tap(); vm.goHome() }) {
                    Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.home), tint = TextSecondary)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    val title = if (state.mode == RunMode.DAILY) stringResource(R.string.daily_number, state.dailyNumber)
                    else if (state.gardenSize > 0) stringResource(R.string.level_in_garden, stringResource(state.gardenNameRes), state.gardenPosition, state.gardenSize)
                    else stringResource(R.string.level_in_endless, stringResource(state.gardenNameRes), state.level)
                    Text(title, color = state.palette.accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, maxLines = 1)
                    val scoreDesc = stringResource(R.string.a11y_score, state.score)
                    Text(state.score.toString(), color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black, modifier = Modifier.clearAndSetSemantics { contentDescription = scoreDesc })
                }
                StarsRow(stars = state.starsSoFar, size = 20.dp, modifier = Modifier.padding(end = 6.dp))
            }

            // Progress + remaining + combo
            val cleared = state.totalArrows - state.remainingArrows
            LinearProgressIndicator(
                progress = { if (state.totalArrows > 0) cleared.toFloat() / state.totalArrows else 0f },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                color = state.palette.accent,
                trackColor = SurfaceCard,
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    res.getQuantityString(R.plurals.arrows_left, state.remainingArrows, state.remainingArrows),
                    color = TextSecondary,
                    fontSize = 12.sp,
                )
                if (state.combo >= 2) {
                    Text(
                        stringResource(R.string.combo_badge, state.combo),
                        color = AccentCombo,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            // Coaching for the first board
            AnimatedVisibility(visible = state.coachTextRes != null, enter = fadeIn(), exit = fadeOut()) {
                val textRes = state.coachTextRes ?: R.string.coach_tap_arrow
                Text(
                    stringResource(textRes),
                    color = state.palette.accent,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                )
            }

            // Board
            ArrowCanvas(
                board = state.board,
                palette = state.palette,
                escapeEvent = state.escapeEvent,
                blockedEvent = state.blockedEvent,
                hintArrowId = state.hintArrowId,
                contentDescription = stringResource(R.string.a11y_board, state.board.rows, state.board.cols, state.remainingArrows),
                onCellTapped = vm::onCellTapped,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
            )

            // Controls
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                ControlButton(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.undo), enabled = state.canUndo) { vm.undo() }
                ControlButton(
                    Icons.Filled.Lightbulb,
                    stringResource(R.string.hint_count, state.hintsRemaining),
                    tint = if (state.hintsRemaining > 0) AccentCombo else TextMuted,
                ) { vm.hint() }
                ControlButton(Icons.Filled.Refresh, stringResource(R.string.restart)) { vm.restart() }
            }
        }

        state.outcome?.let { OutcomeOverlay(it, state, vm) }
        ToastHost(state, vm)
    }
}

@Composable
private fun ControlButton(icon: ImageVector, label: String, enabled: Boolean = true, tint: Color = TextPrimary, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .pressable(enabled = enabled, onClick = onClick)
            .padding(8.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f },
    ) {
        Box(
            Modifier.size(54.dp).background(SurfaceCard, CircleShape).border(1.dp, SurfaceBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(6.dp))
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
            Surface(shape = RoundedCornerShape(14.dp), color = SurfaceCard, modifier = Modifier.padding(bottom = 96.dp)) {
                Text(text ?: "", color = TextPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
            }
        }
    }
}


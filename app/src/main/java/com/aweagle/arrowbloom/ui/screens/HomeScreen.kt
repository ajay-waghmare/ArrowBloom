package com.aweagle.arrowbloom.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aweagle.arrowbloom.BuildConfig
import com.aweagle.arrowbloom.R
import com.aweagle.arrowbloom.daily.DailyBloom
import com.aweagle.arrowbloom.data.RunMode
import com.aweagle.arrowbloom.engine.Direction
import com.aweagle.arrowbloom.ui.GameUiState
import com.aweagle.arrowbloom.ui.GameViewModel
import com.aweagle.arrowbloom.ui.IconButton
import com.aweagle.arrowbloom.ui.LocalReduceMotion
import com.aweagle.arrowbloom.ui.TextButton
import com.aweagle.arrowbloom.ui.components.PrimaryButton
import com.aweagle.arrowbloom.ui.components.SectionCard
import com.aweagle.arrowbloom.ui.components.SoftButton
import com.aweagle.arrowbloom.ui.components.StarsRow
import com.aweagle.arrowbloom.ui.components.StatTile
import com.aweagle.arrowbloom.ui.components.drawLineArrow
import com.aweagle.arrowbloom.ui.shareText
import com.aweagle.arrowbloom.ui.theme.DotColor
import com.aweagle.arrowbloom.ui.theme.GoldText
import com.aweagle.arrowbloom.ui.theme.Ink
import com.aweagle.arrowbloom.ui.theme.Palettes
import com.aweagle.arrowbloom.ui.theme.StarGold
import com.aweagle.arrowbloom.ui.theme.SurfaceSoft
import com.aweagle.arrowbloom.ui.theme.TextPrimary
import com.aweagle.arrowbloom.ui.theme.TextSecondary

@Composable
fun HomeScreen(state: GameUiState, vm: GameViewModel) {
    val context = LocalContext.current
    val res = context.resources
    var showHowTo by remember { mutableStateOf(false) }
    val titleSize = with(LocalDensity.current) { 38.dp.toSp() }
    val accent = state.palette.accent
    val dailyAccent = Palettes.DAILY.accent

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { vm.feedback.tap(); vm.openSettings() }) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings), tint = accent)
                }
            }

            // Logotype
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.brand_arrow), color = Ink, fontSize = titleSize, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.brand_bloom), color = accent, fontSize = titleSize, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
            Text(stringResource(R.string.tagline), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            HeroBoard(accent)
            Spacer(Modifier.height(12.dp))

            // Journey
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.journey).uppercase(), color = accent, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(stringResource(state.gardenNameRes), color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            if (state.gardenSize > 0) stringResource(R.string.level_of, state.gardenPosition, state.gardenSize) else stringResource(R.string.level_n, state.currentLevel),
                            color = TextSecondary, style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (state.gardenMaxStars > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = StarGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.stars_collected, state.gardenStars, state.gardenMaxStars), color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
                if (state.gardenSize > 0) {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { ((state.gardenPosition - 1).toFloat() / state.gardenSize).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = accent,
                        trackColor = SurfaceSoft,
                    )
                }
                Spacer(Modifier.height(14.dp))
                val resumingLevel = state.hasSavedRun && state.savedRunMode == RunMode.LEVEL
                PrimaryButton(
                    text = if (resumingLevel) stringResource(R.string.continue_run) else stringResource(R.string.play),
                    leading = Icons.Filled.PlayArrow,
                    container = accent,
                    onClick = { vm.feedback.tap(); vm.playOrContinue() },
                )
            }

            Spacer(Modifier.height(12.dp))

            // Daily Bloom
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.daily_bloom).uppercase(), color = dailyAccent, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(stringResource(R.string.daily_number, state.dailyNumber), color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            when {
                                state.dailyDoneToday && state.dailyBlockedToday == 0 -> stringResource(R.string.perfect_clear_short)
                                state.dailyDoneToday -> res.getQuantityString(R.plurals.blocked_taps, state.dailyBlockedToday, state.dailyBlockedToday)
                                else -> stringResource(R.string.daily_same_for_everyone)
                            },
                            color = TextSecondary, style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (state.dailyDoneToday) StarsRow(stars = state.dailyStarsToday, size = 18.dp)
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = if (state.dailyStreak > 0) StarGold else TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (state.dailyStreak > 0) res.getQuantityString(R.plurals.streak_days, state.dailyStreak, state.dailyStreak) else stringResource(R.string.streak_start),
                        color = if (state.dailyStreak > 0) GoldText else TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    )
                    if (state.bestStreak > 1) {
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.best_streak, state.bestStreak), color = TextSecondary, fontSize = 12.sp)
                    }
                }
                if (state.streakShieldReady) {
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.shield_ready), color = TextSecondary, fontSize = 12.sp)
                }
                Spacer(Modifier.height(14.dp))
                val resumingDaily = state.hasSavedRun && state.savedRunMode == RunMode.DAILY
                if (state.dailyDoneToday) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryButton(
                            stringResource(R.string.share), leading = Icons.Filled.Share, container = dailyAccent, modifier = Modifier.weight(1f),
                            onClick = {
                                vm.onShared()
                                shareText(context, DailyBloom.shareText(state.dailyNumber, state.dailyStarsToday, state.dailyArrows, state.dailyBlockedToday, BuildConfig.STORE_URL))
                            },
                        )
                        SoftButton(stringResource(R.string.daily_replay), modifier = Modifier.weight(1f), onClick = { vm.feedback.tap(); vm.startDaily() })
                    }
                } else {
                    PrimaryButton(
                        text = if (resumingDaily) stringResource(R.string.continue_run) else stringResource(R.string.daily_play),
                        leading = Icons.Filled.PlayArrow,
                        container = dailyAccent,
                        onClick = { vm.feedback.tap(); if (resumingDaily) vm.playOrContinue() else vm.startDaily() },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(state.levelsCleared.toString(), stringResource(R.string.stats_levels), accent, Modifier.weight(1f))
                StatTile(state.perfectClears.toString(), stringResource(R.string.stats_perfect), GoldText, Modifier.weight(1f))
                StatTile(if (state.bestCombo > 0) "×${state.bestCombo}" else "—", stringResource(R.string.stats_combo), dailyAccent, Modifier.weight(1f))
            }

            Spacer(Modifier.height(6.dp))
            TextButton(onClick = { vm.feedback.tap(); showHowTo = true }) {
                Text(stringResource(R.string.how_to_play), color = TextSecondary, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(16.dp))
        }

        if (showHowTo) HowToPlayDialog(onDismiss = { showHowTo = false })
    }
}

/**
 * A tiny board drawn with the game's own arrows: three winding lines on a dot grid, the free
 * one glowing in the garden colour. Decorative only.
 */
@Composable
private fun HeroBoard(accent: androidx.compose.ui.graphics.Color) {
    val reduceMotion = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "hero")
    val glow by transition.animateFloat(
        initialValue = if (reduceMotion) 1f else 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "heroGlow",
    )
    Canvas(Modifier.fillMaxWidth().height(104.dp)) {
        val cols = 8
        val rows = 3
        val cell = minOf(size.width / cols, size.height / rows)
        val ox = (size.width - cols * cell) / 2f
        val oy = (size.height - rows * cell) / 2f
        fun px(r: Int, c: Int) = Offset(ox + (c + 0.5f) * cell, oy + (r + 0.5f) * cell)
        // Tail first, head last.
        val arrows = listOf(
            Triple(listOf(2 to 0, 1 to 0, 0 to 0, 0 to 1, 0 to 2), Direction.RIGHT, false),
            Triple(listOf(2 to 2, 2 to 3, 1 to 3, 0 to 3), Direction.UP, true),
            Triple(listOf(0 to 7, 1 to 7, 1 to 6, 2 to 6, 2 to 5, 2 to 4), Direction.LEFT, false),
            Triple(listOf(0 to 5, 1 to 5, 1 to 4), Direction.LEFT, false),
        )
        val used = arrows.flatMap { it.first }.toSet()
        for (r in 0 until rows) for (c in 0 until cols) {
            if ((r to c) !in used) drawCircle(DotColor, cell * 0.075f, px(r, c))
        }
        val stroke = cell * 0.15f
        for ((cells, dir, free) in arrows) {
            val pts = cells.map { (r, c) -> px(r, c) }
            val tail = pts[0] + (pts[0] - pts[1]) * 0.22f
            drawLineArrow(listOf(tail) + pts, dir, stroke, if (free) accent else Ink, 1f, if (free) accent.copy(alpha = 0.22f * glow) else null)
        }
    }
}

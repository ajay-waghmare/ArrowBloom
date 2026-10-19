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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aweagle.arrowbloom.R
import com.aweagle.arrowbloom.data.RunMode
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
import com.aweagle.arrowbloom.ui.shareText
import com.aweagle.arrowbloom.ui.theme.AccentCombo
import com.aweagle.arrowbloom.ui.theme.ArrowBlossom
import com.aweagle.arrowbloom.ui.theme.ArrowGold
import com.aweagle.arrowbloom.ui.theme.SurfaceCard
import com.aweagle.arrowbloom.ui.theme.TextPrimary
import com.aweagle.arrowbloom.ui.theme.TextSecondary
import com.aweagle.arrowbloom.daily.DailyBloom
import com.aweagle.arrowbloom.BuildConfig

@Composable
fun HomeScreen(state: GameUiState, vm: GameViewModel) {
    val context = LocalContext.current
    val res = context.resources
    var showHowTo by remember { mutableStateOf(false) }
    val titleSize = with(LocalDensity.current) { 40.dp.toSp() }

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
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings), tint = TextSecondary)
                }
            }

            // Logotype
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.brand_arrow), color = TextPrimary, fontSize = titleSize, fontWeight = FontWeight.Black, letterSpacing = 2.sp,
                    style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(0f, 4f), 8f)),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.brand_bloom), color = state.palette.accent, fontSize = titleSize, fontWeight = FontWeight.Black, letterSpacing = 2.sp,
                    style = TextStyle(shadow = Shadow(state.palette.accent.copy(alpha = 0.45f), Offset(0f, 4f), 12f)),
                )
            }
            Text(stringResource(R.string.tagline), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(14.dp))
            HeroArrows(state)
            Spacer(Modifier.height(14.dp))

            // Journey
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.journey).uppercase(), color = state.palette.accent, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(stringResource(state.gardenNameRes), color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            if (state.gardenSize > 0) stringResource(R.string.level_of, state.gardenPosition, state.gardenSize) else stringResource(R.string.level_n, state.currentLevel),
                            color = TextSecondary, style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (state.gardenMaxStars > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = ArrowGold, modifier = Modifier.size(16.dp))
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
                        color = state.palette.accent,
                        trackColor = SurfaceCard,
                    )
                }
                Spacer(Modifier.height(14.dp))
                val resumingLevel = state.hasSavedRun && state.savedRunMode == RunMode.LEVEL
                PrimaryButton(
                    text = if (resumingLevel) stringResource(R.string.continue_run) else stringResource(R.string.play),
                    leading = Icons.Filled.PlayArrow,
                    container = state.palette.accent,
                    onClick = { vm.feedback.tap(); vm.playOrContinue() },
                )
            }

            Spacer(Modifier.height(12.dp))

            // Daily Bloom
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.daily_bloom).uppercase(), color = ArrowBlossom, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
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
                    Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = if (state.dailyStreak > 0) ArrowGold else TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (state.dailyStreak > 0) res.getQuantityString(R.plurals.streak_days, state.dailyStreak, state.dailyStreak) else stringResource(R.string.streak_start),
                        color = if (state.dailyStreak > 0) ArrowGold else TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold,
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
                            stringResource(R.string.share), leading = Icons.Filled.Share, container = ArrowBlossom, modifier = Modifier.weight(1f),
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
                        container = ArrowBlossom,
                        onClick = { vm.feedback.tap(); if (resumingDaily) vm.playOrContinue() else vm.startDaily() },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(state.levelsCleared.toString(), stringResource(R.string.stats_levels), state.palette.accent, Modifier.weight(1f))
                StatTile(state.perfectClears.toString(), stringResource(R.string.stats_perfect), ArrowGold, Modifier.weight(1f))
                StatTile(if (state.bestCombo > 0) "×${state.bestCombo}" else "—", stringResource(R.string.stats_combo), AccentCombo, Modifier.weight(1f))
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

/** Four arrows of the current garden's colours, gently breathing. Decorative only. */
@Composable
private fun HeroArrows(state: GameUiState) {
    val reduceMotion = LocalReduceMotion.current
    val amplitude = with(LocalDensity.current) { 3.dp.toPx() }
    val transition = rememberInfiniteTransition(label = "hero")
    val float by transition.animateFloat(
        initialValue = if (reduceMotion) 0f else -amplitude,
        targetValue = if (reduceMotion) 0f else amplitude,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "heroFloat",
    )
    val path = remember {
        Path().apply {
            moveTo(0f, -0.38f); lineTo(0.29f, 0.05f); lineTo(0.11f, 0.05f); lineTo(0.11f, 0.33f)
            lineTo(-0.11f, 0.33f); lineTo(-0.11f, 0.05f); lineTo(-0.29f, 0.05f); close()
        }
    }
    val colors = state.palette.arrows
    Canvas(Modifier.fillMaxWidth().height(96.dp)) {
        val cell = size.height * 0.9f
        val gap = cell * 1.15f
        val startX = size.width / 2f - gap * 1.5f
        val angles = listOf(0f, 90f, 180f, 270f)
        for (i in 0 until 4) {
            val cx = startX + i * gap
            val cy = size.height / 2f + (if (i % 2 == 0) float else -float)
            withTransform({
                translate(cx, cy)
                rotate(angles[i], pivot = Offset.Zero)
                scale(cell, cell, pivot = Offset.Zero)
            }) {
                drawPath(path, color = colors[i % colors.size])
                drawPath(path, color = Color.White.copy(alpha = 0.8f), style = Stroke(width = 0.035f))
                drawCircle(Color.White.copy(alpha = 0.9f), radius = 0.06f, center = Offset(0f, -0.08f))
            }
        }
    }
}

package com.aweagle.arrowbloom.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
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
import com.aweagle.arrowbloom.engine.Scoring
import com.aweagle.arrowbloom.ui.GameUiState
import com.aweagle.arrowbloom.ui.GameViewModel
import com.aweagle.arrowbloom.ui.LocalReduceMotion
import com.aweagle.arrowbloom.ui.Outcome
import com.aweagle.arrowbloom.ui.TextButton
import com.aweagle.arrowbloom.ui.components.PrimaryButton
import com.aweagle.arrowbloom.ui.components.SoftButton
import com.aweagle.arrowbloom.ui.shareText
import com.aweagle.arrowbloom.ui.theme.GoldText
import com.aweagle.arrowbloom.ui.theme.HeartRed
import com.aweagle.arrowbloom.ui.theme.Scrim
import com.aweagle.arrowbloom.ui.theme.StarEmpty
import com.aweagle.arrowbloom.ui.theme.StarGold
import com.aweagle.arrowbloom.ui.theme.SurfaceCard
import com.aweagle.arrowbloom.ui.theme.SurfaceSoft
import com.aweagle.arrowbloom.ui.theme.TextPrimary
import com.aweagle.arrowbloom.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/** A white card on a dimmed board, scaling in. */
@Composable
private fun OverlayCard(onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val reduceMotion = LocalReduceMotion.current
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { if (reduceMotion) appear.snapTo(1f) else appear.animateTo(1f, tween(300)) }
    BackHandler(onBack = onBack)
    Box(
        Modifier
            .fillMaxSize()
            .background(Scrim.copy(alpha = Scrim.alpha * appear.value))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = SurfaceCard,
            modifier = Modifier
                .padding(28.dp)
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(28.dp))
                .graphicsLayer {
                    val sc = 0.88f + 0.12f * appear.value
                    scaleX = sc; scaleY = sc; alpha = appear.value
                },
        ) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, content = content)
        }
    }
}

/** The card after a cleared board: stars pop in one by one, then the next step. */
@Composable
fun OutcomeOverlay(outcome: Outcome, state: GameUiState, vm: GameViewModel) {
    val context = LocalContext.current
    val res = context.resources
    val reduceMotion = LocalReduceMotion.current
    val accent = state.palette.accent
    OverlayCard(onBack = { if (outcome.mode == RunMode.LEVEL) vm.nextLevel() else vm.goHome() }) {
        Text(
            stringResource(if (outcome.mode == RunMode.DAILY) R.string.daily_cleared else R.string.bloom_cleared),
            color = accent,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (outcome.mode == RunMode.DAILY) stringResource(R.string.daily_number, outcome.dailyNumber) else stringResource(R.string.level_n, outcome.level),
            color = TextSecondary,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(18.dp))
        PoppingStars(outcome.stars, reduceMotion)
        Spacer(Modifier.height(12.dp))
        val perfect = outcome.stars == Scoring.MAX_STARS
        Text(
            if (perfect) stringResource(R.string.perfect_clear) else resultLine(res, outcome.heartsLost, outcome.hintsUsed),
            color = if (perfect) GoldText else TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        if (outcome.isNewBest && outcome.mode == RunMode.LEVEL) {
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.new_best), color = accent, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
        if (outcome.mode == RunMode.DAILY) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = StarGold, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(res.getQuantityString(R.plurals.streak_days, outcome.streak, outcome.streak), color = TextPrimary, fontWeight = FontWeight.Bold)
            }
            if (outcome.usedShield) {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.shield_used), color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(22.dp))
        if (outcome.mode == RunMode.LEVEL) {
            PrimaryButton(stringResource(R.string.next_level), container = accent, onClick = { vm.feedback.tap(); vm.nextLevel() })
        } else {
            PrimaryButton(
                stringResource(R.string.share),
                leading = Icons.Filled.Share,
                container = accent,
                onClick = { vm.onShared(); outcome.shareText?.let { shareText(context, it) } },
            )
        }
        Spacer(Modifier.height(10.dp))
        SoftButton(stringResource(R.string.replay), onClick = { vm.feedback.tap(); vm.replay() })
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = { vm.feedback.tap(); vm.goHome() }) {
            Text(stringResource(R.string.home), color = TextSecondary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Out of hearts: the same board again, or home. */
@Composable
fun FailOverlay(state: GameUiState, vm: GameViewModel) {
    OverlayCard(onBack = { vm.goHome() }) {
        HeartsRow(0, size = 34)
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.out_of_hearts), color = HeartRed, fontSize = 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.out_of_hearts_text), color = TextSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(22.dp))
        PrimaryButton(stringResource(R.string.try_again), container = state.palette.accent, onClick = { vm.restart() })
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = { vm.feedback.tap(); vm.goHome() }) {
            Text(stringResource(R.string.home), color = TextSecondary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun PoppingStars(stars: Int, reduceMotion: Boolean) {
    val desc = stringResource(R.string.a11y_stars, stars)
    Row(Modifier.clearAndSetSemantics { contentDescription = desc }, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
        repeat(Scoring.MAX_STARS) { i ->
            val lit = i < stars
            val big = i == Scoring.MAX_STARS / 2
            val near = kotlin.math.abs(i - Scoring.MAX_STARS / 2) == 1
            val box = if (big) 58.dp else if (near) 50.dp else 44.dp
            val drop = if (big) 0.dp else if (near) 8.dp else 18.dp
            val scale = remember { Animatable(if (reduceMotion) 1f else 0f) }
            LaunchedEffect(Unit) {
                if (!reduceMotion) {
                    delay(110L * i + 120L)
                    scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium))
                }
            }
            Box(
                Modifier
                    .padding(top = drop)
                    .size(box)
                    .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                    .background(if (lit) StarGold.copy(alpha = 0.16f) else SurfaceSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = if (lit) StarGold else StarEmpty, modifier = Modifier.size(box * 0.64f))
            }
        }
    }
}

/** Three short cards on a scrim. Reached from Home and Settings. */
@Composable
fun HowToPlayDialog(onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    Box(
        Modifier
            .fillMaxSize()
            .background(Scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = SurfaceCard,
            modifier = Modifier.padding(24.dp).fillMaxWidth().shadow(16.dp, RoundedCornerShape(28.dp)),
        ) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.how_to_play), color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(14.dp))
                HowToStep("1", stringResource(R.string.htp_1_title), stringResource(R.string.htp_1_text))
                HowToStep("2", stringResource(R.string.htp_2_title), stringResource(R.string.htp_2_text))
                HowToStep("3", stringResource(R.string.htp_3_title), stringResource(R.string.htp_3_text))
                Spacer(Modifier.height(10.dp))
                PrimaryButton(stringResource(R.string.got_it), onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun HowToStep(number: String, title: String, text: String) {
    Row(Modifier.padding(bottom = 14.dp)) {
        Box(Modifier.size(28.dp).background(SurfaceSoft, CircleShape), contentAlignment = Alignment.Center) {
            Text(number, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(text, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** "No hearts lost · 1 hint used" style summary of a cleared board that is not perfect. */
fun resultLine(res: android.content.res.Resources, heartsLost: Int, hintsUsed: Int): String = buildList {
    add(if (heartsLost == 0) res.getString(R.string.no_hearts_lost) else res.getQuantityString(R.plurals.blocked_taps, heartsLost, heartsLost))
    if (hintsUsed > 0) add(res.getQuantityString(R.plurals.hints_used, hintsUsed, hintsUsed))
}.joinToString(" · ")

package com.aweagle.arrowbloom.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aweagle.arrowbloom.ui.components.ArrowCanvas
import com.aweagle.arrowbloom.ui.theme.AccentCombo
import com.aweagle.arrowbloom.ui.theme.AccentWin
import com.aweagle.arrowbloom.ui.theme.ArrowCyan
import com.aweagle.arrowbloom.ui.theme.BgDark
import com.aweagle.arrowbloom.ui.theme.SurfaceBorder
import com.aweagle.arrowbloom.ui.theme.SurfaceCard
import com.aweagle.arrowbloom.ui.theme.SurfaceDark
import com.aweagle.arrowbloom.ui.theme.TextMuted
import com.aweagle.arrowbloom.ui.theme.TextPrimary
import com.aweagle.arrowbloom.ui.theme.TextSecondary

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Status Header
            TopHeader(
                level = uiState.levelNumber,
                score = uiState.score,
                combo = uiState.combo,
                remaining = uiState.remainingArrows,
                total = uiState.totalArrows
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Main Interactive Arrow Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ArrowCanvas(
                    board = uiState.board,
                    launchingProgress = uiState.launchingProgress,
                    shakingArrowId = uiState.shakingArrowId,
                    shakeOffset = uiState.shakeOffset,
                    hintArrowId = uiState.hintArrowId,
                    onCellTapped = viewModel::onCellTapped
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Bottom Control Actions
            BottomControls(
                hintsRemaining = uiState.hintsRemaining,
                onUndo = viewModel::onUndo,
                onRestart = viewModel::onRestartLevel,
                onHint = viewModel::onUseHint
            )
        }

        // 4. Level Complete Victory Modal
        AnimatedVisibility(
            visible = uiState.isLevelComplete,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut()
        ) {
            LevelCompleteModal(
                level = uiState.levelNumber,
                score = uiState.score,
                onNextLevel = viewModel::onNextLevel
            )
        }
    }
}

@Composable
private fun TopHeader(
    level: Int,
    score: Int,
    combo: Int,
    remaining: Int,
    total: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level Pill
            Box(
                modifier = Modifier
                    .background(SurfaceCard, RoundedCornerShape(12.dp))
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "LEVEL $level",
                    color = ArrowCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Score Display
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "SCORE",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = score.toString(),
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress bar (Arrows Cleared)
        val progress = if (total > 0) ((total - remaining).toFloat() / total).coerceIn(0f, 1f) else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = ArrowCyan,
            trackColor = SurfaceCard
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$remaining remaining",
                color = TextSecondary,
                fontSize = 12.sp
            )

            // Dynamic Combo Indicator
            if (combo > 1) {
                Text(
                    text = "🔥 ${combo}x COMBO!",
                    color = AccentCombo,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BottomControls(
    hintsRemaining: Int,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onHint: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionButton(icon = Icons.AutoMirrored.Filled.Undo, label = "Undo", onClick = onUndo)
        ActionButton(icon = Icons.Default.Refresh, label = "Restart", onClick = onRestart)
        ActionButton(
            icon = Icons.Default.Lightbulb,
            label = "Hint ($hintsRemaining)",
            tint = if (hintsRemaining > 0) AccentCombo else TextMuted,
            onClick = onHint
        )
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    tint: Color = TextPrimary,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(SurfaceCard, CircleShape)
                .border(1.dp, SurfaceBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LevelCompleteModal(
    level: Int,
    score: Int,
    onNextLevel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(24.dp))
                .border(1.5.dp, SurfaceBorder, RoundedCornerShape(24.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🌸 BLOOM CLEARED! 🌸",
                color = AccentWin,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Level $level Complete",
                color = TextSecondary,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCard, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TOTAL SCORE",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = score.toString(),
                        color = ArrowCyan,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onNextLevel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ArrowCyan,
                    contentColor = BgDark
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "NEXT LEVEL",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Next",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

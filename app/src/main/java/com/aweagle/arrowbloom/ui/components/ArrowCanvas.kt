package com.aweagle.arrowbloom.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.aweagle.arrowbloom.engine.Arrow
import com.aweagle.arrowbloom.engine.ArrowState
import com.aweagle.arrowbloom.engine.Board
import com.aweagle.arrowbloom.engine.GridPos
import com.aweagle.arrowbloom.ui.theme.ArrowColors
import com.aweagle.arrowbloom.ui.theme.GridCellBg
import com.aweagle.arrowbloom.ui.theme.GridCellBorder
import kotlin.math.min

/**
 * High-performance Compose Canvas rendering the Arrow Bloom puzzle grid and arrows.
 */
@Composable
fun ArrowCanvas(
    board: Board,
    launchingProgress: Map<Int, Float>,
    shakingArrowId: Int?,
    shakeOffset: Float,
    hintArrowId: Int?,
    onCellTapped: (GridPos) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().padding(16.dp)) {
        val rows = board.rows
        val cols = board.cols

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(board) {
                    detectTapGestures { tapOffset ->
                        val cellSize = min(size.width / cols, size.height / rows)
                        val startX = (size.width - cols * cellSize) / 2f
                        val startY = (size.height - rows * cellSize) / 2f

                        val col = ((tapOffset.x - startX) / cellSize).toInt()
                        val row = ((tapOffset.y - startY) / cellSize).toInt()

                        if (row in 0 until rows && col in 0 until cols) {
                            onCellTapped(GridPos(row, col))
                        }
                    }
                }
        ) {
            val cellSize = min(size.width / cols, size.height / rows)
            val startX = (size.width - cols * cellSize) / 2f
            val startY = (size.height - rows * cellSize) / 2f
            val cellRadius = CornerRadius(cellSize * 0.16f, cellSize * 0.16f)

            // 1. Draw grid background cells
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val x = startX + c * cellSize
                    val y = startY + r * cellSize
                    val cellPadding = cellSize * 0.06f
                    val cellInnerSize = cellSize - cellPadding * 2

                    drawRoundRect(
                        color = GridCellBg,
                        topLeft = Offset(x + cellPadding, y + cellPadding),
                        size = Size(cellInnerSize, cellInnerSize),
                        cornerRadius = cellRadius,
                        style = Fill
                    )

                    drawRoundRect(
                        color = GridCellBorder,
                        topLeft = Offset(x + cellPadding, y + cellPadding),
                        size = Size(cellInnerSize, cellInnerSize),
                        cornerRadius = cellRadius,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            // 2. Draw hint halo if active
            hintArrowId?.let { hintId ->
                val hintArrow = board.getArrowById(hintId)
                if (hintArrow != null && hintArrow.state != ArrowState.ESCAPED) {
                    val hx = startX + hintArrow.head.col * cellSize + cellSize / 2f
                    val hy = startY + hintArrow.head.row * cellSize + cellSize / 2f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x9900E5FF), Color(0x3300E5FF), Color.Transparent),
                            center = Offset(hx, hy),
                            radius = cellSize * 0.85f
                        ),
                        radius = cellSize * 0.85f,
                        center = Offset(hx, hy)
                    )
                }
            }

            // 3. Draw arrows
            board.arrows.values.forEach { arrow ->
                val isLaunching = launchingProgress.containsKey(arrow.id)
                if (arrow.state == ArrowState.ESCAPED && !isLaunching) {
                    return@forEach
                }

                val progress = launchingProgress[arrow.id] ?: 0f
                val headCol = arrow.head.col
                val headRow = arrow.head.row

                var cx = startX + headCol * cellSize + cellSize / 2f
                var cy = startY + headRow * cellSize + cellSize / 2f

                // Apply launch translation
                if (isLaunching) {
                    val maxTravel = maxOf(size.width, size.height) * 1.2f
                    val travelDistance = maxTravel * progress
                    cx += arrow.direction.dCol * travelDistance
                    cy += arrow.direction.dRow * travelDistance
                }

                // Apply shake offset
                if (arrow.id == shakingArrowId) {
                    // Shake perpendicular to direction for maximum visual punch
                    val perpCol = -arrow.direction.dRow
                    val perpRow = arrow.direction.dCol
                    cx += perpCol * shakeOffset * (cellSize * 0.12f)
                    cy += perpRow * shakeOffset * (cellSize * 0.12f)
                }

                val color = ArrowColors[arrow.colorIndex % ArrowColors.size]
                val arrowAlpha = if (isLaunching) (1f - progress * 0.6f).coerceIn(0f, 1f) else 1f

                drawArrowShape(
                    cx = cx,
                    cy = cy,
                    cellSize = cellSize,
                    directionDegrees = arrow.direction.degrees,
                    baseColor = color.copy(alpha = arrowAlpha),
                    isBlocked = arrow.state == ArrowState.BLOCKED || arrow.id == shakingArrowId
                )
            }
        }
    }
}

/**
 * Draws a single sleek neon arrow rotated in the specified direction degrees.
 */
private fun DrawScope.drawArrowShape(
    cx: Float,
    cy: Float,
    cellSize: Float,
    directionDegrees: Float,
    baseColor: Color,
    isBlocked: Boolean
) {
    val r = cellSize * 0.38f
    val tipY = cy - r * 0.95f
    val wingY = cy + r * 0.15f
    val wingX = r * 0.72f
    val notchX = r * 0.28f
    val stemY = cy + r * 0.85f

    val path = Path().apply {
        // Arrow tip
        moveTo(cx, tipY)
        // Right wing
        lineTo(cx + wingX, wingY)
        // Right inner notch
        lineTo(cx + notchX, wingY)
        // Right stem down
        lineTo(cx + notchX, stemY)
        // Base flat/rounded
        lineTo(cx - notchX, stemY)
        // Left stem up
        lineTo(cx - notchX, wingY)
        // Left wing
        lineTo(cx - wingX, wingY)
        close()
    }

    withTransform({
        rotate(directionDegrees, pivot = Offset(cx, cy))
    }) {
        val fillColor = if (isBlocked) Color(0xFFFF3366) else baseColor

        // Soft glow / drop shadow
        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(fillColor.copy(alpha = 0.5f), Color.Transparent),
                center = Offset(cx, cy),
                radius = r * 1.4f
            ),
            style = Fill
        )

        // Main vibrant arrow body
        drawPath(
            path = path,
            color = fillColor,
            style = Fill
        )

        // Crisp luminous rim
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.85f),
            style = Stroke(width = (cellSize * 0.035f).coerceAtLeast(1.5f))
        )

        // Inner directional highlight pip
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = cellSize * 0.06f,
            center = Offset(cx, cy - r * 0.2f)
        )
    }
}

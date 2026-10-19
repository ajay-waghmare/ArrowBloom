package com.aweagle.arrowbloom.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.aweagle.arrowbloom.engine.Arrow
import com.aweagle.arrowbloom.engine.ArrowState
import com.aweagle.arrowbloom.engine.Board
import com.aweagle.arrowbloom.engine.Direction
import com.aweagle.arrowbloom.engine.GridPos
import com.aweagle.arrowbloom.ui.BlockedEvent
import com.aweagle.arrowbloom.ui.EscapeEvent
import com.aweagle.arrowbloom.ui.LocalReduceMotion
import com.aweagle.arrowbloom.ui.theme.AccentError
import com.aweagle.arrowbloom.ui.theme.AccentHint
import com.aweagle.arrowbloom.ui.theme.GardenPalette
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/** An arrow flying off the board: the engine already removed it, the canvas still draws it. */
private class LaunchAnim(val arrow: Arrow, val progress: Animatable<Float, AnimationVector1D>)

/** A petal burst where an arrow stood. */
private class Bloom(val cell: GridPos, val colorIndex: Int, val petals: Int, var life: Float, val maxLife: Float)

/**
 * The board. Everything is drawn: cells, long-arrow shafts, arrow heads, the hint halo,
 * escape flights and the bloom bursts that give the game its name. Geometry lives in unit
 * space (one cell = 1) so one cached path and one cached brush per colour serve every size.
 */
@Composable
fun ArrowCanvas(
    board: Board,
    palette: GardenPalette,
    escapeEvent: EscapeEvent?,
    blockedEvent: BlockedEvent?,
    hintArrowId: Int?,
    contentDescription: String,
    onCellTapped: (GridPos) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalReduceMotion.current
    val launches = remember { mutableStateMapOf<Int, LaunchAnim>() }
    val blooms = remember { mutableStateListOf<Bloom>() }
    var bloomFrame by remember { mutableLongStateOf(0L) }
    var shakingId by remember { mutableStateOf<Int?>(null) }
    val shake = remember { Animatable(0f) }
    val headPath = remember { unitArrowPath() }
    val glowBrushes = remember(palette) { palette.arrows.map { glowBrush(it) } }

    LaunchedEffect(escapeEvent?.seq) {
        val ev = escapeEvent ?: return@LaunchedEffect
        blooms.add(
            Bloom(
                cell = ev.arrow.head,
                colorIndex = ev.arrow.colorIndex,
                petals = (5 + ev.combo).coerceIn(5, 9),
                life = 0f,
                maxLife = if (reduceMotion) 0.25f else 0.62f,
            ),
        )
        if (reduceMotion) return@LaunchedEffect
        val anim = LaunchAnim(ev.arrow, Animatable(0f))
        launches[ev.arrow.id] = anim
        anim.progress.animateTo(1f, tween(320, easing = FastOutLinearInEasing))
        launches.remove(ev.arrow.id)
    }

    val bloomsActive = blooms.isNotEmpty()
    LaunchedEffect(bloomsActive) {
        if (!bloomsActive) return@LaunchedEffect
        var last = 0L
        while (blooms.isNotEmpty()) {
            withFrameNanos { t ->
                if (last == 0L) last = t
                val dt = ((t - last) / 1_000_000_000f).coerceAtMost(0.05f)
                last = t
                for (i in blooms.indices.reversed()) {
                    val b = blooms[i]
                    b.life += dt
                    if (b.life >= b.maxLife) blooms.removeAt(i)
                }
                bloomFrame = t
            }
        }
    }

    LaunchedEffect(blockedEvent?.seq) {
        val ev = blockedEvent ?: return@LaunchedEffect
        shakingId = ev.arrowId
        if (reduceMotion) {
            delay(220)
        } else {
            shake.snapTo(0f)
            shake.animateTo(1f, tween(300, easing = LinearEasing))
        }
        shakingId = null
    }

    val hintPulse = if (hintArrowId != null && !reduceMotion) {
        val transition = rememberInfiniteTransition(label = "hint")
        val v by transition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "hintPulse",
        )
        v
    } else 1f

    Canvas(
        modifier = modifier
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(board) {
                detectTapGestures { tap ->
                    val cell = min(size.width / board.cols.toFloat(), size.height / board.rows.toFloat())
                    val ox = (size.width - board.cols * cell) / 2f
                    val oy = (size.height - board.rows * cell) / 2f
                    val col = ((tap.x - ox) / cell).toInt()
                    val row = ((tap.y - oy) / cell).toInt()
                    if (tap.x >= ox && tap.y >= oy && row in 0 until board.rows && col in 0 until board.cols) {
                        onCellTapped(GridPos(row, col))
                    }
                }
            },
    ) {
        @Suppress("UNUSED_EXPRESSION") bloomFrame
        val rows = board.rows
        val cols = board.cols
        val cell = min(size.width / cols, size.height / rows)
        val ox = (size.width - cols * cell) / 2f
        val oy = (size.height - rows * cell) / 2f
        val radius = CornerRadius(cell * 0.16f, cell * 0.16f)
        val pad = cell * 0.06f
        val inner = cell - pad * 2
        val border = (cell * 0.018f).coerceAtLeast(1f)

        fun centerOf(p: GridPos) = Offset(ox + p.col * cell + cell / 2f, oy + p.row * cell + cell / 2f)

        // 1. Cells
        for (r in 0 until rows) for (c in 0 until cols) {
            val tl = Offset(ox + c * cell + pad, oy + r * cell + pad)
            drawRoundRect(palette.cell, tl, Size(inner, inner), radius)
            drawRoundRect(palette.cellBorder, tl, Size(inner, inner), radius, style = Stroke(border))
        }

        // 2. Hint halo
        hintArrowId?.let { id ->
            val a = board.getArrowById(id)
            if (a != null && a.state != ArrowState.ESCAPED) {
                val c = centerOf(a.head)
                val rr = cell * 0.9f * hintPulse
                drawCircle(
                    brush = Brush.radialGradient(listOf(AccentHint.copy(alpha = 0.55f), AccentHint.copy(alpha = 0.18f), Color.Transparent), c, rr),
                    radius = rr,
                    center = c,
                )
            }
        }

        // 3. Arrows
        val travel = maxOf(size.width, size.height) * 1.15f
        val shakeAmp = if (shakingId != null) sin(shake.value * PI.toFloat() * 6f) * (1f - shake.value) * cell * 0.12f else 0f
        for (arrow in board.arrows.values) {
            val launch = launches[arrow.id]
            if (arrow.state == ArrowState.ESCAPED && launch == null) continue
            val progress = launch?.progress?.value ?: 0f
            var dx = arrow.direction.dCol * travel * progress
            var dy = arrow.direction.dRow * travel * progress
            if (arrow.id == shakingId) {
                dx += -arrow.direction.dRow * shakeAmp
                dy += arrow.direction.dCol * shakeAmp
            }
            val color = palette.arrows[arrow.colorIndex % palette.arrows.size]
            val blocked = arrow.state == ArrowState.BLOCKED || arrow.id == shakingId
            val fill = if (blocked) AccentError else color
            val alpha = if (launch != null) (1f - progress * 0.75f).coerceIn(0f, 1f) else 1f
            val head = centerOf(arrow.head)
            drawArrow(
                arrow = arrow,
                head = Offset(head.x + dx, head.y + dy),
                cell = cell,
                fill = fill,
                alpha = alpha,
                glow = glowBrushes[arrow.colorIndex % glowBrushes.size],
                headPath = headPath,
            )
        }

        // 4. Blooms
        for (b in blooms) {
            val p = (b.life / b.maxLife).coerceIn(0f, 1f)
            val grow = 1f - (1f - p) * (1f - p)
            val alpha = (1f - p) * 0.7f
            val c = centerOf(b.cell)
            val reach = cell * 0.62f * grow
            val petalW = cell * 0.26f
            val petalH = cell * 0.42f
            val color = palette.arrows[b.colorIndex % palette.arrows.size]
            for (k in 0 until b.petals) {
                val deg = k * (360f / b.petals) + grow * 35f
                rotate(deg, pivot = c) {
                    drawOval(
                        color = color.copy(alpha = alpha),
                        topLeft = Offset(c.x - petalW / 2f, c.y - reach - petalH / 2f),
                        size = Size(petalW, petalH),
                    )
                }
            }
            drawCircle(Color.White.copy(alpha = alpha * 0.8f), cell * 0.12f, c)
        }
    }
}

/** Unit-space arrow head: tip up, centred on the origin, fits in a 0.8 x 0.8 box. */
private fun unitArrowPath(): Path = Path().apply {
    moveTo(0f, -0.38f)
    lineTo(0.29f, 0.05f)
    lineTo(0.11f, 0.05f)
    lineTo(0.11f, 0.33f)
    lineTo(-0.11f, 0.33f)
    lineTo(-0.11f, 0.05f)
    lineTo(-0.29f, 0.05f)
    close()
}

private fun glowBrush(color: Color): Brush =
    Brush.radialGradient(listOf(color.copy(alpha = 0.55f), Color.Transparent), center = Offset.Zero, radius = 0.6f)

private fun DrawScope.drawArrow(
    arrow: Arrow,
    head: Offset,
    cell: Float,
    fill: Color,
    alpha: Float,
    glow: Brush,
    headPath: Path,
) {
    // Shaft for long arrows: from the tail cell centre to the head cell centre.
    if (arrow.body.size > 1) {
        val len = (arrow.body.size - 1) * cell
        val w = cell * 0.42f
        val tailDir = arrow.direction.opposite
        val tail = Offset(head.x + tailDir.dCol * len, head.y + tailDir.dRow * len)
        val left = min(head.x, tail.x) - w / 2f
        val top = min(head.y, tail.y) - w / 2f
        val width = if (arrow.direction == Direction.LEFT || arrow.direction == Direction.RIGHT) len + w else w
        val height = if (arrow.direction == Direction.UP || arrow.direction == Direction.DOWN) len + w else w
        drawRoundRect(fill.copy(alpha = alpha * 0.9f), Offset(left, top), Size(width, height), CornerRadius(w / 2f, w / 2f))
        drawRoundRect(Color.White.copy(alpha = alpha * 0.5f), Offset(left, top), Size(width, height), CornerRadius(w / 2f, w / 2f), style = Stroke((cell * 0.03f).coerceAtLeast(1.5f)))
        drawCircle(Color.White.copy(alpha = alpha * 0.55f), w * 0.22f, tail)
    }
    withTransform({
        translate(head.x, head.y)
        rotate(arrow.direction.degrees, pivot = Offset.Zero)
        scale(cell, cell, pivot = Offset.Zero)
    }) {
        drawPath(headPath, brush = glow, alpha = alpha)
        drawPath(headPath, color = fill, alpha = alpha)
        drawPath(headPath, color = Color.White, alpha = alpha * 0.85f, style = Stroke(width = 0.035f))
        drawCircle(Color.White, radius = 0.06f, center = Offset(0f, -0.08f), alpha = alpha * 0.9f)
    }
}

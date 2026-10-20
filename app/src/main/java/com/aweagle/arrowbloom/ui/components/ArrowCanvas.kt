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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aweagle.arrowbloom.engine.Arrow
import com.aweagle.arrowbloom.engine.ArrowState
import com.aweagle.arrowbloom.engine.Board
import com.aweagle.arrowbloom.engine.CollisionDetector
import com.aweagle.arrowbloom.engine.Direction
import com.aweagle.arrowbloom.engine.GridPos
import com.aweagle.arrowbloom.ui.BlockedEvent
import com.aweagle.arrowbloom.ui.EscapeEvent
import com.aweagle.arrowbloom.ui.LocalReduceMotion
import com.aweagle.arrowbloom.ui.theme.AccentError
import com.aweagle.arrowbloom.ui.theme.DotColor
import com.aweagle.arrowbloom.ui.theme.GardenPalette
import com.aweagle.arrowbloom.ui.theme.GridLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** How long the slide-and-bump after a wrong tap takes; the view model waits for it. */
fun bumpDurationMs(freeSteps: Int): Long = (280L + 70L * freeSteps).coerceAtMost(900L)

private val MAX_STROKE = 6.dp
private val MAX_DOT = 3.2.dp

/** How far a line runs past the centre of its tail cell, in cells. */
private const val TAIL = 0.16f

/**
 * An arrow moving along its own line: out of the board, or forward and back after a wrong tap.
 * [track] is in grid units (cell centres at x + 0.5): the body from tail to head, then the
 * straight line ahead of the head. The arrow is the stretch of the track from s to s + length.
 */
private class Flight(
    val arrow: Arrow,
    val track: List<Offset>,
    val escape: Boolean,
    val reach: Float,
    val progress: Animatable<Float, AnimationVector1D>,
)

/** Petals bursting at the point where an arrow left the board. */
private class Bloom(val at: Offset, val petals: Int, var life: Float, val maxLife: Float)

private class Geo(val cell: Float, val ox: Float, val oy: Float) {
    fun px(g: Offset) = Offset(ox + g.x * cell, oy + g.y * cell)
    fun grid(p: Offset) = Offset((p.x - ox) / cell, (p.y - oy) / cell)
}

private fun geoFor(width: Float, height: Float, board: Board): Geo {
    val cell = min(width / board.cols, height / board.rows)
    return Geo(cell, (width - board.cols * cell) / 2f, (height - board.rows * cell) / 2f)
}

private fun centre(p: GridPos) = Offset(p.col + 0.5f, p.row + 0.5f)
private fun vec(d: Direction) = Offset(d.dCol.toFloat(), d.dRow.toFloat())

/** Tail to head, then [ahead] more cells in the head's direction. */
private fun trackOf(arrow: Arrow, ahead: Int): List<Offset> {
    val out = ArrayList<Offset>(arrow.body.size + ahead)
    for (i in arrow.body.indices.reversed()) out.add(centre(arrow.body[i]))
    val h = centre(arrow.head)
    val d = vec(arrow.direction)
    for (k in 1..ahead) out.add(h + d * k.toFloat())
    return out
}

/** The point at arc length [x] along [track]; segments are one cell long, so x indexes it. */
private fun pointAt(track: List<Offset>, x: Float): Offset {
    val i = floor(x).toInt().coerceIn(0, track.size - 2)
    val f = x - i
    return track[i] + (track[i + 1] - track[i]) * f
}

/** The stretch of [track] from [from] to [to] as a polyline (extrapolates a little past the tail). */
private fun stretch(track: List<Offset>, from: Float, to: Float): List<Offset> {
    val pts = ArrayList<Offset>()
    pts.add(pointAt(track, from))
    var i = floor(from).toInt() + 1
    while (i < to && i < track.size) {
        if (i > from) pts.add(track[i])
        i++
    }
    pts.add(pointAt(track, to))
    return pts
}

/**
 * The board: dots on empty cells, ink lines for arrows, the hint glow, the slide-out and
 * bump animations, and the petal bursts that give the game its name.
 */
@Composable
fun ArrowCanvas(
    board: Board,
    palette: GardenPalette,
    escapeEvent: EscapeEvent?,
    blockedEvent: BlockedEvent?,
    hintArrowId: Int?,
    showGrid: Boolean,
    contentDescription: String,
    onArrowTapped: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalReduceMotion.current
    val scope = rememberCoroutineScope()
    val flights = remember { mutableStateMapOf<Int, Flight>() }
    val blooms = remember { mutableStateListOf<Bloom>() }
    var bloomFrame by remember { mutableLongStateOf(0L) }
    var flashId by remember { mutableStateOf<Int?>(null) }
    val flash = remember { Animatable(0f) }

    LaunchedEffect(escapeEvent?.seq) {
        val ev = escapeEvent ?: return@LaunchedEffect
        val a = ev.arrow
        val ray = CollisionDetector.rayLength(a.head, a.direction, board.rows, board.cols)
        val bloom = Bloom(
            at = centre(a.head) + vec(a.direction) * (ray + 0.5f),
            petals = (5 + ev.combo / 2).coerceIn(5, 9),
            life = 0f,
            maxLife = if (reduceMotion) 0.3f else 0.7f,
        )
        if (reduceMotion) {
            blooms.add(bloom)
            return@LaunchedEffect
        }
        val reach = ray + a.body.size + 1.5f
        val duration = (220 + 24 * reach).toInt().coerceAtMost(700)
        val flight = Flight(a, trackOf(a, ceil(reach).toInt() + 2), escape = true, reach = reach, progress = Animatable(0f))
        flights[a.id] = flight
        // Own coroutines: the next tap must not cancel this flight half way.
        scope.launch {
            try {
                flight.progress.animateTo(1f, tween(duration, easing = FastOutLinearInEasing))
            } finally {
                if (flights[a.id] === flight) flights.remove(a.id)
            }
        }
        // The petals open when the head crosses the edge (the easing is roughly quadratic).
        scope.launch {
            delay((duration * sqrt(((ray + 0.5f) / reach).coerceIn(0f, 1f))).toLong())
            blooms.add(bloom)
        }
    }

    LaunchedEffect(blockedEvent?.seq) {
        val ev = blockedEvent ?: return@LaunchedEffect
        val a = board.getArrowById(ev.arrowId) ?: return@LaunchedEffect
        val duration = bumpDurationMs(ev.freeSteps)
        if (!reduceMotion) {
            val flight = Flight(a, trackOf(a, ev.freeSteps + 2), escape = false, reach = ev.freeSteps + 0.3f, progress = Animatable(0f))
            flights[a.id] = flight
            scope.launch {
                try {
                    flight.progress.animateTo(1f, tween(duration.toInt(), easing = LinearEasing))
                } finally {
                    if (flights[a.id] === flight) flights.remove(a.id)
                }
            }
        }
        scope.launch {
            if (!reduceMotion) delay(duration / 2)
            flashId = ev.blockerId
            flash.snapTo(1f)
            flash.animateTo(0f, tween(560))
            if (flashId == ev.blockerId) flashId = null
        }
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

    val hintPulse = if (hintArrowId != null && !reduceMotion) {
        val transition = rememberInfiniteTransition(label = "hint")
        val v by transition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "hintPulse",
        )
        v
    } else 1f

    Canvas(
        modifier = modifier
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(board) {
                detectTapGestures { tap ->
                    val geo = geoFor(size.width.toFloat(), size.height.toFloat(), board)
                    nearestArrow(board, geo.grid(tap))?.let(onArrowTapped)
                }
            },
    ) {
        @Suppress("UNUSED_EXPRESSION") bloomFrame
        val geo = geoFor(size.width, size.height, board)
        val cell = geo.cell
        // Lines stay slim on small boards: a share of the cell, capped in dp.
        val stroke = min(cell * 0.15f, MAX_STROKE.toPx())
        val dot = min(cell * 0.075f, MAX_DOT.toPx())

        // 1. Optional grid, then a dot on every empty playable cell.
        for (r in 0 until board.rows) for (c in 0 until board.cols) {
            val p = GridPos(r, c)
            if (!board.isPlayable(p)) continue
            if (showGrid) {
                drawRect(GridLine, geo.px(Offset(c.toFloat(), r.toFloat())), Size(cell, cell), style = Stroke(1.dp.toPx()))
            }
            if (board.getArrowAt(p) == null) drawCircle(DotColor, dot, geo.px(centre(p)))
        }

        // 2. Arrows at rest.
        for (arrow in board.arrows.values) {
            if (arrow.state == ArrowState.ESCAPED || flights.containsKey(arrow.id)) continue
            val track = trackOf(arrow, 1)
            val pts = stretch(track, -TAIL, (arrow.body.size - 1).toFloat()).map(geo::px)
            val hinted = arrow.id == hintArrowId
            val color = when {
                arrow.state == ArrowState.BLOCKED -> AccentError
                arrow.id == flashId -> lerp(palette.ink, AccentError, flash.value)
                hinted -> palette.accent
                else -> palette.ink
            }
            drawLineArrow(pts, arrow.direction, stroke, color, 1f, if (hinted) palette.accent.copy(alpha = 0.22f * hintPulse) else null)
        }

        // 3. Arrows in motion.
        for (f in flights.values) {
            val p = f.progress.value
            val s = if (f.escape) f.reach * p else f.reach * sin(PI.toFloat() * p)
            val length = (f.arrow.body.size - 1).toFloat()
            val pts = stretch(f.track, s - TAIL, s + length).map(geo::px)
            val alpha = if (f.escape && p > 0.6f) (1f - (p - 0.6f) / 0.4f).coerceIn(0f, 1f) else 1f
            drawLineArrow(pts, f.arrow.direction, stroke, if (f.escape) palette.ink else AccentError, alpha, null)
        }

        // 4. Blooms where arrows left.
        for (b in blooms) {
            val t = (b.life / b.maxLife).coerceIn(0f, 1f)
            val grow = 1f - (1f - t) * (1f - t)
            val alpha = (1f - t) * 0.85f
            val c = geo.px(b.at)
            val reach = cell * 0.75f * grow
            val w = cell * 0.24f
            val h = cell * 0.42f
            for (k in 0 until b.petals) {
                val deg = k * (360f / b.petals) + grow * 40f
                rotate(deg, pivot = c) {
                    drawOval(palette.petals[k % palette.petals.size].copy(alpha = alpha), Offset(c.x - w / 2f, c.y - reach - h / 2f), Size(w, h))
                }
            }
            drawCircle(Color.White.copy(alpha = alpha), cell * 0.12f, c)
        }
    }
}

/** The arrow whose line passes closest to [g] (grid units), if within reach of a finger. */
private fun nearestArrow(board: Board, g: Offset): Int? {
    var best: Int? = null
    var bestDist = 0.62f
    for (a in board.activeArrows) {
        val pts = a.body.map(::centre)
        var d = dist(g, pts[0])
        for (i in 1 until pts.size) d = min(d, distToSegment(g, pts[i - 1], pts[i]))
        d = min(d, dist(g, pts[0] + vec(a.direction) * 0.3f))
        if (d < bestDist) {
            bestDist = d
            best = a.id
        }
    }
    return best
}

private fun dist(a: Offset, b: Offset): Float = (a - b).getDistance()

private fun distToSegment(p: Offset, a: Offset, b: Offset): Float {
    val ab = b - a
    val len2 = ab.x * ab.x + ab.y * ab.y
    if (len2 == 0f) return dist(p, a)
    val t = (((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / len2).coerceIn(0f, 1f)
    return dist(p, a + ab * t)
}

/**
 * One arrow: a line through [points] (pixels, tail first) and a solid head at the last point
 * pointing [dir]. Shared by the board and the decorative arrows on Home.
 */
fun DrawScope.drawLineArrow(points: List<Offset>, dir: Direction, strokeWidth: Float, color: Color, alpha: Float, glow: Color?) {
    if (points.size < 2 || alpha <= 0f) return
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
    }
    if (glow != null) drawPath(path, glow, style = Stroke(strokeWidth * 3.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, color, alpha = alpha, style = Stroke(strokeWidth, cap = StrokeCap.Butt, join = StrokeJoin.Miter))
    val unit = strokeWidth / 0.15f
    val d = Offset(dir.dCol.toFloat(), dir.dRow.toFloat())
    val perp = Offset(-d.y, d.x)
    val head = points.last()
    val tip = head + d * (0.40f * unit)
    val base = head - d * (0.04f * unit)
    val half = 0.25f * unit
    val tri = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(base.x + perp.x * half, base.y + perp.y * half)
        lineTo(base.x - perp.x * half, base.y - perp.y * half)
        close()
    }
    if (glow != null) drawCircle(glow, half * 1.6f, head)
    drawPath(tri, color, alpha = alpha)
}

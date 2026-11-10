package net.sentinelsafeguard.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import net.sentinelsafeguard.app.ui.theme.SentinelGreen
import kotlin.math.PI
import kotlin.math.sin

/**
 * M-18 icon state-morphs for the app (Icon State-Morph Program,
 * 2026-11-07 — DESIGN-LANGUAGE.md M-18, Reel 8: "icons should
 * move, not swap"). An icon that represents a state change
 * animates between its states; the morph is decoration on a
 * real state change — semantics and behavior toggle exactly as
 * before, and under the system animator setting (reduced
 * motion, [rememberReducedMotion]) every morph collapses to
 * the instant swap.
 *
 * Honest-fit (the M-18 rule): a pattern ships only where the
 * control genuinely changes that state in this app.
 *  - Search↔X — the live search/filter fields (global Search,
 *    Clients directory, Tickets directory): the field's
 *    trailing affordance is the
 *    magnifier while the query is empty and morphs into the
 *    clear (X) action while text is present; tapping it in
 *    the X state clears the query. Forward and reverse are
 *    the same 400 ms motion.
 *  - Send arc — the genuine send controls (ticket portal
 *    reply, portal thread reply): on a
 *    SUCCESSFUL send the plane flies out on a curved arc and
 *    a fresh plane springs in behind it. A failed send never
 *    flies — the message did not leave.
 *  - Download story-arc — the Settings app-update download,
 *    the one download whose phases this screen owns: arrow
 *    drops in, becomes a ring (indeterminate — the update
 *    transport reports no byte total to this layer, so no
 *    progress value is invented), resolves into the M-06
 *    drawn tick when the download actually completes.
 *  - Unlock spring — the Lock screen: the lock state truly
 *    changes there (biometric success unlocks the shell).
 *    The shackle springs up with 34% overshoot and swings
 *    open, then the shell unlocks.
 * NOT shipped, by the same rule: play/pause (the app has no
 * media playback control — voice notes download + share
 * only) and theme switch (the app is dark-only, a single
 * darkColorScheme; no theme toggle exists to morph).
 *
 * The pure numbers live in [IconMorphLogic] so the unit suite
 * can pin them without a device; the composables only render
 * what the logic decides. Colors: Sentinel green family only.
 */
object IconMorphLogic {
    /** (d) Search↔X: one 400 ms motion, forward and reverse. */
    const val SEARCH_MORPH_MS: Int = 400

    /** (e) Send arc: the M-07 flight beat, ~500 ms. */
    const val SEND_ARC_MS: Int = 500

    /** (b) Download story-arc: 1.2 s for the full story. */
    const val DOWNLOAD_STORY_MS: Int = 1200

    /** (f) Unlock spring: shackle lift overshoots by 34%. */
    const val UNLOCK_OVERSHOOT: Float = 1.34f

    /** (f) Unlock spring: duration of the lift + swing. */
    const val UNLOCK_SPRING_MS: Int = 560

    /** (f) Unlock spring: the shackle also swings open. */
    const val UNLOCK_SWING_DEGREES: Float = 32f

    /** (d) The magnifier circle undraws linearly with progress. */
    fun searchCircleFraction(progress: Float): Float =
        1f - progress.coerceIn(0f, 1f)

    /**
     * (d) The X's second diagonal grows over the second half —
     * the handle first swings into the first diagonal, then
     * the cross stroke completes the X.
     */
    fun searchCrossFraction(progress: Float): Float =
        ((progress.coerceIn(0f, 1f) - 0.5f) / 0.5f).coerceIn(0f, 1f)

    /**
     * (e) Send-arc offsets in dp for [progress] of the flight:
     * the plane leaves along a rising curve (x grows, y arcs
     * up then levels) — M-07 physics in miniature.
     */
    fun sendArcXDp(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return 26f * p
    }

    fun sendArcYDp(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return (-14f * sin(p * PI.toFloat() * 0.72f)).toFloat()
    }

    /** (e) The departing plane fades only in its last stretch. */
    fun sendDepartAlpha(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return if (p < 0.7f) 1f else 1f - (p - 0.7f) / 0.3f
    }

    /** (e) The fresh plane springs in over the last 30%. */
    fun sendArriveScale(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return if (p < 0.7f) 0f else (p - 0.7f) / 0.3f
    }

    /**
     * (f) Shackle lift for the unlock spring: an ease-out-back
     * curve tuned so the lift peaks at [UNLOCK_OVERSHOOT]×
     * its resting travel before settling at 1. For
     * easeOutBack the peak is 1 + (4/27)·s³/(s+1)², so
     * s = 3.7 lands the peak at ≈1.34 (pinned in the suite).
     */
    fun unlockLift(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f).toDouble()
        val s = 3.7
        val q = p - 1.0
        return (1.0 + (s + 1.0) * q * q * q + s * q * q).toFloat()
    }

    /** (f) The swing follows the same spring, scaled to degrees. */
    fun unlockSwingDegrees(progress: Float): Float =
        UNLOCK_SWING_DEGREES * unlockLift(progress)

    /**
     * (b) Download phases: the arrow owns the first quarter of
     * the story, the ring the middle, the tick the last fifth
     * (the tick itself reuses MotionLogic's 60/40 beats).
     */
    fun downloadArrowFraction(progress: Float): Float =
        (progress.coerceIn(0f, 1f) / 0.25f).coerceAtMost(1f)

    fun downloadRingFraction(progress: Float): Float =
        ((progress.coerceIn(0f, 1f) - 0.25f) / 0.55f).coerceIn(0f, 1f)

    /** (g) Play/pause: one 300 ms morph (1.4.0 item 10 — the
     *  app gained a genuine playback control, ticket voice
     *  notes, so the honest-fit rule now admits the pattern
     *  the header records as previously unshipped). */
    const val PLAY_PAUSE_MORPH_MS: Int = 300

    /**
     * (g) The play triangle collapses horizontally into the
     * left pause bar as progress runs 0 → 1.
     */
    fun playCollapseFraction(progress: Float): Float =
        1f - progress.coerceIn(0f, 1f)

    /**
     * (g) The pause bars grow from the vertical center over
     * the same motion (bar height fraction + alpha).
     */
    fun pauseBarFraction(progress: Float): Float =
        progress.coerceIn(0f, 1f)
}

/** The phases the Settings update download actually owns. */
enum class DownloadPhase { IDLE, ACTIVE, DONE }

/**
 * (d) Search↔X morph icon for a search field's trailing slot.
 * [hasQuery] false renders the magnifier; true renders the X
 * and makes the icon the field's clear action ([onClear]).
 * The state semantics are the field's own: contentDescription
 * names the action the icon currently performs. Under reduced
 * motion the swap is instant.
 */
@Composable
fun SearchMorphIcon(
    hasQuery: Boolean,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = SentinelGreen,
) {
    val reduced = rememberReducedMotion()
    val progress by animateFloatAsState(
        targetValue = if (hasQuery) 1f else 0f,
        animationSpec = if (reduced) {
            tween(0)
        } else {
            tween(IconMorphLogic.SEARCH_MORPH_MS)
        },
        label = "searchMorph",
    )
    Canvas(
        modifier
            .size(24.dp)
            .clickable(
                enabled = hasQuery,
                role = Role.Button,
                onClickLabel = "Clear search",
                onClick = onClear,
            ),
    ) {
        val stroke = 2.dp.toPx()
        val u = size.minDimension / 24f
        fun pt(x: Float, y: Float) = Offset(x * u, y * u)
        fun lerp(a: Offset, b: Offset, f: Float) =
            Offset(a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f)
        val p = progress
        // Magnifier circle undraws as the X forms.
        val circleFrac = IconMorphLogic.searchCircleFraction(p)
        if (circleFrac > 0.01f) {
            drawArc(
                color = tint,
                startAngle = -90f,
                sweepAngle = 360f * circleFrac,
                useCenter = false,
                topLeft = pt(4.5f, 4.5f),
                size = Size(13f * u, 13f * u),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        // The handle swings into the X's first diagonal.
        val handleStart = pt(15.8f, 15.8f)
        val handleEnd = pt(20.5f, 20.5f)
        val diagAStart = pt(6.5f, 6.5f)
        val diagAEnd = pt(17.5f, 17.5f)
        drawLine(
            color = tint,
            start = lerp(handleStart, diagAStart, p),
            end = lerp(handleEnd, diagAEnd, p),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        // The second diagonal completes the X.
        val cross = IconMorphLogic.searchCrossFraction(p)
        if (cross > 0f) {
            val c = pt(12f, 12f)
            val bStart = pt(17.5f, 6.5f)
            val bEnd = pt(6.5f, 17.5f)
            drawLine(
                color = tint,
                start = lerp(c, bStart, cross),
                end = lerp(c, bEnd, cross),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Draws the paper plane (send glyph) pointing up-right. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPlane(
    color: Color,
    alpha: Float,
    scale: Float,
) {
    if (scale <= 0f || alpha <= 0f) return
    val u = size.minDimension / 24f
    val cx = size.width / 2f
    val cy = size.height / 2f
    fun pt(x: Float, y: Float) = Offset(
        cx + (x - 12f) * u * scale,
        cy + (y - 12f) * u * scale,
    )
    val path = Path().apply {
        moveTo(pt(3f, 11.5f).x, pt(3f, 11.5f).y) // left point
        lineTo(pt(21f, 3.5f).x, pt(21f, 3.5f).y) // nose
        lineTo(pt(13.5f, 20.5f).x, pt(13.5f, 20.5f).y) // bottom fin
        lineTo(pt(10.5f, 14f).x, pt(10.5f, 14f).y) // inner notch
        close()
    }
    drawPath(path, color = color.copy(alpha = alpha))
    // The fold line from notch to nose.
    drawLine(
        color = color.copy(alpha = alpha * 0.55f),
        start = pt(10.5f, 14f),
        end = pt(21f, 3.5f),
        strokeWidth = 1.2.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

/**
 * (e) Send arc icon. [sendKey] increments ONLY on a successful
 * send: the plane flies out on the curved arc while a fresh
 * plane springs in behind it, then all is at rest again.
 * Under reduced motion the plane never moves.
 */
@Composable
fun SendArcIcon(
    sendKey: Int,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    val reduced = rememberReducedMotion()
    val progress = remember { Animatable(1f) }
    LaunchedEffect(sendKey) {
        if (sendKey > 0) {
            if (reduced) {
                progress.snapTo(1f)
            } else {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(IconMorphLogic.SEND_ARC_MS))
            }
        }
    }
    val color = if (tint == Color.Unspecified) {
        androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
    } else {
        tint
    }
    Canvas(modifier.size(20.dp)) {
        val p = progress.value
        if (p >= 1f) {
            drawPlane(color, alpha = 1f, scale = 1f)
        } else {
            // The fresh plane springs in behind the departing one.
            val arrive = IconMorphLogic.sendArriveScale(p)
            if (arrive > 0f) {
                drawPlane(color, alpha = arrive, scale = 0.5f + 0.5f * arrive)
            }
            translate(
                left = IconMorphLogic.sendArcXDp(p).dp.toPx(),
                top = IconMorphLogic.sendArcYDp(p).dp.toPx(),
            ) {
                drawPlane(color, alpha = IconMorphLogic.sendDepartAlpha(p), scale = 1f)
            }
        }
    }
}

/**
 * (b) Download story-arc icon, driven by the real [phase] of
 * the Settings update download. IDLE: the arrow at rest.
 * ACTIVE: the arrow drops in, then the ring — indeterminate
 * (a rotating filled sweep), because the update transport
 * reports no byte total to this layer and no progress value
 * may be invented. DONE: the ring completes and the tick
 * draws (MotionLogic's M-06 beats), held until the phase
 * leaves DONE. Under reduced motion each phase is its final
 * still frame.
 */
@Composable
fun DownloadStoryIcon(
    phase: DownloadPhase,
    modifier: Modifier = Modifier,
    tint: Color = SentinelGreen,
) {
    val reduced = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "downloadRing")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "downloadRingSpin",
    )
    val tickProgress = remember { Animatable(0f) }
    LaunchedEffect(phase) {
        if (phase == DownloadPhase.DONE) {
            if (reduced) {
                tickProgress.snapTo(1f)
            } else {
                tickProgress.snapTo(0f)
                tickProgress.animateTo(
                    1f,
                    tween(IconMorphLogic.DOWNLOAD_STORY_MS / 3),
                )
            }
        } else {
            tickProgress.snapTo(0f)
        }
    }
    Canvas(modifier.size(22.dp)) {
        val stroke = 2.2.dp.toPx()
        val u = size.minDimension / 24f
        fun pt(x: Float, y: Float) = Offset(x * u, y * u)
        when (phase) {
            DownloadPhase.IDLE -> {
                // Arrow at rest over its tray line.
                drawLine(tint, pt(12f, 4f), pt(12f, 15f), stroke, StrokeCap.Round)
                drawLine(tint, pt(6.5f, 10f), pt(12f, 15.5f), stroke, StrokeCap.Round)
                drawLine(tint, pt(17.5f, 10f), pt(12f, 15.5f), stroke, StrokeCap.Round)
                drawLine(tint, pt(5f, 20f), pt(19f, 20f), stroke, StrokeCap.Round)
            }
            DownloadPhase.ACTIVE -> {
                // Faint full ring, then the indeterminate sweep.
                drawCircle(
                    color = tint.copy(alpha = 0.25f),
                    radius = 8.5f * u,
                    center = pt(12f, 12f),
                    style = Stroke(width = stroke),
                )
                drawArc(
                    color = tint,
                    startAngle = if (reduced) -90f else spin,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = pt(3.5f, 3.5f),
                    size = Size(17f * u, 17f * u),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                // The arrow, dropped into the ring's center.
                drawLine(tint, pt(12f, 8f), pt(12f, 15f), stroke, StrokeCap.Round)
                drawLine(tint, pt(8.5f, 12f), pt(12f, 15.5f), stroke, StrokeCap.Round)
                drawLine(tint, pt(15.5f, 12f), pt(12f, 15.5f), stroke, StrokeCap.Round)
            }
            DownloadPhase.DONE -> {
                drawCircle(
                    color = tint,
                    radius = 8.5f * u,
                    center = pt(12f, 12f),
                    style = Stroke(width = stroke),
                )
                val markFrac = MotionLogic.tickMarkFraction(tickProgress.value)
                if (markFrac > 0f) {
                    val a = pt(8f, 12.3f)
                    val b = pt(11f, 15.3f)
                    val c = pt(16.5f, 9f)
                    val first = (markFrac / 0.5f).coerceAtMost(1f)
                    val second = ((markFrac - 0.5f) / 0.5f).coerceIn(0f, 1f)
                    if (first > 0f) {
                        drawLine(
                            tint, a,
                            Offset(a.x + (b.x - a.x) * first, a.y + (b.y - a.y) * first),
                            stroke, StrokeCap.Round,
                        )
                    }
                    if (second > 0f) {
                        drawLine(
                            tint, b,
                            Offset(b.x + (c.x - b.x) * second, b.y + (c.y - b.y) * second),
                            stroke, StrokeCap.Round,
                        )
                    }
                }
            }
        }
    }
}

/**
 * (f) Unlock spring icon for the Lock screen. [unlocked]
 * flips only when the lock state genuinely changes
 * (biometric success): the shackle springs up with 34%
 * overshoot and swings open around its left hinge. Under
 * reduced motion it swaps instantly.
 */
@Composable
fun UnlockSpringIcon(
    unlocked: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = SentinelGreen,
) {
    val reduced = rememberReducedMotion()
    // The spring IS the pinned pure curve: unlockLift used as
    // the easing gives the shackle exactly the spec'd 34%
    // overshoot peak before it settles (unit-pinned).
    val progress by animateFloatAsState(
        targetValue = if (unlocked) 1f else 0f,
        animationSpec = if (reduced) {
            tween(0)
        } else {
            tween(
                IconMorphLogic.UNLOCK_SPRING_MS,
                easing = androidx.compose.animation.core.Easing {
                    IconMorphLogic.unlockLift(it)
                },
            )
        },
        label = "unlockSpring",
    )
    Canvas(modifier.size(56.dp)) {
        val stroke = 3.dp.toPx()
        val u = size.minDimension / 24f
        fun pt(x: Float, y: Float) = Offset(x * u, y * u)
        // progress overshoots past 1 (to ≈1.34) mid-flight and
        // settles at 1 unlocked / 0 locked.
        val rise = 3.2f * u * progress.coerceAtLeast(0f)
        val swing = IconMorphLogic.UNLOCK_SWING_DEGREES * progress.coerceAtLeast(0f)
        // Shackle: an arch hinged at the body's left side; as
        // it unlocks it rises and swings open around that hinge.
        val hinge = pt(7.5f, 13f)
        rotate(degrees = -swing, pivot = hinge) {
            drawArc(
                color = tint,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(pt(7.5f, 5f).x, pt(7.5f, 5f).y - rise),
                size = Size(9f * u, 8f * u),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawLine(
                color = tint,
                start = Offset(hinge.x, hinge.y - rise),
                end = Offset(hinge.x, pt(7.5f, 9f).y - rise),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = tint,
                start = Offset(pt(16.5f, 13f).x, pt(16.5f, 13f).y - rise),
                end = Offset(pt(16.5f, 13f).x, pt(16.5f, 9f).y - rise),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
        // Body.
        drawRoundRect(
            color = tint,
            topLeft = pt(5f, 12.5f),
            size = Size(14f * u, 9f * u),
            cornerRadius = CornerRadius(2f * u, 2f * u),
            style = Stroke(width = stroke),
        )
        // Keyhole.
        drawCircle(color = tint, radius = 1.6f * u, center = pt(12f, 17f))
    }
}

/** How long the unlock spring plays before the shell unlocks. */
suspend fun unlockSpringSettle(reduced: Boolean) {
    if (!reduced) delay(IconMorphLogic.UNLOCK_SPRING_MS.toLong())
}

/**
 * (g) Play/pause morph icon for the voice-note player (1.4.0
 * item 10). [playing] is the player's real state: false draws
 * the play triangle; true draws the pause bars; between them
 * the triangle collapses into the left bar while both bars
 * grow from the center — one 300 ms motion, forward and
 * reverse. Under reduced motion the swap is instant. The
 * hosting control owns the tap and the content description.
 */
@Composable
fun PlayPauseMorphIcon(
    playing: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = SentinelGreen,
) {
    val reduced = rememberReducedMotion()
    val progress by animateFloatAsState(
        targetValue = if (playing) 1f else 0f,
        animationSpec = if (reduced) {
            tween(0)
        } else {
            tween(IconMorphLogic.PLAY_PAUSE_MORPH_MS)
        },
        label = "playPauseMorph",
    )
    Canvas(modifier.size(24.dp)) {
        val u = size.minDimension / 24f
        fun pt(x: Float, y: Float) = Offset(x * u, y * u)
        val p = progress
        // The triangle, collapsing toward its left edge.
        val collapse = IconMorphLogic.playCollapseFraction(p)
        if (collapse > 0.02f) {
            val anchorX = 8f
            fun cx(x: Float) = anchorX + (x - anchorX) * collapse
            val triangle = Path().apply {
                moveTo(pt(cx(8f), 5f).x, pt(cx(8f), 5f).y)
                lineTo(pt(cx(19f), 12f).x, pt(cx(19f), 12f).y)
                lineTo(pt(cx(8f), 19f).x, pt(cx(8f), 19f).y)
                close()
            }
            drawPath(triangle, color = tint.copy(alpha = collapse))
        }
        // The pause bars, growing from the vertical center.
        val bar = IconMorphLogic.pauseBarFraction(p)
        if (bar > 0.02f) {
            val half = 7f * bar
            drawRoundRect(
                color = tint.copy(alpha = bar),
                topLeft = pt(7f, 12f - half),
                size = Size(3.4f * u, 2f * half * u),
                cornerRadius = CornerRadius(1.2f * u, 1.2f * u),
            )
            drawRoundRect(
                color = tint.copy(alpha = bar),
                topLeft = pt(13.6f, 12f - half),
                size = Size(3.4f * u, 2f * half * u),
                cornerRadius = CornerRadius(1.2f * u, 1.2f * u),
            )
        }
    }
}

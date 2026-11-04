package net.sentinelsafeguard.app.ui

import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.sentinelsafeguard.app.ui.theme.SentinelGreen
import kotlin.math.PI
import kotlin.math.sin

/**
 * The estate motion layer for the app (Motion & Quality Design
 * Program, 2026-11-04 — patterns M-01/M-06/M-08/M-09/M-13 from
 * DESIGN-LANGUAGE.md, reels 1–3). Motion only: no screen's
 * structure, labels, or data change here. Every pattern honors
 * the system animator setting (reduced motion) — the same
 * Settings.Global check the splash has always used.
 *
 * The pure numbers live in [MotionLogic] so the unit suite can
 * pin them without a device; the composables below only render
 * what MotionLogic decides.
 */
object MotionLogic {
    /** M-01: pressables sink to 95% on press. */
    const val PRESS_SINK_SCALE: Float = 0.95f

    /** M-09: rows cascade at 70 ms, capped at the first 10 rows. */
    const val STAGGER_STEP_MS: Long = 70L
    const val STAGGER_CAP: Int = 10
    const val STAGGER_FADE_MS: Int = 180

    /** M-13: the error shake runs ~360 ms, peaking at 8 dp. */
    const val SHAKE_DURATION_MS: Int = 360
    const val SHAKE_PEAK_DP: Float = 8f

    /** M-06: circle-then-tick draws in 470 ms total. */
    const val TICK_DURATION_MS: Int = 470

    /** Reduced-motion fallback: a plain fade of at most 120 ms. */
    const val REDUCED_FADE_MS: Int = 120

    /** M-08: the drawn tick rests this long before the pill returns. */
    const val MORPH_HOLD_MS: Long = 1100L

    /**
     * The stagger delay for the row at [index]: rows 0..9 delay
     * 0, 70, …, 630 ms; rows at or past the cap return -1, the
     * "render instantly" sentinel, so perceived calm never
     * becomes actual delay on a long list.
     */
    fun staggerDelayMs(index: Int): Long = when {
        index < 0 -> 0L
        index < STAGGER_CAP -> index * STAGGER_STEP_MS
        else -> -1L
    }

    /**
     * M-13 shake offset in dp for [fraction] of the shake: a
     * decaying sine — three swings, each smaller, ending exactly
     * at rest. Bounded by ±[SHAKE_PEAK_DP]; 0 at both ends.
     */
    fun shakeOffsetDp(fraction: Float): Float {
        val f = fraction.coerceIn(0f, 1f)
        return (SHAKE_PEAK_DP * sin(f * PI * 3.0) * (1.0 - f)).toFloat()
    }

    /** M-06 first beat: the circle draws over the first 60%. */
    fun tickCircleFraction(progress: Float): Float =
        (progress.coerceIn(0f, 1f) / 0.6f).coerceAtMost(1f)

    /** M-06 second beat: the tick draws over the last 40%. */
    fun tickMarkFraction(progress: Float): Float =
        ((progress.coerceIn(0f, 1f) - 0.6f) / 0.4f).coerceIn(0f, 1f)
}

/**
 * True when the system animator duration scale is 0 — the
 * device's reduced-motion setting. Read once per composition
 * that asks, with the splash's exact method (SplashScreen):
 * Settings.Global.ANIMATOR_DURATION_SCALE == 0f. Every motion
 * pattern in this file consults it; a pattern with no fallback
 * does not ship.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * M-01 press-sink scale for a pressable that owns an
 * [interaction] source: 95% while pressed, springing back with
 * a gentle overshoot settle on release. Under reduced motion
 * the target never leaves 1f — the press is a plain state
 * change, no scale at all.
 */
@Composable
fun rememberPressScale(interaction: MutableInteractionSource, enabled: Boolean = true): Float {
    val reduced = rememberReducedMotion()
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed && !reduced) {
            MotionLogic.PRESS_SINK_SCALE
        } else {
            1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pressSinkScale",
    )
    return scale
}

/**
 * M-01 for rows and other non-Button pressables: the shared
 * clickable + press-sink in one modifier, ripple preserved
 * (LocalIndication), so a wired row keeps its exact Material
 * touch feedback and gains only the sink.
 */
@Composable
fun Modifier.sentinelClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, enabled)
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(
            interactionSource = interaction,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick,
        )
}

/**
 * M-13 error shake: when [trigger] changes to a new non-zero
 * value, the content shakes once (~360 ms, decaying). The error
 * TEXT beside it is untouched — the shake carries the "no,"
 * the words stay exactly as they were. Under reduced motion
 * nothing moves.
 */
@Composable
fun Modifier.errorShake(trigger: Int): Modifier {
    val reduced = rememberReducedMotion()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger != 0) {
            progress.snapTo(0f)
            if (!reduced) {
                progress.animateTo(1f, tween(MotionLogic.SHAKE_DURATION_MS))
            } else {
                progress.snapTo(1f)
            }
        }
    }
    return this.graphicsLayer {
        translationX = MotionLogic.shakeOffsetDp(progress.value) * density
    }
}

/**
 * M-09 list entrance for the row at [index]: a short fade +
 * settle, delayed by the capped 70 ms stagger. Rows past the
 * cap and every row under reduced motion render instantly.
 */
@Composable
fun Modifier.listEntrance(index: Int): Modifier {
    val reduced = rememberReducedMotion()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        val delayMs = MotionLogic.staggerDelayMs(index)
        if (reduced || delayMs < 0L) {
            progress.snapTo(1f)
        } else {
            if (delayMs > 0L) delay(delayMs)
            progress.animateTo(1f, tween(MotionLogic.STAGGER_FADE_MS))
        }
    }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 12f * density
    }
}

/**
 * M-06 drawn success tick: the circle stroke draws first, then
 * the tick, 470 ms total, in [color] on whatever surface hosts
 * it. Reserved for high-trust completions. Under reduced
 * motion the finished tick simply appears.
 */
@Composable
fun DrawnTick(modifier: Modifier = Modifier, color: Color = SentinelGreen) {
    val reduced = rememberReducedMotion()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (reduced) {
            progress.snapTo(1f)
        } else {
            progress.animateTo(1f, tween(MotionLogic.TICK_DURATION_MS))
        }
    }
    Canvas(modifier) {
        val stroke = 2.5.dp.toPx()
        val radius = size.minDimension / 2f - stroke
        val center = Offset(size.width / 2f, size.height / 2f)
        val circleFrac = MotionLogic.tickCircleFraction(progress.value)
        if (circleFrac > 0f) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * circleFrac,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        val markFrac = MotionLogic.tickMarkFraction(progress.value)
        if (markFrac > 0f) {
            val a = Offset(size.width * 0.30f, size.height * 0.53f)
            val b = Offset(size.width * 0.45f, size.height * 0.68f)
            val c = Offset(size.width * 0.72f, size.height * 0.34f)
            val first = (markFrac / 0.5f).coerceAtMost(1f)
            val second = ((markFrac - 0.5f) / 0.5f).coerceIn(0f, 1f)
            if (first > 0f) {
                drawLine(
                    color = color,
                    start = a,
                    end = Offset(a.x + (b.x - a.x) * first, a.y + (b.y - a.y) * first),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
            if (second > 0f) {
                drawLine(
                    color = color,
                    start = b,
                    end = Offset(b.x + (c.x - b.x) * second, b.y + (c.y - b.y) * second),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

/**
 * M-08 submit morph: one shape carries idle → working → done.
 * The pill shows its label; on tap it shrinks to a spinner;
 * on success it resolves into the M-06 drawn tick (in the
 * button's own content color), rests, then returns to the
 * label. On failure it returns straight to the label — the
 * caller's error line carries what happened, unchanged.
 * [onSubmit] returns true on success. Taps while working or
 * celebrating are ignored, never queued.
 */
@Composable
fun MorphSubmitButton(
    text: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onSubmit: suspend () -> Boolean,
) {
    val reduced = rememberReducedMotion()
    val scope = rememberCoroutineScope()
    // 0 idle · 1 working · 2 done
    var phase by remember { mutableStateOf(0) }
    Button(
        onClick = {
            if (phase == 0) {
                phase = 1
                scope.launch {
                    val ok = try {
                        onSubmit()
                    } catch (e: Exception) {
                        false
                    }
                    if (ok) {
                        phase = 2
                        delay(if (reduced) 400L else MotionLogic.MORPH_HOLD_MS)
                    }
                    phase = 0
                }
            }
        },
        enabled = enabled,
        modifier = modifier.animateContentSize(tween(200)),
    ) {
        when (phase) {
            1 -> CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
            2 -> DrawnTick(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
            )
            else -> Text(text)
        }
    }
}

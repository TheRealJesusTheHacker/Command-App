package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import net.sentinelsafeguard.app.ui.theme.SentinelGreen
import net.sentinelsafeguard.app.ui.theme.SentinelMuted
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min

/**
 * COMMAND PULSE (Command App 1.7.5) — the app side of Live's
 * queue-pulse field (COMMAND-PULSE-2026-11-13, binding relay).
 * GET /api/app/today/panels carries a top-level `pulse` object
 * computed by the server's single helper _queue_pulse — the
 * same object the web Today renders. The app renders the
 * server bpm VERBATIM: the curve is never recomputed here,
 * the caption is built from the same payload counts, and when
 * the counts cannot load (load failure, or an older server
 * with no pulse field) the trace renders flat with an honest
 * unavailable state — never an invented rhythm.
 *
 * The pure decisions live in [PulseLogic] so the unit suite
 * can pin them without a device; the composables below only
 * render what PulseLogic decides.
 */
data class QueuePulse(
    val open: Int,
    val normal: Int,
    val high: Int,
    val urgent: Int,
    val pressure: Int,
    val bpm: Int,
)

object PulseLogic {

    /** LIVE animates at the server bpm; FLAT is the
     *  reduced-motion rendering (flat line + numeric readout);
     *  UNAVAILABLE is the honest no-counts state. */
    enum class Mode { LIVE, FLAT, UNAVAILABLE }

    /** Parse the top-level `pulse` object off the panels
     *  response. All six server integers are required — a
     *  partial object is not a pulse, and the app never fills
     *  a missing count in. Null when absent (older server) or
     *  malformed. */
    fun parse(panelsResponse: JsonObject?): QueuePulse? {
        val p = Phase3Logic.obj(panelsResponse, "pulse") ?: return null
        return QueuePulse(
            open = strictInt(p, "open") ?: return null,
            normal = strictInt(p, "normal") ?: return null,
            high = strictInt(p, "high") ?: return null,
            urgent = strictInt(p, "urgent") ?: return null,
            pressure = strictInt(p, "pressure") ?: return null,
            bpm = strictInt(p, "bpm") ?: return null,
        )
    }

    /** A count is a JSON number, never a quoted string:
     *  kotlinx's intOrNull would happily parse the string
     *  "6" as 6 (the same trap the W-5 key-status parse was
     *  tightened against), so a string primitive is rejected
     *  before it can be read as a number. */
    private fun strictInt(obj: JsonObject, key: String): Int? {
        val prim = obj[key] as? JsonPrimitive ?: return null
        if (prim.isString) return null
        return prim.intOrNull
    }

    /** The owner's caption, built from the payload's own
     *  counts: the payload's `urgent` IS the Critical count,
     *  read as urgent per the spec. */
    fun caption(pulse: QueuePulse): String =
        "Queue pulse · ${pulse.open} open · ${pulse.urgent} urgent"

    /** The numeric readout (the web block's own format). */
    fun readout(pulse: QueuePulse): String =
        "${pulse.bpm} bpm · pressure ${pulse.pressure}"

    fun mode(pulse: QueuePulse?, reducedMotion: Boolean): Mode = when {
        pulse == null -> Mode.UNAVAILABLE
        reducedMotion -> Mode.FLAT
        else -> Mode.LIVE
    }

    /** One beat's length at the server bpm. */
    fun beatPeriodMs(bpm: Int): Double =
        if (bpm > 0) 60_000.0 / bpm else 1_250.0

    /** Beats visible across the trace's width — at any bpm the
     *  same number of beats is on screen, so TEMPO reads as
     *  scroll speed (the web block's signal: amplitude steady,
     *  tempo carries the state). */
    const val WINDOW_BEATS: Double = 2.5

    /**
     * The ECG waveform: normalized amplitude for a position
     * [phase] within one beat (0..1, wrapping). A quiet
     * baseline, a small P bump, the sharp QRS spike, a T bump
     * — gaussian parts summed, so the curve is smooth at any
     * sample density and exactly periodic (amplitude at phase
     * p equals amplitude at p+1). Amplitude is steady by
     * construction: nothing here reads the queue counts.
     */
    fun waveAmplitude(phase: Double): Double {
        val ph = phase - floor(phase)
        fun bump(center: Double, width: Double, height: Double): Double {
            var d = abs(ph - center)
            d = min(d, 1.0 - d)
            return height * exp(-(d * d) / (2.0 * width * width))
        }
        // Signs ride the heights themselves so the sum reads
        // exactly as the trace draws: P up, Q down, R up,
        // S down, T up.
        return bump(0.14, 0.030, 0.14) + // P wave
            bump(0.285, 0.011, -0.10) + // Q dip
            bump(0.320, 0.013, 1.00) + // R spike
            bump(0.355, 0.011, -0.20) + // S dip
            bump(0.560, 0.045, 0.24) // T wave
    }
}

/**
 * The pulse block on Today: the living ECG line, its truth
 * caption, and the numeric readout. Rides the panels payload
 * Today already loads — no new polling. Under reduced motion
 * the line is flat and the readout still shows (family motion
 * law); with no counts it is flat and says so.
 */
@Composable
fun QueuePulseCard(
    pulse: QueuePulse?,
    loadFailed: Boolean,
    onRetry: () -> Unit,
) {
    val reduced = rememberReducedMotion()
    val mode = PulseLogic.mode(pulse, reduced)
    MirrorCard("Queue pulse", topTier = true) {
        PulseTrace(bpm = if (mode == PulseLogic.Mode.LIVE) pulse?.bpm else null)
        Spacer(Modifier.height(6.dp))
        when (mode) {
            PulseLogic.Mode.UNAVAILABLE -> Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Queue pulse unavailable — the queue counts " +
                        "could not be loaded.",
                    color = SentinelMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                if (loadFailed) {
                    TextButton(onClick = onRetry) { Text("Try again") }
                }
            }
            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    PulseLogic.caption(pulse!!),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    PulseLogic.readout(pulse),
                    color = SentinelMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/**
 * The trace itself. [bpm] null (unavailable) or reduced
 * motion → a flat baseline: no rhythm is ever drawn without
 * the server's number behind it. Live: a frame clock scrolls
 * the waveform left at the server bpm, drawn twice (a wide,
 * faint pass under a crisp pass) for the estate's depth.
 */
@Composable
private fun PulseTrace(bpm: Int?) {
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var originMs by remember { mutableLongStateOf(-1L) }
    LaunchedEffect(bpm) {
        if (bpm != null) {
            originMs = -1L
            while (true) {
                withFrameMillis { frame ->
                    if (originMs < 0L) originMs = frame
                    elapsedMs = frame - originMs
                }
            }
        }
    }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .padding(vertical = 2.dp),
    ) {
        val w = size.width
        val h = size.height
        val midY = h / 2f
        val stroke = 2.dp.toPx()
        if (bpm == null) {
            drawLine(
                color = SentinelGreen,
                start = Offset(0f, midY),
                end = Offset(w, midY),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            return@Canvas
        }
        val period = PulseLogic.beatPeriodMs(bpm)
        val windowMs = period * PulseLogic.WINDOW_BEATS
        val ampPx = h * 0.40f
        val path = Path()
        val steps = (w / 3f).toInt().coerceAtLeast(2)
        for (i in 0..steps) {
            val x = w * i / steps
            val t = elapsedMs - (w - x) / w * windowMs
            val phase = (((t % period) + period) % period) / period
            val y = midY - (PulseLogic.waveAmplitude(phase) * ampPx).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path,
            color = SentinelGreen.copy(alpha = 0.16f),
            style = Stroke(
                width = stroke * 3f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
        drawPath(
            path,
            color = SentinelGreen,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

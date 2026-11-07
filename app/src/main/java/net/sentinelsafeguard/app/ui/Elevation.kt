package net.sentinelsafeguard.app.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.sentinelsafeguard.app.ui.theme.SentinelGreen
import net.sentinelsafeguard.app.ui.theme.SentinelGreenDim
import net.sentinelsafeguard.app.ui.theme.SentinelMuted
import net.sentinelsafeguard.app.ui.theme.SentinelOutline
import net.sentinelsafeguard.app.ui.theme.SentinelSurface
import net.sentinelsafeguard.app.ui.theme.SentinelSurfaceHigh
import net.sentinelsafeguard.app.ui.theme.SentinelSurfaceTop
import kotlin.math.exp

/**
 * Lane B (1.4.0) shared elevation kit — the "not flat" layer.
 *
 * Design correction (Skyler, 2026-11-07, governing): "We
 * definitely don't want a flat app. We want alive, premium,
 * professional, sentinel, stark industries, Microsoft, Meta."
 * This file is the app-wide kit that carries it: layered
 * Sentinel cards (Emerald Depth gradient + 1dp edge light +
 * top glow), glow dividers, shimmer skeletons, a press-spring
 * modifier (reusing Motion.kt's M-01 numbers — not duplicating
 * them), the primary-action haptic, the M-02 pull-damping
 * logic + indicator, and the tool-deck pattern generalized
 * from the OSINT deck (grouped Sentinel cards that break an
 * area's distinct tools out as distinct reachable cards).
 *
 * Palette discipline: near-black + emerald/green only. Zero
 * purple anywhere. Reduced motion is honored through
 * [rememberReducedMotion] exactly as Motion.kt does.
 * Pure numbers live in [ElevationLogic] / [DampingLogic] so
 * the unit suite can pin them without a device.
 */
object EmeraldGradient {
    /** Card fill: [top] easing ~35% toward black at the base. */
    fun cardBrush(top: Color = SentinelSurface): Brush =
        Brush.verticalGradient(listOf(top, lerp(top, Color.Black, 0.35f)))

    /** Top-tier card fill (the primary door on a screen). */
    fun topCardBrush(): Brush = cardBrush(SentinelSurfaceTop)

    /** Edge light: emerald at 45% easing into the flat outline. */
    fun edgeBrush(): Brush =
        Brush.horizontalGradient(listOf(SentinelGreen.copy(alpha = 0.45f), SentinelOutline))

    /** Primary-action fill: green → green-dim, vertical. */
    fun primaryBrush(enabled: Boolean = true): Brush =
        Brush.verticalGradient(
            listOf(
                SentinelGreen.copy(alpha = if (enabled) 1f else 0.38f),
                SentinelGreenDim.copy(alpha = if (enabled) 1f else 0.38f),
            ),
        )

    /** A soft emerald glow band (divider / separator light). */
    fun glowBrush(): Brush =
        Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                SentinelGreen.copy(alpha = 0.35f),
                Color.Transparent,
            ),
        )
}

/** Pure elevation numbers, pinned by ElevationLogicTest. */
object ElevationLogic {
    /** Shimmer skeleton alpha travel: 0.35 → 0.80 (the shared
     *  skeleton pulse's own range, kept in step with Common). */
    const val SHIMMER_MIN_ALPHA: Float = 0.35f
    const val SHIMMER_MAX_ALPHA: Float = 0.80f

    /** Top glow (white edge light) alpha on a SentinelCard. */
    const val TOP_GLOW_ALPHA: Float = 0.04f

    /** Deck / card entrance stagger: 60 ms steps, cap 8. */
    const val DECK_STAGGER_STEP_MS: Long = 60L
    const val DECK_STAGGER_CAP: Int = 8

    /** Shimmer alpha for a 0..1 pulse [phase] (triangle wave
     *  handled by the caller; this is the linear leg). */
    fun shimmerAlpha(phase: Float): Float {
        val p = phase.coerceIn(0f, 1f)
        return SHIMMER_MIN_ALPHA + (SHIMMER_MAX_ALPHA - SHIMMER_MIN_ALPHA) * p
    }

    /** Stagger delay for deck entry [index]; -1 past the cap
     *  is the "render instantly" sentinel (MotionLogic's M-09
     *  convention, with the deck's own step/cap). */
    fun deckStaggerDelayMs(index: Int): Long = when {
        index < 0 -> 0L
        index < DECK_STAGGER_CAP -> index * DECK_STAGGER_STEP_MS
        else -> -1L
    }

    /** Glow strength for an elevation in dp: a saturating
     *  curve — 0 at rest, ~0.63 of full at 4dp, capped at 1. */
    fun glowAlpha(elevationDp: Float): Float {
        val e = elevationDp.coerceAtLeast(0f)
        return (1.0 - exp(-0.25 * e.toDouble())).toFloat().coerceIn(0f, 1f)
    }
}

/**
 * M-02 pull damping (Lane B item 3): rubber-band resistance
 * for pull-to-refresh. The damped offset follows
 * `max · (1 − exp(−RESISTANCE · raw / max))` — its slope at
 * the origin is exactly [RESISTANCE] (the pull initially
 * answers at 35% of the finger's travel) and it asymptotes
 * to [max] instead of running away. Pure arithmetic, pinned
 * by DampingLogicTest.
 */
object DampingLogic {
    /** Target resistance at the start of the pull: 35%. */
    const val RESISTANCE: Float = 0.35f

    /** Damped offset in px for a raw pull of [rawPx] against
     *  a cap of [maxPx]. Non-positive inputs rest at 0. */
    fun dampedOffsetPx(rawPx: Float, maxPx: Float): Float {
        if (rawPx <= 0f || maxPx <= 0f) return 0f
        return (maxPx * (1.0 - exp(-RESISTANCE.toDouble() * rawPx / maxPx))).toFloat()
    }

    /**
     * The same curve normalized as a 0..1 fraction of the
     * pull target: 0 at rest, exactly 1 at fraction 1, and
     * clamped to 1 beyond it (the indicator never overfills).
     */
    fun dampedFraction(fraction: Float): Float {
        if (fraction <= 0f) return 0f
        val atTarget = 1.0 - exp(-RESISTANCE.toDouble())
        val value = (1.0 - exp(-RESISTANCE.toDouble() * fraction)) / atTarget
        return value.toFloat().coerceIn(0f, 1f)
    }
}

/**
 * The layered Sentinel card: Emerald Depth gradient fill,
 * a 1dp edge-light border (gradient hairline on the top tier,
 * flat outline below it), card elevation, and the 1dp top
 * glow inside. Content, labels, and actions are the caller's
 * — this is depth only, the hunt18 MirrorCard discipline
 * generalized into one shared surface.
 */
@Composable
fun SentinelCard(
    modifier: Modifier = Modifier,
    topTier: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            1.dp,
            if (topTier) EmeraldGradient.edgeBrush() else Brush.linearGradient(
                listOf(SentinelOutline, SentinelOutline),
            ),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (topTier) 4.dp else 2.dp),
        modifier = modifier,
    ) {
        Box(
            Modifier.background(
                if (topTier) EmeraldGradient.topCardBrush() else EmeraldGradient.cardBrush(),
            ),
        ) {
            Column(Modifier.padding(16.dp), content = content)
            Box(Modifier.align(Alignment.TopCenter)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = ElevationLogic.TOP_GLOW_ALPHA)),
                )
            }
        }
    }
}

/** A divider that carries light: a soft emerald glow band
 *  over a 1dp core line, instead of a flat rule. */
@Composable
fun GlowDivider(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(EmeraldGradient.glowBrush()),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SentinelOutline),
        )
    }
}

/**
 * Shimmer skeleton rows in the SentinelCard shape: the shared
 * skeleton pulse (Common's LocalSkeletonAlpha, installed once
 * by SkeletonPulseHost) drives the alpha, so a loading screen
 * shimmers in one rhythm app-wide; outside a host — and under
 * reduced motion, where the host's consumers rest — the alpha
 * sits at the static mid-pulse value the pulse local defaults
 * to. No per-card infinite transitions are started here.
 */
@Composable
fun ShimmerSkeleton(rows: Int = 3, modifier: Modifier = Modifier) {
    val alpha = LocalSkeletonAlpha.current
    Column(modifier) {
        repeat(rows) {
            SentinelCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth(0.55f)
                        .height(16.dp)
                        .graphicsLayer { this.alpha = alpha }
                        .background(SentinelSurfaceHigh, RoundedCornerShape(10.dp)),
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.8f)
                        .height(12.dp)
                        .graphicsLayer { this.alpha = alpha }
                        .background(SentinelSurfaceHigh, RoundedCornerShape(10.dp)),
                )
            }
        }
    }
}

/**
 * M-01 press-spring as a standalone modifier for surfaces
 * that own their own click handling: the scale half of
 * Motion.kt's sentinelClickable, reusing its pinned 95%
 * sink + spring — no second set of numbers is introduced.
 */
@Composable
fun Modifier.pressSpring(
    interaction: MutableInteractionSource,
    enabled: Boolean = true,
): Modifier {
    val scale = rememberPressScale(interaction, enabled)
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * The primary-action haptic (Lane B item 6): a confirming
 * thock for a primary submit / send, stronger than the light
 * navigation tick (Common's rememberHapticTick, unchanged).
 * Uses HapticFeedbackConstants.CONFIRM on API 30+ and falls
 * back to LONG_PRESS below it, always inside try/catch —
 * haptics are garnish and must never break the action.
 */
@Composable
fun rememberPrimaryHaptic(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        {
            try {
                if (Build.VERSION.SDK_INT >= 30) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                } else {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
            } catch (ignored: Exception) {
                // A device without a vibrator path must never
                // break the interaction this garnishes.
            }
        }
    }
}

/**
 * M-02 indicator: the stock PullToRefresh indicator rendered
 * through [DampingLogic] — its alpha (and, while the user is
 * still pulling, a slight settle) follows the damped curve
 * instead of the raw fraction, so the indicator itself feels
 * rubber-banded. The refresh state machine stays the stock
 * PullToRefreshBox's; nothing about when refresh fires
 * changes. Device verification of the feel is still owed —
 * see the lane report.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DampedPullIndicator(
    state: androidx.compose.material3.pulltorefresh.PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val damped = DampingLogic.dampedFraction(state.distanceFraction)
    val alpha by animateFloatAsState(
        targetValue = if (isRefreshing) 1f else damped.coerceAtLeast(0.25f),
        animationSpec = tween(120),
        label = "dampedPullAlpha",
    )
    androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator(
        state = state,
        isRefreshing = isRefreshing,
        modifier = modifier.graphicsLayer { this.alpha = alpha },
    )
}

/** One tool on an area home's deck: a label that already
 *  exists on that screen, its selected state, and the REAL
 *  action it performs (a mode switch or a navigation that
 *  already exists in the file). No deck entry is decorative. */
class ToolDeckEntry(
    val label: String,
    val selected: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * The tool deck (scope generalization, 2026-11-07 — the
 * OSINT deck pattern generalized): grouped SentinelCards at
 * the top of an area home that break the area's distinct
 * tools out as distinct reachable cards instead of one flat
 * chip row flattening them together. Every entry performs
 * its action on tap (mode switch or existing navigation),
 * with the M-01 press sink, the light haptic tick, and the
 * capped stagger entrance; the selected tool carries the
 * top-tier fill + edge light. Two per row, so every tool is
 * at most one tap from the area home itself.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolDeck(entries: List<ToolDeckEntry>, modifier: Modifier = Modifier) {
    val tick = rememberHapticTick()
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 2,
    ) {
        entries.forEachIndexed { index, entry ->
            val interaction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .weight(1f)
                    .listEntrance(index)
                    .pressSpring(interaction),
            ) {
                // A clickable SentinelCard: Card's own onClick
                // keeps the ripple inside the card shape.
                Card(
                    onClick = { tick(); entry.onClick() },
                    interactionSource = interaction,
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(
                        1.dp,
                        if (entry.selected) EmeraldGradient.edgeBrush() else Brush.linearGradient(
                            listOf(SentinelOutline, SentinelOutline),
                        ),
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (entry.selected) 4.dp else 2.dp,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        Modifier.background(
                            if (entry.selected) {
                                EmeraldGradient.topCardBrush()
                            } else {
                                EmeraldGradient.cardBrush()
                            },
                        ),
                    ) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (entry.selected) {
                                    SectionTick()
                                    Box(Modifier.padding(start = 8.dp)) {
                                        Text(
                                            entry.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                } else {
                                    Text(
                                        entry.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SentinelMuted,
                                    )
                                }
                            }
                        }
                        Box(Modifier.align(Alignment.TopCenter)) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(
                                        Color.White.copy(alpha = ElevationLogic.TOP_GLOW_ALPHA),
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

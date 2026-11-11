package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.sentinelsafeguard.app.R
import net.sentinelsafeguard.app.ui.theme.SentinelAmber
import net.sentinelsafeguard.app.ui.theme.SentinelGreen
import net.sentinelsafeguard.app.ui.theme.SentinelMuted
import net.sentinelsafeguard.app.ui.theme.SentinelOutline
import net.sentinelsafeguard.app.ui.theme.SentinelSurface
import net.sentinelsafeguard.app.ui.theme.SentinelSurfaceHigh
import net.sentinelsafeguard.app.ui.theme.SentinelSurfaceTop

/** Shared building blocks for the mirrored Command screens:
 *  the web stat cards, titled cards, chips, and steppers. */

/**
 * hunt18 "Emerald Depth" card fill: a subtle vertical gradient,
 * the surface at top easing ~35% toward black at the bottom edge,
 * so a flat panel reads as a machined plate instead of paper.
 */
fun sentinelCardBrush(top: Color = SentinelSurface): Brush =
    Brush.verticalGradient(listOf(top, lerp(top, Color.Black, 0.35f)))

/** The hunt18 gradient hairline: emerald at 45% easing into the
 *  flat outline — carried by stat cards and top-tier cards. */
fun gradientHairlineBrush(): Brush =
    Brush.horizontalGradient(listOf(SentinelGreen.copy(alpha = 0.45f), SentinelOutline))

/** The 1dp inner top highlight (white at 4% alpha) that pairs
 *  with the card gradient. Drawn inside a card's Box, aligned
 *  to its top edge. */
@Composable
fun CardTopHighlight() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.04f)),
    )
}

/** The 3dp × 14dp emerald tick at the left edge of a section or
 *  card title row (hunt18) — pure decoration, no added copy. */
@Composable
fun SectionTick() {
    Box(
        Modifier
            .width(3.dp)
            .height(14.dp)
            .background(SentinelGreen, RoundedCornerShape(2.dp)),
    )
}

/** A web crmstat card: big number over its exact Command label.
 *  hunt18: display-numeral face (32sp tabular, shrinking for long
 *  values), gradient fill + hairline, 16dp padding, and the 16dp
 *  corner glyph at 60% emerald. Warning values the data already
 *  flags (an "Overdue …" stat above zero) render in the warning
 *  amber — the only place amber may appear. */
@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    val warning = label.startsWith("Overdue") &&
        value !in setOf("0", "$0", "$0.00", "—", "")
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, gradientHairlineBrush()),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.padding(4.dp),
    ) {
        Box(Modifier.background(sentinelCardBrush())) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    val valueStyle = when {
                        value.length <= 6 -> MaterialTheme.typography.displaySmall
                        value.length <= 10 ->
                            MaterialTheme.typography.displaySmall.copy(fontSize = 24.sp)
                        else ->
                            MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp)
                    }
                    Text(
                        value,
                        style = valueStyle,
                        color = if (warning) SentinelAmber else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_stat_glyph),
                        contentDescription = null,
                        tint = (if (warning) SentinelAmber else SentinelGreen).copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(label, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
            }
            Box(Modifier.align(Alignment.TopCenter)) { CardTopHighlight() }
        }
    }
}

/** A titled card in web's shape: "<title> <count>" header + content.
 *  hunt18: gradient fill + top highlight, 16dp radius and inner
 *  padding, the emerald tick beside the title; [topTier] swaps the
 *  fill to the SurfaceTop tier with a gradient hairline (the
 *  primary door on a screen — Today's pinned top card). */
@Composable
fun MirrorCard(
    title: String,
    count: Int? = null,
    footerLabel: String? = null,
    onFooter: (() -> Unit)? = null,
    topTier: Boolean = false,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = if (topTier) {
            BorderStroke(1.dp, gradientHairlineBrush())
        } else {
            BorderStroke(1.dp, SentinelOutline)
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Box(
            Modifier.background(
                sentinelCardBrush(if (topTier) SentinelSurfaceTop else SentinelSurface),
            ),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTick()
                    Spacer(Modifier.width(8.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (count != null) {
                        Text(
                            count.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                content()
                if (footerLabel != null && onFooter != null) {
                    TextButton(onClick = onFooter) { Text(footerLabel) }
                }
            }
            Box(Modifier.align(Alignment.TopCenter)) { CardTopHighlight() }
        }
    }
}

/** A small static chip (priority / stage / source / flags).
 *  hunt18: renders as a filled pill (10dp radius). */
@Composable
fun MirrorChip(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SentinelSurfaceHigh),
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * A filled status pill (hunt18): the status string the screen
 * already renders, carried as a pill — tint fill, outline, amber
 * or muted by [kind]. The wording never changes; only the shape.
 */
@Composable
fun StatusPill(text: String, kind: String) {
    val (fill, content, border) = when (kind) {
        "paid" -> Triple(SentinelGreen.copy(alpha = 0.16f), SentinelGreen, null)
        "amber" -> Triple(SentinelAmber.copy(alpha = 0.14f), SentinelAmber, null)
        "muted" -> Triple(SentinelSurfaceHigh, SentinelMuted, null)
        else -> Triple(Color.Transparent, MaterialTheme.colorScheme.primary, SentinelOutline)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = fill),
        shape = RoundedCornerShape(10.dp),
        border = border?.let { BorderStroke(1.dp, it) },
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            color = content,
        )
    }
}

/**
 * The one gradient-filled button in the app (hunt18), reserved
 * for primary actions: green → green-dim vertical, black text.
 * Same label, same action as the flat Button it replaces.
 */
@Composable
fun GradientPrimaryButton(
    text: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    // Motion pass (2026-11-04): M-01 press-sink — the button
    // sinks to 95% on press and springs back, riding its own
    // interaction source; label, gradient, and action unchanged.
    val pressInteraction = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(pressInteraction, enabled)
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = pressInteraction,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color(0xFF031007),
            disabledContainerColor = Color.Transparent,
        ),
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .background(
            Brush.verticalGradient(
                listOf(
                    net.sentinelsafeguard.app.ui.theme.SentinelGreen
                        .copy(alpha = if (enabled) 1f else 0.38f),
                    net.sentinelsafeguard.app.ui.theme.SentinelGreenDim
                        .copy(alpha = if (enabled) 1f else 0.38f),
                ),
            ),
            RoundedCornerShape(10.dp),
        ),
    ) {
        Text(text)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipRow(
    options: List<Pair<String, String>>,
    selected: String?,
    onPick: (String?) -> Unit,
) {
    // Lane B (1.4.0) M-14: a sliding pill rides behind the
    // selected chip. Chip positions are measured with
    // onGloballyPositioned (relative to this row's box, which
    // the FlowRow also fills from the same origin) and the
    // pill's offset + size spring between them. The selected
    // chip's own container is transparent so the pill shows
    // through; labels, picks, and the haptic tick are exactly
    // as before. Under reduced motion the pill snaps.
    val reduced = rememberReducedMotion()
    val tick = rememberHapticTick()
    var layouts by remember {
        mutableStateOf<Map<String, Pair<androidx.compose.ui.unit.IntOffset, androidx.compose.ui.unit.IntSize>>>(emptyMap())
    }
    // Chip offsets come from LayoutCoordinates.positionInParent()
    // (the extension import at the top of this file) — the chip's
    // offset inside the FlowRow, the same origin the pill is laid
    // out from. The Pair is constructed with explicit types: the
    // bare infix form let the compiler widen the offset to Any
    // and the layouts map type would not unify (CI-verified).
    val targetLayout = selected?.let { layouts[it] }
    val pillSpec = if (reduced) {
        androidx.compose.animation.core.tween<androidx.compose.ui.unit.IntOffset>(0)
    } else {
        androidx.compose.animation.core.spring<androidx.compose.ui.unit.IntOffset>(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
        )
    }
    val pillSizeSpec = if (reduced) {
        androidx.compose.animation.core.tween<androidx.compose.ui.unit.IntSize>(0)
    } else {
        androidx.compose.animation.core.spring<androidx.compose.ui.unit.IntSize>(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
        )
    }
    val pillOffset by androidx.compose.animation.core.animateIntOffsetAsState(
        targetValue = targetLayout?.first ?: androidx.compose.ui.unit.IntOffset.Zero,
        animationSpec = pillSpec,
        label = "chipPillOffset",
    )
    val pillSize by androidx.compose.animation.core.animateIntSizeAsState(
        targetValue = targetLayout?.second ?: androidx.compose.ui.unit.IntSize.Zero,
        animationSpec = pillSizeSpec,
        label = "chipPillSize",
    )
    val density = androidx.compose.ui.platform.LocalDensity.current
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        if (targetLayout != null) {
            Box(
                Modifier
                    .offset { pillOffset }
                    .size(
                        width = with(density) { pillSize.width.toDp() },
                        height = with(density) { pillSize.height.toDp() },
                    )
                    .background(SentinelGreen.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                    .border(
                        1.dp,
                        SentinelGreen.copy(alpha = 0.45f),
                        RoundedCornerShape(8.dp),
                    ),
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { (key, label) ->
                FilterChip(
                    selected = selected == key,
                    onClick = { tick(); onPick(if (key == "all") null else key) },
                    label = { Text(label) },
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.Transparent,
                    ),
                    modifier = Modifier.onGloballyPositioned { coords ->
                        // positionInParent() returns geometry.Offset
                        // (float) in this Compose version — convert.
                        val pos = coords.positionInParent()
                        val entry: Pair<androidx.compose.ui.unit.IntOffset, androidx.compose.ui.unit.IntSize> =
                            Pair(
                                androidx.compose.ui.unit.IntOffset(
                                    pos.x.toInt(), pos.y.toInt(),
                                ),
                                coords.size,
                            )
                        if (layouts[key] != entry) {
                            layouts = layouts + (key to entry)
                        }
                    },
                )
            }
        }
    }
}

/** The ‹ › stage steppers web cards carry next to Open. */
@Composable
fun StageStepper(
    current: String,
    onStep: (String) -> Unit,
    onOpen: () -> Unit,
    openLabel: String,
) {
    val tick = rememberHapticTick()
    Row(verticalAlignment = Alignment.CenterVertically) {
        val back = MirrorVocab.stepStage(current, -1)
        val fwd = MirrorVocab.stepStage(current, +1)
        // AUDIT-171 L-03: the glyph-only steppers got real
        // accessibility names — a screen reader used to voice
        // the punctuation, not the action.
        TextButton(
            onClick = { back?.let { tick(); onStep(it) } },
            enabled = back != null,
            modifier = Modifier.semantics { contentDescription = "Previous stage" },
        ) { Text("‹") }
        TextButton(onClick = onOpen) { Text(openLabel) }
        TextButton(
            onClick = { fwd?.let { tick(); onStep(it) } },
            enabled = fwd != null,
            modifier = Modifier.semantics { contentDescription = "Next stage" },
        ) { Text("›") }
        Spacer(Modifier.width(4.dp))
    }
}

/** A screen header exactly as web: <h1> title + subtitle + actions. */
@Composable
fun MirrorHeader(
    title: String,
    subtitle: String? = null,
    actions: @Composable () -> Unit = {},
) {
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            actions()
        }
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = SentinelMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** A labeled select — the mirrored form control for web's <select>. */
@Composable
fun ChoiceMenu(
    label: String,
    value: String,
    options: List<String>,
    onPick: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val tick = rememberHapticTick()
    Column {
        Text(label, color = SentinelMuted, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { open = true }) {
            Text(value.replaceFirstChar { it.uppercase() })
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.replaceFirstChar { it.uppercase() }) },
                    onClick = { tick(); onPick(option); open = false },
                )
            }
        }
    }
}

/** Website tickets show their source badge in plain words. */
fun sourceLabel(source: String): String = when (source.lowercase()) {
    "website", "report-issue", "report_issue" -> "From the website"
    "contact", "contact-form" -> "From the contact form"
    "email" -> "From email"
    "manual" -> "Added by hand"
    else -> source.replaceFirstChar { it.uppercase() }
}

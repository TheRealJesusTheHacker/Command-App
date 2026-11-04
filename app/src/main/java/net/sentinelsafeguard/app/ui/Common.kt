package net.sentinelsafeguard.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.Dp
import net.sentinelsafeguard.app.R
import net.sentinelsafeguard.app.ui.theme.SentinelGreen
import net.sentinelsafeguard.app.ui.theme.SentinelSurfaceLow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.sentinelsafeguard.app.data.api.ApiException
import net.sentinelsafeguard.app.data.api.SignedOutException
import net.sentinelsafeguard.app.ui.theme.SentinelDanger
import net.sentinelsafeguard.app.ui.theme.SentinelMuted
import net.sentinelsafeguard.app.ui.theme.SentinelOutline
import net.sentinelsafeguard.app.ui.theme.SentinelSurface
import net.sentinelsafeguard.app.ui.theme.SentinelSurfaceHigh
import net.sentinelsafeguard.app.util.Diagnostics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Error dignity (hunt4 premium pass): the owner is never shown raw
 * technical text — no parser offsets, no class names, no stack
 * fragments. The failure is logged to the internal diagnostics log
 * and the screen gets [fallback] in plain language. The one safe
 * exception is the API layer's own messages, which are written for
 * the owner by construction (ApiException / SignedOutException).
 */
fun plainError(e: Throwable, fallback: String): String {
    Diagnostics.log("ui", e)
    return when (e) {
        is ApiException -> e.message
        is SignedOutException -> e.message ?: fallback
        else -> fallback
    }
}

/**
 * The ONE skeleton pulse for the whole app (hunt17, Polish
 * Pass 3). Before hunt17 every SkeletonList / SkeletonLines
 * instance ran its OWN infinite transition — a Today screen with
 * a dozen loading cards recomposed a dozen subtrees every frame
 * for the whole load. [SkeletonPulseHost] (installed once in
 * AppRoot) runs a single transition and shares its alpha through
 * this local; every skeleton reads the same value, so one frame
 * recomposes the pulsing boxes once, not once per card. Outside
 * a host the alpha rests at a static mid-pulse value.
 */
val LocalSkeletonAlpha = androidx.compose.runtime.compositionLocalOf { 0.6f }

/** Installs the shared skeleton pulse above [content]. */
@Composable
fun SkeletonPulseHost(content: @Composable () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "skeletonShared")
    val alpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeletonSharedAlpha",
    )
    androidx.compose.runtime.CompositionLocalProvider(
        LocalSkeletonAlpha provides alpha,
        content = content,
    )
}

/**
 * Loading placeholder in the shape of the list that is coming
 * (hunt4 premium pass): softly pulsing card silhouettes instead of
 * a bare spinner on a blank screen.
 */
@Composable
fun SkeletonList(rows: Int = 5) {
    val alpha = LocalSkeletonAlpha.current
    Column {
        repeat(rows) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SentinelSurfaceLow),
                border = BorderStroke(1.dp, SentinelOutline),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.55f)
                            .height(16.dp)
                            .alpha(alpha)
                            .background(SentinelSurfaceHigh, RoundedCornerShape(10.dp)),
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth(0.8f)
                            .height(12.dp)
                            .alpha(alpha)
                            .background(SentinelSurfaceHigh, RoundedCornerShape(10.dp)),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** The backend stamps rows with epoch milliseconds; render them
 *  the way the rest of the app writes dates (yyyy-MM-dd HH:mm). */
fun formatEpochMs(ms: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(ms))

@Composable
fun LoadingBox(message: String = "Loading…") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(message, color = SentinelMuted)
    }
}

/** Every error says what happened and what to do next (spec §8). */
@Composable
fun ErrorBox(message: String, onRetry: (() -> Unit)? = null) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SentinelSurface),
        border = BorderStroke(1.dp, SentinelOutline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Something went wrong", style = MaterialTheme.typography.titleMedium, color = SentinelDanger)
            Spacer(Modifier.height(6.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
            if (onRetry != null) {
                TextButton(onClick = onRetry) { Text("Try again") }
            }
        }
    }
}

@Composable
fun EmptyBox(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(body, color = SentinelMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SectionHeader(title: String, count: Int? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionTick()
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (count != null) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = if (count > 0) MaterialTheme.colorScheme.primary else SentinelMuted,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun ItemRow(
    title: String,
    subtitle: String? = null,
    badge: String? = null,
    entranceIndex: Int? = null,
    onClick: (() -> Unit)? = null,
) {
    // Motion pass (2026-11-04): a wired row gains the M-09
    // capped stagger entrance (only LazyColumn call sites pass
    // an index) and clickable rows gain the M-01 press-sink via
    // sentinelClickable — the ripple, padding, and card shape
    // are exactly as before; unwired rows are untouched.
    val rowModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp)
        .then(if (entranceIndex != null) Modifier.listEntrance(entranceIndex) else Modifier)
        .then(if (onClick != null) Modifier.sentinelClickable { onClick() } else Modifier)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, SentinelOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = rowModifier,
    ) {
        Box(Modifier.background(sentinelCardBrush())) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge)
                    if (!subtitle.isNullOrBlank()) {
                        Text(subtitle, color = SentinelMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (!badge.isNullOrBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SentinelSurfaceHigh),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            badge,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else if (onClick != null) {
                    // hunt18: the chevron affordance in the trailing
                    // space a badge would take — rows that go
                    // somewhere say so. Badge behavior unchanged
                    // when a badge exists (above).
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = SentinelMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Box(Modifier.align(Alignment.TopCenter)) { CardTopHighlight() }
        }
    }
}

/**
 * A monogram disc (hunt18): the initials of a name already on
 * screen, in an emerald ring on SurfaceHigh — no new data, no
 * new copy; the disc is decoration beside the unchanged name.
 */
@Composable
fun MonogramDisc(name: String, sizeDp: Dp = 40.dp) {
    val initials = name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("")
    Box(
        Modifier
            .size(sizeDp)
            .background(SentinelSurfaceHigh, CircleShape)
            .border(1.dp, SentinelGreen.copy(alpha = 0.55f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials,
            style = if (sizeDp >= 48.dp) MaterialTheme.typography.titleMedium
            else MaterialTheme.typography.labelLarge,
            color = SentinelGreen,
        )
    }
}

/**
 * The established haptic tick (hunt16, Polish Pass 2): one light
 * keyboard-tap tick on tab/drawer switches, chip picks, stage
 * steppers, and picker confirms — the surfaces the owner moves
 * by feel. Never on destructive confirms beyond the confirm
 * itself, never on passive rendering. Returns a stable lambda.
 */
@Composable
fun rememberHapticTick(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        {
            try {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            } catch (ignored: Exception) {
                // Haptics are garnish; a device without a vibrator
                // path must never break the interaction.
            }
        }
    }
}

/**
 * Skeleton lines WITHOUT the card wrapper — for bodies that
 * already live inside a card (AsyncCard) or a plain column, so
 * the loading shape never nests a card in a card (hunt16).
 */
@Composable
fun SkeletonLines(rows: Int = 3) {
    val alpha = LocalSkeletonAlpha.current
    Column {
        repeat(rows) { i ->
            Box(
                Modifier
                    .fillMaxWidth(if (i % 2 == 0) 0.85f else 0.6f)
                    .height(13.dp)
                    .alpha(alpha)
                    .background(SentinelSurfaceHigh, RoundedCornerShape(10.dp)),
            )
            Spacer(Modifier.height(9.dp))
        }
    }
}

/**
 * A mid-list failure banner (hunt16): when content is already on
 * screen and a refresh or an action fails, the content STAYS and
 * the failure rides as a dismissible banner with Retry — never a
 * full-screen replacement for data the owner already has.
 */
@Composable
fun ErrorBanner(message: String, onRetry: (() -> Unit)? = null, onDismiss: (() -> Unit)? = null) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SentinelSurface),
        border = BorderStroke(1.dp, SentinelDanger),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (onRetry != null) {
                TextButton(onClick = onRetry) { Text("Retry") }
            }
            if (onDismiss != null) {
                TextButton(onClick = onDismiss) { Text("Dismiss") }
            }
        }
    }
}

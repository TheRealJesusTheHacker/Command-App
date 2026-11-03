package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.sentinelsafeguard.app.ui.theme.SentinelMuted
import net.sentinelsafeguard.app.ui.theme.SentinelOutline
import net.sentinelsafeguard.app.ui.theme.SentinelSurface
import net.sentinelsafeguard.app.ui.theme.SentinelSurfaceHigh

/** Shared building blocks for the mirrored Command screens:
 *  the web stat cards, titled cards, chips, and steppers. */

/** A web crmstat card: big number over its exact Command label. */
@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SentinelSurface),
        border = BorderStroke(1.dp, SentinelOutline),
        modifier = modifier.padding(4.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(label, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** A titled card in web's shape: "<title> <count>" header + content. */
@Composable
fun MirrorCard(
    title: String,
    count: Int? = null,
    footerLabel: String? = null,
    onFooter: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SentinelSurface),
        border = BorderStroke(1.dp, SentinelOutline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
    }
}

/** A small static chip (priority / stage / source / flags). */
@Composable
fun MirrorChip(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = SentinelSurfaceHigh)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipRow(
    options: List<Pair<String, String>>,
    selected: String?,
    onPick: (String?) -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (key, label) ->
            FilterChip(
                selected = selected == key,
                onClick = { onPick(if (key == "all") null else key) },
                label = { Text(label) },
            )
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        val back = MirrorVocab.stepStage(current, -1)
        val fwd = MirrorVocab.stepStage(current, +1)
        TextButton(onClick = { back?.let(onStep) }, enabled = back != null) { Text("‹") }
        TextButton(onClick = onOpen) { Text(openLabel) }
        TextButton(onClick = { fwd?.let(onStep) }, enabled = fwd != null) { Text("›") }
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

/** Placeholder for a Command view that lands in a later phase —
 *  exact Command title + one plain line naming its phase. */
@Composable
fun PlaceholderScreen(title: String, phase: String) {
    Column {
        MirrorHeader(title)
        EmptyBox(
            title,
            "$title lands in $phase of the mirror build.",
        )
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
    Column {
        Text(label, color = SentinelMuted, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { open = true }) {
            Text(value.replaceFirstChar { it.uppercase() })
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.replaceFirstChar { it.uppercase() }) },
                    onClick = { onPick(option); open = false },
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

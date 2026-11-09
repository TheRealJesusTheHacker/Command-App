package net.sentinelsafeguard.app.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import net.sentinelsafeguard.app.data.api.wysAudio
import net.sentinelsafeguard.app.ui.theme.SentinelAmber
import net.sentinelsafeguard.app.ui.theme.SentinelDanger
import net.sentinelsafeguard.app.ui.theme.SentinelGreen
import net.sentinelsafeguard.app.ui.theme.SentinelMuted
import java.util.Locale

/**
 * 1.6.1 (Feature Wave mirror) surfaces. Every composable here
 * renders ONLY what its payload carries — the parsing and the
 * honesty rails live in [Wave161Logic]; these are the paints.
 */

// ---------- A + B. While You Slept card + briefing audio ----------

/**
 * The overnight card, mirroring the web card's discipline:
 * the window label as served, FAILURES FIRST, then the
 * counts, then backups (summary + expandable chains), then
 * the briefing-audio row. Rendered only when the dashboard
 * payload carries whileYouSlept (owner sessions) — the
 * caller gates on the parsed value, so staff/client payloads
 * (which never carry the field) show nothing here.
 */
@Composable
fun WysCard(viewModel: AppViewModel, wys: Wave161Logic.Wys) {
    MirrorCard("While you slept") {
        Text(wys.windowLabel, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)

        if (wys.hasFailures) {
            Spacer(Modifier.height(8.dp))
            wys.failures.forEach { f ->
                Text(
                    f.label,
                    color = SentinelDanger,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (f.detail.isNotBlank()) {
                    Text(f.detail, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        WysCountRow("News filed", wys.newsFiled)
        WysCountRow("News published", wys.newsPublished)
        WysCountRow("Threat briefs delivered", wys.threatBriefsDelivered)
        WysCountRow("Tickets touched", wys.ticketsTouched)
        WysCountRow("Jobs touched", wys.jobsTouched)

        Spacer(Modifier.height(8.dp))
        Text(
            Wave161Logic.backupsSummaryLine(wys.backups),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (wys.backups.available && !wys.backups.allOk) SentinelAmber else MaterialTheme.colorScheme.onSurface,
        )
        if (wys.backups.available) {
            wys.backups.checkedAt?.let {
                Text("Checked ${formatEpochMs(it)}", color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
            }
            var expanded by remember { mutableStateOf(false) }
            if (wys.backups.chains.isNotEmpty()) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide backup chains" else "Show backup chains")
                }
                if (expanded) {
                    wys.backups.chains.forEach { chain ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(chain.name, modifier = Modifier.weight(1f))
                            Text(
                                if (chain.ok) "OK" else "FAILED",
                                color = if (chain.ok) SentinelGreen else SentinelDanger,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            )
                        }
                        val sub = listOfNotNull(
                            chain.archive?.takeIf { it.isNotBlank() },
                            chain.detail.takeIf { it.isNotBlank() },
                        ).joinToString(" · ")
                        if (sub.isNotBlank()) {
                            Text(sub, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        WysAudioRow(viewModel, wys.audio)
    }
}

@Composable
private fun WysCountRow(label: String, value: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        Text(
            value.toString(),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum",
            ),
        )
    }
}

/**
 * The briefing-audio row. audio.available == false renders
 * the quiet honest line and NO control. When the card says
 * audio exists, the play control fetches it through the
 * getwysaudio action (the caller never names a file — the
 * server recomputes the current card's hash) and plays the
 * returned bytes through the app's voice-note machinery.
 * A denied or unavailable answer degrades to the same quiet
 * line — never an error dialog.
 */
@Composable
private fun WysAudioRow(viewModel: AppViewModel, audio: Wave161Logic.WysAudio) {
    if (!audio.available) {
        Text(Wave161Logic.AUDIO_NOT_GENERATED_LINE, color = SentinelMuted)
        return
    }
    val scope = rememberCoroutineScope()
    val player = rememberVoicePlayer()
    var fetching by remember { mutableStateOf(false) }
    var unavailable by remember { mutableStateOf(false) }
    if (unavailable) {
        Text(Wave161Logic.AUDIO_NOT_GENERATED_LINE, color = SentinelMuted)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            enabled = !fetching,
            onClick = {
                if (player.activeKey == "wys-briefing") {
                    player.togglePause()
                } else {
                    fetching = true
                    scope.launch {
                        try {
                            val body = viewModel.api.wysAudio()
                            when (val r = Wave161Logic.parseWysAudio(body)) {
                                is Wave161Logic.WysAudioResult.Ready ->
                                    if (!player.play("wys-briefing", r.bytes, r.extension)) {
                                        unavailable = true
                                    }
                                else -> unavailable = true
                            }
                        } catch (e: Exception) {
                            // The fetch failing is the same quiet
                            // state to the reader — the card never
                            // grows an error dialog for audio.
                            unavailable = true
                        } finally {
                            fetching = false
                        }
                    }
                }
            },
        ) {
            PlayPauseMorphIcon(playing = player.activeKey == "wys-briefing" && player.isPlaying)
        }
        Column {
            Text("Morning briefing", style = MaterialTheme.typography.bodyMedium)
            val state = when {
                fetching -> "Loading…"
                player.activeKey == "wys-briefing" && player.durationMs > 0 ->
                    "${VoicePlayerLogic.clockText(player.positionMs)} / ${VoicePlayerLogic.clockText(player.durationMs)}"
                else -> "Listen to the overnight briefing"
            }
            Text(state, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ---------- C. Client health (staff health timeline) ----------

/**
 * The clientHealth block on the staff health-timeline card.
 * Score + band render the server's values verbatim (band
 * chip colored BY BAND STRING via [Wave161Logic.bandColorKey]
 * — thresholds are never recomputed client-side). The
 * insufficient state is an honest sentence, never a 0.
 */
@Composable
fun ClientHealthBlock(health: Wave161Logic.ClientHealth) {
    Column {
        Text("Client health", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        if (Wave161Logic.isInsufficient(health)) {
            Text(Wave161Logic.HEALTH_INSUFFICIENT_COPY, color = SentinelMuted)
            return
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${health.score}",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            )
            health.band?.takeIf { it.isNotBlank() }?.let { band ->
                val color = when (Wave161Logic.bandColorKey(band)) {
                    "good" -> SentinelGreen
                    "fair" -> SentinelAmber
                    "atrisk" -> SentinelAmber
                    "critical" -> SentinelDanger
                    else -> SentinelMuted
                }
                Text(
                    "  $band",
                    color = color,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
        DetailLine("Open tickets", health.openTickets.toString())
        DetailLine("Oldest open ticket", "${health.oldestOpenTicketAgeDays} day(s)")
        DetailLine("Overdue invoices", health.overdueInvoices.toString())
        DetailLine("Assets expiring within 30 days", health.expiringAssets.toString())
        DetailLine(
            "Days since contact",
            health.daysSinceContact?.toString() ?: "—",
        )
        if (health.deductions.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text("Why this score", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            health.deductions.forEach { d ->
                DetailLine(d.label, d.points.toString())
            }
        }
        Wave161Logic.trendLine(
            health,
            health.trendBaselineAt?.let { formatEpochMs(it).substringBefore(" ") },
        )?.let { line ->
            Spacer(Modifier.height(4.dp))
            Text("Trend: $line", color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ---------- D. Photo timeline ----------

/**
 * A stage-grouped photo timeline in the server's served
 * order: a stageLabel header per consecutive group, then the
 * group's photos as viewable rows (the app's photo download
 * path + preview dialog, read-only — management stays on the
 * staff Photos card). Renders nothing for an empty list.
 */
@Composable
fun PhotoTimelineGroups(viewModel: AppViewModel, entries: List<JsonObject>) {
    if (entries.isEmpty()) return
    val scope = rememberCoroutineScope()
    var viewPhoto by remember { mutableStateOf<ByteArray?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    Wave161Logic.photoGroups(entries).forEach { group ->
        Text(
            group.stageLabel,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        group.photos.forEach { photo ->
            val pid = Phase3Logic.int(photo, "id")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(Wave161Logic.photoLabel(photo), style = MaterialTheme.typography.bodyMedium)
                    Phase3Logic.long(photo, "createdAt")?.let {
                        Text(formatEpochMs(it), color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                TextButton(onClick = {
                    if (pid != null) scope.launch {
                        try {
                            viewPhoto = viewModel.api.downloadAll("/api/app/photos/$pid/download")
                        } catch (e: Exception) {
                            error = plainError(e, "The photo could not be opened. Try again.")
                        }
                    }
                }) { Text("View") }
            }
        }
    }
    ActionErrorText(error)
    viewPhoto?.let { bytes ->
        val bitmap = remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
        AlertDialog(
            onDismissRequest = { viewPhoto = null },
            title = { Text("Photo") },
            text = {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Timeline photo",
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text("This file is not an image the phone can preview.")
                }
            },
            confirmButton = {
                TextButton(onClick = { viewPhoto = null }) { Text("Close") }
            },
        )
    }
}

// ---------- E. Booking prep ----------

/**
 * The prep card for one booking/service payload: the lines
 * for the app's current language, nothing at all when the
 * payload carries no prep. (BookingAdmin is the in-app
 * consumer of pub_cf_service / pub_cf_booking today; this
 * block is payload-driven, so any surface fed by those
 * serializers — the a_cfbook confirmation shape included —
 * lights up identically.)
 */
@Composable
fun PrepBlock(holder: JsonObject) {
    val lang = Locale.getDefault().language
    val lines = Wave161Logic.prepLines(holder, lang) ?: return
    Column(Modifier.padding(top = 4.dp, bottom = 4.dp)) {
        Text(
            if (lang.equals("es", ignoreCase = true)) "Antes de la visita" else "Before the visit",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        lines.forEach { line ->
            Text("• $line", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

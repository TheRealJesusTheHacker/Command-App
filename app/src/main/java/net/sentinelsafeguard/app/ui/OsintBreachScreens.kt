package net.sentinelsafeguard.app.ui

import android.content.Intent
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.data.api.breachCirculationCreate
import net.sentinelsafeguard.app.data.api.breachCirculationDelete
import net.sentinelsafeguard.app.data.api.breachCreate
import net.sentinelsafeguard.app.data.api.breachDelete
import net.sentinelsafeguard.app.data.api.breachEdit
import net.sentinelsafeguard.app.data.api.breachIntel
import net.sentinelsafeguard.app.data.api.breachIntakeRun
import net.sentinelsafeguard.app.data.api.osintCreateSearch
import net.sentinelsafeguard.app.data.api.osintCreateWatch
import net.sentinelsafeguard.app.data.api.osintDeleteReport
import net.sentinelsafeguard.app.data.api.osintDeleteWatch
import net.sentinelsafeguard.app.data.api.osintDetect
import net.sentinelsafeguard.app.data.api.osintReport
import net.sentinelsafeguard.app.data.api.osintRunWatchNow
import net.sentinelsafeguard.app.data.api.osintWorkspace
import net.sentinelsafeguard.app.ui.theme.SentinelAmber
import net.sentinelsafeguard.app.ui.theme.SentinelDanger
import net.sentinelsafeguard.app.ui.theme.SentinelGreen
import net.sentinelsafeguard.app.ui.theme.SentinelMuted
import net.sentinelsafeguard.app.ui.theme.SentinelOutline
import net.sentinelsafeguard.app.ui.theme.SentinelSurfaceHigh
import net.sentinelsafeguard.app.ui.theme.SentinelSurfaceTop

/**
 * OSINT flagship deck + Breach Intel (Lane E, 1.4.0).
 *
 * The OSINT surface is a DECK of distinct tools — one dedicated
 * screen per server capability — grouped Identity / Network /
 * Vehicles & Air / Background / Breach / On-device. Every tool
 * rides the SAME /api/app/osint* endpoints and payload shapes
 * the generic form used (mirror law: same actions, labels and
 * data; presentation elevates), so server-side purpose notes
 * and activity logging hold exactly as before.
 *
 * 1.5.0: the deck wires Command's nine new live server legs —
 * subdomain / dns / site / archive / btc / eth / breach are
 * identifier types on the same surface (no new endpoints);
 * the IP tool carries the allocation & geo leg and plots it
 * when the payload carries coordinates; the email breach tool
 * runs the dedicated two-corpus "breach" battery; the username
 * tool's variations are swept server-side inside its run (the
 * old on-device batch tool was removed — see OsintLogic); the
 * dork generator stays an explicitly labeled on-device preview
 * fed by the server's templates, new type keys included.
 *
 * Estate rails, in the UI and not just the docs:
 *  - a purpose note is required on EVERY run (owner included) —
 *    the field is on every run form and the server refuses
 *    without one, in its own words;
 *  - hits carry the server's possible-match label verbatim;
 *    a no-hit criminal outcome carries the server's no-records
 *    wording; unavailable sources grade "unavailable", never
 *    as negatives;
 *  - manual legs are labeled manual — pre-filled links the
 *    searcher opens by hand; nothing is scraped;
 *  - reports close with the FCRA footer;
 *  - breach circulation venue links are never stored (the
 *    server refuses them in its own words; the form says so).
 *
 * 1.5.2 (Elevation F3): pivot actions. Completed search rows
 * and the full report view offer the server-computed pivots
 * ({fromType, value, type, label} — ip → site/subdomain,
 * domain → subdomains/DNS/Wayback, email → breach/username;
 * nothing else offers any). One tap queues the chained run
 * with NO purpose in the body — the server carries the
 * parent run's purpose verbatim and ignores a sent one —
 * and the child report names its parent (parentSearchId).
 * No pivot is ever derived client-side.
 */

// ---------------------------------------------------------------------
// Shared run machinery
// ---------------------------------------------------------------------

/** Run phases for a tool screen: the live state the owner sees. */
private enum class RunPhase { IDLE, SUBMITTING, RUNNING, DONE, FAILED }

/** Poll the workspace until [searchId] settles. Returns
 *  (reportId, failureNote); reportId null + note null = still
 *  running when the polling budget ran out (the search itself
 *  keeps running server-side and lands in Searches/Reports). */
private suspend fun awaitSearch(
    viewModel: AppViewModel,
    searchId: Int,
    onStatus: (String) -> Unit,
): Pair<Int?, String?> {
    var polls = 0
    while (polls < 100) {
        delay(3000)
        polls++
        val ws = try {
            viewModel.api.osintWorkspace()
        } catch (e: Exception) {
            null
        } ?: continue
        val search = Phase3Logic.arr(ws, "searches")
            .firstOrNull { Phase3Logic.int(it, "id") == searchId } ?: continue
        when (Phase3Logic.str(search, "status")) {
            "done" -> return Phase3Logic.int(search, "reportId") to null
            "failed" -> return null to
                (Phase3Logic.strOrNull(search, "failureNote")
                    ?: "The search failed on the server.")
            "in_progress" -> onStatus("Running — sources are answering…")
            else -> onStatus("Queued — waiting for the search worker…")
        }
    }
    return null to null
}

/**
 * 1.5.2 (Elevation F3): submit ONE server-offered pivot and
 * poll it to its report, exactly like a fresh run — except
 * the body carries NO purpose (see OsintLogic.pivotRunBody):
 * the server carries the parent run's purpose verbatim and
 * says so in its response, which the run state repeats.
 * Returns awaitSearch's (reportId, failureNote) pair.
 */
private suspend fun runPivotSearch(
    viewModel: AppViewModel,
    parentSearchId: Int,
    pivot: OsintLogic.Pivot,
    onStatus: (String) -> Unit,
): Pair<Int?, String?> {
    val out = viewModel.api.osintCreateSearch(
        OsintLogic.pivotRunBody(parentSearchId, pivot))
    val sid = Phase3Logic.int(out, "searchId")
        ?: return null to "The server did not queue this pivot run. Try again."
    val carried = Phase3Logic.strOrNull(out, "purpose")
    onStatus(
        "Pivot queued — running under the purpose carried from " +
            "the parent run" + (carried?.let { ": \"$it\"" } ?: "") + "…",
    )
    return awaitSearch(viewModel, sid, onStatus)
}

/** The pivot buttons block, shared by the report view and the
 *  deck's completed search rows: one button per server offer,
 *  nothing offered that the server did not compute. */
@Composable
private fun PivotActions(
    pivots: List<OsintLogic.Pivot>,
    busy: Boolean,
    statusLine: String,
    error: String?,
    onRun: (OsintLogic.Pivot) -> Unit,
) {
    pivots.forEach { pivot ->
        OutlinedButton(
            onClick = { onRun(pivot) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        ) { Text(OsintLogic.pivotActionLabel(pivot)) }
    }
    if (busy) RunStateLine(RunPhase.RUNNING, statusLine)
    ActionErrorText(error)
}

@Composable
private fun GradePill(grade: OsintLogic.Grade) {
    val (fill, content) = when (grade) {
        OsintLogic.Grade.HIT -> SentinelGreen.copy(alpha = 0.16f) to SentinelGreen
        OsintLogic.Grade.NO_HIT -> SentinelSurfaceHigh to SentinelMuted
        OsintLogic.Grade.UNAVAILABLE -> SentinelAmber.copy(alpha = 0.14f) to SentinelAmber
        OsintLogic.Grade.MANUAL -> Color.Transparent to SentinelGreen
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = fill),
        shape = RoundedCornerShape(10.dp),
        border = if (grade == OsintLogic.Grade.MANUAL) {
            BorderStroke(1.dp, SentinelOutline)
        } else {
            null
        },
    ) {
        Text(
            grade.label.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            color = content,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SeverityPill(severity: String) {
    val (fill, content) = when (OsintLogic.severityKey(severity)) {
        "critical" -> SentinelDanger.copy(alpha = 0.22f) to Color(0xFFFF8A7A)
        "high" -> SentinelAmber.copy(alpha = 0.16f) to SentinelAmber
        "medium" -> SentinelGreen.copy(alpha = 0.12f) to SentinelGreen
        else -> SentinelSurfaceHigh to SentinelMuted
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = fill),
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(
            severity.ifBlank { "Low" }.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            color = content,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun RunStateLine(phase: RunPhase, statusLine: String) {
    when (phase) {
        RunPhase.SUBMITTING, RunPhase.RUNNING -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 6.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = SentinelGreen,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                statusLine.ifBlank { "Working…" },
                style = MaterialTheme.typography.bodyMedium,
                color = SentinelMuted,
            )
        }
        RunPhase.DONE -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 6.dp),
        ) {
            DrawnTick(Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(statusLine, style = MaterialTheme.typography.bodyMedium)
        }
        else -> Unit
    }
}

/** Back row + title block shared by every dedicated tool screen. */
@Composable
private fun ToolScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 16.dp),
        ) {
            TextButton(onClick = onBack) { Text("‹ Deck") }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, color = SentinelMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        LazyColumn(content = content)
    }
}

/** The purpose-note + run/watch block every server run carries. */
@Composable
private fun PurposeRunPanel(
    purpose: String,
    onPurpose: (String) -> Unit,
    phase: RunPhase,
    statusLine: String,
    error: String?,
    notice: String?,
    runLabel: String,
    onRun: () -> Unit,
    onSaveWatch: (() -> Unit)? = null,
) {
    TextFieldRow("Purpose note (required)", purpose, onChange = onPurpose, singleLine = false, minLines = 2)
    Text(
        "Every run is recorded in the activity log with its purpose — the server refuses a run without one.",
        color = SentinelMuted,
        style = MaterialTheme.typography.bodySmall,
    )
    RunStateLine(phase, statusLine)
    ActionErrorText(error)
    if (notice != null) Text(notice)
    Spacer(Modifier.height(8.dp))
    Row {
        GradientPrimaryButton(
            text = runLabel,
            enabled = phase != RunPhase.SUBMITTING && phase != RunPhase.RUNNING,
            modifier = Modifier.padding(end = 8.dp),
            onClick = onRun,
        )
        if (onSaveWatch != null) {
            OutlinedButton(
                onClick = onSaveWatch,
                enabled = phase != RunPhase.SUBMITTING && phase != RunPhase.RUNNING,
            ) { Text("Save as watch") }
        }
    }
}

/** Checklist + dork preview for a type, from the workspace payload. */
@Composable
private fun TypeSupportCards(workspace: JsonObject?, typeKey: String, value: String, city: String, state: String) {
    if (workspace == null) return
    // checklists/dorkTemplates are objects keyed by type holding arrays.
    val checklistEl: JsonElement? = (workspace["checklists"] as? JsonObject)?.get(typeKey)
    val checklistItems = (checklistEl as? JsonArray)?.mapNotNull {
        (it as? JsonPrimitive)?.content
    } ?: emptyList()
    if (checklistItems.isNotEmpty()) {
        MirrorCard("Checklist — $typeKey") {
            checklistItems.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        }
    }
    val dorkEl: JsonElement? = (workspace["dorkTemplates"] as? JsonObject)?.get(typeKey)
    val templates = (dorkEl as? JsonArray)?.mapNotNull {
        (it as? JsonPrimitive)?.content
    } ?: emptyList()
    if (templates.isNotEmpty() && value.isNotBlank()) {
        MirrorCard("Dork pack — this target") {
            OsintLogic.fillDorks(templates, value, city, state).forEach { q ->
                Text(q, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// ---------------------------------------------------------------------
// Deck home
// ---------------------------------------------------------------------

@Composable
fun OsintScreen(viewModel: AppViewModel, nav: NavController) {
    val scope = rememberCoroutineScope()
    val state = rememberJsonScreen { viewModel.api.osintWorkspace() }
    var route by remember { mutableStateOf<String?>(null) }
    var prefill by remember { mutableStateOf("") }
    var reportTarget by remember { mutableStateOf<Triple<Int, String?, Boolean>?>(null) }
    var confirmDeleteReport by remember { mutableStateOf<Int?>(null) }
    var confirmDeleteWatch by remember { mutableStateOf<Int?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val run: (suspend () -> String?) -> Unit = { block ->
        scope.launch {
            try {
                notice = block()
                error = null
                state.reloadKey++
            } catch (e: Exception) {
                error = plainError(e, "That could not be done. Try again.")
            }
        }
    }

    // 1.5.2 (Elevation F3): pivot runs launched from the deck's
    // completed search rows. The offers ride the search objects
    // themselves; one tap queues the chained run (no purpose —
    // the server carries the parent's) and lands in its report.
    var pivotBusyFor by remember { mutableStateOf<Int?>(null) }
    var pivotStatus by remember { mutableStateOf("") }
    var pivotError by remember { mutableStateOf<Pair<Int, String>?>(null) }
    fun runDeckPivot(parentSid: Int, pivot: OsintLogic.Pivot) {
        if (pivotBusyFor != null) return
        scope.launch {
            pivotBusyFor = parentSid
            pivotError = null
            pivotStatus = "Submitting the pivot run…"
            try {
                val (rid, fail) = runPivotSearch(viewModel, parentSid, pivot) {
                    pivotStatus = it
                }
                when {
                    rid != null -> reportTarget = Triple(rid, null, false)
                    fail != null -> pivotError = parentSid to fail
                    else -> notice = "Pivot still running on the server — " +
                        "watch Searches on the deck."
                }
            } catch (e: Exception) {
                pivotError = parentSid to
                    plainError(e, "That pivot could not be run. Try again.")
            } finally {
                pivotBusyFor = null
                state.reloadKey++
            }
        }
    }

    // A dedicated tool or the report view replaces the deck.
    reportTarget?.let { (id, focus, manualOnly) ->
        OsintReportView(
            viewModel = viewModel,
            reportId = id,
            focusSource = focus,
            focusManual = manualOnly,
            onBack = { reportTarget = null; state.reloadKey++ },
            onDelete = { confirmDeleteReport = id; reportTarget = null },
            onOpenChildReport = { childId ->
                reportTarget = Triple(childId, null, false)
            },
        )
        confirmDeleteReport?.let { rid ->
            ConfirmGate("Delete report", "This permanently deletes this OSINT report.", "Delete",
                onDismiss = { confirmDeleteReport = null },
                onConfirm = {
                    confirmDeleteReport = null
                    run { viewModel.api.osintDeleteReport(rid); "Report deleted." }
                })
        }
        return
    }
    route?.let { toolId ->
        val workspace = state.data
        val back: () -> Unit = { route = null; state.reloadKey++ }
        val openReport: (Int, String?, Boolean) -> Unit = { id, focus, manualOnly ->
            reportTarget = Triple(id, focus, manualOnly)
        }
        when (toolId) {
            "username" -> UsernameToolScreen(viewModel, workspace, prefill, back, openReport)
            "email" -> EmailToolScreen(viewModel, workspace, prefill, back, openReport)
            "emailBreach" -> EmailBreachToolScreen(viewModel, workspace, prefill, back, openReport)
            "name" -> NameToolScreen(viewModel, workspace, prefill, back, openReport)
            "phone" -> PhoneToolScreen(viewModel, workspace, prefill, back, openReport)
            "address" -> AddressToolScreen(viewModel, workspace, prefill, back, openReport)
            "peopleLinks" -> PeopleLinksToolScreen(viewModel, workspace, prefill, back, openReport)
            "domain" -> DomainToolScreen(viewModel, workspace, prefill, back, openReport)
            "ip" -> IpToolScreen(viewModel, workspace, prefill, back, openReport)
            "subdomains" -> SubdomainsToolScreen(viewModel, workspace, prefill, back, openReport)
            "dns" -> DnsToolScreen(viewModel, workspace, prefill, back, openReport)
            "siteInspector" -> SiteInspectorToolScreen(viewModel, workspace, prefill, back, openReport)
            "archive" -> ArchiveToolScreen(viewModel, workspace, prefill, back, openReport)
            "btc" -> BtcToolScreen(viewModel, workspace, prefill, back, openReport)
            "eth" -> EthToolScreen(viewModel, workspace, prefill, back, openReport)
            "dork" -> DorkToolScreen(viewModel, workspace, prefill, back, openReport)
            "plate" -> PlateToolScreen(viewModel, workspace, prefill, back, openReport)
            "vin" -> VinToolScreen(viewModel, workspace, prefill, back, openReport)
            "tail" -> TailToolScreen(viewModel, workspace, prefill, back, openReport)
            "callsign" -> CallsignToolScreen(viewModel, workspace, prefill, back, openReport)
            "criminal" -> CriminalToolScreen(viewModel, workspace, back, openReport)
            "ctos" -> CtosScreen(viewModel, back)
            "exif" -> ExifToolScreen(back)
            else -> back()
        }
        return
    }

    Column {
        MirrorHeader(MirrorCopy.title("osint", "OSINT"), MirrorCopy.subtitle("osint"))
        when {
            state.error != null && state.data == null -> ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null -> SkeletonList()
            else -> {
                val data = state.data!!
                var detectValue by remember { mutableStateOf("") }
                var detectNote by remember { mutableStateOf<String?>(null) }
                var detectCandidates by remember { mutableStateOf<List<String>>(emptyList()) }
                LazyColumn {
                    item {
                        MirrorCard("OSINT flagship deck", topTier = true) {
                            Text(
                                "Distinct tools, each with its own run — not one flat form. " +
                                    "Every run needs a purpose note and is activity-logged. " +
                                    "Hits are possible matches until verified on the source.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            ActionErrorText(error)
                            if (notice != null) Text(notice!!)
                        }
                    }
                    item {
                        MirrorCard("CToS — OSINT tools", topTier = true) {
                            Text(
                                "The working deck: open a case on a target, then run " +
                                    "the Wave 1 tools against it — Username Profiler " +
                                    "across the full platform catalog, DNS with its " +
                                    "spoofability verdict, IP intelligence, WHOIS, " +
                                    "fingerprinting, metadata, and the local-compute " +
                                    "toolbox. Findings collect in the case and export " +
                                    "as one document.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.height(8.dp))
                            GradientPrimaryButton(
                                text = "Open OSINT tools",
                                onClick = { route = "ctos" },
                            )
                        }
                    }
                    item {
                        MirrorCard("Detect an identifier") {
                            TextFieldRow("Identifier", detectValue, onChange = { detectValue = it })
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        try {
                                            val out = viewModel.api.osintDetect(detectValue)
                                            detectCandidates = Phase3Logic.arrRaw(out, "candidates")
                                                .mapNotNull { (it as? JsonPrimitive)?.content }
                                            detectNote = if (detectCandidates.isEmpty()) {
                                                "Detect: no match"
                                            } else {
                                                "Detect: tap a type to open its tool"
                                            }
                                        } catch (e: Exception) {
                                            error = plainError(e, "Detect could not run. Try again.")
                                        }
                                    }
                                }) { Text("Detect") }
                                if (detectNote != null) {
                                    Spacer(Modifier.width(10.dp))
                                    Text(detectNote!!, color = SentinelMuted)
                                }
                            }
                            if (detectCandidates.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    detectCandidates.take(5).forEach { cand ->
                                        OutlinedButton(onClick = {
                                            prefill = detectValue
                                            route = when (cand) {
                                                "name" -> "name"
                                                "username" -> "username"
                                                "email" -> "email"
                                                "phone" -> "phone"
                                                "address" -> "address"
                                                "domain" -> "domain"
                                                "ip" -> "ip"
                                                "plate" -> "plate"
                                                "vin" -> "vin"
                                                "tail" -> "tail"
                                                "callsign" -> "callsign"
                                                "subdomain" -> "subdomains"
                                                "dns" -> "dns"
                                                "site" -> "siteInspector"
                                                "archive" -> "archive"
                                                "btc" -> "btc"
                                                "eth" -> "eth"
                                                "breach" -> "emailBreach"
                                                else -> "username"
                                            }
                                        }) { Text(cand) }
                                    }
                                }
                            }
                        }
                    }
                    var entrance = 0
                    for (group in OsintLogic.GROUPS) {
                        item { SectionHeader(group.title, OsintLogic.toolsInGroup(group).size) }
                        item {
                            Text(
                                group.subtitle,
                                color = SentinelMuted,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                            )
                        }
                        items(OsintLogic.toolsInGroup(group)) { tool ->
                            val idx = entrance++
                            ToolCard(tool, idx) {
                                if (tool.id == "breachRegister") {
                                    nav.navigate("breachintel")
                                } else {
                                    prefill = ""
                                    route = tool.id
                                }
                            }
                        }
                    }
                    val searches = Phase3Logic.arr(data, "searches")
                    item { SectionHeader("Searches", searches.size) }
                    if (searches.isEmpty()) {
                        item { EmptyBox("No searches yet", "Runs from any deck tool land here while they work.") }
                    }
                    itemsIndexed(searches) { i, search ->
                        val rid = Phase3Logic.int(search, "reportId")
                        val sid = Phase3Logic.int(search, "id")
                        val searchPivots =
                            if (Phase3Logic.str(search, "status") == "done") {
                                OsintLogic.parsePivots(search)
                            } else {
                                emptyList()
                            }
                        val searchParent =
                            Phase3Logic.int(search, "parentSearchId")
                        Column {
                            ItemRow(
                                title = Phase3Logic.str(search, "subject"),
                                subtitle = listOfNotNull(
                                    Phase3Logic.strOrNull(search, "status"),
                                    if (searchParent != null) {
                                        "Pivot of search #$searchParent"
                                    } else {
                                        null
                                    },
                                    Phase3Logic.strOrNull(search, "purpose"),
                                    Phase3Logic.strOrNull(search, "requestedByName"),
                                ).joinToString(" · "),
                                badge = Phase3Logic.strOrNull(search, "status"),
                                entranceIndex = i,
                                onClick = if (rid != null) {
                                    { reportTarget = Triple(rid, null, false) }
                                } else {
                                    null
                                },
                            )
                            if (searchPivots.isNotEmpty() && sid != null) {
                                MirrorCard("Pivot from this result") {
                                    Text(
                                        "One tap runs one related leg on the same " +
                                            "identifier, under this search's own " +
                                            "purpose — the server carries it over " +
                                            "and logs the run as a pivot of this " +
                                            "search, so no purpose note is asked " +
                                            "for here.",
                                        color = SentinelMuted,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    PivotActions(
                                        pivots = searchPivots,
                                        busy = pivotBusyFor == sid,
                                        statusLine = pivotStatus,
                                        error = pivotError
                                            ?.takeIf { it.first == sid }?.second,
                                        onRun = { runDeckPivot(sid, it) },
                                    )
                                }
                            }
                        }
                    }
                    val reports = Phase3Logic.arr(data, "reports")
                    item { SectionHeader("Reports", reports.size) }
                    if (reports.isEmpty()) {
                        item { EmptyBox("No reports yet", "Completed searches compile their reports here.") }
                    }
                    itemsIndexed(reports) { i, report ->
                        ItemRow(
                            title = Phase3Logic.str(report, "title"),
                            subtitle = listOfNotNull(
                                Phase3Logic.strOrNull(report, "subject"),
                                Phase3Logic.strOrNull(report, "summary"),
                            ).joinToString(" · "),
                            entranceIndex = i,
                            onClick = {
                                Phase3Logic.int(report, "id")?.let {
                                    reportTarget = Triple(it, null, false)
                                }
                            },
                        )
                    }
                    val watches = Phase3Logic.arr(data, "watches")
                    item { SectionHeader("Watchlist", watches.size) }
                    items(watches) { watch ->
                        val wid = Phase3Logic.int(watch, "id")
                        MirrorCard(Phase3Logic.str(watch, "label")) {
                            JsonLines(watch, "purpose" to "Purpose", "changeSummary" to "Changes")
                            DetailLine("Active", if (Phase3Logic.bool(watch, "active") == true) "Yes" else "No")
                            Phase3Logic.long(watch, "lastRunAt")?.let {
                                DetailLine("Last run", formatEpochMs(it))
                            }
                            Phase3Logic.long(watch, "nextRunAt")?.let {
                                DetailLine("Next run", formatEpochMs(it))
                            }
                            Row {
                                Button(
                                    onClick = { wid?.let { id -> run { viewModel.api.osintRunWatchNow(id); "Watch run queued." } } },
                                    modifier = Modifier.padding(end = 8.dp),
                                ) { Text("Run now") }
                                OutlinedButton(onClick = { confirmDeleteWatch = wid }) { Text("Delete watch") }
                            }
                        }
                    }
                    item {
                        MirrorCard("Method — read before relying on a hit") {
                            Text(
                                OsintLogic.REPORT_FOOTER,
                                color = SentinelMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
    confirmDeleteReport?.let { id ->
        ConfirmGate("Delete report", "This permanently deletes this OSINT report.", "Delete",
            onDismiss = { confirmDeleteReport = null },
            onConfirm = {
                confirmDeleteReport = null
                run { viewModel.api.osintDeleteReport(id); "Report deleted." }
            })
    }
    confirmDeleteWatch?.let { id ->
        ConfirmGate("Delete watch", "This permanently deletes this watch.", "Delete",
            onDismiss = { confirmDeleteWatch = null },
            onConfirm = {
                confirmDeleteWatch = null
                run { viewModel.api.osintDeleteWatch(id); "Watch deleted." }
            })
    }
}

@Composable
private fun ToolCard(tool: OsintLogic.Tool, entranceIndex: Int, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, SentinelOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .listEntrance(entranceIndex)
            .sentinelClickable { onClick() },
    ) {
        Box(Modifier.background(sentinelCardBrush())) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tool.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        if (tool.manual) {
                            Spacer(Modifier.width(8.dp))
                            MirrorChip("MANUAL")
                        }
                        if (tool.chip != null) {
                            Spacer(Modifier.width(8.dp))
                            MirrorChip(tool.chip!!)
                        }
                    }
                    Text(tool.subtitle, color = SentinelMuted, style = MaterialTheme.typography.bodyMedium)
                }
                Text("›", color = SentinelMuted, style = MaterialTheme.typography.titleLarge)
            }
            Box(Modifier.align(Alignment.TopCenter)) { CardTopHighlight() }
        }
    }
}

// ---------------------------------------------------------------------
// Dedicated identifier tools
// ---------------------------------------------------------------------

/** Server type labels (OSINT_TYPE_LABELS), verbatim. */
private val OSINT_TYPE_LABELS: Map<String, String> = mapOf(
    "name" to "Full name", "username" to "Username / handle",
    "email" to "Email address", "phone" to "Phone number",
    "address" to "Street address", "domain" to "Domain",
    "ip" to "IP address", "plate" to "License plate",
    "vin" to "VIN", "tail" to "Aircraft tail number",
    "callsign" to "Amateur radio callsign",
    "subdomain" to "Subdomain enumeration", "dns" to "DNS explorer",
    "site" to "Website (TLS / headers)", "archive" to "Web archive history",
    "btc" to "Bitcoin address", "eth" to "Ethereum address",
    "breach" to "Email breach check",
)

/**
 * The shared run engine for identifier tools: one shaped form,
 * one purpose-noted run over POST /api/app/osint/searches, live
 * polling of the search until its report lands, and a watch
 * save over the same inputs. Each dedicated screen below is
 * its own composable with its own field shape; this engine
 * only carries the run mechanics they all share.
 */
@Composable
private fun IdentifierRunScreen(
    viewModel: AppViewModel,
    tool: OsintLogic.Tool,
    workspace: JsonObject?,
    initialValue: String,
    onBack: () -> Unit,
    onOpenReport: (Int, String?, Boolean) -> Unit,
    focusSource: String? = null,
    focusManual: Boolean = false,
    banner: String? = null,
    showCityState: Boolean = false,
    showAssocName: Boolean = true,
    runLabel: String = "Run search",
) {
    val typeKey = tool.typeKey ?: return
    val scope = rememberCoroutineScope()
    var value by remember { mutableStateOf(initialValue) }
    var nameCtx by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var watchLabel by remember { mutableStateOf("") }
    var phase by remember { mutableStateOf(RunPhase.IDLE) }
    var statusLine by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    fun identifiersBody(): JsonObject = buildJsonObject {
        put("purpose", purpose)
        put("identifiers", JsonArray(listOf(buildJsonObject {
            put("type", typeKey); put("value", value)
        })))
        if (city.isNotBlank()) put("city", city)
        if (state.isNotBlank()) put("state", state)
        if (nameCtx.isNotBlank()) put("name", nameCtx)
    }

    fun doRun() {
        scope.launch {
            phase = RunPhase.SUBMITTING
            statusLine = "Submitting — the purpose note is recorded with the run…"
            error = null
            notice = null
            try {
                val out = viewModel.api.osintCreateSearch(identifiersBody())
                val sid = Phase3Logic.int(out, "searchId")
                if (sid == null) {
                    phase = RunPhase.FAILED
                    error = "The server did not queue this search. Try again."
                    return@launch
                }
                phase = RunPhase.RUNNING
                statusLine = "Queued — waiting for the search worker…"
                val (rid, fail) = awaitSearch(viewModel, sid) { statusLine = it }
                when {
                    rid != null -> {
                        phase = RunPhase.DONE
                        statusLine = "Report ready."
                        onOpenReport(rid, focusSource, focusManual)
                    }
                    fail != null -> {
                        phase = RunPhase.FAILED
                        error = fail
                    }
                    else -> {
                        phase = RunPhase.IDLE
                        notice = "Still running on the server — watch Searches on the deck; " +
                            "the compiled report lands under Reports."
                    }
                }
            } catch (e: Exception) {
                phase = RunPhase.FAILED
                error = plainError(e, "That search could not be run. Try again.")
            }
        }
    }

    fun doSaveWatch() {
        scope.launch {
            try {
                val body = buildJsonObject {
                    put("purpose", purpose)
                    put("label", watchLabel.ifBlank { "${tool.title} — $value" })
                    put("identifiers", JsonArray(listOf(buildJsonObject {
                        put("type", typeKey); put("value", value)
                    })))
                    if (city.isNotBlank()) put("city", city)
                    if (state.isNotBlank()) put("state", state)
                    if (nameCtx.isNotBlank()) put("name", nameCtx)
                }
                viewModel.api.osintCreateWatch(body)
                notice = "Watch saved — it re-runs weekly and diffs against the previous report."
                error = null
            } catch (e: Exception) {
                error = plainError(e, "That watch could not be saved. Try again.")
            }
        }
    }

    ToolScaffold(tool.title, tool.subtitle, onBack) {
        if (banner != null) {
            item {
                MirrorCard("How this leg works", topTier = true) {
                    Text(banner, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            MirrorCard("Run", topTier = banner == null) {
                TextFieldRow(OSINT_TYPE_LABELS[typeKey] ?: "Identifier", value, onChange = { value = it })
                if (showCityState) {
                    TextFieldRow("City (optional)", city, onChange = { city = it })
                    TextFieldRow("State (optional, e.g. MO)", state, onChange = { state = it })
                }
                if (showAssocName && typeKey != "name") {
                    TextFieldRow("Associated name (optional)", nameCtx, onChange = { nameCtx = it })
                }
                TextFieldRow("Watch label (optional)", watchLabel, onChange = { watchLabel = it })
                PurposeRunPanel(
                    purpose = purpose,
                    onPurpose = { purpose = it },
                    phase = phase,
                    statusLine = statusLine,
                    error = error,
                    notice = notice,
                    runLabel = runLabel,
                    onRun = { doRun() },
                    onSaveWatch = { doSaveWatch() },
                )
            }
        }
        item { TypeSupportCards(workspace, typeKey, value, city, state) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun UsernameToolScreen(
    viewModel: AppViewModel, workspace: JsonObject?, initial: String,
    onBack: () -> Unit, onOpenReport: (Int, String?, Boolean) -> Unit,
) {
    IdentifierRunScreen(
        viewModel = viewModel,
        tool = OsintLogic.tool("username")!!, workspace = workspace,
        initialValue = initial, onBack = onBack, onOpenReport = onOpenReport,
        banner = "The server sweeps handle variations inside this same run — " +
            "separator swaps and common alternate forms, re-checked on the " +
            "fast subset of sites — and reports them as their own graded " +
            "entry (\"Username variations\") in the report. A matching " +
            "variation is a different handle: a lead, not an identification.",
        runLabel = "Run username sweep",
    )
}

@Composable
private fun EmailToolScreen(
    viewModel: AppViewModel, workspace: JsonObject?, initial: String,
    onBack: () -> Unit, onOpenReport: (Int, String?, Boolean) -> Unit,
) {
    IdentifierRunScreen(
        viewModel = viewModel,
        tool = OsintLogic.tool("email")!!, workspace = workspace,
        initialValue = initial, onBack = onBack, onOpenReport = onOpenReport,
        runLabel = "Run email check",
    )
}

@Composable
private fun EmailBreachToolScreen(
    viewModel: AppViewModel, workspace: JsonObject?, initial: String,
    onBack: () -> Unit, onOpenReport: (Int, String?, Boolean) -> Unit,
) {
    IdentifierRunScreen(
        viewModel = viewModel,
        tool = OsintLogic.tool("emailBreach")!!, workspace = workspace,
        initialValue = initial, onBack = onBack, onOpenReport = onOpenReport,
        banner = "A dedicated breach battery on the server (type \"breach\") — " +
            "not the general email check, and no profile leg. Two independent " +
            "corpora are searched and graded separately in the report: the " +
            "XposedOrNot breach index, enriched by its exposure analytics " +
            "(breach names, years, exposed data classes — never password " +
            "values), and the LeakCheck public index. A corpus that does " +
            "not answer grades unavailable, never a negative. " +
            OsintLogic.BREACH_COVERAGE_LINE,
        runLabel = "Check this email for breaches",
    )
}

@Composable
private fun NameToolScreen(
    viewModel: AppViewModel, workspace: JsonObject?, initial: String,
    onBack: () -> Unit, onOpenReport: (Int, String?, Boolean) -> Unit,
) {
    IdentifierRunScreen(
        viewModel = viewModel,
        tool = OsintLogic.tool("name")!!, workspace = workspace,
        initialValue = initial, onBack = onBack, onOpenReport = onOpenReport,
        showCityState = true, showAssocName = false,
        runLabel = "Run name search",
    )
}

@Composable
private fun PhoneToolScreen(
    viewModel: AppViewModel, workspace: JsonObject?, initial: String,
    onBack: () -> Unit, onOpenReport: (Int, String?, Boolean) -> Unit,
) {
    IdentifierRunScreen(
        viewModel = viewModel,
        tool = OsintLogic.tool("phone")!!, workspace = workspace,
        initialValue = initial, onBack = onBack, onOpenReport = onOpenReport,
        runLabel = "Run phone check",
    )
}

@Composable
private fun AddressToolScreen(
    viewModel: AppViewModel, workspace: JsonObject?, initial: String,
    onBack: () -> Unit, onOpenReport: (Int, String?, Boolean) -> Unit,
) {
    IdentifierRunScreen(
        viewModel = viewModel,
        tool = OsintLogic.tool("address")!!, workspace = workspace,
        initialValue = initial, onBack = onBack, onOpenReport = onOpenReport,
        showCityState = true,
        runLabel = "Run address check",
    )
}

@Composable
private fun PeopleLinksToolScreen(
    viewModel: AppViewModel, workspace: JsonObject?, initial: String,
    onBack: () -> Unit, onOpenReport: (Int, String?, Boolean) -> Unit,
) {
    IdentifierRunScreen(
        viewModel = viewModel,
        tool = OsintLogic.tool("peopleLinks")!!, workspace = workspace,
        initialValue = initial, onBack = onBack, onOpenReport = onOpenReport,
        focusManual = true,
        showCityState = true, showAssocName = false,
        banner = "MANUAL LEG — labeled manual, worked by hand. People-search sites " +
            "block automated access, so nothing is scraped and nothing is evaded: " +
            "this run compiles the server's pre-filled link pack (plus the records " +
            "battery for the name), and the report opens on the manual links for " +
            "you to open one by one. Anything an aggregator shows is an unverified " +
            "mention — lead material, never a record.",
        runLabel = "Build the link pack",
    )
}

@Composable
private fun DomainToolScreen(
    viewModel: AppViewModel, workspace: JsonObject?, initial: String,
    onBack: () -> Unit, onOpenReport: (Int, String?, Boolean) -> Unit,
) {
    IdentifierRunScreen(
        viewModel = viewModel,
        tool = OsintLogic.tool("domain")!!, workspace = workspace,
        initialValue = initial, onBack = onBack, onOpenReport = onOpenReport,
        runLabel = "Run domain intel",
    )
}

@Composable
private fun IpToolScreen(
    viewModel: AppViewModel, workspace: JsonObject?, initial: String,
    onBack: () -> Unit, onOpenReport: (Int, String?, Boolean) -> Unit,
) {
    IdentifierRunScreen(
        viewModel = viewModel,
        tool = OsintLogic.tool("ip")!!, workspace = workspace,
        initialValue = initial, onBack = onBack, onOpenReport = onOpenReport,
        banner = "Three legs in one run: the RDAP registration record, reverse " +
            "DNS (PTR), and IP intelligence — allocation and geo from public " +
            "registries. A resolver failure grades \"unavailable\", never as " +
            "no-record. Allocation and geo data describe the network " +
            "operator, not a person. When a result carries coordinates, " +
            "the report plots them on the Sentinel map.",
        runLabel = "Run IP intel",
    )
}

@Composable
private fun SubdomainsToolScreen(
    viewModel: AppViewModel, workspace: JsonObject?, initial: String,
    onBack: () -> Unit, onOpenReport: (Int, String?, Boolean) -> Unit,
) {
    IdentifierRunScreen(
        viewModel = viewModel,
        tool = OsintLogic.tool("subdomains")!!, workspace = workspace,
        initialValue = initial, onBack = onBack, onOpenReport = onOpenReport,
        banner = "Two evidence sources merged into one graded entry: " +
            "certificate-transparency logs (crt.sh + Certspotter) and a " +
            "live DNS sweep of 70 common names. Every name found states " +
            "its evidence — in the CT logs, resolving, or both. If the " +
            "domain runs wildcard DNS, the report says so plainly: a name " +
            "that answers only because everything answers is flagged.",
        runLabel = "Enumerate subdomains",
    )
}

@Composable
private fun DnsToolScreen(
    viewModel: AppViewModel, workspace: JsonObject?, initial: String,
    onBack: () -> Unit, onOpenReport: (Int, String?, Boolean) -> Unit,
) {
    IdentifierRunScreen(
        viewModel = viewModel,
        tool = OsintLogic.tool("dns")!!, workspace = workspace,
        initialValue = initial, onBack = onBack, onOpenReport = onOpenReport,
        banner = "The full record battery over DNS-over-HTTPS: A, AAAA, " +
            "CNAME, MX, NS, SOA, TXT and CAA, the SPF policy split out of " +
            "TXT, DMARC, DKIM via the common selectors (labeled as " +
            "guesses — selectors cannot be known from the domain alone), " +
            "and the DNSSEC state. Hand it an IP address instead and it " +
            "runs reverse DNS (PTR) plus IP intelligence.",
        runLabel = "Explore DNS",
    )
}

@Composable
private fun SiteInspectorToolScreen(
    viewModel: AppViewModel, workspace: JsonObject?, initial: String,
    onBack: () -> Unit, onOpenReport: (Int, String?, Boolean) -> Unit,
) {
    IdentifierRunScreen(
        viewModel = viewModel,
        tool = OsintLogic.tool("siteInspector")!!, workspace = workspace,
        initialValue = initial, onBack = onBack, onOpenReport = onOpenReport,
        banner = "Facts only — no letter grades, no secure/insecure verdicts. " +
            "The TLS leg shakes hands with the site directly and reports " +
            "the certificate it is actually served (subject, SANs, issuer, " +
            "validity, protocol, cipher); certificate transparency " +
            "supplements it; the headers leg reports which response " +
            "headers were sent, with their values. If an intermediary " +
            "answers the handshake instead of the site, the TLS leg " +
            "grades unavailable and says so — the CT facts stand in.",
        runLabel = "Inspect site",
    )
}

@Composable
private fun ArchiveToolScreen(
    viewModel: AppViewModel, workspace: JsonObject?, initial: String,
    onBack: () -> Unit, onOpenReport: (Int, String?, Boolean) -> Unit,
) {
    IdentifierRunScreen(
        viewModel = viewModel,
        tool = OsintLogic.tool("archive")!!, workspace = workspace,
        initialValue = initial, onBack = onBack, onOpenReport = onOpenReport,
        banner = "The Wayback Machine's capture history for a domain or URL: " +
            "total captures (\"at least N\" when the archive's answer is " +
            "bounded — honesty over precision), earliest and latest " +
            "captures, a per-year histogram, and sample captures with " +
            "working replay links. Zero captures from an archive that " +
            "answered is an honest not-found; an archive that does not " +
            "answer grades unavailable, never a negative.",
        runLabel = "Browse snapshots",
    )
}

@Composable
private fun BtcToolScreen(
    viewModel: AppViewModel, workspace: JsonObject?, initial: String,
    onBack: () -> Unit, onOpenReport: (Int, String?, Boolean) -> Unit,
) {
    IdentifierRunScreen(
        viewModel = viewModel,
        tool = OsintLogic.tool("btc")!!, workspace = workspace,
        initialValue = initial, onBack = onBack, onOpenReport = onOpenReport,
        banner = OsintLogic.WALLET_NOTE + " Balance and totals are stated in " +
            "bitcoin and in raw satoshis — the smallest unit, exact. " +
            "Public chain explorers are tried in order and the report " +
            "names the one that answered.",
        runLabel = "Look up Bitcoin address",
    )
}

@Composable
private fun EthToolScreen(
    viewModel: AppViewModel, workspace: JsonObject?, initial: String,
    onBack: () -> Unit, onOpenReport: (Int, String?, Boolean) -> Unit,
) {
    IdentifierRunScreen(
        viewModel = viewModel,
        tool = OsintLogic.tool("eth")!!, workspace = workspace,
        initialValue = initial, onBack = onBack, onOpenReport = onOpenReport,
        banner = OsintLogic.WALLET_NOTE + " Balance and totals are stated in " +
            "ether and in raw wei — the smallest unit, exact. Public " +
            "chain explorers are tried in order and the report names " +
            "the one that answered.",
        runLabel = "Look up Ethereum address",
    )
}

@Composable
private fun PlateToolScreen(
    viewModel: AppViewModel, workspace: JsonObject?, initial: String,
    onBack: () -> Unit, onOpenReport: (Int, String?, Boolean) -> Unit,
) {
    IdentifierRunScreen(
        viewModel = viewModel,
        tool = OsintLogic.tool("plate")!!, workspace = workspace,
        initialValue = initial, onBack = onBack, onOpenReport = onOpenReport,
        showCityState = true,
        banner = "Plate-to-owner identity data is not publicly available — it is " +
            "restricted under the federal Driver's Privacy Protection Act (DPPA). " +
            "This tool runs the format check and lays out the lawful official " +
            "routes; it does not query owner data and no public source lawfully " +
            "provides it.",
        runLabel = "Run plate check",
    )
}

@Composable
private fun VinToolScreen(
    viewModel: AppViewModel, workspace: JsonObject?, initial: String,
    onBack: () -> Unit, onOpenReport: (Int, String?, Boolean) -> Unit,
) {
    IdentifierRunScreen(
        viewModel = viewModel,
        tool = OsintLogic.tool("vin")!!, workspace = workspace,
        initialValue = initial, onBack = onBack, onOpenReport = onOpenReport,
        runLabel = "Decode VIN",
    )
}

@Composable
private fun TailToolScreen(
    viewModel: AppViewModel, workspace: JsonObject?, initial: String,
    onBack: () -> Unit, onOpenReport: (Int, String?, Boolean) -> Unit,
) {
    IdentifierRunScreen(
        viewModel = viewModel,
        tool = OsintLogic.tool("tail")!!, workspace = workspace,
        initialValue = initial, onBack = onBack, onOpenReport = onOpenReport,
        runLabel = "Look up tail number",
    )
}

@Composable
private fun CallsignToolScreen(
    viewModel: AppViewModel, workspace: JsonObject?, initial: String,
    onBack: () -> Unit, onOpenReport: (Int, String?, Boolean) -> Unit,
) {
    IdentifierRunScreen(
        viewModel = viewModel,
        tool = OsintLogic.tool("callsign")!!, workspace = workspace,
        initialValue = initial, onBack = onBack, onOpenReport = onOpenReport,
        runLabel = "Look up callsign",
    )
}

// ---------------------------------------------------------------------
// Criminal background (server kind = "criminal")
// ---------------------------------------------------------------------

@Composable
private fun CriminalToolScreen(
    viewModel: AppViewModel, workspace: JsonObject?,
    onBack: () -> Unit, onOpenReport: (Int, String?, Boolean) -> Unit,
) {
    val tool = OsintLogic.tool("criminal")!!
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var watchLabel by remember { mutableStateOf("") }
    var phase by remember { mutableStateOf(RunPhase.IDLE) }
    var statusLine by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    fun criminalBody(): JsonObject = buildJsonObject {
        put("kind", "criminal")
        put("purpose", purpose)
        put("name", name)
        if (city.isNotBlank()) put("city", city)
        if (state.isNotBlank()) put("state", state)
        if (dob.isNotBlank()) put("dob", dob)
    }

    ToolScaffold(tool.title, tool.subtitle, onBack) {
        item {
            MirrorCard("Read first — what this battery is", topTier = true) {
                Text(
                    "A public-information battery: federal dockets, the FBI Wanted list, " +
                        "the federal inmate locator, sanctions lists, news mentions, and " +
                        "the official state route pack. Every hit is a " +
                        OsintLogic.POSSIBLE_MATCH_LABEL + " — a name match is not an " +
                        "identification. A battery with no hits reads \"" +
                        OsintLogic.NO_RECORDS_LABEL + "\" and lists the sources searched; " +
                        "it says nothing more about the person.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    OsintLogic.CRIMINAL_FOOTER,
                    color = SentinelMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            MirrorCard("Run") {
                TextFieldRow("Full name", name, onChange = { name = it })
                TextFieldRow("City (optional)", city, onChange = { city = it })
                TextFieldRow("State (optional, e.g. MO)", state, onChange = { state = it })
                DateFieldRow("Date of birth (optional)", dob, onChange = { dob = it })
                Text(
                    "DOB is held in this system for disambiguation; none of the automated " +
                        "sources take a DOB. Missouri's Case.net — the authoritative state " +
                        "record — refuses automated queries and ships in the report as a " +
                        "pre-filled manual step.",
                    color = SentinelMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                TextFieldRow("Watch label (optional)", watchLabel, onChange = { watchLabel = it })
                PurposeRunPanel(
                    purpose = purpose, onPurpose = { purpose = it },
                    phase = phase, statusLine = statusLine,
                    error = error, notice = notice,
                    runLabel = "Run criminal background check",
                    onRun = {
                        scope.launch {
                            phase = RunPhase.SUBMITTING
                            statusLine = "Submitting — the purpose note is recorded with the run…"
                            error = null
                            notice = null
                            try {
                                val out = viewModel.api.osintCreateSearch(criminalBody())
                                val sid = Phase3Logic.int(out, "searchId")
                                if (sid == null) {
                                    phase = RunPhase.FAILED
                                    error = "The server did not queue this check. Try again."
                                    return@launch
                                }
                                phase = RunPhase.RUNNING
                                statusLine = "Queued — waiting for the search worker…"
                                val (rid, fail) = awaitSearch(viewModel, sid) { statusLine = it }
                                when {
                                    rid != null -> {
                                        phase = RunPhase.DONE
                                        statusLine = "Report ready."
                                        onOpenReport(rid, null, false)
                                    }
                                    fail != null -> {
                                        phase = RunPhase.FAILED
                                        error = fail
                                    }
                                    else -> {
                                        phase = RunPhase.IDLE
                                        notice = "Still running on the server — the report " +
                                            "lands under Reports on the deck."
                                    }
                                }
                            } catch (e: Exception) {
                                phase = RunPhase.FAILED
                                error = plainError(e, "That check could not be run. Try again.")
                            }
                        }
                    },
                    onSaveWatch = {
                        scope.launch {
                            try {
                                val body = buildJsonObject {
                                    put("kind", "criminal")
                                    put("purpose", purpose)
                                    put("label", watchLabel.ifBlank {
                                        "Criminal background check — $name"
                                    })
                                    put("name", name)
                                    if (city.isNotBlank()) put("city", city)
                                    if (state.isNotBlank()) put("state", state)
                                    if (dob.isNotBlank()) put("dob", dob)
                                }
                                viewModel.api.osintCreateWatch(body)
                                notice = "Watch saved — it re-runs weekly and diffs " +
                                    "against the previous report."
                                error = null
                            } catch (e: Exception) {
                                error = plainError(e, "That watch could not be saved. Try again.")
                            }
                        }
                    },
                )
            }
        }
        item { TypeSupportCards(workspace, "name", name, city, state) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ---------------------------------------------------------------------
// Dork pack generator (per-target, from the workspace templates)
// ---------------------------------------------------------------------

@Composable
private fun DorkToolScreen(
    viewModel: AppViewModel, workspace: JsonObject?, initial: String,
    onBack: () -> Unit, onOpenReport: (Int, String?, Boolean) -> Unit,
) {
    val tool = OsintLogic.tool("dork")!!
    val scope = rememberCoroutineScope()
    val types = workspace?.let { Phase3Logic.arr(it, "types") } ?: emptyList()
    var typeKey by remember { mutableStateOf("name") }
    var value by remember { mutableStateOf(initial) }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var phase by remember { mutableStateOf(RunPhase.IDLE) }
    var statusLine by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current
    val templates = remember(workspace, typeKey) {
        val el = (workspace?.get("dorkTemplates") as? JsonObject)?.get(typeKey)
        (el as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content } ?: emptyList()
    }
    val pack = remember(templates, value, city, state) {
        if (value.isBlank()) emptyList() else OsintLogic.fillDorks(templates, value, city, state)
    }
    ToolScaffold(tool.title, tool.subtitle, onBack) {
        item {
            MirrorCard("Build the pack", topTier = true) {
                if (types.isNotEmpty()) {
                    ChoiceMenu(
                        "Target type",
                        types.firstOrNull { Phase3Logic.str(it, "key") == typeKey }
                            ?.let { Phase3Logic.str(it, "label") } ?: typeKey,
                        types.map { Phase3Logic.str(it, "label") },
                    ) { picked ->
                        typeKey = types.firstOrNull { Phase3Logic.str(it, "label") == picked }
                            ?.let { Phase3Logic.str(it, "key") } ?: typeKey
                    }
                }
                TextFieldRow("Target", value, onChange = { value = it })
                TextFieldRow("City (optional)", city, onChange = { city = it })
                TextFieldRow("State (optional, e.g. MO)", state, onChange = { state = it })
                Text(
                    "ON-DEVICE PREVIEW — generation happens on this phone from the " +
                        "server's own templates (the new subdomain, DNS and wallet " +
                        "types included). It is not a search: nothing is sent, " +
                        "nothing is logged, and no purpose note is needed to " +
                        "preview. The graded dork pack that ships inside every " +
                        "search report is the server's own. Running the full " +
                        "search below is a server run and does need one.",
                    color = SentinelMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (pack.isNotEmpty()) {
            item { SectionHeader("Dork pack", pack.size) }
            items(pack) { q ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, SentinelOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Row(
                        Modifier
                            .background(sentinelCardBrush())
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            q,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { clipboard.setText(AnnotatedString(q)) }) {
                            Text("Copy")
                        }
                    }
                }
            }
        }
        item {
            MirrorCard("Run the full search with this pack") {
                PurposeRunPanel(
                    purpose = purpose, onPurpose = { purpose = it },
                    phase = phase, statusLine = statusLine,
                    error = error, notice = notice,
                    runLabel = "Run search",
                    onRun = {
                        scope.launch {
                            phase = RunPhase.SUBMITTING
                            statusLine = "Submitting — the purpose note is recorded with the run…"
                            error = null
                            notice = null
                            try {
                                val body = buildJsonObject {
                                    put("purpose", purpose)
                                    put("identifiers", JsonArray(listOf(buildJsonObject {
                                        put("type", typeKey); put("value", value)
                                    })))
                                    if (city.isNotBlank()) put("city", city)
                                    if (state.isNotBlank()) put("state", state)
                                }
                                val out = viewModel.api.osintCreateSearch(body)
                                val sid = Phase3Logic.int(out, "searchId")
                                if (sid == null) {
                                    phase = RunPhase.FAILED
                                    error = "The server did not queue this search. Try again."
                                    return@launch
                                }
                                phase = RunPhase.RUNNING
                                statusLine = "Queued — waiting for the search worker…"
                                val (rid, fail) = awaitSearch(viewModel, sid) { statusLine = it }
                                when {
                                    rid != null -> {
                                        phase = RunPhase.DONE
                                        statusLine = "Report ready."
                                        onOpenReport(rid, null, false)
                                    }
                                    fail != null -> {
                                        phase = RunPhase.FAILED
                                        error = fail
                                    }
                                    else -> {
                                        phase = RunPhase.IDLE
                                        notice = "Still running on the server — the report " +
                                            "lands under Reports on the deck."
                                    }
                                }
                            } catch (e: Exception) {
                                phase = RunPhase.FAILED
                                error = plainError(e, "That search could not be run. Try again.")
                            }
                        }
                    },
                )
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ---------------------------------------------------------------------
// Image EXIF viewer (fully on-device; no server run, nothing uploaded)
// ---------------------------------------------------------------------

private val EXIF_TAGS: List<String> = listOf(
    ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL, ExifInterface.TAG_DATETIME,
    ExifInterface.TAG_DATETIME_ORIGINAL, ExifInterface.TAG_DATETIME_DIGITIZED,
    ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LATITUDE_REF,
    ExifInterface.TAG_GPS_LONGITUDE, ExifInterface.TAG_GPS_LONGITUDE_REF,
    ExifInterface.TAG_GPS_ALTITUDE, ExifInterface.TAG_IMAGE_WIDTH,
    ExifInterface.TAG_IMAGE_LENGTH, ExifInterface.TAG_ORIENTATION,
    ExifInterface.TAG_SOFTWARE, ExifInterface.TAG_EXPOSURE_TIME,
    ExifInterface.TAG_F_NUMBER, ExifInterface.TAG_ISO_SPEED_RATINGS,
    ExifInterface.TAG_FOCAL_LENGTH, ExifInterface.TAG_FLASH,
    ExifInterface.TAG_WHITE_BALANCE, ExifInterface.TAG_ARTIST,
    ExifInterface.TAG_COPYRIGHT, ExifInterface.TAG_IMAGE_DESCRIPTION,
    // NOTE: TAG_CAMERA_OWNER_NAME / TAG_BODY_SERIAL_NUMBER /
    // TAG_LENS_MAKE / TAG_LENS_MODEL exist only in the androidx
    // ExifInterface, which is not a dependency of this app — the
    // framework android.media.ExifInterface used here has no such
    // constants (CI proved it), so they are not read.
)

@Composable
private fun ExifToolScreen(onBack: () -> Unit) {
    val tool = OsintLogic.tool("exif")!!
    val context = LocalContext.current
    var fileName by remember { mutableStateOf<String?>(null) }
    var rows by remember { mutableStateOf<List<OsintLogic.ExifRow>>(emptyList()) }
    var gpsReadout by remember { mutableStateOf<String?>(null) }
    var picked by remember { mutableStateOf(false) }
    var readError by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            picked = true
            readError = null
            gpsReadout = null
            rows = emptyList()
            fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "photo"
            try {
                val raw = mutableMapOf<String, String?>()
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    for (tag in EXIF_TAGS) {
                        raw[tag] = exif.getAttribute(tag)
                    }
                    val latLon = FloatArray(2)
                    gpsReadout = if (exif.getLatLong(latLon)) {
                        OsintLogic.coordinateReadout(
                            latLon[0].toDouble(), latLon[1].toDouble(),
                        )
                    } else {
                        val lat = OsintLogic.dmsToDecimal(
                            raw[ExifInterface.TAG_GPS_LATITUDE],
                            raw[ExifInterface.TAG_GPS_LATITUDE_REF],
                        )
                        val lon = OsintLogic.dmsToDecimal(
                            raw[ExifInterface.TAG_GPS_LONGITUDE],
                            raw[ExifInterface.TAG_GPS_LONGITUDE_REF],
                        )
                        if (lat != null && lon != null) {
                            OsintLogic.coordinateReadout(lat, lon)
                        } else {
                            null
                        }
                    }
                } ?: throw IllegalStateException("unreadable")
                // Hemisphere refs are consumed by the GPS readout, not rows.
                raw.remove(ExifInterface.TAG_GPS_LATITUDE_REF)
                raw.remove(ExifInterface.TAG_GPS_LONGITUDE_REF)
                rows = OsintLogic.exifRows(raw)
            } catch (e: Exception) {
                readError = "This image could not be read on this phone. Try another file."
            }
        }
    }
    ToolScaffold(tool.title, tool.subtitle, onBack) {
        item {
            MirrorCard("On-device only", topTier = true) {
                Text(
                    "This tool reads the metadata embedded in a photo ON THIS PHONE. " +
                        "Nothing is uploaded, nothing is searched, and no run is logged — " +
                        "it is a local file viewer, not an OSINT search, so it carries " +
                        "no purpose note. Send the photo through a search instead if " +
                        "you need it investigated.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                GradientPrimaryButton("Pick a photo") { picker.launch("image/*") }
                ActionErrorText(readError)
            }
        }
        if (picked && readError == null) {
            item {
                MirrorCard("Metadata — ${fileName ?: "photo"}", topTier = true) {
                    if (rows.isEmpty() && gpsReadout == null) {
                        Text(
                            "No EXIF metadata in this image. Messaging and social apps " +
                                "usually strip it on upload, and screenshots never carry " +
                                "camera EXIF — an empty read says the file has none, " +
                                "nothing more.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        rows.forEach { row -> DetailLine(row.label, row.value) }
                    }
                }
            }
            if (gpsReadout != null) {
                item {
                    MirrorCard("GPS position in this photo") {
                        Text(
                            gpsReadout!!,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleMedium,
                            color = SentinelGreen,
                        )
                        Text(
                            "Coordinates are shown as a technical readout. Map plotting " +
                                "joins this card when the Live Intel map view is wired " +
                                "into the deck (integration pending) — until then the " +
                                "numbers above are the record.",
                            color = SentinelMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ---------------------------------------------------------------------
// Rich report view (payload modules -> source-by-source cards)
// ---------------------------------------------------------------------

@Composable
private fun OsintReportView(
    viewModel: AppViewModel,
    reportId: Int,
    focusSource: String?,
    focusManual: Boolean,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onOpenChildReport: (Int) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = rememberJsonScreen { viewModel.api.osintReport(reportId) }
    // 1.5.2 (Elevation F3): a pivot launched from this report
    // runs under this search's own purpose (carried by the
    // server) and lands in the child run's report, which names
    // this search as its parent.
    var pivotBusy by remember { mutableStateOf(false) }
    var pivotStatus by remember { mutableStateOf("") }
    var pivotError by remember { mutableStateOf<String?>(null) }
    fun runReportPivot(parentSid: Int, pivot: OsintLogic.Pivot) {
        if (pivotBusy) return
        scope.launch {
            pivotBusy = true
            pivotError = null
            pivotStatus = "Submitting the pivot run…"
            try {
                val (rid, fail) = runPivotSearch(viewModel, parentSid, pivot) {
                    pivotStatus = it
                }
                when {
                    rid != null -> onOpenChildReport(rid)
                    fail != null -> pivotError = fail
                    else -> pivotStatus = "Still running on the server — " +
                        "it lands under Searches on the deck."
                }
            } catch (e: Exception) {
                pivotError = plainError(e, "That pivot could not be run. Try again.")
            } finally {
                pivotBusy = false
            }
        }
    }
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 16.dp),
        ) {
            TextButton(onClick = onBack) { Text("‹ Deck") }
            Text("OSINT report", style = MaterialTheme.typography.titleLarge)
        }
        when {
            state.error != null && state.data == null -> ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null -> SkeletonList()
            else -> {
                val data = state.data!!
                val report = Phase3Logic.obj(data, "report") ?: data
                val payload = Phase3Logic.obj(report, "payload")
                val modules = payload?.let { Phase3Logic.arr(it, "modules") } ?: emptyList()
                val kind = payload?.let { Phase3Logic.str(it, "kind") } ?: ""
                val allResults = modules.flatMap { Phase3Logic.arr(it, "results") }
                val anyPossible = allResults.any { r ->
                    OsintLogic.carriesPossibleMatch(
                        Phase3Logic.strOrNull(r, "confidence"),
                        listOf(
                            Phase3Logic.strOrNull(r, "summary"),
                            *Phase3Logic.arr(r, "findings").map {
                                Phase3Logic.strOrNull(it, "detail")
                            }.toTypedArray(),
                        ),
                    )
                }
                val criminalNoHit = kind == "criminal" &&
                    allResults.none { Phase3Logic.str(it, "status") == "found" }
                LazyColumn {
                    item {
                        MirrorCard(Phase3Logic.str(report, "title"), topTier = true) {
                            JsonLines(
                                report, "subject" to "Subject", "summary" to "Summary",
                                "createdByName" to "Created by",
                            )
                            Phase3Logic.long(report, "createdAt")?.let {
                                DetailLine("Created", formatEpochMs(it))
                            }
                            if (anyPossible) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    OsintLogic.POSSIBLE_MATCH_LABEL,
                                    color = SentinelAmber,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "At least one source below returned a possible match. " +
                                        "Verify it on the source itself (DOB / location) " +
                                        "before treating it as the subject's.",
                                    color = SentinelMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            if (criminalNoHit) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    OsintLogic.NO_RECORDS_LABEL + ": " +
                                        allResults.joinToString("; ") {
                                            Phase3Logic.str(it, "source")
                                        } + ".",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            if (focusSource != null) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Opened on the $focusSource leg (breach focus). " +
                                        "The full battery follows below it.",
                                    color = SentinelMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            if (focusManual) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Opened on the manual legs — pre-filled links to work " +
                                        "by hand. Nothing was scraped.",
                                    color = SentinelMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    // 1.5.2 (Elevation F3): lineage + pivot offers,
                    // both read from the report object itself —
                    // the server adds purpose, parentSearchId and
                    // pivots to the full report. A child report
                    // names its parent; a completed parent offers
                    // its pivots. Neither renders when absent.
                    val reportPivots = OsintLogic.parsePivots(report)
                    val reportSearchId = Phase3Logic.int(report, "searchId")
                    val reportParentId = Phase3Logic.int(report, "parentSearchId")
                    val reportPurpose = Phase3Logic.strOrNull(report, "purpose")
                    if (reportParentId != null) {
                        item {
                            MirrorCard("A pivot run") {
                                Text(
                                    "This search is a pivot of search " +
                                        "#$reportParentId. Its purpose was carried " +
                                        "from the parent run by the server" +
                                        (reportPurpose?.let { ": \"$it\"" } ?: "") +
                                        " — a pivot never takes a new purpose " +
                                        "of its own.",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                    if (reportPivots.isNotEmpty() && reportSearchId != null) {
                        item {
                            MirrorCard("Pivot from this result") {
                                Text(
                                    "One tap runs one related leg on the same " +
                                        "identifier. The run uses this search's " +
                                        "purpose — carried over by the server " +
                                        "and logged as a pivot of this search — " +
                                        "so no purpose note is asked for here.",
                                    color = SentinelMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Spacer(Modifier.height(6.dp))
                                PivotActions(
                                    pivots = reportPivots,
                                    busy = pivotBusy,
                                    statusLine = pivotStatus,
                                    error = pivotError,
                                    onRun = { runReportPivot(reportSearchId, it) },
                                )
                            }
                        }
                    }
                    if (modules.isEmpty()) {
                        item {
                            MirrorCard("Report") {
                                Text(
                                    Phase3Logic.str(report, "body"),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    } else {
                        // Focus ordering: the focused source (or the manual
                        // legs) render first; the rest keep server order.
                        val orderedModules = modules.map { m ->
                            val results = Phase3Logic.arr(m, "results")
                            val focused = results.filter { r ->
                                when {
                                    focusSource != null ->
                                        Phase3Logic.str(r, "source").contains(focusSource, true)
                                    focusManual ->
                                        Phase3Logic.str(r, "status") == "manual"
                                    else -> false
                                }
                            }
                            m to (focused + results.filterNot { it in focused })
                        }
                        for ((module, results) in orderedModules) {
                            item {
                                SectionHeader(
                                    Phase3Logic.str(module, "label"),
                                    results.size,
                                )
                            }
                            items(results) { result ->
                                SourceResultCard(result) { url ->
                                    try {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                                        )
                                    } catch (e: Exception) {
                                        // A link the phone cannot open stays
                                        // visible as text on the card.
                                    }
                                }
                            }
                        }
                        val checklist = payload?.let {
                            Phase3Logic.arrRaw(it, "checklist")
                        }?.mapNotNull { (it as? JsonPrimitive)?.content } ?: emptyList()
                        if (checklist.isNotEmpty()) {
                            item { SectionHeader("Checklist — next steps by hand", checklist.size) }
                            item {
                                MirrorCard("Checklist") {
                                    checklist.forEachIndexed { i, line ->
                                        Text("${i + 1}. $line", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        MirrorCard("Footer") {
                            Text(
                                OsintLogic.REPORT_FOOTER,
                                color = SentinelMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (kind == "criminal") {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    OsintLogic.CRIMINAL_FOOTER,
                                    color = SentinelMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row {
                                OutlinedButton(
                                    onClick = {
                                        shareText(
                                            context,
                                            Phase3Logic.str(report, "title"),
                                            Phase3Logic.str(report, "body"),
                                        )
                                    },
                                    modifier = Modifier.padding(end = 8.dp),
                                ) { Text("Share report text") }
                                OutlinedButton(onClick = onDelete) { Text("Delete report") }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceResultCard(result: JsonObject, onOpenUrl: (String) -> Unit) {
    val status = Phase3Logic.str(result, "status")
    val grade = OsintLogic.gradeForStatus(status)
    val confidence = Phase3Logic.strOrNull(result, "confidence")
    val sourceUrl = Phase3Logic.strOrNull(result, "url")
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            1.dp,
            if (grade == OsintLogic.Grade.HIT) {
                gradientHairlineBrush()
            } else {
                androidx.compose.ui.graphics.SolidColor(SentinelOutline)
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Box(Modifier.background(sentinelCardBrush())) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Phase3Logic.str(result, "source"),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    GradePill(grade)
                }
                Text(
                    OsintLogic.confidenceLabel(confidence),
                    color = if (confidence == "possible") SentinelAmber else SentinelMuted,
                    style = MaterialTheme.typography.labelMedium,
                )
                val summary = Phase3Logic.strOrNull(result, "summary")
                if (!summary.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(summary, style = MaterialTheme.typography.bodyMedium)
                }
                Phase3Logic.long(result, "retrievedAt")?.let {
                    Text(
                        "Retrieved " + formatEpochMs(it),
                        color = SentinelMuted,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                val entry = OsintLogic.parseSourceEntry(result)
                val findings = entry.findings
                findings.forEach { f ->
                    Spacer(Modifier.height(8.dp))
                    val fTitle = f.title
                    val fDetail = f.detail
                    val fUrl = f.url
                    if (fTitle.isNotBlank()) {
                        Text(fTitle, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }
                    if (!fDetail.isNullOrBlank()) Text(fDetail, style = MaterialTheme.typography.bodyMedium)
                    val readout = OsintLogic.coordinateReadout(
                        listOfNotNull(fTitle, fDetail).joinToString(" "),
                    )
                    if (readout != null) {
                        Text(
                            readout,
                            color = SentinelGreen,
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    if (!fUrl.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                fUrl,
                                color = SentinelMuted,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            if (fUrl.startsWith("http")) {
                                TextButton(onClick = { onOpenUrl(fUrl) }) { Text("Open") }
                            }
                        }
                    }
                }
                // IP intelligence: plot ONLY when the payload itself
                // carries coordinates (OsintLogic.ipIntelCoordinates) —
                // the current server payload reports location as text,
                // so this stays empty rather than inventing a pin.
                val ipPoint = OsintLogic.ipIntelCoordinates(result)
                if (ipPoint != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Payload coordinates — plotted exactly as reported",
                        color = SentinelMuted,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    SentinelMap(
                        markers = listOf(
                            IntelMapLogic.SimpleMarker(
                                markerId = "ipintel",
                                lat = ipPoint.first,
                                lon = ipPoint.second,
                                title = Phase3Logic.str(result, "source"),
                                detailLines = listOf(
                                    "Position" to OsintLogic.coordinateReadout(
                                        ipPoint.first, ipPoint.second,
                                    ),
                                ),
                                pulse = true,
                                pulseBaseRadiusMeters = 20_000.0,
                            ),
                        ),
                        onMarkerTap = {},
                        accent = SentinelGreen,
                        frameCenter = IntelMapLogic.MapPoint(ipPoint.first, ipPoint.second),
                        frameZoom = 9.0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .padding(top = 6.dp),
                    )
                }
                val note = Phase3Logic.strOrNull(result, "note")
                if (!note.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Note: $note",
                        color = SentinelMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (!sourceUrl.isNullOrBlank() && findings.isEmpty()) {
                    TextButton(onClick = { onOpenUrl(sourceUrl) }) { Text("Open source") }
                }
                if (grade == OsintLogic.Grade.MANUAL) {
                    Text(
                        "MANUAL — open the links by hand; nothing was scraped.",
                        color = SentinelMuted,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Box(Modifier.align(Alignment.TopCenter)) { CardTopHighlight() }
        }
    }
}

// ---------------------------------------------------------------------
// Breach Intel
// ---------------------------------------------------------------------

@Composable
fun BreachIntelScreen(viewModel: AppViewModel, nav: NavController) {
    var sector by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var detailId by remember { mutableStateOf<Int?>(null) }
    var editing by remember { mutableStateOf<Int?>(null) }
    var creating by remember { mutableStateOf(false) }
    // The register payload returns the full list (server caps at 300);
    // the search field below filters it locally on this phone. The
    // app endpoint also accepts a q parameter, but the handler does
    // not apply it — so no server search is claimed here.
    val state = rememberJsonScreen { viewModel.api.breachIntel(sector = sector) }
    LaunchedEffect(sector) { state.reloadKey++ }

    detailId?.let { id ->
        BreachDetailView(
            viewModel = viewModel,
            breachId = id,
            onBack = { detailId = null; state.reloadKey++ },
            onEdit = { editing = id; detailId = null },
            onChanged = { state.reloadKey++ },
        )
        if (creating || editing != null) {
            BreachFormSheet(viewModel, editing, state.data,
                onClose = { creating = false; editing = null },
                onSaved = { creating = false; editing = null; state.reloadKey++ })
        }
        return
    }

    Column {
        MirrorHeader("Breach Intel", MirrorCopy.subtitle("breachintel")) {
            Button(onClick = { creating = true }) { Text("Record a breach") }
        }
        state.data?.let { data ->
            val sectors = Phase3Logic.arrRaw(data, "sectors")
                .mapNotNull { (it as? JsonPrimitive)?.content }
            ChipRow(
                options = listOf("all" to "All sectors") + sectors.map { it to it },
                selected = sector ?: "all",
                onPick = { sector = it },
            )
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            TextFieldRow("Search the register (on this phone)", query, onChange = { query = it })
        }
        when {
            state.error != null && state.data == null -> ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null -> SkeletonList()
            else -> {
                val data = state.data!!
                val stats = Phase3Logic.obj(data, "stats")
                LazyColumn {
                    if (stats != null) {
                        item {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                            ) {
                                StatCard(
                                    "Total breaches",
                                    Phase3Logic.str(stats, "totalBreaches"),
                                    Modifier.weight(1f),
                                )
                                StatCard(
                                    "Breaches this month",
                                    Phase3Logic.str(stats, "breachesThisMonth"),
                                    Modifier.weight(1f),
                                )
                            }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                            ) {
                                StatCard(
                                    "Records this month",
                                    Phase3Logic.str(stats, "recordsThisMonth"),
                                    Modifier.weight(1f),
                                )
                                StatCard(
                                    "Circulating publicly",
                                    Phase3Logic.str(stats, "circulatingPublicly"),
                                    Modifier.weight(1f),
                                )
                            }
                            MirrorCard("Boundary — circulation intel") {
                                Phase3Logic.strOrNull(data, "boundaryCopy")?.let {
                                    Text(it, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(
                                    "Venue links are never stored — a venue is a NAME as " +
                                        "reported; the citation is the reporting about it.",
                                    color = SentinelMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    val allRows = Phase3Logic.arr(data, "breaches")
                    val rows = if (query.isBlank()) {
                        allRows
                    } else {
                        allRows.filter { b ->
                            listOf("org", "sector", "summary", "attackVector", "severity")
                                .any { k ->
                                    Phase3Logic.str(b, k).contains(query, ignoreCase = true)
                                }
                        }
                    }
                    item { SectionHeader("Breaches", rows.size) }
                    if (rows.isEmpty()) {
                        item { EmptyBox("No breaches recorded", "Breach entries appear here.") }
                    }
                    itemsIndexed(rows) { i, breach ->
                        BreachCard(breach, i) { detailId = Phase3Logic.int(breach, "id") }
                    }
                    val runs = Phase3Logic.arr(data, "intakeRuns")
                    item { SectionHeader("Intake runs", runs.size) }
                    item {
                        MirrorCard("Breach intake") {
                            Text(
                                "Queue a collection run over the register's public sources. " +
                                    "Runs are activity-logged; results land as register " +
                                    "updates below.",
                                color = SentinelMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Spacer(Modifier.height(6.dp))
                            BreachIntakeButton(viewModel) { state.reloadKey++ }
                        }
                    }
                    itemsIndexed(runs) { i, runRow ->
                        ItemRow(
                            title = "Intake run #${Phase3Logic.int(runRow, "id") ?: ""}",
                            subtitle = listOfNotNull(
                                Phase3Logic.strOrNull(runRow, "status"),
                                Phase3Logic.int(runRow, "added")?.let { "added $it" },
                                Phase3Logic.int(runRow, "updated")?.let { "updated $it" },
                                Phase3Logic.int(runRow, "skipped")?.let { "skipped $it" },
                                Phase3Logic.strOrNull(runRow, "note"),
                            ).joinToString(" · "),
                            badge = Phase3Logic.strOrNull(runRow, "status"),
                            entranceIndex = i,
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
    if (creating || editing != null) {
        BreachFormSheet(viewModel, editing, state.data,
            onClose = { creating = false; editing = null },
            onSaved = { creating = false; editing = null; state.reloadKey++ })
    }
}

@Composable
private fun BreachIntakeButton(viewModel: AppViewModel, onQueued: () -> Unit) {
    val scope = rememberCoroutineScope()
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    Column {
        GradientPrimaryButton("Run breach intake") {
            scope.launch {
                try {
                    viewModel.api.breachIntakeRun()
                    notice = "Breach intake queued."
                    error = null
                    onQueued()
                } catch (e: Exception) {
                    error = plainError(e, "Intake could not be queued. Try again.")
                }
            }
        }
        ActionErrorText(error)
        if (notice != null) Text(notice!!)
    }
}

@Composable
private fun BreachCard(breach: JsonObject, entranceIndex: Int, onClick: () -> Unit) {
    val severity = Phase3Logic.str(breach, "severity")
    val circ = Phase3Logic.arrRaw(breach, "circStatuses")
        .mapNotNull { (it as? JsonPrimitive)?.content }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            1.dp,
            if (OsintLogic.severityRank(severity) >= 3) {
                gradientHairlineBrush()
            } else {
                androidx.compose.ui.graphics.SolidColor(SentinelOutline)
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .listEntrance(entranceIndex)
            .sentinelClickable { onClick() },
    ) {
        Box(Modifier.background(sentinelCardBrush())) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Phase3Logic.str(breach, "org"),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    SeverityPill(severity)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    listOfNotNull(
                        Phase3Logic.strOrNull(breach, "sector"),
                        Phase3Logic.int(breach, "recordsAffected")?.let { "$it records" },
                        Phase3Logic.strOrNull(breach, "disclosureDate")?.let { "disclosed $it" },
                        Phase3Logic.strOrNull(breach, "attackVector"),
                    ).joinToString(" · "),
                    color = SentinelMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (circ.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        circ.distinct().forEach { MirrorChip(it) }
                    }
                }
            }
            Box(Modifier.align(Alignment.TopCenter)) { CardTopHighlight() }
        }
    }
}

@Composable
private fun BreachDetailView(
    viewModel: AppViewModel, breachId: Int,
    onBack: () -> Unit, onEdit: () -> Unit, onChanged: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var venueType by remember { mutableStateOf("") }
    var venueName by remember { mutableStateOf("") }
    var circStatus by remember { mutableStateOf("") }
    var askingPrice by remember { mutableStateOf("") }
    var firstSeen by remember { mutableStateOf("") }
    var confidence by remember { mutableStateOf("") }
    var sourceUrl by remember { mutableStateOf("") }
    var sourceLabel by remember { mutableStateOf("") }
    val state = rememberJsonScreen { viewModel.api.breachIntel(breachId = breachId) }
    val run: (suspend () -> String?) -> Unit = { block ->
        scope.launch {
            try {
                notice = block(); error = null; state.reloadKey++; onChanged()
            } catch (e: Exception) {
                error = plainError(e, "That could not be done. Try again.")
            }
        }
    }
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 16.dp),
        ) {
            TextButton(onClick = onBack) { Text("‹ Breach Intel") }
            Text("Breach detail", style = MaterialTheme.typography.titleLarge)
        }
        when {
            state.error != null && state.data == null -> ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null -> SkeletonList()
            else -> {
                val data = state.data!!
                val detail = Phase3Logic.obj(data, "detail")
                if (detail == null) {
                    EmptyBox("Not found", "This breach could not be found.")
                } else {
                    val severity = Phase3Logic.str(detail, "severity")
                    LazyColumn {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                border = BorderStroke(1.dp, gradientHairlineBrush()),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                            ) {
                                Box(Modifier.background(sentinelCardBrush(SentinelSurfaceTop))) {
                                    Column(Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                Phase3Logic.str(detail, "org"),
                                                style = MaterialTheme.typography.titleLarge,
                                                modifier = Modifier.weight(1f),
                                            )
                                            SeverityPill(severity)
                                        }
                                        Text(
                                            "Severity is the register's deterministic grade " +
                                                "(records scale, sensitive data classes, " +
                                                "circulation status) — recomputed whenever " +
                                                "circulation intel changes.",
                                            color = SentinelMuted,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                        JsonLines(
                                            detail, "sector" to "Sector",
                                            "incidentDate" to "Incident date",
                                            "disclosureDate" to "Disclosure date",
                                            "attackVector" to "Attack vector",
                                            "summary" to "Summary",
                                            "createdByName" to "Recorded by",
                                        )
                                        DetailLine(
                                            "Records affected",
                                            Phase3Logic.int(detail, "recordsAffected")?.toString(),
                                        )
                                        val classes = Phase3Logic.arrRaw(detail, "dataClasses")
                                            .mapNotNull { (it as? JsonPrimitive)?.content }
                                        if (classes.isNotEmpty()) {
                                            DetailLine("Data classes", classes.joinToString(", "))
                                        }
                                    }
                                    Box(Modifier.align(Alignment.TopCenter)) { CardTopHighlight() }
                                }
                            }
                        }
                        val sources = Phase3Logic.arr(detail, "sources")
                        if (sources.isNotEmpty()) {
                            item { SectionHeader("Sources", sources.size) }
                            items(sources) { source ->
                                ItemRow(
                                    title = Phase3Logic.str(source, "label"),
                                    subtitle = Phase3Logic.strOrNull(source, "url"),
                                )
                            }
                        }
                        val circs = Phase3Logic.arr(detail, "circulation")
                        item { SectionHeader("Circulation", circs.size) }
                        if (circs.isEmpty()) {
                            item {
                                EmptyBox(
                                    "No circulation intel",
                                    "Nothing reputable has reported this data circulating.",
                                )
                            }
                        }
                        items(circs) { circ ->
                            val cid = Phase3Logic.int(circ, "id")
                            MirrorCard(
                                listOfNotNull(
                                    Phase3Logic.strOrNull(circ, "venueType"),
                                    Phase3Logic.strOrNull(circ, "venueName"),
                                ).joinToString(" — "),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SeverityPill(Phase3Logic.str(circ, "status"))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Confidence: " +
                                            (Phase3Logic.strOrNull(circ, "confidence")
                                                ?: "as publicly reported"),
                                        color = SentinelMuted,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                JsonLines(
                                    circ, "askingPrice" to "Asking price",
                                    "firstSeen" to "First seen",
                                    "intelSourceLabel" to "Intel source",
                                    "intelSourceUrl" to "Intel source URL",
                                    "recordedByName" to "Recorded by",
                                )
                                cid?.let {
                                    OutlinedButton(onClick = {
                                        run {
                                            viewModel.api.breachCirculationDelete(it)
                                            "Circulation deleted."
                                        }
                                    }) { Text("Delete circulation") }
                                }
                            }
                        }
                        item {
                            MirrorCard("Record circulation intel") {
                                Text(
                                    "Record the venue NAME as reported — never a link to " +
                                        "the leak itself. The citation is the reporting " +
                                        "about it; the server refuses venue links.",
                                    color = SentinelMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                val venueTypes = Phase3Logic.arrRaw(data, "venueTypes")
                                    .mapNotNull { (it as? JsonPrimitive)?.content }
                                val circStatuses = Phase3Logic.arrRaw(data, "circStatuses")
                                    .mapNotNull { (it as? JsonPrimitive)?.content }
                                if (venueTypes.isNotEmpty()) {
                                    ChoiceMenu(
                                        "Venue type",
                                        venueType.ifBlank { venueTypes.first() },
                                        venueTypes,
                                    ) { venueType = it }
                                }
                                TextFieldRow("Venue name (never a link to the leak)", venueName,
                                    onChange = { venueName = it })
                                if (circStatuses.isNotEmpty()) {
                                    ChoiceMenu(
                                        "Status",
                                        circStatus.ifBlank { circStatuses.first() },
                                        circStatuses,
                                    ) { circStatus = it }
                                }
                                TextFieldRow("Asking price (optional)", askingPrice,
                                    onChange = { askingPrice = it })
                                DateFieldRow("First seen (optional)", firstSeen,
                                    onChange = { firstSeen = it })
                                TextFieldRow("Confidence (optional)", confidence,
                                    onChange = { confidence = it })
                                TextFieldRow("Intel source URL (the reporting)", sourceUrl,
                                    onChange = { sourceUrl = it })
                                TextFieldRow("Intel source label", sourceLabel,
                                    onChange = { sourceLabel = it })
                                Button(onClick = {
                                    run {
                                        viewModel.api.breachCirculationCreate(breachId, buildJsonObject {
                                            put("venueType", venueType.ifBlank { venueTypes.firstOrNull() ?: "" })
                                            put("venueName", venueName)
                                            put("status", circStatus.ifBlank { circStatuses.firstOrNull() ?: "" })
                                            put("askingPrice", askingPrice)
                                            put("firstSeen", firstSeen)
                                            put("confidence", confidence)
                                            put("intelSourceUrl", sourceUrl)
                                            put("intelSourceLabel", sourceLabel)
                                        })
                                        "Circulation recorded."
                                    }
                                }) { Text("Record circulation intel") }
                                ActionErrorText(error)
                                if (notice != null) Text(notice!!)
                            }
                        }
                        item {
                            MirrorCard("Actions") {
                                Row {
                                    OutlinedButton(
                                        onClick = onEdit,
                                        modifier = Modifier.padding(end = 8.dp),
                                    ) { Text("Edit breach") }
                                    OutlinedButton(onClick = { confirmDelete = true }) {
                                        Text("Delete breach")
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                BreachIntakeButton(viewModel) { state.reloadKey++ }
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        ConfirmGate("Delete breach", "This permanently deletes this breach entry and its circulation intel.", "Delete",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                run { viewModel.api.breachDelete(breachId); "Breach deleted." }
                onBack()
            })
    }
}

@Composable
private fun BreachFormSheet(
    viewModel: AppViewModel, breachId: Int?, workspace: JsonObject?,
    onClose: () -> Unit, onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var org by remember { mutableStateOf("") }
    var sector by remember { mutableStateOf("") }
    var incidentDate by remember { mutableStateOf("") }
    var disclosureDate by remember { mutableStateOf("") }
    var records by remember { mutableStateOf("") }
    var attackVector by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var dataClasses by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(breachId) {
        if (breachId != null) {
            try {
                val data = viewModel.api.breachIntel(breachId = breachId)
                val detail = Phase3Logic.obj(data, "detail")
                if (detail != null) {
                    org = Phase3Logic.str(detail, "org")
                    sector = Phase3Logic.str(detail, "sector")
                    incidentDate = Phase3Logic.str(detail, "incidentDate")
                    disclosureDate = Phase3Logic.str(detail, "disclosureDate")
                    records = Phase3Logic.int(detail, "recordsAffected")?.toString() ?: ""
                    attackVector = Phase3Logic.str(detail, "attackVector")
                    summary = Phase3Logic.str(detail, "summary")
                    dataClasses = Phase3Logic.arrRaw(detail, "dataClasses")
                        .mapNotNull { (it as? JsonPrimitive)?.content }
                        .joinToString(", ")
                }
            } catch (e: Exception) {
                error = plainError(e, "Couldn't load this breach. Try again.")
            }
        }
    }
    val sectors = workspace?.let {
        Phase3Logic.arrRaw(it, "sectors")
            .mapNotNull { el -> (el as? JsonPrimitive)?.content }
    } ?: emptyList()
    val knownClasses = workspace?.let {
        Phase3Logic.arrRaw(it, "dataClasses")
            .mapNotNull { el -> (el as? JsonPrimitive)?.content }
    } ?: emptyList()
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (breachId == null) "Record a breach" else "Edit breach") },
        text = {
            Column {
                if (sectors.isNotEmpty()) {
                    ChoiceMenu("Sector", sector.ifBlank { sectors.first() }, sectors) { sector = it }
                } else {
                    TextFieldRow("Sector", sector, onChange = { sector = it })
                }
                TextFieldRow("Organization", org, onChange = { org = it })
                DateFieldRow("Incident date", incidentDate, onChange = { incidentDate = it })
                DateFieldRow("Disclosure date", disclosureDate, onChange = { disclosureDate = it })
                TextFieldRow("Records affected", records, onChange = { records = it })
                TextFieldRow("Data classes (comma list)", dataClasses, onChange = { dataClasses = it })
                if (knownClasses.isNotEmpty()) {
                    Text(
                        "Known classes: " + knownClasses.joinToString(", ") +
                            " — the server keeps only these.",
                        color = SentinelMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextFieldRow("Attack vector", attackVector, onChange = { attackVector = it })
                TextFieldRow("Summary", summary, onChange = { summary = it }, singleLine = false, minLines = 3)
                ActionErrorText(error)
            }
        },
        confirmButton = {
            Button(onClick = {
                scope.launch {
                    try {
                        val body = buildJsonObject {
                            put("org", org)
                            put("sector", sector.ifBlank { sectors.firstOrNull() ?: "" })
                            put("incidentDate", incidentDate)
                            put("disclosureDate", disclosureDate)
                            records.toIntOrNull()?.let { put("recordsAffected", it) }
                            put("dataClasses", JsonArray(dataClasses.split(",")
                                .map { it.trim() }.filter { it.isNotBlank() }
                                .map { JsonPrimitive(it) }))
                            put("attackVector", attackVector)
                            put("summary", summary)
                        }
                        if (breachId == null) viewModel.api.breachCreate(body)
                        else viewModel.api.breachEdit(breachId, body)
                        onSaved()
                    } catch (e: Exception) {
                        error = plainError(e, "That could not be saved. Try again.")
                    }
                }
            }) { Text("Save") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onClose) { Text("Cancel") }
        },
    )
}

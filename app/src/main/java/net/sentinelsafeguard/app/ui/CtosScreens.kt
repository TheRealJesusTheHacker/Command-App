package net.sentinelsafeguard.app.ui

import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.data.api.ctosCase
import net.sentinelsafeguard.app.data.api.ctosCaseReport
import net.sentinelsafeguard.app.data.api.ctosClearCase
import net.sentinelsafeguard.app.data.api.ctosCreateCase
import net.sentinelsafeguard.app.data.api.ctosDeleteCase
import net.sentinelsafeguard.app.data.api.ctosExportCase
import net.sentinelsafeguard.app.data.api.ctosRecord
import net.sentinelsafeguard.app.data.api.ctosRunTool
import net.sentinelsafeguard.app.data.api.ctosRunToolAsync
import net.sentinelsafeguard.app.data.api.ctosSaveProfile
import net.sentinelsafeguard.app.data.api.ctosToolRun
import net.sentinelsafeguard.app.data.api.ctosWorkspace
import net.sentinelsafeguard.app.ui.theme.SentinelAmber
import net.sentinelsafeguard.app.ui.theme.SentinelGreen
import net.sentinelsafeguard.app.ui.theme.SentinelMuted
import net.sentinelsafeguard.app.ui.theme.SentinelSurfaceHigh

/**
 * CToS — the native "OSINT tools" section (Command App 1.7.0,
 * CTOS-APP-2026-11-11; BUILD-PLAN section 6). A case bar, a
 * tool deck driven by the server's own tool metadata, and
 * structured native results — no WebView, no raw JSON dumps.
 *
 * Estate rails, in the UI and not just the docs:
 *  - every run rides the server twins, so results ARE the
 *    web's results and the server's activity log, rate limits
 *    and SSRF guards hold exactly as on web;
 *  - statuses keep the server's honesty semantics — found /
 *    not found / unknown / unavailable / not configured are
 *    distinct pills, and unknown is never shown as a negative;
 *  - source API keys are never asked for, accepted, or shown
 *    here: the premium-sources card shows configured state
 *    only and points at web Command settings;
 *  - the File Hash Generator computes on this phone — the
 *    file never leaves it, exactly as the web computes it in
 *    the browser; the Metadata Extractor's file goes to the
 *    server action, which parses it in memory and stores
 *    nothing (the web's own posture, stated on the panel).
 */

private val prettyJson = Json { prettyPrint = true }

private fun openUrl(context: android.content.Context, url: String) {
    // AUDIT-171 M-05 [OSINT]: finding/source URLs are server-
    // collected third-party data — only absolute http(s) URLs
    // reach the intent resolver (a crafted intent:/file: value
    // must never be handed to Android by a tap).
    val openable = OsintLogic.openableUrl(url) ?: return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(openable)))
    } catch (ignored: Exception) {
        // No browser able to take it — the URL stays on screen.
    }
}

@Composable
private fun StatusChip(status: CtosLogic.Status) {
    val kind = when (status) {
        CtosLogic.Status.FOUND -> "paid"
        CtosLogic.Status.NOT_FOUND -> "muted"
        CtosLogic.Status.MANUAL -> "outline"
        else -> "amber"
    }
    StatusPill(status.label.uppercase(), kind)
}

@Composable
private fun ChipRow(
    options: List<String>,
    selected: String?,
    onPick: (String?) -> Unit,
    allLabel: String? = null,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        if (allLabel != null) {
            item {
                val on = selected == null
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (on) SentinelGreen.copy(alpha = 0.16f)
                        else SentinelSurfaceHigh,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    onClick = { onPick(null) },
                ) {
                    Text(
                        allLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
        items(options) { opt ->
            val on = selected == opt
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (on) SentinelGreen.copy(alpha = 0.16f)
                    else SentinelSurfaceHigh,
                ),
                shape = RoundedCornerShape(10.dp),
                onClick = { onPick(opt) },
            ) {
                Text(
                    opt,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------
// Deck home: case bar + tool deck + premium sources
// ---------------------------------------------------------------------

@Composable
fun CtosScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val state = rememberJsonScreen { viewModel.api.ctosWorkspace() }
    var selectedCaseId by remember { mutableStateOf<Int?>(null) }
    var openCaseId by remember { mutableStateOf<Int?>(null) }
    var openTool by remember { mutableStateOf<CtosLogic.ToolMeta?>(null) }
    var showNewCase by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    openTool?.let { tool ->
        CtosToolScreen(
            viewModel = viewModel,
            tool = tool,
            cases = CtosLogic.parseCases(state.data),
            initialCaseId = selectedCaseId,
            workspace = state.data,
            onBack = { openTool = null; state.reloadKey++ },
        )
        return
    }
    openCaseId?.let { cid ->
        CtosCaseScreen(
            viewModel = viewModel,
            caseId = cid,
            findingTypes = CtosLogic.findingTypes(state.data),
            onBack = { openCaseId = null; state.reloadKey++ },
            // PERFECTION MIRROR R-1: the case answered with the
            // gone refusal — the deck takes over with the
            // refreshed (scoped) list and the server's sentence
            // as the notice, the deleted-case end state.
            onGone = {
                openCaseId = null
                selectedCaseId = null
                notice = CtosLogic.CASE_GONE_MESSAGE
                error = null
                state.reloadKey++
            },
        )
        return
    }

    val run: (suspend () -> String?) -> Unit = { block ->
        scope.launch {
            try {
                notice = block()
                error = null
                state.reloadKey++
            } catch (e: Exception) {
                // PERFECTION MIRROR R-1: a case-scoped call that
                // answers with the gone body (export / clear /
                // delete of a case that is gone or another
                // operator's) lands in the same end state as a
                // deleted case — refreshed list, the server's
                // sentence as the notice — never an error dump.
                if (CtosLogic.isCaseGone(e)) {
                    notice = CtosLogic.CASE_GONE_MESSAGE
                    error = null
                    selectedCaseId = null
                    state.reloadKey++
                } else {
                    error = plainError(e, "That could not be done. Try again.")
                }
            }
        }
    }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 16.dp),
        ) {
            TextButton(onClick = onBack) { Text("‹ OSINT") }
            Column(Modifier.weight(1f)) {
                Text("OSINT tools", style = MaterialTheme.typography.titleLarge)
                Text(
                    "The CToS working deck — cases, findings, and the tool deck.",
                    color = SentinelMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        when {
            state.error != null && state.data == null ->
                ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null -> SkeletonList()
            else -> {
                val data = state.data!!
                val cases = CtosLogic.parseCases(data)
                val tools = CtosLogic.parseTools(data)
                if (selectedCaseId != null && cases.none { it.id == selectedCaseId }) {
                    selectedCaseId = null
                }
                val selected = cases.firstOrNull { it.id == selectedCaseId }
                LazyColumn {
                    item {
                        MirrorCard("Case", selected?.findings, topTier = true) {
                            if (cases.isEmpty()) {
                                Text(
                                    "No cases yet. A case holds one target; tool runs " +
                                        "against it collect findings and artifacts you " +
                                        "can export as one document.",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            } else {
                                // PERFECTION MIRROR R-5: the count
                                // is the server's total (casesTotal)
                                // beside the rows it sent. The app
                                // loads all of them (no paging
                                // params — the defaults reproduce
                                // the full scoped list), and the
                                // line shows both numbers if they
                                // ever disagree.
                                Text(
                                    CtosLogic.caseCountLine(
                                        CtosLogic.casesTotal(data), cases.size,
                                    ),
                                    color = SentinelMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                ChipRow(
                                    // AUDIT-171 B-07: chips carry the
                                    // case id — titles collide by
                                    // construction (they default to
                                    // the target).
                                    options = cases.map { CtosLogic.caseChipLabel(it) },
                                    selected = selected?.let { CtosLogic.caseChipLabel(it) },
                                    onPick = { chip ->
                                        selectedCaseId =
                                            chip?.let { CtosLogic.caseFromChip(cases, it)?.id }
                                    },
                                )
                                if (selected != null) {
                                    DetailLine("Target", selected.target)
                                    if (selected.targetType.isNotBlank()) {
                                        DetailLine("Type", selected.targetType)
                                    }
                                    DetailLine(
                                        "Findings / artifacts",
                                        "${selected.findings} / ${selected.artifacts}",
                                    )
                                    DetailLine(
                                        "Updated",
                                        if (selected.updatedAt > 0) {
                                            formatEpochMs(selected.updatedAt)
                                        } else {
                                            null
                                        },
                                    )
                                    if (selected.note.isNotBlank()) {
                                        Text(
                                            selected.note,
                                            color = SentinelMuted,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                } else {
                                    Text(
                                        "Pick a case to aim tool runs at it — or run " +
                                            "tools case-free from the deck below.",
                                        color = SentinelMuted,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                            ActionErrorText(error)
                            if (notice != null) Text(notice!!)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GradientPrimaryButton(
                                    text = "New case",
                                    onClick = { showNewCase = true },
                                )
                                if (selected != null) {
                                    OutlinedButton(onClick = { openCaseId = selected.id }) {
                                        Text("Open case")
                                    }
                                }
                            }
                            if (selected != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = {
                                        run {
                                            val out = viewModel.api.ctosExportCase(selected.id)
                                            val exp = out["export"] ?: out
                                            shareText(
                                                context,
                                                "CToS case — ${selected.title}",
                                                prettyJson.encodeToString(
                                                    JsonElement.serializer(), exp,
                                                ),
                                            )
                                            "Export shared."
                                        }
                                    }) { Text("Share export") }
                                    TextButton(onClick = { confirmClear = true }) {
                                        Text("Clear")
                                    }
                                    TextButton(onClick = { confirmDelete = true }) {
                                        Text("Delete")
                                    }
                                }
                            }
                        }
                    }
                    for (group in CtosLogic.groupsPresent(tools)) {
                        item { SectionHeader(group.title, CtosLogic.toolsInGroup(tools, group).size) }
                        item {
                            Text(
                                group.subtitle,
                                color = SentinelMuted,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                        items(CtosLogic.toolsInGroup(tools, group)) { tool ->
                            ItemRow(
                                title = tool.label,
                                subtitle = tool.desc,
                                badge = when {
                                    // AUDIT-171 B-17: gated active
                                    // tools are badged as such —
                                    // they used to look identical
                                    // to passive lookups.
                                    tool.gated -> "AUTHORIZATION GATE"
                                    tool.kind == "builder" -> "BUILDER"
                                    tool.kind == "browser" -> "ON-DEVICE"
                                    tool.network -> null
                                    else -> "LOCAL"
                                },
                                onClick = { openTool = tool },
                            )
                        }
                    }
                    // W-5 (1.7.3): the key-status surface — status
                    // words only. The payload carries booleans,
                    // never values; a state the server did not
                    // report renders "Status unavailable", never a
                    // guessed configured / not-configured.
                    item { SectionHeader("Premium sources") }
                    item {
                        MirrorCard("Source API keys") {
                            Text(
                                "Keys deepen tools that already work on free sources. " +
                                    "They are set in web Command settings by the " +
                                    "Owner — the app never asks for, accepts, or " +
                                    "stores a key.",
                                color = SentinelMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Spacer(Modifier.height(6.dp))
                            if (!CtosLogic.keySourcesReported(data)) {
                                Text(
                                    "Key status is unavailable — the server did " +
                                        "not report it in this workspace, and " +
                                        "nothing is guessed here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            } else {
                                CtosLogic.parseKeySources(data).forEach { ks ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                    ) {
                                        Text(ks.label, modifier = Modifier.weight(1f))
                                        StatusPill(
                                            CtosLogic.keyStatusWord(ks.status)
                                                .uppercase(Locale.US),
                                            when (ks.status) {
                                                CtosLogic.KeyStatus.CONFIGURED -> "paid"
                                                CtosLogic.KeyStatus.NOT_CONFIGURED -> "muted"
                                                CtosLogic.KeyStatus.UNAVAILABLE -> "amber"
                                            },
                                        )
                                    }
                                    CtosLogic.keyGuidance(ks)?.let { line ->
                                        Text(
                                            line,
                                            color = SentinelMuted,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(bottom = 3.dp),
                                        )
                                    }
                                }
                            }
                            CtosLogic.catalogLine(data)?.let { line ->
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    line,
                                    color = SentinelMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    // AUDIT-171 B-17 [OSINT]: the authorization gate
                    // rides every workspace payload and was never
                    // rendered. The registry state is shown here;
                    // managing it stays owner-only on the web (the
                    // payload's own canManage flag says who may).
                    CtosLogic.parseGate(data)?.let { gate ->
                        item { SectionHeader("Authorization gate", gate.entries.size) }
                        item {
                            MirrorCard("Registered targets") {
                                if (gate.rule.isNotBlank()) {
                                    Text(
                                        gate.rule,
                                        color = SentinelMuted,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                }
                                if (gate.entries.isEmpty()) {
                                    Text(
                                        "No targets are registered. Gated tools " +
                                            "(Pentest Suite, Bug Bounty Hunter, Proxy " +
                                            "Scanner) refuse unregistered targets.",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                gate.entries.forEach { entry ->
                                    DetailLine(
                                        entry.target,
                                        listOf(
                                            entry.basisKind,
                                            entry.status,
                                            entry.clientLabel,
                                        ).filter { it.isNotBlank() }.joinToString(" · "),
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    if (gate.canManage) {
                                        "You are the owner — the registry is managed " +
                                            "in web Command (Gate settings)."
                                    } else {
                                        "The registry is managed by the Owner in web Command."
                                    },
                                    color = SentinelMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    if (showNewCase) {
        NewCaseDialog(
            onDismiss = { showNewCase = false },
            onCreate = { body ->
                showNewCase = false
                run {
                    val out = viewModel.api.ctosCreateCase(body)
                    selectedCaseId = Phase3Logic.int(
                        Phase3Logic.obj(out, "case"), "id",
                    ) ?: selectedCaseId
                    "Case created."
                }
            },
        )
    }
    if (confirmClear) {
        val sel = CtosLogic.parseCases(state.data).firstOrNull { it.id == selectedCaseId }
        ConfirmGate(
            "Clear case",
            "This empties ${sel?.title ?: "the case"}'s findings and artifacts. " +
                "The case itself stays.",
            "Clear",
            onDismiss = { confirmClear = false },
            onConfirm = {
                confirmClear = false
                sel?.let { c -> run { viewModel.api.ctosClearCase(c.id); "Case cleared." } }
            },
        )
    }
    if (confirmDelete) {
        val sel = CtosLogic.parseCases(state.data).firstOrNull { it.id == selectedCaseId }
        ConfirmGate(
            "Delete case",
            "This permanently deletes ${sel?.title ?: "the case"} with all its " +
                "findings and artifacts.",
            "Delete",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                sel?.let { c ->
                    selectedCaseId = null
                    run { viewModel.api.ctosDeleteCase(c.id); "Case deleted." }
                }
            },
        )
    }
}

@Composable
private fun NewCaseDialog(
    onDismiss: () -> Unit,
    onCreate: (JsonObject) -> Unit,
) {
    var target by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var targetType by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New case") },
        text = {
            Column {
                TextFieldRow("Target (domain, IP, email, username…)", target,
                    onChange = { target = it })
                TextFieldRow("Title (optional — defaults to the target)", title,
                    onChange = { title = it })
                TextFieldRow("Target type (optional)", targetType,
                    onChange = { targetType = it })
                TextFieldRow("Note (optional)", note,
                    onChange = { note = it }, singleLine = false, minLines = 2)
            }
        },
        confirmButton = {
            Button(
                enabled = target.isNotBlank(),
                onClick = {
                    onCreate(buildJsonObject {
                        put("target", target.trim())
                        if (title.isNotBlank()) put("title", title.trim())
                        if (targetType.isNotBlank()) put("targetType", targetType.trim())
                        if (note.isNotBlank()) put("note", note.trim())
                    })
                },
            ) { Text("Create case") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

// ---------------------------------------------------------------------
// Case view: findings (filtered) + artifacts + manual record
// ---------------------------------------------------------------------

@Composable
private fun CtosCaseScreen(
    viewModel: AppViewModel,
    caseId: Int,
    findingTypes: List<String>,
    onBack: () -> Unit,
    onGone: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val ftypeState = remember { mutableStateOf<String?>(null) }
    val ftype = ftypeState.value
    val state = rememberJsonScreen {
        viewModel.api.ctosCase(caseId, ftypeState.value)
    }
    var showFinding by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }
    // AUDIT-171 B-18/B-20: the printable case report, and the
    // artifact whose content is being read.
    var showReport by remember { mutableStateOf(false) }
    var openArtifact by remember { mutableStateOf<CtosLogic.ArtifactRow?>(null) }
    var artifactContent by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val run: (suspend () -> String?) -> Unit = { block ->
        scope.launch {
            try {
                notice = block()
                error = null
                state.reloadKey++
            } catch (e: Exception) {
                // PERFECTION MIRROR R-1: export / record calls
                // that answer with the gone body leave the case
                // view for the deck's refreshed list + notice —
                // the deleted-case end state, not an error dump.
                if (CtosLogic.isCaseGone(e)) {
                    onGone()
                } else {
                    error = plainError(e, "That could not be done. Try again.")
                }
            }
        }
    }

    // PERFECTION MIRROR R-1: any LOAD of this case (the open
    // itself, a filter change) that answers with the gone
    // refusal leaves the same way — the server deliberately
    // does not distinguish deleted from another operator's,
    // and neither does this screen.
    LaunchedEffect(state.errorCause) {
        if (CtosLogic.isCaseGone(state.errorCause)) onGone()
    }

    if (showReport) {
        CtosReportScreen(
            viewModel = viewModel,
            caseId = caseId,
            onBack = { showReport = false },
            onGone = onGone,
        )
        return
    }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 16.dp),
        ) {
            TextButton(onClick = onBack) { Text("‹ Tools") }
            Column(Modifier.weight(1f)) {
                Text("Case file", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Findings and artifacts collected against this target.",
                    color = SentinelMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        when {
            state.error != null && state.data == null ->
                ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null -> SkeletonList()
            else -> {
                val data = state.data!!
                val case = CtosLogic.parseCases(data).firstOrNull { it.id == caseId }
                val findings = CtosLogic.parseFindings(data)
                val artifacts = CtosLogic.parseArtifacts(data)
                LazyColumn {
                    item {
                        MirrorCard(case?.title ?: "Case", topTier = true) {
                            if (case != null) {
                                DetailLine("Target", case.target)
                                if (case.targetType.isNotBlank()) {
                                    DetailLine("Type", case.targetType)
                                }
                                DetailLine("Status", case.status)
                                DetailLine(
                                    "Findings / artifacts",
                                    "${case.findings} / ${case.artifacts}",
                                )
                                if (case.note.isNotBlank()) {
                                    Text(
                                        case.note,
                                        color = SentinelMuted,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                            ActionErrorText(error)
                            if (notice != null) Text(notice!!)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GradientPrimaryButton(
                                    text = "Add finding",
                                    onClick = { showFinding = true },
                                )
                                OutlinedButton(onClick = { showNote = true }) {
                                    Text("Add note")
                                }
                                // AUDIT-171 B-18: the printable
                                // case report (server twin) as a
                                // readable screen, not raw JSON.
                                OutlinedButton(onClick = { showReport = true }) {
                                    Text("Case report")
                                }
                                TextButton(onClick = {
                                    run {
                                        val out = viewModel.api.ctosExportCase(caseId)
                                        val exp = out["export"] ?: out
                                        shareText(
                                            context,
                                            "CToS case — ${case?.title ?: caseId}",
                                            prettyJson.encodeToString(
                                                JsonElement.serializer(), exp,
                                            ),
                                        )
                                        "Export shared."
                                    }
                                }) { Text("Share export") }
                            }
                        }
                    }
                    // PERFECTION MIRROR R-5: section counts are
                    // the server's totals (findingsTotal follows
                    // the active type filter, like the list it
                    // heads; artifactsTotal is the case's whole
                    // artifact count) — never the capped list's
                    // own size dressed as the total. Payloads
                    // predating the fields fall back to the list
                    // sizes, which the defaults keep complete.
                    item {
                        SectionHeader(
                            "Findings",
                            CtosLogic.findingsTotal(data) ?: findings.size,
                        )
                    }
                    item {
                        ChipRow(
                            options = findingTypes,
                            selected = ftype,
                            onPick = {
                                ftypeState.value = it
                                state.reloadKey++
                            },
                            allLabel = "All types",
                        )
                    }
                    if (findings.isEmpty()) {
                        item {
                            EmptyBox(
                                "No findings here",
                                if (ftype == null) {
                                    "Run a tool against this case, or add a finding by hand."
                                } else {
                                    "No findings of this type yet — the filter is honest, not a negative."
                                },
                            )
                        }
                    }
                    items(findings) { f ->
                        MirrorCard(f.value) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MirrorChip(f.type)
                                if (f.tool.isNotBlank()) MirrorChip(f.tool)
                            }
                            if (f.detail.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(f.detail, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (f.url.isNotBlank()) {
                                TextButton(onClick = { openUrl(context, f.url) }) {
                                    Text("Open source")
                                }
                            }
                            if (f.createdAt > 0) {
                                Text(
                                    formatEpochMs(f.createdAt),
                                    color = SentinelMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    item {
                        SectionHeader(
                            "Artifacts",
                            CtosLogic.artifactsTotal(data) ?: artifacts.size,
                        )
                    }
                    if (artifacts.isEmpty()) {
                        item {
                            EmptyBox(
                                "No artifacts yet",
                                "Tool runs against this case file their full results here.",
                            )
                        }
                    }
                    items(artifacts) { a ->
                        ItemRow(
                            title = a.label,
                            subtitle = listOfNotNull(
                                a.kind.takeIf { it.isNotBlank() },
                                a.tool.takeIf { it.isNotBlank() },
                                if (a.createdAt > 0) formatEpochMs(a.createdAt) else null,
                            ).joinToString(" · "),
                            // AUDIT-171 B-20: artifacts were
                            // label-only rows. A tap now loads the
                            // case export and shows this artifact's
                            // content (the case payload omits it
                            // by design).
                            onClick = {
                                openArtifact = a
                                artifactContent = null
                                run {
                                    val out = viewModel.api.ctosExportCase(caseId)
                                    val content = CtosLogic.artifactContent(
                                        out, a.label, a.tool, a.createdAt,
                                    )
                                    artifactContent = content?.let {
                                        prettyJson.encodeToString(
                                            JsonElement.serializer(), it,
                                        )
                                    } ?: "This artifact's content was not in the export."
                                    null
                                }
                            },
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    if (showFinding) {
        RecordFindingDialog(
            findingTypes = findingTypes,
            onDismiss = { showFinding = false },
            onSave = { body ->
                showFinding = false
                run {
                    val out = viewModel.api.ctosRecord(caseId, body)
                    val added = Phase3Logic.int(out, "added") ?: 0
                    if (added > 0) "Finding recorded." else "Already in the case — not duplicated."
                }
            },
        )
    }
    if (showNote) {
        RecordNoteDialog(
            onDismiss = { showNote = false },
            onSave = { body ->
                showNote = false
                run { viewModel.api.ctosRecord(caseId, body); "Note recorded." }
            },
        )
    }
    openArtifact?.let { artifact ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { openArtifact = null },
            title = { Text(artifact.label) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        listOfNotNull(
                            artifact.kind.takeIf { it.isNotBlank() },
                            artifact.tool.takeIf { it.isNotBlank() },
                        ).joinToString(" · "),
                        color = SentinelMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        artifactContent ?: "Loading the artifact's content…",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        ),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { openArtifact = null }) { Text("Close") }
            },
        )
    }
}

/**
 * AUDIT-171 B-18 [OSINT]: the printable CToS case report as a
 * readable screen — the server builds it (a_ctosreport) with its
 * honesty footer; the app renders the structure and can share it
 * as text. Before this, the twin had no app method at all.
 */
@Composable
private fun CtosReportScreen(
    viewModel: AppViewModel,
    caseId: Int,
    onBack: () -> Unit,
    onGone: () -> Unit,
) {
    val context = LocalContext.current
    val state = rememberJsonScreen(key = caseId) { viewModel.api.ctosCaseReport(caseId) }
    // PERFECTION MIRROR R-1: a report load that answers with
    // the gone refusal leaves for the deck's refreshed list +
    // notice, like every other case surface — going "back" to
    // the case view would only meet the same refusal there.
    LaunchedEffect(state.errorCause) {
        if (CtosLogic.isCaseGone(state.errorCause)) onGone()
    }
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 16.dp),
        ) {
            TextButton(onClick = onBack) { Text("‹ Case") }
            Column(Modifier.weight(1f)) {
                Text("Case report", style = MaterialTheme.typography.titleLarge)
                Text(
                    "The printable report Command builds for this case.",
                    color = SentinelMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        when {
            state.error != null && state.data == null ->
                ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null -> SkeletonList()
            else -> {
                val report = CtosLogic.parseReport(state.data)
                if (report == null) {
                    ErrorBox("The report payload could not be read.") { state.reloadKey++ }
                } else {
                    LazyColumn {
                        item {
                            MirrorCard(report.case?.title ?: "Case", topTier = true) {
                                report.case?.let { c ->
                                    DetailLine("Target", c.target)
                                    if (c.targetType.isNotBlank()) {
                                        DetailLine("Type", c.targetType)
                                    }
                                    DetailLine("Status", c.status)
                                }
                                if (report.generatedAt > 0) {
                                    DetailLine("Generated", formatEpochMs(report.generatedAt))
                                }
                                Spacer(Modifier.height(8.dp))
                                GradientPrimaryButton(
                                    text = "Share report",
                                    onClick = {
                                        shareText(
                                            context,
                                            "CToS case report — ${report.case?.title ?: caseId}",
                                            CtosLogic.renderReportText(report),
                                        )
                                    },
                                )
                            }
                        }
                        item { SectionHeader("Findings", report.findings.size) }
                        if (report.findingsByType.isNotEmpty()) {
                            item {
                                MirrorCard("Findings by type") {
                                    report.findingsByType.forEach { (type, count) ->
                                        DetailLine(type, count.toString())
                                    }
                                }
                            }
                        }
                        items(report.findings) { f ->
                            MirrorCard(f.value.ifBlank { f.type }) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (f.type.isNotBlank()) MirrorChip(f.type)
                                    if (f.tool.isNotBlank()) MirrorChip(f.tool)
                                }
                                if (f.detail.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(f.detail, style = MaterialTheme.typography.bodyMedium)
                                }
                                if (f.url.isNotBlank()) {
                                    TextButton(onClick = { openUrl(context, f.url) }) {
                                        Text("Open source")
                                    }
                                }
                            }
                        }
                        if (report.artifacts.isNotEmpty()) {
                            item { SectionHeader("Artifacts", report.artifacts.size) }
                            items(report.artifacts) { a ->
                                ItemRow(
                                    title = a.label,
                                    subtitle = listOfNotNull(
                                        a.kind.takeIf { it.isNotBlank() },
                                        a.tool.takeIf { it.isNotBlank() },
                                    ).joinToString(" · "),
                                )
                            }
                        }
                        if (report.footer.isNotBlank()) {
                            item {
                                Text(
                                    report.footer,
                                    color = SentinelMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordFindingDialog(
    findingTypes: List<String>,
    onDismiss: () -> Unit,
    onSave: (JsonObject) -> Unit,
) {
    var type by remember { mutableStateOf(findingTypes.firstOrNull() ?: "other") }
    var value by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add finding") },
        text = {
            Column {
                ChipRow(
                    options = findingTypes.ifEmpty { listOf("other") },
                    selected = type,
                    onPick = { it?.let { v -> type = v } },
                )
                TextFieldRow("Value", value, onChange = { value = it })
                TextFieldRow("Detail (optional)", detail,
                    onChange = { detail = it }, singleLine = false, minLines = 2)
                TextFieldRow("Source URL (optional)", url, onChange = { url = it })
            }
        },
        confirmButton = {
            Button(
                enabled = value.isNotBlank(),
                onClick = {
                    onSave(buildJsonObject {
                        put("kind", "finding")
                        put("type", type)
                        put("value", value.trim())
                        if (detail.isNotBlank()) put("detail", detail.trim())
                        if (url.isNotBlank()) put("url", url.trim())
                    })
                },
            ) { Text("Record finding") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun RecordNoteDialog(
    onDismiss: () -> Unit,
    onSave: (JsonObject) -> Unit,
) {
    var label by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add note") },
        text = {
            Column {
                TextFieldRow("Label", label, onChange = { label = it })
                TextFieldRow("Note", text,
                    onChange = { text = it }, singleLine = false, minLines = 3)
            }
        },
        confirmButton = {
            Button(
                enabled = text.isNotBlank(),
                onClick = {
                    onSave(buildJsonObject {
                        put("kind", "artifact")
                        put("artifactKind", "note")
                        put("label", label.ifBlank { "Manual note" })
                        put("text", text)
                    })
                },
            ) { Text("Record note") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

// ---------------------------------------------------------------------
// Tool screen: native form + structured results
// ---------------------------------------------------------------------

@Composable
private fun CtosToolScreen(
    viewModel: AppViewModel,
    tool: CtosLogic.ToolMeta,
    cases: List<CtosLogic.CaseRow>,
    initialCaseId: Int?,
    workspace: JsonObject?,
    onBack: () -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 16.dp),
        ) {
            TextButton(onClick = onBack) { Text("‹ Tools") }
            Column(Modifier.weight(1f)) {
                Text(tool.label, style = MaterialTheme.typography.titleLarge)
                Text(
                    tool.desc,
                    color = SentinelMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        // AUDIT-171 B-16 [OSINT]: builders and the settings tile
        // used to fall through to the server-run panel, whose Run
        // the server refuses ("Unknown CToS tool") — every Run
        // failed. Builders compose a command ON THIS PHONE (the
        // web's own design — Command never executes them), and
        // Encryption Settings opens the OPSEC profile panel.
        when {
            tool.kind == "browser" -> CtosFileHashPanel()
            CtosBuilders.isBuilder(tool.name) -> CtosBuilderPanel(tool)
            tool.kind == "settings" || tool.name == "encsettings" ->
                CtosOpsecPanel(viewModel, workspace)
            else -> CtosServerToolPanel(viewModel, tool, cases, initialCaseId)
        }
    }
}

/**
 * AUDIT-171 B-16 [OSINT]: the command-builder panel. Composes
 * the command on this phone (CtosBuilders — the web's templates)
 * and offers it for copy; Command never executes it. The fields
 * are the web form's, with the web's defaults.
 */
@Composable
private fun CtosBuilderPanel(tool: CtosLogic.ToolMeta) {
    val clipboard = LocalClipboardManager.current
    var target by remember { mutableStateOf("") }
    var values by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var checked by remember {
        mutableStateOf(
            when (tool.name) {
                "buildernmap" -> setOf("t4")
                "buildersqlmap" -> setOf("batch")
                else -> emptySet()
            },
        )
    }
    var copied by remember { mutableStateOf(false) }
    val commands = if (target.isBlank()) {
        emptyList()
    } else {
        CtosBuilders.commands(tool.name, target, values, checked)
    }

    fun setValue(key: String, value: String) {
        values = values + (key to value)
        copied = false
    }

    fun toggle(key: String, on: Boolean) {
        checked = if (on) checked + key else checked - key
        copied = false
    }

    LazyColumn {
        item {
            MirrorCard("Build — ${tool.label}", topTier = true) {
                Text(
                    "Composes a command on this phone for your own terminal. " +
                        "Command never runs it.",
                    color = SentinelMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                TextFieldRow(tool.inputHint.ifBlank { "Target" }, target, onChange = {
                    target = it
                    copied = false
                })
                when (tool.name) {
                    "buildernmap" -> {
                        Text("Scan", color = SentinelMuted, style = MaterialTheme.typography.labelLarge)
                        ChipRow(
                            options = listOf("-sV", "-sS", "-A", "-sU", "-sn"),
                            selected = values["scan"] ?: "-sV",
                            onPick = { it?.let { v -> setValue("scan", v) } },
                        )
                        TextFieldRow("Ports (e.g. 22,80,443 or 1-1000)", values["ports"] ?: "",
                            onChange = { setValue("ports", it) })
                        BuilderCheck("Timing -T4", "t4", checked, ::toggle)
                        BuilderCheck("Default scripts (-sC)", "scripts", checked, ::toggle)
                        BuilderCheck("OS detect (-O)", "osdetect", checked, ::toggle)
                    }
                    "buildergobuster" -> {
                        Text("Mode", color = SentinelMuted, style = MaterialTheme.typography.labelLarge)
                        ChipRow(
                            options = listOf("dir", "dns", "vhost", "fuzz"),
                            selected = values["mode"] ?: "dir",
                            onPick = { it?.let { v -> setValue("mode", v) } },
                        )
                        TextFieldRow("Wordlist path", values["wordlist"] ?: CtosBuilders.DEFAULT_WORDLIST,
                            onChange = { setValue("wordlist", it) })
                        TextFieldRow("Extensions (php,html,…) — dir mode", values["extensions"] ?: "",
                            onChange = { setValue("extensions", it) })
                    }
                    "buildersqlmap" -> {
                        Text("Level", color = SentinelMuted, style = MaterialTheme.typography.labelLarge)
                        ChipRow(
                            options = listOf("1", "2", "3", "4", "5"),
                            selected = values["level"] ?: "1",
                            onPick = { it?.let { v -> setValue("level", v) } },
                        )
                        Text("Risk", color = SentinelMuted, style = MaterialTheme.typography.labelLarge)
                        ChipRow(
                            options = listOf("1", "2", "3"),
                            selected = values["risk"] ?: "1",
                            onPick = { it?.let { v -> setValue("risk", v) } },
                        )
                        BuilderCheck("--forms", "forms", checked, ::toggle)
                        BuilderCheck("--dbs", "dbs", checked, ::toggle)
                        BuilderCheck("--batch", "batch", checked, ::toggle)
                    }
                    "buildersublist3r" -> {
                        TextFieldRow("Output file (-o), optional", values["output"] ?: "",
                            onChange = { setValue("output", it) })
                        BuilderCheck("Brute force (-b)", "brute", checked, ::toggle)
                        BuilderCheck("Verbose (-v)", "verbose", checked, ::toggle)
                    }
                    "builderdorks" -> {
                        Text("Category", color = SentinelMuted, style = MaterialTheme.typography.labelLarge)
                        ChipRow(
                            options = listOf("ALL") + CtosBuilders.DORKS.keys.toList(),
                            selected = values["category"] ?: "ALL",
                            onPick = { it?.let { v -> setValue("category", v) } },
                        )
                    }
                }
            }
        }
        if (commands.isNotEmpty()) {
            item {
                MirrorCard(if (commands.size == 1) "Command" else "Queries — ${commands.size}") {
                    commands.forEach { cmd ->
                        Text(
                            cmd,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            ),
                            modifier = Modifier.padding(vertical = 3.dp),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GradientPrimaryButton(
                            text = if (copied) "Copied" else "Copy",
                            onClick = {
                                clipboard.setText(AnnotatedString(commands.joinToString("\n")))
                                copied = true
                            },
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun BuilderCheck(
    label: String,
    key: String,
    checked: Set<String>,
    onToggle: (String, Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Checkbox(
            checked = key in checked,
            onCheckedChange = { onToggle(key, it) },
        )
        Text(label)
    }
}

/**
 * AUDIT-171 B-17 [OSINT]: the OPSEC profile panel (the
 * encsettings tile's real home). Shows the operator's current
 * profile values and the server's own wired/unavailable rows
 * with their reasons. Since TIER Phase B (S-2) the profile is
 * also EDITABLE here: the save rides POST
 * /api/app/osint/ctos/profile (the web ctossetprofile handler),
 * sending only the changed fields — the handler preserves the
 * rest — and its refusals show verbatim. The panel re-reads its
 * state from the save's own answer ({profile, profileOptions}),
 * the same shapes the workspace payload carries.
 */
@Composable
private fun CtosOpsecPanel(viewModel: AppViewModel, workspace: JsonObject?) {
    val scope = rememberCoroutineScope()
    var ws by remember { mutableStateOf(workspace) }
    val profile = CtosLogic.parseProfile(ws)
    val options = CtosLogic.parseProfileOptions(ws)
    // Edit state seeded from the server-confirmed profile; the
    // remember key reseeds it whenever a save confirms new values.
    var doh by remember(profile) { mutableStateOf(profile?.doh ?: "google") }
    var dohCustom by remember(profile) { mutableStateOf(profile?.dohCustom ?: "") }
    var uaRotate by remember(profile) { mutableStateOf(profile?.uaRotate ?: false) }
    var jitterText by remember(profile) {
        mutableStateOf((profile?.jitterMs ?: 0).toString())
    }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val payload = profile?.let {
        CtosLogic.profileSavePayload(
            it, doh, dohCustom, uaRotate, jitterText.toIntOrNull() ?: it.jitterMs,
        )
    }
    LazyColumn {
        item {
            MirrorCard("Encryption Settings — OPSEC profile", topTier = true) {
                Text(
                    "How CToS network runs present themselves. These are your " +
                        "current settings, as the server holds them.",
                    color = SentinelMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                if (profile == null) {
                    Text("The profile could not be read from the workspace payload.")
                } else if (options != null && options.wired.isNotEmpty()) {
                    options.wired.forEach { row ->
                        DetailLine(row.label, CtosLogic.profileValueLine(profile, row.key))
                        if (row.detail.isNotBlank()) {
                            Text(
                                row.detail,
                                color = SentinelMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                } else if (profile != null) {
                    DetailLine("DNS-over-HTTPS provider", profile.doh)
                    DetailLine("User-Agent rotation", if (profile.uaRotate) "On" else "Off")
                    DetailLine("Request jitter", if (profile.jitterMs > 0) "Up to ${profile.jitterMs} ms" else "Off")
                }
                if (profile != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Change the profile",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    ChoiceMenu(
                        "DNS-over-HTTPS provider",
                        doh,
                        options?.providers?.takeIf { it.isNotEmpty() }
                            ?: listOf("google", "cloudflare", "quad9", "custom"),
                    ) { doh = it }
                    if (doh == "custom") {
                        TextFieldRow(
                            "Custom DoH endpoint (https://…)",
                            dohCustom,
                            onChange = { dohCustom = it },
                        )
                    }
                    SwitchRow("User-Agent rotation", uaRotate) { uaRotate = it }
                    TextFieldRow(
                        "Request jitter (ms, 0–2000)",
                        jitterText,
                        onChange = { jitterText = it.filter { ch -> ch.isDigit() }.take(4) },
                        kind = InteractionLogic.FieldKind.NUMBER,
                    )
                    Button(
                        enabled = !busy && payload != null && payload.isNotEmpty(),
                        onClick = {
                            val body = payload ?: return@Button
                            busy = true
                            notice = null
                            saveError = null
                            scope.launch {
                                try {
                                    ws = viewModel.api.ctosSaveProfile(body)
                                    notice = "Profile saved."
                                } catch (e: Exception) {
                                    saveError = plainError(
                                        e,
                                        "The profile could not be saved. Try again.",
                                    )
                                } finally {
                                    busy = false
                                }
                            }
                        },
                    ) { Text(if (busy) "Saving…" else "Save profile") }
                    notice?.let {
                        Text(
                            it,
                            color = SentinelMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    saveError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Changes save to your server profile. Only the fields " +
                            "you change are sent — the rest stay as they are.",
                        color = SentinelMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        if (options != null && options.unavailable.isNotEmpty()) {
            item { SectionHeader("Not available", options.unavailable.size) }
            item {
                MirrorCard("Not available — and why") {
                    options.unavailable.forEach { row ->
                        Text(row.label, fontWeight = FontWeight.Bold)
                        Text(
                            row.reason,
                            color = SentinelMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun CtosServerToolPanel(
    viewModel: AppViewModel,
    tool: CtosLogic.ToolMeta,
    cases: List<CtosLogic.CaseRow>,
    initialCaseId: Int?,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var caseId by remember { mutableStateOf(initialCaseId) }
    var depth by remember { mutableStateOf("quick") }
    var mode by remember { mutableStateOf(CtosLogic.MODE_TOOLS[tool.name]?.second?.first()?.first ?: "") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    // PERFECTION MIRROR R-1: a case this panel aimed a run at
    // that answers with the gone refusal leaves the picker for
    // the rest of the panel's life (the deck's list refreshes
    // from the server when the operator goes back).
    var goneCaseIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val panelCases = cases.filter { it.id !in goneCaseIds }
    var result by remember { mutableStateOf<JsonObject?>(null) }
    // PERFECTION R-6 (W-4): network tools run ASYNC — the
    // start answers immediately with a run id and the panel
    // polls the run's own state (CtosLogic's cadence). A watch
    // that hits its cap keeps the run id here: the run itself
    // continues server-side and "Check again" resumes the
    // watch on the SAME run, never a second run.
    var runPhase by remember { mutableStateOf<CtosLogic.RunPhase?>(null) }
    var stalledRunId by remember { mutableStateOf<String?>(null) }
    // Metadata extractor: the picked file's bytes ride as base64
    // in options (the server's own contract); parsed in memory
    // server-side, never stored.
    var pickedName by remember { mutableStateOf<String?>(null) }
    var pickedB64 by remember { mutableStateOf<String?>(null) }
    var pickError by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val (name, b64) = withContext(Dispatchers.IO) {
                    val resolver = context.contentResolver
                    val display = resolver.query(uri, null, null, null, null)?.use { cur ->
                        val idx = cur.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0 && cur.moveToFirst()) cur.getString(idx) else null
                    } ?: "image"
                    val bytes = resolver.openInputStream(uri)?.use { stream ->
                        val out = java.io.ByteArrayOutputStream()
                        val buf = ByteArray(64 * 1024)
                        var total = 0
                        while (true) {
                            val n = stream.read(buf)
                            if (n < 0) break
                            total += n
                            if (total > 8 * 1024 * 1024) {
                                throw IllegalStateException("over-limit")
                            }
                            out.write(buf, 0, n)
                        }
                        out.toByteArray()
                    } ?: throw IllegalStateException("unreadable")
                    display to Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
                pickedName = name
                pickedB64 = b64
                pickError = null
            } catch (e: IllegalStateException) {
                pickError = if (e.message == "over-limit") {
                    "That file is over the 8 MB metadata limit."
                } else {
                    "That file could not be read."
                }
            } catch (e: Exception) {
                pickError = "That file could not be read."
            }
        }
    }

    // The one run-body construction (CtosLogic.toolRunBody,
    // suite-pinned): the case binding rides the body's top
    // level, where both dispatch paths resolve it through the
    // identical F-C1 mutate scoping check.
    fun buildRunBody(): JsonObject = CtosLogic.toolRunBody(
        toolName = tool.name,
        input = input,
        caseId = caseId,
        depth = depth,
        mode = mode,
        fileB64 = pickedB64,
        fileName = pickedName,
    )

    /** The shared failure mapping for a run (sync, async start,
     *  or a resumed watch). */
    fun noteRunError(e: Exception) {
        // PERFECTION MIRROR R-1: a run aimed at a case
        // that answers with the gone body un-aims the
        // panel (the case leaves the picker) and the
        // server's sentence is the notice — not a
        // generic run failure, and never a sign-in
        // signal (a dead token is a different exception
        // and never reaches this catch).
        val aimed = caseId
        if (aimed != null && CtosLogic.isCaseGone(e)) {
            goneCaseIds = goneCaseIds + aimed
            caseId = null
            notice = CtosLogic.CASE_GONE_MESSAGE
            error = null
        } else {
            error = plainError(e, "The tool could not run. Try again.")
        }
    }

    /**
     * Watch an async run to its outcome. DONE lands the poll
     * payload in [result] (its shape IS the sync result shape,
     * so the result view below renders it unchanged); FAILED
     * surfaces the server's own error string; a run that
     * outlives the watch cap parks its id in [stalledRunId]
     * with an honest notice. A poll that answers with the
     * run-gone refusal (server restarted, or the run is not
     * this operator's) throws into the caller's error mapping
     * — plainError renders the server's sentence verbatim.
     */
    suspend fun watchRun(runId: String) {
        runPhase = CtosLogic.RunPhase.RUNNING
        var polls = 0
        var misses = 0
        while (true) {
            val payload = try {
                viewModel.api.ctosToolRun(runId)
            } catch (e: Exception) {
                if (CtosLogic.isRunGone(e)) throw e
                misses++
                if (misses >= CtosLogic.RUN_POLL_MAX_MISSES) throw e
                delay(CtosLogic.RUN_POLL_INTERVAL_MS)
                continue
            }
            misses = 0
            val poll = CtosLogic.parseRunPoll(payload)
            when (poll.phase) {
                CtosLogic.RunPhase.DONE -> {
                    result = payload
                    return
                }
                CtosLogic.RunPhase.FAILED -> {
                    error = poll.error.ifBlank {
                        "The run failed on Command."
                    }
                    return
                }
                else -> {
                    polls++
                    if (polls >= CtosLogic.RUN_POLL_MAX) {
                        stalledRunId = runId
                        notice = "Still running on Command — the run " +
                            "continues on the server. Check again to " +
                            "keep watching it here."
                        return
                    }
                    delay(CtosLogic.RUN_POLL_INTERVAL_MS)
                }
            }
        }
    }

    fun launchWatch(runId: String) {
        scope.launch {
            busy = true
            error = null
            notice = null
            try {
                watchRun(runId)
            } catch (e: Exception) {
                noteRunError(e)
            } finally {
                busy = false
                if (stalledRunId == null) runPhase = null
            }
        }
    }

    fun run() {
        if (busy) return
        scope.launch {
            busy = true
            error = null
            notice = null
            stalledRunId = null
            try {
                val body = buildRunBody()
                if (!tool.network) {
                    // Local-compute tools answer in the request,
                    // as they always have — the sync twin.
                    result = viewModel.api.ctosRunTool(tool.name, body)
                } else {
                    // PERFECTION R-6: network tools run async.
                    // The start performs every pre-run gate
                    // server-side and answers with a run id;
                    // the panel then watches the run itself.
                    runPhase = CtosLogic.RunPhase.STARTING
                    val start = CtosLogic.parseRunStart(
                        viewModel.api.ctosRunToolAsync(tool.name, body),
                    ) ?: throw IllegalStateException(
                        "Command started the run without a run id.",
                    )
                    watchRun(start.runId)
                }
            } catch (e: Exception) {
                noteRunError(e)
            } finally {
                busy = false
                if (stalledRunId == null) runPhase = null
            }
        }
    }

    LazyColumn {
        item {
            MirrorCard("Run ${tool.label}", topTier = true) {
                if (tool.gated) {
                    // AUDIT-171 B-17: a gated active tool states its
                    // terms BEFORE the run, not as a refusal after.
                    Text(
                        "Authorization-gated: this active tool runs only " +
                            "against targets registered in the authorization " +
                            "gate (see the deck's Authorization gate card). " +
                            "The server checks the gate and audit-logs the " +
                            "decision either way.",
                        color = SentinelAmber,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(6.dp))
                }
                if (tool.name == "analyst") {
                    // CTOS TRIO (1.7.6): the Analyst's terms, stated
                    // before the run like the gate's — it is
                    // case-bound by design (its context is ONE
                    // case's findings and the analysis is recorded
                    // back into that case), and its no-key state is
                    // a designed report, not a failure: with no
                    // Anthropic or OpenAI key configured it answers
                    // not configured and generates nothing. Keys are
                    // set in web Command settings only.
                    Text(
                        "Case-bound: the AI Analyst answers your question " +
                            "from ONE case's recorded findings and writes " +
                            "the analysis back into that case — pick the " +
                            "case below before running. With no Anthropic " +
                            "or OpenAI key configured, it reports not " +
                            "configured and generates nothing; keys are " +
                            "set in web Command settings.",
                        color = SentinelMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (panelCases.isEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "There are no cases to bind to yet — create " +
                                "one from the deck first. Without a case, " +
                                "Command refuses the run in its own words.",
                            color = SentinelAmber,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
                if (tool.name == "metadata") {
                    Text(
                        "Pick an image — it is sent to Command, parsed in memory, " +
                            "and never stored. Only the extracted fields can be " +
                            "recorded into a case.",
                        color = SentinelMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { picker.launch("image/*") }) {
                            Text("Choose image")
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            pickedName ?: "No file chosen",
                            color = SentinelMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    ActionErrorText(pickError)
                } else {
                    TextFieldRow(
                        label = tool.inputHint.ifBlank { "Input" },
                        value = input,
                        onChange = { input = it },
                        singleLine = tool.name !in CtosLogic.MULTILINE_TOOLS,
                        minLines = if (tool.name in CtosLogic.MULTILINE_TOOLS) 3 else 1,
                    )
                }
                if (tool.name in CtosLogic.DEPTH_TOOLS) {
                    Text("Depth", style = MaterialTheme.typography.labelLarge)
                    ChipRow(
                        options = listOf("quick", "full"),
                        selected = depth,
                        onPick = { it?.let { v -> depth = v } },
                    )
                    Text(
                        if (depth == "full") {
                            "Full depth probes the entire platform catalog."
                        } else {
                            "Quick depth probes the curated high-signal set."
                        },
                        color = SentinelMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                CtosLogic.MODE_TOOLS[tool.name]?.let { (_, choices) ->
                    Text("Mode", style = MaterialTheme.typography.labelLarge)
                    ChipRow(
                        options = choices.map { it.second },
                        selected = choices.firstOrNull { it.first == mode }?.second,
                        onPick = { label ->
                            mode = choices.firstOrNull { it.second == label }?.first ?: mode
                        },
                    )
                }
                if (panelCases.isNotEmpty()) {
                    Text("Record results into", style = MaterialTheme.typography.labelLarge)
                    ChipRow(
                        // AUDIT-171 B-07: id-carrying chips, as on
                        // the deck — titles collide by construction.
                        options = panelCases.map { CtosLogic.caseChipLabel(it) },
                        selected = panelCases.firstOrNull { it.id == caseId }
                            ?.let { CtosLogic.caseChipLabel(it) },
                        onPick = { chip ->
                            caseId = chip?.let { CtosLogic.caseFromChip(panelCases, it)?.id }
                        },
                        allLabel = "No case",
                    )
                }
                if (busy) {
                    Row(
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
                            if (runPhase == CtosLogic.RunPhase.STARTING) {
                                "Starting on Command…"
                            } else {
                                "Running on Command…"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = SentinelMuted,
                        )
                    }
                }
                ActionErrorText(error)
                if (notice != null) {
                    Text(
                        notice!!,
                        color = SentinelMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                val stalled = stalledRunId
                if (stalled != null && !busy) {
                    // PERFECTION R-6: resume the watch on the
                    // SAME run — it never stopped server-side.
                    TextButton(onClick = {
                        stalledRunId = null
                        launchWatch(stalled)
                    }) { Text("Check again") }
                }
                Spacer(Modifier.height(8.dp))
                GradientPrimaryButton(
                    text = "Run ${tool.label}",
                    enabled = !busy && (tool.name == "metadata" ||
                        input.isNotBlank() || tool.name == "ports"),
                    onClick = { run() },
                )
            }
        }
        val payload = result
        if (payload != null) {
            item { SectionHeader("Result") }
            item {
                CtosResultView(
                    toolName = tool.name,
                    parsed = CtosLogic.parseToolResult(payload),
                    rawResult = Phase3Logic.obj(payload, "result"),
                )
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

/** One tool result: recorded line, source cards, structured data. */
@Composable
private fun CtosResultView(
    toolName: String,
    parsed: CtosLogic.ToolResult,
    rawResult: JsonObject?,
) {
    val context = LocalContext.current
    Column {
        if (parsed.recorded != null) {
            Text(
                if (parsed.recorded > 0) {
                    "Recorded ${parsed.recorded} new finding(s) into the case."
                } else {
                    "Run filed on the case (no new findings — duplicates are not re-recorded)."
                },
                color = SentinelMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        parsed.sources.forEach { src ->
            MirrorCard(src.source) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(CtosLogic.statusFor(src.status))
                    MirrorChip(OsintLogic.confidenceLabel(src.confidence))
                }
                if (!src.summary.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(src.summary, style = MaterialTheme.typography.bodyMedium)
                }
                if (!src.note.isNullOrBlank()) {
                    Text(
                        src.note,
                        color = SentinelMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                src.findings.forEach { f ->
                    Spacer(Modifier.height(6.dp))
                    if (f.title.isNotBlank()) {
                        Text(f.title, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    if (!f.detail.isNullOrBlank()) {
                        Text(f.detail!!, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (!f.url.isNullOrBlank()) {
                        TextButton(onClick = { openUrl(context, f.url!!) }) {
                            Text("Open source")
                        }
                    }
                }
                // AUDIT-171 B-19: the source's own URL, when the
                // payload carries one — a source whose only output
                // is its URL used to render as a bare summary.
                if (!src.url.isNullOrBlank() && src.findings.isEmpty()) {
                    TextButton(onClick = { openUrl(context, src.url!!) }) {
                        Text("Open source")
                    }
                }
            }
        }
        // AUDIT-171 B-19: the result's top-level findings — parsed
        // since this audit (CtosLogic.parseToolResult) and rendered
        // here; a tool that returns findings only here used to show
        // an empty result.
        if (parsed.findings.isNotEmpty()) {
            SectionHeader("Findings", parsed.findings.size)
            parsed.findings.forEach { f ->
                MirrorCard(f.value.ifBlank { f.type }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (f.type.isNotBlank()) MirrorChip(f.type)
                        if (f.tool.isNotBlank()) MirrorChip(f.tool)
                    }
                    if (f.detail.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(f.detail, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (f.url.isNotBlank()) {
                        TextButton(onClick = { openUrl(context, f.url) }) {
                            Text("Open source")
                        }
                    }
                }
            }
        }
        val data = parsed.data
        if (data != null) {
            val username = CtosLogic.parseUsernameData(data)
            val modules = Phase3Logic.arr(data, "modules")
            // CTOS TRIO (1.7.6): the analyst's configured state
            // and the composite's Identity Graph get their own
            // renderings ahead of the generic shaping (an
            // UNREPORTED analyst state falls through to it —
            // the surface never settles on an unreported state).
            val analyst = if (toolName == "analyst") {
                CtosLogic.parseAnalystData(data)
                    ?.takeIf { it.state != CtosLogic.AnalystState.UNREPORTED }
            } else {
                null
            }
            val graph = if (toolName == "composite") {
                CtosLogic.parseIdentityGraph(data)
            } else {
                null
            }
            when {
                analyst != null -> CtosAnalystView(analyst)
                username != null -> CtosUsernameView(username)
                graph != null -> {
                    CtosDataNodes(
                        title = "Composite profile",
                        nodes = CtosLogic.shapeData(
                            JsonObject(
                                data.filterKeys {
                                    it != "modules" && it != "identityGraph"
                                },
                            ),
                        ),
                    )
                    CtosIdentityGraphView(graph)
                    CtosModulesView(modules)
                }
                toolName == "dashboard" && modules.isNotEmpty() -> {
                    CtosDataNodes(
                        title = "Target",
                        nodes = CtosLogic.shapeData(
                            JsonObject(data.filterKeys { it != "modules" }),
                        ),
                    )
                    CtosModulesView(modules)
                }
                else -> CtosDataNodes(title = "Data", nodes = CtosLogic.shapeData(data))
            }
        }
    }
}

/** The per-module rendering the dashboard pioneered — each
 *  module's own result through the one result view (the
 *  composite's legs ride it unchanged). */
@Composable
private fun CtosModulesView(modules: List<JsonObject>) {
    modules.forEach { mod ->
        val ok = Phase3Logic.bool(mod, "ok") == true
        val mTool = Phase3Logic.str(mod, "tool")
        SectionHeader("Module — $mTool")
        if (!ok) {
            Text(
                Phase3Logic.str(mod, "error"),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        } else {
            val modResult = Phase3Logic.obj(mod, "result")
            CtosResultView(
                toolName = mTool,
                parsed = CtosLogic.ToolResult(
                    sources = (modResult?.get("sources")
                        as? kotlinx.serialization.json.JsonArray)
                        ?.mapNotNull { it as? JsonObject }
                        ?.map { OsintLogic.parseSourceEntry(it) }
                        ?: emptyList(),
                    data = modResult?.get("data") as? JsonObject,
                    recorded = null,
                ),
                rawResult = modResult,
            )
        }
    }
}

/**
 * CTOS TRIO (1.7.6): the AI Analyst's result. Three honest
 * shapes and no fourth: NOT_CONFIGURED renders as the DESIGNED
 * state it is (nothing was generated, nothing is wrong — the
 * next step is a key the Owner sets on the web); READY with
 * text renders the analysis under the server's own
 * AI-generated label; READY without text renders the
 * provider's failure plainly (the server's words ride the
 * source card above; the error line here is the server's own
 * string). An UNREPORTED state never reaches this view — the
 * caller falls back to the generic data rendering.
 */
@Composable
private fun CtosAnalystView(analyst: CtosLogic.AnalystResult) {
    when (analyst.state) {
        CtosLogic.AnalystState.NOT_CONFIGURED -> {
            MirrorCard("AI Analyst — not configured") {
                Text(
                    "No Anthropic or OpenAI key is configured, so the " +
                        "Analyst generated nothing — by design, it never " +
                        "invents an answer in place of one.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (analyst.question.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    DetailLine("Question", analyst.question)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Next step: the Owner adds an Anthropic or OpenAI " +
                        "key in web Command settings (CToS source keys), " +
                        "then run again. Keys are never entered in this " +
                        "app.",
                    color = SentinelMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        CtosLogic.AnalystState.READY -> {
            val text = analyst.analysis
            if (text != null) {
                MirrorCard("AI-generated analysis") {
                    val who = listOf(
                        CtosLogic.analystProviderLabel(analyst.provider),
                        analyst.model,
                    ).filter { it.isNotBlank() }.joinToString(" / ")
                    Text(
                        if (who.isNotBlank()) {
                            "AI-GENERATED ANALYSIS ($who) — analysis of " +
                                "this case's findings, not a verified fact."
                        } else {
                            "AI-GENERATED ANALYSIS — analysis of this " +
                                "case's findings, not a verified fact."
                        },
                        color = SentinelAmber,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                    if (analyst.findingsConsidered != null) {
                        Spacer(Modifier.height(6.dp))
                        DetailLine(
                            "Findings considered",
                            if (analyst.findingsTotal != null) {
                                "${analyst.findingsConsidered} of " +
                                    "${analyst.findingsTotal}"
                            } else {
                                "${analyst.findingsConsidered}"
                            },
                        )
                    }
                    if (analyst.contextTruncated) {
                        Text(
                            "The case context was truncated at the " +
                                "server's cap — the analysis covers the " +
                                "findings shown, not the whole case.",
                            color = SentinelMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            } else {
                MirrorCard("AI Analyst — no analysis") {
                    Text(
                        "The provider did not return an analysis. No " +
                            "analysis was generated, and nothing was " +
                            "recorded in place of one.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (analyst.error.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        DetailLine("Provider outcome", analyst.error)
                    }
                }
            }
        }
        CtosLogic.AnalystState.UNREPORTED -> Unit
    }
}

/**
 * CTOS TRIO (1.7.6): the Composite Profiler's Identity Graph,
 * rendered from the server's own structure — nodes with their
 * verbatim provenance, the per-leg statuses, and the counts.
 * Confidence is the server's corroboration bucket (how many
 * distinct tools carry a "found" source for the identifier);
 * it is shown as the server's word plus that count, never as
 * a score or a probability. A leg that answered
 * not_configured or unavailable shows as exactly that — its
 * silence is visible, never dressed as a negative.
 */
@Composable
private fun CtosIdentityGraphView(graph: CtosLogic.IdentityGraph) {
    SectionHeader("Identity Graph", graph.nodes.size)
    MirrorCard("Identity Graph") {
        Text(
            "Every identifier the passive legs actually produced, with " +
                "the provenance the server recorded for it. Confidence " +
                "is a corroboration count — the number of distinct " +
                "tools carrying a found source for the identifier — " +
                "never a probability.",
            color = SentinelMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        DetailLine(
            "Identifiers / links",
            "${graph.nodes.size} / ${graph.edges.size}",
        )
        if (graph.truncated) {
            Text(
                "The graph reached the server's node cap — some " +
                    "identifiers are not shown here.",
                color = SentinelAmber,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        CtosLogic.graphDisplayNodes(graph).forEach { node ->
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    node.label.ifBlank { node.id },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (node.seed) MirrorChip("TARGET")
                if (node.kind.isNotBlank()) MirrorChip(node.kind)
            }
            if (!node.seed && node.confidence.isNotBlank()) {
                Text(
                    "Confidence ${CtosLogic.confidenceLabel(node.confidence)}" +
                        " — ${node.corroborations} corroborating tool(s)",
                    color = SentinelMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            node.provenance.forEach { prov ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 3.dp),
                ) {
                    StatusChip(prov.status)
                    Text(
                        listOf(prov.tool, prov.source)
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        color = SentinelMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
    if (graph.legs.isNotEmpty()) {
        MirrorCard("Composite legs") {
            graph.legs.forEach { leg ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 3.dp),
                ) {
                    StatusChip(leg.status)
                    Text(
                        if (leg.input.isNotBlank()) {
                            "${leg.tool} on ${leg.input}"
                        } else {
                            leg.tool
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Text(
                "A leg marked not configured or unavailable said " +
                    "nothing either way — it is shown, not hidden.",
                color = SentinelMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CtosUsernameView(result: CtosLogic.UsernameResult) {
    val context = LocalContext.current
    MirrorCard("Profiles — ${result.username}") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MirrorChip("${result.found} found")
            MirrorChip("${result.notFound} no profile")
            MirrorChip("${result.unknown} unknown")
        }
        Text(
            "Unknown means the platform refused or could not answer — it is not a negative.",
            color = SentinelMuted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    val found = result.rows.filter { it.status == CtosLogic.Status.FOUND }
    val absent = result.rows.filter { it.status == CtosLogic.Status.NOT_FOUND }
    val unknown = result.rows.filter {
        it.status != CtosLogic.Status.FOUND && it.status != CtosLogic.Status.NOT_FOUND
    }
    if (found.isNotEmpty()) {
        SectionHeader("Found", found.size)
        found.forEach { row ->
            MirrorCard(row.site) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(row.status)
                    if (row.cat.isNotBlank()) MirrorChip(row.cat)
                }
                if (row.url.isNotBlank()) {
                    TextButton(onClick = { openUrl(context, row.url) }) {
                        Text("Open profile")
                    }
                }
            }
        }
    }
    if (absent.isNotEmpty()) {
        SectionHeader("No profile", absent.size)
        MirrorCard("No profile found") {
            Text(
                absent.joinToString(", ") { it.site },
                color = SentinelMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    if (unknown.isNotEmpty()) {
        SectionHeader("Unknown", unknown.size)
        MirrorCard("Could not be determined") {
            Text(
                unknown.joinToString(", ") { it.site },
                color = SentinelMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CtosDataNodes(title: String, nodes: List<CtosLogic.DataNode>) {
    if (nodes.isEmpty()) return
    SectionHeader(title)
    MirrorCard(title) {
        CtosNodes(nodes, depth = 0)
    }
}

@Composable
private fun CtosNodes(nodes: List<CtosLogic.DataNode>, depth: Int) {
    nodes.forEach { node ->
        when (node) {
            is CtosLogic.DataNode.Line -> DetailLine(node.label, node.value)
            is CtosLogic.DataNode.Section -> {
                Spacer(Modifier.height(8.dp))
                Text(
                    node.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Column(Modifier.padding(start = if (depth < 2) 10.dp else 0.dp)) {
                    CtosNodes(node.children, depth + 1)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// File Hash Generator — on-device, like the web's in-browser tool
// ---------------------------------------------------------------------

@Composable
private fun CtosFileHashPanel() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var fileName by remember { mutableStateOf<String?>(null) }
    var hashes by remember { mutableStateOf<CtosLogic.FileHashes?>(null) }
    var busy by remember { mutableStateOf(false) }
    var pickError by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            pickError = null
            try {
                val (name, result) = withContext(Dispatchers.IO) {
                    val resolver = context.contentResolver
                    val display = resolver.query(uri, null, null, null, null)?.use { cur ->
                        val idx = cur.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0 && cur.moveToFirst()) cur.getString(idx) else null
                    } ?: "file"
                    val hasher = CtosLogic.Hasher()
                    resolver.openInputStream(uri)?.use { stream ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = stream.read(buf)
                            if (n < 0) break
                            hasher.update(buf, n)
                        }
                    } ?: throw IllegalStateException("unreadable")
                    display to hasher.finish()
                }
                fileName = name
                hashes = result
            } catch (e: Exception) {
                pickError = "That file could not be read."
            } finally {
                busy = false
            }
        }
    }

    LazyColumn {
        item {
            MirrorCard("File Hash Generator", topTier = true) {
                Text(
                    "SHA-1 / SHA-256 / SHA-512 and MD5 of a file — computed on this " +
                        "phone. The file never leaves it, exactly as the web " +
                        "computes it in your browser.",
                    color = SentinelMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GradientPrimaryButton(
                        text = "Choose file",
                        enabled = !busy,
                        onClick = { picker.launch("*/*") },
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        fileName ?: "No file chosen",
                        color = SentinelMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (busy) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 6.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = SentinelGreen,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Hashing…", color = SentinelMuted)
                    }
                }
                ActionErrorText(pickError)
            }
        }
        hashes?.let { h ->
            item {
                MirrorCard("Hashes — ${fileName ?: "file"}") {
                    DetailLine("Size", CtosLogic.sizeText(h.sizeBytes))
                    listOf(
                        "SHA-256" to h.sha256,
                        "SHA-512" to h.sha512,
                        "SHA-1" to h.sha1,
                        "MD5" to h.md5,
                    ).forEach { (label, value) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(label, style = MaterialTheme.typography.labelMedium,
                                    color = SentinelMuted)
                                Text(value, fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = {
                                clipboard.setText(AnnotatedString(value))
                            }) { Text("Copy") }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

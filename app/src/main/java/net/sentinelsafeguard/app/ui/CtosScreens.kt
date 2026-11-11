package net.sentinelsafeguard.app.ui

import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.data.api.ctosCase
import net.sentinelsafeguard.app.data.api.ctosClearCase
import net.sentinelsafeguard.app.data.api.ctosCreateCase
import net.sentinelsafeguard.app.data.api.ctosDeleteCase
import net.sentinelsafeguard.app.data.api.ctosExportCase
import net.sentinelsafeguard.app.data.api.ctosRecord
import net.sentinelsafeguard.app.data.api.ctosRunTool
import net.sentinelsafeguard.app.data.api.ctosWorkspace
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
    if (url.isBlank()) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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
                error = plainError(e, "That could not be done. Try again.")
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
                    "The CToS working deck — cases, findings, and the Wave 1 tools.",
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
                                ChipRow(
                                    options = cases.map { it.title },
                                    selected = selected?.title,
                                    onPick = { title ->
                                        selectedCaseId =
                                            cases.firstOrNull { it.title == title }?.id
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
                                    }) { Text("Export") }
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
                                    tool.kind == "browser" -> "ON-DEVICE"
                                    tool.network -> null
                                    else -> "LOCAL"
                                },
                                onClick = { openTool = tool },
                            )
                        }
                    }
                    item { SectionHeader("Premium sources") }
                    item {
                        MirrorCard("Source API keys") {
                            Text(
                                "Keys deepen tools that already work on free sources. " +
                                    "They are entered in web Command settings by the " +
                                    "Owner — the app never asks for, accepts, or " +
                                    "stores a key.",
                                color = SentinelMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Spacer(Modifier.height(6.dp))
                            CtosLogic.parseKeySources(data).forEach { ks ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                ) {
                                    Text(ks.label, modifier = Modifier.weight(1f))
                                    StatusPill(
                                        if (ks.configured) "CONFIGURED" else "NOT CONFIGURED",
                                        if (ks.configured) "paid" else "muted",
                                    )
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
                        Spacer(Modifier.height(16.dp))
                    }
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
) {
    val scope = rememberCoroutineScope()
    val ftypeState = remember { mutableStateOf<String?>(null) }
    val ftype = ftypeState.value
    val state = rememberJsonScreen {
        viewModel.api.ctosCase(caseId, ftypeState.value)
    }
    var showFinding by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }
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
                error = plainError(e, "That could not be done. Try again.")
            }
        }
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
                                }) { Text("Export") }
                            }
                        }
                    }
                    item { SectionHeader("Findings", findings.size) }
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
                    item { SectionHeader("Artifacts", artifacts.size) }
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
        if (tool.kind == "browser") {
            CtosFileHashPanel()
        } else {
            CtosServerToolPanel(viewModel, tool, cases, initialCaseId)
        }
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
    var result by remember { mutableStateOf<JsonObject?>(null) }
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

    fun run() {
        if (busy) return
        scope.launch {
            busy = true
            error = null
            try {
                val body = buildJsonObject {
                    put("input", input.trim())
                    caseId?.let { put("caseId", it) }
                    val opts = buildJsonObject {
                        if (tool.name in CtosLogic.DEPTH_TOOLS) put("depth", depth)
                        CtosLogic.MODE_TOOLS[tool.name]?.let { (key, _) ->
                            if (mode.isNotBlank()) put(key, mode)
                        }
                        if (tool.name == "metadata") {
                            put("fileB64", pickedB64 ?: "")
                            pickedName?.let { put("filename", it) }
                        }
                    }
                    if (opts.size > 0) put("options", opts)
                }
                result = viewModel.api.ctosRunTool(tool.name, body)
            } catch (e: Exception) {
                error = plainError(e, "The tool could not run. Try again.")
            } finally {
                busy = false
            }
        }
    }

    LazyColumn {
        item {
            MirrorCard("Run ${tool.label}", topTier = true) {
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
                if (cases.isNotEmpty()) {
                    Text("Record results into", style = MaterialTheme.typography.labelLarge)
                    ChipRow(
                        options = cases.map { it.title },
                        selected = cases.firstOrNull { it.id == caseId }?.title,
                        onPick = { title ->
                            caseId = cases.firstOrNull { it.title == title }?.id
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
                            "Running on Command…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SentinelMuted,
                        )
                    }
                }
                ActionErrorText(error)
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
            }
        }
        val data = parsed.data
        if (data != null) {
            val username = CtosLogic.parseUsernameData(data)
            val modules = Phase3Logic.arr(data, "modules")
            when {
                username != null -> CtosUsernameView(username)
                toolName == "dashboard" && modules.isNotEmpty() -> {
                    CtosDataNodes(
                        title = "Target",
                        nodes = CtosLogic.shapeData(
                            JsonObject(data.filterKeys { it != "modules" }),
                        ),
                    )
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
                else -> CtosDataNodes(title = "Data", nodes = CtosLogic.shapeData(data))
            }
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

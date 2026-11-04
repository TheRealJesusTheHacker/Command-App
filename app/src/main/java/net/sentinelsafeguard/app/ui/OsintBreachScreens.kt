package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
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
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * OSINT + Breach Intel (Phase 6). OSINT mirrors web's workspace:
 * New search (type + value + the mandatory purpose note — the
 * server refuses without one, in its own words), Detect,
 * Searches, Reports, Watchlist with Run now, plus the payload's
 * Dork pack and Checklist for the chosen type. Breach Intel
 * mirrors the living register: stats, filters, detail with
 * circulation intelligence, CRUD, and Run breach intake — under
 * the payload's own boundary copy (venue links are never
 * stored; the server refuses them in its own words).
 */

@Composable
fun OsintScreen(viewModel: AppViewModel, nav: NavController) {
    val scope = rememberCoroutineScope()
    val state = rememberJsonScreen { viewModel.api.osintWorkspace() }
    var typeKey by remember { mutableStateOf("email") }
    var value by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var watchLabel by remember { mutableStateOf("") }
    var detectNote by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reportId by remember { mutableStateOf<Int?>(null) }
    var confirmDeleteReport by remember { mutableStateOf<Int?>(null) }
    var confirmDeleteWatch by remember { mutableStateOf<Int?>(null) }
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
        MirrorHeader(MirrorCopy.title("osint", "OSINT"), MirrorCopy.subtitle("osint"))
        when {
            state.error != null && state.data == null -> ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null -> SkeletonList()
            else -> {
                val data = state.data!!
                val types = Phase3Logic.arr(data, "types")
                LazyColumn {
                    item {
                        MirrorCard("New search") {
                            if (types.isNotEmpty()) {
                                ChoiceMenu(
                                    "Identifier type",
                                    types.firstOrNull { Phase3Logic.str(it, "key") == typeKey }
                                        ?.let { Phase3Logic.str(it, "label") } ?: typeKey,
                                    types.map { Phase3Logic.str(it, "label") },
                                ) { picked ->
                                    typeKey = types.firstOrNull { Phase3Logic.str(it, "label") == picked }
                                        ?.let { Phase3Logic.str(it, "key") } ?: typeKey
                                }
                            }
                            TextFieldRow("Identifier", value, onChange = { value = it })
                            TextFieldRow("Purpose note (required)", purpose, onChange = { purpose = it })
                            if (detectNote != null) Text(detectNote!!, color = SentinelMuted)
                            ActionErrorText(error)
                            if (notice != null) Text(notice!!)
                            Row {
                                Button(
                                    onClick = {
                                        run {
                                            viewModel.api.osintCreateSearch(buildJsonObject {
                                                put("purpose", purpose)
                                                put("identifiers", JsonArray(listOf(buildJsonObject {
                                                    put("type", typeKey); put("value", value)
                                                })))
                                            })
                                            "Search queued."
                                        }
                                    },
                                    modifier = Modifier.padding(end = 8.dp),
                                ) { Text("Submit search") }
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        try {
                                            val out = viewModel.api.osintDetect(value)
                                            val candidates = Phase3Logic.arrRaw(out, "candidates")
                                                .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                                            detectNote = "Detect: " + candidates.joinToString(", ").ifBlank { "no match" }
                                        } catch (e: Exception) {
                                            error = plainError(e, "Detect could not run. Try again.")
                                        }
                                    }
                                }) { Text("Detect") }
                            }
                            Spacer(Modifier.height(8.dp))
                            TextFieldRow("Watch label", watchLabel, onChange = { watchLabel = it })
                            Button(onClick = {
                                run {
                                    viewModel.api.osintCreateWatch(buildJsonObject {
                                        put("purpose", purpose)
                                        put("label", watchLabel)
                                        put("identifiers", JsonArray(listOf(buildJsonObject {
                                            put("type", typeKey); put("value", value)
                                        })))
                                    })
                                    "Watch saved."
                                }
                            }) { Text("Save watch") }
                        }
                    }
                    val searches = Phase3Logic.arr(data, "searches")
                    item { SectionHeader("Searches", searches.size) }
                    items(searches) { search ->
                        ItemRow(
                            title = Phase3Logic.str(search, "subject"),
                            subtitle = listOfNotNull(
                                Phase3Logic.strOrNull(search, "status"),
                                Phase3Logic.strOrNull(search, "purpose"),
                                Phase3Logic.strOrNull(search, "requestedByName"),
                            ).joinToString(" · "),
                        )
                    }
                    val reports = Phase3Logic.arr(data, "reports")
                    item { SectionHeader("Reports", reports.size) }
                    items(reports) { report ->
                        ItemRow(
                            title = Phase3Logic.str(report, "title"),
                            subtitle = listOfNotNull(
                                Phase3Logic.strOrNull(report, "subject"),
                                Phase3Logic.strOrNull(report, "summary"),
                            ).joinToString(" · "),
                            onClick = { reportId = Phase3Logic.int(report, "id") },
                        )
                    }
                    val watches = Phase3Logic.arr(data, "watches")
                    item { SectionHeader("Watchlist", watches.size) }
                    items(watches) { watch ->
                        val wid = Phase3Logic.int(watch, "id")
                        MirrorCard(Phase3Logic.str(watch, "label")) {
                            JsonLines(watch, "purpose" to "Purpose", "changeSummary" to "Changes")
                            DetailLine("Active", if (Phase3Logic.bool(watch, "active") == true) "Yes" else "No")
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
                        val dorks = Phase3Logic.obj(data, "dorkTemplates")?.get(typeKey)
                        val checklist = Phase3Logic.obj(data, "checklists")?.get(typeKey)
                        MirrorCard("Dork pack") {
                            Phase6Logic.flatten(dorks).forEach { (_, v) -> Text(v) }
                        }
                        MirrorCard("Checklist") {
                            Phase6Logic.flatten(checklist).forEach { (_, v) -> Text("• $v") }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
    reportId?.let { id ->
        OsintReportSheet(viewModel, id, onClose = { reportId = null },
            onDelete = { confirmDeleteReport = id; reportId = null })
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
private fun OsintReportSheet(viewModel: AppViewModel, id: Int, onClose: () -> Unit, onDelete: () -> Unit) {
    val state = rememberJsonScreen { viewModel.api.osintReport(id) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onClose,
        title = { Text("OSINT report") },
        text = {
            Column {
                when {
                    state.error != null && state.data == null -> Text(state.error!!, color = MaterialTheme.colorScheme.error)
                    state.data == null -> SkeletonList(3)
                    else -> {
                        val report = Phase3Logic.obj(state.data, "report") ?: state.data!!
                        Text(Phase3Logic.str(report, "title"), style = MaterialTheme.typography.titleSmall)
                        JsonLines(report, "subject" to "Subject", "summary" to "Summary",
                            "createdByName" to "Created by", "body" to "Report")
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDelete) { Text("Delete report") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onClose) { Text("Close") }
        },
    )
}

// ---- Breach Intel ----

@Composable
fun BreachIntelScreen(viewModel: AppViewModel, nav: NavController) {
    var sector by remember { mutableStateOf<String?>(null) }
    var detailId by remember { mutableStateOf<Int?>(null) }
    var editing by remember { mutableStateOf<Int?>(null) }
    var creating by remember { mutableStateOf(false) }
    val state = rememberJsonScreen { viewModel.api.breachIntel(sector = sector) }
    LaunchedEffect(sector) { state.reloadKey++ }
    Column {
        MirrorHeader("Breach Intel", MirrorCopy.subtitle("breachintel")) {
            Button(onClick = { creating = true }) { Text("Record a breach") }
        }
        state.data?.let { data ->
            val sectors = Phase3Logic.arrRaw(data, "sectors")
                .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
            ChipRow(
                options = listOf("all" to "All sectors") + sectors.map { it to it },
                selected = sector ?: "all",
                onPick = { sector = it },
            )
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
                            MirrorCard("Register") {
                                JsonLines(stats, "totalBreaches" to "Total breaches",
                                    "breachesThisMonth" to "Breaches this month",
                                    "recordsThisMonth" to "Records this month",
                                    "circulatingPublicly" to "Circulating publicly",
                                    "month" to "Month")
                                Phase3Logic.strOrNull(data, "boundaryCopy")?.let {
                                    Text(it, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    val rows = Phase3Logic.arr(data, "breaches")
                    item { SectionHeader("Breaches", rows.size) }
                    if (rows.isEmpty()) {
                        item { EmptyBox("No breaches recorded", "Breach entries appear here.") }
                    }
                    items(rows) { breach ->
                        ItemRow(
                            title = Phase3Logic.str(breach, "org"),
                            subtitle = listOfNotNull(
                                Phase3Logic.strOrNull(breach, "sector"),
                                Phase3Logic.strOrNull(breach, "severity"),
                                Phase3Logic.int(breach, "recordsAffected")?.let { "$it records" },
                                Phase3Logic.strOrNull(breach, "disclosureDate"),
                            ).joinToString(" · "),
                            onClick = { detailId = Phase3Logic.int(breach, "id") },
                        )
                    }
                    val runs = Phase3Logic.arr(data, "intakeRuns")
                    item { SectionHeader("Intake runs", runs.size) }
                    items(runs) { runRow ->
                        ItemRow(
                            title = "Intake run #${Phase3Logic.int(runRow, "id") ?: ""}",
                            subtitle = listOfNotNull(
                                Phase3Logic.strOrNull(runRow, "status"),
                                Phase3Logic.strOrNull(runRow, "note"),
                            ).joinToString(" · "),
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
    detailId?.let { id ->
        BreachDetailSheet(viewModel, id,
            onClose = { detailId = null },
            onEdit = { editing = id; detailId = null },
            onChanged = { state.reloadKey++ })
    }
    if (creating || editing != null) {
        BreachFormSheet(viewModel, editing, state.data,
            onClose = { creating = false; editing = null },
            onSaved = { creating = false; editing = null; state.reloadKey++ })
    }
}

@Composable
private fun BreachDetailSheet(
    viewModel: AppViewModel, breachId: Int,
    onClose: () -> Unit, onEdit: () -> Unit, onChanged: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var venueType by remember { mutableStateOf("") }
    var venueName by remember { mutableStateOf("") }
    var circStatus by remember { mutableStateOf("") }
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
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Breach Intel") },
        text = {
            Column {
                when {
                    state.error != null && state.data == null -> Text(state.error!!, color = MaterialTheme.colorScheme.error)
                    state.data == null -> SkeletonList(3)
                    else -> {
                        val data = state.data!!
                        val detail = Phase3Logic.obj(data, "detail")
                        if (detail == null) {
                            Text("This breach could not be found.", color = SentinelMuted)
                        } else {
                            Text(Phase3Logic.str(detail, "org"), style = MaterialTheme.typography.titleSmall)
                            JsonLines(detail, "sector" to "Sector", "incidentDate" to "Incident date",
                                "disclosureDate" to "Disclosure date", "attackVector" to "Attack vector",
                                "severity" to "Severity", "summary" to "Summary",
                                "createdByName" to "Recorded by")
                            DetailLine("Records affected", Phase3Logic.int(detail, "recordsAffected")?.toString())
                            val classes = Phase3Logic.arrRaw(detail, "dataClasses")
                                .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                            if (classes.isNotEmpty()) DetailLine("Data classes", classes.joinToString(", "))
                            Phase3Logic.arr(detail, "sources").forEach { source ->
                                DetailLine("Source", listOfNotNull(
                                    Phase3Logic.strOrNull(source, "label"),
                                    Phase3Logic.strOrNull(source, "url")).joinToString(" · "))
                            }
                            SectionHeader("Circulation")
                            Phase3Logic.arr(detail, "circulation").forEach { circ ->
                                val cid = Phase3Logic.int(circ, "id")
                                JsonLines(circ, "venueType" to "Venue type", "venueName" to "Venue",
                                    "status" to "Status", "intelSourceLabel" to "Intel source")
                                cid?.let {
                                    OutlinedButton(onClick = {
                                        run { viewModel.api.breachCirculationDelete(it); "Circulation deleted." }
                                    }) { Text("Delete circulation") }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Record circulation intel", style = MaterialTheme.typography.titleSmall)
                            val venueTypes = Phase3Logic.arrRaw(data, "venueTypes")
                                .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                            val circStatuses = Phase3Logic.arrRaw(data, "circStatuses")
                                .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                            if (venueTypes.isNotEmpty()) {
                                ChoiceMenu("Venue type", venueType.ifBlank { venueTypes.first() }, venueTypes) { venueType = it }
                            }
                            TextFieldRow("Venue name (never a link to the leak)", venueName, onChange = { venueName = it })
                            if (circStatuses.isNotEmpty()) {
                                ChoiceMenu("Status", circStatus.ifBlank { circStatuses.first() }, circStatuses) { circStatus = it }
                            }
                            TextFieldRow("Intel source URL (the reporting)", sourceUrl, onChange = { sourceUrl = it })
                            TextFieldRow("Intel source label", sourceLabel, onChange = { sourceLabel = it })
                            Button(onClick = {
                                run {
                                    viewModel.api.breachCirculationCreate(breachId, buildJsonObject {
                                        put("venueType", venueType.ifBlank { venueTypes.firstOrNull() ?: "" })
                                        put("venueName", venueName)
                                        put("status", circStatus.ifBlank { circStatuses.firstOrNull() ?: "" })
                                        put("intelSourceUrl", sourceUrl)
                                        put("intelSourceLabel", sourceLabel)
                                    })
                                    "Circulation recorded."
                                }
                            }) { Text("Record circulation intel") }
                            Spacer(Modifier.height(8.dp))
                            Row {
                                OutlinedButton(onClick = {
                                    run { viewModel.api.breachIntakeRun(); "Breach intake queued." }
                                }, modifier = Modifier.padding(end = 8.dp)) { Text("Run breach intake") }
                                OutlinedButton(onClick = { confirmDelete = true }) { Text("Delete breach") }
                            }
                            if (notice != null) Text(notice!!)
                            ActionErrorText(error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onEdit) { Text("Edit breach") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onClose) { Text("Close") }
        },
    )
    if (confirmDelete) {
        ConfirmGate("Delete breach", "This permanently deletes this breach entry and its circulation intel.", "Delete",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                run { viewModel.api.breachDelete(breachId); "Breach deleted." }
                onClose()
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
                        .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                        .joinToString(", ")
                }
            } catch (e: Exception) {
                error = plainError(e, "Couldn't load this breach. Try again.")
            }
        }
    }
    val sectors = workspace?.let {
        Phase3Logic.arrRaw(it, "sectors")
            .mapNotNull { el -> (el as? kotlinx.serialization.json.JsonPrimitive)?.content }
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
                                .map { kotlinx.serialization.json.JsonPrimitive(it) }))
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

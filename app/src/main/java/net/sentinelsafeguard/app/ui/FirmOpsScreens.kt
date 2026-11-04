package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.data.api.firmAvailCreate
import net.sentinelsafeguard.app.data.api.firmAvailDelete
import net.sentinelsafeguard.app.data.api.firmAvailPatch
import net.sentinelsafeguard.app.data.api.firmAvailability
import net.sentinelsafeguard.app.data.api.firmBookingWarnings
import net.sentinelsafeguard.app.data.api.firmJobCosting
import net.sentinelsafeguard.app.data.api.firmJobCosts
import net.sentinelsafeguard.app.data.api.firmTaskConvert
import net.sentinelsafeguard.app.data.api.firmTaskCreate
import net.sentinelsafeguard.app.data.api.firmTaskPatch
import net.sentinelsafeguard.app.data.api.firmTaskState
import net.sentinelsafeguard.app.data.api.firmTasks
import net.sentinelsafeguard.app.data.api.firmVendorCreate
import net.sentinelsafeguard.app.data.api.firmVendorDelete
import net.sentinelsafeguard.app.data.api.firmVendorPatch
import net.sentinelsafeguard.app.data.api.firmVendors
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * Firm Ops (Phase 8A) — the mirror of web's cf2firmops view:
 * Job costing, Vendor directory, Owner task board, Staff
 * availability, and the booking-warnings strip, in web's
 * section order. The vendor directory is internal only, as
 * web's own note says; job costing is owner-only inside the
 * handler, and its refusal surfaces in the server's words.
 */
@Composable
fun FirmOpsScreen(viewModel: AppViewModel, nav: NavController) {
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    Column {
        MirrorHeader("Firm Ops")
        LazyColumn {
            item {
                if (notice != null) {
                    Text(
                        notice!!, color = SentinelMuted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                if (error != null) {
                    Text(
                        error!!, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            item { BookingWarningsCard(viewModel) }
            item { JobCostsCard(viewModel, onError = { error = it }) }
            item {
                VendorsCard(
                    viewModel,
                    onNotice = { notice = it; error = null },
                    onError = { error = it },
                )
            }
            item {
                OwnerTasksCard(
                    viewModel, nav,
                    onNotice = { notice = it; error = null },
                    onError = { error = it },
                )
            }
            item {
                AvailabilityCard(
                    viewModel,
                    onNotice = { notice = it; error = null },
                    onError = { error = it },
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun BookingWarningsCard(viewModel: AppViewModel) {
    val state = rememberJsonScreen { viewModel.api.firmBookingWarnings() }
    val data = state.data
    if (data == null && state.error == null) return
    MirrorCard("Booking warnings") {
        when {
            state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error)
            data != null -> {
                val warnings = Phase3Logic.arr(data, "warnings")
                if (warnings.isEmpty()) {
                    Text("No booking warnings.", color = SentinelMuted)
                }
                warnings.forEach { w ->
                    Phase6Logic.flatten(w).forEach { (k, v) ->
                        Text("$k: $v", color = SentinelMuted)
                    }
                    Spacer(Modifier.height(4.dp))
                }
                val note = Phase3Logic.str(data, "note")
                if (note.isNotBlank()) {
                    Text(note, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun JobCostsCard(viewModel: AppViewModel, onError: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val state = rememberJsonScreen { viewModel.api.firmJobCosts() }
    var detail by remember { mutableStateOf<JsonObject?>(null) }
    MirrorCard("Job costing") {
        when {
            state.error != null -> {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { state.reloadKey++ }) { Text("Retry") }
            }
            state.data == null -> Text("Loading…", color = SentinelMuted)
            else -> {
                val data = state.data!!
                val note = Phase3Logic.str(data, "note")
                if (note.isNotBlank()) {
                    Text(note, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(6.dp))
                }
                val jobs = Phase3Logic.arr(data, "jobs")
                if (jobs.isEmpty()) {
                    Text("No jobs to cost yet.", color = SentinelMuted)
                }
                jobs.forEach { job ->
                    Text(Phase3Logic.str(job, "title"))
                    Text(
                        Phase8Logic.costingMeta(job),
                        color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
                    )
                    val margin = Phase8Logic.marginLabel(job)
                    if (margin.isNotBlank()) {
                        Text(margin, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = {
                        val kind = Phase3Logic.str(job, "kind")
                        val id = Phase3Logic.int(job, "id") ?: return@TextButton
                        scope.launch {
                            try {
                                val out = viewModel.api.firmJobCosting(kind, id)
                                detail = Phase3Logic.obj(out, "costing")
                            } catch (e: Exception) {
                                onError(plainError(e, "That job's costing could not be loaded."))
                            }
                        }
                    }) { Text("Job costing") }
                    Spacer(Modifier.height(4.dp))
                }
                detail?.let { c ->
                    Spacer(Modifier.height(6.dp))
                    Text(Phase3Logic.str(c, "title"), style = MaterialTheme.typography.titleSmall)
                    JsonLines(
                        c,
                        "clientName" to "Client",
                        "status" to "Status",
                        "timeEntries" to "Time entries",
                        "timeHours" to "Time (hours)",
                        "timeCostCents" to "Time cost",
                        "expenseTotalCents" to "Expenses",
                        "partsTotalCents" to "Parts",
                        "totalCostCents" to "Total cost",
                        "invoicedCents" to "Invoiced",
                        "marginCents" to "Margin",
                        "revenueNote" to "Revenue note",
                        "marginNote" to "Margin note",
                    )
                }
            }
        }
    }
}

@Composable
private fun VendorsCard(
    viewModel: AppViewModel,
    onNotice: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val state = rememberJsonScreen { viewModel.api.firmVendors() }
    var editingId by remember { mutableStateOf<Int?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var service by remember { mutableStateOf("") }
    var contractStart by remember { mutableStateOf("") }
    var renewalDate by remember { mutableStateOf("") }
    var costNote by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<JsonObject?>(null) }

    fun openForm(v: JsonObject?) {
        editingId = v?.let { Phase3Logic.int(it, "id") }
        name = v?.let { Phase3Logic.str(it, "name") } ?: ""
        contactName = v?.let { Phase3Logic.str(it, "contactName") } ?: ""
        contactEmail = v?.let { Phase3Logic.str(it, "contactEmail") } ?: ""
        contactPhone = v?.let { Phase3Logic.str(it, "contactPhone") } ?: ""
        service = v?.let { Phase3Logic.str(it, "service") } ?: ""
        contractStart = v?.let { Phase3Logic.str(it, "contractStart") } ?: ""
        renewalDate = v?.let { Phase3Logic.str(it, "renewalDate") } ?: ""
        costNote = v?.let { Phase3Logic.str(it, "costNote") } ?: ""
        notes = v?.let { Phase3Logic.str(it, "notes") } ?: ""
        showForm = true
    }

    MirrorCard("Vendor directory") {
        when {
            state.error != null -> {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { state.reloadKey++ }) { Text("Retry") }
            }
            state.data == null -> Text("Loading…", color = SentinelMuted)
            else -> {
                val data = state.data!!
                val note = Phase3Logic.str(data, "note")
                if (note.isNotBlank()) {
                    Text(note, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(6.dp))
                }
                val vendors = Phase3Logic.arr(data, "vendors")
                if (vendors.isEmpty()) Text("No vendors yet.", color = SentinelMuted)
                vendors.forEach { v ->
                    Text(Phase3Logic.str(v, "name"))
                    val meta = listOf(
                        Phase3Logic.str(v, "service"),
                        Phase3Logic.str(v, "contactEmail"),
                        Phase3Logic.str(v, "renewalDate").let { if (it.isBlank()) "" else "Renews $it" },
                    ).filter { it.isNotBlank() }.joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Text(meta, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    Row {
                        TextButton(onClick = { openForm(v) }) { Text("Edit") }
                        TextButton(onClick = { deleteTarget = v }) { Text("Delete") }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                if (!showForm) {
                    Button(onClick = { openForm(null) }) { Text("Add vendor") }
                }
                if (showForm) {
                    TextFieldRow("Name", name, { name = it })
                    TextFieldRow("Contact name", contactName, { contactName = it })
                    TextFieldRow("Contact email", contactEmail, { contactEmail = it })
                    TextFieldRow("Contact phone", contactPhone, { contactPhone = it })
                    TextFieldRow("Service", service, { service = it })
                    TextFieldRow("Contract start", contractStart, { contractStart = it })
                    TextFieldRow("Renewal date", renewalDate, { renewalDate = it })
                    TextFieldRow("Cost note", costNote, { costNote = it })
                    TextFieldRow("Notes", notes, { notes = it }, singleLine = false, minLines = 2)
                    Row {
                        Button(onClick = {
                            if (name.isBlank()) { onError("Name is required."); return@Button }
                            scope.launch {
                                try {
                                    val out = if (editingId == null) {
                                        viewModel.api.firmVendorCreate(buildJsonObject {
                                            put("name", name.trim())
                                            put("contactName", contactName)
                                            put("contactEmail", contactEmail)
                                            put("contactPhone", contactPhone)
                                            put("service", service)
                                            put("contractStart", contractStart)
                                            put("renewalDate", renewalDate)
                                            put("costNote", costNote)
                                            put("notes", notes)
                                        })
                                    } else {
                                        viewModel.api.firmVendorPatch(editingId!!, buildJsonObject {
                                            put("name", name.trim())
                                            put("contactName", contactName)
                                            put("contactEmail", contactEmail)
                                            put("contactPhone", contactPhone)
                                            put("service", service)
                                            put("contractStart", contractStart)
                                            put("renewalDate", renewalDate)
                                            put("costNote", costNote)
                                            put("notes", notes)
                                        })
                                    }
                                    onNotice(
                                        Phase3Logic.str(out, "message")
                                            .ifBlank { "Vendor saved." },
                                    )
                                    showForm = false
                                    state.reloadKey++
                                } catch (e: Exception) {
                                    onError(plainError(e, "Could not save the vendor. Try again."))
                                }
                            }
                        }) { Text("Save vendor") }
                        OutlinedButton(onClick = { showForm = false }) { Text("Cancel") }
                    }
                }
            }
        }
    }

    deleteTarget?.let { v ->
        val id = Phase3Logic.int(v, "id") ?: 0
        ConfirmGate(
            title = "Delete this vendor?",
            body = Phase3Logic.str(v, "name") + "\n\nThis cannot be undone.",
            confirmLabel = "Delete",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                scope.launch {
                    try {
                        val out = viewModel.api.firmVendorDelete(id)
                        onNotice(Phase3Logic.str(out, "message"))
                        state.reloadKey++
                    } catch (e: Exception) {
                        onError(plainError(e, "Could not delete the vendor. Try again."))
                    }
                }
            },
        )
    }
}

@Composable
private fun OwnerTasksCard(
    viewModel: AppViewModel,
    nav: NavController,
    onNotice: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var stateFilter by remember { mutableStateOf<String?>(null) }
    val state = rememberJsonScreen { viewModel.api.firmTasks(stateFilter) }
    androidx.compose.runtime.LaunchedEffect(stateFilter) { state.reloadKey++ }
    var editingId by remember { mutableStateOf<Int?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Normal") }
    var convertTarget by remember { mutableStateOf<JsonObject?>(null) }

    MirrorCard("Owner tasks") {
        when {
            state.error != null -> {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { state.reloadKey++ }) { Text("Retry") }
            }
            state.data == null -> Text("Loading…", color = SentinelMuted)
            else -> {
                val data = state.data!!
                val note = Phase3Logic.str(data, "note")
                if (note.isNotBlank()) {
                    Text(note, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(6.dp))
                }
                Row {
                    FilterChip(
                        selected = stateFilter == null,
                        onClick = { stateFilter = null },
                        label = { Text("All") },
                    )
                    Phase8Logic.taskStates(data).forEach { s ->
                        Spacer(Modifier.padding(horizontal = 3.dp))
                        FilterChip(
                            selected = stateFilter == s,
                            onClick = { stateFilter = s },
                            label = { Text(Phase8Logic.stateLabel(s)) },
                        )
                    }
                }
                val tasks = Phase3Logic.arr(data, "tasks")
                if (tasks.isEmpty()) Text("No tasks here.", color = SentinelMuted)
                tasks.forEach { t ->
                    val id = Phase3Logic.int(t, "id") ?: 0
                    Text(Phase3Logic.str(t, "title"))
                    val meta = listOf(
                        Phase3Logic.str(t, "priority"),
                        Phase3Logic.str(t, "state"),
                        Phase3Logic.str(t, "clientName"),
                        Phase3Logic.str(t, "dueDate").let { if (it.isBlank()) "" else "Due $it" },
                    ).filter { it.isNotBlank() }.joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Text(meta, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    val linked = Phase3Logic.int(t, "linkedTicketId")
                    Row {
                        TextButton(onClick = {
                            editingId = id
                            showForm = true
                            title = Phase3Logic.str(t, "title")
                            detail = Phase3Logic.str(t, "detail")
                            dueDate = Phase3Logic.str(t, "dueDate")
                            priority = Phase3Logic.str(t, "priority").ifBlank { "Normal" }
                        }) { Text("Edit") }
                        if (Phase3Logic.str(t, "state") == "open") {
                            TextButton(onClick = {
                                scope.launch {
                                    try {
                                        val out = viewModel.api.firmTaskState(id, "done")
                                        onNotice(
                                            Phase3Logic.str(out, "message").ifBlank { "Task done." },
                                        )
                                        state.reloadKey++
                                    } catch (e: Exception) {
                                        onError(plainError(e, "Could not update the task."))
                                    }
                                }
                            }) { Text("Done") }
                            TextButton(onClick = {
                                scope.launch {
                                    try {
                                        val out = viewModel.api.firmTaskState(id, "dropped")
                                        onNotice(
                                            Phase3Logic.str(out, "message").ifBlank { "Task dropped." },
                                        )
                                        state.reloadKey++
                                    } catch (e: Exception) {
                                        onError(plainError(e, "Could not update the task."))
                                    }
                                }
                            }) { Text("Drop") }
                            TextButton(onClick = { convertTarget = t }) { Text("Convert to ticket") }
                        }
                        if (linked != null) {
                            TextButton(onClick = { nav.navigate("ticket/$linked") }) {
                                Text("Ticket #$linked")
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                if (!showForm) {
                    Button(onClick = {
                        editingId = null
                        showForm = true
                        title = ""; detail = ""; dueDate = ""; priority = "Normal"
                    }) { Text("Add task") }
                }
                if (showForm) {
                    TextFieldRow("Title", title, { title = it })
                    TextFieldRow("Detail", detail, { detail = it }, singleLine = false, minLines = 2)
                    TextFieldRow("Due date", dueDate, { dueDate = it })
                    ChoiceMenu("Priority", priority, MirrorVocab.PRIORITIES) { priority = it }
                    Row {
                        Button(onClick = {
                            if (title.isBlank()) { onError("Title is required."); return@Button }
                            scope.launch {
                                try {
                                    val out = if (editingId == null) {
                                        viewModel.api.firmTaskCreate(buildJsonObject {
                                            put("title", title.trim())
                                            put("detail", detail)
                                            put("dueDate", dueDate)
                                            put("priority", priority)
                                        })
                                    } else {
                                        viewModel.api.firmTaskPatch(editingId!!, buildJsonObject {
                                            put("title", title.trim())
                                            put("detail", detail)
                                            put("dueDate", dueDate)
                                            put("priority", priority)
                                        })
                                    }
                                    onNotice(
                                        Phase3Logic.str(out, "message").ifBlank { "Task saved." },
                                    )
                                    showForm = false
                                    state.reloadKey++
                                } catch (e: Exception) {
                                    onError(plainError(e, "Could not save the task. Try again."))
                                }
                            }
                        }) { Text("Save task") }
                        OutlinedButton(onClick = { showForm = false }) { Text("Cancel") }
                    }
                }
            }
        }
    }

    convertTarget?.let { t ->
        val id = Phase3Logic.int(t, "id") ?: 0
        ConfirmGate(
            title = "Convert to ticket?",
            body = Phase3Logic.str(t, "title") +
                "\n\nConverting creates a NEW ticket; this task is marked done and linked to it.",
            confirmLabel = "Convert to ticket",
            onDismiss = { convertTarget = null },
            onConfirm = {
                convertTarget = null
                scope.launch {
                    try {
                        val out = viewModel.api.firmTaskConvert(id)
                        onNotice(Phase3Logic.str(out, "message"))
                        state.reloadKey++
                    } catch (e: Exception) {
                        onError(plainError(e, "Could not convert the task. Try again."))
                    }
                }
            },
        )
    }
}

@Composable
private fun AvailabilityCard(
    viewModel: AppViewModel,
    onNotice: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val state = rememberJsonScreen { viewModel.api.firmAvailability() }
    var editingId by remember { mutableStateOf<Int?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var staffPick by remember { mutableStateOf<JsonObject?>(null) }
    var dateFrom by remember { mutableStateOf("") }
    var dateTo by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("unavailable") }
    var note by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<JsonObject?>(null) }

    MirrorCard("Staff availability") {
        when {
            state.error != null -> {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { state.reloadKey++ }) { Text("Retry") }
            }
            state.data == null -> Text("Loading…", color = SentinelMuted)
            else -> {
                val data = state.data!!
                val staff = Phase3Logic.arr(data, "staff")
                val statuses = Phase8Logic.availStatuses(data)
                val warnings = Phase3Logic.arr(data, "bookingDayWarnings")
                warnings.forEach { w ->
                    Phase6Logic.flatten(w).forEach { (k, v) -> Text("$k: $v", color = SentinelMuted) }
                    Spacer(Modifier.height(4.dp))
                }
                val records = Phase3Logic.arr(data, "records")
                if (records.isEmpty()) Text("No availability records.", color = SentinelMuted)
                records.forEach { r ->
                    Text(Phase3Logic.str(r, "userName"))
                    val meta = listOf(
                        Phase3Logic.str(r, "status"),
                        "${Phase3Logic.str(r, "dateFrom")} → ${Phase3Logic.str(r, "dateTo")}",
                        Phase3Logic.str(r, "note"),
                    ).filter { it.isNotBlank() }.joinToString(" · ")
                    Text(meta, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
                    Row {
                        TextButton(onClick = {
                            editingId = Phase3Logic.int(r, "id")
                            showForm = true
                            staffPick = staff.firstOrNull {
                                Phase3Logic.int(it, "id") == Phase3Logic.int(r, "userId")
                            }
                            dateFrom = Phase3Logic.str(r, "dateFrom")
                            dateTo = Phase3Logic.str(r, "dateTo")
                            status = Phase3Logic.str(r, "status").ifBlank { "unavailable" }
                            note = Phase3Logic.str(r, "note")
                        }) { Text("Edit") }
                        TextButton(onClick = { deleteTarget = r }) { Text("Delete") }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                if (!showForm) {
                    Button(onClick = {
                        editingId = null
                        showForm = true
                        staffPick = null; dateFrom = ""; dateTo = ""
                        status = "unavailable"; note = ""
                    }) { Text("Add record") }
                }
                if (showForm) {
                    if (staff.isNotEmpty()) {
                        ChoiceMenu(
                            "Staff member",
                            staffPick?.let { Phase3Logic.str(it, "displayName") }
                                ?: Phase3Logic.str(staff.first(), "displayName"),
                            staff.map { Phase3Logic.str(it, "displayName") },
                        ) { picked ->
                            staffPick = staff.firstOrNull { Phase3Logic.str(it, "displayName") == picked }
                        }
                    }
                    TextFieldRow("Date from", dateFrom, { dateFrom = it })
                    TextFieldRow("Date to", dateTo, { dateTo = it })
                    ChoiceMenu(
                        "Status",
                        Phase8Logic.stateLabel(status),
                        statuses.map { Phase8Logic.stateLabel(it) },
                    ) { picked ->
                        status = statuses.firstOrNull { Phase8Logic.stateLabel(it) == picked }
                            ?: picked.lowercase()
                    }
                    TextFieldRow("Note", note, { note = it })
                    Row {
                        Button(onClick = {
                            scope.launch {
                                try {
                                    val out = if (editingId == null) {
                                        val userId = staffPick?.let { Phase3Logic.int(it, "id") }
                                            ?: staff.firstOrNull()?.let { Phase3Logic.int(it, "id") }
                                        if (userId == null) {
                                            onError("Pick a staff member.")
                                            return@launch
                                        }
                                        viewModel.api.firmAvailCreate(buildJsonObject {
                                            put("userId", userId)
                                            put("dateFrom", dateFrom)
                                            put("dateTo", dateTo)
                                            put("status", status)
                                            put("note", note)
                                        })
                                    } else {
                                        viewModel.api.firmAvailPatch(editingId!!, buildJsonObject {
                                            put("dateFrom", dateFrom)
                                            put("dateTo", dateTo)
                                            put("status", status)
                                            put("note", note)
                                        })
                                    }
                                    onNotice(
                                        Phase3Logic.str(out, "message").ifBlank { "Availability saved." },
                                    )
                                    showForm = false
                                    state.reloadKey++
                                } catch (e: Exception) {
                                    onError(plainError(e, "Could not save availability. Try again."))
                                }
                            }
                        }) { Text("Save") }
                        OutlinedButton(onClick = { showForm = false }) { Text("Cancel") }
                    }
                }
            }
        }
    }

    deleteTarget?.let { r ->
        val id = Phase3Logic.int(r, "id") ?: 0
        ConfirmGate(
            title = "Delete this record?",
            body = "${Phase3Logic.str(r, "userName")} · ${Phase3Logic.str(r, "dateFrom")} → " +
                "${Phase3Logic.str(r, "dateTo")}\n\nThis cannot be undone.",
            confirmLabel = "Delete",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                scope.launch {
                    try {
                        val out = viewModel.api.firmAvailDelete(id)
                        onNotice(Phase3Logic.str(out, "message"))
                        state.reloadKey++
                    } catch (e: Exception) {
                        onError(plainError(e, "Could not delete the record. Try again."))
                    }
                }
            },
        )
    }
}

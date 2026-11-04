package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import net.sentinelsafeguard.app.data.api.CaseItem
import net.sentinelsafeguard.app.data.api.Client
import net.sentinelsafeguard.app.data.api.Job
import net.sentinelsafeguard.app.data.api.JobFullEdit
import net.sentinelsafeguard.app.data.api.ScheduleEntry
import net.sentinelsafeguard.app.data.api.ScheduleEntryRequest
import net.sentinelsafeguard.app.data.api.ScheduleResponse
import net.sentinelsafeguard.app.data.api.User
import net.sentinelsafeguard.app.ui.theme.SentinelMuted
import net.sentinelsafeguard.app.ui.theme.SentinelOutline
import net.sentinelsafeguard.app.ui.theme.SentinelSurface
import java.time.LocalDate

/**
 * Schedule (mirror spec §6 Phase 2): the full web mirror — Month /
 * Week / People / Agenda modes over the web's own date-window
 * semantics (GET /api/app/schedule?start&end), employee + status
 * filters, job profile with status moves, the full job form
 * (savejob field set), delete, and schedule entries (save/delete).
 * Phase 3 (hunt7) adds web's Booking admin panels (availability,
 * services, bookings manage) as the Booking admin mode
 * (BookingAdmin.kt). This replaces the hunt-era read-only screen,
 * which was disclosed as interim in the hunt5 record.
 */

private enum class SchedMode { Month, Week, People, Agenda, Booking }

private fun SchedMode.label(): String = when (this) {
    SchedMode.Booking -> "Booking admin"
    else -> name
}

private fun windowFor(mode: SchedMode, anchor: LocalDate): Pair<String, String> = when (mode) {
    SchedMode.Month -> MirrorVocab.monthWindow(anchor.toString())
    SchedMode.Week -> MirrorVocab.weekWindow(anchor.toString())
    SchedMode.People -> anchor.toString() to anchor.plusDays(59).toString()
    SchedMode.Agenda, SchedMode.Booking -> anchor.minusDays(7).toString() to anchor.plusDays(60).toString()
}

@Composable
fun ScheduleScreen(viewModel: AppViewModel, nav: NavController) {
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(SchedMode.Week) }
    var anchor by remember { mutableStateOf(LocalDate.now()) }
    var status by remember { mutableStateOf("All") }
    var employee by remember { mutableStateOf("All") }
    var employeeLabel by remember { mutableStateOf("All") }
    var data by remember { mutableStateOf<ScheduleResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var selectedDay by remember { mutableStateOf<String?>(null) }
    var showEntryForm by remember { mutableStateOf(false) }
    val isOwner = viewModel.me?.user?.role == "OWNER"

    val (start, end) = windowFor(mode, anchor)
    LaunchedEffect(start, end, status, employee, reloadKey) {
        error = null
        try {
            data = viewModel.api.scheduleWindow(
                start, end,
                status = status,
                employeeId = employee,
            )
        } catch (e: Exception) {
            error = plainError(e, "The schedule could not be loaded. Check your connection and try again.")
        }
    }

    Column {
        MirrorHeader("Schedule", MirrorCopy.subtitle("schedule")) {
            Button(onClick = { nav.navigate("jobNew") }) { Text("+ New job") }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SchedMode.values().forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = { mode = m },
                    label = { Text(m.label()) },
                )
            }
        }
        if (mode == SchedMode.Booking) {
            BookingAdminPanel(viewModel)
        } else {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = {
                anchor = when (mode) {
                    SchedMode.Month -> anchor.minusMonths(1)
                    else -> anchor.minusWeeks(1)
                }
            }) { Text("‹ Prev") }
            OutlinedButton(onClick = { anchor = LocalDate.now() }) { Text("Today") }
            OutlinedButton(onClick = {
                anchor = when (mode) {
                    SchedMode.Month -> anchor.plusMonths(1)
                    else -> anchor.plusWeeks(1)
                }
            }) { Text("Next ›") }
            Text("$start – $end", color = SentinelMuted)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChoiceMenu(
                "Status", status,
                listOf("All") + MirrorVocab.JOB_STATUSES,
            ) { status = it }
            if (isOwner && data != null) {
                val empOptions = listOf("All") +
                    data!!.employees.map { it.displayName } + listOf("Unassigned")
                ChoiceMenu("Employee", employeeLabel, empOptions) { picked ->
                    employeeLabel = picked
                    employee = when (picked) {
                        "All" -> "All"
                        "Unassigned" -> "unassigned"
                        else -> data!!.employees
                            .firstOrNull { it.displayName == picked }?.id?.toString() ?: "All"
                    }
                }
            }
        }
        when {
            error != null -> ErrorBox(error!!) { reloadKey++ }
            data == null -> SkeletonList(6)
            else -> {
                val d = data!!
                LazyColumn {
                    when (mode) {
                        SchedMode.Month -> {
                            item { MonthGrid(d, start) { day -> selectedDay = day } }
                            val day = selectedDay
                            if (day != null) {
                                item { SectionHeader(day) }
                                val dayJobs = d.jobs.filter { it.workDate == day }
                                val dayEntries = d.entries.filter { it.workDate == day }
                                if (dayJobs.isEmpty() && dayEntries.isEmpty()) {
                                    item { ItemRow("Nothing scheduled", "No jobs or entries on this day.") }
                                }
                                items(dayJobs.size) { i ->
                                    JobRow(dayJobs[i]) { nav.navigate("job/${dayJobs[i].id}") }
                                }
                                items(dayEntries.size) { i ->
                                    EntryRow(dayEntries[i]) {
                                        scope.launch {
                                            try {
                                                viewModel.api.deleteScheduleEntry(dayEntries[i].id)
                                                reloadKey++
                                            } catch (e: Exception) {
                                                error = plainError(e, "The entry could not be deleted. Try again.")
                                            }
                                        }
                                    }
                                }
                                item {
                                    OutlinedButton(
                                        onClick = { nav.navigate("jobNew/$day") },
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    ) { Text("+ New job") }
                                }
                            }
                        }
                        SchedMode.Week, SchedMode.Agenda -> {
                            val grouped = MirrorVocab.groupJobsByDate(d.jobs)
                            if (grouped.isEmpty() && d.entries.isEmpty()) {
                                item { EmptyBox("Nothing scheduled", "No jobs are booked in this window.") }
                            }
                            grouped.forEach { (day, dayJobs) ->
                                item { SectionHeader(day, dayJobs.size) }
                                items(dayJobs.size) { i ->
                                    JobRow(dayJobs[i]) { nav.navigate("job/${dayJobs[i].id}") }
                                }
                                val dayEntries = d.entries.filter { it.workDate == day }
                                items(dayEntries.size) { i ->
                                    EntryRow(dayEntries[i]) {
                                        scope.launch {
                                            try {
                                                viewModel.api.deleteScheduleEntry(dayEntries[i].id)
                                                reloadKey++
                                            } catch (e: Exception) {
                                                error = plainError(e, "The entry could not be deleted. Try again.")
                                            }
                                        }
                                    }
                                }
                            }
                            val orphanDays = d.entries.map { it.workDate }.distinct()
                                .filter { day -> grouped.none { it.first == day } }
                            orphanDays.forEach { day ->
                                item { SectionHeader(day) }
                                val dayEntries = d.entries.filter { it.workDate == day }
                                items(dayEntries.size) { i ->
                                    EntryRow(dayEntries[i]) {
                                        scope.launch {
                                            try {
                                                viewModel.api.deleteScheduleEntry(dayEntries[i].id)
                                                reloadKey++
                                            } catch (e: Exception) {
                                                error = plainError(e, "The entry could not be deleted. Try again.")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        SchedMode.People -> {
                            d.employees.forEach { person ->
                                val theirs = d.jobs.filter { person.id in it.assigneeIds }
                                item { SectionHeader(person.displayName, theirs.size) }
                                if (theirs.isEmpty()) {
                                    item {
                                        Text(
                                            "No jobs in the next 60 days.",
                                            color = SentinelMuted,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                        )
                                    }
                                }
                                items(theirs.size) { i ->
                                    JobRow(theirs[i]) { nav.navigate("job/${theirs[i].id}") }
                                }
                                val theirEntries = d.entries.filter { it.userId == person.id }
                                items(theirEntries.size) { i ->
                                    EntryRow(theirEntries[i]) {
                                        scope.launch {
                                            try {
                                                viewModel.api.deleteScheduleEntry(theirEntries[i].id)
                                                reloadKey++
                                            } catch (e: Exception) {
                                                error = plainError(e, "The entry could not be deleted. Try again.")
                                            }
                                        }
                                    }
                                }
                            }
                            val unassigned = d.jobs.filter { it.assigneeIds.isEmpty() }
                            item { SectionHeader("Unassigned", unassigned.size) }
                            items(unassigned.size) { i ->
                                JobRow(unassigned[i]) { nav.navigate("job/${unassigned[i].id}") }
                            }
                        }
                        SchedMode.Booking -> Unit
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                        if (!showEntryForm) {
                            OutlinedButton(
                                onClick = { showEntryForm = true },
                                modifier = Modifier.padding(horizontal = 16.dp),
                            ) { Text("Add schedule entry") }
                        } else {
                            EntryForm(
                                viewModel = viewModel,
                                employees = d.employees,
                                presetDate = selectedDay ?: anchor.toString(),
                                onDone = { showEntryForm = false; reloadKey++ },
                                onCancel = { showEntryForm = false },
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun MonthGrid(data: ScheduleResponse, start: String, onDay: (String) -> Unit) {
    val startDate = LocalDate.parse(start)
    val days = (0 until 42).map { startDate.plusDays(it.toLong()) }
    Column(Modifier.padding(horizontal = 8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { dow ->
                Text(
                    dow,
                    color = SentinelMuted,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
                )
            }
        }
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    val dayStr = day.toString()
                    val dayJobs = data.jobs.filter { it.workDate == dayStr }
                    val dayEntries = data.entries.count { it.workDate == dayStr }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SentinelSurface),
                        border = BorderStroke(1.dp, SentinelOutline),
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp)
                            .clickable { onDay(dayStr) },
                    ) {
                        Column(Modifier.padding(4.dp)) {
                            Text(
                                day.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelLarge,
                            )
                            dayJobs.take(2).forEach { job ->
                                Text(
                                    job.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                )
                            }
                            if (dayJobs.size > 2) {
                                Text("+${dayJobs.size - 2} more", style = MaterialTheme.typography.labelSmall)
                            }
                            if (dayEntries > 0) {
                                Text("$dayEntries entr(y/ies)", style = MaterialTheme.typography.labelSmall, color = SentinelMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JobRow(job: Job, onClick: () -> Unit) {
    ItemRow(
        title = job.title,
        subtitle = listOfNotNull(
            job.workDate.takeIf { it.isNotBlank() },
            if (job.allDay) "All day" else job.startTime.takeIf { it.isNotBlank() }?.let { "at $it" },
            job.clientName?.takeIf { it.isNotBlank() },
            job.status.takeIf { it.isNotBlank() },
            job.location.takeIf { it.isNotBlank() },
        ).joinToString(" · "),
        onClick = onClick,
    )
}

@Composable
private fun EntryRow(entry: ScheduleEntry, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            ItemRow(
                title = listOfNotNull(
                    entry.userName.takeIf { it.isNotBlank() },
                    entry.entryType.takeIf { it.isNotBlank() },
                ).joinToString(" · ").ifBlank { "Schedule entry" },
                subtitle = listOfNotNull(
                    entry.workDate.takeIf { it.isNotBlank() },
                    entry.startTime.takeIf { it.isNotBlank() },
                    entry.endTime.takeIf { it.isNotBlank() }?.let { "until $it" },
                    entry.notes.takeIf { it.isNotBlank() },
                ).joinToString(" · "),
            )
        }
        TextButton(onClick = onDelete) { Text("Delete") }
    }
}

@Composable
private fun EntryForm(
    viewModel: AppViewModel,
    employees: List<User>,
    presetDate: String,
    onDone: () -> Unit,
    onCancel: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var userId by remember { mutableStateOf(employees.firstOrNull()?.id ?: 0) }
    var entryType by remember { mutableStateOf("") }
    var workDate by remember { mutableStateOf(presetDate) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var saveError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val meId = viewModel.me?.user?.id
    LaunchedEffect(meId) {
        if (userId == 0 && meId != null) userId = meId
    }

    Column(Modifier.padding(horizontal = 16.dp)) {
        Text("New schedule entry", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (employees.isNotEmpty()) {
            val options = employees.map { it.displayName }
            ChoiceMenu(
                "Person",
                options[employees.indexOfFirst { it.id == userId }.let { if (it < 0) 0 else it }],
                options,
            ) { picked ->
                userId = employees.firstOrNull { it.displayName == picked }?.id ?: userId
            }
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(
            value = entryType,
            onValueChange = { entryType = it },
            label = { Text("Entry type") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        DateFieldRow("Date", workDate) { workDate = it }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = startTime,
                onValueChange = { startTime = it },
                label = { Text("Start time (HH:MM)") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = endTime,
                onValueChange = { endTime = it },
                label = { Text("End time (HH:MM)") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
        if (saveError != null) {
            Text(saveError!!, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !busy && userId != 0 && workDate.isNotBlank(),
                onClick = {
                    busy = true
                    scope.launch {
                        try {
                            viewModel.api.saveScheduleEntry(
                                ScheduleEntryRequest(
                                    userId = userId,
                                    entryType = entryType.trim(),
                                    workDate = workDate.trim(),
                                    startTime = startTime.trim(),
                                    endTime = endTime.trim(),
                                    notes = notes,
                                )
                            )
                            onDone()
                        } catch (e: Exception) {
                            busy = false
                            saveError = plainError(e, "The entry could not be saved. Try again.")
                        }
                    }
                },
            ) { Text(if (busy) "Saving…" else "Save entry") }
            OutlinedButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}

@Composable
fun JobProfileScreen(viewModel: AppViewModel, nav: NavController, jobId: Int) {
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(jobId, reloadKey) {
        error = null
        try {
            job = viewModel.api.job(jobId)
        } catch (e: Exception) {
            error = plainError(e, "This job could not be loaded. Go back and try again.")
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete job") },
            text = { Text("Delete this job permanently? This cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    confirmDelete = false
                    scope.launch {
                        try {
                            viewModel.api.deleteJob(jobId)
                            nav.popBackStack()
                        } catch (e: Exception) {
                            actionError = plainError(e, "The job could not be deleted. Try again.")
                        }
                    }
                }) { Text("Delete job") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        TextButton(onClick = { nav.popBackStack() }) { Text("‹ Back to schedule") }
        when {
            error != null -> ErrorBox(error!!) { reloadKey++ }
            job == null -> SkeletonList(3)
            else -> {
                val j = job!!
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(j.title, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    if (actionError != null) {
                        Text(actionError!!, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ChoiceMenu("Status", j.status, MirrorVocab.JOB_STATUSES) { picked ->
                            scope.launch {
                                try {
                                    viewModel.api.setJobStatus(j.id, picked)
                                    reloadKey++
                                } catch (e: Exception) {
                                    actionError = plainError(e, "That change could not be saved. Try again.")
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { nav.navigate("jobEdit/${j.id}") }) { Text("Edit job") }
                        OutlinedButton(onClick = { confirmDelete = true }) { Text("Delete job") }
                    }
                }
                MirrorCard("Job details") {
                    DetailLine(
                        "When",
                        listOfNotNull(
                            j.workDate.takeIf { it.isNotBlank() },
                            if (j.allDay) "All day" else j.startTime.takeIf { it.isNotBlank() },
                            if (j.allDay) null
                            else j.endTime.takeIf { it.isNotBlank() }?.let { "until $it" },
                        ).joinToString(" "),
                    )
                    DetailLine("Status", j.status)
                    DetailLine("Location", j.location)
                    DetailLine(
                        "Assigned to",
                        if (j.assignees.isEmpty()) "Unassigned"
                        else j.assignees.joinToString(", ") { it.displayName },
                    )
                    DetailLine("Notes", j.notes)
                }
                MirrorCard("Client") {
                    if (j.clientId == null) {
                        Text("No client linked to this job.", color = SentinelMuted)
                    } else {
                        DetailLine("Client", j.clientName)
                        DetailLine("Contact", j.clientContactName)
                        DetailLine("Email", j.clientEmail)
                        DetailLine("Phone", j.clientPhone)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { nav.navigate("client/${j.clientId}") }) {
                            Text("Open client profile")
                        }
                    }
                }
                MirrorCard("Case") {
                    if (j.caseId == null) {
                        Text("No case linked to this job.", color = SentinelMuted)
                    } else {
                        DetailLine("Case", "#${j.caseId}")
                        DetailLine("Title", j.caseTitle)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { nav.navigate("case/${j.caseId}") }) {
                            Text("Open case profile")
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun JobFormScreen(viewModel: AppViewModel, nav: NavController, jobId: Int?, presetDate: String?) {
    val scope = rememberCoroutineScope()
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var crew by remember { mutableStateOf<List<User>>(emptyList()) }
    var cases by remember { mutableStateOf<List<CaseItem>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var title by remember { mutableStateOf("") }
    var clientId by remember { mutableStateOf<Int?>(null) }
    var caseId by remember { mutableStateOf<Int?>(null) }
    var location by remember { mutableStateOf("") }
    var workDate by remember { mutableStateOf(presetDate ?: LocalDate.now().toString()) }
    var allDay by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Scheduled") }
    var assigneeIds by remember { mutableStateOf<List<Int>>(emptyList()) }
    var notes by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(jobId) {
        try {
            clients = viewModel.api.clients().items
            crew = try {
                viewModel.api.roster()
            } catch (e: Exception) {
                emptyList()
            }
            if (jobId != null) {
                val j = viewModel.api.job(jobId)
                title = j.title
                clientId = j.clientId
                caseId = j.caseId
                location = j.location
                workDate = j.workDate
                allDay = j.allDay
                startTime = j.startTime
                endTime = j.endTime
                status = j.status.ifBlank { "Scheduled" }
                assigneeIds = j.assigneeIds
                notes = j.notes
                if (j.clientId != null) {
                    cases = try {
                        viewModel.api.clientDetail(j.clientId).cases
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }
            loaded = true
        } catch (e: Exception) {
            loadError = plainError(e, "The form could not be loaded. Go back and try again.")
        }
    }

    // Changing Client refreshes the Case options (web behavior).
    LaunchedEffect(clientId, loaded) {
        if (!loaded) return@LaunchedEffect
        val cid = clientId
        cases = if (cid == null) {
            emptyList()
        } else {
            try {
                viewModel.api.clientDetail(cid).cases
            } catch (e: Exception) {
                emptyList()
            }
        }
        if (caseId != null && cases.none { it.id == caseId }) caseId = null
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        MirrorHeader(if (jobId == null) "New job" else "Edit job")
        when {
            loadError != null -> ErrorBox(loadError!!)
            !loaded -> SkeletonList(4)
            else -> Column(Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; titleError = null },
                    label = { Text("Job title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (titleError != null) {
                    Text(titleError!!, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(12.dp))
                val clientOptions = listOf("No client") + clients.map { it.legalName }
                ChoiceMenu(
                    "Client",
                    clientOptions[
                        if (clientId == null) 0
                        else clients.indexOfFirst { it.id == clientId }.let { if (it < 0) 0 else it + 1 }
                    ],
                    clientOptions,
                ) { picked ->
                    clientId = if (picked == "No client") null
                    else clients.firstOrNull { it.legalName == picked }?.id
                }
                Spacer(Modifier.height(12.dp))
                val caseOptions = listOf("No case") + cases.map { "#${it.id} · ${it.title}" }
                ChoiceMenu(
                    "Case",
                    caseOptions[
                        if (caseId == null) 0
                        else cases.indexOfFirst { it.id == caseId }.let { if (it < 0) 0 else it + 1 }
                    ],
                    caseOptions,
                ) { picked ->
                    caseId = if (picked == "No case") null
                    else cases.firstOrNull { "#${it.id} · ${it.title}" == picked }?.id
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                DateFieldRow("Date", workDate) { workDate = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = allDay, onCheckedChange = { allDay = it })
                    Text("All-day job")
                }
                if (!allDay) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("Start time (HH:MM)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("End time (HH:MM)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
                ChoiceMenu("Status", status, MirrorVocab.JOB_STATUSES) { status = it }
                Spacer(Modifier.height(12.dp))
                Text("Assigned to", color = SentinelMuted, style = MaterialTheme.typography.labelLarge)
                crew.forEach { person ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = person.id in assigneeIds,
                            onCheckedChange = { checked ->
                                assigneeIds = if (checked) assigneeIds + person.id
                                else assigneeIds - person.id
                            },
                        )
                        Text(
                            listOfNotNull(
                                person.displayName,
                                person.role.takeIf { it.isNotBlank() },
                                person.jobTitle?.takeIf { it.isNotBlank() },
                            ).joinToString(" · "),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                if (saveError != null) {
                    Text(saveError!!, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = !busy,
                        onClick = {
                            if (title.isBlank()) {
                                titleError = "A job title is required."
                            } else {
                                busy = true
                                scope.launch {
                                    try {
                                        val edit = JobFullEdit(
                                            title = title.trim(),
                                            clientId = clientId,
                                            caseId = caseId,
                                            location = location,
                                            workDate = workDate.trim(),
                                            startTime = if (allDay) "" else startTime.trim(),
                                            endTime = if (allDay) "" else endTime.trim(),
                                            allDay = allDay,
                                            status = status,
                                            notes = notes,
                                            assigneeIds = assigneeIds,
                                        )
                                        if (jobId == null) {
                                            val created = viewModel.api.createJob(edit)
                                            nav.popBackStack()
                                            nav.navigate("job/${created.id}")
                                        } else {
                                            viewModel.api.patchJob(jobId, edit)
                                            nav.popBackStack()
                                        }
                                    } catch (e: Exception) {
                                        busy = false
                                        saveError = plainError(e, "The job could not be saved. Try again.")
                                    }
                                }
                            }
                        },
                    ) { Text(if (busy) "Saving…" else "Save job") }
                    OutlinedButton(onClick = { nav.popBackStack() }) { Text("Cancel") }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun DetailLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Spacer(Modifier.height(10.dp))
    Text(label, color = SentinelMuted, style = MaterialTheme.typography.labelLarge)
    Text(value, style = MaterialTheme.typography.bodyLarge)
}

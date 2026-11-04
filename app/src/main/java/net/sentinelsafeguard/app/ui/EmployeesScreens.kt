package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.data.api.createEmployee
import net.sentinelsafeguard.app.data.api.createEmployeeInvite
import net.sentinelsafeguard.app.data.api.deleteEmployee
import net.sentinelsafeguard.app.data.api.editEmployee
import net.sentinelsafeguard.app.data.api.employeeDetail
import net.sentinelsafeguard.app.data.api.employees
import net.sentinelsafeguard.app.data.api.employeesWorkload
import net.sentinelsafeguard.app.data.api.revokeAllEmployeeSessions
import net.sentinelsafeguard.app.data.api.revokeEmployeeInvite
import net.sentinelsafeguard.app.data.api.revokeEmployeeSession
import net.sentinelsafeguard.app.data.api.setEmployeeActive
import net.sentinelsafeguard.app.data.api.setEmployeeClient
import net.sentinelsafeguard.app.data.api.setEmployeeModNote
import net.sentinelsafeguard.app.data.api.setEmployeeMustReset
import net.sentinelsafeguard.app.data.api.setEmployeeRole
import net.sentinelsafeguard.app.data.api.setEmployeeSuspend
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * Employees (Phase 5): web's Directory | Workload tabs, profile
 * (Contact / Account / Moderation / sessions where the payload
 * carries them), create/edit, and the Admin → Members writes the
 * web profile links to. The directory is employees:read-gated
 * exactly as web: a STAFF caller gets the server's 403 in the
 * server's own words — never a fake empty state.
 */

private enum class EmpMode { Directory, Workload }

@Composable
fun EmployeesScreen(viewModel: AppViewModel, nav: NavController) {
    var mode by remember { mutableStateOf(EmpMode.Directory) }
    val isOwner = viewModel.me?.user?.role == "OWNER"
    Column {
        MirrorHeader("Employees", MirrorCopy.subtitle("employees")) {
            if (isOwner) {
                Button(onClick = { nav.navigate("employeeNew") }) { Text("+ New employee") }
            }
        }
        Row(Modifier.padding(horizontal = 16.dp)) {
            EmpMode.values().forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = { mode = m },
                    label = { Text(m.name) },
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        }
        when (mode) {
            EmpMode.Directory -> EmployeesDirectory(viewModel, nav)
            EmpMode.Workload -> EmployeesWorkload(viewModel, nav)
        }
    }
}

@Composable
private fun EmployeesDirectory(viewModel: AppViewModel, nav: NavController) {
    var query by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("All") }
    var reloadKey by remember { mutableStateOf(0) }
    var data by remember { mutableStateOf<JsonObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(query, role, reloadKey) {
        try {
            data = viewModel.api.employees(q = query, role = role)
            error = null
        } catch (e: Exception) {
            error = plainError(e, "Couldn't load the team directory. Try again.")
        }
    }
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search name, title, email or phone") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Row(Modifier.padding(horizontal = 16.dp)) {
            Phase5Logic.EMPLOYEE_ROLE_FILTERS.forEach { (label, param) ->
                FilterChip(
                    selected = role == param,
                    onClick = { role = param },
                    label = { Text(label) },
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        }
        when {
            error != null -> ErrorBox(error!!) { reloadKey++ }
            data == null -> SkeletonList()
            else -> {
                val stats = Phase3Logic.obj(data, "stats")
                if (stats != null) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        StatCard("Team members", "${Phase3Logic.int(stats, "team") ?: 0}", Modifier.weight(1f))
                        StatCard("Active", "${Phase3Logic.int(stats, "active") ?: 0}", Modifier.weight(1f))
                        StatCard("Owners", "${Phase3Logic.int(stats, "owners") ?: 0}", Modifier.weight(1f))
                        StatCard("Staff", "${Phase3Logic.int(stats, "staff") ?: 0}", Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        // Pending invites is OWNER-only in the payload:
                        // the key is omitted for other callers.
                        if (stats.containsKey("pendingInvites")) {
                            StatCard(
                                "Pending invites",
                                "${Phase3Logic.int(stats, "pendingInvites") ?: 0}",
                                Modifier.weight(1f),
                            )
                        }
                        StatCard(
                            "Unassigned open",
                            "${Phase3Logic.int(stats, "unassigned") ?: 0}",
                            Modifier.weight(1f),
                        )
                    }
                }
                val rows = Phase3Logic.arr(data, "employees")
                if (rows.isEmpty()) {
                    EmptyBox("No matches", "No team members match the current search and filters.")
                    TextButton(
                        onClick = { query = ""; role = "All" },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) { Text("Clear search and filters") }
                }
                LazyColumn {
                    listOf("Team", "Client access", "Demo access").forEach { section ->
                        val sectionRows = rows.filter { Phase5Logic.employeeSection(it) == section }
                        if (sectionRows.isNotEmpty()) {
                            item { SectionHeader(section, sectionRows.size) }
                            items(sectionRows) { user ->
                                val counts = Phase3Logic.obj(user, "openCounts")
                                ItemRow(
                                    title = Phase3Logic.str(user, "displayName"),
                                    subtitle = listOfNotNull(
                                        Phase3Logic.str(user, "jobTitle").takeIf { it.isNotBlank() },
                                        Phase3Logic.str(user, "role"),
                                        Phase3Logic.str(user, "email").takeIf { it.isNotBlank() },
                                        counts?.let { "Open ${Phase3Logic.int(it, "total") ?: 0}" },
                                        if (Phase3Logic.bool(user, "active") != true) "Inactive" else null,
                                    ).joinToString(" · "),
                                    onClick = {
                                        Phase3Logic.int(user, "id")?.let { nav.navigate("employee/$it") }
                                    },
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun EmployeesWorkload(viewModel: AppViewModel, nav: NavController) {
    val state = rememberJsonScreen { viewModel.api.employeesWorkload() }
    when {
        state.error != null && state.data == null -> ErrorBox(state.error!!) { state.reloadKey++ }
        state.data == null -> SkeletonList()
        else -> {
            val data = state.data!!
            LazyColumn {
                val unassigned = Phase3Logic.obj(data, "unassigned")
                if (unassigned != null) {
                    item {
                        MirrorCard("Unassigned", Phase3Logic.int(unassigned, "total")) {
                            Text(
                                "Open cases ${Phase3Logic.arr(unassigned, "cases").size} · " +
                                    "Open tickets ${Phase3Logic.arr(unassigned, "tickets").size}",
                                color = SentinelMuted,
                            )
                        }
                    }
                }
                items(Phase3Logic.arr(data, "members")) { member ->
                    val user = Phase3Logic.obj(member, "user") ?: member
                    val workload = Phase3Logic.obj(member, "workload")
                    ItemRow(
                        title = Phase3Logic.str(user, "displayName"),
                        subtitle = listOfNotNull(
                            Phase3Logic.str(user, "role"),
                            workload?.let {
                                "Cases ${Phase3Logic.obj(it, "cases")?.let { c -> Phase3Logic.int(c, "openCount") } ?: 0} · " +
                                    "Tickets ${Phase3Logic.obj(it, "tickets")?.let { t -> Phase3Logic.int(t, "openCount") } ?: 0} · " +
                                    "Assets ${Phase3Logic.obj(it, "assets")?.let { a -> Phase3Logic.int(a, "openCount") } ?: 0} · " +
                                    "Open total ${Phase3Logic.int(it, "openTotal") ?: 0}"
                            },
                        ).joinToString(" · "),
                        onClick = {
                            Phase3Logic.int(user, "id")?.let { nav.navigate("employee/$it") }
                        },
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun EmployeeProfileScreen(viewModel: AppViewModel, nav: NavController, employeeId: Int) {
    val state = rememberJsonScreen { viewModel.api.employeeDetail(employeeId) }
    val scope = rememberCoroutineScope()
    var actionError by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var modNote by remember { mutableStateOf<String?>(null) }
    var inviteRole by remember { mutableStateOf("STAFF") }
    var inviteCode by remember { mutableStateOf<String?>(null) }
    val run: (suspend () -> Unit) -> Unit = { block ->
        scope.launch {
            try {
                block()
                state.reloadKey++
            } catch (e: Exception) {
                actionError = plainError(e, "That could not be done. Try again.")
            }
        }
    }
    Column {
        MirrorHeader("Employees") {
            OutlinedButton(onClick = { nav.popBackStack() }) { Text("← Back") }
        }
        when {
            state.error != null && state.data == null -> ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null -> SkeletonList()
            else -> {
                val data = state.data!!
                val user = Phase3Logic.obj(data, "user") ?: data
                val uid = Phase3Logic.int(user, "id") ?: employeeId
                val isActive = Phase3Logic.bool(user, "active") == true
                LazyColumn {
                    item {
                        MirrorCard(Phase3Logic.str(user, "displayName")) {
                            JsonLines(
                                user,
                                "role" to "Role",
                                "jobTitle" to "Job title",
                                "email" to "Email",
                                "phone" to "Phone",
                            )
                            DetailLine("Active", if (isActive) "Yes" else "No")
                            val client = Phase3Logic.obj(data, "client")
                            if (client != null) {
                                DetailLine("Client", Phase3Logic.str(client, "legalName"))
                            }
                            Spacer(Modifier.height(8.dp))
                            Row {
                                Button(
                                    onClick = { nav.navigate("employeeEdit/$uid") },
                                    modifier = Modifier.padding(end = 8.dp),
                                ) { Text("Edit") }
                                OutlinedButton(
                                    onClick = { run { viewModel.api.setEmployeeActive(uid, !isActive) } },
                                ) { Text(if (isActive) "Deactivate" else "Reactivate") }
                            }
                            ActionErrorText(actionError)
                        }
                    }
                    val workload = Phase3Logic.obj(data, "workload")
                    if (workload != null) {
                        item {
                            MirrorCard("Workload", Phase3Logic.int(workload, "openTotal")) {
                                listOf("cases", "tickets", "assets").forEach { kind ->
                                    val bucket = Phase3Logic.obj(workload, kind)
                                    if (bucket != null) {
                                        DetailLine(
                                            kind.replaceFirstChar { it.uppercase() },
                                            "Open ${Phase3Logic.int(bucket, "openCount") ?: 0} · " +
                                                "Closed ${Phase3Logic.int(bucket, "closedCount") ?: 0}",
                                        )
                                        Phase3Logic.arr(bucket, "open").forEach { row ->
                                            Text(
                                                "• ${Phase3Logic.str(row, "title").ifBlank { Phase3Logic.str(row, "name") }}",
                                                color = SentinelMuted,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    val schedule = Phase3Logic.arr(data, "schedule")
                    if (schedule.isNotEmpty()) {
                        item {
                            MirrorCard("Upcoming schedule", schedule.size) {
                                schedule.forEach { job ->
                                    DetailLine(
                                        Phase3Logic.str(job, "title"),
                                        listOfNotNull(
                                            Phase3Logic.strOrNull(job, "workDate"),
                                            Phase3Logic.strOrNull(job, "startTime"),
                                            Phase3Logic.strOrNull(job, "status"),
                                        ).joinToString(" · "),
                                    )
                                }
                            }
                        }
                    }
                    // Sessions ride only for OWNER/self/admin viewers —
                    // the card renders exactly when the payload has them.
                    if (data.containsKey("sessions")) {
                        item {
                            val sessions = Phase3Logic.arr(data, "sessions")
                            MirrorCard("Sessions", sessions.size) {
                                if (sessions.isEmpty()) {
                                    Text("No active sessions.", color = SentinelMuted)
                                }
                                sessions.forEach { session ->
                                    Row(Modifier.fillMaxWidth()) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                listOfNotNull(
                                                    Phase3Logic.strOrNull(session, "ip"),
                                                    Phase3Logic.strOrNull(session, "device"),
                                                ).joinToString(" · ").ifBlank { "Session" },
                                            )
                                            Text(
                                                Phase3Logic.displayValue("createdAt", session["createdAt"]) ?: "",
                                                color = SentinelMuted,
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                        val sid = Phase3Logic.strOrNull(session, "id")
                                            ?: Phase3Logic.int(session, "id")?.toString()
                                        if (sid != null) {
                                            TextButton(onClick = {
                                                run { viewModel.api.revokeEmployeeSession(uid, sid) }
                                            }) { Text("Revoke") }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                OutlinedButton(onClick = {
                                    run { viewModel.api.revokeAllEmployeeSessions(uid) }
                                }) { Text("Revoke all sessions") }
                            }
                        }
                    }
                    val moderation = Phase3Logic.obj(data, "moderation")
                    if (moderation != null) {
                        item {
                            LaunchedEffect(moderation) {
                                if (modNote == null) {
                                    modNote = Phase3Logic.str(moderation, "modNote")
                                }
                            }
                            MirrorCard("Moderation") {
                                TextFieldRow("Moderator note — private, admins only", modNote ?: "", onChange = { modNote = it })
                                Button(onClick = {
                                    run { viewModel.api.setEmployeeModNote(uid, modNote ?: "") }
                                }) { Text("Save note") }
                                Spacer(Modifier.height(8.dp))
                                DetailLine(
                                    "Suspended until",
                                    Phase3Logic.displayValue("suspendUntil", moderation["suspendUntil"]),
                                )
                                Row {
                                    OutlinedButton(
                                        onClick = {
                                            run {
                                                viewModel.api.setEmployeeSuspend(
                                                    uid,
                                                    System.currentTimeMillis() + 7L * 24 * 3600 * 1000,
                                                )
                                            }
                                        },
                                        modifier = Modifier.padding(end = 8.dp),
                                    ) { Text("Suspend 7 days") }
                                    OutlinedButton(onClick = {
                                        run { viewModel.api.setEmployeeSuspend(uid, 0) }
                                    }) { Text("Lift suspension") }
                                }
                                Spacer(Modifier.height(8.dp))
                                SwitchRow(
                                    "Must reset passphrase",
                                    Phase3Logic.bool(moderation, "mustReset") == true,
                                ) { required -> run { viewModel.api.setEmployeeMustReset(uid, required) } }
                                DetailLine(
                                    "Two-factor enrolled",
                                    if (Phase3Logic.bool(moderation, "totpEnrolled") == true) "Yes" else "No",
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("Role", color = SentinelMuted, style = MaterialTheme.typography.labelLarge)
                                ChoiceMenu(
                                    "Role",
                                    Phase3Logic.str(user, "role"),
                                    listOf("OWNER", "STAFF", "Admin", "Client", "Family", "Tourist"),
                                ) { picked -> run { viewModel.api.setEmployeeRole(uid, picked) } }
                                Spacer(Modifier.height(8.dp))
                                Row {
                                    OutlinedButton(
                                        onClick = { run { viewModel.api.setEmployeeClient(uid, null) } },
                                        modifier = Modifier.padding(end = 8.dp),
                                    ) { Text("Clear client link") }
                                    TextButton(onClick = { nav.navigate("admin") }) {
                                        Text("Open in Admin → Members")
                                    }
                                }
                            }
                        }
                    }
                    val invites = Phase3Logic.arr(data, "invites")
                    item {
                        MirrorCard("Invites", invites.size) {
                            invites.forEach { invite ->
                                Row(Modifier.fillMaxWidth()) {
                                    Text(
                                        "${Phase3Logic.str(invite, "role")} · " +
                                            (Phase3Logic.displayValue("createdAt", invite["createdAt"]) ?: ""),
                                        modifier = Modifier.weight(1f),
                                    )
                                    Phase3Logic.int(invite, "id")?.let { inviteId ->
                                        TextButton(onClick = {
                                            run { viewModel.api.revokeEmployeeInvite(inviteId) }
                                        }) { Text("Revoke") }
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            ChoiceMenu("Invite role", inviteRole, listOf("STAFF", "Admin", "Client")) {
                                inviteRole = it
                            }
                            Button(onClick = {
                                scope.launch {
                                    try {
                                        val out = viewModel.api.createEmployeeInvite(inviteRole)
                                        inviteCode = Phase3Logic.str(out, "code")
                                        state.reloadKey++
                                    } catch (e: Exception) {
                                        actionError = plainError(e, "The invite could not be created.")
                                    }
                                }
                            }) { Text("+ Create invite") }
                            if (inviteCode != null) {
                                Spacer(Modifier.height(6.dp))
                                Text("Invite code (shown once): $inviteCode")
                            }
                        }
                    }
                    val activity = Phase3Logic.arr(data, "activity")
                    if (activity.isNotEmpty()) {
                        item {
                            MirrorCard("Activity", activity.size) {
                                activity.forEach { row ->
                                    Text(
                                        "${Phase3Logic.str(row, "action")} — ${Phase3Logic.str(row, "detail")}",
                                    )
                                    Text(
                                        Phase3Logic.displayValue("createdAt", row["createdAt"]) ?: "",
                                        color = SentinelMuted,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Column(Modifier.padding(16.dp)) {
                            OutlinedButton(onClick = { confirmDelete = true }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        ConfirmGate(
            title = "Delete employee",
            body = "This permanently deletes this account. This cannot be undone.",
            confirmLabel = "Delete",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                run {
                    viewModel.api.deleteEmployee(employeeId)
                    nav.popBackStack()
                }
            },
        )
    }
}

@Composable
fun EmployeeFormScreen(viewModel: AppViewModel, nav: NavController, employeeId: Int?) {
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(employeeId == null) }
    var displayName by remember { mutableStateOf("") }
    var jobTitle by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(employeeId) {
        if (employeeId != null) {
            try {
                val data = viewModel.api.employeeDetail(employeeId)
                val user = Phase3Logic.obj(data, "user") ?: data
                displayName = Phase3Logic.str(user, "displayName")
                jobTitle = Phase3Logic.str(user, "jobTitle")
                email = Phase3Logic.str(user, "email")
                phone = Phase3Logic.str(user, "phone")
            } catch (e: Exception) {
                error = plainError(e, "Couldn't load this employee. Try again.")
            } finally {
                loaded = true
            }
        }
    }
    LazyColumn {
        item { MirrorHeader(if (employeeId == null) "New employee" else "Edit employee") }
        if (!loaded) {
            item { SkeletonList(3) }
        } else {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    TextFieldRow("Display name", displayName, onChange = { displayName = it })
                    TextFieldRow("Job title", jobTitle, onChange = { jobTitle = it })
                    TextFieldRow("Email", email, onChange = { email = it })
                    TextFieldRow("Phone", phone, onChange = { phone = it })
                    if (employeeId == null) {
                        TextFieldRow("Passphrase", passphrase, onChange = { passphrase = it })
                    }
                    ActionErrorText(error)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = {
                        if (displayName.isBlank()) {
                            error = "Display name is required."
                            return@Button
                        }
                        scope.launch {
                            try {
                                val body = buildJsonObject {
                                    put("displayName", displayName)
                                    put("jobTitle", jobTitle)
                                    put("email", email)
                                    put("phone", phone)
                                    if (employeeId == null) put("passphrase", passphrase)
                                }
                                if (employeeId == null) viewModel.api.createEmployee(body)
                                else viewModel.api.editEmployee(employeeId, body)
                                nav.popBackStack()
                            } catch (e: Exception) {
                                error = plainError(e, "That could not be saved. Try again.")
                            }
                        }
                    }) { Text("Save") }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

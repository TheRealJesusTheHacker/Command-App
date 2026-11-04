package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.data.api.adminAuditExport
import net.sentinelsafeguard.app.data.api.adminAuditLog
import net.sentinelsafeguard.app.data.api.adminBackupDiscard
import net.sentinelsafeguard.app.data.api.adminBackupRequest
import net.sentinelsafeguard.app.data.api.adminBackupStatus
import net.sentinelsafeguard.app.data.api.adminData
import net.sentinelsafeguard.app.data.api.adminDmarc
import net.sentinelsafeguard.app.data.api.adminHealthStrip
import net.sentinelsafeguard.app.data.api.adminLawAi
import net.sentinelsafeguard.app.data.api.adminLawAiKeyInstall
import net.sentinelsafeguard.app.data.api.adminLawAiKeyStatus
import net.sentinelsafeguard.app.data.api.adminLawAiSave
import net.sentinelsafeguard.app.data.api.adminLawAiTest
import net.sentinelsafeguard.app.data.api.adminLoginSecurity
import net.sentinelsafeguard.app.data.api.adminMemberCreate
import net.sentinelsafeguard.app.data.api.adminMemberPassphrase
import net.sentinelsafeguard.app.data.api.adminOpsReport
import net.sentinelsafeguard.app.data.api.adminRecoveryCodes
import net.sentinelsafeguard.app.data.api.adminRetention
import net.sentinelsafeguard.app.data.api.adminRetentionReport
import net.sentinelsafeguard.app.data.api.adminRetentionSet
import net.sentinelsafeguard.app.data.api.adminRevokeAllSessions
import net.sentinelsafeguard.app.data.api.adminRoleCreate
import net.sentinelsafeguard.app.data.api.adminRoleDelete
import net.sentinelsafeguard.app.data.api.adminRolePatch
import net.sentinelsafeguard.app.data.api.adminRoleTabs
import net.sentinelsafeguard.app.data.api.adminRoles
import net.sentinelsafeguard.app.data.api.adminSecuritySave
import net.sentinelsafeguard.app.data.api.adminSettingsSave
import net.sentinelsafeguard.app.data.api.exportFull
import net.sentinelsafeguard.app.data.api.exportWorkspace
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * The Admin console (Phase 8B) — the mirror of web's Admin view:
 * its own grouped structure (Overview · People · Security ·
 * Law AI · Backups & Data · Danger Zone), 13 sections + the
 * b21-injected DMARC reports section, exact names and order.
 *
 * Reuse, exactly as the backend rides shared handlers
 * (Addendum 26): Members moderation, Invites creation/revoke,
 * and per-session revoke live on the Phase 5 Employees and
 * Phase 8A Activity surfaces — those sections link there rather
 * than duplicating the UI; the Danger Zone exports ride the
 * Phase 3 export endpoints; Admin Audit is the Phase 8A
 * Activity console plus the Oversight audit explorer below.
 *
 * SECRECY: Law AI renders the handler's masked public state
 * only. Recovery codes render ONCE from the regeneration
 * response, in-session only, behind web's current-passphrase
 * gate. The owner-gated passphrase value and the Law AI key are
 * write-only inputs — sent once, cleared, never displayed back.
 */
@Composable
fun AdminSectionScreen(viewModel: AppViewModel, nav: NavController, sectionKey: String) {
    val label = MirrorNav.ALL_VIEWS[sectionKey]?.label ?: "Admin"
    Column {
        MirrorHeader(label)
        LazyColumn {
            item {
                when (sectionKey) {
                    "overview" -> AdminOverview(viewModel)
                    "report" -> AdminOpsReport(viewModel)
                    "roles" -> AdminRoles(viewModel)
                    "members" -> AdminMembers(viewModel, nav)
                    "invites" -> AdminInvites(viewModel, nav)
                    "sessions" -> AdminSessions(viewModel, nav)
                    "security" -> AdminSecurity(viewModel)
                    "oversight" -> AdminOversight(viewModel)
                    "audit" -> AdminAudit(viewModel, nav)
                    "lawai" -> AdminLawAi(viewModel)
                    "backups" -> AdminBackups(viewModel)
                    "dmarc" -> AdminDmarc(viewModel)
                    "danger" -> AdminDanger(viewModel)
                    else -> AdminOverview(viewModel)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AdminOverview(viewModel: AppViewModel) {
    AsyncCard("Overview", loader = { viewModel.api.adminData() }) { data, _ ->
        MessageLine(data)
        val users = Phase3Logic.arr(data, "users")
        val byRole = users.groupBy { Phase3Logic.str(it, "role") }
        Text("Users by role", style = MaterialTheme.typography.titleSmall)
        byRole.entries.sortedBy { it.key }.forEach { (role, rows) ->
            DetailLine(role, rows.size.toString())
        }
        Spacer(Modifier.height(8.dp))
        NestedLines("Settings", Phase3Logic.obj(data, "settings"), "ownerInitialized")
        val overview = Phase3Logic.obj(data, "overview")
        if (overview != null) {
            NestedLines("Workspace", overview)
        }
    }
    AsyncCard("Health", loader = { viewModel.api.adminHealthStrip() }) { data, _ ->
        NestedLines("Services", Phase3Logic.obj(data, "services"))
        val notes = Phase3Logic.arrRaw(data, "notes")
        notes.forEach { note ->
            Text(
                (note as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "",
                color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    AsyncCard("Admin audit", loader = { viewModel.api.adminData() }) { data, _ ->
        AnyRows(data, "activity", listOf("detail"))
    }
}

@Composable
private fun AdminOpsReport(viewModel: AppViewModel) {
    var month by remember { mutableStateOf("") }
    var reloadKey by remember { mutableStateOf(0) }
    var appliedMonth by remember { mutableStateOf<String?>(null) }
    TextFieldRow("Month (YYYY-MM)", month, { month = it })
    OutlinedButton(onClick = {
        appliedMonth = month.ifBlank { null }
        reloadKey++
    }) { Text("Load report") }
    val loaderKey = reloadKey to appliedMonth
    var data by remember { mutableStateOf<JsonObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    androidx.compose.runtime.LaunchedEffect(loaderKey) {
        error = null
        try {
            data = viewModel.api.adminOpsReport(appliedMonth)
        } catch (e: Exception) {
            error = plainError(e, "The report could not be loaded. Try again.")
        }
    }
    when {
        error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
        data == null -> Text("Loading…", color = SentinelMuted)
        else -> {
            val report = data!!
            DetailLine("Month", Phase3Logic.str(report, "month"))
            val sections = Phase3Logic.obj(report, "sections")
            sections?.entries?.forEach { (name, section) ->
                (section as? JsonObject)?.let { NestedLines(name, it) }
            }
            val context = LocalContext.current
            OutlinedButton(onClick = {
                shareText(context, "Ops Report", report.toString())
            }) { Text("Share report") }
        }
    }
}

@Composable
private fun AdminRoles(viewModel: AppViewModel) {
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<String?>(null) }
    AsyncCard("Roles", "roles", loader = { viewModel.api.adminRoles() }) { data, reload ->
        val act = rememberAction({ error = it }, { reload() })
        Phase3Logic.arr(data, "roles").forEach { role ->
            val name = Phase3Logic.str(role, "name")
            Column {
                Text(name, style = MaterialTheme.typography.bodyLarge)
                val meta = listOfNotNull(
                    Phase3Logic.str(role, "summary").ifBlank { null },
                    "Members: ${Phase3Logic.int(role, "memberCount") ?: 0}",
                    if (Phase3Logic.bool(role, "isSystem") == true) "System role" else null,
                ).joinToString(" · ")
                Text(meta, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
                val tabs = Phase3Logic.obj(role, "tabPermissions")
                if (tabs != null && tabs.isNotEmpty()) {
                    Text("Tabs", style = MaterialTheme.typography.titleSmall)
                    AnyLines(tabs)
                }
                if (Phase3Logic.bool(role, "isSystem") != true) {
                    OutlinedButton(onClick = { confirmDelete = name }) { Text("Delete") }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        ActionErrorText(error)
        if (confirmDelete != null) {
            val target = confirmDelete!!
            ConfirmGate(
                "Delete role",
                "Delete the role $target? Members keep their accounts; the role's access goes away.",
                "Delete",
                onDismiss = { confirmDelete = null },
                onConfirm = {
                    confirmDelete = null
                    act { viewModel.api.adminRoleDelete(target) }
                },
            )
        }
    }
    // New role + per-role edits (color / permissions / tabs).
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var editName by remember { mutableStateOf("") }
    var editColor by remember { mutableStateOf("") }
    var tabRole by remember { mutableStateOf("") }
    var tabKey by remember { mutableStateOf("") }
    var tabAccess by remember { mutableStateOf("read") }
    var formError by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun run(block: suspend () -> JsonObject) {
        scope.launch {
            try {
                val r = block()
                notice = Phase3Logic.str(r, "message").ifBlank { "Saved." }
                formError = null
            } catch (e: Exception) {
                formError = plainError(e, "That could not be done. Try again.")
            }
        }
    }
    MirrorCard("New role") {
        TextFieldRow("Role name", name, { name = it })
        TextFieldRow("Color", color, { color = it })
        Button(onClick = {
            run {
                viewModel.api.adminRoleCreate(buildJsonObject {
                    put("name", name)
                    if (color.isNotBlank()) put("color", color)
                })
            }
        }) { Text("Create role") }
    }
    MirrorCard("Edit role") {
        TextFieldRow("Role name", editName, { editName = it })
        TextFieldRow("Color", editColor, { editColor = it })
        Button(onClick = {
            run {
                viewModel.api.adminRolePatch(
                    editName,
                    buildJsonObject { if (editColor.isNotBlank()) put("color", editColor) },
                )
            }
        }) { Text("Save") }
        Spacer(Modifier.height(8.dp))
        TextFieldRow("Role name (tabs)", tabRole, { tabRole = it })
        TextFieldRow("Tab", tabKey, { tabKey = it })
        TextFieldRow("Access (none / read / write)", tabAccess, { tabAccess = it })
        Button(onClick = {
            run {
                viewModel.api.adminRoleTabs(
                    tabRole,
                    buildJsonObject { put(tabKey, tabAccess) },
                )
            }
        }) { Text("Save tabs") }
    }
    if (notice != null) Text(notice!!, modifier = Modifier.padding(horizontal = 16.dp))
    ActionErrorText(formError)
}

@Composable
private fun AdminMembers(viewModel: AppViewModel, nav: NavController) {
    AsyncCard("Members", "users", loader = { viewModel.api.adminData() }) { data, _ ->
        val users = Phase3Logic.arr(data, "users")
        val active = users.filter { Phase3Logic.bool(it, "active") != false }
        val deactivated = users.filter { Phase3Logic.bool(it, "active") == false }
        active.forEach { user ->
            Text(
                Phase3Logic.str(user, "displayName"),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                Phase3Logic.str(user, "role") +
                    (Phase3Logic.str(user, "jobTitle").ifBlank { null }?.let { " · $it" } ?: ""),
                color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
        }
        if (deactivated.isNotEmpty()) {
            Text("Deactivated", style = MaterialTheme.typography.titleSmall)
            deactivated.forEach { user ->
                Text(Phase3Logic.str(user, "displayName"))
            }
        }
        Spacer(Modifier.height(6.dp))
        ReuseLink(
            "Open in Admin → Members",
            "Moderation (role, active, suspend, must-reset, client link, " +
                "revoke sessions) rides the Employees surface, as web's " +
                "Members table opens the same member profile.",
        ) { nav.navigate("employees") }
    }
    // Provisioning (web provisionaccount).
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("STAFF") }
    var accountType by remember { mutableStateOf("command") }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    MirrorCard("New member") {
        TextFieldRow("Display name", displayName, { displayName = it })
        TextFieldRow("Email", email, { email = it })
        TextFieldRow("Role", role, { role = it })
        TextFieldRow("Account type", accountType, { accountType = it })
        Button(onClick = {
            scope.launch {
                try {
                    val r = viewModel.api.adminMemberCreate(buildJsonObject {
                        put("displayName", displayName)
                        put("email", email)
                        put("role", role)
                        put("accountType", accountType)
                    })
                    notice = Phase3Logic.str(r, "message").ifBlank { "Account created." }
                    error = null
                } catch (e: Exception) {
                    error = plainError(e, "That account could not be created. Try again.")
                }
            }
        }) { Text("Create invite") }
        if (notice != null) Text(notice!!)
        ActionErrorText(error)
    }
    // Owner-gated passphrase set (value is write-only).
    var memberId by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    var passError by remember { mutableStateOf<String?>(null) }
    var passNotice by remember { mutableStateOf<String?>(null) }
    MirrorCard("Set passphrase") {
        Text("Owner only — the handler enforces the gate.", color = SentinelMuted)
        TextFieldRow("Member ID", memberId, { memberId = it })
        OutlinedTextField(
            value = passphrase,
            onValueChange = { passphrase = it },
            label = { Text("New passphrase") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )
        Button(onClick = {
            val id = memberId.toIntOrNull()
            if (id == null) {
                passError = "Enter the member's ID."
            } else {
                scope.launch {
                    try {
                        val r = viewModel.api.adminMemberPassphrase(id, passphrase)
                        passphrase = ""
                        passNotice = Phase3Logic.str(r, "message").ifBlank { "Passphrase set." }
                        passError = null
                    } catch (e: Exception) {
                        passphrase = ""
                        passError = plainError(e, "The passphrase could not be set. Try again.")
                    }
                }
            }
        }) { Text("Set passphrase") }
        if (passNotice != null) Text(passNotice!!)
        ActionErrorText(passError)
    }
}

@Composable
private fun AdminInvites(viewModel: AppViewModel, nav: NavController) {
    AsyncCard("Invites", "invites", loader = { viewModel.api.adminData() }) { data, _ ->
        AnyRows(data, "invites", listOf("role", "expiresAt"))
        Spacer(Modifier.height(6.dp))
        ReuseLink(
            "Open in Employees",
            "Creating and revoking invites rides the Employees surface " +
                "(the same invite handlers web's Admin and Employees views share).",
        ) { nav.navigate("employees") }
    }
}

@Composable
private fun AdminSessions(viewModel: AppViewModel, nav: NavController) {
    var error by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    MirrorCard("Sessions") {
        ReuseLink(
            "Open Active sessions",
            "The session list and per-session revoke ride the Activity " +
                "surface (the same getactivesessions read web's Admin uses).",
        ) { nav.navigate("activity") }
        Spacer(Modifier.height(10.dp))
        Button(onClick = { confirm = true }) { Text("Revoke all sessions") }
        if (notice != null) Text(notice!!)
        ActionErrorText(error)
    }
    if (confirm) {
        ConfirmGate(
            "Revoke all sessions",
            "Revoke every signed-in session for every user? Everyone, " +
                "including you, will need to sign in again.",
            "Revoke all sessions",
            onDismiss = { confirm = false },
            onConfirm = {
                confirm = false
                scope.launch {
                    try {
                        val r = viewModel.api.adminRevokeAllSessions()
                        notice = Phase3Logic.str(r, "message").ifBlank { "All sessions revoked." }
                        error = null
                    } catch (e: Exception) {
                        error = plainError(e, "Sessions could not be revoked. Try again.")
                    }
                }
            },
        )
    }
}

@Composable
private fun AdminSecurity(viewModel: AppViewModel) {
    // Settings (workspace name + live settings) — web updatesettings.
    AsyncCard("Settings", loader = { viewModel.api.adminData() }) { data, reload ->
        val settings = Phase3Logic.obj(data, "settings") ?: JsonObject(emptyMap())
        var workspaceName by remember(settings) {
            mutableStateOf(Phase3Logic.str(settings, "workspaceName"))
        }
        var publicSiteUrl by remember(settings) {
            mutableStateOf(Phase3Logic.str(settings, "publicSiteUrl"))
        }
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        AnyLines(settings, "workspaceName", "publicSiteUrl", "ownerInitialized")
        TextFieldRow("Workspace name", workspaceName, { workspaceName = it })
        TextFieldRow("Public site URL", publicSiteUrl, { publicSiteUrl = it })
        Button(onClick = {
            act {
                viewModel.api.adminSettingsSave(buildJsonObject {
                    put("workspaceName", workspaceName)
                    put("publicSiteUrl", publicSiteUrl)
                })
            }
        }) { Text("Save") }
        ActionErrorText(error)
    }
    // Sign-in protection — web updatesecurity.
    AsyncCard("Security", loader = { viewModel.api.adminData() }) { data, reload ->
        val settings = Phase3Logic.obj(data, "settings") ?: JsonObject(emptyMap())
        var lockdown by remember(settings) {
            mutableStateOf(Phase3Logic.bool(settings, "lockdown") ?: false)
        }
        var requireTotp by remember(settings) {
            mutableStateOf(Phase3Logic.bool(settings, "requireTotpAdminWrite") ?: false)
        }
        var ttl by remember(settings) {
            mutableStateOf((Phase3Logic.int(settings, "sessionTtlHours") ?: 8).toString())
        }
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        SwitchRow("Lockdown", lockdown) { lockdown = it }
        SwitchRow("Require two-factor for admin writes", requireTotp) { requireTotp = it }
        TextFieldRow("Session length (hours)", ttl, { ttl = it })
        Button(onClick = {
            act {
                viewModel.api.adminSecuritySave(buildJsonObject {
                    put("lockdown", lockdown)
                    put("requireTotpAdminWrite", requireTotp)
                    ttl.toIntOrNull()?.let { put("sessionTtlHours", it) }
                })
            }
        }) { Text("Save") }
        ActionErrorText(error)
    }
    // Recovery codes — web's current-passphrase gate; codes shown once.
    var currentPass by remember { mutableStateOf("") }
    var codes by remember { mutableStateOf<List<String>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    MirrorCard("Recovery codes") {
        Text(
            "Regenerating replaces every existing code. New codes are " +
                "shown once, here — save them somewhere safe.",
            color = SentinelMuted,
        )
        OutlinedTextField(
            value = currentPass,
            onValueChange = { currentPass = it },
            label = { Text("Current passphrase") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )
        Button(onClick = {
            scope.launch {
                try {
                    val r = viewModel.api.adminRecoveryCodes(currentPass)
                    currentPass = ""
                    codes = Phase8bLogic.recoveryCodeLines(r)
                    error = null
                } catch (e: Exception) {
                    currentPass = ""
                    error = plainError(e, "Recovery codes could not be regenerated. Try again.")
                }
            }
        }) { Text("Regenerate recovery codes") }
        codes.forEach { code ->
            Text(code, style = MaterialTheme.typography.bodyLarge)
        }
        if (codes.isNotEmpty()) {
            OutlinedButton(onClick = { codes = emptyList() }) { Text("Done") }
        }
        ActionErrorText(error)
    }
}

@Composable
private fun AdminOversight(viewModel: AppViewModel) {
    AsyncCard("Retention", "settings", loader = { viewModel.api.adminRetention() }) { data, reload ->
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        MessageLine(data)
        Phase3Logic.str(data, "note").takeIf { it.isNotBlank() }?.let {
            Text(it, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
        }
        Phase3Logic.arr(data, "settings").forEach { row ->
            val entity = Phase3Logic.str(row, "entity")
            var days by remember(row) {
                mutableStateOf((Phase3Logic.int(row, "retentionDays") ?: "").toString())
            }
            Column {
                Text(Phase3Logic.str(row, "label"), style = MaterialTheme.typography.bodyLarge)
                Text(
                    Phase3Logic.str(row, "blurb"),
                    color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
                )
                Row {
                    OutlinedTextField(
                        value = days,
                        onValueChange = { days = it },
                        label = { Text("Retention days") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                    )
                    Button(onClick = {
                        act {
                            viewModel.api.adminRetentionSet(entity, days.toIntOrNull())
                        }
                    }) { Text("Save") }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        OutlinedButton(onClick = { act { viewModel.api.adminRetentionReport() } }) {
            Text("Run retention report")
        }
        Spacer(Modifier.height(6.dp))
        AnyRows(data, "reports", listOf("detail"))
        ActionErrorText(error)
    }
    AsyncCard("Audit log", "rows", loader = { viewModel.api.adminAuditLog() }) { data, _ ->
        AnyRows(data, "rows", listOf("detail", "ip"))
    }
    val context = LocalContext.current
    AsyncCard("Audit export", loader = { viewModel.api.adminAuditExport() }) { data, _ ->
        AnyLines(data, "csv")
        val csv = Phase3Logic.obj(data, "csv")
        Text(
            "Export rows: ${Phase3Logic.int(data, "count") ?: 0}",
            color = SentinelMuted,
        )
        if (csv != null) {
            Text("The full CSV is generated by the export on web.", color = SentinelMuted)
        }
        OutlinedButton(onClick = { shareText(context, "Audit export", data.toString()) }) {
            Text("Share export summary")
        }
    }
    AsyncCard("Login security", loader = { viewModel.api.adminLoginSecurity() }) { data, _ ->
        AnyLines(data, "lockouts", "sessions", "failedByIp")
        NestedLines("Lockouts", Phase3Logic.obj(data, "lockouts"))
        Text("Active sessions: ${Phase3Logic.int(data, "activeSessions") ?: 0}")
        AnyRows(data, "sessions", listOf("role"))
        AnyRows(data, "failedByIp")
    }
}

@Composable
private fun AdminAudit(viewModel: AppViewModel, nav: NavController) {
    MirrorCard("Audit") {
        ReuseLink(
            "Open Activity",
            "The full audit feed rides the Activity console (Phase 8A); " +
                "the Oversight audit explorer is in Security Oversight above it.",
        ) { nav.navigate("activity") }
        Spacer(Modifier.height(6.dp))
        OutlinedButton(onClick = { nav.navigate("adminsec/oversight") }) {
            Text("Open Security Oversight")
        }
    }
}

@Composable
private fun AdminLawAi(viewModel: AppViewModel) {
    AsyncCard("Law AI", loader = { viewModel.api.adminLawAi() }) { data, reload ->
        Phase8bLogic.lawAiSummary(data).forEach { (label, value) ->
            DetailLine(label, value)
        }
        val research = Phase3Logic.arrRaw(data, "modelsResearch")
            .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
        val docgen = Phase3Logic.arrRaw(data, "modelsDocgen")
            .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
        if (research.isNotEmpty()) DetailLine("Research models", research.joinToString(", "))
        if (docgen.isNotEmpty()) DetailLine("Doc-gen models", docgen.joinToString(", "))
        NestedLines("Usage totals", Phase3Logic.obj(data, "totals"))
        var provider by remember(data) { mutableStateOf(Phase3Logic.str(data, "provider")) }
        var cap by remember(data) {
            mutableStateOf((Phase3Logic.int(data, "dailyCap") ?: "").toString())
        }
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        TextFieldRow("Provider", provider, { provider = it })
        TextFieldRow("Daily cap", cap, { cap = it })
        Button(onClick = {
            act {
                viewModel.api.adminLawAiSave(buildJsonObject {
                    put("provider", provider)
                    cap.toIntOrNull()?.let { put("dailyCap", it) }
                })
            }
        }) { Text("Save") }
        ActionErrorText(error)
    }
    AsyncCard("API key", loader = { viewModel.api.adminLawAiKeyStatus() }) { data, reload ->
        AnyLines(data)
        var key by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        var notice by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, {
            key = ""
            notice = "Key installed."
            reload()
        })
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text("API key") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )
        Row {
            Button(onClick = { act { viewModel.api.adminLawAiKeyInstall(key) } }) {
                Text("Install key")
            }
            OutlinedButton(onClick = {
                act { viewModel.api.adminLawAiTest() }
            }) { Text("Test connection") }
        }
        if (notice != null) Text(notice!!)
        ActionErrorText(error)
    }
}

@Composable
private fun AdminBackups(viewModel: AppViewModel) {
    var confirmRequest by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmText by remember { mutableStateOf("") }
    AsyncCard("Backups", loader = { viewModel.api.adminBackupStatus() }) { data, reload ->
        val state = Phase3Logic.str(data, "state")
        DetailLine("State", Phase8bLogic.backupStateLabel(state))
        AnyLines(data, "state")
        Text(
            "The bundle file itself downloads from Command on the web " +
                "with the single-use token this status issues — the app " +
                "mirrors the full request / ready / discard state machine.",
            color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
        )
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        Spacer(Modifier.height(8.dp))
        TextFieldRow("Type the confirm text exactly as web asks", confirmText, { confirmText = it })
        Row {
            Button(onClick = { confirmRequest = true }) { Text("Request backup bundle") }
            OutlinedButton(onClick = { confirmDiscard = true }) { Text("Discard") }
        }
        ActionErrorText(error)
        if (confirmRequest) {
            ConfirmGate(
                "Request backup bundle",
                "Request a fresh server backup bundle? The handler checks " +
                    "the confirm text you typed, exactly as web does.",
                "Request backup bundle",
                onDismiss = { confirmRequest = false },
                onConfirm = {
                    confirmRequest = false
                    act { viewModel.api.adminBackupRequest(confirmText) }
                },
            )
        }
        if (confirmDiscard) {
            ConfirmGate(
                "Discard backup bundle",
                "Discard the current backup bundle? It cannot be downloaded afterwards.",
                "Discard",
                onDismiss = { confirmDiscard = false },
                onConfirm = {
                    confirmDiscard = false
                    act { viewModel.api.adminBackupDiscard() }
                },
            )
        }
    }
}

@Composable
private fun AdminDmarc(viewModel: AppViewModel) {
    var days by remember { mutableStateOf(30) }
    androidx.compose.runtime.key(days) {
    AsyncCard("DMARC reports", loader = { viewModel.api.adminDmarc(days) }) { data, _ ->
        Text(
            "Read-only — this view never changes mail, DNS, or DMARC policy.",
            color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
        )
        ChipRow(
            Phase8bLogic.DMARC_WINDOWS.map { it.first.toString() to it.second },
            days.toString(),
        ) { picked -> picked?.toIntOrNull()?.let { days = it } }
        val summary = Phase3Logic.obj(data, "summary")
        if (summary != null) NestedLines("Summary", summary)
        val inventory = Phase3Logic.obj(data, "inventory")
        if (inventory != null) NestedLines("Inventory", inventory)
        Text("Per-source results", style = MaterialTheme.typography.titleSmall)
        AnyRows(data, "bySource", listOf("volume"))
        Text("Per domain", style = MaterialTheme.typography.titleSmall)
        AnyRows(data, "byDomain", listOf("volume"))
    }
    }
}

@Composable
private fun AdminDanger(viewModel: AppViewModel) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    var confirmRevoke by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    MirrorCard("Danger Zone") {
        Text(
            "Exports ride the same export handlers as web (Phase 3). " +
                "Exports contain the whole workspace — handle them like credentials.",
            color = SentinelMuted,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = {
            scope.launch {
                try {
                    val r = viewModel.api.exportWorkspace()
                    shareText(context, "Workspace export", r.toString())
                } catch (e: Exception) {
                    error = plainError(e, "The export could not be made. Try again.")
                }
            }
        }) { Text("Export workspace") }
        OutlinedButton(onClick = {
            scope.launch {
                try {
                    val r = viewModel.api.exportFull()
                    shareText(context, "Full export", r.toString())
                } catch (e: Exception) {
                    error = plainError(e, "The export could not be made. Try again.")
                }
            }
        }) { Text("Export full archive") }
        Spacer(Modifier.height(10.dp))
        Button(onClick = { confirmRevoke = true }) { Text("Revoke all sessions") }
        ActionErrorText(error)
    }
    if (confirmRevoke) {
        ConfirmGate(
            "Revoke all sessions",
            "Revoke every signed-in session for every user? Everyone, " +
                "including you, will need to sign in again.",
            "Revoke all sessions",
            onDismiss = { confirmRevoke = false },
            onConfirm = {
                confirmRevoke = false
                scope.launch {
                    try {
                        viewModel.api.adminRevokeAllSessions()
                    } catch (e: Exception) {
                        error = plainError(e, "Sessions could not be revoked. Try again.")
                    }
                }
            },
        )
    }
}

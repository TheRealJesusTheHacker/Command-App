package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.data.api.Job
import net.sentinelsafeguard.app.data.api.portalAssign
import net.sentinelsafeguard.app.data.api.portalCurrentTip
import net.sentinelsafeguard.app.data.api.portalOnePager
import net.sentinelsafeguard.app.data.api.portalSecretCreate
import net.sentinelsafeguard.app.data.api.portalSecrets
import net.sentinelsafeguard.app.data.api.portalSubs
import net.sentinelsafeguard.app.data.api.portalTipCreate
import net.sentinelsafeguard.app.data.api.portalTipDelete
import net.sentinelsafeguard.app.data.api.portalTipPatch
import net.sentinelsafeguard.app.data.api.portalTips
import net.sentinelsafeguard.app.data.api.portalUnassign
import net.sentinelsafeguard.app.ui.theme.SentinelMuted
import java.time.LocalDate

/**
 * Portal Settings (Phase 8A) — the mirror of web's b3ops view:
 * Subcontractor assignments, One-time secret delivery, Client
 * one-pager, and Portal security tips, in web's section order.
 * (The view's Review showcase control rides the Phase 6 Reviews
 * surface, exactly as the backend addendum records — it is not
 * duplicated here.)
 *
 * Secrets are METADATA ONLY in this mirror, exactly as web: the
 * create form collects the one-time value and sends it once —
 * it is never displayed back, never stored on the phone, and
 * the list below it shows labels and states only.
 */
@Composable
fun PortalSettingsScreen(viewModel: AppViewModel, nav: NavController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // Shared pickers: clients for secrets / one-pager, jobs for
    // assignments (a wide schedule window, so the picker is not
    // limited to this week).
    var clients by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
    var jobs by remember { mutableStateOf<List<Job>>(emptyList()) }
    LaunchedEffect(Unit) {
        clients = try {
            viewModel.api.clients().items.map { it.id to it.legalName }
        } catch (e: Exception) { emptyList() }
        jobs = try {
            val today = LocalDate.now()
            viewModel.api.scheduleWindow(
                today.minusDays(180).toString(), today.plusDays(180).toString(),
            ).jobs
        } catch (e: Exception) { emptyList() }
    }

    Column {
        MirrorHeader("Portal Settings")
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
            item {
                SubAssignmentsCard(
                    viewModel,
                    jobs,
                    onNotice = { notice = it; error = null },
                    onError = { error = it },
                )
            }
            item {
                SecretsCard(
                    viewModel, clients,
                    onNotice = { notice = it; error = null },
                    onError = { error = it },
                )
            }
            item {
                OnePagerCard(viewModel, clients, context, onError = { error = it })
            }
            item { TipsCard(viewModel, onNotice = { notice = it; error = null }, onError = { error = it }) }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SubAssignmentsCard(
    viewModel: AppViewModel,
    jobs: List<Job>,
    onNotice: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val state = rememberJsonScreen { viewModel.api.portalSubs() }
    var jobPick by remember { mutableStateOf<Job?>(null) }
    var subPick by remember { mutableStateOf<JsonObject?>(null) }
    MirrorCard("Subcontractor assignments") {
        when {
            state.error != null -> {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { state.reloadKey++ }) { Text("Retry") }
            }
            state.data == null -> Text("Loading…", color = SentinelMuted)
            else -> {
                val data = state.data!!
                val assignments = Phase3Logic.arr(data, "assignments")
                val subs = Phase3Logic.arr(data, "subcontractors")
                if (assignments.isEmpty()) {
                    Text("No subcontractor assignments.", color = SentinelMuted)
                }
                assignments.forEach { a ->
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(Phase3Logic.str(a, "jobTitle").ifBlank { "Job #${Phase3Logic.int(a, "jobId") ?: ""}" })
                            Text(
                                Phase3Logic.str(a, "userName"),
                                color = SentinelMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        TextButton(onClick = {
                            val jobId = Phase3Logic.int(a, "jobId") ?: return@TextButton
                            val userId = Phase3Logic.int(a, "userId") ?: return@TextButton
                            scope.launch {
                                try {
                                    val out = viewModel.api.portalUnassign(jobId, userId)
                                    onNotice(Phase3Logic.str(out, "message"))
                                    state.reloadKey++
                                } catch (e: Exception) {
                                    onError(plainError(e, "Could not unassign. Try again."))
                                }
                            }
                        }) { Text("Unassign") }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (jobs.isNotEmpty() && subs.isNotEmpty()) {
                    ChoiceMenu(
                        "Job",
                        jobPick?.title ?: jobs.first().title,
                        jobs.map { it.title },
                    ) { picked -> jobPick = jobs.firstOrNull { it.title == picked } }
                    ChoiceMenu(
                        "Subcontractor",
                        subPick?.let { Phase3Logic.str(it, "displayName") }
                            ?: Phase3Logic.str(subs.first(), "displayName"),
                        subs.map { Phase3Logic.str(it, "displayName") },
                    ) { picked -> subPick = subs.firstOrNull { Phase3Logic.str(it, "displayName") == picked } }
                    Button(onClick = {
                        val job = jobPick ?: jobs.firstOrNull() ?: return@Button
                        val sub = subPick ?: subs.firstOrNull() ?: return@Button
                        val userId = Phase3Logic.int(sub, "id") ?: return@Button
                        scope.launch {
                            try {
                                val out = viewModel.api.portalAssign(job.id, userId)
                                onNotice(Phase3Logic.str(out, "message"))
                                state.reloadKey++
                            } catch (e: Exception) {
                                onError(plainError(e, "Could not assign. Try again."))
                            }
                        }
                    }) { Text("Assign") }
                } else {
                    Text(
                        "Assign needs a scheduled job and a subcontractor account.",
                        color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun SecretsCard(
    viewModel: AppViewModel,
    clients: List<Pair<Int, String>>,
    onNotice: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var clientId by remember { mutableStateOf<Int?>(null) }
    var label by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var expires by remember { mutableStateOf("24") }
    val state = rememberJsonScreen {
        val id = clientId ?: clients.firstOrNull()?.first ?: 0
        viewModel.api.portalSecrets(id)
    }
    LaunchedEffect(clientId, clients) { state.reloadKey++ }
    MirrorCard("One-time secret delivery") {
        Text(
            "Secrets are view-once for the client in their portal. This list shows labels and states only — a secret's value is never shown here.",
            color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(8.dp))
        if (clients.isEmpty()) {
            Text("The client list could not be loaded here.", color = SentinelMuted)
            return@MirrorCard
        }
        val current = clients.firstOrNull { it.first == clientId } ?: clients.first()
        ChoiceMenu("Client", current.second, clients.map { it.second }) { picked ->
            clientId = clients.firstOrNull { it.second == picked }?.first
        }
        when {
            state.error != null -> {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { state.reloadKey++ }) { Text("Retry") }
            }
            state.data == null -> Text("Loading…", color = SentinelMuted)
            else -> {
                val secrets = Phase3Logic.arr(state.data, "secrets")
                if (secrets.isEmpty()) {
                    Text("No secrets for this client.", color = SentinelMuted)
                }
                secrets.forEach { s ->
                    Text(Phase3Logic.str(s, "label").ifBlank { "Secret #${Phase3Logic.int(s, "id") ?: ""}" })
                    Text(
                        Phase8Logic.secretMeta(s),
                        color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        TextFieldRow("Label", label, { label = it })
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text("Secret value (sent once, never shown back)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )
        TextFieldRow("Expires in (hours)", expires, { expires = it })
        Button(onClick = {
            if (label.isBlank()) { onError("Label is required."); return@Button }
            if (value.isBlank()) { onError("Enter the secret value to send."); return@Button }
            val target = clientId ?: clients.firstOrNull()?.first ?: return@Button
            val sentValue = value
            value = "" // write-only: cleared the moment it is sent
            scope.launch {
                try {
                    val out = viewModel.api.portalSecretCreate(
                        target, label.trim(), sentValue, expires.toIntOrNull(),
                    )
                    onNotice(Phase3Logic.str(out, "message"))
                    label = ""
                    state.reloadKey++
                } catch (e: Exception) {
                    onError(plainError(e, "Could not create the secret. Try again."))
                }
            }
        }) { Text("Create secret") }
    }
}

@Composable
private fun OnePagerCard(
    viewModel: AppViewModel,
    clients: List<Pair<Int, String>>,
    context: android.content.Context,
    onError: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var clientId by remember { mutableStateOf<Int?>(null) }
    var pager by remember { mutableStateOf<JsonObject?>(null) }
    var built by remember { mutableStateOf(false) }
    MirrorCard("Client one-pager") {
        if (clients.isEmpty()) {
            Text("The client list could not be loaded here.", color = SentinelMuted)
            return@MirrorCard
        }
        val current = clients.firstOrNull { it.first == clientId } ?: clients.first()
        ChoiceMenu("Client", current.second, clients.map { it.second }) { picked ->
            clientId = clients.firstOrNull { it.second == picked }?.first
            built = false
            pager = null
        }
        Button(onClick = {
            val target = clientId ?: clients.firstOrNull()?.first ?: return@Button
            scope.launch {
                try {
                    pager = viewModel.api.portalOnePager(target)
                    built = true
                } catch (e: Exception) {
                    onError(plainError(e, "The one-pager could not be built. Try again."))
                }
            }
        }) { Text("Build one-pager") }
        if (built) {
            val op = pager?.let { Phase3Logic.obj(it, "onepager") }
            if (op == null) {
                Spacer(Modifier.height(6.dp))
                Text("No one-pager for that client.", color = SentinelMuted)
            } else {
                val text = Phase3Logic.str(op, "documentText")
                Spacer(Modifier.height(6.dp))
                Text(text)
                OutlinedButton(onClick = {
                    shareText(context, "Service summary", text)
                }) { Text("Share") }
            }
        }
    }
}

@Composable
private fun TipsCard(
    viewModel: AppViewModel,
    onNotice: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val state = rememberJsonScreen { viewModel.api.portalTips() }
    val currentState = rememberJsonScreen { viewModel.api.portalCurrentTip() }
    var editingId by remember { mutableStateOf<Int?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(true) }
    var sortOrder by remember { mutableStateOf("1") }
    var deleteTarget by remember { mutableStateOf<JsonObject?>(null) }

    MirrorCard("Portal security tips") {
        val current = currentState.data?.let { Phase3Logic.obj(it, "tip") }
        if (current != null) {
            Text("Current tip: ${Phase3Logic.str(current, "title")}", color = SentinelMuted)
            Spacer(Modifier.height(4.dp))
        }
        when {
            state.error != null -> {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { state.reloadKey++ }) { Text("Retry") }
            }
            state.data == null -> Text("Loading…", color = SentinelMuted)
            else -> {
                val data = state.data!!
                val tips = Phase3Logic.arr(data, "tips")
                val currentTipId = Phase3Logic.int(data, "currentTipId")
                val canWrite = Phase3Logic.bool(data, "canWrite") == true
                if (tips.isEmpty()) {
                    Text("No tips yet.", color = SentinelMuted)
                }
                tips.forEach { tip ->
                    val id = Phase3Logic.int(tip, "id") ?: 0
                    Row {
                        MirrorChip(Phase3Logic.str(tip, "title"))
                        if (id == currentTipId) {
                            Spacer(Modifier.padding(horizontal = 3.dp))
                            MirrorChip("Current")
                        }
                        if (Phase3Logic.bool(tip, "active") != true) {
                            Spacer(Modifier.padding(horizontal = 3.dp))
                            MirrorChip("Inactive")
                        }
                    }
                    Text(Phase3Logic.str(tip, "body"), color = SentinelMuted)
                    if (canWrite) {
                        Row {
                            TextButton(onClick = {
                                editingId = id
                                showForm = true
                                title = Phase3Logic.str(tip, "title")
                                body = Phase3Logic.str(tip, "body")
                                active = Phase3Logic.bool(tip, "active") == true
                                sortOrder = (Phase3Logic.int(tip, "sortOrder") ?: 1).toString()
                            }) { Text("Edit") }
                            TextButton(onClick = { deleteTarget = tip }) { Text("Delete") }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                if (canWrite && !showForm) {
                    Button(onClick = {
                        editingId = null
                        showForm = true
                        title = ""; body = ""; active = true; sortOrder = "1"
                    }) { Text("Add tip") }
                }
                if (showForm) {
                    TextFieldRow("Title", title, { title = it })
                    TextFieldRow("Body", body, { body = it }, singleLine = false, minLines = 3)
                    SwitchRow("Active", active, { active = it })
                    TextFieldRow("Sort order", sortOrder, { sortOrder = it })
                    Row {
                        Button(onClick = {
                            if (title.isBlank()) { onError("Title is required."); return@Button }
                            scope.launch {
                                try {
                                    val out = if (editingId == null) {
                                        viewModel.api.portalTipCreate(
                                            title.trim(), body, active, sortOrder.toIntOrNull() ?: 1,
                                        )
                                    } else {
                                        viewModel.api.portalTipPatch(
                                            editingId!!,
                                            buildJsonObject {
                                                put("title", title.trim())
                                                put("body", body)
                                                put("active", active)
                                                put("sortOrder", sortOrder.toIntOrNull() ?: 1)
                                            },
                                        )
                                    }
                                    onNotice(Phase3Logic.str(out, "message"))
                                    showForm = false
                                    editingId = null
                                    state.reloadKey++
                                    currentState.reloadKey++
                                } catch (e: Exception) {
                                    onError(plainError(e, "Could not save the tip. Try again."))
                                }
                            }
                        }) { Text("Save tip") }
                        OutlinedButton(onClick = { showForm = false; editingId = null }) { Text("Cancel") }
                    }
                }
            }
        }
    }

    deleteTarget?.let { tip ->
        val id = Phase3Logic.int(tip, "id") ?: 0
        ConfirmGate(
            title = "Delete this tip?",
            body = Phase3Logic.str(tip, "title") + "\n\nThis cannot be undone.",
            confirmLabel = "Delete",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                scope.launch {
                    try {
                        val out = viewModel.api.portalTipDelete(id)
                        onNotice(Phase3Logic.str(out, "message"))
                        state.reloadKey++
                        currentState.reloadKey++
                    } catch (e: Exception) {
                        onError(plainError(e, "Could not delete the tip. Try again."))
                    }
                }
            },
        )
    }
}

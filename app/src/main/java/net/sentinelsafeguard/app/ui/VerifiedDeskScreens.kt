package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.data.api.deskDetail
import net.sentinelsafeguard.app.data.api.deskEvidencePack
import net.sentinelsafeguard.app.data.api.deskIssueBadge
import net.sentinelsafeguard.app.data.api.deskList
import net.sentinelsafeguard.app.data.api.deskRevokeBadge
import net.sentinelsafeguard.app.data.api.deskSave
import net.sentinelsafeguard.app.data.api.deskStart
import net.sentinelsafeguard.app.data.api.deskState
import net.sentinelsafeguard.app.data.api.portalOnePager
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * Verified Desk (Phase 8A) — the mirror of web's b4assess view:
 * New assessment, Assessments list, Verified register, and the
 * Generated documents section (evidence-pack download + the
 * one-pager preview, which rides the Portal Settings one-pager
 * endpoint exactly as the backend addendum records). Review
 * responses in the Desk ride the Phase 6 Reviews surface and
 * are not duplicated; a link card points there, as web's Desk
 * sections share that surface.
 *
 * Badge issuance is OWNER-only (enforced inside the handler;
 * the mirror also keeps web's owner gate on the button) and
 * runs behind a confirm with web's semantics. The Desk's badge
 * revoke is its own flow — confirm + a REQUIRED reason kept on
 * the row — distinct from the Phase 3 register revoke in the
 * Clients / Sentinel Verified surface, which stays as it was.
 *
 * Production's register is empty today (0 verifications, 0
 * assessments at the Phase 8A deploy), so the empty states
 * below are the live shapes; populated shapes are pinned by
 * the contract tests against the captured workflow bodies.
 */
@Composable
fun VerifiedDeskScreen(viewModel: AppViewModel, nav: NavController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isOwner = viewModel.me?.user?.role == "OWNER"
    val state = rememberJsonScreen { viewModel.api.deskList() }
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var clients by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
    var startClient by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var category by remember { mutableStateOf("Security Assessment") }
    var packClient by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var framework by remember { mutableStateOf("") }
    var pagerText by remember { mutableStateOf<String?>(null) }
    var revokeTarget by remember { mutableStateOf<JsonObject?>(null) }
    var revokeReason by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        clients = try {
            viewModel.api.clients().items.map { it.id to it.legalName }
        } catch (e: Exception) { emptyList() }
    }

    Column {
        MirrorHeader("Verified Desk", MirrorCopy.subtitle("b4assess"))
        when {
            state.error != null -> ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null -> SkeletonList()
            else -> {
                val data = state.data!!
                val categories = Phase3Logic.arrRaw(data, "categories")
                    .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                val assessments = Phase3Logic.arr(data, "assessments")
                val verifications = Phase3Logic.arr(data, "verifications")
                val canWrite = Phase3Logic.bool(data, "canWrite") == true
                val badgeMeaning = Phase3Logic.str(data, "badgeMeaning")
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
                    if (canWrite) {
                        item {
                            MirrorCard("New assessment") {
                                if (clients.isEmpty()) {
                                    Text("The client list could not be loaded here.", color = SentinelMuted)
                                } else {
                                    val current = startClient ?: clients.first()
                                    ChoiceMenu("Client", current.second, clients.map { it.second }) { picked ->
                                        startClient = clients.firstOrNull { it.second == picked }
                                    }
                                    if (categories.isNotEmpty()) {
                                        ChoiceMenu("Category", category, categories) { category = it }
                                    }
                                    Button(onClick = {
                                        val target = startClient ?: clients.firstOrNull()
                                            ?: return@Button
                                        scope.launch {
                                            try {
                                                val out = viewModel.api.deskStart(target.first, category)
                                                notice = Phase3Logic.str(out, "message").ifBlank { null }
                                                error = null
                                                val newId = Phase3Logic.obj(out, "assessment")
                                                    ?.let { Phase3Logic.int(it, "id") }
                                                state.reloadKey++
                                                if (newId != null) nav.navigate("assessment/$newId")
                                            } catch (e: Exception) {
                                                error = plainError(e, "Could not start the assessment.")
                                            }
                                        }
                                    }) { Text("Start assessment") }
                                }
                            }
                        }
                    }
                    item {
                        MirrorCard("Assessments", assessments.size) {
                            if (assessments.isEmpty()) {
                                Text("No assessments yet.", color = SentinelMuted)
                            }
                            assessments.forEach { a ->
                                val id = Phase3Logic.int(a, "id") ?: 0
                                Text(Phase3Logic.str(a, "title"))
                                val meta = listOf(
                                    Phase3Logic.str(a, "category"),
                                    Phase8Logic.assessmentStateLabel(Phase3Logic.str(a, "status")),
                                    Phase8Logic.scoreLabel(a),
                                ).filter { it.isNotBlank() }.joinToString(" · ")
                                Text(meta, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { nav.navigate("assessment/$id") }) { Text("Open") }
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                    item {
                        MirrorCard("Verified register", verifications.size) {
                            if (badgeMeaning.isNotBlank()) {
                                Text(
                                    badgeMeaning, color = SentinelMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                            if (verifications.isEmpty()) {
                                Text("No verifications yet.", color = SentinelMuted)
                            }
                            verifications.forEach { v ->
                                Text(Phase3Logic.str(v, "businessName"))
                                val meta = listOf(
                                    Phase3Logic.str(v, "code"),
                                    Phase3Logic.str(v, "category"),
                                    Phase3Logic.str(v, "status"),
                                    Phase3Logic.dbl(v, "score")?.let { "Score $it" } ?: "",
                                ).filter { it.isNotBlank() }.joinToString(" · ")
                                Text(meta, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
                                val reason = Phase3Logic.str(v, "revokedReason")
                                if (reason.isNotBlank()) {
                                    Text(
                                        "Revoked: $reason", color = SentinelMuted,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                if (isOwner && Phase3Logic.str(v, "status") == "active") {
                                    TextButton(onClick = {
                                        revokeTarget = v
                                        revokeReason = ""
                                    }) { Text("Revoke badge") }
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                            OutlinedButton(onClick = { nav.navigate("verified") }) {
                                Text("Open Sentinel Verified register")
                            }
                        }
                    }
                    item {
                        MirrorCard("Generated documents") {
                            if (clients.isEmpty()) {
                                Text("The client list could not be loaded here.", color = SentinelMuted)
                            } else {
                                val current = packClient ?: clients.first()
                                ChoiceMenu("Client", current.second, clients.map { it.second }) { picked ->
                                    packClient = clients.firstOrNull { it.second == picked }
                                    pagerText = null
                                }
                                TextFieldRow("Framework (optional)", framework, { framework = it })
                                Row {
                                    Button(onClick = {
                                        val target = packClient ?: clients.firstOrNull()
                                            ?: return@Button
                                        scope.launch {
                                            try {
                                                val pack = viewModel.api.deskEvidencePack(
                                                    target.first, framework.ifBlank { null },
                                                )
                                                val bytes = Phase8Logic.evidencePdfBytes(pack)
                                                if (bytes == null) {
                                                    error = "The evidence pack came back without its PDF. Try again."
                                                } else {
                                                    shareBytes(
                                                        context,
                                                        Phase3Logic.str(pack, "fileName")
                                                            .ifBlank { "evidence-pack.pdf" },
                                                        "application/pdf",
                                                        bytes,
                                                    )
                                                    notice = "Evidence pack downloaded."
                                                    error = null
                                                }
                                            } catch (e: Exception) {
                                                error = plainError(e, "The evidence pack could not be built.")
                                            }
                                        }
                                    }) { Text("Download evidence pack") }
                                    Spacer(Modifier.padding(horizontal = 4.dp))
                                    OutlinedButton(onClick = {
                                        val target = packClient ?: clients.firstOrNull()
                                            ?: return@OutlinedButton
                                        scope.launch {
                                            try {
                                                val out = viewModel.api.portalOnePager(target.first)
                                                pagerText = Phase3Logic.obj(out, "onepager")
                                                    ?.let { Phase3Logic.str(it, "documentText") }
                                                    ?: "No one-pager for that client."
                                            } catch (e: Exception) {
                                                error = plainError(e, "The one-pager could not be built.")
                                            }
                                        }
                                    }) { Text("Preview one-pager") }
                                }
                                if (pagerText != null) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(pagerText!!)
                                }
                            }
                        }
                    }
                    item {
                        MirrorCard("Review responses") {
                            Text(
                                "Review responses are handled in Reviews — the same surface web's Desk shares.",
                                color = SentinelMuted,
                            )
                            TextButton(onClick = { nav.navigate("reviews") }) { Text("Open Reviews") }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    revokeTarget?.let { v ->
        val vid = Phase3Logic.int(v, "id") ?: 0
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { revokeTarget = null },
            title = { Text("Revoke this badge?") },
            text = {
                Column {
                    Text(
                        "${Phase3Logic.str(v, "businessName")} · ${Phase3Logic.str(v, "code")}" +
                            "\n\nRevoking is permanent. The reason is kept on the record.",
                    )
                    TextFieldRow("Reason (required)", revokeReason, { revokeReason = it })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (revokeReason.isBlank()) {
                        error = "A reason is required to revoke a badge."
                        revokeTarget = null
                        return@Button
                    }
                    val reason = revokeReason.trim()
                    revokeTarget = null
                    scope.launch {
                        try {
                            val out = viewModel.api.deskRevokeBadge(vid, reason)
                            notice = Phase3Logic.str(out, "message").ifBlank { null }
                            error = null
                            state.reloadKey++
                        } catch (e: Exception) {
                            error = plainError(e, "Could not revoke the badge. Try again.")
                        }
                    }
                }) { Text("Revoke") }
            },
            dismissButton = {
                TextButton(onClick = { revokeTarget = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
fun AssessmentDetailScreen(viewModel: AppViewModel, nav: NavController, id: Int) {
    val scope = rememberCoroutineScope()
    val isOwner = viewModel.me?.user?.role == "OWNER"
    val state = rememberJsonScreen { viewModel.api.deskDetail(id) }
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    // Local checklist edits, keyed by control key.
    var edits by remember { mutableStateOf<Map<String, Pair<String, String>>>(emptyMap()) }
    var seededFor by remember { mutableStateOf<Int?>(null) }
    var confirmIssue by remember { mutableStateOf(false) }
    var revokeReason by remember { mutableStateOf("") }
    var showRevoke by remember { mutableStateOf(false) }

    val data = state.data
    val assessment = data?.let { Phase3Logic.obj(it, "assessment") }
    LaunchedEffect(data) {
        if (assessment != null && seededFor != id) {
            edits = Phase3Logic.arr(assessment, "controls").associate { c ->
                Phase3Logic.str(c, "key") to
                    (Phase3Logic.str(c, "status") to Phase3Logic.str(c, "evidenceNote"))
            }
            seededFor = id
        }
    }

    Column {
        TextButton(onClick = { nav.popBackStack() }) { Text("← Verified Desk") }
        when {
            state.error != null -> ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null || assessment == null -> SkeletonList()
            else -> {
                val a = assessment
                val status = Phase3Logic.str(a, "status")
                val verification = Phase3Logic.obj(a, "verification")
                LazyColumn {
                    item { MirrorHeader(Phase3Logic.str(a, "title")) }
                    item {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            if (notice != null) Text(notice!!, color = SentinelMuted)
                            if (error != null) {
                                Text(error!!, color = MaterialTheme.colorScheme.error)
                            }
                            JsonLines(
                                a,
                                "category" to "Category",
                                "status" to "Status",
                                "createdAt" to "Created",
                                "updatedAt" to "Last updated",
                                "completedAt" to "Completed",
                            )
                            DetailLine("Score", Phase8Logic.scoreLabel(a))
                            val meaning = Phase3Logic.str(a, "badgeMeaning")
                            if (meaning.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(meaning, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    // Checklist, grouped by family in payload order.
                    val controls = Phase3Logic.arr(a, "controls")
                    val families = controls.map { Phase3Logic.str(it, "family") }.distinct()
                    families.forEach { family ->
                        item { SectionHeader(family) }
                        items(controls.filter { Phase3Logic.str(it, "family") == family }.size) { idx ->
                            val c = controls.filter { Phase3Logic.str(it, "family") == family }[idx]
                            val key = Phase3Logic.str(c, "key")
                            val edit = edits[key] ?: (Phase3Logic.str(c, "status") to Phase3Logic.str(c, "evidenceNote"))
                            MirrorCard(Phase3Logic.str(c, "label")) {
                                Text(
                                    "Weight ${Phase3Logic.dbl(c, "weight") ?: ""}",
                                    color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
                                )
                                ChoiceMenu(
                                    "Status",
                                    Phase8Logic.controlStatusLabel(edit.first),
                                    Phase8Logic.CONTROL_STATUSES.map { it.second },
                                ) { picked ->
                                    edits = edits + (key to (Phase8Logic.controlStatusValue(picked) to edit.second))
                                }
                                TextFieldRow("Evidence note", edit.second, { note ->
                                    edits = edits + (key to (edit.first to note))
                                })
                            }
                        }
                    }
                    item {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Row {
                                Button(onClick = {
                                    scope.launch {
                                        try {
                                            val arr = JsonArray(
                                                Phase3Logic.arr(a, "controls").map { c ->
                                                    val key = Phase3Logic.str(c, "key")
                                                    val edit = edits[key]
                                                        ?: (Phase3Logic.str(c, "status") to Phase3Logic.str(c, "evidenceNote"))
                                                    buildJsonObject {
                                                        put("key", key)
                                                        put("status", edit.first)
                                                        put("evidenceNote", edit.second)
                                                    }
                                                }
                                            )
                                            val out = viewModel.api.deskSave(id, arr)
                                            notice = Phase3Logic.str(out, "message").ifBlank { null }
                                            error = null
                                            seededFor = null
                                            state.reloadKey++
                                        } catch (e: Exception) {
                                            error = plainError(e, "Could not save the checklist.")
                                        }
                                    }
                                }) { Text("Save draft") }
                                Phase8Logic.nextStates(status).forEach { to ->
                                    Spacer(Modifier.padding(horizontal = 4.dp))
                                    OutlinedButton(onClick = {
                                        scope.launch {
                                            try {
                                                val out = viewModel.api.deskState(id, to)
                                                notice = Phase3Logic.str(out, "message").ifBlank { null }
                                                error = null
                                                seededFor = null
                                                state.reloadKey++
                                            } catch (e: Exception) {
                                                error = plainError(e, "Could not move the assessment.")
                                            }
                                        }
                                    }) {
                                        Text(
                                            when (to) {
                                                "in_review" -> "Move to in review"
                                                "completed" -> "Complete assessment"
                                                else -> Phase8Logic.assessmentStateLabel(to)
                                            }
                                        )
                                    }
                                }
                            }
                            if (isOwner && status == "completed" && verification == null) {
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { confirmIssue = true }) { Text("Issue badge") }
                            }
                            if (verification != null) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Badge ${Phase3Logic.str(verification, "code")} · " +
                                        Phase3Logic.str(verification, "status"),
                                )
                                val vReason = Phase3Logic.str(verification, "revokedReason")
                                if (vReason.isNotBlank()) Text("Revoked: $vReason", color = SentinelMuted)
                                if (isOwner && Phase3Logic.str(verification, "status") == "active") {
                                    TextButton(onClick = { showRevoke = true; revokeReason = "" }) {
                                        Text("Revoke badge")
                                    }
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }

                if (confirmIssue) {
                    ConfirmGate(
                        title = "Issue the Verified badge?",
                        body = "${Phase3Logic.str(a, "title")}\n\n" +
                            "Issuing publishes this client to the Verified register with its score. " +
                            Phase3Logic.str(a, "badgeMeaning"),
                        confirmLabel = "Issue badge",
                        onDismiss = { confirmIssue = false },
                        onConfirm = {
                            confirmIssue = false
                            scope.launch {
                                try {
                                    val out = viewModel.api.deskIssueBadge(id)
                                    notice = Phase3Logic.str(out, "message").ifBlank { null }
                                    error = null
                                    seededFor = null
                                    state.reloadKey++
                                } catch (e: Exception) {
                                    error = plainError(e, "Could not issue the badge. Try again.")
                                }
                            }
                        },
                    )
                }
                if (showRevoke) {
                    val vid = Phase3Logic.int(verification!!, "id") ?: 0
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showRevoke = false },
                        title = { Text("Revoke this badge?") },
                        text = {
                            Column {
                                Text("Revoking is permanent. The reason is kept on the record.")
                                TextFieldRow("Reason (required)", revokeReason, { revokeReason = it })
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                if (revokeReason.isBlank()) {
                                    error = "A reason is required to revoke a badge."
                                    showRevoke = false
                                    return@Button
                                }
                                val reason = revokeReason.trim()
                                showRevoke = false
                                scope.launch {
                                    try {
                                        val out = viewModel.api.deskRevokeBadge(vid, reason)
                                        notice = Phase3Logic.str(out, "message").ifBlank { null }
                                        error = null
                                        seededFor = null
                                        state.reloadKey++
                                    } catch (e: Exception) {
                                        error = plainError(e, "Could not revoke the badge. Try again.")
                                    }
                                }
                            }) { Text("Revoke") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRevoke = false }) { Text("Cancel") }
                        },
                    )
                }
            }
        }
    }
}

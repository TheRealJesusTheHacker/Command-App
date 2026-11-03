package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import net.sentinelsafeguard.app.data.api.Client
import net.sentinelsafeguard.app.data.api.Engagement
import net.sentinelsafeguard.app.data.api.EngagementEdit
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * Engagements (mirror spec §6 Phase 2): web's Engagements view —
 * the pentest engagement workspace. List / profile / create /
 * edit ride the Phase 2 backend (getcfengagements /
 * savecfengagement semantics). Web's own rule rides along:
 * findings cannot open until a signed authorization is on file.
 * (The Contracts table, contract templates, and findings library
 * sections of the web view ride web-only actions the Phase 2
 * backend did not expose — flagged in the hunt6 record, not
 * silently cut.)
 */

@Composable
fun EngagementsScreen(viewModel: AppViewModel, nav: NavController) {
    var engagements by remember { mutableStateOf<List<Engagement>?>(null) }
    var clientsById by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        error = null
        try {
            engagements = viewModel.api.engagements().engagements
            clientsById = try {
                viewModel.api.clients().items.associate { it.id to it.legalName }
            } catch (e: Exception) {
                emptyMap()
            }
        } catch (e: Exception) {
            error = plainError(e, "Engagements could not be loaded. Check your connection and try again.")
        }
    }

    Column {
        MirrorHeader("Engagements") {
            Button(onClick = { nav.navigate("engagementNew") }) { Text("New engagement") }
        }
        when {
            error != null -> ErrorBox(error!!) { reloadKey++ }
            engagements == null -> SkeletonList(5)
            engagements!!.isEmpty() -> EmptyBox(
                "No engagements yet",
                "No engagements have been created yet.",
            )
            else -> LazyColumn {
                items(engagements!!) { eng ->
                    ItemRow(
                        eng.title,
                        listOfNotNull(
                            eng.clientId?.let { clientsById[it] ?: "Client #$it" },
                            eng.status.takeIf { it.isNotBlank() },
                            eng.scope.takeIf { it.isNotBlank() },
                            eng.startDate?.takeIf { it.isNotBlank() },
                            eng.endDate?.takeIf { it.isNotBlank() }?.let { "to $it" },
                            "${eng.findings.size} finding(s)",
                            if (eng.hasSignedAuthorization) "Signed authorization on file" else null,
                        ).joinToString(" · "),
                    ) { nav.navigate("engagement/${eng.id}") }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun EngagementProfileScreen(viewModel: AppViewModel, nav: NavController, engagementId: Int) {
    var eng by remember { mutableStateOf<Engagement?>(null) }
    var clientName by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(engagementId, reloadKey) {
        error = null
        try {
            eng = viewModel.api.engagement(engagementId)
            val cid = eng!!.clientId
            clientName = if (cid != null) {
                try {
                    viewModel.api.client(cid).legalName
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            error = plainError(e, "This engagement could not be loaded. Go back and try again.")
        }
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        TextButton(onClick = { nav.popBackStack() }) { Text("← Back to engagements") }
        when {
            error != null -> ErrorBox(error!!) { reloadKey++ }
            eng == null -> SkeletonList(4)
            else -> {
                val e = eng!!
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(e.title, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MirrorChip(e.status)
                        if (e.scope.isNotBlank()) MirrorChip(e.scope)
                        if (e.hasSignedAuthorization) MirrorChip("Signed authorization on file")
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { nav.navigate("engagementEdit/${e.id}") }) {
                        Text("Edit engagement")
                    }
                }
                MirrorCard("Engagement details") {
                    DetailLine("Client", clientName ?: e.clientId?.let { "Client #$it" })
                    DetailLine("Scope", e.scope)
                    DetailLine("Status", e.status)
                    DetailLine("Start date", e.startDate)
                    DetailLine("End date", e.endDate)
                    DetailLine(
                        "Signed authorization",
                        if (e.hasSignedAuthorization) "On file" else "Not on file",
                    )
                    DetailLine(
                        "Report published",
                        if (e.reportPublished) "Published" else "Not published",
                    )
                    DetailLine("Created", if (e.createdAt > 0) formatEpochMs(e.createdAt) else null)
                    DetailLine("Last updated", if (e.updatedAt > 0) formatEpochMs(e.updatedAt) else null)
                    if (e.clientId != null) {
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { nav.navigate("client/${e.clientId}") }) {
                            Text("Open client profile")
                        }
                    }
                }
                MirrorCard("Rules of engagement") {
                    Text(e.rulesOfEngagement.ifBlank { "No rules of engagement recorded." })
                }
                MirrorCard("Findings", e.findings.size) {
                    if (!e.hasSignedAuthorization) {
                        Text(
                            "Findings cannot open until a signed authorization is on file.",
                            color = SentinelMuted,
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    if (e.findings.isEmpty()) {
                        Text("No findings recorded yet.", color = SentinelMuted)
                    }
                    e.findings.forEach { finding ->
                        ItemRow(
                            finding.title.ifBlank { "Finding" },
                            listOfNotNull(
                                finding.severity.takeIf { it.isNotBlank() },
                                finding.status.takeIf { it.isNotBlank() },
                                finding.description.takeIf { it.isNotBlank() }
                                    .ifBlank { finding.detail },
                            ).joinToString(" · "),
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun EngagementFormScreen(viewModel: AppViewModel, nav: NavController, engagementId: Int?) {
    val scope = rememberCoroutineScope()
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var statuses by remember { mutableStateOf(MirrorVocab.ENGAGEMENT_STATUSES) }
    var loaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var clientId by remember { mutableStateOf<Int?>(null) }
    var title by remember { mutableStateOf("") }
    var engScope by remember { mutableStateOf("") }
    var rules by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Planned") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(engagementId) {
        try {
            clients = viewModel.api.clients().items
            try {
                val list = viewModel.api.engagements()
                if (list.engagementStatuses.isNotEmpty()) statuses = list.engagementStatuses
            } catch (e: Exception) {
                // The built-in vocabulary stands.
            }
            if (engagementId != null) {
                val e = viewModel.api.engagement(engagementId)
                clientId = e.clientId
                title = e.title
                engScope = e.scope
                rules = e.rulesOfEngagement
                status = e.status
                startDate = e.startDate ?: ""
                endDate = e.endDate ?: ""
            }
            loaded = true
        } catch (e: Exception) {
            loadError = plainError(e, "The form could not be loaded. Go back and try again.")
        }
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        MirrorHeader(if (engagementId == null) "New engagement" else "Edit engagement")
        when {
            loadError != null -> ErrorBox(loadError!!)
            !loaded -> SkeletonList(4)
            else -> Column(Modifier.padding(horizontal = 16.dp)) {
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
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; titleError = null },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (titleError != null) {
                    Text(titleError!!, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = engScope,
                    onValueChange = { engScope = it },
                    label = { Text("Scope") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = rules,
                    onValueChange = { rules = it },
                    label = { Text("Rules of engagement") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                Spacer(Modifier.height(12.dp))
                ChoiceMenu("Status", status, statuses) { status = it }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start date (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("End date (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
                                titleError = "Title is required."
                            } else {
                                busy = true
                                scope.launch {
                                    try {
                                        val edit = EngagementEdit(
                                            clientId = clientId,
                                            title = title.trim(),
                                            scope = engScope,
                                            rulesOfEngagement = rules,
                                            status = status,
                                            startDate = startDate.ifBlank { null },
                                            endDate = endDate.ifBlank { null },
                                        )
                                        if (engagementId == null) {
                                            val created = viewModel.api.createEngagement(edit)
                                            nav.popBackStack()
                                            nav.navigate("engagement/${created.id}")
                                        } else {
                                            viewModel.api.patchEngagement(engagementId, edit)
                                            nav.popBackStack()
                                        }
                                    } catch (e: Exception) {
                                        busy = false
                                        saveError = plainError(e, "The engagement could not be saved. Try again.")
                                    }
                                }
                            }
                        },
                    ) { Text(if (busy) "Saving…" else "Save engagement") }
                    OutlinedButton(onClick = { nav.popBackStack() }) { Text("Cancel") }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

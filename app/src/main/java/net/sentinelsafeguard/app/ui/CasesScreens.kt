package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.BorderStroke
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
import net.sentinelsafeguard.app.data.api.CaseFullEdit
import net.sentinelsafeguard.app.data.api.CaseDetailResponse
import net.sentinelsafeguard.app.data.api.CaseItem
import net.sentinelsafeguard.app.data.api.Client
import net.sentinelsafeguard.app.data.api.User
import net.sentinelsafeguard.app.ui.theme.SentinelMuted
import net.sentinelsafeguard.app.ui.theme.SentinelOutline
import net.sentinelsafeguard.app.ui.theme.SentinelSurface

/**
 * Cases (mirror spec §6 Phase 2): web's Cases view — header stats,
 * Board | Directory, the web card field set, the full profile
 * (details / client / notes / linked tickets / activity), and the
 * create/edit form on the Phase 2 backend (savecase semantics;
 * a status change in the same save rides setcasestatus server-side,
 * the web form's own order). Cross-links run both ways: a case
 * opens its client and its tickets; tickets and clients open it.
 */

private fun caseAssigneeName(case: CaseItem, roster: List<User>): String =
    roster.firstOrNull { it.id == case.assignedUserId }?.displayName ?: "Unassigned"

private fun caseClientLabel(case: CaseItem): String =
    case.clientName?.takeIf { it.isNotBlank() }
        ?: case.clientId?.let { "Client #$it" }
        ?: "No client"

private fun caseDueLabel(case: CaseItem, today: String): String? {
    val due = case.dueDate?.takeIf { it.isNotBlank() } ?: return null
    val overdue = MirrorVocab.isOverdue(due, today, MirrorVocab.isDone(case.status))
    return "Due $due" + if (overdue) " · OVERDUE" else ""
}

@Composable
fun CasesScreen(viewModel: AppViewModel, nav: NavController) {
    val scope = rememberCoroutineScope()
    var all by remember { mutableStateOf<List<CaseItem>?>(null) }
    var total by remember { mutableStateOf(0) }
    var roster by remember { mutableStateOf<List<User>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var directoryMode by remember { mutableStateOf(false) }
    val todayStr = remember { java.time.LocalDate.now().toString() }

    LaunchedEffect(reloadKey) {
        error = null
        try {
            val page = viewModel.api.casesFiltered()
            all = page.items
            total = page.total
            roster = try {
                viewModel.api.roster()
            } catch (e: Exception) {
                emptyList()
            }
        } catch (e: Exception) {
            error = plainError(e, "Cases could not be loaded. Check your connection and try again.")
        }
    }

    fun moveStage(case: CaseItem, stage: String) {
        scope.launch {
            try {
                viewModel.api.setCaseStatus(case.id, stage)
                reloadKey++
            } catch (e: Exception) {
                error = plainError(e, "That stage move could not be saved. Try again.")
            }
        }
    }

    Column {
        MirrorHeader("Cases", MirrorCopy.subtitle("cases")) {
            Button(onClick = { nav.navigate("caseNew") }) { Text("+ New case") }
        }
        when {
            error != null -> ErrorBox(error!!) { reloadKey++ }
            all == null -> SkeletonList(6)
            else -> {
                val cases = all!!
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    StatCard(
                        "Open cases",
                        cases.count { !MirrorVocab.isDone(it.status) }.toString(),
                        Modifier.weight(1f),
                    )
                    StatCard(
                        "In progress",
                        cases.count { it.status == "In Progress" }.toString(),
                        Modifier.weight(1f),
                    )
                    StatCard(
                        "Overdue",
                        cases.count {
                            MirrorVocab.isOverdue(it.dueDate, todayStr, MirrorVocab.isDone(it.status))
                        }.toString(),
                        Modifier.weight(1f),
                    )
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    StatCard(
                        "Unassigned",
                        cases.count { it.assignedUserId == null }.toString(),
                        Modifier.weight(1f),
                    )
                    StatCard(
                        "Closed",
                        cases.count { it.status == "Closed" }.toString(),
                        Modifier.weight(1f),
                    )
                    StatCard("Total cases", total.toString(), Modifier.weight(1f))
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = !directoryMode,
                        onClick = { directoryMode = false },
                        label = { Text("Board") },
                    )
                    FilterChip(
                        selected = directoryMode,
                        onClick = { directoryMode = true },
                        label = { Text("Directory") },
                    )
                    Text(
                        "$total case(s)",
                        color = SentinelMuted,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                if (directoryMode) {
                    CaseDirectory(
                        viewModel = viewModel,
                        base = cases,
                        roster = roster,
                        todayStr = todayStr,
                        onOpen = { nav.navigate("case/${it.id}") },
                        onClient = { c -> c.clientId?.let { nav.navigate("client/$it") } },
                        onNew = { nav.navigate("caseNew") },
                    )
                } else {
                    LazyColumn {
                        MirrorVocab.groupCaseBoard(cases).forEach { (stage, cards) ->
                            item { SectionHeader(stage, cards.size) }
                            if (cards.isEmpty()) {
                                item {
                                    Text(
                                        "No cases in this stage",
                                        color = SentinelMuted,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    )
                                }
                            }
                            items(cards) { case ->
                                CaseCard(
                                    case = case,
                                    roster = roster,
                                    todayStr = todayStr,
                                    onOpen = { nav.navigate("case/${case.id}") },
                                    onClient = {
                                        case.clientId?.let { nav.navigate("client/$it") }
                                    },
                                    onStage = { moveStage(case, it) },
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
private fun CaseCard(
    case: CaseItem,
    roster: List<User>,
    todayStr: String,
    onOpen: () -> Unit,
    onClient: () -> Unit,
    onStage: (String) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SentinelSurface),
        border = BorderStroke(1.dp, SentinelOutline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            TextButton(onClick = onOpen) {
                Text(case.title, style = MaterialTheme.typography.bodyLarge)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (case.clientId != null) {
                    TextButton(onClick = onClient) { Text(caseClientLabel(case)) }
                } else {
                    Text(
                        caseClientLabel(case),
                        color = SentinelMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    " · Case #${case.id}",
                    color = SentinelMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MirrorChip(case.status)
                MirrorChip(case.priority)
                if (case.adMaterial) MirrorChip("★ Ad material")
            }
            if (case.category.isNotBlank()) {
                Text(case.category, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "Assignee ${caseAssigneeName(case, roster)}",
                color = SentinelMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (case.openedDate.isNotBlank()) {
                Text(
                    "Opened ${case.openedDate}",
                    color = SentinelMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            caseDueLabel(case, todayStr)?.let {
                Text(it, color = SentinelMuted, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "${case.ticketCount} ticket(s)",
                color = SentinelMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChoiceMenu("Status", case.status, MirrorVocab.stageOptions(case.status), onStage)
                Spacer(Modifier.weight(1f))
                StageStepper(
                    current = case.status,
                    onStep = onStage,
                    onOpen = onOpen,
                    openLabel = "Open case",
                )
            }
        }
    }
}

@Composable
private fun CaseDirectory(
    viewModel: AppViewModel,
    base: List<CaseItem>,
    roster: List<User>,
    todayStr: String,
    onOpen: (CaseItem) -> Unit,
    onClient: (CaseItem) -> Unit,
    onNew: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(MirrorVocab.CASE_SORTS[0]) }
    var stage by remember { mutableStateOf<String?>(null) }
    var priority by remember { mutableStateOf<String?>(null) }
    var overdueOnly by remember { mutableStateOf(false) }
    var unassignedOnly by remember { mutableStateOf(false) }
    var adOnly by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<CaseItem>?>(null) }
    var dirError by remember { mutableStateOf<String?>(null) }

    fun reset() {
        q = ""; sort = MirrorVocab.CASE_SORTS[0]; stage = null; priority = null
        overdueOnly = false; unassignedOnly = false; adOnly = false
    }

    LaunchedEffect(q, sort, stage, priority, overdueOnly, unassignedOnly, adOnly) {
        dirError = null
        try {
            val params = MirrorVocab.caseDirectoryParams(
                q, stage, priority, overdueOnly, unassignedOnly, adOnly, sort,
            )
            results = viewModel.api.casesFiltered(
                q = params["q"],
                status = params["status"],
                priority = params["priority"],
                overdue = params["overdue"] != null,
                unassigned = params["unassigned"] != null,
                adMaterial = params["adMaterial"] != null,
                sort = params["sort"],
            ).items
        } catch (e: Exception) {
            dirError = plainError(e, "The case search could not be run. Try again.")
        }
    }

    LazyColumn {
        item {
            OutlinedTextField(
                value = q,
                onValueChange = { q = it },
                label = { Text("Search cases") },
                placeholder = { Text("Title, client, case #, or category") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChoiceMenu("Sort cases", sort, MirrorVocab.CASE_SORTS) { sort = it }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { reset() }) { Text("Reset filters") }
            }
            Text(
                "Stage",
                color = SentinelMuted,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            )
            ChipRow(
                options = listOf("all" to "All ${base.size}") +
                    MirrorVocab.TICKET_STAGES.map { sg -> sg to "$sg ${base.count { it.status == sg }}" },
                selected = stage ?: "all",
                onPick = { stage = it },
            )
            Text(
                "Priority",
                color = SentinelMuted,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            )
            ChipRow(
                options = listOf("all" to "All ${base.size}") +
                    MirrorVocab.PRIORITIES.map { p -> p to "$p ${base.count { it.priority == p }}" },
                selected = priority ?: "all",
                onPick = { priority = it },
            )
            Text(
                "Show only",
                color = SentinelMuted,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = overdueOnly,
                    onClick = { overdueOnly = !overdueOnly },
                    label = {
                        Text("Overdue ${base.count { MirrorVocab.isOverdue(it.dueDate, todayStr, MirrorVocab.isDone(it.status)) }}")
                    },
                )
                FilterChip(
                    selected = unassignedOnly,
                    onClick = { unassignedOnly = !unassignedOnly },
                    label = { Text("Unassigned ${base.count { it.assignedUserId == null }}") },
                )
                FilterChip(
                    selected = adOnly,
                    onClick = { adOnly = !adOnly },
                    label = { Text("★ Ad material ${base.count { it.adMaterial }}") },
                )
            }
        }
        when {
            dirError != null -> item { ErrorBox(dirError!!) }
            results == null -> item { SkeletonList(4) }
            base.isEmpty() -> item {
                EmptyBox("No cases yet", "No cases have been opened yet.")
                Button(
                    onClick = onNew,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) { Text("+ New case") }
            }
            results!!.isEmpty() -> item {
                EmptyBox("No matches", "No cases match the current search and filters.")
                OutlinedButton(
                    onClick = { reset() },
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) { Text("Reset search & filters") }
            }
            else -> items(results!!) { case ->
                CaseCard(
                    case = case,
                    roster = roster,
                    todayStr = todayStr,
                    onOpen = { onOpen(case) },
                    onClient = { onClient(case) },
                    onStage = { stageName ->
                        scope.launch {
                            try {
                                viewModel.api.setCaseStatus(case.id, stageName)
                                results = results!!.map {
                                    if (it.id == case.id) it.copy(status = stageName) else it
                                }
                            } catch (e: Exception) {
                                dirError = plainError(e, "That stage move could not be saved. Try again.")
                            }
                        }
                    },
                )
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun CaseProfileScreen(viewModel: AppViewModel, nav: NavController, caseId: Int) {
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<CaseDetailResponse?>(null) }
    var roster by remember { mutableStateOf<List<User>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    val todayStr = remember { java.time.LocalDate.now().toString() }

    LaunchedEffect(caseId, reloadKey) {
        error = null
        try {
            detail = viewModel.api.caseDetail(caseId)
            roster = try {
                viewModel.api.roster()
            } catch (e: Exception) {
                emptyList()
            }
        } catch (e: Exception) {
            error = plainError(e, "This case could not be loaded. Go back and try again.")
        }
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        TextButton(onClick = { nav.popBackStack() }) { Text("← Back to board") }
        when {
            error != null -> ErrorBox(error!!) { reloadKey++ }
            detail == null -> SkeletonList(4)
            else -> {
                val d = detail!!
                val c = d.case
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(c.title, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Case #${c.id} · " + (
                            d.client?.legalName
                                ?: c.clientId?.let { "Client #$it" }
                                ?: "No client"
                            ),
                        color = SentinelMuted,
                    )
                    Row(
                        Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        MirrorChip(c.status)
                        MirrorChip(c.priority)
                        if (c.adMaterial) MirrorChip("★ Ad material")
                    }
                    if (actionError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(actionError!!, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ChoiceMenu("Status", c.status, MirrorVocab.stageOptions(c.status)) { picked ->
                            scope.launch {
                                try {
                                    viewModel.api.setCaseStatus(c.id, picked)
                                    reloadKey++
                                } catch (e: Exception) {
                                    actionError = plainError(e, "That change could not be saved. Try again.")
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { nav.navigate("caseEdit/${c.id}") }) { Text("Edit case") }
                        if (c.archivedAt == null) {
                            OutlinedButton(onClick = {
                                scope.launch {
                                    try {
                                        viewModel.api.archiveCase(c.id)
                                        nav.popBackStack()
                                    } catch (e: Exception) {
                                        actionError = plainError(e, "The case could not be archived. Try again.")
                                    }
                                }
                            }) { Text("Archive") }
                        } else {
                            OutlinedButton(onClick = {
                                scope.launch {
                                    try {
                                        viewModel.api.restoreCase(c.id)
                                        reloadKey++
                                    } catch (e: Exception) {
                                        actionError = plainError(e, "The case could not be restored. Try again.")
                                    }
                                }
                            }) { Text("Restore") }
                        }
                    }
                }

                MirrorCard("Case details") {
                    DetailLine("Status", c.status)
                    DetailLine("Priority", c.priority)
                    DetailLine("Category", c.category)
                    DetailLine("Assignee", caseAssigneeName(c, roster))
                    DetailLine("Client", d.client?.legalName ?: c.clientName)
                    DetailLine("Opened", c.openedDate.takeIf { it.isNotBlank() })
                    DetailLine(
                        "Due",
                        c.dueDate?.takeIf { it.isNotBlank() }?.let { due ->
                            due + if (MirrorVocab.isOverdue(due, todayStr, MirrorVocab.isDone(c.status))) " · OVERDUE" else ""
                        },
                    )
                    DetailLine("Closed", c.closedDate.takeIf { it.isNotBlank() })
                    DetailLine("Created", if (c.createdAt > 0) formatEpochMs(c.createdAt) else null)
                    DetailLine("Last updated", if (c.updatedAt > 0) formatEpochMs(c.updatedAt) else null)
                }

                MirrorCard("Client") {
                    if (d.client == null) {
                        Text("No client linked to this case.", color = SentinelMuted)
                    } else {
                        val client = d.client
                        DetailLine("Client", client.legalName)
                        DetailLine("Contact", client.contactName)
                        DetailLine("Email", client.email)
                        DetailLine("Phone", client.phone)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { nav.navigate("client/${client.id}") }) {
                            Text("Open client profile")
                        }
                    }
                }

                MirrorCard("Notes") {
                    Text(c.notes.ifBlank { "No notes on this case." })
                }

                MirrorCard("Tickets", d.tickets.size) {
                    if (d.tickets.isEmpty()) {
                        Text("No tickets linked to this case yet.", color = SentinelMuted)
                    }
                    d.tickets.forEach { ticket ->
                        ItemRow(
                            ticket.title,
                            listOfNotNull(
                                ticket.status.takeIf { it.isNotBlank() },
                                ticket.priority.takeIf { it.isNotBlank() },
                                "Ticket #${ticket.id}",
                            ).joinToString(" · "),
                        ) { nav.navigate("ticket/${ticket.id}") }
                    }
                }

                MirrorCard("Case activity", d.activity.size) {
                    if (d.activity.isEmpty()) {
                        Text("No activity recorded for this case yet.", color = SentinelMuted)
                    }
                    d.activity.forEach { row ->
                        Text(
                            "${row.action} — ${row.detail ?: ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                        Text(formatEpochMs(row.createdAt), color = SentinelMuted)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun CaseFormScreen(viewModel: AppViewModel, nav: NavController, caseId: Int?) {
    val scope = rememberCoroutineScope()
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var roster by remember { mutableStateOf<List<User>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var clientId by remember { mutableStateOf<Int?>(null) }
    var title by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Open") }
    var originalStatus by remember { mutableStateOf("Open") }
    var assignedUserId by remember { mutableStateOf<Int?>(null) }
    var priority by remember { mutableStateOf("Normal") }
    var category by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var adMaterial by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(caseId) {
        try {
            clients = viewModel.api.clients().items
            roster = try {
                viewModel.api.roster()
            } catch (e: Exception) {
                emptyList()
            }
            if (caseId != null) {
                val d = viewModel.api.caseDetail(caseId)
                val c = d.case
                clientId = c.clientId
                title = c.title
                status = c.status
                originalStatus = c.status
                assignedUserId = c.assignedUserId
                priority = c.priority
                category = c.category
                dueDate = c.dueDate ?: ""
                notes = c.notes
                adMaterial = c.adMaterial
            }
            loaded = true
        } catch (e: Exception) {
            loadError = plainError(e, "The form could not be loaded. Go back and try again.")
        }
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        MirrorHeader(if (caseId == null) "New case" else "Edit case")
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
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ChoiceMenu("Status", status, MirrorVocab.stageOptions(originalStatus)) { status = it }
                    ChoiceMenu("Priority", priority, MirrorVocab.PRIORITIES) { priority = it }
                }
                Spacer(Modifier.height(12.dp))
                val assigneeOptions = listOf("Unassigned") + roster.map { it.displayName }
                ChoiceMenu(
                    "Assigned to",
                    assigneeOptions[
                        if (assignedUserId == null) 0
                        else roster.indexOfFirst { it.id == assignedUserId }.let { if (it < 0) 0 else it + 1 }
                    ],
                    assigneeOptions,
                ) { picked ->
                    assignedUserId = if (picked == "Unassigned") null
                    else roster.firstOrNull { it.displayName == picked }?.id
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MirrorVocab.CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due date (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = adMaterial, onCheckedChange = { adMaterial = it })
                    Text(
                        "Ad material — flag this job as a candidate for a post or ad " +
                            "(family jobs especially; flagging never publishes anything)"
                    )
                }
                if (saveError != null) {
                    Text(saveError!!, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = !busy,
                        onClick = {
                            if (title.isBlank()) {
                                titleError = "A case title is required."
                            } else {
                                busy = true
                                scope.launch {
                                    try {
                                        val edit = CaseFullEdit(
                                            clientId = clientId,
                                            assignedUserId = assignedUserId,
                                            title = title.trim(),
                                            status = status,
                                            priority = priority,
                                            category = category,
                                            dueDate = dueDate.ifBlank { null },
                                            notes = notes,
                                            adMaterial = adMaterial,
                                        )
                                        if (caseId == null) {
                                            val created = viewModel.api.createCase(edit)
                                            nav.popBackStack()
                                            nav.navigate("case/${created.id}")
                                        } else {
                                            viewModel.api.patchCase(caseId, edit)
                                            nav.popBackStack()
                                        }
                                    } catch (e: Exception) {
                                        busy = false
                                        saveError = plainError(e, "The case could not be saved. Try again.")
                                    }
                                }
                            }
                        },
                    ) { Text(if (busy) "Saving…" else "Save case") }
                    OutlinedButton(onClick = { nav.popBackStack() }) { Text("Cancel") }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

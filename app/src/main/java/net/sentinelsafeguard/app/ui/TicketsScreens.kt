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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.data.api.CaseItem
import net.sentinelsafeguard.app.data.api.Client
import net.sentinelsafeguard.app.data.api.PortalMessagesResponse
import net.sentinelsafeguard.app.data.api.Ticket
import net.sentinelsafeguard.app.data.api.TicketCreateRequest
import net.sentinelsafeguard.app.data.api.TicketDetailResponse
import net.sentinelsafeguard.app.data.api.TicketFullEdit
import net.sentinelsafeguard.app.data.api.TicketPatchRequest
import net.sentinelsafeguard.app.data.api.User
import net.sentinelsafeguard.app.data.api.applySavedView
import net.sentinelsafeguard.app.data.api.deleteSavedView
import net.sentinelsafeguard.app.data.api.saveSavedView
import net.sentinelsafeguard.app.data.api.savedViews
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * Tickets (mirror spec §3.3): web's list shell (header, subtitle,
 * stat cards, Board | Directory), board columns with the web
 * card's own Status select + steppers, the B3-backed directory,
 * the full profile envelope (case + activity rendered, never
 * dropped), and the full create/edit form. Phase 3 adds the
 * Ticket Ops sections (TicketOps.kt), saved views for tickets,
 * and merge / split inside the profile.
 */

private fun assigneeName(ticket: Ticket, roster: List<User>): String =
    roster.firstOrNull { it.id == ticket.assignedUserId }?.displayName ?: "Unassigned"

private fun dueLabel(ticket: Ticket, today: String): String? {
    val due = ticket.dueDate?.takeIf { it.isNotBlank() } ?: return null
    val overdue = MirrorVocab.isOverdue(due, today, MirrorVocab.isDone(ticket.status))
    return "Due $due" + if (overdue) " · OVERDUE" else ""
}

@Composable
fun TicketsScreen(viewModel: AppViewModel, nav: NavController) {
    val scope = rememberCoroutineScope()
    var all by remember { mutableStateOf<List<Ticket>?>(null) }
    var total by remember { mutableStateOf(0) }
    var roster by remember { mutableStateOf<List<User>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var directoryMode by remember { mutableStateOf(false) }
    val todayStr = remember { java.time.LocalDate.now().toString() }

    LaunchedEffect(reloadKey) {
        error = null
        try {
            val page = viewModel.api.ticketsFiltered()
            all = page.items
            total = page.total
            roster = try {
                viewModel.api.roster()
            } catch (e: Exception) {
                emptyList()
            }
        } catch (e: Exception) {
            error = plainError(e, "Tickets could not be loaded. Check your connection and try again.")
        }
    }

    fun moveStage(ticket: Ticket, stage: String) {
        scope.launch {
            try {
                viewModel.api.patchTicket(ticket.id, TicketPatchRequest(status = stage))
                reloadKey++
            } catch (e: Exception) {
                error = plainError(e, "That stage move could not be saved. Try again.")
            }
        }
    }

    Column {
        MirrorHeader(
            "Tickets",
            "The internal work queue — board, directory, and full ticket files.",
        ) {
            Button(onClick = { nav.navigate("ticketNew") }) { Text("+ New ticket") }
        }
        when {
            error != null -> ErrorBox(error!!) { reloadKey++ }
            all == null -> SkeletonList(6)
            else -> {
                val tickets = all!!
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    StatCard(
                        "Open tickets",
                        tickets.count { !MirrorVocab.isDone(it.status) }.toString(),
                        Modifier.weight(1f),
                    )
                    StatCard(
                        "In progress",
                        tickets.count { it.status == "In Progress" }.toString(),
                        Modifier.weight(1f),
                    )
                    StatCard(
                        "Overdue",
                        tickets.count { MirrorVocab.isOverdue(it.dueDate, todayStr, MirrorVocab.isDone(it.status)) }.toString(),
                        Modifier.weight(1f),
                    )
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    StatCard(
                        "Unassigned",
                        tickets.count { it.assignedUserId == null }.toString(),
                        Modifier.weight(1f),
                    )
                    StatCard(
                        "No case",
                        tickets.count { it.caseId == null }.toString(),
                        Modifier.weight(1f),
                    )
                    StatCard(
                        "Closed",
                        tickets.count { it.status == "Closed" }.toString(),
                        Modifier.weight(1f),
                    )
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
                        "$total ticket(s)",
                        color = SentinelMuted,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                if (directoryMode) {
                    TicketDirectory(
                        viewModel = viewModel,
                        base = tickets,
                        roster = roster,
                        todayStr = todayStr,
                        onOpen = { nav.navigate("ticket/${it.id}") },
                        onCase = { cid -> nav.navigate("case/$cid") },
                        onNew = { nav.navigate("ticketNew") },
                    )
                } else {
                    LazyColumn {
                        MirrorVocab.groupBoard(tickets).forEach { (stage, cards) ->
                            item {
                                SectionHeader(stage, cards.size)
                            }
                            if (cards.isEmpty()) {
                                item {
                                    Text(
                                        "No tickets in this stage",
                                        color = SentinelMuted,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    )
                                }
                            }
                            items(cards) { ticket ->
                                TicketCard(
                                    ticket = ticket,
                                    roster = roster,
                                    todayStr = todayStr,
                                    onOpen = { nav.navigate("ticket/${ticket.id}") },
                                    onStage = { moveStage(ticket, it) },
                                    onCase = ticket.caseId?.let { cid ->
                                        { nav.navigate("case/$cid") }
                                    },
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
private fun TicketCard(
    ticket: Ticket,
    roster: List<User>,
    todayStr: String,
    onOpen: () -> Unit,
    onStage: (String) -> Unit,
    onCase: (() -> Unit)? = null,
) {
    androidx.compose.material3.Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = net.sentinelsafeguard.app.ui.theme.SentinelSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, net.sentinelsafeguard.app.ui.theme.SentinelOutline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            TextButton(onClick = onOpen) {
                Text(ticket.title, style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                listOfNotNull(
                    ticket.clientName?.takeIf { it.isNotBlank() } ?: "No client",
                    "Ticket #${ticket.id}",
                ).joinToString(" · "),
                color = SentinelMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MirrorChip(ticket.priority)
                if (ticket.source.equals("website", ignoreCase = true)) {
                    MirrorChip(sourceLabel(ticket.source))
                }
                if (ticket.adMaterial) MirrorChip("★ Ad material")
            }
            if (ticket.category.isNotBlank()) {
                Text(ticket.category, style = MaterialTheme.typography.bodyMedium)
            }
            if (ticket.caseId != null && onCase != null) {
                TextButton(onClick = onCase) {
                    Text(
                        "Case #${ticket.caseId}" +
                            (ticket.caseTitle?.takeIf { it.isNotBlank() }
                                ?.let { " \u00b7 $it" } ?: ""),
                    )
                }
            } else {
                Text(
                    ticket.caseId?.let { cid ->
                        "Case #$cid" + (ticket.caseTitle?.takeIf { it.isNotBlank() }
                            ?.let { " \u00b7 $it" } ?: "")
                    } ?: "No case",
                    color = SentinelMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                "Assignee ${assigneeName(ticket, roster)}",
                color = SentinelMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            dueLabel(ticket, todayStr)?.let {
                Text(it, color = SentinelMuted, style = MaterialTheme.typography.bodyMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChoiceMenu("Status", ticket.status, MirrorVocab.stageOptions(ticket.status), onStage)
                Spacer(Modifier.weight(1f))
                StageStepper(
                    current = ticket.status,
                    onStep = onStage,
                    onOpen = onOpen,
                    openLabel = "Open ticket",
                )
            }
        }
    }
}

/** Saved views for tickets (web's bar, Phase 3 backend). */
@Composable
private fun TicketViewsBar(viewModel: AppViewModel) {
    val scope = rememberCoroutineScope()
    var views by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var name by remember { mutableStateOf("") }
    var statusChoice by remember { mutableStateOf("Any status") }
    var error by remember { mutableStateOf<String?>(null) }
    var applied by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var appliedLabel by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            try {
                views = Phase3Logic.arr(viewModel.api.savedViews("tickets"), "views")
            } catch (e: Exception) {
                error = plainError(e, "Saved views could not be loaded. Try again.")
            }
        }
    }
    LaunchedEffect(Unit) { reload() }

    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text("Saved views", style = MaterialTheme.typography.titleSmall)
        views.forEach { view ->
            val vid = Phase3Logic.int(view, "id")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Phase3Logic.str(view, "name"), Modifier.weight(1f))
                TextButton(onClick = {
                    if (vid != null) scope.launch {
                        try {
                            val out = viewModel.api.applySavedView(vid)
                            applied = Phase3Logic.arr(out, "rows")
                            appliedLabel = Phase3Logic.str(view, "name")
                        } catch (e: Exception) {
                            error = plainError(e, "The view could not be applied. Try again.")
                        }
                    }
                }) { Text("Apply") }
                if (vid != null) {
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                viewModel.api.deleteSavedView(vid); reload()
                            } catch (e: Exception) {
                                error = plainError(e, "The view could not be deleted. Try again.")
                            }
                        }
                    }) { Text("Delete") }
                }
            }
        }
        if (appliedLabel != null) {
            Text(
                "Applied “${appliedLabel}” — ${applied.size} matching ticket(s)",
                color = SentinelMuted,
            )
            applied.take(10).forEach { row ->
                Text(
                    "· ${Phase3Logic.str(row, "title")} (${Phase3Logic.str(row, "status")})",
                    color = SentinelMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        TextFieldRow("Name for a new view", name, { name = it })
        ChoiceMenu(
            "Status in the view",
            statusChoice,
            listOf("Any status") + MirrorVocab.TICKET_STAGES,
        ) { statusChoice = it }
        Button(
            enabled = name.isNotBlank(),
            onClick = {
                scope.launch {
                    try {
                        val criteria = buildJsonObject {
                            put("status", if (statusChoice == "Any status") "" else statusChoice)
                        }
                        viewModel.api.saveSavedView("tickets", name.trim(), criteria)
                        name = ""; reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The view could not be saved. Try again.")
                    }
                }
            },
        ) { Text("Save view") }
        ActionErrorText(error)
    }
}

@Composable
private fun TicketDirectory(
    viewModel: AppViewModel,
    base: List<Ticket>,
    roster: List<User>,
    todayStr: String,
    onOpen: (Ticket) -> Unit,
    onCase: (Int) -> Unit,
    onNew: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(MirrorVocab.TICKET_SORTS[0]) }
    var stage by remember { mutableStateOf<String?>(null) }
    var priority by remember { mutableStateOf<String?>(null) }
    var overdueOnly by remember { mutableStateOf(false) }
    var unassignedOnly by remember { mutableStateOf(false) }
    var noCaseOnly by remember { mutableStateOf(false) }
    var adOnly by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<Ticket>?>(null) }
    var dirError by remember { mutableStateOf<String?>(null) }

    fun reset() {
        q = ""; sort = MirrorVocab.TICKET_SORTS[0]; stage = null; priority = null
        overdueOnly = false; unassignedOnly = false; noCaseOnly = false; adOnly = false
    }

    LaunchedEffect(q, sort, stage, priority, overdueOnly, unassignedOnly, noCaseOnly, adOnly) {
        dirError = null
        try {
            val params = MirrorVocab.ticketDirectoryParams(
                q, stage, priority, overdueOnly, unassignedOnly, noCaseOnly, adOnly, sort,
            )
            results = viewModel.api.ticketsFiltered(
                q = params["q"],
                status = params["status"],
                priority = params["priority"],
                overdue = params["overdue"] != null,
                unassigned = params["unassigned"] != null,
                noCase = params["noCase"] != null,
                adMaterial = params["adMaterial"] != null,
                sort = params["sort"],
            ).items
        } catch (e: Exception) {
            dirError = plainError(e, "The ticket search could not be run. Try again.")
        }
    }

    LazyColumn {
        item {
            OutlinedTextField(
                value = q,
                onValueChange = { q = it },
                label = { Text("Search tickets") },
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
                ChoiceMenu("Sort tickets", sort, MirrorVocab.TICKET_SORTS) { sort = it }
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
                    MirrorVocab.TICKET_STAGES.map { s -> s to "$s ${base.count { it.status == s }}" },
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
                    selected = noCaseOnly,
                    onClick = { noCaseOnly = !noCaseOnly },
                    label = { Text("No case ${base.count { it.caseId == null }}") },
                )
                FilterChip(
                    selected = adOnly,
                    onClick = { adOnly = !adOnly },
                    label = { Text("★ Ad material ${base.count { it.adMaterial }}") },
                )
            }
            TicketViewsBar(viewModel)
        }
        when {
            dirError != null -> item { ErrorBox(dirError!!) }
            results == null -> item { SkeletonList(4) }
            base.isEmpty() -> item {
                EmptyBox("No tickets yet", "The work queue is empty.")
                Button(
                    onClick = onNew,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) { Text("+ New ticket") }
            }
            results!!.isEmpty() -> item {
                EmptyBox("No matches", "No tickets match the current search and filters.")
                OutlinedButton(
                    onClick = { reset() },
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) { Text("Reset search & filters") }
            }
            else -> items(results!!) { ticket ->
                TicketCard(
                    ticket = ticket,
                    roster = roster,
                    todayStr = todayStr,
                    onOpen = { onOpen(ticket) },
                    onCase = ticket.caseId?.let { cid ->
                        { onCase(cid) }
                    },
                    onStage = { stageName ->
                        scope.launch {
                            try {
                                viewModel.api.patchTicket(ticket.id, TicketPatchRequest(status = stageName))
                                results = results!!.map {
                                    if (it.id == ticket.id) it.copy(status = stageName) else it
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
fun TicketProfileScreen(viewModel: AppViewModel, nav: NavController, ticketId: Int) {
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<TicketDetailResponse?>(null) }
    var roster by remember { mutableStateOf<List<User>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var noteText by remember { mutableStateOf("") }
    var portal by remember { mutableStateOf<PortalMessagesResponse?>(null) }
    var portalLoaded by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var reviewSent by remember { mutableStateOf(false) }
    val todayStr = remember { java.time.LocalDate.now().toString() }
    val myRole = viewModel.me?.user?.role

    LaunchedEffect(ticketId, reloadKey) {
        error = null
        try {
            detail = viewModel.api.ticketDetail(ticketId)
            roster = try {
                viewModel.api.roster()
            } catch (e: Exception) {
                emptyList()
            }
        } catch (e: Exception) {
            error = plainError(e, "This ticket could not be loaded. Go back and try again.")
        }
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        TextButton(onClick = { nav.popBackStack() }) { Text("← Back to board") }
        when {
            error != null -> ErrorBox(error!!) { reloadKey++ }
            detail == null -> SkeletonList(4)
            else -> {
                val d = detail!!
                val t = d.ticket
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(t.title, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Ticket #${t.id} · " + (
                            d.client?.legalName
                                ?: if (t.clientId == null) "Website submission" else "Client #${t.clientId}"
                            ),
                        color = SentinelMuted,
                    )
                    Row(
                        Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        MirrorChip(t.status)
                        MirrorChip(t.priority)
                        if (t.source.isNotBlank()) MirrorChip(sourceLabel(t.source))
                        if (t.adMaterial) MirrorChip("★ Ad material")
                    }
                    if (actionError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(actionError!!, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ChoiceMenu("Status", t.status, MirrorVocab.stageOptions(t.status)) { picked ->
                            scope.launch {
                                try {
                                    viewModel.api.patchTicket(t.id, TicketPatchRequest(status = picked))
                                    reloadKey++
                                } catch (e: Exception) {
                                    actionError = plainError(e, "That change could not be saved. Try again.")
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { nav.navigate("ticketEdit/${t.id}") }) { Text("Edit ticket") }
                        OutlinedButton(onClick = {
                            scope.launch {
                                try {
                                    viewModel.api.patchTicket(
                                        t.id, TicketPatchRequest(adMaterial = !t.adMaterial)
                                    )
                                    reloadKey++
                                } catch (e: Exception) {
                                    actionError = plainError(e, "That change could not be saved. Try again.")
                                }
                            }
                        }) {
                            Text(if (t.adMaterial) "★ Ad material — on" else "☆ Flag as ad material")
                        }
                        OutlinedButton(onClick = {
                            scope.launch {
                                try {
                                    viewModel.api.archiveTicket(t.id)
                                    nav.popBackStack()
                                } catch (e: Exception) {
                                    actionError = plainError(e, "The ticket could not be archived. Try again.")
                                }
                            }
                        }) { Text("Archive") }
                    }
                }

                MirrorCard("Ticket details") {
                    DetailLine("Status", t.status)
                    DetailLine("Priority", t.priority)
                    DetailLine("Category", t.category)
                    DetailLine("Assignee", assigneeName(t, roster))
                    DetailLine("Source", t.source)
                    if (t.source.equals("website", ignoreCase = true)) {
                        DetailLine("Website ticket", t.sourceRef?.let { "#$it" })
                    }
                    DetailLine("Created", if (t.createdAt > 0) formatEpochMs(t.createdAt) else null)
                    DetailLine(
                        "Due",
                        t.dueDate?.takeIf { it.isNotBlank() }?.let { due ->
                            due + if (MirrorVocab.isOverdue(due, todayStr, MirrorVocab.isDone(t.status))) " · OVERDUE" else ""
                        },
                    )
                    DetailLine("Closed", t.closedDate.takeIf { it.isNotBlank() })
                    DetailLine("Last updated", if (t.updatedAt > 0) formatEpochMs(t.updatedAt) else null)
                }

                MirrorCard("Client") {
                    if (d.client == null) {
                        Text(
                            "Filed from the public website — no client record. The reporter's " +
                                "name and email are in the notes below; attach a client any " +
                                "time from Edit ticket."
                        )
                    } else {
                        val c = d.client
                        DetailLine("Client", c.legalName)
                        DetailLine("Contact", c.contactName)
                        DetailLine("Email", c.email)
                        DetailLine("Phone", c.phone)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { nav.navigate("client/${c.id}") }) {
                            Text("Open client profile")
                        }
                    }
                }

                if (t.status == "Closed" && myRole == "OWNER") {
                    MirrorCard("Client review") {
                        if (reviewSent) {
                            Text("Review request sent.")
                        } else {
                            Button(onClick = {
                                scope.launch {
                                    try {
                                        viewModel.api.requestReview(t.id)
                                        reviewSent = true
                                    } catch (e: Exception) {
                                        actionError = plainError(e, "The review request could not be sent. Try again.")
                                    }
                                }
                            }) { Text("Request review") }
                        }
                    }
                }

                MirrorCard("Portal messages") {
                    if (!portalLoaded) {
                        OutlinedButton(onClick = {
                            scope.launch {
                                try {
                                    portal = viewModel.api.portalMessages(t.id)
                                    portalLoaded = true
                                } catch (e: Exception) {
                                    actionError = plainError(e, "The portal thread could not be loaded. Try again.")
                                }
                            }
                        }) { Text("Load portal thread") }
                    } else {
                        val thread = portal
                        if (thread == null || thread.messages.isEmpty()) {
                            Text("No portal messages on this ticket yet.", color = SentinelMuted)
                        } else {
                            thread.messages.forEach { msg ->
                                Text(
                                    "${msg.senderName} — ${msg.body}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 2.dp),
                                )
                                Text(formatEpochMs(msg.createdAt), color = SentinelMuted)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            label = { Text("Reply") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                        )
                        Button(
                            enabled = replyText.isNotBlank(),
                            onClick = {
                                scope.launch {
                                    try {
                                        viewModel.api.postPortalMessage(t.id, replyText.trim())
                                        replyText = ""
                                        portal = viewModel.api.portalMessages(t.id)
                                    } catch (e: Exception) {
                                        actionError = plainError(e, "The reply could not be sent. Try again.")
                                    }
                                }
                            },
                        ) { Text("Send reply") }
                    }
                }

                MirrorCard("Notes") {
                    Text(t.notes.ifBlank { "No notes on this ticket." })
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Add a note") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    Button(
                        enabled = noteText.isNotBlank(),
                        onClick = {
                            scope.launch {
                                try {
                                    viewModel.api.addTicketNote(t.id, noteText.trim())
                                    noteText = ""
                                    reloadKey++
                                } catch (e: Exception) {
                                    actionError = plainError(e, "The note could not be saved. Try again.")
                                }
                            }
                        },
                    ) { Text("Add note") }
                }

                MirrorCard("Case") {
                    val linked = d.case
                    if (linked == null) {
                        Text("No case linked to this ticket.")
                        Text("Link a case in Edit", color = SentinelMuted)
                    } else {
                        DetailLine("Case", "#${linked.id}")
                        DetailLine("Title", linked.title)
                        DetailLine("Status", linked.status)
                        DetailLine("Priority", linked.priority)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { nav.navigate("case/${linked.id}") }) {
                            Text("Open case profile")
                        }
                    }
                }

                TicketRunbookPanel(viewModel, t.id)

                TicketSignOffCard(viewModel, t.id, t.clientId)

                TicketOpsSections(viewModel, nav, t)

                MirrorCard("Ticket activity", d.activity.size) {
                    if (d.activity.isEmpty()) {
                        Text("No activity recorded for this ticket yet.", color = SentinelMuted)
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
fun TicketFormScreen(viewModel: AppViewModel, nav: NavController, ticketId: Int?) {
    val scope = rememberCoroutineScope()
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var roster by remember { mutableStateOf<List<User>>(emptyList()) }
    var cases by remember { mutableStateOf<List<CaseItem>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var clientId by remember { mutableStateOf<Int?>(null) }
    var title by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("New") }
    var originalStatus by remember { mutableStateOf("New") }
    var assignedUserId by remember { mutableStateOf<Int?>(null) }
    var priority by remember { mutableStateOf("Normal") }
    var category by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var caseId by remember { mutableStateOf<Int?>(null) }
    var notes by remember { mutableStateOf("") }
    var adMaterial by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(ticketId) {
        try {
            clients = viewModel.api.clients().items
            roster = try {
                viewModel.api.roster()
            } catch (e: Exception) {
                emptyList()
            }
            if (ticketId != null) {
                val d = viewModel.api.ticketDetail(ticketId)
                val t = d.ticket
                clientId = t.clientId
                title = t.title
                status = t.status
                originalStatus = t.status
                assignedUserId = t.assignedUserId
                priority = t.priority
                category = t.category
                dueDate = t.dueDate ?: ""
                caseId = t.caseId
                notes = t.notes
                adMaterial = t.adMaterial
                if (t.clientId != null) {
                    cases = try {
                        viewModel.api.clientDetail(t.clientId).cases
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
        MirrorHeader(if (ticketId == null) "New ticket" else "Edit ticket")
        when {
            loadError != null -> ErrorBox(loadError!!)
            !loaded -> SkeletonList(4)
            else -> Column(Modifier.padding(horizontal = 16.dp)) {
                val clientOptions = listOf("No client — website submission") +
                    clients.map { it.legalName }
                ChoiceMenu(
                    "Client",
                    clientOptions[
                        if (clientId == null) 0
                        else clients.indexOfFirst { it.id == clientId }.let { if (it < 0) 0 else it + 1 }
                    ],
                    clientOptions,
                ) { picked ->
                    clientId = if (picked == "No client — website submission") null
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
                    MirrorVocab.CATEGORIES.forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(c) },
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
                                titleError = "Title is required."
                            } else {
                            busy = true
                            scope.launch {
                                try {
                                    if (ticketId == null) {
                                        val created = viewModel.api.createTicket(
                                            TicketCreateRequest(
                                                title = title.trim(),
                                                notes = notes,
                                                priority = priority,
                                                clientId = clientId,
                                                caseId = caseId,
                                                assignedUserId = assignedUserId,
                                                category = category,
                                                status = status,
                                                dueDate = dueDate.ifBlank { null },
                                            )
                                        )
                                        if (adMaterial) {
                                            viewModel.api.patchTicket(
                                                created.id,
                                                TicketPatchRequest(adMaterial = true),
                                            )
                                        }
                                        nav.popBackStack()
                                        nav.navigate("ticket/${created.id}")
                                    } else {
                                        viewModel.api.patchTicketFull(
                                            ticketId,
                                            TicketFullEdit(
                                                title = title.trim(),
                                                status = status,
                                                priority = priority,
                                                assignedUserId = assignedUserId,
                                                category = category,
                                                dueDate = dueDate.ifBlank { null },
                                                clientId = clientId,
                                                caseId = caseId,
                                                adMaterial = adMaterial,
                                                notes = notes,
                                            ),
                                        )
                                        nav.popBackStack()
                                    }
                                } catch (e: Exception) {
                                    busy = false
                                    saveError = plainError(e, "The ticket could not be saved. Try again.")
                                }
                            }
                            }
                        },
                    ) { Text(if (busy) "Saving…" else "Save ticket") }
                    OutlinedButton(onClick = { nav.popBackStack() }) { Text("Cancel") }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

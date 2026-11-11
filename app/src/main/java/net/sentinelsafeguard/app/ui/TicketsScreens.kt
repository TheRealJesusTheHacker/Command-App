package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import net.sentinelsafeguard.app.data.OfflineLogic
import net.sentinelsafeguard.app.data.OfflineQueue
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
import net.sentinelsafeguard.app.ui.theme.SentinelAmber
import net.sentinelsafeguard.app.ui.theme.SentinelDanger
import net.sentinelsafeguard.app.ui.theme.SentinelGreen
import net.sentinelsafeguard.app.ui.theme.SentinelGreenDim
import net.sentinelsafeguard.app.ui.theme.SentinelMuted
import net.sentinelsafeguard.app.ui.theme.SentinelSurfaceHigh
import net.sentinelsafeguard.app.ui.theme.SentinelSurfaceLow

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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TicketsScreen(viewModel: AppViewModel, nav: NavController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // AUDIT-171 B-08: writes queued offline are real, durable
    // state — the list says so instead of letting a queued
    // ticket look lost.
    var queuedCount by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        queuedCount = OfflineQueue(context).pending().size
    }
    var all by remember { mutableStateOf<List<Ticket>?>(null) }
    var total by remember { mutableStateOf(0) }
    var roster by remember { mutableStateOf<List<User>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var banner by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    var directoryMode by remember { mutableStateOf(false) }
    val todayStr = remember { java.time.LocalDate.now().toString() }

    LaunchedEffect(reloadKey) {
        // hunt16: a failed RELOAD keeps the loaded list on screen
        // and reports as a banner; only a failed FIRST load is a
        // full-screen error.
        try {
            val page = viewModel.api.ticketsFiltered()
            all = page.items
            total = page.total
            error = null
            roster = try {
                viewModel.api.roster()
            } catch (e: Exception) {
                emptyList()
            }
        } catch (e: Exception) {
            val msg = plainError(e, "Tickets could not be loaded. Check your connection and try again.")
            if (all == null) error = msg else banner = msg
        } finally {
            refreshing = false
        }
    }

    fun assignToMe(ticket: Ticket) {
        // The same assign-to-me the Today screen uses
        // (TicketPatchRequest.assignToMe, resolved server-side
        // to the signed-in account) - no new backend call.
        scope.launch {
            try {
                viewModel.api.patchTicket(ticket.id, TicketPatchRequest(assignToMe = true))
                reloadKey++
            } catch (e: Exception) {
                banner = plainError(e, "That could not be saved. Try again.")
            }
        }
    }

    fun moveStage(ticket: Ticket, stage: String) {
        // hunt16: optimistic move — the card steps immediately;
        // on failure it rolls back and the banner says so plainly.
        val previous = all
        all = previous?.map { if (it.id == ticket.id) it.copy(status = stage) else it }
        scope.launch {
            try {
                viewModel.api.patchTicket(ticket.id, TicketPatchRequest(status = stage))
                reloadKey++
            } catch (e: Exception) {
                all = previous
                banner = plainError(e, "That stage move could not be saved — the ticket was put back. Try again.")
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
        if (queuedCount > 0) {
            Text(
                "$queuedCount ticket change(s) saved on this phone — " +
                    "they send themselves when the connection returns.",
                color = SentinelAmber,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        // Lane B / scope generalization (1.4.0): the queue's
        // distinct tools as a deck - Board and Directory are
        // this screen's own modes; New ticket is its route.
        ToolDeck(
            entries = listOf(
                ToolDeckEntry("Board", selected = !directoryMode) { directoryMode = false },
                ToolDeckEntry("Directory", selected = directoryMode) { directoryMode = true },
                ToolDeckEntry("+ New ticket") { nav.navigate("ticketNew") },
            ),
        )
        when {
            error != null && all == null -> ErrorBox(error!!) { reloadKey++ }
            all == null -> SkeletonList(6)
            else -> {
                val tickets = all!!
                if (banner != null) {
                    ErrorBanner(banner!!, onRetry = { banner = null; reloadKey++ }, onDismiss = { banner = null })
                }
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
                Text(
                    "$total ticket(s)",
                    color = SentinelMuted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
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
                    // hunt16: pull-to-refresh on the board list —
                    // web's mental model for a work queue.
                    val pullState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
                    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                        isRefreshing = refreshing,
                        onRefresh = { refreshing = true; reloadKey++ },
                        state = pullState,
                        indicator = { DampedPullIndicator(pullState, refreshing, Modifier.align(Alignment.TopCenter)) },
                    ) {
                    LazyColumn {
                        MirrorVocab.groupBoard(tickets).forEach { (stage, cards) ->
                            item {
                                // hunt18: the stage section sits in a
                                // SurfaceLow well — same header, same
                                // cards, same order.
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(SentinelSurfaceLow)
                                        .padding(vertical = 4.dp),
                                ) {
                                    SectionHeader(stage, cards.size)
                                    if (cards.isEmpty()) {
                                        Text(
                                            "No tickets in this stage",
                                            color = SentinelMuted,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                        )
                                    }
                                    cards.forEach { ticket ->
                                        SwipeTicketActions(
                                            ticket = ticket,
                                            onAssign = { assignToMe(ticket) },
                                            onAdvance = MirrorVocab.stepStage(ticket.status, +1)?.let { next ->
                                                { moveStage(ticket, next) }
                                            },
                                        ) {
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
                                }
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
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
    entranceIndex: Int? = null,
) {
    androidx.compose.material3.Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, net.sentinelsafeguard.app.ui.theme.SentinelOutline
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .then(if (entranceIndex != null) Modifier.listEntrance(entranceIndex) else Modifier)
            .background(sentinelCardBrush(), RoundedCornerShape(16.dp)),
    ) {
        // hunt18: a 3dp left spine colored by the priority the card
        // already shows as a chip (the chip stays): Critical/High
        // danger, Medium / On Hold amber, else outline-green.
        val spineColor = when {
            ticket.priority == "Critical" || ticket.priority == "High" -> SentinelDanger
            ticket.priority == "Medium" ||
                ticket.status.contains("Hold", ignoreCase = true) -> SentinelAmber
            else -> SentinelGreenDim
        }
        Row(Modifier.height(IntrinsicSize.Max)) {
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(spineColor),
            )
        Column(Modifier.padding(16.dp)) {
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
        MorphSubmitButton(text = "Save view", enabled = name.isNotBlank()) {
            try {
                val criteria = buildJsonObject {
                    put("status", if (statusChoice == "Any status") "" else statusChoice)
                }
                viewModel.api.saveSavedView("tickets", name.trim(), criteria)
                name = ""; reload()
                true
            } catch (e: Exception) {
                error = plainError(e, "The view could not be saved. Try again.")
                false
            }
        }
        ActionErrorText(error)
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
    // hunt17: pull-to-refresh extends the board pattern to the
    // directory (Pass 3 decision): pulling re-runs the current
    // filtered query.
    var refreshKey by remember { mutableStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }

    fun reset() {
        q = ""; sort = MirrorVocab.TICKET_SORTS[0]; stage = null; priority = null
        overdueOnly = false; unassignedOnly = false; noCaseOnly = false; adOnly = false
    }

    LaunchedEffect(q, sort, stage, priority, overdueOnly, unassignedOnly, noCaseOnly, adOnly, refreshKey) {
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
        } finally {
            refreshing = false
        }
    }

    val pullState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { refreshing = true; refreshKey++ },
        state = pullState,
        indicator = { DampedPullIndicator(pullState, refreshing, Modifier.align(Alignment.TopCenter)) },
    ) {
    LazyColumn {
        item {
            OutlinedTextField(
                value = q,
                onValueChange = { q = it },
                label = { Text("Search tickets") },
                placeholder = { Text("Title, client, case #, or category") },
                singleLine = true,
                // M-18 (d) Search↔X: trailing affordance morphs
                // magnifier→clear while text is present.
                trailingIcon = {
                    SearchMorphIcon(
                        hasQuery = q.isNotEmpty(),
                        onClear = { q = "" },
                    )
                },
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
            else -> itemsIndexed(results!!) { index, ticket ->
                SwipeTicketActions(
                    ticket = ticket,
                    onAssign = {
                        scope.launch {
                            try {
                                viewModel.api.patchTicket(ticket.id, TicketPatchRequest(assignToMe = true))
                                results = results!!.map {
                                    if (it.id == ticket.id) {
                                        it.copy(assignedUserId = viewModel.me?.user?.id ?: it.assignedUserId)
                                    } else {
                                        it
                                    }
                                }
                            } catch (e: Exception) {
                                dirError = plainError(e, "That could not be saved. Try again.")
                            }
                        }
                    },
                    onAdvance = MirrorVocab.stepStage(ticket.status, +1)?.let { next ->
                        {
                            scope.launch {
                                try {
                                    viewModel.api.patchTicket(ticket.id, TicketPatchRequest(status = next))
                                    results = results!!.map {
                                        if (it.id == ticket.id) it.copy(status = next) else it
                                    }
                                } catch (e: Exception) {
                                    dirError = plainError(e, "That stage move could not be saved. Try again.")
                                }
                            }
                        }
                    },
                ) {
                    TicketCard(
                        entranceIndex = index,
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
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
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
    // M-18 (e) Send arc: bumped only on a successful send.
    var sendKey by remember { mutableStateOf(0) }
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
                                        sendKey++
                                        portal = viewModel.api.portalMessages(t.id)
                                    } catch (e: Exception) {
                                        actionError = plainError(e, "The reply could not be sent. Try again.")
                                    }
                                }
                            },
                        ) {
                            SendArcIcon(sendKey)
                            Spacer(Modifier.padding(start = 8.dp))
                            Text("Send reply")
                        }
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
                    MorphSubmitButton(text = "Add note", enabled = noteText.isNotBlank()) {
                        try {
                            viewModel.api.addTicketNote(t.id, noteText.trim())
                            noteText = ""
                            reloadKey++
                            true
                        } catch (e: Exception) {
                            actionError = plainError(e, "The note could not be saved. Try again.")
                            false
                        }
                    }
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

                // 1.6.1 (Feature Wave Item 23): the profile
                // payload's stage-ordered photo timeline, in
                // the server's served order (grouped by
                // stageLabel, never re-sorted). Absent/empty →
                // the profile renders exactly as before.
                if (d.photoTimeline.isNotEmpty()) {
                    MirrorCard("Photo timeline") {
                        PhotoTimelineGroups(viewModel, d.photoTimeline)
                    }
                }

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
    val context = LocalContext.current
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
    var dueError by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    // AUDIT-171 B-04: set when a create succeeded but the follow-up
    // ad-material patch failed — Save then retries only the patch.
    var createdTicketId by remember { mutableStateOf<Int?>(null) }
    // AUDIT-171 B-08: the loaded ticket's updatedAt (edit mode) and
    // a created ticket's updatedAt — the offline queue's conflict
    // base when a save has to be queued instead of sent.
    var loadedUpdatedAt by remember { mutableStateOf<Long?>(null) }
    var createdTicketUpdatedAt by remember { mutableStateOf<Long?>(null) }

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
                loadedUpdatedAt = t.updatedAt
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
                    MirrorVocab.clientOptions(clients)
                ChoiceMenu(
                    "Client",
                    clientOptions[
                        if (clientId == null) 0
                        else clients.indexOfFirst { it.id == clientId }.let { if (it < 0) 0 else it + 1 }
                    ],
                    clientOptions,
                ) { picked ->
                    clientId = if (picked == "No client — website submission") null
                    else MirrorVocab.clientIdAt(clients, clientOptions, picked, sentinelCount = 1)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; if (titleError != null) titleError = FieldValidators.required(it, "Title") },
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
                // AUDIT-171 B-07: name-keyed pickers silently chose
                // the FIRST namesake. Options are made unique (a
                // shared display name gets its id appended) and the
                // pick resolves by position, never by name match.
                val assigneeOptions = listOf("Unassigned") + roster.map { u ->
                    if (roster.count { it.displayName == u.displayName } > 1) {
                        "${u.displayName} (#${u.id})"
                    } else {
                        u.displayName
                    }
                }
                ChoiceMenu(
                    "Assigned to",
                    assigneeOptions[
                        if (assignedUserId == null) 0
                        else roster.indexOfFirst { it.id == assignedUserId }.let { if (it < 0) 0 else it + 1 }
                    ],
                    assigneeOptions,
                ) { picked ->
                    val idx = assigneeOptions.indexOf(picked)
                    assignedUserId = if (idx <= 0) null else roster.getOrNull(idx - 1)?.id
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
                DateFieldRow("Due date", dueDate) { dueDate = it; if (dueError != null) dueError = FieldValidators.dateIfPresent(it) }
                if (dueError != null) {
                    Text(dueError!!, color = MaterialTheme.colorScheme.error)
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
                    MorphSubmitButton(text = "Save ticket") {
                        titleError = FieldValidators.required(title, "Title")
                        dueError = FieldValidators.dateIfPresent(dueDate)
                        if (FieldValidators.firstError(titleError, dueError) != null) {
                            false
                        } else {
                            try {
                                if (ticketId == null) {
                                    // AUDIT-171 B-04: create and the
                                    // ad-material patch are separate
                                    // steps with separate truths. The
                                    // old single try reported a patch
                                    // failure as "not saved" — and a
                                    // retry then created a DUPLICATE.
                                    // Now a created id is remembered:
                                    // a retry re-runs only the patch.
                                    val created = createdTicketId?.let { existing ->
                                        // Retry path: the ticket already
                                        // exists; only the flag is owed.
                                        viewModel.api.patchTicket(
                                            existing,
                                            TicketPatchRequest(adMaterial = true),
                                        )
                                        nav.popBackStack()
                                        nav.navigate("ticket/$existing")
                                        return@MorphSubmitButton true
                                    } ?: viewModel.api.createTicket(
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
                                        try {
                                            viewModel.api.patchTicket(
                                                created.id,
                                                TicketPatchRequest(adMaterial = true),
                                            )
                                        } catch (patchFailure: Exception) {
                                            // AUDIT-171 B-08: no connection
                                            // for the flag — queue the
                                            // patch; the ticket itself is
                                            // safely created either way.
                                            if (OfflineLogic.shouldQueue(patchFailure)) {
                                                OfflineQueue(context).enqueue(
                                                    key = "ticket-patch-ad-${created.id}",
                                                    kind = "ticket_patch",
                                                    payload = buildJsonObject {
                                                        put("ticketId", created.id)
                                                        put("adMaterial", true)
                                                    },
                                                    baseUpdatedAt = created.updatedAt.toString(),
                                                )
                                                nav.popBackStack()
                                                nav.navigate("ticket/${created.id}")
                                                return@MorphSubmitButton true
                                            }
                                            createdTicketId = created.id
                                            createdTicketUpdatedAt = created.updatedAt
                                            saveError = "The ticket WAS saved " +
                                                "(#${created.id}) — only marking it " +
                                                "as ad material failed. Save again to " +
                                                "retry just that, or Cancel to leave it."
                                            return@MorphSubmitButton false
                                        }
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
                                true
                            } catch (e: Exception) {
                                // AUDIT-171 B-08: the server never
                                // answered — the write belongs in
                                // the offline queue, not in an
                                // error the user has to retype from.
                                // A server REFUSAL (ApiException)
                                // still lands as an error below.
                                if (OfflineLogic.shouldQueue(e)) {
                                    val queue = OfflineQueue(context)
                                    when {
                                        ticketId != null -> {
                                            val edit = TicketFullEdit(
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
                                            )
                                            queue.enqueue(
                                                // Stable key per ticket:
                                                // re-saving the same edit
                                                // offline replaces, never
                                                // duplicates.
                                                key = "ticket-patch-$ticketId",
                                                kind = "ticket_patch",
                                                payload = buildJsonObject {
                                                    put("ticketId", ticketId)
                                                    viewModel.api.ticketFullBody(edit).forEach { (k, v) -> put(k, v) }
                                                },
                                                baseUpdatedAt = loadedUpdatedAt?.toString(),
                                            )
                                            nav.popBackStack()
                                            return@MorphSubmitButton true
                                        }
                                        createdTicketId != null -> {
                                            queue.enqueue(
                                                key = "ticket-patch-ad-$createdTicketId",
                                                kind = "ticket_patch",
                                                payload = buildJsonObject {
                                                    put("ticketId", createdTicketId!!)
                                                    put("adMaterial", true)
                                                },
                                                baseUpdatedAt = createdTicketUpdatedAt?.toString(),
                                            )
                                            nav.popBackStack()
                                            nav.navigate("ticket/$createdTicketId")
                                            return@MorphSubmitButton true
                                        }
                                        else -> {
                                            queue.enqueue(
                                                key = "ticket-create-" +
                                                    java.util.UUID.randomUUID().toString(),
                                                kind = "ticket_create",
                                                payload = viewModel.api.ticketCreateBody(
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
                                                ),
                                            )
                                            nav.popBackStack()
                                            return@MorphSubmitButton true
                                        }
                                    }
                                }
                                saveError = plainError(e, "The ticket could not be saved. Try again.")
                                false
                            }
                        }
                    }
                    OutlinedButton(onClick = { nav.popBackStack() }) { Text("Cancel") }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}


/**
 * Lane B (1.4.0) M-10 for ticket rows: swipe right assigns
 * the ticket to me, swipe left advances its status one
 * stage - both are actions this file already performs
 * elsewhere (TicketPatchRequest.assignToMe from the Today
 * screen's pattern; MirrorVocab.stepStage behind the card's
 * own StageStepper). The row always settles back afterwards
 * (confirmValueChange returns false): the swipe is a quick
 * action trigger, never a dismissal, and the card's data
 * updates through the caller's own state. The threshold
 * pass fires the primary haptic.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SwipeTicketActions(
    ticket: Ticket,
    onAssign: () -> Unit,
    onAdvance: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val primaryHaptic = rememberPrimaryHaptic()
    val state = androidx.compose.material3.rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd -> {
                    primaryHaptic(); onAssign()
                }
                androidx.compose.material3.SwipeToDismissBoxValue.EndToStart -> {
                    if (onAdvance != null) {
                        primaryHaptic(); onAdvance()
                    }
                }
                else -> {}
            }
            false
        },
    )
    androidx.compose.material3.SwipeToDismissBox(
        state = state,
        enableDismissFromEndToStart = onAdvance != null,
        backgroundContent = {
            val direction = state.dismissDirection
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(
                        if (direction == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart) {
                            SentinelSurfaceHigh
                        } else {
                            SentinelGreen.copy(alpha = 0.18f)
                        },
                        RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = if (direction == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart) {
                    Alignment.CenterEnd
                } else {
                    Alignment.CenterStart
                },
            ) {
                Text(
                    if (direction == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart) {
                        MirrorVocab.stepStage(ticket.status, +1) ?: ticket.status
                    } else {
                        "Assign to me"
                    },
                    color = SentinelGreen,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    ) { content() }
}

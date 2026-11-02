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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import kotlinx.coroutines.launch
import net.sentinelsafeguard.app.data.api.Ticket
import net.sentinelsafeguard.app.data.api.TicketCreateRequest
import net.sentinelsafeguard.app.data.api.TicketPatchRequest
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

private val FILTERS = listOf("New", "Mine", "All open", "Done")
// The backend's own vocabularies (server TICKET_STAGES /
// _DASH_PRIORITIES) — the patch endpoint refuses anything else.
private val STATUSES = listOf("New", "Open", "In Progress", "On Hold", "Resolved", "Closed")
private val PRIORITIES = listOf("Low", "Normal", "High", "Critical")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketsScreen(viewModel: AppViewModel) {
    var selectedId by remember { mutableStateOf<Int?>(null) }
    if (selectedId != null) {
        TicketDetailScreen(viewModel, selectedId!!) { selectedId = null }
        return
    }

    var filter by remember { mutableStateOf("New") }
    var result by remember { mutableStateOf<net.sentinelsafeguard.app.data.api.Page<Ticket>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(filter, reloadKey) {
        error = null
        result = null
        try {
            result = when (filter) {
                "New" -> viewModel.api.tickets(status = "New")
                "Mine" -> viewModel.api.tickets(assignee = "me")
                "Done" -> viewModel.api.tickets(status = "Closed")
                else -> viewModel.api.tickets()
            }
        } catch (e: Exception) {
            error = plainError(e, "Tickets could not be loaded. Check your connection and try again.")
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New ticket")
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FILTERS.forEach { name ->
                    FilterChip(
                        selected = filter == name,
                        onClick = { filter = name },
                        label = { Text(name) },
                    )
                }
            }
            when {
                error != null -> ErrorBox(error!!) { reloadKey++ }
                result == null -> SkeletonList()
                result!!.items.isEmpty() -> EmptyBox(
                    "No tickets here",
                    when (filter) {
                        "New" -> "No new tickets are waiting."
                        "Done" -> "Nothing has been finished yet."
                        else -> "Nothing matches this list right now."
                    },
                )
                else -> LazyColumn {
                    item {
                        Text(
                            // The complete filtered set, with the
                            // backend's true total for this view.
                            "${result!!.total} " +
                                if (result!!.total == 1) "ticket" else "tickets",
                            color = SentinelMuted,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    items(result!!.items) { ticket ->
                        ItemRow(
                            title = ticket.title,
                            subtitle = listOfNotNull(
                                ticket.clientName?.takeIf { it.isNotBlank() },
                                ticket.status.takeIf { it.isNotBlank() },
                                if (ticket.createdAt > 0) formatEpochMs(ticket.createdAt) else null,
                            ).joinToString(" · "),
                            badge = ticket.source
                                .takeIf { it.isNotBlank() && it != "Internal" }
                                ?.let { sourceLabel(it) }
                                ?: ticket.priority.takeIf { it == "High" || it == "Critical" },
                        ) { selectedId = ticket.id }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateTicketDialog(
            viewModel = viewModel,
            onDismiss = { showCreate = false },
            onCreated = { showCreate = false; reloadKey++ },
        )
    }
}

/** Website tickets show their source badge in plain words (spec §2.1). */
fun sourceLabel(source: String): String = when (source.lowercase()) {
    "website", "report-issue", "report_issue" -> "From the website"
    "contact", "contact-form" -> "From the contact form"
    "email" -> "From email"
    "manual" -> "Added by hand"
    else -> source.replaceFirstChar { it.uppercase() }
}

@Composable
private fun CreateTicketDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onCreated: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Normal") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New ticket") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What needs doing?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Details") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                Spacer(Modifier.height(8.dp))
                ChoiceMenu("Priority", priority, PRIORITIES) { priority = it }
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy && title.isNotBlank(),
                onClick = {
                    busy = true
                    scope.launch {
                        try {
                            viewModel.api.createTicket(
                                TicketCreateRequest(title.trim(), message.trim(), priority)
                            )
                            onCreated()
                        } catch (e: Exception) {
                            busy = false
                            error = plainError(e, "The ticket could not be saved. Try again.")
                        }
                    }
                },
            ) { Text(if (busy) "Saving…" else "Save ticket") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun ChoiceMenu(
    label: String,
    value: String,
    options: List<String>,
    onPick: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label, color = SentinelMuted, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { open = true }) {
            Text(value.replaceFirstChar { it.uppercase() })
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.replaceFirstChar { it.uppercase() }) },
                    onClick = { onPick(option); open = false },
                )
            }
        }
    }
}

@Composable
fun TicketDetailScreen(viewModel: AppViewModel, ticketId: Int, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var ticket by remember { mutableStateOf<Ticket?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var noteText by remember { mutableStateOf("") }
    var actionError by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(ticketId, reloadKey) {
        try {
            ticket = viewModel.api.ticket(ticketId)
        } catch (e: Exception) {
            error = plainError(e, "This ticket could not be loaded. Go back and try again.")
        }
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        TextButton(onClick = onBack) { Text("‹ Back to tickets") }
        when {
            error != null -> ErrorBox(error!!) { reloadKey++ }
            ticket == null -> SkeletonList(3)
            else -> {
                val t = ticket!!
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(t.title, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        listOfNotNull(
                            t.source.takeIf { it.isNotBlank() }?.let { sourceLabel(it) },
                            if (t.createdAt > 0) formatEpochMs(t.createdAt) else null,
                        ).joinToString(" · "),
                        color = SentinelMuted,
                    )
                    if (!t.clientName.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Client: ${t.clientName}", color = SentinelMuted)
                    }

                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ChoiceMenu("Status", t.status, STATUSES) { picked ->
                            scope.launch {
                                try {
                                    ticket = viewModel.api.patchTicket(
                                        t.id, TicketPatchRequest(status = picked)
                                    )
                                } catch (e: Exception) {
                                    actionError = plainError(e, "That change could not be saved. Try again.")
                                }
                            }
                        }
                        ChoiceMenu("Priority", t.priority, PRIORITIES) { picked ->
                            scope.launch {
                                try {
                                    ticket = viewModel.api.patchTicket(
                                        t.id, TicketPatchRequest(priority = picked)
                                    )
                                } catch (e: Exception) {
                                    actionError = plainError(e, "That change could not be saved. Try again.")
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = {
                        scope.launch {
                            try {
                                ticket = viewModel.api.patchTicket(
                                    t.id, TicketPatchRequest(assignToMe = true)
                                )
                            } catch (e: Exception) {
                                actionError = plainError(e, "That change could not be saved. Try again.")
                            }
                        }
                    }) {
                        Text(
                            if (t.assignedUserId == null) "Assign to me"
                            else "Assigned — assign to me instead"
                        )
                    }
                    if (actionError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(actionError!!, color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(Modifier.height(24.dp))
                    Text("Notes (only the team sees these)", style = MaterialTheme.typography.titleMedium)
                    // The backend keeps a ticket's notes as one text
                    // block — added notes are appended into it with
                    // their own "App note by …" stamp lines.
                    if (t.notes.isBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No notes yet.",
                            color = SentinelMuted,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    } else {
                        Spacer(Modifier.height(8.dp))
                        Text(t.notes, style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Add a note") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        enabled = noteText.isNotBlank(),
                        onClick = {
                            val body = noteText.trim()
                            noteText = ""
                            scope.launch {
                                try {
                                    ticket = viewModel.api.addTicketNote(t.id, body)
                                } catch (e: Exception) {
                                    actionError = plainError(e, "That change could not be saved. Try again.")
                                }
                            }
                        },
                    ) { Text("Save note") }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

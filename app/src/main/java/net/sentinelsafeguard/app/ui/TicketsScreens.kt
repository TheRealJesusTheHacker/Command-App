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
private val STATUSES = listOf("new", "in progress", "waiting", "done")
private val PRIORITIES = listOf("low", "normal", "high", "urgent")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketsScreen(viewModel: AppViewModel) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    if (selectedId != null) {
        TicketDetailScreen(viewModel, selectedId!!) { selectedId = null }
        return
    }

    var filter by remember { mutableStateOf("New") }
    var tickets by remember { mutableStateOf<List<Ticket>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(filter, reloadKey) {
        error = null
        tickets = null
        try {
            tickets = viewModel.api.tickets(
                when (filter) {
                    "New" -> "new"
                    "Mine" -> "mine"
                    "Done" -> "done"
                    else -> "open"
                }
            )
        } catch (e: Exception) {
            error = e.message ?: "Tickets could not be loaded. Check your connection and try again."
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
                tickets == null -> LoadingBox("Loading tickets…")
                tickets!!.isEmpty() -> EmptyBox(
                    "No tickets here",
                    when (filter) {
                        "New" -> "No new tickets are waiting."
                        "Done" -> "Nothing has been finished yet."
                        else -> "Nothing matches this list right now."
                    },
                )
                else -> LazyColumn {
                    items(tickets!!) { ticket ->
                        ItemRow(
                            title = ticket.title,
                            subtitle = listOfNotNull(
                                ticket.reporter,
                                ticket.status.replaceFirstChar { it.uppercase() },
                                ticket.createdAt,
                            ).joinToString(" · "),
                            badge = ticket.source?.let { sourceLabel(it) }
                                ?: ticket.priority.takeIf { it == "high" || it == "urgent" }
                                    ?.replaceFirstChar { it.uppercase() },
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
    var priority by remember { mutableStateOf("normal") }
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
                            error = e.message ?: "The ticket could not be saved. Try again."
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
fun TicketDetailScreen(viewModel: AppViewModel, ticketId: String, onBack: () -> Unit) {
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
            error = e.message ?: "This ticket could not be loaded. Go back and try again."
        }
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        TextButton(onClick = onBack) { Text("‹ Back to tickets") }
        when {
            error != null -> ErrorBox(error!!) { reloadKey++ }
            ticket == null -> LoadingBox("Loading ticket…")
            else -> {
                val t = ticket!!
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(t.title, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        listOfNotNull(
                            t.reporter?.let { "From $it" },
                            t.source?.let { sourceLabel(it) },
                            t.createdAt,
                        ).joinToString(" · "),
                        color = SentinelMuted,
                    )
                    if (!t.message.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(t.message, style = MaterialTheme.typography.bodyLarge)
                    }
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
                                    actionError = e.message
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
                                    actionError = e.message
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
                                actionError = e.message
                            }
                        }
                    }) {
                        Text(
                            if (t.assignee.isNullOrBlank()) "Assign to me"
                            else "Assigned to ${t.assignee} — assign to me instead"
                        )
                    }
                    if (actionError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(actionError!!, color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(Modifier.height(24.dp))
                    Text("Notes (only the team sees these)", style = MaterialTheme.typography.titleMedium)
                    t.notes.forEach { note ->
                        Spacer(Modifier.height(8.dp))
                        Text(note.body, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            listOfNotNull(note.author, note.createdAt).joinToString(" · "),
                            color = SentinelMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
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
                                    actionError = e.message
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

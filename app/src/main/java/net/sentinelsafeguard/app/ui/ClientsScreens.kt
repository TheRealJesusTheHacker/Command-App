package net.sentinelsafeguard.app.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import net.sentinelsafeguard.app.data.api.Client
import net.sentinelsafeguard.app.data.api.PipelineResponse
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

private enum class ClientsView { LIST, PIPELINE }

/**
 * Clients (spec §2.1): search, profiles with one-tap call / text /
 * email, the follow-ups-due list, and the read-only Pipeline board
 * (moving cards is a later version — the board lives here, inside
 * the Clients area, so the five bottom tabs stay exactly as spec'd).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(viewModel: AppViewModel) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    if (selectedId != null) {
        ClientProfileScreen(viewModel, selectedId!!) { selectedId = null }
        return
    }
    var view by remember { mutableStateOf(ClientsView.LIST) }

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = view == ClientsView.LIST,
                onClick = { view = ClientsView.LIST },
                label = { Text("Clients") },
            )
            FilterChip(
                selected = view == ClientsView.PIPELINE,
                onClick = { view = ClientsView.PIPELINE },
                label = { Text("Pipeline") },
            )
        }
        when (view) {
            ClientsView.LIST -> ClientList(viewModel) { selectedId = it }
            ClientsView.PIPELINE -> PipelineBoard(viewModel)
        }
    }
}

@Composable
private fun ClientList(viewModel: AppViewModel, onOpen: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var clients by remember { mutableStateOf<List<Client>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(query, reloadKey) {
        error = null
        try {
            clients = viewModel.api.clients(query.ifBlank { null })
        } catch (e: Exception) {
            error = e.message ?: "Clients could not be loaded. Check your connection and try again."
        }
    }

    Column {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search by name, phone, or business") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )
        when {
            error != null -> ErrorBox(error!!) { reloadKey++ }
            clients == null -> LoadingBox("Loading clients…")
            clients!!.isEmpty() -> EmptyBox(
                "No clients found",
                if (query.isBlank()) "No clients are saved yet."
                else "No client matches \"$query\". Try a different search.",
            )
            else -> {
                val due = clients!!.filter { !it.followUpDate.isNullOrBlank() }
                LazyColumn {
                    if (due.isNotEmpty()) {
                        item { SectionHeader("Follow-ups due", due.size) }
                        items(due) { client -> ClientRow(client) { onOpen(client.id) } }
                    }
                    item { SectionHeader("All clients", clients!!.size) }
                    items(clients!!) { client -> ClientRow(client) { onOpen(client.id) } }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ClientRow(client: Client, onClick: () -> Unit) {
    ItemRow(
        title = client.name,
        subtitle = listOfNotNull(client.business, client.phone, client.stage)
            .joinToString(" · "),
        badge = client.tier?.let { "Tier $it" },
        onClick = onClick,
    )
}

@Composable
fun ClientProfileScreen(viewModel: AppViewModel, clientId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var client by remember { mutableStateOf<Client?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(clientId, reloadKey) {
        try {
            client = viewModel.api.client(clientId)
        } catch (e: Exception) {
            error = e.message ?: "This client could not be loaded. Go back and try again."
        }
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        TextButton(onClick = onBack) { Text("‹ Back to clients") }
        when {
            error != null -> ErrorBox(error!!) { reloadKey++ }
            client == null -> LoadingBox("Loading client…")
            else -> {
                val c = client!!
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(c.name, style = MaterialTheme.typography.titleLarge)
                    DetailLine("Business", c.business)
                    DetailLine("Phone", c.phone)
                    DetailLine("Email", c.email)
                    DetailLine("Tier", c.tier?.let { "Tier $it" })
                    DetailLine(
                        "Tags",
                        if (c.tags.isEmpty()) null else c.tags.joinToString(", "),
                    )
                    DetailLine("Stage", c.stage)
                    DetailLine("Next follow-up", c.followUpDate)
                    DetailLine("Notes", c.notes)

                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!c.phone.isNullOrBlank()) {
                            Button(onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:${c.phone}"))
                                )
                            }) { Text("Call") }
                            OutlinedButton(onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${c.phone}"))
                                )
                            }) { Text("Text") }
                        }
                        if (!c.email.isNullOrBlank()) {
                            OutlinedButton(onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${c.email}"))
                                )
                            }) { Text("Email") }
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

/** Read-only board (spec §2.1): stage columns with counts + names. */
@Composable
private fun PipelineBoard(viewModel: AppViewModel) {
    var board by remember { mutableStateOf<PipelineResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        error = null
        try {
            board = viewModel.api.pipeline()
        } catch (e: Exception) {
            error = e.message ?: "The pipeline could not be loaded. Check your connection and try again."
        }
    }

    when {
        error != null -> ErrorBox(error!!) { reloadKey++ }
        board == null -> LoadingBox("Loading pipeline…")
        else -> LazyColumn {
            item {
                Text(
                    "This board is for looking, not moving — move clients between stages in Sentinel Command on the web.",
                    color = SentinelMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            board!!.stages.forEach { stage ->
                item { SectionHeader(stage.name, stage.count) }
                if (stage.clients.isEmpty()) {
                    item { ItemRow("No clients", "Nobody is at this stage right now.") }
                } else {
                    items(stage.clients) { card ->
                        ItemRow(card.name)
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

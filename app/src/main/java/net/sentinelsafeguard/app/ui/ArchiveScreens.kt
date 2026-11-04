package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import net.sentinelsafeguard.app.data.api.archiveDelete
import net.sentinelsafeguard.app.data.api.archiveList
import net.sentinelsafeguard.app.data.api.archiveRestore
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * Archive (Phase 7) — the mirror of web's Archive view: one
 * aggregated table of archived records across modules, in
 * web's order (clients, then cases, then tickets, then
 * assets), each row carrying its full pub row. The backend
 * applies web's sight rule (OWNER or admin:read) and per-module
 * gating — a kind the caller cannot read simply isn't there,
 * exactly as web's filtered workspace renders it.
 *
 * Restore rides the shared restore writers server-side (a
 * client restore cascades to the records archived with it,
 * timestamp-matched, as web) and runs directly, as web's
 * Restore button does. Delete permanently is OWNER-only — web
 * shows the button only to the owner and gates it behind its
 * confirm ("Permanently delete? Owner only."); the server's
 * delete writers carry the same owner gate, and their refusal
 * surfaces in the server's words. After a restore, the row's
 * record opens in its own mirrored view.
 *
 * The kind chips filter the aggregated rows on the client
 * without reordering them (web renders the single table; the
 * chips are the phone's filter over that same table).
 */
@Composable
fun ArchiveScreen(viewModel: AppViewModel, nav: NavController) {
    val isOwner = viewModel.me?.user?.role == "OWNER"
    val scope = rememberCoroutineScope()
    val state = rememberJsonScreen { viewModel.api.archiveList() }
    var kindFilter by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var detailRow by remember { mutableStateOf<JsonObject?>(null) }
    var deleteRow by remember { mutableStateOf<JsonObject?>(null) }
    var restoredRoute by remember { mutableStateOf<String?>(null) }

    fun restore(row: JsonObject) {
        val kind = Phase3Logic.str(row, "kind")
        val id = Phase3Logic.int(row, "id") ?: return
        scope.launch {
            try {
                val out = viewModel.api.archiveRestore(kind, id)
                notice = Phase3Logic.str(out, "message").ifBlank { null }
                error = null
                restoredRoute = Phase7Logic.archiveRouteFor(kind, id)
                state.reloadKey++
            } catch (e: Exception) {
                error = plainError(e, "Not allowed")
            }
        }
    }

    Column {
        MirrorHeader("Archive")
        when {
            state.error != null -> ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null -> SkeletonList()
            else -> {
                val allRows = Phase3Logic.arr(state.data, "rows")
                val kindsPresent = Phase7Logic.ARCHIVE_KINDS.filter { kind ->
                    allRows.any { Phase3Logic.str(it, "kind") == kind }
                }
                val rows = if (kindFilter == null) allRows
                else allRows.filter { Phase3Logic.str(it, "kind") == kindFilter }
                LazyColumn {
                    if (kindsPresent.size > 1) {
                        item {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                            ) {
                                FilterChip(
                                    selected = kindFilter == null,
                                    onClick = { kindFilter = null },
                                    label = { Text("All · ${allRows.size}") },
                                )
                                kindsPresent.forEach { kind ->
                                    val count = allRows.count {
                                        Phase3Logic.str(it, "kind") == kind
                                    }
                                    Spacer(Modifier.padding(horizontal = 3.dp))
                                    FilterChip(
                                        selected = kindFilter == kind,
                                        onClick = { kindFilter = kind },
                                        label = { Text("$kind · $count") },
                                    )
                                }
                            }
                        }
                    }
                    if (notice != null) {
                        item {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            ) {
                                Text(notice!!, color = SentinelMuted, modifier = Modifier.weight(1f))
                                restoredRoute?.let { route ->
                                    TextButton(onClick = {
                                        restoredRoute = null
                                        nav.navigate(route)
                                    }) { Text("Open") }
                                }
                            }
                        }
                    }
                    item { ActionErrorText(error) }
                    if (allRows.isEmpty()) {
                        item {
                            Text(
                                "Nothing archived.",
                                color = SentinelMuted,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    } else if (rows.isEmpty()) {
                        item {
                            Text(
                                "Nothing archived.",
                                color = SentinelMuted,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                    items(rows) { row ->
                        val kind = Phase3Logic.str(row, "kind")
                        ItemRow(
                            title = Phase3Logic.str(row, "name"),
                            subtitle = listOf(kind, Phase7Logic.archiveRowMeta(row))
                                .filter { it.isNotBlank() }.joinToString(" · "),
                            onClick = { detailRow = row },
                        )
                        Row(Modifier.padding(horizontal = 16.dp)) {
                            OutlinedButton(onClick = { restore(row) }) { Text("Restore") }
                            if (isOwner) {
                                Spacer(Modifier.padding(horizontal = 4.dp))
                                TextButton(onClick = { deleteRow = row }) {
                                    Text("Delete permanently")
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    detailRow?.let { row ->
        val entry = Phase3Logic.obj(row, "entry")
        AlertDialog(
            onDismissRequest = { detailRow = null },
            title = { Text(Phase3Logic.str(row, "name")) },
            text = {
                Column {
                    Text(
                        Phase3Logic.str(row, "kind"),
                        color = SentinelMuted,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    if (entry != null) {
                        when (Phase3Logic.str(row, "kind")) {
                            "client" -> JsonLines(
                                entry,
                                "legalName" to "Legal name", "dba" to "DBA",
                                "stage" to "Pipeline stage", "status" to "Status",
                                "contactName" to "Contact", "email" to "Email",
                                "phone" to "Phone", "archivedAt" to "Archived",
                            )
                            "case", "ticket" -> JsonLines(
                                entry,
                                "title" to "Title", "status" to "Status",
                                "priority" to "Priority", "category" to "Category",
                                "clientName" to "Client", "archivedAt" to "Archived",
                            )
                            "asset" -> JsonLines(
                                entry,
                                "name" to "Name", "status" to "Status",
                                "scope" to "Scope", "type" to "Type",
                                "identifier" to "Identifier / serial",
                                "archivedAt" to "Archived",
                            )
                            else -> Text(Phase7Logic.archiveRowMeta(row), color = SentinelMuted)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { detailRow = null; restore(row) }) { Text("Restore") }
            },
            dismissButton = {
                Row {
                    if (isOwner) {
                        TextButton(onClick = { detailRow = null; deleteRow = row }) {
                            Text("Delete permanently")
                        }
                    }
                    TextButton(onClick = { detailRow = null }) { Text("Close") }
                }
            },
        )
    }

    deleteRow?.let { row ->
        val kind = Phase3Logic.str(row, "kind")
        val id = Phase3Logic.int(row, "id") ?: 0
        ConfirmGate(
            title = "Delete permanently",
            body = "Permanently delete? Owner only.",
            confirmLabel = "Delete permanently",
            onDismiss = { deleteRow = null },
            onConfirm = {
                deleteRow = null
                scope.launch {
                    try {
                        val out = viewModel.api.archiveDelete(kind, id)
                        notice = Phase3Logic.str(out, "message").ifBlank { null }
                        error = null
                        state.reloadKey++
                    } catch (e: Exception) {
                        error = plainError(e, "Owner only")
                    }
                }
            },
        )
    }
}

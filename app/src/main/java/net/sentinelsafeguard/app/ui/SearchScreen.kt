package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import net.sentinelsafeguard.app.data.api.SearchResponse

/**
 * Global search (mirror spec §6 Phase 2, G-D4): the shell's
 * Search surface, hitting GET /api/app/search (web globalsearch
 * verbatim, role-scoped server-side). Results arrive grouped
 * exactly as web groups them — Clients / Cases / Tickets /
 * Assets / Invoices / Quotes / Engagements / Knowledge / Law /
 * Attachments — and rows navigate to the record where the app
 * has that view; groups whose views land in later phases route
 * to their named placeholders, never to a dead end.
 */
@Composable
fun SearchScreen(viewModel: AppViewModel, nav: NavController) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<SearchResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun runSearch() {
        val q = query.trim()
        if (q.isBlank()) return
        busy = true
        error = null
        scope.launch {
            try {
                results = viewModel.api.search(q)
            } catch (e: Exception) {
                error = plainError(e, "The search could not be run. Try again.")
            } finally {
                busy = false
            }
        }
    }

    Column {
        MirrorHeader("Search")
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search") },
                placeholder = { Text("Search clients, cases, tickets…") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { runSearch() },
                enabled = !busy && query.isNotBlank(),
                modifier = Modifier.padding(start = 8.dp, top = 8.dp),
            ) { Text(if (busy) "Searching…" else "Search") }
        }
        when {
            error != null -> ErrorBox(error!!) { runSearch() }
            results == null -> EmptyBox(
                "Search Command",
                "Search clients, cases, tickets…",
            )
            else -> {
                val r = results!!
                val any = MirrorVocab.SEARCH_GROUPS.any { (key, _) ->
                    (r.groups[key] ?: emptyList()).isNotEmpty()
                } || r.groups.values.any { it.isNotEmpty() }
                if (!any) {
                    EmptyBox("No results", "Nothing matched “${r.q}”.")
                } else {
                    LazyColumn {
                        MirrorVocab.SEARCH_GROUPS.forEach { (key, label) ->
                            val rows = r.groups[key] ?: emptyList()
                            if (rows.isNotEmpty()) {
                                item { SectionHeader(label, rows.size) }
                                items(rows.size) { i ->
                                    val row = rows[i]
                                    val route = searchRoute(row.type, row.id)
                                    ItemRow(
                                        row.title,
                                        listOfNotNull(
                                            row.subtitle.takeIf { it.isNotBlank() },
                                            row.snippet.takeIf {
                                                it.isNotBlank() && it != row.subtitle
                                            },
                                        ).joinToString(" · "),
                                        onClick = if (route != null) {
                                            { nav.navigate(route) }
                                        } else {
                                            null
                                        },
                                    )
                                }
                            }
                        }
                        // Any group the backend adds later still shows,
                        // under its own key, after web's known groups.
                        val known = MirrorVocab.SEARCH_GROUPS.map { it.first }.toSet()
                        r.groups.keys.filter { it !in known }.sorted().forEach { key ->
                            val rows = r.groups[key] ?: emptyList()
                            if (rows.isNotEmpty()) {
                                item { SectionHeader(key, rows.size) }
                                items(rows.size) { i ->
                                    val row = rows[i]
                                    ItemRow(row.title, row.subtitle)
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

/** Where a search row lands: the mirrored record view when it
 *  exists, the named phase placeholder for later-phase views,
 *  and null (a plain row) for attachments, which have no view. */
private fun searchRoute(type: String, id: Int): String? = when (type) {
    "clients" -> "client/$id"
    "cases" -> "case/$id"
    "tickets" -> "ticket/$id"
    "engagements" -> "engagement/$id"
    "assets" -> "view/assets"
    "invoices", "quotes" -> "view/invoices"
    "knowledge" -> "view/knowledge"
    "law" -> "view/law"
    else -> null
}

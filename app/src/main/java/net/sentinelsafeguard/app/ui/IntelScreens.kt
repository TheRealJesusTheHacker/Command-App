package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import kotlinx.serialization.json.JsonObject
import net.sentinelsafeguard.app.data.api.liveFlightLookup
import net.sentinelsafeguard.app.data.api.liveFlights
import net.sentinelsafeguard.app.data.api.liveGeoData
import net.sentinelsafeguard.app.data.api.liveThreatData
import net.sentinelsafeguard.app.data.api.threatIntel
import net.sentinelsafeguard.app.data.api.threatPushToNews
import net.sentinelsafeguard.app.data.api.threatSync
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * Live Intel + Threat Intel (Phase 6). Live Intel mirrors web's
 * three tabs — Threat Map, Geopolitics, Flights (with Track a
 * flight) — composed from getthreatdata / getgeodata /
 * getliveflights / getflightlookup exactly as web composes them.
 * It has no boot flag: the module perm is enforced server-side
 * and a refusal surfaces in the server's own words. Threat Intel
 * mirrors the fused feed with its filters, detail, Run threat
 * sync, and Push to News Desk (always lands as a draft).
 */

@Composable
fun LiveIntelScreen(viewModel: AppViewModel, nav: NavController) {
    var tab by remember { mutableStateOf("Threat Map") }
    Column {
        MirrorHeader("Live Intel") {
            Text("Public feeds", color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
        }
        ChipRow(
            options = listOf("threat" to "Threat Map", "geo" to "Geopolitics", "flights" to "Flights"),
            selected = when (tab) { "Threat Map" -> "threat"; "Geopolitics" -> "geo"; else -> "flights" },
            onPick = { tab = when (it) { "geo" -> "Geopolitics"; "flights" -> "Flights"; else -> "Threat Map" } },
        )
        when (tab) {
            "Threat Map" -> LiveFeedCard(viewModel, "Threat Map") { viewModel.api.liveThreatData() }
            "Geopolitics" -> LiveFeedCard(viewModel, "Geopolitics") { viewModel.api.liveGeoData() }
            else -> FlightsTab(viewModel)
        }
    }
}

@Composable
private fun LiveFeedCard(viewModel: AppViewModel, title: String, loader: suspend () -> JsonObject) {
    val state = rememberJsonScreen(loader)
    Column {
        Row(Modifier.padding(horizontal = 16.dp)) {
            OutlinedButton(onClick = { state.reloadKey++ }) { Text("Reload") }
        }
        when {
            state.error != null -> ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null -> SkeletonList(3)
            else -> {
                val data = state.data!!
                MirrorCard(title) {
                    DetailLine("Connected", if (Phase3Logic.bool(data, "connected") == true) "Yes" else "No")
                    DetailLine("Stale", if (Phase3Logic.bool(data, "stale") == true) "Yes" else null)
                    Phase6Logic.flatten(data["data"]).forEach { (label, value) ->
                        DetailLine(label, value)
                    }
                }
            }
        }
    }
}

@Composable
private fun FlightsTab(viewModel: AppViewModel) {
    val scope = rememberCoroutineScope()
    var flightNumber by remember { mutableStateOf("") }
    var lookup by remember { mutableStateOf<JsonObject?>(null) }
    var lookupError by remember { mutableStateOf<String?>(null) }
    Column {
        MirrorCard("Track a flight") {
            TextFieldRow("Flight number", flightNumber, onChange = { flightNumber = it })
            ActionErrorText(lookupError)
            Button(onClick = {
                scope.launch {
                    try {
                        lookup = viewModel.api.liveFlightLookup(flightNumber)
                        lookupError = null
                    } catch (e: Exception) {
                        lookupError = plainError(e, "The flight lookup could not be done. Try again.")
                    }
                }
            }) { Text("Look up flight") }
            lookup?.let { result ->
                Phase3Logic.strOrNull(result, "error")?.let { DetailLine("Lookup", it) }
                Phase6Logic.flatten(result["result"]).forEach { (label, value) ->
                    DetailLine(label, value)
                }
            }
        }
        LiveFeedCard(viewModel, "Live flights") { viewModel.api.liveFlights() }
    }
}

@Composable
fun ThreatIntelScreen(viewModel: AppViewModel, nav: NavController) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf<String?>(null) }
    var category by remember { mutableStateOf<String?>(null) }
    var detailId by remember { mutableStateOf<Int?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val state = rememberJsonScreen {
        viewModel.api.threatIntel(q = query, severity = severity, category = category)
    }
    androidx.compose.runtime.LaunchedEffect(query, severity, category) { state.reloadKey++ }
    Column {
        MirrorHeader("Threat Intel") {
            Button(onClick = {
                scope.launch {
                    try {
                        val out = viewModel.api.threatSync()
                        notice = Phase3Logic.str(out, "message")
                        state.reloadKey++
                    } catch (e: Exception) {
                        error = plainError(e, "The sync could not be queued. Try again.")
                    }
                }
            }) { Text("Run threat sync") }
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            if (notice != null) Text(notice!!)
            ActionErrorText(error)
        }
        androidx.compose.material3.OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search threat intel") },
            singleLine = true,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        ChipRow(
            options = listOf("all" to "All severities") + Phase6Logic.THREAT_SEVERITIES.map { it to it },
            selected = severity ?: "all",
            onPick = { severity = it },
        )
        state.data?.let { data ->
            val categories = Phase3Logic.arrRaw(data, "categories")
                .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
            if (categories.isNotEmpty()) {
                ChipRow(
                    options = listOf("all" to "All categories") + categories.map { it to it },
                    selected = category ?: "all",
                    onPick = { category = it },
                )
            }
        }
        when {
            state.error != null -> ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null -> SkeletonList()
            else -> {
                val data = state.data!!
                val stats = Phase3Logic.obj(data, "stats")
                if (stats != null) {
                    MirrorCard("Feed") {
                        JsonLines(stats, "total" to "Total", "critical" to "Critical",
                            "high" to "High", "medium" to "Medium", "low" to "Low", "last24h" to "Last 24h")
                        Phase3Logic.strOrNull(data, "boundaryCopy")?.let {
                            Text(it, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                val items = Phase3Logic.arr(data, "items")
                LazyColumn {
                    item { SectionHeader("Items", items.size) }
                    if (items.isEmpty()) {
                        item { EmptyBox("No threat intel items", "Items from the fused feed appear here.") }
                    }
                    items(items) { item ->
                        ItemRow(
                            title = Phase3Logic.str(item, "title"),
                            subtitle = listOfNotNull(
                                Phase3Logic.strOrNull(item, "severity"),
                                Phase3Logic.strOrNull(item, "category"),
                                Phase3Logic.int(item, "score")?.let { "Score $it" },
                                Phase3Logic.strOrNull(item, "primarySource"),
                            ).joinToString(" · "),
                            badge = Phase3Logic.int(item, "newsItemId")?.let { "News #$it" },
                            onClick = { detailId = Phase3Logic.int(item, "id") },
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
    detailId?.let { id ->
        ThreatDetailSheet(viewModel, id) { detailId = null; state.reloadKey++ }
    }
}

@Composable
private fun ThreatDetailSheet(viewModel: AppViewModel, itemId: Int, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val state = rememberJsonScreen { viewModel.api.threatIntel(itemId = itemId) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Threat Intel item") },
        text = {
            Column {
                when {
                    state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error)
                    state.data == null -> Text("Loading…", color = SentinelMuted)
                    else -> {
                        val detail = Phase3Logic.obj(state.data, "detail") ?: state.data!!
                        Text(Phase3Logic.str(detail, "title"), style = MaterialTheme.typography.titleSmall)
                        JsonLines(detail,
                            "severity" to "Severity", "category" to "Category",
                            "primarySource" to "Source", "url" to "URL")
                        DetailLine("Score", Phase3Logic.int(detail, "score")?.toString())
                        DetailLine("Summary", Phase3Logic.strOrNull(detail, "summary"))
                        val cves = Phase3Logic.arrRaw(detail, "cveIds")
                            .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                        if (cves.isNotEmpty()) DetailLine("CVEs", cves.joinToString(", "))
                        if (notice != null) Text(notice!!)
                        ActionErrorText(error)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                scope.launch {
                    try {
                        val out = viewModel.api.threatPushToNews(itemId)
                        notice = Phase3Logic.str(out, "message")
                    } catch (e: Exception) {
                        error = plainError(e, "That could not be pushed to News Desk. Try again.")
                    }
                }
            }) { Text("Push to News Desk") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onClose) { Text("Close") }
        },
    )
}

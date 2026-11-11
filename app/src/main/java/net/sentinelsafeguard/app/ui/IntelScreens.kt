package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import net.sentinelsafeguard.app.ui.theme.SentinelAmber
import net.sentinelsafeguard.app.ui.theme.SentinelDanger
import net.sentinelsafeguard.app.ui.theme.SentinelDangerBright
import net.sentinelsafeguard.app.ui.theme.SentinelGreen
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
            "Threat Map" -> ThreatMapTab(viewModel)
            "Geopolitics" -> GeopoliticsTab(viewModel)
            else -> FlightsTab(viewModel)
        }
    }
}

/**
 * The STALE chip: shown whenever a tab is rendering last-good
 * data (upstream flagged stale, or a refresh failed and the
 * previous good model is being held). Carries the payload's
 * own updated/fetched time so the operator can judge the age.
 */
@Composable
private fun StaleChip(updatedLabel: String?) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .background(
                SentinelAmber.copy(alpha = 0.14f),
                androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            "STALE",
            color = SentinelAmber,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        )
        if (!updatedLabel.isNullOrBlank()) {
            Text(
                " · last good $updatedLabel",
                color = SentinelMuted,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/** Marker tap → the HUD readout sheet for that marker. */
@Composable
private fun MarkerDetailDialog(
    marker: IntelMapLogic.MapMarkerModel?,
    onClose: () -> Unit,
) {
    if (marker == null) return
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onClose,
        title = { Text(marker.title) },
        text = {
            Column {
                marker.detailLines.forEach { (label, value) ->
                    DetailLine(label, value)
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onClose) { Text("Close") }
        },
    )
}

/** A count that rolls to its new value on refresh (Motion
 *  discipline: instant under reduced motion). */
@Composable
private fun rollingCount(target: Int): Int {
    val reduced = rememberReducedMotion()
    val shown by androidx.compose.animation.core.animateIntAsState(
        targetValue = target,
        animationSpec = if (reduced) {
            androidx.compose.animation.core.tween(0)
        } else {
            androidx.compose.animation.core.tween(700)
        },
        label = "rollingCount",
    )
    return shown
}

private fun Int.commas(): String =
    String.format(java.util.Locale.US, "%,d", this)

@Composable
private fun ThreatMapTab(viewModel: AppViewModel) {
    val state = rememberJsonScreen { viewModel.api.liveThreatData() }
    val parsed = remember(state.data) {
        state.data?.let { IntelMapLogic.parseThreat(it) }
    }
    // Last-good: a fresh-but-empty or failed render never
    // blanks a map that already showed real attackers.
    var lastGood by remember { mutableStateOf<IntelMapLogic.ThreatMapModel?>(null) }
    androidx.compose.runtime.LaunchedEffect(parsed) {
        if (parsed != null && parsed.hasContent) lastGood = parsed
    }
    val model = if (parsed?.hasContent == true) parsed else (lastGood ?: parsed)
    val showingLastGood = model != null && model === lastGood && parsed?.hasContent != true
    var selectedId by remember { mutableStateOf<String?>(null) }
    val attacksShown = rollingCount(model?.attacksToday ?: 0)

    Column {
        Row(Modifier.padding(horizontal = 16.dp)) {
            OutlinedButton(onClick = { state.reloadKey++ }) { Text("Reload") }
        }
        if (model != null && (model.stale || showingLastGood)) {
            StaleChip(model.updated)
        }
        SentinelMap(
            markers = model?.markers ?: emptyList(),
            circles = model?.countryCircles ?: emptyList(),
            polylines = model?.arcs ?: emptyList(),
            onMarkerTap = { selectedId = it },
            accent = SentinelAmber,
            frameCenter = IntelMapLogic.WORLD_CENTER,
            frameZoom = IntelMapLogic.WORLD_ZOOM,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth()
                .height(340.dp),
        )
        when {
            model == null && state.error != null ->
                ErrorBox(state.error!!) { state.reloadKey++ }
            model == null -> SkeletonList(3)
            else -> {
                MirrorCard("Threat Map", model.markers.size) {
                    DetailLine("Connected", if (model.connected) "Yes" else "No")
                    DetailLine("Attacks today", attacksShown.commas())
                    DetailLine("Sources today", model.sourcesToday.commas())
                    DetailLine("Countries tracked", model.countryCircles.size.toString())
                    DetailLine("Updated", model.updated)
                    DetailLine("Feed status", model.status)
                    DetailLine("Feed note", model.error)
                }
                if (model.feed.isNotEmpty()) {
                    MirrorCard("Attack feed", model.feed.size) {
                        model.feed.forEach { row ->
                            Text(
                                "${row.ip} · ${row.attacks.commas()} attacks",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            Text(
                                listOfNotNull(
                                    row.countryCode.takeIf { it.isNotBlank() },
                                    row.asname.takeIf { it.isNotBlank() },
                                    row.typesLabel.takeIf { it.isNotBlank() },
                                ).joinToString(" · "),
                                color = SentinelMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
    MarkerDetailDialog(
        marker = model?.markers?.firstOrNull { it.markerId == selectedId },
        onClose = { selectedId = null },
    )
}

@Composable
private fun GeopoliticsTab(viewModel: AppViewModel) {
    val state = rememberJsonScreen { viewModel.api.liveGeoData() }
    val parsed = remember(state.data) {
        state.data?.let { IntelMapLogic.parseGeo(it) }
    }
    var lastGood by remember { mutableStateOf<IntelMapLogic.GeoMapModel?>(null) }
    androidx.compose.runtime.LaunchedEffect(parsed) {
        if (parsed != null && parsed.hasContent) lastGood = parsed
    }
    val model = if (parsed?.hasContent == true) parsed else (lastGood ?: parsed)
    val showingLastGood = model != null && model === lastGood && parsed?.hasContent != true
    var selectedId by remember { mutableStateOf<String?>(null) }
    val headlinesShown = rollingCount(model?.items?.size ?: 0)

    Column {
        Row(Modifier.padding(horizontal = 16.dp)) {
            OutlinedButton(onClick = { state.reloadKey++ }) { Text("Reload") }
        }
        if (model != null && (model.stale || showingLastGood)) {
            StaleChip(model.updated)
        }
        SentinelMap(
            markers = model?.spots ?: emptyList(),
            onMarkerTap = { selectedId = it },
            accent = SentinelGreen,
            frameCenter = IntelMapLogic.WORLD_CENTER,
            frameZoom = IntelMapLogic.WORLD_ZOOM,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth()
                .height(340.dp),
        )
        Text(
            "Hotspots placed by headline location",
            color = SentinelMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        )
        when {
            model == null && state.error != null ->
                ErrorBox(state.error!!) { state.reloadKey++ }
            model == null -> SkeletonList(3)
            else -> {
                MirrorCard("Geopolitics", model.spots.size) {
                    DetailLine("Connected", if (model.connected) "Yes" else "No")
                    DetailLine("Headlines", headlinesShown.toString())
                    DetailLine("Hotspots plotted", model.spots.size.toString())
                    DetailLine(
                        "Headlines not placed",
                        model.unplacedCount?.toString(),
                    )
                    DetailLine("Updated", model.updated)
                    DetailLine("Feed note", model.error)
                }
                if (model.items.isNotEmpty()) {
                    MirrorCard("Headlines", model.items.size) {
                        model.items.forEach { item ->
                            Text(
                                item.title,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                            Text(
                                listOfNotNull(
                                    item.source.takeIf { it.isNotBlank() },
                                    item.published.takeIf { it.isNotBlank() },
                                ).joinToString(" · "),
                                color = SentinelMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (item.impact.isNotBlank()) {
                                Text(
                                    item.impact,
                                    color = SentinelMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    MarkerDetailDialog(
        marker = model?.spots?.firstOrNull { it.markerId == selectedId },
        onClose = { selectedId = null },
    )
}

@Composable
private fun FlightsTab(viewModel: AppViewModel) {
    val scope = rememberCoroutineScope()
    var flightNumber by remember { mutableStateOf("") }
    var lookup by remember { mutableStateOf<JsonObject?>(null) }
    var lookupError by remember { mutableStateOf<String?>(null) }
    val route = remember(lookup) {
        IntelMapLogic.routeForLookup(
            lookup?.let { Phase3Logic.obj(it, "result") },
        )
    }

    val state = rememberJsonScreen { viewModel.api.liveFlights() }
    // Live aircraft: refresh on the server's own cadence (the
    // Command cache over the feed is 30 s) while this tab is
    // composed; leaving the tab stops the loop.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000L)
            state.reloadKey++
        }
    }
    val parsed = remember(state.data) {
        state.data?.let { IntelMapLogic.parseFlights(it) }
    }
    var lastGood by remember { mutableStateOf<IntelMapLogic.FlightsMapModel?>(null) }
    androidx.compose.runtime.LaunchedEffect(parsed) {
        if (parsed != null && parsed.hasContent) lastGood = parsed
    }
    val model = if (parsed?.hasContent == true) parsed else (lastGood ?: parsed)
    val showingLastGood = model != null && model === lastGood && parsed?.hasContent != true
    var selectedId by remember { mutableStateOf<String?>(null) }
    val aircraftShown = rollingCount(model?.total ?: 0)

    val mapMarkers: List<IntelMapLogic.MapMarkerModel> =
        (model?.aircraft ?: emptyList()) + (route?.endpointMarkers() ?: emptyList())
    // Ambient aircraft carry no origin/destination — their
    // heading vectors (from the feed's own heading + speed)
    // ride the map instead of an invented route; the looked-up
    // flight's route polyline joins when it resolves.
    val mapPolylines: List<IntelMapLogic.MapPolylineModel> =
        (model?.headingVectors ?: emptyList()) + listOfNotNull(route?.polyline)
    val selectedMarker = mapMarkers.firstOrNull { it.markerId == selectedId }

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
                if (Phase3Logic.obj(result, "result") != null) {
                    if (route != null) {
                        DetailLine(
                            "Route",
                            "${route.fromIata} → ${route.toIata} — plotted on the map below",
                        )
                    } else {
                        Text(
                            "Route not plotted — airport coordinates are unavailable for one of the endpoints.",
                            color = SentinelMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        Row(Modifier.padding(horizontal = 16.dp)) {
            OutlinedButton(onClick = { state.reloadKey++ }) { Text("Reload") }
        }
        if (model != null && (model.stale || showingLastGood)) {
            StaleChip(model.fetchedAt)
        }
        SentinelMap(
            markers = mapMarkers,
            polylines = mapPolylines,
            onMarkerTap = { selectedId = it },
            accent = SentinelGreen,
            frameCenter = model?.center ?: IntelMapLogic.KC_CENTER,
            frameZoom = IntelMapLogic.FLIGHTS_ZOOM,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth()
                .height(340.dp),
        )
        when {
            model == null && state.error != null ->
                ErrorBox(state.error!!) { state.reloadKey++ }
            model == null -> SkeletonList(3)
            else -> {
                MirrorCard("Live flights", model.aircraft.size) {
                    DetailLine("Connected", if (model.connected) "Yes" else "No")
                    DetailLine("Aircraft in range", aircraftShown.toString())
                    DetailLine(
                        "Data age",
                        model.ageS?.let {
                            String.format(java.util.Locale.US, "%.0f s", it)
                        },
                    )
                    DetailLine("Source", model.source)
                    DetailLine("Fetched", model.fetchedAt)
                    model.note?.let {
                        Text(
                            it,
                            color = SentinelMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                if (model.aircraft.isNotEmpty()) {
                    MirrorCard("Aircraft", model.aircraft.size) {
                        model.aircraft.forEach { flight ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .sentinelClickable { selectedId = flight.markerId }
                                    .padding(vertical = 4.dp),
                            ) {
                                Row {
                                    Text(
                                        flight.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (flight.emergency) {
                                        Text(
                                            "EMERGENCY",
                                            color = SentinelDangerBright,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        )
                                    }
                                }
                                Text(
                                    listOfNotNull(
                                        flight.type.takeIf { it.isNotBlank() },
                                        if (flight.onGround) {
                                            "On ground"
                                        } else {
                                            flight.altitudeFt?.let { alt -> "${alt.commas()} ft" }
                                        },
                                        flight.speedKt?.let { spd ->
                                            String.format(java.util.Locale.US, "%.0f kt", spd)
                                        },
                                    ).joinToString(" · "),
                                    color = SentinelMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    MarkerDetailDialog(marker = selectedMarker, onClose = { selectedId = null })
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
        MirrorHeader("Threat Intel", MirrorCopy.subtitle("threatintel")) {
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
            state.error != null && state.data == null -> ErrorBox(state.error!!) { state.reloadKey++ }
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
                    state.error != null && state.data == null -> Text(state.error!!, color = MaterialTheme.colorScheme.error)
                    state.data == null -> SkeletonList(3)
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

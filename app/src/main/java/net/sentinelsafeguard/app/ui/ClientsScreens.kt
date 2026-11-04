package net.sentinelsafeguard.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import net.sentinelsafeguard.app.R
import net.sentinelsafeguard.app.ui.theme.SentinelSurface
import net.sentinelsafeguard.app.ui.theme.SentinelSurfaceTop
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.data.api.Client
import net.sentinelsafeguard.app.data.api.ClientDetailResponse
import net.sentinelsafeguard.app.data.api.ClientTimelineResponse
import net.sentinelsafeguard.app.data.api.ClientWriteRequest
import net.sentinelsafeguard.app.data.api.PipelineResponse
import net.sentinelsafeguard.app.data.api.applySavedView
import net.sentinelsafeguard.app.data.api.bulkClients
import net.sentinelsafeguard.app.data.api.clientFilterPresets
import net.sentinelsafeguard.app.data.api.deleteClientFilterPreset
import net.sentinelsafeguard.app.data.api.deleteSavedView
import net.sentinelsafeguard.app.data.api.saveClientFilterPreset
import net.sentinelsafeguard.app.data.api.saveSavedView
import net.sentinelsafeguard.app.data.api.savedViews
import net.sentinelsafeguard.app.ui.theme.SentinelMuted
import net.sentinelsafeguard.app.ui.theme.SentinelSurfaceHigh

/**
 * Clients / Pipeline (mirror spec §3.4): web's shell (header,
 * subtitle, stat cards, Directory | Pipeline | Duplicates),
 * server-side phone-book search over the complete set, the
 * 8-stage pipeline board on the B7 full-card payload, the full
 * profile (B8 timeline + linked grid), and the full form.
 * Phase 3 adds the depth sections (ClientDepth.kt), the live
 * Duplicates tab, saved views / filter presets, and the client
 * bulk actions bar — all web's names and order.
 */

private fun followUpLabel(client: Client, today: String): String? {
    val due = client.nextFollowUp?.takeIf { it.isNotBlank() } ?: return null
    return if (due < today) "Follow-up overdue · $due" else "Follow-up $due"
}

@Composable
fun ClientsScreen(viewModel: AppViewModel, nav: NavController) {
    var mode by remember { mutableStateOf("Directory") }
    val isOwner = viewModel.me?.user?.role == "OWNER"
    Column {
        MirrorHeader(
            "Clients",
            "A working phone book, client profiles, and a live sales pipeline — all in one place.",
        ) {
            OutlinedButton(onClick = { nav.navigate("verified") }) {
                Text("Sentinel Verified")
            }
            Button(
                onClick = { nav.navigate("clientNew") },
                modifier = Modifier.padding(start = 8.dp),
            ) { Text("+ New client") }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = mode == "Directory", onClick = { mode = "Directory" }, label = { Text("Directory") })
            FilterChip(selected = mode == "Pipeline", onClick = { mode = "Pipeline" }, label = { Text("Pipeline") })
            if (isOwner) {
                FilterChip(
                    selected = mode == "Duplicates",
                    onClick = { mode = "Duplicates" },
                    label = { Text("Duplicates") },
                )
            }
        }
        when (mode) {
            "Pipeline" -> PipelineBoard(viewModel, nav)
            "Duplicates" -> DuplicatesMode(viewModel, nav)
            else -> ClientDirectory(viewModel, nav)
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ClientDirectory(viewModel: AppViewModel, nav: NavController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var all by remember { mutableStateOf<List<Client>?>(null) }
    var total by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var sort by remember { mutableStateOf(MirrorVocab.CLIENT_SORTS[0]) }
    var stage by remember { mutableStateOf<String?>(null) }
    var tier by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    // hunt17: pull-to-refresh extends to the Clients directory —
    // the last list surface without it (Pass 3 decision).
    var refreshing by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf(setOf<Int>()) }
    var bulkTag by remember { mutableStateOf("") }
    var bulkFollowUp by remember { mutableStateOf("") }
    var bulkNotice by remember { mutableStateOf<String?>(null) }
    val todayStr = remember { java.time.LocalDate.now().toString() }

    LaunchedEffect(query, reloadKey) {
        error = null
        try {
            val page = viewModel.api.clients(query.ifBlank { null })
            all = page.items
            total = page.total
        } catch (e: Exception) {
            error = plainError(e, "Clients could not be loaded. Check your connection and try again.")
        } finally {
            refreshing = false
        }
    }

    fun reset() {
        query = ""; sort = MirrorVocab.CLIENT_SORTS[0]; stage = null; tier = null
    }

    fun bulk(operation: JsonObject) {
        scope.launch {
            try {
                val out = viewModel.api.bulkClients(selection.toList(), operation)
                bulkNotice = Phase3Logic.str(out, "message")
                selection = emptySet()
                reloadKey++
            } catch (e: Exception) {
                bulkNotice = plainError(e, "The bulk action could not be done. Try again.")
            }
        }
    }

    when {
        error != null -> ErrorBox(error!!) { reloadKey++ }
        all == null -> SkeletonList(6)
        else -> {
            val clients = all!!
            val filtered = MirrorVocab.sortClients(
                clients.filter {
                    (stage == null || it.stage == stage) &&
                        (tier == null || MirrorVocab.tierOf(it) == tier)
                },
                sort,
            )
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { refreshing = true; reloadKey++ },
            ) {
            LazyColumn {
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        StatCard("Total clients", total.toString(), Modifier.weight(1f))
                        StatCard(
                            "In pipeline",
                            MirrorVocab.inPipelineCount(clients).toString(),
                            Modifier.weight(1f),
                        )
                        StatCard(
                            "Active clients",
                            clients.count { it.stage == "Active Client" }.toString(),
                            Modifier.weight(1f),
                        )
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        StatCard(
                            "Pipeline value",
                            MirrorVocab.formatDollars(MirrorVocab.pipelineValue(clients)),
                            Modifier.weight(1f),
                        )
                        StatCard(
                            "Top sources",
                            MirrorVocab.topSources(clients)
                                .joinToString("  ") { "${it.first} · ${it.second}" }
                                .ifBlank { "—" },
                            Modifier.weight(2f),
                        )
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search the phone book") },
                        placeholder = { Text("Name, company, email, or phone") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_nav_search),
                                contentDescription = null,
                                tint = SentinelMuted,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = net.sentinelsafeguard.app.ui.theme.SentinelSurfaceLow,
                            focusedContainerColor = net.sentinelsafeguard.app.ui.theme.SentinelSurfaceLow,
                        ),
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
                        ChoiceMenu("Sort clients", sort, MirrorVocab.CLIENT_SORTS) { sort = it }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { reset() }) { Text("Reset filters") }
                    }
                    ChipRow(
                        options = listOf("all" to "All ${clients.size}") +
                            MirrorVocab.stageChipCounts(clients)
                                .map { (s, n) -> s to "$s $n" },
                        selected = stage ?: "all",
                        onPick = { stage = it },
                    )
                    Text(
                        "Triage tier",
                        color = SentinelMuted,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                    )
                    ChipRow(
                        options = listOf("all" to "All ${clients.size}") +
                            MirrorVocab.tierChipCounts(clients)
                                .map { (t, n) -> t to "${MirrorVocab.TIER_LABELS[t]} $n" },
                        selected = tier ?: "all",
                        onPick = { tier = it },
                    )
                    ClientViewsBar(viewModel, stage) { picked -> stage = picked }
                    Row(
                        Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = {
                            selecting = !selecting
                            if (!selecting) selection = emptySet()
                        }) {
                            Text(if (selecting) "Done selecting" else "Select for bulk actions")
                        }
                        if (selecting && selection.isNotEmpty()) {
                            Text("${selection.size} selected", color = SentinelMuted)
                        }
                    }
                    if (selecting && selection.isNotEmpty()) {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            ChoiceMenu("Bulk — set stage", "Choose a stage", MirrorVocab.CLIENT_STAGES) { picked ->
                                bulk(buildJsonObject {
                                    put("kind", "stage"); put("stage", picked)
                                })
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(1f)) {
                                    TextFieldRow("Bulk — apply tag", bulkTag, { bulkTag = it })
                                }
                                Button(
                                    enabled = bulkTag.isNotBlank(),
                                    onClick = {
                                        bulk(buildJsonObject {
                                            put("kind", "tag"); put("tag", bulkTag.trim())
                                        })
                                        bulkTag = ""
                                    },
                                ) { Text("Apply tag") }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(1f)) {
                                    DateFieldRow(
                                        "Bulk — next follow-up",
                                        bulkFollowUp, { bulkFollowUp = it },
                                    )
                                }
                                Button(
                                    enabled = bulkFollowUp.isNotBlank(),
                                    onClick = {
                                        bulk(buildJsonObject {
                                            put("kind", "followup")
                                            put("nextFollowUp", bulkFollowUp.trim())
                                        })
                                        bulkFollowUp = ""
                                    },
                                ) { Text("Set follow-up") }
                            }
                            TextButton(onClick = { selection = emptySet() }) {
                                Text("Clear selection")
                            }
                        }
                    }
                    if (bulkNotice != null) {
                        Text(
                            bulkNotice!!,
                            color = SentinelMuted,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
                when {
                    clients.isEmpty() && query.isBlank() -> item {
                        EmptyBox(
                            "Your phone book is ready",
                            "No clients yet. Add the first one with + New client.",
                        )
                    }
                    filtered.isEmpty() -> item {
                        EmptyBox("No matches", "No clients match the current search and filters.")
                        OutlinedButton(
                            onClick = { reset() },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) { Text("Reset search & filters") }
                    }
                    else -> itemsIndexed(filtered) { index, client ->
                        ClientCard(
                            entranceIndex = index,
                            client = client,
                            todayStr = todayStr,
                            onOpen = { nav.navigate("client/${client.id}") },
                            onEdit = { nav.navigate("clientEdit/${client.id}") },
                            onCall = {
                                if (client.phone.isNotBlank()) {
                                    context.startActivity(
                                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${client.phone}"))
                                    )
                                }
                            },
                            onEmail = {
                                if (client.email.isNotBlank()) {
                                    context.startActivity(
                                        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${client.email}"))
                                    )
                                }
                            },
                            onArchive = {
                                scope.launch {
                                    try {
                                        viewModel.api.archiveClient(client.id)
                                        reloadKey++
                                    } catch (e: Exception) {
                                        error = plainError(e, "The client could not be archived. Try again.")
                                    }
                                }
                            },
                            selectChecked = if (selecting) client.id in selection else null,
                            onSelectToggle = if (selecting) {
                                {
                                    selection = if (client.id in selection) {
                                        selection - client.id
                                    } else {
                                        selection + client.id
                                    }
                                }
                            } else {
                                null
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

/** Saved views + filter presets for the directory (web's bar). */
@Composable
private fun ClientViewsBar(
    viewModel: AppViewModel,
    currentStage: String?,
    onApplyStage: (String?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var presets by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var views by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var applied by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var appliedLabel by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            try {
                presets = Phase3Logic.arr(viewModel.api.clientFilterPresets(), "presets")
                views = Phase3Logic.arr(viewModel.api.savedViews("clients"), "views")
            } catch (e: Exception) {
                error = plainError(e, "Saved views could not be loaded. Try again.")
            }
        }
    }
    LaunchedEffect(Unit) { reload() }

    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(
            "Saved views & filter presets",
            style = MaterialTheme.typography.titleSmall,
        )
        presets.forEach { preset ->
            val pid = Phase3Logic.int(preset, "id")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Phase3Logic.str(preset, "name"), Modifier.weight(1f))
                TextButton(onClick = {
                    onApplyStage(
                        Phase3Logic.criteriaStage(Phase3Logic.obj(preset, "criteria")),
                    )
                }) { Text("Apply") }
                if (pid != null) {
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                viewModel.api.deleteClientFilterPreset(pid); reload()
                            } catch (e: Exception) {
                                error = plainError(e, "The preset could not be deleted. Try again.")
                            }
                        }
                    }) { Text("Delete") }
                }
            }
        }
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
                            onApplyStage(
                                Phase3Logic.criteriaStage(Phase3Logic.obj(view, "criteria")),
                            )
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
                "Applied “${appliedLabel}” — ${applied.size} matching client(s)",
                color = SentinelMuted,
            )
            applied.take(10).forEach { row ->
                Text(
                    "· ${Phase3Logic.str(row, "name")}",
                    color = SentinelMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        TextFieldRow("Name for the current stage filter", name, { name = it })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    scope.launch {
                        try {
                            val criteria = buildJsonObject {
                                put("stage", currentStage ?: "")
                            }
                            viewModel.api.saveClientFilterPreset(name.trim(), criteria)
                            name = ""; reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The preset could not be saved. Try again.")
                        }
                    }
                },
            ) { Text("Save preset") }
            OutlinedButton(
                enabled = name.isNotBlank(),
                onClick = {
                    scope.launch {
                        try {
                            val criteria = buildJsonObject {
                                put("stage", currentStage ?: "")
                            }
                            viewModel.api.saveSavedView("clients", name.trim(), criteria)
                            name = ""; reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The view could not be saved. Try again.")
                        }
                    }
                },
            ) { Text("Save view") }
        }
        ActionErrorText(error)
    }
}

@Composable
private fun ClientCard(
    client: Client,
    todayStr: String,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onCall: (() -> Unit)? = null,
    onEmail: (() -> Unit)? = null,
    onArchive: (() -> Unit)? = null,
    selectChecked: Boolean? = null,
    onSelectToggle: (() -> Unit)? = null,
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
        Column(Modifier.padding(16.dp)) {
            if (selectChecked != null && onSelectToggle != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = selectChecked, onCheckedChange = { onSelectToggle() })
                    Text("Select for bulk actions", color = SentinelMuted)
                }
            }
            // hunt18: 40dp monogram disc from the displayed name +
            // the chevron affordance — row structure unchanged.
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonogramDisc(client.legalName, 40.dp)
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(client.legalName, style = MaterialTheme.typography.bodyLarge)
                    if (client.dba.isNotBlank()) {
                        Text(client.dba, color = SentinelMuted)
                    }
                }
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = SentinelMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
            Row(
                Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MirrorChip(client.stage)
                if (client.familyLinked) MirrorChip("Family")
                else if (client.friends) MirrorChip("Friends — 20% off")
                MirrorVocab.TIER_LABELS[MirrorVocab.tierOf(client)]?.let { MirrorChip(it) }
            }
            if (client.contactName.isNotBlank()) {
                Text(
                    listOfNotNull(
                        client.contactName,
                        client.contactTitle.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (client.phone.isNotBlank()) Text("Phone ${client.phone}", color = SentinelMuted)
            if (client.email.isNotBlank()) Text("Email ${client.email}", color = SentinelMuted)
            MirrorVocab.formatDollars(client.value).takeIf { it.isNotBlank() }?.let {
                Text("Value $it")
            }
            followUpLabel(client, todayStr)?.let { Text(it, color = SentinelMuted) }
            client.tags?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (client.phone.isNotBlank() && onCall != null) {
                    TextButton(onClick = onCall) { Text("Call") }
                }
                if (client.email.isNotBlank() && onEmail != null) {
                    TextButton(onClick = onEmail) { Text("Email") }
                }
                TextButton(onClick = onEdit) { Text("Edit") }
                if (onArchive != null) TextButton(onClick = onArchive) { Text("Archive") }
                Button(onClick = onOpen) { Text("Open profile") }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun PipelineBoard(viewModel: AppViewModel, nav: NavController) {
    val scope = rememberCoroutineScope()
    var pipeline by remember { mutableStateOf<PipelineResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    val todayStr = remember { java.time.LocalDate.now().toString() }

    var banner by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    LaunchedEffect(reloadKey) {
        error = null
        try {
            pipeline = viewModel.api.pipeline()
        } catch (e: Exception) {
            val msg = plainError(e, "The pipeline could not be loaded. Try again.")
            if (pipeline == null) error = msg else banner = msg
        } finally {
            refreshing = false
        }
    }

    fun move(client: Client, stage: String) {
        // hunt16: optimistic pipeline move — the card lands in
        // the new stage at once; on failure the board rolls back
        // and the banner says so plainly (never a full-screen
        // error over a loaded board).
        val previous = pipeline
        if (previous != null) {
            val moved = client.copy(stage = stage)
            pipeline = previous.copy(
                stages = previous.stages.map { st ->
                    when {
                        st.stage == stage && st.stage == client.stage ->
                            st.copy(clients = st.clients.map { if (it.id == client.id) moved else it })
                        st.stage == stage ->
                            st.copy(clients = st.clients + moved, count = st.count + 1)
                        st.stage == client.stage ->
                            st.copy(
                                clients = st.clients.filter { it.id != client.id },
                                count = (st.count - 1).coerceAtLeast(0),
                            )
                        else -> st
                    }
                },
            )
        }
        scope.launch {
            try {
                viewModel.api.setClientStage(client.id, stage)
                reloadKey++
            } catch (e: Exception) {
                pipeline = previous
                banner = plainError(e, "That stage move could not be saved — the client was put back. Try again.")
            }
        }
    }

    when {
        error != null && pipeline == null -> ErrorBox(error!!) { reloadKey++ }
        pipeline == null -> SkeletonList(6)
        else -> {
            val stages = pipeline!!.stages
            if (banner != null) {
                ErrorBanner(banner!!, onRetry = { banner = null; reloadKey++ }, onDismiss = { banner = null })
            }
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { refreshing = true; reloadKey++ },
            ) {
            LazyColumn {
                MirrorVocab.CLIENT_STAGES.forEach { stageName ->
                    val stageData = stages.firstOrNull { it.stage == stageName }
                    val cards = MirrorVocab.sortPipelineCards(stageData?.clients ?: emptyList())
                    val sum = cards.sumOf { it.value ?: 0.0 }
                    item {
                        SectionHeader(
                            "$stageName · ${cards.size} client(s) · ${MirrorVocab.formatDollars(sum)}",
                        )
                    }
                    if (cards.isEmpty()) {
                        item {
                            Text(
                                "No clients in this stage",
                                color = SentinelMuted,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    }
                    itemsIndexed(cards) { index, client ->
                        Column {
                            ClientCard(
                                entranceIndex = index,
                                client = client,
                                todayStr = todayStr,
                                onOpen = { nav.navigate("client/${client.id}") },
                                onEdit = { nav.navigate("clientEdit/${client.id}") },
                            )
                            Row(
                                Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ChoiceMenu("Pipeline stage", client.stage, MirrorVocab.CLIENT_STAGES) {
                                    move(client, it)
                                }
                                Spacer(Modifier.weight(1f))
                                val stageIdx = MirrorVocab.CLIENT_STAGES.indexOf(client.stage)
                                TextButton(
                                    onClick = {
                                        if (stageIdx > 0) {
                                            move(client, MirrorVocab.CLIENT_STAGES[stageIdx - 1])
                                        }
                                    },
                                    enabled = stageIdx > 0,
                                ) { Text("‹") }
                                TextButton(onClick = { nav.navigate("client/${client.id}") }) {
                                    Text("Open profile")
                                }
                                TextButton(
                                    onClick = {
                                        if (stageIdx in 0 until MirrorVocab.CLIENT_STAGES.lastIndex) {
                                            move(client, MirrorVocab.CLIENT_STAGES[stageIdx + 1])
                                        }
                                    },
                                    enabled = stageIdx in 0 until MirrorVocab.CLIENT_STAGES.lastIndex,
                                ) { Text("›") }
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

@Composable
fun ClientProfileScreen(viewModel: AppViewModel, nav: NavController, clientId: Int) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var detail by remember { mutableStateOf<ClientDetailResponse?>(null) }
    var timeline by remember { mutableStateOf<ClientTimelineResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(clientId, reloadKey) {
        error = null
        try {
            detail = viewModel.api.clientDetail(clientId)
            timeline = try {
                viewModel.api.clientTimeline(clientId)
            } catch (e: Exception) {
                null
            }
        } catch (e: Exception) {
            error = plainError(e, "This client could not be loaded. Go back and try again.")
        }
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        TextButton(onClick = { nav.popBackStack() }) { Text("← Back to directory") }
        when {
            error != null -> ErrorBox(error!!) { reloadKey++ }
            detail == null -> SkeletonList(4)
            else -> {
                val d = detail!!
                val c = d.client
                // hunt18: the profile header becomes a SurfaceTop
                // banner card with a 56dp monogram disc — same
                // name, same sub-line, same chips beneath.
                androidx.compose.material3.Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = Color.Transparent,
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, net.sentinelsafeguard.app.ui.theme.SentinelOutline
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(sentinelCardBrush(SentinelSurfaceTop), RoundedCornerShape(16.dp)),
                ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MonogramDisc(c.legalName, 56.dp)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(c.legalName, style = MaterialTheme.typography.titleLarge)
                            Text(
                                listOfNotNull(
                                    c.dba.takeIf { it.isNotBlank() },
                                    c.contactName.takeIf { it.isNotBlank() } ?: "No contact named",
                                ).joinToString(" · "),
                                color = SentinelMuted,
                            )
                        }
                    }
                    Row(
                        Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        MirrorChip(c.stage)
                        if (c.familyLinked) MirrorChip("Family")
                        else if (c.friends) MirrorChip("Friends — 20% off")
                        MirrorVocab.TIER_LABELS[MirrorVocab.tierOf(c)]?.let { MirrorChip(it) }
                    }
                    c.tags?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    if (actionError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(actionError!!, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ChoiceMenu("Pipeline stage", c.stage, MirrorVocab.CLIENT_STAGES) { picked ->
                            scope.launch {
                                try {
                                    viewModel.api.setClientStage(c.id, picked)
                                    reloadKey++
                                } catch (e: Exception) {
                                    actionError = plainError(e, "That stage move could not be saved. Try again.")
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { nav.navigate("clientEdit/${c.id}") }) { Text("Edit client") }
                        OutlinedButton(onClick = {
                            scope.launch {
                                try {
                                    viewModel.api.archiveClient(c.id)
                                    nav.popBackStack()
                                } catch (e: Exception) {
                                    actionError = plainError(e, "The client could not be archived. Try again.")
                                }
                            }
                        }) { Text("Archive") }
                    }
                }
                }

                MirrorCard("Contact") {
                    DetailLine("Contact", c.contactName)
                    DetailLine("Title", c.contactTitle)
                    DetailLine("Email", c.email)
                    DetailLine("Phone", c.phone)
                    DetailLine(
                        "Address",
                        listOf(c.street, c.city, c.state, c.zip)
                            .filter { it.isNotBlank() }.joinToString(", ").ifBlank { null },
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (c.phone.isNotBlank()) {
                            Button(onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:${c.phone}"))
                                )
                            }) { Text("Call client") }
                        }
                        if (c.email.isNotBlank()) {
                            Button(onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${c.email}"))
                                )
                            }) { Text("Email client") }
                        }
                    }
                    Text(
                        "Permanent deletion is owner-only via Archive → Archive view.",
                        color = SentinelMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                MirrorCard("Client details") {
                    DetailLine("Legal name", c.legalName)
                    DetailLine("DBA / company", c.dba)
                    DetailLine("Type", c.type)
                    DetailLine("Relationship status", c.status)
                    DetailLine("Pipeline stage", c.stage)
                    DetailLine("Industry", c.industry)
                    DetailLine(
                        "Pipeline / contract value",
                        MirrorVocab.formatDollars(c.value).ifBlank { null },
                    )
                    DetailLine("Next follow-up", c.nextFollowUp)
                    DetailLine(
                        "Source",
                        when {
                            c.source.isBlank() && c.sourceDetail.isBlank() -> "Not recorded"
                            c.sourceDetail.isNotBlank() -> "${c.source} — ${c.sourceDetail}"
                            else -> c.source
                        },
                    )
                    DetailLine("Tags", c.tags)
                    DetailLine("Created", if (c.createdAt > 0) formatEpochMs(c.createdAt) else null)
                    DetailLine("Last updated", if (c.updatedAt > 0) formatEpochMs(c.updatedAt) else null)
                    DetailLine("Notes", c.notes)
                }

                ClientDepthPart1(viewModel, c.id)

                MirrorCard("Sentinel Verified", d.verifications.size) {
                    if (d.verifications.isEmpty()) {
                        Text("No verifications issued yet.", color = SentinelMuted)
                    }
                    d.verifications.forEach { v ->
                        Text(
                            listOfNotNull(
                                v.category.takeIf { it.isNotBlank() },
                                v.status.takeIf { it.isNotBlank() },
                                v.issuedDate?.takeIf { it.isNotBlank() },
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                    TextButton(onClick = { nav.navigate("verified") }) {
                        Text("Issue verification")
                    }
                }

                ClientDepthPart2(viewModel, nav, c.id)

                MirrorCard("Timeline", timeline?.total) {
                    val entries = timeline?.timeline ?: emptyList()
                    if (entries.isEmpty()) {
                        Text("No timeline entries yet.", color = SentinelMuted)
                    }
                    entries.forEach { entry ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(entry.title, style = MaterialTheme.typography.bodyMedium)
                                if (entry.detail.isNotBlank()) {
                                    Text(entry.detail, color = SentinelMuted)
                                }
                                Text(
                                    if (entry.at > 0) formatEpochMs(entry.at) else entry.date,
                                    color = SentinelMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            if (entry.view != null && entry.refId != null) {
                                TextButton(onClick = {
                                    nav.navigate(timelineRoute(entry.view!!, entry.refId!!))
                                }) {
                                    Text("Open")
                                }
                            }
                        }
                    }
                }

                MirrorCard("Cases", d.cases.size) {
                    if (d.cases.isEmpty()) Text("No cases.", color = SentinelMuted)
                    d.cases.forEach { case ->
                        ItemRow(
                            case.title,
                            listOf(case.status, case.priority, case.openedDate)
                                .filter { it.isNotBlank() }.joinToString(" · "),
                        ) { nav.navigate("case/${case.id}") }
                    }
                }
                MirrorCard("Tickets", d.tickets.size) {
                    if (d.tickets.isEmpty()) Text("No tickets.", color = SentinelMuted)
                    d.tickets.forEach { ticket ->
                        ItemRow(
                            ticket.title,
                            listOf(ticket.status, ticket.priority)
                                .filter { it.isNotBlank() }.joinToString(" · "),
                        ) { nav.navigate("ticket/${ticket.id}") }
                    }
                }
                MirrorCard("Assets", d.assets.size) {
                    if (d.assets.isEmpty()) Text("No assets.", color = SentinelMuted)
                    d.assets.forEach { asset ->
                        ItemRow(
                            asset.name,
                            listOfNotNull(
                                asset.type.takeIf { it.isNotBlank() },
                                asset.identifier?.takeIf { it.isNotBlank() },
                                asset.reviewDueDate?.takeIf { it.isNotBlank() }
                                    ?.let { "Review $it" },
                            ).joinToString(" · "),
                        ) { nav.navigate("asset/${asset.id}") }
                    }
                }
                MirrorCard("Invoices", d.invoices.size) {
                    if (d.invoices.isEmpty()) Text("No invoices.", color = SentinelMuted)
                    d.invoices.forEach { invoice ->
                        ItemRow(
                            invoice.invoiceNumber.ifBlank { "Invoice #${invoice.id}" },
                            listOfNotNull(
                                invoice.status.takeIf { it.isNotBlank() },
                                if (invoice.overdue) "OVERDUE" else null,
                                MirrorVocab.formatCents(invoice.totalCents),
                                invoice.dueDate.takeIf { it.isNotBlank() }?.let { "Due $it" },
                            ).joinToString(" · "),
                        ) { nav.navigate("invoice/${invoice.id}") }
                    }
                }
                MirrorCard("Forms", d.forms.size) {
                    if (d.forms.isEmpty()) Text("No forms.", color = SentinelMuted)
                    d.forms.forEach { form ->
                        ItemRow(
                            form.title,
                            listOfNotNull(
                                form.type.takeIf { it.isNotBlank() },
                                if (form.createdAt > 0) formatEpochMs(form.createdAt) else null,
                            ).joinToString(" · "),
                        ) { nav.navigate("forms") }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ClientFormScreen(viewModel: AppViewModel, nav: NavController, clientId: Int?) {
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var legalName by remember { mutableStateOf("") }
    var dba by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Corporate") }
    var status by remember { mutableStateOf("Prospect") }
    var stage by remember { mutableStateOf("Lead") }
    var valueText by remember { mutableStateOf("") }
    var nextFollowUp by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var sourceDetail by remember { mutableStateOf("") }
    var industry by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var contactTitle by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var zip by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var friends by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(clientId) {
        if (clientId == null) {
            loaded = true
            return@LaunchedEffect
        }
        try {
            val c = viewModel.api.clientDetail(clientId).client
            legalName = c.legalName; dba = c.dba
            type = c.type.ifBlank { "Corporate" }
            status = c.status.ifBlank { "Prospect" }
            stage = c.stage
            valueText = c.value?.toString() ?: ""
            nextFollowUp = c.nextFollowUp ?: ""
            source = c.source; sourceDetail = c.sourceDetail; industry = c.industry
            contactName = c.contactName; contactTitle = c.contactTitle
            email = c.email; phone = c.phone
            street = c.street; city = c.city; state = c.state; zip = c.zip
            tags = c.tags ?: ""; friends = c.friends; notes = c.notes
            loaded = true
        } catch (e: Exception) {
            loadError = plainError(e, "The form could not be loaded. Go back and try again.")
        }
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        MirrorHeader(if (clientId == null) "New client" else "Edit client")
        when {
            loadError != null -> ErrorBox(loadError!!)
            !loaded -> SkeletonList(4)
            else -> Column(Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = legalName,
                    onValueChange = { legalName = it; nameError = null },
                    label = { Text("Legal name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (nameError != null) Text(nameError!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = dba, onValueChange = { dba = it },
                    label = { Text("DBA") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ChoiceMenu("Type", type, MirrorVocab.CLIENT_TYPES) { type = it }
                    ChoiceMenu("Status", status, MirrorVocab.CLIENT_STATUSES) { status = it }
                }
                Spacer(Modifier.height(10.dp))
                ChoiceMenu("Pipeline stage", stage, MirrorVocab.CLIENT_STAGES) { stage = it }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = valueText, onValueChange = { valueText = it },
                    label = { Text("Pipeline / contract value ($)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                DateFieldRow("Next follow-up", nextFollowUp) { nextFollowUp = it }
                Spacer(Modifier.height(10.dp))
                ChoiceMenu(
                    "How did they find us?",
                    source.ifBlank { "(blank)" },
                    listOf("(blank)") + MirrorVocab.CLIENT_SOURCES,
                ) { source = if (it == "(blank)") "" else it }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = sourceDetail, onValueChange = { sourceDetail = it },
                    label = { Text("Referred by (name / detail)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = industry, onValueChange = { industry = it },
                    label = { Text("Industry") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = contactName, onValueChange = { contactName = it },
                    label = { Text("Contact name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = contactTitle, onValueChange = { contactTitle = it },
                    label = { Text("Contact title") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Email") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it },
                    label = { Text("Phone") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = street, onValueChange = { street = it },
                    label = { Text("Street") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = city, onValueChange = { city = it },
                        label = { Text("City") }, singleLine = true,
                        modifier = Modifier.weight(2f),
                    )
                    OutlinedTextField(
                        value = state, onValueChange = { state = it },
                        label = { Text("State") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = zip, onValueChange = { zip = it },
                        label = { Text("ZIP") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = tags, onValueChange = { tags = it },
                    label = { Text("Tags (comma list)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = friends, onCheckedChange = { friends = it })
                    Text(
                        "Friends tier — 20% off all prices (Family records are never " +
                            "charged and take precedence)"
                    )
                }
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                if (saveError != null) {
                    Text(saveError!!, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = !busy,
                        onClick = {
                            if (legalName.isBlank()) {
                                nameError = "Legal name is required."
                            } else {
                                busy = true
                                scope.launch {
                                    try {
                                        val request = ClientWriteRequest(
                                            legalName = legalName.trim(),
                                            dba = dba, type = type, status = status,
                                            stage = stage,
                                            value = valueText.toDoubleOrNull(),
                                            nextFollowUp = nextFollowUp.ifBlank { null },
                                            source = source, sourceDetail = sourceDetail,
                                            industry = industry,
                                            contactName = contactName, contactTitle = contactTitle,
                                            email = email, phone = phone,
                                            street = street, city = city, state = state, zip = zip,
                                            tags = tags, friends = friends, notes = notes,
                                        )
                                        val saved = if (clientId == null) {
                                            viewModel.api.createClient(request)
                                        } else {
                                            viewModel.api.patchClient(clientId, request)
                                        }
                                        nav.popBackStack()
                                        if (clientId == null) nav.navigate("client/${saved.id}")
                                    } catch (e: Exception) {
                                        busy = false
                                        saveError = plainError(e, "The client could not be saved. Try again.")
                                    }
                                }
                            }
                        },
                    ) { Text(if (busy) "Saving…" else "Save client") }
                    OutlinedButton(onClick = { nav.popBackStack() }) { Text("Cancel") }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** Timeline Open targets: every Command view is built as of
 *  Phase 9 (hunt14), so a timeline row routes to its record or
 *  to the named view itself; an unknown name lands on Today
 *  rather than a dead end. */
private fun timelineRoute(view: String, refId: Int): String = when (view) {
    "cases" -> "case/$refId"
    "tickets" -> "ticket/$refId"
    "clients" -> "client/$refId"
    "invoices" -> "invoice/$refId"
    "assets" -> "asset/$refId"
    "engagements" -> "engagement/$refId"
    "knowledge" -> "knowledge/$refId"
    else -> MirrorNav.ALL_VIEWS[view]?.key ?: "today"
}

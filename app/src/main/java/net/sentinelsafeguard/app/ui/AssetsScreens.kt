package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.data.api.archiveAsset
import net.sentinelsafeguard.app.data.api.assetDetail
import net.sentinelsafeguard.app.data.api.assetLabels
import net.sentinelsafeguard.app.data.api.assetRenewals
import net.sentinelsafeguard.app.data.api.assetUrlLabels
import net.sentinelsafeguard.app.data.api.assets
import net.sentinelsafeguard.app.data.api.createAsset
import net.sentinelsafeguard.app.data.api.deleteAsset
import net.sentinelsafeguard.app.data.api.editAsset
import net.sentinelsafeguard.app.data.api.ensureAssetQr
import net.sentinelsafeguard.app.data.api.regenerateAssetQr
import net.sentinelsafeguard.app.data.api.restoreAsset
import net.sentinelsafeguard.app.data.api.setAssetStatus
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * Assets (Phase 5): web's Board | Directory modes over the two
 * registers (Sentinel Equipment / Client assets), the profile
 * (details / flags / renewals / QR label), create/edit, status
 * moves, archive/restore, OWNER-only delete, and the Renewals
 * surface whose flagged/renewals payloads are Today's own
 * derivations (they reconcile with the Today cards by
 * construction — Addendum 18 battery).
 */

private enum class AssetMode { Directory, Board, Renewals, Labels }

@Composable
fun AssetsScreen(viewModel: AppViewModel, nav: NavController) {
    var mode by remember { mutableStateOf(AssetMode.Directory) }
    Column {
        MirrorHeader("Assets", MirrorCopy.subtitle("assets")) {
            Button(onClick = { nav.navigate("assetNew") }) { Text("+ New asset") }
        }
        Row(Modifier.padding(horizontal = 16.dp)) {
            AssetMode.values().forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = { mode = m },
                    label = { Text(m.name) },
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        }
        when (mode) {
            AssetMode.Directory -> AssetsDirectory(viewModel, nav, board = false)
            AssetMode.Board -> AssetsDirectory(viewModel, nav, board = true)
            AssetMode.Renewals -> AssetRenewalsSurface(viewModel, nav)
            AssetMode.Labels -> AssetLabelsSurface(viewModel)
        }
    }
}

@Composable
private fun AssetsDirectory(viewModel: AppViewModel, nav: NavController, board: Boolean) {
    var query by remember { mutableStateOf("") }
    var type by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var scopeFilter by remember { mutableStateOf<String?>(null) }
    var alert by remember { mutableStateOf("All") }
    var sortLabel by remember { mutableStateOf("Name A–Z") }
    var showArchived by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    var data by remember { mutableStateOf<JsonObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val sortParam = Phase5Logic.ASSET_SORTS.firstOrNull { it.first == sortLabel }?.second ?: "name"
    LaunchedEffect(query, type, status, scopeFilter, alert, sortParam, showArchived, reloadKey) {
        try {
            data = viewModel.api.assets(
                q = query, type = type, status = status, scope = scopeFilter,
                alert = alert, sort = sortParam, archived = showArchived,
            )
            error = null
        } catch (e: Exception) {
            error = plainError(e, "Couldn't load assets. Try again.")
        }
    }
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Name, type, serial/tag, owner, or asset #") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
        if (!board) {
            Row(Modifier.padding(horizontal = 16.dp)) {
                Text("Sort assets", color = SentinelMuted, modifier = Modifier.padding(end = 8.dp))
                ChoiceMenu("Sort assets", sortLabel, Phase5Logic.ASSET_SORTS.map { it.first }) {
                    sortLabel = it
                }
            }
            Row(Modifier.padding(horizontal = 16.dp)) {
                Phase5Logic.ASSET_ALERT_FILTERS.forEach { (label, param) ->
                    FilterChip(
                        selected = alert == param,
                        onClick = { alert = param },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
            }
            val types = Phase3Logic.arr(data, "types")
            if (types.isNotEmpty()) {
                Row(Modifier.padding(horizontal = 16.dp)) {
                    FilterChip(
                        selected = type == null,
                        onClick = { type = null },
                        label = { Text("All types") },
                        modifier = Modifier.padding(end = 6.dp),
                    )
                    types.forEach { t ->
                        val name = Phase3Logic.str(t, "type")
                        FilterChip(
                            selected = type == name,
                            onClick = { type = name },
                            label = { Text("$name ${Phase3Logic.int(t, "count") ?: 0}") },
                            modifier = Modifier.padding(end = 6.dp),
                        )
                    }
                }
            }
            Row(Modifier.padding(horizontal = 16.dp)) {
                FilterChip(
                    selected = status == null,
                    onClick = { status = null },
                    label = { Text("All statuses") },
                    modifier = Modifier.padding(end = 6.dp),
                )
                Phase5Logic.ASSET_STATUSES.forEach { s ->
                    FilterChip(
                        selected = status == s,
                        onClick = { status = s },
                        label = { Text(s) },
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
            }
            Row(Modifier.padding(horizontal = 16.dp)) {
                FilterChip(
                    selected = scopeFilter == null,
                    onClick = { scopeFilter = null },
                    label = { Text("All scopes") },
                    modifier = Modifier.padding(end = 6.dp),
                )
                Phase5Logic.ASSET_SCOPES.forEach { (label, param) ->
                    FilterChip(
                        selected = scopeFilter == param,
                        onClick = { scopeFilter = param },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
                FilterChip(
                    selected = showArchived,
                    onClick = { showArchived = !showArchived },
                    label = { Text("Archived") },
                )
            }
            TextButton(
                onClick = {
                    query = ""; type = null; status = null; scopeFilter = null
                    alert = "All"; sortLabel = "Name A–Z"; showArchived = false
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            ) { Text("Reset filters") }
        }
        when {
            error != null -> ErrorBox(error!!) { reloadKey++ }
            data == null -> SkeletonList()
            else -> {
                val stats = Phase3Logic.obj(data, "stats")
                if (stats != null && !board) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        StatCard("Total", "${Phase3Logic.int(stats, "total") ?: 0}", Modifier.weight(1f))
                        StatCard("Sentinel", "${Phase3Logic.int(stats, "sentinel") ?: 0}", Modifier.weight(1f))
                        StatCard("Client", "${Phase3Logic.int(stats, "client") ?: 0}", Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        StatCard("Needs attention", "${Phase3Logic.int(stats, "attention") ?: 0}", Modifier.weight(1f))
                        StatCard("Reviews overdue", "${Phase3Logic.int(stats, "reviewsOverdue") ?: 0}", Modifier.weight(1f))
                        StatCard("Warranty expiring", "${Phase3Logic.int(stats, "warrantyExpiring") ?: 0}", Modifier.weight(1f))
                    }
                }
                val rows = Phase3Logic.arr(data, "assets")
                if (rows.isEmpty()) {
                    EmptyBox("No matches", "No assets match the current search and filters.")
                }
                LazyColumn {
                    if (board) {
                        val statuses = (Phase5Logic.ASSET_STATUSES + rows.map { Phase3Logic.str(it, "status") })
                            .distinct()
                        statuses.forEach { st ->
                            val inStatus = rows.filter { Phase3Logic.str(it, "status") == st }
                            item { SectionHeader("$st — ${inStatus.size} asset(s)") }
                            if (inStatus.isEmpty()) {
                                item {
                                    Text(
                                        "No assets in this status",
                                        color = SentinelMuted,
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                    )
                                }
                            }
                            items(inStatus) { asset -> AssetRow(viewModel, nav, asset) { reloadKey++ } }
                        }
                    } else {
                        items(rows) { asset -> AssetRow(viewModel, nav, asset) { reloadKey++ } }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AssetRow(
    viewModel: AppViewModel,
    nav: NavController,
    asset: JsonObject,
    onChanged: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val id = Phase3Logic.int(asset, "id") ?: return
    val flags = Phase5Logic.flagLabels(Phase3Logic.obj(asset, "flags"))
    val earliest = Phase3Logic.obj(asset, "earliestRenewal")
    ItemRow(
        title = Phase3Logic.str(asset, "name"),
        subtitle = listOfNotNull(
            Phase3Logic.strOrNull(asset, "ownerName"),
            Phase3Logic.strOrNull(asset, "type"),
            Phase3Logic.strOrNull(asset, "identifier"),
            Phase3Logic.strOrNull(asset, "status"),
            flags.takeIf { it.isNotEmpty() }?.joinToString(", "),
            earliest?.let { Phase5Logic.renewalLabel(it) },
        ).joinToString(" · "),
        onClick = { nav.navigate("asset/$id") },
    )
    Row(Modifier.padding(horizontal = 16.dp)) {
        ChoiceMenu("Status", Phase3Logic.str(asset, "status"), Phase5Logic.ASSET_STATUSES) { picked ->
            scope.launch {
                try {
                    viewModel.api.setAssetStatus(id, picked)
                    onChanged()
                } catch (e: Exception) {
                    // Status refusals surface on reload; the row keeps
                    // the server's status until then.
                }
            }
        }
    }
}

@Composable
private fun AssetRenewalsSurface(viewModel: AppViewModel, nav: NavController) {
    val state = rememberJsonScreen { viewModel.api.assetRenewals() }
    when {
        state.error != null && state.data == null -> ErrorBox(state.error!!) { state.reloadKey++ }
        state.data == null -> SkeletonList()
        else -> {
            val data = state.data!!
            val flagged = Phase3Logic.obj(data, "flagged")
            val renewals = Phase3Logic.obj(data, "renewals")
            LazyColumn {
                item { SectionHeader("Assets flagged — warranty / review", flagged?.let { Phase3Logic.int(it, "total") }) }
                items(Phase3Logic.arr(flagged, "items")) { asset ->
                    val flagText = Phase3Logic.arrRaw(asset, "flags").mapNotNull { el ->
                        (el as? JsonObject)?.let {
                            "${Phase5Logic.renewalFieldLabel(Phase3Logic.str(it, "field"))} " +
                                "${Phase3Logic.str(it, "state")} ${Phase3Logic.str(it, "date")}"
                        }
                    }.joinToString(", ")
                    ItemRow(
                        title = Phase3Logic.str(asset, "name"),
                        subtitle = listOfNotNull(
                            Phase3Logic.strOrNull(asset, "type"),
                            flagText.takeIf { it.isNotBlank() },
                        ).joinToString(" · "),
                        onClick = {
                            Phase3Logic.int(asset, "id")?.let { nav.navigate("asset/$it") }
                        },
                    )
                }
                item { SectionHeader("Asset renewals", renewals?.let { Phase3Logic.int(it, "total") }) }
                listOf("within30" to "Within 30 days", "within60" to "Within 60 days", "within90" to "Within 90 days")
                    .forEach { (key, label) ->
                        val bucket = Phase3Logic.obj(renewals, key)
                        val bucketRows = Phase3Logic.arr(bucket, "items")
                        if (bucketRows.isNotEmpty()) {
                            item { SectionHeader(label, Phase3Logic.int(bucket, "total")) }
                            items(bucketRows) { asset ->
                                val earliest = Phase3Logic.obj(asset, "earliest")
                                ItemRow(
                                    title = Phase3Logic.str(asset, "name"),
                                    subtitle = listOfNotNull(
                                        Phase3Logic.strOrNull(asset, "type"),
                                        earliest?.let { Phase5Logic.renewalLabel(it) },
                                    ).joinToString(" · "),
                                    onClick = {
                                        Phase3Logic.int(asset, "id")?.let { nav.navigate("asset/$it") }
                                    },
                                )
                            }
                        }
                    }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun AssetLabelsSurface(viewModel: AppViewModel) {
    val context = LocalContext.current
    val labelsState = rememberJsonScreen { viewModel.api.assetLabels() }
    val urlState = rememberJsonScreen { viewModel.api.assetUrlLabels() }
    LazyColumn {
        item { SectionHeader("Print labels") }
        when {
            labelsState.error != null && labelsState.data == null -> item { ErrorBox(labelsState.error!!) { labelsState.reloadKey++ } }
            labelsState.data == null -> item { SkeletonList(3) }
            else -> items(Phase3Logic.arr(labelsState.data, "labels")) { label ->
                MirrorCard(Phase3Logic.str(label, "name")) {
                    JsonLines(
                        label,
                        "type" to "Type",
                        "identifier" to "Identifier / serial",
                        "code" to "QR code",
                        "payload" to "QR payload",
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = {
                        shareText(
                            context, Phase3Logic.str(label, "name"),
                            Phase3Logic.str(label, "svg"),
                        )
                    }) { Text("Share label SVG") }
                }
            }
        }
        item { SectionHeader("URL labels") }
        when {
            urlState.error != null && urlState.data == null -> item { ErrorBox(urlState.error!!) { urlState.reloadKey++ } }
            urlState.data == null -> item { SkeletonList(3) }
            else -> {
                item {
                    val format = Phase3Logic.str(urlState.data, "urlFormat")
                    if (format.isNotBlank()) {
                        Text(
                            "URL format: $format",
                            color = SentinelMuted,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
                items(Phase3Logic.arr(urlState.data, "labels")) { label ->
                    MirrorCard(Phase3Logic.str(label, "name")) {
                        JsonLines(
                            label,
                            "type" to "Type",
                            "serial" to "Identifier / serial",
                            "url" to "URL",
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(onClick = {
                            shareText(
                                context, Phase3Logic.str(label, "name"),
                                Phase3Logic.str(label, "svg"),
                            )
                        }) { Text("Share label SVG") }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun AssetProfileScreen(viewModel: AppViewModel, nav: NavController, assetId: Int) {
    val state = rememberJsonScreen { viewModel.api.assetDetail(assetId) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var actionError by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var qrPayload by remember { mutableStateOf<String?>(null) }
    var qrSvg by remember { mutableStateOf<String?>(null) }
    val run: (suspend () -> Unit) -> Unit = { block ->
        scope.launch {
            try {
                block()
                state.reloadKey++
            } catch (e: Exception) {
                actionError = plainError(e, "That could not be done. Try again.")
            }
        }
    }
    Column {
        MirrorHeader("Assets") {
            OutlinedButton(onClick = { nav.popBackStack() }) { Text("← Back") }
        }
        when {
            state.error != null && state.data == null -> ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null -> SkeletonList()
            else -> {
                val data = state.data!!
                val asset = Phase3Logic.obj(data, "asset") ?: data
                val archived = Phase3Logic.strOrNull(asset, "archivedAt") != null
                LazyColumn {
                    item {
                        MirrorCard(Phase3Logic.str(asset, "name")) {
                            JsonLines(
                                asset,
                                "type" to "Type",
                                "identifier" to "Identifier / serial",
                                "scope" to "Register",
                                "status" to "Status",
                                "reviewDueDate" to "Review due date",
                                "warrantyUntil" to "Warranty until",
                                "notes" to "Notes",
                            )
                            DetailLine(
                                "Created",
                                Phase3Logic.displayValue("createdAt", asset["createdAt"]),
                            )
                            ActionErrorText(actionError)
                            Spacer(Modifier.height(8.dp))
                            Row {
                                Button(
                                    onClick = { nav.navigate("assetEdit/$assetId") },
                                    modifier = Modifier.padding(end = 8.dp),
                                ) { Text("Edit asset") }
                                if (archived) {
                                    OutlinedButton(onClick = { run { viewModel.api.restoreAsset(assetId) } }) {
                                        Text("Restore")
                                    }
                                } else {
                                    OutlinedButton(onClick = { run { viewModel.api.archiveAsset(assetId) } }) {
                                        Text("Archive")
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("Status", color = SentinelMuted, style = MaterialTheme.typography.labelLarge)
                            ChoiceMenu(
                                "Status",
                                Phase3Logic.str(asset, "status"),
                                Phase5Logic.ASSET_STATUSES,
                            ) { picked -> run { viewModel.api.setAssetStatus(assetId, picked) } }
                        }
                    }
                    item {
                        val flags = Phase5Logic.flagLabels(Phase3Logic.obj(data, "flags"))
                        MirrorCard("Flags & renewals") {
                            if (flags.isEmpty()) {
                                Text("No flags on this asset.", color = SentinelMuted)
                            }
                            flags.forEach { MirrorChip(it) }
                            Phase3Logic.arr(data, "renewals").forEach { renewal ->
                                DetailLine("Renewal", Phase5Logic.renewalLabel(renewal))
                            }
                        }
                    }
                    val client = Phase3Logic.obj(data, "client")
                    if (client != null) {
                        item {
                            MirrorCard("Owner") {
                                JsonLines(client, "legalName" to "Client", "contactName" to "Contact")
                                val cid = Phase3Logic.int(client, "id")
                                if (cid != null) {
                                    OutlinedButton(onClick = { nav.navigate("client/$cid") }) {
                                        Text("Open client profile")
                                    }
                                }
                            }
                        }
                    }
                    item {
                        MirrorCard("QR label") {
                            JsonLines(asset, "qrCode" to "QR code")
                            if (qrPayload != null) {
                                DetailLine("QR payload", qrPayload)
                            }
                            Row {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                val out = viewModel.api.ensureAssetQr(assetId)
                                                qrPayload = Phase3Logic.str(out, "payload")
                                                qrSvg = Phase3Logic.str(out, "svg")
                                            } catch (e: Exception) {
                                                actionError = plainError(e, "The QR code could not be loaded.")
                                            }
                                        }
                                    },
                                    modifier = Modifier.padding(end = 8.dp),
                                ) { Text("Ensure QR") }
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        try {
                                            val out = viewModel.api.regenerateAssetQr(assetId)
                                            qrPayload = Phase3Logic.str(out, "payload")
                                            qrSvg = Phase3Logic.str(out, "svg")
                                            state.reloadKey++
                                        } catch (e: Exception) {
                                            actionError = plainError(e, "The QR code could not be regenerated.")
                                        }
                                    }
                                }) { Text("Regenerate QR") }
                            }
                            if (qrSvg != null) {
                                Spacer(Modifier.height(6.dp))
                                OutlinedButton(onClick = {
                                    shareText(context, "Asset label", qrSvg!!)
                                }) { Text("Share label SVG") }
                            }
                        }
                    }
                    val activity = Phase3Logic.arr(data, "activity")
                    if (activity.isNotEmpty()) {
                        item {
                            MirrorCard("Asset activity", activity.size) {
                                activity.forEach { row ->
                                    Text("${Phase3Logic.str(row, "action")} — ${Phase3Logic.str(row, "detail")}")
                                    Text(
                                        Phase3Logic.displayValue("createdAt", row["createdAt"]) ?: "",
                                        color = SentinelMuted,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Column(Modifier.padding(16.dp)) {
                            OutlinedButton(onClick = { confirmDelete = true }) { Text("Delete") }
                            Text(
                                "Delete is OWNER-only, as in web Command.",
                                color = SentinelMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        ConfirmGate(
            title = "Delete asset",
            body = "This permanently deletes this asset. This cannot be undone.",
            confirmLabel = "Delete",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                run {
                    viewModel.api.deleteAsset(assetId)
                    nav.popBackStack()
                }
            },
        )
    }
}

@Composable
fun AssetFormScreen(viewModel: AppViewModel, nav: NavController, assetId: Int?) {
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(assetId == null) }
    var scopeLabel by remember { mutableStateOf("Sentinel Equipment") }
    var clientIdText by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("In Service") }
    var assignedText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var identifier by remember { mutableStateOf("") }
    var reviewDue by remember { mutableStateOf("") }
    var warrantyUntil by remember { mutableStateOf("") }
    var warrantyExpires by remember { mutableStateOf("") }
    var supportExpires by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var clients by remember { mutableStateOf<List<net.sentinelsafeguard.app.data.api.Client>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(assetId) {
        try {
            clients = viewModel.api.clients().items
        } catch (e: Exception) {
            // The client picker is best-effort; the id field remains.
        }
        if (assetId != null) {
            try {
                val data = viewModel.api.assetDetail(assetId)
                val asset = Phase3Logic.obj(data, "asset") ?: data
                scopeLabel = if (Phase3Logic.str(asset, "scope") == "Client") "Client asset" else "Sentinel Equipment"
                clientIdText = Phase3Logic.int(asset, "clientId")?.toString() ?: ""
                name = Phase3Logic.str(asset, "name")
                status = Phase3Logic.str(asset, "status").ifBlank { "In Service" }
                assignedText = Phase3Logic.int(asset, "assignedUserId")?.toString() ?: ""
                type = Phase3Logic.str(asset, "type")
                identifier = Phase3Logic.str(asset, "identifier")
                reviewDue = Phase3Logic.str(asset, "reviewDueDate")
                warrantyUntil = Phase3Logic.str(asset, "warrantyUntil")
                warrantyExpires = Phase3Logic.str(asset, "warrantyExpires")
                supportExpires = Phase3Logic.str(asset, "supportExpires")
                notes = Phase3Logic.str(asset, "notes")
            } catch (e: Exception) {
                error = plainError(e, "Couldn't load this asset. Try again.")
            } finally {
                loaded = true
            }
        }
    }
    LazyColumn {
        item { MirrorHeader(if (assetId == null) "New asset" else "Edit asset") }
        if (!loaded) {
            item { SkeletonList(3) }
        } else {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text("Register", color = SentinelMuted, style = MaterialTheme.typography.labelLarge)
                    ChoiceMenu(
                        "Register", scopeLabel,
                        Phase5Logic.ASSET_SCOPES.map { it.first },
                    ) { scopeLabel = it }
                    val scopeParam = Phase5Logic.ASSET_SCOPES.firstOrNull { it.first == scopeLabel }?.second
                        ?: "Sentinel"
                    if (scopeParam == "Client") {
                        if (clients.isNotEmpty()) {
                            val current = clients.firstOrNull { it.id.toString() == clientIdText }
                            ChoiceMenu(
                                "Client",
                                current?.legalName ?: "Choose client",
                                clients.map { it.legalName },
                            ) { picked ->
                                clientIdText = clients.firstOrNull { it.legalName == picked }?.id?.toString() ?: ""
                            }
                        }
                        TextFieldRow("Client ID", clientIdText, onChange = { clientIdText = it })
                    }
                    TextFieldRow("Asset name", name, onChange = { name = it })
                    Text("Status", color = SentinelMuted, style = MaterialTheme.typography.labelLarge)
                    ChoiceMenu("Status", status, Phase5Logic.ASSET_STATUSES) { status = it }
                    TextFieldRow("Assigned to (user ID)", assignedText, onChange = { assignedText = it })
                    TextFieldRow("Type", type, onChange = { type = it })
                    TextFieldRow("Identifier / serial", identifier, onChange = { identifier = it })
                    DateFieldRow("Review due date", reviewDue, onChange = { reviewDue = it })
                    TextFieldRow("Warranty until", warrantyUntil, onChange = { warrantyUntil = it })
                    DateFieldRow("Warranty expires (renewal watch)", warrantyExpires, onChange = { warrantyExpires = it })
                    DateFieldRow("Support expires (renewal watch)", supportExpires, onChange = { supportExpires = it })
                    TextFieldRow("Notes", notes, onChange = { notes = it }, singleLine = false, minLines = 3)
                    ActionErrorText(error)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = {
                        scope.launch {
                            try {
                                val item = buildJsonObject {
                                    put("scope", scopeParam)
                                    if (scopeParam == "Client") {
                                        clientIdText.toIntOrNull()?.let { put("clientId", it) }
                                    }
                                    put("name", name)
                                    put("status", status)
                                    assignedText.toIntOrNull()?.let { put("assignedUserId", it) }
                                    put("type", type)
                                    put("identifier", identifier)
                                    put("reviewDueDate", reviewDue.ifBlank { null })
                                    put("warrantyUntil", warrantyUntil.ifBlank { null })
                                    put("warrantyExpires", warrantyExpires.ifBlank { null })
                                    put("supportExpires", supportExpires.ifBlank { null })
                                    put("notes", notes)
                                }
                                if (assetId == null) {
                                    viewModel.api.createAsset(item)
                                } else {
                                    // Web edit order rides the backend's
                                    // PATCH (save seeded, then status).
                                    viewModel.api.editAsset(assetId, item)
                                }
                                nav.popBackStack()
                            } catch (e: Exception) {
                                error = plainError(e, "That could not be saved. Try again.")
                            }
                        }
                    }) { Text("Save") }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

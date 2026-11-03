package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.data.api.assuranceBackupTests
import net.sentinelsafeguard.app.data.api.assuranceDisposals
import net.sentinelsafeguard.app.data.api.assuranceFileRequests
import net.sentinelsafeguard.app.data.api.assuranceFirmDocuments
import net.sentinelsafeguard.app.data.api.assuranceKbShares
import net.sentinelsafeguard.app.data.api.assurancePackets
import net.sentinelsafeguard.app.data.api.assuranceScans
import net.sentinelsafeguard.app.data.api.attestAssuranceDisposal
import net.sentinelsafeguard.app.data.api.completeAssuranceDisposal
import net.sentinelsafeguard.app.data.api.createAssuranceKbShare
import net.sentinelsafeguard.app.data.api.deleteAssuranceFirmDocument
import net.sentinelsafeguard.app.data.api.importScanFindings
import net.sentinelsafeguard.app.data.api.issueAssurancePacket
import net.sentinelsafeguard.app.data.api.revokeAssuranceKbShare
import net.sentinelsafeguard.app.data.api.saveAssuranceBackupTest
import net.sentinelsafeguard.app.data.api.saveAssuranceFileRequest
import net.sentinelsafeguard.app.data.api.saveAssuranceFirmDocument
import net.sentinelsafeguard.app.data.api.saveAssurancePacket
import net.sentinelsafeguard.app.data.api.saveAssuranceScan
import net.sentinelsafeguard.app.data.api.setBackupCadence
import net.sentinelsafeguard.app.data.api.startAssuranceDisposal
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * Documents & Assurance (cf2assurance, Phase 3): web's sections
 * in web's order — Document packets, Client file requests,
 * Backup restore-test log (+ per-asset cadence), Vulnerability
 * scan register (behind web's signed-authorization hard gate:
 * the server refuses until a signed authorization is on file,
 * and the refusal is surfaced in the server's own words),
 * asset disposals, the firm's own document vault, KB shares —
 * plus the Exports card (G-C9 payloads).
 */
@Composable
fun AssuranceScreen(viewModel: AppViewModel, nav: NavController) {
    var clients by remember { mutableStateOf<List<net.sentinelsafeguard.app.data.api.Client>>(emptyList()) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        clients = try { viewModel.api.clients().items } catch (e: Exception) { emptyList() }
    }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        MirrorHeader("Documents & Assurance")
        PacketsCard(viewModel, clients)
        FileRequestsCard(viewModel, clients)
        BackupTestsCard(viewModel)
        ScansCard(viewModel, clients)
        DisposalsCard(viewModel)
        FirmDocumentsCard(viewModel)
        KbSharesCard(viewModel)
        ExportsCard(viewModel)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ClientPicker(
    clients: List<net.sentinelsafeguard.app.data.api.Client>,
    selectedId: Int?,
    onPick: (Int) -> Unit,
) {
    if (clients.isEmpty()) {
        Text("Loading clients…", color = SentinelMuted)
        return
    }
    val selected = clients.firstOrNull { it.id == selectedId } ?: clients.first()
    ChoiceMenu("Client", selected.legalName, clients.map { it.legalName }) { picked ->
        clients.firstOrNull { it.legalName == picked }?.let { onPick(it.id) }
    }
}

@Composable
private fun PacketsCard(
    viewModel: AppViewModel,
    clients: List<net.sentinelsafeguard.app.data.api.Client>,
) {
    val scope = rememberCoroutineScope()
    var clientId by remember { mutableStateOf<Int?>(null) }
    var title by remember { mutableStateOf("") }
    var pickedKb by remember { mutableStateOf(setOf<Int>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    AsyncCard("Document packets", "packets", loader = { viewModel.api.assurancePackets() }) { data, reload ->
        val packets = Phase3Logic.arr(data, "packets")
        val kbItems = Phase3Logic.arr(data, "kbItems")
        if (packets.isEmpty()) Text("No packets yet.", color = SentinelMuted)
        packets.forEach { packet ->
            val pid = Phase3Logic.int(packet, "id")
            Text(
                "${Phase3Logic.str(packet, "title")} · ${Phase3Logic.str(packet, "state")} · ${Phase3Logic.str(packet, "itemCount")} item(s)",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 2.dp),
            )
            if (Phase3Logic.str(packet, "state") == "draft" && pid != null) {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            val out = viewModel.api.issueAssurancePacket(pid)
                            notice = Phase3Logic.str(out, "message"); reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The packet could not be issued. Try again.")
                        }
                    }
                }) { Text("Issue to portal") }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Save packet as draft", style = MaterialTheme.typography.titleSmall)
        ClientPicker(clients, clientId) { clientId = it }
        TextFieldRow("Packet title", title, { title = it })
        kbItems.forEach { item ->
            val kid = Phase3Logic.int(item, "id") ?: return@forEach
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = kid in pickedKb,
                    onCheckedChange = { on ->
                        pickedKb = if (on) pickedKb + kid else pickedKb - kid
                    },
                )
                Text(Phase3Logic.str(item, "title"), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Button(
            enabled = title.isNotBlank() && (clientId != null || clients.isNotEmpty()),
            onClick = {
                val cid = clientId ?: clients.firstOrNull()?.id ?: return@Button
                scope.launch {
                    try {
                        viewModel.api.saveAssurancePacket(
                            cid, title,
                            JsonArray(pickedKb.map { kid ->
                                buildJsonObject { put("kind", "kb"); put("id", kid) }
                            }),
                        )
                        title = ""; pickedKb = emptySet(); reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The packet could not be saved. Try again.")
                    }
                }
            },
        ) { Text("Save packet as draft") }
        if (notice != null) Text(notice!!, color = SentinelMuted)
        ActionErrorText(error)
    }
}

@Composable
private fun FileRequestsCard(
    viewModel: AppViewModel,
    clients: List<net.sentinelsafeguard.app.data.api.Client>,
) {
    val scope = rememberCoroutineScope()
    var clientId by remember { mutableStateOf<Int?>(null) }
    var title by remember { mutableStateOf("") }
    var itemLabel by remember { mutableStateOf("") }
    var required by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    AsyncCard("Client file requests", "requests", loader = { viewModel.api.assuranceFileRequests() }) { data, reload ->
        val requests = Phase3Logic.arr(data, "requests")
        if (requests.isEmpty()) Text("No file requests yet.", color = SentinelMuted)
        requests.forEach { request ->
            Text(
                Phase3Logic.str(request, "title"),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Phase3Logic.arr(request, "items").forEach { item ->
                Text(
                    "· ${Phase3Logic.str(item, "label")} — ${Phase3Logic.str(item, "status")}" +
                        if (Phase3Logic.bool(item, "required") == true) " (required)" else "",
                    color = SentinelMuted,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Create request", style = MaterialTheme.typography.titleSmall)
        ClientPicker(clients, clientId) { clientId = it }
        TextFieldRow("Request title", title, { title = it })
        TextFieldRow("First item label", itemLabel, { itemLabel = it })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = required, onCheckedChange = { required = it })
            Text("Required")
        }
        Button(
            enabled = title.isNotBlank() && itemLabel.isNotBlank(),
            onClick = {
                val cid = clientId ?: clients.firstOrNull()?.id ?: return@Button
                scope.launch {
                    try {
                        viewModel.api.saveAssuranceFileRequest(
                            cid, title,
                            JsonArray(listOf(buildJsonObject {
                                put("label", itemLabel); put("required", required)
                            })),
                        )
                        title = ""; itemLabel = ""; reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The request could not be created. Try again.")
                    }
                }
            },
        ) { Text("Create request") }
        ActionErrorText(error)
    }
}

@Composable
private fun BackupTestsCard(viewModel: AppViewModel) {
    val scope = rememberCoroutineScope()
    var assetId by remember { mutableStateOf("") }
    var testedAt by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("full_restore") }
    var result by remember { mutableStateOf("pass") }
    var notes by remember { mutableStateOf("") }
    var cadenceDays by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AsyncCard("Backup restore-test log", "tests", loader = { viewModel.api.assuranceBackupTests() }) { data, reload ->
        val methods = Phase3Logic.arrRaw(data, "methods")
            .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
        val results = Phase3Logic.arrRaw(data, "results")
            .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
        if (methods.isNotEmpty() && method !in methods) method = methods.first()
        if (results.isNotEmpty() && result !in results) result = results.first()
        val states = Phase3Logic.arr(data, "assetStates")
        states.forEach { state ->
            Text(
                "${Phase3Logic.str(state, "assetName")} · cadence ${Phase3Logic.str(state, "cadenceDays")}d · " +
                    "last ${Phase3Logic.str(state, "lastTestedAt")} (${Phase3Logic.str(state, "lastResult")}) · ${Phase3Logic.str(state, "status")}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 2.dp),
            )
        }
        val tests = Phase3Logic.arr(data, "tests")
        if (tests.isEmpty()) Text("No backup tests logged.", color = SentinelMuted)
        tests.forEach { test ->
            Text(
                "${Phase3Logic.str(test, "assetName")} · ${Phase3Logic.str(test, "testedAt")} · " +
                    "${Phase3Logic.str(test, "methodLabel")} · ${Phase3Logic.str(test, "result")}",
                color = SentinelMuted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 1.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text("Log test", style = MaterialTheme.typography.titleSmall)
        TextFieldRow("Asset id", assetId, { assetId = it })
        TextFieldRow("Tested at (yyyy-MM-dd)", testedAt, { testedAt = it })
        if (methods.isNotEmpty()) ChoiceMenu("Method", method, methods) { method = it }
        if (results.isNotEmpty()) ChoiceMenu("Result", result, results) { result = it }
        TextFieldRow("Notes", notes, { notes = it })
        Button(
            enabled = assetId.toIntOrNull() != null && testedAt.isNotBlank(),
            onClick = {
                scope.launch {
                    try {
                        viewModel.api.saveAssuranceBackupTest(buildJsonObject {
                            put("assetId", assetId.toInt()); put("testedAt", testedAt)
                            put("method", method); put("result", result); put("notes", notes)
                        })
                        notes = ""; reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The test could not be logged. Try again.")
                    }
                }
            },
        ) { Text("Log test") }
        Spacer(Modifier.height(8.dp))
        TextFieldRow("Cadence days (for the asset above)", cadenceDays, { cadenceDays = it })
        Button(
            enabled = assetId.toIntOrNull() != null && cadenceDays.toIntOrNull() != null,
            onClick = {
                scope.launch {
                    try {
                        viewModel.api.setBackupCadence(assetId.toInt(), cadenceDays.toInt())
                        error = null
                        reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The cadence could not be saved. Try again.")
                    }
                }
            },
        ) { Text("Set backup cadence") }
        ActionErrorText(error)
    }
}

@Composable
private fun ScansCard(
    viewModel: AppViewModel,
    clients: List<net.sentinelsafeguard.app.data.api.Client>,
) {
    val scope = rememberCoroutineScope()
    var clientId by remember { mutableStateOf<Int?>(null) }
    var target by remember { mutableStateOf("") }
    var scanDate by remember { mutableStateOf("") }
    var tool by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var importFor by remember { mutableStateOf<Int?>(null) }
    var csvText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var gateNote by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    AsyncCard("Vulnerability scan register", "scans", loader = { viewModel.api.assuranceScans() }) { data, reload ->
        Text(
            "Scans record only behind the signed-authorization gate: until a signed " +
                "authorization is on file for the client, the server refuses — and the " +
                "refusal below is the server's own wording, exactly as web shows it.",
            color = SentinelMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        val scans = Phase3Logic.arr(data, "scans")
        if (scans.isEmpty()) Text("No scans recorded.", color = SentinelMuted)
        scans.forEach { scan ->
            val sid = Phase3Logic.int(scan, "id")
            Text(
                "${Phase3Logic.str(scan, "clientName")} · ${Phase3Logic.str(scan, "target")} · " +
                    "${Phase3Logic.str(scan, "tool")} · ${Phase3Logic.str(scan, "scanDate")}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(Phase3Logic.str(scan, "summary"), color = SentinelMuted)
            Phase3Logic.arr(scan, "findings").forEach { finding ->
                Text(
                    "· ${Phase3Logic.str(finding, "title")} (${Phase3Logic.str(finding, "severity")}) — ${Phase3Logic.str(finding, "host")}",
                    color = SentinelMuted,
                )
            }
            if (sid != null) {
                TextButton(onClick = { importFor = sid }) { Text("Import findings (CSV)") }
            }
        }
        if (importFor != null) {
            TextFieldRow("CSV findings (title,severity,host,detail)", csvText, { csvText = it }, singleLine = false, minLines = 4)
            Button(
                enabled = csvText.isNotBlank(),
                onClick = {
                    val sid = importFor ?: return@Button
                    scope.launch {
                        try {
                            val out = viewModel.api.importScanFindings(sid, csvText)
                            notice = Phase3Logic.str(out, "message")
                            csvText = ""; importFor = null; reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The import could not be done. Try again.")
                        }
                    }
                },
            ) { Text("Import findings") }
        }
        Spacer(Modifier.height(8.dp))
        Text("Record scan", style = MaterialTheme.typography.titleSmall)
        ClientPicker(clients, clientId) { clientId = it }
        TextFieldRow("Target", target, { target = it })
        TextFieldRow("Scan date (yyyy-MM-dd)", scanDate, { scanDate = it })
        TextFieldRow("Tool", tool, { tool = it })
        TextFieldRow("Summary", summary, { summary = it })
        Button(
            enabled = target.isNotBlank() && (clientId != null || clients.isNotEmpty()),
            onClick = {
                val cid = clientId ?: clients.firstOrNull()?.id ?: return@Button
                scope.launch {
                    try {
                        viewModel.api.saveAssuranceScan(buildJsonObject {
                            put("clientId", cid); put("target", target)
                            put("scanDate", scanDate); put("tool", tool); put("summary", summary)
                        })
                        gateNote = null; target = ""; summary = ""; reload()
                    } catch (e: Exception) {
                        val msg = plainError(e, "The scan could not be recorded. Try again.")
                        if (Phase3Logic.isGateRefusal(msg)) gateNote = msg else error = msg
                    }
                }
            },
        ) { Text("Record scan") }
        if (gateNote != null) {
            Spacer(Modifier.height(6.dp))
            Text(gateNote!!, color = MaterialTheme.colorScheme.error)
        }
        if (notice != null) Text(notice!!, color = SentinelMuted)
        ActionErrorText(error)
    }
}

@Composable
private fun DisposalsCard(viewModel: AppViewModel) {
    val scope = rememberCoroutineScope()
    var assetId by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("wipe") }
    var reason by remember { mutableStateOf("") }
    var attestation by remember { mutableStateOf("") }
    var attestFor by remember { mutableStateOf<Int?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    AsyncCard("Asset disposals", "disposals", loader = { viewModel.api.assuranceDisposals() }) { data, reload ->
        val methods = Phase3Logic.arrRaw(data, "methods")
            .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
        if (methods.isNotEmpty() && method !in methods) method = methods.first()
        val disposals = Phase3Logic.arr(data, "disposals")
        if (disposals.isEmpty()) Text("No disposals yet.", color = SentinelMuted)
        disposals.forEach { disposal ->
            val did = Phase3Logic.int(disposal, "id")
            Text(
                "${Phase3Logic.str(disposal, "assetName")} · ${Phase3Logic.str(disposal, "method")} · ${Phase3Logic.str(disposal, "state")}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            val cert = Phase3Logic.str(disposal, "certCode")
            if (cert.isNotBlank()) Text("Certificate $cert", color = SentinelMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (Phase3Logic.str(disposal, "state") != "completed" && did != null) {
                    TextButton(onClick = { attestFor = did }) { Text("Attest") }
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                viewModel.api.completeAssuranceDisposal(did); reload()
                            } catch (e: Exception) {
                                error = plainError(e, "The disposal could not be completed. Try again.")
                            }
                        }
                    }) { Text("Complete") }
                }
            }
        }
        if (attestFor != null) {
            TextFieldRow("Attestation", attestation, { attestation = it }, singleLine = false, minLines = 2)
            Button(
                enabled = attestation.isNotBlank(),
                onClick = {
                    val did = attestFor ?: return@Button
                    scope.launch {
                        try {
                            viewModel.api.attestAssuranceDisposal(did, attestation)
                            attestation = ""; attestFor = null; reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The attestation could not be recorded. Try again.")
                        }
                    }
                },
            ) { Text("Record attestation") }
        }
        Spacer(Modifier.height(8.dp))
        Text("Start disposal", style = MaterialTheme.typography.titleSmall)
        TextFieldRow("Asset id", assetId, { assetId = it })
        if (methods.isNotEmpty()) ChoiceMenu("Method", method, methods) { method = it }
        TextFieldRow("Reason", reason, { reason = it })
        Button(
            enabled = assetId.toIntOrNull() != null,
            onClick = {
                scope.launch {
                    try {
                        viewModel.api.startAssuranceDisposal(assetId.toInt(), method, reason)
                        assetId = ""; reason = ""; reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The disposal could not be started. Try again.")
                    }
                }
            },
        ) { Text("Start disposal") }
        ActionErrorText(error)
    }
}

@Composable
private fun FirmDocumentsCard(viewModel: AppViewModel) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("other") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AsyncCard("Firm documents", "documents", loader = { viewModel.api.assuranceFirmDocuments() }) { data, reload ->
        val kinds = Phase3Logic.arrRaw(data, "kinds")
            .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
        if (kinds.isNotEmpty() && kind !in kinds) kind = kinds.first()
        val documents = Phase3Logic.arr(data, "documents")
        if (documents.isEmpty()) Text("No firm documents yet.", color = SentinelMuted)
        documents.forEach { document ->
            val did = Phase3Logic.int(document, "id")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${Phase3Logic.str(document, "title")} · ${Phase3Logic.str(document, "kind")}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (did != null) {
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                viewModel.api.deleteAssuranceFirmDocument(did); reload()
                            } catch (e: Exception) {
                                error = plainError(e, "The document could not be removed. Try again.")
                            }
                        }
                    }) { Text("Delete") }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Add vault document", style = MaterialTheme.typography.titleSmall)
        TextFieldRow("Title", title, { title = it })
        if (kinds.isNotEmpty()) ChoiceMenu("Kind", kind, kinds) { kind = it }
        TextFieldRow("Notes", notes, { notes = it })
        Button(
            enabled = title.isNotBlank(),
            onClick = {
                scope.launch {
                    try {
                        viewModel.api.saveAssuranceFirmDocument(buildJsonObject {
                            put("title", title); put("kind", kind); put("notes", notes)
                        })
                        title = ""; notes = ""; reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The document could not be saved. Try again.")
                    }
                }
            },
        ) { Text("Add vault document") }
        ActionErrorText(error)
    }
}

@Composable
private fun KbSharesCard(viewModel: AppViewModel) {
    val scope = rememberCoroutineScope()
    var kbId by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("7") }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    AsyncCard("KB shares", "shares", loader = { viewModel.api.assuranceKbShares() }) { data, reload ->
        val shares = Phase3Logic.arr(data, "shares")
        if (shares.isEmpty()) Text("No KB shares yet.", color = SentinelMuted)
        shares.forEach { share ->
            val sid = Phase3Logic.int(share, "id")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${Phase3Logic.str(share, "kbTitle")} · ${Phase3Logic.str(share, "state")} · ${Phase3Logic.str(share, "accessCount")} access(es)",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (sid != null && Phase3Logic.bool(share, "revoked") != true) {
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                viewModel.api.revokeAssuranceKbShare(sid); reload()
                            } catch (e: Exception) {
                                error = plainError(e, "The share could not be revoked. Try again.")
                            }
                        }
                    }) { Text("Revoke") }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Create share", style = MaterialTheme.typography.titleSmall)
        TextFieldRow("Knowledge entry id", kbId, { kbId = it })
        TextFieldRow("Expires in days", days, { days = it })
        Button(
            enabled = kbId.toIntOrNull() != null,
            onClick = {
                scope.launch {
                    try {
                        val out = viewModel.api.createAssuranceKbShare(
                            kbId.toInt(), days.toIntOrNull() ?: 7,
                        )
                        notice = Phase3Logic.str(out, "message")
                        kbId = ""; reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The share could not be created. Try again.")
                    }
                }
            },
        ) { Text("Create share link") }
        if (notice != null) Text(notice!!, color = SentinelMuted)
        ActionErrorText(error)
    }
}

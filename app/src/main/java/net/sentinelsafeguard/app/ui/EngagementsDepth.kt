package net.sentinelsafeguard.app.ui

import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.data.api.Engagement
import net.sentinelsafeguard.app.data.api.addFindingEvidence
import net.sentinelsafeguard.app.data.api.contractDocument
import net.sentinelsafeguard.app.data.api.contracts
import net.sentinelsafeguard.app.data.api.countersignContract
import net.sentinelsafeguard.app.data.api.createContract
import net.sentinelsafeguard.app.data.api.createFinding
import net.sentinelsafeguard.app.data.api.createFindingFromTemplate
import net.sentinelsafeguard.app.data.api.deleteFindingTemplate
import net.sentinelsafeguard.app.data.api.editFinding
import net.sentinelsafeguard.app.data.api.engagementReportDocument
import net.sentinelsafeguard.app.data.api.findingTemplates
import net.sentinelsafeguard.app.data.api.saveContractTemplate
import net.sentinelsafeguard.app.data.api.saveFindingTemplate
import net.sentinelsafeguard.app.data.api.sendContract
import net.sentinelsafeguard.app.data.api.setFindingStatus
import net.sentinelsafeguard.app.data.api.verifyContractHash
import net.sentinelsafeguard.app.data.api.voidContract
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * Engagements depth (Phase 2 remainder, on the Phase 3 backend):
 * web's Contracts table + contract templates + Findings library
 * (frontend inventory §2.1 engagements), the contract document
 * surface, and finding create/edit/status/evidence — findings
 * stay behind web's signed-authorization hard gate, surfaced in
 * the server's own words.
 */

@Composable
fun ContractsCard(viewModel: AppViewModel, nav: NavController) {
    val scope = rememberCoroutineScope()
    var clients by remember { mutableStateOf<List<net.sentinelsafeguard.app.data.api.Client>>(emptyList()) }
    var clientId by remember { mutableStateOf<Int?>(null) }
    var title by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var editTemplate by remember { mutableStateOf<Int?>(null) }
    var tplTitle by remember { mutableStateOf("") }
    var tplBody by remember { mutableStateOf("") }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        clients = try { viewModel.api.clients().items } catch (e: Exception) { emptyList() }
    }
    AsyncCard("Contracts", "contracts", loader = { viewModel.api.contracts() }) { data, reload ->
        val contracts = Phase3Logic.arr(data, "contracts")
        val templates = Phase3Logic.arr(data, "templates")
        val types = Phase3Logic.arrRaw(data, "contractTypes")
            .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
        if (contracts.isEmpty()) Text("No contracts yet.", color = SentinelMuted)
        contracts.forEach { contract ->
            val cid = Phase3Logic.int(contract, "id")
            ItemRow(
                Phase3Logic.str(contract, "title").ifBlank { "Contract" },
                listOfNotNull(
                    Phase3Logic.str(contract, "clientName").takeIf { it.isNotBlank() },
                    Phase3Logic.str(contract, "templateType").takeIf { it.isNotBlank() },
                    Phase3Logic.str(contract, "status").takeIf { it.isNotBlank() },
                ).joinToString(" · "),
            ) { if (cid != null) nav.navigate("contract/$cid") }
        }
        Spacer(Modifier.height(8.dp))
        Text("New contract from template", style = MaterialTheme.typography.titleSmall)
        if (clients.isEmpty()) {
            Text("Loading clients…", color = SentinelMuted)
        } else {
            val selected = clients.firstOrNull { it.id == clientId } ?: clients.first()
            val clientOpts = MirrorVocab.clientOptions(clients)
            ChoiceMenu("Client", MirrorVocab.clientOptionLabel(clients, selected), clientOpts) { picked ->
                clientId = MirrorVocab.clientIdAt(clients, clientOpts, picked)
            }
        }
        var templateType by remember { mutableStateOf(types.firstOrNull() ?: "authorization") }
        if (types.isNotEmpty() && templateType !in types) templateType = types.first()
        if (types.isNotEmpty()) {
            ChoiceMenu("Template", templateType, types) { templateType = it }
        }
        TextFieldRow("Contract title", title, { title = it })
        Button(
            enabled = title.isNotBlank() && clients.isNotEmpty(),
            onClick = {
                val cid = clientId ?: clients.firstOrNull()?.id ?: return@Button
                scope.launch {
                    try {
                        viewModel.api.createContract(cid, templateType, title)
                        title = ""; reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The contract could not be created. Try again.")
                    }
                }
            },
        ) { Text("New contract") }

        Spacer(Modifier.height(12.dp))
        Text("Contract templates", style = MaterialTheme.typography.titleSmall)
        templates.forEach { template ->
            val tid = Phase3Logic.int(template, "id")
            Text(
                "${Phase3Logic.str(template, "title")} · ${Phase3Logic.str(template, "templateType")}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            TextButton(onClick = {
                editTemplate = tid
                tplTitle = Phase3Logic.str(template, "title")
                tplBody = Phase3Logic.str(template, "body")
            }) { Text("Edit body") }
        }
        if (editTemplate != null) {
            TextFieldRow("Template title", tplTitle, { tplTitle = it })
            TextFieldRow("Template body", tplBody, { tplBody = it }, singleLine = false, minLines = 6)
            Button(onClick = {
                val tid = editTemplate ?: return@Button
                scope.launch {
                    try {
                        viewModel.api.saveContractTemplate(tid, tplTitle, tplBody)
                        editTemplate = null; reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The template could not be saved. Try again.")
                    }
                }
            }) { Text("Save template") }
        }
        if (notice != null) Text(notice!!, color = SentinelMuted)
        ActionErrorText(error)
    }
}

@Composable
fun ContractDetailScreen(viewModel: AppViewModel, nav: NavController, contractId: Int) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var signer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var confirmVoid by remember { mutableStateOf(false) }
    val state = rememberJsonScreen { viewModel.api.contracts() }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        TextButton(onClick = { nav.popBackStack() }) { Text("← Back to engagements") }
        when {
            state.error != null && state.data == null -> ErrorBox(state.error!!) { state.reloadKey++ }
            state.data == null -> SkeletonList(4)
            else -> {
                val contract = Phase3Logic.arr(state.data!!, "contracts")
                    .firstOrNull { Phase3Logic.int(it, "id") == contractId }
                MirrorHeader("Contract")
                if (contract == null) {
                    EmptyBox("Contract not found", "This contract is not in the contracts list. Go back and refresh.")
                } else {
                    MirrorCard("Contract details") {
                        JsonLines(
                            contract,
                            "title" to "Title", "clientName" to "Client",
                            "templateType" to "Type", "status" to "Status",
                            "createdAt" to "Created", "updatedAt" to "Last updated",
                        )
                    }
                    MirrorCard("Document") {
                        var document by remember { mutableStateOf<kotlinx.serialization.json.JsonObject?>(null) }
                        OutlinedButton(onClick = {
                            scope.launch {
                                try {
                                    document = viewModel.api.contractDocument(contractId)
                                } catch (e: Exception) {
                                    error = plainError(e, "The document could not be loaded. Try again.")
                                }
                            }
                        }) { Text("Load document") }
                        document?.let { doc ->
                            val pdf = Phase3Logic.str(doc, "pdfBase64")
                            val text = Phase3Logic.str(doc, "text")
                                .ifBlank { Phase3Logic.str(doc, "body") }
                            if (text.isNotBlank()) Text(text, style = MaterialTheme.typography.bodyMedium)
                            if (pdf.isNotBlank()) {
                                OutlinedButton(onClick = {
                                    val bytes = Base64.decode(pdf, Base64.DEFAULT)
                                    shareBytes(context, "contract-$contractId.pdf", "application/pdf", bytes)
                                }) { Text("PDF — share / save") }
                            }
                            if (pdf.isBlank() && text.isBlank()) {
                                Text(doc.toString().take(2000), color = SentinelMuted)
                            }
                        }
                    }
                    MirrorCard("Actions") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                scope.launch {
                                    try {
                                        val out = viewModel.api.verifyContractHash(contractId)
                                        notice = "Hash match: ${Phase3Logic.str(out, "match")}"
                                    } catch (e: Exception) {
                                        error = plainError(e, "The hash could not be verified. Try again.")
                                    }
                                }
                            }) { Text("Verify hash") }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    try {
                                        val out = viewModel.api.sendContract(contractId)
                                        notice = Phase3Logic.str(out, "message"); state.reloadKey++
                                    } catch (e: Exception) {
                                        error = plainError(e, "The contract could not be sent. Try again.")
                                    }
                                }
                            }) { Text("Send") }
                        }
                        TextFieldRow("Countersign — signer name", signer, { signer = it })
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                enabled = signer.isNotBlank(),
                                onClick = {
                                    scope.launch {
                                        try {
                                            val out = viewModel.api.countersignContract(contractId, signer.trim())
                                            notice = Phase3Logic.str(out, "message"); state.reloadKey++
                                        } catch (e: Exception) {
                                            error = plainError(e, "The contract could not be countersigned. Try again.")
                                        }
                                    }
                                },
                            ) { Text("Countersign") }
                            OutlinedButton(onClick = { confirmVoid = true }) { Text("Void") }
                        }
                        if (notice != null) Text(notice!!, color = SentinelMuted)
                        ActionErrorText(error)
                    }
                    if (confirmVoid) {
                        ConfirmGate(
                            "Void contract",
                            "Voiding a contract requires confirmation. The contract stays on file as void.",
                            "Void",
                            onDismiss = { confirmVoid = false },
                            onConfirm = {
                                confirmVoid = false
                                scope.launch {
                                    try {
                                        val out = viewModel.api.voidContract(contractId, confirm = true)
                                        notice = Phase3Logic.str(out, "message"); state.reloadKey++
                                    } catch (e: Exception) {
                                        error = plainError(e, "The contract could not be voided. Try again.")
                                    }
                                }
                            },
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** The findings depth inside the engagement profile: create,
 *  edit, status, evidence, insert-from-library, report download. */
@Composable
fun FindingsDepth(
    viewModel: AppViewModel,
    engagement: Engagement,
    onChanged: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("High") }
    var description by remember { mutableStateOf("") }
    var impact by remember { mutableStateOf("") }
    var remediation by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<Int?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var gateNote by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var evidenceFor by remember { mutableStateOf<Int?>(null) }

    val evidencePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        val fid = evidenceFor
        evidenceFor = null
        if (uri != null && fid != null) {
            scope.launch {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                        ?: throw IllegalStateException("unreadable")
                    val name = uri.lastPathSegment?.substringAfterLast('/') ?: "evidence"
                    val type = context.contentResolver.getType(uri) ?: "application/octet-stream"
                    viewModel.api.addFindingEvidence(
                        fid, name, type,
                        Base64.encodeToString(bytes, Base64.NO_WRAP), bytes.size,
                    )
                    notice = "Evidence attached."
                    onChanged()
                } catch (e: Exception) {
                    error = plainError(e, "The evidence could not be attached. Try again.")
                }
            }
        }
    }

    MirrorCard("Findings — add & edit") {
        if (!engagement.hasSignedAuthorization) {
            Text(
                "Findings cannot open until a signed authorization is on file.",
                color = SentinelMuted,
            )
            Spacer(Modifier.height(6.dp))
        }
        engagement.findings.forEach { finding ->
            val fid = finding.id
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    editingId = fid
                    title = finding.title
                    severity = finding.severity
                    description = finding.description.ifBlank { finding.detail }
                }) { Text("Edit") }
                if (fid != null) {
                    TextButton(onClick = { evidenceFor = fid; evidencePicker.launch("*/*") }) {
                        Text("Add evidence")
                    }
                }
            }
            if (fid != null) {
                ChoiceMenu("Status", finding.status, listOf("Open", "In Progress", "Resolved", "Accepted")) { picked ->
                    scope.launch {
                        try {
                            viewModel.api.setFindingStatus(fid, picked)
                            onChanged()
                        } catch (e: Exception) {
                            error = plainError(e, "The status could not be updated. Try again.")
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (editingId == null) "Add finding" else "Edit finding",
            style = MaterialTheme.typography.titleSmall,
        )
        TextFieldRow("Title", title, { title = it })
        ChoiceMenu("Severity", severity, listOf("Critical", "High", "Medium", "Low", "Informational")) {
            severity = it
        }
        TextFieldRow("Description", description, { description = it }, singleLine = false, minLines = 2)
        TextFieldRow("Impact", impact, { impact = it }, singleLine = false, minLines = 2)
        TextFieldRow("Remediation", remediation, { remediation = it }, singleLine = false, minLines = 2)
        Button(
            enabled = title.isNotBlank(),
            onClick = {
                scope.launch {
                    try {
                        val body = buildJsonObject {
                            put("title", title); put("severity", severity)
                            put("description", description); put("impact", impact)
                            put("remediation", remediation)
                        }
                        if (editingId == null) {
                            viewModel.api.createFinding(engagement.id, body)
                        } else {
                            viewModel.api.editFinding(editingId!!, body)
                        }
                        title = ""; description = ""; impact = ""; remediation = ""
                        editingId = null
                        onChanged()
                    } catch (e: Exception) {
                        val msg = plainError(e, "The finding could not be saved. Try again.")
                        if (Phase3Logic.isGateRefusal(msg)) gateNote = msg else error = msg
                    }
                }
            },
        ) { Text(if (editingId == null) "Add finding" else "Save finding") }
        if (gateNote != null) {
            Spacer(Modifier.height(6.dp))
            Text(gateNote!!, color = MaterialTheme.colorScheme.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                scope.launch {
                    try {
                        val doc = viewModel.api.engagementReportDocument(engagement.id)
                        val pdf = Phase3Logic.str(doc, "pdfBase64")
                        if (pdf.isNotBlank()) {
                            shareBytes(
                                context, "engagement-${engagement.id}-report.pdf",
                                "application/pdf", Base64.decode(pdf, Base64.DEFAULT),
                            )
                        } else {
                            shareText(context, "Engagement report", doc.toString())
                        }
                    } catch (e: Exception) {
                        val msg = plainError(e, "No report has been generated for this engagement yet.")
                        if (Phase3Logic.isGateRefusal(msg) || msg.contains("No report")) {
                            notice = msg
                        } else {
                            error = msg
                        }
                    }
                }
            }) { Text("Share report") }
        }
        if (notice != null) Text(notice!!, color = SentinelMuted)
        ActionErrorText(error)
    }
    FindingsLibraryCard(viewModel, engagement)
}

@Composable
private fun FindingsLibraryCard(viewModel: AppViewModel, engagement: Engagement) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("Critical") }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AsyncCard("Findings library", "templates", loader = { viewModel.api.findingTemplates() }) { data, reload ->
        val templates = Phase3Logic.arr(data, "templates")
        val severities = Phase3Logic.arrRaw(data, "severities")
            .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
        if (severities.isNotEmpty() && severity !in severities) severity = severities.first()
        if (templates.isEmpty()) Text("No finding templates yet.", color = SentinelMuted)
        templates.forEach { template ->
            val tid = Phase3Logic.int(template, "id")
            Text(
                "${Phase3Logic.str(template, "title")} · ${Phase3Logic.str(template, "defaultSeverity")}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    if (tid != null) scope.launch {
                        try {
                            viewModel.api.createFindingFromTemplate(engagement.id, tid)
                            reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The finding could not be inserted. Try again.")
                        }
                    }
                }) { Text("Insert from library") }
                TextButton(onClick = {
                    if (tid != null) scope.launch {
                        try {
                            viewModel.api.deleteFindingTemplate(tid); reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The template could not be deleted. Try again.")
                        }
                    }
                }) { Text("Delete template") }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Add template", style = MaterialTheme.typography.titleSmall)
        TextFieldRow("Title", title, { title = it })
        if (severities.isNotEmpty()) {
            ChoiceMenu("Default severity", severity, severities) { severity = it }
        }
        TextFieldRow("Description", description, { description = it }, singleLine = false, minLines = 2)
        Button(
            enabled = title.isNotBlank(),
            onClick = {
                scope.launch {
                    try {
                        viewModel.api.saveFindingTemplate(
                            buildJsonObject {
                                put("title", title); put("defaultSeverity", severity)
                                put("description", description)
                                put("impact", ""); put("remediation", ""); put("category", "")
                            },
                            null,
                        )
                        title = ""; description = ""; reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The template could not be saved. Try again.")
                    }
                }
            },
        ) { Text("Add template") }
        ActionErrorText(error)
    }
}

package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import net.sentinelsafeguard.app.data.api.appApiKeyCreate
import net.sentinelsafeguard.app.data.api.appApiKeyRevoke
import net.sentinelsafeguard.app.data.api.appDeliveriesRun
import net.sentinelsafeguard.app.data.api.appDeviceRename
import net.sentinelsafeguard.app.data.api.appDeviceRevoke
import net.sentinelsafeguard.app.data.api.appIntegrations
import net.sentinelsafeguard.app.data.api.appWebhookActive
import net.sentinelsafeguard.app.data.api.appWebhookCreate
import net.sentinelsafeguard.app.data.api.automationContractReminderScan
import net.sentinelsafeguard.app.data.api.automationContractReminders
import net.sentinelsafeguard.app.data.api.automationContractTerm
import net.sentinelsafeguard.app.data.api.automationEscalationConfig
import net.sentinelsafeguard.app.data.api.automationEscalationConfigSave
import net.sentinelsafeguard.app.data.api.automationEscalationScan
import net.sentinelsafeguard.app.data.api.automationEscalations
import net.sentinelsafeguard.app.data.api.automationQuoteFollowupScan
import net.sentinelsafeguard.app.data.api.automationQuoteFollowups
import net.sentinelsafeguard.app.data.api.automationRuleDelete
import net.sentinelsafeguard.app.data.api.automationRuleSave
import net.sentinelsafeguard.app.data.api.automationRules
import net.sentinelsafeguard.app.data.api.automationRulesApply
import net.sentinelsafeguard.app.data.api.clientImport
import net.sentinelsafeguard.app.data.api.clientImportConfirm
import net.sentinelsafeguard.app.data.api.clientImportPreview
import net.sentinelsafeguard.app.data.api.clientImports
import net.sentinelsafeguard.app.data.api.clientOpsEmergency
import net.sentinelsafeguard.app.data.api.clientOpsEmergencyDelete
import net.sentinelsafeguard.app.data.api.clientOpsEmergencySave
import net.sentinelsafeguard.app.data.api.clientOpsRetainerDraw
import net.sentinelsafeguard.app.data.api.clientOpsRetainerEntry
import net.sentinelsafeguard.app.data.api.clientOpsRetainerSave
import net.sentinelsafeguard.app.data.api.clientOpsRetainers
import net.sentinelsafeguard.app.data.api.clientOpsWindowDelete
import net.sentinelsafeguard.app.data.api.clientOpsWindowSave
import net.sentinelsafeguard.app.data.api.clientOpsWindows
import net.sentinelsafeguard.app.data.api.clientReports
import net.sentinelsafeguard.app.data.api.collectionsBoard
import net.sentinelsafeguard.app.data.api.collectionsWorkflow
import net.sentinelsafeguard.app.data.api.consumePart
import net.sentinelsafeguard.app.data.api.deskEvidencePack
import net.sentinelsafeguard.app.data.api.fieldCountApply
import net.sentinelsafeguard.app.data.api.fieldCountClose
import net.sentinelsafeguard.app.data.api.fieldCountScan
import net.sentinelsafeguard.app.data.api.fieldCountStart
import net.sentinelsafeguard.app.data.api.fieldCounts
import net.sentinelsafeguard.app.data.api.fieldImportConfirm
import net.sentinelsafeguard.app.data.api.fieldImportPreview
import net.sentinelsafeguard.app.data.api.fieldJobMode
import net.sentinelsafeguard.app.data.api.fieldLeadRoi
import net.sentinelsafeguard.app.data.api.fieldLeadScores
import net.sentinelsafeguard.app.data.api.fieldScan
import net.sentinelsafeguard.app.data.api.opsCapacity
import net.sentinelsafeguard.app.data.api.opsCapacityThreshold
import net.sentinelsafeguard.app.data.api.opsContractRadar
import net.sentinelsafeguard.app.data.api.opsRiskHeatmap
import net.sentinelsafeguard.app.data.api.opsTimeline
import net.sentinelsafeguard.app.data.api.packetBuild
import net.sentinelsafeguard.app.data.api.packetMarkMailed
import net.sentinelsafeguard.app.data.api.packetsClaimAttach
import net.sentinelsafeguard.app.data.api.packetsClaimFile
import net.sentinelsafeguard.app.data.api.packetsClaimReopen
import net.sentinelsafeguard.app.data.api.packetsClaimTransition
import net.sentinelsafeguard.app.data.api.packetsClaims
import net.sentinelsafeguard.app.data.api.packetsConfig
import net.sentinelsafeguard.app.data.api.packetsGeneratePack
import net.sentinelsafeguard.app.data.api.packetsList
import net.sentinelsafeguard.app.data.api.packetsOps
import net.sentinelsafeguard.app.data.api.packetsRoute
import net.sentinelsafeguard.app.data.api.packetsRouteOrder
import net.sentinelsafeguard.app.data.api.packetsUrgentAck
import net.sentinelsafeguard.app.data.api.parts
import net.sentinelsafeguard.app.data.api.pipelineChangeRequests
import net.sentinelsafeguard.app.data.api.pipelineChangeStatus
import net.sentinelsafeguard.app.data.api.pipelineChangeVerify
import net.sentinelsafeguard.app.data.api.pipelineLeadConvert
import net.sentinelsafeguard.app.data.api.pipelineLeadSave
import net.sentinelsafeguard.app.data.api.pipelineLeadState
import net.sentinelsafeguard.app.data.api.pipelineLeads
import net.sentinelsafeguard.app.data.api.pipelineProposalDocument
import net.sentinelsafeguard.app.data.api.pipelineProposalStatus
import net.sentinelsafeguard.app.data.api.pipelineProposals
import net.sentinelsafeguard.app.data.api.reportBriefDelete
import net.sentinelsafeguard.app.data.api.reportBriefGenerate
import net.sentinelsafeguard.app.data.api.reportBriefMarkSent
import net.sentinelsafeguard.app.data.api.reportBriefs
import net.sentinelsafeguard.app.data.api.reportDocument
import net.sentinelsafeguard.app.data.api.reportReferralSave
import net.sentinelsafeguard.app.data.api.reportReferrals
import net.sentinelsafeguard.app.data.api.reportScheduleRun
import net.sentinelsafeguard.app.data.api.reportScheduleSave
import net.sentinelsafeguard.app.data.api.reportSchedules
import net.sentinelsafeguard.app.data.api.reportSchedulesRunDue
import net.sentinelsafeguard.app.data.api.reportSnapshots
import net.sentinelsafeguard.app.data.api.reportSurveyCreate
import net.sentinelsafeguard.app.data.api.reportSurveys
import net.sentinelsafeguard.app.data.api.reportsGenerate
import net.sentinelsafeguard.app.data.api.reportsList
import net.sentinelsafeguard.app.data.api.restockPart
import net.sentinelsafeguard.app.data.api.savePart
import net.sentinelsafeguard.app.data.api.sendClientReport
import net.sentinelsafeguard.app.data.api.discardClientReport
import net.sentinelsafeguard.app.data.api.staffGearCertDelete
import net.sentinelsafeguard.app.data.api.staffGearCertSave
import net.sentinelsafeguard.app.data.api.staffGearDirectory
import net.sentinelsafeguard.app.data.api.staffGearEquipment
import net.sentinelsafeguard.app.data.api.staffGearEquipmentReturn
import net.sentinelsafeguard.app.data.api.staffGearEquipmentSave
import net.sentinelsafeguard.app.data.api.staffGearTraining
import net.sentinelsafeguard.app.data.api.staffGearTrainingComplete
import net.sentinelsafeguard.app.data.api.staffGearTrainingSave
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * The 12 nav-injected entries (Phase 8B) as full screens under
 * their exact Command names — a phone has no overlay sheets over
 * a sidebar, so web's six sheets mirror as screens with the same
 * names and content (spec §2.1).
 *
 * REUSE (Addendum 26): Parts & Inventory rides the Phase 3 parts
 * endpoints in full (the same surface the ticket profile's Parts
 * card uses); the Ops-Views evidence pack rides Phase 8A;
 * Pipeline & Field playbooks ride Phase 3 (the Runbooks surface);
 * Reports send/discard ride Phase 3's client-report routes.
 * Shown-once values (API keys, webhook secrets, survey tokens)
 * render in-session only, exactly as web, and are never stored.
 */
@Composable
fun InjectedScreen(viewModel: AppViewModel, nav: NavController, key: String) {
    val label = MirrorNav.ALL_VIEWS[key]?.label ?: "Command"
    Column {
        MirrorHeader(MirrorCopy.title(key, label), MirrorCopy.subtitle(key))
        LazyColumn {
            item {
                when (key) {
                    "b3cops" -> OpsViewsScreen(viewModel)
                    "b3dfield" -> PipelineFieldScreen(viewModel)
                    "parts" -> PartsScreen(viewModel)
                    "collections" -> CollectionsScreen(viewModel)
                    "staffgear" -> StaffGearScreen(viewModel)
                    "automation" -> AutomationScreen(viewModel)
                    "clientops" -> ClientOpsScreen(viewModel)
                    "pipelinepackets" -> PipelinePacketsScreen(viewModel)
                    "cf2reports" -> ReportsFeedbackScreen(viewModel)
                    "b4cadmin" -> AppIntegrationsScreen(viewModel)
                    "b4dimport" -> ClientImportScreen(viewModel)
                    "b4jops" -> PacketsRoutesScreen(viewModel)
                    else -> OpsViewsScreen(viewModel)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** Client picker shared by the client-scoped injected surfaces. */
@Composable
private fun ClientIdField(value: String, onChange: (String) -> Unit) {
    TextFieldRow("Client ID", value, onChange)
}

// ---- Ops Views (web b3cops) ----

@Composable
private fun OpsViewsScreen(viewModel: AppViewModel) {
    AsyncCard("Risk heatmap", "rows", loader = { viewModel.api.opsRiskHeatmap() }) { data, _ ->
        Phase3Logic.arr(data, "rows").forEach { row ->
            Text(Phase3Logic.str(row, "clientName"), style = MaterialTheme.typography.bodyLarge)
            val meta = listOfNotNull(
                Phase3Logic.str(row, "riskLevel").ifBlank { null }?.let { "Risk $it" },
                "Open tickets ${Phase3Logic.int(row, "openTickets") ?: 0}",
                Phase3Logic.dbl(row, "score")?.let { "Posture $it" },
                "Breach hits ${Phase3Logic.int(row, "breachHits") ?: 0}",
            ).joinToString(" · ")
            Text(meta, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
        }
    }
    MirrorCard("Evidence packs") {
        Text(
            "Compliance evidence packs ride the Verified Desk surface (Phase 8A), " +
                "exactly as web composes the shared handler.",
            color = SentinelMuted,
        )
    }
    AsyncCard("Timeline", loader = { viewModel.api.opsTimeline() }) { data, _ ->
        AnyLines(data, "events", "rows")
        AnyRows(data, "events", listOf("detail"))
        AnyRows(data, "rows", listOf("detail"))
    }
    AsyncCard("Contract radar", loader = { viewModel.api.opsContractRadar() }) { data, _ ->
        AnyLines(data, "rows", "contracts")
        AnyRows(data, "rows", listOf("detail"))
        AnyRows(data, "contracts", listOf("detail"))
    }
    AsyncCard("Capacity", loader = { viewModel.api.opsCapacity() }) { data, reload ->
        var threshold by remember(data) {
            mutableStateOf((Phase3Logic.dbl(data, "thresholdHours") ?: "").toString())
        }
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        DetailLine("Threshold hours", Phase3Logic.dbl(data, "thresholdHours")?.toString())
        Phase3Logic.arr(data, "weeks").forEach { week ->
            Text(
                "Week of ${Phase3Logic.str(week, "weekStart")}",
                style = MaterialTheme.typography.titleSmall,
            )
            Phase3Logic.arr(week, "staff").forEach { person ->
                Text(
                    "${Phase3Logic.str(person, "name")} · " +
                        "${Phase3Logic.dbl(person, "committedHours") ?: 0}h committed" +
                        if (Phase3Logic.bool(person, "overThreshold") == true) " · OVER" else "",
                )
            }
            Spacer(Modifier.height(6.dp))
        }
        TextFieldRow("Threshold hours", threshold, { threshold = it })
        Button(onClick = {
            threshold.toDoubleOrNull()?.let { act { viewModel.api.opsCapacityThreshold(it) } }
        }) { Text("Save threshold") }
        ActionErrorText(error)
    }
}

// ---- Pipeline & Field (web b3dfield) ----

@Composable
private fun PipelineFieldScreen(viewModel: AppViewModel) {
    AsyncCard("Job mode", "jobs", loader = { viewModel.api.fieldJobMode() }) { data, _ ->
        DetailLine("Today", Phase3Logic.str(data, "today"))
        AnyRows(data, "jobs", listOf("status"))
        Text(
            "Per-job timers, checklists, photos, and status moves ride the " +
                "ticket surfaces (Phase 3), as web's job mode composes them.",
            color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
        )
    }
    AsyncCard("Lead scoring", "leads", loader = { viewModel.api.fieldLeadScores() }) { data, _ ->
        AnyRows(data, "leads", listOf("score"))
        Phase3Logic.arr(data, "rules").forEach { rule ->
            Text(
                "${Phase3Logic.str(rule, "rule")}: ${Phase3Logic.str(rule, "detail")}",
                color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    AsyncCard("Source ROI", loader = { viewModel.api.fieldLeadRoi() }) { data, _ ->
        AnyLines(data, "rows", "sources")
        AnyRows(data, "rows")
        AnyRows(data, "sources")
    }
    // Asset import: staged preview -> confirm, as web.
    var fileName by remember { mutableStateOf("") }
    var csv by remember { mutableStateOf("") }
    var clientId by remember { mutableStateOf("") }
    var batchId by remember { mutableStateOf<Int?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    MirrorCard("Asset import") {
        TextFieldRow("File name (for the record)", fileName, { fileName = it })
        ClientIdField(clientId) { clientId = it }
        TextFieldRow("CSV content", csv, { csv = it }, singleLine = false, minLines = 3)
        Button(onClick = {
            scope.launch {
                try {
                    val r = viewModel.api.fieldImportPreview(buildJsonObject {
                        put("fileName", fileName)
                        put("content", csv)
                        clientId.toIntOrNull()?.let { put("clientId", it) }
                    })
                    batchId = Phase3Logic.obj(r, "batch")?.let { Phase3Logic.int(it, "id") }
                    notice = Phase3Logic.str(r, "message").ifBlank { "Preview staged." }
                    error = null
                } catch (e: Exception) {
                    error = plainError(e, "The preview could not be staged. Try again.")
                }
            }
        }) { Text("Preview import") }
        if (batchId != null) {
            Button(onClick = {
                scope.launch {
                    try {
                        val r = viewModel.api.fieldImportConfirm(buildJsonObject {
                            put("batchId", batchId!!)
                        })
                        notice = Phase3Logic.str(r, "message").ifBlank { "Import confirmed." }
                        batchId = null
                        error = null
                    } catch (e: Exception) {
                        error = plainError(e, "The import could not be confirmed. Try again.")
                    }
                }
            }) { Text("Confirm import") }
        }
        if (notice != null) Text(notice!!)
        ActionErrorText(error)
    }
    // Inventory counts + scan.
    AsyncCard("Inventory counts", "sessions", loader = { viewModel.api.fieldCounts() }) { data, reload ->
        var scanCode by remember { mutableStateOf("") }
        var actError by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ actError = it }, { reload() })
        Phase3Logic.arr(data, "sessions").forEach { session ->
            val sid = Phase3Logic.int(session, "id")
            Text(
                "Count #${sid ?: ""} · ${Phase3Logic.str(session, "status")}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Phase3Logic.arr(session, "lines").forEach { line ->
                Text(
                    "${Phase3Logic.str(line, "name")} · system " +
                        "${Phase3Logic.int(line, "systemQty") ?: 0} · counted " +
                        "${Phase3Logic.int(line, "countedQty") ?: 0}",
                    color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
                )
            }
            if (sid != null) {
                Row {
                    TextButton(onClick = { act { viewModel.api.fieldCountClose(sid) } }) {
                        Text("Close")
                    }
                    TextButton(onClick = { act { viewModel.api.fieldCountApply(sid) } }) {
                        Text("Apply")
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        TextFieldRow("Scan code", scanCode, { scanCode = it })
        Row {
            Button(onClick = {
                act {
                    viewModel.api.fieldCountStart(buildJsonObject { put("label", "Count") })
                }
            }) { Text("Start count") }
            OutlinedButton(onClick = {
                act { viewModel.api.fieldScan(buildJsonObject { put("code", scanCode) }) }
            }) { Text("Scan") }
        }
        ActionErrorText(actError)
    }
}

// ---- Parts & Inventory (sheet; rides Phase 3 parts in full) ----

@Composable
private fun PartsScreen(viewModel: AppViewModel) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var consumeTicket by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AsyncCard("Parts & Inventory", "parts", loader = { viewModel.api.parts() }) { data, reload ->
        val parts = Phase3Logic.arr(data, "parts")
        if (parts.isEmpty()) Text("No parts in inventory.", color = SentinelMuted)
        parts.forEach { part ->
            val pid = Phase3Logic.int(part, "id")
            Text(
                "${Phase3Logic.str(part, "name")} · ${Phase3Logic.str(part, "sku")} · " +
                    "on hand ${Phase3Logic.str(part, "qtyOnHand")} · " +
                    "low at ${Phase3Logic.str(part, "lowStockThreshold")}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row {
                TextButton(onClick = {
                    if (pid != null) scope.launch {
                        try {
                            viewModel.api.restockPart(pid, 1); reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The part could not be restocked. Try again.")
                        }
                    }
                }) { Text("Restock") }
                TextButton(onClick = {
                    val tid = consumeTicket.toIntOrNull()
                    if (pid != null && tid != null) scope.launch {
                        try {
                            viewModel.api.consumePart(pid, tid, 1); reload()
                        } catch (e: Exception) {
                            error = plainError(e, "The part could not be consumed. Try again.")
                        }
                    }
                }) { Text("Consume onto ticket…") }
            }
        }
        TextFieldRow("Ticket ID (for Consume)", consumeTicket, { consumeTicket = it })
        Spacer(Modifier.height(8.dp))
        Text("Add part", style = MaterialTheme.typography.titleSmall)
        TextFieldRow("Name", name, { name = it })
        TextFieldRow("SKU", sku, { sku = it })
        TextFieldRow("Qty", qty, { qty = it })
        TextFieldRow("Unit $", cost, { cost = it })
        Button(
            enabled = name.isNotBlank(),
            onClick = {
                scope.launch {
                    try {
                        viewModel.api.savePart(
                            buildJsonObject {
                                put("name", name); put("sku", sku)
                                put("qtyOnHand", qty.toIntOrNull() ?: 0)
                                cost.toDoubleOrNull()?.let { put("unitCostCents", (it * 100).toInt()) }
                                put("lowStockThreshold", 1)
                            },
                            null,
                        )
                        name = ""; sku = ""; qty = ""; cost = ""; reload()
                    } catch (e: Exception) {
                        error = plainError(e, "The part could not be saved. Try again.")
                    }
                }
            },
        ) { Text("Add part") }
        ActionErrorText(error)
    }
}

// ---- Collections (sheet) ----

@Composable
private fun CollectionsScreen(viewModel: AppViewModel) {
    AsyncCard("Collections", loader = { viewModel.api.collectionsBoard() }) { data, reload ->
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        DetailLine("Total", Phase3Logic.long(data, "totalCents")?.let { MirrorVocab.formatCents(it) })
        Phase3Logic.arr(data, "buckets").forEach { bucket ->
            Text(
                "${Phase3Logic.str(bucket, "bucket")} · ${Phase3Logic.int(bucket, "count") ?: 0} · " +
                    (Phase3Logic.long(bucket, "totalCents")?.let { MirrorVocab.formatCents(it) } ?: ""),
                style = MaterialTheme.typography.titleSmall,
            )
            Phase3Logic.arr(bucket, "invoices").forEach { inv ->
                val iid = Phase3Logic.int(inv, "invoiceId")
                Column {
                    Text(
                        "${Phase3Logic.str(inv, "invoiceNumber")} · ${Phase3Logic.str(inv, "clientName")}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        "Status ${Phase3Logic.str(inv, "status")} · workflow " +
                            "${Phase3Logic.str(inv, "workflowState")} · balance " +
                            (Phase3Logic.long(inv, "balanceDueCents")?.let { MirrorVocab.formatCents(it) } ?: ""),
                        color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
                    )
                    if (iid != null) {
                        Row {
                            listOf("none", "reminded", "promised", "escalated").forEach { state ->
                                TextButton(onClick = {
                                    act {
                                        viewModel.api.collectionsWorkflow(
                                            iid,
                                            buildJsonObject { put("workflowState", state) },
                                        )
                                    }
                                }) { Text(state) }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        ActionErrorText(error)
    }
}

// ---- Staff & Gear (sheet) ----

@Composable
private fun StaffGearScreen(viewModel: AppViewModel) {
    AsyncCard("Staff directory", "staff", loader = { viewModel.api.staffGearDirectory() }) { data, _ ->
        Phase3Logic.arr(data, "staff").forEach { person ->
            Text(Phase3Logic.str(person, "displayName"), style = MaterialTheme.typography.bodyLarge)
            Text(
                Phase3Logic.str(person, "role"),
                color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
            )
            Phase3Logic.arr(person, "certs").forEach { cert ->
                Text("Cert: ${Phase3Logic.str(cert, "name")}")
            }
            Phase3Logic.arr(person, "equipment").forEach { item ->
                Text("Gear: ${Phase3Logic.str(item, "name")}")
            }
            Spacer(Modifier.height(6.dp))
        }
    }
    AsyncCard("Training", "trainings", loader = { viewModel.api.staffGearTraining() }) { data, reload ->
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        AnyRows(data, "trainings", listOf("status"))
        var userId by remember { mutableStateOf("") }
        var title by remember { mutableStateOf("") }
        TextFieldRow("Staff user ID", userId, { userId = it })
        TextFieldRow("Training", title, { title = it })
        Button(onClick = {
            act {
                viewModel.api.staffGearTrainingSave(buildJsonObject {
                    userId.toIntOrNull()?.let { put("userId", it) }
                    put("title", title)
                })
            }
        }) { Text("Start training") }
        ActionErrorText(error)
    }
    AsyncCard("Certifications", loader = { viewModel.api.staffGearDirectory() }) { data, reload ->
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        var userId by remember { mutableStateOf("") }
        var certName by remember { mutableStateOf("") }
        var issuer by remember { mutableStateOf("") }
        TextFieldRow("Staff user ID", userId, { userId = it })
        TextFieldRow("Certification", certName, { certName = it })
        TextFieldRow("Issuer", issuer, { issuer = it })
        Button(onClick = {
            act {
                viewModel.api.staffGearCertSave(buildJsonObject {
                    userId.toIntOrNull()?.let { put("userId", it) }
                    put("name", certName)
                    put("issuer", issuer)
                })
            }
        }) { Text("Add certification") }
        Phase3Logic.arr(data, "staff").forEach { person ->
            Phase3Logic.arr(person, "certs").forEach { cert ->
                val cid = Phase3Logic.int(cert, "id")
                Row {
                    Text(
                        "${Phase3Logic.str(person, "displayName")} · ${Phase3Logic.str(cert, "name")}",
                        modifier = Modifier.weight(1f),
                    )
                    if (cid != null) {
                        TextButton(onClick = { act { viewModel.api.staffGearCertDelete(cid) } }) {
                            Text("Remove")
                        }
                    }
                }
            }
        }
        ActionErrorText(error)
    }
    AsyncCard("Equipment", "equipment", loader = { viewModel.api.staffGearEquipment() }) { data, reload ->
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        Phase3Logic.arr(data, "equipment").forEach { item ->
            val eid = Phase3Logic.int(item, "id")
            Row {
                Text(
                    "${Phase3Logic.str(item, "name")} · ${Phase3Logic.str(item, "kind")}",
                    modifier = Modifier.weight(1f),
                )
                if (eid != null) {
                    TextButton(onClick = { act { viewModel.api.staffGearEquipmentReturn(eid) } }) {
                        Text("Mark returned")
                    }
                }
            }
        }
        if (Phase3Logic.arr(data, "equipment").isEmpty()) Text("None", color = SentinelMuted)
        var name by remember { mutableStateOf("") }
        var kind by remember { mutableStateOf("tool") }
        var holder by remember { mutableStateOf("") }
        TextFieldRow("Equipment name", name, { name = it })
        TextFieldRow("Kind (tool / vehicle / other)", kind, { kind = it })
        TextFieldRow("Held by (user ID)", holder, { holder = it })
        Button(onClick = {
            act {
                viewModel.api.staffGearEquipmentSave(buildJsonObject {
                    put("name", name)
                    put("kind", kind)
                    holder.toIntOrNull()?.let { put("userId", it) }
                })
            }
        }) { Text("Add equipment") }
        ActionErrorText(error)
    }
}

// ---- Automation (sheet) ----

@Composable
private fun AutomationScreen(viewModel: AppViewModel) {
    AsyncCard("Assignment rules", "rules", loader = { viewModel.api.automationRules() }) { data, reload ->
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        Phase3Logic.arr(data, "rules").forEach { rule ->
            val rid = Phase3Logic.int(rule, "id")
            Row {
                Text(
                    "${Phase3Logic.str(rule, "name")} → ${Phase3Logic.str(rule, "assignToName")}",
                    modifier = Modifier.weight(1f),
                )
                if (rid != null) {
                    TextButton(onClick = { act { viewModel.api.automationRuleDelete(rid) } }) {
                        Text("Delete")
                    }
                }
            }
        }
        if (Phase3Logic.arr(data, "rules").isEmpty()) AnyRows(data, "rules")
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("") }
        var source by remember { mutableStateOf("") }
        var priority by remember { mutableStateOf("") }
        var assignTo by remember { mutableStateOf("") }
        TextFieldRow("Rule name", name, { name = it })
        TextFieldRow("Category (blank = any)", category, { category = it })
        TextFieldRow("Source (blank = any)", source, { source = it })
        TextFieldRow("Priority (blank = any)", priority, { priority = it })
        TextFieldRow("Assign to (user ID)", assignTo, { assignTo = it })
        Row {
            Button(onClick = {
                act {
                    viewModel.api.automationRuleSave(buildJsonObject {
                        put("name", name)
                        put("matchCategory", category)
                        put("matchSource", source)
                        put("matchPriority", priority)
                        assignTo.toIntOrNull()?.let { put("assignTo", it) }
                    })
                }
            }) { Text("Add rule") }
            OutlinedButton(onClick = { act { viewModel.api.automationRulesApply() } }) {
                Text("Apply rules to unassigned tickets")
            }
        }
        ActionErrorText(error)
    }
    AsyncCard("SLA escalation", loader = { viewModel.api.automationEscalationConfig() }) { data, reload ->
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        Phase3Logic.arr(data, "config").forEach { row ->
            Text(
                "${Phase3Logic.str(row, "priority")} · ${Phase3Logic.int(row, "thresholdPct") ?: ""}% · " +
                    if (Phase3Logic.bool(row, "enabled") == true) "on" else "off",
            )
        }
        var threshold by remember { mutableStateOf("") }
        TextFieldRow("Threshold %", threshold, { threshold = it })
        Row {
            Button(onClick = {
                act {
                    viewModel.api.automationEscalationConfigSave(buildJsonObject {
                        threshold.toIntOrNull()?.let { put("thresholdPct", it) }
                    })
                }
            }) { Text("Save threshold") }
            OutlinedButton(onClick = { act { viewModel.api.automationEscalationScan() } }) {
                Text("Run escalation scan")
            }
        }
        ActionErrorText(error)
    }
    AsyncCard("Escalations", "escalations", loader = { viewModel.api.automationEscalations() }) { data, _ ->
        AnyRows(data, "escalations", listOf("detail"))
    }
    AsyncCard("Quote follow-ups", loader = { viewModel.api.automationQuoteFollowups() }) { data, reload ->
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        AnyLines(data, "followups", "items")
        AnyRows(data, "followups", listOf("detail"))
        AnyRows(data, "items", listOf("detail"))
        OutlinedButton(onClick = { act { viewModel.api.automationQuoteFollowupScan() } }) {
            Text("Run follow-up scan")
        }
        ActionErrorText(error)
    }
    AsyncCard("Contract reminders", loader = { viewModel.api.automationContractReminders() }) { data, reload ->
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        AnyLines(data, "reminders", "items")
        AnyRows(data, "reminders", listOf("detail"))
        AnyRows(data, "items", listOf("detail"))
        OutlinedButton(onClick = { act { viewModel.api.automationContractReminderScan() } }) {
            Text("Run reminder scan")
        }
        var contractId by remember { mutableStateOf("") }
        var term by remember { mutableStateOf("") }
        TextFieldRow("Contract ID", contractId, { contractId = it })
        TextFieldRow("Term (months)", term, { term = it })
        Button(onClick = {
            act {
                viewModel.api.automationContractTerm(buildJsonObject {
                    contractId.toIntOrNull()?.let { put("contractId", it) }
                    term.toIntOrNull()?.let { put("termMonths", it) }
                })
            }
        }) { Text("Set a contract term…") }
        ActionErrorText(error)
    }
}

// ---- Client Ops (sheet, OWNER) ----

@Composable
private fun ClientOpsScreen(viewModel: AppViewModel) {
    var clientIdText by remember { mutableStateOf("") }
    val clientId = clientIdText.toIntOrNull()
    ClientIdField(clientIdText) { clientIdText = it }
    if (clientId == null) {
        Text(
            "Enter a client ID to load its maintenance windows, retainer bank, and emergency tree.",
            color = SentinelMuted, modifier = Modifier.padding(horizontal = 16.dp),
        )
        return
    }
    androidx.compose.runtime.key(clientId) {
    AsyncCard("Maintenance windows", "windows", loader = { viewModel.api.clientOpsWindows(clientId) }) { data, reload ->
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        AnyRows(data, "windows", listOf("detail"))
        var label by remember { mutableStateOf("") }
        TextFieldRow("Window label", label, { label = it })
        Button(onClick = {
            act {
                viewModel.api.clientOpsWindowSave(buildJsonObject {
                    put("clientId", clientId)
                    put("label", label)
                })
            }
        }) { Text("Save window") }
        Phase3Logic.arr(data, "windows").forEach { w ->
            val wid = Phase3Logic.int(w, "id")
            if (wid != null) {
                TextButton(onClick = { act { viewModel.api.clientOpsWindowDelete(wid) } }) {
                    Text("Delete window #${wid}")
                }
            }
        }
        ActionErrorText(error)
    }
    AsyncCard("Retainer hours bank", "retainers", loader = { viewModel.api.clientOpsRetainers(clientId) }) { data, reload ->
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        Phase3Logic.arr(data, "retainers").forEach { r ->
            val rid = Phase3Logic.int(r, "id")
            Text(
                "Balance ${Phase3Logic.dbl(r, "balanceHours") ?: 0}h · purchased " +
                    "${Phase3Logic.dbl(r, "purchasedHours") ?: 0}h · drawn " +
                    "${Phase3Logic.dbl(r, "drawnHours") ?: 0}h",
            )
            if (rid != null) {
                Row {
                    TextButton(onClick = {
                        act {
                            viewModel.api.clientOpsRetainerEntry(
                                rid, buildJsonObject { put("hours", 1.0) },
                            )
                        }
                    }) { Text("Add entry (+1h)") }
                    TextButton(onClick = {
                        act {
                            viewModel.api.clientOpsRetainerDraw(
                                rid, buildJsonObject { put("hours", 1.0) },
                            )
                        }
                    }) { Text("Draw 1h") }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        if (Phase3Logic.arr(data, "retainers").isEmpty()) Text("None", color = SentinelMuted)
        var hours by remember { mutableStateOf("") }
        TextFieldRow("Purchased hours (new retainer)", hours, { hours = it })
        Button(onClick = {
            act {
                viewModel.api.clientOpsRetainerSave(buildJsonObject {
                    put("clientId", clientId)
                    hours.toDoubleOrNull()?.let { put("purchasedHours", it) }
                })
            }
        }) { Text("Save retainer") }
        ActionErrorText(error)
    }
    AsyncCard("Emergency contact tree", "contacts", loader = { viewModel.api.clientOpsEmergency(clientId) }) { data, reload ->
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        val contacts = Phase3Logic.arr(data, "contacts").ifEmpty { Phase3Logic.arr(data, "emergencyContacts") }
        contacts.forEach { c ->
            val cid = Phase3Logic.int(c, "id")
            Row {
                Text(Phase8bLogic.rowTitle(c), modifier = Modifier.weight(1f))
                if (cid != null) {
                    TextButton(onClick = { act { viewModel.api.clientOpsEmergencyDelete(cid) } }) {
                        Text("Delete")
                    }
                }
            }
        }
        if (contacts.isEmpty()) Text("None", color = SentinelMuted)
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        TextFieldRow("Contact name", name, { name = it })
        TextFieldRow("Phone", phone, { phone = it })
        Button(onClick = {
            act {
                viewModel.api.clientOpsEmergencySave(buildJsonObject {
                    put("clientId", clientId)
                    put("name", name)
                    put("phone", phone)
                })
            }
        }) { Text("Save emergency contact") }
        ActionErrorText(error)
    }
    }
}

// ---- Pipeline & Packets (sheet) ----

@Composable
private fun PipelinePacketsScreen(viewModel: AppViewModel) {
    AsyncCard("Lead pipeline", "leads", loader = { viewModel.api.pipelineLeads() }) { data, reload ->
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        var confirmConvert by remember { mutableStateOf<Int?>(null) }
        Phase3Logic.arr(data, "leads").forEach { lead ->
            val lid = Phase3Logic.int(lead, "id")
            Column {
                Text(Phase3Logic.str(lead, "business"), style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${Phase3Logic.str(lead, "state")} · ${Phase3Logic.str(lead, "email")}",
                    color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
                )
                if (lid != null) {
                    Row {
                        listOf("new", "contacted", "qualified", "won", "lost").forEach { state ->
                            TextButton(onClick = {
                                act {
                                    viewModel.api.pipelineLeadState(
                                        lid, buildJsonObject { put("state", state) },
                                    )
                                }
                            }) { Text(state) }
                        }
                        TextButton(onClick = { confirmConvert = lid }) { Text("Convert lead") }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        if (Phase3Logic.arr(data, "leads").isEmpty()) Text("None", color = SentinelMuted)
        var business by remember { mutableStateOf("") }
        var contact by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        TextFieldRow("Business", business, { business = it })
        TextFieldRow("Contact name", contact, { contact = it })
        TextFieldRow("Email", email, { email = it })
        Button(onClick = {
            act {
                viewModel.api.pipelineLeadSave(buildJsonObject {
                    put("business", business)
                    put("contactName", contact)
                    put("email", email)
                })
            }
        }) { Text("Add lead") }
        ActionErrorText(error)
        if (confirmConvert != null) {
            val target = confirmConvert!!
            ConfirmGate(
                "Convert lead",
                "Converting sends this lead to the onboarding review queue — " +
                    "it does not create a live client directly.",
                "Convert lead",
                onDismiss = { confirmConvert = null },
                onConfirm = {
                    confirmConvert = null
                    act { viewModel.api.pipelineLeadConvert(target) }
                },
            )
        }
    }
    AsyncCard("Proposal builder", "proposals", loader = { viewModel.api.pipelineProposals() }) { data, reload ->
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        val context = LocalContext.current
        Phase3Logic.arr(data, "proposals").forEach { proposal ->
            val pid = Phase3Logic.int(proposal, "id")
            Column {
                Text(Phase8bLogic.rowTitle(proposal), style = MaterialTheme.typography.bodyLarge)
                Text(
                    Phase3Logic.str(proposal, "status"),
                    color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
                )
                if (pid != null) {
                    Row {
                        TextButton(onClick = {
                            act {
                                val doc = viewModel.api.pipelineProposalDocument(pid)
                                shareText(context, "Proposal", doc.toString())
                                doc
                            }
                        }) { Text("View proposal document") }
                        listOf("sent", "accepted", "declined").forEach { status ->
                            TextButton(onClick = {
                                act {
                                    viewModel.api.pipelineProposalStatus(
                                        pid, buildJsonObject { put("status", status) },
                                    )
                                }
                            }) { Text(status) }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        if (Phase3Logic.arr(data, "proposals").isEmpty()) Text("None", color = SentinelMuted)
        ActionErrorText(error)
    }
    AsyncCard("Change requests", "changeRequests", loader = { viewModel.api.pipelineChangeRequests() }) { data, reload ->
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        Phase3Logic.arr(data, "changeRequests").forEach { cr ->
            val cid = Phase3Logic.int(cr, "id")
            Column {
                Text(Phase8bLogic.rowTitle(cr), style = MaterialTheme.typography.bodyLarge)
                Text(
                    Phase3Logic.str(cr, "status"),
                    color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
                )
                if (cid != null) {
                    Row {
                        listOf("approved", "rejected", "completed").forEach { status ->
                            TextButton(onClick = {
                                act {
                                    viewModel.api.pipelineChangeStatus(
                                        cid, buildJsonObject { put("status", status) },
                                    )
                                }
                            }) { Text(status) }
                        }
                        TextButton(onClick = {
                            act {
                                viewModel.api.pipelineChangeVerify(
                                    cid, buildJsonObject { put("confirm", true) },
                                )
                            }
                        }) { Text("Verify") }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        if (Phase3Logic.arr(data, "changeRequests").isEmpty()) Text("None", color = SentinelMuted)
        ActionErrorText(error)
    }
}

// ---- Reports & Feedback (web cf2reports) ----

@Composable
private fun ReportsFeedbackScreen(viewModel: AppViewModel) {
    AsyncCard("Monthly client reports", "reports", loader = { viewModel.api.reportsList() }) { data, reload ->
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        val context = LocalContext.current
        Phase3Logic.arr(data, "reports").forEach { report ->
            val rid = Phase3Logic.int(report, "id")
            Column {
                Text(
                    "${Phase3Logic.str(report, "clientName")} · ${Phase3Logic.str(report, "period")}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (rid != null) {
                    Row {
                        TextButton(onClick = {
                            act {
                                val doc = viewModel.api.reportDocument(rid)
                                shareText(context, "Client report", doc.toString())
                                doc
                            }
                        }) { Text("PDF") }
                        TextButton(onClick = { act { viewModel.api.sendClientReport(rid) } }) {
                            Text("Send")
                        }
                        TextButton(onClick = { act { viewModel.api.discardClientReport(rid) } }) {
                            Text("Discard")
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        if (Phase3Logic.arr(data, "reports").isEmpty()) Text("None", color = SentinelMuted)
        var clientId by remember { mutableStateOf("") }
        var period by remember { mutableStateOf("") }
        ClientIdField(clientId) { clientId = it }
        TextFieldRow("Period (YYYY-MM)", period, { period = it })
        Button(onClick = {
            act {
                viewModel.api.reportsGenerate(buildJsonObject {
                    clientId.toIntOrNull()?.let { put("clientId", it) }
                    put("period", period)
                })
            }
        }) { Text("Generate report draft") }
        ActionErrorText(error)
    }
    AsyncCard("Satisfaction surveys", "surveys", loader = { viewModel.api.reportSurveys() }) { data, reload ->
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        AnyRows(data, "surveys", listOf("status"))
        var clientId by remember { mutableStateOf("") }
        var shownOnce by remember { mutableStateOf<String?>(null) }
        ClientIdField(clientId) { clientId = it }
        Button(onClick = {
            scopeLaunch(act) {
                val r = viewModel.api.reportSurveyCreate(buildJsonObject {
                    clientId.toIntOrNull()?.let { put("clientId", it) }
                })
                shownOnce = Phase8bLogic.shownOnceValue(r, "surveyToken", "token")
                r
            }
        }) { Text("Create survey") }
        if (shownOnce != null) {
            Text("Survey token — shown once:", style = MaterialTheme.typography.titleSmall)
            Text(shownOnce!!)
            OutlinedButton(onClick = { shownOnce = null }) { Text("Done") }
        }
        ActionErrorText(error)
    }
    AsyncCard("Referral tracker", "referrals", loader = { viewModel.api.reportReferrals() }) { data, reload ->
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        AnyRows(data, "referrals", listOf("status"))
        var name by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        TextFieldRow("Referral name", name, { name = it })
        TextFieldRow("Notes", notes, { notes = it })
        Button(onClick = {
            act {
                viewModel.api.reportReferralSave(buildJsonObject {
                    put("name", name)
                    put("notes", notes)
                })
            }
        }) { Text("Record referral") }
        ActionErrorText(error)
    }
    AsyncCard("Per-client threat briefs", "briefs", loader = { viewModel.api.reportBriefs() }) { data, reload ->
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        val briefs = Phase3Logic.arr(data, "briefs").ifEmpty { Phase3Logic.arr(data, "threatBriefs") }
        briefs.forEach { brief ->
            val bid = Phase3Logic.int(brief, "id")
            Column {
                Text(Phase8bLogic.rowTitle(brief), style = MaterialTheme.typography.bodyLarge)
                Text(
                    Phase3Logic.str(brief, "state").ifBlank { Phase3Logic.str(brief, "status") },
                    color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
                )
                if (bid != null) {
                    Row {
                        TextButton(onClick = { act { viewModel.api.reportBriefMarkSent(bid) } }) {
                            Text("Mark sent")
                        }
                        TextButton(onClick = { act { viewModel.api.reportBriefDelete(bid) } }) {
                            Text("Discard brief")
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        if (briefs.isEmpty()) Text("None", color = SentinelMuted)
        var clientId by remember { mutableStateOf("") }
        ClientIdField(clientId) { clientId = it }
        Button(onClick = {
            act {
                viewModel.api.reportBriefGenerate(buildJsonObject {
                    clientId.toIntOrNull()?.let { put("clientId", it) }
                })
            }
        }) { Text("Generate brief draft") }
        ActionErrorText(error)
    }
    AsyncCard("Report schedules", "schedules", loader = { viewModel.api.reportSchedules() }) { data, reload ->
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        Phase3Logic.arr(data, "schedules").forEach { schedule ->
            val sid = Phase3Logic.int(schedule, "id")
            Row {
                Text(
                    "${Phase3Logic.str(schedule, "kind")} · ${Phase3Logic.str(schedule, "cadence")}",
                    modifier = Modifier.weight(1f),
                )
                if (sid != null) {
                    TextButton(onClick = { act { viewModel.api.reportScheduleRun(sid) } }) {
                        Text("Run")
                    }
                }
            }
        }
        if (Phase3Logic.arr(data, "schedules").isEmpty()) Text("None", color = SentinelMuted)
        Row {
            Button(onClick = {
                act {
                    viewModel.api.reportScheduleSave(buildJsonObject {
                        put("kind", "owner_digest")
                        put("cadence", "weekly")
                        put("active", true)
                    })
                }
            }) { Text("Save schedule") }
            OutlinedButton(onClick = { act { viewModel.api.reportSchedulesRunDue() } }) {
                Text("Run due")
            }
        }
        ActionErrorText(error)
    }
    AsyncCard("Snapshots", "snapshots", loader = { viewModel.api.reportSnapshots() }) { data, _ ->
        Phase3Logic.arr(data, "snapshots").forEach { snap ->
            Text(Phase3Logic.str(snap, "kind"), style = MaterialTheme.typography.bodyLarge)
            Text(
                Phase3Logic.str(snap, "summary"),
                color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(6.dp))
        }
        if (Phase3Logic.arr(data, "snapshots").isEmpty()) Text("None", color = SentinelMuted)
    }
}

/** rememberAction's block returns Unit; this adapter lets a
 *  caller capture a value out of the action (shown-once flows). */
private fun scopeLaunch(
    act: (suspend () -> Unit) -> Unit,
    block: suspend () -> kotlinx.serialization.json.JsonObject,
) {
    act { block() }
}

// ---- App & Integrations (web b4cadmin, OWNER) ----

@Composable
private fun AppIntegrationsScreen(viewModel: AppViewModel) {
    var shownOnce by remember { mutableStateOf<String?>(null) }
    var shownLabel by remember { mutableStateOf("") }
    AsyncCard("App & Integrations", loader = { viewModel.api.appIntegrations() }) { data, reload ->
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        Text(
            "Keys and secrets are shown once at creation.",
            color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
        )
        Text("App devices", style = MaterialTheme.typography.titleSmall)
        Phase3Logic.arr(data, "devices").forEach { device ->
            val did = Phase3Logic.int(device, "id")
            Row {
                Text(
                    "${Phase3Logic.str(device, "deviceName")} · ${Phase3Logic.str(device, "userName")} · " +
                        Phase3Logic.str(device, "status"),
                    modifier = Modifier.weight(1f),
                )
                if (did != null) {
                    TextButton(onClick = { act { viewModel.api.appDeviceRevoke(did) } }) {
                        Text("Revoke")
                    }
                }
            }
        }
        Text("Partner lead API keys", style = MaterialTheme.typography.titleSmall)
        val keys = Phase3Logic.arr(data, "apiKeys").ifEmpty { Phase3Logic.arr(data, "keys") }
        keys.forEach { key ->
            val kid = Phase3Logic.int(key, "id")
            Row {
                Text(
                    "${Phase3Logic.str(key, "name")} · ${Phase3Logic.str(key, "keyPrefix")} · " +
                        Phase3Logic.str(key, "status"),
                    modifier = Modifier.weight(1f),
                )
                if (kid != null) {
                    TextButton(onClick = { act { viewModel.api.appApiKeyRevoke(kid) } }) {
                        Text("Revoke")
                    }
                }
            }
        }
        if (keys.isEmpty()) Text("None", color = SentinelMuted)
        Text("Outbound webhooks", style = MaterialTheme.typography.titleSmall)
        Phase3Logic.arr(data, "webhooks").forEach { hook ->
            val hid = Phase3Logic.int(hook, "id")
            Row {
                Text(
                    "${Phase3Logic.str(hook, "name")} · ${Phase3Logic.str(hook, "url")}",
                    modifier = Modifier.weight(1f),
                )
                if (hid != null) {
                    val activeNow = Phase3Logic.bool(hook, "active") ?: false
                    TextButton(onClick = {
                        act { viewModel.api.appWebhookActive(hid, !activeNow) }
                    }) { Text(if (activeNow) "Deactivate" else "Activate") }
                }
            }
        }
        if (Phase3Logic.arr(data, "webhooks").isEmpty()) Text("None", color = SentinelMuted)
        AnyRows(data, "deliveries", listOf("status"))
        OutlinedButton(onClick = { act { viewModel.api.appDeliveriesRun() } }) {
            Text("Run delivery pass now")
        }
        ActionErrorText(error)
    }
    // Create forms + shown-once display (in-session only).
    var keyName by remember { mutableStateOf("") }
    var keyTag by remember { mutableStateOf("") }
    var hookName by remember { mutableStateOf("") }
    var hookUrl by remember { mutableStateOf("") }
    var deviceId by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    MirrorCard("Create") {
        TextFieldRow("API key name", keyName, { keyName = it })
        TextFieldRow("Source tag", keyTag, { keyTag = it })
        Button(onClick = {
            scope.launch {
                try {
                    val r = viewModel.api.appApiKeyCreate(buildJsonObject {
                        put("name", keyName)
                        put("sourceTag", keyTag)
                    })
                    shownLabel = "API key — copy it now; it is shown only once."
                    shownOnce = Phase8bLogic.shownOnceValue(r, "key", "secret", "apiKeyValue")
                    formError = null
                } catch (e: Exception) {
                    formError = plainError(e, "The key could not be created. Try again.")
                }
            }
        }) { Text("Create key") }
        Spacer(Modifier.height(8.dp))
        TextFieldRow("Webhook name", hookName, { hookName = it })
        TextFieldRow("Webhook URL", hookUrl, { hookUrl = it })
        Button(onClick = {
            scope.launch {
                try {
                    val r = viewModel.api.appWebhookCreate(buildJsonObject {
                        put("name", hookName)
                        put("url", hookUrl)
                    })
                    shownLabel = "Webhook signing secret — shown only once."
                    shownOnce = Phase8bLogic.shownOnceValue(r, "secret", "key")
                    formError = null
                } catch (e: Exception) {
                    formError = plainError(e, "The webhook could not be created. Try again.")
                }
            }
        }) { Text("Create webhook") }
        Spacer(Modifier.height(8.dp))
        TextFieldRow("Device ID (relabel)", deviceId, { deviceId = it })
        TextFieldRow("New device name", deviceName, { deviceName = it })
        Button(onClick = {
            val id = deviceId.toIntOrNull()
            if (id != null) scope.launch {
                try {
                    viewModel.api.appDeviceRename(id, deviceName)
                    formError = null
                } catch (e: Exception) {
                    formError = plainError(e, "The device could not be relabeled. Try again.")
                }
            }
        }) { Text("Relabel") }
        ActionErrorText(formError)
    }
    if (shownOnce != null) {
        MirrorCard(shownLabel) {
            Text(shownOnce!!)
            OutlinedButton(onClick = { shownOnce = null }) { Text("Done") }
        }
    }
}

// ---- Client Import (web b4dimport) ----

@Composable
private fun ClientImportScreen(viewModel: AppViewModel) {
    AsyncCard("Import batches", "batches", loader = { viewModel.api.clientImports() }) { data, _ ->
        val batches = Phase3Logic.arr(data, "batches").ifEmpty { Phase3Logic.arr(data, "imports") }
        batches.forEach { batch ->
            Text(
                "${Phase3Logic.str(batch, "fileName")} · ${Phase3Logic.str(batch, "state")} · " +
                    "${Phase3Logic.int(batch, "totalRows") ?: 0} rows",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(4.dp))
        }
        if (batches.isEmpty()) Text("None", color = SentinelMuted)
        val contract = Phase3Logic.arrRaw(data, "columnContract")
        contract.forEach { col ->
            Text(
                (col as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "",
                color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    // Preview -> review -> confirm. NOTHING becomes a live client:
    // preview stages a dry run; confirm lands in the review queue.
    var fileName by remember { mutableStateOf("") }
    var csv by remember { mutableStateOf("") }
    var batch by remember { mutableStateOf<kotlinx.serialization.json.JsonObject?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmStage by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    MirrorCard("Client CSV Import") {
        Text(
            "Dry run first — review the per-row verdicts, then confirm. " +
                "Confirmed imports land in the onboarding review queue, not live clients.",
            color = SentinelMuted,
        )
        TextFieldRow("File name (for the record)", fileName, { fileName = it })
        TextFieldRow("CSV content", csv, { csv = it }, singleLine = false, minLines = 4)
        Button(onClick = {
            scope.launch {
                try {
                    val r = viewModel.api.clientImportPreview(buildJsonObject {
                        put("fileName", fileName)
                        put("content", csv)
                    })
                    batch = Phase3Logic.obj(r, "batch")
                    notice = Phase3Logic.str(r, "message").ifBlank { "Dry run staged." }
                    error = null
                } catch (e: Exception) {
                    error = plainError(e, "The dry run could not be staged. Try again.")
                }
            }
        }) { Text("Dry run — preview") }
        val current = batch
        if (current != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "${Phase3Logic.str(current, "fileName")} · ${Phase3Logic.str(current, "state")} · " +
                    "valid ${Phase3Logic.int(current, "validRows") ?: 0} · " +
                    "duplicates ${Phase3Logic.int(current, "duplicateRows") ?: 0} · " +
                    "errors ${Phase3Logic.int(current, "errorRows") ?: 0}",
                style = MaterialTheme.typography.titleSmall,
            )
            Phase3Logic.arr(current, "rows").forEach { row ->
                val rowData = Phase3Logic.obj(row, "data")
                Text(
                    "Row ${Phase3Logic.int(row, "rowNumber") ?: ""} · " +
                        "${Phase3Logic.str(row, "verdict")} · " +
                        (rowData?.let { Phase3Logic.str(it, "name") } ?: ""),
                )
                Phase3Logic.str(row, "detail").takeIf { it.isNotBlank() }?.let {
                    Text(it, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            val bid = Phase3Logic.int(current, "id")
            if (bid != null) {
                Button(onClick = { confirmStage = true }) { Text("Confirm import") }
            }
        }
        if (notice != null) Text(notice!!)
        ActionErrorText(error)
    }
    if (confirmStage) {
        val bid = batch?.let { Phase3Logic.int(it, "id") }
        ConfirmGate(
            "Confirm import",
            "Confirm this import? Rows land in the onboarding review queue " +
                "for a person to approve — no live clients are created by this step.",
            "Confirm import",
            onDismiss = { confirmStage = false },
            onConfirm = {
                confirmStage = false
                if (bid != null) scope.launch {
                    try {
                        val r = viewModel.api.clientImportConfirm(bid)
                        notice = Phase3Logic.str(r, "message").ifBlank { "Import confirmed." }
                        batch = Phase3Logic.obj(r, "batch") ?: batch
                        error = null
                    } catch (e: Exception) {
                        error = plainError(e, "The import could not be confirmed. Try again.")
                    }
                }
            },
        )
    }
}

// ---- Packets & Routes (web b4jops) ----

@Composable
private fun PacketsRoutesScreen(viewModel: AppViewModel) {
    AsyncCard("Urgent intake", loader = { viewModel.api.packetsOps() }) { data, reload ->
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        val urgent = Phase3Logic.obj(data, "urgent")
        Text("Urgent total: ${urgent?.let { Phase3Logic.int(it, "total") } ?: 0}")
        urgent?.let {
            Phase3Logic.arr(it, "items").forEach { item ->
                val uid = Phase3Logic.int(item, "id")
                Row {
                    Text(Phase8bLogic.rowTitle(item), modifier = Modifier.weight(1f))
                    if (uid != null) {
                        TextButton(onClick = { act { viewModel.api.packetsUrgentAck(uid) } }) {
                            Text("Acknowledge")
                        }
                    }
                }
            }
        }
        Phase3Logic.str(data, "latencyNote").takeIf { it.isNotBlank() }?.let {
            Text(it, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
        }
        AnyRows(data, "followups", listOf("detail"))
        AnyRows(data, "statements", listOf("detail"))
        ActionErrorText(error)
    }
    AsyncCard("Paper invoice packets", loader = { viewModel.api.packetsList() }) { data, reload ->
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        val packets = Phase3Logic.arr(data, "packets").ifEmpty { Phase3Logic.arr(data, "batches") }
        packets.forEach { packet ->
            val pid = Phase3Logic.int(packet, "id")
            Row {
                Text(Phase8bLogic.rowTitle(packet), modifier = Modifier.weight(1f))
                if (pid != null) {
                    TextButton(onClick = { act { viewModel.api.packetMarkMailed(pid) } }) {
                        Text("Mark mailed by hand")
                    }
                }
            }
        }
        if (packets.isEmpty()) Text("None", color = SentinelMuted)
        var clientId by remember { mutableStateOf("") }
        ClientIdField(clientId) { clientId = it }
        Row {
            Button(onClick = {
                act {
                    viewModel.api.packetBuild(buildJsonObject {
                        clientId.toIntOrNull()?.let { put("clientId", it) }
                    })
                }
            }) { Text("Build packet") }
            OutlinedButton(onClick = {
                act { viewModel.api.packetsGeneratePack(buildJsonObject { put("all", true) }) }
            }) { Text("Build packets for all eligible clients") }
        }
        ActionErrorText(error)
    }
    AsyncCard("Day route planner", loader = { viewModel.api.packetsRoute() }) { data, reload ->
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        DetailLine("Date", Phase3Logic.str(data, "date"))
        Phase3Logic.str(data, "method").takeIf { it.isNotBlank() }?.let {
            Text(it, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
        }
        Phase3Logic.str(data, "driveNote").takeIf { it.isNotBlank() }?.let {
            Text(it, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
        }
        AnyRows(data, "stops", listOf("detail"))
        Row {
            OutlinedButton(onClick = {
                act {
                    viewModel.api.packetsRouteOrder(buildJsonObject { put("order", kotlinx.serialization.json.JsonArray(emptyList())) })
                }
            }) { Text("Save routing") }
            OutlinedButton(onClick = {
                act {
                    viewModel.api.packetsConfig(buildJsonObject { put("note", "route order") })
                }
            }) { Text("Save this order as a note") }
        }
        ActionErrorText(error)
    }
    AsyncCard("Warranty claims", "claims", loader = { viewModel.api.packetsClaims() }) { data, reload ->
        var error by remember { mutableStateOf<String?>(null) }
        val act = rememberAction({ error = it }, { reload() })
        val statuses = Phase3Logic.arrRaw(data, "statuses")
            .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
        Phase3Logic.arr(data, "claims").forEach { claim ->
            val cid = Phase3Logic.int(claim, "id")
            Column {
                Text(Phase8bLogic.rowTitle(claim), style = MaterialTheme.typography.bodyLarge)
                Text(
                    Phase3Logic.str(claim, "status"),
                    color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
                )
                if (cid != null) {
                    Row {
                        statuses.forEach { status ->
                            TextButton(onClick = {
                                act {
                                    viewModel.api.packetsClaimTransition(
                                        cid, buildJsonObject { put("status", status) },
                                    )
                                }
                            }) { Text(status) }
                        }
                        TextButton(onClick = { act { viewModel.api.packetsClaimReopen(cid) } }) {
                            Text("Reopen")
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        if (Phase3Logic.arr(data, "claims").isEmpty()) Text("None", color = SentinelMuted)
        var assetId by remember { mutableStateOf("") }
        var detail by remember { mutableStateOf("") }
        var vaultId by remember { mutableStateOf("") }
        TextFieldRow("Asset ID", assetId, { assetId = it })
        TextFieldRow("Claim detail", detail, { detail = it })
        Button(onClick = {
            act {
                viewModel.api.packetsClaimFile(buildJsonObject {
                    assetId.toIntOrNull()?.let { put("assetId", it) }
                    put("detail", detail)
                })
            }
        }) { Text("File claim") }
        TextFieldRow("Vault file id (attach to first claim above)", vaultId, { vaultId = it })
        ActionErrorText(error)
    }
    AsyncCard("Retainer statement drafts", loader = { viewModel.api.packetsOps() }) { data, _ ->
        AnyRows(data, "statements", listOf("detail"))
    }
}

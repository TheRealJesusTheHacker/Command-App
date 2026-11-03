package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import net.sentinelsafeguard.app.data.api.CalendarSyncResponse
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * Today's injected panels (mirror spec §6 Phase 2): the ~16
 * bundle panels web appends after the core Today grid, rendered
 * in web's composition/order from GET /api/app/today/panels plus
 * the dedicated panel endpoints (ask, automations, digests,
 * estate health, calendar sync, reports summary, staff certs,
 * registers, urgent). The urgent-intake banner pins above the
 * grid, as web pins it. Panel payloads are the web handlers' own
 * section objects; rows render their real fields and navigate
 * to the record when a row carries one.
 */

// ---- open-JSON helpers (the panel sections' shared shapes) ----

private fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

private fun JsonObject.int(key: String): Int? =
    (this[key] as? JsonPrimitive)?.intOrNull

private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

private fun JsonObject.arr(key: String): List<JsonElement> =
    (this[key] as? JsonArray)?.toList() ?: emptyList()

private fun JsonObject.items(): List<JsonObject> =
    arr("items").mapNotNull { it as? JsonObject }

/** A section's total: its own total field, else its items, else
 *  the sum of its sub-sections' totals. */
private fun JsonObject.totalOf(): Int {
    int("total")?.let { return it }
    val items = items()
    if (items.isNotEmpty() || containsKey("items")) return items.size
    return entries.sumOf { (_, v) -> (v as? JsonObject)?.totalOf() ?: 0 }
}

private val TITLE_KEYS = listOf("title", "name", "clientName", "displayName", "legalName", "label")
private val SUB_KEYS = listOf(
    "status", "state", "severity", "stage", "category",
    "dueDate", "date", "workDate", "expires", "renewalDate",
)

private fun panelRowTitle(obj: JsonObject): String =
    TITLE_KEYS.firstNotNullOfOrNull { obj.str(it) } ?: "Item"

private fun panelRowSubtitle(obj: JsonObject): String {
    val parts = mutableListOf<String>()
    SUB_KEYS.forEach { key -> obj.str(key)?.let { parts += "$key $it" } }
    obj.int("count")?.let { parts += "$it" }
    obj.int("openItems")?.let { parts += "$it open item(s)" }
    obj.int("daysLeft")?.let { parts += "$it day(s) left" }
    return parts.joinToString(" · ")
}

private fun panelRoute(obj: JsonObject): String? = when {
    obj.int("ticketId") != null -> "ticket/${obj.int("ticketId")}"
    obj.int("caseId") != null -> "case/${obj.int("caseId")}"
    obj.int("engagementId") != null -> "engagement/${obj.int("engagementId")}"
    obj.int("clientId") != null -> "client/${obj.int("clientId")}"
    else -> null
}

@Composable
private fun PanelRows(objs: List<JsonObject>, nav: NavController, emptyText: String) {
    if (objs.isEmpty()) {
        Text(emptyText, color = SentinelMuted)
    }
    objs.forEach { obj ->
        val route = panelRoute(obj)
        ItemRow(
            panelRowTitle(obj),
            panelRowSubtitle(obj).ifBlank { null },
            onClick = if (route != null) ({ nav.navigate(route) }) else null,
        )
    }
}

@Composable
private fun CountLine(label: String, count: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        Text(count.toString())
    }
}

/** The urgent-intake banner: pinned above the Today grid when
 *  Critical Alerts are waiting (web b4j rule). */
@Composable
fun UrgentBanner(viewModel: AppViewModel, nav: NavController) {
    var urgent by remember { mutableStateOf<JsonObject?>(null) }
    LaunchedEffect(Unit) {
        urgent = try {
            viewModel.api.todayUrgent().obj("urgent")
        } catch (e: Exception) {
            null
        }
    }
    val u = urgent ?: return
    val total = u.totalOf()
    if (total <= 0) return
    MirrorCard("Urgent intake", total) {
        PanelRows(u.items(), nav, "No urgent alerts.")
        u.str("latencyNote")?.let {
            Text(it, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun TodayPanels(viewModel: AppViewModel, nav: NavController) {
    val scope = rememberCoroutineScope()
    var panels by remember { mutableStateOf<JsonObject?>(null) }
    var automations by remember { mutableStateOf<JsonObject?>(null) }
    var digests by remember { mutableStateOf<JsonObject?>(null) }
    var reports by remember { mutableStateOf<JsonObject?>(null) }
    var staffCerts by remember { mutableStateOf<JsonObject?>(null) }
    var estate by remember { mutableStateOf<JsonObject?>(null) }
    var calendar by remember { mutableStateOf<CalendarSyncResponse?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    var actionNote by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reloadKey) {
        panels = try {
            viewModel.api.todayPanels().obj("panels")
        } catch (e: Exception) {
            null
        }
        automations = try {
            viewModel.api.todayAutomations()
        } catch (e: Exception) {
            null
        }
        digests = try {
            viewModel.api.todayDigests()
        } catch (e: Exception) {
            null
        }
        reports = try {
            viewModel.api.todayReportsSummary()
        } catch (e: Exception) {
            null
        }
        staffCerts = try {
            viewModel.api.todayStaffCerts()
        } catch (e: Exception) {
            null
        }
        estate = try {
            viewModel.api.todayEstateHealth()
        } catch (e: Exception) {
            null
        }
        calendar = try {
            viewModel.api.todayCalendarSync()
        } catch (e: Exception) {
            null
        }
        loaded = true
    }

    if (!loaded) {
        SkeletonList(3)
        return
    }
    val p = panels

    Column {
        if (actionNote != null) {
            Text(
                actionNote!!,
                color = SentinelMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        // Ask Command (b3f)
        MirrorCard("Ask Command") {
            var question by remember { mutableStateOf("") }
            var answer by remember { mutableStateOf<String?>(null) }
            var asking by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("Ask a question") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                enabled = !asking && question.isNotBlank(),
                onClick = {
                    asking = true
                    scope.launch {
                        try {
                            val out = viewModel.api.todayAsk(question.trim())
                            answer = out.text.ifBlank {
                                "That question could not be answered. Try asking about counts, due work, or a client, case, or ticket."
                            }
                        } catch (e: Exception) {
                            answer = plainError(e, "Ask Command could not answer just now. Try again.")
                        } finally {
                            asking = false
                        }
                    }
                },
            ) { Text(if (asking) "Asking…" else "Ask") }
            if (answer != null) {
                Spacer(Modifier.height(8.dp))
                Text(answer!!)
            }
        }

        // Automations (b3e)
        val auto = automations?.obj("today") ?: p?.obj("b3eAutomation")
        MirrorCard("Automations") {
            if (auto == null) {
                Text("Automation status could not be loaded.", color = SentinelMuted)
            } else {
                CountLine("Stale tickets", auto.obj("staleTickets")?.totalOf() ?: 0)
                CountLine("Runaway timers", auto.obj("runawayTimers")?.totalOf() ?: 0)
                CountLine("Device alerts", auto.int("deviceAlerts") ?: 0)
                CountLine("Ticket drafts", auto.int("ticketDrafts") ?: 0)
                CountLine("Postmortems", auto.int("postmortems") ?: 0)
                CountLine("Review drafts", auto.int("reviewDrafts") ?: 0)
                CountLine("KB review", auto.int("kbReview") ?: 0)
                CountLine("Lead packets", auto.int("leadPackets") ?: 0)
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                scope.launch {
                    try {
                        viewModel.api.runAutomations()
                        actionNote = "Automation pass run."
                        reloadKey++
                    } catch (e: Exception) {
                        actionNote = plainError(e, "The automation pass could not be run. Try again.")
                    }
                }
            }) { Text("Run") }
        }

        // SLA — breaches & at risk (cfG/b08) + the scan action
        val sla = p?.obj("sla")
        MirrorCard("SLA — breaches & at risk", sla?.let {
            (it.obj("breaches")?.totalOf() ?: 0) + (it.obj("atRisk")?.totalOf() ?: 0)
        }) {
            if (sla == null) {
                Text("SLA status could not be loaded.", color = SentinelMuted)
            } else {
                Text("Breaches · ${sla.obj("breaches")?.totalOf() ?: 0}", style = MaterialTheme.typography.titleSmall)
                PanelRows(sla.obj("breaches")?.items() ?: emptyList(), nav, "No SLA breaches.")
                Spacer(Modifier.height(6.dp))
                Text("At risk · ${sla.obj("atRisk")?.totalOf() ?: 0}", style = MaterialTheme.typography.titleSmall)
                PanelRows(sla.obj("atRisk")?.items() ?: emptyList(), nav, "Nothing is at risk.")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                scope.launch {
                    try {
                        val out = viewModel.api.slaScan()
                        actionNote = "SLA scan complete — ${out.totalOf()} item(s) flagged."
                        reloadKey++
                    } catch (e: Exception) {
                        actionNote = plainError(e, "The SLA scan could not be run. Try again.")
                    }
                }
            }) { Text("Run SLA scan") }
        }

        // Weekly digest queue (b07)
        val digestRows = digests?.arr("digests")?.mapNotNull { it as? JsonObject } ?: emptyList()
        MirrorCard("Weekly digest queue", digestRows.size) {
            if (digestRows.isEmpty()) {
                Text("No digests are queued.", color = SentinelMuted)
            }
            digestRows.forEach { digest ->
                Column(Modifier.padding(vertical = 4.dp)) {
                    Text(
                        listOfNotNull(
                            digest.str("clientName") ?: digest.int("clientId")?.let { "Client #$it" },
                            digest.str("status"),
                            digest.str("weekStart") ?: digest.str("week"),
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    val did = digest.int("id")
                    if (did != null) {
                        Row {
                            TextButton(onClick = {
                                scope.launch {
                                    try {
                                        viewModel.api.sendDigest(did)
                                        actionNote = "Digest sent."
                                        reloadKey++
                                    } catch (e: Exception) {
                                        actionNote = plainError(e, "The digest could not be sent. Try again.")
                                    }
                                }
                            }) { Text("Send") }
                            TextButton(onClick = {
                                scope.launch {
                                    try {
                                        viewModel.api.discardDigest(did)
                                        actionNote = "Digest discarded."
                                        reloadKey++
                                    } catch (e: Exception) {
                                        actionNote = plainError(e, "The digest could not be discarded. Try again.")
                                    }
                                }
                            }) { Text("Discard") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = {
                scope.launch {
                    try {
                        viewModel.api.generateDigests()
                        actionNote = "Digests generated."
                        reloadKey++
                    } catch (e: Exception) {
                        actionNote = plainError(e, "Digests could not be generated. Try again.")
                    }
                }
            }) { Text("Generate digests") }
        }

        // Pipeline & packets summary (b18 — web shows it when total > 0)
        val pipeline = p?.obj("cf2Pipeline")
        if (pipeline != null && pipeline.totalOf() > 0) {
            MirrorCard("Pipeline & packets", pipeline.totalOf()) {
                PanelRows(pipeline.items(), nav, "Nothing in the pipeline.")
                p.obj("cf2LaneB")?.items()?.let { laneB ->
                    if (laneB.isNotEmpty()) PanelRows(laneB, nav, "")
                }
            }
        }

        // Reports & Feedback summary (b19)
        MirrorCard("Reports & Feedback") {
            val r = reports
            if (r == null) {
                Text("Reports summary could not be loaded.", color = SentinelMuted)
            } else {
                val reps = r.obj("reports")?.arr("reports")?.size ?: 0
                val survey = r.obj("surveys")?.obj("summary") ?: r.obj("reports")?.obj("surveySummary")
                val referrals = r.obj("referrals")?.arr("referrals")?.size ?: 0
                val briefs = r.obj("threatBriefs")?.arr("briefs")?.size ?: 0
                CountLine("Client reports", reps)
                CountLine("Surveys", survey?.int("count") ?: 0)
                survey?.str("avgRating")?.let { Text("Average rating $it") }
                survey?.str("recommendPct")?.let { Text("Recommend $it%") }
                CountLine("Referrals", referrals)
                CountLine("Threat briefs", briefs)
            }
        }

        // Maintenance & retainers (b16)
        val maint = p?.obj("cf2MaintUpcoming")
        val retainer = p?.obj("cf2RetainerLow")
        if ((maint?.totalOf() ?: 0) + (retainer?.totalOf() ?: 0) > 0) {
            MirrorCard(
                "Maintenance & retainers",
                (maint?.totalOf() ?: 0) + (retainer?.totalOf() ?: 0),
            ) {
                PanelRows(maint?.items() ?: emptyList(), nav, "No maintenance is coming up.")
                PanelRows(retainer?.items() ?: emptyList(), nav, "No retainers are running low.")
            }
        }

        // Firm operations — renewals & tasks (b10 + b09 figures)
        MirrorCard("Firm operations — renewals & tasks") {
            val firmOps = p?.obj("cf2FirmOps")
            val firm = p?.obj("cf2Firm")
            if (firmOps == null && firm == null) {
                Text("Firm operations could not be loaded.", color = SentinelMuted)
            } else {
                CountLine("Vendors", firmOps?.obj("vendors")?.totalOf() ?: 0)
                CountLine("Owner tasks", firmOps?.obj("ownerTasks")?.totalOf() ?: 0)
                CountLine("Disposals in progress", firm?.int("disposalsInProgress") ?: 0)
                CountLine("Active KB shares", firm?.int("activeKbShares") ?: 0)
                CountLine("Draft snapshots", firm?.int("draftSnapshots") ?: 0)
                CountLine("Due report schedules", firm?.int("dueReportSchedules") ?: 0)
                CountLine("Money desk items", p?.obj("b3Money")?.totalOf() ?: 0)
                CountLine("Contract follow-ups", p?.obj("cfFollowUps")?.totalOf() ?: 0)
                CountLine("Low stock", p?.obj("lowStock")?.totalOf() ?: 0)
                CountLine("Risk reviews due", p?.obj("cfRiskReviewDue")?.totalOf() ?: 0)
                CountLine("Breach matches", p?.obj("cfBreachMatches")?.totalOf() ?: 0)
            }
        }

        // Staff certifications & equipment service (b11)
        MirrorCard("Staff certifications & equipment service") {
            val dir = staffCerts?.obj("directory")
            val staff = dir?.arr("staff")?.mapNotNull { it as? JsonObject } ?: emptyList()
            if (staffCerts == null) {
                Text("Staff certifications could not be loaded.", color = SentinelMuted)
            } else if (staff.isEmpty()) {
                Text("No staff records.", color = SentinelMuted)
            }
            staff.forEach { person ->
                val certs = person.arr("certs").size
                val training = person.obj("training")
                val equipment = person.arr("equipment").size
                ItemRow(
                    person.str("displayName") ?: "Staff",
                    listOfNotNull(
                        person.str("role"),
                        "$certs certification(s)",
                        training?.let {
                            "${it.int("inProgress") ?: 0} in progress · ${it.int("completed") ?: 0} completed"
                        },
                        "$equipment equipment item(s)",
                    ).joinToString(" · "),
                )
            }
            val alerts = p?.obj("cf2StaffAlerts")
            if (alerts != null) {
                CountLine("Certifications expired", alerts.obj("certExpired")?.totalOf() ?: 0)
                CountLine("Certifications expiring", alerts.obj("certExpiring")?.totalOf() ?: 0)
                CountLine("Equipment service due", alerts.obj("serviceDue")?.totalOf() ?: 0)
            }
        }

        // Registers (b17): warranty / license renewal registers
        MirrorCard("Registers") {
            val regs = p?.obj("cf2Registers")
            if (regs == null) {
                Text("Registers could not be loaded.", color = SentinelMuted)
            } else {
                listOf("warranties" to "Warranties", "licenses" to "Licenses").forEach { (key, label) ->
                    val reg = regs.obj(key)
                    Text(
                        "$label · ${reg?.totalOf() ?: 0}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    CountLine("Within 30 days", reg?.obj("within30")?.totalOf() ?: 0)
                    CountLine("Within 60 days", reg?.obj("within60")?.totalOf() ?: 0)
                    CountLine("Within 90 days", reg?.obj("within90")?.totalOf() ?: 0)
                    PanelRows(reg?.items() ?: emptyList(), nav, "Nothing near renewal.")
                }
            }
        }

        // Estate health (b20, OWNER)
        MirrorCard("Estate health") {
            val present = (estate?.get("present") as? JsonPrimitive)?.booleanOrNull ?: false
            val health = estate?.obj("health")
            if (!present || health == null) {
                Text("No estate health report yet.", color = SentinelMuted)
            } else {
                health.entries.forEach { (key, value) ->
                    val text = (value as? JsonPrimitive)?.contentOrNull
                    if (text != null) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(key, modifier = Modifier.weight(1f))
                            Text(text)
                        }
                    }
                }
            }
        }

        // Restore verification & self-monitoring (b4b)
        MirrorCard("Restore verification & self-monitoring") {
            val overdue = p?.obj("cf2BackupOverdue")
            CountLine("Backup restore tests overdue", overdue?.totalOf() ?: 0)
            PanelRows(overdue?.items() ?: emptyList(), nav, "No restore tests are overdue.")
        }

        // Calendar sync (Google) (b4h — read surface only)
        MirrorCard("Calendar sync (Google)") {
            val cal = calendar
            if (cal == null) {
                Text("Calendar sync status could not be loaded.", color = SentinelMuted)
            } else {
                Text("Enabled: ${if (cal.enabled) "yes" else "no"} · Prepared: ${if (cal.prepared) "yes" else "no"} · Live: ${if (cal.live) "yes" else "no"}")
                if (cal.lastSummary.isNotBlank()) Text(cal.lastSummary)
                if (cal.note.isNotBlank()) {
                    Text(cal.note, color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Automation summary (b13) + security/search-health lines
        MirrorCard("Automation summary") {
            CountLine("Assignment rules & escalations", p?.obj("cf2Automation")?.totalOf() ?: 0)
            val security = p?.obj("cf2Security")
            if (security != null) {
                CountLine("Failed sign-ins (24h)", security.int("failedLogins24h") ?: 0)
                val retention = (security["retentionConfigured"] as? JsonPrimitive)?.booleanOrNull ?: false
                Text("Retention configured: ${if (retention) "yes" else "no"}")
                val gaps = security.obj("hygieneGaps")
                if (gaps != null && gaps.totalOf() > 0) {
                    Text("Credential hygiene gaps", style = MaterialTheme.typography.titleSmall)
                    PanelRows(gaps.items(), nav, "No hygiene gaps.")
                }
            }
            val searchHealth = p?.obj("b3fSearchHealth")
            if (searchHealth != null) {
                val divergent = (searchHealth["divergent"] as? JsonPrimitive)?.booleanOrNull ?: false
                Text("Search health: ${if (divergent) "divergent" else "in sync"}")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                scope.launch {
                    try {
                        viewModel.api.runAutomations()
                        viewModel.api.slaScan()
                        actionNote = "All scans run."
                        reloadKey++
                    } catch (e: Exception) {
                        actionNote = plainError(e, "The scans could not all be run. Try again.")
                    }
                }
            }) { Text("Run all scans") }
        }
    }
}

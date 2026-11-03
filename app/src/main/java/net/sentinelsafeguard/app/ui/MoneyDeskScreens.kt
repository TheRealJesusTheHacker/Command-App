package net.sentinelsafeguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.data.api.firmPulse
import net.sentinelsafeguard.app.data.api.moneyAgingDrafts
import net.sentinelsafeguard.app.data.api.moneyAgingScan
import net.sentinelsafeguard.app.data.api.moneyBuildQuote
import net.sentinelsafeguard.app.data.api.moneyBuilder
import net.sentinelsafeguard.app.data.api.moneyConvertUnbilled
import net.sentinelsafeguard.app.data.api.moneyDesk
import net.sentinelsafeguard.app.data.api.moneyPayBoard
import net.sentinelsafeguard.app.data.api.moneyPurchaseDrafts
import net.sentinelsafeguard.app.data.api.moneyQuoteFollowUps
import net.sentinelsafeguard.app.data.api.moneyQuoteFollowUpsScan
import net.sentinelsafeguard.app.data.api.moneyScanLowStock
import net.sentinelsafeguard.app.data.api.moneySetPayState
import net.sentinelsafeguard.app.data.api.moneySetPurchaseDraftState
import net.sentinelsafeguard.app.data.api.moneyUnbilled
import net.sentinelsafeguard.app.data.api.taxPack
import net.sentinelsafeguard.app.ui.theme.SentinelMuted
import java.util.Calendar

/**
 * Money Desk (b3money) + Tax & Profit (b4etax) — Phase 4.
 *
 * Money Desk presents web's sections in web's order under web's
 * own header note: the automations create DRAFTS only — nothing
 * is sent or ordered. The pay board's states are manual states
 * only (invoice status and Stripe fields untouched, as web).
 *
 * Tax & Profit renders the tax-year pack behind the same flag
 * gate web enforces (taxAccess; the backend refuses in its own
 * words — a taxOnly account lands only here). Profit rollups ride
 * the Phase 3 Firm Pulse payload, where web composes them.
 */

@Composable
fun MoneyDeskScreen(viewModel: AppViewModel, nav: NavController) {
    val state = rememberJsonScreen { viewModel.api.moneyDesk() }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val act = rememberAction({ error = it }, { state.reloadKey++ })
    LazyColumn {
        item {
            MirrorHeader("Money Desk")
            Text(
                "Automations create DRAFTS only — nothing is sent or ordered.",
                color = SentinelMuted, modifier = Modifier.padding(horizontal = 16.dp),
            )
            ActionErrorText(error)
            if (notice != null) Text(notice!!, color = SentinelMuted, modifier = Modifier.padding(16.dp))
        }
        when {
            state.error != null -> item {
                Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
            state.data == null -> item { Text("Loading…", color = SentinelMuted, modifier = Modifier.padding(16.dp)) }
            else -> {
                val desk = state.data!!
                item { BuilderCard(viewModel, Phase3Logic.obj(desk, "builder"), act, { notice = it }, { error = it }) }
                item { UnbilledCard(viewModel, Phase3Logic.arr(desk, "unbilled"), act, { notice = it }, { error = it }) }
                item {
                    PurchaseDraftsCard(
                        Phase3Logic.arr(desk, "purchaseDrafts"),
                        onScan = { act { val out = viewModel.api.moneyScanLowStock(); notice = Phase3Logic.str(out, "message") } },
                        onState = { id, st -> act { viewModel.api.moneySetPurchaseDraftState(id, st) } },
                    )
                }
                item {
                    PayBoardCard(
                        Phase3Logic.obj(desk, "payBoard"),
                        nav,
                        onState = { id, st, note -> act { viewModel.api.moneySetPayState(id, st, note) } },
                    )
                }
                item {
                    FollowUpsCard(
                        Phase3Logic.arr(desk, "quoteFollowUps"),
                        onScan = { act { val out = viewModel.api.moneyQuoteFollowUpsScan(); notice = Phase3Logic.str(out, "message") } },
                    )
                }
                item {
                    AgingCard(
                        Phase3Logic.arr(desk, "agingDrafts"),
                        onScan = { act { val out = viewModel.api.moneyAgingScan(); notice = Phase3Logic.str(out, "message") } },
                    )
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun BuilderCard(
    viewModel: AppViewModel,
    builder: JsonObject?,
    act: (suspend () -> Unit) -> Unit,
    onNotice: (String) -> Unit,
    onError: (String) -> Unit,
) {
    var clientId by remember { mutableStateOf<Int?>(null) }
    var quantities by remember { mutableStateOf(mapOf<Int, String>()) }
    val items = Phase3Logic.arr(builder, "items")
    val clients = Phase3Logic.arr(builder, "clients")
    MirrorCard("Quote builder — from the price book", items.size) {
        Text(
            "Build draft quote — saved as a draft in the existing quote flow.",
            color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
        )
        if (clients.isEmpty()) Text("No clients.", color = SentinelMuted)
        else {
            val selected = clients.firstOrNull { Phase3Logic.int(it, "id") == clientId } ?: clients.first()
            ChoiceMenu("Client", Phase3Logic.str(selected, "legalName"), clients.map { Phase3Logic.str(it, "legalName") }) { picked ->
                clients.firstOrNull { Phase3Logic.str(it, "legalName") == picked }
                    ?.let { clientId = Phase3Logic.int(it, "id") }
            }
            if (Phase3Logic.bool(selected, "familyLinked") == true) {
                Text("Family records are never charged.", color = SentinelMuted)
            }
        }
        items.forEach { item ->
            val id = Phase3Logic.int(item, "id") ?: return@forEach
            Row {
                Checkbox(
                    checked = quantities.containsKey(id),
                    onCheckedChange = { on ->
                        quantities = if (on) quantities + (id to "1") else quantities - id
                    },
                )
                Text("${Phase3Logic.str(item, "name")} · ${MirrorVocab.formatCents(Phase3Logic.long(item, "rateCents") ?: 0)}")
            }
            if (quantities.containsKey(id)) {
                TextFieldRow("Quantity", quantities[id] ?: "1", { v -> quantities = quantities + (id to v) })
            }
        }
        Button(onClick = {
            val cid = clientId ?: clients.firstOrNull()?.let { Phase3Logic.int(it, "id") }
            if (cid == null) onError("Choose a client first.")
            else if (quantities.isEmpty()) onError("Pick at least one price book line.")
            else act {
                val lines = buildJsonArray {
                    quantities.forEach { (pid, qty) ->
                        add(buildJsonObject { put("priceItemId", pid); put("quantity", qty.toDoubleOrNull() ?: 1.0) })
                    }
                }
                val out = viewModel.api.moneyBuildQuote(cid, lines)
                onNotice(Phase3Logic.str(out, "message"))
            }
        }) { Text("Build draft quote") }
    }
}

@Composable
private fun UnbilledCard(
    viewModel: AppViewModel,
    unbilled: List<JsonObject>,
    act: (suspend () -> Unit) -> Unit,
    onNotice: (String) -> Unit,
    onError: (String) -> Unit,
) {
    var detail by remember { mutableStateOf<JsonObject?>(null) }
    MirrorCard("Unbilled work — convert to a draft invoice", unbilled.size) {
        if (unbilled.isEmpty()) Text("No unbilled work waiting.", color = SentinelMuted)
        unbilled.forEach { row ->
            val cid = Phase3Logic.int(row, "clientId") ?: return@forEach
            Text(
                "${Phase3Logic.str(row, "clientName")} · ${Phase3Logic.int(row, "timeCount") ?: 0} time · ${Phase3Logic.int(row, "expenseCount") ?: 0} expenses",
                style = MaterialTheme.typography.bodyLarge,
            )
            Row {
                OutlinedButton(onClick = {
                    act {
                        detail = viewModel.api.moneyUnbilled(cid)
                    }
                }) { Text("View unbilled") }
                Button(onClick = {
                    act {
                        val out = viewModel.api.moneyConvertUnbilled(cid)
                        onNotice(Phase3Logic.str(out, "message").ifBlank { "Converted to a draft invoice." })
                    }
                }) { Text("Convert to draft invoice") }
            }
        }
        detail?.let { d ->
            Spacer(Modifier.height(8.dp))
            DetailLine("Time", "${Phase3Logic.int(d, "timeCount") ?: 0} · ${MirrorVocab.formatCents(Phase3Logic.long(d, "timeCents") ?: 0)}")
            DetailLine("Expenses", "${Phase3Logic.int(d, "expenseCount") ?: 0} · ${MirrorVocab.formatCents(Phase3Logic.long(d, "expenseCents") ?: 0)}")
            DetailLine("Total", MirrorVocab.formatCents(Phase3Logic.long(d, "totalCents") ?: 0))
            if (Phase3Logic.bool(d, "familyLinked") == true) {
                Text("Family records are never charged.", color = SentinelMuted)
            }
        }
        if (unbilled.isEmpty()) {
            Text(
                "A client's unbilled detail also opens from a ticket's Time tracking.",
                color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PurchaseDraftsCard(
    drafts: List<JsonObject>,
    onScan: () -> Unit,
    onState: (Int, String) -> Unit,
) {
    MirrorCard("Low-stock purchase drafts", drafts.size) {
        Text(
            "Low-stock scan complete — purchase drafts only, nothing was ordered.",
            color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
        )
        Button(onClick = onScan) { Text("Scan low stock") }
        if (drafts.isEmpty()) Text("No purchase drafts.", color = SentinelMuted)
        drafts.forEach { draft ->
            Text(
                "${Phase3Logic.str(draft, "partName")} · SKU ${Phase3Logic.str(draft, "sku")} · on hand ${Phase3Logic.int(draft, "qtyOnHand") ?: 0} · order ${Phase3Logic.int(draft, "qtyToOrder") ?: 0} · ${MirrorVocab.formatCents(Phase3Logic.long(draft, "estTotalCents") ?: 0)} · ${Phase3Logic.str(draft, "status")}",
            )
            Row {
                Phase4Logic.PURCHASE_DRAFT_STATES.forEach { st ->
                    OutlinedButton(onClick = {
                        Phase3Logic.int(draft, "id")?.let { onState(it, st) }
                    }) { Text(st.replace('_', ' ').replaceFirstChar { it.uppercase() }) }
                }
            }
        }
    }
}

@Composable
private fun PayBoardCard(
    board: JsonObject?,
    nav: NavController,
    onState: (Int, String, String) -> Unit,
) {
    val rows = Phase3Logic.arr(board, "invoices")
    val states = (board?.get("manualStates") as? kotlinx.serialization.json.JsonArray)
        ?.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content } ?: emptyList()
    MirrorCard("Payment status board", rows.size) {
        Text(
            "Manual states only — the invoice's own status is not changed here.",
            color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
        )
        DetailLine("Total balance due", MirrorVocab.formatCents(Phase3Logic.long(board, "totalBalanceDueCents") ?: 0))
        if (rows.isEmpty()) Text("No invoices on the board.", color = SentinelMuted)
        rows.forEach { row ->
            val invoiceId = Phase3Logic.int(row, "invoiceId") ?: return@forEach
            Text(
                "${Phase3Logic.str(row, "invoiceNumber")} · ${Phase3Logic.str(row, "clientName")} · ${Phase3Logic.str(row, "status")} · balance ${MirrorVocab.formatCents(Phase3Logic.long(row, "balanceDueCents") ?: 0)}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "Manual state: ${Phase3Logic.str(row, "manualState")} · Pay link: ${Phase3Logic.str(row, "payLinkState")}",
                color = SentinelMuted,
            )
            if (states.isNotEmpty()) {
                ChoiceMenu("Manual state", Phase3Logic.str(row, "manualState"), states) { picked ->
                    onState(invoiceId, picked, Phase3Logic.str(row, "manualNote"))
                }
            }
            OutlinedButton(onClick = { nav.navigate("invoice/$invoiceId") }) { Text("Open invoice") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FollowUpsCard(followUps: List<JsonObject>, onScan: () -> Unit) {
    MirrorCard("Follow-up drafts (quotes, day 7 / 14)", followUps.size) {
        Text(
            "Quote follow-up scan — drafts only, nothing was sent.",
            color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
        )
        Button(onClick = onScan) { Text("Run follow-up scan") }
        if (followUps.isEmpty()) Text("No follow-up drafts.", color = SentinelMuted)
        followUps.forEach { fu ->
            Text("${Phase3Logic.str(fu, "quoteNumber")} · ${Phase3Logic.str(fu, "clientName")} · day ${Phase3Logic.int(fu, "day") ?: ""}")
            Phase3Logic.str(fu, "subject").takeIf { it.isNotBlank() }?.let { Text(it) }
            Phase3Logic.str(fu, "body").takeIf { it.isNotBlank() }?.let { Text(it, color = SentinelMuted) }
        }
    }
}

@Composable
private fun AgingCard(drafts: List<JsonObject>, onScan: () -> Unit) {
    MirrorCard("Aging drafts (invoices, 30 / 60 / 90 days)", drafts.size) {
        Text(
            "Invoice aging scan — drafts only, nothing was sent.",
            color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
        )
        Button(onClick = onScan) { Text("Run aging scan") }
        if (drafts.isEmpty()) Text("No aging drafts.", color = SentinelMuted)
        drafts.forEach { draft ->
            Text("${Phase3Logic.str(draft, "invoiceNumber")} · ${Phase3Logic.str(draft, "clientName")} · ${Phase3Logic.int(draft, "ageDays") ?: Phase3Logic.int(draft, "bucket") ?: ""}")
            Phase3Logic.str(draft, "subject").takeIf { it.isNotBlank() }?.let { Text(it) }
            Phase3Logic.str(draft, "body").takeIf { it.isNotBlank() }?.let { Text(it, color = SentinelMuted) }
        }
    }
}

// ---- Tax & Profit ----

@Composable
fun TaxProfitScreen(viewModel: AppViewModel, nav: NavController) {
    val context = LocalContext.current
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    var year by remember { mutableStateOf(currentYear) }
    val state = rememberJsonScreen { viewModel.api.taxPack(year) }
    LaunchedEffect(year) { state.reloadKey++ }
    var pulseError by remember { mutableStateOf<String?>(null) }
    LazyColumn {
        item {
            MirrorHeader("Tax & Profit")
            ChoiceMenu("Tax year", year.toString(), Phase4Logic.taxYearOptions(currentYear).map { it.toString() }) {
                year = it.toIntOrNull() ?: currentYear
            }
        }
        when {
            state.error != null -> item {
                // The flag gate refuses in the server's own words
                // (for example a STAFF account: "Not authorized.").
                Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
            state.data == null -> item { Text("Loading…", color = SentinelMuted, modifier = Modifier.padding(16.dp)) }
            else -> {
                val pack = state.data!!
                val summary = Phase3Logic.obj(pack, "summary")
                item {
                    MirrorCard("Tax-year pack") {
                        if (summary != null) {
                            DetailLine("Year", Phase3Logic.int(summary, "year")?.toString())
                            DetailLine("Timezone", Phase3Logic.str(summary, "timezone").ifBlank { null })
                            DetailLine("Invoices", Phase3Logic.int(summary, "invoiceCount")?.toString())
                            DetailLine("Paid total", MirrorVocab.formatCents(Phase3Logic.long(summary, "paidTotalCents") ?: 0))
                            DetailLine("Issued, unpaid", MirrorVocab.formatCents(Phase3Logic.long(summary, "issuedUnpaidTotalCents") ?: 0))
                            DetailLine("Draft invoices", Phase3Logic.int(summary, "draftInvoiceCount")?.toString())
                            DetailLine("Void invoices", Phase3Logic.int(summary, "voidInvoiceCount")?.toString())
                            DetailLine("Payments total", MirrorVocab.formatCents(Phase3Logic.long(summary, "paymentsTotalCents") ?: 0))
                            DetailLine("Expenses total", MirrorVocab.formatCents(Phase3Logic.long(summary, "expensesTotalCents") ?: 0))
                            DetailLine("Mileage", "${Phase3Logic.dbl(summary, "mileageTotalMiles") ?: 0} mi · ${MirrorVocab.formatCents(Phase3Logic.long(summary, "mileageTotalCents") ?: 0)}")
                            Phase3Logic.obj(summary, "familyMemo")?.let { memo ->
                                DetailLine("Family memo", Phase3Logic.str(memo, "note").ifBlank { null })
                            }
                            (summary["notes"] as? kotlinx.serialization.json.JsonArray)?.forEach { note ->
                                Text((note as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "", color = SentinelMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = {
                            val files = Phase3Logic.obj(pack, "files")
                            val bundle = listOf(
                                "invoices.csv" to "invoicesCsv", "expenses.csv" to "expensesCsv",
                                "mileage.csv" to "mileageCsv", "payments.csv" to "paymentsCsv",
                            ).joinToString("\n\n") { (label, key) ->
                                "===== $label =====\n" + Phase3Logic.str(files, key)
                            }
                            shareText(context, Phase3Logic.str(pack, "filename").ifBlank { "Tax pack $year" }, bundle)
                        }) { Text("Download bundle (.zip)") }
                        Text(
                            "The bundle shares the pack's CSV files (invoices.csv, expenses.csv, mileage.csv, payments.csv) as text; the server's .zip and Summary PDF payloads ride the same pack response.",
                            color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
                        )
                        Row {
                            listOf(
                                "invoices.csv" to "invoicesCsv", "expenses.csv" to "expensesCsv",
                                "mileage.csv" to "mileageCsv", "payments.csv" to "paymentsCsv",
                            ).forEach { (label, key) ->
                                OutlinedButton(onClick = {
                                    val files = Phase3Logic.obj(pack, "files")
                                    shareText(context, label, Phase3Logic.str(files, key))
                                }) { Text(label) }
                            }
                        }
                        Phase3Logic.obj(pack, "pdf")?.let { pdf ->
                            DetailLine("Summary PDF", "${Phase3Logic.int(pdf, "pdfSizeBytes") ?: 0} bytes")
                            OutlinedButton(onClick = {
                                // pdfBase64 is scrubbed to length+sha in the
                                // fixtures but is real base64 in production.
                                val b64 = Phase3Logic.str(pdf, "pdfBase64")
                                val bytes = try {
                                    android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                                } catch (e: Exception) { ByteArray(0) }
                                if (bytes.isNotEmpty()) {
                                    shareBytes(context, "tax-summary-$year.pdf", "application/pdf", bytes)
                                }
                            }) { Text("Summary PDF") }
                        }
                    }
                }
                item { ProfitCard(viewModel, { pulseError = it }) }
                if (pulseError != null) {
                    item { Text(pulseError!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ProfitCard(viewModel: AppViewModel, onError: (String) -> Unit) {
    val state = rememberJsonScreen { viewModel.api.firmPulse() }
    LaunchedEffect(state.error) { state.error?.let(onError) }
    MirrorCard("Profit rollups — Firm Pulse") {
        when {
            state.data == null && state.error == null -> Text("Loading…", color = SentinelMuted)
            state.data == null -> Text("Profit rollups are owner-only, as on web.", color = SentinelMuted)
            else -> {
                val pulse = state.data!!
                // Firm Pulse payload (Phase 3 fixture-pinned):
                // revenueByService / revenueByMonth / pipeline /
                // planMrr — web composes Tax & Profit's profit
                // rollups from these same figures.
                Text("Revenue by service", style = MaterialTheme.typography.titleSmall)
                Phase3Logic.arr(pulse, "revenueByService").forEach { row ->
                    DetailLine(
                        Phase3Logic.str(row, "service").ifBlank { Phase3Logic.str(row, "name") },
                        MirrorVocab.formatCents(Phase3Logic.long(row, "totalCents") ?: 0),
                    )
                }
                Text("Revenue by month", style = MaterialTheme.typography.titleSmall)
                Phase3Logic.arr(pulse, "revenueByMonth").forEach { row ->
                    DetailLine(
                        Phase3Logic.str(row, "month"),
                        MirrorVocab.formatCents(Phase3Logic.long(row, "totalCents") ?: 0),
                    )
                }
                Phase3Logic.obj(pulse, "pipeline")?.let { pipeline ->
                    DetailLine("Pipeline", MirrorVocab.formatCents(Phase3Logic.long(pipeline, "totalCents") ?: 0))
                }
                Phase3Logic.obj(pulse, "planMrr")?.let { mrr ->
                    DetailLine("Plan MRR", MirrorVocab.formatCents(Phase3Logic.long(mrr, "totalCents") ?: 0))
                }
                if (Phase3Logic.arr(pulse, "revenueByService").isEmpty() &&
                    Phase3Logic.arr(pulse, "revenueByMonth").isEmpty()
                ) {
                    Text("No profit rollups yet.", color = SentinelMuted)
                }
            }
        }
    }
}

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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.data.api.createInvoice
import net.sentinelsafeguard.app.data.api.createInvoicePaymentLink
import net.sentinelsafeguard.app.data.api.createInvoicePaymentPlan
import net.sentinelsafeguard.app.data.api.createQuote
import net.sentinelsafeguard.app.data.api.deleteInvoice
import net.sentinelsafeguard.app.data.api.deletePriceItem
import net.sentinelsafeguard.app.data.api.editInvoice
import net.sentinelsafeguard.app.data.api.editQuote
import net.sentinelsafeguard.app.data.api.generateReminderDraft
import net.sentinelsafeguard.app.data.api.invoiceDetail
import net.sentinelsafeguard.app.data.api.invoiceDocument
import net.sentinelsafeguard.app.data.api.invoices
import net.sentinelsafeguard.app.data.api.overdueInstallments
import net.sentinelsafeguard.app.data.api.priceBook
import net.sentinelsafeguard.app.data.api.quoteDetail
import net.sentinelsafeguard.app.data.api.quotes
import net.sentinelsafeguard.app.data.api.recordInvoicePayment
import net.sentinelsafeguard.app.data.api.reminderDrafts
import net.sentinelsafeguard.app.data.api.savePriceItem
import net.sentinelsafeguard.app.data.api.setInvoiceStatus
import net.sentinelsafeguard.app.data.api.setQuoteStatus
import net.sentinelsafeguard.app.ui.theme.SentinelMuted

/**
 * Invoices (Phase 4): web's modes in web's order — Board |
 * Directory | Quotes | Price Book | Plans & Overdue — plus the
 * invoice profile and the create/edit form. Money is integer
 * cents on the wire and renders exactly as web formats it.
 * Stripe is flags-OFF in production: the payment-link action
 * surfaces web's own "not enabled" refusal verbatim — no fake
 * payment flow is presented. Family records are never charged
 * (the backend's guard refusal surfaces in its own words);
 * Friends totals carry the explicit 20% discount line the
 * backend payload carries.
 */

private enum class InvMode { Board, Directory, Quotes, PriceBook, Plans }

@Composable
fun InvoicesScreen(viewModel: AppViewModel, nav: NavController) {
    var mode by remember { mutableStateOf(InvMode.Directory) }
    Column {
        MirrorHeader("Invoices", MirrorCopy.subtitle("invoices")) {
            Button(onClick = { nav.navigate("invoiceNew") }) { Text("+ New invoice") }
        }
        Row(Modifier.padding(horizontal = 16.dp)) {
            InvMode.values().forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = { mode = m },
                    label = {
                        Text(
                            when (m) {
                                InvMode.PriceBook -> "Price Book"
                                InvMode.Plans -> "Plans & Overdue"
                                else -> m.name
                            }
                        )
                    },
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        }
        when (mode) {
            InvMode.Board -> InvoiceBoard(viewModel, nav)
            InvMode.Directory -> InvoiceDirectory(viewModel, nav)
            InvMode.Quotes -> QuotesSection(viewModel, nav)
            InvMode.PriceBook -> PriceBookSection(viewModel)
            InvMode.Plans -> PlansSection(viewModel, nav)
        }
    }
}

@Composable
private fun InvoiceStats(stats: JsonObject?) {
    if (stats == null) return
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        StatCard(
            "Outstanding",
            MirrorVocab.formatCents(Phase3Logic.long(stats, "outstandingCents") ?: 0),
            Modifier.weight(1f),
        )
        StatCard(
            "Overdue",
            MirrorVocab.formatCents(Phase3Logic.long(stats, "overdueCents") ?: 0),
            Modifier.weight(1f),
        )
        StatCard(
            "Collected",
            MirrorVocab.formatCents(Phase3Logic.long(stats, "collectedCents") ?: 0),
            Modifier.weight(1f),
        )
        StatCard(
            "Drafts",
            "${Phase3Logic.int(stats, "draftCount") ?: 0}",
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun InvoiceDirectory(viewModel: AppViewModel, nav: NavController) {
    var query by remember { mutableStateOf("") }
    var appliedQuery by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(Phase4Logic.INVOICE_SORTS[0]) }
    var status by remember { mutableStateOf<String?>(null) }
    val state = rememberJsonScreen {
        viewModel.api.invoices(
            status = status, q = appliedQuery,
            sort = Phase4Logic.invoiceSortParam(sort),
        )
    }
    LaunchedEffect(status, appliedQuery, sort) { state.reloadKey++ }
    Column {
        InvoiceStats(state.data?.let { Phase3Logic.obj(it, "stats") })
        TextFieldRow("Search invoices", query, { query = it })
        Row(Modifier.padding(horizontal = 16.dp)) {
            Button(onClick = { appliedQuery = query }) { Text("Search") }
            TextButton(onClick = {
                query = ""; appliedQuery = ""; status = null; sort = Phase4Logic.INVOICE_SORTS[0]
            }) { Text("Reset filters") }
        }
        Text(
            "Search: Number, client, or recipient email",
            color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        ChoiceMenu("Sort invoices", sort, Phase4Logic.INVOICE_SORTS) { sort = it }
        ChipRow(
            listOf("all" to "All") + Phase4Logic.INVOICE_FILTERS.map { it to it },
            status ?: "all",
        ) { picked -> status = picked }
        when {
            state.error != null && state.data == null -> Text(
                state.error!!, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp),
            )
            state.data == null -> SkeletonList(3)
            else -> {
                val rows = Phase3Logic.arr(state.data!!, "invoices")
                val total = Phase3Logic.int(state.data!!, "total") ?: rows.size
                if (rows.isEmpty()) {
                    Text(
                        if (total == 0 && appliedQuery.isBlank() && status == null) "No invoices yet"
                        else "No matches",
                        color = SentinelMuted, modifier = Modifier.padding(16.dp),
                    )
                }
                LazyColumn {
                    items(rows) { inv ->
                        InvoiceRow(inv) {
                            Phase3Logic.int(inv, "id")?.let { nav.navigate("invoice/$it") }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun InvoiceRow(inv: JsonObject, onOpen: () -> Unit) {
    MirrorCard(Phase3Logic.str(inv, "invoiceNumber")) {
        Text(Phase3Logic.str(inv, "clientName"), style = MaterialTheme.typography.bodyLarge)
        Row {
            MirrorChip(Phase3Logic.str(inv, "status"))
            if (Phase4Logic.isOverdueInvoice(inv)) {
                Spacer(Modifier.padding(horizontal = 4.dp)); MirrorChip("Overdue")
            }
            val discount = Phase3Logic.long(inv, "discountCents") ?: 0
            if (discount > 0) {
                Spacer(Modifier.padding(horizontal = 4.dp)); MirrorChip("Friends — 20% off")
            }
        }
        DetailLine("Total", MirrorVocab.formatCents(Phase3Logic.long(inv, "totalCents") ?: 0))
        DetailLine("Balance due", MirrorVocab.formatCents(Phase3Logic.long(inv, "balanceDueCents") ?: 0))
        DetailLine("Due", Phase3Logic.str(inv, "dueDate").ifBlank { null })
        OutlinedButton(onClick = onOpen) { Text("Open profile") }
    }
}

@Composable
private fun InvoiceBoard(viewModel: AppViewModel, nav: NavController) {
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    val state = rememberJsonScreen { viewModel.api.invoices() }
    Column {
        InvoiceStats(state.data?.let { Phase3Logic.obj(it, "stats") })
        ActionErrorText(error)
        when {
            state.error != null && state.data == null -> Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            state.data == null -> SkeletonList(3)
            else -> {
                val rows = Phase3Logic.arr(state.data!!, "invoices")
                LazyColumn {
                    Phase4Logic.INVOICE_STATUSES.forEach { stage ->
                        val cards = rows.filter { Phase3Logic.str(it, "status") == stage }
                        item {
                            Text(
                                "$stage · ${cards.size} invoice(s)",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 16.dp, top = 12.dp),
                            )
                        }
                        if (cards.isEmpty()) {
                            item { Text("No invoices in this stage", color = SentinelMuted, modifier = Modifier.padding(16.dp)) }
                        }
                        items(cards) { inv ->
                            InvoiceRow(inv) { Phase3Logic.int(inv, "id")?.let { nav.navigate("invoice/$it") } }
                            Row(Modifier.padding(horizontal = 16.dp)) {
                                ChoiceMenu("Status", Phase3Logic.str(inv, "status"), Phase4Logic.INVOICE_STATUSES) { picked ->
                                    val id = Phase3Logic.int(inv, "id") ?: return@ChoiceMenu
                                    scope.launch {
                                        try {
                                            viewModel.api.setInvoiceStatus(id, picked); state.reloadKey++
                                        } catch (e: Exception) {
                                            error = plainError(e, "That could not be done. Try again.")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

// ---- Invoice profile ----

@Composable
fun InvoiceProfileScreen(viewModel: AppViewModel, nav: NavController, invoiceId: Int) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val state = rememberJsonScreen { viewModel.api.invoiceDetail(invoiceId) }
    val act = rememberAction({ error = it }, { state.reloadKey++ })
    Column(Modifier.padding(bottom = 24.dp)) {
        OutlinedButton(onClick = { nav.popBackStack() }, modifier = Modifier.padding(16.dp)) { Text("← Back to invoices") }
        when {
            state.error != null && state.data == null -> Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            state.data == null -> SkeletonList(3)
            else -> {
                val data = state.data!!
                val inv = Phase3Logic.obj(data, "invoice") ?: JsonObject(emptyMap())
                val client = Phase3Logic.obj(data, "client")
                MirrorHeader(Phase3Logic.str(inv, "invoiceNumber")) {
                    if (Phase3Logic.str(inv, "status") == "Draft") {
                        Button(onClick = { nav.navigate("invoiceEdit/$invoiceId") }) { Text("Edit") }
                    }
                }
                Row(Modifier.padding(horizontal = 16.dp)) {
                    MirrorChip(Phase3Logic.str(inv, "status"))
                    if (Phase4Logic.isOverdueInvoice(inv)) MirrorChip("Overdue")
                }
                ActionErrorText(error)
                if (notice != null) Text(notice!!, color = SentinelMuted, modifier = Modifier.padding(16.dp))
                LazyColumn {
                    item {
                        MirrorCard("Invoice details") {
                            DetailLine("Client", client?.let { Phase3Logic.str(it, "legalName") })
                            DetailLine("Recipient email", Phase3Logic.str(inv, "recipientEmail").ifBlank { null })
                            DetailLine("Issue date", Phase3Logic.str(inv, "issueDate").ifBlank { null })
                            DetailLine("Due date", Phase3Logic.str(inv, "dueDate").ifBlank { null })
                            DetailLine("Paid date", Phase3Logic.str(inv, "paidDate").ifBlank { null })
                            DetailLine("Notes", Phase3Logic.str(inv, "notes").ifBlank { null })
                            client?.let { c ->
                                OutlinedButton(onClick = {
                                    Phase3Logic.int(c, "id")?.let { nav.navigate("client/$it") }
                                }) { Text("Open client profile") }
                            }
                        }
                        MirrorCard("Line items") {
                            Phase3Logic.arr(inv, "items").forEach { line ->
                                Row(Modifier.fillMaxWidth()) {
                                    Text(Phase3Logic.str(line, "description"), Modifier.weight(1f))
                                    Text("${Phase3Logic.dbl(line, "quantity") ?: 0} × ${MirrorVocab.formatCents(Phase3Logic.long(line, "unitRateCents") ?: 0)}")
                                    Text(
                                        MirrorVocab.formatCents(Phase3Logic.long(line, "lineTotalCents") ?: 0),
                                        Modifier.padding(start = 8.dp),
                                    )
                                }
                            }
                            DetailLine("Subtotal", MirrorVocab.formatCents(Phase3Logic.long(inv, "subtotalCents") ?: 0))
                            val discount = Phase3Logic.long(inv, "discountCents") ?: 0
                            if (discount > 0) {
                                DetailLine("Friends — 20% off", "−" + MirrorVocab.formatCents(discount))
                            }
                            DetailLine("Tax", MirrorVocab.formatCents(Phase3Logic.long(inv, "taxCents") ?: 0))
                            DetailLine("Total", MirrorVocab.formatCents(Phase3Logic.long(inv, "totalCents") ?: 0))
                            DetailLine("Paid", MirrorVocab.formatCents(Phase3Logic.long(inv, "paidCents") ?: 0))
                            DetailLine("Balance due", MirrorVocab.formatCents(Phase3Logic.long(inv, "balanceDueCents") ?: 0))
                        }
                        MirrorCard("Status") {
                            ChoiceMenu("Status", Phase3Logic.str(inv, "status"), Phase4Logic.INVOICE_STATUSES) { picked ->
                                act { viewModel.api.setInvoiceStatus(invoiceId, picked) }
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = {
                                scope.launch {
                                    try {
                                        val doc = viewModel.api.invoiceDocument(invoiceId)
                                        shareText(context, Phase3Logic.str(doc, "invoiceNumber"), Phase3Logic.str(doc, "html"))
                                    } catch (e: Exception) {
                                        error = plainError(e, "That could not be done. Try again.")
                                    }
                                }
                            }) { Text("Print / Save PDF") }
                            OutlinedButton(onClick = {
                                // Payments are flags-OFF in production: the
                                // server refuses in web's own words, and the
                                // mirror shows exactly that honest state.
                                act {
                                    val out = viewModel.api.createInvoicePaymentLink(invoiceId)
                                    notice = Phase3Logic.str(out, "paymentLinkUrl").ifBlank { Phase3Logic.str(out, "message") }
                                }
                            }) { Text("Create payment link") }
                            OutlinedButton(onClick = { confirmDelete = true }) { Text("Delete permanently") }
                        }
                    }
                    item { PaymentsCard(viewModel, invoiceId, Phase3Logic.obj(data, "payments"), act, { error = it }) }
                    item { PaymentPlanCard(viewModel, invoiceId, Phase3Logic.obj(data, "plan"), act, { error = it }) }
                    item {
                        MirrorCard("Invoice activity") {
                            val activity = Phase3Logic.arr(data, "activity")
                            if (activity.isEmpty()) Text("No activity recorded for this invoice yet.", color = SentinelMuted)
                            activity.forEach { row ->
                                Text(Phase3Logic.str(row, "detail"), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        ConfirmGate(
            "Delete permanently",
            "Delete this invoice permanently? This cannot be undone.",
            "Delete permanently",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                scope.launch {
                    try {
                        viewModel.api.deleteInvoice(invoiceId); nav.popBackStack()
                    } catch (e: Exception) {
                        error = plainError(e, "That could not be done. Try again.")
                    }
                }
            },
        )
    }
}

@Composable
private fun PaymentsCard(
    viewModel: AppViewModel,
    invoiceId: Int,
    payments: JsonObject?,
    act: (suspend () -> Unit) -> Unit,
    onError: (String) -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(Phase4Logic.PAYMENT_METHODS[0]) }
    var date by remember { mutableStateOf("") }
    MirrorCard("Payments", Phase3Logic.arr(payments, "payments").size) {
        if (payments != null) {
            DetailLine("Total", MirrorVocab.formatCents(Phase3Logic.long(payments, "totalCents") ?: 0))
            DetailLine("Paid", MirrorVocab.formatCents(Phase3Logic.long(payments, "paidCents") ?: 0))
            DetailLine("Balance due", MirrorVocab.formatCents(Phase3Logic.long(payments, "balanceDueCents") ?: 0))
            Phase3Logic.arr(payments, "payments").forEach { p ->
                Text(
                    "${MirrorVocab.formatCents(Phase3Logic.long(p, "amountCents") ?: 0)} · ${Phase3Logic.str(p, "method")} · ${Phase3Logic.str(p, "paymentDate")}",
                )
            }
        }
        TextFieldRow("Amount ($)", amount, { amount = it })
        ChoiceMenu("Method", method, Phase4Logic.PAYMENT_METHODS) { method = it }
        DateFieldRow("Payment date", date, { date = it })
        Button(onClick = {
            val cents = Phase4Logic.dollarsToCents(amount)
            if (cents == null) onError("Enter the payment amount as dollars, for example 50.00.")
            else act { viewModel.api.recordInvoicePayment(invoiceId, cents, method, date) }
        }) { Text("Record payment") }
    }
}

@Composable
private fun PaymentPlanCard(
    viewModel: AppViewModel,
    invoiceId: Int,
    plan: JsonObject?,
    act: (suspend () -> Unit) -> Unit,
    onError: (String) -> Unit,
) {
    var rows by remember { mutableStateOf(listOf("" to "")) }
    var notes by remember { mutableStateOf("") }
    MirrorCard("Payment plan") {
        if (plan == null) {
            Text("No payment plan for this invoice.", color = SentinelMuted)
            rows.forEachIndexed { idx, (amt, due) ->
                TextFieldRow("Installment ${idx + 1} amount ($)", amt, { v ->
                    rows = rows.toMutableList().also { it[idx] = v to due }
                })
                DateFieldRow("Installment ${idx + 1} due date", due, { v ->
                    rows = rows.toMutableList().also { it[idx] = amt to v }
                })
            }
            TextButton(onClick = { rows = rows + ("" to "") }) { Text("+ Add installment") }
            TextFieldRow("Notes", notes, { notes = it })
            Button(onClick = {
                val built = rows.mapNotNull { (amt, due) ->
                    Phase4Logic.dollarsToCents(amt)?.let { cents ->
                        buildJsonObject { put("amountCents", cents); put("dueDate", due) }
                    }
                }
                if (built.isEmpty()) onError("Add at least one installment amount.")
                else act {
                    viewModel.api.createInvoicePaymentPlan(
                        invoiceId, JsonArray(built), notes,
                    )
                }
            }) { Text("Create payment plan") }
        } else {
            DetailLine("Status", Phase3Logic.str(plan, "status"))
            DetailLine("Total", MirrorVocab.formatCents(Phase3Logic.long(plan, "totalCents") ?: 0))
            DetailLine("Paid", MirrorVocab.formatCents(Phase3Logic.long(plan, "paidCents") ?: 0))
            Phase3Logic.arr(plan, "installments").forEach { inst ->
                Text(
                    "${Phase3Logic.int(inst, "seq")}. ${MirrorVocab.formatCents(Phase3Logic.long(inst, "amountCents") ?: 0)} · due ${Phase3Logic.str(inst, "dueDate")} · ${Phase3Logic.str(inst, "status")} · remaining ${MirrorVocab.formatCents(Phase3Logic.long(inst, "remainingCents") ?: 0)}",
                )
            }
        }
    }
}

// ---- Invoice create / edit form ----

@Composable
fun InvoiceFormScreen(viewModel: AppViewModel, nav: NavController, invoiceId: Int?) {
    val scope = rememberCoroutineScope()
    var clients by remember { mutableStateOf<List<net.sentinelsafeguard.app.data.api.Client>>(emptyList()) }
    var clientId by remember { mutableStateOf<Int?>(null) }
    var issueDate by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var recipientEmail by remember { mutableStateOf("") }
    var taxRate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var lines by remember { mutableStateOf(listOf(Triple("", "1", ""))) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        clients = try { viewModel.api.clients().items } catch (e: Exception) { emptyList() }
        if (invoiceId != null) {
            try {
                val inv = Phase3Logic.obj(viewModel.api.invoiceDetail(invoiceId), "invoice")
                if (inv != null) {
                    clientId = Phase3Logic.int(inv, "clientId")
                    issueDate = Phase3Logic.str(inv, "issueDate")
                    dueDate = Phase3Logic.str(inv, "dueDate")
                    recipientEmail = Phase3Logic.str(inv, "recipientEmail")
                    taxRate = (Phase3Logic.dbl(inv, "taxRatePercent") ?: 0.0).toString()
                    notes = Phase3Logic.str(inv, "notes")
                    lines = Phase3Logic.arr(inv, "items").map {
                        Triple(
                            Phase3Logic.str(it, "description"),
                            (Phase3Logic.dbl(it, "quantity") ?: 1.0).toString(),
                            Phase4Logic.centsToDollarsText(Phase3Logic.long(it, "unitRateCents") ?: 0),
                        )
                    }.ifEmpty { listOf(Triple("", "1", "")) }
                }
            } catch (e: Exception) {
                error = plainError(e, "This invoice could not be loaded. Try again.")
            }
        }
    }
    LazyColumn {
        item {
            MirrorHeader(if (invoiceId == null) "New invoice" else "Edit invoice")
            ActionErrorText(error)
            if (clients.isEmpty()) Text("Loading clients…", color = SentinelMuted, modifier = Modifier.padding(16.dp))
            else {
                val selected = clients.firstOrNull { it.id == clientId } ?: clients.first()
                ChoiceMenu("Client", selected.legalName, clients.map { it.legalName }) { picked ->
                    clients.firstOrNull { it.legalName == picked }?.let { clientId = it.id }
                }
            }
            DateFieldRow("Issue date", issueDate, { issueDate = it })
            DateFieldRow("Due date", dueDate, { dueDate = it })
            TextFieldRow("Recipient email", recipientEmail, { recipientEmail = it })
            TextFieldRow("Tax rate (%)", taxRate, { taxRate = it })
            Text("Line items", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
            lines.forEachIndexed { idx, (desc, qty, rate) ->
                TextFieldRow("Description", desc, { v -> lines = lines.toMutableList().also { it[idx] = Triple(v, qty, rate) } })
                Row {
                    Column(Modifier.weight(1f)) {
                        TextFieldRow("Qty", qty, { v -> lines = lines.toMutableList().also { it[idx] = Triple(desc, v, rate) } })
                    }
                    Column(Modifier.weight(1f)) {
                        TextFieldRow("Rate $", rate, { v -> lines = lines.toMutableList().also { it[idx] = Triple(desc, qty, v) } })
                    }
                }
                if (lines.size > 1) {
                    TextButton(onClick = { lines = lines.toMutableList().also { it.removeAt(idx) } }) { Text("Remove line item") }
                }
            }
            TextButton(onClick = { lines = lines + Triple("", "1", "") }) { Text("+ Add line item") }
            val parsed = lines.map { (d, q, r) -> Triple(d, q.toDoubleOrNull() ?: 0.0, Phase4Logic.dollarsToCents(r) ?: 0) }
            val (subtotal, tax, total) = Phase4Logic.previewTotals(parsed, taxRate.toDoubleOrNull() ?: 0.0)
            DetailLine("Subtotal", MirrorVocab.formatCents(subtotal))
            DetailLine("Tax", MirrorVocab.formatCents(tax))
            DetailLine("Total", MirrorVocab.formatCents(total))
            TextFieldRow("Notes", notes, { notes = it }, singleLine = false, minLines = 3)
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                val cid = clientId ?: clients.firstOrNull()?.id
                if (cid == null) {
                    error = "Choose a client first."
                    return@Button
                }
                val items = buildJsonArray {
                    parsed.filter { it.first.isNotBlank() }.forEach { (d, q, r) ->
                        add(buildJsonObject {
                            put("description", d); put("quantity", q); put("unitRateCents", r)
                        })
                    }
                }
                scope.launch {
                    try {
                        if (invoiceId == null) {
                            viewModel.api.createInvoice(buildJsonObject {
                                put("clientId", cid); put("issueDate", issueDate); put("dueDate", dueDate)
                                put("recipientEmail", recipientEmail); put("notes", notes)
                                put("taxRatePercent", taxRate.toDoubleOrNull() ?: 0.0); put("items", items)
                            })
                        } else {
                            viewModel.api.editInvoice(
                                invoiceId,
                                buildJsonObject {
                                    put("clientId", cid); put("issueDate", issueDate); put("dueDate", dueDate)
                                    put("recipientEmail", recipientEmail); put("notes", notes)
                                    put("taxRatePercent", taxRate.toDoubleOrNull() ?: 0.0); put("items", items)
                                },
                            )
                        }
                        nav.popBackStack()
                    } catch (e: Exception) {
                        error = plainError(e, "This invoice could not be saved. Try again.")
                    }
                }
            }) { Text("Save invoice") }
            OutlinedButton(onClick = { nav.popBackStack() }) { Text("Cancel") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---- Quotes sub-view ----

@Composable
private fun QuotesSection(viewModel: AppViewModel, nav: NavController) {
    val state = rememberJsonScreen { viewModel.api.quotes() }
    Column {
        Row(Modifier.padding(horizontal = 16.dp)) {
            Button(onClick = { nav.navigate("quoteNew") }) { Text("+ New quote") }
        }
        when {
            state.error != null && state.data == null -> Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            state.data == null -> SkeletonList(3)
            else -> {
                val rows = Phase3Logic.arr(state.data!!, "quotes")
                if (rows.isEmpty()) Text("No quotes yet.", color = SentinelMuted, modifier = Modifier.padding(16.dp))
                LazyColumn {
                    items(rows) { q ->
                        MirrorCard(Phase3Logic.str(q, "quoteNumber")) {
                            Text(Phase3Logic.str(q, "title"), style = MaterialTheme.typography.bodyLarge)
                            Text(Phase3Logic.str(q, "clientName"), color = SentinelMuted)
                            Row {
                                MirrorChip(Phase3Logic.str(q, "status"))
                                val discount = Phase3Logic.long(q, "discountCents") ?: 0
                                if (discount > 0) MirrorChip("Friends — 20% off")
                            }
                            DetailLine("Total", MirrorVocab.formatCents(Phase3Logic.long(q, "totalCents") ?: 0))
                            OutlinedButton(onClick = {
                                Phase3Logic.int(q, "id")?.let { nav.navigate("quote/$it") }
                            }) { Text("Open profile") }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
fun QuoteProfileScreen(viewModel: AppViewModel, nav: NavController, quoteId: Int) {
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val state = rememberJsonScreen { viewModel.api.quoteDetail(quoteId) }
    val act = rememberAction({ error = it }, { state.reloadKey++ })
    Column {
        OutlinedButton(onClick = { nav.popBackStack() }, modifier = Modifier.padding(16.dp)) { Text("← Back to Quotes") }
        when {
            state.error != null && state.data == null -> Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            state.data == null -> SkeletonList(3)
            else -> {
                val q = Phase3Logic.obj(state.data!!, "quote") ?: JsonObject(emptyMap())
                MirrorHeader(Phase3Logic.str(q, "quoteNumber")) {
                    Button(onClick = { nav.navigate("quoteEdit/$quoteId") }) { Text("Edit") }
                }
                ActionErrorText(error)
                if (notice != null) Text(notice!!, color = SentinelMuted, modifier = Modifier.padding(16.dp))
                LazyColumn {
                    item {
                        MirrorCard("Quote details") {
                            DetailLine("Title", Phase3Logic.str(q, "title").ifBlank { null })
                            DetailLine("Client", Phase3Logic.str(q, "clientName").ifBlank { null })
                            DetailLine("Status", Phase3Logic.str(q, "status"))
                            DetailLine("Convert to", Phase3Logic.str(q, "convertTo").ifBlank { null })
                            DetailLine("Expires", Phase3Logic.str(q, "expiresDate").ifBlank { null })
                            DetailLine("Notes", Phase3Logic.str(q, "notes").ifBlank { null })
                            Phase3Logic.arr(q, "lines").forEach { line ->
                                Row(Modifier.fillMaxWidth()) {
                                    Text(Phase3Logic.str(line, "description"), Modifier.weight(1f))
                                    Text("${Phase3Logic.dbl(line, "quantity") ?: 0} × ${MirrorVocab.formatCents(Phase3Logic.long(line, "unitRateCents") ?: 0)}")
                                    Text(MirrorVocab.formatCents(Phase3Logic.long(line, "lineTotalCents") ?: 0), Modifier.padding(start = 8.dp))
                                }
                            }
                            DetailLine("Subtotal", MirrorVocab.formatCents(Phase3Logic.long(q, "subtotalCents") ?: 0))
                            val discount = Phase3Logic.long(q, "discountCents") ?: 0
                            if (discount > 0) DetailLine("Friends — 20% off", "−" + MirrorVocab.formatCents(discount))
                            DetailLine("Tax", MirrorVocab.formatCents(Phase3Logic.long(q, "taxCents") ?: 0))
                            DetailLine("Total", MirrorVocab.formatCents(Phase3Logic.long(q, "totalCents") ?: 0))
                            Phase3Logic.int(q, "convertedTicketId")?.let { DetailLine("Converted ticket", "#$it") }
                            Phase3Logic.int(q, "convertedCaseId")?.let { DetailLine("Converted case", "#$it") }
                            Phase3Logic.int(q, "convertedInvoiceId")?.let { DetailLine("Converted invoice", "#$it") }
                        }
                        MirrorCard("Status") {
                            Text(
                                "Approving a quote converts it as web does — to the ticket, case, or invoice named in Convert to.",
                                color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
                            )
                            ChoiceMenu("Status", Phase3Logic.str(q, "status"), Phase4Logic.QUOTE_STATUSES) { picked ->
                                act {
                                    val out = viewModel.api.setQuoteStatus(quoteId, picked)
                                    Phase3Logic.obj(out, "conversion")?.let { conv ->
                                        notice = listOfNotNull(
                                            Phase3Logic.int(conv, "ticketId")?.let { "Ticket #$it" },
                                            Phase3Logic.int(conv, "caseId")?.let { "Case #$it" },
                                            Phase3Logic.int(conv, "invoiceId")?.let { "Invoice #$it" },
                                        ).joinToString(" · ").ifBlank { null }?.let { "Converted: $it" }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
fun QuoteFormScreen(viewModel: AppViewModel, nav: NavController, quoteId: Int?) {
    val scope = rememberCoroutineScope()
    var clients by remember { mutableStateOf<List<net.sentinelsafeguard.app.data.api.Client>>(emptyList()) }
    var priceItems by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var clientId by remember { mutableStateOf<Int?>(null) }
    var title by remember { mutableStateOf("") }
    var convertTo by remember { mutableStateOf(Phase4Logic.QUOTE_CONVERT_TO[0]) }
    var taxRate by remember { mutableStateOf("") }
    var expiresDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf(setOf<Int>()) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        clients = try { viewModel.api.clients().items } catch (e: Exception) { emptyList() }
        priceItems = try { Phase3Logic.arr(viewModel.api.priceBook(), "items") } catch (e: Exception) { emptyList() }
        if (quoteId != null) {
            try {
                val q = Phase3Logic.obj(viewModel.api.quoteDetail(quoteId), "quote")
                if (q != null) {
                    clientId = Phase3Logic.int(q, "clientId")
                    title = Phase3Logic.str(q, "title")
                    convertTo = Phase3Logic.str(q, "convertTo").ifBlank { convertTo }
                    taxRate = (Phase3Logic.dbl(q, "taxRatePercent") ?: 0.0).toString()
                    expiresDate = Phase3Logic.str(q, "expiresDate")
                    notes = Phase3Logic.str(q, "notes")
                    picked = Phase3Logic.arr(q, "lines").mapNotNull { Phase3Logic.int(it, "priceItemId") }.toSet()
                }
            } catch (e: Exception) {
                error = plainError(e, "This quote could not be loaded. Try again.")
            }
        }
    }
    LazyColumn {
        item {
            MirrorHeader(if (quoteId == null) "New quote" else "Edit quote")
            ActionErrorText(error)
            if (clients.isEmpty()) Text("Loading clients…", color = SentinelMuted, modifier = Modifier.padding(16.dp))
            else {
                val selected = clients.firstOrNull { it.id == clientId } ?: clients.first()
                ChoiceMenu("Client", selected.legalName, clients.map { it.legalName }) { p ->
                    clients.firstOrNull { it.legalName == p }?.let { clientId = it.id }
                }
            }
            TextFieldRow("Title", title, { title = it })
            ChoiceMenu("Convert to", convertTo, Phase4Logic.QUOTE_CONVERT_TO) { convertTo = it }
            TextFieldRow("Tax rate (%)", taxRate, { taxRate = it })
            DateFieldRow("Expires date", expiresDate, { expiresDate = it })
            Text("Price book lines", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
            priceItems.forEach { item ->
                val id = Phase3Logic.int(item, "id") ?: return@forEach
                Row {
                    androidx.compose.material3.Checkbox(
                        checked = id in picked,
                        onCheckedChange = { on -> picked = if (on) picked + id else picked - id },
                    )
                    Text("${Phase3Logic.str(item, "name")} · ${MirrorVocab.formatCents(Phase3Logic.long(item, "rateCents") ?: 0)}")
                }
            }
            TextFieldRow("Notes", notes, { notes = it }, singleLine = false, minLines = 3)
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                val cid = clientId ?: clients.firstOrNull()?.id
                if (cid == null) {
                    error = "Choose a client first."
                    return@Button
                }
                val lines = buildJsonArray {
                    picked.forEach { pid -> add(buildJsonObject { put("priceItemId", pid); put("quantity", 1) }) }
                }
                scope.launch {
                    try {
                        if (quoteId == null) {
                            viewModel.api.createQuote(buildJsonObject {
                                put("clientId", cid); put("title", title); put("convertTo", convertTo)
                                put("taxRatePercent", taxRate.toDoubleOrNull() ?: 0.0)
                                put("expiresDate", expiresDate); put("notes", notes); put("lines", lines)
                            })
                        } else {
                            viewModel.api.editQuote(
                                quoteId,
                                buildJsonObject {
                                    put("title", title); put("convertTo", convertTo)
                                    put("taxRatePercent", taxRate.toDoubleOrNull() ?: 0.0)
                                    put("expiresDate", expiresDate); put("notes", notes); put("lines", lines)
                                },
                            )
                        }
                        nav.popBackStack()
                    } catch (e: Exception) {
                        error = plainError(e, "This quote could not be saved. Try again.")
                    }
                }
            }) { Text("Save quote") }
            OutlinedButton(onClick = { nav.popBackStack() }) { Text("Cancel") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---- Price Book sub-view ----

@Composable
private fun PriceBookSection(viewModel: AppViewModel) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("flat") }
    var editingId by remember { mutableStateOf<Int?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val state = rememberJsonScreen { viewModel.api.priceBook() }
    val act = rememberAction({ error = it }, { state.reloadKey++ })
    Column {
        ActionErrorText(error)
        MirrorCard(if (editingId == null) "New price item" else "Edit price item") {
            TextFieldRow("Name", name, { name = it })
            TextFieldRow("Description", description, { description = it })
            TextFieldRow("Rate ($)", rate, { rate = it })
            TextFieldRow("Unit", unit, { unit = it })
            Button(onClick = {
                val cents = Phase4Logic.dollarsToCents(rate)
                if (name.isBlank()) error = "Name is required."
                else if (cents == null) error = "Enter the rate as dollars, for example 500.00."
                else act {
                    viewModel.api.savePriceItem(
                        buildJsonObject {
                            put("name", name); put("description", description)
                            put("rateCents", cents); put("unit", unit); put("active", true)
                        },
                        editingId,
                    )
                    name = ""; description = ""; rate = ""; editingId = null
                }
            }) { Text("Save price item") }
            if (editingId != null) {
                TextButton(onClick = { editingId = null; name = ""; description = ""; rate = "" }) { Text("Cancel edit") }
            }
        }
        when {
            state.error != null && state.data == null -> Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            state.data == null -> SkeletonList(3)
            else -> {
                val rows = Phase3Logic.arr(state.data!!, "items")
                if (rows.isEmpty()) Text("No price items yet.", color = SentinelMuted, modifier = Modifier.padding(16.dp))
                LazyColumn {
                    items(rows) { item ->
                        MirrorCard(Phase3Logic.str(item, "name")) {
                            DetailLine("Description", Phase3Logic.str(item, "description").ifBlank { null })
                            DetailLine("Rate", MirrorVocab.formatCents(Phase3Logic.long(item, "rateCents") ?: 0))
                            DetailLine("Unit", Phase3Logic.str(item, "unit").ifBlank { null })
                            DetailLine("Active", if (Phase3Logic.bool(item, "active") == true) "Yes" else "No")
                            Row {
                                OutlinedButton(onClick = {
                                    editingId = Phase3Logic.int(item, "id")
                                    name = Phase3Logic.str(item, "name")
                                    description = Phase3Logic.str(item, "description")
                                    rate = Phase4Logic.centsToDollarsText(Phase3Logic.long(item, "rateCents") ?: 0)
                                    unit = Phase3Logic.str(item, "unit").ifBlank { "flat" }
                                }) { Text("Edit") }
                                TextButton(onClick = {
                                    Phase3Logic.int(item, "id")?.let { id -> act { viewModel.api.deletePriceItem(id) } }
                                }) { Text("Delete") }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

// ---- Plans & Overdue sub-view ----

@Composable
private fun PlansSection(viewModel: AppViewModel, nav: NavController) {
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val overdueState = rememberJsonScreen { viewModel.api.overdueInstallments() }
    val draftsState = rememberJsonScreen { viewModel.api.reminderDrafts() }
    val act = rememberAction({ error = it }, { overdueState.reloadKey++; draftsState.reloadKey++ })
    LazyColumn {
        item {
            ActionErrorText(error)
            if (notice != null) Text(notice!!, color = SentinelMuted, modifier = Modifier.padding(16.dp))
            MirrorCard("Overdue installments", overdueState.data?.let { Phase3Logic.int(it, "count") }) {
                when {
                    overdueState.error != null && overdueState.data == null -> Text(overdueState.error!!, color = MaterialTheme.colorScheme.error)
                    overdueState.data == null -> SkeletonList(3)
                    else -> {
                        val rows = Phase3Logic.arr(overdueState.data!!, "overdue")
                        if (rows.isEmpty()) Text("No overdue installments.", color = SentinelMuted)
                        rows.forEach { row ->
                            Text(
                                "${Phase3Logic.str(row, "invoiceNumber")} · installment ${Phase3Logic.int(row, "seq")} · ${Phase3Logic.str(row, "clientName")} · remaining ${MirrorVocab.formatCents(Phase3Logic.long(row, "remainingCents") ?: 0)} · due ${Phase3Logic.str(row, "dueDate")}",
                            )
                            OutlinedButton(onClick = {
                                Phase3Logic.int(row, "invoiceId")?.let { nav.navigate("invoice/$it") }
                            }) { Text("Open invoice") }
                            Button(onClick = {
                                val inst = Phase3Logic.int(row, "installmentId") ?: return@Button
                                act {
                                    val out = viewModel.api.generateReminderDraft(inst)
                                    notice = Phase3Logic.str(out, "message")
                                }
                            }) { Text("Generate reminder draft") }
                        }
                    }
                }
            }
            MirrorCard("Reminder drafts", draftsState.data?.let { Phase3Logic.arr(it, "drafts").size }) {
                Text(
                    "Drafts only — nothing is sent. Review a draft and send it by hand when ready.",
                    color = SentinelMuted, style = MaterialTheme.typography.bodySmall,
                )
                when {
                    draftsState.error != null && draftsState.data == null -> Text(draftsState.error!!, color = MaterialTheme.colorScheme.error)
                    draftsState.data == null -> SkeletonList(3)
                    else -> {
                        val rows = Phase3Logic.arr(draftsState.data!!, "drafts")
                        if (rows.isEmpty()) Text("No reminder drafts.", color = SentinelMuted)
                        rows.forEach { draft ->
                            Text(Phase3Logic.str(draft, "subject"), style = MaterialTheme.typography.bodyLarge)
                            Text(Phase3Logic.str(draft, "body"), color = SentinelMuted)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

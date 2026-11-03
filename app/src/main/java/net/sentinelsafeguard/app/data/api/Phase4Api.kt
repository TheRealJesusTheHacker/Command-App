package net.sentinelsafeguard.app.data.api

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Phase 4 (hunt8) endpoint methods: Invoices (directory / profile /
 * create / edit / status / payments / payment plan / reminder
 * drafts / document / payment link), Quotes, Price Book, Money
 * Desk (b3money), and Tax & Profit (b4etax).
 *
 * Every method rides the deployed Phase 4 backend (Addendum 16a;
 * production server.py sha256 6b07dca8…), whose payloads are the
 * web handlers' own — so these ride the open-JSON verbs on
 * [SentinelApi] exactly like Phase 3. Refusals surface the server's
 * own message via [SentinelApi.callJson] where the honest state
 * matters (Family never-charged, overpay, payments-disabled).
 *
 * Request bodies mirror phase4-work/rehearsal_capture.py field
 * for field.
 */

private fun JsonObject.toBody4(): String = toString()

// ---- Invoices ----

suspend fun SentinelApi.invoices(
    status: String? = null,
    q: String = "",
    sort: String = "id_desc",
    clientId: Int? = null,
    overdue: Boolean = false,
): JsonObject {
    val parts = mutableListOf<String>("limit=200")
    if (!status.isNullOrBlank()) parts += "status=${encodeQuery(status)}"
    if (q.isNotBlank()) parts += "q=${encodeQuery(q)}"
    if (sort != "id_desc") parts += "sort=${encodeQuery(sort)}"
    if (clientId != null) parts += "clientId=$clientId"
    if (overdue) parts += "overdue=1"
    val suffix = if (parts.isEmpty()) "" else "?" + parts.joinToString("&")
    return getJson("/api/app/invoices$suffix")
}

suspend fun SentinelApi.invoiceDetail(id: Int): JsonObject =
    getJson("/api/app/invoices/$id")

suspend fun SentinelApi.createInvoice(invoice: JsonObject): JsonObject =
    callJson("POST", "/api/app/invoices", buildJsonObject { put("invoice", invoice) }.toBody4())

suspend fun SentinelApi.editInvoice(id: Int, body: JsonObject): JsonObject =
    callJson("PATCH", "/api/app/invoices/$id", body.toBody4())

suspend fun SentinelApi.setInvoiceStatus(id: Int, status: String): JsonObject =
    postJson("/api/app/invoices/$id/status", buildJsonObject { put("status", status) }.toBody4())

suspend fun SentinelApi.deleteInvoice(id: Int): JsonObject =
    deleteJson("/api/app/invoices/$id")

suspend fun SentinelApi.invoiceDocument(id: Int): JsonObject =
    getJson("/api/app/invoices/$id/document")

suspend fun SentinelApi.invoicePayments(id: Int): JsonObject =
    getJson("/api/app/invoices/$id/payments")

suspend fun SentinelApi.recordInvoicePayment(
    id: Int,
    amountCents: Long,
    method: String,
    paymentDate: String,
): JsonObject = callJson(
    "POST", "/api/app/invoices/$id/payments",
    buildJsonObject {
        put("amountCents", amountCents); put("method", method)
        if (paymentDate.isNotBlank()) put("paymentDate", paymentDate)
    }.toBody4(),
)

suspend fun SentinelApi.invoicePaymentPlan(id: Int): JsonObject =
    getJson("/api/app/invoices/$id/payment-plan")

suspend fun SentinelApi.createInvoicePaymentPlan(
    id: Int,
    installments: kotlinx.serialization.json.JsonArray,
    notes: String,
): JsonObject = postJson(
    "/api/app/invoices/$id/payment-plan",
    buildJsonObject { put("installments", installments); put("notes", notes) }.toBody4(),
)

suspend fun SentinelApi.createInvoicePaymentLink(id: Int): JsonObject =
    callJson("POST", "/api/app/invoices/$id/payment-link", "{}")

suspend fun SentinelApi.completeInvoicePayment(sessionId: String): JsonObject =
    callJson(
        "POST", "/api/app/invoices/complete-payment",
        buildJsonObject { put("sessionId", sessionId) }.toBody4(),
    )

suspend fun SentinelApi.overdueInstallments(): JsonObject =
    getJson("/api/app/overdue-installments")

suspend fun SentinelApi.reminderDrafts(): JsonObject =
    getJson("/api/app/reminder-drafts")

suspend fun SentinelApi.generateReminderDraft(installmentId: Int): JsonObject =
    postJson(
        "/api/app/reminder-drafts/generate",
        buildJsonObject { put("installmentId", installmentId) }.toBody4(),
    )

// ---- Quotes (Invoices sub-view) ----

suspend fun SentinelApi.quotes(): JsonObject = getJson("/api/app/quotes")

suspend fun SentinelApi.quoteDetail(id: Int): JsonObject = getJson("/api/app/quotes/$id")

suspend fun SentinelApi.createQuote(quote: JsonObject): JsonObject =
    postJson("/api/app/quotes", buildJsonObject { put("quote", quote) }.toBody4())

suspend fun SentinelApi.editQuote(id: Int, body: JsonObject): JsonObject =
    patchJson("/api/app/quotes/$id", body.toBody4())

suspend fun SentinelApi.setQuoteStatus(id: Int, status: String): JsonObject =
    callJson(
        "POST", "/api/app/quotes/$id/status",
        buildJsonObject { put("status", status) }.toBody4(),
    )

// ---- Price Book (Invoices sub-view) ----

suspend fun SentinelApi.priceBook(): JsonObject = getJson("/api/app/price-book")

suspend fun SentinelApi.savePriceItem(item: JsonObject, id: Int?): JsonObject {
    val body = buildJsonObject { put("item", item) }.toBody4()
    return if (id == null) postJson("/api/app/price-book", body)
    else patchJson("/api/app/price-book/$id", body)
}

suspend fun SentinelApi.deletePriceItem(id: Int): JsonObject =
    deleteJson("/api/app/price-book/$id")

// ---- Money Desk (b3money) ----

suspend fun SentinelApi.moneyDesk(): JsonObject = getJson("/api/app/money-desk")

suspend fun SentinelApi.moneyBuilder(): JsonObject = getJson("/api/app/money-desk/builder")

suspend fun SentinelApi.moneyBuildQuote(clientId: Int, lines: kotlinx.serialization.json.JsonArray): JsonObject =
    callJson(
        "POST", "/api/app/money-desk/build-quote",
        buildJsonObject { put("clientId", clientId); put("lines", lines) }.toBody4(),
    )

suspend fun SentinelApi.moneyUnbilled(clientId: Int): JsonObject =
    getJson("/api/app/money-desk/unbilled?clientId=$clientId")

suspend fun SentinelApi.moneyConvertUnbilled(clientId: Int): JsonObject =
    callJson(
        "POST", "/api/app/money-desk/convert-unbilled",
        buildJsonObject { put("clientId", clientId) }.toBody4(),
    )

suspend fun SentinelApi.moneyScanLowStock(): JsonObject =
    postJson("/api/app/money-desk/scan-low-stock", "{}")

suspend fun SentinelApi.moneyPurchaseDrafts(): JsonObject =
    getJson("/api/app/money-desk/purchase-drafts")

suspend fun SentinelApi.moneySetPurchaseDraftState(draftId: Int, state: String): JsonObject =
    postJson(
        "/api/app/money-desk/purchase-drafts/$draftId/state",
        buildJsonObject { put("state", state) }.toBody4(),
    )

suspend fun SentinelApi.moneyPayBoard(): JsonObject = getJson("/api/app/money-desk/pay-board")

suspend fun SentinelApi.moneySetPayState(invoiceId: Int, state: String, note: String): JsonObject =
    callJson(
        "POST", "/api/app/money-desk/pay-board/$invoiceId/state",
        buildJsonObject { put("state", state); put("note", note) }.toBody4(),
    )

suspend fun SentinelApi.moneyQuoteFollowUpsScan(): JsonObject =
    postJson("/api/app/money-desk/quote-followups/scan", "{}")

suspend fun SentinelApi.moneyQuoteFollowUps(): JsonObject =
    getJson("/api/app/money-desk/quote-followups")

suspend fun SentinelApi.moneyAgingScan(): JsonObject =
    postJson("/api/app/money-desk/aging/scan", "{}")

suspend fun SentinelApi.moneyAgingDrafts(): JsonObject =
    getJson("/api/app/money-desk/aging-drafts")

// ---- Tax & Profit (b4etax) ----

suspend fun SentinelApi.taxPack(year: Int): JsonObject =
    callJson("GET", "/api/app/tax-pack?year=$year")

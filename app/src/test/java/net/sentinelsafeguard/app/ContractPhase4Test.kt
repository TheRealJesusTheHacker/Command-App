package net.sentinelsafeguard.app

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.config.ApiConfig
import net.sentinelsafeguard.app.data.api.ApiException
import net.sentinelsafeguard.app.data.api.HttpRequest
import net.sentinelsafeguard.app.data.api.HttpResponse
import net.sentinelsafeguard.app.data.api.HttpTransport
import net.sentinelsafeguard.app.data.api.SentinelApi
import net.sentinelsafeguard.app.data.api.completeInvoicePayment
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
import net.sentinelsafeguard.app.data.api.invoicePaymentPlan
import net.sentinelsafeguard.app.data.api.invoicePayments
import net.sentinelsafeguard.app.data.api.invoices
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
import net.sentinelsafeguard.app.data.api.overdueInstallments
import net.sentinelsafeguard.app.data.api.priceBook
import net.sentinelsafeguard.app.data.api.quoteDetail
import net.sentinelsafeguard.app.data.api.quotes
import net.sentinelsafeguard.app.data.api.recordInvoicePayment
import net.sentinelsafeguard.app.data.api.reminderDrafts
import net.sentinelsafeguard.app.data.api.savePriceItem
import net.sentinelsafeguard.app.data.api.setInvoiceStatus
import net.sentinelsafeguard.app.data.api.setQuoteStatus
import net.sentinelsafeguard.app.data.api.taxPack
import net.sentinelsafeguard.app.ui.Phase4Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Phase 4 (hunt8) contract shapes: every Phase 4 endpoint driven
 * through the app's API methods against REAL bodies captured from
 * the Phase 4 backend (see [RealBodiesPhase4]; Addenda 16/16a).
 * Refusal captures pin the money-honesty semantics in the server's
 * own words: Family never charged, overpay refused, payments
 * disabled while Stripe is flags-OFF, tax-pack flag gate.
 *
 * Two routes have no rehearsal capture — POST money-desk
 * purchase-drafts/{id}/state and pay-board/{id}/state — and are
 * pinned by request path/body only (their handlers' state
 * vocabularies are pinned from the deployed server source:
 * ordered_offline|dismissed and the pay board's manualStates).
 */
class ContractPhase4Test {

    private class QueueTransport(vararg val steps: Pair<Int, String>) : HttpTransport {
        val requests = mutableListOf<HttpRequest>()
        private var index = 0
        override suspend fun execute(request: HttpRequest): HttpResponse {
            requests += request
            val step = steps[minOf(index, steps.size - 1)]
            index++
            return HttpResponse(step.first, step.second)
        }

        override suspend fun download(
            request: HttpRequest,
            sink: (ByteArray) -> Unit,
        ): HttpResponse {
            requests += request
            val step = steps[minOf(index, steps.size - 1)]
            index++
            sink(step.second.toByteArray())
            return HttpResponse(step.first, "")
        }
    }

    private fun api(transport: HttpTransport): SentinelApi =
        SentinelApi(ApiConfig("https://command.example.test", "", ""), transport)

    private fun step(name: String): Pair<Int, String> =
        RealBodiesPhase4.status(name) to RealBodiesPhase4.body(name)

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.content

    private fun JsonObject.int(key: String): Int? =
        (this[key] as? JsonPrimitive)?.content?.toIntOrNull()

    private fun JsonObject.long(key: String): Long? =
        (this[key] as? JsonPrimitive)?.content?.toLongOrNull()

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.arr(key: String): List<JsonObject> =
        (this[key] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

    private suspend fun expectRefusal(
        status: Int,
        messagePart: String,
        block: suspend () -> Unit,
    ) {
        try {
            block()
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(status, e.status)
            assertTrue("message was: ${e.message}", e.message.contains(messagePart))
        }
    }

    // ---- Invoices: list / stats ----

    @Test
    fun invoicesListParsesRealRowsAndStats() = runBlocking {
        val transport = QueueTransport(step("invoices_list"))
        val out = api(transport).invoices()
        assertEquals("GET", transport.requests[0].method)
        assertTrue(transport.requests[0].path.startsWith("/api/app/invoices"))
        val rows = out.arr("invoices")
        assertEquals(1, rows.size)
        assertEquals("INV-0001", rows[0].str("invoiceNumber"))
        assertEquals("Capture Client LLC", rows[0].str("clientName"))
        assertEquals(55000L, rows[0].long("totalCents"))
        val stats = out.obj("stats")!!
        assertEquals(1, stats.int("draftCount"))
        assertEquals(55000L, stats.long("draftCents"))
    }

    @Test
    fun invoicesListFiltersRideQueryParams() = runBlocking {
        val transport = QueueTransport(step("invoices_list_overdue_after_send"))
        val out = api(transport).invoices(status = "Overdue", sort = "due")
        assertEquals(0, out.int("total"))
        val path = transport.requests[0].path
        assertTrue(path.contains("status=Overdue"))
        assertTrue(path.contains("sort=due"))
    }

    @Test
    fun invoicesRefusalsPinRoleGates() = runBlocking {
        expectRefusal(403, "Not authorized.") {
            api(QueueTransport(step("invoices_staff_refusal"))).invoices()
        }
        expectRefusal(403, "Not authorized.") {
            api(QueueTransport(step("invoices_accountant_refusal"))).invoices()
        }
        expectRefusal(401, "Sign-in required.") {
            api(QueueTransport(step("unauth_invoices"))).invoices()
        }
    }

    // ---- Invoices: create / edit / status / detail / delete ----

    @Test
    fun invoiceCreateRidesWebShape() = runBlocking {
        val transport = QueueTransport(step("invoice_create"))
        val out = api(transport).createInvoice(
            buildJsonObject {
                put("clientId", 1); put("issueDate", "2026-11-01")
                put("dueDate", "2026-10-01"); put("recipientEmail", "billing@example.invalid")
                put("notes", "Capture invoice"); put("taxRatePercent", 10)
                put(
                    "items",
                    buildJsonArray {
                        add(buildJsonObject {
                            put("description", "Security Assessment")
                            put("quantity", 1); put("unitRateCents", 50000)
                        })
                    },
                )
            },
        )
        assertEquals("POST /api/app/invoices", "${transport.requests[0].method} ${transport.requests[0].path}")
        assertTrue(transport.requests[0].body!!.contains("\"invoice\""))
        assertEquals(1, out.int("id"))
        assertEquals(55000L, out.obj("invoice")!!.long("totalCents"))
        assertEquals(5000L, out.obj("invoice")!!.long("taxCents"))
    }

    @Test
    fun invoiceCreateFamilyRefusalSurfacesServerWords() = runBlocking {
        val api = api(QueueTransport(step("invoice_create_family_refusal")))
        expectRefusal(403, "Family accounts are never charged") {
            api.createInvoice(buildJsonObject { put("clientId", 2) })
        }
    }

    @Test
    fun invoicePatchStatusDetailDelete() = runBlocking {
        val transport = QueueTransport(
            step("invoice_patch"), step("invoice_status_sent"),
            step("invoice_detail"), step("invoice_delete"),
        )
        val api = api(transport)
        val patched = api.editInvoice(1, buildJsonObject { put("notes", "Edited") })
        assertEquals("Edited", patched.obj("invoice")!!.str("notes"))
        val status = api.setInvoiceStatus(1, "Sent")
        assertEquals("Sent", status.str("status"))
        val detail = api.invoiceDetail(1)
        assertEquals("Sent", detail.obj("invoice")!!.str("status"))
        assertEquals(true, (detail.obj("invoice")!!["overdue"] as? JsonPrimitive)?.content?.toBoolean())
        assertNotNull(detail.obj("payments"))
        assertEquals("Invoice deleted.", api.deleteInvoice(1).str("message"))
        assertEquals(
            listOf(
                "PATCH /api/app/invoices/1",
                "POST /api/app/invoices/1/status",
                "GET /api/app/invoices/1",
                "DELETE /api/app/invoices/1",
            ),
            transport.requests.map { "${it.method} ${it.path}" },
        )
    }

    @Test
    fun invoiceDocumentCarriesHtml() = runBlocking {
        val out = api(QueueTransport(step("invoice_document"))).invoiceDocument(1)
        assertEquals("INV-0001", out.str("invoiceNumber"))
        assertTrue(out.str("html")!!.contains("INVOICE INV-0001"))
    }

    // ---- Payments / plan / reminders ----

    @Test
    fun invoicePaymentsLifecycleAndOverpayRefusal() = runBlocking {
        val transport = QueueTransport(
            step("invoice_payments_empty"), step("invoice_payment_record_partial"),
            step("invoice_payment_overpay_refusal"), step("invoice_payment_final"),
        )
        val api = api(transport)
        val empty = api.invoicePayments(1)
        assertEquals(55000L, empty.long("balanceDueCents"))
        assertEquals(0, empty.arr("payments").size)
        val partial = api.recordInvoicePayment(1, 5000, "check", "2026-11-02")
        assertEquals(50000L, partial.long("balanceDueCents"))
        expectRefusal(403, "exceeds the balance due") {
            api.recordInvoicePayment(1, 99999999, "check", "")
        }
        val final = api.recordInvoicePayment(1, 50000, "check", "")
        assertEquals("Paid", final.str("status"))
        assertEquals(0L, final.long("balanceDueCents"))
    }

    @Test
    fun paymentPlanOverdueAndReminderDrafts() = runBlocking {
        val transport = QueueTransport(
            step("invoice_plan_create"), step("invoice_plan_get"),
            step("overdue_installments"), step("reminder_generate"), step("reminder_drafts"),
        )
        val api = api(transport)
        val created = api.createInvoicePaymentPlan(
            1,
            buildJsonArray {
                add(buildJsonObject { put("amountCents", 25000); put("dueDate", "2026-10-15") })
                add(buildJsonObject { put("amountCents", 25000); put("dueDate", "2026-12-15") })
            },
            "",
        )
        assertEquals("Payment plan created.", created.str("message"))
        val plan = api.invoicePaymentPlan(1).obj("plan")!!
        assertEquals(2, plan.arr("installments").size)
        assertEquals("overdue", plan.arr("installments")[0].str("status"))
        val overdue = api.overdueInstallments()
        assertEquals(1, overdue.int("count"))
        assertEquals(20000L, overdue.arr("overdue")[0].long("remainingCents"))
        val generated = api.generateReminderDraft(1)
        assertTrue(generated.str("message")!!.contains("nothing was sent"))
        val drafts = api.reminderDrafts()
        assertEquals(1, drafts.arr("drafts").size)
        assertTrue(drafts.arr("drafts")[0].str("subject")!!.contains("INV-0001"))
    }

    @Test
    fun paymentLinkRefusalIsWebsVerbatimDisabledMessage() = runBlocking {
        val api = api(QueueTransport(step("invoice_payment_link_disabled_refusal")))
        expectRefusal(403, Phase4Logic.PAYMENTS_DISABLED_MESSAGE) {
            api.createInvoicePaymentLink(1)
        }
        val fixture = RealBodiesPhase4.bodyJson("invoice_payment_link_disabled_refusal")
        assertEquals(Phase4Logic.PAYMENTS_DISABLED_MESSAGE, fixture.str("message"))
    }

    @Test
    fun completePaymentRefusalPinsSessionLookup() = runBlocking {
        val api = api(QueueTransport(step("invoice_complete_payment_refusal")))
        expectRefusal(403, "No payment link matches that Stripe session.") {
            api.completeInvoicePayment("cs_test_capture")
        }
    }

    // ---- Quotes ----

    @Test
    fun quotesLifecycleAndApprovalConversion() = runBlocking {
        val transport = QueueTransport(
            step("quote_create"), step("quotes_list"), step("quote_detail"),
            step("quote_patch"), step("quote_status_sent"),
            step("quote_status_approved_conversion"),
        )
        val api = api(transport)
        val created = api.createQuote(
            buildJsonObject {
                put("clientId", 1); put("title", "Capture Quote")
                put("convertTo", "ticket"); put("taxRatePercent", 0)
            },
        )
        assertEquals("QT-0001", created.obj("quote")!!.str("quoteNumber"))
        assertEquals(1, api.quotes().arr("quotes").size)
        assertEquals("draft", api.quoteDetail(1).obj("quote")!!.str("status"))
        val patched = api.editQuote(1, buildJsonObject { put("title", "Capture Quote v2") })
        assertEquals("Capture Quote v2", patched.obj("quote")!!.str("title"))
        assertEquals("sent", api.setQuoteStatus(1, "sent").str("status"))
        val approved = api.setQuoteStatus(1, "approved")
        assertEquals("approved", approved.str("status"))
        val conversion = approved.obj("conversion")!!
        assertEquals(2, conversion.int("ticketId"))
        assertEquals(2, conversion.int("invoiceId"))
        assertEquals(
            "POST /api/app/quotes/1/status",
            "${transport.requests.last().method} ${transport.requests.last().path}",
        )
    }

    // ---- Price Book ----

    @Test
    fun priceBookCrudRidesRealRoutes() = runBlocking {
        val transport = QueueTransport(
            step("price_book_save"), step("price_book_list"),
            step("price_book_save"), step("price_book_delete"),
        )
        val api = api(transport)
        val saved = api.savePriceItem(
            buildJsonObject {
                put("name", "Security Assessment"); put("description", "Full assessment")
                put("rateCents", 50000); put("unit", "flat"); put("active", true)
            },
            null,
        )
        assertEquals(1, saved.int("id"))
        val list = api.priceBook()
        assertEquals(50000L, list.arr("items")[0].long("rateCents"))
        assertNotNull(api.savePriceItem(buildJsonObject { put("name", "Security Assessment") }, 1))
        assertEquals("Price item deleted.", api.deletePriceItem(1).str("message"))
        assertEquals(
            listOf(
                "POST /api/app/price-book",
                "GET /api/app/price-book",
                "PATCH /api/app/price-book/1",
                "DELETE /api/app/price-book/1",
            ),
            transport.requests.map { "${it.method} ${it.path}" },
        )
    }

    // ---- Money Desk ----

    @Test
    fun moneyBuilderAndBuildQuote() = runBlocking {
        val transport = QueueTransport(step("money_builder"), step("money_build_quote"))
        val api = api(transport)
        val builder = api.moneyBuilder()
        assertEquals(1, builder.arr("items").size)
        assertEquals("Capture Client LLC", builder.arr("clients")[0].str("legalName"))
        val built = api.moneyBuildQuote(
            1,
            buildJsonArray {
                add(buildJsonObject { put("priceItemId", 1); put("quantity", 2) })
            },
        )
        assertTrue(built.str("message")!!.contains("saved as a draft"))
        assertEquals("QT-0002", built.obj("quote")!!.str("quoteNumber"))
        assertEquals(
            "POST /api/app/money-desk/build-quote",
            "${transport.requests[1].method} ${transport.requests[1].path}",
        )
    }

    @Test
    fun moneyUnbilledAndConvert() = runBlocking {
        val transport = QueueTransport(step("money_unbilled"), step("money_convert_unbilled"))
        val api = api(transport)
        val unbilled = api.moneyUnbilled(1)
        assertEquals(12000L, unbilled.long("totalCents"))
        assertEquals(3600L, unbilled.long("timeSeconds"))
        assertTrue(transport.requests[0].path.contains("clientId=1"))
        val converted = api.moneyConvertUnbilled(1)
        assertEquals(3, converted.int("invoiceId"))
        assertEquals("INV-0003", converted.obj("invoice")!!.str("invoiceNumber"))
    }

    @Test
    fun moneyScanPurchaseDraftsAndPayBoard() = runBlocking {
        val transport = QueueTransport(
            step("money_scan_low_stock"), step("money_purchase_drafts"), step("money_pay_board"),
        )
        val api = api(transport)
        val scan = api.moneyScanLowStock()
        assertTrue(scan.str("message")!!.contains("nothing was ordered"))
        assertEquals(1, scan.int("newDrafts"))
        val drafts = api.moneyPurchaseDrafts()
        assertEquals("Cap Widget", drafts.arr("drafts")[0].str("partName"))
        val board = api.moneyPayBoard()
        assertEquals(3, board.arr("invoices").size)
        assertTrue(
            (board["manualStates"] as JsonArray).map { (it as JsonPrimitive).content }
                .contains("paid_manual"),
        )
    }

    @Test
    fun moneyStateRoutesAreShapePinned() = runBlocking {
        // No rehearsal capture exists for the two state writes;
        // pin method/path/body only (see class doc).
        val transport = QueueTransport(
            200 to """{"ok":true,"message":"Purchase draft updated."}""",
            200 to """{"ok":true,"message":"Payment board state updated."}""",
        )
        val api = api(transport)
        assertNotNull(api.moneySetPurchaseDraftState(1, "dismissed"))
        assertNotNull(api.moneySetPayState(1, "awaiting", ""))
        assertEquals(
            listOf(
                "POST /api/app/money-desk/purchase-drafts/1/state",
                "POST /api/app/money-desk/pay-board/1/state",
            ),
            transport.requests.map { "${it.method} ${it.path}" },
        )
        assertTrue(transport.requests[0].body!!.contains("\"dismissed\""))
        assertTrue(transport.requests[1].body!!.contains("\"awaiting\""))
    }

    @Test
    fun moneyFollowUpsAgingAndAggregate() = runBlocking {
        val transport = QueueTransport(
            step("money_quote_followups_scan"), step("money_quote_followups"),
            step("money_aging_scan"), step("money_aging_drafts"), step("money_desk_aggregate"),
        )
        val api = api(transport)
        val fuScan = api.moneyQuoteFollowUpsScan()
        assertTrue(fuScan.str("message")!!.contains("drafts only, nothing was sent"))
        assertEquals(0, api.moneyQuoteFollowUps().arr("followUps").size)
        val agingScan = api.moneyAgingScan()
        assertTrue(agingScan.str("message")!!.contains("drafts only, nothing was sent"))
        assertEquals(0, api.moneyAgingDrafts().arr("drafts").size)
        val desk = api.moneyDesk()
        assertNotNull(desk.obj("builder"))
        assertNotNull(desk.obj("payBoard"))
        assertEquals(1, desk.arr("purchaseDrafts").size)
        assertEquals(0, desk.arr("agingDrafts").size)
    }

    // ---- Tax & Profit ----

    @Test
    fun taxPackOwnerSummaryFilesAndMarkers() = runBlocking {
        val transport = QueueTransport(step("tax_pack_owner"))
        val out = api(transport).taxPack(2026)
        assertTrue(transport.requests[0].path.contains("year=2026"))
        val summary = out.obj("summary")!!
        assertEquals(55000L, summary.long("paidTotalCents"))
        assertEquals(3, summary.int("invoiceCount"))
        assertEquals("sentinel-tax-pack-2026.zip", out.str("filename"))
        val files = out.obj("files")!!
        assertTrue(files.str("invoicesCsv")!!.contains("INV-0001"))
        assertTrue(files.str("paymentsCsv")!!.contains("check"))
        // Binary payloads are length+sha markers in the fixtures.
        val pdf = out.obj("pdf")!!.obj("pdfBase64")!!
        assertEquals(3428, pdf.int("length"))
    }

    @Test
    fun taxPackAccountantReadsAndStaffIsRefused() = runBlocking {
        val acct = api(QueueTransport(step("tax_pack_accountant"))).taxPack(2026)
        assertEquals(2026, acct.int("year"))
        expectRefusal(403, "Not authorized.") {
            api(QueueTransport(step("tax_pack_staff_refusal"))).taxPack(2026)
        }
    }

    // ---- Phase4Logic pins ----

    @Test
    fun moneyParsingAndPreviewTotals() {
        assertEquals(50000L, Phase4Logic.dollarsToCents("500"))
        assertEquals(50000L, Phase4Logic.dollarsToCents("$500.00"))
        assertEquals(123456L, Phase4Logic.dollarsToCents("1,234.56"))
        assertEquals(null, Phase4Logic.dollarsToCents("abc"))
        assertEquals("500.00", Phase4Logic.centsToDollarsText(50000))
        val (subtotal, tax, total) = Phase4Logic.previewTotals(
            listOf(Triple("Security Assessment", 1.0, 50000L)), 10.0,
        )
        assertEquals(50000L, subtotal); assertEquals(5000L, tax); assertEquals(55000L, total)
    }

    @Test
    fun friendsMathMatchesBackendProof() {
        // Addendum 16: 10000c subtotal, 10% tax -> discount 2000,
        // tax 800, total 8800 (tax on the discounted amount).
        val (discount, tax, total) = Phase4Logic.friendsTotals(10000, 10.0)
        assertEquals(2000L, discount); assertEquals(800L, tax); assertEquals(8800L, total)
    }

    @Test
    fun invoiceSortAndVocabPins() {
        assertEquals("id_desc", Phase4Logic.invoiceSortParam("Newest"))
        assertEquals("due", Phase4Logic.invoiceSortParam("Due soonest"))
        assertEquals("amount", Phase4Logic.invoiceSortParam("Amount high → low"))
        assertEquals("status", Phase4Logic.invoiceSortParam("Status order"))
        assertEquals(listOf("Draft", "Sent", "Paid", "Void"), Phase4Logic.INVOICE_STATUSES)
        assertTrue(Phase4Logic.INVOICE_FILTERS.contains("Overdue"))
        assertEquals(listOf("ticket", "case", "invoice"), Phase4Logic.QUOTE_CONVERT_TO)
        assertEquals(listOf("ordered_offline", "dismissed"), Phase4Logic.PURCHASE_DRAFT_STATES)
    }
}

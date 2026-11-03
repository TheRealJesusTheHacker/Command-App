package net.sentinelsafeguard.app.ui

import kotlinx.serialization.json.JsonObject

/**
 * Pure helpers for the Phase 4 (hunt8) Money surfaces — kept free
 * of Compose so the unit suite can pin them against the real
 * captured payloads (ContractPhase4Test). Vocabularies are web
 * Command's exact sets (mirror inventory § Money rows).
 */
object Phase4Logic {

    val INVOICE_STATUSES: List<String> = listOf("Draft", "Sent", "Paid", "Void")

    /** Directory status chips: All + the four statuses + the
     *  derived Overdue chip web adds. */
    val INVOICE_FILTERS: List<String> = listOf("Draft", "Sent", "Paid", "Void", "Overdue")

    val INVOICE_SORTS: List<String> = listOf(
        "Newest", "Due soonest", "Amount high → low", "Status order",
    )

    fun invoiceSortParam(label: String): String = when (label) {
        "Due soonest" -> "due"
        "Amount high → low" -> "amount"
        "Status order" -> "status"
        else -> "id_desc"
    }

    val QUOTE_STATUSES: List<String> = listOf("draft", "sent", "approved", "declined", "expired")

    val QUOTE_CONVERT_TO: List<String> = listOf("ticket", "case", "invoice")

    val PAYMENT_METHODS: List<String> = listOf("check", "cash", "card", "bank", "other")

    val PURCHASE_DRAFT_STATES: List<String> = listOf("ordered_offline", "dismissed")

    /** Parse a dollars text field ("500", "500.00", "$1,234.56")
     *  into integer cents; null when the text is not a number. */
    fun dollarsToCents(text: String): Long? {
        val cleaned = text.trim().removePrefix("$").replace(",", "")
        if (cleaned.isBlank()) return null
        val value = cleaned.toDoubleOrNull() ?: return null
        if (value < 0) return null
        return Math.round(value * 100)
    }

    fun centsToDollarsText(cents: Long): String {
        val dollars = cents / 100
        val rem = Math.abs(cents % 100)
        return "$dollars." + rem.toString().padStart(2, '0')
    }

    /** Line-item preview totals, the form's live preview: plain
     *  subtotal + tax on the subtotal. The backend recomputes the
     *  authoritative totals (Friends 20% discount first, tax on
     *  the discounted amount) and the profile renders those. */
    fun previewTotals(
        lines: List<Triple<String, Double, Long>>,
        taxRatePercent: Double,
    ): Triple<Long, Long, Long> {
        var subtotal = 0L
        lines.forEach { (_, qty, rateCents) -> subtotal += Math.round(qty * rateCents) }
        val tax = Math.round(subtotal * taxRatePercent / 100.0)
        return Triple(subtotal, tax, subtotal + tax)
    }

    /** Friends math as the backend carries it (Addendum 16 proof):
     *  20% discount off the subtotal, tax on the discounted amount. */
    fun friendsTotals(subtotalCents: Long, taxRatePercent: Double): Triple<Long, Long, Long> {
        val discount = Math.round(subtotalCents * 0.20)
        val taxable = subtotalCents - discount
        val tax = Math.round(taxable * taxRatePercent / 100.0)
        return Triple(discount, tax, taxable + tax)
    }

    fun isOverdueInvoice(invoice: JsonObject): Boolean =
        Phase3Logic.bool(invoice, "overdue") == true

    /** The payment-link honest state: web refuses with its own
     *  words while payments are not enabled; the fixture pins the
     *  exact sentence the app surfaces. */
    const val PAYMENTS_DISABLED_MESSAGE: String =
        "Online payments are not enabled — set PAYMENTS_ENABLED=1 in the payments config to turn them on."

    fun taxYearOptions(currentYear: Int): List<Int> =
        listOf(currentYear, currentYear - 1, currentYear - 2)

    /** Quarter rows of a tax-pack summary, in web order. */
    fun quarterKeys(): List<String> = listOf("1", "2", "3", "4")
}

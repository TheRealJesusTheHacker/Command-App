package net.sentinelsafeguard.app.ui

import kotlinx.serialization.json.JsonObject

/**
 * Pure helpers for the Phase 5 (hunt9) Team & Assets surfaces —
 * kept free of Compose so the unit suite can pin them against
 * the real captured payloads (ContractPhase5Test). Vocabularies
 * are web Command's exact sets (mirror inventory §2.1 Team &
 * Assets rows; the deployed Phase 5 wrappers' filter/sort maps).
 */
object Phase5Logic {

    /** Employees directory role chips: label → backend `role`
     *  parameter (the wrapper's own filter vocabulary). */
    val EMPLOYEE_ROLE_FILTERS: List<Pair<String, String>> = listOf(
        "All" to "All",
        "Owners" to "Owners",
        "Staff" to "Staff",
        "Client access" to "Client",
        "Demo access" to "Demo",
        "Inactive" to "Inactive",
    )

    /** Web empIsTeam: team = every staff-side role. */
    private val TEAM_EXCLUDE = setOf("Client", "Family", "Tourist")

    fun isTeam(user: JsonObject): Boolean =
        Phase3Logic.str(user, "role") !in TEAM_EXCLUDE

    /** Directory sections in web order: Team / Client access /
     *  Demo access. */
    fun employeeSection(user: JsonObject): String = when (Phase3Logic.str(user, "role")) {
        "Tourist" -> "Demo access"
        "Client", "Family" -> "Client access"
        else -> "Team"
    }

    val ASSET_STATUSES: List<String> =
        listOf("In Service", "In Storage", "In Repair", "Retired", "Lost")

    /** Directory sort select: label → backend `sort` parameter. */
    val ASSET_SORTS: List<Pair<String, String>> = listOf(
        "Name A–Z" to "name",
        "Newest" to "newest",
        "Status order" to "status",
        "Review due soonest" to "review",
        "Warranty ending soonest" to "warranty",
    )

    /** Alert filter chips: label → backend `alert` parameter. */
    val ASSET_ALERT_FILTERS: List<Pair<String, String>> = listOf(
        "All dates" to "All",
        "Needs attention" to "alerts",
        "Review overdue" to "review-overdue",
        "Warranty expired" to "warranty-expired",
        "Warranty expiring (30 days)" to "warranty-expiring",
    )

    val ASSET_SCOPES: List<Pair<String, String>> = listOf(
        "Sentinel Equipment" to "Sentinel",
        "Client asset" to "Client",
    )

    /** Flag chips for one asset row's `flags` object, in web
     *  order (review, then warranty states). */
    fun flagLabels(flags: JsonObject?): List<String> {
        if (flags == null) return emptyList()
        val out = mutableListOf<String>()
        if (Phase3Logic.bool(flags, "reviewOverdue") == true) out += "Review overdue"
        if (Phase3Logic.bool(flags, "warrantyExpired") == true) out += "Warranty expired"
        if (Phase3Logic.bool(flags, "warrantyExpiring") == true) out += "Warranty expiring"
        return out
    }

    fun renewalFieldLabel(field: String): String = when (field) {
        "review" -> "Review"
        "warranty" -> "Warranty"
        "support" -> "Support"
        "warrantyExpires" -> "Warranty"
        "supportExpires" -> "Support"
        else -> field
    }

    /** One renewal entry's chip text, e.g. "Warranty expired
     *  2026-11-01" / "Review due 2027-02-01". */
    fun renewalLabel(renewal: JsonObject): String {
        val field = renewalFieldLabel(Phase3Logic.str(renewal, "field"))
        val date = Phase3Logic.str(renewal, "date")
        val state = Phase3Logic.str(renewal, "state")
        val verb = if (state == "expired") "expired" else "due"
        return listOf(field, verb, date).filter { it.isNotBlank() }.joinToString(" ")
    }

    /** Form-files rules (web's own, enforced server-side too):
     *  PDF only, 25 MB cap. */
    const val FORM_FILE_MAX_BYTES: Int = 25 * 1024 * 1024

    fun isPdfUpload(fileName: String, bytes: ByteArray): Boolean =
        fileName.lowercase().endsWith(".pdf") &&
            bytes.size >= 5 &&
            bytes.copyOfRange(0, 5).toString(Charsets.US_ASCII) == "%PDF-"

    /** The plain pre-check message the Forms view shows before
     *  sending, mirroring web's rules; null when the file passes.
     *  The server remains the authority — its refusal wording is
     *  what surfaces when it disagrees. */
    fun uploadPrecheck(fileName: String, bytes: ByteArray): String? = when {
        bytes.isEmpty() -> "That file is empty."
        bytes.size > FORM_FILE_MAX_BYTES -> "That PDF is larger than 25 MB."
        !isPdfUpload(fileName, bytes) -> "Form documents must be PDF files."
        else -> null
    }

    fun formatBytes(size: Long): String = when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> String.format("%.1f MB", size / (1024.0 * 1024.0))
    }
}

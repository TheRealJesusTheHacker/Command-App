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
     *  parameter (the wrapper's own filter vocabulary). The
     *  "Demo access" chip retired in 1.5.4 with the Tourist
     *  role itself — production cannot mint one, so there is
     *  no demo section of the directory to filter to. */
    val EMPLOYEE_ROLE_FILTERS: List<Pair<String, String>> = listOf(
        "All" to "All",
        "Owners" to "Owners",
        "Staff" to "Staff",
        "Client access" to "Client",
        "Inactive" to "Inactive",
    )

    /** The roles the employee editor may ASSIGN (1.5.4): the
     *  production set. "Family" and "Tourist" are retired —
     *  the server refuses to mint either, so the picker no
     *  longer offers them. (A roles LIST from the server may
     *  still carry one inert legacy "Family" row; wherever a
     *  surface renders the server's list it renders it as
     *  sent — this constant governs only what the app itself
     *  offers for assignment.) */
    val ASSIGNABLE_ROLES: List<String> = listOf("OWNER", "STAFF", "Admin", "Client")

    /**
     * Scan-to-asset (1.4.0): what the operator typed, pasted, or
     * scanned off a printed label → the bare code the resolver
     * takes. Label payloads are
     * https://sentinelsafeguard.net/a/<SCA-code>; a bare code
     * passes through untouched. (The `#asset=<id>` label form is
     * a web-route shape the app resolver doesn't take — it is
     * returned as-is and the server answers honestly.)
     */
    fun assetCodeFromInput(raw: String): String {
        val trimmed = raw.trim()
        return if ("/a/" in trimmed) {
            trimmed.trimEnd('/').substringAfterLast("/a/").substringBefore('?')
        } else {
            trimmed
        }
    }

    /**
     * Team membership for one directory row (1.5.4): the same
     * decision the shell makes, asked of the row's own role +
     * structureRole — [MirrorNav.shellKind] is the single
     * classifier, so a row the shell would land in the portal
     * (client structure, a retired role string, anything
     * unrecognized) is never counted as team here either.
     * Web's empIsTeam said "every staff-side role"; the
     * role-structure window made the two structures explicit,
     * and this follows them.
     */
    fun isTeam(user: JsonObject): Boolean =
        MirrorNav.shellKind(
            Phase3Logic.str(user, "role"),
            portalFlag = false,
            structureRole = Phase3Logic.strOrNull(user, "structureRole"),
        ) == MirrorNav.ShellKind.COMMAND

    /** Directory sections: Team / Client access. (The "Demo
     *  access" section retired with the Tourist role.) */
    fun employeeSection(user: JsonObject): String =
        if (isTeam(user)) "Team" else "Client access"

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

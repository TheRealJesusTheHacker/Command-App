package net.sentinelsafeguard.app.ui

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Pure helpers for the Phase 8A (hunt12) Firm surfaces — kept
 * free of Compose so the unit suite can pin them against the
 * real captured payloads (ContractPhase8Test). Vocabularies are
 * web Command's own where the payloads carry them (task states,
 * availability statuses, desk categories and control set all
 * ride the payloads); the rest are web's filter/state sets from
 * the production views (mirror inventory §2.1/§2.3).
 */
object Phase8Logic {

    // ---- Activity ----

    /** Web's Activity category filters (class keys → labels),
     *  in web's chip order. Counts ride the payload's
     *  classCounts under these same keys ("all" included). */
    val CLASS_FILTERS: List<Pair<String, String>> = listOf(
        "all" to "All",
        "auth" to "Sign-ins & auth",
        "changes" to "Changes",
        "files" to "Files",
        "views" to "Views",
        "admin" to "Admin",
    )

    fun classLabel(key: String): String =
        CLASS_FILTERS.firstOrNull { it.first == key }?.second ?: key

    /** The days filter choices (blank = the handler's default
     *  window; the value rides as the `days` parameter). */
    val DAYS_FILTERS: List<Pair<String, String>> = listOf(
        "" to "Default window",
        "1" to "Last 24 hours",
        "7" to "Last 7 days",
        "30" to "Last 30 days",
        "90" to "Last 90 days",
    )

    fun daysLabel(value: String): String =
        DAYS_FILTERS.firstOrNull { it.first == value }?.second ?: DAYS_FILTERS[0].second

    fun daysValue(label: String): String =
        DAYS_FILTERS.firstOrNull { it.second == label }?.first ?: ""

    /** One audit row's headline: web renders the handler's
     *  label, falling back to the raw action when unlabeled. */
    fun activityHeadline(row: JsonObject): String =
        Phase3Logic.str(row, "label").ifBlank { Phase3Logic.str(row, "action") }

    /** One audit row's meta line: actor · IP · time, as web's
     *  feed composes them. */
    fun activityMeta(row: JsonObject): String {
        val parts = mutableListOf<String>()
        Phase3Logic.str(row, "actorName").takeIf { it.isNotBlank() }?.let { parts += it }
        Phase3Logic.str(row, "ip").takeIf { it.isNotBlank() }?.let { parts += it }
        val at = Phase3Logic.long(row, "createdAt")
        if (at != null && at > 0) parts += formatEpochMs(at)
        return parts.joinToString(" · ")
    }

    // ---- Firm Ops ----

    /** Owner-task states fall back to web's set when a payload
     *  somehow omits its own `states` list. */
    val TASK_STATES: List<String> = listOf("open", "done", "dropped")

    fun taskStates(payload: JsonObject?): List<String> {
        val fromPayload = payload?.let {
            (it["states"] as? kotlinx.serialization.json.JsonArray)
                ?.mapNotNull { el -> (el as? JsonPrimitive)?.content }
                ?.filter { s -> s.isNotBlank() }
        }
        return fromPayload?.takeIf { list -> list.isNotEmpty() } ?: TASK_STATES
    }

    fun stateLabel(state: String): String =
        state.replaceFirstChar { it.uppercase() }

    /** Availability statuses fall back to web's set likewise. */
    val AVAIL_STATUSES: List<String> = listOf("available", "unavailable", "tentative")

    fun availStatuses(payload: JsonObject?): List<String> {
        val fromPayload = payload?.let {
            (it["statuses"] as? kotlinx.serialization.json.JsonArray)
                ?.mapNotNull { el -> (el as? JsonPrimitive)?.content }
                ?.filter { s -> s.isNotBlank() }
        }
        return fromPayload?.takeIf { list -> list.isNotEmpty() } ?: AVAIL_STATUSES
    }

    /** A job-costing line's margin text: unrated jobs show no
     *  margin — no rate is ever invented (the payload's own
     *  rule, carried in its note). */
    fun marginLabel(costing: JsonObject): String {
        val margin = Phase3Logic.long(costing, "marginCents") ?: return ""
        val pct = Phase3Logic.dbl(costing, "marginPercent")
        return if (pct != null) {
            "Margin ${MirrorVocab.formatCents(margin)} ($pct%)"
        } else {
            "Margin ${MirrorVocab.formatCents(margin)}"
        }
    }

    fun costingMeta(costing: JsonObject): String {
        val parts = mutableListOf<String>()
        Phase3Logic.str(costing, "clientName").takeIf { it.isNotBlank() }?.let { parts += it }
        Phase3Logic.str(costing, "status").takeIf { it.isNotBlank() }?.let { parts += it }
        val hours = Phase3Logic.dbl(costing, "timeHours")
        if (hours != null) parts += "${hours}h tracked"
        val cost = Phase3Logic.long(costing, "totalCostCents")
        if (cost != null) parts += "Cost ${MirrorVocab.formatCents(cost)}"
        val invoiced = Phase3Logic.long(costing, "invoicedCents")
        if (invoiced != null) parts += "Invoiced ${MirrorVocab.formatCents(invoiced)}"
        return parts.joinToString(" · ")
    }

    // ---- Verified Desk ----

    /** Control verdicts (the checklist's own vocabulary). */
    val CONTROL_STATUSES: List<Pair<String, String>> = listOf(
        "pass" to "Pass",
        "partial" to "Partial",
        "fail" to "Fail",
        "na" to "N/A",
    )

    fun controlStatusLabel(value: String): String =
        CONTROL_STATUSES.firstOrNull { it.first == value }?.second ?: value

    fun controlStatusValue(label: String): String =
        CONTROL_STATUSES.firstOrNull { it.second == label }?.first ?: "na"

    /** Assessment workflow states → labels (payload values). */
    fun assessmentStateLabel(state: String): String = when (state) {
        "in_review" -> "In review"
        else -> state.replaceFirstChar { it.uppercase() }
    }

    /** The legal next states from each workflow state — the
     *  handler refuses anything else in its own words. */
    fun nextStates(state: String): List<String> = when (state) {
        "draft" -> listOf("in_review")
        "in_review" -> listOf("completed")
        else -> emptyList()
    }

    /** The evidence pack's PDF bytes, decoded from the payload's
     *  base64. A truncated descriptor (the fixture form) is NOT
     *  a PDF — this returns null rather than inventing bytes. */
    fun evidencePdfBytes(pack: JsonObject): ByteArray? {
        val raw = (pack["pdfBase64"] as? JsonPrimitive)?.content ?: return null
        if (raw.isBlank()) return null
        return runCatching {
            java.util.Base64.getMimeDecoder().decode(raw)
        }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    /** Score text: the stored score once completed, else the
     *  live score, else web's unrated blank. */
    fun scoreLabel(assessment: JsonObject): String {
        val score = Phase3Logic.dbl(assessment, "score")
            ?: Phase3Logic.dbl(assessment, "liveScore")
        return if (score != null) "Score $score" else "Not scored yet"
    }

    // ---- Portal Settings ----

    /** A secret's metadata line — label/state/dates only. The
     *  value is never in the payload, so it can never be here. */
    fun secretMeta(secret: JsonObject): String {
        val parts = mutableListOf<String>()
        Phase3Logic.str(secret, "state").takeIf { it.isNotBlank() }?.let { parts += it }
        val created = Phase3Logic.long(secret, "createdAt")
        if (created != null && created > 0) parts += "Created " + formatEpochMs(created)
        val expires = Phase3Logic.long(secret, "expiresAt")
        if (expires != null && expires > 0) parts += "Expires " + formatEpochMs(expires)
        val viewed = Phase3Logic.long(secret, "viewedAt")
        if (viewed != null && viewed > 0) parts += "Viewed " + formatEpochMs(viewed)
        return parts.joinToString(" · ")
    }
}

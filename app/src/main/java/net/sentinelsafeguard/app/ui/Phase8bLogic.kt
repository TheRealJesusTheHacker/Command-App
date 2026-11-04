package net.sentinelsafeguard.app.ui

import kotlinx.serialization.json.JsonObject

/**
 * Pure helpers for the Phase 8B (hunt13) surfaces — the Admin
 * console and the 12 nav-injected entries — kept free of Compose
 * so the unit suite can pin them against the real captured
 * payloads (ContractPhase8BTest). Vocabularies are web Command's
 * own where the payloads carry them (DMARC windows, claim
 * statuses, workflow states ride the payloads); the rest are
 * web's sets from the production views (mirror inventory
 * §1.2/§2.3–§2.5).
 */
object Phase8bLogic {

    /** DMARC window chips, web's exact set (b21 section). */
    val DMARC_WINDOWS: List<Pair<Int, String>> = listOf(
        7 to "7 days",
        30 to "30 days",
        90 to "90 days",
        365 to "365 days",
    )

    /** The backup bundle states the status handler reports; the
     *  app renders the handler's own state word, this only maps
     *  the empty/none case to web's plain wording. */
    fun backupStateLabel(state: String): String = when (state) {
        "", "none" -> "No backup bundle"
        else -> state.replaceFirstChar { it.uppercase() }
    }

    /** A generic row's headline from the keys web's own tables
     *  lead with, in priority order. */
    private val TITLE_KEYS = listOf(
        "title", "displayName", "name", "business", "legalName",
        "clientName", "label", "invoiceNumber", "action",
    )

    fun rowTitle(row: JsonObject): String {
        for (key in TITLE_KEYS) {
            val v = Phase3Logic.str(row, key)
            if (v.isNotBlank()) return v
        }
        val id = Phase3Logic.int(row, "id")
        return if (id != null) "#$id" else "—"
    }

    /** A generic row's meta line: state-ish fields web's tables
     *  show beside the headline. */
    fun rowMeta(row: JsonObject): String {
        val parts = mutableListOf<String>()
        for (key in listOf("role", "status", "state", "verdict", "period", "bucket", "kind")) {
            val v = Phase3Logic.str(row, key)
            if (v.isNotBlank()) parts += v
        }
        val created = Phase3Logic.long(row, "createdAt")
        if (created != null && created > 0) parts += formatEpochMs(created)
        return parts.joinToString(" · ")
    }

    /** The shown-once value in a create response, when the
     *  handler returns it as a plain string field (API key /
     *  survey token). Shape-redacted object forms (fixtures)
     *  return null — there is no value to show. The caller keeps
     *  the result in memory only, exactly as web's one-time
     *  display does. */
    fun shownOnceValue(response: JsonObject, vararg keys: String): String? {
        for (key in keys) {
            val v = Phase3Logic.strOrNull(response, key)
            if (!v.isNullOrBlank()) return v
        }
        return null
    }

    /** Recovery-code display lines from a regeneration response:
     *  a plain list of codes when the handler returns one; the
     *  shape-only form (count + lengths) renders as a count line
     *  and never as invented codes. */
    fun recoveryCodeLines(response: JsonObject): List<String> {
        val codes = Phase3Logic.obj(response, "recoveryCodes")
        if (codes != null) {
            val count = Phase3Logic.int(codes, "count")
            return if (count != null) listOf("$count recovery codes regenerated.")
            else emptyList()
        }
        return Phase3Logic.arrRaw(response, "recoveryCodes")
            .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
            .filter { it.isNotBlank() }
    }

    /** Law AI masked-state lines: provider / models / caps only.
     *  The payload carries no key value by construction (the
     *  handler masks it); this helper reads only the public
     *  fields web itself renders. */
    fun lawAiSummary(config: JsonObject): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        Phase3Logic.str(config, "provider").takeIf { it.isNotBlank() }
            ?.let { out += "Provider" to it }
        val configured = Phase3Logic.bool(config, "configured")
        if (configured != null) out += "Configured" to if (configured) "Yes" else "No"
        val cap = Phase3Logic.int(config, "dailyCap")
        if (cap != null) out += "Daily cap" to cap.toString()
        Phase3Logic.str(config, "freeNotice").takeIf { it.isNotBlank() }
            ?.let { out += "Tier" to it }
        return out
    }
}

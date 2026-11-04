package net.sentinelsafeguard.app.ui

import kotlinx.serialization.json.JsonObject

/**
 * Pure helpers for the Phase 7 (hunt11) Knowledge surfaces —
 * kept free of Compose so the unit suite can pin them against
 * the real captured payloads (ContractPhase7Test). Every
 * vocabulary and format here is web Command's own, taken from
 * the production Knowledge view (kbStats / kbSize / kbCard /
 * kbFiltered / editKnowledge / kbCheckFile).
 */
object Phase7Logic {

    /** Web's sort vocabulary (kbSort select), key → label. */
    val SORTS: List<Pair<String, String>> = listOf(
        "title" to "Title A–Z",
        "newest" to "Newest first",
        "pages" to "Most pages",
        "largest" to "Largest first",
    )

    fun sortLabel(key: String): String =
        SORTS.firstOrNull { it.first == key }?.second ?: "Title A–Z"

    fun sortKey(label: String): String =
        SORTS.firstOrNull { it.second == label }?.first ?: "title"

    /** Web's category datalist (KB_CATEGORIES) — suggestions for
     *  the editor's free-text Category field, never a closed set. */
    val CATEGORIES: List<String> = listOf(
        "Codes & Standards", "Law & Regulation", "Compliance",
        "Field Practice", "Reference",
    )

    /** Web's audience select (editKnowledge), value → label. */
    val AUDIENCES: List<Pair<String, String>> = listOf(
        "none" to "Internal only — clients never see it",
        "all" to "All clients — self-help library",
        "specific" to "Specific client(s) — ids below",
    )

    fun audienceLabel(value: String): String =
        AUDIENCES.firstOrNull { it.first == value }?.second
            ?: AUDIENCES[0].second

    fun audienceValue(label: String): String =
        AUDIENCES.firstOrNull { it.second == label }?.first ?: "none"

    /** The entry's stored audience: an entry that is not
     *  client-visible reads "none" whatever its stored value
     *  (web's editor seeds its select the same way). */
    fun entryAudience(entry: JsonObject): String {
        val visible = Phase3Logic.bool(entry, "clientVisible") == true
        val stored = Phase3Logic.str(entry, "audience")
        return if (!visible || stored.isBlank()) "none" else stored
    }

    /** Web kbSize, byte for byte. */
    fun kbSize(n: Long): String = when {
        n < 1024 -> "$n B"
        n < 1024 * 1024 -> (n / 1024.0).let { "%.1f KB".format(it) }
        n < 1024L * 1024 * 1024 -> "%.1f MB".format(n / (1024.0 * 1024))
        else -> "%.2f GB".format(n / (1024.0 * 1024 * 1024))
    }

    /** Web kbStats' Total pages cell: "—" when nothing is ready
     *  to read, the summed pages, with a trailing " +" while
     *  some ready book's page count is still unknown. The app
     *  payload carries the same facts as totalPages +
     *  pagesComplete over the complete catalog. */
    fun totalPagesLabel(stats: JsonObject?): String {
        if (stats == null) return "—"
        val ready = Phase3Logic.int(stats, "readyToRead") ?: 0
        if (ready == 0) return "—"
        val pages = Phase3Logic.int(stats, "totalPages") ?: 0
        val complete = Phase3Logic.bool(stats, "pagesComplete") == true
        return if (complete) pages.toString() else "$pages +"
    }

    /** Web kbCard's meta line for one entry. */
    fun entryMeta(entry: JsonObject): String {
        if (Phase3Logic.bool(entry, "hasFile") != true) {
            return "Catalog entry — no PDF housed yet."
        }
        val parts = mutableListOf<String>()
        val pages = Phase3Logic.int(entry, "pageCount")
        parts += if (pages != null) "$pages pages" else "Pages —"
        parts += kbSize(Phase3Logic.long(entry, "fileSize") ?: 0)
        val at = Phase3Logic.long(entry, "fileUploadedAt")
        if (at != null && at > 0) {
            val by = Phase3Logic.str(entry, "uploadedByName")
            parts += "Added " + formatEpochMs(at) + if (by.isNotBlank()) " by $by" else ""
        }
        return parts.joinToString(" · ")
    }

    /** The card's audience chip, exactly as web words it. */
    fun audienceChip(entry: JsonObject): String =
        if (Phase3Logic.bool(entry, "clientVisible") == true) {
            "Client-visible · ${Phase3Logic.str(entry, "audience")}"
        } else {
            "Internal only"
        }

    fun fileStateChip(entry: JsonObject): String =
        if (Phase3Logic.bool(entry, "hasFile") == true) "Ready to read" else "Awaiting file"

    /** Web kbCheckFile, message for message (the server then
     *  re-validates the bytes themselves — signature, EOF, caps). */
    fun checkFile(name: String?, size: Long): String? = when {
        name.isNullOrBlank() -> "Choose a PDF file."
        !name.lowercase().endsWith(".pdf") -> "Choose a PDF file."
        size <= 0L -> "That file is empty."
        size > 250L * 1024 * 1024 -> "That PDF is larger than 250 MB."
        else -> null
    }

    // ---- Archive ----

    /** The Archive kinds in web's aggregation order. */
    val ARCHIVE_KINDS: List<String> = listOf("client", "case", "ticket", "asset")

    /** Where a restored record's own mirrored view lives. */
    fun archiveRouteFor(kind: String, id: Int): String? = when (kind) {
        "client" -> "client/$id"
        "case" -> "case/$id"
        "ticket" -> "ticket/$id"
        "asset" -> "asset/$id"
        else -> null
    }

    /** A row's secondary line from its full pub row: the fields
     *  web's own module views lead with for that kind. */
    fun archiveRowMeta(row: JsonObject): String {
        val entry = Phase3Logic.obj(row, "entry") ?: return ""
        val parts = mutableListOf<String>()
        when (Phase3Logic.str(row, "kind")) {
            "client" -> {
                Phase3Logic.str(entry, "stage").takeIf { it.isNotBlank() }?.let { parts += it }
                Phase3Logic.str(entry, "contactName").takeIf { it.isNotBlank() }?.let { parts += it }
            }
            "case", "ticket" -> {
                Phase3Logic.str(entry, "status").takeIf { it.isNotBlank() }?.let { parts += it }
                Phase3Logic.str(entry, "clientName").takeIf { it.isNotBlank() }?.let { parts += it }
            }
            "asset" -> {
                Phase3Logic.str(entry, "status").takeIf { it.isNotBlank() }?.let { parts += it }
                Phase3Logic.str(entry, "scope").takeIf { it.isNotBlank() }?.let { parts += it }
            }
        }
        val at = Phase3Logic.long(entry, "archivedAt")
        if (at != null && at > 0) parts += "Archived " + formatEpochMs(at)
        return parts.joinToString(" · ")
    }
}

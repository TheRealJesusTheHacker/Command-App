package net.sentinelsafeguard.app.ui

import kotlinx.serialization.json.JsonObject

/**
 * 1.5.5 — the portal payload's two delivered-sections
 * (DRAFTSTORE-2026-11-09, server census F-06/F-07a): the
 * client's own DELIVERED threat briefs and monthly reports,
 * carried by the same getmyportal payload the portal home
 * already mirrors. Shapes pinned byte-verbatim from the
 * 2026-11-09 staging rehearsal (portal-briefs-rehearsal):
 *
 *  - threatBriefs rows: {id, period, title, body, deliveredAt}
 *    — the portal whitelist shape; source refs, staff ids and
 *    internal state never ride it, and the app renders nothing
 *    beyond these five fields.
 *  - monthlyReports rows: {id, period, body, deliveredAt} —
 *    the body rides inline; the payload carries NO document
 *    or download field, so the section offers no download
 *    control (the brief's rule: open/download strictly per
 *    the fields actually carried).
 *  - Unlinked accounts and work-class-gated contacts get both
 *    keys as EMPTY ARRAYS server-side; the payload shape never
 *    changes. A key ABSENT entirely (a pre-window server)
 *    degrades to the same empty state — the web portal reads
 *    both sections through `|| []` fallbacks and always paints
 *    the section with its empty-state line, so the app mirrors
 *    exactly that: the section renders, empty means the
 *    honest empty line, never a hidden section and never
 *    invented rows.
 */
object PortalBriefs155Logic {

    const val BRIEFS_EMPTY = "No threat briefs delivered yet."
    const val REPORTS_EMPTY = "No monthly reports delivered yet."

    /** Delivered briefs, in payload order (server: id DESC). */
    fun briefs(home: JsonObject): List<JsonObject> =
        Phase3Logic.arr(home, "threatBriefs")

    /** Delivered monthly reports, in payload order. */
    fun reports(home: JsonObject): List<JsonObject> =
        Phase3Logic.arr(home, "monthlyReports")

    /** True when the key rides the payload at all (even as an
     *  empty array). Absence degrades to the same rendering as
     *  empty — this exists so tests can pin the distinction. */
    fun sectionPresent(home: JsonObject, key: String): Boolean =
        home.containsKey(key)

    /** The brief's headline: the payload's own title, else a
     *  period-derived label — never an invented subject. */
    fun briefTitle(row: JsonObject): String {
        val title = Phase3Logic.str(row, "title")
        if (title.isNotBlank()) return title
        val period = Phase3Logic.str(row, "period")
        return if (period.isNotBlank()) "Threat brief — $period" else "Threat brief"
    }

    /** The report's headline: monthly reports carry no title
     *  field, so the label is period-derived only. */
    fun reportTitle(row: JsonObject): String {
        val period = Phase3Logic.str(row, "period")
        return if (period.isNotBlank()) "Monthly report — $period" else "Monthly report"
    }

    /** A brief row's meta line: the period (only when the
     *  headline isn't already the period-derived label) plus
     *  the delivered date — both from the payload, either
     *  omitted when absent. */
    fun briefMeta(row: JsonObject): String? {
        val parts = mutableListOf<String>()
        val period = Phase3Logic.str(row, "period")
        if (period.isNotBlank() && Phase3Logic.str(row, "title").isNotBlank()) {
            parts += period
        }
        deliveredPart(row)?.let { parts += it }
        return parts.joinToString(" · ").ifBlank { null }
    }

    /** A report row's meta line: the delivered date only (the
     *  period already headlines every report row). */
    fun reportMeta(row: JsonObject): String? = deliveredPart(row)

    private fun deliveredPart(row: JsonObject): String? {
        val delivered = Phase3Logic.long(row, "deliveredAt")
        return if (delivered != null && delivered > 0) {
            "Delivered ${formatEpochMs(delivered)}"
        } else {
            null
        }
    }

    /** The read-only body a tap opens; blank when the payload
     *  carries none (the tap then offers nothing). */
    fun body(row: JsonObject): String = Phase3Logic.str(row, "body")
}

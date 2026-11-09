package net.sentinelsafeguard.app.ui

import kotlinx.serialization.json.JsonObject
import net.sentinelsafeguard.app.data.api.ApiException
import java.util.Locale

/**
 * 1.5.4: the pure shape of Command's website analytics, as
 * served by GET /api/app/analytics — the web dashboard's
 * analyticssummary payload byte-for-byte (the server reuses
 * the web handler and its 300 s cached Umami read-model; the
 * 2026-11-09 rehearsal against the staging tree pinned every
 * shape below against the real handler).
 *
 * Honesty rails, same discipline as PushPrefsLogic:
 *  - every rendered number comes from the payload; a payload
 *    whose headline metrics are missing or non-numeric is
 *    [State.Malformed] and the surfaces show an honest failure
 *    — never invented or zero-filled statistics;
 *  - the one exception is a genuinely empty period: the
 *    server answers available with REAL zeros when nothing
 *    was recorded, and [isEmptyPeriod] lets the surfaces
 *    style that as the empty state instead of a dashboard;
 *  - the gate is the server's: 403 means this account may
 *    not see analytics (the dashboard card then renders
 *    NOTHING — no error noise), 401 is the app's standard
 *    session-expiry path and never reaches this logic.
 */
object AnalyticsLogic {

    sealed interface State {
        data class Available(val stats: Stats) : State
        data class Unavailable(val reason: String) : State
        data object Unauthorized : State
        data object Malformed : State
    }

    /** One top-pages / top-referrers row, as served. */
    data class MetricRow(val label: String, val count: Long)

    data class Stats(
        val visitors: Long,
        val pageviews: Long,
        val visits: Long,
        /** Percent, as served (38.2 means 38.2%). */
        val bounceRate: Double,
        val avgVisitSecs: Long,
        val activeVisitors: Long,
        val topPages: List<MetricRow>,
        val topReferrers: List<MetricRow>,
        val periodDays: Int,
        /** Server "asOf", epoch SECONDS; null when absent. */
        val asOfEpochSec: Long?,
        /** Server "stale": a cached copy served past its TTL. */
        val stale: Boolean,
    )

    /** The web widget's own label for the active count. */
    const val ACTIVE_LABEL = "Active now (≤5 min old)"

    /**
     * Parse one response body. authorized == false (the
     * denied shape, if it ever arrives with a 200) is
     * [State.Unauthorized]; available == false is
     * [State.Unavailable] with the server's reason, normalized
     * to the hyphenated spelling the server ships (the brief's
     * underscore spellings normalize to the same copy).
     */
    fun parse(body: JsonObject): State {
        if (Phase3Logic.bool(body, "authorized") == false) return State.Unauthorized
        return when (Phase3Logic.bool(body, "available")) {
            true -> parseAvailable(body)?.let { State.Available(it) } ?: State.Malformed
            false -> State.Unavailable(normalizeReason(Phase3Logic.strOrNull(body, "reason")))
            null -> State.Malformed
        }
    }

    private fun parseAvailable(body: JsonObject): Stats? {
        // Every headline metric must be present and numeric —
        // anything else is not a payload this surface may
        // render numbers from.
        val visitors = Phase3Logic.long(body, "visitors") ?: return null
        val pageviews = Phase3Logic.long(body, "pageviews") ?: return null
        val visits = Phase3Logic.long(body, "visits") ?: return null
        val bounceRate = Phase3Logic.dbl(body, "bounceRate") ?: return null
        val avgVisitSecs = Phase3Logic.long(body, "avgVisitSecs") ?: return null
        val activeVisitors = Phase3Logic.long(body, "activeVisitors") ?: return null
        return Stats(
            visitors = visitors,
            pageviews = pageviews,
            visits = visits,
            bounceRate = bounceRate,
            avgVisitSecs = avgVisitSecs,
            activeVisitors = activeVisitors,
            topPages = metricRows(body, "topPages"),
            topReferrers = metricRows(body, "topReferrers"),
            // The endpoint's window is fixed server-side
            // (_analytics_fetch reads exactly 30 days and
            // reports periodDays 30); the default only covers
            // a payload that omits the label, never a number
            // anyone reads as a measurement.
            periodDays = Phase3Logic.int(body, "periodDays") ?: 30,
            asOfEpochSec = Phase3Logic.long(body, "asOf"),
            stale = Phase3Logic.bool(body, "stale") == true,
        )
    }

    /** Rows as served, in order, capped at the contract's 5;
     *  a row without a numeric count is dropped, not zeroed. */
    private fun metricRows(body: JsonObject, key: String): List<MetricRow> =
        Phase3Logic.arr(body, key).mapNotNull { row ->
            val count = Phase3Logic.long(row, "count") ?: return@mapNotNull null
            MetricRow(Phase3Logic.str(row, "label"), count)
        }.take(5)

    /** The server ships hyphenated reasons; tolerate the
     *  brief's underscore spellings identically. */
    fun normalizeReason(raw: String?): String =
        raw?.trim()?.lowercase()?.replace('_', '-') ?: ""

    /** The quiet one-liner for an unavailable state — the
     *  server's reason in plain words, never an apology wall. */
    fun reasonCopy(reason: String): String = when (reason) {
        "not-configured" -> "Analytics isn't configured on the server yet."
        "unavailable" -> "Analytics is unreachable right now."
        "fetch-in-progress" -> "Analytics is being refreshed — try again in a moment."
        "demo" -> "Analytics isn't available in the demo."
        else -> "Analytics isn't available right now."
    }

    /**
     * Classify a failed call: only the gate's 403 is a STATE
     * (the account may not see analytics). Anything else
     * returns null — the caller renders its honest failure
     * with a retry. (401 never arrives here: [net.sentinelsafeguard.app.data.api.SentinelApi]
     * turns it into SignedOutException first.)
     */
    fun stateForError(error: Exception): State? =
        if (error is ApiException && error.status == 403) State.Unauthorized else null

    /** A configured, reachable analytics with nothing
     *  recorded: the server's zeros are real, and the
     *  surfaces style them as the empty state. */
    fun isEmptyPeriod(stats: Stats): Boolean =
        stats.visitors == 0L && stats.pageviews == 0L && stats.visits == 0L

    fun emptyPeriodCopy(stats: Stats): String =
        "No visits recorded in the last ${stats.periodDays} days yet."

    // ---- formatting (mirrors the web widget's own) ----

    /** Thousands separators, like web's toLocaleString. */
    fun formatCount(value: Long): String = String.format(Locale.US, "%,d", value)

    /** The percent as served: 38.2 -> "38.2%", 40.0 -> "40%". */
    fun formatBounce(rate: Double): String =
        (if (rate % 1.0 == 0.0) rate.toLong().toString() else rate.toString()) + "%"

    /** Web fmtDur: m:ss — 120 -> "2:00", 45 -> "0:45". */
    fun formatAvgVisit(totalSecs: Long): String {
        val s = maxOf(0L, totalSecs)
        return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
    }

    fun periodLabel(periodDays: Int): String = "Last $periodDays days"

    /** Web renders a referrer's empty label as "(direct)";
     *  page labels pass through untouched. */
    fun referrerLabel(label: String): String = label.ifBlank { "(direct)" }
}

package net.sentinelsafeguard.app.ui

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * 1.6.1 (Feature Wave mirror): the pure shapes of the wave's
 * app-consumed payload additions, each parsed from the payload
 * exactly as the shipped server composes it (contracts quoted
 * from the staging server source; goldens pinned in
 * Wave161Test from the 2026-11-09 rehearsal):
 *
 *  A. whileYouSlept on the dashboard payload — OWNER only,
 *     server-side; the field's ABSENCE is the whole gate (the
 *     app derives nothing and renders no card without it).
 *  B. the briefing-audio action (getwysaudio): the caller
 *     never names a file; the server recomputes the current
 *     card's hash and serves {authorized, available, hash} or
 *     {…, contentType, audioB64}. A denied shape or an
 *     unavailable answer is a QUIET state here, never an error.
 *  C. clientHealth on the staff health-timeline payload —
 *     staff-only by server construction (proven absent from
 *     every client-scoped payload); bands are the server's
 *     strings, rendered verbatim, thresholds never recomputed.
 *  D. photoTimeline groups — the server orders stage rank →
 *     created_at → id; the app groups CONSECUTIVE entries by
 *     stageLabel and never re-sorts.
 *  E. booking prep — {"en":[…], "es":[…]} or null; null (or a
 *     missing key) renders NOTHING, no filler.
 *  F. pipeline lead sources — CF2_LEAD_SOURCES of record with
 *     their labels; unknown future sources humanize.
 */
object Wave161Logic {

    // ---------- A. While You Slept ----------

    data class WysFailure(val kind: String, val label: String, val detail: String)

    data class WysChain(val name: String, val ok: Boolean, val archive: String?, val detail: String)

    data class WysBackups(
        val available: Boolean,
        val checkedAt: Long?,
        val chains: List<WysChain>,
        val allOk: Boolean,
    )

    data class WysAudio(val hash: String, val available: Boolean)

    data class Wys(
        val windowLabel: String,
        val failures: List<WysFailure>,
        val hasFailures: Boolean,
        val newsFiled: Int,
        val newsPublished: Int,
        val threatBriefsDelivered: Int,
        val ticketsTouched: Int,
        val jobsTouched: Int,
        val backups: WysBackups,
        val audio: WysAudio,
    )

    /** Parse the whileYouSlept object; null when the payload
     *  carries no usable card (absent key included) — the
     *  caller renders no card at all in that case. */
    fun parseWys(dashboardField: JsonObject?): Wys? {
        val o = dashboardField ?: return null
        val backupsObj = Phase3Logic.obj(o, "backups")
        val chainsObj = Phase3Logic.obj(backupsObj, "chains")
        val chains = chainsObj?.entries?.map { (name, el) ->
            val c = el as? JsonObject
            WysChain(
                name = name,
                ok = Phase3Logic.bool(c, "ok") == true,
                archive = Phase3Logic.displayValue("archive", c?.get("archive")),
                detail = Phase3Logic.str(c, "detail"),
            )
        } ?: emptyList()
        val audioObj = Phase3Logic.obj(o, "audio")
        return Wys(
            windowLabel = Phase3Logic.str(o, "windowLabel"),
            failures = Phase3Logic.arr(o, "failures").map {
                WysFailure(
                    Phase3Logic.str(it, "kind"),
                    Phase3Logic.str(it, "label"),
                    Phase3Logic.str(it, "detail"),
                )
            },
            hasFailures = Phase3Logic.bool(o, "hasFailures") == true,
            newsFiled = Phase3Logic.int(o, "newsFiled") ?: 0,
            newsPublished = Phase3Logic.int(o, "newsPublished") ?: 0,
            threatBriefsDelivered = Phase3Logic.int(o, "threatBriefsDelivered") ?: 0,
            ticketsTouched = Phase3Logic.int(o, "ticketsTouched") ?: 0,
            jobsTouched = Phase3Logic.int(o, "jobsTouched") ?: 0,
            backups = WysBackups(
                available = Phase3Logic.bool(backupsObj, "available") == true,
                checkedAt = Phase3Logic.long(backupsObj, "checkedAt"),
                chains = chains,
                allOk = Phase3Logic.bool(backupsObj, "allOk") == true,
            ),
            audio = WysAudio(
                hash = Phase3Logic.str(audioObj, "hash"),
                available = Phase3Logic.bool(audioObj, "available") == true,
            ),
        )
    }

    const val BACKUPS_UNAVAILABLE_LINE = "Backup status unavailable"
    const val AUDIO_NOT_GENERATED_LINE = "Briefing audio not generated yet."

    fun backupsSummaryLine(b: WysBackups): String =
        if (!b.available) BACKUPS_UNAVAILABLE_LINE
        else if (b.allOk) "Backups: all checks OK"
        else "Backups: a check needs attention"

    // ---------- B. Briefing audio ----------

    sealed interface WysAudioResult {
        /** Bytes decoded from the server's audioB64 + the
         *  player extension from its contentType. */
        data class Ready(val bytes: ByteArray, val extension: String) : WysAudioResult

        /** available == false from the server. */
        data object NotGenerated : WysAudioResult

        /** The denied shape ({authorized:false}) or anything
         *  unusable — the row degrades to the quiet line. */
        data object Denied : WysAudioResult
    }

    fun parseWysAudio(body: JsonObject): WysAudioResult {
        if (Phase3Logic.bool(body, "authorized") != true) return WysAudioResult.Denied
        if (Phase3Logic.bool(body, "available") != true) return WysAudioResult.NotGenerated
        val b64 = Phase3Logic.strOrNull(body, "audioB64") ?: return WysAudioResult.Denied
        val bytes = try {
            // java.util.Base64, the Phase8Logic pattern — the
            // MIME decoder tolerates any line wrapping and the
            // logic stays JVM-unit-testable.
            java.util.Base64.getMimeDecoder().decode(b64)
        } catch (e: Exception) {
            return WysAudioResult.Denied
        }
        if (bytes.isEmpty()) return WysAudioResult.Denied
        val ext = when (Phase3Logic.str(body, "contentType")) {
            "audio/mpeg" -> "mp3"
            "audio/wav" -> "wav"
            else -> "mp3"
        }
        return WysAudioResult.Ready(bytes, ext)
    }

    // ---------- C. Client health ----------

    data class HealthDeduction(val label: String, val points: Int)

    data class ClientHealth(
        val state: String,
        val score: Int?,
        val band: String?,
        val openTickets: Int,
        val oldestOpenTicketAgeDays: Int,
        val overdueInvoices: Int,
        val expiringAssets: Int,
        /** Server sends an integer, or null when no contact
         *  timestamp exists at all. */
        val daysSinceContact: Long?,
        val deductions: List<HealthDeduction>,
        val trendDirection: String,
        val trendDelta: Int?,
        val trendBaselineScore: Int?,
        val trendBaselineAt: Long?,
    )

    const val HEALTH_INSUFFICIENT_COPY = "Not enough history to score this client yet."

    /** Parse clientHealth; null when the key is absent (older
     *  payloads — the surface then renders exactly as before). */
    fun parseClientHealth(data: JsonObject?): ClientHealth? {
        val h = Phase3Logic.obj(data, "clientHealth") ?: return null
        val signals = Phase3Logic.obj(h, "signals")
        val trend = Phase3Logic.obj(h, "trend")
        return ClientHealth(
            state = Phase3Logic.str(h, "state"),
            score = Phase3Logic.int(h, "score"),
            band = Phase3Logic.strOrNull(h, "band"),
            openTickets = Phase3Logic.int(signals, "openTickets") ?: 0,
            oldestOpenTicketAgeDays = Phase3Logic.int(signals, "oldestOpenTicketAgeDays") ?: 0,
            overdueInvoices = Phase3Logic.int(signals, "overdueInvoices") ?: 0,
            expiringAssets = Phase3Logic.int(signals, "expiringAssets") ?: 0,
            daysSinceContact = Phase3Logic.long(signals, "daysSinceContact"),
            deductions = Phase3Logic.arr(h, "deductions").map {
                HealthDeduction(Phase3Logic.str(it, "label"), Phase3Logic.int(it, "points") ?: 0)
            },
            trendDirection = Phase3Logic.str(trend, "direction"),
            trendDelta = Phase3Logic.int(trend, "delta"),
            trendBaselineScore = Phase3Logic.int(trend, "baselineScore"),
            trendBaselineAt = Phase3Logic.long(trend, "baselineAt"),
        )
    }

    fun isInsufficient(h: ClientHealth): Boolean = h.state == "insufficient" || h.score == null

    /** The color key for a band — keyed to the server's band
     *  STRING, never to a client-side threshold. */
    fun bandColorKey(band: String?): String = when (band) {
        "Good" -> "good"
        "Fair" -> "fair"
        "At risk" -> "atrisk"
        "Critical" -> "critical"
        else -> "none"
    }

    /** "Down 6 from 82 (Nov 2)" style inputs, as served: the
     *  direction + signed delta + the baseline pair. Null when
     *  the server reports no baseline (direction "none"). */
    fun trendLine(h: ClientHealth, baselineDate: String?): String? {
        val delta = h.trendDelta ?: return null
        val base = h.trendBaselineScore ?: return null
        val magnitude = kotlin.math.abs(delta)
        val core = when (h.trendDirection) {
            "up" -> "Up $magnitude from $base"
            "down" -> "Down $magnitude from $base"
            "flat" -> "Flat at $base"
            else -> return null
        }
        return if (baselineDate != null) "$core on $baselineDate" else core
    }

    // ---------- D. Photo timeline ----------

    data class PhotoGroup(val stageLabel: String, val photos: List<JsonObject>)

    /** Group CONSECUTIVE entries by stageLabel, served order
     *  preserved exactly (the server already ordered stage
     *  rank → created_at → id; re-sorting here could only
     *  break that). */
    fun photoGroups(entries: List<JsonObject>): List<PhotoGroup> {
        val out = mutableListOf<PhotoGroup>()
        for (e in entries) {
            val label = Phase3Logic.str(e, "stageLabel").ifBlank { "Other" }
            val last = out.lastOrNull()
            if (last != null && last.stageLabel == label) {
                out[out.lastIndex] = last.copy(photos = last.photos + e)
            } else {
                out += PhotoGroup(label, listOf(e))
            }
        }
        return out
    }

    /** The row's display name: caption first (the human
     *  label), then the original file name. */
    fun photoLabel(photo: JsonObject): String =
        Phase3Logic.str(photo, "caption").takeIf { it.isNotBlank() }
            ?: Phase3Logic.str(photo, "originalFilename").takeIf { it.isNotBlank() }
            ?: Phase3Logic.str(photo, "fileName").takeIf { it.isNotBlank() }
            ?: "Photo"

    // ---------- E. Booking prep ----------

    /** The prep lines for [lang] ("es" → Spanish, anything
     *  else → English); null when the payload carries no prep
     *  (or the chosen list is empty) — the caller renders
     *  NOTHING in that case, no header, no filler. */
    fun prepLines(holder: JsonObject, lang: String): List<String>? {
        val prep = Phase3Logic.obj(holder, "prep") ?: return null
        val key = if (lang.equals("es", ignoreCase = true)) "es" else "en"
        val lines = (prep[key] as? JsonArray)?.mapNotNull { el ->
            (el as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
        } ?: emptyList()
        return lines.ifEmpty { null }
    }

    // ---------- F. Pipeline lead sources ----------

    /** CF2_LEAD_SOURCES of record (server, verbatim) — the
     *  complete set a pipeline payload can carry today. */
    val LEAD_SOURCES = listOf(
        "website_contact", "website_refer", "manual", "contact-form",
        "intake-wizard", "other", "academy-owner-reset", "security-scorecard",
    )

    private val LEAD_SOURCE_LABELS = mapOf(
        "website_contact" to "Website contact",
        "website_refer" to "Website referral",
        "manual" to "Manual",
        "contact-form" to "Contact form",
        "intake-wizard" to "Intake wizard",
        "other" to "Other",
        "academy-owner-reset" to "Academy owner reset",
        "security-scorecard" to "Security scorecard",
    )

    /** The label for a lead source; an unknown future source
     *  humanizes ("partner-referral" → "Partner referral")
     *  instead of rendering raw or vanishing. */
    fun leadSourceLabel(source: String): String {
        val key = source.trim().lowercase()
        LEAD_SOURCE_LABELS[key]?.let { return it }
        if (key.isBlank()) return "Other"
        val words = key.replace('_', ' ').replace('-', ' ')
            .split(' ').filter { it.isNotBlank() }
        if (words.isEmpty()) return "Other"
        return words.joinToString(" ") { it.lowercase() }
            .replaceFirstChar { it.uppercase() }
    }

    /** The filter-chip set derived from the same mapping:
     *  every source of record, in the server's order. */
    fun leadSourceFilters(): List<Pair<String, String>> =
        LEAD_SOURCES.map { it to leadSourceLabel(it) }
}

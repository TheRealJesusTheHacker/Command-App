package net.sentinelsafeguard.app.ui

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.Locale

/**
 * Pure logic for the OSINT flagship deck (Lane E, 1.4.0) — kept
 * free of Compose and Android so the unit suite can pin every
 * rule without a device.
 *
 * The deck mirrors the server toolkit exactly (Command
 * server.py, OSINT-2026-10-27): the identifier types, the
 * two run kinds (identifier / criminal), the per-source status
 * vocabulary (found / not_found / unavailable / manual) and the
 * confidence vocabulary (record / mention / possible / info).
 * Nothing here invents a source, a grade, or a label the server
 * does not already use — presentation elevates, data does not
 * change (mirror law).
 *
 * 1.5.0: the deck wires Command's nine new live server legs
 * (contracts in sentinel-live-deploy/COMMAND-TOOLS-PROGRAM-
 * 2026-11-07/contracts/): seven new identifier types
 * (subdomain, dns, site, archive, btc, eth, breach) ride the
 * same /api/app/osint surface, the IP battery gains its
 * allocation & geo leg, the username battery gains its
 * server-side variation sweep, and the dork templates gain
 * the new type keys. Reconciliations are stated where the
 * tools live, below.
 */
object OsintLogic {

    /** The server's own hit label, verbatim (OSINT_POSSIBLE). */
    const val POSSIBLE_MATCH_LABEL = "POSSIBLE MATCH — identity not verified"

    /** The server's own no-hit wording for the criminal battery
     *  (OSINT_NO_RECORDS), verbatim. */
    const val NO_RECORDS_LABEL = "No public records located in the sources searched"

    /** Report footers, mirroring the server's OSINT_FOOTER and
     *  OSINT_CRIMINAL_FOOTER so the app report carries the same
     *  FCRA posture the compiled report carries. */
    const val REPORT_FOOTER =
        "Methodology and legality: compiled exclusively from publicly " +
            "available sources on the date shown, by the Sentinel " +
            "Safeguard OSINT Toolkit. Findings labeled \"confirmed public " +
            "record\" come from official or primary public records; " +
            "findings labeled \"unverified mention\" come from news or " +
            "aggregator reporting and are leads, not records. Not a " +
            "consumer report under the FCRA; not for use in employment, " +
            "housing, credit, or insurance eligibility decisions."
    const val CRIMINAL_FOOTER =
        "Public sources may miss sealed, expunged, juvenile, or " +
            "non-digitized records. A name match is a possible match only " +
            "— identity is not verified by this compilation. This " +
            "compilation is not an FCRA consumer report and is not, by " +
            "itself, a lawful basis for employment, housing, or credit " +
            "eligibility decisions."

    /** The server's standing wallet note (_WALLET_NOTE), verbatim.
     *  Carried on every wallet result the server returns; the deck
     *  states it up front, before the run. */
    const val WALLET_NOTE =
        "An address is not an identity. Only public chain facts are " +
            "shown; no dollar value is stated anywhere in this result " +
            "(no price source is consulted)."

    /** The breach battery's coverage statement (email-breach
     *  contract, verbatim): partial coverage stated plainly —
     *  Have I Been Pwned, the largest corpus, is NOT covered. */
    const val BREACH_COVERAGE_LINE =
        "Corpora searched: XposedOrNot + LeakCheck public index. " +
            "Have I Been Pwned is NOT covered — its account lookup " +
            "requires a paid key."

    /** The server's IP-intelligence source name, verbatim. */
    const val IP_INTEL_SOURCE = "IP intelligence (allocation & geo)"

    // ---- Deck model -------------------------------------------------

    enum class Group(val title: String, val subtitle: String) {
        IDENTITY("Identity", "Handles, inboxes, people and places"),
        NETWORK("Network", "Domains, addresses, chains and search craft"),
        VEHICLES_AIR("Vehicles & Air", "Plates, VINs, tails and callsigns"),
        BACKGROUND("Background", "Public-records background battery"),
        BREACH("Breach", "Exposure checks and the breach register"),
        ON_DEVICE("On-device", "Tools that never leave this phone"),
    }

    /**
     * One deck tool. [typeKey] is the server identifier type the
     * tool runs (null for tools with no server identifier run);
     * [kind] is the server run kind ("identifier" / "criminal").
     */
    data class Tool(
        val id: String,
        val title: String,
        val subtitle: String,
        val group: Group,
        val typeKey: String? = null,
        val kind: String = "identifier",
        val manual: Boolean = false,
        /** Corner chip for tools whose nature must be labeled on
         *  the card itself (on-device tools). Null = no chip. */
        val chip: String? = null,
    )

    val TOOLS: List<Tool> = listOf(
        Tool("username", "Username sweep",
            "Profile checks across public sites — variations swept server-side",
            Group.IDENTITY, typeKey = "username"),
        Tool("email", "Email check", "Validation, exposure and profile traces",
            Group.IDENTITY, typeKey = "email"),
        Tool("name", "Name / person", "Court, campaign, filings and news records",
            Group.IDENTITY, typeKey = "name"),
        Tool("phone", "Phone", "Carrier, region and reputation checks",
            Group.IDENTITY, typeKey = "phone"),
        Tool("address", "Address", "Property routes, news and archive traces",
            Group.IDENTITY, typeKey = "address"),
        Tool("peopleLinks", "People-search link pack",
            "Pre-filled manual lookups — opened by hand, never scraped",
            Group.IDENTITY, typeKey = "name", manual = true),
        Tool("domain", "Domain intel", "Registration, DNS, certificates, archive",
            Group.NETWORK, typeKey = "domain"),
        Tool("ip", "IP intel", "RDAP, reverse DNS, allocation & geo",
            Group.NETWORK, typeKey = "ip"),
        Tool("subdomains", "Subdomain finder",
            "Certificate transparency + live DNS wordlist sweep",
            Group.NETWORK, typeKey = "subdomain"),
        Tool("dns", "DNS explorer",
            "Every record type, DMARC, DKIM guesses, DNSSEC state",
            Group.NETWORK, typeKey = "dns"),
        Tool("siteInspector", "Site inspector",
            "TLS certificate, response headers and tech facts",
            Group.NETWORK, typeKey = "site"),
        Tool("btc", "Bitcoin wallet",
            "Balance and totals in BTC and satoshis — never a dollar value",
            Group.NETWORK, typeKey = "btc"),
        Tool("eth", "Ethereum wallet",
            "Balance and totals in ETH and wei — never a dollar value",
            Group.NETWORK, typeKey = "eth"),
        Tool("dork", "Dork pack generator",
            "On-device preview — the graded pack ships inside server reports",
            Group.NETWORK, chip = "ON-DEVICE PREVIEW"),
        Tool("plate", "License plate", "Format check and lawful official routes",
            Group.VEHICLES_AIR, typeKey = "plate"),
        Tool("vin", "VIN decode", "Factory build data from the VIN decoder",
            Group.VEHICLES_AIR, typeKey = "vin"),
        Tool("tail", "FAA tail number", "Aircraft registry record for an N-number",
            Group.VEHICLES_AIR, typeKey = "tail"),
        Tool("callsign", "Amateur callsign", "Callook record and FCC routes",
            Group.VEHICLES_AIR, typeKey = "callsign"),
        Tool("criminal", "Criminal background",
            "Public-information battery — possible matches only",
            Group.BACKGROUND, kind = "criminal"),
        Tool("archive", "Wayback snapshot browser",
            "Capture history and replay links for a site, back to its first snapshot",
            Group.BACKGROUND, typeKey = "archive"),
        Tool("emailBreach", "Email breach check",
            "Two corpora, graded separately — HIBP not covered",
            Group.BREACH, typeKey = "breach"),
        Tool("breachRegister", "Breach Intel register",
            "The living breach register and circulation intel",
            Group.BREACH),
        Tool("exif", "Image EXIF viewer",
            "On-device only — photo metadata, nothing uploaded",
            Group.ON_DEVICE, chip = "ON-DEVICE"),
    )

    fun tool(id: String): Tool? = TOOLS.firstOrNull { it.id == id }

    fun toolsInGroup(group: Group): List<Tool> = TOOLS.filter { it.group == group }

    /** Groups in deck order, only those that carry tools. */
    val GROUPS: List<Group> = Group.entries.filter { g -> TOOLS.any { it.group == g } }

    /** Server identifier types in the server's own order
     *  (OSINT_TYPES): the original eleven, then the 1.5.0 legs. */
    val SERVER_TYPES: List<String> = listOf(
        "name", "username", "email", "phone", "address",
        "domain", "ip", "plate", "vin", "tail", "callsign",
        "subdomain", "dns", "site", "archive", "btc", "eth", "breach",
    )

    /** Every server type has a dedicated deck tool. */
    fun typeHasDedicatedTool(typeKey: String): Boolean =
        TOOLS.any { it.typeKey == typeKey && it.kind == "identifier" && !it.manual }

    // ---- Source grades ----------------------------------------------

    enum class Grade(val label: String) {
        HIT("Hit"),
        NO_HIT("No hit"),
        UNAVAILABLE("Unavailable"),
        MANUAL("Manual step"),
    }

    /** Map a server per-source status onto the deck grade. An
     *  unknown status is graded UNAVAILABLE — never a negative:
     *  an unrecognized answer is not evidence of absence. */
    fun gradeForStatus(status: String?): Grade = when (status?.trim()?.lowercase()) {
        "found" -> Grade.HIT
        "not_found" -> Grade.NO_HIT
        "manual" -> Grade.MANUAL
        else -> Grade.UNAVAILABLE
    }

    /** The server's confidence vocabulary (_OSINT_CONF_LABELS). */
    fun confidenceLabel(confidence: String?): String = when (confidence?.trim()?.lowercase()) {
        "record" -> "Confirmed public record"
        "mention" -> "Unverified mention"
        "possible" -> POSSIBLE_MATCH_LABEL
        "info" -> "Reference"
        else -> confidence?.takeIf { it.isNotBlank() } ?: "Reference"
    }

    /** True when a result must carry the possible-match label:
     *  the server marks it so via confidence, or the finding
     *  text already carries the label. */
    fun carriesPossibleMatch(confidence: String?, texts: List<String?>): Boolean {
        if (confidence?.trim()?.lowercase() == "possible") return true
        return texts.any { it?.contains("POSSIBLE MATCH") == true }
    }

    // ---- Server source entries (the _osint_src shape) -------------

    data class SourceFinding(val title: String, val detail: String?, val url: String?)

    data class SourceEntry(
        val source: String,
        val status: String,
        val summary: String?,
        val note: String?,
        val confidence: String?,
        val findings: List<SourceFinding>,
    )

    /**
     * Shape one server per-source entry — the `_osint_src` shape
     * every OSINT leg returns ({source, status, summary,
     * findings[{title, url, detail}], url, confidence, note,
     * retrievedAt}) — into the deck's model. Pure field reads:
     * nothing is reworded, re-graded, or dropped.
     */
    fun parseSourceEntry(result: JsonObject): SourceEntry = SourceEntry(
        source = Phase3Logic.str(result, "source"),
        status = Phase3Logic.str(result, "status"),
        summary = Phase3Logic.strOrNull(result, "summary"),
        note = Phase3Logic.strOrNull(result, "note"),
        confidence = Phase3Logic.strOrNull(result, "confidence"),
        findings = Phase3Logic.arr(result, "findings").map { f ->
            SourceFinding(
                title = Phase3Logic.str(f, "title"),
                detail = Phase3Logic.strOrNull(f, "detail"),
                url = Phase3Logic.strOrNull(f, "url"),
            )
        },
    )

    /**
     * Coordinates for the IP-intelligence entry, ONLY when the
     * payload itself carries them: a structured lat/lon pair on
     * the entry or one of its findings, or an explicit
     * "lat, lon" pair in the entry's own text. The server's
     * current payload reports location as text ("Location:
     * City, Region, Country") with no coordinates — this
     * returns null for it and the report shows no pin. Nothing
     * is geocoded or invented here; if a future payload carries
     * coordinates, they are plotted verbatim.
     */
    fun ipIntelCoordinates(result: JsonObject): Pair<Double, Double>? {
        if (!Phase3Logic.str(result, "source")
                .equals(IP_INTEL_SOURCE, ignoreCase = true)
        ) {
            return null
        }
        structuredPair(result)?.let { return it }
        for (f in Phase3Logic.arr(result, "findings")) {
            structuredPair(f)?.let { return it }
        }
        val texts = listOfNotNull(
            Phase3Logic.strOrNull(result, "summary"),
            Phase3Logic.strOrNull(result, "note"),
        ) + Phase3Logic.arr(result, "findings").flatMap { f ->
            listOfNotNull(
                Phase3Logic.strOrNull(f, "title"),
                Phase3Logic.strOrNull(f, "detail"),
            )
        }
        for (t in texts) {
            extractCoordinates(t)?.let { return it }
        }
        return null
    }

    private fun structuredPair(obj: JsonObject): Pair<Double, Double>? {
        fun num(vararg keys: String): Double? {
            for (k in keys) {
                val p = obj[k] as? JsonPrimitive ?: continue
                p.content.toDoubleOrNull()?.let { return it }
            }
            return null
        }
        val lat = num("lat", "latitude") ?: return null
        val lon = num("lon", "lng", "longitude") ?: return null
        return if (lat in -90.0..90.0 && lon in -180.0..180.0) {
            lat to lon
        } else {
            null
        }
    }

    // ---- Username variants ------------------------------------------

    private val HANDLE_OK = Regex("^[a-z0-9][a-z0-9._-]{1,38}[a-z0-9]$|^[a-z0-9]{2,40}$")

    /**
     * Deterministic handle variants: separator swaps, joined
     * forms, initial forms, and the common numeric / qualifier
     * affixes. The handle itself is always first; output is
     * de-duplicated, validity-filtered, and capped at [max].
     *
     * 1.5.0 reconciliation: the variation SWEEP itself moved
     * server-side — Command generates its own bounded variant
     * set (max 4) inside every username run and reports it as
     * a graded, logged "Username variations" entry in the same
     * report, so the deck's old standalone on-device batch tool
     * (up to 12 sequential searches) was removed rather than
     * duplicate the run. This generator stays as the pinned
     * pattern reference for the variant family; it is not
     * wired to any run.
     */
    fun usernameVariants(raw: String, max: Int = 12): List<String> {
        val base = raw.trim().removePrefix("@").lowercase()
        if (base.length < 2) return emptyList()
        val out = LinkedHashSet<String>()
        fun add(v: String) {
            val c = v.lowercase().trim()
            if (c.length in 2..40 && HANDLE_OK.matches(c) &&
                !c.startsWith(".") && !c.endsWith(".")
            ) {
                out.add(c)
            }
        }
        add(base)
        val parts = base.split('.', '_', '-').filter { it.isNotBlank() }
        if (parts.size >= 2) {
            add(parts.joinToString(""))
            add(parts.joinToString("."))
            add(parts.joinToString("_"))
            add(parts.first().take(1) + parts.last())
            add(parts.first() + parts.last().take(1))
        } else {
            add(base.replace('.', '_'))
            add(base.replace('_', '.'))
        }
        add(base + "1")
        add(base + "01")
        add(base + "123")
        add("the" + base)
        add("real" + base)
        add(base + "official")
        add("its" + base)
        return out.take(max.coerceAtLeast(1))
    }

    // ---- Dork pack ----------------------------------------------------

    /**
     * Fill workspace dork templates for a target — the exact
     * substitution the server performs (_osint_dorks): {v},
     * {city}, {state}, whitespace collapsed.
     */
    fun fillDorks(
        templates: List<String>,
        value: String,
        city: String = "",
        state: String = "",
    ): List<String> = templates.map { t ->
        t.replace("{v}", value).replace("{city}", city)
            .replace("{state}", state)
            .replace(Regex("\\s+"), " ").trim()
    }.filter { it.isNotBlank() }

    // ---- Coordinates readout ------------------------------------------

    private val COORD_PAIR =
        Regex("(-?\\d{1,3}\\.\\d{3,})\\s*[, ]\\s*(-?\\d{1,3}\\.\\d{3,})")

    /** Extract the first plausible lat/lon pair from free text. */
    fun extractCoordinates(text: String?): Pair<Double, Double>? {
        if (text.isNullOrBlank()) return null
        for (m in COORD_PAIR.findAll(text)) {
            val lat = m.groupValues[1].toDoubleOrNull() ?: continue
            val lon = m.groupValues[2].toDoubleOrNull() ?: continue
            if (lat in -90.0..90.0 && lon in -180.0..180.0) return lat to lon
        }
        return null
    }

    /** Stark-style technical readout for a coordinate pair. Map
     *  plotting is integration-pending (Lane C map view); the
     *  deck shows the readout, never a dead map affordance. */
    fun coordinateReadout(lat: Double, lon: Double): String =
        String.format(Locale.US, "LAT %.6f · LON %.6f", lat, lon)

    fun coordinateReadout(text: String?): String? =
        extractCoordinates(text)?.let { (lat, lon) -> coordinateReadout(lat, lon) }

    // ---- Breach severity ----------------------------------------------

    /** Severity ordering for grading visuals (server bands). */
    fun severityRank(severity: String?): Int = when (severity?.trim()?.lowercase()) {
        "critical" -> 4
        "high" -> 3
        "medium" -> 2
        "low" -> 1
        else -> 0
    }

    fun severityKey(severity: String?): String =
        severity?.trim()?.lowercase()?.takeIf {
            it in setOf("critical", "high", "medium", "low")
        } ?: "low"

    // ---- EXIF (pure shaping; reading itself is framework-side) -------

    data class ExifRow(val label: String, val value: String)

    /** Preferred display order for the EXIF card. */
    val EXIF_ORDER: List<Pair<String, String>> = listOf(
        "Make" to "Camera make",
        "Model" to "Camera model",
        "DateTime" to "Captured (device clock)",
        "DateTimeOriginal" to "Captured (original)",
        "GPSLatitude" to "GPS latitude (raw)",
        "GPSLongitude" to "GPS longitude (raw)",
        "GPSAltitude" to "GPS altitude (raw)",
        "ImageWidth" to "Width",
        "ImageLength" to "Height",
        "Orientation" to "Orientation",
        "Software" to "Software",
        "ExposureTime" to "Exposure",
        "FNumber" to "Aperture",
        "ISOSpeedRatings" to "ISO",
        "FocalLength" to "Focal length",
        "Flash" to "Flash",
        "WhiteBalance" to "White balance",
    )

    /** Shape a raw tag map into ordered display rows, skipping
     *  blanks; unknown tags follow in alphabetical order so no
     *  metadata the file carries is silently dropped. */
    fun exifRows(raw: Map<String, String?>): List<ExifRow> {
        val out = mutableListOf<ExifRow>()
        val seen = mutableSetOf<String>()
        for ((tag, label) in EXIF_ORDER) {
            val v = raw[tag]?.trim().orEmpty()
            if (v.isNotBlank()) {
                out += ExifRow(label, v)
                seen += tag
            }
        }
        for (tag in raw.keys.sorted()) {
            if (tag in seen) continue
            val v = raw[tag]?.trim().orEmpty()
            if (v.isNotBlank()) out += ExifRow(tag, v)
        }
        return out
    }

    /**
     * Parse an EXIF rational DMS string ("38/1,53/1,5160/100")
     * plus hemisphere ref into decimal degrees. Returns null on
     * anything malformed — a bad coordinate is shown raw, never
     * guessed.
     */
    fun dmsToDecimal(dms: String?, ref: String?): Double? {
        if (dms.isNullOrBlank()) return null
        val parts = dms.split(",").map { it.trim() }
        if (parts.size != 3) return null
        fun rational(s: String): Double? {
            val bits = s.split("/")
            return when (bits.size) {
                1 -> bits[0].toDoubleOrNull()
                2 -> {
                    val n = bits[0].toDoubleOrNull() ?: return null
                    val d = bits[1].toDoubleOrNull() ?: return null
                    if (d == 0.0) null else n / d
                }
                else -> null
            }
        }
        val deg = rational(parts[0]) ?: return null
        val min = rational(parts[1]) ?: return null
        val sec = rational(parts[2]) ?: return null
        var dec = deg + min / 60.0 + sec / 3600.0
        val r = ref?.trim()?.uppercase().orEmpty()
        if (r == "S" || r == "W") dec = -dec
        return dec
    }
}

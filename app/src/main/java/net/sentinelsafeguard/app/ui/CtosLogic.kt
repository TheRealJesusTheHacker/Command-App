package net.sentinelsafeguard.app.ui

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.util.Locale

/**
 * Pure logic for the CToS "OSINT tools" section (Command App
 * 1.7.0, CTOS-APP-2026-11-11) — kept free of Compose and Android
 * so the unit suite can pin every rule without a device.
 *
 * Mirror law applies exactly as it does in [OsintLogic]: the
 * deck is driven by the SERVER's tool metadata (ctosget.tools),
 * the status vocabulary is the server's (found / not_found /
 * unavailable / manual / not_configured, plus the profiler's
 * unknown), and nothing here invents a source, a grade, or a
 * label the server does not already use. An unrecognized status
 * is surfaced as unavailable — never as a negative.
 *
 * Tool RUNS ride the server twins so app results are the web's
 * results, byte for byte, and record into cases server-side.
 * The one on-device tool is the File Hash Generator: the web
 * computes it in the browser and the file never leaves it; the
 * app computes it on the phone and the file never leaves the
 * phone. Every other utility rides ctostool like the web.
 */
object CtosLogic {

    // ---- Deck model -------------------------------------------------

    enum class Group(val title: String, val subtitle: String) {
        PROFILER("Profiler", "People and handle recon"),
        INFRASTRUCTURE("Infrastructure", "Domains, addresses and sites"),
        FILES("Files & Numbers", "Media metadata, file hashes, phone parse"),
        UTILITIES("Utilities", "Local-compute toolbox"),
    }

    /** Server tool name → deck group. Tools the server adds in a
     *  later wave land in [Group.INFRASTRUCTURE] until grouped
     *  here — visible, never hidden. */
    fun groupFor(toolName: String): Group = when (toolName) {
        "quickrecon", "username", "diligence" -> Group.PROFILER
        "dashboard", "dns", "ip", "whois", "fingerprint" -> Group.INFRASTRUCTURE
        "metadata", "filehash", "phone" -> Group.FILES
        "base64", "hex", "jsonformat", "jwt", "password", "timestamp",
        "urlanalyze", "email", "cidr", "defang", "extractor",
        "hashanalyze", "uaparse", "ports" -> Group.UTILITIES
        else -> Group.INFRASTRUCTURE
    }

    data class ToolMeta(
        val name: String,
        val label: String,
        val desc: String,
        val inputHint: String,
        val network: Boolean,
        val kind: String,
        /** AUDIT-171 B-17 [OSINT]: true for the authorization-gated
         *  active tools (pentest / bounty / proxyscan). The flag
         *  rode the payload all along; the parse used to drop it,
         *  so a gated scan looked exactly like a passive lookup. */
        val gated: Boolean = false,
    )

    fun parseTools(workspace: JsonObject?): List<ToolMeta> =
        Phase3Logic.arr(workspace, "tools").map { t ->
            ToolMeta(
                name = Phase3Logic.str(t, "name"),
                label = Phase3Logic.str(t, "label"),
                desc = Phase3Logic.str(t, "desc"),
                inputHint = Phase3Logic.str(t, "input"),
                network = Phase3Logic.bool(t, "network") == true,
                kind = Phase3Logic.str(t, "kind"),
                gated = Phase3Logic.bool(t, "gated") == true,
            )
        }.filter { it.name.isNotBlank() }

    fun toolsInGroup(tools: List<ToolMeta>, group: Group): List<ToolMeta> =
        tools.filter { groupFor(it.name) == group }

    /** Groups in deck order, only those carrying tools. */
    fun groupsPresent(tools: List<ToolMeta>): List<Group> =
        Group.entries.filter { g -> tools.any { groupFor(it.name) == g } }

    // ---- Cases --------------------------------------------------------

    data class CaseRow(
        val id: Int,
        val title: String,
        val target: String,
        val targetType: String,
        val note: String,
        val status: String,
        val findings: Int,
        val artifacts: Int,
        val updatedAt: Long,
    )

    fun parseCase(obj: JsonObject): CaseRow? {
        val id = Phase3Logic.int(obj, "id") ?: return null
        return CaseRow(
            id = id,
            title = Phase3Logic.str(obj, "title"),
            target = Phase3Logic.str(obj, "target"),
            targetType = Phase3Logic.str(obj, "targetType"),
            note = Phase3Logic.str(obj, "note"),
            status = Phase3Logic.str(obj, "status"),
            findings = Phase3Logic.int(obj, "findings") ?: 0,
            artifacts = Phase3Logic.int(obj, "artifacts") ?: 0,
            updatedAt = Phase3Logic.long(obj, "updatedAt") ?: 0L,
        )
    }

    fun parseCases(workspace: JsonObject?): List<CaseRow> =
        Phase3Logic.arr(workspace, "cases").mapNotNull { parseCase(it) }

    /**
     * AUDIT-171 B-07 [OSINT]: case pickers resolved the tap by
     * case TITLE — and case titles default to the target on the
     * server, so two cases on one target share a title by
     * construction and the first silently won. Chips now carry
     * the case id ("#7 · target"), and resolution reads the id.
     */
    fun caseChipLabel(c: CaseRow): String = "#${c.id} · ${c.title}"

    fun caseFromChip(cases: List<CaseRow>, chip: String): CaseRow? {
        val id = chip.removePrefix("#").substringBefore(" ·").trim().toIntOrNull()
        if (id != null) return cases.firstOrNull { it.id == id }
        return cases.firstOrNull { it.title == chip }
    }

    /**
     * AUDIT-171 B-20 [OSINT]: one artifact's content out of a case
     * export. The case GET deliberately omits artifact content;
     * the export carries it. Artifacts have no id on either
     * payload, so the match is the (label, tool, createdAt)
     * triple the case screen rendered.
     */
    fun artifactContent(
        exportPayload: JsonObject?,
        label: String,
        tool: String,
        createdAt: Long,
    ): JsonObject? {
        val exp = Phase3Logic.obj(exportPayload, "export") ?: exportPayload
        val match = Phase3Logic.arr(exp, "artifacts").firstOrNull { a ->
            Phase3Logic.str(a, "label") == label &&
                Phase3Logic.str(a, "tool") == tool &&
                (Phase3Logic.long(a, "createdAt") ?: 0L) == createdAt
        } ?: return null
        return match["content"] as? JsonObject
    }

    // ---- Case report (AUDIT-171 B-18) ----------------------------------

    data class CaseReport(
        val case: CaseRow?,
        val findingsByType: List<Pair<String, Int>>,
        val findings: List<FindingRow>,
        val artifacts: List<ArtifactRow>,
        val footer: String,
        val generatedAt: Long,
    )

    /** Parse the printable-report twin's payload ({ok, report:{…}}). */
    fun parseReport(payload: JsonObject?): CaseReport? {
        val report = Phase3Logic.obj(payload, "report") ?: return null
        val byType = Phase3Logic.obj(report, "findingsByType")
        return CaseReport(
            case = Phase3Logic.obj(report, "case")?.let { parseCase(it) },
            findingsByType = byType?.entries
                ?.map { (k, v) -> k to ((v as? JsonPrimitive)?.content?.toIntOrNull() ?: 0) }
                ?.sortedBy { it.first } ?: emptyList(),
            findings = Phase3Logic.arr(report, "findings").map { f ->
                FindingRow(
                    id = 0,
                    tool = Phase3Logic.str(f, "tool"),
                    type = Phase3Logic.str(f, "type"),
                    value = Phase3Logic.str(f, "value"),
                    detail = Phase3Logic.str(f, "detail"),
                    url = Phase3Logic.str(f, "url"),
                    createdAt = Phase3Logic.long(f, "createdAt") ?: 0L,
                )
            },
            artifacts = parseArtifacts(report),
            footer = Phase3Logic.str(report, "footer"),
            generatedAt = Phase3Logic.long(report, "generatedAt") ?: 0L,
        )
    }

    /** The report as shareable plain text — the same content the
     *  report screen renders, in the server's own words (footer
     *  included verbatim). */
    fun renderReportText(report: CaseReport): String {
        val sb = StringBuilder()
        val c = report.case
        sb.appendLine("CToS case report — ${c?.title ?: "Case"}")
        if (c != null) {
            sb.appendLine("Target: ${c.target}${if (c.targetType.isNotBlank()) " (${c.targetType})" else ""}")
            sb.appendLine("Status: ${c.status}")
        }
        sb.appendLine()
        sb.appendLine("Findings (${report.findings.size})")
        for ((type, count) in report.findingsByType) {
            sb.appendLine("  $type: $count")
        }
        for (f in report.findings) {
            sb.appendLine()
            sb.appendLine("[${f.type}] ${f.value}")
            if (f.tool.isNotBlank()) sb.appendLine("  Tool: ${f.tool}")
            if (f.detail.isNotBlank()) sb.appendLine("  ${f.detail}")
            if (f.url.isNotBlank()) sb.appendLine("  ${f.url}")
        }
        if (report.artifacts.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("Artifacts (${report.artifacts.size})")
            for (a in report.artifacts) {
                sb.appendLine("  ${a.label}${if (a.tool.isNotBlank()) " — ${a.tool}" else ""}")
            }
        }
        if (report.footer.isNotBlank()) {
            sb.appendLine()
            sb.appendLine(report.footer)
        }
        return sb.toString().trim()
    }

    data class FindingRow(
        val id: Int,
        val tool: String,
        val type: String,
        val value: String,
        val detail: String,
        val url: String,
        val createdAt: Long,
    )

    fun parseFindings(casePayload: JsonObject?): List<FindingRow> =
        Phase3Logic.arr(casePayload, "findings").map { f ->
            FindingRow(
                id = Phase3Logic.int(f, "id") ?: 0,
                tool = Phase3Logic.str(f, "tool"),
                type = Phase3Logic.str(f, "type"),
                value = Phase3Logic.str(f, "value"),
                detail = Phase3Logic.str(f, "detail"),
                url = Phase3Logic.str(f, "url"),
                createdAt = Phase3Logic.long(f, "createdAt") ?: 0L,
            )
        }

    data class ArtifactRow(
        val id: Int,
        val tool: String,
        val kind: String,
        val label: String,
        val createdAt: Long,
    )

    fun parseArtifacts(casePayload: JsonObject?): List<ArtifactRow> =
        Phase3Logic.arr(casePayload, "artifacts").map { a ->
            ArtifactRow(
                id = Phase3Logic.int(a, "id") ?: 0,
                tool = Phase3Logic.str(a, "tool"),
                kind = Phase3Logic.str(a, "kind"),
                label = Phase3Logic.str(a, "label"),
                createdAt = Phase3Logic.long(a, "createdAt") ?: 0L,
            )
        }

    fun findingTypes(workspace: JsonObject?): List<String> =
        Phase3Logic.arrRaw(workspace, "findingTypes")
            .mapNotNull { (it as? JsonPrimitive)?.content }
            .filter { it.isNotBlank() }

    // ---- Source keys (configured state ONLY) -------------------------

    data class KeySource(val source: String, val label: String, val configured: Boolean)

    /** Friendly names for the server's wired key sources
     *  (CTOS_KEY_SOURCES). An unlisted source still renders —
     *  under its raw name — so a future wired source is visible
     *  the day the server adds it. */
    val KEY_LABELS: Map<String, String> = mapOf(
        "hibp" to "Have I Been Pwned",
        "greynoise" to "GreyNoise",
        "virustotal" to "VirusTotal",
        "abuseipdb" to "AbuseIPDB",
        "shodan" to "Shodan",
        "securitytrails" to "SecurityTrails",
    )

    fun parseKeySources(workspace: JsonObject?): List<KeySource> {
        val obj = Phase3Logic.obj(workspace, "keySources") ?: return emptyList()
        return obj.entries.map { (k, v) ->
            KeySource(
                source = k,
                label = KEY_LABELS[k] ?: k,
                configured = (v as? JsonPrimitive)?.content == "true",
            )
        }.sortedBy { it.label.lowercase() }
    }

    // ---- OPSEC profile + authorization gate (AUDIT-171 B-17) ------
    //
    // The workspace payload has always carried `profile`,
    // `profileOptions` and `gate` (a_ctosget); the app parsed none
    // of them, so the OPSEC profile was web-only and the gate
    // invisible in-app. Profile EDITING rode the S-2 twin from
    // TIER Phase B (POST /api/app/osint/ctos/profile — see
    // profileSavePayload); gate management stays owner-only on
    // the web — the app shows the registry state, including the
    // server's own canManage flag, never a write.

    data class OpsecProfile(
        val doh: String,
        val dohCustom: String,
        val uaRotate: Boolean,
        val jitterMs: Int,
    )

    fun parseProfile(workspace: JsonObject?): OpsecProfile? {
        val p = Phase3Logic.obj(workspace, "profile") ?: return null
        return OpsecProfile(
            doh = Phase3Logic.str(p, "doh").ifBlank { "google" },
            dohCustom = Phase3Logic.str(p, "dohCustom"),
            uaRotate = Phase3Logic.bool(p, "uaRotate") == true,
            jitterMs = Phase3Logic.int(p, "jitterMs") ?: 0,
        )
    }

    data class ProfileOptionRow(val key: String, val label: String, val detail: String)

    data class UnavailableRow(val label: String, val reason: String)

    data class ProfileOptions(
        val wired: List<ProfileOptionRow>,
        val unavailable: List<UnavailableRow>,
        val providers: List<String>,
    )

    fun parseProfileOptions(workspace: JsonObject?): ProfileOptions? {
        val o = Phase3Logic.obj(workspace, "profileOptions") ?: return null
        return ProfileOptions(
            wired = Phase3Logic.arr(o, "wired").map { w ->
                ProfileOptionRow(
                    key = Phase3Logic.str(w, "key"),
                    label = Phase3Logic.str(w, "label"),
                    detail = Phase3Logic.str(w, "detail"),
                )
            },
            unavailable = Phase3Logic.arr(o, "unavailable").map { u ->
                UnavailableRow(
                    label = Phase3Logic.str(u, "label"),
                    reason = Phase3Logic.str(u, "reason"),
                )
            },
            providers = Phase3Logic.arrRaw(o, "providers")
                .mapNotNull { (it as? JsonPrimitive)?.content }
                .filter { it.isNotBlank() },
        )
    }

    /**
     * S-2 save payload (TIER Phase B): the profile twin's rule is
     * absent = preserve, so the payload carries ONLY the fields
     * whose edited value differs from the profile the server last
     * confirmed ([current]). Sending an unchanged field would be
     * harmless on the server but dishonest about what changed —
     * and re-sending a stale dohCustom could clobber a newer web
     * edit. Jitter is clamped to the handler's 0..2000 range.
     * An empty result means "nothing to save" — the panel keeps
     * its Save button disabled rather than posting a no-op.
     */
    fun profileSavePayload(
        current: OpsecProfile,
        doh: String,
        dohCustom: String,
        uaRotate: Boolean,
        jitterMs: Int,
    ): JsonObject = buildJsonObject {
        if (doh != current.doh) put("doh", doh)
        if (dohCustom.trim() != current.dohCustom) put("dohCustom", dohCustom.trim())
        if (uaRotate != current.uaRotate) put("uaRotate", uaRotate)
        val jitter = jitterMs.coerceIn(0, 2000)
        if (jitter != current.jitterMs) put("jitterMs", jitter)
    }

    /** Human line for one wired profile setting's current value. */
    fun profileValueLine(profile: OpsecProfile, key: String): String = when (key) {
        "doh" -> if (profile.doh == "custom") {
            "Custom endpoint${profile.dohCustom.takeIf { it.isNotBlank() }?.let { " — $it" } ?: ""}"
        } else {
            profile.doh.replaceFirstChar { it.uppercase() }
        }
        "uaRotate" -> if (profile.uaRotate) "On — rotates per run" else "Off"
        "jitterMs" -> if (profile.jitterMs > 0) "Up to ${profile.jitterMs} ms" else "Off"
        else -> ""
    }

    data class GateEntry(
        val id: Int,
        val target: String,
        val targetKind: String,
        val basisKind: String,
        val clientLabel: String,
        val status: String,
    )

    data class GatePanel(
        val rule: String,
        val canManage: Boolean,
        val entries: List<GateEntry>,
        val auditCount: Int,
        val poolCount: Int,
    )

    fun parseGate(workspace: JsonObject?): GatePanel? {
        val g = Phase3Logic.obj(workspace, "gate") ?: return null
        return GatePanel(
            rule = Phase3Logic.str(g, "rule"),
            canManage = Phase3Logic.bool(g, "canManage") == true,
            entries = Phase3Logic.arr(g, "entries").map { e ->
                GateEntry(
                    id = Phase3Logic.int(e, "id") ?: 0,
                    target = Phase3Logic.str(e, "target"),
                    targetKind = Phase3Logic.str(e, "targetKind"),
                    basisKind = Phase3Logic.str(e, "basisKind"),
                    clientLabel = Phase3Logic.str(e, "clientLabel"),
                    status = Phase3Logic.str(e, "status"),
                )
            },
            auditCount = Phase3Logic.arr(g, "audit").size,
            poolCount = Phase3Logic.arr(g, "pool").size,
        )
    }

    fun catalogLine(workspace: JsonObject?): String? {
        val cat = Phase3Logic.obj(workspace, "catalog") ?: return null
        val platforms = Phase3Logic.int(cat, "platforms") ?: return null
        val quick = Phase3Logic.int(cat, "quick")
        return if (quick != null && quick != platforms) {
            "Username catalog: $platforms platforms ($quick at quick depth)"
        } else {
            "Username catalog: $platforms platforms"
        }
    }

    // ---- Status vocabulary --------------------------------------------

    enum class Status(val label: String) {
        FOUND("Found"),
        NOT_FOUND("Not found"),
        UNKNOWN("Unknown"),
        UNAVAILABLE("Unavailable"),
        MANUAL("Manual step"),
        NOT_CONFIGURED("Not configured"),
    }

    /** The server's honesty semantics, verbatim: a source that
     *  refused, timed out, or is keyless is NEVER a negative.
     *  "unknown" is the profiler's word for the same thing and
     *  keeps its own label because the server distinguishes it
     *  in counts. Anything unrecognized grades UNAVAILABLE. */
    fun statusFor(raw: String?): Status = when (raw?.trim()?.lowercase()) {
        "found" -> Status.FOUND
        "not_found" -> Status.NOT_FOUND
        "unknown" -> Status.UNKNOWN
        "manual" -> Status.MANUAL
        "not_configured" -> Status.NOT_CONFIGURED
        else -> Status.UNAVAILABLE
    }

    // ---- Tool results ---------------------------------------------------

    data class ToolResult(
        val sources: List<OsintLogic.SourceEntry>,
        val data: JsonObject?,
        val recorded: Int?,
        /** AUDIT-171 B-19 [OSINT]: the result's top-level findings.
         *  The parse used to read sources + data only, so anything
         *  a tool returned solely as a finding was invisible here
         *  (it could still record into a case server-side, which
         *  the operator could not verify from this screen). */
        val findings: List<FindingRow> = emptyList(),
    )

    /** Shape a ctostool twin payload ({ok, tool, result:
     *  {sources, data, findings}, recorded}) for the screen.
     *  The per-source entries are the same _osint_src shape the
     *  flagship deck already parses — one parser, no fork. */
    fun parseToolResult(payload: JsonObject?): ToolResult {
        val result = Phase3Logic.obj(payload, "result")
        val sources = (result?.get("sources") as? JsonArray)
            ?.mapNotNull { it as? JsonObject }
            ?.map { OsintLogic.parseSourceEntry(it) }
            ?: emptyList()
        val toolName = Phase3Logic.str(payload, "tool")
        return ToolResult(
            sources = sources,
            data = result?.get("data") as? JsonObject,
            recorded = Phase3Logic.int(payload, "recorded"),
            findings = Phase3Logic.arr(result, "findings").map { f ->
                FindingRow(
                    id = Phase3Logic.int(f, "id") ?: 0,
                    tool = Phase3Logic.str(f, "tool").ifBlank { toolName },
                    type = Phase3Logic.str(f, "type"),
                    value = Phase3Logic.str(f, "value"),
                    detail = Phase3Logic.str(f, "detail"),
                    url = Phase3Logic.str(f, "url"),
                    createdAt = Phase3Logic.long(f, "createdAt") ?: 0L,
                )
            },
        )
    }

    // ---- Username profiler result --------------------------------------

    data class ProfileRow(
        val site: String,
        val cat: String,
        val status: Status,
        val url: String,
    )

    data class UsernameResult(
        val username: String,
        val depth: String,
        val found: Int,
        val notFound: Int,
        val unknown: Int,
        val total: Int,
        val rows: List<ProfileRow>,
    )

    /** Parse the profiler's data block ({username, depth,
     *  counts{found, not_found, unknown, total}, results[]}).
     *  Returns null for any other tool's data. Rows keep the
     *  server's order (found first, then not_found, then
     *  unknown) — the screen groups, it does not re-sort. */
    fun parseUsernameData(data: JsonObject?): UsernameResult? {
        if (data == null) return null
        val rowsEl = data["results"] as? JsonArray ?: return null
        val counts = Phase3Logic.obj(data, "counts")
        val rows = rowsEl.mapNotNull { it as? JsonObject }.map { r ->
            ProfileRow(
                site = Phase3Logic.str(r, "site"),
                cat = Phase3Logic.str(r, "cat"),
                status = statusFor(Phase3Logic.str(r, "status")),
                url = Phase3Logic.str(r, "url"),
            )
        }
        return UsernameResult(
            username = Phase3Logic.str(data, "username"),
            depth = Phase3Logic.str(data, "depth"),
            found = Phase3Logic.int(counts, "found") ?: rows.count { it.status == Status.FOUND },
            notFound = Phase3Logic.int(counts, "not_found")
                ?: rows.count { it.status == Status.NOT_FOUND },
            unknown = Phase3Logic.int(counts, "unknown")
                ?: rows.count { it.status == Status.UNKNOWN || it.status == Status.UNAVAILABLE },
            total = Phase3Logic.int(counts, "total") ?: rows.size,
            rows = rows,
        )
    }

    // ---- Generic structured data shaping -------------------------------

    sealed interface DataNode {
        data class Line(val label: String, val value: String) : DataNode
        data class Section(val title: String, val children: List<DataNode>) : DataNode
    }

    /** "spfRecord" → "Spf record"; "detected_type" → "Detected
     *  type". Display-only casing over the server's own keys. */
    private val KEY_ACRONYMS = setOf(
        "spf", "dkim", "dmarc", "dns", "tls", "ssl", "ip", "ipv4",
        "ipv6", "url", "uri", "http", "https", "html", "mx", "ns",
        "soa", "txt", "asn", "rdap", "id", "os", "ua", "cve",
    )

    /** Keys read as sentence-case labels ("sizeBytes" -> "Size
     *  bytes"); known acronyms stay upper-case ("spfRecord" ->
     *  "SPF record"). */
    fun prettyKey(key: String): String {
        val spaced = key.replace('_', ' ').replace('-', ' ')
            .replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), " ")
            .trim()
        if (spaced.isEmpty()) return key
        val words = spaced.split(Regex("\\s+"))
        return words.mapIndexed { index, word ->
            val lower = word.lowercase(Locale.US)
            when {
                lower in KEY_ACRONYMS -> lower.uppercase(Locale.US)
                index == 0 -> word.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.US)
                    else it.toString()
                }
                else -> lower
            }
        }.joinToString(" ")
    }

    private fun primitiveText(el: JsonElement): String? = when (el) {
        is JsonPrimitive -> when {
            el.isString -> el.content
            el.content == "null" -> null
            el.content == "true" -> "Yes"
            el.content == "false" -> "No"
            else -> el.content
        }
        else -> null
    }

    /** A short title for an object inside an array: the first
     *  of the server's own identifying fields, else its index. */
    private fun itemTitle(obj: JsonObject, index: Int): String {
        for (k in listOf("site", "name", "tool", "port", "title", "label", "type")) {
            val v = (obj[k] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
            if (v != null) return if (k == "port") "Port $v" else v
        }
        return "Item ${index + 1}"
    }

    /**
     * Flatten a tool's `data` object into display nodes. Pure
     * field reads in the payload's own order — nothing reworded,
     * nothing dropped except JSON nulls and blank strings (the
     * detail-grid rule the rest of the app follows). [depth]
     * caps pathological nesting (the dashboard's module results)
     * at a readable depth; past the cap a subtree renders as
     * its key count rather than vanishing.
     *
     * AUDIT-171 B-19: the cap rises from 3 to 5 — at 3, real tool
     * payloads (nested module data) degraded to "N fields" counts
     * instead of their values on a normal result. The cap still
     * bounds pathological nesting.
     */
    fun shapeData(data: JsonObject?, depth: Int = 0): List<DataNode> {
        if (data == null) return emptyList()
        val out = mutableListOf<DataNode>()
        for ((key, el) in data.entries) {
            val label = prettyKey(key)
            when (el) {
                is JsonPrimitive -> primitiveText(el)?.takeIf { it.isNotBlank() }
                    ?.let { out += DataNode.Line(label, it) }
                is JsonObject -> {
                    if (depth >= 5) {
                        out += DataNode.Line(label, "${el.size} fields")
                    } else {
                        val children = shapeData(el, depth + 1)
                        if (children.isNotEmpty()) {
                            out += DataNode.Section(label, children)
                        }
                    }
                }
                is JsonArray -> {
                    if (el.isEmpty()) continue
                    val prims = el.map { primitiveText(it) }
                    if (prims.all { it != null }) {
                        val joined = prims.filterNotNull().filter { it.isNotBlank() }
                        if (joined.isNotEmpty()) {
                            out += DataNode.Line(label, joined.joinToString(", "))
                        }
                    } else if (depth >= 5) {
                        out += DataNode.Line(label, "${el.size} items")
                    } else {
                        val children = mutableListOf<DataNode>()
                        el.forEachIndexed { i, item ->
                            if (item is JsonObject) {
                                val sub = shapeData(item, depth + 2)
                                if (sub.isNotEmpty()) {
                                    children += DataNode.Section(itemTitle(item, i), sub)
                                }
                            } else {
                                primitiveText(item)?.takeIf { it.isNotBlank() }
                                    ?.let { children += DataNode.Line("Item ${i + 1}", it) }
                            }
                        }
                        if (children.isNotEmpty()) {
                            out += DataNode.Section(label, children)
                        }
                    }
                }
                else -> Unit
            }
        }
        return out
    }

    // ---- Run-form options ------------------------------------------------

    /** Tools whose form carries a depth toggle (server options
     *  key "depth": quick | full). */
    val DEPTH_TOOLS: Set<String> = setOf("username", "quickrecon")

    /** Tools whose form carries a mode toggle: tool → (option
     *  key, choices as (value, label)). Mirrors _ctos_util's
     *  option reads exactly. */
    val MODE_TOOLS: Map<String, Pair<String, List<Pair<String, String>>>> = mapOf(
        "base64" to ("mode" to listOf("encode" to "Encode", "decode" to "Decode")),
        "hex" to ("mode" to listOf("encode" to "Encode", "decode" to "Decode")),
        "defang" to ("mode" to listOf("defang" to "Defang", "refang" to "Refang")),
        "jsonformat" to ("mode" to listOf("pretty" to "Pretty-print", "minify" to "Minify")),
    )

    /** Multiline input tools (bulk text / tokens / user agents
     *  are pasted, not typed line by line). */
    val MULTILINE_TOOLS: Set<String> =
        setOf("extractor", "jwt", "uaparse", "jsonformat")

    // ---- On-device file hashing (the File Hash Generator) ---------------

    data class FileHashes(
        val md5: String,
        val sha1: String,
        val sha256: String,
        val sha512: String,
        val sizeBytes: Long,
    )

    /** Streaming digests — the caller feeds chunks, so a large
     *  file never sits whole in memory. */
    class Hasher {
        private val md5 = MessageDigest.getInstance("MD5")
        private val sha1 = MessageDigest.getInstance("SHA-1")
        private val sha256 = MessageDigest.getInstance("SHA-256")
        private val sha512 = MessageDigest.getInstance("SHA-512")
        private var size = 0L

        fun update(chunk: ByteArray, length: Int) {
            md5.update(chunk, 0, length)
            sha1.update(chunk, 0, length)
            sha256.update(chunk, 0, length)
            sha512.update(chunk, 0, length)
            size += length
        }

        fun finish(): FileHashes = FileHashes(
            md5 = md5.digest().toHex(),
            sha1 = sha1.digest().toHex(),
            sha256 = sha256.digest().toHex(),
            sha512 = sha512.digest().toHex(),
            sizeBytes = size,
        )
    }

    fun hashesOf(bytes: ByteArray): FileHashes {
        val h = Hasher()
        h.update(bytes, bytes.size)
        return h.finish()
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }

    /** Human byte size for the hash card. */
    fun sizeText(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else -> String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
    }
}

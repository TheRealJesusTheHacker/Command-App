package net.sentinelsafeguard.app.ui

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
        return ToolResult(
            sources = sources,
            data = result?.get("data") as? JsonObject,
            recorded = Phase3Logic.int(payload, "recorded"),
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
    fun prettyKey(key: String): String {
        val spaced = key.replace('_', ' ')
            .replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), " ")
            .trim()
        if (spaced.isEmpty()) return key
        return spaced.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
        }
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
                    if (depth >= 3) {
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
                    } else if (depth >= 3) {
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

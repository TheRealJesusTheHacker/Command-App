package net.sentinelsafeguard.app.data.api

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream

/**
 * Phase 7 (hunt11) endpoint methods: the Knowledge group —
 * Knowledge and Archive.
 *
 * Every method rides the deployed Phase 7 backend (Addendum 22;
 * production server.py sha256 ceb3ba33…), whose payloads are the
 * web handlers' own — so these ride the open-JSON verbs on
 * [SentinelApi] exactly like Phases 3–6. Refusals surface the
 * server's own message via [SentinelApi.callJson] — never a fake
 * empty state. Request bodies mirror
 * phase7-work/rehearsal_capture.py field for field.
 *
 * The document file rides the binary twin: the detail payload's
 * `document.url` already points at `/api/app/knowledge/{id}/file`
 * (the backend rewrote the web URL to the app twin), and the
 * client streams it in ranges (see [knowledgeFileBytes]).
 */

private fun JsonObject.toBody7(): String = toString()

private fun SentinelApi.qs7(params: Map<String, String?>): String {
    val parts = params.entries.filter { !it.value.isNullOrBlank() }
        .map { "${it.key}=${encodeQuery(it.value!!)}" }
    return if (parts.isEmpty()) "" else "?" + parts.joinToString("&")
}

/** encodeURIComponent-style encoding for the X-File-Name header
 *  (the server unquotes it; a literal '+' must not survive). */
private fun encodeFileName(name: String): String =
    java.net.URLEncoder.encode(name, "UTF-8").replace("+", "%20")

// ---- Knowledge (web knowledgeView / kbStats / saveknowledge) ----

/**
 * The catalog the Knowledge view renders from: pub_knowledge
 * rows filtered/sorted with the view's own vocabulary (q over
 * title/category/description/uploader, category chip, sorts
 * title/newest/pages/largest), plus kbStats header stats over
 * the COMPLETE catalog and the category counts behind the chips.
 * The defaults are web's (sort Title A–Z, category All), so a
 * plain open sends no query string at all.
 */
suspend fun SentinelApi.knowledgeList(
    q: String = "",
    category: String? = null,
    sort: String? = null,
): JsonObject = callJson(
    "GET", "/api/app/knowledge" + qs7(
        mapOf(
            "q" to q.ifBlank { null },
            "category" to category?.takeIf { it != "All" },
            "sort" to sort?.takeIf { it != "title" },
        )
    )
)

/** One entry + its getknowledgedocument descriptor; the
 *  descriptor's URL is the app file twin (backend-rewritten). */
suspend fun SentinelApi.knowledgeDetail(id: Int): JsonObject =
    callJson("GET", "/api/app/knowledge/$id")

/** The editor save body: saveknowledge fields, with the
 *  audience fields riding along — the backend runs
 *  setknowledgeaudience right after the save, exactly the web
 *  editor's two-step flow. */
fun knowledgeSaveBody(
    title: String,
    category: String,
    description: String,
    audience: String,
    clientIds: List<Int>,
): JsonObject = buildJsonObject {
    put("title", title)
    put("category", category)
    put("description", description)
    put("audience", audience)
    put("clientVisible", audience != "none")
    put("clientIds", JsonArray(clientIds.map { JsonPrimitive(it) }))
}

suspend fun SentinelApi.knowledgeCreate(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/knowledge", body.toBody7())

suspend fun SentinelApi.knowledgeEdit(id: Int, body: JsonObject): JsonObject =
    patchJson("/api/app/knowledge/$id", body.toBody7())

/** The dedicated audience endpoint (web's editor rides the
 *  same setknowledgeaudience handler after a save). */
suspend fun SentinelApi.knowledgeAudience(
    id: Int,
    audience: String,
    clientIds: List<Int>,
): JsonObject = callJson(
    "POST", "/api/app/knowledge/$id/audience",
    buildJsonObject {
        put("audience", audience)
        put("clientVisible", audience != "none")
        put("clientIds", JsonArray(clientIds.map { JsonPrimitive(it) }))
    }.toBody7(),
)

suspend fun SentinelApi.knowledgeDeleteDocument(id: Int): JsonObject =
    deleteJson("/api/app/knowledge/$id/document")

suspend fun SentinelApi.knowledgeDelete(id: Int): JsonObject =
    deleteJson("/api/app/knowledge/$id")

/**
 * The binary upload twin (POST /knowledge/upload's device-token
 * twin): raw PDF bytes with web's X-File-Name header contract.
 * Validation is the server's (%PDF- signature / %%EOF, .pdf name
 * rule, caps) and its refusals surface in the server's own words
 * via [SentinelApi.uploadBytesJson].
 */
suspend fun SentinelApi.knowledgeUpload(
    id: Int,
    bytes: ByteArray,
    fileName: String,
): JsonObject = uploadBytesJson(
    "/api/app/knowledge/$id/upload", bytes,
    mapOf(
        "Content-Type" to "application/pdf",
        "X-File-Name" to encodeFileName(fileName),
    ),
)

/** One ranged chunk of the file twin. [range] is a full Range
 *  header value (e.g. "bytes=0-524287"); a null range is a plain
 *  full download. Returns the HTTP status (200 whole file, 206
 *  partial, 416 unsatisfiable surfaces as an ApiException). */
suspend fun SentinelApi.knowledgeFileChunk(
    url: String,
    range: String?,
    sink: (ByteArray) -> Unit,
): Int = downloadBytes(
    url,
    if (range == null) emptyMap() else mapOf("Range" to range),
    sink,
)

/**
 * The whole book, streamed in ranges: each chunk is requested
 * with a Range header and appended in order, so a 250 MB book
 * never depends on one giant response. A 200 answer (the server
 * sent the whole file at once) ends the walk with that body; a
 * short 206 chunk is the last one.
 */
suspend fun SentinelApi.knowledgeFileBytes(url: String): ByteArray {
    val out = ByteArrayOutputStream()
    var start = 0L
    while (true) {
        val end = start + KNOWLEDGE_RANGE_CHUNK - 1
        val chunk = ByteArrayOutputStream()
        val status = knowledgeFileChunk(url, "bytes=$start-$end") { chunk.write(it) }
        val bytes = chunk.toByteArray()
        out.write(bytes)
        if (status == 200) break
        if (bytes.size < KNOWLEDGE_RANGE_CHUNK) break
        start += bytes.size
    }
    return out.toByteArray()
}

/** The download variant of the file twin (?download=1 — the
 *  server writes its KNOWLEDGE_PDF_DOWNLOADED row only here). */
fun knowledgeDownloadUrl(inlineUrl: String): String =
    inlineUrl + if (inlineUrl.contains("?")) "&download=1" else "?download=1"

const val KNOWLEDGE_RANGE_CHUNK: Long = 512 * 1024

// ---- Archive (web archiveView aggregation) ----

/** Archived records in web's aggregation order — clients, then
 *  cases, then tickets, then assets — each kind present only
 *  when the caller can read its module (the backend's filtered
 *  semantics; a module the caller can't read simply isn't there). */
suspend fun SentinelApi.archiveList(): JsonObject =
    callJson("GET", "/api/app/archive")

/** Restore rides the shared restore writers server-side (a
 *  client restore cascades to the records archived with it,
 *  exactly as web). */
suspend fun SentinelApi.archiveRestore(kind: String, id: Int): JsonObject =
    callJson("POST", "/api/app/archive/$kind/$id/restore", "{}")

/** Permanent delete is owner-only inside the server's delete
 *  writers; the confirm step is the client's, as on web. */
suspend fun SentinelApi.archiveDelete(kind: String, id: Int): JsonObject =
    deleteJson("/api/app/archive/$kind/$id")

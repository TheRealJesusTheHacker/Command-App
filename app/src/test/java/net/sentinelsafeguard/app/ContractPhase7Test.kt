package net.sentinelsafeguard.app

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.config.ApiConfig
import net.sentinelsafeguard.app.data.api.ApiException
import net.sentinelsafeguard.app.data.api.HttpRequest
import net.sentinelsafeguard.app.data.api.HttpResponse
import net.sentinelsafeguard.app.data.api.HttpTransport
import net.sentinelsafeguard.app.data.api.SentinelApi
import net.sentinelsafeguard.app.data.api.SignedOutException
import net.sentinelsafeguard.app.data.api.archiveDelete
import net.sentinelsafeguard.app.data.api.archiveList
import net.sentinelsafeguard.app.data.api.archiveRestore
import net.sentinelsafeguard.app.data.api.knowledgeAudience
import net.sentinelsafeguard.app.data.api.knowledgeCreate
import net.sentinelsafeguard.app.data.api.knowledgeDelete
import net.sentinelsafeguard.app.data.api.knowledgeDeleteDocument
import net.sentinelsafeguard.app.data.api.knowledgeDetail
import net.sentinelsafeguard.app.data.api.knowledgeDownloadUrl
import net.sentinelsafeguard.app.data.api.knowledgeEdit
import net.sentinelsafeguard.app.data.api.knowledgeFileBytes
import net.sentinelsafeguard.app.data.api.knowledgeFileChunk
import net.sentinelsafeguard.app.data.api.knowledgeList
import net.sentinelsafeguard.app.data.api.knowledgeSaveBody
import net.sentinelsafeguard.app.data.api.knowledgeUpload
import net.sentinelsafeguard.app.ui.Phase7Logic
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Phase 7 (hunt11) contract shapes: every Phase 7 endpoint
 * driven through the app's API methods against REAL bodies
 * captured from the deployed Phase 7 backend (see
 * [RealBodiesPhase7]; Addendum 22) — including the read-gate
 * 403 bodies, the handlers' business refusals in the server's
 * own words, the binary upload twin, and the file twin's
 * range behavior.
 */
class ContractPhase7Test {

    private class QueueTransport(vararg val steps: Pair<Int, String>) : HttpTransport {
        val requests = mutableListOf<HttpRequest>()
        private var index = 0
        override suspend fun execute(request: HttpRequest): HttpResponse {
            requests += request
            val step = steps[minOf(index, steps.size - 1)]
            index++
            return HttpResponse(step.first, step.second)
        }

        override suspend fun download(
            request: HttpRequest,
            sink: (ByteArray) -> Unit,
        ): HttpResponse {
            requests += request
            val step = steps[minOf(index, steps.size - 1)]
            index++
            if (step.first in 200..299) sink(step.second.toByteArray())
            return HttpResponse(step.first, "")
        }
    }

    /** Serves one in-memory "book" over the file twin's Range
     *  protocol, the way the deployed twin does (206 partial,
     *  200 whole, 416 unsatisfiable). */
    private class RangeTransport(val data: ByteArray) : HttpTransport {
        val requests = mutableListOf<HttpRequest>()
        override suspend fun execute(request: HttpRequest): HttpResponse =
            HttpResponse(500, "{}")

        override suspend fun download(
            request: HttpRequest,
            sink: (ByteArray) -> Unit,
        ): HttpResponse {
            requests += request
            val range = request.headers["Range"] ?: run {
                sink(data)
                return HttpResponse(200, "")
            }
            val m = Regex("bytes=(\\d+)-(\\d+)").find(range)!!
            val start = m.groupValues[1].toInt()
            val end = m.groupValues[2].toInt()
            if (start >= data.size) return HttpResponse(416, "")
            val last = minOf(end, data.size - 1)
            sink(data.copyOfRange(start, last + 1))
            return HttpResponse(206, "")
        }
    }

    private fun api(transport: HttpTransport): SentinelApi =
        SentinelApi(ApiConfig("https://command.example.test", "", ""), transport)

    private fun step(name: String): Pair<Int, String> =
        RealBodiesPhase7.status(name) to RealBodiesPhase7.body(name)

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.content

    private fun JsonObject.int(key: String): Int? =
        (this[key] as? JsonPrimitive)?.content?.toIntOrNull()

    private fun JsonObject.bool(key: String): Boolean? =
        (this[key] as? JsonPrimitive)?.content?.toBooleanStrictOrNull()

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.arr(key: String): List<JsonObject> =
        (this[key] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

    // ---- Gates ----

    @Test
    fun flagOffKnowledgeAndArchiveRefuseInServerWords() = runBlocking {
        try {
            api(QueueTransport(step("GET /api/app/knowledge (flag off)"))).knowledgeList()
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(403, e.status)
            assertEquals("Not authorized.", e.message)
        }
        try {
            api(QueueTransport(step("GET /api/app/archive (flag off)"))).archiveList()
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(403, e.status)
            assertEquals("Not authorized.", e.message)
        }
    }

    @Test
    fun unauthenticatedKnowledgeIs401() = runBlocking {
        try {
            api(QueueTransport(step("GET /api/app/knowledge (unauthenticated)"))).knowledgeList()
            fail("expected a SignedOutException")
        } catch (e: SignedOutException) {
            assertEquals("Your sign-in has ended. Please sign in again.", e.message)
        }
        val fileRefusal = RealBodiesPhase7.bodyJson("GET /api/app/knowledge/{id}/file (unauthenticated)")
        assertEquals("Sign-in required.", fileRefusal.str("message"))
        assertEquals(
            401,
            RealBodiesPhase7.status("GET /api/app/knowledge/{id}/file (unauthenticated)"),
        )
    }

    // ---- Knowledge catalog ----

    @Test
    fun knowledgeListParsesStatsCategoriesAndEntries() = runBlocking {
        val transport = QueueTransport(step("GET /api/app/knowledge"))
        val out = api(transport).knowledgeList()
        assertEquals("GET", transport.requests[0].method)
        assertEquals("/api/app/knowledge", transport.requests[0].path)
        assertEquals(2, out.int("total"))
        val stats = out.obj("stats")!!
        assertEquals(2, stats.int("total"))
        assertEquals(0, stats.int("readyToRead"))
        assertEquals(2, stats.int("awaitingFile"))
        assertEquals("—", Phase7Logic.totalPagesLabel(stats))
        val cats = out.arr("categories")
        assertEquals(listOf("Compliance", "Reference"), cats.map { it.str("category") })
        assertEquals(listOf(1, 1), cats.map { it.int("count") })
        val entries = out.arr("entries")
        assertEquals("Capture Alpha Guide", entries[0].str("title"))
        assertEquals(false, entries[0].bool("hasFile"))
        assertEquals("Catalog entry — no PDF housed yet.", Phase7Logic.entryMeta(entries[0]))
        assertEquals("Internal only", Phase7Logic.audienceChip(entries[0]))

        // STAFF reads the same catalog (the web workspace rule).
        val staff = api(QueueTransport(step("GET /api/app/knowledge (staff)"))).knowledgeList()
        assertEquals(2, staff.int("total"))
    }

    @Test
    fun knowledgeSearchRidesQueryAndStatsStayComplete() = runBlocking {
        val transport = QueueTransport(step("GET /api/app/knowledge?q=alpha"))
        val out = api(transport).knowledgeList(q = "alpha")
        assertEquals("/api/app/knowledge?q=alpha", transport.requests[0].path)
        assertEquals(1, out.int("total"))
        assertEquals("Capture Alpha Guide", out.arr("entries")[0].str("title"))
        // kbStats ride over the COMPLETE catalog, not the filter.
        assertEquals(2, out.obj("stats")!!.int("total"))
    }

    @Test
    fun knowledgeCategoryAndSortRideQueryParams() = runBlocking {
        val catT = QueueTransport(step("GET /api/app/knowledge?category=Compliance"))
        val cat = api(catT).knowledgeList(category = "Compliance")
        assertEquals("/api/app/knowledge?category=Compliance", catT.requests[0].path)
        assertEquals("Capture Beta Standard", cat.arr("entries")[0].str("title"))

        val sortT = QueueTransport(step("GET /api/app/knowledge?sort=newest"))
        val sorted = api(sortT).knowledgeList(sort = "newest")
        assertEquals("/api/app/knowledge?sort=newest", sortT.requests[0].path)
        assertEquals(
            listOf("Capture Beta Standard", "Capture Alpha Guide"),
            sorted.arr("entries").map { it.str("title") },
        )
    }

    @Test
    fun knowledgeBadSortRefusesInServerWords() = runBlocking {
        try {
            api(QueueTransport(step("GET /api/app/knowledge?sort=bogus (refusal)")))
                .knowledgeList(sort = "bogus")
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(403, e.status)
            assertEquals("Unknown sort.", e.message)
        }
    }

    // ---- Knowledge detail / reader ----

    @Test
    fun knowledgeDetailNoFileAndFiledShapes() = runBlocking {
        val noFile = api(QueueTransport(step("GET /api/app/knowledge/{id} (no file)")))
            .knowledgeDetail(1)
        assertEquals("Capture Alpha Guide", noFile.obj("entry")!!.str("title"))
        val doc = noFile.obj("document")!!
        assertEquals("No PDF has been added to this entry yet.", doc.str("message"))
        assertEquals("null", doc["url"]!!.toString())

        val filed = api(QueueTransport(step("GET /api/app/knowledge/{id}"))).knowledgeDetail(1)
        val filedDoc = filed.obj("document")!!
        // The descriptor URL is the app file twin, rewritten by
        // the backend from the web URL.
        assertEquals("/api/app/knowledge/1/file", filedDoc.str("url"))
        assertEquals("Ready to read.", filedDoc.str("message"))
        assertEquals("alpha.pdf", filedDoc.str("fileName"))
        val entry = filed.obj("entry")!!
        assertEquals(true, entry.bool("hasFile"))
        assertEquals("OwnerCap7", entry.str("uploadedByName"))
        assertTrue(Phase7Logic.entryMeta(entry).contains("1 pages"))
        assertTrue(Phase7Logic.entryMeta(entry).contains("218 B"))
        assertEquals(
            "/api/app/knowledge/1/file?download=1",
            knowledgeDownloadUrl(filedDoc.str("url")!!),
        )
    }

    @Test
    fun knowledgeDetailMissingIs404() = runBlocking {
        try {
            api(QueueTransport(step("GET /api/app/knowledge/{id} (missing)"))).knowledgeDetail(999999)
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(404, e.status)
            assertEquals("Not found.", e.message)
        }
    }

    // ---- Knowledge writes ----

    @Test
    fun knowledgeCreateParsesAndSendsFields() = runBlocking {
        val transport = QueueTransport(step("POST /api/app/knowledge"))
        val out = api(transport).knowledgeCreate(buildJsonObject {
            put("title", "Capture Delta Manual")
            put("category", "Field Practice")
            put("description", "Created in capture.")
        })
        assertEquals("POST", transport.requests[0].method)
        assertEquals("/api/app/knowledge", transport.requests[0].path)
        assertTrue(transport.requests[0].body!!.contains("Capture Delta Manual"))
        assertEquals(3, out.int("id"))
        assertEquals("Knowledge entry saved.", out.str("message"))
        assertEquals("Capture Delta Manual", out.obj("entry")!!.str("title"))
    }

    @Test
    fun knowledgeCreateBlankTitleRefusesInServerWords() = runBlocking {
        try {
            api(QueueTransport(step("POST /api/app/knowledge (blank title)")))
                .knowledgeCreate(buildJsonObject { put("title", " ") })
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(403, e.status)
            assertEquals("Title is required.", e.message)
        }
    }

    @Test
    fun knowledgePatchIsSeededSave() = runBlocking {
        val transport = QueueTransport(step("PATCH /api/app/knowledge/{id}"))
        val out = api(transport).knowledgeEdit(3, buildJsonObject {
            put("title", "Capture Delta Renamed")
        })
        assertEquals("PATCH", transport.requests[0].method)
        assertEquals("/api/app/knowledge/3", transport.requests[0].path)
        assertEquals("Capture Delta Renamed", out.obj("entry")!!.str("title"))
        // Seeded from the stored row: untouched fields survive.
        assertEquals("Created in capture.", out.obj("entry")!!.str("description"))
    }

    @Test
    fun knowledgeCreateWithAudienceRidesCombinedBody() = runBlocking {
        val transport = QueueTransport(step("POST /api/app/knowledge (with audience)"))
        val out = api(transport).knowledgeCreate(
            knowledgeSaveBody("Capture Epsilon Combined", "Reference", "Editor flow.", "none", emptyList())
        )
        val body = transport.requests[0].body!!
        assertTrue(body.contains("\"audience\":\"none\"") || body.contains("\"audience\": \"none\"") || body.contains("audience"))
        assertEquals(4, out.int("id"))
        assertEquals("Capture Epsilon Combined", out.obj("entry")!!.str("title"))
    }

    @Test
    fun knowledgeAudienceEndpointRefusalAndSuccess() = runBlocking {
        try {
            api(QueueTransport(step("POST /api/app/knowledge/{id}/audience (refusal)")))
                .knowledgeAudience(3, "bogus", emptyList())
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(403, e.status)
            assertEquals("Audience must be none, all, or specific.", e.message)
        }
        val transport = QueueTransport(step("POST /api/app/knowledge/{id}/audience"))
        val out = api(transport).knowledgeAudience(3, "all", emptyList())
        assertEquals("/api/app/knowledge/3/audience", transport.requests[0].path)
        assertEquals("Knowledge audience updated.", out.str("message"))
        assertEquals(true, out.bool("clientVisible"))
        assertEquals("all", out.str("audience"))
    }

    @Test
    fun knowledgeDocumentAndEntryDelete() = runBlocking {
        val docT = QueueTransport(step("DELETE /api/app/knowledge/{id}/document"))
        val doc = api(docT).knowledgeDeleteDocument(1)
        assertEquals("DELETE", docT.requests[0].method)
        assertEquals("/api/app/knowledge/1/document", docT.requests[0].path)
        assertEquals("Knowledge document removed.", doc.str("message"))

        val del = api(QueueTransport(step("DELETE /api/app/knowledge/{id}"))).knowledgeDelete(3)
        assertEquals("Knowledge entry deleted.", del.str("message"))
    }

    // ---- Binary upload twin ----

    private val fakePdf: ByteArray =
        ("%PDF-1.4 capture\n" + "x".repeat(180) + "\n%%EOF\n").toByteArray()

    @Test
    fun knowledgeUploadSendsBytesAndFileNameHeader() = runBlocking {
        val transport = QueueTransport(step("POST /api/app/knowledge/{id}/upload"))
        val out = api(transport).knowledgeUpload(1, fakePdf, "alpha.pdf")
        val request = transport.requests[0]
        assertEquals("POST", request.method)
        assertEquals("/api/app/knowledge/1/upload", request.path)
        assertEquals("alpha.pdf", request.headers["X-File-Name"])
        assertArrayEquals(fakePdf, request.bodyBytes)
        assertEquals("PDF stored — the book is ready to read.", out.str("message"))
        assertEquals("alpha.pdf", out.str("fileName"))
        assertEquals(218, out.int("fileSize"))
        assertEquals(1, out.int("pageCount"))
    }

    @Test
    fun knowledgeUploadRefusalsSurfaceInServerWords() = runBlocking {
        try {
            api(QueueTransport(step("POST /api/app/knowledge/{id}/upload (not a PDF)")))
                .knowledgeUpload(1, "junk".toByteArray(), "alpha.pdf")
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(400, e.status)
            assertEquals("That file is not a complete PDF.", e.message)
        }
        try {
            api(QueueTransport(step("POST /api/app/knowledge/{id}/upload (staff refusal)")))
                .knowledgeUpload(1, fakePdf, "alpha.pdf")
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(403, e.status)
            assertEquals("Not authorized.", e.message)
        }
    }

    // ---- Binary file twin ----

    @Test
    fun knowledgeFileDescriptorsPinStatusesAndRanges() {
        val inline = RealBodiesPhase7.bodyJson("GET /api/app/knowledge/{id}/file (inline)")
        assertEquals(200, RealBodiesPhase7.status("GET /api/app/knowledge/{id}/file (inline)"))
        assertEquals(218, inline.int("size"))
        assertEquals(
            "a6946076beaeb23e666cc3e268cac0b34c2c281d1497aaf7952609d31043199f",
            inline.str("sha256"),
        )
        assertTrue(inline.str("contentDisposition")!!.startsWith("inline;"))

        val download = RealBodiesPhase7.bodyJson("GET /api/app/knowledge/{id}/file?download=1")
        assertTrue(download.str("contentDisposition")!!.startsWith("attachment;"))

        val range = RealBodiesPhase7.bodyJson("GET /api/app/knowledge/{id}/file (range)")
        assertEquals(206, RealBodiesPhase7.status("GET /api/app/knowledge/{id}/file (range)"))
        assertEquals(8, range.int("size"))
        assertEquals("bytes 0-7/218", range.str("contentRange"))

        val unsat = RealBodiesPhase7.bodyJson("GET /api/app/knowledge/{id}/file (range unsatisfiable)")
        assertEquals(416, RealBodiesPhase7.status("GET /api/app/knowledge/{id}/file (range unsatisfiable)"))
        assertEquals("bytes */218", unsat.str("contentRange"))

        val noPdf = RealBodiesPhase7.bodyJson("GET /api/app/knowledge/{id}/file (no PDF)")
        assertEquals(404, RealBodiesPhase7.status("GET /api/app/knowledge/{id}/file (no PDF)"))
        assertEquals("No PDF has been added to this entry yet.", noPdf.str("message"))
    }

    @Test
    fun knowledgeFileBytesReassembleAcrossRanges() = runBlocking {
        // A book larger than one range chunk: the client must
        // walk ranges and reassemble it byte for byte.
        val book = ByteArray(600 * 1024) { (it % 251).toByte() }
        "%PDF-".toByteArray().copyInto(book)
        val transport = RangeTransport(book)
        val out = api(transport).knowledgeFileBytes("/api/app/knowledge/1/file")
        assertArrayEquals(book, out)
        assertEquals(2, transport.requests.size)
        assertEquals("bytes=0-524287", transport.requests[0].headers["Range"])
        assertEquals("bytes=524288-1048575", transport.requests[1].headers["Range"])
        assertTrue(transport.requests.all { it.path == "/api/app/knowledge/1/file" })
    }

    @Test
    fun knowledgeFileChunkUnsatisfiableIs416() = runBlocking {
        val transport = RangeTransport(ByteArray(218) { 1 })
        try {
            api(transport).knowledgeFileChunk("/api/app/knowledge/1/file", "bytes=99999-999999") {}
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(416, e.status)
        }
    }

    @Test
    fun knowledgeFileDownloadMissingPdfIs404() = runBlocking {
        val transport = QueueTransport(404 to RealBodiesPhase7.body("GET /api/app/knowledge/{id}/file (no PDF)"))
        try {
            api(transport).knowledgeFileBytes("/api/app/knowledge/2/file")
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(404, e.status)
        }
    }

    // ---- Archive ----

    @Test
    fun archiveListKeepsWebAggregationOrder() = runBlocking {
        val transport = QueueTransport(step("GET /api/app/archive"))
        val out = api(transport).archiveList()
        assertEquals("/api/app/archive", transport.requests[0].path)
        assertEquals(4, out.int("total"))
        val rows = out.arr("rows")
        // Web's order: clients, then cases, then tickets, then assets.
        assertEquals(
            listOf("client", "case", "ticket", "asset"),
            rows.map { it.str("kind") },
        )
        assertEquals(
            listOf(
                "Capture Archive Client", "Capture Archive Case",
                "Capture Archive Ticket", "Capture Archive Asset",
            ),
            rows.map { it.str("name") },
        )
        assertEquals("client/1", Phase7Logic.archiveRouteFor("client", 1))
        assertEquals("asset/1", Phase7Logic.archiveRouteFor("asset", 1))
        assertTrue(Phase7Logic.archiveRowMeta(rows[0]).contains("Lead"))
        assertTrue(Phase7Logic.archiveRowMeta(rows[3]).contains("In Service"))

        val empty = api(QueueTransport(step("GET /api/app/archive (empty)"))).archiveList()
        assertEquals(0, empty.int("total"))
        assertTrue(empty.arr("rows").isEmpty())
    }

    @Test
    fun archiveRestoresRidePerKindWriters() = runBlocking {
        val ticketT = QueueTransport(step("POST /api/app/archive/ticket/{id}/restore"))
        val ticket = api(ticketT).archiveRestore("ticket", 1)
        assertEquals("/api/app/archive/ticket/1/restore", ticketT.requests[0].path)
        assertEquals("Ticket restored.", ticket.str("message"))

        val case = api(QueueTransport(step("POST /api/app/archive/case/{id}/restore")))
            .archiveRestore("case", 1)
        assertEquals("Case restored.", case.str("message"))

        val asset = api(QueueTransport(step("POST /api/app/archive/asset/{id}/restore")))
            .archiveRestore("asset", 1)
        assertEquals("Asset restored.", asset.str("message"))

        // The client restore cascades server-side (the records
        // archived with the client revive with it), exactly as
        // web's restoreclient does.
        val client = api(QueueTransport(step("POST /api/app/archive/client/{id}/restore")))
            .archiveRestore("client", 1)
        assertEquals("Client restored.", client.str("message"))
        assertEquals("Capture Archive Client", client.obj("client")!!.str("legalName"))
        assertEquals("null", client.obj("client")!!["archivedAt"]!!.toString())
    }

    @Test
    fun archiveDeleteAndNotFoundShapes() = runBlocking {
        val delT = QueueTransport(step("DELETE /api/app/archive/ticket/{id}"))
        val deleted = api(delT).archiveDelete("ticket", 1)
        assertEquals("DELETE", delT.requests[0].method)
        assertEquals("/api/app/archive/ticket/1", delT.requests[0].path)
        assertEquals("Ticket deleted.", deleted.str("message"))

        try {
            api(QueueTransport(step("DELETE /api/app/archive/ticket/{id} (missing)")))
                .archiveDelete("ticket", 999999)
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(404, e.status)
            assertEquals("Not found.", e.message)
        }
        try {
            api(QueueTransport(step("POST /api/app/archive/widget/{id}/restore (unknown kind)")))
                .archiveRestore("widget", 1)
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(404, e.status)
            assertEquals("Not found.", e.message)
        }
    }

    // ---- Pure logic pins ----

    @Test
    fun phase7LogicPins() {
        // kbSize, web's formatting exactly.
        assertEquals("0 B", Phase7Logic.kbSize(0))
        assertEquals("218 B", Phase7Logic.kbSize(218))
        assertEquals("1.5 KB", Phase7Logic.kbSize(1536))
        assertEquals("1.0 MB", Phase7Logic.kbSize(1024 * 1024))
        assertEquals("2.50 GB", Phase7Logic.kbSize(2684354560L))

        // Total pages: dash when nothing is ready, "+" while a
        // ready book's count is unknown.
        val noneReady = RealBodiesPhase7.bodyJson("GET /api/app/knowledge").obj("stats")!!
        assertEquals("—", Phase7Logic.totalPagesLabel(noneReady))

        // kbCheckFile, web's messages.
        assertEquals("Choose a PDF file.", Phase7Logic.checkFile(null, 10))
        assertEquals("Choose a PDF file.", Phase7Logic.checkFile("notes.txt", 10))
        assertEquals("That file is empty.", Phase7Logic.checkFile("a.pdf", 0))
        assertEquals(
            "That PDF is larger than 250 MB.",
            Phase7Logic.checkFile("a.pdf", 250L * 1024 * 1024 + 1),
        )
        assertEquals(null, Phase7Logic.checkFile("a.pdf", 218))

        // Sort + audience vocabularies round-trip.
        assertEquals("Most pages", Phase7Logic.sortLabel("pages"))
        assertEquals("largest", Phase7Logic.sortKey("Largest first"))
        assertEquals("all", Phase7Logic.audienceValue("All clients — self-help library"))
        assertEquals(
            "Internal only — clients never see it",
            Phase7Logic.audienceLabel("none"),
        )
        assertEquals(5, Phase7Logic.CATEGORIES.size)
        assertEquals("Codes & Standards", Phase7Logic.CATEGORIES[0])
    }
}

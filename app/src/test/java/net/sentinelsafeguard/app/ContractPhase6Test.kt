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
import net.sentinelsafeguard.app.data.api.breachCirculationCreate
import net.sentinelsafeguard.app.data.api.breachCirculationDelete
import net.sentinelsafeguard.app.data.api.breachCreate
import net.sentinelsafeguard.app.data.api.breachDelete
import net.sentinelsafeguard.app.data.api.breachEdit
import net.sentinelsafeguard.app.data.api.breachIntel
import net.sentinelsafeguard.app.data.api.breachIntakeRun
import net.sentinelsafeguard.app.data.api.lawDocCreate
import net.sentinelsafeguard.app.data.api.lawDocDelete
import net.sentinelsafeguard.app.data.api.lawDocEdit
import net.sentinelsafeguard.app.data.api.lawDocgen
import net.sentinelsafeguard.app.data.api.lawResearch
import net.sentinelsafeguard.app.data.api.lawWorkspace
import net.sentinelsafeguard.app.data.api.liveFlightLookup
import net.sentinelsafeguard.app.data.api.liveFlights
import net.sentinelsafeguard.app.data.api.liveGeoData
import net.sentinelsafeguard.app.data.api.liveThreatData
import net.sentinelsafeguard.app.data.api.newsBulkDeleteDrafts
import net.sentinelsafeguard.app.data.api.newsCreate
import net.sentinelsafeguard.app.data.api.newsDelete
import net.sentinelsafeguard.app.data.api.newsEdit
import net.sentinelsafeguard.app.data.api.newsFeed
import net.sentinelsafeguard.app.data.api.newsItems
import net.sentinelsafeguard.app.data.api.newsPublish
import net.sentinelsafeguard.app.data.api.newsUnpublish
import net.sentinelsafeguard.app.data.api.osintCreateSearch
import net.sentinelsafeguard.app.data.api.osintCreateWatch
import net.sentinelsafeguard.app.data.api.osintDeleteReport
import net.sentinelsafeguard.app.data.api.osintDeleteWatch
import net.sentinelsafeguard.app.data.api.osintDetect
import net.sentinelsafeguard.app.data.api.osintReport
import net.sentinelsafeguard.app.data.api.osintRunWatchNow
import net.sentinelsafeguard.app.data.api.osintWorkspace
import net.sentinelsafeguard.app.data.api.reviewFeatured
import net.sentinelsafeguard.app.data.api.reviewHide
import net.sentinelsafeguard.app.data.api.reviewPublish
import net.sentinelsafeguard.app.data.api.reviewResponsePublish
import net.sentinelsafeguard.app.data.api.reviewResponseSave
import net.sentinelsafeguard.app.data.api.reviewResponseUnpublish
import net.sentinelsafeguard.app.data.api.reviewResponses
import net.sentinelsafeguard.app.data.api.reviews
import net.sentinelsafeguard.app.data.api.threatIntel
import net.sentinelsafeguard.app.data.api.threatPushToNews
import net.sentinelsafeguard.app.data.api.threatSync
import net.sentinelsafeguard.app.ui.Phase6Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Phase 6 (hunt10) contract shapes: every Phase 6 endpoint
 * driven through the app's API methods against REAL bodies
 * captured from the deployed Phase 6 backend (see
 * [RealBodiesPhase6]; Addendum 20) — including every view's
 * flag-off 403 body and the handlers' business refusals, which
 * must reach the UI in the server's own words.
 */
class ContractPhase6Test {

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
            sink(step.second.toByteArray())
            return HttpResponse(step.first, "")
        }
    }

    private fun api(transport: HttpTransport): SentinelApi =
        SentinelApi(ApiConfig("https://command.example.test", "", ""), transport)

    private fun step(name: String): Pair<Int, String> =
        RealBodiesPhase6.status(name) to RealBodiesPhase6.body(name)

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.content

    private fun JsonObject.int(key: String): Int? =
        (this[key] as? JsonPrimitive)?.content?.toIntOrNull()

    private fun JsonObject.bool(key: String): Boolean? =
        (this[key] as? JsonPrimitive)?.content?.toBooleanStrictOrNull()

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.arr(key: String): List<JsonObject> =
        (this[key] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

    private fun refusal(name: String, call: suspend (SentinelApi) -> JsonObject) = runBlocking {
        try {
            call(api(QueueTransport(step(name))))
            fail("expected an ApiException for $name")
        } catch (e: ApiException) {
            assertEquals(403, e.status)
            assertEquals("Not authorized.", e.message)
        }
    }

    // ---- Flag gates: every view's 403 in the server's words ----

    @Test
    fun flagOffRefusalsSurfaceServerWordsForEveryView() {
        refusal("GET /api/app/live-intel/threat-data (flag off)") { it.liveThreatData() }
        refusal("GET /api/app/osint (flag off)") { it.osintWorkspace() }
        refusal("GET /api/app/breach-intel (flag off)") { it.breachIntel() }
        refusal("GET /api/app/threat-intel (flag off)") { it.threatIntel() }
        refusal("GET /api/app/news/feed (flag off)") { it.newsFeed() }
        refusal("GET /api/app/news/items (flag off)") { it.newsItems() }
        refusal("GET /api/app/reviews (flag off)") { it.reviews() }
        refusal("GET /api/app/law (flag off)") { it.lawWorkspace() }
    }

    @Test
    fun unauthenticatedLiveIntelIs401() = runBlocking {
        // A 401 is the client's sign-in-required path:
        // SignedOutException carries the server's own words.
        try {
            api(QueueTransport(step("GET /api/app/live-intel/threat-data (unauthenticated)")))
                .liveThreatData()
            fail("expected a SignedOutException")
        } catch (e: SignedOutException) {
            assertEquals("Sign-in required.", e.message)
        }
    }

    // ---- Live Intel ----

    @Test
    fun liveIntelSurfacesParse() = runBlocking {
        val threatT = QueueTransport(step("GET /api/app/live-intel/threat-data"))
        val threat = api(threatT).liveThreatData()
        assertEquals("/api/app/live-intel/threat-data", threatT.requests[0].path)
        assertEquals(true, threat.bool("connected"))
        assertEquals("phase6-threat", threat.obj("data")!!.str("marker"))
        assertEquals(42, threat.obj("data")!!.int("attacks"))

        val geo = api(QueueTransport(step("GET /api/app/live-intel/geo-data"))).liveGeoData()
        assertEquals("phase6-geo", geo.obj("data")!!.str("marker"))

        val flights = api(QueueTransport(step("GET /api/app/live-intel/flights"))).liveFlights()
        assertEquals("NSEED6", flights.obj("data")!!.arr("flights")[0].str("tail"))
    }

    @Test
    fun flightLookupRidesQueryAndSurfacesWebErrorLine() = runBlocking {
        val transport = QueueTransport(step("GET /api/app/live-intel/flight-lookup"))
        val out = api(transport).liveFlightLookup("AA100")
        assertTrue(transport.requests[0].path.contains("flightNumber=AA100"))
        assertEquals("Seed airborne", out.obj("result")!!.str("status"))

        val invalid = api(QueueTransport(step("GET /api/app/live-intel/flight-lookup (invalid)")))
            .liveFlightLookup("!!!")
        assertEquals(
            "Enter a flight number (e.g. AA100, UAL512, SWA204).",
            invalid.str("error"),
        )
    }

    // ---- OSINT ----

    @Test
    fun osintWorkspaceParsesTypesSearchesReportsWatches() = runBlocking {
        val empty = api(QueueTransport(step("GET /api/app/osint (empty)"))).osintWorkspace()
        assertEquals(11, empty.arr("types").size)
        assertEquals("email", empty.arr("types")[2].str("key"))
        assertTrue(empty.arr("searches").isEmpty())

        val seeded = api(QueueTransport(step("GET /api/app/osint"))).osintWorkspace()
        assertEquals(2, seeded.arr("searches").size)
        assertEquals("Capture Seed Report", seeded.arr("reports")[0].str("title"))
        assertEquals("Seed watch", seeded.arr("watches")[0].str("label"))
        assertNotNull(seeded.obj("dorkTemplates"))
        assertNotNull(seeded.obj("checklists"))
    }

    @Test
    fun osintDetectAndSearchCreate() = runBlocking {
        val detectT = QueueTransport(step("GET /api/app/osint/detect"))
        val detect = api(detectT).osintDetect("seed@example.invalid")
        assertTrue(detectT.requests[0].path.contains("/api/app/osint/detect"))
        assertTrue(detect["candidates"]!!.toString().contains("email"))

        val createT = QueueTransport(step("POST /api/app/osint/searches"))
        val created = api(createT).osintCreateSearch(buildJsonObject {
            put("purpose", "Capture seed")
            put("identifiers", kotlinx.serialization.json.JsonArray(listOf(buildJsonObject {
                put("type", "email"); put("value", "seed@example.invalid")
            })))
        })
        assertEquals("POST", createT.requests[0].method)
        assertEquals(1, created.int("searchId"))
    }

    @Test
    fun osintSearchWithoutPurposeRefusesInServerWords() = runBlocking {
        try {
            api(QueueTransport(step("POST /api/app/osint/searches (no purpose)")))
                .osintCreateSearch(buildJsonObject {
                    put("identifiers", kotlinx.serialization.json.JsonArray(listOf(buildJsonObject {
                        put("type", "email"); put("value", "seed@example.invalid")
                    })))
                })
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(403, e.status)
            assertTrue(e.message!!.contains("A purpose note is required"))
        }
    }

    @Test
    fun osintReportAndWatchLifecycle() = runBlocking {
        val report = api(QueueTransport(step("GET /api/app/osint/reports/{id}"))).osintReport(1)
        assertEquals("Capture Seed Report", report.obj("report")!!.str("title"))
        assertEquals("Seed body", report.obj("report")!!.str("body"))

        val watch = api(QueueTransport(step("POST /api/app/osint/watches")))
            .osintCreateWatch(buildJsonObject { put("purpose", "Capture watch") })
        assertEquals(1, watch.int("watchId"))

        val runT = QueueTransport(step("POST /api/app/osint/watches/{id}/run-now"))
        val ran = api(runT).osintRunWatchNow(1)
        assertEquals("/api/app/osint/watches/1/run-now", runT.requests[0].path)
        assertEquals(2, ran.int("searchId"))

        val delWatch = api(QueueTransport(step("DELETE /api/app/osint/watches/{id}"))).osintDeleteWatch(1)
        assertEquals(true, delWatch.bool("ok"))
        val delReport = api(QueueTransport(step("DELETE /api/app/osint/reports/{id}"))).osintDeleteReport(1)
        assertEquals(true, delReport.bool("ok"))
    }

    // ---- Breach Intel ----

    @Test
    fun breachWorkspaceParsesVocabStatsAndDetail() = runBlocking {
        val empty = api(QueueTransport(step("GET /api/app/breach-intel (empty)"))).breachIntel()
        assertEquals(0, empty.obj("stats")!!.int("totalBreaches"))
        assertEquals(8, (empty["venueTypes"] as JsonArray).size)
        assertTrue(empty.str("boundaryCopy")!!.contains("Never enter a link to the leak"))

        val filteredT = QueueTransport(step("GET /api/app/breach-intel?sector=technology"))
        val filtered = api(filteredT).breachIntel(sector = "technology")
        assertTrue(filteredT.requests[0].path.contains("sector=technology"))
        assertEquals(1, filtered.arr("breaches").size)

        val detail = api(QueueTransport(step("GET /api/app/breach-intel?breachId={id}")))
            .breachIntel(breachId = 1)
        val d = detail.obj("detail")!!
        assertEquals("Capture Fictional Breach Co", d.str("org"))
        assertEquals("Seed reporting", d.arr("sources")[0].str("label"))
        assertEquals(123, d.int("recordsAffected"))
    }

    @Test
    fun breachCrudAndIntake() = runBlocking {
        val createT = QueueTransport(step("POST /api/app/breach-intel"))
        val created = api(createT).breachCreate(buildJsonObject {
            put("org", "Capture Fictional Breach Co"); put("sector", "technology")
        })
        assertEquals(1, created.int("breachId"))

        val edited = api(QueueTransport(step("PATCH /api/app/breach-intel/{id}")))
            .breachEdit(1, buildJsonObject { put("recordsAffected", 456) })
        assertEquals(1, edited.int("breachId"))

        val intake = api(QueueTransport(step("POST /api/app/breach-intel/intake-run"))).breachIntakeRun()
        assertNotNull(intake["runId"])

        val deleted = api(QueueTransport(step("DELETE /api/app/breach-intel/{id}"))).breachDelete(1)
        assertEquals(true, deleted.bool("ok"))
    }

    @Test
    fun breachCirculationRefusesVenueLinksInServerWords() = runBlocking {
        try {
            api(QueueTransport(step("POST /api/app/breach-intel/{id}/circulation (link refusal)")))
                .breachCirculationCreate(1, buildJsonObject {
                    put("venueType", "dark-web forum")
                    put("venueName", "https://evil.example.invalid/leak")
                })
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(403, e.status)
            assertTrue(e.message!!.contains("Venue links are never stored"))
        }
        val created = api(QueueTransport(step("POST /api/app/breach-intel/{id}/circulation")))
            .breachCirculationCreate(1, buildJsonObject {
                put("venueType", "dark-web forum"); put("venueName", "Seed Forum Name")
            })
        assertNotNull(created["circulationId"])
        val deleted = api(QueueTransport(step("DELETE /api/app/breach-intel/circulation/{id}")))
            .breachCirculationDelete(1)
        assertEquals(true, deleted.bool("ok"))
    }

    // ---- Threat Intel ----

    @Test
    fun threatFeedAndDetailParse() = runBlocking {
        val feed = api(QueueTransport(step("GET /api/app/threat-intel"))).threatIntel()
        val stats = feed.obj("stats")!!
        assertEquals(52, stats.int("total"))
        assertEquals(1, stats.int("critical"))
        assertEquals("Capture Fictional CVE Item", feed.arr("items")[0].str("title"))
        assertEquals(90, feed.arr("items")[0].int("score"))
        assertTrue(feed.arr("sources").isNotEmpty())
        assertEquals(6, (feed["categories"] as JsonArray).size)

        val detailT = QueueTransport(step("GET /api/app/threat-intel?itemId={id}"))
        val detail = api(detailT).threatIntel(itemId = 52)
        assertTrue(detailT.requests[0].path.contains("itemId=52"))
        assertEquals("Capture Fictional CVE Item", detail.obj("detail")!!.str("title"))
    }

    @Test
    fun threatSyncAndPushToNews() = runBlocking {
        val sync = api(QueueTransport(step("POST /api/app/threat-intel/sync"))).threatSync()
        assertTrue(sync.str("message")!!.contains("Threat Intel sync queued"))

        val pushT = QueueTransport(step("POST /api/app/threat-intel/items/{id}/push-to-news"))
        val pushed = api(pushT).threatPushToNews(52)
        assertEquals("/api/app/threat-intel/items/52/push-to-news", pushT.requests[0].path)
        assertEquals(1, pushed.int("newsItemId"))
        assertTrue(pushed.str("message")!!.contains("NOT published"))

        val repeat = api(QueueTransport(step("POST /api/app/threat-intel/items/{id}/push-to-news (repeat)")))
            .threatPushToNews(52)
        assertEquals(1, repeat.int("newsItemId"))
    }

    // ---- Sentinel News vs News Desk separation ----

    @Test
    fun newsDeskDraftsNeverAppearInFeedUntilPublished() = runBlocking {
        val before = api(QueueTransport(step("GET /api/app/news/feed (before publish)"))).newsFeed()
        assertTrue(before.arr("items").isEmpty())

        val desk = api(QueueTransport(step("GET /api/app/news/items"))).newsItems()
        assertEquals(2, desk.arr("items").size)
        assertEquals(true, desk.bool("canWrite"))
        assertEquals(2, desk.obj("triage")!!.int("drafts"))
        assertEquals("draft", desk.arr("items")[0].str("state"))

        val after = api(QueueTransport(step("GET /api/app/news/feed"))).newsFeed()
        assertEquals(1, after.arr("items").size)
        assertEquals("published", after.arr("items")[0].str("state"))
        assertEquals("Updated seed summary", after.arr("items")[0].str("summary"))
    }

    @Test
    fun newsItemLifecycle() = runBlocking {
        val createT = QueueTransport(step("POST /api/app/news/items"))
        val created = api(createT).newsCreate(buildJsonObject {
            put("title", "Capture Seed Headline"); put("state", "published")
        })
        assertTrue(createT.requests[0].body!!.contains("\"item\""))
        // Items are born drafts even when saved with state=published.
        assertEquals("draft", created.obj("item")!!.str("state"))

        val edited = api(QueueTransport(step("PATCH /api/app/news/items/{id}")))
            .newsEdit(2, buildJsonObject { put("summary", "Updated seed summary") })
        assertEquals("Updated seed summary", edited.obj("item")!!.str("summary"))

        val published = api(QueueTransport(step("POST /api/app/news/items/{id}/publish"))).newsPublish(2)
        assertEquals("Published to Sentinel News.", published.str("message"))
        assertEquals("published", published.obj("item")!!.str("state"))

        val unpublished = api(QueueTransport(step("POST /api/app/news/items/{id}/unpublish"))).newsUnpublish(2)
        assertEquals("draft", unpublished.obj("item")!!.str("state"))

        val forDelete = api(QueueTransport(step("POST /api/app/news/items (for delete)")))
            .newsCreate(buildJsonObject { put("title", "Capture Delete Me") })
        assertNotNull(forDelete.obj("item")!!.int("id"))
        val deleted = api(QueueTransport(step("DELETE /api/app/news/items/{id}"))).newsDelete(3)
        assertNotNull(deleted.str("message"))
    }

    @Test
    fun newsPublishIncompleteRefusesInServerWords() = runBlocking {
        api(QueueTransport(step("POST /api/app/news/items (incomplete)")))
            .newsCreate(buildJsonObject { put("title", "Capture Incomplete Draft") })
        try {
            api(QueueTransport(step("POST /api/app/news/items/{id}/publish (incomplete)"))).newsPublish(3)
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(403, e.status)
            assertEquals("Before publishing, the item still needs: summary, body.", e.message)
        }
    }

    @Test
    fun newsBulkDeleteRefusesPublishedThenDeletesDrafts() = runBlocking {
        try {
            api(QueueTransport(step("POST /api/app/news/items/bulk-delete-drafts (published)")))
                .newsBulkDeleteDrafts(listOf(2))
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(403, e.status)
            assertTrue(e.message!!.contains("Published items can never be deleted"))
        }
        val out = api(QueueTransport(step("POST /api/app/news/items/bulk-delete-drafts")))
            .newsBulkDeleteDrafts(listOf(2, 3, 1))
        assertEquals("Deleted 3 draft(s).", out.str("message"))
        assertEquals(3, out.int("deleted"))
    }

    // ---- Reviews ----

    @Test
    fun reviewsQueueLifecycle() = runBlocking {
        val list = api(QueueTransport(step("GET /api/app/reviews"))).reviews()
        assertEquals(true, list.bool("canWrite"))
        val review = list.arr("reviews")[0]
        assertEquals("Capture Fictional Biz", review.str("businessName"))
        assertEquals("pending", review.str("status"))
        assertEquals(5, review.int("rating"))

        try {
            api(QueueTransport(step("POST /api/app/reviews/{id}/featured (pending)")))
                .reviewFeatured(1, true)
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(403, e.status)
            assertEquals("Only approved (published) reviews can be featured.", e.message)
        }

        val published = api(QueueTransport(step("POST /api/app/reviews/{id}/publish"))).reviewPublish(1)
        assertEquals("Review published.", published.str("message"))

        val featured = api(QueueTransport(step("POST /api/app/reviews/{id}/featured")))
            .reviewFeatured(1, true, 1)
        assertEquals(true, featured.obj("review")!!.bool("featured"))

        val hidden = api(QueueTransport(step("POST /api/app/reviews/{id}/hide"))).reviewHide(1)
        assertNotNull(hidden.str("message"))
    }

    @Test
    fun reviewResponsesLifecycle() = runBlocking {
        val empty = api(QueueTransport(step("GET /api/app/reviews/responses (empty)"))).reviewResponses()
        assertTrue(empty.arr("reviews").isNotEmpty())

        val saved = api(QueueTransport(step("POST /api/app/reviews/{id}/response")))
            .reviewResponseSave(1, "Seed owner response")
        assertEquals(1, saved.int("reviewId"))

        val seeded = api(QueueTransport(step("GET /api/app/reviews/responses"))).reviewResponses()
        val response = seeded.arr("reviews")[0].obj("response")!!
        assertEquals("Seed owner response", response.str("body"))
        assertEquals("draft", response.str("status"))
        assertEquals("Response: draft", Phase6Logic.responseStatus(seeded.arr("reviews")[0]))

        val published = api(QueueTransport(step("POST /api/app/reviews/{id}/response/publish")))
            .reviewResponsePublish(1)
        assertEquals(1, published.int("reviewId"))
        val unpublished = api(QueueTransport(step("POST /api/app/reviews/{id}/response/unpublish")))
            .reviewResponseUnpublish(1)
        assertEquals(1, unpublished.int("reviewId"))
    }

    // ---- Dogg and Associates ----

    @Test
    fun lawWorkspaceParsesCatalogDocumentsAndRequests() = runBlocking {
        val empty = api(QueueTransport(step("GET /api/app/law (empty)"))).lawWorkspace()
        assertEquals(5, empty.arr("practiceAreas").size)
        assertTrue(empty.arr("documents").isEmpty())
        assertNotNull(Phase6Logic.lawTemplate(empty, "operating-agreement"))
        assertEquals(true, Phase6Logic.lawTemplate(empty, "operating-agreement")!!.bool("fastPath"))

        val seeded = api(QueueTransport(step("GET /api/app/law"))).lawWorkspace()
        assertEquals(2, seeded.arr("documents").size)
        assertEquals(2, seeded.arr("aiRequests").size)
        assertEquals("docgen", seeded.arr("aiRequests")[0].str("kind"))
    }

    @Test
    fun lawDocumentCrudAndFastPathRefusal() = runBlocking {
        val noteT = QueueTransport(step("POST /api/app/law/documents (research note)"))
        val note = api(noteT).lawDocCreate(buildJsonObject {
            put("kind", "research"); put("question", "Capture seed question?")
            put("title", "Capture Research Note")
        })
        assertTrue(noteT.requests[0].body!!.contains("\"doc\""))
        assertEquals("Saved.", note.str("message"))
        assertEquals(1, note.obj("document")!!.int("id"))

        try {
            api(QueueTransport(step("POST /api/app/law/documents (non-fast refusal)")))
                .lawDocCreate(buildJsonObject {
                    put("kind", "document"); put("templateKey", "consulting-agreement")
                    put("title", "Nope")
                })
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(403, e.status)
            assertTrue(e.message!!.contains("not a standard form"))
        }

        val fast = api(QueueTransport(step("POST /api/app/law/documents")))
            .lawDocCreate(buildJsonObject {
                put("kind", "document"); put("templateKey", "operating-agreement")
            })
        assertTrue(fast.obj("document")!!.str("title")!!.contains("Operating Agreement"))

        val edited = api(QueueTransport(step("PATCH /api/app/law/documents/{id}")))
            .lawDocEdit(1, buildJsonObject { put("title", "Capture Research Note Edited") })
        assertEquals("Capture Research Note Edited", edited.obj("document")!!.str("title"))

        val deleted = api(QueueTransport(step("DELETE /api/app/law/documents/{id}"))).lawDocDelete(1)
        assertEquals(1, deleted.int("id"))
    }

    @Test
    fun lawResearchAndDocgenQueues() = runBlocking {
        try {
            api(QueueTransport(step("POST /api/app/law/research (bad jurisdiction)")))
                .lawResearch(buildJsonObject {
                    put("question", "What is a fictional statute?")
                    put("jurisdiction", "Nowhere Land")
                })
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(403, e.status)
            assertEquals("Pick a jurisdiction from the list.", e.message)
        }
        val research = api(QueueTransport(step("POST /api/app/law/research")))
            .lawResearch(buildJsonObject {
                put("question", "What is a fictional statute?")
                put("jurisdiction", "US Federal")
            })
        assertEquals("queued", research.obj("request")!!.str("status"))
        assertEquals("research", research.obj("request")!!.str("kind"))

        val docgen = api(QueueTransport(step("POST /api/app/law/docgen")))
            .lawDocgen(buildJsonObject { put("templateKey", "consulting-agreement") })
        assertEquals("docgen", docgen.obj("request")!!.str("kind"))
        assertEquals("Consulting Agreement", docgen.obj("request")!!.str("label"))
    }

    // ---- Pure logic pins ----

    @Test
    fun phase6LogicPins() {
        val threat = RealBodiesPhase6.bodyJson("GET /api/app/live-intel/threat-data")
        val lines = Phase6Logic.flatten(threat["data"])
        assertTrue(lines.any { it.first == "marker" && it.second == "phase6-threat" })
        assertTrue(lines.any { it.second == "42" })

        val desk = RealBodiesPhase6.bodyJson("GET /api/app/news/items")
        assertEquals(listOf(2, 1), Phase6Logic.draftIds(desk.arr("items")))

        val feed = RealBodiesPhase6.bodyJson("GET /api/app/news/feed")
        assertTrue(Phase6Logic.draftIds(feed.arr("items")).isEmpty())

        val noResponse = RealBodiesPhase6.bodyJson("GET /api/app/reviews")
        assertEquals("No response", Phase6Logic.responseStatus(noResponse.arr("reviews")[0]))
    }
}

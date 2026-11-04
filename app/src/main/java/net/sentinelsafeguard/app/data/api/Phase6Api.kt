package net.sentinelsafeguard.app.data.api

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Phase 6 (hunt10) endpoint methods: the Intel group — Live
 * Intel, OSINT, Breach Intel, Threat Intel, Sentinel News,
 * News Desk, Reviews, and Dogg and Associates (law).
 *
 * Every method rides the deployed Phase 6 backend (Addendum 20;
 * production server.py sha256 4351a08a…), whose payloads are the
 * web handlers' own — so these ride the open-JSON verbs on
 * [SentinelApi] exactly like Phases 3–5. Flag refusals and
 * handler business refusals surface the server's own message via
 * [SentinelApi.callJson] — never a fake empty state. Request
 * bodies mirror phase6-work/rehearsal_capture.py field for field.
 * Law AI config/key actions are Phase 8 Admin and are NOT here.
 */

private fun JsonObject.toBody6(): String = toString()

private fun SentinelApi.qs(params: Map<String, String?>): String {
    val parts = params.entries.filter { !it.value.isNullOrBlank() }
        .map { "${it.key}=${encodeQuery(it.value!!)}" }
    return if (parts.isEmpty()) "" else "?" + parts.joinToString("&")
}

// ---- Live Intel (module perm enforced server-side; no boot flag) ----

suspend fun SentinelApi.liveThreatData(): JsonObject =
    callJson("GET", "/api/app/live-intel/threat-data")

suspend fun SentinelApi.liveGeoData(): JsonObject =
    callJson("GET", "/api/app/live-intel/geo-data")

suspend fun SentinelApi.liveFlights(): JsonObject =
    callJson("GET", "/api/app/live-intel/flights")

suspend fun SentinelApi.liveFlightLookup(flightNumber: String): JsonObject =
    callJson("GET", "/api/app/live-intel/flight-lookup" + qs(mapOf("flightNumber" to flightNumber)))

// ---- OSINT (osintAccess) ----

suspend fun SentinelApi.osintWorkspace(): JsonObject =
    callJson("GET", "/api/app/osint")

suspend fun SentinelApi.osintDetect(value: String): JsonObject =
    callJson("GET", "/api/app/osint/detect" + qs(mapOf("value" to value)))

suspend fun SentinelApi.osintCreateSearch(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/osint/searches", body.toBody6())

suspend fun SentinelApi.osintReport(id: Int): JsonObject =
    callJson("GET", "/api/app/osint/reports/$id")

suspend fun SentinelApi.osintDeleteReport(id: Int): JsonObject =
    deleteJson("/api/app/osint/reports/$id")

suspend fun SentinelApi.osintCreateWatch(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/osint/watches", body.toBody6())

suspend fun SentinelApi.osintDeleteWatch(id: Int): JsonObject =
    deleteJson("/api/app/osint/watches/$id")

suspend fun SentinelApi.osintRunWatchNow(id: Int): JsonObject =
    callJson("POST", "/api/app/osint/watches/$id/run-now", "{}")

// ---- Breach Intel (breachAccess) ----

suspend fun SentinelApi.breachIntel(
    q: String = "",
    sector: String? = null,
    breachId: Int? = null,
): JsonObject = callJson(
    "GET", "/api/app/breach-intel" + qs(
        mapOf(
            "q" to q.ifBlank { null },
            "sector" to sector,
            "breachId" to breachId?.toString(),
        )
    )
)

suspend fun SentinelApi.breachCreate(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/breach-intel", body.toBody6())

suspend fun SentinelApi.breachEdit(id: Int, body: JsonObject): JsonObject =
    patchJson("/api/app/breach-intel/$id", body.toBody6())

suspend fun SentinelApi.breachDelete(id: Int): JsonObject =
    deleteJson("/api/app/breach-intel/$id")

suspend fun SentinelApi.breachCirculationCreate(breachId: Int, body: JsonObject): JsonObject =
    callJson("POST", "/api/app/breach-intel/$breachId/circulation", body.toBody6())

suspend fun SentinelApi.breachCirculationDelete(circulationId: Int): JsonObject =
    deleteJson("/api/app/breach-intel/circulation/$circulationId")

suspend fun SentinelApi.breachIntakeRun(): JsonObject =
    callJson("POST", "/api/app/breach-intel/intake-run", "{}")

// ---- Threat Intel (threatAccess) ----

suspend fun SentinelApi.threatIntel(
    source: String? = null,
    category: String? = null,
    severity: String? = null,
    q: String = "",
    itemId: Int? = null,
): JsonObject = callJson(
    "GET", "/api/app/threat-intel" + qs(
        mapOf(
            "source" to source,
            "category" to category,
            "severity" to severity,
            "q" to q.ifBlank { null },
            "itemId" to itemId?.toString(),
        )
    )
)

suspend fun SentinelApi.threatSync(): JsonObject =
    callJson("POST", "/api/app/threat-intel/sync", "{}")

suspend fun SentinelApi.threatPushToNews(itemId: Int): JsonObject =
    callJson("POST", "/api/app/threat-intel/items/$itemId/push-to-news", "{}")

// ---- Sentinel News reader (newsAccess; published only) ----

suspend fun SentinelApi.newsFeed(): JsonObject =
    callJson("GET", "/api/app/news/feed")

// ---- News Desk (newsAccess) ----

suspend fun SentinelApi.newsItems(): JsonObject =
    callJson("GET", "/api/app/news/items")

suspend fun SentinelApi.newsCreate(item: JsonObject): JsonObject =
    callJson("POST", "/api/app/news/items", buildJsonObject { put("item", item) }.toBody6())

suspend fun SentinelApi.newsEdit(id: Int, item: JsonObject): JsonObject =
    patchJson("/api/app/news/items/$id", buildJsonObject { put("item", item) }.toBody6())

suspend fun SentinelApi.newsPublish(id: Int): JsonObject =
    callJson("POST", "/api/app/news/items/$id/publish", "{}")

suspend fun SentinelApi.newsUnpublish(id: Int): JsonObject =
    callJson("POST", "/api/app/news/items/$id/unpublish", "{}")

suspend fun SentinelApi.newsDelete(id: Int): JsonObject =
    deleteJson("/api/app/news/items/$id")

suspend fun SentinelApi.newsBulkDeleteDrafts(ids: List<Int>): JsonObject =
    callJson(
        "POST", "/api/app/news/items/bulk-delete-drafts",
        buildJsonObject {
            put("ids", kotlinx.serialization.json.JsonArray(ids.map {
                kotlinx.serialization.json.JsonPrimitive(it)
            }))
        }.toBody6(),
    )

// ---- Reviews (reviewsAccess; responses OWNER-only server-side) ----

suspend fun SentinelApi.reviews(): JsonObject =
    callJson("GET", "/api/app/reviews")

suspend fun SentinelApi.reviewPublish(id: Int): JsonObject =
    callJson("POST", "/api/app/reviews/$id/publish", "{}")

suspend fun SentinelApi.reviewHide(id: Int): JsonObject =
    callJson("POST", "/api/app/reviews/$id/hide", "{}")

suspend fun SentinelApi.reviewFeatured(id: Int, featured: Boolean, featuredOrder: Int? = null): JsonObject =
    callJson(
        "POST", "/api/app/reviews/$id/featured",
        buildJsonObject {
            put("featured", featured)
            featuredOrder?.let { put("featuredOrder", it) }
        }.toBody6(),
    )

suspend fun SentinelApi.reviewResponses(): JsonObject =
    callJson("GET", "/api/app/reviews/responses")

suspend fun SentinelApi.reviewResponseSave(id: Int, body: String): JsonObject =
    callJson(
        "POST", "/api/app/reviews/$id/response",
        buildJsonObject { put("body", body) }.toBody6(),
    )

suspend fun SentinelApi.reviewResponsePublish(id: Int): JsonObject =
    callJson("POST", "/api/app/reviews/$id/response/publish", "{}")

suspend fun SentinelApi.reviewResponseUnpublish(id: Int): JsonObject =
    callJson("POST", "/api/app/reviews/$id/response/unpublish", "{}")

// ---- Dogg and Associates (lawAccess; NO config/key UI — Phase 8) ----

suspend fun SentinelApi.lawWorkspace(): JsonObject =
    callJson("GET", "/api/app/law")

suspend fun SentinelApi.lawDocCreate(doc: JsonObject): JsonObject =
    callJson("POST", "/api/app/law/documents", buildJsonObject { put("doc", doc) }.toBody6())

suspend fun SentinelApi.lawDocEdit(id: Int, doc: JsonObject): JsonObject =
    patchJson("/api/app/law/documents/$id", buildJsonObject { put("doc", doc) }.toBody6())

suspend fun SentinelApi.lawDocDelete(id: Int): JsonObject =
    deleteJson("/api/app/law/documents/$id")

suspend fun SentinelApi.lawResearch(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/law/research", body.toBody6())

suspend fun SentinelApi.lawDocgen(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/law/docgen", body.toBody6())

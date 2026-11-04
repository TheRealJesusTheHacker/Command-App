package net.sentinelsafeguard.app.data.api

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Phase 8A (hunt12) endpoint methods: the Firm group, part A —
 * Portal Settings (b3ops), Firm Ops (cf2firmops), Verified Desk
 * (b4assess), and the full Activity view.
 *
 * Every method rides the deployed Phase 8A backend (Addendum 24;
 * production server.py sha256 f12d215e…), whose payloads are the
 * web handlers' own — so these ride the open-JSON verbs on
 * [SentinelApi] exactly like Phases 3–7. Refusals surface the
 * server's own message via [SentinelApi.callJson] — never a fake
 * empty state. Request bodies mirror
 * phase8a-work/rehearsal_capture.py field for field.
 *
 * Standing decisions honored here:
 *  - Portal Settings secrets are METADATA ONLY: the one-time
 *    value is a write-only input to the create call and is never
 *    returned by any endpoint (the list is the handler's
 *    metadata shape, exactly as web).
 *  - The Verified Desk badge revoke is its OWN endpoint with a
 *    required reason (web b4revokebadge) — distinct from the
 *    Phase 3 register revoke, which carries no reason.
 *  - Availability writes ride the handler's {record:{…}} shape
 *    exactly as web's editor sends it.
 *  - The Activity console pages with the handler's own
 *    limit+offset; [activityAll] walks the complete filtered
 *    set, as web's totals require.
 */

private fun JsonObject.toBody8(): String = toString()

private fun SentinelApi.qs8(params: Map<String, String?>): String {
    val parts = params.entries.filter { !it.value.isNullOrBlank() }
        .map { "${it.key}=${encodeQuery(it.value!!)}" }
    return if (parts.isEmpty()) "" else "?" + parts.joinToString("&")
}

// ---- Portal Settings (web b3ops) ----

/** Subcontractor assignments + the assignment targets. The
 *  payload's assignments are the caller's own sight (web's
 *  handler filters by role exactly as the capture shows). */
suspend fun SentinelApi.portalSubs(): JsonObject =
    callJson("GET", "/api/app/portal-settings/sub-assignments")

suspend fun SentinelApi.portalAssign(jobId: Int, userId: Int): JsonObject =
    callJson(
        "POST", "/api/app/portal-settings/sub-assignments",
        buildJsonObject {
            put("jobId", jobId)
            put("userId", userId)
        }.toBody8(),
    )

suspend fun SentinelApi.portalUnassign(jobId: Int, userId: Int): JsonObject =
    callJson(
        "POST", "/api/app/portal-settings/sub-assignments/unassign",
        buildJsonObject {
            put("jobId", jobId)
            put("userId", userId)
        }.toBody8(),
    )

/** One-time secret METADATA for a client (label / state /
 *  created / expires / viewed) — the value itself is never in
 *  this payload, on web or here. */
suspend fun SentinelApi.portalSecrets(clientId: Int): JsonObject =
    callJson("GET", "/api/app/portal-settings/secrets" + qs8(mapOf("clientId" to clientId.toString())))

/** Create a view-once secret. [value] is write-only: it is sent
 *  once, encrypted by the handler inside the create call, and
 *  never displayed back — the response carries only the
 *  handler's message and the new id. */
suspend fun SentinelApi.portalSecretCreate(
    clientId: Int,
    label: String,
    value: String,
    expiresHours: Int?,
): JsonObject = callJson(
    "POST", "/api/app/portal-settings/secrets",
    buildJsonObject {
        put("clientId", clientId)
        put("label", label)
        put("value", value)
        if (expiresHours != null) put("expiresHours", expiresHours)
    }.toBody8(),
)

/** The client one-pager (web b3getonepager). A client the
 *  handler doesn't know answers `onepager: null` — web's own
 *  semantics, kept verbatim. */
suspend fun SentinelApi.portalOnePager(clientId: Int): JsonObject =
    callJson("GET", "/api/app/portal-settings/one-pager" + qs8(mapOf("clientId" to clientId.toString())))

/** Portal security tips + the rotation's current tip id. */
suspend fun SentinelApi.portalTips(): JsonObject =
    callJson("GET", "/api/app/portal-settings/tips")

suspend fun SentinelApi.portalTipCreate(
    title: String,
    body: String,
    active: Boolean,
    sortOrder: Int,
): JsonObject = callJson(
    "POST", "/api/app/portal-settings/tips",
    buildJsonObject {
        put("title", title)
        put("body", body)
        put("active", active)
        put("sortOrder", sortOrder)
    }.toBody8(),
)

/** Tip edit: a flat partial body — the backend seeds omitted
 *  fields from the stored row, exactly as web's editor does. */
suspend fun SentinelApi.portalTipPatch(id: Int, fields: JsonObject): JsonObject =
    patchJson("/api/app/portal-settings/tips/$id", fields.toBody8())

suspend fun SentinelApi.portalTipDelete(id: Int): JsonObject =
    deleteJson("/api/app/portal-settings/tips/$id")

/** The portal rotation read the tips surface feeds. */
suspend fun SentinelApi.portalCurrentTip(): JsonObject =
    callJson("GET", "/api/app/portal-settings/current-tip")

// ---- Firm Ops (web cf2firmops) ----

/** Job costs across jobs (owner-only inside the handler). */
suspend fun SentinelApi.firmJobCosts(): JsonObject =
    callJson("GET", "/api/app/firm-ops/job-costs")

/** One job's costing detail (kind ticket|case, per the list). */
suspend fun SentinelApi.firmJobCosting(kind: String, id: Int): JsonObject =
    callJson(
        "GET", "/api/app/firm-ops/job-costing" + qs8(
            mapOf("kind" to kind, "id" to id.toString())
        )
    )

suspend fun SentinelApi.firmVendors(): JsonObject =
    callJson("GET", "/api/app/firm-ops/vendors")

/** Vendor create rides web's wrapped {vendor:{…}} body. */
suspend fun SentinelApi.firmVendorCreate(vendor: JsonObject): JsonObject =
    callJson(
        "POST", "/api/app/firm-ops/vendors",
        buildJsonObject { put("vendor", vendor) }.toBody8(),
    )

/** Vendor edit: a flat partial body — the handler seeds omitted
 *  fields from the stored row itself. */
suspend fun SentinelApi.firmVendorPatch(id: Int, fields: JsonObject): JsonObject =
    patchJson("/api/app/firm-ops/vendors/$id", fields.toBody8())

suspend fun SentinelApi.firmVendorDelete(id: Int): JsonObject =
    deleteJson("/api/app/firm-ops/vendors/$id")

/** Owner tasks, optionally filtered by state (web's states:
 *  open / done / dropped, riding the payload's own list). */
suspend fun SentinelApi.firmTasks(state: String? = null): JsonObject =
    callJson("GET", "/api/app/firm-ops/tasks" + qs8(mapOf("state" to state)))

/** Owner-task create rides web's wrapped {task:{…}} body. */
suspend fun SentinelApi.firmTaskCreate(task: JsonObject): JsonObject =
    callJson(
        "POST", "/api/app/firm-ops/tasks",
        buildJsonObject { put("task", task) }.toBody8(),
    )

/** Owner-task edit: a flat partial body, handler-seeded. */
suspend fun SentinelApi.firmTaskPatch(id: Int, fields: JsonObject): JsonObject =
    patchJson("/api/app/firm-ops/tasks/$id", fields.toBody8())

suspend fun SentinelApi.firmTaskState(id: Int, state: String): JsonObject =
    callJson(
        "POST", "/api/app/firm-ops/tasks/$id/state",
        buildJsonObject { put("state", state) }.toBody8(),
    )

/** Convert creates a NEW ticket; the task stays done + linked. */
suspend fun SentinelApi.firmTaskConvert(id: Int): JsonObject =
    callJson("POST", "/api/app/firm-ops/tasks/$id/convert", "{}")

suspend fun SentinelApi.firmAvailability(): JsonObject =
    callJson("GET", "/api/app/firm-ops/availability")

/** Availability create rides the handler's {record:{…}} shape
 *  exactly as web sends it — a flat body is refused by the
 *  handler itself, on web and app alike. */
suspend fun SentinelApi.firmAvailCreate(record: JsonObject): JsonObject =
    callJson(
        "POST", "/api/app/firm-ops/availability",
        buildJsonObject { put("record", record) }.toBody8(),
    )

/** Availability edit: the same {record:{…}} shape, partial
 *  inside the record (the stored row supplies the rest through
 *  the handler's own save path, as the capture proves). */
suspend fun SentinelApi.firmAvailPatch(id: Int, record: JsonObject): JsonObject =
    patchJson(
        "/api/app/firm-ops/availability/$id",
        buildJsonObject { put("record", record) }.toBody8(),
    )

suspend fun SentinelApi.firmAvailDelete(id: Int): JsonObject =
    deleteJson("/api/app/firm-ops/availability/$id")

suspend fun SentinelApi.firmBookingWarnings(): JsonObject =
    callJson("GET", "/api/app/firm-ops/booking-warnings")

// ---- Verified Desk (web b4assess) ----

/** The Desk landing payload: assessments + the Verified
 *  register + the control set + categories + badge meaning. */
suspend fun SentinelApi.deskList(clientId: Int? = null): JsonObject =
    callJson(
        "GET", "/api/app/verified-desk/assessments" + qs8(
            mapOf("clientId" to clientId?.toString())
        )
    )

suspend fun SentinelApi.deskDetail(id: Int): JsonObject =
    callJson("GET", "/api/app/verified-desk/assessments/$id")

suspend fun SentinelApi.deskStart(clientId: Int, category: String): JsonObject =
    callJson(
        "POST", "/api/app/verified-desk/assessments",
        buildJsonObject {
            put("clientId", clientId)
            put("category", category)
        }.toBody8(),
    )

/** Save the checklist: the full control set with each control's
 *  status + evidence note (web b4assesssave). */
suspend fun SentinelApi.deskSave(id: Int, controls: JsonArray): JsonObject =
    callJson(
        "POST", "/api/app/verified-desk/assessments/$id/save",
        buildJsonObject { put("controls", controls) }.toBody8(),
    )

/** Workflow state move: draft → in_review → completed (the
 *  handler computes + stores the score on completion). */
suspend fun SentinelApi.deskState(id: Int, to: String): JsonObject =
    callJson(
        "POST", "/api/app/verified-desk/assessments/$id/state",
        buildJsonObject { put("to", to) }.toBody8(),
    )

/** Issue the badge from a completed assessment (OWNER-only
 *  inside the handler; the confirm step is the client's, with
 *  web's confirm semantics). */
suspend fun SentinelApi.deskIssueBadge(id: Int): JsonObject =
    callJson("POST", "/api/app/verified-desk/assessments/$id/issue-badge", "{}")

/** The Desk's badge revoke (web b4revokebadge): confirm +
 *  REQUIRED reason, kept on the row. Distinct from the Phase 3
 *  register revoke — both stay where web keeps them. */
suspend fun SentinelApi.deskRevokeBadge(verificationId: Int, reason: String): JsonObject =
    callJson(
        "POST", "/api/app/verified-desk/verifications/$verificationId/revoke",
        buildJsonObject {
            put("confirm", true)
            put("reason", reason)
        }.toBody8(),
    )

/** The evidence-pack download (the Desk's Generated documents
 *  section): a base64 PDF + its sha256/size descriptor. */
suspend fun SentinelApi.deskEvidencePack(clientId: Int, framework: String? = null): JsonObject =
    callJson(
        "GET", "/api/app/verified-desk/evidence-pack" + qs8(
            mapOf("clientId" to clientId.toString(), "framework" to framework)
        )
    )

// ---- Activity (full view, web getactivity) ----

/** One page of the Activity console, with web's filters
 *  (class / q / ip / actorId / days) and the handler's own
 *  limit+offset paging. The payload carries rows / total /
 *  summary / failedByIp / sessions / classCounts. */
suspend fun SentinelApi.activityPage(
    cls: String? = null,
    q: String? = null,
    ip: String? = null,
    actorId: Int? = null,
    days: Int? = null,
    limit: Int = ACTIVITY_PAGE_SIZE,
    offset: Int = 0,
): JsonObject = callJson(
    "GET", "/api/app/activity" + qs8(
        mapOf(
            "class" to cls?.takeIf { it != "all" },
            "q" to q,
            "ip" to ip,
            "actorId" to actorId?.toString(),
            "days" to days?.toString(),
            "limit" to limit.toString(),
            "offset" to offset.toString(),
        )
    )
)

/**
 * The complete filtered set: walk the handler's limit+offset
 * paging until the payload's own [total] is collected — web
 * shows totals over the whole filtered set, so the mirror
 * never renders a silent first page. The walk ends on an empty
 * page, and at web's own 200-page bound, exactly as the
 * backend addendum records. The returned payload is the first
 * page's envelope with `rows` replaced by the full walk (its
 * summary / failedByIp / sessions / classCounts ride the first
 * page, as the handler computes them per request).
 */
suspend fun SentinelApi.activityAll(
    cls: String? = null,
    q: String? = null,
    ip: String? = null,
    actorId: Int? = null,
    days: Int? = null,
): JsonObject {
    val all = mutableListOf<kotlinx.serialization.json.JsonElement>()
    var first: JsonObject? = null
    var total = Int.MAX_VALUE
    var offset = 0
    var pages = 0
    while (all.size < total && pages < 200) {
        val page = activityPage(cls, q, ip, actorId, days, ACTIVITY_PAGE_SIZE, offset)
        if (first == null) first = page
        val rows = (page["rows"] as? JsonArray) ?: JsonArray(emptyList())
        total = (page["total"] as? kotlinx.serialization.json.JsonPrimitive)
            ?.content?.toIntOrNull() ?: rows.size
        if (rows.isEmpty()) break
        all.addAll(rows)
        offset += rows.size
        pages++
    }
    val base = first ?: JsonObject(emptyMap())
    return JsonObject(base + ("rows" to JsonArray(all)))
}

/** Active sessions (admin:read inside the handler). */
suspend fun SentinelApi.activitySessions(): JsonObject =
    callJson("GET", "/api/app/activity/sessions")

/** Revoke one session (admin:write inside the handler). */
suspend fun SentinelApi.activityRevokeSession(id: Int): JsonObject =
    callJson("POST", "/api/app/activity/sessions/$id/revoke", "{}")

/** Public visitors: the honest payload — `connected: false`
 *  when the feed is not wired, never a fabricated list. */
suspend fun SentinelApi.activityVisitors(): JsonObject =
    callJson("GET", "/api/app/activity/public-visitors")

/** The console page size the walker requests (the handler's
 *  own paging; fewest round trips, no invented cap). */
const val ACTIVITY_PAGE_SIZE: Int = 200

package net.sentinelsafeguard.app.data.api

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Phase 9 (hunt14) endpoint methods: the Client/Family PORTAL
 * shell surface under /api/app/portal/ plus the STAFF shell's
 * My Schedule surface, riding the deployed Phase 9 backend
 * (Addendum 28; production server.py sha256 b267d652…). Every
 * portal endpoint rides the actual web portal handler — the
 * client identity is derived from the device token's account
 * server-side; no method here ever sends a client id. Payloads
 * are the web handlers' own, so these ride the open-JSON verbs
 * on [SentinelApi] exactly like Phases 3–8B. Refusals surface
 * the server's own message (callJson) — a cross-client id guess
 * answers in web's words, never a fake empty state.
 *
 * Named deferrals (Addendum 28) stay web-only and are NOT
 * methods here: self-booking/wizard, locations / maintenance
 * windows / retainer / emergency-contact portal reads, one-time
 * secrets / one-pagers / tips portal reads, checklist runs, and
 * passkey enrollment for portal accounts. The portal shell's
 * twelve views (COMMAND-MIRROR-SPEC §2.1/§6 Phase 9) are the
 * complete surface.
 */

private fun JsonObject.toBody9(): String = toString()

// ---- Portal home composition (web getmyportal) ----

/** My Profile / Progress / Your Documents / invoices / quotes /
 *  library in the handler's one payload. */
suspend fun SentinelApi.portalHome(): JsonObject =
    callJson("GET", "/api/app/portal")

suspend fun SentinelApi.portalHistory(): JsonObject =
    callJson("GET", "/api/app/portal/history")

suspend fun SentinelApi.portalHealthTimeline(): JsonObject =
    callJson("GET", "/api/app/portal/health-timeline")

/** Portal-audience knowledge only — the server enforces the
 *  audience rules in SQL; the app renders what returns. */
suspend fun SentinelApi.portalKnowledge(): JsonObject =
    callJson("GET", "/api/app/portal/knowledge")

/** Critical Alert: files a Critical ticket on the caller's own
 *  record; priority/client are minted server-side and any
 *  smuggled field is ignored there (backend-proven). */
suspend fun SentinelApi.portalCriticalAlert(title: String, details: String): JsonObject =
    callJson(
        "POST", "/api/app/portal/critical-alert",
        buildJsonObject {
            put("title", title)
            put("details", details)
        }.toBody9(),
    )

// ---- Contracts & Reports ----

suspend fun SentinelApi.portalContracts(): JsonObject =
    callJson("GET", "/api/app/portal/contracts")

suspend fun SentinelApi.portalContract(id: Int): JsonObject =
    callJson("GET", "/api/app/portal/contracts/$id")

/** Sign a contract: web's ceremony is the typed signer name
 *  behind a confirm — the caller collects both. */
suspend fun SentinelApi.portalContractSign(id: Int, signerName: String): JsonObject =
    callJson(
        "POST", "/api/app/portal/contracts/$id/sign",
        buildJsonObject { put("signerName", signerName) }.toBody9(),
    )

suspend fun SentinelApi.portalContractDocument(id: Int): JsonObject =
    callJson("GET", "/api/app/portal/contracts/$id/document")

suspend fun SentinelApi.portalReportDocument(id: Int): JsonObject =
    callJson("GET", "/api/app/portal/reports/$id/document")

/** Decide a quote: web's vocabulary is approve | decline,
 *  behind a confirm (caller-side). */
suspend fun SentinelApi.portalQuoteDecide(id: Int, decision: String): JsonObject =
    callJson(
        "POST", "/api/app/portal/quotes/$id/decide",
        buildJsonObject { put("decision", decision) }.toBody9(),
    )

suspend fun SentinelApi.portalProposals(): JsonObject =
    callJson("GET", "/api/app/portal/proposals")

suspend fun SentinelApi.portalProposal(id: Int): JsonObject =
    callJson("GET", "/api/app/portal/proposals/$id")

/** Decide a proposal: web's vocabulary is accept | decline. */
suspend fun SentinelApi.portalProposalDecide(id: Int, decision: String): JsonObject =
    callJson(
        "POST", "/api/app/portal/proposals/$id/decide",
        buildJsonObject { put("decision", decision) }.toBody9(),
    )

suspend fun SentinelApi.portalProposalDocument(id: Int): JsonObject =
    callJson("GET", "/api/app/portal/proposals/$id/document")

suspend fun SentinelApi.portalChangeRequests(): JsonObject =
    callJson("GET", "/api/app/portal/change-requests")

/** Decide a change request: approve | decline; an approval is
 *  a signed record and requires the typed signer name (the
 *  handler enforces it). */
suspend fun SentinelApi.portalChangeRequestDecide(
    id: Int,
    decision: String,
    signerName: String? = null,
): JsonObject =
    callJson(
        "POST", "/api/app/portal/change-requests/$id/decide",
        buildJsonObject {
            put("decision", decision)
            if (!signerName.isNullOrBlank()) put("signerName", signerName)
        }.toBody9(),
    )

/** The approval inbox: pending quotes / proposals / change
 *  requests / checklist sign-offs counted in one payload. */
suspend fun SentinelApi.portalApprovalInbox(): JsonObject =
    callJson("GET", "/api/app/portal/approval-inbox")

// ---- My Sign-offs ----

suspend fun SentinelApi.portalSignOffs(): JsonObject =
    callJson("GET", "/api/app/portal/sign-offs")

suspend fun SentinelApi.portalAuthorizations(): JsonObject =
    callJson("GET", "/api/app/portal/authorizations")

/** Sign an authorization: typed full name is the signature
 *  (handler-enforced, role EXACTLY Client there — Family is
 *  refused exactly as on web). */
suspend fun SentinelApi.portalAuthorizationSign(id: Int, signerName: String): JsonObject =
    callJson(
        "POST", "/api/app/portal/authorizations/$id/sign",
        buildJsonObject { put("signerName", signerName) }.toBody9(),
    )

// ---- Messages (portal thread on the client's OWN tickets) ----

suspend fun SentinelApi.portalTicketMessages(ticketId: Int): JsonObject =
    callJson("GET", "/api/app/portal/tickets/$ticketId/messages")

suspend fun SentinelApi.portalTicketMessagePost(ticketId: Int, text: String): JsonObject =
    callJson(
        "POST", "/api/app/portal/tickets/$ticketId/messages",
        buildJsonObject { put("body", text) }.toBody9(),
    )

// ---- My Files & Packets ----

suspend fun SentinelApi.portalPackets(): JsonObject =
    callJson("GET", "/api/app/portal/packets")

/** The packet document payload carries the PDF as base64 —
 *  the caller shares it as a file, never renders the string. */
suspend fun SentinelApi.portalPacketDocument(id: Int): JsonObject =
    callJson("GET", "/api/app/portal/packets/$id/document")

suspend fun SentinelApi.portalFileRequests(): JsonObject =
    callJson("GET", "/api/app/portal/file-requests")

/** Upload one file-request item: web's body is the file name
 *  plus base64 content; the server validates (PDF magic, cap)
 *  and files it into the vault. */
suspend fun SentinelApi.portalFileRequestUpload(
    itemId: Int,
    fileName: String,
    contentBase64: String,
): JsonObject =
    callJson(
        "POST", "/api/app/portal/file-requests/items/$itemId/upload",
        buildJsonObject {
            put("fileName", fileName)
            put("contentBase64", contentBase64)
        }.toBody9(),
    )

// ---- Status Board ----

suspend fun SentinelApi.portalStatusBoard(): JsonObject =
    callJson("GET", "/api/app/portal/status-board")

// ---- Feedback & Referrals ----

suspend fun SentinelApi.portalSurveys(): JsonObject =
    callJson("GET", "/api/app/portal/surveys")

/** Answer a survey: web's fields are rating / wouldRecommend /
 *  comment, behind the answer confirm. */
suspend fun SentinelApi.portalSurveyAnswer(
    id: Int,
    rating: Int,
    wouldRecommend: Boolean,
    comment: String,
): JsonObject =
    callJson(
        "POST", "/api/app/portal/surveys/$id/answer",
        buildJsonObject {
            put("rating", rating)
            put("wouldRecommend", wouldRecommend)
            put("comment", comment)
        }.toBody9(),
    )

suspend fun SentinelApi.portalReferrals(): JsonObject =
    callJson("GET", "/api/app/portal/referrals")

// ---- Own-account surfaces ----

suspend fun SentinelApi.portalNotificationPrefs(): JsonObject =
    callJson("GET", "/api/app/portal/notification-prefs")

suspend fun SentinelApi.portalNotificationPrefsSet(prefs: JsonObject): JsonObject =
    callJson("POST", "/api/app/portal/notification-prefs", prefs.toBody9())

/** Own client's contacts, read-only — contact WRITES stay
 *  staff-side exactly as on web (Addendum 28). */
suspend fun SentinelApi.portalContacts(): JsonObject =
    callJson("GET", "/api/app/portal/contacts")

suspend fun SentinelApi.portalLang(): JsonObject =
    callJson("GET", "/api/app/portal/lang")

suspend fun SentinelApi.portalLangSet(lang: String): JsonObject =
    callJson(
        "POST", "/api/app/portal/lang",
        buildJsonObject { put("portalLang", lang) }.toBody9(),
    )

// ---- STAFF shell: My Schedule ----
// My Schedule rides the typed ScheduleResponse, so its method
// lives on SentinelApi itself beside scheduleWindow (Phase 9
// backend: GET /api/app/my-schedule, the same _app_schedule
// ride as the Schedule view — for STAFF the handler forces
// scope "mine", own jobs only; no forked payload).

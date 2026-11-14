package net.sentinelsafeguard.app.data.api

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * CToS twins (CTOS-APP-2026-11-11; BUILD-PLAN section 6) — the
 * Command App 1.7.0 "OSINT tools" surface. Every method rides
 * the deployed-shape Phase 6 pattern: device-token auth, the
 * server's own handler cores behind the routes (a_ctosget /
 * a_ctoscreatecase / a_ctosdeletecase / a_ctosclearcase /
 * a_ctosexportcase / a_ctosrecord / a_ctostool), and refusals
 * surfacing the server's own message via [SentinelApi.callJson].
 *
 * There is deliberately NO key method here: source API keys are
 * entered in web Command settings only. The workspace payload's
 * keySources carry configured booleans — that is all the app
 * ever sees.
 *
 * PERFECTION MIRROR R-1 (F-C1): the case twins are creator-
 * scoped server-side. The workspace list arrives already
 * scoped (the app renders it verbatim, never filters it), and
 * every case-scoped twin answers a case the caller may not
 * touch with the unknown-id refusal — 403 with the message
 * CtosLogic.CASE_GONE_MESSAGE — which callJson surfaces as an
 * ApiException the screens map to the deleted-case end state
 * via CtosLogic.isCaseGone. R-5 (W-2): the workspace/case
 * payloads also carry the server's unpaged totals
 * (casesTotal / findingsTotal / artifactsTotal); the app
 * sends no paging params, so the defaults reproduce the full
 * historic payloads and the totals head the lists.
 */

private fun JsonObject.toBodyC(): String = toString()

suspend fun SentinelApi.ctosWorkspace(): JsonObject =
    callJson("GET", "/api/app/osint/ctos")

suspend fun SentinelApi.ctosCase(caseId: Int, ftype: String? = null): JsonObject =
    callJson(
        "GET",
        "/api/app/osint/ctos/cases/$caseId" +
            if (ftype.isNullOrBlank()) "" else "?ftype=${encodeQuery(ftype)}",
    )

suspend fun SentinelApi.ctosCreateCase(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/osint/ctos/cases", body.toBodyC())

suspend fun SentinelApi.ctosClearCase(caseId: Int): JsonObject =
    callJson("POST", "/api/app/osint/ctos/cases/$caseId/clear", "{}")

suspend fun SentinelApi.ctosDeleteCase(caseId: Int): JsonObject =
    deleteJson("/api/app/osint/ctos/cases/$caseId")

suspend fun SentinelApi.ctosExportCase(caseId: Int): JsonObject =
    callJson("GET", "/api/app/osint/ctos/cases/$caseId/export")

/**
 * AUDIT-171 B-18 [OSINT]: the printable case report twin
 * (a_ctosreport — structured report with the honesty footer).
 * The endpoint shipped with Wave 2 but had no app method and no
 * UI; only the raw export JSON could be shared.
 */
suspend fun SentinelApi.ctosCaseReport(caseId: Int): JsonObject =
    callJson("GET", "/api/app/osint/ctos/cases/$caseId/report")

suspend fun SentinelApi.ctosRecord(caseId: Int, body: JsonObject): JsonObject =
    callJson("POST", "/api/app/osint/ctos/cases/$caseId/record", body.toBodyC())

/**
 * TIER-PHASE-B S-2: the OPSEC profile save twin (the web
 * ctossetprofile handler verbatim behind the route). [profile]
 * carries ONLY the fields being changed — the handler treats an
 * absent field as "preserve the current value", so callers build
 * it with CtosLogic.profileSavePayload, never a full snapshot.
 * Success answers {ok, profile, profileOptions}; refusals are
 * the handler's own 403 messages and ride callJson verbatim.
 */
suspend fun SentinelApi.ctosSaveProfile(profile: JsonObject): JsonObject =
    callJson(
        "POST", "/api/app/osint/ctos/profile",
        buildJsonObject { put("profile", profile) }.toString(),
    )

suspend fun SentinelApi.ctosRunTool(tool: String, body: JsonObject): JsonObject =
    callJson("POST", "/api/app/osint/ctos/tools/$tool", body.toBodyC())

/**
 * PERFECTION R-6 (W-4): the async tool-run twins. The start
 * answers 202 with {ok, runId, status, tool} once EVERY
 * synchronous pre-run step has run server-side (osint/write
 * gate, network rate cap, F-C1 case scoping, the Wave 3
 * authorization gate with its audit row committed BEFORE
 * anything runs — a gate refusal is a completed run whose
 * result IS the refusal, exactly as the sync path returns
 * it). Refusals that precede any run (unknown tool, rate
 * cap, a case the caller may not touch) raise through
 * callJson exactly like the sync twin's. The poll answers
 * the run's own state — {ok, runId, tool, status, recorded}
 * plus `result` when done or `error` when failed — and a
 * run is readable only by its starter or an OWNER/Admin:
 * anyone else (and any unknown id) gets the server's 403
 * "That CToS tool run does not exist."
 */
suspend fun SentinelApi.ctosRunToolAsync(tool: String, body: JsonObject): JsonObject =
    callJson("POST", "/api/app/osint/ctos/tools/$tool/async", body.toBodyC())

suspend fun SentinelApi.ctosToolRun(runId: String): JsonObject =
    callJson("GET", "/api/app/osint/ctos/runs/${encodeQuery(runId)}")

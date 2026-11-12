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

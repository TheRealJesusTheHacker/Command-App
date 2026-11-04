package net.sentinelsafeguard.app.data.api

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Phase 8B (hunt13) endpoint methods: the Admin console (13
 * sections + DMARC) and the 12 nav-injected entries, riding the
 * deployed Phase 8B backend (Addendum 26; production server.py
 * sha256 6ffee506…). Payloads are the web handlers' own, so these
 * ride the open-JSON verbs on [SentinelApi] exactly like Phases
 * 3–8A. Refusals surface the server's own message — never a fake
 * empty state.
 *
 * SECRECY (mirrors web): Law AI config is the handler's MASKED
 * public state only — no endpoint offers a raw key value and
 * none is ever requested or displayed. Shown-once values (API
 * keys, webhook secrets, survey tokens, regenerated recovery
 * codes) ride the creating call's response only; the app renders
 * them in-session exactly as web does and never persists or logs
 * them. The recovery-codes call carries web's current-passphrase
 * gate; the handler enforces it.
 *
 * REUSE (Addendum 26 accounting — not duplicated here): Admin
 * Members moderation, Invites, and Sessions ride the Phase 5
 * Employees + Phase 8A Activity endpoints; Danger-Zone exports
 * ride Phase 3; the Ops-Views evidence pack rides Phase 8A;
 * Pipeline & Field playbooks + per-ticket audio notes ride
 * Phase 3; Parts & Inventory rides Phase 3 in full; Reports
 * send/discard ride Phase 3's client-report routes; Admin Audit
 * is the Phase 8A Activity console plus the Oversight explorer.
 *
 * BACKUP DEFERRAL (named, Addendum 26): the backup-bundle FILE
 * download is a browser file route fed by the single-use token
 * the status handler issues — there is no binary app twin. The
 * methods below mirror the full state machine (status incl.
 * token issuance / request / discard) honestly.
 */

private fun JsonObject.toBody8b(): String = toString()

private fun SentinelApi.qs8b(params: Map<String, String?>): String {
    val parts = params.entries.filter { !it.value.isNullOrBlank() }
        .map { "${it.key}=${encodeQuery(it.value!!)}" }
    return if (parts.isEmpty()) "" else "?" + parts.joinToString("&")
}

// ---- Admin console ----

/** Admin landing payload (web getadmindata): users, invites,
 *  settings, security state, admin audit tail. */
suspend fun SentinelApi.adminData(): JsonObject =
    callJson("GET", "/api/app/admin")

suspend fun SentinelApi.adminHealthStrip(): JsonObject =
    callJson("GET", "/api/app/admin/health-strip")

suspend fun SentinelApi.adminRoles(): JsonObject =
    callJson("GET", "/api/app/admin/roles")

suspend fun SentinelApi.adminRoleCreate(role: JsonObject): JsonObject =
    callJson("POST", "/api/app/admin/roles", role.toBody8b())

suspend fun SentinelApi.adminRolePatch(name: String, fields: JsonObject): JsonObject =
    patchJson("/api/app/admin/roles/${encodeQuery(name)}", fields.toBody8b())

suspend fun SentinelApi.adminRoleTabs(name: String, tabs: JsonObject): JsonObject =
    callJson(
        "POST", "/api/app/admin/roles/${encodeQuery(name)}/tabs",
        tabs.toBody8b(),
    )

suspend fun SentinelApi.adminRoleDelete(name: String): JsonObject =
    deleteJson("/api/app/admin/roles/${encodeQuery(name)}")

/** Ops Report for a month (web getopsreport; blank = current). */
suspend fun SentinelApi.adminOpsReport(month: String? = null): JsonObject =
    callJson("GET", "/api/app/admin/ops-report" + qs8b(mapOf("month" to month)))

/** Provision an account (web provisionaccount; 201). The
 *  response carries delivery metadata only — no credential. */
suspend fun SentinelApi.adminMemberCreate(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/admin/members", body.toBody8b())

/** Owner-gated passphrase set (web setuserpassphrase — the
 *  handler's owner double-gate enforces). [passphrase] is a
 *  write-only input: sent once, never displayed back. */
suspend fun SentinelApi.adminMemberPassphrase(id: Int, passphrase: String): JsonObject =
    callJson(
        "POST", "/api/app/admin/members/$id/passphrase",
        buildJsonObject { put("passphrase", passphrase) }.toBody8b(),
    )

suspend fun SentinelApi.adminSettingsSave(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/admin/settings", body.toBody8b())

suspend fun SentinelApi.adminSecuritySave(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/admin/security", body.toBody8b())

/** Regenerate recovery codes (web regeneraterecoverycodes).
 *  [currentPassphrase] is web's gate, enforced by the handler.
 *  The returned codes are shown ONCE, in-session only — the
 *  caller must never persist or log them. */
suspend fun SentinelApi.adminRecoveryCodes(currentPassphrase: String): JsonObject =
    callJson(
        "POST", "/api/app/admin/recovery-codes",
        buildJsonObject { put("currentPassphrase", currentPassphrase) }.toBody8b(),
    )

/** Revoke ALL users' sessions (web revokeallusersessions). */
suspend fun SentinelApi.adminRevokeAllSessions(): JsonObject =
    callJson("POST", "/api/app/admin/sessions/revoke-all", "{}")

// ---- Security Oversight ----

suspend fun SentinelApi.adminRetention(): JsonObject =
    callJson("GET", "/api/app/admin/oversight/retention")

suspend fun SentinelApi.adminRetentionSet(entity: String, days: Int?): JsonObject =
    callJson(
        "POST", "/api/app/admin/oversight/retention",
        buildJsonObject {
            put("entity", entity)
            if (days != null) put("retentionDays", days)
            else put("retentionDays", kotlinx.serialization.json.JsonNull)
        }.toBody8b(),
    )

suspend fun SentinelApi.adminRetentionReport(): JsonObject =
    callJson("POST", "/api/app/admin/oversight/retention/report", "{}")

suspend fun SentinelApi.adminAuditLog(): JsonObject =
    callJson("GET", "/api/app/admin/oversight/audit-log")

suspend fun SentinelApi.adminAuditExport(): JsonObject =
    callJson("GET", "/api/app/admin/oversight/audit-export")

suspend fun SentinelApi.adminLoginSecurity(): JsonObject =
    callJson("GET", "/api/app/admin/oversight/login-security")

// ---- Law AI (masked public state only) ----

/** Law AI config — the handler's MASKED public state, exactly
 *  as web renders it. No raw key value exists in this payload. */
suspend fun SentinelApi.adminLawAi(): JsonObject =
    callJson("GET", "/api/app/admin/law-ai")

suspend fun SentinelApi.adminLawAiSave(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/admin/law-ai", body.toBody8b())

/** Key status: state shape only (installed / not), never a value. */
suspend fun SentinelApi.adminLawAiKeyStatus(): JsonObject =
    callJson("GET", "/api/app/admin/law-ai/key-status")

/** Install a Law AI key. [key] is a write-only input: sent once
 *  to the handler's install path, never displayed back. */
suspend fun SentinelApi.adminLawAiKeyInstall(key: String): JsonObject =
    callJson(
        "POST", "/api/app/admin/law-ai/key",
        buildJsonObject { put("key", key) }.toBody8b(),
    )

suspend fun SentinelApi.adminLawAiTest(): JsonObject =
    callJson("POST", "/api/app/admin/law-ai/test", "{}")

// ---- Backups & DMARC ----

/** Backup bundle state machine read (web backupbundlestatus —
 *  the handler also issues the single-use download token for
 *  web's file route; the app mirrors the state, per the named
 *  deferral, and never fakes a download). */
suspend fun SentinelApi.adminBackupStatus(): JsonObject =
    callJson("GET", "/api/app/admin/backups/status")

/** Request a bundle. [confirmText] must be web's exact confirm
 *  text — the handler enforces it. */
suspend fun SentinelApi.adminBackupRequest(confirmText: String): JsonObject =
    callJson(
        "POST", "/api/app/admin/backups/request",
        buildJsonObject { put("confirm", confirmText) }.toBody8b(),
    )

suspend fun SentinelApi.adminBackupDiscard(): JsonObject =
    callJson("POST", "/api/app/admin/backups/discard", "{}")

suspend fun SentinelApi.adminDmarc(days: Int = 30): JsonObject =
    callJson("GET", "/api/app/admin/dmarc" + qs8b(mapOf("days" to days.toString())))

// ---- Ops Views (web b3cops) ----

suspend fun SentinelApi.opsRiskHeatmap(): JsonObject =
    callJson("GET", "/api/app/ops-views/risk-heatmap")

suspend fun SentinelApi.opsTimeline(): JsonObject =
    callJson("GET", "/api/app/ops-views/timeline")

suspend fun SentinelApi.opsContractRadar(): JsonObject =
    callJson("GET", "/api/app/ops-views/contract-radar")

suspend fun SentinelApi.opsCapacity(): JsonObject =
    callJson("GET", "/api/app/ops-views/capacity")

suspend fun SentinelApi.opsCapacityThreshold(hours: Double): JsonObject =
    callJson(
        "POST", "/api/app/ops-views/capacity-threshold",
        buildJsonObject { put("thresholdHours", hours) }.toBody8b(),
    )

// ---- Pipeline & Field (web b3dfield + b4g field lane) ----

suspend fun SentinelApi.fieldJobMode(): JsonObject =
    callJson("GET", "/api/app/pipeline-field/job-mode")

suspend fun SentinelApi.fieldLeadScores(): JsonObject =
    callJson("GET", "/api/app/pipeline-field/lead-scores")

suspend fun SentinelApi.fieldLeadRoi(): JsonObject =
    callJson("GET", "/api/app/pipeline-field/lead-roi")

suspend fun SentinelApi.fieldImportPreview(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/pipeline-field/asset-import/preview", body.toBody8b())

suspend fun SentinelApi.fieldImportConfirm(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/pipeline-field/asset-import/confirm", body.toBody8b())

suspend fun SentinelApi.fieldSyncPush(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/pipeline-field/sync-push", body.toBody8b())

suspend fun SentinelApi.fieldScan(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/pipeline-field/scan", body.toBody8b())

suspend fun SentinelApi.fieldCounts(): JsonObject =
    callJson("GET", "/api/app/pipeline-field/counts")

suspend fun SentinelApi.fieldCountStart(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/pipeline-field/counts", body.toBody8b())

suspend fun SentinelApi.fieldCountScan(id: Int, body: JsonObject): JsonObject =
    callJson("POST", "/api/app/pipeline-field/counts/$id/scan", body.toBody8b())

suspend fun SentinelApi.fieldCountClose(id: Int): JsonObject =
    callJson("POST", "/api/app/pipeline-field/counts/$id/close", "{}")

suspend fun SentinelApi.fieldCountApply(id: Int): JsonObject =
    callJson("POST", "/api/app/pipeline-field/counts/$id/apply", "{}")

suspend fun SentinelApi.fieldPhotoOverlay(photoId: Int): JsonObject =
    callJson("GET", "/api/app/pipeline-field/photos/$photoId/overlay")

suspend fun SentinelApi.fieldPhotoOverlaySave(photoId: Int, body: JsonObject): JsonObject =
    callJson("POST", "/api/app/pipeline-field/photos/$photoId/overlay", body.toBody8b())

suspend fun SentinelApi.fieldPhotoOverlayDelete(photoId: Int): JsonObject =
    deleteJson("/api/app/pipeline-field/photos/$photoId/overlay")

// ---- Reports & Feedback (web cf2reports) ----

/** Cross-client monthly reports list (web cf2getclientreports). */
suspend fun SentinelApi.reportsList(clientId: Int? = null): JsonObject =
    callJson("GET", "/api/app/reports" + qs8b(mapOf("clientId" to clientId?.toString())))

suspend fun SentinelApi.reportsGenerate(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/reports", body.toBody8b())

suspend fun SentinelApi.reportDocument(id: Int): JsonObject =
    callJson("GET", "/api/app/reports/$id/document")

suspend fun SentinelApi.reportSurveys(): JsonObject =
    callJson("GET", "/api/app/reports/surveys")

/** Create a survey. The response's token is shown ONCE (web's
 *  own semantics) — render in-session, never persist. */
suspend fun SentinelApi.reportSurveyCreate(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/reports/surveys", body.toBody8b())

suspend fun SentinelApi.reportReferrals(): JsonObject =
    callJson("GET", "/api/app/reports/referrals")

suspend fun SentinelApi.reportReferralSave(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/reports/referrals", body.toBody8b())

suspend fun SentinelApi.reportBriefs(): JsonObject =
    callJson("GET", "/api/app/reports/threat-briefs")

suspend fun SentinelApi.reportBriefGenerate(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/reports/threat-briefs", body.toBody8b())

suspend fun SentinelApi.reportBriefMarkSent(id: Int): JsonObject =
    callJson("POST", "/api/app/reports/threat-briefs/$id/mark-sent", "{}")

suspend fun SentinelApi.reportBriefDelete(id: Int): JsonObject =
    deleteJson("/api/app/reports/threat-briefs/$id")

suspend fun SentinelApi.reportSchedules(): JsonObject =
    callJson("GET", "/api/app/reports/schedules")

suspend fun SentinelApi.reportScheduleSave(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/reports/schedules", body.toBody8b())

suspend fun SentinelApi.reportScheduleRun(id: Int): JsonObject =
    callJson("POST", "/api/app/reports/schedules/$id/run", "{}")

suspend fun SentinelApi.reportSchedulesRunDue(): JsonObject =
    callJson("POST", "/api/app/reports/schedules/run-due", "{}")

suspend fun SentinelApi.reportSnapshots(): JsonObject =
    callJson("GET", "/api/app/reports/snapshots")

// ---- App & Integrations (web b4cadmin, OWNER) ----

suspend fun SentinelApi.appIntegrations(): JsonObject =
    callJson("GET", "/api/app/app-integrations")

suspend fun SentinelApi.appDeviceRename(id: Int, name: String): JsonObject =
    callJson(
        "POST", "/api/app/app-integrations/devices/$id/rename",
        buildJsonObject { put("deviceName", name) }.toBody8b(),
    )

suspend fun SentinelApi.appDeviceRevoke(id: Int): JsonObject =
    callJson("POST", "/api/app/app-integrations/devices/$id/revoke", "{}")

/** Create a partner API key (201). The secret is shown ONCE in
 *  this response, exactly as web — render in-session only. */
suspend fun SentinelApi.appApiKeyCreate(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/app-integrations/api-keys", body.toBody8b())

suspend fun SentinelApi.appApiKeyRevoke(id: Int): JsonObject =
    callJson("POST", "/api/app/app-integrations/api-keys/$id/revoke", "{}")

/** Create a webhook (201). The signing secret is shown ONCE in
 *  this response, exactly as web — render in-session only. */
suspend fun SentinelApi.appWebhookCreate(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/app-integrations/webhooks", body.toBody8b())

suspend fun SentinelApi.appWebhookActive(id: Int, active: Boolean): JsonObject =
    callJson(
        "POST", "/api/app/app-integrations/webhooks/$id/active",
        buildJsonObject { put("active", active) }.toBody8b(),
    )

suspend fun SentinelApi.appDeliveriesRun(): JsonObject =
    callJson("POST", "/api/app/app-integrations/deliveries/run", "{}")

// ---- Client Import (web b4dimport) ----

suspend fun SentinelApi.clientImports(): JsonObject =
    callJson("GET", "/api/app/client-import")

suspend fun SentinelApi.clientImport(id: Int): JsonObject =
    callJson("GET", "/api/app/client-import/$id")

/** Dry-run preview: rows are staged for review — NOTHING becomes
 *  a live client at this step (web's contract, handler-proven). */
suspend fun SentinelApi.clientImportPreview(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/client-import/preview", body.toBody8b())

/** Confirm: stages into the onboarding review queue — still NOT
 *  a live-client insert (the backend battery proves zero). */
suspend fun SentinelApi.clientImportConfirm(id: Int): JsonObject =
    callJson("POST", "/api/app/client-import/$id/confirm", "{}")

// ---- Packets & Routes (web b4jops) ----

suspend fun SentinelApi.packetsOps(): JsonObject =
    callJson("GET", "/api/app/packets-routes/ops")

suspend fun SentinelApi.packetsList(): JsonObject =
    callJson("GET", "/api/app/packets-routes/packets")

suspend fun SentinelApi.packetBuild(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/packets-routes/packets", body.toBody8b())

suspend fun SentinelApi.packetMarkMailed(id: Int): JsonObject =
    callJson("POST", "/api/app/packets-routes/packets/$id/mark-mailed", "{}")

suspend fun SentinelApi.packetsRoute(date: String? = null): JsonObject =
    callJson("GET", "/api/app/packets-routes/route" + qs8b(mapOf("date" to date)))

suspend fun SentinelApi.packetsRouteOrder(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/packets-routes/route/order", body.toBody8b())

suspend fun SentinelApi.packetsConfig(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/packets-routes/config", body.toBody8b())

suspend fun SentinelApi.packetsUrgentAck(id: Int): JsonObject =
    callJson("POST", "/api/app/packets-routes/urgent/$id/ack", "{}")

suspend fun SentinelApi.packetsClaims(): JsonObject =
    callJson("GET", "/api/app/packets-routes/claims")

suspend fun SentinelApi.packetsClaimFile(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/packets-routes/claims", body.toBody8b())

suspend fun SentinelApi.packetsClaimTransition(id: Int, body: JsonObject): JsonObject =
    callJson("POST", "/api/app/packets-routes/claims/$id/transition", body.toBody8b())

suspend fun SentinelApi.packetsClaimAttach(id: Int, body: JsonObject): JsonObject =
    callJson("POST", "/api/app/packets-routes/claims/$id/attach", body.toBody8b())

suspend fun SentinelApi.packetsClaimReopen(id: Int): JsonObject =
    callJson("POST", "/api/app/packets-routes/claims/$id/reopen", "{}")

suspend fun SentinelApi.packetsGeneratePack(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/packets-routes/statements/generate-pack", body.toBody8b())

// ---- Collections sheet (web cf2OpenBoard) ----

suspend fun SentinelApi.collectionsBoard(): JsonObject =
    callJson("GET", "/api/app/collections")

/** Set an invoice's collections WORKFLOW state — the invoice's
 *  own status is never touched (handler-pinned). */
suspend fun SentinelApi.collectionsWorkflow(invoiceId: Int, body: JsonObject): JsonObject =
    callJson("POST", "/api/app/collections/invoices/$invoiceId/workflow", body.toBody8b())

// ---- Staff & Gear sheet (web cf2OpenStaff) ----

suspend fun SentinelApi.staffGearDirectory(): JsonObject =
    callJson("GET", "/api/app/staff-gear/directory")

suspend fun SentinelApi.staffGearTraining(): JsonObject =
    callJson("GET", "/api/app/staff-gear/training")

suspend fun SentinelApi.staffGearEquipment(): JsonObject =
    callJson("GET", "/api/app/staff-gear/equipment")

suspend fun SentinelApi.staffGearCertSave(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/staff-gear/certs", body.toBody8b())

suspend fun SentinelApi.staffGearCertDelete(id: Int): JsonObject =
    deleteJson("/api/app/staff-gear/certs/$id")

suspend fun SentinelApi.staffGearTrainingSave(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/staff-gear/training", body.toBody8b())

suspend fun SentinelApi.staffGearTrainingComplete(id: Int, body: JsonObject): JsonObject =
    callJson("POST", "/api/app/staff-gear/training/$id/complete", body.toBody8b())

suspend fun SentinelApi.staffGearEquipmentSave(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/staff-gear/equipment", body.toBody8b())

suspend fun SentinelApi.staffGearEquipmentReturn(id: Int): JsonObject =
    callJson("POST", "/api/app/staff-gear/equipment/$id/return", "{}")

suspend fun SentinelApi.staffGearEquipmentLinkMileage(id: Int, body: JsonObject): JsonObject =
    callJson("POST", "/api/app/staff-gear/equipment/$id/link-mileage", body.toBody8b())

suspend fun SentinelApi.staffGearEquipmentUnlinkMileage(id: Int): JsonObject =
    callJson("POST", "/api/app/staff-gear/equipment/$id/unlink-mileage", "{}")

// ---- Automation sheet (web cf2GOpen) ----

suspend fun SentinelApi.automationRules(): JsonObject =
    callJson("GET", "/api/app/automation/assign-rules")

suspend fun SentinelApi.automationRuleSave(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/automation/assign-rules", body.toBody8b())

suspend fun SentinelApi.automationRuleDelete(id: Int): JsonObject =
    deleteJson("/api/app/automation/assign-rules/$id")

suspend fun SentinelApi.automationRulesApply(): JsonObject =
    callJson("POST", "/api/app/automation/assign-rules/apply", "{}")

suspend fun SentinelApi.automationEscalationConfig(): JsonObject =
    callJson("GET", "/api/app/automation/escalation-config")

suspend fun SentinelApi.automationEscalationConfigSave(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/automation/escalation-config", body.toBody8b())

suspend fun SentinelApi.automationEscalations(): JsonObject =
    callJson("GET", "/api/app/automation/escalations")

suspend fun SentinelApi.automationEscalationScan(): JsonObject =
    callJson("POST", "/api/app/automation/escalations/scan", "{}")

suspend fun SentinelApi.automationQuoteFollowups(): JsonObject =
    callJson("GET", "/api/app/automation/quote-followups")

suspend fun SentinelApi.automationQuoteFollowupScan(): JsonObject =
    callJson("POST", "/api/app/automation/quote-followups/scan", "{}")

suspend fun SentinelApi.automationContractReminders(): JsonObject =
    callJson("GET", "/api/app/automation/contract-reminders")

suspend fun SentinelApi.automationContractReminderScan(): JsonObject =
    callJson("POST", "/api/app/automation/contract-reminders/scan", "{}")

suspend fun SentinelApi.automationContractTerm(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/automation/contract-term", body.toBody8b())

// ---- Client Ops sheet (web cf2OpsOpen, OWNER) ----

suspend fun SentinelApi.clientOpsWindows(clientId: Int): JsonObject =
    callJson("GET", "/api/app/client-ops/maintenance-windows" + qs8b(mapOf("clientId" to clientId.toString())))

suspend fun SentinelApi.clientOpsWindowSave(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/client-ops/maintenance-windows", body.toBody8b())

suspend fun SentinelApi.clientOpsWindowDelete(id: Int): JsonObject =
    deleteJson("/api/app/client-ops/maintenance-windows/$id")

suspend fun SentinelApi.clientOpsRetainers(clientId: Int): JsonObject =
    callJson("GET", "/api/app/client-ops/retainers" + qs8b(mapOf("clientId" to clientId.toString())))

suspend fun SentinelApi.clientOpsRetainerSave(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/client-ops/retainers", body.toBody8b())

suspend fun SentinelApi.clientOpsRetainerEntry(id: Int, body: JsonObject): JsonObject =
    callJson("POST", "/api/app/client-ops/retainers/$id/entries", body.toBody8b())

suspend fun SentinelApi.clientOpsRetainerDraw(id: Int, body: JsonObject): JsonObject =
    callJson("POST", "/api/app/client-ops/retainers/$id/draw", body.toBody8b())

suspend fun SentinelApi.clientOpsEmergency(clientId: Int): JsonObject =
    callJson("GET", "/api/app/client-ops/emergency-contacts" + qs8b(mapOf("clientId" to clientId.toString())))

suspend fun SentinelApi.clientOpsEmergencySave(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/client-ops/emergency-contacts", body.toBody8b())

suspend fun SentinelApi.clientOpsEmergencyDelete(id: Int): JsonObject =
    deleteJson("/api/app/client-ops/emergency-contacts/$id")

suspend fun SentinelApi.clientOpsEmergencyReorder(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/client-ops/emergency-contacts/reorder", body.toBody8b())

// ---- Pipeline & Packets sheet (web cf2OpenPipeline) ----

suspend fun SentinelApi.pipelineLeads(): JsonObject =
    callJson("GET", "/api/app/pipeline-packets/leads")

suspend fun SentinelApi.pipelineLeadSave(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/pipeline-packets/leads", body.toBody8b())

suspend fun SentinelApi.pipelineLeadState(id: Int, body: JsonObject): JsonObject =
    callJson("POST", "/api/app/pipeline-packets/leads/$id/state", body.toBody8b())

/** Convert a lead: enqueues ONE review-queue submission — never
 *  a live-client insert (backend-proven). */
suspend fun SentinelApi.pipelineLeadConvert(id: Int): JsonObject =
    callJson("POST", "/api/app/pipeline-packets/leads/$id/convert", "{}")

suspend fun SentinelApi.pipelineProposals(): JsonObject =
    callJson("GET", "/api/app/pipeline-packets/proposals")

suspend fun SentinelApi.pipelineProposalDocument(id: Int): JsonObject =
    callJson("GET", "/api/app/pipeline-packets/proposals/$id/document")

suspend fun SentinelApi.pipelineProposalStatus(id: Int, body: JsonObject): JsonObject =
    callJson("POST", "/api/app/pipeline-packets/proposals/$id/status", body.toBody8b())

suspend fun SentinelApi.pipelineChangeRequests(): JsonObject =
    callJson("GET", "/api/app/pipeline-packets/change-requests")

suspend fun SentinelApi.pipelineChangeStatus(id: Int, body: JsonObject): JsonObject =
    callJson("POST", "/api/app/pipeline-packets/change-requests/$id/status", body.toBody8b())

suspend fun SentinelApi.pipelineChangeVerify(id: Int, body: JsonObject): JsonObject =
    callJson("POST", "/api/app/pipeline-packets/change-requests/$id/verify", body.toBody8b())

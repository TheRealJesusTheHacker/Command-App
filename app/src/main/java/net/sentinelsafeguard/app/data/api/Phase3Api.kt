package net.sentinelsafeguard.app.data.api

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Phase 3 (hunt7) endpoint methods: Clients depth, Duplicates +
 * merge, the Sentinel Verified register, saved views / filter
 * presets / bulk, the ticket-ops family, ticket merge/split,
 * exports, Documents & Assurance, Engagements depth (contracts,
 * findings, library), and the Schedule booking-admin panels.
 *
 * Every method rides the deployed Phase 3 backend (Addendum 14 of
 * the sign-in diagnosis record; server.py sha256 5344879a…), whose
 * payloads are the web handlers' own — so these ride the open-JSON
 * verbs on [SentinelApi] and the screens render the web payload
 * section by section, exactly like the Today panels. Refusals
 * surface the server's own message (confirm gates, the
 * signed-authorization hard gate) via [SentinelApi.callJson].
 *
 * Request bodies mirror the rehearsal-captured shapes in
 * phase3-work/rehearsal_capture.py field for field.
 */

private fun JsonObject.toBody(): String = toString()

// ---- Clients depth: staff notes ----

suspend fun SentinelApi.clientNotes(clientId: Int): JsonObject =
    getJson("/api/app/clients/$clientId/notes")

suspend fun SentinelApi.createClientNote(clientId: Int, body: String, pinned: Boolean): JsonObject =
    postJson(
        "/api/app/clients/$clientId/notes",
        buildJsonObject { put("body", body); put("pinned", pinned) }.toBody(),
    )

suspend fun SentinelApi.editClientNote(clientId: Int, noteId: Int, body: String, pinned: Boolean): JsonObject =
    patchJson(
        "/api/app/clients/$clientId/notes/$noteId",
        buildJsonObject { put("body", body); put("pinned", pinned) }.toBody(),
    )

suspend fun SentinelApi.pinClientNote(clientId: Int, noteId: Int): JsonObject =
    postJson("/api/app/clients/$clientId/notes/$noteId/pin", "{}")

suspend fun SentinelApi.deleteClientNote(clientId: Int, noteId: Int): JsonObject =
    deleteJson("/api/app/clients/$clientId/notes/$noteId")

// ---- Clients depth: contacts + notification prefs ----

suspend fun SentinelApi.clientContacts(clientId: Int): JsonObject =
    getJson("/api/app/clients/$clientId/contacts")

suspend fun SentinelApi.saveClientContact(clientId: Int, contact: JsonObject, contactId: Int?): JsonObject {
    val body = buildJsonObject { put("contact", contact) }.toBody()
    return if (contactId == null) {
        postJson("/api/app/clients/$clientId/contacts", body)
    } else {
        patchJson("/api/app/clients/$clientId/contacts/$contactId", body)
    }
}

suspend fun SentinelApi.deleteClientContact(clientId: Int, contactId: Int): JsonObject =
    deleteJson("/api/app/clients/$clientId/contacts/$contactId")

suspend fun SentinelApi.inviteClientContact(clientId: Int, contactId: Int, username: String): JsonObject =
    callJson(
        "POST",
        "/api/app/clients/$clientId/contacts/$contactId/invite",
        buildJsonObject { put("username", username) }.toBody(),
    )

suspend fun SentinelApi.clientNotificationPrefs(clientId: Int): JsonObject =
    getJson("/api/app/clients/$clientId/notification-prefs")

suspend fun SentinelApi.setClientNotificationPrefs(clientId: Int, prefs: JsonObject): JsonObject =
    postJson(
        "/api/app/clients/$clientId/notification-prefs",
        buildJsonObject { put("prefs", prefs) }.toBody(),
    )

// ---- Clients depth: outreach ----

suspend fun SentinelApi.clientOutreachDraft(clientId: Int, channel: String): JsonObject =
    postJson(
        "/api/app/clients/$clientId/outreach-draft",
        buildJsonObject { put("channel", channel) }.toBody(),
    )

suspend fun SentinelApi.saveClientOutreachDraft(clientId: Int, channel: String, draft: String): JsonObject =
    postJson(
        "/api/app/clients/$clientId/outreach-draft/save",
        buildJsonObject { put("channel", channel); put("draft", draft) }.toBody(),
    )

// ---- Clients depth: security (score / roadmap / risks / breach / compliance) ----

suspend fun SentinelApi.clientSecurity(clientId: Int): JsonObject =
    getJson("/api/app/clients/$clientId/security")

suspend fun SentinelApi.createRoadmapItem(clientId: Int, title: String, detail: String, priority: String): JsonObject =
    postJson(
        "/api/app/clients/$clientId/security/roadmap",
        buildJsonObject {
            put("title", title); put("detail", detail); put("priority", priority)
        }.toBody(),
    )

suspend fun SentinelApi.generateRoadmap(clientId: Int): JsonObject =
    postJson("/api/app/clients/$clientId/security/roadmap/generate", "{}")

suspend fun SentinelApi.setRoadmapState(clientId: Int, itemId: Int, state: String): JsonObject =
    postJson(
        "/api/app/clients/$clientId/security/roadmap/$itemId/state",
        buildJsonObject { put("state", state) }.toBody(),
    )

suspend fun SentinelApi.archiveRoadmapItem(clientId: Int, itemId: Int): JsonObject =
    postJson("/api/app/clients/$clientId/security/roadmap/$itemId/archive", "{}")

suspend fun SentinelApi.createRisk(clientId: Int, title: String, detail: String, state: String): JsonObject =
    postJson(
        "/api/app/clients/$clientId/security/risks",
        buildJsonObject {
            put("title", title); put("detail", detail); put("state", state)
        }.toBody(),
    )

suspend fun SentinelApi.setRiskState(clientId: Int, riskId: Int, state: String): JsonObject =
    postJson(
        "/api/app/clients/$clientId/security/risks/$riskId/state",
        buildJsonObject { put("state", state) }.toBody(),
    )

suspend fun SentinelApi.createBreachWatch(clientId: Int, domain: String): JsonObject =
    postJson(
        "/api/app/clients/$clientId/security/breach-watches",
        buildJsonObject { put("domain", domain) }.toBody(),
    )

suspend fun SentinelApi.deleteBreachWatch(clientId: Int, watchId: Int): JsonObject =
    deleteJson("/api/app/clients/$clientId/security/breach-watches/$watchId")

suspend fun SentinelApi.runBreachMatch(clientId: Int): JsonObject =
    postJson("/api/app/clients/$clientId/security/breach-match", "{}")

suspend fun SentinelApi.seedCompliance(clientId: Int, framework: String): JsonObject =
    postJson(
        "/api/app/clients/$clientId/security/compliance/seed",
        buildJsonObject { put("framework", framework) }.toBody(),
    )

suspend fun SentinelApi.setComplianceItem(clientId: Int, itemId: Int, state: String): JsonObject =
    postJson(
        "/api/app/clients/$clientId/security/compliance",
        buildJsonObject { put("id", itemId); put("state", state) }.toBody(),
    )

// ---- Clients depth: site profiles + diagrams ----

suspend fun SentinelApi.clientSiteProfiles(clientId: Int): JsonObject =
    getJson("/api/app/clients/$clientId/site-profiles")

suspend fun SentinelApi.saveSiteProfile(clientId: Int, site: JsonObject, profileId: Int?): JsonObject {
    val body = buildJsonObject { put("site", site) }.toBody()
    return if (profileId == null) {
        postJson("/api/app/clients/$clientId/site-profiles", body)
    } else {
        // Web savesiteprofile is one save path; edits ride the same
        // POST with the profile id inside the site object.
        postJson("/api/app/clients/$clientId/site-profiles", body)
    }
}

suspend fun SentinelApi.deleteSiteProfile(clientId: Int, profileId: Int): JsonObject =
    deleteJson("/api/app/clients/$clientId/site-profiles/$profileId")

suspend fun SentinelApi.addSiteDiagram(
    clientId: Int,
    profileId: Int,
    fileName: String,
    contentType: String,
    dataBase64: String,
    sizeBytes: Int,
): JsonObject = postJson(
    "/api/app/clients/$clientId/site-profiles/$profileId/diagrams",
    buildJsonObject {
        put("fileName", fileName); put("contentType", contentType)
        put("dataBase64", dataBase64); put("sizeBytes", sizeBytes)
    }.toBody(),
)

suspend fun SentinelApi.deleteSiteDiagram(diagramId: Int): JsonObject =
    deleteJson("/api/app/site-diagrams/$diagramId")

// ---- Clients depth: hygiene / posture / health / offboard / mail /
// training / doc-expiry / score events / reports / digests ----

suspend fun SentinelApi.clientHygiene(clientId: Int): JsonObject =
    getJson("/api/app/clients/$clientId/hygiene")

suspend fun SentinelApi.setHygieneItem(clientId: Int, itemKey: String, state: String, note: String): JsonObject =
    postJson(
        "/api/app/clients/$clientId/hygiene",
        buildJsonObject {
            put("itemKey", itemKey); put("state", state); put("note", note)
        }.toBody(),
    )

suspend fun SentinelApi.clientPostureLetter(clientId: Int): JsonObject =
    getJson("/api/app/clients/$clientId/posture-letter")

suspend fun SentinelApi.sendPostureLetter(clientId: Int): JsonObject =
    postJson("/api/app/clients/$clientId/posture-letter/send", "{}")

suspend fun SentinelApi.clientHealthTimeline(clientId: Int): JsonObject =
    getJson("/api/app/clients/$clientId/health-timeline")

suspend fun SentinelApi.clientOffboard(clientId: Int): JsonObject =
    getJson("/api/app/clients/$clientId/offboard")

suspend fun SentinelApi.startOffboard(clientId: Int): JsonObject =
    postJson("/api/app/clients/$clientId/offboard/start", "{}")

suspend fun SentinelApi.runOffboardStep(clientId: Int, step: String): JsonObject =
    postJson(
        "/api/app/clients/$clientId/offboard/steps",
        buildJsonObject { put("step", step) }.toBody(),
    )

suspend fun SentinelApi.clientTraining(clientId: Int): JsonObject =
    getJson("/api/app/clients/$clientId/training")

suspend fun SentinelApi.clientDocExpiry(clientId: Int): JsonObject =
    getJson("/api/app/clients/$clientId/doc-expiry")

suspend fun SentinelApi.setClientDocExpiry(clientId: Int, body: JsonObject): JsonObject =
    postJson("/api/app/clients/$clientId/doc-expiry", body.toBody())

suspend fun SentinelApi.setClientAcademyOrg(clientId: Int, orgId: String): JsonObject =
    postJson(
        "/api/app/clients/$clientId/academy-org",
        buildJsonObject { put("orgId", orgId) }.toBody(),
    )

suspend fun SentinelApi.clientScoreEvents(clientId: Int): JsonObject =
    getJson("/api/app/clients/$clientId/score-events")

suspend fun SentinelApi.clientReports(clientId: Int): JsonObject =
    getJson("/api/app/clients/$clientId/reports")

suspend fun SentinelApi.generateClientReport(clientId: Int, period: String): JsonObject =
    postJson(
        "/api/app/clients/$clientId/reports/generate",
        buildJsonObject { put("period", period) }.toBody(),
    )

suspend fun SentinelApi.sendClientReport(reportId: Int): JsonObject =
    postJson("/api/app/client-reports/$reportId/send", "{}")

suspend fun SentinelApi.discardClientReport(reportId: Int): JsonObject =
    postJson("/api/app/client-reports/$reportId/discard", "{}")

suspend fun SentinelApi.clientDigests(): JsonObject =
    getJson("/api/app/client-digests")

suspend fun SentinelApi.generateClientDigests(): JsonObject =
    postJson("/api/app/client-digests/generate", "{}")

suspend fun SentinelApi.sendClientDigest(digestId: Int): JsonObject =
    postJson("/api/app/client-digests/$digestId/send", "{}")

suspend fun SentinelApi.discardClientDigest(digestId: Int): JsonObject =
    postJson("/api/app/client-digests/$digestId/discard", "{}")

// ---- Firm Pulse ----

suspend fun SentinelApi.firmPulse(): JsonObject = getJson("/api/app/firm-pulse")

suspend fun SentinelApi.firmPulseDrilldown(figure: String, key: String): JsonObject =
    getJson("/api/app/firm-pulse/drilldown?figure=${encodeQuery(figure)}&key=${encodeQuery(key)}")

// ---- Duplicates + merge (OWNER) ----

suspend fun SentinelApi.clientDuplicates(): JsonObject =
    getJson("/api/app/clients/duplicates")

suspend fun SentinelApi.clientDuplicateCount(): JsonObject =
    getJson("/api/app/clients/duplicate-count")

suspend fun SentinelApi.mergeClients(winnerId: Int, loserId: Int): JsonObject =
    callJson(
        "POST",
        "/api/app/clients/merge",
        buildJsonObject { put("winnerId", winnerId); put("loserId", loserId) }.toBody(),
    )

// ---- Sentinel Verified register ----

suspend fun SentinelApi.verifications(): JsonObject = getJson("/api/app/verifications")

suspend fun SentinelApi.issueVerification(clientId: Int, category: String): JsonObject =
    callJson(
        "POST", "/api/app/verifications",
        buildJsonObject { put("clientId", clientId); put("category", category) }.toBody(),
    )

suspend fun SentinelApi.revokeVerification(id: Int, confirm: Boolean): JsonObject =
    callJson(
        "POST",
        "/api/app/verifications/$id/revoke",
        buildJsonObject { put("confirm", confirm) }.toBody(),
    )

suspend fun SentinelApi.exportVerifications(): JsonObject =
    postJson("/api/app/verifications/export", "{}")

// ---- Saved views / filter presets / bulk ----

suspend fun SentinelApi.clientFilterPresets(): JsonObject =
    getJson("/api/app/client-filter-presets")

suspend fun SentinelApi.saveClientFilterPreset(name: String, spec: JsonObject): JsonObject =
    postJson(
        "/api/app/client-filter-presets",
        buildJsonObject { put("name", name); put("spec", spec) }.toBody(),
    )

suspend fun SentinelApi.deleteClientFilterPreset(id: Int): JsonObject =
    deleteJson("/api/app/client-filter-presets/$id")

suspend fun SentinelApi.savedViews(tab: String): JsonObject =
    getJson("/api/app/saved-views?tab=${encodeQuery(tab)}")

suspend fun SentinelApi.saveSavedView(tab: String, name: String, criteria: JsonObject): JsonObject =
    postJson(
        "/api/app/saved-views",
        buildJsonObject {
            put("tab", tab); put("name", name); put("criteria", criteria)
        }.toBody(),
    )

suspend fun SentinelApi.applySavedView(id: Int): JsonObject =
    postJson(
        "/api/app/saved-views/apply",
        buildJsonObject { put("id", id) }.toBody(),
    )

suspend fun SentinelApi.deleteSavedView(id: Int): JsonObject =
    deleteJson("/api/app/saved-views/$id")

suspend fun SentinelApi.bulkClients(ids: List<Int>, operation: JsonObject): JsonObject =
    postJson(
        "/api/app/clients/bulk",
        buildJsonObject {
            put(
                "ids",
                kotlinx.serialization.json.JsonArray(
                    ids.map { kotlinx.serialization.json.JsonPrimitive(it) },
                ),
            )
            put("operation", operation)
        }.toBody(),
    )

// ---- Ticket ops family ----

suspend fun SentinelApi.ticketOps(ticketId: Int): JsonObject =
    getJson("/api/app/tickets/$ticketId/ops")

suspend fun SentinelApi.ticketFollowUps(state: String = "open"): JsonObject =
    getJson("/api/app/ticket-followups?state=${encodeQuery(state)}")

suspend fun SentinelApi.setFollowUpState(id: Int, state: String): JsonObject =
    postJson(
        "/api/app/ticket-followups/$id/state",
        buildJsonObject { put("state", state) }.toBody(),
    )

suspend fun SentinelApi.ticketExpenses(ticketId: Int): JsonObject =
    getJson("/api/app/expenses?ticketId=$ticketId")

suspend fun SentinelApi.createExpense(body: JsonObject): JsonObject =
    postJson("/api/app/expenses", body.toBody())

suspend fun SentinelApi.deleteExpense(id: Int): JsonObject =
    deleteJson("/api/app/expenses/$id")

suspend fun SentinelApi.slaConfig(): JsonObject = getJson("/api/app/tickets/sla-config")

suspend fun SentinelApi.saveSlaConfig(priority: String, responseTargetMin: Int, resolutionTargetMin: Int): JsonObject =
    postJson(
        "/api/app/tickets/sla-config",
        buildJsonObject {
            put("priority", priority)
            put("responseTargetMin", responseTargetMin)
            put("resolutionTargetMin", resolutionTargetMin)
        }.toBody(),
    )

suspend fun SentinelApi.ticketPhotos(ticketId: Int): JsonObject =
    getJson("/api/app/tickets/$ticketId/photos")

/** hunt17 (final contract census): the case twin of
 *  [ticketPhotos] — the SAME web handler (a_cfgetphotos) keyed by
 *  caseId, the same body shape. The case profile carried no photo
 *  surface before this pass. */
suspend fun SentinelApi.casePhotos(caseId: Int): JsonObject =
    getJson("/api/app/cases/$caseId/photos")

suspend fun SentinelApi.uploadPhoto(
    ticketId: Int,
    bytes: ByteArray,
    fileName: String,
    caption: String,
    kind: String = "other",
    targetType: String = "ticket",
): JsonObject = uploadBytes(
    "/api/app/photos/upload", bytes,
    mapOf(
        "Content-Type" to "application/octet-stream",
        "X-Photo-Target-Type" to targetType,
        "X-Photo-Target-Id" to ticketId.toString(),
        "X-Photo-Kind" to kind,
        "X-File-Name" to fileName,
        "X-Photo-Caption" to caption,
    ),
)

suspend fun SentinelApi.deletePhoto(id: Int): JsonObject =
    deleteJson("/api/app/photos/$id")

suspend fun SentinelApi.ticketAudioNotes(ticketId: Int): JsonObject =
    getJson("/api/app/tickets/$ticketId/audio-notes")

suspend fun SentinelApi.uploadAudio(
    ticketId: Int,
    bytes: ByteArray,
    fileName: String,
    caption: String,
): JsonObject = uploadBytes(
    "/api/app/audio/upload", bytes,
    mapOf(
        "Content-Type" to "application/octet-stream",
        "X-Audio-Ticket-Id" to ticketId.toString(),
        "X-File-Name" to fileName,
        "X-Audio-Caption" to caption,
    ),
)

suspend fun SentinelApi.cannedResponses(): JsonObject = getJson("/api/app/canned-responses")

suspend fun SentinelApi.saveCannedResponse(response: JsonObject, id: Int?): JsonObject {
    val body = buildJsonObject { put("response", response) }.toBody()
    return if (id == null) postJson("/api/app/canned-responses", body)
    else patchJson("/api/app/canned-responses/$id", body)
}

suspend fun SentinelApi.deleteCannedResponse(id: Int): JsonObject =
    deleteJson("/api/app/canned-responses/$id")

suspend fun SentinelApi.renderCannedResponse(id: Int, ticketId: Int): JsonObject =
    postJson(
        "/api/app/canned-responses/$id/render",
        buildJsonObject { put("ticketId", ticketId) }.toBody(),
    )

suspend fun SentinelApi.timeEntries(ticketId: Int): JsonObject =
    getJson("/api/app/time-entries?ticketId=$ticketId")

suspend fun SentinelApi.startTimer(ticketId: Int, description: String, billable: Boolean): JsonObject =
    postJson(
        "/api/app/time-entries/start",
        buildJsonObject {
            put("ticketId", ticketId); put("description", description)
            put("billable", billable)
        }.toBody(),
    )

suspend fun SentinelApi.stopTimer(entryId: Int): JsonObject =
    postJson("/api/app/time-entries/$entryId/stop", "{}")

suspend fun SentinelApi.createTimeEntry(entry: JsonObject): JsonObject =
    postJson(
        "/api/app/time-entries",
        buildJsonObject { put("entry", entry) }.toBody(),
    )

suspend fun SentinelApi.editTimeEntry(entryId: Int, entry: JsonObject): JsonObject =
    patchJson(
        "/api/app/time-entries/$entryId",
        buildJsonObject { put("entry", entry) }.toBody(),
    )

suspend fun SentinelApi.deleteTimeEntry(id: Int): JsonObject =
    deleteJson("/api/app/time-entries/$id")

suspend fun SentinelApi.convertTimeToInvoice(clientId: Int, entryIds: List<Int>): JsonObject =
    postJson(
        "/api/app/time-entries/convert",
        buildJsonObject {
            put("clientId", clientId)
            put(
                "entryIds",
                kotlinx.serialization.json.JsonArray(
                    entryIds.map { kotlinx.serialization.json.JsonPrimitive(it) },
                ),
            )
        }.toBody(),
    )

suspend fun SentinelApi.playbooks(): JsonObject = getJson("/api/app/playbooks")

suspend fun SentinelApi.savePlaybook(playbook: JsonObject, id: Int?): JsonObject {
    val body = buildJsonObject { put("playbook", playbook) }.toBody()
    return if (id == null) postJson("/api/app/playbooks", body)
    else patchJson("/api/app/playbooks/$id", body)
}

suspend fun SentinelApi.applyPlaybook(id: Int, clientId: Int): JsonObject =
    postJson(
        "/api/app/playbooks/$id/apply",
        buildJsonObject { put("clientId", clientId) }.toBody(),
    )

suspend fun SentinelApi.checklists(): JsonObject = getJson("/api/app/checklists")

suspend fun SentinelApi.saveChecklistTemplate(name: String, items: kotlinx.serialization.json.JsonArray): JsonObject =
    postJson(
        "/api/app/checklists/templates",
        buildJsonObject { put("name", name); put("items", items) }.toBody(),
    )

suspend fun SentinelApi.startChecklistRun(templateId: Int, ticketId: Int): JsonObject =
    callJson(
        "POST", "/api/app/checklists/runs",
        buildJsonObject {
            put("templateId", templateId); put("ticketId", ticketId)
        }.toBody(),
    )

suspend fun SentinelApi.checklistRun(runId: Int): JsonObject =
    getJson("/api/app/checklists/runs/$runId")

suspend fun SentinelApi.completeChecklistItem(runId: Int, index: Int, done: Boolean): JsonObject =
    postJson(
        "/api/app/checklists/runs/$runId/items",
        buildJsonObject { put("index", index); put("done", done) }.toBody(),
    )

suspend fun SentinelApi.signoffChecklistRun(runId: Int, signoffName: String): JsonObject =
    postJson(
        "/api/app/checklists/runs/$runId/signoff",
        buildJsonObject { put("signoffName", signoffName) }.toBody(),
    )

/** hunt17 (final contract census): web's Complete-run action —
 *  a_cfcompletechecklistrun — distinct from sign-off: it closes
 *  the run; the app offered only sign-off before this pass. */
suspend fun SentinelApi.completeChecklistRun(runId: Int): JsonObject =
    postJson("/api/app/checklists/runs/$runId/complete", "{}")

suspend fun SentinelApi.plans(): JsonObject = getJson("/api/app/plans")

suspend fun SentinelApi.savePlan(plan: JsonObject, id: Int?): JsonObject {
    val body = buildJsonObject { put("plan", plan) }.toBody()
    return if (id == null) postJson("/api/app/plans", body)
    else patchJson("/api/app/plans/$id", body)
}

suspend fun SentinelApi.deletePlan(id: Int): JsonObject = deleteJson("/api/app/plans/$id")

/** hunt17 (final contract census): web's "run due plans now"
 *  action (a_runrecurringplans, POST /api/app/plans/run) — the
 *  Plans card listed and deleted plans but could not run them. */
suspend fun SentinelApi.runPlans(): JsonObject =
    postJson("/api/app/plans/run", "{}")

suspend fun SentinelApi.parts(): JsonObject = getJson("/api/app/parts")

suspend fun SentinelApi.savePart(item: JsonObject, id: Int?): JsonObject {
    val body = buildJsonObject { put("item", item) }.toBody()
    return if (id == null) postJson("/api/app/parts", body)
    else patchJson("/api/app/parts/$id", body)
}

suspend fun SentinelApi.consumePart(id: Int, ticketId: Int, qty: Int): JsonObject =
    postJson(
        "/api/app/parts/$id/consume",
        buildJsonObject { put("ticketId", ticketId); put("qty", qty) }.toBody(),
    )

suspend fun SentinelApi.restockPart(id: Int, qty: Int): JsonObject =
    postJson(
        "/api/app/parts/$id/restock",
        buildJsonObject { put("qty", qty) }.toBody(),
    )

// ---- Ticket merge / split ----

suspend fun SentinelApi.mergeTickets(survivorId: Int, duplicateId: Int, confirm: Boolean): JsonObject =
    callJson(
        "POST",
        "/api/app/tickets/merge",
        buildJsonObject {
            put("survivorId", survivorId); put("duplicateId", duplicateId)
            put("confirm", confirm)
        }.toBody(),
    )

suspend fun SentinelApi.splitTicket(ticketId: Int, newTitle: String, pivotActivityId: Int): JsonObject =
    postJson(
        "/api/app/tickets/$ticketId/split",
        buildJsonObject {
            put("newTitle", newTitle); put("pivotActivityId", pivotActivityId)
        }.toBody(),
    )

// ---- Exports (G-C9 decisions, Addendum 14) ----

suspend fun SentinelApi.exportFull(): JsonObject = getJson("/api/app/exports/full")

suspend fun SentinelApi.exportWorkspace(): JsonObject = getJson("/api/app/exports/workspace")

suspend fun SentinelApi.exportPriceBook(): JsonObject = getJson("/api/app/exports/price-book")

suspend fun SentinelApi.exportLabels(): JsonObject = getJson("/api/app/exports/labels")

suspend fun SentinelApi.exportPacket(batchId: Int): JsonObject =
    callJson("GET", "/api/app/exports/packet?batchId=$batchId")

// ---- Documents & Assurance (cf2assurance) ----

suspend fun SentinelApi.assurancePackets(clientId: Int? = null): JsonObject =
    getJson(
        if (clientId != null) "/api/app/assurance/packets?clientId=$clientId"
        else "/api/app/assurance/packets",
    )

suspend fun SentinelApi.saveAssurancePacket(clientId: Int, title: String, items: kotlinx.serialization.json.JsonArray): JsonObject =
    postJson(
        "/api/app/assurance/packets",
        buildJsonObject {
            put("clientId", clientId); put("title", title); put("items", items)
        }.toBody(),
    )

suspend fun SentinelApi.issueAssurancePacket(id: Int): JsonObject =
    postJson("/api/app/assurance/packets/$id/issue", "{}")

suspend fun SentinelApi.assuranceFileRequests(clientId: Int? = null): JsonObject =
    getJson(
        if (clientId != null) "/api/app/assurance/file-requests?clientId=$clientId"
        else "/api/app/assurance/file-requests",
    )

suspend fun SentinelApi.saveAssuranceFileRequest(clientId: Int, title: String, items: kotlinx.serialization.json.JsonArray): JsonObject =
    postJson(
        "/api/app/assurance/file-requests",
        buildJsonObject {
            put("clientId", clientId); put("title", title); put("items", items)
        }.toBody(),
    )

suspend fun SentinelApi.assuranceBackupTests(): JsonObject =
    getJson("/api/app/assurance/backup-tests")

suspend fun SentinelApi.saveAssuranceBackupTest(body: JsonObject): JsonObject =
    postJson("/api/app/assurance/backup-tests", body.toBody())

suspend fun SentinelApi.setBackupCadence(assetKey: String?, cadenceDays: Int): JsonObject =
    postJson(
        "/api/app/assurance/backup-cadence",
        buildJsonObject {
            put("cadenceDays", cadenceDays)
            if (!assetKey.isNullOrBlank()) put("assetKey", assetKey)
        }.toBody(),
    )

suspend fun SentinelApi.assuranceScans(clientId: Int? = null): JsonObject =
    getJson(
        if (clientId != null) "/api/app/assurance/scans?clientId=$clientId"
        else "/api/app/assurance/scans",
    )

suspend fun SentinelApi.saveAssuranceScan(body: JsonObject): JsonObject =
    callJson("POST", "/api/app/assurance/scans", body.toBody())

suspend fun SentinelApi.importScanFindings(scanId: Int, csvText: String): JsonObject =
    postJson(
        "/api/app/assurance/scans/import-findings",
        buildJsonObject { put("scanId", scanId); put("csvText", csvText) }.toBody(),
    )

suspend fun SentinelApi.assuranceDisposals(): JsonObject =
    getJson("/api/app/assurance/disposals")

suspend fun SentinelApi.startAssuranceDisposal(assetId: Int, method: String, reason: String): JsonObject =
    postJson(
        "/api/app/assurance/disposals",
        buildJsonObject {
            put("assetId", assetId); put("method", method); put("reason", reason)
        }.toBody(),
    )

suspend fun SentinelApi.attestAssuranceDisposal(id: Int, attestation: String): JsonObject =
    postJson(
        "/api/app/assurance/disposals/$id/attest",
        buildJsonObject { put("attestation", attestation) }.toBody(),
    )

suspend fun SentinelApi.completeAssuranceDisposal(id: Int): JsonObject =
    postJson("/api/app/assurance/disposals/$id/complete", "{}")

suspend fun SentinelApi.assuranceFirmDocuments(): JsonObject =
    getJson("/api/app/assurance/firm-documents")

suspend fun SentinelApi.saveAssuranceFirmDocument(document: JsonObject): JsonObject =
    postJson(
        "/api/app/assurance/firm-documents",
        buildJsonObject { put("document", document) }.toBody(),
    )

suspend fun SentinelApi.deleteAssuranceFirmDocument(id: Int): JsonObject =
    deleteJson("/api/app/assurance/firm-documents/$id")

suspend fun SentinelApi.assuranceKbShares(): JsonObject =
    getJson("/api/app/assurance/kb-shares")

suspend fun SentinelApi.createAssuranceKbShare(kbId: Int, expiresInDays: Int): JsonObject =
    postJson(
        "/api/app/assurance/kb-shares",
        buildJsonObject {
            put("kbId", kbId); put("expiresInDays", expiresInDays)
        }.toBody(),
    )

suspend fun SentinelApi.revokeAssuranceKbShare(id: Int): JsonObject =
    postJson("/api/app/assurance/kb-shares/$id/revoke", "{}")

// ---- Engagements depth: contracts / findings / library ----

suspend fun SentinelApi.contracts(): JsonObject = getJson("/api/app/contracts")

suspend fun SentinelApi.createContract(clientId: Int, templateType: String, title: String): JsonObject =
    postJson(
        "/api/app/contracts",
        buildJsonObject {
            put("clientId", clientId); put("templateType", templateType)
            put("title", title)
        }.toBody(),
    )

suspend fun SentinelApi.saveContractTemplate(id: Int, title: String, body: String): JsonObject =
    postJson(
        "/api/app/contracts/templates",
        buildJsonObject {
            put("id", id); put("title", title); put("body", body)
        }.toBody(),
    )

suspend fun SentinelApi.contractDocument(id: Int): JsonObject =
    getJson("/api/app/contracts/$id/document")

suspend fun SentinelApi.verifyContractHash(id: Int): JsonObject =
    postJson("/api/app/contracts/$id/verify-hash", "{}")

suspend fun SentinelApi.sendContract(id: Int): JsonObject =
    postJson("/api/app/contracts/$id/send", "{}")

suspend fun SentinelApi.countersignContract(id: Int, signerName: String): JsonObject =
    postJson(
        "/api/app/contracts/$id/countersign",
        buildJsonObject { put("signerName", signerName) }.toBody(),
    )

suspend fun SentinelApi.voidContract(id: Int, confirm: Boolean): JsonObject =
    callJson(
        "POST",
        "/api/app/contracts/$id/void",
        buildJsonObject { put("confirm", confirm) }.toBody(),
    )

suspend fun SentinelApi.createFinding(engagementId: Int, body: JsonObject): JsonObject =
    callJson("POST", "/api/app/engagements/$engagementId/findings", body.toBody())

suspend fun SentinelApi.editFinding(findingId: Int, body: JsonObject): JsonObject =
    patchJson("/api/app/findings/$findingId", body.toBody())

suspend fun SentinelApi.setFindingStatus(findingId: Int, status: String): JsonObject =
    postJson(
        "/api/app/findings/$findingId/status",
        buildJsonObject { put("status", status) }.toBody(),
    )

suspend fun SentinelApi.addFindingEvidence(
    findingId: Int,
    fileName: String,
    contentType: String,
    contentBase64: String,
    sizeBytes: Int,
): JsonObject = postJson(
    "/api/app/findings/$findingId/evidence",
    buildJsonObject {
        put("fileName", fileName); put("contentType", contentType)
        put("contentBase64", contentBase64); put("sizeBytes", sizeBytes)
    }.toBody(),
)

suspend fun SentinelApi.findingTemplates(): JsonObject = getJson("/api/app/finding-templates")

suspend fun SentinelApi.saveFindingTemplate(body: JsonObject, id: Int?): JsonObject =
    if (id == null) postJson("/api/app/finding-templates", body.toBody())
    else patchJson("/api/app/finding-templates/$id", body.toBody())

suspend fun SentinelApi.deleteFindingTemplate(id: Int): JsonObject =
    deleteJson("/api/app/finding-templates/$id")

suspend fun SentinelApi.createFindingFromTemplate(engagementId: Int, templateId: Int): JsonObject =
    postJson(
        "/api/app/engagements/$engagementId/findings/from-template",
        buildJsonObject { put("templateId", templateId) }.toBody(),
    )

suspend fun SentinelApi.engagementReportDocument(engagementId: Int): JsonObject =
    callJson("GET", "/api/app/engagements/$engagementId/report-document")

// ---- Schedule booking-admin panels ----

suspend fun SentinelApi.bookingAdmin(): JsonObject = getJson("/api/app/booking-admin")

suspend fun SentinelApi.saveBookingAvailability(body: JsonObject, id: Int?): JsonObject {
    return if (id == null) postJson("/api/app/booking-admin/availability", body.toBody())
    else patchJson("/api/app/booking-admin/availability/$id", body.toBody())
}

suspend fun SentinelApi.deleteBookingAvailability(id: Int): JsonObject =
    deleteJson("/api/app/booking-admin/availability/$id")

suspend fun SentinelApi.saveBookingService(body: JsonObject, id: Int?): JsonObject {
    return if (id == null) postJson("/api/app/booking-admin/services", body.toBody())
    else patchJson("/api/app/booking-admin/services/$id", body.toBody())
}

suspend fun SentinelApi.deleteBookingService(id: Int): JsonObject =
    deleteJson("/api/app/booking-admin/services/$id")

suspend fun SentinelApi.manageBooking(bookingId: Int, manageAction: String, newDate: String?): JsonObject =
    callJson(
        "POST", "/api/app/booking-admin/bookings/manage",
        buildJsonObject {
            put("bookingId", bookingId); put("manageAction", manageAction)
            if (newDate != null) put("newDate", newDate)
        }.toBody(),
    )

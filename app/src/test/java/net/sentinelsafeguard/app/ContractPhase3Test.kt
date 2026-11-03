package net.sentinelsafeguard.app

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
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
import net.sentinelsafeguard.app.data.api.addFindingEvidence
import net.sentinelsafeguard.app.data.api.addSiteDiagram
import net.sentinelsafeguard.app.data.api.applyPlaybook
import net.sentinelsafeguard.app.data.api.applySavedView
import net.sentinelsafeguard.app.data.api.archiveRoadmapItem
import net.sentinelsafeguard.app.data.api.assuranceBackupTests
import net.sentinelsafeguard.app.data.api.assuranceDisposals
import net.sentinelsafeguard.app.data.api.assuranceFileRequests
import net.sentinelsafeguard.app.data.api.assuranceFirmDocuments
import net.sentinelsafeguard.app.data.api.assuranceKbShares
import net.sentinelsafeguard.app.data.api.assurancePackets
import net.sentinelsafeguard.app.data.api.assuranceScans
import net.sentinelsafeguard.app.data.api.attestAssuranceDisposal
import net.sentinelsafeguard.app.data.api.bookingAdmin
import net.sentinelsafeguard.app.data.api.bulkClients
import net.sentinelsafeguard.app.data.api.cannedResponses
import net.sentinelsafeguard.app.data.api.checklistRun
import net.sentinelsafeguard.app.data.api.checklists
import net.sentinelsafeguard.app.data.api.clientContacts
import net.sentinelsafeguard.app.data.api.clientDigests
import net.sentinelsafeguard.app.data.api.clientDocExpiry
import net.sentinelsafeguard.app.data.api.clientDuplicateCount
import net.sentinelsafeguard.app.data.api.clientDuplicates
import net.sentinelsafeguard.app.data.api.clientFilterPresets
import net.sentinelsafeguard.app.data.api.clientHealthTimeline
import net.sentinelsafeguard.app.data.api.clientHygiene
import net.sentinelsafeguard.app.data.api.clientMail
import net.sentinelsafeguard.app.data.api.clientNotes
import net.sentinelsafeguard.app.data.api.clientNotificationPrefs
import net.sentinelsafeguard.app.data.api.clientOffboard
import net.sentinelsafeguard.app.data.api.clientOutreachDraft
import net.sentinelsafeguard.app.data.api.clientPostureLetter
import net.sentinelsafeguard.app.data.api.clientReports
import net.sentinelsafeguard.app.data.api.clientScoreEvents
import net.sentinelsafeguard.app.data.api.clientSecurity
import net.sentinelsafeguard.app.data.api.clientSiteProfiles
import net.sentinelsafeguard.app.data.api.clientTraining
import net.sentinelsafeguard.app.data.api.completeAssuranceDisposal
import net.sentinelsafeguard.app.data.api.completeChecklistItem
import net.sentinelsafeguard.app.data.api.consumePart
import net.sentinelsafeguard.app.data.api.contractDocument
import net.sentinelsafeguard.app.data.api.contracts
import net.sentinelsafeguard.app.data.api.convertTimeToInvoice
import net.sentinelsafeguard.app.data.api.countersignContract
import net.sentinelsafeguard.app.data.api.createAssuranceKbShare
import net.sentinelsafeguard.app.data.api.createBreachWatch
import net.sentinelsafeguard.app.data.api.createClientNote
import net.sentinelsafeguard.app.data.api.createContract
import net.sentinelsafeguard.app.data.api.createExpense
import net.sentinelsafeguard.app.data.api.createFinding
import net.sentinelsafeguard.app.data.api.createFindingFromTemplate
import net.sentinelsafeguard.app.data.api.createRisk
import net.sentinelsafeguard.app.data.api.createRoadmapItem
import net.sentinelsafeguard.app.data.api.createTimeEntry
import net.sentinelsafeguard.app.data.api.deleteAssuranceFirmDocument
import net.sentinelsafeguard.app.data.api.deleteBookingAvailability
import net.sentinelsafeguard.app.data.api.deleteBookingService
import net.sentinelsafeguard.app.data.api.deleteBreachWatch
import net.sentinelsafeguard.app.data.api.deleteCannedResponse
import net.sentinelsafeguard.app.data.api.deleteClientContact
import net.sentinelsafeguard.app.data.api.deleteClientFilterPreset
import net.sentinelsafeguard.app.data.api.deleteClientNote
import net.sentinelsafeguard.app.data.api.deleteExpense
import net.sentinelsafeguard.app.data.api.deleteFindingTemplate
import net.sentinelsafeguard.app.data.api.deletePhoto
import net.sentinelsafeguard.app.data.api.deletePlan
import net.sentinelsafeguard.app.data.api.deleteSavedView
import net.sentinelsafeguard.app.data.api.deleteSiteDiagram
import net.sentinelsafeguard.app.data.api.deleteSiteProfile
import net.sentinelsafeguard.app.data.api.deleteTimeEntry
import net.sentinelsafeguard.app.data.api.discardClientDigest
import net.sentinelsafeguard.app.data.api.discardClientReport
import net.sentinelsafeguard.app.data.api.editClientNote
import net.sentinelsafeguard.app.data.api.editFinding
import net.sentinelsafeguard.app.data.api.engagementReportDocument
import net.sentinelsafeguard.app.data.api.exportFull
import net.sentinelsafeguard.app.data.api.exportLabels
import net.sentinelsafeguard.app.data.api.exportPacket
import net.sentinelsafeguard.app.data.api.exportPriceBook
import net.sentinelsafeguard.app.data.api.exportVerifications
import net.sentinelsafeguard.app.data.api.exportWorkspace
import net.sentinelsafeguard.app.data.api.findingTemplates
import net.sentinelsafeguard.app.data.api.firmPulse
import net.sentinelsafeguard.app.data.api.firmPulseDrilldown
import net.sentinelsafeguard.app.data.api.generateClientDigests
import net.sentinelsafeguard.app.data.api.generateClientReport
import net.sentinelsafeguard.app.data.api.generateRoadmap
import net.sentinelsafeguard.app.data.api.importScanFindings
import net.sentinelsafeguard.app.data.api.inviteClientContact
import net.sentinelsafeguard.app.data.api.issueAssurancePacket
import net.sentinelsafeguard.app.data.api.issueVerification
import net.sentinelsafeguard.app.data.api.manageBooking
import net.sentinelsafeguard.app.data.api.mergeClients
import net.sentinelsafeguard.app.data.api.mergeTickets
import net.sentinelsafeguard.app.data.api.parts
import net.sentinelsafeguard.app.data.api.pinClientNote
import net.sentinelsafeguard.app.data.api.plans
import net.sentinelsafeguard.app.data.api.playbooks
import net.sentinelsafeguard.app.data.api.renderCannedResponse
import net.sentinelsafeguard.app.data.api.restockPart
import net.sentinelsafeguard.app.data.api.revokeAssuranceKbShare
import net.sentinelsafeguard.app.data.api.revokeVerification
import net.sentinelsafeguard.app.data.api.runBreachMatch
import net.sentinelsafeguard.app.data.api.runOffboardStep
import net.sentinelsafeguard.app.data.api.saveAssuranceBackupTest
import net.sentinelsafeguard.app.data.api.saveAssuranceFileRequest
import net.sentinelsafeguard.app.data.api.saveAssuranceFirmDocument
import net.sentinelsafeguard.app.data.api.saveAssurancePacket
import net.sentinelsafeguard.app.data.api.saveAssuranceScan
import net.sentinelsafeguard.app.data.api.saveBookingAvailability
import net.sentinelsafeguard.app.data.api.saveBookingService
import net.sentinelsafeguard.app.data.api.saveCannedResponse
import net.sentinelsafeguard.app.data.api.saveChecklistTemplate
import net.sentinelsafeguard.app.data.api.saveClientContact
import net.sentinelsafeguard.app.data.api.saveClientFilterPreset
import net.sentinelsafeguard.app.data.api.saveClientOutreachDraft
import net.sentinelsafeguard.app.data.api.saveContractTemplate
import net.sentinelsafeguard.app.data.api.saveFindingTemplate
import net.sentinelsafeguard.app.data.api.savePart
import net.sentinelsafeguard.app.data.api.savePlan
import net.sentinelsafeguard.app.data.api.savePlaybook
import net.sentinelsafeguard.app.data.api.saveSavedView
import net.sentinelsafeguard.app.data.api.saveSiteProfile
import net.sentinelsafeguard.app.data.api.savedViews
import net.sentinelsafeguard.app.data.api.seedCompliance
import net.sentinelsafeguard.app.data.api.sendClientDigest
import net.sentinelsafeguard.app.data.api.sendClientReport
import net.sentinelsafeguard.app.data.api.sendContract
import net.sentinelsafeguard.app.data.api.sendPostureLetter
import net.sentinelsafeguard.app.data.api.setBackupCadence
import net.sentinelsafeguard.app.data.api.setClientAcademyOrg
import net.sentinelsafeguard.app.data.api.setClientDocExpiry
import net.sentinelsafeguard.app.data.api.setClientNotificationPrefs
import net.sentinelsafeguard.app.data.api.setComplianceItem
import net.sentinelsafeguard.app.data.api.setFindingStatus
import net.sentinelsafeguard.app.data.api.setFollowUpState
import net.sentinelsafeguard.app.data.api.setHygieneItem
import net.sentinelsafeguard.app.data.api.setRiskState
import net.sentinelsafeguard.app.data.api.setRoadmapState
import net.sentinelsafeguard.app.data.api.signoffChecklistRun
import net.sentinelsafeguard.app.data.api.slaConfig
import net.sentinelsafeguard.app.data.api.splitTicket
import net.sentinelsafeguard.app.data.api.startAssuranceDisposal
import net.sentinelsafeguard.app.data.api.startChecklistRun
import net.sentinelsafeguard.app.data.api.startOffboard
import net.sentinelsafeguard.app.data.api.startTimer
import net.sentinelsafeguard.app.data.api.stopTimer
import net.sentinelsafeguard.app.data.api.ticketAudioNotes
import net.sentinelsafeguard.app.data.api.ticketExpenses
import net.sentinelsafeguard.app.data.api.ticketFollowUps
import net.sentinelsafeguard.app.data.api.ticketOps
import net.sentinelsafeguard.app.data.api.ticketPhotos
import net.sentinelsafeguard.app.data.api.timeEntries
import net.sentinelsafeguard.app.data.api.uploadAudio
import net.sentinelsafeguard.app.data.api.uploadPhoto
import net.sentinelsafeguard.app.data.api.verifications
import net.sentinelsafeguard.app.data.api.verifyContractHash
import net.sentinelsafeguard.app.data.api.voidContract
import net.sentinelsafeguard.app.ui.Phase3Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Phase 3 (hunt7) contract shapes: every Phase 3 endpoint driven
 * through the app's API methods against REAL bodies captured from
 * the deployed Phase 3 backend (see [RealBodiesPhase3]; server.py
 * sha256 5344879a…, Addendum 14). Refusal captures pin web's
 * confirm / hard-gate semantics: the server's own message rides
 * the ApiException, exactly as the mirror surfaces it.
 */
class ContractPhase3Test {

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
        RealBodiesPhase3.status(name) to RealBodiesPhase3.body(name)

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.content

    private fun JsonObject.int(key: String): Int? =
        (this[key] as? JsonPrimitive)?.content?.toIntOrNull()

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.arr(key: String): List<JsonObject> =
        (this[key] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

    private suspend fun expectRefusal(
        status: Int,
        messagePart: String,
        block: suspend () -> Unit,
    ) {
        try {
            block()
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(status, e.status)
            assertTrue(
                "message was: ${e.message}",
                e.message.contains(messagePart),
            )
        }
    }

    // ---- Clients depth: notes ----

    @Test
    fun notesListParsesRealRows() = runBlocking {
        val out = api(QueueTransport(step("client_notes_list"))).clientNotes(1)
        val notes = out.arr("notes")
        assertEquals(1, notes.size)
        assertEquals("First staff note", notes[0].str("body"))
        assertEquals("OwnerP3", notes[0].str("authorName"))
    }

    @Test
    fun noteWritesRideTheRealRoutes() = runBlocking {
        val transport = QueueTransport(
            step("client_note_create"), step("client_note_edit"),
            step("client_note_pin"), step("client_note_delete"),
        )
        val api = api(transport)
        val created = api.createClientNote(1, "First staff note", pinned = false)
        assertEquals("Note saved.", created.str("message"))
        api.editClientNote(1, 1, "First staff note (edited)", pinned = false)
        api.pinClientNote(1, 1)
        api.deleteClientNote(1, 1)
        assertEquals(
            listOf(
                "POST /api/app/clients/1/notes",
                "PATCH /api/app/clients/1/notes/1",
                "POST /api/app/clients/1/notes/1/pin",
                "DELETE /api/app/clients/1/notes/1",
            ),
            transport.requests.map { "${it.method} ${it.path}" },
        )
    }

    // ---- Clients depth: contacts & prefs ----

    @Test
    fun contactsListAndWrites() = runBlocking {
        val transport = QueueTransport(
            step("client_contacts_list"), step("client_contact_create"),
            step("client_contact_edit"), step("client_contact_delete"),
        )
        val api = api(transport)
        val list = api.clientContacts(1)
        assertEquals("Second Contact", list.arr("contacts")[0].str("name"))
        api.saveClientContact(
            1,
            buildJsonObject {
                put("name", "Second Contact"); put("email", "second@example.invalid")
                put("kind", "other"); put("active", true)
            },
            null,
        )
        api.saveClientContact(
            1,
            buildJsonObject { put("name", "Second Contact"); put("kind", "other") },
            1,
        )
        api.deleteClientContact(1, 1)
        assertEquals("DELETE", transport.requests.last().method)
    }

    @Test
    fun contactInviteRefusalSurfacesServerWords() = runBlocking {
        val api = api(QueueTransport(step("client_contact_invite_refusal")))
        expectRefusal(403, "username is required") {
            api.inviteClientContact(1, 1, "")
        }
    }

    @Test
    fun notificationPrefsRoundTrip() = runBlocking {
        val transport = QueueTransport(
            step("client_notification_prefs_get"), step("client_notification_prefs_set"),
        )
        val api = api(transport)
        val prefs = api.clientNotificationPrefs(1).obj("prefs")
        assertNotNull(prefs)
        assertEquals("true", prefs!!.str("ticketUpdates"))
        assertEquals("false", prefs.str("digest"))
        api.setClientNotificationPrefs(1, prefs)
        assertEquals("POST", transport.requests.last().method)
    }

    // ---- Clients depth: outreach / security ----

    @Test
    fun outreachDraftIsTheRealLetter() = runBlocking {
        val transport = QueueTransport(
            step("client_outreach_draft"), step("client_outreach_draft_save"),
        )
        val api = api(transport)
        val draft = api.clientOutreachDraft(1, "Email")
        assertTrue(draft.str("draft")!!.startsWith("Subject: A quick hello from Sentinel Safeguard"))
        api.saveClientOutreachDraft(1, "Email", draft.str("draft")!!)
    }

    @Test
    fun securitySectionParsesAndWrites() = runBlocking {
        val transport = QueueTransport(
            step("client_security_get"), step("client_security_roadmap_create"),
            step("client_security_roadmap_generate"), step("client_security_roadmap_state"),
            step("client_security_roadmap_archive"), step("client_security_risk_create"),
            step("client_security_risk_state"), step("client_security_breach_watch_create"),
            step("client_security_breach_watch_delete"), step("client_security_breach_match"),
            step("client_security_compliance_seed"), step("client_security_compliance_set"),
        )
        val api = api(transport)
        val security = api.clientSecurity(1)
        assertNotNull(security.obj("securityScore"))
        assertEquals(3, (security["complianceFrameworks"] as JsonArray).size)
        api.createRoadmapItem(1, "Enable MFA everywhere", "All accounts", "High")
        api.generateRoadmap(1)
        api.setRoadmapState(1, 1, "done")
        api.archiveRoadmapItem(1, 1)
        api.createRisk(1, "Open RDP", "Exposed to the internet", "open")
        api.setRiskState(1, 1, "mitigated")
        api.createBreachWatch(1, "example.invalid")
        api.deleteBreachWatch(1, 1)
        api.runBreachMatch(1)
        api.seedCompliance(1, "NIST CSF")
        api.setComplianceItem(1, 1, "done")
        assertEquals(12, transport.requests.size)
    }

    // ---- Clients depth: sites / hygiene / posture / health ----

    @Test
    fun siteProfilesAndDiagramWrites() = runBlocking {
        val transport = QueueTransport(
            step("client_site_profiles_list"), step("client_site_profile_create"),
            step("client_site_diagram_add"), step("site_diagram_delete"),
            step("client_site_profile_delete"),
        )
        val api = api(transport)
        val list = api.clientSiteProfiles(1)
        assertEquals(1, list.arr("siteProfiles").size)
        assertEquals(5242880, list.int("diagramMaxBytes"))
        api.saveSiteProfile(
            1,
            buildJsonObject { put("siteName", "HQ"); put("isp", "Example ISP") },
            null,
        )
        api.addSiteDiagram(1, 1, "diagram.png", "image/png", "aGVsbG8=", 5)
        api.deleteSiteDiagram(1)
        api.deleteSiteProfile(1, 1)
    }

    @Test
    fun diagramDownloadFlowsBytes() = runBlocking {
        // The capture records binary downloads by size + sha256
        // marker; the transport serves that marker body and the
        // app must stream exactly what the transport sends.
        val transport = QueueTransport(step("site_diagram_download"))
        val api = api(transport)
        var received: ByteArray? = null
        val status = api.downloadBytes("/api/app/site-diagrams/1/download") { received = it }
        assertEquals(200, status)
        assertNotNull(received)
        assertEquals(
            RealBodiesPhase3.body("site_diagram_download"),
            String(received!!, Charsets.UTF_8),
        )
        assertEquals("/api/app/site-diagrams/1/download", transport.requests[0].path)
    }

    @Test
    fun hygieneGetCountsMatchPhase3Logic() = runBlocking {
        val transport = QueueTransport(step("client_hygiene_get"), step("client_hygiene_set"))
        val api = api(transport)
        val hygiene = api.clientHygiene(1).obj("hygiene")!!
        val items = hygiene.arr("items")
        val (yes, no, unknown) = Phase3Logic.hygieneCounts(items)
        assertEquals(items.size, yes + no + unknown)
        api.setHygieneItem(1, "mfa", "yes", "")
    }

    @Test
    fun postureLetterAndHealthTimeline() = runBlocking {
        val transport = QueueTransport(
            step("client_posture_letter_get"), step("client_posture_letter_send"),
            step("client_health_timeline"),
        )
        val api = api(transport)
        val letter = api.clientPostureLetter(1).obj("letter")
        assertNotNull(letter)
        assertTrue(letter!!.str("subject")!!.isNotBlank())
        api.sendPostureLetter(1)
        val timeline = api.clientHealthTimeline(1)
        assertEquals(29, timeline.int("total"))
        assertEquals(25, timeline.arr("events").size)
    }

    @Test
    fun offboardTrainingDocExpiryScoreEvents() = runBlocking {
        val transport = QueueTransport(
            step("client_offboard_get"), step("client_offboard_start"),
            step("client_offboard_step"), step("client_training_get"),
            step("client_doc_expiry_get"), step("client_score_events_get"),
        )
        val api = api(transport)
        assertNotNull(api.clientOffboard(1).obj("offboard"))
        api.startOffboard(1)
        api.runOffboardStep(1, "export")
        assertEquals("unlinked", api.clientTraining(1).str("state"))
        assertEquals("none", api.clientDocExpiry(1).str("expiryState"))
        val events = api.clientScoreEvents(1)
        assertEquals(0, events.arr("events").size)
        assertTrue(events.str("note")!!.contains("Score changes"))
    }

    @Test
    fun docExpiryAndAcademyOrgWrites() = runBlocking {
        // No dedicated captures exist for these two POSTs (the
        // routes ride the Phase 3 backend per test_app_phase3.py);
        // the GET body answers, and the request shape is pinned.
        val transport = QueueTransport(
            step("client_doc_expiry_get"), step("client_doc_expiry_get"),
        )
        val api = api(transport)
        api.setClientDocExpiry(1, buildJsonObject { put("days", 30) })
        api.setClientAcademyOrg(1, "org-1")
        assertEquals(
            listOf(
                "POST /api/app/clients/1/doc-expiry",
                "POST /api/app/clients/1/academy-org",
            ),
            transport.requests.map { "${it.method} ${it.path}" },
        )
    }

    // ---- Clients depth: mail / reports / digests ----

    @Test
    fun clientMailShowsTheUnconfiguredBridgeHonestly() = runBlocking {
        val out = api(QueueTransport(step("client_mail_get"))).clientMail(1)
        assertEquals("false", out.str("configured"))
        assertTrue(out.str("note")!!.contains("Read-only"))
        assertTrue(out.str("message")!!.contains("not configured"))
    }

    @Test
    fun reportsAndDigestsFlows() = runBlocking {
        val transport = QueueTransport(
            step("client_reports_list"), step("client_report_generate"),
            step("client_report_send"), step("client_digests_list"),
            step("client_digests_generate"),
        )
        val api = api(transport)
        assertEquals(1, api.clientReports(1).arr("reports").size)
        api.generateClientReport(1, "2026-10")
        api.sendClientReport(1)
        assertEquals(0, api.clientDigests().arr("digests").size)
        api.generateClientDigests()
        assertEquals(5, transport.requests.size)
    }

    @Test
    fun reportAndDigestDiscardRoutes() = runBlocking {
        // Discard captures were not rehearsed separately; the
        // list bodies answer and the routes are pinned.
        val transport = QueueTransport(
            step("client_reports_list"), step("client_digests_list"),
        )
        val api = api(transport)
        api.discardClientReport(1)
        api.discardClientDigest(1)
        api.sendClientDigest(1)
        assertEquals("/api/app/client-reports/1/discard", transport.requests[0].path)
        assertEquals("/api/app/client-digests/1/discard", transport.requests[1].path)
        assertEquals("/api/app/client-digests/1/send", transport.requests[2].path)
    }

    // ---- Firm Pulse ----

    @Test
    fun firmPulseParsesAndDrillsDown() = runBlocking {
        val transport = QueueTransport(step("firm_pulse_get"), step("firm_pulse_drilldown"))
        val api = api(transport)
        val pulse = api.firmPulse()
        assertEquals(5, pulse.arr("aging").size)
        assertNotNull(pulse.obj("pipeline"))
        assertEquals(8, pulse.arr("ticketThroughput").size)
        val drill = api.firmPulseDrilldown("aging", "current")
        assertEquals("aging", drill.str("figure"))
        assertEquals("current", drill.str("key"))
    }

    // ---- Duplicates + merge + bulk ----

    @Test
    fun duplicatesPairAndSuggestedWinner() = runBlocking {
        val transport = QueueTransport(
            step("client_duplicates_list"), step("client_duplicate_count"),
            step("client_merge"),
        )
        val api = api(transport)
        val pairs = api.clientDuplicates().arr("pairs")
        assertEquals(1, pairs.size)
        val winner = Phase3Logic.suggestedWinnerId(pairs[0])
        assertNotNull(winner)
        assertEquals(1, api.clientDuplicateCount().int("count"))
        val merged = api.mergeClients(winner!!, if (winner == 1) 2 else 1)
        assertTrue(merged.str("message")!!.isNotBlank())
        assertTrue(transport.requests.last().body!!.contains("\"winnerId\":$winner"))
    }

    @Test
    fun clientsBulkAction() = runBlocking {
        val out = api(QueueTransport(step("clients_bulk"))).bulkClients(
            listOf(1, 2),
            buildJsonObject { put("kind", "stage"); put("stage", "Lead") },
        )
        assertTrue(out.str("message")!!.isNotBlank())
    }

    // ---- Sentinel Verified ----

    @Test
    fun verificationsRegisterPopulatedState() = runBlocking {
        val out = api(QueueTransport(step("verifications_list"))).verifications()
        val rows = out.arr("verifications")
        assertEquals(1, rows.size)
        assertEquals("SV-YYJ6ZU-DAS6L7", rows[0].str("code"))
        // Web's computed status: valid inside the window, expired after.
        assertEquals("Valid", Phase3Logic.verificationStatus(rows[0], "2026-11-03"))
        assertEquals("Expired", Phase3Logic.verificationStatus(rows[0], "2028-01-01"))
    }

    @Test
    fun verificationIssueRevokeExport() = runBlocking {
        val transport = QueueTransport(
            step("verification_issue"), step("verification_revoke"),
            step("verifications_export"),
        )
        val api = api(transport)
        val issued = api.issueVerification(1, "Security Assessment")
        assertEquals(201, RealBodiesPhase3.status("verification_issue"))
        assertEquals(
            "SV-YYJ6ZU-DAS6L7",
            issued.obj("verification")!!.str("code"),
        )
        api.revokeVerification(1, confirm = true)
        val export = api.exportVerifications()
        assertEquals(1, export.int("count"))
        assertEquals("Exported 1 verification(s).", export.str("message"))
    }

    @Test
    fun verificationRevokeRefusalSurfacesServerWords() = runBlocking {
        val api = api(QueueTransport(step("verification_revoke_refusal")))
        expectRefusal(403, "Revocation requires confirmation") {
            api.revokeVerification(1, confirm = false)
        }
    }

    // ---- Saved views & filter presets ----

    @Test
    fun filterPresetsCrud() = runBlocking {
        val transport = QueueTransport(
            step("client_filter_presets_list"), step("client_filter_preset_create"),
            step("client_filter_preset_delete"),
        )
        val api = api(transport)
        assertTrue(api.clientFilterPresets().arr("presets").isNotEmpty())
        api.saveClientFilterPreset("P3 preset", buildJsonObject { put("stage", "Lead") })
        api.deleteClientFilterPreset(1)
        assertEquals("DELETE", transport.requests.last().method)
    }

    @Test
    fun savedViewsApplyReturnsRealRows() = runBlocking {
        val transport = QueueTransport(
            step("saved_views_list"), step("saved_view_create"),
            step("saved_view_apply"), step("saved_view_delete"),
        )
        val api = api(transport)
        assertTrue(api.savedViews("clients").arr("views").isNotEmpty())
        api.saveSavedView("clients", "P3 view", buildJsonObject { put("stage", "Lead") })
        val applied = api.applySavedView(1)
        assertEquals(2, applied.arr("rows").size)
        assertEquals("Phase3 Client LLC", applied.arr("rows")[0].str("legalName"))
        assertEquals(
            "Lead",
            Phase3Logic.criteriaStage(applied.obj("criteria")),
        )
        api.deleteSavedView(1)
    }
}

/**
 * Phase 3 contract shapes, part 2: ticket ops, exports,
 * Documents & Assurance, engagements depth (contracts +
 * findings), booking admin, and the pure Phase3Logic helpers.
 */
class ContractPhase3OpsTest {

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
        RealBodiesPhase3.status(name) to RealBodiesPhase3.body(name)

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.content

    private fun JsonObject.int(key: String): Int? =
        (this[key] as? JsonPrimitive)?.content?.toIntOrNull()

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.arr(key: String): List<JsonObject> =
        (this[key] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

    private suspend fun expectRefusal(
        status: Int,
        messagePart: String,
        block: suspend () -> Unit,
    ) {
        try {
            block()
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(status, e.status)
            assertTrue("message was: ${e.message}", e.message.contains(messagePart))
        }
    }

    // ---- Ticket ops ----

    @Test
    fun ticketOpsEnvelopeParses() = runBlocking {
        val out = api(QueueTransport(step("ticket_ops_get"))).ticketOps(1)
        assertEquals(1, out.obj("ticket")!!.int("id"))
        assertNotNull(out.obj("ops"))
    }

    @Test
    fun followUpsAndExpenses() = runBlocking {
        val transport = QueueTransport(
            step("ticket_followups_list"), step("ticket_followup_state"),
            step("expenses_list"), step("expense_create"), step("expense_delete"),
        )
        val api = api(transport)
        assertEquals(2, api.ticketFollowUps("open").arr("followUps").size)
        api.setFollowUpState(1, "done")
        val expenses = api.ticketExpenses(1)
        assertEquals(1, expenses.arr("expenses").size)
        assertEquals(5, (expenses["categories"] as JsonArray).size)
        api.createExpense(buildJsonObject {
            put("ticketId", 1); put("category", "parts")
            put("description", "Cable"); put("amountCents", 1299)
        })
        api.deleteExpense(1)
    }

    @Test
    fun slaConfigGetAndSave() = runBlocking {
        val transport = QueueTransport(step("sla_config_get"), step("sla_config_save"))
        val api = api(transport)
        val config = api.slaConfig().obj("config")!!
        assertTrue(config.containsKey("Critical"))
        assertTrue(config.containsKey("Normal"))
        val saved = api.saveSlaConfig("Normal", 240, 1440)
        assertNotNull(saved)
        assertEquals("/api/app/tickets/sla-config", transport.requests.last().path)
    }

    @Test
    fun photoUploadRidesBytesAndHeaders() = runBlocking {
        val transport = QueueTransport(
            step("ticket_photos_list"), step("photo_upload"),
            step("photo_download"), step("photo_delete"),
        )
        val api = api(transport)
        assertTrue(api.ticketPhotos(1).containsKey("photos"))
        val bytes = byteArrayOf(1, 2, 3, 4)
        val uploaded = api.uploadPhoto(1, bytes, "site.jpg", "Before", "before")
        assertEquals("Photo stored.", uploaded.str("message"))
        val request = transport.requests[1]
        assertEquals("/api/app/photos/upload", request.path)
        assertTrue(request.bodyBytes!!.contentEquals(bytes))
        assertEquals("ticket", request.headers["X-Photo-Target-Type"])
        assertEquals("site.jpg", request.headers["X-File-Name"])
        var received: ByteArray? = null
        api.downloadBytes("/api/app/photos/1/download") { received = it }
        assertNotNull(received)
        api.deletePhoto(1)
    }

    @Test
    fun audioUploadAndList() = runBlocking {
        val transport = QueueTransport(step("ticket_audio_list"), step("audio_upload"))
        val api = api(transport)
        assertEquals(1, api.ticketAudioNotes(1).arr("audioNotes").size)
        val uploaded = api.uploadAudio(1, byteArrayOf(9, 9), "note.m4a", "Voice note")
        assertEquals("Voice note stored.", uploaded.str("message"))
        assertEquals("1", transport.requests[1].headers["X-Audio-Ticket-Id"])
    }

    @Test
    fun cannedResponsesCrudAndRender() = runBlocking {
        val transport = QueueTransport(
            step("canned_responses_list"), step("canned_response_create"),
            step("canned_response_render"), step("canned_response_delete"),
        )
        val api = api(transport)
        assertTrue(api.cannedResponses().arr("responses").isNotEmpty())
        api.saveCannedResponse(
            buildJsonObject { put("title", "Intro"); put("body", "Hello {client}") },
            null,
        )
        val rendered = api.renderCannedResponse(1, 1)
        assertTrue(rendered.str("text")!!.isNotBlank())
        api.deleteCannedResponse(1)
    }

    @Test
    fun timeTrackingLifecycle() = runBlocking {
        val transport = QueueTransport(
            step("time_entries_list"), step("time_entry_start"),
            step("time_entry_stop"), step("time_entry_create"),
            step("time_entry_delete"), step("time_entries_convert"),
        )
        val api = api(transport)
        val list = api.timeEntries(1)
        assertEquals(2, list.arr("entries").size)
        assertTrue(list["running"] is JsonNull)
        api.startTimer(1, "Diagnosis", billable = true)
        api.stopTimer(1)
        api.createTimeEntry(buildJsonObject {
            put("ticketId", 1); put("description", "Manual")
            put("startedAt", 1793728782000L); put("endedAt", 1793728785000L)
            put("billable", true)
        })
        api.deleteTimeEntry(1)
        val converted = api.convertTimeToInvoice(1, listOf(1))
        assertTrue(converted.str("message")!!.isNotBlank())
    }

    @Test
    fun playbooksChecklistsPlansParts() = runBlocking {
        val transport = QueueTransport(
            step("playbooks_list"), step("playbook_create"), step("playbook_apply"),
            step("checklists_list"), step("checklist_template_create"),
            step("checklist_run_start"), step("checklist_run_get"),
            step("checklist_run_item"), step("checklist_run_item2"),
            step("checklist_run_signoff"),
            step("plans_list"), step("plan_create"), step("plan_delete"),
            step("parts_list"), step("part_create"), step("part_consume"),
            step("part_restock"),
        )
        val api = api(transport)
        assertTrue(api.playbooks().arr("playbooks").isNotEmpty())
        api.savePlaybook(buildJsonObject { put("name", "P3 playbook") }, null)
        api.applyPlaybook(1, 1)
        assertTrue(api.checklists().arr("templates").isNotEmpty())
        api.saveChecklistTemplate("P3 checklist", JsonArray(listOf(JsonPrimitive("Step one"))))
        api.startChecklistRun(1, 1)
        val run = api.checklistRun(1).obj("run")!!
        assertEquals("completed_signed", run.str("status"))
        assertEquals("true", run.str("signed"))
        api.completeChecklistItem(1, 0, done = true)
        api.completeChecklistItem(1, 1, done = true)
        api.signoffChecklistRun(1, "Pat P")
        assertTrue(api.plans().arr("plans").isNotEmpty())
        api.savePlan(buildJsonObject {
            put("clientId", 1); put("name", "P3 plan"); put("amountCents", 5000)
        }, null)
        api.deletePlan(1)
        assertTrue(api.parts().arr("parts").isNotEmpty())
        api.savePart(buildJsonObject { put("name", "P3 part"); put("qty", 3) }, null)
        api.consumePart(1, 1, 1)
        api.restockPart(1, 2)
        assertEquals(17, transport.requests.size)
    }

    @Test
    fun ticketMergeSplitAndMergeGate() = runBlocking {
        val transport = QueueTransport(step("ticket_merge"), step("ticket_split"))
        val api = api(transport)
        val merged = api.mergeTickets(1, 2, confirm = true)
        assertTrue(merged.str("message")!!.isNotBlank())
        assertTrue(transport.requests[0].body!!.contains("\"confirm\":true"))
        api.splitTicket(1, "Split-off work", 1)

        val refusing = api(QueueTransport(step("ticket_merge_refusal")))
        expectRefusal(403, "Merging is permanent") {
            refusing.mergeTickets(1, 2, confirm = false)
        }
    }

    // ---- Exports ----

    @Test
    fun exportsPayloadsParse() = runBlocking {
        val transport = QueueTransport(
            step("export_full"), step("export_workspace"),
            step("export_price_book"), step("export_labels"),
        )
        val api = api(transport)
        val full = api.exportFull()
        assertEquals("sentinel-command-full-export.zip", full.str("filename"))
        assertEquals(465, full.int("totalRows"))
        assertNotNull(api.exportWorkspace().obj("export"))
        assertNotNull(api.exportPriceBook())
        assertEquals(0, api.exportLabels().arr("labels").size)
    }

    @Test
    fun exportPacketMissingIsA404() = runBlocking {
        val api = api(QueueTransport(step("export_packet_missing")))
        expectRefusal(404, "Not found.") { api.exportPacket(99999) }
    }

    // ---- Documents & Assurance ----

    @Test
    fun assurancePacketsAndFileRequests() = runBlocking {
        val transport = QueueTransport(
            step("assurance_packets_list"), step("assurance_packet_create"),
            step("assurance_packet_issue"), step("assurance_file_requests_list"),
            step("assurance_file_request_create"),
        )
        val api = api(transport)
        val packets = api.assurancePackets()
        assertEquals(1, packets.arr("packets").size)
        assertEquals(1, packets.arr("kbItems").size)
        api.saveAssurancePacket(
            1, "Welcome packet",
            JsonArray(listOf(buildJsonObject { put("kind", "kb"); put("id", 1) })),
        )
        api.issueAssurancePacket(1)
        assertTrue(api.assuranceFileRequests().containsKey("requests"))
        api.saveAssuranceFileRequest(
            1, "Docs please",
            JsonArray(listOf(buildJsonObject {
                put("label", "ID"); put("required", true)
            })),
        )
    }

    @Test
    fun assuranceBackupTestsAndCadence() = runBlocking {
        val transport = QueueTransport(
            step("assurance_backup_tests_list"), step("assurance_backup_test_create"),
            step("assurance_backup_cadence_set"),
        )
        val api = api(transport)
        val tests = api.assuranceBackupTests()
        val methods = (tests["methods"] as JsonArray).map { (it as JsonPrimitive).content }
        assertTrue("full_restore" in methods)
        assertEquals(3, (tests["results"] as JsonArray).size)
        assertEquals(90, tests.int("globalCadenceDays"))
        api.saveAssuranceBackupTest(buildJsonObject {
            put("assetId", 1); put("testedAt", "2026-11-03")
            put("method", "full_restore"); put("result", "pass"); put("notes", "")
        })
        api.setBackupCadence(1, 30)
    }

    @Test
    fun scanGateRefusalSurfacesWebsWords() = runBlocking {
        val refusing = api(QueueTransport(step("assurance_scan_refusal_no_authz")))
        expectRefusal(403, "Hard gate: a vulnerability scan can only be recorded") {
            refusing.saveAssuranceScan(buildJsonObject {
                put("clientId", 2); put("target", "203.0.113.0/24")
                put("scanDate", "2026-11-03"); put("tool", "nmap"); put("summary", "")
            })
        }
        assertTrue(
            Phase3Logic.isGateRefusal(
                "Hard gate: a vulnerability scan can only be recorded for a client " +
                    "with a fully signed pentest authorization on file",
            ),
        )
    }

    @Test
    fun assuranceScansImportDisposalsDocsShares() = runBlocking {
        val transport = QueueTransport(
            step("assurance_scans_list"), step("assurance_scan_create"),
            step("assurance_scan_import"), step("assurance_disposals_list"),
            step("assurance_disposal_start"), step("assurance_disposal_attest"),
            step("assurance_disposal_complete"), step("assurance_firm_documents_list"),
            step("assurance_firm_document_create"), step("assurance_firm_document_delete"),
            step("assurance_kb_shares_list"), step("assurance_kb_share_create"),
            step("assurance_kb_share_revoke"),
        )
        val api = api(transport)
        assertTrue(api.assuranceScans().arr("scans").isNotEmpty())
        api.saveAssuranceScan(buildJsonObject {
            put("clientId", 1); put("target", "192.0.2.10")
            put("scanDate", "2026-11-03"); put("tool", "nmap"); put("summary", "Clean")
        })
        api.importScanFindings(1, "Open port,Low,192.0.2.10,Port 22 open")
        assertTrue(api.assuranceDisposals().containsKey("disposals"))
        api.startAssuranceDisposal(1, "wipe", "Retired")
        api.attestAssuranceDisposal(1, "I attest the wipe completed.")
        api.completeAssuranceDisposal(1)
        assertTrue(api.assuranceFirmDocuments().containsKey("documents"))
        api.saveAssuranceFirmDocument(buildJsonObject {
            put("title", "Articles of Organization"); put("kind", "other"); put("notes", "")
        })
        api.deleteAssuranceFirmDocument(1)
        assertTrue(api.assuranceKbShares().containsKey("shares"))
        api.createAssuranceKbShare(1, 7)
        api.revokeAssuranceKbShare(1)
        assertEquals(13, transport.requests.size)
    }

    // ---- Engagements depth: contracts + findings ----

    @Test
    fun contractsTableTemplatesAndLifecycle() = runBlocking {
        val transport = QueueTransport(
            step("contracts_list"), step("contract_create"),
            step("contract_template_edit"), step("contract_document"),
            step("contract_verify_hash"), step("contract_send"),
            step("contract_countersign"), step("contract_void"),
        )
        val api = api(transport)
        val list = api.contracts()
        assertEquals(2, list.arr("templates").size)
        assertTrue(
            (list["contractTypes"] as JsonArray)
                .map { (it as JsonPrimitive).content }
                .contains("service_agreement"),
        )
        api.createContract(1, "service_agreement", "P3 contract")
        api.saveContractTemplate(1, "Service Agreement", "Body text")
        assertNotNull(api.contractDocument(1))
        assertTrue(api.verifyContractHash(1).containsKey("match"))
        api.sendContract(1)
        api.countersignContract(1, "Skyler Paul Henderson")
        api.voidContract(1, confirm = true)
        assertEquals(8, transport.requests.size)
    }

    @Test
    fun contractVoidRefusalSurfacesServerWords() = runBlocking {
        val api = api(QueueTransport(step("contract_void_refusal")))
        expectRefusal(403, "Voiding a contract requires confirmation") {
            api.voidContract(1, confirm = false)
        }
    }

    @Test
    fun findingsLifecycleAndLibrary() = runBlocking {
        val transport = QueueTransport(
            step("finding_create"), step("finding_edit"), step("finding_status"),
            step("finding_evidence"), step("finding_templates_list"),
            step("finding_template_create"), step("finding_from_template"),
            step("finding_template_delete"), step("engagement_report_document"),
        )
        val api = api(transport)
        val body = buildJsonObject {
            put("title", "Weak TLS"); put("severity", "High")
            put("description", "TLS 1.0 enabled"); put("impact", "")
            put("remediation", "Disable TLS 1.0")
        }
        api.createFinding(1, body)
        api.editFinding(1, body)
        api.setFindingStatus(1, "Resolved")
        api.addFindingEvidence(1, "shot.png", "image/png", "aGVsbG8=", 5)
        assertTrue(api.findingTemplates().arr("templates").isNotEmpty())
        api.saveFindingTemplate(buildJsonObject {
            put("title", "Weak TLS"); put("defaultSeverity", "High")
            put("description", ""); put("impact", ""); put("remediation", "")
            put("category", "")
        }, null)
        api.createFindingFromTemplate(1, 1)
        api.deleteFindingTemplate(1)
        val report = api.engagementReportDocument(1)
        assertTrue(report.str("filename")!!.isNotBlank())
        assertEquals(9, transport.requests.size)
    }

    // ---- Booking admin ----

    @Test
    fun bookingAdminPayloadAndWrites() = runBlocking {
        val transport = QueueTransport(
            step("booking_admin_get"), step("booking_availability_create"),
            step("booking_service_create"), step("booking_availability_delete"),
            step("booking_service_delete"),
        )
        val api = api(transport)
        val admin = api.bookingAdmin()
        assertTrue(admin.str("weekdayNote")!!.startsWith("Weekday: Monday=0"))
        assertEquals("Monday", Phase3Logic.weekdayLabel(0))
        assertEquals("Sunday", Phase3Logic.weekdayLabel(6))
        api.saveBookingAvailability(buildJsonObject {
            put("weekday", 1); put("startTime", "09:00")
            put("endTime", "17:00"); put("active", true)
        }, null)
        api.saveBookingService(buildJsonObject {
            put("name", "P3 Service"); put("durationMinutes", 60); put("active", true)
        }, null)
        api.deleteBookingAvailability(1)
        api.deleteBookingService(1)
    }

    @Test
    fun bookingManageRefusalSurfacesServerWords() = runBlocking {
        val api = api(QueueTransport(step("booking_manage_refusal")))
        expectRefusal(403, "Booking not found.") {
            api.manageBooking(99999, "cancel", null)
        }
    }

    // ---- Phase3Logic pure helpers ----

    @Test
    fun verificationStatusRevokedWins() {
        val revoked = buildJsonObject {
            put("status", "revoked"); put("expiryDate", "2027-11-03")
        }
        assertEquals("Revoked", Phase3Logic.verificationStatus(revoked, "2026-11-03"))
        val noStatus = buildJsonObject { put("expiryDate", "2027-11-03") }
        assertEquals("Valid", Phase3Logic.verificationStatus(noStatus, "2026-11-03"))
    }

    @Test
    fun durationAndTargetFormatting() {
        assertEquals("1h 30m", Phase3Logic.formatTargetMinutes(90))
        assertEquals("2h", Phase3Logic.formatTargetMinutes(120))
        assertEquals("45m", Phase3Logic.formatTargetMinutes(45))
        assertEquals("", Phase3Logic.formatTargetMinutes(null))
        assertEquals("1h 1m", Phase3Logic.formatDurationSeconds(3661))
        assertEquals("0m", Phase3Logic.formatDurationSeconds(0))
    }

    @Test
    fun criteriaAndWeekdayHelpers() {
        assertEquals("Lead", Phase3Logic.criteriaStage(buildJsonObject { put("stage", "Lead") }))
        assertEquals(null, Phase3Logic.criteriaStage(buildJsonObject { put("stage", "") }))
        assertEquals(null, Phase3Logic.criteriaStage(null))
        assertEquals("", Phase3Logic.weekdayLabel(7))
        assertEquals("", Phase3Logic.weekdayLabel(null))
        assertFalse(Phase3Logic.isGateRefusal("The server hit a problem."))
    }

    @Test
    fun displayValueFormatsCentsAndEpoch() {
        assertEquals(
            "$123.45",
            Phase3Logic.displayValue("amountCents", JsonPrimitive(12345)),
        )
        assertEquals(
            "Yes",
            Phase3Logic.displayValue("active", JsonPrimitive(true)),
        )
        assertEquals(null, Phase3Logic.displayValue("name", JsonNull))
    }
}

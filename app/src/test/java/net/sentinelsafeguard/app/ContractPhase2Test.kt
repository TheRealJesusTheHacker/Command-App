package net.sentinelsafeguard.app

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.sentinelsafeguard.app.config.ApiConfig
import net.sentinelsafeguard.app.data.api.CaseFullEdit
import net.sentinelsafeguard.app.data.api.EngagementEdit
import net.sentinelsafeguard.app.data.api.HttpRequest
import net.sentinelsafeguard.app.data.api.HttpResponse
import net.sentinelsafeguard.app.data.api.HttpTransport
import net.sentinelsafeguard.app.data.api.JobFullEdit
import net.sentinelsafeguard.app.data.api.RbStep
import net.sentinelsafeguard.app.data.api.ScheduleEntryRequest
import net.sentinelsafeguard.app.data.api.SentinelApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 2 (hunt6) contract shapes: every new endpoint driven
 * through the app's API methods against REAL bodies captured
 * from the deployed Phase 2 backend (see [RealBodiesPhase2];
 * server.py sha256 8146986d…, Addendum 12).
 */
class ContractPhase2Test {

    private class QueueTransport(vararg val steps: Pair<Int, String>) : HttpTransport {
        val requests = mutableListOf<HttpRequest>()
        private var index = 0
        override suspend fun execute(request: HttpRequest): HttpResponse {
            requests += request
            val step = steps[minOf(index, steps.size - 1)]
            index++
            return HttpResponse(step.first, step.second)
        }
    }

    private fun api(transport: HttpTransport): SentinelApi =
        SentinelApi(ApiConfig("https://command.example.test", "", ""), transport)

    private fun JsonObject.int(key: String): Int? =
        (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull()

    // ---- Cases ----

    @Test
    fun casesListParsesRowsWithClientNameAndTicketCount() = runBlocking {
        val page = api(QueueTransport(200 to RealBodiesPhase2.CASES_LIST)).casesFiltered()
        assertEquals(1, page.total)
        val case = page.items.single()
        assertEquals("Phase2 case alpha", case.title)
        assertEquals("Phase2 Client LLC", case.clientName)
        assertEquals(0, case.ticketCount)
        assertEquals("High", case.priority)
    }

    @Test
    fun casesFilteredRidesTheB3QueryParams() = runBlocking {
        val transport = QueueTransport(200 to RealBodiesPhase2.CASES_LIST_FILTERED)
        val page = api(transport).casesFiltered(q = "alpha", sort = "priority")
        assertEquals(1, page.total)
        val path = transport.requests.single().path
        assertTrue(path.contains("q=alpha"))
        assertTrue(path.contains("sort=priority"))
    }

    @Test
    fun caseDetailParsesProfileEnvelope() = runBlocking {
        val detail = api(QueueTransport(200 to RealBodiesPhase2.CASE_DETAIL)).caseDetail(1)
        assertEquals("Phase2 case alpha", detail.case.title)
        assertEquals("Phase2 Client LLC", detail.client!!.legalName)
        assertEquals("pat@example.invalid", detail.client!!.email)
        assertTrue(detail.tickets.isEmpty())
        assertEquals(1, detail.activity.size)
        assertEquals("Case_SAVED", detail.activity.single().action)
    }

    @Test
    fun caseCreateParsesThe201Body() = runBlocking {
        val transport = QueueTransport(201 to RealBodiesPhase2.CASE_CREATE)
        val created = api(transport).createCase(
            CaseFullEdit(
                clientId = 1, assignedUserId = null, title = "Phase2 case alpha",
                status = "Open", priority = "High", category = "Support",
                dueDate = "2020-01-01", notes = "alpha", adMaterial = false,
            )
        )
        assertEquals(1, created.id)
        assertEquals("High", created.priority)
        assertTrue(transport.requests.single().method == "POST")
    }

    @Test
    fun casePatchParsesTheEditedCase() = runBlocking {
        val patched = api(QueueTransport(200 to RealBodiesPhase2.CASE_PATCH)).patchCase(
            1,
            CaseFullEdit(
                clientId = 1, assignedUserId = null, title = "Phase2 case alpha",
                status = "Open", priority = "Critical", category = "Support",
                dueDate = "2020-01-01", notes = "alpha", adMaterial = false,
            ),
        )
        assertEquals("Critical", patched.priority)
    }

    @Test
    fun caseStatusParsesTheMovedCase() = runBlocking {
        val moved = api(QueueTransport(200 to RealBodiesPhase2.CASE_STATUS)).setCaseStatus(1, "In Progress")
        assertEquals("In Progress", moved.status)
    }

    @Test
    fun caseArchiveAndRestoreParse() = runBlocking {
        val archived = api(QueueTransport(200 to RealBodiesPhase2.CASE_ARCHIVE)).archiveCase(1)
        assertNotNull(archived.archivedAt)
        assertEquals(1, archived.ticketCount)
        val restored = api(QueueTransport(200 to RealBodiesPhase2.CASE_RESTORE)).restoreCase(1)
        assertNull(restored.archivedAt)
    }

    @Test
    fun ticketListRowsCarryCaseTitle() = runBlocking {
        val page = api(QueueTransport(200 to RealBodiesPhase2.TICKETS_LIST_CASETITLE)).ticketsFiltered()
        val ticket = page.items.single()
        assertEquals(1, ticket.caseId)
        assertEquals("Phase2 case alpha", ticket.caseTitle)
    }

    // ---- Schedule ----

    @Test
    fun jobCreateParsesAssigneesAndCaseTitle() = runBlocking {
        val job = api(QueueTransport(201 to RealBodiesPhase2.JOB_CREATE)).createJob(
            JobFullEdit(
                title = "Phase2 job", clientId = 1, caseId = 1, location = "Site A",
                workDate = "2026-11-10", startTime = "09:00", endTime = "10:00",
                allDay = false, status = "Scheduled", notes = "bring tools",
                assigneeIds = listOf(2),
            )
        )
        assertEquals("Phase2 job", job.title)
        assertEquals("Phase2 case alpha", job.caseTitle)
        assertEquals(listOf(2), job.assigneeIds)
        assertEquals("StaffP2", job.assignees.single().displayName)
    }

    @Test
    fun jobPatchAndStatusParse() = runBlocking {
        val patched = api(QueueTransport(200 to RealBodiesPhase2.JOB_PATCH)).patchJob(
            1,
            JobFullEdit(
                title = "Phase2 job", clientId = 1, caseId = 1, location = "Site B",
                workDate = "2026-11-10", startTime = "09:00", endTime = "10:00",
                allDay = false, status = "Scheduled", notes = "bring tools",
                assigneeIds = listOf(2),
            ),
        )
        assertEquals("Site B", patched.location)
        val moved = api(QueueTransport(200 to RealBodiesPhase2.JOB_STATUS)).setJobStatus(1, "In Progress")
        assertEquals("In Progress", moved.status)
    }

    @Test
    fun scheduleWindowParsesJobsEmployeesEntries() = runBlocking {
        val transport = QueueTransport(200 to RealBodiesPhase2.SCHEDULE_WINDOW)
        val window = api(transport).scheduleWindow("2026-11-09", "2026-11-16")
        assertEquals(1, window.jobs.size)
        assertEquals(2, window.employees.size)
        assertEquals("OwnerP2", window.employees[0].displayName)
        assertEquals("StaffP2", window.employees[1].displayName)
        assertTrue(window.entries.isEmpty())
        assertEquals("2026-11-09", window.range!!.startDate)
        assertEquals("2026-11-16", window.range!!.endDate)
        assertTrue(transport.requests.single().path.contains("start=2026-11-09"))
    }

    @Test
    fun scheduleEntrySaveParsesId() = runBlocking {
        val id = api(QueueTransport(200 to RealBodiesPhase2.ENTRY_SAVE)).saveScheduleEntry(
            ScheduleEntryRequest(
                userId = 2, entryType = "PTO", workDate = "2026-11-12", notes = "day off",
            )
        )
        assertEquals(1, id)
    }

    @Test
    fun jobDeleteAcceptsTheOkBody() = runBlocking {
        api(QueueTransport(200 to RealBodiesPhase2.JOB_DELETE)).deleteJob(1)
    }

    // ---- Engagements ----

    @Test
    fun engagementCreateParses() = runBlocking {
        val created = api(QueueTransport(201 to RealBodiesPhase2.ENGAGEMENT_CREATE)).createEngagement(
            EngagementEdit(
                clientId = 1, title = "Phase2 engagement", scope = "External",
                rulesOfEngagement = "Written authorization on file.", status = "Planned",
                startDate = "2026-11-05", endDate = "2026-11-20",
            )
        )
        assertEquals("Phase2 engagement", created.title)
        assertFalse(created.hasSignedAuthorization)
        assertTrue(created.findings.isEmpty())
    }

    @Test
    fun engagementsListCarriesVocabularies() = runBlocking {
        val list = api(QueueTransport(200 to RealBodiesPhase2.ENGAGEMENTS_LIST)).engagements()
        assertEquals(1, list.engagements.size)
        assertEquals(
            listOf("Planned", "Active", "On Hold", "Complete", "Cancelled"),
            list.engagementStatuses,
        )
        assertEquals(
            listOf("Critical", "High", "Medium", "Low", "Informational"),
            list.severities,
        )
    }

    @Test
    fun engagementDetailParses() = runBlocking {
        val eng = api(QueueTransport(200 to RealBodiesPhase2.ENGAGEMENT_DETAIL)).engagement(1)
        assertEquals("External", eng.scope)
        assertEquals("2026-11-05", eng.startDate)
    }

    // ---- Runbooks & sign-off ----

    @Test
    fun proceduresParseSteps() = runBlocking {
        val procedures = api(QueueTransport(200 to RealBodiesPhase2.RUNBOOK_PROCEDURES)).rbProcedures()
        val proc = procedures.single()
        assertEquals("Phase2 procedure", proc.title)
        assertTrue(proc.isProcedure)
        assertEquals(2, proc.stepCount)
        assertEquals("Step one", proc.steps[0].title)
    }

    @Test
    fun procedureSetParses() = runBlocking {
        val proc = api(QueueTransport(200 to RealBodiesPhase2.RUNBOOK_PROCEDURE_SET)).rbSetProcedure(
            1, true, listOf(RbStep("Step one"), RbStep("Step two")),
        )
        assertNotNull(proc)
        assertEquals(2, proc!!.steps.size)
    }

    @Test
    fun runStartParsesActiveRun() = runBlocking {
        val run = api(QueueTransport(201 to RealBodiesPhase2.RUNBOOK_RUN_START)).rbStartRun(1, 1)
        assertNotNull(run)
        assertEquals("active", run!!.status)
        assertEquals(2, run.totalSteps)
        assertEquals(0, run.doneCount)
        assertEquals("OwnerP2", run.startedByName)
    }

    @Test
    fun runsListFiltersByTicket() = runBlocking {
        val transport = QueueTransport(200 to RealBodiesPhase2.RUNBOOK_RUNS)
        val runs = api(transport).rbRuns(ticketId = 1)
        assertEquals(1, runs.size)
        assertTrue(transport.requests.single().path.contains("ticketId=1"))
    }

    @Test
    fun checkStepParsesProgress() = runBlocking {
        val run = api(QueueTransport(200 to RealBodiesPhase2.RUNBOOK_CHECK_STEP))
            .rbCheckStep(1, 0, "done")
        assertNotNull(run)
        assertEquals(1, run!!.doneCount)
        assertEquals("done", run.stepStates[0].state)
        assertEquals("OwnerP2", run.stepStates[0].byName)
    }

    @Test
    fun extractionStatusParses() = runBlocking {
        val ex = api(QueueTransport(200 to RealBodiesPhase2.RUNBOOK_EXTRACTION_STATUS)).rbExtraction()
        assertTrue(ex.summary.isEmpty())
        assertTrue(ex.attachments.isEmpty())
    }

    @Test
    fun signoffLifecycleParses() = runBlocking {
        val none = api(QueueTransport(200 to RealBodiesPhase2.SIGNOFF_GET_NONE)).ticketSignOff(1)
        assertNull(none.signOff)
        assertFalse(none.completedWithSignOff)
        val posted = api(QueueTransport(200 to RealBodiesPhase2.SIGNOFF_POST))
            .postTicketSignOff(1, "Pat P", 1)
        assertNotNull(posted.signOff)
        assertEquals("Pat P", posted.signOff!!.signerName)
        assertEquals("signed", posted.signOff!!.status)
        assertTrue(posted.signOff!!.documentHash.startsWith("b888"))
        val got = api(QueueTransport(200 to RealBodiesPhase2.SIGNOFF_GET)).ticketSignOff(1)
        assertTrue(got.completedWithSignOff)
        assertEquals("Closed", got.ticketStatus)
    }

    // ---- Today panels ----

    @Test
    fun todayPanelsParseSections() = runBlocking {
        val body = api(QueueTransport(200 to RealBodiesPhase2.TODAY_PANELS)).todayPanels()
        val panels = body["panels"]!!.jsonObject
        val security = panels["cf2Security"]!!.jsonObject
        assertEquals(1, security["hygieneGaps"]!!.jsonObject.int("total"))
        assertEquals(
            "Phase2 Client LLC",
            security["hygieneGaps"]!!.jsonObject["items"]!!.jsonArray[0]
                .jsonObject["clientName"]!!.jsonPrimitive.content,
        )
        assertNotNull(panels["sla"])
        assertNotNull(panels["b4jUrgent"])
        assertNotNull(panels["cf2Registers"])
        assertEquals(false, body["estateHealth"]!!.jsonObject["present"]!!.jsonPrimitive.content.toBoolean())
    }

    @Test
    fun todayAskParsesTheAnswer() = runBlocking {
        val transport = QueueTransport(200 to RealBodiesPhase2.TODAY_ASK)
        val answer = api(transport).todayAsk("how many clients")
        assertTrue(answer.answered)
        assertEquals("There are 1 clients.", answer.text)
        assertEquals("count", answer.kind)
        assertTrue(transport.requests.single().body!!.contains("\"q\""))
    }

    @Test
    fun todayAutomationsParse() = runBlocking {
        val body = api(QueueTransport(200 to RealBodiesPhase2.TODAY_AUTOMATIONS)).todayAutomations()
        assertEquals(0, body["today"]!!.jsonObject["staleTickets"]!!.jsonObject.int("total"))
        assertNotNull(body["config"])
    }

    @Test
    fun todayDigestsParseEmptyQueue() = runBlocking {
        val body = api(QueueTransport(200 to RealBodiesPhase2.TODAY_DIGESTS)).todayDigests()
        assertEquals(0, body["digests"]!!.jsonArray.size)
    }

    @Test
    fun estateAndPlatformDigestParseAbsent() = runBlocking {
        val estate = api(QueueTransport(200 to RealBodiesPhase2.TODAY_ESTATE_HEALTH)).todayEstateHealth()
        assertEquals("false", estate["present"]!!.jsonPrimitive.content)
        val digest = api(QueueTransport(200 to RealBodiesPhase2.TODAY_PLATFORM_DIGEST)).todayPlatformDigest()
        assertEquals("false", digest["present"]!!.jsonPrimitive.content)
    }

    @Test
    fun calendarSyncParsesPreparedState() = runBlocking {
        val sync = api(QueueTransport(200 to RealBodiesPhase2.TODAY_CALENDAR_SYNC)).todayCalendarSync()
        assertTrue(sync.enabled)
        assertTrue(sync.prepared)
        assertFalse(sync.live)
        assertTrue(sync.note.contains("Prepared, not live"))
    }

    @Test
    fun reportsSummaryParses() = runBlocking {
        val body = api(QueueTransport(200 to RealBodiesPhase2.TODAY_REPORTS_SUMMARY)).todayReportsSummary()
        assertEquals(0, body["reports"]!!.jsonObject["reports"]!!.jsonArray.size)
        assertEquals(0, body["surveys"]!!.jsonObject["surveys"]!!.jsonArray.size)
        assertEquals(0, body["threatBriefs"]!!.jsonObject["briefs"]!!.jsonArray.size)
    }

    @Test
    fun staffCertsParseDirectory() = runBlocking {
        val body = api(QueueTransport(200 to RealBodiesPhase2.TODAY_STAFF_CERTS)).todayStaffCerts()
        val staff = body["directory"]!!.jsonObject["staff"]!!.jsonArray
        assertEquals(2, staff.size)
        assertEquals(
            "StaffP2",
            staff[1].jsonObject["displayName"]!!.jsonPrimitive.content,
        )
    }

    @Test
    fun registersParseRollup() = runBlocking {
        val body = api(QueueTransport(200 to RealBodiesPhase2.TODAY_REGISTERS)).todayRegisters(clientId = 1)
        val licenses = body["licenses"]!!.jsonObject
        assertEquals(0, licenses["rollup"]!!.jsonObject.int("licenseCount"))
        assertNotNull(body["todayRegisters"])
    }

    @Test
    fun urgentParsesBannerData() = runBlocking {
        val body = api(QueueTransport(200 to RealBodiesPhase2.TODAY_URGENT)).todayUrgent()
        val urgent = body["urgent"]!!.jsonObject
        assertEquals(0, urgent.int("total"))
        assertTrue(urgent["latencyNote"]!!.jsonPrimitive.content.contains("banner"))
    }

    // ---- Global search ----

    @Test
    fun searchParsesGroupedResults() = runBlocking {
        val transport = QueueTransport(200 to RealBodiesPhase2.SEARCH)
        val results = api(transport).search("Phase2")
        assertEquals("Phase2", results.q)
        assertEquals("Phase2 Client LLC", results.groups["clients"]!!.single().title)
        assertEquals("Phase2 case alpha", results.groups["cases"]!!.single().title)
        assertEquals("Phase2 ticket", results.groups["tickets"]!!.single().title)
        assertEquals("Phase2 engagement", results.groups["engagements"]!!.single().title)
        assertEquals("Phase2 procedure", results.groups["knowledge"]!!.single().title)
        assertTrue(results.groups["assets"]!!.isEmpty())
        assertTrue(transport.requests.single().path.contains("q=Phase2"))
    }
}

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
import net.sentinelsafeguard.app.data.api.activityAll
import net.sentinelsafeguard.app.data.api.activityPage
import net.sentinelsafeguard.app.data.api.activityRevokeSession
import net.sentinelsafeguard.app.data.api.activitySessions
import net.sentinelsafeguard.app.data.api.activityVisitors
import net.sentinelsafeguard.app.data.api.deskDetail
import net.sentinelsafeguard.app.data.api.deskEvidencePack
import net.sentinelsafeguard.app.data.api.deskIssueBadge
import net.sentinelsafeguard.app.data.api.deskList
import net.sentinelsafeguard.app.data.api.deskRevokeBadge
import net.sentinelsafeguard.app.data.api.deskSave
import net.sentinelsafeguard.app.data.api.deskStart
import net.sentinelsafeguard.app.data.api.deskState
import net.sentinelsafeguard.app.data.api.firmAvailCreate
import net.sentinelsafeguard.app.data.api.firmAvailDelete
import net.sentinelsafeguard.app.data.api.firmAvailPatch
import net.sentinelsafeguard.app.data.api.firmAvailability
import net.sentinelsafeguard.app.data.api.firmBookingWarnings
import net.sentinelsafeguard.app.data.api.firmJobCosting
import net.sentinelsafeguard.app.data.api.firmJobCosts
import net.sentinelsafeguard.app.data.api.firmTaskConvert
import net.sentinelsafeguard.app.data.api.firmTaskCreate
import net.sentinelsafeguard.app.data.api.firmTaskPatch
import net.sentinelsafeguard.app.data.api.firmTaskState
import net.sentinelsafeguard.app.data.api.firmTasks
import net.sentinelsafeguard.app.data.api.firmVendorCreate
import net.sentinelsafeguard.app.data.api.firmVendorDelete
import net.sentinelsafeguard.app.data.api.firmVendorPatch
import net.sentinelsafeguard.app.data.api.firmVendors
import net.sentinelsafeguard.app.data.api.portalAssign
import net.sentinelsafeguard.app.data.api.portalCurrentTip
import net.sentinelsafeguard.app.data.api.portalOnePager
import net.sentinelsafeguard.app.data.api.portalSecretCreate
import net.sentinelsafeguard.app.data.api.portalSecrets
import net.sentinelsafeguard.app.data.api.portalSubs
import net.sentinelsafeguard.app.data.api.portalTipCreate
import net.sentinelsafeguard.app.data.api.portalTipDelete
import net.sentinelsafeguard.app.data.api.portalTipPatch
import net.sentinelsafeguard.app.data.api.portalTips
import net.sentinelsafeguard.app.data.api.portalUnassign
import net.sentinelsafeguard.app.ui.Phase3Logic
import net.sentinelsafeguard.app.ui.Phase8Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Phase 8A (hunt12) contract shapes: every Phase 8A endpoint
 * driven through the app's API methods against REAL bodies
 * captured from the deployed Phase 8A backend (see
 * [RealBodiesPhase8]; Addendum 24) — including the read-gate
 * 403 bodies, the metadata-only secret surface, the
 * {record:{…}} availability shape, the Desk workflow bodies,
 * and the Activity console's paging walk.
 */
class ContractPhase8Test {

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
            return HttpResponse(200, "")
        }
    }

    /** Serves the captured console rows sliced by the request's
     *  own offset, capped at [pageCap] rows per page — the
     *  handler's limit+offset paging, so the walker must page
     *  by offset until the payload total is collected. */
    private class SlicingTransport(
        private val allRows: JsonArray,
        private val total: Int,
        private val pageCap: Int,
    ) : HttpTransport {
        val requests = mutableListOf<HttpRequest>()
        override suspend fun execute(request: HttpRequest): HttpResponse {
            requests += request
            val offset = Regex("offset=(\\d+)").find(request.path)
                ?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val slice = allRows.drop(offset).take(pageCap)
            val body = buildJsonObject {
                put("rows", JsonArray(slice))
                put("total", total)
                put("offset", offset)
                put("limit", pageCap)
                put("ok", true)
            }
            return HttpResponse(200, body.toString())
        }

        override suspend fun download(
            request: HttpRequest,
            sink: (ByteArray) -> Unit,
        ): HttpResponse = HttpResponse(200, "")
    }

    private fun api(transport: HttpTransport): SentinelApi =
        SentinelApi(ApiConfig("https://command.example.test", "", ""), transport)

    private fun step(name: String): Pair<Int, String> =
        RealBodiesPhase8.status(name) to RealBodiesPhase8.body(name)

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.content

    private fun JsonObject.int(key: String): Int? =
        (this[key] as? JsonPrimitive)?.content?.toIntOrNull()

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.arr(key: String): List<JsonObject> =
        (this[key] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

    // ---- Gates ----

    @Test
    fun gateBodiesRefuseInServerWords() = runBlocking {
        try {
            api(QueueTransport(step("GET /api/app/portal-settings/tips (staff 403)"))).portalTips()
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(403, e.status)
            assertEquals("Not authorized.", e.message)
        }
        try {
            api(QueueTransport(step("GET /api/app/firm-ops/job-costs (staff 403)"))).firmJobCosts()
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(403, e.status)
            assertEquals("Owner only.", e.message)
        }
        try {
            api(QueueTransport(step("GET /api/app/activity (staff 403)"))).activityPage()
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(403, e.status)
            assertEquals("Not authorized.", e.message)
        }
    }

    @Test
    fun unauthenticatedIsSignedOut() = runBlocking {
        // The 401 shape is the backend battery's proven refusal
        // (Addendum 24: every new GET route answers 401 tokenless);
        // the status alone drives the app's sign-out path.
        try {
            api(QueueTransport(401 to """{"ok":false,"message":"Sign-in required."}""")).portalTips()
            fail("expected a SignedOutException")
        } catch (e: SignedOutException) {
            assertEquals("Your sign-in has ended. Please sign in again.", e.message)
        }
    }

    // ---- Portal Settings ----

    @Test
    fun portalSubsListAssignUnassign() = runBlocking {
        val listT = QueueTransport(step("GET /api/app/portal-settings/sub-assignments"))
        val list = api(listT).portalSubs()
        assertEquals("/api/app/portal-settings/sub-assignments", listT.requests[0].path)
        val assignments = list.arr("assignments")
        assertEquals(1, assignments.size)
        assertEquals("Capture site job", assignments[0].str("jobTitle"))
        assertEquals("SubCap8A", assignments[0].str("userName"))
        assertEquals("SubCap8A", list.arr("subcontractors")[0].str("displayName"))

        // STAFF sight is the handler's own filtering: same
        // targets, no assignments of others.
        val staff = api(QueueTransport(step("GET /api/app/portal-settings/sub-assignments (staff)")))
            .portalSubs()
        assertTrue(staff.arr("assignments").isEmpty())
        assertEquals(1, staff.arr("subcontractors").size)

        val assignT = QueueTransport(step("POST /api/app/portal-settings/sub-assignments"))
        val assigned = api(assignT).portalAssign(1, 3)
        assertEquals("POST", assignT.requests[0].method)
        assertTrue(assignT.requests[0].body!!.contains("\"jobId\":1"))
        assertTrue(assignT.requests[0].body!!.contains("\"userId\":3"))
        assertEquals("Subcontractor assigned.", assigned.str("message"))

        val unassigned = api(QueueTransport(step("POST /api/app/portal-settings/sub-assignments/unassign")))
            .portalUnassign(1, 3)
        assertEquals("Subcontractor unassigned.", unassigned.str("message"))
    }

    @Test
    fun portalSecretsAreMetadataOnly() = runBlocking {
        val createT = QueueTransport(step("POST /api/app/portal-settings/secrets"))
        val created = api(createT).portalSecretCreate(1, "Capture mailbox secret", "write-only-value", 24)
        assertEquals(201, RealBodiesPhase8.status("POST /api/app/portal-settings/secrets"))
        assertEquals(
            "Secret created — the client can view it once in their portal.",
            created.str("message"),
        )
        // The value rode the request once (write-only) …
        assertTrue(createT.requests[0].body!!.contains("write-only-value"))
        // … and the response carries no value back.
        assertFalse(created.toString().contains("write-only-value"))

        val list = api(QueueTransport(step("GET /api/app/portal-settings/secrets?clientId=1")))
            .portalSecrets(1)
        val rows = list.arr("secrets")
        assertEquals(1, rows.size)
        // Metadata keys ONLY — no value-shaped key exists.
        assertEquals(
            setOf("id", "clientId", "label", "state", "createdAt", "expiresAt", "viewedAt"),
            rows[0].keys,
        )
        assertEquals("Capture mailbox secret", rows[0].str("label"))
        assertEquals("pending", rows[0].str("state"))
        assertTrue(Phase8Logic.secretMeta(rows[0]).contains("pending"))
    }

    @Test
    fun portalOnePagerAndMissingClient() = runBlocking {
        val pager = api(QueueTransport(step("GET /api/app/portal-settings/one-pager?clientId=1")))
            .portalOnePager(1)
        val op = pager.obj("onepager")!!
        assertEquals("Capture Fixture LLC", op.str("legalName"))
        assertTrue(op.str("documentText")!!.contains("Service summary"))
        assertEquals(42, op.obj("posture")!!.int("score"))

        // Web's own semantics: an unknown client answers null.
        val missing = api(QueueTransport(step("GET /api/app/portal-settings/one-pager?clientId=999999")))
            .portalOnePager(999999)
        assertEquals("null", missing["onepager"]!!.toString())
    }

    @Test
    fun portalTipsCrudAndCurrentTip() = runBlocking {
        val created = api(QueueTransport(step("POST /api/app/portal-settings/tips")))
            .portalTipCreate("Capture tip", "Capture tip body.", true, 1)
        assertEquals("Tip saved.", created.str("message"))

        val listT = QueueTransport(step("GET /api/app/portal-settings/tips"))
        val list = api(listT).portalTips()
        assertEquals(1, list.arr("tips").size)
        assertEquals("Capture tip", list.arr("tips")[0].str("title"))
        assertEquals(1, list.int("currentTipId"))
        assertEquals(true, (list["canWrite"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull())

        val patchT = QueueTransport(step("PATCH /api/app/portal-settings/tips/1"))
        val patched = api(patchT).portalTipPatch(
            1, buildJsonObject { put("title", "Capture tip (edited)") },
        )
        assertEquals("PATCH", patchT.requests[0].method)
        assertEquals("/api/app/portal-settings/tips/1", patchT.requests[0].path)
        assertEquals("Tip saved.", patched.str("message"))

        val current = api(QueueTransport(step("GET /api/app/portal-settings/current-tip")))
            .portalCurrentTip()
        assertEquals("Capture tip (edited)", current.obj("tip")!!.str("title"))

        val deleted = api(QueueTransport(step("DELETE /api/app/portal-settings/tips/1")))
            .portalTipDelete(1)
        assertEquals("Tip deleted.", deleted.str("message"))
    }

    // ---- Firm Ops ----

    @Test
    fun firmJobCostsAndCostingFigures() = runBlocking {
        val costs = api(QueueTransport(step("GET /api/app/firm-ops/job-costs"))).firmJobCosts()
        val jobs = costs.arr("jobs")
        assertEquals(1, jobs.size)
        assertEquals("Capture costing ticket", jobs[0].str("title"))
        assertEquals(4500, jobs[0].int("totalCostCents"))
        assertTrue(Phase8Logic.costingMeta(jobs[0]).contains("Cost $45"))
        assertTrue(costs.str("note")!!.contains("no rate is ever invented"))

        val costingT = QueueTransport(step("GET /api/app/firm-ops/job-costing?kind=ticket&id=1"))
        val costing = api(costingT).firmJobCosting("ticket", 1)
        assertEquals(
            "/api/app/firm-ops/job-costing?kind=ticket&id=1",
            costingT.requests[0].path,
        )
        val c = costing.obj("costing")!!
        assertEquals(-4500, c.int("marginCents"))
        // MirrorVocab.formatCents is the house formatter; a
        // negative margin renders through it unchanged.
        assertEquals("Margin $-45", Phase8Logic.marginLabel(c))
    }

    @Test
    fun firmVendorsCrud() = runBlocking {
        val createT = QueueTransport(step("POST /api/app/firm-ops/vendors"))
        val created = api(createT).firmVendorCreate(buildJsonObject {
            put("name", "Capture Vendor Co")
            put("service", "Capture hosting")
            put("contactEmail", "ops@capturevendor.example.invalid")
            put("renewalDate", "2027-02-01")
            put("costNote", "capture rate")
        })
        // Create rides web's wrapped {vendor:{…}} body.
        assertTrue(createT.requests[0].body!!.contains("\"vendor\""))
        assertEquals("Capture Vendor Co", created.obj("vendor")!!.str("name"))

        val patchT = QueueTransport(step("PATCH /api/app/firm-ops/vendors/1"))
        val patched = api(patchT).firmVendorPatch(
            1, buildJsonObject { put("name", "Capture Vendor Co II") },
        )
        assertEquals("PATCH", patchT.requests[0].method)
        assertEquals("Capture Vendor Co II", patched.obj("vendor")!!.str("name"))

        val list = api(QueueTransport(step("GET /api/app/firm-ops/vendors"))).firmVendors()
        assertEquals(1, list.arr("vendors").size)
        assertTrue(list.str("note")!!.contains("never shown to clients"))

        val deleted = api(QueueTransport(step("DELETE /api/app/firm-ops/vendors/1")))
            .firmVendorDelete(1)
        assertEquals("Vendor deleted.", deleted.str("message"))
    }

    @Test
    fun firmTasksLifecycleAndConvert() = runBlocking {
        val createT = QueueTransport(step("POST /api/app/firm-ops/tasks"))
        val created = api(createT).firmTaskCreate(buildJsonObject {
            put("title", "Capture owner task")
            put("detail", "Internal capture.")
            put("priority", "High")
            put("clientId", 1)
        })
        assertTrue(createT.requests[0].body!!.contains("\"task\""))
        assertEquals("Capture owner task", created.obj("task")!!.str("title"))

        // The flat create shape is also the handler's (capture 2).
        val flat = api(QueueTransport(step("POST /api/app/firm-ops/tasks (flat)")))
            .firmTaskCreate(buildJsonObject { put("title", "Capture task two") })
        assertEquals("Capture task two", flat.obj("task")!!.str("title"))

        val patched = api(QueueTransport(step("PATCH /api/app/firm-ops/tasks/1")))
            .firmTaskPatch(1, buildJsonObject { put("title", "Capture owner task v2") })
        assertEquals("Capture owner task v2", patched.obj("task")!!.str("title"))

        val list = api(QueueTransport(step("GET /api/app/firm-ops/tasks"))).firmTasks()
        assertEquals(listOf("open", "done", "dropped"), Phase8Logic.taskStates(list))
        assertTrue(list.str("note")!!.contains("never shown to clients"))

        val stated = api(QueueTransport(step("POST /api/app/firm-ops/tasks/2/state")))
            .firmTaskState(2, "done")
        assertEquals("done", stated.obj("task")!!.str("state"))

        val converted = api(QueueTransport(step("POST /api/app/firm-ops/tasks/1/convert")))
            .firmTaskConvert(1)
        assertEquals(2, converted.int("ticketId"))
        assertTrue(converted.str("message")!!.contains("task is marked done and linked"))
    }

    @Test
    fun firmAvailabilityRidesRecordShape() = runBlocking {
        val createT = QueueTransport(step("POST /api/app/firm-ops/availability"))
        val created = api(createT).firmAvailCreate(buildJsonObject {
            put("userId", 2)
            put("dateFrom", "2026-12-01")
            put("dateTo", "2026-12-02")
            put("status", "unavailable")
            put("note", "Capture leave")
        })
        // The handler's own {record:{…}} shape, exactly as web.
        assertTrue(createT.requests[0].body!!.contains("\"record\""))
        assertTrue(createT.requests[0].body!!.contains("\"userId\":2"))
        assertEquals("unavailable", created.obj("record")!!.str("status"))

        val patchT = QueueTransport(step("PATCH /api/app/firm-ops/availability/1"))
        val patched = api(patchT).firmAvailPatch(
            1, buildJsonObject { put("status", "tentative") },
        )
        assertTrue(patchT.requests[0].body!!.contains("\"record\""))
        assertEquals("tentative", patched.obj("record")!!.str("status"))
        // Handler-seeded: untouched fields survive the patch.
        assertEquals("Capture leave", patched.obj("record")!!.str("note"))

        val list = api(QueueTransport(step("GET /api/app/firm-ops/availability"))).firmAvailability()
        assertEquals(listOf("available", "unavailable", "tentative"), Phase8Logic.availStatuses(list))
        assertEquals(3, list.arr("staff").size)

        val deleted = api(QueueTransport(step("DELETE /api/app/firm-ops/availability/1")))
            .firmAvailDelete(1)
        assertEquals("Availability record deleted.", deleted.str("message"))

        val warnings = api(QueueTransport(step("GET /api/app/firm-ops/booking-warnings")))
            .firmBookingWarnings()
        assertTrue(warnings.arr("warnings").isEmpty())
        assertTrue(warnings.str("note")!!.contains("never blocks a booking"))
    }

    // ---- Verified Desk ----

    @Test
    fun deskListEmptyStateAndVocabulary() = runBlocking {
        val list = api(QueueTransport(step("GET /api/app/verified-desk/assessments"))).deskList()
        // The live production shape at deploy: register empty.
        assertTrue(list.arr("assessments").isEmpty())
        assertTrue(list.arr("verifications").isEmpty())
        assertEquals(14, list.arr("controlSet").size)
        assertEquals("mfa_enforced", list.arr("controlSet")[0].str("key"))
        assertTrue(Phase3Logic.arrRaw(list, "categories").isNotEmpty())
        assertTrue(list.str("badgeMeaning")!!.contains("not a certification"))
        assertEquals(true, (list["isOwner"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull())
    }

    @Test
    fun deskWorkflowStartSaveStates() = runBlocking {
        val started = api(QueueTransport(step("POST /api/app/verified-desk/assessments")))
            .deskStart(1, "Security Assessment")
        assertEquals("Assessment started.", started.str("message"))
        val a0 = started.obj("assessment")!!
        assertEquals("draft", a0.str("status"))
        assertEquals(14, a0.arr("controls").size)
        assertEquals("Not scored yet", Phase8Logic.scoreLabel(a0))
        assertEquals(listOf("in_review"), Phase8Logic.nextStates("draft"))

        val detail = api(QueueTransport(step("GET /api/app/verified-desk/assessments/1")))
            .deskDetail(1)
        assertEquals("Verified assessment -- Capture Fixture LLC", detail.obj("assessment")!!.str("title"))

        val saveT = QueueTransport(step("POST /api/app/verified-desk/assessments/1/save"))
        val saved = api(saveT).deskSave(
            1,
            JsonArray(
                listOf(
                    buildJsonObject {
                        put("key", "mfa_enforced")
                        put("status", "pass")
                        put("evidenceNote", "Capture evidence.")
                    },
                ),
            ),
        )
        assertTrue(saveT.requests[0].body!!.contains("mfa_enforced"))
        assertEquals("Checklist saved.", saved.str("message"))
        assertEquals(100.0, (saved.obj("assessment")!!["liveScore"] as? JsonPrimitive)?.content?.toDoubleOrNull())

        val review = api(QueueTransport(step("POST /api/app/verified-desk/assessments/1/state (in_review)")))
            .deskState(1, "in_review")
        assertEquals("in_review", review.obj("assessment")!!.str("status"))
        assertEquals(listOf("completed"), Phase8Logic.nextStates("in_review"))

        val completed = api(QueueTransport(step("POST /api/app/verified-desk/assessments/1/state (completed)")))
            .deskState(1, "completed")
        assertEquals("completed", completed.obj("assessment")!!.str("status"))
        assertEquals("Score 100.0", Phase8Logic.scoreLabel(completed.obj("assessment")!!))
        assertEquals(emptyList<String>(), Phase8Logic.nextStates("completed"))
    }

    @Test
    fun deskBadgeIssueAndRevokeWithReason() = runBlocking {
        val issueT = QueueTransport(step("POST /api/app/verified-desk/assessments/1/issue-badge"))
        val issued = api(issueT).deskIssueBadge(1)
        assertEquals("Verified badge issued.", issued.str("message"))
        val v = issued.obj("verification")!!
        assertEquals("active", v.str("status"))
        assertEquals("Security Assessment", v.str("category"))
        assertEquals(100.0, (v["score"] as? JsonPrimitive)?.content?.toDoubleOrNull())

        val revokeT = QueueTransport(step("POST /api/app/verified-desk/verifications/1/revoke"))
        val revoked = api(revokeT).deskRevokeBadge(1, "Capture revocation.")
        // Reason is required and rides the request …
        assertTrue(revokeT.requests[0].body!!.contains("\"confirm\":true"))
        assertTrue(revokeT.requests[0].body!!.contains("Capture revocation."))
        assertEquals("Verification revoked permanently.", revoked.str("message"))
        // … and is kept on the row.
        assertEquals("revoked", revoked.obj("verification")!!.str("status"))
        assertEquals("Capture revocation.", revoked.obj("verification")!!.str("revokedReason"))
    }

    @Test
    fun deskEvidencePackDescriptorAndMissingDetail() = runBlocking {
        val pack = api(QueueTransport(step("GET /api/app/verified-desk/evidence-pack?clientId=1")))
            .deskEvidencePack(1)
        assertEquals("evidence-pack-client-1.pdf", pack.str("fileName"))
        assertEquals(949, pack.int("pdfSizeBytes"))
        assertEquals(0, pack.int("controlCount"))
        // The captured body truncates pdfBase64 to a descriptor —
        // that is NOT a PDF, and the decoder must refuse it
        // rather than invent bytes.
        assertEquals(null, Phase8Logic.evidencePdfBytes(pack))

        try {
            api(QueueTransport(step("GET /api/app/verified-desk/assessments/999999 (missing)")))
                .deskDetail(999999)
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(404, e.status)
            assertEquals("Not found.", e.message)
        }
    }

    @Test
    fun evidencePdfBytesDecodeRealBase64() {
        val pdf = "%PDF-1.4 capture".toByteArray()
        val pack = buildJsonObject {
            put(
                "pdfBase64",
                java.util.Base64.getEncoder().encodeToString(pdf),
            )
        }
        assertTrue(pdf.contentEquals(Phase8Logic.evidencePdfBytes(pack)!!))
    }

    // ---- Activity ----

    @Test
    fun activityConsoleSurfaces() = runBlocking {
        val console = api(QueueTransport(step("GET /api/app/activity"))).activityPage()
        assertEquals(37, console.int("total"))
        assertEquals(37, console.arr("rows").size)
        val summary = console.obj("summary")!!
        assertEquals(37, summary.int("eventsToday"))
        assertEquals(1, summary.int("activeSessions"))
        val counts = console.obj("classCounts")!!
        assertEquals(37, counts.int("all"))
        assertEquals(6, counts.int("auth"))
        assertEquals(26, counts.int("changes"))
        val failed = console.arr("failedByIp")
        assertEquals(1, failed.size)
        assertEquals("Failed sign-in attempt", failed[0].str("lastActionLabel"))
        val first = console.arr("rows")[0]
        assertEquals("Compliance evidence pack exported", Phase8Logic.activityHeadline(first))
        assertTrue(Phase8Logic.activityMeta(first).contains("OwnerCap8A"))
    }

    @Test
    fun activityClassFilterRidesQuery() = runBlocking {
        val t = QueueTransport(step("GET /api/app/activity?class=auth&limit=5"))
        val out = api(t).activityPage(cls = "auth", limit = 5, offset = 0)
        assertTrue(t.requests[0].path.contains("class=auth"))
        assertTrue(t.requests[0].path.contains("limit=5"))
        assertEquals(6, out.int("total"))
        assertEquals(5, out.arr("rows").size)
        assertTrue(out.arr("rows").all { Phase3Logic.str(it, "eventClass") == "auth" })
    }

    @Test
    fun activityWalkerCollectsCompleteFilteredSet() = runBlocking {
        val consoleBody = RealBodiesPhase8.bodyJson("GET /api/app/activity")
        val allRows = (consoleBody["rows"] as JsonArray)
        // The handler caps pages; the walker must page by offset
        // until the payload's own total (37) is collected.
        val transport = SlicingTransport(allRows, total = 37, pageCap = 2)
        val out = api(transport).activityAll()
        assertEquals(37, out.int("total"))
        assertEquals(37, out.arr("rows").size)
        assertEquals(37, out.arr("rows").map { it.int("id") }.distinct().size)
        assertTrue(transport.requests.size >= 19)
        assertTrue(transport.requests[1].path.contains("offset=2"))
    }

    @Test
    fun activitySessionsVisitorsAndRevoke() = runBlocking {
        val sessions = api(QueueTransport(step("GET /api/app/activity/sessions"))).activitySessions()
        assertEquals(1, sessions.int("count"))
        assertEquals("OwnerCap8A", sessions.arr("sessions")[0].str("displayName"))
        // Device tokens are not web sessions: isCurrent is false.
        assertEquals(
            false,
            (sessions.arr("sessions")[0]["isCurrent"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull(),
        )

        val visitors = api(QueueTransport(step("GET /api/app/activity/public-visitors")))
            .activityVisitors()
        // The honest not-connected state, never a fake list.
        assertEquals(
            false,
            (visitors["connected"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull(),
        )

        // Revoke was exercised in the backend battery but not
        // fixture-captured; pin method + path against a minimal
        // success body.
        val revokeT = QueueTransport(200 to """{"ok":true,"message":"Session revoked."}""")
        val revoked = api(revokeT).activityRevokeSession(1)
        assertEquals("POST", revokeT.requests[0].method)
        assertEquals("/api/app/activity/sessions/1/revoke", revokeT.requests[0].path)
        assertEquals("Session revoked.", revoked.str("message"))
    }

    // ---- Pure logic pins ----

    @Test
    fun phase8LogicPins() {
        assertEquals("Sign-ins & auth", Phase8Logic.classLabel("auth"))
        assertEquals("all", Phase8Logic.CLASS_FILTERS[0].first)
        assertEquals(6, Phase8Logic.CLASS_FILTERS.size)
        assertEquals("Last 7 days", Phase8Logic.daysLabel("7"))
        assertEquals("30", Phase8Logic.daysValue("Last 30 days"))
        assertEquals("Pass", Phase8Logic.controlStatusLabel("pass"))
        assertEquals("partial", Phase8Logic.controlStatusValue("Partial"))
        assertEquals("In review", Phase8Logic.assessmentStateLabel("in_review"))
        assertEquals("Completed", Phase8Logic.assessmentStateLabel("completed"))
        // Fallbacks when a payload omits its own vocabularies.
        assertEquals(Phase8Logic.TASK_STATES, Phase8Logic.taskStates(null))
        assertEquals(Phase8Logic.AVAIL_STATUSES, Phase8Logic.availStatuses(null))
    }
}

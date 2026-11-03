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
import net.sentinelsafeguard.app.data.api.archiveAsset
import net.sentinelsafeguard.app.data.api.assetDetail
import net.sentinelsafeguard.app.data.api.assetLabels
import net.sentinelsafeguard.app.data.api.assetRenewals
import net.sentinelsafeguard.app.data.api.assetUrlLabels
import net.sentinelsafeguard.app.data.api.assets
import net.sentinelsafeguard.app.data.api.createAsset
import net.sentinelsafeguard.app.data.api.createEmployee
import net.sentinelsafeguard.app.data.api.createEmployeeInvite
import net.sentinelsafeguard.app.data.api.createForm
import net.sentinelsafeguard.app.data.api.deleteAsset
import net.sentinelsafeguard.app.data.api.deleteEmployee
import net.sentinelsafeguard.app.data.api.deleteForm
import net.sentinelsafeguard.app.data.api.deleteFormFile
import net.sentinelsafeguard.app.data.api.downloadFormFile
import net.sentinelsafeguard.app.data.api.editAsset
import net.sentinelsafeguard.app.data.api.editEmployee
import net.sentinelsafeguard.app.data.api.editForm
import net.sentinelsafeguard.app.data.api.employeeDetail
import net.sentinelsafeguard.app.data.api.employees
import net.sentinelsafeguard.app.data.api.employeesWorkload
import net.sentinelsafeguard.app.data.api.ensureAssetQr
import net.sentinelsafeguard.app.data.api.formDetail
import net.sentinelsafeguard.app.data.api.formFiles
import net.sentinelsafeguard.app.data.api.forms
import net.sentinelsafeguard.app.data.api.regenerateAssetQr
import net.sentinelsafeguard.app.data.api.resolveAssetQr
import net.sentinelsafeguard.app.data.api.restoreAsset
import net.sentinelsafeguard.app.data.api.revokeAllEmployeeSessions
import net.sentinelsafeguard.app.data.api.revokeEmployeeInvite
import net.sentinelsafeguard.app.data.api.revokeEmployeeSession
import net.sentinelsafeguard.app.data.api.setAssetStatus
import net.sentinelsafeguard.app.data.api.setEmployeeActive
import net.sentinelsafeguard.app.data.api.setEmployeeClient
import net.sentinelsafeguard.app.data.api.setEmployeeModNote
import net.sentinelsafeguard.app.data.api.setEmployeeMustReset
import net.sentinelsafeguard.app.data.api.setEmployeeRole
import net.sentinelsafeguard.app.data.api.setEmployeeSuspend
import net.sentinelsafeguard.app.data.api.updateFormFile
import net.sentinelsafeguard.app.data.api.uploadFormFile
import net.sentinelsafeguard.app.ui.Phase5Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Phase 5 (hunt9) contract shapes: every Phase 5 endpoint driven
 * through the app's API methods against REAL bodies captured from
 * the deployed Phase 5 backend (see [RealBodiesPhase5];
 * Addendum 18). The STAFF employees refusal has no owner-side
 * capture; its test pins the client's passthrough behavior (the
 * server's own message reaches the UI) rather than any wording.
 */
class ContractPhase5Test {

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
        RealBodiesPhase5.status(name) to RealBodiesPhase5.body(name)

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.content

    private fun JsonObject.int(key: String): Int? =
        (this[key] as? JsonPrimitive)?.content?.toIntOrNull()

    private fun JsonObject.bool(key: String): Boolean? =
        (this[key] as? JsonPrimitive)?.content?.toBooleanStrictOrNull()

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.arr(key: String): List<JsonObject> =
        (this[key] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

    // ---- Employees ----

    @Test
    fun employeesListParsesRealRowsStatsAndCounts() = runBlocking {
        val transport = QueueTransport(step("GET /api/app/employees"))
        val out = api(transport).employees()
        assertEquals("GET", transport.requests[0].method)
        assertTrue(transport.requests[0].path.startsWith("/api/app/employees"))
        assertEquals(2, out.int("total"))
        val rows = out.arr("employees")
        assertEquals(2, rows.size)
        assertEquals("Capture Owner", rows[0].str("displayName"))
        assertEquals("OWNER", rows[0].str("role"))
        assertEquals(1, rows[1].obj("openCounts")!!.int("tickets"))
        val stats = out.obj("stats")!!
        assertEquals(2, stats.int("team"))
        assertEquals(1, stats.int("owners"))
        // OWNER-only key rides the payload for an OWNER caller.
        assertNotNull(stats["pendingInvites"])
    }

    @Test
    fun employeesRoleFilterRidesParam() = runBlocking {
        val transport = QueueTransport(step("GET /api/app/employees?role=Staff"))
        val out = api(transport).employees(role = "Staff")
        assertTrue(transport.requests[0].path.contains("role=Staff"))
        assertEquals(1, out.int("total"))
        assertEquals("STAFF", out.arr("employees")[0].str("role"))
    }

    @Test
    fun employeesRefusalSurfacesServerWords() = runBlocking {
        // STAFF has no employees grant (Addendum 18): the wrapper
        // answers 403 and callJson must surface the server's own
        // message — never a fake empty directory. The body here is
        // a refusal-shaped probe of the client passthrough, not a
        // captured wording.
        val transport = QueueTransport(403 to """{"ok":false,"message":"REFUSAL-PROBE"}""")
        try {
            api(transport).employees()
            fail("expected an ApiException")
        } catch (e: ApiException) {
            assertEquals(403, e.status)
            assertEquals("REFUSAL-PROBE", e.message)
        }
    }

    @Test
    fun employeesWorkloadParses() = runBlocking {
        val out = api(QueueTransport(step("GET /api/app/employees/workload"))).employeesWorkload()
        val members = out.arr("members")
        assertEquals(2, members.size)
        assertEquals(1, members[1].obj("workload")!!.int("openTotal"))
        assertEquals(0, out.obj("unassigned")!!.int("total"))
    }

    @Test
    fun employeeProfileParsesModerationAndWorkload() = runBlocking {
        val out = api(QueueTransport(step("GET /api/app/employees/{id}"))).employeeDetail(2)
        assertEquals("Capture Tech", out.obj("user")!!.str("displayName"))
        val moderation = out.obj("moderation")!!
        assertEquals(false, moderation.bool("mustReset"))
        assertEquals("", moderation.str("modNote"))
        assertNotNull(out["sessions"])
        assertEquals(1, out.obj("workload")!!.int("openTotal"))
    }

    @Test
    fun employeeCreateRidesWebShape() = runBlocking {
        val transport = QueueTransport(step("POST /api/app/employees"))
        val out = api(transport).createEmployee(
            buildJsonObject {
                put("displayName", "Capture Second"); put("jobTitle", "Analyst")
                put("email", "second@capture.invalid"); put("phone", "555-0112")
                put("passphrase", "CaptureSecondPass42")
            },
        )
        assertEquals("POST", transport.requests[0].method)
        assertEquals("/api/app/employees", transport.requests[0].path)
        assertEquals("Employee created.", out.str("message"))
        assertEquals(3, out.int("id"))
        assertEquals("STAFF", out.obj("user")!!.str("role"))
    }

    @Test
    fun employeeEditAndModerationVerbs() = runBlocking {
        val editT = QueueTransport(step("PATCH /api/app/employees/{id}"))
        val edited = api(editT).editEmployee(3, buildJsonObject { put("jobTitle", "Senior Analyst") })
        assertEquals("PATCH", editT.requests[0].method)
        assertEquals("Senior Analyst", edited.obj("user")!!.str("jobTitle"))

        val note = api(QueueTransport(step("POST /api/app/employees/{id}/mod-note")))
            .setEmployeeModNote(3, "Capture note")
        assertEquals("Moderator note saved.", note.str("message"))

        val reset = api(QueueTransport(step("POST /api/app/employees/{id}/must-reset")))
            .setEmployeeMustReset(3, false)
        assertEquals("Passphrase requirement updated.", reset.str("message"))

        val lift = api(QueueTransport(step("POST /api/app/employees/{id}/suspend (lift)")))
            .setEmployeeSuspend(3, 0)
        assertEquals("Suspension lifted.", lift.str("message"))
        assertTrue(lift["suspendUntil"] is kotlinx.serialization.json.JsonNull)

        val role = api(QueueTransport(step("POST /api/app/employees/{id}/role")))
            .setEmployeeRole(3, "STAFF")
        assertEquals("Role unchanged.", role.str("message"))

        val client = api(QueueTransport(step("POST /api/app/employees/{id}/client")))
            .setEmployeeClient(3, null)
        assertEquals("Client link unchanged.", client.str("message"))

        val active = api(QueueTransport(step("POST /api/app/employees/{id}/active")))
            .setEmployeeActive(3, true)
        assertEquals("User status updated.", active.str("message"))
    }

    @Test
    fun employeeSessionAndInviteVerbs() = runBlocking {
        val revoke = api(QueueTransport(step("POST /api/app/employees/{id}/sessions/{sid}/revoke")))
            .revokeEmployeeSession(3, "sess-1")
        assertEquals("Session revoked.", revoke.str("message"))

        val revokeAllT = QueueTransport(step("POST /api/app/employees/{id}/revoke-all-sessions"))
        val revokeAll = api(revokeAllT).revokeAllEmployeeSessions(3)
        assertEquals("/api/app/employees/3/revoke-all-sessions", revokeAllT.requests[0].path)
        assertEquals(0, revokeAll.int("count"))

        val inviteT = QueueTransport(step("POST /api/app/employees/invites"))
        val invite = api(inviteT).createEmployeeInvite("STAFF")
        assertEquals("Invite created.", invite.str("message"))
        assertEquals("STAFF", invite.str("role"))
        assertTrue(invite.str("code")!!.startsWith("INV-"))

        val revoked = api(QueueTransport(step("POST /api/app/employees/invites/{id}/revoke")))
            .revokeEmployeeInvite(1)
        assertEquals("Invite revoked.", revoked.str("message"))

        val deleted = api(QueueTransport(step("DELETE /api/app/employees/{id}"))).deleteEmployee(3)
        assertEquals("User deleted.", deleted.str("message"))
    }

    // ---- Assets ----

    @Test
    fun assetCreateRidesItemShape() = runBlocking {
        val transport = QueueTransport(step("POST /api/app/assets (Sentinel)"))
        val out = api(transport).createAsset(
            buildJsonObject {
                put("scope", "Sentinel"); put("name", "Capture Laptop")
                put("status", "In Service"); put("type", "Laptop")
                put("identifier", "SN-CAP-001"); put("assignedUserId", 2)
                put("reviewDueDate", "2027-02-01"); put("warrantyUntil", "2026-11-13")
                put("notes", "capture")
            },
        )
        assertEquals("POST", transport.requests[0].method)
        assertTrue(transport.requests[0].body!!.contains("\"item\""))
        assertEquals("Asset saved.", out.str("message"))
        assertNotNull(out.obj("asset")!!.str("qrCode"))
    }

    @Test
    fun assetsListParsesFlagsRenewalsAndStats() = runBlocking {
        val out = api(QueueTransport(step("GET /api/app/assets"))).assets()
        assertEquals(2, out.int("total"))
        val rows = out.arr("assets")
        val firewall = rows.first { it.int("id") == 2 }
        assertEquals("Capture Client LLC", firewall.str("ownerName"))
        assertEquals(true, firewall.obj("flags")!!.bool("attention"))
        assertEquals("review", firewall.obj("earliestRenewal")!!.str("field"))
        val stats = out.obj("stats")!!
        assertEquals(1, stats.int("sentinel"))
        assertEquals(2, stats.int("attention"))
        assertEquals(2, out.arr("types").size)
    }

    @Test
    fun assetsAlertFilterRidesParam() = runBlocking {
        val transport = QueueTransport(step("GET /api/app/assets?alert=alerts"))
        val out = api(transport).assets(alert = "alerts")
        assertTrue(transport.requests[0].path.contains("alert=alerts"))
        assertEquals(2, out.int("total"))
    }

    @Test
    fun assetDetailParsesFlagsAndClient() = runBlocking {
        val out = api(QueueTransport(step("GET /api/app/assets/{id}"))).assetDetail(2)
        assertEquals("Capture Firewall", out.obj("asset")!!.str("name"))
        assertEquals("Capture Client LLC", out.obj("client")!!.str("legalName"))
        assertEquals(true, out.obj("flags")!!.bool("warrantyExpired"))
        assertEquals(2, out.arr("renewals").size)
        assertEquals(1, out.arr("activity").size)
    }

    @Test
    fun assetRenewalsReconcileWithList() = runBlocking {
        val renewals = api(QueueTransport(step("GET /api/app/assets/renewals"))).assetRenewals()
        val flagged = renewals.obj("flagged")!!
        assertEquals(2, flagged.int("total"))
        val within30 = renewals.obj("renewals")!!.obj("within30")!!
        assertEquals(2, within30.int("total"))
        // The same two assets the directory's attention stat counts.
        val list = RealBodiesPhase5.bodyJson("GET /api/app/assets")
        assertEquals(list.obj("stats")!!.int("attention"), flagged.int("total"))
    }

    @Test
    fun assetEditStatusAndLifecycleVerbs() = runBlocking {
        val patchT = QueueTransport(step("PATCH /api/app/assets/{id}"))
        val patched = api(patchT).editAsset(
            1, buildJsonObject { put("name", "Capture Laptop Pro"); put("status", "In Repair") },
        )
        assertEquals("PATCH", patchT.requests[0].method)
        assertEquals("In Repair", patched.obj("asset")!!.str("status"))

        val status = api(QueueTransport(step("POST /api/app/assets/{id}/status")))
            .setAssetStatus(1, "In Storage")
        assertEquals("In Storage", status.str("status"))

        val archived = api(QueueTransport(step("POST /api/app/assets/{id}/archive"))).archiveAsset(1)
        assertEquals("Asset archived.", archived.str("message"))
        val restored = api(QueueTransport(step("POST /api/app/assets/{id}/restore"))).restoreAsset(1)
        assertEquals("Asset restored.", restored.str("message"))
        val deleted = api(QueueTransport(step("DELETE /api/app/assets/{id}"))).deleteAsset(1)
        assertEquals("Asset deleted.", deleted.str("message"))
    }

    @Test
    fun assetQrAndLabelsParse() = runBlocking {
        val ensure = api(QueueTransport(step("POST /api/app/assets/{id}/qr/ensure"))).ensureAssetQr(2)
        assertEquals(2, ensure.int("assetId"))
        assertTrue(ensure.str("payload")!!.startsWith("https://sentinelsafeguard.net/a/"))
        assertTrue(ensure.str("svg")!!.contains("<svg"))

        val regen = api(QueueTransport(step("POST /api/app/assets/{id}/qr/regenerate")))
            .regenerateAssetQr(2)
        assertTrue(regen.str("payload")!!.startsWith("https://sentinelsafeguard.net/a/"))

        val resolved = api(QueueTransport(step("GET /api/app/assets/resolve?code= (pre-existing)")))
            .resolveAssetQr("SCA-code")
        assertEquals(true, resolved.bool("found"))
        assertEquals("Capture Firewall", resolved.obj("asset")!!.str("name"))

        val labels = api(QueueTransport(step("GET /api/app/assets/labels"))).assetLabels()
        assertEquals(2, labels.arr("labels").size)
        assertEquals("Capture Laptop Pro", labels.arr("labels")[0].str("name"))

        val urlLabels = api(QueueTransport(step("GET /api/app/assets/url-labels"))).assetUrlLabels()
        assertEquals(
            "https://command.sentinelsafeguard.net/#asset=<assetId>",
            urlLabels.str("urlFormat"),
        )
    }

    // ---- Forms ----

    @Test
    fun formRecordLifecycle() = runBlocking {
        val createT = QueueTransport(step("POST /api/app/forms"))
        val created = api(createT).createForm(
            buildJsonObject {
                put("clientId", 1); put("type", "Engagement Letter")
                put("title", "Capture Engagement"); put("dataJson", "{\"client\": \"Capture\"}")
            },
        )
        assertEquals("POST", createT.requests[0].method)
        assertEquals("Form saved.", created.str("message"))

        val list = api(QueueTransport(step("GET /api/app/forms"))).forms()
        assertEquals(1, list.int("total"))
        assertEquals("Capture Client LLC", list.arr("forms")[0].str("clientName"))

        val detail = api(QueueTransport(step("GET /api/app/forms/{id}"))).formDetail(1)
        assertEquals("Capture Engagement", detail.obj("form")!!.str("title"))

        val patched = api(QueueTransport(step("PATCH /api/app/forms/{id}")))
            .editForm(1, buildJsonObject { put("title", "Capture Engagement v2") })
        assertEquals("Capture Engagement v2", patched.obj("form")!!.str("title"))

        val deleted = api(QueueTransport(step("DELETE /api/app/forms/{id}"))).deleteForm(1)
        assertEquals("Form deleted.", deleted.str("message"))
    }

    @Test
    fun formFileUploadRidesBinaryTwin() = runBlocking {
        val transport = QueueTransport(step("POST /api/app/form-files/upload"))
        val pdf = "%PDF-1.4\n1 0 obj".toByteArray()
        val out = api(transport).uploadFormFile(pdf, "capture.pdf", "Capture PDF", "capture note")
        assertEquals("Form document stored.", out.str("message"))
        assertEquals(1, out.int("id"))
        val req = transport.requests[0]
        assertEquals("/api/app/form-files/upload", req.path)
        assertEquals("capture.pdf", req.headers["X-File-Name"])
        assertEquals("Capture+PDF", req.headers["X-Form-Name"])
    }

    @Test
    fun formFilesCatalogAndDownload() = runBlocking {
        val list = api(QueueTransport(step("GET /api/app/form-files"))).formFiles()
        assertEquals(1, list.int("total"))
        val file = list.arr("files")[0]
        assertEquals("Capture PDF", file.str("displayName"))
        assertEquals(48, file.int("sizeBytes"))

        // The download capture is binary: 48 bytes beginning with
        // the PDF magic the fixture records.
        assertEquals(48, RealBodiesPhase5.bodyBytes("GET /api/app/form-files/{id}/download"))
        assertTrue(RealBodiesPhase5.bodyPrefix("GET /api/app/form-files/{id}/download")
            .startsWith("%PDF-"))
        val dlTransport = QueueTransport(200 to "%PDF-1.4 fake")
        val bytes = api(dlTransport).downloadFormFile(1)
        assertEquals("/api/app/form-files/1/download", dlTransport.requests[0].path)
        assertTrue(bytes.toString(Charsets.US_ASCII).startsWith("%PDF-"))

        val updated = api(QueueTransport(step("PATCH /api/app/form-files/{id}")))
            .updateFormFile(1, buildJsonObject {
                put("displayName", "Capture PDF (filed)"); put("note", "filed")
            })
        assertEquals("Form document updated.", updated.str("message"))

        val deleted = api(QueueTransport(step("DELETE /api/app/form-files/{id}"))).deleteFormFile(1)
        assertEquals("Form document deleted.", deleted.str("message"))
    }

    // ---- Pure logic pins ----

    @Test
    fun phase5LogicPins() {
        val list = RealBodiesPhase5.bodyJson("GET /api/app/assets")
        val firewall = list.arr("assets").first { it.int("id") == 2 }
        assertEquals(
            listOf("Review overdue", "Warranty expired"),
            Phase5Logic.flagLabels(firewall.obj("flags")),
        )
        val renewal = firewall.arr("renewals")[0]
        assertEquals("Review expired 2026-10-29", Phase5Logic.renewalLabel(renewal))

        val employees = RealBodiesPhase5.bodyJson("GET /api/app/employees")
        assertEquals("Team", Phase5Logic.employeeSection(employees.arr("employees")[0]))
        assertTrue(Phase5Logic.isTeam(employees.arr("employees")[1]))

        assertNull(Phase5Logic.uploadPrecheck("a.pdf", "%PDF-1.4 x".toByteArray()))
        assertEquals(
            "Form documents must be PDF files.",
            Phase5Logic.uploadPrecheck("a.txt", "hello".toByteArray()),
        )
        assertEquals(
            "That PDF is larger than 25 MB.",
            Phase5Logic.uploadPrecheck(
                "a.pdf",
                ByteArray(Phase5Logic.FORM_FILE_MAX_BYTES + 1),
            ),
        )
        assertEquals("48 B", Phase5Logic.formatBytes(48))
    }
}

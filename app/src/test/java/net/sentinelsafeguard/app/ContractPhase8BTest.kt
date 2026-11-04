package net.sentinelsafeguard.app

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.config.ApiConfig
import net.sentinelsafeguard.app.data.api.HttpRequest
import net.sentinelsafeguard.app.data.api.HttpResponse
import net.sentinelsafeguard.app.data.api.HttpTransport
import net.sentinelsafeguard.app.data.api.SentinelApi
import net.sentinelsafeguard.app.data.api.adminAuditExport
import net.sentinelsafeguard.app.data.api.adminAuditLog
import net.sentinelsafeguard.app.data.api.adminBackupRequest
import net.sentinelsafeguard.app.data.api.adminBackupStatus
import net.sentinelsafeguard.app.data.api.adminData
import net.sentinelsafeguard.app.data.api.adminDmarc
import net.sentinelsafeguard.app.data.api.adminHealthStrip
import net.sentinelsafeguard.app.data.api.adminLawAi
import net.sentinelsafeguard.app.data.api.adminLawAiKeyInstall
import net.sentinelsafeguard.app.data.api.adminLawAiKeyStatus
import net.sentinelsafeguard.app.data.api.adminLawAiSave
import net.sentinelsafeguard.app.data.api.adminLawAiTest
import net.sentinelsafeguard.app.data.api.adminLoginSecurity
import net.sentinelsafeguard.app.data.api.adminMemberCreate
import net.sentinelsafeguard.app.data.api.adminMemberPassphrase
import net.sentinelsafeguard.app.data.api.adminOpsReport
import net.sentinelsafeguard.app.data.api.adminRecoveryCodes
import net.sentinelsafeguard.app.data.api.adminRetention
import net.sentinelsafeguard.app.data.api.adminRetentionReport
import net.sentinelsafeguard.app.data.api.adminRetentionSet
import net.sentinelsafeguard.app.data.api.adminRevokeAllSessions
import net.sentinelsafeguard.app.data.api.adminRoleCreate
import net.sentinelsafeguard.app.data.api.adminRoleDelete
import net.sentinelsafeguard.app.data.api.adminRolePatch
import net.sentinelsafeguard.app.data.api.adminRoleTabs
import net.sentinelsafeguard.app.data.api.adminRoles
import net.sentinelsafeguard.app.data.api.adminSecuritySave
import net.sentinelsafeguard.app.data.api.adminSettingsSave
import net.sentinelsafeguard.app.data.api.appApiKeyCreate
import net.sentinelsafeguard.app.data.api.appApiKeyRevoke
import net.sentinelsafeguard.app.data.api.appDeliveriesRun
import net.sentinelsafeguard.app.data.api.appIntegrations
import net.sentinelsafeguard.app.data.api.appWebhookActive
import net.sentinelsafeguard.app.data.api.appWebhookCreate
import net.sentinelsafeguard.app.data.api.automationContractReminders
import net.sentinelsafeguard.app.data.api.automationEscalationConfig
import net.sentinelsafeguard.app.data.api.automationEscalations
import net.sentinelsafeguard.app.data.api.automationQuoteFollowups
import net.sentinelsafeguard.app.data.api.automationRuleSave
import net.sentinelsafeguard.app.data.api.automationRules
import net.sentinelsafeguard.app.data.api.clientImport
import net.sentinelsafeguard.app.data.api.clientImportConfirm
import net.sentinelsafeguard.app.data.api.clientImportPreview
import net.sentinelsafeguard.app.data.api.clientImports
import net.sentinelsafeguard.app.data.api.clientOpsEmergency
import net.sentinelsafeguard.app.data.api.clientOpsEmergencySave
import net.sentinelsafeguard.app.data.api.clientOpsRetainerSave
import net.sentinelsafeguard.app.data.api.clientOpsRetainers
import net.sentinelsafeguard.app.data.api.clientOpsWindowSave
import net.sentinelsafeguard.app.data.api.clientOpsWindows
import net.sentinelsafeguard.app.data.api.collectionsBoard
import net.sentinelsafeguard.app.data.api.collectionsWorkflow
import net.sentinelsafeguard.app.data.api.fieldCountApply
import net.sentinelsafeguard.app.data.api.fieldCountClose
import net.sentinelsafeguard.app.data.api.fieldCountScan
import net.sentinelsafeguard.app.data.api.fieldCountStart
import net.sentinelsafeguard.app.data.api.fieldCounts
import net.sentinelsafeguard.app.data.api.fieldImportConfirm
import net.sentinelsafeguard.app.data.api.fieldImportPreview
import net.sentinelsafeguard.app.data.api.fieldJobMode
import net.sentinelsafeguard.app.data.api.fieldLeadRoi
import net.sentinelsafeguard.app.data.api.fieldLeadScores
import net.sentinelsafeguard.app.data.api.fieldScan
import net.sentinelsafeguard.app.data.api.fieldSyncPush
import net.sentinelsafeguard.app.data.api.opsCapacity
import net.sentinelsafeguard.app.data.api.opsCapacityThreshold
import net.sentinelsafeguard.app.data.api.opsContractRadar
import net.sentinelsafeguard.app.data.api.opsRiskHeatmap
import net.sentinelsafeguard.app.data.api.opsTimeline
import net.sentinelsafeguard.app.data.api.packetBuild
import net.sentinelsafeguard.app.data.api.packetMarkMailed
import net.sentinelsafeguard.app.data.api.packetsClaims
import net.sentinelsafeguard.app.data.api.packetsList
import net.sentinelsafeguard.app.data.api.packetsOps
import net.sentinelsafeguard.app.data.api.packetsRoute
import net.sentinelsafeguard.app.data.api.pipelineChangeRequests
import net.sentinelsafeguard.app.data.api.pipelineLeadConvert
import net.sentinelsafeguard.app.data.api.pipelineLeadSave
import net.sentinelsafeguard.app.data.api.pipelineLeadState
import net.sentinelsafeguard.app.data.api.pipelineLeads
import net.sentinelsafeguard.app.data.api.pipelineProposals
import net.sentinelsafeguard.app.data.api.reportBriefGenerate
import net.sentinelsafeguard.app.data.api.reportBriefs
import net.sentinelsafeguard.app.data.api.reportDocument
import net.sentinelsafeguard.app.data.api.reportReferralSave
import net.sentinelsafeguard.app.data.api.reportReferrals
import net.sentinelsafeguard.app.data.api.reportScheduleRun
import net.sentinelsafeguard.app.data.api.reportScheduleSave
import net.sentinelsafeguard.app.data.api.reportSchedules
import net.sentinelsafeguard.app.data.api.reportSnapshots
import net.sentinelsafeguard.app.data.api.reportSurveyCreate
import net.sentinelsafeguard.app.data.api.reportSurveys
import net.sentinelsafeguard.app.data.api.reportsGenerate
import net.sentinelsafeguard.app.data.api.reportsList
import net.sentinelsafeguard.app.data.api.staffGearCertSave
import net.sentinelsafeguard.app.data.api.staffGearDirectory
import net.sentinelsafeguard.app.data.api.staffGearEquipment
import net.sentinelsafeguard.app.data.api.staffGearEquipmentSave
import net.sentinelsafeguard.app.data.api.staffGearTraining
import net.sentinelsafeguard.app.ui.Phase8bLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 8B (hunt13) contract shapes: every Phase 8B endpoint
 * driven through the app's API methods against REAL bodies
 * captured from the deployed Phase 8B backend (see
 * [RealBodiesPhase8B]; Addendum 26) — including the gate 403s,
 * the masked Law AI state, the shape-only shown-once surfaces,
 * the recovery-code shape (count + lengths, never values), the
 * backup state machine, and the import/lead-conversion flows
 * that must never create a live client.
 */
class ContractPhase8BTest {

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

    private fun api(transport: HttpTransport): SentinelApi =
        SentinelApi(ApiConfig("https://command.example.test", "", ""), transport)

    private fun step(name: String): Pair<Int, String> =
        RealBodiesPhase8B.status(name) to RealBodiesPhase8B.body(name)

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.content

    private fun JsonObject.int(key: String): Int? =
        (this[key] as? JsonPrimitive)?.content?.toIntOrNull()

    private fun JsonObject.bool(key: String): Boolean? =
        (this[key] as? JsonPrimitive)?.content?.toBooleanStrictOrNull()

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.arr(key: String): List<JsonObject> =
        (this[key] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

    @Test
    fun adminDataAndGate() = runBlocking {
        val t = QueueTransport(step("GET /api/app/admin"))
        val data = api(t).adminData()
        assertEquals("GET", t.requests[0].method)
        assertEquals("/api/app/admin", t.requests[0].path)
        assertEquals(true, data.bool("isOwner"))
        assertTrue(data.arr("users").size >= 3)
        assertTrue(data.arr("invites").isNotEmpty())
        assertEquals(403, RealBodiesPhase8B.status("GET /api/app/admin [admin_data_noperm_403]"))
    }

    @Test
    fun adminHealthAndOpsReport() = runBlocking {
        val t = QueueTransport(
            step("GET /api/app/admin/health-strip"),
            step("GET /api/app/admin/ops-report"),
        )
        val a = api(t)
        val health = a.adminHealthStrip()
        assertTrue(health.obj("services") != null)
        val report = a.adminOpsReport("2026-11")
        assertEquals("2026-11", report.str("month"))
        assertTrue(report.obj("sections") != null)
        assertTrue(t.requests[1].path.contains("month=2026-11"))
    }

    @Test
    fun adminRolesCrudChain() = runBlocking {
        val t = QueueTransport(
            step("GET /api/app/admin/roles"),
            step("POST /api/app/admin/roles"),
            step("PATCH /api/app/admin/roles/CaptureRole8B"),
            step("POST /api/app/admin/roles/CaptureRole8B/tabs"),
            step("DELETE /api/app/admin/roles/CaptureRole8B"),
        )
        val a = api(t)
        val roles = a.adminRoles()
        assertTrue(roles.arr("roles").size >= 3)
        val system = roles.arr("roles").first { it.str("name") == "OWNER" }
        assertEquals(true, system.bool("isSystem"))
        val created = a.adminRoleCreate(buildJsonObject { put("name", "CaptureRole8B") })
        assertEquals("Role created.", created.str("message"))
        assertEquals(201, RealBodiesPhase8B.status("POST /api/app/admin/roles"))
        val patched = a.adminRolePatch("CaptureRole8B", buildJsonObject { put("color", "#42a5f5") })
        assertTrue(patched.bool("ok") == true)
        val tabs = a.adminRoleTabs("CaptureRole8B", buildJsonObject { put("tickets", "read") })
        assertTrue(tabs.bool("ok") == true)
        val deleted = a.adminRoleDelete("CaptureRole8B")
        assertTrue(deleted.bool("ok") == true)
        assertEquals(
            listOf("GET", "POST", "PATCH", "POST", "DELETE"),
            t.requests.map { it.method },
        )
        assertEquals("/api/app/admin/roles/CaptureRole8B/tabs", t.requests[3].path)
    }

    @Test
    fun adminMembersProvisionAndPassphrase() = runBlocking {
        val t = QueueTransport(
            step("POST /api/app/admin/members"),
            step("POST /api/app/admin/members/4/passphrase"),
        )
        val a = api(t)
        val created = a.adminMemberCreate(
            buildJsonObject {
                put("displayName", "ProvCap8B")
                put("email", "prov@capture8b.example.invalid")
                put("role", "STAFF")
                put("accountType", "command")
            },
        )
        assertEquals(201, RealBodiesPhase8B.status("POST /api/app/admin/members"))
        assertEquals(4, created.int("userId"))
        assertEquals(true, created.bool("pendingSetup"))
        // The response carries delivery metadata only — no credential field.
        assertFalse(created.containsKey("passphrase"))
        assertFalse(created.containsKey("password"))
        val set = a.adminMemberPassphrase(4, "write-only-value")
        assertTrue(set.bool("ok") == true)
        assertEquals("/api/app/admin/members/4/passphrase", t.requests[1].path)
    }

    @Test
    fun adminSettingsSecurityAndRevokeAll() = runBlocking {
        val t = QueueTransport(
            step("POST /api/app/admin/settings"),
            step("POST /api/app/admin/security"),
            step("POST /api/app/admin/sessions/revoke-all"),
        )
        val a = api(t)
        val settings = a.adminSettingsSave(buildJsonObject { put("workspaceName", "Cap") })
        assertEquals("Settings updated.", settings.str("message"))
        val security = a.adminSecuritySave(buildJsonObject { put("lockdown", false) })
        assertEquals("Security settings updated.", security.str("message"))
        assertEquals(false, security.bool("lockdown"))
        val revoked = a.adminRevokeAllSessions()
        assertTrue(revoked.bool("ok") == true)
        assertEquals("/api/app/admin/sessions/revoke-all", t.requests[2].path)
    }

    @Test
    fun adminRecoveryCodesShapeOnly() = runBlocking {
        // Wrong-passphrase gate: the handler refuses, exactly as web.
        assertEquals(403, RealBodiesPhase8B.status("POST /api/app/admin/recovery-codes"))
        val t = QueueTransport(step("POST /api/app/admin/recovery-codes [admin_recovery_shapeonly]"))
        val r = api(t).adminRecoveryCodes("current-passphrase")
        assertEquals("Recovery codes regenerated.", r.str("message"))
        val codes = r.obj("recoveryCodes")!!
        // Fixture discipline: count + lengths only, never code values.
        assertEquals(true, codes.bool("_shape_only"))
        assertEquals(10, codes.int("count"))
        val lengths = (codes["lengths"] as JsonArray)
            .map { (it as JsonPrimitive).content.toInt() }
        assertTrue(lengths.all { it == 32 })
        assertEquals(
            listOf("10 recovery codes regenerated."),
            Phase8bLogic.recoveryCodeLines(r),
        )
    }

    @Test
    fun adminOversightSurfaces() = runBlocking {
        val t = QueueTransport(
            step("GET /api/app/admin/oversight/retention"),
            step("POST /api/app/admin/oversight/retention"),
            step("POST /api/app/admin/oversight/retention/report"),
            step("GET /api/app/admin/oversight/audit-log"),
            step("GET /api/app/admin/oversight/audit-export"),
            step("GET /api/app/admin/oversight/login-security"),
        )
        val a = api(t)
        val retention = a.adminRetention()
        assertTrue(retention.arr("settings").size >= 4)
        assertTrue(retention.str("note")!!.contains("never deletes"))
        val set = a.adminRetentionSet("activity", 90)
        val activity = set.arr("settings").first { it.str("entity") == "activity" }
        assertEquals(90, activity.int("retentionDays"))
        val run = a.adminRetentionReport()
        assertTrue(run.bool("ok") == true)
        val log = a.adminAuditLog()
        assertTrue(log.arr("rows").isNotEmpty())
        val export = a.adminAuditExport()
        assertEquals(23, export.int("count"))
        // The export fixture is a truncated descriptor, never CSV text.
        assertEquals(true, export.obj("csv")!!.bool("_truncated"))
        val loginSec = a.adminLoginSecurity()
        assertEquals(1, loginSec.int("activeSessions"))
    }

    @Test
    fun adminLawAiMaskedStateOnly() = runBlocking {
        val t = QueueTransport(
            step("GET /api/app/admin/law-ai"),
            step("POST /api/app/admin/law-ai"),
            step("GET /api/app/admin/law-ai/key-status"),
            step("POST /api/app/admin/law-ai/key"),
            step("GET /api/app/admin/law-ai/key-status [admin_lawai_keystatus_after]"),
        )
        val a = api(t)
        val config = a.adminLawAi()
        assertEquals("openrouter", config.str("provider"))
        assertEquals(true, config.bool("configured"))
        // Masked public state: no raw-key-shaped field exists.
        assertFalse(config.containsKey("apiKey"))
        assertFalse(config.containsKey("key"))
        assertFalse(config.containsKey("secret"))
        val summary = Phase8bLogic.lawAiSummary(config)
        assertTrue(summary.any { it.first == "Provider" && it.second == "openrouter" })
        val saved = a.adminLawAiSave(buildJsonObject { put("provider", "openrouter") })
        assertTrue(saved.bool("ok") == true)
        val status = a.adminLawAiKeyStatus()
        assertTrue(status.bool("ok") == true)
        val installed = a.adminLawAiKeyInstall("dummy-fixture-key")
        assertTrue(installed.bool("ok") == true)
        // The install response never echoes the key back.
        assertFalse(installed.toString().contains("dummy-fixture-key"))
        val after = a.adminLawAiKeyStatus()
        assertTrue(after.bool("ok") == true)
    }

    @Test
    fun adminLawAiTestEndpoint() = runBlocking {
        // No dedicated capture exists for the test call; pin
        // method + path against the key-status body (the route
        // itself is proven in the backend battery, Addendum 26).
        val t = QueueTransport(step("GET /api/app/admin/law-ai/key-status"))
        api(t).adminLawAiTest()
        assertEquals("POST", t.requests[0].method)
        assertEquals("/api/app/admin/law-ai/test", t.requests[0].path)
    }

    @Test
    fun adminBackupsStateMachine() = runBlocking {
        val t = QueueTransport(
            step("GET /api/app/admin/backups/status"),
            step("POST /api/app/admin/backups/request"),
        )
        val a = api(t)
        val status = a.adminBackupStatus()
        assertEquals("requested", status.str("state"))
        assertEquals("Requested", Phase8bLogic.backupStateLabel("requested"))
        assertEquals("No backup bundle", Phase8bLogic.backupStateLabel("none"))
        // A second request while one is outstanding is refused by
        // the handler in its own words (the captured 403 body).
        assertEquals(403, RealBodiesPhase8B.status("POST /api/app/admin/backups/request"))
        val refused = RealBodiesPhase8B.bodyJson("POST /api/app/admin/backups/request")
        assertEquals(false, refused.bool("authorized"))
        assertTrue(refused.str("message")!!.contains("already requested"))
        assertEquals("/api/app/admin/backups/status", t.requests[0].path)
    }

    @Test
    fun adminDmarcWindows() = runBlocking {
        val t = QueueTransport(step("GET /api/app/admin/dmarc?days=30"))
        val dmarc = api(t).adminDmarc(30)
        assertEquals(30, dmarc.int("windowDays"))
        assertEquals(true, dmarc.bool("empty"))
        assertTrue(t.requests[0].path.contains("days=30"))
        assertEquals(403, RealBodiesPhase8B.status("GET /api/app/admin/dmarc"))
        assertEquals(
            listOf(7, 30, 90, 365),
            Phase8bLogic.DMARC_WINDOWS.map { it.first },
        )
    }

    @Test
    fun opsViewsSurfaces() = runBlocking {
        val t = QueueTransport(
            step("GET /api/app/ops-views/risk-heatmap"),
            step("GET /api/app/ops-views/timeline"),
            step("GET /api/app/ops-views/contract-radar"),
            step("GET /api/app/ops-views/capacity"),
            step("POST /api/app/ops-views/capacity-threshold"),
        )
        val a = api(t)
        val heat = a.opsRiskHeatmap()
        assertEquals(2, heat.arr("rows").size)
        assertEquals("medium", heat.arr("rows")[0].str("riskLevel"))
        assertTrue(a.opsTimeline().bool("ok") == true)
        assertTrue(a.opsContractRadar().bool("ok") == true)
        val capacity = a.opsCapacity()
        assertTrue(capacity.arr("weeks").isNotEmpty())
        val set = a.opsCapacityThreshold(40.0)
        assertTrue(set.bool("ok") == true)
        assertEquals("/api/app/ops-views/capacity-threshold", t.requests[4].path)
    }

    @Test
    fun pipelineFieldSurfaces() = runBlocking {
        val t = QueueTransport(
            step("GET /api/app/pipeline-field/job-mode"),
            step("GET /api/app/pipeline-field/lead-scores"),
            step("GET /api/app/pipeline-field/lead-roi"),
            step("POST /api/app/pipeline-field/asset-import/preview"),
            step("POST /api/app/pipeline-field/asset-import/confirm"),
            step("POST /api/app/pipeline-field/counts"),
            step("POST /api/app/pipeline-field/counts/1/scan"),
            step("POST /api/app/pipeline-field/counts/1/close"),
            step("POST /api/app/pipeline-field/counts/1/apply"),
            step("GET /api/app/pipeline-field/counts"),
            step("POST /api/app/pipeline-field/scan"),
        )
        val a = api(t)
        val jobMode = a.fieldJobMode()
        assertEquals("2026-11-03", jobMode.str("today"))
        assertTrue(a.fieldLeadScores().arr("rules").isNotEmpty())
        assertTrue(a.fieldLeadRoi().bool("ok") == true)
        val preview = a.fieldImportPreview(buildJsonObject { put("fileName", "cap-assets.csv") })
        assertEquals("staged", preview.obj("batch")!!.str("state"))
        val confirm = a.fieldImportConfirm(buildJsonObject { put("batchId", 1) })
        assertTrue(confirm.bool("ok") == true)
        val started = a.fieldCountStart(buildJsonObject { put("label", "Count") })
        assertEquals(201, RealBodiesPhase8B.status("POST /api/app/pipeline-field/counts"))
        assertTrue(started.bool("ok") == true)
        assertTrue(a.fieldCountScan(1, buildJsonObject { put("code", "CW8B") }).bool("ok") == true)
        assertTrue(a.fieldCountClose(1).bool("ok") == true)
        val applied = a.fieldCountApply(1)
        assertTrue(applied.bool("ok") == true)
        val counts = a.fieldCounts()
        assertEquals("applied", counts.arr("sessions")[0].str("status"))
        assertTrue(a.fieldScan(buildJsonObject { put("code", "CW8B") }).bool("ok") == true)
        // Sync-push with an empty queue is refused by the handler.
        assertEquals(403, RealBodiesPhase8B.status("POST /api/app/pipeline-field/sync-push"))
    }

    @Test
    fun reportsFeedbackSurfaces() = runBlocking {
        val t = QueueTransport(
            step("POST /api/app/reports"),
            step("GET /api/app/reports"),
            step("GET /api/app/reports/1/document"),
            step("POST /api/app/reports/surveys"),
            step("GET /api/app/reports/surveys"),
            step("POST /api/app/reports/referrals"),
            step("GET /api/app/reports/referrals"),
            step("POST /api/app/reports/threat-briefs"),
            step("GET /api/app/reports/threat-briefs"),
            step("POST /api/app/reports/schedules"),
            step("GET /api/app/reports/schedules"),
            step("POST /api/app/reports/schedules/2/run"),
            step("GET /api/app/reports/snapshots"),
        )
        val a = api(t)
        val generated = a.reportsGenerate(buildJsonObject { put("clientId", 1) })
        assertTrue(generated.str("message")!!.contains("nothing was sent"))
        val list = a.reportsList()
        assertEquals(1, list.arr("reports").size)
        assertTrue(a.reportDocument(1).bool("ok") == true)
        val survey = a.reportSurveyCreate(buildJsonObject { put("clientId", 1) })
        assertEquals(201, RealBodiesPhase8B.status("POST /api/app/reports/surveys"))
        // The token rides the create response (shown once, as web).
        assertTrue(Phase8bLogic.shownOnceValue(survey, "surveyToken") != null)
        assertTrue(a.reportSurveys().bool("ok") == true)
        assertTrue(a.reportReferralSave(buildJsonObject { put("name", "Cap") }).bool("ok") == true)
        assertTrue(a.reportReferrals().bool("ok") == true)
        val brief = a.reportBriefGenerate(buildJsonObject { put("clientId", 1) })
        assertTrue(brief.str("message")!!.contains("nothing was sent"))
        assertTrue(a.reportBriefs().bool("ok") == true)
        val schedule = a.reportScheduleSave(buildJsonObject { put("kind", "owner_digest") })
        assertEquals("weekly", schedule.obj("schedule")!!.str("cadence"))
        assertTrue(a.reportSchedules().bool("ok") == true)
        assertTrue(a.reportScheduleRun(2).bool("ok") == true)
        assertTrue(a.reportSnapshots().arr("snapshots").isNotEmpty())
    }

    @Test
    fun appIntegrationsOwnerSurface() = runBlocking {
        assertEquals(403, RealBodiesPhase8B.status("GET /api/app/app-integrations [b4c_admin_staff_403]"))
        val t = QueueTransport(
            step("GET /api/app/app-integrations"),
            step("POST /api/app/app-integrations/api-keys"),
            step("POST /api/app/app-integrations/api-keys/1/revoke"),
            step("POST /api/app/app-integrations/webhooks"),
            step("POST /api/app/app-integrations/webhooks/1/active"),
            step("POST /api/app/app-integrations/deliveries/run"),
        )
        val a = api(t)
        val admin = a.appIntegrations()
        assertTrue(admin.arr("devices").size >= 3)
        val key = a.appApiKeyCreate(buildJsonObject { put("name", "Capture Partner") })
        assertEquals(201, RealBodiesPhase8B.status("POST /api/app/app-integrations/api-keys"))
        // Shown-once secret is shape-only in the fixture: no value exists.
        assertEquals(true, key.obj("key")!!.bool("_shape_only"))
        assertEquals(null, Phase8bLogic.shownOnceValue(key, "key"))
        assertEquals("Capture Partner", key.obj("apiKey")!!.str("name"))
        assertTrue(a.appApiKeyRevoke(1).bool("ok") == true)
        val hook = a.appWebhookCreate(buildJsonObject { put("name", "Capture Hook") })
        assertEquals(true, hook.obj("secret")!!.bool("_shape_only"))
        assertEquals("Capture Hook", hook.obj("webhook")!!.str("name"))
        assertTrue(a.appWebhookActive(1, false).bool("ok") == true)
        assertTrue(a.appDeliveriesRun().bool("ok") == true)
    }

    @Test
    fun clientImportNeverCreatesLiveClients() = runBlocking {
        val t = QueueTransport(
            step("POST /api/app/client-import/preview"),
            step("GET /api/app/client-import"),
            step("GET /api/app/client-import/1"),
            step("POST /api/app/client-import/1/confirm"),
        )
        val a = api(t)
        val preview = a.clientImportPreview(
            buildJsonObject {
                put("fileName", "cap-clients.csv")
                put("content", "name,email")
            },
        )
        val batch = preview.obj("batch")!!
        assertEquals("dryrun", batch.str("state"))
        assertEquals(0, batch.int("stagedCount"))
        assertTrue(a.clientImports().bool("ok") == true)
        val got = a.clientImport(1)
        assertEquals("valid", got.obj("batch")!!.arr("rows")[0].str("verdict"))
        val confirmed = a.clientImportConfirm(1)
        assertTrue(confirmed.bool("ok") == true)
        // Confirm stages into the review queue: the handler's own
        // message says no client was created, and every staged row
        // carries a submission id with a null client id.
        assertTrue(confirmed.str("message")!!.contains("No client was created"))
        assertEquals(1, confirmed.int("stagedCount"))
        val stagedRow = confirmed.arr("staged")[0]
        assertTrue(stagedRow.int("submissionId") != null)
        assertTrue(stagedRow["clientId"] is kotlinx.serialization.json.JsonNull)
    }

    @Test
    fun packetsRoutesSurfaces() = runBlocking {
        val t = QueueTransport(
            step("GET /api/app/packets-routes/ops"),
            step("GET /api/app/packets-routes/packets"),
            step("POST /api/app/packets-routes/packets"),
            step("POST /api/app/packets-routes/packets/1/mark-mailed"),
            step("GET /api/app/packets-routes/route"),
            step("GET /api/app/packets-routes/claims"),
        )
        val a = api(t)
        val ops = a.packetsOps()
        assertEquals(0, ops.obj("urgent")!!.int("total"))
        assertTrue(a.packetsList().bool("ok") == true)
        assertTrue(a.packetBuild(buildJsonObject { put("clientId", 2) }).bool("ok") == true)
        assertTrue(a.packetMarkMailed(1).bool("ok") == true)
        val route = a.packetsRoute()
        assertEquals(true, route.bool("readOnly"))
        assertTrue(route.str("method")!!.contains("No external geocoding"))
        val claims = a.packetsClaims()
        assertTrue(claims.arr("claims").isEmpty())
        assertTrue(
            (claims["statuses"] as JsonArray)
                .map { (it as JsonPrimitive).content }.contains("filed"),
        )
    }

    @Test
    fun collectionsWorkflowKeepsInvoiceStatus() = runBlocking {
        val t = QueueTransport(
            step("GET /api/app/collections"),
            step("POST /api/app/collections/invoices/2/workflow"),
        )
        val a = api(t)
        val board = a.collectionsBoard()
        val first = board.arr("buckets")[0].arr("invoices")[0]
        assertEquals("Sent", first.str("status"))
        assertEquals("none", first.str("workflowState"))
        val set = a.collectionsWorkflow(2, buildJsonObject { put("workflowState", "contacted") })
        assertTrue(set.bool("ok") == true)
        assertEquals("/api/app/collections/invoices/2/workflow", t.requests[1].path)
    }

    @Test
    fun staffGearSurfaces() = runBlocking {
        val t = QueueTransport(
            step("GET /api/app/staff-gear/directory"),
            step("GET /api/app/staff-gear/training"),
            step("GET /api/app/staff-gear/equipment"),
            step("POST /api/app/staff-gear/certs"),
            step("POST /api/app/staff-gear/equipment"),
        )
        val a = api(t)
        assertTrue(a.staffGearDirectory().arr("staff").isNotEmpty())
        assertTrue(a.staffGearTraining().obj("counts") != null)
        assertTrue(a.staffGearEquipment().arr("equipment").isEmpty())
        val cert = a.staffGearCertSave(
            buildJsonObject {
                put("userId", 2)
                put("name", "Capture Cert 8B")
                put("issuer", "CaptureOrg")
            },
        )
        assertEquals("Certification saved.", cert.str("message"))
        assertTrue(
            a.staffGearEquipmentSave(buildJsonObject { put("name", "Cap Tool") }).bool("ok") == true,
        )
    }

    @Test
    fun automationSurfaces() = runBlocking {
        val t = QueueTransport(
            step("GET /api/app/automation/assign-rules"),
            step("POST /api/app/automation/assign-rules"),
            step("GET /api/app/automation/escalation-config"),
            step("GET /api/app/automation/escalations"),
            step("GET /api/app/automation/quote-followups"),
            step("GET /api/app/automation/contract-reminders"),
        )
        val a = api(t)
        assertTrue(a.automationRules().bool("ok") == true)
        val saved = a.automationRuleSave(
            buildJsonObject {
                put("name", "Capture Rule 8B")
                put("matchCategory", "Support")
                put("assignTo", 2)
            },
        )
        assertEquals("Assignment rule saved.", saved.str("message"))
        val config = a.automationEscalationConfig()
        assertTrue(config.arr("config").isNotEmpty())
        assertTrue(a.automationEscalations().arr("escalations").isEmpty())
        assertTrue(a.automationQuoteFollowups().bool("ok") == true)
        assertTrue(a.automationContractReminders().bool("ok") == true)
    }

    @Test
    fun clientOpsSurfaces() = runBlocking {
        val t = QueueTransport(
            step("GET /api/app/client-ops/maintenance-windows?clientId=1"),
            step("POST /api/app/client-ops/maintenance-windows"),
            step("GET /api/app/client-ops/retainers?clientId=1"),
            step("POST /api/app/client-ops/retainers"),
            step("POST /api/app/client-ops/emergency-contacts"),
            step("GET /api/app/client-ops/emergency-contacts?clientId=1"),
        )
        val a = api(t)
        assertTrue(a.clientOpsWindows(1).arr("windows").isEmpty())
        assertTrue(
            a.clientOpsWindowSave(buildJsonObject { put("clientId", 1) }).bool("ok") == true,
        )
        assertTrue(a.clientOpsRetainers(1).bool("ok") == true)
        val retainer = a.clientOpsRetainerSave(buildJsonObject { put("clientId", 1) })
        assertEquals("Retainer saved.", retainer.str("message"))
        assertTrue(
            a.clientOpsEmergencySave(buildJsonObject { put("clientId", 1) }).bool("ok") == true,
        )
        assertTrue(a.clientOpsEmergency(1).bool("ok") == true)
        assertTrue(t.requests[0].path.contains("clientId=1"))
    }

    @Test
    fun pipelinePacketsSurfaces() = runBlocking {
        val t = QueueTransport(
            step("POST /api/app/pipeline-packets/leads"),
            step("GET /api/app/pipeline-packets/leads"),
            step("POST /api/app/pipeline-packets/leads/1/state"),
            step("POST /api/app/pipeline-packets/leads/1/convert"),
            step("GET /api/app/pipeline-packets/proposals"),
            step("GET /api/app/pipeline-packets/change-requests"),
        )
        val a = api(t)
        val saved = a.pipelineLeadSave(
            buildJsonObject {
                put("business", "Capture Lead Co 8B")
                put("email", "lou@capture8b.example.invalid")
            },
        )
        assertEquals("Lead added.", saved.str("message"))
        assertEquals(true, saved.bool("created"))
        assertTrue(a.pipelineLeads().arr("leads").isNotEmpty())
        assertTrue(
            a.pipelineLeadState(1, buildJsonObject { put("state", "contacted") }).bool("ok") == true,
        )
        val converted = a.pipelineLeadConvert(1)
        assertTrue(converted.bool("ok") == true)
        // Conversion enqueues a review submission, never a client:
        // the lead records the submission id and a null client id.
        assertTrue(converted.int("submissionId") != null)
        assertEquals(null, converted.obj("lead")!!.int("convertedClientId"))
        assertTrue(converted.str("message")!!.contains("review queue"))
        assertTrue(a.pipelineProposals().arr("proposals").isEmpty())
        assertTrue(a.pipelineChangeRequests().arr("changeRequests").isEmpty())
    }

    @Test
    fun phase8bLogicPins() {
        assertEquals(
            "Capture Fixture LLC",
            Phase8bLogic.rowTitle(
                RealBodiesPhase8B.bodyJson("GET /api/app/ops-views/risk-heatmap").arr("rows")[0],
            ),
        )
        val dmarc = RealBodiesPhase8B.bodyJson("GET /api/app/admin/dmarc?days=30")
        assertEquals(0, dmarc.obj("summary")!!.int("volume"))
    }
}

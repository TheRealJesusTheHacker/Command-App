package net.sentinelsafeguard.app

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
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
import net.sentinelsafeguard.app.data.api.MeResponse
import net.sentinelsafeguard.app.data.api.SentinelApi
import net.sentinelsafeguard.app.data.api.portalApprovalInbox
import net.sentinelsafeguard.app.data.api.portalAuthorizations
import net.sentinelsafeguard.app.data.api.portalChangeRequestDecide
import net.sentinelsafeguard.app.data.api.portalChangeRequests
import net.sentinelsafeguard.app.data.api.portalContacts
import net.sentinelsafeguard.app.data.api.portalContract
import net.sentinelsafeguard.app.data.api.portalContractDocument
import net.sentinelsafeguard.app.data.api.portalContractSign
import net.sentinelsafeguard.app.data.api.portalContracts
import net.sentinelsafeguard.app.data.api.portalCriticalAlert
import net.sentinelsafeguard.app.data.api.portalFileRequestUpload
import net.sentinelsafeguard.app.data.api.portalFileRequests
import net.sentinelsafeguard.app.data.api.portalHealthTimeline
import net.sentinelsafeguard.app.data.api.portalHistory
import net.sentinelsafeguard.app.data.api.portalHome
import net.sentinelsafeguard.app.data.api.portalKnowledge
import net.sentinelsafeguard.app.data.api.portalLang
import net.sentinelsafeguard.app.data.api.portalLangSet
import net.sentinelsafeguard.app.data.api.portalNotificationPrefs
import net.sentinelsafeguard.app.data.api.portalNotificationPrefsSet
import net.sentinelsafeguard.app.data.api.portalPacketDocument
import net.sentinelsafeguard.app.data.api.portalPackets
import net.sentinelsafeguard.app.data.api.portalProposalDecide
import net.sentinelsafeguard.app.data.api.portalProposals
import net.sentinelsafeguard.app.data.api.portalQuoteDecide
import net.sentinelsafeguard.app.data.api.portalReferrals
import net.sentinelsafeguard.app.data.api.portalSignOffs
import net.sentinelsafeguard.app.data.api.portalStatusBoard
import net.sentinelsafeguard.app.data.api.portalSurveyAnswer
import net.sentinelsafeguard.app.data.api.portalSurveys
import net.sentinelsafeguard.app.data.api.portalTicketMessagePost
import net.sentinelsafeguard.app.data.api.portalTicketMessages
import net.sentinelsafeguard.app.ui.MirrorNav
import net.sentinelsafeguard.app.ui.Phase9Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Phase 9 (hunt14) contract shapes + role shapes: every portal
 * endpoint driven through the app's API methods against REAL
 * bodies captured from the deployed Phase 9 backend (see
 * [RealBodiesPhase9]; Addendum 28) — including the portal flag
 * on /api/app/me, the shape-only login token, and the refusal
 * bodies a portal token gets on staff endpoints. The role-shape
 * tests pin the three shells from the /api/app/me payload:
 * portal (Client/Family), STAFF (exactly the web subset, My
 * Schedule included, everything else absent), and Command.
 */
class ContractPhase9Test {

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
        RealBodiesPhase9.status(name) to RealBodiesPhase9.body(name)

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.content

    private fun JsonObject.int(key: String): Int? =
        (this[key] as? JsonPrimitive)?.content?.toIntOrNull()

    private fun JsonObject.bool(key: String): Boolean? =
        (this[key] as? JsonPrimitive)?.content?.toBooleanStrictOrNull()

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.arr(key: String): List<JsonObject> =
        (this[key] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

    // ---- /me + login: the shell-routing payloads ----

    @Test
    fun meCarriesPortalFlagForPortalRoles() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
        val me = json.decodeFromString<MeResponse>(
            RealBodiesPhase9.body("GET /api/app/me"),
        )
        assertTrue(me.portal)
        assertEquals("Client", me.user.role)
        assertEquals(1, me.user.clientId)
        assertEquals(
            MirrorNav.ShellKind.PORTAL,
            MirrorNav.shellKind(me.user.role, me.portal),
        )
    }

    @Test
    fun portalLoginBodyIsShapeOnlyForTheToken() {
        val body = RealBodiesPhase9.bodyJson("POST /api/app/auth/login (portal)")
        assertEquals("Client", body.obj("user")!!.str("role"))
        // The capture records the token as shape only — no value.
        assertFalse(body.containsKey("deviceToken"))
        val shape = body.obj("deviceTokenShape")!!
        assertEquals(64, shape.int("length"))
        assertEquals(8, shape.str("sha256Prefix")!!.length)
    }

    @Test
    fun shellKindRouting() {
        assertEquals(MirrorNav.ShellKind.PORTAL, MirrorNav.shellKind("Client", true))
        assertEquals(MirrorNav.ShellKind.COMMAND, MirrorNav.shellKind("OWNER", false))
        assertEquals(MirrorNav.ShellKind.COMMAND, MirrorNav.shellKind("STAFF", false))
        // 1.5.4 (role-structure window): "Family" and "Tourist"
        // are retired account roles — production cannot mint
        // either. The portal-by-Family branch and the old
        // refused-by-Tourist shell are gone; both strings now
        // land in the defensive default (PORTAL, least
        // privilege), never a privileged path. The full matrix
        // (structureRole resolution, disagreements, the
        // defaulted predicate) is pinned in RoleStructure154Test.
        assertEquals(MirrorNav.ShellKind.PORTAL, MirrorNav.shellKind("Family", true))
        assertEquals(MirrorNav.ShellKind.PORTAL, MirrorNav.shellKind("Family", false))
        assertEquals(MirrorNav.ShellKind.PORTAL, MirrorNav.shellKind("Tourist", false))
        assertEquals(MirrorNav.ShellKind.PORTAL, MirrorNav.shellKind("Tourist", true))
    }

    // ---- Role shapes: the three drawers ----

    @Test
    fun portalShellIsExactlyTheTwelveWebViewsInOrder() {
        assertEquals(
            listOf(
                "My Profile", "Progress", "History", "Your Documents",
                "My Files & Packets", "Messages", "Critical Alert",
                "My Sign-offs", "Contracts & Reports", "Knowledge",
                "Status Board", "Feedback & Referrals",
            ),
            MirrorNav.PORTAL_VIEWS.map { it.label },
        )
        assertEquals(12, MirrorNav.PORTAL_VIEWS.map { it.key }.distinct().size)
        assertEquals("portal_profile", MirrorNav.portalLandingKey())
        // No placeholder survives anywhere in the nav structure.
        assertTrue(MirrorNav.PORTAL_VIEWS.all { it.phase == null })
        assertTrue(MirrorNav.GROUPS.flatMap { it.views }.all { it.phase == null })
        assertTrue(MirrorNav.INJECTED.all { it.phase == null })
        assertTrue(MirrorNav.ADMIN_GROUPS.flatMap { it.views }.all { it.phase == null })
    }

    @Test
    fun staffShellIsExactlyTheWebSubset() {
        val groups = MirrorNav.visibleGroups("STAFF", emptyMap(), taxOnly = false)
        val keys = groups.flatMap { it.views }.map { it.key }
        assertEquals(MirrorNav.STAFF_KEYS, keys.toSet())
        assertEquals(7, keys.size)
        // My Schedule is present; the full Schedule view is absent.
        assertTrue("myschedule" in keys)
        assertFalse("schedule" in keys)
        // Every non-subset view is absent, not merely disabled.
        listOf(
            "today", "clients", "invoices", "employees", "engagements",
            "b4fdesk", "cf2assurance", "b3money", "liveintel", "archive",
            "b3ops", "cf2firmops", "b4assess", "activity", "admin",
        ).forEach { assertFalse("$it must be absent for STAFF", it in keys) }
        assertTrue(MirrorNav.visibleInjected("STAFF", emptyMap(), taxOnly = false).isEmpty())
        assertEquals("dashboard", MirrorNav.landingKey("STAFF", taxOnly = false))
    }

    @Test
    fun ownerShellUnaffectedByStaffAndPortalShapes() {
        val groups = MirrorNav.visibleGroups("OWNER", emptyMap(), taxOnly = false)
        val keys = groups.flatMap { it.views }.map { it.key }
        assertTrue("schedule" in keys)
        assertFalse("myschedule" in keys)
        assertTrue("today" in keys && "admin" in keys)
        assertEquals(12, MirrorNav.visibleInjected("OWNER", emptyMap(), taxOnly = false).size)
        assertEquals("today", MirrorNav.landingKey("OWNER", taxOnly = false))
        // Flag gating still applies exactly as web.
        val flagged = MirrorNav.visibleGroups(
            "OWNER", mapOf("lawAccess" to true), taxOnly = false,
        ).flatMap { it.views }.map { it.key }
        assertTrue("law" in flagged)
        assertFalse("osint" in flagged)
    }

    // ---- Portal reads against the real bodies ----

    @Test
    fun portalHomeComposition() = runBlocking {
        val t = QueueTransport(step("GET /api/app/portal"))
        val home = api(t).portalHome()
        assertEquals("/api/app/portal", t.requests[0].path)
        assertEquals(true, home.bool("linked"))
        assertEquals("en", home.str("portalLang"))
        val profile = home.obj("profile")!!
        assertTrue(profile.str("legalName")!!.isNotBlank())
        assertEquals(1, home.arr("tickets").size)
        assertEquals(1, home.arr("progress").size)
        assertEquals(1, home.arr("library").size)
        val progress = home.arr("progress")[0]
        assertTrue(Phase9Logic.progressLine(progress).contains("of"))
        assertTrue(home.obj("notificationPrefs") != null)
        assertTrue(home.obj("securityScore") != null)
    }

    @Test
    fun portalHistoryAndHealthTimeline() = runBlocking {
        val t = QueueTransport(
            step("GET /api/app/portal/history"),
            step("GET /api/app/portal/health-timeline"),
        )
        val a = api(t)
        val history = a.portalHistory()
        assertEquals(1, history.arr("history").size)
        assertTrue(history.arr("history")[0].str("title")!!.isNotBlank())
        val health = a.portalHealthTimeline()
        assertEquals(2, health.int("total"))
        assertEquals(2, health.arr("events").size)
        assertEquals("/api/app/portal/health-timeline", t.requests[1].path)
    }

    @Test
    fun portalKnowledgeIsPortalAudienceOnly() = runBlocking {
        val t = QueueTransport(step("GET /api/app/portal/knowledge"))
        val kb = api(t).portalKnowledge()
        assertEquals(true, kb.bool("linked"))
        assertEquals(1, kb.arr("library").size)
        assertTrue(kb.arr("library")[0].str("title")!!.isNotBlank())
    }

    @Test
    fun portalContractsSignOffsAndDocuments() = runBlocking {
        val t = QueueTransport(
            step("GET /api/app/portal/contracts"),
            step("GET /api/app/portal/contracts/{id}"),
            step("GET /api/app/portal/sign-offs"),
            step("GET /api/app/portal/authorizations"),
        )
        val a = api(t)
        val contracts = a.portalContracts()
        assertEquals(1, contracts.arr("contracts").size)
        val contract = contracts.arr("contracts")[0]
        assertTrue(Phase9Logic.contractSignable(contract))
        val view = a.portalContract(contract.int("id")!!)
        assertEquals(contract.str("title"), view.obj("contract")!!.str("title"))
        val signOffs = a.portalSignOffs()
        assertTrue(signOffs.arr("signOffs").isEmpty())
        val authz = a.portalAuthorizations()
        assertTrue(authz.arr("authorizations").isEmpty())
        assertEquals("/api/app/portal/authorizations", t.requests[3].path)
    }

    @Test
    fun portalMessagesThreadShape() = runBlocking {
        val t = QueueTransport(step("GET /api/app/portal/tickets/{id}/messages"))
        val thread = api(t).portalTicketMessages(1)
        assertEquals(1, thread.int("ticketId"))
        assertEquals(1, thread.int("clientId"))
        assertTrue(thread.arr("messages").isEmpty())
        assertEquals("/api/app/portal/tickets/1/messages", t.requests[0].path)
    }

    @Test
    fun portalPacketsAndDocument() = runBlocking {
        val t = QueueTransport(
            step("GET /api/app/portal/packets"),
            step("GET /api/app/portal/packets/{id}/document"),
            step("GET /api/app/portal/file-requests"),
        )
        val a = api(t)
        val packets = a.portalPackets()
        assertEquals(1, packets.arr("packets").size)
        val doc = a.portalPacketDocument(packets.arr("packets")[0].int("id")!!)
        assertEquals(746, doc.int("pdfSizeBytes"))
        assertTrue(doc.str("pdfBase64")!!.startsWith("JVBERi"))
        assertEquals(64, doc.str("pdfSha256")!!.length)
        val requests = a.portalFileRequests()
        assertTrue(requests.arr("requests").isEmpty())
    }

    @Test
    fun portalStatusSurveysReferralsProposalsChangesInbox() = runBlocking {
        val t = QueueTransport(
            step("GET /api/app/portal/status-board"),
            step("GET /api/app/portal/surveys"),
            step("GET /api/app/portal/referrals"),
            step("GET /api/app/portal/proposals"),
            step("GET /api/app/portal/change-requests"),
            step("GET /api/app/portal/approval-inbox"),
        )
        val a = api(t)
        val board = a.portalStatusBoard()
        assertTrue(board.obj("board") != null)
        assertEquals(1, board.obj("board")!!.int("clientId"))
        assertTrue(a.portalSurveys().arr("surveys").isEmpty())
        val referrals = a.portalReferrals()
        assertEquals(0, referrals.int("count"))
        assertTrue(a.portalProposals().arr("proposals").isEmpty())
        assertTrue(a.portalChangeRequests().arr("changeRequests").isEmpty())
        val inbox = a.portalApprovalInbox()
        assertEquals(true, inbox.bool("linked"))
        assertEquals(0, Phase9Logic.inboxTotal(inbox))
        assertEquals("/api/app/portal/approval-inbox", t.requests[5].path)
    }

    @Test
    fun portalPrefsContactsLang() = runBlocking {
        val t = QueueTransport(
            step("GET /api/app/portal/notification-prefs"),
            step("GET /api/app/portal/contacts"),
            step("GET /api/app/portal/lang"),
        )
        val a = api(t)
        val prefs = a.portalNotificationPrefs()
        assertEquals(1, prefs.int("clientId"))
        assertTrue(prefs.obj("prefs") != null)
        val contacts = a.portalContacts()
        assertEquals(1, contacts.int("clientId"))
        assertTrue(contacts.arr("contacts").isEmpty())
        assertEquals("en", a.portalLang().str("portalLang"))
    }

    // ---- Refusals: the portal token on staff surfaces ----

    @Test
    fun portalTokenRefusalBodiesAreTheServersOwn() {
        for (name in listOf(
            "GET /api/app/my-schedule (portal token — refusal)",
            "GET /api/app/clients (portal token — refusal)",
        )) {
            assertEquals(403, RealBodiesPhase9.status(name))
            val body = RealBodiesPhase9.bodyJson(name)
            assertEquals(false, body.bool("ok"))
            assertEquals(false, body.bool("authorized"))
            assertEquals("Not authorized.", body.str("message"))
        }
    }

    @Test
    fun myScheduleRefusalSurfacesAsApiException() = runBlocking {
        val t = QueueTransport(step("GET /api/app/my-schedule (portal token — refusal)"))
        try {
            api(t).myScheduleWindow("2026-11-09", "2026-11-16")
            fail("expected the refusal to throw")
        } catch (e: ApiException) {
            assertEquals(403, e.status)
        }
        assertTrue(t.requests[0].path.startsWith("/api/app/my-schedule?start=2026-11-09"))
    }

    @Test
    fun myScheduleParsesTheScopedPayload() = runBlocking {
        // The backend battery (Addendum 28) proves the real body:
        // scope "mine" with only the staffer's jobs. This pins the
        // app's parse of that shape + the method/path.
        val body = """{"ok":true,"scope":"mine","jobs":[{"id":7,"title":"Own job",""" +
            """"workDate":"2026-11-10","startTime":"09:00","endTime":"10:00",""" +
            """"status":"Scheduled","assignees":[],"assigneeIds":[]}],""" +
            """"employees":[],"entries":[],"range":{"startDate":"2026-11-09","endDate":"2026-11-16"}}"""
        val t = QueueTransport(200 to body)
        val resp = api(t).myScheduleWindow("2026-11-09", "2026-11-16")
        assertEquals("mine", resp.scope)
        assertEquals(listOf(7), resp.jobs.map { it.id })
        assertEquals("GET", t.requests[0].method)
    }

    // ---- Portal writes: method / path / body vocabularies ----

    @Test
    fun portalWriteEndpointsCarryWebVocabularies() = runBlocking {
        val ok = 200 to """{"ok":true,"message":"done"}"""
        val t = QueueTransport(ok, ok, ok, ok, ok, ok, ok, ok, ok, ok)
        val a = api(t)
        a.portalContractSign(3, "Ann Alpha")
        a.portalQuoteDecide(4, "approve")
        a.portalProposalDecide(5, "accept")
        a.portalChangeRequestDecide(6, "approve", "Ann Alpha")
        a.portalCriticalAlert("Help", "details")
        a.portalSurveyAnswer(7, 5, true, "great")
        a.portalFileRequestUpload(8, "alpha.pdf", "QUJD")
        a.portalLangSet("es")
        a.portalNotificationPrefsSet(buildJsonObject { put("digest", true) })
        a.portalTicketMessagePost(1, "hello")
        assertEquals(
            listOf(
                "/api/app/portal/contracts/3/sign",
                "/api/app/portal/quotes/4/decide",
                "/api/app/portal/proposals/5/decide",
                "/api/app/portal/change-requests/6/decide",
                "/api/app/portal/critical-alert",
                "/api/app/portal/surveys/7/answer",
                "/api/app/portal/file-requests/items/8/upload",
                "/api/app/portal/lang",
                "/api/app/portal/notification-prefs",
                "/api/app/portal/tickets/1/messages",
            ),
            t.requests.map { it.path },
        )
        assertTrue(t.requests.all { it.method == "POST" })
        assertTrue(t.requests[0].body!!.contains("\"signerName\":\"Ann Alpha\""))
        assertTrue(t.requests[1].body!!.contains("\"decision\":\"approve\""))
        assertTrue(t.requests[2].body!!.contains("\"decision\":\"accept\""))
        assertTrue(t.requests[3].body!!.contains("\"signerName\":\"Ann Alpha\""))
        assertTrue(t.requests[5].body!!.contains("\"rating\":5"))
        assertTrue(t.requests[5].body!!.contains("\"wouldRecommend\":true"))
        assertTrue(t.requests[6].body!!.contains("\"contentBase64\":\"QUJD\""))
        assertTrue(t.requests[7].body!!.contains("\"portalLang\":\"es\""))
        // No portal write ever carries a client id — identity is
        // the token's, server-side (Addendum 28).
        assertTrue(t.requests.none { it.body!!.contains("clientId") })
    }

    @Test
    fun phase9LogicVocabulariesMatchTheHandlers() {
        assertEquals(listOf("approve", "decline"), Phase9Logic.QUOTE_DECISIONS)
        assertEquals(listOf("accept", "decline"), Phase9Logic.PROPOSAL_DECISIONS)
        assertEquals(listOf("approve", "decline"), Phase9Logic.CHANGE_DECISIONS)
        assertEquals(listOf("en", "es"), Phase9Logic.PORTAL_LANGS.map { it.first })
        assertEquals(listOf(1, 2, 3, 4, 5), Phase9Logic.SURVEY_RATINGS)
    }
}

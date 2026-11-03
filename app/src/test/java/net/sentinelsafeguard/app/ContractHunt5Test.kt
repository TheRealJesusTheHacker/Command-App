package net.sentinelsafeguard.app

import kotlinx.coroutines.runBlocking
import net.sentinelsafeguard.app.config.ApiConfig
import net.sentinelsafeguard.app.data.api.ClientWriteRequest
import net.sentinelsafeguard.app.data.api.HttpRequest
import net.sentinelsafeguard.app.data.api.HttpResponse
import net.sentinelsafeguard.app.data.api.HttpTransport
import net.sentinelsafeguard.app.data.api.SentinelApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * hunt5 contract shapes: the mirror endpoints (B0–B8) driven
 * through the app's API methods against REAL bodies captured
 * from the deployed production code (see [RealBodiesHunt5]).
 */
class ContractHunt5Test {

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

    @Test
    fun meCarriesTheB0AccessFlags() = runBlocking {
        val me = api(QueueTransport(200 to RealBodiesHunt5.ME_H5)).me()
        assertEquals("OwnerCap", me.user.displayName)
        assertTrue(me.lawAccess)
        assertTrue(me.newsAccess)
        assertTrue(me.osintAccess)
        assertTrue(me.breachAccess)
        assertTrue(me.threatAccess)
        assertTrue(me.reviewsAccess)
        assertTrue(me.taxAccess)
        assertFalse(me.taxOnly)
    }

    @Test
    fun dashboardParsesTheWebAggregate() = runBlocking {
        val d = api(QueueTransport(200 to RealBodiesHunt5.DASHBOARD_H5)).dashboard()
        assertEquals("2026-11-02", d.today)
        assertEquals(7, d.tickets!!.openTotal)
        assertEquals(5, d.tickets!!.unassigned)
        assertEquals(4, d.tickets!!.byPriority.first { it.priority == "Normal" }.count)
        assertEquals(58, d.clients!!.total)
        assertEquals(15, d.clients!!.byTier["tier-a"])
        assertEquals(11700000L, d.clients!!.pipelineValueCents)
        assertEquals(500L, d.invoices!!.outstanding.totalCents)
        assertEquals(1, d.presence!!.activeSessionCount)
        assertEquals("OwnerCap", d.presence!!.team.single().displayName)
        assertEquals(19, d.attention.first { it.kind == "followups" }.count)
        assertTrue(d.canWrite.clients)
        assertTrue(d.canWrite.tickets)
        assertTrue(d.recentActivity.isNotEmpty())
    }

    @Test
    fun rosterParsesPubUsers() = runBlocking {
        val users = api(QueueTransport(200 to RealBodiesHunt5.ROSTER_H5)).roster()
        assertTrue(users.size >= 3)
        assertTrue(users.any { it.displayName == "OwnerCap" && it.role == "OWNER" })
        assertTrue(users.any { it.displayName == "StaffCap" && it.role == "STAFF" })
    }

    @Test
    fun ticketSearchRidesTheB3QueryParam() = runBlocking {
        val transport = QueueTransport(200 to RealBodiesHunt5.TICKETS_Q_H5)
        val page = api(transport).ticketsFiltered(q = "Capture ticket Open")
        assertEquals(1, page.total)
        assertEquals(2, page.items.single().id)
        assertTrue(transport.requests.single().path.contains("q=Capture+ticket+Open"))
    }

    @Test
    fun ticketDetailRendersCaseAndActivity() = runBlocking {
        val detail = api(QueueTransport(200 to RealBodiesHunt5.TICKET_DETAIL_H5)).ticketDetail(2)
        assertEquals("Capture ticket Open", detail.ticket.title)
        assertNotNull(detail.case)
        assertEquals("Capture case 1", detail.case!!.title)
        assertEquals(1, detail.activity.size)
        assertEquals("Ticket_SAVED", detail.activity.single().action)
        assertEquals("Capture Client 01 LLC", detail.client!!.legalName)
    }

    @Test
    fun portalThreadParsesMessages() = runBlocking {
        val thread = api(QueueTransport(200 to RealBodiesHunt5.PORTAL_MESSAGES_H5)).portalMessages(2)
        assertEquals(2, thread.ticketId)
        assertEquals(1, thread.messages.size)
        assertEquals("staff", thread.messages.single().senderKind)
        assertEquals("OwnerCap", thread.messages.single().senderName)
        assertEquals(1, thread.unreadForClient)
    }

    @Test
    fun clientTimelineParsesEntries() = runBlocking {
        val timeline = api(QueueTransport(200 to RealBodiesHunt5.TIMELINE_H5)).clientTimeline(59)
        assertEquals(59, timeline.clientId)
        assertEquals(2, timeline.total)
        assertEquals("stage", timeline.timeline.first().type)
        assertEquals("Pipeline stage changed", timeline.timeline.first().title)
    }

    @Test
    fun clientDetailCarriesFriendsAndRegisters() = runBlocking {
        val detail = api(QueueTransport(200 to RealBodiesHunt5.CLIENT_DETAIL_FRIENDS_H5))
            .clientDetail(56)
        assertEquals("Capture Friends LLC", detail.client.legalName)
        assertTrue(detail.client.friends)
        assertFalse(detail.client.familyLinked)
        assertNotNull(detail.notificationPrefs)
        assertTrue(detail.notificationPrefs!!.ticketUpdates)
        assertTrue(detail.cases.isEmpty())
        assertTrue(detail.verifications.isEmpty())
    }

    @Test
    fun pipelineCardsAreFullClients() = runBlocking {
        val board = api(QueueTransport(200 to RealBodiesHunt5.PIPELINE_H5)).pipeline()
        val lead = board.stages.first { it.stage == "Lead" }
        assertEquals(7, lead.count)
        val card = lead.clients.first { it.id == 1 }
        assertEquals("Capture Client 01 LLC", card.legalName)
        assertEquals("CapDBA 1", card.dba)
        assertEquals("tier-a", card.tier)
        assertFalse(card.familyLinked)
        assertFalse(card.friends)
        assertEquals("555-0100", card.phone)
    }

    @Test
    fun clientCreateParsesThe201Body() = runBlocking {
        val transport = QueueTransport(201 to RealBodiesHunt5.CLIENT_CREATE_H5)
        val client = api(transport).createClient(
            ClientWriteRequest(legalName = "App Created Capture LLC", stage = "Qualified")
        )
        assertEquals(59, client.id)
        assertEquals("Qualified", client.stage)
        assertEquals(250.0, client.value!!, 0.001)
    }

    @Test
    fun followupSnoozeParsesTheClient() = runBlocking {
        val client = api(QueueTransport(200 to RealBodiesHunt5.FOLLOWUP_H5))
            .clientFollowup(59, days = 7)
        assertEquals("2026-11-09", client.nextFollowUp)
        assertEquals("Proposal", client.stage)
    }

    @Test
    fun ticketClosePatchParsesTheClosedTicket() = runBlocking {
        val transport = QueueTransport(200 to RealBodiesHunt5.TICKET_PATCH_CLOSE_H5)
        val api = api(transport)
        // The quick patch path is what board/profile stage moves use;
        // the body pins the post-close shape (closedDate stamped).
        val ticket = api.patchTicket(
            5,
            net.sentinelsafeguard.app.data.api.TicketPatchRequest(status = "Closed"),
        )
        assertEquals("Closed", ticket.status)
        assertEquals("2026-11-02", ticket.closedDate)
    }
}

package net.sentinelsafeguard.app

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.sentinelsafeguard.app.config.ApiConfig
import net.sentinelsafeguard.app.data.api.HttpRequest
import net.sentinelsafeguard.app.data.api.HttpResponse
import net.sentinelsafeguard.app.data.api.HttpTransport
import net.sentinelsafeguard.app.data.api.LoginResult
import net.sentinelsafeguard.app.data.api.OkResponse
import net.sentinelsafeguard.app.data.api.SentinelApi
import net.sentinelsafeguard.app.data.api.TicketCreateRequest
import net.sentinelsafeguard.app.data.api.TicketPatchRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract shapes — every response body here is a REAL capture
 * from the production server.py rehearsal (see [RealBodies]),
 * driven through the app's actual API methods and parsed with the
 * app's own Json config. hunt1 shipped a TOTP contract the backend
 * never sends and hunt2 shipped read models the backend never
 * sends; these tests pin the real contract so that defect class
 * cannot come back silently.
 */
class ContractShapeTest {

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

    // ---- auth ---------------------------------------------------

    @Test
    fun loginSuccessCarriesTheBackendUserObject() = runBlocking {
        val transport = QueueTransport(200 to RealBodies.LOGIN_SUCCESS)
        val result = api(transport).login("Reh Owner", "pass", "Rehearsal phone")
        val success = result as LoginResult.Success
        assertEquals("tok-rehearsal-a", success.token)
        assertEquals("Reh Owner", success.displayName)
        assertEquals("OWNER", success.role)
    }

    @Test
    fun loginDeniedShowsTheBackendMessage() = runBlocking {
        val transport = QueueTransport(403 to RealBodies.LOGIN_DENIED)
        val result = api(transport).login("Reh Owner", "wrong", "Rehearsal phone")
        assertEquals("Invalid credentials.", (result as LoginResult.Failed).message)
    }

    @Test
    fun loginTotpChallengeMatchesTheRealShape() = runBlocking {
        val transport = QueueTransport(200 to RealBodies.LOGIN_TOTP_REQUIRED)
        val result = api(transport).login("Reh Totp", "pass", "Second phone")
        assertEquals("ch-rehearsal", (result as LoginResult.TotpRequired).challenge)
    }

    @Test
    fun totpCompletionParsesTheRealSuccessBody() = runBlocking {
        val transport = QueueTransport(200 to RealBodies.LOGIN_TOTP_SUCCESS)
        val api = api(transport)
        val result = api.completeTotp("ch-rehearsal", "123456", "Second phone")
        val success = result as LoginResult.Success
        assertEquals("tok-rehearsal-b", success.token)
        assertEquals("Reh Totp", success.displayName)
        // The completion call must not resend the passphrase.
        assertFalse(transport.requests.single().body!!.contains("passphrase"))
    }

    @Test
    fun meParsesTheRealBody() = runBlocking {
        val transport = QueueTransport(200 to RealBodies.ME)
        val me = api(transport).me()
        assertEquals(1, me.user.id)
        assertEquals("Reh Owner", me.user.displayName)
        assertFalse(me.user.totpEnrolled) // the rehearsal owner has no TOTP; Reh Totp does
        assertTrue(me.serverTime > 0)
        assertEquals("Rehearsal phone", me.device?.deviceName)
        assertEquals(true, me.device?.current)
    }

    // ---- today --------------------------------------------------

    @Test
    fun todayParsesPopulatedSections() = runBlocking {
        val transport = QueueTransport(200 to RealBodies.TODAY)
        val today = api(transport).today()
        assertEquals("2026-11-02", today.today)

        assertEquals(1, today.followUps.total)
        val followUp = today.followUps.items.single()
        assertEquals("Rehearsal Bakery LLC", followUp.legalName)
        assertEquals("tier-a", followUp.tier)
        assertEquals(2500.0, followUp.value!!, 0.001)

        assertEquals(1, today.newTickets.total)
        val ticket = today.newTickets.items.single()
        assertEquals(1, ticket.id)
        assertEquals("Rehearsal Bakery LLC", ticket.clientName)
        assertEquals("High", ticket.priority)

        assertEquals(1, today.overdueCases.total)
        assertEquals("Laptop fleet audit", today.overdueCases.items.single().title)
        assertEquals("Rehearsal Bakery LLC", today.overdueCases.items.single().clientName)

        assertEquals(1, today.tierA.total)
        assertEquals(1, today.signinAlerts.total)
        val alert = today.signinAlerts.items.single()
        assertEquals("Reh Owner", alert.actorName)
        assertEquals("203.0.113.7", alert.ip)

        assertEquals(35000L, today.money.outstanding.totalCents)
        assertTrue(today.briefing.contains("${'$'}350 outstanding"))
    }

    // ---- tickets ------------------------------------------------

    @Test
    fun ticketsListParsesNumericIdsAndNames() = runBlocking {
        val transport = QueueTransport(200 to RealBodies.TICKETS_LIST)
        val page = api(transport).tickets()
        assertEquals(3, page.total)
        val tickets = page.items
        assertEquals(3, tickets.size)
        val first = tickets.first { it.title == "POS terminal drops offline" }
        assertEquals(1, first.id)
        assertEquals(1, first.clientId)
        assertEquals(1, first.caseId)
        assertNull(first.assignedUserId)
        assertEquals("New", first.status)
        assertEquals("Store reports drops every evening.", first.notes)
        assertEquals("Rehearsal Bakery LLC", first.clientName)
        val booking = tickets.first { it.source == "Website" }
        assertEquals("Open", booking.status)
        assertEquals("Rehearsal Bakery LLC", booking.clientName)
    }

    @Test
    fun ticketsStatusFilterUsesTheBackendParameter() = runBlocking {
        val transport = QueueTransport(200 to RealBodies.TICKETS_FILTERED)
        val page = api(transport).tickets(status = "New")
        assertEquals(1, page.total)
        assertEquals(1, page.items.size)
        assertEquals("New", page.items.single().status)
        assertTrue(transport.requests.single().path.contains("status=New"))
    }

    @Test
    fun ticketDetailUnwrapsTheEnvelopeAndBackfillsClientName() = runBlocking {
        val transport = QueueTransport(200 to RealBodies.TICKET_DETAIL)
        val ticket = api(transport).ticket(1)
        assertEquals("POS terminal drops offline", ticket.title)
        assertEquals("Rehearsal Bakery LLC", ticket.clientName)
    }

    @Test
    fun createTicketSendsNotesAndParsesTheWriteEnvelope() = runBlocking {
        val transport = QueueTransport(201 to RealBodies.TICKET_CREATE)
        val created = api(transport).createTicket(
            TicketCreateRequest(
                title = "Fridge sensor offline",
                notes = "Walk-in fridge sensor stopped reporting.",
                priority = "High",
                clientId = 3,
            )
        )
        // The captured response answers the rehearsal driver's own
        // create call; what this test pins is the envelope shape.
        assertEquals(4, created.id)
        assertEquals("Rehearsal-created ticket", created.title)
        assertEquals("Created during the contract rehearsal.", created.notes)
        val sent = Json.parseToJsonElement(transport.requests.single().body!!).jsonObject
        assertEquals("Walk-in fridge sensor stopped reporting.", sent["notes"]!!.jsonPrimitive.content)
        assertFalse(sent.containsKey("message"))
        assertEquals("3", sent["clientId"]!!.jsonPrimitive.content)
    }

    @Test
    fun patchTicketOmitsUnsetFields() = runBlocking {
        val transport = QueueTransport(200 to RealBodies.TICKET_PATCH)
        val patched = api(transport).patchTicket(1, TicketPatchRequest(status = "In Progress"))
        assertEquals("In Progress", patched.status)
        val sent = Json.parseToJsonElement(transport.requests.single().body!!).jsonObject
        assertEquals("In Progress", sent["status"]!!.jsonPrimitive.content)
        assertFalse(sent.containsKey("assignedUserId"))
        assertFalse(sent.containsKey("priority"))
    }

    @Test
    fun patchTicketAssignToMeResolvesTheSignedInUser() = runBlocking {
        val transport = QueueTransport(
            200 to RealBodies.LOGIN_SUCCESS,
            200 to RealBodies.TICKET_PATCH,
        )
        val api = api(transport)
        api.login("Reh Owner", "pass", "Rehearsal phone")
        api.patchTicket(1, TicketPatchRequest(assignToMe = true))
        val sent = Json.parseToJsonElement(transport.requests[1].body!!).jsonObject
        assertEquals("1", sent["assignedUserId"]!!.jsonPrimitive.content)
    }

    @Test
    fun addTicketNoteSendsTheNoteFieldTheBackendReads() = runBlocking {
        val transport = QueueTransport(200 to RealBodies.TICKET_NOTE)
        val updated = api(transport).addTicketNote(1, "Called the store; switch replacement ordered.")
        // The captured response carries the rehearsal driver's note,
        // appended into the ticket's single notes block under the
        // backend's own stamp line — the shape the detail screen
        // renders as one text block.
        assertTrue(updated.notes.contains("--- App note by Reh Owner"))
        assertTrue(updated.notes.contains("Rehearsal note from the contract pass."))
        val sent = Json.parseToJsonElement(transport.requests.single().body!!).jsonObject
        assertTrue(sent.containsKey("note"))
        assertFalse(sent.containsKey("body"))
    }

    // ---- clients / pipeline -------------------------------------

    @Test
    fun clientsListParsesNullablesAndTierTokens() = runBlocking {
        val transport = QueueTransport(200 to RealBodies.CLIENTS_LIST)
        val page = api(transport).clients()
        assertEquals(3, page.total)
        val clients = page.items
        assertEquals(3, clients.size)
        val bakery = clients.first { it.legalName == "Rehearsal Bakery LLC" }
        assertEquals(1, bakery.id)
        assertEquals("tier-a", bakery.tier)
        assertEquals("tier-a, vip", bakery.tags)
        assertEquals("2026-11-02", bakery.nextFollowUp)
        val northwind = clients.first { it.legalName == "Northwind Traders Inc" }
        assertNull(northwind.value)
        assertNull(northwind.nextFollowUp)
        assertNull(northwind.tags)
        assertEquals("", northwind.tier)
    }

    @Test
    fun clientDetailUnwrapsTheEnvelope() = runBlocking {
        val transport = QueueTransport(200 to RealBodies.CLIENT_DETAIL)
        val client = api(transport).client(1)
        assertEquals("Rehearsal Bakery LLC", client.legalName)
        assertEquals("tier-a", client.tier)
        assertFalse(client.familyLinked)
    }

    @Test
    fun pipelineParsesStageObjects() = runBlocking {
        val transport = QueueTransport(200 to RealBodies.PIPELINE)
        val board = api(transport).pipeline()
        assertEquals(3, board.totalClients)
        assertEquals("Lead", board.stages.first().stage)
        assertEquals(1, board.stages.first().count)
        assertEquals("Northwind Traders Inc", board.stages.first().clients.single().legalName)
        val card = board.stages.flatMap { it.clients }
            .first { it.legalName == "Rehearsal Bakery LLC" }
        assertEquals(2500.0, card.value!!, 0.001)
        assertEquals("tier-a", card.tier)
    }

    // ---- schedule / jobs ----------------------------------------

    @Test
    fun scheduleParsesJobsAndAssignees() = runBlocking {
        val transport = QueueTransport(200 to RealBodies.SCHEDULE)
        val jobs = api(transport).schedule()
        assertEquals(2, jobs.size)
        assertFalse(transport.requests.single().path.contains("?"))
        val install = jobs.first { it.title.startsWith("Camera install") }
        assertEquals("2026-11-02", install.workDate)
        assertEquals("09:00", install.startTime)
        assertEquals("Rehearsal Bakery LLC", install.clientName)
        assertEquals(
            listOf("Reh Owner", "Reh Staff"),
            install.assignees.map { it.displayName },
        )
        val review = jobs.first { it.allDay }
        assertNull(review.clientName)
        assertTrue(review.assignees.isEmpty())
    }

    @Test
    fun jobDetailUnwrapsTheEnvelope() = runBlocking {
        val transport = QueueTransport(200 to RealBodies.JOB_DETAIL)
        val job = api(transport).job(1)
        assertEquals("Camera install — front counter", job.title)
        assertEquals("12 Main St, Kansas City, MO", job.location)
    }

    // ---- devices / version / logout -----------------------------

    @Test
    fun devicesParseNumericIdsAndStamps() = runBlocking {
        val transport = QueueTransport(200 to RealBodies.DEVICES)
        val devices = api(transport).devices()
        assertEquals(2, devices.size)
        val current = devices.first { it.current }
        assertEquals("Rehearsal phone", current.deviceName)
        assertTrue(current.createdAt > 0)
        assertFalse(current.revoked)
    }

    @Test
    fun revokeDeviceIssuesTheDelete() = runBlocking {
        val transport = QueueTransport(200 to RealBodies.DEVICE_REVOKE)
        api(transport).revokeDevice(3)
        val request = transport.requests.single()
        assertEquals("DELETE", request.method)
        assertTrue(request.path.endsWith("/api/app/devices/3"))
    }

    @Test
    fun versionUnavailableParsesNullVersion() = runBlocking {
        val transport = QueueTransport(200 to RealBodies.VERSION_UNAVAILABLE)
        val version = api(transport).version()
        assertFalse(version.available)
        assertNull(version.version)
        assertNotNull(version.message)
    }

    @Test
    fun versionAvailableParsesTheStagedBuild() = runBlocking {
        val transport = QueueTransport(200 to RealBodies.VERSION_AVAILABLE)
        val version = api(transport).version()
        assertTrue(version.available)
        assertEquals("9.9.9-rehearsal", version.version)
        assertEquals(99, version.versionCode)
        assertEquals("/api/app/update.apk", version.downloadPath)
    }

    @Test
    fun logoutAcceptsTheRealOkEnvelope() = runBlocking {
        val transport = QueueTransport(200 to RealBodies.LOGOUT)
        api(transport).logout() // must not throw on the real body
    }

    @Test
    fun unauthorizedBodyIsTheRealErrorShape() {
        val parsed = Json { ignoreUnknownKeys = true }
            .decodeFromString<OkResponse>(RealBodies.ME_UNAUTHORIZED)
        assertFalse(parsed.ok)
    }
}

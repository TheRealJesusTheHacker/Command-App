package net.sentinelsafeguard.app

import kotlinx.serialization.json.Json
import net.sentinelsafeguard.app.data.api.ClientListResponse
import net.sentinelsafeguard.app.data.api.PipelineResponse
import net.sentinelsafeguard.app.data.api.ScheduleResponse
import net.sentinelsafeguard.app.data.api.TicketListResponse
import net.sentinelsafeguard.app.data.api.TodayResponse
import net.sentinelsafeguard.app.data.api.VersionResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract-shape tests for the spec §3.3 payloads: the exact JSON the
 * backend is being built to send must parse into the client models,
 * and unknown extra fields must never break parsing.
 */
class ContractShapeTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun todayPayloadParses() {
        val payload = """
            {
              "followUpsDue": 3, "newTickets": 2, "overdueCases": 1,
              "tierAUntouched": 4, "signInAlerts": 1,
              "followUps": [{"id":"c1","title":"Call the bakery","subtitle":"Due today"}],
              "tickets": [{"id":"t1","title":"Printer down"}],
              "cases": [], "tierA": [], "alerts": [{"id":"a1","title":"New sign-in"}],
              "futureField": {"anything": true}
            }
        """.trimIndent()
        val today = json.decodeFromString<TodayResponse>(payload)
        assertEquals(3, today.followUpsDue)
        assertEquals(2, today.newTickets)
        assertEquals("Call the bakery", today.followUps.single().title)
        assertEquals(1, today.alerts.size)
    }

    @Test
    fun ticketPayloadParsesWithNotesAndSource() {
        val payload = """
            {
              "tickets": [{
                "id": "t9", "title": "Website is slow", "message": "It takes forever.",
                "reporter": "Sam", "status": "new", "priority": "high",
                "assignee": null, "source": "website", "clientId": "c2",
                "clientName": "Bakery", "caseId": null,
                "createdAt": "2026-10-25 09:14",
                "notes": [{"id":"n1","author":"Skyler","body":"Looking into it.","createdAt":"2026-10-25 09:20"}]
              }]
            }
        """.trimIndent()
        val ticket = json.decodeFromString<TicketListResponse>(payload).tickets.single()
        assertEquals("website", ticket.source)
        assertEquals("high", ticket.priority)
        assertEquals("Looking into it.", ticket.notes.single().body)
    }

    @Test
    fun clientAndPipelinePayloadsParse() {
        val clients = json.decodeFromString<ClientListResponse>(
            """{"clients":[{"id":"c1","name":"Sam","business":"Bakery","phone":"555-0100",
               "email":"sam@example.com","tier":"A","tags":["vip"],"stage":"Active",
               "followUpDate":"2026-10-27","notes":null}]}"""
        ).clients
        assertEquals("A", clients.single().tier)
        assertTrue(clients.single().tags.contains("vip"))

        val board = json.decodeFromString<PipelineResponse>(
            """{"stages":[{"name":"New","count":2,"clients":[{"id":"c1","name":"Sam"}]},
                          {"name":"Won","count":0,"clients":[]}]}"""
        )
        assertEquals(2, board.stages.size)
        assertEquals("Sam", board.stages.first().clients.single().name)
    }

    @Test
    fun schedulePayloadParses() {
        val jobs = json.decodeFromString<ScheduleResponse>(
            """{"jobs":[{"id":"j1","title":"Camera install","clientName":"Bakery",
               "address":"12 Main St","start":"2026-10-26T15:00","end":"2026-10-26T17:00",
               "date":"2026-10-26","assignees":["Skyler"],"notes":"Bring the ladder"}]}"""
        ).jobs
        assertEquals("Camera install", jobs.single().title)
        assertEquals(listOf("Skyler"), jobs.single().assignees)
    }

    @Test
    fun versionPayloadParses() {
        val version = json.decodeFromString<VersionResponse>(
            """{"latestVersion":"0.2.0","latestVersionCode":2,"notes":"Fixes the mail list."}"""
        )
        assertEquals("0.2.0", version.latestVersion)
        assertEquals("Fixes the mail list.", version.notes)
    }
}

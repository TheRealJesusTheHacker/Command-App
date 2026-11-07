package net.sentinelsafeguard.app

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.notify.NotifyCategory
import net.sentinelsafeguard.app.notify.PushLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PushLogicTest {

    @Test
    fun criticalAlertUsesSignInAlertCategory() {
        assertEquals(
            NotifyCategory.SIGN_IN_ALERT,
            PushLogic.categoryFor("critical_alert"),
        )
    }

    @Test
    fun ticketKindsUseTicketsCategory() {
        assertEquals(NotifyCategory.TICKETS, PushLogic.categoryFor("ticket_assigned"))
        assertEquals(NotifyCategory.TICKETS, PushLogic.categoryFor("ticket_replied"))
        assertEquals(NotifyCategory.TICKETS, PushLogic.categoryFor("ticket_created"))
    }

    @Test
    fun unknownKindsFallBackToTickets() {
        assertEquals(NotifyCategory.TICKETS, PushLogic.categoryFor("something_new"))
        assertEquals(NotifyCategory.TICKETS, PushLogic.categoryFor(""))
    }

    private fun sampleResponse() = buildJsonObject {
        put("events", JsonArray(listOf(
            buildJsonObject {
                put("id", 11); put("kind", "ticket_assigned")
                put("title", "Ticket assigned"); put("body", "Ticket 5 is yours.")
                put("refType", "ticket"); put("refId", 5)
            },
            buildJsonObject {
                put("id", 12); put("kind", "critical_alert")
                put("title", ""); put("body", "")
            },
        )))
    }

    @Test
    fun parseEventsReadsIdsKindsAndRefs() {
        val events = PushLogic.parseEvents(sampleResponse())
        assertEquals(2, events.size)
        assertEquals("11", events[0].id)
        assertEquals("ticket_assigned", events[0].kind)
        assertEquals("Ticket 5 is yours.", events[0].body)
        assertEquals("ticket", events[0].refType)
        assertEquals("5", events[0].refId)
        assertEquals("critical_alert", events[1].kind)
    }

    @Test
    fun parseEventsOfEmptyResponseIsEmpty() {
        assertTrue(PushLogic.parseEvents(buildJsonObject { put("ok", true) }).isEmpty())
    }

    @Test
    fun cursorAdvancesToHighestEventId() {
        val events = PushLogic.parseEvents(sampleResponse())
        assertEquals(12L, PushLogic.nextCursor(0L, sampleResponse(), events))
    }

    @Test
    fun cursorNeverMovesBackwards() {
        val events = PushLogic.parseEvents(sampleResponse())
        assertEquals(99L, PushLogic.nextCursor(99L, sampleResponse(), events))
        assertEquals(99L, PushLogic.nextCursor(99L, buildJsonObject { }, emptyList()))
    }

    @Test
    fun serverStatedCursorWins() {
        val response = buildJsonObject { put("cursor", 500) }
        assertEquals(500L, PushLogic.nextCursor(12L, response, emptyList()))
    }

    @Test
    fun fallbackTextIsPlainAndKindAware() {
        assertEquals("Critical alert", PushLogic.fallbackTitle("critical_alert"))
        assertEquals("Ticket update", PushLogic.fallbackTitle("ticket_replied"))
        assertTrue(PushLogic.fallbackBody("critical_alert").contains("critical alert"))
    }
}

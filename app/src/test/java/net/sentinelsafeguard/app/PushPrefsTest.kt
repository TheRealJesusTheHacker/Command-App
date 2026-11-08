package net.sentinelsafeguard.app

import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import net.sentinelsafeguard.app.notify.PushPrefsLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 1.5.2 (Elevation F4) pins for the server-side push event
 * preferences mirror. Fixtures are the contract's own shapes
 * (LANE-FEAT-F3F4.md relay block): GET/POST /api/app/push/prefs
 * carry {ok, prefs:{ticket_assigned, ticket_replied,
 * critical_alert}}; writes are partial {"prefs": {…}}.
 *
 * Honesty rails pinned here:
 *  - a response missing a kind (or carrying a non-boolean) is
 *    UNPARSEABLE — the surface must fail visibly, never render
 *    invented defaults;
 *  - a write body carries exactly the one changed kind.
 */
class PushPrefsTest {

    private fun response(
        assigned: Boolean = true,
        replied: Boolean = true,
        critical: Boolean = true,
    ) = buildJsonObject {
        put("ok", true)
        put("prefs", buildJsonObject {
            put("ticket_assigned", assigned)
            put("ticket_replied", replied)
            put("critical_alert", critical)
        })
    }

    @Test
    fun parseReadsAllThreeServerValues() {
        val prefs = PushPrefsLogic.parse(response(assigned = true, replied = false))!!
        assertTrue(prefs.ticketAssigned)
        assertFalse(prefs.ticketReplied)
        assertTrue(prefs.criticalAlert)
        assertEquals(false, prefs.value(PushPrefsLogic.TICKET_REPLIED))
        assertEquals(true, prefs.value(PushPrefsLogic.CRITICAL_ALERT))
    }

    @Test
    fun keysAndLabelsAreTheContractThree() {
        assertEquals(
            listOf("ticket_assigned", "ticket_replied", "critical_alert"),
            PushPrefsLogic.KEYS,
        )
        assertEquals("A ticket is assigned to me",
            PushPrefsLogic.label(PushPrefsLogic.TICKET_ASSIGNED))
        assertEquals("A client replies to a ticket",
            PushPrefsLogic.label(PushPrefsLogic.TICKET_REPLIED))
        assertEquals("Critical alerts",
            PushPrefsLogic.label(PushPrefsLogic.CRITICAL_ALERT))
    }

    @Test
    fun parseRejectsMissingPrefsObject() {
        assertNull(PushPrefsLogic.parse(buildJsonObject { put("ok", true) }))
    }

    @Test
    fun parseRejectsAMissingKind() {
        val body = buildJsonObject {
            put("ok", true)
            put("prefs", buildJsonObject {
                put("ticket_assigned", true)
                put("critical_alert", true)
            })
        }
        assertNull(PushPrefsLogic.parse(body))
    }

    @Test
    fun parseRejectsANonBooleanKind() {
        val body = buildJsonObject {
            put("ok", true)
            put("prefs", buildJsonObject {
                put("ticket_assigned", true)
                put("ticket_replied", "yes")
                put("critical_alert", true)
            })
        }
        assertNull(PushPrefsLogic.parse(body))
    }

    @Test
    fun writeBodyIsExactlyOnePartialKind() {
        val body = PushPrefsLogic.writeBody(PushPrefsLogic.TICKET_REPLIED, false)
        assertEquals(setOf("prefs"), body.keys)
        val prefs = body["prefs"]!!.jsonObject
        assertEquals(setOf("ticket_replied"), prefs.keys)
        assertFalse(prefs["ticket_replied"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun withUpdatesOnlyTheNamedKind() {
        val prefs = PushPrefsLogic.parse(response())!!
            .with(PushPrefsLogic.CRITICAL_ALERT, false)
        assertTrue(prefs.ticketAssigned)
        assertTrue(prefs.ticketReplied)
        assertFalse(prefs.criticalAlert)
    }
}

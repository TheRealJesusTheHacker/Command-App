package net.sentinelsafeguard.app

import java.io.IOException
import net.sentinelsafeguard.app.data.OfflineLogic
import net.sentinelsafeguard.app.data.api.ApiException
import net.sentinelsafeguard.app.data.api.Client
import net.sentinelsafeguard.app.data.api.HttpRequest
import net.sentinelsafeguard.app.ui.MirrorNav
import net.sentinelsafeguard.app.ui.MirrorVocab
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AUDIT-171 guard tests: the deep-link role gate (B-26), the
 * offline-queue admission rule (B-08), and the request-log
 * redaction (L-08).
 */
class Audit171GuardTest {

    // ---- B-26: deep links cross the drawer's gate ----

    private val noFlags = emptyMap<String, Boolean>()

    @Test
    fun ownerFollowsTicketDeepLink() {
        assertTrue(MirrorNav.deepLinkAllowed("ticket/12", "OWNER", noFlags, false))
        assertTrue(MirrorNav.deepLinkAllowed("tickets", "OWNER", noFlags, false))
        assertTrue(MirrorNav.deepLinkAllowed("today", "OWNER", noFlags, false))
        assertTrue(MirrorNav.deepLinkAllowed("invoice/3", "OWNER", noFlags, false))
        assertTrue(MirrorNav.deepLinkAllowed("client/7", "OWNER", noFlags, false))
        assertTrue(MirrorNav.deepLinkAllowed("activity", "OWNER", noFlags, false))
    }

    @Test
    fun taxOnlyAccountIsConfinedToTax() {
        // A taxOnly account sees only Tax & Profit: a ticket or
        // Today deep link must NOT navigate it anywhere.
        assertFalse(MirrorNav.deepLinkAllowed("ticket/12", "STAFF", noFlags, true))
        assertFalse(MirrorNav.deepLinkAllowed("today", "STAFF", noFlags, true))
        assertFalse(MirrorNav.deepLinkAllowed("invoice/3", "STAFF", noFlags, true))
        assertTrue(MirrorNav.deepLinkAllowed("b4etax", "STAFF", noFlags, true))
    }

    @Test
    fun unknownRouteIsDropped() {
        assertFalse(MirrorNav.deepLinkAllowed("nope/1", "OWNER", noFlags, false))
        assertFalse(MirrorNav.deepLinkAllowed("", "OWNER", noFlags, false))
    }

    // ---- B-08: only unanswered writes queue ----

    @Test
    fun transportFailuresQueue() {
        assertTrue(OfflineLogic.shouldQueue(IOException("no route to host")))
        assertTrue(OfflineLogic.shouldQueue(RuntimeException("timeout")))
    }

    @Test
    fun serverRefusalsNeverQueue() {
        assertFalse(OfflineLogic.shouldQueue(ApiException(400, "Title is required.")))
        assertFalse(OfflineLogic.shouldQueue(ApiException(403, "Denied.")))
        assertFalse(OfflineLogic.shouldQueue(ApiException(500, "Server error.")))
    }

    // ---- B-07: pickers resolve by position, never by name ----

    @Test
    fun namesakeClientsGetUniqueOptionsAndResolveByPosition() {
        val clients = listOf(
            Client(id = 1, legalName = "Acme LLC"),
            Client(id = 2, legalName = "Acme LLC"),
            Client(id = 3, legalName = "Beta Inc"),
        )
        val options = MirrorVocab.clientOptions(clients)
        assertTrue(options.toSet().size == options.size)
        // The second Acme is pickable and resolves to id 2 —
        // a name match would have returned id 1.
        assertTrue(MirrorVocab.clientIdAt(clients, options, options[1]) == 2)
        assertTrue(MirrorVocab.clientIdAt(clients, options, options[0]) == 1)
        assertTrue(MirrorVocab.clientIdAt(clients, options, options[2]) == 3)
        // With a leading sentinel ("No client"), positions shift.
        val withSentinel = listOf("No client") + options
        assertTrue(
            MirrorVocab.clientIdAt(clients, withSentinel, options[1], sentinelCount = 1) == 2
        )
        // Unique names keep their plain label.
        assertTrue(options[2] == "Beta Inc")
    }

    // ---- L-08: a request's toString never carries secrets ----

    @Test
    fun httpRequestToStringRedactsHeadersAndBody() {
        val req = HttpRequest(
            method = "POST",
            path = "/api/app/tickets",
            body = "{\"title\":\"secret title\"}",
            headers = mapOf("Authorization" to "Bearer device-token-xyz"),
        )
        val text = req.toString()
        assertFalse(text.contains("device-token-xyz"))
        assertFalse(text.contains("secret title"))
        assertTrue(text.contains("/api/app/tickets"))
        assertTrue(text.contains("Authorization"))
    }
}

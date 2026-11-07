package net.sentinelsafeguard.app

import net.sentinelsafeguard.app.notify.NotifyCategory
import net.sentinelsafeguard.app.notify.NotifyRouting
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Expansion A1: a notification kind + record id maps to the
 * category and the exact in-app route its tap opens (AppRoot
 * nav-graph shapes). Kinds with no detail route land on their
 * section — pinned here so the mapping never silently drifts.
 */
class NotifyRoutingTest {

    @Test
    fun ticketKindsDeepLinkToTheTicket() {
        for (kind in listOf("ticket_created", "ticket_assigned", "ticket_replied", "new_ticket")) {
            val target = NotifyRouting.targetFor(kind, "42")
            assertEquals(kind, NotifyCategory.TICKETS, target.category)
            assertEquals(kind, "ticket/42", target.route)
        }
    }

    @Test
    fun ticketKindWithoutUsableIdLandsOnTheSection() {
        assertEquals("tickets", NotifyRouting.targetFor("ticket_assigned", null).route)
        assertEquals("tickets", NotifyRouting.targetFor("ticket_assigned", "abc").route)
        assertEquals("tickets", NotifyRouting.targetFor("ticket_assigned", "").route)
    }

    @Test
    fun criticalAlertLandsOnTodayUrgent() {
        val target = NotifyRouting.targetFor("critical_alert", null)
        assertEquals(NotifyCategory.CRITICAL, target.category)
        assertEquals("today", target.route)
    }

    @Test
    fun signInAlertLandsOnActivity() {
        val target = NotifyRouting.targetFor("sign_in_alert", null)
        assertEquals(NotifyCategory.SIGN_IN_ALERT, target.category)
        assertEquals("activity", target.route)
    }

    @Test
    fun estateDownIsItsOwnCategoryOnToday() {
        val target = NotifyRouting.targetFor("estate_down", null)
        assertEquals(NotifyCategory.ESTATE_DOWN, target.category)
        assertEquals("today", target.route)
    }

    @Test
    fun moneyKindsDeepLinkToTheInvoice() {
        val target = NotifyRouting.targetFor("invoice_overdue", "7")
        assertEquals(NotifyCategory.MONEY, target.category)
        assertEquals("invoice/7", target.route)
        assertEquals("invoices", NotifyRouting.targetFor("payment_received", null).route)
    }

    @Test
    fun mailHasNoDetailRouteSoItLandsOnTheSection() {
        val target = NotifyRouting.targetFor("new_mail", "99")
        assertEquals(NotifyCategory.MAIL, target.category)
        assertEquals("mail", target.route)
    }

    @Test
    fun digestKindsLandOnToday() {
        val target = NotifyRouting.targetFor("platform_digest", null)
        assertEquals(NotifyCategory.DIGEST, target.category)
        assertEquals("today", target.route)
    }

    @Test
    fun leadKindsDeepLinkToTheClient() {
        assertEquals("client/3", NotifyRouting.targetFor("new_lead", "3").route)
        assertEquals("clients", NotifyRouting.targetFor("new_lead", null).route)
    }

    @Test
    fun unknownKindFallsBackToTodayNeverHome() {
        val target = NotifyRouting.targetFor("some_future_kind", "1")
        assertEquals("today", target.route)
    }

    @Test
    fun kindsAreCaseAndWhitespaceTolerant() {
        assertEquals("ticket/5", NotifyRouting.targetFor("  Ticket_Assigned ", "5").route)
    }
}

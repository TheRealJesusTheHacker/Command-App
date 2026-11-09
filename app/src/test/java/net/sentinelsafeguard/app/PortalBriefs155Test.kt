package net.sentinelsafeguard.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import net.sentinelsafeguard.app.ui.Phase8bLogic
import net.sentinelsafeguard.app.ui.PortalBriefs155Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 1.5.5 pins for the portal's delivered sections + the
 * auto-delivered threat-brief label.
 *
 * The four fixtures below are BYTE-VERBATIM captures from the
 * 2026-11-09 rehearsal against the staging Command server
 * (portal-briefs-rehearsal/goldens/ — a throwaway DB seeded
 * with a linked client holding delivered rows, a linked client
 * holding none, an unlinked portal account, and staff briefs
 * in all three states; the app's own endpoints called exactly
 * as the client calls them; production never contacted).
 *
 * Honesty rails pinned here:
 *  - the portal sections render ONLY the whitelist fields the
 *    payload carries (id, period, title, body, deliveredAt) —
 *    no staff field, state, or source ref exists to leak;
 *  - drafts never ride the portal payload (server-side, and
 *    the populated golden proves both delivered briefs list
 *    while the seeded draft does not);
 *  - an absent section key degrades to the same empty state
 *    as an empty array — the web portal's `|| []` behavior;
 *  - a staff brief that is state "sent" with sentBy carried
 *    as an explicit null is labeled "Delivered automatically";
 *    a real sentBy, a draft, or a payload with no sentBy key
 *    at all gets NO note — no sender is fabricated either way.
 */
class PortalBriefs155Test {

    private fun obj(body: String) = Json.parseToJsonElement(body).jsonObject

    private val PORTAL_POPULATED = """
{"linked": true, "portalLang": "en", "contactKind": "primary", "portalAccess": {"work": true, "money": true}, "portalMessageUnread": 0, "notificationPrefs": {"ticketUpdates": true, "invoiceEvents": true, "monitoringAlerts": true, "digest": false}, "canEditPrefs": true, "securityScore": {"state": "insufficient", "score": null, "baseline": 40, "factors": [{"label": "Not enough information yet \u2014 no assets, tickets, or cases on file for this client.", "points": 0}], "counts": {"assets": 0, "openTickets": 0, "openCases": 0, "closedItems": 0, "totalItems": 0, "overdueInvoices": 0, "forms": 0}}, "profile": {"legalName": "Rehearsal Bakery LLC", "dba": "", "contactName": "Sam Reyes", "contactTitle": "", "email": "sam@rehbakery.test", "phone": "", "street": "", "city": "", "state": "", "zip": ""}, "cases": [], "tickets": [], "invoices": [], "documents": [], "appointments": [], "assets": [], "progress": [], "quotes": [], "paymentPlans": [], "cfRoadmap": {"items": [], "total": 0, "doneCount": 0, "percent": 0}, "cfBreachMatches": [], "cfBreachAlerts": [], "cfComplianceSummary": [], "library": [], "siteProfiles": [], "cf2Hygiene": {"items": [{"key": "mfa_enabled", "label": "Multi-factor authentication enabled", "state": "unknown", "checkedAt": null}, {"key": "password_manager", "label": "Password manager in use", "state": "unknown", "checkedAt": null}, {"key": "unique_passwords_attested", "label": "Unique passwords attested (no reuse)", "state": "unknown", "checkedAt": null}, {"key": "backup_codes_stored", "label": "Backup / recovery codes stored safely", "state": "unknown", "checkedAt": null}, {"key": "recovery_email_current", "label": "Recovery email address current", "state": "unknown", "checkedAt": null}, {"key": "recovery_phone_current", "label": "Recovery phone number current", "state": "unknown", "checkedAt": null}, {"key": "admin_accounts_reviewed", "label": "Admin accounts reviewed", "state": "unknown", "checkedAt": null}, {"key": "shared_passwords_rotated", "label": "Shared passwords rotated when staff changes", "state": "unknown", "checkedAt": null}], "counts": {"yes": 0, "no": 0, "unknown": 8}, "total": 8}, "threatBriefs": [{"id": 2, "period": "2026-10", "title": "Threat brief \u2014 October 2026", "body": "October brief body, delivered by the owner in person.", "deliveredAt": 1793728769151}, {"id": 1, "period": "2026-11", "title": "Threat brief \u2014 November 2026", "body": "Threat brief for Rehearsal Bakery LLC.\n\nTwo phishing lures used your business name this month; both were reported and neither reached your inbox rules.\n\nRecommended: keep the front-counter mailbox filter on, and call Sentinel before paying any changed bank details.", "deliveredAt": 1794333569151}], "monthlyReports": [{"id": 1, "period": "2026-10", "body": "Monthly report \u2014 October 2026\n\nTickets closed: 4. Backups verified: 2. Patch coverage: 98%.\nNo incidents required escalation this month.", "deliveredAt": 1794333569151}], "ok": true}
    """

    private val PORTAL_LINKED_EMPTY = """
{"linked": true, "portalLang": "en", "contactKind": "primary", "portalAccess": {"work": true, "money": true}, "portalMessageUnread": 0, "notificationPrefs": {"ticketUpdates": true, "invoiceEvents": true, "monitoringAlerts": true, "digest": false}, "canEditPrefs": true, "securityScore": {"state": "insufficient", "score": null, "baseline": 40, "factors": [{"label": "Not enough information yet \u2014 no assets, tickets, or cases on file for this client.", "points": 0}], "counts": {"assets": 0, "openTickets": 0, "openCases": 0, "closedItems": 0, "totalItems": 0, "overdueInvoices": 0, "forms": 0}}, "profile": {"legalName": "Empty Shelves LLC", "dba": "", "contactName": "Jo Park", "contactTitle": "", "email": "jo@emptyshelves.test", "phone": "", "street": "", "city": "", "state": "", "zip": ""}, "cases": [], "tickets": [], "invoices": [], "documents": [], "appointments": [], "assets": [], "progress": [], "quotes": [], "paymentPlans": [], "cfRoadmap": {"items": [], "total": 0, "doneCount": 0, "percent": 0}, "cfBreachMatches": [], "cfBreachAlerts": [], "cfComplianceSummary": [], "library": [], "siteProfiles": [], "cf2Hygiene": {"items": [{"key": "mfa_enabled", "label": "Multi-factor authentication enabled", "state": "unknown", "checkedAt": null}, {"key": "password_manager", "label": "Password manager in use", "state": "unknown", "checkedAt": null}, {"key": "unique_passwords_attested", "label": "Unique passwords attested (no reuse)", "state": "unknown", "checkedAt": null}, {"key": "backup_codes_stored", "label": "Backup / recovery codes stored safely", "state": "unknown", "checkedAt": null}, {"key": "recovery_email_current", "label": "Recovery email address current", "state": "unknown", "checkedAt": null}, {"key": "recovery_phone_current", "label": "Recovery phone number current", "state": "unknown", "checkedAt": null}, {"key": "admin_accounts_reviewed", "label": "Admin accounts reviewed", "state": "unknown", "checkedAt": null}, {"key": "shared_passwords_rotated", "label": "Shared passwords rotated when staff changes", "state": "unknown", "checkedAt": null}], "counts": {"yes": 0, "no": 0, "unknown": 8}, "total": 8}, "threatBriefs": [], "monthlyReports": [], "ok": true}
    """

    private val PORTAL_UNLINKED = """
{"linked": false, "portalLang": "en", "profile": null, "contactKind": "primary", "portalAccess": {"work": false, "money": false}, "portalMessageUnread": 0, "notificationPrefs": {"ticketUpdates": true, "invoiceEvents": true, "monitoringAlerts": true, "digest": false}, "canEditPrefs": false, "securityScore": {"state": "insufficient", "score": null, "baseline": 40, "factors": [{"label": "Not enough information yet \u2014 no assets, tickets, or cases on file for this client.", "points": 0}], "counts": {"assets": 0, "openTickets": 0, "openCases": 0, "closedItems": 0, "totalItems": 0, "overdueInvoices": 0, "forms": 0}}, "cases": [], "tickets": [], "invoices": [], "documents": [], "appointments": [], "assets": [], "progress": [], "quotes": [], "paymentPlans": [], "cfRoadmap": {"items": [], "total": 0, "doneCount": 0, "percent": 0}, "monthlyReports": [], "cfBreachMatches": [], "cfBreachAlerts": [], "cfComplianceSummary": [], "library": [], "siteProfiles": [], "threatBriefs": [], "ok": true}
    """

    private val STAFF_BRIEFS = """
{"ok": true, "briefs": [{"id": 3, "clientId": 1, "clientName": "Rehearsal Bakery LLC", "period": "2026-12", "title": "DRAFT \u2014 December working notes", "body": "Unreviewed draft body.", "sourceRefs": {}, "state": "draft", "createdBy": 1, "createdAt": 1793728769151, "sentAt": null, "sentBy": null}, {"id": 2, "clientId": 1, "clientName": "Rehearsal Bakery LLC", "period": "2026-10", "title": "Threat brief \u2014 October 2026", "body": "October brief body, delivered by the owner in person.", "sourceRefs": {}, "state": "sent", "createdBy": 1, "createdAt": 1793728769151, "sentAt": 1793728769151, "sentBy": 1}, {"id": 1, "clientId": 1, "clientName": "Rehearsal Bakery LLC", "period": "2026-11", "title": "Threat brief \u2014 November 2026", "body": "Threat brief for Rehearsal Bakery LLC.\n\nTwo phishing lures used your business name this month; both were reported and neither reached your inbox rules.\n\nRecommended: keep the front-counter mailbox filter on, and call Sentinel before paying any changed bank details.", "sourceRefs": {}, "state": "sent", "createdBy": 1, "createdAt": 1793728769151, "sentAt": 1794333569151, "sentBy": null}]}
    """

    // ---- A. Portal sections ----

    @Test
    fun populatedGoldenBriefsParseVerbatim() {
        val rows = PortalBriefs155Logic.briefs(obj(PORTAL_POPULATED))
        assertEquals(2, rows.size)
        // Payload order is the server's (id DESC) and is kept.
        val october = rows[0]
        val november = rows[1]
        assertEquals("Threat brief — October 2026", PortalBriefs155Logic.briefTitle(october))
        assertEquals("Threat brief — November 2026", PortalBriefs155Logic.briefTitle(november))
        assertTrue(PortalBriefs155Logic.body(november).contains("Two phishing lures"))
        // The seeded draft never rides the portal payload.
        assertTrue(rows.none { PortalBriefs155Logic.briefTitle(it).contains("DRAFT") })
        // A titled row's meta carries the period + delivered date.
        val meta = PortalBriefs155Logic.briefMeta(november)
        assertTrue(meta!!.startsWith("2026-11 · Delivered "))
    }

    @Test
    fun populatedGoldenReportsParseVerbatim() {
        val rows = PortalBriefs155Logic.reports(obj(PORTAL_POPULATED))
        assertEquals(1, rows.size)
        assertEquals("Monthly report — 2026-10", PortalBriefs155Logic.reportTitle(rows[0]))
        assertTrue(PortalBriefs155Logic.body(rows[0]).contains("Tickets closed: 4"))
        assertTrue(PortalBriefs155Logic.reportMeta(rows[0])!!.startsWith("Delivered "))
        // The payload carries no download/document field: the
        // row's key set is exactly the whitelist, nothing more
        // exists for a control to offer.
        assertEquals(
            setOf("id", "period", "body", "deliveredAt"),
            rows[0].keys,
        )
        val brief = PortalBriefs155Logic.briefs(obj(PORTAL_POPULATED))[0]
        assertEquals(
            setOf("id", "period", "title", "body", "deliveredAt"),
            brief.keys,
        )
    }

    @Test
    fun linkedEmptyGoldenYieldsEmptySections() {
        val home = obj(PORTAL_LINKED_EMPTY)
        assertTrue(PortalBriefs155Logic.sectionPresent(home, "threatBriefs"))
        assertTrue(PortalBriefs155Logic.sectionPresent(home, "monthlyReports"))
        assertTrue(PortalBriefs155Logic.briefs(home).isEmpty())
        assertTrue(PortalBriefs155Logic.reports(home).isEmpty())
        assertEquals("No threat briefs delivered yet.", PortalBriefs155Logic.BRIEFS_EMPTY)
        assertEquals("No monthly reports delivered yet.", PortalBriefs155Logic.REPORTS_EMPTY)
    }

    @Test
    fun unlinkedGoldenYieldsEmptySections() {
        val home = obj(PORTAL_UNLINKED)
        assertTrue(PortalBriefs155Logic.briefs(home).isEmpty())
        assertTrue(PortalBriefs155Logic.reports(home).isEmpty())
    }

    @Test
    fun absentSectionKeysDegradeToTheSameEmptyState() {
        // A pre-window payload (older server / cached body)
        // without either key: same rendering as empty arrays —
        // the web portal's `|| []` fallback, mirrored.
        val home = kotlinx.serialization.json.JsonObject(
            obj(PORTAL_POPULATED).filterKeys {
                it != "threatBriefs" && it != "monthlyReports"
            },
        )
        assertTrue(!PortalBriefs155Logic.sectionPresent(home, "threatBriefs"))
        assertTrue(!PortalBriefs155Logic.sectionPresent(home, "monthlyReports"))
        assertTrue(PortalBriefs155Logic.briefs(home).isEmpty())
        assertTrue(PortalBriefs155Logic.reports(home).isEmpty())
    }

    @Test
    fun blankBriefTitleFallsBackToThePeriodLabel() {
        val row = obj("""{"id": 7, "period": "2026-09", "title": "", "body": "b", "deliveredAt": 1793728769151}""")
        assertEquals("Threat brief — 2026-09", PortalBriefs155Logic.briefTitle(row))
        // With a period-derived headline the meta is the date alone.
        assertTrue(PortalBriefs155Logic.briefMeta(row)!!.startsWith("Delivered "))
        val bare = obj("""{"id": 8, "period": "", "title": "", "body": "", "deliveredAt": null}""")
        assertEquals("Threat brief", PortalBriefs155Logic.briefTitle(bare))
        assertNull(PortalBriefs155Logic.briefMeta(bare))
        assertEquals("Monthly report", PortalBriefs155Logic.reportTitle(bare))
        assertNull(PortalBriefs155Logic.reportMeta(bare))
    }

    // ---- B. Auto-delivered labeling (staff) ----

    @Test
    fun staffGoldenLabelsOnlyTheAutoDeliveredBrief() {
        val briefs = obj(STAFF_BRIEFS)["briefs"]!!.let {
            (it as kotlinx.serialization.json.JsonArray).map { e -> e.jsonObject }
        }
        assertEquals(3, briefs.size)
        val notes = briefs.map { Phase8bLogic.briefDeliveryNote(it) }
        val auto = briefs.single { it["period"]?.toString()?.contains("2026-11") == true }
        assertEquals("Delivered automatically", Phase8bLogic.briefDeliveryNote(auto))
        // The staff-sent brief and the draft get NO note.
        assertEquals(1, notes.count { it == "Delivered automatically" })
        assertEquals(2, notes.count { it == null })
    }

    @Test
    fun deliveryNoteMatrixIsExact() {
        fun note(body: String) = Phase8bLogic.briefDeliveryNote(obj(body))
        assertEquals(
            "Delivered automatically",
            note("""{"state": "sent", "sentBy": null, "sentAt": 1794333569151}"""),
        )
        assertNull(note("""{"state": "sent", "sentBy": 1, "sentAt": 1793728769151}"""))
        assertNull(note("""{"state": "draft", "sentBy": null, "sentAt": null}"""))
        assertNull(note("""{"state": "discarded", "sentBy": null, "sentAt": null}"""))
        // No sentBy KEY at all (pre-window payload): no note —
        // the app labels only what the payload states.
        assertNull(note("""{"state": "sent", "sentAt": 1793728769151}"""))
        // The legacy status spelling still reads.
        assertEquals(
            "Delivered automatically",
            note("""{"status": "sent", "sentBy": null}"""),
        )
    }
}

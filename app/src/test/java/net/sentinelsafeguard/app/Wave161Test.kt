package net.sentinelsafeguard.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import net.sentinelsafeguard.app.data.api.DashboardResponse
import net.sentinelsafeguard.app.data.api.TicketDetailResponse
import net.sentinelsafeguard.app.ui.Phase3Logic
import net.sentinelsafeguard.app.ui.Wave161Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 1.6.1 pins for the Feature Wave mirror (Items 7, 22, 14,
 * 23, 26, 13).
 *
 * The fixtures below are BYTE-VERBATIM captures from the
 * 2026-11-09 rehearsal against the staging Command server
 * (wave161-rehearsal/goldens/ — a throwaway DB seeded with an
 * owner, staff, a linked portal client, an overnight window
 * with a FAILED backup chain + a failed action, a scored and
 * a zero-data client, staged photos in shuffled order, and
 * prep-carrying + prep-null booking services; the app's own
 * endpoints called exactly as the client calls them;
 * production never contacted; 21/21 rehearsal checks PASS).
 *
 * Honesty rails pinned here:
 *  - the While You Slept card exists ONLY when the payload
 *    carries the field (owner-only server-side); failures
 *    parse in served order and lead; a missing verdict file
 *    is "Backup status unavailable", never invented chains;
 *  - briefing audio: available -> the server bytes decode;
 *    denied / app-shaped answers degrade to Denied (the quiet
 *    line), never an error state; unavailable -> NotGenerated;
 *  - client health: the insufficient state NEVER yields a
 *    score (no rendered 0); band color keys off the band
 *    STRING the server sent, never a recomputed threshold;
 *  - photo timelines group by CONSECUTIVE stageLabel in the
 *    server's served order — the client never re-sorts;
 *  - prep renders only when non-null, in the asked language;
 *  - lead sources: the wave's two new labels are exact, and
 *    unknown future sources humanize instead of vanishing.
 */
class Wave161Test {

    private val json = Json { ignoreUnknownKeys = true }
    private fun obj(body: String) = Json.parseToJsonElement(body).jsonObject

    private val DASH_AUDIO = """
{"today": "2026-11-09", "tickets": {"openTotal": 1, "byPriority": [{"priority": "Low", "count": 0}, {"priority": "Normal", "count": 1}, {"priority": "High", "count": 0}, {"priority": "Critical", "count": 0}], "byStatus": [{"status": "New", "count": 0}, {"status": "Open", "count": 1}, {"status": "In Progress", "count": 0}, {"status": "On Hold", "count": 0}, {"status": "Resolved", "count": 0}, {"status": "Closed", "count": 0}], "unassigned": 1}, "cases": {"total": 0, "byStage": [{"status": "New", "count": 0}, {"status": "Open", "count": 0}, {"status": "In Progress", "count": 0}, {"status": "On Hold", "count": 0}, {"status": "Resolved", "count": 0}, {"status": "Closed", "count": 0}], "overdue": 0, "unassigned": 0}, "clients": {"total": 2, "byTier": {"tier-a": 0, "tier-b": 0, "tier-c": 0, "tier-d": 0, "partner": 0, "untiered": 2}, "byStage": [{"stage": "Lead", "count": 2, "valueCents": 0, "valuedCount": 0}, {"stage": "Contacted", "count": 0, "valueCents": 0, "valuedCount": 0}, {"stage": "Qualified", "count": 0, "valueCents": 0, "valuedCount": 0}, {"stage": "Proposal", "count": 0, "valueCents": 0, "valuedCount": 0}, {"stage": "Negotiation", "count": 0, "valueCents": 0, "valuedCount": 0}, {"stage": "Active Client", "count": 0, "valueCents": 0, "valuedCount": 0}, {"stage": "Past Client", "count": 0, "valueCents": 0, "valuedCount": 0}, {"stage": "Lost", "count": 0, "valueCents": 0, "valuedCount": 0}], "pipelineValueCents": 0}, "invoices": {"count": 1, "outstanding": {"count": 1, "totalCents": 0}, "overdue": {"count": 1, "totalCents": 0}, "draftCount": 0, "paidTotalCents": 0}, "assets": {"total": 0, "byStatus": [{"status": "In Service", "count": 0}, {"status": "In Storage", "count": 0}, {"status": "In Repair", "count": 0}, {"status": "Retired", "count": 0}, {"status": "Lost", "count": 0}], "byScope": {"Sentinel": 0, "Client": 0}}, "presence": {"activeSessionCount": 2, "team": [{"id": 1, "displayName": "OwnerWV", "role": "OWNER", "sessions": 1, "lastExpiresAt": 1794272336491}, {"id": 2, "displayName": "StaffWV", "role": "STAFF", "sessions": 1, "lastExpiresAt": 1794272336727}]}, "attention": [{"kind": "followups", "count": 0}, {"kind": "overdueCases", "count": 0}, {"kind": "unassignedTickets", "count": 1}, {"kind": "signinAlerts", "count": 0}], "recentActivity": [{"id": 8, "actorId": 3, "action": "APP_LOGIN", "entityType": "user", "entityId": 3, "detail": "App sign-in \u2014 device enrolled: wv-test.", "createdAt": 1794243539613}, {"id": 7, "actorId": 2, "action": "APP_LOGIN", "entityType": "user", "entityId": 2, "detail": "App sign-in \u2014 device enrolled: wv-test.", "createdAt": 1794243539508}, {"id": 6, "actorId": 1, "action": "APP_LOGIN", "entityType": "user", "entityId": 1, "detail": "App sign-in \u2014 device enrolled: wv-test.", "createdAt": 1794243539388}, {"id": 5, "actorId": null, "action": "maildigest FAILED to send", "entityType": "system", "entityId": null, "detail": "smtp timeout", "createdAt": 1794243528747}, {"id": 4, "actorId": 2, "action": "LOGIN", "entityType": "session", "entityId": 2, "detail": "Session opened; expires in 8 hours", "createdAt": 1794243536727}, {"id": 3, "actorId": 1, "action": "EMPLOYEE_CREATED", "entityType": "user", "entityId": 2, "detail": "Staff user StaffWV created.", "createdAt": 1794243536605}, {"id": 2, "actorId": 1, "action": "LOGIN", "entityType": "session", "entityId": 1, "detail": "Session opened; expires in 8 hours", "createdAt": 1794243536491}, {"id": 1, "actorId": 1, "action": "OWNER_INITIALIZED", "entityType": "settings", "entityId": 1, "detail": "Owner account initialized; recovery codes issued.", "createdAt": 1794243536382}], "canWrite": {"clients": true, "cases": true, "tickets": true, "invoices": true}, "whileYouSlept": {"windowStartMs": 1794182400000, "windowEndMs": 1794243539741, "windowLabel": "Since 6:00 PM CT yesterday", "failures": [{"kind": "backup", "label": "Backup check FAILED \u2014 weekly", "detail": "Checksum mismatch on segment 12."}, {"kind": "activity", "label": "1 failed action(s) overnight", "detail": "See Recent Activity for the failure rows."}], "hasFailures": true, "newsFiled": 2, "newsPublished": 1, "threatBriefsDelivered": 1, "backups": {"available": true, "checkedAt": 1794243528747, "chains": {"nightly": {"ok": true, "archive": "nightly-2026-11-09.tar.age", "detail": "Restore drill passed."}, "weekly": {"ok": false, "archive": null, "detail": "Checksum mismatch on segment 12."}}, "allOk": false}, "ticketsTouched": 1, "jobsTouched": 1, "audio": {"hash": "d713d47fb43cdb4a", "available": true}}, "migrationHead": 172, "pushProviderEnabled": false, "ok": true}
    """

    private val DASH_NOAUDIO = """
{"today": "2026-11-09", "tickets": {"openTotal": 1, "byPriority": [{"priority": "Low", "count": 0}, {"priority": "Normal", "count": 1}, {"priority": "High", "count": 0}, {"priority": "Critical", "count": 0}], "byStatus": [{"status": "New", "count": 0}, {"status": "Open", "count": 1}, {"status": "In Progress", "count": 0}, {"status": "On Hold", "count": 0}, {"status": "Resolved", "count": 0}, {"status": "Closed", "count": 0}], "unassigned": 1}, "cases": {"total": 0, "byStage": [{"status": "New", "count": 0}, {"status": "Open", "count": 0}, {"status": "In Progress", "count": 0}, {"status": "On Hold", "count": 0}, {"status": "Resolved", "count": 0}, {"status": "Closed", "count": 0}], "overdue": 0, "unassigned": 0}, "clients": {"total": 2, "byTier": {"tier-a": 0, "tier-b": 0, "tier-c": 0, "tier-d": 0, "partner": 0, "untiered": 2}, "byStage": [{"stage": "Lead", "count": 2, "valueCents": 0, "valuedCount": 0}, {"stage": "Contacted", "count": 0, "valueCents": 0, "valuedCount": 0}, {"stage": "Qualified", "count": 0, "valueCents": 0, "valuedCount": 0}, {"stage": "Proposal", "count": 0, "valueCents": 0, "valuedCount": 0}, {"stage": "Negotiation", "count": 0, "valueCents": 0, "valuedCount": 0}, {"stage": "Active Client", "count": 0, "valueCents": 0, "valuedCount": 0}, {"stage": "Past Client", "count": 0, "valueCents": 0, "valuedCount": 0}, {"stage": "Lost", "count": 0, "valueCents": 0, "valuedCount": 0}], "pipelineValueCents": 0}, "invoices": {"count": 1, "outstanding": {"count": 1, "totalCents": 0}, "overdue": {"count": 1, "totalCents": 0}, "draftCount": 0, "paidTotalCents": 0}, "assets": {"total": 0, "byStatus": [{"status": "In Service", "count": 0}, {"status": "In Storage", "count": 0}, {"status": "In Repair", "count": 0}, {"status": "Retired", "count": 0}, {"status": "Lost", "count": 0}], "byScope": {"Sentinel": 0, "Client": 0}}, "presence": {"activeSessionCount": 2, "team": [{"id": 1, "displayName": "OwnerWV", "role": "OWNER", "sessions": 1, "lastExpiresAt": 1794272336491}, {"id": 2, "displayName": "StaffWV", "role": "STAFF", "sessions": 1, "lastExpiresAt": 1794272336727}]}, "attention": [{"kind": "followups", "count": 0}, {"kind": "overdueCases", "count": 0}, {"kind": "unassignedTickets", "count": 1}, {"kind": "signinAlerts", "count": 0}], "recentActivity": [{"id": 8, "actorId": 3, "action": "APP_LOGIN", "entityType": "user", "entityId": 3, "detail": "App sign-in \u2014 device enrolled: wv-test.", "createdAt": 1794243539613}, {"id": 7, "actorId": 2, "action": "APP_LOGIN", "entityType": "user", "entityId": 2, "detail": "App sign-in \u2014 device enrolled: wv-test.", "createdAt": 1794243539508}, {"id": 6, "actorId": 1, "action": "APP_LOGIN", "entityType": "user", "entityId": 1, "detail": "App sign-in \u2014 device enrolled: wv-test.", "createdAt": 1794243539388}, {"id": 5, "actorId": null, "action": "maildigest FAILED to send", "entityType": "system", "entityId": null, "detail": "smtp timeout", "createdAt": 1794243528747}, {"id": 4, "actorId": 2, "action": "LOGIN", "entityType": "session", "entityId": 2, "detail": "Session opened; expires in 8 hours", "createdAt": 1794243536727}, {"id": 3, "actorId": 1, "action": "EMPLOYEE_CREATED", "entityType": "user", "entityId": 2, "detail": "Staff user StaffWV created.", "createdAt": 1794243536605}, {"id": 2, "actorId": 1, "action": "LOGIN", "entityType": "session", "entityId": 1, "detail": "Session opened; expires in 8 hours", "createdAt": 1794243536491}, {"id": 1, "actorId": 1, "action": "OWNER_INITIALIZED", "entityType": "settings", "entityId": 1, "detail": "Owner account initialized; recovery codes issued.", "createdAt": 1794243536382}], "canWrite": {"clients": true, "cases": true, "tickets": true, "invoices": true}, "whileYouSlept": {"windowStartMs": 1794182400000, "windowEndMs": 1794243539673, "windowLabel": "Since 6:00 PM CT yesterday", "failures": [{"kind": "backup", "label": "Backup check FAILED \u2014 weekly", "detail": "Checksum mismatch on segment 12."}, {"kind": "activity", "label": "1 failed action(s) overnight", "detail": "See Recent Activity for the failure rows."}], "hasFailures": true, "newsFiled": 2, "newsPublished": 1, "threatBriefsDelivered": 1, "backups": {"available": true, "checkedAt": 1794243528747, "chains": {"nightly": {"ok": true, "archive": "nightly-2026-11-09.tar.age", "detail": "Restore drill passed."}, "weekly": {"ok": false, "archive": null, "detail": "Checksum mismatch on segment 12."}}, "allOk": false}, "ticketsTouched": 1, "jobsTouched": 1, "audio": {"hash": "d713d47fb43cdb4a", "available": false}}, "migrationHead": 172, "pushProviderEnabled": false, "ok": true}
    """

    private val DASH_BK_UNAVAIL = """
{"today": "2026-11-09", "tickets": {"openTotal": 1, "byPriority": [{"priority": "Low", "count": 0}, {"priority": "Normal", "count": 1}, {"priority": "High", "count": 0}, {"priority": "Critical", "count": 0}], "byStatus": [{"status": "New", "count": 0}, {"status": "Open", "count": 1}, {"status": "In Progress", "count": 0}, {"status": "On Hold", "count": 0}, {"status": "Resolved", "count": 0}, {"status": "Closed", "count": 0}], "unassigned": 1}, "cases": {"total": 0, "byStage": [{"status": "New", "count": 0}, {"status": "Open", "count": 0}, {"status": "In Progress", "count": 0}, {"status": "On Hold", "count": 0}, {"status": "Resolved", "count": 0}, {"status": "Closed", "count": 0}], "overdue": 0, "unassigned": 0}, "clients": {"total": 2, "byTier": {"tier-a": 0, "tier-b": 0, "tier-c": 0, "tier-d": 0, "partner": 0, "untiered": 2}, "byStage": [{"stage": "Lead", "count": 2, "valueCents": 0, "valuedCount": 0}, {"stage": "Contacted", "count": 0, "valueCents": 0, "valuedCount": 0}, {"stage": "Qualified", "count": 0, "valueCents": 0, "valuedCount": 0}, {"stage": "Proposal", "count": 0, "valueCents": 0, "valuedCount": 0}, {"stage": "Negotiation", "count": 0, "valueCents": 0, "valuedCount": 0}, {"stage": "Active Client", "count": 0, "valueCents": 0, "valuedCount": 0}, {"stage": "Past Client", "count": 0, "valueCents": 0, "valuedCount": 0}, {"stage": "Lost", "count": 0, "valueCents": 0, "valuedCount": 0}], "pipelineValueCents": 0}, "invoices": {"count": 1, "outstanding": {"count": 1, "totalCents": 0}, "overdue": {"count": 1, "totalCents": 0}, "draftCount": 0, "paidTotalCents": 0}, "assets": {"total": 0, "byStatus": [{"status": "In Service", "count": 0}, {"status": "In Storage", "count": 0}, {"status": "In Repair", "count": 0}, {"status": "Retired", "count": 0}, {"status": "Lost", "count": 0}], "byScope": {"Sentinel": 0, "Client": 0}}, "presence": {"activeSessionCount": 2, "team": [{"id": 1, "displayName": "OwnerWV", "role": "OWNER", "sessions": 1, "lastExpiresAt": 1794272336491}, {"id": 2, "displayName": "StaffWV", "role": "STAFF", "sessions": 1, "lastExpiresAt": 1794272336727}]}, "attention": [{"kind": "followups", "count": 0}, {"kind": "overdueCases", "count": 0}, {"kind": "unassignedTickets", "count": 1}, {"kind": "signinAlerts", "count": 0}], "recentActivity": [{"id": 8, "actorId": 3, "action": "APP_LOGIN", "entityType": "user", "entityId": 3, "detail": "App sign-in \u2014 device enrolled: wv-test.", "createdAt": 1794243539613}, {"id": 7, "actorId": 2, "action": "APP_LOGIN", "entityType": "user", "entityId": 2, "detail": "App sign-in \u2014 device enrolled: wv-test.", "createdAt": 1794243539508}, {"id": 6, "actorId": 1, "action": "APP_LOGIN", "entityType": "user", "entityId": 1, "detail": "App sign-in \u2014 device enrolled: wv-test.", "createdAt": 1794243539388}, {"id": 5, "actorId": null, "action": "maildigest FAILED to send", "entityType": "system", "entityId": null, "detail": "smtp timeout", "createdAt": 1794243528747}, {"id": 4, "actorId": 2, "action": "LOGIN", "entityType": "session", "entityId": 2, "detail": "Session opened; expires in 8 hours", "createdAt": 1794243536727}, {"id": 3, "actorId": 1, "action": "EMPLOYEE_CREATED", "entityType": "user", "entityId": 2, "detail": "Staff user StaffWV created.", "createdAt": 1794243536605}, {"id": 2, "actorId": 1, "action": "LOGIN", "entityType": "session", "entityId": 1, "detail": "Session opened; expires in 8 hours", "createdAt": 1794243536491}, {"id": 1, "actorId": 1, "action": "OWNER_INITIALIZED", "entityType": "settings", "entityId": 1, "detail": "Owner account initialized; recovery codes issued.", "createdAt": 1794243536382}], "canWrite": {"clients": true, "cases": true, "tickets": true, "invoices": true}, "whileYouSlept": {"windowStartMs": 1794182400000, "windowEndMs": 1794243541516, "windowLabel": "Since 6:00 PM CT yesterday", "failures": [{"kind": "backup", "label": "Backup verdict unavailable", "detail": "The backup integrity verdict file could not be read \u2014 no backup state is claimed."}, {"kind": "activity", "label": "1 failed action(s) overnight", "detail": "See Recent Activity for the failure rows."}], "hasFailures": true, "newsFiled": 2, "newsPublished": 1, "threatBriefsDelivered": 1, "backups": {"available": false, "checkedAt": null, "chains": {}, "allOk": false}, "ticketsTouched": 1, "jobsTouched": 1, "audio": {"hash": "bd2d73a44806acff", "available": false}}, "migrationHead": 172, "pushProviderEnabled": false, "ok": true}
    """

    private val AUDIO_OK = """
{"authorized": true, "available": true, "hash": "d713d47fb43cdb4a", "contentType": "audio/wav", "audioB64": "UklGRmQGAABXQVZFZm10IBAAAAABAAEAQB8AAIA+AAACABAAZGF0YUAGAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"}
    """

    private val AUDIO_DENIED = """
{"authorized": false}
    """

    private val AUDIO_APPSHAPE = """
{"authorized": false}
    """

    private val HEALTH_SCORED = """
{"clientId": 1, "events": [{"kind": "appointment", "at": 1794243528747, "title": "Appointment: Morning rounds", "detail": "Scheduled", "refType": "job", "refId": 1}, {"kind": "invoice-issued", "at": 1790787528747, "title": "Invoice INV-WV-1 issued", "detail": "Sent \u00b7 $0", "refType": "invoice", "refId": 1}, {"kind": "ticket-opened", "at": 1790787528747, "title": "Ticket opened: Lobby camera offline", "detail": "Open \u00b7 Normal", "refType": "ticket", "refId": 1}, {"kind": "client-created", "at": 1789059528747, "title": "Client record created", "detail": "Wave Bakery LLC entered the client book.", "refType": "client", "refId": 1}], "nextCursor": null, "total": 4, "pageSize": 25, "securityScore": {"state": "scored", "score": 37, "baseline": 40, "factors": [{"label": "1 overdue invoice", "points": -5}, {"label": "Activity in the last 90 days", "points": 5}, {"label": "1 open normal-priority ticket", "points": -3}, {"label": "No assets on file", "points": 0}, {"label": "0 of 1 tickets and cases resolved or closed", "points": 0}, {"label": "No form records on file", "points": 0}], "counts": {"assets": 0, "openTickets": 1, "openCases": 0, "closedItems": 0, "totalItems": 1, "overdueInvoices": 1, "forms": 0}}, "clientHealth": {"state": "scored", "score": 82, "band": "Good", "signals": {"openTickets": 1, "oldestOpenTicketAgeDays": 40, "overdueInvoices": 1, "expiringAssets": 0, "daysSinceContact": 0}, "deductions": [{"label": "Open ticket age", "points": -10}, {"label": "Overdue invoices", "points": -8}], "trend": {"direction": "down", "delta": -8, "baselineScore": 90, "baselineAt": 1793379528747}}, "ok": true}
    """

    private val HEALTH_INSUF = """
{"clientId": 2, "events": [{"kind": "client-created", "at": 1789059528747, "title": "Client record created", "detail": "Zero Data LLC entered the client book.", "refType": "client", "refId": 2}], "nextCursor": null, "total": 1, "pageSize": 25, "securityScore": {"state": "insufficient", "score": null, "baseline": 40, "factors": [{"label": "Not enough information yet \u2014 no assets, tickets, or cases on file for this client.", "points": 0}], "counts": {"assets": 0, "openTickets": 0, "openCases": 0, "closedItems": 0, "totalItems": 0, "overdueInvoices": 0, "forms": 0}}, "clientHealth": {"state": "insufficient", "score": null, "band": null, "signals": {"openTickets": 0, "oldestOpenTicketAgeDays": 0, "overdueInvoices": 0, "expiringAssets": 0, "daysSinceContact": null}, "deductions": [], "trend": {"direction": "none", "delta": null, "baselineScore": null, "baselineAt": null}}, "ok": true}
    """

    private val TICKET_PROFILE = """
{"ticket": {"id": 1, "clientId": 1, "assignedUserId": null, "title": "Lobby camera offline", "status": "Open", "priority": "Normal", "category": "", "dueDate": null, "openedDate": "", "closedDate": "", "notes": "", "adMaterial": false, "archivedAt": null, "archivedBy": null, "createdAt": 1790787528747, "updatedAt": 1794243528747, "caseId": null, "source": "Internal", "sourceRef": null, "mergedIntoId": null, "tags": ""}, "client": {"id": 1, "legalName": "Wave Bakery LLC", "name": "Wave Bakery LLC"}, "case": null, "activity": [], "cfOps": {"ticketId": 1, "merged": false, "mergedIntoId": null, "sla": {"priority": "Normal", "responseTargetMin": 480, "resolutionTargetMin": 4320, "firstResponseAt": null, "responseDueAt": 1790816328747, "resolutionDueAt": 1791046728747, "resolvedAt": null, "responseState": "breached", "resolutionState": "breached", "breaches": []}, "photos": [{"id": 1, "ticketId": 1, "caseId": null, "clientId": 1, "kind": "after", "caption": "Camera back online", "originalFilename": "after.jpg", "contentType": "image/jpeg", "sizeBytes": 1234, "uploadedBy": null, "createdAt": 1794243525747, "downloadUrl": "/photos/download?id=1"}, {"id": 2, "ticketId": 1, "caseId": null, "clientId": 1, "kind": "other", "caption": "Cable run", "originalFilename": "cable.jpg", "contentType": "image/jpeg", "sizeBytes": 1234, "uploadedBy": null, "createdAt": 1794243526747, "downloadUrl": "/photos/download?id=2"}, {"id": 3, "ticketId": 1, "caseId": null, "clientId": 1, "kind": "before", "caption": "Dead camera", "originalFilename": "before.jpg", "contentType": "image/jpeg", "sizeBytes": 1234, "uploadedBy": null, "createdAt": 1794243527747, "downloadUrl": "/photos/download?id=3"}], "followUps": [], "merges": []}, "b4fSignOff": null, "photoTimeline": [{"id": 3, "ticketId": 1, "caseId": null, "clientId": 1, "kind": "before", "caption": "Dead camera", "originalFilename": "before.jpg", "contentType": "image/jpeg", "sizeBytes": 1234, "uploadedBy": null, "createdAt": 1794243527747, "downloadUrl": "/photos/download?id=3", "stage": "before", "stageLabel": "Before"}, {"id": 2, "ticketId": 1, "caseId": null, "clientId": 1, "kind": "other", "caption": "Cable run", "originalFilename": "cable.jpg", "contentType": "image/jpeg", "sizeBytes": 1234, "uploadedBy": null, "createdAt": 1794243526747, "downloadUrl": "/photos/download?id=2", "stage": "other", "stageLabel": "Other"}, {"id": 1, "ticketId": 1, "caseId": null, "clientId": 1, "kind": "after", "caption": "Camera back online", "originalFilename": "after.jpg", "contentType": "image/jpeg", "sizeBytes": 1234, "uploadedBy": null, "createdAt": 1794243525747, "downloadUrl": "/photos/download?id=1", "stage": "after", "stageLabel": "After"}], "ok": true}
    """

    private val PORTAL_HOME = """
{"linked": true, "portalLang": "en", "contactKind": "primary", "portalAccess": {"work": true, "money": true}, "portalMessageUnread": 0, "notificationPrefs": {"ticketUpdates": true, "invoiceEvents": true, "monitoringAlerts": true, "digest": false}, "canEditPrefs": true, "securityScore": {"state": "scored", "score": 37, "baseline": 40, "factors": [{"label": "1 overdue invoice", "points": -5}, {"label": "Activity in the last 90 days", "points": 5}, {"label": "1 open normal-priority ticket", "points": -3}, {"label": "No assets on file", "points": 0}, {"label": "0 of 1 tickets and cases resolved or closed", "points": 0}, {"label": "No form records on file", "points": 0}], "counts": {"assets": 0, "openTickets": 1, "openCases": 0, "closedItems": 0, "totalItems": 1, "overdueInvoices": 1, "forms": 0}}, "profile": {"legalName": "Wave Bakery LLC", "dba": "", "contactName": "Wren Vale", "contactTitle": "", "email": "wren@wavebakery.test", "phone": "", "street": "", "city": "", "state": "", "zip": ""}, "cases": [], "tickets": [{"id": 1, "title": "Lobby camera offline", "status": "Open", "priority": "Normal", "category": "", "dueDate": null, "closedDate": "", "createdAt": 1790787528747, "updatedAt": 1794243528747}], "invoices": [{"id": 1, "invoiceNumber": "INV-WV-1", "status": "Sent", "issueDate": "", "dueDate": "2026-01-01", "paidDate": "", "overdue": true, "items": [], "subtotalCents": 0, "taxCents": 0, "totalCents": 0, "discountCents": 0, "payUrl": null, "paymentEnabled": false, "paidCents": 0, "balanceDueCents": 0, "payments": []}], "documents": [], "appointments": [{"id": 1, "title": "Morning rounds", "location": "", "workDate": "", "startTime": "", "endTime": "", "allDay": false, "status": "Scheduled"}], "assets": [], "progress": [{"kind": "ticket", "id": 1, "title": "Lobby camera offline", "status": "Open", "priority": "Normal", "category": "", "dueDate": null, "closedDate": "", "createdAt": 1790787528747, "updatedAt": 1794243528747, "progressStage": "Received", "progressStageIndex": 0, "progressStages": ["Received", "In Progress", "Resolved"], "history": [{"progressStage": "Received", "at": 1790787528747, "atKind": "created"}], "photos": [{"id": 1, "ticketId": 1, "caseId": null, "clientId": 1, "kind": "after", "caption": "Camera back online", "originalFilename": "after.jpg", "contentType": "image/jpeg", "sizeBytes": 1234, "uploadedBy": null, "createdAt": 1794243525747, "downloadUrl": "/photos/download?id=1"}, {"id": 2, "ticketId": 1, "caseId": null, "clientId": 1, "kind": "other", "caption": "Cable run", "originalFilename": "cable.jpg", "contentType": "image/jpeg", "sizeBytes": 1234, "uploadedBy": null, "createdAt": 1794243526747, "downloadUrl": "/photos/download?id=2"}, {"id": 3, "ticketId": 1, "caseId": null, "clientId": 1, "kind": "before", "caption": "Dead camera", "originalFilename": "before.jpg", "contentType": "image/jpeg", "sizeBytes": 1234, "uploadedBy": null, "createdAt": 1794243527747, "downloadUrl": "/photos/download?id=3"}], "photoTimeline": [{"id": 3, "ticketId": 1, "caseId": null, "clientId": 1, "kind": "before", "caption": "Dead camera", "originalFilename": "before.jpg", "contentType": "image/jpeg", "sizeBytes": 1234, "uploadedBy": null, "createdAt": 1794243527747, "downloadUrl": "/photos/download?id=3", "stage": "before", "stageLabel": "Before"}, {"id": 2, "ticketId": 1, "caseId": null, "clientId": 1, "kind": "other", "caption": "Cable run", "originalFilename": "cable.jpg", "contentType": "image/jpeg", "sizeBytes": 1234, "uploadedBy": null, "createdAt": 1794243526747, "downloadUrl": "/photos/download?id=2", "stage": "other", "stageLabel": "Other"}, {"id": 1, "ticketId": 1, "caseId": null, "clientId": 1, "kind": "after", "caption": "Camera back online", "originalFilename": "after.jpg", "contentType": "image/jpeg", "sizeBytes": 1234, "uploadedBy": null, "createdAt": 1794243525747, "downloadUrl": "/photos/download?id=1", "stage": "after", "stageLabel": "After"}]}], "quotes": [], "paymentPlans": [], "cfRoadmap": {"items": [], "total": 0, "doneCount": 0, "percent": 0}, "cfBreachMatches": [], "cfBreachAlerts": [], "cfComplianceSummary": [], "library": [], "siteProfiles": [], "cf2Hygiene": {"items": [{"key": "mfa_enabled", "label": "Multi-factor authentication enabled", "state": "unknown", "checkedAt": null}, {"key": "password_manager", "label": "Password manager in use", "state": "unknown", "checkedAt": null}, {"key": "unique_passwords_attested", "label": "Unique passwords attested (no reuse)", "state": "unknown", "checkedAt": null}, {"key": "backup_codes_stored", "label": "Backup / recovery codes stored safely", "state": "unknown", "checkedAt": null}, {"key": "recovery_email_current", "label": "Recovery email address current", "state": "unknown", "checkedAt": null}, {"key": "recovery_phone_current", "label": "Recovery phone number current", "state": "unknown", "checkedAt": null}, {"key": "admin_accounts_reviewed", "label": "Admin accounts reviewed", "state": "unknown", "checkedAt": null}, {"key": "shared_passwords_rotated", "label": "Shared passwords rotated when staff changes", "state": "unknown", "checkedAt": null}], "counts": {"yes": 0, "no": 0, "unknown": 8}, "total": 8}, "threatBriefs": [{"id": 1, "period": "2026-11", "title": "Overnight brief", "body": "Body.", "deliveredAt": 1794243528747}], "monthlyReports": [], "ok": true}
    """

    private val BOOKING_ADMIN = """
{"availability": [], "services": [{"id": 3, "name": "General Consultation", "durationMinutes": 60, "active": true, "createdAt": 1794243528747, "updatedAt": 1794243528747, "prep": null}, {"id": 1, "name": "Operating System Reinstall", "durationMinutes": 60, "active": true, "createdAt": 1794243528747, "updatedAt": 1794243528747, "prep": {"en": ["Back up any files you want to keep \u2014 a reinstall erases the drive.", "Have your account passwords handy (for example your Microsoft or Apple account sign-in).", "Know which programs you need reinstalled afterwards."], "es": ["Haga una copia de seguridad de los archivos que quiera conservar \u2014 una reinstalaci\u00f3n borra el disco.", "Tenga a mano las contrase\u00f1as de sus cuentas (por ejemplo, su cuenta de Microsoft o Apple).", "Sepa qu\u00e9 programas necesita reinstalar despu\u00e9s."]}}, {"id": 2, "name": "Password Reset", "durationMinutes": 60, "active": true, "createdAt": 1794243528747, "updatedAt": 1794243528747, "prep": {"en": ["Bring proof the account or device is yours (photo ID or a purchase receipt).", "Have any old passwords, or the recovery email or phone, that you still have access to."], "es": ["Traiga una prueba de que la cuenta o el dispositivo es suyo (identificaci\u00f3n con foto o recibo de compra).", "Tenga a mano cualquier contrase\u00f1a antigua, o el correo o tel\u00e9fono de recuperaci\u00f3n al que todav\u00eda tenga acceso."]}}], "bookings": [], "staffAvailabilityWarnings": [], "weekdayNote": "Weekday: Monday=0 .. Sunday=6; times are America/Chicago wall-clock, like the Schedule.", "ok": true}
    """

    // ---- A. While You Slept ----

    @Test
    fun wysGoldenParsesWithFailuresFirst() {
        val wys = Wave161Logic.parseWys(obj(DASH_AUDIO)["whileYouSlept"]?.jsonObject)
        assertTrue(wys != null)
        wys!!
        assertEquals("Since 6:00 PM CT yesterday", wys.windowLabel)
        assertTrue(wys.hasFailures)
        assertEquals(2, wys.failures.size)
        assertEquals("backup", wys.failures[0].kind)
        assertEquals("Backup check FAILED \u2014 weekly", wys.failures[0].label)
        assertEquals("Checksum mismatch on segment 12.", wys.failures[0].detail)
        assertEquals("activity", wys.failures[1].kind)
        assertEquals(2, wys.newsFiled)
        assertEquals(1, wys.newsPublished)
        assertEquals(1, wys.threatBriefsDelivered)
        assertEquals(1, wys.ticketsTouched)
        assertEquals(1, wys.jobsTouched)
        assertTrue(wys.audio.available)
        assertEquals("d713d47fb43cdb4a", wys.audio.hash)
    }

    @Test
    fun wysBackupsBlockParsesChainsInServedOrder() {
        val wys = Wave161Logic.parseWys(obj(DASH_AUDIO)["whileYouSlept"]?.jsonObject)!!
        assertTrue(wys.backups.available)
        assertFalse(wys.backups.allOk)
        assertEquals(listOf("nightly", "weekly"), wys.backups.chains.map { it.name })
        assertTrue(wys.backups.chains[0].ok)
        assertEquals("nightly-2026-11-09.tar.age", wys.backups.chains[0].archive)
        assertFalse(wys.backups.chains[1].ok)
        assertNull(wys.backups.chains[1].archive)
        assertEquals("Backups: a check needs attention", Wave161Logic.backupsSummaryLine(wys.backups))
    }

    @Test
    fun wysBackupsUnavailableIsHonest() {
        val wys = Wave161Logic.parseWys(obj(DASH_BK_UNAVAIL)["whileYouSlept"]?.jsonObject)!!
        assertFalse(wys.backups.available)
        assertTrue(wys.backups.chains.isEmpty())
        assertEquals(
            Wave161Logic.BACKUPS_UNAVAILABLE_LINE,
            Wave161Logic.backupsSummaryLine(wys.backups),
        )
        assertTrue(wys.failures.any { it.label == "Backup verdict unavailable" })
    }

    @Test
    fun wysAbsentFieldParsesToNoCard() {
        assertNull(Wave161Logic.parseWys(null))
        // The typed decode carries the field when present...
        val dash = json.decodeFromString<DashboardResponse>(DASH_AUDIO)
        assertTrue(dash.whileYouSlept != null)
        // ...and a payload WITHOUT the key (every staff/client
        // dashboard) decodes with it null — no card renders.
        val stripped = kotlinx.serialization.json.JsonObject(
            obj(DASH_AUDIO) - "whileYouSlept",
        ).toString()
        val dashStripped = json.decodeFromString<DashboardResponse>(stripped)
        assertNull(dashStripped.whileYouSlept)
        assertNull(Wave161Logic.parseWys(dashStripped.whileYouSlept))
    }

    @Test
    fun wysAudioUnavailableBeforeGeneration() {
        val wys = Wave161Logic.parseWys(obj(DASH_NOAUDIO)["whileYouSlept"]?.jsonObject)!!
        assertFalse(wys.audio.available)
        assertEquals("Briefing audio not generated yet.", Wave161Logic.AUDIO_NOT_GENERATED_LINE)
    }

    // ---- B. Briefing audio ----

    @Test
    fun wysAudioAvailableDecodesServerBytes() {
        val r = Wave161Logic.parseWysAudio(obj(AUDIO_OK))
        assertTrue(r is Wave161Logic.WysAudioResult.Ready)
        r as Wave161Logic.WysAudioResult.Ready
        assertEquals("wav", r.extension)
        assertEquals(1644, r.bytes.size)
        assertEquals("RIFF", String(r.bytes.copyOfRange(0, 4), Charsets.US_ASCII))
    }

    @Test
    fun wysAudioDeniedShapesDegradeQuietly() {
        assertTrue(Wave161Logic.parseWysAudio(obj(AUDIO_DENIED)) is Wave161Logic.WysAudioResult.Denied)
        // The app-shaped call (device header, no web session)
        // receives the same denied shape on current servers —
        // pinned so the day the server lights the route up,
        // this fixture is the thing that changes.
        assertTrue(Wave161Logic.parseWysAudio(obj(AUDIO_APPSHAPE)) is Wave161Logic.WysAudioResult.Denied)
    }

    @Test
    fun wysAudioAuthorizedButUngeneratedIsNotGenerated() {
        val body = obj("""{"authorized": true, "available": false, "hash": "abc123"}""")
        assertTrue(Wave161Logic.parseWysAudio(body) is Wave161Logic.WysAudioResult.NotGenerated)
    }

    // ---- C. Client health ----

    @Test
    fun clientHealthScoredGoldenParses() {
        val h = Wave161Logic.parseClientHealth(obj(HEALTH_SCORED))
        assertTrue(h != null)
        h!!
        assertEquals("scored", h.state)
        assertEquals(82, h.score)
        assertEquals("Good", h.band)
        assertEquals("good", Wave161Logic.bandColorKey(h.band))
        assertEquals(1, h.openTickets)
        assertEquals(40, h.oldestOpenTicketAgeDays)
        assertEquals(1, h.overdueInvoices)
        assertEquals(0, h.expiringAssets)
        assertEquals(0L, h.daysSinceContact)
        assertEquals(2, h.deductions.size)
        assertEquals("Open ticket age", h.deductions[0].label)
        assertEquals(-10, h.deductions[0].points)
        assertEquals("Overdue invoices", h.deductions[1].label)
        assertEquals(-8, h.deductions[1].points)
        assertEquals("down", h.trendDirection)
        assertEquals(-8, h.trendDelta)
        assertEquals(90, h.trendBaselineScore)
        assertFalse(Wave161Logic.isInsufficient(h))
        assertEquals("Down 8 from 90 on 2026-10-30", Wave161Logic.trendLine(h, "2026-10-30"))
    }

    @Test
    fun clientHealthBandColorsKeyOffTheBandString() {
        assertEquals("good", Wave161Logic.bandColorKey("Good"))
        assertEquals("fair", Wave161Logic.bandColorKey("Fair"))
        assertEquals("atrisk", Wave161Logic.bandColorKey("At risk"))
        assertEquals("critical", Wave161Logic.bandColorKey("Critical"))
        assertEquals("none", Wave161Logic.bandColorKey(null))
        assertEquals("none", Wave161Logic.bandColorKey("Something new"))
    }

    @Test
    fun clientHealthInsufficientNeverYieldsAScore() {
        val h = Wave161Logic.parseClientHealth(obj(HEALTH_INSUF))
        assertTrue(h != null)
        h!!
        assertEquals("insufficient", h.state)
        assertNull(h.score)
        assertNull(h.band)
        assertTrue(Wave161Logic.isInsufficient(h))
        assertEquals(
            "Not enough history to score this client yet.",
            Wave161Logic.HEALTH_INSUFFICIENT_COPY,
        )
        assertNull(Wave161Logic.trendLine(h, "2026-10-30"))
    }

    @Test
    fun clientHealthAbsentFieldLeavesSurfaceUnchanged() {
        assertNull(Wave161Logic.parseClientHealth(obj("""{"events": []}""")))
        assertNull(Wave161Logic.parseClientHealth(null))
    }

    // ---- D. Photo timeline ----

    @Test
    fun photoTimelineGroupsInServedOrder() {
        val detail = json.decodeFromString<TicketDetailResponse>(TICKET_PROFILE)
        assertEquals(3, detail.photoTimeline.size)
        val groups = Wave161Logic.photoGroups(detail.photoTimeline)
        assertEquals(listOf("Before", "Other", "After"), groups.map { it.stageLabel })
        assertTrue(groups.all { it.photos.size == 1 })
        assertEquals("Dead camera", Wave161Logic.photoLabel(groups[0].photos[0]))
        assertEquals("Camera back online", Wave161Logic.photoLabel(groups[2].photos[0]))
    }

    @Test
    fun photoTimelinePortalEntryMatches() {
        val progress = Phase3Logic.arr(obj(PORTAL_HOME), "progress")
        assertEquals(1, progress.size)
        val tl = Phase3Logic.arr(progress[0], "photoTimeline")
        assertEquals(
            listOf("Before", "Other", "After"),
            Wave161Logic.photoGroups(tl).map { it.stageLabel },
        )
    }

    @Test
    fun photoGroupingNeverResorts() {
        // Served order interleaved on purpose: consecutive
        // grouping must produce three groups, NOT a re-sort
        // into two. (Server order is stage-ranked already;
        // this pins that the client adds no ordering of its
        // own even if a future payload interleaves.)
        val rows = listOf(
            obj("""{"id": 1, "stage": "before", "stageLabel": "Before"}"""),
            obj("""{"id": 2, "stage": "after", "stageLabel": "After"}"""),
            obj("""{"id": 3, "stage": "before", "stageLabel": "Before"}"""),
        )
        val groups = Wave161Logic.photoGroups(rows)
        assertEquals(listOf("Before", "After", "Before"), groups.map { it.stageLabel })
        assertTrue(Wave161Logic.photoGroups(emptyList()).isEmpty())
    }

    // ---- E. Booking prep ----

    @Test
    fun prepParsesFromBookingAdminGolden() {
        val svcs = Phase3Logic.arr(obj(BOOKING_ADMIN), "services")
        val byName = svcs.associateBy { Phase3Logic.str(it, "name") }
        val reinstall = Wave161Logic.prepLines(byName.getValue("Operating System Reinstall"), "en")
        assertEquals(3, reinstall!!.size)
        assertTrue(reinstall[0].startsWith("Back up any files you want to keep"))
        val reinstallEs = Wave161Logic.prepLines(byName.getValue("Operating System Reinstall"), "es")
        assertEquals(3, reinstallEs!!.size)
        val password = Wave161Logic.prepLines(byName.getValue("Password Reset"), "en")
        assertEquals(2, password!!.size)
        // The no-prep service: null in BOTH languages — no
        // filler, no header, nothing.
        assertNull(Wave161Logic.prepLines(byName.getValue("General Consultation"), "en"))
        assertNull(Wave161Logic.prepLines(byName.getValue("General Consultation"), "es"))
    }

    @Test
    fun prepLanguageSelectionAndEmpties() {
        val holder = obj("""{"prep": {"en": ["EN one"], "es": ["ES uno"]}}""")
        assertEquals(listOf("EN one"), Wave161Logic.prepLines(holder, "en"))
        assertEquals(listOf("ES uno"), Wave161Logic.prepLines(holder, "es"))
        assertEquals(listOf("EN one"), Wave161Logic.prepLines(holder, "fr"))
        assertNull(Wave161Logic.prepLines(obj("""{"prep": null}"""), "en"))
        assertNull(Wave161Logic.prepLines(obj("""{}"""), "en"))
        assertNull(Wave161Logic.prepLines(obj("""{"prep": {"en": [], "es": []}}"""), "en"))
    }

    // ---- F. Pipeline sources ----

    @Test
    fun leadSourceLabelsCoverTheWave() {
        assertEquals("Academy owner reset", Wave161Logic.leadSourceLabel("academy-owner-reset"))
        assertEquals("Security scorecard", Wave161Logic.leadSourceLabel("security-scorecard"))
    }

    @Test
    fun leadSourceLabelsCoverTheFullSetOfRecord() {
        val expected = linkedMapOf(
            "website_contact" to "Website contact",
            "website_refer" to "Website referral",
            "manual" to "Manual",
            "contact-form" to "Contact form",
            "intake-wizard" to "Intake wizard",
            "other" to "Other",
            "academy-owner-reset" to "Academy owner reset",
            "security-scorecard" to "Security scorecard",
        )
        assertEquals(expected.keys.toList(), Wave161Logic.LEAD_SOURCES)
        expected.forEach { (raw, label) ->
            assertEquals(label, Wave161Logic.leadSourceLabel(raw))
        }
        // The filter set derives from the same mapping, in
        // the same order.
        assertEquals(
            expected.entries.map { it.key to it.value },
            Wave161Logic.leadSourceFilters(),
        )
    }

    @Test
    fun leadSourceUnknownHumanizes() {
        assertEquals("Partner referral", Wave161Logic.leadSourceLabel("partner-referral"))
        assertEquals("Trade show", Wave161Logic.leadSourceLabel("trade_show"))
    }
}

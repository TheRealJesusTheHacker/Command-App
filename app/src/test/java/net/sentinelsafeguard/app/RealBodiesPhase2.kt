package net.sentinelsafeguard.app

/**
 * Phase 2 REAL response bodies, captured 2026-11-03 from a
 * rehearsal instance running the byte-exact deployed Phase 2
 * backend (server.py sha256
 * 8146986dee15fd210d1762037dcd9b4d0f43b1274b110d8159c64a34f429b8a5,
 * Addendum 12), against a seeded scratch database. No tokens or
 * secrets were recorded. These pin every Phase 2 endpoint the
 * hunt6 app consumes (cases, jobs/schedule, engagements,
 * runbooks & sign-off, Today panels, global search) to the
 * real contract.
 */
object RealBodiesPhase2 {
    const val CASE_CREATE: String = """
{"ok": true, "id": 1, "case": {"id": 1, "clientId": 1, "assignedUserId": null, "title": "Phase2 case alpha", "status": "Open", "priority": "High", "category": "Support", "dueDate": "2020-01-01", "openedDate": "", "closedDate": "", "notes": "alpha", "adMaterial": false, "archivedAt": null, "archivedBy": null, "createdAt": 1793722291411, "updatedAt": 1793722291411, "clientName": "Phase2 Client LLC", "ticketCount": 0}}
    """

    const val CASES_LIST: String = """
{"ok": true, "total": 1, "limit": 50, "offset": 0, "cases": [{"id": 1, "clientId": 1, "assignedUserId": null, "title": "Phase2 case alpha", "status": "Open", "priority": "High", "category": "Support", "dueDate": "2020-01-01", "openedDate": "", "closedDate": "", "notes": "alpha", "adMaterial": false, "archivedAt": null, "archivedBy": null, "createdAt": 1793722291411, "updatedAt": 1793722291411, "clientName": "Phase2 Client LLC", "ticketCount": 0}]}
    """

    const val CASES_LIST_FILTERED: String = """
{"ok": true, "total": 1, "limit": 50, "offset": 0, "cases": [{"id": 1, "clientId": 1, "assignedUserId": null, "title": "Phase2 case alpha", "status": "Open", "priority": "High", "category": "Support", "dueDate": "2020-01-01", "openedDate": "", "closedDate": "", "notes": "alpha", "adMaterial": false, "archivedAt": null, "archivedBy": null, "createdAt": 1793722291411, "updatedAt": 1793722291411, "clientName": "Phase2 Client LLC", "ticketCount": 0}]}
    """

    const val CASE_DETAIL: String = """
{"case": {"id": 1, "clientId": 1, "assignedUserId": null, "title": "Phase2 case alpha", "status": "Open", "priority": "High", "category": "Support", "dueDate": "2020-01-01", "openedDate": "", "closedDate": "", "notes": "alpha", "adMaterial": false, "archivedAt": null, "archivedBy": null, "createdAt": 1793722291411, "updatedAt": 1793722291411}, "client": {"id": 1, "legalName": "Phase2 Client LLC", "dba": "", "type": "Corporate", "industry": "Testing", "status": "Active", "stage": "Active Client", "value": 2000.0, "nextFollowUp": null, "source": "", "sourceDetail": "", "contactName": "Pat P", "contactTitle": "", "email": "pat@example.invalid", "phone": "555-0101", "street": "", "city": "", "state": "", "zip": "", "notes": "", "tags": "tier-a", "friends": false, "importBatchId": null, "archivedAt": null, "archivedBy": null, "createdAt": 1793722291406, "updatedAt": 1793722291406}, "tickets": [], "activity": [{"id": 6, "actorId": 1, "action": "Case_SAVED", "entityType": "case", "entityId": 1, "detail": "Case saved.", "createdAt": 1793722291411}], "ok": true}
    """

    const val CASE_PATCH: String = """
{"ok": true, "case": {"id": 1, "clientId": 1, "assignedUserId": null, "title": "Phase2 case alpha", "status": "Open", "priority": "Critical", "category": "Support", "dueDate": "2020-01-01", "openedDate": "", "closedDate": "", "notes": "alpha", "adMaterial": false, "archivedAt": null, "archivedBy": null, "createdAt": 1793722291411, "updatedAt": 1793722291430, "clientName": "Phase2 Client LLC", "ticketCount": 0}}
    """

    const val CASE_STATUS: String = """
{"ok": true, "status": "In Progress", "case": {"id": 1, "clientId": 1, "assignedUserId": null, "title": "Phase2 case alpha", "status": "In Progress", "priority": "Critical", "category": "Support", "dueDate": "2020-01-01", "openedDate": "", "closedDate": "", "notes": "alpha", "adMaterial": false, "archivedAt": null, "archivedBy": null, "createdAt": 1793722291411, "updatedAt": 1793722291435, "clientName": "Phase2 Client LLC", "ticketCount": 0}}
    """

    const val TICKETS_LIST_CASETITLE: String = """
{"ok": true, "total": 1, "limit": 50, "offset": 0, "tickets": [{"id": 1, "clientId": 1, "assignedUserId": null, "title": "Phase2 ticket", "status": "Open", "priority": "Normal", "category": "Support", "dueDate": null, "openedDate": "", "closedDate": "", "notes": "", "adMaterial": false, "archivedAt": null, "archivedBy": null, "createdAt": 1793722291441, "updatedAt": 1793722291441, "caseId": 1, "source": "Internal", "sourceRef": null, "mergedIntoId": null, "caseTitle": "Phase2 case alpha", "clientName": "Phase2 Client LLC"}]}
    """

    const val JOB_CREATE: String = """
{"ok": true, "id": 1, "job": {"id": 1, "title": "Phase2 job", "clientId": 1, "caseId": 1, "clientName": "Phase2 Client LLC", "clientContactName": "Pat P", "clientContactTitle": "", "clientEmail": "pat@example.invalid", "clientPhone": "555-0101", "caseTitle": "Phase2 case alpha", "location": "Site A", "workDate": "2026-11-10", "startTime": "09:00", "endTime": "10:00", "allDay": false, "status": "Scheduled", "notes": "bring tools", "assignees": [{"id": 2, "displayName": "StaffP2", "role": "STAFF"}], "assigneeIds": [2], "createdBy": 1, "createdAt": 1793722291452, "updatedAt": 1793722291452}}
    """

    const val JOB_PATCH: String = """
{"ok": true, "job": {"id": 1, "title": "Phase2 job", "clientId": 1, "caseId": 1, "clientName": "Phase2 Client LLC", "clientContactName": "Pat P", "clientContactTitle": "", "clientEmail": "pat@example.invalid", "clientPhone": "555-0101", "caseTitle": "Phase2 case alpha", "location": "Site B", "workDate": "2026-11-10", "startTime": "09:00", "endTime": "10:00", "allDay": false, "status": "Scheduled", "notes": "bring tools", "assignees": [{"id": 2, "displayName": "StaffP2", "role": "STAFF"}], "assigneeIds": [2], "createdBy": 1, "createdAt": 1793722291452, "updatedAt": 1793722291462}}
    """

    const val JOB_STATUS: String = """
{"ok": true, "status": "In Progress", "job": {"id": 1, "title": "Phase2 job", "clientId": 1, "caseId": 1, "clientName": "Phase2 Client LLC", "clientContactName": "Pat P", "clientContactTitle": "", "clientEmail": "pat@example.invalid", "clientPhone": "555-0101", "caseTitle": "Phase2 case alpha", "location": "Site B", "workDate": "2026-11-10", "startTime": "09:00", "endTime": "10:00", "allDay": false, "status": "In Progress", "notes": "bring tools", "assignees": [{"id": 2, "displayName": "StaffP2", "role": "STAFF"}], "assigneeIds": [2], "createdBy": 1, "createdAt": 1793722291452, "updatedAt": 1793722291469}}
    """

    const val SCHEDULE_WINDOW: String = """
{"scope": "all", "jobs": [{"id": 1, "title": "Phase2 job", "clientId": 1, "caseId": 1, "clientName": "Phase2 Client LLC", "clientContactName": "Pat P", "clientContactTitle": "", "clientEmail": "pat@example.invalid", "clientPhone": "555-0101", "caseTitle": "Phase2 case alpha", "location": "Site B", "workDate": "2026-11-10", "startTime": "09:00", "endTime": "10:00", "allDay": false, "status": "In Progress", "notes": "bring tools", "assignees": [{"id": 2, "displayName": "StaffP2", "role": "STAFF"}], "assigneeIds": [2], "createdBy": 1, "createdAt": 1793722291452, "updatedAt": 1793722291469}], "employees": [{"id": 1, "displayName": "OwnerP2", "role": "OWNER", "portalLang": "en", "clientId": null, "jobTitle": "", "email": "", "phone": "", "active": true, "createdAt": 1793722291080}, {"id": 2, "displayName": "StaffP2", "role": "STAFF", "portalLang": "en", "clientId": null, "jobTitle": "Tech", "email": "", "phone": "", "active": true, "createdAt": 1793722291299}], "entries": [], "range": {"startDate": "2026-11-09", "endDate": "2026-11-16"}, "ok": true}
    """

    const val ENTRY_SAVE: String = """
{"ok": true, "id": 1, "message": "Schedule entry saved."}
    """

    const val ENGAGEMENT_CREATE: String = """
{"ok": true, "id": 1, "message": "Engagement created.", "engagement": {"id": 1, "clientId": 1, "title": "Phase2 engagement", "scope": "External", "rulesOfEngagement": "Written authorization on file.", "status": "Planned", "startDate": "2026-11-05", "endDate": "2026-11-20", "reportPublished": false, "reportGeneratedAt": null, "hasSignedAuthorization": false, "createdAt": 1793722291484, "updatedAt": 1793722291484, "findings": []}}
    """

    const val ENGAGEMENTS_LIST: String = """
{"ok": true, "engagements": [{"id": 1, "clientId": 1, "title": "Phase2 engagement", "scope": "External", "rulesOfEngagement": "Written authorization on file.", "status": "Planned", "startDate": "2026-11-05", "endDate": "2026-11-20", "reportPublished": false, "reportGeneratedAt": null, "hasSignedAuthorization": false, "createdAt": 1793722291484, "updatedAt": 1793722291484, "findings": []}], "severities": ["Critical", "High", "Medium", "Low", "Informational"], "findingStatuses": ["Open", "In Progress", "Resolved", "Closed"], "engagementStatuses": ["Planned", "Active", "On Hold", "Complete", "Cancelled"]}
    """

    const val ENGAGEMENT_DETAIL: String = """
{"ok": true, "engagement": {"id": 1, "clientId": 1, "title": "Phase2 engagement", "scope": "External", "rulesOfEngagement": "Written authorization on file.", "status": "Planned", "startDate": "2026-11-05", "endDate": "2026-11-20", "reportPublished": false, "reportGeneratedAt": null, "hasSignedAuthorization": false, "createdAt": 1793722291484, "updatedAt": 1793722291484, "findings": []}}
    """

    const val RUNBOOK_PROCEDURE_SET: String = """
{"ok": true, "message": "Procedure saved.", "procedure": {"id": 1, "title": "Phase2 procedure", "category": "Runbooks", "isProcedure": true, "steps": [{"title": "Step one", "detail": ""}, {"title": "Step two", "detail": ""}], "stepCount": 2, "clientVisible": false, "updatedAt": 1793722291505}}
    """

    const val RUNBOOK_PROCEDURES: String = """
{"ok": true, "procedures": [{"id": 1, "title": "Phase2 procedure", "category": "Runbooks", "isProcedure": true, "steps": [{"title": "Step one", "detail": ""}, {"title": "Step two", "detail": ""}], "stepCount": 2, "clientVisible": false, "updatedAt": 1793722291505}]}
    """

    const val RUNBOOK_RUN_START: String = """
{"ok": true, "message": "Runbook run started.", "id": 1, "run": {"id": 1, "kbId": 1, "ticketId": 1, "clientId": 1, "title": "Phase2 procedure", "steps": [{"title": "Step one", "detail": ""}, {"title": "Step two", "detail": ""}], "stepStates": [{"state": "pending", "by": null, "byName": "", "at": null, "note": ""}, {"state": "pending", "by": null, "byName": "", "at": null, "note": ""}], "status": "active", "doneCount": 0, "totalSteps": 2, "startedBy": 1, "startedByName": "OwnerP2", "startedAt": 1793722291524, "finishedAt": null, "createdAt": 1793722291524, "updatedAt": 1793722291524}}
    """

    const val RUNBOOK_RUNS: String = """
{"ok": true, "runs": [{"id": 1, "kbId": 1, "ticketId": 1, "clientId": 1, "title": "Phase2 procedure", "steps": [{"title": "Step one", "detail": ""}, {"title": "Step two", "detail": ""}], "stepStates": [{"state": "pending", "by": null, "byName": "", "at": null, "note": ""}, {"state": "pending", "by": null, "byName": "", "at": null, "note": ""}], "status": "active", "doneCount": 0, "totalSteps": 2, "startedBy": 1, "startedByName": "OwnerP2", "startedAt": 1793722291524, "finishedAt": null, "createdAt": 1793722291524, "updatedAt": 1793722291524}]}
    """

    const val RUNBOOK_CHECK_STEP: String = """
{"ok": true, "message": "Step checked off.", "completed": false, "run": {"id": 1, "kbId": 1, "ticketId": 1, "clientId": 1, "title": "Phase2 procedure", "steps": [{"title": "Step one", "detail": ""}, {"title": "Step two", "detail": ""}], "stepStates": [{"state": "done", "by": 1, "byName": "OwnerP2", "at": 1793722291539, "note": "done"}, {"state": "pending", "by": null, "byName": "", "at": null, "note": ""}], "status": "active", "doneCount": 1, "totalSteps": 2, "startedBy": 1, "startedByName": "OwnerP2", "startedAt": 1793722291524, "finishedAt": null, "createdAt": 1793722291524, "updatedAt": 1793722291539}}
    """

    const val RUNBOOK_EXTRACTION_STATUS: String = """
{"ok": true, "summary": [], "attachments": []}
    """

    const val SIGNOFF_GET_NONE: String = """
{"ok": true, "ticketId": 1, "ticketStatus": "Open", "completedWithSignOff": false, "signOff": null}
    """

    const val SIGNOFF_POST: String = """
{"ok": true, "message": "Job signed off.", "signOff": {"contractId": 1, "ticketId": 1, "clientId": 1, "title": "Job completion sign-off \u2014 Ticket #1: Phase2 ticket", "summary": "JOB COMPLETION SIGN-OFF\n\nTicket ID: 1\nClient ID: 1\nClient: Phase2 Client LLC\nJob: Phase2 ticket\nCategory: Support\nStatus at sign-off: In Progress\n\nJob summary shown to the signer:\n(no job notes recorded)\n\nPresented by: OwnerP2\nCeremony timestamp (epoch ms): 1793722291567\n\nThe signer confirms the work described above is complete.", "documentHash": "b8887126a31bb526e7eb6d4b20decf66b076534ed0c4fe5deb66c987e4d3b0d7", "status": "signed", "signerName": "Pat P", "signedAt": 1793722291567, "signatureId": 1, "signerIp": "127.0.0.1"}}
    """

    const val SIGNOFF_GET: String = """
{"ok": true, "ticketId": 1, "ticketStatus": "Closed", "completedWithSignOff": true, "signOff": {"contractId": 1, "ticketId": 1, "clientId": 1, "title": "Job completion sign-off \u2014 Ticket #1: Phase2 ticket", "summary": "JOB COMPLETION SIGN-OFF\n\nTicket ID: 1\nClient ID: 1\nClient: Phase2 Client LLC\nJob: Phase2 ticket\nCategory: Support\nStatus at sign-off: In Progress\n\nJob summary shown to the signer:\n(no job notes recorded)\n\nPresented by: OwnerP2\nCeremony timestamp (epoch ms): 1793722291567\n\nThe signer confirms the work described above is complete.", "documentHash": "b8887126a31bb526e7eb6d4b20decf66b076534ed0c4fe5deb66c987e4d3b0d7", "status": "signed", "signerName": "Pat P", "signedAt": 1793722291567, "signatureId": 1, "signerIp": "127.0.0.1"}}
    """

    const val TODAY_PANELS: String = """
{"ok": true, "today": "2026-11-03", "panels": {"cf2Pipeline": {"total": 0, "items": []}, "cf2LaneB": {"total": 0, "items": []}, "cf2Registers": {"warranties": {"total": 0, "within30": {"total": 0, "items": []}, "within60": {"total": 0, "items": []}, "within90": {"total": 0, "items": []}, "items": []}, "licenses": {"total": 0, "within30": {"total": 0, "items": []}, "within60": {"total": 0, "items": []}, "within90": {"total": 0, "items": []}, "items": []}}, "cf2MaintUpcoming": {"total": 0, "items": []}, "cf2RetainerLow": {"total": 0, "items": []}, "cf2BackupOverdue": {"total": 0, "items": []}, "cf2Security": {"hygieneGaps": {"total": 1, "items": [{"clientId": 1, "clientName": "Phase2 Client LLC", "openItems": 8, "noCount": 0, "unknownCount": 8, "yesCount": 0}]}, "failedLogins24h": 0, "retentionConfigured": false}, "cf2Automation": {"total": 0, "items": []}, "cf2StaffAlerts": {"total": 0, "items": [], "certExpired": {"total": 0, "items": []}, "certExpiring": {"total": 0, "items": []}, "serviceDue": {"total": 0, "items": []}}, "cf2FirmOps": {"vendors": {"total": 0, "items": []}, "ownerTasks": {"total": 0, "items": []}, "total": 0}, "cf2Firm": {"total": 0, "items": [], "disposalsInProgress": 0, "activeKbShares": 0, "draftSnapshots": 0, "dueReportSchedules": 0}, "b3Money": {"total": 0, "items": []}, "b3eAutomation": {"staleTickets": {"total": 0, "items": []}, "runawayTimers": {"total": 0, "items": []}, "deviceAlerts": 0, "ticketDrafts": 0, "postmortems": 0, "reviewDrafts": 0, "kbReview": 0, "leadPackets": 0}, "b4jUrgent": {"total": 0, "items": [], "config": {"claimStaleDays": 14, "urgent": {"quietStart": "22:00", "quietEnd": "07:00", "bannerSeverities": ["Critical"], "digestSeverities": []}}, "latencyNote": "How fast this moves: the banner below is computed live every time Today loads, so a new Critical Alert shows on the next Today load. The internal owner-alert draft is written by the automation tick, which runs every 60 seconds by default -- worst case about 60 seconds after the alert is filed."}, "b3fSearchHealth": {"divergent": false, "lastRunDay": null, "checkedAt": null, "modules": []}, "sla": {"breaches": {"total": 0, "items": []}, "atRisk": {"total": 0, "items": []}}, "cfFollowUps": {"total": 0, "items": []}, "lowStock": {"total": 0, "items": []}, "cfRiskReviewDue": {"total": 0, "items": []}, "cfBreachMatches": {"total": 0, "items": []}}, "platformDigest": {"present": false, "digest": null, "ok": true}, "estateHealth": {"present": false, "health": null, "ok": true}}
    """

    const val TODAY_ASK: String = """
{"answered": true, "kind": "count", "entity": "clients", "text": "There are 1 clients.", "data": {"count": 1}, "ok": true}
    """

    const val TODAY_AUTOMATIONS: String = """
{"ok": true, "config": {"expiry_lead_days": 30, "stale_days": 7, "timer_hours": 12, "kb_months": 12, "renewal_horizon_days": 90}, "today": {"staleTickets": {"total": 0, "items": []}, "runawayTimers": {"total": 0, "items": []}, "deviceAlerts": 0, "ticketDrafts": 0, "postmortems": 0, "reviewDrafts": 0, "kbReview": 0, "leadPackets": 0}, "runs": [], "ticketDrafts": [], "ticketNotes": [], "ownerAlerts": [], "kbReview": [], "postmortems": [], "reviewDrafts": [], "leadPackets": []}
    """

    const val TODAY_DIGESTS: String = """
{"digests": [], "ok": true}
    """

    const val TODAY_ESTATE_HEALTH: String = """
{"present": false, "health": null, "ok": true}
    """

    const val TODAY_PLATFORM_DIGEST: String = """
{"present": false, "digest": null, "ok": true}
    """

    const val TODAY_CALENDAR_SYNC: String = """
{"ok": true, "enabled": true, "calendarId": "primary", "prepared": true, "live": false, "helperLastSeen": null, "lastRunMs": null, "lastSummary": "", "linkCounts": {}, "note": "Prepared, not live, until the VM-side helper is installed: the box holds no Google credential (the connector's auth lives in the VM agent runtime only). Sync intent and state are recorded here; the helper carries the actual Google calls. Target is the owner's Google account only."}
    """

    const val TODAY_REPORTS_SUMMARY: String = """
{"ok": true, "reports": {"ok": true, "reports": [], "surveySummary": {"count": 0, "avgRating": null, "recommendPct": null}}, "surveys": {"ok": true, "surveys": [], "summary": {"count": 0, "avgRating": null, "recommendPct": null}}, "referrals": {"ok": true, "referrals": [], "countsByReferrer": {}}, "threatBriefs": {"ok": true, "briefs": []}}
    """

    const val TODAY_STAFF_CERTS: String = """
{"ok": true, "directory": {"staff": [{"id": 1, "displayName": "OwnerP2", "role": "OWNER", "portalLang": "en", "clientId": null, "jobTitle": "", "email": "", "phone": "", "active": true, "createdAt": 1793722291080, "certs": [], "training": {"inProgress": 0, "completed": 0, "abandoned": 0}, "equipment": []}, {"id": 2, "displayName": "StaffP2", "role": "STAFF", "portalLang": "en", "clientId": null, "jobTitle": "Tech", "email": "", "phone": "", "active": true, "createdAt": 1793722291299, "certs": [], "training": {"inProgress": 0, "completed": 0, "abandoned": 0}, "equipment": []}], "total": 2, "ok": true}, "training": {"trainings": [], "counts": {"inProgress": 0, "completed": 0, "abandoned": 0}, "ok": true}}
    """

    const val TODAY_REGISTERS: String = """
{"ok": true, "warranties": {"assetId": null, "clientId": 1, "warranties": [], "ok": true}, "licenses": {"clientId": 1, "licenses": [], "rollup": {"clientId": 1, "licenseCount": 0, "totalSeats": 0, "totalSeatsUsed": 0, "monthlyEquivalentCents": 0, "annualEquivalentCents": 0}, "ok": true}, "todayRegisters": {"warranties": {"total": 0, "within30": {"total": 0, "items": []}, "within60": {"total": 0, "items": []}, "within90": {"total": 0, "items": []}, "items": []}, "licenses": {"total": 0, "within30": {"total": 0, "items": []}, "within60": {"total": 0, "items": []}, "within90": {"total": 0, "items": []}, "items": []}}}
    """

    const val TODAY_URGENT: String = """
{"ok": true, "urgent": {"total": 0, "items": [], "config": {"claimStaleDays": 14, "urgent": {"quietStart": "22:00", "quietEnd": "07:00", "bannerSeverities": ["Critical"], "digestSeverities": []}}, "latencyNote": "How fast this moves: the banner below is computed live every time Today loads, so a new Critical Alert shows on the next Today load. The internal owner-alert draft is written by the automation tick, which runs every 60 seconds by default -- worst case about 60 seconds after the alert is filed."}}
    """

    const val SEARCH: String = """
{"q": "Phase2", "groups": {"clients": [{"id": 1, "title": "Phase2 Client LLC", "subtitle": "Pat P", "type": "clients", "snippet": "Pat P"}], "cases": [{"id": 1, "title": "Phase2 case alpha", "subtitle": "In Progress", "type": "cases", "snippet": "In Progress"}], "tickets": [{"id": 1, "title": "Phase2 ticket", "subtitle": "Closed", "type": "tickets", "snippet": "Closed"}], "assets": [], "knowledge": [{"id": 1, "title": "Phase2 procedure", "subtitle": "Runbooks", "type": "knowledge", "snippet": "Runbooks"}], "invoices": [], "quotes": [], "engagements": [{"id": 1, "title": "Phase2 engagement", "subtitle": "Planned", "type": "engagements", "snippet": "Planned"}], "law": []}, "ok": true}
    """

    const val CASE_ARCHIVE: String = """
{"ok": true, "message": "Case archived.", "case": {"id": 1, "clientId": 1, "assignedUserId": null, "title": "Phase2 case alpha", "status": "In Progress", "priority": "Critical", "category": "Support", "dueDate": "2020-01-01", "openedDate": "", "closedDate": "", "notes": "alpha", "adMaterial": false, "archivedAt": 1793722291698, "archivedBy": 1, "createdAt": 1793722291411, "updatedAt": 1793722291698, "clientName": "Phase2 Client LLC", "ticketCount": 1}}
    """

    const val CASE_RESTORE: String = """
{"ok": true, "message": "Case restored.", "case": {"id": 1, "clientId": 1, "assignedUserId": null, "title": "Phase2 case alpha", "status": "In Progress", "priority": "Critical", "category": "Support", "dueDate": "2020-01-01", "openedDate": "", "closedDate": "", "notes": "alpha", "adMaterial": false, "archivedAt": null, "archivedBy": null, "createdAt": 1793722291411, "updatedAt": 1793722291703, "clientName": "Phase2 Client LLC", "ticketCount": 1}}
    """

    const val JOB_DELETE: String = """
{"ok": true, "message": "Job deleted."}
    """

}
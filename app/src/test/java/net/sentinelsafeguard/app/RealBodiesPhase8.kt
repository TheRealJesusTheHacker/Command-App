package net.sentinelsafeguard.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Phase 8A REAL response bodies, captured 2026-11-03 from a
 * rehearsal boot of the exact Phase 8A candidate bytes
 * (server.py sha256 f12d215e87e6f689a2de1716190effff4d9efdc5887025a861721f6b8d72551d,
 * Addendum 24 — the deployed production bytes), against a seeded
 * scratch database. Seed data is fictional; the one-time secret
 * value appears in NO body (asserted at capture); the evidence-pack
 * pdfBase64 is recorded truncated to prefix+length+sha256.
 * [ALL] is phase8a-work/fixtures/real-bodies.json verbatim,
 * re-keyed from capture names to route names.
 */
object RealBodiesPhase8 {
    val ALL: String = """{
 "_meta": {
  "serverPySha256": "f12d215e87e6f689a2de1716190effff4d9efdc5887025a861721f6b8d72551d",
  "capturedFrom": "rehearsal boot of the exact Phase 8A candidate bytes (deployed production, Addendum 24)",
  "note": "Real bodies captured from the exact candidate bytes on a residue-zero scratch DB; seed data fictional; the one-time secret value appears in NO body (asserted at capture); evidence-pack pdfBase64 truncated to prefix+length+sha256; verification codes redacted to shape."
 },
 "bodies": {
  "POST /api/app/portal-settings/sub-assignments": {
   "method": "POST",
   "path": "/api/app/portal-settings/sub-assignments",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Subcontractor assigned."
   }
  },
  "GET /api/app/portal-settings/sub-assignments": {
   "method": "GET",
   "path": "/api/app/portal-settings/sub-assignments",
   "status": 200,
   "body": {
    "assignments": [
     {
      "jobId": 1,
      "userId": 3,
      "userName": "SubCap8A",
      "jobTitle": "Capture site job"
     }
    ],
    "subcontractors": [
     {
      "id": 3,
      "displayName": "SubCap8A"
     }
    ],
    "ok": true
   }
  },
  "POST /api/app/portal-settings/sub-assignments/unassign": {
   "method": "POST",
   "path": "/api/app/portal-settings/sub-assignments/unassign",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Subcontractor unassigned."
   }
  },
  "POST /api/app/portal-settings/secrets": {
   "method": "POST",
   "path": "/api/app/portal-settings/secrets",
   "status": 201,
   "body": {
    "ok": true,
    "id": 1,
    "message": "Secret created \u2014 the client can view it once in their portal."
   }
  },
  "GET /api/app/portal-settings/secrets?clientId=1": {
   "method": "GET",
   "path": "/api/app/portal-settings/secrets?clientId=1",
   "status": 200,
   "body": {
    "secrets": [
     {
      "id": 1,
      "clientId": 1,
      "label": "Capture mailbox secret",
      "state": "pending",
      "createdAt": 1793757970817,
      "expiresAt": 1793844370817,
      "viewedAt": null
     }
    ],
    "ok": true
   }
  },
  "GET /api/app/portal-settings/one-pager?clientId=1": {
   "method": "GET",
   "path": "/api/app/portal-settings/one-pager?clientId=1",
   "status": 200,
   "body": {
    "onepager": {
     "clientId": 1,
     "legalName": "Capture Fixture LLC",
     "dba": "",
     "services": [
      {
       "name": "Support",
       "total": 1,
       "open": 1
      }
     ],
     "posture": {
      "state": "scored",
      "score": 42,
      "baseline": 40,
      "factors": [
       {
        "label": "Activity in the last 90 days",
        "points": 5
       },
       {
        "label": "1 open normal-priority ticket",
        "points": -3
       },
       {
        "label": "No assets on file",
        "points": 0
       },
       {
        "label": "0 of 1 tickets and cases resolved or closed",
        "points": 0
       },
       {
        "label": "No form records on file",
        "points": 0
       }
      ],
      "counts": {
       "assets": 0,
       "openTickets": 1,
       "openCases": 0,
       "closedItems": 0,
       "totalItems": 1,
       "overdueInvoices": 0,
       "forms": 0
      }
     },
     "contacts": [
      {
       "name": "Cal Capture",
       "title": "",
       "email": "cal@capture8a.example.invalid",
       "phone": "555-0108"
      }
     ],
     "nextSteps": [
      {
       "title": "Open ticket: Capture costing ticket",
       "detail": "Open"
      }
     ],
     "generatedAt": 1793757971130,
     "lang": "en",
     "labels": {
      "title": "Service summary",
      "services": "Services",
      "posture": "Posture",
      "contacts": "Contacts",
      "nextSteps": "Next steps",
      "noServices": "No services on file yet.",
      "noSteps": "No open next steps.",
      "itemsOpen": "{total} item(s) \u00b7 {open} open",
      "scoreLine": "Score: {score} / 100",
      "notScored": "Not enough information to score yet.",
      "openTicketPrefix": "Open ticket: "
     },
     "documentText": "Sentinel Safeguard LLC \u2014 Service summary\nCapture Fixture LLC\n\nServices\nSupport: 1 item(s) \u00b7 1 open\n\nPosture\nScore: 42 / 100\n\nContacts\nCal Capture \u00b7 cal@capture8a.example.invalid \u00b7 555-0108\n\nNext steps\n\u2022 Open ticket: Capture costing ticket \u2014 Open"
    },
    "ok": true
   }
  },
  "GET /api/app/portal-settings/one-pager?clientId=999999": {
   "method": "GET",
   "path": "/api/app/portal-settings/one-pager?clientId=999999",
   "status": 200,
   "body": {
    "onepager": null,
    "ok": true
   }
  },
  "POST /api/app/portal-settings/tips": {
   "method": "POST",
   "path": "/api/app/portal-settings/tips",
   "status": 201,
   "body": {
    "ok": true,
    "id": 1,
    "message": "Tip saved."
   }
  },
  "GET /api/app/portal-settings/tips": {
   "method": "GET",
   "path": "/api/app/portal-settings/tips",
   "status": 200,
   "body": {
    "tips": [
     {
      "id": 1,
      "title": "Capture tip",
      "body": "Capture tip body.",
      "active": true,
      "sortOrder": 1,
      "createdAt": 1793757971334,
      "updatedAt": 1793757971334
     }
    ],
    "currentTipId": 1,
    "canWrite": true,
    "ok": true
   }
  },
  "PATCH /api/app/portal-settings/tips/1": {
   "method": "PATCH",
   "path": "/api/app/portal-settings/tips/1",
   "status": 200,
   "body": {
    "ok": true,
    "id": 1,
    "message": "Tip saved."
   }
  },
  "GET /api/app/portal-settings/current-tip": {
   "method": "GET",
   "path": "/api/app/portal-settings/current-tip",
   "status": 200,
   "body": {
    "tip": {
     "title": "Capture tip (edited)",
     "body": "Capture tip body."
    },
    "ok": true
   }
  },
  "DELETE /api/app/portal-settings/tips/1": {
   "method": "DELETE",
   "path": "/api/app/portal-settings/tips/1",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Tip deleted."
   }
  },
  "GET /api/app/portal-settings/sub-assignments (staff)": {
   "method": "GET",
   "path": "/api/app/portal-settings/sub-assignments",
   "status": 200,
   "body": {
    "assignments": [],
    "subcontractors": [
     {
      "id": 3,
      "displayName": "SubCap8A"
     }
    ],
    "ok": true
   }
  },
  "GET /api/app/portal-settings/tips (staff 403)": {
   "method": "GET",
   "path": "/api/app/portal-settings/tips",
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Not authorized."
   }
  },
  "GET /api/app/firm-ops/job-costs": {
   "method": "GET",
   "path": "/api/app/firm-ops/job-costs",
   "status": 200,
   "body": {
    "ok": true,
    "jobs": [
     {
      "kind": "ticket",
      "id": 1,
      "title": "Capture costing ticket",
      "clientId": 1,
      "clientName": "Capture Fixture LLC",
      "status": "Open",
      "invoicedCents": 0,
      "invoiceIds": [],
      "invoiceNumbers": [],
      "revenueNote": "No invoice is linked to this job through billed time entries, so invoiced revenue reads 0.",
      "timeEntries": 1,
      "runningEntries": 0,
      "timeSeconds": 1800,
      "timeHours": 0.5,
      "timeCostCents": 4500,
      "expenseTotalCents": 0,
      "expenseCount": 0,
      "partsTotalCents": 0,
      "partsMovementCount": 0,
      "rated": true,
      "rateState": "rated",
      "unratedEntries": 0,
      "totalCostCents": 4500,
      "knownCostCents": 4500,
      "marginCents": -4500,
      "marginPercent": null,
      "marginNote": ""
     }
    ],
    "unratedCount": 0,
    "note": "Job costing is derived live from tracked time, expenses, consumed parts, and the invoices linked through billed time. Unrated jobs show no margin \u2014 no rate is ever invented."
   }
  },
  "GET /api/app/firm-ops/job-costing?kind=ticket&id=1": {
   "method": "GET",
   "path": "/api/app/firm-ops/job-costing?kind=ticket&id=1",
   "status": 200,
   "body": {
    "ok": true,
    "costing": {
     "kind": "ticket",
     "id": 1,
     "title": "Capture costing ticket",
     "clientId": 1,
     "clientName": "Capture Fixture LLC",
     "status": "Open",
     "invoicedCents": 0,
     "invoiceIds": [],
     "invoiceNumbers": [],
     "revenueNote": "No invoice is linked to this job through billed time entries, so invoiced revenue reads 0.",
     "timeEntries": 1,
     "runningEntries": 0,
     "timeSeconds": 1800,
     "timeHours": 0.5,
     "timeCostCents": 4500,
     "expenseTotalCents": 0,
     "expenseCount": 0,
     "partsTotalCents": 0,
     "partsMovementCount": 0,
     "rated": true,
     "rateState": "rated",
     "unratedEntries": 0,
     "totalCostCents": 4500,
     "knownCostCents": 4500,
     "marginCents": -4500,
     "marginPercent": null,
     "marginNote": ""
    }
   }
  },
  "GET /api/app/firm-ops/job-costs (staff 403)": {
   "method": "GET",
   "path": "/api/app/firm-ops/job-costs",
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Owner only."
   }
  },
  "POST /api/app/firm-ops/vendors": {
   "method": "POST",
   "path": "/api/app/firm-ops/vendors",
   "status": 201,
   "body": {
    "ok": true,
    "id": 1,
    "vendor": {
     "id": 1,
     "name": "Capture Vendor Co",
     "contactName": "",
     "contactEmail": "ops@capturevendor.example.invalid",
     "contactPhone": "",
     "service": "Capture hosting",
     "contractStart": "",
     "renewalDate": "2027-02-01",
     "costNote": "capture rate",
     "notes": "",
     "createdBy": 1,
     "createdAt": 1793757972513,
     "updatedAt": 1793757972513,
     "daysUntilRenewal": 90,
     "renewalBucket": "90"
    }
   }
  },
  "PATCH /api/app/firm-ops/vendors/1": {
   "method": "PATCH",
   "path": "/api/app/firm-ops/vendors/1",
   "status": 200,
   "body": {
    "ok": true,
    "id": 1,
    "vendor": {
     "id": 1,
     "name": "Capture Vendor Co II",
     "contactName": "",
     "contactEmail": "ops@capturevendor.example.invalid",
     "contactPhone": "",
     "service": "Capture hosting",
     "contractStart": "",
     "renewalDate": "2027-02-01",
     "costNote": "capture rate",
     "notes": "",
     "createdBy": 1,
     "createdAt": 1793757972513,
     "updatedAt": 1793757972696,
     "daysUntilRenewal": 90,
     "renewalBucket": "90"
    }
   }
  },
  "GET /api/app/firm-ops/vendors": {
   "method": "GET",
   "path": "/api/app/firm-ops/vendors",
   "status": 200,
   "body": {
    "ok": true,
    "vendors": [
     {
      "id": 1,
      "name": "Capture Vendor Co II",
      "contactName": "",
      "contactEmail": "ops@capturevendor.example.invalid",
      "contactPhone": "",
      "service": "Capture hosting",
      "contractStart": "",
      "renewalDate": "2027-02-01",
      "costNote": "capture rate",
      "notes": "",
      "createdBy": 1,
      "createdAt": 1793757972513,
      "updatedAt": 1793757972696,
      "daysUntilRenewal": 90,
      "renewalBucket": "90"
     }
    ],
    "internalOnly": true,
    "note": "Sentinel's own vendors. Internal only \u2014 this directory is never shown to clients."
   }
  },
  "DELETE /api/app/firm-ops/vendors/1": {
   "method": "DELETE",
   "path": "/api/app/firm-ops/vendors/1",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Vendor deleted.",
    "id": 1
   }
  },
  "POST /api/app/firm-ops/tasks": {
   "method": "POST",
   "path": "/api/app/firm-ops/tasks",
   "status": 201,
   "body": {
    "ok": true,
    "id": 1,
    "task": {
     "id": 1,
     "title": "Capture owner task",
     "detail": "Internal capture.",
     "dueDate": "",
     "priority": "High",
     "state": "open",
     "clientId": 1,
     "clientName": "Capture Fixture LLC",
     "linkedTicketId": null,
     "createdBy": 1,
     "createdAt": 1793757973134,
     "updatedAt": 1793757973134,
     "completedAt": null
    }
   }
  },
  "PATCH /api/app/firm-ops/tasks/1": {
   "method": "PATCH",
   "path": "/api/app/firm-ops/tasks/1",
   "status": 200,
   "body": {
    "ok": true,
    "id": 1,
    "task": {
     "id": 1,
     "title": "Capture owner task v2",
     "detail": "Internal capture.",
     "dueDate": "",
     "priority": "High",
     "state": "open",
     "clientId": null,
     "clientName": "",
     "linkedTicketId": null,
     "createdBy": 1,
     "createdAt": 1793757973134,
     "updatedAt": 1793757973293,
     "completedAt": null
    }
   }
  },
  "GET /api/app/firm-ops/tasks": {
   "method": "GET",
   "path": "/api/app/firm-ops/tasks",
   "status": 200,
   "body": {
    "ok": true,
    "tasks": [
     {
      "id": 1,
      "title": "Capture owner task v2",
      "detail": "Internal capture.",
      "dueDate": "",
      "priority": "High",
      "state": "open",
      "clientId": null,
      "clientName": "",
      "linkedTicketId": null,
      "createdBy": 1,
      "createdAt": 1793757973134,
      "updatedAt": 1793757973293,
      "completedAt": null
     }
    ],
    "states": [
     "open",
     "done",
     "dropped"
    ],
    "note": "Internal operations tasks. A task is not a ticket and is never shown to clients; converting one creates a NEW ticket and links it here."
   }
  },
  "POST /api/app/firm-ops/tasks/1/convert": {
   "method": "POST",
   "path": "/api/app/firm-ops/tasks/1/convert",
   "status": 200,
   "body": {
    "ok": true,
    "id": 1,
    "taskId": 1,
    "ticketId": 2,
    "message": "Ticket #2 created from the task; the task is marked done and linked to it."
   }
  },
  "POST /api/app/firm-ops/tasks (flat)": {
   "method": "POST",
   "path": "/api/app/firm-ops/tasks",
   "status": 201,
   "body": {
    "ok": true,
    "id": 2,
    "task": {
     "id": 2,
     "title": "Capture task two",
     "detail": "",
     "dueDate": "",
     "priority": "Normal",
     "state": "open",
     "clientId": null,
     "clientName": "",
     "linkedTicketId": null,
     "createdBy": 1,
     "createdAt": 1793757973720,
     "updatedAt": 1793757973720,
     "completedAt": null
    }
   }
  },
  "POST /api/app/firm-ops/tasks/2/state": {
   "method": "POST",
   "path": "/api/app/firm-ops/tasks/2/state",
   "status": 200,
   "body": {
    "ok": true,
    "id": 2,
    "task": {
     "id": 2,
     "title": "Capture task two",
     "detail": "",
     "dueDate": "",
     "priority": "Normal",
     "state": "done",
     "clientId": null,
     "clientName": "",
     "linkedTicketId": null,
     "createdBy": 1,
     "createdAt": 1793757973720,
     "updatedAt": 1793757973889,
     "completedAt": 1793757973889
    }
   }
  },
  "POST /api/app/firm-ops/availability": {
   "method": "POST",
   "path": "/api/app/firm-ops/availability",
   "status": 201,
   "body": {
    "ok": true,
    "id": 1,
    "record": {
     "id": 1,
     "userId": 2,
     "userName": "StaffCap8A",
     "dateFrom": "2026-12-01",
     "dateTo": "2026-12-02",
     "status": "unavailable",
     "note": "Capture leave",
     "createdBy": 1,
     "createdAt": 1793757974128,
     "updatedAt": 1793757974128
    }
   }
  },
  "PATCH /api/app/firm-ops/availability/1": {
   "method": "PATCH",
   "path": "/api/app/firm-ops/availability/1",
   "status": 200,
   "body": {
    "ok": true,
    "id": 1,
    "record": {
     "id": 1,
     "userId": 2,
     "userName": "StaffCap8A",
     "dateFrom": "2026-12-01",
     "dateTo": "2026-12-02",
     "status": "tentative",
     "note": "Capture leave",
     "createdBy": 1,
     "createdAt": 1793757974128,
     "updatedAt": 1793757974369
    }
   }
  },
  "GET /api/app/firm-ops/availability": {
   "method": "GET",
   "path": "/api/app/firm-ops/availability",
   "status": 200,
   "body": {
    "ok": true,
    "records": [
     {
      "id": 1,
      "userId": 2,
      "userName": "StaffCap8A",
      "dateFrom": "2026-12-01",
      "dateTo": "2026-12-02",
      "status": "tentative",
      "note": "Capture leave",
      "createdBy": 1,
      "createdAt": 1793757974128,
      "updatedAt": 1793757974369
     }
    ],
    "statuses": [
     "available",
     "unavailable",
     "tentative"
    ],
    "bookingDayWarnings": [],
    "staff": [
     {
      "id": 1,
      "displayName": "OwnerCap8A"
     },
     {
      "id": 2,
      "displayName": "StaffCap8A"
     },
     {
      "id": 3,
      "displayName": "SubCap8A"
     }
    ]
   }
  },
  "DELETE /api/app/firm-ops/availability/1": {
   "method": "DELETE",
   "path": "/api/app/firm-ops/availability/1",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Availability record deleted.",
    "id": 1
   }
  },
  "GET /api/app/firm-ops/booking-warnings": {
   "method": "GET",
   "path": "/api/app/firm-ops/booking-warnings",
   "status": 200,
   "body": {
    "ok": true,
    "warnings": [],
    "note": "Warnings only: staff availability never blocks a booking slot \u2014 #9 bookings are not staff-assigned, so unavailability is surfaced for owner review instead."
   }
  },
  "GET /api/app/verified-desk/assessments": {
   "method": "GET",
   "path": "/api/app/verified-desk/assessments",
   "status": 200,
   "body": {
    "ok": true,
    "assessments": [],
    "verifications": [],
    "controlSet": [
     {
      "key": "mfa_enforced",
      "family": "Identity & Access",
      "label": "Multi-factor authentication enforced for email, remote access and admin accounts",
      "weight": 3
     },
     {
      "key": "access_least_privilege",
      "family": "Identity & Access",
      "label": "Least-privilege access: admin accounts separated, access reviewed on a cadence",
      "weight": 2
     },
     {
      "key": "patching_current",
      "family": "Patching",
      "label": "Operating systems and applications patched on a defined cadence",
      "weight": 3
     },
     {
      "key": "vuln_management",
      "family": "Patching",
      "label": "Known vulnerabilities scanned for and remediated",
      "weight": 2
     },
     {
      "key": "backups_running",
      "family": "Backups & Recovery",
      "label": "Backups run on schedule and are isolated or immutable",
      "weight": 3
     },
     {
      "key": "restore_tested",
      "family": "Backups & Recovery",
      "label": "Backup restores are tested, not just scheduled",
      "weight": 2
     },
     {
      "key": "endpoint_protection",
      "family": "Endpoint",
      "label": "Endpoint protection installed and current on all devices",
      "weight": 3
     },
     {
      "key": "device_encryption",
      "family": "Endpoint",
      "label": "Full-disk encryption enabled on endpoints",
      "weight": 2
     },
     {
      "key": "email_security_records",
      "family": "Email Security",
      "label": "SPF, DKIM and DMARC published and enforced for the domain",
      "weight": 2
     },
     {
      "key": "phishing_defense",
      "family": "Email Security",
      "label": "Anti-phishing filtering and a user reporting path in place",
      "weight": 1
     },
     {
      "key": "firewall_network",
      "family": "Network",
      "label": "Firewall in place; unused services and remote access off by default",
      "weight": 2
     },
     {
      "key": "logging_monitoring",
      "family": "Monitoring",
      "label": "Security logs collected and reviewed",
      "weight": 1
     },
     {
      "key": "incident_plan",
      "family": "Response",
      "label": "Incident response plan exists and is findable",
      "weight": 1
     },
     {
      "key": "security_training",
      "family": "People",
      "label": "Staff receive security awareness training",
      "weight": 1
     }
    ],
    "categories": [
     "Security Assessment",
     "Penetration Testing",
     "Security Installs",
     "Incident Response",
     "Cybersecurity Consulting",
     "IT Services"
    ],
    "badgeMeaning": "Assessed by Sentinel Safeguard against its published checklist on the date shown. This is not a certification, and Sentinel Safeguard is not an accrediting body.",
    "canWrite": true,
    "isOwner": true
   }
  },
  "POST /api/app/verified-desk/assessments": {
   "method": "POST",
   "path": "/api/app/verified-desk/assessments",
   "status": 201,
   "body": {
    "ok": true,
    "message": "Assessment started.",
    "assessment": {
     "id": 1,
     "clientId": 1,
     "title": "Verified assessment -- Capture Fixture LLC",
     "category": "Security Assessment",
     "status": "draft",
     "score": null,
     "liveScore": null,
     "scoreBreakdown": {
      "formula": "score = round(100 * sum(weight * points) / sum(weight), 1) over controls not marked na; points: pass=1, partial=0.5, fail=0; na controls are excluded from both sums",
      "earned": 0.0,
      "possible": 0.0,
      "counts": {
       "pass": 0,
       "partial": 0,
       "fail": 0,
       "na": 14
      },
      "controls": [
       {
        "key": "mfa_enforced",
        "status": "na",
        "weight": 3.0,
        "points": null
       },
       {
        "key": "access_least_privilege",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "patching_current",
        "status": "na",
        "weight": 3.0,
        "points": null
       },
       {
        "key": "vuln_management",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "backups_running",
        "status": "na",
        "weight": 3.0,
        "points": null
       },
       {
        "key": "restore_tested",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "endpoint_protection",
        "status": "na",
        "weight": 3.0,
        "points": null
       },
       {
        "key": "device_encryption",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "email_security_records",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "phishing_defense",
        "status": "na",
        "weight": 1.0,
        "points": null
       },
       {
        "key": "firewall_network",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "logging_monitoring",
        "status": "na",
        "weight": 1.0,
        "points": null
       },
       {
        "key": "incident_plan",
        "status": "na",
        "weight": 1.0,
        "points": null
       },
       {
        "key": "security_training",
        "status": "na",
        "weight": 1.0,
        "points": null
       }
      ]
     },
     "storedScoreJson": "{}",
     "verificationId": null,
     "verification": null,
     "badgeMeaning": "Assessed by Sentinel Safeguard against its published checklist on the date shown. This is not a certification, and Sentinel Safeguard is not an accrediting body.",
     "createdBy": 1,
     "reviewedBy": null,
     "createdAt": 1793757974829,
     "updatedAt": 1793757974829,
     "completedAt": null,
     "controls": [
      {
       "key": "mfa_enforced",
       "label": "Multi-factor authentication enforced for email, remote access and admin accounts",
       "family": "Identity & Access",
       "weight": 3.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "access_least_privilege",
       "label": "Least-privilege access: admin accounts separated, access reviewed on a cadence",
       "family": "Identity & Access",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "patching_current",
       "label": "Operating systems and applications patched on a defined cadence",
       "family": "Patching",
       "weight": 3.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "vuln_management",
       "label": "Known vulnerabilities scanned for and remediated",
       "family": "Patching",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "backups_running",
       "label": "Backups run on schedule and are isolated or immutable",
       "family": "Backups & Recovery",
       "weight": 3.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "restore_tested",
       "label": "Backup restores are tested, not just scheduled",
       "family": "Backups & Recovery",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "endpoint_protection",
       "label": "Endpoint protection installed and current on all devices",
       "family": "Endpoint",
       "weight": 3.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "device_encryption",
       "label": "Full-disk encryption enabled on endpoints",
       "family": "Endpoint",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "email_security_records",
       "label": "SPF, DKIM and DMARC published and enforced for the domain",
       "family": "Email Security",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "phishing_defense",
       "label": "Anti-phishing filtering and a user reporting path in place",
       "family": "Email Security",
       "weight": 1.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "firewall_network",
       "label": "Firewall in place; unused services and remote access off by default",
       "family": "Network",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "logging_monitoring",
       "label": "Security logs collected and reviewed",
       "family": "Monitoring",
       "weight": 1.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "incident_plan",
       "label": "Incident response plan exists and is findable",
       "family": "Response",
       "weight": 1.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "security_training",
       "label": "Staff receive security awareness training",
       "family": "People",
       "weight": 1.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      }
     ]
    }
   }
  },
  "GET /api/app/verified-desk/assessments/1": {
   "method": "GET",
   "path": "/api/app/verified-desk/assessments/1",
   "status": 200,
   "body": {
    "assessment": {
     "id": 1,
     "clientId": 1,
     "title": "Verified assessment -- Capture Fixture LLC",
     "category": "Security Assessment",
     "status": "draft",
     "score": null,
     "liveScore": null,
     "scoreBreakdown": {
      "formula": "score = round(100 * sum(weight * points) / sum(weight), 1) over controls not marked na; points: pass=1, partial=0.5, fail=0; na controls are excluded from both sums",
      "earned": 0.0,
      "possible": 0.0,
      "counts": {
       "pass": 0,
       "partial": 0,
       "fail": 0,
       "na": 14
      },
      "controls": [
       {
        "key": "mfa_enforced",
        "status": "na",
        "weight": 3.0,
        "points": null
       },
       {
        "key": "access_least_privilege",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "patching_current",
        "status": "na",
        "weight": 3.0,
        "points": null
       },
       {
        "key": "vuln_management",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "backups_running",
        "status": "na",
        "weight": 3.0,
        "points": null
       },
       {
        "key": "restore_tested",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "endpoint_protection",
        "status": "na",
        "weight": 3.0,
        "points": null
       },
       {
        "key": "device_encryption",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "email_security_records",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "phishing_defense",
        "status": "na",
        "weight": 1.0,
        "points": null
       },
       {
        "key": "firewall_network",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "logging_monitoring",
        "status": "na",
        "weight": 1.0,
        "points": null
       },
       {
        "key": "incident_plan",
        "status": "na",
        "weight": 1.0,
        "points": null
       },
       {
        "key": "security_training",
        "status": "na",
        "weight": 1.0,
        "points": null
       }
      ]
     },
     "storedScoreJson": "{}",
     "verificationId": null,
     "verification": null,
     "badgeMeaning": "Assessed by Sentinel Safeguard against its published checklist on the date shown. This is not a certification, and Sentinel Safeguard is not an accrediting body.",
     "createdBy": 1,
     "reviewedBy": null,
     "createdAt": 1793757974829,
     "updatedAt": 1793757974829,
     "completedAt": null,
     "controls": [
      {
       "key": "mfa_enforced",
       "label": "Multi-factor authentication enforced for email, remote access and admin accounts",
       "family": "Identity & Access",
       "weight": 3.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "access_least_privilege",
       "label": "Least-privilege access: admin accounts separated, access reviewed on a cadence",
       "family": "Identity & Access",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "patching_current",
       "label": "Operating systems and applications patched on a defined cadence",
       "family": "Patching",
       "weight": 3.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "vuln_management",
       "label": "Known vulnerabilities scanned for and remediated",
       "family": "Patching",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "backups_running",
       "label": "Backups run on schedule and are isolated or immutable",
       "family": "Backups & Recovery",
       "weight": 3.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "restore_tested",
       "label": "Backup restores are tested, not just scheduled",
       "family": "Backups & Recovery",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "endpoint_protection",
       "label": "Endpoint protection installed and current on all devices",
       "family": "Endpoint",
       "weight": 3.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "device_encryption",
       "label": "Full-disk encryption enabled on endpoints",
       "family": "Endpoint",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "email_security_records",
       "label": "SPF, DKIM and DMARC published and enforced for the domain",
       "family": "Email Security",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "phishing_defense",
       "label": "Anti-phishing filtering and a user reporting path in place",
       "family": "Email Security",
       "weight": 1.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "firewall_network",
       "label": "Firewall in place; unused services and remote access off by default",
       "family": "Network",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "logging_monitoring",
       "label": "Security logs collected and reviewed",
       "family": "Monitoring",
       "weight": 1.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "incident_plan",
       "label": "Incident response plan exists and is findable",
       "family": "Response",
       "weight": 1.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "security_training",
       "label": "Staff receive security awareness training",
       "family": "People",
       "weight": 1.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      }
     ]
    },
    "ok": true
   }
  },
  "POST /api/app/verified-desk/assessments/1/save": {
   "method": "POST",
   "path": "/api/app/verified-desk/assessments/1/save",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Checklist saved.",
    "assessment": {
     "id": 1,
     "clientId": 1,
     "title": "Verified assessment -- Capture Fixture LLC",
     "category": "Security Assessment",
     "status": "draft",
     "score": 100.0,
     "liveScore": 100.0,
     "scoreBreakdown": {
      "formula": "score = round(100 * sum(weight * points) / sum(weight), 1) over controls not marked na; points: pass=1, partial=0.5, fail=0; na controls are excluded from both sums",
      "earned": 3.0,
      "possible": 3.0,
      "counts": {
       "pass": 1,
       "partial": 0,
       "fail": 0,
       "na": 13
      },
      "controls": [
       {
        "key": "mfa_enforced",
        "status": "pass",
        "weight": 3.0,
        "points": 1.0
       },
       {
        "key": "access_least_privilege",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "patching_current",
        "status": "na",
        "weight": 3.0,
        "points": null
       },
       {
        "key": "vuln_management",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "backups_running",
        "status": "na",
        "weight": 3.0,
        "points": null
       },
       {
        "key": "restore_tested",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "endpoint_protection",
        "status": "na",
        "weight": 3.0,
        "points": null
       },
       {
        "key": "device_encryption",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "email_security_records",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "phishing_defense",
        "status": "na",
        "weight": 1.0,
        "points": null
       },
       {
        "key": "firewall_network",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "logging_monitoring",
        "status": "na",
        "weight": 1.0,
        "points": null
       },
       {
        "key": "incident_plan",
        "status": "na",
        "weight": 1.0,
        "points": null
       },
       {
        "key": "security_training",
        "status": "na",
        "weight": 1.0,
        "points": null
       }
      ]
     },
     "storedScoreJson": "{}",
     "verificationId": null,
     "verification": null,
     "badgeMeaning": "Assessed by Sentinel Safeguard against its published checklist on the date shown. This is not a certification, and Sentinel Safeguard is not an accrediting body.",
     "createdBy": 1,
     "reviewedBy": null,
     "createdAt": 1793757974829,
     "updatedAt": 1793757975148,
     "completedAt": null,
     "controls": [
      {
       "key": "mfa_enforced",
       "label": "Multi-factor authentication enforced for email, remote access and admin accounts",
       "family": "Identity & Access",
       "weight": 3.0,
       "status": "pass",
       "evidenceNote": "Capture evidence.",
       "updatedAt": 1793757975148
      },
      {
       "key": "access_least_privilege",
       "label": "Least-privilege access: admin accounts separated, access reviewed on a cadence",
       "family": "Identity & Access",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "patching_current",
       "label": "Operating systems and applications patched on a defined cadence",
       "family": "Patching",
       "weight": 3.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "vuln_management",
       "label": "Known vulnerabilities scanned for and remediated",
       "family": "Patching",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "backups_running",
       "label": "Backups run on schedule and are isolated or immutable",
       "family": "Backups & Recovery",
       "weight": 3.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "restore_tested",
       "label": "Backup restores are tested, not just scheduled",
       "family": "Backups & Recovery",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "endpoint_protection",
       "label": "Endpoint protection installed and current on all devices",
       "family": "Endpoint",
       "weight": 3.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "device_encryption",
       "label": "Full-disk encryption enabled on endpoints",
       "family": "Endpoint",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "email_security_records",
       "label": "SPF, DKIM and DMARC published and enforced for the domain",
       "family": "Email Security",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "phishing_defense",
       "label": "Anti-phishing filtering and a user reporting path in place",
       "family": "Email Security",
       "weight": 1.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "firewall_network",
       "label": "Firewall in place; unused services and remote access off by default",
       "family": "Network",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "logging_monitoring",
       "label": "Security logs collected and reviewed",
       "family": "Monitoring",
       "weight": 1.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "incident_plan",
       "label": "Incident response plan exists and is findable",
       "family": "Response",
       "weight": 1.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "security_training",
       "label": "Staff receive security awareness training",
       "family": "People",
       "weight": 1.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      }
     ]
    }
   }
  },
  "POST /api/app/verified-desk/assessments/1/state (in_review)": {
   "method": "POST",
   "path": "/api/app/verified-desk/assessments/1/state",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Assessment in_review.",
    "assessment": {
     "id": 1,
     "clientId": 1,
     "title": "Verified assessment -- Capture Fixture LLC",
     "category": "Security Assessment",
     "status": "in_review",
     "score": 100.0,
     "liveScore": 100.0,
     "scoreBreakdown": {
      "formula": "score = round(100 * sum(weight * points) / sum(weight), 1) over controls not marked na; points: pass=1, partial=0.5, fail=0; na controls are excluded from both sums",
      "earned": 3.0,
      "possible": 3.0,
      "counts": {
       "pass": 1,
       "partial": 0,
       "fail": 0,
       "na": 13
      },
      "controls": [
       {
        "key": "mfa_enforced",
        "status": "pass",
        "weight": 3.0,
        "points": 1.0
       },
       {
        "key": "access_least_privilege",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "patching_current",
        "status": "na",
        "weight": 3.0,
        "points": null
       },
       {
        "key": "vuln_management",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "backups_running",
        "status": "na",
        "weight": 3.0,
        "points": null
       },
       {
        "key": "restore_tested",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "endpoint_protection",
        "status": "na",
        "weight": 3.0,
        "points": null
       },
       {
        "key": "device_encryption",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "email_security_records",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "phishing_defense",
        "status": "na",
        "weight": 1.0,
        "points": null
       },
       {
        "key": "firewall_network",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "logging_monitoring",
        "status": "na",
        "weight": 1.0,
        "points": null
       },
       {
        "key": "incident_plan",
        "status": "na",
        "weight": 1.0,
        "points": null
       },
       {
        "key": "security_training",
        "status": "na",
        "weight": 1.0,
        "points": null
       }
      ]
     },
     "storedScoreJson": "{}",
     "verificationId": null,
     "verification": null,
     "badgeMeaning": "Assessed by Sentinel Safeguard against its published checklist on the date shown. This is not a certification, and Sentinel Safeguard is not an accrediting body.",
     "createdBy": 1,
     "reviewedBy": null,
     "createdAt": 1793757974829,
     "updatedAt": 1793757975339,
     "completedAt": null,
     "controls": [
      {
       "key": "mfa_enforced",
       "label": "Multi-factor authentication enforced for email, remote access and admin accounts",
       "family": "Identity & Access",
       "weight": 3.0,
       "status": "pass",
       "evidenceNote": "Capture evidence.",
       "updatedAt": 1793757975148
      },
      {
       "key": "access_least_privilege",
       "label": "Least-privilege access: admin accounts separated, access reviewed on a cadence",
       "family": "Identity & Access",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "patching_current",
       "label": "Operating systems and applications patched on a defined cadence",
       "family": "Patching",
       "weight": 3.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "vuln_management",
       "label": "Known vulnerabilities scanned for and remediated",
       "family": "Patching",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "backups_running",
       "label": "Backups run on schedule and are isolated or immutable",
       "family": "Backups & Recovery",
       "weight": 3.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "restore_tested",
       "label": "Backup restores are tested, not just scheduled",
       "family": "Backups & Recovery",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "endpoint_protection",
       "label": "Endpoint protection installed and current on all devices",
       "family": "Endpoint",
       "weight": 3.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "device_encryption",
       "label": "Full-disk encryption enabled on endpoints",
       "family": "Endpoint",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "email_security_records",
       "label": "SPF, DKIM and DMARC published and enforced for the domain",
       "family": "Email Security",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "phishing_defense",
       "label": "Anti-phishing filtering and a user reporting path in place",
       "family": "Email Security",
       "weight": 1.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "firewall_network",
       "label": "Firewall in place; unused services and remote access off by default",
       "family": "Network",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "logging_monitoring",
       "label": "Security logs collected and reviewed",
       "family": "Monitoring",
       "weight": 1.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "incident_plan",
       "label": "Incident response plan exists and is findable",
       "family": "Response",
       "weight": 1.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "security_training",
       "label": "Staff receive security awareness training",
       "family": "People",
       "weight": 1.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      }
     ]
    }
   }
  },
  "POST /api/app/verified-desk/assessments/1/state (completed)": {
   "method": "POST",
   "path": "/api/app/verified-desk/assessments/1/state",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Assessment completed.",
    "assessment": {
     "id": 1,
     "clientId": 1,
     "title": "Verified assessment -- Capture Fixture LLC",
     "category": "Security Assessment",
     "status": "completed",
     "score": 100.0,
     "liveScore": 100.0,
     "scoreBreakdown": {
      "formula": "score = round(100 * sum(weight * points) / sum(weight), 1) over controls not marked na; points: pass=1, partial=0.5, fail=0; na controls are excluded from both sums",
      "earned": 3.0,
      "possible": 3.0,
      "counts": {
       "pass": 1,
       "partial": 0,
       "fail": 0,
       "na": 13
      },
      "controls": [
       {
        "key": "mfa_enforced",
        "status": "pass",
        "weight": 3.0,
        "points": 1.0
       },
       {
        "key": "access_least_privilege",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "patching_current",
        "status": "na",
        "weight": 3.0,
        "points": null
       },
       {
        "key": "vuln_management",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "backups_running",
        "status": "na",
        "weight": 3.0,
        "points": null
       },
       {
        "key": "restore_tested",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "endpoint_protection",
        "status": "na",
        "weight": 3.0,
        "points": null
       },
       {
        "key": "device_encryption",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "email_security_records",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "phishing_defense",
        "status": "na",
        "weight": 1.0,
        "points": null
       },
       {
        "key": "firewall_network",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "logging_monitoring",
        "status": "na",
        "weight": 1.0,
        "points": null
       },
       {
        "key": "incident_plan",
        "status": "na",
        "weight": 1.0,
        "points": null
       },
       {
        "key": "security_training",
        "status": "na",
        "weight": 1.0,
        "points": null
       }
      ]
     },
     "storedScoreJson": "{\"formula\": \"score = round(100 * sum(weight * points) / sum(weight), 1) over controls not marked na; points: pass=1, partial=0.5, fail=0; na controls are excluded from both sums\", \"earned\": 3.0, \"possible\": 3.0, \"counts\": {\"pass\": 1, \"partial\": 0, \"fail\": 0, \"na\": 13}, \"controls\": [{\"key\": \"mfa_enforced\", \"status\": \"pass\", \"weight\": 3.0, \"points\": 1.0}, {\"key\": \"access_least_privilege\", \"status\": \"na\", \"weight\": 2.0, \"points\": null}, {\"key\": \"patching_current\", \"status\": \"na\", \"weight\": 3.0, \"points\": null}, {\"key\": \"vuln_management\", \"status\": \"na\", \"weight\": 2.0, \"points\": null}, {\"key\": \"backups_running\", \"status\": \"na\", \"weight\": 3.0, \"points\": null}, {\"key\": \"restore_tested\", \"status\": \"na\", \"weight\": 2.0, \"points\": null}, {\"key\": \"endpoint_protection\", \"status\": \"na\", \"weight\": 3.0, \"points\": null}, {\"key\": \"device_encryption\", \"status\": \"na\", \"weight\": 2.0, \"points\": null}, {\"key\": \"email_security_records\", \"status\": \"na\", \"weight\": 2.0, \"points\": null}, {\"key\": \"phishing_defense\", \"status\": \"na\", \"weight\": 1.0, \"points\": null}, {\"key\": \"firewall_network\", \"status\": \"na\", \"weight\": 2.0, \"points\": null}, {\"key\": \"logging_monitoring\", \"status\": \"na\", \"weight\": 1.0, \"points\": null}, {\"key\": \"incident_plan\", \"status\": \"na\", \"weight\": 1.0, \"points\": null}, {\"key\": \"security_training\", \"status\": \"na\", \"weight\": 1.0, \"points\": null}]}",
     "verificationId": null,
     "verification": null,
     "badgeMeaning": "Assessed by Sentinel Safeguard against its published checklist on the date shown. This is not a certification, and Sentinel Safeguard is not an accrediting body.",
     "createdBy": 1,
     "reviewedBy": 1,
     "createdAt": 1793757974829,
     "updatedAt": 1793757975490,
     "completedAt": 1793757975490,
     "controls": [
      {
       "key": "mfa_enforced",
       "label": "Multi-factor authentication enforced for email, remote access and admin accounts",
       "family": "Identity & Access",
       "weight": 3.0,
       "status": "pass",
       "evidenceNote": "Capture evidence.",
       "updatedAt": 1793757975148
      },
      {
       "key": "access_least_privilege",
       "label": "Least-privilege access: admin accounts separated, access reviewed on a cadence",
       "family": "Identity & Access",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "patching_current",
       "label": "Operating systems and applications patched on a defined cadence",
       "family": "Patching",
       "weight": 3.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "vuln_management",
       "label": "Known vulnerabilities scanned for and remediated",
       "family": "Patching",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "backups_running",
       "label": "Backups run on schedule and are isolated or immutable",
       "family": "Backups & Recovery",
       "weight": 3.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "restore_tested",
       "label": "Backup restores are tested, not just scheduled",
       "family": "Backups & Recovery",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "endpoint_protection",
       "label": "Endpoint protection installed and current on all devices",
       "family": "Endpoint",
       "weight": 3.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "device_encryption",
       "label": "Full-disk encryption enabled on endpoints",
       "family": "Endpoint",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "email_security_records",
       "label": "SPF, DKIM and DMARC published and enforced for the domain",
       "family": "Email Security",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "phishing_defense",
       "label": "Anti-phishing filtering and a user reporting path in place",
       "family": "Email Security",
       "weight": 1.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "firewall_network",
       "label": "Firewall in place; unused services and remote access off by default",
       "family": "Network",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "logging_monitoring",
       "label": "Security logs collected and reviewed",
       "family": "Monitoring",
       "weight": 1.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "incident_plan",
       "label": "Incident response plan exists and is findable",
       "family": "Response",
       "weight": 1.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "security_training",
       "label": "Staff receive security awareness training",
       "family": "People",
       "weight": 1.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      }
     ]
    }
   }
  },
  "POST /api/app/verified-desk/assessments/1/issue-badge": {
   "method": "POST",
   "path": "/api/app/verified-desk/assessments/1/issue-badge",
   "status": 201,
   "body": {
    "ok": true,
    "message": "Verified badge issued.",
    "verification": {
     "id": 1,
     "clientId": 1,
     "businessName": "Capture Fixture LLC",
     "code": "SV-XXXXXX-XXXXXX",
     "category": "Security Assessment",
     "issuedDate": "2026-11-03",
     "expiryDate": "2027-11-03",
     "status": "active",
     "storedStatus": "active",
     "revokedAt": null,
     "createdAt": 1793757975665,
     "assessmentId": 1,
     "score": 100.0,
     "revokedReason": "",
     "badgeSnippet": "<a href=\"https://sentinelsafeguard.net/verified?code=SV-XXXXXX-XXXXXX\" title=\"Sentinel Verified 2026\"><img src=\"https://sentinelsafeguard.net/static/verified-badge.svg\" alt=\"Sentinel Verified 2026\" width=\"220\" height=\"56\"></a>"
    },
    "assessment": {
     "id": 1,
     "clientId": 1,
     "title": "Verified assessment -- Capture Fixture LLC",
     "category": "Security Assessment",
     "status": "completed",
     "score": 100.0,
     "liveScore": 100.0,
     "scoreBreakdown": {
      "formula": "score = round(100 * sum(weight * points) / sum(weight), 1) over controls not marked na; points: pass=1, partial=0.5, fail=0; na controls are excluded from both sums",
      "earned": 3.0,
      "possible": 3.0,
      "counts": {
       "pass": 1,
       "partial": 0,
       "fail": 0,
       "na": 13
      },
      "controls": [
       {
        "key": "mfa_enforced",
        "status": "pass",
        "weight": 3.0,
        "points": 1.0
       },
       {
        "key": "access_least_privilege",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "patching_current",
        "status": "na",
        "weight": 3.0,
        "points": null
       },
       {
        "key": "vuln_management",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "backups_running",
        "status": "na",
        "weight": 3.0,
        "points": null
       },
       {
        "key": "restore_tested",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "endpoint_protection",
        "status": "na",
        "weight": 3.0,
        "points": null
       },
       {
        "key": "device_encryption",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "email_security_records",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "phishing_defense",
        "status": "na",
        "weight": 1.0,
        "points": null
       },
       {
        "key": "firewall_network",
        "status": "na",
        "weight": 2.0,
        "points": null
       },
       {
        "key": "logging_monitoring",
        "status": "na",
        "weight": 1.0,
        "points": null
       },
       {
        "key": "incident_plan",
        "status": "na",
        "weight": 1.0,
        "points": null
       },
       {
        "key": "security_training",
        "status": "na",
        "weight": 1.0,
        "points": null
       }
      ]
     },
     "storedScoreJson": "{\"formula\": \"score = round(100 * sum(weight * points) / sum(weight), 1) over controls not marked na; points: pass=1, partial=0.5, fail=0; na controls are excluded from both sums\", \"earned\": 3.0, \"possible\": 3.0, \"counts\": {\"pass\": 1, \"partial\": 0, \"fail\": 0, \"na\": 13}, \"controls\": [{\"key\": \"mfa_enforced\", \"status\": \"pass\", \"weight\": 3.0, \"points\": 1.0}, {\"key\": \"access_least_privilege\", \"status\": \"na\", \"weight\": 2.0, \"points\": null}, {\"key\": \"patching_current\", \"status\": \"na\", \"weight\": 3.0, \"points\": null}, {\"key\": \"vuln_management\", \"status\": \"na\", \"weight\": 2.0, \"points\": null}, {\"key\": \"backups_running\", \"status\": \"na\", \"weight\": 3.0, \"points\": null}, {\"key\": \"restore_tested\", \"status\": \"na\", \"weight\": 2.0, \"points\": null}, {\"key\": \"endpoint_protection\", \"status\": \"na\", \"weight\": 3.0, \"points\": null}, {\"key\": \"device_encryption\", \"status\": \"na\", \"weight\": 2.0, \"points\": null}, {\"key\": \"email_security_records\", \"status\": \"na\", \"weight\": 2.0, \"points\": null}, {\"key\": \"phishing_defense\", \"status\": \"na\", \"weight\": 1.0, \"points\": null}, {\"key\": \"firewall_network\", \"status\": \"na\", \"weight\": 2.0, \"points\": null}, {\"key\": \"logging_monitoring\", \"status\": \"na\", \"weight\": 1.0, \"points\": null}, {\"key\": \"incident_plan\", \"status\": \"na\", \"weight\": 1.0, \"points\": null}, {\"key\": \"security_training\", \"status\": \"na\", \"weight\": 1.0, \"points\": null}]}",
     "verificationId": 1,
     "verification": {
      "id": 1,
      "clientId": 1,
      "businessName": "Capture Fixture LLC",
      "code": "SV-XXXXXX-XXXXXX",
      "category": "Security Assessment",
      "issuedDate": "2026-11-03",
      "expiryDate": "2027-11-03",
      "status": "active",
      "storedStatus": "active",
      "revokedAt": null,
      "createdAt": 1793757975665,
      "assessmentId": 1,
      "score": 100.0,
      "revokedReason": "",
      "badgeSnippet": "<a href=\"https://sentinelsafeguard.net/verified?code=SV-XXXXXX-XXXXXX\" title=\"Sentinel Verified 2026\"><img src=\"https://sentinelsafeguard.net/static/verified-badge.svg\" alt=\"Sentinel Verified 2026\" width=\"220\" height=\"56\"></a>"
     },
     "badgeMeaning": "Assessed by Sentinel Safeguard against its published checklist on the date shown. This is not a certification, and Sentinel Safeguard is not an accrediting body.",
     "createdBy": 1,
     "reviewedBy": 1,
     "createdAt": 1793757974829,
     "updatedAt": 1793757975661,
     "completedAt": 1793757975490,
     "controls": [
      {
       "key": "mfa_enforced",
       "label": "Multi-factor authentication enforced for email, remote access and admin accounts",
       "family": "Identity & Access",
       "weight": 3.0,
       "status": "pass",
       "evidenceNote": "Capture evidence.",
       "updatedAt": 1793757975148
      },
      {
       "key": "access_least_privilege",
       "label": "Least-privilege access: admin accounts separated, access reviewed on a cadence",
       "family": "Identity & Access",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "patching_current",
       "label": "Operating systems and applications patched on a defined cadence",
       "family": "Patching",
       "weight": 3.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "vuln_management",
       "label": "Known vulnerabilities scanned for and remediated",
       "family": "Patching",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "backups_running",
       "label": "Backups run on schedule and are isolated or immutable",
       "family": "Backups & Recovery",
       "weight": 3.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "restore_tested",
       "label": "Backup restores are tested, not just scheduled",
       "family": "Backups & Recovery",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "endpoint_protection",
       "label": "Endpoint protection installed and current on all devices",
       "family": "Endpoint",
       "weight": 3.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "device_encryption",
       "label": "Full-disk encryption enabled on endpoints",
       "family": "Endpoint",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "email_security_records",
       "label": "SPF, DKIM and DMARC published and enforced for the domain",
       "family": "Email Security",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "phishing_defense",
       "label": "Anti-phishing filtering and a user reporting path in place",
       "family": "Email Security",
       "weight": 1.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "firewall_network",
       "label": "Firewall in place; unused services and remote access off by default",
       "family": "Network",
       "weight": 2.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "logging_monitoring",
       "label": "Security logs collected and reviewed",
       "family": "Monitoring",
       "weight": 1.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "incident_plan",
       "label": "Incident response plan exists and is findable",
       "family": "Response",
       "weight": 1.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      },
      {
       "key": "security_training",
       "label": "Staff receive security awareness training",
       "family": "People",
       "weight": 1.0,
       "status": "na",
       "evidenceNote": "",
       "updatedAt": 1793757974829
      }
     ]
    },
    "badgeMeaning": "Assessed by Sentinel Safeguard against its published checklist on the date shown. This is not a certification, and Sentinel Safeguard is not an accrediting body."
   }
  },
  "POST /api/app/verified-desk/verifications/1/revoke": {
   "method": "POST",
   "path": "/api/app/verified-desk/verifications/1/revoke",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Verification revoked permanently.",
    "verification": {
     "id": 1,
     "clientId": 1,
     "businessName": "Capture Fixture LLC",
     "code": "SV-XXXXXX-XXXXXX",
     "category": "Security Assessment",
     "issuedDate": "2026-11-03",
     "expiryDate": "2027-11-03",
     "status": "revoked",
     "storedStatus": "revoked",
     "revokedAt": 1793757975860,
     "createdAt": 1793757975665,
     "assessmentId": 1,
     "score": 100.0,
     "revokedReason": "Capture revocation.",
     "badgeSnippet": "<a href=\"https://sentinelsafeguard.net/verified?code=SV-XXXXXX-XXXXXX\" title=\"Sentinel Verified 2026\"><img src=\"https://sentinelsafeguard.net/static/verified-badge.svg\" alt=\"Sentinel Verified 2026\" width=\"220\" height=\"56\"></a>"
    }
   }
  },
  "GET /api/app/verified-desk/evidence-pack?clientId=1": {
   "method": "GET",
   "path": "/api/app/verified-desk/evidence-pack?clientId=1",
   "status": 200,
   "body": {
    "pdfBase64": {
     "_truncated": true,
     "prefix": "JVBERi0xLjQKMSAwIG9iago8",
     "chars": 1268,
     "sha256": "3c4f90d4ce38989fe0cd1cdb8dfbbf80d718a418765e95a825ab32d1164eb391"
    },
    "pdfSha256": "f830df4bae78f7277acf3ba47430421ff5742e158ebcc0c1bd1f8dbac59b9e02",
    "pdfSizeBytes": 949,
    "ok": true,
    "fileName": "evidence-pack-client-1.pdf",
    "clientId": 1,
    "framework": "",
    "controlCount": 0
   }
  },
  "GET /api/app/verified-desk/assessments/999999 (missing)": {
   "method": "GET",
   "path": "/api/app/verified-desk/assessments/999999",
   "status": 404,
   "body": {
    "ok": false,
    "message": "Not found."
   }
  },
  "GET /api/app/activity": {
   "method": "GET",
   "path": "/api/app/activity",
   "status": 200,
   "body": {
    "rows": [
     {
      "id": 37,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "B3C_EVIDENCE_PACK",
      "label": "Compliance evidence pack exported",
      "eventClass": "changes",
      "entityType": "client",
      "entityId": 1,
      "entityExists": true,
      "entityLabel": "Capture Fixture LLC",
      "detail": "Compliance evidence pack exported for Capture Fixture LLC (all frameworks, 0 control(s)).",
      "createdAt": 1793757975999,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 36,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "B4_BADGE_REVOKED",
      "label": "Verified badge revoked (assessment path)",
      "eventClass": "changes",
      "entityType": "verification",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Sentinel Verified SV-XXXXXX-XXXXXX revoked permanently. Reason: Capture revocation.",
      "createdAt": 1793757975861,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 35,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "B4_BADGE_ISSUED",
      "label": "Verified badge issued from assessment",
      "eventClass": "changes",
      "entityType": "verification",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Verified badge SV-XXXXXX-XXXXXX issued from assessment #1 (score 100.0). Assessed by Sentinel Safeguard against its published checklist on the date shown. This is not a certification, and Sentinel Safeguard is not an accrediting body.",
      "createdAt": 1793757975730,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 34,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "VERIFICATION_ISSUED",
      "label": "Verification issued",
      "eventClass": "changes",
      "entityType": "verification",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Sentinel Verified SV-XXXXXX-XXXXXX issued to Capture Fixture LLC for Security Assessment; issued 2026-11-03, expires 2027-11-03.",
      "createdAt": 1793757975667,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 33,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "B4_ASSESSMENT_STATE",
      "label": "Verified assessment workflow state changed",
      "eventClass": "changes",
      "entityType": "client",
      "entityId": 1,
      "entityExists": true,
      "entityLabel": "Capture Fixture LLC",
      "detail": "Verified assessment #1 moved in_review -> completed.",
      "createdAt": 1793757975491,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 32,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "B4_ASSESSMENT_STATE",
      "label": "Verified assessment workflow state changed",
      "eventClass": "changes",
      "entityType": "client",
      "entityId": 1,
      "entityExists": true,
      "entityLabel": "Capture Fixture LLC",
      "detail": "Verified assessment #1 moved draft -> in_review.",
      "createdAt": 1793757975339,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 31,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "B4_ASSESSMENT_SAVED",
      "label": "Verified assessment checklist saved",
      "eventClass": "changes",
      "entityType": "client",
      "entityId": 1,
      "entityExists": true,
      "entityLabel": "Capture Fixture LLC",
      "detail": "Verified assessment #1 checklist saved (1 control(s)).",
      "createdAt": 1793757975148,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 30,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "B4_ASSESSMENT_STARTED",
      "label": "Verified assessment started",
      "eventClass": "changes",
      "entityType": "client",
      "entityId": 1,
      "entityExists": true,
      "entityLabel": "Capture Fixture LLC",
      "detail": "Verified assessment #1 started for Capture Fixture LLC (Security Assessment).",
      "createdAt": 1793757974834,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 29,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "CF2_STAFF_AVAILABILITY_DELETED",
      "label": "Cf2 staff availability deleted",
      "eventClass": "admin",
      "entityType": "cf_staff_availability",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Staff availability record deleted (was tentative 2026-12-01 to 2026-12-02).",
      "createdAt": 1793757974558,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 28,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "CF2_STAFF_AVAILABILITY_SAVED",
      "label": "Cf2 staff availability saved",
      "eventClass": "changes",
      "entityType": "cf_staff_availability",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Staff availability updated: StaffCap8A is tentative 2026-12-01 to 2026-12-02.",
      "createdAt": 1793757974370,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 27,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "CF2_STAFF_AVAILABILITY_SAVED",
      "label": "Cf2 staff availability saved",
      "eventClass": "changes",
      "entityType": "cf_staff_availability",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Staff availability added: StaffCap8A is unavailable 2026-12-01 to 2026-12-02.",
      "createdAt": 1793757974129,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 26,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "CF2_OWNER_TASK_STATE",
      "label": "Cf2 owner task state",
      "eventClass": "changes",
      "entityType": "cf_owner_task",
      "entityId": 2,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Owner task #2 state changed from open to done: Capture task two.",
      "createdAt": 1793757973889,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 25,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "CF2_OWNER_TASK_SAVED",
      "label": "Cf2 owner task saved",
      "eventClass": "changes",
      "entityType": "cf_owner_task",
      "entityId": 2,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Owner task added: Capture task two (priority Normal).",
      "createdAt": 1793757973720,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 24,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "CF2_OWNER_TASK_CONVERTED",
      "label": "Cf2 owner task converted",
      "eventClass": "changes",
      "entityType": "cf_owner_task",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Owner task #1 converted: NEW ticket #2 created ('Capture owner task v2'); the task stays on the board, done, linked to that ticket.",
      "createdAt": 1793757973555,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 23,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "CF2_OWNER_TASK_SAVED",
      "label": "Cf2 owner task saved",
      "eventClass": "changes",
      "entityType": "cf_owner_task",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Owner task updated: Capture owner task v2 (priority High).",
      "createdAt": 1793757973293,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 22,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "CF2_OWNER_TASK_SAVED",
      "label": "Cf2 owner task saved",
      "eventClass": "changes",
      "entityType": "cf_owner_task",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Owner task added: Capture owner task (priority High).",
      "createdAt": 1793757973134,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 21,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "CF2_VENDOR_DELETED",
      "label": "Cf2 vendor deleted",
      "eventClass": "admin",
      "entityType": "cf_vendor",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Vendor deleted: Capture Vendor Co II.",
      "createdAt": 1793757972946,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 20,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "CF2_VENDOR_SAVED",
      "label": "Cf2 vendor saved",
      "eventClass": "changes",
      "entityType": "cf_vendor",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Vendor updated: Capture Vendor Co II (Capture hosting, renews 2027-02-01).",
      "createdAt": 1793757972696,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 19,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "CF2_VENDOR_SAVED",
      "label": "Cf2 vendor saved",
      "eventClass": "changes",
      "entityType": "cf_vendor",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Vendor added: Capture Vendor Co (Capture hosting, renews 2027-02-01).",
      "createdAt": 1793757972513,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 18,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "B3_TIP_DELETED",
      "label": "B3 tip deleted",
      "eventClass": "admin",
      "entityType": "tip",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Portal security tip deleted.",
      "createdAt": 1793757971882,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 17,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "B3_TIP_SAVED",
      "label": "B3 tip saved",
      "eventClass": "changes",
      "entityType": "tip",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Portal security tip saved.",
      "createdAt": 1793757971606,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 16,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "B3_TIP_SAVED",
      "label": "B3 tip saved",
      "eventClass": "changes",
      "entityType": "tip",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Portal security tip saved.",
      "createdAt": 1793757971336,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 15,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "B3_SECRET_CREATED",
      "label": "B3 secret created",
      "eventClass": "changes",
      "entityType": "client",
      "entityId": 1,
      "entityExists": true,
      "entityLabel": "Capture Fixture LLC",
      "detail": "One-time secret 'Capture mailbox secret' created for the client portal (expires in 24 hour(s)).",
      "createdAt": 1793757970822,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 14,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "B3_SUB_UNASSIGNED",
      "label": "B3 sub unassigned",
      "eventClass": "changes",
      "entityType": "job",
      "entityId": 1,
      "entityExists": true,
      "entityLabel": "Capture site job",
      "detail": "Subcontractor unassigned from a job.",
      "createdAt": 1793757970623,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 13,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "B3_SUB_ASSIGNED",
      "label": "B3 sub assigned",
      "eventClass": "changes",
      "entityType": "job",
      "entityId": 1,
      "entityExists": true,
      "entityLabel": "Capture site job",
      "detail": "Subcontractor assigned to a job.",
      "createdAt": 1793757970340,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 12,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "JOB_SAVED",
      "label": "Job saved",
      "eventClass": "changes",
      "entityType": "job",
      "entityId": 1,
      "entityExists": true,
      "entityLabel": "Capture site job",
      "detail": "Job 'Capture site job' saved for 2026-11-10 with 1 assignee(s).",
      "createdAt": 1793757970161,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 11,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "TIME_ENTRY_CREATED",
      "label": "Time entry created",
      "eventClass": "changes",
      "entityType": "cf_time_entry",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Manual time entry created: Capture work (1800s).",
      "createdAt": 1793757969954,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 10,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "TICKET_SAVED",
      "label": "Ticket saved",
      "eventClass": "changes",
      "entityType": "ticket",
      "entityId": 1,
      "entityExists": true,
      "entityLabel": "Capture costing ticket",
      "detail": "Ticket saved (via app).",
      "createdAt": 1793757969788,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 9,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "CLIENT_SAVED",
      "label": "Client saved",
      "eventClass": "changes",
      "entityType": "client",
      "entityId": 1,
      "entityExists": true,
      "entityLabel": "Capture Fixture LLC",
      "detail": "Client saved.",
      "createdAt": 1793757969605,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 8,
      "actorId": 2,
      "actorName": "StaffCap8A",
      "actorRole": "STAFF",
      "action": "APP_LOGIN",
      "label": "App sign-in \u2014 device enrolled",
      "eventClass": "auth",
      "entityType": "user",
      "entityId": 2,
      "entityExists": true,
      "entityLabel": "StaffCap8A",
      "detail": "App sign-in \u2014 device enrolled: cap8a.",
      "createdAt": 1793757969364,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 7,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "APP_LOGIN",
      "label": "App sign-in \u2014 device enrolled",
      "eventClass": "auth",
      "entityType": "user",
      "entityId": 1,
      "entityExists": true,
      "entityLabel": "OwnerCap8A",
      "detail": "App sign-in \u2014 device enrolled: cap8a.",
      "createdAt": 1793757969180,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 6,
      "actorId": null,
      "actorName": "",
      "actorRole": "",
      "action": "AUTH_FAILED",
      "label": "Failed sign-in attempt",
      "eventClass": "auth",
      "entityType": "action",
      "entityId": null,
      "entityExists": false,
      "entityLabel": "",
      "detail": "login denied; id-hash 27bc00555482",
      "createdAt": 1793757968976,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 5,
      "actorId": 3,
      "actorName": "SubCap8A",
      "actorRole": "Subcontractor",
      "action": "INVITE_REDEEMED",
      "label": "Invite redeemed \u2014 account created",
      "eventClass": "auth",
      "entityType": "invite",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Subcontractor account created via invite.",
      "createdAt": 1793757968790,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 4,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "INVITE_CREATED",
      "label": "Invite created",
      "eventClass": "admin",
      "entityType": "invite",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Invite created for role Subcontractor.",
      "createdAt": 1793757968606,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 3,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "EMPLOYEE_CREATED",
      "label": "Employee created",
      "eventClass": "admin",
      "entityType": "user",
      "entityId": 2,
      "entityExists": true,
      "entityLabel": "StaffCap8A",
      "detail": "Staff user StaffCap8A created.",
      "createdAt": 1793757968497,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 2,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "LOGIN",
      "label": "Signed in",
      "eventClass": "auth",
      "entityType": "session",
      "entityId": 1,
      "entityExists": true,
      "entityLabel": "OwnerCap8A",
      "detail": "Session opened; expires in 8 hours",
      "createdAt": 1793757968289,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 1,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "OWNER_INITIALIZED",
      "label": "Owner account initialized",
      "eventClass": "auth",
      "entityType": "settings",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Owner account initialized; recovery codes issued.",
      "createdAt": 1793757968094,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     }
    ],
    "total": 37,
    "offset": 0,
    "limit": 100,
    "summary": {
     "eventsToday": 37,
     "signins24h": 1,
     "failed24h": 1,
     "uniqueIps24h": 1,
     "downloads24h": 0,
     "activeSessions": 1
    },
    "failedByIp": [
     {
      "ip": "127.0.0.1",
      "count": 1,
      "lastAt": 1793757968976,
      "lastAction": "AUTH_FAILED",
      "lastActionLabel": "Failed sign-in attempt"
     }
    ],
    "sessions": [
     {
      "userId": 1,
      "name": "OwnerCap8A",
      "role": "OWNER",
      "createdAt": 1793757968286,
      "expiresAt": 1793786768286,
      "isCurrent": false,
      "lastIp": "127.0.0.1",
      "lastUaSummary": "Python-urllib/3.12"
     }
    ],
    "classCounts": {
     "all": 37,
     "auth": 6,
     "changes": 26,
     "files": 0,
     "views": 0,
     "admin": 5
    },
    "ok": true
   }
  },
  "GET /api/app/activity?class=auth&limit=5": {
   "method": "GET",
   "path": "/api/app/activity?class=auth&limit=5",
   "status": 200,
   "body": {
    "rows": [
     {
      "id": 8,
      "actorId": 2,
      "actorName": "StaffCap8A",
      "actorRole": "STAFF",
      "action": "APP_LOGIN",
      "label": "App sign-in \u2014 device enrolled",
      "eventClass": "auth",
      "entityType": "user",
      "entityId": 2,
      "entityExists": true,
      "entityLabel": "StaffCap8A",
      "detail": "App sign-in \u2014 device enrolled: cap8a.",
      "createdAt": 1793757969364,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 7,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "APP_LOGIN",
      "label": "App sign-in \u2014 device enrolled",
      "eventClass": "auth",
      "entityType": "user",
      "entityId": 1,
      "entityExists": true,
      "entityLabel": "OwnerCap8A",
      "detail": "App sign-in \u2014 device enrolled: cap8a.",
      "createdAt": 1793757969180,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 6,
      "actorId": null,
      "actorName": "",
      "actorRole": "",
      "action": "AUTH_FAILED",
      "label": "Failed sign-in attempt",
      "eventClass": "auth",
      "entityType": "action",
      "entityId": null,
      "entityExists": false,
      "entityLabel": "",
      "detail": "login denied; id-hash 27bc00555482",
      "createdAt": 1793757968976,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 5,
      "actorId": 3,
      "actorName": "SubCap8A",
      "actorRole": "Subcontractor",
      "action": "INVITE_REDEEMED",
      "label": "Invite redeemed \u2014 account created",
      "eventClass": "auth",
      "entityType": "invite",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Subcontractor account created via invite.",
      "createdAt": 1793757968790,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 2,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "LOGIN",
      "label": "Signed in",
      "eventClass": "auth",
      "entityType": "session",
      "entityId": 1,
      "entityExists": true,
      "entityLabel": "OwnerCap8A",
      "detail": "Session opened; expires in 8 hours",
      "createdAt": 1793757968289,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     }
    ],
    "total": 6,
    "offset": 0,
    "limit": 5,
    "summary": {
     "eventsToday": 37,
     "signins24h": 1,
     "failed24h": 1,
     "uniqueIps24h": 1,
     "downloads24h": 0,
     "activeSessions": 1
    },
    "failedByIp": [
     {
      "ip": "127.0.0.1",
      "count": 1,
      "lastAt": 1793757968976,
      "lastAction": "AUTH_FAILED",
      "lastActionLabel": "Failed sign-in attempt"
     }
    ],
    "sessions": [
     {
      "userId": 1,
      "name": "OwnerCap8A",
      "role": "OWNER",
      "createdAt": 1793757968286,
      "expiresAt": 1793786768286,
      "isCurrent": false,
      "lastIp": "127.0.0.1",
      "lastUaSummary": "Python-urllib/3.12"
     }
    ],
    "classCounts": {
     "all": 37,
     "auth": 6,
     "changes": 26,
     "files": 0,
     "views": 0,
     "admin": 5
    },
    "ok": true
   }
  },
  "GET /api/app/activity?limit=2&offset=2": {
   "method": "GET",
   "path": "/api/app/activity?limit=2&offset=2",
   "status": 200,
   "body": {
    "rows": [
     {
      "id": 35,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "B4_BADGE_ISSUED",
      "label": "Verified badge issued from assessment",
      "eventClass": "changes",
      "entityType": "verification",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Verified badge SV-XXXXXX-XXXXXX issued from assessment #1 (score 100.0). Assessed by Sentinel Safeguard against its published checklist on the date shown. This is not a certification, and Sentinel Safeguard is not an accrediting body.",
      "createdAt": 1793757975730,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     },
     {
      "id": 34,
      "actorId": 1,
      "actorName": "OwnerCap8A",
      "actorRole": "OWNER",
      "action": "VERIFICATION_ISSUED",
      "label": "Verification issued",
      "eventClass": "changes",
      "entityType": "verification",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Sentinel Verified SV-XXXXXX-XXXXXX issued to Capture Fixture LLC for Security Assessment; issued 2026-11-03, expires 2027-11-03.",
      "createdAt": 1793757975667,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": ""
     }
    ],
    "total": 37,
    "offset": 2,
    "limit": 2,
    "summary": {
     "eventsToday": 37,
     "signins24h": 1,
     "failed24h": 1,
     "uniqueIps24h": 1,
     "downloads24h": 0,
     "activeSessions": 1
    },
    "failedByIp": [
     {
      "ip": "127.0.0.1",
      "count": 1,
      "lastAt": 1793757968976,
      "lastAction": "AUTH_FAILED",
      "lastActionLabel": "Failed sign-in attempt"
     }
    ],
    "sessions": [
     {
      "userId": 1,
      "name": "OwnerCap8A",
      "role": "OWNER",
      "createdAt": 1793757968286,
      "expiresAt": 1793786768286,
      "isCurrent": false,
      "lastIp": "127.0.0.1",
      "lastUaSummary": "Python-urllib/3.12"
     }
    ],
    "classCounts": {
     "all": 37,
     "auth": 6,
     "changes": 26,
     "files": 0,
     "views": 0,
     "admin": 5
    },
    "ok": true
   }
  },
  "GET /api/app/activity/sessions": {
   "method": "GET",
   "path": "/api/app/activity/sessions",
   "status": 200,
   "body": {
    "sessions": [
     {
      "id": 1,
      "userId": 1,
      "displayName": "OwnerCap8A",
      "username": "OwnerCap8A",
      "role": "OWNER",
      "createdAt": 1793757968286,
      "lastSeenAt": 1793757968286,
      "expiresAt": 1793786768286,
      "isCurrent": false
     }
    ],
    "count": 1,
    "ok": true
   }
  },
  "GET /api/app/activity/public-visitors": {
   "method": "GET",
   "path": "/api/app/activity/public-visitors",
   "status": 200,
   "body": {
    "connected": false,
    "ok": true
   }
  },
  "GET /api/app/activity (staff 403)": {
   "method": "GET",
   "path": "/api/app/activity",
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Not authorized."
   }
  }
 }
}"""

    private val root: JsonObject by lazy { Json.parseToJsonElement(ALL).jsonObject }
    private val bodies: JsonObject by lazy { root["bodies"]!!.jsonObject }

    fun body(name: String): String =
        bodies[name]!!.jsonObject["body"]!!.toString()

    fun status(name: String): Int =
        bodies[name]!!.jsonObject["status"]!!.jsonPrimitive.int

    fun bodyJson(name: String): JsonObject =
        bodies[name]!!.jsonObject["body"]!!.jsonObject
}

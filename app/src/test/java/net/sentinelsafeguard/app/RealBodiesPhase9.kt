package net.sentinelsafeguard.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Phase 9 REAL response bodies, captured 2026-11-03 from a
 * rehearsal of the deployed Phase 9 backend bytes (server.py
 * sha256 b267d652a71782c2b01ce951d72461bac2ec7aaf6828e53a255a6fd55d3adab9,
 * Addendum 28), against a seeded scratch database. Seed data is
 * fictional; the login device token is recorded as shape only
 * (length + sha256 prefix). [ALL] is
 * phase9-work/fixtures/real-bodies.json verbatim, with the one
 * flattened login capture normalized to {status, body}.
 */
object RealBodiesPhase9 {
    val ALL: String = """{
 "_meta": {
  "serverPySha256": "b267d652a71782c2b01ce951d72461bac2ec7aaf6828e53a255a6fd55d3adab9",
  "capturedFrom": "rehearsal capture of the deployed Phase 9 backend bytes (Addendum 28)",
  "note": "Real bodies from phase9-work/fixtures/real-bodies.json; seed data fictional; the login device token is recorded as shape only (length + sha256 prefix), never a value."
 },
 "bodies": {
  "POST /api/app/auth/login (portal)": {
   "status": 200,
   "body": {
    "ok": true,
    "user": {
     "id": 2,
     "displayName": "CapClient9",
     "role": "Client",
     "portalLang": "en",
     "clientId": 1,
     "jobTitle": "",
     "email": "",
     "phone": "",
     "active": true,
     "createdAt": 1793766548610,
     "mustReset": false,
     "totpEnrolled": false,
     "totpEnrollmentRequired": false
    },
    "deviceTokenShape": {
     "length": 64,
     "sha256Prefix": "3132f702"
    },
    "expiresAt": 1801542548874,
    "device": {
     "id": 1,
     "deviceName": "capture",
     "createdAt": 1793766548874,
     "lastSeenAt": 1793766548874,
     "expiresAt": 1801542548874,
     "revoked": false,
     "revokedAt": null,
     "current": true
    }
   }
  },
  "GET /api/app/me": {
   "status": 200,
   "body": {
    "ok": true,
    "user": {
     "id": 2,
     "displayName": "CapClient9",
     "role": "Client",
     "portalLang": "en",
     "clientId": 1,
     "jobTitle": "",
     "email": "",
     "phone": "",
     "active": true,
     "createdAt": 1793766548610,
     "mustReset": false,
     "totpEnrolled": false,
     "totpEnrollmentRequired": false
    },
    "serverTime": 1793766549013,
    "device": {
     "id": 1,
     "deviceName": "capture",
     "createdAt": 1793766548874,
     "lastSeenAt": 1793766548952,
     "expiresAt": 1801542548952,
     "revoked": false,
     "revokedAt": null,
     "current": true
    },
    "lawAccess": false,
    "newsAccess": false,
    "osintAccess": false,
    "breachAccess": false,
    "threatAccess": false,
    "reviewsAccess": false,
    "taxAccess": false,
    "portal": true,
    "taxOnly": false
   }
  },
  "GET /api/app/portal": {
   "status": 200,
   "body": {
    "linked": true,
    "portalLang": "en",
    "contactKind": "primary",
    "portalAccess": {
     "work": true,
     "money": true
    },
    "portalMessageUnread": 0,
    "notificationPrefs": {
     "ticketUpdates": true,
     "invoiceEvents": true,
     "monitoringAlerts": true,
     "digest": false
    },
    "canEditPrefs": true,
    "securityScore": {
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
    "profile": {
     "legalName": "Capture Alpha Co",
     "dba": "",
     "contactName": "Ann Capture",
     "contactTitle": "",
     "email": "cap-alpha@example.invalid",
     "phone": "555-0111",
     "street": "",
     "city": "",
     "state": "",
     "zip": ""
    },
    "cases": [],
    "tickets": [
     {
      "id": 1,
      "title": "Capture ticket",
      "status": "Open",
      "priority": "Normal",
      "category": "",
      "dueDate": null,
      "closedDate": "",
      "createdAt": 1793766547959,
      "updatedAt": 1793766547959
     }
    ],
    "invoices": [],
    "documents": [],
    "appointments": [],
    "assets": [],
    "progress": [
     {
      "kind": "ticket",
      "id": 1,
      "title": "Capture ticket",
      "status": "Open",
      "priority": "Normal",
      "category": "",
      "dueDate": null,
      "closedDate": "",
      "createdAt": 1793766547959,
      "updatedAt": 1793766547959,
      "progressStage": "Received",
      "progressStageIndex": 0,
      "progressStages": [
       "Received",
       "In Progress",
       "Resolved"
      ],
      "history": [
       {
        "progressStage": "Received",
        "at": 1793766547959,
        "atKind": "created"
       }
      ],
      "photos": []
     }
    ],
    "quotes": [],
    "paymentPlans": [],
    "cfRoadmap": {
     "items": [],
     "total": 0,
     "doneCount": 0,
     "percent": 0
    },
    "cfBreachMatches": [],
    "cfBreachAlerts": [],
    "cfComplianceSummary": [],
    "library": [
     {
      "id": 1,
      "title": "Capture guide",
      "category": "Reference",
      "description": "capture kb",
      "fileName": null,
      "fileSize": null,
      "pageCount": null,
      "hasFile": false
     }
    ],
    "siteProfiles": [],
    "cf2Hygiene": {
     "items": [
      {
       "key": "mfa_enabled",
       "label": "Multi-factor authentication enabled",
       "state": "unknown",
       "checkedAt": null
      },
      {
       "key": "password_manager",
       "label": "Password manager in use",
       "state": "unknown",
       "checkedAt": null
      },
      {
       "key": "unique_passwords_attested",
       "label": "Unique passwords attested (no reuse)",
       "state": "unknown",
       "checkedAt": null
      },
      {
       "key": "backup_codes_stored",
       "label": "Backup / recovery codes stored safely",
       "state": "unknown",
       "checkedAt": null
      },
      {
       "key": "recovery_email_current",
       "label": "Recovery email address current",
       "state": "unknown",
       "checkedAt": null
      },
      {
       "key": "recovery_phone_current",
       "label": "Recovery phone number current",
       "state": "unknown",
       "checkedAt": null
      },
      {
       "key": "admin_accounts_reviewed",
       "label": "Admin accounts reviewed",
       "state": "unknown",
       "checkedAt": null
      },
      {
       "key": "shared_passwords_rotated",
       "label": "Shared passwords rotated when staff changes",
       "state": "unknown",
       "checkedAt": null
      }
     ],
     "counts": {
      "yes": 0,
      "no": 0,
      "unknown": 8
     },
     "total": 8
    },
    "ok": true
   }
  },
  "GET /api/app/portal/history": {
   "status": 200,
   "body": {
    "history": [
     {
      "kind": "ticket",
      "at": 1793766547959,
      "refId": 1,
      "title": "Capture ticket",
      "detail": "Open"
     }
    ],
    "ok": true
   }
  },
  "GET /api/app/portal/health-timeline": {
   "status": 200,
   "body": {
    "linked": true,
    "events": [
     {
      "kind": "ticket-opened",
      "at": 1793766547959,
      "title": "Ticket opened: Capture ticket",
      "detail": "Open \u00b7 Normal",
      "refType": "ticket",
      "refId": 1
     },
     {
      "kind": "contract",
      "at": 1793766546989,
      "title": "Contract: Capture contract",
      "detail": "",
      "refType": "cf_contracts",
      "refId": 1
     }
    ],
    "nextCursor": null,
    "total": 2,
    "pageSize": 25,
    "securityScore": {
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
    "ok": true
   }
  },
  "GET /api/app/portal/knowledge": {
   "status": 200,
   "body": {
    "linked": true,
    "library": [
     {
      "id": 1,
      "title": "Capture guide",
      "category": "Reference",
      "description": "capture kb",
      "fileName": null,
      "fileSize": null,
      "pageCount": null,
      "hasFile": false
     }
    ],
    "ok": true
   }
  },
  "GET /api/app/portal/contracts": {
   "status": 200,
   "body": {
    "ok": true,
    "contracts": [
     {
      "id": 1,
      "clientId": 1,
      "templateId": null,
      "templateType": "service_agreement",
      "title": "Capture contract",
      "body": "Capture body",
      "status": "sent",
      "sentAt": null,
      "signatures": [],
      "createdAt": 1793766546989,
      "updatedAt": 1793766546989
     }
    ],
    "engagements": []
   }
  },
  "GET /api/app/portal/contracts/{id}": {
   "status": 200,
   "body": {
    "ok": true,
    "contract": {
     "id": 1,
     "clientId": 1,
     "templateId": null,
     "templateType": "service_agreement",
     "title": "Capture contract",
     "body": "Capture body",
     "status": "awaiting",
     "sentAt": null,
     "signatures": [],
     "createdAt": 1793766546989,
     "updatedAt": 1793766549628
    }
   }
  },
  "GET /api/app/portal/sign-offs": {
   "status": 200,
   "body": {
    "ok": true,
    "signOffs": []
   }
  },
  "GET /api/app/portal/authorizations": {
   "status": 200,
   "body": {
    "ok": true,
    "authorizations": []
   }
  },
  "GET /api/app/portal/tickets/{id}/messages": {
   "status": 200,
   "body": {
    "ticketId": 1,
    "clientId": 1,
    "messages": [],
    "unreadForStaff": 0,
    "unreadForClient": 0,
    "ok": true
   }
  },
  "GET /api/app/portal/packets": {
   "status": 200,
   "body": {
    "ok": true,
    "packets": [
     {
      "id": 1,
      "title": "Capture packet",
      "itemCount": 0,
      "issuedAt": 1793766546989
     }
    ]
   }
  },
  "GET /api/app/portal/packets/{id}/document": {
   "status": 200,
   "body": {
    "ok": true,
    "packet": {
     "id": 1,
     "title": "Capture packet",
     "issuedAt": 1793766546989
    },
    "pdfBase64": "JVBERi0xLjQKMSAwIG9iago8PCAvVHlwZSAvQ2F0YWxvZyAvUGFnZXMgMiAwIFIgPj4KZW5kb2JqCjIgMCBvYmoKPDwgL1R5cGUgL1BhZ2VzIC9LaWRzIFs0IDAgUl0gL0NvdW50IDEgPj4KZW5kb2JqCjMgMCBvYmoKPDwgL1R5cGUgL0ZvbnQgL1N1YnR5cGUgL1R5cGUxIC9CYXNlRm9udCAvSGVsdmV0aWNhID4+CmVuZG9iago0IDAgb2JqCjw8IC9UeXBlIC9QYWdlIC9QYXJlbnQgMiAwIFIgL01lZGlhQm94IFswIDAgNjEyIDc5Ml0gL1Jlc291cmNlcyA8PCAvRm9udCA8PCAvRjEgMyAwIFIgPj4gPj4gL0NvbnRlbnRzIDUgMCBSID4+CmVuZG9iago1IDAgb2JqCjw8IC9MZW5ndGggMjAxID4+CnN0cmVhbQpCVCAvRjEgMTUgVGYgNTYgNzU2IFRkIChDYXB0dXJlIHBhY2tldCkgVGogRVQKQlQgL0YxIDEwIFRmCjEgMCAwIDEgNTYgNzMwIFRtIChDYXB0dXJlIHBhY2tldCkgVGoKMSAwIDAgMSA1NiA3MTYgVG0gKCkgVGoKMSAwIDAgMSA1NiA3MDIgVG0gKFByZXBhcmVkIGZvcjogQ2FwdHVyZSBBbHBoYSBDbykgVGoKMSAwIDAgMSA1NiA2ODggVG0gKCkgVGoKRVQKZW5kc3RyZWFtCmVuZG9iagp4cmVmCjAgNgowMDAwMDAwMDAwIDY1NTM1IGYgCjAwMDAwMDAwMDkgMDAwMDAgbiAKMDAwMDAwMDA1OCAwMDAwMCBuIAowMDAwMDAwMTE1IDAwMDAwIG4gCjAwMDAwMDAxODUgMDAwMDAgbiAKMDAwMDAwMDMxMSAwMDAwMCBuIAp0cmFpbGVyCjw8IC9TaXplIDYgL1Jvb3QgMSAwIFIgPj4Kc3RhcnR4cmVmCjU2MwolJUVPRgo=",
    "pdfSha256": "69b3981464f8f9598ee1f2cdcadd81812dc09fa5ab4bb2c1c80b67cadb5e5b30",
    "pdfSizeBytes": 746
   }
  },
  "GET /api/app/portal/file-requests": {
   "status": 200,
   "body": {
    "ok": true,
    "requests": []
   }
  },
  "GET /api/app/portal/status-board": {
   "status": 200,
   "body": {
    "ok": true,
    "board": {
     "clientId": 1,
     "clientName": "Capture Alpha Co",
     "generatedAt": 1793766550456,
     "locations": {
      "available": true,
      "count": 0
     },
     "warranties": {
      "available": true,
      "total": 0,
      "expiring": 0
     },
     "licenses": {
      "available": true,
      "total": 0,
      "expiring": 0
     },
     "retainers": {
      "available": true,
      "total": 0,
      "items": [],
      "balanceHours": 0
     },
     "maintUpcoming": {
      "total": 0,
      "items": [],
      "conflictCount": 0
     },
     "emergencyContacts": {
      "count": 0
     },
     "assets": {
      "count": 0
     }
    }
   }
  },
  "GET /api/app/portal/surveys": {
   "status": 200,
   "body": {
    "ok": true,
    "linked": true,
    "surveys": []
   }
  },
  "GET /api/app/portal/referrals": {
   "status": 200,
   "body": {
    "ok": true,
    "linked": true,
    "count": 0,
    "referrals": []
   }
  },
  "GET /api/app/portal/proposals": {
   "status": 200,
   "body": {
    "proposals": [],
    "ok": true
   }
  },
  "GET /api/app/portal/change-requests": {
   "status": 200,
   "body": {
    "changeRequests": [],
    "ok": true
   }
  },
  "GET /api/app/portal/approval-inbox": {
   "status": 200,
   "body": {
    "linked": true,
    "contactKind": "primary",
    "portalAccess": {
     "work": true,
     "money": true
    },
    "items": [],
    "counts": {
     "total": 0,
     "quote": 0,
     "proposal": 0,
     "changeRequest": 0,
     "checklistSignoff": 0
    },
    "ok": true
   }
  },
  "GET /api/app/portal/notification-prefs": {
   "status": 200,
   "body": {
    "clientId": 1,
    "prefs": {
     "ticketUpdates": true,
     "invoiceEvents": true,
     "monitoringAlerts": true,
     "digest": false
    },
    "ok": true
   }
  },
  "GET /api/app/portal/contacts": {
   "status": 200,
   "body": {
    "clientId": 1,
    "contacts": [],
    "ok": true
   }
  },
  "GET /api/app/portal/lang": {
   "status": 200,
   "body": {
    "ok": true,
    "portalLang": "en"
   }
  },
  "GET /api/app/my-schedule (portal token \u2014 refusal)": {
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Not authorized."
   }
  },
  "GET /api/app/clients (portal token \u2014 refusal)": {
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

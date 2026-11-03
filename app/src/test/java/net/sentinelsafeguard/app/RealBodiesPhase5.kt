package net.sentinelsafeguard.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Phase 5 REAL response bodies, captured 2026-11-03 from a
 * rehearsal boot of the exact Phase 5 candidate bytes
 * (server.py sha256 6cd150bd18cdc2e5fe4c73902326430c2a5ec5b1a8b27dcbdda12cd8b2523230,
 * Addendum 18 — the deployed production bytes), against a seeded
 * scratch database. Secrets scrubbed by the backend crew (device
 * tokens, invite codes redacted; QR codes and label SVGs
 * truncated). [ALL] is the captures file verbatim (44 captures
 * + _meta, nested under "bodies").
 */
object RealBodiesPhase5 {
    val ALL: String = """{
 "_meta": {
  "serverPySha256": "6cd150bd18cdc2e5fe4c73902326430c2a5ec5b1a8b27dcbdda12cd8b2523230",
  "capturedFrom": "rehearsal boot of the exact candidate bytes",
  "scratchDb": "capture_phase5.db (fictional seed data only)",
  "scrubbed": [
   "device tokens",
   "invite codes",
   "QR codes truncated",
   "label SVGs truncated"
  ]
 },
 "bodies": {
  "GET /api/app/employees": {
   "status": 200,
   "body": {
    "ok": true,
    "total": 2,
    "limit": 50,
    "offset": 0,
    "employees": [
     {
      "id": 1,
      "displayName": "Capture Owner",
      "role": "OWNER",
      "portalLang": "en",
      "clientId": null,
      "jobTitle": "",
      "email": "",
      "phone": "",
      "active": true,
      "createdAt": 1793745471863,
      "openCounts": {
       "cases": 0,
       "tickets": 0,
       "assets": 0,
       "total": 0
      }
     },
     {
      "id": 2,
      "displayName": "Capture Tech",
      "role": "STAFF",
      "portalLang": "en",
      "clientId": null,
      "jobTitle": "Field Technician",
      "email": "tech@capture.invalid",
      "phone": "555-0110",
      "active": true,
      "createdAt": 1793745472165,
      "openCounts": {
       "cases": 0,
       "tickets": 1,
       "assets": 0,
       "total": 1
      }
     }
    ],
    "stats": {
     "team": 2,
     "active": 2,
     "owners": 1,
     "staff": 1,
     "pendingInvites": 0,
     "unassigned": 0
    }
   }
  },
  "GET /api/app/employees?role=Staff": {
   "status": 200,
   "body": {
    "ok": true,
    "total": 1,
    "limit": 50,
    "offset": 0,
    "employees": [
     {
      "id": 2,
      "displayName": "Capture Tech",
      "role": "STAFF",
      "portalLang": "en",
      "clientId": null,
      "jobTitle": "Field Technician",
      "email": "tech@capture.invalid",
      "phone": "555-0110",
      "active": true,
      "createdAt": 1793745472165,
      "openCounts": {
       "cases": 0,
       "tickets": 1,
       "assets": 0,
       "total": 1
      }
     }
    ],
    "stats": {
     "team": 2,
     "active": 2,
     "owners": 1,
     "staff": 1,
     "pendingInvites": 0,
     "unassigned": 0
    }
   }
  },
  "GET /api/app/employees/workload": {
   "status": 200,
   "body": {
    "ok": true,
    "members": [
     {
      "user": {
       "id": 1,
       "displayName": "Capture Owner",
       "role": "OWNER",
       "portalLang": "en",
       "clientId": null,
       "jobTitle": "",
       "email": "",
       "phone": "",
       "active": true,
       "createdAt": 1793745471863
      },
      "workload": {
       "cases": {
        "open": [],
        "closed": [],
        "openCount": 0,
        "closedCount": 0
       },
       "tickets": {
        "open": [],
        "closed": [],
        "openCount": 0,
        "closedCount": 0
       },
       "assets": {
        "open": [],
        "closed": [],
        "openCount": 0,
        "closedCount": 0
       },
       "openTotal": 0
      }
     },
     {
      "user": {
       "id": 2,
       "displayName": "Capture Tech",
       "role": "STAFF",
       "portalLang": "en",
       "clientId": null,
       "jobTitle": "Field Technician",
       "email": "tech@capture.invalid",
       "phone": "555-0110",
       "active": true,
       "createdAt": 1793745472165
      },
      "workload": {
       "cases": {
        "open": [],
        "closed": [],
        "openCount": 0,
        "closedCount": 0
       },
       "tickets": {
        "open": [
         {
          "id": 1,
          "clientId": 1,
          "assignedUserId": 2,
          "title": "Capture ticket",
          "status": "Open",
          "priority": "Normal",
          "category": "Support",
          "dueDate": null,
          "openedDate": "",
          "closedDate": "",
          "notes": "",
          "adMaterial": false,
          "archivedAt": null,
          "archivedBy": null,
          "createdAt": 1793745472554,
          "updatedAt": 1793745472554,
          "caseId": null,
          "source": "Internal",
          "sourceRef": null,
          "mergedIntoId": null,
          "tags": ""
         }
        ],
        "closed": [],
        "openCount": 1,
        "closedCount": 0
       },
       "assets": {
        "open": [],
        "closed": [],
        "openCount": 0,
        "closedCount": 0
       },
       "openTotal": 1
      }
     }
    ],
    "unassigned": {
     "cases": [],
     "tickets": [],
     "total": 0
    }
   }
  },
  "GET /api/app/employees/{id}": {
   "status": 200,
   "body": {
    "user": {
     "id": 2,
     "displayName": "Capture Tech",
     "role": "STAFF",
     "portalLang": "en",
     "clientId": null,
     "jobTitle": "Field Technician",
     "email": "tech@capture.invalid",
     "phone": "555-0110",
     "active": true,
     "createdAt": 1793745472165
    },
    "client": null,
    "workload": {
     "cases": {
      "open": [],
      "closed": [],
      "openCount": 0,
      "closedCount": 0
     },
     "tickets": {
      "open": [
       {
        "id": 1,
        "clientId": 1,
        "assignedUserId": 2,
        "title": "Capture ticket",
        "status": "Open",
        "priority": "Normal",
        "category": "Support",
        "dueDate": null,
        "openedDate": "",
        "closedDate": "",
        "notes": "",
        "adMaterial": false,
        "archivedAt": null,
        "archivedBy": null,
        "createdAt": 1793745472554,
        "updatedAt": 1793745472554,
        "caseId": null,
        "source": "Internal",
        "sourceRef": null,
        "mergedIntoId": null,
        "tags": ""
       }
      ],
      "closed": [],
      "openCount": 1,
      "closedCount": 0
     },
     "assets": {
      "open": [],
      "closed": [],
      "openCount": 0,
      "closedCount": 0
     },
     "openTotal": 1
    },
    "schedule": [],
    "activity": [],
    "sessions": [],
    "moderation": {
     "suspendUntil": null,
     "mustReset": false,
     "totpEnrolled": false,
     "modNote": ""
    },
    "invites": [],
    "ok": true
   }
  },
  "POST /api/app/employees": {
   "status": 201,
   "body": {
    "ok": true,
    "message": "Employee created.",
    "id": 3,
    "user": {
     "id": 3,
     "displayName": "Capture Second",
     "role": "STAFF",
     "portalLang": "en",
     "clientId": null,
     "jobTitle": "Analyst",
     "email": "second@capture.invalid",
     "phone": "555-0112",
     "active": true,
     "createdAt": 1793745473252
    }
   }
  },
  "PATCH /api/app/employees/{id}": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Employee updated.",
    "user": {
     "id": 3,
     "displayName": "Capture Second",
     "role": "STAFF",
     "portalLang": "en",
     "clientId": null,
     "jobTitle": "Senior Analyst",
     "email": "second@capture.invalid",
     "phone": "555-0112",
     "active": true,
     "createdAt": 1793745473252
    }
   }
  },
  "POST /api/app/employees/{id}/mod-note": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Moderator note saved."
   }
  },
  "POST /api/app/employees/{id}/must-reset": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Passphrase requirement updated."
   }
  },
  "POST /api/app/employees/{id}/suspend (lift)": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Suspension lifted.",
    "suspendUntil": null
   }
  },
  "POST /api/app/employees/{id}/role": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Role unchanged."
   }
  },
  "POST /api/app/employees/{id}/client": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Client link unchanged."
   }
  },
  "POST /api/app/employees/{id}/sessions/{sid}/revoke": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Session revoked."
   }
  },
  "POST /api/app/employees/{id}/revoke-all-sessions": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "All sessions revoked (0).",
    "count": 0
   }
  },
  "POST /api/app/employees/{id}/active": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "User status updated."
   }
  },
  "POST /api/app/employees/invites": {
   "status": 201,
   "body": {
    "ok": true,
    "message": "Invite created.",
    "id": 1,
    "code": "INV-<redacted>",
    "role": "STAFF"
   }
  },
  "POST /api/app/employees/invites/{id}/revoke": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Invite revoked."
   }
  },
  "GET /api/app/today/staff-certs (Phase 2 reuse)": {
   "status": 200,
   "body": {
    "ok": true,
    "directory": {
     "staff": [
      {
       "id": 1,
       "displayName": "Capture Owner",
       "role": "OWNER",
       "portalLang": "en",
       "clientId": null,
       "jobTitle": "",
       "email": "",
       "phone": "",
       "active": true,
       "createdAt": 1793745471863,
       "certs": [],
       "training": {
        "inProgress": 0,
        "completed": 0,
        "abandoned": 0
       },
       "equipment": []
      },
      {
       "id": 3,
       "displayName": "Capture Second",
       "role": "STAFF",
       "portalLang": "en",
       "clientId": null,
       "jobTitle": "Senior Analyst",
       "email": "second@capture.invalid",
       "phone": "555-0112",
       "active": true,
       "createdAt": 1793745473252,
       "certs": [],
       "training": {
        "inProgress": 0,
        "completed": 0,
        "abandoned": 0
       },
       "equipment": []
      },
      {
       "id": 2,
       "displayName": "Capture Tech",
       "role": "STAFF",
       "portalLang": "en",
       "clientId": null,
       "jobTitle": "Field Technician",
       "email": "tech@capture.invalid",
       "phone": "555-0110",
       "active": true,
       "createdAt": 1793745472165,
       "certs": [],
       "training": {
        "inProgress": 0,
        "completed": 0,
        "abandoned": 0
       },
       "equipment": []
      }
     ],
     "total": 3,
     "ok": true
    },
    "training": {
     "trainings": [],
     "counts": {
      "inProgress": 0,
      "completed": 0,
      "abandoned": 0
     },
     "ok": true
    }
   }
  },
  "POST /api/app/assets (Sentinel)": {
   "status": 201,
   "body": {
    "ok": true,
    "message": "Asset saved.",
    "id": 1,
    "asset": {
     "id": 1,
     "clientId": null,
     "assignedUserId": 2,
     "name": "Capture Laptop",
     "type": "Laptop",
     "identifier": "SN-CAP-001",
     "reviewDueDate": "2027-02-01",
     "scope": "Sentinel",
     "status": "In Service",
     "warrantyUntil": "2026-11-13",
     "warrantyExpires": null,
     "supportExpires": null,
     "qrCode": "SCA-IH\u2026<truncated>",
     "notes": "capture",
     "archivedAt": null,
     "archivedBy": null,
     "createdAt": 1793745475832,
     "updatedAt": 1793745475917
    }
   }
  },
  "POST /api/app/assets (Client)": {
   "status": 201,
   "body": {
    "ok": true,
    "message": "Asset saved.",
    "id": 2,
    "asset": {
     "id": 2,
     "clientId": 1,
     "assignedUserId": null,
     "name": "Capture Firewall",
     "type": "Firewall",
     "identifier": "SN-CAP-002",
     "reviewDueDate": "2026-10-29",
     "scope": "Client",
     "status": "In Service",
     "warrantyUntil": "2026-11-01",
     "warrantyExpires": null,
     "supportExpires": null,
     "qrCode": "SCA-ZB\u2026<truncated>",
     "notes": "",
     "archivedAt": null,
     "archivedBy": null,
     "createdAt": 1793745476119,
     "updatedAt": 1793745476220
    }
   }
  },
  "GET /api/app/assets": {
   "status": 200,
   "body": {
    "ok": true,
    "total": 2,
    "limit": 50,
    "offset": 0,
    "assets": [
     {
      "id": 2,
      "clientId": 1,
      "assignedUserId": null,
      "name": "Capture Firewall",
      "type": "Firewall",
      "identifier": "SN-CAP-002",
      "reviewDueDate": "2026-10-29",
      "scope": "Client",
      "status": "In Service",
      "warrantyUntil": "2026-11-01",
      "warrantyExpires": null,
      "supportExpires": null,
      "qrCode": "SCA-ZB\u2026<truncated>",
      "notes": "",
      "archivedAt": null,
      "archivedBy": null,
      "createdAt": 1793745476119,
      "updatedAt": 1793745476220,
      "clientName": "Capture Client LLC",
      "assigneeName": "",
      "ownerName": "Capture Client LLC",
      "flags": {
       "reviewOverdue": true,
       "warrantyExpired": true,
       "warrantyExpiring": false,
       "attention": true
      },
      "renewals": [
       {
        "field": "review",
        "date": "2026-10-29",
        "daysLeft": -5,
        "bucket": "30",
        "state": "expired"
       },
       {
        "field": "warranty",
        "date": "2026-11-01",
        "daysLeft": -2,
        "bucket": "30",
        "state": "expired"
       }
      ],
      "earliestRenewal": {
       "field": "review",
       "date": "2026-10-29",
       "daysLeft": -5,
       "bucket": "30",
       "state": "expired"
      }
     },
     {
      "id": 1,
      "clientId": null,
      "assignedUserId": 2,
      "name": "Capture Laptop",
      "type": "Laptop",
      "identifier": "SN-CAP-001",
      "reviewDueDate": "2027-02-01",
      "scope": "Sentinel",
      "status": "In Service",
      "warrantyUntil": "2026-11-13",
      "warrantyExpires": null,
      "supportExpires": null,
      "qrCode": "SCA-IH\u2026<truncated>",
      "notes": "capture",
      "archivedAt": null,
      "archivedBy": null,
      "createdAt": 1793745475832,
      "updatedAt": 1793745475917,
      "clientName": "",
      "assigneeName": "Capture Tech",
      "ownerName": "Sentinel Safeguard",
      "flags": {
       "reviewOverdue": false,
       "warrantyExpired": false,
       "warrantyExpiring": true,
       "attention": true
      },
      "renewals": [
       {
        "field": "warranty",
        "date": "2026-11-13",
        "daysLeft": 10,
        "bucket": "30",
        "state": "due"
       },
       {
        "field": "review",
        "date": "2027-02-01",
        "daysLeft": 90,
        "bucket": "90",
        "state": "due"
       }
      ],
      "earliestRenewal": {
       "field": "warranty",
       "date": "2026-11-13",
       "daysLeft": 10,
       "bucket": "30",
       "state": "due"
      }
     }
    ],
    "stats": {
     "total": 2,
     "sentinel": 1,
     "client": 1,
     "warrantyExpiring": 1,
     "reviewsOverdue": 1,
     "attention": 2
    },
    "types": [
     {
      "type": "Firewall",
      "count": 1
     },
     {
      "type": "Laptop",
      "count": 1
     }
    ]
   }
  },
  "GET /api/app/assets?alert=alerts": {
   "status": 200,
   "body": {
    "ok": true,
    "total": 2,
    "limit": 50,
    "offset": 0,
    "assets": [
     {
      "id": 2,
      "clientId": 1,
      "assignedUserId": null,
      "name": "Capture Firewall",
      "type": "Firewall",
      "identifier": "SN-CAP-002",
      "reviewDueDate": "2026-10-29",
      "scope": "Client",
      "status": "In Service",
      "warrantyUntil": "2026-11-01",
      "warrantyExpires": null,
      "supportExpires": null,
      "qrCode": "SCA-ZB\u2026<truncated>",
      "notes": "",
      "archivedAt": null,
      "archivedBy": null,
      "createdAt": 1793745476119,
      "updatedAt": 1793745476220,
      "clientName": "Capture Client LLC",
      "assigneeName": "",
      "ownerName": "Capture Client LLC",
      "flags": {
       "reviewOverdue": true,
       "warrantyExpired": true,
       "warrantyExpiring": false,
       "attention": true
      },
      "renewals": [
       {
        "field": "review",
        "date": "2026-10-29",
        "daysLeft": -5,
        "bucket": "30",
        "state": "expired"
       },
       {
        "field": "warranty",
        "date": "2026-11-01",
        "daysLeft": -2,
        "bucket": "30",
        "state": "expired"
       }
      ],
      "earliestRenewal": {
       "field": "review",
       "date": "2026-10-29",
       "daysLeft": -5,
       "bucket": "30",
       "state": "expired"
      }
     },
     {
      "id": 1,
      "clientId": null,
      "assignedUserId": 2,
      "name": "Capture Laptop",
      "type": "Laptop",
      "identifier": "SN-CAP-001",
      "reviewDueDate": "2027-02-01",
      "scope": "Sentinel",
      "status": "In Service",
      "warrantyUntil": "2026-11-13",
      "warrantyExpires": null,
      "supportExpires": null,
      "qrCode": "SCA-IH\u2026<truncated>",
      "notes": "capture",
      "archivedAt": null,
      "archivedBy": null,
      "createdAt": 1793745475832,
      "updatedAt": 1793745475917,
      "clientName": "",
      "assigneeName": "Capture Tech",
      "ownerName": "Sentinel Safeguard",
      "flags": {
       "reviewOverdue": false,
       "warrantyExpired": false,
       "warrantyExpiring": true,
       "attention": true
      },
      "renewals": [
       {
        "field": "warranty",
        "date": "2026-11-13",
        "daysLeft": 10,
        "bucket": "30",
        "state": "due"
       },
       {
        "field": "review",
        "date": "2027-02-01",
        "daysLeft": 90,
        "bucket": "90",
        "state": "due"
       }
      ],
      "earliestRenewal": {
       "field": "warranty",
       "date": "2026-11-13",
       "daysLeft": 10,
       "bucket": "30",
       "state": "due"
      }
     }
    ],
    "stats": {
     "total": 2,
     "sentinel": 1,
     "client": 1,
     "warrantyExpiring": 1,
     "reviewsOverdue": 1,
     "attention": 2
    },
    "types": [
     {
      "type": "Firewall",
      "count": 1
     },
     {
      "type": "Laptop",
      "count": 1
     }
    ]
   }
  },
  "GET /api/app/assets/{id}": {
   "status": 200,
   "body": {
    "asset": {
     "id": 2,
     "clientId": 1,
     "assignedUserId": null,
     "name": "Capture Firewall",
     "type": "Firewall",
     "identifier": "SN-CAP-002",
     "reviewDueDate": "2026-10-29",
     "scope": "Client",
     "status": "In Service",
     "warrantyUntil": "2026-11-01",
     "warrantyExpires": null,
     "supportExpires": null,
     "qrCode": "SCA-ZB\u2026<truncated>",
     "notes": "",
     "archivedAt": null,
     "archivedBy": null,
     "createdAt": 1793745476119,
     "updatedAt": 1793745476220
    },
    "client": {
     "id": 1,
     "legalName": "Capture Client LLC",
     "dba": "",
     "type": "Corporate",
     "industry": "Testing",
     "status": "Active",
     "stage": "Lead",
     "value": 1000.0,
     "nextFollowUp": null,
     "source": "",
     "sourceDetail": "",
     "contactName": "Pat C",
     "contactTitle": "",
     "email": "pat@capture.invalid",
     "phone": "555-0111",
     "street": "",
     "city": "",
     "state": "",
     "zip": "",
     "notes": "",
     "tags": "",
     "friends": false,
     "mergedIntoId": null,
     "importBatchId": null,
     "archivedAt": null,
     "archivedBy": null,
     "createdAt": 1793745472412,
     "updatedAt": 1793745472412
    },
    "activity": [
     {
      "id": 19,
      "actorId": 1,
      "action": "Asset_SAVED",
      "entityType": "asset",
      "entityId": 2,
      "detail": "Asset saved.",
      "createdAt": 1793745476119
     }
    ],
    "ok": true,
    "flags": {
     "reviewOverdue": true,
     "warrantyExpired": true,
     "warrantyExpiring": false,
     "attention": true
    },
    "renewals": [
     {
      "field": "review",
      "date": "2026-10-29",
      "daysLeft": -5,
      "bucket": "30",
      "state": "expired"
     },
     {
      "field": "warranty",
      "date": "2026-11-01",
      "daysLeft": -2,
      "bucket": "30",
      "state": "expired"
     }
    ],
    "earliestRenewal": {
     "field": "review",
     "date": "2026-10-29",
     "daysLeft": -5,
     "bucket": "30",
     "state": "expired"
    }
   }
  },
  "GET /api/app/assets/renewals": {
   "status": 200,
   "body": {
    "ok": true,
    "flagged": {
     "total": 2,
     "items": [
      {
       "id": 2,
       "clientId": 1,
       "assignedUserId": null,
       "name": "Capture Firewall",
       "type": "Firewall",
       "identifier": "SN-CAP-002",
       "reviewDueDate": "2026-10-29",
       "scope": "Client",
       "status": "In Service",
       "warrantyUntil": "2026-11-01",
       "warrantyExpires": null,
       "supportExpires": null,
       "qrCode": "SCA-ZB\u2026<truncated>",
       "notes": "",
       "archivedAt": null,
       "archivedBy": null,
       "createdAt": 1793745476119,
       "updatedAt": 1793745476220,
       "flags": [
        {
         "field": "review",
         "date": "2026-10-29",
         "state": "expired"
        },
        {
         "field": "warranty",
         "date": "2026-11-01",
         "state": "expired"
        }
       ]
      },
      {
       "id": 1,
       "clientId": null,
       "assignedUserId": 2,
       "name": "Capture Laptop",
       "type": "Laptop",
       "identifier": "SN-CAP-001",
       "reviewDueDate": "2027-02-01",
       "scope": "Sentinel",
       "status": "In Service",
       "warrantyUntil": "2026-11-13",
       "warrantyExpires": null,
       "supportExpires": null,
       "qrCode": "SCA-IH\u2026<truncated>",
       "notes": "capture",
       "archivedAt": null,
       "archivedBy": null,
       "createdAt": 1793745475832,
       "updatedAt": 1793745475917,
       "flags": [
        {
         "field": "warranty",
         "date": "2026-11-13",
         "state": "due"
        }
       ]
      }
     ]
    },
    "renewals": {
     "total": 2,
     "within30": {
      "total": 2,
      "items": [
       {
        "id": 2,
        "clientId": 1,
        "assignedUserId": null,
        "name": "Capture Firewall",
        "type": "Firewall",
        "identifier": "SN-CAP-002",
        "reviewDueDate": "2026-10-29",
        "scope": "Client",
        "status": "In Service",
        "warrantyUntil": "2026-11-01",
        "warrantyExpires": null,
        "supportExpires": null,
        "qrCode": "SCA-ZB\u2026<truncated>",
        "notes": "",
        "archivedAt": null,
        "archivedBy": null,
        "createdAt": 1793745476119,
        "updatedAt": 1793745476220,
        "renewals": [
         {
          "field": "review",
          "date": "2026-10-29",
          "daysLeft": -5,
          "bucket": "30",
          "state": "expired"
         },
         {
          "field": "warranty",
          "date": "2026-11-01",
          "daysLeft": -2,
          "bucket": "30",
          "state": "expired"
         }
        ],
        "earliest": {
         "field": "review",
         "date": "2026-10-29",
         "daysLeft": -5,
         "bucket": "30",
         "state": "expired"
        }
       },
       {
        "id": 1,
        "clientId": null,
        "assignedUserId": 2,
        "name": "Capture Laptop",
        "type": "Laptop",
        "identifier": "SN-CAP-001",
        "reviewDueDate": "2027-02-01",
        "scope": "Sentinel",
        "status": "In Service",
        "warrantyUntil": "2026-11-13",
        "warrantyExpires": null,
        "supportExpires": null,
        "qrCode": "SCA-IH\u2026<truncated>",
        "notes": "capture",
        "archivedAt": null,
        "archivedBy": null,
        "createdAt": 1793745475832,
        "updatedAt": 1793745475917,
        "renewals": [
         {
          "field": "warranty",
          "date": "2026-11-13",
          "daysLeft": 10,
          "bucket": "30",
          "state": "due"
         },
         {
          "field": "review",
          "date": "2027-02-01",
          "daysLeft": 90,
          "bucket": "90",
          "state": "due"
         }
        ],
        "earliest": {
         "field": "warranty",
         "date": "2026-11-13",
         "daysLeft": 10,
         "bucket": "30",
         "state": "due"
        }
       }
      ]
     },
     "within60": {
      "total": 0,
      "items": []
     },
     "within90": {
      "total": 0,
      "items": []
     },
     "items": [
      {
       "id": 2,
       "clientId": 1,
       "assignedUserId": null,
       "name": "Capture Firewall",
       "type": "Firewall",
       "identifier": "SN-CAP-002",
       "reviewDueDate": "2026-10-29",
       "scope": "Client",
       "status": "In Service",
       "warrantyUntil": "2026-11-01",
       "warrantyExpires": null,
       "supportExpires": null,
       "qrCode": "SCA-ZB\u2026<truncated>",
       "notes": "",
       "archivedAt": null,
       "archivedBy": null,
       "createdAt": 1793745476119,
       "updatedAt": 1793745476220,
       "renewals": [
        {
         "field": "review",
         "date": "2026-10-29",
         "daysLeft": -5,
         "bucket": "30",
         "state": "expired"
        },
        {
         "field": "warranty",
         "date": "2026-11-01",
         "daysLeft": -2,
         "bucket": "30",
         "state": "expired"
        }
       ],
       "earliest": {
        "field": "review",
        "date": "2026-10-29",
        "daysLeft": -5,
        "bucket": "30",
        "state": "expired"
       }
      },
      {
       "id": 1,
       "clientId": null,
       "assignedUserId": 2,
       "name": "Capture Laptop",
       "type": "Laptop",
       "identifier": "SN-CAP-001",
       "reviewDueDate": "2027-02-01",
       "scope": "Sentinel",
       "status": "In Service",
       "warrantyUntil": "2026-11-13",
       "warrantyExpires": null,
       "supportExpires": null,
       "qrCode": "SCA-IH\u2026<truncated>",
       "notes": "capture",
       "archivedAt": null,
       "archivedBy": null,
       "createdAt": 1793745475832,
       "updatedAt": 1793745475917,
       "renewals": [
        {
         "field": "warranty",
         "date": "2026-11-13",
         "daysLeft": 10,
         "bucket": "30",
         "state": "due"
        },
        {
         "field": "review",
         "date": "2027-02-01",
         "daysLeft": 90,
         "bucket": "90",
         "state": "due"
        }
       ],
       "earliest": {
        "field": "warranty",
        "date": "2026-11-13",
        "daysLeft": 10,
        "bucket": "30",
        "state": "due"
       }
      }
     ]
    }
   }
  },
  "PATCH /api/app/assets/{id}": {
   "status": 200,
   "body": {
    "asset": {
     "id": 1,
     "clientId": null,
     "assignedUserId": 2,
     "name": "Capture Laptop Pro",
     "type": "Laptop",
     "identifier": "SN-CAP-001",
     "reviewDueDate": "2027-02-01",
     "scope": "Sentinel",
     "status": "In Repair",
     "warrantyUntil": "2026-11-13",
     "warrantyExpires": null,
     "supportExpires": null,
     "qrCode": "SCA-IH\u2026<truncated>",
     "notes": "capture",
     "archivedAt": null,
     "archivedBy": null,
     "createdAt": 1793745475832,
     "updatedAt": 1793745476730
    },
    "client": null,
    "activity": [
     {
      "id": 21,
      "actorId": 1,
      "action": "ASSET_STATUS_CHANGED",
      "entityType": "asset",
      "entityId": 1,
      "detail": "Asset status changed from In Service to In Repair for Capture Laptop Pro.",
      "createdAt": 1793745476730
     },
     {
      "id": 20,
      "actorId": 1,
      "action": "Asset_SAVED",
      "entityType": "asset",
      "entityId": 1,
      "detail": "Asset saved.",
      "createdAt": 1793745476685
     },
     {
      "id": 18,
      "actorId": 1,
      "action": "Asset_SAVED",
      "entityType": "asset",
      "entityId": 1,
      "detail": "Asset saved.",
      "createdAt": 1793745475833
     }
    ],
    "ok": true,
    "flags": {
     "reviewOverdue": false,
     "warrantyExpired": false,
     "warrantyExpiring": true,
     "attention": true
    },
    "renewals": [
     {
      "field": "warranty",
      "date": "2026-11-13",
      "daysLeft": 10,
      "bucket": "30",
      "state": "due"
     },
     {
      "field": "review",
      "date": "2027-02-01",
      "daysLeft": 90,
      "bucket": "90",
      "state": "due"
     }
    ],
    "earliestRenewal": {
     "field": "warranty",
     "date": "2026-11-13",
     "daysLeft": 10,
     "bucket": "30",
     "state": "due"
    }
   }
  },
  "POST /api/app/assets/{id}/status": {
   "status": 200,
   "body": {
    "ok": true,
    "id": 1,
    "status": "In Storage"
   }
  },
  "POST /api/app/assets/{id}/qr/ensure": {
   "status": 200,
   "body": {
    "ok": true,
    "assetId": 2,
    "code": "SCA-ZB\u2026<truncated>",
    "payload": "https://sentinelsafeguard.net/a/SCA-ZBc2nNIP3NBpIQCQq-zBza",
    "svg": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"164\" height=\"164\" viewBox=\"0 0 16\u2026<svg truncated>"
   }
  },
  "POST /api/app/assets/{id}/qr/regenerate": {
   "status": 200,
   "body": {
    "ok": true,
    "assetId": 2,
    "code": "SCA-mj\u2026<truncated>",
    "payload": "https://sentinelsafeguard.net/a/SCA-mjbYu7nytUHFzUnIrN-GpS",
    "svg": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"164\" height=\"164\" viewBox=\"0 0 16\u2026<svg truncated>"
   }
  },
  "GET /api/app/assets/resolve?code= (pre-existing)": {
   "status": 200,
   "body": {
    "authorized": true,
    "ok": true,
    "found": true,
    "message": "No asset matches that code.",
    "asset": {
     "id": 2,
     "clientId": 1,
     "assignedUserId": null,
     "name": "Capture Firewall",
     "type": "Firewall",
     "identifier": "SN-CAP-002",
     "reviewDueDate": "2026-10-29",
     "scope": "Client",
     "status": "In Service",
     "warrantyUntil": "2026-11-01",
     "warrantyExpires": null,
     "supportExpires": null,
     "qrCode": "SCA-mj\u2026<truncated>",
     "notes": "",
     "archivedAt": null,
     "archivedBy": null,
     "createdAt": 1793745476119,
     "updatedAt": 1793745477019
    }
   }
  },
  "GET /api/app/assets/labels": {
   "status": 200,
   "body": {
    "labels": [
     {
      "assetId": 1,
      "name": "Capture Laptop Pro",
      "type": "Laptop",
      "identifier": "SN-CAP-001",
      "code": "SCA-IH\u2026<truncated>",
      "payload": "https://sentinelsafeguard.net/a/SCA-IHl2xrAX025JGCwe94KGfd",
      "svg": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"164\" height=\"164\" viewBox=\"0 0 16\u2026<svg truncated>"
     },
     {
      "assetId": 2,
      "name": "Capture Firewall",
      "type": "Firewall",
      "identifier": "SN-CAP-002",
      "code": "SCA-mj\u2026<truncated>",
      "payload": "https://sentinelsafeguard.net/a/SCA-mjbYu7nytUHFzUnIrN-GpS",
      "svg": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"164\" height=\"164\" viewBox=\"0 0 16\u2026<svg truncated>"
     }
    ],
    "ok": true
   }
  },
  "GET /api/app/assets/url-labels": {
   "status": 200,
   "body": {
    "labels": [
     {
      "assetId": 1,
      "name": "Capture Laptop Pro",
      "type": "Laptop",
      "identifier": "SN-CAP-001",
      "serial": "SN-CAP-001",
      "url": "https://command.sentinelsafeguard.net/#asset=1",
      "svg": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"148\" height=\"148\" viewBox=\"0 0 14\u2026<svg truncated>"
     },
     {
      "assetId": 2,
      "name": "Capture Firewall",
      "type": "Firewall",
      "identifier": "SN-CAP-002",
      "serial": "SN-CAP-002",
      "url": "https://command.sentinelsafeguard.net/#asset=2",
      "svg": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"148\" height=\"148\" viewBox=\"0 0 14\u2026<svg truncated>"
     }
    ],
    "urlFormat": "https://command.sentinelsafeguard.net/#asset=<assetId>",
    "ok": true
   }
  },
  "POST /api/app/assets/{id}/archive": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Asset archived."
   }
  },
  "POST /api/app/assets/{id}/restore": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Asset restored."
   }
  },
  "POST /api/app/forms": {
   "status": 201,
   "body": {
    "ok": true,
    "message": "Form saved.",
    "id": 1,
    "form": {
     "id": 1,
     "clientId": 1,
     "type": "Engagement Letter",
     "title": "Capture Engagement",
     "dataJson": "{\"client\": \"Capture\"}",
     "createdAt": 1793745477459,
     "updatedAt": 1793745477459
    }
   }
  },
  "GET /api/app/forms": {
   "status": 200,
   "body": {
    "ok": true,
    "total": 1,
    "limit": 50,
    "offset": 0,
    "forms": [
     {
      "id": 1,
      "clientId": 1,
      "type": "Engagement Letter",
      "title": "Capture Engagement",
      "dataJson": "{\"client\": \"Capture\"}",
      "createdAt": 1793745477459,
      "updatedAt": 1793745477459,
      "clientName": "Capture Client LLC"
     }
    ]
   }
  },
  "GET /api/app/forms/{id}": {
   "status": 200,
   "body": {
    "ok": true,
    "form": {
     "id": 1,
     "clientId": 1,
     "type": "Engagement Letter",
     "title": "Capture Engagement",
     "dataJson": "{\"client\": \"Capture\"}",
     "createdAt": 1793745477459,
     "updatedAt": 1793745477459,
     "clientName": "Capture Client LLC"
    }
   }
  },
  "PATCH /api/app/forms/{id}": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Form saved.",
    "id": 1,
    "form": {
     "id": 1,
     "clientId": 1,
     "type": "Engagement Letter",
     "title": "Capture Engagement v2",
     "dataJson": "{\"client\": \"Capture\"}",
     "createdAt": 1793745477459,
     "updatedAt": 1793745477652
    }
   }
  },
  "POST /api/app/form-files/upload": {
   "status": 200,
   "body": {
    "ok": true,
    "id": 1,
    "message": "Form document stored."
   }
  },
  "GET /api/app/form-files": {
   "status": 200,
   "body": {
    "files": [
     {
      "id": 1,
      "displayName": "Capture PDF",
      "originalFilename": "capture.pdf",
      "sizeBytes": 48,
      "sha256": "d7dd0115be8b79ae057b3f6ca0fcee578085ba6919dcb70e8643a2aff537d9b5",
      "note": "capture note",
      "uploadedBy": 1,
      "uploadedByName": "Capture Owner",
      "createdAt": 1793745477749,
      "updatedAt": 1793745477749
     }
    ],
    "ok": true,
    "total": 1
   }
  },
  "GET /api/app/form-files/{id}/download": {
   "status": 200,
   "bodyBytes": 48,
   "bodyPrefix": "%PDF-1.4\n1 0 obj"
  },
  "PATCH /api/app/form-files/{id}": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Form document updated.",
    "id": 1
   }
  },
  "DELETE /api/app/form-files/{id}": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Form document deleted.",
    "id": 1
   }
  },
  "DELETE /api/app/forms/{id}": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Form deleted."
   }
  },
  "DELETE /api/app/assets/{id}": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Asset deleted."
   }
  },
  "DELETE /api/app/employees/{id}": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "User deleted."
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

    fun bodyBytes(name: String): Int =
        bodies[name]!!.jsonObject["bodyBytes"]!!.jsonPrimitive.int

    fun bodyPrefix(name: String): String =
        bodies[name]!!.jsonObject["bodyPrefix"]!!.jsonPrimitive.content
}

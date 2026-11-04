package net.sentinelsafeguard.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Phase 8B REAL response bodies, captured 2026-11-03 from a
 * rehearsal boot of the exact Phase 8B candidate bytes
 * (server.py sha256 6ffee506507ef6a315f6fe7d7633d01fae270cc53e1bbdcf2ded02a7466dc2ce,
 * Addendum 26 - the deployed production bytes), against a seeded
 * scratch database. Seed data is fictional; shown-once values are
 * recorded as SHAPE ONLY (length + sha256 prefix); recovery codes
 * as count + lengths only; Law AI bodies are the handler's masked
 * public state. [ALL] is phase8b-work/fixtures/real-bodies.json
 * verbatim, re-keyed from capture names to route names (a route
 * captured under two callers keeps the capture name in brackets).
 */
object RealBodiesPhase8B {
    val ALL: String = """{
 "_meta": {
  "serverPySha256": "6ffee506507ef6a315f6fe7d7633d01fae270cc53e1bbdcf2ded02a7466dc2ce",
  "capturedFrom": "rehearsal boot of the exact Phase 8B candidate bytes (deployed production, Addendum 26)",
  "note": "Real bodies; shown-once values redacted to shape by the backend crew (key/hook/survey: length+sha256 prefix; recovery codes: count+lengths only); Law AI bodies are the handler's masked public state; no secret value appears."
 },
 "bodies": {
  "GET /api/app/admin": {
   "method": "GET",
   "path": "/api/app/admin",
   "status": 200,
   "body": {
    "isOwner": true,
    "users": [
     {
      "id": 1,
      "displayName": "OwnerCap8B",
      "role": "OWNER",
      "portalLang": "en",
      "clientId": null,
      "jobTitle": "",
      "email": "",
      "phone": "",
      "active": true,
      "createdAt": 1793763011465,
      "suspendUntil": null,
      "mustReset": false,
      "pendingSetup": false,
      "activationDelivery": "",
      "activationExpiresAt": null,
      "totpEnrolled": false,
      "modNote": "",
      "lastActivityAt": 1793763013265
     },
     {
      "id": 2,
      "displayName": "StaffCap8B",
      "role": "STAFF",
      "portalLang": "en",
      "clientId": null,
      "jobTitle": "Tech",
      "email": "staff@capture8b.example.invalid",
      "phone": "555-0108",
      "active": true,
      "createdAt": 1793763011800,
      "suspendUntil": null,
      "mustReset": false,
      "pendingSetup": false,
      "activationDelivery": "",
      "activationExpiresAt": null,
      "totpEnrolled": false,
      "modNote": "",
      "lastActivityAt": 1793763012546
     },
     {
      "id": 3,
      "displayName": "NoPermCap8B",
      "role": "NoPermCap8B",
      "portalLang": "en",
      "clientId": null,
      "jobTitle": "",
      "email": "",
      "phone": "",
      "active": true,
      "createdAt": 1793763012204,
      "suspendUntil": null,
      "mustReset": false,
      "pendingSetup": false,
      "activationDelivery": "",
      "activationExpiresAt": null,
      "totpEnrolled": false,
      "modNote": "",
      "lastActivityAt": 1793763012710
     }
    ],
    "invites": [
     {
      "id": 1,
      "role": "NoPermCap8B",
      "active": false,
      "redeemedAt": 1793763012120,
      "createdAt": 1793763012026,
      "createdBy": 1,
      "expiresAt": 1794367812026
     }
    ],
    "recoveryCodes": {
     "total": 10,
     "unused": 10
    },
    "settings": {
     "ownerInitialized": true,
     "publicSiteUrl": "",
     "recoveryIssuedAt": 1793763011465,
     "securityAlert": "",
     "securityAlertAt": null,
     "sessionTtlHours": 8,
     "lockdown": false,
     "activityRetentionDays": 0,
     "workspaceName": "",
     "requireTotpAdminWrite": false,
     "authFailLimit": 10,
     "authFailWindowMinutes": 10
    },
    "overview": {
     "counts": {
      "clients": 2,
      "cases": 0,
      "tickets": 1,
      "assets": 0,
      "invoices": 0,
      "knowledge": 0,
      "forms": 0,
      "jobs": 0,
      "users": 3
     },
     "usersByRole": {
      "NoPermCap8B": 1,
      "OWNER": 1,
      "STAFF": 1
     },
     "activity24h": 12,
     "failed24h": 0,
     "activeSessions": 1
    },
    "activity": [
     {
      "id": 12,
      "actorId": 1,
      "action": "TICKET_SAVED",
      "entityType": "ticket",
      "entityId": 1,
      "detail": "Ticket saved (via app).",
      "createdAt": 1793763013265
     },
     {
      "id": 11,
      "actorId": 1,
      "action": "CLIENT_SAVED",
      "entityType": "client",
      "entityId": 2,
      "detail": "Client saved.",
      "createdAt": 1793763013048
     },
     {
      "id": 10,
      "actorId": 1,
      "action": "CLIENT_SAVED",
      "entityType": "client",
      "entityId": 1,
      "detail": "Client saved.",
      "createdAt": 1793763012880
     },
     {
      "id": 9,
      "actorId": 3,
      "action": "APP_LOGIN",
      "entityType": "user",
      "entityId": 3,
      "detail": "App sign-in \u2014 device enrolled: cap8b.",
      "createdAt": 1793763012710
     },
     {
      "id": 8,
      "actorId": 2,
      "action": "APP_LOGIN",
      "entityType": "user",
      "entityId": 2,
      "detail": "App sign-in \u2014 device enrolled: cap8b.",
      "createdAt": 1793763012546
     },
     {
      "id": 7,
      "actorId": 1,
      "action": "APP_LOGIN",
      "entityType": "user",
      "entityId": 1,
      "detail": "App sign-in \u2014 device enrolled: cap8b.",
      "createdAt": 1793763012396
     },
     {
      "id": 6,
      "actorId": 3,
      "action": "INVITE_REDEEMED",
      "entityType": "invite",
      "entityId": 1,
      "detail": "NoPermCap8B account created via invite.",
      "createdAt": 1793763012204
     },
     {
      "id": 5,
      "actorId": 1,
      "action": "INVITE_CREATED",
      "entityType": "invite",
      "entityId": 1,
      "detail": "Invite created for role NoPermCap8B.",
      "createdAt": 1793763012026
     },
     {
      "id": 4,
      "actorId": 1,
      "action": "ROLE_CREATED",
      "entityType": "role",
      "entityId": null,
      "detail": "Role 'NoPermCap8B' created (cases read).",
      "createdAt": 1793763011944
     },
     {
      "id": 3,
      "actorId": 1,
      "action": "EMPLOYEE_CREATED",
      "entityType": "user",
      "entityId": 2,
      "detail": "Staff user StaffCap8B created.",
      "createdAt": 1793763011800
     },
     {
      "id": 2,
      "actorId": 1,
      "action": "LOGIN",
      "entityType": "session",
      "entityId": 1,
      "detail": "Session opened; expires in 8 hours",
      "createdAt": 1793763011636
     },
     {
      "id": 1,
      "actorId": 1,
      "action": "OWNER_INITIALIZED",
      "entityType": "settings",
      "entityId": 1,
      "detail": "Owner account initialized; recovery codes issued.",
      "createdAt": 1793763011466
     }
    ],
    "ok": true
   }
  },
  "GET /api/app/admin [admin_data_noperm_403]": {
   "method": "GET",
   "path": "/api/app/admin",
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Not authorized."
   }
  },
  "GET /api/app/admin/health-strip": {
   "method": "GET",
   "path": "/api/app/admin/health-strip",
   "status": 200,
   "body": {
    "asOf": "2026-11-04T03:30:13+00:00",
    "services": {
     "sentinel-command": "unknown",
     "sentinel-live": "unknown",
     "postfix": "unknown",
     "dovecot": "unknown",
     "rspamd": "unknown",
     "cloudflared": "unknown"
    },
    "disk": {
     "/": {
      "totalBytes": 8053063680,
      "usedBytes": 306929664,
      "freeBytes": 7475699712,
      "usedPct": 3.8,
      "paths": [
       "/",
       "/var/backups",
       "/var/mail"
      ]
     }
    },
    "mailQueue": null,
    "backups": {
     "command": {
      "lastSuccess": null,
      "source": "no readable success signal"
     },
     "sentinel-live": {
      "lastSuccess": null,
      "source": "no readable success signal"
     },
     "mail": {
      "lastSuccess": null,
      "source": "no readable success signal"
     }
    },
    "bans": null,
    "monitor": null,
    "backupCheck": null,
    "notes": [
     "Service sentinel-command: unit not found on this host.",
     "Service sentinel-live: unit not found on this host.",
     "Service postfix: unit not found on this host.",
     "Service dovecot: unit not found on this host.",
     "Service rspamd: unit not found on this host.",
     "Service cloudflared: unit not found on this host.",
     "Mail queue depth unknown: postqueue is not available on this host.",
     "Backup chain 'command': last success unknown \u2014 no readable archive in /opt/sentinel-command/backups and no readable success state from sentinel-command-backup.service.",
     "Backup chain 'sentinel-live': last success unknown \u2014 no readable archive in /var/backups/sentinel-live and no readable success state from sentinel-live-backup.service.",
     "Backup chain 'mail': last success unknown \u2014 no readable archive in /var/backups/sentinel-mail and no readable success state from sentinel-mail-backup.service.",
     "Fail2ban ban counts omitted: fail2ban-client is not available on this host.",
     "Availability monitor results unavailable: the probe log could not be read.",
     "Backup integrity spot-check unavailable: the verdict file could not be read."
    ],
    "ok": true
   }
  },
  "GET /api/app/admin/roles": {
   "method": "GET",
   "path": "/api/app/admin/roles",
   "status": 200,
   "body": {
    "roles": [
     {
      "name": "OWNER",
      "color": "#00e676",
      "isSystem": true,
      "permissions": {
       "clients": "write",
       "cases": "write",
       "tickets": "write",
       "assets": "write",
       "invoices": "write",
       "knowledge": "write",
       "forms": "write",
       "schedule": "write",
       "employees": "write",
       "activity": "write",
       "admin": "write",
       "liveintel": "write",
       "law": "write",
       "osint": "write",
       "breachintel": "write",
       "threatintel": "write",
       "news": "write",
       "reviews": "write",
       "taxpack": "write"
      },
      "tabPermissions": {},
      "summary": "clients write, cases write, tickets write, assets write, invoices write, knowledge write, forms write, schedule write, employees write, activity write, admin write, liveintel write, law write, osint write, breachintel write, threatintel write, news write, reviews write, taxpack write",
      "memberCount": 1,
      "createdAt": 1793763010626,
      "updatedAt": 1793763010626
     },
     {
      "name": "STAFF",
      "color": "#7ee2a8",
      "isSystem": true,
      "permissions": {
       "clients": "none",
       "cases": "write",
       "tickets": "write",
       "assets": "write",
       "invoices": "none",
       "knowledge": "none",
       "forms": "write",
       "schedule": "read",
       "employees": "none",
       "activity": "none",
       "admin": "none",
       "liveintel": "none",
       "law": "none",
       "osint": "write",
       "breachintel": "write",
       "threatintel": "write",
       "news": "none",
       "reviews": "none",
       "taxpack": "none"
      },
      "tabPermissions": {},
      "summary": "cases write, tickets write, assets write, forms write, schedule read, osint write, breachintel write, threatintel write",
      "memberCount": 1,
      "createdAt": 1793763010626,
      "updatedAt": 1793763010626
     },
     {
      "name": "Family",
      "color": "#69f0ae",
      "isSystem": false,
      "permissions": {
       "clients": "none",
       "cases": "none",
       "tickets": "none",
       "assets": "none",
       "invoices": "none",
       "knowledge": "read",
       "forms": "none",
       "schedule": "none",
       "employees": "none",
       "activity": "none",
       "admin": "none",
       "liveintel": "none",
       "law": "none",
       "osint": "none",
       "breachintel": "none",
       "threatintel": "none",
       "news": "none",
       "reviews": "none",
       "taxpack": "none"
      },
      "tabPermissions": {},
      "summary": "knowledge read",
      "memberCount": 0,
      "createdAt": 1793763010627,
      "updatedAt": 1793763010627
     },
     {
      "name": "NoPermCap8B",
      "color": "#00e676",
      "isSystem": false,
      "permissions": {
       "clients": "none",
       "cases": "read",
       "tickets": "none",
       "assets": "none",
       "invoices": "none",
       "knowledge": "none",
       "forms": "none",
       "schedule": "none",
       "employees": "none",
       "activity": "none",
       "admin": "none",
       "liveintel": "none",
       "law": "none",
       "osint": "none",
       "breachintel": "none",
       "threatintel": "none",
       "news": "none",
       "reviews": "none",
       "taxpack": "none"
      },
      "tabPermissions": {},
      "summary": "cases read",
      "memberCount": 1,
      "createdAt": 1793763011944,
      "updatedAt": 1793763011944
     },
     {
      "name": "Subcontractor",
      "color": "#00e676",
      "isSystem": false,
      "permissions": {
       "clients": "none",
       "cases": "none",
       "tickets": "none",
       "assets": "none",
       "invoices": "none",
       "knowledge": "none",
       "forms": "none",
       "schedule": "none",
       "employees": "none",
       "activity": "none",
       "admin": "none",
       "liveintel": "none",
       "law": "none",
       "osint": "none",
       "breachintel": "none",
       "threatintel": "none",
       "news": "none",
       "reviews": "none",
       "taxpack": "none"
      },
      "tabPermissions": {},
      "summary": "no access",
      "memberCount": 0,
      "createdAt": 1793763010644,
      "updatedAt": 1793763010644
     }
    ],
    "modules": [
     "clients",
     "cases",
     "tickets",
     "assets",
     "invoices",
     "knowledge",
     "forms",
     "schedule",
     "employees",
     "activity",
     "admin",
     "liveintel",
     "law",
     "osint",
     "breachintel",
     "threatintel",
     "news",
     "reviews",
     "taxpack"
    ],
    "levels": [
     "none",
     "read",
     "write"
    ],
    "tabs": [
     {
      "key": "today",
      "label": "Today",
      "module": null
     },
     {
      "key": {
       "_shape_only": true,
       "length": 9,
       "sha256": "66cd9688a2ae0682"
      },
      "label": "Dashboard",
      "module": null
     },
     {
      "key": "clients",
      "label": "Clients",
      "module": "clients"
     },
     {
      "key": "cases",
      "label": "Cases",
      "module": "cases"
     },
     {
      "key": "tickets",
      "label": "Tickets",
      "module": "tickets"
     },
     {
      "key": "assets",
      "label": "Assets",
      "module": "assets"
     },
     {
      "key": {
       "_shape_only": true,
       "length": 8,
       "sha256": "491dabd42b00f84e"
      },
      "label": "Invoices",
      "module": "invoices"
     },
     {
      "key": "forms",
      "label": "Forms",
      "module": "forms"
     },
     {
      "key": {
       "_shape_only": true,
       "length": 8,
       "sha256": "5f72005e81c4ecbd"
      },
      "label": "Schedule",
      "module": "schedule"
     },
     {
      "key": {
       "_shape_only": true,
       "length": 9,
       "sha256": "c49de3d265fb1d97"
      },
      "label": "Employees",
      "module": "employees"
     },
     {
      "key": {
       "_shape_only": true,
       "length": 9,
       "sha256": "e0f895872d65b252"
      },
      "label": "Knowledge",
      "module": "knowledge"
     },
     {
      "key": {
       "_shape_only": true,
       "length": 9,
       "sha256": "844dcb1c9efcccd9"
      },
      "label": "Live Intel",
      "module": "liveintel"
     },
     {
      "key": "archive",
      "label": "Archive",
      "module": null
     },
     {
      "key": {
       "_shape_only": true,
       "length": 8,
       "sha256": "0651863bf5d90226"
      },
      "label": "Activity",
      "module": "activity"
     },
     {
      "key": "admin",
      "label": "Admin",
      "module": "admin"
     },
     {
      "key": "law",
      "label": "Dogg and Associates",
      "module": "law"
     },
     {
      "key": {
       "_shape_only": true,
       "length": 12,
       "sha256": "76774244af22101a"
      },
      "label": "Sentinel News",
      "module": "news"
     },
     {
      "key": {
       "_shape_only": true,
       "length": 8,
       "sha256": "8f8ab61bcdc75e29"
      },
      "label": "News Desk",
      "module": "news"
     },
     {
      "key": "osint",
      "label": "OSINT",
      "module": "osint"
     },
     {
      "key": {
       "_shape_only": true,
       "length": 11,
       "sha256": "1fbf60424846ae74"
      },
      "label": "Breach Intel",
      "module": "breachintel"
     },
     {
      "key": {
       "_shape_only": true,
       "length": 11,
       "sha256": "36d02be92a4826e5"
      },
      "label": "Threat Intel",
      "module": "threatintel"
     },
     {
      "key": "reviews",
      "label": "Reviews",
      "module": "reviews"
     }
    ],
    "ok": true
   }
  },
  "POST /api/app/admin/roles": {
   "method": "POST",
   "path": "/api/app/admin/roles",
   "status": 201,
   "body": {
    "ok": true,
    "message": "Role created.",
    "role": {
     "name": "CaptureRole8B",
     "color": "#42a5f5",
     "isSystem": false,
     "permissions": {
      "clients": "none",
      "cases": "none",
      "tickets": "read",
      "assets": "none",
      "invoices": "none",
      "knowledge": "none",
      "forms": "none",
      "schedule": "none",
      "employees": "none",
      "activity": "none",
      "admin": "none",
      "liveintel": "none",
      "law": "none",
      "osint": "none",
      "breachintel": "none",
      "threatintel": "none",
      "news": "none",
      "reviews": "none",
      "taxpack": "none"
     },
     "tabPermissions": {},
     "summary": "tickets read",
     "memberCount": 0,
     "createdAt": 1793763013997,
     "updatedAt": 1793763013997
    }
   }
  },
  "PATCH /api/app/admin/roles/CaptureRole8B": {
   "method": "PATCH",
   "path": "/api/app/admin/roles/CaptureRole8B",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Role updated.",
    "role": {
     "name": "CaptureRole8B",
     "color": "#66bb6a",
     "isSystem": false,
     "permissions": {
      "clients": "none",
      "cases": "none",
      "tickets": "read",
      "assets": "none",
      "invoices": "none",
      "knowledge": "none",
      "forms": "none",
      "schedule": "none",
      "employees": "none",
      "activity": "none",
      "admin": "none",
      "liveintel": "none",
      "law": "none",
      "osint": "none",
      "breachintel": "none",
      "threatintel": "none",
      "news": "none",
      "reviews": "none",
      "taxpack": "none"
     },
     "tabPermissions": {},
     "summary": "tickets read",
     "memberCount": 0,
     "createdAt": 1793763013997,
     "updatedAt": 1793763014197
    }
   }
  },
  "POST /api/app/admin/roles/CaptureRole8B/tabs": {
   "method": "POST",
   "path": "/api/app/admin/roles/CaptureRole8B/tabs",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Tab grants updated.",
    "role": {
     "name": "CaptureRole8B",
     "color": "#66bb6a",
     "isSystem": false,
     "permissions": {
      "clients": "none",
      "cases": "none",
      "tickets": "read",
      "assets": "none",
      "invoices": "none",
      "knowledge": "none",
      "forms": "none",
      "schedule": "none",
      "employees": "none",
      "activity": "none",
      "admin": "none",
      "liveintel": "none",
      "law": "none",
      "osint": "none",
      "breachintel": "none",
      "threatintel": "none",
      "news": "none",
      "reviews": "none",
      "taxpack": "none"
     },
     "tabPermissions": {},
     "summary": "tickets read",
     "memberCount": 0,
     "createdAt": 1793763013997,
     "updatedAt": 1793763014363
    }
   }
  },
  "DELETE /api/app/admin/roles/CaptureRole8B": {
   "method": "DELETE",
   "path": "/api/app/admin/roles/CaptureRole8B",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Role deleted."
   }
  },
  "GET /api/app/admin/ops-report": {
   "method": "GET",
   "path": "/api/app/admin/ops-report",
   "status": 200,
   "body": {
    "month": "2026-11",
    "generatedAt": "2026-11-04T03:30:14+00:00",
    "sections": {
     "pipeline": {
      "basis": "Current snapshot of the live (non-archived) client book \u2014 not a month-end reconstruction.",
      "totalClients": 2,
      "byStage": {
       "Lead": {
        "count": 2,
        "valueCents": 0
       },
       "Contacted": {
        "count": 0,
        "valueCents": 0
       },
       "Qualified": {
        "count": 0,
        "valueCents": 0
       },
       "Proposal": {
        "count": 0,
        "valueCents": 0
       },
       "Negotiation": {
        "count": 0,
        "valueCents": 0
       },
       "Active Client": {
        "count": 0,
        "valueCents": 0
       },
       "Past Client": {
        "count": 0,
        "valueCents": 0
       },
       "Lost": {
        "count": 0,
        "valueCents": 0
       }
      },
      "byTier": {
       "Tier A": {
        "count": 0,
        "valueCents": 0
       },
       "Tier B": {
        "count": 0,
        "valueCents": 0
       },
       "Tier C": {
        "count": 0,
        "valueCents": 0
       },
       "Tier D": {
        "count": 0,
        "valueCents": 0
       },
       "Partner": {
        "count": 0,
        "valueCents": 0
       },
       "Untagged": {
        "count": 2,
        "valueCents": 0
       }
      },
      "totalValueCents": 0
     },
     "tickets": {
      "basis": "Opened counts tickets created in the month; closed counts tickets whose stored closed_date falls in the month.",
      "opened": 1,
      "closed": 0,
      "openNow": 1
     },
     "cases": {
      "basis": "Opened counts cases whose opened_date falls in the month (creation time when no opened_date is stored); closed counts the stored closed_date; movements are counted from the activity log, the only movement history the database stores.",
      "opened": 0,
      "closed": 0,
      "movements": {
       "total": 0,
       "toStatus": {}
      },
      "byStatusNow": {}
     },
     "invoices": {
      "basis": "Issued counts non-Draft invoices whose issue_date falls in the month; collected counts invoices whose paid_date falls in the month (full invoice totals); outstanding is the CURRENT Sent balance, with the derived-overdue part broken out.",
      "issued": {
       "count": 0,
       "totalCents": 0
      },
      "collected": {
       "count": 0,
       "totalCents": 0
      },
      "outstanding": {
       "count": 0,
       "totalCents": 0
      },
      "overdue": {
       "count": 0,
       "totalCents": 0
      }
     }
    },
    "ok": true
   }
  },
  "POST /api/app/admin/members": {
   "method": "POST",
   "path": "/api/app/admin/members",
   "status": 201,
   "body": {
    "ok": true,
    "accountType": "command",
    "userId": 4,
    "displayName": "ProvCap8B",
    "role": "STAFF",
    "deliveryEmail": "prov@capture8b.example.invalid",
    "pendingSetup": true,
    "linkExpiresAt": 1793770214886,
    "message": "Command account created \u2014 invite sent to prov@capture8b.example.invalid."
   }
  },
  "POST /api/app/admin/members/4/passphrase": {
   "method": "POST",
   "path": "/api/app/admin/members/4/passphrase",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Temporary passphrase set for ProvCap8B. They must choose a new passphrase when they sign in."
   }
  },
  "POST /api/app/admin/settings": {
   "method": "POST",
   "path": "/api/app/admin/settings",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Settings updated."
   }
  },
  "POST /api/app/admin/security": {
   "method": "POST",
   "path": "/api/app/admin/security",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Security settings updated.",
    "lockdown": false,
    "sessionTtlHours": 24,
    "requireTotpAdminWrite": false,
    "activityRetentionDays": 0
   }
  },
  "POST /api/app/admin/recovery-codes": {
   "method": "POST",
   "path": "/api/app/admin/recovery-codes",
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Current passphrase does not match."
   }
  },
  "POST /api/app/admin/recovery-codes [admin_recovery_shapeonly]": {
   "method": "POST",
   "path": "/api/app/admin/recovery-codes",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Recovery codes regenerated.",
    "recoveryCodes": {
     "_shape_only": true,
     "count": 10,
     "lengths": [
      32,
      32,
      32,
      32,
      32,
      32,
      32,
      32,
      32,
      32
     ]
    }
   }
  },
  "GET /api/app/admin/oversight/retention": {
   "method": "GET",
   "path": "/api/app/admin/oversight/retention",
   "status": 200,
   "body": {
    "settings": [
     {
      "entity": "offboarded_clients",
      "label": "Offboarded clients",
      "blurb": "Clients whose offboarding completed (archived_at set). The #40 offboard key is this entity.",
      "retentionDays": null,
      "set": false,
      "updatedAt": null
     },
     {
      "entity": "closed_tickets",
      "label": "Closed tickets",
      "blurb": "Tickets in a closed state, by last update.",
      "retentionDays": null,
      "set": false,
      "updatedAt": null
     },
     {
      "entity": "activity",
      "label": "Activity log",
      "blurb": "Audit/activity rows, by event time.",
      "retentionDays": null,
      "set": false,
      "updatedAt": null
     },
     {
      "entity": "client_digests",
      "label": "Client digests",
      "blurb": "Weekly client digest rows (#22), by generation time.",
      "retentionDays": null,
      "set": false,
      "updatedAt": null
     }
    ],
    "reports": [],
    "note": "Report-only: this manager never deletes data. A report shows what WOULD expire under a setting; actual deletion, if ever wanted, is a separate explicit owner action outside this feature.",
    "ok": true
   }
  },
  "POST /api/app/admin/oversight/retention": {
   "method": "POST",
   "path": "/api/app/admin/oversight/retention",
   "status": 200,
   "body": {
    "ok": true,
    "settings": [
     {
      "entity": "offboarded_clients",
      "label": "Offboarded clients",
      "blurb": "Clients whose offboarding completed (archived_at set). The #40 offboard key is this entity.",
      "retentionDays": null,
      "set": false,
      "updatedAt": null
     },
     {
      "entity": "closed_tickets",
      "label": "Closed tickets",
      "blurb": "Tickets in a closed state, by last update.",
      "retentionDays": null,
      "set": false,
      "updatedAt": null
     },
     {
      "entity": "activity",
      "label": "Activity log",
      "blurb": "Audit/activity rows, by event time.",
      "retentionDays": 90,
      "set": true,
      "updatedAt": 1793763016371
     },
     {
      "entity": "client_digests",
      "label": "Client digests",
      "blurb": "Weekly client digest rows (#22), by generation time.",
      "retentionDays": null,
      "set": false,
      "updatedAt": null
     }
    ]
   }
  },
  "POST /api/app/admin/oversight/retention/report": {
   "method": "POST",
   "path": "/api/app/admin/oversight/retention/report",
   "status": 200,
   "body": {
    "ok": true,
    "reportId": 1,
    "report": {
     "generatedAt": 1793763016480,
     "reportOnly": true,
     "sections": [
      {
       "entity": "offboarded_clients",
       "retentionDays": null,
       "wouldExpire": null,
       "cutoffAt": null,
       "samples": []
      },
      {
       "entity": "closed_tickets",
       "retentionDays": null,
       "wouldExpire": null,
       "cutoffAt": null,
       "samples": []
      },
      {
       "entity": "activity",
       "retentionDays": 90,
       "wouldExpire": 0,
       "cutoffAt": 1785987016480,
       "samples": []
      },
      {
       "entity": "client_digests",
       "retentionDays": null,
       "wouldExpire": null,
       "cutoffAt": null,
       "samples": []
      }
     ]
    }
   }
  },
  "GET /api/app/admin/oversight/audit-log": {
   "method": "GET",
   "path": "/api/app/admin/oversight/audit-log",
   "status": 200,
   "body": {
    "rows": [
     {
      "id": 23,
      "actorId": 1,
      "actorName": "OwnerCap8B",
      "actorRole": "OWNER",
      "action": "CF2_RETENTION_REPORT",
      "label": "Cf2 retention report",
      "eventClass": "changes",
      "entityType": "settings",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Retention report run -- report-only, nothing was deleted.",
      "createdAt": 1793763016480,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": "",
      "anomaly": ""
     },
     {
      "id": 22,
      "actorId": 1,
      "actorName": "OwnerCap8B",
      "actorRole": "OWNER",
      "action": "CF2_RETENTION_SET",
      "label": "Cf2 retention set",
      "eventClass": "changes",
      "entityType": "settings",
      "entityId": null,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Retention for activity set to 90 by OwnerCap8B. Report-only -- nothing was deleted.",
      "createdAt": 1793763016371,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": "",
      "anomaly": ""
     },
     {
      "id": 21,
      "actorId": 1,
      "actorName": "OwnerCap8B",
      "actorRole": "OWNER",
      "action": "SECURITY",
      "label": "Security action",
      "eventClass": "auth",
      "entityType": "recovery_codes",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Recovery codes regenerated; prior unused codes invalidated.",
      "createdAt": 1793763016191,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": "",
      "anomaly": ""
     },
     {
      "id": 20,
      "actorId": 1,
      "actorName": "OwnerCap8B",
      "actorRole": "OWNER",
      "action": "SETTINGS_UPDATED",
      "label": "Settings updated",
      "eventClass": "admin",
      "entityType": "settings",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Security settings updated: session lifetime 24h.",
      "createdAt": 1793763015820,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": "",
      "anomaly": ""
     },
     {
      "id": 19,
      "actorId": 1,
      "actorName": "OwnerCap8B",
      "actorRole": "OWNER",
      "action": "SETTINGS_UPDATED",
      "label": "Settings updated",
      "eventClass": "admin",
      "entityType": "settings",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Settings updated: workspace name.",
      "createdAt": 1793763015686,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": "",
      "anomaly": ""
     },
     {
      "id": 18,
      "actorId": 1,
      "actorName": "OwnerCap8B",
      "actorRole": "OWNER",
      "action": "USER_PASSPHRASE_SET",
      "label": "Temporary passphrase set by the owner",
      "eventClass": "admin",
      "entityType": "user",
      "entityId": 4,
      "entityExists": true,
      "entityLabel": "ProvCap8B",
      "detail": "Temporary passphrase set for ProvCap8B by the owner; their sessions were revoked and a passphrase change is required at next sign-in.",
      "createdAt": 1793763015571,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": "",
      "anomaly": ""
     },
     {
      "id": 17,
      "actorId": 1,
      "actorName": "OwnerCap8B",
      "actorRole": "OWNER",
      "action": "ACCOUNT_PROVISIONED",
      "label": "Account provisioned",
      "eventClass": "admin",
      "entityType": "user",
      "entityId": 4,
      "entityExists": true,
      "entityLabel": "ProvCap8B",
      "detail": "Command-only account provisioned for ProvCap8B (STAFF). No mailbox, no client link. Invite sent to prov@capture8b.example.invalid.",
      "createdAt": 1793763015360,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": "",
      "anomaly": ""
     },
     {
      "id": 16,
      "actorId": 1,
      "actorName": "OwnerCap8B",
      "actorRole": "OWNER",
      "action": "ROLE_DELETED",
      "label": "Role deleted",
      "eventClass": "admin",
      "entityType": "role",
      "entityId": null,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Role 'CaptureRole8B' deleted.",
      "createdAt": 1793763014530,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": "",
      "anomaly": ""
     },
     {
      "id": 15,
      "actorId": 1,
      "actorName": "OwnerCap8B",
      "actorRole": "OWNER",
      "action": "ROLE_TABS_UPDATED",
      "label": "Role tabs updated",
      "eventClass": "changes",
      "entityType": "role",
      "entityId": null,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Role 'CaptureRole8B' tab grants set: none (module matrix decides).",
      "createdAt": 1793763014363,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": "",
      "anomaly": ""
     },
     {
      "id": 14,
      "actorId": 1,
      "actorName": "OwnerCap8B",
      "actorRole": "OWNER",
      "action": "ROLE_UPDATED",
      "label": "Role updated",
      "eventClass": "admin",
      "entityType": "role",
      "entityId": null,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Role 'CaptureRole8B' updated: color.",
      "createdAt": 1793763014197,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": "",
      "anomaly": ""
     },
     {
      "id": 13,
      "actorId": 1,
      "actorName": "OwnerCap8B",
      "actorRole": "OWNER",
      "action": "ROLE_CREATED",
      "label": "Role created",
      "eventClass": "admin",
      "entityType": "role",
      "entityId": null,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Role 'CaptureRole8B' created (tickets read).",
      "createdAt": 1793763013997,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": "",
      "anomaly": ""
     },
     {
      "id": 12,
      "actorId": 1,
      "actorName": "OwnerCap8B",
      "actorRole": "OWNER",
      "action": "TICKET_SAVED",
      "label": "Ticket saved",
      "eventClass": "changes",
      "entityType": "ticket",
      "entityId": 1,
      "entityExists": true,
      "entityLabel": "Capture ticket",
      "detail": "Ticket saved (via app).",
      "createdAt": 1793763013265,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": "",
      "anomaly": ""
     },
     {
      "id": 11,
      "actorId": 1,
      "actorName": "OwnerCap8B",
      "actorRole": "OWNER",
      "action": "CLIENT_SAVED",
      "label": "Client saved",
      "eventClass": "changes",
      "entityType": "client",
      "entityId": 2,
      "entityExists": true,
      "entityLabel": "Capture No Email LLC",
      "detail": "Client saved.",
      "createdAt": 1793763013048,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": "",
      "anomaly": ""
     },
     {
      "id": 10,
      "actorId": 1,
      "actorName": "OwnerCap8B",
      "actorRole": "OWNER",
      "action": "CLIENT_SAVED",
      "label": "Client saved",
      "eventClass": "changes",
      "entityType": "client",
      "entityId": 1,
      "entityExists": true,
      "entityLabel": "Capture Fixture LLC",
      "detail": "Client saved.",
      "createdAt": 1793763012880,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": "",
      "anomaly": ""
     },
     {
      "id": 9,
      "actorId": 3,
      "actorName": "NoPermCap8B",
      "actorRole": "NoPermCap8B",
      "action": "APP_LOGIN",
      "label": "App sign-in \u2014 device enrolled",
      "eventClass": "auth",
      "entityType": "user",
      "entityId": 3,
      "entityExists": true,
      "entityLabel": "NoPermCap8B",
      "detail": "App sign-in \u2014 device enrolled: cap8b.",
      "createdAt": 1793763012710,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": "",
      "anomaly": ""
     },
     {
      "id": 8,
      "actorId": 2,
      "actorName": "StaffCap8B",
      "actorRole": "STAFF",
      "action": "APP_LOGIN",
      "label": "App sign-in \u2014 device enrolled",
      "eventClass": "auth",
      "entityType": "user",
      "entityId": 2,
      "entityExists": true,
      "entityLabel": "StaffCap8B",
      "detail": "App sign-in \u2014 device enrolled: cap8b.",
      "createdAt": 1793763012546,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": "",
      "anomaly": ""
     },
     {
      "id": 7,
      "actorId": 1,
      "actorName": "OwnerCap8B",
      "actorRole": "OWNER",
      "action": "APP_LOGIN",
      "label": "App sign-in \u2014 device enrolled",
      "eventClass": "auth",
      "entityType": "user",
      "entityId": 1,
      "entityExists": true,
      "entityLabel": "OwnerCap8B",
      "detail": "App sign-in \u2014 device enrolled: cap8b.",
      "createdAt": 1793763012396,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": "",
      "anomaly": ""
     },
     {
      "id": 6,
      "actorId": 3,
      "actorName": "NoPermCap8B",
      "actorRole": "NoPermCap8B",
      "action": "INVITE_REDEEMED",
      "label": "Invite redeemed \u2014 account created",
      "eventClass": "auth",
      "entityType": "invite",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "NoPermCap8B account created via invite.",
      "createdAt": 1793763012204,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": "",
      "anomaly": ""
     },
     {
      "id": 5,
      "actorId": 1,
      "actorName": "OwnerCap8B",
      "actorRole": "OWNER",
      "action": "INVITE_CREATED",
      "label": "Invite created",
      "eventClass": "admin",
      "entityType": "invite",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Invite created for role NoPermCap8B.",
      "createdAt": 1793763012026,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": "",
      "anomaly": ""
     },
     {
      "id": 4,
      "actorId": 1,
      "actorName": "OwnerCap8B",
      "actorRole": "OWNER",
      "action": "ROLE_CREATED",
      "label": "Role created",
      "eventClass": "admin",
      "entityType": "role",
      "entityId": null,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Role 'NoPermCap8B' created (cases read).",
      "createdAt": 1793763011944,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": "",
      "anomaly": ""
     },
     {
      "id": 3,
      "actorId": 1,
      "actorName": "OwnerCap8B",
      "actorRole": "OWNER",
      "action": "EMPLOYEE_CREATED",
      "label": "Employee created",
      "eventClass": "admin",
      "entityType": "user",
      "entityId": 2,
      "entityExists": true,
      "entityLabel": "StaffCap8B",
      "detail": "Staff user StaffCap8B created.",
      "createdAt": 1793763011800,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": "",
      "anomaly": ""
     },
     {
      "id": 2,
      "actorId": 1,
      "actorName": "OwnerCap8B",
      "actorRole": "OWNER",
      "action": "LOGIN",
      "label": "Signed in",
      "eventClass": "auth",
      "entityType": "session",
      "entityId": 1,
      "entityExists": true,
      "entityLabel": "OwnerCap8B",
      "detail": "Session opened; expires in 8 hours",
      "createdAt": 1793763011636,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": "",
      "anomaly": ""
     },
     {
      "id": 1,
      "actorId": 1,
      "actorName": "OwnerCap8B",
      "actorRole": "OWNER",
      "action": "OWNER_INITIALIZED",
      "label": "Owner account initialized",
      "eventClass": "auth",
      "entityType": "settings",
      "entityId": 1,
      "entityExists": false,
      "entityLabel": "",
      "detail": "Owner account initialized; recovery codes issued.",
      "createdAt": 1793763011466,
      "ip": "127.0.0.1",
      "ua": "Python-urllib/3.12",
      "uaSummary": "Python-urllib/3.12",
      "view": "",
      "viewLabel": "",
      "anomaly": ""
     }
    ],
    "total": 23,
    "offset": 0,
    "limit": 100,
    "ok": true
   }
  },
  "GET /api/app/admin/oversight/audit-export": {
   "method": "GET",
   "path": "/api/app/admin/oversight/audit-export",
   "status": 200,
   "body": {
    "ok": true,
    "csv": {
     "_truncated": true,
     "chars": 2705,
     "sha256": "d60f52c8645aeb561dd4e31935dda52b590e3c1b20c26bf0772f4149344a9f31"
    },
    "count": 23,
    "firstId": 23,
    "lastId": 1
   }
  },
  "GET /api/app/admin/oversight/login-security": {
   "method": "GET",
   "path": "/api/app/admin/oversight/login-security",
   "status": 200,
   "body": {
    "failed24h": 0,
    "failed7d": 0,
    "signins24h": 1,
    "failedByIp": [],
    "lockouts": {
     "stateSource": "in-memory pre-auth throttle (per-process; not persisted -- clears on restart)",
     "limit": 10,
     "windowMinutes": 10,
     "currentlyThrottled": []
    },
    "sessions": [
     {
      "id": 1,
      "userId": 1,
      "displayName": "OwnerCap8B",
      "username": "OwnerCap8B",
      "role": "OWNER",
      "createdAt": 1793763011636,
      "lastSeenAt": 1793763011636,
      "expiresAt": 1793791811635,
      "isCurrent": false
     }
    ],
    "activeSessions": 1,
    "ok": true
   }
  },
  "GET /api/app/admin/law-ai": {
   "method": "GET",
   "path": "/api/app/admin/law-ai",
   "status": 200,
   "body": {
    "configured": true,
    "provider": "openrouter",
    "modelsResearch": [
     "fixture/model:free"
    ],
    "modelsDocgen": [
     "fixture/model:free"
    ],
    "dailyCap": 20,
    "maxInflight": 1,
    "notice": "",
    "todayCount": 0,
    "totals": {
     "requests": 0,
     "tokensIn": 0,
     "tokensOut": 0
    },
    "byDay": [],
    "nonFreeConfigured": false,
    "freeNotice": "Free tier \u2014 $0 spent, $0 configured",
    "ok": true
   }
  },
  "POST /api/app/admin/law-ai": {
   "method": "POST",
   "path": "/api/app/admin/law-ai",
   "status": 200,
   "body": {
    "configured": true,
    "provider": "openrouter",
    "modelsResearch": [
     "fixture/model:free"
    ],
    "modelsDocgen": [
     "fixture/model:free"
    ],
    "dailyCap": 20,
    "maxInflight": 1,
    "notice": "",
    "todayCount": 0,
    "totals": {
     "requests": 0,
     "tokensIn": 0,
     "tokensOut": 0
    },
    "byDay": [],
    "nonFreeConfigured": false,
    "freeNotice": "Free tier \u2014 $0 spent, $0 configured",
    "ok": true,
    "message": "Law AI settings saved."
   }
  },
  "GET /api/app/admin/law-ai/key-status": {
   "method": "GET",
   "path": "/api/app/admin/law-ai/key-status",
   "status": 200,
   "body": {
    "configured": true,
    "provider": "openrouter",
    "modelsResearch": [
     "fixture/model:free"
    ],
    "modelsDocgen": [
     "fixture/model:free"
    ],
    "dailyCap": 20,
    "maxInflight": 1,
    "notice": "",
    "ok": true
   }
  },
  "POST /api/app/admin/law-ai/key": {
   "method": "POST",
   "path": "/api/app/admin/law-ai/key",
   "status": 200,
   "body": {
    "ok": true,
    "message": "API key installed. The legal assistant is configured.",
    "configured": true,
    "provider": "openrouter",
    "modelsResearch": [
     "fixture/model:free"
    ],
    "modelsDocgen": [
     "fixture/model:free"
    ],
    "dailyCap": 20,
    "maxInflight": 1,
    "notice": ""
   }
  },
  "GET /api/app/admin/law-ai/key-status [admin_lawai_keystatus_after]": {
   "method": "GET",
   "path": "/api/app/admin/law-ai/key-status",
   "status": 200,
   "body": {
    "configured": true,
    "provider": "openrouter",
    "modelsResearch": [
     "fixture/model:free"
    ],
    "modelsDocgen": [
     "fixture/model:free"
    ],
    "dailyCap": 20,
    "maxInflight": 1,
    "notice": "",
    "ok": true
   }
  },
  "GET /api/app/admin/backups/status": {
   "method": "GET",
   "path": "/api/app/admin/backups/status",
   "status": 200,
   "body": {
    "state": "requested",
    "requestId": "5aeab123925c4e7cba9e0196f02749dc",
    "ok": true
   }
  },
  "POST /api/app/admin/backups/request": {
   "method": "POST",
   "path": "/api/app/admin/backups/request",
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "A backup bundle is already requested \u2014 download or discard it first."
   }
  },
  "GET /api/app/admin/dmarc?days=30": {
   "method": "GET",
   "path": "/api/app/admin/dmarc?days=30",
   "status": 200,
   "body": {
    "windowDays": 30,
    "empty": true,
    "inventory": {
     "totalReports": 0,
     "reportsInWindow": 0,
     "newestReportEnd": null,
     "malformed": 0,
     "storePresent": false,
     "generatedAt": null
    },
    "summary": {
     "volume": 0,
     "spfPassVolume": 0,
     "dkimPassVolume": 0,
     "spfPassPct": null,
     "dkimPassPct": null,
     "failingSources": 0
    },
    "byDomain": [],
    "bySource": [],
    "reports": [],
    "ok": true
   }
  },
  "GET /api/app/admin/dmarc": {
   "method": "GET",
   "path": "/api/app/admin/dmarc",
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Not authorized."
   }
  },
  "GET /api/app/ops-views/risk-heatmap": {
   "method": "GET",
   "path": "/api/app/ops-views/risk-heatmap",
   "status": 200,
   "body": {
    "ok": true,
    "today": "2026-11-03",
    "rows": [
     {
      "clientId": 1,
      "clientName": "Capture Fixture LLC",
      "familyLinked": false,
      "scoreState": "scored",
      "score": 42,
      "openTickets": 1,
      "overdueCents": 0,
      "overdueCount": 0,
      "expiringCount": 0,
      "expiring": [],
      "breachHits": 0,
      "riskLevel": "medium"
     },
     {
      "clientId": 2,
      "clientName": "Capture No Email LLC",
      "familyLinked": false,
      "scoreState": "insufficient",
      "score": null,
      "openTickets": 0,
      "overdueCents": 0,
      "overdueCount": 0,
      "expiringCount": 0,
      "expiring": [],
      "breachHits": 0,
      "riskLevel": "low"
     }
    ]
   }
  },
  "GET /api/app/ops-views/timeline": {
   "method": "GET",
   "path": "/api/app/ops-views/timeline",
   "status": 200,
   "body": {
    "ok": true,
    "month": "2026-11",
    "events": []
   }
  },
  "GET /api/app/ops-views/contract-radar": {
   "method": "GET",
   "path": "/api/app/ops-views/contract-radar",
   "status": 200,
   "body": {
    "ok": true,
    "contracts": [],
    "today": "2026-11-03"
   }
  },
  "GET /api/app/ops-views/capacity": {
   "method": "GET",
   "path": "/api/app/ops-views/capacity",
   "status": 200,
   "body": {
    "ok": true,
    "thresholdHours": 40.0,
    "weeks": [
     {
      "weekStart": "2026-11-02",
      "staff": [
       {
        "userId": 1,
        "name": "OwnerCap8B",
        "role": "OWNER",
        "timeHours": 0.0,
        "jobHours": 0.0,
        "committedHours": 0.0,
        "overThreshold": false,
        "jobIds": []
       },
       {
        "userId": 4,
        "name": "ProvCap8B",
        "role": "STAFF",
        "timeHours": 0.0,
        "jobHours": 0.0,
        "committedHours": 0.0,
        "overThreshold": false,
        "jobIds": []
       },
       {
        "userId": 2,
        "name": "StaffCap8B",
        "role": "STAFF",
        "timeHours": 0.0,
        "jobHours": 0.0,
        "committedHours": 0.0,
        "overThreshold": false,
        "jobIds": []
       }
      ],
      "anyOver": false
     },
     {
      "weekStart": "2026-11-09",
      "staff": [
       {
        "userId": 1,
        "name": "OwnerCap8B",
        "role": "OWNER",
        "timeHours": 0.0,
        "jobHours": 0.0,
        "committedHours": 0.0,
        "overThreshold": false,
        "jobIds": []
       },
       {
        "userId": 4,
        "name": "ProvCap8B",
        "role": "STAFF",
        "timeHours": 0.0,
        "jobHours": 0.0,
        "committedHours": 0.0,
        "overThreshold": false,
        "jobIds": []
       },
       {
        "userId": 2,
        "name": "StaffCap8B",
        "role": "STAFF",
        "timeHours": 0.0,
        "jobHours": 0.0,
        "committedHours": 0.0,
        "overThreshold": false,
        "jobIds": []
       }
      ],
      "anyOver": false
     },
     {
      "weekStart": "2026-11-16",
      "staff": [
       {
        "userId": 1,
        "name": "OwnerCap8B",
        "role": "OWNER",
        "timeHours": 0.0,
        "jobHours": 0.0,
        "committedHours": 0.0,
        "overThreshold": false,
        "jobIds": []
       },
       {
        "userId": 4,
        "name": "ProvCap8B",
        "role": "STAFF",
        "timeHours": 0.0,
        "jobHours": 0.0,
        "committedHours": 0.0,
        "overThreshold": false,
        "jobIds": []
       },
       {
        "userId": 2,
        "name": "StaffCap8B",
        "role": "STAFF",
        "timeHours": 0.0,
        "jobHours": 0.0,
        "committedHours": 0.0,
        "overThreshold": false,
        "jobIds": []
       }
      ],
      "anyOver": false
     },
     {
      "weekStart": "2026-11-23",
      "staff": [
       {
        "userId": 1,
        "name": "OwnerCap8B",
        "role": "OWNER",
        "timeHours": 0.0,
        "jobHours": 0.0,
        "committedHours": 0.0,
        "overThreshold": false,
        "jobIds": []
       },
       {
        "userId": 4,
        "name": "ProvCap8B",
        "role": "STAFF",
        "timeHours": 0.0,
        "jobHours": 0.0,
        "committedHours": 0.0,
        "overThreshold": false,
        "jobIds": []
       },
       {
        "userId": 2,
        "name": "StaffCap8B",
        "role": "STAFF",
        "timeHours": 0.0,
        "jobHours": 0.0,
        "committedHours": 0.0,
        "overThreshold": false,
        "jobIds": []
       }
      ],
      "anyOver": false
     }
    ],
    "ownerDoubleBooked": [],
    "availability": []
   }
  },
  "POST /api/app/ops-views/capacity-threshold": {
   "method": "POST",
   "path": "/api/app/ops-views/capacity-threshold",
   "status": 200,
   "body": {
    "ok": true,
    "thresholdHours": 30.0
   }
  },
  "GET /api/app/pipeline-field/job-mode": {
   "method": "GET",
   "path": "/api/app/pipeline-field/job-mode",
   "status": 200,
   "body": {
    "today": "2026-11-03",
    "jobs": [],
    "writePaths": {
     "timer": [
      "starttimer",
      "stoptimer"
     ],
     "checklist": [
      "cfcompletechecklistitem",
      "cfcompletechecklistrun"
     ],
     "photo": "/photos/upload",
     "status": "setticketstatus"
    },
    "ok": true
   }
  },
  "GET /api/app/pipeline-field/lead-scores": {
   "method": "GET",
   "path": "/api/app/pipeline-field/lead-scores",
   "status": 200,
   "body": {
    "leads": [],
    "rules": [
     {
      "rule": "source",
      "detail": "referral +25 \u00b7 website_book +20 \u00b7 website_contact +15 \u00b7 google +15 \u00b7 yelp +10 \u00b7 manual/other +5"
     },
     {
      "rule": "serviceFit",
      "detail": "notes/business/name mentions a known service (camera, network, security, computer, install, recovery, wifi, firewall, backup, IT support, penetration, audit, cabling, access control) +20"
     },
     {
      "rule": "contactCompleteness",
      "detail": "email +15 \u00b7 phone +15 \u00b7 business named +5"
     },
     {
      "rule": "responseSignals",
      "detail": "state contacted +10 \u00b7 qualified +20"
     }
    ],
    "maxScore": 100,
    "ok": true
   }
  },
  "GET /api/app/pipeline-field/lead-roi": {
   "method": "GET",
   "path": "/api/app/pipeline-field/lead-roi",
   "status": 200,
   "body": {
    "sources": [
     {
      "source": "website_contact",
      "leads": 0,
      "converted": 0,
      "invoicedCents": 0,
      "familyExcluded": 0
     },
     {
      "source": "website_book",
      "leads": 0,
      "converted": 0,
      "invoicedCents": 0,
      "familyExcluded": 0
     },
     {
      "source": "referral",
      "leads": 0,
      "converted": 0,
      "invoicedCents": 0,
      "familyExcluded": 0
     },
     {
      "source": "google",
      "leads": 0,
      "converted": 0,
      "invoicedCents": 0,
      "familyExcluded": 0
     },
     {
      "source": "yelp",
      "leads": 0,
      "converted": 0,
      "invoicedCents": 0,
      "familyExcluded": 0
     },
     {
      "source": "other",
      "leads": 0,
      "converted": 0,
      "invoicedCents": 0,
      "familyExcluded": 0
     }
    ],
    "totals": {
     "leads": 0,
     "converted": 0,
     "invoicedCents": 0,
     "familyExcluded": 0
    },
    "note": "Family-converted clients count as conversions but their money is excluded from every rollup.",
    "ok": true
   }
  },
  "POST /api/app/pipeline-field/asset-import/preview": {
   "method": "POST",
   "path": "/api/app/pipeline-field/asset-import/preview",
   "status": 200,
   "body": {
    "ok": true,
    "batch": {
     "id": 1,
     "clientId": 1,
     "fileName": "cap-assets.csv",
     "state": "staged",
     "totalRows": 1,
     "newRows": 1,
     "duplicateRows": 0,
     "errorRows": 0,
     "importedCount": 0,
     "createdAt": 1793763018340,
     "confirmedAt": null,
     "rows": [
      {
       "id": 1,
       "rowNumber": 2,
       "verdict": "new",
       "detail": "",
       "data": {
        "name": "Capture Laptop",
        "type": "computer",
        "identifier": "CL-001",
        "mac": "",
        "status": "In Service",
        "notes": "",
        "warrantyUntil": "",
        "reviewDueDate": ""
       },
       "assetId": null
      }
     ]
    },
    "ignoredColumns": [],
    "matchKeys": [
     "serial/identifier (exact, case-insensitive)",
     "MAC (normalized)",
     "name+type (case-insensitive)"
    ],
    "message": "Preview staged \u2014 review the per-row verdicts, then confirm to import."
   }
  },
  "POST /api/app/pipeline-field/asset-import/confirm": {
   "method": "POST",
   "path": "/api/app/pipeline-field/asset-import/confirm",
   "status": 200,
   "body": {
    "ok": true,
    "id": 1,
    "imported": 1,
    "assetIds": [
     1
    ],
    "skippedDuplicates": 0,
    "message": "Imported 1 asset(s); 0 duplicate(s) skipped."
   }
  },
  "POST /api/app/pipeline-field/counts": {
   "method": "POST",
   "path": "/api/app/pipeline-field/counts",
   "status": 201,
   "body": {
    "ok": true,
    "sessionId": 1,
    "message": "Count session started. Scanning records counts only \u2014 stock does not move until a confirmed adjustment is applied."
   }
  },
  "POST /api/app/pipeline-field/counts/1/scan": {
   "method": "POST",
   "path": "/api/app/pipeline-field/counts/1/scan",
   "status": 200,
   "body": {
    "ok": true,
    "found": true,
    "kind": "part",
    "part": {
     "id": 1,
     "name": "Capture Widget 8B",
     "sku": "CW8B",
     "qtyOnHand": 10,
     "unitCostCents": 0,
     "lowStockThreshold": 0,
     "lowStock": false,
     "createdBy": 1,
     "createdAt": 1793763018700,
     "updatedAt": 1793763018700
    },
    "session": {
     "id": 1,
     "status": "open",
     "lines": [
      {
       "partId": 1,
       "name": "Capture Widget 8B",
       "sku": "CW8B",
       "systemQty": 10,
       "countedQty": 9,
       "currentQty": 10
      }
     ],
     "variance": [],
     "createdBy": 2,
     "createdAt": 1793763018943,
     "closedAt": null,
     "appliedAt": null
    }
   }
  },
  "POST /api/app/pipeline-field/counts/1/close": {
   "method": "POST",
   "path": "/api/app/pipeline-field/counts/1/close",
   "status": 200,
   "body": {
    "ok": true,
    "session": {
     "id": 1,
     "status": "draft",
     "lines": [
      {
       "partId": 1,
       "name": "Capture Widget 8B",
       "sku": "CW8B",
       "systemQty": 10,
       "countedQty": 9,
       "currentQty": 10
      }
     ],
     "variance": [
      {
       "partId": 1,
       "name": "Capture Widget 8B",
       "systemQty": 10,
       "countedQty": 9,
       "variance": -1,
       "reason": "Capture recount"
      }
     ],
     "createdBy": 2,
     "createdAt": 1793763018943,
     "closedAt": 1793763019254,
     "appliedAt": null
    },
    "message": "Adjustment drafted. Stock has NOT changed \u2014 a staff confirm applies it."
   }
  },
  "POST /api/app/pipeline-field/counts/1/apply": {
   "method": "POST",
   "path": "/api/app/pipeline-field/counts/1/apply",
   "status": 200,
   "body": {
    "ok": true,
    "session": {
     "id": 1,
     "status": "applied",
     "lines": [
      {
       "partId": 1,
       "name": "Capture Widget 8B",
       "sku": "CW8B",
       "systemQty": 10,
       "countedQty": 9,
       "currentQty": 9
      }
     ],
     "variance": [
      {
       "partId": 1,
       "name": "Capture Widget 8B",
       "systemQty": 10,
       "countedQty": 9,
       "variance": -1,
       "reason": "Capture recount"
      }
     ],
     "createdBy": 2,
     "createdAt": 1793763018943,
     "closedAt": 1793763019254,
     "appliedAt": 1793763019417
    },
    "message": "Adjustment applied through the movement ledger."
   }
  },
  "GET /api/app/pipeline-field/counts": {
   "method": "GET",
   "path": "/api/app/pipeline-field/counts",
   "status": 200,
   "body": {
    "sessions": [
     {
      "id": 1,
      "status": "applied",
      "lines": [
       {
        "partId": 1,
        "name": "Capture Widget 8B",
        "sku": "CW8B",
        "systemQty": 10,
        "countedQty": 9,
        "currentQty": 9
       }
      ],
      "variance": [
       {
        "partId": 1,
        "name": "Capture Widget 8B",
        "systemQty": 10,
        "countedQty": 9,
        "variance": -1,
        "reason": "Capture recount"
       }
      ],
      "createdBy": 2,
      "createdAt": 1793763018943,
      "closedAt": 1793763019254,
      "appliedAt": 1793763019417
     }
    ],
    "ok": true
   }
  },
  "POST /api/app/pipeline-field/scan": {
   "method": "POST",
   "path": "/api/app/pipeline-field/scan",
   "status": 200,
   "body": {
    "ok": true,
    "found": true,
    "kind": "part",
    "part": {
     "id": 1,
     "name": "Capture Widget 8B",
     "sku": "CW8B",
     "qtyOnHand": 9,
     "unitCostCents": 0,
     "lowStockThreshold": 0,
     "lowStock": false,
     "createdBy": 1,
     "createdAt": 1793763018700,
     "updatedAt": 1793763019417
    }
   }
  },
  "POST /api/app/pipeline-field/sync-push": {
   "method": "POST",
   "path": "/api/app/pipeline-field/sync-push",
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "The sync queue is empty."
   }
  },
  "POST /api/app/reports": {
   "method": "POST",
   "path": "/api/app/reports",
   "status": 200,
   "body": {
    "ok": true,
    "id": 1,
    "message": "Report draft generated; nothing was sent.",
    "report": {
     "id": 1,
     "clientId": 1,
     "clientName": "Capture Fixture LLC",
     "period": "2026-10",
     "figures": {
      "period": "2026-10",
      "posture": {
       "state": "scored",
       "score": 57,
       "baseline": 40,
       "factors": [
        {
         "label": "1 asset on file",
         "points": 15
        },
        {
         "label": "Activity in the last 90 days",
         "points": 5
        },
        {
         "label": "1 open normal-priority ticket",
         "points": -3
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
        "assets": 1,
        "openTickets": 1,
        "openCases": 0,
        "closedItems": 0,
        "totalItems": 1,
        "overdueInvoices": 0,
        "forms": 0
       }
      },
      "ticketsOpened": 0,
      "ticketsClosed": 0,
      "trackedSeconds": 0,
      "trackedHours": 0.0,
      "timeEntries": 0,
      "renewals": [],
      "roadmap": {
       "planned": 0,
       "in_progress": 0,
       "done": 0,
       "accepted_risk": 0,
       "total": 0
      }
     },
     "body": "Monthly report for Capture Fixture LLC \u2014 2026-10\n\nSecurity posture: 57 out of 100.\n\nTickets opened: 0\nTickets closed: 0\n\nTracked time: 0.0 hours (0 seconds across 0 entries)\n\nRenewals in the next 60 days: 0\n\nSecurity roadmap: 0 item(s) \u2014 0 planned, 0 in progress, 0 done, 0 accepted risk.\n\nPrepared by Sentinel Safeguard LLC from the work recorded in Command. Figures are counts of real records; a quiet month reports zeros.\n",
     "state": "draft",
     "generatedBy": 1,
     "generatedAt": 1793763019733,
     "sentAt": null,
     "sentBy": null,
     "createdAt": 1793763019733,
     "updatedAt": 1793763019733
    }
   }
  },
  "GET /api/app/reports": {
   "method": "GET",
   "path": "/api/app/reports",
   "status": 200,
   "body": {
    "ok": true,
    "reports": [
     {
      "id": 1,
      "clientId": 1,
      "clientName": "Capture Fixture LLC",
      "period": "2026-10",
      "figures": {
       "period": "2026-10",
       "posture": {
        "state": "scored",
        "score": 57,
        "baseline": 40,
        "factors": [
         {
          "label": "1 asset on file",
          "points": 15
         },
         {
          "label": "Activity in the last 90 days",
          "points": 5
         },
         {
          "label": "1 open normal-priority ticket",
          "points": -3
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
         "assets": 1,
         "openTickets": 1,
         "openCases": 0,
         "closedItems": 0,
         "totalItems": 1,
         "overdueInvoices": 0,
         "forms": 0
        }
       },
       "ticketsOpened": 0,
       "ticketsClosed": 0,
       "trackedSeconds": 0,
       "trackedHours": 0.0,
       "timeEntries": 0,
       "renewals": [],
       "roadmap": {
        "planned": 0,
        "in_progress": 0,
        "done": 0,
        "accepted_risk": 0,
        "total": 0
       }
      },
      "body": "Monthly report for Capture Fixture LLC \u2014 2026-10\n\nSecurity posture: 57 out of 100.\n\nTickets opened: 0\nTickets closed: 0\n\nTracked time: 0.0 hours (0 seconds across 0 entries)\n\nRenewals in the next 60 days: 0\n\nSecurity roadmap: 0 item(s) \u2014 0 planned, 0 in progress, 0 done, 0 accepted risk.\n\nPrepared by Sentinel Safeguard LLC from the work recorded in Command. Figures are counts of real records; a quiet month reports zeros.\n",
      "state": "draft",
      "generatedBy": 1,
      "generatedAt": 1793763019733,
      "sentAt": null,
      "sentBy": null,
      "createdAt": 1793763019733,
      "updatedAt": 1793763019733
     }
    ],
    "surveySummary": {
     "count": 0,
     "avgRating": null,
     "recommendPct": null
    }
   }
  },
  "GET /api/app/reports/1/document": {
   "method": "GET",
   "path": "/api/app/reports/1/document",
   "status": 200,
   "body": {
    "ok": true,
    "id": 1,
    "fileName": "monthly-report-2026-10-client-1.pdf",
    "pdfBase64": {
     "_truncated": true,
     "chars": 1916,
     "sha256": "952de2ba1ec50ca79751511b0332177be0faf0aaec6ede1ece5dcdea4ed71396"
    },
    "pdfSha256": "cc322b5f198bb6f8dcaf60263d1fd06a40f918b224ab22e3d7df78c2f8e95521",
    "pdfSizeBytes": 1437
   }
  },
  "POST /api/app/reports/surveys": {
   "method": "POST",
   "path": "/api/app/reports/surveys",
   "status": 201,
   "body": {
    "ok": true,
    "id": 1,
    "surveyToken": "D-F0aC_P61H15IIcxd0RMAeym0GKJXJXAQJ9e9aTb9A",
    "message": "Survey created. Share the token link with the client \u2014 the token is shown once and only its hash is stored.",
    "survey": {
     "id": 1,
     "clientId": 1,
     "ticketId": 1,
     "caseId": null,
     "locationId": null,
     "status": "pending",
     "createdBy": 1,
     "createdAt": 1793763019996,
     "expiresAt": 1796355019996,
     "response": null
    }
   }
  },
  "GET /api/app/reports/surveys": {
   "method": "GET",
   "path": "/api/app/reports/surveys",
   "status": 200,
   "body": {
    "ok": true,
    "surveys": [
     {
      "id": 1,
      "clientId": 1,
      "ticketId": 1,
      "caseId": null,
      "locationId": null,
      "status": "pending",
      "createdBy": 1,
      "createdAt": 1793763019996,
      "expiresAt": 1796355019996,
      "response": null,
      "clientName": "Capture Fixture LLC"
     }
    ],
    "summary": {
     "count": 0,
     "avgRating": null,
     "recommendPct": null
    }
   }
  },
  "POST /api/app/reports/referrals": {
   "method": "POST",
   "path": "/api/app/reports/referrals",
   "status": 200,
   "body": {
    "ok": true,
    "id": 1,
    "message": "Referral saved.",
    "referral": {
     "id": 1,
     "referrerClientId": 1,
     "referrerName": "Capture Fixture LLC",
     "referredName": "Capture Prospect",
     "referredEmail": "",
     "referredBusiness": "",
     "status": "received",
     "leadId": null,
     "linkedClientId": null,
     "linkedClientName": "",
     "notes": "",
     "createdAt": 1793763020174,
     "updatedAt": 1793763020174
    }
   }
  },
  "GET /api/app/reports/referrals": {
   "method": "GET",
   "path": "/api/app/reports/referrals",
   "status": 200,
   "body": {
    "ok": true,
    "referrals": [
     {
      "id": 1,
      "referrerClientId": 1,
      "referrerName": "Capture Fixture LLC",
      "referredName": "Capture Prospect",
      "referredEmail": "",
      "referredBusiness": "",
      "status": "received",
      "leadId": null,
      "linkedClientId": null,
      "linkedClientName": "",
      "notes": "",
      "createdAt": 1793763020174,
      "updatedAt": 1793763020174
     }
    ],
    "countsByReferrer": {
     "1": 1
    }
   }
  },
  "POST /api/app/reports/threat-briefs": {
   "method": "POST",
   "path": "/api/app/reports/threat-briefs",
   "status": 200,
   "body": {
    "ok": true,
    "id": 1,
    "message": "Threat brief drafted; nothing was sent.",
    "brief": {
     "id": 1,
     "clientId": 1,
     "clientName": "Capture Fixture LLC",
     "period": "2026-11",
     "title": "Threat brief \u2014 Capture Fixture LLC \u2014 2026-11",
     "body": "Threat brief \u2014 Capture Fixture LLC \u2014 2026-11\n\nPrepared for internal review. Draft only \u2014 nothing in this brief has been sent to the client.\n\nSecurity posture: 85 out of 100.\n\nThreat Intel items matched to this client's profile: 0\n\nOpen findings on this client's engagements: 0\n\nBreach-monitoring matches: 0\n\nFacts only: breach entries state where data was reported to have surfaced, as recorded in Command. No leaked-data links are included, ever.\n",
     "sourceRefs": {
      "threatItemIds": [],
      "findingIds": [],
      "breachMatchIds": [],
      "posture": {
       "state": "scored",
       "score": 85,
       "baseline": 40,
       "factors": [
        {
         "label": "1 of 1 tickets and cases resolved or closed",
         "points": 25
        },
        {
         "label": "1 asset on file",
         "points": 15
        },
        {
         "label": "Activity in the last 90 days",
         "points": 5
        },
        {
         "label": "No form records on file",
         "points": 0
        }
       ],
       "counts": {
        "assets": 1,
        "openTickets": 0,
        "openCases": 0,
        "closedItems": 1,
        "totalItems": 1,
        "overdueInvoices": 0,
        "forms": 0
       }
      }
     },
     "state": "draft",
     "createdBy": 1,
     "createdAt": 1793763020334,
     "sentAt": null,
     "sentBy": null
    }
   }
  },
  "GET /api/app/reports/threat-briefs": {
   "method": "GET",
   "path": "/api/app/reports/threat-briefs",
   "status": 200,
   "body": {
    "ok": true,
    "briefs": [
     {
      "id": 1,
      "clientId": 1,
      "clientName": "Capture Fixture LLC",
      "period": "2026-11",
      "title": "Threat brief \u2014 Capture Fixture LLC \u2014 2026-11",
      "body": "Threat brief \u2014 Capture Fixture LLC \u2014 2026-11\n\nPrepared for internal review. Draft only \u2014 nothing in this brief has been sent to the client.\n\nSecurity posture: 85 out of 100.\n\nThreat Intel items matched to this client's profile: 0\n\nOpen findings on this client's engagements: 0\n\nBreach-monitoring matches: 0\n\nFacts only: breach entries state where data was reported to have surfaced, as recorded in Command. No leaked-data links are included, ever.\n",
      "sourceRefs": {
       "threatItemIds": [],
       "findingIds": [],
       "breachMatchIds": [],
       "posture": {
        "state": "scored",
        "score": 85,
        "baseline": 40,
        "factors": [
         {
          "label": "1 of 1 tickets and cases resolved or closed",
          "points": 25
         },
         {
          "label": "1 asset on file",
          "points": 15
         },
         {
          "label": "Activity in the last 90 days",
          "points": 5
         },
         {
          "label": "No form records on file",
          "points": 0
         }
        ],
        "counts": {
         "assets": 1,
         "openTickets": 0,
         "openCases": 0,
         "closedItems": 1,
         "totalItems": 1,
         "overdueInvoices": 0,
         "forms": 0
        }
       }
      },
      "state": "draft",
      "createdBy": 1,
      "createdAt": 1793763020334,
      "sentAt": null,
      "sentBy": null
     }
    ]
   }
  },
  "POST /api/app/reports/schedules": {
   "method": "POST",
   "path": "/api/app/reports/schedules",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Report schedule saved.",
    "id": 2,
    "schedule": {
     "id": 2,
     "kind": "owner_digest",
     "cadence": "weekly",
     "active": true,
     "nextRunAt": 1794367820500,
     "lastRunAt": null,
     "createdBy": 1,
     "createdAt": 1793763020500,
     "updatedAt": 1793763020500
    }
   }
  },
  "GET /api/app/reports/schedules": {
   "method": "GET",
   "path": "/api/app/reports/schedules",
   "status": 200,
   "body": {
    "schedules": [
     {
      "id": 2,
      "kind": "owner_digest",
      "cadence": "weekly",
      "active": true,
      "nextRunAt": 1794367820500,
      "lastRunAt": null,
      "createdBy": 1,
      "createdAt": 1793763020500,
      "updatedAt": 1793763020500
     },
     {
      "id": 1,
      "kind": "owner_digest",
      "cadence": "weekly",
      "active": true,
      "nextRunAt": 1794367810663,
      "lastRunAt": null,
      "createdBy": null,
      "createdAt": 1793763010663,
      "updatedAt": 1793763010663
     }
    ],
    "kinds": [
     "firm_pulse",
     "aging",
     "renewals",
     "owner_digest"
    ],
    "cadences": [
     "weekly",
     "monthly"
    ],
    "ok": true
   }
  },
  "POST /api/app/reports/schedules/2/run": {
   "method": "POST",
   "path": "/api/app/reports/schedules/2/run",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Report snapshot created (draft).",
    "snapshotId": 1,
    "nextRunAt": 1794367820681
   }
  },
  "GET /api/app/reports/snapshots": {
   "method": "GET",
   "path": "/api/app/reports/snapshots",
   "status": 200,
   "body": {
    "snapshots": [
     {
      "id": 1,
      "scheduleId": 2,
      "kind": "owner_digest",
      "state": "draft",
      "summary": "Owner Business Digest \u2014 week of 2026-11-02 (America/Chicago).\nRevenue invoiced: $0.00 across 0 invoice(s).\nRevenue collected: $0.00.\nPipeline: $0.00 in open quotes; 0 new lead(s), 2 new client(s) this week.\nTickets: 1 opened, 1 closed.\nOverdue: 0 invoice(s) ($0.00), 0 ticket(s).",
      "payload": {
       "kind": "owner_digest",
       "period": {
        "weekStart": "2026-11-02",
        "through": "2026-11-03",
        "label": "Week of 2026-11-02 (America/Chicago)"
       },
       "revenueInvoicedCents": 0,
       "revenueInvoicedCount": 0,
       "revenueCollectedCents": 0,
       "revenueSource": "cf_invoice_payments",
       "pipeline": {
        "openQuotesCents": 0,
        "newLeads": 0,
        "newClients": 2,
        "note": "Pipeline movement = new leads + new clients this week beside the current open-quote value; Command keeps no stage-change history, so none is invented."
       },
       "ticketsOpened": 1,
       "ticketsClosed": 1,
       "overdue": {
        "invoices": 0,
        "invoiceCents": 0,
        "tickets": 0
       },
       "definitions": "Invoiced: Sent/Paid invoices issued in the window at full invoice totals (Drafts excluded). Collected: the same payments basis Firm Pulse uses. Ticket flow: Firm Pulse ticketThroughput for the current week. Overdue invoices: the invoice shape's own overdue flag; overdue tickets: open, past due_date.",
       "emptyPeriod": false
      },
      "createdBy": 1,
      "createdAt": 1793763020677
     }
    ],
    "ok": true
   }
  },
  "GET /api/app/app-integrations": {
   "method": "GET",
   "path": "/api/app/app-integrations",
   "status": 200,
   "body": {
    "ok": true,
    "devices": [
     {
      "id": 3,
      "deviceName": "cap8b",
      "userId": 3,
      "userName": "NoPermCap8B",
      "userRole": "NoPermCap8B",
      "createdAt": 1793763012709,
      "firstSeenAt": 1793763012709,
      "lastSeenAt": 1793763013581,
      "sessionLastSeenAt": null,
      "expiresAt": 1801539013581,
      "appVersion": null,
      "status": "active",
      "revokedAt": null
     },
     {
      "id": 2,
      "deviceName": "cap8b",
      "userId": 2,
      "userName": "StaffCap8B",
      "userRole": "STAFF",
      "createdAt": 1793763012546,
      "firstSeenAt": 1793763012546,
      "lastSeenAt": 1793763019634,
      "sessionLastSeenAt": null,
      "expiresAt": 1801539019634,
      "appVersion": null,
      "status": "active",
      "revokedAt": null
     },
     {
      "id": 1,
      "deviceName": "cap8b",
      "userId": 1,
      "userName": "OwnerCap8B",
      "userRole": "OWNER",
      "createdAt": 1793763012393,
      "firstSeenAt": 1793763012393,
      "lastSeenAt": 1793763020793,
      "sessionLastSeenAt": 1793763011636,
      "expiresAt": 1801539020793,
      "appVersion": null,
      "status": "active",
      "revokedAt": null
     }
    ],
    "apiKeys": [],
    "webhooks": [],
    "deliveries": [],
    "events": [
     "lead_booked",
     "invoice_paid_manual"
    ],
    "backoffSeconds": [
     60,
     300,
     1800,
     7200
    ],
    "maxAttempts": 5,
    "leadRatePerHour": 120
   }
  },
  "GET /api/app/app-integrations [b4c_admin_staff_403]": {
   "method": "GET",
   "path": "/api/app/app-integrations",
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Owner only."
   }
  },
  "POST /api/app/app-integrations/api-keys": {
   "method": "POST",
   "path": "/api/app/app-integrations/api-keys",
   "status": 201,
   "body": {
    "ok": true,
    "message": "API key created -- copy it now; it is shown only once.",
    "key": {
     "_shape_only": true,
     "length": 49,
     "sha256": "d18a75ddc8611d86"
    },
    "apiKey": {
     "id": 1,
     "name": "Capture Partner",
     "sourceTag": "capture8b",
     "keyPrefix": "scapi_mKnKLM3t",
     "createdAt": 1793763021042,
     "lastUsedAt": null,
     "status": "active",
     "revokedAt": null
    }
   }
  },
  "POST /api/app/app-integrations/api-keys/1/revoke": {
   "method": "POST",
   "path": "/api/app/app-integrations/api-keys/1/revoke",
   "status": 200,
   "body": {
    "ok": true,
    "message": "API key revoked."
   }
  },
  "POST /api/app/app-integrations/webhooks": {
   "method": "POST",
   "path": "/api/app/app-integrations/webhooks",
   "status": 201,
   "body": {
    "ok": true,
    "message": "Webhook created -- copy the signing secret now; it is shown only once.",
    "secret": {
     "_shape_only": true,
     "length": 48,
     "sha256": "05f858b698eb985e"
    },
    "webhook": {
     "id": 1,
     "name": "Capture Hook",
     "url": "https://capture8b.example.invalid/hook",
     "events": [
      "lead_booked"
     ],
     "active": true,
     "createdAt": 1793763021581,
     "updatedAt": 1793763021581
    }
   }
  },
  "POST /api/app/app-integrations/webhooks/1/active": {
   "method": "POST",
   "path": "/api/app/app-integrations/webhooks/1/active",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Webhook updated.",
    "webhook": {
     "id": 1,
     "name": "Capture Hook",
     "url": "https://capture8b.example.invalid/hook",
     "events": [
      "lead_booked"
     ],
     "active": false,
     "createdAt": 1793763021581,
     "updatedAt": 1793763021843
    }
   }
  },
  "POST /api/app/app-integrations/deliveries/run": {
   "method": "POST",
   "path": "/api/app/app-integrations/deliveries/run",
   "status": 200,
   "body": {
    "ok": true,
    "summary": {
     "attempted": 0,
     "delivered": 0,
     "failed": 0,
     "dead": 0
    },
    "message": "Delivery pass complete."
   }
  },
  "POST /api/app/client-import/preview": {
   "method": "POST",
   "path": "/api/app/client-import/preview",
   "status": 200,
   "body": {
    "ok": true,
    "batch": {
     "id": 1,
     "fileName": "cap-clients.csv",
     "state": "dryrun",
     "totalRows": 1,
     "validRows": 1,
     "duplicateRows": 0,
     "errorRows": 0,
     "stagedCount": 0,
     "createdAt": 1793763022247,
     "confirmedAt": null,
     "rows": [
      {
       "id": 1,
       "rowNumber": 2,
       "verdict": "valid",
       "detail": "",
       "data": {
        "name": "Imported Capture LLC",
        "contactName": "Ivy Capture",
        "contacts": [],
        "email": "ivy@capture8b.example.invalid",
        "phone": "555-0110",
        "street": "",
        "city": "",
        "state": "",
        "zip": "",
        "type": "",
        "familySuggested": false,
        "notes": ""
       },
       "matches": [],
       "submissionId": null
      }
     ]
    },
    "ignoredColumns": [],
    "columnContract": [
     "name (required)",
     "contact_name",
     "contacts (';'-separated extra contact names)",
     "email",
     "phone",
     "address / street, city, state, zip",
     "type",
     "family (yes/no \u2014 a review suggestion only, never a link)",
     "notes"
    ],
    "message": "Dry run only \u2014 review the per-row verdicts, then confirm to stage the rows in the onboarding review queue. Nothing has been staged and no client has been created."
   }
  },
  "GET /api/app/client-import": {
   "method": "GET",
   "path": "/api/app/client-import",
   "status": 200,
   "body": {
    "batches": [
     {
      "id": 1,
      "fileName": "cap-clients.csv",
      "state": "dryrun",
      "totalRows": 1,
      "validRows": 1,
      "duplicateRows": 0,
      "errorRows": 0,
      "stagedCount": 0,
      "createdAt": 1793763022247,
      "confirmedAt": null
     }
    ],
    "columnContract": [
     "name (required)",
     "contact_name",
     "contacts (';'-separated extra contact names)",
     "email",
     "phone",
     "address / street, city, state, zip",
     "type",
     "family (yes/no \u2014 a review suggestion only, never a link)",
     "notes"
    ],
    "ok": true
   }
  },
  "GET /api/app/client-import/1": {
   "method": "GET",
   "path": "/api/app/client-import/1",
   "status": 200,
   "body": {
    "batch": {
     "id": 1,
     "fileName": "cap-clients.csv",
     "state": "dryrun",
     "totalRows": 1,
     "validRows": 1,
     "duplicateRows": 0,
     "errorRows": 0,
     "stagedCount": 0,
     "createdAt": 1793763022247,
     "confirmedAt": null,
     "rows": [
      {
       "id": 1,
       "rowNumber": 2,
       "verdict": "valid",
       "detail": "",
       "data": {
        "name": "Imported Capture LLC",
        "contactName": "Ivy Capture",
        "contacts": [],
        "email": "ivy@capture8b.example.invalid",
        "phone": "555-0110",
        "street": "",
        "city": "",
        "state": "",
        "zip": "",
        "type": "",
        "familySuggested": false,
        "notes": ""
       },
       "matches": [],
       "submissionId": null
      }
     ]
    },
    "columnContract": [
     "name (required)",
     "contact_name",
     "contacts (';'-separated extra contact names)",
     "email",
     "phone",
     "address / street, city, state, zip",
     "type",
     "family (yes/no \u2014 a review suggestion only, never a link)",
     "notes"
    ],
    "ok": true
   }
  },
  "POST /api/app/client-import/1/confirm": {
   "method": "POST",
   "path": "/api/app/client-import/1/confirm",
   "status": 200,
   "body": {
    "ok": true,
    "id": 1,
    "staged": [
     {
      "rowNumber": 2,
      "submissionId": 1,
      "clientId": null
     }
    ],
    "skipped": [],
    "stagedCount": 1,
    "message": "Staged 1 row(s) in the onboarding review queue. No client was created \u2014 a human merges each row in the wizard."
   }
  },
  "GET /api/app/packets-routes/ops": {
   "method": "GET",
   "path": "/api/app/packets-routes/ops",
   "status": 200,
   "body": {
    "ok": true,
    "statements": [],
    "followups": [],
    "urgent": {
     "total": 0,
     "items": [],
     "config": {
      "claimStaleDays": 14,
      "urgent": {
       "quietStart": "22:00",
       "quietEnd": "07:00",
       "bannerSeverities": [
        "Critical"
       ],
       "digestSeverities": []
      }
     },
     "latencyNote": "How fast this moves: the banner below is computed live every time Today loads, so a new Critical Alert shows on the next Today load. The internal owner-alert draft is written by the automation tick, which runs every 60 seconds by default -- worst case about 60 seconds after the alert is filed."
    },
    "config": {
     "claimStaleDays": 14,
     "urgent": {
      "quietStart": "22:00",
      "quietEnd": "07:00",
      "bannerSeverities": [
       "Critical"
      ],
      "digestSeverities": []
     }
    }
   }
  },
  "GET /api/app/packets-routes/packets": {
   "method": "GET",
   "path": "/api/app/packets-routes/packets",
   "status": 200,
   "body": {
    "ok": true,
    "eligible": [
     {
      "clientId": 2,
      "clientName": "Capture No Email LLC",
      "contactName": "Ned Capture",
      "address": "",
      "invoices": [
       {
        "id": 1,
        "invoiceNumber": "INV-0001",
        "issueDate": "",
        "dueDate": "",
        "totalCents": 7500,
        "overdue": false,
        "packetId": null
       }
      ]
     }
    ],
    "familyBlocked": [],
    "batches": []
   }
  },
  "POST /api/app/packets-routes/packets": {
   "method": "POST",
   "path": "/api/app/packets-routes/packets",
   "status": 200,
   "body": {
    "ok": true,
    "batch": {
     "id": 1,
     "state": "built",
     "clientCount": 1,
     "invoiceCount": 1,
     "createdBy": 1,
     "createdByName": "OwnerCap8B",
     "createdAt": 1793763023758,
     "mailedBy": null,
     "mailedByName": "",
     "mailedAt": null,
     "clients": [
      "Capture No Email LLC"
     ],
     "invoices": [
      "INV-0001"
     ]
    },
    "warnings": [],
    "message": "Packet batch #1 built. Print it, post it, then mark it mailed by hand."
   }
  },
  "POST /api/app/packets-routes/packets/1/mark-mailed": {
   "method": "POST",
   "path": "/api/app/packets-routes/packets/1/mark-mailed",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Batch #1 marked mailed by hand."
   }
  },
  "GET /api/app/packets-routes/route": {
   "method": "GET",
   "path": "/api/app/packets-routes/route",
   "status": 200,
   "body": {
    "ok": true,
    "date": "2026-11-03",
    "readOnly": true,
    "method": "Stops with stored coordinates are ordered nearest-neighbor + 2-opt over straight-line distance. No client in Command has stored coordinates today (addresses are text only), so stops list as unmapped in scheduled-time order. No external geocoding API is called -- ever.",
    "stops": [],
    "mappedCount": 0,
    "unmappedCount": 0,
    "driveMiles": null,
    "driveNote": "No drive estimate: no stop for this day has stored coordinates to compute one from.",
    "savedOrder": null
   }
  },
  "GET /api/app/packets-routes/claims": {
   "method": "GET",
   "path": "/api/app/packets-routes/claims",
   "status": 200,
   "body": {
    "ok": true,
    "claims": [],
    "followups": [],
    "config": {
     "claimStaleDays": 14,
     "urgent": {
      "quietStart": "22:00",
      "quietEnd": "07:00",
      "bannerSeverities": [
       "Critical"
      ],
      "digestSeverities": []
     }
    },
    "statuses": [
     "filed",
     "shipped",
     "vendor_decision",
     "resolved",
     "replaced",
     "denied"
    ]
   }
  },
  "GET /api/app/collections": {
   "method": "GET",
   "path": "/api/app/collections",
   "status": 200,
   "body": {
    "today": "2026-11-03",
    "buckets": [
     {
      "bucket": "current",
      "totalCents": 7500,
      "count": 1,
      "invoices": [
       {
        "invoiceId": 1,
        "invoiceNumber": "INV-0001",
        "clientId": 2,
        "clientName": "Capture No Email LLC",
        "status": "Sent",
        "issueDate": "",
        "dueDate": "",
        "bucket": "current",
        "daysOverdue": 0,
        "totalCents": 7500,
        "paidCents": 0,
        "balanceDueCents": 7500,
        "workflowState": "none",
        "workflowNote": "",
        "workflowUpdatedAt": null
       }
      ]
     },
     {
      "bucket": "1-30",
      "totalCents": 0,
      "count": 0,
      "invoices": []
     },
     {
      "bucket": "31-60",
      "totalCents": 0,
      "count": 0,
      "invoices": []
     },
     {
      "bucket": "61-90",
      "totalCents": 0,
      "count": 0,
      "invoices": []
     },
     {
      "bucket": "90+",
      "totalCents": 0,
      "count": 0,
      "invoices": []
     }
    ],
    "totalCents": 7500,
    "totalCount": 1,
    "ok": true
   }
  },
  "POST /api/app/collections/invoices/2/workflow": {
   "method": "POST",
   "path": "/api/app/collections/invoices/2/workflow",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Collections workflow updated.",
    "workflow": {
     "invoiceId": 2,
     "state": "reminded",
     "note": "",
     "updatedAt": 1793763024958
    },
    "invoiceStatus": "Draft"
   }
  },
  "GET /api/app/staff-gear/directory": {
   "method": "GET",
   "path": "/api/app/staff-gear/directory",
   "status": 200,
   "body": {
    "staff": [
     {
      "id": 3,
      "displayName": "NoPermCap8B",
      "role": "NoPermCap8B",
      "portalLang": "en",
      "clientId": null,
      "jobTitle": "",
      "email": "",
      "phone": "",
      "active": true,
      "createdAt": 1793763012204,
      "certs": [],
      "training": {
       "inProgress": 0,
       "completed": 0,
       "abandoned": 0
      },
      "equipment": []
     },
     {
      "id": 1,
      "displayName": "OwnerCap8B",
      "role": "OWNER",
      "portalLang": "en",
      "clientId": null,
      "jobTitle": "",
      "email": "",
      "phone": "",
      "active": true,
      "createdAt": 1793763011465,
      "certs": [],
      "training": {
       "inProgress": 0,
       "completed": 0,
       "abandoned": 0
      },
      "equipment": []
     },
     {
      "id": 4,
      "displayName": "ProvCap8B",
      "role": "STAFF",
      "portalLang": "en",
      "clientId": null,
      "jobTitle": "",
      "email": "prov@capture8b.example.invalid",
      "phone": "",
      "active": true,
      "createdAt": 1793763014885,
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
      "displayName": "StaffCap8B",
      "role": "STAFF",
      "portalLang": "en",
      "clientId": null,
      "jobTitle": "Tech",
      "email": "staff@capture8b.example.invalid",
      "phone": "555-0108",
      "active": true,
      "createdAt": 1793763011800,
      "certs": [],
      "training": {
       "inProgress": 0,
       "completed": 0,
       "abandoned": 0
      },
      "equipment": []
     }
    ],
    "total": 4,
    "ok": true
   }
  },
  "GET /api/app/staff-gear/training": {
   "method": "GET",
   "path": "/api/app/staff-gear/training",
   "status": 200,
   "body": {
    "trainings": [],
    "counts": {
     "inProgress": 0,
     "completed": 0,
     "abandoned": 0
    },
    "ok": true
   }
  },
  "GET /api/app/staff-gear/equipment": {
   "method": "GET",
   "path": "/api/app/staff-gear/equipment",
   "status": 200,
   "body": {
    "equipment": [],
    "ok": true
   }
  },
  "POST /api/app/staff-gear/certs": {
   "method": "POST",
   "path": "/api/app/staff-gear/certs",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Certification saved.",
    "id": 1,
    "cert": {
     "id": 1,
     "userId": 2,
     "staffName": "StaffCap8B",
     "name": "Capture Cert 8B",
     "issuer": "CaptureOrg",
     "issuedDate": "",
     "expiresDate": "",
     "daysUntilExpiry": null,
     "expiryState": "none",
     "notes": "",
     "createdAt": 1793763025497,
     "updatedAt": 1793763025497
    }
   }
  },
  "POST /api/app/staff-gear/equipment": {
   "method": "POST",
   "path": "/api/app/staff-gear/equipment",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Equipment saved.",
    "id": 1,
    "equipment": {
     "id": 1,
     "name": "Capture Ladder 8B",
     "kind": "tool",
     "serial": "",
     "assignedTo": 2,
     "assignedToName": "StaffCap8B",
     "assignedAt": 1793763025738,
     "returnedAt": null,
     "serviceDue": "",
     "notes": "",
     "mileageExpenses": [],
     "mileageTotalCents": 0,
     "mileageTotalMiles": 0,
     "createdAt": 1793763025738,
     "updatedAt": 1793763025738
    }
   }
  },
  "GET /api/app/automation/assign-rules": {
   "method": "GET",
   "path": "/api/app/automation/assign-rules",
   "status": 200,
   "body": {
    "rules": [],
    "staff": [
     {
      "id": 1,
      "displayName": "OwnerCap8B",
      "role": "OWNER"
     },
     {
      "id": 4,
      "displayName": "ProvCap8B",
      "role": "STAFF"
     },
     {
      "id": 2,
      "displayName": "StaffCap8B",
      "role": "STAFF"
     }
    ],
    "priorities": [
     "Critical",
     "High",
     "Normal",
     "Low"
    ],
    "ok": true
   }
  },
  "POST /api/app/automation/assign-rules": {
   "method": "POST",
   "path": "/api/app/automation/assign-rules",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Assignment rule saved.",
    "id": 1,
    "rule": {
     "id": 1,
     "name": "Capture Rule 8B",
     "matchCategory": "Support",
     "matchPriority": "",
     "matchSource": "",
     "assignTo": 2,
     "assignToName": "StaffCap8B",
     "enabled": true,
     "sortOrder": 0,
     "createdAt": 1793763026272,
     "updatedAt": 1793763026272
    }
   }
  },
  "GET /api/app/automation/escalation-config": {
   "method": "GET",
   "path": "/api/app/automation/escalation-config",
   "status": 200,
   "body": {
    "config": [
     {
      "id": 1,
      "priority": "*",
      "thresholdPct": 75,
      "enabled": true
     },
     {
      "id": 2,
      "priority": "*",
      "thresholdPct": 100,
      "enabled": true
     }
    ],
    "defaults": [
     75,
     100
    ],
    "priorities": [
     "*",
     "Critical",
     "High",
     "Normal",
     "Low"
    ],
    "ok": true
   }
  },
  "GET /api/app/automation/escalations": {
   "method": "GET",
   "path": "/api/app/automation/escalations",
   "status": 200,
   "body": {
    "escalations": [],
    "ok": true
   }
  },
  "GET /api/app/automation/quote-followups": {
   "method": "GET",
   "path": "/api/app/automation/quote-followups",
   "status": 200,
   "body": {
    "followUps": [],
    "ok": true
   }
  },
  "GET /api/app/automation/contract-reminders": {
   "method": "GET",
   "path": "/api/app/automation/contract-reminders",
   "status": 200,
   "body": {
    "reminders": [],
    "ok": true
   }
  },
  "GET /api/app/client-ops/maintenance-windows?clientId=1": {
   "method": "GET",
   "path": "/api/app/client-ops/maintenance-windows?clientId=1",
   "status": 200,
   "body": {
    "windows": [],
    "ok": true
   }
  },
  "POST /api/app/client-ops/maintenance-windows": {
   "method": "POST",
   "path": "/api/app/client-ops/maintenance-windows",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Maintenance window saved.",
    "id": 1,
    "window": {
     "id": 1,
     "clientId": 1,
     "clientName": "Capture Fixture LLC",
     "title": "Capture Window 8B",
     "description": "",
     "startsAt": 1794000000000,
     "endsAt": 1794007200000,
     "status": "planned",
     "hasConflict": false,
     "conflicts": [],
     "createdAt": 1793763027155,
     "updatedAt": 1793763027155
    },
    "conflicts": [],
    "hasConflict": false
   }
  },
  "GET /api/app/client-ops/retainers?clientId=1": {
   "method": "GET",
   "path": "/api/app/client-ops/retainers?clientId=1",
   "status": 200,
   "body": {
    "retainers": [],
    "balances": {
     "total": 0,
     "items": []
    },
    "ok": true
   }
  },
  "POST /api/app/client-ops/retainers": {
   "method": "POST",
   "path": "/api/app/client-ops/retainers",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Retainer saved.",
    "id": 1,
    "retainer": {
     "id": 1,
     "clientId": 1,
     "periodLabel": "",
     "active": true,
     "createdAt": 1793763027601,
     "updatedAt": 1793763027601,
     "purchasedHours": 0.0,
     "adjustmentHours": 0.0,
     "drawnHours": 0.0,
     "balanceHours": 0.0
    }
   }
  },
  "POST /api/app/client-ops/emergency-contacts": {
   "method": "POST",
   "path": "/api/app/client-ops/emergency-contacts",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Emergency contact saved.",
    "id": 1
   }
  },
  "GET /api/app/client-ops/emergency-contacts?clientId=1": {
   "method": "GET",
   "path": "/api/app/client-ops/emergency-contacts?clientId=1",
   "status": 200,
   "body": {
    "contacts": [
     {
      "id": 1,
      "clientId": 1,
      "name": "Capture Emergency",
      "roleTitle": "",
      "phone": "555-0111",
      "email": "",
      "priorityOrder": 1,
      "notes": "",
      "createdAt": 1793763027823,
      "updatedAt": 1793763027823
     }
    ],
    "sentinelContact": {
     "name": "Capture Workspace",
     "phone": "816-533-8418",
     "email": "sentinel@sentinelsafeguard.net",
     "siteUrl": "",
     "note": "By appointment only \u2014 Kansas City, Missouri."
    },
    "ok": true
   }
  },
  "POST /api/app/pipeline-packets/leads": {
   "method": "POST",
   "path": "/api/app/pipeline-packets/leads",
   "status": 200,
   "body": {
    "ok": true,
    "id": 1,
    "created": true,
    "deduped": false,
    "message": "Lead added.",
    "lead": {
     "id": 1,
     "name": "",
     "business": "Capture Lead Co 8B",
     "email": "lou@capture8b.example.invalid",
     "phone": "",
     "source": "manual",
     "notes": "",
     "state": "new",
     "convertedClientId": null,
     "conversionSubmissionId": null,
     "createdAt": 1793763028107,
     "updatedAt": 1793763028107
    }
   }
  },
  "GET /api/app/pipeline-packets/leads": {
   "method": "GET",
   "path": "/api/app/pipeline-packets/leads",
   "status": 200,
   "body": {
    "leads": [
     {
      "id": 1,
      "name": "",
      "business": "Capture Lead Co 8B",
      "email": "lou@capture8b.example.invalid",
      "phone": "",
      "source": "manual",
      "notes": "",
      "state": "new",
      "convertedClientId": null,
      "conversionSubmissionId": null,
      "createdAt": 1793763028107,
      "updatedAt": 1793763028107
     }
    ],
    "counts": {
     "new": 1
    },
    "states": [
     "new",
     "contacted",
     "qualified",
     "conversion_pending",
     "converted",
     "lost"
    ],
    "sources": [
     "website_contact",
     "website_refer",
     "manual",
     "contact-form",
     "intake-wizard",
     "other"
    ],
    "ok": true
   }
  },
  "POST /api/app/pipeline-packets/leads/1/state": {
   "method": "POST",
   "path": "/api/app/pipeline-packets/leads/1/state",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Lead state updated.",
    "lead": {
     "id": 1,
     "name": "",
     "business": "Capture Lead Co 8B",
     "email": "lou@capture8b.example.invalid",
     "phone": "",
     "source": "manual",
     "notes": "",
     "state": "contacted",
     "convertedClientId": null,
     "conversionSubmissionId": null,
     "createdAt": 1793763028107,
     "updatedAt": 1793763028444
    }
   }
  },
  "POST /api/app/pipeline-packets/leads/1/convert": {
   "method": "POST",
   "path": "/api/app/pipeline-packets/leads/1/convert",
   "status": 200,
   "body": {
    "ok": true,
    "submissionId": 2,
    "message": "Lead queued in the onboarding review queue. A client is created only when the owner approves it there.",
    "lead": {
     "id": 1,
     "name": "",
     "business": "Capture Lead Co 8B",
     "email": "lou@capture8b.example.invalid",
     "phone": "",
     "source": "manual",
     "notes": "",
     "state": "conversion_pending",
     "convertedClientId": null,
     "conversionSubmissionId": 2,
     "createdAt": 1793763028107,
     "updatedAt": 1793763028578
    }
   }
  },
  "GET /api/app/pipeline-packets/proposals": {
   "method": "GET",
   "path": "/api/app/pipeline-packets/proposals",
   "status": 200,
   "body": {
    "proposals": [],
    "ok": true
   }
  },
  "GET /api/app/pipeline-packets/change-requests": {
   "method": "GET",
   "path": "/api/app/pipeline-packets/change-requests",
   "status": 200,
   "body": {
    "changeRequests": [],
    "ok": true
   }
  },
  "POST /api/app/admin/sessions/revoke-all": {
   "method": "POST",
   "path": "/api/app/admin/sessions/revoke-all",
   "status": 200,
   "body": {
    "ok": true,
    "message": "Revoked 1 session(s).",
    "count": 1
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

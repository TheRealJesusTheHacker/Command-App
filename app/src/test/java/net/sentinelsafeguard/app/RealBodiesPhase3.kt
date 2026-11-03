package net.sentinelsafeguard.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Phase 3 REAL response bodies, captured 2026-11-03 from a
 * rehearsal instance running the byte-exact deployed Phase 3
 * backend (server.py sha256
 * 5344879a61b5929cda8dfffd955a2f314abab80ff16968e0bdb6e2552b3efb95,
 * Addendum 14), against a seeded scratch database. No tokens or
 * secrets were recorded. [ALL] is the captures file verbatim
 * (165 captures: name -> {status, body}); binary downloads are
 * recorded as {"_binary": true, size, sha256} markers, exactly as
 * captured. These pin every Phase 3 endpoint the hunt7 app
 * consumes to the real contract.
 */
object RealBodiesPhase3 {
    val ALL: String = """{
 "_meta": {
  "kind": "phase3-rehearsal-captures",
  "serverPySha256": "5344879a61b5929cda8dfffd955a2f314abab80ff16968e0bdb6e2552b3efb95",
  "note": "Real bodies from rehearsal instance of deployed bytes; scratch DB shredded after capture; no tokens or secrets recorded."
 },
 "client_note_create": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Note saved.",
   "note": {
    "id": 1,
    "clientId": 1,
    "body": "First staff note",
    "pinned": false,
    "authorId": 1,
    "authorName": "OwnerP3",
    "edits": [],
    "createdAt": 1793728746673,
    "updatedAt": 1793728746673
   }
  }
 },
 "client_notes_list": {
  "status": 200,
  "body": {
   "clientId": 1,
   "notes": [
    {
     "id": 1,
     "clientId": 1,
     "body": "First staff note",
     "pinned": false,
     "authorId": 1,
     "authorName": "OwnerP3",
     "edits": [],
     "createdAt": 1793728746673,
     "updatedAt": 1793728746673
    }
   ],
   "ok": true
  }
 },
 "client_note_edit": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Note saved.",
   "note": {
    "id": 1,
    "clientId": 1,
    "body": "First staff note (edited)",
    "pinned": false,
    "authorId": 1,
    "authorName": "OwnerP3",
    "edits": [
     {
      "body": "First staff note",
      "editedBy": 1,
      "editedAt": 1793728747242
     }
    ],
    "createdAt": 1793728746673,
    "updatedAt": 1793728747242
   }
  }
 },
 "client_note_pin": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Note pin updated.",
   "note": {
    "id": 1,
    "clientId": 1,
    "body": "First staff note (edited)",
    "pinned": true,
    "authorId": 1,
    "authorName": "OwnerP3",
    "edits": [
     {
      "body": "First staff note",
      "editedBy": 1,
      "editedAt": 1793728747242
     }
    ],
    "createdAt": 1793728746673,
    "updatedAt": 1793728747974
   }
  }
 },
 "client_contact_create": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Contact saved.",
   "id": 1,
   "contact": {
    "id": 1,
    "clientId": 1,
    "name": "Second Contact",
    "email": "second@example.invalid",
    "kind": "billing",
    "active": true,
    "userId": null,
    "createdAt": 1793728748440,
    "updatedAt": 1793728748440
   }
  }
 },
 "client_contacts_list": {
  "status": 200,
  "body": {
   "clientId": 1,
   "contacts": [
    {
     "id": 1,
     "clientId": 1,
     "name": "Second Contact",
     "email": "second@example.invalid",
     "kind": "billing",
     "active": true,
     "userId": null,
     "createdAt": 1793728748440,
     "updatedAt": 1793728748440
    }
   ],
   "ok": true
  }
 },
 "client_contact_edit": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Contact saved.",
   "id": 1,
   "contact": {
    "id": 1,
    "clientId": 1,
    "name": "Second Contact Jr",
    "email": "",
    "kind": "primary",
    "active": true,
    "userId": null,
    "createdAt": 1793728748440,
    "updatedAt": 1793728750813
   }
  }
 },
 "client_contact_invite_refusal": {
  "status": 403,
  "body": {
   "ok": false,
   "authorized": false,
   "message": "A username is required (80 characters max)."
  }
 },
 "client_notification_prefs_get": {
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
 "client_notification_prefs_set": {
  "status": 200,
  "body": {
   "ok": true,
   "clientId": 1,
   "prefs": {
    "ticketUpdates": true,
    "invoiceEvents": true,
    "monitoringAlerts": true,
    "digest": false
   },
   "message": "Notification preferences saved."
  }
 },
 "client_outreach_draft": {
  "status": 200,
  "body": {
   "ok": true,
   "clientId": 1,
   "channel": "email",
   "framing": "generic",
   "framingLabel": "Small business (general)",
   "draft": "Subject: A quick hello from Sentinel Safeguard in Kansas City\n\nHello Pat P,\n\nMy name is Skyler Paul Henderson, and I'm the President of Sentinel Safeguard LLC here in Kansas City. Sentinel works on cybersecurity, IT, and security systems for businesses and homes in the Kansas City area, by appointment.\n\nMost small businesses run on a handful of connected systems \u2014 email, invoicing, customer records, cameras, and the phones and computers the work happens on. A short look at how those are set up often turns up one or two simple fixes that make the business harder to get into, without changing how the work gets done.\n\nI'm writing to offer Phase3 Client LLC a free look at your setup \u2014 a short, no-pressure conversation about what you have, what looks solid, and anything that could use attention. If something needs fixing, I'll tell you plainly what I found. If it doesn't, I'll tell you that too, and that will be the end of it.\n\nIf you're open to it, just reply to this email or call me at 816-533-8418 and we'll find a time that suits you.\n\nThank you for your time,\nSkyler Paul Henderson\nPresident, Sentinel Safeguard LLC\nKansas City, Missouri\n816-533-8418 \u00b7 Sentinelsafeguard@outlook.com\nBy appointment only"
  }
 },
 "client_outreach_draft_save": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Draft saved to the client's notes, marked as a draft.",
   "id": 1
  }
 },
 "client_security_get": {
  "status": 200,
  "body": {
   "ok": true,
   "clientId": 1,
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
   "roadmap": [],
   "risks": [],
   "breachWatches": [],
   "breachMatches": [],
   "compliance": [],
   "complianceSummary": [],
   "complianceFrameworks": [
    "NIST CSF",
    "HIPAA",
    "PCI DSS"
   ],
   "complianceTemplates": {
    "NIST CSF": [
     {
      "controlId": "GV",
      "title": "Govern"
     },
     {
      "controlId": "ID",
      "title": "Identify"
     },
     {
      "controlId": "PR",
      "title": "Protect"
     },
     {
      "controlId": "DE",
      "title": "Detect"
     },
     {
      "controlId": "RS",
      "title": "Respond"
     },
     {
      "controlId": "RC",
      "title": "Recover"
     }
    ],
    "HIPAA": [
     {
      "controlId": "HIPAA-ADMIN",
      "title": "Administrative safeguards"
     },
     {
      "controlId": "HIPAA-PHYS",
      "title": "Physical safeguards"
     },
     {
      "controlId": "HIPAA-TECH",
      "title": "Technical safeguards"
     },
     {
      "controlId": "HIPAA-ORG",
      "title": "Organizational requirements"
     },
     {
      "controlId": "HIPAA-DOC",
      "title": "Policies, procedures and documentation"
     },
     {
      "controlId": "HIPAA-PRIV",
      "title": "Privacy Rule administrative requirements"
     }
    ],
    "PCI DSS": [
     {
      "controlId": "PCI-1",
      "title": "Requirement 1 - Network security controls"
     },
     {
      "controlId": "PCI-2",
      "title": "Requirement 2 - Secure configurations"
     },
     {
      "controlId": "PCI-3",
      "title": "Requirement 3 - Protect stored account data"
     },
     {
      "controlId": "PCI-4",
      "title": "Requirement 4 - Protect cardholder data in transit"
     },
     {
      "controlId": "PCI-5",
      "title": "Requirement 5 - Protect against malicious software"
     },
     {
      "controlId": "PCI-6",
      "title": "Requirement 6 - Secure systems and software"
     },
     {
      "controlId": "PCI-7",
      "title": "Requirement 7 - Restrict access by business need"
     },
     {
      "controlId": "PCI-8",
      "title": "Requirement 8 - Identify users and authenticate access"
     },
     {
      "controlId": "PCI-9",
      "title": "Requirement 9 - Restrict physical access"
     },
     {
      "controlId": "PCI-10",
      "title": "Requirement 10 - Log and monitor access"
     },
     {
      "controlId": "PCI-11",
      "title": "Requirement 11 - Test security regularly"
     },
     {
      "controlId": "PCI-12",
      "title": "Requirement 12 - Information security policies"
     }
    ]
   }
  }
 },
 "client_security_roadmap_create": {
  "status": 200,
  "body": {
   "ok": true,
   "id": 1,
   "item": {
    "id": 1,
    "clientId": 1,
    "title": "Enable MFA everywhere",
    "detail": "Roll out TOTP.",
    "priority": "High",
    "source": "manual",
    "sourceKey": "",
    "state": "planned",
    "linkedRiskId": null,
    "archivedAt": null,
    "createdAt": 1793728752939,
    "updatedAt": 1793728752939
   }
  }
 },
 "client_security_roadmap_generate": {
  "status": 200,
  "body": {
   "ok": true,
   "created": 3,
   "existing": 0,
   "roadmap": [
    {
     "id": 1,
     "clientId": 1,
     "title": "Enable MFA everywhere",
     "detail": "Roll out TOTP.",
     "priority": "High",
     "source": "manual",
     "sourceKey": "",
     "state": "planned",
     "linkedRiskId": null,
     "archivedAt": null,
     "createdAt": 1793728752939,
     "updatedAt": 1793728752939
    },
    {
     "id": 2,
     "clientId": 1,
     "title": "Build an asset inventory",
     "detail": "Posture input: no assets on file for this client (the score's asset-inventory factor contributes 0).",
     "priority": "High",
     "source": "posture-input",
     "sourceKey": "posture:no-assets",
     "state": "planned",
     "linkedRiskId": null,
     "archivedAt": null,
     "createdAt": 1793728753769,
     "updatedAt": 1793728753769
    },
    {
     "id": 3,
     "clientId": 1,
     "title": "Address: 1 open normal-priority ticket",
     "detail": "Posture factor from the Client Security Score: 1 open normal-priority ticket (-3 points).",
     "priority": "Normal",
     "source": "posture-input",
     "sourceKey": "posture:1-open-normal-priority-ticket",
     "state": "planned",
     "linkedRiskId": null,
     "archivedAt": null,
     "createdAt": 1793728753769,
     "updatedAt": 1793728753769
    },
    {
     "id": 4,
     "clientId": 1,
     "title": "File core engagement documents",
     "detail": "Posture input: no form records on file for this client (the score's form-records factor contributes 0).",
     "priority": "Low",
     "source": "posture-input",
     "sourceKey": "posture:no-forms",
     "state": "planned",
     "linkedRiskId": null,
     "archivedAt": null,
     "createdAt": 1793728753769,
     "updatedAt": 1793728753769
    }
   ]
  }
 },
 "client_security_roadmap_state": {
  "status": 200,
  "body": {
   "ok": true,
   "id": 1,
   "item": {
    "id": 1,
    "clientId": 1,
    "title": "Enable MFA everywhere",
    "detail": "Roll out TOTP.",
    "priority": "High",
    "source": "manual",
    "sourceKey": "",
    "state": "in_progress",
    "linkedRiskId": null,
    "archivedAt": null,
    "createdAt": 1793728752939,
    "updatedAt": 1793728754465
   }
  }
 },
 "client_security_risk_create": {
  "status": 200,
  "body": {
   "ok": true,
   "id": 1,
   "risk": {
    "id": 1,
    "clientId": 1,
    "title": "Legacy VPN",
    "description": "",
    "likelihood": 3,
    "impact": 3,
    "riskScore": 9,
    "compensatingControls": "",
    "reviewDate": "",
    "state": "open",
    "roadmapItemId": null,
    "reviewDue": false,
    "acceptanceExpired": false,
    "acceptedBy": null,
    "acceptedAt": null,
    "createdAt": 1793728755204,
    "updatedAt": 1793728755204
   }
  }
 },
 "client_security_risk_state": {
  "status": 200,
  "body": {
   "ok": true,
   "id": 1,
   "risk": {
    "id": 1,
    "clientId": 1,
    "title": "Legacy VPN",
    "description": "",
    "likelihood": 3,
    "impact": 3,
    "riskScore": 9,
    "compensatingControls": "",
    "reviewDate": "",
    "state": "accepted",
    "roadmapItemId": null,
    "reviewDue": false,
    "acceptanceExpired": false,
    "acceptedBy": 1,
    "acceptedAt": 1793728755683,
    "createdAt": 1793728755204,
    "updatedAt": 1793728755683
   }
  }
 },
 "client_security_breach_watch_create": {
  "status": 200,
  "body": {
   "ok": true,
   "id": 1,
   "watch": {
    "id": 1,
    "clientId": 1,
    "domain": "example.invalid",
    "active": true,
    "createdAt": 1793728756060,
    "updatedAt": 1793728756060
   }
  }
 },
 "client_security_breach_match": {
  "status": 200,
  "body": {
   "ok": true,
   "created": 0,
   "watches": 1,
   "matches": 0
  }
 },
 "client_security_breach_watch_delete": {
  "status": 200,
  "body": {
   "ok": true,
   "id": 1,
   "message": "Watch paused; its match history is kept."
  }
 },
 "client_security_compliance_seed": {
  "status": 200,
  "body": {
   "ok": true,
   "created": 6,
   "framework": "NIST CSF",
   "items": [
    {
     "id": 1,
     "clientId": 1,
     "framework": "NIST CSF",
     "controlId": "GV",
     "title": "Govern",
     "state": "not_started",
     "evidenceRef": "",
     "notes": "",
     "createdAt": 1793728758186,
     "updatedAt": 1793728758186
    },
    {
     "id": 2,
     "clientId": 1,
     "framework": "NIST CSF",
     "controlId": "ID",
     "title": "Identify",
     "state": "not_started",
     "evidenceRef": "",
     "notes": "",
     "createdAt": 1793728758186,
     "updatedAt": 1793728758186
    },
    {
     "id": 3,
     "clientId": 1,
     "framework": "NIST CSF",
     "controlId": "PR",
     "title": "Protect",
     "state": "not_started",
     "evidenceRef": "",
     "notes": "",
     "createdAt": 1793728758186,
     "updatedAt": 1793728758186
    },
    {
     "id": 4,
     "clientId": 1,
     "framework": "NIST CSF",
     "controlId": "DE",
     "title": "Detect",
     "state": "not_started",
     "evidenceRef": "",
     "notes": "",
     "createdAt": 1793728758186,
     "updatedAt": 1793728758186
    },
    {
     "id": 5,
     "clientId": 1,
     "framework": "NIST CSF",
     "controlId": "RS",
     "title": "Respond",
     "state": "not_started",
     "evidenceRef": "",
     "notes": "",
     "createdAt": 1793728758186,
     "updatedAt": 1793728758186
    },
    {
     "id": 6,
     "clientId": 1,
     "framework": "NIST CSF",
     "controlId": "RC",
     "title": "Recover",
     "state": "not_started",
     "evidenceRef": "",
     "notes": "",
     "createdAt": 1793728758186,
     "updatedAt": 1793728758186
    }
   ],
   "summary": [
    {
     "framework": "NIST CSF",
     "total": 6,
     "compliant": 0,
     "inProgress": 0,
     "notStarted": 6,
     "notApplicable": 0,
     "percent": 0
    }
   ]
  }
 },
 "client_security_compliance_set": {
  "status": 200,
  "body": {
   "ok": true,
   "id": 1,
   "item": {
    "id": 1,
    "clientId": 1,
    "framework": "NIST CSF",
    "controlId": "GV",
    "title": "Govern",
    "state": "in_progress",
    "evidenceRef": "",
    "notes": "",
    "createdAt": 1793728758186,
    "updatedAt": 1793728758988
   },
   "summary": [
    {
     "framework": "NIST CSF",
     "total": 6,
     "compliant": 0,
     "inProgress": 1,
     "notStarted": 5,
     "notApplicable": 0,
     "percent": 0
    }
   ]
  }
 },
 "client_security_roadmap_archive": {
  "status": 200,
  "body": {
   "ok": true,
   "id": 1,
   "message": "Roadmap item archived."
  }
 },
 "client_site_profile_create": {
  "status": 200,
  "body": {
   "ok": true,
   "id": 1,
   "message": "Site profile saved.",
   "siteProfile": {
    "id": 1,
    "clientId": 1,
    "siteName": "HQ",
    "addressLabel": "1 Main St",
    "isp": "",
    "circuitType": "",
    "circuitSpeed": "",
    "assets": [],
    "diagrams": [],
    "createdAt": 1793728759884,
    "updatedAt": 1793728759884,
    "assetIds": [],
    "notes": ""
   }
  }
 },
 "client_site_profiles_list": {
  "status": 200,
  "body": {
   "clientId": 1,
   "siteProfiles": [
    {
     "id": 1,
     "clientId": 1,
     "siteName": "HQ",
     "addressLabel": "1 Main St",
     "isp": "",
     "circuitType": "",
     "circuitSpeed": "",
     "assets": [],
     "diagrams": [],
     "createdAt": 1793728759884,
     "updatedAt": 1793728759884,
     "assetIds": [],
     "notes": ""
    }
   ],
   "diagramMaxBytes": 5242880,
   "diagramTypes": [
    "application/pdf",
    "image/gif",
    "image/jpeg",
    "image/png",
    "image/webp"
   ],
   "ok": true
  }
 },
 "client_site_diagram_add": {
  "status": 200,
  "body": {
   "ok": true,
   "id": 1,
   "message": "Diagram attached.",
   "diagram": {
    "id": 1,
    "profileId": 1,
    "fileName": "floor.png",
    "contentType": "image/png",
    "sizeBytes": 70,
    "storedName": "cf-site-49f5bebe382b48988494e5282db30177.png",
    "sha256": "c414cd0e204de974f73753c7e28d7638e7b3691bb8b1a2bab6b25bb7fed7ce77"
   }
  }
 },
 "site_diagram_download": {
  "status": 200,
  "body": {
   "_binary": true,
   "size": 70,
   "sha256": "c414cd0e204de974f73753c7e28d7638e7b3691bb8b1a2bab6b25bb7fed7ce77"
  }
 },
 "site_diagram_delete": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Diagram removed.",
   "id": 1
  }
 },
 "client_site_profile_delete": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Site profile deleted.",
   "id": 1
  }
 },
 "client_hygiene_get": {
  "status": 200,
  "body": {
   "hygiene": {
    "clientId": 1,
    "items": [
     {
      "key": "mfa_enabled",
      "label": "Multi-factor authentication enabled",
      "state": "unknown",
      "note": "",
      "checkedBy": null,
      "checkedAt": null
     },
     {
      "key": "password_manager",
      "label": "Password manager in use",
      "state": "unknown",
      "note": "",
      "checkedBy": null,
      "checkedAt": null
     },
     {
      "key": "unique_passwords_attested",
      "label": "Unique passwords attested (no reuse)",
      "state": "unknown",
      "note": "",
      "checkedBy": null,
      "checkedAt": null
     },
     {
      "key": "backup_codes_stored",
      "label": "Backup / recovery codes stored safely",
      "state": "unknown",
      "note": "",
      "checkedBy": null,
      "checkedAt": null
     },
     {
      "key": "recovery_email_current",
      "label": "Recovery email address current",
      "state": "unknown",
      "note": "",
      "checkedBy": null,
      "checkedAt": null
     },
     {
      "key": "recovery_phone_current",
      "label": "Recovery phone number current",
      "state": "unknown",
      "note": "",
      "checkedBy": null,
      "checkedAt": null
     },
     {
      "key": "admin_accounts_reviewed",
      "label": "Admin accounts reviewed",
      "state": "unknown",
      "note": "",
      "checkedBy": null,
      "checkedAt": null
     },
     {
      "key": "shared_passwords_rotated",
      "label": "Shared passwords rotated when staff changes",
      "state": "unknown",
      "note": "",
      "checkedBy": null,
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
 "client_hygiene_set": {
  "status": 200,
  "body": {
   "ok": true,
   "hygiene": {
    "clientId": 1,
    "items": [
     {
      "key": "mfa_enabled",
      "label": "Multi-factor authentication enabled",
      "state": "yes",
      "note": "verified in rehearsal",
      "checkedBy": 1,
      "checkedAt": 1793728763159
     },
     {
      "key": "password_manager",
      "label": "Password manager in use",
      "state": "unknown",
      "note": "",
      "checkedBy": null,
      "checkedAt": null
     },
     {
      "key": "unique_passwords_attested",
      "label": "Unique passwords attested (no reuse)",
      "state": "unknown",
      "note": "",
      "checkedBy": null,
      "checkedAt": null
     },
     {
      "key": "backup_codes_stored",
      "label": "Backup / recovery codes stored safely",
      "state": "unknown",
      "note": "",
      "checkedBy": null,
      "checkedAt": null
     },
     {
      "key": "recovery_email_current",
      "label": "Recovery email address current",
      "state": "unknown",
      "note": "",
      "checkedBy": null,
      "checkedAt": null
     },
     {
      "key": "recovery_phone_current",
      "label": "Recovery phone number current",
      "state": "unknown",
      "note": "",
      "checkedBy": null,
      "checkedAt": null
     },
     {
      "key": "admin_accounts_reviewed",
      "label": "Admin accounts reviewed",
      "state": "unknown",
      "note": "",
      "checkedBy": null,
      "checkedAt": null
     },
     {
      "key": "shared_passwords_rotated",
      "label": "Shared passwords rotated when staff changes",
      "state": "unknown",
      "note": "",
      "checkedBy": null,
      "checkedAt": null
     }
    ],
    "counts": {
     "yes": 1,
     "no": 0,
     "unknown": 7
    },
    "total": 8
   }
  }
 },
 "client_posture_letter_get": {
  "status": 200,
  "body": {
   "ok": true,
   "letter": {
    "clientId": 1,
    "clientName": "Phase3 Client LLC",
    "month": "2026-11",
    "monthLabel": "November 2026",
    "subject": "Your Sentinel Safeguard security summary \u2014 November 2026",
    "text": "Hello Pat P,\n\nHere is your monthly security summary from Sentinel Safeguard for November 2026.\n\nWhat this letter is: a summary of what is recorded for you in Sentinel Command \u2014 nothing more. It is not a claim that we monitor your systems around the clock, and it does not describe coverage beyond the work recorded here. If something on your record looks wrong or missing, tell us and we will correct it.\n\nYour security score\nYour score for November 2026: 42 out of 100.\n\nThe main things behind that score:\n  - Helping: Recent activity \u2014 Activity in the last 90 days\n  - Hurting: Open tickets \u2014 1 open normal-priority ticket\n\nOpen items on your record\n  - Cases: none open.\n  - Tickets: 1 open \u2014 the oldest has been open for 0 days.\n\nOne next step\nBook a review so we can work through your open tickets together, starting with the oldest one.\n\nBook a time here: https://sentinelsafeguard.net/book\n\nThank you,\nSentinel Safeguard LLC \u2014 Kansas City, Missouri\nBy appointment only.\n",
    "html": "<!doctype html><html><body><p>Hello Pat P,</p><p>Here is your monthly security summary from Sentinel Safeguard for November 2026.</p><p>What this letter is: a summary of what is recorded for you in Sentinel Command \u2014 nothing more. It is not a claim that we monitor your systems around the clock, and it does not describe coverage beyond the work recorded here. If something on your record looks wrong or missing, tell us and we will correct it.</p><h2>Your security score</h2><p>Your score for November 2026: <strong>42 out of 100</strong>.</p><p>The main things behind that score:</p><ul><li>Helping: Recent activity \u2014 Activity in the last 90 days</li><li>Hurting: Open tickets \u2014 1 open normal-priority ticket</li></ul><h2>Open items on your record</h2><ul><li>Cases: none open.</li><li>Tickets: 1 open \u2014 the oldest has been open for 0 days.</li></ul><h2>One next step</h2><p>Book a review so we can work through your open tickets together, starting with the oldest one.</p><p>Book a time here: <a href=\"https://sentinelsafeguard.net/book\">https://sentinelsafeguard.net/book</a></p><p>Thank you,<br>Sentinel Safeguard LLC \u2014 Kansas City, Missouri<br>By appointment only.</p></body></html>",
    "score": {
     "score": 42,
     "factors": [
      {
       "key": "recent_activity",
       "label": "Recent activity",
       "direction": "positive",
       "detail": "Activity in the last 90 days"
      },
      {
       "key": "open_tickets",
       "label": "Open tickets",
       "direction": "negative",
       "detail": "1 open normal-priority ticket"
      }
     ]
    },
    "openItems": {
     "cases": {
      "count": 0,
      "oldestDays": null
     },
     "tickets": {
      "count": 1,
      "oldestDays": 0
     }
    },
    "recommendation": "Book a review so we can work through your open tickets together, starting with the oldest one.",
    "assetRenewals": [],
    "bookingUrl": "https://sentinelsafeguard.net/book"
   }
  }
 },
 "client_posture_letter_send": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Posture letter for November 2026 sent to pat@example.invalid.",
   "id": 1
  }
 },
 "client_health_timeline": {
  "status": 200,
  "body": {
   "clientId": 1,
   "events": [
    {
     "kind": "activity:POSTURE_LETTER_SENT",
     "at": 1793728764355,
     "title": "POSTURE_LETTER_SENT",
     "detail": "Monthly posture letter for November 2026 sent to pat@example.invalid for Phase3 Client LLC.",
     "refType": "client",
     "refId": 1
    },
    {
     "kind": "activity:POSTURE_LETTER_GENERATED",
     "at": 1793728763684,
     "title": "POSTURE_LETTER_GENERATED",
     "detail": "Monthly posture letter for November 2026 generated for Phase3 Client LLC (score 42); nothing was sent.",
     "refType": "client",
     "refId": 1
    },
    {
     "kind": "activity:CF2_HYGIENE_SET",
     "at": 1793728763166,
     "title": "CF2_HYGIENE_SET",
     "detail": "Hygiene 'mfa_enabled' marked yes for Phase3 Client LLC (state only -- no credential value stored).",
     "refType": "client",
     "refId": 1
    },
    {
     "kind": "activity:CF_SITE_PROFILE_DELETED",
     "at": 1793728762337,
     "title": "CF_SITE_PROFILE_DELETED",
     "detail": "Site profile 'HQ' deleted.",
     "refType": "client",
     "refId": 1
    },
    {
     "kind": "activity:CF_SITE_DIAGRAM_ADDED",
     "at": 1793728760653,
     "title": "CF_SITE_DIAGRAM_ADDED",
     "detail": "Diagram 'floor.png' attached to site profile 'HQ'.",
     "refType": "client",
     "refId": 1
    },
    {
     "kind": "activity:CF_SITE_PROFILE_SAVED",
     "at": 1793728759939,
     "title": "CF_SITE_PROFILE_SAVED",
     "detail": "Site profile 'HQ' saved for client #1 (0 asset reference(s)).",
     "refType": "client",
     "refId": 1
    },
    {
     "kind": "activity:CF_ROADMAP_ARCHIVED",
     "at": 1793728759351,
     "title": "CF_ROADMAP_ARCHIVED",
     "detail": "Roadmap item #1 archived (history preserved): Enable MFA everywhere.",
     "refType": "client",
     "refId": 1
    },
    {
     "kind": "activity:CF_COMPLIANCE_SAVED",
     "at": 1793728758988,
     "title": "CF_COMPLIANCE_SAVED",
     "detail": "Compliance item updated for Phase3 Client LLC: NIST CSF GV -> in_progress.",
     "refType": "client",
     "refId": 1
    },
    {
     "kind": "activity:CF_COMPLIANCE_SEEDED",
     "at": 1793728758192,
     "title": "CF_COMPLIANCE_SEEDED",
     "detail": "Compliance tracker seeded for Phase3 Client LLC: NIST CSF (6 new item(s), heading-level template).",
     "refType": "client",
     "refId": 1
    },
    {
     "kind": "activity:CF_BREACH_WATCH_REMOVED",
     "at": 1793728757471,
     "title": "CF_BREACH_WATCH_REMOVED",
     "detail": "Breach watch removed (paused, history kept) for client #1: example.invalid.",
     "refType": "client",
     "refId": 1
    },
    {
     "kind": "activity:CF_BREACH_MATCH_RUN",
     "at": 1793728756958,
     "title": "CF_BREACH_MATCH_RUN",
     "detail": "Breach matcher run: 0 new match(es) across 1 active watch(es).",
     "refType": "client",
     "refId": 1
    },
    {
     "kind": "activity:CF_BREACH_WATCH_SAVED",
     "at": 1793728756062,
     "title": "CF_BREACH_WATCH_SAVED",
     "detail": "Breach watch added for Phase3 Client LLC: example.invalid (active).",
     "refType": "client",
     "refId": 1
    },
    {
     "kind": "activity:CF_RISK_STATE_CHANGED",
     "at": 1793728755683,
     "title": "CF_RISK_STATE_CHANGED",
     "detail": "Risk #1 state changed from open to accepted: Legacy VPN.",
     "refType": "client",
     "refId": 1
    },
    {
     "kind": "activity:CF_RISK_SAVED",
     "at": 1793728755205,
     "title": "CF_RISK_SAVED",
     "detail": "Risk register: risk #1 recorded for Phase3 Client LLC: Legacy VPN (state open, score 9).",
     "refType": "client",
     "refId": 1
    },
    {
     "kind": "activity:CF_ROADMAP_STATE_CHANGED",
     "at": 1793728754466,
     "title": "CF_ROADMAP_STATE_CHANGED",
     "detail": "Roadmap item #1 state changed from planned to in_progress: Enable MFA everywhere.",
     "refType": "client",
     "refId": 1
    },
    {
     "kind": "activity:CF_ROADMAP_GENERATED",
     "at": 1793728753779,
     "title": "CF_ROADMAP_GENERATED",
     "detail": "Roadmap suggestions generated for Phase3 Client LLC from posture inputs: 3 new, 0 already on the roadmap.",
     "refType": "client",
     "refId": 1
    },
    {
     "kind": "roadmap",
     "at": 1793728753769,
     "title": "Roadmap: File core engagement documents",
     "detail": "",
     "refType": "cf_roadmap_items",
     "refId": 4
    },
    {
     "kind": "roadmap",
     "at": 1793728753769,
     "title": "Roadmap: Address: 1 open normal-priority ticket",
     "detail": "",
     "refType": "cf_roadmap_items",
     "refId": 3
    },
    {
     "kind": "roadmap",
     "at": 1793728753769,
     "title": "Roadmap: Build an asset inventory",
     "detail": "",
     "refType": "cf_roadmap_items",
     "refId": 2
    },
    {
     "kind": "activity:CF_ROADMAP_SAVED",
     "at": 1793728752941,
     "title": "CF_ROADMAP_SAVED",
     "detail": "Roadmap item #1 recorded for Phase3 Client LLC: Enable MFA everywhere (state planned).",
     "refType": "client",
     "refId": 1
    },
    {
     "kind": "roadmap",
     "at": 1793728752939,
     "title": "Roadmap: Enable MFA everywhere",
     "detail": "",
     "refType": "cf_roadmap_items",
     "refId": 1
    },
    {
     "kind": "activity:OUTREACH_DRAFTED",
     "at": 1793728752291,
     "title": "OUTREACH_DRAFTED",
     "detail": "Outreach draft (email) for Phase3 Client LLC saved to the client's notes, marked as a draft \u2014 nothing was contacted.",
     "refType": "client",
     "refId": 1
    },
    {
     "kind": "activity:NOTIFICATION_PREFS_UPDATED",
     "at": 1793728751642,
     "title": "NOTIFICATION_PREFS_UPDATED",
     "detail": "Notification preferences updated: ticket_updates, invoice_events, monitoring_alerts, digest.",
     "refType": "client",
     "refId": 1
    },
    {
     "kind": "activity:CLIENT_CONTACT_SAVED",
     "at": 1793728750848,
     "title": "CLIENT_CONTACT_SAVED",
     "detail": "Contact Second Contact Jr (primary) saved for Phase3 Client LLC.",
     "refType": "client",
     "refId": 1
    },
    {
     "kind": "activity:CLIENT_CONTACT_SAVED",
     "at": 1793728748446,
     "title": "CLIENT_CONTACT_SAVED",
     "detail": "Contact Second Contact (billing) saved for Phase3 Client LLC.",
     "refType": "client",
     "refId": 1
    }
   ],
   "nextCursor": 25,
   "total": 29,
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
 "client_offboard_get": {
  "status": 200,
  "body": {
   "offboard": {
    "started": false,
    "clientId": 1,
    "archived": false,
    "retentionDays": null
   },
   "ok": true
  }
 },
 "client_offboard_start": {
  "status": 200,
  "body": {
   "ok": true,
   "resumed": false,
   "offboard": {
    "started": true,
    "id": 1,
    "clientId": 1,
    "status": "in_progress",
    "steps": {
     "export": false,
     "revoke": false,
     "archive": false
    },
    "exportRef": "",
    "exportCounts": {},
    "revokedUsers": 0,
    "retentionDays": null,
    "archived": false,
    "createdAt": 1793728765364,
    "updatedAt": 1793728765364,
    "completedAt": null
   }
  }
 },
 "client_offboard_step": {
  "status": 200,
  "body": {
   "ok": true,
   "alreadyDone": false,
   "step": "export",
   "offboard": {
    "started": true,
    "id": 1,
    "clientId": 1,
    "status": "in_progress",
    "steps": {
     "export": true,
     "revoke": false,
     "archive": false
    },
    "exportRef": "cf-export-client-1-1793728765838",
    "exportCounts": {
     "assets": 0,
     "b3_secrets": 0,
     "b3e_auto_runs": 0,
     "b3e_lead_packets": 0,
     "b3e_review_drafts": 0,
     "b3e_ticket_drafts": 0,
     "b3e_ticket_notes": 0,
     "b4_assessments": 0,
     "b4f_runbook_runs": 0,
     "b4g_audio_notes": 0,
     "b4i_pack_log": 0,
     "b4i_score_events": 0,
     "b4i_score_state": 0,
     "b4j_packet_clients": 0,
     "b4j_packet_items": 0,
     "b4j_warranty_claims": 0,
     "booking_onboarding": 0,
     "cases": 0,
     "cf_asset_import_batches": 0,
     "cf_b3_aging_drafts": 0,
     "cf_backup_tests": 0,
     "cf_bookings": 0,
     "cf_breach_matches": 0,
     "cf_change_requests": 0,
     "cf_checklist_runs": 0,
     "cf_client_contacts": 1,
     "cf_client_digests": 0,
     "cf_client_notes": 1,
     "cf_client_reports": 0,
     "cf_compliance_items": 6,
     "cf_contract_reminders": 0,
     "cf_contracts": 0,
     "cf_doc_packets": 0,
     "cf_domain_watches": 1,
     "cf_emergency_contacts": 0,
     "cf_engagements": 0,
     "cf_file_request_items": 0,
     "cf_file_requests": 0,
     "cf_followups": 0,
     "cf_hygiene_items": 1,
     "cf_job_photos": 0,
     "cf_kb_audience": 0,
     "cf_licenses": 0,
     "cf_locations": 0,
     "cf_maint_windows": 0,
     "cf_messages": 0,
     "cf_notification_prefs": 1,
     "cf_offboards": 1,
     "cf_onboarding_submissions": 0,
     "cf_owner_tasks": 0,
     "cf_payment_plans": 0,
     "cf_plans": 0,
     "cf_playbook_runs": 0,
     "cf_proposals": 0,
     "cf_quote_followups": 0,
     "cf_quotes": 0,
     "cf_reminder_drafts": 0,
     "cf_retainer_entries": 0,
     "cf_retainers": 0,
     "cf_risks": 1,
     "cf_roadmap_items": 4,
     "cf_scans": 0,
     "cf_site_profiles": 0,
     "cf_surveys": 0,
     "cf_threat_briefs": 0,
     "cf_time_entries": 0,
     "clients": 1,
     "form_records": 0,
     "invoice_items": 0,
     "invoices": 0,
     "jobs": 0,
     "osint_reports": 0,
     "osint_searches": 0,
     "osint_watches": 0,
     "review_requests": 0,
     "reviews": 0,
     "tickets": 1,
     "users": 0,
     "verifications": 0
    },
    "revokedUsers": 0,
    "retentionDays": null,
    "archived": false,
    "createdAt": 1793728765364,
    "updatedAt": 1793728765838,
    "completedAt": null
   }
  }
 },
 "client_mail_get": {
  "status": 200,
  "body": {
   "ok": true,
   "configured": false,
   "note": "Mail with this client: threads in the firm's own mailboxes (skyler@ / sentinel@) that involve the client's email addresses. The client's own mailbox is not on this server and is never read. Read-only: nothing here can send, move, delete or flag mail.",
   "threads": [],
   "message": "The mail bridge is not configured on this server yet (no Dovecot master-user credential provisioned)."
  }
 },
 "client_training_get": {
  "status": 200,
  "body": {
   "state": "unlinked",
   "orgId": null,
   "members": [],
   "rollup": null,
   "queryCensus": [],
   "ok": true,
   "clientId": 1
  }
 },
 "client_doc_expiry_get": {
  "status": 200,
  "body": {
   "ok": true,
   "clientId": 1,
   "expiryState": "none",
   "documents": [],
   "expiringDocuments": []
  }
 },
 "client_score_events_get": {
  "status": 200,
  "body": {
   "ok": true,
   "clientId": 1,
   "events": [],
   "note": "Score changes are recorded from the first observation after this feature shipped; earlier changes have no recorded reasons."
  }
 },
 "client_report_generate": {
  "status": 200,
  "body": {
   "ok": true,
   "id": 1,
   "message": "Report draft generated; nothing was sent.",
   "report": {
    "id": 1,
    "clientId": 1,
    "clientName": "Phase3 Client LLC",
    "period": "2026-10",
    "figures": {
     "period": "2026-10",
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
     "ticketsOpened": 0,
     "ticketsClosed": 0,
     "trackedSeconds": 0,
     "trackedHours": 0.0,
     "timeEntries": 0,
     "renewals": [],
     "roadmap": {
      "planned": 3,
      "in_progress": 0,
      "done": 0,
      "accepted_risk": 0,
      "total": 3
     }
    },
    "body": "Monthly report for Phase3 Client LLC \u2014 2026-10\n\nSecurity posture: 42 out of 100.\n\nTickets opened: 0\nTickets closed: 0\n\nTracked time: 0.0 hours (0 seconds across 0 entries)\n\nRenewals in the next 60 days: 0\n\nSecurity roadmap: 3 item(s) \u2014 3 planned, 0 in progress, 0 done, 0 accepted risk.\n\nPrepared by Sentinel Safeguard LLC from the work recorded in Command. Figures are counts of real records; a quiet month reports zeros.\n",
    "state": "draft",
    "generatedBy": 1,
    "generatedAt": 1793728768066,
    "sentAt": null,
    "sentBy": null,
    "createdAt": 1793728768066,
    "updatedAt": 1793728768066
   }
  }
 },
 "client_reports_list": {
  "status": 200,
  "body": {
   "ok": true,
   "reports": [
    {
     "id": 1,
     "clientId": 1,
     "clientName": "Phase3 Client LLC",
     "period": "2026-10",
     "figures": {
      "period": "2026-10",
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
      "ticketsOpened": 0,
      "ticketsClosed": 0,
      "trackedSeconds": 0,
      "trackedHours": 0.0,
      "timeEntries": 0,
      "renewals": [],
      "roadmap": {
       "planned": 3,
       "in_progress": 0,
       "done": 0,
       "accepted_risk": 0,
       "total": 3
      }
     },
     "body": "Monthly report for Phase3 Client LLC \u2014 2026-10\n\nSecurity posture: 42 out of 100.\n\nTickets opened: 0\nTickets closed: 0\n\nTracked time: 0.0 hours (0 seconds across 0 entries)\n\nRenewals in the next 60 days: 0\n\nSecurity roadmap: 3 item(s) \u2014 3 planned, 0 in progress, 0 done, 0 accepted risk.\n\nPrepared by Sentinel Safeguard LLC from the work recorded in Command. Figures are counts of real records; a quiet month reports zeros.\n",
     "state": "draft",
     "generatedBy": 1,
     "generatedAt": 1793728768066,
     "sentAt": null,
     "sentBy": null,
     "createdAt": 1793728768066,
     "updatedAt": 1793728768066
    }
   ],
   "surveySummary": {
    "count": 0,
    "avgRating": null,
    "recommendPct": null
   }
  }
 },
 "client_report_send": {
  "status": 200,
  "body": {
   "ok": true,
   "id": 1,
   "message": "Report sent to pat@example.invalid.",
   "report": {
    "id": 1,
    "clientId": 1,
    "clientName": "Phase3 Client LLC",
    "period": "2026-10",
    "figures": {
     "period": "2026-10",
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
     "ticketsOpened": 0,
     "ticketsClosed": 0,
     "trackedSeconds": 0,
     "trackedHours": 0.0,
     "timeEntries": 0,
     "renewals": [],
     "roadmap": {
      "planned": 3,
      "in_progress": 0,
      "done": 0,
      "accepted_risk": 0,
      "total": 3
     }
    },
    "body": "Monthly report for Phase3 Client LLC \u2014 2026-10\n\nSecurity posture: 42 out of 100.\n\nTickets opened: 0\nTickets closed: 0\n\nTracked time: 0.0 hours (0 seconds across 0 entries)\n\nRenewals in the next 60 days: 0\n\nSecurity roadmap: 3 item(s) \u2014 3 planned, 0 in progress, 0 done, 0 accepted risk.\n\nPrepared by Sentinel Safeguard LLC from the work recorded in Command. Figures are counts of real records; a quiet month reports zeros.\n",
    "state": "sent",
    "generatedBy": 1,
    "generatedAt": 1793728768066,
    "sentAt": 1793728769151,
    "sentBy": 1,
    "createdAt": 1793728768066,
    "updatedAt": 1793728769151
   }
  }
 },
 "firm_pulse_get": {
  "status": 200,
  "body": {
   "ok": true,
   "generatedAt": 1793728769760,
   "revenueSource": "cf_invoice_payments",
   "revenueByMonth": [],
   "revenueByService": [],
   "aging": [
    {
     "bucket": "current",
     "totalCents": 0,
     "invoiceIds": []
    },
    {
     "bucket": "1-30",
     "totalCents": 0,
     "invoiceIds": []
    },
    {
     "bucket": "31-60",
     "totalCents": 0,
     "invoiceIds": []
    },
    {
     "bucket": "61-90",
     "totalCents": 0,
     "invoiceIds": []
    },
    {
     "bucket": "90+",
     "totalCents": 0,
     "invoiceIds": []
    }
   ],
   "pipeline": {
    "totalCents": 0,
    "quoteIds": [],
    "note": "Open quotes from cf_quotes."
   },
   "planMrr": {
    "totalCents": 0,
    "planIds": [],
    "note": "Active plans from cf_plans."
   },
   "ticketThroughput": [
    {
     "weekStart": "2026-09-14",
     "opened": 0,
     "closed": 0,
     "openedIds": [],
     "closedIds": []
    },
    {
     "weekStart": "2026-09-21",
     "opened": 0,
     "closed": 0,
     "openedIds": [],
     "closedIds": []
    },
    {
     "weekStart": "2026-09-28",
     "opened": 0,
     "closed": 0,
     "openedIds": [],
     "closedIds": []
    },
    {
     "weekStart": "2026-10-05",
     "opened": 0,
     "closed": 0,
     "openedIds": [],
     "closedIds": []
    },
    {
     "weekStart": "2026-10-12",
     "opened": 0,
     "closed": 0,
     "openedIds": [],
     "closedIds": []
    },
    {
     "weekStart": "2026-10-19",
     "opened": 0,
     "closed": 0,
     "openedIds": [],
     "closedIds": []
    },
    {
     "weekStart": "2026-10-26",
     "opened": 0,
     "closed": 0,
     "openedIds": [],
     "closedIds": []
    },
    {
     "weekStart": "2026-11-02",
     "opened": 1,
     "closed": 0,
     "openedIds": [
      1
     ],
     "closedIds": []
    }
   ],
   "postureDistribution": [
    {
     "band": "90-100",
     "count": 0,
     "clientIds": []
    },
    {
     "band": "70-89",
     "count": 0,
     "clientIds": []
    },
    {
     "band": "40-69",
     "count": 1,
     "clientIds": [
      1
     ]
    },
    {
     "band": "0-39",
     "count": 0,
     "clientIds": []
    },
    {
     "band": "insufficient",
     "count": 0,
     "clientIds": []
    }
   ],
   "profitByClient": [],
   "profitByService": [],
   "profitBasis": "Revenue: Paid invoices at full total (Firm Pulse's revenue-by-service basis). Costs: tracked time at its stored rate + expenses + consumed parts, per #69 job costing. Family clients excluded entirely; Draft invoices never count; an unrated time entry makes its cut's profit read as not computed, never ${'$'}0.",
   "retainerMrr": {
    "totalCents": 0,
    "planIds": [],
    "note": "Active recurring plans from cf_plans, each price normalized to monthly (quarterly /3, annual /12, custom scaled by 30.4375 days per month). The retainer hours bank (#53) stores hours, not money, so it contributes no MRR.",
    "basis": "monthly-normalized"
   }
  }
 },
 "firm_pulse_drilldown": {
  "status": 200,
  "body": {
   "ok": true,
   "figure": "aging",
   "key": "current",
   "totalCents": 0,
   "records": []
  }
 },
 "client_digests_generate": {
  "status": 200,
  "body": {
   "ok": true,
   "weekStart": "2026-11-02",
   "generated": [],
   "skipped": [
    {
     "clientId": 1,
     "clientName": "Phase3 Client LLC",
     "reason": "client notification preference: weekly digest off"
    }
   ],
   "undeliverable": [],
   "message": "0 draft digest(s) generated; nothing was sent."
  }
 },
 "client_digests_list": {
  "status": 200,
  "body": {
   "digests": [],
   "ok": true
  }
 },
 "client_duplicate_count": {
  "status": 200,
  "body": {
   "ok": true,
   "count": 1,
   "asOf": "2026-11-03T17:59:31+00:00"
  }
 },
 "client_duplicates_list": {
  "status": 200,
  "body": {
   "ok": true,
   "pairs": [
    {
     "aId": 2,
     "bId": 3,
     "reasons": [
      "phone",
      "email"
     ],
     "nameSimilarity": null,
     "a": {
      "id": 2,
      "legalName": "Dup Alpha LLC",
      "dba": "",
      "type": "Corporate",
      "industry": "Testing",
      "status": "Active",
      "stage": "Lead",
      "value": 1000.0,
      "nextFollowUp": null,
      "source": "",
      "sourceDetail": "",
      "contactName": "Pat P",
      "contactTitle": "",
      "email": "dup@example.invalid",
      "phone": "555-7777",
      "street": "",
      "city": "",
      "state": "",
      "zip": "",
      "notes": "",
      "tags": "",
      "friends": false,
      "importBatchId": null,
      "archivedAt": null,
      "archivedBy": null,
      "createdAt": 1793728770752,
      "updatedAt": 1793728770752
     },
     "b": {
      "id": 3,
      "legalName": "Dup Beta LLC",
      "dba": "",
      "type": "Corporate",
      "industry": "Testing",
      "status": "Active",
      "stage": "Lead",
      "value": 1000.0,
      "nextFollowUp": null,
      "source": "",
      "sourceDetail": "",
      "contactName": "Pat P",
      "contactTitle": "",
      "email": "dup@example.invalid",
      "phone": "555-7777",
      "street": "",
      "city": "",
      "state": "",
      "zip": "",
      "notes": "",
      "tags": "",
      "friends": false,
      "importBatchId": null,
      "archivedAt": null,
      "archivedBy": null,
      "createdAt": 1793728770988,
      "updatedAt": 1793728770988
     },
     "completeness": {
      "a": 9,
      "b": 9
     },
     "suggestedWinnerId": 2
    }
   ],
   "count": 1
  }
 },
 "client_merge": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Merged Dup Beta LLC into Dup Alpha LLC; the duplicate record was archived.",
   "id": 2,
   "winnerId": 2,
   "loserId": 3,
   "repointed": {
    "cases": 0,
    "tickets": 0,
    "assets": 0,
    "invoices": 0,
    "form_records": 0,
    "jobs": 0,
    "users": 0
   }
  }
 },
 "verification_issue": {
  "status": 201,
  "body": {
   "ok": true,
   "message": "Verification issued.",
   "verification": {
    "id": 1,
    "clientId": 1,
    "businessName": "Phase3 Client LLC",
    "code": "SV-YYJ6ZU-DAS6L7",
    "category": "Security Assessment",
    "issuedDate": "2026-11-03",
    "expiryDate": "2027-11-03",
    "status": "active",
    "storedStatus": "active",
    "revokedAt": null,
    "createdAt": 1793728772805,
    "assessmentId": null,
    "score": null,
    "revokedReason": "",
    "badgeSnippet": "<a href=\"https://sentinelsafeguard.net/verified?code=SV-YYJ6ZU-DAS6L7\" title=\"Sentinel Verified 2026\"><img src=\"https://sentinelsafeguard.net/static/verified-badge.svg\" alt=\"Sentinel Verified 2026\" width=\"220\" height=\"56\"></a>"
   }
  }
 },
 "verifications_list": {
  "status": 200,
  "body": {
   "ok": true,
   "verifications": [
    {
     "id": 1,
     "clientId": 1,
     "businessName": "Phase3 Client LLC",
     "code": "SV-YYJ6ZU-DAS6L7",
     "category": "Security Assessment",
     "issuedDate": "2026-11-03",
     "expiryDate": "2027-11-03",
     "status": "active",
     "storedStatus": "active",
     "revokedAt": null,
     "createdAt": 1793728772805,
     "assessmentId": null,
     "score": null,
     "revokedReason": "",
     "badgeSnippet": "<a href=\"https://sentinelsafeguard.net/verified?code=SV-YYJ6ZU-DAS6L7\" title=\"Sentinel Verified 2026\"><img src=\"https://sentinelsafeguard.net/static/verified-badge.svg\" alt=\"Sentinel Verified 2026\" width=\"220\" height=\"56\"></a>"
    }
   ],
   "categories": [
    "Security Assessment",
    "Penetration Testing",
    "Security Installs",
    "Incident Response",
    "Cybersecurity Consulting",
    "IT Services"
   ]
  }
 },
 "verification_revoke_refusal": {
  "status": 403,
  "body": {
   "ok": false,
   "authorized": false,
   "message": "Revocation requires confirmation \u2014 it is permanent."
  }
 },
 "verification_revoke": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Verification revoked permanently.",
   "verification": {
    "id": 1,
    "clientId": 1,
    "businessName": "Phase3 Client LLC",
    "code": "SV-YYJ6ZU-DAS6L7",
    "category": "Security Assessment",
    "issuedDate": "2026-11-03",
    "expiryDate": "2027-11-03",
    "status": "revoked",
    "storedStatus": "revoked",
    "revokedAt": 1793728773660,
    "createdAt": 1793728772805,
    "assessmentId": null,
    "score": null,
    "revokedReason": "",
    "badgeSnippet": "<a href=\"https://sentinelsafeguard.net/verified?code=SV-YYJ6ZU-DAS6L7\" title=\"Sentinel Verified 2026\"><img src=\"https://sentinelsafeguard.net/static/verified-badge.svg\" alt=\"Sentinel Verified 2026\" width=\"220\" height=\"56\"></a>"
   }
  }
 },
 "verifications_export": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Exported 1 verification(s).",
   "count": 1,
   "generatedAt": "2026-11-03",
   "export": {
    "schema": 1,
    "generated_at": "2026-11-03",
    "verifications": [
     {
      "code": "SV-YYJ6ZU-DAS6L7",
      "business_name": "Phase3 Client LLC",
      "category": "Security Assessment",
      "issued": "2026-11-03",
      "expiry": "2027-11-03",
      "status": "revoked"
     }
    ]
   }
  }
 },
 "client_filter_preset_create": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Filter preset 'P3 preset' saved.",
   "id": 1
  }
 },
 "client_filter_presets_list": {
  "status": 200,
  "body": {
   "presets": [
    {
     "id": 1,
     "name": "P3 preset",
     "spec": {
      "query": "",
      "stage": "Lead",
      "tier": "All",
      "sort": "name"
     },
     "updatedAt": 1793728774349
    }
   ],
   "ok": true
  }
 },
 "client_filter_preset_delete": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Filter preset deleted.",
   "id": 1
  }
 },
 "saved_view_create": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "View saved.",
   "id": 1
  }
 },
 "saved_views_list": {
  "status": 200,
  "body": {
   "views": [
    {
     "id": 1,
     "tab": "clients",
     "name": "P3 view",
     "criteria": {
      "stage": "Lead"
     },
     "builtin": false,
     "createdAt": 1793728775738,
     "updatedAt": 1793728775738
    }
   ],
   "builtins": [
    {
     "key": "unpaid30",
     "tab": "invoices",
     "name": "Unpaid 30+",
     "builtin": true,
     "count": 0,
     "criteria": {
      "builtin": "unpaid30"
     },
     "note": ""
    },
    {
     "key": "expiring60",
     "tab": "assets",
     "name": "Expiring 60d",
     "builtin": true,
     "count": 0,
     "criteria": {
      "builtin": "expiring60"
     },
     "note": "Date fields checked: warranty_until (+ review_due_date fallback)."
    },
    {
     "key": "breachedsla",
     "tab": "tickets",
     "name": "Breached SLA",
     "builtin": true,
     "count": 0,
     "criteria": {
      "builtin": "breachedsla"
     },
     "note": ""
    },
    {
     "key": "awaitingclient",
     "tab": "tickets",
     "name": "Awaiting client",
     "builtin": true,
     "count": 0,
     "criteria": {
      "builtin": "awaitingclient"
     },
     "note": "Maps to status 'On Hold' \u2014 the real ticket vocabulary has no dedicated awaiting-client status."
    }
   ],
   "tabs": [
    "clients",
    "cases",
    "tickets",
    "invoices",
    "assets"
   ],
   "ok": true
  }
 },
 "saved_view_apply": {
  "status": 200,
  "body": {
   "tab": "clients",
   "viewId": 1,
   "name": "P3 view",
   "criteria": {
    "stage": "Lead"
   },
   "rows": [
    {
     "id": 1,
     "legalName": "Phase3 Client LLC",
     "dba": "",
     "type": "Corporate",
     "industry": "Testing",
     "status": "Active",
     "stage": "Lead",
     "value": 1000.0,
     "nextFollowUp": null,
     "source": "",
     "sourceDetail": "",
     "contactName": "Pat P",
     "contactTitle": "",
     "email": "pat@example.invalid",
     "phone": "555-0101",
     "street": "",
     "city": "",
     "state": "",
     "zip": "",
     "notes": "[DRAFT \u2014 not sent] 2026-11-03 (email)\nHello from Sentinel.",
     "tags": "",
     "friends": false,
     "importBatchId": null,
     "archivedAt": null,
     "archivedBy": null,
     "createdAt": 1793728745365,
     "updatedAt": 1793728752290,
     "familyLinked": false
    },
    {
     "id": 2,
     "legalName": "Dup Alpha LLC",
     "dba": "",
     "type": "Corporate",
     "industry": "Testing",
     "status": "Active",
     "stage": "Lead",
     "value": 1000.0,
     "nextFollowUp": null,
     "source": "",
     "sourceDetail": "",
     "contactName": "Pat P",
     "contactTitle": "",
     "email": "dup@example.invalid",
     "phone": "555-7777",
     "street": "",
     "city": "",
     "state": "",
     "zip": "",
     "notes": "Merged from Dup Beta LLC (record #3) on 2026-11-03:\nlegalName: Dup Beta LLC",
     "tags": "",
     "friends": false,
     "importBatchId": null,
     "archivedAt": null,
     "archivedBy": null,
     "createdAt": 1793728770752,
     "updatedAt": 1793728772139,
     "familyLinked": false
    }
   ],
   "count": 2,
   "ok": true
  }
 },
 "saved_view_delete": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "View deleted.",
   "id": 1
  }
 },
 "clients_bulk": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Bulk stage: 1 updated, 0 unchanged, 0 not found.",
   "updated": [
    4
   ],
   "unchanged": [],
   "missing": []
  }
 },
 "ticket_ops_get": {
  "status": 200,
  "body": {
   "ticket": {
    "id": 1,
    "clientId": 1,
    "assignedUserId": null,
    "title": "Phase3 ticket",
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
    "createdAt": 1793728745741,
    "updatedAt": 1793728745741,
    "caseId": null,
    "source": "Internal",
    "sourceRef": null,
    "mergedIntoId": null
   },
   "ops": {
    "ticketId": 1,
    "merged": false,
    "mergedIntoId": null,
    "sla": {
     "priority": "Normal",
     "responseTargetMin": 480,
     "resolutionTargetMin": 4320,
     "firstResponseAt": null,
     "responseDueAt": 1793757545741,
     "resolutionDueAt": 1793987945741,
     "resolvedAt": null,
     "responseState": "on-track",
     "resolutionState": "on-track",
     "breaches": []
    },
    "photos": [],
    "followUps": [],
    "merges": [],
    "expenses": [],
    "expensesTotalCents": 0
   },
   "ok": true
  }
 },
 "ticket_followups_list": {
  "status": 200,
  "body": {
   "followUps": [
    {
     "id": 1,
     "ticketId": 1,
     "clientId": 1,
     "kind": "day30",
     "dueDate": "2026-12-03",
     "state": "open",
     "draftText": "Hi Pat P \u2014 checking in 30 days after we closed 'Phase3 ticket'. Is everything still working the way you need? If anything has come up, reply here and we will take care of it. \u2014 Sentinel Safeguard",
     "closedAt": 1793728778054,
     "createdAt": 1793728778054,
     "updatedAt": 1793728778054
    },
    {
     "id": 2,
     "ticketId": 1,
     "clientId": 1,
     "kind": "day90",
     "dueDate": "2027-02-01",
     "state": "open",
     "draftText": "Hi Pat P \u2014 checking in 90 days after we closed 'Phase3 ticket'. Is everything still working the way you need? If anything has come up, reply here and we will take care of it. \u2014 Sentinel Safeguard",
     "closedAt": 1793728778054,
     "createdAt": 1793728778055,
     "updatedAt": 1793728778055
    }
   ],
   "ok": true
  }
 },
 "ticket_followup_state": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Follow-up marked done.",
   "id": 1
  }
 },
 "expense_create": {
  "status": 200,
  "body": {
   "ok": true,
   "id": 1,
   "message": "Expense logged.",
   "expense": {
    "id": 1,
    "ticketId": 1,
    "caseId": null,
    "category": "mileage",
    "description": "Drive to site",
    "amountCents": 670,
    "miles": 10.0,
    "rateCentsPerMile": 67,
    "receiptNote": "",
    "spentOn": "2026-11-01",
    "createdBy": 1,
    "createdAt": 1793728778941
   }
  }
 },
 "expenses_list": {
  "status": 200,
  "body": {
   "expenses": [
    {
     "id": 1,
     "ticketId": 1,
     "caseId": null,
     "category": "mileage",
     "description": "Drive to site",
     "amountCents": 670,
     "miles": 10.0,
     "rateCentsPerMile": 67,
     "receiptNote": "",
     "spentOn": "2026-11-01",
     "createdBy": 1,
     "createdAt": 1793728778941
    }
   ],
   "categories": [
    "parts",
    "mileage",
    "software",
    "subcontractor",
    "other"
   ],
   "rollup": {
    "totalCents": 670,
    "byMonth": {
     "2026-11": 670
    },
    "byCategory": {
     "mileage": 670
    },
    "byJob": [
     {
      "kind": "ticket",
      "id": 1,
      "totalCents": 670,
      "count": 1
     }
    ]
   },
   "ok": true
  }
 },
 "expense_delete": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Expense deleted.",
   "id": 1
  }
 },
 "sla_config_get": {
  "status": 200,
  "body": {
   "config": {
    "Critical": {
     "responseTargetMin": 60,
     "resolutionTargetMin": 480
    },
    "High": {
     "responseTargetMin": 240,
     "resolutionTargetMin": 1440
    },
    "Normal": {
     "responseTargetMin": 480,
     "resolutionTargetMin": 4320
    },
    "Low": {
     "responseTargetMin": 1440,
     "resolutionTargetMin": 10080
    }
   },
   "defaults": {
    "Critical": {
     "responseTargetMin": 60,
     "resolutionTargetMin": 480
    },
    "High": {
     "responseTargetMin": 240,
     "resolutionTargetMin": 1440
    },
    "Normal": {
     "responseTargetMin": 480,
     "resolutionTargetMin": 4320
    },
    "Low": {
     "responseTargetMin": 1440,
     "resolutionTargetMin": 10080
    }
   },
   "ok": true
  }
 },
 "sla_config_save": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "SLA targets saved.",
   "config": {
    "Critical": {
     "responseTargetMin": 60,
     "resolutionTargetMin": 480
    },
    "High": {
     "responseTargetMin": 30,
     "resolutionTargetMin": 240
    },
    "Normal": {
     "responseTargetMin": 480,
     "resolutionTargetMin": 4320
    },
    "Low": {
     "responseTargetMin": 1440,
     "resolutionTargetMin": 10080
    }
   }
  }
 },
 "photo_upload": {
  "status": 200,
  "body": {
   "ok": true,
   "id": 1,
   "message": "Photo stored."
  }
 },
 "ticket_photos_list": {
  "status": 200,
  "body": {
   "photos": [
    {
     "id": 1,
     "ticketId": 1,
     "caseId": null,
     "clientId": 1,
     "kind": "other",
     "caption": "rack",
     "originalFilename": "site.png",
     "contentType": "image/png",
     "sizeBytes": 70,
     "uploadedBy": 1,
     "createdAt": 1793728779957,
     "downloadUrl": "/photos/download?id=1"
    }
   ],
   "ok": true
  }
 },
 "photo_download": {
  "status": 200,
  "body": {
   "_binary": true,
   "size": 70,
   "sha256": "c414cd0e204de974f73753c7e28d7638e7b3691bb8b1a2bab6b25bb7fed7ce77"
  }
 },
 "photo_delete": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Photo deleted.",
   "id": 1
  }
 },
 "audio_upload": {
  "status": 200,
  "body": {
   "ok": true,
   "id": 1,
   "message": "Voice note stored."
  }
 },
 "ticket_audio_list": {
  "status": 200,
  "body": {
   "audioNotes": [
    {
     "id": 1,
     "ticketId": 1,
     "caseId": null,
     "clientId": 1,
     "caption": "summary",
     "originalFilename": "note.wav",
     "contentType": "audio/wav",
     "sizeBytes": 44,
     "uploadedBy": 1,
     "createdAt": 1793728780808,
     "downloadUrl": "/audio/download?id=1"
    }
   ],
   "ok": true
  }
 },
 "audio_download": {
  "status": 200,
  "body": {
   "_binary": true,
   "size": 44,
   "sha256": "4f8734c5e13ac599e168cf247a51c1dd0758537ce00bf16d7fed1a3d14d07041"
  }
 },
 "canned_response_create": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Canned response saved.",
   "id": 1,
   "response": {
    "id": 1,
    "title": "P3 greeting",
    "category": "General",
    "body": "Hello {client_name}, ticket #{ticket_id}.",
    "createdAt": 1793728781252,
    "updatedAt": 1793728781252
   }
  }
 },
 "canned_responses_list": {
  "status": 200,
  "body": {
   "responses": [
    {
     "id": 1,
     "title": "P3 greeting",
     "category": "General",
     "body": "Hello {client_name}, ticket #{ticket_id}.",
     "createdAt": 1793728781252,
     "updatedAt": 1793728781252
    }
   ],
   "ok": true
  }
 },
 "canned_response_render": {
  "status": 200,
  "body": {
   "id": 1,
   "text": "Hello Phase3 Client LLC, ticket #1.",
   "ok": true
  }
 },
 "canned_response_delete": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Canned response deleted.",
   "id": 1
  }
 },
 "time_entry_start": {
  "status": 200,
  "body": {
   "ok": true,
   "entry": {
    "id": 1,
    "userId": 1,
    "clientId": 1,
    "ticketId": 1,
    "caseId": null,
    "description": "Diagnosis",
    "startedAt": 1793728782055,
    "endedAt": null,
    "durationSeconds": 0,
    "running": true,
    "billable": true,
    "rateCents": null,
    "rateSource": "manual",
    "billed": false,
    "invoiceId": null,
    "createdAt": 1793728782055,
    "updatedAt": 1793728782055
   },
   "autoStoppedId": null
  }
 },
 "time_entry_stop": {
  "status": 200,
  "body": {
   "ok": true,
   "entry": {
    "id": 1,
    "userId": 1,
    "clientId": 1,
    "ticketId": 1,
    "caseId": null,
    "description": "Diagnosis",
    "startedAt": 1793728782055,
    "endedAt": 1793728782364,
    "durationSeconds": 0,
    "running": false,
    "billable": true,
    "rateCents": null,
    "rateSource": "manual",
    "billed": false,
    "invoiceId": null,
    "createdAt": 1793728782055,
    "updatedAt": 1793728782364
   },
   "durationSeconds": 0
  }
 },
 "time_entry_create": {
  "status": 200,
  "body": {
   "ok": true,
   "entry": {
    "id": 2,
    "userId": 1,
    "clientId": 1,
    "ticketId": 1,
    "caseId": null,
    "description": "Manual work",
    "startedAt": 1793726082555,
    "endedAt": 1793728782555,
    "durationSeconds": 2700,
    "running": false,
    "billable": true,
    "rateCents": 12000,
    "rateSource": "manual",
    "billed": false,
    "invoiceId": null,
    "createdAt": 1793728782555,
    "updatedAt": 1793728782555
   }
  }
 },
 "time_entries_list": {
  "status": 200,
  "body": {
   "entries": [
    {
     "id": 1,
     "userId": 1,
     "clientId": 1,
     "ticketId": 1,
     "caseId": null,
     "description": "Diagnosis",
     "startedAt": 1793728782055,
     "endedAt": 1793728782364,
     "durationSeconds": 0,
     "running": false,
     "billable": true,
     "rateCents": null,
     "rateSource": "manual",
     "billed": false,
     "invoiceId": null,
     "createdAt": 1793728782055,
     "updatedAt": 1793728782364
    },
    {
     "id": 2,
     "userId": 1,
     "clientId": 1,
     "ticketId": 1,
     "caseId": null,
     "description": "Manual work",
     "startedAt": 1793726082555,
     "endedAt": 1793728782555,
     "durationSeconds": 2700,
     "running": false,
     "billable": true,
     "rateCents": 12000,
     "rateSource": "manual",
     "billed": false,
     "invoiceId": null,
     "createdAt": 1793728782555,
     "updatedAt": 1793728782555
    }
   ],
   "running": null,
   "ok": true
  }
 },
 "time_entries_convert": {
  "status": 200,
  "body": {
   "ok": true,
   "invoiceId": 1,
   "invoice": {
    "id": 1,
    "invoiceNumber": "INV-0001",
    "clientId": 1,
    "status": "Draft",
    "issueDate": "2026-11-03",
    "dueDate": "",
    "paidDate": "",
    "recipientEmail": "",
    "notes": "Created from tracked time.",
    "taxRatePercent": 0.0,
    "items": [
     {
      "description": "Manual work (0h 45m)",
      "quantity": 0.75,
      "unitRateCents": 12000,
      "lineTotalCents": 9000
     }
    ],
    "subtotalCents": 9000,
    "taxCents": 0,
    "totalCents": 9000,
    "overdue": false,
    "createdAt": 1793728782984,
    "updatedAt": 1793728782984,
    "discountCents": 0,
    "paidCents": 0,
    "balanceDueCents": 9000,
    "paymentEnabled": false,
    "paymentMode": "",
    "paymentLinkUrl": null,
    "paymentFamilyBlocked": false
   },
   "entryIds": [
    2
   ],
   "message": "Draft invoice INV-0001 created from 1 time entries."
  }
 },
 "time_entry_delete": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Time entry deleted.",
   "id": 1
  }
 },
 "plan_create": {
  "status": 200,
  "body": {
   "ok": true,
   "plan": {
    "id": 1,
    "clientId": 1,
    "clientName": "Phase3 Client LLC",
    "name": "Monthly care",
    "cadence": "monthly",
    "customDays": null,
    "services": "",
    "priceCents": 0,
    "startDate": "2026-11-03",
    "nextDueDate": "2026-11-03",
    "active": true,
    "createdAt": 1793728783638,
    "updatedAt": 1793728783638
   }
  }
 },
 "plans_list": {
  "status": 200,
  "body": {
   "plans": [
    {
     "id": 1,
     "clientId": 1,
     "clientName": "Phase3 Client LLC",
     "name": "Monthly care",
     "cadence": "monthly",
     "customDays": null,
     "services": "",
     "priceCents": 0,
     "startDate": "2026-11-03",
     "nextDueDate": "2026-11-03",
     "active": true,
     "createdAt": 1793728783638,
     "updatedAt": 1793728783638,
     "periods": []
    }
   ],
   "cadences": [
    "monthly",
    "quarterly",
    "annual",
    "custom"
   ],
   "ok": true
  }
 },
 "plan_delete": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Plan deleted.",
   "id": 1
  }
 },
 "playbook_create": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Playbook saved.",
   "id": 1
  }
 },
 "playbooks_list": {
  "status": 200,
  "body": {
   "playbooks": [
    {
     "id": 1,
     "name": "P3 playbook",
     "serviceLabel": "",
     "ticketTitle": "Onboard workstation",
     "ticketCategory": "Install",
     "ticketPriority": "Normal",
     "checklistTemplateId": null,
     "checklistTemplateName": "",
     "siteName": "",
     "notes": "",
     "active": true,
     "createdAt": 1793728784291,
     "updatedAt": 1793728784291
    }
   ],
   "runs": [],
   "ok": true
  }
 },
 "playbook_apply": {
  "status": 200,
  "body": {
   "ok": true,
   "id": 1,
   "message": "Playbook applied \u2014 ticket, checklist, and site survey spawned and linked.",
   "ticketId": 2,
   "checklistRunId": null,
   "siteProfileId": null,
   "clientId": 1
  }
 },
 "checklist_template_create": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Checklist template saved.",
   "id": 1
  }
 },
 "checklists_list": {
  "status": 200,
  "body": {
   "templates": [
    {
     "id": 1,
     "name": "P3 checklist",
     "items": [
      {
       "kind": "check",
       "label": "Step one",
       "required": true
      },
      {
       "kind": "check",
       "label": "Step two",
       "required": true
      }
     ],
     "active": true,
     "createdAt": 1793728785336,
     "updatedAt": 1793728785336
    }
   ],
   "runs": [],
   "ok": true
  }
 },
 "checklist_run_start": {
  "status": 201,
  "body": {
   "ok": true,
   "message": "Checklist run started.",
   "id": 1
  }
 },
 "checklist_run_item": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Checklist item updated.",
   "id": 1
  }
 },
 "checklist_run_item2": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Checklist item updated.",
   "id": 1
  }
 },
 "checklist_run_signoff": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Run signed off by Pat P.",
   "id": 1,
   "status": "completed_signed",
   "summaryHash": "0505b73c8adb51ea2d25401131b5fd3a6c750b3e45bf2d588f0afdf55e6f888e"
  }
 },
 "checklist_run_get": {
  "status": 200,
  "body": {
   "run": {
    "id": 1,
    "templateId": 1,
    "templateName": "P3 checklist",
    "ticketId": 1,
    "caseId": null,
    "clientId": 1,
    "status": "completed_signed",
    "storedStatus": "completed_signed",
    "signed": true,
    "unsigned": false,
    "unsignedFlag": "",
    "signoffName": "Pat P",
    "signoffAt": 1793728786837,
    "summaryHash": "0505b73c8adb51ea2d25401131b5fd3a6c750b3e45bf2d588f0afdf55e6f888e",
    "hashValid": true,
    "items": [
     {
      "kind": "check",
      "label": "Step one",
      "required": true,
      "done": true,
      "note": "",
      "photo": null,
      "completedBy": 1,
      "completedAt": 1793728786086
     },
     {
      "kind": "check",
      "label": "Step two",
      "required": true,
      "done": true,
      "note": "",
      "photo": null,
      "completedBy": 1,
      "completedAt": 1793728786438
     }
    ],
    "startedBy": 1,
    "completedAt": 1793728786837,
    "createdAt": 1793728785737,
    "updatedAt": 1793728786837
   },
   "ok": true
  }
 },
 "part_create": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Part saved.",
   "id": 1
  }
 },
 "part_consume": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Part consumed.",
   "partId": 1,
   "qtyOnHand": 3
  }
 },
 "part_restock": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Part restocked.",
   "partId": 1,
   "qtyOnHand": 7,
   "expenseId": null
  }
 },
 "parts_list": {
  "status": 200,
  "body": {
   "parts": [
    {
     "id": 1,
     "name": "P3 Widget",
     "sku": "W-1",
     "qtyOnHand": 7,
     "unitCostCents": 250,
     "lowStockThreshold": 1,
     "lowStock": false,
     "createdBy": 1,
     "createdAt": 1793728787241,
     "updatedAt": 1793728787801
    }
   ],
   "lowStockCount": 0,
   "movements": [
    {
     "id": 3,
     "partId": 1,
     "ticketId": null,
     "kind": "restock",
     "qty": 4,
     "unitCostCents": 250,
     "expenseId": null,
     "note": "",
     "createdBy": 1,
     "createdAt": 1793728787801
    },
    {
     "id": 2,
     "partId": 1,
     "ticketId": 1,
     "kind": "consume",
     "qty": 2,
     "unitCostCents": 250,
     "expenseId": null,
     "note": "",
     "createdBy": 1,
     "createdAt": 1793728787538
    },
    {
     "id": 1,
     "partId": 1,
     "ticketId": null,
     "kind": "restock",
     "qty": 5,
     "unitCostCents": 250,
     "expenseId": null,
     "note": "Opening stock",
     "createdBy": 1,
     "createdAt": 1793728787241
    }
   ],
   "ok": true
  }
 },
 "ticket_merge_refusal": {
  "status": 403,
  "body": {
   "ok": false,
   "authorized": false,
   "message": "Merging is permanent and cannot be undone. Re-send with confirm set to confirm the merge."
  }
 },
 "ticket_merge": {
  "status": 200,
  "body": {
   "ok": true,
   "id": 1,
   "survivorId": 3,
   "duplicateId": 4,
   "message": "Tickets merged. This is permanent \u2014 there is no unmerge."
  }
 },
 "ticket_split": {
  "status": 200,
  "body": {
   "ok": true,
   "id": 5,
   "ticketId": 3,
   "movedEntries": 2,
   "message": "Ticket split."
  }
 },
 "export_full": {
  "status": 200,
  "body": {
   "ok": true,
   "filename": "sentinel-command-full-export.zip",
   "zipBase64": {
    "length": 114928,
    "sha256": "859f431bcce7885c171ed5433dc72b85b770c3221c2100a7374aa016c65008f8"
   },
   "counts": {
    "activity": 85,
    "assets": 0,
    "b3_keystore": 0,
    "b3_secrets": 0,
    "b3_sub_assignments": 0,
    "b3_sub_uploads": 0,
    "b3_tips": 0,
    "b3e_auto_runs": 0,
    "b3e_device_hashes": 1,
    "b3e_kb_review": 0,
    "b3e_lead_packets": 0,
    "b3e_owner_alerts": 0,
    "b3e_postmortems": 0,
    "b3e_review_drafts": 0,
    "b3e_ticket_drafts": 0,
    "b3e_ticket_notes": 0,
    "b4_assessment_controls": 0,
    "b4_assessments": 0,
    "b4_review_responses": 0,
    "b4c_api_keys": 0,
    "b4c_webhook_deliveries": 0,
    "b4c_webhooks": 0,
    "b4f_attachment_text": 1,
    "b4f_runbook_runs": 0,
    "b4g_audio_notes": 1,
    "b4g_count_lines": 0,
    "b4g_count_sessions": 0,
    "b4g_photo_overlays": 0,
    "b4g_sync_receipts": 0,
    "b4h_gcal_links": 0,
    "b4h_gcal_outbox": 0,
    "b4i_pack_log": 0,
    "b4i_score_events": 0,
    "b4i_score_state": 1,
    "b4j_claim_documents": 0,
    "b4j_packet_batches": 0,
    "b4j_packet_clients": 0,
    "b4j_packet_items": 0,
    "b4j_warranty_claim_events": 0,
    "b4j_warranty_claims": 0,
    "booking_onboarding": 0,
    "booking_reminders": 0,
    "breach_circulation": 0,
    "breach_intake_runs": 1,
    "breach_intel": 0,
    "breach_sources": 0,
    "cases": 0,
    "cf_asset_disposals": 0,
    "cf_asset_import_batches": 0,
    "cf_asset_import_rows": 0,
    "cf_assign_rules": 0,
    "cf_availability": 0,
    "cf_b3_aging_drafts": 0,
    "cf_b3_pay_states": 0,
    "cf_b3_purchase_drafts": 0,
    "cf_backup_tests": 0,
    "cf_booking_services": 0,
    "cf_bookings": 0,
    "cf_breach_matches": 0,
    "cf_canned_responses": 0,
    "cf_change_requests": 0,
    "cf_checklist_runs": 1,
    "cf_checklist_templates": 1,
    "cf_client_contacts": 1,
    "cf_client_digests": 0,
    "cf_client_import_batches": 0,
    "cf_client_import_rows": 0,
    "cf_client_notes": 1,
    "cf_client_reports": 1,
    "cf_compliance_items": 6,
    "cf_contract_reminders": 0,
    "cf_contract_templates": 0,
    "cf_contracts": 0,
    "cf_doc_packets": 0,
    "cf_domain_watches": 1,
    "cf_emergency_contacts": 0,
    "cf_engagements": 0,
    "cf_equipment": 0,
    "cf_equipment_mileage_links": 0,
    "cf_expenses": 0,
    "cf_file_request_items": 0,
    "cf_file_requests": 0,
    "cf_finding_evidence": 0,
    "cf_finding_templates": 0,
    "cf_findings": 0,
    "cf_firm_documents": 0,
    "cf_followups": 2,
    "cf_hygiene_items": 1,
    "cf_installments": 0,
    "cf_invoice_payments": 0,
    "cf_invoice_workflow": 0,
    "cf_job_photos": 0,
    "cf_kb_audience": 0,
    "cf_kb_shares": 0,
    "cf_leads": 0,
    "cf_licenses": 0,
    "cf_locations": 0,
    "cf_maint_windows": 0,
    "cf_messages": 0,
    "cf_networks": 0,
    "cf_notification_prefs": 1,
    "cf_offboards": 1,
    "cf_onboarding_submissions": 0,
    "cf_owner_tasks": 0,
    "cf_part_movements": 3,
    "cf_parts": 1,
    "cf_payment_plans": 0,
    "cf_plan_periods": 0,
    "cf_plans": 0,
    "cf_playbook_runs": 1,
    "cf_playbooks": 1,
    "cf_price_items": 0,
    "cf_proposals": 0,
    "cf_quote_followups": 0,
    "cf_quote_lines": 0,
    "cf_quotes": 0,
    "cf_referrals": 0,
    "cf_reminder_drafts": 0,
    "cf_report_schedules": 0,
    "cf_report_snapshots": 0,
    "cf_retainer_entries": 0,
    "cf_retainers": 0,
    "cf_retention_reports": 0,
    "cf_retention_settings": 0,
    "cf_risks": 1,
    "cf_roadmap_items": 4,
    "cf_saved_views": 0,
    "cf_scan_findings": 0,
    "cf_scans": 0,
    "cf_signatures": 0,
    "cf_site_diagrams": 0,
    "cf_site_profiles": 0,
    "cf_sla_breaches": 0,
    "cf_sla_config": 4,
    "cf_sla_escalation_config": 2,
    "cf_sla_escalations": 0,
    "cf_staff_availability": 0,
    "cf_staff_certs": 0,
    "cf_staff_training": 0,
    "cf_survey_responses": 0,
    "cf_surveys": 0,
    "cf_threat_briefs": 0,
    "cf_ticket_merges": 2,
    "cf_time_entries": 1,
    "cf_vendors": 0,
    "cf_warranties": 0,
    "client_filter_presets": 0,
    "clients": 4,
    "entries": 0,
    "estate_reports": 0,
    "form_files": 0,
    "form_records": 0,
    "import_batches": 0,
    "invoice_items": 1,
    "invoices": 1,
    "job_assignees": 0,
    "jobs": 0,
    "knowledge": 0,
    "law_ai_requests": 0,
    "law_documents": 0,
    "news_items": 0,
    "osint_reports": 0,
    "osint_searches": 0,
    "osint_watches": 0,
    "platform_digests": 0,
    "review_requests": 0,
    "reviews": 0,
    "roles": 4,
    "schedule_entries": 0,
    "settings": 1,
    "threat_intel_item_sources": 156,
    "threat_intel_items": 152,
    "threat_intel_runs": 1,
    "threat_intel_source_health": 9,
    "threat_to_news": 0,
    "tickets": 5,
    "user_known_ips": 1,
    "users": 2,
    "verifications": 1
   },
   "totalRows": 465,
   "excludedTables": [
    "__drizzle_migrations",
    "app_devices",
    "automation_state",
    "invites",
    "recovery_codes",
    "sessions",
    "sqlite_sequence",
    "totp_challenges"
   ]
  }
 },
 "export_workspace": {
  "status": 200,
  "body": {
   "export": {
    "exportedAt": 1793728789489,
    "exportedBy": "OwnerP3",
    "workspace": {
     "clients": [
      {
       "id": 1,
       "legalName": "Phase3 Client LLC",
       "dba": "",
       "type": "Corporate",
       "industry": "Testing",
       "status": "Active",
       "stage": "Lead",
       "value": 1000.0,
       "nextFollowUp": null,
       "source": "",
       "sourceDetail": "",
       "contactName": "Pat P",
       "contactTitle": "",
       "email": "pat@example.invalid",
       "phone": "555-0101",
       "street": "",
       "city": "",
       "state": "",
       "zip": "",
       "notes": "[DRAFT \u2014 not sent] 2026-11-03 (email)\nHello from Sentinel.",
       "tags": "",
       "friends": false,
       "importBatchId": null,
       "archivedAt": null,
       "archivedBy": null,
       "createdAt": 1793728745365,
       "updatedAt": 1793728752290,
       "familyLinked": false
      },
      {
       "id": 2,
       "legalName": "Dup Alpha LLC",
       "dba": "",
       "type": "Corporate",
       "industry": "Testing",
       "status": "Active",
       "stage": "Lead",
       "value": 1000.0,
       "nextFollowUp": null,
       "source": "",
       "sourceDetail": "",
       "contactName": "Pat P",
       "contactTitle": "",
       "email": "dup@example.invalid",
       "phone": "555-7777",
       "street": "",
       "city": "",
       "state": "",
       "zip": "",
       "notes": "Merged from Dup Beta LLC (record #3) on 2026-11-03:\nlegalName: Dup Beta LLC",
       "tags": "",
       "friends": false,
       "importBatchId": null,
       "archivedAt": null,
       "archivedBy": null,
       "createdAt": 1793728770752,
       "updatedAt": 1793728772139,
       "familyLinked": false
      },
      {
       "id": 3,
       "legalName": "Dup Beta LLC",
       "dba": "",
       "type": "Corporate",
       "industry": "Testing",
       "status": "Active",
       "stage": "Lead",
       "value": 1000.0,
       "nextFollowUp": null,
       "source": "",
       "sourceDetail": "",
       "contactName": "Pat P",
       "contactTitle": "",
       "email": "dup@example.invalid",
       "phone": "555-7777",
       "street": "",
       "city": "",
       "state": "",
       "zip": "",
       "notes": "",
       "tags": "",
       "friends": false,
       "importBatchId": null,
       "archivedAt": 1793728772141,
       "archivedBy": 1,
       "createdAt": 1793728770988,
       "updatedAt": 1793728772141,
       "familyLinked": false
      },
      {
       "id": 4,
       "legalName": "Bulk One LLC",
       "dba": "",
       "type": "Corporate",
       "industry": "Testing",
       "status": "Active",
       "stage": "Qualified",
       "value": 1000.0,
       "nextFollowUp": null,
       "source": "",
       "sourceDetail": "",
       "contactName": "Pat P",
       "contactTitle": "",
       "email": "pat@example.invalid",
       "phone": "555-0101",
       "street": "",
       "city": "",
       "state": "",
       "zip": "",
       "notes": "",
       "tags": "",
       "friends": false,
       "importBatchId": null,
       "archivedAt": null,
       "archivedBy": null,
       "createdAt": 1793728776857,
       "updatedAt": 1793728777274,
       "familyLinked": false
      }
     ],
     "cases": [],
     "tickets": [
      {
       "id": 5,
       "clientId": 1,
       "assignedUserId": null,
       "title": "Split-off ticket",
       "status": "Open",
       "priority": "Normal",
       "category": "Support",
       "dueDate": null,
       "openedDate": "",
       "closedDate": "",
       "notes": "Split from ticket #3.",
       "adMaterial": false,
       "archivedAt": null,
       "archivedBy": null,
       "createdAt": 1793728789010,
       "updatedAt": 1793728789010,
       "caseId": null,
       "source": "Internal",
       "sourceRef": null,
       "mergedIntoId": null
      },
      {
       "id": 3,
       "clientId": 1,
       "assignedUserId": null,
       "title": "Merge survivor",
       "status": "Open",
       "priority": "Normal",
       "category": "Support",
       "dueDate": null,
       "openedDate": "",
       "closedDate": "",
       "notes": "keep",
       "adMaterial": false,
       "archivedAt": null,
       "archivedBy": null,
       "createdAt": 1793728788170,
       "updatedAt": 1793728788170,
       "caseId": null,
       "source": "Internal",
       "sourceRef": null,
       "mergedIntoId": null
      },
      {
       "id": 2,
       "clientId": 1,
       "assignedUserId": 1,
       "title": "Onboard workstation",
       "status": "Open",
       "priority": "Normal",
       "category": "Install",
       "dueDate": null,
       "openedDate": "",
       "closedDate": "",
       "notes": "Spawned by Playbook 'P3 playbook' (run #1).",
       "adMaterial": false,
       "archivedAt": null,
       "archivedBy": null,
       "createdAt": 1793728784762,
       "updatedAt": 1793728784881,
       "caseId": null,
       "source": "Internal",
       "sourceRef": null,
       "mergedIntoId": null
      },
      {
       "id": 1,
       "clientId": 1,
       "assignedUserId": null,
       "title": "Phase3 ticket",
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
       "createdAt": 1793728745741,
       "updatedAt": 1793728778662,
       "caseId": null,
       "source": "Internal",
       "sourceRef": null,
       "mergedIntoId": null
      }
     ],
     "assets": [],
     "invoices": [
      {
       "id": 1,
       "invoiceNumber": "INV-0001",
       "clientId": 1,
       "status": "Draft",
       "issueDate": "2026-11-03",
       "dueDate": "",
       "paidDate": "",
       "recipientEmail": "",
       "notes": "Created from tracked time.",
       "taxRatePercent": 0.0,
       "items": [
        {
         "description": "Manual work (0h 45m)",
         "quantity": 0.75,
         "unitRateCents": 12000,
         "lineTotalCents": 9000
        }
       ],
       "subtotalCents": 9000,
       "taxCents": 0,
       "totalCents": 9000,
       "overdue": false,
       "createdAt": 1793728782984,
       "updatedAt": 1793728782984,
       "discountCents": 0,
       "paidCents": 0,
       "balanceDueCents": 9000,
       "paymentEnabled": false,
       "paymentMode": "",
       "paymentLinkUrl": null,
       "paymentFamilyBlocked": false
      }
     ],
     "forms": [],
     "knowledge": [],
     "scheduleEntries": [],
     "jobs": [],
     "employees": [
      {
       "id": 1,
       "displayName": "OwnerP3",
       "role": "OWNER",
       "portalLang": "en",
       "clientId": null,
       "jobTitle": "",
       "email": "",
       "phone": "",
       "active": true,
       "createdAt": 1793728742174
      },
      {
       "id": 2,
       "displayName": "StaffP3",
       "role": "STAFF",
       "portalLang": "en",
       "clientId": null,
       "jobTitle": "Tech",
       "email": "",
       "phone": "",
       "active": true,
       "createdAt": 1793728744292
      }
     ],
     "activity": [
      {
       "id": 86,
       "actorId": 1,
       "action": "FULL_EXPORT",
       "entityType": "settings",
       "entityId": 1,
       "detail": "Full CRM export built by OwnerP3: 179 tables, 465 rows (credential tables excluded).",
       "createdAt": 1793728789298
      },
      {
       "id": 85,
       "actorId": 1,
       "action": "CF_TICKET_SPLIT_IN",
       "entityType": "ticket",
       "entityId": 5,
       "detail": "This ticket was split from ticket #3 'Merge survivor'.",
       "createdAt": 1793728789010
      },
      {
       "id": 84,
       "actorId": 1,
       "action": "CF_TICKET_SPLIT_OUT",
       "entityType": "ticket",
       "entityId": 3,
       "detail": "Timeline entries after entry #80 were split into new ticket #5 'Split-off ticket'.",
       "createdAt": 1793728789010
      },
      {
       "id": 83,
       "actorId": 1,
       "action": "CF_TICKET_MERGED_INTO",
       "entityType": "ticket",
       "entityId": 4,
       "detail": "This ticket was merged into ticket #3 'Merge survivor'. The merge is permanent \u2014 there is no unmerge.",
       "createdAt": 1793728788695
      },
      {
       "id": 82,
       "actorId": 1,
       "action": "CF_TICKET_MERGED",
       "entityType": "ticket",
       "entityId": 5,
       "detail": "[Split from ticket #3] Ticket #4 'Merge duplicate' was merged into this ticket. The merge is permanent \u2014 there is no unmerge.",
       "createdAt": 1793728788694
      },
      {
       "id": 81,
       "actorId": 1,
       "action": "Ticket_SAVED",
       "entityType": "ticket",
       "entityId": 5,
       "detail": "[Split from ticket #3] [From merged ticket #4] Ticket saved.",
       "createdAt": 1793728788332
      },
      {
       "id": 80,
       "actorId": 1,
       "action": "Ticket_SAVED",
       "entityType": "ticket",
       "entityId": 3,
       "detail": "Ticket saved.",
       "createdAt": 1793728788170
      },
      {
       "id": 79,
       "actorId": 1,
       "action": "CF_PART_RESTOCKED",
       "entityType": "asset",
       "entityId": 1,
       "detail": "4 x 'P3 Widget' restocked (7 on hand).",
       "createdAt": 1793728787802
      },
      {
       "id": 78,
       "actorId": 1,
       "action": "CF_PART_CONSUMED",
       "entityType": "ticket",
       "entityId": 1,
       "detail": "2 x 'P3 Widget' consumed onto this ticket (3 left).",
       "createdAt": 1793728787539
      },
      {
       "id": 77,
       "actorId": 1,
       "action": "CF_PART_SAVED",
       "entityType": "asset",
       "entityId": 1,
       "detail": "Part 'P3 Widget' saved.",
       "createdAt": 1793728787246
      },
      {
       "id": 76,
       "actorId": 1,
       "action": "CF_CHECKLIST_SIGNED",
       "entityType": "cf_checklist",
       "entityId": 1,
       "detail": "Checklist run signed off by Pat P (summary hash 0505b73c8adb\u2026).",
       "createdAt": 1793728786837
      },
      {
       "id": 75,
       "actorId": 1,
       "action": "CF_CHECKLIST_ITEM_DONE",
       "entityType": "cf_checklist",
       "entityId": 1,
       "detail": "Checklist item completed: Step two",
       "createdAt": 1793728786438
      },
      {
       "id": 74,
       "actorId": 1,
       "action": "CF_CHECKLIST_ITEM_DONE",
       "entityType": "cf_checklist",
       "entityId": 1,
       "detail": "Checklist item completed: Step one",
       "createdAt": 1793728786086
      },
      {
       "id": 73,
       "actorId": 1,
       "action": "CF_CHECKLIST_RUN_STARTED",
       "entityType": "cf_checklist",
       "entityId": 1,
       "detail": "Site-visit checklist 'P3 checklist' started against ticket #1.",
       "createdAt": 1793728785740
      },
      {
       "id": 72,
       "actorId": 1,
       "action": "CF_CHECKLIST_TEMPLATE_SAVED",
       "entityType": "cf_checklist",
       "entityId": 1,
       "detail": "Checklist template 'P3 checklist' saved (2 items).",
       "createdAt": 1793728785342
      },
      {
       "id": 71,
       "actorId": 1,
       "action": "B3D_PLAYBOOK_APPLIED",
       "entityType": "cf_playbook",
       "entityId": 1,
       "detail": "Playbook 'P3 playbook' applied to Phase3 Client LLC: ticket #2, checklist run #None, site survey #None.",
       "createdAt": 1793728785018
      },
      {
       "id": 70,
       "actorId": 1,
       "action": "Ticket_SAVED",
       "entityType": "ticket",
       "entityId": 2,
       "detail": "Ticket saved.",
       "createdAt": 1793728784881
      },
      {
       "id": 69,
       "actorId": 1,
       "action": "Ticket_SAVED",
       "entityType": "ticket",
       "entityId": 2,
       "detail": "Ticket saved.",
       "createdAt": 1793728784763
      },
      {
       "id": 68,
       "actorId": 1,
       "action": "B3D_PLAYBOOK_SAVED",
       "entityType": "cf_playbook",
       "entityId": 1,
       "detail": "Service playbook 'P3 playbook' saved.",
       "createdAt": 1793728784338
      },
      {
       "id": 67,
       "actorId": 1,
       "action": "PLAN_DELETED",
       "entityType": "cf_plan",
       "entityId": 1,
       "detail": "Recurring plan 'Monthly care' deleted.",
       "createdAt": 1793728783995
      },
      {
       "id": 66,
       "actorId": 1,
       "action": "PLAN_CREATED",
       "entityType": "cf_plan",
       "entityId": 1,
       "detail": "Recurring plan 'Monthly care' created (monthly, ${'$'}0.00).",
       "createdAt": 1793728783641
      },
      {
       "id": 65,
       "actorId": 1,
       "action": "TIME_ENTRY_DELETED",
       "entityType": "cf_time_entry",
       "entityId": 1,
       "detail": "Time entry deleted: Diagnosis.",
       "createdAt": 1793728783298
      },
      {
       "id": 64,
       "actorId": 1,
       "action": "TIME_CONVERTED_TO_INVOICE",
       "entityType": "invoice",
       "entityId": 1,
       "detail": "1 time entry (2700s) converted to draft invoice INV-0001.",
       "createdAt": 1793728782987
      },
      {
       "id": 63,
       "actorId": 1,
       "action": "TIME_ENTRY_CREATED",
       "entityType": "cf_time_entry",
       "entityId": 2,
       "detail": "Manual time entry created: Manual work (2700s).",
       "createdAt": 1793728782555
      },
      {
       "id": 62,
       "actorId": 1,
       "action": "TIME_TIMER_STOPPED",
       "entityType": "cf_time_entry",
       "entityId": 1,
       "detail": "Timer stopped after 0s: Diagnosis.",
       "createdAt": 1793728782364
      },
      {
       "id": 61,
       "actorId": 1,
       "action": "TIME_TIMER_STARTED",
       "entityType": "cf_time_entry",
       "entityId": 1,
       "detail": "Timer started: Diagnosis.",
       "createdAt": 1793728782057
      },
      {
       "id": 60,
       "actorId": 1,
       "action": "CANNED_RESPONSE_DELETED",
       "entityType": "canned_response",
       "entityId": 1,
       "detail": "Canned response 'P3 greeting' deleted.",
       "createdAt": 1793728781754
      },
      {
       "id": 59,
       "actorId": 1,
       "action": "CANNED_RESPONSE_SAVED",
       "entityType": "canned_response",
       "entityId": 1,
       "detail": "Canned response 'P3 greeting' saved.",
       "createdAt": 1793728781254
      },
      {
       "id": 58,
       "actorId": 1,
       "action": "B4G_AUDIO_NOTE_ADDED",
       "entityType": "ticket",
       "entityId": 1,
       "detail": "Voice note added to this ticket: note.wav.",
       "createdAt": 1793728780809
      },
      {
       "id": 57,
       "actorId": 1,
       "action": "CF_PHOTO_DELETED",
       "entityType": "ticket",
       "entityId": 1,
       "detail": "Job photo (other) site.png deleted from this ticket.",
       "createdAt": 1793728780621
      },
      {
       "id": 56,
       "actorId": 1,
       "action": "CF_PHOTO_DOWNLOADED",
       "entityType": "ticket",
       "entityId": 1,
       "detail": "Job photo (other) downloaded.",
       "createdAt": 1793728780438
      },
      {
       "id": 55,
       "actorId": 1,
       "action": "CF_PHOTO_UPLOADED",
       "entityType": "ticket",
       "entityId": 1,
       "detail": "Job photo (other) added to this ticket: site.png.",
       "createdAt": 1793728779958
      },
      {
       "id": 54,
       "actorId": 1,
       "action": "CF_SLA_CONFIG_UPDATED",
       "entityType": "ticket",
       "entityId": null,
       "detail": "SLA targets for High set to respond within 30 min, resolve within 240 min.",
       "createdAt": 1793728779766
      },
      {
       "id": 53,
       "actorId": 1,
       "action": "CF_EXPENSE_DELETED",
       "entityType": "ticket",
       "entityId": 1,
       "detail": "Expense #1 (mileage, ${'$'}6.70) deleted.",
       "createdAt": 1793728779342
      },
      {
       "id": 52,
       "actorId": 1,
       "action": "CF_EXPENSE_ADDED",
       "entityType": "ticket",
       "entityId": 1,
       "detail": "Expense added (mileage, ${'$'}6.70) on this ticket.",
       "createdAt": 1793728778941
      },
      {
       "id": 51,
       "actorId": 1,
       "action": "TICKET_STATUS_CHANGED",
       "entityType": "ticket",
       "entityId": 1,
       "detail": "Ticket status changed from Closed to Open for Phase3 ticket.",
       "createdAt": 1793728778662
      },
      {
       "id": 50,
       "actorId": 1,
       "action": "CF_FOLLOWUP_STATE",
       "entityType": "ticket",
       "entityId": 1,
       "detail": "Follow-up task #1 (day30, due 2026-12-03) marked done.",
       "createdAt": 1793728778507
      },
      {
       "id": 49,
       "actorId": 1,
       "action": "CF_FOLLOWUPS_CREATED",
       "entityType": "ticket",
       "entityId": 1,
       "detail": "2 follow-up tasks scheduled for closed ticket 'Phase3 ticket' (+30 days, +90 days; drafts only \u2014 nothing is sent automatically).",
       "createdAt": 1793728778055
      },
      {
       "id": 48,
       "actorId": 1,
       "action": "TICKET_STATUS_CHANGED",
       "entityType": "ticket",
       "entityId": 1,
       "detail": "Ticket status changed from Open to Closed for Phase3 ticket.",
       "createdAt": 1793728778054
      },
      {
       "id": 47,
       "actorId": 1,
       "action": "CLIENT_STAGE_CHANGED",
       "entityType": "client",
       "entityId": 4,
       "detail": "Client stage changed from Lead to Qualified for Bulk One LLC.",
       "createdAt": 1793728777274
      },
      {
       "id": 46,
       "actorId": 1,
       "action": "CLIENT_SAVED",
       "entityType": "client",
       "entityId": 4,
       "detail": "Client saved.",
       "createdAt": 1793728776860
      },
      {
       "id": 45,
       "actorId": 1,
       "action": "CF_VIEW_SAVED",
       "entityType": "cf_saved_views",
       "entityId": null,
       "detail": "Saved view 'P3 view' on clients.",
       "createdAt": 1793728775744
      },
      {
       "id": 44,
       "actorId": 1,
       "action": "CLIENT_FILTER_PRESET_DELETED",
       "entityType": "client_filter_preset",
       "entityId": 1,
       "detail": "Filter preset 'P3 preset' deleted.",
       "createdAt": 1793728775181
      },
      {
       "id": 43,
       "actorId": 1,
       "action": "CLIENT_FILTER_PRESET_SAVED",
       "entityType": "client_filter_preset",
       "entityId": 1,
       "detail": "Filter preset 'P3 preset' saved.",
       "createdAt": 1793728774357
      },
      {
       "id": 42,
       "actorId": 1,
       "action": "VERIFICATIONS_EXPORTED",
       "entityType": "verification",
       "entityId": null,
       "detail": "Public verifications export written: 1 record(s), generated 2026-11-03.",
       "createdAt": 1793728774056
      },
      {
       "id": 41,
       "actorId": 1,
       "action": "VERIFICATION_REVOKED",
       "entityType": "verification",
       "entityId": 1,
       "detail": "Sentinel Verified SV-YYJ6ZU-DAS6L7 revoked permanently.",
       "createdAt": 1793728773660
      },
      {
       "id": 40,
       "actorId": 1,
       "action": "VERIFICATION_ISSUED",
       "entityType": "verification",
       "entityId": 1,
       "detail": "Sentinel Verified SV-YYJ6ZU-DAS6L7 issued to Phase3 Client LLC for Security Assessment; issued 2026-11-03, expires 2027-11-03.",
       "createdAt": 1793728772808
      },
      {
       "id": 39,
       "actorId": 1,
       "action": "CLIENT_MERGED",
       "entityType": "client",
       "entityId": 2,
       "detail": "Merged client #3 (Dup Beta LLC) into client #2 (Dup Alpha LLC); re-pointed . Pre-merge snapshot: {\"winnerId\": 2, \"loserId\": 3, \"winner\": {\"id\": 2, \"legal_name\": \"Dup Alpha LLC\", \"dba\": \"\", \"type\": \"Corporate\", \"industry\": \"Testing\", \"status\": \"Active\", \"contact_name\": \"Pat P\", \"contact_title\": \"\", \"email\": \"dup@example.invalid\", \"phone\": \"555-7777\", \"street\": \"\", \"city\": \"\", \"state\": \"\", \"zip\": \"\", \"notes\": \"\", \"tags\": \"\", \"stage\": \"Lead\", \"value\": 1000.0, \"next_follow_up\": null, \"source\": \"\", \"source_detail\": \"\", \"friends\": 0, \"import_batch_id\": null, \"archived_at\": null, \"archived_by\": null, \"created_at\": 1793728770752, \"updated_at\": 1793728770752, \"academy_org_id\": null}, \"loser\": {\"id\": 3, \"legal_name\": \"Dup Beta LLC\", \"dba\": \"\", \"type\": \"Corporate\", \"industry\": \"Testing\", \"status\": \"Active\", \"contact_name\": \"Pat P\", \"contact_title\": \"\", \"email\": \"dup@example.invalid\", \"phone\": \"555-7777\", \"street\": \"\", \"city\": \"\", \"state\": \"\", \"zip\": \"\", \"notes\": \"\", \"tags\": \"\", \"stage\": \"Lead\", \"value\": 1000.0, \"next_follow_up\": null, \"source\": \"\", \"source_detail\": \"\", \"friends\": 0, \"import_batch_id\": null, \"archived_at\": null, \"archived_by\": null, \"created_at\": 1793728770988, \"updated_at\": 1793728770988, \"academy_org_id\": null}, \"picks\": {}, \"mergedAt\": 1793728772136}",
       "createdAt": 1793728772141
      },
      {
       "id": 38,
       "actorId": 1,
       "action": "CLIENT_SAVED",
       "entityType": "client",
       "entityId": 3,
       "detail": "Client saved.",
       "createdAt": 1793728770988
      },
      {
       "id": 37,
       "actorId": 1,
       "action": "CLIENT_SAVED",
       "entityType": "client",
       "entityId": 2,
       "detail": "Client saved.",
       "createdAt": 1793728770752
      },
      {
       "id": 36,
       "actorId": 1,
       "action": "CF_DIGESTS_GENERATED",
       "entityType": "cf_client_digest",
       "entityId": null,
       "detail": "Weekly digest drafts generated for week of 2026-11-02: 0 draft(s), 1 skipped (Phase3 Client LLC: client notification preference: weekly digest off), 0 undeliverable (no email). Nothing was sent.",
       "createdAt": 1793728770190
      },
      {
       "id": 35,
       "actorId": 1,
       "action": "CF_DIGEST_SKIPPED",
       "entityType": "client",
       "entityId": 1,
       "detail": "Weekly digest for week of 2026-11-02 skipped for Phase3 Client LLC: client notification preference: weekly digest off.",
       "createdAt": 1793728770189
      },
      {
       "id": 34,
       "actorId": 1,
       "action": "CF2_REPORT_SENT",
       "entityType": "client",
       "entityId": 1,
       "detail": "Monthly report for 2026-10 sent to pat@example.invalid for Phase3 Client LLC.",
       "createdAt": 1793728769155
      },
      {
       "id": 33,
       "actorId": 1,
       "action": "CF2_REPORT_GENERATED",
       "entityType": "client",
       "entityId": 1,
       "detail": "Monthly client report for 2026-10 generated for Phase3 Client LLC (draft; nothing was sent).",
       "createdAt": 1793728768075
      },
      {
       "id": 32,
       "actorId": 1,
       "action": "OFFBOARD_STEP",
       "entityType": "client",
       "entityId": 1,
       "detail": "Offboarding export generated for Phase3 Client LLC (19 rows across 79 tables; ref cf-export-client-1-1793728765838).",
       "createdAt": 1793728766265
      },
      {
       "id": 31,
       "actorId": 1,
       "action": "OFFBOARD_STARTED",
       "entityType": "client",
       "entityId": 1,
       "detail": "Offboarding started for Phase3 Client LLC.",
       "createdAt": 1793728765367
      },
      {
       "id": 30,
       "actorId": 1,
       "action": "POSTURE_LETTER_SENT",
       "entityType": "client",
       "entityId": 1,
       "detail": "Monthly posture letter for November 2026 sent to pat@example.invalid for Phase3 Client LLC.",
       "createdAt": 1793728764355
      },
      {
       "id": 29,
       "actorId": 1,
       "action": "POSTURE_LETTER_GENERATED",
       "entityType": "client",
       "entityId": 1,
       "detail": "Monthly posture letter for November 2026 generated for Phase3 Client LLC (score 42); nothing was sent.",
       "createdAt": 1793728763684
      },
      {
       "id": 28,
       "actorId": 1,
       "action": "CF2_HYGIENE_SET",
       "entityType": "client",
       "entityId": 1,
       "detail": "Hygiene 'mfa_enabled' marked yes for Phase3 Client LLC (state only -- no credential value stored).",
       "createdAt": 1793728763166
      },
      {
       "id": 27,
       "actorId": 1,
       "action": "CF_SITE_PROFILE_DELETED",
       "entityType": "client",
       "entityId": 1,
       "detail": "Site profile 'HQ' deleted.",
       "createdAt": 1793728762337
      },
      {
       "id": 26,
       "actorId": 1,
       "action": "CF_SITE_DIAGRAM_DELETED",
       "entityType": "cf_site_diagram",
       "entityId": 1,
       "detail": "Diagram 'floor.png' removed.",
       "createdAt": 1793728761843
      },
      {
       "id": 25,
       "actorId": 1,
       "action": "CF_SITE_DIAGRAM_DOWNLOADED",
       "entityType": "cf_site_diagram",
       "entityId": 1,
       "detail": "Site diagram 'floor.png' downloaded.",
       "createdAt": 1793728761244
      },
      {
       "id": 24,
       "actorId": 1,
       "action": "CF_SITE_DIAGRAM_ADDED",
       "entityType": "client",
       "entityId": 1,
       "detail": "Diagram 'floor.png' attached to site profile 'HQ'.",
       "createdAt": 1793728760653
      },
      {
       "id": 23,
       "actorId": 1,
       "action": "CF_SITE_PROFILE_SAVED",
       "entityType": "client",
       "entityId": 1,
       "detail": "Site profile 'HQ' saved for client #1 (0 asset reference(s)).",
       "createdAt": 1793728759939
      },
      {
       "id": 22,
       "actorId": 1,
       "action": "CF_ROADMAP_ARCHIVED",
       "entityType": "client",
       "entityId": 1,
       "detail": "Roadmap item #1 archived (history preserved): Enable MFA everywhere.",
       "createdAt": 1793728759351
      },
      {
       "id": 21,
       "actorId": 1,
       "action": "CF_COMPLIANCE_SAVED",
       "entityType": "client",
       "entityId": 1,
       "detail": "Compliance item updated for Phase3 Client LLC: NIST CSF GV -> in_progress.",
       "createdAt": 1793728758988
      },
      {
       "id": 20,
       "actorId": 1,
       "action": "CF_COMPLIANCE_SEEDED",
       "entityType": "client",
       "entityId": 1,
       "detail": "Compliance tracker seeded for Phase3 Client LLC: NIST CSF (6 new item(s), heading-level template).",
       "createdAt": 1793728758192
      },
      {
       "id": 19,
       "actorId": null,
       "action": "THREAT_SYNC_RUN",
       "entityType": "threat_intel",
       "entityId": 1,
       "detail": "Threat Intel sync: +152 new, 5 updated, 1 already on file.",
       "createdAt": 1793728757845
      },
      {
       "id": 18,
       "actorId": 1,
       "action": "CF_BREACH_WATCH_REMOVED",
       "entityType": "client",
       "entityId": 1,
       "detail": "Breach watch removed (paused, history kept) for client #1: example.invalid.",
       "createdAt": 1793728757471
      },
      {
       "id": 17,
       "actorId": 1,
       "action": "CF_BREACH_MATCH_RUN",
       "entityType": "client",
       "entityId": 1,
       "detail": "Breach matcher run: 0 new match(es) across 1 active watch(es).",
       "createdAt": 1793728756958
      },
      {
       "id": 16,
       "actorId": 1,
       "action": "CF_BREACH_WATCH_SAVED",
       "entityType": "client",
       "entityId": 1,
       "detail": "Breach watch added for Phase3 Client LLC: example.invalid (active).",
       "createdAt": 1793728756062
      },
      {
       "id": 15,
       "actorId": 1,
       "action": "CF_RISK_STATE_CHANGED",
       "entityType": "client",
       "entityId": 1,
       "detail": "Risk #1 state changed from open to accepted: Legacy VPN.",
       "createdAt": 1793728755683
      },
      {
       "id": 14,
       "actorId": 1,
       "action": "CF_RISK_SAVED",
       "entityType": "client",
       "entityId": 1,
       "detail": "Risk register: risk #1 recorded for Phase3 Client LLC: Legacy VPN (state open, score 9).",
       "createdAt": 1793728755205
      },
      {
       "id": 13,
       "actorId": 1,
       "action": "CF_ROADMAP_STATE_CHANGED",
       "entityType": "client",
       "entityId": 1,
       "detail": "Roadmap item #1 state changed from planned to in_progress: Enable MFA everywhere.",
       "createdAt": 1793728754466
      },
      {
       "id": 12,
       "actorId": 1,
       "action": "CF_ROADMAP_GENERATED",
       "entityType": "client",
       "entityId": 1,
       "detail": "Roadmap suggestions generated for Phase3 Client LLC from posture inputs: 3 new, 0 already on the roadmap.",
       "createdAt": 1793728753779
      },
      {
       "id": 11,
       "actorId": 1,
       "action": "CF_ROADMAP_SAVED",
       "entityType": "client",
       "entityId": 1,
       "detail": "Roadmap item #1 recorded for Phase3 Client LLC: Enable MFA everywhere (state planned).",
       "createdAt": 1793728752941
      },
      {
       "id": 10,
       "actorId": 1,
       "action": "OUTREACH_DRAFTED",
       "entityType": "client",
       "entityId": 1,
       "detail": "Outreach draft (email) for Phase3 Client LLC saved to the client's notes, marked as a draft \u2014 nothing was contacted.",
       "createdAt": 1793728752291
      },
      {
       "id": 9,
       "actorId": 1,
       "action": "NOTIFICATION_PREFS_UPDATED",
       "entityType": "client",
       "entityId": 1,
       "detail": "Notification preferences updated: ticket_updates, invoice_events, monitoring_alerts, digest.",
       "createdAt": 1793728751642
      },
      {
       "id": 8,
       "actorId": 1,
       "action": "CLIENT_CONTACT_SAVED",
       "entityType": "client",
       "entityId": 1,
       "detail": "Contact Second Contact Jr (primary) saved for Phase3 Client LLC.",
       "createdAt": 1793728750848
      },
      {
       "id": 7,
       "actorId": 1,
       "action": "CLIENT_CONTACT_SAVED",
       "entityType": "client",
       "entityId": 1,
       "detail": "Contact Second Contact (billing) saved for Phase3 Client LLC.",
       "createdAt": 1793728748446
      },
      {
       "id": 6,
       "actorId": 1,
       "action": "Ticket_SAVED",
       "entityType": "ticket",
       "entityId": 1,
       "detail": "Ticket saved.",
       "createdAt": 1793728745747
      },
      {
       "id": 5,
       "actorId": 1,
       "action": "CLIENT_SAVED",
       "entityType": "client",
       "entityId": 1,
       "detail": "Client saved.",
       "createdAt": 1793728745437
      },
      {
       "id": 4,
       "actorId": 1,
       "action": "APP_LOGIN",
       "entityType": "user",
       "entityId": 1,
       "detail": "App sign-in \u2014 device enrolled: p3-capture.",
       "createdAt": 1793728745084
      },
      {
       "id": 3,
       "actorId": 1,
       "action": "EMPLOYEE_CREATED",
       "entityType": "user",
       "entityId": 2,
       "detail": "Staff user StaffP3 created.",
       "createdAt": 1793728744293
      },
      {
       "id": 2,
       "actorId": 1,
       "action": "LOGIN",
       "entityType": "session",
       "entityId": 1,
       "detail": "Session opened; expires in 8 hours",
       "createdAt": 1793728743140
      },
      {
       "id": 1,
       "actorId": 1,
       "action": "OWNER_INITIALIZED",
       "entityType": "settings",
       "entityId": 1,
       "detail": "Owner account initialized; recovery codes issued.",
       "createdAt": 1793728742246
      }
     ],
     "importBatches": []
    }
   },
   "ok": true
  }
 },
 "export_price_book": {
  "status": 200,
  "body": {
   "ok": true,
   "count": 0,
   "priceBook": {
    "schema": 1,
    "generated_at": "2026-11-03",
    "lines": {},
    "items": []
   }
  }
 },
 "export_labels": {
  "status": 200,
  "body": {
   "labels": [],
   "ok": true
  }
 },
 "export_packet_missing": {
  "status": 404,
  "body": {
   "ok": false,
   "message": "Not found."
  }
 },
 "assurance_packet_create": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Packet saved as a draft.",
   "id": 1
  }
 },
 "assurance_packet_issue": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Packet issued to the client portal."
  }
 },
 "assurance_packets_list": {
  "status": 200,
  "body": {
   "ok": true,
   "packets": [
    {
     "id": 1,
     "clientId": 1,
     "title": "P3 packet",
     "manifest": [
      {
       "kind": "kb",
       "id": 1,
       "title": "P3 Packet KB",
       "category": "Guides",
       "description": "packet source"
      }
     ],
     "itemCount": 1,
     "state": "issued",
     "issuedAt": 1793728791038,
     "issuedBy": 1,
     "createdAt": 1793728790696,
     "updatedAt": 1793728791038
    }
   ],
   "vaultDocs": [],
   "kbItems": [
    {
     "id": 1,
     "title": "P3 Packet KB",
     "category": "Guides",
     "description": "packet source"
    }
   ]
  }
 },
 "assurance_file_request_create": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "File request created.",
   "id": 1
  }
 },
 "assurance_file_requests_list": {
  "status": 200,
  "body": {
   "ok": true,
   "requests": [
    {
     "id": 1,
     "clientId": 1,
     "title": "P3 request",
     "items": [
      {
       "id": 1,
       "label": "W-9",
       "description": "",
       "required": true,
       "status": "pending",
       "uploadedAt": null,
       "formFileId": null,
       "vaultRecordId": null
      }
     ],
     "progress": {
      "total": 1,
      "uploaded": 0,
      "requiredTotal": 1,
      "requiredUploaded": 0,
      "allRequiredDone": false,
      "complete": false
     },
     "createdAt": 1793728791753,
     "updatedAt": 1793728791753
    }
   ]
  }
 },
 "assurance_backup_test_create": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Backup test logged.",
   "id": 1
  }
 },
 "assurance_backup_tests_list": {
  "status": 200,
  "body": {
   "ok": true,
   "tests": [
    {
     "id": 1,
     "assetId": 1,
     "assetName": "P3 NAS",
     "clientId": 1,
     "testedAt": "2026-11-01",
     "method": "full_restore",
     "methodLabel": "Full restore",
     "result": "pass",
     "notes": "ok",
     "testedBy": "OwnerP3",
     "createdAt": 1793728792345
    }
   ],
   "assetStates": [
    {
     "assetKey": "asset:1",
     "assetId": 1,
     "assetName": "P3 NAS",
     "clientId": 1,
     "clientName": "Phase3 Client LLC",
     "cadenceDays": 90,
     "status": "current",
     "lastTestedAt": "2026-11-01",
     "lastResult": "pass",
     "lastMethod": "full_restore",
     "daysSinceTest": 2
    }
   ],
   "globalCadenceDays": 90,
   "methods": [
    "full_restore",
    "file_level",
    "boot_test",
    "other"
   ],
   "methodLabels": {
    "full_restore": "Full restore",
    "file_level": "File-level restore",
    "boot_test": "Boot test",
    "other": "Other"
   },
   "results": [
    "pass",
    "fail",
    "partial"
   ]
  }
 },
 "assurance_backup_cadence_set": {
  "status": 404,
  "body": {
   "ok": false,
   "message": "Not found."
  }
 },
 "assurance_scan_refusal_no_authz": {
  "status": 403,
  "body": {
   "ok": false,
   "authorized": false,
   "message": "Hard gate: a vulnerability scan can only be recorded for a client with a fully signed pentest authorization on file (client signature AND owner countersignature). No signed authorization exists for this client, so nothing was recorded."
  }
 },
 "contracts_list": {
  "status": 200,
  "body": {
   "ok": true,
   "contracts": [],
   "templates": [
    {
     "id": 1,
     "templateType": "service_agreement",
     "title": "Service Agreement",
     "body": "[OWNER-REPLACE PLACEHOLDER -- Service Agreement]\n\nNo Knowledge Base / client-form record for this template was found when it was seeded, so no legal text has been invented for it. The owner must paste the approved Service Agreement text here (Knowledge Base: client forms) before this template is sent. Do not send this placeholder to a client.",
     "kbSourceType": "",
     "kbSourceId": null,
     "kbSourceTitle": "",
     "createdAt": 1793728792763,
     "updatedAt": 1793728792763
    },
    {
     "id": 2,
     "templateType": "authorization",
     "title": "Pentest Authorization",
     "body": "[OWNER-REPLACE PLACEHOLDER -- Pentest Authorization]\n\nNo Knowledge Base / client-form record for this template was found when it was seeded, so no legal text has been invented for it. The owner must paste the approved Pentest Authorization text here (Knowledge Base: client forms) before this template is sent. Do not send this placeholder to a client.",
     "kbSourceType": "",
     "kbSourceId": null,
     "kbSourceTitle": "",
     "createdAt": 1793728792768,
     "updatedAt": 1793728792768
    }
   ],
   "contractTypes": [
    "service_agreement",
    "authorization"
   ],
   "contractStatuses": [
    "draft",
    "sent",
    "awaiting",
    "partially-signed",
    "signed",
    "void"
   ]
  }
 },
 "contract_create": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Contract created in draft.",
   "id": 1,
   "contract": {
    "id": 1,
    "clientId": 1,
    "templateId": 2,
    "templateType": "authorization",
    "title": "P3 Authorization",
    "body": "[OWNER-REPLACE PLACEHOLDER -- Pentest Authorization]\n\nNo Knowledge Base / client-form record for this template was found when it was seeded, so no legal text has been invented for it. The owner must paste the approved Pentest Authorization text here (Knowledge Base: client forms) before this template is sent. Do not send this placeholder to a client.",
    "bodyHash": "a4ba7ebeefe1060a34c3a1034f2c68e6616cf6938a40be47fdd2c904a6174ba7",
    "status": "draft",
    "sentAt": null,
    "signatures": [],
    "createdAt": 1793728793040,
    "updatedAt": 1793728793040
   }
  }
 },
 "contract_template_edit": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Template saved.",
   "template": {
    "id": 1,
    "templateType": "service_agreement",
    "title": "Service Agreement (P3)",
    "body": "Template body.",
    "kbSourceType": "",
    "kbSourceId": null,
    "kbSourceTitle": "",
    "createdAt": 1793728792763,
    "updatedAt": 1793728793196
   }
  }
 },
 "contract_document": {
  "status": 200,
  "body": {
   "pdfBase64": "JVBERi0xLjQKMSAwIG9iago8PCAvVHlwZSAvQ2F0YWxvZyAvUGFnZXMgMiAwIFIgPj4KZW5kb2JqCjIgMCBvYmoKPDwgL1R5cGUgL1BhZ2VzIC9LaWRzIFs0IDAgUl0gL0NvdW50IDEgPj4KZW5kb2JqCjMgMCBvYmoKPDwgL1R5cGUgL0ZvbnQgL1N1YnR5cGUgL1R5cGUxIC9CYXNlRm9udCAvSGVsdmV0aWNhID4+CmVuZG9iago0IDAgb2JqCjw8IC9UeXBlIC9QYWdlIC9QYXJlbnQgMiAwIFIgL01lZGlhQm94IFswIDAgNjEyIDc5Ml0gL1Jlc291cmNlcyA8PCAvRm9udCA8PCAvRjEgMyAwIFIgPj4gPj4gL0NvbnRlbnRzIDUgMCBSID4+CmVuZG9iago1IDAgb2JqCjw8IC9MZW5ndGggOTAzID4+CnN0cmVhbQpCVCAvRjEgMTUgVGYgNTYgNzU2IFRkIChQMyBBdXRob3JpemF0aW9uKSBUaiBFVApCVCAvRjEgMTAgVGYKMSAwIDAgMSA1NiA3MzAgVG0gKFAzIEF1dGhvcml6YXRpb24pIFRqCjEgMCAwIDEgNTYgNzE2IFRtICgpIFRqCjEgMCAwIDEgNTYgNzAyIFRtIChbT1dORVItUkVQTEFDRSBQTEFDRUhPTERFUiAtLSBQZW50ZXN0IEF1dGhvcml6YXRpb25dKSBUagoxIDAgMCAxIDU2IDY4OCBUbSAoKSBUagoxIDAgMCAxIDU2IDY3NCBUbSAoTm8gS25vd2xlZGdlIEJhc2UgLyBjbGllbnQtZm9ybSByZWNvcmQgZm9yIHRoaXMgdGVtcGxhdGUgd2FzIGZvdW5kIHdoZW4gaXQgd2FzIHNlZWRlZCwgc28gbm8pIFRqCjEgMCAwIDEgNTYgNjYwIFRtIChsZWdhbCB0ZXh0IGhhcyBiZWVuIGludmVudGVkIGZvciBpdC4gVGhlIG93bmVyIG11c3QgcGFzdGUgdGhlIGFwcHJvdmVkIFBlbnRlc3QgQXV0aG9yaXphdGlvbikgVGoKMSAwIDAgMSA1NiA2NDYgVG0gKHRleHQgaGVyZSBcKEtub3dsZWRnZSBCYXNlOiBjbGllbnQgZm9ybXNcKSBiZWZvcmUgdGhpcyB0ZW1wbGF0ZSBpcyBzZW50LiBEbyBub3Qgc2VuZCB0aGlzKSBUagoxIDAgMCAxIDU2IDYzMiBUbSAocGxhY2Vob2xkZXIgdG8gYSBjbGllbnQuKSBUagoxIDAgMCAxIDU2IDYxOCBUbSAoKSBUagoxIDAgMCAxIDU2IDYwNCBUbSAoLS0tIFNpZ25hdHVyZXMgLS0tKSBUagoxIDAgMCAxIDU2IDU5MCBUbSAoXChub25lIHlldFwpKSBUagoxIDAgMCAxIDU2IDU3NiBUbSAoKSBUagoxIDAgMCAxIDU2IDU2MiBUbSAoRG9jdW1lbnQgYm9keSBTSEEtMjU2OiBhNGJhN2ViZWVmZTEwNjBhMzRjM2ExMDM0ZjJjNjhlNjYxNmNmNjkzOGE0MGJlNDdmZGQyYzkwNGE2MTc0YmE3KSBUagoxIDAgMCAxIDU2IDU0OCBUbSAoQ29udHJhY3Qgc3RhdHVzOiBkcmFmdCkgVGoKRVQKZW5kc3RyZWFtCmVuZG9iagp4cmVmCjAgNgowMDAwMDAwMDAwIDY1NTM1IGYgCjAwMDAwMDAwMDkgMDAwMDAgbiAKMDAwMDAwMDA1OCAwMDAwMCBuIAowMDAwMDAwMTE1IDAwMDAwIG4gCjAwMDAwMDAxODUgMDAwMDAgbiAKMDAwMDAwMDMxMSAwMDAwMCBuIAp0cmFpbGVyCjw8IC9TaXplIDYgL1Jvb3QgMSAwIFIgPj4Kc3RhcnR4cmVmCjEyNjUKJSVFT0YK",
   "pdfSha256": "442534d1b4148f691fc499322fdc826aeaebb7911700a3233354a5711b4b6630",
   "pdfSizeBytes": 1449,
   "ok": true,
   "fileName": "contract-1.pdf",
   "html": "<!doctype html><html><body><h1>P3 Authorization</h1><pre>[OWNER-REPLACE PLACEHOLDER -- Pentest Authorization]\n\nNo Knowledge Base / client-form record for this template was found when it was seeded, so no legal text has been invented for it. The owner must paste the approved Pentest Authorization text here (Knowledge Base: client forms) before this template is sent. Do not send this placeholder to a client.</pre><hr><h2>Signatures</h2><p>(none yet)</p><p>Document body SHA-256: <code>a4ba7ebeefe1060a34c3a1034f2c68e6616cf6938a40be47fdd2c904a6174ba7</code></p><p>Status: draft</p></body></html>",
   "bodyHash": "a4ba7ebeefe1060a34c3a1034f2c68e6616cf6938a40be47fdd2c904a6174ba7",
   "status": "draft"
  }
 },
 "contract_verify_hash": {
  "status": 200,
  "body": {
   "ok": true,
   "match": true,
   "storedHash": "a4ba7ebeefe1060a34c3a1034f2c68e6616cf6938a40be47fdd2c904a6174ba7",
   "recomputedHash": "a4ba7ebeefe1060a34c3a1034f2c68e6616cf6938a40be47fdd2c904a6174ba7",
   "signatureHashesMatch": true,
   "message": "Document hash matches the signed body."
  }
 },
 "contract_send": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Contract sent.",
   "contract": {
    "id": 1,
    "clientId": 1,
    "templateId": 2,
    "templateType": "authorization",
    "title": "P3 Authorization",
    "body": "[OWNER-REPLACE PLACEHOLDER -- Pentest Authorization]\n\nNo Knowledge Base / client-form record for this template was found when it was seeded, so no legal text has been invented for it. The owner must paste the approved Pentest Authorization text here (Knowledge Base: client forms) before this template is sent. Do not send this placeholder to a client.",
    "bodyHash": "a4ba7ebeefe1060a34c3a1034f2c68e6616cf6938a40be47fdd2c904a6174ba7",
    "status": "sent",
    "sentAt": 1793728794220,
    "signatures": [],
    "createdAt": 1793728793040,
    "updatedAt": 1793728794220
   }
  }
 },
 "contract_countersign": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Contract countersigned.",
   "signatureId": 1,
   "contract": {
    "id": 1,
    "clientId": 1,
    "templateId": 2,
    "templateType": "authorization",
    "title": "P3 Authorization",
    "body": "[OWNER-REPLACE PLACEHOLDER -- Pentest Authorization]\n\nNo Knowledge Base / client-form record for this template was found when it was seeded, so no legal text has been invented for it. The owner must paste the approved Pentest Authorization text here (Knowledge Base: client forms) before this template is sent. Do not send this placeholder to a client.",
    "bodyHash": "a4ba7ebeefe1060a34c3a1034f2c68e6616cf6938a40be47fdd2c904a6174ba7",
    "status": "partially-signed",
    "sentAt": 1793728794220,
    "signatures": [
     {
      "id": 1,
      "contractId": 1,
      "signerRole": "owner",
      "signerName": "Owner P3",
      "signerUserId": 1,
      "signedAt": 1793728794727,
      "ip": "127.0.0.1",
      "documentHash": "a4ba7ebeefe1060a34c3a1034f2c68e6616cf6938a40be47fdd2c904a6174ba7"
     }
    ],
    "createdAt": 1793728793040,
    "updatedAt": 1793728794729
   }
  }
 },
 "assurance_scan_create": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Scan recorded.",
   "id": 1,
   "authorizationContractId": 1
  }
 },
 "assurance_scan_import": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Import finished.",
   "accepted": 1,
   "rejected": [],
   "rejectedCount": 0
  }
 },
 "assurance_scans_list": {
  "status": 200,
  "body": {
   "ok": true,
   "scans": [
    {
     "id": 1,
     "clientId": 1,
     "authorizationContractId": 1,
     "target": "203.0.113.0/24",
     "tool": "nmap",
     "scanDate": "2026-11-01",
     "summary": "baseline",
     "counts": {
      "critical": 0,
      "high": 0,
      "medium": 0,
      "low": 0,
      "informational": 0
     },
     "findings": [
      {
       "id": 1,
       "title": "Open port",
       "severity": "High",
       "host": "203.0.113.5",
       "detail": "ssh open"
      }
     ],
     "createdAt": 1793728795326,
     "updatedAt": 1793728795911,
     "clientName": "Phase3 Client LLC"
    }
   ]
  }
 },
 "assurance_disposal_start": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Disposal started.",
   "id": 1,
   "disposal": {
    "id": 1,
    "assetId": 1,
    "assetName": "P3 NAS",
    "assetIdentifier": "NAS-1",
    "asset": {
     "id": 1,
     "clientId": 1,
     "assignedUserId": null,
     "name": "P3 NAS",
     "type": "NAS",
     "identifier": "NAS-1",
     "reviewDueDate": "",
     "scope": "Client",
     "status": "In Service",
     "warrantyUntil": null,
     "warrantyExpires": null,
     "supportExpires": null,
     "qrCode": "SCA-DVQ-6PC-h7-hyYcVDyKJ4N",
     "notes": "",
     "archivedAt": null,
     "archivedBy": null,
     "createdAt": 1793728791944,
     "updatedAt": 1793728792056
    },
    "reason": "decommission",
    "method": "wipe",
    "state": "initiated",
    "initiatedBy": 1,
    "initiatedByName": "OwnerP3",
    "initiatedAt": 1793728796341,
    "attestation": "",
    "methodDetail": "",
    "attestedBy": null,
    "attestedByName": "",
    "attestedAt": null,
    "certCode": "",
    "certHash": "",
    "assetSnapshot": {
     "assetId": 1,
     "name": "P3 NAS",
     "type": "NAS",
     "identifier": "NAS-1",
     "clientId": 1,
     "scope": "Client",
     "statusBefore": "In Service"
    },
    "completedBy": null,
    "completedAt": null,
    "createdAt": 1793728796341,
    "updatedAt": 1793728796341
   }
  }
 },
 "assurance_disposal_attest": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Attestation recorded.",
   "id": 1,
   "disposal": {
    "id": 1,
    "assetId": 1,
    "assetName": "P3 NAS",
    "assetIdentifier": "NAS-1",
    "asset": {
     "id": 1,
     "clientId": 1,
     "assignedUserId": null,
     "name": "P3 NAS",
     "type": "NAS",
     "identifier": "NAS-1",
     "reviewDueDate": "",
     "scope": "Client",
     "status": "In Service",
     "warrantyUntil": null,
     "warrantyExpires": null,
     "supportExpires": null,
     "qrCode": "SCA-DVQ-6PC-h7-hyYcVDyKJ4N",
     "notes": "",
     "archivedAt": null,
     "archivedBy": null,
     "createdAt": 1793728791944,
     "updatedAt": 1793728792056
    },
    "reason": "decommission",
    "method": "wipe",
    "state": "attested",
    "initiatedBy": 1,
    "initiatedByName": "OwnerP3",
    "initiatedAt": 1793728796341,
    "attestation": "Drive wiped with a 3-pass overwrite and verified.",
    "methodDetail": "",
    "attestedBy": 1,
    "attestedByName": "OwnerP3",
    "attestedAt": 1793728796543,
    "certCode": "",
    "certHash": "",
    "assetSnapshot": {
     "assetId": 1,
     "name": "P3 NAS",
     "type": "NAS",
     "identifier": "NAS-1",
     "clientId": 1,
     "scope": "Client",
     "statusBefore": "In Service"
    },
    "completedBy": null,
    "completedAt": null,
    "createdAt": 1793728796341,
    "updatedAt": 1793728796543
   }
  }
 },
 "assurance_disposal_complete": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Disposal completed \u2014 certificate of destruction issued.",
   "id": 1,
   "certCode": "COD-8dBbDHJtf0h59I50",
   "disposal": {
    "id": 1,
    "assetId": 1,
    "assetName": "P3 NAS",
    "assetIdentifier": "NAS-1",
    "asset": {
     "id": 1,
     "clientId": 1,
     "assignedUserId": null,
     "name": "P3 NAS",
     "type": "NAS",
     "identifier": "NAS-1",
     "reviewDueDate": "",
     "scope": "Client",
     "status": "Retired",
     "warrantyUntil": null,
     "warrantyExpires": null,
     "supportExpires": null,
     "qrCode": "SCA-DVQ-6PC-h7-hyYcVDyKJ4N",
     "notes": "",
     "archivedAt": 1793728796955,
     "archivedBy": 1,
     "createdAt": 1793728791944,
     "updatedAt": 1793728796955
    },
    "reason": "decommission",
    "method": "wipe",
    "state": "completed",
    "initiatedBy": 1,
    "initiatedByName": "OwnerP3",
    "initiatedAt": 1793728796341,
    "attestation": "Drive wiped with a 3-pass overwrite and verified.",
    "methodDetail": "",
    "attestedBy": 1,
    "attestedByName": "OwnerP3",
    "attestedAt": 1793728796543,
    "certCode": "COD-8dBbDHJtf0h59I50",
    "certHash": "1d1e0807fb5c5afcdf1f584a420dde28e412fea1c93e7017be8f7490cf3f81f6",
    "assetSnapshot": {
     "assetId": 1,
     "name": "P3 NAS",
     "type": "NAS",
     "identifier": "NAS-1",
     "clientId": 1,
     "scope": "Client",
     "statusBefore": "In Service"
    },
    "completedBy": 1,
    "completedAt": 1793728796955,
    "createdAt": 1793728796341,
    "updatedAt": 1793728796955
   }
  }
 },
 "assurance_disposals_list": {
  "status": 200,
  "body": {
   "disposals": [
    {
     "id": 1,
     "assetId": 1,
     "assetName": "P3 NAS",
     "assetIdentifier": "NAS-1",
     "asset": {
      "id": 1,
      "clientId": 1,
      "assignedUserId": null,
      "name": "P3 NAS",
      "type": "NAS",
      "identifier": "NAS-1",
      "reviewDueDate": "",
      "scope": "Client",
      "status": "Retired",
      "warrantyUntil": null,
      "warrantyExpires": null,
      "supportExpires": null,
      "qrCode": "SCA-DVQ-6PC-h7-hyYcVDyKJ4N",
      "notes": "",
      "archivedAt": 1793728796955,
      "archivedBy": 1,
      "createdAt": 1793728791944,
      "updatedAt": 1793728796955
     },
     "reason": "decommission",
     "method": "wipe",
     "state": "completed",
     "initiatedBy": 1,
     "initiatedByName": "OwnerP3",
     "initiatedAt": 1793728796341,
     "attestation": "Drive wiped with a 3-pass overwrite and verified.",
     "methodDetail": "",
     "attestedBy": 1,
     "attestedByName": "OwnerP3",
     "attestedAt": 1793728796543,
     "certCode": "COD-8dBbDHJtf0h59I50",
     "certHash": "1d1e0807fb5c5afcdf1f584a420dde28e412fea1c93e7017be8f7490cf3f81f6",
     "assetSnapshot": {
      "assetId": 1,
      "name": "P3 NAS",
      "type": "NAS",
      "identifier": "NAS-1",
      "clientId": 1,
      "scope": "Client",
      "statusBefore": "In Service"
     },
     "completedBy": 1,
     "completedAt": 1793728796955,
     "createdAt": 1793728796341,
     "updatedAt": 1793728796955
    }
   ],
   "methods": [
    "wipe",
    "destroy",
    "resale",
    "return"
   ],
   "ok": true
  }
 },
 "assurance_firm_document_create": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Firm document saved.",
   "id": 1,
   "document": {
    "id": 1,
    "title": "P3 Firm Policy",
    "kind": "other",
    "issuer": "",
    "issuedDate": "",
    "expiresDate": "",
    "expiryState": "none",
    "daysLeft": null,
    "formFileId": null,
    "fileName": "",
    "note": "",
    "createdBy": 1,
    "createdAt": 1793728797477,
    "updatedAt": 1793728797477
   }
  }
 },
 "assurance_firm_documents_list": {
  "status": 200,
  "body": {
   "documents": [
    {
     "id": 1,
     "title": "P3 Firm Policy",
     "kind": "other",
     "issuer": "",
     "issuedDate": "",
     "expiresDate": "",
     "expiryState": "none",
     "daysLeft": null,
     "formFileId": null,
     "fileName": "",
     "note": "",
     "createdBy": 1,
     "createdAt": 1793728797477,
     "updatedAt": 1793728797477
    }
   ],
   "kinds": [
    "insurance",
    "coi",
    "license",
    "permit",
    "certification",
    "other"
   ],
   "ok": true
  }
 },
 "assurance_firm_document_delete": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Firm document removed.",
   "id": 1
  }
 },
 "assurance_kb_share_create": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Share link created \u2014 copy it now; the token is shown only once.",
   "id": 1,
   "token": "S5LBpPjWdVKa7PFu3K2Z-nwRs3CvcpWTtJI-jduXfUQ",
   "expiresAt": 1794333598579,
   "share": {
    "id": 1,
    "kbId": 2,
    "kbTitle": "P3 KB article",
    "state": "active",
    "revoked": false,
    "expiresAt": 1794333598579,
    "accessCount": 0,
    "lastAccessAt": null,
    "createdBy": 1,
    "createdByName": "OwnerP3",
    "createdAt": 1793728798579
   }
  }
 },
 "assurance_kb_shares_list": {
  "status": 200,
  "body": {
   "shares": [
    {
     "id": 1,
     "kbId": 2,
     "kbTitle": "P3 KB article",
     "state": "active",
     "revoked": false,
     "expiresAt": 1794333598579,
     "accessCount": 0,
     "lastAccessAt": null,
     "createdBy": 1,
     "createdByName": "OwnerP3",
     "createdAt": 1793728798579
    }
   ],
   "ok": true
  }
 },
 "assurance_kb_share_revoke": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Share revoked.",
   "id": 1,
   "share": {
    "id": 1,
    "kbId": 2,
    "kbTitle": "P3 KB article",
    "state": "revoked",
    "revoked": true,
    "expiresAt": 1794333598579,
    "accessCount": 0,
    "lastAccessAt": null,
    "createdBy": 1,
    "createdByName": "OwnerP3",
    "createdAt": 1793728798579
   }
  }
 },
 "engagement_create": {
  "status": 201,
  "body": {
   "ok": true,
   "id": 1,
   "message": "Engagement created.",
   "engagement": {
    "id": 1,
    "clientId": 1,
    "title": "Phase3 engagement",
    "scope": "External",
    "rulesOfEngagement": "Written authorization on file.",
    "status": "Planned",
    "startDate": "2026-11-05",
    "endDate": "2026-11-20",
    "reportPublished": false,
    "reportGeneratedAt": null,
    "hasSignedAuthorization": true,
    "createdAt": 1793728799489,
    "updatedAt": 1793728799489,
    "findings": []
   }
  }
 },
 "finding_create": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Finding created.",
   "id": 1
  }
 },
 "finding_status": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Finding status updated."
  }
 },
 "finding_edit": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Finding saved.",
   "id": 1
  }
 },
 "finding_evidence": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Evidence attached.",
   "id": 1,
   "sha256": "299cf5f4d9fa2f4cb00395b589697e346db557434bd9ed4108d15644d94cc666",
   "sizeBytes": 13
  }
 },
 "finding_template_create": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Finding template created.",
   "id": 1
  }
 },
 "finding_templates_list": {
  "status": 200,
  "body": {
   "ok": true,
   "templates": [
    {
     "id": 1,
     "title": "Default creds",
     "defaultSeverity": "Critical",
     "description": "d",
     "impact": "i",
     "remediation": "r",
     "category": "Auth",
     "createdAt": 1793728804136,
     "updatedAt": 1793728804136
    }
   ],
   "severities": [
    "Critical",
    "High",
    "Medium",
    "Low",
    "Informational"
   ]
  }
 },
 "finding_from_template": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Finding inserted from template.",
   "id": 2
  }
 },
 "finding_template_delete": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Finding template deleted."
  }
 },
 "engagement_report_document": {
  "status": 403,
  "body": {
   "ok": false,
   "authorized": false,
   "message": "No report has been generated for this engagement yet."
  }
 },
 "contract_create_second": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Contract created in draft.",
   "id": 2,
   "contract": {
    "id": 2,
    "clientId": 1,
    "templateId": 2,
    "templateType": "authorization",
    "title": "P3 Void Me",
    "body": "[OWNER-REPLACE PLACEHOLDER -- Pentest Authorization]\n\nNo Knowledge Base / client-form record for this template was found when it was seeded, so no legal text has been invented for it. The owner must paste the approved Pentest Authorization text here (Knowledge Base: client forms) before this template is sent. Do not send this placeholder to a client.",
    "bodyHash": "a4ba7ebeefe1060a34c3a1034f2c68e6616cf6938a40be47fdd2c904a6174ba7",
    "status": "draft",
    "sentAt": null,
    "signatures": [],
    "createdAt": 1793728805943,
    "updatedAt": 1793728805943
   }
  }
 },
 "contract_void_refusal": {
  "status": 403,
  "body": {
   "ok": false,
   "authorized": false,
   "message": "Voiding a contract requires confirmation."
  }
 },
 "contract_void": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Contract voided."
  }
 },
 "booking_admin_get": {
  "status": 200,
  "body": {
   "availability": [],
   "services": [],
   "bookings": [],
   "staffAvailabilityWarnings": [],
   "weekdayNote": "Weekday: Monday=0 .. Sunday=6; times are America/Chicago wall-clock, like the Schedule.",
   "ok": true
  }
 },
 "booking_availability_create": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Availability window saved.",
   "id": 1
  }
 },
 "booking_service_create": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Booking service saved.",
   "id": 1
  }
 },
 "booking_manage_refusal": {
  "status": 403,
  "body": {
   "ok": false,
   "authorized": false,
   "message": "Booking not found."
  }
 },
 "booking_availability_delete": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Availability window deleted.",
   "id": 1
  }
 },
 "booking_service_delete": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Booking service deleted.",
   "id": 1
  }
 },
 "client_note_delete": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Note deleted.",
   "id": 1
  }
 },
 "client_contact_delete": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Contact removed.",
   "id": 1
  }
 }
}"""

    private val parsed: JsonObject by lazy {
        Json.parseToJsonElement(ALL).jsonObject
    }

    /** The HTTP status the deployed backend returned for [name]. */
    fun status(name: String): Int =
        parsed.getValue(name).jsonObject.getValue("status").jsonPrimitive.int

    /** The response body text for [name], as served. */
    fun body(name: String): String =
        parsed.getValue(name).jsonObject.getValue("body").toString()

    /** The response body for [name], parsed. */
    fun bodyJson(name: String): JsonObject =
        parsed.getValue(name).jsonObject.getValue("body").jsonObject
}

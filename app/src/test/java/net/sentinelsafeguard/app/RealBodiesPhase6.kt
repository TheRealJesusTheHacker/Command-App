package net.sentinelsafeguard.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Phase 6 REAL response bodies, captured 2026-11-03 from a
 * rehearsal boot of the exact Phase 6 candidate bytes
 * (server.py sha256 4351a08a9264962b5e25dbc0f36bba2b014e95093b0d6a43b6484fa186d950e3,
 * Addendum 20 — the deployed production bytes), against a seeded
 * scratch database with loopback mock upstreams. Seed data is
 * fictional; no secrets appear (device tokens never appear in
 * these bodies). [ALL] is phase6-work/fixtures/real-bodies.json
 * verbatim, re-keyed from capture names to route names.
 */
object RealBodiesPhase6 {
    val ALL: String = """{
 "_meta": {
  "serverPySha256": "4351a08a9264962b5e25dbc0f36bba2b014e95093b0d6a43b6484fa186d950e3",
  "capturedFrom": "rehearsal boot of the exact Phase 6 candidate bytes (deployed production, Addendum 20)",
  "note": "Real bodies captured from the exact candidate bytes on a residue-zero scratch DB; upstreams are loopback mocks; seed data fictional; no secrets (device tokens never appear in these bodies)."
 },
 "bodies": {
  "GET /api/app/live-intel/threat-data (unauthenticated)": {
   "status": 401,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Sign-in required."
   }
  },
  "GET /api/app/live-intel/threat-data (flag off)": {
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Not authorized."
   }
  },
  "GET /api/app/osint (flag off)": {
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Not authorized."
   }
  },
  "GET /api/app/breach-intel (flag off)": {
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Not authorized."
   }
  },
  "GET /api/app/threat-intel (flag off)": {
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Not authorized."
   }
  },
  "GET /api/app/news/feed (flag off)": {
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Not authorized."
   }
  },
  "GET /api/app/news/items (flag off)": {
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Not authorized."
   }
  },
  "GET /api/app/reviews (flag off)": {
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Not authorized."
   }
  },
  "GET /api/app/law (flag off)": {
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Not authorized."
   }
  },
  "GET /api/app/live-intel/threat-data": {
   "status": 200,
   "body": {
    "connected": true,
    "stale": false,
    "data": {
     "marker": "phase6-threat",
     "attacks": 42
    },
    "ok": true
   }
  },
  "GET /api/app/live-intel/geo-data": {
   "status": 200,
   "body": {
    "connected": true,
    "stale": false,
    "data": {
     "marker": "phase6-geo",
     "hotspots": [
      "Seed Hotspot"
     ]
    },
    "ok": true
   }
  },
  "GET /api/app/live-intel/flights": {
   "status": 200,
   "body": {
    "connected": true,
    "stale": false,
    "data": {
     "marker": "phase6-flights",
     "flights": [
      {
       "tail": "NSEED6"
      }
     ]
    },
    "ok": true
   }
  },
  "GET /api/app/live-intel/flight-lookup": {
   "status": 200,
   "body": {
    "connected": true,
    "query": "AA100",
    "stale": false,
    "result": {
     "flight": "AA100",
     "status": "Seed airborne"
    },
    "error": null,
    "keyMissing": false,
    "ok": true
   }
  },
  "GET /api/app/live-intel/flight-lookup (invalid)": {
   "status": 200,
   "body": {
    "connected": true,
    "query": "",
    "result": null,
    "keyMissing": false,
    "stale": false,
    "error": "Enter a flight number (e.g. AA100, UAL512, SWA204).",
    "ok": true
   }
  },
  "GET /api/app/osint (empty)": {
   "status": 200,
   "body": {
    "ok": true,
    "types": [
     {
      "key": "name",
      "label": "Full name"
     },
     {
      "key": "username",
      "label": "Username / handle"
     },
     {
      "key": "email",
      "label": "Email address"
     },
     {
      "key": "phone",
      "label": "Phone number"
     },
     {
      "key": "address",
      "label": "Street address"
     },
     {
      "key": "domain",
      "label": "Domain"
     },
     {
      "key": "ip",
      "label": "IP address"
     },
     {
      "key": "plate",
      "label": "License plate"
     },
     {
      "key": "vin",
      "label": "VIN"
     },
     {
      "key": "tail",
      "label": "Aircraft tail number"
     },
     {
      "key": "callsign",
      "label": "Amateur radio callsign"
     }
    ],
    "checklists": {
     "name": [
      "Confirm spelling variants and maiden/married names.",
      "Pull the FEC + court findings into the case file with their source links.",
      "Run the people-search links by hand and screenshot anything material.",
      "Check the state court portal and DOC locator from the route pack.",
      "Search the subject's known city + name in the news index weekly (watchlist)."
     ],
     "username": [
      "Open each located profile and note public bio, avatar reuse, and join dates.",
      "Reverse-image-search public avatars (by hand) for cross-platform reuse.",
      "Check the same handle on the login-walled platforms from the manual links.",
      "Record which profiles are confirmed the subject's vs. same-handle strangers."
     ],
     "email": [
      "Note every breach corpus the address appears in; check the dates.",
      "Check the address by hand at haveibeenpwned.com for the full breach list.",
      "Try the address's local part as a username search.",
      "Look for the address in the dork-pack results (data-broker and paste sites)."
     ],
     "phone": [
      "Run the reverse-phone people-search links by hand.",
      "Text/callback verification is an investigative step — not part of this report.",
      "Check the number's area-code region against the subject's known addresses.",
      "Search the number in quotes with 'scam' and 'spam' (dork pack)."
     ],
     "address": [
      "Pull the county assessor / parcel record for the address (route pack).",
      "Check the address in the Wayback/news results for prior occupants or incidents.",
      "Confirm residency with a second source before treating it as the subject's."
     ],
     "domain": [
      "Review crt.sh subdomains for exposed panels (admin, vpn, mail).",
      "Compare the Wayback history for ownership changes.",
      "Check the RDAP expiry — expiring domains get impersonated."
     ],
     "ip": [
      "Confirm the allocation holder in RDAP before attributing anything to it.",
      "Check whether the address is a shared/VPN exit — attribution stops there."
     ],
     "plate": [
      "Plate-to-owner data is restricted (DPPA) — pursue it only under a permissible purpose through official channels.",
      "If a VIN is obtained lawfully, run the VIN module for the vehicle decode."
     ],
     "vin": [
      "Compare the decoded build data against the vehicle's observed features.",
      "Run the NICB VINCheck total-loss check by hand (link in the report)."
     ],
     "tail": [
      "Pull the FAA record; note the registrant type (individual vs. LLC/trust).",
      "Check the tail in the dork pack for incident and sale history."
     ],
     "callsign": [
      "Note the license class and address locality from the FCC record.",
      "Check the callsign on QRZ by hand for the public bio page."
     ]
    },
    "dorkTemplates": {
     "name": [
      "\"{v}\"",
      "\"{v}\" {city} {state}",
      "\"{v}\" obituary",
      "\"{v}\" linkedin OR facebook OR instagram",
      "\"{v}\" arrest OR charged OR court",
      "\"{v}\" resume OR CV"
     ],
     "username": [
      "\"{v}\"",
      "site:github.com \"{v}\"",
      "site:reddit.com \"{v}\"",
      "\"{v}\" profile",
      "inurl:{v}",
      "\"{v}\" forum OR community"
     ],
     "email": [
      "\"{v}\"",
      "\"{v}\" breach OR leak",
      "\"{v}\" contact",
      "\"{v}\" site:linkedin.com",
      "\"{v}\" password OR account"
     ],
     "phone": [
      "\"{v}\"",
      "\"{v}\" scam OR spam",
      "\"{v}\" text OR call",
      "\"{v}\" business OR company"
     ],
     "address": [
      "\"{v}\"",
      "\"{v}\" property records",
      "\"{v}\" sold",
      "\"{v}\" owner OR parcel"
     ],
     "domain": [
      "site:{v}",
      "\"{v}\" login",
      "site:{v} admin OR portal",
      "\"{v}\" breach OR hacked"
     ],
     "ip": [
      "\"{v}\"",
      "\"{v}\" abuse OR blacklist",
      "\"{v}\" server"
     ],
     "plate": [
      "\"{v}\" plate",
      "\"{v}\" vehicle"
     ],
     "vin": [
      "\"{v}\"",
      "\"{v}\" sale OR auction OR history"
     ],
     "tail": [
      "\"{v}\" aircraft",
      "\"{v}\" flight OR crash OR incident"
     ],
     "callsign": [
      "\"{v}\" radio",
      "\"{v}\" ham OR amateur OR qrz"
     ]
    },
    "searches": [],
    "reports": [],
    "watches": [],
    "cases": [],
    "clients": []
   }
  },
  "GET /api/app/osint/detect": {
   "status": 200,
   "body": {
    "ok": true,
    "candidates": [
     "email"
    ],
    "types": [
     "name",
     "username",
     "email",
     "phone",
     "address",
     "domain",
     "ip",
     "plate",
     "vin",
     "tail",
     "callsign"
    ]
   }
  },
  "POST /api/app/osint/searches (no purpose)": {
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "A purpose note is required before any OSINT search runs — one short line (e.g. the case reference) is enough."
   }
  },
  "POST /api/app/osint/searches": {
   "status": 201,
   "body": {
    "ok": true,
    "searchId": 1
   }
  },
  "GET /api/app/osint/reports/{id}": {
   "status": 200,
   "body": {
    "ok": true,
    "report": {
     "id": 1,
     "searchId": 1,
     "title": "Capture Seed Report",
     "subject": "seed@example.invalid",
     "summary": "Seed summary",
     "caseId": null,
     "clientId": null,
     "createdAt": 1793749633968,
     "createdByName": "OwnerCap6",
     "body": "Seed body",
     "payload": {}
    }
   }
  },
  "POST /api/app/osint/watches": {
   "status": 201,
   "body": {
    "ok": true,
    "watchId": 1
   }
  },
  "POST /api/app/osint/watches/{id}/run-now": {
   "status": 200,
   "body": {
    "ok": true,
    "searchId": 2
   }
  },
  "GET /api/app/osint": {
   "status": 200,
   "body": {
    "ok": true,
    "types": [
     {
      "key": "name",
      "label": "Full name"
     },
     {
      "key": "username",
      "label": "Username / handle"
     },
     {
      "key": "email",
      "label": "Email address"
     },
     {
      "key": "phone",
      "label": "Phone number"
     },
     {
      "key": "address",
      "label": "Street address"
     },
     {
      "key": "domain",
      "label": "Domain"
     },
     {
      "key": "ip",
      "label": "IP address"
     },
     {
      "key": "plate",
      "label": "License plate"
     },
     {
      "key": "vin",
      "label": "VIN"
     },
     {
      "key": "tail",
      "label": "Aircraft tail number"
     },
     {
      "key": "callsign",
      "label": "Amateur radio callsign"
     }
    ],
    "checklists": {
     "name": [
      "Confirm spelling variants and maiden/married names.",
      "Pull the FEC + court findings into the case file with their source links.",
      "Run the people-search links by hand and screenshot anything material.",
      "Check the state court portal and DOC locator from the route pack.",
      "Search the subject's known city + name in the news index weekly (watchlist)."
     ],
     "username": [
      "Open each located profile and note public bio, avatar reuse, and join dates.",
      "Reverse-image-search public avatars (by hand) for cross-platform reuse.",
      "Check the same handle on the login-walled platforms from the manual links.",
      "Record which profiles are confirmed the subject's vs. same-handle strangers."
     ],
     "email": [
      "Note every breach corpus the address appears in; check the dates.",
      "Check the address by hand at haveibeenpwned.com for the full breach list.",
      "Try the address's local part as a username search.",
      "Look for the address in the dork-pack results (data-broker and paste sites)."
     ],
     "phone": [
      "Run the reverse-phone people-search links by hand.",
      "Text/callback verification is an investigative step — not part of this report.",
      "Check the number's area-code region against the subject's known addresses.",
      "Search the number in quotes with 'scam' and 'spam' (dork pack)."
     ],
     "address": [
      "Pull the county assessor / parcel record for the address (route pack).",
      "Check the address in the Wayback/news results for prior occupants or incidents.",
      "Confirm residency with a second source before treating it as the subject's."
     ],
     "domain": [
      "Review crt.sh subdomains for exposed panels (admin, vpn, mail).",
      "Compare the Wayback history for ownership changes.",
      "Check the RDAP expiry — expiring domains get impersonated."
     ],
     "ip": [
      "Confirm the allocation holder in RDAP before attributing anything to it.",
      "Check whether the address is a shared/VPN exit — attribution stops there."
     ],
     "plate": [
      "Plate-to-owner data is restricted (DPPA) — pursue it only under a permissible purpose through official channels.",
      "If a VIN is obtained lawfully, run the VIN module for the vehicle decode."
     ],
     "vin": [
      "Compare the decoded build data against the vehicle's observed features.",
      "Run the NICB VINCheck total-loss check by hand (link in the report)."
     ],
     "tail": [
      "Pull the FAA record; note the registrant type (individual vs. LLC/trust).",
      "Check the tail in the dork pack for incident and sale history."
     ],
     "callsign": [
      "Note the license class and address locality from the FCC record.",
      "Check the callsign on QRZ by hand for the public bio page."
     ]
    },
    "dorkTemplates": {
     "name": [
      "\"{v}\"",
      "\"{v}\" {city} {state}",
      "\"{v}\" obituary",
      "\"{v}\" linkedin OR facebook OR instagram",
      "\"{v}\" arrest OR charged OR court",
      "\"{v}\" resume OR CV"
     ],
     "username": [
      "\"{v}\"",
      "site:github.com \"{v}\"",
      "site:reddit.com \"{v}\"",
      "\"{v}\" profile",
      "inurl:{v}",
      "\"{v}\" forum OR community"
     ],
     "email": [
      "\"{v}\"",
      "\"{v}\" breach OR leak",
      "\"{v}\" contact",
      "\"{v}\" site:linkedin.com",
      "\"{v}\" password OR account"
     ],
     "phone": [
      "\"{v}\"",
      "\"{v}\" scam OR spam",
      "\"{v}\" text OR call",
      "\"{v}\" business OR company"
     ],
     "address": [
      "\"{v}\"",
      "\"{v}\" property records",
      "\"{v}\" sold",
      "\"{v}\" owner OR parcel"
     ],
     "domain": [
      "site:{v}",
      "\"{v}\" login",
      "site:{v} admin OR portal",
      "\"{v}\" breach OR hacked"
     ],
     "ip": [
      "\"{v}\"",
      "\"{v}\" abuse OR blacklist",
      "\"{v}\" server"
     ],
     "plate": [
      "\"{v}\" plate",
      "\"{v}\" vehicle"
     ],
     "vin": [
      "\"{v}\"",
      "\"{v}\" sale OR auction OR history"
     ],
     "tail": [
      "\"{v}\" aircraft",
      "\"{v}\" flight OR crash OR incident"
     ],
     "callsign": [
      "\"{v}\" radio",
      "\"{v}\" ham OR amateur OR qrz"
     ]
    },
    "searches": [
     {
      "id": 2,
      "kind": "identifier",
      "subject": "domain: seed.example.invalid",
      "purpose": "Capture watch",
      "status": "queued",
      "failureNote": "",
      "reportId": null,
      "caseId": null,
      "clientId": null,
      "watchId": 1,
      "requestedBy": 1,
      "requestedByName": "OwnerCap6",
      "createdAt": 1793749637952,
      "completedAt": null
     },
     {
      "id": 1,
      "kind": "identifier",
      "subject": "email: seed@example.invalid",
      "purpose": "Capture seed",
      "status": "queued",
      "failureNote": "",
      "reportId": null,
      "caseId": null,
      "clientId": null,
      "watchId": null,
      "requestedBy": 1,
      "requestedByName": "OwnerCap6",
      "createdAt": 1793749637351,
      "completedAt": null
     }
    ],
    "reports": [
     {
      "id": 1,
      "searchId": 1,
      "title": "Capture Seed Report",
      "subject": "seed@example.invalid",
      "summary": "Seed summary",
      "caseId": null,
      "clientId": null,
      "createdAt": 1793749633968,
      "createdByName": "OwnerCap6"
     }
    ],
    "watches": [
     {
      "id": 1,
      "label": "Seed watch",
      "kind": "identifier",
      "purpose": "Capture watch",
      "active": true,
      "caseId": null,
      "clientId": null,
      "lastRunAt": null,
      "nextRunAt": 1794354437734,
      "lastReportId": null,
      "changeSummary": "",
      "createdAt": 1793749637734
     }
    ],
    "cases": [],
    "clients": []
   }
  },
  "DELETE /api/app/osint/watches/{id}": {
   "status": 200,
   "body": {
    "ok": true
   }
  },
  "DELETE /api/app/osint/reports/{id}": {
   "status": 200,
   "body": {
    "ok": true
   }
  },
  "GET /api/app/breach-intel (empty)": {
   "status": 200,
   "body": {
    "ok": true,
    "demo": false,
    "boundaryCopy": "Circulation intelligence records ONLY what reputable reporting says about where data surfaced. Never enter a link to the leak or download itself, and never fetch or store leaked data — venue links are never stored; link the reporting. If a detail might cross this line, record it coarser (\"a dark-web forum, per <source>\").",
    "venueTypes": [
     "ransomware leak site",
     "dark-web forum",
     "marketplace listing",
     "Telegram channel",
     "public file host",
     "paste site",
     "not yet circulating",
     "circulating (venue unspecified)"
    ],
    "circStatuses": [
     "claimed",
     "for sale",
     "published freely",
     "held privately",
     "not circulating"
    ],
    "dataClasses": [
     "credentials",
     "SSN / government IDs",
     "financial",
     "health / medical",
     "email addresses",
     "phone numbers",
     "physical addresses",
     "dates of birth",
     "payment cards",
     "other personal data"
    ],
    "sectors": [
     "healthcare",
     "finance",
     "retail",
     "technology",
     "education",
     "government",
     "hospitality",
     "energy / utilities",
     "legal",
     "manufacturing",
     "nonprofit",
     "other",
     "unknown"
    ],
    "stats": {
     "month": "2026-11",
     "breachesThisMonth": 0,
     "recordsThisMonth": 0,
     "circulatingPublicly": 0,
     "totalBreaches": 0
    },
    "breaches": [],
    "detail": null,
    "intakeRuns": [
     {
      "id": 1,
      "status": "queued",
      "added": 0,
      "updated": 0,
      "skipped": 0,
      "note": "scheduled daily intake",
      "createdAt": 1793749634032,
      "finishedAt": null
     }
    ]
   }
  },
  "POST /api/app/breach-intel": {
   "status": 201,
   "body": {
    "ok": true,
    "breachId": 1
   }
  },
  "GET /api/app/breach-intel?sector=technology": {
   "status": 200,
   "body": {
    "ok": true,
    "demo": false,
    "boundaryCopy": "Circulation intelligence records ONLY what reputable reporting says about where data surfaced. Never enter a link to the leak or download itself, and never fetch or store leaked data — venue links are never stored; link the reporting. If a detail might cross this line, record it coarser (\"a dark-web forum, per <source>\").",
    "venueTypes": [
     "ransomware leak site",
     "dark-web forum",
     "marketplace listing",
     "Telegram channel",
     "public file host",
     "paste site",
     "not yet circulating",
     "circulating (venue unspecified)"
    ],
    "circStatuses": [
     "claimed",
     "for sale",
     "published freely",
     "held privately",
     "not circulating"
    ],
    "dataClasses": [
     "credentials",
     "SSN / government IDs",
     "financial",
     "health / medical",
     "email addresses",
     "phone numbers",
     "physical addresses",
     "dates of birth",
     "payment cards",
     "other personal data"
    ],
    "sectors": [
     "healthcare",
     "finance",
     "retail",
     "technology",
     "education",
     "government",
     "hospitality",
     "energy / utilities",
     "legal",
     "manufacturing",
     "nonprofit",
     "other",
     "unknown"
    ],
    "stats": {
     "month": "2026-11",
     "breachesThisMonth": 0,
     "recordsThisMonth": 0,
     "circulatingPublicly": 0,
     "totalBreaches": 1
    },
    "breaches": [
     {
      "id": 1,
      "org": "Capture Fictional Breach Co",
      "sector": "technology",
      "incidentDate": "2026-10-01",
      "disclosureDate": "2026-10-15",
      "recordsAffected": 123,
      "dataClasses": [
       "email addresses"
      ],
      "attackVector": "Seed vector",
      "summary": "Fictional seed breach.",
      "severity": "Low",
      "origin": "manual",
      "createdByName": "OwnerCap6",
      "createdAt": 1793749639170,
      "updatedAt": 1793749639170,
      "circStatuses": []
     }
    ],
    "detail": null,
    "intakeRuns": [
     {
      "id": 1,
      "status": "queued",
      "added": 0,
      "updated": 0,
      "skipped": 0,
      "note": "scheduled daily intake",
      "createdAt": 1793749634032,
      "finishedAt": null
     }
    ]
   }
  },
  "GET /api/app/breach-intel?breachId={id}": {
   "status": 200,
   "body": {
    "ok": true,
    "demo": false,
    "boundaryCopy": "Circulation intelligence records ONLY what reputable reporting says about where data surfaced. Never enter a link to the leak or download itself, and never fetch or store leaked data — venue links are never stored; link the reporting. If a detail might cross this line, record it coarser (\"a dark-web forum, per <source>\").",
    "venueTypes": [
     "ransomware leak site",
     "dark-web forum",
     "marketplace listing",
     "Telegram channel",
     "public file host",
     "paste site",
     "not yet circulating",
     "circulating (venue unspecified)"
    ],
    "circStatuses": [
     "claimed",
     "for sale",
     "published freely",
     "held privately",
     "not circulating"
    ],
    "dataClasses": [
     "credentials",
     "SSN / government IDs",
     "financial",
     "health / medical",
     "email addresses",
     "phone numbers",
     "physical addresses",
     "dates of birth",
     "payment cards",
     "other personal data"
    ],
    "sectors": [
     "healthcare",
     "finance",
     "retail",
     "technology",
     "education",
     "government",
     "hospitality",
     "energy / utilities",
     "legal",
     "manufacturing",
     "nonprofit",
     "other",
     "unknown"
    ],
    "stats": {
     "month": "2026-11",
     "breachesThisMonth": 0,
     "recordsThisMonth": 0,
     "circulatingPublicly": 0,
     "totalBreaches": 1
    },
    "breaches": [
     {
      "id": 1,
      "org": "Capture Fictional Breach Co",
      "sector": "technology",
      "incidentDate": "2026-10-01",
      "disclosureDate": "2026-10-15",
      "recordsAffected": 123,
      "dataClasses": [
       "email addresses"
      ],
      "attackVector": "Seed vector",
      "summary": "Fictional seed breach.",
      "severity": "Low",
      "origin": "manual",
      "createdByName": "OwnerCap6",
      "createdAt": 1793749639170,
      "updatedAt": 1793749639170,
      "circStatuses": []
     }
    ],
    "detail": {
     "id": 1,
     "org": "Capture Fictional Breach Co",
     "sector": "technology",
     "incidentDate": "2026-10-01",
     "disclosureDate": "2026-10-15",
     "recordsAffected": 123,
     "dataClasses": [
      "email addresses"
     ],
     "attackVector": "Seed vector",
     "summary": "Fictional seed breach.",
     "severity": "Low",
     "origin": "manual",
     "createdByName": "OwnerCap6",
     "createdAt": 1793749639170,
     "updatedAt": 1793749639170,
     "sources": [
      {
       "id": 1,
       "label": "Seed reporting",
       "url": "https://example.invalid/report",
       "kind": "report"
      }
     ],
     "circulation": []
    },
    "intakeRuns": [
     {
      "id": 1,
      "status": "queued",
      "added": 0,
      "updated": 0,
      "skipped": 0,
      "note": "scheduled daily intake",
      "createdAt": 1793749634032,
      "finishedAt": null
     }
    ]
   }
  },
  "PATCH /api/app/breach-intel/{id}": {
   "status": 200,
   "body": {
    "ok": true,
    "breachId": 1
   }
  },
  "POST /api/app/breach-intel/{id}/circulation (link refusal)": {
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Venue links are never stored. Record the venue NAME as reported, and cite the REPORTING about it as the intel source."
   }
  },
  "POST /api/app/breach-intel/{id}/circulation": {
   "status": 201,
   "body": {
    "ok": true,
    "circulationId": 1
   }
  },
  "DELETE /api/app/breach-intel/circulation/{id}": {
   "status": 200,
   "body": {
    "ok": true
   }
  },
  "POST /api/app/breach-intel/intake-run": {
   "status": 200,
   "body": {
    "ok": true,
    "runId": 2
   }
  },
  "DELETE /api/app/breach-intel/{id}": {
   "status": 200,
   "body": {
    "ok": true
   }
  },
  "GET /api/app/threat-intel": {
   "status": 200,
   "body": {
    "ok": true,
    "demo": false,
    "items": [
     {
      "id": 52,
      "title": "Capture Fictional CVE Item",
      "summary": "Seed summary",
      "url": "https://example.invalid/cve",
      "category": "CVE / Vulnerability",
      "severity": "Critical",
      "score": 90,
      "cveIds": [
       "CVE-2026-0001"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": null,
      "publishedAt": 1793749633968,
      "firstSeenAt": 1793749633968,
      "lastSeenAt": 1793749633968,
      "primarySource": "Seed Source",
      "sources": [],
      "newsItemId": null
     },
     {
      "id": 1,
      "title": "WordPress WordPress Core Time-of-check Time-of-use (TOCTOU) Race Condition Vulnerability — CVE-2026-119977",
      "summary": "WordPress WordPress Core contains a time-of-check time-of-use (TOCTOU) race condition vulnerability that could allow unauthenticated attackers to execute code on the server during the upgrade window. This is due to predictable, web-accessible staging paths used during full-package core upgrades. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 55,
      "cveIds": [
       "CVE-2026-119977"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1793923200000,
      "publishedAt": 1793664000000,
      "firstSeenAt": 1793749638207,
      "lastSeenAt": 1793749638207,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 2,
      "title": "Ivanti Connect Secure Generation of Predictable Numbers or Identifiers Vulnerability — CVE-2026-118527",
      "summary": "Ivanti Connect Secure contains a generation of predictable numbers or identifiers vulnerability that allows a remote authenticated attacker to upload files with attacker-controlled content to a known location on disk. This vulnerability can be chained with CVE-2026-104935 and CVE-2026-111357. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 55,
      "cveIds": [
       "CVE-2026-118527"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1794528000000,
      "publishedAt": 1793318400000,
      "firstSeenAt": 1793749638208,
      "lastSeenAt": 1793749638208,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 6,
      "title": "Broadcom VMWare ESX Insufficient Logging Vulnerability — CVE-2026-41709",
      "summary": "Broadcom VMWare ESX contains an insufficient logging vulnerability that could allow a malicious actor with admin access to perform certain operations without being logged. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 55,
      "cveIds": [
       "CVE-2026-41709"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1794355200000,
      "publishedAt": 1793145600000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 8,
      "title": "Adobe Commerce and Magento Unrestricted Upload of File with Dangerous Type Vulnerability — CVE-2026-48356",
      "summary": "Adobe Commerce and Magento contain an unrestricted upload of file with dangerous type vulnerability that could result in arbitrary code execution in the context of the current user, potentially gaining elevated access or control over the victim's account or session. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 55,
      "cveIds": [
       "CVE-2026-48356"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1794268800000,
      "publishedAt": 1793059200000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 9,
      "title": "Google Brotli Unchecked Return Value Vulnerability — CVE-2026-111497",
      "summary": "Google Brotli contains an unchecked return value vulnerability that allows a remote unauthenticated attacker to corrupt adjacent decoder state memory, disclose sensitive memory contents, cause a denial of service, or potentially execute arbitrary code using a crafted Brotli-compressed stream. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 55,
      "cveIds": [
       "CVE-2026-111497"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1794268800000,
      "publishedAt": 1793059200000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 10,
      "title": "PHP Memory Corruption Vulnerability — CVE-2026-109618",
      "summary": "PHP contains an out-of-memory corruption vulnerability that allows for unauthenticated remote code execution. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 53,
      "cveIds": [
       "CVE-2026-109618"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1793232000000,
      "publishedAt": 1792972800000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 11,
      "title": "PHP Stale Internal Request-State Vulnerability — CVE-2026-109619",
      "summary": "PHP contains a stale internal request-state vulnerability that allows for unauthenticated remote code execution. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 53,
      "cveIds": [
       "CVE-2026-109619"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1793232000000,
      "publishedAt": 1792972800000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 13,
      "title": "Collabora Online – Built-in CODE Server OS Command Injection Vulnerability — CVE-2026-110447",
      "summary": "Collabora Collabora Online – Built-in CODE Server when used within the NextCloud platform contains an OS command injection vulnerability that allows for remote code execution when the CODE server isn't running. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 53,
      "cveIds": [
       "CVE-2026-110447"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792972800000,
      "publishedAt": 1792713600000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 31,
      "title": "Strapi Cleartext Storage of Sensitive Information Vulnerability — CVE-2023-22894",
      "summary": "Strapi contains a cleartext storage of sensitive information vulnerability that could allow attackers with access to the admin panel to discover sensitive user details via the query filter. The impacted product(s) could be end-of-life (EoL) and/or end-of-service (EoS). Users are advised to discontinue use and/or transition to a supported version. This vulnerability can be chained with CVE-2023-22621 to achieve remote code execution.  Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 53,
      "cveIds": [
       "CVE-2023-22894"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1791676800000,
      "publishedAt": 1791417600000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 32,
      "title": "ONLYOFFICE Docs Server Path Traversal Vulnerability — CVE-2021-3199",
      "summary": "ONLYOFFICE Docs contains a path traversal vulnerability that can occur when JWT is used, via a /.. sequence in an image upload parameter and could allow for remote code execution. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 53,
      "cveIds": [
       "CVE-2021-3199"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1791676800000,
      "publishedAt": 1791417600000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 36,
      "title": "Zammad GmbH Zammad Session Fixation Vulnerability — CVE-2026-102489",
      "summary": "Zammad GmbH Zammad contains a session fixation vulnerability that can lead to remote code execution as the zammad user. This vulnerability can be chained with CVE-2026-102490. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 53,
      "cveIds": [
       "CVE-2026-102489"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1791158400000,
      "publishedAt": 1790899200000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 40,
      "title": "Citrix NetScaler Improper Restriction of Operations within the Bounds of a Memory Buffer Vulnerability — CVE-2026-88772",
      "summary": "Citrix NetScaler ADC and NetScaler Gateway contain an improper restriction of operations within the bounds of a memory buffer vulnerability that could allow for remote code execution or denial of service Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 53,
      "cveIds": [
       "CVE-2026-88772"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790726400000,
      "publishedAt": 1790467200000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 44,
      "title": "WordPress Core Remote File Inclusion Vulnerability — CVE-2026-87902",
      "summary": "WordPress Core contains a remote file inclusion vulnerability which could allow an unauthenticated attacker to make page-template resolution include a chosen readable local `.php` file outside the active theme directories, leading to remote code execution. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 53,
      "cveIds": [
       "CVE-2026-87902"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790553600000,
      "publishedAt": 1790294400000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 45,
      "title": "WSO2 Multiple Products Path Traversal Vulnerability  — CVE-2026-5430",
      "summary": "WSO2 API Control Plane, API Manager, Traffic Manager & Universal Gateway contain a path traversal vulnerability that could allow for unrestricted file upload and lead to remote code execution.  Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 53,
      "cveIds": [
       "CVE-2026-5430"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790467200000,
      "publishedAt": 1790208000000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 48,
      "title": "F5 BIG-IP APM Heap-based Buffer Overflow Vulnerability — CVE-2026-94127",
      "summary": "F5 BIG-IP APM contains a heap-based buffer overflow vulnerability when access policy and an OAuth profile are configured on a virtual server. This vulnerability could allow an unauthenticated attacker to perform remote code execution. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 53,
      "cveIds": [
       "CVE-2026-94127"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790294400000,
      "publishedAt": 1790035200000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 3,
      "title": "Ivanti Connect Secure Missing Authentication for Critical Function Vulnerability — CVE-2026-111357",
      "summary": "Ivanti Connect Secure contains a missing authentication for critical function vulnerability that could allow a local authenticated attacker to escalate their privileges. This vulnerability can be chained with CVE-2026-104935 and CVE-2026-118527. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 51,
      "cveIds": [
       "CVE-2026-111357"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1793577600000,
      "publishedAt": 1793318400000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 4,
      "title": "GitLab Community Edition and Enterprise Edition Integer Overflow or Wraparound Vulnerability — CVE-2026-97229",
      "summary": "GitLab Community Edition and Enterprise Edition contain a integer overflow or wraparound vulnerability that could allow an unauthenticated user to execute arbitrary code on the GitLab server via a specially crafted regular expression in a CI/CD configuration, due to insufficient regular expression validation. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-97229"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1793491200000,
      "publishedAt": 1793232000000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 5,
      "title": "Cisco Secure Firewall ASA Software and Cisco Secure FTD Software Unchecked Return Value to NULL Pointer Dereference Vulnerability — CVE-2026-76418",
      "summary": "Cisco Secure Firewall ASA Software and Cisco Secure FTD Software contains an unchecked return value to NULL pointer dereference vulnerability that could allow an unauthenticated, remote attacker to cause the device to reload unexpectedly, resulting in a denial of service (DoS) condition. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-76418"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1793491200000,
      "publishedAt": 1793232000000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 7,
      "title": "Linux Kernel Missing Initialization of a Variable Vulnerability — CVE-2026-46300",
      "summary": "Linux Kernel contains a missing initialization of a variable vulnerability that could allow for unintended modification of page-cache-backed data via failure to preserve the shared-fragment marker during socket buffer coalescing, causing ESP input processing to decrypt shared fragments in place. This vulnerability can be chained with CVE-2026-109618 and CVE-2026-109619 to implant PHP webshells and run commands as root. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-46300"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1793404800000,
      "publishedAt": 1793145600000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 12,
      "title": "Arista VeloCloud Orchestrator SQL Injection Vulnerability — CVE-2026-114625",
      "summary": "Arista VeloCloud Orchestrator on-prem contains a SQL injection vulnerability that may allow a remote attacker to access privileged internal functionality and impact the VCO host. Successful exploitation may compromise the confidentiality, integrity, and availability of the orchestrator and data managed by the orchestrator. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-114625"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792972800000,
      "publishedAt": 1792713600000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 14,
      "title": "EG Secure Solutions SiteGuard Server Edition OS Command Injection Vulnerability — CVE-2026-106931",
      "summary": "EG Secure Solutions SiteGuard Server Edition contains an OS command injection vulnerability which may allow an attacker to execute arbitrary OS commands with root or SYSTEM privileges on the system running the affected product. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-106931"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792713600000,
      "publishedAt": 1792454400000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 15,
      "title": "Plex Media Server External Control of File Name or Path Vulnerability — CVE-2026-96656",
      "summary": "Plex Media Server contains an external control of file name or path vulnerability which can allow an admin user to write arbitrary files that may be executed on load. This vulnerability can be chained with CVE-2026-96652, CVE-2026-96654, and CVE-2026-96655. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-96656"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792713600000,
      "publishedAt": 1792454400000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 16,
      "title": "Plex Media Server Server-Side Request Forgery (SSRF) Vulnerability — CVE-2026-96655",
      "summary": "Plex Media Server contains a server-side request forgery (SSRF) vulnerability that could allow an authenticated user to request arbitrary internal or external addresses via the '/video/:/transcode/universal' path parameter. This vulnerability can be chained with CVE-2026-96652, CVE-2026-96654, and CVE-2026-96656. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-96655"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1793664000000,
      "publishedAt": 1792454400000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 17,
      "title": "Plex Media Server Improper Neutralization of Encoded URI Schemes in a Web Page Vulnerability — CVE-2026-96654",
      "summary": "Plex Media Server contains an improper neutralization of encoded URI schemes in a web page vulnerability that could allow an attacker to call other plugins' functions and supply their own parameters. This vulnerability can be chained with CVE-2026-96652, CVE-2026-96655, and CVE-2026-96656. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-96654"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792713600000,
      "publishedAt": 1792454400000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 18,
      "title": "Plex Media Server Server-Side Request Forgery (SSRF) Vulnerability — CVE-2026-96652",
      "summary": "Plex Media Server contains a server-side request forgery (SSRF) vulnerability which could allow an attacker using any X-Plex-Token value to include a full URL in the 'protocol' parameter and force the Plex server to POST to the attacker's chosen destination. This vulnerability can be chained with CVE-2026-96654, CVE-2026-96655, and CVE-2026-96656.  Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-96652"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1793664000000,
      "publishedAt": 1792454400000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 19,
      "title": "Siemens Software Center Uncontrolled Search Path Element Vulnerability — CVE-2026-110936",
      "summary": "Siemens Software Center contains an uncontrolled search path element vulnerability that could allow an authenticated local attacker to place a specially crafted DLL in a directory that the application searches, leading to arbitrary code execution with the privileges of the affected application. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-110936"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1793664000000,
      "publishedAt": 1792454400000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 20,
      "title": "BeyondTrust Remote Support Authentication Bypass by Spoofing Vulnerability — CVE-2026-102764",
      "summary": "BeyondTrust Remote Support contains an authentication bypass by spoofing vulnerability that could allow a remote attacker to perform file operations on the appliance. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-102764"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792368000000,
      "publishedAt": 1792108800000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 21,
      "title": "Fortinet Multiple Products Path Traversal Vulnerability — CVE-2025-61624",
      "summary": "FortiOS, FortiPAM, FortiProxy, and FortiSwitchManager contain a path traversal vulnerability that may allow an authenticated attacker with admin profile and at least read-write permissions to write or delete arbitrary files via specific CLI commands. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2025-61624"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792368000000,
      "publishedAt": 1792108800000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 22,
      "title": "Atlassian Multiple Products Files or Directories Accessible to External Parties Vulnerability — CVE-2026-21589",
      "summary": "Multiple Atlassian products (Bitbucket Data Center ,Confluence Data Center, Jira Service Management Data Center, Jira Software Data Center, Bamboo Data Center, Crowd Data Center, Crucible and Fisheye) contain a files or directories accessible to external parties vulnerability which could allow an unauthenticated attacker to access specific files within the web application root directory in affected versions. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-21589"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792281600000,
      "publishedAt": 1792022400000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 23,
      "title": "Ahsay AhsayCBS Command Injection Vulnerability — CVE-2026-105134",
      "summary": "Ahsay AhsayCBS contains a command injection vulnerability that could allow a remote attacker to execute operating system commands via manipulation of the random parameter in the Replication Receiver component’s /rps/api/json/UpdateReceivers.do endpoint. This vulnerability can be chained with CVE-2026-105133. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-105134"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792195200000,
      "publishedAt": 1791936000000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 24,
      "title": "Cisco Secure Email Gateway Relative Path Traversal Vulnerability — CVE-2026-76519",
      "summary": "Cisco Secure Email Gateway contains a relative path traversal vulnerability that could allow an unauthenticated, remote attacker to write arbitrary files on the underlying operating system. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-76519"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792195200000,
      "publishedAt": 1791936000000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 25,
      "title": "Ahsay AhsayCBS Improper Authentication Vulnerability — CVE-2026-105133",
      "summary": "Ahsay AhsayCBS contains an improper authentication vulnerability that could allow a remote attacker to bypass authentication via manipulation of the random parameter in the API component’s checkSysPwd function. This vulnerability can be chained with CVE-2026-105134. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-105133"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792195200000,
      "publishedAt": 1791936000000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 26,
      "title": "Palo Alto Networks PAN-OS Code Injection Vulnerability — CVE-2026-0322",
      "summary": "Palo Alto Networks PAN-OS contains a code injection vulnerability that could allow an authenticated attacker with network access to execute arbitrary code with root privileges on the firewall. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-0322"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792195200000,
      "publishedAt": 1791936000000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 27,
      "title": "Ivanti Connect Secure Authentication Bypass by Spoofing Vulnerability — CVE-2026-104935",
      "summary": "Ivanti Connect Secure contains an authentication bypass by spoofing vulnerability that allows a remote unauthenticated attacker to execute arbitrary commands. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-104935"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792108800000,
      "publishedAt": 1791849600000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 28,
      "title": "Microsoft Windows Command Injection Vulnerability — CVE-2026-96951",
      "summary": "Microsoft Windows Shell contains a command injection vulnerability that allows an unauthorized attacker to bypass a security feature over a network. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-96951"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792108800000,
      "publishedAt": 1791849600000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 29,
      "title": " ISC BIND Data Processing Errors Vulnerability — CVE-2015-5477",
      "summary": "ISC BIND contains a data processing errors vulnerability that could allow remote attackers to cause a denial of service via TKEY queries. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2015-5477"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1791676800000,
      "publishedAt": 1791417600000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 30,
      "title": "Apache Struts Command Injection Vulnerability — CVE-2016-3081",
      "summary": "Apache Struts contains a command injection vulnerability that could allow remote attackers to execute arbitrary code via method:prefix when Dynamic Method Invocation is enabled. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2016-3081"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1791676800000,
      "publishedAt": 1791417600000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 33,
      "title": "ProFTPD Improper Access Control Vulnerability — CVE-2015-3306",
      "summary": "ProFTPD contains an improper access control vulnerability that could allow remote attackers to read and write to arbitrary files via the site cpfr and site cpto commands. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2015-3306"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1791676800000,
      "publishedAt": 1791417600000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 34,
      "title": "Citrix NetScaler Improper Restriction of Operations within the Bounds of a Memory Buffer Vulnerability — CVE-2026-88779",
      "summary": "Citrix NetScaler ADC (formerly Citrix ADC) and Citrix NetScaler Gateway (formerly Citrix Gateway) contain an improper restriction of operations within the bounds of a memory buffer vulnerability that could allow for a denial of service. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-88779"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1791331200000,
      "publishedAt": 1791072000000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 35,
      "title": "Zammad GmbH Zammad Improper Privilege Management Vulnerability — CVE-2026-102490",
      "summary": "Zammad GmbH Zammad contains an improper privilege management vulnerability that can allow the local zammad user to escalate privileges to root. This vulnerability can be chained with CVE-2026-102489. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-102490"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1791158400000,
      "publishedAt": 1790899200000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 37,
      "title": "Fortinet FortiMail Path Traversal Vulnerability — CVE-2026-104286",
      "summary": "Fortinet FortiMail contains a path traversal and an improper neutralization of NULL byte or NULL character vulnerability that may allow an unauthenticated attacker to write arbitrary files on the underlying system via crafted HTTP or HTTPS requests. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-104286"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1791072000000,
      "publishedAt": 1790812800000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 38,
      "title": "Cisco Catalyst SD-WAN Manager Hex Encoding Vulnerability — CVE-2026-76504",
      "summary": "Cisco Catalyst SD-WAN Manager contains a hex encoding vulnerability that could allow an unauthenticated, remote attacker to access an affected system with privileges of the admin user due to improper handling of URI encoding in an HTTP request. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-76504"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790985600000,
      "publishedAt": 1790726400000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 39,
      "title": "Apple Multiple Products Out-of-Bounds Write Vulnerability — CVE-2026-86950",
      "summary": "Apple iOS, macOS, and iPadOS contain an out-of-bounds write vulnerability in CoreGraphics that may lead to arbitrary code execution. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-86950"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1791849600000,
      "publishedAt": 1790640000000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 41,
      "title": "Citrix NetScaler Improper Input Validation Vulnerability — CVE-2026-88771",
      "summary": "Citrix NetScaler ADC and NetScaler Gateway contain an improper input validation vulnerability that could allow an unauthenticated attacker to execute arbitrary commands. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-88771"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790726400000,
      "publishedAt": 1790467200000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 42,
      "title": "Mikrotik RouterOS Improper Enforcement of Behavioral Workflow Vulnerability — CVE-2026-67279",
      "summary": "Mikrotik RouterOS contains an improper enforcement of behavioral workflow vulnerability that could allow an unauthenticated client to open a session channel and send an exec request. This vulnerability can be chained to achieve unauthenticated exploitation of CVE-2026-86060. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-67279"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790553600000,
      "publishedAt": 1790294400000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 43,
      "title": "Microsoft SharePoint Code Injection Vulnerability — CVE-2026-65660",
      "summary": "Microsoft SharePoint contains a code injection vulnerability which could allow an authorized attacker to execute code over a network. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-65660"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790553600000,
      "publishedAt": 1790294400000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 46,
      "title": "Adobe Commerce and Magento Incorrect Authorization Vulnerability  — CVE-2026-71362",
      "summary": "Adobe Commerce and Magento contains an incorrect authorization vulnerability that could allow an attacker to leverage this vulnerability to gain elevated access to sensitive resources without any user interaction.  Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-71362"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790467200000,
      "publishedAt": 1790208000000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 47,
      "title": "Arista VeloCloud Orchestrator Improper Input Validation Vulnerability — CVE-2026-93952",
      "summary": "Arista VeloCloud Orchestrator (VCO) on-prem contains an improper input validation vulnerability that may allow a remote attacker to access privileged internal functionality and impact the VCO host. Successful exploitation may compromise the confidentiality, integrity, and availability of the orchestrator and data managed by the orchestrator. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-93952"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790294400000,
      "publishedAt": 1790035200000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 49,
      "title": "Check Point Multiple Products Path Traversal Vulnerability — CVE-2026-93616",
      "summary": "Check Point Security Management Server, Multi-Domain Security Management Server, Log Server, Multi-Domain Log Server, and SmartEvent contain a path traversal vulnerability that allows an unauthenticated attacker to upload and execute arbitrary scripts. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-93616"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790294400000,
      "publishedAt": 1790035200000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 50,
      "title": "Check Point Multiple Products Improper Certificate Validation Vulnerability — CVE-2026-85102",
      "summary": "Check Point Security Gateway and Check Point Spark Firewall using Site to Site VPN or Remote Access VPN contain an improper certificate validation vulnerability which could allow an unauthenticated remote attacker to execute arbitrary code on the Gateway. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-85102"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790294400000,
      "publishedAt": 1790035200000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 51,
      "title": "Zyxel GS1900 Series Switches Stack-Based Buffer Overflow Vulnerability — CVE-2026-7273",
      "summary": "Zyxel GS1900 series switches contain a stack-based buffer overflow vulnerability in the CGI program which could allow a LAN-based, unauthenticated attacker to exploit the flaw and potentially execute OS commands via a crafted HTTP request. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-7273"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790208000000,
      "publishedAt": 1789948800000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     }
    ],
    "detail": null,
    "stats": {
     "total": 52,
     "critical": 1,
     "high": 16,
     "medium": 35,
     "low": 0,
     "last24h": 52
    },
    "sources": [
     {
      "key": "cisa-kev",
      "label": "CISA KEV",
      "kind": "kev",
      "status": "ok",
      "lastOkAt": 1793749638212,
      "lastAttemptAt": 1793749638212,
      "itemCount": 51,
      "note": "KEV: 51 in the 45-day window"
     },
     {
      "key": "nvd",
      "label": "NVD (critical CVEs)",
      "kind": "nvd",
      "status": "pending",
      "lastOkAt": null,
      "lastAttemptAt": null,
      "itemCount": 0,
      "note": ""
     },
     {
      "key": "cisa",
      "label": "CISA advisories",
      "kind": "rss",
      "status": "pending",
      "lastOkAt": null,
      "lastAttemptAt": null,
      "itemCount": 0,
      "note": ""
     },
     {
      "key": "bleepingcomputer",
      "label": "BleepingComputer",
      "kind": "rss",
      "status": "pending",
      "lastOkAt": null,
      "lastAttemptAt": null,
      "itemCount": 0,
      "note": ""
     },
     {
      "key": "thehackernews",
      "label": "The Hacker News",
      "kind": "rss",
      "status": "pending",
      "lastOkAt": null,
      "lastAttemptAt": null,
      "itemCount": 0,
      "note": ""
     },
     {
      "key": "krebsonsecurity",
      "label": "KrebsOnSecurity",
      "kind": "rss",
      "status": "pending",
      "lastOkAt": null,
      "lastAttemptAt": null,
      "itemCount": 0,
      "note": ""
     },
     {
      "key": "darkreading",
      "label": "Dark Reading",
      "kind": "rss",
      "status": "pending",
      "lastOkAt": null,
      "lastAttemptAt": null,
      "itemCount": 0,
      "note": ""
     },
     {
      "key": "securityweek",
      "label": "SecurityWeek",
      "kind": "rss",
      "status": "pending",
      "lastOkAt": null,
      "lastAttemptAt": null,
      "itemCount": 0,
      "note": ""
     },
     {
      "key": "sans-isc",
      "label": "SANS ISC",
      "kind": "rss",
      "status": "pending",
      "lastOkAt": null,
      "lastAttemptAt": null,
      "itemCount": 0,
      "note": ""
     }
    ],
    "categories": [
     "CVE / Vulnerability",
     "Ransomware",
     "Breach",
     "Malware",
     "Phishing",
     "General"
    ],
    "syncRuns": [
     {
      "id": 1,
      "status": "running",
      "added": 0,
      "updated": 0,
      "skipped": 0,
      "note": "",
      "createdAt": 1793749633906,
      "finishedAt": null
     }
    ],
    "boundaryCopy": "Nothing here auto-publishes. Filing to the News Desk is a deliberate tap and always lands as a draft; the desk's own publish action is the only road to the public site. Scores are a documented deterministic triage sort, not a risk measurement."
   }
  },
  "GET /api/app/threat-intel?itemId={id}": {
   "status": 200,
   "body": {
    "ok": true,
    "demo": false,
    "items": [
     {
      "id": 52,
      "title": "Capture Fictional CVE Item",
      "summary": "Seed summary",
      "url": "https://example.invalid/cve",
      "category": "CVE / Vulnerability",
      "severity": "Critical",
      "score": 90,
      "cveIds": [
       "CVE-2026-0001"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": null,
      "publishedAt": 1793749633968,
      "firstSeenAt": 1793749633968,
      "lastSeenAt": 1793749633968,
      "primarySource": "Seed Source",
      "sources": [],
      "newsItemId": null
     },
     {
      "id": 1,
      "title": "WordPress WordPress Core Time-of-check Time-of-use (TOCTOU) Race Condition Vulnerability — CVE-2026-119977",
      "summary": "WordPress WordPress Core contains a time-of-check time-of-use (TOCTOU) race condition vulnerability that could allow unauthenticated attackers to execute code on the server during the upgrade window. This is due to predictable, web-accessible staging paths used during full-package core upgrades. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 55,
      "cveIds": [
       "CVE-2026-119977"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1793923200000,
      "publishedAt": 1793664000000,
      "firstSeenAt": 1793749638207,
      "lastSeenAt": 1793749638207,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 2,
      "title": "Ivanti Connect Secure Generation of Predictable Numbers or Identifiers Vulnerability — CVE-2026-118527",
      "summary": "Ivanti Connect Secure contains a generation of predictable numbers or identifiers vulnerability that allows a remote authenticated attacker to upload files with attacker-controlled content to a known location on disk. This vulnerability can be chained with CVE-2026-104935 and CVE-2026-111357. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 55,
      "cveIds": [
       "CVE-2026-118527"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1794528000000,
      "publishedAt": 1793318400000,
      "firstSeenAt": 1793749638208,
      "lastSeenAt": 1793749638208,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 6,
      "title": "Broadcom VMWare ESX Insufficient Logging Vulnerability — CVE-2026-41709",
      "summary": "Broadcom VMWare ESX contains an insufficient logging vulnerability that could allow a malicious actor with admin access to perform certain operations without being logged. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 55,
      "cveIds": [
       "CVE-2026-41709"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1794355200000,
      "publishedAt": 1793145600000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 8,
      "title": "Adobe Commerce and Magento Unrestricted Upload of File with Dangerous Type Vulnerability — CVE-2026-48356",
      "summary": "Adobe Commerce and Magento contain an unrestricted upload of file with dangerous type vulnerability that could result in arbitrary code execution in the context of the current user, potentially gaining elevated access or control over the victim's account or session. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 55,
      "cveIds": [
       "CVE-2026-48356"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1794268800000,
      "publishedAt": 1793059200000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 9,
      "title": "Google Brotli Unchecked Return Value Vulnerability — CVE-2026-111497",
      "summary": "Google Brotli contains an unchecked return value vulnerability that allows a remote unauthenticated attacker to corrupt adjacent decoder state memory, disclose sensitive memory contents, cause a denial of service, or potentially execute arbitrary code using a crafted Brotli-compressed stream. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 55,
      "cveIds": [
       "CVE-2026-111497"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1794268800000,
      "publishedAt": 1793059200000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 10,
      "title": "PHP Memory Corruption Vulnerability — CVE-2026-109618",
      "summary": "PHP contains an out-of-memory corruption vulnerability that allows for unauthenticated remote code execution. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 53,
      "cveIds": [
       "CVE-2026-109618"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1793232000000,
      "publishedAt": 1792972800000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 11,
      "title": "PHP Stale Internal Request-State Vulnerability — CVE-2026-109619",
      "summary": "PHP contains a stale internal request-state vulnerability that allows for unauthenticated remote code execution. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 53,
      "cveIds": [
       "CVE-2026-109619"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1793232000000,
      "publishedAt": 1792972800000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 13,
      "title": "Collabora Online – Built-in CODE Server OS Command Injection Vulnerability — CVE-2026-110447",
      "summary": "Collabora Collabora Online – Built-in CODE Server when used within the NextCloud platform contains an OS command injection vulnerability that allows for remote code execution when the CODE server isn't running. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 53,
      "cveIds": [
       "CVE-2026-110447"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792972800000,
      "publishedAt": 1792713600000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 31,
      "title": "Strapi Cleartext Storage of Sensitive Information Vulnerability — CVE-2023-22894",
      "summary": "Strapi contains a cleartext storage of sensitive information vulnerability that could allow attackers with access to the admin panel to discover sensitive user details via the query filter. The impacted product(s) could be end-of-life (EoL) and/or end-of-service (EoS). Users are advised to discontinue use and/or transition to a supported version. This vulnerability can be chained with CVE-2023-22621 to achieve remote code execution.  Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 53,
      "cveIds": [
       "CVE-2023-22894"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1791676800000,
      "publishedAt": 1791417600000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 32,
      "title": "ONLYOFFICE Docs Server Path Traversal Vulnerability — CVE-2021-3199",
      "summary": "ONLYOFFICE Docs contains a path traversal vulnerability that can occur when JWT is used, via a /.. sequence in an image upload parameter and could allow for remote code execution. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 53,
      "cveIds": [
       "CVE-2021-3199"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1791676800000,
      "publishedAt": 1791417600000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 36,
      "title": "Zammad GmbH Zammad Session Fixation Vulnerability — CVE-2026-102489",
      "summary": "Zammad GmbH Zammad contains a session fixation vulnerability that can lead to remote code execution as the zammad user. This vulnerability can be chained with CVE-2026-102490. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 53,
      "cveIds": [
       "CVE-2026-102489"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1791158400000,
      "publishedAt": 1790899200000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 40,
      "title": "Citrix NetScaler Improper Restriction of Operations within the Bounds of a Memory Buffer Vulnerability — CVE-2026-88772",
      "summary": "Citrix NetScaler ADC and NetScaler Gateway contain an improper restriction of operations within the bounds of a memory buffer vulnerability that could allow for remote code execution or denial of service Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 53,
      "cveIds": [
       "CVE-2026-88772"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790726400000,
      "publishedAt": 1790467200000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 44,
      "title": "WordPress Core Remote File Inclusion Vulnerability — CVE-2026-87902",
      "summary": "WordPress Core contains a remote file inclusion vulnerability which could allow an unauthenticated attacker to make page-template resolution include a chosen readable local `.php` file outside the active theme directories, leading to remote code execution. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 53,
      "cveIds": [
       "CVE-2026-87902"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790553600000,
      "publishedAt": 1790294400000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 45,
      "title": "WSO2 Multiple Products Path Traversal Vulnerability  — CVE-2026-5430",
      "summary": "WSO2 API Control Plane, API Manager, Traffic Manager & Universal Gateway contain a path traversal vulnerability that could allow for unrestricted file upload and lead to remote code execution.  Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 53,
      "cveIds": [
       "CVE-2026-5430"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790467200000,
      "publishedAt": 1790208000000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 48,
      "title": "F5 BIG-IP APM Heap-based Buffer Overflow Vulnerability — CVE-2026-94127",
      "summary": "F5 BIG-IP APM contains a heap-based buffer overflow vulnerability when access policy and an OAuth profile are configured on a virtual server. This vulnerability could allow an unauthenticated attacker to perform remote code execution. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 53,
      "cveIds": [
       "CVE-2026-94127"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790294400000,
      "publishedAt": 1790035200000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 3,
      "title": "Ivanti Connect Secure Missing Authentication for Critical Function Vulnerability — CVE-2026-111357",
      "summary": "Ivanti Connect Secure contains a missing authentication for critical function vulnerability that could allow a local authenticated attacker to escalate their privileges. This vulnerability can be chained with CVE-2026-104935 and CVE-2026-118527. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "High",
      "score": 51,
      "cveIds": [
       "CVE-2026-111357"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1793577600000,
      "publishedAt": 1793318400000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 4,
      "title": "GitLab Community Edition and Enterprise Edition Integer Overflow or Wraparound Vulnerability — CVE-2026-97229",
      "summary": "GitLab Community Edition and Enterprise Edition contain a integer overflow or wraparound vulnerability that could allow an unauthenticated user to execute arbitrary code on the GitLab server via a specially crafted regular expression in a CI/CD configuration, due to insufficient regular expression validation. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-97229"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1793491200000,
      "publishedAt": 1793232000000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 5,
      "title": "Cisco Secure Firewall ASA Software and Cisco Secure FTD Software Unchecked Return Value to NULL Pointer Dereference Vulnerability — CVE-2026-76418",
      "summary": "Cisco Secure Firewall ASA Software and Cisco Secure FTD Software contains an unchecked return value to NULL pointer dereference vulnerability that could allow an unauthenticated, remote attacker to cause the device to reload unexpectedly, resulting in a denial of service (DoS) condition. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-76418"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1793491200000,
      "publishedAt": 1793232000000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 7,
      "title": "Linux Kernel Missing Initialization of a Variable Vulnerability — CVE-2026-46300",
      "summary": "Linux Kernel contains a missing initialization of a variable vulnerability that could allow for unintended modification of page-cache-backed data via failure to preserve the shared-fragment marker during socket buffer coalescing, causing ESP input processing to decrypt shared fragments in place. This vulnerability can be chained with CVE-2026-109618 and CVE-2026-109619 to implant PHP webshells and run commands as root. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-46300"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1793404800000,
      "publishedAt": 1793145600000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 12,
      "title": "Arista VeloCloud Orchestrator SQL Injection Vulnerability — CVE-2026-114625",
      "summary": "Arista VeloCloud Orchestrator on-prem contains a SQL injection vulnerability that may allow a remote attacker to access privileged internal functionality and impact the VCO host. Successful exploitation may compromise the confidentiality, integrity, and availability of the orchestrator and data managed by the orchestrator. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-114625"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792972800000,
      "publishedAt": 1792713600000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 14,
      "title": "EG Secure Solutions SiteGuard Server Edition OS Command Injection Vulnerability — CVE-2026-106931",
      "summary": "EG Secure Solutions SiteGuard Server Edition contains an OS command injection vulnerability which may allow an attacker to execute arbitrary OS commands with root or SYSTEM privileges on the system running the affected product. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-106931"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792713600000,
      "publishedAt": 1792454400000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 15,
      "title": "Plex Media Server External Control of File Name or Path Vulnerability — CVE-2026-96656",
      "summary": "Plex Media Server contains an external control of file name or path vulnerability which can allow an admin user to write arbitrary files that may be executed on load. This vulnerability can be chained with CVE-2026-96652, CVE-2026-96654, and CVE-2026-96655. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-96656"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792713600000,
      "publishedAt": 1792454400000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 16,
      "title": "Plex Media Server Server-Side Request Forgery (SSRF) Vulnerability — CVE-2026-96655",
      "summary": "Plex Media Server contains a server-side request forgery (SSRF) vulnerability that could allow an authenticated user to request arbitrary internal or external addresses via the '/video/:/transcode/universal' path parameter. This vulnerability can be chained with CVE-2026-96652, CVE-2026-96654, and CVE-2026-96656. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-96655"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1793664000000,
      "publishedAt": 1792454400000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 17,
      "title": "Plex Media Server Improper Neutralization of Encoded URI Schemes in a Web Page Vulnerability — CVE-2026-96654",
      "summary": "Plex Media Server contains an improper neutralization of encoded URI schemes in a web page vulnerability that could allow an attacker to call other plugins' functions and supply their own parameters. This vulnerability can be chained with CVE-2026-96652, CVE-2026-96655, and CVE-2026-96656. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-96654"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792713600000,
      "publishedAt": 1792454400000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 18,
      "title": "Plex Media Server Server-Side Request Forgery (SSRF) Vulnerability — CVE-2026-96652",
      "summary": "Plex Media Server contains a server-side request forgery (SSRF) vulnerability which could allow an attacker using any X-Plex-Token value to include a full URL in the 'protocol' parameter and force the Plex server to POST to the attacker's chosen destination. This vulnerability can be chained with CVE-2026-96654, CVE-2026-96655, and CVE-2026-96656.  Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-96652"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1793664000000,
      "publishedAt": 1792454400000,
      "firstSeenAt": 1793749638209,
      "lastSeenAt": 1793749638209,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 19,
      "title": "Siemens Software Center Uncontrolled Search Path Element Vulnerability — CVE-2026-110936",
      "summary": "Siemens Software Center contains an uncontrolled search path element vulnerability that could allow an authenticated local attacker to place a specially crafted DLL in a directory that the application searches, leading to arbitrary code execution with the privileges of the affected application. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-110936"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1793664000000,
      "publishedAt": 1792454400000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 20,
      "title": "BeyondTrust Remote Support Authentication Bypass by Spoofing Vulnerability — CVE-2026-102764",
      "summary": "BeyondTrust Remote Support contains an authentication bypass by spoofing vulnerability that could allow a remote attacker to perform file operations on the appliance. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-102764"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792368000000,
      "publishedAt": 1792108800000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 21,
      "title": "Fortinet Multiple Products Path Traversal Vulnerability — CVE-2025-61624",
      "summary": "FortiOS, FortiPAM, FortiProxy, and FortiSwitchManager contain a path traversal vulnerability that may allow an authenticated attacker with admin profile and at least read-write permissions to write or delete arbitrary files via specific CLI commands. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2025-61624"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792368000000,
      "publishedAt": 1792108800000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 22,
      "title": "Atlassian Multiple Products Files or Directories Accessible to External Parties Vulnerability — CVE-2026-21589",
      "summary": "Multiple Atlassian products (Bitbucket Data Center ,Confluence Data Center, Jira Service Management Data Center, Jira Software Data Center, Bamboo Data Center, Crowd Data Center, Crucible and Fisheye) contain a files or directories accessible to external parties vulnerability which could allow an unauthenticated attacker to access specific files within the web application root directory in affected versions. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-21589"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792281600000,
      "publishedAt": 1792022400000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 23,
      "title": "Ahsay AhsayCBS Command Injection Vulnerability — CVE-2026-105134",
      "summary": "Ahsay AhsayCBS contains a command injection vulnerability that could allow a remote attacker to execute operating system commands via manipulation of the random parameter in the Replication Receiver component’s /rps/api/json/UpdateReceivers.do endpoint. This vulnerability can be chained with CVE-2026-105133. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-105134"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792195200000,
      "publishedAt": 1791936000000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 24,
      "title": "Cisco Secure Email Gateway Relative Path Traversal Vulnerability — CVE-2026-76519",
      "summary": "Cisco Secure Email Gateway contains a relative path traversal vulnerability that could allow an unauthenticated, remote attacker to write arbitrary files on the underlying operating system. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-76519"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792195200000,
      "publishedAt": 1791936000000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 25,
      "title": "Ahsay AhsayCBS Improper Authentication Vulnerability — CVE-2026-105133",
      "summary": "Ahsay AhsayCBS contains an improper authentication vulnerability that could allow a remote attacker to bypass authentication via manipulation of the random parameter in the API component’s checkSysPwd function. This vulnerability can be chained with CVE-2026-105134. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-105133"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792195200000,
      "publishedAt": 1791936000000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 26,
      "title": "Palo Alto Networks PAN-OS Code Injection Vulnerability — CVE-2026-0322",
      "summary": "Palo Alto Networks PAN-OS contains a code injection vulnerability that could allow an authenticated attacker with network access to execute arbitrary code with root privileges on the firewall. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-0322"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792195200000,
      "publishedAt": 1791936000000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 27,
      "title": "Ivanti Connect Secure Authentication Bypass by Spoofing Vulnerability — CVE-2026-104935",
      "summary": "Ivanti Connect Secure contains an authentication bypass by spoofing vulnerability that allows a remote unauthenticated attacker to execute arbitrary commands. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-104935"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792108800000,
      "publishedAt": 1791849600000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 28,
      "title": "Microsoft Windows Command Injection Vulnerability — CVE-2026-96951",
      "summary": "Microsoft Windows Shell contains a command injection vulnerability that allows an unauthorized attacker to bypass a security feature over a network. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-96951"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1792108800000,
      "publishedAt": 1791849600000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 29,
      "title": " ISC BIND Data Processing Errors Vulnerability — CVE-2015-5477",
      "summary": "ISC BIND contains a data processing errors vulnerability that could allow remote attackers to cause a denial of service via TKEY queries. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2015-5477"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1791676800000,
      "publishedAt": 1791417600000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 30,
      "title": "Apache Struts Command Injection Vulnerability — CVE-2016-3081",
      "summary": "Apache Struts contains a command injection vulnerability that could allow remote attackers to execute arbitrary code via method:prefix when Dynamic Method Invocation is enabled. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2016-3081"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1791676800000,
      "publishedAt": 1791417600000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 33,
      "title": "ProFTPD Improper Access Control Vulnerability — CVE-2015-3306",
      "summary": "ProFTPD contains an improper access control vulnerability that could allow remote attackers to read and write to arbitrary files via the site cpfr and site cpto commands. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2015-3306"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1791676800000,
      "publishedAt": 1791417600000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 34,
      "title": "Citrix NetScaler Improper Restriction of Operations within the Bounds of a Memory Buffer Vulnerability — CVE-2026-88779",
      "summary": "Citrix NetScaler ADC (formerly Citrix ADC) and Citrix NetScaler Gateway (formerly Citrix Gateway) contain an improper restriction of operations within the bounds of a memory buffer vulnerability that could allow for a denial of service. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-88779"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1791331200000,
      "publishedAt": 1791072000000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 35,
      "title": "Zammad GmbH Zammad Improper Privilege Management Vulnerability — CVE-2026-102490",
      "summary": "Zammad GmbH Zammad contains an improper privilege management vulnerability that can allow the local zammad user to escalate privileges to root. This vulnerability can be chained with CVE-2026-102489. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-102490"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1791158400000,
      "publishedAt": 1790899200000,
      "firstSeenAt": 1793749638211,
      "lastSeenAt": 1793749638211,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 37,
      "title": "Fortinet FortiMail Path Traversal Vulnerability — CVE-2026-104286",
      "summary": "Fortinet FortiMail contains a path traversal and an improper neutralization of NULL byte or NULL character vulnerability that may allow an unauthenticated attacker to write arbitrary files on the underlying system via crafted HTTP or HTTPS requests. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-104286"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1791072000000,
      "publishedAt": 1790812800000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 38,
      "title": "Cisco Catalyst SD-WAN Manager Hex Encoding Vulnerability — CVE-2026-76504",
      "summary": "Cisco Catalyst SD-WAN Manager contains a hex encoding vulnerability that could allow an unauthenticated, remote attacker to access an affected system with privileges of the admin user due to improper handling of URI encoding in an HTTP request. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-76504"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790985600000,
      "publishedAt": 1790726400000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 39,
      "title": "Apple Multiple Products Out-of-Bounds Write Vulnerability — CVE-2026-86950",
      "summary": "Apple iOS, macOS, and iPadOS contain an out-of-bounds write vulnerability in CoreGraphics that may lead to arbitrary code execution. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-86950"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1791849600000,
      "publishedAt": 1790640000000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 41,
      "title": "Citrix NetScaler Improper Input Validation Vulnerability — CVE-2026-88771",
      "summary": "Citrix NetScaler ADC and NetScaler Gateway contain an improper input validation vulnerability that could allow an unauthenticated attacker to execute arbitrary commands. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-88771"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790726400000,
      "publishedAt": 1790467200000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 42,
      "title": "Mikrotik RouterOS Improper Enforcement of Behavioral Workflow Vulnerability — CVE-2026-67279",
      "summary": "Mikrotik RouterOS contains an improper enforcement of behavioral workflow vulnerability that could allow an unauthenticated client to open a session channel and send an exec request. This vulnerability can be chained to achieve unauthenticated exploitation of CVE-2026-86060. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-67279"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790553600000,
      "publishedAt": 1790294400000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 43,
      "title": "Microsoft SharePoint Code Injection Vulnerability — CVE-2026-65660",
      "summary": "Microsoft SharePoint contains a code injection vulnerability which could allow an authorized attacker to execute code over a network. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-65660"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790553600000,
      "publishedAt": 1790294400000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 46,
      "title": "Adobe Commerce and Magento Incorrect Authorization Vulnerability  — CVE-2026-71362",
      "summary": "Adobe Commerce and Magento contains an incorrect authorization vulnerability that could allow an attacker to leverage this vulnerability to gain elevated access to sensitive resources without any user interaction.  Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-71362"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790467200000,
      "publishedAt": 1790208000000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 47,
      "title": "Arista VeloCloud Orchestrator Improper Input Validation Vulnerability — CVE-2026-93952",
      "summary": "Arista VeloCloud Orchestrator (VCO) on-prem contains an improper input validation vulnerability that may allow a remote attacker to access privileged internal functionality and impact the VCO host. Successful exploitation may compromise the confidentiality, integrity, and availability of the orchestrator and data managed by the orchestrator. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-93952"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790294400000,
      "publishedAt": 1790035200000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 49,
      "title": "Check Point Multiple Products Path Traversal Vulnerability — CVE-2026-93616",
      "summary": "Check Point Security Management Server, Multi-Domain Security Management Server, Log Server, Multi-Domain Log Server, and SmartEvent contain a path traversal vulnerability that allows an unauthenticated attacker to upload and execute arbitrary scripts. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-93616"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790294400000,
      "publishedAt": 1790035200000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 50,
      "title": "Check Point Multiple Products Improper Certificate Validation Vulnerability — CVE-2026-85102",
      "summary": "Check Point Security Gateway and Check Point Spark Firewall using Site to Site VPN or Remote Access VPN contain an improper certificate validation vulnerability which could allow an unauthenticated remote attacker to execute arbitrary code on the Gateway. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-85102"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790294400000,
      "publishedAt": 1790035200000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     },
     {
      "id": 51,
      "title": "Zyxel GS1900 Series Switches Stack-Based Buffer Overflow Vulnerability — CVE-2026-7273",
      "summary": "Zyxel GS1900 series switches contain a stack-based buffer overflow vulnerability in the CGI program which could allow a LAN-based, unauthenticated attacker to exploit the flaw and potentially execute OS commands via a crafted HTTP request. Required action: Apply mitigations in accordance with vendor instructions, ensuring compliance with CISA’s BOD 26-04 Prioritizing Security Updates Based on Risk (see URL in Notes) guidance and CISA’s “Forensics Triage Requirements” (see URL in Notes). Follow applicable BOD 26-04 guidance for cloud services or discontinue use of the product if mitigations are unavailable. Stakeholders are responsible for evaluating each asset's internet exposure and ensuring adherence to BOD 26-04 patching guidelines.",
      "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog",
      "category": "CVE / Vulnerability",
      "severity": "Medium",
      "score": 45,
      "cveIds": [
       "CVE-2026-7273"
      ],
      "cvss": null,
      "kevRansomware": false,
      "kevDueAt": 1790208000000,
      "publishedAt": 1789948800000,
      "firstSeenAt": 1793749638212,
      "lastSeenAt": 1793749638212,
      "primarySource": "cisa-kev",
      "sources": [
       {
        "key": "cisa-kev",
        "label": "CISA KEV",
        "url": "https://www.cisa.gov/known-exploited-vulnerabilities-catalog"
       }
      ],
      "newsItemId": null
     }
    ],
    "detail": {
     "id": 52,
     "title": "Capture Fictional CVE Item",
     "summary": "Seed summary",
     "url": "https://example.invalid/cve",
     "category": "CVE / Vulnerability",
     "severity": "Critical",
     "score": 90,
     "cveIds": [
      "CVE-2026-0001"
     ],
     "cvss": null,
     "kevRansomware": false,
     "kevDueAt": null,
     "publishedAt": 1793749633968,
     "firstSeenAt": 1793749633968,
     "lastSeenAt": 1793749633968,
     "primarySource": "Seed Source",
     "sources": [],
     "newsItemId": null
    },
    "stats": {
     "total": 52,
     "critical": 1,
     "high": 16,
     "medium": 35,
     "low": 0,
     "last24h": 52
    },
    "sources": [
     {
      "key": "cisa-kev",
      "label": "CISA KEV",
      "kind": "kev",
      "status": "ok",
      "lastOkAt": 1793749638212,
      "lastAttemptAt": 1793749638212,
      "itemCount": 51,
      "note": "KEV: 51 in the 45-day window"
     },
     {
      "key": "nvd",
      "label": "NVD (critical CVEs)",
      "kind": "nvd",
      "status": "pending",
      "lastOkAt": null,
      "lastAttemptAt": null,
      "itemCount": 0,
      "note": ""
     },
     {
      "key": "cisa",
      "label": "CISA advisories",
      "kind": "rss",
      "status": "pending",
      "lastOkAt": null,
      "lastAttemptAt": null,
      "itemCount": 0,
      "note": ""
     },
     {
      "key": "bleepingcomputer",
      "label": "BleepingComputer",
      "kind": "rss",
      "status": "pending",
      "lastOkAt": null,
      "lastAttemptAt": null,
      "itemCount": 0,
      "note": ""
     },
     {
      "key": "thehackernews",
      "label": "The Hacker News",
      "kind": "rss",
      "status": "pending",
      "lastOkAt": null,
      "lastAttemptAt": null,
      "itemCount": 0,
      "note": ""
     },
     {
      "key": "krebsonsecurity",
      "label": "KrebsOnSecurity",
      "kind": "rss",
      "status": "pending",
      "lastOkAt": null,
      "lastAttemptAt": null,
      "itemCount": 0,
      "note": ""
     },
     {
      "key": "darkreading",
      "label": "Dark Reading",
      "kind": "rss",
      "status": "pending",
      "lastOkAt": null,
      "lastAttemptAt": null,
      "itemCount": 0,
      "note": ""
     },
     {
      "key": "securityweek",
      "label": "SecurityWeek",
      "kind": "rss",
      "status": "pending",
      "lastOkAt": null,
      "lastAttemptAt": null,
      "itemCount": 0,
      "note": ""
     },
     {
      "key": "sans-isc",
      "label": "SANS ISC",
      "kind": "rss",
      "status": "pending",
      "lastOkAt": null,
      "lastAttemptAt": null,
      "itemCount": 0,
      "note": ""
     }
    ],
    "categories": [
     "CVE / Vulnerability",
     "Ransomware",
     "Breach",
     "Malware",
     "Phishing",
     "General"
    ],
    "syncRuns": [
     {
      "id": 1,
      "status": "running",
      "added": 0,
      "updated": 0,
      "skipped": 0,
      "note": "",
      "createdAt": 1793749633906,
      "finishedAt": null
     }
    ],
    "boundaryCopy": "Nothing here auto-publishes. Filing to the News Desk is a deliberate tap and always lands as a draft; the desk's own publish action is the only road to the public site. Scores are a documented deterministic triage sort, not a risk measurement."
   }
  },
  "POST /api/app/threat-intel/sync": {
   "status": 200,
   "body": {
    "ok": true,
    "runId": 2,
    "message": "Threat Intel sync queued — the feed refreshes within a few seconds."
   }
  },
  "POST /api/app/threat-intel/items/{id}/push-to-news": {
   "status": 200,
   "body": {
    "ok": true,
    "newsItemId": 1,
    "message": "Filed to the News Desk as draft #1. It is NOT published — review and publish it from the desk."
   }
  },
  "POST /api/app/threat-intel/items/{id}/push-to-news (repeat)": {
   "status": 200,
   "body": {
    "ok": true,
    "newsItemId": 1,
    "message": "Already filed to the News Desk as draft #1 — no duplicate created."
   }
  },
  "POST /api/app/news/items": {
   "status": 201,
   "body": {
    "ok": true,
    "message": "Draft saved.",
    "item": {
     "id": 2,
     "slug": "capture-seed-headline",
     "date": "2026-11-03",
     "tag": "Announcement",
     "scope": "",
     "title": "Capture Seed Headline",
     "summary": "Seed summary",
     "body": "Seed body text",
     "state": "draft",
     "sourceName": "Seed Wire",
     "sourceUrl": "https://example.invalid/story",
     "origin": "desk",
     "createdAt": 1793749641934,
     "updatedAt": 1793749641934,
     "publishedAt": null
    }
   }
  },
  "GET /api/app/news/items": {
   "status": 200,
   "body": {
    "tags": [
     "Announcement",
     "Threat Watch",
     "Patch Alert",
     "From the House"
    ],
    "canWrite": true,
    "triage": {
     "drafts": 2,
     "published": 0,
     "today": 2,
     "thisWeek": 0,
     "older": 0,
     "failedLegs": {
      "verified": 0,
      "factual": 0,
      "scope": 0,
      "unverified": 0,
      "unrecorded": 2
     },
     "staleDays": 7
    },
    "items": [
     {
      "id": 2,
      "slug": "capture-seed-headline",
      "date": "2026-11-03",
      "tag": "Announcement",
      "scope": "",
      "title": "Capture Seed Headline",
      "summary": "Seed summary",
      "body": "Seed body text",
      "state": "draft",
      "sourceName": "Seed Wire",
      "sourceUrl": "https://example.invalid/story",
      "origin": "desk",
      "createdAt": 1793749641934,
      "updatedAt": 1793749641934,
      "publishedAt": null,
      "ageDays": 0,
      "isStale": false,
      "failedLeg": null
     },
     {
      "id": 1,
      "slug": "capture-fictional-cve-item",
      "date": "2026-11-03",
      "tag": "Threat Watch",
      "scope": "",
      "title": "Capture Fictional CVE Item",
      "summary": "Seed summary",
      "body": "Seed summary\n\nSeverity Critical · score 90 (deterministic triage score — see Threat Intel). CVEs: CVE-2026-0001\n\nFiled from Threat Intel item #52 by OwnerCap6. Draft only — review, edit, and publish from the News Desk.",
      "state": "draft",
      "sourceName": "Seed Source",
      "sourceUrl": "https://example.invalid/cve",
      "origin": "threat-intel",
      "createdAt": 1793749641750,
      "updatedAt": 1793749641750,
      "publishedAt": null,
      "ageDays": 0,
      "isStale": false,
      "failedLeg": null
     }
    ],
    "ok": true
   }
  },
  "GET /api/app/news/feed (before publish)": {
   "status": 200,
   "body": {
    "items": [],
    "ok": true
   }
  },
  "PATCH /api/app/news/items/{id}": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "News item saved.",
    "item": {
     "id": 2,
     "slug": "capture-seed-headline",
     "date": "2026-11-03",
     "tag": "Announcement",
     "scope": "",
     "title": "Capture Seed Headline",
     "summary": "Updated seed summary",
     "body": "Seed body text",
     "state": "draft",
     "sourceName": "Seed Wire",
     "sourceUrl": "https://example.invalid/story",
     "origin": "desk",
     "createdAt": 1793749641934,
     "updatedAt": 1793749642285,
     "publishedAt": null
    }
   }
  },
  "POST /api/app/news/items (incomplete)": {
   "status": 201,
   "body": {
    "ok": true,
    "message": "Draft saved.",
    "item": {
     "id": 3,
     "slug": "capture-incomplete-draft",
     "date": "2026-11-03",
     "tag": "Announcement",
     "scope": "",
     "title": "Capture Incomplete Draft",
     "summary": "",
     "body": "",
     "state": "draft",
     "sourceName": "",
     "sourceUrl": "",
     "origin": "desk",
     "createdAt": 1793749642432,
     "updatedAt": 1793749642432,
     "publishedAt": null
    }
   }
  },
  "POST /api/app/news/items/{id}/publish (incomplete)": {
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Before publishing, the item still needs: summary, body."
   }
  },
  "POST /api/app/news/items/{id}/publish": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Published to Sentinel News.",
    "item": {
     "id": 2,
     "slug": "capture-seed-headline",
     "date": "2026-11-03",
     "tag": "Announcement",
     "scope": "",
     "title": "Capture Seed Headline",
     "summary": "Updated seed summary",
     "body": "Seed body text",
     "state": "published",
     "sourceName": "Seed Wire",
     "sourceUrl": "https://example.invalid/story",
     "origin": "desk",
     "createdAt": 1793749641934,
     "updatedAt": 1793749642621,
     "publishedAt": 1793749642621
    }
   }
  },
  "GET /api/app/news/feed": {
   "status": 200,
   "body": {
    "items": [
     {
      "id": 2,
      "slug": "capture-seed-headline",
      "date": "2026-11-03",
      "tag": "Announcement",
      "scope": "",
      "title": "Capture Seed Headline",
      "summary": "Updated seed summary",
      "body": "Seed body text",
      "state": "published",
      "sourceName": "Seed Wire",
      "sourceUrl": "https://example.invalid/story",
      "origin": "desk",
      "createdAt": 1793749641934,
      "updatedAt": 1793749642621,
      "publishedAt": 1793749642621
     }
    ],
    "ok": true
   }
  },
  "POST /api/app/news/items/bulk-delete-drafts (published)": {
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Published items can never be deleted from draft cleanup — 'Capture Seed Headline' is published. Nothing was deleted."
   }
  },
  "POST /api/app/news/items/{id}/unpublish": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Unpublished — back to a draft.",
    "item": {
     "id": 2,
     "slug": "capture-seed-headline",
     "date": "2026-11-03",
     "tag": "Announcement",
     "scope": "",
     "title": "Capture Seed Headline",
     "summary": "Updated seed summary",
     "body": "Seed body text",
     "state": "draft",
     "sourceName": "Seed Wire",
     "sourceUrl": "https://example.invalid/story",
     "origin": "desk",
     "createdAt": 1793749641934,
     "updatedAt": 1793749643031,
     "publishedAt": 1793749642621
    }
   }
  },
  "POST /api/app/news/items/bulk-delete-drafts": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Deleted 3 draft(s).",
    "deleted": 3,
    "ids": [
     2,
     3,
     1
    ]
   }
  },
  "POST /api/app/news/items (for delete)": {
   "status": 201,
   "body": {
    "ok": true,
    "message": "Draft saved.",
    "item": {
     "id": 4,
     "slug": "capture-delete-me",
     "date": "2026-11-03",
     "tag": "Announcement",
     "scope": "",
     "title": "Capture Delete Me",
     "summary": "s",
     "body": "b",
     "state": "draft",
     "sourceName": "",
     "sourceUrl": "",
     "origin": "desk",
     "createdAt": 1793749643370,
     "updatedAt": 1793749643370,
     "publishedAt": null
    }
   }
  },
  "DELETE /api/app/news/items/{id}": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "News item deleted."
   }
  },
  "GET /api/app/reviews": {
   "status": 200,
   "body": {
    "canWrite": true,
    "reviews": [
     {
      "id": 1,
      "clientId": null,
      "ticketId": null,
      "rating": 5,
      "text": "Seed review text",
      "businessName": "Capture Fictional Biz",
      "status": "pending",
      "createdAt": 1793749633968,
      "updatedAt": 1793749633968,
      "publishedAt": null,
      "featured": false,
      "featuredOrder": 0
     }
    ],
    "ok": true
   }
  },
  "POST /api/app/reviews/{id}/featured (pending)": {
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Only approved (published) reviews can be featured."
   }
  },
  "POST /api/app/reviews/{id}/publish": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Review published.",
    "review": {
     "id": 1,
     "clientId": null,
     "ticketId": null,
     "rating": 5,
     "text": "Seed review text",
     "businessName": "Capture Fictional Biz",
     "status": "published",
     "createdAt": 1793749633968,
     "updatedAt": 1793749644074,
     "publishedAt": 1793749644074,
     "featured": false,
     "featuredOrder": 0
    }
   }
  },
  "POST /api/app/reviews/{id}/featured": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Showcase updated.",
    "review": {
     "id": 1,
     "clientId": null,
     "ticketId": null,
     "rating": 5,
     "text": "Seed review text",
     "businessName": "Capture Fictional Biz",
     "status": "published",
     "createdAt": 1793749633968,
     "updatedAt": 1793749644319,
     "publishedAt": 1793749644074,
     "featured": true,
     "featuredOrder": 1
    }
   }
  },
  "GET /api/app/reviews/responses (empty)": {
   "status": 200,
   "body": {
    "ok": true,
    "canWrite": true,
    "reviews": [
     {
      "id": 1,
      "clientId": null,
      "ticketId": null,
      "rating": 5,
      "text": "Seed review text",
      "businessName": "Capture Fictional Biz",
      "status": "published",
      "createdAt": 1793749633968,
      "updatedAt": 1793749644319,
      "publishedAt": 1793749644074,
      "featured": true,
      "featuredOrder": 1,
      "response": null
     }
    ]
   }
  },
  "POST /api/app/reviews/{id}/response": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Response saved as a draft.",
    "reviewId": 1
   }
  },
  "GET /api/app/reviews/responses": {
   "status": 200,
   "body": {
    "ok": true,
    "canWrite": true,
    "reviews": [
     {
      "id": 1,
      "clientId": null,
      "ticketId": null,
      "rating": 5,
      "text": "Seed review text",
      "businessName": "Capture Fictional Biz",
      "status": "published",
      "createdAt": 1793749633968,
      "updatedAt": 1793749644319,
      "publishedAt": 1793749644074,
      "featured": true,
      "featuredOrder": 1,
      "response": {
       "id": 1,
       "reviewId": 1,
       "body": "Seed owner response",
       "status": "draft",
       "publishedAt": null,
       "createdAt": 1793749644518,
       "updatedAt": 1793749644518
      }
     }
    ]
   }
  },
  "POST /api/app/reviews/{id}/response/publish": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Response published.",
    "reviewId": 1
   }
  },
  "POST /api/app/reviews/{id}/response/unpublish": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Response unpublished.",
    "reviewId": 1
   }
  },
  "POST /api/app/reviews/{id}/hide": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Review hidden.",
    "review": {
     "id": 1,
     "clientId": null,
     "ticketId": null,
     "rating": 5,
     "text": "Seed review text",
     "businessName": "Capture Fictional Biz",
     "status": "hidden",
     "createdAt": 1793749633968,
     "updatedAt": 1793749645039,
     "publishedAt": 1793749644074,
     "featured": true,
     "featuredOrder": 1
    }
   }
  },
  "GET /api/app/law (empty)": {
   "status": 200,
   "body": {
    "practiceAreas": [
     {
      "name": "Personal Law",
      "topics": [
       "Divorce proceedings",
       "Child custody",
       "Estate planning",
       "Personal injury claims",
       "Immigration status",
       "Adoption process"
      ]
     },
     {
      "name": "Business Law",
      "topics": [
       "Forming an LLC",
       "Shareholder rights",
       "Trademark filing",
       "Contract disputes",
       "M&A due diligence",
       "Business tax liability"
      ]
     },
     {
      "name": "Civil Law",
      "topics": [
       "Property boundaries",
       "Small claims",
       "Landlord tenant",
       "Defamation",
       "Civil procedure",
       "Breach of contract"
      ]
     },
     {
      "name": "Criminal Law",
      "topics": [
       "Miranda rights",
       "Bail hearings",
       "Plea bargains",
       "Search & seizure",
       "Evidence rules",
       "Expungement"
      ]
     },
     {
      "name": "Labor Law",
      "topics": [
       "Wrongful termination",
       "Wage and hour",
       "Workplace discrimination",
       "Workers compensation",
       "Union rights",
       "FMLA leave"
      ]
     }
    ],
    "categories": [
     {
      "name": "Business Formation",
      "templates": [
       {
        "key": "articles-of-incorporation",
        "name": "Articles of Incorporation",
        "blurb": "Charter establishing a corporation",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "corporationName",
          "label": "Corporation Name",
          "required": true
         },
         {
          "key": "registeredAgent",
          "label": "Registered Agent",
          "required": true
         },
         {
          "key": "registeredAddress",
          "label": "Registered Office Address",
          "required": true
         },
         {
          "key": "sharesAuthorized",
          "label": "Authorized Shares",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "operating-agreement",
        "name": "Operating Agreement",
        "blurb": "LLC internal governance terms",
        "fastPath": true,
        "draftable": true,
        "fields": [
         {
          "key": "llcName",
          "label": "LLC Name",
          "required": true
         },
         {
          "key": "managingMember",
          "label": "Managing Member",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "amount",
          "label": "Amount / Consideration",
          "required": false
         },
         {
          "key": "purpose",
          "label": "Purpose / Subject Matter",
          "required": true
         }
        ]
       },
       {
        "key": "partnership-agreement",
        "name": "Partnership Agreement",
        "blurb": "Terms between business partners",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "partnershipName",
          "label": "Partnership Name",
          "required": true
         },
         {
          "key": "partnerNames",
          "label": "Partner Names",
          "required": true
         },
         {
          "key": "businessPurpose",
          "label": "Business Purpose",
          "required": true
         },
         {
          "key": "capitalContributions",
          "label": "Capital Contributions",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "shareholders-agreement",
        "name": "Shareholders Agreement",
        "blurb": "Rights and obligations of shareholders",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "companyName",
          "label": "Company Name",
          "required": true
         },
         {
          "key": "shareholderNames",
          "label": "Shareholder Names",
          "required": true
         },
         {
          "key": "shareClasses",
          "label": "Share Classes / Holdings",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "business-license-application",
        "name": "Business License Application",
        "blurb": "Application to operate a business",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "businessName",
          "label": "Business Name",
          "required": true
         },
         {
          "key": "ownerName",
          "label": "Owner Name",
          "required": true
         },
         {
          "key": "businessAddress",
          "label": "Business Address",
          "required": true
         },
         {
          "key": "businessType",
          "label": "Type of Business",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "dba-registration",
        "name": "DBA Registration",
        "blurb": "Doing-business-as fictitious name filing",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "ownerName",
          "label": "Owner / Registrant Name",
          "required": true
         },
         {
          "key": "dbaName",
          "label": "DBA (Fictitious) Name",
          "required": true
         },
         {
          "key": "businessAddress",
          "label": "Business Address",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Contracts",
      "templates": [
       {
        "key": "nda-mutual",
        "name": "Non-Disclosure Agreement (Mutual)",
        "blurb": "Mutual confidentiality between two parties",
        "fastPath": true,
        "draftable": true,
        "fields": [
         {
          "key": "partyOne",
          "label": "Party One",
          "required": true
         },
         {
          "key": "partyTwo",
          "label": "Party Two",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "purpose",
          "label": "Purpose of Disclosure",
          "required": true
         },
         {
          "key": "termYears",
          "label": "Term (Years)",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "nda-one-way",
        "name": "Non-Disclosure Agreement (One-Way)",
        "blurb": "One-way confidentiality undertaking",
        "fastPath": true,
        "draftable": true,
        "fields": [
         {
          "key": "disclosingParty",
          "label": "Disclosing Party",
          "required": true
         },
         {
          "key": "receivingParty",
          "label": "Receiving Party",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "purpose",
          "label": "Purpose of Disclosure",
          "required": true
         },
         {
          "key": "termYears",
          "label": "Term (Years)",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "independent-contractor-agreement",
        "name": "Independent Contractor Agreement",
        "blurb": "Engagement terms for an independent contractor",
        "fastPath": true,
        "draftable": true,
        "fields": [
         {
          "key": "clientName",
          "label": "Client Name",
          "required": true
         },
         {
          "key": "contractorName",
          "label": "Contractor Name",
          "required": true
         },
         {
          "key": "services",
          "label": "Services to Be Performed",
          "required": true
         },
         {
          "key": "startDate",
          "label": "Start Date",
          "required": true
         },
         {
          "key": "endDate",
          "label": "End Date",
          "required": false
         },
         {
          "key": "compensation",
          "label": "Compensation",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "service-agreement",
        "name": "Service Agreement",
        "blurb": "Terms for the provision of services",
        "fastPath": true,
        "draftable": true,
        "fields": [
         {
          "key": "providerName",
          "label": "Service Provider",
          "required": true
         },
         {
          "key": "clientName",
          "label": "Client Name",
          "required": true
         },
         {
          "key": "services",
          "label": "Services",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "fees",
          "label": "Fees / Payment Terms",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "bill-of-sale",
        "name": "Bill of Sale",
        "blurb": "Record of a sale of personal property",
        "fastPath": true,
        "draftable": true,
        "fields": [
         {
          "key": "sellerName",
          "label": "Seller Name",
          "required": true
         },
         {
          "key": "buyerName",
          "label": "Buyer Name",
          "required": true
         },
         {
          "key": "itemDescription",
          "label": "Item Description",
          "required": true
         },
         {
          "key": "purchasePrice",
          "label": "Purchase Price",
          "required": true
         },
         {
          "key": "saleDate",
          "label": "Sale Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "consulting-agreement",
        "name": "Consulting Agreement",
        "blurb": "Terms for consulting services",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "consultantName",
          "label": "Consultant Name",
          "required": true
         },
         {
          "key": "clientName",
          "label": "Client Name",
          "required": true
         },
         {
          "key": "scope",
          "label": "Scope of Consulting Services",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "fees",
          "label": "Fees / Payment Terms",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Real Estate",
      "templates": [
       {
        "key": "residential-lease",
        "name": "Residential Lease Agreement",
        "blurb": "Basic residential tenancy terms",
        "fastPath": true,
        "draftable": true,
        "fields": [
         {
          "key": "landlordName",
          "label": "Landlord Name",
          "required": true
         },
         {
          "key": "tenantName",
          "label": "Tenant Name",
          "required": true
         },
         {
          "key": "propertyAddress",
          "label": "Property Address",
          "required": true
         },
         {
          "key": "monthlyRent",
          "label": "Monthly Rent",
          "required": true
         },
         {
          "key": "leaseStart",
          "label": "Lease Start Date",
          "required": true
         },
         {
          "key": "leaseEnd",
          "label": "Lease End Date",
          "required": true
         },
         {
          "key": "securityDeposit",
          "label": "Security Deposit",
          "required": false
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "commercial-lease",
        "name": "Commercial Lease Agreement",
        "blurb": "Commercial tenancy terms",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "landlordName",
          "label": "Landlord Name",
          "required": true
         },
         {
          "key": "tenantName",
          "label": "Tenant Name",
          "required": true
         },
         {
          "key": "propertyAddress",
          "label": "Property Address",
          "required": true
         },
         {
          "key": "monthlyRent",
          "label": "Monthly Rent",
          "required": true
         },
         {
          "key": "leaseStart",
          "label": "Lease Start Date",
          "required": true
         },
         {
          "key": "leaseEnd",
          "label": "Lease End Date",
          "required": true
         },
         {
          "key": "permittedUse",
          "label": "Permitted Use",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "lease-amendment",
        "name": "Lease Amendment",
        "blurb": "Amendment to an existing lease",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "landlordName",
          "label": "Landlord Name",
          "required": true
         },
         {
          "key": "tenantName",
          "label": "Tenant Name",
          "required": true
         },
         {
          "key": "propertyAddress",
          "label": "Property Address",
          "required": true
         },
         {
          "key": "originalLeaseDate",
          "label": "Original Lease Date",
          "required": true
         },
         {
          "key": "amendmentTerms",
          "label": "Amendment Terms",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "real-estate-purchase-agreement",
        "name": "Real Estate Purchase Agreement",
        "blurb": "Terms for the purchase of real property",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "buyerName",
          "label": "Buyer Name",
          "required": true
         },
         {
          "key": "sellerName",
          "label": "Seller Name",
          "required": true
         },
         {
          "key": "propertyAddress",
          "label": "Property Address",
          "required": true
         },
         {
          "key": "purchasePrice",
          "label": "Purchase Price",
          "required": true
         },
         {
          "key": "closingDate",
          "label": "Closing Date",
          "required": true
         },
         {
          "key": "earnestMoney",
          "label": "Earnest Money",
          "required": false
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "eviction-notice",
        "name": "Eviction Notice",
        "blurb": "Notice to vacate / cure",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "landlordName",
          "label": "Landlord Name",
          "required": true
         },
         {
          "key": "tenantName",
          "label": "Tenant Name",
          "required": true
         },
         {
          "key": "propertyAddress",
          "label": "Property Address",
          "required": true
         },
         {
          "key": "reason",
          "label": "Reason for Notice",
          "required": true
         },
         {
          "key": "cureOrVacateDate",
          "label": "Cure / Vacate By Date",
          "required": true
         },
         {
          "key": "noticeDate",
          "label": "Notice Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "property-management-agreement",
        "name": "Property Management Agreement",
        "blurb": "Terms for managing a property on the owner's behalf",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "ownerName",
          "label": "Property Owner",
          "required": true
         },
         {
          "key": "managerName",
          "label": "Property Manager",
          "required": true
         },
         {
          "key": "propertyAddress",
          "label": "Property Address",
          "required": true
         },
         {
          "key": "managementFee",
          "label": "Management Fee",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Estate Planning",
      "templates": [
       {
        "key": "last-will",
        "name": "Last Will and Testament (Basic)",
        "blurb": "Basic will with executor and beneficiaries",
        "fastPath": true,
        "draftable": true,
        "fields": [
         {
          "key": "testatorName",
          "label": "Testator Full Name",
          "required": true
         },
         {
          "key": "executorName",
          "label": "Executor Name",
          "required": true
         },
         {
          "key": "beneficiaries",
          "label": "Beneficiaries and Their Shares",
          "required": true
         },
         {
          "key": "specificGifts",
          "label": "Specific Gifts",
          "required": false
         },
         {
          "key": "executionDate",
          "label": "Execution Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "general-power-of-attorney",
        "name": "General Power of Attorney",
        "blurb": "Broad agency authority granted to an agent",
        "fastPath": true,
        "draftable": true,
        "fields": [
         {
          "key": "principalName",
          "label": "Principal Name",
          "required": true
         },
         {
          "key": "agentName",
          "label": "Agent (Attorney-in-Fact) Name",
          "required": true
         },
         {
          "key": "alternateAgent",
          "label": "Alternate Agent",
          "required": false
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "powers",
          "label": "Powers Granted",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "living-will",
        "name": "Living Will / Advance Directive",
        "blurb": "End-of-life care instructions",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "declarantName",
          "label": "Declarant Full Name",
          "required": true
         },
         {
          "key": "agentName",
          "label": "Health Care Agent",
          "required": false
         },
         {
          "key": "instructions",
          "label": "Care Instructions",
          "required": true
         },
         {
          "key": "executionDate",
          "label": "Execution Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "revocable-living-trust",
        "name": "Revocable Living Trust Agreement",
        "blurb": "Trust holding assets for beneficiaries",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "grantorName",
          "label": "Grantor Name",
          "required": true
         },
         {
          "key": "trusteeName",
          "label": "Trustee Name",
          "required": true
         },
         {
          "key": "trustName",
          "label": "Trust Name",
          "required": true
         },
         {
          "key": "beneficiaries",
          "label": "Beneficiaries",
          "required": true
         },
         {
          "key": "trustProperty",
          "label": "Initial Trust Property",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "healthcare-power-of-attorney",
        "name": "Health Care Power of Attorney",
        "blurb": "Authority to make health care decisions",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "principalName",
          "label": "Principal Name",
          "required": true
         },
         {
          "key": "agentName",
          "label": "Health Care Agent Name",
          "required": true
         },
         {
          "key": "alternateAgent",
          "label": "Alternate Agent",
          "required": false
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "codicil",
        "name": "Codicil to Will",
        "blurb": "Amendment to an existing will",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "testatorName",
          "label": "Testator Full Name",
          "required": true
         },
         {
          "key": "originalWillDate",
          "label": "Original Will Date",
          "required": true
         },
         {
          "key": "changes",
          "label": "Changes Made by This Codicil",
          "required": true
         },
         {
          "key": "executionDate",
          "label": "Execution Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Family Law",
      "templates": [
       {
        "key": "prenuptial-agreement",
        "name": "Prenuptial Agreement",
        "blurb": "Property terms agreed before marriage",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "partyOne",
          "label": "Party One",
          "required": true
         },
         {
          "key": "partyTwo",
          "label": "Party Two",
          "required": true
         },
         {
          "key": "weddingDate",
          "label": "Planned Wedding Date",
          "required": true
         },
         {
          "key": "propertyTerms",
          "label": "Property / Financial Terms",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "separation-agreement",
        "name": "Separation Agreement",
        "blurb": "Terms for spouses living apart",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "spouseOne",
          "label": "Spouse One",
          "required": true
         },
         {
          "key": "spouseTwo",
          "label": "Spouse Two",
          "required": true
         },
         {
          "key": "separationDate",
          "label": "Separation Date",
          "required": true
         },
         {
          "key": "terms",
          "label": "Agreed Terms",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "child-custody-agreement",
        "name": "Child Custody Agreement",
        "blurb": "Custody and visitation terms",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "parentOne",
          "label": "Parent One",
          "required": true
         },
         {
          "key": "parentTwo",
          "label": "Parent Two",
          "required": true
         },
         {
          "key": "childrenNames",
          "label": "Children (Names and Birth Years)",
          "required": true
         },
         {
          "key": "custodyTerms",
          "label": "Custody and Visitation Terms",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "child-support-agreement",
        "name": "Child Support Agreement",
        "blurb": "Agreed child support terms",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "payingParent",
          "label": "Paying Parent",
          "required": true
         },
         {
          "key": "receivingParent",
          "label": "Receiving Parent",
          "required": true
         },
         {
          "key": "childrenNames",
          "label": "Children (Names and Birth Years)",
          "required": true
         },
         {
          "key": "supportAmount",
          "label": "Support Amount and Frequency",
          "required": true
         },
         {
          "key": "startDate",
          "label": "Start Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "divorce-petition",
        "name": "Divorce Petition (Draft)",
        "blurb": "Petition to dissolve a marriage",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "petitionerName",
          "label": "Petitioner Name",
          "required": true
         },
         {
          "key": "respondentName",
          "label": "Respondent Name",
          "required": true
         },
         {
          "key": "marriageDate",
          "label": "Date of Marriage",
          "required": true
         },
         {
          "key": "marriagePlace",
          "label": "Place of Marriage",
          "required": true
         },
         {
          "key": "grounds",
          "label": "Grounds",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "adoption-consent",
        "name": "Adoption Consent Form",
        "blurb": "Consent to an adoption",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "consentingParent",
          "label": "Consenting Parent / Guardian",
          "required": true
         },
         {
          "key": "childName",
          "label": "Child's Name",
          "required": true
         },
         {
          "key": "adoptiveParents",
          "label": "Adoptive Parent(s)",
          "required": true
         },
         {
          "key": "consentDate",
          "label": "Consent Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Employment",
      "templates": [
       {
        "key": "employment-offer-letter",
        "name": "Employment Offer Letter",
        "blurb": "Written offer of employment",
        "fastPath": true,
        "draftable": true,
        "fields": [
         {
          "key": "companyName",
          "label": "Company Name",
          "required": true
         },
         {
          "key": "candidateName",
          "label": "Candidate Name",
          "required": true
         },
         {
          "key": "positionTitle",
          "label": "Position Title",
          "required": true
         },
         {
          "key": "startDate",
          "label": "Start Date",
          "required": true
         },
         {
          "key": "compensation",
          "label": "Compensation (Salary / Rate)",
          "required": true
         },
         {
          "key": "workLocation",
          "label": "Work Location",
          "required": true
         },
         {
          "key": "hiringManager",
          "label": "Hiring Manager",
          "required": false
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "employment-agreement",
        "name": "Employment Agreement",
        "blurb": "Full terms of employment",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "employerName",
          "label": "Employer Name",
          "required": true
         },
         {
          "key": "employeeName",
          "label": "Employee Name",
          "required": true
         },
         {
          "key": "positionTitle",
          "label": "Position Title",
          "required": true
         },
         {
          "key": "startDate",
          "label": "Start Date",
          "required": true
         },
         {
          "key": "compensation",
          "label": "Compensation",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "non-compete-agreement",
        "name": "Non-Compete Agreement",
        "blurb": "Post-employment competition limits",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "employerName",
          "label": "Employer Name",
          "required": true
         },
         {
          "key": "employeeName",
          "label": "Employee Name",
          "required": true
         },
         {
          "key": "restrictedActivities",
          "label": "Restricted Activities",
          "required": true
         },
         {
          "key": "duration",
          "label": "Restricted Period",
          "required": true
         },
         {
          "key": "geographicScope",
          "label": "Geographic Scope",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "severance-agreement",
        "name": "Severance Agreement",
        "blurb": "Separation and severance terms",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "employerName",
          "label": "Employer Name",
          "required": true
         },
         {
          "key": "employeeName",
          "label": "Employee Name",
          "required": true
         },
         {
          "key": "lastDay",
          "label": "Last Day of Employment",
          "required": true
         },
         {
          "key": "severanceTerms",
          "label": "Severance Terms",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "termination-letter",
        "name": "Termination Letter",
        "blurb": "Written notice ending employment",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "employerName",
          "label": "Employer Name",
          "required": true
         },
         {
          "key": "employeeName",
          "label": "Employee Name",
          "required": true
         },
         {
          "key": "lastDay",
          "label": "Last Day of Employment",
          "required": true
         },
         {
          "key": "reason",
          "label": "Reason for Termination",
          "required": true
         },
         {
          "key": "letterDate",
          "label": "Letter Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "handbook-acknowledgment",
        "name": "Employee Handbook Acknowledgment",
        "blurb": "Employee receipt and acknowledgment of the handbook",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "employerName",
          "label": "Employer Name",
          "required": true
         },
         {
          "key": "employeeName",
          "label": "Employee Name",
          "required": true
         },
         {
          "key": "handbookVersion",
          "label": "Handbook Version / Date",
          "required": true
         },
         {
          "key": "acknowledgmentDate",
          "label": "Date Signed",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Intellectual Property",
      "templates": [
       {
        "key": "trademark-license",
        "name": "Trademark License Agreement",
        "blurb": "License to use a trademark",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "licensorName",
          "label": "Licensor Name",
          "required": true
         },
         {
          "key": "licenseeName",
          "label": "Licensee Name",
          "required": true
         },
         {
          "key": "trademark",
          "label": "Trademark / Registration No.",
          "required": true
         },
         {
          "key": "licensedUse",
          "label": "Licensed Use",
          "required": true
         },
         {
          "key": "royaltyTerms",
          "label": "Royalty / Fee Terms",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "copyright-assignment",
        "name": "Copyright Assignment",
        "blurb": "Transfer of copyright ownership",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "assignorName",
          "label": "Assignor Name",
          "required": true
         },
         {
          "key": "assigneeName",
          "label": "Assignee Name",
          "required": true
         },
         {
          "key": "workDescription",
          "label": "Work Being Assigned",
          "required": true
         },
         {
          "key": "consideration",
          "label": "Consideration",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "patent-assignment",
        "name": "Patent Assignment",
        "blurb": "Transfer of patent rights",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "assignorName",
          "label": "Assignor Name",
          "required": true
         },
         {
          "key": "assigneeName",
          "label": "Assignee Name",
          "required": true
         },
         {
          "key": "patentDescription",
          "label": "Patent / Application No.",
          "required": true
         },
         {
          "key": "consideration",
          "label": "Consideration",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "ip-assignment-agreement",
        "name": "Intellectual Property Assignment Agreement",
        "blurb": "General assignment of intellectual property",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "assignorName",
          "label": "Assignor Name",
          "required": true
         },
         {
          "key": "assigneeName",
          "label": "Assignee Name",
          "required": true
         },
         {
          "key": "ipDescription",
          "label": "Intellectual Property Being Assigned",
          "required": true
         },
         {
          "key": "consideration",
          "label": "Consideration",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "cease-and-desist-letter",
        "name": "Cease and Desist Letter",
        "blurb": "Demand to stop infringing activity",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "senderName",
          "label": "Sender Name",
          "required": true
         },
         {
          "key": "recipientName",
          "label": "Recipient Name",
          "required": true
         },
         {
          "key": "rightsDescription",
          "label": "Right Being Asserted",
          "required": true
         },
         {
          "key": "infringingActivity",
          "label": "Infringing Activity",
          "required": true
         },
         {
          "key": "demandByDate",
          "label": "Stop By Date",
          "required": true
         },
         {
          "key": "letterDate",
          "label": "Letter Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "software-license-agreement",
        "name": "Software License Agreement",
        "blurb": "Terms for licensing software",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "licensorName",
          "label": "Licensor Name",
          "required": true
         },
         {
          "key": "licenseeName",
          "label": "Licensee Name",
          "required": true
         },
         {
          "key": "softwareDescription",
          "label": "Software",
          "required": true
         },
         {
          "key": "licenseScope",
          "label": "License Scope",
          "required": true
         },
         {
          "key": "fees",
          "label": "Fees",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Litigation",
      "templates": [
       {
        "key": "demand-letter",
        "name": "Demand Letter",
        "blurb": "Formal demand before suit",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "senderName",
          "label": "Sender Name",
          "required": true
         },
         {
          "key": "recipientName",
          "label": "Recipient Name",
          "required": true
         },
         {
          "key": "subjectMatter",
          "label": "Subject Matter",
          "required": true
         },
         {
          "key": "amountDemanded",
          "label": "Amount Demanded",
          "required": false
         },
         {
          "key": "demandByDate",
          "label": "Respond / Pay By Date",
          "required": true
         },
         {
          "key": "letterDate",
          "label": "Letter Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "civil-complaint",
        "name": "Civil Complaint (Draft)",
        "blurb": "Draft pleading to start a civil case",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "plaintiffName",
          "label": "Plaintiff Name",
          "required": true
         },
         {
          "key": "defendantName",
          "label": "Defendant Name",
          "required": true
         },
         {
          "key": "courtName",
          "label": "Court",
          "required": true
         },
         {
          "key": "claimsSummary",
          "label": "Summary of Claims",
          "required": true
         },
         {
          "key": "reliefSought",
          "label": "Relief Sought",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "small-claims-statement",
        "name": "Small Claims Statement of Claim",
        "blurb": "Statement of claim for small claims court",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "claimantName",
          "label": "Claimant Name",
          "required": true
         },
         {
          "key": "respondentName",
          "label": "Respondent Name",
          "required": true
         },
         {
          "key": "amountClaimed",
          "label": "Amount Claimed",
          "required": true
         },
         {
          "key": "factsSummary",
          "label": "Summary of Facts",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "settlement-agreement",
        "name": "Settlement Agreement",
        "blurb": "Terms settling a dispute",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "partyOne",
          "label": "Party One",
          "required": true
         },
         {
          "key": "partyTwo",
          "label": "Party Two",
          "required": true
         },
         {
          "key": "disputeDescription",
          "label": "Dispute Being Settled",
          "required": true
         },
         {
          "key": "settlementTerms",
          "label": "Settlement Terms",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "general-release",
        "name": "General Release of Claims",
        "blurb": "Release of claims between parties",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "releasorName",
          "label": "Releasor Name",
          "required": true
         },
         {
          "key": "releaseeName",
          "label": "Releasee Name",
          "required": true
         },
         {
          "key": "claimsDescription",
          "label": "Claims Being Released",
          "required": true
         },
         {
          "key": "consideration",
          "label": "Consideration",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "motion",
        "name": "Motion (Draft)",
        "blurb": "Draft motion to a court",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "movingParty",
          "label": "Moving Party",
          "required": true
         },
         {
          "key": "opposingParty",
          "label": "Opposing Party",
          "required": true
         },
         {
          "key": "courtName",
          "label": "Court",
          "required": true
         },
         {
          "key": "caseNumber",
          "label": "Case Number",
          "required": false
         },
         {
          "key": "reliefRequested",
          "label": "Relief Requested",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Financial",
      "templates": [
       {
        "key": "promissory-note",
        "name": "Promissory Note",
        "blurb": "Written promise to repay a sum of money",
        "fastPath": true,
        "draftable": true,
        "fields": [
         {
          "key": "borrowerName",
          "label": "Borrower Name",
          "required": true
         },
         {
          "key": "lenderName",
          "label": "Lender Name",
          "required": true
         },
         {
          "key": "principalAmount",
          "label": "Principal Amount",
          "required": true
         },
         {
          "key": "interestRate",
          "label": "Interest Rate",
          "required": false
         },
         {
          "key": "issueDate",
          "label": "Issue Date",
          "required": true
         },
         {
          "key": "maturityDate",
          "label": "Maturity / Due Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "loan-agreement",
        "name": "Loan Agreement",
        "blurb": "Full terms of a loan between parties",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "borrowerName",
          "label": "Borrower Name",
          "required": true
         },
         {
          "key": "lenderName",
          "label": "Lender Name",
          "required": true
         },
         {
          "key": "principalAmount",
          "label": "Principal Amount",
          "required": true
         },
         {
          "key": "interestRate",
          "label": "Interest Rate",
          "required": false
         },
         {
          "key": "repaymentTerms",
          "label": "Repayment Terms",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "personal-guaranty",
        "name": "Personal Guaranty",
        "blurb": "Personal guarantee of another's obligation",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "guarantorName",
          "label": "Guarantor Name",
          "required": true
         },
         {
          "key": "debtorName",
          "label": "Debtor Name",
          "required": true
         },
         {
          "key": "creditorName",
          "label": "Creditor Name",
          "required": true
         },
         {
          "key": "obligationDescription",
          "label": "Obligation Guaranteed",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "security-agreement",
        "name": "Security Agreement",
        "blurb": "Collateral securing an obligation",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "debtorName",
          "label": "Debtor Name",
          "required": true
         },
         {
          "key": "securedParty",
          "label": "Secured Party",
          "required": true
         },
         {
          "key": "collateralDescription",
          "label": "Collateral Description",
          "required": true
         },
         {
          "key": "obligationDescription",
          "label": "Obligation Secured",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "debt-settlement-agreement",
        "name": "Debt Settlement Agreement",
        "blurb": "Terms settling an outstanding debt",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "debtorName",
          "label": "Debtor Name",
          "required": true
         },
         {
          "key": "creditorName",
          "label": "Creditor Name",
          "required": true
         },
         {
          "key": "originalAmount",
          "label": "Original Amount Owed",
          "required": true
         },
         {
          "key": "settlementAmount",
          "label": "Settlement Amount",
          "required": true
         },
         {
          "key": "paymentTerms",
          "label": "Payment Terms",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "payment-plan-agreement",
        "name": "Payment Plan Agreement",
        "blurb": "Installment plan for an amount owed",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "payerName",
          "label": "Payer Name",
          "required": true
         },
         {
          "key": "payeeName",
          "label": "Payee Name",
          "required": true
         },
         {
          "key": "totalAmount",
          "label": "Total Amount Owed",
          "required": true
         },
         {
          "key": "installmentTerms",
          "label": "Installment Terms",
          "required": true
         },
         {
          "key": "startDate",
          "label": "First Payment Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Corporate Governance",
      "templates": [
       {
        "key": "board-resolution",
        "name": "Board Resolution",
        "blurb": "Recorded decision of a board of directors",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "companyName",
          "label": "Company Name",
          "required": true
         },
         {
          "key": "resolutionSubject",
          "label": "Subject of Resolution",
          "required": true
         },
         {
          "key": "resolutionText",
          "label": "Resolution Adopted",
          "required": true
         },
         {
          "key": "meetingDate",
          "label": "Meeting / Adoption Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "corporate-bylaws",
        "name": "Corporate Bylaws",
        "blurb": "Internal rules governing a corporation",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "companyName",
          "label": "Company Name",
          "required": true
         },
         {
          "key": "registeredOffice",
          "label": "Registered Office",
          "required": true
         },
         {
          "key": "fiscalYearEnd",
          "label": "Fiscal Year End",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "meeting-minutes",
        "name": "Meeting Minutes",
        "blurb": "Minutes of a board or shareholder meeting",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "companyName",
          "label": "Company Name",
          "required": true
         },
         {
          "key": "meetingType",
          "label": "Meeting Type (Board / Shareholders)",
          "required": true
         },
         {
          "key": "meetingDate",
          "label": "Meeting Date",
          "required": true
         },
         {
          "key": "attendees",
          "label": "Attendees",
          "required": true
         },
         {
          "key": "mattersDecided",
          "label": "Matters Decided",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "written-consent-in-lieu",
        "name": "Written Consent in Lieu of Meeting",
        "blurb": "Action taken by written consent instead of a meeting",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "companyName",
          "label": "Company Name",
          "required": true
         },
         {
          "key": "actingBody",
          "label": "Acting Body (Board / Shareholders)",
          "required": true
         },
         {
          "key": "actionTaken",
          "label": "Action Taken",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "conflict-of-interest-policy",
        "name": "Conflict of Interest Policy",
        "blurb": "Policy governing conflicts of interest",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "organizationName",
          "label": "Organization Name",
          "required": true
         },
         {
          "key": "coveredPersons",
          "label": "Covered Persons",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "secretary-certificate",
        "name": "Corporate Secretary's Certificate",
        "blurb": "Certification of corporate records and authority",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "companyName",
          "label": "Company Name",
          "required": true
         },
         {
          "key": "secretaryName",
          "label": "Corporate Secretary Name",
          "required": true
         },
         {
          "key": "certifiedMatters",
          "label": "Matters Certified",
          "required": true
         },
         {
          "key": "certificateDate",
          "label": "Certificate Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "HR & Labor",
      "templates": [
       {
        "key": "disciplinary-action-form",
        "name": "Disciplinary Action Form",
        "blurb": "Record of a disciplinary action",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "employerName",
          "label": "Employer Name",
          "required": true
         },
         {
          "key": "employeeName",
          "label": "Employee Name",
          "required": true
         },
         {
          "key": "incidentDate",
          "label": "Incident Date",
          "required": true
         },
         {
          "key": "conductDescription",
          "label": "Conduct at Issue",
          "required": true
         },
         {
          "key": "actionTaken",
          "label": "Action Taken",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "performance-improvement-plan",
        "name": "Performance Improvement Plan",
        "blurb": "Plan to address performance issues",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "employerName",
          "label": "Employer Name",
          "required": true
         },
         {
          "key": "employeeName",
          "label": "Employee Name",
          "required": true
         },
         {
          "key": "managerName",
          "label": "Manager Name",
          "required": true
         },
         {
          "key": "concerns",
          "label": "Performance Concerns",
          "required": true
         },
         {
          "key": "improvementPeriod",
          "label": "Improvement Period",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "leave-of-absence-request",
        "name": "Leave of Absence Request",
        "blurb": "Employee request for leave",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "employeeName",
          "label": "Employee Name",
          "required": true
         },
         {
          "key": "employerName",
          "label": "Employer Name",
          "required": true
         },
         {
          "key": "leaveType",
          "label": "Type of Leave",
          "required": true
         },
         {
          "key": "startDate",
          "label": "Leave Start Date",
          "required": true
         },
         {
          "key": "endDate",
          "label": "Leave End Date",
          "required": true
         },
         {
          "key": "reason",
          "label": "Reason (General)",
          "required": true
         }
        ]
       },
       {
        "key": "remote-work-agreement",
        "name": "Remote Work Agreement",
        "blurb": "Terms for working remotely",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "employerName",
          "label": "Employer Name",
          "required": true
         },
         {
          "key": "employeeName",
          "label": "Employee Name",
          "required": true
         },
         {
          "key": "workLocation",
          "label": "Remote Work Location",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "terms",
          "label": "Key Terms",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "employee-warning-notice",
        "name": "Employee Warning Notice",
        "blurb": "Written warning to an employee",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "employerName",
          "label": "Employer Name",
          "required": true
         },
         {
          "key": "employeeName",
          "label": "Employee Name",
          "required": true
         },
         {
          "key": "warningDate",
          "label": "Warning Date",
          "required": true
         },
         {
          "key": "conductDescription",
          "label": "Conduct at Issue",
          "required": true
         },
         {
          "key": "expectedCorrection",
          "label": "Expected Correction",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "overtime-authorization-form",
        "name": "Overtime Authorization Form",
        "blurb": "Pre-approval of overtime work",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "employerName",
          "label": "Employer Name",
          "required": true
         },
         {
          "key": "employeeName",
          "label": "Employee Name",
          "required": true
         },
         {
          "key": "managerName",
          "label": "Authorizing Manager",
          "required": true
         },
         {
          "key": "overtimePeriod",
          "label": "Overtime Period / Hours",
          "required": true
         },
         {
          "key": "reason",
          "label": "Business Reason",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Personal Legal",
      "templates": [
       {
        "key": "general-affidavit",
        "name": "General Affidavit",
        "blurb": "Sworn written statement of facts",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "affiantName",
          "label": "Affiant (Person Swearing) Name",
          "required": true
         },
         {
          "key": "facts",
          "label": "Statement of Facts",
          "required": true
         },
         {
          "key": "executionDate",
          "label": "Execution Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "name-change-petition",
        "name": "Name Change Petition (Draft)",
        "blurb": "Petition to change a legal name",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "currentName",
          "label": "Current Legal Name",
          "required": true
         },
         {
          "key": "requestedName",
          "label": "Requested New Name",
          "required": true
         },
         {
          "key": "reason",
          "label": "Reason for Change",
          "required": true
         },
         {
          "key": "dateOfBirth",
          "label": "Date of Birth",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "minor-travel-consent",
        "name": "Minor Travel Consent",
        "blurb": "Consent for a child to travel",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "parentName",
          "label": "Parent / Guardian Name",
          "required": true
         },
         {
          "key": "childName",
          "label": "Child's Name",
          "required": true
         },
         {
          "key": "travelCompanion",
          "label": "Traveling With (Name)",
          "required": true
         },
         {
          "key": "destination",
          "label": "Destination",
          "required": true
         },
         {
          "key": "travelDates",
          "label": "Travel Dates",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "medical-records-authorization",
        "name": "Medical Records Authorization",
        "blurb": "Authorization to release medical records",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "patientName",
          "label": "Patient Name",
          "required": true
         },
         {
          "key": "recordsHolder",
          "label": "Provider / Records Holder",
          "required": true
         },
         {
          "key": "releaseTo",
          "label": "Release Records To",
          "required": true
         },
         {
          "key": "recordsDescription",
          "label": "Records Covered",
          "required": true
         },
         {
          "key": "authorizationDate",
          "label": "Date Signed",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "formal-complaint-letter",
        "name": "Formal Complaint Letter",
        "blurb": "Written complaint to a business or agency",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "senderName",
          "label": "Sender Name",
          "required": true
         },
         {
          "key": "recipientName",
          "label": "Recipient Name / Organization",
          "required": true
         },
         {
          "key": "subjectMatter",
          "label": "Subject of Complaint",
          "required": true
         },
         {
          "key": "factsSummary",
          "label": "Summary of Facts",
          "required": true
         },
         {
          "key": "resolutionRequested",
          "label": "Resolution Requested",
          "required": true
         },
         {
          "key": "letterDate",
          "label": "Letter Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "personal-property-memo",
        "name": "Personal Property Memorandum",
        "blurb": "Memo assigning tangible personal property",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "ownerName",
          "label": "Owner Name",
          "required": true
         },
         {
          "key": "assignments",
          "label": "Items and Who Receives Them",
          "required": true
         },
         {
          "key": "memoDate",
          "label": "Memo Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Tax",
      "templates": [
       {
        "key": "tax-engagement-letter",
        "name": "Tax Engagement Letter",
        "blurb": "Terms for tax preparation services",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "preparerName",
          "label": "Preparer / Firm Name",
          "required": true
         },
         {
          "key": "clientName",
          "label": "Client Name",
          "required": true
         },
         {
          "key": "taxYears",
          "label": "Tax Year(s) Covered",
          "required": true
         },
         {
          "key": "services",
          "label": "Services Included",
          "required": true
         },
         {
          "key": "fees",
          "label": "Fees",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "tax-power-of-attorney",
        "name": "Tax Power of Attorney Worksheet",
        "blurb": "Information worksheet for a tax power of attorney (e.g. Form 2848)",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "taxpayerName",
          "label": "Taxpayer Name",
          "required": true
         },
         {
          "key": "representativeName",
          "label": "Representative Name",
          "required": true
         },
         {
          "key": "taxMatters",
          "label": "Tax Matters / Forms / Years",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "extension-request-letter",
        "name": "Tax Extension Request Letter",
        "blurb": "Letter requesting filing extension time",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "taxpayerName",
          "label": "Taxpayer Name",
          "required": true
         },
         {
          "key": "taxYear",
          "label": "Tax Year",
          "required": true
         },
         {
          "key": "reason",
          "label": "Reason for Extension",
          "required": true
         },
         {
          "key": "requestedUntil",
          "label": "Extension Requested Until",
          "required": true
         },
         {
          "key": "letterDate",
          "label": "Letter Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "installment-agreement-proposal",
        "name": "IRS Installment Agreement Proposal",
        "blurb": "Proposal to pay a tax balance in installments",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "taxpayerName",
          "label": "Taxpayer Name",
          "required": true
         },
         {
          "key": "balanceOwed",
          "label": "Balance Owed",
          "required": true
         },
         {
          "key": "proposedPayment",
          "label": "Proposed Monthly Payment",
          "required": true
         },
         {
          "key": "taxYears",
          "label": "Tax Year(s)",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "penalty-abatement-letter",
        "name": "Penalty Abatement Request Letter",
        "blurb": "Letter asking a tax authority to remove penalties",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "taxpayerName",
          "label": "Taxpayer Name",
          "required": true
         },
         {
          "key": "taxYear",
          "label": "Tax Year",
          "required": true
         },
         {
          "key": "penaltyDescription",
          "label": "Penalty at Issue",
          "required": true
         },
         {
          "key": "reasonableCause",
          "label": "Reasonable Cause Statement",
          "required": true
         },
         {
          "key": "letterDate",
          "label": "Letter Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "offer-in-compromise-summary",
        "name": "Offer in Compromise Summary",
        "blurb": "Summary worksheet for an offer in compromise",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "taxpayerName",
          "label": "Taxpayer Name",
          "required": true
         },
         {
          "key": "balanceOwed",
          "label": "Balance Owed",
          "required": true
         },
         {
          "key": "offerAmount",
          "label": "Amount Offered",
          "required": true
         },
         {
          "key": "taxYears",
          "label": "Tax Year(s)",
          "required": true
         },
         {
          "key": "groundsSummary",
          "label": "Grounds Summary",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Healthcare",
      "templates": [
       {
        "key": "hipaa-authorization",
        "name": "HIPAA Authorization",
        "blurb": "Authorization to use / disclose health information",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "patientName",
          "label": "Patient Name",
          "required": true
         },
         {
          "key": "disclosingParty",
          "label": "Provider Disclosing Records",
          "required": true
         },
         {
          "key": "recipientName",
          "label": "Recipient of the Information",
          "required": true
         },
         {
          "key": "informationDescription",
          "label": "Information Covered",
          "required": true
         },
         {
          "key": "expirationDate",
          "label": "Authorization Expires",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "medical-treatment-consent",
        "name": "Medical Treatment Consent",
        "blurb": "Consent to medical treatment",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "patientName",
          "label": "Patient Name",
          "required": true
         },
         {
          "key": "providerName",
          "label": "Treating Provider",
          "required": true
         },
         {
          "key": "treatmentDescription",
          "label": "Treatment Consented To",
          "required": true
         },
         {
          "key": "consentDate",
          "label": "Date Signed",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "informed-consent-form",
        "name": "Informed Consent Form",
        "blurb": "Consent after disclosure of risks",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "patientName",
          "label": "Patient Name",
          "required": true
         },
         {
          "key": "providerName",
          "label": "Provider",
          "required": true
         },
         {
          "key": "procedureDescription",
          "label": "Procedure / Treatment",
          "required": true
         },
         {
          "key": "risksSummary",
          "label": "Risks Disclosed",
          "required": true
         },
         {
          "key": "consentDate",
          "label": "Date Signed",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "health-care-proxy",
        "name": "Health Care Proxy",
        "blurb": "Designation of a health care decision-maker",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "principalName",
          "label": "Principal Name",
          "required": true
         },
         {
          "key": "agentName",
          "label": "Health Care Agent Name",
          "required": true
         },
         {
          "key": "alternateAgent",
          "label": "Alternate Agent",
          "required": false
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "telehealth-consent",
        "name": "Telehealth Consent",
        "blurb": "Consent to treatment by telehealth",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "patientName",
          "label": "Patient Name",
          "required": true
         },
         {
          "key": "providerName",
          "label": "Provider / Practice",
          "required": true
         },
         {
          "key": "consentDate",
          "label": "Date Signed",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "medical-records-request",
        "name": "Medical Records Request",
        "blurb": "Request for a copy of medical records",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "patientName",
          "label": "Patient Name",
          "required": true
         },
         {
          "key": "providerName",
          "label": "Provider / Records Holder",
          "required": true
         },
         {
          "key": "recordsDescription",
          "label": "Records Requested",
          "required": true
         },
         {
          "key": "deliverTo",
          "label": "Deliver Records To",
          "required": true
         },
         {
          "key": "requestDate",
          "label": "Request Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Notary & Certifications",
      "templates": [
       {
        "key": "acknowledgment-certificate",
        "name": "Acknowledgment Certificate",
        "blurb": "Notarial certificate of acknowledgment",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "signerName",
          "label": "Signer Name",
          "required": true
         },
         {
          "key": "documentDescription",
          "label": "Document Acknowledged",
          "required": true
         },
         {
          "key": "acknowledgmentDate",
          "label": "Date of Acknowledgment",
          "required": true
         },
         {
          "key": "notaryName",
          "label": "Notary Name",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "jurat-certificate",
        "name": "Jurat Certificate",
        "blurb": "Notarial certificate for a sworn statement",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "signerName",
          "label": "Signer Name",
          "required": true
         },
         {
          "key": "documentDescription",
          "label": "Document Sworn To",
          "required": true
         },
         {
          "key": "juratDate",
          "label": "Date Sworn",
          "required": true
         },
         {
          "key": "notaryName",
          "label": "Notary Name",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "certified-copy-attestation",
        "name": "Certified Copy Attestation",
        "blurb": "Attestation that a copy is true and complete",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "attestorName",
          "label": "Attestor Name",
          "required": true
         },
         {
          "key": "documentDescription",
          "label": "Document Copied",
          "required": true
         },
         {
          "key": "originalHolder",
          "label": "Holder of the Original",
          "required": true
         },
         {
          "key": "attestationDate",
          "label": "Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "apostille-request",
        "name": "Apostille Request Form",
        "blurb": "Request to apostille a document",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "requesterName",
          "label": "Requester Name",
          "required": true
         },
         {
          "key": "documentDescription",
          "label": "Document to Apostille",
          "required": true
         },
         {
          "key": "destinationCountry",
          "label": "Country Where the Document Will Be Used",
          "required": true
         },
         {
          "key": "requestDate",
          "label": "Request Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "sworn-statement",
        "name": "Sworn Statement",
        "blurb": "Statement sworn before an authorized officer",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "declarantName",
          "label": "Declarant Name",
          "required": true
         },
         {
          "key": "statement",
          "label": "Statement of Facts",
          "required": true
         },
         {
          "key": "executionDate",
          "label": "Date Sworn",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "witness-attestation",
        "name": "Witness Attestation",
        "blurb": "Witness certification of a signing",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "witnessName",
          "label": "Witness Name",
          "required": true
         },
         {
          "key": "signerName",
          "label": "Signer Name",
          "required": true
         },
         {
          "key": "documentDescription",
          "label": "Document Witnessed",
          "required": true
         },
         {
          "key": "signingDate",
          "label": "Date of Signing",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       }
      ]
     }
    ],
    "documents": [],
    "aiRequests": [],
    "aiConfigured": true,
    "aiNotice": "",
    "ok": true
   }
  },
  "POST /api/app/law/documents (research note)": {
   "status": 201,
   "body": {
    "ok": true,
    "message": "Saved.",
    "document": {
     "id": 1,
     "kind": "research",
     "title": "Capture Research Note",
     "category": "",
     "templateKey": "",
     "jurisdiction": "",
     "question": "Capture seed question?",
     "notes": "",
     "body": "",
     "fields": {},
     "origin": "",
     "sources": "",
     "createdBy": 1,
     "createdByName": "OwnerCap6",
     "createdAt": 1793749645279,
     "updatedAt": 1793749645279
    }
   }
  },
  "POST /api/app/law/documents (non-fast refusal)": {
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "That template is not a standard form — draft it with the legal assistant from Doc Gen instead."
   }
  },
  "POST /api/app/law/documents": {
   "status": 201,
   "body": {
    "ok": true,
    "message": "Saved.",
    "document": {
     "id": 2,
     "kind": "document",
     "title": "Capture Fictional LLC — Operating Agreement",
     "category": "Business Formation",
     "templateKey": "operating-agreement",
     "jurisdiction": "",
     "question": "",
     "notes": "",
     "body": "OPERATING AGREEMENT\n\nThis Operating Agreement (the \"Agreement\") is entered into by Capture Fictional LLC (the \"Company\"), a limited liability company formed under the laws of Missouri.\n\n1. FORMATION\nThe Company was formed as a limited liability company under the laws of Missouri. The Company shall operate in accordance with this Agreement and the applicable laws of that jurisdiction.\n\n2. PURPOSE\nThe purpose of the Company is: Seed governance\n\n3. MANAGEMENT\nThe Company shall be managed by its Managing Member, Pat Seed, who has the authority to bind the Company in its ordinary business and to act on its behalf in accordance with this Agreement.\n\n4. EFFECTIVE DATE\nThis Agreement is effective as of 2026-11-03.\n\nSIGNATURES\n\nCompany: Capture Fictional LLC\n\nManaging Member: Pat Seed\nSignature: ______________________________    Date: ______________\n\nMember: ______________________________\nSignature: ______________________________    Date: ______________\n\nDRAFT — prepared with Dogg and Associates drafting aids. For review by a licensed attorney before use.\nHave an attorney review before relying on this document.\nLegal intelligence, not legal advice.",
     "fields": {
      "llcName": "Capture Fictional LLC",
      "managingMember": "Pat Seed",
      "jurisdiction": "Missouri",
      "effectiveDate": "2026-11-03",
      "purpose": "Seed governance"
     },
     "origin": "standard_form",
     "sources": "",
     "createdBy": 1,
     "createdByName": "OwnerCap6",
     "createdAt": 1793749645485,
     "updatedAt": 1793749645485
    }
   }
  },
  "PATCH /api/app/law/documents/{id}": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Saved.",
    "document": {
     "id": 1,
     "kind": "research",
     "title": "Capture Research Note Edited",
     "category": "",
     "templateKey": "",
     "jurisdiction": "",
     "question": "Capture seed question?",
     "notes": "",
     "body": "",
     "fields": {},
     "origin": "",
     "sources": "",
     "createdBy": 1,
     "createdByName": "OwnerCap6",
     "createdAt": 1793749645279,
     "updatedAt": 1793749645619
    }
   }
  },
  "POST /api/app/law/research (bad jurisdiction)": {
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Pick a jurisdiction from the list."
   }
  },
  "POST /api/app/law/research": {
   "status": 201,
   "body": {
    "ok": true,
    "request": {
     "id": 1,
     "kind": "research",
     "status": "queued",
     "failureClass": "",
     "failureNote": "",
     "resultDocumentId": null,
     "createdAt": 1793749645862,
     "completedAt": null,
     "label": "What is a fictional statute?"
    }
   }
  },
  "POST /api/app/law/docgen": {
   "status": 201,
   "body": {
    "ok": true,
    "request": {
     "id": 2,
     "kind": "docgen",
     "status": "queued",
     "failureClass": "",
     "failureNote": "",
     "resultDocumentId": null,
     "createdAt": 1793749646169,
     "completedAt": null,
     "label": "Consulting Agreement"
    }
   }
  },
  "GET /api/app/law": {
   "status": 200,
   "body": {
    "practiceAreas": [
     {
      "name": "Personal Law",
      "topics": [
       "Divorce proceedings",
       "Child custody",
       "Estate planning",
       "Personal injury claims",
       "Immigration status",
       "Adoption process"
      ]
     },
     {
      "name": "Business Law",
      "topics": [
       "Forming an LLC",
       "Shareholder rights",
       "Trademark filing",
       "Contract disputes",
       "M&A due diligence",
       "Business tax liability"
      ]
     },
     {
      "name": "Civil Law",
      "topics": [
       "Property boundaries",
       "Small claims",
       "Landlord tenant",
       "Defamation",
       "Civil procedure",
       "Breach of contract"
      ]
     },
     {
      "name": "Criminal Law",
      "topics": [
       "Miranda rights",
       "Bail hearings",
       "Plea bargains",
       "Search & seizure",
       "Evidence rules",
       "Expungement"
      ]
     },
     {
      "name": "Labor Law",
      "topics": [
       "Wrongful termination",
       "Wage and hour",
       "Workplace discrimination",
       "Workers compensation",
       "Union rights",
       "FMLA leave"
      ]
     }
    ],
    "categories": [
     {
      "name": "Business Formation",
      "templates": [
       {
        "key": "articles-of-incorporation",
        "name": "Articles of Incorporation",
        "blurb": "Charter establishing a corporation",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "corporationName",
          "label": "Corporation Name",
          "required": true
         },
         {
          "key": "registeredAgent",
          "label": "Registered Agent",
          "required": true
         },
         {
          "key": "registeredAddress",
          "label": "Registered Office Address",
          "required": true
         },
         {
          "key": "sharesAuthorized",
          "label": "Authorized Shares",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "operating-agreement",
        "name": "Operating Agreement",
        "blurb": "LLC internal governance terms",
        "fastPath": true,
        "draftable": true,
        "fields": [
         {
          "key": "llcName",
          "label": "LLC Name",
          "required": true
         },
         {
          "key": "managingMember",
          "label": "Managing Member",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "amount",
          "label": "Amount / Consideration",
          "required": false
         },
         {
          "key": "purpose",
          "label": "Purpose / Subject Matter",
          "required": true
         }
        ]
       },
       {
        "key": "partnership-agreement",
        "name": "Partnership Agreement",
        "blurb": "Terms between business partners",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "partnershipName",
          "label": "Partnership Name",
          "required": true
         },
         {
          "key": "partnerNames",
          "label": "Partner Names",
          "required": true
         },
         {
          "key": "businessPurpose",
          "label": "Business Purpose",
          "required": true
         },
         {
          "key": "capitalContributions",
          "label": "Capital Contributions",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "shareholders-agreement",
        "name": "Shareholders Agreement",
        "blurb": "Rights and obligations of shareholders",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "companyName",
          "label": "Company Name",
          "required": true
         },
         {
          "key": "shareholderNames",
          "label": "Shareholder Names",
          "required": true
         },
         {
          "key": "shareClasses",
          "label": "Share Classes / Holdings",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "business-license-application",
        "name": "Business License Application",
        "blurb": "Application to operate a business",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "businessName",
          "label": "Business Name",
          "required": true
         },
         {
          "key": "ownerName",
          "label": "Owner Name",
          "required": true
         },
         {
          "key": "businessAddress",
          "label": "Business Address",
          "required": true
         },
         {
          "key": "businessType",
          "label": "Type of Business",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "dba-registration",
        "name": "DBA Registration",
        "blurb": "Doing-business-as fictitious name filing",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "ownerName",
          "label": "Owner / Registrant Name",
          "required": true
         },
         {
          "key": "dbaName",
          "label": "DBA (Fictitious) Name",
          "required": true
         },
         {
          "key": "businessAddress",
          "label": "Business Address",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Contracts",
      "templates": [
       {
        "key": "nda-mutual",
        "name": "Non-Disclosure Agreement (Mutual)",
        "blurb": "Mutual confidentiality between two parties",
        "fastPath": true,
        "draftable": true,
        "fields": [
         {
          "key": "partyOne",
          "label": "Party One",
          "required": true
         },
         {
          "key": "partyTwo",
          "label": "Party Two",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "purpose",
          "label": "Purpose of Disclosure",
          "required": true
         },
         {
          "key": "termYears",
          "label": "Term (Years)",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "nda-one-way",
        "name": "Non-Disclosure Agreement (One-Way)",
        "blurb": "One-way confidentiality undertaking",
        "fastPath": true,
        "draftable": true,
        "fields": [
         {
          "key": "disclosingParty",
          "label": "Disclosing Party",
          "required": true
         },
         {
          "key": "receivingParty",
          "label": "Receiving Party",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "purpose",
          "label": "Purpose of Disclosure",
          "required": true
         },
         {
          "key": "termYears",
          "label": "Term (Years)",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "independent-contractor-agreement",
        "name": "Independent Contractor Agreement",
        "blurb": "Engagement terms for an independent contractor",
        "fastPath": true,
        "draftable": true,
        "fields": [
         {
          "key": "clientName",
          "label": "Client Name",
          "required": true
         },
         {
          "key": "contractorName",
          "label": "Contractor Name",
          "required": true
         },
         {
          "key": "services",
          "label": "Services to Be Performed",
          "required": true
         },
         {
          "key": "startDate",
          "label": "Start Date",
          "required": true
         },
         {
          "key": "endDate",
          "label": "End Date",
          "required": false
         },
         {
          "key": "compensation",
          "label": "Compensation",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "service-agreement",
        "name": "Service Agreement",
        "blurb": "Terms for the provision of services",
        "fastPath": true,
        "draftable": true,
        "fields": [
         {
          "key": "providerName",
          "label": "Service Provider",
          "required": true
         },
         {
          "key": "clientName",
          "label": "Client Name",
          "required": true
         },
         {
          "key": "services",
          "label": "Services",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "fees",
          "label": "Fees / Payment Terms",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "bill-of-sale",
        "name": "Bill of Sale",
        "blurb": "Record of a sale of personal property",
        "fastPath": true,
        "draftable": true,
        "fields": [
         {
          "key": "sellerName",
          "label": "Seller Name",
          "required": true
         },
         {
          "key": "buyerName",
          "label": "Buyer Name",
          "required": true
         },
         {
          "key": "itemDescription",
          "label": "Item Description",
          "required": true
         },
         {
          "key": "purchasePrice",
          "label": "Purchase Price",
          "required": true
         },
         {
          "key": "saleDate",
          "label": "Sale Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "consulting-agreement",
        "name": "Consulting Agreement",
        "blurb": "Terms for consulting services",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "consultantName",
          "label": "Consultant Name",
          "required": true
         },
         {
          "key": "clientName",
          "label": "Client Name",
          "required": true
         },
         {
          "key": "scope",
          "label": "Scope of Consulting Services",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "fees",
          "label": "Fees / Payment Terms",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Real Estate",
      "templates": [
       {
        "key": "residential-lease",
        "name": "Residential Lease Agreement",
        "blurb": "Basic residential tenancy terms",
        "fastPath": true,
        "draftable": true,
        "fields": [
         {
          "key": "landlordName",
          "label": "Landlord Name",
          "required": true
         },
         {
          "key": "tenantName",
          "label": "Tenant Name",
          "required": true
         },
         {
          "key": "propertyAddress",
          "label": "Property Address",
          "required": true
         },
         {
          "key": "monthlyRent",
          "label": "Monthly Rent",
          "required": true
         },
         {
          "key": "leaseStart",
          "label": "Lease Start Date",
          "required": true
         },
         {
          "key": "leaseEnd",
          "label": "Lease End Date",
          "required": true
         },
         {
          "key": "securityDeposit",
          "label": "Security Deposit",
          "required": false
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "commercial-lease",
        "name": "Commercial Lease Agreement",
        "blurb": "Commercial tenancy terms",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "landlordName",
          "label": "Landlord Name",
          "required": true
         },
         {
          "key": "tenantName",
          "label": "Tenant Name",
          "required": true
         },
         {
          "key": "propertyAddress",
          "label": "Property Address",
          "required": true
         },
         {
          "key": "monthlyRent",
          "label": "Monthly Rent",
          "required": true
         },
         {
          "key": "leaseStart",
          "label": "Lease Start Date",
          "required": true
         },
         {
          "key": "leaseEnd",
          "label": "Lease End Date",
          "required": true
         },
         {
          "key": "permittedUse",
          "label": "Permitted Use",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "lease-amendment",
        "name": "Lease Amendment",
        "blurb": "Amendment to an existing lease",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "landlordName",
          "label": "Landlord Name",
          "required": true
         },
         {
          "key": "tenantName",
          "label": "Tenant Name",
          "required": true
         },
         {
          "key": "propertyAddress",
          "label": "Property Address",
          "required": true
         },
         {
          "key": "originalLeaseDate",
          "label": "Original Lease Date",
          "required": true
         },
         {
          "key": "amendmentTerms",
          "label": "Amendment Terms",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "real-estate-purchase-agreement",
        "name": "Real Estate Purchase Agreement",
        "blurb": "Terms for the purchase of real property",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "buyerName",
          "label": "Buyer Name",
          "required": true
         },
         {
          "key": "sellerName",
          "label": "Seller Name",
          "required": true
         },
         {
          "key": "propertyAddress",
          "label": "Property Address",
          "required": true
         },
         {
          "key": "purchasePrice",
          "label": "Purchase Price",
          "required": true
         },
         {
          "key": "closingDate",
          "label": "Closing Date",
          "required": true
         },
         {
          "key": "earnestMoney",
          "label": "Earnest Money",
          "required": false
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "eviction-notice",
        "name": "Eviction Notice",
        "blurb": "Notice to vacate / cure",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "landlordName",
          "label": "Landlord Name",
          "required": true
         },
         {
          "key": "tenantName",
          "label": "Tenant Name",
          "required": true
         },
         {
          "key": "propertyAddress",
          "label": "Property Address",
          "required": true
         },
         {
          "key": "reason",
          "label": "Reason for Notice",
          "required": true
         },
         {
          "key": "cureOrVacateDate",
          "label": "Cure / Vacate By Date",
          "required": true
         },
         {
          "key": "noticeDate",
          "label": "Notice Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "property-management-agreement",
        "name": "Property Management Agreement",
        "blurb": "Terms for managing a property on the owner's behalf",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "ownerName",
          "label": "Property Owner",
          "required": true
         },
         {
          "key": "managerName",
          "label": "Property Manager",
          "required": true
         },
         {
          "key": "propertyAddress",
          "label": "Property Address",
          "required": true
         },
         {
          "key": "managementFee",
          "label": "Management Fee",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Estate Planning",
      "templates": [
       {
        "key": "last-will",
        "name": "Last Will and Testament (Basic)",
        "blurb": "Basic will with executor and beneficiaries",
        "fastPath": true,
        "draftable": true,
        "fields": [
         {
          "key": "testatorName",
          "label": "Testator Full Name",
          "required": true
         },
         {
          "key": "executorName",
          "label": "Executor Name",
          "required": true
         },
         {
          "key": "beneficiaries",
          "label": "Beneficiaries and Their Shares",
          "required": true
         },
         {
          "key": "specificGifts",
          "label": "Specific Gifts",
          "required": false
         },
         {
          "key": "executionDate",
          "label": "Execution Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "general-power-of-attorney",
        "name": "General Power of Attorney",
        "blurb": "Broad agency authority granted to an agent",
        "fastPath": true,
        "draftable": true,
        "fields": [
         {
          "key": "principalName",
          "label": "Principal Name",
          "required": true
         },
         {
          "key": "agentName",
          "label": "Agent (Attorney-in-Fact) Name",
          "required": true
         },
         {
          "key": "alternateAgent",
          "label": "Alternate Agent",
          "required": false
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "powers",
          "label": "Powers Granted",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "living-will",
        "name": "Living Will / Advance Directive",
        "blurb": "End-of-life care instructions",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "declarantName",
          "label": "Declarant Full Name",
          "required": true
         },
         {
          "key": "agentName",
          "label": "Health Care Agent",
          "required": false
         },
         {
          "key": "instructions",
          "label": "Care Instructions",
          "required": true
         },
         {
          "key": "executionDate",
          "label": "Execution Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "revocable-living-trust",
        "name": "Revocable Living Trust Agreement",
        "blurb": "Trust holding assets for beneficiaries",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "grantorName",
          "label": "Grantor Name",
          "required": true
         },
         {
          "key": "trusteeName",
          "label": "Trustee Name",
          "required": true
         },
         {
          "key": "trustName",
          "label": "Trust Name",
          "required": true
         },
         {
          "key": "beneficiaries",
          "label": "Beneficiaries",
          "required": true
         },
         {
          "key": "trustProperty",
          "label": "Initial Trust Property",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "healthcare-power-of-attorney",
        "name": "Health Care Power of Attorney",
        "blurb": "Authority to make health care decisions",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "principalName",
          "label": "Principal Name",
          "required": true
         },
         {
          "key": "agentName",
          "label": "Health Care Agent Name",
          "required": true
         },
         {
          "key": "alternateAgent",
          "label": "Alternate Agent",
          "required": false
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "codicil",
        "name": "Codicil to Will",
        "blurb": "Amendment to an existing will",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "testatorName",
          "label": "Testator Full Name",
          "required": true
         },
         {
          "key": "originalWillDate",
          "label": "Original Will Date",
          "required": true
         },
         {
          "key": "changes",
          "label": "Changes Made by This Codicil",
          "required": true
         },
         {
          "key": "executionDate",
          "label": "Execution Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Family Law",
      "templates": [
       {
        "key": "prenuptial-agreement",
        "name": "Prenuptial Agreement",
        "blurb": "Property terms agreed before marriage",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "partyOne",
          "label": "Party One",
          "required": true
         },
         {
          "key": "partyTwo",
          "label": "Party Two",
          "required": true
         },
         {
          "key": "weddingDate",
          "label": "Planned Wedding Date",
          "required": true
         },
         {
          "key": "propertyTerms",
          "label": "Property / Financial Terms",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "separation-agreement",
        "name": "Separation Agreement",
        "blurb": "Terms for spouses living apart",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "spouseOne",
          "label": "Spouse One",
          "required": true
         },
         {
          "key": "spouseTwo",
          "label": "Spouse Two",
          "required": true
         },
         {
          "key": "separationDate",
          "label": "Separation Date",
          "required": true
         },
         {
          "key": "terms",
          "label": "Agreed Terms",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "child-custody-agreement",
        "name": "Child Custody Agreement",
        "blurb": "Custody and visitation terms",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "parentOne",
          "label": "Parent One",
          "required": true
         },
         {
          "key": "parentTwo",
          "label": "Parent Two",
          "required": true
         },
         {
          "key": "childrenNames",
          "label": "Children (Names and Birth Years)",
          "required": true
         },
         {
          "key": "custodyTerms",
          "label": "Custody and Visitation Terms",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "child-support-agreement",
        "name": "Child Support Agreement",
        "blurb": "Agreed child support terms",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "payingParent",
          "label": "Paying Parent",
          "required": true
         },
         {
          "key": "receivingParent",
          "label": "Receiving Parent",
          "required": true
         },
         {
          "key": "childrenNames",
          "label": "Children (Names and Birth Years)",
          "required": true
         },
         {
          "key": "supportAmount",
          "label": "Support Amount and Frequency",
          "required": true
         },
         {
          "key": "startDate",
          "label": "Start Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "divorce-petition",
        "name": "Divorce Petition (Draft)",
        "blurb": "Petition to dissolve a marriage",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "petitionerName",
          "label": "Petitioner Name",
          "required": true
         },
         {
          "key": "respondentName",
          "label": "Respondent Name",
          "required": true
         },
         {
          "key": "marriageDate",
          "label": "Date of Marriage",
          "required": true
         },
         {
          "key": "marriagePlace",
          "label": "Place of Marriage",
          "required": true
         },
         {
          "key": "grounds",
          "label": "Grounds",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "adoption-consent",
        "name": "Adoption Consent Form",
        "blurb": "Consent to an adoption",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "consentingParent",
          "label": "Consenting Parent / Guardian",
          "required": true
         },
         {
          "key": "childName",
          "label": "Child's Name",
          "required": true
         },
         {
          "key": "adoptiveParents",
          "label": "Adoptive Parent(s)",
          "required": true
         },
         {
          "key": "consentDate",
          "label": "Consent Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Employment",
      "templates": [
       {
        "key": "employment-offer-letter",
        "name": "Employment Offer Letter",
        "blurb": "Written offer of employment",
        "fastPath": true,
        "draftable": true,
        "fields": [
         {
          "key": "companyName",
          "label": "Company Name",
          "required": true
         },
         {
          "key": "candidateName",
          "label": "Candidate Name",
          "required": true
         },
         {
          "key": "positionTitle",
          "label": "Position Title",
          "required": true
         },
         {
          "key": "startDate",
          "label": "Start Date",
          "required": true
         },
         {
          "key": "compensation",
          "label": "Compensation (Salary / Rate)",
          "required": true
         },
         {
          "key": "workLocation",
          "label": "Work Location",
          "required": true
         },
         {
          "key": "hiringManager",
          "label": "Hiring Manager",
          "required": false
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "employment-agreement",
        "name": "Employment Agreement",
        "blurb": "Full terms of employment",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "employerName",
          "label": "Employer Name",
          "required": true
         },
         {
          "key": "employeeName",
          "label": "Employee Name",
          "required": true
         },
         {
          "key": "positionTitle",
          "label": "Position Title",
          "required": true
         },
         {
          "key": "startDate",
          "label": "Start Date",
          "required": true
         },
         {
          "key": "compensation",
          "label": "Compensation",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "non-compete-agreement",
        "name": "Non-Compete Agreement",
        "blurb": "Post-employment competition limits",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "employerName",
          "label": "Employer Name",
          "required": true
         },
         {
          "key": "employeeName",
          "label": "Employee Name",
          "required": true
         },
         {
          "key": "restrictedActivities",
          "label": "Restricted Activities",
          "required": true
         },
         {
          "key": "duration",
          "label": "Restricted Period",
          "required": true
         },
         {
          "key": "geographicScope",
          "label": "Geographic Scope",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "severance-agreement",
        "name": "Severance Agreement",
        "blurb": "Separation and severance terms",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "employerName",
          "label": "Employer Name",
          "required": true
         },
         {
          "key": "employeeName",
          "label": "Employee Name",
          "required": true
         },
         {
          "key": "lastDay",
          "label": "Last Day of Employment",
          "required": true
         },
         {
          "key": "severanceTerms",
          "label": "Severance Terms",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "termination-letter",
        "name": "Termination Letter",
        "blurb": "Written notice ending employment",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "employerName",
          "label": "Employer Name",
          "required": true
         },
         {
          "key": "employeeName",
          "label": "Employee Name",
          "required": true
         },
         {
          "key": "lastDay",
          "label": "Last Day of Employment",
          "required": true
         },
         {
          "key": "reason",
          "label": "Reason for Termination",
          "required": true
         },
         {
          "key": "letterDate",
          "label": "Letter Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "handbook-acknowledgment",
        "name": "Employee Handbook Acknowledgment",
        "blurb": "Employee receipt and acknowledgment of the handbook",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "employerName",
          "label": "Employer Name",
          "required": true
         },
         {
          "key": "employeeName",
          "label": "Employee Name",
          "required": true
         },
         {
          "key": "handbookVersion",
          "label": "Handbook Version / Date",
          "required": true
         },
         {
          "key": "acknowledgmentDate",
          "label": "Date Signed",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Intellectual Property",
      "templates": [
       {
        "key": "trademark-license",
        "name": "Trademark License Agreement",
        "blurb": "License to use a trademark",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "licensorName",
          "label": "Licensor Name",
          "required": true
         },
         {
          "key": "licenseeName",
          "label": "Licensee Name",
          "required": true
         },
         {
          "key": "trademark",
          "label": "Trademark / Registration No.",
          "required": true
         },
         {
          "key": "licensedUse",
          "label": "Licensed Use",
          "required": true
         },
         {
          "key": "royaltyTerms",
          "label": "Royalty / Fee Terms",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "copyright-assignment",
        "name": "Copyright Assignment",
        "blurb": "Transfer of copyright ownership",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "assignorName",
          "label": "Assignor Name",
          "required": true
         },
         {
          "key": "assigneeName",
          "label": "Assignee Name",
          "required": true
         },
         {
          "key": "workDescription",
          "label": "Work Being Assigned",
          "required": true
         },
         {
          "key": "consideration",
          "label": "Consideration",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "patent-assignment",
        "name": "Patent Assignment",
        "blurb": "Transfer of patent rights",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "assignorName",
          "label": "Assignor Name",
          "required": true
         },
         {
          "key": "assigneeName",
          "label": "Assignee Name",
          "required": true
         },
         {
          "key": "patentDescription",
          "label": "Patent / Application No.",
          "required": true
         },
         {
          "key": "consideration",
          "label": "Consideration",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "ip-assignment-agreement",
        "name": "Intellectual Property Assignment Agreement",
        "blurb": "General assignment of intellectual property",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "assignorName",
          "label": "Assignor Name",
          "required": true
         },
         {
          "key": "assigneeName",
          "label": "Assignee Name",
          "required": true
         },
         {
          "key": "ipDescription",
          "label": "Intellectual Property Being Assigned",
          "required": true
         },
         {
          "key": "consideration",
          "label": "Consideration",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "cease-and-desist-letter",
        "name": "Cease and Desist Letter",
        "blurb": "Demand to stop infringing activity",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "senderName",
          "label": "Sender Name",
          "required": true
         },
         {
          "key": "recipientName",
          "label": "Recipient Name",
          "required": true
         },
         {
          "key": "rightsDescription",
          "label": "Right Being Asserted",
          "required": true
         },
         {
          "key": "infringingActivity",
          "label": "Infringing Activity",
          "required": true
         },
         {
          "key": "demandByDate",
          "label": "Stop By Date",
          "required": true
         },
         {
          "key": "letterDate",
          "label": "Letter Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "software-license-agreement",
        "name": "Software License Agreement",
        "blurb": "Terms for licensing software",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "licensorName",
          "label": "Licensor Name",
          "required": true
         },
         {
          "key": "licenseeName",
          "label": "Licensee Name",
          "required": true
         },
         {
          "key": "softwareDescription",
          "label": "Software",
          "required": true
         },
         {
          "key": "licenseScope",
          "label": "License Scope",
          "required": true
         },
         {
          "key": "fees",
          "label": "Fees",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Litigation",
      "templates": [
       {
        "key": "demand-letter",
        "name": "Demand Letter",
        "blurb": "Formal demand before suit",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "senderName",
          "label": "Sender Name",
          "required": true
         },
         {
          "key": "recipientName",
          "label": "Recipient Name",
          "required": true
         },
         {
          "key": "subjectMatter",
          "label": "Subject Matter",
          "required": true
         },
         {
          "key": "amountDemanded",
          "label": "Amount Demanded",
          "required": false
         },
         {
          "key": "demandByDate",
          "label": "Respond / Pay By Date",
          "required": true
         },
         {
          "key": "letterDate",
          "label": "Letter Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "civil-complaint",
        "name": "Civil Complaint (Draft)",
        "blurb": "Draft pleading to start a civil case",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "plaintiffName",
          "label": "Plaintiff Name",
          "required": true
         },
         {
          "key": "defendantName",
          "label": "Defendant Name",
          "required": true
         },
         {
          "key": "courtName",
          "label": "Court",
          "required": true
         },
         {
          "key": "claimsSummary",
          "label": "Summary of Claims",
          "required": true
         },
         {
          "key": "reliefSought",
          "label": "Relief Sought",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "small-claims-statement",
        "name": "Small Claims Statement of Claim",
        "blurb": "Statement of claim for small claims court",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "claimantName",
          "label": "Claimant Name",
          "required": true
         },
         {
          "key": "respondentName",
          "label": "Respondent Name",
          "required": true
         },
         {
          "key": "amountClaimed",
          "label": "Amount Claimed",
          "required": true
         },
         {
          "key": "factsSummary",
          "label": "Summary of Facts",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "settlement-agreement",
        "name": "Settlement Agreement",
        "blurb": "Terms settling a dispute",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "partyOne",
          "label": "Party One",
          "required": true
         },
         {
          "key": "partyTwo",
          "label": "Party Two",
          "required": true
         },
         {
          "key": "disputeDescription",
          "label": "Dispute Being Settled",
          "required": true
         },
         {
          "key": "settlementTerms",
          "label": "Settlement Terms",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "general-release",
        "name": "General Release of Claims",
        "blurb": "Release of claims between parties",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "releasorName",
          "label": "Releasor Name",
          "required": true
         },
         {
          "key": "releaseeName",
          "label": "Releasee Name",
          "required": true
         },
         {
          "key": "claimsDescription",
          "label": "Claims Being Released",
          "required": true
         },
         {
          "key": "consideration",
          "label": "Consideration",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "motion",
        "name": "Motion (Draft)",
        "blurb": "Draft motion to a court",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "movingParty",
          "label": "Moving Party",
          "required": true
         },
         {
          "key": "opposingParty",
          "label": "Opposing Party",
          "required": true
         },
         {
          "key": "courtName",
          "label": "Court",
          "required": true
         },
         {
          "key": "caseNumber",
          "label": "Case Number",
          "required": false
         },
         {
          "key": "reliefRequested",
          "label": "Relief Requested",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Financial",
      "templates": [
       {
        "key": "promissory-note",
        "name": "Promissory Note",
        "blurb": "Written promise to repay a sum of money",
        "fastPath": true,
        "draftable": true,
        "fields": [
         {
          "key": "borrowerName",
          "label": "Borrower Name",
          "required": true
         },
         {
          "key": "lenderName",
          "label": "Lender Name",
          "required": true
         },
         {
          "key": "principalAmount",
          "label": "Principal Amount",
          "required": true
         },
         {
          "key": "interestRate",
          "label": "Interest Rate",
          "required": false
         },
         {
          "key": "issueDate",
          "label": "Issue Date",
          "required": true
         },
         {
          "key": "maturityDate",
          "label": "Maturity / Due Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "loan-agreement",
        "name": "Loan Agreement",
        "blurb": "Full terms of a loan between parties",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "borrowerName",
          "label": "Borrower Name",
          "required": true
         },
         {
          "key": "lenderName",
          "label": "Lender Name",
          "required": true
         },
         {
          "key": "principalAmount",
          "label": "Principal Amount",
          "required": true
         },
         {
          "key": "interestRate",
          "label": "Interest Rate",
          "required": false
         },
         {
          "key": "repaymentTerms",
          "label": "Repayment Terms",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "personal-guaranty",
        "name": "Personal Guaranty",
        "blurb": "Personal guarantee of another's obligation",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "guarantorName",
          "label": "Guarantor Name",
          "required": true
         },
         {
          "key": "debtorName",
          "label": "Debtor Name",
          "required": true
         },
         {
          "key": "creditorName",
          "label": "Creditor Name",
          "required": true
         },
         {
          "key": "obligationDescription",
          "label": "Obligation Guaranteed",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "security-agreement",
        "name": "Security Agreement",
        "blurb": "Collateral securing an obligation",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "debtorName",
          "label": "Debtor Name",
          "required": true
         },
         {
          "key": "securedParty",
          "label": "Secured Party",
          "required": true
         },
         {
          "key": "collateralDescription",
          "label": "Collateral Description",
          "required": true
         },
         {
          "key": "obligationDescription",
          "label": "Obligation Secured",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "debt-settlement-agreement",
        "name": "Debt Settlement Agreement",
        "blurb": "Terms settling an outstanding debt",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "debtorName",
          "label": "Debtor Name",
          "required": true
         },
         {
          "key": "creditorName",
          "label": "Creditor Name",
          "required": true
         },
         {
          "key": "originalAmount",
          "label": "Original Amount Owed",
          "required": true
         },
         {
          "key": "settlementAmount",
          "label": "Settlement Amount",
          "required": true
         },
         {
          "key": "paymentTerms",
          "label": "Payment Terms",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "payment-plan-agreement",
        "name": "Payment Plan Agreement",
        "blurb": "Installment plan for an amount owed",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "payerName",
          "label": "Payer Name",
          "required": true
         },
         {
          "key": "payeeName",
          "label": "Payee Name",
          "required": true
         },
         {
          "key": "totalAmount",
          "label": "Total Amount Owed",
          "required": true
         },
         {
          "key": "installmentTerms",
          "label": "Installment Terms",
          "required": true
         },
         {
          "key": "startDate",
          "label": "First Payment Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Corporate Governance",
      "templates": [
       {
        "key": "board-resolution",
        "name": "Board Resolution",
        "blurb": "Recorded decision of a board of directors",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "companyName",
          "label": "Company Name",
          "required": true
         },
         {
          "key": "resolutionSubject",
          "label": "Subject of Resolution",
          "required": true
         },
         {
          "key": "resolutionText",
          "label": "Resolution Adopted",
          "required": true
         },
         {
          "key": "meetingDate",
          "label": "Meeting / Adoption Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "corporate-bylaws",
        "name": "Corporate Bylaws",
        "blurb": "Internal rules governing a corporation",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "companyName",
          "label": "Company Name",
          "required": true
         },
         {
          "key": "registeredOffice",
          "label": "Registered Office",
          "required": true
         },
         {
          "key": "fiscalYearEnd",
          "label": "Fiscal Year End",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "meeting-minutes",
        "name": "Meeting Minutes",
        "blurb": "Minutes of a board or shareholder meeting",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "companyName",
          "label": "Company Name",
          "required": true
         },
         {
          "key": "meetingType",
          "label": "Meeting Type (Board / Shareholders)",
          "required": true
         },
         {
          "key": "meetingDate",
          "label": "Meeting Date",
          "required": true
         },
         {
          "key": "attendees",
          "label": "Attendees",
          "required": true
         },
         {
          "key": "mattersDecided",
          "label": "Matters Decided",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "written-consent-in-lieu",
        "name": "Written Consent in Lieu of Meeting",
        "blurb": "Action taken by written consent instead of a meeting",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "companyName",
          "label": "Company Name",
          "required": true
         },
         {
          "key": "actingBody",
          "label": "Acting Body (Board / Shareholders)",
          "required": true
         },
         {
          "key": "actionTaken",
          "label": "Action Taken",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "conflict-of-interest-policy",
        "name": "Conflict of Interest Policy",
        "blurb": "Policy governing conflicts of interest",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "organizationName",
          "label": "Organization Name",
          "required": true
         },
         {
          "key": "coveredPersons",
          "label": "Covered Persons",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "secretary-certificate",
        "name": "Corporate Secretary's Certificate",
        "blurb": "Certification of corporate records and authority",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "companyName",
          "label": "Company Name",
          "required": true
         },
         {
          "key": "secretaryName",
          "label": "Corporate Secretary Name",
          "required": true
         },
         {
          "key": "certifiedMatters",
          "label": "Matters Certified",
          "required": true
         },
         {
          "key": "certificateDate",
          "label": "Certificate Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "HR & Labor",
      "templates": [
       {
        "key": "disciplinary-action-form",
        "name": "Disciplinary Action Form",
        "blurb": "Record of a disciplinary action",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "employerName",
          "label": "Employer Name",
          "required": true
         },
         {
          "key": "employeeName",
          "label": "Employee Name",
          "required": true
         },
         {
          "key": "incidentDate",
          "label": "Incident Date",
          "required": true
         },
         {
          "key": "conductDescription",
          "label": "Conduct at Issue",
          "required": true
         },
         {
          "key": "actionTaken",
          "label": "Action Taken",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "performance-improvement-plan",
        "name": "Performance Improvement Plan",
        "blurb": "Plan to address performance issues",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "employerName",
          "label": "Employer Name",
          "required": true
         },
         {
          "key": "employeeName",
          "label": "Employee Name",
          "required": true
         },
         {
          "key": "managerName",
          "label": "Manager Name",
          "required": true
         },
         {
          "key": "concerns",
          "label": "Performance Concerns",
          "required": true
         },
         {
          "key": "improvementPeriod",
          "label": "Improvement Period",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "leave-of-absence-request",
        "name": "Leave of Absence Request",
        "blurb": "Employee request for leave",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "employeeName",
          "label": "Employee Name",
          "required": true
         },
         {
          "key": "employerName",
          "label": "Employer Name",
          "required": true
         },
         {
          "key": "leaveType",
          "label": "Type of Leave",
          "required": true
         },
         {
          "key": "startDate",
          "label": "Leave Start Date",
          "required": true
         },
         {
          "key": "endDate",
          "label": "Leave End Date",
          "required": true
         },
         {
          "key": "reason",
          "label": "Reason (General)",
          "required": true
         }
        ]
       },
       {
        "key": "remote-work-agreement",
        "name": "Remote Work Agreement",
        "blurb": "Terms for working remotely",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "employerName",
          "label": "Employer Name",
          "required": true
         },
         {
          "key": "employeeName",
          "label": "Employee Name",
          "required": true
         },
         {
          "key": "workLocation",
          "label": "Remote Work Location",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "terms",
          "label": "Key Terms",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "employee-warning-notice",
        "name": "Employee Warning Notice",
        "blurb": "Written warning to an employee",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "employerName",
          "label": "Employer Name",
          "required": true
         },
         {
          "key": "employeeName",
          "label": "Employee Name",
          "required": true
         },
         {
          "key": "warningDate",
          "label": "Warning Date",
          "required": true
         },
         {
          "key": "conductDescription",
          "label": "Conduct at Issue",
          "required": true
         },
         {
          "key": "expectedCorrection",
          "label": "Expected Correction",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "overtime-authorization-form",
        "name": "Overtime Authorization Form",
        "blurb": "Pre-approval of overtime work",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "employerName",
          "label": "Employer Name",
          "required": true
         },
         {
          "key": "employeeName",
          "label": "Employee Name",
          "required": true
         },
         {
          "key": "managerName",
          "label": "Authorizing Manager",
          "required": true
         },
         {
          "key": "overtimePeriod",
          "label": "Overtime Period / Hours",
          "required": true
         },
         {
          "key": "reason",
          "label": "Business Reason",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Personal Legal",
      "templates": [
       {
        "key": "general-affidavit",
        "name": "General Affidavit",
        "blurb": "Sworn written statement of facts",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "affiantName",
          "label": "Affiant (Person Swearing) Name",
          "required": true
         },
         {
          "key": "facts",
          "label": "Statement of Facts",
          "required": true
         },
         {
          "key": "executionDate",
          "label": "Execution Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "name-change-petition",
        "name": "Name Change Petition (Draft)",
        "blurb": "Petition to change a legal name",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "currentName",
          "label": "Current Legal Name",
          "required": true
         },
         {
          "key": "requestedName",
          "label": "Requested New Name",
          "required": true
         },
         {
          "key": "reason",
          "label": "Reason for Change",
          "required": true
         },
         {
          "key": "dateOfBirth",
          "label": "Date of Birth",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "minor-travel-consent",
        "name": "Minor Travel Consent",
        "blurb": "Consent for a child to travel",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "parentName",
          "label": "Parent / Guardian Name",
          "required": true
         },
         {
          "key": "childName",
          "label": "Child's Name",
          "required": true
         },
         {
          "key": "travelCompanion",
          "label": "Traveling With (Name)",
          "required": true
         },
         {
          "key": "destination",
          "label": "Destination",
          "required": true
         },
         {
          "key": "travelDates",
          "label": "Travel Dates",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "medical-records-authorization",
        "name": "Medical Records Authorization",
        "blurb": "Authorization to release medical records",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "patientName",
          "label": "Patient Name",
          "required": true
         },
         {
          "key": "recordsHolder",
          "label": "Provider / Records Holder",
          "required": true
         },
         {
          "key": "releaseTo",
          "label": "Release Records To",
          "required": true
         },
         {
          "key": "recordsDescription",
          "label": "Records Covered",
          "required": true
         },
         {
          "key": "authorizationDate",
          "label": "Date Signed",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "formal-complaint-letter",
        "name": "Formal Complaint Letter",
        "blurb": "Written complaint to a business or agency",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "senderName",
          "label": "Sender Name",
          "required": true
         },
         {
          "key": "recipientName",
          "label": "Recipient Name / Organization",
          "required": true
         },
         {
          "key": "subjectMatter",
          "label": "Subject of Complaint",
          "required": true
         },
         {
          "key": "factsSummary",
          "label": "Summary of Facts",
          "required": true
         },
         {
          "key": "resolutionRequested",
          "label": "Resolution Requested",
          "required": true
         },
         {
          "key": "letterDate",
          "label": "Letter Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "personal-property-memo",
        "name": "Personal Property Memorandum",
        "blurb": "Memo assigning tangible personal property",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "ownerName",
          "label": "Owner Name",
          "required": true
         },
         {
          "key": "assignments",
          "label": "Items and Who Receives Them",
          "required": true
         },
         {
          "key": "memoDate",
          "label": "Memo Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Tax",
      "templates": [
       {
        "key": "tax-engagement-letter",
        "name": "Tax Engagement Letter",
        "blurb": "Terms for tax preparation services",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "preparerName",
          "label": "Preparer / Firm Name",
          "required": true
         },
         {
          "key": "clientName",
          "label": "Client Name",
          "required": true
         },
         {
          "key": "taxYears",
          "label": "Tax Year(s) Covered",
          "required": true
         },
         {
          "key": "services",
          "label": "Services Included",
          "required": true
         },
         {
          "key": "fees",
          "label": "Fees",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "tax-power-of-attorney",
        "name": "Tax Power of Attorney Worksheet",
        "blurb": "Information worksheet for a tax power of attorney (e.g. Form 2848)",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "taxpayerName",
          "label": "Taxpayer Name",
          "required": true
         },
         {
          "key": "representativeName",
          "label": "Representative Name",
          "required": true
         },
         {
          "key": "taxMatters",
          "label": "Tax Matters / Forms / Years",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         }
        ]
       },
       {
        "key": "extension-request-letter",
        "name": "Tax Extension Request Letter",
        "blurb": "Letter requesting filing extension time",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "taxpayerName",
          "label": "Taxpayer Name",
          "required": true
         },
         {
          "key": "taxYear",
          "label": "Tax Year",
          "required": true
         },
         {
          "key": "reason",
          "label": "Reason for Extension",
          "required": true
         },
         {
          "key": "requestedUntil",
          "label": "Extension Requested Until",
          "required": true
         },
         {
          "key": "letterDate",
          "label": "Letter Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "installment-agreement-proposal",
        "name": "IRS Installment Agreement Proposal",
        "blurb": "Proposal to pay a tax balance in installments",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "taxpayerName",
          "label": "Taxpayer Name",
          "required": true
         },
         {
          "key": "balanceOwed",
          "label": "Balance Owed",
          "required": true
         },
         {
          "key": "proposedPayment",
          "label": "Proposed Monthly Payment",
          "required": true
         },
         {
          "key": "taxYears",
          "label": "Tax Year(s)",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "penalty-abatement-letter",
        "name": "Penalty Abatement Request Letter",
        "blurb": "Letter asking a tax authority to remove penalties",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "taxpayerName",
          "label": "Taxpayer Name",
          "required": true
         },
         {
          "key": "taxYear",
          "label": "Tax Year",
          "required": true
         },
         {
          "key": "penaltyDescription",
          "label": "Penalty at Issue",
          "required": true
         },
         {
          "key": "reasonableCause",
          "label": "Reasonable Cause Statement",
          "required": true
         },
         {
          "key": "letterDate",
          "label": "Letter Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "offer-in-compromise-summary",
        "name": "Offer in Compromise Summary",
        "blurb": "Summary worksheet for an offer in compromise",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "taxpayerName",
          "label": "Taxpayer Name",
          "required": true
         },
         {
          "key": "balanceOwed",
          "label": "Balance Owed",
          "required": true
         },
         {
          "key": "offerAmount",
          "label": "Amount Offered",
          "required": true
         },
         {
          "key": "taxYears",
          "label": "Tax Year(s)",
          "required": true
         },
         {
          "key": "groundsSummary",
          "label": "Grounds Summary",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Healthcare",
      "templates": [
       {
        "key": "hipaa-authorization",
        "name": "HIPAA Authorization",
        "blurb": "Authorization to use / disclose health information",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "patientName",
          "label": "Patient Name",
          "required": true
         },
         {
          "key": "disclosingParty",
          "label": "Provider Disclosing Records",
          "required": true
         },
         {
          "key": "recipientName",
          "label": "Recipient of the Information",
          "required": true
         },
         {
          "key": "informationDescription",
          "label": "Information Covered",
          "required": true
         },
         {
          "key": "expirationDate",
          "label": "Authorization Expires",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "medical-treatment-consent",
        "name": "Medical Treatment Consent",
        "blurb": "Consent to medical treatment",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "patientName",
          "label": "Patient Name",
          "required": true
         },
         {
          "key": "providerName",
          "label": "Treating Provider",
          "required": true
         },
         {
          "key": "treatmentDescription",
          "label": "Treatment Consented To",
          "required": true
         },
         {
          "key": "consentDate",
          "label": "Date Signed",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "informed-consent-form",
        "name": "Informed Consent Form",
        "blurb": "Consent after disclosure of risks",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "patientName",
          "label": "Patient Name",
          "required": true
         },
         {
          "key": "providerName",
          "label": "Provider",
          "required": true
         },
         {
          "key": "procedureDescription",
          "label": "Procedure / Treatment",
          "required": true
         },
         {
          "key": "risksSummary",
          "label": "Risks Disclosed",
          "required": true
         },
         {
          "key": "consentDate",
          "label": "Date Signed",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "health-care-proxy",
        "name": "Health Care Proxy",
        "blurb": "Designation of a health care decision-maker",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "principalName",
          "label": "Principal Name",
          "required": true
         },
         {
          "key": "agentName",
          "label": "Health Care Agent Name",
          "required": true
         },
         {
          "key": "alternateAgent",
          "label": "Alternate Agent",
          "required": false
         },
         {
          "key": "effectiveDate",
          "label": "Effective Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "telehealth-consent",
        "name": "Telehealth Consent",
        "blurb": "Consent to treatment by telehealth",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "patientName",
          "label": "Patient Name",
          "required": true
         },
         {
          "key": "providerName",
          "label": "Provider / Practice",
          "required": true
         },
         {
          "key": "consentDate",
          "label": "Date Signed",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "medical-records-request",
        "name": "Medical Records Request",
        "blurb": "Request for a copy of medical records",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "patientName",
          "label": "Patient Name",
          "required": true
         },
         {
          "key": "providerName",
          "label": "Provider / Records Holder",
          "required": true
         },
         {
          "key": "recordsDescription",
          "label": "Records Requested",
          "required": true
         },
         {
          "key": "deliverTo",
          "label": "Deliver Records To",
          "required": true
         },
         {
          "key": "requestDate",
          "label": "Request Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       }
      ]
     },
     {
      "name": "Notary & Certifications",
      "templates": [
       {
        "key": "acknowledgment-certificate",
        "name": "Acknowledgment Certificate",
        "blurb": "Notarial certificate of acknowledgment",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "signerName",
          "label": "Signer Name",
          "required": true
         },
         {
          "key": "documentDescription",
          "label": "Document Acknowledged",
          "required": true
         },
         {
          "key": "acknowledgmentDate",
          "label": "Date of Acknowledgment",
          "required": true
         },
         {
          "key": "notaryName",
          "label": "Notary Name",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "jurat-certificate",
        "name": "Jurat Certificate",
        "blurb": "Notarial certificate for a sworn statement",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "signerName",
          "label": "Signer Name",
          "required": true
         },
         {
          "key": "documentDescription",
          "label": "Document Sworn To",
          "required": true
         },
         {
          "key": "juratDate",
          "label": "Date Sworn",
          "required": true
         },
         {
          "key": "notaryName",
          "label": "Notary Name",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "certified-copy-attestation",
        "name": "Certified Copy Attestation",
        "blurb": "Attestation that a copy is true and complete",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "attestorName",
          "label": "Attestor Name",
          "required": true
         },
         {
          "key": "documentDescription",
          "label": "Document Copied",
          "required": true
         },
         {
          "key": "originalHolder",
          "label": "Holder of the Original",
          "required": true
         },
         {
          "key": "attestationDate",
          "label": "Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "apostille-request",
        "name": "Apostille Request Form",
        "blurb": "Request to apostille a document",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "requesterName",
          "label": "Requester Name",
          "required": true
         },
         {
          "key": "documentDescription",
          "label": "Document to Apostille",
          "required": true
         },
         {
          "key": "destinationCountry",
          "label": "Country Where the Document Will Be Used",
          "required": true
         },
         {
          "key": "requestDate",
          "label": "Request Date",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "sworn-statement",
        "name": "Sworn Statement",
        "blurb": "Statement sworn before an authorized officer",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "declarantName",
          "label": "Declarant Name",
          "required": true
         },
         {
          "key": "statement",
          "label": "Statement of Facts",
          "required": true
         },
         {
          "key": "executionDate",
          "label": "Date Sworn",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       },
       {
        "key": "witness-attestation",
        "name": "Witness Attestation",
        "blurb": "Witness certification of a signing",
        "fastPath": false,
        "draftable": false,
        "fields": [
         {
          "key": "witnessName",
          "label": "Witness Name",
          "required": true
         },
         {
          "key": "signerName",
          "label": "Signer Name",
          "required": true
         },
         {
          "key": "documentDescription",
          "label": "Document Witnessed",
          "required": true
         },
         {
          "key": "signingDate",
          "label": "Date of Signing",
          "required": true
         },
         {
          "key": "jurisdiction",
          "label": "Jurisdiction",
          "required": true
         }
        ]
       }
      ]
     }
    ],
    "documents": [
     {
      "id": 2,
      "kind": "document",
      "title": "Capture Fictional LLC — Operating Agreement",
      "category": "Business Formation",
      "templateKey": "operating-agreement",
      "jurisdiction": "",
      "question": "",
      "notes": "",
      "body": "OPERATING AGREEMENT\n\nThis Operating Agreement (the \"Agreement\") is entered into by Capture Fictional LLC (the \"Company\"), a limited liability company formed under the laws of Missouri.\n\n1. FORMATION\nThe Company was formed as a limited liability company under the laws of Missouri. The Company shall operate in accordance with this Agreement and the applicable laws of that jurisdiction.\n\n2. PURPOSE\nThe purpose of the Company is: Seed governance\n\n3. MANAGEMENT\nThe Company shall be managed by its Managing Member, Pat Seed, who has the authority to bind the Company in its ordinary business and to act on its behalf in accordance with this Agreement.\n\n4. EFFECTIVE DATE\nThis Agreement is effective as of 2026-11-03.\n\nSIGNATURES\n\nCompany: Capture Fictional LLC\n\nManaging Member: Pat Seed\nSignature: ______________________________    Date: ______________\n\nMember: ______________________________\nSignature: ______________________________    Date: ______________\n\nDRAFT — prepared with Dogg and Associates drafting aids. For review by a licensed attorney before use.\nHave an attorney review before relying on this document.\nLegal intelligence, not legal advice.",
      "fields": {
       "llcName": "Capture Fictional LLC",
       "managingMember": "Pat Seed",
       "jurisdiction": "Missouri",
       "effectiveDate": "2026-11-03",
       "purpose": "Seed governance"
      },
      "origin": "standard_form",
      "sources": "",
      "createdBy": 1,
      "createdByName": "OwnerCap6",
      "createdAt": 1793749645485,
      "updatedAt": 1793749645485
     },
     {
      "id": 1,
      "kind": "research",
      "title": "Capture Research Note Edited",
      "category": "",
      "templateKey": "",
      "jurisdiction": "",
      "question": "Capture seed question?",
      "notes": "",
      "body": "",
      "fields": {},
      "origin": "",
      "sources": "",
      "createdBy": 1,
      "createdByName": "OwnerCap6",
      "createdAt": 1793749645279,
      "updatedAt": 1793749645619
     }
    ],
    "aiRequests": [
     {
      "id": 2,
      "kind": "docgen",
      "status": "queued",
      "failureClass": "",
      "failureNote": "",
      "resultDocumentId": null,
      "createdAt": 1793749646169,
      "completedAt": null,
      "label": "Consulting Agreement"
     },
     {
      "id": 1,
      "kind": "research",
      "status": "queued",
      "failureClass": "",
      "failureNote": "",
      "resultDocumentId": null,
      "createdAt": 1793749645862,
      "completedAt": null,
      "label": "What is a fictional statute?"
     }
    ],
    "aiConfigured": true,
    "aiNotice": "",
    "ok": true
   }
  },
  "DELETE /api/app/law/documents/{id}": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Deleted.",
    "id": 1
   }
  }
 }
}
"""

    private val root: JsonObject by lazy { Json.parseToJsonElement(ALL).jsonObject }
    private val bodies: JsonObject by lazy { root["bodies"]!!.jsonObject }

    fun body(name: String): String =
        bodies[name]!!.jsonObject["body"]!!.toString()

    fun status(name: String): Int =
        bodies[name]!!.jsonObject["status"]!!.jsonPrimitive.int

    fun bodyJson(name: String): JsonObject =
        bodies[name]!!.jsonObject["body"]!!.jsonObject
}

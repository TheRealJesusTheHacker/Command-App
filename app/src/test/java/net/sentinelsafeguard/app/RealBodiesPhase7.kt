package net.sentinelsafeguard.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Phase 7 REAL response bodies, captured 2026-11-03 from a
 * rehearsal boot of the exact Phase 7 candidate bytes
 * (server.py sha256 ceb3ba33e3542277228c24209c22ca877372a0dfbc029194525b5d81f7296aca,
 * Addendum 22 — the deployed production bytes), against a seeded
 * scratch database. Seed data is fictional; no secrets appear
 * (device tokens never appear in these bodies). [ALL] is
 * phase7-work/fixtures/real-bodies.json verbatim, re-keyed from
 * capture names to route names. Binary file bodies are recorded
 * as status/headers/size/sha256 descriptors, as captured.
 */
object RealBodiesPhase7 {
    val ALL: String = """{
 "_meta": {
  "serverPySha256": "ceb3ba33e3542277228c24209c22ca877372a0dfbc029194525b5d81f7296aca",
  "capturedFrom": "rehearsal boot of the exact Phase 7 candidate bytes (deployed production, Addendum 22)",
  "note": "Real bodies captured from the exact candidate bytes on a residue-zero scratch DB; binary bodies recorded as status/headers/size/sha256 descriptors; seed data fictional; no secrets (device tokens never appear in these bodies)."
 },
 "bodies": {
  "GET /api/app/knowledge (unauthenticated)": {
   "status": 401,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Sign-in required."
   }
  },
  "GET /api/app/archive (unauthenticated)": {
   "status": 401,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Sign-in required."
   }
  },
  "GET /api/app/knowledge/{id}/file (unauthenticated)": {
   "status": 401,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Sign-in required."
   }
  },
  "GET /api/app/knowledge (flag off)": {
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Not authorized."
   }
  },
  "GET /api/app/archive (flag off)": {
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Not authorized."
   }
  },
  "GET /api/app/knowledge (staff)": {
   "status": 200,
   "body": {
    "ok": true,
    "total": 2,
    "limit": 0,
    "offset": 0,
    "entries": [
     {
      "id": 1,
      "title": "Capture Alpha Guide",
      "category": "Reference",
      "description": "First capture book.",
      "fileName": null,
      "fileSize": null,
      "pageCount": null,
      "hasFile": false,
      "fileUploadedAt": null,
      "fileUploadedBy": null,
      "uploadedByName": null,
      "createdAt": 1793753737185,
      "updatedAt": 1793753737185,
      "clientVisible": false,
      "audience": "none"
     },
     {
      "id": 2,
      "title": "Capture Beta Standard",
      "category": "Compliance",
      "description": "Second capture book.",
      "fileName": null,
      "fileSize": null,
      "pageCount": null,
      "hasFile": false,
      "fileUploadedAt": null,
      "fileUploadedBy": null,
      "uploadedByName": null,
      "createdAt": 1793753737310,
      "updatedAt": 1793753737310,
      "clientVisible": false,
      "audience": "none"
     }
    ],
    "stats": {
     "total": 2,
     "readyToRead": 0,
     "awaitingFile": 2,
     "totalSize": 0,
     "totalPages": 0,
     "pagesComplete": true
    },
    "categories": [
     {
      "category": "Compliance",
      "count": 1
     },
     {
      "category": "Reference",
      "count": 1
     }
    ]
   }
  },
  "GET /api/app/knowledge": {
   "status": 200,
   "body": {
    "ok": true,
    "total": 2,
    "limit": 0,
    "offset": 0,
    "entries": [
     {
      "id": 1,
      "title": "Capture Alpha Guide",
      "category": "Reference",
      "description": "First capture book.",
      "fileName": null,
      "fileSize": null,
      "pageCount": null,
      "hasFile": false,
      "fileUploadedAt": null,
      "fileUploadedBy": null,
      "uploadedByName": null,
      "createdAt": 1793753737185,
      "updatedAt": 1793753737185,
      "clientVisible": false,
      "audience": "none"
     },
     {
      "id": 2,
      "title": "Capture Beta Standard",
      "category": "Compliance",
      "description": "Second capture book.",
      "fileName": null,
      "fileSize": null,
      "pageCount": null,
      "hasFile": false,
      "fileUploadedAt": null,
      "fileUploadedBy": null,
      "uploadedByName": null,
      "createdAt": 1793753737310,
      "updatedAt": 1793753737310,
      "clientVisible": false,
      "audience": "none"
     }
    ],
    "stats": {
     "total": 2,
     "readyToRead": 0,
     "awaitingFile": 2,
     "totalSize": 0,
     "totalPages": 0,
     "pagesComplete": true
    },
    "categories": [
     {
      "category": "Compliance",
      "count": 1
     },
     {
      "category": "Reference",
      "count": 1
     }
    ]
   }
  },
  "GET /api/app/knowledge?q=alpha": {
   "status": 200,
   "body": {
    "ok": true,
    "total": 1,
    "limit": 0,
    "offset": 0,
    "entries": [
     {
      "id": 1,
      "title": "Capture Alpha Guide",
      "category": "Reference",
      "description": "First capture book.",
      "fileName": null,
      "fileSize": null,
      "pageCount": null,
      "hasFile": false,
      "fileUploadedAt": null,
      "fileUploadedBy": null,
      "uploadedByName": null,
      "createdAt": 1793753737185,
      "updatedAt": 1793753737185,
      "clientVisible": false,
      "audience": "none"
     }
    ],
    "stats": {
     "total": 2,
     "readyToRead": 0,
     "awaitingFile": 2,
     "totalSize": 0,
     "totalPages": 0,
     "pagesComplete": true
    },
    "categories": [
     {
      "category": "Compliance",
      "count": 1
     },
     {
      "category": "Reference",
      "count": 1
     }
    ]
   }
  },
  "GET /api/app/knowledge?category=Compliance": {
   "status": 200,
   "body": {
    "ok": true,
    "total": 1,
    "limit": 0,
    "offset": 0,
    "entries": [
     {
      "id": 2,
      "title": "Capture Beta Standard",
      "category": "Compliance",
      "description": "Second capture book.",
      "fileName": null,
      "fileSize": null,
      "pageCount": null,
      "hasFile": false,
      "fileUploadedAt": null,
      "fileUploadedBy": null,
      "uploadedByName": null,
      "createdAt": 1793753737310,
      "updatedAt": 1793753737310,
      "clientVisible": false,
      "audience": "none"
     }
    ],
    "stats": {
     "total": 2,
     "readyToRead": 0,
     "awaitingFile": 2,
     "totalSize": 0,
     "totalPages": 0,
     "pagesComplete": true
    },
    "categories": [
     {
      "category": "Compliance",
      "count": 1
     },
     {
      "category": "Reference",
      "count": 1
     }
    ]
   }
  },
  "GET /api/app/knowledge?sort=newest": {
   "status": 200,
   "body": {
    "ok": true,
    "total": 2,
    "limit": 0,
    "offset": 0,
    "entries": [
     {
      "id": 2,
      "title": "Capture Beta Standard",
      "category": "Compliance",
      "description": "Second capture book.",
      "fileName": null,
      "fileSize": null,
      "pageCount": null,
      "hasFile": false,
      "fileUploadedAt": null,
      "fileUploadedBy": null,
      "uploadedByName": null,
      "createdAt": 1793753737310,
      "updatedAt": 1793753737310,
      "clientVisible": false,
      "audience": "none"
     },
     {
      "id": 1,
      "title": "Capture Alpha Guide",
      "category": "Reference",
      "description": "First capture book.",
      "fileName": null,
      "fileSize": null,
      "pageCount": null,
      "hasFile": false,
      "fileUploadedAt": null,
      "fileUploadedBy": null,
      "uploadedByName": null,
      "createdAt": 1793753737185,
      "updatedAt": 1793753737185,
      "clientVisible": false,
      "audience": "none"
     }
    ],
    "stats": {
     "total": 2,
     "readyToRead": 0,
     "awaitingFile": 2,
     "totalSize": 0,
     "totalPages": 0,
     "pagesComplete": true
    },
    "categories": [
     {
      "category": "Compliance",
      "count": 1
     },
     {
      "category": "Reference",
      "count": 1
     }
    ]
   }
  },
  "GET /api/app/knowledge?sort=bogus (refusal)": {
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Unknown sort."
   }
  },
  "GET /api/app/knowledge/{id} (no file)": {
   "status": 200,
   "body": {
    "ok": true,
    "entry": {
     "id": 1,
     "title": "Capture Alpha Guide",
     "category": "Reference",
     "description": "First capture book.",
     "fileName": null,
     "fileSize": null,
     "pageCount": null,
     "hasFile": false,
     "fileUploadedAt": null,
     "fileUploadedBy": null,
     "uploadedByName": null,
     "createdAt": 1793753737185,
     "updatedAt": 1793753737185,
     "clientVisible": false,
     "audience": "none"
    },
    "document": {
     "url": null,
     "message": "No PDF has been added to this entry yet.",
     "ok": true
    }
   }
  },
  "GET /api/app/knowledge/{id} (missing)": {
   "status": 404,
   "body": {
    "ok": false,
    "message": "Not found."
   }
  },
  "POST /api/app/knowledge/{id}/upload (staff refusal)": {
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Not authorized."
   }
  },
  "POST /api/app/knowledge/{id}/upload (not a PDF)": {
   "status": 400,
   "body": {
    "ok": false,
    "message": "That file is not a complete PDF."
   }
  },
  "POST /api/app/knowledge/{id}/upload": {
   "status": 200,
   "body": {
    "ok": true,
    "id": 1,
    "fileName": "alpha.pdf",
    "fileSize": 218,
    "pageCount": 1,
    "message": "PDF stored \u2014 the book is ready to read."
   }
  },
  "GET /api/app/knowledge/{id}": {
   "status": 200,
   "body": {
    "ok": true,
    "entry": {
     "id": 1,
     "title": "Capture Alpha Guide",
     "category": "Reference",
     "description": "First capture book.",
     "fileName": "alpha.pdf",
     "fileSize": 218,
     "pageCount": 1,
     "hasFile": true,
     "fileUploadedAt": 1793753738801,
     "fileUploadedBy": 1,
     "uploadedByName": "OwnerCap7",
     "createdAt": 1793753737185,
     "updatedAt": 1793753738801,
     "clientVisible": false,
     "audience": "none"
    },
    "document": {
     "url": "/api/app/knowledge/1/file",
     "fileName": "alpha.pdf",
     "pageCount": 1,
     "message": "Ready to read.",
     "ok": true
    }
   }
  },
  "GET /api/app/knowledge/{id}/file (inline)": {
   "status": 200,
   "body": {
    "_binary": true,
    "size": 218,
    "sha256": "a6946076beaeb23e666cc3e268cac0b34c2c281d1497aaf7952609d31043199f",
    "contentType": "application/pdf",
    "contentDisposition": "inline; filename=\"alpha.pdf\"; filename*=UTF-8''alpha.pdf",
    "contentRange": null
   }
  },
  "GET /api/app/knowledge/{id}/file?download=1": {
   "status": 200,
   "body": {
    "_binary": true,
    "size": 218,
    "sha256": "a6946076beaeb23e666cc3e268cac0b34c2c281d1497aaf7952609d31043199f",
    "contentType": "application/pdf",
    "contentDisposition": "attachment; filename=\"alpha.pdf\"; filename*=UTF-8''alpha.pdf",
    "contentRange": null
   }
  },
  "GET /api/app/knowledge/{id}/file (range)": {
   "status": 206,
   "body": {
    "_binary": true,
    "size": 8,
    "sha256": "e16fa5d9b51928755db85b917f0297babaf22c7a47e97d9212adab56e61ba04e",
    "contentType": "application/pdf",
    "contentDisposition": "inline; filename=\"alpha.pdf\"; filename*=UTF-8''alpha.pdf",
    "contentRange": "bytes 0-7/218"
   }
  },
  "GET /api/app/knowledge/{id}/file (range unsatisfiable)": {
   "status": 416,
   "body": {
    "_binary": true,
    "size": 0,
    "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "contentType": null,
    "contentDisposition": null,
    "contentRange": "bytes */218"
   }
  },
  "GET /api/app/knowledge/{id}/file (no PDF)": {
   "status": 404,
   "body": {
    "ok": false,
    "message": "No PDF has been added to this entry yet."
   }
  },
  "POST /api/app/knowledge": {
   "status": 201,
   "body": {
    "ok": true,
    "id": 3,
    "message": "Knowledge entry saved.",
    "entry": {
     "id": 3,
     "title": "Capture Delta Manual",
     "category": "Field Practice",
     "description": "Created in capture.",
     "fileName": null,
     "fileSize": null,
     "pageCount": null,
     "hasFile": false,
     "fileUploadedAt": null,
     "fileUploadedBy": null,
     "uploadedByName": null,
     "createdAt": 1793753739681,
     "updatedAt": 1793753739681,
     "clientVisible": false,
     "audience": "none"
    }
   }
  },
  "POST /api/app/knowledge (blank title)": {
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Title is required."
   }
  },
  "PATCH /api/app/knowledge/{id}": {
   "status": 200,
   "body": {
    "ok": true,
    "id": 3,
    "message": "Knowledge entry saved.",
    "entry": {
     "id": 3,
     "title": "Capture Delta Renamed",
     "category": "Field Practice",
     "description": "Created in capture.",
     "fileName": null,
     "fileSize": null,
     "pageCount": null,
     "hasFile": false,
     "fileUploadedAt": null,
     "fileUploadedBy": null,
     "uploadedByName": null,
     "createdAt": 1793753739681,
     "updatedAt": 1793753739942,
     "clientVisible": false,
     "audience": "none"
    }
   }
  },
  "POST /api/app/knowledge/{id}/audience (refusal)": {
   "status": 403,
   "body": {
    "ok": false,
    "authorized": false,
    "message": "Audience must be none, all, or specific."
   }
  },
  "POST /api/app/knowledge/{id}/audience": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Knowledge audience updated.",
    "id": 3,
    "clientVisible": true,
    "audience": "all",
    "clientIds": []
   }
  },
  "POST /api/app/knowledge (with audience)": {
   "status": 201,
   "body": {
    "ok": true,
    "id": 4,
    "message": "Knowledge entry saved.",
    "entry": {
     "id": 4,
     "title": "Capture Epsilon Combined",
     "category": "Reference",
     "description": "Editor flow.",
     "fileName": null,
     "fileSize": null,
     "pageCount": null,
     "hasFile": false,
     "fileUploadedAt": null,
     "fileUploadedBy": null,
     "uploadedByName": null,
     "createdAt": 1793753740518,
     "updatedAt": 1793753740616,
     "clientVisible": false,
     "audience": "none"
    }
   }
  },
  "DELETE /api/app/knowledge/{id}/document": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Knowledge document removed."
   }
  },
  "DELETE /api/app/knowledge/{id}": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Knowledge entry deleted."
   }
  },
  "GET /api/app/archive": {
   "status": 200,
   "body": {
    "ok": true,
    "total": 4,
    "rows": [
     {
      "kind": "client",
      "type": "client",
      "id": 1,
      "name": "Capture Archive Client",
      "entry": {
       "id": 1,
       "legalName": "Capture Archive Client",
       "dba": "",
       "type": "",
       "industry": "",
       "status": "",
       "stage": "Lead",
       "value": null,
       "nextFollowUp": null,
       "source": "",
       "sourceDetail": "",
       "contactName": "",
       "contactTitle": "",
       "email": "",
       "phone": "",
       "street": "",
       "city": "",
       "state": "",
       "zip": "",
       "notes": "",
       "tags": "",
       "friends": false,
       "mergedIntoId": null,
       "importBatchId": null,
       "archivedAt": 1793753742028,
       "archivedBy": 1,
       "createdAt": 1793753741082,
       "updatedAt": 1793753742028
      }
     },
     {
      "kind": "case",
      "type": "case",
      "id": 1,
      "name": "Capture Archive Case",
      "entry": {
       "id": 1,
       "clientId": 1,
       "assignedUserId": null,
       "title": "Capture Archive Case",
       "status": "Open",
       "priority": "Normal",
       "category": "",
       "dueDate": null,
       "openedDate": "",
       "closedDate": "",
       "notes": "",
       "adMaterial": false,
       "archivedAt": 1793753741765,
       "archivedBy": 1,
       "createdAt": 1793753741368,
       "updatedAt": 1793753741765,
       "clientName": "Capture Archive Client",
       "ticketCount": 0
      }
     },
     {
      "kind": "ticket",
      "type": "ticket",
      "id": 1,
      "name": "Capture Archive Ticket",
      "entry": {
       "id": 1,
       "clientId": 1,
       "assignedUserId": null,
       "title": "Capture Archive Ticket",
       "status": "Open",
       "priority": "Normal",
       "category": "",
       "dueDate": null,
       "openedDate": "",
       "closedDate": "",
       "notes": "",
       "adMaterial": false,
       "archivedAt": 1793753741850,
       "archivedBy": 1,
       "createdAt": 1793753741471,
       "updatedAt": 1793753741850,
       "caseId": null,
       "source": "Internal",
       "sourceRef": null,
       "mergedIntoId": null,
       "tags": "",
       "caseTitle": "",
       "clientName": "Capture Archive Client"
      }
     },
     {
      "kind": "asset",
      "type": "asset",
      "id": 1,
      "name": "Capture Archive Asset",
      "entry": {
       "id": 1,
       "clientId": null,
       "assignedUserId": null,
       "name": "Capture Archive Asset",
       "type": "",
       "identifier": "",
       "reviewDueDate": "",
       "scope": "Sentinel",
       "status": "In Service",
       "warrantyUntil": null,
       "warrantyExpires": null,
       "supportExpires": null,
       "qrCode": "SCA-sUTLESuuSWDk-KOojU6Tig",
       "notes": "",
       "archivedAt": 1793753741937,
       "archivedBy": 1,
       "createdAt": 1793753741582,
       "updatedAt": 1793753741937
      }
     }
    ]
   }
  },
  "POST /api/app/archive/ticket/{id}/restore": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Ticket restored.",
    "ticket": {
     "id": 1,
     "clientId": 1,
     "assignedUserId": null,
     "title": "Capture Archive Ticket",
     "status": "Open",
     "priority": "Normal",
     "category": "",
     "dueDate": null,
     "openedDate": "",
     "closedDate": "",
     "notes": "",
     "adMaterial": false,
     "archivedAt": null,
     "archivedBy": null,
     "createdAt": 1793753741471,
     "updatedAt": 1793753742381,
     "caseId": null,
     "source": "Internal",
     "sourceRef": null,
     "mergedIntoId": null,
     "tags": "",
     "caseTitle": "",
     "clientName": "Capture Archive Client"
    }
   }
  },
  "POST /api/app/archive/case/{id}/restore": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Case restored.",
    "case": {
     "id": 1,
     "clientId": 1,
     "assignedUserId": null,
     "title": "Capture Archive Case",
     "status": "Open",
     "priority": "Normal",
     "category": "",
     "dueDate": null,
     "openedDate": "",
     "closedDate": "",
     "notes": "",
     "adMaterial": false,
     "archivedAt": null,
     "archivedBy": null,
     "createdAt": 1793753741368,
     "updatedAt": 1793753742657,
     "clientName": "Capture Archive Client",
     "ticketCount": 0
    }
   }
  },
  "POST /api/app/archive/asset/{id}/restore": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Asset restored."
   }
  },
  "POST /api/app/archive/client/{id}/restore": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Client restored.",
    "client": {
     "id": 1,
     "legalName": "Capture Archive Client",
     "dba": "",
     "type": "",
     "industry": "",
     "status": "",
     "stage": "Lead",
     "value": null,
     "nextFollowUp": null,
     "source": "",
     "sourceDetail": "",
     "contactName": "",
     "contactTitle": "",
     "email": "",
     "phone": "",
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
     "createdAt": 1793753741082,
     "updatedAt": 1793753742983,
     "familyLinked": false,
     "tier": ""
    }
   }
  },
  "GET /api/app/archive (empty)": {
   "status": 200,
   "body": {
    "ok": true,
    "total": 0,
    "rows": []
   }
  },
  "DELETE /api/app/archive/ticket/{id}": {
   "status": 200,
   "body": {
    "ok": true,
    "message": "Ticket deleted."
   }
  },
  "DELETE /api/app/archive/ticket/{id} (missing)": {
   "status": 404,
   "body": {
    "ok": false,
    "message": "Not found."
   }
  },
  "POST /api/app/archive/widget/{id}/restore (unknown kind)": {
   "status": 404,
   "body": {
    "ok": false,
    "message": "Not found."
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

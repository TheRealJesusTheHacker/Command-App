package net.sentinelsafeguard.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Phase 4 REAL response bodies, captured 2026-11-03 from a
 * rehearsal instance running the Phase 4 candidate bytes
 * (server.py sha256 7147f9a14d39cd111445925bc58020886b6d785cdde125d83f2bd78a063c45a6,
 * Addendum 16; the deployed production bytes 6b07dca8… are that
 * candidate rebased — the Phase 4 delta is byte-identical, per
 * Addendum 16a), against a seeded scratch database. No tokens or
 * secrets were recorded; the tax pack's zipBase64 / pdfBase64 are
 * recorded as {length, sha256} markers, exactly as captured.
 * [ALL] is the captures file verbatim (47 captures + _meta).
 */
object RealBodiesPhase4 {
    val ALL: String = """{
 "_meta": {
  "kind": "phase4-rehearsal-captures",
  "serverPySha256": "7147f9a14d39cd111445925bc58020886b6d785cdde125d83f2bd78a063c45a6",
  "note": "Real bodies from rehearsal instance of deployed bytes; scratch DB shredded after capture; no tokens or secrets recorded."
 },
 "price_book_save": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Price item saved.",
   "id": 1,
   "item": {
    "id": 1,
    "name": "Security Assessment",
    "description": "Full assessment",
    "rateCents": 50000,
    "unit": "flat",
    "active": true,
    "createdAt": 1793737412637,
    "updatedAt": 1793737412637
   }
  }
 },
 "price_book_list": {
  "status": 200,
  "body": {
   "items": [
    {
     "id": 1,
     "name": "Security Assessment",
     "description": "Full assessment",
     "rateCents": 50000,
     "unit": "flat",
     "active": true,
     "createdAt": 1793737412637,
     "updatedAt": 1793737412637
    }
   ],
   "ok": true
  }
 },
 "invoice_create": {
  "status": 201,
  "body": {
   "ok": true,
   "message": "Invoice saved.",
   "id": 1,
   "invoice": {
    "id": 1,
    "invoiceNumber": "INV-0001",
    "clientId": 1,
    "status": "Draft",
    "issueDate": "2026-11-01",
    "dueDate": "2026-10-01",
    "paidDate": "",
    "recipientEmail": "billing@example.invalid",
    "notes": "Capture invoice",
    "taxRatePercent": 10.0,
    "items": [
     {
      "description": "Security Assessment",
      "quantity": 1.0,
      "unitRateCents": 50000,
      "lineTotalCents": 50000
     }
    ],
    "subtotalCents": 50000,
    "taxCents": 5000,
    "totalCents": 55000,
    "overdue": false,
    "createdAt": 1793737412680,
    "updatedAt": 1793737412680,
    "discountCents": 0,
    "paidCents": 0,
    "balanceDueCents": 55000,
    "paymentEnabled": false,
    "paymentMode": "",
    "paymentLinkUrl": null,
    "paymentFamilyBlocked": false
   }
  }
 },
 "invoices_list": {
  "status": 200,
  "body": {
   "ok": true,
   "total": 1,
   "limit": 50,
   "offset": 0,
   "invoices": [
    {
     "id": 1,
     "invoiceNumber": "INV-0001",
     "clientId": 1,
     "status": "Draft",
     "issueDate": "2026-11-01",
     "dueDate": "2026-10-01",
     "paidDate": "",
     "recipientEmail": "billing@example.invalid",
     "notes": "Capture invoice",
     "taxRatePercent": 10.0,
     "items": [
      {
       "description": "Security Assessment",
       "quantity": 1.0,
       "unitRateCents": 50000,
       "lineTotalCents": 50000
      }
     ],
     "subtotalCents": 50000,
     "taxCents": 5000,
     "totalCents": 55000,
     "overdue": false,
     "createdAt": 1793737412680,
     "updatedAt": 1793737412680,
     "discountCents": 0,
     "paidCents": 0,
     "balanceDueCents": 55000,
     "paymentEnabled": false,
     "paymentMode": "",
     "paymentLinkUrl": null,
     "paymentFamilyBlocked": false,
     "clientName": "Capture Client LLC"
    }
   ],
   "stats": {
    "outstandingCents": 0,
    "outstandingCount": 0,
    "overdueCents": 0,
    "overdueCount": 0,
    "collectedCents": 0,
    "draftCount": 1,
    "draftCents": 55000
   }
  }
 },
 "invoices_list_overdue_after_send": {
  "status": 200,
  "body": {
   "ok": true,
   "total": 0,
   "limit": 50,
   "offset": 0,
   "invoices": [],
   "stats": {
    "outstandingCents": 0,
    "outstandingCount": 0,
    "overdueCents": 0,
    "overdueCount": 0,
    "collectedCents": 0,
    "draftCount": 1,
    "draftCents": 55000
   }
  }
 },
 "invoice_patch": {
  "status": 200,
  "body": {
   "invoice": {
    "id": 1,
    "invoiceNumber": "INV-0001",
    "clientId": 1,
    "status": "Draft",
    "issueDate": "2026-11-01",
    "dueDate": "2026-10-01",
    "paidDate": "",
    "recipientEmail": "billing@example.invalid",
    "notes": "Edited",
    "taxRatePercent": 10.0,
    "items": [
     {
      "description": "Security Assessment",
      "quantity": 1.0,
      "unitRateCents": 50000,
      "lineTotalCents": 50000
     }
    ],
    "subtotalCents": 50000,
    "taxCents": 5000,
    "totalCents": 55000,
    "overdue": false,
    "createdAt": 1793737412680,
    "updatedAt": 1793737412699,
    "discountCents": 0,
    "paidCents": 0,
    "balanceDueCents": 55000,
    "paymentEnabled": false,
    "paymentMode": "",
    "paymentLinkUrl": null,
    "paymentFamilyBlocked": false
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
    "createdAt": 1793737412627,
    "updatedAt": 1793737412627
   },
   "activity": [
    {
     "id": 14,
     "actorId": 1,
     "action": "INVOICE_SAVED",
     "entityType": "invoice",
     "entityId": 1,
     "detail": "Invoice saved.",
     "createdAt": 1793737412699
    },
    {
     "id": 13,
     "actorId": 1,
     "action": "INVOICE_SAVED",
     "entityType": "invoice",
     "entityId": 1,
     "detail": "Invoice saved.",
     "createdAt": 1793737412680
    }
   ],
   "ok": true,
   "payments": {
    "invoiceId": 1,
    "invoiceNumber": "INV-0001",
    "status": "Draft",
    "totalCents": 55000,
    "paidCents": 0,
    "balanceDueCents": 55000,
    "payments": [],
    "ok": true
   },
   "plan": null
  }
 },
 "invoice_status_sent": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Invoice status updated.",
   "id": 1,
   "status": "Sent",
   "paidDate": ""
  }
 },
 "invoice_detail": {
  "status": 200,
  "body": {
   "invoice": {
    "id": 1,
    "invoiceNumber": "INV-0001",
    "clientId": 1,
    "status": "Sent",
    "issueDate": "2026-11-01",
    "dueDate": "2026-10-01",
    "paidDate": "",
    "recipientEmail": "billing@example.invalid",
    "notes": "Edited",
    "taxRatePercent": 10.0,
    "items": [
     {
      "description": "Security Assessment",
      "quantity": 1.0,
      "unitRateCents": 50000,
      "lineTotalCents": 50000
     }
    ],
    "subtotalCents": 50000,
    "taxCents": 5000,
    "totalCents": 55000,
    "overdue": true,
    "createdAt": 1793737412680,
    "updatedAt": 1793737412705,
    "discountCents": 0,
    "paidCents": 0,
    "balanceDueCents": 55000,
    "paymentEnabled": false,
    "paymentMode": "",
    "paymentLinkUrl": null,
    "paymentFamilyBlocked": false
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
    "createdAt": 1793737412627,
    "updatedAt": 1793737412627
   },
   "activity": [
    {
     "id": 15,
     "actorId": 1,
     "action": "INVOICE_STATUS_CHANGED",
     "entityType": "invoice",
     "entityId": 1,
     "detail": "Invoice status changed from Draft to Sent for INV-0001.",
     "createdAt": 1793737412705
    },
    {
     "id": 14,
     "actorId": 1,
     "action": "INVOICE_SAVED",
     "entityType": "invoice",
     "entityId": 1,
     "detail": "Invoice saved.",
     "createdAt": 1793737412699
    },
    {
     "id": 13,
     "actorId": 1,
     "action": "INVOICE_SAVED",
     "entityType": "invoice",
     "entityId": 1,
     "detail": "Invoice saved.",
     "createdAt": 1793737412680
    }
   ],
   "ok": true,
   "payments": {
    "invoiceId": 1,
    "invoiceNumber": "INV-0001",
    "status": "Sent",
    "totalCents": 55000,
    "paidCents": 0,
    "balanceDueCents": 55000,
    "payments": [],
    "ok": true
   },
   "plan": null
  }
 },
 "invoice_document": {
  "status": 200,
  "body": {
   "ok": true,
   "invoiceId": 1,
   "invoiceNumber": "INV-0001",
   "html": "<!doctype html><html><head><title>Paper invoice packet</title><style>body{font-family:sans-serif;color:#000;background:#fff}.sheet{page-break-after:always}.invdoc{border:1px solid #000;padding:18px;margin:0 0 18px}.invbiz{font-size:20px;font-weight:bold}.invbizsub{color:#1a7f37;font-size:12px}.invtitle{font-size:18px;font-weight:bold;text-align:right}table{border-collapse:collapse;width:100%}td,th{border-bottom:1px solid #999;padding:4px 6px;text-align:left}.num{text-align:right}.grand{font-weight:bold;font-size:15px}pre{white-space:pre-wrap;font-family:inherit}@media print{.sheet{page-break-after:always}}</style></head><body><section class=\"invdoc\"><header><div class=\"invbiz\">Sentinel Safeguard LLC</div><div class=\"invbizsub\">Kansas City, Missouri -- by appointment only</div><div class=\"invtitle\">INVOICE INV-0001</div></header><p>Bill to: <b>Capture Client LLC</b><br>Pat P</p><p>Number <b>INV-0001</b> -- Issued <b>2026-11-01</b> -- Due <b>2026-10-01</b> -- Status <b>Sent - OVERDUE</b></p><table><thead><tr><th>Description</th><th class=\"num\">Qty</th><th class=\"num\">Rate</th><th class=\"num\">Amount</th></tr></thead><tbody><tr><td>Security Assessment</td><td class=\"num\">1.0</td><td class=\"num\">$500.00</td><td class=\"num\">$500.00</td></tr></tbody></table><p>Subtotal $500.00 -- Tax (10.0%) $50.00</p><p class=\"grand\">Total $550.00</p><p>Notes: Edited</p></section></body></html>"
  }
 },
 "invoice_payments_empty": {
  "status": 200,
  "body": {
   "invoiceId": 1,
   "invoiceNumber": "INV-0001",
   "status": "Sent",
   "totalCents": 55000,
   "paidCents": 0,
   "balanceDueCents": 55000,
   "payments": [],
   "ok": true
  }
 },
 "invoice_payment_record_partial": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Payment recorded.",
   "id": 1,
   "invoiceId": 1,
   "invoiceNumber": "INV-0001",
   "status": "Sent",
   "totalCents": 55000,
   "paidCents": 5000,
   "balanceDueCents": 50000,
   "payments": [
    {
     "id": 1,
     "amountCents": 5000,
     "method": "check",
     "paymentDate": "2026-11-02",
     "note": "",
     "recordedBy": 1,
     "createdAt": 1793737412728
    }
   ],
   "invoiceStatus": "Sent"
  }
 },
 "invoice_payment_overpay_refusal": {
  "status": 403,
  "body": {
   "ok": false,
   "authorized": false,
   "message": "Payment of $999999.99 exceeds the balance due of $500.00 on invoice INV-0001 \u2014 a payment can't be more than what is owed, and nothing was recorded."
  }
 },
 "invoice_plan_create": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Payment plan created.",
   "id": 1,
   "plan": {
    "id": 1,
    "invoiceId": 1,
    "invoiceNumber": "INV-0001",
    "clientId": 1,
    "status": "active",
    "notes": "",
    "installments": [
     {
      "id": 1,
      "seq": 1,
      "amountCents": 25000,
      "dueDate": "2026-10-15",
      "appliedCents": 5000,
      "remainingCents": 20000,
      "status": "overdue"
     },
     {
      "id": 2,
      "seq": 2,
      "amountCents": 25000,
      "dueDate": "2026-12-15",
      "appliedCents": 0,
      "remainingCents": 25000,
      "status": "pending"
     }
    ],
    "totalCents": 50000,
    "paidCents": 5000,
    "createdAt": 1793737412742,
    "updatedAt": 1793737412742
   }
  }
 },
 "invoice_plan_get": {
  "status": 200,
  "body": {
   "plan": {
    "id": 1,
    "invoiceId": 1,
    "invoiceNumber": "INV-0001",
    "clientId": 1,
    "status": "active",
    "notes": "",
    "installments": [
     {
      "id": 1,
      "seq": 1,
      "amountCents": 25000,
      "dueDate": "2026-10-15",
      "appliedCents": 5000,
      "remainingCents": 20000,
      "status": "overdue"
     },
     {
      "id": 2,
      "seq": 2,
      "amountCents": 25000,
      "dueDate": "2026-12-15",
      "appliedCents": 0,
      "remainingCents": 25000,
      "status": "pending"
     }
    ],
    "totalCents": 50000,
    "paidCents": 5000,
    "createdAt": 1793737412742,
    "updatedAt": 1793737412742
   },
   "ok": true
  }
 },
 "overdue_installments": {
  "status": 200,
  "body": {
   "overdue": [
    {
     "planId": 1,
     "installmentId": 1,
     "invoiceId": 1,
     "invoiceNumber": "INV-0001",
     "clientId": 1,
     "clientName": "Capture Client LLC",
     "seq": 1,
     "amountCents": 25000,
     "remainingCents": 20000,
     "dueDate": "2026-10-15"
    }
   ],
   "count": 1,
   "ok": true
  }
 },
 "reminder_generate": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Reminder draft created \u2014 nothing was sent. Review it and send it by hand when ready.",
   "id": 1,
   "draft": {
    "id": 1,
    "planId": 1,
    "installmentId": 1,
    "invoiceId": 1,
    "clientId": 1,
    "subject": "Payment reminder \u2014 invoice INV-0001, installment 1",
    "body": "Hello Pat P,\n\nThis is a friendly reminder from Sentinel Safeguard LLC that installment 1 of the payment plan for invoice INV-0001 was due on 2026-10-15. The remaining balance on this installment is $200.00.\n\nIf you have already paid, thank you \u2014 please disregard this note. Otherwise, contact us and we will help you get caught up.\n\n\u2014 Sentinel Safeguard LLC",
    "status": "draft",
    "createdAt": 1793737412784,
    "sentAt": null
   }
  }
 },
 "reminder_drafts": {
  "status": 200,
  "body": {
   "drafts": [
    {
     "id": 1,
     "planId": 1,
     "installmentId": 1,
     "invoiceId": 1,
     "clientId": 1,
     "subject": "Payment reminder \u2014 invoice INV-0001, installment 1",
     "body": "Hello Pat P,\n\nThis is a friendly reminder from Sentinel Safeguard LLC that installment 1 of the payment plan for invoice INV-0001 was due on 2026-10-15. The remaining balance on this installment is $200.00.\n\nIf you have already paid, thank you \u2014 please disregard this note. Otherwise, contact us and we will help you get caught up.\n\n\u2014 Sentinel Safeguard LLC",
     "status": "draft",
     "createdAt": 1793737412784,
     "sentAt": null
    }
   ],
   "ok": true
  }
 },
 "invoice_payment_link_disabled_refusal": {
  "status": 403,
  "body": {
   "ok": false,
   "authorized": false,
   "message": "Online payments are not enabled \u2014 set PAYMENTS_ENABLED=1 in the payments config to turn them on."
  }
 },
 "invoice_complete_payment_refusal": {
  "status": 403,
  "body": {
   "ok": false,
   "authorized": false,
   "message": "No payment link matches that Stripe session."
  }
 },
 "invoice_payment_final": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Payment recorded.",
   "id": 2,
   "invoiceId": 1,
   "invoiceNumber": "INV-0001",
   "status": "Paid",
   "totalCents": 55000,
   "paidCents": 55000,
   "balanceDueCents": 0,
   "payments": [
    {
     "id": 1,
     "amountCents": 5000,
     "method": "check",
     "paymentDate": "2026-11-02",
     "note": "",
     "recordedBy": 1,
     "createdAt": 1793737412728
    },
    {
     "id": 2,
     "amountCents": 50000,
     "method": "check",
     "paymentDate": "2026-11-03",
     "note": "",
     "recordedBy": 1,
     "createdAt": 1793737412805
    }
   ],
   "invoiceStatus": "Paid"
  }
 },
 "quote_create": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Quote saved.",
   "id": 1,
   "quote": {
    "id": 1,
    "quoteNumber": "QT-0001",
    "clientId": 1,
    "clientName": "Capture Client LLC",
    "title": "Capture Quote",
    "status": "draft",
    "convertTo": "ticket",
    "notes": "",
    "taxRatePercent": 0.0,
    "expiresDate": "",
    "expiredByDate": false,
    "lines": [
     {
      "id": 1,
      "priceItemId": 1,
      "description": "Security Assessment",
      "quantity": 1.0,
      "unitRateCents": 50000,
      "lineTotalCents": 50000
     }
    ],
    "subtotalCents": 50000,
    "taxCents": 0,
    "totalCents": 50000,
    "convertedCaseId": null,
    "convertedTicketId": null,
    "convertedInvoiceId": null,
    "createdAt": 1793737412811,
    "updatedAt": 1793737412811,
    "discountCents": 0
   }
  }
 },
 "quotes_list": {
  "status": 200,
  "body": {
   "quotes": [
    {
     "id": 1,
     "quoteNumber": "QT-0001",
     "clientId": 1,
     "clientName": "Capture Client LLC",
     "title": "Capture Quote",
     "status": "draft",
     "convertTo": "ticket",
     "notes": "",
     "taxRatePercent": 0.0,
     "expiresDate": "",
     "expiredByDate": false,
     "lines": [
      {
       "id": 1,
       "priceItemId": 1,
       "description": "Security Assessment",
       "quantity": 1.0,
       "unitRateCents": 50000,
       "lineTotalCents": 50000
      }
     ],
     "subtotalCents": 50000,
     "taxCents": 0,
     "totalCents": 50000,
     "convertedCaseId": null,
     "convertedTicketId": null,
     "convertedInvoiceId": null,
     "createdAt": 1793737412811,
     "updatedAt": 1793737412811,
     "discountCents": 0
    }
   ],
   "ok": true
  }
 },
 "quote_detail": {
  "status": 200,
  "body": {
   "quote": {
    "id": 1,
    "quoteNumber": "QT-0001",
    "clientId": 1,
    "clientName": "Capture Client LLC",
    "title": "Capture Quote",
    "status": "draft",
    "convertTo": "ticket",
    "notes": "",
    "taxRatePercent": 0.0,
    "expiresDate": "",
    "expiredByDate": false,
    "lines": [
     {
      "id": 1,
      "priceItemId": 1,
      "description": "Security Assessment",
      "quantity": 1.0,
      "unitRateCents": 50000,
      "lineTotalCents": 50000
     }
    ],
    "subtotalCents": 50000,
    "taxCents": 0,
    "totalCents": 50000,
    "convertedCaseId": null,
    "convertedTicketId": null,
    "convertedInvoiceId": null,
    "createdAt": 1793737412811,
    "updatedAt": 1793737412811,
    "discountCents": 0
   },
   "ok": true
  }
 },
 "quote_patch": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Quote saved.",
   "id": 1,
   "quote": {
    "id": 1,
    "quoteNumber": "QT-0001",
    "clientId": 1,
    "clientName": "Capture Client LLC",
    "title": "Capture Quote v2",
    "status": "draft",
    "convertTo": "ticket",
    "notes": "",
    "taxRatePercent": 0.0,
    "expiresDate": "",
    "expiredByDate": false,
    "lines": [
     {
      "id": 2,
      "priceItemId": 1,
      "description": "Security Assessment",
      "quantity": 1.0,
      "unitRateCents": 50000,
      "lineTotalCents": 50000
     }
    ],
    "subtotalCents": 50000,
    "taxCents": 0,
    "totalCents": 50000,
    "convertedCaseId": null,
    "convertedTicketId": null,
    "convertedInvoiceId": null,
    "createdAt": 1793737412811,
    "updatedAt": 1793737412826,
    "discountCents": 0
   }
  }
 },
 "quote_status_sent": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Quote status updated.",
   "status": "sent",
   "quote": {
    "id": 1,
    "quoteNumber": "QT-0001",
    "clientId": 1,
    "clientName": "Capture Client LLC",
    "title": "Capture Quote v2",
    "status": "sent",
    "convertTo": "ticket",
    "notes": "",
    "taxRatePercent": 0.0,
    "expiresDate": "",
    "expiredByDate": false,
    "lines": [
     {
      "id": 2,
      "priceItemId": 1,
      "description": "Security Assessment",
      "quantity": 1.0,
      "unitRateCents": 50000,
      "lineTotalCents": 50000
     }
    ],
    "subtotalCents": 50000,
    "taxCents": 0,
    "totalCents": 50000,
    "convertedCaseId": null,
    "convertedTicketId": null,
    "convertedInvoiceId": null,
    "createdAt": 1793737412811,
    "updatedAt": 1793737412871,
    "discountCents": 0
   }
  }
 },
 "quote_status_approved_conversion": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Quote status updated.",
   "status": "approved",
   "conversion": {
    "caseId": null,
    "ticketId": 2,
    "invoiceId": 2,
    "alreadyConverted": false
   },
   "quote": {
    "id": 1,
    "quoteNumber": "QT-0001",
    "clientId": 1,
    "clientName": "Capture Client LLC",
    "title": "Capture Quote v2",
    "status": "approved",
    "convertTo": "ticket",
    "notes": "",
    "taxRatePercent": 0.0,
    "expiresDate": "",
    "expiredByDate": false,
    "lines": [
     {
      "id": 2,
      "priceItemId": 1,
      "description": "Security Assessment",
      "quantity": 1.0,
      "unitRateCents": 50000,
      "lineTotalCents": 50000
     }
    ],
    "subtotalCents": 50000,
    "taxCents": 0,
    "totalCents": 50000,
    "convertedCaseId": null,
    "convertedTicketId": 2,
    "convertedInvoiceId": 2,
    "createdAt": 1793737412811,
    "updatedAt": 1793737412880,
    "discountCents": 0
   }
  }
 },
 "money_builder": {
  "status": 200,
  "body": {
   "items": [
    {
     "id": 1,
     "name": "Security Assessment",
     "description": "Full assessment",
     "rateCents": 50000,
     "unit": "flat",
     "active": true,
     "createdAt": 1793737412637,
     "updatedAt": 1793737412637
    }
   ],
   "clients": [
    {
     "id": 1,
     "legalName": "Capture Client LLC",
     "familyLinked": false
    }
   ],
   "ok": true
  }
 },
 "money_build_quote": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Quote built from the price book \u2014 saved as a draft in the existing quote flow.",
   "id": 2,
   "quote": {
    "id": 2,
    "quoteNumber": "QT-0002",
    "clientId": 1,
    "clientName": "Capture Client LLC",
    "title": "Estimate",
    "status": "draft",
    "convertTo": "ticket",
    "notes": "",
    "taxRatePercent": 0.0,
    "expiresDate": "",
    "expiredByDate": false,
    "lines": [
     {
      "id": 3,
      "priceItemId": 1,
      "description": "Security Assessment",
      "quantity": 2.0,
      "unitRateCents": 50000,
      "lineTotalCents": 100000
     }
    ],
    "subtotalCents": 100000,
    "taxCents": 0,
    "totalCents": 100000,
    "convertedCaseId": null,
    "convertedTicketId": null,
    "convertedInvoiceId": null,
    "createdAt": 1793737412891,
    "updatedAt": 1793737412891,
    "discountCents": 0
   },
   "builtFromPriceBook": true
  }
 },
 "money_unbilled": {
  "status": 200,
  "body": {
   "clientId": 1,
   "familyLinked": false,
   "timeCount": 1,
   "timeSeconds": 3600,
   "timeCents": 12000,
   "expenseCount": 0,
   "expenseCents": 0,
   "mileageCount": 0,
   "totalCents": 12000,
   "ok": true
  }
 },
 "money_convert_unbilled": {
  "status": 200,
  "body": {
   "ok": true,
   "invoiceId": 3,
   "invoice": {
    "id": 3,
    "invoiceNumber": "INV-0003",
    "clientId": 1,
    "status": "Draft",
    "issueDate": "2026-11-03",
    "dueDate": "",
    "paidDate": "",
    "recipientEmail": "",
    "notes": "Created from unbilled time, mileage, and expenses.",
    "taxRatePercent": 0.0,
    "items": [
     {
      "description": "Unbilled work (1h 00m)",
      "quantity": 1.0,
      "unitRateCents": 12000,
      "lineTotalCents": 12000
     }
    ],
    "subtotalCents": 12000,
    "taxCents": 0,
    "totalCents": 12000,
    "overdue": false,
    "createdAt": 1793737412909,
    "updatedAt": 1793737412909,
    "discountCents": 0,
    "paidCents": 0,
    "balanceDueCents": 12000,
    "paymentEnabled": false,
    "paymentMode": "",
    "paymentLinkUrl": null,
    "paymentFamilyBlocked": false
   },
   "timeEntryIds": [
    1
   ],
   "expenseIds": [],
   "message": "Draft invoice INV-0003 created from 1 time entries and 0 expenses."
  }
 },
 "money_scan_low_stock": {
  "status": 200,
  "body": {
   "scanned": 1,
   "newDrafts": 1,
   "ok": true,
   "message": "Low-stock scan complete \u2014 purchase drafts only, nothing was ordered.",
   "drafts": [
    {
     "id": 1,
     "partId": 1,
     "vendorId": null,
     "vendorName": "",
     "partName": "Cap Widget",
     "sku": "W-9",
     "qtyOnHand": 0,
     "reorderThreshold": 1,
     "qtyToOrder": 2,
     "unitCostCents": 500,
     "estTotalCents": 1000,
     "status": "draft",
     "createdAt": 1793737412919
    }
   ]
  }
 },
 "money_purchase_drafts": {
  "status": 200,
  "body": {
   "drafts": [
    {
     "id": 1,
     "partId": 1,
     "vendorId": null,
     "vendorName": "",
     "partName": "Cap Widget",
     "sku": "W-9",
     "qtyOnHand": 0,
     "reorderThreshold": 1,
     "qtyToOrder": 2,
     "unitCostCents": 500,
     "estTotalCents": 1000,
     "status": "draft",
     "createdAt": 1793737412919
    }
   ],
   "ok": true
  }
 },
 "money_pay_board": {
  "status": 200,
  "body": {
   "invoices": [
    {
     "invoiceId": 3,
     "invoiceNumber": "INV-0003",
     "clientId": 1,
     "clientName": "Capture Client LLC",
     "status": "Draft",
     "issueDate": "2026-11-03",
     "dueDate": "",
     "totalCents": 12000,
     "paidCents": 0,
     "balanceDueCents": 12000,
     "payLinkState": "none",
     "payLinkCreatedAt": 0,
     "manualState": "none",
     "manualNote": "",
     "manualUpdatedAt": null
    },
    {
     "invoiceId": 2,
     "invoiceNumber": "INV-0002",
     "clientId": 1,
     "clientName": "Capture Client LLC",
     "status": "Draft",
     "issueDate": "2026-11-03",
     "dueDate": "",
     "totalCents": 50000,
     "paidCents": 0,
     "balanceDueCents": 50000,
     "payLinkState": "none",
     "payLinkCreatedAt": 0,
     "manualState": "none",
     "manualNote": "",
     "manualUpdatedAt": null
    },
    {
     "invoiceId": 1,
     "invoiceNumber": "INV-0001",
     "clientId": 1,
     "clientName": "Capture Client LLC",
     "status": "Paid",
     "issueDate": "2026-11-01",
     "dueDate": "2026-10-01",
     "totalCents": 55000,
     "paidCents": 55000,
     "balanceDueCents": 0,
     "payLinkState": "none",
     "payLinkCreatedAt": 0,
     "manualState": "none",
     "manualNote": "",
     "manualUpdatedAt": null
    }
   ],
   "manualStates": [
    "none",
    "awaiting",
    "reminded",
    "promised",
    "partial",
    "paid_manual",
    "disputed"
   ],
   "totalBalanceDueCents": 0,
   "ok": true
  }
 },
 "money_quote_followups_scan": {
  "status": 200,
  "body": {
   "scanned": 0,
   "newDrafts": 0,
   "superseded": 0,
   "ok": true,
   "message": "Quote follow-up scan complete (day 7/14) \u2014 drafts only, nothing was sent.",
   "followUps": []
  }
 },
 "money_quote_followups": {
  "status": 200,
  "body": {
   "followUps": [],
   "ok": true
  }
 },
 "money_aging_scan": {
  "status": 200,
  "body": {
   "scanned": 0,
   "newDrafts": 0,
   "ok": true,
   "message": "Invoice aging scan complete (30/60/90) \u2014 drafts only, nothing was sent.",
   "drafts": []
  }
 },
 "money_aging_drafts": {
  "status": 200,
  "body": {
   "drafts": [],
   "ok": true
  }
 },
 "money_desk_aggregate": {
  "status": 200,
  "body": {
   "builder": {
    "authorized": true,
    "items": [
     {
      "id": 1,
      "name": "Security Assessment",
      "description": "Full assessment",
      "rateCents": 50000,
      "unit": "flat",
      "active": true,
      "createdAt": 1793737412637,
      "updatedAt": 1793737412637
     }
    ],
    "clients": [
     {
      "id": 1,
      "legalName": "Capture Client LLC",
      "familyLinked": false
     }
    ]
   },
   "payBoard": {
    "authorized": true,
    "invoices": [
     {
      "invoiceId": 3,
      "invoiceNumber": "INV-0003",
      "clientId": 1,
      "clientName": "Capture Client LLC",
      "status": "Draft",
      "issueDate": "2026-11-03",
      "dueDate": "",
      "totalCents": 12000,
      "paidCents": 0,
      "balanceDueCents": 12000,
      "payLinkState": "none",
      "payLinkCreatedAt": 0,
      "manualState": "none",
      "manualNote": "",
      "manualUpdatedAt": null
     },
     {
      "invoiceId": 2,
      "invoiceNumber": "INV-0002",
      "clientId": 1,
      "clientName": "Capture Client LLC",
      "status": "Draft",
      "issueDate": "2026-11-03",
      "dueDate": "",
      "totalCents": 50000,
      "paidCents": 0,
      "balanceDueCents": 50000,
      "payLinkState": "none",
      "payLinkCreatedAt": 0,
      "manualState": "none",
      "manualNote": "",
      "manualUpdatedAt": null
     },
     {
      "invoiceId": 1,
      "invoiceNumber": "INV-0001",
      "clientId": 1,
      "clientName": "Capture Client LLC",
      "status": "Paid",
      "issueDate": "2026-11-01",
      "dueDate": "2026-10-01",
      "totalCents": 55000,
      "paidCents": 55000,
      "balanceDueCents": 0,
      "payLinkState": "none",
      "payLinkCreatedAt": 0,
      "manualState": "none",
      "manualNote": "",
      "manualUpdatedAt": null
     }
    ],
    "manualStates": [
     "none",
     "awaiting",
     "reminded",
     "promised",
     "partial",
     "paid_manual",
     "disputed"
    ],
    "totalBalanceDueCents": 0
   },
   "purchaseDrafts": [
    {
     "id": 1,
     "partId": 1,
     "vendorId": null,
     "vendorName": "",
     "partName": "Cap Widget",
     "sku": "W-9",
     "qtyOnHand": 0,
     "reorderThreshold": 1,
     "qtyToOrder": 2,
     "unitCostCents": 500,
     "estTotalCents": 1000,
     "status": "draft",
     "createdAt": 1793737412919
    }
   ],
   "quoteFollowUps": [],
   "agingDrafts": [],
   "unbilled": [],
   "ok": true
  }
 },
 "tax_pack_owner": {
  "status": 200,
  "body": {
   "ok": true,
   "year": 2026,
   "summary": {
    "year": 2026,
    "timezone": "America/Chicago",
    "invoiceCount": 3,
    "paidTotalCents": 55000,
    "issuedUnpaidTotalCents": 0,
    "draftInvoiceCount": 2,
    "voidInvoiceCount": 0,
    "paymentsTotalCents": 55000,
    "paymentsByMethod": {
     "check": 55000
    },
    "expensesTotalCents": 0,
    "expensesByCategory": {},
    "mileageTotalMiles": 0.0,
    "mileageTotalCents": 0,
    "incomeByServiceLine": {
     "Security assessment": 55000
    },
    "quarters": {
     "1": {
      "paidCents": 0,
      "issuedCents": 0,
      "paymentsCents": 0,
      "expensesCents": 0
     },
     "2": {
      "paidCents": 0,
      "issuedCents": 0,
      "paymentsCents": 0,
      "expensesCents": 0
     },
     "3": {
      "paidCents": 0,
      "issuedCents": 0,
      "paymentsCents": 0,
      "expensesCents": 0
     },
     "4": {
      "paidCents": 55000,
      "issuedCents": 0,
      "paymentsCents": 55000,
      "expensesCents": 0
     }
    },
    "familyMemo": {
     "jobsPerformed": 0,
     "invoicesListedAtZero": 0,
     "note": "Family-linked work is never charged: Family rows are listed at $0 and excluded from every total. This memo counts jobs performed only \u2014 no Family dollars exist to report."
    },
    "notes": [
     "Income totals count Paid invoices at full total; Sent (issued, unpaid) invoices are totaled separately.",
     "Draft invoices are listed in invoices.csv but excluded from every income total; Void invoices are excluded as well.",
     "Payments are the recorded payments ledger (cf_invoice_payments) by payment date.",
     "All dates are America/Chicago calendar dates."
    ]
   },
   "files": {
    "invoicesCsv": "invoice_number,client,client_id,status,manual_payment_state,service_line,issue_date,due_date,paid_date,subtotal,tax,total,family_never_charged\r\nINV-0001,Capture Client LLC,1,Paid,none,Security assessment,2026-11-01,2026-10-01,2026-11-03,500.00,50.00,550.00,\r\nINV-0002,Capture Client LLC,1,Draft,none,Security assessment,2026-11-03,,,500.00,0.00,500.00,\r\nINV-0003,Capture Client LLC,1,Draft,none,Other services,2026-11-03,,,120.00,0.00,120.00,\r\n",
    "expensesCsv": "id,date,category,description,client,client_id,ticket_id,case_id,amount,miles,rate_cents_per_mile,receipt_note\r\n",
    "mileageCsv": "id,date,description,client,ticket_id,case_id,miles,rate_cents_per_mile,amount\r\n",
    "paymentsCsv": "id,payment_date,invoice_number,client,method,amount,note,family_never_charged\r\n1,2026-11-02,INV-0001,Capture Client LLC,check,50.00,,\r\n2,2026-11-03,INV-0001,Capture Client LLC,check,500.00,,\r\n"
   },
   "pdf": {
    "pdfBase64": {
     "length": 3428,
     "sha256": "bffe55f17f76ba6c7acb3a7ac5337485a7a3b3842a838efd71d8ce1c6481b3db"
    },
    "pdfSha256": "17f97b9fdd39519b9997a286603de255b9602117635fa1feaa83d46a90e99c86",
    "pdfSizeBytes": 2571
   },
   "zipBase64": {
    "length": 3704,
    "sha256": "6194f760c904becd3c206ee666cd39cf46affbe58b0a0a5d065b24ea14ecbe0c"
   },
   "filename": "sentinel-tax-pack-2026.zip"
  }
 },
 "tax_pack_accountant": {
  "status": 200,
  "body": {
   "ok": true,
   "year": 2026,
   "summary": {
    "year": 2026,
    "timezone": "America/Chicago",
    "invoiceCount": 3,
    "paidTotalCents": 55000,
    "issuedUnpaidTotalCents": 0,
    "draftInvoiceCount": 2,
    "voidInvoiceCount": 0,
    "paymentsTotalCents": 55000,
    "paymentsByMethod": {
     "check": 55000
    },
    "expensesTotalCents": 0,
    "expensesByCategory": {},
    "mileageTotalMiles": 0.0,
    "mileageTotalCents": 0,
    "incomeByServiceLine": {
     "Security assessment": 55000
    },
    "quarters": {
     "1": {
      "paidCents": 0,
      "issuedCents": 0,
      "paymentsCents": 0,
      "expensesCents": 0
     },
     "2": {
      "paidCents": 0,
      "issuedCents": 0,
      "paymentsCents": 0,
      "expensesCents": 0
     },
     "3": {
      "paidCents": 0,
      "issuedCents": 0,
      "paymentsCents": 0,
      "expensesCents": 0
     },
     "4": {
      "paidCents": 55000,
      "issuedCents": 0,
      "paymentsCents": 55000,
      "expensesCents": 0
     }
    },
    "familyMemo": {
     "jobsPerformed": 0,
     "invoicesListedAtZero": 0,
     "note": "Family-linked work is never charged: Family rows are listed at $0 and excluded from every total. This memo counts jobs performed only \u2014 no Family dollars exist to report."
    },
    "notes": [
     "Income totals count Paid invoices at full total; Sent (issued, unpaid) invoices are totaled separately.",
     "Draft invoices are listed in invoices.csv but excluded from every income total; Void invoices are excluded as well.",
     "Payments are the recorded payments ledger (cf_invoice_payments) by payment date.",
     "All dates are America/Chicago calendar dates."
    ]
   },
   "files": {
    "invoicesCsv": "invoice_number,client,client_id,status,manual_payment_state,service_line,issue_date,due_date,paid_date,subtotal,tax,total,family_never_charged\r\nINV-0001,Capture Client LLC,1,Paid,none,Security assessment,2026-11-01,2026-10-01,2026-11-03,500.00,50.00,550.00,\r\nINV-0002,Capture Client LLC,1,Draft,none,Security assessment,2026-11-03,,,500.00,0.00,500.00,\r\nINV-0003,Capture Client LLC,1,Draft,none,Other services,2026-11-03,,,120.00,0.00,120.00,\r\n",
    "expensesCsv": "id,date,category,description,client,client_id,ticket_id,case_id,amount,miles,rate_cents_per_mile,receipt_note\r\n",
    "mileageCsv": "id,date,description,client,ticket_id,case_id,miles,rate_cents_per_mile,amount\r\n",
    "paymentsCsv": "id,payment_date,invoice_number,client,method,amount,note,family_never_charged\r\n1,2026-11-02,INV-0001,Capture Client LLC,check,50.00,,\r\n2,2026-11-03,INV-0001,Capture Client LLC,check,500.00,,\r\n"
   },
   "pdf": {
    "pdfBase64": {
     "length": 3428,
     "sha256": "bffe55f17f76ba6c7acb3a7ac5337485a7a3b3842a838efd71d8ce1c6481b3db"
    },
    "pdfSha256": "17f97b9fdd39519b9997a286603de255b9602117635fa1feaa83d46a90e99c86",
    "pdfSizeBytes": 2571
   },
   "zipBase64": {
    "length": 3704,
    "sha256": "6194f760c904becd3c206ee666cd39cf46affbe58b0a0a5d065b24ea14ecbe0c"
   },
   "filename": "sentinel-tax-pack-2026.zip"
  }
 },
 "tax_pack_staff_refusal": {
  "status": 403,
  "body": {
   "ok": false,
   "authorized": false,
   "message": "Not authorized."
  }
 },
 "invoices_staff_refusal": {
  "status": 403,
  "body": {
   "ok": false,
   "authorized": false,
   "message": "Not authorized."
  }
 },
 "invoices_accountant_refusal": {
  "status": 403,
  "body": {
   "ok": false,
   "authorized": false,
   "message": "Not authorized."
  }
 },
 "invoice_create_family_refusal": {
  "status": 403,
  "body": {
   "ok": false,
   "authorized": false,
   "message": "Family accounts are never charged \u2014 invoices can't be created or edited for a family member's record."
  }
 },
 "invoice_delete": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Invoice deleted."
  }
 },
 "price_book_delete": {
  "status": 200,
  "body": {
   "ok": true,
   "message": "Price item deleted.",
   "id": 1
  }
 },
 "unauth_invoices": {
  "status": 401,
  "body": {
   "ok": false,
   "authorized": false,
   "message": "Sign-in required."
  }
 }
}
"""

    private val root: JsonObject by lazy { Json.parseToJsonElement(ALL).jsonObject }

    fun body(name: String): String =
        root[name]!!.jsonObject["body"]!!.toString()

    fun status(name: String): Int =
        root[name]!!.jsonObject["status"]!!.jsonPrimitive.int

    fun bodyJson(name: String): JsonObject =
        root[name]!!.jsonObject["body"]!!.jsonObject
}
